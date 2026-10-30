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
    public static final ForgeConfigSpec.DoubleValue JAIR_HEALTH,JAIR_SPEED,JAIR_MELEE,JAIR_EXPLOSION_POWER,JAIR_RISE,JAIR_CANALHA_CHANCE;
    public static final ForgeConfigSpec.IntValue JAIR_DEATH_TICKS,JAIR_SUMMON_COUNT,JAIR_SUMMON_COOLDOWN,JAIR_MAX_SUMMONS;
    public static final ForgeConfigSpec.BooleanValue JAIR_EXPLOSION_ENABLED,JAIR_BLOCK_DAMAGE,JAIR_SUMMON_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FLAVIO_HEALTH,FLAVIO_SPEED,FLAVIO_MELEE,FISSURE_RANGE,FISSURE_WIDTH,FISSURE_DAMAGE,FISSURE_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue FISSURE_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue FISSURE_BLOCK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue FISSURE_FINAL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue HADDAD_HEALTH,HADDAD_MELEE,SLAP_DAMAGE,BOULOS_HEALTH,BOULOS_MELEE,BOULOS_ARMOR;
    public static final ForgeConfigSpec.IntValue SLAP_COUNT,SLAP_PARALYSIS_TICKS,SLAP_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue RIVALRY_ENABLED;
    public static final ForgeConfigSpec.DoubleValue RIVALRY_RANGE;
    public static final ForgeConfigSpec.IntValue BALANCE_REVISION;
    public static final ForgeConfigSpec.BooleanValue FACTION_KIT_ENABLED;
    public static final ForgeConfigSpec.IntValue BEAN_VOLLEY_COOLDOWN,BEAN_VOLLEY_COUNT,PLAYER_FISSURE_COOLDOWN,PLAYER_TROOPS_COOLDOWN,
            PLAYER_TROOPS_COUNT,PLAYER_TROOPS_LIFETIME,GROUP_HEAL_COOLDOWN,GROUP_HEAL_TICKS,ESQUERDA_REGEN_DELAY,
            ESQUERDA_REGEN_INTERVAL,DASH_COOLDOWN,DASH_BUFF_TICKS,STANCE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue BEAN_VOLLEY_DAMAGE,LULISTA_SATURATION_BONUS,PLAYER_FISSURE_DAMAGE,
            PLAYER_FISSURE_RANGE,BOLSONARISTA_ARMOR,MISSAO_HEALTH,GROUP_HEAL_RADIUS,DASH_STRENGTH,DIREITA_SPEED;
    public static final ForgeConfigSpec.IntValue MAX_LEVEL,POINTS_PER_LEVEL,STAT_MAX,XP_BASE,XP_PER_LEVEL,XP_HOSTILE,XP_NPC,XP_BOSS,XP_PLAYER,
            CONVICTION_SECOND_POWER,CONVICTION_THIRD_POWER;
    public static final ForgeConfigSpec.DoubleValue XP_RIVAL_MULTIPLIER,XP_RIVAL_LEADER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STRENGTH_PER_POINT,SPEED_PER_POINT,HEALTH_PER_POINT,IDEOLOGY_PER_POINT,CONVICTION_PER_POINT;
    public static final ForgeConfigSpec.IntValue STOMP_COOLDOWN,FAZ_O_L_COOLDOWN,FAZ_O_L_ITEMS,ATHLETE_COOLDOWN,ATHLETE_TICKS,GRAVEDIGGER_COOLDOWN,
            METEOR_COOLDOWN,METEOR_COUNT,BOOK_COOLDOWN,BOOK_TICKS,BOOK_INTERVAL,BORED_TICKS,PROTEST_COOLDOWN,PROTEST_COUNT,PROTEST_LIFETIME,
            STRIKE_COOLDOWN,STRIKE_TICKS,LEAP_COOLDOWN,PUSH_COOLDOWN,EXEMPTION_COOLDOWN,EXEMPTION_TICKS,DRAIN_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue STOMP_DAMAGE,STOMP_RADIUS,FAZ_O_L_DAMAGE,FAZ_O_L_RADIUS,GRAVEDIGGER_DAMAGE,METEOR_DAMAGE,
            METEOR_RADIUS,BOOK_DAMAGE,STRIKE_RADIUS,LEAP_DAMAGE,PUSH_DAMAGE,PUSH_RADIUS,DRAIN_AMOUNT,DRAIN_RADIUS;
    public static final ForgeConfigSpec.BooleanValue FAZ_O_L_BREAK_BLOCKS,GRAVEDIGGER_BREAK_BLOCKS;

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
        builder.comment("Lulista allies of Lula. They defend the village: hostile mobs and players of other factions nearby.").push("haddad");
        HADDAD_HEALTH=builder.defineInRange("max_health",30D,1D,1024D);
        HADDAD_MELEE=builder.defineInRange("melee_damage",3D,0D,100D);
        SLAP_COUNT=builder.defineInRange("slap_count",3,1,10);
        SLAP_DAMAGE=builder.comment("Damage of each slap; armor applies.").defineInRange("slap_damage",3D,0D,100D);
        SLAP_PARALYSIS_TICKS=builder.comment("Paralysis after each slap that really hurts: no walking, no jumping.")
                .defineInRange("slap_paralysis_ticks",60,0,1200);
        SLAP_COOLDOWN=builder.comment("From the start of one slap combo to the next.").defineInRange("slap_cooldown_ticks",200,40,72000);
        builder.pop();
        builder.push("boulos");
        BOULOS_HEALTH=builder.defineInRange("max_health",60D,1D,1024D);
        BOULOS_MELEE=builder.defineInRange("melee_damage",5D,0D,100D);
        BOULOS_ARMOR=builder.defineInRange("armor",6D,0D,30D);
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
        GROUP_HEAL_COOLDOWN=builder.defineInRange("esquerda_group_heal_cooldown_ticks",600,40,72000);
        GROUP_HEAL_RADIUS=builder.defineInRange("esquerda_group_heal_radius",6D,1D,32D);
        GROUP_HEAL_TICKS=builder.defineInRange("esquerda_group_heal_regeneration_ticks",100,20,1200);
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
        MAX_LEVEL=builder.defineInRange("max_level",30,2,200);
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
        CONVICTION_SECOND_POWER=builder.defineInRange("conviction_for_second_power",5,0,50);
        CONVICTION_THIRD_POWER=builder.defineInRange("conviction_for_third_power",10,0,50);
        builder.pop();
        builder.comment("Second and third faction powers, unlocked by Ideological Conviction.").push("conviction_powers");
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
        STRIKE_COOLDOWN=builder.defineInRange("esquerda_greve_geral_cooldown_ticks",600,20,72000);
        STRIKE_RADIUS=builder.defineInRange("esquerda_greve_geral_radius",8D,1D,32D);
        STRIKE_TICKS=builder.defineInRange("esquerda_greve_geral_ticks",120,20,1200);
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
        SPEC = builder.build();
    }

    private ModConfig() {}
}
