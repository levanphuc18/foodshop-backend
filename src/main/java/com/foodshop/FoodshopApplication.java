package com.foodshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FoodshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodshopApplication.class, args);
	}

}
