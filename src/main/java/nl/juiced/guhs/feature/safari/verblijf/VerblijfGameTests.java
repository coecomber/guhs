package nl.juiced.guhs.feature.safari.verblijf;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Op Safari! step 2 (S5): a board makes exactly one family of 4 (1 male, 1 female, 2 little ones), they never escape their
 * home, buying is dex + coins + a 7-day cooldown, and breaking a board never leaves (or doubles) a family.
 */
public class VerblijfGameTests {
    private static final String BATCH = "safari_verblijf";
    private static final String VLOER = "px_test_16";
    private static final BlockPos BORD = new BlockPos(8, 1, 8);

    private static VerblijfsbordBlockEntity bord(GameTestHelper helper, String soort) {
        return bord(helper, BORD, soort);
    }

    private static VerblijfsbordBlockEntity bord(GameTestHelper helper, BlockPos plek, String soort) {
        helper.setBlock(plek, VerblijfFeature.BORD.get().defaultBlockState());
        VerblijfsbordBlockEntity be = (VerblijfsbordBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(plek));
        helper.assertTrue(be != null, "the board has its block entity");
        be.plaats(helper.getLevel(), null, soort);
        be.zetStraal(helper.getLevel(), VerblijfsbordBlockEntity.MIN_STRAAL);
        return be;
    }

    /** The animals of the board in this test (tests run side by side: other tests' families are near). */
    private static List<ThuisDier> familie(GameTestHelper helper) {
        return familie(helper, BORD);
    }

    private static List<ThuisDier> familie(GameTestHelper helper, BlockPos plek) {
        BlockPos m = helper.absolutePos(plek);
        return helper.getLevel().getEntitiesOfClass(ThuisDier.class, new AABB(m).inflate(40),
                d -> !d.isRemoved() && d.bord().equals(m));
    }

    /** Exactly 4: a male, a female and two little ones, all belonging to this board. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 40)
    public static void familieVanVier(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "leeuw");
        helper.runAfterDelay(5, () -> {
            List<ThuisDier> f = familie(helper);
            helper.assertTrue(f.size() == 4, "4 animals (" + f.size() + ")");
            helper.assertTrue(f.stream().filter(d -> d.variant().equals("man")).count() == 1, "1 male");
            helper.assertTrue(f.stream().filter(d -> d.variant().equals("vrouw")).count() == 1, "1 female");
            helper.assertTrue(f.stream().filter(d -> d.variant().equals("baby")).count() == 2, "2 little ones");
            helper.assertTrue(f.stream().allMatch(d -> be.bordId().equals(d.bordId()) && be.familie(d.getUUID())), "all of this board");
            helper.succeed();
        });
    }

    /** Thrown far out of the board's home: put straight back (also with a rectangular home). */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void ontsnaptNooit(GameTestHelper helper) {
        BlockPos plek = new BlockPos(1, 1, 8);              // (at the side: far away is still inside the ticking test area)
        VerblijfsbordBlockEntity be = bord(helper, plek, "zebra");
        be.zetVorm(helper.getLevel(), true);
        be.zetBreedte(helper.getLevel(), 4);
        be.zetDiepte(helper.getLevel(), 4);
        helper.runAfterDelay(5, () -> {
            ThuisDier d = familie(helper, plek).get(0);
            BlockPos ver = helper.absolutePos(plek.offset(13, 4, 0));
            d.teleportTo(ver.getX() + 0.5, ver.getY(), ver.getZ() + 0.5);
            d.setDeltaMovement(new Vec3(0.8, 0.6, 0));
            helper.succeedWhen(() -> {
                Vec3 m = Vec3.atBottomCenterOf(helper.absolutePos(plek));
                helper.assertTrue(!d.isRemoved() && Math.abs(d.getX() - m.x) <= 4 + 2 && Math.abs(d.getZ() - m.z) <= 4 + 2,
                        "the zebra is back inside its rectangle");
            });
        });
    }

    /** No photo: no board; photo + coins: bought (coins paid, cooldown on); again within 7 days: no. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 20)
    public static void kopenEnCooldown(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        var server = helper.getLevel().getServer();
        SafariSpeler.wis(server, p.getUUID());
        helper.assertTrue(VerblijfWinkel.kanKopen(p, "leeuw") == VerblijfWinkel.Reden.GEEN_FOTO, "first the photo");
        SafariSpeler.registreerFoto(server, p.getUUID(), "leeuw", 2, new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(VerblijfWinkel.kanKopen(p, "leeuw") == VerblijfWinkel.Reden.MUNTEN, "then the coins");
        SafariMunten.zet(server, p.getUUID(), 500);
        int prijs = VerblijfWinkel.prijs("leeuw");
        helper.assertTrue(prijs == 175, "a lion is middel: 175 (" + prijs + ")");
        helper.assertTrue(VerblijfWinkel.koop(p, "leeuw"), "bought");
        helper.assertTrue(SafariMunten.saldo(server, p.getUUID()) == 500 - prijs, "paid");
        helper.assertTrue(p.getInventory().contains(s -> s.is(VerblijfFeature.BORD_ITEM.get()) && "leeuw".equals(VerblijfsbordItem.soort(s))),
                "the lion's board is in the inventory");
        helper.assertTrue(VerblijfWinkel.kanKopen(p, "leeuw") == VerblijfWinkel.Reden.COOLDOWN, "not again for 7 days");
        helper.assertTrue(VerblijfWinkel.kanKopen(p, "gouden_dikdik") == VerblijfWinkel.Reden.GEEN_BORD, "the gouden dikdik is never sold");
        SafariSpeler.wis(server, p.getUUID());
        helper.succeed();
    }

    /** Broken: the family goes back to the park; placed again: one new family, never two. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void brekenGeenDubbel(GameTestHelper helper) {
        bord(helper, "pinguin");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(familie(helper).size() == 4, "a family of 4");
            ServerLevel level = helper.getLevel();
            level.destroyBlock(helper.absolutePos(BORD), false);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(familie(helper).isEmpty(), "broken: back to the park (" + familie(helper).size() + " left)");
                bord(helper, "pinguin");
                helper.runAfterDelay(50, () -> {
                    helper.assertTrue(familie(helper).size() == 4, "placed again: 4, not 8 (" + familie(helper).size() + ")");
                    helper.succeed();
                });
            });
        });
    }
}
