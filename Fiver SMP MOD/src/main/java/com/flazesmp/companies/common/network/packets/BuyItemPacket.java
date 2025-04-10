package com.flazesmp.companies.common.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BuyItemPacket {
    private final String itemId;
    private final int quantity;
    private final boolean useCompanyFunds;

    public BuyItemPacket(String itemId, int quantity, boolean useCompanyFunds) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.useCompanyFunds = useCompanyFunds;
    }

    public static void encode(BuyItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.itemId);
        buffer.writeInt(packet.quantity);
        buffer.writeBoolean(packet.useCompanyFunds);
    }

    public static BuyItemPacket decode(FriendlyByteBuf buffer) {
        return new BuyItemPacket(buffer.readUtf(), buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(BuyItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerPacketHandlerProxy.handleBuyItem(player, packet.itemId, packet.quantity, packet.useCompanyFunds);
            }
        });
        context.setPacketHandled(true);
    }

    @FunctionalInterface
    public interface BuyItemHandler {
        void handle(ServerPlayer player, String itemId, int quantity, boolean useCompanyFunds);
    }

    public static class ServerPacketHandlerProxy {
        public static void handleBuyItem(ServerPlayer player, String itemId, int quantity, boolean useCompanyFunds) {
            // Server-side implementation will be set elsewhere
        }
    }
}