package nl.juiced.guhs.feature.bio.bouwmeer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.world.Terugkeer;

/**
 * Game tests of the biomes3 slice "bouw-meer". The test server has no Guhmensie, so the buildings stand in templates that
 * hold them exactly as the structures place them (tools/features/bio_bouw_meer_bouw.py; a template's y 0 is helper y 1):
 * <ul>
 *   <li>botenhuisje_test_meer (20 x 18 x 25): the botenhuisje at (3, 0, 0) on a shore, the lake's top water block at
 *       helper y 8, the deck at helper y 9;</li>
 *   <li>botenhuisje_test_eiland (34 x 8 x 30): water (top block helper y 3) with a large island (x 5..17, z 6..16), a
 *       small one (x 24..27, z 8..11) and mainland (z 26..29), grass at helper y 4;</li>
 *   <li>picknickeilandje_test_eiland (15 x 14 x 13): grass with a stand-in tree and the picnic at (1, 0, 1).</li>
 * </ul>
 * Day and hour come from {@link Klok}, set per test box.
 */
public class BioBouwMeerGameTests {
    private static final String BATCH = "bio_bouw_meer";
    private static final String MEER = "botenhuisje_test_meer", EILAND = "botenhuisje_test_eiland", PICKNICK = "picknickeilandje_test_eiland";
    /** The meerpaal in botenhuisje_test_meer and the mand in picknickeilandje_test_eiland (helper positions). */
    private static final BlockPos MEERPAAL = new BlockPos(3 + 6, 1 + 8, 15), MAND = new BlockPos(1 + 7, 1 + 1, 1 + 5);

    private static ServerPlayer speler(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        p.snapTo(at.x, at.y, at.z, 0f, 0f);
        return p;
    }

