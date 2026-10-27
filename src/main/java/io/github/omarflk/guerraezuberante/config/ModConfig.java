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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
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
        LULA_HEALTH = builder.defineInRange("max_health", 30D, 1D, 1024D);
        LULA_EXPLOSION_ENABLED = builder.define("death_explosion_enabled", true);
        LULA_EXPLOSION_POWER = builder.comment("Vanilla Wither spawn explosion is 7.0 in Minecraft 1.20.1.")
                .defineInRange("death_explosion_power", 7D, 0D, 16D);
        LULA_DEATH_TICKS = builder.defineInRange("death_animation_ticks", 60, 20, 200);
        LULA_RISE = builder.defineInRange("death_rise_blocks", 2D, 0D, 8D);
        LULA_BLOCK_DAMAGE = builder.comment("Also respects mobGriefing and Forge explosion protection events.")
                .define("death_explosion_block_damage", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ModConfig() {}
}
