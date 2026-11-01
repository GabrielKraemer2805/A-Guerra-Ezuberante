package io.github.omarflk.guerraezuberante.client.renderer;
import io.github.omarflk.guerraezuberante.entity.SpecialDeathEntity;
import software.bernie.geckolib.core.object.Color;
public final class SpecialDeathTint {
    public static Color color(SpecialDeathEntity e,float partial){
        if(e.deathState()==SpecialDeathEntity.ALIVE)return Color.WHITE;
        float other=Math.max(.08F,.8F-e.deathAnimationAge(partial)*.016F);
        var energy=e.deathEnergyColor();
        // Each channel fades towards the energy colour: red, green, blue or yellow.
        float max=Math.max(energy.x(),Math.max(energy.y(),energy.z()));
        return Color.ofRGBA(other+(1-other)*energy.x()/max,other+(1-other)*energy.y()/max,other+(1-other)*energy.z()/max,1F);
    }
    private SpecialDeathTint(){}
}
