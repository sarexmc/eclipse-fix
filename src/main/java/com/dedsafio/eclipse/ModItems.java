package com.dedsafio.eclipse;

import com.dedsafio.eclipse.item.LinternaItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DedsafioEclipse.MOD_ID);

    public static final RegistryObject<Item> LINTERNA = ITEMS.register("linterna",
            () -> new LinternaItem(new Item.Properties().durability(LinternaItem.MAX_DURABILITY)));

    private ModItems() {}
}
