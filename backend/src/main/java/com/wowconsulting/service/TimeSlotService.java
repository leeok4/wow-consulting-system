package com.wowconsulting.service;

import com.wowconsulting.model.TimeSlot;
import com.wowconsulting.model.User;
import com.wowconsulting.repository.TimeSlotRepository;
import com.wowconsulting.dto.TimeSlotRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TimeSlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public List<TimeSlot> getAvailableTimeSlots() {
        return timeSlotRepository.findByIsAvailableTrueAndStartTimeAfterOrderByStartTime(LocalDateTime.now());
    }

    public List<TimeSlot> getAvailableTimeSlotsForDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        return timeSlotRepository.findByIsAvailableTrueAndStartTimeBetweenOrderByStartTime(
                startOfDay, endOfDay
        );
    }

    public List<TimeSlot> getAllTimeSlots() {
        return timeSlotRepository.findAllByOrderByStartTime();
    }

    public List<TimeSlot> getTimeSlotsForDateRange(LocalDateTime start, LocalDateTime end) {
        return timeSlotRepository.findByStartTimeBetweenOrderByStartTime(start, end);
    }

    @Transactional
    public TimeSlot createTimeSlot(TimeSlotRequest request, User admin) {
        // Validações
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Não é possível criar horário no passado");
        }

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new RuntimeException("Horário de fim deve ser posterior ao horário de início");
        }

        // Verifica conflitos
        List<TimeSlot> conflictingSlots = timeSlotRepository.findByStartTimeBetweenOrEndTimeBetween(
                request.getStartTime(), request.getEndTime(),
                request.getStartTime(), request.getEndTime()
        );

        if (!conflictingSlots.isEmpty()) {
            throw new RuntimeException("Já existe um horário cadastrado neste período");
        }

        TimeSlot timeSlot = new TimeSlot(request.getStartTime(), request.getEndTime(), admin);
        timeSlot.setDescription(request.getDescription());

        return timeSlotRepository.save(timeSlot);
    }

    @Transactional
    public List<TimeSlot> createRecurringTimeSlots(TimeSlotRequest request, User admin, int weeks) {
        List<TimeSlot> createdSlots = new java.util.ArrayList<>();

        for (int i = 0; i < weeks; i++) {
            LocalDateTime weeklyStartTime = request.getStartTime().plusWeeks(i);
            LocalDateTime weeklyEndTime = request.getEndTime().plusWeeks(i);

            // Verifica se não é no passado
            if (weeklyStartTime.isBefore(LocalDateTime.now())) {
                continue;
            }

            // Verifica conflitos para cada semana
            List<TimeSlot> conflictingSlots = timeSlotRepository.findByStartTimeBetweenOrEndTimeBetween(
                    weeklyStartTime, weeklyEndTime,
                    weeklyStartTime, weeklyEndTime
            );

            if (conflictingSlots.isEmpty()) {
                TimeSlot timeSlot = new TimeSlot(weeklyStartTime, weeklyEndTime, admin);
                timeSlot.setDescription(request.getDescription() + " (Recorrente)");
                createdSlots.add(timeSlotRepository.save(timeSlot));
            }
        }

        return createdSlots;
    }

    @Transactional
    public TimeSlot updateTimeSlot(String id, TimeSlotRequest request) {
        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findById(id);
        if (timeSlotOpt.isEmpty()) {
            throw new RuntimeException("Horário não encontrado");
        }

        TimeSlot timeSlot = timeSlotOpt.get();

        // Não permite alterar horário que já tem agendamento
        if (timeSlot.getAppointment() != null) {
            throw new RuntimeException("Não é possível alterar horário que já possui agendamento");
        }

        // Validações similares ao criar
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Não é possível alterar para horário no passado");
        }

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new RuntimeException("Horário de fim deve ser posterior ao horário de início");
        }

        // Verifica conflitos (excluindo o próprio horário)
        List<TimeSlot> conflictingSlots = timeSlotRepository.findByStartTimeBetweenOrEndTimeBetweenAndIdNot(
                request.getStartTime(), request.getEndTime(),
                request.getStartTime(), request.getEndTime(),
                id
        );

        if (!conflictingSlots.isEmpty()) {
            throw new RuntimeException("Já existe um horário cadastrado neste período");
        }

        timeSlot.setStartTime(request.getStartTime());
        timeSlot.setEndTime(request.getEndTime());
        timeSlot.setDescription(request.getDescription());
        timeSlot.setUpdatedAt(LocalDateTime.now());

        return timeSlotRepository.save(timeSlot);
    }

    @Transactional
    public void deleteTimeSlot(String id) {
        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findById(id);
        if (timeSlotOpt.isEmpty()) {
            throw new RuntimeException("Horário não encontrado");
        }

        TimeSlot timeSlot = timeSlotOpt.get();

        // Não permite deletar horário que já tem agendamento
        if (timeSlot.getAppointment() != null) {
            throw new RuntimeException("Não é possível deletar horário que já possui agendamento");
        }

        timeSlotRepository.delete(timeSlot);
    }

    @Transactional
    public void deleteExpiredEmptyTimeSlots() {
        LocalDateTime now = LocalDateTime.now();
        List<TimeSlot> expiredSlots = timeSlotRepository.findByIsAvailableTrueAndEndTimeBefore(now);
        timeSlotRepository.deleteAll(expiredSlots);
    }

    public Optional<TimeSlot> getTimeSlotById(String id) {
        return timeSlotRepository.findById(id);
    }

    public List<TimeSlot> getTimeSlotsByAdmin(String adminId) {
        return timeSlotRepository.findByCreatedByDiscordIdOrderByStartTime(adminId);
    }

    @Transactional
    public void fixInconsistentTimeSlots() {
        List<TimeSlot> allSlots = timeSlotRepository.findAll();
        for (TimeSlot slot : allSlots) {
            if (slot.getAppointment() == null && !slot.isAvailable()) {
                slot.setAvailable(true);
                slot.setUpdatedAt(LocalDateTime.now());
                timeSlotRepository.save(slot);
            }
            if (slot.getAppointment() != null && slot.isAvailable()) {
                slot.setAvailable(false);
                slot.setUpdatedAt(LocalDateTime.now());
                timeSlotRepository.save(slot);
            }
        }
    }
}