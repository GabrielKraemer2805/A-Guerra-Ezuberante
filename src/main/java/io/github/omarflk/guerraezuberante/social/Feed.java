package io.github.omarflk.guerraezuberante.social;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.mission.Mission;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * The game's fictional X: the same timeline for everyone in the world, saved with it. Posts appear when a mission is
 * done: the player's own, one from a politician of the party and sometimes a jab from a rival. Newest last.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class Feed extends SavedData {
    public static final int MAX_POSTS = 60;
    /** Chance of a rival politician commenting. */
    private static final float RIVAL_CHANCE = .4F;
    private static final int PLAYER_LINES = 2, REACT_LINES = 2;
    private final List<Post> posts = new ArrayList<>();

    /** author is -1 for a player's post; arg fills the text's %s: a translation key when argIsKey, otherwise literal. */
    public record Post(int author, String player, int faction, String text, String arg, boolean argIsKey, long time, int likes) {
        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Author", author); tag.putString("Player", player); tag.putInt("Faction", faction);
            tag.putString("Text", text); tag.putString("Arg", arg); tag.putBoolean("ArgIsKey", argIsKey);
            tag.putLong("Time", time); tag.putInt("Likes", likes);
            return tag;
        }
        static Post load(CompoundTag tag) {
            return new Post(tag.getInt("Author"), tag.getString("Player"), tag.getInt("Faction"), tag.getString("Text"),
                    tag.getString("Arg"), tag.getBoolean("ArgIsKey"), tag.getLong("Time"), tag.getInt("Likes"));
        }
    }

    public static Feed get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Feed::load, Feed::new, "guerra_ezuberante_feed");
    }

    private static Feed load(CompoundTag tag) {
        Feed feed = new Feed();
        for (Tag post : tag.getList("Posts", Tag.TAG_COMPOUND)) { feed.posts.add(Post.load((CompoundTag) post)); }
        return feed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        posts.forEach(post -> list.add(post.save()));
        tag.put("Posts", list);
        return tag;
    }

    public List<Post> posts() { return List.copyOf(posts); }

    private void add(MinecraftServer server, Post post) {
        posts.add(post);
        while (posts.size() > MAX_POSTS) { posts.remove(0); }
        setDirty();
        server.getPlayerList().getPlayers().forEach(player -> FactionNetwork.sendPost(player, post));
    }

    private static int likes(RandomSource random) { return 10 + random.nextInt(50000); }

    /** What a finished mission makes appear on the timeline. */
    public static void missionDone(ServerPlayer player, Faction faction, Mission mission) {
        MinecraftServer server = player.server;
        Feed feed = get(server);
        RandomSource random = player.getRandom();
        long now = server.overworld().getGameTime();
        feed.add(server, new Post(-1, player.getGameProfile().getName(), faction.ordinal(), "x.guerra_ezuberante.player." + random.nextInt(PLAYER_LINES),
                mission.key("title"), true, now, random.nextInt(40)));
        var own = Author.of(faction);
        if (!own.isEmpty()) {
            Author author = own.get(random.nextInt(own.size()));
            feed.add(server, new Post(author.ordinal(), "", faction.ordinal(), "x.guerra_ezuberante.author." + author.id() + "." + random.nextInt(Author.LINES),
                    "", false, now, likes(random)));
        }
        if (random.nextFloat() < RIVAL_CHANCE) {
            var rivals = new ArrayList<Author>();
            for (Author author : Author.values()) { if (author.faction() != faction) { rivals.add(author); } }
            Author rival = rivals.get(random.nextInt(rivals.size()));
            feed.add(server, new Post(rival.ordinal(), "", rival.faction().ordinal(),
                    "x.guerra_ezuberante.react." + rival.faction().id() + "." + random.nextInt(REACT_LINES),
                    player.getGameProfile().getName(), false, now, likes(random)));
        }
    }

    /** A castle fell: its own party comments. */
    public static void castleFell(MinecraftServer server, Faction faction) {
        Feed feed = get(server);
        var own = Author.of(faction);
        if (own.isEmpty()) { return; }
        RandomSource random = server.overworld().getRandom();
        Author author = own.get(random.nextInt(own.size()));
        feed.add(server, new Post(author.ordinal(), "", faction.ordinal(), "x.guerra_ezuberante.castle." + faction.id(), "", false, server.overworld().getGameTime(), likes(random)));
    }

    /** Whoever comes in sees the timeline as it is. */
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FactionNetwork.sendPosts(player, get(player.server).posts());
        }
    }
}
