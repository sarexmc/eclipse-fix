package com.dedsafio.eclipse;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DedsafioEclipse.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + DedsafioEclipse.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.LINTERNA.get()))
                    .displayItems((params, output) -> output.accept(ModItems.LINTERNA.get()))
                    .build());

    private ModCreativeTabs() {}
}
