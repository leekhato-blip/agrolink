package com.agrolink.model;

import java.util.Map;

public record EntitySummary(
        String type,
        String id,
        String name,
        Map<String, Object> properties
) {
}
