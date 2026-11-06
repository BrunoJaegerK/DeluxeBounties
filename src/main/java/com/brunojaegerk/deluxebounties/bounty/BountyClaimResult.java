package com.brunojaegerk.deluxebounties.bounty;

public final class BountyClaimResult {

    private final int totalClaims;
    private final double totalEarned;

    public BountyClaimResult(int totalClaims, double totalEarned) {
        this.totalClaims = totalClaims;
        this.totalEarned = totalEarned;
    }

    public int getTotalClaims() {
        return totalClaims;
    }

    public double getTotalEarned() {
        return totalEarned;
    }

    public boolean hasClaims() {
        return totalClaims > 0;
    }
}
