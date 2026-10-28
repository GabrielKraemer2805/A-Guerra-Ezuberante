package io.github.omarflk.guerraezuberante.faction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** World-wide faction membership, stored once in the overworld so every dimension sees the same choice. */
public final class FactionData extends SavedData {
    private final Map<UUID, Faction> members = new HashMap<>();
    /** Kept across faction changes and clears: a player gets one kit per world. */
    private final Set<UUID> kitsGiven = new HashSet<>();

    public static FactionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FactionData::load, FactionData::new,
                "guerra_ezuberante_factions");
    }

    public Optional<Faction> get(UUID player) {
        return Optional.ofNullable(members.get(player));
    }

    public void set(UUID player, Faction faction) {
        if (members.put(player, faction) != faction) { setDirty(); }
    }

    public boolean clear(UUID player) {
        if (members.remove(player) == null) { return false; }
        setDirty();
        return true;
    }

    /** Marks the kit as given; false if this player already received one in this world. */
    public boolean markKitGiven(UUID player) {
        if (!kitsGiven.add(player)) { return false; }
        setDirty();
        return true;
    }

    public boolean kitGiven(UUID player) {
        return kitsGiven.contains(player);
    }

    public Map<UUID, Faction> all() {
        return Map.copyOf(members);
    }

    /** Unknown ids are dropped instead of failing the whole file, so a removed faction cannot corrupt the world. */
    public static FactionData load(CompoundTag tag) {
        FactionData data = new FactionData();
        for (Tag entry : tag.getList("Members", Tag.TAG_COMPOUND)) {
            CompoundTag member = (CompoundTag) entry;
            Faction.byId(member.getString("Faction")).ifPresent(faction -> data.members.put(member.getUUID("Player"), faction));
        }
        for (Tag entry : tag.getList("Kits", Tag.TAG_INT_ARRAY)) {
            data.kitsGiven.add(NbtUtils.loadUUID(entry));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        members.forEach((player, faction) -> {
            CompoundTag member = new CompoundTag();
            member.putUUID("Player", player);
            member.putString("Faction", faction.id());
            list.add(member);
        });
        tag.put("Members", list);
        ListTag kits = new ListTag();
        kitsGiven.forEach(player -> kits.add(NbtUtils.createUUID(player)));
        tag.put("Kits", kits);
        return tag;
    }
}
