package com.example.mspayment;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Date;
import java.util.TimeZone;

@SpringBootApplication
public class MsPaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsPaymentApplication.class, args);
	}

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+7:00"));
		System.out.println("Spring boot application running in WIB timezone :" + new Date());
	}
}
