// This class is for CLIENT side
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
            if (domain.name.equals(domainName)) {
                return domain.subdomains;
            }
        }
        return new ArrayList<>();
    }

    public static class DomainData {
        public String name;
        public ItemStack icon = new ItemStack(Items.DIAMOND);
        public String description;
        public List<SubdomainData> subdomains = new ArrayList<>();

        public String getName() {
            return name;
        }

        public ItemStack getIcon() {
            return icon;
        }

        public String getDescription() {
            return description;
        }

        public List<SubdomainData> getSubdomains() {
            return subdomains;
        }
    }

    public static class SubdomainData {
        public String name;
        public ItemStack icon = new ItemStack(Items.DIAMOND);
        public String buff;
        public boolean locked;

        public String getName() {
            return name;
        }

        public ItemStack getIcon() {
            return icon;
        }

        public String getBuff() {
            return buff;
        }

        public boolean isLocked() {
            return locked;
        }
    }
}