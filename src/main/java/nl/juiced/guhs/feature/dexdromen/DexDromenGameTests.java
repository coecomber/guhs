package nl.juiced.guhs.feature.dexdromen;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubSnoes;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the 1.8.0 Guhdex side: the nine new pages are bonus pages (Snoes' happy kind fills Snoes' page), and the
 * Blub-meterkaart: taming a Blub fills the box of its soort and tenth, meter 0 and 100 also their golden box.
 */
public class DexDromenGameTests {
    private static final String TUIN = "landdiertjes_test_tuin";

    @GuhTest(template = TUIN, batch = "dexdromen")
    public static void dexdromenPaginas(GameTestHelper helper) {
        for (GuhVariant v : java.util.List.of(GuhVariant.BLUBGUH, GuhVariant.STERRENVISJE, GuhVariant.SNOES, GuhVariant.ZANDGUH,
                GuhVariant.WEKKERGUH, GuhVariant.ROMMELGUH, GuhVariant.DROOM_OMAGUH, GuhVariant.KUSSENGEVECHT_GUH, GuhVariant.BUBBELBADGUH)) {
            helper.assertTrue(GuhDex.ENTRIES.contains(v) && GuhDex.EXTRA.contains(v) && !GuhDex.TELLEND.contains(v), v + ": a bonus page");
        }
        helper.assertTrue(GuhVariant.ofCharacter(GuhNpcEntity.Kind.SNOES) == GuhVariant.SNOES
                && GuhVariant.ofCharacter(GuhNpcEntity.Kind.SNOES_BLIJ) == GuhVariant.SNOES, "both Snoeses fill Snoes' page");
        helper.assertTrue(GuhVariant.BLUBGUH.npcKind() == null && GuhVariant.STERRENVISJE.npcKind() == null
                && GuhVariant.ZANDGUH.npcKind() == GuhNpcEntity.Kind.ZANDGUH, "creature pages vs character pages");
        helper.assertTrue(GuhDex.isCreaturePage(GuhVariant.STERRENVISJE), "the sterrenvisje is a creature page");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GuhTest(template = TUIN, batch = "dexdromen")
    public static void meterkaartVultBijTemmen(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(new BlockPos(4, 1, 4));
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        BlubSnoes.zet(p, true);
        helper.assertTrue(BlubMeterkaart.gevuld(BlubMeterkaart.kaart(p)) == 0, "an empty map to begin with");
        tem(helper, p, 47, BlubTrekking.Soort.WEIDE);
        int weide = BlubMeterkaart.rij(p, BlubTrekking.Soort.WEIDE);
        helper.assertTrue(weide == 1 << 4, "a weide Blub of 47 fills the box 40-49: " + weide);
        tem(helper, p, 0, BlubTrekking.Soort.BLAUW);
        tem(helper, p, 100, BlubTrekking.Soort.BLAUW);
        int blauw = BlubMeterkaart.rij(p, BlubTrekking.Soort.BLAUW);
        helper.assertTrue(blauw == (1 | 1 << 9 | 1 << BlubMeterkaart.PUUR | 1 << BlubMeterkaart.VOL),
                "0 and 100: the first and last box and both golden ones: " + blauw);
        helper.assertTrue(BlubMeterkaart.gevuld(BlubMeterkaart.kaart(p)) == 5 && BlubMeterkaart.VAKJES == 60, "5 of the 60 boxes");
        helper.assertTrue(BlubMeterkaart.bits(95) == 1 << 9 && BlubMeterkaart.bits(9) == 1, "the tenths");
        helper.succeed();
    }

    private static void tem(GameTestHelper helper, ServerPlayer p, int meter, BlubTrekking.Soort soort) {
        BlubGuhEntity b = helper.spawn(BlubFeature.BLUBGUH.get(), new BlockPos(5, 1, 5));
        BlubTrekking.Waarden w = b.waarden();
        b.zetWaarden(new BlubTrekking.Waarden(meter, soort, w.tint(), w.grootte()));
        for (int i = 0; i < 80 && !b.isTame(); i++) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
            b.mobInteract(p, InteractionHand.MAIN_HAND);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(b.isTame(), "tamed (meter " + meter + ")");
        b.discard();
    }
}
