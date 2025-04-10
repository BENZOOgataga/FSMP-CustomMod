// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ClientDomainCache {
    private static List<DomainData> domains = new ArrayList<>();

    public static void setDomains(List<DomainData> newDomains) {
        domains = newDomains;
    }

    public static List<DomainData> getDomains() {
        return domains;
    }

    public static List<SubdomainData> getSubdomains(String domainName) {
        for (DomainData domain : domains) {
            if (domain.getName().equals(domainName)) {
                return domain.getSubdomains();
            }
        }
        return new ArrayList<>();
    }

    // Data classes with proper getters
    public static class DomainData {
        public String name;
        public ItemStack icon;
        public String description;
        public List<SubdomainData> subdomains = new ArrayList<>();

        public String getName() { return name; }
        public ItemStack getIcon() { return icon; }
        public String getDescription() { return description; }
        public List<SubdomainData> getSubdomains() { return subdomains; }
    }

    public static class SubdomainData {
        public String name;
        public ItemStack icon;
        public String buff;
        public boolean locked;
        public boolean showBuff = true; // Added showBuff field with default value

        public String getName() { return name; }
        public ItemStack getIcon() { return icon; }
        public String getBuff() { return buff; }
        public boolean isLocked() { return locked; }
        public boolean isShowBuff() { return showBuff; } // Added getter for showBuff
    }
}
