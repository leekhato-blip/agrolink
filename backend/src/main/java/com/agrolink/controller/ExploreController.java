package com.agrolink.controller;

import com.agrolink.model.EcosystemResponse;
import com.agrolink.model.ImpactResponse;
import com.agrolink.model.TraversalResponse;
import com.agrolink.service.GraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/explore")
public class ExploreController {

    private final GraphService graphService;

    public ExploreController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/{type}/{id}")
    public TraversalResponse direct(@PathVariable String type, @PathVariable String id) {
        return graphService.direct(type, id);
    }

    @GetMapping("/supplier/{id}/farms")
    public TraversalResponse supplierFarms(@PathVariable String id) {
        return graphService.supplierFarms(id);
    }

    @GetMapping("/disease/{id}/suppliers")
    public TraversalResponse diseaseSuppliers(@PathVariable String id) {
        return graphService.diseaseSuppliers(id);
    }

    @GetMapping("/farm/{id}/shared-suppliers")
    public TraversalResponse sharedSuppliers(@PathVariable String id) {
        return graphService.sharedSuppliers(id);
    }

    @GetMapping("/farm/{id}/ecosystem")
    public EcosystemResponse ecosystem(@PathVariable String id, @RequestParam(defaultValue = "3") int hops) {
        return graphService.farmEcosystem(id, hops);
    }

    @GetMapping("/supplier/{id}/impact")
    public ImpactResponse supplierImpact(@PathVariable String id) {
        return graphService.supplierImpact(id);
    }
}
