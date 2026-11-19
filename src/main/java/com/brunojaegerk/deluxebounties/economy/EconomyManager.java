package com.brunojaegerk.deluxebounties.economy;

import cn.nukkit.plugin.Plugin;
import com.brunojaegerk.deluxebounties.Main;
import com.bruno.economyexpand.EconomyExpandNK;
import com.bruno.economyexpand.api.Currency;
import com.bruno.economyexpand.api.SecondaryEconomy;
import me.onebone.economyapi.EconomyAPI;

import java.math.BigDecimal;
import java.util.UUID;

public final class EconomyManager {

    private final EconomyAPI economyAPI;
    private final SecondaryEconomy secondaryEconomy;
    private final String provider;
    private final String currencyId;
    private final Currency currency;

    public EconomyManager(Main plugin) {
        this.provider = plugin.getConfig().getString("economy.provider", "EconomyAPI")
                .trim();
        this.currencyId = plugin.getConfig().getString("economy.currency-id", "crystals")
                .trim();

        if ("EconomyExpandNK".equalsIgnoreCase(provider)) {
            Plugin dependency = plugin.getServer().getPluginManager()
                    .getPlugin("EconomyExpandNK");

            if (!(dependency instanceof EconomyExpandNK)) {
                throw new IllegalStateException(
                        "EconomyExpandNK no está instalado o no se encuentra habilitado."
                );
            }

            this.secondaryEconomy = ((EconomyExpandNK) dependency).getEconomy();
            if (this.secondaryEconomy == null) {
                throw new IllegalStateException(
                        "La API de EconomyExpandNK todavía no está disponible."
                );
            }

            this.currency = secondaryEconomy.getCurrency(currencyId);
            if (currency == null || !currency.isEnabled()) {
                throw new IllegalStateException(
                        "La moneda '" + currencyId
                                + "' no existe o está deshabilitada en EconomyExpandNK."
                );
            }

            this.economyAPI = null;
        } else if ("EconomyAPI".equalsIgnoreCase(provider)) {
            this.economyAPI = EconomyAPI.getInstance();
            if (this.economyAPI == null) {
                throw new IllegalStateException("EconomyAPI no está disponible.");
            }

            this.secondaryEconomy = null;
            this.currency = null;
        } else {
            throw new IllegalStateException(
                    "Proveedor económico desconocido: " + provider
                            + ". Valores permitidos: EconomyAPI, EconomyExpandNK."
            );
        }
    }

    public String getProvider() {
        return provider;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public String getCurrencySymbol() {
        return currency != null
                ? currency.getSymbol()
                : Main.getInstance().getConfig().getString("economy.currency-symbol", "$");
    }

    public String formatAmount(double amount) {
        if (currency != null) {
            return getCurrencySymbol()
                    + currency.format(BigDecimal.valueOf(amount));
        }

        return getCurrencySymbol()
                + String.format(java.util.Locale.US, "%,.2f", amount);
    }

    public double normalizeAmount(double amount) {
        if (currency == null) {
            return amount;
        }

        BigDecimal original = BigDecimal.valueOf(amount);
        BigDecimal normalized = currency.normalize(original);
        if (normalized.compareTo(original) != 0) {
            throw new IllegalArgumentException(
                    "La moneda " + currency.getName()
                            + " admite como máximo " + currency.getDecimals()
                            + " decimales."
            );
        }
        return normalized.doubleValue();
    }

    public double getBalance(UUID playerUUID) {
        if (playerUUID == null) {
            return -1;
        }

        if (secondaryEconomy != null) {
            return secondaryEconomy.getBalance(playerUUID, currencyId).doubleValue();
        }

        if (!economyAPI.hasAccount(playerUUID)) {
            return -1;
        }
        return economyAPI.myMoney(playerUUID);
    }

    public boolean hasBalance(UUID playerUUID, double amount) {
        if (!isValidAmount(amount)) {
            return false;
        }
        return getBalance(playerUUID) >= amount;
    }

    public boolean withdraw(UUID playerUUID, double amount) {
        if (playerUUID == null || !isValidAmount(amount)
                || !hasBalance(playerUUID, amount)) {
            return false;
        }

        if (secondaryEconomy != null) {
            return secondaryEconomy.withdraw(
                    playerUUID, currencyId, BigDecimal.valueOf(amount)
            );
        }

        return economyAPI.reduceMoney(playerUUID, amount) == EconomyAPI.RET_SUCCESS;
    }

    public boolean deposit(UUID playerUUID, double amount) {
        if (playerUUID == null || !isValidAmount(amount)) {
            return false;
        }

        if (secondaryEconomy != null) {
            return secondaryEconomy.deposit(
                    playerUUID, currencyId, BigDecimal.valueOf(amount)
            );
        }

        return economyAPI.addMoney(playerUUID, amount) == EconomyAPI.RET_SUCCESS;
    }

    private boolean isValidAmount(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }
}
