package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.FazOLProjectile;
import io.github.omarflk.guerraezuberante.entity.LulaEntity;
import io.github.omarflk.guerraezuberante.entity.ManifestanteEntity;
import io.github.omarflk.guerraezuberante.faction.ConvictionPowers;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Lula's boss powers: Faz o L and Manifestação, triggered by the hits he takes. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LulaPowersGameTests {
    /** High in the open air: lower down the test area can cut into natural caves, whose water or lava flows in. */
    private static Vec3 arena(GameTestHelper h) { return arena(h, 200); }

    private static Vec3 arena(GameTestHelper h, int height) {
        var p = h.absolutePos(new BlockPos(3, height, 3));
        for (int x = -9; x <= 9; x++) for (int z = -9; z <= 12; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }

    private static <T extends Entity> T spawn(GameTestHelper h, EntityType<T> type, Vec3 at) {
        T entity = type.create(h.getLevel());
        entity.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addFreshEntity(entity);
        return entity;
    }

    /** Husk: hostile, does not burn in daylight, big health and no AI so it stays where it is. */
    private static Husk attacker(GameTestHelper h, Vec3 at) {
        Husk husk = spawn(h, EntityType.HUSK, at);
        husk.setNoAi(true);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
        husk.setHealth(1024);
        return husk;
    }

    private static void hit(GameTestHelper h, LulaEntity lula, Husk husk, int times) {
        for (int i = 0; i < times; i++) {
            lula.invulnerableTime = 0;
            lula.hurt(h.getLevel().damageSources().mobAttack(husk), 1);
        }
    }

    /** Removes what the test made: the given entities, Lula's crowd and any L still flying. */
    private static void cleanup(GameTestHelper h, Vec3 p, LulaEntity lula, Entity... others) {
        ConvictionPowers.protesters(lula).forEach(Entity::discard);
        h.getLevel().getEntitiesOfClass(FazOLProjectile.class, new AABB(p, p).inflate(12), l -> l.getOwner() == lula).forEach(Entity::discard);
        lula.discard();
        for (Entity other : others) { other.discard(); }
    }

    @GameTest(template = "empty", batch = "ge_lula_poderes", timeoutTicks = 120)
    public static void fazOLHitsTheAttackerAndSparesAllies(GameTestHelper h) {
        var p = arena(h);
        // Blocks stay, so the petista standing by is not dropped by the crater: this test is only about who gets hurt.
        boolean blocks = ModConfig.LULA_FAZ_O_L_BREAK_BLOCKS.get();
        ModConfig.LULA_FAZ_O_L_BREAK_BLOCKS.set(false);
        LulaEntity lula = spawn(h, ModEntities.LULA.get(), p);
        lula.setNoAi(true);
        Husk husk = attacker(h, p.add(0, 0, 6));
        Villager petista = spawn(h, EntityType.VILLAGER, p.add(1.5, 0, 6));
        ModVillagers.makePetista(petista);
        petista.setNoAi(true);
        // Taken before the throw: the blast pushes the husk away.
        var below = BlockPos.containing(husk.position()).below();
        h.assertTrue(lula.throwFazOL(husk), "Lula did not throw the Faz o L");
        h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(lula.getYRot())) < 1 && lula.fazOLToken() == h.getLevel().getGameTime(),
                "Lula did not turn to the target with the arm pose: yaw " + lula.getYRot());
        h.runAfterDelay(40, () -> {
            try {
                h.assertTrue(husk.getHealth() <= 1024 - 5, "The L did not hurt the attacker: " + husk.getHealth());
                h.assertTrue(petista.isAlive() && petista.getHealth() == petista.getMaxHealth(), "The L hurt a petista");
                h.assertTrue(lula.getHealth() == lula.getMaxHealth(), "The L hurt Lula");
                h.assertTrue(h.getLevel().getBlockState(below).is(Blocks.STONE), "Lula's L broke blocks with faz_o_l_breaks_blocks off");
            } finally { ModConfig.LULA_FAZ_O_L_BREAK_BLOCKS.set(blocks); cleanup(h, p, lula, husk, petista); }
            h.succeed();
        });
    }

    /**
     * Alone in its batch, high up and without block drops: the debris of a power-7 blast otherwise rains on the arenas
     * of the next batches (the Jair summon test that runs right after it failed with drops on, and passed without this test).
     */
    @GameTest(template = "empty", batch = "ge_lula_explosao", timeoutTicks = 120)
    public static void fazOLBlowsUpTheBlocksAround(GameTestHelper h) {
        var p = arena(h, 230);
        var drops = h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
        boolean dropped = drops.get();
        drops.set(false, h.getLevel().getServer());
        LulaEntity lula = spawn(h, ModEntities.LULA.get(), p);
        lula.setNoAi(true);
        Husk husk = attacker(h, p.add(0, 0, 8));
        var floor = BlockPos.containing(husk.position()).below();
        h.assertTrue(ModConfig.LULA_FAZ_O_L_BREAK_BLOCKS.get(), "Lula's L should break blocks by default");
        h.assertTrue(lula.throwFazOL(husk), "Lula did not throw the Faz o L");
        h.runAfterDelay(40, () -> {
            try {
                h.assertTrue(!h.getLevel().getBlockState(floor).is(Blocks.STONE), "The L did not blow up the floor");
            } finally { drops.set(dropped, h.getLevel().getServer()); cleanup(h, p, lula, husk); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_lula_poderes", timeoutTicks = 60)
    public static void fazOLComesOnTheTenthHit(GameTestHelper h) {
        var p = arena(h);
        LulaEntity lula = spawn(h, ModEntities.LULA.get(), p);
        Husk husk = attacker(h, p.add(0, 0, 6));
        double chance = ModConfig.LULA_FAZ_O_L_CHANCE.get(), protest = ModConfig.LULA_PROTEST_CHANCE.get();
        try {
            ModConfig.LULA_FAZ_O_L_CHANCE.set(1D);
            ModConfig.LULA_PROTEST_CHANCE.set(0D);
            var area = new AABB(p, p).inflate(16);
            hit(h, lula, husk, 9);
            h.assertTrue(h.getLevel().getEntitiesOfClass(FazOLProjectile.class, area).isEmpty(), "Faz o L before the 10th hit");
            hit(h, lula, husk, 1);
            h.assertTrue(lula.hitsTaken() == 10, "Hits counted: " + lula.hitsTaken());
            h.assertTrue(h.getLevel().getEntitiesOfClass(FazOLProjectile.class, area).size() == 1, "No Faz o L on the 10th hit");
            h.assertTrue(ConvictionPowers.protesters(lula).isEmpty(), "Manifestação with chance 0");
        } finally {
            ModConfig.LULA_FAZ_O_L_CHANCE.set(chance);
            ModConfig.LULA_PROTEST_CHANCE.set(protest);
            cleanup(h, p, lula, husk);
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ge_lula_poderes", timeoutTicks = 80)
    public static void manifestacaoOnTheSeventhHitFightsForLula(GameTestHelper h) {
        var p = arena(h);
        LulaEntity lula = spawn(h, ModEntities.LULA.get(), p);
        Husk husk = attacker(h, p.add(0, 0, 8));
        var haddad = spawn(h, ModEntities.HADDAD.get(), p.add(-3, 0, 0));
        double chance = ModConfig.LULA_FAZ_O_L_CHANCE.get(), protest = ModConfig.LULA_PROTEST_CHANCE.get();
        try {
            ModConfig.LULA_FAZ_O_L_CHANCE.set(0D);
            ModConfig.LULA_PROTEST_CHANCE.set(1D);
            hit(h, lula, husk, 6);
            h.assertTrue(ConvictionPowers.protesters(lula).isEmpty(), "Manifestação before the 7th hit");
            hit(h, lula, husk, 1);
        } finally {
            ModConfig.LULA_FAZ_O_L_CHANCE.set(chance);
            ModConfig.LULA_PROTEST_CHANCE.set(protest);
        }
        var crowd = ConvictionPowers.protesters(lula);
        h.assertTrue(crowd.size() == ModConfig.PROTEST_COUNT.get(), "Crowd size " + crowd.size());
        h.runAfterDelay(30, () -> {
            try {
                for (ManifestanteEntity zombie : crowd) {
                    h.assertTrue(zombie.getOwner() == lula && zombie.isAlliedTo(lula) && lula.isAlliedTo(zombie) && haddad.isAlliedTo(zombie),
                            "Crowd is not on Lula's side");
                    h.assertTrue(zombie.getTarget() != lula && zombie.getTarget() != haddad, "Crowd turned on a Lulista");
                }
                h.assertTrue(haddad.getTarget() == null || !(haddad.getTarget() instanceof ManifestanteEntity), "Haddad attacked Lula's crowd");
                h.assertTrue(crowd.stream().anyMatch(zombie -> zombie.getTarget() == husk), "Crowd does not fight Lula's attacker");
            } finally { cleanup(h, p, lula, husk, haddad); }
            h.succeed();
        });
    }
}
