package nl.juiced.guhs.feature.einde2.spd2finale;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.EigenWezen;
import nl.juiced.guhs.feature.einde2.sponsguh.MeneerKrabguhEntity;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 "Sponsguh", slice sp_d2_finale: the clicks on the cast ({@code SponsguhWezen.BIJ_KLIK}, CONTRACT_E2_C 4.3). It answers
 * only (a) the player's own entities of this slice (his Sponsguh, Patguh, bucket guhs, freed guhs, Plankmika on the stage),
 * (b) a resident while the player's current step is one of this slice's (9-11), and (c) after the whole story: every story
 * character of the cast answers with one or two lines of its own ("after" lines), and Meneer Krabguh sells a Knabbelburger for
 * three kaasknabbels held in the paw. Never a buddy or pet with an owner, never the spots of sp_zij (Sandy-guh, Zeemeerman,
 * Mosselguh, Octo at home, the ghost captain). Small talk is a chat line.
 */
final class D2fKlik {
    static final String G = E2SpD2Finale.G;
    /** The price of a Knabbelburger after the story (kaasknabbels). */
    static final int PRIJS = 3;
    /** sp_zij's spots: always its own (CONTRACT_E2_C 4.3). */
    static final Set<String> ZIJ = Set.of("bikinibodem/sandy", "bikinibodem/zeemeerman", "bikinibodem/mosselguh", "bikinibodem/octo",
            "bikinibodem/spookkapitein");
    /** The casts with "after" lines, and how many each has. */
    static final Map<Cast, Integer> NA = Map.ofEntries(Map.entry(Cast.MENEER_KRABGUH, 2), Map.entry(Cast.SPONSGUH, 2), Map.entry(Cast.PATGUH, 2),
            Map.entry(Cast.PAREL_GUH, 2), Map.entry(Cast.OCTO_MIKA, 2), Map.entry(Cast.OCTOLIAM, 1), Map.entry(Cast.PLANKMIKA, 2),
            Map.entry(Cast.JUF_PUFGUH, 1), Map.entry(Cast.BIKINIBODEM_GUH, 2), Map.entry(Cast.CYCLOOP_MIKA, 1), Map.entry(Cast.DAVID_HASSELGUH, 1),
            Map.entry(Cast.LOOPGRAAFMONSTER, 1), Map.entry(Cast.GERRIT, 1));
    /** Per player and cast: which "after" line comes next (they take turns). */
    private static final Map<String, Integer> BEURT = new ConcurrentHashMap<>();

    private D2fKlik() {
    }

    static boolean klik(SponsguhWezen w, ServerPlayer p) {
        if (w instanceof EigenWezen ew && ew.heeftEigenaar()) {
            return false;                                                    // (a buddy or a pet: its own clicks)
        }
        // (a) the player's own entities of this slice
        if (w.entityTags().contains(D2fStoet.MERK)) {
            if (!p.getUUID().equals(Zicht.eigenaar(w))) {
                return false;
            }
            D2fStoet.Sessie s = D2fStoet.als(p);
            String key;
            if (s != null && w == s.sponsguh) {
                key = "klik.sponsguh";
            } else if (s != null && w == s.patguh) {
                key = "klik.patguh";
            } else if (s != null && w == s.plankmika) {
                key = "klik.plankmika";
            } else if (s != null && java.util.Arrays.asList(s.emmers).contains(w)) {
                key = "klik.emmer";
            } else {
                key = "klik.vrij";
            }
            praat(w, p, G + key);
            return true;
        }
        String plek = w.getPersistentData().getStringOr(Bewoners.PLEK, "");
        if (plek.isEmpty() || ZIJ.contains(plek) || !Bewoners.isBewoner(w)) {
            return false;
        }
        int stap = SpVerhaal.KROON_LIJN.stap(p);
        // (b) a resident during this slice's steps: a guh of the town has hidden from the buckets
        if (SpVerhaal.receptKlaar(p) && !SpVerhaal.klaar(p) && stap >= SpVerhaal.K_HASSELGUH && stap <= SpVerhaal.K_GUHFY) {
            if (w.cast() == Cast.BIKINIBODEM_GUH && plek.startsWith(SpVerhaal.BIKINIBODEM + "/") && stap == SpVerhaal.K_EMMERS) {
                praat(w, p, G + "klik.bewoner");
                return true;
            }
            return false;
        }
        // (c) after the whole story
        if (!SpVerhaal.klaar(p)) {
            return false;
        }
        if (w.cast() == Cast.MENEER_KRABGUH && !(w instanceof MeneerKrabguhEntity k && k.isBevroren()) && verkoop(w, p)) {
            return true;
        }
        Integer n = NA.get(w.cast());
        if (n == null) {
            return false;
        }
        String beurtKey = p.getUUID() + "/" + w.cast().id();
        int i = BEURT.merge(beurtKey, 1, Integer::sum);
        praat(w, p, G + "na." + w.cast().id() + "." + (1 + (i - 1) % n));
        return true;
    }

    /** Krabguh sells: three kaasknabbels held in the main hand buy one Knabbelburger. False: nothing in the paw (just talk). */
    private static boolean verkoop(SponsguhWezen w, ServerPlayer p) {
        ItemStack hand = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (!hand.is(ModItems.KAAS_KNABBELS.get())) {
            return false;
        }
        w.getLookControl().setLookAt(p);
        if (hand.getCount() < PRIJS && !p.getAbilities().instabuild) {
            w.zetAnimatie("praat");
            p.sendSystemMessage(Component.translatable(G + "na.krabguh.te_weinig").withStyle(ChatFormatting.WHITE));
            return true;
        }
        if (!p.getAbilities().instabuild) {
            hand.shrink(PRIJS);
        }
        Minigames.give(p, new ItemStack(E2SponsguhFeature.KNABBELBURGER.get()));
        w.zetAnimatie("lach");
        w.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.3f);
        p.sendSystemMessage(Component.translatable(G + "na.krabguh.verkocht").withStyle(ChatFormatting.WHITE));
        return true;
    }

    private static void praat(SponsguhWezen w, ServerPlayer p, String key) {
        w.getLookControl().setLookAt(p);
        w.zetAnimatie(w.cast() == Cast.GERRIT ? "njeg" : "praat");
        GuhQuests.say(p, w, key);
    }

    static void vergeet(UUID speler) {
        String pre = speler + "/";
        BEURT.keySet().removeIf(k -> k.startsWith(pre));
    }
}
