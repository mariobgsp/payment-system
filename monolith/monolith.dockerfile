FROM golang:1.25-alpine AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go vet ./... && go test ./... -count=1 && go build -o /monolith ./cmd

FROM alpine:3.20
RUN apk add --no-cache wget && adduser -D app
USER app
COPY --from=build /monolith /monolith
EXPOSE 8085
ENTRYPOINT ["/monolith"]
