package com.agrolink.model;

import java.util.List;

public record ImpactResponse(
        String status,
        EntitySummary supplier,
        List<ImpactItem> feeds,
        List<ImpactItem> livestock,
        List<ImpactItem> ponds,
        List<ImpactItem> farms
) {
}
