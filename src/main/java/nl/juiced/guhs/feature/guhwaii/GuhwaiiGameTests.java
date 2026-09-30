package nl.juiced.guhs.feature.guhwaii;

import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.piep.PoepschillyEntity;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of Guhwai'i (feature/guhwaii): the vadsigheid-scanner on any guh, the Schilly-eitjes that hatch, the ohana
 * questline step by step (with the 626-guh tameable once per player), the 626-guh's climbing, ceiling and carrying, his
 * ukelele, the guh-palm (its leaves never decay) and the outfit sources. Templates: guhwaii_test_scanner (a scanner at
 * helper 4,2,4), guhwaii_test_strand (sand, water on the east side), guhwaii_test_huisje (20 x 20 floor), guhwaii_test_muur
 * (a wall on the west side, a ceiling 4 blocks over the floor). A template's y 0 is helper y 1: things stand at helper y 2.
 */
public class GuhwaiiGameTests {
    private static final String BATCH = "guhwaii";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static GuhEntity guh(GameTestHelper helper, double x, double y, double z) {
        GuhEntity g = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 v = helper.absoluteVec(new Vec3(x, y, z));
        g.snapTo(v.x, v.y, v.z, 0f, 0f);
        g.setPersistenceRequired();
        helper.getLevel().addFreshEntity(g);
        return g;
    }

    // =================================================================================================================
    // the scanner
    // =================================================================================================================

    /** Every guh on the plate is ONBEREKENBAAR VAHOEG: a wild one, a tamed 626-guh, a tiny baby; your own guh hops on. */
    @GuhTest(template = "guhwaii_test_scanner", batch = BATCH, timeoutTicks = 400)
    public static void guhwaiiScannerOpElkeGuh(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos scanner = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.assertBlockPresent(GuhwaiiFeature.VADSIGHEID_SCANNER.get(), new BlockPos(4, 2, 4));
        Scanner.vergeet();
        ServerPlayer p = speler(helper, new BlockPos(4, 2, 7));
        GuhEntity wild = guh(helper, 4.5, 2.25, 4.5);
        helper.assertTrue(Scanner.opDePlaat(level, scanner) == wild, "the wild guh stands on the plate");
        Scanner.Meting m = Scanner.gebruik(level, scanner, p, ItemStack.EMPTY);
        helper.assertTrue(m != null && m.guh() == wild && Scanner.RESULTAAT.equals(m.uitkomst()), "a wild guh: ONBEREKENBAAR VAHOEG");
        helper.assertTrue(Scanner.bezig(level, scanner) && Scanner.gebruik(level, scanner, p, ItemStack.EMPTY) == null, "one at a time");
        Scanner.vergeet();
        wild.discard();
        // a tamed 626-guh a few blocks away hops onto the plate by itself
        GuhEntity stitch = guh(helper, 1.5, 2, 1.5);
        stitch.tame(p);
        stitch.setVariant(GuhVariant.STITCH626);
        Scanner.Meting m2 = Scanner.gebruik(level, scanner, p, ItemStack.EMPTY);
        helper.assertTrue(m2 != null && m2.guh() == stitch && stitch.position().distanceTo(Vec3.atBottomCenterOf(scanner)) < 0.6,
                "your own guh hops on and is measured: " + stitch.position());
        helper.assertTrue(Scanner.RESULTAAT.equals(m2.uitkomst()), "626: ONBEREKENBAAR VAHOEG too");
        Scanner.vergeet();
        // a baby from a picked-up guh item
        stitch.discard();
        GuhEntity baby = guh(helper, 7.5, 2, 7.5);
        baby.setBaby(true);
        baby.tame(p);
        ItemStack opgepakt = nl.juiced.guhs.item.PickedUpGuhItem.pickUp(baby);
        baby.discard();
        Scanner.Meting m3 = Scanner.gebruik(level, scanner, p, opgepakt);
        helper.assertTrue(m3 != null && m3.guh().isBaby() && opgepakt.isEmpty() && Scanner.RESULTAAT.equals(m3.uitkomst()),
                "a baby from your hand: ONBEREKENBAAR VAHOEG");
        GuhEntity b = m3.guh();
        helper.succeedWhen(() -> {
            helper.assertTrue(!Scanner.bezig(level, scanner), "the measurement finishes");
            b.discard();
            weg(helper, p);
        });
    }

    // =================================================================================================================
    // the eggs
    // =================================================================================================================

