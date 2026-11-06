package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The mount screen ("Rijdieren", G-MOUNTS): every mount, the ones you have in colour with their level, xp, speed and jump, the
 * others as a silhouette with where to get them; pick the active mount and (Pluimguh) its colour, with a turning preview.
 * <ul>
 *   <li>{@link #overzicht}: the mount key N, anywhere in the Vadsrijk (no shop);</li>
 *   <li>{@link #open}: at the stalmeester (npc slice), the same screen with buy buttons (stal mounts, the other starter colours,
 *   honey) and a spare rijfluitje;</li>
 *   <li>{@link #starterScherm}: the zone-3 starter choice ({@code Rijdieren.kiesStarter}).</li>
 * </ul>
 */
public final class Stal {
    public static final long FLUITJE_PRIJS = 25;
    static final String CTX_STAL = "stal";

    private Stal() {
    }

    /** API (npc slice): opens the stal (with the shop). Only in the Vadsrijk. */
    public static void open(ServerPlayer p) {
        if (Vadsrijk.in(p)) {
            stuur(p, CTX_STAL, "");
        }
    }

    /** The mount key N: the mount screen without the shop. */
    public static void overzicht(ServerPlayer p) {
        nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.inhalen(p);   // T-RIJDIER: achievements/boss/secret looks first
        if (!Rijdieren.heeftEen(p)) {   // (I-MOUNTS: mounts start with the zone-3 starter)
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.geen");
            return;
        }
        if (Vadsrijk.in(p)) {
            stuur(p, "", "");
        }
    }

    static CompoundTag data(ServerPlayer p, boolean stal) {
        CompoundTag d = new CompoundTag();
        ListTag l = new ListTag();
        RijdierSoort actief = Rijdieren.actief(p);
        for (RijdierSoort s : RijdierSoort.values()) {
            CompoundTag t = new CompoundTag();
            boolean heeft = Rijdieren.heeft(p, s);
            int level = heeft ? Rijdieren.level(p, s) : 1;
            t.putString("soort", s.id());
            t.putBoolean("heeft", heeft);
            t.putInt("level", level);
            t.putLong("xp", heeft ? Rijdieren.xp(p, s) : 0);
            t.putLong("nodig", RijdierSoort.xpNodig(level));
            t.putDouble("snelheid", s.snelheid(level));
            t.putDouble("sprong", s.sprong(level));
            t.putBoolean("actief", s == actief);
            t.putLong("prijs", s.prijs);
            if (s.kleuren()) {
                t.putString("kleur", Rijdieren.kleur(p, s).id());
                ListTag kl = new ListTag();
                for (PluimKleur k : PluimKleur.values()) {
                    CompoundTag kt = new CompoundTag();
                    kt.putString("id", k.id());
                    kt.putBoolean("heeft", Rijdieren.heeftKleur(p, k));
                    kt.putLong("prijs", k.prijs);
                    kl.add(kt);
                }
                t.put("kleuren", kl);
            }
            nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.schermData(p, s, t);   // T-RIJDIER: looks + where the new mounts come from
            l.add(t);
        }
        d.put("rijdieren", l);
        d.putBoolean("stal", stal);
        d.putBoolean("starter", Rijdieren.heeftStarter(p));
        d.putBoolean("fluitje", EcoUtil.tel(p.getInventory(), EcoSpul.RIJFLUITJE.get()) > 0);
        d.putLong("munten", Spelers.data(p).munten);
        nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.schermData(p, d);   // T-RIJDIER: Gouden Veren
        return d;
    }

    private static void stuur(ServerPlayer p, String ctx, String melding) {
        CompoundTag d = data(p, CTX_STAL.equals(ctx));
        d.putString("melding", melding);
        EcoPayloads.open(p, EcoPayloads.STAL, ctx, d, List.of());
    }

    static void doe(ServerPlayer p, String ctx, String actie, CompoundTag a) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        boolean stal = CTX_STAL.equals(ctx);
        String melding = "";
        RijdierSoort s = RijdierSoort.van(a.getStringOr("soort", ""));
        PluimKleur k = PluimKleur.vanOfNull(a.getStringOr("kleur", ""));
        switch (actie) {
            case "kies" -> {
                if (s != null && Rijdieren.kies(p, s, s.kleuren() ? k : null)) {
                    melding = "vadsrijk.economie.stal.gekozen";
                    EcoUtil.geluid(p, SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.7f, 1.4f);
                }
            }
            case "koop" -> melding = stal && s != null ? koop(p, s, k) : "";
            case "fluitje" -> {
                if (!stal) {
                    melding = "";
                } else if (EcoUtil.tel(p.getInventory(), EcoSpul.RIJFLUITJE.get()) > 0) {
                    melding = "vadsrijk.economie.stal.heb_al";
                } else if (EcoUtil.vrij(p.getInventory()) == 0) {
                    melding = "vadsrijk.economie.winkel.tas_vol";
                } else if (!Spelers.neemMunten(p, FLUITJE_PRIJS)) {
                    melding = "vadsrijk.economie.smid.te_weinig";
                } else {
                    EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.RIJFLUITJE.get()));
                    melding = "vadsrijk.economie.stal.fluitje";
                }
            }
            default -> {
                String m = nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.doe(p, stal, actie, a);   // T-RIJDIER: looks / feathers
                melding = m == null ? "" : m;
            }
        }
        stuur(p, ctx, melding);
    }

    /** Buys a stal mount or a Pluimguh colour. Returns the message key. */
    static String koop(ServerPlayer p, RijdierSoort s, PluimKleur k) {
        if (s.kleuren()) {
            if (k == null || k.prijs <= 0) {
                return "";
            }
            if (Rijdieren.heeftKleur(p, k)) {
                return "vadsrijk.economie.rijdier.al";
            }
            if (!Rijdieren.heeftStarter(p) && !Rijdieren.heeft(p, RijdierSoort.PLUIMGUH)) {
                return "vadsrijk.economie.stal.eerst_starter";
            }
            if (!Spelers.neemMunten(p, k.prijs)) {
                return "vadsrijk.economie.smid.te_weinig";
            }
            Rijdieren.geefKleur(p, k);
            return "vadsrijk.economie.stal.gekocht";
        }
        if (s.prijs <= 0) {
            return "";
        }
        if (Rijdieren.heeft(p, s)) {
            return "vadsrijk.economie.rijdier.al";
        }
        if (!Rijdieren.heeftEen(p)) {   // (I-MOUNTS: the stal sells only after your first mount, the zone-3 starter)
            return "vadsrijk.economie.stal.eerst_starter";
        }
        if (!Spelers.neemMunten(p, s.prijs)) {
            return "vadsrijk.economie.smid.te_weinig";
        }
        Rijdieren.geef(p, s);
        return "vadsrijk.economie.stal.gekocht";
    }

    // ---- the starter choice (zone 3) -----------------------------------------------------------------------------------------

    static void starterScherm(ServerPlayer p) {
        CompoundTag d = new CompoundTag();
        ListTag l = new ListTag();
        for (PluimKleur k : PluimKleur.values()) {
            if (k.starter) {
                l.add(net.minecraft.nbt.StringTag.valueOf(k.id()));
            }
        }
        d.put("starters", l);
        RijdierSoort s = RijdierSoort.PLUIMGUH;
        d.putDouble("snelheid", s.snelheid(1));
        d.putDouble("sprong", s.sprong(1));
        EcoPayloads.open(p, EcoPayloads.STARTER, "", d, List.of());
    }

    static void doeStarter(ServerPlayer p, String actie, CompoundTag a) {
        if (!"kies".equals(actie)) {
            return;
        }
        PluimKleur k = PluimKleur.vanOfNull(a.getStringOr("kleur", ""));
        if (k != null && Rijdieren.geefStarter(p, k)) {
            EcoPayloads.sluit(p);
        }
    }
}
