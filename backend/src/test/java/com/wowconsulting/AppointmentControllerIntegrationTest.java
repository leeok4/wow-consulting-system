package com.wowconsulting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wowconsulting.model.Appointment;
import com.wowconsulting.service.AppointmentService;
import com.wowconsulting.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private AuthService authService;

    @Test
    void testGetAllAppointments() throws Exception {
        com.wowconsulting.model.User adminUser = new com.wowconsulting.model.User();
        adminUser.setAdmin(true);
        // Garante que o método isAdmin retorna true
        when(authService.validateToken(anyString())).thenReturn(java.util.Optional.of(adminUser));
        when(appointmentService.getAllAppointments()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/appointments")
                .header("Authorization", "Bearer testtoken"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
