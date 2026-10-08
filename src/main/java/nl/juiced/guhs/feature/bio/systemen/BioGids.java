package nl.juiced.guhs.feature.bio.systemen;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.bouwdal.Cadeaus;
import nl.juiced.guhs.feature.guhpixel.Bouwer;
import nl.juiced.guhs.feature.guhpixel.GidsSectie;

/**
 * biomes3: the Guhdex section of the weebhuisje (tab "Guhpixel &amp; uitjes", next to the Reisbureau's album, which is the
 * comparable thing): how many presents Evivads and Nielsvads brought, the twelve pieces of the Japan collection greyed out
 * until they were given, and the four outfits. Shown once the player got a first present (no spoiler before that), and
 * without the Guhpixel unlock.
 */
public final class BioGids implements GidsSectie {
    private static final String G = "gui.guhs.biosystemen.gids.";

    @Override
    public String id() {
        return "biosystemen";
    }

    @Override
    public int volgorde() {
        return 85;   // (right after the Reisbureau, 80)
    }

    @Override
    public boolean zichtbaar(ServerPlayer p) {
        return Cadeaus.aantal(p) > 0 || Cadeaus.reeks(p) != 0;
    }

    @Override
    public boolean zonderToegang() {
        return true;
    }

    @Override
    public void vul(ServerPlayer p, Bouwer b) {
        int reeks = Cadeaus.reeks(p), outfits = Cadeaus.outfits(p);
        b.kop(Component.translatable(G + "kop"));
        b.regel(Component.translatable(G + "uitleg"));
        b.stat(Component.translatable(G + "cadeaus"), Component.literal(String.valueOf(Cadeaus.aantal(p))));
        b.voortgang(Component.translatable(G + "reeks"), Integer.bitCount(reeks & Cadeaus.REEKS_VOL), Cadeaus.REEKS.length);
        b.voortgang(Component.translatable(G + "outfits"), Integer.bitCount(outfits & Cadeaus.OUTFITS_VOL), Cadeaus.OUTFITS.length);
        for (int i = 0; i < Cadeaus.REEKS.length; i++) {
            ItemStack s = new ItemStack(Bio.item(Cadeaus.REEKS[i], Items.PAPER));
            b.plaatje(s, s.getHoverName(), Component.translatable(G + "tip"), (reeks & 1 << i) != 0);
        }
    }
}
