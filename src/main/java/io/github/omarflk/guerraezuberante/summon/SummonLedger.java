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

    public void register(UUID owner, UUID unit) {
        Group group = owners.computeIfAbsent(owner, ignored -> new Group());
        if (!group.closed && group.units.add(unit)) { setDirty(); }
    }

    public void release(UUID owner, UUID unit) {
        Group group = owners.get(owner);
        if (group != null && group.units.remove(unit)) {
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
            ListTag units = new ListTag();
            group.units.forEach(unit -> units.add(NbtUtils.createUUID(unit)));
            entry.put("Units", units);
            list.add(entry);
        });
        tag.put("Owners", list);
        return tag;
    }
}
