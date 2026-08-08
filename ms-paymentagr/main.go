package main

import (
	"paymentagr/config"
	"paymentagr/delivery"

	"github.com/gin-gonic/gin"
)

func main() {
	gin.SetMode(gin.ReleaseMode)
	router := gin.Default()

	router.GET("/health", delivery.HealthCheck)
	router.GET("/payments/redirect/:trxid", delivery.RedirectPayment)
	router.POST("/payments/charge", delivery.RequireApiKey(), delivery.ChargePayment)
	router.POST("/payments/refund", delivery.RequireApiKey(), delivery.RefundPayment)

	router.Run(":" + config.Load().Port)
}
