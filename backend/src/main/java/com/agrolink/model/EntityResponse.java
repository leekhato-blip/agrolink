package com.agrolink.model;

import java.util.List;

public record EntityResponse(String status, EntitySummary entity, List<ConnectionResult> connections) {
}
