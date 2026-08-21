package com.agrolink.repository;

import com.agrolink.config.Neo4jProperties;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GraphRepositoryTest {

    @Test
    void readUsesParameterizedCypherAndConfiguredDatabase() {
        Driver driver = mock(Driver.class);
        Session session = mock(Session.class);
        Result result = mock(Result.class);
        Neo4jProperties properties = new Neo4jProperties();
        properties.setDatabase("agrolink");

        when(driver.session(any(SessionConfig.class))).thenReturn(session);
        when(session.run(eq("RETURN $value AS value"), eq(Map.of("value", "GreenFeed")))).thenReturn(result);

        GraphRepository repository = new GraphRepository(driver, properties);

        String outcome = repository.read(
                "RETURN $value AS value",
                Map.of("value", "GreenFeed"),
                ignored -> "ok"
        );

        assertEquals("ok", outcome);
        verify(driver).session(any(SessionConfig.class));
        verify(session).run(eq("RETURN $value AS value"), eq(Map.of("value", "GreenFeed")));
    }
}
