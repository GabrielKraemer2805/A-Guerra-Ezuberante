package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.BoulosEntity;
import io.github.omarflk.guerraezuberante.entity.HaddadEntity;
import io.github.omarflk.guerraezuberante.entity.PetistaEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.util.FakePlayerFactory;
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

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class LulistaAllyGameTests {
    /** Hits taken by watched entities: game time and the amount before armor. */
    private static final Map<UUID, List<long[]>> HITS = new HashMap<>();

    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        var hits = HITS.get(event.getEntity().getUUID());
        if (hits != null && event.getSource().getEntity() instanceof HaddadEntity) {
            hits.add(new long[]{event.getEntity().level().getGameTime(), Math.round(event.getAmount() * 100)});
        }
    }

    private static Vec3 arena(GameTestHelper h, int height) {
        var p = h.absolutePos(new BlockPos(3, height, 3));
        for (int x = -7; x <= 7; x++) for (int z = -7; z <= 12; z++) {
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
    /** Husk: hostile, does not burn in daylight. Big health so the whole combo lands. */
    private static Husk dummy(GameTestHelper h, Vec3 at) {
        Husk husk = spawn(h, EntityType.HUSK, at);
        husk.setNoAi(true);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        husk.setHealth(1024);
        return husk;
    }
    private static ServerPlayer player(GameTestHelper h, String name, Faction faction) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL);
        if (faction != null) Factions.choose(player, faction);
        return player;
    }

    @GameTest(template = "empty", batch = "ge_lulistas", timeoutTicks = 200)
    public static void haddadSlapsThreeTimesForThreeDamageAndParalyses(GameTestHelper h) {
        var p = arena(h, 30);
        HaddadEntity haddad = spawn(h, ModEntities.HADDAD.get(), p);
        Husk husk = dummy(h, p.add(0, 0, 1.6));
        HITS.put(husk.getUUID(), new ArrayList<>());
        h.runAfterDelay(70, () -> {
            var hits = HITS.remove(husk.getUUID());
            try {
                h.assertTrue(hits.size() >= 3, "Haddad did not attack the husk on his own: " + hits.size() + " hits");
                long slap = Math.round(ModConfig.SLAP_DAMAGE.get() * 100);
                for (int i = 0; i < 3; i++) h.assertTrue(hits.get(i)[1] == slap, "Slap " + i + " dealt " + hits.get(i)[1] / 100.0);
                h.assertTrue(hits.get(1)[0] - hits.get(0)[0] == 12 && hits.get(2)[0] - hits.get(1)[0] == 12,
                        "Slaps not 12 ticks apart: " + hits.get(0)[0] + "," + hits.get(1)[0] + "," + hits.get(2)[0]);
                h.assertTrue(husk.hasEffect(ModEffects.PARALYZED.get()), "Target not paralysed");
                h.assertTrue(husk.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "Paralysed target can still walk");
                System.out.println("HADDAD_SLAPS " + hits.stream().map(hit -> hit[0] + ":" + hit[1] / 100.0).toList());
            } finally { haddad.discard(); husk.discard(); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_lulistas", timeoutTicks = 200)
    public static void slapThatTakesNoHealthDoesNotParalyse(GameTestHelper h) {
        var p = arena(h, 30);
        HaddadEntity haddad = spawn(h, ModEntities.HADDAD.get(), p);
        Husk husk = dummy(h, p.add(0, 0, 1.6));
        husk.setInvulnerable(true);
        h.runAfterDelay(60, () -> {
            try {
                h.assertTrue(husk.getHealth() == 1024 && !husk.hasEffect(ModEffects.PARALYZED.get()), "Blocked slap still paralysed");
            } finally { haddad.discard(); husk.discard(); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_lulistas")
    public static void alliesDefendAgainstRivalsButSpareLulistasAndCreepers(GameTestHelper h) {
        var p = arena(h, 30);
        BoulosEntity boulos = spawn(h, ModEntities.BOULOS.get(), p);
        HaddadEntity haddad = spawn(h, ModEntities.HADDAD.get(), p.add(2, 0, 0));
        var lula = spawn(h, ModEntities.LULA.get(), p.add(-2, 0, 0));
        Villager petista = spawn(h, EntityType.VILLAGER, p.add(0, 0, 3));
        ModVillagers.makePetista(petista);
        Villager plain = spawn(h, EntityType.VILLAGER, p.add(1, 0, 3));
        var creeper = spawn(h, EntityType.CREEPER, p.add(3, 0, 3));
        var zombie = spawn(h, EntityType.HUSK, p.add(-3, 0, 3));
        var cow = spawn(h, EntityType.COW, p.add(0, 0, 6));
        ServerPlayer rival = player(h, "Rival", Faction.BOLSONARISTA);
        ServerPlayer comrade = player(h, "Comrade", Faction.LULISTA);
        ServerPlayer neutral = player(h, "Neutral", null);
        try {
            for (var ally : List.of(boulos, haddad)) {
                h.assertTrue(ally.isVillageThreat(rival), "Rival-faction player is not a threat");
                h.assertTrue(ally.isVillageThreat(zombie), "Hostile mob is not a threat");
                h.assertTrue(!ally.isVillageThreat(comrade) && !ally.isVillageThreat(neutral), "Lulista or faction-less player targeted");
                h.assertTrue(!ally.isVillageThreat(creeper) && !ally.isVillageThreat(cow), "Creeper or animal targeted");
                h.assertTrue(!ally.isVillageThreat(lula) && !ally.isVillageThreat(petista) && !ally.isVillageThreat(plain)
                        && !ally.isVillageThreat(ally == boulos ? haddad : boulos), "Ally or villager targeted");
                h.assertTrue(ally.isAlliedTo(petista), "Petista is not an ally");
            }
            h.assertTrue(lula.isAlliedTo(haddad) && lula.isAlliedTo(boulos) && haddad.isAlliedTo(lula), "Lula alliance missing");
            rival.setGameMode(GameType.CREATIVE);
            h.assertTrue(!boulos.isVillageThreat(rival), "Creative player targeted");
        } finally {
            for (var player : List.of(rival, comrade, neutral)) Factions.clear(player);
            for (Entity entity : List.of(boulos, haddad, lula, petista, plain, creeper, zombie, cow)) entity.discard();
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ge_lulistas", timeoutTicks = 60)
    public static void petistaEggMakesARealVillagerThatTradesAndBreedsPetistas(GameTestHelper h) {
        var p = arena(h, 30);
        var egg = (ForgeSpawnEggItem) ModItems.PETISTA_SPAWN_EGG.get();
        var spawned = egg.getType(null).spawn(h.getLevel(), new ItemStack(egg), null, BlockPos.containing(p), MobSpawnType.SPAWN_EGG, false, false);
        h.assertTrue(spawned instanceof PetistaEntity, "Egg did not spawn a petista");
        h.runAfterDelay(3, () -> {
            var area = new net.minecraft.world.phys.AABB(p, p).inflate(3);
            var villagers = h.getLevel().getEntitiesOfClass(Villager.class, area);
            Villager mother = null;
            try {
                h.assertTrue(h.getLevel().getEntitiesOfClass(PetistaEntity.class, area).isEmpty(), "Old petista entity was not converted");
                h.assertTrue(villagers.size() == 1 && villagers.get(0).getType() == EntityType.VILLAGER
                        && ModVillagers.isPetista(villagers.get(0)), "Egg did not become a petista villager: " + villagers);
                mother = villagers.get(0);
                var job = mother.getVillagerData().getProfession();
                h.assertTrue(job != VillagerProfession.NONE && (job == VillagerProfession.NITWIT || mother.getVillagerXp() > 0),
                        "Egg petista has no lasting job: " + job + " xp " + mother.getVillagerXp());
                mother.setVillagerData(mother.getVillagerData().setProfession(VillagerProfession.FARMER).setLevel(1));
                h.assertTrue(!mother.getOffers().isEmpty(), "Farmer petista has no trades");
                CompoundTag tag = mother.saveWithoutId(new CompoundTag());
                Villager copy = EntityType.VILLAGER.create(h.getLevel());
                copy.load(tag);
                h.assertTrue(ModVillagers.isPetista(copy) && copy.getOffers().size() == mother.getOffers().size(), "Type or trades lost on save");
                copy.discard();
                // Same steps as VillagerMakeLove.breed: the parent gets age 6000, the baby (age -24000) appears on it.
                mother.setAge(6000);
                Villager baby = EntityType.VILLAGER.create(h.getLevel());
                baby.setAge(-24000);
                baby.moveTo(mother.getX(), mother.getY(), mother.getZ(), 0, 0);
                h.getLevel().addFreshEntityWithPassengers(baby);
                h.assertTrue(ModVillagers.isPetista(baby), "Baby of a petista is not a petista");
                Villager stranger = EntityType.VILLAGER.create(h.getLevel());
                stranger.setAge(-24000);
                stranger.moveTo(p.x + 2.5, p.y, p.z, 0, 0);
                h.getLevel().addFreshEntityWithPassengers(stranger);
                h.assertTrue(!ModVillagers.isPetista(stranger), "Baby away from petistas became a petista");
            } finally {
                h.getLevel().getEntitiesOfClass(Villager.class, area).forEach(Entity::discard);
            }
            h.succeed();
        });
    }

    /** Each egg petista gets one job per skin group, so 90 eggs show all six skins (chance of missing one: 4e-7). */
    @GameTest(template = "empty", batch = "ge_lulistas", timeoutTicks = 60)
    public static void petistaEggGivesEverySkin(GameTestHelper h) {
        var p = arena(h, 30);
        for (int i = 0; i < 90; i++) spawn(h, ModEntities.PETISTA.get(), p.add(i % 9 - 4, 0, i / 9 - 4));
        h.runAfterDelay(3, () -> {
            var area = new net.minecraft.world.phys.AABB(p, p).inflate(8);
            var villagers = h.getLevel().getEntitiesOfClass(Villager.class, area);
            try {
                var jobs = villagers.stream().map(v -> v.getVillagerData().getProfession()).distinct().toList();
                h.assertTrue(villagers.size() == 90 && villagers.stream().allMatch(ModVillagers::isPetista), "Eggs did not all become petistas");
                h.assertTrue(jobs.size() == 6 && jobs.containsAll(List.of(VillagerProfession.FARMER, VillagerProfession.LIBRARIAN,
                        VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.MASON, VillagerProfession.NITWIT)), "Jobs: " + jobs);
            } finally { villagers.forEach(Entity::discard); }
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "ge_lulistas")
    public static void alliesPatrolLikeGolemsAndKeepTheirHome(GameTestHelper h) {
        var p = arena(h, 30);
        HaddadEntity haddad = spawn(h, ModEntities.HADDAD.get(), p);
        try {
            BlockPos home = BlockPos.containing(p);
            haddad.setHome(home);
            HaddadEntity copy = ModEntities.HADDAD.get().create(h.getLevel());
            copy.load(haddad.saveWithoutId(new CompoundTag()));
            h.assertTrue(home.equals(copy.home()) && copy.hasRestriction() && copy.getRestrictCenter().equals(home)
                    && copy.getRestrictRadius() == io.github.omarflk.guerraezuberante.entity.LulistaAllyEntity.HOME_RADIUS, "Home lost on save");
            copy.discard();
        } finally { haddad.discard(); }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "ge_lulistas")
    public static void slapCooldownSurvivesSaveAndLoad(GameTestHelper h) {
        HaddadEntity haddad = ModEntities.HADDAD.get().create(h.getLevel());
        CompoundTag tag = new CompoundTag();
        tag.putInt("HaddadSlapRemaining", 150);
        haddad.load(haddad.saveWithoutId(new CompoundTag()).merge(tag));
        CompoundTag again = haddad.saveWithoutId(new CompoundTag());
        h.assertTrue(again.getInt("HaddadSlapRemaining") == 150, "Cooldown lost: " + again.getInt("HaddadSlapRemaining"));
        h.assertTrue(haddad.getMaxHealth() == ModConfig.HADDAD_HEALTH.get(), "Haddad health is not the configured value");
        haddad.discard();
        h.succeed();
    }
}
