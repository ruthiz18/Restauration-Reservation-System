package com.reservo.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Audit trail of every message consumed from RabbitMQ and dispatched to a channel. */
@Document("notification_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog {
    @Id
    private String id;
    @Indexed
    private Long reservationId;
    private String channel;
    private String eventType;
    private String recipient;
    private String message;
    private String status;
    @Builder.Default
    private Instant createdAt = Instant.now();
}
