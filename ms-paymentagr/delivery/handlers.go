package delivery

import (
	"errors"
	"log"
	"net/http"
	"paymentagr/models"
	"paymentagr/usecase"

	"github.com/gin-gonic/gin"
)

func ChargePayment(c *gin.Context) {
	rq := new(models.ChargeRq)
	if err := c.BindJSON(rq); err != nil {
		log.Println("invalid charge request:", err)
		c.IndentedJSON(http.StatusBadRequest, gin.H{"message": "invalid request body"})
		return
	}

	rs, err := usecase.ChargePayment(rq)
	if err != nil {
		writeApiError(c, err)
		return
	}
	c.IndentedJSON(http.StatusOK, rs)
}

func RefundPayment(c *gin.Context) {
	rq := new(models.RefundRq)
	if err := c.BindJSON(rq); err != nil {
		log.Println("invalid refund request:", err)
		c.IndentedJSON(http.StatusBadRequest, gin.H{"message": "invalid request body"})
		return
	}

	rs, err := usecase.RefundPayment(rq)
	if err != nil {
		writeApiError(c, err)
		return
	}
	c.IndentedJSON(http.StatusOK, rs)
}

func RedirectPayment(c *gin.Context) {
	trxid := c.Param("trxid")

	rs, err := usecase.RedirectPayment(trxid)
	if err != nil {
		writeApiError(c, err)
		return
	}
	c.IndentedJSON(http.StatusOK, rs)
}

func HealthCheck(c *gin.Context) {
	rdc := usecase.Redis()
	if _, err := rdc.Ping(c.Request.Context()).Result(); err != nil {
		c.IndentedJSON(http.StatusServiceUnavailable, gin.H{
			"status":  "failed",
			"code":    "99",
			"message": "redis unavailable",
		})
		return
	}
	c.IndentedJSON(http.StatusOK, gin.H{
		"status":  "ok",
		"code":    "00",
		"message": "success-check-health",
	})
}

func writeApiError(c *gin.Context, err error) {
	var apiErr *models.ApiError
	if errors.As(err, &apiErr) {
		c.IndentedJSON(apiErr.Code, apiErr)
		return
	}
	log.Println("unexpected error:", err)
	c.IndentedJSON(http.StatusInternalServerError, gin.H{"message": "internal server error"})
}
