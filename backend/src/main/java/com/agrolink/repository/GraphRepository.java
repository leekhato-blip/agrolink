package com.agrolink.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.Neo4jException;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.springframework.stereotype.Repository;

import com.agrolink.config.Neo4jProperties;
import com.agrolink.exception.DatabaseUnavailableException;
import com.agrolink.exception.EntityNotFoundException;
import com.agrolink.model.ConnectionResult;
import com.agrolink.model.EntitySummary;
import com.agrolink.model.EntityType;
import com.agrolink.model.ImpactItem;
import com.agrolink.model.PathStep;

@Repository
public class GraphRepository {

    private final Driver driver;
    private final Neo4jProperties properties;

    public GraphRepository(Driver driver, Neo4jProperties properties) {
        this.driver = driver;
        this.properties = properties;
    }

    public void verifyConnection() {
        read("RETURN 1 AS ok", Map.of(), result -> {
            result.single().get("ok").asInt();
            return null;
        });
    }

    public List<EntitySummary> listEntities() {
        String cypher = """
                MATCH (n)
                RETURN
                    coalesce(n.id, '') AS id,
                    labels(n)[0] AS type,
                    coalesce(n.name, n.type, n.fishType, n.severity, n.id) AS name,
                    properties(n) AS properties
                ORDER BY type, id
                """;

        return read(cypher, Map.of(), result -> {
            List<EntitySummary> items = new ArrayList<>();
            while (result.hasNext()) {
                items.add(toEntitySummary(result.next()));
            }
            return items;
        });
    }

    public EntitySummary findEntity(EntityType type, String id) {
        String cypher = """
                MATCH (n:%s {id: $id})
                RETURN
                    coalesce(n.id, '') AS id,
                    labels(n)[0] AS type,
                    coalesce(n.name, n.type, n.fishType, n.severity, n.id) AS name,
                    properties(n) AS properties
                """.formatted(type.label());

        return readOne(cypher, Map.of("id", id), this::toEntitySummary);
    }

    public List<ConnectionResult> directConnections(EntityType type, String id) {
        EntitySummary selected = findEntity(type, id);
        String cypher = """
                MATCH (n:%s {id: $id})-[r]-(m)
                RETURN DISTINCT
                    coalesce(m.id, '') AS id,
                    labels(m)[0] AS type,
                    coalesce(m.name, m.type, m.fishType, m.severity, m.id) AS name,
                    properties(m) AS properties,
                    type(r) AS relationship,
                    CASE WHEN startNode(r) = n THEN 'OUT' ELSE 'IN' END AS direction
                ORDER BY type, name
                """.formatted(type.label());

        return read(cypher, Map.of("id", id), result -> {
            List<ConnectionResult> items = new ArrayList<>();
            while (result.hasNext()) {
                items.add(toConnectionResult(selected, result.next()));
            }
            return items;
        });
    }

