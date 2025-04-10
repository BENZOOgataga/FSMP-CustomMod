// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.events;

import com.flazesmp.companies.gui.PermissionsGui;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GuiEventHandler {

    // We'll handle clicks through the container implementation instead of events
    // since ContainerClickEvent isn't available in Forge 1.20.1

    @SubscribeEvent
    public static void onInventoryClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            PermissionsGui.onInventoryClose(player);
        }
    }
}
