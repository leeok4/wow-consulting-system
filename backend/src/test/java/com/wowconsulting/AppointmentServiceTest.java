package com.wowconsulting.service;

import com.wowconsulting.model.Appointment;
import com.wowconsulting.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {
    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAppointmentById() {
        Appointment appointment = new Appointment();
        appointment.setId("abc123");
        when(appointmentRepository.findById("abc123")).thenReturn(Optional.of(appointment));

        Optional<Appointment> result = appointmentService.getAppointmentById("abc123");
        assertTrue(result.isPresent());
        assertEquals("abc123", result.get().getId());
    }
}
