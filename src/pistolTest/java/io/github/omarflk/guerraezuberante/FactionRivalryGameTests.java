package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Characters of different factions fight on sight; characters of the same faction stay allies. The rivalry is off for
 * every other batch: test structures sit a few blocks apart, and Lula in one test would attack Jair in the next.
 */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class FactionRivalryGameTests {
    @SubscribeEvent public static void isolateOtherTests(ServerStartedEvent event) { ModConfig.RIVALRY_ENABLED.set(false); }
    @BeforeBatch(batch = "ge_rivalidade") public static void enable(ServerLevel level) { ModConfig.RIVALRY_ENABLED.set(true); }
    @AfterBatch(batch = "ge_rivalidade") public static void disable(ServerLevel level) { ModConfig.RIVALRY_ENABLED.set(false); }

    private static Vec3 arena(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 40, 3));
        for (int x = -7; x <= 7; x++) for (int z = -10; z <= 12; z++) {
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

    /** Two characters 6 blocks apart must pick each other as target within 40 ticks. */
    private static void duel(GameTestHelper h, EntityType<? extends RetaliatingCharacterEntity> first,
                             EntityType<? extends RetaliatingCharacterEntity> second) {
        var p = arena(h);
        RetaliatingCharacterEntity a = spawn(h, first, p);
        RetaliatingCharacterEntity b = spawn(h, second, p.add(0, 0, 6));
        h.runAfterDelay(40, () -> {
            try {
                h.assertTrue(a.isRival(b) && b.isRival(a) && !a.isAlliedTo(b), "Not rivals: " + a.faction() + " x " + b.faction());
                h.assertTrue(a.getTarget() == b && b.getTarget() == a, first.getDescriptionId() + " -> " + a.getTarget()
                        + ", " + second.getDescriptionId() + " -> " + b.getTarget() + " alive=" + b.isAlive() + " hp=" + b.getHealth()
                        + " locked=" + b.isActionLocked() + " sees=" + b.getSensing().hasLineOfSight(a) + " lastHurtBy=" + b.getLastHurtByMob()
                        + " dist=" + Math.sqrt(a.distanceToSqr(b)));
            } finally { a.discard(); b.discard(); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_rivalidade", timeoutTicks = 100)
    public static void lulaAndJairFight(GameTestHelper h) { duel(h, ModEntities.LULA.get(), ModEntities.JAIR_BOLSONARO.get()); }

    @GameTest(template = "empty", batch = "ge_rivalidade", timeoutTicks = 100)
    public static void renanAndLulaFight(GameTestHelper h) { duel(h, ModEntities.RENAN_SANTOS.get(), ModEntities.LULA.get()); }

    @GameTest(template = "empty", batch = "ge_rivalidade", timeoutTicks = 100)
    public static void arthurAndLulaFight(GameTestHelper h) { duel(h, ModEntities.ARTHUR_COMBATENTE.get(), ModEntities.LULA.get()); }

    @GameTest(template = "empty", batch = "ge_rivalidade", timeoutTicks = 100)
    public static void arthurAndHaddadFight(GameTestHelper h) { duel(h, ModEntities.ARTHUR_COMBATENTE.get(), ModEntities.HADDAD.get()); }

    @GameTest(template = "empty", batch = "ge_rivalidade", timeoutTicks = 100)
    public static void flavioAndBoulosFight(GameTestHelper h) { duel(h, ModEntities.FLAVIO_BOLSONARO.get(), ModEntities.BOULOS.get()); }

    /** Characters placed close together must not attack each other for 60 ticks. */
    private static void peace(GameTestHelper h, Vec3 p, List<RetaliatingCharacterEntity> group, Runnable checks) {
        h.runAfterDelay(60, () -> {
            try {
                for (var c : group) h.assertTrue(c.getTarget() == null, c.getType().getDescriptionId() + " attacked " + c.getTarget());
                checks.run();
            } finally { group.forEach(Entity::discard); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_rivalidade", timeoutTicks = 100)
    public static void missaoStaysAllied(GameTestHelper h) {
        var p = arena(h);
        var renan = spawn(h, ModEntities.RENAN_SANTOS.get(), p);
        var arthur = spawn(h, ModEntities.ARTHUR_COMBATENTE.get(), p.add(0, 0, 3));
        peace(h, p, List.of(renan, arthur), () -> h.assertTrue(renan.isAlliedTo(arthur) && arthur.isAlliedTo(renan), "Renan and Arthur are not allies"));
    }

    @GameTest(template = "empty", batch = "ge_rivalidade", timeoutTicks = 100)
    public static void lulistasStayAlliedWithPetistas(GameTestHelper h) {
        var p = arena(h);
        var lula = spawn(h, ModEntities.LULA.get(), p);
        var haddad = spawn(h, ModEntities.HADDAD.get(), p.add(0, 0, 3));
        var boulos = spawn(h, ModEntities.BOULOS.get(), p.add(2, 0, 2));
        Villager petista = spawn(h, EntityType.VILLAGER, p.add(-2, 0, 2));
        ModVillagers.makePetista(petista);
        peace(h, p, List.of(lula, haddad, boulos), () -> {
            h.assertTrue(lula.isAlliedTo(haddad) && haddad.isAlliedTo(boulos) && boulos.isAlliedTo(lula), "Lulistas are not allies");
            h.assertTrue(lula.isAlliedTo(petista) && haddad.isAlliedTo(petista), "Petista is not a Lulista ally");
            petista.discard();
        });
    }

    @GameTest(template = "empty", batch = "ge_rivalidade", timeoutTicks = 100)
    public static void bolsonaristasStayAllied(GameTestHelper h) {
        var p = arena(h);
        var jair = spawn(h, ModEntities.JAIR_BOLSONARO.get(), p);
        var flavio = spawn(h, ModEntities.FLAVIO_BOLSONARO.get(), p.add(0, 0, 3));
        Villager petista = spawn(h, EntityType.VILLAGER, p.add(-2, 0, 2));
        ModVillagers.makePetista(petista);
        peace(h, p, List.of(jair, flavio), () -> {
            h.assertTrue(jair.isAlliedTo(flavio) && flavio.isAlliedTo(jair), "Jair and Flávio are not allies");
            h.assertTrue(!jair.isAlliedTo(petista), "Jair is allied to a petista");
            petista.discard();
        });
    }

    @GameTest(template = "empty", batch = "ge_rivalidade", timeoutTicks = 100)
    public static void rivalsBeyondRangeIgnoreEachOther(GameTestHelper h) {
        var p = arena(h);
        var lula = spawn(h, ModEntities.LULA.get(), p.add(0, 0, -10));
        var jair = spawn(h, ModEntities.JAIR_BOLSONARO.get(), p.add(0, 0, 12));
        peace(h, p, List.of(lula, jair), () -> h.assertTrue(lula.isRival(jair), "Lula and Jair are not rivals"));
    }
}
