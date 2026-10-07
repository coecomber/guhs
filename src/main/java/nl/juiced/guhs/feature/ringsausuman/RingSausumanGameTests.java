package nl.juiced.guhs.feature.ringsausuman;

import java.util.List;
import java.util.Optional;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of bbq2 (ring-sausuman). Template: ringsausuman_test_kamer (21 x 12 x 21, a bare floor of polished blackstone;
 * things on the floor stand at helper y 2).
 * <ul>
 *   <li>the tower's template holds what the Java side expects where it expects it; the structure, its guaranteed copy, the
 *       sluier, the Superkompas entry, the questline and the baking scene exist;</li>
 *   <li>the nod to Guh-technologie is real: the hall's five machines, wire and Mika-rad, taken from the template, are one
 *       net of vadskracht that is too heavy and stands still, and it runs as soon as enough machines are gone (so nothing is faked);</li>
 *   <li>the whole questline for one player while a second one's does not move: no access before chapter 4, the refusal, the
 *       three stations (once each, again when lost), the lever (only with all three, the scene, the onion ring), the sulking
 *       wizard (both endings), the rewards once, the daily onion ring; the blocks never change;</li>
 *   <li>the Pannantir shows a vision (never the same twice in a row); the Mika-rad can be poked.</li>
 * </ul>
 */
