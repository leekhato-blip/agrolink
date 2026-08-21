package com.agrolink.service;

import com.agrolink.model.HealthResponse;
import com.agrolink.repository.GraphRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class GraphServiceTest {

    @Test
    void healthReturnsUpAfterSuccessfulConnectionCheck() {
        GraphRepository repository = mock(GraphRepository.class);
        doNothing().when(repository).verifyConnection();

        GraphService service = new GraphService(repository);
        HealthResponse response = service.health();

        assertEquals("UP", response.status());
        assertEquals("CognoDB", response.database());
    }
}
