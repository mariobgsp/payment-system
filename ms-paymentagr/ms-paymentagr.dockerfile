# Use the official Go image
FROM golang:1.21-alpine AS builder

# Set the Current Working Directory inside the container
WORKDIR /app

# Copy go mod and sum files
COPY go.mod go.sum ./

# Download all dependencies. Dependencies will be cached if the go.mod and go.sum files are not changed
RUN go mod download

# Copy the source code into the container
COPY . .

# Build the Go app
RUN CGO_ENABLED=0 go build -o main .

# Minimal runtime image
FROM alpine:3.19

# Run as non-root user
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app

COPY --from=builder /app/main .

USER app

EXPOSE 8081

# Start the app
ENTRYPOINT ["./main"]
