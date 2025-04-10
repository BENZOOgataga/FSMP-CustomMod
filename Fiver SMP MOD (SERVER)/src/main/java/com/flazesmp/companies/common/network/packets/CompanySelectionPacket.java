package com.flazesmp.companies.common.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CompanySelectionPacket {
    private final int slotId;
    private final String selectionType; // "domain", "subdomain", or "back"

    public CompanySelectionPacket(int slotId, String selectionType) {
        this.slotId = slotId;
        this.selectionType = selectionType;
    }

    public static void encode(CompanySelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.slotId);
        buffer.writeUtf(packet.selectionType);
    }

    public static CompanySelectionPacket decode(FriendlyByteBuf buffer) {
        return new CompanySelectionPacket(buffer.readInt(), buffer.readUtf());
    }

    public static void handle(CompanySelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // On the server side, we don't need to handle this packet in detail
                // Just log that we received it
                System.out.println("Received selection packet: slot=" + packet.slotId + ", type=" + packet.selectionType);

                // If you need to maintain compatibility with existing code, add stub implementations
                if (packet.selectionType.equals("back")) {
                    // Handle back button logic if needed
                    handleBackButton(player, packet.slotId);
                }
            }
        });
        context.setPacketHandled(true);
    }

    // Stub implementation to handle back button logic
    private static void handleBackButton(ServerPlayer player, int slotId) {
        // This is a simplified version that doesn't rely on missing methods
        if (slotId == 36) { // Back button slot ID
            // You can implement simplified logic here if needed
            System.out.println("Back button pressed by player: " + player.getName().getString());
        }
    }
}
