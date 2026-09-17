# CONVENTIONS

See `specs/tech-architecture/tech-stack.md` for domain language.

## Tests

- F.I.R.S.T compliant.

## Code Style

- Functions 4-20 lines, files <300 lines, DRY, early returns.
- Module error prefixes — every user-facing error and log line names its module first:
  `LC` lifecycle, `ID` identity, `INV` invoice, `JOB` jobs. One line answers "which part failed".
- Split hatch — modules talk through narrow seams (`invoice.WriteFile(ctx, store, payload, dir)`,
  `INVOICE_URL` branch in `routeSender`) so a module can become a separate binary
  (`monolith/invoiceworker`) with no broker, no new DB, no rewrite.

## Output

- Spec artefacts in `specs/`.
