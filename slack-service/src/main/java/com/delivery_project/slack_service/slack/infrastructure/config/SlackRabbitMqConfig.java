package com.delivery_project.slack_service.slack.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackRabbitMqConfig {

    public static final String EXCHANGE = "slack.message.exchange";

    public static final String MAIN_QUEUE = "slack.message.main.queue";
    public static final String RETRY_QUEUE = "slack.message.retry.queue";

    public static final String MAIN_ROUTING_KEY = "slack.message.send";
    public static final String RETRY_ROUTING_KEY = "slack.message.retry";

    private static final int RETRY_TTL_MS = 5_000;

    private static final String TRUSTED_PACKAGE =
            "com.delivery_project.slack_service.slack.infrastructure.messaging.rabbitmq";

    @Bean
    public DirectExchange slackMessageExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue slackMessageMainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE)
                .build();
    }

    @Bean
    public Queue slackMessageRetryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)
                .ttl(RETRY_TTL_MS)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(MAIN_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding slackMessageMainBinding(
            Queue slackMessageMainQueue,
            DirectExchange slackMessageExchange
    ) {
        return BindingBuilder
                .bind(slackMessageMainQueue)
                .to(slackMessageExchange)
                .with(MAIN_ROUTING_KEY);
    }

    @Bean
    public Binding slackMessageRetryBinding(
            Queue slackMessageRetryQueue,
            DirectExchange slackMessageExchange
    ) {
        return BindingBuilder
                .bind(slackMessageRetryQueue)
                .to(slackMessageExchange)
                .with(RETRY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter(
                TRUSTED_PACKAGE
        );
    }
}