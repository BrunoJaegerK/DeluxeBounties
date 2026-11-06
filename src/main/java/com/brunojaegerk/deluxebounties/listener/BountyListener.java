package com.brunojaegerk.deluxebounties.listener;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerDeathEvent;

import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.BountyClaimResult;

import java.sql.SQLException;

public final class BountyListener implements Listener {

    private final Main plugin;

    public BountyListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player victim = event.getEntity();

        EntityDamageEvent lastDamage =
                victim.getLastDamageCause();

        if (!(lastDamage instanceof EntityDamageByEntityEvent damageEvent)) {
            return;
        }

        Entity damager = damageEvent.getDamager();

        if (!(damager instanceof Player hunter)) {
            return;
        }

        if (hunter.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        try {
            BountyClaimResult result =
                    plugin.getBountyService().claimBounties(
                            victim.getUniqueId(),
                            hunter.getUniqueId(),
                            hunter.getName()
                    );

            if (!result.hasClaims()) {
                return;
            }

            hunter.sendMessage(
                    "§5§lDeluxeBounties §r§8» §a¡Has cazado a §f"
                            + victim.getName() + "§a!"
            );

            hunter.sendMessage(
                    "§7Recompensas reclamadas: §d"
                            + result.getTotalClaims()
            );

            hunter.sendMessage(
                    "§7Dinero obtenido: §a+"
                            + result.getTotalEarned()
            );

            victim.sendMessage(
                    "§5§lDeluxeBounties §r§8» §cTus recompensas "
                            + "han sido reclamadas por §f"
                            + hunter.getName() + "§c."
            );

        } catch (SQLException | IllegalStateException
                 | IllegalArgumentException exception) {

            plugin.getLogger().error(
                    "No se pudieron reclamar las recompensas de "
                            + victim.getName() + ".",
                    exception
            );

            hunter.sendMessage(
                    "§cOcurrió un error al procesar las recompensas."
            );
        }
    }
}
