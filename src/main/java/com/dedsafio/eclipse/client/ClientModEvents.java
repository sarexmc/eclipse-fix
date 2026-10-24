package com.dedsafio.eclipse.client;

import com.dedsafio.eclipse.DedsafioEclipse;
import com.dedsafio.eclipse.ModEntities;
import com.dedsafio.eclipse.ModItems;
import com.dedsafio.eclipse.item.LinternaItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DedsafioEclipse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.LINTERNA.get(),
                new ResourceLocation(DedsafioEclipse.MOD_ID, "on"),
                (stack, level, entity, seed) -> LinternaItem.isOn(stack) ? 1.0F : 0.0F));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HEROBRINE.get(), HerobrineRenderer::new);
    }

    @SubscribeEvent
    public static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(new ResourceLocation(DedsafioEclipse.MOD_ID, "overworld_eclipse"), new EclipseSpecialEffects());
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("linterna_arranque", LinternaArranqueOverlay::render);
    }
}
