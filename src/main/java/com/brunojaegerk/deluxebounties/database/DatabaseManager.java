package com.brunojaegerk.deluxebounties.database;

import cn.nukkit.utils.Config;
import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.Bounty;
import com.brunojaegerk.deluxebounties.bounty.BountyStatus;
import com.brunojaegerk.deluxebounties.bounty.BountyClaimResult;
import com.brunojaegerk.deluxebounties.bounty.HunterStats;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.IOException;


public final class DatabaseManager {

    private final Main plugin;

    private Connection connection;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public synchronized void initialize() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("No se encontró el controlador SQLite JDBC.", exception);
        }

        if (!plugin.getDataFolder().exists()
                && !plugin.getDataFolder().mkdirs()) {
            throw new SQLException("No se pudo crear la carpeta del plugin.");
        }

        Config config = plugin.getConfig();
        String databaseName = config.getString(
                "database.file",
                "bounties.db"
        );


        File databaseFile = new File(plugin.getDataFolder(), databaseName);

        try {
            File canonicalDatabase = databaseFile.getCanonicalFile();
            File canonicalFolder = plugin.getDataFolder().getCanonicalFile();

            if (!canonicalDatabase.toPath().startsWith(canonicalFolder.toPath())) {
                throw new SQLException("Ruta de base de datos no válida.");
            }

            databaseFile = canonicalDatabase;

        } catch (IOException exception) {
            throw new SQLException(
                    "No se pudo validar la ruta de la base de datos.",
                    exception
            );
        }


        connection = DriverManager.getConnection(
                "jdbc:sqlite:" + databaseFile.getAbsolutePath()
        );

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
        }

        createTables();

        plugin.getLogger().info("§aBase de datos SQLite inicializada.");
    }

    private void createTables() throws SQLException {

        String bountiesTable = """
                CREATE TABLE IF NOT EXISTS bounties (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_uuid TEXT NOT NULL,
                    target_name TEXT NOT NULL,
                    creator_uuid TEXT NOT NULL,
                    creator_name TEXT NOT NULL,
                    amount REAL NOT NULL CHECK(amount > 0),
                    anonymous INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    created_at INTEGER NOT NULL,
                    claimed_by_uuid TEXT,
                    claimed_by_name TEXT,
                    claimed_at INTEGER
                )
                """;

        String historyTable = """
                CREATE TABLE IF NOT EXISTS bounty_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_type TEXT NOT NULL,
                    actor_uuid TEXT,
                    actor_name TEXT,
                    target_uuid TEXT,
                    target_name TEXT,
                    amount REAL NOT NULL,
                    created_at INTEGER NOT NULL,
                    details TEXT
                )
                """;

        String hunterStatsTable = """
                CREATE TABLE IF NOT EXISTS hunter_stats (
                    player_uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    total_claims INTEGER NOT NULL DEFAULT 0,
                    total_earned REAL NOT NULL DEFAULT 0,
                    updated_at INTEGER NOT NULL
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(bountiesTable);
            statement.execute(historyTable);
            statement.execute(hunterStatsTable);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_bounty_target
                    ON bounties(target_uuid, status)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_bounty_creator
                    ON bounties(creator_uuid, status)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_history_actor
                    ON bounty_history(actor_uuid)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_history_target
                    ON bounty_history(target_uuid)
                    """);
        }
    }

    public synchronized Bounty createBounty(
            UUID targetUUID,
            String targetName,
            UUID creatorUUID,
            String creatorName,
            double amount,
            boolean anonymous
    ) throws SQLException {

        if (targetUUID == null || creatorUUID == null) {
            throw new IllegalArgumentException("Los UUID no pueden ser nulos.");
        }

        if (targetName == null || creatorName == null
                || !Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Datos de recompensa no válidos.");
        }

        boolean previousAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            long createdAt = System.currentTimeMillis();
            long id;

            String sql = """
                    INSERT INTO bounties (
                        target_uuid,
                        target_name,
                        creator_uuid,
                        creator_name,
                        amount,
                        anonymous,
                        status,
                        created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 sql,
                                 Statement.RETURN_GENERATED_KEYS
                         )) {

                statement.setString(1, targetUUID.toString());
                statement.setString(2, targetName);
                statement.setString(3, creatorUUID.toString());
                statement.setString(4, creatorName);
                statement.setDouble(5, amount);
                statement.setInt(6, anonymous ? 1 : 0);
                statement.setLong(7, createdAt);

                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("No se pudo obtener el ID de la recompensa.");
                    }

                    id = keys.getLong(1);
                }
            }

            insertHistory(
                    "CREATED",
                    creatorUUID,
                    creatorName,
                    targetUUID,
                    targetName,
                    amount,
                    "Recompensa creada"
            );

            connection.commit();

            return new Bounty(
                    id,
                    targetUUID,
                    targetName,
                    creatorUUID,
                    creatorName,
                    amount,
                    anonymous,
                    BountyStatus.ACTIVE,
                    createdAt
            );

        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public synchronized List<Bounty> getActiveBountiesForTarget(
            UUID targetUUID
    ) throws SQLException {

        List<Bounty> bounties = new ArrayList<>();

        String sql = """
                SELECT * FROM bounties
                WHERE target_uuid = ? AND status = 'ACTIVE'
                ORDER BY created_at ASC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, targetUUID.toString());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    bounties.add(mapBounty(result));
                }
            }
        }

        return bounties;
    }

    public synchronized List<Bounty> getAllActiveBounties(
            int limit,
            int offset
    ) throws SQLException {

        if (limit <= 0 || limit > 100 || offset < 0) {
            throw new IllegalArgumentException(
                    "Los parámetros de paginación no son válidos."
            );
        }

        List<Bounty> bounties = new ArrayList<>();

        String sql = """
            SELECT *
            FROM bounties
            WHERE status = 'ACTIVE'
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);
            statement.setInt(2, offset);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    bounties.add(mapBounty(result));
                }
            }
        }

        return bounties;
    }

    public synchronized List<Bounty> getActiveBountiesByCreator(
            UUID creatorUUID
    ) throws SQLException {

        List<Bounty> bounties = new ArrayList<>();

        String sql = """
                SELECT * FROM bounties
                WHERE creator_uuid = ? AND status = 'ACTIVE'
                ORDER BY created_at DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, creatorUUID.toString());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    bounties.add(mapBounty(result));
                }
            }
        }

        return bounties;
    }

    public synchronized Bounty getActiveBountyById(
            long bountyId,
            UUID creatorUUID
    ) throws SQLException {

        String sql = """
            SELECT *
            FROM bounties
            WHERE id = ?
            AND creator_uuid = ?
            AND status = 'ACTIVE'
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, bountyId);
            statement.setString(2, creatorUUID.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapBounty(result);
                }
            }
        }

        return null;
    }

    public synchronized double getActiveBountyTotal(
            UUID targetUUID
    ) throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM bounties
                WHERE target_uuid = ? AND status = 'ACTIVE'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, targetUUID.toString());

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getDouble(1) : 0.0;
            }
        }
    }

    public synchronized boolean cancelBounty(
            long bountyId,
            UUID creatorUUID
    ) throws SQLException {

        boolean previousAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            String selectSql = """
                    SELECT target_uuid, target_name, creator_name, amount
                    FROM bounties
                    WHERE id = ?
                    AND creator_uuid = ?
                    AND status = 'ACTIVE'
                    """;

            String targetUUID;
            String targetName;
            String creatorName;
            double amount;

            try (PreparedStatement statement =
                         connection.prepareStatement(selectSql)) {

                statement.setLong(1, bountyId);
                statement.setString(2, creatorUUID.toString());

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        connection.rollback();
                        return false;
                    }

                    targetUUID = result.getString("target_uuid");
                    targetName = result.getString("target_name");
                    creatorName = result.getString("creator_name");
                    amount = result.getDouble("amount");
                }
            }

            String updateSql = """
                    UPDATE bounties
                    SET status = 'CANCELLED'
                    WHERE id = ?
                    AND creator_uuid = ?
                    AND status = 'ACTIVE'
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(updateSql)) {

                statement.setLong(1, bountyId);
                statement.setString(2, creatorUUID.toString());

                if (statement.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
            }

            insertHistory(
                    "CANCELLED",
                    creatorUUID,
                    creatorName,
                    UUID.fromString(targetUUID),
                    targetName,
                    amount,
                    "Aportación cancelada"
            );

            connection.commit();
            return true;

        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private void insertHistory(
            String eventType,
            UUID actorUUID,
            String actorName,
            UUID targetUUID,
            String targetName,
            double amount,
            String details
    ) throws SQLException {

        String sql = """
                INSERT INTO bounty_history (
                    event_type,
                    actor_uuid,
                    actor_name,
                    target_uuid,
                    target_name,
                    amount,
                    created_at,
                    details
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, eventType);
            statement.setString(2, actorUUID.toString());
            statement.setString(3, actorName);
            statement.setString(4, targetUUID.toString());
            statement.setString(5, targetName);
            statement.setDouble(6, amount);
            statement.setLong(7, System.currentTimeMillis());
            statement.setString(8, details);

            statement.executeUpdate();
        }
    }

    private Bounty mapBounty(ResultSet result) throws SQLException {

        return new Bounty(
                result.getLong("id"),
                UUID.fromString(result.getString("target_uuid")),
                result.getString("target_name"),
                UUID.fromString(result.getString("creator_uuid")),
                result.getString("creator_name"),
                result.getDouble("amount"),
                result.getInt("anonymous") == 1,
                BountyStatus.valueOf(result.getString("status")),
                result.getLong("created_at")
        );
    }

    public synchronized BountyClaimResult claimBountiesForTarget(
            UUID targetUUID,
            UUID hunterUUID,
            String hunterName
    ) throws SQLException {

        if (targetUUID == null || hunterUUID == null
                || hunterName == null || hunterName.isBlank()) {
            throw new IllegalArgumentException(
                    "Los datos de reclamación no son válidos."
            );
        }

        if (targetUUID.equals(hunterUUID)) {
            throw new IllegalArgumentException(
                    "Un jugador no puede reclamar su propia recompensa."
            );
        }

        boolean previousAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            String selectSql = """
                SELECT id, target_name, amount
                FROM bounties
                WHERE target_uuid = ?
                AND status = 'ACTIVE'
                ORDER BY created_at ASC, id ASC
                """;

            List<Long> bountyIds = new ArrayList<>();
            List<String> targetNames = new ArrayList<>();
            List<Double> amounts = new ArrayList<>();

            double totalEarned = 0.0;

            try (PreparedStatement statement =
                         connection.prepareStatement(selectSql)) {

                statement.setString(1, targetUUID.toString());

                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        double amount = result.getDouble("amount");

                        if (!Double.isFinite(amount) || amount <= 0) {
                            throw new SQLException(
                                    "Se encontró una recompensa con un importe inválido."
                            );
                        }

                        bountyIds.add(result.getLong("id"));
                        targetNames.add(result.getString("target_name"));
                        amounts.add(amount);

                        totalEarned += amount;
                    }
                }
            }

            if (bountyIds.isEmpty()) {
                connection.rollback();
                return new BountyClaimResult(0, 0.0);
            }

            if (!Double.isFinite(totalEarned) || totalEarned <= 0) {
                throw new SQLException(
                        "El importe total de las recompensas no es válido."
                );
            }

            long claimedAt = System.currentTimeMillis();

            String updateSql = """
                UPDATE bounties
                SET status = 'CLAIMED',
                    claimed_by_uuid = ?,
                    claimed_by_name = ?,
                    claimed_at = ?
                WHERE id = ?
                AND status = 'ACTIVE'
                """;

            for (int i = 0; i < bountyIds.size(); i++) {

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setString(1, hunterUUID.toString());
                    statement.setString(2, hunterName);
                    statement.setLong(3, claimedAt);
                    statement.setLong(4, bountyIds.get(i));

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "No se pudo actualizar la recompensa #"
                                        + bountyIds.get(i)
                        );
                    }
                }

                insertHistory(
                        "CLAIMED",
                        hunterUUID,
                        hunterName,
                        targetUUID,
                        targetNames.get(i),
                        amounts.get(i),
                        "Recompensa #" + bountyIds.get(i)
                                + " reclamada por " + hunterName
                );
            }

            String statsSql = """
                INSERT INTO hunter_stats (
                    player_uuid,
                    player_name,
                    total_claims,
                    total_earned,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    player_name = excluded.player_name,
                    total_claims = hunter_stats.total_claims
                            + excluded.total_claims,
                    total_earned = hunter_stats.total_earned
                            + excluded.total_earned,
                    updated_at = excluded.updated_at
                """;

            try (PreparedStatement statement =
                         connection.prepareStatement(statsSql)) {

                statement.setString(1, hunterUUID.toString());
                statement.setString(2, hunterName);
                statement.setInt(3, bountyIds.size());
                statement.setDouble(4, totalEarned);
                statement.setLong(5, claimedAt);

                statement.executeUpdate();
            }

            connection.commit();

            return new BountyClaimResult(
                    bountyIds.size(),
                    totalEarned
            );

        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;

        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public synchronized List<HunterStats> getTopHunters(
            int limit
    ) throws SQLException {

        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException(
                    "El límite del ranking no es válido."
            );
        }

        List<HunterStats> hunters = new ArrayList<>();

        String sql = """
            SELECT
                player_uuid,
                player_name,
                total_claims,
                total_earned
            FROM hunter_stats
            WHERE total_claims > 0
            ORDER BY
                total_claims DESC,
                total_earned DESC,
                player_name ASC
            LIMIT ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    hunters.add(new HunterStats(
                            UUID.fromString(result.getString("player_uuid")),
                            result.getString("player_name"),
                            result.getInt("total_claims"),
                            result.getDouble("total_earned")
                    ));
                }
            }
        }

        return hunters;
    }

    public synchronized void close() throws SQLException {

        if (connection != null && !connection.isClosed()) {
            connection.close();
            plugin.getLogger().info("§cConexión SQLite cerrada.");
        }
    }
}

