package nl.juiced.guhs.feature.vadsrijk.ambacht;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.kleding.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.kleding.Kledingkast;

/**
 * U-AMBACHT2: the Meesterschort path (VOORSTEL_PROFESSIONS "Meesterschort", deferred by t-ambacht because lv 100 was beyond the cap).
 * It now SCALES WITH THE CAP: an ambacht at the current cap ({@link AmbachtXp#CAP}, 60 now, 70 with zone 6) makes you
 * "Meester-&lt;ambacht&gt;" (a title, shown at home too) and the first time unlocks the Meesterschort, a cosmetic cape in the
 * Kledingkast (A2: cosmetics, never power). All nine at the cap = "Grootmeester". Every new cap is a new mastery moment: the
 * reward is remembered per ambacht AND cap ({@code AmbachtData "meester"}: "smeden@60"), so when the cap rises to 70 reaching 70
 * gives the moment (+ Knutselpenningen) again; the title always follows the current cap.
 */
public final class Meesterschap {
    public static final String KAAP = "kaap:meesterschort";
    public static final int PENNINGEN = 20;

    private Meesterschap() {
    }

    static void registreer() {
        if (Cosmetica.van(KAAP) == null) {
            Cosmetica.registreer(new Cosmetica.Ding(KAAP, Cosmetica.Soort.KAAP, "geheim:meesterschort", null, 0, 0));
        }
        for (Ambacht a : Ambacht.values()) {
            String id = titelId(a);
            if (Titels.van(id) == null) {
                Titels.registreer(new Titels.Titel(id, "gui.guhs.titels.naam." + id, ChatFormatting.LIGHT_PURPLE, a.icoon,
                        p -> AmbachtData.level(p, a) >= AmbachtXp.CAP));
            }
        }
        if (Titels.van("vr_amb_grootmeester") == null) {
            Titels.registreer(new Titels.Titel("vr_amb_grootmeester", "gui.guhs.titels.naam.vr_amb_grootmeester", ChatFormatting.GOLD, "minecraft:nether_star",
                    p -> {
                        for (Ambacht a : Ambacht.values()) {
                            if (AmbachtData.level(p, a) < AmbachtXp.CAP) {
                                return false;
                            }
                        }
                        return true;
                    }));
        }
        AmbachtData.LEVEL_OP.add((p, a, lv) -> {
            if (lv >= AmbachtXp.CAP) {
                beloon(p, a);
            }
        });
    }

    public static String titelId(Ambacht a) {
        return "vr_amb_" + a.id + "_meester";
    }

    public static boolean meester(ServerPlayer p, Ambacht a) {
        return AmbachtData.level(p, a) >= AmbachtXp.CAP;
    }

    /** At login (a player who reached the cap before this existed) and on a level-up to the cap. */
    static void check(ServerPlayer p) {
        for (Ambacht a : Ambacht.values()) {
            if (meester(p, a)) {
                beloon(p, a);
            }
        }
    }

    /** The mastery moment, once per ambacht per cap. */
    static void beloon(ServerPlayer p, Ambacht a) {
        if (!AmbachtData.voegToe(p, "meester", a.id + "@" + AmbachtXp.CAP)) {
            return;
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.meester.nu", Component.translatable(a.naamKey()), AmbachtXp.CAP)
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        EcoUtil.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        Knutselwinkel.geef(p, PENNINGEN);
        try {
            Kledingkast.ontgrendel(p, KAAP, true);
        } catch (RuntimeException | LinkageError e) {
            // (the wardrobe's store is not there, e.g. a bare gametest: the title still counts)
        }
    }
}
