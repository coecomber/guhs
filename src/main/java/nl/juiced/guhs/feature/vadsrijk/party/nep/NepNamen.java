package nl.juiced.guhs.feature.vadsrijk.party.nep;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.RandomSource;

/**
 * W-PARTY: the gamer-style parody names of the nep-spelers and their fake chat (lang keys
 * {@code vadsrijk.party.nep.zeg.<soort>.<n>}, n = 1..{@link #ZEG}). Light parody: lowercase chat, "lfg", "brb mama roept", "gg".
 */
public final class NepNamen {
    public static final List<String> NAMEN = List.of("xX_Vadsjesloper_Xx", "KnuffelKoning2009", "guhlover_nl", "ProGrinder", "Klodderslayer99",
            "xXPluisjeXx", "MikaFan_2011", "VadsMaster3000", "NoobKnuffel", "xx_heler_xx", "TankieTom", "StiekemeSteve", "Kaasknabbel_HD",
            "PinkeGuhPro", "LagKoning", "iiPluisii", "Vadsje_YT", "Sl4apkop", "DrDruipgrot", "Zwamzoet_Zoe", "KnuffelNinja", "AFK_Annie",
            "Mr_Klodder", "GuhGamer_08", "xXx_Sporen_xXx", "BroodjeKnuffel", "TryHardTim", "Pluimpje2010", "OmaGamer", "Bassie_Speelt");

    /** How many lines each kind of chat has (lang keys 1..n). */
    public static final Map<String, Integer> ZEG = new LinkedHashMap<>();

    static {
        ZEG.put("los", 14);      // idle in the wild
        ZEG.put("gevecht", 8);   // fighting
        ZEG.put("heal", 4);      // the healer heals
        ZEG.put("flauw", 4);     // fainted
        ZEG.put("hoi", 5);       // somebody talks to it
        ZEG.put("ja", 4);        // recruited
        ZEG.put("nee", 2);       // cannot come along
        ZEG.put("doei", 5);      // time up / dismissed
        ZEG.put("gg", 4);        // after a defeat
        ZEG.put("vullen", 3);    // filled a dungeon queue
    }

    private NepNamen() {
    }

    /** A name not in {@code bezet} (when all are taken: a name + a number). */
    public static String naam(RandomSource r, Set<String> bezet) {
        for (int i = 0; i < 12; i++) {
            String n = NAMEN.get(r.nextInt(NAMEN.size()));
            if (!bezet.contains(n)) {
                return n;
            }
        }
        return NAMEN.get(r.nextInt(NAMEN.size())) + (r.nextInt(90) + 10);
    }

    /** A random line of this kind (a lang key). */
    public static String zeg(String soort, RandomSource r) {
        int n = ZEG.getOrDefault(soort, 1);
        return "vadsrijk.party.nep.zeg." + soort + "." + (1 + r.nextInt(n));
    }
}
