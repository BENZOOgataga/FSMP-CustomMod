// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

public class GuiUtils {
    public static final int DOUBLE_CHEST_SIZE = 54;

    public static ItemStack createHeadItem(String playerName, Component displayName) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.setHoverName(displayName);

        // Set the player's skin to the head
        head.getOrCreateTag().putString("SkullOwner", playerName);

        return head;
    }

    public static void openDoubleChestGui(ServerPlayer player, Component title, Consumer<ChestMenu> menuSetup) {
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                ChestMenu menu = ChestMenu.sixRows(windowId, playerInventory);
                menuSetup.accept(menu);
                return menu;
            }
        });
    }
}
