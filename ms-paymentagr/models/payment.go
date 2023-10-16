package models

import "time"

type ChargeRq struct {
	ReferenceId    string `json:"reference_id"`
	Currency       string `json:"currency"`
	CheckoutMethod string `json:"checkout_method"`
	Amount         int    `json:"amount"`
	PaymentCode    string `json:"payment_code"`
	RedirectUrl    string `json:"redirect_url"`
	CallbackUrl    string `json:"callback_url"`
}

type ChargeRs struct {
	Id             string              `json:"id"`
	Status         string              `json:"status"`
	Currency       string              `json:"currency"`
	CheckoutMethod string              `json:"checkout_method"`
	Amount         int                 `json:"amount"`
	PaymentCode    string              `json:"payment_code"`
	ReferenceId    string              `json:"reference_id"`
	RedirectUrl    string              `json:"redirect_url"`
	CallbackUrl    string              `json:"callback_url"`
	Created        time.Time           `json:"created"`
	Updated        time.Time           `json:"updated"`
	Action         *Action             `json:"action"`
	Internal       *AdditionalResponse `json:"-"`
}

type RefundRq struct {
	ReferenceId string `json:"reference_id"`
	Amount      int    `json:"amount"`
	Reason      string `json:"reason"`
	Currency    string `json:"currency"`
}

type RefundRs struct {
	Id           string    `json:"id"`
	ReferenceId  string    `json:"reference_id"`
	Amount       int       `json:"amount"`
	Reason       string    `json:"reason"`
	Currency     string    `json:"currency"`
	RefundStatus string    `json:"refund_status"`
	CreatedAt    time.Time `json:"created_at"`
	UpdatedAt    time.Time `json:"updated_at"`
}

type AdditionalResponse struct {
	HttpStatusCode   int    `json:"-"`
	OriginalError    error  `json:"-"`
	CompletionStatus string `json:"-"`
	StringBody       string `json:"-"`
}

type Action struct {
	CheckoutUrl string `json:"checkout_url"`
}

type SimpleResponse struct {
	Status   string              `json:"status"`
	Code     string              `json:"code"`
	Message  string              `json:"message"`
	Data     interface{}         `json:"data,omitempty"`
	Internal *AdditionalResponse `json:"-"`
}
