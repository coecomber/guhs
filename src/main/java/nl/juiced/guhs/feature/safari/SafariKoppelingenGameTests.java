package nl.juiced.guhs.feature.safari;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import nl.juiced.guhs.feature.safari.camera.Fotograaf;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.feature.safari.lobby.Rollen;
import nl.juiced.guhs.feature.safari.momenten.Momenten;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfWinkel;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordItem;
import nl.juiced.guhs.gametest.GuhTest;

/** Op Safari! step 2 integration: the hooks between the slices are wired ({@link SafariKoppelingen}). */
public class SafariKoppelingenGameTests {
    private static final String BATCH = "safari";

    /** S3 → S1: the birthday species counts as JARIG for the camera (double photo coins). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void jarigeSoortVoorDeCamera(GameTestHelper helper) {
        String jarig = Momenten.jarigeSoort();
        helper.assertTrue(jarig != null && !jarig.isEmpty(), "there is a birthday species today");
        helper.assertTrue(Fotograaf.JARIG.test(jarig), "the camera knows the birthday species " + jarig);
        helper.assertTrue(!Fotograaf.JARIG.test("geen_dier"), "other species are not jarig");
        helper.succeed();
    }

    /** S6 → S2: a zone expert hands out a real safari painting. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zoneSchilderij(GameTestHelper helper) {
        ItemStack s = Rollen.zoneSchilderij.apply(helper.getLevel().getServer(), "savanne");
        helper.assertTrue(s.is(Items.PAINTING) && s.has(DataComponents.PAINTING_VARIANT), "the savanne expert gives the savanne painting");
        helper.assertTrue(Rollen.zoneSchilderij.apply(helper.getLevel().getServer(), "entree").isEmpty(), "no painting for the entree");
        helper.succeed();
    }

    /** S4 → S5: the bordbon of the full dex becomes the Gouden dikdik's verblijfsbord. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bordbonGeeftGoudenDikdik(GameTestHelper helper) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "safari_koppel_test"));
        p.getInventory().clearContent();
        VerblijfWinkel.geef(p, "gouden_dikdik");
        boolean gevonden = false;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if ("gouden_dikdik".equals(VerblijfsbordItem.soort(p.getInventory().getItem(i)))) {
                gevonden = true;
            }
        }
        helper.assertTrue(gevonden, "the Gouden dikdik's board is in the inventory");
        helper.assertTrue("leeuw".equals(VerblijfsbordItem.soort(nl.juiced.guhs.feature.safari.lobby.SafariWinkel.borden.apply("leeuw"))),
                "the giftshop sells S5's lion board");
        p.getInventory().clearContent();
        SafariSpeler.wis(helper.getLevel().getServer(), p.getUUID());
        helper.succeed();
    }
}
