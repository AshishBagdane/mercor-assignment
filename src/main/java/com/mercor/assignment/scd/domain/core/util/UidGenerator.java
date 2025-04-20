package com.mercor.assignment.scd.domain.core.util;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility for generating unique IDs for SCD entities
 */
@Component
public class UidGenerator {

    /**
     * Generate a unique ID for an entity version
     *
     * @param entityType the type of entity (e.g., "jobs", "timelog")
     * @return a unique ID string
     */
    public String generateUid(String entityType) {
        // Create a unique ID with entity type prefix
        // Format: {entityType}_uid_{random-string}
        
        UUID uuid = UUID.randomUUID();
        String encodedId = encodeUuid(uuid);
        
        return entityType + "_uid_" + encodedId;
    }
    
    /**
     * Generate a new entity ID
     *
     * @param entityType the type of entity
     * @return a unique entity ID
     */
    public String generateEntityId(String entityType) {
        // Create a unique ID with entity type prefix
        // Format: {entityType}_{random-string}
        
        UUID uuid = UUID.randomUUID();
        String encodedId = encodeUuid(uuid);
        
        return entityType + "_" + encodedId;
    }
    
    /**
     * Encode a UUID into a URL-safe string
     */
    private String encodeUuid(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
    }
}