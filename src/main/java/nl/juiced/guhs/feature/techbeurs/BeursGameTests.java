package nl.juiced.guhs.feature.techbeurs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.onderwol.bouw.KrachtProef;
import nl.juiced.guhs.feature.techbezorg.StepstationBlockEntity;
import nl.juiced.guhs.feature.techbron.BronBlockEntity;
import nl.juiced.guhs.feature.techbuis.FilterBlockEntity;
import nl.juiced.guhs.feature.techmachine.KnutselmachineBlockEntity;
import nl.juiced.guhs.feature.techmachine.PlantagebakBlockEntity;
import nl.juiced.guhs.feature.techsaus.BrouwautomaatBlockEntity;
import nl.juiced.guhs.feature.vadskracht.BatterijBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsBron;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.world.GegarandeerdPlacement;

/**
 * The Guhtechniek-beurs (feature/techbeurs/Beurs): its template placed (also turned, as the jigsaw does) really runs: every
 * source gives (the knuffel and disco guhs are seated by the BeursBeheer), every machine, filter and lamp has vadskracht,
 * every Guhdraad glows; the Droomfabriek fries knabbels from its own field; the two Knutselmachines craft; the Bezorgguhtje
 * brings knabbels from the green post to the orange one. Plus: the switches cut, the caretaker finds its marks, the
 * protection lets the beurs' own machines work but visitors only look, and the worldgen data (guaranteed, alleen_nieuw).
 */
public class BeursGameTests {
    private static final String HAL = "techbeurs/guhtechniek_beurs", BATCH = "techbeurs";

