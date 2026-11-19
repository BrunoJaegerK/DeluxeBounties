package com.brunojaegerk.deluxebounties.economy;

import cn.nukkit.plugin.Plugin;
import com.brunojaegerk.deluxebounties.Main;
import me.onebone.economyapi.EconomyAPI;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

public final class EconomyManager {

    private final EconomyAPI economyAPI;
    private final Object secondaryEconomy;
    private final Class<?> secondaryEconomyApi;
    private final Object currency;
    private final String provider;
    private final String currencyId;

    public EconomyManager(Main plugin) {
        this.provider = plugin.getConfig()
                .getString("economy.provider", "EconomyAPI").trim();
        this.currencyId = plugin.getConfig()
                .getString("economy.currency-id", "crystals").trim();

        if ("EconomyExpandNK".equalsIgnoreCase(provider)) {
            Plugin dependency = plugin.getServer().getPluginManager()
                    .getPlugin("EconomyExpandNK");
            if (dependency == null || !dependency.isEnabled()) {
                throw new IllegalStateException(
                        "EconomyExpandNK no está instalado o no se encuentra habilitado."
                );
            }

            try {
                ClassLoader loader = dependency.getClass().getClassLoader();
                Class<?> pluginClass = dependency.getClass();
                Object api = pluginClass.getMethod("getEconomy").invoke(dependency);
                if (api == null) {
                    throw new IllegalStateException(
                            "La API de EconomyExpandNK todavía no está disponible."
                    );
                }

                // Las llamadas se realizan mediante la interfaz pública, nunca
                // directamente sobre la implementación interna del proveedor.
                this.secondaryEconomyApi = Class.forName(
                        "com.bruno.economyexpand.api.SecondaryEconomy", true, loader
                );
                this.secondaryEconomy = api;

                this.currency = secondaryEconomyApi
                        .getMethod("getCurrency", String.class)
                        .invoke(secondaryEconomy, currencyId);

                if (currency == null
                        || !((Boolean) invokeCurrency("isEnabled")).booleanValue()) {
                    throw new IllegalStateException(
                            "La moneda '" + currencyId
                                    + "' no existe o está deshabilitada en EconomyExpandNK."
                    );
                }

                this.economyAPI = null;
            } catch (IllegalStateException exception) {
                throw exception;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "No se pudo conectar con la API pública de EconomyExpandNK.",
                        unwrap(exception)
                );
            }
        } else if ("EconomyAPI".equalsIgnoreCase(provider)) {
            this.economyAPI = EconomyAPI.getInstance();
            if (this.economyAPI == null) {
                throw new IllegalStateException("EconomyAPI no está disponible.");
            }

            this.secondaryEconomy = null;
            this.secondaryEconomyApi = null;
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
        return "EconomyExpandNK".equalsIgnoreCase(provider) ? currencyId : "";
    }

    public String getCurrencySymbol() {
        if (currency != null) {
            return String.valueOf(invokeCurrency("getSymbol"));
        }
        return Main.getInstance().getConfig()
                .getString("economy.currency-symbol", "$");
    }

    public String formatAmount(double amount) {
        if (currency != null) {
            return getCurrencySymbol() + invokeCurrency("format",
                    new Class<?>[]{BigDecimal.class},
                    new Object[]{BigDecimal.valueOf(amount)});
        }

        return getCurrencySymbol()
                + String.format(Locale.US, "%,.2f", amount);
    }

    public double normalizeAmount(double amount) {
        if (currency == null) {
            return amount;
        }

        int decimals = ((Number) invokeCurrency("getDecimals")).intValue();
        BigDecimal original = BigDecimal.valueOf(amount);
        BigDecimal normalized = original.setScale(decimals, RoundingMode.DOWN);
        if (normalized.compareTo(original) != 0) {
            String name = String.valueOf(invokeCurrency("getName"));
            throw new IllegalArgumentException(
                    "La moneda " + name + " admite como máximo "
                            + decimals + " decimales."
            );
        }
        return normalized.doubleValue();
    }

    public double getBalance(UUID playerUUID) {
        if (playerUUID == null) {
            return -1;
        }

        if (secondaryEconomy != null) {
            return ((BigDecimal) invokeEconomy(
                    "getBalance",
                    new Class<?>[]{UUID.class, String.class},
                    new Object[]{playerUUID, currencyId}
            )).doubleValue();
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
            return (Boolean) invokeEconomy(
                    "withdraw",
                    new Class<?>[]{UUID.class, String.class, BigDecimal.class},
                    new Object[]{playerUUID, currencyId, BigDecimal.valueOf(amount)}
            );
        }

        return economyAPI.reduceMoney(playerUUID, amount) == EconomyAPI.RET_SUCCESS;
    }

    public boolean deposit(UUID playerUUID, double amount) {
        if (playerUUID == null || !isValidAmount(amount)) {
            return false;
        }

        if (secondaryEconomy != null) {
            return (Boolean) invokeEconomy(
                    "deposit",
                    new Class<?>[]{UUID.class, String.class, BigDecimal.class},
                    new Object[]{playerUUID, currencyId, BigDecimal.valueOf(amount)}
            );
        }

        return economyAPI.addMoney(playerUUID, amount) == EconomyAPI.RET_SUCCESS;
    }

    private Object invokeEconomy(
            String method,
            Class<?>[] parameterTypes,
            Object[] arguments
    ) {
        try {
            Method apiMethod = secondaryEconomyApi.getMethod(method, parameterTypes);
            return apiMethod.invoke(secondaryEconomy, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Falló la operación '" + method + "' en EconomyExpandNK.",
                    unwrap(exception)
            );
        }
    }

    private Object invokeCurrency(String method) {
        return invokeCurrency(method, new Class<?>[0], new Object[0]);
    }

    private Object invokeCurrency(
            String method,
            Class<?>[] parameterTypes,
            Object[] arguments
    ) {
        try {
            Method currencyMethod = currency.getClass()
                    .getMethod(method, parameterTypes);
            return currencyMethod.invoke(currency, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "No se pudo consultar la moneda configurada en EconomyExpandNK.",
                    unwrap(exception)
            );
        }
    }

    private static Throwable unwrap(ReflectiveOperationException exception) {
        if (exception instanceof InvocationTargetException
                && ((InvocationTargetException) exception).getCause() != null) {
            return ((InvocationTargetException) exception).getCause();
        }
        return exception;
    }

    private boolean isValidAmount(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }
}
