package io.github.omarflk.guerraezuberante.combat;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.JairBolsonaroEntity;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID)
public final class BolsonaroCombatEvents {
    @SubscribeEvent public static void protectAllies(ExplosionEvent.Detonate event){
        if(event.getExplosion().getDirectSourceEntity() instanceof JairBolsonaroEntity owner)
            event.getAffectedEntities().removeIf(v->owner.isAlliedTo(v) || v.isAlliedTo(owner));
    }
    private BolsonaroCombatEvents(){}
}
