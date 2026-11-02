package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.mission.Mission;
import io.github.omarflk.guerraezuberante.mission.Missions;
import io.github.omarflk.guerraezuberante.social.Author;
import io.github.omarflk.guerraezuberante.social.Feed;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** 0.026: the fictional X timeline, fed by the missions. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FeedGameTests {
    @GameTest(template="empty", batch="ge_feed_authors") public static void everyPartyHasAuthorsWithUniqueHandles(GameTestHelper h) {
        for (Faction faction : Faction.values()) {
            h.assertTrue(!Author.of(faction).isEmpty(), faction + " has no one to post");
        }
        var handles = new java.util.HashSet<String>();
        for (Author author : Author.values()) { h.assertTrue(handles.add(author.handle()), "Repeated handle " + author.handle()); }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_feed_mission") public static void finishedMissionPostsAndTheFeedIsSaved(GameTestHelper h) {
        var server = h.getLevel().getServer();
        // The timeline is saved with the test world, so the name must be new in each run.
        String name = "Feed" + UUID.randomUUID().toString().substring(0, 8);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 2, 1)));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addNewPlayer(player);
        try {
            Factions.assign(player, Faction.BOLSONARISTA);
            var feed = Feed.get(server);
            int before = feed.posts().size();
            Missions.accept(player, Faction.BOLSONARISTA);
            h.assertTrue(feed.posts().size() == before, "Accepting should not post");
            player.getInventory().add(new ItemStack(Items.IRON_INGOT, 12));
            h.assertTrue(Missions.turnIn(player, Faction.BOLSONARISTA), "Turn in refused");
            var posts = feed.posts();
            h.assertTrue(posts.size() >= Math.min(Feed.MAX_POSTS, before + 2), "Posts after a mission: " + posts.size());
            var own = posts.stream().filter(post -> post.player().equals(name)).toList();
            h.assertTrue(own.size() == 1 && own.get(0).arg().equals(Mission.BOL_MINERIO.key("title")) && own.get(0).argIsKey(), "The player's post: " + own);
            var last = posts.get(posts.indexOf(own.get(0)) + 1);
            h.assertTrue(Author.byOrdinal(last.author()).orElseThrow().faction() == Faction.BOLSONARISTA, "The party did not comment: " + last);
            h.assertTrue(last.text().startsWith("x.guerra_ezuberante.author."), "Wrong text " + last.text());

            CompoundTag saved = feed.save(new CompoundTag());
            h.assertTrue(saved.getList("Posts", 10).size() == posts.size(), "Not saved");
            for (int i = 0; i < Feed.MAX_POSTS + 5; i++) { Feed.missionDone(player, Faction.CENTRO, Mission.CEN_EMENDA); }
            h.assertTrue(feed.posts().size() == Feed.MAX_POSTS, "The timeline grows forever: " + feed.posts().size());
        } finally {
            Factions.clear(player);
            h.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }
}
