package com.brunojaegerk.deluxebounties.bounty;

import java.util.UUID;

public final class HunterStats {

    private final UUID playerUUID;
    private final String playerName;
    private final int totalClaims;
    private final double totalEarned;

    public HunterStats(
            UUID playerUUID,
            String playerName,
            int totalClaims,
            double totalEarned
    ) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.totalClaims = totalClaims;
        this.totalEarned = totalEarned;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getTotalClaims() {
        return totalClaims;
    }

    public double getTotalEarned() {
        return totalEarned;
    }
}
