package com.argy.twofactorauth.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

	@Bean
	public ConcurrentHashMap<String, Bucket> userBuckets() {
		return new ConcurrentHashMap<>();
	}

	public static Bucket newDefaultBucket() {
		Bandwidth limit = Bandwidth.builder()
			.capacity(5)
			.refillGreedy(5, Duration.ofMinutes(1))
			.build();
		return Bucket.builder().addLimit(limit).build();
	}
}


