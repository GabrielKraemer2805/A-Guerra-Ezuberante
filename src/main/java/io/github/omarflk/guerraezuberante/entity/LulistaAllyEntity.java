package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.Faction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Lula's allies (Haddad, Boulos, Dilma): they defend the Lulista village. */
public abstract class LulistaAllyEntity extends VillageAllyEntity {
    protected LulistaAllyEntity(EntityType<? extends LulistaAllyEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public Faction faction() {
        return Faction.LULISTA;
    }
}
