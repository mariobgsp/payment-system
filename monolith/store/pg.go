package store

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// PGStore is prod adapter — Local-substitutable, internal seam not exposed via Lifecycle Interface.
// ponytail: no retry inside store; caller (Lifecycle) owns retry/backoff.
type PGStore struct {
	pool *pgxpool.Pool
}

func NewPGStore(pool *pgxpool.Pool) *PGStore { return &PGStore{pool: pool} }

func scanProductTrxRow(row interface{ Scan(dest ...any) error }) (*ProductTrx, error) {
	var p ProductTrx
	var disc float64
	if err := row.Scan(&p.ID, &p.SysCreationDate, &p.TransactionID, &p.OrderStatus, &p.PaymentStatus, &p.UserID, &p.ProductName, &p.Amount, &p.Price, &p.PriceCharge, &p.ProductCode, &p.Param1, &p.Param2, &p.SysUpdateDate, &p.PaymentDate, &p.DiscountEnabled, &disc); err != nil {
		return nil, err
	}
	p.Discount = disc
	return &p, nil
}
func (s *PGStore) FindTxForUpdate(ctx context.Context, txId string) (*ProductTrx, error) {
	row := s.pool.QueryRow(ctx,
		`SELECT id, sys_creation_date, transactionid, orderstatus, paymentstatus, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount
		 FROM transaction.product_trx WHERE transactionid=$1 FOR UPDATE`, txId)
	return scanProductTrxRow(row)
}

func (s *PGStore) InsertTxWithOutbox(ctx context.Context, trx ProductTrx, outbox *Outbox, idemKey string) error {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	if idemKey != "" {
		tag, err := tx.Exec(ctx, `INSERT INTO transaction.idempotency(key, response, created_at) VALUES($1,'{}'::jsonb,now()) ON CONFLICT (key) DO NOTHING`, idemKey)
		if err != nil {
			return err
		}
		if tag.RowsAffected() == 0 {
			return ErrIdempotencyExists
		}
	}
	_, err = tx.Exec(ctx,
		`INSERT INTO transaction.product_trx(id, sys_creation_date, transactionid, orderstatus, paymentstatus, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount)
		 VALUES($1, now(), $2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16)`,
		trx.ID, trx.TransactionID, trx.OrderStatus, trx.PaymentStatus, trx.UserID, trx.ProductName, trx.Amount, trx.Price, trx.PriceCharge, trx.ProductCode, trx.Param1, trx.Param2, trx.SysUpdateDate, trx.PaymentDate, trx.DiscountEnabled, trx.Discount)
	if err != nil {
		return err
	}
	if outbox != nil {
		_, err = tx.Exec(ctx, `INSERT INTO transaction.outbox(id, aggregate_id, topic, payload, created_at) VALUES($1,$2,$3,$4,$5)`,
			outbox.ID, outbox.AggregateID, outbox.Topic, outbox.Payload, outbox.CreatedAt)
		if err != nil {
			return err
		}
	}
	return tx.Commit(ctx)
}

func (s *PGStore) UpdateStatusWithOutbox(ctx context.Context, txId, orderStatus, paymentStatus string, outbox *Outbox) error {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	_, err = tx.Exec(ctx, `UPDATE transaction.product_trx SET orderstatus=$1, paymentstatus=$2, sys_update_date=now(), payment_date=now() WHERE transactionid=$3`, orderStatus, paymentStatus, txId)
	if err != nil {
		return err
	}
	if outbox != nil {
		_, err = tx.Exec(ctx, `INSERT INTO transaction.outbox(id, aggregate_id, topic, payload, created_at) VALUES($1,$2,$3,$4,$5)`,
			outbox.ID, outbox.AggregateID, outbox.Topic, outbox.Payload, outbox.CreatedAt)
		if err != nil {
			return err
		}
	}
	return tx.Commit(ctx)
}

func (s *PGStore) GetIdempotency(ctx context.Context, key string) (string, error) {
	var resp string
	err := s.pool.QueryRow(ctx, `SELECT response::text FROM transaction.idempotency WHERE key=$1`, key).Scan(&resp)
	return resp, err
}

func (s *PGStore) PutIdempotency(ctx context.Context, key, response string) error {
	if response == "" {
		response = "{}"
	}
	_, err := s.pool.Exec(ctx, `INSERT INTO transaction.idempotency(key, response, created_at) VALUES($1,$2::jsonb,now()) ON CONFLICT (key) DO UPDATE SET response=EXCLUDED.response`, key, response)
	return err
}

