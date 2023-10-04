package delivery

import (
	"net/http"
	"paymentagr/usecase"

	"github.com/gin-gonic/gin"
)

func TestPayment(c *gin.Context) {
	// usecase
	rs := usecase.TestServices()
	c.IndentedJSON(http.StatusOK, rs)
}
