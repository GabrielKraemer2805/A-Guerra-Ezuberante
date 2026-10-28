package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.faction.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FactionGameTests {
    private static ServerPlayer player(GameTestHelper h, String name) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }
    @GameTest(template="empty") public static void membershipSurvivesSaveAndDropsUnknownIds(GameTestHelper h) {
        var data = new FactionData(); UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        data.set(a, Faction.LULISTA); data.set(b, Faction.CENTRO);
        var tag = data.save(new CompoundTag());
        tag.getList("Members", 10).getCompound(1).putString("Faction", "removida");
        var loaded = FactionData.load(tag);
        h.assertTrue(loaded.all().size() == 1, "Unknown faction id was not dropped");
        h.assertTrue(loaded.get(a).isPresent() || loaded.get(b).isPresent(), "Valid membership was lost on load");
        var reloaded = FactionData.load(data.save(new CompoundTag()));
        h.assertTrue(reloaded.get(a).orElseThrow() == Faction.LULISTA && reloaded.get(b).orElseThrow() == Faction.CENTRO, "Round trip changed factions");
        h.succeed();
    }
    @GameTest(template="empty") public static void firstChoiceIsFinalUntilOperatorActs(GameTestHelper h) {
        var player = player(h, "FactionQA");
        h.assertTrue(Factions.of(player).isEmpty(), "New player already had a faction");
        h.assertTrue(Factions.choose(player, Faction.MISSAO), "First choice was rejected");
        h.assertTrue(!Factions.choose(player, Faction.LULISTA) && Factions.of(player).orElseThrow() == Faction.MISSAO, "Player changed faction alone");
        Factions.assign(player, Faction.LULISTA);
        h.assertTrue(Factions.of(player).orElseThrow() == Faction.LULISTA, "Operator assignment failed");
        h.assertTrue(Factions.clear(player) && Factions.of(player).isEmpty() && !Factions.clear(player), "Clear did not reset exactly once");
        h.assertTrue(Factions.choose(player, Faction.CENTRO), "Cleared player could not choose again");
        Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty") public static void nameAndTabListShowFactionPrefix(GameTestHelper h) {
        var player = player(h, "TagQA");
        h.assertTrue(player.getDisplayName().getString().equals("TagQA"), "Player without faction got a prefix");
        Factions.assign(player, Faction.BOLSONARISTA);
        h.assertTrue(player.getDisplayName().getString().startsWith("[") && player.getDisplayName().getString().endsWith("] TagQA"), "Display name prefix missing: " + player.getDisplayName().getString());
        var tab = player.getTabListDisplayName();
        h.assertTrue(tab != null && tab.getString().endsWith("] TagQA"), "TAB name prefix missing");
        Factions.clear(player);
        h.assertTrue(player.getDisplayName().getString().equals("TagQA"), "Prefix stayed after clear");
        h.succeed();
    }
    @GameTest(template="empty") public static void sameFactionKeepsFriendlyFire(GameTestHelper h) {
        // The GameTest server disables PvP, so compare against a control pair instead of expecting true.
        var a = player(h, "FireA"); var b = player(h, "FireB");
        var c = player(h, "ControlC"); var d = player(h, "ControlD");
        Factions.assign(a, Faction.CENTRO); Factions.assign(b, Faction.CENTRO);
        h.assertTrue(a.canHarmPlayer(b) == c.canHarmPlayer(d) && b.canHarmPlayer(a) == d.canHarmPlayer(c), "Same faction changed the PvP rule");
        h.assertTrue(!a.isAlliedTo(b) && a.getTeam() == null, "Faction changed vanilla alliance or teams");
        Factions.clear(a); Factions.clear(b); h.succeed();
    }
    @GameTest(template="empty") public static void operatorCommandsRequirePermission(GameTestHelper h) throws Exception {
        var player = player(h, "CommandQA");
        var dispatcher = h.getLevel().getServer().getCommands().getDispatcher();
        var source = player.createCommandSourceStack().withSuppressedOutput();
        var root = dispatcher.getRoot().getChild("ge").getChild("faccao");
        h.assertTrue(root.canUse(source.withPermission(0)), "Players cannot query their own faction");
        h.assertTrue(!root.getChild("definir").canUse(source.withPermission(0)) && root.getChild("definir").canUse(source.withPermission(2)), "definir permission incorrect");
        h.assertTrue(!root.getChild("limpar").canUse(source.withPermission(0)) && root.getChild("limpar").canUse(source.withPermission(2)), "limpar permission incorrect");
        h.assertTrue(dispatcher.execute("ge faccao definir @s direita", source.withPermission(2)) == 1 && Factions.of(player).orElseThrow() == Faction.DIREITA, "definir failed");
        h.assertTrue(dispatcher.execute("ge faccao", source) == 1, "Query failed for member");
        h.assertTrue(dispatcher.execute("ge faccao limpar @s", source.withPermission(2)) == 1 && Factions.of(player).isEmpty(), "limpar failed");
        boolean rejected = false;
        try { dispatcher.execute("ge faccao definir @s inexistente", source.withPermission(2)); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { rejected = true; }
        h.assertTrue(rejected && Factions.of(player).isEmpty(), "Unknown faction id was accepted");
        h.succeed();
    }
}
