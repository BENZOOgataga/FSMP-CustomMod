// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.gui;

import com.flazesmp.companies.data.Company;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class CompanyContainer extends AbstractContainerMenu {
    private final Company company;
    private final UUID targetMemberId;
    private final boolean isPermissionsMenu;

    // Constructor for members menu
    public CompanyContainer(int windowId, Inventory playerInventory, Company company) {
        super(MenuType.GENERIC_9x6, windowId);
        this.company = company;
        this.targetMemberId = null;
        this.isPermissionsMenu = false;

        // Add chest slots (6 rows of 9)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new GhostSlot(col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory slots (these are just for show, they won't be interactive)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // Add player hotbar slots (these are just for show, they won't be interactive)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Setup the members menu
        PermissionsGui.setupMembersMenu((ServerPlayer) playerInventory.player, this, company);
    }

    // Constructor for permissions menu
    public CompanyContainer(int windowId, Inventory playerInventory, Company company, UUID targetMemberId) {
        super(MenuType.GENERIC_9x6, windowId);
        this.company = company;
        this.targetMemberId = targetMemberId;
        this.isPermissionsMenu = true;

        // Add chest slots (6 rows of 9)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new GhostSlot(col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory slots (these are just for show, they won't be interactive)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // Add player hotbar slots (these are just for show, they won't be interactive)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Setup the permissions menu
        PermissionsGui.setupPermissionsMenu((ServerPlayer) playerInventory.player, this, company, targetMemberId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Prevent item movement in our GUI
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (player instanceof ServerPlayer) {
            // Handle our custom GUI clicks
            if (slotId >= 0 && slotId < 54) {
                if (isPermissionsMenu) {
                    PermissionsGui.handlePermissionsMenuClick((ServerPlayer) player, slotId, company, targetMemberId);
                } else {
                    PermissionsGui.handleMembersMenuClick((ServerPlayer) player, slotId, company);
                }
            }
        }

        // Don't call super.clicked() to prevent normal container behavior
    }

    public Company getCompany() {
        return company;
    }

    public UUID getTargetMemberId() {
        return targetMemberId;
    }

    public boolean isPermissionsMenu() {
        return isPermissionsMenu;
    }

    // Custom ghost slot that doesn't allow items to be taken out or put in
    private static class GhostSlot extends Slot {
        public GhostSlot(int index, int x, int y) {
            super(new GhostInventory(), index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
