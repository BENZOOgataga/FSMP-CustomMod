package com.flazesmp.companies.common.network.packets;

import com.flazesmp.companies.FlazeSMP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// This class is for BOTH client and server
public class CompanyCreatePacket {
    private final String companyName;
    private final String domain;
    private final String subdomain;

    public CompanyCreatePacket(String companyName, String domain, String subdomain) {
        this.companyName = companyName;
        this.domain = domain;
        this.subdomain = subdomain;
    }

    public static void encode(CompanyCreatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.companyName);
        buffer.writeUtf(packet.domain);
        buffer.writeUtf(packet.subdomain);
    }

    public static CompanyCreatePacket decode(FriendlyByteBuf buffer) {
        String companyName = buffer.readUtf();
        String domain = buffer.readUtf();
        String subdomain = buffer.readUtf();
        return new CompanyCreatePacket(companyName, domain, subdomain);
    }

    public static void handle(CompanyCreatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // This will only run on the server
            ServerPlayer player = context.getSender();
            if (player != null) {
                FlazeSMP.LOGGER.info("Received CompanyCreatePacket from " + player.getName().getString());

                // Get the company name, domain, and subdomain from the packet
                String companyName = packet.companyName;
                String domain = packet.domain;
                String subdomain = packet.subdomain;

                // Call the server-side handler
                ServerPacketHandlerProxy.handleCompanyCreate.handle(player, companyName, domain, subdomain);
            }
        });
        context.setPacketHandled(true);
    }

    // This is just a static interface that will be implemented on the server side
    public static class ServerPacketHandlerProxy {
        public static void handleCompanyCreate(ServerPlayer player, String companyName, String domain, String subdomain) {
        }

        // Define a functional interface for the handler
        @FunctionalInterface
        public interface CompanyCreateHandler {
            void handle(ServerPlayer player, String companyName, String domain, String subdomain);
        }

        // Static field to hold the handler implementation
        public static CompanyCreateHandler handleCompanyCreate = (player, companyName, domain, subdomain) -> {
            // Default implementation does nothing
            // Will be replaced by server implementation
        };
    }
}
