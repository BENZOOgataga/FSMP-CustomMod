package com.flazesmp.companies.common.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMarketGuiPacket {

    public OpenMarketGuiPacket() {
        // Empty constructor
    }

    public static void encode(OpenMarketGuiPacket packet, FriendlyByteBuf buffer) {
        // Nothing to encode
    }

    public static OpenMarketGuiPacket decode(FriendlyByteBuf buffer) {
        return new OpenMarketGuiPacket();
    }

    public static void handle(OpenMarketGuiPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Handle on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlerProxy.openMarketScreen());
        });
        context.setPacketHandled(true);
    }

    // This class will be referenced but not implemented on the server
    public static class ClientPacketHandlerProxy {
        public static void openMarketScreen() {
            // This is just a stub - the actual implementation is on the client side
        }
    }
}
