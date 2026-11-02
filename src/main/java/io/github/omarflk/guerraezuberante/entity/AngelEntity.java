package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.FigurePowers;
import io.github.omarflk.guerraezuberante.summon.OwnedSummon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.UUID;

/**
 * 0.028: an angel of Cabo Daciolo's Bible. Flies through walls like a vex, serves whoever holds the Bible (the player or
 * Daciolo himself), goes after that owner's enemies, never saved, and is gone when its time is up or the owner is.
 */
public final class AngelEntity extends Vex implements OwnedSummon {
    private static final double SEARCH = 16;
    /** 0.029: the angels of the Bible are not all alike. */
    public enum Kind {
        ANJO("anjo", 1, 14, 4), QUERUBIM("querubim", .7F, 8, 3), SERAFIM("serafim", 1, 14, 5),
        ARCANJO("arcanjo", 1.45F, 30, 8), GUARDIAO("guardiao", 1, 18, 2), TROMBETEIRO("trombeteiro", 1, 14, 3);
        public final String id;
        public final float size;
        public final double health, damage;
        Kind(String id, float size, double health, double damage) { this.id = id; this.size = size; this.health = health; this.damage = damage; }
    }
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(AngelEntity.class, EntityDataSerializers.INT);
    private UUID ownerId;
    private int lifeTicks = 400;

    public AngelEntity(EntityType<? extends AngelEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(KIND, Kind.ANJO.ordinal());
    }

    public Kind kind() { return Kind.values()[Math.floorMod(entityData.get(KIND), Kind.values().length)]; }

    public void setKind(Kind kind) {
        entityData.set(KIND, kind.ordinal());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind.health);
        setHealth(getMaxHealth());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(kind.damage);
        if (kind == Kind.ARCANJO) { setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD)); }
        if (kind == Kind.TROMBETEIRO) { setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOAT_HORN)); }
        setDropChance(EquipmentSlot.MAINHAND, 0);
        refreshDimensions();
    }

    @Override public float getScale() { return kind().size; }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (KIND.equals(key)) { refreshDimensions(); }
    }

    @Override
    protected net.minecraft.network.chat.Component getTypeName() {
        return kind() == Kind.ANJO ? super.getTypeName() : net.minecraft.network.chat.Component.translatable("entity.guerra_ezuberante.anjo." + kind().id);
    }

    /** The seraph sets on fire what it hits. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && kind() == Kind.SERAFIM) { target.setSecondsOnFire(4); }
        return hit;
    }

    /** What each kind does besides hitting: the guardian looks after its owner, the trumpeter scatters the enemies. */
    private void kindTick(ServerLevel server, LivingEntity owner) {
        switch (kind()) {
            case GUARDIAO -> {
                if (tickCount % 40 == 0 && distanceToSqr(owner) <= 24 * 24 && owner.getHealth() < owner.getMaxHealth()) {
                    owner.heal(2);
                    server.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 1.6, owner.getZ(), 3, .4, .3, .4, 0);
                }
            }
            case TROMBETEIRO -> {
                if (tickCount % 80 == 0 && getTarget() != null) {
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.HOSTILE, 1.2F, 1.2F);
                    server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + .5, getZ(), 1, 0, 0, 0, 0);
                    for (LivingEntity enemy : level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(6), other -> wants(owner, other))) {
                        enemy.hurt(damageSources().indirectMagic(this, owner), 3);
                        var away = enemy.position().subtract(position()).normalize();
                        enemy.push(away.x * .9, .35, away.z * .9);
                        enemy.hurtMarked = true;
                    }
                }
            }
            case SERAFIM -> {
                if (tickCount % 10 == 0) { server.sendParticles(ParticleTypes.FLAME, getX(), getY() + .4, getZ(), 3, .25, .3, .25, .01); }
            }
            default -> { }
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Vanilla vexes hunt players and copy their illager's target; these only look to their owner, in tick().
        targetSelector.removeAllGoals(goal -> true);
    }

    public void bindTo(LivingEntity owner, int lifetime) {
        ownerId = owner.getUUID();
        lifeTicks = lifetime;
        setBoundOrigin(owner.blockPosition());
    }

    @Override public UUID getOwnerId() { return ownerId; }

    public LivingEntity owner() {
        if (ownerId == null) { return null; }
        Player player = level().getPlayerByUUID(ownerId);
        if (player != null) { return player; }
        return level() instanceof ServerLevel server && server.getEntity(ownerId) instanceof LivingEntity owner ? owner : null;
    }

    private boolean wants(LivingEntity owner, LivingEntity target) {
        return target != null && target != this && target != owner && target.isAlive() && FigurePowers.foe(owner, target);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        LivingEntity owner = owner();
        return owner != null && wants(owner, target) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return ownerId != null && (ownerId.equals(entity.getUUID()) || entity instanceof OwnedSummon summon && ownerId.equals(summon.getOwnerId()))
                || super.isAlliedTo(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) { return; }
        if (--lifeTicks <= 0) { vanish(server); return; }
        LivingEntity owner = owner();
        if (owner != null && owner.isAlive()) { kindTick(server, owner); }
        if (tickCount % 10 != 0) { return; }
        if (owner == null || !owner.isAlive()) { vanish(server); return; }
        LivingEntity target = getTarget();
        if (target != null && !wants(owner, target)) { setTarget(null); }
        if (getTarget() == null) {
            level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(SEARCH), other -> wants(owner, other))
                    .stream().min(Comparator.comparingDouble(other -> other.distanceToSqr(this))).ifPresent(this::setTarget);
        }
        if (getTarget() == null) { setBoundOrigin(owner.blockPosition().above(2)); }
    }

    private void vanish(ServerLevel server) {
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + .5, getZ(), 8, .2, .3, .2, .02);
        discard();
    }

    @Override protected boolean shouldDropLoot() { return false; }
    @Override public int getExperienceReward() { return 0; }
}
