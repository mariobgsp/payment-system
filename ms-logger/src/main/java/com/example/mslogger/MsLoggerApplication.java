package com.example.mslogger;

import java.util.Date;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import jakarta.annotation.PostConstruct;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class MsLoggerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsLoggerApplication.class, args);
	}

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+7:00"));
		System.out.println("Spring boot application running in WIB timezone :" + new Date());
	}
}
