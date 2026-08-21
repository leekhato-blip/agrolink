package com.agrolink.service;

import com.agrolink.model.ConnectionResult;
import com.agrolink.model.EcosystemResponse;
import com.agrolink.model.EntityListResponse;
import com.agrolink.model.EntityResponse;
import com.agrolink.model.EntitySummary;
import com.agrolink.model.EntityType;
import com.agrolink.model.HealthResponse;
import com.agrolink.model.ImpactItem;
import com.agrolink.model.ImpactResponse;
import com.agrolink.model.TraversalResponse;
import com.agrolink.repository.GraphRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GraphService {

    private static final String DATABASE_NAME = "CognoDB";

    private final GraphRepository graphRepository;

    public GraphService(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    public HealthResponse health() {
        graphRepository.verifyConnection();
        return new HealthResponse("UP", DATABASE_NAME);
    }

    public EntityListResponse listEntities() {
        return new EntityListResponse("OK", graphRepository.listEntities());
    }

    public EntityResponse entity(String type, String id) {
        EntityType entityType = EntityType.fromPathValue(type);
        EntitySummary entity = graphRepository.findEntity(entityType, id);
        List<ConnectionResult> connections = graphRepository.directConnections(entityType, id);
        return new EntityResponse("OK", entity, connections);
    }

    public TraversalResponse direct(String type, String id) {
        EntityType entityType = EntityType.fromPathValue(type);
        EntitySummary entity = graphRepository.findEntity(entityType, id);
        List<ConnectionResult> connections = graphRepository.directConnections(entityType, id);
        return new TraversalResponse("OK", entity, connections);
    }

    public TraversalResponse supplierFarms(String supplierId) {
        EntitySummary supplier = graphRepository.findEntity(EntityType.SUPPLIER, supplierId);
        return new TraversalResponse("OK", supplier, graphRepository.farmsDependingOnSupplier(supplierId));
    }

    public TraversalResponse diseaseSuppliers(String diseaseId) {
        EntitySummary disease = graphRepository.findEntity(EntityType.DISEASE, diseaseId);
        return new TraversalResponse("OK", disease, graphRepository.suppliersConnectedToDisease(diseaseId));
    }

    public TraversalResponse sharedSuppliers(String farmId) {
        EntitySummary farm = graphRepository.findEntity(EntityType.FARM, farmId);
        return new TraversalResponse("OK", farm, graphRepository.sharedSuppliersForFarm(farmId));
    }

    public EcosystemResponse farmEcosystem(String farmId, int hops) {
        validateHopCount(hops);
        EntitySummary farm = graphRepository.findEntity(EntityType.FARM, farmId);
        return new EcosystemResponse("OK", farm, graphRepository.farmEcosystem(farmId, hops));
    }

    public ImpactResponse supplierImpact(String supplierId) {
        EntitySummary supplier = graphRepository.findEntity(EntityType.SUPPLIER, supplierId);
        return new ImpactResponse(
                "OK",
                supplier,
                graphRepository.supplierFeeds(supplierId),
                graphRepository.supplierLivestock(supplierId),
                graphRepository.supplierPonds(supplierId),
                combineFarmImpacts(supplierId)
        );
    }

    private List<ImpactItem> combineFarmImpacts(String supplierId) {
        List<ImpactItem> livestockFarms = graphRepository.supplierFarmsViaLivestock(supplierId);
        List<ImpactItem> pondFarms = graphRepository.supplierFarmsViaPonds(supplierId);

        java.util.Map<String, ImpactItem> map = new java.util.LinkedHashMap<>();
        for (ImpactItem farm : livestockFarms) {
            map.put(farm.id(), farm);
        }
        for (ImpactItem farm : pondFarms) {
            if (map.containsKey(farm.id())) {
                ImpactItem existing = map.get(farm.id());
                String mergedReason = "Affected through livestock & pond operations";
                map.put(farm.id(), new ImpactItem(existing.id(), existing.name(), mergedReason, existing.path()));
            } else {
                map.put(farm.id(), farm);
            }
        }
        return new ArrayList<>(map.values());
    }

    private void validateHopCount(int hops) {
        if (hops < 1 || hops > 4) {
            throw new IllegalArgumentException("Hop count must be between 1 and 4.");
        }
    }
}
