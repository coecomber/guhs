package nl.juiced.guhs.gametest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioLevel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhriobeloning.GuhrioBeloningFeature;
import nl.juiced.guhs.feature.guhriow1.Binnentuin;
import nl.juiced.guhs.feature.guhriow1.GuhrioW1Feature;
import nl.juiced.guhs.feature.guhriow2.GuhrioW2;
import nl.juiced.guhs.feature.guhriow2.GuhrioW2Feature;
import nl.juiced.guhs.feature.guhriow3.GroteNetherMikaEntity;
import nl.juiced.guhs.feature.guhriow3.GuhrioW3Feature;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2, written at the merge of the four Super Guhrio slices (guhrio-w1, guhrio-w2, guhrio-w3, guhrio-beloning): the seams
 * BETWEEN them that no slice could test alone (batch "guhriosamen"). The slices were built in parallel on the engine, each
 * with the others' slots still holding the engine's practice lanes and placeholder NPCs.
 * <ul>
 *     <li>the castle's sixteen tiles hold all of it at once: six levels and the duel with their own start blocks and
 *     level files, every slice's own blocks, the forecourt's board, and every warp pipe of world 2 leads to a level
 *     that is really there;</li>
 *     <li>Pad-guh's "de prinses is in een ander kasteeldeel" at the flagpole of 1-2: the Pad-guh of the forecourt
 *     (guhrio-beloning) calls it at the flagpole, the Pad-guh in the lane (guhrio-w1) a moment later, and world 1 leaves
 *     its own chat copy out by reading the other's once-mark BY NAME - the name must be the one that is really written;
 *     the Pad-guh in the lane has his own role, not the shop;</li>
 *     <li>the end of the duel as world 3 calls it is what the rewards wait for: the princess's crown and cake, and
 *     Guhshi may come along.</li>
 * </ul>
 * Mock players are not ticked by the server: the tests call the engine's tick themselves.
 */
public class GuhrioSamenGameTests {
    private static final String BATCH = "guhriosamen";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            GuhrioSpel.stop(p, GuhrioSpel.Einde.WEG);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Marks levels as finished in the engine's own saved data (what a flagpole does). */
    private static void gehaald(ServerPlayer p, String... levels) {
        CompoundTag gehaald = GuhrioSpel.spaar(p).getCompoundOrEmpty("Gehaald");
        for (String level : levels) {
            gehaald.putBoolean(level, true);
        }
        GuhrioSpel.spaar(p).put("Gehaald", gehaald);
    }

    private static void forceer(ServerLevel level, BlockPos hoek, StructureTemplate template, boolean aan) {
        for (int cx = hoek.getX() >> 4; cx <= (hoek.getX() + template.getSize().getX()) >> 4; cx++) {
            for (int cz = hoek.getZ() >> 4; cz <= (hoek.getZ() + template.getSize().getZ()) >> 4; cz++) {
                level.setChunkForced(cx, cz, aan);
            }
        }
    }

    /** The block of cell (s, row) of a level of world 1 whose test template stands at {@code hoek}. */
    private static BlockPos cel(BlockPos hoek, int s, int rij) {
        return hoek.offset(1 + s, 2 + rij, 1);
    }

    private static void zet(ServerPlayer p, BlockPos hoek, int s, int rij) {
        BlockPos c = cel(hoek, s, rij);
        p.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
    }

    /** The once-mark of guhrio-beloning's running gag that this player holds for a world, or null. */
    private static String gagMerk(Player p, int wereld) {
        for (String key : GuhQuests.saved(p).keySet()) {
            if (key.startsWith("guhs_" + GuhrioBeloningFeature.LIJN.id() + "_") && key.endsWith("gag_" + wereld)) {
                return key;
            }
        }
        return null;
    }

    // =====================================================================================================================
    // the castle as one building
    // =====================================================================================================================

