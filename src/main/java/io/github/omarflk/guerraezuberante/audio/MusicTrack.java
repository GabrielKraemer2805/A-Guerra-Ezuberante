package io.github.omarflk.guerraezuberante.audio;

import net.minecraft.resources.ResourceLocation;

/** Complete original recording; duration is ceil(ffprobe seconds * 20), never a guessed cooldown. */
public record MusicTrack(ResourceLocation sound, int durationTicks) {
    public MusicTrack {
        if (durationTicks < 1) throw new IllegalArgumentException("Positive duration required");
    }
    public boolean available() {
        String path = "/assets/" + sound.getNamespace() + "/sounds/" + sound.getPath().replace('.', '/') + ".ogg";
        try (var input = MusicTrack.class.getResourceAsStream(path)) { return input != null; }
        catch (java.io.IOException e) { return false; }
    }
}
