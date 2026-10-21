package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.List;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Blauwdrukken;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetTabs;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.vormen.VormInstelling;

/**
 * F9's tabs of the Bouwkeet menu, replacing F8's "binnenkort" stand-ins: Vormen and Daken (the vormstaf's settings at the
 * keet's level, "Plaats spook" makes it the keet's project in front of the door) and Blauwdrukken (the browser:
 * {@link Blauwdrukken}).
 * <p>
 * Buttons: Vormen/Daken 1 = a setting ({@code tekst} = the key, or "key=text" for a text), 2 = place the ghost.
 */
public final class KeetGereedschapTabs {
    public static void register() {
        BouwkeetTabs.registreer(new VormTab(BouwkeetTabs.VORMEN, 10, Gereedschap.VORM, Opties.KEET_VORMEN));
        BouwkeetTabs.registreer(new VormTab(BouwkeetTabs.DAKEN, 20, Gereedschap.DAK, Opties.KEET_DAKEN));
        BouwkeetTabs.registreer(new BlauwdrukTab());
    }

    public static final int OPTIE = 1, PLAATS = 2;

    record VormTab(String id, int volgorde, int modus, List<String> keys) implements BouwkeetTabs.Tab {
        @Override
        public void schrijf(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, CompoundTag uit) {
            BouwNiveau n = keet.werkNiveau();
            CompoundTag o = Opties.schrijf(Gereedschap.VORMSTAF, p, n, keys::contains, modus);
            uit.merge(o);
            Ontwerp ontwerp = ontwerp(p, n);
            Vec3i g = ontwerp.grootte();
            uit.putIntArray("Grootte", new int[] {g.getX(), g.getY(), g.getZ()});
            uit.putInt("Aantal", ontwerp.aantal());
        }

        private Ontwerp ontwerp(ServerPlayer p, BouwNiveau n) {
            return Vormstaf.ontwerp(VormInstelling.van(Opties.van(p, Gereedschap.VORMSTAF)), n, modus);
        }

        @Override
        public void doe(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, int actie, int waarde, String tekst) {
            BouwNiveau n = keet.werkNiveau();
            if (actie == OPTIE) {
                int is = tekst.indexOf('=');
                String key = is < 0 ? tekst : tekst.substring(0, is);
                if (keys.contains(key)) {
                    Opties.zet(Gereedschap.VORMSTAF, p, n, key, waarde, is < 0 ? "" : tekst.substring(is + 1));
                }
            } else if (actie == PLAATS) {
                VormInstelling v = VormInstelling.van(Opties.van(p, Gereedschap.VORMSTAF));
                Ontwerp o = Vormstaf.ontwerp(v, n, modus);
                if (o.isLeeg()) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.dicht.natuur"));
                    return;
                }
                String naam = Component.translatable(modus == Gereedschap.DAK ? "gui.guhs.bouwhulp.keuze.dak." + v.dak().id()
                        : "gui.guhs.bouwhulp.keuze.vorm." + v.vorm().id()).getString();
                Blauwdrukken.alsProject(p, level, keet, o, naam);
            }
        }
    }

    static final class BlauwdrukTab implements BouwkeetTabs.Tab {
        @Override
        public String id() {
            return BouwkeetTabs.BLAUWDRUKKEN;
        }

        @Override
        public int volgorde() {
            return 30;
        }

        @Override
        public void schrijf(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, CompoundTag uit) {
            Blauwdrukken.schrijf(p, uit, keet.werkNiveau());
        }

        @Override
        public void doe(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, int actie, int waarde, String tekst) {
            Blauwdrukken.doe(p, actie, waarde, tekst, keet);
        }
    }

    private KeetGereedschapTabs() {
    }
}
