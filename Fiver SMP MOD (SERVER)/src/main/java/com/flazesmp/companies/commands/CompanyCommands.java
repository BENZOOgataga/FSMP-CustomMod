// Owned by BENZOOgataga. Not for redistribution or external server use.

package com.flazesmp.companies.commands;

import com.flazesmp.companies.FlazeSMP;
import com.flazesmp.companies.common.network.NetworkHandler;
import com.flazesmp.companies.common.network.packets.DomainDataPacket;
import com.flazesmp.companies.common.network.packets.OpenCompanyCreationGuiPacket;
import com.flazesmp.companies.config.CompanyConfig;
import com.flazesmp.companies.config.CompanyDomainsConfig;
import com.flazesmp.companies.data.*;
import com.flazesmp.companies.economy.EconomyManager;
import com.flazesmp.companies.gui.PermissionsGui;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;

import java.text.SimpleDateFormat;
import java.util.*;

public class CompanyCommands {


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("company")
                        .then(Commands.literal("create")
                                .executes(CompanyCommands::createCompany))
                        .then(Commands.literal("rename")
                                .then(Commands.literal("paycompany")
                                        .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                        .executes(CompanyCommands::payCompanyCommand))))
                                .then(Commands.argument("new_company_name", StringArgumentType.string())
                                        .executes(CompanyCommands::renameCompany)))
                        .then(Commands.literal("transfer")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CompanyCommands::transferCompany)))
                        .then(Commands.literal("merge")
                                .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                        .executes(CompanyCommands::mergeCompany)))
                        .then(Commands.literal("mergeaccept")
                                .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                        .executes(CompanyCommands::acceptMerge)))
                        .then(Commands.literal("info")
                                .executes(CompanyCommands::showOwnCompanyInfo)
                                .then(Commands.argument("company_name", StringArgumentType.string())
                                        .executes(CompanyCommands::companyInfo)))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CompanyCommands::invitePlayer)))
                        .then(Commands.literal("revokeinv")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(CompanyCommands::revokeInvitation)))
                        .then(Commands.literal("join")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(CompanyCommands::joinCompany)))
                        .then(Commands.literal("fire")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(CompanyCommands::firePlayer)))
                        .then(Commands.literal("leave")
                                .executes(CompanyCommands::leaveCompany))
                        .then(Commands.literal("config")
                                .requires(source -> source.hasPermission(2)) // OP only
                                .then(Commands.literal("maintenance")
                                        .then(Commands.literal("minbalance")
                                                .then(Commands.argument("percentage", DoubleArgumentType.doubleArg(0.1, 100))
                                                        .executes(CompanyCommands::setMinimumBalancePercentage)))
                                        .then(Commands.literal("graceperiod")
                                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 30))
                                                        .executes(CompanyCommands::setGracePeriodDays)))
                                        .then(Commands.literal("basevalue")
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(1000, 1000000))
                                                        .executes(CompanyCommands::setBaseCompanyValue)))
                                        .then(Commands.literal("info")
                                                .executes(CompanyCommands::showMaintenanceConfig))))
                        .then(Commands.literal("deposit")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(CompanyCommands::depositFunds)))
                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(CompanyCommands::withdrawFunds)))
                        .then(Commands.literal("pay")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                .executes(CompanyCommands::payPlayer))))
                        .then(Commands.literal("balance")
                                .executes(CompanyCommands::checkBalance))
                        .then(Commands.literal("subsidiaries")
                                .executes(CompanyCommands::listSubsidiaries))
                        .then(Commands.literal("invest")
                                .then(Commands.argument("subsidiary", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                .executes(CompanyCommands::investInSubsidiary))))
                        .then(Commands.literal("propose")
                                .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("percentage", IntegerArgumentType.integer(1, 50))
                                                .executes(CompanyCommands::proposeSubsidiary))))
                        .then(Commands.literal("editshare")
                                .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("percentage", IntegerArgumentType.integer(1, 50))
                                                .executes(CompanyCommands::editSharePercentage))))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                        .executes(CompanyCommands::acceptProposal)))
                        .then(Commands.literal("upgrade")
                                .executes(CompanyCommands::upgradeCompany))
                        .then(Commands.literal("research")
                                .then(Commands.literal("list")
                                        .executes(CompanyCommands::listResearchProjects))
                                .then(Commands.literal("start")
                                        .then(Commands.argument("project", StringArgumentType.greedyString())
                                                .executes(CompanyCommands::startResearch)))
                                .then(Commands.literal("status")
                                        .executes(CompanyCommands::checkResearchStatus))
                                .then(Commands.literal("history")
                                        .executes(CompanyCommands::viewResearchHistory))
                                .then(Commands.literal("add")
                                        .requires(source -> hasPermission(source, FlazeSMP.COMPANY_RESEARCH_ADD))
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .then(Commands.argument("icon", StringArgumentType.string())
                                                        .then(Commands.argument("description", StringArgumentType.string())
                                                                .then(Commands.argument("cost", DoubleArgumentType.doubleArg(100))
                                                                        .then(Commands.argument("domain", StringArgumentType.string())
                                                                                .executes(CompanyCommands::addResearchProject))))))))
                        .then(Commands.literal("validateupgrade")
                                .requires(source -> hasPermission(source, FlazeSMP.COMPANY_VALIDATE_UPGRADE))
                                .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                        .executes(CompanyCommands::validateUpgrade)))
                        .then(Commands.literal("disband")
                                .executes(CompanyCommands::disbandOwnCompany)
                                .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                        .requires(source -> {
// Allow if console, OP, or has the disband permission
                                            if (!source.isPlayer()) return true; // Console always has access
                                            try {
                                                ServerPlayer player = source.getPlayerOrException();
                                                return source.hasPermission(2) || // OP level 2+
                                                        PermissionAPI.getPermission(player, FlazeSMP.COMPANY_DISBAND);
                                            } catch (CommandSyntaxException e) {
                                                return false;
                                            }
                                        })
                                        .executes(CompanyCommands::disbandAnyCompany)))
                        .then(Commands.literal("permissions")
                                .executes(CompanyCommands::openPermissionsGui))
                        .then(Commands.literal("logs")
                                .executes(context -> showLogs(context, 10, null))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> showLogs(context, IntegerArgumentType.getInteger(context, "limit"), null))
                                        .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                                .requires(source -> source.hasPermission(2))  // OP only
                                                .executes(context -> showLogs(context,
                                                        IntegerArgumentType.getInteger(context, "limit"),
                                                        IntegerArgumentType.getInteger(context, "company_id"))))))
                        .then(Commands.literal("transactions")
                                .executes(context -> showTransactions(context, 10, null, 1))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> showTransactions(context,
                                                IntegerArgumentType.getInteger(context, "limit"), null, 1))
                                        .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                                .requires(source -> source.hasPermission(2))  // OP only
                                                .executes(context -> showTransactions(context,
                                                        IntegerArgumentType.getInteger(context, "limit"),
                                                        IntegerArgumentType.getInteger(context, "company_id"), 1)))))
                        .then(Commands.literal("promote")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(CompanyCommands::promotePlayer)))
                        .then(Commands.literal("demote")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(CompanyCommands::demotePlayer)))
                        .then(Commands.literal("active")
                                .requires(source -> {
                                    if (!source.isPlayer()) return true;
                                    try {
                                        ServerPlayer player = source.getPlayerOrException();
                                        return source.hasPermission(2) ||
                                                PermissionAPI.getPermission(player, FlazeSMP.COMPANY_ACTIVE);
                                    } catch (CommandSyntaxException e) {
                                        return false;
                                    }
                                })
                                .executes(CompanyCommands::showActiveCompanies))
                        .then(Commands.literal("inactive")
                                .requires(source -> {
                                    if (!source.isPlayer()) return true;
                                    try {
                                        ServerPlayer player = source.getPlayerOrException();
                                        return source.hasPermission(2) ||
                                                PermissionAPI.getPermission(player, FlazeSMP.COMPANY_INACTIVE);
                                    } catch (CommandSyntaxException e) {
                                        return false;
                                    }
                                })
                                .executes(CompanyCommands::showInactiveCompanies))
        );
    }

    // Permission check using PermissionNode
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

    // CompanyCommands.java (server side)
    private static int createCompany(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player already has a company
        if (manager.getPlayerCompany(player.getUUID()) != null) {
            context.getSource().sendFailure(Component.literal("You already belong to a company."));
            return 0;
        }

        System.out.println("Creating company for player: " + player.getName().getString());

        // Convert server config to packet format
        List<DomainDataPacket.DomainData> packetDomains = new ArrayList<>();
        for (CompanyDomainsConfig.DomainConfig domainConfig : CompanyDomainsConfig.getInstance().getDomains()) {
            DomainDataPacket.DomainData domain = new DomainDataPacket.DomainData();
            domain.name = domainConfig.name;
            domain.icon = domainConfig.icon;
            domain.description = domainConfig.description;

            domain.subdomains = new ArrayList<>();
            for (CompanyDomainsConfig.SubdomainConfig subdomainConfig : domainConfig.subdomains) {
                DomainDataPacket.SubdomainData subdomain = new DomainDataPacket.SubdomainData();
                subdomain.name = subdomainConfig.name;
                subdomain.icon = subdomainConfig.icon;
                subdomain.buff = subdomainConfig.buff;
                subdomain.locked = subdomainConfig.locked;
                // The showBuff value is not being transferred from config to packet
                subdomain.showBuff = subdomainConfig.showBuff; // Add this line
                domain.subdomains.add(subdomain);
            }

            packetDomains.add(domain);
        }

        System.out.println("Prepared " + packetDomains.size() + " domains to send to client");

        // First send the GUI open packet
        System.out.println("Sending OpenCompanyCreationGuiPacket to player");
        NetworkHandler.sendToPlayer(new OpenCompanyCreationGuiPacket(), player);

        // Then send domain data
        System.out.println("Sending DomainDataPacket to player");
        NetworkHandler.sendToPlayer(new DomainDataPacket(packetDomains), player);

        context.getSource().sendSuccess(() -> Component.literal("Opening company creation GUI..."), false);
        return 1;
    }

    // Rename company command
    private static int renameCompany(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String newName = StringArgumentType.getString(context, "new_company_name");

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

        // Check if player is CEO
        if (!player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can rename the company."));
            return 0;
        }

        String oldName = company.getName();
        company.setName(newName);
        manager.setDirty();

        context.getSource().sendSuccess(() ->
                Component.literal("Company renamed from '" + oldName + "' to '" + newName + "'."), false);
        return 1;
    }

    // Transfer company command
    private static int transferCompany(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

        // Check if player is CEO
        if (!player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can transfer ownership."));
            return 0;
        }

        // Check if target player is in the company
        if (!company.getMembers().containsKey(targetPlayer.getUUID())) {
            context.getSource().sendFailure(Component.literal("The target player must be a member of your company."));
            return 0;
        }

        // Transfer ownership
        company.transferOwnership(targetPlayer.getUUID());
        manager.setDirty();

        context.getSource().sendSuccess(() ->
                Component.literal("Company ownership transferred to " + targetPlayer.getName().getString() + "."), false);
        targetPlayer.sendSystemMessage(Component.literal("You are now the CEO of company '" + company.getName() + "'."));

        return 1;
    }

    // Merge company command
    private static int mergeCompany(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int targetCompanyId = IntegerArgumentType.getInteger(context, "company_id");

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company playerCompany = manager.getPlayerCompany(player.getUUID());
        Company targetCompany = manager.getCompany(targetCompanyId);

        if (playerCompany == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

        // Check if player is CEO
        if (!player.getUUID().equals(playerCompany.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can initiate a merge."));
            return 0;
        }

        if (targetCompany == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + targetCompanyId + " not found."));
            return 0;
        }

        if (playerCompany.getId() == targetCompany.getId()) {
            context.getSource().sendFailure(Component.literal("You cannot merge with your own company."));
            return 0;
        }

        // Propose merge
        manager.proposeMerge(playerCompany.getId(), targetCompany.getId());

        context.getSource().sendSuccess(() ->
                Component.literal("Merge proposal sent to company '" + targetCompany.getName() + "'."), false);

        // Notify the target company CEO if online
        ServerPlayer targetCeo = player.getServer().getPlayerList().getPlayer(targetCompany.getCeoId());
        if (targetCeo != null) {
            targetCeo.sendSystemMessage(Component.literal(
                    "Company '" + playerCompany.getName() + "' has proposed a merge with your company. " +
                            "Use /company mergeaccept " + playerCompany.getId() + " to accept."));
        }

        return 1;
    }

    // Accept merge command
    private static int acceptMerge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int sourceCompanyId = IntegerArgumentType.getInteger(context, "company_id");

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company playerCompany = manager.getPlayerCompany(player.getUUID());
        Company sourceCompany = manager.getCompany(sourceCompanyId);

        if (playerCompany == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

        // Check if player is CEO
        if (!player.getUUID().equals(playerCompany.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can accept a merge."));
            return 0;
        }

        if (sourceCompany == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + sourceCompanyId + " not found."));
            return 0;
        }

        // Check if there's a pending merge proposal
        if (!manager.hasMergeProposal(sourceCompanyId, playerCompany.getId())) {
            context.getSource().sendFailure(Component.literal("No merge proposal from that company."));
            return 0;
        }

        // Execute the merge
        if (manager.executeMerge(sourceCompanyId, playerCompany.getId())) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Companies merged successfully. Your company has been disbanded and you are now an associate in '" +
                            sourceCompany.getName() + "'."), false);

            // Notify the source company CEO
            ServerPlayer sourceCeo = player.getServer().getPlayerList().getPlayer(sourceCompany.getCeoId());
            if (sourceCeo != null) {
                sourceCeo.sendSystemMessage(Component.literal(
                        "Company '" + playerCompany.getName() + "' has accepted your merge proposal. " +
                                "The merge has been completed."));
            }

            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to execute merge."));
            return 0;
        }
    }

    // Invite player command
    private static int invitePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer invitee = EntityArgument.getPlayer(context, "player");

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

        // Check if player has permission to invite
        if (!company.hasPermission(player.getUUID(), CompanyPermission.INVITE_PLAYERS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to invite players."));
            return 0;
        }

        // Check if invitee is already in a company
        if (manager.getPlayerCompany(invitee.getUUID()) != null) {
            context.getSource().sendFailure(Component.literal(invitee.getName().getString() + " is already in a company."));
            return 0;
        }

        // Check if there's already an invitation
        if (manager.hasInvitation(invitee.getUUID(), company.getId())) {
            // Revoke invitation
            manager.revokeInvitation(invitee.getUUID());
            context.getSource().sendSuccess(() ->
                    Component.literal("Invitation to " + invitee.getName().getString() + " has been revoked."), false);
            invitee.sendSystemMessage(Component.literal("Your invitation to join company '" + company.getName() + "' has been revoked."));
            return 1;
        }

        // Send invitation
        if (manager.invitePlayer(company.getId(), player.getUUID(), invitee.getUUID(), CompanyRole.EMPLOYEE)) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Invitation sent to " + invitee.getName().getString() + "."), false);
            invitee.sendSystemMessage(Component.literal(
                    "§aYou have been invited to join company '" + company.getName() + "' (ID: " + company.getId() + ")." +
                            "\n§7Use §f/company join " + company.getName() + "§7 or §f/company join #" + company.getId() + "§7 to accept."));
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to send invitation."));
            return 0;
        }
    }

    /**
     * Join company command
     */
    private static int joinCompany(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String companyIdentifier = StringArgumentType.getString(context, "name");

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player already has a company
        if (manager.getPlayerCompany(player.getUUID()) != null) {
            context.getSource().sendFailure(Component.literal("You already belong to a company."));
            return 0;
        }

        // Check if joining by ID (starts with #)
        Company targetCompany = null;
        if (companyIdentifier.startsWith("#")) {
            try {
                int companyId = Integer.parseInt(companyIdentifier.substring(1));
                targetCompany = manager.getCompany(companyId);
            } catch (NumberFormatException e) {
                context.getSource().sendFailure(Component.literal("Invalid company ID format. Use #ID (e.g., #103)."));
                return 0;
            }
        } else {
            // Find company by name
            for (Company company : manager.getAllCompanies()) {
                if (company.getName().equalsIgnoreCase(companyIdentifier)) {
                    targetCompany = company;
                    break;
                }
            }
        }

        if (targetCompany == null) {
            context.getSource().sendFailure(Component.literal("Company not found: " + companyIdentifier));
            return 0;
        }

        // Check if player has been invited
        if (!manager.hasInvitation(player.getUUID(), targetCompany.getId())) {
            context.getSource().sendFailure(Component.literal("You have not been invited to \"" + targetCompany.getName() + "\""));
            return 0;
        }

        // Join the company
        if (manager.acceptInvitation(player.getUUID(), targetCompany.getId())) {
            final String finalCompanyName = targetCompany.getName();
            context.getSource().sendSuccess(() ->
                    Component.literal("You have joined the company '" + finalCompanyName + "'."), false);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to join company."));
            return 0;
        }
    }

    // Fire player command
    private static int firePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String targetPlayerName = StringArgumentType.getString(context, "player");

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

        // Check if player has permission to fire
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.KICK_MEMBERS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to fire members."));
            return 0;
        }

        // Find the target player UUID
        UUID targetPlayerId = null;
        MinecraftServer server = player.getServer();

        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (onlinePlayer.getName().getString().equalsIgnoreCase(targetPlayerName)) {
                targetPlayerId = onlinePlayer.getUUID();
                break;
            }
        }

        // If not online, try to find from offline players
        if (targetPlayerId == null) {
            targetPlayerId = server.getProfileCache().get(targetPlayerName)
                    .map(GameProfile::getId)
                    .orElse(null);
        }

        if (targetPlayerId == null) {
            context.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }

        // Check if the player is in this company
        if (manager.getPlayerCompany(targetPlayerId) == null ||
                manager.getPlayerCompany(targetPlayerId).getId() != company.getId()) {
            context.getSource().sendFailure(Component.literal(targetPlayerName + " is not a member of your company."));
            return 0;
        }

        // Can't fire the CEO
        if (targetPlayerId.equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("You cannot fire the CEO of the company."));
            return 0;
        }

        // Remove the player from the company
        if (company.removeMember(targetPlayerId)) {
            manager.removePlayerFromCompany(targetPlayerId);

            final String finalTargetName = targetPlayerName;
            context.getSource().sendSuccess(() ->
                    Component.literal("Successfully fired " + finalTargetName + " from your company."), false);

            // Notify the fired player if they're online
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetPlayerId);
            if (targetPlayer != null) {
                targetPlayer.sendSystemMessage(Component.literal("You have been fired from company '" + company.getName() + "'."));
            }

            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to fire player."));
            return 0;
        }
    }

    // Leave company command
    private static int leaveCompany(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

        // CEO can't leave
        if (player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("As the CEO, you cannot leave the company. Transfer ownership first or disband the company."));
            return 0;
        }

        // Leave the company
        if (manager.leaveCompany(player.getUUID())) {
            context.getSource().sendSuccess(() ->
                    Component.literal("You have left the company '" + company.getName() + "'."), false);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to leave company."));
            return 0;
        }
    }

    // Disband own company command
    private static int disbandOwnCompany(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

        // Only CEO can disband
        if (!player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can disband the company."));
            return 0;
        }

        // Disband the company
        if (manager.disbandCompany(company.getId())) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Company '" + company.getName() + "' has been disbanded."), false);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to disband company."));
            return 0;
        }
    }

    // Disband any company command (staff only)
    private static int disbandAnyCompany(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int companyId = IntegerArgumentType.getInteger(context, "company_id");

        CompanyManager manager = CompanyManager.get((ServerLevel) context.getSource().getLevel());
        Company company = manager.getCompany(companyId);

        if (company == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + companyId + " not found."));
            return 0;
        }

        // Disband the company
        if (manager.disbandCompany(companyId)) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Company '" + company.getName() + "' (ID: " + companyId + ") has been disbanded."), false);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to disband company."));
            return 0;
        }
    }

    // Open permissions GUI
    private static int openPermissionsGui(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

        // Only CEO can open permissions GUI
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.EDIT_PERMISSIONS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to edit permissions."));
            return 0;
        }

        // Open the permissions GUI
        PermissionsGui.openCompanyMembersMenu(player, company);
        return 1;
    }

    /**
     * Show company logs
     */
    private static int showLogs(CommandContext<CommandSourceStack> context, int limit, Integer companyId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player has permission to view logs
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.VIEW_LOGS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to view company logs."));
            return 0;
        }

        // Get logs
        CompanyLogger logger = CompanyLogger.get((ServerLevel) player.level());
        List<CompanyLog> logs = logger.getCompanyLogs(company.getId(), limit);

        if (logs.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No logs found for this company."), false);
            return 1;
        }

        // Display logs
        context.getSource().sendSuccess(() ->
                Component.literal("=== Company Logs for " + company.getName() + " (ID: " + company.getId() + ") ==="), false);

        MinecraftServer server = context.getSource().getServer();

        for (CompanyLog log : logs) {
            String actorName;
            if (log.getActorId() != null) {
                actorName = server.getProfileCache()
                        .get(log.getActorId())
                        .map(GameProfile::getName)
                        .orElse("Unknown");
            } else {
                actorName = "System";
            }

            context.getSource().sendSuccess(() ->
                    Component.literal("[" + log.getFormattedTimestamp() + "] " +
                            actorName + " " + log.getType().getDescription() +
                            (log.getDetails().isEmpty() ? "" : ": " + log.getDetails())), false);
        }

        return 1;
    }

    private static int showTransactions(CommandContext<CommandSourceStack> context, int limit, Integer companyId, int page) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        Company company = null;

        // If company_id is provided (for OPs), use that company
        if (companyId != null) {
            company = manager.getCompany(companyId);
            if (company == null) {
                context.getSource().sendFailure(Component.literal("Company with ID " + companyId + " not found."));
                return 0;
            }
        } else {
            // Otherwise use the player's company
            company = manager.getPlayerCompany(player.getUUID());
            if (company == null) {
                context.getSource().sendFailure(Component.literal("You don't belong to any company."));
                return 0;
            }

            // Check if player has permission to view transactions
            if (!player.getUUID().equals(company.getCeoId()) &&
                    !company.hasPermission(player.getUUID(), CompanyPermission.VIEW_TRANSACTIONS)) {
                context.getSource().sendFailure(Component.literal("You don't have permission to view company transactions."));
                return 0;
            }
        }

        // Get transactions
        List<CompanyTransaction> transactions = company.getTransactions(limit);

        if (transactions.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No transactions found for this company."), false);
            return 1;
        }

        // Calculate pages
        int totalTransactions = transactions.size();
        int totalPages = (int) Math.ceil(totalTransactions / 10.0);

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * 10;
        int endIndex = Math.min(startIndex + 10, totalTransactions);

        // Display transactions
        final String companyName = company.getName();
        context.getSource().sendSuccess(() ->
                Component.literal("=== Company Transactions for " + companyName + " (ID: " + companyId + ") ==="), false);

        MinecraftServer server = context.getSource().getServer();

        for (int i = startIndex; i < endIndex; i++) {
            CompanyTransaction transaction = transactions.get(i);
            String actorName = server.getProfileCache()
                    .get(transaction.getActorId())
                    .map(GameProfile::getName)
                    .orElse("Unknown");

            context.getSource().sendSuccess(() ->
                    Component.literal("[" + transaction.getFormattedTimestamp() + "] " +
                            actorName + " " + transaction.getType().getDescription() +
                            ": " + EconomyManager.formatCurrency(transaction.getAmount()) +
                            (transaction.getDetails().isEmpty() ? "" : " (" + transaction.getDetails() + ")")), false);
        }

        // Add pagination buttons
        if (totalPages > 1) {
            int finalPage1 = page;
            Component nextButton = Component.literal("[Next Page]")
                    .withStyle(ChatFormatting.GREEN)
                    .withStyle(style -> style.withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/company transactions " + limit + (companyId != null ? " " + companyId : "") + " " + (finalPage1 + 1))));

            int finalPage = page;
            Component prevButton = Component.literal("[Previous Page]")
                    .withStyle(ChatFormatting.YELLOW)
                    .withStyle(style -> style.withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/company transactions " + limit + (companyId != null ? " " + companyId : "") + " " + (finalPage - 1))));

            if (page < totalPages) {
                context.getSource().sendSuccess(() -> nextButton, false);
            }

            if (page > 1) {
                context.getSource().sendSuccess(() -> prevButton, false);
            }
        }

        // TODO: Implement R&D, Investments, Salaries, Subsidiary revenue share transactions

        return 1;
    }

    // Promote player command
    private static int promotePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String targetPlayerName = StringArgumentType.getString(context, "player");


        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

// Only CEO can promote
        if (!player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can promote members."));
            return 0;
        }

// Find the target player UUID
        UUID targetPlayerId = null;
        MinecraftServer server = player.getServer();

        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (onlinePlayer.getName().getString().equalsIgnoreCase(targetPlayerName)) {
                targetPlayerId = onlinePlayer.getUUID();
                break;
            }
        }

