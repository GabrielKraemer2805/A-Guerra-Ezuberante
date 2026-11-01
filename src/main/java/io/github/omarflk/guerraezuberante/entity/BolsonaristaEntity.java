package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Spawn egg of the Bolsonarista villagers, like {@link PetistaEntity}: on its first server tick it becomes a
 * vanilla villager of the {@link ModVillagers#BOLSONARISTA} type. An adult without a job gets one of {@link #SKIN_JOBS},
 * so the egg gives every bolsonarista skin.
 */
public final class BolsonaristaEntity extends Villager {
    /** One profession per bolsonarista skin group of {@code PetistaRenderer}. */
    private static final List<VillagerProfession> SKIN_JOBS = List.of(VillagerProfession.FARMER, VillagerProfession.LIBRARIAN,
            VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.NITWIT);

    public BolsonaristaEntity(EntityType<? extends BolsonaristaEntity> type, Level level) {
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
                ModVillagers.makeBolsonarista(villager);
                if (!villager.isBaby() && villager.getVillagerData().getProfession() == VillagerProfession.NONE) {
                    var job = SKIN_JOBS.get(random.nextInt(SKIN_JOBS.size()));
                    villager.setVillagerData(villager.getVillagerData().setProfession(job));
                    // Without a job site, a villager with no XP drops its profession; 1 XP keeps it, like a traded one.
                    if (job != VillagerProfession.NITWIT) { villager.setVillagerXp(1); }
                }
                discard();
                server.addFreshEntity(villager);
                return;
            }
        }
        super.tick();
    }
}
