package com.bwagih.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class NotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationApplication.class, args);
	}

	@GetMapping("/{name}")
	public String welcomePage(@PathVariable String name){
		return "Welcome".concat(" ").concat(name).concat(" to the welcomePage notification Service");
	}

}
