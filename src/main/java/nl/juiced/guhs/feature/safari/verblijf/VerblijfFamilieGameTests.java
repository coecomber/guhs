package nl.juiced.guhs.feature.safari.verblijf;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.6.4: the verblijfsbord's family dialog: the standard family until something is chosen, a chosen family is what comes,
 * changing it swaps exactly the members that change (never more than four, names stay), at least one parent, and the
 * male/female choice only for a species whose male and female look different (1.7.4: for every species, two of the same too).
 */
public class VerblijfFamilieGameTests {
    private static final String BATCH = "safari_verblijf_familie";
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

    private static long tel(List<ThuisDier> f, String rol) {
        return f.stream().filter(d -> d.variant().equals(rol)).count();
    }

    /** Placed: the standard family (a male, a female, two little ones), not chosen yet. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 40)
    public static void standaardFamilie(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "leeuw");
        helper.assertTrue(!be.familieGekozen(), "not chosen yet: the dialog opens");
        for (int i = 0; i < VerblijfsbordBlockEntity.FAMILIE.length; i++) {
            helper.assertTrue(be.rol(i).equals(VerblijfsbordBlockEntity.FAMILIE[i]), "member " + i + " is " + VerblijfsbordBlockEntity.FAMILIE[i]);
        }
        helper.runAfterDelay(5, () -> {
            List<ThuisDier> f = familie(helper);
            helper.assertTrue(f.size() == 4 && tel(f, "man") == 1 && tel(f, "vrouw") == 1 && tel(f, "baby") == 2, "1 male, 1 female, 2 little ones");
            helper.succeed();
        });
    }

    /** Chosen: two lionesses, a lion and one cub; only the changed members are swapped, names stay, still four. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 60)
    public static void eigenFamilieWisseltZonderDubbelen(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "leeuw");
        be.hernoem(helper.getLevel(), 2, "Simba");
        helper.runAfterDelay(5, () -> {
            UUID man = be.lid(0), vrouw = be.lid(1), welp = be.lid(2), tweede = be.lid(3);
            helper.assertTrue(be.zetFamilie(helper.getLevel(), new String[] {"man", "vrouw", "vrouw", "baby"}), "a valid family");
            helper.assertTrue(be.familieGekozen(), "chosen");
            helper.assertTrue(man.equals(be.lid(0)) && vrouw.equals(be.lid(1)) && tweede.equals(be.lid(3)), "the unchanged members stay the same animals");
            helper.assertTrue(!welp.equals(be.lid(2)) && helper.getLevel().getEntity(welp) == null, "the changed member is swapped: the old one is gone");
            helper.runAfterDelay(5, () -> {
                List<ThuisDier> f = familie(helper);
                helper.assertTrue(f.size() == 4, "still four, never more (" + f.size() + ")");
                helper.assertTrue(tel(f, "man") == 1 && tel(f, "vrouw") == 2 && tel(f, "baby") == 1, "a lion, two lionesses, one cub");
                helper.assertTrue(be.naam(2).equals("Simba") && helper.getLevel().getEntity(be.lid(2)) instanceof ThuisDier d
                        && d.getCustomName() != null && d.getCustomName().getString().equals("Simba"), "the name stays");
                helper.assertTrue(be.zetFamilie(helper.getLevel(), new String[] {"man", "vrouw", "vrouw", "baby"}) && familie(helper).size() == 4,
                        "the same choice again changes nothing");
                helper.succeed();
            });
        });
    }

    /** No parent at all, or nonsense: refused, nothing changes. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 40)
    public static void minstensEenOuder(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "zebra");
        helper.runAfterDelay(5, () -> {
            UUID eerste = be.lid(0);
            helper.assertTrue(!be.zetFamilie(helper.getLevel(), new String[] {"baby", "baby", "baby", "baby"}), "four little ones alone: no");
            helper.assertTrue(!be.zetFamilie(helper.getLevel(), new String[] {"man", "draak", "baby", "baby"}), "nonsense: no");
            helper.assertTrue(!be.zetFamilie(helper.getLevel(), new String[] {"man", "vrouw"}), "too few: no");
            helper.assertTrue(eerste.equals(be.lid(0)) && be.rol(0).equals("man") && !be.familieGekozen(), "nothing changed");
            helper.succeed();
        });
    }

    /** The male/female choice only where they look different; a same-looking species' parents keep a female (the egg). */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 40)
    public static void geslachtAlleenBijVerschil(GameTestHelper helper) {
        for (String s : List.of("leeuw", "olifant", "giraffe", "nijlpaard")) {
            helper.assertTrue(VerblijfsbordBlockEntity.heeftGeslacht(s), s + ": male and female look different");
        }
        for (String s : List.of("zebra", "struisvogel", "pinguin", "stokstaartje")) {
            helper.assertTrue(!VerblijfsbordBlockEntity.heeftGeslacht(s), s + ": male and female look the same");
        }
        VerblijfsbordBlockEntity be = bord(helper, "struisvogel");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(be.zetFamilie(helper.getLevel(), new String[] {"baby", "baby", "ouder", "baby"}), "one parent, three chicks");
            helper.assertTrue(be.rol(2).equals("vrouw"), "the one new parent is a female (she lays the egg): " + be.rol(2));
            helper.assertTrue(be.zetFamilie(helper.getLevel(), new String[] {"ouder", "baby", "ouder", "baby"}), "two parents");
            helper.assertTrue(be.rol(0).equals("man") && be.rol(2).equals("vrouw"), "then a male joins: " + be.rol(0) + "/" + be.rol(2));
            helper.runAfterDelay(5, () -> {
                List<ThuisDier> f = familie(helper);
                helper.assertTrue(f.size() == 4 && tel(f, "baby") == 2, "four, two chicks (" + f.size() + ")");
                helper.succeed();
            });
        });
    }

    /** 1.7.4: the male/female choice for every species, also two females or two males. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 60)
    public static void geslachtKiezenTweeHetzelfde(GameTestHelper helper) {
        VerblijfsbordBlockEntity be = bord(helper, "zebra");   // (male and female look the same)
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(be.zetFamilie(helper.getLevel(), new String[] {"vrouw", "vrouw", "baby", "baby"}), "two females");
            helper.assertTrue(be.rol(0).equals("vrouw") && be.rol(1).equals("vrouw"), "two females: " + be.rol(0) + "/" + be.rol(1));
            helper.assertTrue(be.zetFamilie(helper.getLevel(), new String[] {"man", "man", "baby", "baby"}), "two males");
            helper.assertTrue(be.rol(0).equals("man") && be.rol(1).equals("man"), "two males: " + be.rol(0) + "/" + be.rol(1));
            helper.runAfterDelay(5, () -> {
                List<ThuisDier> f = familie(helper);
                helper.assertTrue(f.size() == 4 && tel(f, "man") == 2 && tel(f, "baby") == 2, "two males, two foals (" + f.size() + ")");
                helper.succeed();
            });
        });
    }
}
