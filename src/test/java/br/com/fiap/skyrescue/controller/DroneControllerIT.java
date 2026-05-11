package br.com.fiap.skyrescue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DroneControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void shouldCreateAndListDrones() throws Exception {
        Map<String, Object> payload = Map.of(
                "serialNumber", "SR-TEST-100",
                "model", "SkyRescue Alpha Test",
                "batteryLevel", 90
        );

        mockMvc.perform(post("/api/v1/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.serialNumber").value("SR-TEST-100"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        mockMvc.perform(get("/api/v1/drones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.serialNumber=='SR-TEST-100')].model").value(equalTo(java.util.List.of("SkyRescue Alpha Test"))));
    }

    @Test
    void shouldReturn404WhenDroneNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/drones/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusEndpointShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