    public List<ConnectionResult> farmsDependingOnSupplier(String supplierId) {
        EntitySummary supplier = findEntity(EntityType.SUPPLIER, supplierId);
        String cypher = """
                MATCH (s:Supplier {id: $supplierId})-[:SUPPLIES]->(feed:Feed)<-[:CONSUMES]-(livestock:Livestock)<-[:HAS_LIVESTOCK]-(farm:Farm)
                WITH s, farm, collect(DISTINCT feed.name) AS feedNames, collect(DISTINCT livestock.id) AS livestockIds
                RETURN DISTINCT
                    farm.id AS id,
                    'Farm' AS type,
                    farm.name AS name,
                    feedNames AS feedNames,
                    livestockIds AS livestockIds
                ORDER BY name
                """;

        return read(cypher, Map.of("supplierId", supplierId), result -> {
            List<ConnectionResult> items = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                items.add(connectionFromDependencyRecord(
                        supplier,
                        record,
                        "Depends on %s through %s".formatted(
                                supplier.name(),
                                joinValues(record.get("feedNames")),
                                joinValues(record.get("livestockIds"))
                        )
                ));
            }
            return items;
        });
    }

    public List<ConnectionResult> suppliersConnectedToDisease(String diseaseId) {
        EntitySummary disease = findEntity(EntityType.DISEASE, diseaseId);
        String cypher = """
                MATCH (d:Disease {id: $diseaseId})-[:AFFECTS]->(livestock:Livestock)-[:CONSUMES]->(feed:Feed)<-[:SUPPLIES]-(supplier:Supplier)
                WITH d, supplier, collect(DISTINCT feed.name) AS feedNames, collect(DISTINCT livestock.id) AS livestockIds
                RETURN DISTINCT
                    supplier.id AS id,
                    'Supplier' AS type,
                    supplier.name AS name,
                    feedNames AS feedNames,
                    livestockIds AS livestockIds
                ORDER BY name
                """;

        return read(cypher, Map.of("diseaseId", diseaseId), result -> {
            List<ConnectionResult> items = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                items.add(connectionFromDiseaseRecord(
                        disease,
                        record,
                        "Connected via %s".formatted(joinValues(record.get("feedNames")))
                ));
            }
            return items;
        });
    }

    public List<ConnectionResult> sharedSuppliersForFarm(String farmId) {
        EntitySummary farm = findEntity(EntityType.FARM, farmId);
        String cypher = """
                MATCH (farm:Farm {id: $farmId})-[:HAS_LIVESTOCK|HAS_POND]->()-[:CONSUMES]->(feed:Feed)<-[:SUPPLIES]-(supplier:Supplier)
                MATCH (supplier)-[:SUPPLIES]->(peerFeed:Feed)<-[:CONSUMES]-()<-[:HAS_LIVESTOCK|HAS_POND]-(peerFarm:Farm)
                WHERE peerFarm.id <> farm.id
                WITH farm, peerFarm, collect(DISTINCT supplier.name) AS supplierNames, collect(DISTINCT feed.name) AS sourceFeeds, collect(DISTINCT peerFeed.name) AS peerFeeds
                RETURN DISTINCT
                    peerFarm.id AS id,
                    'Farm' AS type,
                    peerFarm.name AS name,
                    supplierNames AS supplierNames,
                    sourceFeeds AS sourceFeeds,
                    peerFeeds AS peerFeeds
                ORDER BY name
                """;

        return read(cypher, Map.of("farmId", farmId), result -> {
            List<ConnectionResult> items = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                String suppliers = joinValues(record.get("supplierNames"));
                items.add(connectionFromSharedSupplierRecord(
                        farm,
                        record,
                        "Shares supplier(s): %s".formatted(suppliers)
                ));
            }
            return items;
        });
    }

    public List<ConnectionResult> farmEcosystem(String farmId, int hops) {
        EntitySummary farm = findEntity(EntityType.FARM, farmId);
        String cypher = """
                MATCH p=(farm:Farm {id: $farmId})-[*1..4]-(other)
                WHERE other.id IS NOT NULL
                  AND other.id <> farm.id
                  AND length(p) <= $hops
                WITH other, head(collect(p)) AS path
                RETURN DISTINCT
                    other.id AS id,
                    labels(other)[0] AS type,
                    coalesce(other.name, other.type, other.fishType, other.severity, other.id) AS name,
                    properties(other) AS properties,
                    path
                ORDER BY type, name
                """;

        return read(cypher, Map.of("farmId", farmId, "hops", hops), result -> {
            Map<String, ConnectionResult> deduped = new LinkedHashMap<>();
            while (result.hasNext()) {
                Record record = result.next();
                ConnectionResult item = connectionFromPathRecord(farm, record, "Within %d hops".formatted(hops));
                deduped.putIfAbsent(item.id(), item);
            }
            return new ArrayList<>(deduped.values());
        });
    }

    public List<ImpactItem> supplierFeeds(String supplierId) {
        EntitySummary supplier = findEntity(EntityType.SUPPLIER, supplierId);
        String cypher = """
                MATCH p=(s:Supplier {id: $supplierId})-[:SUPPLIES]->(feed:Feed)
                RETURN DISTINCT
                    feed.id AS id,
                    feed.name AS name,
                    p AS path
                ORDER BY name
                """;
        return read(cypher, Map.of("supplierId", supplierId), result -> collectImpactItems(result, supplier, "Supplies feed", true));
    }

    public List<ImpactItem> supplierLivestock(String supplierId) {
        EntitySummary supplier = findEntity(EntityType.SUPPLIER, supplierId);
        String cypher = """
                MATCH p=(s:Supplier {id: $supplierId})-[:SUPPLIES]->(feed:Feed)<-[:CONSUMES]-(livestock:Livestock)
                RETURN DISTINCT
                    livestock.id AS id,
                    coalesce(livestock.type, livestock.id) AS name,
                    p AS path
                ORDER BY name
                """;
        return read(cypher, Map.of("supplierId", supplierId), result -> collectImpactItems(result, supplier, "Depends on supplied feed", false));
    }

    public List<ImpactItem> supplierPonds(String supplierId) {
        EntitySummary supplier = findEntity(EntityType.SUPPLIER, supplierId);
        String cypher = """
                MATCH p=(s:Supplier {id: $supplierId})-[:SUPPLIES]->(feed:Feed)<-[:CONSUMES]-(pond:FishPond)
                RETURN DISTINCT
                    pond.id AS id,
                    pond.name AS name,
                    p AS path
                ORDER BY name
                """;
        return read(cypher, Map.of("supplierId", supplierId), result -> collectImpactItems(result, supplier, "Depends on supplied feed", false));
    }

    public List<ImpactItem> supplierFarmsViaLivestock(String supplierId) {
        EntitySummary supplier = findEntity(EntityType.SUPPLIER, supplierId);
        String cypher = """
                MATCH p=(s:Supplier {id: $supplierId})-[:SUPPLIES]->(feed:Feed)<-[:CONSUMES]-(livestock:Livestock)<-[:HAS_LIVESTOCK]-(farm:Farm)
                RETURN DISTINCT
                    farm.id AS id,
                    farm.name AS name,
                    p AS path
                ORDER BY name
                """;
        return read(cypher, Map.of("supplierId", supplierId), result -> collectImpactItems(result, supplier, "Affected through livestock", false));
    }

    public List<ImpactItem> supplierFarmsViaPonds(String supplierId) {
        EntitySummary supplier = findEntity(EntityType.SUPPLIER, supplierId);
        String cypher = """
                MATCH p=(s:Supplier {id: $supplierId})-[:SUPPLIES]->(feed:Feed)<-[:CONSUMES]-(pond:FishPond)<-[:HAS_POND]-(farm:Farm)
                RETURN DISTINCT
                    farm.id AS id,
                    farm.name AS name,
                    p AS path
                ORDER BY name
                """;
        return read(cypher, Map.of("supplierId", supplierId), result -> collectImpactItems(result, supplier, "Affected through pond operations", false));
    }

    public <T> T read(String cypher, Map<String, Object> parameters, Function<Result, T> mapper) {
        try (Session session = driver.session(SessionConfig.forDatabase(properties.getDatabase()))) {
            Result result = session.run(cypher, parameters);
            return mapper.apply(result);
        } catch (Neo4jException exception) {
            throw new DatabaseUnavailableException(
                    "AgroLink could not connect to the graph database.",
                    exception
            );
        }
    }

    public <T> T readOne(String cypher, Map<String, Object> parameters, Function<Record, T> mapper) {
        try (Session session = driver.session(SessionConfig.forDatabase(properties.getDatabase()))) {
            Result result = session.run(cypher, parameters);
            if (!result.hasNext()) {
                throw new EntityNotFoundException("Requested AgroLink entity was not found.");
            }
            return mapper.apply(result.single());
        } catch (NoSuchRecordException exception) {
            throw new EntityNotFoundException("Requested AgroLink entity was not found.");
        } catch (Neo4jException exception) {
            throw new DatabaseUnavailableException(
                    "AgroLink could not connect to the graph database.",
                    exception
            );
        }
    }

    private EntitySummary toEntitySummary(Record record) {
        return new EntitySummary(
                record.get("type").asString("Unknown"),
                record.get("id").asString(""),
                record.get("name").asString(""),
                toMap(record.get("properties"))
        );
    }

    private ConnectionResult toConnectionResult(EntitySummary selected, Record record) {
        EntitySummary connected = new EntitySummary(
                record.get("type").asString("Unknown"),
                record.get("id").asString(""),
                record.get("name").asString(""),
                toMap(record.get("properties"))
        );
        List<PathStep> path = List.of(
                new PathStep(selected.type(), selected.id(), selected.name()),
                new PathStep(connected.type(), connected.id(), connected.name())
        );
        return new ConnectionResult(
                connected.id(),
                connected.name(),
                connected.type(),
                record.get("relationship").asString("CONNECTED"),
                record.get("direction").asString("OUT"),
                "%s %s".formatted(selected.name(), record.get("relationship").asString("CONNECTED")),
                path
        );
    }

    private ConnectionResult connectionFromDependencyRecord(EntitySummary supplier, Record record, String summary) {
        List<PathStep> path = List.of(
                new PathStep(supplier.type(), supplier.id(), supplier.name()),
                new PathStep("Feed", firstValue(record.get("feedNames")), firstValue(record.get("feedNames"))),
                new PathStep("Livestock", firstValue(record.get("livestockIds")), firstValue(record.get("livestockIds"))),
                new PathStep(record.get("type").asString("Farm"), record.get("id").asString(""), record.get("name").asString(""))
        );
        return new ConnectionResult(
                record.get("id").asString(""),
                record.get("name").asString(""),
                record.get("type").asString("Farm"),
                "DEPENDS_ON",
                "OUT",
                summary,
                path
        );
    }

    private ConnectionResult connectionFromDiseaseRecord(EntitySummary disease, Record record, String summary) {
        List<PathStep> path = List.of(
                new PathStep(disease.type(), disease.id(), disease.name()),
                new PathStep("Livestock", firstValue(record.get("livestockIds")), firstValue(record.get("livestockIds"))),
                new PathStep("Feed", firstValue(record.get("feedNames")), firstValue(record.get("feedNames"))),
                new PathStep(record.get("type").asString("Supplier"), record.get("id").asString(""), record.get("name").asString(""))
        );
        return new ConnectionResult(
                record.get("id").asString(""),
                record.get("name").asString(""),
                record.get("type").asString("Supplier"),
                "AFFECTS_SUPPLY",
                "OUT",
                summary,
                path
        );
    }

    private ConnectionResult connectionFromSharedSupplierRecord(EntitySummary farm, Record record, String summary) {
        String supplierLabel = record.containsKey("supplierNames") ? joinValues(record.get("supplierNames")) : record.get("supplierName").asString("");
        List<PathStep> path = List.of(
                new PathStep(farm.type(), farm.id(), farm.name()),
                new PathStep("Feed", firstValue(record.get("sourceFeeds")), firstValue(record.get("sourceFeeds"))),
                new PathStep("Supplier", "", supplierLabel),
                new PathStep("Feed", firstValue(record.get("peerFeeds")), firstValue(record.get("peerFeeds"))),
                new PathStep(record.get("type").asString("Farm"), record.get("id").asString(""), record.get("name").asString(""))
        );
        return new ConnectionResult(
                record.get("id").asString(""),
                record.get("name").asString(""),
                record.get("type").asString("Farm"),
                "SHARES_SUPPLIER",
                "OUT",
                summary,
                path
        );
    }

    private ConnectionResult connectionFromPathRecord(EntitySummary farm, Record record, String summary) {
        return new ConnectionResult(
                record.get("id").asString(""),
                record.get("name").asString(""),
                record.get("type").asString("Unknown"),
                "CONNECTED",
                "OUT",
                summary,
                toPathSteps(record.get("path").asPath())
        );
    }

    private List<ImpactItem> collectImpactItems(Result result, EntitySummary supplier, String reasonPrefix, boolean useFeedReason) {
        List<ImpactItem> items = new ArrayList<>();
        while (result.hasNext()) {
            Record record = result.next();
            String reason = useFeedReason
                    ? "%s: %s".formatted(reasonPrefix, record.get("name").asString(""))
                    : reasonPrefix;
            items.add(new ImpactItem(
                    record.get("id").asString(""),
                    record.get("name").asString(""),
                    reason,
                    toPathSteps(record.get("path").asPath())
            ));
        }
        return items;
    }

    private List<PathStep> toPathSteps(org.neo4j.driver.types.Path path) {
            List<PathStep> steps = new ArrayList<>();

    path.nodes().forEach(node -> {
        List<String> labels = new ArrayList<>();
        node.labels().forEach(labels::add);

        String type = labels.isEmpty()
                ? "Unknown"
                : labels.get(0);

        String id = valueAsString(node.asMap().get("id"));

        steps.add(new PathStep(
                type,
                id,
                displayName(type, node.asMap(), id)
        ));
    });

    return steps;
    }

    private Map<String, Object> toMap(Value value) {
        return value == null || value.isNull()
                ? Map.of()
                : value.asMap(v -> v.asObject());
    }

    private String displayName(String type, Map<String, Object> properties, String id) {
        Object name = properties.get("name");
        if (Objects.equals(type, "Livestock")) {
            Object livestockType = properties.get("type");
            return livestockType == null ? id : livestockType + " batch";
        }
        if (name != null) {
            return String.valueOf(name);
        }
        if (properties.get("type") != null) {
            return String.valueOf(properties.get("type"));
        }
        if (properties.get("severity") != null) {
            return String.valueOf(properties.get("severity"));
        }
        return id;
    }

    private String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String joinValues(Value value) {
        if (value == null || value.isNull()) {
            return "";
        }
        try {
            List<Object> items = value.asList(v -> v.asObject());
            return items.stream().map(String::valueOf).filter(item -> !item.isBlank()).reduce((a, b) -> a + ", " + b).orElse("");
        } catch (Exception ignored) {
            return value.asString("");
        }
    }

    private String firstValue(Value value) {
        if (value == null || value.isNull()) {
            return "";
        }
        try {
            List<Object> items = value.asList(v -> v.asObject());
            return items.isEmpty() ? "" : String.valueOf(items.get(0));
        } catch (Exception ignored) {
            return value.asString("");
        }
    }
}
