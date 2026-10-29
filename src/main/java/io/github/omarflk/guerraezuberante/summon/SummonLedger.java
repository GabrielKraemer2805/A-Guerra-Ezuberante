package io.github.omarflk.guerraezuberante.summon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent, per-dimension accounting. Unloaded troops still occupy their owner's slots. */
public final class SummonLedger extends SavedData {
    private static final class Group {
        final Set<UUID> units = new HashSet<>();
        boolean closed;
        UUID playerToken;
        final Map<UUID, Long> playerExpiry = new HashMap<>();
    }
    private final Map<UUID, Group> owners = new HashMap<>();

    public static SummonLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SummonLedger::load, SummonLedger::new,
                "guerra_ezuberante_summons");
    }

    public int count(UUID owner) {
        Group group = owners.get(owner);
        return group == null ? 0 : group.units.size();
    }

    public boolean isClosed(UUID owner) {
        Group group = owners.get(owner);
        return group != null && group.closed;
    }

    /** A new player life/session gets a different token; old unloaded units cannot rejoin it. */
    public UUID playerToken(UUID owner) {
        Group group = owners.computeIfAbsent(owner, ignored -> new Group());
        if (group.playerToken == null) {
            group.units.clear(); group.playerExpiry.clear(); group.closed = false;
            group.playerToken = UUID.randomUUID(); setDirty();
        }
        return group.playerToken;
    }

    public void registerPlayer(UUID owner, UUID unit, UUID token, long expiresAt) {
        Group group = owners.get(owner);
        if (group != null && token.equals(group.playerToken)) {
            group.units.add(unit); group.playerExpiry.put(unit, expiresAt); setDirty();
        }
    }

    public boolean playerUnitActive(UUID owner, UUID unit, UUID token, long now) {
        Group group = owners.get(owner);
        return group != null && token.equals(group.playerToken) && group.units.contains(unit)
                && group.playerExpiry.getOrDefault(unit, 0L) > now;
    }

    /** Player lifetimes use world time even while the unit's chunk is unloaded. */
    public int countPlayer(UUID owner, long now) {
        Group group = owners.get(owner);
        if (group == null) { return 0; }
        if (group.playerToken == null) { forgetPlayer(owner); return 0; } // Legacy 0.012.0 player troops.
        if (group.units.removeIf(unit -> group.playerExpiry.getOrDefault(unit, 0L) <= now)) {
            group.playerExpiry.keySet().retainAll(group.units); setDirty();
        }
        return group.units.size();
    }

    public void forgetPlayer(UUID owner) {
        if (owners.remove(owner) != null) { setDirty(); }
    }

    public void register(UUID owner, UUID unit) {
        Group group = owners.computeIfAbsent(owner, ignored -> new Group());
        if (!group.closed && group.units.add(unit)) { setDirty(); }
    }

    public void release(UUID owner, UUID unit) {
        Group group = owners.get(owner);
        if (group != null && group.units.remove(unit)) {
            group.playerExpiry.remove(unit);
            if (group.units.isEmpty()) { owners.remove(owner); }
            setDirty();
        }
    }

    public void close(UUID owner) {
        Group group = owners.get(owner);
        if (group != null) { group.closed = true; setDirty(); }
    }

    public static SummonLedger load(CompoundTag tag) {
        SummonLedger ledger = new SummonLedger();
        for (Tag entry : tag.getList("Owners", Tag.TAG_COMPOUND)) {
            CompoundTag data = (CompoundTag) entry;
            Group group = new Group();
            group.closed = data.getBoolean("Closed");
            if (data.hasUUID("PlayerToken")) { group.playerToken = data.getUUID("PlayerToken"); }
            for (Tag value : data.getList("PlayerExpiry", Tag.TAG_COMPOUND)) {
                CompoundTag item = (CompoundTag) value;
                if (item.hasUUID("Unit")) { group.playerExpiry.put(item.getUUID("Unit"), item.getLong("At")); }
            }
            for (Tag unit : data.getList("Units", Tag.TAG_INT_ARRAY)) {
                group.units.add(NbtUtils.loadUUID(unit));
            }
            ledger.owners.put(data.getUUID("Owner"), group);
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        owners.forEach((owner, group) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", owner);
            entry.putBoolean("Closed", group.closed);
            if (group.playerToken != null) { entry.putUUID("PlayerToken", group.playerToken); }
            ListTag expiry = new ListTag();
            group.playerExpiry.forEach((unit, at) -> {
                CompoundTag value = new CompoundTag(); value.putUUID("Unit", unit); value.putLong("At", at); expiry.add(value);
            });
            entry.put("PlayerExpiry", expiry);
            ListTag units = new ListTag();
            group.units.forEach(unit -> units.add(NbtUtils.createUUID(unit)));
            entry.put("Units", units);
            list.add(entry);
        });
        tag.put("Owners", list);
        return tag;
    }
}
