package com.example.demo;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaConsumerBackoffManager;
import org.springframework.kafka.listener.PartitionPausingBackOffManagerFactory;
import org.springframework.kafka.retrytopic.DeadLetterPublishingRecovererFactory;
import org.springframework.kafka.retrytopic.DefaultDestinationTopicResolver;
import org.springframework.kafka.retrytopic.DestinationTopicResolver;
import org.springframework.kafka.retrytopic.ListenerContainerFactoryConfigurer;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.retrytopic.RetryTopicInternalBeanNames;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class DemoApplication {

	public static final String KAFKA_EVENT_LISTENER = "kafkaEventListener";

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean(KAFKA_EVENT_LISTENER)
	public KafkaEventListener kafkaEventListener() {
		return new KafkaEventListener();
	}

	@Bean
	public RetryTopicConfiguration retryTopicConfiguration(
		KafkaTemplate<String, String> template,
		@Value("${kafka.topic.in}") String topicToInclude,
		@Value("${spring.application.name}") String appName) {
		return RetryTopicConfigurationBuilder
			.newInstance()
			.fixedBackOff(5000L)
			.maxAttempts(3)
			.retryTopicSuffix("-" + appName + ".retry")
			.suffixTopicsWithIndexValues()
			.dltSuffix("-" + appName + ".dlq")
			.includeTopic(topicToInclude)
			.dltHandlerMethod(KAFKA_EVENT_LISTENER, "handleDltEvent")
			.create(template);
	}

	/*
	@Bean
	DefaultErrorHandler eh() {
		DefaultErrorHandler eh = new DefaultErrorHandler((r, ex) -> log.info("Error: {}", ex.getMessage()),
			new FixedBackOff(0L, 3L));
		eh.setLogLevel(KafkaException.Level.DEBUG);
	}
	 */

	/*
	 * Three beans that are required to set custom logging level
	 */
	@Bean(name = RetryTopicInternalBeanNames.LISTENER_CONTAINER_FACTORY_CONFIGURER_NAME)
	public ListenerContainerFactoryConfigurer listenerContainerFactoryConfigurer(KafkaConsumerBackoffManager kafkaConsumerBackoffManager,
		DeadLetterPublishingRecovererFactory deadLetterPublishingRecovererFactory) {
		ListenerContainerFactoryConfigurer configurer =
			new ListenerContainerFactoryConfigurer(kafkaConsumerBackoffManager, deadLetterPublishingRecovererFactory, Clock.systemUTC());
		configurer.setErrorHandlerCustomizer(
			commonErrorHandler -> ((DefaultErrorHandler) commonErrorHandler).setLogLevel(KafkaException.Level.INFO));
		return configurer;
	}

	@Bean(name = RetryTopicInternalBeanNames.KAFKA_CONSUMER_BACKOFF_MANAGER)
	public KafkaConsumerBackoffManager backOffManager(ApplicationContext context) {
		PartitionPausingBackOffManagerFactory managerFactory = new PartitionPausingBackOffManagerFactory();
		managerFactory.setApplicationContext(context);
		return managerFactory.create();
	}

	@Bean(name = RetryTopicInternalBeanNames.DESTINATION_TOPIC_CONTAINER_NAME)
	public DestinationTopicResolver destinationTopicResolver(ApplicationContext context) {
		return new DefaultDestinationTopicResolver(Clock.systemUTC(), context);
	}
}
