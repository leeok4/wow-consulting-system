package com.wowconsulting.repository;

import com.wowconsulting.model.TimeSlot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends MongoRepository<TimeSlot, String> {
    List<TimeSlot> findByIsAvailableTrueAndStartTimeAfterOrderByStartTime(LocalDateTime dateTime);
    List<TimeSlot> findByIsAvailableTrueAndStartTimeBetweenOrderByStartTime(
            LocalDateTime start,
            LocalDateTime end
    );
    List<TimeSlot> findAllByOrderByStartTime();
    List<TimeSlot> findByStartTimeBetweenOrderByStartTime(LocalDateTime start, LocalDateTime end);
    List<TimeSlot> findByStartTimeBetweenOrEndTimeBetween(
            LocalDateTime start1, LocalDateTime end1,
            LocalDateTime start2, LocalDateTime end2
    );
    List<TimeSlot> findByStartTimeBetweenOrEndTimeBetweenAndIdNot(
            LocalDateTime start1, LocalDateTime end1,
            LocalDateTime start2, LocalDateTime end2,
            String excludeId
    );
    List<TimeSlot> findByIsAvailableTrueAndEndTimeBefore(LocalDateTime dateTime);
    Optional<TimeSlot> findByAppointmentId(String appointmentId);
    List<TimeSlot> findByCreatedByDiscordIdOrderByStartTime(String discordId);
}