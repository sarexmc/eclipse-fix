package com.dedsafio.eclipse.client;

import com.dedsafio.eclipse.DedsafioEclipse;
import com.dedsafio.eclipse.eclipse.EclipseSky;
import com.dedsafio.eclipse.item.LinternaItem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Oscuridad del eclipse dentro del mundo (gris que crece y luego negro casi total) con un "agujero" de luz por cada
 * linterna encendida: la tuya (centrada en la mirilla) y las de los demás jugadores (donde apunta su haz).
 * La mano y el HUD NO se oscurecen. Después se dibuja el eclipse (círculo negro + aro de luz).
 *
 * IMPORTANTE: en esta etapa la matriz modelview de RenderSystem ya trae la rotación de la cámara, así que se pone
 * en identidad mientras dibujamos (si no, la capa de oscuridad queda "clavada" en el mundo y se ve como una órbita).
 */
@Mod.EventBusSubscriber(modid = DedsafioEclipse.MOD_ID, value = Dist.CLIENT)
public final class EclipseWorldRenderer {
    private EclipseWorldRenderer() {}

    private static final Matrix4f IDENTITY = new Matrix4f();
    private static final int GX = 96, GY = 54;
    private static final int SEG = 48;

    /** Agujero de luz en el plano de pantalla (unidades: tangente del ángulo de visión). */
    private record Hole(float cx, float cy, float rIn, float rOut) {}

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        if (!ClientEclipse.isActiveHere()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        float pt = event.getPartialTick();
        float p = ClientEclipse.get(pt);

        Matrix4f savedProj = new Matrix4f(event.getProjectionMatrix());
        PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.setIdentity();
        RenderSystem.applyModelViewMatrix();
        try {
            try {
                darkness(event, mc, p, pt);
            } finally {
                // la oscuridad usa su propia proyección (sin balanceo); el eclipse vuelve a la del mundo
                RenderSystem.setProjectionMatrix(savedProj, VertexSorting.DISTANCE_TO_ORIGIN);
            }
            float d = mc.gameRenderer.getDepthFar() * 0.5f;
            SkyRenderer.renderBodies(event.getCamera(), p, d);
        } finally {
            RenderSystem.setProjectionMatrix(savedProj, VertexSorting.DISTANCE_TO_ORIGIN);
            mv.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    // ---------- oscuridad ----------

    private static void darkness(RenderLevelStageEvent event, Minecraft mc, float p, float pt) {
        float gray = EclipseSky.grayAmount(p);
        float dark = EclipseSky.darkAmount(p);
        float alpha = Mth.lerp(dark, 0.38f * gray, 0.95f);
        float col = Mth.lerp(dark, 0.45f, 0.0f);
        if (alpha <= 0.001f) return;

        Matrix4f proj = event.getProjectionMatrix();
        float tanY = 1f / proj.m11();
        float tanX = 1f / proj.m00();
        List<Hole> holes = collectHoles(event.getCamera(), mc, pt, tanX, tanY);

        // Proyección propia: la del mundo incluye el balanceo al caminar y el salto, y arrastraría la capa negra.
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(-tanX, tanX, -tanY, tanY, 0.0f, 10.0f), VertexSorting.ORTHOGRAPHIC_Z);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float ex = tanX * 1.05f, ey = tanY * 1.05f;
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        if (holes.isEmpty()) {
            quad(b, -ex, -ey, ex, ey, col, alpha, alpha, alpha, alpha);
            tess.end();
            return;
        }

        float[][] a = new float[GY + 1][GX + 1];
        for (int j = 0; j <= GY; j++) {
            float y = -ey + 2f * ey * j / GY;
            for (int i = 0; i <= GX; i++) {
                float x = -ex + 2f * ex * i / GX;
                float v = alpha;
                for (Hole h : holes) {
                    float d = (float) Math.hypot(x - h.cx(), y - h.cy());
                    v *= EclipseSky.smooth(h.rIn(), h.rOut(), d);
                }
                a[j][i] = v;
            }
        }
        for (int j = 0; j < GY; j++) {
            float y0 = -ey + 2f * ey * j / GY;
            float y1 = -ey + 2f * ey * (j + 1) / GY;
            for (int i = 0; i < GX; i++) {
                float x0 = -ex + 2f * ex * i / GX;
                float x1 = -ex + 2f * ex * (i + 1) / GX;
                b.vertex(IDENTITY, x0, y0, -1f).color(col, col, col, a[j][i]).endVertex();
                b.vertex(IDENTITY, x1, y0, -1f).color(col, col, col, a[j][i + 1]).endVertex();
                b.vertex(IDENTITY, x1, y1, -1f).color(col, col, col, a[j + 1][i + 1]).endVertex();
                b.vertex(IDENTITY, x0, y1, -1f).color(col, col, col, a[j + 1][i]).endVertex();
            }
        }
        tess.end();

        // brillo cálido dentro de cada haz (mezcla aditiva)
        float lit = 0.25f + 0.75f * dark;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        for (Hole h : holes) {
            float r1 = h.rOut();
            glow(h.cx(), h.cy(),
                    new float[]{0f, r1 * 0.15f, r1 * 0.40f, r1 * 0.75f},
                    new float[]{0.30f * lit, 0.20f * lit, 0.07f * lit, 0f});
        }
        RenderSystem.defaultBlendFunc();
    }

    private static List<Hole> collectHoles(Camera cam, Minecraft mc, float pt, float tanX, float tanY) {
        List<Hole> holes = new ArrayList<>();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        Vec3 camPos = cam.getPosition();
        Vector3f look = cam.getLookVector();
        Vector3f up = cam.getUpVector();
        Vector3f left = cam.getLeftVector();
        float screenH = 2f * tanY;

        for (Player pl : mc.level.players()) {
            if (pl.isSpectator() || !LinternaItem.isHolding(pl)) continue;

            if (pl == mc.player && firstPerson) {
                // tu linterna: siempre centrada en la mirilla
                holes.add(new Hole(0f, 0f, 0.16f * screenH, 0.62f * screenH));
                continue;
            }
            Vec3 eye = pl.getEyePosition(pt);
            if (eye.distanceToSqr(camPos) > 64.0 * 64.0) continue;
            Vec3 dir = pl.getViewVector(pt);
            Vec3 end = eye.add(dir.scale(24.0));
            HitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pl));
            Vec3 rel = hit.getLocation().subtract(camPos);

            double vz = rel.x * look.x() + rel.y * look.y() + rel.z * look.z();
            if (vz < 0.3) continue; // el punto iluminado está detrás de la cámara
            double vx = -(rel.x * left.x() + rel.y * left.y() + rel.z * left.z());
            double vy = rel.x * up.x() + rel.y * up.y() + rel.z * up.z();

            float cx = (float) (vx / vz);
            float cy = (float) (vy / vz);
            float rOut = (float) Math.max(0.06, 3.4 / vz); // charco de luz de ~3,4 bloques de radio
            holes.add(new Hole(cx, cy, rOut * 0.35f, rOut));
        }
        return holes;
    }

    // ---------- geometría 2D en el plano z = -1 (espacio de vista) ----------

    private static void quad(BufferBuilder b, float x0, float y0, float x1, float y1, float col,
                             float a00, float a10, float a11, float a01) {
        b.vertex(IDENTITY, x0, y0, -1f).color(col, col, col, a00).endVertex();
        b.vertex(IDENTITY, x1, y0, -1f).color(col, col, col, a10).endVertex();
        b.vertex(IDENTITY, x1, y1, -1f).color(col, col, col, a11).endVertex();
        b.vertex(IDENTITY, x0, y1, -1f).color(col, col, col, a01).endVertex();
    }

    private static void glow(float cx, float cy, float[] radii, float[] alphas) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < radii.length - 1; i++) {
            for (int s = 0; s < SEG; s++) {
                float a0 = (float) (Math.PI * 2.0 * s / SEG);
                float a1 = (float) (Math.PI * 2.0 * (s + 1) / SEG);
                gv(b, cx, cy, radii[i], a0, alphas[i]);
                gv(b, cx, cy, radii[i], a1, alphas[i]);
                gv(b, cx, cy, radii[i + 1], a1, alphas[i + 1]);
                gv(b, cx, cy, radii[i + 1], a0, alphas[i + 1]);
            }
        }
        tess.end();
    }

    private static void gv(BufferBuilder b, float cx, float cy, float r, float ang, float a) {
        b.vertex(IDENTITY, cx + Mth.cos(ang) * r, cy + Mth.sin(ang) * r, -1f).color(1f, 0.97f, 0.88f, a).endVertex();
    }
}
