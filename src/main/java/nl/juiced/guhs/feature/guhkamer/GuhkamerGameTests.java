package nl.juiced.guhs.feature.guhkamer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * Game tests of the Guhkamer (2.10): the room's size grows with the owner's zielsguhs (and only grows); it is built with
 * walls, floor, lamps and its door, and when it grows the walls and door move out while everything inside stays; the
 * Guhbel sends a guh there (it's kept as data while nobody is in the room, "waar is mijn guh" says Guhkamer, the GUHKAMER
 * moment and the dagboek's first time), guests appear in the room when somebody comes in and are kept again when the
 * room is empty, and the Guhbel calls a guest back to you; a full room says so; the doors take you in and out and a
 * private maag keeps its Guhkamer closed to visitors; the maag gets its door. The game test server has no guhmaag, so
 * the rooms are built in the test level ({@link Guhkamer#TEST_PLEK}).
 * (Template guhkamer_test_kamer: 30 x 30 smooth stone at y 0, the room's floor at y 4.)
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class GuhkamerGameTests {
    private static final String KAMER = "guhkamer_test_kamer";
    private static final String BATCH = "guhkamer";
    static final List<UUID> GUHKAMER_MOMENTEN = new CopyOnWriteArrayList<>();

    static {
        Band.opMoment((guh, speler, m, waarde) -> {
            if (m == Moment.GUHKAMER) {
                GUHKAMER_MOMENTEN.add(guh.getUUID());
            }
        });
    }

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static GuhEntity guh(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(owner);
        return guh;
    }

    /** The room of this owner in the test level, floor at y 4 in the middle of the template. */
    static BlockPos kamer(GameTestHelper helper, ServerPlayer owner) {
        BlockPos m = helper.absolutePos(new BlockPos(15, 4, 15));
        Guhkamer.TEST_PLEK.put(owner.getUUID(), new Guhkamer.Plek(helper.getLevel(), m));
        return m;
    }

    /** Raises a guh to zielsguh bff 5evr (hearts only go up: a few "days" of discovered favourites). */
    static void maakZielsguh(GuhEntity guh, ServerPlayer owner) {
        MinecraftServer s = guh.getServer();
        for (int i = 0; i < 8 && Band.niveau(guh) != BandNiveau.ZIELSGUH; i++) {
            BandData.Rec r = BandData.get(s).vind(owner.getUUID(), guh.getUUID());
            if (r != null) {
                r.dag = -1;
            }
            Band.geefHartjes(guh, owner, Reden.FAVORIET_ONTDEKT.dagMax(), Reden.FAVORIET_ONTDEKT);
        }
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Guhkamer.TEST_PLEK.remove(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    // =====================================================================================================================

    @GameTest(template = KAMER, batch = BATCH)
    public static void guhkamerGroeitMetZielsguhs(GameTestHelper helper) {
        helper.assertTrue(Guhkamer.breedte(0) == 16 && Guhkamer.breedte(1) == 20 && Guhkamer.breedte(8) == 48 && Guhkamer.breedte(30) == 48,
                "16, 4 more per zielsguh, at most 48");
        helper.assertTrue(Guhkamer.plekken(0) == 6 && Guhkamer.plekken(2) == 12 && Guhkamer.plekken(99) == 30, "6 guests, 3 more per zielsguh");
        helper.assertTrue(Guhkamer.hoogte(0) == 7 && Guhkamer.hoogte(8) == 11, "and a little higher");
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        GuhEntity a = guh(helper, p, new BlockPos(3, 2, 3));
        guh(helper, p, new BlockPos(4, 2, 3));
        helper.assertTrue(Guhkamer.zielsguhs(helper.getLevel().getServer(), p.getUUID()) == 0, "no zielsguhs yet");
        maakZielsguh(a, p);
        helper.assertTrue(Band.niveau(a) == BandNiveau.ZIELSGUH, "zielsguh bff 5evr <3");
        helper.assertTrue(Guhkamer.zielsguhs(helper.getLevel().getServer(), p.getUUID()) == 1, "one zielsguh: a room of " + Guhkamer.breedte(1));
        weg(helper, p);
        helper.succeed();
    }

    @GameTest(template = KAMER, batch = BATCH)
    public static void guhkamerWordtGebouwdEnGroeit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        BlockPos m = kamer(helper, p);
        helper.assertTrue(Guhkamer.zorgGebouwd(level.getServer(), p.getUUID()), "built");
        GuhkamerData.Kamer k = GuhkamerData.get(level.getServer()).vind(p.getUUID());
        helper.assertTrue(k.breedte == 16 && k.hoogte == 7, "16 x 16, 7 high");
        helper.assertTrue(level.getBlockState(m.offset(-9, 2, 0)).is(Blocks.PINK_TERRACOTTA), "pink walls");
        helper.assertTrue(level.getBlockState(m.offset(3, -1, 3)).is(Blocks.CHERRY_PLANKS), "a wooden floor");
        helper.assertTrue(level.getBlockState(m.offset(-9, 0, 0)).is(Blocks.CHERRY_PLANKS), "a wooden plint");
        helper.assertTrue(level.getBlockState(m.offset(-10, 3, 0)).is(Blocks.BARRIER), "barriers around it");
        BlockPos deur = GuhkamerBouw.kamerDeur(m, 16);
        helper.assertTrue(level.getBlockState(deur).getBlock() instanceof GuhkamerDeurBlock
                && level.getBlockState(deur).getValue(GuhkamerDeurBlock.KANT) == GuhkamerDeurBlock.Kant.KAMER
                && level.getBlockState(deur.above()).getValue(GuhkamerDeurBlock.HALF) == DoubleBlockHalf.UPPER, "the door back to the maag");
        helper.assertTrue(level.getBlockState(m).is(Blocks.PINK_CARPET) || level.getBlockState(m).is(Blocks.MAGENTA_CARPET), "a rug");
        helper.assertFalse(Guhkamer.zorgGebouwd(level.getServer(), p.getUUID()), "nothing to do while nothing changed");
        // furnish it, then a zielsguh: the room grows, the furniture stays
        BlockPos pot = m.offset(5, 0, -5);
        level.setBlock(pot, Blocks.FLOWER_POT.defaultBlockState(), 3);
        GuhEntity guh = guh(helper, p, new BlockPos(2, 2, 2));
        maakZielsguh(guh, p);
        helper.assertTrue(k.breedte == 20 && k.hoogte == 7, "grown by itself with the new zielsguh: " + k.breedte);
        helper.assertTrue(level.getBlockState(m.offset(-11, 2, 0)).is(Blocks.PINK_TERRACOTTA), "the walls moved out");
        helper.assertTrue(level.getBlockState(m.offset(-9, 2, 0)).isAir(), "the old wall is gone");
        helper.assertTrue(level.getBlockState(m.offset(-9, -1, 0)).is(Blocks.CHERRY_PLANKS), "floor where the old wall stood");
        helper.assertTrue(level.getBlockState(pot).is(Blocks.FLOWER_POT), "the furniture stays");
        helper.assertTrue(level.getBlockState(GuhkamerBouw.kamerDeur(m, 20)).getBlock() instanceof GuhkamerDeurBlock
                && !(level.getBlockState(deur).getBlock() instanceof GuhkamerDeurBlock), "the door moved with the wall");
        helper.assertTrue(level.getBlockState(deur.above(2).north()).isAir(), "and the old sign above it too");
        weg(helper, p);
        helper.succeed();
    }

    @GameTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void guhkamerGuhbelStuurtEnRoept(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        BlockPos m = kamer(helper, p);
        Guhkamer.zorgGebouwd(level.getServer(), p.getUUID());
        GuhEntity guh = guh(helper, p, new BlockPos(2, 2, 2));
        UUID id = guh.getUUID();
        helper.assertTrue(Guhkamer.stuur(p, guh) == Guhkamer.Uitkomst.OK, "sent to the Guhkamer");
        GuhkamerData.Kamer k = GuhkamerData.get(level.getServer()).vind(p.getUUID());
        helper.assertTrue(guh.isRemoved() && k.gasten.get(id).opgeslagen(), "nobody in the room: kept as data");
        helper.assertTrue(GuhVolger.plek(level.getServer(), p.getUUID(), id).soort() == PlekSoort.GUHKAMER, "waar is mijn guh: in de Guhkamer");
        helper.assertTrue(GUHKAMER_MOMENTEN.contains(id), "the GUHKAMER moment");
        helper.assertTrue(Dagboek.heeftEersteKeer(level.getServer(), p.getUUID(), id, "eerste_guhkamer"), "its first time in the dagboek");
        // into the room: the guest is there
        Guhkamer.gaNaarBinnen(p, p.getUUID());
        AABB box = Guhkamer.binnen(m, k.breedte, k.hoogte);
        helper.assertTrue(box.contains(p.position()), "the player is in the room");
        helper.runAfterDelay(2, () -> {
            Entity e = level.getEntity(id);
            helper.assertTrue(e instanceof GuhEntity && Guhkamer.isGast(e) && box.inflate(0.5).contains(e.position()), "the guest is in the room");
            helper.assertFalse(k.gasten.get(id).opgeslagen(), "in the world while somebody is there");
            // out again: after a moment the guest is kept as data
            p.teleportTo(level, m.getX() + 0.5 - 13, m.getY() - 2, m.getZ() + 0.5 - 13, 0, 0);
            helper.runAfterDelay(25, () -> {
                helper.assertTrue(level.getEntity(id) == null && k.gasten.get(id).opgeslagen(), "the empty room keeps its guest as data");
                // the Guhbel calls it back
                Entity terug = Guhkamer.roep(p, id);
                helper.assertTrue(terug instanceof GuhEntity g && g.getUUID().equals(id) && terug.distanceTo(p) < 3, "called back to the player");
                helper.assertFalse(Guhkamer.isGast(terug), "not a guest any more");
                helper.assertTrue(!k.gasten.containsKey(id), "gone from the room's list");
                helper.assertTrue(((GuhEntity) terug).isTame() && p.getUUID().equals(((GuhEntity) terug).getOwnerUUID()), "still yours");
                helper.assertTrue(GuhVolger.plek(level.getServer(), p.getUUID(), id).soort() == PlekSoort.WERELD, "waar is mijn guh: in de wereld");
                terug.discard();
                weg(helper, p);
                helper.succeed();
            });
        });
    }

    /**
     * 2.10.1: the Guh menu puts a guh in the logeerkamer and takes it out again. A tamed guh's menu shows "Logeren in de
     * Guhkamer" (it goes there like with the Guhbel); a guest in the room shows "Uit de logeerkamer" (the synced flag
     * tells the client) and comes to you, just your guh again. Someone else's guh can't; a wild guh has no button.
     */
    @GameTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void guhkamerViaHetGuhMenu(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        BlockPos m = kamer(helper, p);
        Guhkamer.zorgGebouwd(level.getServer(), p.getUUID());
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 2));
        helper.assertTrue(Guhkamer.menuKnop(wild) == Guhkamer.MenuKnop.GEEN, "a wild guh: no Guhkamer button");
        ServerPlayer ander = speler(helper, new BlockPos(1, 2, 3));
        GuhEntity vreemd = guh(helper, ander, new BlockPos(6, 2, 2));
        helper.assertTrue(GuhkamerPayloads.menuLogeren(p, vreemd) == Guhkamer.Uitkomst.NIET_JOUW && !vreemd.isRemoved(), "not someone else's guh");
        GuhEntity guh = guh(helper, p, new BlockPos(2, 2, 2));
        UUID id = guh.getUUID();
        helper.assertTrue(Guhkamer.menuKnop(guh) == Guhkamer.MenuKnop.LOGEREN, "your tamed guh: \"Logeren in de Guhkamer\"");
        nl.juiced.guhs.network.GuhActionPayload.apply(guh, p, new nl.juiced.guhs.network.GuhActionPayload(guh.getId(),
                nl.juiced.guhs.network.GuhActionPayload.Action.GUHKAMER_LOGEREN));
        GuhkamerData.Kamer k = GuhkamerData.get(level.getServer()).vind(p.getUUID());
        helper.assertTrue(guh.isRemoved() && k.gasten.containsKey(id) && k.gasten.get(id).opgeslagen(), "off to the Guhkamer, like with the Guhbel");
        helper.assertTrue(GuhVolger.plek(level.getServer(), p.getUUID(), id).soort() == PlekSoort.GUHKAMER, "waar is mijn guh: in de Guhkamer");
        Guhkamer.gaNaarBinnen(p, p.getUUID());
        helper.runAfterDelay(2, () -> {
            Entity gast = level.getEntity(id);
            helper.assertTrue(gast instanceof GuhEntity && Guhkamer.isGast(gast), "the guest is in the room");
            GuhEntity g = (GuhEntity) gast;
            helper.assertTrue(nl.juiced.guhs.feature.band.BandVlaggen.heeft(g, nl.juiced.guhs.feature.band.BandVlaggen.GUHKAMER_GAST)
                    && Guhkamer.menuKnop(g) == Guhkamer.MenuKnop.UIT, "its menu: \"Uit de logeerkamer\" (synced flag)");
            // a second "Logeren" on a guest does nothing
            helper.assertTrue(GuhkamerPayloads.menuLogeren(p, g) == Guhkamer.Uitkomst.OK && k.gasten.size() == 1, "already staying");
            nl.juiced.guhs.network.GuhActionPayload.apply(g, p, new nl.juiced.guhs.network.GuhActionPayload(g.getId(),
                    nl.juiced.guhs.network.GuhActionPayload.Action.GUHKAMER_UIT));
            Entity terug = level.getEntity(id);
            helper.assertTrue(terug instanceof GuhEntity && terug.distanceTo(p) < 3, "it comes to you");
            GuhEntity t = (GuhEntity) terug;
            helper.assertTrue(!Guhkamer.isGast(t) && !nl.juiced.guhs.feature.band.BandVlaggen.heeft(t, nl.juiced.guhs.feature.band.BandVlaggen.GUHKAMER_GAST)
                    && Guhkamer.menuKnop(t) == Guhkamer.MenuKnop.LOGEREN, "just your guh again: the button says \"Logeren\" again");
            helper.assertTrue(!k.gasten.containsKey(id) && t.isTame() && p.getUUID().equals(t.getOwnerUUID()) && !t.isOrderedToSit(),
                    "off the room's list, still yours, ready to follow you out");
            // out of the room: it stays with you (not kept as a guest)
            p.teleportTo(level, m.getX() + 0.5 - 13, m.getY() - 2, m.getZ() + 0.5 - 13, 0, 0);
            helper.runAfterDelay(25, () -> {
                helper.assertTrue(level.getEntity(id) != null && !k.gasten.containsKey(id), "not put back in the room");
                level.getEntity(id).discard();
                weg(helper, p, ander);
                helper.succeed();
            });
        });
    }

    @GameTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void guhkamerOpgepakteGastIsGeenGastMeer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        BlockPos m = kamer(helper, p);
        Guhkamer.zorgGebouwd(level.getServer(), p.getUUID());
        GuhEntity guh = guh(helper, p, new BlockPos(2, 2, 2));
        UUID id = guh.getUUID();
        Guhkamer.stuur(p, guh);
        Guhkamer.gaNaarBinnen(p, p.getUUID());
        helper.runAfterDelay(2, () -> {
            Entity e = level.getEntity(id);
            helper.assertTrue(e instanceof GuhEntity, "the guest is in the room");
            // the owner picks it up (sneak-tap) and takes it along as an item
            net.minecraft.world.item.ItemStack item = nl.juiced.guhs.item.PickedUpGuhItem.pickUp((GuhEntity) e);
            p.teleportTo(level, m.getX() + 0.5 - 13, m.getY() - 2, m.getZ() + 0.5 - 13, 0, 0);
            helper.runAfterDelay(25, () -> {
                GuhkamerData.Kamer k = GuhkamerData.get(level.getServer()).vind(p.getUUID());
                helper.assertFalse(k.gasten.containsKey(id), "picked up out of the room: not a guest any more");
                Entity los = nl.juiced.guhs.item.PickedUpGuhItem.release(level, nl.juiced.guhs.item.PickedUpGuhItem.guhData(item),
                        p.getX(), p.getY(), p.getZ(), 0);
                helper.runAfterDelay(2, () -> {
                    helper.assertFalse(Guhkamer.isGast(los), "and when you let it go it is just your guh again");
                    los.discard();
                    weg(helper, p);
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = KAMER, batch = BATCH)
    public static void guhkamerVolIsVol(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        kamer(helper, p);
        ServerPlayer ander = speler(helper, new BlockPos(1, 2, 3));
        GuhEntity vreemd = guh(helper, ander, new BlockPos(2, 2, 3));
        helper.assertTrue(Guhkamer.stuur(p, vreemd) == Guhkamer.Uitkomst.NIET_JOUW && !vreemd.isRemoved(), "someone else's guh can't stay in your room");
        for (int i = 0; i < Guhkamer.plekken(0); i++) {
            helper.assertTrue(Guhkamer.stuur(p, guh(helper, p, new BlockPos(2 + i, 2, 1))) == Guhkamer.Uitkomst.OK, "guest " + i);
        }
        GuhEntity teVeel = guh(helper, p, new BlockPos(9, 2, 1));
        helper.assertTrue(Guhkamer.stuur(p, teVeel) == Guhkamer.Uitkomst.VOL && !teVeel.isRemoved(), "a full room says so, the guh stays with you");
        GuhkamerData.get(helper.getLevel().getServer()).vind(p.getUUID()).gasten.clear();
        weg(helper, p, ander);
        helper.succeed();
    }

    @GameTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void guhkamerDeurenEnBezoek(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        ServerPlayer bezoeker = speler(helper, new BlockPos(1, 2, 3));
        BlockPos m = kamer(helper, p);
        GuhWorldData.Maag maag = GuhWorldData.get(level.getServer()).createMaag(p.getUUID(), "Eigenaar");
        maag.access = GuhWorldData.Access.PRIVATE;
        Guhkamer.zorgGebouwd(level.getServer(), p.getUUID());
        GuhkamerData.Kamer k = GuhkamerData.get(level.getServer()).vind(p.getUUID());
        AABB box = Guhkamer.binnen(m, k.breedte, k.hoogte);
        BlockPos deur = GuhkamerBouw.kamerDeur(m, k.breedte);
        // a visitor at the maag side of the door of a private maag: stays outside
        Guhkamer.deur(bezoeker, m.offset(0, 0, -12), GuhkamerDeurBlock.Kant.MAAG);
        helper.assertFalse(box.contains(bezoeker.position()), "a private maag keeps its Guhkamer closed");
        // the owner walks in
        Guhkamer.deur(p, m.offset(0, 0, -12), GuhkamerDeurBlock.Kant.MAAG);
        helper.assertTrue(box.contains(p.position()), "the owner walks into the Guhkamer");
        // public: the visitor may come in too
        maag.access = GuhWorldData.Access.PUBLIC;
        bezoeker.getPersistentData().remove(Guhkamer.DEUR_TOT);
        Guhkamer.deur(bezoeker, m.offset(0, 0, -12), GuhkamerDeurBlock.Kant.MAAG);
        helper.assertTrue(box.contains(bezoeker.position()), "a public maag: visitors may look");
        helper.runAfterDelay(45, () -> {
            Guhkamer.deur(p, deur, GuhkamerDeurBlock.Kant.KAMER);
            helper.assertFalse(box.contains(p.position()), "the door in the room takes you back out");
            weg(helper, p, bezoeker);
            helper.succeed();
        });
    }

    @GameTest(template = KAMER, batch = BATCH)
    public static void guhkamerDeurInDeMaag(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(15, 2, 15));
        BlockPos deur = GuhkamerBouw.zetMaagDeur(level, c);
        helper.assertTrue(deur != null, "a spot for the door: " + level.getBlockState(c.offset(-7, -1, 7)) + " / " + level.getBlockState(c.offset(-7, 0, 7)));
        helper.assertTrue(level.getBlockState(deur).getBlock() instanceof GuhkamerDeurBlock
                && level.getBlockState(deur).getValue(GuhkamerDeurBlock.KANT) == GuhkamerDeurBlock.Kant.MAAG
                && level.getBlockState(deur.above()).getBlock() instanceof GuhkamerDeurBlock, "the door in the maag, two high");
        helper.assertTrue(level.getBlockState(deur.east(2)).getBlock() instanceof StandingSignBlock, "with a sign next to it");
        int rx = deur.getX() - c.getX(), rz = deur.getZ() - c.getZ();
        helper.assertFalse(Math.abs(rx) <= 6 && rz >= -2 && rz <= 6, "away from the portals and where you arrive");
        helper.assertTrue(level.getBlockState(deur).getCollisionShape(level, deur).isEmpty(), "you walk right through the curtain");
        helper.succeed();
    }

    static Vec3 midden(BlockPos m) {
        return Vec3.atBottomCenterOf(m);
    }
}
