package de.yusufakcay.contractmanagement.contract.event;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import de.yusufakcay.contractmanagement.shared.config.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Forwards the domain event to RabbitMQ only after a successful commit (no event for rolled-back contracts).
 * Best effort: guaranteed delivery would be the next step via a transactional outbox.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class ContractEventPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final MessagingProperties properties;

	@TransactionalEventListener
	public void onContractCreated(ContractCreatedEvent event) {
		try {
			rabbitTemplate.convertAndSend(properties.exchange(), properties.contractCreatedRoutingKey(), event);
			log.info("ContractCreated published: {} (eventId={})", event.contractNumber(), event.eventId());
		} catch (AmqpException e) {
			log.error("ContractCreated {} could not be published", event.contractNumber(), e);
		}
	}
}
