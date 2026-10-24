package com.dedsafio.eclipse.eclipse;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Geometría y colores del eclipse. Compartido por cliente (render) y servidor (quemadura). */
public final class EclipseSky {
    private EclipseSky() {}

    public static final double SUN_ELEV = Math.toRadians(80);
    public static final double SUN_AZ = Math.toRadians(200);
    /** Separación angular luna-sol al empezar y al terminar (0 = la luna tapa el sol por completo y queda el aro de luz). */
    public static final double MOON_START = Math.toRadians(45);
    public static final double MOON_END = 0.0;
    /** Coseno del ángulo dentro del cual mirar al sol te quema. */
    public static final double BURN_COS = Math.cos(Math.toRadians(14));

    public static Vec3 dir(double elev, double az) {
        return new Vec3(Math.cos(elev) * Math.sin(az), Math.sin(elev), -Math.cos(elev) * Math.cos(az));
    }

    public static Vec3 sunDir() {
        return dir(SUN_ELEV, SUN_AZ);
    }

    /** Dirección de la luna: p=0 lejos del sol, p=1 casi encima. */
    public static Vec3 moonDir(float p) {
        Vec3 s = sunDir();
        Vec3 axis = s.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 t = axis.cross(s).normalize();
        if (t.y > 0) t = t.scale(-1); // la luna viene desde abajo
        double th = MOON_START + (MOON_END - MOON_START) * p;
        return s.scale(Math.cos(th)).add(t.scale(Math.sin(th)));
    }

    public static float smooth(float e0, float e1, float x) {
        float t = Mth.clamp((x - e0) / (e1 - e0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float grayAmount(float p) { return smooth(0f, 0.6f, p); }
    public static float darkAmount(float p) { return smooth(0.6f, 0.95f, p); }
    public static float starAmount(float p) { return smooth(0.55f, 0.95f, p); }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2])};
    }

    public static float[] zenithColor(float p) {
        float[] c = mix(new float[]{0.30f, 0.52f, 0.95f}, new float[]{0.40f, 0.41f, 0.43f}, grayAmount(p));
        float k = 1f - 0.98f * darkAmount(p);
        return new float[]{c[0] * k, c[1] * k, c[2] * k};
    }

    public static float[] horizonColor(float p) {
        float[] c = mix(new float[]{0.68f, 0.80f, 0.98f}, new float[]{0.50f, 0.51f, 0.53f}, grayAmount(p));
        float k = 1f - 0.96f * darkAmount(p);
        return new float[]{c[0] * k, c[1] * k, c[2] * k};
    }
}
