package nl.juiced.guhs.feature.safari.verblijf;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.bouwhulp.helpers.Bouwopdracht;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Test round 5 (FX-C): hiding ends by itself and "Terugroepen" ends it at once (they stayed invisible before), food no longer
 * counts for happiness, the sleeping spot is pointed at with a click inside the home, and "Bouw passend verblijf" is a
 * proposal that waits for "Goedgekeurd" (or goes with "Annuleren").
 */
public class VerblijfFxGameTests {
    private static final String BATCH = "safari_verblijf_fx";
    private static final String VLOER = "px_test_16";
    private static final BlockPos BORD = new BlockPos(8, 1, 8);

    private static VerblijfsbordBlockEntity bord(GameTestHelper helper, String soort) {
        helper.setBlock(BORD, VerblijfFeature.BORD.get().defaultBlockState());
        VerblijfsbordBlockEntity be = (VerblijfsbordBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BORD));
        helper.assertTrue(be != null, "the board has its block entity");
        be.plaats(helper.getLevel(), null, soort);
        be.zetStraal(helper.getLevel(), VerblijfsbordBlockEntity.MIN_STRAAL);
        return be;
    }

    private static List<ThuisDier> familie(GameTestHelper helper) {
        BlockPos m = helper.absolutePos(BORD);
        return helper.getLevel().getEntitiesOfClass(ThuisDier.class, new AABB(m).inflate(40), d -> !d.isRemoved() && d.bord().equals(m));
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 120)
    public static void verblijfVerstoppenEindigtEnTerugroepenToontZe(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "leeuw");
        helper.runAfterDelay(5, () -> {
            List<ThuisDier> f = familie(helper);
            helper.assertTrue(f.size() == 4, "a family of 4");
            ThuisDier kort = f.get(0);
            kort.verstop(helper.getLevel(), 10);
            helper.assertTrue(kort.isInvisible() && kort.weg(), "hidden");
            be.verstoppen(helper.getLevel());
            helper.assertTrue(f.stream().allMatch(ThuisDier::isInvisible), "all hidden");
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(!kort.isInvisible() && !kort.weg(), "a short hiding ends by itself (it was never counted down)");
                be.terugroepen(helper.getLevel());
                helper.assertTrue(f.stream().noneMatch(d -> d.isInvisible() || d.weg()), "Terugroepen: everybody visible and home again");
                helper.succeed();
            });
        });
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 40)
    public static void verblijfEtenTeltNietMeePetWel(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "leeuw");
        be.herreken(helper.getLevel());
        int voor = be.score();
        helper.assertTrue(!be.uitslag().contains("Gevoerd"), "no 'fed today' line any more");
        be.gevoerd();
        be.herreken(helper.getLevel());
        helper.assertTrue(be.score() == voor, "feeding changes nothing: " + voor + " -> " + be.score());
        be.verzorgd();
        be.herreken(helper.getLevel());
        helper.assertTrue(be.score() == Math.min(100, voor + 10), "petting gives 10: " + voor + " -> " + be.score());
        helper.succeed();
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 40)
    public static void verblijfSlaapplekAanwijzen(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "leeuw");
        ServerPlayer p = GuhMockPlayer.of(helper);
        SlaapplekAanwijzen.begin(p, be);
        helper.assertTrue(SlaapplekAanwijzen.wacht(p), "pointing");
        // outside the home (radius 8): no
        BlockPos ver = helper.absolutePos(BORD.offset(10, -1, 0));
        klik(p, ver);
        helper.assertTrue(be.slaapplek() == null && SlaapplekAanwijzen.wacht(p), "outside: not set, still pointing");
        BlockPos grond = helper.absolutePos(BORD.offset(3, -1, 2));
        klik(p, grond);
        helper.assertTrue(grond.above().equals(be.slaapplek()), "the block's top is the sleeping spot: " + be.slaapplek());
        helper.assertTrue(!SlaapplekAanwijzen.wacht(p), "done pointing");
        helper.succeed();
    }

    private static void klik(ServerPlayer p, BlockPos pos) {
        PlayerInteractEvent.RightClickBlock e = new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        SlaapplekAanwijzen.klik(e);
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 60)
    public static void verblijfPassendIsEerstEenVoorstel(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "leeuw");
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.CREATIVE);
        Bouwopdracht.testZoekStraal = 8;
        try {
            helper.assertTrue(Bouwopdracht.verblijf(p, helper.getLevel(), be), "a proposal: " + Bouwopdracht.laatsteNee);
            helper.assertTrue(Bouwopdracht.voorstel(helper.getLevel(), be), "it waits for the OK");
            Spook s = Bouwopdracht.spook(helper.getLevel(), be);
            helper.assertTrue(s != null && s.status() == Spook.Status.SPOOK, "only a ghost, nothing builds");
            Bouwopdracht.verblijfAnnuleren(p, helper.getLevel(), be);
            helper.assertTrue(Bouwopdracht.spook(helper.getLevel(), be) == null && !Bouwopdracht.bezig(helper.getLevel(), be), "cancelled: gone");
            helper.assertTrue(Bouwopdracht.verblijf(p, helper.getLevel(), be), "a new proposal");
            Bouwopdracht.verblijfGoedgekeurd(p, helper.getLevel(), be);
            Spook t = Bouwopdracht.spook(helper.getLevel(), be);
            helper.assertTrue(t != null && t.status() == Spook.Status.BOUWT, "approved: the family builds");
            helper.assertTrue(!Bouwopdracht.voorstel(helper.getLevel(), be), "no longer a proposal");
            Bouwopdracht.stop(helper.getLevel(), be);
        } finally {
            Bouwopdracht.testZoekStraal = -1;
        }
        helper.succeed();
    }

    /** Round 6: the proposal shows what you still miss (names + counts) before you approve it, and gives it as a book. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 60)
    public static void verblijfVoorstelToontTekort(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "leeuw");
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        Bouwopdracht.testZoekStraal = 8;
        try {
            helper.assertTrue(Bouwopdracht.verblijf(p, helper.getLevel(), be), "a proposal: " + Bouwopdracht.laatsteNee);
            net.minecraft.nbt.ListTag tekort = Bouwopdracht.staat(p, helper.getLevel(), be).getListOrEmpty("Tekort");
            helper.assertTrue(!tekort.isEmpty() && tekort.getCompoundOrEmpty(0).getIntOr("N", 0) > 0
                    && !tekort.getCompoundOrEmpty(0).getStringOr("Id", "").isEmpty(), "the shortage is in the menu: " + tekort);
            Bouwopdracht.verblijfLijstje(p, helper.getLevel(), be);
            helper.assertTrue(p.getInventory().contains(s -> s.is(net.minecraft.world.item.Items.WRITTEN_BOOK)), "and as a booklet");
            Bouwopdracht.verblijfAnnuleren(p, helper.getLevel(), be);
            helper.assertTrue(!Bouwopdracht.staat(p, helper.getLevel(), be).contains("Tekort"), "no proposal, no list");
        } finally {
            Bouwopdracht.testZoekStraal = -1;
        }
        helper.succeed();
    }
}
