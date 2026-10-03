package com.reservo.service;

import com.reservo.domain.Reservation;
import com.reservo.domain.ReservationStatus;
import com.reservo.repo.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/** Queues a reminder (email + SMS) for confirmed reservations starting within the next two hours. */
@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    private final ReservationRepository reservations;
    private final ApplicationEventPublisher events;

    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
    @Transactional
    public void sendReminders() {
        LocalTime now = LocalTime.now();
        LocalTime horizon = now.plusHours(2);
        for (Reservation r : reservations.findByStatusAndReminderSentFalseAndDate(
                ReservationStatus.CONFIRMED, LocalDate.now())) {
            boolean inWindow = !r.getStartTime().isBefore(now) && !r.getStartTime().isAfter(horizon);
            if (inWindow) {
                r.setReminderSent(true);
                events.publishEvent(ReservationService.event("reminder", r));
            }
        }
    }
}
