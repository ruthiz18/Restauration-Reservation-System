package com.reservo.event;

import com.reservo.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes to RabbitMQ only after the DB transaction commits, and never lets a broker outage fail a booking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(ReservationEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, event.routingKey(), event);
        } catch (Exception e) {
            log.error("Could not publish {} for reservation {}: {}", event.routingKey(), event.reservationId(), e.getMessage());
        }
    }
}
