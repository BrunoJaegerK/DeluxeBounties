
package com.brunojaegerk.deluxebounties;

import cn.nukkit.plugin.PluginBase;
import com.brunojaegerk.deluxebounties.database.DatabaseManager;
import com.brunojaegerk.deluxebounties.economy.EconomyManager;
import com.brunojaegerk.deluxebounties.manager.BountyService;
import com.brunojaegerk.deluxebounties.command.BountyCommand;
import com.brunojaegerk.deluxebounties.listener.BountyListener;

import java.sql.SQLException;

public final class Main extends PluginBase {

    private static Main instance;

    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private BountyService bountyService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultResources();

        getLogger().info("§d================================");
        getLogger().info("§5       DeluxeBounties");
        getLogger().info("§7  Advanced Bounty System");
        getLogger().info("§d================================");

        // Inicializar H2
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();

            getLogger().info("§aBase de datos H2 inicializada.");

        } catch (SQLException exception) {
            getLogger().error(
                    "No se pudo inicializar DeluxeBounties.",
                    exception
            );

            throw new IllegalStateException(
                    "Error al inicializar la base de datos.",
                    exception
            );
        }

        // Inicializar EconomyAPI
        try {
            economyManager = new EconomyManager(this);

            getLogger().info("§aProveedor económico conectado: " + economyManager.getProvider()
                    + (economyManager.getCurrencyId().isEmpty() ? "" : " (" + economyManager.getCurrencyId() + ")"));

        } catch (IllegalStateException exception) {
            economyManager = null;

            getLogger().warning(
                    "El proveedor económico no está disponible. "
                            + "Las operaciones económicas estarán desactivadas."
            );
        }

        bountyService = new BountyService(this);

        getLogger().info("§aBountyService inicializado correctamente.");

        getServer().getPluginManager().registerEvents(
                new BountyListener(this),
                this
        );

        getLogger().info("§aBountyListener registrado correctamente.");

        getLogger().info("§aDeluxeBounties habilitado correctamente.");
        getLogger().info(
                "§7Versión: " + getDescription().getVersion()
        );

        getServer().getCommandMap().register(
                "deluxebounties",
                new BountyCommand(this)
        );

        getLogger().info("§aComando /bounty registrado correctamente.");
    }

    @Override
    public void onDisable() {

        if (databaseManager != null) {
            try {
                databaseManager.close();

            } catch (SQLException exception) {
                getLogger().error(
                        "Error al cerrar la base de datos.",
                        exception
                );
            }
        }

        getLogger().info("§cDeluxeBounties deshabilitado.");

        instance = null;
    }

    private void saveDefaultResources() {
        saveResource("config.yml", false);
        saveResource("lang.yml", false);
    }

    public static Main getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public boolean isEconomyAvailable() {
        return economyManager != null;
    }

    public String formatAmount(double amount) {
        if (economyManager != null) {
            return economyManager.formatAmount(amount);
        }
        String symbol = getConfig().getString("economy.currency-symbol", "$");
        return symbol + String.format(java.util.Locale.US, "%,.2f", amount);
    }

    public String formatAmount(double amount, String provider, String currencyId) {
        if (economyManager != null) {
            return economyManager.formatAmount(amount, provider, currencyId);
        }
        return getConfig().getString("economy.currency-symbol", "$")
                + String.format(java.util.Locale.US, "%,.2f", amount);
    }

    public String getConfiguredCurrencyLabel() {
        if (economyManager != null) {
            return economyManager.getCurrencySymbol()
                    + (economyManager.getProvider().equalsIgnoreCase("EconomyExpandNK")
                    ? " " + economyManager.getCurrencyId() : "dinero");
        }
        return getConfig().getString("economy.currency-symbol", "$") + "dinero";
    }

    public BountyService getBountyService() {
        return bountyService;
    }
}



