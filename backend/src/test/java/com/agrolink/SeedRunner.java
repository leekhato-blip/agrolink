package com.agrolink;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class SeedRunner {

    @Test
    void runSeed() throws Exception {
        String uri = System.getenv("COGNODB_URI");
        String user = System.getenv("COGNODB_USERNAME");
        String pass = System.getenv("COGNODB_PASSWORD");
        String db = System.getenv().getOrDefault("COGNODB_DATABASE", "neo4j");

        System.out.println("Connecting to " + uri + " [db=" + db + "]...");

        try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, pass))) {
            Path seedPath = Path.of("../seed/seed.cypher");
            if (!Files.exists(seedPath)) {
                seedPath = Path.of("seed/seed.cypher");
            }
            String content = Files.readString(seedPath);

            String[] statements = content.split(";");

            try (Session session = driver.session(SessionConfig.forDatabase(db))) {
                for (String stmt : statements) {
                    String clean = stmt.trim();
                    // filter out pure comment blocks or empty strings
                    String[] lines = clean.split("\n");
                    StringBuilder sb = new StringBuilder();
                    for (String line : lines) {
                        String l = line.trim();
                        if (!l.startsWith("//") && !l.isEmpty()) {
                            sb.append(line).append("\n");
                        }
                    }
                    String cypher = sb.toString().trim();
                    if (!cypher.isEmpty()) {
                        System.out.println("Executing: " + cypher.substring(0, Math.min(cypher.length(), 60)) + "...");
                        try {
                            session.run(cypher);
                        } catch (Exception e) {
                            System.err.println("Warning/Error on statement execution: " + e.getMessage());
                        }
                    }
                }

                // Verify count
                Result result = session.run("""
                        MATCH (n)
                        WITH count(n) AS nodes
                        MATCH ()-[r]->()
                        RETURN nodes, count(r) AS relationships;
                        """);
                if (result.hasNext()) {
                    Record rec = result.next();
                    System.out.println("VERIFICATION -> Nodes: " + rec.get("nodes").asLong() + ", Relationships: " + rec.get("relationships").asLong());
                }
            }
        }
    }
}
