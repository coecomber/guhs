package nl.juiced.guhs.feature.techbeurs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.bio.dieren.WolkenschaapjeEntity;
import nl.juiced.guhs.feature.kaaskas.KaaskasBlockEntity;
import nl.juiced.guhs.feature.techsaus.KnuffelsteenGeneratorBlockEntity;
import nl.juiced.guhs.feature.vadsbasis.KnabbelketelBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.8.0: the renewed Guhtechniek-beurs ({@link BeursUitbouw}). The hall's caretaker finds how its hall stands, stamps the
 * annex, the Fabriekshal and the Koepel once (its version), protects them and builds the half Kaasraket; and then a few
 * stands per floor really run: the Knuffelsteen-generator makes knuffelsteen (annex), the Knabbelketels burn, the Kaaskas
 * gets its sauce, the Scheerstal shears a Wolkenschaapje (Fabriekshal), the Mega-knuffelcentrale has its 16 guhs and the
 * Komeetkern's net runs (Koepel); every machine of the list has vadskracht.
 */
public class BeursUitbouwGameTests {
    private static final String HAL = "techbeurs/guhtechniek_beurs";
    private static final Set<String> MACHINES = Set.of("hapluikje", "knuffelsteen_generator", "bouwblokken_fabriek", "spinmachine",
            "verfbad", "knuffelrecycler", "scheerstal", "kleurmolen", "regenboogpers", "kaaskas", "oogster", "guhterminal",
            "bestelbalie", "mega_oven", "knabbel_weerstation", "draaimolen", "wolkenvanger", "fossiel_broedmachine", "sterrenkoepel");

