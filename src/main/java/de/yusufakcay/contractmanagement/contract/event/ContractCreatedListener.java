package de.yusufakcay.contractmanagement.contract.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Sample consumer standing in for a downstream system (e.g. document generation or policy issuing).
 * Idempotent because it only logs; failed attempts end up in the DLQ after the retries.
 */
@Slf4j
@Component
public final class ContractCreatedListener {

	@RabbitListener(queues = "${app.messaging.contract-created-queue}")
	public void onContractCreated(ContractCreatedEvent event) {
		log.info("ContractCreated received: {} | customer {} | {} | {} EUR | from {} (eventId={})",
			event.contractNumber(), event.customerNumber(), event.lineOfBusiness(), event.premium(), event.startDate(), event.eventId());
	}
}
