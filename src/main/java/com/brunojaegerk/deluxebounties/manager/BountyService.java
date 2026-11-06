package com.brunojaegerk.deluxebounties.manager;

import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.Bounty;
import com.brunojaegerk.deluxebounties.database.DatabaseManager;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public final class BountyService {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    public BountyService(Main plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    public Bounty createBounty(
            UUID targetUUID,
            String targetName,
            UUID creatorUUID,
            String creatorName,
            double amount,
            boolean anonymous
    ) throws SQLException {

        // Validación básica
        if (targetUUID == null || creatorUUID == null) {
            throw new IllegalArgumentException(
                    "Los UUID no pueden ser nulos."
            );
        }

        if (targetName == null || creatorName == null
                || targetName.isBlank() || creatorName.isBlank()) {
            throw new IllegalArgumentException(
                    "Los nombres de los jugadores no son válidos."
            );
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "El importe debe ser mayor que cero."
            );
        }

        // Validar límites económicos
        double minimumAmount = getConfigNumber(
                "bounty.minimum-amount", 100
        );

        double maximumAmount = getConfigNumber(
                "bounty.maximum-amount", 10000000
        );

        if (amount < minimumAmount) {
            throw new IllegalArgumentException(
                    "El importe mínimo es " + minimumAmount + "."
            );
        }

        if (amount > maximumAmount) {
            throw new IllegalArgumentException(
                    "El importe máximo es " + maximumAmount + "."
            );
        }

        // Evitar recompensas a uno mismo
        if (plugin.getConfig().getBoolean(
                "antifarm.prevent-self-bounty", true)
                && targetUUID.equals(creatorUUID)) {

            throw new IllegalArgumentException(
                    "No puedes colocar una recompensa sobre ti mismo."
            );
        }

        // Validar recompensas anónimas
        boolean allowAnonymous = plugin.getConfig().getBoolean(
                "bounty.allow-anonymous", true
        );

        if (anonymous && !allowAnonymous) {
            throw new IllegalArgumentException(
                    "Las recompensas anónimas están desactivadas."
            );
        }

        // Comprobar límite de recompensas activas
        int maximumActive = plugin.getConfig().getInt(
                "bounty.maximum-active-per-player", 10
        );

        List<Bounty> activeBounties =
                databaseManager.getActiveBountiesByCreator(creatorUUID);

        if (activeBounties.size() >= maximumActive) {
            throw new IllegalStateException(
                    "Has alcanzado el límite de recompensas activas."
            );
        }

        // Comprobar disponibilidad de EconomyAPI
        if (!plugin.isEconomyAvailable()) {
            throw new IllegalStateException(
                    "El sistema económico no está disponible."
            );
        }

        // Comprobar saldo
        if (!plugin.getEconomyManager().hasBalance(
                creatorUUID, amount)) {

            throw new IllegalStateException(
                    "No tienes suficiente dinero para esta recompensa."
            );
        }

        // Retirar dinero antes de registrar la recompensa
        boolean withdrawn = plugin.getEconomyManager().withdraw(
                creatorUUID, amount
        );

        if (!withdrawn) {
            throw new IllegalStateException(
                    "No se pudo retirar el dinero de tu cuenta."
            );
        }

        // Registrar la recompensa en SQLite
        try {
            Bounty bounty = databaseManager.createBounty(
                    targetUUID,
                    targetName,
                    creatorUUID,
                    creatorName,
                    amount,
                    anonymous
            );

            plugin.getLogger().info(
                    "Nueva recompensa #" + bounty.getId()
                            + " creada sobre " + targetName
                            + " por " + amount + "."
            );

            return bounty;

        } catch (SQLException | RuntimeException exception) {

            // Si SQLite falla, intentar devolver el dinero
            boolean refunded = false;

            try {
                refunded = plugin.getEconomyManager().deposit(
                        creatorUUID, amount
                );
            } catch (RuntimeException refundException) {
                plugin.getLogger().error(
                        "Error al intentar reembolsar una recompensa.",
                        refundException
                );
            }

            if (!refunded) {
                plugin.getLogger().error(
                        "¡ATENCIÓN! No se pudo reembolsar "
                                + amount + " al jugador " + creatorName
                                + " (UUID: " + creatorUUID + ")."
                );
            }

            throw exception;
        }
    }

    private double getConfigNumber(String path, double defaultValue) {
        Object value = plugin.getConfig().get(path, defaultValue);

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return defaultValue;
    }
}

