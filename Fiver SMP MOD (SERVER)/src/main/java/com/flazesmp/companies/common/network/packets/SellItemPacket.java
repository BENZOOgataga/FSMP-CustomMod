package com.flazesmp.companies.common.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SellItemPacket {
    private final String itemId;
    private final int quantity;

    public SellItemPacket(String itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public static void encode(SellItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.itemId);
        buffer.writeInt(packet.quantity);
    }

    public static SellItemPacket decode(FriendlyByteBuf buffer) {
        return new SellItemPacket(buffer.readUtf(), buffer.readInt());
    }

    public static void handle(SellItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerPacketHandlerProxy.handleSellItem(player, packet.itemId, packet.quantity);
            }
        });
        context.setPacketHandled(true);
    }

    @FunctionalInterface
    public interface SellItemHandler {
        void handle(ServerPlayer player, String itemId, int quantity);
    }

    public static class ServerPacketHandlerProxy {
        public static void handleSellItem(ServerPlayer player, String itemId, int quantity) {
            // Server-side implementation will be set elsewhere
        }
    }
}