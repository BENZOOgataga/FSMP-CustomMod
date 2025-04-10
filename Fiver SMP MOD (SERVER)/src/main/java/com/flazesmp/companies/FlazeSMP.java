// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies;

import com.flazesmp.companies.commands.CompanyCommands;
import com.flazesmp.companies.commands.DomainCommands;
import com.flazesmp.companies.commands.EconomyCommands;
import com.flazesmp.companies.common.network.NetworkHandler;
import com.flazesmp.companies.config.CompanyConfig;
import com.flazesmp.companies.data.Company;
import com.flazesmp.companies.data.CompanyLog;
import com.flazesmp.companies.data.CompanyManager;
import com.flazesmp.companies.data.CompanyTransaction;
import com.flazesmp.companies.economy.EconomyManager;
import com.flazesmp.companies.market.MarketCommands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.UUID;

@Mod("flazesmp")
public class FlazeSMP {
    public static final String MOD_ID = "flazesmp";
    public static final Logger LOGGER = LogManager.getLogger();

    // Define permission nodes as static fields
    public static final PermissionNode<Boolean> ECO_SET_BALANCE = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "economy.setbalance"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> ECO_ADD_BALANCE = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "economy.addbalance"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> ECO_REMOVE_BALANCE = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "economy.removebalance"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMPANY_DISBAND = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "companies.disbandcompany"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMPANY_ACTIVE = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "companies.active"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMPANY_INACTIVE = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "companies.inactive"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMPANY_VALIDATE_UPGRADE = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "companies.validateupgrade"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );
    public static final PermissionNode<Boolean> COMPANY_RESEARCH_ADD = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "companies.researchadd"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );
    public static final PermissionNode<Boolean> ECONOMY_PAYSPY = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "economy.payspy"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );
    // Add market permissions
    PermissionNode<Boolean> MARKET_PERMISSION = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "economy.market"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    PermissionNode<Boolean> MARKET_DISABLE_ITEM = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "economy.market.disableitem"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    PermissionNode<Boolean> MARKET_THRESHOLD = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "economy.market.threshold"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    PermissionNode<Boolean> MARKET_GRAPH = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "economy.market.graph"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    public FlazeSMP() {
        LOGGER.info("FlazeSMP initializing");

        // Register shutdown hook to save companies when server stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Saving companies on shutdown...");
            CompanyManager.getInstance().saveCompanies();
            LOGGER.info("Companies saved!");
        }));
        // Register mod event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register common setup
        modEventBus.addListener(this::commonSetup);

        // Register client setup only on the client side

        // Register server setup only on the server side
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
            modEventBus.addListener(com.flazesmp.companies.server.ModInitializer::init);
        });
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register on the Forge event bus, not the mod event bus
        MinecraftForge.EVENT_BUS.addListener(this::registerPermissions);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Initialize network
        NetworkHandler.init();
    }


    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("FlazeSMP Mod Setup");
    }

    // Register permissions during the appropriate event
    private void registerPermissions(final PermissionGatherEvent.Nodes event) {
        LOGGER.info("Registering FlazeSMP permissions");

        // Add all permission nodes
        event.addNodes(
                ECO_SET_BALANCE,
                ECO_ADD_BALANCE,
                ECO_REMOVE_BALANCE,
                COMPANY_DISBAND,
                COMPANY_ACTIVE,
                COMPANY_INACTIVE,
                COMPANY_NOTIFY_UPGRADE,
                COMPANY_VALIDATE_UPGRADE,
                COMPANY_RESEARCH_ADD,
                ECONOMY_PAYSPY,
                MARKET_PERMISSION,
                MARKET_DISABLE_ITEM,
                MARKET_THRESHOLD,
                MARKET_GRAPH
        );
    }
    public static final PermissionNode<Boolean> COMPANY_NOTIFY_UPGRADE = new PermissionNode<>(
            new ResourceLocation(MOD_ID, "companies.notifyupgrade"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(2)
    );

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CompanyCommands.register(event.getDispatcher());
        EconomyCommands.register(event.getDispatcher());
        DomainCommands.register(event.getDispatcher());
        MarketCommands.register(event.getDispatcher()); // Add this line
        LOGGER.info("FlazeSMP commands registered");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {

        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                // Auto-save every 5 minutes (6000 ticks)
                if (server.getTickCount() % 6000 == 0) {
                    LOGGER.info("Auto-saving companies...");
                    CompanyManager.getInstance().saveCompanies();
                }
            }
        }

        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                // Check once per hour (approximately)
                if (server.getTickCount() % 72000 == 0) { // 1 hour * 60 minutes * 60 seconds * 20 ticks
                    processCompanyMaintenance(server);
                }

                // Check once per day (approximately)
                if (server.getTickCount() % 1728000 == 0) { // 24 hours * 60 minutes * 60 seconds * 20 ticks
                    processRevenueSharing(server);
                }
            }
        }
    }

    private void processCompanyMaintenance(MinecraftServer server) {
        ServerLevel level = server.overworld();
        CompanyManager manager = CompanyManager.get(level);
        CompanyConfig config = CompanyConfig.getInstance();

        // Get all companies
        Collection<Company> companies = manager.getAllCompanies();

        // Current time
        long currentTime = System.currentTimeMillis();

        // Process maintenance checks
        for (Company company : companies) {
            // Skip if company meets requirements
            if (company.meetsMaintenanceRequirements()) {
                // If previously warned, clear the warning
                if (company.isMaintenanceWarningIssued()) {
                    company.setMaintenanceWarningIssued(false);
                    company.setMaintenanceWarningTime(0);

                    // Notify CEO if online
                    ServerPlayer ceo = server.getPlayerList().getPlayer(company.getCeoId());
                    if (ceo != null) {
                        ceo.sendSystemMessage(Component.literal(
                                "§aYour company now meets the minimum balance requirements. Warning cleared."));
                    }
                }
                continue;
            }

            // Company doesn't meet requirements
            if (!company.isMaintenanceWarningIssued()) {
                // First warning
                company.setMaintenanceWarningIssued(true);
                company.setMaintenanceWarningTime(currentTime);

                // Add log
                company.addLog(level, null, CompanyLog.LogType.MAINTENANCE_WARNING,
                        "Company does not meet minimum balance requirement of " +
                                EconomyManager.formatCurrency(company.getMinimumRequiredBalance()));

                // Notify CEO if online
                ServerPlayer ceo = server.getPlayerList().getPlayer(company.getCeoId());
                if (ceo != null) {
                    Component message = Component.literal("§c=== COMPANY MAINTENANCE WARNING ===")
                            .append(Component.literal("\n§7Your company does not meet the minimum balance requirement."))
                            .append(Component.literal("\n§7Current balance: §f" + EconomyManager.formatCurrency(company.getFunds())))
                            .append(Component.literal("\n§7Required balance: §f" + EconomyManager.formatCurrency(company.getMinimumRequiredBalance())))
                            .append(Component.literal("\n§7You have §f" + config.getGracePeriodDays() + " days§7 to deposit funds or your company will be disbanded."));

                    ceo.sendSystemMessage(message);
                }
            } else {
                // Check if grace period has expired
                long warningTime = company.getMaintenanceWarningTime();
                long gracePeriodMillis = config.getGracePeriodDays() * 24 * 60 * 60 * 1000L;

                if (currentTime - warningTime > gracePeriodMillis) {
                    // Grace period expired, disband the company

                    // Notify all members if online
                    for (UUID memberId : company.getMembers().keySet()) {
                        ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                        if (member != null) {
                            member.sendSystemMessage(Component.literal(
                                    "§c=== COMPANY DISBANDED ===\n" +
                                            "§7Your company '" + company.getName() + "' has been disbanded due to insufficient funds."));
                        }
                    }

                    // Disband the company
                    manager.disbandCompany(company.getId());

                    // Log the event
                    FlazeSMP.LOGGER.info("Company '" + company.getName() + "' (ID: " + company.getId() +
                            ") has been disbanded due to insufficient funds.");
                } else {
                    // Still in grace period, send reminder to CEO if online
                    ServerPlayer ceo = server.getPlayerList().getPlayer(company.getCeoId());
                    if (ceo != null) {
                        // Calculate days remaining
                        long daysRemaining = (gracePeriodMillis - (currentTime - warningTime)) / (24 * 60 * 60 * 1000L) + 1;

                        Component message = Component.literal("§c=== COMPANY MAINTENANCE REMINDER ===")
                                .append(Component.literal("\n§7Your company still does not meet the minimum balance requirement."))
                                .append(Component.literal("\n§7Current balance: §f" + EconomyManager.formatCurrency(company.getFunds())))
                                .append(Component.literal("\n§7Required balance: §f" + EconomyManager.formatCurrency(company.getMinimumRequiredBalance())))
                                .append(Component.literal("\n§7You have §f" + daysRemaining + " days§7 remaining to deposit funds."));

                        ceo.sendSystemMessage(message);
                    }
                }
            }
        }

        // Save changes
        manager.setDirty();
    }

    private void processRevenueSharing(MinecraftServer server) {
        ServerLevel level = server.overworld();
        CompanyManager manager = CompanyManager.get(level);

        // Get all companies
        Collection<Company> companies = manager.getAllCompanies();

        // Process revenue sharing for subsidiaries
        for (Company company : companies) {
            // Check if company is a subsidiary
            if (company.hasParentCompany()) {
                Company parentCompany = manager.getCompany(company.getParentCompanyId());
                if (parentCompany == null) {
                    // Parent company no longer exists, clear the relationship
                    company.setParentCompanyId(-1);
                    continue;
                }

                // Get revenue share percentage
                int sharePercentage = parentCompany.getSubsidiaries().getOrDefault(company.getId(), 0);
                if (sharePercentage <= 0) {
                    continue;
                }

                // Calculate share amount
                double shareAmount = (company.getFunds() * sharePercentage) / 100.0;

                // Round to 2 decimal places
                shareAmount = Math.round(shareAmount * 100) / 100.0;

                if (shareAmount <= 0) {
                    continue;
                }

                // Check if subsidiary has enough funds
                if (company.getFunds() < shareAmount) {
                    // Not enough funds, cancel the contract
                    parentCompany.getSubsidiaries().remove(company.getId());
                    company.setParentCompanyId(-1);

                    // Add logs
                    company.addLog(level, null, CompanyLog.LogType.SUBSIDIARY_CONTRACT,
                            "Subsidiary contract with " + parentCompany.getName() + " canceled due to insufficient funds");
                    parentCompany.addLog(level, null, CompanyLog.LogType.SUBSIDIARY_CONTRACT,
                            "Subsidiary contract with " + company.getName() + " canceled due to insufficient funds");

                    // Notify CEOs if online
                    ServerPlayer companyCeo = server.getPlayerList().getPlayer(company.getCeoId());
                    if (companyCeo != null) {
                        companyCeo.sendSystemMessage(Component.literal(
                                "§cYour subsidiary contract with " + parentCompany.getName() +
                                        " has been canceled due to insufficient funds."));
                    }

                    ServerPlayer parentCeo = server.getPlayerList().getPlayer(parentCompany.getCeoId());
                    if (parentCeo != null) {
                        parentCeo.sendSystemMessage(Component.literal(
                                "§cYour subsidiary contract with " + company.getName() +
                                        " has been canceled due to insufficient funds."));
                    }

                    continue;
                }

                // Transfer the funds
                company.withdrawFunds(shareAmount);
                parentCompany.addFunds(shareAmount);

                // Add transactions
                company.addTransaction(null, CompanyTransaction.TransactionType.REVENUE_SHARE, shareAmount,
                        "Weekly revenue share to parent company " + parentCompany.getName());
                parentCompany.addTransaction(null, CompanyTransaction.TransactionType.REVENUE_SHARE, shareAmount,
                        "Weekly revenue share from subsidiary " + company.getName());

                // Add logs
                company.addLog(level, null, CompanyLog.LogType.FINANCIAL,
                        "Paid " + EconomyManager.formatCurrency(shareAmount) + " as revenue share to parent company");
                parentCompany.addLog(level, null, CompanyLog.LogType.FINANCIAL,
                        "Received " + EconomyManager.formatCurrency(shareAmount) + " as revenue share from subsidiary " + company.getName());

                // Notify CEOs if online
                ServerPlayer companyCeo = server.getPlayerList().getPlayer(company.getCeoId());
                if (companyCeo != null) {
                    companyCeo.sendSystemMessage(Component.literal(
                            "§6Your company paid " + EconomyManager.formatCurrency(shareAmount) +
                                    " as revenue share to parent company " + parentCompany.getName() + "."));
                }

                ServerPlayer parentCeo = server.getPlayerList().getPlayer(parentCompany.getCeoId());
                if (parentCeo != null) {
                    parentCeo.sendSystemMessage(Component.literal(
                            "§6Your company received " + EconomyManager.formatCurrency(shareAmount) +
                                    " as revenue share from subsidiary " + company.getName() + "."));
                }
            }
        }

        // Save changes
        manager.setDirty();
    }


}
