package io.github.omarflk.guerraezuberante.arsenal;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

/** References to TaCZ's own gunpack entries, never replacement items or copied assets. */
public record WeaponDefinition(String command, ResourceLocation gunId, ResourceLocation ammoId, Set<String> aliases) {
    public WeaponDefinition(String command, String gunId, String ammoId, String... aliases) {
        this(command, new ResourceLocation("tacz", gunId), new ResourceLocation("tacz", ammoId), Set.of(aliases));
    }
}
