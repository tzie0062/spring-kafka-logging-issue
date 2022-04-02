package com.example.demo;

import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
@EmbeddedKafka(topics = { "topic" }, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class DemoApplicationTests {
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	@Autowired
	private KafkaEventListener listener;
	@Value("${kafka.topic.in}")
	private String topic;

	@Test
	void noErrors() {
		kafkaTemplate.send(topic, "foo"); // happy path
		await()
			.atLeast(Duration.ofMillis(10L))
			.atMost(Duration.ofSeconds(10L))
			.until(() -> listener.isEventReceived());
	}

	@Test
	void errors() {
		kafkaTemplate.send(topic, "bar"); // will cause an exception, which will lead to retries
		await()
			.atLeast(Duration.ofMillis(10L))
			.atMost(Duration.ofSeconds(20L))
			.until(() -> listener.isDltEventReceived());
	}

}
