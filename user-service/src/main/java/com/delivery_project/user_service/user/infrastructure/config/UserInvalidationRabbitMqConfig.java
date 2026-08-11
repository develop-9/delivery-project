package com.delivery_project.user_service.user.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Access Token 무효화 아웃박스를 처리하는 큐. Slack Service의 TTL+DLX 재시도 큐 구조(
 * SlackRabbitMqConfig)를 그대로 따르되, RETRY_QUEUE의 TTL이 지나 MAIN_QUEUE로 되돌아오는
 * 흐름 자체가 "성공할 때까지 반복"이라 MAX_RETRY_COUNT 같은 상한을 두지 않는다.
 */
@Configuration
public class UserInvalidationRabbitMqConfig {

	public static final String EXCHANGE = "user.invalidation.exchange";

	public static final String MAIN_QUEUE = "user.invalidation.main.queue";
	public static final String RETRY_QUEUE = "user.invalidation.retry.queue";

	public static final String MAIN_ROUTING_KEY = "user.invalidation.process";
	public static final String RETRY_ROUTING_KEY = "user.invalidation.retry";

	private static final int RETRY_TTL_MS = 5_000;

	@Bean
	public DirectExchange userInvalidationExchange() {
		return new DirectExchange(EXCHANGE);
	}

	@Bean
	public Queue userInvalidationMainQueue() {
		return QueueBuilder.durable(MAIN_QUEUE)
				.build();
	}

	@Bean
	public Queue userInvalidationRetryQueue() {
		return QueueBuilder.durable(RETRY_QUEUE)
				.ttl(RETRY_TTL_MS)
				.deadLetterExchange(EXCHANGE)
				.deadLetterRoutingKey(MAIN_ROUTING_KEY)
				.build();
	}

	@Bean
	public Binding userInvalidationMainBinding(
			Queue userInvalidationMainQueue,
			DirectExchange userInvalidationExchange
	) {
		return BindingBuilder
				.bind(userInvalidationMainQueue)
				.to(userInvalidationExchange)
				.with(MAIN_ROUTING_KEY);
	}

	@Bean
	public Binding userInvalidationRetryBinding(
			Queue userInvalidationRetryQueue,
			DirectExchange userInvalidationExchange
	) {
		return BindingBuilder
				.bind(userInvalidationRetryQueue)
				.to(userInvalidationExchange)
				.with(RETRY_ROUTING_KEY);
	}

	@Bean
	public MessageConverter rabbitMessageConverter() {
		return new JacksonJsonMessageConverter();
	}
}
