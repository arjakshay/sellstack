package com.stack.sellstack;

import com.stack.sellstack.security.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CorsProperties.class)
public class SellstackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SellstackApplication.class, args);
	}

}
