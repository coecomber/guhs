package nl.juiced.guhs.feature.guhpixel.reisbureau;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.guhpixel.Bouwer;
import nl.juiced.guhs.feature.guhpixel.GidsSectie;
import nl.juiced.guhs.taal.Tekst;

/**
 * The Reisbureau's section of the Guhdex tab "Guhpixel &amp; uitjes" (also shown before the Guhpixel unlock): the questline
 * of the Reisagent-guh, who is away now, the reispas (trips, stamps, Gouden koffertjes) and the album: every ansichtkaart
 * and all souvenirs, greyed out until the player has had them.
 */
public final class ReisGids implements GidsSectie {
    private static final String G = "gui.guhs.reisbureau.gids.";

    @Override
    public String id() {
        return "reisbureau";
    }

    @Override
    public int volgorde() {
        return 80;
    }

    @Override
    public boolean zichtbaar(ServerPlayer p) {
        return true;
    }

    @Override
    public boolean zonderToegang() {
        return true;
    }

    @Override
    public void vul(ServerPlayer p, Bouwer b) {
        CompoundTag d = Reizen.data(p);
        int stap = Reizen.stap(p);
        b.kop(Component.translatable(G + "kop"));
        b.regel(Component.translatable(stap == Reizen.NIEUW ? G + "zoek" : G + "uitleg"));
        b.stat(Component.translatable(G + "stap"), Component.translatable(G + "stap." + Math.max(0, Math.min(Reizen.KLAAR, stap))));
        CompoundTag reis = Reizen.reis(p.level().getServer(), p.getUUID());
        Bestemming waar = reis == null ? null : Reizen.bestemming(reis);
        if (reis != null && waar != null) {
            Component naam = Tekst.get(reis, "Naam");
            long rest = reis.getLongOr("Terug", 0L) - nl.juiced.guhs.feature.guhpixel.Klok.nu();
            b.stat(Component.translatable(G + "nu"), !Reizen.isWeg(reis) ? Component.translatable("gui.guhs.reisbureau.weg.vertrekt", naam)
                    : rest <= 0 ? Component.translatable("gui.guhs.reisbureau.weg.klaar", naam)
                    : Component.translatable(G + "nu.onderweg", naam, waar.naam(), Reizen.tijd(rest)));
        } else {
            b.stat(Component.translatable(G + "nu"), Component.translatable(G + "niemand"));
        }
        b.stat(Component.translatable(G + "reizen"), Component.literal(String.valueOf(d.getIntOr("Reizen", 0))));
        b.voortgang(Component.translatable(G + "stempels"), d.getIntOr("Stempels", 0), Reizen.STEMPELS_VOL);
        b.stat(Component.translatable(G + "koffertjes"), Component.literal(String.valueOf(d.getIntOr("Koffertjes", 0))));
        int kaarten = 0;
        for (Bestemming x : Bestemming.ECHT) {
            kaarten += Reizen.aantal(d, "Kaarten", x.id()) > 0 ? 1 : 0;
        }
        b.voortgang(Component.translatable(G + "kaarten"), kaarten, Bestemming.ECHT.size());
        b.voortgang(Component.translatable(G + "souvenirs"), Reizen.souvenirs(d), 2 * Bestemming.ECHT.size());
        b.kop(Component.translatable(G + "album"));
        for (Bestemming x : Bestemming.ECHT) {
            boolean heb = Reizen.aantal(d, "Souvenirs", "souvenir_" + x.id()) > 0;
            ItemStack s = new ItemStack(x.souvenir());
            b.plaatje(s, s.getHoverName(), Component.translatable(G + "tip.gewoon", x.naam()), heb);
        }
        for (Bestemming x : Bestemming.ECHT) {
            boolean heb = Reizen.aantal(d, "Souvenirs", "zeldzaam_" + x.id()) > 0;
            ItemStack s = new ItemStack(x.zeldzaam());
            b.plaatje(s, s.getHoverName(), Component.translatable(G + "tip.zeldzaam", x.naam()), heb);
        }
        for (Bestemming x : Bestemming.ECHT) {
            boolean heb = Reizen.aantal(d, "Kaarten", x.id()) > 0;
            ItemStack s = new ItemStack(x.kaart());
            b.plaatje(s, s.getHoverName(), x.uitleg(), heb);
        }
    }
}
