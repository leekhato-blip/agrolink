package com.agrolink.model;

import java.util.List;

public record ImpactItem(
        String id,
        String name,
        String reason,
        List<PathStep> path
) {
}
