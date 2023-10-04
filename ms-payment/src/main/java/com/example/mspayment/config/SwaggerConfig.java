package com.example.mspayment.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {


	@Value("${spring.profiles.active:}")
	private String activeProfiles;

	@Bean
	public OpenAPI springShopOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("ms-payment")
						.version(activeProfiles)
				)
				.externalDocs(new ExternalDocumentation()
						.description("Github - mariobgsp - Documentation")
						.url("https://github.com/mariobgsp/ms-java"));
	}

}
