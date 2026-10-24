package com.dedsafio.eclipse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** Estado del eclipse en el cliente (se sincroniza desde el servidor y avanza solo cada tick). */
public final class ClientEclipse {
    private ClientEclipse() {}

    public static boolean active;
    public static float progress;
    public static float prevProgress;
    public static float speed;

    public static void apply(boolean a, float p, float s) {
        active = a;
        progress = p;
        prevProgress = p;
        speed = s;
    }

    public static void reset() {
        apply(false, 0f, 0f);
    }

    public static void tick() {
        prevProgress = progress;
        if (active && speed != 0f) {
            progress = Mth.clamp(progress + speed, 0f, 1f);
        }
    }

    public static float get(float partial) {
        return Mth.lerp(partial, prevProgress, progress);
    }

    /** El eclipse solo se ve en el Overworld. */
    public static boolean isActiveHere() {
        Minecraft mc = Minecraft.getInstance();
        return active && mc.level != null && mc.level.dimension() == Level.OVERWORLD;
    }
}
