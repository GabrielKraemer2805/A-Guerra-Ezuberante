package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.faction.*;
import io.github.omarflk.guerraezuberante.item.MegaphoneItem;
import io.github.omarflk.guerraezuberante.progression.PowerCycle;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** 0.024: the walk through the powers (Shift + scroll) and the craftable items. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CraftableItemGameTests {
    private static final String[] RECIPES = {"picanha", "cerveja", "feijoada", "bolo_de_mandioca", "cloroquina", "megafone", "foice_e_martelo",
            "motosserra", "semente_feijao", "mandioca"};

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h, Vec3 at, String name) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        player.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addNewPlayer(player);
        return player;
    }
    private static void leave(GameTestHelper h, net.minecraft.server.level.ServerPlayer player) {
        Factions.clear(player);
        h.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }
    private static Vec3 ground(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 230, 3));
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }

    @GameTest(template="empty", batch="ge_power_cycle") public static void scrollWalksTheBoughtPowersAndWraps(GameTestHelper h) {
        var slots = List.of(0, 3, 4, 9);
        h.assertTrue(PowerCycle.step(slots, 0, 1) == 3 && PowerCycle.step(slots, 4, 1) == 9, "Forward");
        h.assertTrue(PowerCycle.step(slots, 9, 1) == 0, "Forward does not wrap");
        h.assertTrue(PowerCycle.step(slots, 0, -1) == 9, "Backward does not wrap");
        h.assertTrue(PowerCycle.step(slots, 7, 1) == 3, "A slot no longer owned should count as the first");
        h.assertTrue(PowerCycle.step(List.of(0), 0, 1) == 0, "A single power stays");
        var bought = PowerCycle.slots(Faction.MISSAO, node -> node == io.github.omarflk.guerraezuberante.progression.SkillTree.Node.P2
                || node == io.github.omarflk.guerraezuberante.progression.SkillTree.powerNode(Faction.MISSAO, FigurePower.FLAGRANTE.slot()).orElseThrow());
        h.assertTrue(bought.equals(List.of(0, 1, FigurePower.FLAGRANTE.slot())), "Bought slots: " + bought);
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_items_recipes") public static void everyItemHasARecipeThatWorks(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        for (String id : RECIPES) {
            var recipe = recipes.byKey(new ResourceLocation(GuerraEzuberanteMod.MOD_ID, id));
            h.assertTrue(recipe.isPresent(), "No recipe " + id);
            Item made = recipe.get().getResultItem(h.getLevel().registryAccess()).getItem();
            h.assertTrue(made != Items.AIR && new ResourceLocation(GuerraEzuberanteMod.MOD_ID, id).equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(made)),
                    id + " makes " + made);
            h.assertTrue(!recipe.get().getIngredients().isEmpty(), id + " has no ingredients");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_items_food") public static void foodsDoWhatTheySay(GameTestHelper h) {
        var player = player(h, ground(h), "FoodQA");
        try {
            player.getFoodData().setFoodLevel(2);
            var picanha = new ItemStack(ModItems.PICANHA.get());
            picanha.finishUsingItem(h.getLevel(), player);
            h.assertTrue(player.getFoodData().getFoodLevel() >= 12 && player.hasEffect(MobEffects.DAMAGE_BOOST), "The promised picanha did not arrive");
            player.removeAllEffects();
            var beer = new ItemStack(ModItems.CERVEJA.get());
            ItemStack left = beer.finishUsingItem(h.getLevel(), player);
            h.assertTrue(left.is(Items.GLASS_BOTTLE) && player.hasEffect(MobEffects.MOVEMENT_SPEED) && player.hasEffect(MobEffects.CONFUSION), "Beer: " + left);
            player.removeAllEffects();
            player.setHealth(10);
            int food = player.getFoodData().getFoodLevel();
            new ItemStack(ModItems.CLOROQUINA.get()).finishUsingItem(h.getLevel(), player);
            h.assertTrue(player.getHealth() == 10 && player.getFoodData().getFoodLevel() == food && player.hasEffect(MobEffects.CONFUSION) && player.hasEffect(MobEffects.LUCK),
                    "Cloroquina should cure nothing and leave only the bad taste");
            ItemStack bowl = new ItemStack(ModItems.FEIJOADA.get()).finishUsingItem(h.getLevel(), player);
            h.assertTrue(bowl.is(Items.BOWL), "Feijoada keeps the bowl: " + bowl);
        } finally { leave(h, player); }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_items_tools") public static void megaphonePushesEnemiesAndWeaponsHit(GameTestHelper h) {
        var p = ground(h);
        var player = player(h, p, "ToolsQA");
        Factions.assign(player, Faction.LULISTA);
        var ally = player(h, p.add(2, 0, 0), "ToolsAlly");
        Factions.assign(ally, Faction.LULISTA);
        Husk husk = EntityType.HUSK.create(h.getLevel());
        husk.setNoAi(true); husk.moveTo(p.x, p.y, p.z + 2, 0, 0);
        h.getLevel().addFreshEntity(husk);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MEGAFONE.get()));
            player.getItemInHand(InteractionHand.MAIN_HAND).use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            h.assertTrue(husk.getDeltaMovement().horizontalDistanceSqr() > .01, "The megaphone did not push the husk");
            h.assertTrue(ally.getDeltaMovement().horizontalDistanceSqr() < .001, "The megaphone pushed an ally");
            h.assertTrue(player.getCooldowns().isOnCooldown(ModItems.MEGAFONE.get()), "No breath needed");
            h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() == 1, "It did not wear out");
            h.assertTrue(MegaphoneItem.COOLDOWN == 200, "Cooldown changed");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MOTOSSERRA.get()));
            double chainsaw = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FOICE_E_MARTELO.get()));
            double sickle = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            double sword = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            h.assertTrue(chainsaw >= sickle && sickle >= sword, "Weapons: " + chainsaw + " " + sickle + " " + sword);
        } finally { husk.discard(); leave(h, ally); leave(h, player); }
        h.succeed();
    }
}
