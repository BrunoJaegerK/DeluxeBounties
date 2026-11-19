package com.brunojaegerk.deluxebounties.economy;

import cn.nukkit.plugin.Plugin;
import com.brunojaegerk.deluxebounties.Main;
import me.onebone.economyapi.EconomyAPI;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * EconomyAPI remains the default provider. EconomyExpandNK is discovered
 * optionally and accessed only through its public API interface.
 */
public final class EconomyManager {

    public static final String ECONOMY_API = "EconomyAPI";
    public static final String ECONOMY_EXPAND = "EconomyExpandNK";

    private final Main plugin;
    private final EconomyAPI economyAPI;
    private final Object secondaryEconomy;
    private final Class<?> secondaryEconomyApi;
    private final Class<?> currencyApi;
    private final List<CurrencyOption> currencies;

    public EconomyManager(Main plugin) {
        this.plugin = plugin;
        this.economyAPI = EconomyAPI.getInstance();
        this.currencies = new ArrayList<>();
        currencies.add(new CurrencyOption(ECONOMY_API, "", "Dinero", "$"));

        Object api = null;
        Class<?> apiClass = null;
        Class<?> currencyClass = null;
        Plugin dependency = plugin.getServer().getPluginManager()
                .getPlugin(ECONOMY_EXPAND);

        if (dependency != null && dependency.isEnabled()) {
            try {
                ClassLoader loader = dependency.getClass().getClassLoader();
                api = dependency.getClass().getMethod("getEconomy").invoke(dependency);
                apiClass = Class.forName(
                        "com.bruno.economyexpand.api.SecondaryEconomy", true, loader);
                currencyClass = Class.forName(
                        "com.bruno.economyexpand.api.Currency", true, loader);

                if (api != null) {
                    Method getCurrencies = apiClass.getMethod("getCurrencies");
                    Collection<?> found = (Collection<?>) getCurrencies.invoke(api);
                    for (Object currency : found) {
                        Method enabled = currencyClass.getMethod("isEnabled");
                        if (!Boolean.TRUE.equals(enabled.invoke(currency))) continue;
                        String id = String.valueOf(currencyClass.getMethod("getId").invoke(currency));
                        String name = String.valueOf(currencyClass.getMethod("getName").invoke(currency));
                        String symbol = String.valueOf(currencyClass.getMethod("getSymbol").invoke(currency));
                        currencies.add(new CurrencyOption(ECONOMY_EXPAND, id, name, symbol));
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                plugin.getLogger().warning(
                        "No se pudo cargar la lista de monedas de EconomyExpandNK: "
                                + exception.getMessage());
                api = null;
                apiClass = null;
                currencyClass = null;
                currencies.removeIf(option -> ECONOMY_EXPAND.equals(option.getProvider()));
            }
        }

        this.secondaryEconomy = api;
        this.secondaryEconomyApi = apiClass;
        this.currencyApi = currencyClass;

        String defaultProvider = plugin.getConfig()
                .getString("economy.provider", ECONOMY_API);
        String defaultCurrency = plugin.getConfig()
                .getString("economy.currency-id", "");
        if (ECONOMY_EXPAND.equalsIgnoreCase(defaultProvider)
                && getCurrencyOption(ECONOMY_EXPAND, defaultCurrency) == null) {
            plugin.getLogger().warning(
                    "La moneda configurada no está disponible; se usará EconomyAPI como predeterminada.");
        }
    }

    public String getProvider() {
        return plugin.getConfig().getString("economy.provider", ECONOMY_API);
    }

    public String getCurrencyId() {
        return plugin.getConfig().getString("economy.currency-id", "");
    }

    public String getCurrencySymbol() {
        String provider = getProvider();
        if (ECONOMY_EXPAND.equalsIgnoreCase(provider)) {
            CurrencyOption option = getCurrencyOption(provider, getCurrencyId());
            return option == null ? "" : option.getSymbol();
        }
        return plugin.getConfig().getString("economy.currency-symbol", "$");
    }

    public List<CurrencyOption> getAvailableCurrencies() {
        return Collections.unmodifiableList(currencies);
    }

    public CurrencyOption getCurrencyOption(String provider, String currencyId) {
        for (CurrencyOption option : currencies) {
            if (option.getProvider().equalsIgnoreCase(provider)
                    && option.getCurrencyId().equalsIgnoreCase(currencyId == null ? "" : currencyId)) {
                return option;
            }
        }
        return null;
    }

    public boolean isAvailable(String provider, String currencyId) {
        if (ECONOMY_API.equalsIgnoreCase(provider)) return economyAPI != null;
        return ECONOMY_EXPAND.equalsIgnoreCase(provider)
                && secondaryEconomy != null
                && getCurrencyOption(provider, currencyId) != null;
    }

    public String formatAmount(double amount) {
        String provider = plugin.getConfig().getString("economy.provider", ECONOMY_API);
        String id = plugin.getConfig().getString("economy.currency-id", "");
        if (!isAvailable(provider, id)) {
            provider = ECONOMY_API;
            id = "";
        }
        return formatAmount(amount, provider, id);
    }

    public String formatAmount(double amount, String provider, String currencyId) {
        if (ECONOMY_EXPAND.equalsIgnoreCase(provider)) {
            Object currency = getCurrency(currencyId);
            Object formatted = invokeCurrency(currency, "format",
                    new Class<?>[]{BigDecimal.class},
                    new Object[]{BigDecimal.valueOf(amount)});
            Object symbol = invokeCurrency(currency, "getSymbol", new Class<?>[0], new Object[0]);
            return symbol + String.valueOf(formatted);
        }
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        return symbol + String.format(Locale.US, "%,.2f", amount);
    }

    public double normalizeAmount(double amount) {
        String provider = plugin.getConfig().getString("economy.provider", ECONOMY_API);
        String id = plugin.getConfig().getString("economy.currency-id", "");
        return normalizeAmount(amount, provider, id);
    }

    public double normalizeAmount(double amount, String provider, String currencyId) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor que cero.");
        }
        if (!ECONOMY_EXPAND.equalsIgnoreCase(provider)) return amount;
        Object currency = getCurrency(currencyId);
        int decimals = ((Number) invokeCurrency(currency, "getDecimals",
                new Class<?>[0], new Object[0])).intValue();
        BigDecimal original = BigDecimal.valueOf(amount);
        BigDecimal normalized = original.setScale(decimals, RoundingMode.DOWN);
        if (normalized.compareTo(original) != 0) {
            String name = String.valueOf(invokeCurrency(currency, "getName",
                    new Class<?>[0], new Object[0]));
            throw new IllegalArgumentException("La moneda " + name
                    + " admite como máximo " + decimals + " decimales.");
        }
        return normalized.doubleValue();
    }

