// Owned by BENZOOgataga. Not for redistribution or external server use.

// OpenCompanyCreationGuiPacket.java (client side)
package com.flazesmp.companies.common.network.packets;

import com.flazesmp.companies.client.ClientSetup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenCompanyCreationGuiPacket {

    public OpenCompanyCreationGuiPacket() {
    }

    public static void encode(OpenCompanyCreationGuiPacket packet, FriendlyByteBuf buffer) {
        // Nothing to encode
    }

    public static OpenCompanyCreationGuiPacket decode(FriendlyByteBuf buffer) {
        return new OpenCompanyCreationGuiPacket();
    }

    public static void handle(OpenCompanyCreationGuiPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            System.out.println("Received OpenCompanyCreationGuiPacket on client");
            // Make sure we're on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                System.out.println("Executing client-side code to open screen");
                ClientSetup.openCompanyCreationScreen();
            });
        });
        context.setPacketHandled(true);
    }

    // This class is just a stub for the server-side implementation
    public static class ClientGuiHandlerProxy {
        public static void openCompanyCreationScreen() {
            // This is implemented directly in the client-side code
        }
    }
}