// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.gui;

import com.flazesmp.companies.data.Company;
import com.flazesmp.companies.data.CompanyManager;
import com.flazesmp.companies.data.CompanyPermission;
import com.flazesmp.companies.data.CompanyRole;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PermissionsGui {
    private static final Map<UUID, Company> openCompanyMenus = new HashMap<>();
    private static final Map<Integer, Integer> slotToPermissionIndex = new HashMap<>();

    public static void openCompanyMembersMenu(ServerPlayer player, Company company) {
        // Store the company for this player's session
        openCompanyMenus.put(player.getUUID(), company);

        // Open the container menu
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Company Members - " + company.getName());
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                return new CompanyContainer(windowId, playerInventory, company);
            }
        });
    }

    public static void setupMembersMenu(ServerPlayer player, CompanyContainer container, Company company) {
        MinecraftServer server = player.getServer();
        int slot = 0;

        // Add member heads to the menu
        for (Map.Entry<UUID, CompanyRole> entry : company.getMembers().entrySet()) {
            UUID memberId = entry.getKey();
            CompanyRole role = entry.getValue();

            // Get player name from UUID
            String playerName = server.getProfileCache()
                    .get(memberId)
                    .map(profile -> profile.getName())
                    .orElse("Unknown Player");

            // Create head item with role information
            ItemStack headItem = createHeadItem(playerName,
                    Component.literal(playerName + " - " + role.name()));

            // Add lore with instructions
            headItem.getOrCreateTag().putString("Lore", "Click to edit permissions");

            // Add to menu
            container.getSlot(slot).set(headItem);
            slot++;
        }

        // Add back button
        ItemStack backButton = new ItemStack(Items.BARRIER);
        backButton.setHoverName(Component.literal("§cClose"));
        container.getSlot(53).set(backButton);
    }

    public static void handleMembersMenuClick(ServerPlayer player, int slot, Company company) {
        // Check if it's the back button
        if (slot == 53) {
            player.closeContainer();
            return;
        }

        // Check if it's a valid member slot
        if (slot >= 0 && slot < company.getMembers().size()) {
            // Get the member UUID at this slot
            UUID memberId = (UUID) company.getMembers().keySet().toArray()[slot];

            // Skip if it's the CEO (can't edit CEO permissions)
            if (memberId.equals(company.getCeoId())) {
                player.sendSystemMessage(Component.literal("The CEO has all permissions by default."));
                return;
            }

            // Open permissions menu for this member
            openMemberPermissionsMenu(player, company, memberId);
        }
    }

    public static void openMemberPermissionsMenu(ServerPlayer player, Company company, UUID memberId) {
        // Get player name
        String playerName = player.getServer().getProfileCache()
                .get(memberId)
                .map(profile -> profile.getName())
                .orElse("Unknown Player");

        // Open the container menu
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Permissions - " + playerName);
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                return new CompanyContainer(windowId, playerInventory, company, memberId);
            }
        });
    }

    public static void setupPermissionsMenu(ServerPlayer player, CompanyContainer container, Company company, UUID memberId) {
        // Clear previous mappings
        slotToPermissionIndex.clear();

        // Get player name
        String playerName = player.getServer().getProfileCache()
                .get(memberId)
                .map(profile -> profile.getName())
                .orElse("Unknown Player");

        // Add member head at top
        ItemStack headItem = createHeadItem(playerName,
                Component.literal(playerName + " - " + company.getMemberRole(memberId).name()));
        container.getSlot(4).set(headItem);

        // Add permission items
        int slot = 19; // Start in second row
        int permissionIndex = 0;

        for (CompanyPermission permission : CompanyPermission.values()) {
            boolean hasPermission = company.hasPermission(memberId, permission);

            // Skip middle column
            if (slot % 9 == 4) {
                slot++;
            }

            // Map this slot to this permission index
            slotToPermissionIndex.put(slot, permissionIndex);

            ItemStack permItem;
            if (hasPermission) {
                permItem = new ItemStack(Items.LIME_WOOL);
                permItem.setHoverName(Component.literal("§a" + formatPermissionName(permission)));
                permItem.getOrCreateTag().putString("Lore", "§aENABLED - Click to disable");
            } else {
                permItem = new ItemStack(Items.RED_WOOL);
                permItem.setHoverName(Component.literal("§c" + formatPermissionName(permission)));
                permItem.getOrCreateTag().putString("Lore", "§cDISABLED - Click to enable");
            }

            container.getSlot(slot).set(permItem);
            slot++;
            permissionIndex++;
        }

        // Add back button
        ItemStack backButton = new ItemStack(Items.ARROW);
        backButton.setHoverName(Component.literal("§eBack to Members"));
        container.getSlot(49).set(backButton);
    }

    public static void handlePermissionsMenuClick(ServerPlayer player, int slot, Company company, UUID memberId) {
        // Check if it's the back button
        if (slot == 49) {
            openCompanyMembersMenu(player, company);
            return;
        }

        // Get the permission index directly from our mapping
        Integer permissionIndex = slotToPermissionIndex.get(slot);

        if (permissionIndex != null && permissionIndex < CompanyPermission.values().length) {
            CompanyPermission permission = CompanyPermission.values()[permissionIndex];
            boolean currentValue = company.hasPermission(memberId, permission);

            // Toggle permission
            company.setPermission(memberId, permission, !currentValue);

            // Save changes
            CompanyManager.get((ServerLevel) player.level()).setDirty();

            // Refresh menu
            openMemberPermissionsMenu(player, company, memberId);

            // Notify player
            player.sendSystemMessage(Component.literal(
                    "Permission " + formatPermissionName(permission) + " for player " +
                            player.getServer().getProfileCache().get(memberId).map(profile -> profile.getName()).orElse("Unknown Player") +
                            " is now " + (!currentValue ? "§aENABLED" : "§cDISABLED")));
        }
    }

    public static void onInventoryClose(ServerPlayer player) {
        openCompanyMenus.remove(player.getUUID());
    }

    public static boolean isCustomGui(ServerPlayer player) {
        return openCompanyMenus.containsKey(player.getUUID());
    }

    private static String formatPermissionName(CompanyPermission permission) {
        String name = permission.name();
        String[] words = name.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            formatted.append(word.charAt(0)).append(word.substring(1).toLowerCase()).append(" ");
        }

        return formatted.toString().trim();
    }

    private static ItemStack createHeadItem(String playerName, Component displayName) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.setHoverName(displayName);

        // Set the player's skin to the head
        head.getOrCreateTag().putString("SkullOwner", playerName);

        return head;
    }
}
