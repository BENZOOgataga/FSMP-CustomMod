package com.flazesmp.companies.economy;

import com.flazesmp.companies.FlazeSMP;
import com.flazesmp.companies.data.Company;
import com.flazesmp.companies.data.CompanyManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class EconomyManager extends SavedData {
    private static final String DATA_NAME = FlazeSMP.MOD_ID + "_economy";
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    private final Map<UUID, Double> playerBalances = new HashMap<>();
    private final Map<Integer, Double> companyBalances = new HashMap<>();
    private final ServerLevel level;

    public EconomyManager(ServerLevel level) {
        this.level = level;
    }

    // Update the get method
    public static EconomyManager get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(tag -> load(tag, level), () -> new EconomyManager(level), DATA_NAME);
    }

    public double getPlayerBalance(UUID playerId) {
        return playerBalances.getOrDefault(playerId, 0.0);
    }

    public double getCompanyBalance(int companyId) {
        return companyBalances.getOrDefault(companyId, 0.0);
    }

    public boolean setPlayerBalance(UUID playerId, double amount) {
        if (amount < 0) {
            return false;
        }

        playerBalances.put(playerId, amount);
        setDirty();
        return true;
    }

    public boolean setCompanyBalance(int companyId, double amount) {
        if (amount < 0) {
            return false;
        }

        companyBalances.put(companyId, amount);
        setDirty();
        return true;
    }

    public boolean addToPlayerBalance(UUID playerId, double amount) {
        double currentBalance = getPlayerBalance(playerId);
        return setPlayerBalance(playerId, currentBalance + amount);
    }

    public boolean addToCompanyBalance(int companyId, double amount) {
        double currentBalance = getCompanyBalance(companyId);
        return setCompanyBalance(companyId, currentBalance + amount);
    }

    public boolean removeFromPlayerBalance(UUID playerId, double amount) {
        double currentBalance = getPlayerBalance(playerId);
        if (currentBalance < amount) {
            return false;
        }

        return setPlayerBalance(playerId, currentBalance - amount);
    }

    public boolean removeFromCompanyBalance(int companyId, double amount) {
        double currentBalance = getCompanyBalance(companyId);
        if (currentBalance < amount) {
            return false;
        }

        return setCompanyBalance(companyId, currentBalance - amount);
    }

    public boolean transferToPlayer(UUID fromPlayerId, UUID toPlayerId, double amount) {
        if (amount <= 0 || fromPlayerId.equals(toPlayerId)) {
            return false;
        }

        if (removeFromPlayerBalance(fromPlayerId, amount)) {
            addToPlayerBalance(toPlayerId, amount);
            return true;
        }

        return false;
    }

    /**
     * Transfer money from a company to a player
     */
    public boolean transferFromCompany(int companyId, UUID playerId, double amount) {
        // Get the company
        CompanyManager companyManager = CompanyManager.get(this.level);
        Company company = companyManager.getCompany(companyId);
        if (company == null || company.getFunds() < amount) {
            return false;
        }

        // Remove from company
        if (!company.withdrawFunds(amount)) {
            return false;
        }
        companyManager.setDirty();

        // Add to player
        addToPlayerBalance(playerId, amount);

        return true;
    }

    // Update the transferToCompany method
    public boolean transferToCompany(UUID playerId, int companyId, double amount) {
        // Check if player has enough money
        if (getPlayerBalance(playerId) < amount) {
            return false;
        }

        // Get the company
        CompanyManager companyManager = CompanyManager.get(this.level);
        Company company = companyManager.getCompany(companyId);
        if (company == null) {
            return false;
        }

        // Remove from player
        removeFromPlayerBalance(playerId, amount);

        // Add to company
        company.addFunds(amount);
        companyManager.setDirty();

        return true;
    }

    public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        return playerBalances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount) + " FC$";
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save player balances
        ListTag playerBalancesTag = new ListTag();

        for (Map.Entry<UUID, Double> entry : playerBalances.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putString("UUID", entry.getKey().toString());
            playerTag.putDouble("Balance", entry.getValue());
            playerBalancesTag.add(playerTag);
        }

        tag.put("PlayerBalances", playerBalancesTag);

        // Save company balances
        ListTag companyBalancesTag = new ListTag();

        for (Map.Entry<Integer, Double> entry : companyBalances.entrySet()) {
            CompoundTag companyTag = new CompoundTag();
            companyTag.putInt("ID", entry.getKey());
            companyTag.putDouble("Balance", entry.getValue());
            companyBalancesTag.add(companyTag);
        }

        tag.put("CompanyBalances", companyBalancesTag);

        return tag;
    }

    public static EconomyManager load(CompoundTag tag, ServerLevel level) {
        EconomyManager manager = new EconomyManager(level);

        // Load player balances
        if (tag.contains("PlayerBalances")) {
            ListTag playerBalancesTag = tag.getList("PlayerBalances", Tag.TAG_COMPOUND);

            for (int i = 0; i < playerBalancesTag.size(); i++) {
                CompoundTag playerTag = playerBalancesTag.getCompound(i);
                UUID playerId = UUID.fromString(playerTag.getString("UUID"));
                double balance = playerTag.getDouble("Balance");

                manager.playerBalances.put(playerId, balance);
            }
        }

        // Load company balances
        if (tag.contains("CompanyBalances")) {
            ListTag companyBalancesTag = tag.getList("CompanyBalances", Tag.TAG_COMPOUND);

            for (int i = 0; i < companyBalancesTag.size(); i++) {
                CompoundTag companyTag = companyBalancesTag.getCompound(i);
                int companyId = companyTag.getInt("ID");
                double balance = companyTag.getDouble("Balance");

                manager.companyBalances.put(companyId, balance);
            }
        }

        return manager;
    }

}
