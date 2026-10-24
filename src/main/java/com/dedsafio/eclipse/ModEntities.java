package com.dedsafio.eclipse;

import com.dedsafio.eclipse.entity.HerobrineEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DedsafioEclipse.MOD_ID);

    public static final RegistryObject<EntityType<HerobrineEntity>> HEROBRINE = ENTITIES.register("herobrine",
            () -> EntityType.Builder.of(HerobrineEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("herobrine"));

    private ModEntities() {}
}
