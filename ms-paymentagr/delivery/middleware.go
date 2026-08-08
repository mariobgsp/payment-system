package delivery

import (
	"crypto/hmac"
	"net/http"
	"paymentagr/usecase"

	"github.com/gin-gonic/gin"
)

func RequireApiKey() gin.HandlerFunc {
	return func(c *gin.Context) {
		expected := usecase.Config().ApiKey
		if expected == "" {
			expected = "change-me-api-key"
		}
		got := c.GetHeader("api-key")
		if !hmac.Equal([]byte(got), []byte(expected)) {
			c.IndentedJSON(http.StatusUnauthorized, gin.H{
				"status":  "failed",
				"code":    "41",
				"message": "unauthorized",
			})
			c.Abort()
			return
		}
		c.Next()
	}
}
