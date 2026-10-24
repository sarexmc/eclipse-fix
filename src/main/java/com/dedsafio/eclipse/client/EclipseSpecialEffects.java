package com.dedsafio.eclipse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Copia del cielo del Overworld, pero dibuja nuestro cielo de eclipse mientras hay eclipse. */
public class EclipseSpecialEffects extends DimensionSpecialEffects {
    public EclipseSpecialEffects() {
        super(192.0F, true, DimensionSpecialEffects.SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        if (!ClientEclipse.isActiveHere()) return false;           // cielo vanilla
        if (isFoggy || camera.getFluidInCamera() != FogType.NONE) return false;
        SkyRenderer.renderDome(poseStack, ClientEclipse.get(partialTick));
        return true;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                                double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        return ClientEclipse.isActiveHere(); // sin nubes durante el eclipse
    }
}
