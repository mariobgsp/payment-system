package usecase

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"paymentagr/models"
	"strings"
	"time"

	"github.com/go-resty/resty/v2"
	"github.com/google/uuid"
	redisDriver "github.com/redis/go-redis/v9"
)

var host = "localhost:6379"
var redisPassword = "my-password"

func NewRedisClient(host string, password string) *redisDriver.Client {
	client := redisDriver.NewClient(&redisDriver.Options{
		Addr:     host,
		Password: password,
		DB:       0,
	})
	return client
}

func SetData(rdc *redisDriver.Client, key string, data interface{}, ttl time.Duration) error {
	dataSet := rdc.Set(context.Background(), key, data, ttl)
	return dataSet.Err()
}

func GetData(rdc *redisDriver.Client, key string) (interface{}, error) {
	dataGet := rdc.Get(context.Background(), key)

	if dataGet.Err() != nil {
		fmt.Printf("Invalid Transaction : %v", dataGet.Err())
		return "", dataGet.Err()
	}
	resp, err := dataGet.Result()
	return resp, err
}

func TestServices() (rs *models.SimpleResponse) {
	res := new(models.SimpleResponse)

	// get to redis - redisClient
	rdc := NewRedisClient(host, redisPassword)

	data, err := GetData(rdc, "chargetrx_AXISNETXSHOPEEPAY-20230912-32863549")
	if err != nil {
		fmt.Println("redis not exist")

		res.Status = "internal-error"
		res.Code = "99"
		res.Message = "redist not exist"
		return res
	}

	// convert to string
	strData := fmt.Sprintf("%v", data)
	// convert to struct
	var chargeRs *models.ChargeRs
	json.Unmarshal([]byte(strData), &chargeRs)

	res.Status = "Ok"
	res.Code = "00"
	res.Message = "Request being processed"
	res.Data = chargeRs
	return res
}

func ChargePayment(rq *models.ChargeRq) (rs *models.ChargeRs, err error) {
	res := new(models.ChargeRs)

	// set action
	act := new(models.Action)
	act.CheckoutUrl = strings.ReplaceAll("http://127.0.0.1:1234/payments/redirect/{refid}", "{refid}", rq.ReferenceId)

	// main rs
	res.Id = "pgr_" + uuid.New().String()
	res.ReferenceId = rq.ReferenceId
	res.Status = "PENDING"
	res.Currency = rq.Currency
	res.CheckoutMethod = rq.CheckoutMethod
	res.Amount = rq.Amount
	res.PaymentCode = rq.PaymentCode
	res.RedirectUrl = rq.RedirectUrl
	res.CallbackUrl = rq.CallbackUrl
	res.Created = time.Now()
	res.Updated = time.Now()
	res.Action = act

	bodyPayload, err := json.Marshal(res)
	if err != nil {
		log.Print("failed parsing json request", res, err)
		return
	}

	// save to redis - redisClient
	rdc := NewRedisClient(host, redisPassword)

	key := "chargetrx_" + rq.ReferenceId
	ttl := time.Duration(300) * time.Second

	err = SetData(rdc, key, bodyPayload, ttl)
	if err != nil {
		log.Print("set data to redis failed", err)
	}

	return res, nil
}

func RefundPayment(rq *models.RefundRq) (rs *models.RefundRs, err error) {
	res := new(models.RefundRs)

	res.Id = "pgr_" + uuid.New().String()
	res.ReferenceId = rq.ReferenceId
	res.Amount = rq.Amount
	res.Reason = rq.Reason
	res.RefundStatus = "REFUND-PENDING"
	res.Currency = rq.Currency
	res.CreatedAt = time.Now()
	res.UpdatedAt = time.Now()

	opKey := "refundtrx_" + rq.ReferenceId

	go func() { TriggerCallback(opKey) }()

	return res, nil
}

func RedirectPayment(trxid string) (rs *models.SimpleResponse, err error) {
	res := new(models.SimpleResponse)

	opKey := "chargetrx_" + trxid

	go func() { TriggerCallback(opKey) }()

	// main rs
	res.Status = "Ok"
	res.Code = "00"
	res.Message = "Request being processed"

	return res, nil
}

// invoking partner
func TriggerCallback(opKey string) {
	res := new(models.ChargeRs)
	sRs := new(models.SimpleResponse)
	var rqPayload []byte
	client := resty.New()

	opKeyStr := strings.Split(opKey, "_")

	if opKeyStr[0] == "chargetrx" {

		// get to redis - redisClient
		rdc := NewRedisClient(host, redisPassword)

		data, err := GetData(rdc, opKey)
		if err != nil {
			fmt.Println("redis not exist")
			return
		}

		// convert to string
		strData := fmt.Sprintf("%v", data)
		// convert to struct
		var chargeRs *models.ChargeRs
		json.Unmarshal([]byte(strData), &chargeRs)

		chargeRs.Status = "SUCCEEDED"
		chargeRs.Updated = time.Now()

		bodyPayload, err := json.Marshal(chargeRs)
		if err != nil {
			log.Print("failed parsing json request", chargeRs, err)
			return
		}
		rqPayload = bodyPayload

	} else if opKeyStr[0] == "refundtrx" {

		// get to redis - redisClient
		rdc := NewRedisClient(host, redisPassword)

		data, err := GetData(rdc, "chargetrx_"+opKeyStr[1])
		if err != nil {
			fmt.Println("redis not exist")
			return
		}

		// convert to string
		strData := fmt.Sprintf("%v", data)
		// convert to struct
		var chargeRs *models.ChargeRs
		json.Unmarshal([]byte(strData), &chargeRs)

		chargeRs.Status = "SUCCESS-REFUND"
		chargeRs.Updated = time.Now()

		bodyPayload, err := json.Marshal(chargeRs)
		if err != nil {
			log.Print("failed parsing json request", chargeRs, err)
			return
		}
		rqPayload = bodyPayload
	}

	resp, err := client.R().
		SetHeader("Content-Type", "application/json").
		SetBody(rqPayload).
		SetResult(sRs).
		Post("http://127.0.0.1:9090/ms/api/v1/payment/notify")
	if err != nil {
		log.Print("failed invoke partner", res, err)
		return
	}

	// callback request
	log.Print(resp)
}
