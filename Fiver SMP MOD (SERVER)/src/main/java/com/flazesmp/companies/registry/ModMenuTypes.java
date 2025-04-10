// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is prohibited.

package com.flazesmp.companies.registry;

import com.flazesmp.companies.FlazeSMP;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, FlazeSMP.MOD_ID);

}
