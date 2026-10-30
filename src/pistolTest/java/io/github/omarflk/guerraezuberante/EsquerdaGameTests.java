package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.CiroGomesEntity;
import io.github.omarflk.guerraezuberante.entity.ComunistaEntity;
import io.github.omarflk.guerraezuberante.entity.JonesManoelEntity;
import io.github.omarflk.guerraezuberante.entity.ManifestanteEntity;
import io.github.omarflk.guerraezuberante.entity.MarinaSilvaEntity;
import io.github.omarflk.guerraezuberante.entity.TabataAmaralEntity;
import io.github.omarflk.guerraezuberante.faction.ConvictionPowers;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Esquerda Independente (0.018): Jones Manoel, Tabata Amaral, Marina Silva, Ciro Gomes and the comunista villagers.
 * One batch per test: area powers and the arena cleanup reach far enough to touch a neighbouring test.
 */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EsquerdaGameTests {
    /** Open-air arena, high above the caves. */
    private static Vec3 arena(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 180, 3));
        for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new AABB(p).inflate(24)).forEach(Entity::discard);
        return Vec3.atBottomCenterOf(p);
    }
    private static <T extends Entity> T spawn(GameTestHelper h, EntityType<T> type, Vec3 at) {
        T entity = type.create(h.getLevel());
        entity.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addFreshEntity(entity);
        return entity;
    }
    /** Hostile, does not burn in daylight, never moves; big health so every hit is visible. */
    private static Husk dummy(GameTestHelper h, Vec3 at) {
        Husk husk = spawn(h, EntityType.HUSK, at);
        husk.setNoAi(true);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        husk.setHealth(200);
        return husk;
    }
    private static Villager comunista(GameTestHelper h, Vec3 at) {
        Villager villager = spawn(h, EntityType.VILLAGER, at);
        villager.setNoAi(true);
        ModVillagers.makeComunista(villager);
        return villager;
    }
    private static void cleanup(GameTestHelper h, Vec3 p) {
        h.getLevel().getEntitiesOfClass(Entity.class, new AABB(BlockPos.containing(p)).inflate(16),
                entity -> !(entity instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
    }
    private static void guarded(GameTestHelper h, Vec3 p, Runnable body) {
        try { body.run(); } finally { cleanup(h, p); }
    }

    @GameTest(template = "empty", batch = "ge_esquerda_1", timeoutTicks = 200)
    public static void jonesLectureSlowsEnemiesAndSparesComrades(GameTestHelper h) {
        var p = arena(h);
        JonesManoelEntity jones = spawn(h, ModEntities.JONES_MANOEL.get(), p);
        Husk husk = dummy(h, p.add(4, 0, 0));
        Villager villager = comunista(h, p.add(-3, 0, 0));
        TabataAmaralEntity tabata = spawn(h, ModEntities.TABATA_AMARAL.get(), p.add(0, 0, 3));
        tabata.setNoAi(true);
        h.assertTrue(jones.startLive() && jones.isActionLocked() && !jones.startLive(), "Lecture did not start once and lock him");
        h.runAfterDelay(5, () -> guarded(h, p, () -> {
            h.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1
                    && husk.hasEffect(MobEffects.WEAKNESS), "Husk not slowed and weakened by the lecture");
            h.assertTrue(!villager.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !tabata.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Lecture hit a comrade");
            h.succeed();
        }));
    }

    @GameTest(template = "empty", batch = "ge_esquerda_2", timeoutTicks = 100)
    public static void jonesFreezesTheTargetInPlace(GameTestHelper h) {
        var p = arena(h);
        JonesManoelEntity jones = spawn(h, ModEntities.JONES_MANOEL.get(), p);
        Husk husk = dummy(h, p.add(6, 0, 0));
        guarded(h, p, () -> {
            h.assertTrue(jones.freeze(husk), "Inverno Siberiano failed");
            h.assertTrue(husk.hasEffect(ModEffects.PARALYZED.get()) && husk.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "Target can still walk");
            h.assertTrue(husk.getTicksFrozen() > husk.getTicksRequiredToFreeze(), "No frost on the target");
            Villager villager = comunista(h, p.add(0, 0, 4));
            h.assertTrue(!jones.freeze(villager), "Froze a comunista");
        });
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ge_esquerda_3", timeoutTicks = 100)
    public static void jonesIsABossWithACommitteeAndAlliesAndSavesCooldowns(GameTestHelper h) {
        var p = arena(h);
        JonesManoelEntity jones = spawn(h, ModEntities.JONES_MANOEL.get(), p);
        CiroGomesEntity ciro = spawn(h, ModEntities.CIRO_GOMES.get(), p.add(3, 0, 0));
        var lula = spawn(h, ModEntities.LULA.get(), p.add(-3, 0, 0));
        Villager villager = comunista(h, p.add(0, 0, 3));
        guarded(h, p, () -> {
            h.assertTrue(jones.getMaxHealth() == ModConfig.JONES_HEALTH.get() && jones.bossBar() != null, "Jones is not a 300 HP boss");
            h.assertTrue(jones.faction() == Faction.ESQUERDA && jones.isAlliedTo(ciro) && ciro.isAlliedTo(jones) && jones.isAlliedTo(villager),
                    "Esquerda alliance missing");
            h.assertTrue(jones.isRival(lula) && !jones.isAlliedTo(lula), "Jones should be Lula's rival");
            h.assertTrue(jones.callCommittee() == 3, "Comitê Central did not call 3 comrades");
            var comrades = ConvictionPowers.protesters(jones);
            h.assertTrue(comrades.size() == 3 && comrades.stream().allMatch(c -> c.isAlliedTo(ciro) && ciro.isAlliedTo(c)), "Comrades not on the Esquerda side");
            h.assertTrue(jones.callCommittee() == 0, "Committee went over its size");
            jones.freeze(dummy(h, p.add(0, 0, -5)));
            JonesManoelEntity copy = ModEntities.JONES_MANOEL.get().create(h.getLevel());
            copy.load(jones.saveWithoutId(new CompoundTag()));
            h.assertTrue(copy.saveWithoutId(new CompoundTag()).getInt("JonesFreezeRemaining") > 0, "Freeze cooldown lost on save");
            copy.discard();
        });
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ge_esquerda_4", timeoutTicks = 100)
    public static void tabataTurnsAMobAgainstTheOtherEnemies(GameTestHelper h) {
        var p = arena(h);
        TabataAmaralEntity tabata = spawn(h, ModEntities.TABATA_AMARAL.get(), p);
        tabata.setNoAi(true);
        Husk near = dummy(h, p.add(3, 0, 0));
        Husk far = dummy(h, p.add(-6, 0, 0));
        var creeper = spawn(h, EntityType.CREEPER, p.add(0, 0, 2));
        creeper.setNoAi(true);
        guarded(h, p, () -> {
            var turned = tabata.turnVote();
            h.assertTrue(turned == near && near.getTarget() == far, "The nearest husk did not turn against the other: " + turned);
            h.assertTrue(tabata.hasTurned(near) && !tabata.isVillageThreat(near) && tabata.isVillageThreat(far), "Turned mob still a threat");
            h.assertTrue(!tabata.hasTurned(creeper), "A creeper changed sides");
        });
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ge_esquerda_5", timeoutTicks = 100)
    public static void tabataWithNoOneElseToFightKeepsHerPower(GameTestHelper h) {
        var p = arena(h);
        TabataAmaralEntity tabata = spawn(h, ModEntities.TABATA_AMARAL.get(), p);
        Husk only = dummy(h, p.add(3, 0, 0));
        guarded(h, p, () -> {
            h.assertTrue(tabata.turnVote() == null && !tabata.hasTurned(only), "Turned the only enemy, who has no one to fight");
            h.assertTrue(tabata.hurt(tabata.damageSources().mobAttack(only), 5) && tabata.getAbsorptionAmount() == 8, "Voto de consciência gave no shield");
            tabata.setAbsorptionAmount(0);
            tabata.invulnerableTime = 0;
            h.assertTrue(tabata.hurt(tabata.damageSources().mobAttack(only), 5) && tabata.getAbsorptionAmount() == 0, "Shield ignored its cooldown");
        });
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ge_esquerda_6", timeoutTicks = 100)
    public static void marinaRootsEnemiesAndVanishesWhenLow(GameTestHelper h) {
        var p = arena(h);
        MarinaSilvaEntity marina = spawn(h, ModEntities.MARINA_SILVA.get(), p);
        Husk husk = dummy(h, p.add(3, 0, 0));
        Villager villager = comunista(h, p.add(-3, 0, 0));
        guarded(h, p, () -> {
            h.assertTrue(marina.roots() == 1, "Roots should hold exactly the husk");
            h.assertTrue(husk.hasEffect(ModEffects.PARALYZED.get()) && !villager.hasEffect(ModEffects.PARALYZED.get()), "Roots missed the husk or hit the villager");
            husk.setTarget(marina);
            marina.setTarget(husk);
            marina.setHealth(13);
            marina.hurt(marina.damageSources().mobAttack(husk), 2);
            h.assertTrue(marina.isVanished() && marina.hasEffect(MobEffects.INVISIBILITY) && marina.hasEffect(MobEffects.REGENERATION),
                    "Marina did not vanish below 40%");
            h.assertTrue(marina.getTarget() == null && husk.getTarget() == null, "Fight not dropped on both sides");
        });
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ge_esquerda_7", timeoutTicks = 100)
    public static void ciroLosesHisTemperThenLeavesForParisAndComesBack(GameTestHelper h) {
        var p = arena(h);
        CiroGomesEntity ciro = spawn(h, ModEntities.CIRO_GOMES.get(), p);
        ciro.setNoAi(true);
        Husk husk = dummy(h, p.add(2, 0, 0));
        Husk attacker = dummy(h, p.add(-6, 0, 0));
        try {
            h.assertTrue(ciro.hurt(ciro.damageSources().mobAttack(attacker), 2), "Hit refused");
            h.assertTrue(ciro.hasEffect(MobEffects.DAMAGE_BOOST) && ciro.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 1
                    && ciro.hasEffect(MobEffects.MOVEMENT_SPEED), "Pavio curto gave no Strength II and Speed");
            h.assertTrue(husk.getDeltaMovement().x > .3, "Wave did not push the husk away: " + husk.getDeltaMovement());
            ciro.setHealth(15);
            ciro.invulnerableTime = 0;
            ciro.hurt(ciro.damageSources().mobAttack(attacker), 5);
            h.assertTrue(ciro.isInParis() && ciro.isInvisible() && ciro.isInvulnerable() && ciro.isActionLocked(), "Ciro did not leave for Paris below 30%");
            float health = ciro.getHealth();
            h.assertTrue(!ciro.hurt(ciro.damageSources().mobAttack(attacker), 5) && ciro.getHealth() == health, "Ciro was hurt in Paris");
            CompoundTag tag = ciro.saveWithoutId(new CompoundTag());
            tag.putInt("CiroParisRemaining", 3);
            CiroGomesEntity back = ModEntities.CIRO_GOMES.get().create(h.getLevel());
            back.load(tag);
            back.setUUID(java.util.UUID.randomUUID());
            back.moveTo(p.x, p.y, p.z + 4, 0, 0);
            h.getLevel().addFreshEntity(back);
            h.assertTrue(back.isInParis() && back.isInvisible(), "Paris trip lost on save");
            h.runAfterDelay(10, () -> guarded(h, p, () -> {
                h.assertTrue(!back.isInParis() && !back.isInvisible() && !back.isInvulnerable() && back.getHealth() >= back.getMaxHealth() / 2,
                        "Ciro did not come back from Paris with half his health");
                h.succeed();
            }));
        } catch (RuntimeException | Error failure) {
            cleanup(h, p);
            throw failure;
        }
    }

    @GameTest(template = "empty", batch = "ge_esquerda_8", timeoutTicks = 60)
    public static void comunistaEggGivesEverySkinAndBabiesStayComunistas(GameTestHelper h) {
        var p = arena(h);
        var egg = (ForgeSpawnEggItem) ModItems.COMUNISTA_SPAWN_EGG.get();
        h.assertTrue(egg.getType(null).spawn(h.getLevel(), new ItemStack(egg), null, BlockPos.containing(p), MobSpawnType.SPAWN_EGG, false, false)
                instanceof ComunistaEntity, "Egg did not spawn a comunista");
        for (int i = 0; i < 59; i++) spawn(h, ModEntities.COMUNISTA.get(), p.add(i % 8 - 4, 0, i / 8 - 4));
        h.runAfterDelay(3, () -> guarded(h, p, () -> {
            var villagers = h.getLevel().getEntitiesOfClass(Villager.class, new AABB(BlockPos.containing(p)).inflate(8));
            var jobs = villagers.stream().map(v -> v.getVillagerData().getProfession()).distinct().toList();
            h.assertTrue(villagers.size() == 60 && villagers.stream().allMatch(ModVillagers::isComunista), "Eggs did not all become comunistas: " + villagers.size());
            h.assertTrue(jobs.size() == 4 && jobs.containsAll(List.of(VillagerProfession.FARMER, VillagerProfession.NITWIT,
                    VillagerProfession.ARMORER, VillagerProfession.LIBRARIAN)), "Jobs: " + jobs);
            Villager mother = villagers.get(0);
            mother.setAge(6000);
            Villager baby = EntityType.VILLAGER.create(h.getLevel());
            baby.setAge(-24000);
            baby.moveTo(mother.getX(), mother.getY(), mother.getZ(), 0, 0);
            h.getLevel().addFreshEntityWithPassengers(baby);
            h.assertTrue(ModVillagers.isComunista(baby) && !ModVillagers.isPetista(baby), "Baby of a comunista is not a comunista");
            h.succeed();
        }));
    }
}
