package io.github.omarflk.guerraezuberante.summon;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.UkrainianFighterEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;

/** Retires a player's units, including unloaded ones, without permanently closing the player's UUID. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class PlayerSummoningEvents {
    public static void retire(ServerPlayer player) {
        for (var level : player.server.getAllLevels()) {
            SummonLedger.get(level).forgetPlayer(player.getUUID());
            var loaded = new ArrayList<UkrainianFighterEntity>();
            for (var entity : level.getAllEntities()) {
                if (entity instanceof UkrainianFighterEntity unit && player.getUUID().equals(unit.getOwnerId())) { loaded.add(unit); }
            }
            loaded.forEach(UkrainianFighterEntity::discard);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { retire(player); }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { retire(player); }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { retire(player); }
    }
    private PlayerSummoningEvents() {}
}
