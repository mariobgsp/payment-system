package config

import (
	"os"
	"strconv"
)

type Config struct {
	Port          string
	RedisHost     string
	RedisPassword string
	NotifyURL     string
	PublicBaseURL string
	ApiKey        string
	NotifySecret  string
	RedisTTL      int
}

func Load() *Config {
	return &Config{
		Port:          getEnv("PORT", "8081"),
		RedisHost:     getEnv("REDIS_HOST", "redis:6379"),
		RedisPassword: os.Getenv("REDIS_PASSWORD"),
		NotifyURL:     getEnv("NOTIFY_URL", "http://ms-payment:9090/ms/api/v1/payment/notify"),
		PublicBaseURL: getEnv("PUBLIC_BASE_URL", "http://localhost:8081"),
		ApiKey:        getEnv("PARTNER_API_KEY", "change-me-api-key"),
		NotifySecret:  getEnv("NOTIFY_SECRET", "change-me-notify-secret"),
		RedisTTL:      getEnvInt("REDIS_TTL_SECONDS", 300),
	}
}

func getEnv(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func getEnvInt(key string, def int) int {
	if v := os.Getenv(key); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
	}
	return def
}
