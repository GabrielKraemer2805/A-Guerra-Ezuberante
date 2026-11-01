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
    public static final ForgeConfigSpec.IntValue LULA_DEATH_TICKS,LULA_FAZ_O_L_HITS,LULA_PROTEST_HITS;
    public static final ForgeConfigSpec.DoubleValue LULA_FAZ_O_L_CHANCE,LULA_PROTEST_CHANCE;
    public static final ForgeConfigSpec.BooleanValue LULA_FAZ_O_L_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue JAIR_HEALTH,JAIR_SPEED,JAIR_MELEE,JAIR_EXPLOSION_POWER,JAIR_RISE,JAIR_CANALHA_CHANCE;
    public static final ForgeConfigSpec.IntValue JAIR_DEATH_TICKS,JAIR_SUMMON_COUNT,JAIR_SUMMON_COOLDOWN,JAIR_MAX_SUMMONS;
    public static final ForgeConfigSpec.BooleanValue JAIR_EXPLOSION_ENABLED,JAIR_BLOCK_DAMAGE,JAIR_SUMMON_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FLAVIO_HEALTH,FLAVIO_SPEED,FLAVIO_MELEE,FISSURE_RANGE,FISSURE_WIDTH,FISSURE_DAMAGE,FISSURE_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue FISSURE_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue FISSURE_BLOCK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue FISSURE_FINAL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue HADDAD_HEALTH,HADDAD_MELEE,SLAP_DAMAGE,HADDAD_DAMAGE_VOICE_CHANCE,HADDAD_LOW_HEALTH_FRACTION,BOULOS_HEALTH,BOULOS_MELEE,BOULOS_ARMOR,BOULOS_PO_CHANCE;
    public static final ForgeConfigSpec.IntValue SLAP_COUNT,SLAP_PARALYSIS_TICKS,SLAP_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue RIVALRY_ENABLED;
    public static final ForgeConfigSpec.DoubleValue DILMA_HEALTH,MANDIOCA_DAMAGE,WIND_RANGE,WIND_PUSH,SEM_AR_DAMAGE,LINK_RANGE,LINK_HEAL,LINK_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue MANDIOCA_COUNT,MANDIOCA_INTERVAL,WIND_COOLDOWN,WIND_BREATHLESS_TICKS;
    public static final ForgeConfigSpec.DoubleValue RIVALRY_RANGE;
    public static final ForgeConfigSpec.IntValue BALANCE_REVISION;
    public static final ForgeConfigSpec.DoubleValue RENAN_HEALTH,ARTHUR_HEALTH,RENAN_EXPLOSION_POWER,RENAN_RISE;
    public static final ForgeConfigSpec.BooleanValue RENAN_EXPLOSION_ENABLED,RENAN_BLOCK_DAMAGE,ENEIAS_NUKE_ENABLED,ENEIAS_NUKE_BREAK_BLOCKS;
    public static final ForgeConfigSpec.IntValue RENAN_DEATH_TICKS,ENEIAS_NUKE_COUNTDOWN,ENEIAS_NUKE_CRATER_RADIUS,ENEIAS_NUKE_BLOCKS_PER_TICK,
            PL_MANDIOCA_COOLDOWN,PL_MANDIOCA_COUNT,PL_WIND_COOLDOWN,PL_WIND_BREATHLESS_TICKS,PL_SLAP_COOLDOWN,PL_SLAP_PARALYSIS_TICKS,
            PL_OCCUPATION_COOLDOWN,PL_OCCUPATION_TICKS,PD_BOMB_COOLDOWN,PD_DIPLOMA_COOLDOWN,PD_DIPLOMA_COUNT,PD_SHOUT_COOLDOWN,
            PD_SHOOTER_COOLDOWN,PD_SHOOTER_COUNT,PD_BOMB_DRONE_COOLDOWN,PD_REGEN_COOLDOWN,PD_DRONE_LIFETIME;
    /** 0.022: Romeu Zema and Ronaldo Caiado, characters of the Direita village, and their powers in the Direita tree. */
    public static final ForgeConfigSpec.IntValue ZEMA_SALE_COOLDOWN,ZEMA_PRIVATIZE_COOLDOWN,CAIADO_LASSO_COOLDOWN,CAIADO_STAMPEDE_COOLDOWN,
            SALE_COUNT,PRIVATIZE_PARALYSIS_TICKS,STAMPEDE_COUNT,STAMPEDE_TICKS,PD_SALE_COOLDOWN,PD_PRIVATIZE_COOLDOWN,PD_LASSO_COOLDOWN,
            PD_STAMPEDE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue ZEMA_HEALTH,CAIADO_HEALTH,SALE_DAMAGE,SALE_RADIUS,PRIVATIZE_DAMAGE,PRIVATIZE_RANGE,
            LASSO_DAMAGE,LASSO_RANGE,STAMPEDE_DAMAGE,STAMPEDE_SPEED;
    public static final ForgeConfigSpec.DoubleValue ENEIAS_NUKE_HEALTH,ENEIAS_NUKE_RADIUS,ENEIAS_NUKE_LETHAL_RADIUS,ENEIAS_NUKE_DAMAGE,
            PL_MANDIOCA_DAMAGE,PL_WIND_RANGE,PL_WIND_PUSH,PL_SLAP_DAMAGE,PL_SLAP_RANGE,PL_OCCUPATION_ABSORPTION,PL_OCCUPATION_TAUNT_RADIUS,
            PD_BOMB_DAMAGE,PD_DIPLOMA_DAMAGE,PD_SHOUT_RADIUS;
    public static final ForgeConfigSpec.DoubleValue ENEIAS_HEALTH,ENEIAS_EXPLOSION_POWER,ENEIAS_RISE,ENEIAS_BOMB_DAMAGE,ENEIAS_BOMB_RADIUS,
            ENEIAS_DIPLOMA_DAMAGE,ENEIAS_SHOUT_RADIUS,CURY_HEALTH,DRONE_HEALTH,DRONE_SHOT_DAMAGE,DRONE_BOMB_DAMAGE,DRONE_BOMB_RADIUS,
            DRONE_HEAL,DRONE_HEAL_RADIUS,GUARD_HEALTH,GUARD_RANGE,GUARD_PISTOL_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue ENEIAS_EXPLOSION_ENABLED,ENEIAS_BLOCK_DAMAGE,GUARDS_ENABLED;
    public static final ForgeConfigSpec.IntValue ENEIAS_DEATH_TICKS,ENEIAS_BOMB_COOLDOWN,ENEIAS_DIPLOMA_COUNT,ENEIAS_DIPLOMA_COOLDOWN,
            ENEIAS_SHOUT_COOLDOWN,CURY_SHOOTER_DRONES,CURY_BOMB_DRONES,CURY_REGEN_DRONES,CURY_SUMMON_INTERVAL,CURY_BOMB_COOLDOWN,
            DRONE_SHOT_INTERVAL,GUARD_SHOT_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue FACTION_KIT_ENABLED;
    public static final ForgeConfigSpec.IntValue BEAN_VOLLEY_COOLDOWN,BEAN_VOLLEY_COUNT,PLAYER_FISSURE_COOLDOWN,PLAYER_TROOPS_COOLDOWN,
            PLAYER_TROOPS_COUNT,PLAYER_TROOPS_LIFETIME,ESQUERDA_REGEN_DELAY,
            ESQUERDA_REGEN_INTERVAL,DASH_COOLDOWN,DASH_BUFF_TICKS,STANCE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue BEAN_VOLLEY_DAMAGE,LULISTA_SATURATION_BONUS,PLAYER_FISSURE_DAMAGE,
            PLAYER_FISSURE_RANGE,BOLSONARISTA_ARMOR,MISSAO_HEALTH,DASH_STRENGTH,DIREITA_SPEED;
    public static final ForgeConfigSpec.IntValue MAX_LEVEL,POINTS_PER_LEVEL,STAT_MAX,XP_BASE,XP_PER_LEVEL,XP_HOSTILE,XP_NPC,XP_BOSS,XP_PLAYER,
            CONVICTION_SECOND_POWER,CONVICTION_THIRD_POWER;
    public static final ForgeConfigSpec.DoubleValue XP_RIVAL_MULTIPLIER,XP_RIVAL_LEADER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STRENGTH_PER_POINT,SPEED_PER_POINT,HEALTH_PER_POINT,IDEOLOGY_PER_POINT,CONVICTION_PER_POINT;
    public static final ForgeConfigSpec.IntValue STOMP_COOLDOWN,FAZ_O_L_COOLDOWN,FAZ_O_L_ITEMS,ATHLETE_COOLDOWN,ATHLETE_TICKS,GRAVEDIGGER_COOLDOWN,
            METEOR_COOLDOWN,METEOR_COUNT,BOOK_COOLDOWN,BOOK_TICKS,BOOK_INTERVAL,BORED_TICKS,PROTEST_COOLDOWN,PROTEST_COUNT,PROTEST_LIFETIME,
            LEAP_COOLDOWN,PUSH_COOLDOWN,EXEMPTION_COOLDOWN,EXEMPTION_TICKS,DRAIN_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue STOMP_DAMAGE,STOMP_RADIUS,FAZ_O_L_DAMAGE,FAZ_O_L_RADIUS,GRAVEDIGGER_DAMAGE,METEOR_DAMAGE,
            METEOR_RADIUS,BOOK_DAMAGE,LEAP_DAMAGE,PUSH_DAMAGE,PUSH_RADIUS,DRAIN_AMOUNT,DRAIN_RADIUS;
    public static final ForgeConfigSpec.BooleanValue FAZ_O_L_BREAK_BLOCKS,GRAVEDIGGER_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue JONES_HEALTH,JONES_EXPLOSION_POWER,JONES_RISE,JONES_LIVE_RADIUS,JONES_FREEZE_RANGE,
            TABATA_HEALTH,TABATA_SHIELD_ABSORPTION,TABATA_SHIELD_TRIGGER,MARINA_HEALTH,MARINA_VANISH_HEALTH,MARINA_ROOTS_RADIUS,
            CIRO_HEALTH,CIRO_MELEE,CIRO_TEMPER_RADIUS,CIRO_PARIS_HEALTH;
    public static final ForgeConfigSpec.BooleanValue JONES_EXPLOSION_ENABLED,JONES_BLOCK_DAMAGE,MARINA_ROOTS_LEAVES;
    public static final ForgeConfigSpec.IntValue JONES_DEATH_TICKS,JONES_LIVE_COOLDOWN,JONES_LIVE_TICKS,JONES_FREEZE_COOLDOWN,JONES_FREEZE_TICKS,
            JONES_COMMITTEE_COOLDOWN,JONES_COMMITTEE_COUNT,TABATA_TURNCOAT_COOLDOWN,TABATA_TURNCOAT_TICKS,TABATA_SHIELD_COOLDOWN,
            MARINA_VANISH_COOLDOWN,MARINA_VANISH_TICKS,MARINA_ROOTS_COOLDOWN,MARINA_ROOTS_TICKS,MARINA_ROOTS_POISON_TICKS,CIRO_TEMPER_COOLDOWN,CIRO_TEMPER_TICKS,
            CIRO_PARIS_COOLDOWN,CIRO_PARIS_TICKS;
    public static final ForgeConfigSpec.IntValue FREE_CLASS_CHANGES,SUPORTE_REGEN_DELAY,SUPORTE_REGEN_INTERVAL,CHARGE_COOLDOWN,TAUNT_COOLDOWN,
            TAUNT_RESISTANCE_TICKS,GUARD_COOLDOWN,GUARD_LIFETIME,QUICK_RELOAD_COOLDOWN,CLASS_HEAL_COOLDOWN,TREE_POINTS_PER_CONVICTION,
            MORAL_POWER_USE,MORAL_KILL;
    public static final ForgeConfigSpec.DoubleValue COMBATENTE_HEALTH,COMBATENTE_ATTACK,TANQUE_ARMOR,TANQUE_TOUGHNESS,TANQUE_KNOCKBACK,
            INVOCADOR_SPEED,INVOCADOR_SUMMON_BONUS,ESPECIALISTA_COOLDOWN,ESPECIALISTA_POWER,SUPORTE_HEALTH,CHARGE_DAMAGE,CHARGE_STRENGTH,
            TAUNT_RADIUS,CLASS_HEAL_AMOUNT,CLASS_HEAL_RADIUS,NODE_POWER_BONUS,NODE_COOLDOWN_REDUCTION,NODE_CLASS_COOLDOWN_REDUCTION;
    public static final ForgeConfigSpec.IntValue PE_TURNCOAT_COOLDOWN,PE_TURNCOAT_TICKS,PE_CONSCIENCE_COOLDOWN,PE_ROOTS_COOLDOWN,PE_ROOTS_TICKS,
            PE_VANISH_COOLDOWN,PE_VANISH_TICKS,PE_TEMPER_COOLDOWN,PE_TEMPER_TICKS,PE_PARIS_COOLDOWN,PE_PARIS_TICKS,PE_LIVE_COOLDOWN,PE_LIVE_TICKS,
            PE_FREEZE_COOLDOWN,PE_FREEZE_TICKS,PE_COMMITTEE_COOLDOWN,PE_COMMITTEE_COUNT,PE_COMMITTEE_LIFETIME,PE_ROOTS_POISON_TICKS;
    public static final ForgeConfigSpec.BooleanValue PE_ROOTS_LEAVES;
    public static final ForgeConfigSpec.DoubleValue PE_TURNCOAT_RANGE,PE_CONSCIENCE_ABSORPTION,PE_ROOTS_RADIUS,PE_TEMPER_RADIUS,PE_PARIS_HEALTH,
            PE_LIVE_RADIUS,PE_FREEZE_RANGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        BALANCE_REVISION=builder.comment("Internal migration marker; upgrades only unchanged legacy default values.")
                .defineInRange("balance_revision",0,0,4);
        builder.comment("Renan Santos, leader of the Missão village.").push("renan");
        RENAN_HEALTH=builder.comment("A boss since 0.020, with a yellow boss bar.").defineInRange("max_health",300D,1D,1024D);
        RENAN_EXPLOSION_ENABLED=builder.define("death_explosion_enabled",true);
        RENAN_EXPLOSION_POWER=builder.defineInRange("death_explosion_power",7D,0D,16D);
        RENAN_DEATH_TICKS=builder.defineInRange("death_animation_ticks",60,20,200);
        RENAN_RISE=builder.defineInRange("death_rise_blocks",2D,0D,8D);
        RENAN_BLOCK_DAMAGE=builder.define("death_explosion_block_damage",true);
        builder.pop();
        builder.push("arthur");
        ARTHUR_HEALTH=builder.comment("Both Arthur do Val variants.").defineInRange("arthur_max_health",80D,1D,1024D);
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
        CHARACTER_AUDIO_SESSION = builder.comment("Legacy setting retained for old configs. Each activation now plays one recording, without autoplay.")
                .defineInRange("interaction_session_ticks", 1200, 20, 72000);
        CHARACTER_AUDIO_GRACE = builder.comment("Legacy setting retained for old configs. Combat now starts one recording per encounter.")
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
        LULA_FAZ_O_L_HITS = builder.comment("Every this many hits Lula takes, he may throw a Faz o L at the attacker.",
                "Damage and radius are the player power's: [faction_powers] lulista_faz_o_l_damage/radius.").defineInRange("faz_o_l_every_hits", 10, 1, 100);
        LULA_FAZ_O_L_CHANCE = builder.defineInRange("faz_o_l_chance", .5D, 0D, 1D);
        LULA_FAZ_O_L_BREAK_BLOCKS = builder.comment("The blast breaks blocks like Lula's death explosion, his castle included. Also respects mobGriefing.")
                .define("faz_o_l_breaks_blocks", true);
        LULA_PROTEST_HITS = builder.comment("Every this many hits Lula takes, he may call a Manifestacao (Manifestantes on his side).",
                "Count and lifetime are the Esquerda power's: [faction_powers] esquerda_manifestando_zombies/lifetime_ticks.")
                .defineInRange("manifestacao_every_hits", 7, 1, 100);
        LULA_PROTEST_CHANCE = builder.defineInRange("manifestacao_chance", .3D, 0D, 1D);
        builder.pop();
        builder.push("jair");
        JAIR_HEALTH=builder.defineInRange("max_health",300D,1D,1024D);
        JAIR_SPEED=builder.defineInRange("movement_speed",.24D,.05D,1D);
        JAIR_MELEE=builder.defineInRange("fallback_melee_damage",4D,0D,100D);
        JAIR_EXPLOSION_ENABLED=builder.define("death_explosion_enabled",true);
        JAIR_EXPLOSION_POWER=builder.defineInRange("death_explosion_power",7D,0D,16D);
        JAIR_RISE=builder.defineInRange("death_rise_blocks",2D,0D,8D);
        JAIR_CANALHA_CHANCE=builder.comment("Chance of saying \"Canalha\" each time Jair takes damage (never over another line).")
                .defineInRange("canalha_chance",.4D,0D,1D);
        JAIR_DEATH_TICKS=builder.defineInRange("death_animation_ticks",60,20,200);
        JAIR_BLOCK_DAMAGE=builder.define("death_explosion_block_damage",true);
        JAIR_SUMMON_ENABLED=builder.define("summon_enabled",true);
        JAIR_SUMMON_COUNT=builder.defineInRange("summon_count",1,1,3);
        JAIR_SUMMON_COOLDOWN=builder.defineInRange("summon_cooldown_ticks",700,40,72000);
        JAIR_MAX_SUMMONS=builder.defineInRange("max_active_flavios",3,1,12);
        builder.pop();builder.push("flavio");
        FLAVIO_HEALTH=builder.defineInRange("max_health",80D,1D,1024D);
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
        builder.comment("Lulista allies of Lula. They defend the village: hostile mobs and players of other factions nearby.").push("haddad");
        HADDAD_HEALTH=builder.defineInRange("max_health",80D,1D,1024D);
        HADDAD_MELEE=builder.defineInRange("melee_damage",3D,0D,100D);
        SLAP_COUNT=builder.defineInRange("slap_count",3,1,10);
        SLAP_DAMAGE=builder.comment("Damage of each slap; armor applies.").defineInRange("slap_damage",3D,0D,100D);
        SLAP_PARALYSIS_TICKS=builder.comment("Paralysis after each slap that really hurts: no walking, no jumping.")
                .defineInRange("slap_paralysis_ticks",60,0,1200);
        SLAP_COOLDOWN=builder.comment("From the start of one slap combo to the next.").defineInRange("slap_cooldown_ticks",200,40,72000);
        HADDAD_DAMAGE_VOICE_CHANCE=builder.comment("Chance of a damage line each time Haddad takes damage (never over another line).")
                .defineInRange("damage_voice_chance",1D,0D,1D);
        HADDAD_LOW_HEALTH_FRACTION=builder.comment("Below this fraction of max health the damage line becomes \"perdeu né seu tonto\".")
                .defineInRange("low_health_voice_fraction",.2D,0D,1D);
        builder.pop();
        builder.push("boulos");
        BOULOS_HEALTH=builder.defineInRange("max_health",100D,1D,1024D);
        BOULOS_MELEE=builder.defineInRange("melee_damage",5D,0D,100D);
        BOULOS_ARMOR=builder.defineInRange("armor",6D,0D,30D);
        BOULOS_PO_CHANCE=builder.comment("Chance of playing \"o pó\" each time Boulos takes damage (never over itself).")
                .defineInRange("po_chance",.4D,0D,1D);
        builder.pop();
        builder.comment("Dilma, Lulista ally who fights from a distance: mandioca bursts and Estocar vento.").push("dilma");
        DILMA_HEALTH=builder.defineInRange("max_health",150D,1D,1024D);
        MANDIOCA_COUNT=builder.comment("Mandiocas in each burst, fanned 8 degrees apart.").defineInRange("mandioca_count",3,1,9);
        MANDIOCA_DAMAGE=builder.comment("Damage of each mandioca.").defineInRange("mandioca_damage",3D,0D,100D);
        MANDIOCA_INTERVAL=builder.defineInRange("mandioca_interval_ticks",60,10,72000);
        WIND_COOLDOWN=builder.comment("Estocar vento: 2 s with the arms turning, then either a push or Sem ar (drawn each use).")
                .defineInRange("estocar_vento_cooldown_ticks",300,40,72000);
        WIND_RANGE=builder.defineInRange("estocar_vento_range",12D,2D,32D);
        WIND_PUSH=builder.comment("Horizontal push strength; knockback resistance reduces it.").defineInRange("estocar_vento_push",1.6D,0D,5D);
        WIND_BREATHLESS_TICKS=builder.defineInRange("estocar_vento_sem_ar_ticks",100,20,1200);
        SEM_AR_DAMAGE=builder.comment("Drowning damage per second while the air bar is empty under Sem ar.").defineInRange("sem_ar_damage_per_second",2D,0D,100D);
        LINK_RANGE=builder.comment("Red beam between Dilma and Lula, like the End crystal's: both alive, this close and in sight of each other.")
                .defineInRange("lula_link_range",12D,2D,32D);
        LINK_HEAL=builder.comment("Health Lula gets back per second while the beam is on.").defineInRange("lula_link_heal_per_second",4D,0D,100D);
        LINK_MULTIPLIER=builder.comment("While the beam is on: mandioca damage, push strength and Sem ar damage are multiplied by this.")
                .defineInRange("lula_link_attack_multiplier",2D,1D,10D);
        builder.pop();
        builder.comment("Characters of different factions (Lula, Jair, Renan, Arthur, Haddad...) attack each other on sight;",
                "characters of the same faction are allies. Players are not affected.").push("faction_rivalry");
        RIVALRY_ENABLED=builder.define("enabled",true);
        RIVALRY_RANGE=builder.comment("Blocks within which a character notices a rival it can see.").defineInRange("range",16D,2D,32D);
        builder.pop();
        builder.comment("Player faction powers (B key), passives and the one-time starting kit.").push("faction_powers");
        FACTION_KIT_ENABLED=builder.comment("Kit is given once per player per world, only on the player's own first choice.")
                .define("starting_kit_enabled",true);
        BEAN_VOLLEY_COOLDOWN=builder.defineInRange("lulista_bean_volley_cooldown_ticks",160,20,72000);
        BEAN_VOLLEY_COUNT=builder.defineInRange("lulista_bean_volley_count",3,1,9);
        BEAN_VOLLEY_DAMAGE=builder.defineInRange("lulista_bean_volley_damage",5D,0D,100D);
        LULISTA_SATURATION_BONUS=builder.comment("Extra saturation as a fraction of what the food already gives.")
                .defineInRange("lulista_food_saturation_bonus",.5D,0D,4D);
        PLAYER_FISSURE_COOLDOWN=builder.defineInRange("bolsonarista_rachadinha_cooldown_ticks",300,40,72000);
        PLAYER_FISSURE_DAMAGE=builder.defineInRange("bolsonarista_rachadinha_damage",8D,0D,100D);
        PLAYER_FISSURE_RANGE=builder.defineInRange("bolsonarista_rachadinha_range",8D,2D,16D);
        BOLSONARISTA_ARMOR=builder.defineInRange("bolsonarista_armor_bonus",2D,0D,20D);
        PLAYER_TROOPS_COOLDOWN=builder.defineInRange("missao_ukrainian_army_cooldown_ticks",900,40,72000);
        PLAYER_TROOPS_COUNT=builder.comment("Also the maximum active at once per player.")
                .defineInRange("missao_ukrainian_army_count",2,1,10);
        PLAYER_TROOPS_LIFETIME=builder.defineInRange("missao_ukrainian_army_lifetime_ticks",600,200,72000);
        MISSAO_HEALTH=builder.defineInRange("missao_max_health_bonus",2D,0D,40D);
        ESQUERDA_REGEN_DELAY=builder.comment("Ticks without taking damage before the passive regeneration starts.")
                .defineInRange("esquerda_passive_regen_delay_ticks",200,20,6000);
        ESQUERDA_REGEN_INTERVAL=builder.defineInRange("esquerda_passive_regen_interval_ticks",80,20,6000);
        DASH_COOLDOWN=builder.defineInRange("direita_dash_cooldown_ticks",240,40,72000);
        DASH_STRENGTH=builder.comment("Horizontal launch speed; travel distance depends on ground, effects and movement, not a fixed teleport.")
                .defineInRange("direita_dash_strength",1.3D,.2D,5D);
        DASH_BUFF_TICKS=builder.defineInRange("direita_dash_strength_effect_ticks",100,20,1200);
        DIREITA_SPEED=builder.comment("Fraction of base movement speed; 0.1 = +10%.")
                .defineInRange("direita_speed_bonus",.1D,0D,1D);
        STANCE_COOLDOWN=builder.defineInRange("centro_stance_switch_cooldown_ticks",100,20,72000);
        builder.pop();
        builder.comment("Faction level, XP from kills and the five upgradeable stats (J key).").push("progression");
        MAX_LEVEL=builder.comment("Worlds still on the old default (30) move to 100 once; other values are kept.").defineInRange("max_level",100,2,200);
        POINTS_PER_LEVEL=builder.defineInRange("points_per_level",1,1,10);
        STAT_MAX=builder.defineInRange("stat_max_points",10,1,50);
        XP_BASE=builder.comment("XP from level n to n+1 = xp_base + xp_per_level * n.").defineInRange("xp_base",40,1,100000);
        XP_PER_LEVEL=builder.defineInRange("xp_per_level",20,0,100000);
        XP_HOSTILE=builder.defineInRange("xp_hostile_mob",10,0,100000);
        XP_NPC=builder.defineInRange("xp_mod_character",40,0,100000);
        XP_BOSS=builder.comment("Characters with a boss bar and vanilla bosses.").defineInRange("xp_boss",200,0,100000);
        XP_PLAYER=builder.comment("Only for players of another faction or without one.").defineInRange("xp_enemy_player",60,0,100000);
        XP_RIVAL_MULTIPLIER=builder.comment("Multiplies the XP of a mod character of another faction than the killer's (Haddad for a Bolsonarista).")
                .defineInRange("xp_rival_character_multiplier",2D,1D,20D);
        XP_RIVAL_LEADER_MULTIPLIER=builder.comment("Same, for the leader of another faction (Lula, Jair, Renan). Replaces the multiplier above.")
                .defineInRange("xp_rival_leader_multiplier",4D,1D,20D);
        STRENGTH_PER_POINT=builder.defineInRange("strength_attack_damage_per_point",.5D,0D,10D);
        SPEED_PER_POINT=builder.comment("Fraction of base movement speed per point.").defineInRange("speed_bonus_per_point",.02D,0D,.5D);
        HEALTH_PER_POINT=builder.defineInRange("health_bonus_per_point",1D,0D,20D);
        IDEOLOGY_PER_POINT=builder.comment("Power damage (or duration, for powers without damage) per point.").defineInRange("ideological_strength_per_point",.05D,0D,1D);
        CONVICTION_PER_POINT=builder.comment("Cooldown reduction per point.").defineInRange("conviction_cooldown_reduction_per_point",.02D,0D,.09D);
        CONVICTION_SECOND_POWER=builder.comment("Since 0.017 the powers are bought in the skill tree; these two only migrate older players once.")
                .defineInRange("conviction_for_second_power",5,0,50);
        CONVICTION_THIRD_POWER=builder.defineInRange("conviction_for_third_power",10,0,50);
        builder.pop();
        builder.comment("Second and third faction powers, bought in the skill tree.").push("conviction_powers");
        STOMP_COOLDOWN=builder.defineInRange("lulista_fim_da_escala_cooldown_ticks",400,20,72000);
        STOMP_DAMAGE=builder.defineInRange("lulista_fim_da_escala_damage",6D,0D,100D);
        STOMP_RADIUS=builder.defineInRange("lulista_fim_da_escala_radius",6D,1D,16D);
        FAZ_O_L_COOLDOWN=builder.defineInRange("lulista_faz_o_l_cooldown_ticks",900,20,72000);
        FAZ_O_L_DAMAGE=builder.defineInRange("lulista_faz_o_l_damage",10D,0D,100D);
        FAZ_O_L_RADIUS=builder.comment("Damage radius. The blast itself (blocks and visuals) is the same as Lula's death: [lula] death_explosion_power.")
                .defineInRange("lulista_faz_o_l_radius",7D,1D,16D);
        FAZ_O_L_BREAK_BLOCKS=builder.define("faz_o_l_break_blocks",true);
        FAZ_O_L_ITEMS=builder.comment("Items knocked out of a surviving player of another faction.").defineInRange("lulista_faz_o_l_dropped_items",2,0,36);
        ATHLETE_COOLDOWN=builder.defineInRange("bolsonarista_historico_de_atleta_cooldown_ticks",900,20,72000);
        ATHLETE_TICKS=builder.defineInRange("bolsonarista_historico_de_atleta_ticks",300,20,6000);
        GRAVEDIGGER_COOLDOWN=builder.defineInRange("bolsonarista_nao_sou_coveiro_cooldown_ticks",1200,20,72000);
        GRAVEDIGGER_DAMAGE=builder.defineInRange("bolsonarista_nao_sou_coveiro_damage",10D,0D,100D);
        GRAVEDIGGER_BREAK_BLOCKS=builder.define("nao_sou_coveiro_break_blocks",true);
        METEOR_COOLDOWN=builder.defineInRange("missao_aborto_zacarias_cooldown_ticks",600,20,72000);
        METEOR_COUNT=builder.defineInRange("missao_aborto_zacarias_meteors",20,1,100);
        METEOR_DAMAGE=builder.defineInRange("missao_aborto_zacarias_damage_per_meteor",2D,0D,100D);
        METEOR_RADIUS=builder.defineInRange("missao_aborto_zacarias_radius",7D,2D,16D);
        BOOK_COOLDOWN=builder.comment("Starts when the power is used, so it should be longer than the 10 s volley.")
                .defineInRange("missao_livro_amarelo_cooldown_ticks",900,20,72000);
        BOOK_TICKS=builder.defineInRange("missao_livro_amarelo_duration_ticks",200,20,1200);
        BOOK_INTERVAL=builder.comment("10 = 2 books per second.").defineInRange("missao_livro_amarelo_interval_ticks",10,2,100);
        BOOK_DAMAGE=builder.defineInRange("missao_livro_amarelo_damage",1D,0D,100D);
        BORED_TICKS=builder.defineInRange("missao_livro_amarelo_bored_ticks",40,10,600);
        PROTEST_COOLDOWN=builder.defineInRange("esquerda_manifestando_cooldown_ticks",900,20,72000);
        PROTEST_COUNT=builder.defineInRange("esquerda_manifestando_zombies",10,1,20);
        PROTEST_LIFETIME=builder.defineInRange("esquerda_manifestando_lifetime_ticks",300,20,6000);
        LEAP_COOLDOWN=builder.defineInRange("direita_salto_liberal_cooldown_ticks",300,20,72000);
        LEAP_DAMAGE=builder.defineInRange("direita_salto_liberal_damage",6D,0D,100D);
        PUSH_COOLDOWN=builder.defineInRange("direita_mao_invisivel_cooldown_ticks",400,20,72000);
        PUSH_DAMAGE=builder.defineInRange("direita_mao_invisivel_damage",6D,0D,100D);
        PUSH_RADIUS=builder.defineInRange("direita_mao_invisivel_radius",6D,1D,16D);
        EXEMPTION_COOLDOWN=builder.defineInRange("centro_isencao_cooldown_ticks",600,20,72000);
        EXEMPTION_TICKS=builder.defineInRange("centro_isencao_ticks",60,10,600);
        DRAIN_COOLDOWN=builder.defineInRange("centro_toma_la_da_ca_cooldown_ticks",600,20,72000);
        DRAIN_AMOUNT=builder.defineInRange("centro_toma_la_da_ca_drain_per_target",4D,0D,40D);
        DRAIN_RADIUS=builder.defineInRange("centro_toma_la_da_ca_radius",6D,1D,16D);
        builder.pop();
        builder.comment("Esquerda Independente player powers, bought in the Esquerda skill tree. The first one is Manifestando",
                "(conviction_powers.esquerda_manifestando_*), which Lulistas can also buy.").push("esquerda_player_powers");
        PE_TURNCOAT_COOLDOWN=builder.comment("Virar o voto (Tabata): the nearest enemy mob fights your other enemies for a while.")
                .defineInRange("virar_o_voto_cooldown_ticks",400,20,72000);
        PE_TURNCOAT_TICKS=builder.defineInRange("virar_o_voto_ticks",120,20,1200);
        PE_TURNCOAT_RANGE=builder.defineInRange("virar_o_voto_range",10D,2D,32D);
        PE_CONSCIENCE_COOLDOWN=builder.comment("Voto de consciencia (Tabata): extra absorption health at once.")
                .defineInRange("voto_de_consciencia_cooldown_ticks",600,20,72000);
        PE_CONSCIENCE_ABSORPTION=builder.defineInRange("voto_de_consciencia_absorption",8D,0D,100D);
        PE_ROOTS_COOLDOWN=builder.comment("Raizes da Amazonia (Marina): vines hold the enemies around you (Paralisado).")
                .defineInRange("raizes_da_amazonia_cooldown_ticks",300,20,72000);
        PE_ROOTS_TICKS=builder.defineInRange("raizes_da_amazonia_ticks",60,10,600);
        PE_ROOTS_RADIUS=builder.defineInRange("raizes_da_amazonia_radius",5D,1D,16D);
        PE_ROOTS_POISON_TICKS=builder.comment("A short Poison I on everyone the roots hold.").defineInRange("raizes_da_amazonia_poison_ticks",60,0,600);
        PE_ROOTS_LEAVES=builder.comment("A jungle leaves block grows at the feet of each enemy held, on empty space only, and goes away",
                "when the roots let go. Needs a player who may build there; protection mods can cancel it.").define("raizes_da_amazonia_leaves",true);
        PE_VANISH_COOLDOWN=builder.comment("Sumico de 4 anos (Marina): invisible, Regeneration II and the mobs chasing you lose track.")
                .defineInRange("sumico_cooldown_ticks",1200,20,72000);
        PE_VANISH_TICKS=builder.defineInRange("sumico_ticks",160,20,1200);
        PE_TEMPER_COOLDOWN=builder.comment("Pavio curto (Ciro): Strength II, Speed and a wave that pushes the enemies around away.")
                .defineInRange("pavio_curto_cooldown_ticks",240,20,72000);
        PE_TEMPER_TICKS=builder.defineInRange("pavio_curto_ticks",100,20,1200);
        PE_TEMPER_RADIUS=builder.defineInRange("pavio_curto_radius",4D,1D,16D);
        PE_PARIS_COOLDOWN=builder.comment("Vou pra Paris (Ciro): you leave the fight, invisible and untouchable, without attacking,",
                "and come back with at least this fraction of your health.").defineInRange("vou_pra_paris_cooldown_ticks",1800,20,72000);
        PE_PARIS_TICKS=builder.defineInRange("vou_pra_paris_ticks",200,20,1200);
        PE_PARIS_HEALTH=builder.defineInRange("vou_pra_paris_return_health_fraction",.5D,0D,1D);
        PE_LIVE_COOLDOWN=builder.comment("Live de 3 horas (Jones): every second, enemies around get Slowness II and Weakness.")
                .defineInRange("live_de_3_horas_cooldown_ticks",400,20,72000);
        PE_LIVE_TICKS=builder.defineInRange("live_de_3_horas_ticks",60,20,400);
        PE_LIVE_RADIUS=builder.defineInRange("live_de_3_horas_radius",8D,1D,32D);
        PE_FREEZE_COOLDOWN=builder.comment("Inverno Siberiano (Jones): the enemy you look at freezes in place (Paralisado and frost).")
                .defineInRange("inverno_siberiano_cooldown_ticks",300,20,72000);
        PE_FREEZE_TICKS=builder.defineInRange("inverno_siberiano_ticks",80,10,600);
        PE_FREEZE_RANGE=builder.defineInRange("inverno_siberiano_range",12D,2D,32D);
        PE_COMMITTEE_COOLDOWN=builder.comment("Comite Central (Jones): comrades (Manifestantes with Strength and Resistance) on top of Manifestando.")
                .defineInRange("comite_central_cooldown_ticks",900,20,72000);
        PE_COMMITTEE_COUNT=builder.defineInRange("comite_central_count",3,1,20);
        PE_COMMITTEE_LIFETIME=builder.defineInRange("comite_central_lifetime_ticks",600,20,6000);
        builder.pop();
        builder.comment("Esquerda Independente characters: Jones Manoel (leader, in the castle), Tabata Amaral, Marina Silva and Ciro Gomes.")
                .push("esquerda_independente");
        JONES_HEALTH=builder.defineInRange("jones_max_health",300D,1D,1024D);
        JONES_EXPLOSION_ENABLED=builder.define("jones_death_explosion_enabled",true);
        JONES_EXPLOSION_POWER=builder.defineInRange("jones_death_explosion_power",7D,0D,16D);
        JONES_DEATH_TICKS=builder.defineInRange("jones_death_animation_ticks",60,20,200);
        JONES_RISE=builder.defineInRange("jones_death_rise_blocks",2D,0D,8D);
        JONES_BLOCK_DAMAGE=builder.define("jones_death_explosion_block_damage",true);
        JONES_LIVE_COOLDOWN=builder.comment("Live de 3 horas: stands still lecturing; enemies around get Slowness II and Weakness.")
                .defineInRange("jones_live_cooldown_ticks",400,20,72000);
        JONES_LIVE_TICKS=builder.defineInRange("jones_live_ticks",60,20,400);
        JONES_LIVE_RADIUS=builder.defineInRange("jones_live_radius",8D,1D,32D);
        JONES_FREEZE_COOLDOWN=builder.comment("Inverno Siberiano: the target freezes in place (Paralisado and frost) for a while.")
                .defineInRange("jones_freeze_cooldown_ticks",300,20,72000);
        JONES_FREEZE_TICKS=builder.defineInRange("jones_freeze_ticks",80,10,600);
        JONES_FREEZE_RANGE=builder.defineInRange("jones_freeze_range",12D,2D,32D);
        JONES_COMMITTEE_COOLDOWN=builder.comment("Comitê Central: below half health, comrades (Manifestantes) come to his side.")
                .defineInRange("jones_committee_cooldown_ticks",900,20,72000);
        JONES_COMMITTEE_COUNT=builder.defineInRange("jones_committee_count",3,1,20);
        TABATA_HEALTH=builder.defineInRange("tabata_max_health",80D,1D,1024D);
        TABATA_TURNCOAT_COOLDOWN=builder.comment("Virar o voto: an enemy mob changes sides and fights its own allies for a while.")
                .defineInRange("tabata_turncoat_cooldown_ticks",400,20,72000);
        TABATA_TURNCOAT_TICKS=builder.defineInRange("tabata_turncoat_ticks",120,20,1200);
        TABATA_SHIELD_COOLDOWN=builder.comment("Voto de consciência: a heavy hit gives her extra absorption health.")
                .defineInRange("tabata_shield_cooldown_ticks",600,20,72000);
        TABATA_SHIELD_ABSORPTION=builder.defineInRange("tabata_shield_absorption",8D,0D,100D);
        TABATA_SHIELD_TRIGGER=builder.comment("Damage of a single hit that counts as heavy.").defineInRange("tabata_shield_trigger_damage",4D,0D,100D);
        MARINA_HEALTH=builder.defineInRange("marina_max_health",80D,1D,1024D);
        MARINA_VANISH_COOLDOWN=builder.comment("Sumiço de 4 anos: low on health she turns invisible, drops the fight and heals.")
                .defineInRange("marina_vanish_cooldown_ticks",1200,20,72000);
        MARINA_VANISH_TICKS=builder.defineInRange("marina_vanish_ticks",160,20,1200);
        MARINA_VANISH_HEALTH=builder.comment("Fraction of her health below which she vanishes.").defineInRange("marina_vanish_health_fraction",.4D,0D,1D);
        MARINA_ROOTS_COOLDOWN=builder.comment("Raízes da Amazônia: vines hold the enemies around her (Paralisado).")
                .defineInRange("marina_roots_cooldown_ticks",300,20,72000);
        MARINA_ROOTS_TICKS=builder.defineInRange("marina_roots_ticks",60,10,600);
        MARINA_ROOTS_RADIUS=builder.defineInRange("marina_roots_radius",5D,1D,16D);
        MARINA_ROOTS_POISON_TICKS=builder.comment("A short Poison I on everyone her roots hold, like the player power.")
                .defineInRange("marina_roots_poison_ticks",60,0,600);
        MARINA_ROOTS_LEAVES=builder.comment("A jungle leaves block grows at the feet of each enemy held, on empty space only, until the roots",
                "let go. Follows the mobGriefing game rule; protection mods can cancel it.").define("marina_roots_leaves",true);
        CIRO_HEALTH=builder.defineInRange("ciro_max_health",100D,1D,1024D);
        CIRO_MELEE=builder.defineInRange("ciro_melee_damage",5D,0D,100D);
        CIRO_TEMPER_COOLDOWN=builder.comment("Pavio curto: when hit he blows up: Strength II, Speed and a push wave.")
                .defineInRange("ciro_temper_cooldown_ticks",240,20,72000);
        CIRO_TEMPER_TICKS=builder.defineInRange("ciro_temper_ticks",100,20,1200);
        CIRO_TEMPER_RADIUS=builder.defineInRange("ciro_temper_radius",4D,1D,16D);
        CIRO_PARIS_COOLDOWN=builder.comment("Vou pra Paris: low on health he leaves the fight, vanishes and comes back later.")
                .defineInRange("ciro_paris_cooldown_ticks",2400,20,72000);
        CIRO_PARIS_TICKS=builder.defineInRange("ciro_paris_ticks",600,20,6000);
        CIRO_PARIS_HEALTH=builder.comment("Fraction of his health below which he leaves.").defineInRange("ciro_paris_health_fraction",.3D,0D,1D);
        builder.pop();
        builder.comment("Direita: Eneias (leader, in the castle) and Augusto Cury with his drones.").push("direita_personagens");
        ENEIAS_HEALTH=builder.defineInRange("eneias_max_health",300D,1D,1024D);
        ENEIAS_EXPLOSION_ENABLED=builder.define("eneias_death_explosion_enabled",true);
        ENEIAS_EXPLOSION_POWER=builder.defineInRange("eneias_death_explosion_power",7D,0D,16D);
        ENEIAS_DEATH_TICKS=builder.defineInRange("eneias_death_animation_ticks",60,20,200);
        ENEIAS_RISE=builder.defineInRange("eneias_death_rise_blocks",2D,0D,8D);
        ENEIAS_BLOCK_DAMAGE=builder.define("eneias_death_explosion_block_damage",true);
        ENEIAS_BOMB_DAMAGE=builder.comment("Bombinha: thrown in an arc, goes off 1.5 s later; never breaks blocks.")
                .defineInRange("eneias_bombinha_damage",6D,0D,100D);
        ENEIAS_BOMB_RADIUS=builder.defineInRange("eneias_bombinha_radius",2.5D,.5D,8D);
        ENEIAS_BOMB_COOLDOWN=builder.defineInRange("eneias_bombinha_cooldown_ticks",80,20,72000);
        ENEIAS_DIPLOMA_DAMAGE=builder.comment("Diploma: a burst of diplomas; each hit also gives Slowness.")
                .defineInRange("eneias_diploma_damage",4D,0D,100D);
        ENEIAS_DIPLOMA_COUNT=builder.defineInRange("eneias_diploma_count",3,1,10);
        ENEIAS_DIPLOMA_COOLDOWN=builder.defineInRange("eneias_diploma_cooldown_ticks",100,20,72000);
        ENEIAS_SHOUT_COOLDOWN=builder.comment("\"Meu nome e Eneas!\": below half health, a shout that pushes everyone around and gives Nausea.")
                .defineInRange("eneias_shout_cooldown_ticks",600,20,72000);
        ENEIAS_SHOUT_RADIUS=builder.defineInRange("eneias_shout_radius",6D,1D,16D);
        ENEIAS_NUKE_ENABLED=builder.comment("\"O Brasil precisa de uma bomba atomica\": at low health Eneias turns green; unless he dies before the",
                "countdown ends, he blows up with an atomic explosion.").define("eneias_nuke_enabled",true);
        ENEIAS_NUKE_HEALTH=builder.comment("Fraction of his health that starts the countdown.").defineInRange("eneias_nuke_health_fraction",.25D,.01D,1D);
        ENEIAS_NUKE_COUNTDOWN=builder.defineInRange("eneias_nuke_countdown_ticks",300,20,6000);
        ENEIAS_NUKE_RADIUS=builder.comment("Everything alive this close is hit; the damage falls off from the lethal radius to here.")
                .defineInRange("eneias_nuke_radius",300D,10D,512D);
        ENEIAS_NUKE_LETHAL_RADIUS=builder.defineInRange("eneias_nuke_full_damage_radius",60D,1D,512D);
        ENEIAS_NUKE_DAMAGE=builder.defineInRange("eneias_nuke_damage",400D,0D,10000D);
        ENEIAS_NUKE_BREAK_BLOCKS=builder.comment("The crater; also needs the mobGriefing game rule. Bedrock and unbreakable blocks stay.")
                .define("eneias_nuke_break_blocks",true);
        ENEIAS_NUKE_CRATER_RADIUS=builder.defineInRange("eneias_nuke_crater_radius",80,0,128);
        ENEIAS_NUKE_BLOCKS_PER_TICK=builder.comment("The crater is dug over several ticks; more per tick is faster but heavier.")
                .defineInRange("eneias_nuke_blocks_per_tick",40000,1000,500000);
        CURY_HEALTH=builder.defineInRange("augusto_cury_max_health",100D,1D,1024D);
        CURY_SHOOTER_DRONES=builder.comment("Drones Augusto Cury keeps in a fight.").defineInRange("augusto_cury_shooter_drones",2,0,6);
        CURY_BOMB_DRONES=builder.defineInRange("augusto_cury_bomb_drones",1,0,4);
        CURY_REGEN_DRONES=builder.defineInRange("augusto_cury_regeneration_drones",1,0,4);
        CURY_SUMMON_INTERVAL=builder.comment("Ticks between two drone launches.").defineInRange("augusto_cury_summon_interval_ticks",40,10,1200);
        CURY_BOMB_COOLDOWN=builder.comment("After a bomb drone explodes, ticks before the next one.").defineInRange("augusto_cury_bomb_cooldown_ticks",300,20,72000);
        DRONE_HEALTH=builder.defineInRange("drone_max_health",10D,1D,200D);
        DRONE_SHOT_DAMAGE=builder.defineInRange("drone_shot_damage",2D,0D,100D);
        DRONE_SHOT_INTERVAL=builder.defineInRange("drone_shot_interval_ticks",25,5,400);
        DRONE_BOMB_DAMAGE=builder.defineInRange("drone_bomb_damage",6D,0D,100D);
        DRONE_BOMB_RADIUS=builder.defineInRange("drone_bomb_radius",2.5D,.5D,8D);
        DRONE_HEAL=builder.comment("Regeneration drone: health per second to Augusto Cury and his allies around.").defineInRange("drone_heal_per_second",2D,0D,40D);
        DRONE_HEAL_RADIUS=builder.defineInRange("drone_heal_radius",6D,1D,16D);
        builder.pop();
        builder.comment("Direita: Romeu Zema (Liquidacao Lojas Zema, Privatizacao) and Ronaldo Caiado (Laco, Boiada do agro).",
                "The powers' values are shared by the characters and the players of the Direita tree.").push("zema_caiado");
        ZEMA_HEALTH=builder.defineInRange("romeu_zema_max_health",100D,1D,1024D);
        CAIADO_HEALTH=builder.defineInRange("ronaldo_caiado_max_health",100D,1D,1024D);
        ZEMA_SALE_COOLDOWN=builder.comment("Romeu Zema's Liquidacao, ticks.").defineInRange("romeu_zema_liquidacao_cooldown_ticks",160,20,72000);
        ZEMA_PRIVATIZE_COOLDOWN=builder.defineInRange("romeu_zema_privatizacao_cooldown_ticks",200,20,72000);
        CAIADO_LASSO_COOLDOWN=builder.defineInRange("ronaldo_caiado_laco_cooldown_ticks",120,20,72000);
        CAIADO_STAMPEDE_COOLDOWN=builder.defineInRange("ronaldo_caiado_boiada_cooldown_ticks",300,20,72000);
        SALE_COUNT=builder.comment("Appliances falling in one Liquidacao.").defineInRange("liquidacao_count",6,1,20);
        SALE_DAMAGE=builder.defineInRange("liquidacao_damage",5D,0D,100D);
        SALE_RADIUS=builder.comment("Each appliance hits this far from where it lands; they fall within twice this of the aim.")
                .defineInRange("liquidacao_radius",1.8D,.5D,6D);
        PRIVATIZE_DAMAGE=builder.defineInRange("privatizacao_damage",2D,0D,100D);
        PRIVATIZE_RANGE=builder.defineInRange("privatizacao_range",6D,2D,16D);
        PRIVATIZE_PARALYSIS_TICKS=builder.defineInRange("privatizacao_paralysis_ticks",60,0,400);
        LASSO_DAMAGE=builder.defineInRange("laco_damage",2D,0D,100D);
        LASSO_RANGE=builder.defineInRange("laco_range",14D,4D,32D);
        STAMPEDE_COUNT=builder.comment("Oxen in a Boiada do agro.").defineInRange("boiada_count",5,1,12);
        STAMPEDE_DAMAGE=builder.comment("Each ox hits each victim once.").defineInRange("boiada_damage",6D,0D,100D);
        STAMPEDE_SPEED=builder.comment("Blocks per tick.").defineInRange("boiada_speed",.6D,.1D,2D);
        STAMPEDE_TICKS=builder.defineInRange("boiada_ticks",40,10,200);
        builder.pop();
        builder.comment("Armed guards, two on each tower of every faction castle (new castles only).").push("tower_guards");
        GUARDS_ENABLED=builder.define("enabled",true);
        GUARD_HEALTH=builder.defineInRange("max_health",40D,1D,1024D);
        GUARD_RANGE=builder.comment("They shoot targets up to this far (characters drop targets beyond 20 blocks).").defineInRange("range",20D,4D,20D);
        GUARD_SHOT_INTERVAL=builder.comment("Ticks between shots; without TaCZ at least 15.").defineInRange("shot_interval_ticks",20,5,400);
        GUARD_PISTOL_DAMAGE=builder.comment("Damage of a GE-P12 shot when TaCZ is not installed.").defineInRange("pistol_damage",5D,0D,100D);
        builder.pop();
        builder.comment("Lulista player powers of the 0.020 (Lulista tree): Dilma's mandiocas and Estocar vento, Haddad's slaps, Boulos's Ocupacao.")
                .push("lulista_player_powers");
        PL_MANDIOCA_COOLDOWN=builder.defineInRange("mandiocas_cooldown_ticks",120,20,72000);
        PL_MANDIOCA_COUNT=builder.defineInRange("mandiocas_count",3,1,9);
        PL_MANDIOCA_DAMAGE=builder.defineInRange("mandiocas_damage",3D,0D,100D);
        PL_WIND_COOLDOWN=builder.defineInRange("estocar_vento_cooldown_ticks",300,20,72000);
        PL_WIND_RANGE=builder.defineInRange("estocar_vento_range",12D,2D,32D);
        PL_WIND_PUSH=builder.defineInRange("estocar_vento_push",1.6D,0D,5D);
        PL_WIND_BREATHLESS_TICKS=builder.defineInRange("estocar_vento_sem_ar_ticks",100,20,1200);
        PL_SLAP_COOLDOWN=builder.defineInRange("tapas_na_cara_cooldown_ticks",200,20,72000);
        PL_SLAP_DAMAGE=builder.defineInRange("tapas_na_cara_damage",3D,0D,100D);
        PL_SLAP_RANGE=builder.defineInRange("tapas_na_cara_range",4D,2D,8D);
        PL_SLAP_PARALYSIS_TICKS=builder.defineInRange("tapas_na_cara_paralysis_ticks",40,0,200);
        PL_OCCUPATION_COOLDOWN=builder.defineInRange("ocupacao_cooldown_ticks",600,20,72000);
        PL_OCCUPATION_TICKS=builder.defineInRange("ocupacao_ticks",160,20,1200);
        PL_OCCUPATION_ABSORPTION=builder.defineInRange("ocupacao_absorption",8D,0D,100D);
        PL_OCCUPATION_TAUNT_RADIUS=builder.defineInRange("ocupacao_taunt_radius",8D,1D,32D);
        builder.pop();
        builder.comment("Direita player powers of the 0.020 (Direita tree): Eneias's and Augusto Cury's.").push("direita_player_powers");
        PD_BOMB_COOLDOWN=builder.defineInRange("bombinha_cooldown_ticks",160,20,72000);
        PD_BOMB_DAMAGE=builder.defineInRange("bombinha_damage",6D,0D,100D);
        PD_DIPLOMA_COOLDOWN=builder.defineInRange("diploma_cooldown_ticks",200,20,72000);
        PD_DIPLOMA_COUNT=builder.defineInRange("diploma_count",3,1,10);
        PD_DIPLOMA_DAMAGE=builder.defineInRange("diploma_damage",4D,0D,100D);
        PD_SHOUT_COOLDOWN=builder.defineInRange("meu_nome_e_eneas_cooldown_ticks",600,20,72000);
        PD_SHOUT_RADIUS=builder.defineInRange("meu_nome_e_eneas_radius",6D,1D,16D);
        PD_SHOOTER_COOLDOWN=builder.defineInRange("drone_atirador_cooldown_ticks",600,20,72000);
        PD_SHOOTER_COUNT=builder.defineInRange("drone_atirador_count",2,1,6);
        PD_BOMB_DRONE_COOLDOWN=builder.defineInRange("drone_bomba_cooldown_ticks",400,20,72000);
        PD_REGEN_COOLDOWN=builder.defineInRange("drone_regeneracao_cooldown_ticks",900,20,72000);
        PD_DRONE_LIFETIME=builder.comment("How long a player's drones fly.").defineInRange("drone_lifetime_ticks",600,20,12000);
        PD_SALE_COOLDOWN=builder.comment("Romeu Zema's and Ronaldo Caiado's powers (values in [zema_caiado]).")
                .defineInRange("liquidacao_cooldown_ticks",240,20,72000);
        PD_PRIVATIZE_COOLDOWN=builder.defineInRange("privatizacao_cooldown_ticks",300,20,72000);
        PD_LASSO_COOLDOWN=builder.defineInRange("laco_cooldown_ticks",160,20,72000);
        PD_STAMPEDE_COOLDOWN=builder.defineInRange("boiada_cooldown_ticks",400,20,72000);
        builder.pop();
        builder.comment("Playable classes, chosen after the faction. They only add to the vanilla player, never take away.",
                "Each class has a skill on Shift + right click with an empty main hand.").push("classes");
        FREE_CLASS_CHANGES=builder.comment("Changes a player may make after the first choice; after that only an operator (/ge classe).")
                .defineInRange("free_class_changes",1,0,10);
        COMBATENTE_HEALTH=builder.defineInRange("combatente_health_bonus",4D,0D,40D);
        COMBATENTE_ATTACK=builder.defineInRange("combatente_attack_damage_bonus",1.5D,0D,20D);
        TANQUE_ARMOR=builder.defineInRange("tanque_armor_bonus",6D,0D,30D);
        TANQUE_TOUGHNESS=builder.defineInRange("tanque_armor_toughness_bonus",2D,0D,20D);
        TANQUE_KNOCKBACK=builder.defineInRange("tanque_knockback_resistance",.3D,0D,1D);
        INVOCADOR_SPEED=builder.comment("Fraction of base movement speed; 0.1 = +10%.").defineInRange("invocador_speed_bonus",.1D,0D,1D);
        INVOCADOR_SUMMON_BONUS=builder.comment("Extra health and lifetime of the player's own troops; 0.25 = +25%.")
                .defineInRange("invocador_summon_bonus",.25D,0D,4D);
        ESPECIALISTA_COOLDOWN=builder.comment("Faction power cooldown reduction; 0.15 = -15%.").defineInRange("especialista_cooldown_reduction",.15D,0D,.9D);
        ESPECIALISTA_POWER=builder.comment("Extra power damage (or duration), on top of Ideological Strength.").defineInRange("especialista_power_bonus",.1D,0D,4D);
        SUPORTE_HEALTH=builder.defineInRange("suporte_health_bonus",2D,0D,40D);
        SUPORTE_REGEN_DELAY=builder.comment("Ticks without taking damage before the Suporte regeneration starts.")
                .defineInRange("suporte_regen_delay_ticks",200,20,6000);
        SUPORTE_REGEN_INTERVAL=builder.defineInRange("suporte_regen_interval_ticks",100,20,6000);
        CHARGE_COOLDOWN=builder.defineInRange("combatente_investida_cooldown_ticks",240,20,72000);
        CHARGE_DAMAGE=builder.defineInRange("combatente_investida_damage",6D,0D,100D);
        CHARGE_STRENGTH=builder.comment("Horizontal launch speed of the charge (about 6 blocks with the default).")
                .defineInRange("combatente_investida_strength",1.6D,.2D,5D);
        TAUNT_COOLDOWN=builder.defineInRange("tanque_provocacao_cooldown_ticks",400,20,72000);
        TAUNT_RADIUS=builder.defineInRange("tanque_provocacao_radius",8D,1D,32D);
        TAUNT_RESISTANCE_TICKS=builder.defineInRange("tanque_provocacao_resistance_ticks",100,0,1200);
        GUARD_COOLDOWN=builder.defineInRange("invocador_guarda_costas_cooldown_ticks",600,20,72000);
        GUARD_LIFETIME=builder.defineInRange("invocador_guarda_costas_lifetime_ticks",600,20,6000);
        QUICK_RELOAD_COOLDOWN=builder.defineInRange("especialista_recarga_rapida_cooldown_ticks",1800,20,72000);
        CLASS_HEAL_COOLDOWN=builder.defineInRange("suporte_cura_cooldown_ticks",400,20,72000);
        CLASS_HEAL_AMOUNT=builder.defineInRange("suporte_cura_amount",6D,0D,100D);
        CLASS_HEAL_RADIUS=builder.defineInRange("suporte_cura_radius",6D,1D,32D);
        builder.pop();
        builder.comment("Skill tree (second tab of J). Ideological Conviction gives the points; the 2nd and 3rd powers are bought here.").push("skill_tree");
        TREE_POINTS_PER_CONVICTION=builder.defineInRange("points_per_conviction",1,1,10);
        NODE_POWER_BONUS=builder.comment("Potência nodes: extra damage (or duration) of that power.").defineInRange("power_node_bonus",.2D,0D,4D);
        NODE_COOLDOWN_REDUCTION=builder.comment("Recarga nodes: cooldown reduction of that power.").defineInRange("cooldown_node_reduction",.2D,0D,.9D);
        NODE_CLASS_COOLDOWN_REDUCTION=builder.comment("Classe aprimorada: cooldown reduction of the class skill.").defineInRange("class_node_cooldown_reduction",.35D,0D,.9D);
        builder.pop();
        builder.comment("Moral bar (J): Esquerda, Lulismo, Centro, Missão, Direita, Bolsonarismo. Everyone starts in the Centro.").push("moral");
        MORAL_POWER_USE=builder.comment("Each faction power used pulls the bar this much toward the player's own faction.")
                .defineInRange("power_use_pull",1,0,100);
        MORAL_KILL=builder.comment("Each kill of a player or character of a faction pushes the bar this much away from that faction.")
                .defineInRange("kill_push",4,0,100);
        builder.pop();
        SPEC = builder.build();
    }

    private ModConfig() {}
}
