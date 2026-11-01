package io.github.omarflk.guerraezuberante.config;

import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Existing worlds receive the requested balance, while non-default server choices are preserved. */
public final class ConfigMigrations {
    public static void loading(ModConfigEvent.Loading event){migrate(event);}
    public static void reloading(ModConfigEvent.Reloading event){migrate(event);}
    private static void migrate(ModConfigEvent event){
        if(event.getConfig().getSpec()!=ModConfig.SPEC || ModConfig.BALANCE_REVISION.get()>=4)return;
        upgradeLegacyDefaults();event.getConfig().save();
    }
    public static void upgradeLegacyDefaults(){
        if(ModConfig.BALANCE_REVISION.get()<1){
            if(ModConfig.LULA_HEALTH.get()==30D)ModConfig.LULA_HEALTH.set(300D);
            if(ModConfig.JAIR_HEALTH.get()==30D)ModConfig.JAIR_HEALTH.set(300D);
            if(ModConfig.FISSURE_DAMAGE.get()==6D)ModConfig.FISSURE_DAMAGE.set(10D);
            if(ModConfig.FISSURE_KNOCKBACK.get()==.35D)ModConfig.FISSURE_KNOCKBACK.set(.65D);
            if(ModConfig.FISSURE_COOLDOWN.get()==240)ModConfig.FISSURE_COOLDOWN.set(120);
            ModConfig.BALANCE_REVISION.set(1);
        }
        // 0.019: level cap 30 -> 100.
        if(ModConfig.BALANCE_REVISION.get()<2){
            if(ModConfig.MAX_LEVEL.get()==30)ModConfig.MAX_LEVEL.set(100);
            ModConfig.BALANCE_REVISION.set(2);
        }
        // 0.019: more health for the secondary characters (Dilma a mini boss); Renan and Arthur were fixed before.
        if(ModConfig.BALANCE_REVISION.get()<3){
            if(ModConfig.FLAVIO_HEALTH.get()==30D)ModConfig.FLAVIO_HEALTH.set(80D);
            if(ModConfig.HADDAD_HEALTH.get()==30D)ModConfig.HADDAD_HEALTH.set(80D);
            if(ModConfig.BOULOS_HEALTH.get()==60D)ModConfig.BOULOS_HEALTH.set(100D);
            if(ModConfig.DILMA_HEALTH.get()==40D)ModConfig.DILMA_HEALTH.set(150D);
            if(ModConfig.TABATA_HEALTH.get()==30D)ModConfig.TABATA_HEALTH.set(80D);
            if(ModConfig.MARINA_HEALTH.get()==30D)ModConfig.MARINA_HEALTH.set(80D);
            if(ModConfig.CIRO_HEALTH.get()==40D)ModConfig.CIRO_HEALTH.set(100D);
            ModConfig.BALANCE_REVISION.set(3);
        }
        // 0.020: Renan Santos becomes a boss (the 80 of the 0.019 tests, or the old fixed 30).
        if(ModConfig.BALANCE_REVISION.get()<4){
            if(ModConfig.RENAN_HEALTH.get()==80D || ModConfig.RENAN_HEALTH.get()==30D)ModConfig.RENAN_HEALTH.set(300D);
            ModConfig.BALANCE_REVISION.set(4);
        }
    }
    private ConfigMigrations(){}
}
