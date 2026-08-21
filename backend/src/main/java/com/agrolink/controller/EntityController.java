package com.agrolink.controller;

import com.agrolink.model.EntityListResponse;
import com.agrolink.model.EntityResponse;
import com.agrolink.service.GraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EntityController {

    private final GraphService graphService;

    public EntityController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/entities")
    public EntityListResponse listEntities() {
        return graphService.listEntities();
    }

    @GetMapping("/entities/{type}/{id}")
    public EntityResponse entity(@PathVariable String type, @PathVariable String id) {
        return graphService.entity(type, id);
    }
}
