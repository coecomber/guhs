package nl.juiced.guhs.feature.safari;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.feature.safari.quests.Beloningen;
import nl.juiced.guhs.feature.safari.verblijf.ThuisDier;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfData;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfFeature;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordBlockEntity;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Round 4 (L4): the Kriebelhuis animals. The dex counts its chapter with groups as one species (the sea urchins with the
 * starfish, the anemone not at all); a terrariumbord makes a glass tank with its creatures staying put inside (a group
 * all four); the Nachtzaal turns day and night round in the park only; indoors an animal stands on the floor, not on the
 * roof; a guhs critter of a park home is put back when it strays.
 */
public class KriebelhuisGameTests {
    private static final String BATCH = "safari_kriebelhuis";
    private static final String VLOER = "px_test_16";

    /** Every Kriebelhuis species has a section; the dex counts groups once and the anemone not; the full dex is ~79. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 20)
    public static void dexHoofdstuk(GameTestHelper helper) {
        ParkData d = ParkData.get();
        List<ParkData.Soort> kriebel = d.soorten.values().stream().filter(s -> ParkData.KRIEBELHUIS.equals(s.zone())).toList();
        helper.assertTrue(kriebel.size() == 35, "35 Kriebelhuis species incl. the anemone (" + kriebel.size() + ")");
        for (ParkData.Soort s : kriebel) {
            helper.assertTrue(ParkData.KRIEBEL_SECTIES.contains(s.sectie()), s.naam() + " has a section (" + s.sectie() + ")");
        }
        helper.assertTrue(SafariSpeler.telt("zeester") && !SafariSpeler.telt("zee_egel"), "the sea urchins count with the starfish");
        helper.assertTrue(d.dexSoort("zee_egel").equals("zeester") && d.dexSoort("kwal").equals("kwal"), "a group's dex species");
        helper.assertTrue(!SafariSpeler.telt("anemoon"), "the anemone is decoration");
        helper.assertTrue(SafariSpeler.telt("pelikaan") && "delta".equals(d.soort("pelikaan").zone()), "the pelican counts in the delta");
        long inDex = kriebel.stream().filter(s -> SafariSpeler.telt(s.naam())).count();
        helper.assertTrue(inDex == 33, "33 Kriebelhuis dex species (" + inDex + ")");
        int totaal = Beloningen.totaal();
        helper.assertTrue(totaal >= 75 && totaal <= 85, "the full dex is about 79 (" + totaal + ")");
        List<String> h = d.dexHoofdstukken();
        helper.assertTrue(h.get(h.size() - 1).equals(ParkData.KRIEBELHUIS), "the Kriebelhuis is the last chapter");
        helper.assertTrue(d.soort("kameleon").kleuren().size() == 4 && d.soort("gifkikker").kleuren().size() == 3, "colour variants");
        helper.succeed();
    }

    /** Small creatures get a terrariumbord, the bigger ones a normal board; the decoration and group members none. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 20)
    public static void terrariumbordFamilie(GameTestHelper helper) {
        for (String s : List.of("mier", "springspin", "gifkikker", "kwal", "zeepaardje", "lantaarnvisje")) {
            helper.assertTrue(VerblijfData.dier(s) != null && VerblijfData.dier(s).terrarium(), s + ": a terrariumbord");
        }
        for (String s : List.of("fennek", "galago", "reuzenschildpad", "pelikaan", "zeeschildpad")) {
            helper.assertTrue(VerblijfData.dier(s) != null && !VerblijfData.dier(s).terrarium(), s + ": a normal board");
        }
        helper.assertTrue(VerblijfData.dier("anemoon") == null && VerblijfData.dier("zee_egel") == null, "no board for the anemone or a group member");
        helper.assertTrue(VerblijfData.dier("zeeschildpad").waterNodig() > 0, "the sea turtle needs water at home");
        helper.succeed();
    }

    private static List<ThuisDier> inTank(GameTestHelper helper, BlockPos plek) {
        BlockPos m = helper.absolutePos(plek);
        return helper.getLevel().getEntitiesOfClass(ThuisDier.class, new AABB(m).inflate(20), d -> !d.isRemoved() && d.bord().equals(m));
    }

    /** A terrarium: a female and a little one (a group: all four), staying put inside the tank. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void terrariumBlijftStaan(GameTestHelper helper) {
        BlockPos a = new BlockPos(4, 1, 4), b = new BlockPos(11, 1, 11);
        for (Object[] t : new Object[][]{{a, "springspin"}, {b, "mier"}}) {
            BlockPos p = (BlockPos) t[0];
            helper.setBlock(p, VerblijfFeature.TERRARIUM.get().defaultBlockState());
            VerblijfsbordBlockEntity be = (VerblijfsbordBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p));
            helper.assertTrue(be != null && be.terrarium(), "the terrarium has its block entity");
            be.plaats(helper.getLevel(), null, (String) t[1]);
        }
        helper.runAfterDelay(60, () -> {
            List<ThuisDier> spin = inTank(helper, a), mier = inTank(helper, b);
            helper.assertTrue(spin.size() == 2, "a jumping spider and a little one (" + spin.size() + ")");
            helper.assertTrue(mier.size() == 4, "the ant colony: all four (" + mier.size() + ")");
            for (ThuisDier d : spin) {
                Vec3 m = Vec3.atBottomCenterOf(helper.absolutePos(a));
                helper.assertTrue(d.vast() != null && Math.abs(d.getX() - m.x) < 0.5 && Math.abs(d.getZ() - m.z) < 0.5
                        && d.getY() >= m.y && d.getY() < m.y + 1, "it stays inside its tank");
            }
            helper.succeed();
        });
    }

    /** Nachtzaal species: awake by day in the park (day and night turned round there), nocturnal at home. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 20)
    public static void nachtzaalOmgedraaid(GameTestHelper helper) {
        ParkData d = ParkData.get();
        for (String s : List.of("kiwi", "schubdier", "spookdiertje", "plompe_lori", "fruitvleermuis", "galago", "suikereekhoorn", "fennek")) {
            ParkData.Soort info = d.soort(s);
            helper.assertTrue(info != null && info.heeft("nachtzaal"), s + " lives in the Nachtzaal");
            helper.assertTrue(!SafariDier.nachtdier(info, true), s + " is awake by day in the park");
            helper.assertTrue(SafariDier.nachtdier(info, false), s + " is nocturnal at home");
        }
        helper.assertTrue(SafariDier.nachtdier(d.soort("glimworm"), true), "an ordinary night animal stays nocturnal in the park");
        helper.assertTrue(!SafariDier.nachtdier(d.soort("leeuw"), false), "a day animal stays a day animal");
        helper.succeed();
    }

    /** Indoors (a roof over the home) an animal stands on the floor of its home, a swimmer in the water. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 20)
    public static void binnenOpDeVloer(GameTestHelper helper) {
        BlockPos kolom = new BlockPos(5, 1, 5);
        for (int y = 6; y <= 7; y++) {
            helper.setBlock(kolom.atY(y), Blocks.STONE.defaultBlockState());          // the roof
        }
        BlockPos abs = helper.absolutePos(kolom), dak = helper.absolutePos(kolom.atY(6));
        BlockPos land = SafariDier.binnenGrond(helper.getLevel(), abs.getX(), abs.getZ(), abs.getY(), false, false);
        helper.assertTrue(land != null && land.getY() < dak.getY() && helper.getLevel().getBlockState(land).isAir()
                && !helper.getLevel().getBlockState(land.below()).isAir(), "on the floor, not on the roof (" + land + ", roof " + dak + ")");
        BlockPos nat = new BlockPos(10, 1, 10);
        helper.setBlock(nat, Blocks.WATER.defaultBlockState());
        helper.setBlock(nat.above(), Blocks.WATER.defaultBlockState());
        helper.setBlock(nat.atY(7), Blocks.STONE.defaultBlockState());
        BlockPos a = helper.absolutePos(nat);
        BlockPos zwem = SafariDier.binnenGrond(helper.getLevel(), a.getX(), a.getZ(), a.getY(), true, true);
        helper.assertTrue(zwem != null && !helper.getLevel().getFluidState(zwem).isEmpty() && helper.getLevel().getFluidState(zwem.below()).isEmpty(),
                "a bottom dweller on the bottom of the water (" + zwem + ")");
        helper.succeed();
    }

    /** A guhs critter of a park home (the Kriebelhuis frogs) that strays is put back home; never despawns. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 120)
    public static void critterBlijftThuis(GameTestHelper helper) {
        BlockPos thuis = helper.absolutePos(new BlockPos(3, 1, 3));
        int gen = SafariOpslag.get(helper.getLevel().getServer()).generatie();
        helper.assertTrue(Populatie.spawnCritter(helper.getLevel(), "guhs:kikkerguh", "kikkerguh_mint", Vec3.atBottomCenterOf(thuis),
                thuis.getX(), thuis.getY(), thuis.getZ(), 2, gen, 0f), "the kikkerguh is spawned");
        List<Entity> e = helper.getLevel().getEntities((Entity) null, new AABB(thuis).inflate(2),
                x -> x.getPersistentData().contains(Populatie.CRITTER));
        helper.assertTrue(e.size() == 1, "one critter (" + e.size() + ")");
        Entity kikker = e.get(0);
        helper.assertTrue(kikker.isInvulnerable(), "a park critter is never hurt");
        kikker.teleportTo(thuis.getX() + 11.5, thuis.getY(), thuis.getZ() + 10.5);
        helper.succeedWhen(() -> helper.assertTrue(!kikker.isRemoved() && !Populatie.weg(kikker.getX(), kikker.getY(), kikker.getZ(),
                thuis.getX(), thuis.getY(), thuis.getZ(), 2), "the kikkerguh is back home"));
    }
}
