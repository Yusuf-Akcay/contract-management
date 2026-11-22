package de.yusufakcay.contractmanagement.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	@Bean
	TopicExchange contractExchange(MessagingProperties properties) {
		return new TopicExchange(properties.exchange());
	}

	@Bean
	Queue contractCreatedQueue(MessagingProperties properties) {
		return QueueBuilder.durable(properties.contractCreatedQueue())
			.deadLetterExchange("")
			.deadLetterRoutingKey(properties.contractCreatedQueue() + ".dlq")
			.build();
	}

	@Bean
	Queue contractCreatedDeadLetterQueue(MessagingProperties properties) {
		return QueueBuilder.durable(properties.contractCreatedQueue() + ".dlq").build();
	}

	@Bean
	Binding contractCreatedBinding(Queue contractCreatedQueue, TopicExchange contractExchange, MessagingProperties properties) {
		return BindingBuilder.bind(contractCreatedQueue).to(contractExchange).with(properties.contractCreatedRoutingKey());
	}

	@Bean
	MessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}
}
