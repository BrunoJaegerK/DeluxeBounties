package com.brunojaegerk.deluxebounties.gui;

import cn.nukkit.Player;
import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.Bounty;
import com.formconstructor.form.SimpleForm;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public final class BountyListForm {

    private static final int PAGE_SIZE = 8;

    private final Main plugin;

    public BountyListForm(Main plugin) {
        this.plugin = plugin;
    }

    public void send(Player player) {
        send(player, 0);
    }

    private void send(Player player, int page) {

        int offset = Math.multiplyExact(page, PAGE_SIZE);

        List<Bounty> bounties;

        try {

            // Consultamos un elemento adicional para detectar
            // si existe una página siguiente.
            bounties = plugin.getBountyService()
                    .getAllActiveBounties(PAGE_SIZE + 1, offset);

        } catch (SQLException | IllegalArgumentException exception) {

            plugin.getLogger().error(
                    "Error al consultar las recompensas activas.",
                    exception
            );

            player.sendMessage(
                    "§cNo se pudieron cargar las recompensas."
            );

            return;
        }

        boolean hasNextPage = bounties.size() > PAGE_SIZE;

        if (hasNextPage) {
            bounties = bounties.subList(0, PAGE_SIZE);
        }

        SimpleForm form = new SimpleForm(
                "§5§lRecompensas activas"
        );

        form.addContent(
                "§7Recompensas disponibles en DeluxeMine.\n\n"
                        + "§fPágina: §d" + (page + 1)
                        + "\n\n"
                        + "§7Selecciona una recompensa para consultar sus detalles."
        );

        if (bounties.isEmpty()) {

            form.addButton(
                    "§cNo hay recompensas activas\n§7Todavía no existen objetivos",
                    (target, button) ->
                            target.sendMessage(
                                    "§eNo hay recompensas activas actualmente."
                            )
            );

        } else {

            for (Bounty bounty : bounties) {

                String creator = bounty.isAnonymous()
                        ? "§8Anónimo"
                        : "§f" + bounty.getCreatorName();

                String amount = String.format(
                        Locale.US,
                        "%,.2f",
                        bounty.getAmount()
                );

                String buttonText =
                        "§e" + bounty.getTargetName()
                                + " §8| §6" + amount
                                + "\n§7Aportado por: " + creator;

                form.addButton(
                        buttonText,
                        (target, button) ->
                                showDetails(target, bounty)
                );
            }
        }

        // Página anterior
        if (page > 0) {

            form.addButton(
                    "§ePágina anterior",
                    (target, button) ->
                            send(target, page - 1)
            );
        }

        // Página siguiente
        if (hasNextPage) {

            form.addButton(
                    "§aPágina siguiente",
                    (target, button) ->
                            send(target, page + 1)
            );
        }

        form.setCloseHandler(target -> {
        });

        form.send(player);
    }

    private void showDetails(Player player, Bounty bounty) {

        String creator = bounty.isAnonymous()
                ? "§8Anónimo"
                : "§f" + bounty.getCreatorName();

        String amount = String.format(
                Locale.US,
                "%,.2f",
                bounty.getAmount()
        );

        player.sendMessage("§5§m-------------------------");
        player.sendMessage("§d§lDETALLES DE RECOMPENSA");
        player.sendMessage("§7ID: §f#" + bounty.getId());
        player.sendMessage(
                "§7Objetivo: §e" + bounty.getTargetName()
        );
        player.sendMessage(
                "§7Cantidad: §6" + amount
        );
        player.sendMessage(
                "§7Creador: " + creator
        );
        player.sendMessage(
                "§7Estado: §a" + bounty.getStatus()
        );
        player.sendMessage("§5§m-------------------------");
    }
}
