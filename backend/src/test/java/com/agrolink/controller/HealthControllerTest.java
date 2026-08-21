package com.agrolink.controller;

import com.agrolink.exception.DatabaseUnavailableException;
import com.agrolink.model.HealthResponse;
import com.agrolink.service.GraphService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(com.agrolink.exception.GlobalExceptionHandler.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GraphService graphService;

    @Test
    void healthReturnsUpWhenDatabaseIsReachable() throws Exception {
        when(graphService.health()).thenReturn(new HealthResponse("UP", "CognoDB"));

        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("CognoDB"));
    }

    @Test
    void healthReturnsServiceUnavailableWhenDatabaseIsDown() throws Exception {
        doThrow(new DatabaseUnavailableException(
                "AgroLink could not connect to the graph database.",
                new RuntimeException("boom")
        )).when(graphService).health();

        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("DATABASE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("AgroLink could not connect to the graph database."));
    }
}
