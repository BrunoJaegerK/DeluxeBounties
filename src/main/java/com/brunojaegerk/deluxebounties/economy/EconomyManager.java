package com.brunojaegerk.deluxebounties.economy;

import me.onebone.economyapi.EconomyAPI;

import java.util.UUID;

public final class EconomyManager {

    private final EconomyAPI economyAPI;

    public EconomyManager() {
        this.economyAPI = EconomyAPI.getInstance();

        if (this.economyAPI == null) {
            throw new IllegalStateException(
                    "EconomyAPI no está disponible."
            );
        }
    }

    public double getBalance(UUID playerUUID) {
        if (playerUUID == null || !economyAPI.hasAccount(playerUUID)) {
            return -1;
        }

        return economyAPI.myMoney(playerUUID);
    }

    public boolean hasBalance(UUID playerUUID, double amount) {
        if (!isValidAmount(amount)) {
            return false;
        }

        double balance = getBalance(playerUUID);

        return balance >= amount;
    }

    public boolean withdraw(UUID playerUUID, double amount) {
        if (playerUUID == null || !isValidAmount(amount)) {
            return false;
        }

        if (!hasBalance(playerUUID, amount)) {
            return false;
        }

        return economyAPI.reduceMoney(playerUUID, amount)
                == EconomyAPI.RET_SUCCESS;
    }

    public boolean deposit(UUID playerUUID, double amount) {
        if (playerUUID == null || !isValidAmount(amount)) {
            return false;
        }

        return economyAPI.addMoney(playerUUID, amount)
                == EconomyAPI.RET_SUCCESS;
    }

    private boolean isValidAmount(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }
}