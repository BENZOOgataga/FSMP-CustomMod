// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.commands;

import com.flazesmp.companies.FlazeSMP;
import com.flazesmp.companies.data.Company;
import com.flazesmp.companies.data.CompanyLog;
import com.flazesmp.companies.data.CompanyManager;
import com.flazesmp.companies.data.CompanyTransaction;
import com.flazesmp.companies.economy.EconomyManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;

import java.util.*;

public class EconomyCommands {

    private static final Map<UUID, String> playerSpies = new HashMap<>(); // UUID -> target (null for all)
    private static final Map<UUID, Integer> companySpies = new HashMap<>(); // UUID -> company ID

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Player economy commands
        dispatcher.register(
                Commands.literal("pay")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(EconomyCommands::payCommand)))
        );

        dispatcher.register(
                Commands.literal("balance")
                        .executes(EconomyCommands::balanceCommand)
        );

        dispatcher.register(
                Commands.literal("money")
                        .executes(EconomyCommands::balanceCommand)
        );

        dispatcher.register(
                Commands.literal("balancetop")
                        .executes(context -> balanceTopCommand(context, 10))
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                                .executes(context -> balanceTopCommand(context, IntegerArgumentType.getInteger(context, "limit"))))
        );
        // In EconomyCommands.java - register method
        dispatcher.register(
                Commands.literal("paycompany")
                        .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(EconomyCommands::payCompanyCommand)))
        );
        dispatcher.register(
                Commands.literal("payspy")
                        .requires(source -> hasPermission(source, FlazeSMP.ECONOMY_PAYSPY))
                        .executes(EconomyCommands::toggleGlobalPaySpy)
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(EconomyCommands::togglePlayerPaySpy))
        );

        dispatcher.register(
                Commands.literal("payspycompany")
                        .requires(source -> hasPermission(source, FlazeSMP.ECONOMY_PAYSPY))
                        .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                .executes(EconomyCommands::toggleCompanyPaySpy))
        );



        // Admin economy commands
        dispatcher.register(
                Commands.literal("eco")
                        .requires(source -> {
                            // Allow if console, OP, or has any of the eco permissions
                            if (!source.isPlayer()) return true; // Console always has access

                            try {
                                ServerPlayer player = source.getPlayerOrException();
                                return source.hasPermission(2) || // OP level 2+
                                        PermissionAPI.getPermission(player, FlazeSMP.ECO_SET_BALANCE) ||
                                        PermissionAPI.getPermission(player, FlazeSMP.ECO_ADD_BALANCE) ||
                                        PermissionAPI.getPermission(player, FlazeSMP.ECO_REMOVE_BALANCE);
                            } catch (CommandSyntaxException e) {
                                return false;
                            }
                        })
                        .then(Commands.literal("set")
                                .requires(source -> hasPermission(source, FlazeSMP.ECO_SET_BALANCE))
                                .then(Commands.argument("target", EntityArgument.players())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                                .executes(EconomyCommands::ecoSetCommand))))
                        .then(Commands.literal("add")
                                .requires(source -> hasPermission(source, FlazeSMP.ECO_ADD_BALANCE))
                                .then(Commands.argument("target", EntityArgument.players())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                .executes(EconomyCommands::ecoAddCommand))))
                        .then(Commands.literal("remove")
                                .requires(source -> hasPermission(source, FlazeSMP.ECO_REMOVE_BALANCE))
                                .then(Commands.argument("target", EntityArgument.players())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                .executes(EconomyCommands::ecoRemoveCommand))))
        );
    }

    private static boolean hasPermission(CommandSourceStack source, PermissionNode<Boolean> node) {
        // Console always has permission
        if (!source.isPlayer()) {
            return true;
        }

        try {
            ServerPlayer player = source.getPlayerOrException();
            // Check permission using Forge API with PermissionNode
            return PermissionAPI.getPermission(player, node) || source.hasPermission(2);
        } catch (CommandSyntaxException e) {
            return source.hasPermission(2); // Fallback to op level
        }
    }

    private static int payCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String targetString = StringArgumentType.getString(context, "target");
        double amount = DoubleArgumentType.getDouble(context, "amount");

        // Round to 2 decimal places
        amount = Math.round(amount * 100) / 100.0;

        if (amount <= 0) {
            context.getSource().sendFailure(Component.literal("Amount must be greater than 0."));
            return 0;
        }

        EconomyManager economyManager = EconomyManager.get((ServerLevel) player.level());

        // Check if paying to a company (starts with #)
        if (targetString.startsWith("#")) {
            try {
                int companyId = Integer.parseInt(targetString.substring(1));

                CompanyManager companyManager = CompanyManager.get((ServerLevel) player.level());
                Company company = companyManager.getCompany(companyId);

                if (company == null) {
                    context.getSource().sendFailure(Component.literal("Company with ID " + companyId + " not found."));
                    return 0;
                }

                // Check if player has enough money
                if (economyManager.getPlayerBalance(player.getUUID()) < amount) {
                    context.getSource().sendFailure(Component.literal("You don't have enough money."));
                    return 0;
                }

                // Transfer the money
                if (economyManager.transferToCompany(player.getUUID(), companyId, amount)) {
                    final String formattedAmount = EconomyManager.formatCurrency(amount);
                    final String companyName = company.getName();

                    // Add transaction to company
                    company.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.DEPOSIT, amount,
                            "Payment from " + player.getName().getString());

                    context.getSource().sendSuccess(() ->
                            Component.literal("You paid " + formattedAmount + " to company '" + companyName + "'."), false);

                    // Notify company CEO if online
                    ServerPlayer ceo = player.getServer().getPlayerList().getPlayer(company.getCeoId());
                    if (ceo != null) {
                        ceo.sendSystemMessage(Component.literal(
                                player.getName().getString() + " paid " + formattedAmount + " to your company."));
                    }

                    // If company is a subsidiary, notify parent company
                    if (company.hasParentCompany()) {
                        Company parentCompany = companyManager.getCompany(company.getParentCompanyId());
                        if (parentCompany != null) {
                            // Add log to parent company
                            parentCompany.addLog((ServerLevel) player.level(), player.getUUID(), CompanyLog.LogType.FINANCIAL,
                                    "Subsidiary " + company.getName() + " received payment of " + formattedAmount +
                                            " from " + player.getName().getString());

                            // Notify parent company CEO if online
                            ServerPlayer parentCeo = player.getServer().getPlayerList().getPlayer(parentCompany.getCeoId());
                            if (parentCeo != null) {
                                parentCeo.sendSystemMessage(Component.literal(
                                        "Your subsidiary " + company.getName() + " received " +
                                                formattedAmount + " from " + player.getName().getString()));
                            }
                        }
                    }

                    return 1;
                } else {
                    context.getSource().sendFailure(Component.literal("Failed to transfer money."));
                    return 0;
                }
            } catch (NumberFormatException e) {
                context.getSource().sendFailure(Component.literal("Invalid company ID format. Use #ID (e.g., #103)."));
                return 0;
            }
        } else {
            // Paying to a player
            UUID targetId = null;
            String targetName = null;

            // Check if target is online
            for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                if (onlinePlayer.getName().getString().equalsIgnoreCase(targetString)) {
                    targetId = onlinePlayer.getUUID();
                    targetName = onlinePlayer.getName().getString();
                    break;
                }
            }

            // If not online, try to find from offline players
            if (targetId == null) {
                MinecraftServer server = player.getServer();
                GameProfile profile = server.getProfileCache().get(targetString).orElse(null);

                if (profile != null) {
                    targetId = profile.getId();
                    targetName = profile.getName();
                }
            }

            if (targetId == null) {
                context.getSource().sendFailure(Component.literal("Player not found."));
                return 0;
            }

            // Check if trying to pay self
            if (targetId.equals(player.getUUID())) {
                context.getSource().sendFailure(Component.literal("You cannot pay yourself."));
                return 0;
            }

            // Check if player has enough money
            if (economyManager.getPlayerBalance(player.getUUID()) < amount) {
                context.getSource().sendFailure(Component.literal("You don't have enough money."));
                return 0;
            }

            // Transfer the money
            if (economyManager.transferToPlayer(player.getUUID(), targetId, amount)) {
                final String formattedAmount = EconomyManager.formatCurrency(amount);
                final String finalTargetName = targetName;

                context.getSource().sendSuccess(() ->
                        Component.literal("You paid " + formattedAmount + " to " + finalTargetName + "."), false);

                // Notify target if online
                ServerPlayer targetPlayer = player.getServer().getPlayerList().getPlayer(targetId);
                if (targetPlayer != null) {
                    targetPlayer.sendSystemMessage(Component.literal(
                            player.getName().getString() + " paid you " + formattedAmount + "."));
                }

                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("Failed to transfer money."));
                return 0;
            }
        }
    }

    private static int balanceCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.get((ServerLevel) player.level());

        double balance = economyManager.getPlayerBalance(player.getUUID());
        String formattedBalance = EconomyManager.formatCurrency(balance);

        context.getSource().sendSuccess(() ->
                Component.literal("Your balance: " + formattedBalance), false);

        return 1;
    }

    private static int balanceTopCommand(CommandContext<CommandSourceStack> context, int limit) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.get((ServerLevel) player.level());

        List<Map.Entry<UUID, Double>> topBalances = economyManager.getTopBalances(limit);

        context.getSource().sendSuccess(() ->
                Component.literal("=== Top " + limit + " Balances ==="), false);

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : topBalances) {
            UUID playerId = entry.getKey();
            double balance = entry.getValue();

            String playerName = player.getServer().getProfileCache()
                    .get(playerId)
                    .map(GameProfile::getName)
                    .orElse("Unknown Player");

            final int finalRank = rank;
            final String formattedBalance = EconomyManager.formatCurrency(balance);

            context.getSource().sendSuccess(() ->
                    Component.literal(finalRank + ". " + playerName + " - " + formattedBalance), false);

            rank++;
        }

        return 1;
    }

    private static int ecoSetCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");
        double amount = DoubleArgumentType.getDouble(context, "amount");

        // Round to 2 decimal places
        amount = Math.round(amount * 100) / 100.0;

        if (amount < 0) {
            context.getSource().sendFailure(Component.literal("Amount cannot be negative."));
            return 0;
        }

        EconomyManager economyManager = EconomyManager.get((ServerLevel) context.getSource().getLevel());
        int count = 0;

        for (ServerPlayer target : targets) {
            if (economyManager.setPlayerBalance(target.getUUID(), amount)) {
                count++;

                // Notify the player
                final String formattedAmount = EconomyManager.formatCurrency(amount);
                target.sendSystemMessage(Component.literal(
                        "Your balance has been set to " + formattedAmount + " by an admin."));
            }
        }

        final String formattedAmount = EconomyManager.formatCurrency(amount);
        final int finalCount = count;

        if (count > 0) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Set the balance of " + finalCount + " player(s) to " + formattedAmount + "."), true);
            return count;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to set balance for any players."));
            return 0;
        }
    }

    private static int ecoAddCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");
        double amount = DoubleArgumentType.getDouble(context, "amount");

        // Round to 2 decimal places
        amount = Math.round(amount * 100) / 100.0;

        if (amount <= 0) {
            context.getSource().sendFailure(Component.literal("Amount must be greater than 0."));
            return 0;
        }

        EconomyManager economyManager = EconomyManager.get((ServerLevel) context.getSource().getLevel());
        int count = 0;

        for (ServerPlayer target : targets) {
            if (economyManager.addToPlayerBalance(target.getUUID(), amount)) {
                count++;

                // Notify the player
                final String formattedAmount = EconomyManager.formatCurrency(amount);
                target.sendSystemMessage(Component.literal(
                        "An admin added " + formattedAmount + " to your balance."));
            }
        }

        final String formattedAmount = EconomyManager.formatCurrency(amount);
        final int finalCount = count;

        if (count > 0) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Added " + formattedAmount + " to " + finalCount + " player(s)."), true);
            return count;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to add money to any players."));
            return 0;
        }
    }

    private static int ecoRemoveCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");
        double amount = DoubleArgumentType.getDouble(context, "amount");


