package store

import (
	"context"
	"errors"
	"sort"
	"sync"
	"time"
)

// MemoryStore is test adapter — in-memory fake for TransactionLifecycle tests.
// No PG/Redis/Kafka required; Interface is test surface survives refactors.
type MemoryStore struct {
	mu          sync.Mutex
	trx         map[string]ProductTrx
	outbox      map[string]Outbox
	idempotency map[string]string
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{
		trx:         make(map[string]ProductTrx),
		outbox:      make(map[string]Outbox),
		idempotency: make(map[string]string),
	}
}

func (m *MemoryStore) FindTxForUpdate(_ context.Context, txId string) (*ProductTrx, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	p, ok := m.trx[txId]
	if !ok {
		return nil, errors.New("not found")
	}
	cp := p
	return &cp, nil
}

func (m *MemoryStore) InsertTxWithOutbox(_ context.Context, trx ProductTrx, outbox *Outbox, idemKey string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if idemKey != "" {
		if _, exists := m.idempotency[idemKey]; exists {
			return ErrIdempotencyExists
		}
		m.idempotency[idemKey] = "{}"
	}
	if _, exists := m.trx[trx.TransactionID]; exists {
		return errors.New("duplicate transaction")
	}
	m.trx[trx.TransactionID] = trx
	if outbox != nil {
		m.outbox[outbox.ID] = *outbox
	}
	return nil
}

func (m *MemoryStore) UpdateStatusWithOutbox(_ context.Context, txId, orderStatus, paymentStatus string, outbox *Outbox) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	p, ok := m.trx[txId]
	if !ok {
		return errors.New("not found")
	}
	p.OrderStatus = orderStatus
	p.PaymentStatus = paymentStatus
	now := time.Now()
	p.SysUpdateDate = &now
	m.trx[txId] = p
	if outbox != nil {
		m.outbox[outbox.ID] = *outbox
	}
	return nil
}

func (m *MemoryStore) GetIdempotency(_ context.Context, key string) (string, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	v, ok := m.idempotency[key]
	if !ok {
		return "", errors.New("not found")
	}
	return v, nil
}

func (m *MemoryStore) PutIdempotency(_ context.Context, key, response string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.idempotency[key] = response
	return nil
}

func (m *MemoryStore) FindStaleReady(_ context.Context, before time.Time, limit int) ([]ProductTrx, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	var out []ProductTrx
	for _, p := range m.trx {
		if p.PaymentStatus == "READY" && p.SysCreationDate != nil && p.SysCreationDate.Before(before) {
			out = append(out, p)
		}
	}
	sort.Slice(out, func(i, j int) bool { return out[i].TransactionID < out[j].TransactionID })
	if len(out) > limit {
		out = out[:limit]
	}
	return out, nil
}

func (m *MemoryStore) ListUnprocessedOutbox(_ context.Context, limit int) ([]Outbox, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	var out []Outbox
	for _, o := range m.outbox {
		if o.ProcessedAt == nil {
			out = append(out, o)
		}
	}
	sort.Slice(out, func(i, j int) bool { return out[i].CreatedAt.Before(out[j].CreatedAt) })
	if len(out) > limit {
		out = out[:limit]
	}
	return out, nil
}

func (m *MemoryStore) MarkOutboxProcessed(_ context.Context, id string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	o, ok := m.outbox[id]
	if !ok {
		return errors.New("not found")
	}
	now := time.Now()
	o.ProcessedAt = &now
	m.outbox[id] = o
	return nil
}
func (m *MemoryStore) InsertOutbox(_ context.Context, ob *Outbox) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.outbox[ob.ID] = *ob
	return nil
}

