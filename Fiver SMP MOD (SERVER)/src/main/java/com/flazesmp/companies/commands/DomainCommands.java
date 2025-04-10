// Owned by BENZOOgataga. Not for redistribution or external server use.

package com.flazesmp.companies.commands;

import com.flazesmp.companies.FlazeSMP;
import com.flazesmp.companies.config.CompanyDomainsConfig;
import com.flazesmp.companies.config.CompanyDomainsConfig.DomainConfig;
import com.flazesmp.companies.config.CompanyDomainsConfig.SubdomainConfig;
import com.flazesmp.companies.data.CompanyLog;
import com.flazesmp.companies.data.CompanyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.flazesmp.companies.data.Company;
import java.util.List;

/**
 * Commands for managing company domains and subdomains
 */
public class DomainCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Subdomain commands
        dispatcher.register(
                Commands.literal("subdomain")
                        .then(Commands.literal("info")
                                .requires(source -> hasPermission(source, "fsmp.economy.subdomain.info"))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(DomainCommands::subdomainInfo)))
                        .then(Commands.literal("list")
                                .requires(source -> hasPermission(source, "fsmp.economy.subdomain.list"))
                                .executes(DomainCommands::listSubdomains))
                        .then(Commands.literal("create")
                                .requires(source -> hasPermission(source, "fsmp.economy.subdomain.create"))
                                .then(Commands.argument("subdomain", StringArgumentType.string())
                                        .then(Commands.argument("domain", StringArgumentType.string())
                                                .executes(DomainCommands::createSubdomain))))
                        .then(Commands.literal("grant")
                                .requires(source -> hasPermission(source, "fsmp.economy.subdomain.grant"))
                                .then(Commands.argument("subdomain_id", StringArgumentType.string())
                                        .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                                .executes(DomainCommands::grantSubdomain))))
                        .then(Commands.literal("revoke")
                                .requires(source -> hasPermission(source, "fsmp.economy.subdomain.revoke"))
                                .then(Commands.argument("subdomain_id", StringArgumentType.string())
                                        .then(Commands.argument("company_id", IntegerArgumentType.integer(1))
                                                .executes(DomainCommands::revokeSubdomain))))
                        .then(Commands.literal("delete")
                                .requires(source -> hasPermission(source, "fsmp.economy.domain.delete"))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(DomainCommands::deleteSubdomain)))
        );

        dispatcher.register(
                Commands.literal("domain")
                        .then(Commands.literal("create")
                                .requires(source -> hasPermission(source, "fsmp.economy.domain.create"))
                                .then(Commands.argument("domain", StringArgumentType.string())
                                        .executes(DomainCommands::createDomain)))
                        .then(Commands.literal("config")
                                .requires(source -> hasPermission(source, "fsmp.economy.domain.admin"))
                                .then(Commands.literal("reload")
                                        .executes(DomainCommands::reloadConfig)))
        );
    }
    // Reload Config
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        // Force reload the config from disk without saving first
        CompanyDomainsConfig.reloadConfig();

        context.getSource().sendSuccess(() ->
                        Component.literal("§aDomain configuration reloaded successfully"),
                true);

        return 1;
    }

    /**
     * Check if the command source has the required permission
     */
    private static boolean hasPermission(CommandSourceStack source, String permission) {
        if (source.hasPermission(2)) { // OP level 2+
            return true;
        }

        if (!source.isPlayer()) {
            return true; // Console always has access
        }

        try {
            ServerPlayer player = source.getPlayerOrException();
            // Use your permission system here
            // For example with LuckPerms:
            // return LuckPermsProvider.get().getUserManager().getUser(player.getUUID()).getCachedData().getPermissionData().checkPermission(permission).asBoolean();

            // For simplicity in this example, we'll just check OP status
            return source.hasPermission(2);
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    /**
     * Display information about a domain or subdomain
     */
    private static int subdomainInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        CompanyDomainsConfig config = CompanyDomainsConfig.getInstance();

        // First check if it's a domain
        DomainConfig domain = config.getDomain(name);
        if (domain != null) {
            // It's a domain
            context.getSource().sendSuccess(() -> Component.literal("§6=== Domain: " + domain.name + " ==="), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Description: §f" + domain.description), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Icon: §f" + domain.icon), false);
            context.getSource().sendSuccess(() -> Component.literal("§7Subdomains: §f" + domain.subdomains.size()), false);

            return 1;
        }

        // Check if it's a subdomain in any domain
        for (DomainConfig d : config.getDomains()) {
            for (SubdomainConfig subdomain : d.subdomains) {
                if (subdomain.name.equalsIgnoreCase(name)) {
                    // It's a subdomain
                    context.getSource().sendSuccess(() -> Component.literal("§6=== Subdomain: " + subdomain.name + " ==="), false);
                    context.getSource().sendSuccess(() -> Component.literal("§7Parent Domain: §f" + d.name), false);
                    context.getSource().sendSuccess(() -> Component.literal("§7Icon: §f" + subdomain.icon), false);
                    context.getSource().sendSuccess(() -> Component.literal("§7Buff: §f" + subdomain.buff), false);
                    context.getSource().sendSuccess(() -> Component.literal("§7Locked: §f" + (subdomain.locked ? "Yes" : "No")), false);

                    return 1;
                }
            }
        }

        // Not found
        context.getSource().sendFailure(Component.literal("§cDomain or subdomain not found: §f" + name));
        return 0;
    }

    /**
     * List all domains and subdomains
     */
    private static int listSubdomains(CommandContext<CommandSourceStack> context) {
        CompanyDomainsConfig config = CompanyDomainsConfig.getInstance();
        List<DomainConfig> domains = config.getDomains();

        context.getSource().sendSuccess(() -> Component.literal("§6=== Domains and Subdomains ==="), false);

        for (DomainConfig domain : domains) {
            context.getSource().sendSuccess(() -> Component.literal("§a" + domain.name + " §7- §f" + domain.description), false);

            for (SubdomainConfig subdomain : domain.subdomains) {
                String lockedStatus = subdomain.locked ? "§c[Locked]" : "§a[Unlocked]";
                context.getSource().sendSuccess(() ->
                                Component.literal("  §7- §f" + subdomain.name + " " + lockedStatus),
                        false);
            }
        }

        return domains.size();
    }

    /**
     * Create a new subdomain under a specified domain
     */
    private static int createSubdomain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String subdomainName = StringArgumentType.getString(context, "subdomain");
        String domainName = StringArgumentType.getString(context, "domain");

        CompanyDomainsConfig config = CompanyDomainsConfig.getInstance();
        DomainConfig domain = config.getDomain(domainName);

        if (domain == null) {
            context.getSource().sendFailure(
                    Component.literal("§cDomain not found: §f" + domainName +
                            "\n§cUse §f/domain create " + domainName + "§c to create it first."));
            return 0;
        }

        // Check if subdomain already exists
        for (SubdomainConfig subdomain : domain.subdomains) {
            if (subdomain.name.equalsIgnoreCase(subdomainName)) {
                context.getSource().sendFailure(
                        Component.literal("§cSubdomain already exists: §f" + subdomainName));
                return 0;
            }
        }

        // Create the subdomain with default values
        domain.addSubdomain(subdomainName, "minecraft:paper", "No buff set", false);
        config.saveConfig();

        context.getSource().sendSuccess(() ->
                        Component.literal("§aCreated subdomain: §f" + subdomainName + "§a under domain: §f" + domainName +
                                "\n§7Use commands to set icon and buff."),
                true);

        return 1;
    }

    /**
     * Create a new domain
     */
    private static int createDomain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String domainName = StringArgumentType.getString(context, "domain");

        CompanyDomainsConfig config = CompanyDomainsConfig.getInstance();

        // Check if domain already exists
        if (config.getDomain(domainName) != null) {
            context.getSource().sendFailure(
                    Component.literal("§cDomain already exists: §f" + domainName));
            return 0;
        }

        // Create the domain with default values
        DomainConfig domain = CompanyDomainsConfig.createDomain(domainName, "minecraft:paper", "No description set");
        config.addDomain(domain);
        config.saveConfig();

        context.getSource().sendSuccess(() ->
                        Component.literal("§aCreated domain: §f" + domainName +
                                "\n§7Use commands to set icon and description."),
                true);

        return 1;
    }

    /**
     * Delete a subdomain or domain
     */
    private static int deleteSubdomain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        CompanyDomainsConfig config = CompanyDomainsConfig.getInstance();

        // First check if it's a domain
        DomainConfig domain = config.getDomain(name);
        if (domain != null) {
            // It's a domain - check if it's empty
            if (!domain.subdomains.isEmpty()) {
                context.getSource().sendFailure(
                        Component.literal("§cCannot delete domain: §f" + name +
                                "\n§cDomain must be empty. It contains " + domain.subdomains.size() + " subdomains."));
                return 0;
            }

            // Domain is empty, delete it
            config.removeDomain(name);
            config.saveConfig();

            context.getSource().sendSuccess(() ->
                            Component.literal("§aDeleted domain: §f" + name),
                    true);

            return 1;
        }

        // Check if it's a subdomain in any domain
        for (DomainConfig d : config.getDomains()) {
            if (d.removeSubdomain(name)) {
                config.saveConfig();

                context.getSource().sendSuccess(() ->
                                Component.literal("§aDeleted subdomain: §f" + name + "§a from domain: §f" + d.name),
                        true);

                return 1;
            }
        }

        // Not found
        context.getSource().sendFailure(Component.literal("§cDomain or subdomain not found: §f" + name));
        return 0;
    }

    // In DomainCommands.java - add these methods
    private static int grantSubdomain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer staff = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) staff.level();

        String subdomainId = StringArgumentType.getString(context, "subdomain_id");
        int companyId = IntegerArgumentType.getInteger(context, "company_id");

        CompanyManager manager = CompanyManager.get(level);
        Company company = manager.getCompany(companyId);

        if (company == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + companyId + " not found."));
            return 0;
        }

        // Check if the subdomain exists
        if (!isValidSubdomain(subdomainId)) {
            context.getSource().sendFailure(Component.literal("Subdomain '" + subdomainId + "' not found."));
            return 0;
        }

        // Check if company already has this subdomain
        if (company.getUnlockedSubdomains().contains(subdomainId)) {
            context.getSource().sendFailure(Component.literal("Company already has access to this subdomain."));
            return 0;
        }

        // Check if company's tier allows multiple subdomains
        if (company.getTier() < 2 && company.getUnlockedSubdomains().size() >= 1) {
            context.getSource().sendFailure(Component.literal("Company is Tier 1 and can only have one subdomain."));
            return 0;
        }

        // Grant the subdomain
        company.getUnlockedSubdomains().add(subdomainId);
        company.addLog(level, staff.getUUID(), CompanyLog.LogType.SUBDOMAIN_UNLOCKED,
                "Granted access to subdomain: " + subdomainId);
        manager.setDirty();

        // Notify staff
        context.getSource().sendSuccess(() ->
                Component.literal("Granted subdomain '" + subdomainId + "' to company '" + company.getName() + "'"), true);

        // Notify company CEO
        ServerPlayer ceo = staff.getServer().getPlayerList().getPlayer(company.getCeoId());
        if (ceo != null) {
            ceo.sendSystemMessage(Component.literal("§aYour company has been granted access to subdomain: §f" + subdomainId));
        }

        return 1;
    }
    private static int revokeSubdomain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer staff = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) staff.level();

        String subdomainId = StringArgumentType.getString(context, "subdomain_id");
        int companyId = IntegerArgumentType.getInteger(context, "company_id");

        CompanyManager manager = CompanyManager.get(level);
        Company company = manager.getCompany(companyId);

        if (company == null) {
            context.getSource().sendFailure(Component.literal("Company with ID " + companyId + " not found."));
            return 0;
        }

        // Check if company has this subdomain
        if (!company.getUnlockedSubdomains().contains(subdomainId)) {
            context.getSource().sendFailure(Component.literal("Company does not have access to this subdomain."));
            return 0;
        }

        // Check if this is the company's primary subdomain
        if (subdomainId.equals(company.getSubdomain())) {
            context.getSource().sendFailure(Component.literal("Cannot revoke a company's primary subdomain."));
            return 0;
        }

        // Revoke the subdomain
        company.getUnlockedSubdomains().remove(subdomainId);
        company.addLog(level, staff.getUUID(), CompanyLog.LogType.SUBDOMAIN_UNLOCKED,
                "Revoked access to subdomain: " + subdomainId);
        manager.setDirty();

        // Notify staff
        context.getSource().sendSuccess(() ->
                Component.literal("Revoked subdomain '" + subdomainId + "' from company '" + company.getName() + "'"), true);

        // Notify company CEO
        ServerPlayer ceo = staff.getServer().getPlayerList().getPlayer(company.getCeoId());
        if (ceo != null) {
            ceo.sendSystemMessage(Component.literal("§cYour company's access to subdomain: §f" + subdomainId + " §chas been revoked."));
        }

        return 1;
    }

    private static boolean isValidSubdomain(String subdomainId) {
        CompanyDomainsConfig config = CompanyDomainsConfig.getInstance();

        // Format might be "domain:subdomain" or just "subdomain"
        if (subdomainId.contains(":")) {
            String[] parts = subdomainId.split(":", 2);
            String domainName = parts[0];
            String subdomainName = parts[1];

            DomainConfig domain = config.getDomain(domainName);
            if (domain == null) return false;

            for (SubdomainConfig subdomain : domain.subdomains) {
                if (subdomain.name.equalsIgnoreCase(subdomainName)) {
                    return true;
                }
            }
            return false;
        } else {
            // Just check if any domain has this subdomain
            for (DomainConfig domain : config.getDomains()) {
                for (SubdomainConfig subdomain : domain.subdomains) {
                    if (subdomain.name.equalsIgnoreCase(subdomainId)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
