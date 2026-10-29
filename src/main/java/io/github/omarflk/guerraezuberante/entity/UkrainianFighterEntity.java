package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import io.github.omarflk.guerraezuberante.summon.SummonLedger;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class UkrainianFighterEntity extends RetaliatingCharacterEntity implements io.github.omarflk.guerraezuberante.summon.OwnedSummon {
    private static final EntityDataAccessor<Long> EMERGENCE_START = SynchedEntityData.defineId(
            UkrainianFighterEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> EMERGENCE_DURATION = SynchedEntityData.defineId(
            UkrainianFighterEntity.class, EntityDataSerializers.INT);
    private UUID ownerId;
    private UUID playerSummonToken;
    private LivingEntity cachedOwner;
    private int lifeTicks;
    private int missingOwnerTicks;
    private boolean wasEmerging;

    public UkrainianFighterEntity(EntityType<? extends UkrainianFighterEntity> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.UKRAINIAN_SWORD.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, ModConfig.FIGHTER_HEALTH.getDefault())
                .add(Attributes.ATTACK_DAMAGE, 1.0D) // +5 from the normal iron-tier sword = 6, never +6 twice.
                .add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(EMERGENCE_START, -1000L);
        entityData.define(EMERGENCE_DURATION, 1);
    }

    public void bindTo(ArthurDoValEntity owner) {
        bindTo(owner, 0);
    }

    public void bindTo(ArthurDoValEntity owner, int delayTicks) {
        bindTo(owner, delayTicks, ModConfig.SUMMON_LIFETIME.get());
    }

    /** Owner is an Arthur or, for the Missão faction power, a player. */
    public void bindTo(LivingEntity owner, int delayTicks, int lifetimeTicks) {
        ownerId = owner.getUUID();
        cachedOwner = owner;
        if (owner instanceof net.minecraft.server.level.ServerPlayer && level() instanceof ServerLevel server) {
            playerSummonToken = SummonLedger.get(server).playerToken(ownerId);
        }
        lifeTicks = lifetimeTicks;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.FIGHTER_HEALTH.get());
        setHealth(getMaxHealth());
        entityData.set(EMERGENCE_START, level().getGameTime() + Math.max(0, delayTicks));
        entityData.set(EMERGENCE_DURATION, Math.max(44, ModConfig.EMERGENCE_TICKS.get()));
        wasEmerging = true;
    }

    @Override
    public UUID getOwnerId() { return ownerId; }
    public UUID getPlayerSummonToken() { return playerSummonToken; }
    public ArthurDoValEntity getOwnerEntity() { return cachedOwner instanceof ArthurDoValEntity arthur ? arthur : null; }
    public LivingEntity getOwner() { return cachedOwner; }

    public float emergenceProgress(float partialTick) {
        return net.minecraft.util.Mth.clamp((level().getGameTime() - entityData.get(EMERGENCE_START)
                + partialTick) / entityData.get(EMERGENCE_DURATION), 0.0F, 1.0F);
    }

    @Override
    public boolean isActionLocked() { return emergenceProgress(0) < 1.0F; }

    @Override
    protected List<ModSounds.VoiceLine> interactionVoices() { return List.of(); }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new Goal() {
            private int recalc;
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override
            public boolean canUse() {
                return !isActionLocked() && getTarget() == null && cachedOwner != null && cachedOwner.isAlive()
                        && distanceToSqr(cachedOwner) > 25.0D;
            }
            @Override
            public boolean canContinueToUse() {
                return !isActionLocked() && getTarget() == null && cachedOwner != null && cachedOwner.isAlive()
                        && distanceToSqr(cachedOwner) > 6.25D;
            }
            @Override
            public void start() { recalc = 0; }
            @Override
            public void stop() { getNavigation().stop(); }
            @Override
            public void tick() {
                if (cachedOwner != null && --recalc <= 0) {
                    recalc = 10;
                    getNavigation().moveTo(cachedOwner, 1.1D);
                    getLookControl().setLookAt(cachedOwner, 20, 20);
                }
            }
        });
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity == this || (ownerId != null && ownerId.equals(entity.getUUID()))
                || entity instanceof RenanSantosEntity) { return true; }
        if (entity instanceof UkrainianFighterEntity unit && ownerId != null && ownerId.equals(unit.ownerId)) {
            return true;
        }
        return super.isAlliedTo(entity) || (cachedOwner != null && cachedOwner.isAlliedTo(entity));
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server && playerSummonToken != null
                && !SummonLedger.get(server).playerUnitActive(ownerId, getUUID(), playerSummonToken, server.getServer().overworld().getGameTime())) {
            vanish(server); return;
        }
        if (!level().isClientSide && isActionLocked()) { getNavigation().stop(); }
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) { return; }
        if (wasEmerging && !isActionLocked()) {
            for (Vector3f color : List.of(new Vector3f(0.1F, 0.35F, 1), new Vector3f(1, 0.8F, 0.05F))) {
                server.sendParticles(new DustParticleOptions(color, 0.9F), getX(), getY() + 0.25, getZ(),
                        8, 0.4, 0.2, 0.4, 0.01);
            }
            wasEmerging = false;
        }
        if (lifeTicks > 0 && --lifeTicks == 0) { vanish(server); return; }
        if (tickCount % 10 != 1) { return; }
        if (ownerId == null || SummonLedger.get(server).isClosed(ownerId)) { vanish(server); return; }
        Entity owner = server.getEntity(ownerId);
        cachedOwner = owner instanceof ArthurDoValEntity arthur && arthur.isAlive() ? arthur
                : owner instanceof Player player && player.isAlive() && !player.isSpectator() ? player : null;
        if (cachedOwner == null) {
            if (owner != null || (missingOwnerTicks += 10) >= ModConfig.ORPHAN_GRACE.get()) {
                vanish(server);
            }
            return;
        }
        missingOwnerTicks = 0;
        if (cachedOwner instanceof Player && playerSummonToken == null) { vanish(server); return; } // Retire legacy player units safely.
        if (!(cachedOwner instanceof Player)) { SummonLedger.get(server).register(ownerId, getUUID()); }
        LivingEntity target = cachedOwner instanceof Player player ? playerCombatTarget(player)
                : ((ArthurDoValEntity) cachedOwner).getTarget();
        if (!isActionLocked() && target != null && isValidAggressor(target)) { setTarget(target); }
    }

    /** Defend the player from whoever hit them, otherwise join whatever they recently attacked. */
    private static LivingEntity playerCombatTarget(Player player) {
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) { return attacker; }
        LivingEntity attacked = player.getLastHurtMob();
        return attacked != null && attacked.isAlive() && player.tickCount - player.getLastHurtMobTimestamp() < 100 ? attacked : null;
    }

    private void vanish(ServerLevel server) {
        server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8D, getZ(), 6, 0.2, 0.3, 0.2, 0.01);
        discard();
    }

    private void releaseSlot() {
        if (ownerId != null && level() instanceof ServerLevel server) {
            SummonLedger.get(server).release(ownerId, getUUID());
        }
    }

    @Override
    public void die(DamageSource source) { releaseSlot(); super.die(source); }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED
                || reason == RemovalReason.CHANGED_DIMENSION) { releaseSlot(); }
        super.remove(reason);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!ownerAllowsDamage(target)) { return false; }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            getMainHandItem().getItem().hurtEnemy(getMainHandItem(), living, this);
            if (cachedOwner instanceof ArthurDoValEntity arthur) { arthur.noteTroopAttack(living); }
        }
        return hit;
    }

    private boolean ownerAllowsDamage(Entity target) {
        if (!(target instanceof Player victim)) { return true; }
        LivingEntity owner = cachedOwner;
        if (level() instanceof ServerLevel server && ownerId != null) {
            if (server.getEntity(ownerId) instanceof Player player) { owner = player; }
            else if (playerSummonToken != null) { return false; }
        }
        return !(owner instanceof Player player) || player.canHarmPlayer(victim);
    }

    @Override protected boolean isValidAggressor(LivingEntity target) {
        return super.isValidAggressor(target) && ownerAllowsDamage(target);
    }

    @Override
    protected boolean shouldDropLoot() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) { tag.putUUID("Summoner", ownerId); }
        if (playerSummonToken != null) { tag.putUUID("PlayerSummonToken", playerSummonToken); }
        tag.putInt("LifeTicks", lifeTicks);
        tag.putInt("EmergenceRemaining", Math.max(0, (int) (entityData.get(EMERGENCE_DURATION)
                - (level().getGameTime() - entityData.get(EMERGENCE_START)))));
        tag.putInt("EmergenceDuration", entityData.get(EMERGENCE_DURATION));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Summoner") ? tag.getUUID("Summoner") : null;
        playerSummonToken = tag.hasUUID("PlayerSummonToken") ? tag.getUUID("PlayerSummonToken") : null;
        lifeTicks = tag.getInt("LifeTicks");
        int remaining = net.minecraft.util.Mth.clamp(tag.getInt("EmergenceRemaining"), 0, 260);
        int duration = tag.contains("EmergenceDuration") ? Math.max(44, tag.getInt("EmergenceDuration")) : 44;
        entityData.set(EMERGENCE_DURATION, duration);
        entityData.set(EMERGENCE_START, level().getGameTime() - duration + remaining);
        wasEmerging = remaining > 0;
        cachedOwner = null;
    }
}
