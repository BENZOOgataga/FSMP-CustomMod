// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.client;

import com.flazesmp.companies.FlazeSMP;
import com.flazesmp.companies.client.gui.CompanyCreationScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FlazeSMP.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register client-side setup here
    }

    /**
     * Opens the company creation screen
     */
    public static void openCompanyCreationScreen() {
        // Add debug logging
        System.out.println("Opening CompanyCreationScreen");

        // Make sure we're on the client thread
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new CompanyCreationScreen());
        });
    }
}
