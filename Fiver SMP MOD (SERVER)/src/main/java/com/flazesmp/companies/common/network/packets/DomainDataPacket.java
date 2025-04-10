package com.flazesmp.companies.common.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DomainDataPacket {
    private final List<DomainData> domains;

    public DomainDataPacket(List<DomainData> domains) {
        this.domains = domains;
    }

    public static void encode(DomainDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.domains.size());

        for (DomainData domain : packet.domains) {
            buffer.writeUtf(domain.name);
            buffer.writeUtf(domain.icon);
            buffer.writeUtf(domain.description);

            buffer.writeInt(domain.subdomains.size());
            for (SubdomainData subdomain : domain.subdomains) {
                buffer.writeUtf(subdomain.name);
                buffer.writeUtf(subdomain.icon);
                buffer.writeUtf(subdomain.buff);
                buffer.writeBoolean(subdomain.locked);
                buffer.writeBoolean(subdomain.showBuff); // Add showBuff to the packet
            }
        }
    }

    public static DomainDataPacket decode(FriendlyByteBuf buffer) {
        int domainCount = buffer.readInt();
        List<DomainData> domains = new ArrayList<>();

        for (int i = 0; i < domainCount; i++) {
            DomainData domain = new DomainData();
            domain.name = buffer.readUtf();
            domain.icon = buffer.readUtf();
            domain.description = buffer.readUtf();

            domain.subdomains = new ArrayList<>();
            int subdomainCount = buffer.readInt();
            for (int j = 0; j < subdomainCount; j++) {
                SubdomainData subdomain = new SubdomainData();
                subdomain.name = buffer.readUtf();
                subdomain.icon = buffer.readUtf();
                subdomain.buff = buffer.readUtf();
                subdomain.locked = buffer.readBoolean();
                subdomain.showBuff = buffer.readBoolean(); // Read showBuff from the packet
                domain.subdomains.add(subdomain);
            }

            domains.add(domain);
        }

        return new DomainDataPacket(domains);
    }

    public static void handle(DomainDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Use DistExecutor to safely handle side-specific code
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientPacketHandlerProxy.handleDomainData(packet.domains));
        });
        context.setPacketHandled(true);
    }

    // This class will be referenced but not implemented on the server
    public static class ClientPacketHandlerProxy {
        public static void handleDomainData(List<DomainData> domains) {
            // This is just a stub - the actual implementation is on the client side
            // This method will never be called on the server
        }
    }

    // Data classes that don't reference client/server specific code
    public static class DomainData {
        public String name;
        public String icon;
        public String description;
        public List<SubdomainData> subdomains = new ArrayList<>();
    }

    public static class SubdomainData {
        public String name;
        public String icon;
        public String buff;
        public boolean locked;
        public boolean showBuff = true; // Add showBuff field with default value
    }
}
