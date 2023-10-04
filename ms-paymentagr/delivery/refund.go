package delivery

import (
	"log"
	"net/http"
	"paymentagr/models"
	"paymentagr/usecase"

	"github.com/gin-gonic/gin"
)

func RefundPayment(c *gin.Context) {
	rq := new(models.RefundRq)
	err := c.BindJSON(rq)
	if err != nil {
		log.Println(err)
		c.IndentedJSON(http.StatusInternalServerError, err)
		return
	}

	// usecase
	rs, err := usecase.RefundPayment(rq)
	if err != nil {
		log.Println(err)
		c.IndentedJSON(http.StatusInternalServerError, err)
		return
	}
	c.IndentedJSON(http.StatusOK, rs)
}
