package com.dedsafio.eclipse.eclipse;

import com.dedsafio.eclipse.DedsafioEclipse;
import com.dedsafio.eclipse.ModEntities;
import com.dedsafio.eclipse.entity.HerobrineEntity;
import com.dedsafio.eclipse.network.EclipseSyncPacket;
import com.dedsafio.eclipse.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Lógica del servidor: avance del eclipse, quemadura, aparición de Herobrine y sincronización. */
@Mod.EventBusSubscriber(modid = DedsafioEclipse.MOD_ID)
public final class EclipseServer {
    private EclipseServer() {}

    // ---------- control (lo usan los comandos) ----------

    public static void start(MinecraftServer server, int seconds) {
        EclipseData data = EclipseData.get(server);
        ServerLevel overworld = server.overworld();
        if (!data.active) {
            data.active = true;
            data.progress = 0f;
            data.prevDaylightRule = overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
            overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            long day = overworld.getDayTime() / 24000L;
            overworld.setDayTime(day * 24000L + 6000L); // mediodía, para que el eclipse se vea bien
        }
        data.speed = 1.0f / (Math.max(1, seconds) * 20.0f);
        data.setDirty();
        sync(server, data);
    }

    /** Devuelve false si no había eclipse activo. */
    public static boolean stop(MinecraftServer server, int seconds) {
        EclipseData data = EclipseData.get(server);
        if (!data.active) return false;
        data.speed = -1.0f / (Math.max(1, seconds) * 20.0f);
        data.setDirty();
        sync(server, data);
        return true;
    }

    private static void finish(MinecraftServer server, EclipseData data) {
        data.active = false;
        data.progress = 0f;
        data.speed = 0f;
        data.setDirty();
        server.overworld().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(data.prevDaylightRule, server);
        sync(server, data);
    }

    public static void sync(MinecraftServer server, EclipseData data) {
        ModNetwork.sendToAll(new EclipseSyncPacket(data.active, data.progress, data.speed));
    }

    // ---------- eventos ----------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            EclipseData data = EclipseData.get(sp.server);
            ModNetwork.sendTo(sp, new EclipseSyncPacket(data.active, data.progress, data.speed));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        EclipseData data = EclipseData.get(server);
        if (!data.active) return;

        if (data.speed != 0f) {
            data.progress += data.speed;
            data.setDirty();
            if (data.speed > 0f && data.progress >= 1f) {
                data.progress = 1f;
                data.speed = 0f;
                sync(server, data);
            } else if (data.speed < 0f && data.progress <= 0f) {
                finish(server, data);
                return;
            }
        }

        long t = server.getTickCount();
        ServerLevel overworld = server.overworld();
        if (t % 20 == 0) sync(server, data);          // corrige deriva del cliente
        if (t % 10 == 0) burnWatchers(overworld);
        if (t % 20 == 0) maybeSpawnHerobrine(overworld, data);
    }

    // ---------- mirar al eclipse quema ----------

    private static void burnWatchers(ServerLevel level) {
        Vec3 sun = EclipseSky.sunDir();
        for (ServerPlayer sp : level.players()) {
            if (!sp.isAlive() || sp.isCreative() || sp.isSpectator()) continue;
            if (sp.getLookAngle().dot(sun) < EclipseSky.BURN_COS) continue;
            Vec3 eye = sp.getEyePosition();
            ClipContext ctx = new ClipContext(eye, eye.add(sun.scale(96.0D)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp);
            if (level.clip(ctx).getType() != HitResult.Type.MISS) continue; // hay techo/bloques tapando el sol
            sp.setSecondsOnFire(3);
            sp.hurt(level.damageSources().onFire(), 1.0F);
            sp.displayClientMessage(Component.translatable("message.dedsafio_eclipse.burn"), true);
        }
    }

    // ---------- Herobrine ----------

    private static void maybeSpawnHerobrine(ServerLevel level, EclipseData data) {
        if (data.progress < 0.95f || data.speed < 0f) return; // solo en la oscuridad total
        for (ServerPlayer sp : level.players()) {
            if (!sp.isAlive() || sp.isCreative() || sp.isSpectator()) continue;
            if (data.isInBunker(sp.blockPosition())) continue;
            if (!level.getEntitiesOfClass(HerobrineEntity.class, sp.getBoundingBox().inflate(80.0D)).isEmpty()) continue;
            if (level.random.nextFloat() > 0.10f) continue;
            spawnHerobrineNear(level, sp, data, false);
        }
    }

    public static boolean spawnHerobrineNear(ServerLevel level, ServerPlayer player, EclipseData data, boolean ignoreEclipse) {
        RandomSource r = level.random;
        for (int i = 0; i < 20; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0D;
            double dist = 12.0D + r.nextDouble() * 10.0D;
            int x = Mth.floor(player.getX() + Math.cos(ang) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(ang) * dist);
            BlockPos ground = findGround(level, x, player.blockPosition().getY(), z);
            if (ground == null || data.isInBunker(ground)) continue;
            HerobrineEntity h = ModEntities.HEROBRINE.get().create(level);
            if (h == null) return false;
            h.moveTo(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D, r.nextFloat() * 360.0F, 0.0F);
            h.setIgnoreEclipse(ignoreEclipse);
            level.addFreshEntity(h);
            level.playSound(null, h.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.5F);
            return true;
        }
        return false;
    }

    private static BlockPos findGround(ServerLevel level, int x, int startY, int z) {
        for (int dy = 6; dy >= -8; dy--) {
            BlockPos pos = new BlockPos(x, startY + dy, z);
            if (!level.isLoaded(pos)) return null;
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                return pos;
            }
        }
        return null;
    }
}
