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

// bind replaces 2x BindJSON + log + 400 blocks.
func bind(c *gin.Context, rq any) bool {
	if err := c.BindJSON(rq); err != nil {
		log.Println("invalid request:", err)
		c.IndentedJSON(http.StatusBadRequest, gin.H{"message": "invalid request body"})
		return false
	}
	return true
}

func ok(c *gin.Context, v any) { c.IndentedJSON(http.StatusOK, v) }

func ChargePayment(c *gin.Context) {
	rq := new(models.ChargeRq)
	if !bind(c, rq) {
		return
	}
	if rs, err := getAdapter().Charge(c.Request.Context(), rq.ReferenceId, *rq, ""); err != nil {
		writeApiError(c, err)
	} else {
		ok(c, rs)
	}
}

func RefundPayment(c *gin.Context) {
	rq := new(models.RefundRq)
	if !bind(c, rq) {
		return
	}
	if rs, err := getAdapter().Refund(c.Request.Context(), rq.ReferenceId, *rq); err != nil {
		writeApiError(c, err)
	} else {
		ok(c, rs)
	}
}

func RedirectPayment(c *gin.Context) {
	if rs, err := getAdapter().Redirect(c.Request.Context(), c.Param("trxid")); err != nil {
		writeApiError(c, err)
	} else {
		ok(c, rs)
	}
}

func HealthCheck(c *gin.Context) {
	if rs, ok := getAdapter().Store().(*usecase.RedisStore); ok {
		if err := rs.Ping(c.Request.Context()); err != nil {
			c.IndentedJSON(http.StatusServiceUnavailable, gin.H{"status": "failed", "code": "99", "message": "redis unavailable"})
			return
		}
	}
	c.IndentedJSON(http.StatusOK, gin.H{"status": "ok", "code": "00", "message": "success-check-health"})
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