// ListRecentLogs returns newest servicelogs rows (processed or not) — replaces Mongo ServiceLog reads.
func (m *MemoryStore) ListRecentLogs(_ context.Context, limit int) ([]Outbox, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	var out []Outbox
	for _, o := range m.outbox {
		if o.Topic == "servicelogs" {
			out = append(out, o)
		}
	}
	sort.Slice(out, func(i, j int) bool { return out[j].CreatedAt.Before(out[i].CreatedAt) })
	if limit > 0 && len(out) > limit {
		out = out[:limit]
	}
	return out, nil
}

// PurgeProcessedLogs deletes processed servicelogs older than before — retention replacing Mongo TTL.
// ponytail: fixed retention via LOG_RETENTION_DAYS; per-topic policies if compliance demands.
func (m *MemoryStore) PurgeProcessedLogs(_ context.Context, before time.Time) (int64, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	var n int64
	for id, o := range m.outbox {
		if o.Topic == "servicelogs" && o.ProcessedAt != nil && o.CreatedAt.Before(before) {
			delete(m.outbox, id)
			n++
		}
	}
	return n, nil
}

var demoUsers = map[string]*StoreUser{
	"klhomme0":  {ID: 1, UserID: "b2xrasd", Username: "klhomme0", FirstName: "Kimbra", LastName: "L'Homme", Email: "klhomme0@scribd.com", Password: "$2a$10$hKEM57bZ02TVXn4oYGH3C.QpYrO5OKqawNgfCg.pJyzyWxCgxG1rm", SpecialProduct: true},
	"ewhicher1": {ID: 2, UserID: "rebajea", Username: "ewhicher1", FirstName: "Elora", LastName: "Whicher", Email: "ewhicher1@gov.uk", Password: "$2a$10$jduyUW35N8uHdfwgD.R8uuSYYOD0sxF58Y9f5UpW2iCd06pQD5gYS", SpecialProduct: false},
	"admin1":    {ID: 4, UserID: "v3jjsv0", Username: "admin1", FirstName: "Admin", LastName: "User", Email: "admin1@example.com", Password: "$2a$10$TjG35AEva9aTKphO8zsLe.7tZ13OXhdQS.g4sdfrXpDIh5SLVhZt.", SpecialProduct: true},
}
var demoProducts = []Product{
	{ProductID: 3, ProductCode: "TJX-99896", ProductName: "Carbonated Water - Blackcherry", Price: 61557, Discount: 0.71, EnableDiscount: false, SpecialProduct: true, ProductStatus: true},
	{ProductID: 4, ProductCode: "IDM-44572", ProductName: "Radish - Pickled", Price: 11362, Discount: 0.3, EnableDiscount: true, SpecialProduct: false, ProductStatus: true},
	{ProductID: 6, ProductCode: "BUM-24071", ProductName: "Wine - Red, Black Opal Shiraz", Price: 45647, Discount: 0.87, EnableDiscount: true, SpecialProduct: false, ProductStatus: true},
	{ProductID: 9, ProductCode: "PAD-05945", ProductName: "Puree - Kiwi", Price: 82270, Discount: 0.89, EnableDiscount: false, SpecialProduct: true, ProductStatus: true},
}

func (m *MemoryStore) GetUserDetail(_ context.Context, username string) (*StoreUser, error) {
	if u, ok := demoUsers[username]; ok {
		cp := *u
		return &cp, nil
	}
	return nil, errors.New("not found")
}
func (m *MemoryStore) GetAllProducts(_ context.Context) ([]Product, error) {
	return append([]Product(nil), demoProducts...), nil
}
func (m *MemoryStore) GetSpecialProducts(_ context.Context, special bool) ([]Product, error) {
	var out []Product
	for _, p := range demoProducts {
		if p.SpecialProduct == special {
			out = append(out, p)
		}
	}
	return out, nil
}
func (m *MemoryStore) GetSingleProduct(_ context.Context, code string) (*Product, error) {
	for _, p := range demoProducts {
		if p.ProductCode == code {
			cp := p
			return &cp, nil
		}
	}
	return nil, errors.New("not found")
}
