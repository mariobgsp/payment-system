package models

import (
	"net/http"
	"time"
)

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
	Id             string    `json:"id"`
	Status         string    `json:"status"`
	Currency       string    `json:"currency"`
	CheckoutMethod string    `json:"checkout_method"`
	Amount         int       `json:"amount"`
	PaymentCode    string    `json:"payment_code"`
	ReferenceId    string    `json:"reference_id"`
	RedirectUrl    string    `json:"redirect_url"`
	CallbackUrl    string    `json:"callback_url"`
	Created        time.Time `json:"created"`
	Updated        time.Time `json:"updated"`
	Action         *Action   `json:"action"`
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

type Action struct {
	CheckoutUrl string `json:"checkout_url"`
}

type SimpleResponse struct {
	Status  string      `json:"status"`
	Code    string      `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
}

type ApiError struct {
	Code    int    `json:"-"`
	Message string `json:"message"`
}

func (e *ApiError) Error() string {
	return e.Message
}

func NewBadRequest(msg string) *ApiError {
	return &ApiError{Code: http.StatusBadRequest, Message: msg}
}

func NewNotFound(msg string) *ApiError {
	return &ApiError{Code: http.StatusNotFound, Message: msg}
}

func NewInternalError(msg string) *ApiError {
	return &ApiError{Code: http.StatusInternalServerError, Message: msg}
}
