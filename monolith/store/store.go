package store

import (
	"context"
	"errors"
	"time"
)

var ErrIdempotencyExists = errors.New("idempotency key already exists")

// ProductTrx mirrors transaction.product_trx — canonical row owned by TransactionLifecycle.
// Field names match 21-transaction-schema.sql.
type ProductTrx struct {
	ID              string     `json:"id"`
	SysCreationDate *time.Time `json:"sys_creation_date,omitempty"`
	TransactionID   string     `json:"transaction_id"`
	OrderStatus     string     `json:"order_status"`
	PaymentStatus   string     `json:"payment_status"`
	UserID          string     `json:"user_id"`
	ProductName     string     `json:"product_name"`
	Amount          int64      `json:"amount"`
	Price           int64      `json:"price"`
	PriceCharge     int64      `json:"price_charge"`
	ProductCode     string     `json:"product_code"`
	Param1          *string    `json:"param_1,omitempty"`
	Param2          *string    `json:"param_2,omitempty"`
	SysUpdateDate   *time.Time `json:"sys_update_date,omitempty"`
	PaymentDate     *time.Time `json:"payment_date,omitempty"`
	DiscountEnabled bool       `json:"discount_enabled"`
	Discount        float64    `json:"discount"`
}

// Outbox row — atomic with ProductTrx update, replaces Kafka+Mongo.
// ponytail: single table for both invoice + log topics; split when throughput demands per-topic partitions.
type Outbox struct {
	ID          string     `json:"id"`           // uuid
	AggregateID string     `json:"aggregate_id"` // transactionId
	Topic       string     `json:"topic"`        // ms-notify-payment | servicelogs
	Payload     []byte     `json:"payload"`
	CreatedAt   time.Time  `json:"created_at"`
	ProcessedAt *time.Time `json:"processed_at,omitempty"`
}

// Product and StoreUser mirror store.* tables for legacy compat
type Product struct {
	ProductID       int     `json:"productid"`
	ProductCode     string  `json:"productCode"`
	ProductName     string  `json:"productName"`
	Price           int     `json:"price"`
	Discount        float64 `json:"discount"`
	EnableDiscount  bool    `json:"enableDiscount"`
	SpecialProduct  bool    `json:"specialProduct"`
	ProductStatus   bool    `json:"productStatus"`
}
type StoreUser struct {
	ID             int    `json:"id"`
	UserID         string `json:"userId"`
	Username       string `json:"username"`
	FirstName      string `json:"firstName"`
	LastName       string `json:"lastName"`
	Email          string `json:"email"`
	Password       string `json:"-"` // bcrypt hash
	SpecialProduct bool   `json:"specialProduct"`
	Recurring      bool   `json:"recurring"`
}

// Store is internal seam for TransactionLifecycle — Local-substitutable.
// Prod adapter: pg.go (pgxpool). Test adapter: memory.go (map+Mutex).
type Store interface {
	FindTxForUpdate(ctx context.Context, txId string) (*ProductTrx, error)
	InsertTxWithOutbox(ctx context.Context, trx ProductTrx, outbox *Outbox, idemKey string) error
	UpdateStatusWithOutbox(ctx context.Context, txId, orderStatus, paymentStatus string, outbox *Outbox) error
	GetIdempotency(ctx context.Context, key string) (string, error)
	PutIdempotency(ctx context.Context, key, response string) error
	FindStaleReady(ctx context.Context, before time.Time, limit int) ([]ProductTrx, error)
	ListUnprocessedOutbox(ctx context.Context, limit int) ([]Outbox, error)
	MarkOutboxProcessed(ctx context.Context, id string) error
	InsertOutbox(ctx context.Context, ob *Outbox) error
	// legacy product/user for frontend compat
	GetUserDetail(ctx context.Context, username string) (*StoreUser, error)
	GetAllProducts(ctx context.Context) ([]Product, error)
	GetSpecialProducts(ctx context.Context, special bool) ([]Product, error)
	GetSingleProduct(ctx context.Context, code string) (*Product, error)
}
