package nl.juiced.guhs.feature.ringh1;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.barbecuether.BarbecuePutStructure;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.barbecuether.GrillPortalBlock;
import nl.juiced.guhs.feature.barbecuether.Grillguh;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.ModDimensions;

/**
 * bbq2 (ring-h1): chapter 1 of the Knabbelring, server side. Mock players get no packets and are not ticked: the tests post
 * the player tick themselves (a card and a scene then last two ticks each). Template ringh1_test_kamer
 * (tools/features/ring_h1_bouw.py): 25 x 12 x 25, dirt with a lawn on it (the lawn is helper y 2, you stand on helper y 3).
 */
public final class RingH1GameTests {
    private static final String KAMER = "ringh1_test_kamer", BATCH = "ringh1";

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(x, 3, z));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setOnGround(true);
        Ring.OVERAL = true;
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Ring.wis(p);
            RingH1Events.wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void ruimOp(GameTestHelper helper) {
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(25, 30, 25).inflate(2);
        helper.getLevel().getEntitiesOfClass(Entity.class, kamer, e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
    }

    private static void tik(ServerPlayer p) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
    }

    private static <T extends Entity> T een(GameTestHelper helper, Class<T> soort, java.util.function.Predicate<T> welke, String wat) {
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(25, 12, 25);
        List<T> er = helper.getLevel().getEntitiesOfClass(soort, kamer, e -> e.isAlive() && welke.test(e));
        helper.assertTrue(er.size() == 1, wat + ": " + er.size());
        return er.get(0);
    }

    /**
     * The whole chapter at a camp that is turned a quarter: the gate (the Grillguh first), the card and the scene of the
     * arrival, the three chores, the party with the ring, Sam-guh joins, the provisions, the walk to the portal; the lock of
     * the grill portal opens for this player only. A second player uses the same props for their own story.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void ringh1Verhaal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Verhaallijn l = RingH1Feature.LIJN;
        helper.assertTrue(l == Ring.lijn(1) && l.stappen() == 6, "chapter 1 has six steps");
        ServerPlayer p = speler(helper, 20, 20), ander = speler(helper, 21, 21);
        BlockPos hoek = helper.absolutePos(new BlockPos(14, 2, 4));
        helper.assertTrue(RingH1Events.zetKamp(level, hoek, Rotation.CLOCKWISE_90), "the camp template exists");
        Gouw.Plek kamp = new Gouw.Plek(hoek, Rotation.CLOCKWISE_90);
        GuhNpcEntity guhdalf = een(helper, GuhNpcEntity.class, n -> n.getKind() == GuhNpcEntity.Kind.GUHDALF, "one Guhdalf at the camp");
        GuhEntity samThuis = een(helper, GuhEntity.class, Gouw::isSamThuis, "one Sam-guh at home");
        helper.assertTrue(guhdalf.blockPosition().equals(kamp.wereld(Gouw.GUHDALF)) && Gouw.ROL.equals(guhdalf.roleData.getStringOr(NpcRollen.PLEK, ""))
                && NpcRollen.van(guhdalf) instanceof Feest.GuhdalfRol, "Guhdalf sits on his spot with the role of the Gouw");
        Gouw.Plek gelezen = Gouw.kampBij(level, guhdalf.blockPosition());
        helper.assertTrue(gelezen.draai() == Rotation.CLOCKWISE_90 && gelezen.hoek().equals(hoek), "the camp is read back from the party table: " + gelezen);
        BlockPos kist = null, tafel = kamp.wereld(Gouw.TAFEL);
        BlockPos[] kratten = new BlockPos[3];
        for (BlockPos pos : BlockPos.betweenClosed(kamp.doos().minX(), kamp.doos().minY(), kamp.doos().minZ(), kamp.doos().maxX(), kamp.doos().maxY(), kamp.doos().maxZ())) {
            if (level.getBlockState(pos).is(RingH1Feature.VUURWERKKIST.get())) {
                kist = pos.immutable();
            } else if (level.getBlockState(pos).is(RingH1Feature.PROVIAND.get())) {
                kratten[level.getBlockState(pos).getValue(RingH1Blocks.Proviand.SOORT)] = pos.immutable();
            }
        }
        helper.assertTrue(kist != null && level.getBlockState(tafel).is(RingH1Feature.FEESTTAFEL.get()) && kratten[0] != null && kratten[1] != null && kratten[2] != null,
                "the crate, the table and the three provisions stand in the camp");
        helper.assertTrue(level.getBlockState(kist).getDestroySpeed(level, kist) < 0, "a quest prop can't be broken");
        BlockPos deKist = kist;

        // the gate: the Grillguh's barbecue first
        RingH1Events.seconde(p);
        Feest.praat(guhdalf, p, l.stap(p));
        helper.assertTrue(!l.begonnen(p) && l.stap(p) == 0 && !Cutscenes.bezig(p), "without the Grillguh's quest the story does not start");
        Grillguh.setStep(p, Grillguh.DONE);
        RingH1Events.seconde(p);
        helper.assertTrue(l.begonnen(p) && Ring.begonnen(p) && l.stap(p) == 0, "the barbecue burns: the story has begun (zoek Guhdalf)");
        helper.assertTrue(Zicht.magZien(p, samThuis) && Feest.klikSam(samThuis, p, InteractionHand.MAIN_HAND).consumesAction() && !l.vlag(p, Feest.SAM),
                "Sam-guh at home says hello; nothing to invite him to yet");
        Feest.vuurwerk(p, deKist);
        helper.assertTrue(!l.vlag(p, Feest.VUURWERK), "a rocket before the chores is just a rocket");

        // step 0: the card, the scene, then step 1
        Feest.praat(guhdalf, p, 0);
        helper.assertTrue(Cutscenes.bezig(p) && l.stap(p) == 0, "the narrator card shows");
        Doel thuis = l.doel(p);
        helper.assertTrue(thuis != null && guhdalf.blockPosition().equals(thuis.plek()), "from now on the compass points at this camp");
        helper.onEachTick(() -> {
            tik(p);
            tik(ander);
        });
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Verteller.gezien(p, RingH1Feature.KAART) && Cutscenes.gezien(p, RingH1Feature.AANKOMST_ID) && l.stap(p) == 1,
                    "card, scene, step 1: " + l.stap(p));
            // step 1: the three chores, in any order, each once
            p.getPersistentData().remove("guhs_ringh1_vuurwerk");
            Feest.vuurwerk(p, deKist);
            helper.assertTrue(l.vlag(p, Feest.VUURWERK) && l.stap(p) == 1, "the rocket is the first chore");
            helper.assertTrue(!level.getEntitiesOfClass(FireworkRocketEntity.class, new AABB(deKist).inflate(4, 40, 4)).isEmpty() && p.getHealth() == p.getMaxHealth(),
                    "a rocket bursts high above the crate and hurts nobody");
            Feest.tafel(p, tafel);
            helper.assertTrue(l.vlag(p, Feest.TAFEL) && l.stap(p) == 1, "the table is the second chore");
            Feest.klikSam(samThuis, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(l.vlag(p, Feest.SAM) && l.stap(p) == 2, "Sam-guh is invited: all three done, step 2");
            helper.assertTrue(level.getBlockState(deKist).is(RingH1Feature.VUURWERKKIST.get()) && level.getBlockState(tafel).is(RingH1Feature.FEESTTAFEL.get())
                    && samThuis.isAlive(), "nothing in the world was used up");
            // a second player does the chores with the same props
            Grillguh.setStep(ander, Grillguh.DONE);
            l.begin(ander);
            l.zet(ander, 1);
            Feest.vuurwerk(ander, deKist);
            Feest.tafel(ander, tafel);
            helper.assertTrue(l.vlag(ander, Feest.VUURWERK) && l.vlag(ander, Feest.TAFEL) && l.stap(ander) == 1 && l.stap(p) == 2, "everybody their own chores");
            // step 2: the party
            Feest.proviand(p, kratten[0], 0);
            helper.assertTrue(!l.vlag(p, Feest.PROVIAND.get(0)), "no provisions before Sam-guh walks along");
            Feest.praat(guhdalf, p, 2);
            helper.assertTrue(Cutscenes.bezig(p) && !Ring.heeft(p), "the party scene plays");
        });
        helper.runAfterDelay(24, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && l.stap(p) == 3 && Ring.heeft(p) && Ring.kreeg(p), "after the party: the ring, step 3");
            helper.assertTrue(!Ring.heeft(ander) && l.stap(ander) == 1, "the other player has no ring yet");
            // lost the ring: Guhdalf has another
            Ring.neem(p);
            GuhQuests.saved(p).putBoolean("guhs_ring_gegeven", true);
            Feest.praat(guhdalf, p, 3);
            helper.assertTrue(Ring.heeft(p), "a lost ring comes back at Guhdalf");
            // step 3: Sam-guh joins
            helper.assertTrue(Sam.van(p) == null && !Sam.looptMee(p), "Sam-guh does not walk along yet");
            Feest.klikSam(samThuis, p, InteractionHand.MAIN_HAND);
            GuhEntity sam = Sam.van(p);
            helper.assertTrue(l.stap(p) == 4 && Sam.looptMee(p) && sam != null && sam.getVariant() == GuhVariant.SAM_GUH && Sam.isSam(sam), "he walks along: step 4");
            helper.assertTrue(!Zicht.magZien(p, samThuis) && Zicht.magZien(ander, samThuis) && Zicht.magZien(p, sam) && !Zicht.magZien(ander, sam),
                    "p sees their own Sam-guh, the other player still sees the one at home");
            // step 4: the provisions, each kind once
            Feest.proviand(p, kratten[1], 1);
            Feest.proviand(p, kratten[1], 1);
            helper.assertTrue(l.vlag(p, Feest.PROVIAND.get(1)) && l.stap(p) == 4, "one kind packed");
            Feest.proviand(p, kratten[0], 0);
            Feest.proviand(p, kratten[2], 2);
            helper.assertTrue(l.stap(p) == 5, "all three packed: step 5");
            // step 5: the walk to the portal
            RingH1Events.seconde(p);
            helper.assertTrue(l.stap(p) == 5 && !Feest.bijPortaal(p), "not at a portal yet");
            helper.assertTrue(GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, p) != null, "the grill portal is still closed");
            BlockPos portaal = helper.absolutePos(new BlockPos(22, 3, 22));
            level.setBlock(portaal, BarbecuetherFeature.BARBECUETHER_PORTAAL.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            helper.assertTrue(level.getBlockState(portaal).is(BarbecuetherFeature.BARBECUETHER_PORTAAL.get()), "a burning portal stands in the room");
            RingH1Events.seconde(p);
            helper.assertTrue(l.klaar(p) && Ring.hoofdstuk(p) == 2, "at the portal with Sam-guh: chapter 1 is done");
            helper.assertTrue(GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, p) == null && GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, ander) != null,
                    "the portal is open for p and still closed for the other player");
            level.setBlock(portaal, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            // afterwards: on the trip Guhdalf has no treat; after the whole story one a day, from him or from the table
            Feest.praat(guhdalf, p, l.stap(p));
            helper.assertTrue(GuhQuests.count(p, RingFeature.FEESTKNABBEL.get()) == 0, "no party treat while the trip goes on");
            for (int n = 2; n <= 6; n++) {
                Ring.lijn(n).zet(p, Ring.lijn(n).stappen());
            }
            Feest.praat(guhdalf, p, l.stap(p));
            Feest.tafel(p, tafel);
            Feest.praat(guhdalf, p, l.stap(p));
            helper.assertTrue(GuhQuests.count(p, RingFeature.FEESTKNABBEL.get()) == 1, "one Feestknabbel a day: " + GuhQuests.count(p, RingFeature.FEESTKNABBEL.get()));
            weg(helper, p, ander);
            ruimOp(helper);
            helper.succeed();
        });
    }

    /**
     * An old big barbecueput (a structure start made by hand around the template barbecueput_groot): Bezetting puts the camp
     * on the first free spot of the strip around it, then Guhdalf and Sam-guh at the camp; the frame of the pit is found.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_kamp", timeoutTicks = 2400)
    public static void ringh1KampBijOudePut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String[] ids = {Gouw.KAMP, Gouw.GUHDALF_KAMP, Gouw.SAM_KAMP};
        Structure structure = Kopieen.structuur(level, Gouw.PUT);
        helper.assertTrue(structure != null, "the structure guhs:barbecueput exists");
        // the pit lies so that the first choice of the camp (11, G, -13) is the corner (3, 2, 3) of the room
        BlockPos keuze = Gouw.KEUZES.get(0);
        helper.assertTrue(keuze.equals(new BlockPos(11, Gouw.G, -13)) && Gouw.KEUZES.size() == 48, "the choices: " + Gouw.KEUZES.size() + " " + keuze);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3)).subtract(keuze);
        StructurePoolElement element = StructurePoolElement.single("guhs:" + Gouw.PUT_STUK).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = element.getBoundingBox(level.getStructureManager(), pos, Rotation.NONE);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, pos, 0, Rotation.NONE, box, LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(pos), 0, new PiecesContainer(List.of(piece)));
        // (Bezetting only looks where the chunks and their entities are loaded: all around the pit, for a while)
        BlockPos midden = pos.offset(Gouw.PUT_MIDDEN);
        int x0 = (midden.getX() - Gouw.KAMP_ZOEK - 1) >> 4, x1 = (midden.getX() + Gouw.KAMP_ZOEK + 1) >> 4;
        int z0 = (midden.getZ() - Gouw.KAMP_ZOEK - 1) >> 4, z1 = (midden.getZ() + Gouw.KAMP_ZOEK + 1) >> 4;
        java.util.List<ChunkPos> geforceerd = new java.util.ArrayList<>();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (level.setChunkForced(x, z, true)) {
                    geforceerd.add(new ChunkPos(x, z));
                }
                level.getChunk(x, z);   // (now, not "some time": the test server runs its ticks faster than chunks get made)
            }
        }
        boolean[] gedaan = {false};
        AABB rond = new AABB(midden).inflate(Gouw.KAMP_ZOEK);
        helper.onEachTick(() -> {
            if (gedaan[0] || !level.isPositionEntityTicking(midden) || !Bezetting.geladen(level, rond)) {
                return;   // (the forced chunks are still on their way)
            }
            gedaan[0] = true;
            try {
                Kopieen.test(level, start);
                for (String id : ids) {
                    Bezetting.alleenIn(id, level.dimension());   // (the test server has no Guhmensie)
                }
                BlockPos bij = helper.absolutePos(new BlockPos(12, 3, 12));
                Bezetting.Geplaatst.get(level).vergeet(Gouw.KAMP, start);
                helper.assertTrue(Gouw.kampVan(level, start) == null && Gouw.kampBijPut(level, bij) == null, "no camp yet");
                Bezetting.controleer(level, bij);
                Gouw.Plek kamp = Gouw.kampVan(level, start);
                helper.assertTrue(kamp != null && kamp.hoek().equals(helper.absolutePos(new BlockPos(3, 2, 3))) && kamp.draai() == Rotation.NONE,
                        "the camp stands on the first free spot: " + kamp);
                helper.assertTrue(level.getBlockState(kamp.wereld(Gouw.TAFEL)).is(RingH1Feature.FEESTTAFEL.get()), "with its party table");
                Bezetting.bevestigAlles(level);
                Bezetting.controleer(level, bij);
                GuhNpcEntity guhdalf = een(helper, GuhNpcEntity.class, n -> n.getKind() == GuhNpcEntity.Kind.GUHDALF, "Guhdalf came to the camp");
                GuhEntity sam = een(helper, GuhEntity.class, Gouw::isSamThuis, "Sam-guh came to the camp");
                helper.assertTrue(guhdalf.blockPosition().equals(kamp.wereld(Gouw.GUHDALF)) && guhdalf.isInvulnerable() && Bezetting.isBezetting(guhdalf)
                        && Gouw.ROL.equals(guhdalf.roleData.getStringOr(NpcRollen.PLEK, "")), "Guhdalf on his spot");
                helper.assertTrue(sam.blockPosition().distManhattan(kamp.wereld(Gouw.SAM)) <= 2 && sam.getVariant() == GuhVariant.SAM_GUH && sam.isInvulnerable(),
                        "Sam-guh on his spot");
                Bezetting.bevestigAlles(level);
                helper.assertTrue(Bezetting.controleer(level, bij) == 0, "nobody comes twice");
                Gouw.Plek gelezen = Gouw.kampBij(level, guhdalf.blockPosition());
                helper.assertTrue(gelezen.hoek().equals(kamp.hoek()) && gelezen.draai() == kamp.draai(), "the camp is read back from Guhdalf's spot");
                BlockPos frame = Gouw.frame(level, bij);
                helper.assertTrue(frame != null && frame.equals(pos.offset(Gouw.FRAME)), "the frame of the pit: " + frame);
                helper.assertTrue(!Gouw.inPut(level, bij), "an old pit is no Knabbelgouw");
            } finally {
                for (String id : ids) {
                    Bezetting.alleenIn(id, ModDimensions.GUHMENSION);
                }
                Bezetting.Geplaatst.get(level).vergeet(Gouw.KAMP, start);
                Kopieen.testWissen(level);
                for (ChunkPos c : geforceerd) {
                    level.setChunkForced(c.x(), c.z(), false);
                }
                ruimOp(helper);
            }
            helper.succeed();
        });
    }

    /**
     * The worldgen side: guhs:knabbelgouw is a structure of the barbecueput set, only for biomes of the Guhmensie; the
     * barbecueput makes no big pits in the open any more, and exactly what it made under a ceiling; the template holds the
     * big pit, the camp and every quest prop.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringh1Wereldgen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        Structure gouw = Kopieen.structuur(level, Gouw.STRUCTUUR), put = Kopieen.structuur(level, Gouw.PUT);
        helper.assertTrue(gouw instanceof BarbecuePutStructure && put instanceof BarbecuePutStructure, "both are barbecueput structures");
        StructureSet set = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id(Gouw.PUT)));
        helper.assertTrue(set != null && set.structures().size() == 2 && set.structures().get(0).structure().value() == put
                && set.structures().get(1).structure().value() == gouw, "the set guhs:barbecueput holds the barbecueput and the Knabbelgouw");
        helper.assertTrue(access.lookupOrThrow(Registries.STRUCTURE_SET).containsKey(Guhs.id(Gouw.STRUCTUUR + "_gegarandeerd")), "the Knabbelgouw has a guaranteed copy");
        for (Holder<net.minecraft.world.level.biome.Biome> biome : gouw.biomes()) {
            helper.assertTrue(!biome.is(BarbecuePutStructure.CAVE_BIOMES), "never in the Guhbarbecuether: " + biome.getRegisteredName());
        }
        helper.assertTrue(gouw.biomes().size() >= 4, "in the land biomes of the Guhmensie");
        BarbecuePutStructure p = (BarbecuePutStructure) put, g = (BarbecuePutStructure) gouw;
        helper.assertTrue(templates(p.pool(true)).stream().anyMatch(e -> e.contains(Gouw.PUT_STUK)) && templates(p.pool(true)).size() == 3,
                "under a ceiling: big and small pits as before: " + templates(p.pool(true)));
        helper.assertTrue(templates(p.pool(false)).stream().noneMatch(e -> e.contains(Gouw.PUT_STUK)) && templates(p.pool(false)).size() == 2,
                "in the open: only small pits: " + templates(p.pool(false)));
        helper.assertTrue(templates(g.pool(false)).size() == 1 && templates(g.pool(true)).size() == 1 && g.keepClear() >= 41,
                "the Knabbelgouw is one template: " + templates(g.pool(false)) + " " + g.keepClear());
        Optional<StructureTemplate> template = level.getStructureManager().get(Guhs.id(Gouw.STRUCTUUR));
        helper.assertTrue(template.isPresent() && template.get().getSize().equals(new net.minecraft.core.Vec3i(81, 26, 81)), "the template of the Knabbelgouw");
        StructurePlaceSettings zo = new StructurePlaceSettings();
        StructureTemplate t = template.get();
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, zo, RingH1Feature.VUURWERKKIST.get()).size() == 1 && t.filterBlocks(BlockPos.ZERO, zo, RingH1Feature.FEESTTAFEL.get()).size() == 1
                && t.filterBlocks(BlockPos.ZERO, zo, RingH1Feature.PROVIAND.get()).size() == 3 && t.filterBlocks(BlockPos.ZERO, zo, RingH1Feature.SCHOORSTEENTJE.get()).size() == 5,
                "the quest props and five chimneys");
        var tafel = t.filterBlocks(BlockPos.ZERO, zo, RingH1Feature.FEESTTAFEL.get()).get(0).pos();
        helper.assertTrue(tafel.equals(Gouw.GOUW_KAMP.offset(Gouw.TAFEL)), "the camp of the Knabbelgouw stands where Java thinks: " + tafel);
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, zo, BarbecuetherFeature.GRILLKOOL.get()).size() >= 10, "the pit with its grillkool frame is in it");
        Optional<StructureTemplate> kamp = level.getStructureManager().get(Gouw.KAMP_TEMPLATE);
        helper.assertTrue(kamp.isPresent() && kamp.get().getSize().equals(new net.minecraft.core.Vec3i(Gouw.KAMP_MAAT.getX(), Gouw.KAMP_MAAT.getY(), Gouw.KAMP_MAAT.getZ())),
                "the camp template has the size Java knows");
        helper.succeed();
    }

    /** What a pool can give (the text of its elements: "Single[Left[guhs:barbecueput_groot]]"). */
    private static java.util.Set<String> templates(Holder<StructureTemplatePool> pool) {
        java.util.Set<String> uit = new java.util.TreeSet<>();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(7);
        for (int i = 0; i < 200; i++) {
            uit.add(pool.value().getRandomTemplate(random).toString());
        }
        return uit;
    }

    /** The scenes are written against the camp: every actor stays on the open side of the plate, whatever the script does. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringh1Scenes(GameTestHelper helper) {
        helper.assertTrue(Verteller.van(RingH1Feature.KAART) != null && Verteller.van(RingH1Feature.KAART).regels() == 4, "the narrator card");
        for (Cutscene s : List.of(RingH1Feature.AANKOMST, RingH1Feature.FEEST)) {
            helper.assertTrue(Cutscene.van(s.id()) == s && "ring_h1".equals(s.lijn()) && RingH1Feature.KAART.equals(s.kaart()), s.id() + " is registered for the chapter");
            for (String acteur : List.of(Cutscene.SPELER, "guhdalf", "sam", "gast", "gast2", "gast3", "gast4")) {
                if (s == RingH1Feature.AANKOMST && acteur.startsWith("gast")) {
                    continue;
                }
                for (int t = 0; t <= s.duur(); t += 5) {
                    Vec3 plek = s.plek(acteur, t);
                    // camp-local: Guhdalf's block is (10, 1, 6); the open side is x 4..11, z 8..10, plus his own spot
                    double x = plek.x + Gouw.GUHDALF.getX(), z = plek.z + Gouw.GUHDALF.getZ();
                    boolean opZijnPlek = acteur.equals("guhdalf") && Math.abs(plek.x - 0.5) < 0.01 && Math.abs(plek.z - 0.5) < 0.01;
                    helper.assertTrue(plek.y == 0 && (opZijnPlek || (x >= 4 && x <= 12.01 && z >= 7.99 && z <= 11.01)),
                            s.id() + ": " + acteur + " leaves the open side of the camp at tick " + t + ": " + x + ", " + z);
                }
            }
        }
        helper.succeed();
    }

    private RingH1GameTests() {
    }
}
