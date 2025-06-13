package com.wowconsulting.repository;

import com.wowconsulting.model.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    List<Appointment> findByUser_IdOrderByScheduledTimeDesc(String userId);
    List<Appointment> findAllByOrderByScheduledTimeDesc();
    List<Appointment> findByScheduledTimeAfterOrderByScheduledTime(LocalDateTime dateTime);
    List<Appointment> findByScheduledTimeBetweenAndStatus(
            LocalDateTime start,
            LocalDateTime end,
            Appointment.AppointmentStatus status
    );
    List<Appointment> findByStatus(Appointment.AppointmentStatus status);
    List<Appointment> findByScheduledTimeBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(Appointment.AppointmentStatus status);
}