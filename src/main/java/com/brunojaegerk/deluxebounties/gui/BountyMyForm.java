package com.brunojaegerk.deluxebounties.gui;

import cn.nukkit.Player;
import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.Bounty;

import com.formconstructor.form.SimpleForm;
import com.formconstructor.form.ModalForm;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public final class BountyMyForm {

    private static final int PAGE_SIZE = 8;

    private final Main plugin;

    public BountyMyForm(Main plugin) {
        this.plugin = plugin;
    }

    public void send(Player player) {
        send(player, 0);
    }

    private void send(Player player, int page) {

        int offset = page * PAGE_SIZE;

        List<Bounty> bounties;

        try {
            bounties = plugin.getBountyService()
                    .getMyActiveBounties(player.getUniqueId());

        } catch (SQLException | IllegalArgumentException exception) {

            plugin.getLogger().error(
                    "Error al consultar las recompensas del jugador.",
                    exception
            );

            player.sendMessage(
                    "§cNo se pudieron cargar tus recompensas."
            );
            return;
        }

        SimpleForm form = new SimpleForm(
                "§5§lMis recompensas"
        );

        form.addContent(
                "§7Consulta y administra tus aportaciones.\n\n"
                        + "§fRecompensas activas: §d"
                        + bounties.size()
        );

        if (bounties.isEmpty()) {

            form.addButton(
                    "§cNo tienes recompensas activas\n§7Tus aportaciones aparecerán aquí",
                    (target, button) ->
                            target.sendMessage(
                                    "§eNo tienes recompensas activas."
                            )
            );

        } else {

            int start = Math.min(offset, bounties.size());
            int end = Math.min(start + PAGE_SIZE, bounties.size());

            for (int i = start; i < end; i++) {

                Bounty bounty = bounties.get(i);

                String amount = String.format(
                        Locale.US,
                        "%,.2f",
                        bounty.getAmount()
                );

                form.addButton(
                        "§e#" + bounty.getId()
                                + " §8| §f" + bounty.getTargetName()
                                + "\n§7Aportación: §6" + amount,
                        (target, button) ->
                                showDetails(target, bounty)
                );
            }

            if (page > 0) {
                form.addButton(
                        "§ePágina anterior",
                        (target, button) ->
                                send(target, page - 1)
                );
            }

            if (end < bounties.size()) {
                form.addButton(
                        "§aPágina siguiente",
                        (target, button) ->
                                send(target, page + 1)
                );
            }
        }

        form.setCloseHandler(target -> {
        });

        form.send(player);
    }

    private void showDetails(Player player, Bounty bounty) {

        String amount = String.format(
                Locale.US,
                "%,.2f",
                bounty.getAmount()
        );

        SimpleForm form = new SimpleForm(
                "§5§lDetalle de recompensa"
        );

        form.addContent(
                "§7Información de tu aportación.\n\n"
                        + "§7ID: §f#" + bounty.getId() + "\n"
                        + "§7Objetivo: §e" + bounty.getTargetName() + "\n"
                        + "§7Cantidad: §6" + amount + "\n"
                        + "§7Estado: §a" + bounty.getStatus() + "\n"
                        + "§7Creada: §f" + bounty.getCreatedAt()
        );

        form.addButton(
                "§cCancelar recompensa\n§7Solicitar devolución del dinero",
                (target, button) ->
                        confirmCancellation(target, bounty)
        );

        form.addButton(
                "§7Volver a mis recompensas",
                (target, button) ->
                        send(target)
        );

        form.setCloseHandler(target -> {
        });

        form.send(player);
    }

    private void confirmCancellation(Player player, Bounty bounty) {

        String amount = String.format(
                Locale.US,
                "%,.2f",
                bounty.getAmount()
        );

        ModalForm form = new ModalForm(
                "§c§lCancelar recompensa",
                "§7¿Estás seguro de que deseas cancelar esta aportación?\n\n"
                        + "§7Objetivo: §e" + bounty.getTargetName() + "\n"
                        + "§7Cantidad a devolver: §6" + amount + "\n\n"
                        + "§cEsta acción no se puede deshacer."
        );

        form.setPositiveButton("§cSí, cancelar");
        form.setNegativeButton("§7No, volver");

        form.setHandler((target, accepted) -> {

            if (!accepted) {
                showDetails(target, bounty);
                return;
            }

            try {

                boolean cancelled = plugin.getBountyService()
                        .cancelBounty(
                                target.getUniqueId(),
                                target.getName(),
                                bounty.getId()
                        );

                if (cancelled) {

                    target.sendMessage(
                            "§aTu recompensa ha sido cancelada."
                    );

                    target.sendMessage(
                            "§7Dinero devuelto: §6" + amount
                    );

                    send(target);

                } else {

                    target.sendMessage(
                            "§cNo se pudo cancelar la recompensa."
                    );
                }

            } catch (IllegalArgumentException | IllegalStateException exception) {

                target.sendMessage(
                        "§c" + exception.getMessage()
                );

            } catch (SQLException exception) {

                plugin.getLogger().error(
                        "Error al cancelar la recompensa #" + bounty.getId(),
                        exception
                );

                target.sendMessage(
                        "§cOcurrió un error al cancelar la recompensa."
                );
            }
        });

        form.setCloseHandler(target -> {
        });

        form.send(player);
    }
}
