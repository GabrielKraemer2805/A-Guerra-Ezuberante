package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * An ox of Ronaldo Caiado's Boiada do agro: a cow that runs straight ahead for a while, hits each foe it runs over once
 * (a kick up and forward) and then goes back to the pasture (vanishes). It cannot be hurt, milked, bred or led and
 * drops nothing. Never saved.
 */
public final class StampedeOxEntity extends Cow {
    private LivingEntity owner;
    private Predicate<LivingEntity> foe = victim -> false;
    private Vec3 heading = Vec3.ZERO;
    private float damage;
    private final Set<UUID> hit = new HashSet<>();

    public StampedeOxEntity(EntityType<? extends Cow> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public void setup(LivingEntity owner, Vec3 heading, Predicate<LivingEntity> foe, float damage) {
        this.owner = owner;
        this.heading = heading.normalize();
        this.foe = foe;
        this.damage = damage;
    }

    public LivingEntity owner() { return owner; }

    /** No goals: it only runs where it is sent (a no-AI mob would not move at all). */
    @Override protected void registerGoals() {}

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server) {
            if (owner == null || tickCount > ModConfig.STAMPEDE_TICKS.get()) {
                server.sendParticles(ParticleTypes.POOF, getX(), getY() + .6, getZ(), 6, .4, .3, .4, .02);
                discard();
                return;
            }
            double speed = ModConfig.STAMPEDE_SPEED.get();
            setDeltaMovement(heading.x * speed, getDeltaMovement().y, heading.z * speed);
            // A step up over a block in the way, like a horse.
            if (horizontalCollision && onGround()) { setDeltaMovement(getDeltaMovement().add(0, .45, 0)); }
            trample(server);
            if (tickCount % 3 == 0) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), getX(), getY() + .1, getZ(), 3, .3, 0, .3, .1);
            }
            if (tickCount % 8 == 0) { playSound(SoundEvents.HORSE_GALLOP, .5F, .8F + random.nextFloat() * .2F); }
        }
        super.tick();
        setYRot((float) (Math.atan2(heading.z, heading.x) * 180 / Math.PI) - 90);
        yBodyRot = yHeadRot = getYRot();
    }

    private void trample(ServerLevel server) {
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(.3),
                victim -> victim != owner && !(victim instanceof StampedeOxEntity) && victim.isAlive() && !hit.contains(victim.getUUID()) && foe.test(victim))) {
            hit.add(victim.getUUID());
            DamageSource source = owner instanceof Player player ? server.damageSources().playerAttack(player) : server.damageSources().mobAttack(owner);
            if (victim.hurt(source, damage)) {
                victim.knockback(1.2, -heading.x, -heading.z);
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, .4, 0));
                victim.hurtMarked = true;
                playSound(SoundEvents.COW_HURT, 1, .6F);
            }
        }
    }

    /** How many it has already run over. */
    public int trampled() { return hit.size(); }

    @Override public boolean shouldDropExperience() { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) { return InteractionResult.PASS; }
    @Override public Cow getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override public boolean canFallInLove() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
}
