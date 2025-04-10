// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.events;

import com.flazesmp.companies.data.Company;
import com.flazesmp.companies.data.CompanyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class ChatEventHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer)) {
            return;
        }

        ServerPlayer player = event.getPlayer();

        // Get the player's company
        CompanyManager manager = CompanyManager.get((ServerLevel) player.level());
        Company playerCompany = manager.getPlayerCompany(player.getUUID());

        if (playerCompany == null) {
            // Player doesn't have a company, no prefix needed
            return;
        }

        // Get the original message
        Component originalMessage = event.getMessage();

        // Create the company prefix
        Component companyPrefix = Component.literal("[" + playerCompany.getName() + "] ")
                .withStyle(ChatFormatting.WHITE);

        // Create a new message with the company prefix
        Component newMessage = Component.empty().append(companyPrefix).append(originalMessage);

        // Set the new message
        event.setMessage(newMessage);

        // For players in the same company, we'll make the company name green in their view
        for (ServerPlayer recipient : player.getServer().getPlayerList().getPlayers()) {
            if (recipient == player) continue; // Skip the sender

            Company recipientCompany = manager.getPlayerCompany(recipient.getUUID());

            if (recipientCompany != null && recipientCompany.getId() == playerCompany.getId()) {
                // This player is in the same company, send them a message with green company name
                Component greenPrefix = Component.literal("[" + playerCompany.getName() + "] ")
                        .withStyle(ChatFormatting.GREEN);

                // Create a custom message with green company name
                Component customMessage = Component.empty().append(greenPrefix).append(originalMessage);

                // Send the custom message to this company member
                recipient.sendSystemMessage(customMessage);
            }
        }
    }
}