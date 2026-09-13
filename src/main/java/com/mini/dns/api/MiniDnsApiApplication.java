package com.mini.dns.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling 
public class MiniDnsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiniDnsApiApplication.class, args);
	}
	
}