    static List<BlockEntity> in(ServerLevel level, BoundingBox doos) {
        List<BlockEntity> out = new ArrayList<>();
        for (int cx = doos.minX() >> 4; cx <= doos.maxX() >> 4; cx++) {
            for (int cz = doos.minZ() >> 4; cz <= doos.maxZ() >> 4; cz++) {
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (doos.isInside(be.getBlockPos())) {
                        out.add(be);
                    }
                }
            }
        }
        return out;
    }

    @GuhTest(template = HAL, batch = "techbeurs_uitbouw", timeoutTicks = 1600)
    public static void beursUitbouwDraait(GameTestHelper helper) {
        draait(helper);
    }

    /** The same with the hall turned (the jigsaw turns it too): the floors and the annex turn with it. */
    @GuhTest(template = HAL, batch = "techbeurs_uitbouw_gedraaid", timeoutTicks = 1600, rotationSteps = 1)
    public static void beursUitbouwGedraaid(GameTestHelper helper) {
        draait(helper);
    }

    private static void draait(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<BeursBeheer.Kern> beheer = BeursGameTests.alle(helper, BeursBeheer.Kern.class);
        helper.assertTrue(beheer.size() == 1, "one caretaker");
        BeursBeheer.Kern k = beheer.get(0);
        BeursUitbouw.Hal hal = BeursUitbouw.vind(level, k.getBlockPos());
        helper.assertTrue(hal != null, "the caretaker finds its hall");
        helper.assertTrue(k.uitbouw(level), "it stamps the annex and the floors");
        helper.assertTrue(k.uitbouwVersie() == BeursUitbouw.VERSIE && !k.uitbouw(level), "once: then it has the version");
        // the shape: the doorway in the hall's east wall, the two Gloeisterkernen up there, the half rocket, the lifts
        helper.assertTrue(level.getBlockState(hal.wereld(new BlockPos(73, 2, 20))).isAir(), "the doorway into the annex");
        for (BlockPos kern : List.of(new BlockPos(66, 12, 4), new BlockPos(36, 22, 37))) {
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(level.getBlockState(hal.wereld(kern)).getBlock()).getPath().equals("gloeisterkern"),
                    "a Gloeisterkern at " + kern);
        }
        BlockPos raket = hal.wereld(BeursUitbouw.RAKET);
        helper.assertTrue(!level.getBlockState(raket.offset(0, 2, 2)).isAir() && !level.getBlockState(raket.offset(0, 1, 0)).isAir(),
                "romp + motor of the Kaasraket stand on its platform");
        helper.assertTrue(BuiltInRegistries.BLOCK.getKey(level.getBlockState(hal.wereld(new BlockPos(7, 13, 20))).getBlock()).getPath().equals("wolkenstroom"),
                "the wolkenliftje of the Fabriekshal blows");
        // protected like the hall
        helper.assertTrue(Beurs.STRUCTUUR.equals(Bescherming.structuurBij(level, hal.wereld(new BlockPos(30, 23, 20))))
                && Beurs.STRUCTUUR.equals(Bescherming.structuurBij(level, hal.wereld(new BlockPos(90, 3, 20)))), "the new parts are protected");
        BoundingBox aanbouw = hal.doos(BeursUitbouw.AANBOUW), boven = hal.doos(BeursUitbouw.BOVEN);
        AABB alles = AABB.of(aanbouw).minmax(AABB.of(boven));
        // (the annex sticks out of the test's own chunks: keep its chunks ticking, as a player standing there would)
        List<long[]> geforceerd = new ArrayList<>();
        for (BoundingBox doos : List.of(aanbouw, boven)) {
            for (int cx = doos.minX() >> 4; cx <= doos.maxX() >> 4; cx++) {
                for (int cz = doos.minZ() >> 4; cz <= doos.maxZ() >> 4; cz++) {
                    if (level.setChunkForced(cx, cz, true)) {
                        geforceerd.add(new long[] {cx, cz});
                    }
                }
            }
        }
        helper.succeedWhen(() -> {
            List<BlockEntity> bes = new ArrayList<>(in(level, aanbouw));
            bes.addAll(in(level, boven));
            int machines = 0;
            for (BlockEntity be : bes) {
                String id = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getPath();
                if (MACHINES.contains(id)) {
                    machines++;
                    helper.assertTrue(VadsKracht.heeftKracht(level, be.getBlockPos()), id + " at " + be.getBlockPos().toShortString() + " has vadskracht");
                }
                if (be instanceof KnuffelsteenGeneratorBlockEntity gen) {
                    helper.assertTrue(gen.gemaakt() > 0, "the Knuffelsteen-generator made knuffelsteen (saus " + gen.saus().getAmountAsLong(0)
                            + ", vadskracht " + VadsKracht.heeftKracht(level, gen.getBlockPos()) + ", looked after " + k.gemerkt().contains(gen.getBlockPos())
                            + ", " + VadsKracht.regels(level, gen.getBlockPos()) + ")");
                }
                if (be instanceof KnabbelketelBlockEntity ketel) {
                    helper.assertTrue(ketel.brandt(), "the Knabbelketel at " + be.getBlockPos().toShortString() + " burns");
                }
                if (be instanceof KaaskasBlockEntity kas) {
                    helper.assertTrue(kas.saus().getAmountAsLong(0) > 0, "the Kaaskas has kaassaus");
                }
                if (BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getPath().equals("komeetkern")) {
                    helper.assertTrue(VadsKracht.net(level, be.getBlockPos()).draait(), "the Komeetkern's net runs");
                }
            }
            helper.assertTrue(machines >= MACHINES.size(), "every machine of the list is there: " + machines);
            helper.assertTrue(level.getEntitiesOfClass(WolkenschaapjeEntity.class, alles, WolkenschaapjeEntity::isGeschoren).size() > 0,
                    "the Scheerstal sheared a Wolkenschaapje");
            long zitten = level.getEntitiesOfClass(GuhEntity.class, AABB.of(boven), g -> g.isTame() && g.isOrderedToSit()).size();
            helper.assertTrue(zitten >= 16, "the 16 guhs of the Mega-knuffelcentrale sit: " + zitten);
            for (long[] c : geforceerd) {
                level.setChunkForced((int) c[0], (int) c[1], false);
            }
        });
        Bescherming.wisDozen(level, Beurs.STRUCTUUR);
    }
}
