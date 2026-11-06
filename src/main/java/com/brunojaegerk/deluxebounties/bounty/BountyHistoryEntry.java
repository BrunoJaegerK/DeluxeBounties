package com.brunojaegerk.deluxebounties.bounty;

import java.util.UUID;

public final class BountyHistoryEntry {

    private final long id;
    private final String eventType;
    private final UUID actorUUID;
    private final String actorName;
    private final UUID targetUUID;
    private final String targetName;
    private final double amount;
    private final long createdAt;
    private final String details;

    public BountyHistoryEntry(
            long id,
            String eventType,
            UUID actorUUID,
            String actorName,
            UUID targetUUID,
            String targetName,
            double amount,
            long createdAt,
            String details
    ) {
        this.id = id;
        this.eventType = eventType;
        this.actorUUID = actorUUID;
        this.actorName = actorName;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.amount = amount;
        this.createdAt = createdAt;
        this.details = details;
    }

    public long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getActorUUID() {
        return actorUUID;
    }

    public String getActorName() {
        return actorName;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public String getTargetName() {
        return targetName;
    }

    public double getAmount() {
        return amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getDetails() {
        return details;
    }
}
