package com.brunojaegerk.deluxebounties;

import cn.nukkit.plugin.PluginBase;
import com.brunojaegerk.deluxebounties.database.DatabaseManager;

import java.sql.SQLException;

public final class Main extends PluginBase {

    private static Main instance;

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultResources();

        getLogger().info("§d================================");
        getLogger().info("§5       DeluxeBounties");
        getLogger().info("§7  Advanced Bounty System");
        getLogger().info("§d================================");

        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();

            getLogger().info("§aPlugin habilitado correctamente.");
            getLogger().info("§7Versión: " + getDescription().getVersion());

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
}


