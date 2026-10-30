package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

/**
 * Kept so the "petista" ID, its spawn egg and the petistas saved by 0.015.0 still work. Petistas are now vanilla
 * villagers of the {@link ModVillagers#PETISTA} type (only real villagers breed and gossip with each other), so on its
 * first server tick this entity becomes one, keeping its profession, level, trades, experience, name and position.
 */
public final class PetistaEntity extends Villager {
    public PetistaEntity(EntityType<? extends PetistaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server && !isRemoved()) {
            CompoundTag tag = saveWithoutId(new CompoundTag());
            tag.remove("UUID");
            Villager villager = EntityType.VILLAGER.create(server);
            if (villager != null) {
                villager.load(tag);
                ModVillagers.makePetista(villager);
                discard();
                server.addFreshEntity(villager);
                return;
            }
        }
        super.tick();
    }
}
