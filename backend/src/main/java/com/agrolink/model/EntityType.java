package com.agrolink.model;

public enum EntityType {
    FARM("Farm"),
    LIVESTOCK("Livestock"),
    FISH_POND("FishPond"),
    FEED("Feed"),
    SUPPLIER("Supplier"),
    DISEASE("Disease");

    private final String label;

    EntityType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static EntityType fromPathValue(String value) {
        for (EntityType type : values()) {
            if (type.label.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid entity type: " + value);
    }
}
