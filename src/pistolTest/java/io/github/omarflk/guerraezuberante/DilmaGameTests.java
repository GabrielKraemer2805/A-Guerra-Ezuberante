package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.DilmaEntity;
import io.github.omarflk.guerraezuberante.entity.MandiocaProjectile;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import io.github.omarflk.guerraezuberante.village.FactionCastlePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dilma: mandioca bursts, Estocar vento (push or Sem ar) and her place among the Lulistas. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class DilmaGameTests {
    /** Damage taken by watched entities, with the kind of damage. */
    private static final Map<UUID, List<String>> HITS = new HashMap<>();

    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        var hits = HITS.get(event.getEntity().getUUID());
        if (hits != null) {
            String source = event.getSource().getDirectEntity() instanceof MandiocaProjectile ? "mandioca" : event.getSource().getMsgId();
            hits.add(source + ":" + event.getAmount());
        }
    }

    /** High in the open air: lower down the test area can cut into natural caves, whose water or lava flows in. */
    private static Vec3 arena(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 200, 3));
        for (int x = -8; x <= 8; x++) for (int z = -6; z <= 16; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 5; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }

    private static <T extends Entity> T spawn(GameTestHelper h, EntityType<T> type, Vec3 at) {
        T entity = type.create(h.getLevel());
        entity.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addFreshEntity(entity);
        return entity;
    }

    /**
     * Husk: hostile (a village threat), does not burn in daylight and cannot breathe underwater. Speed 0 instead of no AI,
     * so it stays put but still slides when pushed.
     */
    private static Husk husk(GameTestHelper h, Vec3 at) {
        Husk husk = spawn(h, EntityType.HUSK, at);
        husk.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
        husk.setHealth(1024);
        return husk;
    }

    /** A Dilma whose next Estocar vento is far away, so only the mandiocas fly. */
    private static DilmaEntity dilma(GameTestHelper h, Vec3 at, int windDelay) {
        DilmaEntity dilma = spawn(h, ModEntities.DILMA.get(), at);
        CompoundTag tag = new CompoundTag();
        tag.putInt("DilmaWindRemaining", windDelay);
        dilma.load(dilma.saveWithoutId(new CompoundTag()).merge(tag));
        return dilma;
    }

    @GameTest(template = "empty", batch = "ge_dilma", timeoutTicks = 120)
    public static void mandiocaBurstOfThreeFromHerHand(GameTestHelper h) {
        var p = arena(h);
        DilmaEntity dilma = dilma(h, p, 72000);
        dilma.setNoAi(true);
        Husk husk = husk(h, p.add(0, 0, 7));
        HITS.put(husk.getUUID(), new ArrayList<>());
        dilma.setTarget(husk);
        h.assertTrue(dilma.beginVolley(husk), "No burst started");
        h.assertTrue(!dilma.beginVolley(husk), "Second burst before the interval");
        h.runAfterDelay(DilmaEntity.ATTACK_IMPACT + 1, () -> {
            var flying = h.getLevel().getEntitiesOfClass(MandiocaProjectile.class, new AABB(p, p).inflate(10), m -> m.getOwner() == dilma);
            h.assertTrue(flying.size() == ModConfig.MANDIOCA_COUNT.get(), "Burst of " + flying.size() + " mandiocas");
        });
        h.runAfterDelay(60, () -> {
            var hits = HITS.remove(husk.getUUID());
            try {
                h.assertTrue(hits.stream().anyMatch(hit -> hit.equals("mandioca:" + ModConfig.MANDIOCA_DAMAGE.get().floatValue())),
                        "No mandioca hit for " + ModConfig.MANDIOCA_DAMAGE.get() + ": " + hits);
                h.assertTrue(!dilma.isActionLocked() && dilma.windToken() < 0, "Wind cast while it was on cooldown");
            } finally { dilma.discard(); husk.discard(); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_dilma", timeoutTicks = 120)
    public static void estocarVentoPushesEnemiesAndSparesPetistas(GameTestHelper h) {
        var p = arena(h);
        DilmaEntity dilma = spawn(h, ModEntities.DILMA.get(), p);
        Husk husk = husk(h, p.add(0, 0, 6));
        Villager petista = spawn(h, EntityType.VILLAGER, p.add(.8, 0, 4));
        ModVillagers.makePetista(petista);
        petista.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        dilma.setTarget(husk);
        h.assertTrue(dilma.beginWind(husk, false), "Estocar vento did not start");
        h.assertTrue(dilma.isActionLocked() && !dilma.beginWind(husk, false), "Second cast during the first one");
        double huskStart = husk.position().z, petistaStart = petista.position().z;
        h.runAfterDelay(DilmaEntity.WIND_CAST - 1, () -> h.assertTrue(husk.position().z - huskStart < .2, "Pushed before the cast ended"));
        h.runAfterDelay(DilmaEntity.WIND_CAST + 12, () -> {
            try {
                h.assertTrue(husk.position().z - huskStart > 1.5, "Husk was not pushed away: moved " + (husk.position().z - huskStart));
                h.assertTrue(Math.abs(petista.position().z - petistaStart) < .2 && !petista.hasEffect(ModEffects.SEM_AR.get()),
                        "The wind hit a petista: moved " + (petista.position().z - petistaStart) + ", sem ar " + petista.hasEffect(ModEffects.SEM_AR.get())
                        + ", hurt by " + petista.getLastHurtByMob() + ", alive " + petista.isAlive() + ", at " + petista.position());
                h.assertTrue(!dilma.isActionLocked(), "Dilma still locked after the cast");
            } finally { dilma.discard(); husk.discard(); petista.discard(); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_dilma", timeoutTicks = 160)
    public static void estocarVentoTakesTheAirAndDrowns(GameTestHelper h) {
        var p = arena(h);
        DilmaEntity dilma = spawn(h, ModEntities.DILMA.get(), p);
        dilma.setNoAi(true);
        // Undead mobs (zombies, husks) breathe underwater in vanilla and are immune; an illager is not.
        var illager = spawn(h, EntityType.VINDICATOR, p.add(0, 0, 6));
        illager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        illager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
        illager.setHealth(1024);
        HITS.put(illager.getUUID(), new ArrayList<>());
        h.assertTrue(dilma.beginWind(illager, true), "Estocar vento did not start");
        h.runAfterDelay(DilmaEntity.WIND_CAST + 25, () -> h.assertTrue(illager.hasEffect(ModEffects.SEM_AR.get()) && illager.getAirSupply() <= 0,
                "No Sem ar: effect " + illager.hasEffect(ModEffects.SEM_AR.get()) + ", air " + illager.getAirSupply()));
        h.runAfterDelay(DilmaEntity.WIND_CAST + 70, () -> {
            var hits = HITS.remove(illager.getUUID());
            try {
                h.assertTrue(hits.stream().anyMatch(hit -> hit.startsWith("drown:")), "No drowning damage: " + hits);
                h.assertTrue(Math.abs(illager.position().z - (p.z + 6)) < .2, "Sem ar also pushed");
            } finally { dilma.discard(); illager.discard(); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_dilma", timeoutTicks = 120)
    public static void beamWithLulaHealsHimAndDoublesHerMandiocas(GameTestHelper h) {
        var p = arena(h);
        DilmaEntity dilma = dilma(h, p, 72000);
        dilma.setNoAi(true);
        var lula = spawn(h, ModEntities.LULA.get(), p.add(5, 0, 0));
        lula.setNoAi(true);
        lula.setHealth(100);
        Husk husk = husk(h, p.add(0, 0, 7));
        HITS.put(husk.getUUID(), new ArrayList<>());
        h.runAfterDelay(25, () -> {
            h.assertTrue(dilma.isLinked() && dilma.linkedLula() == lula, "No beam to Lula 5 blocks away");
            h.assertTrue(lula.getHealth() > 100, "Lula was not healed: " + lula.getHealth());
            dilma.setTarget(husk);
            h.assertTrue(dilma.beginVolley(husk), "No burst started");
        });
        h.runAfterDelay(85, () -> {
            var hits = HITS.remove(husk.getUUID());
            try {
                float doubled = (float) (ModConfig.MANDIOCA_DAMAGE.get() * ModConfig.LINK_MULTIPLIER.get());
                h.assertTrue(hits.stream().anyMatch(hit -> hit.equals("mandioca:" + doubled)), "No doubled mandioca (" + doubled + "): " + hits);
            } finally { dilma.discard(); lula.discard(); husk.discard(); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_dilma", timeoutTicks = 60)
    public static void noBeamThroughWallsOrFromAfar(GameTestHelper h) {
        var p = arena(h);
        DilmaEntity walled = spawn(h, ModEntities.DILMA.get(), p.add(-5, 0, 0));
        DilmaEntity far = spawn(h, ModEntities.DILMA.get(), p.add(0, 0, 15));
        var lula = spawn(h, ModEntities.LULA.get(), p);
        for (var dilma : List.of(walled, far)) dilma.setNoAi(true);
        lula.setNoAi(true);
        for (int z = -2; z <= 2; z++) for (int y = 0; y < 4; y++) h.getLevel().setBlockAndUpdate(BlockPos.containing(p.add(-2.5, y, z)), Blocks.STONE.defaultBlockState());
        h.runAfterDelay(25, () -> {
            try {
                h.assertTrue(!walled.isLinked(), "Beam through a wall");
                h.assertTrue(!far.isLinked(), "Beam from 15 blocks");
            } finally { walled.discard(); far.discard(); lula.discard(); }
            h.succeed();
        });
    }

    /** A small walled hall with an iron double door, laid out like the castle gate: Dilma has to open it to save Lula. */
    @GameTest(template = "empty", batch = "ge_dilma", timeoutTicks = 400)
    public static void villageDilmaOpensTheIronDoorToDefendLula(GameTestHelper h) {
        var p = arena(h);
        BlockPos o = BlockPos.containing(p);
        var level = h.getLevel();
        for (int x = -4; x <= 4; x++) for (int z = 3; z <= 11; z++) for (int y = 0; y <= 4; y++) {
            boolean wall = x == -4 || x == 4 || z == 3 || z == 11 || y == 4;
            level.setBlockAndUpdate(o.offset(x, y, z), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        var door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        for (int x : new int[]{0, 1}) {
            var hinge = x == 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            level.setBlock(o.offset(x, 0, 3), door.setValue(DoorBlock.HINGE, hinge).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
            level.setBlock(o.offset(x, 1, 3), door.setValue(DoorBlock.HINGE, hinge).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        }
        var gate = new FactionCastlePiece.Gate(o.offset(0, 0, 3), o.offset(1, 0, 3), o.offset(0, 0, 1),
                o.offset(0, 0, 5), o.offset(0, 0, 7), BoundingBox.fromCorners(o.offset(-3, 0, 4), o.offset(3, 3, 10)));
        DilmaEntity dilma = dilma(h, p.add(0, 0, -5), 72000);
        dilma.setCastle(gate);
        var lula = spawn(h, ModEntities.LULA.get(), p.add(-2, 0, 9));
        lula.setNoAi(true);
        Husk husk = husk(h, p.add(2, 0, 9));
        h.runAfterDelay(5, () -> {
            h.assertTrue(!gate.hall().isInside(dilma.blockPosition()), "Dilma started inside");
            lula.hurt(h.getLevel().damageSources().mobAttack(husk), 1);
        });
        h.runAfterDelay(25, () -> h.assertTrue(dilma.isRescuing(), "Dilma did not notice the attack on Lula"));
        h.runAfterDelay(300, () -> {
            try {
                h.assertTrue(gate.hall().isInside(dilma.blockPosition()), "Dilma did not get into the hall: " + dilma.blockPosition());
                h.assertTrue(dilma.getTarget() == husk, "Dilma is not fighting Lula's attacker: " + dilma.getTarget());
            } finally { dilma.discard(); lula.discard(); husk.discard(); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_dilma")
    public static void castlePostSurvivesSaveAndLoad(GameTestHelper h) {
        DilmaEntity dilma = ModEntities.DILMA.get().create(h.getLevel());
        var gate = new FactionCastlePiece.Gate(new BlockPos(1, 2, 3), new BlockPos(2, 2, 3), new BlockPos(1, 2, 1),
                new BlockPos(1, 2, 5), new BlockPos(1, 2, 12), BoundingBox.fromCorners(new BlockPos(-8, 1, 4), new BlockPos(10, 9, 22)));
        dilma.setCastle(gate);
        DilmaEntity copy = ModEntities.DILMA.get().create(h.getLevel());
        copy.load(dilma.saveWithoutId(new CompoundTag()));
        try {
            h.assertTrue(gate.equals(copy.castle()), "Castle post lost: " + copy.castle());
            h.assertTrue(gate.center().equals(copy.home()), "Home is not the castle");
        } finally { dilma.discard(); copy.discard(); }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ge_dilma")
    public static void dilmaIsLulistaAndKeepsHerCooldowns(GameTestHelper h) {
        DilmaEntity dilma = ModEntities.DILMA.get().create(h.getLevel());
        var lula = ModEntities.LULA.get().create(h.getLevel());
        var jair = ModEntities.JAIR_BOLSONARO.get().create(h.getLevel());
        try {
            h.assertTrue(dilma.faction() == Faction.LULISTA && dilma.isAlliedTo(lula) && lula.isAlliedTo(dilma) && dilma.isRival(jair),
                    "Dilma is not a Lulista ally");
            h.assertTrue(dilma.getMaxHealth() == ModConfig.DILMA_HEALTH.get(), "Health is not the configured value");
            CompoundTag tag = new CompoundTag();
            tag.putInt("DilmaWindRemaining", 150);
            tag.putInt("DilmaVolleyRemaining", 20);
            dilma.load(dilma.saveWithoutId(new CompoundTag()).merge(tag));
            CompoundTag again = dilma.saveWithoutId(new CompoundTag());
            h.assertTrue(again.getInt("DilmaWindRemaining") == 150 && again.getInt("DilmaVolleyRemaining") == 20,
                    "Cooldowns lost: " + again.getInt("DilmaWindRemaining") + "/" + again.getInt("DilmaVolleyRemaining"));
        } finally { dilma.discard(); lula.discard(); jair.discard(); }
        h.succeed();
    }
}
