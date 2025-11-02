package com.thexperience.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.thexperience.backend",
		"com.thexperience.backend.api"
})
public class TheXperienceBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(TheXperienceBackApplication.class, args);
	}

}
