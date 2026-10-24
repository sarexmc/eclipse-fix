package com.dedsafio.eclipse.command;

import com.dedsafio.eclipse.DedsafioEclipse;
import com.dedsafio.eclipse.eclipse.EclipseData;
import com.dedsafio.eclipse.eclipse.EclipseServer;
import com.dedsafio.eclipse.entity.HerobrineEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = DedsafioEclipse.MOD_ID)
public final class EclipseCommand {
    private EclipseCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("eclipse")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(c -> start(c, 120))
                        .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 3600))
                                .executes(c -> start(c, IntegerArgumentType.getInteger(c, "segundos")))))
                .then(Commands.literal("stop")
                        .executes(c -> stop(c, 60))
                        .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 3600))
                                .executes(c -> stop(c, IntegerArgumentType.getInteger(c, "segundos")))))
                .then(Commands.literal("estado").executes(EclipseCommand::status))
                .then(Commands.literal("bunker")
                        .then(Commands.literal("add")
                                .then(Commands.argument("nombre", StringArgumentType.word())
                                        .then(Commands.argument("radio", IntegerArgumentType.integer(1, 256))
                                                .executes(EclipseCommand::bunkerAdd))))
                        .then(Commands.literal("addbox")
                                .then(Commands.argument("nombre", StringArgumentType.word())
                                        .then(Commands.argument("desde", BlockPosArgument.blockPos())
                                                .then(Commands.argument("hasta", BlockPosArgument.blockPos())
                                                        .executes(EclipseCommand::bunkerAddBox)))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("nombre", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                EclipseData.get(c.getSource().getServer()).bunkers.keySet(), b))
                                        .executes(EclipseCommand::bunkerRemove)))
                        .then(Commands.literal("list").executes(EclipseCommand::bunkerList)))
                .then(Commands.literal("herobrine")
                        .then(Commands.literal("spawn").executes(EclipseCommand::herobrineSpawn))
                        .then(Commands.literal("clear").executes(EclipseCommand::herobrineClear))));
    }

    private static int start(CommandContext<CommandSourceStack> c, int seconds) {
        EclipseServer.start(c.getSource().getServer(), seconds);
        c.getSource().sendSuccess(() -> Component.literal("Eclipse iniciado: llegará a la oscuridad total en " + seconds + " s."), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> c, int seconds) {
        if (!EclipseServer.stop(c.getSource().getServer(), seconds)) {
            c.getSource().sendFailure(Component.literal("No hay ningún eclipse activo."));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("El eclipse terminará poco a poco en " + seconds + " s."), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        EclipseData d = EclipseData.get(c.getSource().getServer());
        String msg = d.active
                ? String.format("Eclipse activo: %.0f%% (%s)", d.progress * 100f,
                        d.speed > 0 ? "avanzando" : d.speed < 0 ? "terminando" : "oscuridad total")
                : "No hay eclipse activo.";
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int bunkerAdd(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "nombre");
        int r = IntegerArgumentType.getInteger(c, "radio");
        BlockPos center = BlockPos.containing(c.getSource().getPosition());
        EclipseData d = EclipseData.get(c.getSource().getServer());
        d.addBunker(name, center.offset(-r, -r, -r), center.offset(r, r, r));
        c.getSource().sendSuccess(() -> Component.literal("Búnker '" + name + "' creado (radio " + r + ") en " + center.toShortString()
                + ". Herobrine no aparecerá ahí."), true);
        return 1;
    }

    private static int bunkerAddBox(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String name = StringArgumentType.getString(c, "nombre");
        BlockPos a = BlockPosArgument.getBlockPos(c, "desde");
        BlockPos b = BlockPosArgument.getBlockPos(c, "hasta");
        EclipseData.get(c.getSource().getServer()).addBunker(name, a, b);
        c.getSource().sendSuccess(() -> Component.literal("Búnker '" + name + "' creado de " + a.toShortString() + " a " + b.toShortString() + "."), true);
        return 1;
    }

    private static int bunkerRemove(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "nombre");
        if (EclipseData.get(c.getSource().getServer()).removeBunker(name)) {
            c.getSource().sendSuccess(() -> Component.literal("Búnker '" + name + "' eliminado."), true);
            return 1;
        }
        c.getSource().sendFailure(Component.literal("No existe el búnker '" + name + "'."));
        return 0;
    }

    private static int bunkerList(CommandContext<CommandSourceStack> c) {
        EclipseData d = EclipseData.get(c.getSource().getServer());
        if (d.bunkers.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.literal("No hay búnkeres."), false);
            return 0;
        }
        for (EclipseData.Bunker b : d.bunkers.values()) {
            c.getSource().sendSuccess(() -> Component.literal("- " + b.name() + ": " + b.min().toShortString() + " -> " + b.max().toShortString()), false);
        }
        return d.bunkers.size();
    }

    private static int herobrineSpawn(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        EclipseData d = EclipseData.get(c.getSource().getServer());
        boolean ok = EclipseServer.spawnHerobrineNear(p.serverLevel(), p, d, true);
        if (ok) c.getSource().sendSuccess(() -> Component.literal("Herobrine ha aparecido..."), true);
        else c.getSource().sendFailure(Component.literal("No se encontró un lugar válido para Herobrine."));
        return ok ? 1 : 0;
    }

    private static int herobrineClear(CommandContext<CommandSourceStack> c) {
        int n = 0;
        for (ServerLevel level : c.getSource().getServer().getAllLevels()) {
            List<Entity> found = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (e instanceof HerobrineEntity) found.add(e);
            }
            for (Entity e : found) { e.discard(); n++; }
        }
        final int count = n;
        c.getSource().sendSuccess(() -> Component.literal("Herobrines eliminados: " + count), true);
        return count;
    }
}
