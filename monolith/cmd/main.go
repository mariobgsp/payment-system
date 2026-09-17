package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"payment-system/monolith/identity"
	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"

	"github.com/jackc/pgx/v5/pgxpool"
)

// ponytail: net/http stdlib, no gin dep — native platform covers it.

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	s := mustStore(ctx)
	svc := lifecycle.New(s)
	idSvc := identity.NewIdentity(s, 1800*time.Second, 10, 15*time.Minute, 300*time.Second, nil, os.Getenv("APP_SECRET_KEY"))

	go runSweeper(ctx, s)
	go runOutboxPoller(ctx, s)

	port := env("PORT", "8080")
	log.Printf("monolith listening :%s", port)
	srv := &http.Server{Addr: ":" + port, Handler: withRequestID(newMux(svc, idSvc, s))}
	go func() {
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal(err)
		}
	}()
	<-ctx.Done()
	shut, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_ = srv.Shutdown(shut)
}

func mustStore(ctx context.Context) store.Store {
	if dsn := os.Getenv("DATABASE_URL"); dsn != "" {
		pool, err := pgxpool.New(ctx, dsn)
		if err != nil {
			log.Fatalf("pg pool: %v", err)
		}
		if err := pool.Ping(ctx); err != nil {
			log.Fatalf("pg ping: %v", err)
		}
		return store.NewPGStore(pool)
	}
	if os.Getenv("ENV") == "prod" || os.Getenv("APP_ENV") == "prod" {
		log.Fatal("DATABASE_URL required in prod")
	}
	log.Print("DATABASE_URL empty → MemoryStore (dev, local-substitutable fake)")
	return store.NewMemoryStore()
}
