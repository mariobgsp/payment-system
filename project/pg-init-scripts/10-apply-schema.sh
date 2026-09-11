#!/bin/bash
set -e
set -u

# Everything lives in the single `transaction` database.
# The Go monolith uses one DSN (DATABASE_URL=.../transaction) and queries both
# `store.*` (products/users) and `transaction.*` (product_trx/outbox/idempotency)
# through that one pool, so the schemas must be created in that same database.
#
# The legacy `ms` database was retired with the Java stack: it is no longer in
# POSTGRES_MULTIPLE_DATABASES (see 00-create-databases.sh), so pointing psql at it
# fails with `FATAL: database "ms" does not exist`. Under `set -e` that aborts this
# script and silently skips every statement below it.
#
# All scripts are idempotent (IF NOT EXISTS / ON CONFLICT DO NOTHING), so re-running
# on an existing volume is safe.
echo "Applying store schema to 'transaction' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname transaction -f /docker-entrypoint-initdb.d/sql/20-store-schema.sql

echo "Applying schema to 'transaction' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname transaction -f /docker-entrypoint-initdb.d/sql/21-transaction-schema.sql
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname transaction -f /docker-entrypoint-initdb.d/sql/22-monolith-outbox.sql

echo "Schema initialization complete"
