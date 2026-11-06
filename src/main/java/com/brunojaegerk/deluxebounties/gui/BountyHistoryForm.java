package com.brunojaegerk.deluxebounties.gui;

import cn.nukkit.Player;

import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.BountyHistoryEntry;
import com.formconstructor.form.SimpleForm;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class BountyHistoryForm {

    private static final int PAGE_SIZE = 8;

    private final Main plugin;

    public BountyHistoryForm(Main plugin) {
        this.plugin = plugin;
    }

    public void send(Player player) {
        send(player, 0);
    }

    private void send(Player player, int page) {

        int offset = Math.multiplyExact(page, PAGE_SIZE);

        List<BountyHistoryEntry> history;

        try {

            history = plugin.getBountyService().getPlayerHistory(
                    player.getUniqueId(),
                    PAGE_SIZE + 1,
                    offset
            );

        } catch (SQLException | IllegalArgumentException exception) {

            plugin.getLogger().error(
                    "No se pudo consultar el historial de recompensas.",
                    exception
            );

            player.sendMessage(
                    "§cNo se pudo cargar tu historial."
            );

            return;
        }

        boolean hasNextPage = history.size() > PAGE_SIZE;

        if (hasNextPage) {
            history = history.subList(0, PAGE_SIZE);
        }

        SimpleForm form = new SimpleForm(
                "§5§lMi historial"
        );

        form.addContent(
                "§7Consulta tus operaciones en DeluxeBounties.\n\n"
                        + "§fPágina: §d" + (page + 1)
                        + "\n"
                        + "§7Operaciones recientes:"
        );

        if (history.isEmpty()) {

            form.addButton(
                    "§cHistorial vacío\n§7Todavía no has realizado operaciones",
                    (target, button) ->
                            target.sendMessage(
                                    "§eTodavía no tienes operaciones registradas."
                            )
            );

        } else {

            for (BountyHistoryEntry entry : history) {

                String eventName = getEventName(entry.getEventType());
                String amount = String.format(
                        Locale.US,
                        "%,.2f",
                        entry.getAmount()
                );

                form.addButton(
                        eventName
                                + " §8| §f" + entry.getTargetName()
                                + "\n§7Cantidad: §6" + amount,
                        (target, button) ->
                                showDetails(target, entry, page)
                );
            }
        }

        if (page > 0) {

            form.addButton(
                    "§ePágina anterior",
                    (target, button) ->
                            send(target, page - 1)
            );
        }

        if (hasNextPage) {

            form.addButton(
                    "§aPágina siguiente",
                    (target, button) ->
                            send(target, page + 1)
            );
        }

        form.addButton(
                "§cCerrar",
                (target, button) -> {
                }
        );

        form.setCloseHandler(target -> {
        });

        form.send(player);
    }

    private void showDetails(
            Player player,
            BountyHistoryEntry entry,
            int page
    ) {

        String amount = String.format(
                Locale.US,
                "%,.2f",
                entry.getAmount()
        );

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss"
        );

        String date = dateFormat.format(
                new Date(entry.getCreatedAt())
        );

        SimpleForm form = new SimpleForm(
                "§5§lDetalle del historial"
        );

        form.addContent(
                "§7Información de la operación.\n\n"
                        + "§7ID del registro: §f#" + entry.getId() + "\n"
                        + "§7Operación: " + getEventName(entry.getEventType()) + "\n"
                        + "§7Objetivo: §e" + entry.getTargetName() + "\n"
                        + "§7Cantidad: §6" + amount + "\n"
                        + "§7Fecha: §f" + date + "\n"
                        + "§7Detalles: §f"
                        + (entry.getDetails() == null
                        ? "Sin detalles"
                        : entry.getDetails())
        );

        form.addButton(
                "§7Volver al historial",
                (target, button) ->
                        send(target, page)
        );

        form.send(player);
    }

    private String getEventName(String eventType) {

        if (eventType == null) {
            return "§7Desconocido";
        }

        return switch (eventType) {
            case "CREATED" -> "§aCreada";
            case "CANCELLED" -> "§cCancelada";
            case "CLAIMED" -> "§dReclamada";
            default -> "§7" + eventType;
        };
    }
}
