package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.social.Feed;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** The timeline as the server sent it. Newest last. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class FeedClient {
    private static final List<Feed.Post> POSTS = new ArrayList<>();

    public static void replace(List<Feed.Post> posts) {
        POSTS.clear();
        POSTS.addAll(posts);
    }

    public static void add(Feed.Post post) {
        POSTS.add(post);
        while (POSTS.size() > Feed.MAX_POSTS) { POSTS.remove(0); }
    }

    public static List<Feed.Post> posts() { return List.copyOf(POSTS); }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) { POSTS.clear(); }

    private FeedClient() {}
}
