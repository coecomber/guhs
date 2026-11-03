package nl.juiced.guhs.feature.erfenis;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;

/**
 * De Erfenis: "Handje helpen" (the kaart slice's button on another player's village). Speeds up the running job that ends
 * first (a building, an upgrade or a plukje) by up to {@code minuten}, never more than 25% of that job's whole time. With no
 * job running, the first source building (no inputs) whose basket isn't full makes {@code minuten} of its production at once
 * (again at most 25% of its basket). Never anything lost; the owner is caught up first.
 */
public final class ErfenisHulp {
    private ErfenisHulp() {
    }

    /** Returns the whole minutes actually saved (rounded up), 0 when nothing could be sped up. */
    public static int versnel(MinecraftServer server, UUID eigenaar, int minuten) {
        ErfenisData data = ErfenisData.get(server);
        Base b = data.van(eigenaar);
        if (b == null || b.npc || minuten <= 0) {
            return 0;
        }
        Erfenis.bijwerken(server, b, false);
        long nu = System.currentTimeMillis();
        Base.Bouw eerst = null;
        for (Base.Bouw x : b.bouwen) {
            if (x.eind() > nu && (eerst == null || x.eind() < eerst.eind())) {
                eerst = x;
            }
        }
        int gewonnen = 0;
        if (eerst != null) {
            long scheelt = Math.min(minuten * 60_000L, (eerst.eind() - eerst.start()) / 4);
            scheelt = Math.min(scheelt, eerst.eind() - nu);
            if (scheelt > 0) {
                b.bouwen.set(b.bouwen.indexOf(eerst), new Base.Bouw(eerst.soort(), eerst.ref(), eerst.naar(), eerst.start(), eerst.eind() - scheelt));
                gewonnen = (int) ((scheelt + 59_999) / 60_000);
            }
        } else {
            for (Gebouw g : b.gebouwen) {
                Recept r = Productie.recept(g);
                if (r == null || g.level <= 0 || !r.in().isEmpty() || g.vol()) {
                    continue;
                }
                double rate = Productie.nuPerUur(b, g);
                double cap = Productie.bufferCap(g);
                double erbij = Math.min(Math.min(rate * minuten / 60.0, cap * 0.25), cap - g.inBuffer(r.uit()));
                if (erbij <= 1e-6) {
                    continue;
                }
                g.bijBuffer(r.uit(), erbij);
                gewonnen = (int) Math.ceil(erbij / rate * 60.0 - 1e-6);
                break;
            }
        }
        if (gewonnen > 0) {
            data.setDirty();
            Erfenis.bijwerken(server, b, true);
            Erfenis.syncKijkers(server, b);
        }
        return gewonnen;
    }
}
