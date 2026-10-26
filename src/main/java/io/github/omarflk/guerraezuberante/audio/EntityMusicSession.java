package io.github.omarflk.guerraezuberante.audio;

import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import java.util.*;

/** One authoritative playlist per entity. Combat and interaction share this exact state. */
public final class EntityMusicSession {
    public enum State { STOPPED, PLAYING }
    private final RetaliatingCharacterEntity owner;
    private final ShuffleBag<Integer> bag;
    private State state = State.STOPPED;
    private long ends, manualUntil, combatUntil, nextClick;
    private int serial;
    public EntityMusicSession(RetaliatingCharacterEntity owner) {
        this.owner = owner;
        var available = new ArrayList<Integer>();
        for(int i=0;i<owner.musicTracks().size();i++) if(owner.musicTracks().get(i).available()) available.add(i);
        bag = new ShuffleBag<>(available, owner.getRandom().nextLong());
    }
    public State state() { return state; }
    public long endsAt() { return ends; }
    public void activateCombat() {
        combatUntil = owner.level().getGameTime() + ModConfig.CHARACTER_AUDIO_GRACE.get();
        if(state == State.STOPPED && owner.isAlive()) advance();
    }
    public boolean interact() {
        long now = owner.level().getGameTime();
        if(now < nextClick || !owner.isAlive()) return false;
        nextClick = now + 10;
        manualUntil = now + ModConfig.CHARACTER_AUDIO_SESSION.get();
        advance(); return true;
    }
    public void tick(boolean combat) {
        if(!owner.isAlive() || owner.isRemoved()) { stop(); return; }
        if(combat) activateCombat();
        if(state == State.PLAYING && owner.level().getGameTime() >= ends) {
            long now = owner.level().getGameTime();
            if(now < manualUntil || now < combatUntil) advance(); else stop();
        }
    }
    private void advance() {
        var next = bag.next();
        if(next.isEmpty()) { stop(); return; }
        int index = next.get(); long now = owner.level().getGameTime();
        state = State.PLAYING; ends = now + owner.musicTracks().get(index).durationTicks();
        owner.syncMusic(index, now, ++serial);
    }
    public void stop() {
        if(state == State.STOPPED) return;
        state = State.STOPPED; manualUntil = 0; combatUntil = 0;
        owner.syncMusic(-1, owner.level().getGameTime(), ++serial);
    }
}
