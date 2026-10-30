package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GuerraEzuberanteMod.MOD_ID);
    public static final RegistryObject<SoundEvent> ME_RESPEITA = register("renan.me_respeita");
    public static final RegistryObject<SoundEvent> LULA_BEANS = register("lula.feijao_puro");
    public static final RegistryObject<SoundEvent> LULA_HOMOFOBIA = register("lula.homofobia");
    public static final RegistryObject<SoundEvent> LULA_MACHISMO = register("lula.machismo");
    public static final RegistryObject<SoundEvent> LULA_RACISMO = register("lula.racismo");
    public static final RegistryObject<SoundEvent> PISTOL_SHOT = register("pistol.shot");
    public static final RegistryObject<SoundEvent> PISTOL_RELOAD = register("pistol.reload");
    public static final RegistryObject<SoundEvent> PISTOL_EMPTY = register("pistol.empty");
    public static final RegistryObject<SoundEvent> RISADINHA = register("renan.risadinha");
    public static final RegistryObject<SoundEvent> PHONK = register("renan.phonk");
    public static final RegistryObject<SoundEvent> SUMMON = register("arthur.invocacao");
    public static final RegistryObject<SoundEvent> E_VAGABUNDO_SIM = register("arthur.e_vagabundo_sim");
    public static final RegistryObject<SoundEvent> FICOU_OFENDIDINHO = register("arthur.ficou_ofendidinho");
    public static final RegistryObject<SoundEvent> LIMPA = register("arthur.limpa_o_cu_com_a_linuga");
    public static final RegistryObject<SoundEvent> PARAR_DE_MAMAT = register("arthur.parar_de_mamat");
    public static final RegistryObject<SoundEvent> PRIVATIZAR_TUDO = register("arthur.privatizar_tudo");
    public static final RegistryObject<SoundEvent> SAO_FACEIS = register("arthur.sao_faceis_pq_sao_pobres");
    public static final RegistryObject<SoundEvent> SO_DEUSA = register("arthur.so_deusa");
    public static final RegistryObject<SoundEvent> JAIR_CANALHA_EVENT = register("jair.canalha");
    public static final RegistryObject<SoundEvent> JAIR_EU_NAO_TE = register("jair.eu_nao_te");
    public static final RegistryObject<SoundEvent> JAIR_POPCORN = register("jair.popcorn");
    public static final RegistryObject<SoundEvent> BOULOS_O_PO_EVENT = register("boulos.o_po");
    public static final RegistryObject<SoundEvent> HADDAD_TAXAD_EVENT = register("haddad.taxad");
    public static final RegistryObject<SoundEvent> HADDAD_TAPINHAS_EVENT = register("haddad.tapinhas");
    public static final RegistryObject<SoundEvent> HADDAD_PERDEU_EVENT = register("haddad.perdeu_ne_seu_tonto");
    /** Background music, looped while a player stands inside a Missão village (one folder per politician in "audios batalha"). */
    public static final RegistryObject<SoundEvent> BATTLE_RENAN = register("batalha.renan_santos");

    public record VoiceLine(RegistryObject<SoundEvent> event, String directory, String file, int cooldownTicks) {
        public VoiceLine(RegistryObject<SoundEvent> event, String file, int cooldownTicks) {
            this(event, "renan", file, cooldownTicks);
        }

        public String subtitleKey() {
            return "subtitles.guerra_ezuberante." + directory + "." + file;
        }

        public boolean isAvailable() {
            String path = "/assets/" + GuerraEzuberanteMod.MOD_ID + "/sounds/" + directory + "/" + file + ".ogg";
            try (InputStream stream = ModSounds.class.getResourceAsStream(path)) {
                return stream != null;
            } catch (IOException ignored) {
                return false;
            }
        }
    }

    // Full recordings are preserved. Cooldowns include their duration and a brief gap.
    public static final List<VoiceLine> RENAN_VOICES = List.of(
            new VoiceLine(ME_RESPEITA, "me_respeita", 300),
            new VoiceLine(RISADINHA, "risadinha", 80));

    public static final VoiceLine RENAN_PHONK = new VoiceLine(PHONK, "phonk", 280);
    public static final List<VoiceLine> LULA_VOICES = List.of(new VoiceLine(LULA_BEANS,"lula","feijao_puro",185),
            new VoiceLine(LULA_HOMOFOBIA,"lula","homofobia",109),new VoiceLine(LULA_MACHISMO,"lula","machismo",146),
            new VoiceLine(LULA_RACISMO,"lula","racismo",156));
    public static final VoiceLine ARTHUR_SUMMON = new VoiceLine(SUMMON, "arthur", "invocacao", 120);
    public static final List<VoiceLine> ARTHUR_VOICES = List.of(
            new VoiceLine(E_VAGABUNDO_SIM, "arthur", "e_vagabundo_sim", 220),
            new VoiceLine(FICOU_OFENDIDINHO, "arthur", "ficou_ofendidinho", 360),
            new VoiceLine(LIMPA, "arthur", "limpa_o_cu_com_a_linuga", 320),
            new VoiceLine(PARAR_DE_MAMAT, "arthur", "parar_de_mamat", 300),
            new VoiceLine(PRIVATIZAR_TUDO, "arthur", "privatizar_tudo", 360),
            new VoiceLine(SAO_FACEIS, "arthur", "sao_faceis_pq_sao_pobres", 180),
            new VoiceLine(SO_DEUSA, "arthur", "so_deusa", 220));

    // Full recordings. "Canalha" (6.55 s) only plays when Jair takes damage; the others on right-click.
    public static final VoiceLine JAIR_CANALHA = new VoiceLine(JAIR_CANALHA_EVENT, "jair", "canalha", 140);
    public static final List<VoiceLine> JAIR_INTERACTION_VOICES = List.of(
            new VoiceLine(JAIR_EU_NAO_TE, "jair", "eu_nao_te", 1110),
            new VoiceLine(JAIR_POPCORN, "jair", "popcorn", 890));
    public static final List<VoiceLine> JAIR_VOICES = List.of(JAIR_CANALHA,
            JAIR_INTERACTION_VOICES.get(0), JAIR_INTERACTION_VOICES.get(1));

    // Excerpts chosen by the collaborator: Boulos 0–7 s and Haddad 34–45 s of the received MP3s.
    public static final VoiceLine BOULOS_O_PO = new VoiceLine(BOULOS_O_PO_EVENT, "boulos", "o_po", 140);
    public static final VoiceLine HADDAD_TAXAD = new VoiceLine(HADDAD_TAXAD_EVENT, "haddad", "taxad", 220);
    // Haddad's damage lines, 5–8 s and 8–11 s of the Pavanato video; only on damage, never on right-click.
    public static final VoiceLine HADDAD_TAPINHAS = new VoiceLine(HADDAD_TAPINHAS_EVENT, "haddad", "tapinhas", 60);
    public static final VoiceLine HADDAD_PERDEU = new VoiceLine(HADDAD_PERDEU_EVENT, "haddad", "perdeu_ne_seu_tonto", 60);
    public static final List<VoiceLine> HADDAD_VOICES = List.of(HADDAD_TAXAD, HADDAD_TAPINHAS, HADDAD_PERDEU);

    // Author selected the full recording; its 11-second cooldown exceeds the 9.98-second audio.
    public static final List<VoiceLine> ARTHUR_REACTIONS = List.of(ARTHUR_VOICES.get(0));

    private static RegistryObject<SoundEvent> register(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(GuerraEzuberanteMod.MOD_ID, name)));
    }

    private ModSounds() {}
}
