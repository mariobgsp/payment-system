package usecase

import (
	"encoding/json"
	"log"
	"paymentagr/models"
	"strings"
	"time"

	"github.com/go-resty/resty/v2"
	"github.com/google/uuid"
)

func TestServices() (rs *models.SimpleResponse) {
	res := new(models.SimpleResponse)
	// main rs
	res.Status = "Ok"
	res.Code = "00"
	res.Message = "Request being processed"
	return res
}

func ChargePayment(rq *models.ChargeRq) (rs *models.ChargeRs, err error) {
	res := new(models.ChargeRs)

	// set action
	act := new(models.Action)
	act.CheckoutUrl = strings.ReplaceAll("http://localhost:1234/payments/redirect/{refid}", "{refid}", rq.ReferenceId)

	// set channelproperties
	cp := new(models.ChannelProperties)
	cp.SuccessRedirectUrl = rq.ChannelProperties.SuccessRedirectUrl

	// main rs
	res.Id = "pgr_" + uuid.New().String()
	res.ReferenceId = rq.ReferenceId
	res.Status = "PENDING"
	res.Currency = rq.Currency
	res.CheckoutMethod = rq.CheckoutMethod
	res.Amount = rq.Amount
	res.PaymentCode = rq.PaymentCode
	res.ChannelProperties = cp
	res.CallbackUrl = "url-sample"
	res.Created = time.Now()
	res.Updated = time.Now()
	res.Action = act

	return res, nil
}

func RedirectPayment(trxid string) (rs *models.SimpleResponse, err error) {
	res := new(models.SimpleResponse)

	go func() { TriggerCallback(trxid) }()

	// main rs
	res.Status = "Ok"
	res.Code = "00"
	res.Message = "Request being processed"

	return res, nil
}

// invoking partner
func TriggerCallback(trxid string) {
	res := new(models.CallbackRq)
	sRs := new(models.SimpleResponse)

	// callback request
	res.Id = "pgr_" + uuid.New().String()
	res.ReferenceId = trxid
	res.Status = "SUCCEEDED"
	res.Updated = time.Now()

	// init resty
	client := resty.New()

	bodyPayload, err := json.Marshal(res)
	if err != nil {
		log.Print("failed parsing json request", res, err)
		return
	}

	resp, err := client.R().
		SetHeader("Content-Type", "application/json").
		SetBody(bodyPayload).
		SetResult(sRs).
		Post("http://localhost:9090/notify")
	if err != nil {
		log.Print("failed invoke partner", res, err)
		return
	}

	log.Print(resp)
}
