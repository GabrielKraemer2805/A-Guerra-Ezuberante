package io.github.omarflk.guerraezuberante.client.renderer;
import io.github.omarflk.guerraezuberante.entity.SpecialDeathEntity;
import software.bernie.geckolib.core.object.Color;
public final class SpecialDeathTint {
    public static Color color(SpecialDeathEntity e,float partial){
        if(e.deathState()==SpecialDeathEntity.ALIVE)return Color.WHITE;
        float other=Math.max(.08F,.8F-e.deathAnimationAge(partial)*.016F);
        return e.greenDeathEnergy()?Color.ofRGBA(other,1F,other,1F):Color.ofRGBA(1F,other,other,1F);
    }
    private SpecialDeathTint(){}
}
