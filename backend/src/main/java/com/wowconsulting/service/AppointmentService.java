package com.wowconsulting.service;

import com.wowconsulting.model.Appointment;
import com.wowconsulting.model.TimeSlot;
import com.wowconsulting.model.User;
import com.wowconsulting.repository.AppointmentRepository;
import com.wowconsulting.repository.TimeSlotRepository;
import com.wowconsulting.repository.UserRepository;
import com.wowconsulting.dto.AppointmentRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiscordService discordService;

    @Transactional
    public Appointment createAppointment(AppointmentRequest request, User user) {
        // Verifica se o horário ainda está disponível
        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findById(request.getTimeSlotId());
        if (timeSlotOpt.isEmpty()) {
            throw new RuntimeException("Horário não encontrado");
        }

        TimeSlot timeSlot = timeSlotOpt.get();
        if (!timeSlot.isAvailable()) {
            throw new RuntimeException("Horário não está mais disponível");
        }

        // Cria o agendamento
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setScheduledTime(timeSlot.getStartTime());
        appointment.setBnetId(request.getBnetId());
        appointment.setDiscordTag(request.getDiscordTag());
        appointment.setCharacterClass(request.getCharacterClass());
        appointment.setSpecialization(request.getSpecialization());
        appointment.setKnowledgeLevel(request.getKnowledgeLevel());
        appointment.setCurrentContent(request.getCurrentContent());
        appointment.setExpectations(request.getExpectations());

        // Salva o agendamento
        appointment = appointmentRepository.save(appointment);

        // Atualiza o horário como ocupado
        timeSlot.setAvailable(false);
        timeSlot.setAppointment(appointment);
        timeSlot.setUpdatedAt(LocalDateTime.now());
        timeSlotRepository.save(timeSlot);

        // Envia notificação via Discord
        sendAppointmentNotification(appointment, timeSlot);

        // Envia notificação ao dono do servidor
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String ownerMessage = String.format(
            "**Usuário:** %s\n**Data/Hora:** %s\n**BNet ID:** %s\n**Classe:** %s (%s)\n**Nível:** %s\n**Conteúdo Atual:** %s\n**Expectativas:** %s",
            appointment.getUser().getUsername(),
            appointment.getScheduledTime().format(formatter),
            appointment.getBnetId(),
            appointment.getCharacterClass(),
            appointment.getSpecialization(),
            appointment.getKnowledgeLevel(),
            appointment.getCurrentContent(),
            appointment.getExpectations()
        );
        discordService.sendAppointmentToOwner(ownerMessage);

        return appointment;
    }

    public List<Appointment> getUserAppointments(String discordId) {
        Optional<User> userOpt = userRepository.findByDiscordId(discordId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuário não encontrado");
        }
        return appointmentRepository.findByUser_IdOrderByScheduledTimeDesc(userOpt.get().getId());
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAllByOrderByScheduledTimeDesc();
    }

    public List<Appointment> getUpcomingAppointments() {
        return appointmentRepository.findByScheduledTimeAfterOrderByScheduledTime(LocalDateTime.now());
    }

    public Optional<Appointment> getAppointmentById(String id) {
        return appointmentRepository.findById(id);
    }

    @Transactional
    public Appointment updateAppointmentStatus(String id, Appointment.AppointmentStatus status, String notes) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);
        if (appointmentOpt.isEmpty()) {
            throw new RuntimeException("Agendamento não encontrado");
        }

        Appointment appointment = appointmentOpt.get();
        appointment.setStatus(status);
        if (notes != null && !notes.trim().isEmpty()) {
            appointment.setNotes(notes);
        }
        appointment.setUpdatedAt(LocalDateTime.now());

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        // Envia mensagem privada ao usuário se confirmado
        if (status == Appointment.AppointmentStatus.CONFIRMED) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String message = String.format(
                "✅ Sua consultoria foi confirmada para: %s\nClasse: %s (%s)",
                appointment.getScheduledTime().format(formatter),
                appointment.getCharacterClass(),
                appointment.getSpecialization()
            );
            discordService.sendAppointmentReminder(appointment.getUser().getDiscordId(), message);
        }

        return updatedAppointment;
    }

    @Transactional
    public void cancelAppointment(String id, String reason) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);
        if (appointmentOpt.isEmpty()) {
            throw new RuntimeException("Agendamento não encontrado");
        }

        Appointment appointment = appointmentOpt.get();
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        appointment.setNotes(reason);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        // Libera o horário
        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findByAppointmentId(id);
        if (timeSlotOpt.isPresent()) {
            TimeSlot timeSlot = timeSlotOpt.get();
            timeSlot.setAvailable(true);
            timeSlot.setAppointment(null);
            timeSlot.setUpdatedAt(LocalDateTime.now());
            timeSlotRepository.save(timeSlot);
        }

        // Notifica o cancelamento (público)
        sendCancellationNotification(appointment, reason);

        // Notifica o usuário no privado
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String message = String.format(
            "❌ Sua consultoria marcada para %s foi cancelada. Motivo: %s",
            appointment.getScheduledTime().format(formatter),
            reason != null ? reason : "Não informado"
        );
        discordService.sendAppointmentReminder(appointment.getUser().getDiscordId(), message);
    }

    private void sendAppointmentNotification(Appointment appointment, TimeSlot timeSlot) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String notificationMessage = String.format(
                "**Usuário:** %s\n" +
                        "**Data/Hora:** %s\n" +
                        "**BNet ID:** %s\n" +
                        "**Classe:** %s (%s)\n" +
                        "**Nível:** %s\n" +
                        "**Conteúdo Atual:** %s\n" +
                        "**Expectativas:** %s",
                appointment.getUser().getUsername(),
                appointment.getScheduledTime().format(formatter),
                appointment.getBnetId(),
                appointment.getCharacterClass(),
                appointment.getSpecialization(),
                appointment.getKnowledgeLevel(),
                appointment.getCurrentContent(),
                appointment.getExpectations()
        );

        discordService.sendAppointmentNotification(notificationMessage);
    }

    private void sendCancellationNotification(Appointment appointment, String reason) {
        // Não envia mais para o canal público, apenas para o usuário
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String message = String.format(
            "❌ Sua consultoria marcada para %s foi cancelada. Motivo: %s",
            appointment.getScheduledTime().format(formatter),
            reason != null ? reason : "Não informado"
        );
        discordService.sendAppointmentReminder(appointment.getUser().getDiscordId(), message);
    }

    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusHours(2); // Lembrete 2 horas antes

        List<Appointment> upcomingAppointments = appointmentRepository
                .findByScheduledTimeBetweenAndStatus(
                        now,
                        reminderTime,
                        Appointment.AppointmentStatus.SCHEDULED
                );

        for (Appointment appointment : upcomingAppointments) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String reminderMessage = String.format(
                    "Sua consultoria está agendada para: %s\n" +
                            "Classe: %s (%s)\n" +
                            "Prepare-se e esteja online no Discord!",
                    appointment.getScheduledTime().format(formatter),
                    appointment.getCharacterClass(),
                    appointment.getSpecialization()
            );

            discordService.sendAppointmentReminder(
                    appointment.getUser().getDiscordId(),
                    reminderMessage
            );
        }
    }
}