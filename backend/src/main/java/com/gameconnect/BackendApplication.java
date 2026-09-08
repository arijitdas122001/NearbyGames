package com.gameconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;


/**
 * Spring Security is intentionally disabled during Phase 1. A real security
 * configuration (JWT via HTTP-only cookie) is introduced in Phase 2.
 */
// // @SpringBootApplication(exclude = {
// // 		SecurityAutoConfiguration.class,
// // 		UserDetailsServiceAutoConfiguration.class
// })
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(BackendApplication.class, args);
	}

}
