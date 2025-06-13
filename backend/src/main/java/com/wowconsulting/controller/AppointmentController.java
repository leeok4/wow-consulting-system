package com.wowconsulting.controller;

import com.wowconsulting.service.AppointmentService;
import com.wowconsulting.service.AuthService;
import com.wowconsulting.model.Appointment;
import com.wowconsulting.model.User;
import com.wowconsulting.dto.AppointmentRequest;
import com.wowconsulting.dto.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/appointments")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<Appointment>> createAppointment(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AppointmentRequest request) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Token inválido"));
            }

            Appointment appointment = appointmentService.createAppointment(request, userOpt.get());
            return ResponseEntity.ok(ApiResponse.success("Agendamento criado com sucesso", appointment));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Appointment>>> getMyAppointments(
            @RequestHeader("Authorization") String token) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Token inválido"));
            }

            List<Appointment> appointments = appointmentService.getUserAppointments(userOpt.get().getDiscordId());
            return ResponseEntity.ok(ApiResponse.success(appointments));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Appointment>>> getAllAppointments(
            @RequestHeader("Authorization") String token) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }

            List<Appointment> appointments = appointmentService.getAllAppointments();
            return ResponseEntity.ok(ApiResponse.success(appointments));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Appointment>>> getUpcomingAppointments(
            @RequestHeader("Authorization") String token) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }

            List<Appointment> appointments = appointmentService.getUpcomingAppointments();
            return ResponseEntity.ok(ApiResponse.success(appointments));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Appointment>> updateAppointmentStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody Map<String, String> request) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }

            Appointment.AppointmentStatus status = Appointment.AppointmentStatus.valueOf(request.get("status"));
            String notes = request.get("notes");

            Appointment appointment = appointmentService.updateAppointmentStatus(id, status, notes);
            return ResponseEntity.ok(ApiResponse.success("Status atualizado com sucesso", appointment));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> cancelAppointment(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> request) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Token inválido"));
            }

            // Verifica se é admin ou o próprio usuário do agendamento
            Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
            if (appointmentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Appointment appointment = appointmentOpt.get();
            if (!userOpt.get().isAdmin() && !appointment.getUser().getDiscordId().equals(userOpt.get().getDiscordId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }

            String reason = request != null ? request.get("reason") : "Cancelado pelo usuário";
            appointmentService.cancelAppointment(id, reason);

            return ResponseEntity.ok(ApiResponse.success("Agendamento cancelado com sucesso", "OK"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}