package com.flazesmp.companies.common.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestMarketItemsPacket {
    private final String subdomain;

    public RequestMarketItemsPacket(String subdomain) {
        this.subdomain = subdomain;
    }

    public static void encode(RequestMarketItemsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.subdomain);
    }

    public static RequestMarketItemsPacket decode(FriendlyByteBuf buffer) {
        return new RequestMarketItemsPacket(buffer.readUtf());
    }

    public static void handle(RequestMarketItemsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerPacketHandlerProxy.handleRequestMarketItems(player, packet.subdomain);
            }
        });
        context.setPacketHandled(true);
    }

    // This is just a stub for the client-side implementation
    @FunctionalInterface
    public interface RequestMarketItemsHandler {
        void handle(ServerPlayer player, String subdomain);
    }

    public static class ServerPacketHandlerProxy {
        public static void handleRequestMarketItems(ServerPlayer player, String subdomain) {
            // Server-side implementation will be set elsewhere
        }
    }
}
