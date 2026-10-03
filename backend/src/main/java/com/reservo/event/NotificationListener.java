package com.reservo.event;

import com.reservo.config.RabbitConfig;
import com.reservo.mongo.MongoRepos.NotificationLogRepository;
import com.reservo.mongo.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Component
public class NotificationListener {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final SmsGateway smsGateway;
    private final NotificationLogRepository logs;
    private final SpringTemplateEngine templates;
    private final boolean emailEnabled;
    private final String from;
    private final String frontendUrl;

    public NotificationListener(ObjectProvider<JavaMailSender> mailSender, SmsGateway smsGateway,
                                NotificationLogRepository logs, SpringTemplateEngine templates,
                                @Value("${app.notifications.email-enabled}") boolean emailEnabled,
                                @Value("${app.notifications.from}") String from,
                                @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.smsGateway = smsGateway;
        this.logs = logs;
        this.templates = templates;
        this.emailEnabled = emailEnabled;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void onEmail(ReservationEvent e) {
        String subject = "Reservo - " + e.restaurantName() + " (" + e.type() + ")";
        String body = "Hello %s,\n\n%s\n\nThank you for using Reservo.".formatted(e.recipientName(), e.summary());
        String status = "SIMULATED";
        JavaMailSender sender = mailSender.getIfAvailable();
        if (emailEnabled && sender != null && e.recipientEmail() != null) {
            try {
                var message = sender.createMimeMessage();
                var helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(e.recipientEmail());
                helper.setSubject(subject);
                helper.setText(body, renderHtml(e)); // plain-text part + Thymeleaf HTML part
                sender.send(message);
                status = "SENT";
            } catch (Exception ex) {
                log.warn("Email to {} failed: {}", e.recipientEmail(), ex.getMessage());
                status = "FAILED";
            }
        } else {
            log.info("[EMAIL -> {}] {} | {}", e.recipientEmail(), subject, e.summary());
        }
        record("EMAIL", e, e.recipientEmail(), body, status);
    }

    @RabbitListener(queues = RabbitConfig.SMS_QUEUE)
    public void onSms(ReservationEvent e) {
        if (e.recipientPhone() == null || e.recipientPhone().isBlank()) {
            record("SMS", e, null, e.summary(), "SKIPPED_NO_PHONE");
            return;
        }
        String text = "Reservo: " + e.summary();
        smsGateway.send(e.recipientPhone(), text);
        record("SMS", e, e.recipientPhone(), text, "SENT");
    }

    /** Renders templates/email/reservation.html for the event. */
    public String renderHtml(ReservationEvent e) {
        Context ctx = new Context();
        ctx.setVariable("event", e);
        ctx.setVariable("heading", heading(e.type()));
        ctx.setVariable("intro", intro(e.type()));
        ctx.setVariable("frontendUrl", frontendUrl);
        return templates.process("email/reservation", ctx);
    }

    private static String heading(String type) {
        return switch (type) {
            case "created" -> "We received your reservation";
            case "confirmed" -> "Your reservation is confirmed";
            case "cancelled" -> "Your reservation was cancelled";
            case "reminder" -> "Reminder: your table is coming up";
            default -> "Your reservation was updated";
        };
    }

    private static String intro(String type) {
        return switch (type) {
            case "created" -> "Thanks for booking. The restaurant will confirm your table shortly.";
            case "confirmed" -> "Great news - the restaurant has confirmed your table.";
            case "cancelled" -> "This reservation has been cancelled and the table released.";
            case "reminder" -> "Just a friendly reminder about your upcoming reservation.";
            default -> "There is a change to your reservation; the details are below.";
        };
    }

    private void record(String channel, ReservationEvent e, String recipient, String message, String status) {
        logs.save(NotificationLog.builder().reservationId(e.reservationId()).channel(channel)
                .eventType(e.type()).recipient(recipient).message(message).status(status).build());
    }
}
