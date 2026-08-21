package com.agrolink.model;

import java.util.List;

public record ConnectionResult(
        String id,
        String name,
        String type,
        String relationship,
        String direction,
        String summary,
        List<PathStep> path
) {
}
