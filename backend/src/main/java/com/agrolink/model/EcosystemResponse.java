package com.agrolink.model;

import java.util.List;

public record EcosystemResponse(String status, EntitySummary entity, List<ConnectionResult> nodes) {
}