func (s *PGStore) FindStaleReady(ctx context.Context, before time.Time, limit int) ([]ProductTrx, error) {
	rows, err := s.pool.Query(ctx,
		`SELECT id, sys_creation_date, transactionid, orderstatus, paymentstatus, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount
		 FROM transaction.product_trx WHERE paymentstatus='READY' AND sys_creation_date < $1 LIMIT $2`, before, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []ProductTrx
	for rows.Next() {
		p, err := scanProductTrxRow(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, *p)
	}
	return out, rows.Err()
}

func (s *PGStore) ListUnprocessedOutbox(ctx context.Context, limit int) ([]Outbox, error) {
	rows, err := s.pool.Query(ctx, `SELECT id, aggregate_id, topic, payload, created_at FROM transaction.outbox WHERE processed_at IS NULL ORDER BY created_at LIMIT $1`, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []Outbox
	for rows.Next() {
		var o Outbox
		if err := rows.Scan(&o.ID, &o.AggregateID, &o.Topic, &o.Payload, &o.CreatedAt); err != nil {
			return nil, err
		}
		out = append(out, o)
	}
	return out, rows.Err()
}

func (s *PGStore) MarkOutboxProcessed(ctx context.Context, id string) error {
	_, err := s.pool.Exec(ctx, `UPDATE transaction.outbox SET processed_at=now() WHERE id=$1`, id)
	return err
}
func (s *PGStore) InsertOutbox(ctx context.Context, ob *Outbox) error {
	_, err := s.pool.Exec(ctx, `INSERT INTO transaction.outbox(id, aggregate_id, topic, payload, created_at) VALUES($1,$2,$3,$4,$5)`, ob.ID, ob.AggregateID, ob.Topic, ob.Payload, ob.CreatedAt)
	return err
}

func (s *PGStore) ListRecentLogs(ctx context.Context, limit int) ([]Outbox, error) {
	rows, err := s.pool.Query(ctx, `SELECT id, aggregate_id, topic, payload, created_at FROM transaction.outbox WHERE topic='servicelogs' ORDER BY created_at DESC LIMIT $1`, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []Outbox
	for rows.Next() {
		var o Outbox
		if err := rows.Scan(&o.ID, &o.AggregateID, &o.Topic, &o.Payload, &o.CreatedAt); err != nil {
			return nil, err
		}
		out = append(out, o)
	}
	return out, rows.Err()
}

func (s *PGStore) PurgeProcessedLogs(ctx context.Context, before time.Time) (int64, error) {
	tag, err := s.pool.Exec(ctx, `DELETE FROM transaction.outbox WHERE topic='servicelogs' AND processed_at IS NOT NULL AND created_at < $1`, before)
	if err != nil {
		return 0, err
	}
	return tag.RowsAffected(), nil
}

func (s *PGStore) GetUserDetail(ctx context.Context, username string) (*StoreUser, error) {
	row := s.pool.QueryRow(ctx, `SELECT id, userid, username, firstname, lastname, email, password, specialproduct, recurring FROM store.store_user WHERE username=$1`, username)
	var u StoreUser
	if err := row.Scan(&u.ID, &u.UserID, &u.Username, &u.FirstName, &u.LastName, &u.Email, &u.Password, &u.SpecialProduct, &u.Recurring); err != nil {
		return nil, err
	}
	return &u, nil
}
func scanProductRow(row interface{ Scan(dest ...any) error }) (Product, error) {
	var p Product
	err := row.Scan(&p.ProductID, &p.ProductCode, &p.ProductName, &p.Price, &p.Discount, &p.EnableDiscount, &p.SpecialProduct, &p.ProductStatus)
	return p, err
}
func (s *PGStore) GetAllProducts(ctx context.Context) ([]Product, error) {
	rows, err := s.pool.Query(ctx, `SELECT productid, productcode, productname, price, discount, enablediscount, specialproduct, productstatus FROM store.product`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []Product
	for rows.Next() {
		p, err := scanProductRow(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, p)
	}
	return out, rows.Err()
}
func (s *PGStore) GetSpecialProducts(ctx context.Context, special bool) ([]Product, error) {
	rows, err := s.pool.Query(ctx, `SELECT productid, productcode, productname, price, discount, enablediscount, specialproduct, productstatus FROM store.product WHERE specialproduct=$1 AND productstatus=true`, special)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []Product
	for rows.Next() {
		p, err := scanProductRow(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, p)
	}
	return out, rows.Err()
}
func (s *PGStore) GetSingleProduct(ctx context.Context, code string) (*Product, error) {
	row := s.pool.QueryRow(ctx, `SELECT productid, productcode, productname, price, discount, enablediscount, specialproduct, productstatus FROM store.product WHERE productcode=$1 AND productstatus=true`, code)
	var p Product
	if err := row.Scan(&p.ProductID, &p.ProductCode, &p.ProductName, &p.Price, &p.Discount, &p.EnableDiscount, &p.SpecialProduct, &p.ProductStatus); err != nil {
		return nil, err
	}
	return &p, nil
}
