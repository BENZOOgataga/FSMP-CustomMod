package com.flazesmp.companies.common.network;

import com.flazesmp.companies.FlazeSMP;
import com.flazesmp.companies.common.network.packets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FlazeSMP.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            FlazeSMP.LOGGER.warn("NetworkHandler already initialized! Skipping duplicate initialization.");
            return;
        }

        FlazeSMP.LOGGER.info("CLIENT: Initializing network handler");

        FlazeSMP.LOGGER.info("CLIENT: Registering packet 0: OpenCompanyCreationGuiPacket");
        CHANNEL.registerMessage(packetId++, OpenCompanyCreationGuiPacket.class,
                OpenCompanyCreationGuiPacket::encode,
                OpenCompanyCreationGuiPacket::decode,
                OpenCompanyCreationGuiPacket::handle);

        FlazeSMP.LOGGER.info("CLIENT: Registering packet 1: DomainDataPacket");
        CHANNEL.registerMessage(packetId++, DomainDataPacket.class,
                DomainDataPacket::encode,
                DomainDataPacket::decode,
                DomainDataPacket::handle);

        FlazeSMP.LOGGER.info("CLIENT: Registering packet 2: CompanyCreatePacket");
        CHANNEL.registerMessage(packetId++, CompanyCreatePacket.class,
                CompanyCreatePacket::encode,
                CompanyCreatePacket::decode,
                CompanyCreatePacket::handle);

        FlazeSMP.LOGGER.info("CLIENT: Network initialization complete, registered " + packetId + " packets");
        initialized = true;
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        // Client-side doesn't need this method, but it's here for compatibility
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
