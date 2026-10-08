package nl.juiced.guhs.feature.bio.bouwwolk1;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.sterrenwacht.SterrenwachtFeature;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.Nbt;

/**
 * biomes3 slice bouw-wolk1 (batch {@code bio_bouw_wolk1}): the wolkenhoeder's lesson for two players apart, the
 * schaapje to take home, the fold's herd, the haven's daily ride and what happens when a ride goes wrong, the
 * sterrenkijkerguh by day and night and his sterrenstof, and the data (templates, structures, tabs, roles).
 * The real dimension (the islands in the world) is the dev server's: {@code /guhs bio bouw-wolk1 inspecteer}.
 */
public final class BioBouwWolk1GameTests {
    private static final String BATCH = "bio_bouw_wolk1", WEI = "wolkenhoeder_hut_test_wei", VAART = "luchtballon_haven_test_vaart", LEEG = "empty";

    private static ServerPlayer speler(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        p.snapTo(at.x, at.y, at.z);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            p.stopRiding();
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, int x, int y, int z, String plek) {
        GuhNpcEntity n = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(x, y, z));
        n.setKind(kind);
        if (plek != null) {
            n.roleData.putString(NpcRollen.PLEK, plek);
        }
        return n;
    }

    private static int tel(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    private static boolean bewijs(ServerPlayer p, String naam) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static Mob schaapje(GameTestHelper helper, double x, double y, double z) {
        ServerLevel level = helper.getLevel();
        Mob m = (Mob) Kudde.soort().orElseThrow().create(level, EntitySpawnReason.TRIGGERED);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        m.snapTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(m);
        return m;
    }

    // --- the wolkenhoeder ---------------------------------------------------------------------------------------------

    /** Two players go through the lesson apart: every step counts for the one who did it only, the reward comes once each. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void bioBouwWolk1Les(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity hoeder = npc(helper, GuhNpcEntity.Kind.WOLKENHOEDER, 6, 2, 6, Hoeder.PLEK);
        GuhNpcEntity kapel = npc(helper, GuhNpcEntity.Kind.WOLKENHOEDER, 10, 2, 10, null);
        ServerPlayer a = speler(helper, 4.5, 2, 4.5), b = speler(helper, 8.5, 2, 4.5);
        Mob schaap = schaapje(helper, 3.5, 2, 8.5);
        Item pluis = Bio.item("wolkenpluis", Items.AIR), lamp = Bio.item("wolkenlamp", Items.AIR);
        Block wolk = Bio.blok("wolkenblok_wit", Blocks.AIR), roze = Bio.blok("wolkenblok_roze_plaat", Blocks.AIR);
        try {
            helper.assertTrue(pluis != Items.AIR && lamp != Items.AIR && wolk != Blocks.AIR && roze != Blocks.AIR, "the cloud blocks of slice blokken-wolk are there");
            helper.assertTrue(NpcRollen.van(hoeder) instanceof Hoeder, "at the hut (guhs_plek) the wolkenhoeder is the cloud teacher");
            helper.assertTrue(!(NpcRollen.van(kapel) instanceof Hoeder) && NpcRollen.van(kapel) instanceof nl.juiced.guhs.feature.hemel.Wolkenhoeder,
                    "without the place he keeps his role of the hemelkapelletje");
            NpcRollen.rol(GuhNpcEntity.Kind.WOLKENHOEDER).talk(hoeder, a);
            helper.assertTrue(Hoeder.stap(a) == Hoeder.NIET && bewijs(a, Bewijs.HUT_GEVONDEN) && !bewijs(b, Bewijs.HUT_GEVONDEN), "talking: found the hut, nothing begun yet");
            // steps before the lesson began do nothing
            Hoeder.geschoren(a);
            Hoeder.gerust(a);
            helper.assertTrue(Hoeder.stap(a) == Hoeder.NIET, "nothing counts before the player said yes");
            NpcRollen.rol(GuhNpcEntity.Kind.WOLKENHOEDER).antwoord(hoeder, a, Hoeder.LATER);
            helper.assertTrue(Hoeder.stap(a) == Hoeder.NIET, "'later' begins nothing");
            NpcRollen.rol(GuhNpcEntity.Kind.WOLKENHOEDER).antwoord(hoeder, a, Hoeder.JA);
            helper.assertTrue(Hoeder.stap(a) == Hoeder.KNIPPEN && tel(a, Items.SHEARS) == 1 && Hoeder.stap(b) == Hoeder.NIET, "yes: lesson one, and shears for who has none");
            // 1: shearing (the click that is going to shear a wolkenschaapje)
            ItemStack schaar = new ItemStack(Items.SHEARS);
            helper.assertTrue(BouwWolk1Events.isScheren(a, schaap, schaar) && !BouwWolk1Events.isScheren(a, schaap, new ItemStack(Items.STICK))
                    && !BouwWolk1Events.isScheren(a, hoeder, schaar), "shears on a fluffy wolkenschaapje is shearing");
            Hoeder.gemaakt(a, new ItemStack(wolk));
            helper.assertTrue(Hoeder.stap(a) == Hoeder.KNIPPEN, "a later step does not count early");
            Hoeder.geschoren(a);
            helper.assertTrue(Hoeder.stap(a) == Hoeder.MAKEN && Hoeder.stap(b) == Hoeder.NIET, "sheared: lesson two, for this player only");
            // 2: making a cloud block
            Hoeder.gemaakt(a, new ItemStack(Items.STICK));
            helper.assertTrue(Hoeder.stap(a) == Hoeder.MAKEN, "a stick is no cloud");
            Hoeder.gemaakt(a, new ItemStack(wolk, 4));
            helper.assertTrue(Hoeder.stap(a) == Hoeder.BOUWEN, "a cloud block made: lesson three");
            // 3: a stair of three cloud pieces
            BlockPos s0 = new BlockPos(9, 2, 8), s1 = new BlockPos(10, 3, 8), s2 = new BlockPos(11, 4, 8);
            helper.setBlock(s0, wolk);
            helper.setBlock(s1, roze);
            Hoeder.geplaatst(a, level, helper.absolutePos(s1));
            helper.assertTrue(Hoeder.stap(a) == Hoeder.BOUWEN && !Hoeder.trapje(level, helper.absolutePos(s1)), "two steps are not a stair yet");
            helper.setBlock(new BlockPos(9, 2, 9), wolk);
            helper.setBlock(new BlockPos(9, 2, 10), wolk);
            helper.assertTrue(!Hoeder.trapje(level, helper.absolutePos(new BlockPos(9, 2, 9))), "three in a flat row are not a stair");
            helper.setBlock(s2, wolk);
            helper.assertTrue(Hoeder.trapje(level, helper.absolutePos(s0)) && Hoeder.trapje(level, helper.absolutePos(s1)) && Hoeder.trapje(level, helper.absolutePos(s2)),
                    "three cloud pieces (blocks, slabs, either colour), each one up and one along: a stair, seen from every step");
            Hoeder.geplaatst(b, level, helper.absolutePos(s2));
            helper.assertTrue(Hoeder.stap(b) == Hoeder.NIET, "somebody else's stair does not begin your lesson");
            Hoeder.geplaatst(a, level, helper.absolutePos(s2));
            helper.assertTrue(Hoeder.stap(a) == Hoeder.RUSTEN, "the stair stands: lesson four");
            // 4: resting
            helper.assertTrue(Hoeder.isRustplek(Bio.blok("wolkenbank", Blocks.AIR).defaultBlockState()) && Hoeder.isRustplek(Bio.blok("wolkenbed", Blocks.AIR).defaultBlockState())
                    && !Hoeder.isRustplek(Blocks.OAK_STAIRS.defaultBlockState()) && !Hoeder.isRustplek(wolk.defaultBlockState()), "a cloud bench and a cloud bed are places to rest");
            BlockPos bank = new BlockPos(2, 2, 2);
            helper.setBlock(bank, Bio.blok("wolkenbank", Blocks.AIR));
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(helper.absolutePos(bank)), net.minecraft.core.Direction.UP, helper.absolutePos(bank), false);
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(a, InteractionHand.MAIN_HAND, helper.absolutePos(bank), hit));
            helper.assertTrue(Hoeder.stap(a) == Hoeder.BELONING && tel(a, pluis) == 0, "clicking the cloud bench: rested, now back to the wolkenhoeder");
            NpcRollen.rol(GuhNpcEntity.Kind.WOLKENHOEDER).talk(hoeder, a);
            helper.assertTrue(Hoeder.stap(a) == Hoeder.KLAAR && tel(a, pluis) == Hoeder.PLUIS && tel(a, lamp) == 1 && bewijs(a, Bewijs.HUT_LES) && !bewijs(b, Bewijs.HUT_LES),
                    "the reward: wolkenpluis and a wolkenlamp, and the proof");
            NpcRollen.rol(GuhNpcEntity.Kind.WOLKENHOEDER).talk(hoeder, a);
            Hoeder.beloon(a);
            helper.assertTrue(tel(a, pluis) == Hoeder.PLUIS && tel(a, lamp) == 1, "the reward comes once");
            // the second player, in their own time; with shears and pluis already in the bag
            b.getInventory().add(new ItemStack(Items.SHEARS));
            NpcRollen.rol(GuhNpcEntity.Kind.WOLKENHOEDER).antwoord(hoeder, b, Hoeder.JA);
            helper.assertTrue(Hoeder.stap(b) == Hoeder.KNIPPEN && tel(b, Items.SHEARS) == 1, "who has shears gets no second pair");
            b.getInventory().add(new ItemStack(pluis, 2));
            NpcRollen.rol(GuhNpcEntity.Kind.WOLKENHOEDER).talk(hoeder, b);
            helper.assertTrue(Hoeder.stap(b) == Hoeder.MAKEN && Hoeder.stap(a) == Hoeder.KLAAR, "pluis in the bag: the first lesson is done already; the other player stays done");
            Hoeder.gemaakt(b, new ItemStack(roze));
            Hoeder.geplaatst(b, level, helper.absolutePos(s0));
            Hoeder.gerust(b);
            NpcRollen.rol(GuhNpcEntity.Kind.WOLKENHOEDER).talk(hoeder, b);
            helper.assertTrue(Hoeder.stap(b) == Hoeder.KLAAR && tel(b, pluis) == 2 + Hoeder.PLUIS && bewijs(b, Bewijs.HUT_LES), "the second player finished too, with their own reward");
        } finally {
            weg(helper, a, b);
            schaap.discard();
            hoeder.discard();
            kapel.discard();
        }
        helper.succeed();
    }

    /** One schaapje to take home per player, only after the lesson, on a lead, never one of the fold; it stays for good. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void bioBouwWolk1Schaapje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity hoeder = npc(helper, GuhNpcEntity.Kind.WOLKENHOEDER, 6, 2, 6, Hoeder.PLEK);
        ServerPlayer a = speler(helper, 4.5, 2, 4.5), b = speler(helper, 8.5, 2, 4.5);
        Predicate<Mob> vanA = m -> m.getLeashHolder() == a;
        helper.assertTrue(!Hoeder.geefSchaapje(hoeder, a) && !Hoeder.heeftSchaapje(a), "no schaapje before the lesson is finished");
        Hoeder.zetStap(a, Hoeder.KLAAR);
        Hoeder.zetStap(b, Hoeder.KLAAR);
        int voor = level.getEntitiesOfClass(Mob.class, hoeder.getBoundingBox().inflate(12), m -> Kudde.soort().orElseThrow() == m.getType()).size();
        helper.assertTrue(Hoeder.geefSchaapje(hoeder, a) && Hoeder.heeftSchaapje(a) && bewijs(a, Bewijs.HUT_SCHAAPJE) && !Hoeder.heeftSchaapje(b), "the schaapje, for this player");
        List<Mob> nu = level.getEntitiesOfClass(Mob.class, hoeder.getBoundingBox().inflate(12), m -> Kudde.soort().orElseThrow() == m.getType());
        helper.assertTrue(nu.size() == voor + 1 && nu.stream().filter(vanA).count() == 1, "one new wolkenschaapje, on a lead in the hand of the player");
        Mob mijn = nu.stream().filter(vanA).findFirst().orElseThrow();
        helper.assertTrue(mijn.isPersistenceRequired() && !Kudde.isKudde(mijn) && mijn.shouldBeSaved(), "it is saved and it is not one of the fold");
        helper.assertTrue(!Hoeder.geefSchaapje(hoeder, a) && level.getEntitiesOfClass(Mob.class, hoeder.getBoundingBox().inflate(12),
                m -> Kudde.soort().orElseThrow() == m.getType()).size() == voor + 1, "once per player: asking again gives nothing");
        helper.assertTrue(GuhQuests.saved(a).getBooleanOr(Hoeder.SCHAAPJE, false), "remembered in the saved data of the player");
        helper.assertTrue(Hoeder.geefSchaapje(hoeder, b) && Hoeder.heeftSchaapje(b), "the other player gets their own");
        helper.succeedWhen(() -> {
            // (slice dieren: a wolkenschaapje on a lead becomes kept within a second: never a come-and-go animal again)
            CompoundTag tag = Nbt.saveWithoutId(mijn);
            helper.assertTrue(tag.getBooleanOr("Gehouden", false) && tag.getBooleanOr("PersistenceRequired", false), "kept for good after a moment on the lead");
            Mob terug = (Mob) Kudde.soort().orElseThrow().create(level, EntitySpawnReason.LOAD);
            Nbt.load(terug, tag);
            helper.assertTrue(terug.isPersistenceRequired() && terug.shouldBeSaved(), "still the player's after saving and loading");
            terug.discard();
            for (Mob m : level.getEntitiesOfClass(Mob.class, hoeder.getBoundingBox().inflate(16), m -> Kudde.soort().orElseThrow() == m.getType())) {
                m.discard();
            }
            hoeder.discard();
            weg(helper, a, b);
        });
    }

    /** The fold's herd: never more (no lammetjes), never away (no lead; strays are put back), refilled only when really gone. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void bioBouwWolk1Kudde(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity hoeder = npc(helper, GuhNpcEntity.Kind.WOLKENHOEDER, 9, 2, 6, Hoeder.PLEK);
        ServerPlayer p = speler(helper, 9.5, 2, 9.5);
        Vec3 wei = helper.absoluteVec(new Vec3(5.5, 2, 6.5));
        try {
            helper.assertTrue(Kudde.hoed(level, hoeder)[1] == 0 && Kudde.kudde(level, hoeder).isEmpty(), "no herd seen yet: the wolkenhoeder does not know his fold and calls nothing");
            for (int i = 0; i < Kudde.AANTAL; i++) {
                Kudde.roep(level, wei, i);
            }
            Mob wild = schaapje(helper, 10.5, 2, 2.5);
            List<Mob> kudde = Kudde.kudde(level, hoeder);
            helper.assertTrue(kudde.size() == Kudde.AANTAL && !kudde.contains(wild), "the herd of four; a wild one is not of the herd");
            for (Mob m : kudde) {
                helper.assertTrue(m.isPersistenceRequired() && m.shouldBeSaved() && Nbt.saveWithoutId(m).getBooleanOr("Gehouden", false) && Kudde.isKudde(m),
                        "a herd schaapje is kept and saved (dieren never tidies it up)");
            }
            int[] r = Kudde.hoed(level, hoeder);
            helper.assertTrue(r[0] == 0 && r[1] == 0 && Kudde.kudde(level, hoeder).size() == Kudde.AANTAL, "all at home: nothing to do");
            // never more: no lammetje from a herd schaapje, whoever feeds them
            AgeableMob lam = (AgeableMob) Kudde.soort().orElseThrow().create(level, EntitySpawnReason.BREEDING);
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BabyEntitySpawnEvent(kudde.get(0), kudde.get(1), lam)).isCanceled(), "two of the herd get no lammetje");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BabyEntitySpawnEvent(kudde.get(0), wild, lam)).isCanceled(), "one of the herd with a wild one neither");
            AgeableMob lam2 = (AgeableMob) Kudde.soort().orElseThrow().create(level, EntitySpawnReason.BREEDING);
            Mob wild2 = schaapje(helper, 11.5, 2, 2.5);
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new BabyEntitySpawnEvent(wild, wild2, lam2)).isCanceled(), "other wolkenschaapjes are left alone");
            lam.discard();
            lam2.discard();
            wild2.discard();
            // never away: no lead
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));
            helper.assertTrue(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(p, InteractionHand.MAIN_HAND, kudde.get(2))).isCanceled(),
                    "a lead does not go on a herd schaapje");
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(p, InteractionHand.MAIN_HAND, wild)).isCanceled(), "on a wild one it does");
            // never away: a stray is put back (pushed out of the gate, floated off the island)
            Mob zwerver = kudde.get(3);
            zwerver.teleportTo(wei.x + 3, wei.y + 12, wei.z + 20);
            kudde.get(2).setLeashedTo(p, true);
            r = Kudde.hoed(level, hoeder);
            helper.assertTrue(r[0] == 1 && r[1] == 0 && Math.hypot(zwerver.getX() - wei.x, zwerver.getZ() - wei.z) < Kudde.LOS && Math.abs(zwerver.getY() - wei.y) < 1
                    && !kudde.get(2).isLeashed(), "the stray is back in the fold, a lead that got on anyway is off");
            helper.assertTrue(Kudde.kudde(level, hoeder).size() == Kudde.AANTAL, "still four");
            // refilled only when really gone, and never above four
            kudde.get(0).discard();
            r = Kudde.hoed(level, hoeder);
            helper.assertTrue(r[1] == 0 && Kudde.kudde(level, hoeder).size() == Kudde.AANTAL - 1, "missed once: he waits (the chunk may still be loading)");
            r = Kudde.hoed(level, hoeder);
            helper.assertTrue(r[1] == 1 && Kudde.kudde(level, hoeder).size() == Kudde.AANTAL, "missed twice: he calls one new schaapje");
            for (int i = 0; i < 4; i++) {
                r = Kudde.hoed(level, hoeder);
            }
            helper.assertTrue(r[1] == 0 && Kudde.kudde(level, hoeder).size() == Kudde.AANTAL && wild.isAlive(), "and then it stays four: nothing piles up");
        } finally {
            for (Mob m : level.getEntitiesOfClass(Mob.class, hoeder.getBoundingBox().inflate(80), m -> Kudde.soort().orElseThrow() == m.getType())) {
                m.discard();
            }
            hoeder.discard();
            weg(helper, p);
        }
        helper.succeed();
    }

    // --- the haven ----------------------------------------------------------------------------------------------------

    private static HavenBallonEntity ballon(GameTestHelper helper) {
        BlockPos rel = new BlockPos(6, 22, 3);
        HavenBallonEntity b = helper.spawn(BouwWolk1Slice.HAVEN_BALLON.get(), rel);
        b.setThuis(helper.absolutePos(rel), 0);          // (looks south: out over the edge of the test platform)
        b.setYRot(0);
        return b;
    }

    /** One ride a day per player: down to the floor, the balloon back at its mooring, then "tomorrow". */
    @GuhTest(template = VAART, batch = BATCH, timeoutTicks = 600)
    public static void bioBouwWolk1Vaart(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HavenBallonEntity.tempo = 6;
        GuhNpcEntity vaarder = npc(helper, GuhNpcEntity.Kind.BALLONVAARDERGUH, 5, 22, 2, null);
        HavenBallonEntity ballon = ballon(helper);
        ServerPlayer a = speler(helper, 5.5, 22, 4.5), b = speler(helper, 7.5, 22, 4.5);
        double vloer = helper.absoluteVec(new Vec3(0, 2, 0)).y;
        Vec3 thuis = ballon.thuis();
        helper.assertTrue(NpcRollen.van(vaarder) instanceof Ballonvaarder, "the ballonvaarder's role");
        vaarder.roleData.putLong("BallonThuis", ballon.thuisBlok().asLong());       // (he knows his mooring: tests stand side by side)
        helper.assertTrue(Ballonvaarder.vaar(vaarder, a) == null && a.getVehicle() == ballon && ballon.fase() == HavenBallonEntity.OMLAAG && Ballonvaarder.heeftGevaren(a),
                "the first ride of the day: off");
        helper.assertTrue("weg".equals(Ballonvaarder.vaar(vaarder, b)) && !Ballonvaarder.heeftGevaren(b) && b.getVehicle() == null,
                "the balloon is under way: the next player waits and keeps their ride");
        helper.assertTrue(HavenBallonEntity.Landing.zoek(level, thuis, ballon.uit()) != null && ballon.doel().y == vloer, "it goes to the floor below");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(a.getVehicle() == null && ballon.fase() == HavenBallonEntity.TERUG, "landed: the passenger is out, the balloon goes back up"))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(a.getY() - vloer) < 0.6 && a.position().distanceTo(ballon.doel()) < 1.5 && a.fallDistance == 0,
                            "the player stands on the floor at the landing spot: " + a.position() + " floor " + vloer);
                    helper.assertTrue(bewijs(a, Bewijs.HAVEN_VAART) && !bewijs(b, Bewijs.HAVEN_VAART), "the proof of the first ride");
                })
                .thenWaitUntil(() -> helper.assertTrue(ballon.fase() == HavenBallonEntity.RUST && ballon.position().distanceTo(thuis) < 0.1, "the balloon is back at its mooring"))
                .thenExecute(() -> {
                    Vec3 boven = helper.absoluteVec(new Vec3(5.5, 22, 4.5));
                    a.teleportTo(boven.x, boven.y, boven.z);
                    helper.assertTrue("al".equals(Ballonvaarder.vaar(vaarder, a)) && a.getVehicle() == null && !ballon.onderweg(), "one ride per day: not again today");
                    GuhQuests.saved(a).putLong(Ballonvaarder.DAG, Ballonvaarder.dag(level));          // (the ride was yesterday)
                    helper.assertTrue(!Ballonvaarder.heeftGevaren(a), "a new day: a new ride");
                    helper.assertTrue(Ballonvaarder.vaar(vaarder, b) == null && b.getVehicle() == ballon, "the other player rides the same day, their own ride");
                    helper.assertTrue("weg".equals(Ballonvaarder.vaar(vaarder, a)) && !Ballonvaarder.heeftGevaren(a), "and the first waits for the balloon");
                })
                .thenWaitUntil(() -> helper.assertTrue(b.getVehicle() == null && ballon.fase() == HavenBallonEntity.RUST, "the second ride done, the balloon home again"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getEntitiesOfClass(HavenBallonEntity.class, vaarder.getBoundingBox().inflate(40), e -> !e.isRemoved() && e.thuis().distanceTo(thuis) < 1).size() == 1,
                            "one balloon at this mooring, no orphans");
                    HavenBallonEntity.tempo = 1;
                    ballon.discard();
                    vaarder.discard();
                    weg(helper, a, b);
                })
                .thenSucceed();
    }

    /** What can go wrong on a ride: no landing spot, logging out, the balloon removed, a restart, the landing spot blocked. */
    @GuhTest(template = VAART, batch = BATCH, timeoutTicks = 900)
    public static void bioBouwWolk1VaartVeilig(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HavenBallonEntity.tempo = 6;
        GuhNpcEntity vaarder = npc(helper, GuhNpcEntity.Kind.BALLONVAARDERGUH, 5, 22, 2, null);
        HavenBallonEntity[] ballon = {ballon(helper)};
        ServerPlayer a = speler(helper, 5.5, 22, 4.5);
        Vec3 thuis = ballon[0].thuis(), uit = ballon[0].uit();
        Vec3 landing = HavenBallonEntity.Landing.zoek(level, thuis, uit);
        helper.assertTrue(landing != null && HavenBallonEntity.Landing.wegVrij(level, thuis, uit, landing), "a landing spot and a free way to it");
        vaarder.roleData.putLong("BallonThuis", ballon[0].thuisBlok().asLong());   // (tests stand side by side: his own mooring)
        helper.assertTrue(Ballonvaarder.onderhoud(level, vaarder) == null, "the ballonvaarder notes where his balloon is moored");
        // the curve: starts at the mooring, ends at the landing spot, never rises far, never leaves the room sideways
        for (int i = 0; i <= 20; i++) {
            Vec3 p = HavenBallonEntity.punt(thuis, uit, landing, i / 20.0);
            helper.assertTrue(p.y <= thuis.y + 2 && p.y >= landing.y - 1e-6 && Math.abs(p.x - thuis.x) < 4, "the curve stays between mooring and landing: " + p);
        }
        helper.assertTrue(HavenBallonEntity.punt(thuis, uit, landing, 0).distanceTo(thuis) < 1e-6 && HavenBallonEntity.punt(thuis, uit, landing, 1).distanceTo(landing) < 1e-6,
                "from the mooring to the landing spot");
        // 1. no landing spot (water all over the floor under it): no ride, the day is not used
        BlockPos l = BlockPos.containing(landing);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                level.setBlock(l.offset(dx, 0, dz), Blocks.WATER.defaultBlockState(), 2);
            }
        }
        helper.assertTrue(!HavenBallonEntity.Landing.vrij(level, landing) && HavenBallonEntity.Landing.zoek(level, thuis, uit) == null, "water is no landing spot");
        helper.assertTrue("geblokkeerd".equals(Ballonvaarder.vaar(vaarder, a)) && !Ballonvaarder.heeftGevaren(a) && a.getVehicle() == null && !ballon[0].onderweg(),
                "no landing spot: no ride, and the ride of the day is kept");
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                level.setBlock(l.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        // 1b. something in the way of the curve: no ride either
        BlockPos halverwege = BlockPos.containing(HavenBallonEntity.punt(thuis, uit, landing, 0.5));
        level.setBlock(halverwege, Blocks.GLASS.defaultBlockState(), 2);
        helper.assertTrue("geblokkeerd".equals(Ballonvaarder.vaar(vaarder, a)) && !Ballonvaarder.heeftGevaren(a), "the way is blocked: no ride");
        level.setBlock(halverwege, Blocks.AIR.defaultBlockState(), 2);
        // 2. logging out in flight
        helper.assertTrue(Ballonvaarder.vaar(vaarder, a) == null && a.getVehicle() == ballon[0], "off");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ballon[0].voortgang() > 0.35 && ballon[0].getY() < thuis.y - 3, "in the air, well below the jetty"))
                .thenExecute(() -> {
                    a.setShiftKeyDown(true);
                    a.stopRiding();
                    helper.assertTrue(a.getVehicle() == ballon[0], "sneaking does not get you out of the basket in flight");
                    a.setShiftKeyDown(false);
                    helper.assertTrue(Ballonvaarder.uitloggen(a), "logging out in flight");
                    helper.assertTrue(a.getVehicle() == null && a.position().distanceTo(ballon[0].steiger()) < 0.01 && !ballon[0].onderweg()
                            && ballon[0].position().distanceTo(thuis) < 0.01 && ballon[0].getPassengers().isEmpty(), "the player stands on the jetty, the balloon is at its mooring");
                    helper.assertTrue(!Ballonvaarder.heeftGevaren(a) && !Ballonvaarder.uitloggen(a), "the ride is given back; logging out at rest does nothing");
                    // 3. a restart in flight: the flight is not saved, a loaded balloon is at rest at its mooring
                    helper.assertTrue(Ballonvaarder.vaar(vaarder, a) == null, "off again");
                })
                .thenWaitUntil(() -> helper.assertTrue(ballon[0].voortgang() > 0.35, "in the air"))
                .thenExecute(() -> {
                    HavenBallonEntity geladen = BouwWolk1Slice.HAVEN_BALLON.get().create(level, EntitySpawnReason.LOAD);
                    Nbt.load(geladen, Nbt.saveWithoutId(ballon[0]));
                    helper.assertTrue(!geladen.onderweg() && geladen.thuisBlok().equals(ballon[0].thuisBlok()) && geladen.kleur() == ballon[0].kleur(),
                            "saved in flight and loaded: at rest, with its mooring");
                    geladen.discard();
                    // 4. the balloon removed in flight: the passenger floats down, the ballonvaarder moors a new balloon
                    ballon[0].discard();
                    helper.assertTrue(a.getVehicle() == null && a.hasEffect(MobEffects.SLOW_FALLING), "the balloon is gone: its passenger floats down, never falls");
                    helper.assertTrue(Ballonvaarder.onderhoud(level, vaarder) == null, "gone once: he waits");
                    HavenBallonEntity nieuw = Ballonvaarder.onderhoud(level, vaarder);
                    helper.assertTrue(nieuw != null, "gone twice: a new balloon");
                    helper.assertTrue(nieuw.thuis().distanceTo(thuis) < 0.01 && !nieuw.onderweg(), "at the same mooring, at rest: " + nieuw.thuis() + " " + thuis);
                    helper.assertTrue(Ballonvaarder.onderhoud(level, vaarder) == null && Ballonvaarder.onderhoud(level, vaarder) == null
                            && level.getEntitiesOfClass(HavenBallonEntity.class, vaarder.getBoundingBox().inflate(40),
                            e -> !e.isRemoved() && e.thuis().distanceTo(thuis) < 1).size() == 1, "and no more after that (one balloon at this mooring)");
                    ballon[0] = nieuw;
                    // 5. the landing spot gets blocked while flying: back up with the passenger, out on the jetty, the ride given back
                    a.removeEffect(MobEffects.SLOW_FALLING);
                    Vec3 boven = helper.absoluteVec(new Vec3(5.5, 22, 4.5));
                    a.teleportTo(boven.x, boven.y, boven.z);
                    Ballonvaarder.zetGevaren(a, false);
                    helper.assertTrue(Ballonvaarder.vaar(vaarder, a) == null && a.getVehicle() == nieuw, "off with the new balloon");
                    for (int dy = 0; dy < 3; dy++) {
                        level.setBlock(l.above(dy), Blocks.GLASS.defaultBlockState(), 2);
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(ballon[0].fase() == HavenBallonEntity.TERUG_MET && a.getVehicle() == ballon[0], "blocked: it turns back with the passenger in it"))
                .thenWaitUntil(() -> helper.assertTrue(ballon[0].fase() == HavenBallonEntity.RUST && a.getVehicle() == null, "home"))
                .thenExecute(() -> {
                    helper.assertTrue(a.position().distanceTo(ballon[0].steiger()) < 0.01 && !Ballonvaarder.heeftGevaren(a) && !bewijs(a, Bewijs.HAVEN_VAART),
                            "the passenger is let out on the jetty and keeps the ride of the day");
                    for (int dy = 0; dy < 3; dy++) {
                        level.setBlock(l.above(dy), Blocks.AIR.defaultBlockState(), 2);
                    }
                    HavenBallonEntity.tempo = 1;
                    ballon[0].discard();
                    vaarder.discard();
                    weg(helper, a);
                })
                .thenSucceed();
    }

    // --- the ruin -----------------------------------------------------------------------------------------------------

    /** Asleep by day, dreamy at night; sterrenstof from his telescope once per night per player. */
    @GuhTest(template = LEEG, batch = BATCH)
    public static void bioBouwWolk1Sterrenkijker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity kijker = npc(helper, GuhNpcEntity.Kind.STERRENWACHT_RUINE_STERRENKIJKER, 3, 2, 3, null);
        ServerPlayer a = speler(helper, 2.5, 2, 5.5), b = speler(helper, 4.5, 2, 5.5);
        BlockPos telescoop = helper.absolutePos(new BlockPos(4, 2, 3));
        helper.setBlock(new BlockPos(4, 2, 3), SterrenwachtFeature.TELESCOOP.get());
        Item stof = Bio.item("sterrenstof", Items.AIR);
        Predicate<Level> echt = Sterrenkijker.donker;
        boolean[] nacht = {false};
        Sterrenkijker.donker = l -> nacht[0];
        try {
            helper.assertTrue(stof != Items.AIR && NpcRollen.van(kijker) instanceof Sterrenkijker, "the sterrenstof of the falling stars; his role");
            // by day: asleep
            NpcRollen.rol(kijker.getKind()).talk(kijker, a);
            helper.assertTrue(!Sterrenkijker.wakker(level) && GuhQuests.saved(a).getIntOr(Sterrenkijker.GESPROKEN, 0) == 0 && bewijs(a, Bewijs.RUINE_GEVONDEN),
                    "by day he sleeps: found, but no talk");
            helper.assertTrue(!Sterrenkijker.kijk(a, telescoop) && tel(a, stof) == 0, "by day the telescope gives nothing");
            // at night: awake
            nacht[0] = true;
            NpcRollen.rol(kijker.getKind()).talk(kijker, a);
            helper.assertTrue(Sterrenkijker.wakker(level) && GuhQuests.saved(a).getIntOr(Sterrenkijker.GESPROKEN, 0) == 1, "in the dark he wakes and talks");
            helper.assertTrue(Sterrenkijker.regel(a).endsWith(".nacht0") && Sterrenkijker.regel(a).endsWith(".nacht1") && Sterrenkijker.regel(b).endsWith(".hallo"),
                    "the greeting first, then his lines in turn, per player");
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(telescoop), net.minecraft.core.Direction.UP, telescoop, false);
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(a, InteractionHand.MAIN_HAND, telescoop, hit));
            helper.assertTrue(tel(a, stof) == 1 && bewijs(a, Bewijs.RUINE_STERRENSTOF) && !bewijs(b, Bewijs.RUINE_STERRENSTOF), "looking through his telescope at night: sterrenstof");
            helper.assertTrue(!Sterrenkijker.kijk(a, telescoop) && tel(a, stof) == 1, "once per night");
            helper.assertTrue(Sterrenkijker.kijk(b, telescoop) && tel(b, stof) == 1, "per player");
            long vannacht = nl.juiced.guhs.feature.sterrenwacht.Sterrenkijken.nacht(level);
            helper.assertTrue(Sterrenkijker.geefStof(a, telescoop, vannacht + 1) && tel(a, stof) == 2 && !Sterrenkijker.geefStof(a, telescoop, vannacht + 1),
                    "the next night again, once");
            helper.assertTrue(!Sterrenkijker.kijk(b, telescoop.offset(40, 0, 0)), "a telescope somewhere else is not his");
            nl.juiced.guhs.feature.sterrenwacht.Sterrenkijken.stop(a);
        } finally {
            Sterrenkijker.donker = echt;
            kijker.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    // --- the data -----------------------------------------------------------------------------------------------------

    @GuhTest(template = LEEG, batch = BATCH)
    public static void bioBouwWolk1Data(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var settings = new StructurePlaceSettings();
        var structuren = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String naam : BouwWolk1Slice.STRUCTUREN) {
            var t = level.getStructureManager().get(Guhs.id(naam));
            helper.assertTrue(t.isPresent(), "template " + naam);
            var anker = t.get().filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW, true);
            helper.assertTrue(anker.size() == 1 && anker.get(0).nbt().getStringOr("name", "").equals("guhs:" + naam + "_midden"), naam + ": one anchor");
            helper.assertTrue(structuren.get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(naam))).isPresent(), "structure " + naam);
            helper.assertTrue(t.get().filterBlocks(BlockPos.ZERO, settings, Bio.blok("wolkenlift", Blocks.AIR), true).size() == 18, naam + ": a lift and a stream column (two 3 x 3 pads)");
            int laagste = t.get().filterBlocks(BlockPos.ZERO, settings, Bio.blok("wolkenlift", Blocks.AIR), true).stream().mapToInt(i -> i.pos().getY()).min().orElse(-1);
            helper.assertTrue(laagste == BouwWolk1Commando.VOET, naam + ": the pads lie in the meadow layer, the feet below it: " + laagste);
        }
        var hut = level.getStructureManager().get(Guhs.id("wolkenhoeder_hut")).orElseThrow();
        helper.assertTrue(hut.getSize().getX() >= 56 && hut.filterBlocks(BlockPos.ZERO, settings, Blocks.WATER, true).size() > 80, "the big island with its lake and falls: " + hut.getSize());
        CompoundTag nbt = hut.save(new CompoundTag());
        int schapen = 0, hoeders = 0;
        for (var e : nbt.getListOrEmpty("entities")) {
            CompoundTag en = ((CompoundTag) e).getCompoundOrEmpty("nbt");
            schapen += en.getStringOr("id", "").equals("guhs:wolkenschaapje") && en.toString().contains(Kudde.TAG) && en.getBooleanOr("Gehouden", false) ? 1 : 0;
            hoeders += en.getStringOr("Kind", "").equals("wolkenhoeder") && en.getCompoundOrEmpty("RoleData").getStringOr(NpcRollen.PLEK, "").equals(Hoeder.PLEK) ? 1 : 0;
        }
        helper.assertTrue(schapen == Kudde.AANTAL && hoeders == 1, "the herd of four (tagged, kept) and the wolkenhoeder with his place: " + schapen + " " + hoeders);
        var ruine = level.getStructureManager().get(Guhs.id("sterrenwacht_ruine")).orElseThrow();
        helper.assertTrue(ruine.filterBlocks(BlockPos.ZERO, settings, SterrenwachtFeature.TELESCOOP.get(), true).size() == 1
                && ruine.filterBlocks(BlockPos.ZERO, settings, BouwWolk1Slice.STERRENKAART.get(), true).size() == 1, "the ruin: a telescope and the star chart");
        String haven = level.getStructureManager().get(Guhs.id("luchtballon_haven")).orElseThrow().save(new CompoundTag()).getListOrEmpty("entities").toString();
        helper.assertTrue(haven.split("guhs:luchtballon_haven_ballon", -1).length == 2 && haven.split("guhs:guh_luchtballon", -1).length == 3 && haven.contains("ballonvaarderguh"),
                "the haven: the balloon that flies, two moored ones, the ballonvaarder");
        // tabs of the Superkompas
        for (var c : SuperkompasItem.CATEGORIES) {
            if (c.id().equals("knus")) {
                helper.assertTrue(c.structures().contains("wolkenhoeder_hut") && c.structures().contains("luchtballon_haven") && !c.structures().contains("sterrenwacht_ruine"), "tab knus");
            } else if (c.id().equals("wonderen")) {
                helper.assertTrue(c.structures().contains("sterrenwacht_ruine"), "tab wonderen");
            }
        }
        helper.assertTrue(WolkvoetProcessor.magHier(true) && !WolkvoetProcessor.magHier(false), "a cloud foot block only comes where the world has air");
        helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST).get(ResourceKey.create(Registries.PROCESSOR_LIST, Guhs.id("wolkenhoeder_hut_wolkvoet"))).isPresent(),
                "the processor list");
        for (String a : new String[]{Bewijs.HUT_GEVONDEN, Bewijs.HUT_LES, Bewijs.HUT_SCHAAPJE, Bewijs.RUINE_GEVONDEN, Bewijs.RUINE_STERRENSTOF, Bewijs.HAVEN_GEVONDEN, Bewijs.HAVEN_VAART}) {
            helper.assertTrue(level.getServer().getAdvancements().get(Guhs.id("quest/" + a)) != null, "proof advancement " + a);
        }
        var recepten = level.getServer().getRecipeManager();
        helper.assertTrue(recepten.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id("sterrenwacht_ruine_sterrenkaart"))).isPresent()
                && recepten.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id("sterrenwacht_ruine_sterrenlantaarn"))).isPresent(), "the two sterrenstof recipes");
        helper.succeed();
    }

    private BioBouwWolk1GameTests() {
    }
}