    /** Schilly-eitjes get ready (rijp 0, 1, 2) and hatch: one baby Poepschilly or Schilly per egg. */
    @GuhTest(template = "guhwaii_test_strand", batch = BATCH)
    public static void guhwaiiEitjesKomenUit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockState eitjes = GuhwaiiFeature.SCHILLY_EITJES.get().defaultBlockState().setValue(GuhwaiiBlokken.EITJES, 3);
        helper.assertTrue(eitjes.canSurvive(level, pos), "eggs lie on sand");
        level.setBlock(pos, eitjes, 3);
        List<AgeableMob> babies = List.of();
        for (int i = 0; i < 3 && babies.isEmpty(); i++) {
            babies = SchillyEitjesBlock.broed(level, pos, level.getBlockState(pos));
            if (i < 2) {
                helper.assertTrue(level.getBlockState(pos).getValue(GuhwaiiBlokken.RIJP) == i + 1, "getting ready: " + (i + 1));
            }
        }
        helper.assertTrue(babies.size() == 3 && babies.stream().allMatch(b -> b instanceof PoepschillyEntity && b.isBaby() && b.isAlive()),
                "three baby turtles: " + babies);
        helper.assertTrue(level.getBlockState(pos).isAir(), "the shells are gone");
        for (AgeableMob b : babies) {
            b.discard();
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the ohana questline
    // =================================================================================================================

    /** The whole questline: Lilo, the capsule, the adoption, cleaning up, being lief, ohana, and 626-guh once per player. */
    @GuhTest(template = "guhwaii_test_huisje", batch = BATCH, timeoutTicks = 200)
    public static void guhwaiiOhanaStappen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(10, 2, 13)), q = speler(helper, new BlockPos(12, 2, 13));
        Ohana.vergeet(p);
        Ohana.vergeet(q);
        GuhNpcEntity lilo = npc(helper, GuhNpcEntity.Kind.LILO_GUH, 8, 10);
        GuhNpcEntity nani = npc(helper, GuhNpcEntity.Kind.NANI_GUH, 10, 10);
        GuhEntity kopie = VerhaalGuhs.maakKopie(level, VerhaalGuh.STITCH626, helper.absolutePos(new BlockPos(12, 2, 10)));
        // Lilo sends you to the capsule
        Ohana.lilo(lilo, p);
        helper.assertTrue(Ohana.stap(p) == Ohana.CAPSULE, "to the capsule: " + Ohana.stap(p));
        GuhwaiiEvents.TEST_GEBIEDEN.clear();
        Ohana.inCapsule(p);
        helper.assertTrue(Ohana.stap(p) == Ohana.TERUG, "the capsule is found");
        // the adoption: 626 makes a mess (only mess)
        Ohana.lilo(lilo, p);
        helper.assertTrue(Ohana.stap(p) == Ohana.OPRUIMEN, "adopted, now clean up");
        List<BlockPos> rommel = rommel(level, nani.blockPosition());
        helper.assertTrue(rommel.size() == Ohana.ROMMEL_NODIG, "five rommeltjes: " + rommel.size());
        for (BlockPos r : rommel) {
            Ohana.opgeruimd(p, r);
        }
        helper.assertTrue(Ohana.stap(p) == Ohana.LIEF && rommel(level, nani.blockPosition()).isEmpty(), "all clean: " + Ohana.stap(p));
        // three lovely things for 626 (a wrong thing first)
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
        Ohana.klik626(kopie, p, InteractionHand.MAIN_HAND);
        helper.assertTrue(Ohana.gaven(p) == 0 && p.getMainHandItem().getCount() == 3, "3 knabbels are not enough");
        for (ItemStack gave : List.of(new ItemStack(GuhwaiiFeature.KOKOSNOOT_ITEM.get()), new ItemStack(GuhwaiiFeature.ROZE_HIBISCUS.get()),
                new ItemStack(ModItems.KAAS_KNABBELS.get(), Ohana.KNABBELS))) {
            p.setItemInHand(InteractionHand.MAIN_HAND, gave);
            Ohana.klik626(kopie, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(gave.isEmpty(), "626 takes it: " + gave);
        }
        helper.assertTrue(Ohana.gaven(p) == Ohana.ALLE_GAVEN && Ohana.stap(p) == Ohana.OHANA, "626 is lief now");
        // ohana: the rewards and 626 released
        helper.assertTrue(!VerhaalGuhs.isVrij(p, VerhaalGuh.STITCH626), "not released yet");
        p.getInventory().clearContent();
        Ohana.lilo(lilo, p);
        helper.assertTrue(Ohana.stap(p) == Ohana.KLAAR && VerhaalGuhs.isVrij(p, VerhaalGuh.STITCH626), "ohana: released");
        helper.assertTrue(heeft(p, GuhwaiiFeature.VADSIGHEID_POSTER.get().asItem()) && heeft(p, GuhwaiiFeature.UKELELE.get())
                && heeft(p, ModItems.clothingItem(GuhClothes.GUHWAII_HULAROKJE)) && heeft(p, ModItems.clothingItem(GuhClothes.GUHWAII_BLOEMENKRANS))
                && heeft(p, ModItems.clothingItem(GuhClothes.GUHWAII_STITCHOREN)) && heeft(p, ModItems.clothingItem(GuhClothes.GUHWAII_SURFPLANKJE)),
                "the poster, the ukelele and the four outfit pieces");
        // 626-guh: a kaasknabbel and he comes home with you... once
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        Ohana.klik626(kopie, p, InteractionHand.MAIN_HAND);
        List<GuhEntity> eigen = eigen626(level, kopie, p);
        helper.assertTrue(eigen.size() == 1 && eigen.get(0).isTame() && VerhaalGuhs.heeftGetemd(p, VerhaalGuh.STITCH626)
                && p.getMainHandItem().getCount() == 4 && !kopie.isTame(), "p's own 626-guh (the story copy stays)");
        Ohana.klik626(kopie, p, InteractionHand.MAIN_HAND);
        helper.assertTrue(eigen626(level, kopie, p).size() == 1 && p.getMainHandItem().getCount() == 4, "only once");
        // q hasn't done the story: 626 won't come along; after it he does
        q.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        Ohana.klik626(kopie, q, InteractionHand.MAIN_HAND);
        helper.assertTrue(eigen626(level, kopie, q).isEmpty(), "q: not before the story");
        Ohana.zet(q, Ohana.OHANA);
        Ohana.lilo(lilo, q);
        Ohana.klik626(kopie, q, InteractionHand.MAIN_HAND);
        helper.assertTrue(eigen626(level, kopie, q).size() == 1, "q gets one of their own");
        for (ServerPlayer s : List.of(p, q)) {
            for (GuhEntity g : eigen626(level, kopie, s)) {
                g.discard();
            }
            Ohana.vergeet(s);
        }
        kopie.discard();
        lilo.discard();
        nani.discard();
        weg(helper, p, q);
        helper.succeed();
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, int x, int z) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        Vec3 v = helper.absoluteVec(new Vec3(x + 0.5, 2, z + 0.5));
        npc.snapTo(v.x, v.y, v.z, 0f, 0f);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    private static List<BlockPos> rommel(ServerLevel level, BlockPos rond) {
        List<BlockPos> uit = new java.util.ArrayList<>();
        int r = Ohana.ROMMEL_STRAAL;
        for (BlockPos q : BlockPos.betweenClosed(rond.offset(-r, -3, -r), rond.offset(r, 3, r))) {
            if (level.getBlockState(q).is(GuhwaiiFeature.ROMMELTJE.get())) {
                uit.add(q.immutable());
            }
        }
        return uit;
    }

