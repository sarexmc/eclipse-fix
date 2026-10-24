package com.dedsafio.eclipse.network;

import com.dedsafio.eclipse.DedsafioEclipse;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DedsafioEclipse.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, EclipseSyncPacket.class,
                EclipseSyncPacket::encode, EclipseSyncPacket::decode, EclipseSyncPacket::handle);
    }

    public static void sendToAll(EclipseSyncPacket pkt) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
    }

    public static void sendTo(ServerPlayer player, EclipseSyncPacket pkt) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pkt);
    }
}
