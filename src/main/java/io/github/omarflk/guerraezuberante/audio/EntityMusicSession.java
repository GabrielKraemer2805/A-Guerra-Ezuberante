package io.github.omarflk.guerraezuberante.audio;

import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import java.util.*;

/** One recording per activation. A persistent combat target must never create an endless playlist. */
public final class EntityMusicSession {
    public enum State { STOPPED, PLAYING }
    private final RetaliatingCharacterEntity owner;
    private final ShuffleBag<Integer> bag;
    private State state = State.STOPPED;
    private long ends, nextClick;
    private boolean combatActive;
    private int serial;
    public EntityMusicSession(RetaliatingCharacterEntity owner) {
        this.owner = owner;
        var available = new ArrayList<Integer>();
        for(int i=0;i<owner.musicTracks().size();i++)
            if(owner.isInteractionTrack(i) && owner.musicTracks().get(i).available()) available.add(i);
        bag = new ShuffleBag<>(available, owner.getRandom().nextLong());
    }
    public State state() { return state; }
    public long endsAt() { return ends; }
    public void activateCombat() {
        if(combatActive || !owner.canPlayCharacterAudio()) return;
        combatActive = true;
        if(state == State.STOPPED && owner.startsAudioOnCombat()) advance();
    }
    public boolean interact() {
        long now = owner.level().getGameTime();
        if(now < nextClick || !owner.canPlayCharacterAudio()) return false;
        nextClick = now + 10;
        advance(); return true;
    }
    public void tick(boolean combat) {
        if(!owner.canPlayCharacterAudio()) { stop(); return; }
        if(combat) activateCombat(); else combatActive = false;
        if(state == State.PLAYING && owner.level().getGameTime() >= ends) stop();
    }
    private void advance() {
        var next = bag.next();
        if(next.isEmpty()) { stop(); return; }
        playTrack(next.get());
    }
    /** Reactions share the synchronized channel, but are excluded from the interaction shuffle. */
    public boolean playTrack(int index) {
        if(!owner.canPlayCharacterAudio() || index < 0 || index >= owner.musicTracks().size()
                || !owner.musicTracks().get(index).available()) return false;
        long now = owner.level().getGameTime();
        state = State.PLAYING; ends = now + owner.musicTracks().get(index).durationTicks();
        owner.syncMusic(index, now, ++serial);
        return true;
    }
    public void stop() {
        if(state == State.STOPPED) return;
        state = State.STOPPED;
        owner.syncMusic(-1, owner.level().getGameTime(), ++serial);
    }
}
