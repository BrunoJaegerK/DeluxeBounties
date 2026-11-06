package com.brunojaegerk.deluxebounties.gui;

import cn.nukkit.Player;

import com.brunojaegerk.deluxebounties.Main;
import com.formconstructor.form.SimpleForm;
import com.brunojaegerk.deluxebounties.gui.BountyCreateForm;
import com.brunojaegerk.deluxebounties.gui.BountyTopForm;

public final class BountyMainForm {

    private final Main plugin;

    public BountyMainForm(Main plugin) {
        this.plugin = plugin;
    }

    public void send(Player player) {

        SimpleForm form = new SimpleForm(
                "§5§lDeluxeBounties"
        );

        form.addContent(
                "§7Sistema de recompensas\n\n"
                        + "§fColoca recompensas sobre otros jugadores "
                        + "y conviértete en el mejor cazador.\n\n"
                        + "§dSelecciona una opción:"
        );

        form.addButton("§aCrear recompensa\n§7Colocar una recompensa",
                (target, button) ->
                        new BountyCreateForm(plugin).send(target)
        );

        form.addButton("§6Recompensas activas\n§7Consultar objetivos",
                (target, button) ->
                        new BountyListForm(plugin).send(target)
        );

        form.addButton("§bMis recompensas\n§7Consultar mis aportaciones",
                (target, button) ->
                        new BountyMyForm(plugin).send(target)
        );

        form.addButton(
                "§dTop cazadores\n§7Ranking de jugadores",
                (target, button) ->
                        new BountyTopForm(plugin).send(target)
        );

        form.setCloseHandler(target -> {
            // El jugador cerró el menú.
        });

        form.send(player);
    }
}

