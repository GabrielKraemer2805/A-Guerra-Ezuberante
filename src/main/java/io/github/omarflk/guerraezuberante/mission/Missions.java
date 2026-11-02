package io.github.omarflk.guerraezuberante.mission;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.entity.TowerGuardEntity;
import io.github.omarflk.guerraezuberante.faction.ConvictionPowers;
import io.github.omarflk.guerraezuberante.village.FactionVillageStructure;
import io.github.omarflk.guerraezuberante.village.FactionVillages;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.faction.Figure;
import io.github.omarflk.guerraezuberante.progression.Morality;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import io.github.omarflk.guerraezuberante.village.FactionBanners;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

/**
 * Generals, the party story and what it does to the moral bar. Server authoritative. Each party keeps its own place in
 * the story on the player, so a change of faction resumes the old story when coming back.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class Missions {
    private static final String DATA = "GuerraEzuberanteMissions", INDEX = "Index", ACTIVE = "Active", PROGRESS = "Progress", DONE = "Done";
    /** After the story the general hands out the missions from this one on, as routine work. */
    private static final int ROUTINE_FROM = 2;
    private static final double TALK_RANGE = 8;

    public static final int ACCEPT = 0, TURN_IN = 1, ABANDON = 2, RECRUIT_MILITIA = 3, RECRUIT_SOLDIER = 4;
    private static final String TARGET_FACTION = "TargetFaction", TARGET_X = "TargetX", TARGET_Z = "TargetZ";
    /** How far from the middle of the target village a kill counts: the castle and base are both within this. */
    public static final double CASTLE_RADIUS = 110;

    /** The general of a party. Jones Manoel stays on the Esquerda's throne as its leader; Ciro runs the base. */
    public static Optional<EntityType<?>> general(Faction faction) {
        EntityType<?> type = switch (faction) {
            case LULISTA -> ModEntities.figure(Figure.ANDRE_JANONES).get();
            case BOLSONARISTA -> ModEntities.figure(Figure.TARCISIO_FREITAS).get();
            case MISSAO -> ModEntities.figure(Figure.KIM_KATAGUIRI).get();
            case ESQUERDA -> ModEntities.CIRO_GOMES.get();
            case DIREITA -> ModEntities.RONALDO_CAIADO.get();
            case CENTRO -> ModEntities.CENTRO_OFFICER.get();
        };
        return Optional.ofNullable(type);
    }

    /** The party whose general this entity type is. */
    public static Optional<Faction> partyOfGeneral(EntityType<?> type) {
        for (Faction faction : Faction.values()) {
            if (general(faction).filter(general -> general == type).isPresent()) { return Optional.of(faction); }
        }
        return Optional.empty();
    }

    private static CompoundTag data(Player player, Faction faction) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag all = persisted.getCompound(DATA);
        CompoundTag own = all.getCompound(faction.id());
        all.put(faction.id(), own);
        persisted.put(DATA, all);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return own;
    }

    /** The mission the party's general has for the player now, taken or not. Empty for a party with no story. */
    public static Optional<Mission> current(Player player, Faction faction) {
        List<Mission> story = Mission.of(faction);
        if (story.isEmpty()) { return Optional.empty(); }
        return Optional.of(story.get(Math.min(data(player, faction).getInt(INDEX), story.size() - 1)));
    }

    private static Optional<Faction> factionOf(Player player) { return Factions.of(player); }

    public static boolean isActive(Player player, Faction faction) { return data(player, faction).getBoolean(ACTIVE); }

    /** Missions finished for the party, routine included. */
    public static int done(Player player, Faction faction) { return data(player, faction).getInt(DONE); }

    /**
     * High alignment: the moral bar stands in the party's own zone and enough missions were done for it. Opens the recruits;
     * the castle chapter comes after the whole story, so it also needs this.
     */
    public static boolean aligned(Player player, Faction faction) {
        return Morality.zone(Morality.moral(player)) == faction && done(player, faction) >= ModConfig.MISSION_RECRUIT_MIN_DONE.get();
    }

    /** Paid recruits who follow the player: the village's militia, or better armed soldiers. Refused with the reason. */
    public static boolean recruit(ServerPlayer player, Faction faction, boolean soldier) {
        if (!aligned(player, faction)) {
            player.displayClientMessage(Component.translatable("mission.guerra_ezuberante.recruit.not_aligned"), true);
            return false;
        }
        if (ConvictionPowers.recruits(player).size() >= ModConfig.MISSION_RECRUIT_MAX.get()) {
            player.displayClientMessage(Component.translatable("mission.guerra_ezuberante.recruit.full"), true);
            return false;
        }
        int cost = soldier ? ModConfig.MISSION_SOLDIER_COST.get() : ModConfig.MISSION_MILITIA_COST.get();
        if (!player.getAbilities().instabuild && player.getInventory().countItem(net.minecraft.world.item.Items.EMERALD) < cost) {
            player.displayClientMessage(Component.translatable("mission.guerra_ezuberante.recruit.no_money", cost), true);
            return false;
        }
        var recruit = ConvictionPowers.spawnRecruit(player, soldier);
        if (recruit.isEmpty()) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_space"), true);
            return false;
        }
        if (!player.getAbilities().instabuild) {
            int missing = cost;
            for (ItemStack stack : player.getInventory().items) {
                if (missing > 0 && stack.is(net.minecraft.world.item.Items.EMERALD)) {
                    int used = Math.min(missing, stack.getCount());
                    stack.shrink(used);
                    missing -= used;
                }
            }
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS, 1, 1);
        return true;
    }

    /** The castle the player was sent to: faction of the target and the middle of its village. */
    public static Optional<CastleTarget> target(Player player, Faction faction) {
        CompoundTag data = data(player, faction);
        return Faction.byId(data.getString(TARGET_FACTION)).map(rival -> new CastleTarget(rival, new net.minecraft.core.BlockPos(data.getInt(TARGET_X), 0, data.getInt(TARGET_Z))));
    }

    public record CastleTarget(Faction faction, net.minecraft.core.BlockPos village) {}

    /** Takes the castle chapter with this target. Public for GameTests, where the test world has no villages to find. */
    public static boolean acceptCastle(ServerPlayer player, Faction faction, CastleTarget target) {
        CompoundTag data = data(player, faction);
        data.putBoolean(ACTIVE, true);
        data.putInt(PROGRESS, 0);
        data.putString(TARGET_FACTION, target.faction().id());
        data.putInt(TARGET_X, target.village().getX());
        data.putInt(TARGET_Z, target.village().getZ());
        return true;
    }

    /** One of the three nearest standing castles of the other parties, so giving up can send the player elsewhere. */
    private static Optional<CastleTarget> findTarget(ServerPlayer player, Faction own) {
        var candidates = new java.util.ArrayList<CastleTarget>();
        CastleFall fallen = CastleFall.get(player.server);
        for (Faction rival : Faction.values()) {
            if (rival == own) { continue; }
            FactionVillages.locate(player.server, rival).ifPresent(villages -> villages.stream()
                    .filter(village -> !fallen.hasFallen(rival, village)).forEach(village -> candidates.add(new CastleTarget(rival, village))));
        }
        candidates.sort(java.util.Comparator.comparingDouble(target -> target.village().distSqr(player.blockPosition().atY(0))));
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(player.getRandom().nextInt(Math.min(3, candidates.size()))));
    }

    /** Adds points to the castle chapter; reaching the goal brings the castle down. Public for GameTests. */
    public static void castlePoints(ServerPlayer player, Faction faction, int points) {
        var mission = current(player, faction);
        var target = target(player, faction);
        if (mission.isEmpty() || mission.get().type() != Mission.Type.DESTROY_CASTLE || !isActive(player, faction) || target.isEmpty()) { return; }
        CompoundTag data = data(player, faction);
        int before = data.getInt(PROGRESS);
        if (before >= mission.get().count()) { return; }
        data.putInt(PROGRESS, Math.min(mission.get().count(), before + points));
        player.displayClientMessage(Component.translatable("mission.guerra_ezuberante.progress",
                Component.translatable(mission.get().key("title")), data.getInt(PROGRESS), mission.get().count()).withStyle(ChatFormatting.GOLD), true);
        if (data.getInt(PROGRESS) >= mission.get().count()) { CastleFall.fall(player.server, target.get().faction(), target.get().village()); }
        sync(player, false);
    }

    /** Points the victim is worth for the player's castle chapter: defenders of the target, close to its village. Public for GameTests. */
    public static int castleValue(Player player, Faction faction, LivingEntity victim) {
        var target = target(player, faction);
        if (target.isEmpty() || !(victim instanceof RetaliatingCharacterEntity character) || character.faction() != target.get().faction()) { return 0; }
        double dx = victim.getX() - target.get().village().getX(), dz = victim.getZ() - target.get().village().getZ();
        if (dx * dx + dz * dz > CASTLE_RADIUS * CASTLE_RADIUS) { return 0; }
        if (victim instanceof TowerGuardEntity) { return Mission.CASTLE_GUARD_POINTS; }
        return FactionVillageStructure.leader(target.get().faction()).filter(type -> type == victim.getType()).isPresent() ? Mission.CASTLE_LEADER_POINTS : 0;
    }

    /** Items in the inventory for a delivery; the counter otherwise. */
    public static int progress(Player player, Mission mission) {
        if (mission.type() == Mission.Type.DELIVER || mission.type() == Mission.Type.RETRIEVE) {
            int have = 0;
            for (ItemStack stack : player.getInventory().items) { if (stack.is(mission.wanted())) { have += stack.getCount(); } }
            return Math.min(have, mission.count());
        }
        return Math.min(data(player, mission.faction()).getInt(PROGRESS), mission.count());
    }

    /** Whether the party's general stands close enough to talk. */
    public static boolean nearGeneral(ServerPlayer player, Faction faction) {
        return general(faction).filter(type -> !player.level().getEntities(type, player.getBoundingBox().inflate(TALK_RANGE),
                entity -> entity.isAlive() && entity.distanceToSqr(player) <= TALK_RANGE * TALK_RANGE).isEmpty()).isPresent();
    }

    /** One of the buttons of the general's screen. Everything is checked again here. */
    public static boolean act(ServerPlayer player, int action) {
        var faction = factionOf(player);
        if (faction.isEmpty() || !nearGeneral(player, faction.get())) { return false; }
        boolean changed = switch (action) {
            case ACCEPT -> accept(player, faction.get());
            case TURN_IN -> turnIn(player, faction.get());
            case ABANDON -> abandon(player, faction.get());
            case RECRUIT_MILITIA -> recruit(player, faction.get(), false);
            case RECRUIT_SOLDIER -> recruit(player, faction.get(), true);
            default -> false;
        };
        sync(player, false);
        return changed;
    }

    public static boolean accept(ServerPlayer player, Faction faction) {
        if (isActive(player, faction) || current(player, faction).isEmpty()) { return false; }
        if (current(player, faction).get().type() == Mission.Type.DESTROY_CASTLE) {
            if (!aligned(player, faction)) {
                player.displayClientMessage(Component.translatable("mission.guerra_ezuberante.recruit.not_aligned"), true);
                return false;
            }
            var target = findTarget(player, faction);
            if (target.isEmpty()) {
                player.displayClientMessage(Component.translatable("mission.guerra_ezuberante.no_castle"), true);
                return false;
            }
            return acceptCastle(player, faction, target.get());
        }
        CompoundTag data = data(player, faction);
        data.putBoolean(ACTIVE, true);
        data.putInt(PROGRESS, 0);
        if (current(player, faction).get().type() == Mission.Type.RETRIEVE) { pointToChurch(player, data); }
        return true;
    }

    /** The nearest church, when one is found; the screen then shows where it is. Searching is why this only runs on accepting. */
    private static void pointToChurch(ServerPlayer player, CompoundTag data) {
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                new net.minecraft.resources.ResourceLocation(GuerraEzuberanteMod.MOD_ID, "igreja_daciolo"));
        var church = player.serverLevel().findNearestMapStructure(tag, player.blockPosition(), 64, false);
        if (church == null) {
            data.remove(TARGET_FACTION);
            return;
        }
        data.putString(TARGET_FACTION, Faction.CENTRO.id());
        data.putInt(TARGET_X, church.getX());
        data.putInt(TARGET_Z, church.getZ());
    }

    /** Delivers, pays and advances the story; refused until the goal is met. */
    public static boolean turnIn(ServerPlayer player, Faction faction) {
        var mission = current(player, faction);
        if (mission.isEmpty() || !isActive(player, faction) || progress(player, mission.get()) < mission.get().count()) { return false; }
        Mission done = mission.get();
        if (done.type() == Mission.Type.DELIVER) { take(player, done); }
        CompoundTag data = data(player, faction);
        int story = Mission.of(faction).size();
        int next = data.getInt(INDEX) + 1;
        data.putInt(INDEX, next >= story ? ROUTINE_FROM : next);
        data.putBoolean(ACTIVE, false);
        data.putInt(PROGRESS, 0);
        data.remove(TARGET_FACTION);
        data.putInt(DONE, data.getInt(DONE) + 1);
        Morality.toward(player, faction, ModConfig.MISSION_MORAL.get());
        Progression.addXp(player, ModConfig.MISSION_XP.get());
        if (done.rewardCount() > 0) {
            ItemStack reward = new ItemStack(done.reward(), done.rewardCount());
            if (!player.getInventory().add(reward)) { player.drop(reward, false); }
        }
        player.sendSystemMessage(Component.translatable("mission.guerra_ezuberante.completed", Component.translatable(done.key("title")).withStyle(ChatFormatting.GOLD)));
        player.sendSystemMessage(Component.translatable(done.key("done")).withStyle(ChatFormatting.YELLOW));
        io.github.omarflk.guerraezuberante.social.Feed.missionDone(player, faction, done);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, .7F, 1);
        return true;
    }

    private static void take(ServerPlayer player, Mission mission) {
        int missing = mission.count();
        for (ItemStack stack : player.getInventory().items) {
            if (missing > 0 && stack.is(mission.wanted())) {
                int used = Math.min(missing, stack.getCount());
                stack.shrink(used);
                missing -= used;
            }
        }
    }

    /** Giving up keeps the place in the story, and the party remembers it: the bar moves away from it. */
    public static boolean abandon(ServerPlayer player, Faction faction) {
        if (!isActive(player, faction)) { return false; }
        CompoundTag data = data(player, faction);
        data.putBoolean(ACTIVE, false);
        data.putInt(PROGRESS, 0);
        data.remove(TARGET_FACTION);
        Morality.awayFrom(player, faction, ModConfig.MISSION_ABANDON_MORAL.get());
        player.sendSystemMessage(Component.translatable("mission.guerra_ezuberante.abandoned").withStyle(ChatFormatting.RED));
        return true;
    }

    /** Adds to the active mission when it is of that type. Public for GameTests. */
    public static void count(ServerPlayer player, Mission.Type type) {
        var faction = factionOf(player);
        if (faction.isEmpty() || !isActive(player, faction.get())) { return; }
        var mission = current(player, faction.get());
        if (mission.isEmpty() || mission.get().type() != type) { return; }
        CompoundTag data = data(player, faction.get());
        if (data.getInt(PROGRESS) >= mission.get().count()) { return; }
        data.putInt(PROGRESS, data.getInt(PROGRESS) + 1);
        player.displayClientMessage(Component.translatable("mission.guerra_ezuberante.progress",
                Component.translatable(mission.get().key("title")), data.getInt(PROGRESS), mission.get().count()).withStyle(ChatFormatting.GOLD), true);
        sync(player, false);
    }

    /** Whether the victim counts for that party. Public for GameTests. */
    public static boolean isRivalNpc(LivingEntity victim, Faction own) {
        if (victim instanceof RetaliatingCharacterEntity character) { return character.faction() != null && character.faction() != own; }
        if (victim instanceof Villager villager) {
            Faction other = ModVillagers.faction(villager);
            return other != null && other != own;
        }
        return false;
    }

    /** A flag of another party: same base colour as its design. The two red flags tell apart only by party, so red counts for neither red party. */
    public static boolean isRivalFlag(BlockState state, Faction own) {
        if (!state.is(BlockTags.BANNERS)) { return false; }
        var color = state.getBlock() instanceof BannerBlock banner ? banner.getColor()
                : state.getBlock() instanceof WallBannerBlock wall ? wall.getColor() : null;
        if (color == null || color == FactionBanners.design(own).base()) { return false; }
        for (Faction faction : Faction.values()) {
            if (faction != own && FactionBanners.design(faction).base() == color) { return true; }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.isCanceled()) { return; }
        // Recruits and other summons kill on their owner's account.
        ServerPlayer player = Progression.creditedPlayer(event.getEntity(), event.getSource());
        if (player == null) { return; }
        var faction = factionOf(player);
        if (faction.isEmpty() || !isRivalNpc(event.getEntity(), faction.get())) { return; }
        int value = castleValue(player, faction.get(), event.getEntity());
        if (value > 0) { castlePoints(player, faction.get(), value); }
        count(player, Mission.Type.KILL_RIVALS);
        if (event.getEntity() instanceof TowerGuardEntity) { count(player, Mission.Type.KILL_GUARDS); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) { return; }
        var faction = factionOf(player);
        if (faction.isPresent() && isRivalFlag(event.getState(), faction.get())) { count(player, Mission.Type.SABOTAGE); }
    }

    @SubscribeEvent
    public static void talk(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || player.isShiftKeyDown()) { return; }
        var faction = factionOf(player);
        var party = partyOfGeneral(target.getType());
        if (faction.isEmpty() || party.isEmpty()) { return; }
        if (player instanceof ServerPlayer server) {
            if (party.get() == faction.get()) {
                sync(server, true);
            } else {
                // A rival general has no missions for this player, and says so.
                server.sendSystemMessage(Component.literal("<").append(target.getDisplayName()).append("> ")
                        .append(Component.translatable("mission.guerra_ezuberante.rival." + party.get().id())).withStyle(party.get().color()));
            }
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { sync(player, false); }
    }

    /** Sends the player's place in the story; with {@code open}, the general's screen opens too. */
    public static void sync(ServerPlayer player, boolean open) {
        var faction = factionOf(player);
        var mission = faction.flatMap(party -> current(player, party));
        var target = faction.flatMap(party -> target(player, party));
        FactionNetwork.sendMission(player, mission.map(Enum::ordinal).orElse(-1),
                faction.map(party -> isActive(player, party)).orElse(false),
                mission.map(current -> progress(player, current)).orElse(0),
                faction.map(party -> done(player, party)).orElse(0), open,
                faction.map(party -> aligned(player, party)).orElse(false), ConvictionPowers.recruits(player).size(),
                target.map(value -> value.faction().ordinal()).orElse(-1),
                target.map(value -> value.village().getX()).orElse(0), target.map(value -> value.village().getZ()).orElse(0));
    }

    private Missions() {}
}
