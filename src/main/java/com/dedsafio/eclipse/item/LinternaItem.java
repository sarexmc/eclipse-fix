package com.dedsafio.eclipse.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LinternaItem extends Item {
    /** Durabilidad = segundos de luz (1200 = 20 minutos). */
    public static final int MAX_DURABILITY = 1200;
    /** Tiempo de arranque: al hacer clic derecho tarda 2 segundos en encenderse (cuenta regresiva en pantalla). */
    public static final int WARMUP_TICKS = 40;

    private static final String TAG_ON = "Encendida";
    private static final String TAG_START = "ArranqueInicio";

    public LinternaItem(Properties properties) {
        super(properties);
    }

    // ---------- estado (guardado en el NBT del item) ----------

    public static boolean isOn(ItemStack stack) {
        return stack.getItem() instanceof LinternaItem && stack.hasTag() && stack.getTag().getBoolean(TAG_ON);
    }

    public static void setOn(ItemStack stack, boolean on) {
        stack.getOrCreateTag().putBoolean(TAG_ON, on);
    }

    /** True mientras corre la cuenta regresiva de encendido. */
    public static boolean isStarting(ItemStack stack) {
        return stack.getItem() instanceof LinternaItem && stack.hasTag() && stack.getTag().contains(TAG_START);
    }

    /** Tick de juego en el que se empezó a encender. */
    public static long startTime(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getLong(TAG_START) : 0L;
    }

    private static void clearStart(ItemStack stack) {
        if (stack.hasTag()) stack.getTag().remove(TAG_START);
    }

    /** True si el jugador sostiene una linterna ya encendida (mano principal o secundaria). */
    public static boolean isHolding(Player player) {
        return isOn(player.getMainHandItem()) || isOn(player.getOffhandItem());
    }

    // ---------- comportamiento ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (isOn(stack)) {
                setOn(stack, false); // apagar es instantáneo
                level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6F, 0.8F);
            } else if (isStarting(stack)) {
                clearStart(stack);   // cancelar el encendido
                level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6F, 0.8F);
            } else {
                stack.getOrCreateTag().putLong(TAG_START, level.getGameTime()); // empieza la cuenta de 2 s
                level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5F, 1.0F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        final boolean inMain = player.getMainHandItem() == stack;
        final boolean inOff = player.getOffhandItem() == stack;

        if (isStarting(stack)) {
            if (!inMain && !inOff) { clearStart(stack); return; } // cambiaste de slot: se cancela
            if (level.getGameTime() - startTime(stack) >= WARMUP_TICKS) {
                clearStart(stack);
                setOn(stack, true);
                level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.7F, 1.4F);
            }
            return;
        }

        if (!isOn(stack)) return;
        if (!inMain && !inOff) {
            setOn(stack, false); // se apaga sola si no la llevas en la mano
            return;
        }
        if (level.getGameTime() % 20 == 0) { // 1 de durabilidad por segundo encendida
            stack.hurtAndBreak(1, player,
                    p -> p.broadcastBreakEvent(inMain ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        setOn(item, false);
        clearStart(item);
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.REDSTONE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dedsafio_eclipse.linterna"));
        tooltip.add(Component.translatable("tooltip.dedsafio_eclipse.linterna2"));
    }
}
