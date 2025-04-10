package com.flazesmp.companies.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class GhostInventory implements Container {
    private final Map<Integer, ItemStack> items = new HashMap<>();

    @Override
    public int getContainerSize() {
        return 54; // Double chest size
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        // This is a ghost inventory, so we don't actually remove items
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        // This is a ghost inventory, so we don't actually remove items
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.put(slot, stack);
    }

    @Override
    public void setChanged() {
        // No need to mark as changed since this is a ghost inventory
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
