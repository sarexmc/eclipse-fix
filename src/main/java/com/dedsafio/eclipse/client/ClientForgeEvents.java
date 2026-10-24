package com.dedsafio.eclipse.client;

import com.dedsafio.eclipse.DedsafioEclipse;
import com.dedsafio.eclipse.eclipse.EclipseSky;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DedsafioEclipse.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ClientEclipse.tick();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientEclipse.reset();
    }

    /** El color de la niebla sigue al horizonte del cielo para que no se vea un corte. */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!ClientEclipse.isActiveHere()) return;
        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;
        float p = ClientEclipse.get((float) event.getPartialTick());
        float[] c = EclipseSky.horizonColor(p);
        event.setRed(c[0]);
        event.setGreen(c[1]);
        event.setBlue(c[2]);
    }
}
