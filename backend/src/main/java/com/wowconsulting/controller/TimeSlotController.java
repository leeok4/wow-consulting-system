package com.wowconsulting.controller;

import com.wowconsulting.service.TimeSlotService;
import com.wowconsulting.service.AuthService;
import com.wowconsulting.model.TimeSlot;
import com.wowconsulting.model.User;
import com.wowconsulting.dto.TimeSlotRequest;
import com.wowconsulting.dto.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/timeslots")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class TimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private AuthService authService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<TimeSlot>>> getAvailableTimeSlots(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Token inválido"));
            }

            List<TimeSlot> timeSlots;
            if (date != null) {
                timeSlots = timeSlotService.getAvailableTimeSlotsForDate(date);
            } else {
                timeSlots = timeSlotService.getAvailableTimeSlots();
            }

            return ResponseEntity.ok(ApiResponse.success(timeSlots));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TimeSlot>>> getAllTimeSlots(
            @RequestHeader("Authorization") String token) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }

            List<TimeSlot> timeSlots = timeSlotService.getAllTimeSlots();
            return ResponseEntity.ok(ApiResponse.success(timeSlots));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TimeSlot>> createTimeSlot(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody TimeSlotRequest request) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }

            TimeSlot timeSlot = timeSlotService.createTimeSlot(request, userOpt.get());
            return ResponseEntity.ok(ApiResponse.success("Horário criado com sucesso", timeSlot));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/recurring")
    public ResponseEntity<ApiResponse<List<TimeSlot>>> createRecurringTimeSlots(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody TimeSlotRequest request,
            @RequestParam(defaultValue = "4") int weeks) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }

            List<TimeSlot> timeSlots = timeSlotService.createRecurringTimeSlots(request, userOpt.get(), weeks);
            return ResponseEntity.ok(ApiResponse.success("Horários recorrentes criados com sucesso", timeSlots));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TimeSlot>> updateTimeSlot(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @Valid @RequestBody TimeSlotRequest request) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }

            TimeSlot timeSlot = timeSlotService.updateTimeSlot(id, request);
            return ResponseEntity.ok(ApiResponse.success("Horário atualizado com sucesso", timeSlot));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTimeSlot(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {

        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);

            if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }

            timeSlotService.deleteTimeSlot(id);
            return ResponseEntity.ok(ApiResponse.success("Horário deletado com sucesso", "OK"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/fix-inconsistencies")
    public ResponseEntity<ApiResponse<String>> fixInconsistencies(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Optional<User> userOpt = authService.validateToken(jwtToken);
            if (userOpt.isEmpty() || !userOpt.get().isAdmin()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Acesso negado"));
            }
            timeSlotService.fixInconsistentTimeSlots();
            return ResponseEntity.ok(ApiResponse.success("Horários corrigidos com sucesso", "OK"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}