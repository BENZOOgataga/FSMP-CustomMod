package com.flazesmp.companies.common.network.packets;

import com.flazesmp.companies.common.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// This class is for BOTH client and server, but this is the CLIENT version
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
            // On client side, we don't need to do anything when receiving this packet
            // This packet is sent from client to server
        });
        context.setPacketHandled(true);
    }

    // This is just a stub for the server-side implementation
    public static class ServerPacketHandlerProxy {
        // Define a functional interface for the handler
        @FunctionalInterface
        public interface CompanyCreateHandler {
            void handle(Object player, String companyName, String domain, String subdomain);
        }

        // Static field to hold the handler implementation - not used on client
        public static CompanyCreateHandler handleCompanyCreate = (player, companyName, domain, subdomain) -> {
            // Client-side stub does nothing
        };
    }
}