    @GuhTest(template = "empty", batch = BATCH)
    public static void guhrioSamenKasteelHeeftAlles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Map<String, Integer> starts = new HashMap<>();
        Map<Block, Integer> aantal = new HashMap<>();
        List<Block> soorten = List.of(GuhrioFeature.STARTBLOK.get(), GuhrioW1Feature.GRAS.get(), GuhrioW1Feature.TIP.get(), GuhrioW2Feature.KELDERGROND.get(),
                GuhrioW2Feature.WARPPIJP.get(), GuhrioW2Feature.NEST.get(), GuhrioW3Feature.BRUG.get(), GuhrioW3Feature.BAAS_PLEK.get(),
                GuhrioW3Feature.HENDEL.get(), GuhrioBeloningFeature.SCOREBORD.get(), GuhrioBeloningFeature.PIJP.get(), GuhrioBeloningFeature.VLAGGENMAST.get());
        StructurePlaceSettings settings = new StructurePlaceSettings();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                StructureTemplate tegel = level.getStructureManager().get(Guhs.id(GuhrioKasteel.STRUCTUUR + "/stuk_" + i + "_" + j)).orElse(null);
                helper.assertTrue(tegel != null, "the castle's tile " + i + " " + j);
                for (Block soort : soorten) {
                    for (StructureTemplate.StructureBlockInfo info : tegel.filterBlocks(new BlockPos(i * 32, 0, j * 32), settings, soort)) {
                        aantal.merge(soort, 1, Integer::sum);
                        if (soort == GuhrioFeature.STARTBLOK.get()) {
                            starts.merge(info.nbt().getStringOr("Level", ""), 1, Integer::sum);
                        }
                    }
                }
            }
        }
        // one start block per level, and a level file for each: nobody's slot still holds a practice lane of another id
        List<String> alle = new java.util.ArrayList<>(GuhrioKasteel.LEVELS);
        alle.add(GuhrioKasteel.DUEL);
        helper.assertTrue(starts.size() == alle.size(), "seven start blocks, each its own level: " + starts);
        for (String id : alle) {
            helper.assertTrue(starts.getOrDefault(id, 0) == 1, "one start block of " + id + ": " + starts);
            helper.assertTrue(GuhrioLevel.bestand(level.getServer(), id) != null, "the level file of " + id);
        }
        helper.assertTrue(GuhrioLevel.bestand(level.getServer(), Binnentuin.LEVEL_1).banen().size() == 3
                && GuhrioLevel.bestand(level.getServer(), Binnentuin.LEVEL_2).banen().size() == 3, "world 1: three lanes per level");
        // every slice's own things stand in the same castle
        for (Block soort : soorten) {
            helper.assertTrue(aantal.getOrDefault(soort, 0) > 0, "in the castle: " + soort);
        }
        // (world 2's own tests add one warp to a test level: only the warps into the castle count here)
        List<String> warps = GuhrioW2.WARP.values().stream().filter(doel -> doel.startsWith("kasteel_")).toList();
        helper.assertTrue(aantal.get(GuhrioW2Feature.WARPPIJP.get()) == warps.size(), "a warp pipe per warp: " + aantal.get(GuhrioW2Feature.WARPPIJP.get())
                + " / " + warps);
        helper.assertTrue(aantal.get(GuhrioW3Feature.BAAS_PLEK.get()) == 1 && aantal.get(GuhrioW3Feature.HENDEL.get()) == 1, "one boss, one lever");
        helper.assertTrue(aantal.get(GuhrioBeloningFeature.SCOREBORD.get()) == 1, "one highscore board");
        // a warp of world 2 leads to a level of the castle that another slice built
        for (String doel : warps) {
            helper.assertTrue(GuhrioKasteel.LEVELS.contains(doel) && starts.containsKey(doel), "the warp to " + doel);
        }
        helper.assertTrue(warps.contains(GuhrioW3Feature.LEVEL_3_1) && warps.contains(GuhrioW3Feature.LEVEL_3_2), "warps to world 3: " + warps);
        helper.succeed();
    }

    // =====================================================================================================================
    // two Pad-guhs, one sentence
    // =====================================================================================================================

    /**
     * A player who finished 1-1 reaches the flagpole of 1-2 for the first time: the Pad-guh of the forecourt calls the
     * famous sentence at once (his once-mark for world 1 is used), the Pad-guh in the lane thanks 25 ticks later, and the
     * key world 1 looks at to leave its own copy out is exactly the mark that was written. A player who walked into 1-2
     * without 1-1 (a warp) gets no call from the forecourt, so the Pad-guh in the lane says all of it himself.
     */
    // (the timeout is in ticks and the test server runs hundreds of ticks a second; the template's Pad-guh is there once
    //  his chunk is loaded, which takes real time)
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 40000)
    public static void guhrioSamenPrinsesEenKeerGezegd(GameTestHelper helper) throws ReflectiveOperationException {
        String naam = "guhriow1_test_1_2";
        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager().get(Guhs.id(naam)).orElse(null);
        helper.assertTrue(template != null, "the template guhs:" + naam);
        BlockPos hoek = helper.absolutePos(new BlockPos(0, 70, 0));
        forceer(level, hoek, template, true);
        template.placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
        Field veld = Binnentuin.class.getDeclaredField("BELONING_GAG");
        veld.setAccessible(true);
        String leest = (String) veld.get(null);

        ServerPlayer p = speler(helper), q = speler(helper);
        BlockPos start = cel(hoek, 2, 3);
        gehaald(p, Binnentuin.LEVEL_1);
        GuhrioKasteel.ontgrendel(p, Binnentuin.LEVEL_2);
        GuhrioKasteel.ontgrendel(q, Binnentuin.LEVEL_2);
        helper.assertTrue(GuhrioSpel.start(p, start) && GuhrioSpel.start(q, start), "both players in 1-2");
        helper.assertTrue(gagMerk(p, 1) == null && gagMerk(q, 1) == null, "nothing said yet");
        zet(p, hoek, 64, 12);
        GuhrioSpel.tick(p);
        zet(q, hoek, 64, 12);
        GuhrioSpel.tick(q);
        helper.assertTrue(GuhrioKasteel.gehaald(p, Binnentuin.LEVEL_2) && GuhrioKasteel.gehaald(q, Binnentuin.LEVEL_2), "the flagpole of 1-2, both");
        // the forecourt's Pad-guh has called after P (world 1 is done) and not after Q (1-1 is not)
        String merk = gagMerk(p, 1);
        helper.assertTrue(merk != null, "the forecourt's Pad-guh called after the player who finished world 1");
        helper.assertTrue(merk.equals(leest), "world 1 reads the mark that is really written: " + leest + " / " + merk);
        helper.assertTrue(GuhQuests.saved(p).getBooleanOr(leest, false) && !GuhQuests.saved(q).getBooleanOr(leest, false), "the mark, per player");
        helper.assertTrue(gagMerk(q, 1) == null, "1-1 not done: the forecourt keeps the sentence for later");
        helper.assertTrue(Binnentuin.bedankt(p) == 0 && Binnentuin.bedankt(q) == 0, "the Pad-guh in the lane takes a breath first");
        helper.succeedWhen(() -> {
            helper.assertTrue(Binnentuin.bedankt(p) == 1 && Binnentuin.bedankt(q) == 1, "the Pad-guh in the lane thanked both: "
                    + Binnentuin.bedankt(p) + " / " + Binnentuin.bedankt(q));
            helper.assertTrue(GidsFeature.heeft(p, "quest/guhrio_w1_prinses") && GidsFeature.heeft(q, "quest/guhrio_w1_prinses"), "world 1's advancement, both");
            // the Pad-guh behind the flagpole is the lane's own (a line), not the shop of the forecourt
            List<GuhNpcEntity> padguh = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(cel(hoek, 67, 12)).inflate(2),
                    e -> e.getKind() == GuhNpcEntity.Kind.PADGUH);
            helper.assertTrue(padguh.size() == 1, "Pad-guh at the end of 1-2: " + padguh.size());
            GuhNpcEntity winkel = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            winkel.setKind(GuhNpcEntity.Kind.PADGUH);
            helper.assertTrue(NpcRollen.van(padguh.get(0)) != NpcRollen.van(winkel), "the Pad-guh in the lane does not run the shop");
            winkel.discard();
            weg(helper, p, q);
            BlockPos eind = hoek.offset(template.getSize().getX() - 1, template.getSize().getY() - 1, template.getSize().getZ() - 1);
            for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(hoek.getX() - 2, hoek.getY() - 2, hoek.getZ() - 2, eind.getX() + 3, eind.getY() + 3,
                    eind.getZ() + 3), e -> !(e instanceof Player))) {
                e.discard();
            }
            for (BlockPos pos : BlockPos.betweenClosed(hoek, eind)) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
            forceer(level, hoek, template, false);
        });
    }

    // =====================================================================================================================
    // the duel's end is what the rewards wait for
    // =====================================================================================================================

    @GuhTest(template = "empty", batch = BATCH)
    public static void guhrioSamenNaHetDuel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper), b = speler(helper);
        GuhNpcEntity perzik = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        perzik.setKind(GuhNpcEntity.Kind.PERZIKGUH);
        BlockPos bij = helper.absolutePos(new BlockPos(2, 2, 2));
        perzik.snapTo(bij.getX() + 0.5, bij.getY(), bij.getZ() + 0.5);
        perzik.setPersistenceRequired();
        level.addFreshEntity(perzik);
        var kroon = ModItems.clothingItem(GuhClothes.GUHRIOBELONING_PRINSESSENKROON);
        NpcRollen.van(perzik).talk(perzik, a);
        helper.assertTrue(GuhQuests.count(a, kroon) == 0 && !VerhaalGuhs.magTemmen(a, VerhaalGuh.GUHSHI), "before the duel: no crown, no Guhshi");
        // world 3's own end of the duel (what the end scene calls when it is over)
        GroteNetherMikaEntity.gewonnen(a);
        helper.assertTrue(GuhrioKasteel.duelGewonnen(a) && !GuhrioKasteel.duelGewonnen(b), "the duel is won, per player");
        helper.assertTrue(GidsFeature.heeft(a, "guhrio/guhrio_w3_taart"), "world 3's advancement");
        helper.assertTrue(VerhaalGuhs.magTemmen(a, VerhaalGuh.GUHSHI) && !VerhaalGuhs.magTemmen(b, VerhaalGuh.GUHSHI), "Guhshi may come along with the winner");
        NpcRollen.van(perzik).talk(perzik, a);
        helper.assertTrue(GuhQuests.count(a, kroon) == 1 && GuhQuests.count(a, Items.CAKE) == 1 && GidsFeature.heeft(a, "quest/guhrio_beloning_kroon"),
                "the princess: the crown and a cake");
        NpcRollen.van(perzik).talk(perzik, b);
        helper.assertTrue(GuhQuests.count(b, kroon) == 0, "nothing for who has not won");
        perzik.discard();
        VerhaalGuhs.vergeet(a, VerhaalGuh.GUHSHI);
        Praat.vergeet(a);
        Praat.vergeet(b);
        weg(helper, a, b);
        helper.succeed();
    }
}
