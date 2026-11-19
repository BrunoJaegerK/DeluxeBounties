package com.brunojaegerk.deluxebounties.gui;

import cn.nukkit.Player;
import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.Bounty;
import com.brunojaegerk.deluxebounties.economy.EconomyManager;
import com.formconstructor.form.CustomForm;
import com.formconstructor.form.element.general.Header;
import com.formconstructor.form.element.custom.Input;
import com.formconstructor.form.element.custom.Toggle;
import com.formconstructor.form.element.custom.Dropdown;
import com.formconstructor.form.element.SelectableElement;
import com.formconstructor.form.response.CustomFormResponse;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BountyCreateForm {
    private final Main plugin;

    public BountyCreateForm(Main plugin) {
        this.plugin = plugin;
    }

    public void send(Player player) {
        CustomForm form = new CustomForm("§5§lCrear recompensa");
        form.addElement("information", new Header(
                "§7Coloca una recompensa sobre otro jugador.\n"
                        + "§8Elige con qué moneda deseas pagar."));

        form.addElement("target", new Input("§fJugador objetivo")
                .setPlaceholder("Nombre del jugador"));

        List<SelectableElement> options = new ArrayList<>();
        for (EconomyManager.CurrencyOption currency
                : plugin.getEconomyManager().getAvailableCurrencies()) {
            if (!plugin.getEconomyManager().isAvailable(
                    currency.getProvider(), currency.getCurrencyId())) continue;
            String value = currency.getProvider() + "::" + currency.getCurrencyId();
            options.add(new SelectableElement(currency.getLabel(), value));
        }

        if (options.isEmpty()) {
            player.sendMessage("§cNo hay proveedores económicos disponibles.");
            return;
        }

        form.addElement("currency", new Dropdown("§fMoneda de pago", options, 0));
        form.addElement("amount", new Input("§fCantidad de dinero")
                .setPlaceholder("Ejemplo: 5000"));
        form.addElement("anonymous", new Toggle("§dPublicar anónimamente", false));
        form.setSubmitButton("§aCrear recompensa");
        form.setHandler((target, response) -> handleSubmission(target, response));
        form.setCloseHandler(target -> { });
        form.send(player);
    }

    private void handleSubmission(Player player, CustomFormResponse response) {
        String targetName = response.getInput("target").getValue();
        String amountText = response.getInput("amount").getValue();
        SelectableElement selected = response.getDropdown("currency").getValue();
        boolean anonymous = response.getToggle("anonymous").getValue();

        if (targetName == null || targetName.isBlank()) {
            player.sendMessage("§cDebes introducir el nombre del jugador.");
            return;
        }
        if (selected == null || selected.getValue() == null) {
            player.sendMessage("§cDebes seleccionar una moneda válida.");
            return;
        }
        if (amountText == null || amountText.isBlank()) {
            player.sendMessage("§cDebes introducir una cantidad.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText.trim());
        } catch (NumberFormatException exception) {
            player.sendMessage("§cLa cantidad introducida no es válida.");
            return;
        }

        String[] selection = String.valueOf(selected.getValue()).split("::", 2);
        if (selection.length != 2) {
            player.sendMessage("§cLa moneda seleccionada no es válida.");
            return;
        }
        String provider = selection[0];
        String currencyId = selection[1];

        Player target = plugin.getServer().getPlayerExact(targetName.trim());
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cEse jugador no está conectado.");
            return;
        }

        try {
            Bounty bounty = plugin.getBountyService().createBounty(
                    target.getUniqueId(), target.getName(),
                    player.getUniqueId(), player.getName(),
                    amount, provider, currencyId, anonymous);

            String formatted = plugin.getEconomyManager().formatAmount(
                    bounty.getAmount(), bounty.getCurrencyProvider(), bounty.getCurrencyId());

            player.sendMessage("§a¡Recompensa creada correctamente!");
            player.sendMessage("§7ID: §f#" + bounty.getId());
            player.sendMessage("§7Objetivo: §f" + target.getName());
            player.sendMessage("§7Cantidad: §6" + formatted);
            player.sendMessage(anonymous ? "§7Publicación: §dAnónima" : "§7Publicación: §aPública");

            target.sendMessage("§6¡Tienes una nueva recompensa sobre tu cabeza!");
            target.sendMessage("§7Cantidad añadida: §6" + formatted);

            plugin.getLogger().info("El jugador " + player.getName()
                    + " creó la recompensa #" + bounty.getId()
                    + " sobre " + target.getName() + " por " + formatted + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage("§c" + exception.getMessage());
        } catch (SQLException exception) {
            plugin.getLogger().error("Error al registrar una nueva recompensa.", exception);
            player.sendMessage("§cOcurrió un error al guardar la recompensa.");
        }
    }
}