    private static boolean heeft(ServerPlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item) > 0;
    }

    private static List<GuhEntity> eigen626(ServerLevel level, Entity bij, ServerPlayer p) {
        return level.getEntitiesOfClass(GuhEntity.class, new AABB(bij.blockPosition()).inflate(16),
                g -> g.getVariant() == GuhVariant.STITCH626 && g.isOwnedBy(p));
    }

    // =================================================================================================================
    // the 626-guh
    // =================================================================================================================

    /** 626-guh climbs walls, hangs from the ceiling (and lets go), carries two, strums the ukelele; other guhs don't. */
    @GuhTest(template = "guhwaii_test_muur", batch = BATCH, timeoutTicks = 200)
    public static void guhwaii626KlimtEnDraagt(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5));
        GuhEntity stitch = guh(helper, 1.5, 2, 4.5);
        GuhEntity gewoon = guh(helper, 1.5, 2, 7.5);
        stitch.tame(p);
        gewoon.tame(p);
        stitch.setVariant(GuhVariant.STITCH626);
        // walls: bumping into one is a ladder for him
        stitch.horizontalCollision = true;
        gewoon.horizontalCollision = true;
        helper.assertTrue(stitch.onClimbable() && !gewoon.onClimbable(), "626 climbs a wall, a normal guh doesn't");
        stitch.horizontalCollision = false;
        helper.assertTrue(!stitch.onClimbable(), "no wall, no climbing");
        // carrying: twice as much per chore trip
        helper.assertTrue(VariantGedragen.draagFactor(stitch) == 2 && VariantGedragen.draagFactor(gewoon) == 1, "draagFactor 2");
        // the ceiling: up the west wall, along the ceiling, hanging upside down, then he lets go
        Stitch626.Plafond plafond = new Stitch626.Plafond(stitch);
        helper.assertTrue(plafond.begin(), "a wall and a ceiling here");
        double plafondY = helper.absolutePos(new BlockPos(0, 6, 0)).getY();
        for (int i = 0; i < 80; i++) {
            plafond.tick();
            stitch.move(net.minecraft.world.entity.MoverType.SELF, stitch.getDeltaMovement());
        }
        helper.assertTrue(Stitch626.Plafond.hangt(stitch) && stitch.isNoGravity() && GuhHooks.heeft(stitch, VerhaalVlaggen.KLIMT)
                && stitch.getY() + stitch.getBbHeight() > plafondY - 0.3, "hanging from the ceiling: " + stitch.getY() + " / " + plafondY);
        plafond.stop();
        helper.assertTrue(!Stitch626.Plafond.hangt(stitch) && !stitch.isNoGravity() && !GuhHooks.heeft(stitch, VerhaalVlaggen.KLIMT),
                "let go: gravity back");
        // the ukelele: only he can, from his own button
        helper.assertTrue(Emote.magVoor(Emote.UKELELE, stitch) && !Emote.magVoor(Emote.UKELELE, gewoon), "only 626 has the ukelele emote");
        helper.assertTrue("gui.guhs.guhwaii.ukelele".equals(VariantGedragen.van(stitch).speciaalKnop()), "his own button");
        helper.succeedWhen(() -> {
            helper.assertTrue(stitch.onGround(), "he lands again");
            if (stitch.emotes.current() != Emote.UKELELE) {
                VariantGedragen.van(stitch).speciaal(stitch, p);
            }
            helper.assertTrue(stitch.emotes.current() == Emote.UKELELE, "he strums");
            stitch.discard();
            gewoon.discard();
            weg(helper, p);
        });
    }

    // =================================================================================================================
    // the palm, the outfits
    // =================================================================================================================

    /** A guh-palm on the beach: a trunk with a guh face, fronds that touch (they never decay), coconuts; the outfit sources. */
    @GuhTest(template = "guhwaii_test_strand", batch = BATCH)
    public static void guhwaiiPalmEnKleding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos grond = helper.absolutePos(new BlockPos(4, 1, 5));
        boolean gezet = false;
        for (int i = 0; i < 20 && !gezet; i++) {
            gezet = GuhwaiiWorldgen.boom(level, level.getRandom(), grond);
        }
        helper.assertTrue(gezet, "a palm grows on the sand");
        int gezichten = 0, bladeren = 0, noten = 0;
        for (BlockPos q : BlockPos.betweenClosed(grond.offset(-6, 1, -6), grond.offset(6, 12, 6))) {
            BlockState s = level.getBlockState(q);
            if (s.is(GuhwaiiFeature.PALM_GEZICHT.get())) {
                gezichten++;
            } else if (s.is(GuhwaiiFeature.PALM_BLAD.get())) {
                bladeren++;
                helper.assertTrue(s.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE, "every frond touches the tree: " + q);
            } else if (s.is(GuhwaiiFeature.KOKOSNOOT.get())) {
                noten++;
                helper.assertTrue(s.canSurvive(level, q.immutable()), "a coconut hangs under a frond");
            }
        }
        helper.assertTrue(gezichten == 1 && bladeren >= 12 && noten >= 1, "face " + gezichten + ", fronds " + bladeren + ", coconuts " + noten);
        for (GuhClothes c : List.of(GuhClothes.GUHWAII_HULAROKJE, GuhClothes.GUHWAII_BLOEMENKRANS, GuhClothes.GUHWAII_STITCHOREN,
                GuhClothes.GUHWAII_SURFPLANKJE)) {
            helper.assertTrue(GuhwaiiFeature.BRON.equals(KledingBronnen.bron(c)), "outfit source of " + c);
        }
        helper.assertTrue(GuhClothes.GUHWAII_STITCHOREN.slot == GuhClothes.Slot.OREN && GuhClothes.GUHWAII_SURFPLANKJE.slot == GuhClothes.Slot.BACK,
                "the slots");
        // clean up the palm (not the sand)
        for (BlockPos q : BlockPos.betweenClosed(grond.offset(-6, 1, -6), grond.offset(6, 12, 6))) {
            if (!level.getBlockState(q).isAir() && !level.getBlockState(q).is(net.minecraft.world.level.block.Blocks.WATER)) {
                level.setBlock(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
        helper.succeed();
    }
}
