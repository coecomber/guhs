package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The stal at the stalmeester: your mounts with their level, xp, speed and jump; pick the one the rijfluitje calls; a new
 * rijfluitje when you lost yours. API for the npc slice: {@link #open}.
 */
public final class Stal {
    public static final long FLUITJE_PRIJS = 25;

    private Stal() {
    }

    /** API (npc slice): opens the stal. Only in the Vadsrijk. */
    public static void open(ServerPlayer p) {
        if (Vadsrijk.in(p)) {
            stuur(p, "");
        }
    }

    private static void stuur(ServerPlayer p, String melding) {
        CompoundTag d = new CompoundTag();
        List<ItemStack> items = new ArrayList<>();
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
            t.putInt("item", items.size());
            items.add(new ItemStack(s == RijdierSoort.KLODDERROS ? EcoSpul.ZADEL_KLODDERROS.get() : EcoSpul.ZADEL_HOEDHUPPER.get()));
            l.add(t);
        }
        d.put("rijdieren", l);
        d.putBoolean("fluitje", EcoUtil.tel(p.getInventory(), EcoSpul.RIJFLUITJE.get()) > 0);
        d.putLong("munten", Spelers.data(p).munten);
        d.putString("melding", melding);
        EcoPayloads.open(p, EcoPayloads.STAL, "", d, items);
    }

    static void doe(ServerPlayer p, String actie, CompoundTag a) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        String melding = "";
        switch (actie) {
            case "kies" -> {
                RijdierSoort s = RijdierSoort.van(a.getStringOr("soort", ""));
                if (s != null && Rijdieren.heeft(p, s)) {
                    Rijdieren.kies(p, s);
                    melding = "vadsrijk.economie.stal.gekozen";
                    EcoUtil.geluid(p, SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.7f, 1.4f);
                }
            }
            case "fluitje" -> {
                if (EcoUtil.tel(p.getInventory(), EcoSpul.RIJFLUITJE.get()) > 0) {
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
            }
        }
        stuur(p, melding);
    }
}
