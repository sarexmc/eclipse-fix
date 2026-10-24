package com.dedsafio.eclipse.network;

import com.dedsafio.eclipse.client.ClientEclipse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record EclipseSyncPacket(boolean active, float progress, float speed) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeFloat(progress);
        buf.writeFloat(speed);
    }

    public static EclipseSyncPacket decode(FriendlyByteBuf buf) {
        return new EclipseSyncPacket(buf.readBoolean(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(EclipseSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientEclipse.apply(pkt.active(), pkt.progress(), pkt.speed())));
        ctx.get().setPacketHandled(true);
    }
}
