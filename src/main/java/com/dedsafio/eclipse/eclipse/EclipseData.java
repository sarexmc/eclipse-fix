package com.dedsafio.eclipse.eclipse;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/** Estado persistente del eclipse y de los búnkeres (se guarda en el mundo). */
public class EclipseData extends SavedData {
    public boolean active;
    /** 0 = luna lejos / día normal, 1 = eclipse total. */
    public float progress;
    /** Cambio de progress por tick (positivo = avanzando, negativo = aclarando, 0 = quieto). */
    public float speed;
    public boolean prevDaylightRule = true;
    public final Map<String, Bunker> bunkers = new LinkedHashMap<>();

    public record Bunker(String name, BlockPos min, BlockPos max) {
        public boolean contains(BlockPos p) {
            return p.getX() >= min.getX() && p.getX() <= max.getX()
                    && p.getY() >= min.getY() && p.getY() <= max.getY()
                    && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
        }
    }

    public static EclipseData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(EclipseData::load, EclipseData::new, "dedsafio_eclipse");
    }

    public boolean isInBunker(BlockPos pos) {
        for (Bunker b : bunkers.values()) {
            if (b.contains(pos)) return true;
        }
        return false;
    }

    public void addBunker(String name, BlockPos a, BlockPos b) {
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        bunkers.put(name, new Bunker(name, min, max));
        setDirty();
    }

    public boolean removeBunker(String name) {
        boolean removed = bunkers.remove(name) != null;
        if (removed) setDirty();
        return removed;
    }

    public static EclipseData load(CompoundTag tag) {
        EclipseData d = new EclipseData();
        d.active = tag.getBoolean("active");
        d.progress = tag.getFloat("progress");
        d.speed = tag.getFloat("speed");
        d.prevDaylightRule = !tag.contains("prevDaylight") || tag.getBoolean("prevDaylight");
        ListTag list = tag.getList("bunkers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag b = list.getCompound(i);
            String name = b.getString("name");
            d.bunkers.put(name, new Bunker(name,
                    new BlockPos(b.getInt("x1"), b.getInt("y1"), b.getInt("z1")),
                    new BlockPos(b.getInt("x2"), b.getInt("y2"), b.getInt("z2"))));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("active", active);
        tag.putFloat("progress", progress);
        tag.putFloat("speed", speed);
        tag.putBoolean("prevDaylight", prevDaylightRule);
        ListTag list = new ListTag();
        for (Bunker b : bunkers.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("name", b.name());
            c.putInt("x1", b.min().getX()); c.putInt("y1", b.min().getY()); c.putInt("z1", b.min().getZ());
            c.putInt("x2", b.max().getX()); c.putInt("y2", b.max().getY()); c.putInt("z2", b.max().getZ());
            list.add(c);
        }
        tag.put("bunkers", list);
        return tag;
    }
}