public class RingSausumanGameTests {
    private static final String KAMER = "ringsausuman_test_kamer", BATCH = "ringsausuman";
    private static final Verhaallijn LIJN = RingSausumanFeature.LIJN;
    /** The answers of SausumanRol. */
    private static final int NEE = 1, DELEN = 2, HAPJE = 3, WEG = 4, MACHINES = 5, NOG_EEN = 6;

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setOnGround(true);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void klik(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        BlockPos abs = helper.absolutePos(at);
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    private static int heeft(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    private static boolean behaald(ServerPlayer p, String id) {
        AdvancementHolder holder = p.level().getServer().getAdvancements().get(Guhs.id(id));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Chapter 4 is done for this player: the tower is theirs to enter. */
    private static void hoofdstuk4(ServerPlayer p) {
        Ring.lijn(4).zet(p, Ring.lijn(4).stappen());
    }

    // =================================================================================================================
    // the tower
    // =================================================================================================================

    /** The template holds what Toren.java says, where it says it; the structure, the sluier, the questline and the scene exist. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringsausumanTorenBestaat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<StructureTemplate> toren = level.getStructureManager().get(Guhs.id(Toren.STRUCTUUR));
        helper.assertTrue(toren.isPresent(), "the template guhs:" + Toren.STRUCTUUR);
        StructurePlaceSettings zo = new StructurePlaceSettings();
        StructureTemplate t = toren.get();
        helper.assertTrue(t.getSize().getY() >= Toren.G + 34, "a real tower: " + t.getSize());
        List<StructureTemplate.StructureBlockInfo> bakkers = t.filterBlocks(BlockPos.ZERO, zo, RingSausumanFeature.RINGENBAKKER.get());
        helper.assertTrue(bakkers.size() == 1 && bakkers.get(0).pos().equals(Toren.BAKKER)
                && bakkers.get(0).state().getValue(HorizontalDirectionalBlock.FACING) == Direction.SOUTH, "one Ringenbakker at " + Toren.BAKKER + ", looking south");
        List<StructureTemplate.StructureBlockInfo> voorraden = t.filterBlocks(BlockPos.ZERO, zo, RingSausumanFeature.VOORRAAD.get());
        helper.assertTrue(voorraden.size() == 3, "three stations: " + voorraden.size());
        for (Ingredient soort : Ingredient.values()) {
            BlockPos plek = Toren.VOORRADEN.get(soort.ordinal());
            helper.assertTrue(voorraden.stream().anyMatch(i -> i.pos().equals(plek) && i.state().getValue(VoorraadBlock.SOORT) == soort),
                    "the station of " + soort + " at " + plek);
            helper.assertTrue(plek.getY() > Toren.NPC.getY() + 5, "every station is upstairs");
        }
        List<StructureTemplate.StructureBlockInfo> raden = t.filterBlocks(BlockPos.ZERO, zo, RingSausumanFeature.MIKARAD.get());
        helper.assertTrue(raden.size() == 1 && raden.get(0).pos().equals(Toren.MIKARAD), "one Mika-rad at " + Toren.MIKARAD);
        List<StructureTemplate.StructureBlockInfo> pannen = t.filterBlocks(BlockPos.ZERO, zo, RingSausumanFeature.PANNANTIR.get());
        helper.assertTrue(pannen.size() == 1 && pannen.get(0).pos().equals(Toren.PANNANTIR), "the Pannantir at " + Toren.PANNANTIR);
        List<StructureTemplate.StructureBlockInfo> vuren = t.filterBlocks(BlockPos.ZERO, zo, RingFeature.RUSTVUUR.get());
        helper.assertTrue(vuren.size() == 1 && vuren.get(0).pos().equals(Toren.RUSTVUUR), "a rest point of the Knabbelring in the yard");
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, zo, RingSausumanFeature.SPUTTERPIJP.get()).size() >= 8, "a tower full of sputtering pipes");
        helper.assertTrue(Toren.NPC.getY() == Toren.G + 1 && Toren.DEUR.getY() == Toren.G + 1 && Toren.BAKKER.getY() == Toren.G + 2, "the hall is on the ground");
        // the scene's rotation is the template's rotation
        for (Rotation draai : Rotation.values()) {
            helper.assertTrue(Toren.draai(draai.rotate(Direction.SOUTH)) == draai, "Toren.draai for " + draai);
        }
        // structure, guaranteed copy (and only that), sluier, Superkompas
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        helper.assertTrue(Kopieen.structuur(level, Toren.STRUCTUUR) != null, "the structure guhs:" + Toren.STRUCTUUR);
        helper.assertTrue(sets.getValue(Guhs.id(Toren.STRUCTUUR + "_gegarandeerd")) != null && sets.getValue(Guhs.id(Toren.STRUCTUUR)) == null,
                "exactly one copy per world: the guaranteed set and no random spread");
        helper.assertTrue(Sluiers.structuren().contains(Toren.STRUCTUUR), "Guhdalfs sluier round the tower");
        helper.assertTrue(SuperkompasItem.categoryOf(Toren.STRUCTUUR) >= 0, "in the Superkompas");
        // the questline (ring-kern's halte), the scene
        helper.assertTrue(Verhaallijnen.van("ring_sausuman") == LIJN && Ring.lijn(Ring.SAUSUMAN_NR) == LIJN && LIJN.stappen() == 4
                && "ring_h4".equals(LIJN.na()) && LIJN.groep().equals(Ring.GROEP), "the questline ring_sausuman: 4 steps after chapter 4");
        helper.assertTrue(Cutscene.van("ringsausuman_bakken") == Bakkerij.BAKKEN && Bakkerij.BAKKEN.duur() == Bakkerij.DUUR, "the baking scene");
        // the sluier opens with chapter 4, per player
        ServerPlayer a = speler(helper, 3, 3), b = speler(helper, 5, 3);
        hoofdstuk4(a);
        helper.assertTrue(Sluiers.open(a, Toren.STRUCTUUR) && !Sluiers.open(b, Toren.STRUCTUUR) && LIJN.aanDeBeurt(a) && !LIJN.aanDeBeurt(b),
                "open for whoever finished chapter 4, closed for the others");
        // the quest items never go into storage
        for (Ingredient soort : Ingredient.values()) {
            helper.assertTrue(new ItemStack(soort.item()).is(Features.LOANED), soort + " is a loaned item");
        }
        weg(helper, a, b);
        helper.succeed();
    }

    // =================================================================================================================
    // the nod to Guh-technologie
    // =================================================================================================================

    /**
     * The hall's net, block for block out of the template (the Mika-rad, the Guhdraad, the five machines): one real net of
     * vadskracht, too heavy, so it stands still; with only the Oogster left it runs.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void ringsausumanTeZwaar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate t = level.getStructureManager().get(Guhs.id(Toren.STRUCTUUR)).orElseThrow();
        // the hall's west side of the template, on the test floor: template (x, G + 1, z) -> helper (x - 5, 2, z - 6)
        int machines = 0, draden = 0;
        for (Block block : List.of(RingSausumanFeature.MIKARAD.get(), nl.juiced.guhs.registry.ModBlocks.GUH_WIRE.get())) {
            for (StructureTemplate.StructureBlockInfo info : t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block)) {
                if (info.pos().getY() == Toren.G + 1) {
                    helper.setBlock(info.pos().offset(-5, 2 - (Toren.G + 1), -6), info.state());
                    draden += block instanceof GuhWireBlock ? 1 : 0;
                }
            }
        }
        BlockPos rad = Toren.MIKARAD.offset(-5, 2 - (Toren.G + 1), -6);
        BlockPos[] rond = new BlockPos[5];
        int vraag = 0;
        for (var machine : List.of(
                new Object[]{nl.juiced.guhs.feature.techmachine.TechmachineFeature.KNABBELAAR.get(), VadsGetallen.KNABBELAAR},
                new Object[]{nl.juiced.guhs.feature.techmachine.TechmachineFeature.KNUTSELMACHINE.get(), VadsGetallen.KNUTSELMACHINE},
                new Object[]{nl.juiced.guhs.feature.techmachine.TechmachineFeature.OOGSTER.get(), VadsGetallen.OOGSTER},
                new Object[]{nl.juiced.guhs.feature.guhoven.GuhovenFeature.GUH_OVEN.get(), VadsGetallen.GUH_OVEN},
                new Object[]{nl.juiced.guhs.feature.techmachine.TechmachineFeature.VADSMOLEN.get(), VadsGetallen.MOLEN})) {
            for (StructureTemplate.StructureBlockInfo info : t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), (Block) machine[0])) {
                if (info.pos().getY() == Toren.G + 1) {
                    BlockPos hier = info.pos().offset(-5, 2 - (Toren.G + 1), -6);
                    helper.setBlock(hier, info.state());
                    rond[machines++] = hier;
                    vraag += (int) machine[1];
                }
            }
        }
        final int gevraagd = vraag, aantal = machines;
        helper.assertTrue(aantal == 5 && draden >= 5, "the hall has five machines on a run of Guhdraad: " + aantal + ", " + draden);
        helper.assertTrue(gevraagd > MikaradBlock.VERMOGEN, "they ask more than a Mika-rad gives");
        helper.runAfterDelay(45, () -> {
            VadsNet net = VadsKracht.net(level, helper.absolutePos(rad));
            helper.assertTrue(net.status() == VadsNet.Status.TE_ZWAAR && !net.draait(), "too heavy, so everything stands still: " + net.status());
            helper.assertTrue(net.aanbod() == MikaradBlock.VERMOGEN && net.vraag() == gevraagd && net.tekort() == gevraagd - MikaradBlock.VERMOGEN,
                    "the readout: " + net.vraag() + "/" + net.aanbod());
            for (int i = 0; i < aantal; i++) {
                BlockState state = helper.getBlockState(rond[i]);
                helper.assertTrue(level.getBlockEntity(helper.absolutePos(rond[i])) != null && state.getValue(MachineBlock.SNOET) == Snoet.SLAAPT,
                        "a real machine, asleep: " + state);
            }
            helper.assertTrue(!VadsKracht.regels(level, helper.absolutePos(rad)).isEmpty(), "the hover readout has lines for the Mika-rad");
            // everything but the Oogster goes (the oven and the mill hang behind it): what is left fits on one Mika-rad
            for (int i : new int[]{0, 1, 3, 4}) {
                helper.setBlock(rond[i], Blocks.AIR);
            }
        });
        helper.runAfterDelay(95, () -> {
            VadsNet net = VadsKracht.net(level, helper.absolutePos(rad));
            helper.assertTrue(net.draait() && net.vraag() == VadsGetallen.OOGSTER && net.aanbod() == MikaradBlock.VERMOGEN,
                    "one Oogster does run on a Mika-rad: " + net.status() + " " + net.vraag() + "/" + net.aanbod());
            helper.assertTrue(helper.getBlockState(rond[2]).getValue(MachineBlock.SNOET) != Snoet.SLAAPT, "and it woke up: " + helper.getBlockState(rond[2]));
            helper.succeed();
        });
    }

    // =================================================================================================================
    // the questline
    // =================================================================================================================

    private static GuhNpcEntity sausuman(GameTestHelper helper, BlockPos at) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.SAUSUMAN);
        BlockPos abs = helper.absolutePos(at);
        npc.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        npc.setPersistenceRequired();
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    /** The whole questline for player a (who gives the bite) and b (who ate it), each on their own; the blocks never change. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void ringsausumanQuestlijn(GameTestHelper helper) {
        BlockPos bakker = new BlockPos(10, 2, 4), deeg = new BlockPos(6, 2, 4), saus = new BlockPos(8, 2, 4), kaas = new BlockPos(12, 2, 4);
        helper.setBlock(bakker, RingSausumanFeature.RINGENBAKKER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        BlockPos[] stations = {deeg, saus, kaas};
        for (Ingredient soort : Ingredient.values()) {
            helper.setBlock(stations[soort.ordinal()], RingSausumanFeature.VOORRAAD.get().defaultBlockState().setValue(VoorraadBlock.SOORT, soort));
        }
        BlockState bakkerState = helper.getBlockState(bakker);
        GuhNpcEntity npc = sausuman(helper, new BlockPos(8, 2, 8));
        NpcRole rol = NpcRollen.van(npc);
        helper.assertTrue(rol instanceof SausumanRol, "Sausuman has his role");
        ServerPlayer a = speler(helper, 9, 9), b = speler(helper, 11, 9);
        Item ring = RingSausumanFeature.UIENRING.get(), pan = RingSausumanFeature.PANNANTIR_ITEM.get();
        // before chapter 4: he sends you away, nothing gives anything
        rol.talk(npc, a);
        rol.antwoord(npc, a, NEE);
        klik(helper, a, deeg);
        klik(helper, a, bakker);
        helper.assertTrue(LIJN.stap(a) == 0 && !LIJN.begonnen(a) && Bakkerij.aantal(a) == 0, "nothing before chapter 4");
        hoofdstuk4(a);
        hoofdstuk4(b);
        // 0: he wants a bite; closing the screen is no answer; the stations and the lever wait for the talk
        klik(helper, a, deeg);
        klik(helper, a, bakker);
        helper.assertTrue(Bakkerij.aantal(a) == 0, "the stations give nothing before you talked to him");
        rol.talk(npc, a);
        helper.assertTrue(LIJN.begonnen(a) && LIJN.stap(a) == 0, "talking begins the questline");
        rol.antwoord(npc, a, -1);
        helper.assertTrue(LIJN.stap(a) == 0, "closing the screen is no answer");
        rol.antwoord(npc, a, NEE);
        helper.assertTrue(LIJN.stap(a) == 1 && behaald(a, "quest/ring_sausuman_stap_1"), "no bite for him: he bakes his own (step 1)");
        // 1: the three stations, once each
        klik(helper, a, bakker);
        helper.assertTrue(LIJN.stap(a) == 1, "the lever does nothing without the ingredients");
        klik(helper, a, deeg);
        klik(helper, a, deeg);
        helper.assertTrue(heeft(a, Ingredient.DEEG.item()) == 1 && LIJN.stap(a) == 1, "one ringdeeg, however often you click");
        klik(helper, a, saus);
        helper.assertTrue(Bakkerij.aantal(a) == 2 && LIJN.stap(a) == 1, "two of three");
        klik(helper, a, kaas);
        helper.assertTrue(heeft(a, RingSausumanFeature.UI.get()) == 1 && LIJN.stap(a) == 2, "the Kaaskast only had an onion; all three: step 2");
        helper.assertTrue(LIJN.stap(b) == 0 && Bakkerij.aantal(b) == 0, "b's questline did not move");
        // 2: lose one: the lever refuses, the station gives it again; then the lever takes all three and the scene plays
        GuhQuests.take(a, RingSausumanFeature.UI.get(), 1);
        klik(helper, a, bakker);
        helper.assertTrue(LIJN.stap(a) == 2 && !Cutscenes.bezig(a) && Bakkerij.aantal(a) == 2 && !LIJN.vlag(a, Bakkerij.GEVULD), "one short: no baking, nothing taken");
        klik(helper, a, kaas);
        helper.assertTrue(Bakkerij.aantal(a) == 3, "a lost ingredient is given again");
        klik(helper, a, bakker);
        helper.assertTrue(Cutscenes.bezig(a) && Bakkerij.aantal(a) == 0 && LIJN.vlag(a, Bakkerij.GEVULD) && LIJN.stap(a) == 2,
                "the ingredients go in and the baking scene plays; the step waits for its end");
        klik(helper, a, deeg);
        helper.assertTrue(Bakkerij.aantal(a) == 0, "nothing more to fetch once the machine is filled");
        helper.onEachTick(() -> {
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(a));
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(b));
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(!Cutscenes.bezig(a) && Cutscenes.gezien(a, Bakkerij.BAKKEN.id()) && LIJN.stap(a) == 3 && heeft(a, ring) == 1
                    && behaald(a, "quest/ring_sausuman_uienring"), "after the scene: an onion ring, step 3 (" + LIJN.stap(a) + ", " + heeft(a, ring) + ")");
            klik(helper, a, bakker);
            helper.assertTrue(heeft(a, ring) == 1 && !Cutscenes.bezig(a), "the machine cools down while he sulks");
            // 3: he sulks; leaving him is no progress; a bite of the onion ring finishes it
            rol.talk(npc, a);
            rol.antwoord(npc, a, WEG);
            helper.assertTrue(LIJN.stap(a) == 3, "letting him sulk is fine, and changes nothing");
            rol.antwoord(npc, a, HAPJE);
            helper.assertTrue(LIJN.klaar(a) && heeft(a, ring) == SausumanRol.UIENRINGEN && heeft(a, pan) == 1,
                    "he ate the bite; the Pannantir and " + SausumanRol.UIENRINGEN + " onion rings (" + heeft(a, ring) + ", " + heeft(a, pan) + ")");
            helper.assertTrue(behaald(a, "quest/ring_sausuman_stap_4") && behaald(a, "quest/ring_sausuman_mok") && behaald(a, "knabbelring/ring_sausuman_mok"),
                    "the advancements of the end");
            rol.antwoord(npc, a, HAPJE);
            helper.assertTrue(heeft(a, ring) == SausumanRol.UIENRINGEN && heeft(a, pan) == 1, "the rewards come once");
            // afterwards: he explains his machines, the lever bakes one onion ring a day
            rol.talk(npc, a);
            rol.antwoord(npc, a, MACHINES);
            rol.antwoord(npc, a, NOG_EEN);
            klik(helper, a, bakker);
            helper.assertTrue(heeft(a, ring) == SausumanRol.UIENRINGEN + 1 && behaald(a, "quest/ring_sausuman_dagelijks") && !Cutscenes.bezig(a),
                    "one onion ring from the lever today");
            klik(helper, a, bakker);
            helper.assertTrue(heeft(a, ring) == SausumanRol.UIENRINGEN + 1, "and no second one the same day");
            // b, at the same blocks and the same Sausuman, from the start: the other answer, and b eats the onion ring
            helper.assertTrue(LIJN.stap(b) == 0, "b is still at the start");
            rol.talk(npc, b);
            rol.antwoord(npc, b, DELEN);
            for (BlockPos station : stations) {
                klik(helper, b, station);
            }
            helper.assertTrue(LIJN.stap(b) == 2 && Bakkerij.aantal(b) == 3, "b has the three ingredients from the same stations");
            klik(helper, b, bakker);
            helper.assertTrue(Cutscenes.bezig(b), "b bakes too");
        });
        helper.runAfterDelay(26, () -> {
            helper.assertTrue(LIJN.stap(b) == 3 && heeft(b, ring) == 1, "b's own onion ring");
            GuhQuests.take(b, ring, 1);                                  // (eaten)
            rol.talk(npc, b);
            rol.antwoord(npc, b, HAPJE);
            helper.assertTrue(LIJN.klaar(b) && heeft(b, ring) == SausumanRol.UIENRINGEN && heeft(b, pan) == 1,
                    "owning up to having eaten it finishes the questline too");
            // nothing in the world changed
            helper.assertTrue(helper.getBlockState(bakker) == bakkerState, "the Ringenbakker is as it was");
            for (Ingredient soort : Ingredient.values()) {
                helper.assertTrue(helper.getBlockState(stations[soort.ordinal()]).getValue(VoorraadBlock.SOORT) == soort, "the stations are as they were");
            }
            npc.discard();
            weg(helper, a, b);
            helper.succeed();
        });
    }

    // =================================================================================================================
    // the Pannantir, the Mika-rad
    // =================================================================================================================

    /** A look into the Pannantir: a vision, never the same one twice in a row, and its advancement; a poke at the Mika-rad. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringsausumanPannantirEnMikarad(GameTestHelper helper) {
        BlockPos pan = new BlockPos(4, 2, 4), rad = new BlockPos(8, 2, 4);
        helper.setBlock(pan, RingSausumanFeature.PANNANTIR.get());
        helper.setBlock(rad, RingSausumanFeature.MIKARAD.get());
        ServerPlayer p = speler(helper, 5, 6);
        int vorige = -1;
        for (int i = 0; i < 24; i++) {
            klik(helper, p, pan);
            int nu = LIJN.teller(p, "beeld");
            helper.assertTrue(nu >= 0 && nu < PannantirBlock.BEELDEN && nu != vorige, "vision " + nu + " after " + vorige);
            vorige = nu;
        }
        helper.assertTrue(behaald(p, "quest/ring_sausuman_pannantir"), "looked into the Pannantir");
        helper.assertTrue(LIJN.stap(p) == 0 && !LIJN.begonnen(p), "looking is no step of the questline");
        klik(helper, p, rad);
        helper.assertTrue(behaald(p, "quest/ring_sausuman_mikarad"), "poked the Mika");
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(rad)) instanceof MikaradBlock.Kern kern && kern.vadsAanbod() == MikaradBlock.VERMOGEN
                && kern.vadsSoort() == null, "the Mika-rad is a source of " + MikaradBlock.VERMOGEN);
        weg(helper, p);
        helper.succeed();
    }
}
