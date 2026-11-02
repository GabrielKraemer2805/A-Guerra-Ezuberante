package io.github.omarflk.guerraezuberante.mission;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.entity.TowerGuardEntity;
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

    public static final int ACCEPT = 0, TURN_IN = 1, ABANDON = 2;

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

    /** Items in the inventory for a delivery; the counter otherwise. */
    public static int progress(Player player, Mission mission) {
        if (mission.type() == Mission.Type.DELIVER) {
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
            default -> false;
        };
        sync(player, false);
        return changed;
    }

    public static boolean accept(ServerPlayer player, Faction faction) {
        if (isActive(player, faction) || current(player, faction).isEmpty()) { return false; }
        CompoundTag data = data(player, faction);
        data.putBoolean(ACTIVE, true);
        data.putInt(PROGRESS, 0);
        return true;
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
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)) { return; }
        var faction = factionOf(player);
        if (faction.isEmpty() || !isRivalNpc(event.getEntity(), faction.get())) { return; }
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
        FactionNetwork.sendMission(player, mission.map(Enum::ordinal).orElse(-1),
                faction.map(party -> isActive(player, party)).orElse(false),
                mission.map(current -> progress(player, current)).orElse(0),
                faction.map(party -> done(player, party)).orElse(0), open);
    }

    private Missions() {}
}
