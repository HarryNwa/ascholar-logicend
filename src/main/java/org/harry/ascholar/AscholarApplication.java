package org.harry.ascholar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EntityScan(basePackages = "org.harry.ascholar.data.models")
public class AscholarApplication {

	public static void main(String[] args) {

        SpringApplication.run(AscholarApplication.class, args);
	}



}
