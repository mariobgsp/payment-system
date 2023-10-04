package delivery

import (
	"log"
	"net/http"
	"paymentagr/models"
	"paymentagr/usecase"

	"github.com/gin-gonic/gin"
)

func ChargePayment(c *gin.Context) {
	rq := new(models.ChargeRq)
	err := c.BindJSON(rq)
	if err != nil {
		log.Println(err)
		c.IndentedJSON(http.StatusInternalServerError, err)
		return
	}

	// usecase
	rs, err := usecase.ChargePayment(rq)
	if err != nil {
		log.Println(err)
		c.IndentedJSON(http.StatusInternalServerError, err)
		return
	}
	c.IndentedJSON(http.StatusOK, rs)
}
