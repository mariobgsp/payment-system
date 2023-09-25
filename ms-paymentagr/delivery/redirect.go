package delivery

import (
	"log"
	"net/http"
	"paymentagr/usecase"

	"github.com/gin-gonic/gin"
)

func RedirectPayment(c *gin.Context) {
	trxid := c.Param("trxid")

	// usecase
	rs, err := usecase.RedirectPayment(trxid)
	if err != nil {
		log.Println(err)
		rs.Message = err.Error()
		c.IndentedJSON(http.StatusInternalServerError, rs)
		return
	} else {
		c.IndentedJSON(http.StatusOK, rs)
	}

}
