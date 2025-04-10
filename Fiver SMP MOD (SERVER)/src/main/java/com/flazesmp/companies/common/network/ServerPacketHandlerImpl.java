// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.common.network;

import com.flazesmp.companies.FlazeSMP;
import com.flazesmp.companies.common.network.packets.CompanyCreatePacket;
import com.flazesmp.companies.data.CompanyLog;
import com.flazesmp.companies.data.CompanyManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// This class is SERVER-SIDE ONLY
public class ServerPacketHandlerImpl {
    // Static initializer to register our handler
    static {
        // Register our handler method using a lambda instead of a method reference
        CompanyCreatePacket.ServerPacketHandlerProxy.handleCompanyCreate =
                (player, companyName, domain, subdomain) -> {
                    handleCompanyCreate(player, companyName, domain, subdomain);
                };
    }

    // Separate method to avoid direct method reference which causes compilation errors
    private static void registerHandler() {
        // Instead of using method reference, we'll use a lambda
        CompanyCreatePacket.ServerPacketHandlerProxy.handleCompanyCreate =
                (player, companyName, domain, subdomain) -> {
                    handleCompanyCreate(player, companyName, domain, subdomain);
                };
    }

    public static void handleCompanyCreate(ServerPlayer player, String companyName, String domain, String subdomain) {
        FlazeSMP.LOGGER.info("Handling company creation: " + companyName + ", " + domain + ", " + subdomain);

        // Create the company
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());

        // Check if player already has a company
        if (manager.getPlayerCompany(player.getUUID()) != null) {
            player.sendSystemMessage(Component.literal("You already belong to a company."));
            return;
        }

        // Create the company
        FlazeSMP.LOGGER.info("Creating company with manager: " + manager);
        var company = manager.createCompany(companyName, player.getUUID());

        if (company != null) {
            // Set domain and subdomain
            company.setDomain(domain);
            company.setSubdomain(subdomain);

            // Log the creation
            company.addLog((ServerLevel) player.level(), player.getUUID(), CompanyLog.LogType.COMPANY_CREATED,
                    "Created company with domain: " + domain + ", subdomain: " + subdomain);

            FlazeSMP.LOGGER.info("Company created successfully: " + company);
            player.sendSystemMessage(Component.literal("§aCompany '" + companyName + "' created successfully!"));
            player.sendSystemMessage(Component.literal("§aDomain: " + domain));
            player.sendSystemMessage(Component.literal("§aSubdomain: " + subdomain));
        } else {
            FlazeSMP.LOGGER.error("Failed to create company: " + companyName);
            player.sendSystemMessage(Component.literal("§cFailed to create company. Please try again."));
        }
    }
}
