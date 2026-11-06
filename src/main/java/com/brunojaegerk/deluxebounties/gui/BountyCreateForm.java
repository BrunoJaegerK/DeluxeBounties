package com.brunojaegerk.deluxebounties.gui;

import cn.nukkit.Player;
import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.Bounty;

import com.formconstructor.form.CustomForm;
import com.formconstructor.form.element.general.Header;
import com.formconstructor.form.element.custom.Input;
import com.formconstructor.form.element.custom.Toggle;
import com.formconstructor.form.response.CustomFormResponse;

import java.sql.SQLException;

public final class BountyCreateForm {

    private final Main plugin;

    public BountyCreateForm(Main plugin) {
        this.plugin = plugin;
    }

    public void send(Player player) {

        CustomForm form = new CustomForm("§5§lCrear recompensa");

        // Información del formulario
        form.addElement(
                "information",
                new Header("§7Coloca una recompensa sobre otro jugador.\n"
                        + "§8El dinero se descontará de tu saldo.")
        );

        // Jugador objetivo
        form.addElement(
                "target",
                new Input("§fJugador objetivo")
                        .setPlaceholder("Nombre del jugador")
        );

        // Cantidad
        form.addElement(
                "amount",
                new Input("§fCantidad de dinero")
                        .setPlaceholder("Ejemplo: 5000")
        );

        // Publicación anónima
        form.addElement(
                "anonymous",
                new Toggle("§dPublicar anónimamente", false)
        );

        form.setSubmitButton("§aCrear recompensa");

        form.setHandler((target, response) ->
                handleSubmission(target, response)
        );

        form.setCloseHandler(target -> {
        });

        form.send(player);
    }

    private void handleSubmission(
            Player player,
            CustomFormResponse response
    ) {

        String targetName = response.getInput("target").getValue();
        String amountText = response.getInput("amount").getValue();
        boolean anonymous = response.getToggle("anonymous").getValue();

        // Validar jugador objetivo
        if (targetName == null || targetName.isBlank()) {
            player.sendMessage(
                    "§cDebes introducir el nombre del jugador."
            );
            return;
        }

        // Validar cantidad
        if (amountText == null || amountText.isBlank()) {
            player.sendMessage(
                    "§cDebes introducir una cantidad."
            );
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(amountText.trim());
        } catch (NumberFormatException exception) {
            player.sendMessage(
                    "§cLa cantidad introducida no es válida."
            );
            return;
        }

        // Comprobar jugador conectado
        Player target = plugin.getServer().getPlayerExact(
                targetName.trim()
        );

        if (target == null || !target.isOnline()) {
            player.sendMessage(
                    "§cEse jugador no está conectado."
            );
            return;
        }

        // Crear recompensa
        try {

            Bounty bounty = plugin.getBountyService().createBounty(
                    target.getUniqueId(),
                    target.getName(),
                    player.getUniqueId(),
                    player.getName(),
                    amount,
                    anonymous
            );

            player.sendMessage(
                    "§a¡Recompensa creada correctamente!"
            );

            player.sendMessage(
                    "§7ID: §f#" + bounty.getId()
            );

            player.sendMessage(
                    "§7Objetivo: §f" + target.getName()
            );

            player.sendMessage(
                    "§7Cantidad: §6" + amount
            );

            player.sendMessage(
                    anonymous
                            ? "§7Publicación: §dAnónima"
                            : "§7Publicación: §aPública"
            );

            target.sendMessage(
                    "§6¡Tienes una nueva recompensa sobre tu cabeza!"
            );

            target.sendMessage(
                    "§7Cantidad añadida: §6" + amount
            );

            plugin.getLogger().info(
                    "El jugador " + player.getName()
                            + " creó la recompensa #" + bounty.getId()
                            + " sobre " + target.getName()
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {

            player.sendMessage(
                    "§c" + exception.getMessage()
            );

        } catch (SQLException exception) {

            plugin.getLogger().error(
                    "Error al registrar una nueva recompensa.",
                    exception
            );

            player.sendMessage(
                    "§cOcurrió un error al guardar la recompensa."
            );
        }
    }
}
