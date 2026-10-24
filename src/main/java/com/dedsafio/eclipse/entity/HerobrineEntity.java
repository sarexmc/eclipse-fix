package com.dedsafio.eclipse.entity;

import com.dedsafio.eclipse.eclipse.EclipseData;
import com.dedsafio.eclipse.item.LinternaItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Herobrine: se queda quieto mirándote. Si lo alumbras con la linterna medio segundo, desaparece;
 * si pasan 5 segundos sin alumbrarlo, te persigue y te golpea. Desaparece al acabar el eclipse o si tú o él están en un búnker.
 */
public class HerobrineEntity extends Monster {
    public static final int UNLIT_TICKS_TO_CHASE = 100; // 5 segundos sin alumbrarlo -> te persigue
    public static final int LIT_TICKS_TO_VANISH = 10;   // medio segundo alumbrado -> desaparece

    private int litTicks;

    private int unlitTicks;
    private boolean chasing;
    private int attackCooldown;
    private int lonelyTicks;
    private boolean ignoreEclipse;

    public HerobrineEntity(EntityType<? extends HerobrineEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public boolean isIgnoreEclipse() { return ignoreEclipse; }
    public void setIgnoreEclipse(boolean v) { this.ignoreEclipse = v; }
    public boolean isChasing() { return chasing; }

    private boolean isLitBy(Player player) {
        if (!LinternaItem.isHolding(player)) return false;
        Vec3 to = this.getEyePosition().subtract(player.getEyePosition());
        double dist = to.length();
        if (dist > 28.0D || dist < 0.5D) return false;
        if (player.getLookAngle().dot(to.scale(1.0D / dist)) < 0.93D) return false; // ~21 grados de cono
        return player.hasLineOfSight(this);
    }

    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(), 25, 0.3D, 0.7D, 0.3D, 0.01D);
        this.discard();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level)) return;
        EclipseData data = EclipseData.get(level.getServer());

        if (!ignoreEclipse && !data.active) { vanish(level); return; }
        if (data.isInBunker(this.blockPosition())) { vanish(level); return; }

        Player target = level.getNearestPlayer(getX(), getY(), getZ(), 64.0D, e -> e.isAlive() && !e.isSpectator());
        if (target == null) {
            if (++lonelyTicks > 200) vanish(level);
            return;
        }
        lonelyTicks = 0;
        if (data.isInBunker(target.blockPosition())) { vanish(level); return; }

        this.getLookControl().setLookAt(target, 90.0F, 90.0F);

        if (isLitBy(target)) {
            litTicks++;
            unlitTicks = 0;
            this.getNavigation().stop();
            if (litTicks >= LIT_TICKS_TO_VANISH) {
                level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.5F);
                vanish(level);
                return;
            }
        } else {
            litTicks = 0;
            if (!chasing && ++unlitTicks >= UNLIT_TICKS_TO_CHASE) {
                chasing = true;
                this.playSound(SoundEvents.ENDERMAN_SCREAM, 2.0F, 0.5F);
            }
        }

        if (chasing) {
            if (this.tickCount % 5 == 0) this.getNavigation().moveTo(target, 1.0D);
            if (this.getNavigation().isDone()) {
                // por si no hay camino calculado: camina directo hacia ti
                this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
            }
            if (this.horizontalCollision && this.onGround()) this.getJumpControl().jump();
            if (attackCooldown > 0) attackCooldown--;
            if (attackCooldown <= 0 && this.distanceToSqr(target) < 4.0D) {
                this.swing(InteractionHand.MAIN_HAND);
                this.doHurtTarget(target);
                attackCooldown = 12;
            }
        }
    }

    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
    @Nullable @Override protected SoundEvent getAmbientSound() { return null; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IgnoreEclipse", ignoreEclipse);
        tag.putBoolean("Chasing", chasing);
        tag.putInt("UnlitTicks", unlitTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ignoreEclipse = tag.getBoolean("IgnoreEclipse");
        chasing = tag.getBoolean("Chasing");
        unlitTicks = tag.getInt("UnlitTicks");
    }
}
