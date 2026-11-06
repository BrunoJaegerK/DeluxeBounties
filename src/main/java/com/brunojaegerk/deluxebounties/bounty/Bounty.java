package com.brunojaegerk.bounty;

import java.util.UUID;

public final class Bounty {

    private final long id;

    private final UUID targetUUID;
    private final String targetName;

    private final UUID creatorUUID;
    private final String creatorName;

    private final double amount;

    private final boolean anonymous;

    private final BountyStatus status;

    private final long createdAt;

    public Bounty(
            long id,
            UUID targetUUID,
            String targetName,
            UUID creatorUUID,
            String creatorName,
            double amount,
            boolean anonymous,
            BountyStatus status,
            long createdAt
    ) {
        this.id = id;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.creatorUUID = creatorUUID;
        this.creatorName = creatorName;
        this.amount = amount;
        this.anonymous = anonymous;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getCreatorUUID() {
        return creatorUUID;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public BountyStatus getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

