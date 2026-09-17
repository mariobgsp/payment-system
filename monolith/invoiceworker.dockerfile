FROM golang:1.25-alpine AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go vet ./invoice/... ./store/... && go build -o /invoice-worker ./invoiceworker

FROM alpine:3.20
RUN apk add --no-cache wget && adduser -D app
USER app
COPY --from=build /invoice-worker /invoice-worker
EXPOSE 8086
ENTRYPOINT ["/invoice-worker"]
