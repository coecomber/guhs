package nl.juiced.guhs.feature.vadsrijk.vint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Specs;

/**
 * V-INT (round U integration): the server-side glue between round-U slices that were built apart.
 * <ul>
 * <li>u-specs -> u-tabs: the specialisation line in the Karakter tab ({@code MenuServer.KARAKTER_EXTRA});</li>
 * <li>u-specs -> u-hud: the spec name under a party member's name ({@code PartyFrames.SPEC_BRON});</li>
 * <li>u-dubbel: u-ruiters' riders and the Knuffelkwartier are left out of the heuristic double scan.</li>
 * <li>u-z6raid: the Sterrennacht-Bultguh as a RijdierBuit source + bond xp per clear (Z6RaidHaken.OPGEVROLIJKT).</li>
 * </ul>
 */
public final class VInt {
    /** The raid's mount source (RijdierBuit) and the bond xp of one raid clear (the daily cap of 600 per member still applies). */
    public static final String Z6RAID_BRON = "z6raid_sterrennacht";
    public static final long RAID_BOND_XP = 200;
    /** The raid pocket's soundscape: no area themes, the tower drone. */
    public static final nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan.Streek RAID_STREEK =
            new nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan.Streek("z6raid_toren", java.util.List.of(), "toren");

    private VInt() {
    }

    public static void register(IEventBus modBus) {
        RijdierBuit.register();
        // u-z6verhaal's airship line E (Hoogvads Staarthaven <-> Rozenkloof) in u-luchtschip's "why closed" explanations: after the
        // swim film (flag z6_lijn_e_open) and from level 60; flying back is always open (Z6Lijn's own gate)
        nl.juiced.guhs.feature.vadsrijk.luchtschip.Poorten.POORTEN.put(nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6Lijn.ID,
                new nl.juiced.guhs.feature.vadsrijk.luchtschip.Poorten.Poort("z5_hq10_stormrand", 60,
                        p -> nl.juiced.guhs.feature.vadsrijk.npc.Questlog.vlag(p, "z6_lijn_e_open")));   // the user's mount-drop rule: 10 % per player per kill/clear, sure after 15 misses
        // u-dubbel's heuristic scan: u-ruiters (one pair per route#k, own join guard + sweep) and everything in the Knuffelkwartier
        // (u-huizen's figures + one Mollie per ward share an npc id on purpose) are not doubles.
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.NIET_HEURISTISCH.add(e -> e instanceof nl.juiced.guhs.feature.vadsrijk.ruiters.RuiterGuh
                || e instanceof nl.juiced.guhs.feature.vadsrijk.ruiters.RuiterPluim);
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.NIET_HEURISTISCH.add(e -> "knuffelkwartier".equals(e.level().dimension().identifier().getPath()));
        // u-z6raid: the Sterrennacht-Bultguh is a mount drop under the user's rule (10 % per player per clear, sure after 15 misses),
        // rolled for every group member in the tower at the clear; the clear also gives bond xp (u-gilden) on top of the 4 boss kills
        RijdierBuit.bron(Z6RAID_BRON, "Sterrennacht-Bultguh (Sterrendovende Toren, zone 6)", null, p -> true,
                nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidHalster::heeftSterrennacht,
                nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidHalster::geefSterrennacht);
        nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidHaken.OPGEVROLIJKT.add(p -> {
            RijdierBuit.rol(p, Z6RAID_BRON);
            nl.juiced.guhs.feature.vadsrijk.gilden.BondXp.deed(p, "bazen", 1, RAID_BOND_XP);
        });
        // u-z6raid + u-z6muziek: in the raid pocket (the Wachtzaal and the four floors) the area music is off and the tower drone
        // plays (bed "toren" = sfeer/bed_toren, the same sound as vr.z6raid.muziek.wachtzaal); the boss themes play on top in a fight.
        // Ringeling's victory fanfare (= vr.z6raid.muziek.opgevrolijkt, fanfare/ringeling) comes from u-audio2's Haken.VERSLAGEN hook.
        nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan.PLEK_STREKEN.add((x, y, z) ->
                nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidPlekken.verdieping(x, y, z) >= 0 ? RAID_STREEK : null);
        // u-questrobot's find (z6_s15_oostroute): the tracker pointed every soup bowl at refugee 1; now at the next one without soup
        nl.juiced.guhs.feature.vadsrijk.npc.QuestStap.PLEK_KIEZER.add((p, plek) -> {
            if (!"npc:z6_vluchteling_1".equals(plek)) {
                return plek;
            }
            CompoundTag soep = nl.juiced.guhs.feature.vadsrijk.npc.Questlog.map(p, "z6_soep");
            for (int i = 1; i <= 3; i++) {
                if (!soep.getBooleanOr("z6_vluchteling_" + i, false)) {
                    return "npc:z6_vluchteling_" + i;
                }
            }
            return plek;
        });
        nl.juiced.guhs.feature.vadsrijk.menu.MenuServer.KARAKTER_EXTRA.add((p, data) -> {
            ListTag extra = data.getListOrEmpty("extra");
            CompoundTag regel = new CompoundTag();
            Spec s = specVan(p);
            if (s != null) {
                regel.putString("k", "vadsrijk.vint.karakter.spec");
                regel.putString("a", s.naamKey());
            } else {
                regel.putString("k", "vadsrijk.vint.karakter.spec.geen");
            }
            extra.add(regel);
            data.put("extra", extra);
        });
        nl.juiced.guhs.feature.vadsrijk.hud.PartyFrames.SPEC_BRON.add(p -> {
            Spec s = specVan(p);
            return s == null ? null : s.naamKey();
        });
    }

    /** The spec of the player's current class (null: none chosen yet, or no class). */
    static Spec specVan(net.minecraft.world.entity.player.Player p) {
        try {
            if (Spelers.klasse(p) == null) {
                return null;
            }
            return Specs.spec(p);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