    @SuppressWarnings("unchecked")
    static <T> List<T> alle(GameTestHelper helper, Class<T> soort) {
        ServerLevel level = helper.getLevel();
        AABB box = helper.getBounds();
        List<T> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed((int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ),
                (int) Math.ceil(box.maxX) - 1, (int) Math.ceil(box.maxY) - 1, (int) Math.ceil(box.maxZ) - 1)) {
            if (level.getBlockState(p).hasBlockEntity()) {
                BlockEntity be = level.getBlockEntity(p);
                if (soort.isInstance(be)) {
                    out.add((T) be);
                }
            }
        }
        return out;
    }

    static int tel(Container c, Predicate<ItemStack> wat) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (wat.test(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void draait(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<BronBlockEntity> bronnen = alle(helper, BronBlockEntity.class);
        List<FilterBlockEntity> filters = alle(helper, FilterBlockEntity.class);
        List<KnutselmachineBlockEntity> knutsel = alle(helper, KnutselmachineBlockEntity.class);
        helper.assertTrue(bronnen.size() >= 5, "the sources: knuffel, disco, blub, two kernen; found " + bronnen.size());
        helper.assertTrue(filters.size() == 3 && knutsel.size() == 2, "three filters, two Knutselmachines");
        int[] blokken = new int[1];
        KrachtProef.draait(helper, 1, 30, 0, g -> {
            for (BronBlockEntity b : bronnen) {
                VadsNet net = VadsKracht.net(level, b.getBlockPos());
                helper.assertTrue(b instanceof VadsBron bron && bron.vadsAanbod() > 0 && net.draait() && net.teltMee(bron),
                        level.getBlockState(b.getBlockPos()).getBlock() + " at " + b.getBlockPos() + " gives vadskracht (" + net.status() + ")");
            }
            for (FilterBlockEntity f : filters) {
                helper.assertTrue(VadsKracht.heeftKracht(level, f.getBlockPos()), "the Filterstuk at " + f.getBlockPos() + " has vadskracht");
            }
            for (BatterijBlockEntity bat : alle(helper, BatterijBlockEntity.class)) {
                helper.assertTrue(VadsKracht.net(level, bat.getBlockPos()).draait(), "the Knabbelbatterij's net runs");
            }
            // the Droomfabriek: fried knabbels came out of the Frituurautomaat (in the chest or on their way to the brewer)
            int gefrituurd = 0;
            for (BlockEntity be : alle(helper, BlockEntity.class)) {
                if (be instanceof Container c) {
                    gefrituurd += tel(c, s -> s.is(nl.juiced.guhs.registry.ModItems.GEFRITUURDE_KAASKNABBELS.get()));
                }
                if (be instanceof BrouwautomaatBlockEntity br) {
                    gefrituurd += br.vakken().getAmountAsInt(0);
                }
            }
            helper.assertTrue(gefrituurd > 0, "the Droomfabriek fried knabbels");
            // the Knutselmachines craft: a knabbel block went round
            blokken[0] = 0;
            for (KnutselmachineBlockEntity k : knutsel) {
                for (int i = 1; i < k.vakken().size(); i++) {
                    if (!k.vakken().getResource(i).isEmpty() && k.vakken().getResource(i).toStack(1).is(nl.juiced.guhs.registry.ModBlocks.BLOCK_OF_KAASKNABBELS.get().asItem())) {
                        blokken[0] += k.vakken().getAmountAsInt(i);
                    }
                }
            }
            helper.assertTrue(blokken[0] != 2 || knutsel.stream().anyMatch(KnutselmachineBlockEntity::bezig), "the Knutselmachines are crafting");
            // the beurs guhs sit on the Knuffelgenerator and the dance floor
            long zitten = level.getEntitiesOfClass(GuhEntity.class, helper.getBounds(), gu -> gu.isTame() && gu.isOrderedToSit()).size();
            helper.assertTrue(zitten >= 8, "eight beurs guhs sit on their source, " + zitten);
        });
    }

    @GuhTest(template = HAL, batch = BATCH, timeoutTicks = 600)
    public static void beursDraait(GameTestHelper helper) {
        draait(helper);
    }

    @GuhTest(template = HAL, batch = BATCH + "_gedraaid", timeoutTicks = 600, rotationSteps = 1)
    public static void beursDraaitGedraaid(GameTestHelper helper) {
        draait(helper);
    }

    @GuhTest(template = HAL, batch = BATCH + "_gedraaid2", timeoutTicks = 600, rotationSteps = 2)
    public static void beursDraaitOmgedraaid(GameTestHelper helper) {
        draait(helper);
    }

    /** Every Vadsschakelaar of the hall cuts its stand: switched off, the machines behind it sleep; on again, they wake. */
    @GuhTest(template = HAL, batch = BATCH + "_schakel", timeoutTicks = 400)
    public static void beursSchakelaarsKnippen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<VadsschakelaarBlock.Kern> schakelaars = alle(helper, VadsschakelaarBlock.Kern.class);
        helper.assertTrue(schakelaars.size() >= 16, "a switch per stand, found " + schakelaars.size());
        helper.runAfterDelay(100, () -> {
            for (VadsschakelaarBlock.Kern k : schakelaars) {
                helper.assertTrue(level.getBlockState(k.getBlockPos()).getValue(VadsschakelaarBlock.AAN), "every switch starts on");
                VadsschakelaarBlock.zet(level, k.getBlockPos(), false);
            }
            helper.runAfterDelay(40, () -> {
                List<VadslampBlockEntity> lampen = alle(helper, VadslampBlockEntity.class);
                // (the Blubkacheltje's lamps burn on: its Knabbelbatterij takes over, that is what the stand shows)
                long aan = lampen.stream().filter(l -> l.heeftKracht() && VadsKracht.net(level, l.getBlockPos()).buffer() == 0).count();
                long opBatterij = lampen.stream().filter(l -> l.heeftKracht() && VadsKracht.net(level, l.getBlockPos()).buffer() > 0).count();
                helper.assertTrue(aan == 0, "with every switch off no Vadslamp burns (but on the battery); " + aan + " still do");
                helper.assertTrue(opBatterij == 3, "the three lamps of the Blubkacheltje burn on its battery, " + opBatterij);
                for (VadsschakelaarBlock.Kern k : schakelaars) {
                    VadsschakelaarBlock.zet(level, k.getBlockPos(), true);
                }
                helper.succeedWhen(() -> helper.assertTrue(lampen.stream().allMatch(VadslampBlockEntity::heeftKracht), "on again: every lamp burns"));
            });
        });
    }

    /** The Bezorgguhtje: the caretaker links the posts, the guhtje brings knabbels from the green post to the orange one. */
    @GuhTest(template = HAL, batch = BATCH + "_bezorg", timeoutTicks = 1600)
    public static void beursBezorgguhtjeBrengt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<StepstationBlockEntity> stations = alle(helper, StepstationBlockEntity.class);
        helper.assertTrue(stations.size() == 1, "one Stepstation");
        // the orange post's chest: no longer emptied by the caretaker, so what arrives stays
        List<net.minecraft.world.level.block.entity.ChestBlockEntity> leeg = new ArrayList<>();
        for (var kist : alle(helper, net.minecraft.world.level.block.entity.ChestBlockEntity.class)) {
            var merk = kist.getPersistentData().getCompoundOrEmpty(BeursBeheer.MERK);
            if (merk.getIntOr("Leeg", 0) == 32) {
                kist.getPersistentData().remove(BeursBeheer.MERK);
                leeg.add(kist);
            }
        }
        helper.assertTrue(leeg.size() == 1, "the delivery chest");
        helper.succeedWhen(() -> {
            helper.assertTrue(stations.get(0).haltes().size() == 2, "the station has its two posts: " + stations.get(0).haltes());
            helper.assertTrue(tel(leeg.get(0), s -> s.is(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get())) > 0, "knabbels were delivered");
        });
    }

    /** The caretaker finds its marks, keeps the Plantagebak planting, and the brewer gets its bottles. */
    @GuhTest(template = HAL, batch = BATCH + "_beheer", timeoutTicks = 200)
    public static void beursBeheerZorgt(GameTestHelper helper) {
        List<BeursBeheer.Kern> beheer = alle(helper, BeursBeheer.Kern.class);
        helper.assertTrue(beheer.size() == 1, "one caretaker");
        helper.succeedWhen(() -> {
            helper.assertTrue(beheer.get(0).gemerkt().size() >= 18, "it looks after its stands: " + beheer.get(0).gemerkt().size());
            for (BrouwautomaatBlockEntity br : alle(helper, BrouwautomaatBlockEntity.class)) {
                helper.assertTrue(br.vakken().getAmountAsInt(1) > 0, "the brewer at " + br.getBlockPos() + " has bottles");
            }
            for (PlantagebakBlockEntity bak : alle(helper, PlantagebakBlockEntity.class)) {
                helper.assertTrue(bak.stand() != PlantagebakBlockEntity.Stand.LEEG, "the Plantagebak planted");
            }
        });
    }

    /** Inside the beurs the own machines may change blocks, visitors may flip switches but not open a stand's chest. */
    @GuhTest(template = HAL, batch = BATCH + "_bescherming", timeoutTicks = 100)
    public static void beursKijkenMagPakkenNiet(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB b = helper.getBounds();
        BoundingBox doos = new BoundingBox((int) b.minX, (int) b.minY, (int) b.minZ, (int) b.maxX - 1, (int) b.maxY - 1, (int) b.maxZ - 1);
        Bescherming.zetDoos(level, Beurs.STRUCTUUR, doos);
        try {
            BlockPos kist = alle(helper, net.minecraft.world.level.block.entity.ChestBlockEntity.class).get(0).getBlockPos();
            BlockPos schakelaar = alle(helper, VadsschakelaarBlock.Kern.class).get(0).getBlockPos();
            BlockPos midden = BlockPos.containing(b.getCenter());
            helper.assertTrue(Bescherming.beschermd(level, midden), "the hall is protected");
            helper.assertTrue(Bescherming.magWijzigen(level, midden, null), "its own machines (nobody) may change blocks");
            helper.assertTrue(!Bescherming.magWijzigen(level, midden, java.util.UUID.randomUUID()), "a player's machine may not");
            ServerPlayer p = GuhMockPlayer.of(helper);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            helper.assertTrue(!Beurs.kijkenMag(level, p, kist), "a visitor can't open a stand's chest");
            helper.assertTrue(Beurs.kijkenMag(level, p, schakelaar), "a visitor may flip a switch");
        } finally {
            Bescherming.wisDozen(level, Beurs.STRUCTUUR);
        }
        helper.succeed();
    }

    /** The worldgen data: guaranteed once 800-1500 from spawn in new terrain only, the texts, the beursguhs' lines. */
    @GuhTest(template = "empty", batch = BATCH + "_data")
    public static void beursWereldEnTeksten(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        StructureSet zeker = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(Beurs.STRUCTUUR + "_gegarandeerd"));
        helper.assertTrue(zeker != null && zeker.placement() instanceof GegarandeerdPlacement g && g.alleenNieuw()
                && g.minAfstand() == 800 && g.maxAfstand() == 1500, "guaranteed 800-1500, alleen_nieuw");
        helper.assertTrue(access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(Beurs.STRUCTUUR)) != null, "and rare copies elsewhere");
        Language lang = Language.getInstance();
        helper.assertTrue(lang.has("structure.guhs." + Beurs.STRUCTUUR) && lang.has("structure.guhs." + Beurs.STRUCTUUR + ".tooltip"), "Superkompas texts");
        Beurs.PRAAT.forEach((plek, p) -> {
            for (int i = 1; i <= p.regels(); i++) {
                String key = "quest.guhs.techbeurs." + plek.substring("techbeurs_".length()) + "." + i;
                helper.assertTrue(lang.has(key), "the line " + key);
            }
        });
        helper.assertTrue(lang.has("gui.guhs.techbeurs.kijken"), "the look-don't-take message");
        helper.succeed();
    }
}
