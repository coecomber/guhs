package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * G-MOUNTS: the mount system's rules. Ownership and selection (per player, colours of the Pluimguh, buying at the stal), the zone-3
 * starter choice (once, only a starter colour, the quest hook fires), and the dismount rules (a hit = off + a "geschrokken"
 * cooldown, fainting = off, a boss arena = off, towns/open world = keep riding).
 * Run: {@code runGameTestServer -Pgt=VadsrijkRijdieren}.
 */
public class VadsrijkRijdierenGameTests {
    private static final String BATCH = "vadsrijk_rijdieren";

    @GuhTest(template = "empty", batch = BATCH)
    public static void eigendomEnKeuze(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(Rijdieren.actief(p) == null, "no mount yet");
        for (RijdierSoort s : RijdierSoort.values()) {
            helper.assertTrue(!Rijdieren.heeft(p, s), "nothing owned: " + s.id());
        }
        helper.assertTrue(Rijdieren.geef(p, RijdierSoort.WOLKENSCHAAPJE) && !Rijdieren.geef(p, RijdierSoort.WOLKENSCHAAPJE), "given once");
        helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.WOLKENSCHAAPJE, "the first mount becomes active");
        helper.assertTrue(Rijdieren.geefKleur(p, PluimKleur.MINT) && !Rijdieren.geefKleur(p, PluimKleur.MINT), "mint once");
        helper.assertTrue(Rijdieren.heeft(p, RijdierSoort.PLUIMGUH) && Rijdieren.heeftKleur(p, PluimKleur.MINT)
                && !Rijdieren.heeftKleur(p, PluimKleur.ROZE), "pluimguh in mint only");
        helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.WOLKENSCHAAPJE, "a new mount does not steal the active slot");
        helper.assertTrue(!Rijdieren.kies(p, RijdierSoort.PLUIMGUH, PluimKleur.ROZE), "no riding a colour you lack");
        helper.assertTrue(!Rijdieren.kies(p, RijdierSoort.SLAKKIE, null), "no riding a mount you lack");
        helper.assertTrue(Rijdieren.kies(p, RijdierSoort.PLUIMGUH, PluimKleur.MINT), "pick the mint pluimguh");
        helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.PLUIMGUH && Rijdieren.kleur(p, RijdierSoort.PLUIMGUH) == PluimKleur.MINT, "active + colour");
        helper.assertTrue(Spelers.data(p).extra("economie").getCompoundOrEmpty("rijdieren").contains("pluimguh"), "stored per player");
        // buying at the stal
        Spelers.data(p).munten = 0;
        helper.assertTrue(Stal.koop(p, RijdierSoort.SLAKKIE, null).equals("vadsrijk.economie.smid.te_weinig") && !Rijdieren.heeft(p, RijdierSoort.SLAKKIE),
                "no money, no snail");
        Spelers.data(p).munten = RijdierSoort.SLAKKIE.prijs + 7;
        helper.assertTrue(Stal.koop(p, RijdierSoort.SLAKKIE, null).equals("vadsrijk.economie.stal.gekocht") && Rijdieren.heeft(p, RijdierSoort.SLAKKIE)
                && Spelers.data(p).munten == 7, "bought for exactly its price");
        helper.assertTrue(Stal.koop(p, RijdierSoort.WOLKENSCHAAPJE, null).equals("vadsrijk.economie.rijdier.al"), "not twice");
        helper.assertTrue(Stal.koop(p, RijdierSoort.KLODDERROS, null).isEmpty() && !Rijdieren.heeft(p, RijdierSoort.KLODDERROS), "boss mounts are not sold");
        helper.assertTrue(Stal.koop(p, RijdierSoort.PLUIMGUH, PluimKleur.STERRENNACHT).isEmpty(), "boss colours are not sold");
        Spelers.data(p).munten = PluimKleur.ROZE.prijs;
        helper.assertTrue(Stal.koop(p, RijdierSoort.PLUIMGUH, PluimKleur.ROZE).equals("vadsrijk.economie.stal.gekocht") && Rijdieren.heeftKleur(p, PluimKleur.ROZE),
                "another starter colour for sale once you have a pluimguh");
        helper.assertTrue(Rijdieren.geef(p, "guhwolf") && Rijdieren.heeft(p, RijdierSoort.GUHWOLF) && !Rijdieren.geef(p, "guhwolf"), "quest API by id");
        helper.assertTrue(Rijdieren.geef(p, "pluimguh:honing") && Rijdieren.heeftKleur(p, PluimKleur.HONING) && !Rijdieren.geef(p, "bestaatniet"),
                "colour by id, unknown id refused");
        Rijdieren.alles(p);
        for (RijdierSoort s : RijdierSoort.values()) {
            helper.assertTrue(Rijdieren.heeft(p, s) && EcoSpul.soort(EcoSpul.type(s)) == s, "alle + type: " + s.id());
        }
        for (PluimKleur k : PluimKleur.values()) {
            helper.assertTrue(Rijdieren.heeftKleur(p, k), "alle colours: " + k.id());
        }
        Rijdieren.reset(p);
        helper.assertTrue(Rijdieren.actief(p) == null && !Rijdieren.heeft(p, RijdierSoort.PLUIMGUH), "reset");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void starterKeuze(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        AtomicInteger gekozen = new AtomicInteger();
        BiConsumer<ServerPlayer, PluimKleur> haak = (q, k) -> {
            if (q == p) {
                gekozen.incrementAndGet();
            }
        };
        Rijdieren.STARTER_GEKOZEN.add(haak);
        try {
            helper.assertTrue(!Rijdieren.heeftStarter(p), "no starter yet");
            Spelers.data(p).munten = 1_000_000;
            helper.assertTrue(Stal.koop(p, RijdierSoort.PLUIMGUH, PluimKleur.MINT).equals("vadsrijk.economie.stal.eerst_starter")
                    && !Rijdieren.heeftKleur(p, PluimKleur.MINT), "no buying a starter colour before the free choice");
            helper.assertTrue(!Rijdieren.geefStarter(p, PluimKleur.HONING), "honey is no starter");
            helper.assertTrue(Rijdieren.kiesStarter(p), "the choice opens");
            helper.assertTrue(Rijdieren.geefStarter(p, PluimKleur.LAVENDEL), "pick lavender");
            helper.assertTrue(Rijdieren.heeftStarter(p) && Rijdieren.heeftKleur(p, PluimKleur.LAVENDEL) && !Rijdieren.heeftKleur(p, PluimKleur.ROZE)
                    && !Rijdieren.heeftKleur(p, PluimKleur.MINT), "only lavender for free");
            helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.PLUIMGUH && Rijdieren.kleur(p, RijdierSoort.PLUIMGUH) == PluimKleur.LAVENDEL,
                    "the starter is ridden at once");
            helper.assertTrue(gekozen.get() == 1, "the quest hook fired once");
            helper.assertTrue(!Rijdieren.geefStarter(p, PluimKleur.MINT) && !Rijdieren.heeftKleur(p, PluimKleur.MINT), "only one free starter");
            helper.assertTrue(!Rijdieren.kiesStarter(p), "the choice does not open again");
            helper.assertTrue(gekozen.get() == 1, "no second hook");
            long voor = Spelers.data(p).munten;
            helper.assertTrue(Stal.koop(p, RijdierSoort.PLUIMGUH, PluimKleur.MINT).equals("vadsrijk.economie.stal.gekocht")
                    && Spelers.data(p).munten == voor - PluimKleur.MINT.prijs, "the other starters at the stal");
            Rijdieren.reset(p);
            helper.assertTrue(!Rijdieren.heeftStarter(p), "dev reset forgets the choice");
        } finally {
            Rijdieren.STARTER_GEKOZEN.remove(haak);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void afstijgRegels(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Rijdieren.geefKleur(p, PluimKleur.ROZE);
        RijdierEntity r = Rijdieren.roep(p, RijdierSoort.PLUIMGUH);
        helper.assertTrue(r != null && p.getVehicle() == r && r.kleur() == PluimKleur.ROZE, "summoned and seated, in its colour");
        helper.assertTrue(Rijdieren.hudStaat(p)[0] == Rijdieren.HUD_RIJDT, "HUD: riding");
        Rijdieren.geraakt(p);
        helper.assertTrue(!r.isAlive() && p.getVehicle() == null, "a hit: off it goes");
        helper.assertTrue(Rijdieren.cooldown(p) > 0 && Rijdieren.hudStaat(p)[0] == Rijdieren.HUD_GESCHROKKEN, "HUD: scared, cooldown");
        RijdierEntity r2 = Rijdieren.roep(p, RijdierSoort.PLUIMGUH);
        helper.assertTrue(r2 != null && p.getVehicle() == r2, "seated again");
        Haken.FLAUW.forEach(h -> h.accept(p));
        helper.assertTrue(!r2.isAlive() && p.getVehicle() == null, "fainting: off");
        Arena a = null;
        for (String id : Arenas.ids()) {
            a = Arenas.van(id);
            if (a != null) {
                break;
            }
        }
        helper.assertTrue(a != null, "there are boss arenas");
        BlockPos c = a.centrum();
        helper.assertTrue("arena".equals(Rijdieren.afReden(true, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5)), "in an arena: off");
        helper.assertTrue(Rijdieren.afReden(true, c.getX() + a.straal() + 40, c.getY(), c.getZ()) == null, "outside the arena: keep riding");
        BlockPos stad = EconomiePlekken.MARKT;
        helper.assertTrue(Rijdieren.afReden(true, stad.getX() + 0.5, stad.getY(), stad.getZ() + 0.5) == null, "in town: keep riding");
        helper.assertTrue("rijk".equals(Rijdieren.afReden(false, 0, 64, 0)), "outside the Vadsrijk: off");
        Rijdieren.geraakt(p);   // (a hit while not riding does nothing)
        helper.succeed();
    }
}