// If not online, try to find from offline players
        if (targetPlayerId == null) {
            targetPlayerId = server.getProfileCache().get(targetPlayerName)
                    .map(GameProfile::getId)
                    .orElse(null);
        }

        if (targetPlayerId == null) {
            context.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }

// Check if the player is in this company
        if (manager.getPlayerCompany(targetPlayerId) == null ||
                manager.getPlayerCompany(targetPlayerId).getId() != company.getId()) {
            context.getSource().sendFailure(Component.literal(targetPlayerName + " is not a member of your company."));
            return 0;
        }

// Check current role
        CompanyRole currentRole = company.getMemberRole(targetPlayerId);
        if (currentRole == CompanyRole.ASSOCIATE) {
            context.getSource().sendFailure(Component.literal(targetPlayerName + " is already an associate."));
            return 0;
        }

// Promote the player
        company.setMemberRole(targetPlayerId, CompanyRole.ASSOCIATE);
        manager.setDirty();

        context.getSource().sendSuccess(() ->
                Component.literal("Successfully promoted " + targetPlayerName + " to associate."), false);

// Notify the promoted player if they're online
        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetPlayerId);
        if (targetPlayer != null) {
            targetPlayer.sendSystemMessage(Component.literal("You have been promoted to associate in company '" + company.getName() + "'."));
        }

        return 1;
    }

    private static int validateUpgrade(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer staff = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) staff.level();
        CompanyManager manager = CompanyManager.get(level);

        int companyId = IntegerArgumentType.getInteger(context, "company_id");
        Company company = manager.getCompany(companyId);

        if (company == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + companyId + " not found."));
            return 0;
        }

        if (!company.isPendingUpgrade()) {
            context.getSource().sendFailure(Component.literal("This company has not requested an upgrade."));
            return 0;
        }

        // Perform the upgrade
        int oldTier = company.getTier();
        int newTier = oldTier + 1;
        company.setTier(newTier);
        company.setPendingUpgrade(false);
        company.addLog(level, staff.getUUID(), CompanyLog.LogType.UPGRADE_APPROVED,
                "Upgraded from Tier " + oldTier + " to Tier " + newTier);
        manager.setDirty();

        // Notify staff
        context.getSource().sendSuccess(() ->
                Component.literal("Approved upgrade for company '" + company.getName() + "' to Tier " + newTier), true);

        // Notify company CEO
        ServerPlayer ceo = staff.getServer().getPlayerList().getPlayer(company.getCeoId());
        if (ceo != null) {
            ceo.sendSystemMessage(Component.literal("§aYour company has been upgraded to Tier " + newTier + "!"));

            // Explain new tier benefits
            switch (newTier) {
                case 2:
                    ceo.sendSystemMessage(Component.literal("§7You can now expand into a related subdomain."));
                    break;
                case 3:
                    ceo.sendSystemMessage(Component.literal("§7You have unlocked Research & Development (R&D)."));
                    break;
                case 4:
                    ceo.sendSystemMessage(Component.literal("§7Your company is now a Mother Company and can manage subsidiaries."));
                    break;
            }
        }

        return 1;
    }


    private static int upgradeCompany(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player is CEO
        if (!player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can request company upgrades."));
            return 0;
        }

        // Check if company is already at max tier
        if (company.getTier() >= 4) {
            context.getSource().sendFailure(Component.literal("Your company is already at the maximum tier (Tier 4)."));
            return 0;
        }

        // Check if there's already a pending upgrade
        if (company.isPendingUpgrade()) {
            context.getSource().sendFailure(Component.literal("Your company already has a pending upgrade request."));
            return 0;
        }

        // Request upgrade
        company.setPendingUpgrade(true);
        company.addLog(level, player.getUUID(), CompanyLog.LogType.UPGRADE_REQUESTED,
                "Requested upgrade from Tier " + company.getTier() + " to Tier " + (company.getTier() + 1));
        manager.setDirty();

        // Notify player
        context.getSource().sendSuccess(() ->
                Component.literal("Upgrade request submitted. A staff member will review your request after you complete the required challenge."), false);

        // Notify staff members
        notifyStaffAboutUpgrade(player.getServer(), company);

        return 1;
    }

    private static void notifyStaffAboutUpgrade(MinecraftServer server, Company company) {
        Component message = Component.literal("§6Company '" + company.getName() +
                "' (ID: " + company.getId() + ") has requested an upgrade to Tier " + (company.getTier() + 1));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Check if player has the notification permission
            if (player.hasPermissions(2) ||
                    PermissionAPI.getPermission(player, FlazeSMP.COMPANY_NOTIFY_UPGRADE)) {
                player.sendSystemMessage(message);
            }
        }
    }

    // Demote player command
    private static int demotePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String targetPlayerName = StringArgumentType.getString(context, "player");

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company company = manager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You don't belong to any company."));
            return 0;
        }

