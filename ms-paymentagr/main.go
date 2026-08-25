package main

import (
	"paymentagr/config"
	"paymentagr/delivery"
	"paymentagr/usecase"

	"github.com/gin-gonic/gin"
)

func main() {
	gin.SetMode(gin.ReleaseMode)
	cfg := config.Load()
	adapter := usecase.NewAdapter(cfg, usecase.NewRedisStore(cfg), usecase.NewHttpNotifier(cfg.NotifyURL, cfg.NotifySecret))
	delivery.SetAdapter(adapter)
	router := gin.Default()

	router.GET("/health", delivery.HealthCheck)
	router.GET("/payments/redirect/:trxid", delivery.RedirectPayment)
	router.POST("/payments/charge", delivery.RequireApiKey(), delivery.ChargePayment)
	router.POST("/payments/refund", delivery.RequireApiKey(), delivery.RefundPayment)

	router.Run(":" + cfg.Port)
}
