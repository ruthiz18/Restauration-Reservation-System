package com.reservo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "reservo.events";
    public static final String EMAIL_QUEUE = "notification.email";
    public static final String SMS_QUEUE = "notification.sms";

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }

    @Bean
    Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE).build();
    }

    /** Every reservation event produces an email. */
    @Bean
    Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(eventsExchange()).with("reservation.#");
    }

    /** SMS is reserved for time-sensitive events. */
    @Bean
    Declarables smsBindings() {
        return new Declarables(
                BindingBuilder.bind(smsQueue()).to(eventsExchange()).with("reservation.confirmed"),
                BindingBuilder.bind(smsQueue()).to(eventsExchange()).with("reservation.cancelled"),
                BindingBuilder.bind(smsQueue()).to(eventsExchange()).with("reservation.reminder"));
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.reservo.event");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
