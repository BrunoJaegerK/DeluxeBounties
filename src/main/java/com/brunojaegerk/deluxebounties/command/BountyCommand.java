package com.brunojaegerk.deluxebounties.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;

import com.brunojaegerk.deluxebounties.Main;
import com.brunojaegerk.deluxebounties.bounty.Bounty;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public final class BountyCommand extends Command {

    private final Main plugin;

    public BountyCommand(Main plugin) {
        super(
                "bounty",
                "Sistema de recompensas de DeluxeBounties",
                "/bounty",
                new String[]{"bounties"}
        );

        this.plugin = plugin;
        setPermission("deluxebounties.use");
    }

    @Override
    public boolean execute(
            CommandSender sender,
            String commandLabel,
            String[] args
    ) {

        if (!sender.hasPermission("deluxebounties.use")) {
            sender.sendMessage("§cNo tienes permiso para utilizar este comando.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "list" -> {
                if (args.length != 2) {
                    sender.sendMessage("§cUso: /bounty list <jugador>");
                    return true;
                }

                showBounties(sender, args[1]);
                return true;
            }

            case "top" -> {
                sender.sendMessage(
                        "§eEl ranking de cazadores estará disponible "
                                + "en una próxima actualización."
                );
                return true;
            }

            default -> {
                createBounty(sender, args);
                return true;
            }

            case "cancel" -> {
                if (args.length != 2) {
                    sender.sendMessage("§cUso: /bounty cancel <id>");
                    return true;
                }

                cancelBounty(sender, args[1]);
                return true;
            }
        }
    }

    private void createBounty(CommandSender sender, String[] args) {

        if (!(sender instanceof Player creator)) {
            sender.sendMessage(
                    "§cSolo los jugadores pueden crear recompensas."
            );
            return;
        }

        if (args.length < 2 || args.length > 3) {
            sendHelp(sender);
            return;
        }

        String targetName = args[0];

        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§cLa cantidad indicada no es válida.");
            return;
        }

        boolean anonymous = false;

        if (args.length == 3) {
            if (!args[2].equalsIgnoreCase("anonimo")
                    && !args[2].equalsIgnoreCase("anónimo")) {
                sender.sendMessage(
                        "§cUso: /bounty <jugador> <cantidad> [anonimo]"
                );
                return;
            }

            anonymous = true;
        }

        Player target = plugin.getServer().getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(
                    "§cEl jugador debe estar conectado para recibir "
                            + "una recompensa."
            );
            return;
        }

        if (target.getUniqueId().equals(creator.getUniqueId())) {
            sender.sendMessage(
                    "§cNo puedes colocar una recompensa sobre ti mismo."
            );
            return;
        }

        try {
            Bounty bounty = plugin.getBountyService().createBounty(
                    target.getUniqueId(),
                    target.getName(),
                    creator.getUniqueId(),
                    creator.getName(),
                    amount,
                    anonymous
            );

            sender.sendMessage(
                    "§a¡Recompensa creada correctamente!"
            );

            sender.sendMessage(
                    "§7Objetivo: §f" + target.getName()
            );

            sender.sendMessage(
                    "§7Cantidad: §a"
                            + String.format(Locale.US, "%,.2f", amount)
            );

            sender.sendMessage(
                    "§7ID de recompensa: §f#" + bounty.getId()
            );

            if (anonymous) {
                sender.sendMessage("§7Modalidad: §dAnónima");
            }

            target.sendMessage(
                    "§c¡Han colocado una recompensa sobre ti!"
            );

            target.sendMessage(
                    "§7Cantidad añadida: §a"
                            + String.format(Locale.US, "%,.2f", amount)
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {

            sender.sendMessage("§c" + exception.getMessage());

        } catch (SQLException exception) {

            plugin.getLogger().error(
                    "Error al crear una recompensa.",
                    exception
            );

            sender.sendMessage(
                    "§cOcurrió un error al guardar la recompensa."
            );
        }
    }

    private void showBounties(
            CommandSender sender,
            String targetName
    ) {

        Player target = plugin.getServer().getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(
                    "§cEl jugador debe estar conectado para consultar "
                            + "sus recompensas."
            );
            return;
        }

        try {
            List<Bounty> bounties =
                    plugin.getDatabaseManager()
                            .getActiveBountiesForTarget(target.getUniqueId());

            if (bounties.isEmpty()) {
                sender.sendMessage(
                        "§e" + target.getName()
                                + " no tiene recompensas activas."
                );
                return;
            }

            double total = 0;

            sender.sendMessage("§5§lRecompensas de " + target.getName());
            sender.sendMessage("§7Recompensas activas:");

            for (Bounty bounty : bounties) {

                total += bounty.getAmount();

                sender.sendMessage(
                        "§8» §f#" + bounty.getId()
                                + " §a"
                                + String.format(
                                Locale.US,
                                "%,.2f",
                                bounty.getAmount()
                        )
                );
            }

            sender.sendMessage("§7----------------------------");

            sender.sendMessage(
                    "§7Total acumulado: §a"
                            + String.format(Locale.US, "%,.2f", total)
            );

        } catch (SQLException exception) {

            plugin.getLogger().error(
                    "Error al consultar las recompensas.",
                    exception
            );

            sender.sendMessage(
                    "§cNo se pudieron consultar las recompensas."
            );
        }
    }

    private void sendHelp(CommandSender sender) {

        sender.sendMessage("§5§lDeluxeBounties §7- Ayuda");
        sender.sendMessage("§d/bounty <jugador> <cantidad>");
        sender.sendMessage(
                "§d/bounty <jugador> <cantidad> anonimo"
        );
        sender.sendMessage("§d/bounty list <jugador>");
        sender.sendMessage("§d/bounty top");
        sender.sendMessage("§d/bounty cancel <id>");
    }


    private void cancelBounty(CommandSender sender, String idArgument) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    "§cSolo los jugadores pueden cancelar recompensas."
            );
            return;
        }

        long bountyId;

        try {
            bountyId = Long.parseLong(idArgument);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§cEl ID de la recompensa no es válido.");
            return;
        }

        try {
            boolean cancelled = plugin.getBountyService().cancelBounty(
                    player.getUniqueId(),
                    player.getName(),
                    bountyId
            );

            if (cancelled) {
                sender.sendMessage(
                        "§aHas cancelado la recompensa §f#" + bountyId
                );

                sender.sendMessage(
                        "§7Se ha devuelto el 100 % de tu aportación."
                );
            }

        } catch (IllegalArgumentException | IllegalStateException exception) {

            sender.sendMessage("§c" + exception.getMessage());

        } catch (SQLException exception) {

            plugin.getLogger().error(
                    "Error al cancelar la recompensa #" + bountyId,
                    exception
            );

            sender.sendMessage(
                    "§cOcurrió un error al cancelar la recompensa."
            );
        }
    }
}

