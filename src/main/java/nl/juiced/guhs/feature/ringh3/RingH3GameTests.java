package nl.juiced.guhs.feature.ringh3;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuePutStructure;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Herstel;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GegarandeerdPlacement;
import nl.juiced.guhs.world.grond.GrondPoolElement;

/**
 * bbq2 (ring-h3): De Mijnen van Knabbelmoria, server side. The test server has no Guhbarbecuether and the mine is far bigger
 * than a test room, so every test lays a FRAME over its room ({@link MijnProef#kopie}: "a copy of the mine stands so that this
 * part of the template lies in my room") and puts down the few blocks it needs. Mock players are not ticked by the server:
 * the tests post their tick event themselves ({@link #tik}). Template ringh3_test_kamer: a floor of 25 x 25.
 */
public final class RingH3GameTests {
    private static final String KAMER = "ringh3_test_kamer";
    private static final Verhaallijn LIJN = RingH3Feature.LIJN;

    private static ServerPlayer speler(GameTestHelper helper, Mijn m, BlockPos lokaal, int stap) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        zet(p, m, lokaal);
        Ring.OVERAL = true;
        Ring.lijn(1).begin(p);
        if (stap >= 0) {
            for (int n = 1; n <= 2; n++) {
                Ring.lijn(n).zet(p, Ring.lijn(n).stappen());
            }
            LIJN.begin(p);
            LIJN.zet(p, stap);
        }
        return p;
    }

    private static void zet(ServerPlayer p, Mijn m, BlockPos lokaal) {
        Vec3 plek = m.midden(lokaal);
        p.snapTo(plek.x, plek.y, plek.z);
        p.setOnGround(true);
        Mijn.vergeet(p.getUUID());
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Achtervolging.stop(p);
            Brug.vergeet(p.getUUID());
            Ring.wis(p);
            Mijn.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        MijnProef.weg(helper.getLevel());
    }

    /** What the server does for a real player every tick. */
    private static void tik(ServerPlayer p) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
    }

    /** A copy of the mine laid over the test room so that template block {@code lokaal} is the room's block (0, 2, 0) (the first you can stand in). */
    private static Mijn frame(GameTestHelper helper, BlockPos lokaal) {
        ServerLevel level = helper.getLevel();
        MijnProef.weg(level);
        BlockPos hoek = helper.absolutePos(new BlockPos(0, 2, 0));
        BlockPos a = helper.absolutePos(new BlockPos(0, 1, 0)), b = helper.absolutePos(new BlockPos(24, 9, 24));
        MijnProef.kopie(level, hoek.subtract(lokaal), Rotation.NONE, BoundingBox.fromCorners(a, b));
        Mijn m = Mijn.bij(level, helper.absolutePos(new BlockPos(12, 3, 12)));
        helper.assertTrue(m != null && m.wereld(lokaal).equals(hoek), "the frame lies over the room");
        return m;
    }

    private static void plaatsDeur(Mijn m, Deuren.Deur d) {
        StructureTemplate t = m.level().getStructureManager().get(d.template()).orElseThrow();
        BlockPos hoek = m.wereld(d.doos.hoek());
        t.placeInWorld(m.level(), hoek, hoek, new StructurePlaceSettings(), m.level().getRandom(), Block.UPDATE_ALL);
    }

    private static void sluit(Mijn m) {
        Herstel.verschuif(m.level(), Deuren.OPEN_TICKS + 1);
        Herstel.verwerk(m.level());
    }

    // =====================================================================================================================

    /**
     * The gate: "zeg njeg en treed binnen". The word in the chat near the gate, only for a player who is at that step; the
     * inscription with its three answers; the door closes again and then knows who solved it; nobody is ever locked in.
     */
    @GuhTest(template = KAMER, batch = "ringh3_poort")
    public static void ringh3PoortZegNjeg(GameTestHelper helper) {
        Mijn m = frame(helper, new BlockPos(24, Plekken.BOVEN, 50));
        plaatsDeur(m, Deuren.Deur.WEST);
        ServerPlayer p = speler(helper, m, new BlockPos(33, Plekken.BOVEN, 62), 1);
        ServerPlayer ver = speler(helper, m, new BlockPos(48, Plekken.BOVEN, 74), 1);
        ServerPlayer nieuw = speler(helper, m, new BlockPos(32, Plekken.BOVEN, 63), -1);
        try {
            helper.assertTrue(Bescherming.beschermd(m.level(), m.wereld(Plekken.DEUR_WEST.hoek())), "the mine is a protected building");
            helper.assertTrue(!Deuren.isOpen(m, Deuren.Deur.WEST), "the gate is closed");
            tik(p);
            helper.assertTrue(!Deuren.isOpen(m, Deuren.Deur.WEST) && LIJN.stap(p) == 1, "walking up to it does nothing");
            Raadsels.chat(p, "Vads! Sesam open u!");
            Raadsels.chat(ver, "njeg");
            Raadsels.chat(nieuw, "njeg njeg njeg");
            helper.assertTrue(!Deuren.isOpen(m, Deuren.Deur.WEST) && LIJN.stap(p) == 1 && LIJN.stap(ver) == 1 && LIJN.stap(nieuw) == 0,
                    "the wrong word, the right word too far away, the right word by somebody whose story isn't here yet: nothing");
            Raadsels.chat(p, "ik zeg gewoon NJEG hoor");
            helper.assertTrue(Deuren.isOpen(m, Deuren.Deur.WEST) && LIJN.stap(p) == 2 && LIJN.stap(nieuw) == 0, "njeg: the gate slides open, for the one who said it");
            helper.assertTrue(Herstel.wacht(m.level(), m.wereld(Plekken.DEUR_WEST.hoek())), "and it will close again by itself");
            sluit(m);
            helper.assertTrue(!Deuren.isOpen(m, Deuren.Deur.WEST) && m.level().getBlockState(m.wereld(new BlockPos(38, Plekken.BOVEN + 3, 62))).is(
                    m.level().registryAccess().lookupOrThrow(Registries.BLOCK).getValue(Guhs.id("gatenkaas_stenen"))), "closed again: the slab with its star is back");
            // the inscription: a wrong answer, then the right one
            zet(ver, m, new BlockPos(34, Plekken.BOVEN, 61));
            Raadsels.poortKlik(ver, m);
            Praat.antwoord(ver, null, 1);
            helper.assertTrue(LIJN.stap(ver) == 1 && LIJN.teller(ver, Raadsels.POORT_FOUT) == 1 && !Deuren.isOpen(m, Deuren.Deur.WEST), "'Vads!' is not the word");
            Raadsels.poortKlik(ver, m);
            Praat.antwoord(ver, null, Raadsels.ANTWOORD_NJEG);
            helper.assertTrue(LIJN.stap(ver) == 2 && Deuren.isOpen(m, Deuren.Deur.WEST), "'Njeg.' is");
            sluit(m);
            // afterwards the gate opens by itself for who solved it; never from outside for who didn't; always from inside
            zet(nieuw, m, new BlockPos(36, Plekken.BOVEN, 62));
            tik(nieuw);
            helper.assertTrue(!Deuren.isOpen(m, Deuren.Deur.WEST), "closed for a player who did not solve it");
            zet(nieuw, m, new BlockPos(40, Plekken.BOVEN, 62));
            tik(nieuw);
            helper.assertTrue(Deuren.isOpen(m, Deuren.Deur.WEST), "but nobody is locked in: from the inside it opens for everybody");
            sluit(m);
            zet(nieuw, m, new BlockPos(28, Plekken.BOVEN, 52));
            zet(p, m, new BlockPos(36, Plekken.BOVEN, 62));
            tik(p);
            helper.assertTrue(Deuren.isOpen(m, Deuren.Deur.WEST), "and from the outside for who knows the word");
            Herstel.verschuif(m.level(), Deuren.OPEN_TICKS - 20);
            tik(p);
            Herstel.verschuif(m.level(), 40);
            Herstel.verwerk(m.level());
            helper.assertTrue(Deuren.isOpen(m, Deuren.Deur.WEST), "it never closes on a player who stands in it");
        } finally {
            sluit(m);
            weg(helper, p, ver, nieuw);
        }
        helper.succeed();
    }

    /** The levers: the order of the rhyme, per player; a wrong one starts over; the portcullis opens for who solved it. */
    @GuhTest(template = KAMER, batch = "ringh3_hefbomen")
    public static void ringh3HefbomenInVolgorde(GameTestHelper helper) {
        Mijn m = frame(helper, new BlockPos(30, Plekken.MIDDEL, 24));
        ServerLevel level = m.level();
        plaatsDeur(m, Deuren.Deur.VALHEK);
        for (int i = 0; i < Plekken.HENDELS.size(); i++) {
            level.setBlock(m.wereld(Plekken.HENDELS.get(i)), RingH3Feature.HENDEL.get().defaultBlockState().setValue(RingH3Blocks.Hendel.NR, i), Block.UPDATE_ALL);
        }
        ServerPlayer a = speler(helper, m, new BlockPos(42, Plekken.MIDDEL, 33), 2), b = speler(helper, m, new BlockPos(44, Plekken.MIDDEL, 33), 2);
        ServerPlayer c = speler(helper, m, new BlockPos(46, Plekken.MIDDEL, 33), 1);
        try {
            int[] orde = Plekken.HENDEL_VOLGORDE;
            // a real click on a lever inside the protected building: the block moves and tells the riddle
            BlockPos eerste = m.wereld(Plekken.HENDELS.get(orde[0]));
            a.gameMode.useItemOn(a, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(eerste), net.minecraft.core.Direction.SOUTH,
                    eerste, false));
            helper.assertTrue(level.getBlockState(eerste).getValue(RingH3Blocks.Hendel.OM) && LIJN.teller(a, Raadsels.HEFBOOM) == 1,
                    "a click pulls the lever (the building is protected, its levers work) and counts for the player");
            Raadsels.hendel(b, m.wereld(Plekken.HENDELS.get(orde[0])), orde[0]);
            Raadsels.hendel(c, m.wereld(Plekken.HENDELS.get(orde[1])), orde[1]);
            helper.assertTrue(LIJN.teller(a, Raadsels.HEFBOOM) == 1 && LIJN.teller(b, Raadsels.HEFBOOM) == 1 && LIJN.teller(c, Raadsels.HEFBOOM) == 0,
                    "everybody has an order of their own; a player who is not at this step moves nothing");
            Raadsels.hendel(b, m.wereld(Plekken.HENDELS.get(orde[3])), orde[3]);
            helper.assertTrue(LIJN.teller(b, Raadsels.HEFBOOM) == 0 && LIJN.teller(b, Raadsels.HEFBOOM_FOUT) == 1 && LIJN.teller(a, Raadsels.HEFBOOM) == 1,
                    "a wrong lever: that player starts over, nobody else");
            for (int i = 1; i < orde.length; i++) {
                helper.assertTrue(!Deuren.isOpen(m, Deuren.Deur.VALHEK) && LIJN.stap(a) == 2, "not yet");
                Raadsels.hendel(a, m.wereld(Plekken.HENDELS.get(orde[i])), orde[i]);
            }
            helper.assertTrue(Deuren.isOpen(m, Deuren.Deur.VALHEK) && LIJN.stap(a) == 3 && LIJN.stap(b) == 2, "worst, kaas, saus, knabbel: the portcullis opens, a is on");
            sluit(m);
            helper.assertTrue(!Deuren.isOpen(m, Deuren.Deur.VALHEK) && level.getBlockState(m.wereld(Plekken.DEUR_VALHEK.hoek())).is(
                    level.registryAccess().lookupOrThrow(Registries.BLOCK).getValue(Guhs.id("roosterijzer_tralies"))), "and comes down again for the next player");
            for (int nr : orde) {
                Raadsels.hendel(b, m.wereld(Plekken.HENDELS.get(nr)), nr);
            }
            helper.assertTrue(LIJN.stap(b) == 3 && Deuren.isOpen(m, Deuren.Deur.VALHEK), "b solves it too, the same way");
            sluit(m);
            zet(c, m, new BlockPos(33, Plekken.MIDDEL, 35));
            tik(c);
            helper.assertTrue(!Deuren.isOpen(m, Deuren.Deur.VALHEK), "shut for c, who didn't");
            zet(a, m, new BlockPos(33, Plekken.MIDDEL, 35));
            tik(a);
            helper.assertTrue(Deuren.isOpen(m, Deuren.Deur.VALHEK), "open for a, who did");
        } finally {
            sluit(m);
            weg(helper, a, b, c);
        }
        helper.succeed();
    }

    /** Gimguh's door: three knocks on the rune of the cheese; what Gimguh and Araguh say; a rune stone at home turns to the next sign. */
    @GuhTest(template = KAMER, batch = "ringh3_runen")
    public static void ringh3RunenEnRollen(GameTestHelper helper) {
        Mijn m = frame(helper, new BlockPos(8, Plekken.MIDDEL, 24));
        ServerLevel level = m.level();
        plaatsDeur(m, Deuren.Deur.GEHEIM);
        int goed = -1, fout = -1;
        for (int i = 0; i < Plekken.RUNEN.size(); i++) {
            level.setBlock(m.wereld(Plekken.RUNEN.get(i)), RingH3Feature.RUNE.get().defaultBlockState().setValue(RingH3Blocks.Rune.TEKEN, Plekken.RUNE_TEKENS[i]),
                    Block.UPDATE_ALL);
            if (Plekken.RUNE_TEKENS[i] == Plekken.RUNE_GOED) {
                goed = i;
            } else {
                fout = i;
            }
        }
        ServerPlayer p = speler(helper, m, new BlockPos(16, Plekken.MIDDEL, 35), 4), eind = speler(helper, m, new BlockPos(18, Plekken.MIDDEL, 30), 6);
        GuhNpcEntity gimguh = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED), araguh = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        try {
            helper.assertTrue(goed >= 0 && fout >= 0, "one of the six stones is the cheese");
            BlockPos kaas = m.wereld(Plekken.RUNEN.get(goed)), anders = m.wereld(Plekken.RUNEN.get(fout));
            Raadsels.rune(p, kaas, Plekken.RUNE_GOED);
            Raadsels.rune(p, kaas, Plekken.RUNE_GOED);
            Raadsels.rune(p, anders, Plekken.RUNE_TEKENS[fout]);
            helper.assertTrue(LIJN.teller(p, Raadsels.KLOP) == 0 && LIJN.teller(p, Raadsels.KLOP_FOUT) == 1 && !Deuren.isOpen(m, Deuren.Deur.GEHEIM),
                    "two knocks and then another rune: start over");
            Raadsels.rune(eind, kaas, Plekken.RUNE_GOED);
            helper.assertTrue(LIJN.teller(eind, Raadsels.KLOP) == 0 && Deuren.isOpen(m, Deuren.Deur.GEHEIM), "who is past it knocks for nothing: the door just opens for them");
            sluit(m);
            for (int i = 0; i < Plekken.RUNE_KLOPPEN; i++) {
                helper.assertTrue(LIJN.stap(p) == 4, "not yet");
                Raadsels.rune(p, kaas, Plekken.RUNE_GOED);
            }
            helper.assertTrue(LIJN.stap(p) == 5 && Deuren.isOpen(m, Deuren.Deur.GEHEIM), "three knocks on the cheese: Gimguh's door");
            // the characters
            gimguh.setKind(GuhNpcEntity.Kind.GIMGUH);
            gimguh.roleData.putString(NpcRollen.PLEK, "ringh3_gang");
            araguh.setKind(GuhNpcEntity.Kind.ARAGUH);
            araguh.roleData.putString(NpcRollen.PLEK, "ringh3_buiten");
            Vec3 plek = m.midden(new BlockPos(20, Plekken.MIDDEL, 36));
            gimguh.snapTo(plek.x, plek.y, plek.z);
            araguh.snapTo(plek.x + 2, plek.y, plek.z);
            level.addFreshEntity(gimguh);
            level.addFreshEntity(araguh);
            LIJN.wis(p);
            LIJN.begin(p);
            LIJN.zet(p, 4);
            NpcRollen.van(gimguh).talk(gimguh, p);
            helper.assertTrue(LIJN.teller(p, "gimguh") == 1 && LIJN.stap(p) == 4, "Gimguh says what to knock on (and says more the next time)");
            NpcRollen.van(araguh).talk(araguh, p);
            helper.assertTrue(LIJN.stap(p) == 4, "Araguh ends nothing for a player who isn't out yet");
            NpcRollen.van(araguh).talk(araguh, eind);
            helper.assertTrue(LIJN.klaar(eind) && GuhQuests.count(eind, RingH3Feature.RUNE_ITEM.get()) == 4 && Ring.bereikt(eind, 4),
                    "Araguh outside: the chapter is done, four rune stones, chapter 4 is reached");
            NpcRollen.van(araguh).talk(araguh, eind);
            helper.assertTrue(GuhQuests.count(eind, RingH3Feature.RUNE_ITEM.get()) == 4, "once");
            // a rune stone at home (no mine around it): a knock turns it to the next sign
            MijnProef.weg(level);
            Mijn.OVERAL = true;
            Raadsels.rune(p, kaas, Plekken.RUNE_GOED);
            helper.assertTrue(level.getBlockState(kaas).getValue(RingH3Blocks.Rune.TEKEN) == Plekken.RUNE_GOED + 1, "the next sign");
            helper.assertTrue(RingH3Feature.RUNE.get().defaultBlockState().getValue(RingH3Blocks.Rune.TEKEN) == Plekken.RUNE_NJEG
                    && RingH3Feature.RUNE.get().defaultBlockState().getLightEmission(level, kaas) > 0, "the keepsake is the njeg rune, and it glows");
        } finally {
            gimguh.discard();
            araguh.discard();
            weg(helper, p, eind);
        }
        helper.succeed();
    }

    /** Brokkelsteen: cracks under a player, is gone a moment later, grows back by itself. */
    @GuhTest(template = KAMER, batch = "ringh3_brokkel", timeoutTicks = 300)
    public static void ringh3BrokkelsteenValtEnKomtTerug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(6, 3, 6));
        level.setBlock(pos, RingH3Feature.BROKKELSTEEN.get().defaultBlockState(), Block.UPDATE_ALL);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(pos.getX() + 20.5, pos.getY() + 1, pos.getZ() + 0.5);
        BlockState heel = level.getBlockState(pos);
        helper.assertTrue(!heel.getCollisionShape(level, pos).isEmpty() && heel.getDestroySpeed(level, pos) < 0, "whole: you can walk on it, you can't break it");
        heel.getBlock().stepOn(level, pos, heel, p);
        helper.assertTrue(level.getBlockState(pos).getValue(RingH3Blocks.Brokkelsteen.STAAT) == 1 && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(),
                "a player steps on it: it cracks, and still carries");
        long start = level.getGameTime();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(pos).getValue(RingH3Blocks.Brokkelsteen.STAAT) == 2, "cracking..."))
                .thenExecute(() -> {
                    long ticks = level.getGameTime() - start;
                    helper.assertTrue(ticks >= RingH3Blocks.Brokkelsteen.BARST_TICKS - 1 && ticks <= RingH3Blocks.Brokkelsteen.BARST_TICKS + 6, "gone after about " + RingH3Blocks.Brokkelsteen.BARST_TICKS + " ticks: " + ticks);
                    helper.assertTrue(level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(), "gone: nothing to stand on");
                })
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(pos).getValue(RingH3Blocks.Brokkelsteen.STAAT) == 0, "growing back..."))
                .thenExecute(() -> {
                    long ticks = level.getGameTime() - start;
                    helper.assertTrue(ticks >= RingH3Blocks.Brokkelsteen.BARST_TICKS + RingH3Blocks.Brokkelsteen.WEG_TICKS - 2, "back after the wait: " + ticks);
                    helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
                })
                .thenSucceed();
    }

    /** Falling into the chasm costs nothing: back at the rest fire, unharmed; in the mine a fall never hurts. */
    @GuhTest(template = KAMER, batch = "ringh3_val")
    public static void ringh3VallenKostNiks(GameTestHelper helper) {
        // (the room is the bottom of the chasm: template y 6 is the room's floor)
        Mijn m = frame(helper, new BlockPos(50, 6, 2));
        ServerLevel level = m.level();
        ServerPlayer p = speler(helper, m, new BlockPos(60, 6, 14), 5), gast = speler(helper, m, new BlockPos(62, 6, 14), 7);
        ServerPlayer derde = speler(helper, m, new BlockPos(64, 9, 16), 5);
        try {
            // (a player whose client never said "loaded" can't be hurt at all: this one is loaded, so the mine is what protects them)
            derde.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            derde.hurtServer(level, level.damageSources().fall(), 8f);
            helper.assertTrue(derde.getHealth() == derde.getMaxHealth(), "a fall never hurts in the mine");
            derde.hurtServer(level, level.damageSources().generic(), 2f);
            helper.assertTrue(derde.getHealth() < derde.getMaxHealth(), "(other damage is not ours to cancel: the check above means something)");
            float leven = p.getHealth();
            p.getInventory().add(new ItemStack(Blocks.COBBLESTONE, 7));
            Vec3 vuur = m.midden(new BlockPos(70, 6, 20));
            Ring.rustpunt(p, vuur, 90f);
            p.fallDistance = 30;
            helper.assertTrue(Plekken.KLOOF.binnen(m.lokaal(p.blockPosition())), "the player is in the chasm");
            tik(p);
            helper.assertTrue(p.position().distanceTo(vuur) < 0.1 && p.getHealth() == leven && p.fallDistance == 0
                    && GuhQuests.count(p, Blocks.COBBLESTONE.asItem()) == 7, "back at the rest fire: unharmed, nothing lost");
            // a visitor whose rest point is not in this mine lands at the mouth of Gimguh's passage
            tik(gast);
            helper.assertTrue(gast.position().distanceTo(m.midden(Plekken.HAL_INGANG)) < 0.1 && gast.getHealth() == gast.getMaxHealth(), "a visitor: at the hall's entrance");
        } finally {
            weg(helper, p, gast, derde);
        }
        helper.succeed();
    }

    /**
     * The Barbecuerog: everybody's own, only shoves. He wakes, rises, walks; whoever he reaches with his whip is back at their
     * rest fire, unharmed, and he is gone; he can't be hurt; nobody else's game knows about him; he leaves when the step is over.
     */
    @GuhTest(template = KAMER, batch = "ringh3_rog", timeoutTicks = 600)
    public static void ringh3BarbecuerogDuwtAlleen(GameTestHelper helper) {
        Mijn m = frame(helper, new BlockPos(20, Plekken.DIEP, 2));
        ServerLevel level = m.level();
        ServerPlayer p = speler(helper, m, new BlockPos(38, Plekken.DIEP, 14), 5), ander = speler(helper, m, new BlockPos(40, Plekken.DIEP, 20), 5);
        ServerPlayer klaar = speler(helper, m, new BlockPos(30, Plekken.DIEP, 20), 5);
        float leven = p.getHealth();
        Vec3 vuur = m.midden(new BlockPos(24, Plekken.DIEP, 22));
        Ring.rustpunt(p, vuur, 0f);
        BarbecuerogEntity rog = Achtervolging.start(p, m);
        helper.assertTrue(rog != null && Achtervolging.start(p, m) == rog && Achtervolging.van(p) == rog, "one Barbecuerog per player");
        // (the real chase starts 30 blocks away in the Diepe Poort: here he sleeps at the near end of the room and walks 8 blocks)
        Vec3 slaapt = m.midden(new BlockPos(24, Plekken.DIEP, 14)), einde = m.midden(new BlockPos(32, Plekken.DIEP, 14));
        rog.snapTo(slaapt.x, slaapt.y, slaapt.z);
        rog.jaag(p, einde);
        helper.assertTrue(rog.staat() == BarbecuerogEntity.DONKER && rog.fase() == BarbecuerogEntity.SLAAPT && !rog.getType().canSerialize(), "he sleeps in the dark; never saved");
        helper.assertTrue(p.getUUID().equals(Zicht.eigenaar(rog)) && Zicht.magZien(p, rog) && !Zicht.magZien(ander, rog), "only his own player's game knows about him");
        rog.hurtServer(level, level.damageSources().playerAttack(p), 50f);
        helper.assertTrue(rog.getHealth() == rog.getMaxHealth(), "nothing hurts him");
        BarbecuerogEntity tweede = Achtervolging.start(klaar, m);
        tweede.snapTo(slaapt.x, slaapt.y, slaapt.z + 8);
        LIJN.zet(klaar, 6);
        long start = level.getGameTime();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tweede == null || tweede.isRemoved(), "a player whose step is over: their Barbecuerog leaves"))
                .thenWaitUntil(() -> helper.assertTrue(rog.fase() >= BarbecuerogEntity.JAAGT, "waking, rising..."))
                .thenExecute(() -> {
                    long ticks = level.getGameTime() - start;
                    helper.assertTrue(ticks >= BarbecuerogEntity.OPKOMST_TICKS && rog.staat() == BarbecuerogEntity.LOOPT, "he took his time to rise (" + ticks + "), now he walks");
                    helper.assertTrue(p.getHealth() == leven && p.position().distanceTo(vuur) > 5, "the player is still where they stood");
                })
                .thenWaitUntil(() -> helper.assertTrue(rog.isRemoved(), "walking, the whip..."))
                .thenExecute(() -> {
                    helper.assertTrue(p.position().distanceTo(vuur) < 0.1 && p.getHealth() == leven, "caught: back at the rest fire, unharmed");
                    helper.assertTrue(Achtervolging.van(p) == null && LIJN.stap(p) == 5, "he is gone; the step is still the player's to do");
                    helper.assertTrue(ander.getHealth() == ander.getMaxHealth() && ander.position().distanceTo(m.midden(new BlockPos(40, Plekken.DIEP, 20))) < 0.1,
                            "somebody else, standing right there, is not his business");
                    weg(helper, p, ander, klaar);
                })
                .thenSucceed();
    }

    /** The story from zone to zone: the narrator card on the forecourt, the bucket, the hall wakes him, the bridge scene; the bridge that is broken for one. */
    @GuhTest(template = KAMER, batch = "ringh3_verhaal", timeoutTicks = 400)
    public static void ringh3VerhaalVanPoortTotBrug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Mijn plein = frame(helper, new BlockPos(20, Plekken.BOVEN, 50));
        ServerPlayer p = speler(helper, plein, new BlockPos(30, Plekken.BOVEN, 62), 0);
        ServerPlayer vroeg = speler(helper, plein, new BlockPos(31, Plekken.BOVEN, 62), -1);
        helper.assertTrue(!LIJN.aanDeBeurt(vroeg) && LIJN.aanDeBeurt(p) && !Sluiers.open(vroeg, Mijn.STRUCTUUR) && Sluiers.open(p, Mijn.STRUCTUUR),
                "the mine is behind Guhdalfs sluier until chapter 2 is done");
        tik(vroeg);
        helper.assertTrue(LIJN.stap(vroeg) == 0 && !Cutscenes.bezig(vroeg), "nothing starts for a player whose story isn't here");
        tik(p);
        helper.assertTrue(Cutscenes.bezig(p) && LIJN.begonnen(p), "arriving on the forecourt: the narrator card");
        helper.startSequence()
                .thenWaitUntil(() -> {
                    tik(p);
                    helper.assertTrue(LIJN.stap(p) == 1 && Verteller.gezien(p, RingH3Feature.KAART) && !Cutscenes.bezig(p), "reading the card...");
                })
                .thenExecute(() -> {
                    Mijn put = frame(helper, new BlockPos(10, Plekken.MIDDEL, 24));
                    LIJN.zet(p, 3);
                    zet(p, put, new BlockPos(26, Plekken.MIDDEL, 35));
                })
                .thenWaitUntil(() -> {
                    tik(p);
                    helper.assertTrue(LIJN.stap(p) == 4 && Cutscenes.gezien(p, Scenes.EMMER.id()) && !Cutscenes.bezig(p), "the well room: the scene with the bucket...");
                })
                .thenExecute(() -> {
                    Mijn hal = frame(helper, new BlockPos(30, Plekken.DIEP, 2));
                    LIJN.zet(p, 5);
                    zet(p, hal, new BlockPos(40, Plekken.DIEP, 14));
                })
                .thenWaitUntil(() -> {
                    tik(p);
                    helper.assertTrue(Achtervolging.van(p) != null, "the great hall: his Barbecuerog wakes...");
                })
                .thenExecute(() -> {
                    Mijn oever = frame(helper, new BlockPos(66, Plekken.DIEP, 2));
                    zet(p, oever, new BlockPos(80, Plekken.DIEP, 14));
                    Brug.breek(p, oever, 5);
                })
                .thenWaitUntil(() -> {
                    tik(p);
                    helper.assertTrue(LIJN.stap(p) == 6 && Cutscenes.gezien(p, Scenes.BRUG.id()) && !Cutscenes.bezig(p), "the east bank: the bridge scene...");
                })
                .thenExecute(() -> {
                    Mijn oever = Mijn.van(p);
                    helper.assertTrue(Achtervolging.van(p) == null, "the chase is over");
                    helper.assertTrue(Brug.isKapot(p) && !Brug.isKapot(vroeg), "the bridge is broken for who saw it break, and only for them");
                    BlockPos dek = oever.wereld(new BlockPos(70, Plekken.DIEP - 1, 14));
                    level.setBlock(dek, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    zet(p, oever, new BlockPos(73, Plekken.DIEP, 14));
                    p.setDeltaMovement(Vec3.ZERO);
                    // (the after-care of the scene: nobody shoves a player who just watched; a moment later the gap does)
                    Brug.heel(p);
                    helper.assertTrue(!Brug.isKapot(p) && level.getBlockState(dek).is(Blocks.STONE), "whole again; the real bridge never changed");
                    weg(helper, p, vroeg);
                })
                .thenSucceed();
    }

    /**
     * The template is what the Java side thinks it is (levers, rune stones, rest fires, the Brokkelpad, the doors, the cast),
     * the scenes and the questline are registered, and the worldgen data: a cave building with exactly one guaranteed copy in
     * new terrain whose pool says where the ground is, nothing spawns in it.
     */
    @GuhTest(template = "empty", batch = "ringh3")
    public static void ringh3SjabloonEnWereld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate mijn = level.getStructureManager().get(Guhs.id(Mijn.STRUCTUUR)).orElse(null);
        helper.assertTrue(mijn != null && mijn.getSize().equals(Plekken.MAAT), "the template has the size Plekken says");
        StructurePlaceSettings zo = new StructurePlaceSettings();
        List<StructureTemplate.StructureBlockInfo> hendels = mijn.filterBlocks(BlockPos.ZERO, zo, RingH3Feature.HENDEL.get());
        helper.assertTrue(hendels.size() == Plekken.HENDELS.size(), "four levers");
        for (StructureTemplate.StructureBlockInfo info : hendels) {
            helper.assertTrue(Plekken.HENDELS.indexOf(info.pos()) == info.state().getValue(RingH3Blocks.Hendel.NR), "lever " + info.pos() + " is where Plekken says");
        }
        List<StructureTemplate.StructureBlockInfo> runen = mijn.filterBlocks(BlockPos.ZERO, zo, RingH3Feature.RUNE.get());
        int stenen = 0, schrift = 0;
        for (StructureTemplate.StructureBlockInfo info : runen) {
            int i = Plekken.RUNEN.indexOf(info.pos());
            if (i >= 0) {
                stenen++;
                helper.assertTrue(info.state().getValue(RingH3Blocks.Rune.TEKEN) == Plekken.RUNE_TEKENS[i], "rune stone " + i);
            }
            if (info.state().getValue(RingH3Blocks.Rune.TEKEN) == Plekken.RUNE_NJEG) {
                schrift++;
            }
        }
        helper.assertTrue(stenen == Plekken.RUNEN.size() && schrift >= 8, "six rune stones round Gimguh's door, an inscription over the gate: " + stenen + ", " + schrift);
        helper.assertTrue(mijn.filterBlocks(BlockPos.ZERO, zo, nl.juiced.guhs.feature.ring.RingFeature.RUSTVUUR.get()).size() == 4, "four rest fires");
        helper.assertTrue(mijn.filterBlocks(BlockPos.ZERO, zo, RingH3Feature.BROKKELSTEEN.get()).size() >= 40, "a Brokkelpad");
        List<StructureTemplate.StructureBlockInfo> jigsaws = mijn.filterBlocks(BlockPos.ZERO, zo, Blocks.JIGSAW);
        helper.assertTrue(jigsaws.size() == 1 && jigsaws.get(0).pos().getY() == 0, "one centre jigsaw, in layer 0");
        for (Deuren.Deur d : Deuren.Deur.values()) {
            StructureTemplate deur = level.getStructureManager().get(d.template()).orElse(null);
            helper.assertTrue(deur != null && deur.getSize().getX() == d.doos.x1() - d.doos.x0() + 1 && deur.getSize().getY() == d.doos.y1() - d.doos.y0() + 1
                    && deur.getSize().getZ() == d.doos.z1() - d.doos.z0() + 1, "the door " + d.naam + " has its own template");
        }
        // the cast: every character of Plekken stands in the template with its id, and only for its steps
        int cast = 0;
        for (Tag t : mijn.save(new CompoundTag()).getListOrEmpty("entities")) {
            CompoundTag e = t.asCompound().orElse(new CompoundTag());
            CompoundTag nbt = e.getCompoundOrEmpty("nbt");
            String id = nbt.getCompoundOrEmpty("NeoForgeData").getStringOr(Bezetting.TAG, "");
            var pos = e.getListOrEmpty("blockPos");
            for (Plekken.Rol rol : Plekken.CAST) {
                if (rol.id().equals(id)) {
                    cast++;
                    helper.assertTrue(pos.getIntOr(0, -1) == rol.plek().getX() && pos.getIntOr(1, -1) == rol.plek().getY() && pos.getIntOr(2, -1) == rol.plek().getZ()
                            && nbt.getStringOr("Kind", "").equals(rol.kind())
                            && nbt.getCompoundOrEmpty("NeoForgeData").getStringOr(Zicht.BIJ, "").equals("ring_h3:" + rol.van() + "-" + rol.tot()), id + " is as Plekken says");
                }
            }
        }
        helper.assertTrue(cast == Plekken.CAST.size(), "the whole cast: " + cast);
        // the story
        helper.assertTrue(LIJN.stappen() == 7 && "ring_h2".equals(LIJN.na()) && Ring.lijn(3) == LIJN && Sluiers.structuren().contains(Mijn.STRUCTUUR), "the questline and the sluier");
        helper.assertTrue(Cutscene.van("ringh3_brug") == Scenes.BRUG && Cutscene.van("ringh3_emmer") == Scenes.EMMER && Scenes.BRUG.duur() > 1500
                && Scenes.BRUG_BREEKT < Scenes.BRUG.duur() && Scenes.FLITSEN.length >= 3 && Scenes.BRUG.acteur("rog") != null, "the two scenes");
        helper.assertTrue(Scenes.BRUG.zinnen().stream().filter(z -> z.key().startsWith("you_")).count() == 4, "Guhdalf's line comes word by word");
        // worldgen
        helper.assertTrue(Kopieen.structuur(level, Mijn.STRUCTUUR) instanceof BarbecuePutStructure, "a cave building");
        var pool = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getValue(Guhs.id(Mijn.STRUCTUUR + "/start"));
        helper.assertTrue(pool != null, "its start pool");
        StructurePoolElement element = pool.getRandomTemplate(level.getRandom());
        helper.assertTrue(element instanceof GrondPoolElement g && g.groundLevelDelta() == Plekken.G + 1, "the pool says where the ground is: " + element);
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        var gegarandeerd = sets.getValue(Guhs.id(Mijn.STRUCTUUR + "_gegarandeerd"));
        helper.assertTrue(sets.getValue(Guhs.id(Mijn.STRUCTUUR)) == null && gegarandeerd != null
                && gegarandeerd.placement() instanceof GegarandeerdPlacement g && g.alleenNieuw(), "exactly one copy per world, in new terrain, no random spread");
        helper.assertTrue(Kopieen.structuur(level, Mijn.STRUCTUUR).spawnOverrides().size() >= 2, "nothing spawns in the halls");
        helper.succeed();
    }
}
