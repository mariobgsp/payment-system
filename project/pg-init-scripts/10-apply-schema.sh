#!/bin/bash
set -e
set -u

echo "Applying schema to 'ms' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname ms -f /docker-entrypoint-initdb.d/sql/20-store-schema.sql

echo "Applying schema to 'transaction' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname transaction -f /docker-entrypoint-initdb.d/sql/21-transaction-schema.sql

echo "Schema initialization complete"
