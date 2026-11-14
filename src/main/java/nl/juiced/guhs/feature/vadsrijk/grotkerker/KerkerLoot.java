package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Materialen;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.vint.RijdierBuit;

/**
 * U-GROTKERKER: what goes into a player's personal end chest, on EVERY clear (only for players above kern's 5 % contribution):
 * <ul>
 *   <li>Vadsmunten: 400·zone² + 300 brons;</li>
 *   <li>one gear piece at the boss's level that the player can use (a weapon of their class or any armour): the very first clear
 *       of this cave EPISCH, after that ZELDZAAM with {@link #EPISCH_KANS} for EPISCH;</li>
 *   <li>cave materials: 3-5 ores of the zone's mining tier (30 % "vadsig"), and {@link #KERN_KANS} for the zone's boss core;</li>
 *   <li>the rare cave PET (a chibi of the boss): {@link #PET_KANS} per clear, sure after {@link #PET_PITY} misses;</li>
 *   <li>the rare cave MOUNT (a Pluimguh colour): the user's mount rule through {@link RijdierBuit} (10 % per player per clear, sure
 *       after 15), only once you own a Pluimguh.</li>
 * </ul>
 * The pet and the mount are handed out right away (with their own moment); the rest goes into the chest.
 */
public final class KerkerLoot {
    public static final double EPISCH_KANS = 0.12, KERN_KANS = 0.25, PET_KANS = 0.06;
    public static final int PET_PITY = 25;
    /** 1101-balans: gear pieces in the chest (was 1). */
    public static final int STUKKEN = 2;
    /** Test hook: the next pet roll (null = random). */
    @Nullable
    static Double testPetRol;

    private KerkerLoot() {
    }

    static CompoundTag data(ServerPlayer p) {
        return Spelers.data(p).extra(GrotKerker.DATA);
    }

    public static LootTabel.Resultaat kist(GrotBaas b, ServerPlayer p) {
        return kist(b.grot(), Math.max(1, b.mobLevel()), p, true);
    }

    /** The chest of cave {@code grot} at boss level {@code lv}; {@code zeldzaam} = also roll the pet + mount. */
    public static LootTabel.Resultaat kist(String grot, int lv, ServerPlayer p, boolean zeldzaam) {
        KerkerData.Kerker k = Kerkers.van(grot);
        int zone = k == null ? 1 : k.zone();
        RandomSource r = p.getRandom();
        List<ItemStack> buit = new ArrayList<>();
        boolean eerste = GrotKerker.clears(p, grot) == 0;
        for (int i = 0; i < STUKKEN; i++) {   // 1101-balans: 2 pieces per clear (was 1); first clear: the first one Episch
            BasisType t = stukVoor(p, zone);
            if (t != null) {
                Zeldzaamheid z = (eerste && i == 0) || r.nextDouble() < EPISCH_KANS ? Zeldzaamheid.EPISCH : Zeldzaamheid.ZELDZAAM;
                buit.add(Rollen.stack(Rollen.rol(t, lv, z, r)));
            }
        }
        List<Materialen.Materiaal> ertsen = Materialen.van(Ambacht.MIJNEN, Math.min(5, zone));
        if (!ertsen.isEmpty()) {
            Materialen.Materiaal m = ertsen.get(r.nextInt(ertsen.size()));
            ItemStack s = Materialen.stack(m.id(), r.nextDouble() < 0.3 ? 1 : 0, 3 + r.nextInt(3));
            if (!s.isEmpty()) {
                buit.add(s);
            }
        }
        if (zone <= Materialen.KERNEN.size() && r.nextDouble() < KERN_KANS) {
            ItemStack s = Materialen.stack(Materialen.KERNEN.get(zone - 1), 0, 1);
            if (!s.isEmpty()) {
                buit.add(s);
            }
        }
        if (zeldzaam) {
            GrotBazen.Def d = GrotBazen.def(grot);
            if (d != null) {
                rolPet(p, d);
                RijdierBuit.rol(p, d.mountBron());
            }
        }
        return new LootTabel.Resultaat(buit, 400L * zone * zone + 300);
    }

    /** The rare pet roll ({@link #PET_KANS}, pity {@link #PET_PITY}); true = given now. Nothing (and no count) when owned. */
    public static boolean rolPet(ServerPlayer p, GrotBazen.Def d) {
        if (Huisdieren.heeft(p, d.pet())) {
            return false;
        }
        String sleutel = d.grot() + ".petpech";
        int pech = data(p).getIntOr(sleutel, 0);
        double rol = testPetRol != null ? testPetRol : p.getRandom().nextDouble();
        testPetRol = null;
        if (pech >= PET_PITY || rol < PET_KANS) {
            data(p).remove(sleutel);
            Huisdieren.geef(p, d.pet(), Huisdieren.MOMENT_WACHT);
            p.sendSystemMessage(Component.translatable("vadsrijk.grotkerker.zeldzaam", Component.translatable("vadsrijk.huisdier." + d.pet() + ".naam"))
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            return true;
        }
        data(p).putInt(sleutel, pech + 1);
        return false;
    }

    /** A random piece the player can use: a weapon of their class or any armour (like the cave treasures). */
    @Nullable
    static BasisType stukVoor(ServerPlayer p, int zone) {
        return nl.juiced.guhs.feature.vadsrijk.kern.loot.ZonePools.stuk(zone, Spelers.data(p).klasse, p.getRandom());   // 1101-balans: only this zone's pool
    }
}
