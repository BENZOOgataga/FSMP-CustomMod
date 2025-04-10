package com.flazesmp.companies.common.network.packets;

import com.flazesmp.companies.client.ClientSetup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Server version
public class OpenCompanyCreationGuiPacket {
    public OpenCompanyCreationGuiPacket() {}

    public static void encode(OpenCompanyCreationGuiPacket packet, FriendlyByteBuf buffer) {
        // Nothing to encode
    }

    public static OpenCompanyCreationGuiPacket decode(FriendlyByteBuf buffer) {
        return new OpenCompanyCreationGuiPacket();
    }

    public static void handle(OpenCompanyCreationGuiPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Server-side doesn't need to handle this packet
        });
        context.setPacketHandled(true);
    }
}