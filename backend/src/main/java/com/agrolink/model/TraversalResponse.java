package com.agrolink.model;

import java.util.List;

public record TraversalResponse(String status, EntitySummary entity, List<ConnectionResult> items) {
}
