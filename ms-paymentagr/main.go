package main

import (
	"paymentagr/delivery"

	"github.com/gin-gonic/gin"
)

func main() {
	router := gin.Default()
	router.GET("payments/test", delivery.TestPayment)
	router.POST("payments/charge", delivery.ChargePayment)
	router.POST("payments/refund", delivery.RefundPayment)
	router.GET("payments/redirect/:trxid", delivery.RedirectPayment)

	router.Run(":8081")
}
