package nl.juiced.guhs.feature.safari.extra;

import java.util.UUID;

import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.SafariKoppelingen;
import nl.juiced.guhs.feature.safari.gedrag.Bewoners;
import nl.juiced.guhs.feature.safari.gedrag.SafariHoedje;
import nl.juiced.guhs.feature.safari.lobby.SafariWinkel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/** G1 (the gaps of the design audit): the safari hat, the giftshop's dex requirement, the paintings, Dikkie, the Aussies. */
public class SafariExtraGameTests {
    private static final String BATCH = "safari";

    /** Own guhs wear a safari hat in the park and get their own hat back outside. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void safariHoedjeOpEnAf(GameTestHelper helper) {
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhEntity g = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        g.tame(p);
        g.wear(GuhClothes.STRAW_HAT);
        SafariHoedje.pas(g, true);
        helper.assertTrue(g.getClothes(GuhClothes.SAFARI_HOEDJE.slot) == GuhClothes.SAFARI_HOEDJE, "a safari hat in the park");
        SafariHoedje.pas(g, true);
        SafariHoedje.pas(g, false);
        helper.assertTrue(g.getClothes(GuhClothes.SAFARI_HOEDJE.slot) == GuhClothes.STRAW_HAT, "its own hat back outside (also after a reload)");
        GuhEntity kaal = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 1, 2));
        kaal.tame(p);
        kaal.takeOff(GuhClothes.SAFARI_HOEDJE.slot);
        SafariHoedje.pas(kaal, true);
        SafariHoedje.pas(kaal, false);
        helper.assertTrue(kaal.getClothes(GuhClothes.SAFARI_HOEDJE.slot) == null, "a guh without a hat is bare again");
        helper.succeed();
    }

    /** The giftshop's requirement: a new player has photographed nothing, so a "dex 10" offer is locked; none = open. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void winkelEisDex(GameTestHelper helper) {
        JsonObject eis = new JsonObject();
        eis.addProperty("dex", 10);
        UUID nieuw = UUID.randomUUID();
        helper.assertTrue(SafariWinkel.eis(eis).apply(helper.getLevel().getServer(), nieuw) != null, "locked for a new player");
        helper.assertTrue(SafariWinkel.eis(null).apply(helper.getLevel().getServer(), nieuw) == null, "no requirement: open");
        helper.succeed();
    }

    /** The Kriebelhuis painting and a photo painting per species exist as painting variants. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void fotoSchilderijen(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ItemStack k = SafariKoppelingen.schilderij(server, "safari_kriebelhuis");
        helper.assertTrue(k.is(Items.PAINTING) && k.has(DataComponents.PAINTING_VARIANT), "the Kriebelhuis painting");
        int mist = 0;
        StringBuilder wie = new StringBuilder();
        for (ParkData.Soort s : ParkData.get().soorten.values()) {
            if (s.decor()) {
                continue;
            }
            if (SafariKoppelingen.schilderij(server, "safari_foto_" + s.naam()).isEmpty()) {
                mist++;
                wie.append(s.naam()).append(' ');
            }
        }
        helper.assertTrue(mist == 0, "every species has its photo painting, missing: " + wie);
        helper.succeed();
    }

    /** Dikkie of the side quest is a bekende bewoner, and never a park home's animal. The Aussies have their roles. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dikkieEnAussies(GameTestHelper helper) {
        helper.assertTrue(Bewoners.get().stream().anyMatch(b -> b.id().equals(Bewoners.DIKKIE)), "Dikkie is in the list");
        for (ParkData.Plek p : ParkData.get().plekken) {
            Bewoners.Bewoner b = Bewoners.voor(p, 0);
            helper.assertTrue(b == null || !b.id().startsWith(Bewoners.QUEST), "no park home gets Dikkie");
        }
        for (var kind : new nl.juiced.guhs.entity.GuhNpcEntity.Kind[] {nl.juiced.guhs.entity.GuhNpcEntity.Kind.SAFARI_AUSSIE_HANGMAT,
                nl.juiced.guhs.entity.GuhNpcEntity.Kind.SAFARI_AUSSIE_POST, nl.juiced.guhs.entity.GuhNpcEntity.Kind.SAFARI_AUSSIE_KAMP}) {
            NpcRole r = NpcRollen.rol(kind);
            helper.assertTrue(r != null, "a role for " + kind);
        }
        helper.succeed();
    }
}