// Only CEO can demote
        if (!player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can demote members."));
            return 0;
        }

// Find the target player UUID
        UUID targetPlayerId = null;
        MinecraftServer server = player.getServer();

        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (onlinePlayer.getName().getString().equalsIgnoreCase(targetPlayerName)) {
                targetPlayerId = onlinePlayer.getUUID();
                break;
            }
        }

// If not online, try to find from offline players
        if (targetPlayerId == null) {
            targetPlayerId = server.getProfileCache().get(targetPlayerName)
                    .map(GameProfile::getId)
                    .orElse(null);
        }

        if (targetPlayerId == null) {
            context.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }

// Check if the player is in this company
        if (manager.getPlayerCompany(targetPlayerId) == null ||
                manager.getPlayerCompany(targetPlayerId).getId() != company.getId()) {
            context.getSource().sendFailure(Component.literal(targetPlayerName + " is not a member of your company."));
            return 0;
        }

// Check current role
        CompanyRole currentRole = company.getMemberRole(targetPlayerId);
        if (currentRole == CompanyRole.EMPLOYEE) {
            context.getSource().sendFailure(Component.literal(targetPlayerName + " is already an employee."));
            return 0;
        }

// Demote the player
        company.setMemberRole(targetPlayerId, CompanyRole.EMPLOYEE);
        manager.setDirty();

        context.getSource().sendSuccess(() ->
                Component.literal("Successfully demoted " + targetPlayerName + " to employee."), false);

