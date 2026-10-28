package io.github.omarflk.guerraezuberante.config;

import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Existing worlds receive the requested balance, while non-default server choices are preserved. */
public final class ConfigMigrations {
    public static void loading(ModConfigEvent.Loading event){migrate(event);}
    public static void reloading(ModConfigEvent.Reloading event){migrate(event);}
    private static void migrate(ModConfigEvent event){
        if(event.getConfig().getSpec()!=ModConfig.SPEC || ModConfig.BALANCE_REVISION.get()>=1)return;
        upgradeLegacyDefaults();event.getConfig().save();
    }
    public static void upgradeLegacyDefaults(){
        if(ModConfig.BALANCE_REVISION.get()>=1)return;
        if(ModConfig.LULA_HEALTH.get()==30D)ModConfig.LULA_HEALTH.set(300D);
        if(ModConfig.JAIR_HEALTH.get()==30D)ModConfig.JAIR_HEALTH.set(300D);
        if(ModConfig.FISSURE_DAMAGE.get()==6D)ModConfig.FISSURE_DAMAGE.set(10D);
        if(ModConfig.FISSURE_KNOCKBACK.get()==.35D)ModConfig.FISSURE_KNOCKBACK.set(.65D);
        if(ModConfig.FISSURE_COOLDOWN.get()==240)ModConfig.FISSURE_COOLDOWN.set(120);
        ModConfig.BALANCE_REVISION.set(1);
    }
    private ConfigMigrations(){}
}
