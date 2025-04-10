package com.flazesmp.companies.server;

import com.flazesmp.companies.common.network.NetworkHandler;
import com.flazesmp.companies.common.network.ServerPacketHandlerImpl;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

// This class is SERVER-SIDE ONLY
public class ModInitializer {
    public static void init(FMLCommonSetupEvent event) {
        // Initialize network
        NetworkHandler.init();

        // Force class loading to register handlers
        // This ensures the static initializer in ServerPacketHandlerImpl runs
        try {
            Class.forName(ServerPacketHandlerImpl.class.getName());
        } catch (ClassNotFoundException e) {
            // This shouldn't happen
            e.printStackTrace();
        }
    }
}
