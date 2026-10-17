package com.swe307.pro2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling 
public class Pro2Application {

	public static void main(String[] args) {
		SpringApplication.run(Pro2Application.class, args);
	}

}
