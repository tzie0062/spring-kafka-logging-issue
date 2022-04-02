package com.example.demo;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class KafkaEventListener {
	// just for testing
	private boolean eventReceived;
	// just for testing
	private boolean dltEventReceived;

	@KafkaListener(id = "kafkaEventListener", topics = "${kafka.topic.in}", groupId = "${spring.application.name}")
	public void handleEvent(@Payload String event) {
		log.info("Received event: {}", event);
		if ("bar".equalsIgnoreCase(event)) {
			throw new RuntimeException("Error while processing " + event);
		}
		eventReceived = true;
	}

	public void handleDltEvent(Object message) {
		log.info("Received DLT event: {}", message);
		dltEventReceived = true;
	}
}
