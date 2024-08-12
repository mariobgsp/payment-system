#!/bin/bash
set -e

# Clear the MongoDB data directory to ensure new files are created
rm -rf /data/db/*

# Execute the original MongoDB entrypoint
exec "$@"