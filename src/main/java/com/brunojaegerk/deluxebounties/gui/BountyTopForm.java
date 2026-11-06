package com.brunojaegerk.deluxebounties.gui;

import cn.nukkit.Player;

import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.HunterStats;
import com.formconstructor.form.SimpleForm;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public final class BountyTopForm {

    private final Main plugin;

    public BountyTopForm(Main plugin) {
        this.plugin = plugin;
    }

    public void send(Player player) {

        SimpleForm form = new SimpleForm(
                "§5§lTop cazadores"
        );

        StringBuilder content = new StringBuilder();

        content.append("§7Ranking de los mejores cazadores\n\n");
        content.append("§dTop 10 jugadores:\n\n");

        try {

            List<HunterStats> hunters =
                    plugin.getBountyService().getTopHunters(10);

            if (hunters.isEmpty()) {

                content.append(
                        "§7Todavía no hay cazadores en el ranking."
                );

            } else {

                String[] positions = {
                        "§6🥇",
                        "§7🥈",
                        "§c🥉"
                };

                for (int i = 0; i < hunters.size(); i++) {

                    HunterStats hunter = hunters.get(i);

                    String position = i < 3
                            ? positions[i]
                            : "§8#" + (i + 1);

                    content.append(position)
                            .append(" §f")
                            .append(hunter.getPlayerName())
                            .append("\n");

                    content.append("  §7Recompensas: §d")
                            .append(hunter.getTotalClaims())
                            .append("\n");

                    content.append("  §7Dinero obtenido: §a")
                            .append(String.format(
                                    Locale.US,
                                    "%,.2f",
                                    hunter.getTotalEarned()
                            ))
                            .append("\n\n");
                }
            }

        } catch (SQLException | IllegalArgumentException exception) {

            plugin.getLogger().error(
                    "No se pudo cargar el ranking de cazadores.",
                    exception
            );

            content.append(
                    "§cNo se pudo cargar el ranking. Inténtalo nuevamente."
            );
        }

        form.addContent(content.toString());

        form.addButton(
                "§cCerrar",
                (target, button) -> {
                    // Cerrar el ranking.
                }
        );

        form.send(player);
    }
}