    public double getBalance(UUID playerUUID) {
        String provider = plugin.getConfig().getString("economy.provider", ECONOMY_API);
        String id = plugin.getConfig().getString("economy.currency-id", "");
        return getBalance(playerUUID, provider, id);
    }

    public double getBalance(UUID playerUUID, String provider, String currencyId) {
        if (playerUUID == null || !isAvailable(provider, currencyId)) return -1;
        if (ECONOMY_EXPAND.equalsIgnoreCase(provider)) {
            Object value = invokeEconomy("getBalance",
                    new Class<?>[]{UUID.class, String.class},
                    new Object[]{playerUUID, currencyId});
            return ((BigDecimal) value).doubleValue();
        }
        if (!economyAPI.hasAccount(playerUUID)) return -1;
        return economyAPI.myMoney(playerUUID);
    }

    public boolean hasBalance(UUID playerUUID, double amount) {
        String provider = plugin.getConfig().getString("economy.provider", ECONOMY_API);
        String id = plugin.getConfig().getString("economy.currency-id", "");
        return hasBalance(playerUUID, amount, provider, id);
    }

    public boolean hasBalance(UUID playerUUID, double amount, String provider, String currencyId) {
        return isValidAmount(amount) && getBalance(playerUUID, provider, currencyId) >= amount;
    }