// Notify the demoted player if they're online
        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetPlayerId);
        if (targetPlayer != null) {
            targetPlayer.sendSystemMessage(Component.literal("You have been demoted to employee in company '" + company.getName() + "'."));
        }

        return 1;
    }

    // Show active companies command
    private static int showActiveCompanies(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CompanyManager manager = CompanyManager.get((ServerLevel) context.getSource().getLevel());


// TODO: Implement active companies logic
        context.getSource().sendSuccess(() -> Component.literal("=== Active Companies ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("Active companies feature not implemented yet."), false);

        return 1;
    }

    // Show inactive companies command
    private static int showInactiveCompanies(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CompanyManager manager = CompanyManager.get((ServerLevel) context.getSource().getLevel());


// TODO: Implement inactive companies logic
        context.getSource().sendSuccess(() -> Component.literal("=== Inactive Companies ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("Inactive companies feature not implemented yet."), false);

        return 1;
    }

    private static int listResearchProjects(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if company has the required tier
        if (company.getTier() < 3) {
            context.getSource().sendFailure(Component.literal("Research & Development is only available for Tier 3+ companies."));
            return 0;
        }

        // Get available projects
        ResearchManager researchManager = ResearchManager.getInstance();
        List<ResearchManager.ResearchProject> projects = researchManager.getAvailableProjectsForCompany(company);

        if (projects.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No research projects available for your company domain."), false);
            return 0;
        }

        // Display projects
        context.getSource().sendSuccess(() -> Component.literal("=== Available Research Projects ==="), false);

        for (ResearchManager.ResearchProject project : projects) {
            double cost = researchManager.calculateResearchCost(company.getId(), project);

            Component projectInfo = Component.literal("§6" + project.getName() + "§r - Cost: §a" + EconomyManager.formatCurrency(cost) + "§r")
                    .append(Component.literal("\n§7" + project.getDescription()));

            Component startButton = Component.literal(" [Start Research]")
                    .withStyle(ChatFormatting.GREEN)
                    .withStyle(style -> style.withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/company research start " + project.getName())));

            context.getSource().sendSuccess(() -> projectInfo.copy().append(startButton), false);
        }

        return 1;
    }

    private static int startResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player is CEO or has permission
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.INVEST_FUNDS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to start research."));
            return 0;
        }

        // Check if company has the required tier
        if (company.getTier() < 3) {
            context.getSource().sendFailure(Component.literal("Research & Development is only available for Tier 3+ companies."));
            return 0;
        }

        // Check if company already has active research
        ResearchManager researchManager = ResearchManager.getInstance();
        if (researchManager.getActiveResearch(company.getId()) != null) {
            context.getSource().sendFailure(Component.literal("Your company already has an active research project."));
            return 0;
        }

        // Get the project name
        String projectName = StringArgumentType.getString(context, "project");

        // Find the project
        ResearchManager.ResearchProject project = researchManager.getAvailableProjectsForCompany(company).stream().filter(p -> p.getName().equalsIgnoreCase(projectName)).findFirst().orElse(null);

        if (project == null) {
            context.getSource().sendFailure(Component.literal("Research project not found: " + projectName));
            return 0;
        }

        // Calculate cost
        double cost = researchManager.calculateResearchCost(company.getId(), project);

        // Check if company has enough funds
        if (company.getFunds() < cost) {
            context.getSource().sendFailure(Component.literal("Your company doesn't have enough funds. Required: " +
                    EconomyManager.formatCurrency(cost)));
            return 0;
        }

        // Withdraw funds
        company.withdrawFunds(cost);

        // Add transaction
        company.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.RESEARCH_INVESTMENT, cost,
                "Research: " + project.getName());

        // Start research
        if (researchManager.startResearch(company.getId(), project.getName(), player.getUUID())) {
            // Add log
            company.addLog(level, player.getUUID(), CompanyLog.LogType.RESEARCH,
                    "Started research: " + project.getName() + " (Cost: " + EconomyManager.formatCurrency(cost) + ")");

            // Notify player
            context.getSource().sendSuccess(() ->
                    Component.literal("Started research project: " + project.getName() +
                            " (Cost: " + EconomyManager.formatCurrency(cost) + ")"), false);

            // Schedule research completion (randomly between 10-30 minutes)
            int minutes = 10 + new Random().nextInt(21); // 10-30 minutes
            scheduleResearchCompletion(level.getServer(), company.getId(), minutes * 60);

            return 1;
        } else {
            // Refund if research couldn't be started
            company.addFunds(cost);
            context.getSource().sendFailure(Component.literal("Failed to start research project."));
            return 0;
        }
    }

    private static void scheduleResearchCompletion(MinecraftServer server, int companyId, int seconds) {
        // Schedule a task to complete the research after the specified time
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + (seconds * 20), () -> {
            ServerLevel level = server.overworld();
            CompanyManager manager = CompanyManager.get(level);
            Company company = manager.getCompany(companyId);

            if (company == null) {
                return; // Company no longer exists
            }

            ResearchManager researchManager = ResearchManager.getInstance();
            ResearchManager.ActiveResearch activeResearch = researchManager.getActiveResearch(companyId);

            if (activeResearch == null) {
                return; // No active research
            }

            // Complete the research
            ResearchManager.CompletedResearch completed = researchManager.completeResearch(companyId);

            if (completed == null) {
                return; // Failed to complete research
            }

            // Process research outcomes
            processResearchOutcomes(level, company, completed);

            // Add log
            company.addLog(level, completed.getResearcherId(), CompanyLog.LogType.RESEARCH,
                    "Completed research: " + completed.getProjectName());

            // Notify company members
            notifyCompanyAboutResearch(server, company, completed);
        }));
    }

    private static void processResearchOutcomes(ServerLevel level, Company company, ResearchManager.CompletedResearch research) {
        Map<String, String> outcomes = research.getOutcomes();

        // Process each outcome
        for (Map.Entry<String, String> entry : outcomes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            switch (key) {
                case "unlock_subdomain":
                    // Unlock a new subdomain
                    company.getUnlockedSubdomains().add(value);
                    break;

                // Add more outcome types as needed

                default:
                    // Store as a company buff/perk
                    if (!company.getPerks().containsKey(key)) {
                        company.getPerks().put(key, value);
                    } else {
                        // If the perk already exists, increase its value if numeric
                        try {
                            double currentValue = Double.parseDouble(company.getPerks().get(key));
                            double addValue = Double.parseDouble(value);
                            company.getPerks().put(key, String.valueOf(currentValue + addValue));
                        } catch (NumberFormatException e) {
                            // Not numeric, just replace
                            company.getPerks().put(key, value);
                        }
                    }
                    break;
            }
        }

        // Save company changes
        CompanyManager.get(level).setDirty();
    }

    private static void notifyCompanyAboutResearch(MinecraftServer server, Company company, ResearchManager.CompletedResearch research) {
        // Create the message
        Component message = Component.literal("§aResearch Completed: §f" + research.getProjectName())
                .append(Component.literal("\n§7Your company has completed a research project!"));

        // Add outcome descriptions
        if (!research.getOutcomes().isEmpty()) {
            message = Component.empty().append(message).append(Component.literal("\n§6Research Outcomes:"));

            for (Map.Entry<String, String> entry : research.getOutcomes().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                String outcomeDesc;
                switch (key) {
                    case "unlock_subdomain":
                        outcomeDesc = "Unlocked new subdomain: " + value;
                        break;
                    case "mining_efficiency":
                        outcomeDesc = "Mining efficiency increased by " + value + "%";
                        break;
                    case "crop_yield":
                        outcomeDesc = "Crop yield increased by " + value + "%";
                        break;
                    case "market_discount":
                        outcomeDesc = "Market prices improved by " + value + "%";
                        break;
                    default:
                        outcomeDesc = key + ": " + value;
                        break;
                }

                message = Component.empty().append(message).append(Component.literal("\n§7- §f" + outcomeDesc));
            }
        }

        // Send to all online company members
        Component finalMessage = message;
        for (UUID memberId : company.getMembers().keySet()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.sendSystemMessage(finalMessage);
            }
        }
    }

    private static int checkResearchStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if company has the required tier
        if (company.getTier() < 3) {
            context.getSource().sendFailure(Component.literal("Research & Development is only available for Tier 3+ companies."));
            return 0;
        }

        // Get active research
        ResearchManager researchManager = ResearchManager.getInstance();
        ResearchManager.ActiveResearch activeResearch = researchManager.getActiveResearch(company.getId());

        if (activeResearch == null) {
            context.getSource().sendSuccess(() -> Component.literal("Your company has no active research projects."), false);
            return 0;
        }

        // Calculate progress (just for show, actual completion is scheduled)
        long elapsedTime = System.currentTimeMillis() - activeResearch.getStartTime();
        long totalTime = 20 * 60 * 1000; // Assume 20 minutes total
        int progressPercent = (int) Math.min(100, (elapsedTime * 100) / totalTime);

        // Get researcher name
        String researcherName = context.getSource().getServer().getProfileCache()
                .get(activeResearch.getResearcherId())
                .map(GameProfile::getName)
                .orElse("Unknown");

        // Display status
        context.getSource().sendSuccess(() -> Component.literal("=== Research Status ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("Project: " + activeResearch.getProjectName()), false);
        context.getSource().sendSuccess(() -> Component.literal("Started by: " + researcherName), false);
        context.getSource().sendSuccess(() -> Component.literal("Cost: " + EconomyManager.formatCurrency(activeResearch.getCost())), false);
        context.getSource().sendSuccess(() -> Component.literal("Progress: " + progressPercent + "%"), false);

        return 1;
    }

    private static int viewResearchHistory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Get research history
        ResearchManager researchManager = ResearchManager.getInstance();
        List<ResearchManager.CompletedResearch> history = researchManager.getResearchHistory(company.getId());

        if (history.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Your company has not completed any research projects yet."), false);
            return 0;
        }

        // Display history
        context.getSource().sendSuccess(() -> Component.literal("=== Research History ==="), false);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (int i = 0; i < history.size(); i++) {
            ResearchManager.CompletedResearch research = history.get(i);

            // Get researcher name
            String researcherName = context.getSource().getServer().getProfileCache()
                    .get(research.getResearcherId())
                    .map(GameProfile::getName)
                    .orElse("Unknown");

            // Format completion date
            String completionDate = dateFormat.format(new Date(research.getCompletionTime()));

            final int index = i + 1;
            context.getSource().sendSuccess(() ->
                    Component.literal(index + ". " + research.getProjectName() +
                            " - Completed on " + completionDate +
                            " by " + researcherName +
                            " (Cost: " + EconomyManager.formatCurrency(research.getCost()) + ")"), false);
        }

        return 1;
    }

    private static int addResearchProject(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        String icon = StringArgumentType.getString(context, "icon");
        String description = StringArgumentType.getString(context, "description");
        double cost = DoubleArgumentType.getDouble(context, "cost");
        String domain = StringArgumentType.getString(context, "domain");

        // Create a new research project
        ResearchManager.ResearchProject project = new ResearchManager.ResearchProject(
                name, icon, description, cost, domain.toLowerCase(), new HashMap<>()
        );

        // Add the project
        ResearchManager.getInstance().addResearchProject(project);

        context.getSource().sendSuccess(() ->
                Component.literal("Added new research project: " + name), true);

        return 1;
    }

    /**
     * Deposit personal funds into company
     */
    private static int depositFunds(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);
        // Make amount final by creating a separate variable for the rounded value
        double rawAmount = DoubleArgumentType.getDouble(context, "amount");
        final double amount = Math.round(rawAmount * 100) / 100.0;

        // Round to 2 decimal places

        if (amount <= 0) {
            context.getSource().sendFailure(Component.literal("Amount must be greater than 0."));
            return 0;
        }

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player has permission to deposit
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.DEPOSIT_FUNDS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to deposit funds."));
            return 0;
        }

        // Check if player has enough money
        EconomyManager economyManager = EconomyManager.get(level);
        if (economyManager.getPlayerBalance(player.getUUID()) < amount) {
            context.getSource().sendFailure(Component.literal("You don't have enough money."));
            return 0;
        }

        // Transfer the money
        if (economyManager.transferToCompany(player.getUUID(), company.getId(), amount)) {
            // Add transaction
            company.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.DEPOSIT, amount,
                    "Deposit from " + player.getName().getString());

            // Add log
            company.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                    "Deposited " + EconomyManager.formatCurrency(amount));

            // Notify player
            context.getSource().sendSuccess(() ->
                    Component.literal("You deposited " + EconomyManager.formatCurrency(amount) + " into your company."), false);

            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to deposit funds."));
            return 0;
        }
    }

    /**
     * Withdraw funds from company
     */
    private static int withdrawFunds(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);
        double amount = DoubleArgumentType.getDouble(context, "amount");

        // Round to 2 decimal places
        amount = Math.round(amount * 100) / 100.0;

        if (amount <= 0) {
            context.getSource().sendFailure(Component.literal("Amount must be greater than 0."));
            return 0;
        }

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player has permission to withdraw
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.WITHDRAW_FUNDS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to withdraw funds."));
            return 0;
        }

        // Check if company has enough funds
        if (company.getFunds() < amount) {
            context.getSource().sendFailure(Component.literal("The company doesn't have enough funds."));
            return 0;
        }

        // Transfer the money
        EconomyManager economyManager = EconomyManager.get(level);
        if (economyManager.transferFromCompany(company.getId(), player.getUUID(), amount)) {
            // Add transaction
            company.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.WITHDRAWAL, amount,
                    "Withdrawal by " + player.getName().getString());

            // Add log
            company.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                    "Withdrew " + EconomyManager.formatCurrency(amount));

            // Notify player
            double finalAmount = amount;
            context.getSource().sendSuccess(() ->
                    Component.literal("You withdrew " + EconomyManager.formatCurrency(finalAmount) + " from your company."), false);

            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to withdraw funds."));
            return 0;
        }
    }

    /**
     * Pay a player from company funds
     */
    private static int payPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);
        String targetName = StringArgumentType.getString(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");

        // Round to 2 decimal places
        amount = Math.round(amount * 100) / 100.0;

        if (amount <= 0) {
            context.getSource().sendFailure(Component.literal("Amount must be greater than 0."));
            return 0;
        }

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player has permission to pay
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.WITHDRAW_FUNDS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to pay from company funds."));
            return 0;
        }

        // Check if company has enough funds
        if (company.getFunds() < amount) {
            context.getSource().sendFailure(Component.literal("The company doesn't have enough funds."));
            return 0;
        }

        // Check if paying to a company (starts with #)
        if (targetName.startsWith("#")) {
            try {
                int targetCompanyId = Integer.parseInt(targetName.substring(1));
                Company targetCompany = manager.getCompany(targetCompanyId);

                if (targetCompany == null) {
                    context.getSource().sendFailure(Component.literal("Company with ID " + targetCompanyId + " not found."));
                    return 0;
                }

                // Transfer the funds
                company.withdrawFunds(amount);
                targetCompany.addFunds(amount);

                // Add transactions
                company.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.PAYMENT, amount,
                        "Payment to company " + targetCompany.getName());
                targetCompany.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.DEPOSIT, amount,
                        "Payment from company " + company.getName());

                // Add logs
                company.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                        "Paid " + EconomyManager.formatCurrency(amount) + " to company " + targetCompany.getName());
                targetCompany.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                        "Received " + EconomyManager.formatCurrency(amount) + " from company " + company.getName());

                // Notify player
                final String formattedAmount = EconomyManager.formatCurrency(amount);
                final String targetCompanyName = targetCompany.getName();
                context.getSource().sendSuccess(() ->
                        Component.literal("Paid " + formattedAmount + " to company '" + targetCompanyName + "' from company funds."), false);

                // Notify target company CEO if online
                ServerPlayer targetCeo = player.getServer().getPlayerList().getPlayer(targetCompany.getCeoId());
                if (targetCeo != null) {
                    targetCeo.sendSystemMessage(Component.literal(
                            "Your company received " + formattedAmount + " from company '" + company.getName() + "'."));
                }

                // If target company is a subsidiary, notify parent company
                if (targetCompany.hasParentCompany()) {
                    Company parentCompany = manager.getCompany(targetCompany.getParentCompanyId());
                    if (parentCompany != null) {
                        // Add log to parent company
                        parentCompany.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                                "Subsidiary " + targetCompany.getName() + " received payment of " + formattedAmount +
                                        " from company " + company.getName());

                        // Notify parent company CEO if online
                        ServerPlayer parentCeo = player.getServer().getPlayerList().getPlayer(parentCompany.getCeoId());
                        if (parentCeo != null) {
                            parentCeo.sendSystemMessage(Component.literal(
                                    "Your subsidiary " + targetCompany.getName() + " received " +
                                            formattedAmount + " from company " + company.getName()));
                        }
                    }
                }

                manager.setDirty();
                return 1;
            } catch (NumberFormatException e) {
                context.getSource().sendFailure(Component.literal("Invalid company ID format. Use #ID (e.g., #103)."));
                return 0;
            }
        } else {
            // Find target player
            UUID targetId = null;
            String targetDisplayName = null;

            // Check if target is online
            for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                if (onlinePlayer.getName().getString().equalsIgnoreCase(targetName)) {
                    targetId = onlinePlayer.getUUID();
                    targetDisplayName = onlinePlayer.getName().getString();
                    break;
                }
            }

            // If not online, try to find from offline players
            if (targetId == null) {
                MinecraftServer server = player.getServer();
                GameProfile profile = server.getProfileCache().get(targetName).orElse(null);

                if (profile != null) {
                    targetId = profile.getId();
                    targetDisplayName = profile.getName();
                }
            }

            if (targetId == null) {
                context.getSource().sendFailure(Component.literal("Player not found: " + targetName));
                return 0;
            }

            // Transfer the money
            EconomyManager economyManager = EconomyManager.get(level);
            if (economyManager.transferFromCompany(company.getId(), targetId, amount)) {
                // Add transaction
                company.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.PAYMENT, amount,
                        "Payment to " + targetDisplayName);

                // Add log
                company.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                        "Paid " + EconomyManager.formatCurrency(amount) + " to " + targetDisplayName);

                // Notify player
                final String finalTargetName = targetDisplayName;
                double finalAmount = amount;
                context.getSource().sendSuccess(() ->
                        Component.literal("Paid " + EconomyManager.formatCurrency(finalAmount) + " to " + finalTargetName + " from company funds."), false);

                // Notify target if online
                ServerPlayer targetPlayer = player.getServer().getPlayerList().getPlayer(targetId);
                if (targetPlayer != null) {
                    targetPlayer.sendSystemMessage(Component.literal(
                            "You received " + EconomyManager.formatCurrency(amount) + " from company '" + company.getName() + "'."));
                }

                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("Failed to transfer funds."));
                return 0;
            }
        }
    }

    /**
     * Check company balance
     */
    private static int checkBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Display balance
        context.getSource().sendSuccess(() ->
                Component.literal("Company Balance: " + EconomyManager.formatCurrency(company.getFunds())), false);

        return 1;
    }

    /**
     * List all subsidiaries
     */
    private static int listSubsidiaries(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if company is Tier 4
        if (company.getTier() < 4) {
            context.getSource().sendFailure(Component.literal("Only Tier 4 companies can have subsidiaries."));
            return 0;
        }

        // Get subsidiaries
        Map<Integer, Integer> subsidiaries = company.getSubsidiaries();

        if (subsidiaries.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Your company has no subsidiaries."), false);
            return 0;
        }

        // Display subsidiaries
        context.getSource().sendSuccess(() -> Component.literal("=== Company Subsidiaries ==="), false);

        int index = 1;
        for (Map.Entry<Integer, Integer> entry : subsidiaries.entrySet()) {
            int subsidiaryId = entry.getKey();
            int sharePercentage = entry.getValue();

            Company subsidiary = manager.getCompany(subsidiaryId);
            if (subsidiary == null) {
                continue; // Skip if company no longer exists
            }

            final int finalIndex = index;
            context.getSource().sendSuccess(() ->
                    Component.literal(finalIndex + ". " + subsidiary.getName() +
                            " (ID: " + subsidiaryId + ") - Revenue Share: " + sharePercentage + "%"), false);

            index++;
        }

        return 1;
    }

    /**
     * Invest in a subsidiary's R&D
     */
    private static int investInSubsidiary(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);

        int subsidiaryId = IntegerArgumentType.getInteger(context, "subsidiary");
        double amount = DoubleArgumentType.getDouble(context, "amount");

        // Round to 2 decimal places
        amount = Math.round(amount * 100) / 100.0;

        if (amount <= 0) {
            context.getSource().sendFailure(Component.literal("Amount must be greater than 0."));
            return 0;
        }

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player is CEO or has permission
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.INVEST_FUNDS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to invest funds."));
            return 0;
        }

        // Check if company is Tier 4
        if (company.getTier() < 4) {
            context.getSource().sendFailure(Component.literal("Only Tier 4 companies can invest in subsidiaries."));
            return 0;
        }

        // Check if company has the subsidiary
        if (!company.getSubsidiaries().containsKey(subsidiaryId)) {
            context.getSource().sendFailure(Component.literal("That company is not your subsidiary."));
            return 0;
        }

        // Get the subsidiary
        Company subsidiary = manager.getCompany(subsidiaryId);
        if (subsidiary == null) {
            context.getSource().sendFailure(Component.literal("Subsidiary company not found."));
            return 0;
        }

        // Check if company has enough funds
        if (company.getFunds() < amount) {
            context.getSource().sendFailure(Component.literal("Your company doesn't have enough funds."));
            return 0;
        }

        // Check if subsidiary has active research
        ResearchManager researchManager = ResearchManager.getInstance();
        if (researchManager.getActiveResearch(subsidiaryId) == null) {
            context.getSource().sendFailure(Component.literal("The subsidiary has no active research to invest in."));
            return 0;
        }

        // Transfer the funds
        company.withdrawFunds(amount);
        subsidiary.addFunds(amount);
        manager.setDirty();

        // Add transactions
        company.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.INVESTMENT, amount,
                "Investment in subsidiary " + subsidiary.getName());
        subsidiary.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.DEPOSIT, amount,
                "Investment from parent company " + company.getName());

        // Add logs
        company.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                "Invested " + EconomyManager.formatCurrency(amount) + " in subsidiary " + subsidiary.getName());
        subsidiary.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                "Received investment of " + EconomyManager.formatCurrency(amount) + " from parent company " + company.getName());

        // Notify player
        double finalAmount = amount;
        context.getSource().sendSuccess(() ->
                Component.literal("Invested " + EconomyManager.formatCurrency(finalAmount) + " in subsidiary " + subsidiary.getName()), false);

        // Notify subsidiary CEO if online
        ServerPlayer subsidiaryCeo = player.getServer().getPlayerList().getPlayer(subsidiary.getCeoId());
        if (subsidiaryCeo != null) {
            subsidiaryCeo.sendSystemMessage(Component.literal(
                    "Your company received an investment of " + EconomyManager.formatCurrency(amount) +
                            " from parent company " + company.getName()));
        }

        return 1;
    }

    /**
     * Propose a subsidiary relationship
     */
    private static int proposeSubsidiary(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);

        int targetCompanyId = IntegerArgumentType.getInteger(context, "company_id");
        int percentage = IntegerArgumentType.getInteger(context, "percentage");

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player is CEO
        if (!player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can propose subsidiary relationships."));
            return 0;
        }

        // Check if company is Tier 4
        if (company.getTier() < 4) {
            context.getSource().sendFailure(Component.literal("Only Tier 4 companies can have subsidiaries."));
            return 0;
        }

        // Get the target company
        Company targetCompany = manager.getCompany(targetCompanyId);
        if (targetCompany == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + targetCompanyId + " not found."));
            return 0;
        }

        // Check if target company is already a subsidiary
        if (targetCompany.hasParentCompany()) {
            context.getSource().sendFailure(Component.literal("That company is already a subsidiary."));
            return 0;
        }

        // Check if target company is already proposed to
        if (targetCompany.hasSubsidiaryProposal()) {
            context.getSource().sendFailure(Component.literal("That company already has a pending subsidiary proposal."));
            return 0;
        }

        // Create the proposal (expires in 24 hours)
        long expirationTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        targetCompany.setSubsidiaryProposal(company.getId(), percentage, expirationTime);
        manager.setDirty();

        // Notify player
        context.getSource().sendSuccess(() ->
                Component.literal("Sent subsidiary proposal to " + targetCompany.getName() +
                        " with " + percentage + "% revenue share."), false);

        // Notify target company CEO if online
        ServerPlayer targetCeo = player.getServer().getPlayerList().getPlayer(targetCompany.getCeoId());
        if (targetCeo != null) {
            Component message = Component.literal("§6Subsidiary Proposal§r\n")
                    .append(Component.literal("Company '" + company.getName() + "' has proposed to make your company a subsidiary.\n"))
                    .append(Component.literal("Revenue Share: " + percentage + "%\n"))
                    .append(Component.literal("This means " + percentage + "% of your company's income will go to them.\n"))
                    .append(Component.literal("Use §a/company accept " + company.getId() + "§r to accept, or ignore to decline."));

            targetCeo.sendSystemMessage(message);
        }

        return 1;
    }

    /**
     * Edit share percentage for a subsidiary
     */
    private static int editSharePercentage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);

        int subsidiaryId = IntegerArgumentType.getInteger(context, "company_id");
        int newPercentage = IntegerArgumentType.getInteger(context, "percentage");

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player is CEO
        if (!player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can edit subsidiary shares."));
            return 0;
        }

        // Check if company is Tier 4
        if (company.getTier() < 4) {
            context.getSource().sendFailure(Component.literal("Only Tier 4 companies can have subsidiaries."));
            return 0;
        }

        // Check if company has the subsidiary
        if (!company.getSubsidiaries().containsKey(subsidiaryId)) {
            context.getSource().sendFailure(Component.literal("That company is not your subsidiary."));
            return 0;
        }

        // Get the current percentage
        int currentPercentage = company.getSubsidiaries().get(subsidiaryId);

        // Check if new percentage is higher (requires approval) or lower (immediate)
        if (newPercentage > currentPercentage) {
            // Higher percentage requires approval
            Company subsidiary = manager.getCompany(subsidiaryId);
            if (subsidiary == null) {
                context.getSource().sendFailure(Component.literal("Subsidiary company not found."));
                return 0;
            }

            // Create share edit proposal
            subsidiary.setShareEditProposal(company.getId(), newPercentage);
            manager.setDirty();

            // Notify player
            context.getSource().sendSuccess(() ->
                    Component.literal("Sent share edit proposal to " + subsidiary.getName() +
                            " to increase from " + currentPercentage + "% to " + newPercentage + "%."), false);

            // Notify subsidiary CEO if online
            ServerPlayer subsidiaryCeo = player.getServer().getPlayerList().getPlayer(subsidiary.getCeoId());
            if (subsidiaryCeo != null) {
                Component message = Component.literal("§6Share Edit Proposal§r\n")
                        .append(Component.literal("Parent company '" + company.getName() + "' wants to increase your revenue share.\n"))
                        .append(Component.literal("Current: " + currentPercentage + "% → Proposed: " + newPercentage + "%\n"))
                        .append(Component.literal("Use §a/company accept " + company.getId() + "§r to accept, or ignore to decline."));

                subsidiaryCeo.sendSystemMessage(message);
            }
        } else {
            // Lower percentage is immediate
            company.getSubsidiaries().put(subsidiaryId, newPercentage);
            manager.setDirty();

            // Add log
            company.addLog(level, player.getUUID(), CompanyLog.LogType.SUBSIDIARY_CONTRACT_CHANGED,
                    "Reduced revenue share for subsidiary ID " + subsidiaryId + " from " + currentPercentage + "% to " + newPercentage + "%");

            // Notify player
            context.getSource().sendSuccess(() ->
                    Component.literal("Reduced revenue share for subsidiary from " + currentPercentage + "% to " + newPercentage + "%."), false);

            // Get the subsidiary
            Company subsidiary = manager.getCompany(subsidiaryId);
            if (subsidiary != null) {
                // Add log to subsidiary
                subsidiary.addLog(level, player.getUUID(), CompanyLog.LogType.SUBSIDIARY_CONTRACT_CHANGED,
                        "Parent company reduced revenue share from " + currentPercentage + "% to " + newPercentage + "%");

                // Notify subsidiary CEO if online
                ServerPlayer subsidiaryCeo = player.getServer().getPlayerList().getPlayer(subsidiary.getCeoId());
                if (subsidiaryCeo != null) {
                    subsidiaryCeo.sendSystemMessage(Component.literal(
                            "Your parent company has reduced your revenue share from " + currentPercentage + "% to " + newPercentage + "%."));
                }
            }
        }

        return 1;
    }

    /**
     * Accept a subsidiary or share edit proposal
     */
    private static int acceptProposal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);

        int proposingCompanyId = IntegerArgumentType.getInteger(context, "company_id");

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player is CEO
        if (!player.getUUID().equals(company.getCeoId())) {
            context.getSource().sendFailure(Component.literal("Only the CEO can accept proposals."));
            return 0;
        }

        // Check for subsidiary proposal
        if (company.hasSubsidiaryProposal() && company.getSubsidiaryProposalCompanyId() == proposingCompanyId) {
            // Accept subsidiary proposal
            Company parentCompany = manager.getCompany(proposingCompanyId);
            if (parentCompany == null) {
                context.getSource().sendFailure(Component.literal("The proposing company no longer exists."));
                return 0;
            }

            // Check if proposal is expired
            if (company.isSubsidiaryProposalExpired()) {
                company.clearSubsidiaryProposal();
                context.getSource().sendFailure(Component.literal("The subsidiary proposal has expired."));
                return 0;
            }

            // Set parent-subsidiary relationship
            int sharePercentage = company.getSubsidiaryProposalPercentage();
            company.setParentCompanyId(parentCompany.getId());
            parentCompany.getSubsidiaries().put(company.getId(), sharePercentage);

            // Clear proposal
            company.clearSubsidiaryProposal();

            // Add logs
            company.addLog(level, player.getUUID(), CompanyLog.LogType.SUBSIDIARY_CONTRACT,
                    "Became a subsidiary of " + parentCompany.getName() + " with " + sharePercentage + "% revenue share");
            parentCompany.addLog(level, player.getUUID(), CompanyLog.LogType.SUBSIDIARY_CONTRACT,
                    "Added " + company.getName() + " as a subsidiary with " + sharePercentage + "% revenue share");

            manager.setDirty();

            // Notify player
            context.getSource().sendSuccess(() ->
                    Component.literal("Your company is now a subsidiary of " + parentCompany.getName() +
                            " with a " + sharePercentage + "% revenue share."), false);

            // Notify parent company CEO if online
            ServerPlayer parentCeo = player.getServer().getPlayerList().getPlayer(parentCompany.getCeoId());
            if (parentCeo != null) {
                parentCeo.sendSystemMessage(Component.literal(
                        company.getName() + " has accepted your subsidiary proposal with a " + sharePercentage + "% revenue share."));
            }

            return 1;
        }

        // Check for share edit proposal
        if (company.hasShareEditProposal() && company.getShareEditProposalCompanyId() == proposingCompanyId) {
            // Accept share edit proposal
            Company parentCompany = manager.getCompany(proposingCompanyId);
            if (parentCompany == null) {
                context.getSource().sendFailure(Component.literal("The parent company no longer exists."));
                return 0;
            }

            // Check if this is actually a subsidiary
            if (!company.hasParentCompany() || company.getParentCompanyId() != parentCompany.getId()) {
                context.getSource().sendFailure(Component.literal("That company is not your parent company."));
                return 0;
            }

            // Get current and new percentages
            int currentPercentage = parentCompany.getSubsidiaries().getOrDefault(company.getId(), 0);
            int newPercentage = company.getShareEditProposalPercentage();

            // Update share percentage
            parentCompany.getSubsidiaries().put(company.getId(), newPercentage);

            // Clear proposal
            company.clearShareEditProposal();

            // Add logs
            company.addLog(level, player.getUUID(), CompanyLog.LogType.SUBSIDIARY_CONTRACT_CHANGED,
                    "Accepted increase in revenue share from " + currentPercentage + "% to " + newPercentage + "%");
            parentCompany.addLog(level, player.getUUID(), CompanyLog.LogType.SUBSIDIARY_CONTRACT_CHANGED,
                    "Subsidiary " + company.getName() + " accepted increase in revenue share from " +
                            currentPercentage + "% to " + newPercentage + "%");

            manager.setDirty();

            // Notify player
            context.getSource().sendSuccess(() ->
                    Component.literal("You accepted the revenue share increase from " + currentPercentage + "% to " + newPercentage + "%."), false);

            // Notify parent company CEO if online
            ServerPlayer parentCeo = player.getServer().getPlayerList().getPlayer(parentCompany.getCeoId());
            if (parentCeo != null) {
                parentCeo.sendSystemMessage(Component.literal(
                        company.getName() + " has accepted the revenue share increase from " +
                                currentPercentage + "% to " + newPercentage + "%."));
            }

            return 1;
        }

        // No valid proposal found
        context.getSource().sendFailure(Component.literal("No valid proposal from that company."));
        return 0;
    }

    /**
     * Set minimum balance percentage
     */
    private static int setMinimumBalancePercentage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        double percentage = DoubleArgumentType.getDouble(context, "percentage");

        CompanyConfig config = CompanyConfig.getInstance();
        config.setMinimumBalancePercentage(percentage);
        config.saveConfig();

        context.getSource().sendSuccess(() ->
                Component.literal("Set minimum balance percentage to " + percentage + "%."), true);

        return 1;
    }

    /**
     * Set grace period days
     */
    private static int setGracePeriodDays(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int days = IntegerArgumentType.getInteger(context, "days");

        CompanyConfig config = CompanyConfig.getInstance();
        config.setGracePeriodDays(days);
        config.saveConfig();

        context.getSource().sendSuccess(() ->
                Component.literal("Set maintenance grace period to " + days + " days."), true);

        return 1;
    }

    /**
     * Set base company value
     */
    private static int setBaseCompanyValue(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        double value = DoubleArgumentType.getDouble(context, "value");

        CompanyConfig config = CompanyConfig.getInstance();
        config.setBaseCompanyValue(value);
        config.saveConfig();

        context.getSource().sendSuccess(() ->
                Component.literal("Set base company value to " + EconomyManager.formatCurrency(value) + "."), true);

        return 1;
    }

    /**
     * Show maintenance configuration
     */
    private static int showMaintenanceConfig(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CompanyConfig config = CompanyConfig.getInstance();

        context.getSource().sendSuccess(() -> Component.literal("=== Company Maintenance Configuration ==="), false);
        context.getSource().sendSuccess(() ->
                Component.literal("Minimum Balance: " + config.getMinimumBalancePercentage() + "% of company value"), false);
        context.getSource().sendSuccess(() ->
                Component.literal("Grace Period: " + config.getGracePeriodDays() + " days"), false);
        context.getSource().sendSuccess(() ->
                Component.literal("Base Company Value: " + EconomyManager.formatCurrency(config.getBaseCompanyValue())), false);

        // Show example minimum balances for each tier
        context.getSource().sendSuccess(() -> Component.literal("Minimum Required Balances by Tier:"), false);
        for (int tier = 1; tier <= 4; tier++) {
            double minBalance = config.calculateMinimumBalance(tier);
            int finalTier = tier;
            context.getSource().sendSuccess(() ->
                    Component.literal("- Tier " + finalTier + ": " + EconomyManager.formatCurrency(minBalance)), false);
        }

        return 1;
    }
    /**
     * Revoke invitation command
     */
    private static int revokeInvitation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String targetPlayerName = StringArgumentType.getString(context, "player");

        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player has permission to invite (same permission needed to revoke)
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.INVITE_PLAYERS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to revoke invitations."));
            return 0;
        }

        // Find the target player UUID
        UUID targetPlayerId = null;
        MinecraftServer server = player.getServer();

        // Check online players
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (onlinePlayer.getName().getString().equalsIgnoreCase(targetPlayerName)) {
                targetPlayerId = onlinePlayer.getUUID();
                break;
            }
        }

        // If not found online, try offline players
        if (targetPlayerId == null) {
            targetPlayerId = server.getProfileCache().get(targetPlayerName)
                    .map(GameProfile::getId)
                    .orElse(null);
        }

        if (targetPlayerId == null) {
            context.getSource().sendFailure(Component.literal("Player not found: " + targetPlayerName));
            return 0;
        }

        // Check if there's an invitation for this player to this company
        if (!manager.hasInvitation(targetPlayerId, company.getId())) {
            context.getSource().sendFailure(Component.literal("No pending invitation for " + targetPlayerName + " to your company."));
            return 0;
        }

        // Revoke the invitation
        if (manager.revokeInvitation(targetPlayerId)) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Revoked invitation for " + targetPlayerName + " to join your company."), false);

            // Notify the player if they're online
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetPlayerId);
            if (targetPlayer != null) {
                targetPlayer.sendSystemMessage(Component.literal(
                        "Your invitation to join company '" + company.getName() + "' has been revoked."));
            }

            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to revoke invitation."));
            return 0;
        }
    }


    /**
     * Display information about the player's own company
     */
    private static int showOwnCompanyInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        return displayCompanyInfo(context, company);
    }



    /**
     * Display information about a specified company
     */
    private static int companyInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String companyName = StringArgumentType.getString(context, "company_name");
        CompanyManager manager = CompanyManager.get((ServerLevel) context.getSource().getLevel());

        // Check if it's a company ID (starts with #)
        Company company = null;
        if (companyName.startsWith("#")) {
            try {
                int companyId = Integer.parseInt(companyName.substring(1));
                company = manager.getCompany(companyId);
            } catch (NumberFormatException e) {
                context.getSource().sendFailure(Component.literal("Invalid company ID format. Use #ID (e.g., #103)."));
                return 0;
            }
        } else {
            // Find company by name
            company = manager.getAllCompanies().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(companyName))
                    .findFirst().orElse(null);
        }

        if (company == null) {
            context.getSource().sendFailure(Component.literal("Company not found: " + companyName));
            return 0;
        }

        return displayCompanyInfo(context, company);
    }

    /**
     * Helper method to display company info
     */
    private static int displayCompanyInfo(CommandContext<CommandSourceStack> context, Company company) {
        // Get CEO name
        MinecraftServer server = context.getSource().getServer();
        String ceoName = server.getProfileCache()
                .get(company.getCeoId())
                .map(GameProfile::getName)
                .orElse("Unknown");

        // Display company info
        context.getSource().sendSuccess(() -> Component.literal("=== Company Information ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("Name: " + company.getName()), false);
        context.getSource().sendSuccess(() -> Component.literal("ID: " + company.getId()), false);
        context.getSource().sendSuccess(() -> Component.literal("Tier: " + company.getTier()), false);
        context.getSource().sendSuccess(() -> Component.literal("Domain: " + company.getDomain()), false);

        // Check if subdomain is null or empty
        String primarySubdomain = company.getSubdomain();
        if (primarySubdomain != null && !primarySubdomain.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Primary Subdomain: " + primarySubdomain), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Primary Subdomain: None"), false);
        }

        // Display all unlocked subdomains
        Set<String> unlockedSubdomains = company.getUnlockedSubdomains();
        if (!unlockedSubdomains.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("=== Unlocked Subdomains ==="), false);
            int index = 1;
            for (String subdomain : unlockedSubdomains) {
                final int i = index++;
                context.getSource().sendSuccess(() -> Component.literal(i + ". " + subdomain), false);
            }
        } else {
            context.getSource().sendSuccess(() -> Component.literal("No subdomains unlocked."), false);
        }

        // Format funds with commas
        String formattedFunds = String.format("%,.2f", company.getFunds());
        context.getSource().sendSuccess(() -> Component.literal("Funds: " + formattedFunds + " FC$"), false);
        context.getSource().sendSuccess(() -> Component.literal("CEO: " + ceoName), false);

        // Display members
        context.getSource().sendSuccess(() -> Component.literal("=== Members ==="), false);
        for (Map.Entry<UUID, CompanyRole> entry : company.getMembers().entrySet()) {
            UUID memberId = entry.getKey();
            CompanyRole role = entry.getValue();

            String memberName = server.getProfileCache()
                    .get(memberId)
                    .map(GameProfile::getName)
                    .orElse("Unknown");

            context.getSource().sendSuccess(() ->
                    Component.literal("- " + memberName + " (" + role.name() + ")"), false);
        }

        // Display parent company info if this is a subsidiary
        if (company.hasParentCompany()) {
            Company parentCompany = CompanyManager.getCompany(company.getParentCompanyId());
            if (parentCompany != null) {
                int sharePercentage = parentCompany.getSubsidiaries().getOrDefault(company.getId(), 0);
                context.getSource().sendSuccess(() -> Component.literal("=== Subsidiary Information ==="), false);
                context.getSource().sendSuccess(() -> Component.literal("Parent Company: " + parentCompany.getName() + " (ID: " + parentCompany.getId() + ")"), false);
                context.getSource().sendSuccess(() -> Component.literal("Revenue Share: " + sharePercentage + "%"), false);
            }
        }

        // Display subsidiaries if this is a mother company
        if (!company.getSubsidiaries().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("=== Subsidiaries ==="), false);
            for (Map.Entry<Integer, Integer> entry : company.getSubsidiaries().entrySet()) {
                int subsidiaryId = entry.getKey();
                int sharePercentage = entry.getValue();

                Company subsidiary = CompanyManager.getCompany(subsidiaryId);
                if (subsidiary != null) {
                    context.getSource().sendSuccess(() ->
                            Component.literal("- " + subsidiary.getName() + " (ID: " + subsidiaryId + ") - Revenue Share: " + sharePercentage + "%"), false);
                }
            }
        }

        return 1;
    }

    private static int payCompanyCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CompanyManager manager = CompanyManager.get(level);

        int targetCompanyId = IntegerArgumentType.getInteger(context, "company_id");
        double amount = DoubleArgumentType.getDouble(context, "amount");

        // Round to 2 decimal places
        amount = Math.round(amount * 100) / 100.0;

        if (amount <= 0) {
            context.getSource().sendFailure(Component.literal("Amount must be greater than 0."));
            return 0;
        }

        // Check if player is in a company
        Company company = manager.getPlayerCompany(player.getUUID());
        if (company == null) {
            context.getSource().sendFailure(Component.literal("You are not a member of any company."));
            return 0;
        }

        // Check if player has permission to pay
        if (!player.getUUID().equals(company.getCeoId()) &&
                !company.hasPermission(player.getUUID(), CompanyPermission.WITHDRAW_FUNDS)) {
            context.getSource().sendFailure(Component.literal("You don't have permission to pay from company funds."));
            return 0;
        }

        // Check if company has enough funds
        if (company.getFunds() < amount) {
            context.getSource().sendFailure(Component.literal("The company doesn't have enough funds."));
            return 0;
        }

        Company targetCompany = manager.getCompany(targetCompanyId);

        if (targetCompany == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + targetCompanyId + " not found."));
            return 0;
        }

        // Transfer the funds
        company.withdrawFunds(amount);
        targetCompany.addFunds(amount);

        // Add transactions
        company.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.PAYMENT, amount,
                "Payment to company " + targetCompany.getName());
        targetCompany.addTransaction(player.getUUID(), CompanyTransaction.TransactionType.DEPOSIT, amount,
                "Payment from company " + company.getName());

        // Add logs
        company.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                "Paid " + EconomyManager.formatCurrency(amount) + " to company " + targetCompany.getName());
        targetCompany.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                "Received " + EconomyManager.formatCurrency(amount) + " from company " + company.getName());

        // Notify player
        final String formattedAmount = EconomyManager.formatCurrency(amount);
        final String targetCompanyName = targetCompany.getName();
        context.getSource().sendSuccess(() ->
                Component.literal("Paid " + formattedAmount + " to company '" + targetCompanyName + "' from company funds."), false);

        // Notify target company CEO if online
        ServerPlayer targetCeo = player.getServer().getPlayerList().getPlayer(targetCompany.getCeoId());
        if (targetCeo != null) {
            targetCeo.sendSystemMessage(Component.literal(
                    "Your company received " + formattedAmount + " from company '" + company.getName() + "'."));
        }

        // If target company is a subsidiary, notify parent company
        if (targetCompany.hasParentCompany()) {
            Company parentCompany = manager.getCompany(targetCompany.getParentCompanyId());
            if (parentCompany != null) {
                // Add log to parent company
                parentCompany.addLog(level, player.getUUID(), CompanyLog.LogType.FINANCIAL,
                        "Subsidiary " + targetCompany.getName() + " received payment of " + formattedAmount +
                                " from company " + company.getName());

                // Notify parent company CEO if online
                ServerPlayer parentCeo = player.getServer().getPlayerList().getPlayer(parentCompany.getCeoId());
                if (parentCeo != null) {
                    parentCeo.sendSystemMessage(Component.literal(
                            "Your subsidiary " + targetCompany.getName() + " received " +
                                    formattedAmount + " from company " + company.getName()));
                }
            }
        }

        manager.setDirty();
        return 1;
    }
}