package nl.juiced.guhs.feature.onderwol.bouw;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the underground buildings part 2 ({@link OnderWolStructuren2}, tools/features/onderwol_structuren2.py):
 * the eight structures, their templates and anchors, the Mijnguhdorp's side buildings, the texts and the towel turban.
 * Template {@code onderwol_bouw_test} (tools/features/onderwol_structuren1.py).
 */
public class OnderWolStructuren2GameTests {
    private static final String VELD = "onderwol_bouw_test";
    private static final String BATCH = "onderwol_structuren2";

    private static ResourceKey<Structure> structuur(String naam) {
        return ResourceKey.create(Registries.STRUCTURE, Guhs.id(naam));
    }

    /** Every structure is a guhs:grot_bouw with a sane band (above the saus sea), a reach, biomes, and an "af en toe" spread. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolStructuren2Bestaan(GameTestHelper helper) {
        var structuren = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (String naam : OnderWolStructuren2.STRUCTUREN) {
            Optional<Structure> s = structuren.getOptional(structuur(naam));
            helper.assertTrue(s.isPresent() && s.get() instanceof GrotBouwStructure, naam + ": a guhs:grot_bouw");
            GrotBouwStructure g = (GrotBouwStructure) s.get();
            helper.assertTrue(g.bandOnder() > GrotBouwStructure.SAUS_TOT && g.maxY() >= g.bandOnder(), naam + ": its band lies above the saus sea");
            helper.assertTrue(g.keepClear() > 0, naam + ": it claims its room (keep_clear)");
            helper.assertTrue(g.biomes().size() > 5, naam + ": it can stand under the Guhmension's biomes");
            Optional<StructureSet> set = sets.getOptional(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id(naam)));
            helper.assertTrue(set.isPresent() && set.get().placement() instanceof RandomSpreadStructurePlacement r && r.spacing() >= 30,
                    naam + ": af en toe (a random spread, spacing 30+)");
        }
        helper.succeed();
    }

    /** Every start template has its anchor; the side buildings have the connector for the Mijnguhdorp's tunnels. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolStructuren2Ankers(GameTestHelper helper) {
        var pools = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var manager = helper.getLevel().getStructureManager();
        for (String naam : OnderWolStructuren2.STRUCTUREN) {
            StructureTemplatePool pool = pools.getOptional(ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id("onderwol/" + naam + "/start"))).orElse(null);
            helper.assertTrue(pool != null, naam + ": its start pool");
            for (StructurePoolElement e : pool.getShuffledTemplates(RandomSource.create(1))) {
                var jigsaws = e.getShuffledJigsawBlocks(manager, BlockPos.ZERO, Rotation.NONE, RandomSource.create(1));
                helper.assertTrue(jigsaws.stream().anyMatch(j -> j.name().equals(Guhs.id(naam + "_midden"))), naam + ": the anchor guhs:" + naam + "_midden");
                if (OnderWolStructuren2.DORP_ZIJ.contains(naam)) {
                    helper.assertTrue(jigsaws.stream().anyMatch(j -> j.name().equals(Guhs.id("mijnguhdorp_zij_in"))), naam + ": the connector guhs:mijnguhdorp_zij_in");
                }
            }
        }
        StructureTemplatePool zij = pools.getOptional(ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id("onderwol/mijnguhdorp/zij"))).orElseThrow();
        String elementen = zij.getShuffledTemplates(RandomSource.create(1)).toString();
        for (String naam : OnderWolStructuren2.DORP_ZIJ) {
            helper.assertTrue(elementen.contains("guhs:onderwol/" + naam + "/start"), naam + ": a side building of the Mijnguhdorp");
        }
        helper.succeed();
    }

    /** The talking guhs have their lines, every structure its Superkompas name, the turban its name and source. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolStructuren2Teksten(GameTestHelper helper) {
        try (InputStream in = OnderWolStructuren2GameTests.class.getResourceAsStream("/assets/guhs/lang/nl_nl.json")) {
            JsonObject nl = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            OnderWolStructuren2.PRAAT.forEach((plek, p) -> {
                for (int i = 1; i <= p.regels(); i++) {
                    helper.assertTrue(nl.has("quest.guhs.onderwol_bouw." + plek + "." + i), plek + " line " + i);
                }
            });
            for (String s : OnderWolStructuren2.STRUCTUREN) {
                helper.assertTrue(nl.has("structure.guhs." + s) && nl.has("structure.guhs." + s + ".tooltip"), s + ": its Superkompas texts");
            }
            helper.assertTrue(nl.has("item.guhs.handdoek_tulband") && nl.has("gui.guhs.kledingbron." + OnderWolStructuren2.KLEDINGBRON)
                    && nl.has("gui.guhs.kleding.prijs.hint.in_badhuis_de_warme_knabbel"), "the turban's texts");
            helper.assertTrue(nl.has("sign.guhs.onderwol_bouw.badhuis.knabbelbadjes"), "the signs (the little baths)");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.assertTrue(GuhClothes.HANDDOEK_TULBAND.slot == GuhClothes.Slot.HEAD && ModItems.clothingItem(GuhClothes.HANDDOEK_TULBAND) != null,
                "the towel turban: a hat with an item");
        helper.assertTrue(OnderWolStructuren2.KLEDINGBRON.equals(KledingBronnen.bron(GuhClothes.HANDDOEK_TULBAND)), "the turban comes from the bathhouse");
        helper.succeed();
    }

    /**
     * 1.7.0 playtest: the Kaasmakerij's pomphuis really works in the placed template: both Guhraden run, the pumps (on the
     * saus lake) and the Guhoven have vadskracht, the Guhdraad glows and its lamps burn, the vats fill and the oven grills.
     */
    @GuhTest(template = "onderwol/kaasmakerij/start", batch = BATCH, timeoutTicks = 300)
    public static void kaasmakerijPomphuisWerkt(GameTestHelper helper) {
        pomphuis(helper);
    }

    /** The same, placed turned half around (the jigsaw placement turns buildings). */
    @GuhTest(template = "onderwol/kaasmakerij/start", batch = BATCH, timeoutTicks = 300, rotationSteps = 2)
    public static void kaasmakerijPomphuisWerktGedraaid(GameTestHelper helper) {
        pomphuis(helper);
    }

    private static void pomphuis(GameTestHelper helper) {
        var level = helper.getLevel();
        KrachtProef.draait(helper, 2, 3, 9, g -> {
            helper.assertTrue(g.vaten().size() == 8, "eight Sausvaten");
            helper.assertTrue(g.vaten().stream().anyMatch(v -> level.getBlockEntity(v) instanceof nl.juiced.guhs.feature.techsaus.SausvatBlockEntity vat
                    && vat.tank().inhoud() > 0), "the pumps fill the Sausvaten");
            helper.assertTrue(g.ovens().stream().allMatch(o -> level.getBlockState(o).getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)),
                    "the Guhoven grills");
        });
    }
}
