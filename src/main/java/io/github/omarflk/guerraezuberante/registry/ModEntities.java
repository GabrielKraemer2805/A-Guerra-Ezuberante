package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import io.github.omarflk.guerraezuberante.entity.UkrainianFighterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GuerraEzuberanteMod.MOD_ID);
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.JairBolsonaroEntity>> JAIR_BOLSONARO=REGISTER.register("jair_bolsonaro",
            ()->EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.JairBolsonaroEntity::new,MobCategory.CREATURE).sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":jair_bolsonaro"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.FlavioBolsonaroEntity>> FLAVIO_BOLSONARO=REGISTER.register("flavio_bolsonaro",
            ()->EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.FlavioBolsonaroEntity::new,MobCategory.CREATURE).sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":flavio_bolsonaro"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.RachadinhaEntity>> RACHADINHA=REGISTER.register("rachadinha",
            ()->EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.RachadinhaEntity>of(io.github.omarflk.guerraezuberante.entity.RachadinhaEntity::new,MobCategory.MISC)
                    .sized(1,.1F).clientTrackingRange(8).updateInterval(1).noSave().noSummon().build(GuerraEzuberanteMod.MOD_ID+":rachadinha"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.LulaEntity>> LULA = REGISTER.register(
            "lula", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.LulaEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":lula"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.HaddadEntity>> HADDAD = REGISTER.register(
            "haddad", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.HaddadEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":haddad"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.BoulosEntity>> BOULOS = REGISTER.register(
            "guilherme_boulos", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.BoulosEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":guilherme_boulos"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.DilmaEntity>> DILMA = REGISTER.register(
            "dilma", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.DilmaEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":dilma"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.MandiocaProjectile>> MANDIOCA = REGISTER.register(
            "mandioca", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.MandiocaProjectile>of(io.github.omarflk.guerraezuberante.entity.MandiocaProjectile::new, MobCategory.MISC)
                    .sized(.3F,.3F).clientTrackingRange(8).updateInterval(1).build(GuerraEzuberanteMod.MOD_ID+":mandioca"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.JonesManoelEntity>> JONES_MANOEL = REGISTER.register(
            "jones_manoel", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.JonesManoelEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":jones_manoel"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.TabataAmaralEntity>> TABATA_AMARAL = REGISTER.register(
            "tabata_amaral", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.TabataAmaralEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":tabata_amaral"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.MarinaSilvaEntity>> MARINA_SILVA = REGISTER.register(
            "marina_silva", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.MarinaSilvaEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":marina_silva"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.CiroGomesEntity>> CIRO_GOMES = REGISTER.register(
            "ciro_gomes", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.CiroGomesEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":ciro_gomes"));
    /** 0.020: Enéias, leader of the Direita (boss), and Augusto Cury with his drones. */
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.EneiasEntity>> ENEIAS = REGISTER.register(
            "eneias", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.EneiasEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":eneias"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.AugustoCuryEntity>> AUGUSTO_CURY = REGISTER.register(
            "augusto_cury", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.AugustoCuryEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":augusto_cury"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.DroneEntity>> DRONE = REGISTER.register(
            "drone", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.DroneEntity::new, MobCategory.MISC)
                    .sized(.7F,.4F).clientTrackingRange(8).updateInterval(1).build(GuerraEzuberanteMod.MOD_ID+":drone"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.BombinhaProjectile>> BOMBINHA = REGISTER.register(
            "bombinha", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.BombinhaProjectile>of(io.github.omarflk.guerraezuberante.entity.BombinhaProjectile::new, MobCategory.MISC)
                    .sized(.3F,.3F).clientTrackingRange(8).updateInterval(1).build(GuerraEzuberanteMod.MOD_ID+":bombinha"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.DiplomaProjectile>> DIPLOMA = REGISTER.register(
            "diploma", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.DiplomaProjectile>of(io.github.omarflk.guerraezuberante.entity.DiplomaProjectile::new, MobCategory.MISC)
                    .sized(.3F,.3F).clientTrackingRange(8).updateInterval(1).build(GuerraEzuberanteMod.MOD_ID+":diploma"));
    /** 0.022: Romeu Zema and Ronaldo Caiado of the Direita; the appliances and oxen of their powers are never saved. */
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.RomeuZemaEntity>> ROMEU_ZEMA = REGISTER.register(
            "romeu_zema", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.RomeuZemaEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":romeu_zema"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.RonaldoCaiadoEntity>> RONALDO_CAIADO = REGISTER.register(
            "ronaldo_caiado", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.RonaldoCaiadoEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":ronaldo_caiado"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.ApplianceEntity>> APPLIANCE = REGISTER.register(
            "eletrodomestico", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.ApplianceEntity>of(io.github.omarflk.guerraezuberante.entity.ApplianceEntity::new, MobCategory.MISC)
                    .sized(.9F,.9F).clientTrackingRange(8).updateInterval(1).noSave().build(GuerraEzuberanteMod.MOD_ID+":eletrodomestico"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.StampedeOxEntity>> STAMPEDE_OX = REGISTER.register(
            "boi_da_boiada", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.StampedeOxEntity>of(io.github.omarflk.guerraezuberante.entity.StampedeOxEntity::new, MobCategory.MISC)
                    .sized(.9F,1.4F).clientTrackingRange(8).updateInterval(1).noSave().build(GuerraEzuberanteMod.MOD_ID+":boi_da_boiada"));
    /** Spawn egg of the Direita villagers (0.022); same size and tracking as the vanilla villager. */
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.DireitistaEntity>> DIREITISTA = REGISTER.register(
            "direitista", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.DireitistaEntity::new, MobCategory.MISC)
                    .sized(.6F,1.95F).clientTrackingRange(10).build(GuerraEzuberanteMod.MOD_ID+":direitista"));
    /** 0.020: two armed guards on each tower of every faction castle. */
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.TowerGuardEntity>> TOWER_GUARD = REGISTER.register(
            "guarda_da_torre", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.TowerGuardEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(10).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":guarda_da_torre"));
    /** Spawn egg of the Bolsonarista villagers; same size and tracking as the vanilla villager. */
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.BolsonaristaEntity>> BOLSONARISTA = REGISTER.register(
            "bolsonarista", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.BolsonaristaEntity::new, MobCategory.MISC)
                    .sized(.6F,1.95F).clientTrackingRange(10).build(GuerraEzuberanteMod.MOD_ID+":bolsonarista"));
    /** Spawn egg of the Esquerda Independente villagers; same size and tracking as the vanilla villager. */
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.ComunistaEntity>> COMUNISTA = REGISTER.register(
            "comunista", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.ComunistaEntity::new, MobCategory.MISC)
                    .sized(.6F,1.95F).clientTrackingRange(10).build(GuerraEzuberanteMod.MOD_ID+":comunista"));
    /** Same size and tracking as the vanilla villager. */
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.PetistaEntity>> PETISTA = REGISTER.register(
            "petista", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.PetistaEntity::new, MobCategory.MISC)
                    .sized(.6F,1.95F).clientTrackingRange(10).build(GuerraEzuberanteMod.MOD_ID+":petista"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.BeanProjectile>> BEAN = REGISTER.register(
            "semente_feijao", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.BeanProjectile>of(
                    io.github.omarflk.guerraezuberante.entity.BeanProjectile::new, MobCategory.MISC)
                    .sized(.22F,.22F).clientTrackingRange(8).updateInterval(1).build(GuerraEzuberanteMod.MOD_ID+":semente_feijao"));

    public static final RegistryObject<EntityType<RenanSantosEntity>> RENAN_SANTOS = REGISTER.register(
            "renan_santos", () -> EntityType.Builder.of(RenanSantosEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(8)
                    .build(GuerraEzuberanteMod.MOD_ID + ":renan_santos"));

    public static final RegistryObject<EntityType<ArthurDoValEntity>> ARTHUR_DO_VAL = REGISTER.register(
            "arthur_do_val", () -> EntityType.Builder.<ArthurDoValEntity>of(ArthurDoValEntity::new, MobCategory.CREATURE)
                    .sized(0.6F * ArthurDoValEntity.SCALE, 1.8F * ArthurDoValEntity.SCALE)
                    .clientTrackingRange(8).build(GuerraEzuberanteMod.MOD_ID + ":arthur_do_val"));

    public static final RegistryObject<EntityType<UkrainianFighterEntity>> UKRAINIAN_FIGHTER = REGISTER.register(
            "combatente_ucraniana", () -> EntityType.Builder.of(UkrainianFighterEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(8)
                    .build(GuerraEzuberanteMod.MOD_ID + ":combatente_ucraniana"));

    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.FazOLProjectile>> FAZ_O_L = REGISTER.register(
            "faz_o_l", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.FazOLProjectile>of(
                    io.github.omarflk.guerraezuberante.entity.FazOLProjectile::new, MobCategory.MISC)
                    .sized(.9F, .9F).clientTrackingRange(8).updateInterval(1).noSave().build(GuerraEzuberanteMod.MOD_ID + ":faz_o_l"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.MeteorProjectile>> METEOR = REGISTER.register(
            "meteoro", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.MeteorProjectile>of(
                    io.github.omarflk.guerraezuberante.entity.MeteorProjectile::new, MobCategory.MISC)
                    .sized(.6F, .6F).clientTrackingRange(8).updateInterval(1).noSave().build(GuerraEzuberanteMod.MOD_ID + ":meteoro"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.YellowBookProjectile>> YELLOW_BOOK = REGISTER.register(
            "livro_amarelo", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.YellowBookProjectile>of(
                    io.github.omarflk.guerraezuberante.entity.YellowBookProjectile::new, MobCategory.MISC)
                    .sized(.35F, .35F).clientTrackingRange(8).updateInterval(1).noSave().build(GuerraEzuberanteMod.MOD_ID + ":livro_amarelo"));
    /** Never saved: they last 15 s and belong to a player who may not be online after a restart. */
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.ManifestanteEntity>> MANIFESTANTE = REGISTER.register(
            "manifestante", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.ManifestanteEntity>of(
                    io.github.omarflk.guerraezuberante.entity.ManifestanteEntity::new, MobCategory.MISC)
                    .sized(.6F, 1.95F).clientTrackingRange(8).noSave().build(GuerraEzuberanteMod.MOD_ID + ":manifestante"));

    public static final RegistryObject<EntityType<ArthurDoValEntity>> ARTHUR_COMBATENTE = arthur("arthur_do_val_combatente", false);
    public static final RegistryObject<EntityType<ArthurDoValEntity>> ARTHUR_INVOCADOR = arthur("arthur_do_val_invocador", true);

    private static RegistryObject<EntityType<ArthurDoValEntity>> arthur(String id, boolean summoner) {
        return REGISTER.register(id, () -> EntityType.Builder.<ArthurDoValEntity>of(
                (type, level) -> new ArthurDoValEntity(type, level, summoner), MobCategory.CREATURE)
                .sized(0.6F * ArthurDoValEntity.SCALE, 1.8F * ArthurDoValEntity.SCALE)
                .clientTrackingRange(8).build(GuerraEzuberanteMod.MOD_ID + ":" + id));
    }

    private ModEntities() {}
}
