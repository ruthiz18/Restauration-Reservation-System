package com.reservo.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

public interface SmsGateway {
    void send(String phone, String message);

    /** Default gateway: logs the SMS. Replace with a Twilio/Africa's Talking implementation in production. */
    @Slf4j
    @Component
    class LoggingSmsGateway implements SmsGateway {
        @Override
        public void send(String phone, String message) {
            log.info("[SMS -> {}] {}", phone, message);
        }
    }
}
