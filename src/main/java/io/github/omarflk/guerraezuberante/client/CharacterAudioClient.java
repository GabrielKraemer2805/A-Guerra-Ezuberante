package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** One positional, non-looping instance per tracked entity; never a global soundtrack. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class CharacterAudioClient {
    private static final Map<UUID, AttachedRecording> active = new HashMap<>();
    private static final Map<UUID, Integer> observed = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel previousLevel;
    public static int activeCount() { return active.size(); }
    public static int instancesFor(UUID owner) { return active.containsKey(owner) ? 1 : 0; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.level!=previousLevel) {
            active.values().forEach(s->{s.cancel();mc.getSoundManager().stop(s);});
            active.clear();observed.clear();previousLevel=mc.level;
        }
        if(mc.level==null)return;
        var seen=new HashSet<UUID>();
        for(var e:mc.level.entitiesForRendering()) {
            if(!(e instanceof RetaliatingCharacterEntity entity) || entity.musicTracks().isEmpty())continue;
            UUID id=entity.getUUID(); seen.add(id);
            int serial=entity.musicSerial(), index=entity.musicTrackIndex();
            boolean valid=entity.isAlive() && index>=0 && index<entity.musicTracks().size();
            var old=active.get(id);
            if(old!=null && (!valid || old.serial!=serial || old.owner!=entity)) {
                old.cancel();mc.getSoundManager().stop(old);active.remove(id);
            }
            if(!Objects.equals(observed.put(id,serial),serial) && valid) {
                var sound=new AttachedRecording(entity,index,serial);
                active.put(id,sound);mc.getSoundManager().play(sound);
            }
        }
        var iterator=active.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();
            if(!seen.contains(entry.getKey()) || entry.getValue().isStopped()) {
                entry.getValue().cancel();mc.getSoundManager().stop(entry.getValue());iterator.remove();
            }
        }
        observed.keySet().retainAll(seen);
    }
    private static final class AttachedRecording extends AbstractTickableSoundInstance {
        final RetaliatingCharacterEntity owner;
        final int serial;
        AttachedRecording(RetaliatingCharacterEntity owner,int index,int serial) {
            super(SoundEvent.createVariableRangeEvent(owner.musicTracks().get(index).sound()), SoundSource.RECORDS, RandomSource.create());
            this.owner=owner;this.serial=serial;
            looping=false;relative=false;attenuation=Attenuation.LINEAR;volume=owner.musicVolume();pitch=1;
            x=owner.getX();y=owner.getY();z=owner.getZ();
        }
        void cancel() { stop(); }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            if(owner.isRemoved() || !owner.isAlive() || Minecraft.getInstance().level!=owner.level()
                    || owner.musicSerial()!=serial || owner.musicTrackIndex()<0) {stop();return;}
            x=owner.getX();y=owner.getY();z=owner.getZ();volume=owner.musicVolume();
        }
    }
    private CharacterAudioClient() {}
}
