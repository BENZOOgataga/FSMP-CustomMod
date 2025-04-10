package com.flazesmp.companies.common.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MarketItemsPacket {
    private final String subdomain;
    private final List<MarketItemData> items;

    public MarketItemsPacket(String subdomain, List<MarketItemData> items) {
        this.subdomain = subdomain;
        this.items = items;
    }

    public static void encode(MarketItemsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.subdomain);
        buffer.writeInt(packet.items.size());

        for (MarketItemData item : packet.items) {
            buffer.writeUtf(item.itemId);
            buffer.writeUtf(item.displayName);
            buffer.writeDouble(item.basePrice);
            buffer.writeDouble(item.sellPrice);
            buffer.writeDouble(item.buyPrice);
            buffer.writeDouble(item.priceFluctuation);
            buffer.writeBoolean(item.disabled);
        }
    }

    public static MarketItemsPacket decode(FriendlyByteBuf buffer) {
        String subdomain = buffer.readUtf();
        int itemCount = buffer.readInt();
        List<MarketItemData> items = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            String itemId = buffer.readUtf();
            String displayName = buffer.readUtf();
            double basePrice = buffer.readDouble();
            double sellPrice = buffer.readDouble();
            double buyPrice = buffer.readDouble();
            double priceFluctuation = buffer.readDouble();
            boolean disabled = buffer.readBoolean();

            items.add(new MarketItemData(itemId, displayName, basePrice, sellPrice, buyPrice, priceFluctuation, disabled));
        }

        return new MarketItemsPacket(subdomain, items);
    }

    public static void handle(MarketItemsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientPacketHandlerProxy.handleMarketItems(packet.subdomain, packet.items));
        });
        context.setPacketHandled(true);
    }

    // This class will be referenced but not implemented on the server
    public static class ClientPacketHandlerProxy {
        public static void handleMarketItems(String subdomain, List<MarketItemData> items) {
            // This is just a stub - the actual implementation is on the client side
        }
    }

    // Data class for market items
    public static class MarketItemData {
        public final String itemId;
        public final String displayName;
        public final double basePrice;
        public final double sellPrice;
        public final double buyPrice;
        public final double priceFluctuation;
        public final boolean disabled;

        public MarketItemData(String itemId, String displayName, double basePrice,
                              double sellPrice, double buyPrice, double priceFluctuation,
                              boolean disabled) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.basePrice = basePrice;
            this.sellPrice = sellPrice;
            this.buyPrice = buyPrice;
            this.priceFluctuation = priceFluctuation;
            this.disabled = disabled;
        }
    }
}
