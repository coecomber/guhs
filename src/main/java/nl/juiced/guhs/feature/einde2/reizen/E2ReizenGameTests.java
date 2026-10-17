package nl.juiced.guhs.feature.einde2.reizen;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.einde2.zwevers.LeegtewalvisguhEntity;
import nl.juiced.guhs.feature.einde2.zwevers.Leegtewalvissen;
import nl.juiced.guhs.feature.einde2.zwevers.Walviszicht;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Game tests of einde2 phase B, slice reizen (CONTRACT_E2_B 8: "reizen: guest rule, whale visibility"):
 * <ul>
 *   <li>the guest rule: riding in the host's basket makes a guest, after getting off one stays a guest close by and stops
 *       being one beyond {@value E2Reizen#GAST_AFSTAND} blocks; a stranger is nobody's guest;</li>
 *   <li>the whale questline: believing the captain makes the whale visible for that player only (and keeps the first sighting
 *       for the scene); feeding {@value Leegtewalvis#VOER} times at step 3 makes the whale the player's own (an older copy
 *       steps aside, the chests keep their items).</li>
 * </ul>
 * Template einde2_appa_test_wei (16 x 16 grass).
 */
public class E2ReizenGameTests {
    private static final String WEI = "einde2_appa_test_wei";

    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            AppaGuhEntity appa = Appa.geladen(p);
            if (appa != null) {
                appa.discard();
            }
            E2Reizen.weg(p);
            p.stopRiding();
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    @GuhTest(template = WEI, batch = "einde2_reizen", timeoutTicks = 200)
    public static void einde2ReizenGast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer host = speler(helper, new BlockPos(4, 2, 4));
        ServerPlayer gast = speler(helper, new BlockPos(6, 2, 4));
        ServerPlayer vreemde = speler(helper, new BlockPos(10, 2, 10));
        AppaGuhEntity appa = Appa.geef(host);
        helper.assertTrue(appa != null, "the host has an Appa");
        BlockPos at = helper.absolutePos(new BlockPos(8, 2, 8));
        appa.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        host.startRiding(appa, true, false);
        helper.assertTrue(gast.startRiding(appa, true, false), "the guest climbs into the basket");
        helper.assertTrue(E2Reizen.gastheer(gast) == host, "riding in the host's basket: a guest");
        helper.assertTrue(E2Reizen.gasten(host).contains(gast) && !E2Reizen.gasten(host).contains(vreemde), "the host's guests");
        helper.assertTrue(E2Reizen.gastheer(host) == null && E2Reizen.gastheer(vreemde) == null, "the host and a stranger are nobody's guest");
        E2Reizen.gastenTick(level.getServer());
        gast.stopRiding();
        E2Reizen.gastenTick(level.getServer());
        helper.assertTrue(E2Reizen.gastheer(gast) == host, "off the basket, close by: still a guest");
        gast.teleportTo(level, host.getX() + E2Reizen.GAST_AFSTAND + 6, host.getY(), host.getZ(), Set.of(), 0, 0, true);
        E2Reizen.gastenTick(level.getServer());
        helper.assertTrue(E2Reizen.gastheer(gast) == null, "more than " + E2Reizen.GAST_AFSTAND + " blocks away: no guest any more");
        gast.teleportTo(level, host.getX() + 2, host.getY(), host.getZ(), Set.of(), 0, 0, true);
        E2Reizen.gastenTick(level.getServer());
        helper.assertTrue(E2Reizen.gastheer(gast) == null, "and coming back does not make one a guest again (only flying along does)");
        weg(helper, host, gast, vreemde);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = "einde2_reizen", timeoutTicks = 200)
    public static void einde2ReizenWalvis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer gelovige = speler(helper, new BlockPos(4, 2, 4));
        ServerPlayer ander = speler(helper, new BlockPos(10, 2, 4));
        Leegtewalvis.LIJN.wis(gelovige);
        Leegtewalvis.LIJN.wis(ander);
        GuhQuests.saved(gelovige).remove(Walvis.KEY);
        Walviszicht.zet(gelovige, false);
        Walviszicht.zet(ander, false);
        Leegtewalvis.neemAan(gelovige, null);
        helper.assertTrue(Leegtewalvis.LIJN.stap(gelovige) == 1 && Leegtewalvis.wachtplek(gelovige) != null, "believing: step 1 and a waiting spot");
        helper.assertTrue(Walviszicht.ziet(gelovige) && !Walviszicht.ziet(ander), "only who believes sees the whale");
        helper.assertTrue(!Leegtewalvissen.MAG_KOMEN.test(gelovige), "no whale comes by itself before the first sighting");
        BlockPos boven = helper.absolutePos(new BlockPos(8, 30, 8));
        LeegtewalvisguhEntity w = E2ZweversFeature.LEEGTEWALVISGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        w.snapTo(boven.getX(), boven.getY(), boven.getZ(), 0, 0);
        level.addFreshEntity(w);
        helper.assertTrue(w.broadcastToPlayer(gelovige) && !w.broadcastToPlayer(ander), "the whale is sent to the believer only");
        Leegtewalvis.LIJN.zet(gelovige, 3);
        helper.assertTrue(Leegtewalvissen.MAG_KOMEN.test(gelovige), "after the sighting whales come by themselves");
        ItemStack vruchten = new ItemStack(E2BiomesFeature.KNABBELKOORVRUCHT.get(), Leegtewalvis.VOER);
        for (int i = 0; i < Leegtewalvis.VOER; i++) {
            helper.assertTrue(w.eigenaar() == null, "still wild after " + i + " fruits");
            w.voer(gelovige, vruchten);
        }
        helper.assertTrue(gelovige.getUUID().equals(w.eigenaar()) && Walvis.heeft(gelovige), "fed " + Leegtewalvis.VOER + " times: the whale is theirs");
        helper.assertTrue(Leegtewalvis.LIJN.klaar(gelovige), "and the questline is done");
        helper.assertTrue(Walvis.geladen(gelovige) == w, "it is their loaded own whale");
        // an older copy (made before, from a chunk that loads later) steps aside
        LeegtewalvisguhEntity oud = E2ZweversFeature.LEEGTEWALVISGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        oud.snapTo(boven.getX() + 40, boven.getY(), boven.getZ(), 0, 0);
        oud.zetEigenaar(gelovige.getUUID(), w.gen() - 1);
        level.addFreshEntity(oud);
        Walvis.meld(oud);
        helper.assertTrue(oud.isRemoved() && !w.isRemoved(), "an older copy of an own whale removes itself");
        // the furniture and a chest that really keeps items
        Walvis.zetDeel(gelovige, "kisten", true);
        Walvis.meld(w);
        helper.assertTrue(Walvis.heeftDeel(w.inrichting(), "kisten") && Walvis.heeftDeel(w.inrichting(), "vlag"), "the whale wears its furniture");
        SimpleContainer kist = new SimpleContainer(Walvis.KIST_VAKKEN);
        kist.setItem(4, new ItemStack(Items.DIAMOND, 3));
        Walvis.schrijf(gelovige, "Kist1", kist);
        helper.assertTrue(Walvis.kist(gelovige, "Kist1").get(4).is(Items.DIAMOND) && Walvis.kist(gelovige, "Kist1").get(4).getCount() == 3,
                "the chest keeps its items with the player");
        w.discard();
        Leegtewalvis.LIJN.wis(gelovige);
        GuhQuests.saved(gelovige).remove(Walvis.KEY);
        Walviszicht.zet(gelovige, false);
        weg(helper, gelovige, ander);
        helper.succeed();
    }

    /**
     * einde2 fix-reizen (playtest 1, items 2, 4 and 12): at each of the four main buildings Appa's landing spot
     * ({@link Landingen#HOOFDGEBOUWEN}) is real free space on the landing place of the structure itself: the tiles of its
     * template ({@code <structuur>/stuk_i_j}) are put down far away, and there his box is free, every block under him is
     * solid, the column around him is free up to the top of the build (he comes straight down), the spot lies in the
     * {@code aankomst} zone, and the rider's spot {@code aankomst_speler} is free with ground under it. And who rides along
     * follows from the story alone: Aang-guh from chapter 1 step 1 on, nobody before.
     */
    @GuhTest(template = WEI, batch = "einde2_reizen", timeoutTicks = 400)
    public static void einde2ReizenLandingen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var appaType = nl.juiced.guhs.feature.einde2.appa.E2AppaFeature.APPA_GUH.get();
        var manager = level.getStructureManager();
        BlockPos basis = helper.absolutePos(BlockPos.ZERO).offset(640, 8, 640);
        int k = 0;
        for (var e : Landingen.HOOFDGEBOUWEN.entrySet()) {
            String s = e.getKey();
            Landingen.Plek plek = e.getValue();
            nl.juiced.guhs.feature.einde2.Plekken.Plek speler = nl.juiced.guhs.feature.einde2.Plekken.plek(s, "aankomst_speler");
            nl.juiced.guhs.feature.einde2.Plekken.Plek zone = nl.juiced.guhs.feature.einde2.Plekken.plek(s, "aankomst");
            helper.assertTrue(speler != null && zone != null && zone.tot() != null, s + ": the sidecar has aankomst and aankomst_speler");
            BlockPos o = basis.offset(k++ * 320, 0, 0);
            int hoogte = nl.juiced.guhs.feature.einde2.Plekken.blad(s).grootte().getY();
            double ax = speler.pos().getX() + plek.x(), ay = speler.pos().getY() + plek.y(), az = speler.pos().getZ() + plek.z();
            // the tiles around Appa's spot and the rider's spot
            Set<java.util.List<Integer>> tegels = new java.util.HashSet<>();
            for (double[] c : new double[][]{{ax, az}, {speler.pos().getX(), speler.pos().getZ()}}) {
                for (int dx = -4; dx <= 4; dx += 4) {
                    for (int dz = -4; dz <= 4; dz += 4) {
                        tegels.add(java.util.List.of(Math.floorDiv((int) Math.floor(c[0]) + dx, 32), Math.floorDiv((int) Math.floor(c[1]) + dz, 32)));
                    }
                }
            }
            for (java.util.List<Integer> t : tegels) {
                int i = t.get(0), j = t.get(1);
                var tegel = manager.get(nl.juiced.guhs.Guhs.id(s + "/stuk_" + i + "_" + j));
                if (tegel.isPresent()) {
                    BlockPos at = o.offset(i * 32, 0, j * 32);
                    tegel.get().placeInWorld(level, at, at, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                            level.getRandom(), 2);
                }
            }
            Vec3 a = new Vec3(o.getX() + ax, o.getY() + ay, o.getZ() + az);
            var doos = appaType.getSpawnAABB(a.x, a.y, a.z);
            helper.assertTrue(level.noCollision(doos), s + ": Appa's box is free at his landing spot " + a);
            for (int x = (int) Math.floor(doos.minX); x <= (int) Math.floor(doos.maxX - 1e-6); x++) {
                for (int z = (int) Math.floor(doos.minZ); z <= (int) Math.floor(doos.maxZ - 1e-6); z++) {
                    BlockPos onder = BlockPos.containing(x, a.y - 1, z);
                    helper.assertTrue(!level.getBlockState(onder).getCollisionShape(level, onder).isEmpty(), s + ": solid under Appa at " + onder);
                }
            }
            var kolom = new net.minecraft.world.phys.AABB(a.x - 3, a.y, a.z - 3, a.x + 3, o.getY() + hoogte, a.z + 3);
            helper.assertTrue(level.noCollision(kolom), s + ": free up to the top around his spot (he comes straight down)");
            BlockPos z0 = zone.pos(), z1 = zone.tot();
            helper.assertTrue(ax >= Math.min(z0.getX(), z1.getX()) && ax <= Math.max(z0.getX(), z1.getX()) + 1
                    && az >= Math.min(z0.getZ(), z1.getZ()) && az <= Math.max(z0.getZ(), z1.getZ()) + 1, s + ": his spot lies in the aankomst zone");
            Vec3 sp = new Vec3(o.getX() + speler.pos().getX() + 0.5, o.getY() + speler.pos().getY(), o.getZ() + speler.pos().getZ() + 0.5);
            var spelerDoos = net.minecraft.world.entity.EntityType.PLAYER.getDimensions().makeBoundingBox(sp);
            helper.assertTrue(level.noCollision(spelerDoos) && !level.noCollision(spelerDoos.move(0, -0.5, 0)), s + ": the rider's spot is free, on solid ground");
        }
        // the team on Appa's back: from the story state alone
        ServerPlayer p = speler(helper, new BlockPos(4, 2, 4));
        nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev.wis(p, nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal.LUCHT);
        helper.assertTrue(!E2Team.opAppa(p).contains(E2Team.Lid.AANG), "before the kaasbol nobody rides along");
        nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev.zetTot(p, nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal.LUCHT, 1);
        helper.assertTrue(E2Team.opAppa(p).contains(E2Team.Lid.AANG), "from chapter 1 step 1 (the first flight) Aang-guh rides along");
        nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev.wis(p, nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal.LUCHT);
        weg(helper, p);
        helper.succeed();
    }

    /**
     * einde2 fix3-reizen (playtest 2 item 4): Appa's menu has ONE story row, the chapter's main building (a flight to its
     * landing spot), and no row that puts the player straight at the step; an old "#verder" pick never teleports them.
     */
    @GuhTest(template = WEI, batch = "einde2_reizen", timeoutTicks = 100)
    public static void einde2ReizenVerhaalRij(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(4, 2, 4));
        try {
            E2VerhaalDev.zetTot(p, E2Verhaal.LUCHT, 3);
            helper.assertTrue(Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL.equals(verhaalRij(helper, p)), "chapter 1: the gold row is the temple");
            E2VerhaalDev.klaar(p, E2Verhaal.LUCHT);
            helper.assertTrue(Einde2Ids.NOORDELIJKE_WATERSTAM.equals(verhaalRij(helper, p)), "chapter 2: the gold row is the Waterstam");
            Vec3 voor = p.position();
            Fluitlijst.kies(p, Fluitlijst.VERDER);
            helper.assertTrue(p.position().distanceTo(voor) < 0.01, "an old '#verder' pick never puts the player at the step");
        } finally {
            E2VerhaalDev.wis(p, E2Verhaal.LUCHT);
            E2VerhaalDev.wis(p, E2Verhaal.WATER);
            weg(helper, p);
        }
        helper.succeed();
    }

    /** The id of the menu's one gold story row (and checks there is exactly one, and no "straight to your step" row). */
    private static String verhaalRij(GameTestHelper helper, ServerPlayer p) {
        ListTag rijen = Fluitlijst.rijen(p);
        String verhaal = null;
        int n = 0;
        for (Tag t : rijen) {
            CompoundTag c = (CompoundTag) t;
            String id = c.getStringOr("Id", "");
            helper.assertTrue(!id.equals(Fluitlijst.VERDER) && !c.getBooleanOr("Hervat", false), "no 'meteen naar je stap' row any more");
            if (c.getBooleanOr("Verhaal", false)) {
                verhaal = id;
                n++;
            }
        }
        helper.assertTrue(n == 1, "exactly one story row (" + n + ")");
        helper.assertTrue(verhaal.equals(E2Reizen.verhaalDoel(p)), "the story row is the story's place");
        return verhaal;
    }

    // =====================================================================================================================
    // fix4-appa (playtest 3): the own Appa-platform
    // =====================================================================================================================

    /** Lays a complete platform with its middle at {@code midden} (relative): smooth stone around, 3 blocks of air above. */
    private static BlockPos legPlatform(GameTestHelper helper, BlockPos midden) {
        ServerLevel level = helper.getLevel();
        BlockPos m = helper.absolutePos(midden);
        for (int dx = -AppaPlatform.HALF; dx <= AppaPlatform.HALF; dx++) {
            for (int dz = -AppaPlatform.HALF; dz <= AppaPlatform.HALF; dz++) {
                BlockPos q = m.offset(dx, 0, dz);
                level.setBlockAndUpdate(q, dx == 0 && dz == 0 ? E2ReizenFeature.APPA_PLATFORM.get().defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                for (int dy = 1; dy <= AppaPlatform.LUCHT; dy++) {
                    level.setBlockAndUpdate(q.above(dy), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
        }
        return m;
    }

    /**
     * fix4-appa: the 5x5 rule of the Appa-platform. A complete square is ok; every missing floor block and every block in the
     * 3 blocks of air above the square is named; without the platform block itself it is no platform. Placing it counts only
     * after the whole story; and the name is good Dutch (Joosts, Anna's, Max').
     */
    @GuhTest(template = WEI, batch = "einde2_reizen", timeoutTicks = 100)
    public static void einde2ReizenPlatform5x5(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos m = legPlatform(helper, new BlockPos(8, 1, 8));
        helper.assertTrue(AppaPlatform.controleer(level, m).ok(), "a complete 5x5 square is ok");
        level.setBlockAndUpdate(m.offset(-2, 0, -2), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(m.offset(2, 0, 1), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(m.offset(0, 0, 2), net.minecraft.world.level.block.Blocks.OAK_SLAB.defaultBlockState());
        level.setBlockAndUpdate(m.offset(1, 3, -1), net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState());
        AppaPlatform.Controle c = AppaPlatform.controleer(level, m);
        helper.assertTrue(c.blok() && !c.ok(), "a square with holes is not ok");
        helper.assertTrue(c.vloer().size() == 3 && c.vloer().contains(m.offset(-2, 0, -2)) && c.vloer().contains(m.offset(2, 0, 1))
                && c.vloer().contains(m.offset(0, 0, 2)), "the three missing floor blocks are named (air, water, a low slab): " + c.vloer());
        helper.assertTrue(c.inDeWeg().size() == 1 && c.inDeWeg().contains(m.offset(1, 3, -1)), "the fence in the air above is named: " + c.inDeWeg());
        legPlatform(helper, new BlockPos(8, 1, 8));
        level.setBlockAndUpdate(m, net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState());
        helper.assertTrue(!AppaPlatform.controleer(level, m).blok(), "without the Appa-platform block in the middle it is no platform");
        legPlatform(helper, new BlockPos(8, 1, 8));
        // placing it counts only after the whole story
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 5));
        E2VerhaalDev.wis(p, E2Verhaal.FINALE);
        GuhQuests.saved(p).remove(AppaPlatform.KEY);
        AppaPlatform.geplaatst(p, level, m);
        helper.assertTrue(AppaPlatform.van(p, level.dimension()) == null, "before the end of the story it is nobody's platform");
        E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, 0);
        E2VerhaalDev.klaar(p, E2Verhaal.FINALE);
        AppaPlatform.geplaatst(p, level, m);
        helper.assertTrue(m.equals(AppaPlatform.van(p, level.dimension())), "after the story it is the player's platform in this dimension");
        helper.assertTrue(Fluitlijst.rijen(p).getCompound(0).map(t -> t.getBooleanOr("Platform", false)).orElse(false),
                "the own platform is the top row of Appa's menu");
        helper.assertTrue(AppaPlatform.vorm("Joost").equals("s") && AppaPlatform.vorm("Anna").equals("as") && AppaPlatform.vorm("Max").equals("a")
                && AppaPlatform.vorm("Steve_").equals("as") && AppaPlatform.vorm("Thomas").equals("a"), "Joosts, Anna's, Max', Steve_'s, Thomas'");
        GuhQuests.saved(p).remove(AppaPlatform.KEY);
        weg(helper, p);
        helper.succeed();
    }

    /**
     * fix4-appa: the whistle outside the Guheinde (the game test level is the overworld). Without a platform, or more than
     * {@value AppaPlatform#BEREIK} blocks from it, Appa does not come; near an unfinished one he does not come either; near
     * a complete one he appears above its middle and sinks down onto it.
     */
    @GuhTest(template = WEI, batch = "einde2_reizen", timeoutTicks = 260)
    public static void einde2ReizenFluitBereik(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 3));
        GuhQuests.saved(p).remove(AppaPlatform.KEY);
        Appa.houdWeg(p, false);
        Appa.geef(p);
        Appa.geladen(p).discard();
        nl.juiced.guhs.feature.einde2.appa.AppaFluitjeItem.blaas(p, false);
        helper.assertTrue(Appa.geladen(p) == null, "no platform here: the whistle does not bring him outside the Guheinde");
        BlockPos m = legPlatform(helper, new BlockPos(8, 1, 8));
        AppaPlatform.zet(p, level.dimension(), m);
        p.teleportTo(m.getX() + 0.5 + AppaPlatform.BEREIK + 5, m.getY() + 1, m.getZ() + 0.5);
        nl.juiced.guhs.feature.einde2.appa.AppaFluitjeItem.blaas(p, false);
        helper.assertTrue(Appa.geladen(p) == null, "more than " + (int) AppaPlatform.BEREIK + " blocks from the platform: he does not come");
        p.teleportTo(m.getX() + 0.5, m.getY() + 1, m.getZ() + 0.5 - 5);
        level.setBlockAndUpdate(m.offset(2, 0, 2), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        nl.juiced.guhs.feature.einde2.appa.AppaFluitjeItem.blaas(p, false);
        helper.assertTrue(Appa.geladen(p) == null, "near an unfinished platform: he does not come (it says what is missing)");
        legPlatform(helper, new BlockPos(8, 1, 8));
        nl.juiced.guhs.feature.einde2.appa.AppaFluitjeItem.blaas(p, false);
        AppaGuhEntity appa = Appa.geladen(p);
        helper.assertTrue(appa != null && Math.abs(appa.getX() - (m.getX() + 0.5)) < 0.01 && Math.abs(appa.getZ() - (m.getZ() + 0.5)) < 0.01
                && appa.getY() > m.getY() + 1.5, "within range of the complete platform: he appears above its middle");
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(!appa.isRemoved() && Math.abs(appa.getY() - (m.getY() + 1)) < 0.6 && Math.abs(appa.getX() - (m.getX() + 0.5)) < 1.0
                    && Math.abs(appa.getZ() - (m.getZ() + 0.5)) < 1.0, "and lands on it (" + appa.position() + ")");
            GuhQuests.saved(p).remove(AppaPlatform.KEY);
            weg(helper, p);
            helper.succeed();
        });
    }
}
