package delivery

import (
	"errors"
	"log"
	"net/http"
	"paymentagr/config"
	"paymentagr/models"
	"paymentagr/usecase"

	"github.com/gin-gonic/gin"
)

var adapter *usecase.Adapter

func SetAdapter(a *usecase.Adapter) { adapter = a }
func getAdapter() *usecase.Adapter {
	if adapter != nil {
		return adapter
	}
	cfg := config.Load()
	return usecase.NewAdapter(cfg, usecase.NewRedisStore(cfg), usecase.NewHttpNotifier(cfg.NotifyURL, cfg.NotifySecret))
}

func ChargePayment(c *gin.Context) {
	rq := new(models.ChargeRq)
	if err := c.BindJSON(rq); err != nil {
		log.Println("invalid charge request:", err)
		c.IndentedJSON(http.StatusBadRequest, gin.H{"message": "invalid request body"})
		return
	}
	a := getAdapter()
	rs, err := a.Charge(c.Request.Context(), rq.ReferenceId, *rq, "")
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
	a := getAdapter()
	rs, err := a.Refund(c.Request.Context(), rq.ReferenceId, *rq)
	if err != nil {
		writeApiError(c, err)
		return
	}
	c.IndentedJSON(http.StatusOK, rs)
}

func RedirectPayment(c *gin.Context) {
	trxid := c.Param("trxid")
	a := getAdapter()
	rs, err := a.Redirect(c.Request.Context(), trxid)
	if err != nil {
		writeApiError(c, err)
		return
	}
	c.IndentedJSON(http.StatusOK, rs)
}

func HealthCheck(c *gin.Context) {
	a := getAdapter()
	if rs, ok := a.Store().(*usecase.RedisStore); ok {
		if err := rs.Ping(c.Request.Context()); err != nil {
			c.IndentedJSON(http.StatusServiceUnavailable, gin.H{
				"status":  "failed",
				"code":    "99",
				"message": "redis unavailable",
			})
			return
		}
	}
	// ponytail: fixed 5m TTL health — no per-request TTL knob
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