// Round to 2 decimal places
        amount = Math.round(amount * 100) / 100.0;

        if (amount <= 0) {
            context.getSource().sendFailure(Component.literal("Amount must be greater than 0."));
            return 0;
        }

        EconomyManager economyManager = EconomyManager.get((ServerLevel) context.getSource().getLevel());
        int count = 0;

        for (ServerPlayer target : targets) {
            if (economyManager.removeFromPlayerBalance(target.getUUID(), amount)) {
                count++;

                // Notify the player
                final String formattedAmount = EconomyManager.formatCurrency(amount);
                target.sendSystemMessage(Component.literal(
                        "An admin removed " + formattedAmount + " from your balance."));
            }
        }

        final String formattedAmount = EconomyManager.formatCurrency(amount);
        final int finalCount = count;

        if (count > 0) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Removed " + formattedAmount + " from " + finalCount + " player(s)."), true);
            return count;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to remove money from any players."));
            return 0;
        }
    }

    /**
     * Pay a company command
     */
    private static int payCompanyCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int companyId = IntegerArgumentType.getInteger(context, "company_id");
        double amount = DoubleArgumentType.getDouble(context, "amount");

        // Round to 2 decimal places
        amount = Math.round(amount * 100) / 100.0;

        if (amount <= 0) {
            context.getSource().sendFailure(Component.literal("Amount must be greater than 0."));
            return 0;
        }

        EconomyManager economyManager = EconomyManager.get((ServerLevel) player.level());
        CompanyManager companyManager = CompanyManager.get((ServerLevel) player.level());
        Company company = companyManager.getCompany(companyId);

        if (company == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + companyId + " not found."));
            return 0;
        }

        // Check if player has enough money
        if (economyManager.getPlayerBalance(player.getUUID()) < amount) {
            context.getSource().sendFailure(Component.literal("You don't have enough money."));
            return 0;
        }

        // Transfer the money
        if (economyManager.transferToCompany(player.getUUID(), companyId, amount)) {
            final String formattedAmount = EconomyManager.formatCurrency(amount);
            final String companyName = company.getName();

            // Add transaction to company
            company.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.DEPOSIT, amount,
                    "Payment from " + player.getName().getString());

            context.getSource().sendSuccess(() ->
                    Component.literal("You paid " + formattedAmount + " to company '" + companyName + "'."), false);

            // Notify company CEO if online
            ServerPlayer ceo = player.getServer().getPlayerList().getPlayer(company.getCeoId());
            if (ceo != null) {
                ceo.sendSystemMessage(Component.literal(
                        player.getName().getString() + " paid " + formattedAmount + " to your company."));
            }

            // If company is a subsidiary, notify parent company
            if (company.hasParentCompany()) {
                Company parentCompany = companyManager.getCompany(company.getParentCompanyId());
                if (parentCompany != null) {
                    // Add log to parent company
                    parentCompany.addLog((ServerLevel) player.level(), player.getUUID(), CompanyLog.LogType.FINANCIAL,
                            "Subsidiary " + company.getName() + " received payment of " + formattedAmount +
                                    " from " + player.getName().getString());

                    // Notify parent company CEO if online
                    ServerPlayer parentCeo = player.getServer().getPlayerList().getPlayer(parentCompany.getCeoId());
                    if (parentCeo != null) {
                        parentCeo.sendSystemMessage(Component.literal(
                                "Your subsidiary " + company.getName() + " received " +
                                        formattedAmount + " from " + player.getName().getString()));
                    }
                }
            }

            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to transfer money."));
            return 0;
        }
    }

    private static int toggleGlobalPaySpy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();

        if (playerSpies.containsKey(playerId) && playerSpies.get(playerId) == null) {
            // Turn off global spy
            playerSpies.remove(playerId);
            context.getSource().sendSuccess(() ->
                    Component.literal("§cDisabled payment spying."), false);
        } else {
            // Turn on global spy, remove any specific spying
            playerSpies.put(playerId, null);
            companySpies.remove(playerId);
            context.getSource().sendSuccess(() ->
                    Component.literal("§aEnabled payment spying for §fall§a transactions."), false);
        }

        return 1;
    }

    /**
     * Toggle payment spying for a specific player
     */
    private static int togglePlayerPaySpy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();
        String targetName = StringArgumentType.getString(context, "player");

        if (playerSpies.containsKey(playerId) && targetName.equalsIgnoreCase(playerSpies.get(playerId))) {
            // Turn off spying for this player
            playerSpies.remove(playerId);
            context.getSource().sendSuccess(() ->
                    Component.literal("§cDisabled payment spying for player §f" + targetName + "§c."), false);
        } else {
            // Turn on spying for this player, remove any other spying
            playerSpies.put(playerId, targetName.toLowerCase());
            companySpies.remove(playerId);
            context.getSource().sendSuccess(() ->
                    Component.literal("§aEnabled payment spying for player §f" + targetName + "§a."), false);
        }

        return 1;
    }

    /**
     * Toggle payment spying for a specific company
     */
    private static int toggleCompanyPaySpy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();
        int companyId = IntegerArgumentType.getInteger(context, "company_id");

        // Verify the company exists
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getCompany(companyId);

        if (company == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + companyId + " not found."));
            return 0;
        }

        if (companySpies.containsKey(playerId) && companySpies.get(playerId) == companyId) {
            // Turn off spying for this company
            companySpies.remove(playerId);
            context.getSource().sendSuccess(() ->
                    Component.literal("§cDisabled payment spying for company §f" + company.getName() + " (ID: " + companyId + ")§c."), false);
        } else {
            // Turn on spying for this company, remove any other spying
            companySpies.put(playerId, companyId);
            playerSpies.remove(playerId);
            context.getSource().sendSuccess(() ->
                    Component.literal("§aEnabled payment spying for company §f" + company.getName() + " (ID: " + companyId + ")§a."), false);
        }

        return 1;
    }

    /**
     * Send payment spy notifications to staff
     */
    private static void sendPaymentSpyNotifications(ServerPlayer sender, String receiverName, UUID receiverId,
                                                    Integer companyId, String companyName, double amount) {
        MinecraftServer server = sender.getServer();
        String formattedAmount = String.format("%.2f", amount);
        Component message;

        if (companyId != null) {
            // Payment to company
            message = Component.literal("§8[§cPaySpy§8] §f" + sender.getName().getString() +
                    " §7paid §f" + formattedAmount + " FC$ §7to company §f" + companyName + " (ID: " + companyId + ")");
        } else {
            // Payment to player
            message = Component.literal("§8[§cPaySpy§8] §f" + sender.getName().getString() +
                    " §7paid §f" + formattedAmount + " FC$ §7to §f" + receiverName);
        }

        // Send to global spies
        for (Map.Entry<UUID, String> entry : playerSpies.entrySet()) {
            UUID spyId = entry.getKey();
            String targetName = entry.getValue();

            // Skip if the spy is the sender
            if (spyId.equals(sender.getUUID())) {
                continue;
            }

            // Send if spying on all or if spying on sender or receiver
            if (targetName == null ||
                    sender.getName().getString().toLowerCase().contains(targetName) ||
                    (receiverName != null && receiverName.toLowerCase().contains(targetName))) {

                ServerPlayer spy = server.getPlayerList().getPlayer(spyId);
                if (spy != null) {
                    spy.sendSystemMessage(message);
                }
            }
        }

        // Send to company spies
        if (companyId != null) {
            for (Map.Entry<UUID, Integer> entry : companySpies.entrySet()) {
                if (entry.getValue() == companyId) {
                    ServerPlayer spy = server.getPlayerList().getPlayer(entry.getKey());
                    if (spy != null && !spy.getUUID().equals(sender.getUUID())) {
                        spy.sendSystemMessage(message);
                    }
                }
            }
        }
    }

    /**
     * Send company payment spy notifications to staff
     */
    private static void sendCompanyPaymentSpyNotifications(int senderCompanyId, String senderCompanyName,
                                                           UUID actorId, String actorName,
                                                           UUID receiverId, String receiverName,
                                                           Integer receiverCompanyId, String receiverCompanyName,
                                                           double amount) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        String formattedAmount = String.format("%.2f", amount);
        Component message;

        if (receiverCompanyId != null) {
            // Company to company payment
            message = Component.literal("§8[§cPaySpy§8] §f" + actorName + " §7paid §f" + formattedAmount +
                    " FC$ §7from company §f" + senderCompanyName + " (ID: " + senderCompanyId +
                    ") §7to company §f" + receiverCompanyName + " (ID: " + receiverCompanyId + ")");
        } else {
            // Company to player payment
            message = Component.literal("§8[§cPaySpy§8] §f" + actorName + " §7paid §f" + formattedAmount +
                    " FC$ §7from company §f" + senderCompanyName + " (ID: " + senderCompanyId + ") §7to §f" + receiverName);
        }

        // Send to global spies
        for (Map.Entry<UUID, String> entry : playerSpies.entrySet()) {
            UUID spyId = entry.getKey();
            String targetName = entry.getValue();

            // Skip if the spy is the actor
            if (spyId.equals(actorId)) {
                continue;
            }

            // Send if spying on all or if spying on actor or receiver
            if (targetName == null ||
                    actorName.toLowerCase().contains(targetName) ||
                    (receiverName != null && receiverName.toLowerCase().contains(targetName))) {

                ServerPlayer spy = server.getPlayerList().getPlayer(spyId);
                if (spy != null) {
                    spy.sendSystemMessage(message);
                }
            }
        }

        // Send to company spies (sender company)
        for (Map.Entry<UUID, Integer> entry : companySpies.entrySet()) {
            if (entry.getValue() == senderCompanyId) {
                ServerPlayer spy = server.getPlayerList().getPlayer(entry.getKey());
                if (spy != null && !spy.getUUID().equals(actorId)) {
                    spy.sendSystemMessage(message);
                }
            }
        }

        // Send to company spies (receiver company if applicable)
        if (receiverCompanyId != null) {
            for (Map.Entry<UUID, Integer> entry : companySpies.entrySet()) {
                if (entry.getValue() == receiverCompanyId) {
                    ServerPlayer spy = server.getPlayerList().getPlayer(entry.getKey());
                    if (spy != null && !spy.getUUID().equals(actorId)) {
                        spy.sendSystemMessage(message);
                    }
                }
            }
        }
    }
}