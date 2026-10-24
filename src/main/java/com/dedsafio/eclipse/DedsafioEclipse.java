package com.dedsafio.eclipse;

import com.dedsafio.eclipse.entity.HerobrineEntity;
import com.dedsafio.eclipse.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DedsafioEclipse.MOD_ID)
public class DedsafioEclipse {
    public static final String MOD_ID = "dedsafio_eclipse";

    public DedsafioEclipse() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModCreativeTabs.TABS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::attributes);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.HEROBRINE.get(), HerobrineEntity.createAttributes().build());
    }
}