    private static void klaar(GameTestHelper helper, AABB box, ServerPlayer... spelers) {
        Klok.zet(box, null, 0);
        for (ServerPlayer p : spelers) {
            Visserguh.vergeet(p);
            Praat.vergeet(p);
            Minigames.forget(p);
            if (!p.isRemoved()) {
                helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private static boolean adv(ServerPlayer p, String naam) {
        AdvancementHolder holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static int tel(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    private static List<RoeibootjeEntity> bootjes(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(RoeibootjeEntity.class, helper.getBounds().inflate(2), Entity::isAlive);
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, double x, double y, double z) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        npc.snapTo(at.x, at.y, at.z, 180f, 0f);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    private static Entity koi(GameTestHelper helper, double x, double y, double z, int leeftijd) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Guhs.id("koi")).orElseThrow();
        Entity koi = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        koi.snapTo(at.x, at.y, at.z, 0f, 0f);
        helper.getLevel().addFreshEntity(koi);
        koi.tickCount = leeftijd;
        return koi;
    }

    // =====================================================================================================================
    // the botenhuisje: the building, the boat (never a farm, never a pile), the visser-guh's return, the lantern
    // =====================================================================================================================
    @GuhTest(template = MEER, batch = BATCH, timeoutTicks = 200)
    public static void bioBouwMeerSteigerEnBootje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB box = helper.getBounds().inflate(2);
        Klok.zet(box, 3L, 6000);
        BlockPos anker = helper.absolutePos(MEERPAAL);
        BlockState paal = level.getBlockState(anker);
        helper.assertTrue(paal.getBlock() instanceof MeerpaalBlock && paal.getValue(MeerpaalBlock.FACING) == Direction.NORTH, "the meerpaal stands in the jetty: " + paal);
        Direction kijk = Direction.NORTH;
        BlockPos ligplaats = MeerpaalBlock.ligplaats(level, anker, kijk);
        helper.assertTrue(ligplaats != null && ligplaats.equals(anker.offset(2, -1, 0)), "the berth is the water right beside the jetty, one below the deck: " + ligplaats);
        // the jetty reaches open water: every column of the berth and of the way out is water with air above it
        for (int zuid = -14; zuid <= 3; zuid++) {
            for (int oost = 1; oost <= 3; oost++) {
                BlockPos p = MeerpaalBlock.plek(anker, kijk, oost, -1, zuid);
                helper.assertTrue(level.getFluidState(p).isSource() && level.getBlockState(p.above()).isAir(), "open water for the boat at " + oost + " / " + zuid);
            }
        }
        // the visser-guh sits on the end of the jetty, where the mooring expects him, on a deck block over the water
        Vec3 zit = MeerpaalBlock.punt(anker, kijk, MeerpaalBlock.VISSER[0], MeerpaalBlock.VISSER[1], MeerpaalBlock.VISSER[2]);
        List<GuhNpcEntity> vissers = level.getEntitiesOfClass(GuhNpcEntity.class, box, n -> n.getKind() == GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH);
        helper.assertTrue(vissers.size() == 1 && vissers.get(0).position().distanceTo(zit) < 0.3, "one visser-guh on his spot: " + vissers.size());
        GuhNpcEntity visser = vissers.get(0);
        helper.assertTrue(NpcRollen.van(visser) == Visserguh.ROL && visser.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "he is the visser-guh and never walks");
        BlockPos onderHem = BlockPos.containing(zit).below();
        helper.assertTrue(!level.getBlockState(onderHem).isAir() && level.getFluidState(onderHem.below()).isSource(), "he sits on the deck over the water");
        BlockPos lantaarn = MeerpaalBlock.plek(anker, kijk, MeerpaalBlock.LANTAARN[0], MeerpaalBlock.LANTAARN[1], MeerpaalBlock.LANTAARN[2]);
        helper.assertTrue(level.getBlockState(lantaarn).getBlock() instanceof SteigerlantaarnBlock, "the lantern stands at the jetty's end");
        // its post goes down through the water and stands on the bottom (nothing floats)
        BlockPos post = lantaarn.below(3);
        int diep = 0;
        while (diep < 12 && level.getBlockState(post.below(diep)).is(BlockTags.LOGS)) {
            diep++;
        }
        helper.assertTrue(diep >= 4 && !level.getBlockState(post.below(diep)).isAir() && level.getFluidState(post.below(diep)).isEmpty(), "the post under the lantern stands on the bottom: " + diep);

        // --- the boat: one lies ready, and only one ---
        helper.assertTrue(bootjes(helper).isEmpty(), "no boat before a player came near");
        helper.assertTrue(MeerpaalBlock.zorg(level, anker) == 1 && bootjes(helper).size() == 1, "the mooring lays a boat ready");
        RoeibootjeEntity boot = bootjes(helper).get(0);
        helper.assertTrue(ligplaats.equals(boot.thuis()) && boot.isThuis() && boot.getY() > ligplaats.getY() + 0.5, "it lies in its berth, on the water");
        helper.assertTrue(MeerpaalBlock.zorg(level, anker) == 0 && bootjes(helper).size() == 1, "looking again does not make a second boat");
        helper.assertTrue(boot.regels(level, 100000) && boot.isAlive(), "a boat in its berth stays, however long it lies empty");

        ServerPlayer p = speler(helper, 7.5, 10, 12.5), q = speler(helper, 8.5, 10, 13.5);
        try {
            // --- not a thing to take: hitting it gives nothing, a mob cannot ride off with it ---
            for (int i = 0; i < 12; i++) {
                boot.hurtServer(level, level.damageSources().playerAttack(p), 4f);
            }
            helper.assertTrue(boot.isAlive() && level.getEntitiesOfClass(ItemEntity.class, box).isEmpty(), "hitting the boat breaks nothing and drops nothing");
            GuhNpcEntity lifter = npc(helper, GuhNpcEntity.Kind.HANAMI_GUH, 12.5, 10, 12.5);
            helper.assertTrue(!lifter.startRiding(boot) && boot.getPassengers().isEmpty(), "only players get in");
            lifter.discard();

            // --- each player finds a boat: p rows away, the berth gets a new one for q; p's boat is gone a minute after p leaves it ---
            helper.assertTrue(p.startRiding(boot) && boot.getPassengers().contains(p), "a player gets in");
            boot.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
            Vec3 ver = helper.absoluteVec(new Vec3(15.5, 9, 2.5));
            boot.snapTo(ver.x, ver.y, ver.z, 0f, 0f);
            helper.assertTrue(!boot.isThuis() && boot.regels(level, 100000), "a boat with a rower stays wherever it is");
            helper.assertTrue(MeerpaalBlock.zorg(level, anker) == 1 && bootjes(helper).size() == 2, "the empty berth gets a boat for the next player");
            RoeibootjeEntity tweede = MeerpaalBlock.liggend(level, ligplaats).get(0);
            helper.assertTrue(q.startRiding(tweede), "the second player rows too");
            q.stopRiding();
            p.stopRiding();
            helper.assertTrue(boot.regels(level, 600) && boot.leeg() == 600, "left empty away from the berth: it waits");
            // (saved and loaded, it still knows its berth and how long it lay empty)
            CompoundTag tag = Nbt.saveWithoutId(boot);
            RoeibootjeEntity kopie = BouwMeerSlice.ROEIBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED);
            Nbt.load(kopie, tag);
            helper.assertTrue(ligplaats.equals(kopie.thuis()) && kopie.leeg() == 600, "the boat remembers its berth: " + kopie.thuis() + " / " + kopie.leeg());
            kopie.discard();
            helper.assertTrue(!boot.regels(level, 600) && !boot.isAlive(), "a minute empty away from the berth: gone");
            helper.assertTrue(bootjes(helper).size() == 1 && level.getEntitiesOfClass(ItemEntity.class, box).isEmpty(), "one boat again, and nothing was left behind");

            // --- no pile: a rower who parks beside the waiting boat leaves one boat ---
            for (int i = 0; i < 3; i++) {
                RoeibootjeEntity extra = BouwMeerSlice.ROEIBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED);
                extra.setInitialPos(ligplaats.getX() + 0.5 + (i - 1), ligplaats.getY() + 0.9, ligplaats.getZ() + 1.5 + i * 0.5);
                level.addFreshEntity(extra);
            }
            helper.assertTrue(bootjes(helper).size() == 4, "three more boats parked in the berth");
            helper.assertTrue(MeerpaalBlock.zorg(level, anker) == 0 && bootjes(helper).size() == 1, "the mooring keeps one: " + bootjes(helper).size());
            for (int i = 0; i < 20; i++) {
                MeerpaalBlock.zorg(level, anker);
                helper.getBlockState(MEERPAAL).useWithoutItem(level, q, new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(anker), Direction.UP, anker, false));
            }
            helper.assertTrue(bootjes(helper).size() == 1, "asking again and again never makes more than the one in the berth");

            // --- the boat stays at its lake ---
            RoeibootjeEntity ver2 = bootjes(helper).get(0);
            helper.assertTrue(p.startRiding(ver2), "p gets in again");
            ver2.zetThuis(anker.offset(0, 0, (int) RoeibootjeEntity.BEREIK + 40));
            helper.assertTrue(!ver2.regels(level, 20) && !ver2.isAlive() && p.getVehicle() == null, "too far from its mooring: the rower is let out and the boat is gone");
            helper.assertTrue(adv(p, "roeibootje_gevaren") && !adv(q, "roeibootje_gevaren"), "the proof of rowing is the rower's own");

            // --- the visser-guh comes back, and never doubles ---
            visser.discard();
            BlockPos zitBlok = BlockPos.containing(zit);
            MeerpaalBlock.zorg(level, anker);
            helper.assertTrue(level.getEntitiesOfClass(GuhNpcEntity.class, box, n -> n.getKind() == GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH).isEmpty(), "seen missing once: not yet");
            Terugkeer.zetGemist(level, MeerpaalBlock.TERUG_VISSER, zitBlok, Terugkeer.BEVESTIG + 5);
            MeerpaalBlock.zorg(level, anker);
            vissers = level.getEntitiesOfClass(GuhNpcEntity.class, box, n -> n.getKind() == GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH);
            helper.assertTrue(vissers.size() == 1 && vissers.get(0).position().distanceTo(zit) < 0.05 && Math.abs(vissers.get(0).getYRot() - Direction.NORTH.toYRot()) < 1,
                    "seen missing twice: a new visser-guh sits on the jetty's end, facing the lake: " + vissers.size());
            npc(helper, GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH, 3 + 5.5, 10, 4.5);
            MeerpaalBlock.zorg(level, anker);
            helper.assertTrue(level.getEntitiesOfClass(GuhNpcEntity.class, box, n -> n.getKind() == GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH && n.isAlive()).size() == 1, "a double goes");

            // --- the lantern: lit from dusk to dawn ---
            helper.assertTrue(!level.getBlockState(lantaarn).getValue(SteigerlantaarnBlock.LIT), "by day the lantern is out");
            Klok.zet(box, 3L, Klok.LAMP_AAN + 10);
            MeerpaalBlock.zorg(level, anker);
            BlockState aan = level.getBlockState(lantaarn);
            helper.assertTrue(aan.getValue(SteigerlantaarnBlock.LIT) && aan.getLightEmission(level, lantaarn) == SteigerlantaarnBlock.LICHT, "at dusk it lights");
            Klok.zet(box, 4L, Klok.LAMP_UIT + 10);
            aan.randomTick(level, lantaarn, level.getRandom());
            helper.assertTrue(!level.getBlockState(lantaarn).getValue(SteigerlantaarnBlock.LIT) && level.getBlockState(lantaarn).getLightEmission(level, lantaarn) == 0,
                    "at dawn it goes out (a random tick is enough)");
            Klok.zet(box, 4L, Klok.LAMP_AAN - 10);
            MeerpaalBlock.zorg(level, anker);
            helper.assertTrue(!level.getBlockState(lantaarn).getValue(SteigerlantaarnBlock.LIT), "just before dusk it is still out");
        } finally {
            bootjes(helper).forEach(Entity::discard);
            klaar(helper, box, p, q);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the visser-guh: koivoer once a day per player, his moods by the time of day, every text exists
    // =====================================================================================================================
    @GuhTest(template = MEER, batch = BATCH)
    public static void bioBouwMeerKoivoerPerDag(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB box = helper.getBounds().inflate(2);
        Klok.zet(box, 10L, 6000);
        GuhNpcEntity visser = level.getEntitiesOfClass(GuhNpcEntity.class, box, n -> n.getKind() == GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH).get(0);
        ServerPlayer p = speler(helper, 7.5, 10, 6.5), q = speler(helper, 8.5, 10, 6.5);
        try {
            Item voer = Visserguh.koivoer();
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(voer).equals(Guhs.id("koivoer")), "koivoer is the dieren slice's item");
            Visserguh.ROL.talk(visser, p);
            helper.assertTrue(adv(p, "botenhuisje_visser") && tel(p, voer) == 0, "a chat alone gives nothing");
            Visserguh.ROL.antwoord(visser, p, Visserguh.KOIVOER);
            helper.assertTrue(tel(p, voer) == Visserguh.VOER_PER_DAG && adv(p, "botenhuisje_koivoer"), "the day's handful: " + tel(p, voer));
            for (int i = 0; i < 5; i++) {
                Visserguh.ROL.antwoord(visser, p, Visserguh.KOIVOER);
            }
            helper.assertTrue(tel(p, voer) == Visserguh.VOER_PER_DAG && !Visserguh.geefKoivoer(visser, p), "asking again the same day gives no more");
            helper.assertTrue(tel(q, voer) == 0 && !adv(q, "botenhuisje_koivoer"), "the other player got nothing from that");
            Visserguh.ROL.antwoord(visser, q, Visserguh.KOIVOER);
            helper.assertTrue(tel(q, voer) == Visserguh.VOER_PER_DAG, "and gets their own handful the same day");
            Klok.zet(box, 10L, 23000);
            helper.assertTrue(!Visserguh.geefKoivoer(visser, p), "later the same day: still none");
            Klok.zet(box, 11L, 100);
            Visserguh.ROL.antwoord(visser, p, Visserguh.KOIVOER);
            helper.assertTrue(tel(p, voer) == 2 * Visserguh.VOER_PER_DAG, "the next day there is koivoer again");
            // the boat option lays a boat ready, never two
            Visserguh.ROL.antwoord(visser, p, Visserguh.BOOTJE);
            Visserguh.ROL.antwoord(visser, q, Visserguh.BOOTJE);
            helper.assertTrue(bootjes(helper).size() == 1, "asking for the boat: one lies in the berth: " + bootjes(helper).size());
            // his greeting follows the time of day
            long[][] uren = {{1000, 0}, {8000, 1}, {12500, 2}, {18000, 3}, {23500, 0}};
            for (long[] uur : uren) {
                Klok.zet(box, 11L, uur[0]);
                Klok.Dagdeel deel = Klok.Dagdeel.values()[(int) uur[1]];
                String groet = Visserguh.groet(visser, p);
                helper.assertTrue(Klok.dagdeel(level, visser.blockPosition()) == deel && groet.startsWith("gui.guhs.botenhuisje.visser." + deel.id() + "."),
                        "at " + uur[0] + " he greets like " + deel + ": " + groet);
            }
            // every line he and the hanami guhs can say exists
            List<String> keys = new ArrayList<>();
            for (Klok.Dagdeel deel : Klok.Dagdeel.values()) {
                for (int i = 0; i < Visserguh.GROETEN; i++) {
                    keys.add("gui.guhs.botenhuisje.visser." + deel.id() + "." + i);
                }
            }
            for (int i = 0; i < Visserguh.GEDACHTEN; i++) {
                keys.add("gui.guhs.botenhuisje.visser.koi." + i);
            }
            for (int i = 0; i < Hanami.LIEF; i++) {
                keys.add("gui.guhs.hanami.lief." + i);
            }
            for (int i = 0; i < Hanami.GEWOON; i++) {
                keys.add("gui.guhs.hanami.guh." + i);
            }
            for (int i = 0; i < Hanami.SLAAP; i++) {
                keys.add("gui.guhs.hanami.slaap." + i);
            }
            for (String s : List.of("optie.bijten", "optie.koivoer", "optie.bootje", "optie.meer", "optie.meer_bezig", "optie.meer_klaar", "visser.koivoer", "visser.koivoer_op",
                    "visser.bootje", "les.begin", "les.voer_nog", "les.voer_klaar", "les.blaadje_nog", "les.blaadje_klaar", "les.eiland_nog", "les.eiland_klaar", "les.koi_nog",
                    "les.koi_loslaten", "les.klaar", "les.klaar.chat", "les.na")) {
                keys.add("gui.guhs.botenhuisje." + s);
            }
            for (String s : List.of("voer", "blaadje", "eiland", "koi", "loslaten", "terug")) {
                keys.add("quest.guhs.botenhuisje." + s);
            }
            keys.addAll(List.of("gui.guhs.roeibootje.te_ver", "gui.guhs.roeibootje.geroepen", "gui.guhs.roeibootje.ligt_klaar", "gui.guhs.picknickeilandje.mand.pak",
                    "gui.guhs.picknickeilandje.mand.gehad", "structure.guhs.botenhuisje", "structure.guhs.botenhuisje.tooltip", "structure.guhs.picknickeilandje",
                    "structure.guhs.picknickeilandje.tooltip", "entity.guhs.roeibootje"));
            for (String key : keys) {
                helper.assertTrue(Language.getInstance().has(key), "text " + key);
            }
        } finally {
            bootjes(helper).forEach(Entity::discard);
            klaar(helper, box, p, q);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the lessons of the lake: four steps, two players side by side
    // =====================================================================================================================
    @GuhTest(template = EILAND, batch = BATCH)
    public static void bioBouwMeerLessenVanHetMeer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB box = helper.getBounds().inflate(2);
        Klok.zet(box, 20L, 6000);
        GuhNpcEntity visser = npc(helper, GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH, 10.5, 5, 27.5);
        ServerPlayer p = speler(helper, 10.5, 5, 28.5), q = speler(helper, 12.5, 5, 28.5);
        int waterY = helper.absolutePos(new BlockPos(0, 3, 0)).getY();
        try {
            Item voer = Visserguh.koivoer(), blaadje = Visserguh.blaadje(), emmer = Visserguh.koiEmmer();
            helper.assertTrue(Visserguh.stap(p) == Visserguh.NIET && Visserguh.voortgang(visser, p) == null, "nothing yet");
            // --- start: koivoer for step 1 ---
            Visserguh.ROL.antwoord(visser, p, Visserguh.MEER);
            helper.assertTrue(Visserguh.stap(p) == Visserguh.VOER && tel(p, voer) == Visserguh.VOER_PER_DAG && Visserguh.stap(q) == Visserguh.NIET, "p starts, q did not");
            Visserguh.ROL.antwoord(visser, p, Visserguh.MEER);
            helper.assertTrue(tel(p, voer) == Visserguh.VOER_PER_DAG, "asking what to do again gives no second koivoer");
            // --- 1: koivoer near a koi ---
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(voer, 2));
            p.gameMode.useItem(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND);
            helper.assertTrue(!Visserguh.gedaan(p), "koivoer with no koi around is no feeding");
            Entity vis = koi(helper, 10.5, 3.2, 24.5, 500);
            p.gameMode.useItem(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND);
            helper.assertTrue(Visserguh.gedaan(p) && Visserguh.stap(p) == Visserguh.VOER, "koivoer near a koi: the deed is done, now tell him");
            helper.assertTrue(!Visserguh.gevoerd(q) && !Visserguh.gedaan(q), "q, who has not started, is not moved by koivoer");
            Visserguh.ROL.talk(visser, p);
            helper.assertTrue(Visserguh.stap(p) == Visserguh.BLAADJE && !Visserguh.gedaan(p), "he sends p for a petal");
            // --- 2: a floating petal ---
            Visserguh.ROL.talk(visser, p);
            helper.assertTrue(Visserguh.stap(p) == Visserguh.BLAADJE, "no petal, no next step");
            p.getInventory().add(new ItemStack(blaadje, 3));
            Visserguh.ROL.talk(visser, p);
            helper.assertTrue(Visserguh.stap(p) == Visserguh.EILAND && tel(p, blaadje) == 2, "he takes one petal: " + tel(p, blaadje));
            // --- 3: row to a large island and step ashore ---
            BlockPos groot = helper.absolutePos(new BlockPos(11, 5, 11)), klein = helper.absolutePos(new BlockPos(25, 5, 9)), vast = helper.absolutePos(new BlockPos(10, 5, 27));
            helper.assertTrue(Visserguh.grootEiland(level, groot, waterY, Visserguh.EILAND_WATER), "the large island is a large island");
            helper.assertTrue(Visserguh.grootEiland(level, helper.absolutePos(new BlockPos(5, 5, 6)), waterY, Visserguh.EILAND_WATER), "also on its corner");
            helper.assertTrue(!Visserguh.grootEiland(level, klein, waterY, Visserguh.EILAND_WATER), "the small island is too small");
            helper.assertTrue(!Visserguh.grootEiland(level, vast, waterY, 3), "the mainland has no water inland");
            helper.assertTrue(!Visserguh.grootEiland(level, helper.absolutePos(new BlockPos(20, 5, 20)), waterY, Visserguh.EILAND_WATER), "open water is no island");
            p.snapTo(groot.getX() + 0.5, groot.getY(), groot.getZ() + 0.5, 0f, 0f);
            p.setOnGround(true);
            Visserguh.tik(p);
            helper.assertTrue(!Visserguh.gedaan(p), "standing on the island without having rowed does not count");
            RoeibootjeEntity boot = BouwMeerSlice.ROEIBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED);
            Vec3 opWater = helper.absoluteVec(new Vec3(20.5, 3.9, 20.5));
            boot.setInitialPos(opWater.x, opWater.y, opWater.z);
            boot.zetThuis(BlockPos.containing(opWater.x, waterY, opWater.z));
            level.addFreshEntity(boot);
            helper.assertTrue(p.startRiding(boot), "p rows");
            Visserguh.tik(p);
            helper.assertTrue(!Visserguh.gedaan(p), "sitting in the boat is not being on the island");
            p.stopRiding();
            p.snapTo(klein.getX() + 0.5, klein.getY(), klein.getZ() + 0.5, 0f, 0f);
            p.setOnGround(true);
            Visserguh.tik(p);
            helper.assertTrue(!Visserguh.gedaan(p), "a small island does not count");
            p.snapTo(groot.getX() + 0.5, groot.getY(), groot.getZ() + 0.5, 0f, 0f);
            p.setOnGround(true);
            Visserguh.tik(p);
            helper.assertTrue(Visserguh.gedaan(p), "rowed, and ashore on the large island: done");
            boot.discard();
            Visserguh.ROL.talk(visser, p);
            helper.assertTrue(Visserguh.stap(p) == Visserguh.KOI && tel(p, Items.BUCKET) == 1, "he lends his bucket for the last step");
            // --- 4: a koi in a bucket, and out again ---
            Visserguh.tik(p);
            helper.assertTrue(!GuhQuests.saved(p).getBooleanOr(Visserguh.GEVANGEN, false), "no koi in a bucket yet");
            p.getInventory().add(new ItemStack(emmer));
            Visserguh.tik(p);
            helper.assertTrue(GuhQuests.saved(p).getBooleanOr(Visserguh.GEVANGEN, false) && !Visserguh.gedaan(p), "a koi in the bucket: caught, not yet let go");
            Visserguh.ROL.talk(visser, p);
            helper.assertTrue(Visserguh.stap(p) == Visserguh.KOI, "telling him now changes nothing");
            GuhQuests.take(p, emmer, 1);
            p.snapTo(vast.getX() + 0.5, vast.getY(), vast.getZ() + 0.5, 0f, 0f);
            Visserguh.tik(p);
            helper.assertTrue(!Visserguh.gedaan(p), "the bucket gone but no koi set free near (the old koi does not count): not done");
            p.getInventory().add(new ItemStack(emmer));
            Visserguh.tik(p);
            GuhQuests.take(p, emmer, 1);
            koi(helper, 11.5, 3.2, 24.5, 5);
            Visserguh.tik(p);
            helper.assertTrue(Visserguh.gedaan(p), "the koi swims free again: done");
            // --- the reward, once ---
            Visserguh.ROL.talk(visser, p);
            Item lantaarn = BouwMeerSlice.STEIGERLANTAARN_ITEM.get(), hengel = BouwMeerSlice.HENGELSTANDAARD_ITEM.get(), windzak = BouwMeerSlice.KOIWINDZAK_ITEM.get();
            helper.assertTrue(Visserguh.stap(p) == Visserguh.KLAAR && adv(p, "botenhuisje_visser_klaar"), "p knows the lake");
            helper.assertTrue(tel(p, lantaarn) == 2 && tel(p, hengel) == 1 && tel(p, windzak) == 1, "two lanterns, a rod on a stand and a koi windsock");
            Visserguh.ROL.talk(visser, p);
            Visserguh.ROL.antwoord(visser, p, Visserguh.MEER);
            helper.assertTrue(tel(p, lantaarn) == 2 && tel(p, hengel) == 1 && tel(p, windzak) == 1 && Visserguh.stap(p) == Visserguh.KLAAR, "no second reward");
            // --- q does it on their own, untouched by p ---
            helper.assertTrue(Visserguh.stap(q) == Visserguh.NIET && !adv(q, "botenhuisje_visser_klaar") && tel(q, lantaarn) == 0, "q has nothing of p's");
            Visserguh.ROL.antwoord(visser, q, Visserguh.MEER);
            helper.assertTrue(Visserguh.stap(q) == Visserguh.VOER && tel(q, voer) == Visserguh.VOER_PER_DAG && Visserguh.stap(p) == Visserguh.KLAAR, "q starts their own lessons");
            q.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(voer));
            q.gameMode.useItem(q, level, q.getMainHandItem(), InteractionHand.MAIN_HAND);
            Visserguh.ROL.talk(visser, q);
            q.getInventory().add(new ItemStack(blaadje));
            Visserguh.ROL.talk(visser, q);
            Visserguh.doeAlsofGevaren(q, waterY);
            q.snapTo(groot.getX() + 0.5, groot.getY(), groot.getZ() + 0.5, 0f, 0f);
            q.setOnGround(true);
            Visserguh.tik(q);
            Visserguh.ROL.talk(visser, q);
            q.getInventory().add(new ItemStack(emmer));
            Visserguh.tik(q);
            GuhQuests.take(q, emmer, 1);
            q.snapTo(vast.getX() + 0.5, vast.getY(), vast.getZ() + 0.5, 0f, 0f);
            koi(helper, 12.5, 3.2, 24.5, 5);
            Visserguh.tik(q);
            Visserguh.ROL.talk(visser, q);
            helper.assertTrue(Visserguh.stap(q) == Visserguh.KLAAR && adv(q, "botenhuisje_visser_klaar") && tel(q, lantaarn) == 2 && tel(p, lantaarn) == 2,
                    "q finished too, with their own reward: step " + Visserguh.stap(q));
            vis.discard();
        } finally {
            EntityType<?> koiType = BuiltInRegistries.ENTITY_TYPE.getOptional(Guhs.id("koi")).orElse(null);
            level.getEntities((Entity) null, box, e -> e.getType() == koiType || e instanceof RoeibootjeEntity || e instanceof ItemEntity).forEach(Entity::discard);
            visser.discard();
            klaar(helper, box, p, q);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the picknickeilandje: the hanami guhs (stay, never double, day and night), the mand per player, lanterns in the tree
    // =====================================================================================================================
    private static List<GuhNpcEntity> hanami(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(2), n -> Hanami.isHanami(n.getKind()) && n.isAlive());
    }

    private static long slapers(GameTestHelper helper) {
        return hanami(helper).stream().filter(n -> Hanami.slaapt(n.getKind())).count();
    }

    @GuhTest(template = PICKNICK, batch = BATCH, timeoutTicks = 200)
    public static void bioBouwMeerPicknick(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB box = helper.getBounds().inflate(2);
        Klok.zet(box, 30L, 6000);
        BlockPos mand = helper.absolutePos(MAND);
        BlockState state = level.getBlockState(mand);
        helper.assertTrue(state.getBlock() instanceof MandBlock && state.getValue(MandBlock.FACING) == Direction.NORTH && !state.getValue(MandBlock.VERSIERD), "the mand: " + state);
        Direction kijk = Direction.NORTH;
        // the rug lies in the ground round the trunk; the island's tree stands at its head
        int kleed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(mand.offset(-5, -1, -3), mand.offset(3, -1, 2))) {
            kleed += level.getBlockState(pos).is(BlockTags.WOOL) ? 1 : 0;
        }
        helper.assertTrue(kleed == 27 && level.getBlockState(mand.offset(-1, 0, -2)).is(BlockTags.LOGS), "27 tiles of rug round the trunk: " + kleed);
        // three guhs on their seats, as the mand expects them
        helper.assertTrue(hanami(helper).size() == 3, "three hanami guhs: " + hanami(helper).size());
        for (MandBlock.Zitplek z : MandBlock.ZITPLEKKEN) {
            Vec3 zit = MandBlock.zit(mand, kijk, z);
            long hier = hanami(helper).stream().filter(n -> n.position().distanceTo(zit) < 0.3).count();
            helper.assertTrue(hier == 1, "one guh on seat " + z.naam() + ": " + hier);
            helper.assertTrue(level.getBlockState(BlockPos.containing(zit).below()).is(BlockTags.WOOL), "seat " + z.naam() + " is on the rug");
        }
        helper.assertTrue(MandBlock.zorg(level, mand) == 0 && hanami(helper).size() == 3 && slapers(helper) == 1, "by day: two look at the blossom, one naps");
        for (GuhNpcEntity g : hanami(helper)) {
            helper.assertTrue(NpcRollen.van(g) == Hanami.ROL && g.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0 && !g.isCustomNameVisible() && !g.isPushable()
                    && !g.canBeLeashed(), "a hanami guh is a character: it cannot walk, be pushed or led, and wears no name plate");
        }
        List<Vec3> plekken = hanami(helper).stream().map(Entity::position).toList();
        ServerPlayer p = speler(helper, 13.5, 2, 11.5), q = speler(helper, 13.5, 2, 10.5);
        helper.runAfterDelay(60, () -> {
            try {
                // --- they did not wander off, and nothing made more of them ---
                for (int i = 0; i < 30; i++) {
                    MandBlock.zorg(level, mand);
                }
                List<GuhNpcEntity> nu = hanami(helper);
                helper.assertTrue(nu.size() == 3, "still three after sixty ticks and thirty looks: " + nu.size());
                for (GuhNpcEntity g : nu) {
                    helper.assertTrue(plekken.stream().anyMatch(v -> v.distanceTo(g.position()) < 0.05), "a hanami guh stays on its seat: " + g.position());
                    helper.assertTrue(g.goalSelector.getAvailableGoals().isEmpty(), "it has no mind for anything but the blossom");
                }
                for (GuhNpcEntity g : nu) {
                    if (!Hanami.slaapt(g.getKind())) {
                        helper.assertTrue(g.getXRot() < -8, "an awake hanami guh looks up at the blossom: " + g.getXRot());
                    } else {
                        helper.assertTrue(g.getXRot() > 2, "the napping one hangs its head: " + g.getXRot());
                    }
                }
                // --- night and day ---
                Klok.zet(box, 30L, Klok.SLAAP_VAN + 100);
                helper.assertTrue(MandBlock.zorg(level, mand) == 0 && slapers(helper) == 3 && hanami(helper).size() == 3, "from dusk all three sleep on the rug");
                Klok.zet(box, 31L, Klok.SLAAP_TOT + 100);
                helper.assertTrue(MandBlock.zorg(level, mand) == 0 && slapers(helper) == 1, "at dawn two wake up again");
                // --- they talk; the bloesemguh says something sweet ---
                for (GuhNpcEntity g : hanami(helper)) {
                    Hanami.ROL.talk(g, p);
                }
                helper.assertTrue(adv(p, "hanami_gesproken") && !adv(q, "hanami_gesproken"), "talked to a hanami guh");
                // --- one goes missing: it comes back; a double goes ---
                GuhNpcEntity weg = hanami(helper).stream().filter(g -> g.getKind() == GuhNpcEntity.Kind.HANAMI_BLOESEMGUH).findFirst().orElseThrow();
                Vec3 was = weg.position();
                weg.discard();
                MandBlock.zorg(level, mand);
                helper.assertTrue(hanami(helper).size() == 2, "seen missing once: not yet");
                Terugkeer.zetGemist(level, MandBlock.terugNaam(MandBlock.ZITPLEKKEN.get(0)), BlockPos.containing(was), Terugkeer.BEVESTIG + 5);
                helper.assertTrue(MandBlock.zorg(level, mand) == 1 && hanami(helper).size() == 3, "seen missing twice: back");
                GuhNpcEntity terug = hanami(helper).stream().filter(g -> g.getKind() == GuhNpcEntity.Kind.HANAMI_BLOESEMGUH).findFirst().orElseThrow();
                helper.assertTrue(terug.position().distanceTo(was) < 0.05 && !terug.isCustomNameVisible(), "the bloesemguh sits on its own seat again");
                Vec3 at = MandBlock.zit(mand, kijk, MandBlock.ZITPLEKKEN.get(1));
                GuhNpcEntity dubbel = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
                dubbel.setKind(GuhNpcEntity.Kind.HANAMI_GUH);
                dubbel.snapTo(at.x + 0.3, at.y, at.z, 0f, 0f);
                level.addFreshEntity(dubbel);
                MandBlock.zorg(level, mand);
                helper.assertTrue(hanami(helper).size() == 3, "a double on a seat goes: " + hanami(helper).size());

                // --- the mand: a treat for every player, once ---
                Item koek = BuiltInRegistries.ITEM.getValue(Guhs.id("roze_guh_koek")), knabbels = BuiltInRegistries.ITEM.getValue(Guhs.id("kaas_knabbels"));
                helper.assertTrue(!MandBlock.gehad(p, mand) && MandBlock.pak(p, mand), "p takes their treat");
                int koeken = tel(p, koek), knab = tel(p, knabbels);
                helper.assertTrue(koeken >= 2 && koeken <= 3 && knab >= 3 && knab <= 5 && adv(p, "picknickeilandje_mand"), "a modest treat: " + koeken + " koeken, " + knab + " knabbels");
                int alles = p.getInventory().getNonEquipmentItems().stream().mapToInt(ItemStack::getCount).sum();
                for (int i = 0; i < 5; i++) {
                    state.useWithoutItem(level, p, new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(mand), Direction.UP, mand, false));
                }
                helper.assertTrue(MandBlock.gehad(p, mand) && !MandBlock.pak(p, mand)
                        && p.getInventory().getNonEquipmentItems().stream().mapToInt(ItemStack::getCount).sum() == alles, "the same mand gives p nothing more");
                helper.assertTrue(!MandBlock.gehad(q, mand) && tel(q, koek) == 0 && MandBlock.pak(q, mand) && tel(q, koek) >= 2, "q still gets their own");
                helper.assertTrue(MandBlock.pak(p, mand.offset(500, 0, 0)) && MandBlock.gehad(p, mand), "the mand of another picknickeilandje is a new treat, and the first is remembered");
                long[] veel = new long[MandBlock.ONTHOUD + 30];
                GuhQuests.saved(p).putLongArray(MandBlock.GEHAD, veel);
                MandBlock.pak(p, mand.offset(900, 0, 0));
                helper.assertTrue(GuhQuests.saved(p).getLongArray(MandBlock.GEHAD).orElseThrow().length == MandBlock.ONTHOUD, "the memory of manden stays small");

                // --- lanterns in the island's own tree ---
                int lampen = MandBlock.versier(level, mand, kijk);
                helper.assertTrue(lampen >= 3 && lampen <= MandBlock.LAMPIONNEN, "paper lanterns hang in the tree: " + lampen);
                int hangend = 0;
                for (BlockPos pos : BlockPos.betweenClosed(mand.offset(-8, 2, -8), mand.offset(8, 12, 8))) {
                    BlockState s = level.getBlockState(pos);
                    if (s.getBlock() instanceof LanternBlock && s.getValue(LanternBlock.HANGING)) {
                        hangend++;
                        helper.assertTrue(level.getBlockState(pos.above()).is(BlockTags.LOGS) && s.canSurvive(level, pos), "a lantern hangs from a twig: " + pos);
                        helper.assertTrue(pos.getY() >= mand.getY() + MandBlock.LAMP_LAAG - 1, "above head height: " + pos);
                    }
                }
                helper.assertTrue(hangend == lampen, "all of them hang: " + hangend + " of " + lampen);
                helper.assertTrue(MandBlock.versier(level, mand, kijk) == lampen, "decorating again adds nothing: " + MandBlock.versier(level, mand, kijk));
            } finally {
                level.getEntitiesOfClass(ItemEntity.class, box).forEach(Entity::discard);
                klaar(helper, box, p, q);
            }
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the data: structures, sets, the Superkompas, proofs, recipes, drops
    // =====================================================================================================================
    @GuhTest(template = "empty", batch = BATCH)
    public static void bioBouwMeerData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (String id : List.of("botenhuisje", "picknickeilandje")) {
            helper.assertTrue(structures.containsKey(Guhs.id(id)) && BuiltInRegistries.STRUCTURE_TYPE.getKey(structures.getValue(Guhs.id(id)).type()).equals(Guhs.id("bio_plek")),
                    id + " is a guhs:bio_plek structure");
            helper.assertTrue(sets.containsKey(Guhs.id(id)) && sets.getValue(Guhs.id(id)).placement() instanceof RandomSpreadStructurePlacement, id + " has a random set");
            helper.assertTrue(!sets.containsKey(Guhs.id(id + "_gegarandeerd")), "no guaranteed copy of " + id);
            helper.assertTrue(SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals("knus") && c.structures().contains(id)), id + " is in the Superkompas tab knus");
        }
        helper.assertTrue(structures.getValue(Guhs.id("botenhuisje")).biomes().stream().anyMatch(b -> b.is(Bio.BLOESEMMEERTJE))
                && structures.getValue(Guhs.id("botenhuisje")).biomes().stream().anyMatch(b -> b.is(Bio.KLATERDAL)), "the botenhuisje's shore may lie in the lake or the valley floor");
        helper.assertTrue(structures.getValue(Guhs.id("picknickeilandje")).biomes().stream().allMatch(b -> b.is(Bio.BLOESEMMEERTJE)), "the picknickeilandje only in the lake");
        for (String naam : BouwMeerSlice.BEWIJZEN) {
            helper.assertTrue(level.getServer().getAdvancements().get(Guhs.id("quest/" + naam)) != null, "proof advancement " + naam);
        }
        for (String id : List.of("botenhuisje_steigerlantaarn", "botenhuisje_hengelstandaard", "botenhuisje_koiwindzak")) {
            helper.assertTrue(level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).isPresent(), "recipe " + id);
        }
        BlockPos hoog = helper.absolutePos(new BlockPos(1, 3, 1));
        for (var blok : List.of(BouwMeerSlice.STEIGERLANTAARN, BouwMeerSlice.HENGELSTANDAARD, BouwMeerSlice.KOIWINDZAK)) {
            helper.assertTrue(Block.getDrops(blok.get().defaultBlockState(), level, hoog, null).stream().anyMatch(d -> d.is(blok.get().asItem())), blok.getId() + " drops itself");
        }
        for (var blok : List.of(BouwMeerSlice.MEERPAAL, BouwMeerSlice.MAND)) {
            helper.assertTrue(blok.get().defaultDestroyTime() < 0 && Block.getDrops(blok.get().defaultBlockState(), level, hoog, null).isEmpty() && blok.get().asItem() == Items.AIR,
                    blok.getId() + " is part of its building: unbreakable, no item");
        }
        helper.assertTrue(BouwMeerSlice.ROEIBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED).getPickResult().isEmpty(), "the roeibootje is no item");
        helper.succeed();
    }
}
