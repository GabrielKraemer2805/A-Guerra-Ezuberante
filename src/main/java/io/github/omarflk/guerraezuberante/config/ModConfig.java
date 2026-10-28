package io.github.omarflk.guerraezuberante.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ModConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SUMMON_ENABLED;
    public static final ForgeConfigSpec.IntValue SUMMON_COUNT, SUMMON_COOLDOWN, MAX_SUMMONS, EMERGENCE_TICKS;
    public static final ForgeConfigSpec.DoubleValue FIGHTER_HEALTH;
    public static final ForgeConfigSpec.IntValue SUMMON_LIFETIME, ORPHAN_GRACE;
    public static final ForgeConfigSpec.DoubleValue CHARACTER_AUDIO_VOLUME;
    public static final ForgeConfigSpec.IntValue CHARACTER_AUDIO_SESSION, CHARACTER_AUDIO_GRACE;
    public static final ForgeConfigSpec.DoubleValue LULA_HEALTH, LULA_EXPLOSION_POWER, LULA_RISE;
    public static final ForgeConfigSpec.BooleanValue LULA_EXPLOSION_ENABLED, LULA_BLOCK_DAMAGE;
    public static final ForgeConfigSpec.IntValue LULA_DEATH_TICKS;
    public static final ForgeConfigSpec.DoubleValue JAIR_HEALTH,JAIR_SPEED,JAIR_MELEE,JAIR_EXPLOSION_POWER,JAIR_RISE;
    public static final ForgeConfigSpec.IntValue JAIR_DEATH_TICKS,JAIR_SUMMON_COUNT,JAIR_SUMMON_COOLDOWN,JAIR_MAX_SUMMONS;
    public static final ForgeConfigSpec.BooleanValue JAIR_EXPLOSION_ENABLED,JAIR_BLOCK_DAMAGE,JAIR_SUMMON_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FLAVIO_HEALTH,FLAVIO_SPEED,FLAVIO_MELEE,FISSURE_RANGE,FISSURE_WIDTH,FISSURE_DAMAGE,FISSURE_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue FISSURE_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue FISSURE_BLOCK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue FISSURE_FINAL_RADIUS;
    public static final ForgeConfigSpec.IntValue BALANCE_REVISION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        BALANCE_REVISION=builder.comment("Internal migration marker; upgrades only unchanged legacy default values.")
                .defineInRange("balance_revision",0,0,1);
        builder.push("arthur");
        SUMMON_ENABLED = builder.define("arthur_summon_enabled", true);
        SUMMON_COUNT = builder.defineInRange("arthur_summon_count", 5, 1, 30);
        SUMMON_COOLDOWN = builder.defineInRange("arthur_summon_cooldown_ticks", 700, 20, 72000);
        MAX_SUMMONS = builder.defineInRange("arthur_max_active_summons", 30, 1, 200);
        FIGHTER_HEALTH = builder.defineInRange("ukrainian_fighter_max_health", 15.0D, 1.0D, 1024.0D);
        EMERGENCE_TICKS = builder.comment("Full articulated emergence; old values below 44 are migrated automatically.")
                .defineInRange("ukrainian_fighter_emergence_ticks", 44, 44, 200);
        SUMMON_LIFETIME = builder.comment("Lifetime while loaded; 6000 ticks = 5 minutes at 20 TPS.")
                .defineInRange("ukrainian_fighter_lifetime_ticks", 6000, 200, 72000);
        ORPHAN_GRACE = builder.comment("Grace period for an unloaded/missing owner before despawning.")
                .defineInRange("ukrainian_fighter_orphan_grace_ticks", 200, 20, 1200);
        builder.pop();
        builder.push("character_audio");
        CHARACTER_AUDIO_VOLUME = builder.defineInRange("volume", .8D, 0D, 2D);
        CHARACTER_AUDIO_SESSION = builder.comment("Manual session length; finish the current recording at expiry.")
                .defineInRange("interaction_session_ticks", 1200, 20, 72000);
        CHARACTER_AUDIO_GRACE = builder.comment("Combat grace; finish the current recording instead of cutting repeatedly.")
                .defineInRange("combat_grace_ticks", 40, 0, 1200);
        builder.pop();
        builder.push("lula");
        LULA_HEALTH = builder.defineInRange("max_health", 300D, 1D, 1024D);
        LULA_EXPLOSION_ENABLED = builder.define("death_explosion_enabled", true);
        LULA_EXPLOSION_POWER = builder.comment("Vanilla Wither spawn explosion is 7.0 in Minecraft 1.20.1.")
                .defineInRange("death_explosion_power", 7D, 0D, 16D);
        LULA_DEATH_TICKS = builder.defineInRange("death_animation_ticks", 60, 20, 200);
        LULA_RISE = builder.defineInRange("death_rise_blocks", 2D, 0D, 8D);
        LULA_BLOCK_DAMAGE = builder.comment("Also respects mobGriefing and Forge explosion protection events.")
                .define("death_explosion_block_damage", true);
        builder.pop();
        builder.push("jair");
        JAIR_HEALTH=builder.defineInRange("max_health",300D,1D,1024D);
        JAIR_SPEED=builder.defineInRange("movement_speed",.24D,.05D,1D);
        JAIR_MELEE=builder.defineInRange("fallback_melee_damage",4D,0D,100D);
        JAIR_EXPLOSION_ENABLED=builder.define("death_explosion_enabled",true);
        JAIR_EXPLOSION_POWER=builder.defineInRange("death_explosion_power",7D,0D,16D);
        JAIR_RISE=builder.defineInRange("death_rise_blocks",2D,0D,8D);
        JAIR_DEATH_TICKS=builder.defineInRange("death_animation_ticks",60,20,200);
        JAIR_BLOCK_DAMAGE=builder.define("death_explosion_block_damage",true);
        JAIR_SUMMON_ENABLED=builder.define("summon_enabled",true);
        JAIR_SUMMON_COUNT=builder.defineInRange("summon_count",1,1,3);
        JAIR_SUMMON_COOLDOWN=builder.defineInRange("summon_cooldown_ticks",700,40,72000);
        JAIR_MAX_SUMMONS=builder.defineInRange("max_active_flavios",3,1,12);
        builder.pop();builder.push("flavio");
        FLAVIO_HEALTH=builder.defineInRange("max_health",30D,1D,1024D);
        FLAVIO_SPEED=builder.defineInRange("movement_speed",.25D,.05D,1D);
        FLAVIO_MELEE=builder.defineInRange("melee_damage",4D,0D,100D);
        FISSURE_RANGE=builder.defineInRange("rachadinha_range",8D,2D,16D);
        FISSURE_WIDTH=builder.defineInRange("rachadinha_width",1D,.5D,2D);
        FISSURE_DAMAGE=builder.defineInRange("rachadinha_damage",10D,0D,100D);
        FISSURE_KNOCKBACK=builder.defineInRange("rachadinha_knockback",.65D,0D,2D);
        FISSURE_COOLDOWN=builder.defineInRange("rachadinha_cooldown_ticks",120,40,72000);
        FISSURE_FINAL_RADIUS=builder.defineInRange("rachadinha_final_radius",2.25D,.65D,5D);
        FISSURE_BLOCK_DAMAGE=builder.comment("Narrow surface break only; respects mobGriefing, block hardness and Forge destroy events.")
                .define("rachadinha_block_damage",true);
        builder.pop();
        SPEC = builder.build();
    }

    private ModConfig() {}
}
