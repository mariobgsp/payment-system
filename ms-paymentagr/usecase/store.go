package usecase

import (
	"context"
	"errors"
	"time"

	"paymentagr/config"

	redisDriver "github.com/redis/go-redis/v9"
)

var errNotFound = errors.New("not found")

// Store seam — where behavior can be altered without editing in place
type Store interface {
	Set(ctx context.Context, key string, val []byte, ttl time.Duration) error
	Get(ctx context.Context, key string) (string, error)
}

// RedisStore prod adapter
type RedisStore struct {
	client *redisDriver.Client
}

func NewRedisStore(cfg *config.Config) *RedisStore {
	return &RedisStore{client: redisDriver.NewClient(&redisDriver.Options{
		Addr:     cfg.RedisHost,
		Password: cfg.RedisPassword,
		DB:       0,
	})}
}
func (r *RedisStore) Set(ctx context.Context, key string, val []byte, ttl time.Duration) error {
	return r.client.Set(ctx, key, val, ttl).Err()
}
func (r *RedisStore) Get(ctx context.Context, key string) (string, error) {
	return r.client.Get(ctx, key).Result()
}
func (r *RedisStore) Ping(ctx context.Context) error {
	_, err := r.client.Ping(ctx).Result()
	return err
}
func (r *RedisStore) Client() *redisDriver.Client { return r.client }

// MemoryStore fake adapter for tests
type MemoryStore struct {
	data map[string]string
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{data: make(map[string]string)}
}
func (m *MemoryStore) Set(_ context.Context, key string, val []byte, _ time.Duration) error {
	m.data[key] = string(val)
	return nil
}
func (m *MemoryStore) Get(_ context.Context, key string) (string, error) {
	v, ok := m.data[key]
	if !ok {
		return "", errNotFound
	}
	return v, nil
}
