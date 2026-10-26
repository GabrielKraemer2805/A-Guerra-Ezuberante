package io.github.omarflk.guerraezuberante.arsenal;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Original integration only: TaCZ owns firing, damage, ADS, recoil, reloads and networking. */
public final class Arsenal {
    public static final WeaponDefinition AKM = new WeaponDefinition("ak47", "ak47", "762x39", "akm");
    public static final WeaponDefinition M4A1 = new WeaponDefinition("m4a1", "m4a1", "556x45");
    public static final WeaponDefinition GLOCK = new WeaponDefinition("glock17", "glock_17", "9mm", "glock_17");
    public static final WeaponDefinition DEAGLE = new WeaponDefinition("deagle", "deagle", "50ae");
    public static final WeaponDefinition MP5 = new WeaponDefinition("mp5", "hk_mp5a5", "9mm", "hk_mp5a5", "mp5a5");
    public static final List<WeaponDefinition> WEAPONS = List.of(AKM, M4A1, GLOCK, DEAGLE, MP5);

    public record Kit(ItemStack gun, List<ItemStack> ammo, int reserveRounds) {}

    public static Optional<WeaponDefinition> find(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return WEAPONS.stream().filter(w -> w.command().equals(normalized) || w.aliases().contains(normalized)).findFirst();
    }

    /** Resolve at use-time, after common gunpack data has loaded or synchronized. Never forceBuild. */
    public static Optional<CommonGunIndex> availableIndex(WeaponDefinition weapon) {
        return TimelessAPI.getCommonGunIndex(weapon.gunId()).filter(index -> index.getGunData() != null
                && weapon.ammoId().equals(index.getGunData().getAmmoId())
                && !index.getGunData().getFireModeSet().isEmpty()
                && TimelessAPI.getCommonAmmoIndex(weapon.ammoId()).isPresent());
    }

    public static ItemStack createGun(WeaponDefinition weapon) {
        return availableIndex(weapon).map(index -> {
            var data = index.getGunData();
            ItemStack stack = GunItemBuilder.create().setId(weapon.gunId())
                    .setFireMode(data.getFireModeSet().get(0)).setAmmoCount(data.getAmmoAmount())
                    .setAmmoInBarrel(data.getBolt() != com.tacz.guns.resource.pojo.data.gun.Bolt.OPEN_BOLT)
                    .setHeatData(data.hasHeatData()).build();
            IGun gun = IGun.getIGunOrNull(stack);
            return gun != null && weapon.gunId().equals(gun.getGunId(stack)) ? stack : ItemStack.EMPTY;
        }).orElse(ItemStack.EMPTY);
    }

    public static ItemStack createAmmo(WeaponDefinition weapon, int count) {
        if (count <= 0 || availableIndex(weapon).isEmpty()) { return ItemStack.EMPTY; }
        ItemStack stack = AmmoItemBuilder.create().setId(weapon.ammoId()).setCount(count).build();
        IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
        return ammo != null && weapon.ammoId().equals(ammo.getAmmoId(stack)) ? stack : ItemStack.EMPTY;
    }

    /** Build and validate the entire kit before changing any inventory. Split by the real ammo stack limit. */
    public static Optional<Kit> createKit(WeaponDefinition weapon) {
        var index = availableIndex(weapon);
        if (index.isEmpty()) { return Optional.empty(); }
        ItemStack gun = createGun(weapon);
        ItemStack sample = createAmmo(weapon, 1);
        if (gun.isEmpty() || sample.isEmpty()) { return Optional.empty(); }
        int rounds = Math.max(64, index.get().getGunData().getAmmoAmount() * 4);
        int stackSize = Math.max(1, sample.getMaxStackSize());
        List<ItemStack> ammo = new ArrayList<>();
        for (int remaining = rounds; remaining > 0;) {
            int count = Math.min(remaining, stackSize);
            ItemStack stack = sample.copyWithCount(count);
            ammo.add(stack);
            remaining -= count;
        }
        return Optional.of(new Kit(gun, List.copyOf(ammo), rounds));
    }

    public static List<ItemStack> creativeEntries() {
        List<ItemStack> stacks = new ArrayList<>();
        var ammoIds = new java.util.HashSet<net.minecraft.resources.ResourceLocation>();
        for (WeaponDefinition weapon : WEAPONS) {
            ItemStack gun = createGun(weapon);
            if (gun.isEmpty()) { continue; }
            stacks.add(gun);
            if (ammoIds.add(weapon.ammoId())) {
                ItemStack ammo = createAmmo(weapon, 1);
                if (!ammo.isEmpty()) { stacks.add(ammo); }
            }
        }
        return stacks;
    }

    private Arsenal() {}
}
