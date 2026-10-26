package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import io.github.omarflk.guerraezuberante.entity.UkrainianFighterEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import io.github.omarflk.guerraezuberante.summon.ArthurSummoning;
import io.github.omarflk.guerraezuberante.summon.SummonLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SummonGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void castCreatesFiveSafeEmergingUnits(GameTestHelper helper) {
        ArthurDoValEntity owner = owner(helper);
        Cow enemy = enemy(helper);
        owner.hurt(helper.getLevel().damageSources().mobAttack(enemy), 1);
        helper.assertTrue(owner.isActionLocked(), "Cast preparation did not start on aggression");
        helper.assertTrue(units(helper, owner).isEmpty(), "Troops appeared before casting finished");
        helper.assertTrue(ModSounds.ARTHUR_SUMMON.isAvailable(), "Summon sound resource missing");
        helper.runAfterDelay(21, () -> {
            List<UkrainianFighterEntity> units = units(helper, owner);
            helper.assertTrue(units.size() == 5, "First cast must create exactly five units");
            for (UkrainianFighterEntity unit : units) {
                helper.assertTrue(unit.getHealth() == 15, "Wrong troop health");
                helper.assertTrue(unit.getMainHandItem().is(ModItems.UKRAINIAN_SWORD.get()), "Troop has no custom sword");
                helper.assertTrue(unit.isActionLocked(), "Troop attacked before emergence completed");
                helper.assertTrue(helper.getLevel().noCollision(unit, unit.getBoundingBox()), "Troop spawned inside blocks");
                helper.assertTrue(unit.getY() >= owner.getY() - 3, "Physical position was put below the ground");
                for (UkrainianFighterEntity other : units) {
                    if (unit != other) { helper.assertTrue(!unit.getBoundingBox().intersects(other.getBoundingBox()), "Overlapping troops"); }
                }
            }
            helper.runAfterDelay(ModConfig.EMERGENCE_TICKS.get() + 10, () -> {
                helper.assertTrue(units.stream().noneMatch(UkrainianFighterEntity::isActionLocked), "Emergence never finished");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 850)
    public static void wavesRespectSevenHundredTickInterval(GameTestHelper helper) {
        ArthurDoValEntity owner = owner(helper);
        Cow enemy = enemy(helper);
        owner.hurt(helper.getLevel().damageSources().mobAttack(enemy), 1);
        helper.runAfterDelay(699, () -> helper.assertTrue(SummonLedger.get(helper.getLevel()).count(owner.getUUID()) == 5,
                "A second wave arrived before 700 ticks"));
        helper.runAfterDelay(725, () -> {
            helper.assertTrue(SummonLedger.get(helper.getLevel()).count(owner.getUUID()) == 10,
                    "Second wave missing: count=" + SummonLedger.get(helper.getLevel()).count(owner.getUUID())
                            + ", alive=" + owner.isAlive() + ", target=" + owner.getTarget()
                            + ", cooldown=" + owner.getSummonCooldown() + ", enemyHealth=" + enemy.getHealth());
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void capIsPerOwnerAndAllowsPartialWave(GameTestHelper helper) {
        ArthurDoValEntity first = owner(helper);
        int cap = ModConfig.MAX_SUMMONS.get();
        for (int i = 0; i < cap - 2; i++) {
            SummonLedger.get(helper.getLevel()).register(first.getUUID(), java.util.UUID.randomUUID());
        }
        helper.assertTrue(ArthurSummoning.summon(first) == 2, "Partial wave exceeded owner capacity");
        helper.assertTrue(ArthurSummoning.summon(first) == 0, "Owner cap was exceeded");
        ArthurDoValEntity second = ModEntities.ARTHUR_DO_VAL.get().create(helper.getLevel());
        second.moveTo(first.getX() + 7, first.getY(), first.getZ() + 7);
        helper.getLevel().addFreshEntity(second);
        helper.assertTrue(ArthurSummoning.summon(second) == 5, "Another Arthur inherited the first owner's cap");
        helper.assertTrue(second.getSummonCooldown() == 0, "Owner cooldowns were shared");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unsafeTerrainCannotSpawnTroops(GameTestHelper helper) {
        ArthurDoValEntity owner = ModEntities.ARTHUR_DO_VAL.get().create(helper.getLevel());
        owner.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 3, 8))));
        helper.getLevel().addFreshEntity(owner);
        for (int x = 1; x < 16; x++) {
            for (int z = 1; z < 16; z++) {
                for (int y = 0; y <= 7; y++) { helper.setBlock(new BlockPos(x, y, z), Blocks.STONE); }
            }
        }
        helper.assertTrue(ArthurSummoning.summon(owner) == 0, "Spawned inside solid terrain");
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.LAVA);
        helper.assertTrue(!ArthurSummoning.isSafe(helper.getLevel(), helper.absolutePos(new BlockPos(3, 3, 3))), "Accepted lava");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void ironSwordUsesVanillaPropertiesAndDamage(GameTestHelper helper) {
        ArthurDoValEntity owner = owner(helper);
        UkrainianFighterEntity unit = unit(helper, owner, 5);
        unit.setNoAi(true);
        Cow target = enemy(helper);
        target.removeAllEffects();
        target.setPos(unit.position().add(1, 0, 0));
        var sword = ModItems.UKRAINIAN_SWORD.get();
        helper.assertTrue(sword.getMaxDamage() == Items.IRON_SWORD.getMaxDamage(), "Sword durability changed");
        for (var attribute : List.of(Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED)) {
            var custom = sword.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute);
            var vanilla = Items.IRON_SWORD.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute);
            helper.assertTrue(custom.size() == vanilla.size(), "Extra weapon modifiers");
            helper.assertTrue(custom.iterator().next().getAmount() == vanilla.iterator().next().getAmount(), "Iron weapon modifier changed");
        }
        helper.assertTrue(!unit.doHurtTarget(target), "Attacked during emergence");
        helper.runAfterDelay(ModConfig.EMERGENCE_TICKS.get() + 2, () -> {
            helper.assertTrue(unit.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6.0D, "Sword damage was double counted");
            float before = target.getHealth();
            helper.assertTrue(unit.doHurtTarget(target), "Sword attack failed");
            helper.assertTrue(Math.abs(before - target.getHealth() - 6) < 0.01, "Expected exactly six damage");
            helper.assertTrue(unit.getMainHandItem().getDamageValue() == 1, "Sword did not wear normally");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void troopsShareOnlyLegitimateOwnerTargets(GameTestHelper helper) {
        ArthurDoValEntity owner = owner(helper);
        UkrainianFighterEntity unit = unit(helper, owner, 6);
        UkrainianFighterEntity sister = unit(helper, owner, 5);
        var renan = ModEntities.RENAN_SANTOS.get().create(helper.getLevel());
        renan.setPos(unit.position().add(0, 0, 1));
        helper.getLevel().addFreshEntity(renan);
        Cow enemy = enemy(helper);
        helper.assertTrue(unit.isAlliedTo(owner) && unit.isAlliedTo(sister) && unit.isAlliedTo(renan), "Missing ally rule");
        helper.runAfterDelay(ModConfig.EMERGENCE_TICKS.get() + 2, () -> {
            helper.assertTrue(unit.getTarget() == null, "Unprovoked aggression");
            helper.assertTrue(!unit.doHurtTarget(owner) && !unit.doHurtTarget(sister) && !unit.doHurtTarget(renan), "Friendly fire allowed");
            owner.hurt(helper.getLevel().damageSources().mobAttack(enemy), 1);
            helper.runAfterDelay(11, () -> {
                helper.assertTrue(unit.getTarget() == enemy, "Did not share owner's legitimate target");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void ownershipLedgerAndCooldownSurviveSaveLoad(GameTestHelper helper) {
        ArthurDoValEntity owner = owner(helper);
        UkrainianFighterEntity unit = unit(helper, owner, 5);
        SummonLedger.get(helper.getLevel()).register(owner.getUUID(), unit.getUUID());
        CompoundTag saved = unit.saveWithoutId(new CompoundTag());
        UkrainianFighterEntity restored = ModEntities.UKRAINIAN_FIGHTER.get().create(helper.getLevel());
        restored.load(saved);
        helper.assertTrue(owner.getUUID().equals(restored.getOwnerId()), "Owner UUID was lost");
        helper.assertTrue(restored.isActionLocked(), "Emergence state lost on save/load");
        SummonLedger copy = SummonLedger.load(SummonLedger.get(helper.getLevel()).save(new CompoundTag()));
        helper.assertTrue(copy.count(owner.getUUID()) == 1, "Unloaded troop capacity was lost");
        Cow enemy = enemy(helper);
        owner.hurt(helper.getLevel().damageSources().mobAttack(enemy), 1);
        var loadedOwner = ModEntities.ARTHUR_DO_VAL.get().create(helper.getLevel());
        loadedOwner.load(owner.saveWithoutId(new CompoundTag()));
        helper.assertTrue(loadedOwner.getSummonCooldown() == owner.getSummonCooldown(), "Owner cooldown not saved");
        owner.kill();
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(unit.isRemoved(), "Owner death left orphan troops");
            helper.assertTrue(SummonLedger.get(helper.getLevel()).count(owner.getUUID()) == 0, "Dead troops still occupy slots");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void combatEndsWithoutExtraSummons(GameTestHelper helper) {
        ArthurDoValEntity owner = owner(helper);
        Cow enemy = enemy(helper);
        owner.hurt(helper.getLevel().damageSources().mobAttack(enemy), 1);
        enemy.discard();
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(owner.getTarget() == null && units(helper, owner).isEmpty(), "Cast continued without an enemy");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void idleTroopsFollowArthur(GameTestHelper helper) {
        ArthurDoValEntity owner = owner(helper);
        owner.setNoAi(true);
        UkrainianFighterEntity unit = unit(helper, owner, 1);
        double initial = unit.distanceToSqr(owner);
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(unit.getTarget() == null, "Following created an enemy");
            helper.assertTrue(unit.distanceToSqr(owner) < initial - 4, "Idle troop did not follow Arthur");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void punchImpactIsSynchronizedAndNotDuplicated(GameTestHelper helper) {
        owner(helper).discard();
        var renan = ModEntities.RENAN_SANTOS.get().create(helper.getLevel());
        // Exercise the preserved unarmed fallback explicitly; Renan now normally spawns armed.
        renan.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
        renan.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 1, 8))));
        helper.getLevel().addFreshEntity(renan);
        Cow target = enemy(helper);
        target.removeAllEffects();
        target.setPos(renan.position().add(1, 0, 0));
        renan.hurt(helper.getLevel().damageSources().mobAttack(target), 1);
        float[] health = {target.getHealth()};
        int[] hits = {0};
        helper.onEachTick(() -> {
            if (target.getHealth() < health[0]) {
                helper.assertTrue(renan.attackAnimationAge(0) == 5, "Damage did not match visual impact frame");
                helper.assertTrue(health[0] - target.getHealth() == 4, "Renan damage changed or was duplicated");
                hits[0]++;
                health[0] = target.getHealth();
            }
        });
        helper.runAfterDelay(65, () -> {
            helper.assertTrue(hits[0] >= 1 && hits[0] <= 4, "Wrong punch frequency: hits=" + hits[0]
                    + ", age=" + renan.attackAnimationAge(0) + ", target=" + renan.getTarget()
                    + ", distance=" + renan.distanceToSqr(target));
            helper.succeed();
        });
    }

    private static ArthurDoValEntity owner(GameTestHelper helper) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 17; z++) { helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE); }
        }
        ArthurDoValEntity owner = ModEntities.ARTHUR_DO_VAL.get().create(helper.getLevel());
        owner.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 1, 8))));
        helper.getLevel().addFreshEntity(owner);
        return owner;
    }

    private static Cow enemy(GameTestHelper helper) {
        Cow enemy = EntityType.COW.create(helper.getLevel());
        enemy.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 1, 8))));
        enemy.setNoAi(true);
        enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
        enemy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        enemy.setHealth(1024);
        enemy.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2000, 3));
        helper.getLevel().addFreshEntity(enemy);
        return enemy;
    }

    private static UkrainianFighterEntity unit(GameTestHelper helper, ArthurDoValEntity owner, int x) {
        UkrainianFighterEntity unit = ModEntities.UKRAINIAN_FIGHTER.get().create(helper.getLevel());
        unit.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 1, 8))));
        unit.bindTo(owner);
        helper.getLevel().addFreshEntity(unit);
        return unit;
    }

    private static List<UkrainianFighterEntity> units(GameTestHelper helper, ArthurDoValEntity owner) {
        return helper.getLevel().getEntitiesOfClass(UkrainianFighterEntity.class,
                owner.getBoundingBox().inflate(20), unit -> owner.getUUID().equals(unit.getOwnerId()) && unit.isAlive());
    }
}
