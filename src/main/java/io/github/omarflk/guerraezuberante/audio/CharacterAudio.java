package io.github.omarflk.guerraezuberante.audio;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Whole recordings from the author's folders. Identical MP3 copies are represented once. */
public final class CharacterAudio {
    private static MusicTrack track(String name, int ticks) {
        return new MusicTrack(new ResourceLocation("guerra_ezuberante", name), ticks);
    }
    public static final List<MusicTrack> RENAN = List.of(
            track("renan.me_respeita",270), track("renan.phonk",252), track("renan.risadinha",48));
    // ceil(ffprobe duration * 20), not the old voice cooldown (which included padding).
    public static final int JAIR_DAMAGE_TRACK = 2;
    public static final List<MusicTrack> JAIR = List.of(
            track("jair.eu_nao_te",1066), track("jair.popcorn",848), track("jair.canalha",131));
    public static final List<MusicTrack> LULA = List.of(track("lula.feijao_puro",185),track("lula.homofobia",109),
            track("lula.machismo",146),track("lula.racismo",156));
    public static final List<MusicTrack> ARTHUR = List.of(
            track("arthur.e_vagabundo_sim",200), track("arthur.ficou_ofendidinho",332),
            track("arthur.limpa_o_cu_com_a_linuga",281), track("arthur.parar_de_mamat",264),
            track("arthur.privatizar_tudo",334), track("arthur.sao_faceis_pq_sao_pobres",158), track("arthur.so_deusa",189));
    // Excerpts the collaborator cut from longer videos; durations measured on the OGG.
    public static final List<MusicTrack> BOULOS = List.of(track("boulos.o_po",140));
    // Taxad on right-click; tapinhas on damage, replaced by "perdeu né seu tonto" below low_health_voice_fraction.
    public static final int HADDAD_DAMAGE_TRACK = 1, HADDAD_LOW_HEALTH_TRACK = 2;
    public static final List<MusicTrack> HADDAD = List.of(
            track("haddad.taxad",220), track("haddad.tapinhas",60), track("haddad.perdeu_ne_seu_tonto",60));
    private CharacterAudio() {}
}
