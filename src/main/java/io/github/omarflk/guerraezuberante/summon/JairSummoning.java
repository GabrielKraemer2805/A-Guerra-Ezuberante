package io.github.omarflk.guerraezuberante.summon;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.JairBolsonaroEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;

public final class JairSummoning {
    public static int summon(JairBolsonaroEntity owner){
        if(!(owner.level() instanceof ServerLevel server) || !owner.isAlive() || owner.deathState()!=JairBolsonaroEntity.ALIVE
                || !ModConfig.JAIR_SUMMON_ENABLED.get() || !owner.validRangedTarget(owner.getTarget()))return 0;
        var ledger=SummonLedger.get(server);if(ledger.isClosed(owner.getUUID()))return 0;
        int requested=Math.min(ModConfig.JAIR_SUMMON_COUNT.get(),ModConfig.JAIR_MAX_SUMMONS.get()-ledger.count(owner.getUUID())),created=0;
        for(int radius=2;radius<=5 && created<requested;radius++)for(int attempt=0;attempt<24 && created<requested;attempt++){
            double angle=attempt*2.399963229728653;
            var center=owner.blockPosition().offset((int)Math.round(Math.cos(angle)*radius),0,(int)Math.round(Math.sin(angle)*radius));
            for(int dy:new int[]{0,1,-1,2,-2}){
                var pos=center.above(dy);if(!ArthurSummoning.isSafe(server,pos))continue;
                var unit=ModEntities.FLAVIO_BOLSONARO.get().create(server);unit.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,owner.getYRot(),0);
                if(!server.noCollision(unit,unit.getBoundingBox()) || !server.getEntities(unit,unit.getBoundingBox()).isEmpty())continue;
                unit.bindTo(owner);
                if(server.addFreshEntity(unit)){
                    ledger.register(owner.getUUID(),unit.getUUID());created++;
                    server.sendParticles(new DustParticleOptions(new Vector3f(.05F,1,.1F),1.1F),unit.getX(),unit.getY()+1,unit.getZ(),14,.4,.7,.4,.02);
                    server.sendParticles(new DustParticleOptions(new Vector3f(1,.9F,.02F),1),unit.getX(),unit.getY()+.4,unit.getZ(),8,.4,.3,.4,.01);
                }
                break;
            }
        }
        return created;
    }
    private JairSummoning(){}
}
