// invoice-worker is the middle-path split hatch: same repo, same packages
// (invoice, store) as the monolith, but a separate deployable. The monolith's
// routeSender already branches on INVOICE_URL — point it at this service and
// invoice generation moves out-of-process with no broker, no new DB, no rewrite.
//
// Default compose does NOT run it (single binary). Enable with:
//
//	docker compose -f docker-compose.yml -f docker-compose.split.yml up --build
package main

import (
	"context"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"os"

	"payment-system/monolith/invoice"
	"payment-system/monolith/store"

	"github.com/jackc/pgx/v5/pgxpool"
)

func main() {
	ctx := context.Background()
	s := mustStore(ctx)
	dir := os.Getenv("INVOICE_DIR")
	if dir == "" {
		dir = "/tmp/invoices"
	}
	mux := newMux(s, dir)
	port := os.Getenv("PORT")
	if port == "" {
		port = "8086"
	}
	log.Printf("INV: invoice-worker listening :%s dir=%s", port, dir)
	log.Fatal(http.ListenAndServe(":"+port, mux))
}

func newMux(s store.Store, dir string) *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /invoice", func(w http.ResponseWriter, r *http.Request) {
		raw, _ := io.ReadAll(r.Body)
		path, err := invoice.WriteFile(r.Context(), s, raw, dir)
		if err != nil {
			log.Printf("INV: worker failed: %v", err)
			w.Header().Set("content-type", "application/json")
			w.WriteHeader(500)
			_ = json.NewEncoder(w).Encode(map[string]any{"code": "99", "status": "failed", "message": err.Error()})
			return
		}
		log.Printf("INV: worker wrote %s", path)
		w.Header().Set("content-type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]any{"code": "00", "status": "ok", "data": map[string]any{"path": path}})
	})
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("content-type", "application/json")
		w.Write([]byte(`{"status":"ok","code":"00"}`))
	})
	return mux
}

func mustStore(ctx context.Context) store.Store {
	if dsn := os.Getenv("DATABASE_URL"); dsn != "" {
		pool, err := pgxpool.New(ctx, dsn)
		if err != nil {
			log.Fatalf("INV: pg pool: %v", err)
		}
		if err := pool.Ping(ctx); err != nil {
			log.Fatalf("INV: pg ping: %v", err)
		}
		return store.NewPGStore(pool)
	}
	log.Print("INV: DATABASE_URL empty → MemoryStore (dev only, no cross-process state)")
	return store.NewMemoryStore()
}
