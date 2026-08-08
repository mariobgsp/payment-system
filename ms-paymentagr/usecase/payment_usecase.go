package usecase

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"paymentagr/config"
	"paymentagr/models"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/go-resty/resty/v2"
	"github.com/google/uuid"
	redisDriver "github.com/redis/go-redis/v9"
)

var (
	cfgOnce sync.Once
	cfg     *config.Config
	rdcOnce sync.Once
	rdc     *redisDriver.Client
)

var referenceIdPattern = regexp.MustCompile(`^[A-Za-z0-9\-_.]{3,64}$`)

func Config() *config.Config {
	cfgOnce.Do(func() { cfg = config.Load() })
	return cfg
}

func Redis() *redisDriver.Client {
	rdcOnce.Do(func() {
		rdc = redisDriver.NewClient(&redisDriver.Options{
			Addr:     Config().RedisHost,
			Password: Config().RedisPassword,
			DB:       0,
		})
	})
	return rdc
}

func SetData(key string, data interface{}, ttl time.Duration) error {
	return Redis().Set(context.Background(), key, data, ttl).Err()
}

func GetData(key string) (string, error) {
	return Redis().Get(context.Background(), key).Result()
}

func SignPayload(secret string, timestamp string, payload []byte) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(timestamp))
	mac.Write(payload)
	return hex.EncodeToString(mac.Sum(nil))
}

func validateCharge(rq *models.ChargeRq) error {
	if rq.ReferenceId == "" || !referenceIdPattern.MatchString(rq.ReferenceId) {
		return errors.New("invalid or missing reference_id")
	}
	if rq.Amount <= 0 || rq.Amount > 1000000000 {
		return errors.New("invalid amount, must be between 1 and 1000000000")
	}
	if rq.Currency == "" {
		return errors.New("missing currency")
	}
	if rq.PaymentCode == "" {
		return errors.New("missing payment_code")
	}
	if rq.CheckoutMethod == "" {
		return errors.New("missing checkout_method")
	}
	return nil
}

func ChargePayment(rq *models.ChargeRq) (*models.ChargeRs, error) {
	if err := validateCharge(rq); err != nil {
		return nil, models.NewBadRequest(err.Error())
	}

	checkoutUrl := fmt.Sprintf("%s/payments/redirect/%s", Config().PublicBaseURL, rq.ReferenceId)

	res := &models.ChargeRs{
		Id:             "pgr_" + uuid.New().String(),
		ReferenceId:    rq.ReferenceId,
		Status:         "PENDING",
		Currency:       strings.ToUpper(rq.Currency),
		CheckoutMethod: rq.CheckoutMethod,
		Amount:         rq.Amount,
		PaymentCode:    rq.PaymentCode,
		RedirectUrl:    rq.RedirectUrl,
		CallbackUrl:    rq.CallbackUrl,
		Created:        time.Now(),
		Updated:        time.Now(),
		Action:         &models.Action{CheckoutUrl: checkoutUrl},
	}

	bodyPayload, err := json.Marshal(res)
	if err != nil {
		log.Print("failed parsing json request", res, err)
		return nil, models.NewInternalError("failed to process charge")
	}

	key := "chargetrx_" + rq.ReferenceId
	ttl := time.Duration(Config().RedisTTL) * time.Second

	if err := SetData(key, bodyPayload, ttl); err != nil {
		log.Print("set data to redis failed", err)
		return nil, models.NewInternalError("failed to store charge")
	}

	return res, nil
}

func RefundPayment(rq *models.RefundRq) (*models.RefundRs, error) {
	if rq.ReferenceId == "" || !referenceIdPattern.MatchString(rq.ReferenceId) {
		return nil, models.NewBadRequest("invalid or missing reference_id")
	}
	if rq.Amount <= 0 {
		return nil, models.NewBadRequest("invalid amount, must be greater than 0")
	}
	if rq.Currency == "" {
		return nil, models.NewBadRequest("missing currency")
	}

	charge, err := GetCharge(rq.ReferenceId)
	if err != nil {
		return nil, models.NewNotFound("charge not found for reference_id")
	}
	if charge.Status != "SUCCEEDED" {
		return nil, models.NewBadRequest("charge is not in a refundable state")
	}

	res := &models.RefundRs{
		Id:           "pgr_" + uuid.New().String(),
		ReferenceId:  rq.ReferenceId,
		Amount:       rq.Amount,
		Reason:       rq.Reason,
		RefundStatus: "REFUND-PENDING",
		Currency:     strings.ToUpper(rq.Currency),
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
	}

	if err := SetData("refundtrx_"+rq.ReferenceId, res, time.Duration(Config().RedisTTL)*time.Second); err != nil {
		log.Print("set refund data to redis failed", err)
		return nil, models.NewInternalError("failed to store refund")
	}

	go TriggerCallback("refundtrx_" + rq.ReferenceId)

	return res, nil
}

func RedirectPayment(trxid string) (*models.SimpleResponse, error) {
	if trxid == "" || !referenceIdPattern.MatchString(trxid) {
		return nil, models.NewBadRequest("invalid or missing transaction id")
	}

	charge, err := GetCharge(trxid)
	if err != nil {
		return nil, models.NewNotFound("charge not found")
	}
	if charge.Status == "SUCCEEDED" {
		return &models.SimpleResponse{Status: "Ok", Code: "00", Message: "Payment already confirmed"}, nil
	}

	go TriggerCallback("chargetrx_" + trxid)

	return &models.SimpleResponse{Status: "Ok", Code: "00", Message: "Request being processed"}, nil
}

func GetCharge(refId string) (*models.ChargeRs, error) {
	data, err := GetData("chargetrx_" + refId)
	if err != nil {
		return nil, err
	}
	var charge models.ChargeRs
	if err := json.Unmarshal([]byte(data), &charge); err != nil {
		return nil, err
	}
	return &charge, nil
}

func TriggerCallback(opKey string) {
	parts := strings.SplitN(opKey, "_", 2)
	if len(parts) < 2 {
		return
	}

	charge, err := GetCharge(parts[1])
	if err != nil {
		log.Printf("callback skipped, charge not found: %v", err)
		return
	}

	switch parts[0] {
	case "chargetrx":
		charge.Status = "SUCCEEDED"
	case "refundtrx":
		charge.Status = "SUCCESS-REFUND"
	default:
		return
	}
	charge.Updated = time.Now()

	payload, err := json.Marshal(charge)
	if err != nil {
		log.Print("failed parsing callback payload", err)
		return
	}

	sendCallback(payload)
}

func sendCallback(payload []byte) {
	timestamp := strconv.FormatInt(time.Now().Unix(), 10)
	signature := SignPayload(Config().NotifySecret, timestamp, payload)

	resp, err := resty.New().SetTimeout(15 * time.Second).R().
		SetHeader("Content-Type", "application/json").
		SetHeader("x-callback-signature", signature).
		SetHeader("x-callback-timestamp", timestamp).
		SetBody(payload).
		Post(Config().NotifyURL)
	if err != nil {
		log.Printf("failed invoke notify: %v", err)
		return
	}
	log.Printf("notify response status: %d, body: %s", resp.StatusCode(), resp.String())
}
