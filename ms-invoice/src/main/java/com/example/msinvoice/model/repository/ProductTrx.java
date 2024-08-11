package com.example.msinvoice.model.repository;

import jakarta.persistence.*;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "product_trx", schema = "transaction")
public class ProductTrx {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    private String id;

    @Column(name = "sys_creation_date")
    private Date sysCreationDate;

    @Column(name = "transactionid", length = 100)
    private String transactionId;

    @Column(name = "orderstatus", length = 25)
    private String orderStatus;

    @Column(name = "paymentstatus", length = 25)
    private String paymentStatus;

    @Column(name = "userid", length = 50)
    private String userId;

    @Column(name = "productname", length = 50)
    private String productName;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "pricecharge")
    private BigDecimal priceCharge;

    @Column(name = "productcode", length = 50)
    private String productCode;

    @Column(name = "param_1", length = 50)
    private String param1;

    @Column(name = "param_2", length = 50)
    private String param2;

    @Column(name = "sys_update_date")
    private Date sysUpdateDate;

    @Column(name = "payment_date")
    private Date paymentDate;

    @Column(name = "discount_enabled")
    private Boolean discountEnabled;

    @Column(name = "discount", precision = 3, scale = 2)
    private BigDecimal discount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getSysCreationDate() {
        return sysCreationDate;
    }

    public void setSysCreationDate(Date sysCreationDate) {
        this.sysCreationDate = sysCreationDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPriceCharge() {
        return priceCharge;
    }

    public void setPriceCharge(BigDecimal priceCharge) {
        this.priceCharge = priceCharge;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getParam1() {
        return param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }

    public String getParam2() {
        return param2;
    }

    public void setParam2(String param2) {
        this.param2 = param2;
    }

    public Date getSysUpdateDate() {
        return sysUpdateDate;
    }

    public void setSysUpdateDate(Date sysUpdateDate) {
        this.sysUpdateDate = sysUpdateDate;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Boolean getDiscountEnabled() {
        return discountEnabled;
    }

    public void setDiscountEnabled(Boolean discountEnabled) {
        this.discountEnabled = discountEnabled;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }
}
