package com.agrolink.model;

import java.util.List;

public record EntityListResponse(String status, List<EntitySummary> items) {
}
