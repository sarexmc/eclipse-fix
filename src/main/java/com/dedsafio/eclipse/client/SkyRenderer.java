package com.dedsafio.eclipse.client;

import com.dedsafio.eclipse.eclipse.EclipseSky;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Cielo del eclipse, todo con geometría (sin texturas, nítido a cualquier resolución).
 * - renderDome: fondo degradado (azul -> gris -> negro). Sin estrellas ni nubes.
 * - renderBodies: círculo negro (luna) y aro de luz (sol), FIJOS en un punto del cielo del mundo.
 *   Se dibuja DESPUÉS de la capa de oscuridad para que el aro siga brillando aunque todo lo demás esté negro.
 *
 * Los cuerpos celestes se pasan a espacio de vista con los vectores de la cámara (izquierda/arriba/frente),
 * sin depender de ninguna matriz de modelview, así quedan anclados al mundo pase lo que pase.
 */
public final class SkyRenderer {
    private SkyRenderer() {}

    private static final int SEG = 48;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Matrix4f IDENTITY = new Matrix4f();

    /** Base de la cámara: convierte direcciones del mundo a espacio de vista (x derecha, y arriba, -z al frente). */
    private static final class View {
        final Vector3f left, up, look;
        View(Camera cam) {
            left = cam.getLeftVector();
            up = cam.getUpVector();
            look = cam.getLookVector();
        }
        Vec3 toView(Vec3 w) {
            return new Vec3(-(w.x * left.x() + w.y * left.y() + w.z * left.z()),
                    w.x * up.x() + w.y * up.y() + w.z * up.z(),
                    -(w.x * look.x() + w.y * look.y() + w.z * look.z()));
        }
    }

    // ---------- fondo ----------

    public static void renderDome(PoseStack ps, float p) {
        Matrix4f m = ps.last().pose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float[] zen = EclipseSky.zenithColor(p);
        float[] hor = EclipseSky.horizonColor(p);
        double[] el = {-90, -5, 0, 10, 25, 45, 70, 90};
        float[] t = {0f, 0f, 0f, 0.20f, 0.45f, 0.70f, 0.92f, 1f};

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < el.length - 1; i++) {
            float[] c0 = mix(hor, zen, t[i]);
            float[] c1 = mix(hor, zen, t[i + 1]);
            for (int s = 0; s < SEG; s++) {
                double a0 = Math.PI * 2.0 * s / SEG;
                double a1 = Math.PI * 2.0 * (s + 1) / SEG;
                vm(b, m, sphere(el[i], a0), c0[0], c0[1], c0[2], 1f);
                vm(b, m, sphere(el[i], a1), c0[0], c0[1], c0[2], 1f);
                vm(b, m, sphere(el[i + 1], a1), c1[0], c1[1], c1[2], 1f);
                vm(b, m, sphere(el[i + 1], a0), c1[0], c1[1], c1[2], 1f);
            }
        }
        tess.end();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    // ---------- sol, luna y aro de luz ----------

    /**
     * Requiere la modelview de RenderSystem en identidad (lo hace EclipseWorldRenderer).
     * d = distancia a la que se dibujan (más lejos que el terreno, dentro del plano lejano).
     */
    public static void renderBodies(Camera camera, float p, float d) {
        View view = new View(camera);
        float s = d / 100f; // todo está definido para distancia 100; se escala a d
        Vec3 sun = EclipseSky.sunDir();
        Vec3 moon = EclipseSky.moonDir(p);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();   // el terreno tapa el eclipse como debe
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // resplandor y aro (mezcla aditiva)
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float h = 0.55f + 0.45f * EclipseSky.smooth(0.3f, 1f, p);
        radial(view, sun, d, s, new float[]{0f, 7f, 7.6f, 10f, 16f, 30f},
                new float[]{1f, 1f, 0.85f * h, 0.35f * h, 0.12f * h, 0f}, 1f, 0.97f, 0.90f);
        float c = EclipseSky.smooth(0.8f, 1f, p); // aro fino y brillante al formarse el eclipse
        if (c > 0f) {
            radial(view, sun, d, s, new float[]{7.3f, 7.7f, 9.5f},
                    new float[]{c, 0.7f * c, 0f}, 1f, 1f, 1f);
        }

        // disco del sol y luna negra encima
        RenderSystem.defaultBlendFunc();
        disc(view, sun, d, 7.0f * s, 1f, 0.98f, 0.92f, 1f);
        float moonAlpha = EclipseSky.smooth(0f, 0.08f, p); // la luna "aparece"
        if (moonAlpha > 0f) disc(view, moon, d, 7.45f * s, 0f, 0f, 0f, moonAlpha);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    // ---------- helpers ----------

    /** Vértice con matriz (cúpula). */
    private static void vm(BufferBuilder b, Matrix4f m, Vec3 pt, float r, float g, float bl, float a) {
        b.vertex(m, (float) pt.x, (float) pt.y, (float) pt.z).color(r, g, bl, a).endVertex();
    }

    /** Vértice de un punto del mundo (relativo a la cámara) pasado a espacio de vista. */
    private static void vv(BufferBuilder b, View view, Vec3 world, float r, float g, float bl, float a) {
        Vec3 v = view.toView(world);
        b.vertex(IDENTITY, (float) v.x, (float) v.y, (float) v.z).color(r, g, bl, a).endVertex();
    }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2])};
    }

    private static Vec3 sphere(double elevDeg, double az) {
        return EclipseSky.dir(Math.toRadians(elevDeg), az).scale(100.0);
    }

    private static Vec3 ringPt(Vec3 c, Vec3 u, Vec3 w, double rad, double a) {
        return c.add(u.scale(Math.cos(a) * rad)).add(w.scale(Math.sin(a) * rad));
    }

    private static void radial(View view, Vec3 dir, float d, float s, float[] radii, float[] alphas,
                               float cr, float cg, float cb) {
        Vec3 c = dir.scale(d);
        Vec3 u = dir.cross(UP).normalize();
        Vec3 w = u.cross(dir).normalize();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < radii.length - 1; i++) {
            for (int k = 0; k < SEG; k++) {
                double a0 = Math.PI * 2.0 * k / SEG;
                double a1 = Math.PI * 2.0 * (k + 1) / SEG;
                vv(b, view, ringPt(c, u, w, radii[i] * s, a0), cr, cg, cb, alphas[i]);
                vv(b, view, ringPt(c, u, w, radii[i] * s, a1), cr, cg, cb, alphas[i]);
                vv(b, view, ringPt(c, u, w, radii[i + 1] * s, a1), cr, cg, cb, alphas[i + 1]);
                vv(b, view, ringPt(c, u, w, radii[i + 1] * s, a0), cr, cg, cb, alphas[i + 1]);
            }
        }
        tess.end();
    }

    private static void disc(View view, Vec3 dir, float d, float radius, float r, float g, float bl, float a) {
        Vec3 c = dir.scale(d);
        Vec3 u = dir.cross(UP).normalize();
        Vec3 w = u.cross(dir).normalize();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        vv(b, view, c, r, g, bl, a);
        for (int k = 0; k <= SEG; k++) {
            vv(b, view, ringPt(c, u, w, radius, Math.PI * 2.0 * k / SEG), r, g, bl, a);
        }
        tess.end();
    }
}