    public boolean withdraw(UUID playerUUID, double amount) {
        String provider = plugin.getConfig().getString("economy.provider", ECONOMY_API);
        String id = plugin.getConfig().getString("economy.currency-id", "");
        return withdraw(playerUUID, amount, provider, id);
    }

    public boolean withdraw(UUID playerUUID, double amount, String provider, String currencyId) {
        if (playerUUID == null || !isValidAmount(amount)
                || !hasBalance(playerUUID, amount, provider, currencyId)) return false;
        if (ECONOMY_EXPAND.equalsIgnoreCase(provider)) {
            return Boolean.TRUE.equals(invokeEconomy("withdraw",
                    new Class<?>[]{UUID.class, String.class, BigDecimal.class},
                    new Object[]{playerUUID, currencyId, BigDecimal.valueOf(amount)}));
        }
        return economyAPI.reduceMoney(playerUUID, amount) == EconomyAPI.RET_SUCCESS;
    }

    public boolean deposit(UUID playerUUID, double amount) {
        String provider = plugin.getConfig().getString("economy.provider", ECONOMY_API);
        String id = plugin.getConfig().getString("economy.currency-id", "");
        return deposit(playerUUID, amount, provider, id);
    }

    public boolean deposit(UUID playerUUID, double amount, String provider, String currencyId) {
        if (playerUUID == null || !isValidAmount(amount) || !isAvailable(provider, currencyId)) return false;
        if (ECONOMY_EXPAND.equalsIgnoreCase(provider)) {
            return Boolean.TRUE.equals(invokeEconomy("deposit",
                    new Class<?>[]{UUID.class, String.class, BigDecimal.class},
                    new Object[]{playerUUID, currencyId, BigDecimal.valueOf(amount)}));
        }
        return economyAPI.addMoney(playerUUID, amount) == EconomyAPI.RET_SUCCESS;
    }

    private Object getCurrency(String id) {
        if (secondaryEconomy == null || secondaryEconomyApi == null) {
            throw new IllegalStateException("EconomyExpandNK no está disponible.");
        }
        Object currency = invokeEconomy("getCurrency",
                new Class<?>[]{String.class}, new Object[]{id});
        if (currency == null
                || !Boolean.TRUE.equals(invokeCurrency(currency, "isEnabled",
                new Class<?>[0], new Object[0]))) {
            throw new IllegalArgumentException("La moneda '" + id
                    + "' no existe o está deshabilitada en EconomyExpandNK.");
        }
        return currency;
    }

    private Object invokeEconomy(String method, Class<?>[] types, Object[] args) {
        try {
            return secondaryEconomyApi.getMethod(method, types).invoke(secondaryEconomy, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Falló la operación '" + method
                    + "' en EconomyExpandNK.", unwrap(exception));
        }
    }

    private Object invokeCurrency(Object currency, String method, Class<?>[] types, Object[] args) {
        try {
            // Invoke through the public API class to avoid IllegalAccessException
            // against EconomyExpandNK's private implementation classes.
            return currencyApi.getMethod(method, types).invoke(currency, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("No se pudo consultar una moneda de EconomyExpandNK.",
                    unwrap(exception));
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

    public static final class CurrencyOption {
        private final String provider, currencyId, name, symbol;

        public CurrencyOption(String provider, String currencyId, String name, String symbol) {
            this.provider = provider;
            this.currencyId = currencyId;
            this.name = name;
            this.symbol = symbol;
        }
        public String getProvider() { return provider; }
        public String getCurrencyId() { return currencyId; }
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
        public String getLabel() {
            return ECONOMY_API.equals(provider)
                    ? "EconomyAPI (" + symbol + ")"
                    : name + " (" + symbol + ")";
        }
    }
}
