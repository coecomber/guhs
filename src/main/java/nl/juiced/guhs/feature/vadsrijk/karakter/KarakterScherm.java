package nl.juiced.guhs.feature.vadsrijk.karakter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * T-KARAKTER, server side of the character screen: what it shows ({@link #data}) and what its buttons do ({@link #doe}).
 * The server checks everything again (free slot, name, only lowering the mode, the place rule).
 */
public final class KarakterScherm {
    private KarakterScherm() {
    }

    /** Opens (or refreshes) the screen. {@code aankomst} = shown on entering the Vadsrijk ("Verder als ..."). */
    public static void open(ServerPlayer p, boolean aankomst) {
        CompoundTag d = data(p);
        d.putBoolean("aankomst", aankomst);
        KarakterPayloads.stuur(p, d);
    }

    static CompoundTag data(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        int actief = Karakter.slot(p);
        t.putInt("actief", actief);
        String niet = Karakters.waaromNiet(p);
        t.putBoolean("mag", niet == null);
        t.putString("waarom", niet == null ? "" : niet);
        ListTag l = new ListTag();
        SpelerData d = Spelers.data(p);
        for (int i = 0; i < Karakter.SLOTS; i++) {
            CompoundTag s = new CompoundTag();
            s.putInt("slot", i);
            if (!Karakter.bestaat(p, i)) {
                s.putBoolean("leeg", true);
                l.add(s);
                continue;
            }
            CompoundTag info = Karakter.info(p, i);
            s.putString("naam", info.getStringOr("naam", "?"));
            s.putString("modus", info.getStringOr("modus", Modus.NORMAAL.id));
            s.putString("begin", info.getStringOr("begin", Modus.NORMAAL.id));
            s.putBoolean("pensioen", info.getBooleanOr("pensioen", false));
            s.putBoolean("nieuw", info.getBooleanOr("nieuw", false));
            if (i == actief) {
                s.putInt("glevel", d.level());
                s.putInt("level", d.klasseLevel());
                s.putString("klasse", d.klasse);
                s.putLong("munten", d.munten);
            } else {
                s.putInt("glevel", info.getIntOr("glevel", 1));
                s.putInt("level", info.getIntOr("level", 1));
                s.putString("klasse", info.getStringOr("klasse", "knuffelridder"));
                s.putLong("munten", info.getLongOr("munten", 0));
            }
            s.putString("badge", (s.getBooleanOr("pensioen", false) ? "p" : "") + (Modus.van(s.getStringOr("modus", "")).badge.isEmpty() ? ""
                    : s.getStringOr("modus", "")));
            l.add(s);
        }
        t.put("slots", l);
        return t;
    }

    static void doe(ServerPlayer p, String actie, CompoundTag a) {
        int slot = a.getIntOr("slot", -1);
        switch (actie) {
            case "open" -> open(p, false);
            case "wissel" -> {
                if (Karakters.wissel(p, slot, false)) {
                    p.closeContainer();
                }
                open(p, false);
            }
            case "maak" -> {
                if (Karakters.maak(p, slot, a.getStringOr("naam", ""), Modus.van(a.getStringOr("modus", Modus.NORMAAL.id)))) {
                    if (a.getBooleanOr("spelen", false)) {
                        Karakters.wissel(p, slot, false);
                    }
                }
                open(p, false);
            }
            case "naam" -> {
                Karakters.hernoem(p, slot, a.getStringOr("naam", ""));
                open(p, false);
            }
            case "modus" -> {
                Karakters.zetModus(p, slot, Modus.van(a.getStringOr("modus", "")), false);
                open(p, false);
            }
            default -> {
            }
        }
    }
}
