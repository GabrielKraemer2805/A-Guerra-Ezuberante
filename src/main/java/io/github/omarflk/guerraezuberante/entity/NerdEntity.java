package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;

import java.util.List;

/** Spawn egg of the Missão villagers (0.028), like {@link DireitistaEntity}: on its first server tick it becomes a vanilla villager of the nerd type. */
public final class NerdEntity extends Villager {
    /** One profession per nerd skin group of {@code PetistaRenderer}. */
    private static final List<VillagerProfession> SKIN_JOBS = List.of(VillagerProfession.LIBRARIAN, VillagerProfession.FARMER,
            VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.NITWIT);

    public NerdEntity(EntityType<? extends NerdEntity> type, Level level) {
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
                ModVillagers.makeNerd(villager);
                if (!villager.isBaby() && villager.getVillagerData().getProfession() == VillagerProfession.NONE) {
                    var job = SKIN_JOBS.get(random.nextInt(SKIN_JOBS.size()));
                    villager.setVillagerData(villager.getVillagerData().setProfession(job));
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
