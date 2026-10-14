package nl.juiced.guhs.feature.park.plek;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.bio.bouwmeer.RoeibootjeEntity;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.waterdiertjes.GuhEendjeEntity;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.4.2 (Het Knabbelpark, plekjes A): the game tests of the five plekjes.
 * <ul>
 *   <li>{@link #plekTemplates}: the five templates really placed in the test world (far away, flat land) and walked from
 *       their entrance: you reach the jetty and the berth, the sandbox, the swings, the seesaw and the slide's ladder, the
 *       kraampje's counter and a blanket, the jukebox on the tent's floor and the kas, the zen corner over the bridge and
 *       the pond; and what they hold (the playground pieces, the jukebox, the inhabitants with their Bezetting tags);</li>
 *   <li>the mechanics: the sandbox (once a day, per player), the swing and the seesaw (they move, the block follows, back at
 *       rest when you get off), the slide (an entity really slides down), the park jukebox (a park record on, off, never
 *       out; your own record goes its own way), the eendjes (fed, their rijtje hops and stays little, a lost rijtje comes
 *       back), the rowing boat (one in the berth, never two), the Knabbelkoi (a knabbel, no bucket, no kleintjes), the
 *       kraampjesguh's shop (all for vads), going home.</li>
 * </ul>
 */
public class PlekAGameTests {
    private static final String KAMER = "park_test_kamer";
    private static final String BATCH = "parkplek_a";

    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        p.stopRiding();
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static boolean adv(ServerPlayer p, String naam) {
        AdvancementHolder holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    // =================================================================================================================
    /** Where a template's local spot is when its corner (template 0,0,0) is at {@code hoek} (not turned). */
    private static BlockPos w(BlockPos hoek, int x, int y, int z) {
        return hoek.offset(x, y, z);
    }

    @GuhTest(template = "empty", batch = BATCH + "_bouw", timeoutTicks = 2000)
    public static void plekTemplates(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> fouten = new ArrayList<>();
        int i = 0;
        for (String naam : List.of("eendenvijver", "speeltuin", "picknickweide", "muziektuin", "japanse_tuin")) {
            StructureTemplate t = level.getStructureManager().get(Guhs.id("knabbelpark/" + naam)).orElse(null);
            if (t == null) {
                fouten.add(naam + ": no template");
                continue;
            }
            int bx = 1900 * 16 + 40 * i++, bz = 60 * 16;
            for (int cx = (bx - 8) >> 4; cx <= (bx + 40) >> 4; cx++) {
                for (int cz = (bz - 8) >> 4; cz <= (bz + 40) >> 4; cz++) {
                    level.getChunk(cx, cz);
                }
            }
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
            BlockPos hoek = new BlockPos(bx, top - 2, bz);
            t.placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
            var size = t.getSize();
            BoundingBox doos = BoundingBox.fromCorners(hoek.offset(-1, -1, -1), hoek.offset(size.getX(), size.getY(), size.getZ()));
            Set<BlockPos> bereikt = loop(level, w(hoek, size.getX() / 2, 2, size.getZ() - 1), doos);
            Map<String, double[]> doelen = switch (naam) {
                case "eendenvijver" -> Map.of("de steiger", new double[]{9, 2, 8, 1.2}, "de ligplaats", new double[]{6, 2, 7, 2.5},
                        "een bankje aan de vijver", new double[]{3, 2, 11, 1.5});
                case "speeltuin" -> Map.of("de zandbak", new double[]{4, 2, 10, 1.0}, "de schommels", new double[]{11, 2, 8, 1.5},
                        "de wip", new double[]{12, 2, 12, 1.5}, "de ladder", new double[]{1, 2, 3, 1.5}, "het eind van de glijbaan", new double[]{8, 2, 3, 1.5});
                case "picknickweide" -> Map.of("het kraampje", new double[]{7, 2, 2, 2.5}, "een kleed", new double[]{2, 2, 6, 1.5});
                case "muziektuin" -> Map.of("de jukebox", new double[]{8, 3, 9, 1.5}, "de kas", new double[]{13, 2, 3, 1.0},
                        "de bloemen", new double[]{2, 2, 3, 1.5});
                default -> Map.of("de zenhoek over het bruggetje", new double[]{4, 2, 2, 1.5}, "het bankje bij de koivijver", new double[]{7, 2, 8, 1.5},
                        "de top van het bruggetje", new double[]{3, 3, 5, 1.2});
            };
            for (Map.Entry<String, double[]> d : doelen.entrySet()) {
                double[] v = d.getValue();
                if (!dichtbij(bereikt, w(hoek, (int) v[0], (int) v[1], (int) v[2]), v[3])) {
                    fouten.add(naam + ": " + d.getKey() + " is not reached on foot (" + bereikt.size() + " spots)");
                }
            }
            // what the template holds
            Map<Block, Integer> n = tel(level, doos);
            switch (naam) {
                case "eendenvijver" -> eis(fouten, naam, n, Blocks.WATER, 40);
                case "speeltuin" -> {
                    eis(fouten, naam, n, PlekA.ZANDBAK.get(), 9);
                    eis(fouten, naam, n, PlekA.GLIJBAAN.get(), 3);
                    eis(fouten, naam, n, PlekA.SCHOMMEL.get(), 2);
                    eis(fouten, naam, n, PlekA.WIP.get(), 1);
                }
                case "picknickweide" -> eis(fouten, naam, n, PlekA.PICKNICKKLEED.get(), 20);
                case "muziektuin" -> eis(fouten, naam, n, Blocks.JUKEBOX, 1);
                default -> {
                    eis(fouten, naam, n, Blocks.MANGROVE_SLAB, 12);
                    eis(fouten, naam, n, Blocks.WATER, 30);
                }
            }
            AABB a = AABB.of(doos);
            switch (naam) {
                case "picknickweide" -> {
                    if (level.getEntitiesOfClass(GuhNpcEntity.class, a, e -> e.getKind() == GuhNpcEntity.Kind.KRAAMGUH
                            && PlekA.KRAAM_ID.equals(e.getPersistentData().getStringOr(Bezetting.TAG, ""))).size() != 1) {
                        fouten.add(naam + ": not one kraampjesguh with its tag");
                    }
                }
                case "muziektuin" -> {
                    if (level.getEntitiesOfClass(GuhEntity.class, a, e -> e.getPersistentData().getStringOr(Bezetting.TAG, "").startsWith(PlekA.DANSER_ID)).size() != 3) {
                        fouten.add(naam + ": not three dancers with their tags");
                    }
                }
                case "japanse_tuin" -> {
                    if (level.getEntitiesOfClass(Knabbelkoi.class, a, e -> e.getPersistentData().getStringOr(Bezetting.TAG, "").startsWith(PlekA.KOI_ID)
                            && e.isInWater()).size() != 4) {
                        fouten.add(naam + ": not four Knabbelkoi's in the water");
                    }
                }
                default -> {
                }
            }
            for (Entity e : level.getEntitiesOfClass(Entity.class, a.inflate(4), e -> !(e instanceof Player))) {
                e.discard();
            }
        }
        helper.assertTrue(fouten.isEmpty(), "the plekjes: " + fouten);
        helper.succeed();
    }

    private static void eis(List<String> fouten, String naam, Map<Block, Integer> n, Block b, int min) {
        if (n.getOrDefault(b, 0) < min) {
            fouten.add(naam + ": " + n.getOrDefault(b, 0) + " x " + b + " (at least " + min + ")");
        }
    }

    private static Map<Block, Integer> tel(ServerLevel level, BoundingBox doos) {
        Map<Block, Integer> n = new java.util.HashMap<>();
        for (BlockPos p : BlockPos.betweenClosed(doos.minX(), doos.minY(), doos.minZ(), doos.maxX(), doos.maxY(), doos.maxZ())) {
            n.merge(level.getBlockState(p).getBlock(), 1, Integer::sum);
        }
        return n;
    }

    private static boolean dichtbij(Set<BlockPos> bereikt, BlockPos doel, double r) {
        for (BlockPos p : bereikt) {
            double dx = p.getX() - doel.getX(), dz = p.getZ() - doel.getZ();
            if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - doel.getY()) <= 2) {
                return true;
            }
        }
        return false;
    }

    private static boolean vrij(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }

    private static boolean staan(ServerLevel level, BlockPos p) {
        BlockPos onder = p.below();
        BlockState s = level.getBlockState(onder);
        boolean vloer = !s.getCollisionShape(level, onder).isEmpty() && s.getCollisionShape(level, onder).max(Direction.Axis.Y) <= 1.0
                || !level.getFluidState(onder).isEmpty();
        return vloer && vrij(level, p) && vrij(level, p.above());
    }

    /** Every spot reached on foot (up 1 with headroom, down 3; water is waded) from {@code van} inside {@code doos} (like ParkKeur). */
    private static Set<BlockPos> loop(ServerLevel level, BlockPos van, BoundingBox doos) {
        Set<BlockPos> gezien = new HashSet<>();
        BlockPos begin = null;
        for (int dy = 0; dy <= 2 && begin == null; dy++) {
            for (int s : new int[]{dy, -dy}) {
                if (staan(level, van.above(s))) {
                    begin = van.above(s);
                    break;
                }
            }
        }
        if (begin == null) {
            return gezien;
        }
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        rij.add(begin);
        gezien.add(begin);
        while (!rij.isEmpty()) {
            BlockPos p = rij.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!doos.isInside(n)) {
                    continue;
                }
                for (int dy : new int[]{0, 1, -1, -2, -3}) {
                    BlockPos q = n.above(dy);
                    if (gezien.contains(q)) {
                        break;
                    }
                    if (dy == 1 && !vrij(level, p.above(2))) {
                        continue;
                    }
                    if (dy < 0) {
                        boolean open = true;
                        for (int k = 0; k > dy; k--) {
                            open &= vrij(level, n.above(k)) && vrij(level, n.above(k + 1));
                        }
                        if (!open) {
                            break;
                        }
                    }
                    if (staan(level, q)) {
                        gezien.add(q);
                        rij.add(q);
                        break;
                    }
                }
            }
        }
        return gezien;
    }

    // =================================================================================================================
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void plekZandbak(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 7));
        ServerPlayer q = speler(helper, new BlockPos(6, 2, 7));
        try {
            BlockPos zand = helper.absolutePos(new BlockPos(5, 1, 5));
            level.setBlockAndUpdate(zand, PlekA.ZANDBAK.get().defaultBlockState());
            GuhQuests.saved(p).remove(SpeelBlokken.Zandbak.DAG);
            GuhQuests.saved(q).remove(SpeelBlokken.Zandbak.DAG);
            helper.assertTrue(SpeelBlokken.Zandbak.graaf(p, zand) && GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()) == 1, "the kaasknabbel of the day");
            helper.assertTrue(!SpeelBlokken.Zandbak.graaf(p, zand) && GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()) == 1, "only once a day");
            helper.assertTrue(SpeelBlokken.Zandbak.graaf(q, zand) && GuhQuests.count(q, ModItems.KAAS_KNABBELS.get()) == 1, "every player their own");
            GuhQuests.saved(p).putLong(SpeelBlokken.Zandbak.DAG, level.getGameTime() / SpeelBlokken.Zandbak.DAG_TICKS - 1);
            helper.assertTrue(SpeelBlokken.Zandbak.graaf(p, zand) && GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()) == 2, "the next day another");
            helper.assertTrue(adv(p, "parkplek_zandbak"), "the advancement");
            // a click with anything in your hand digs too, and the sand can't be had
            helper.assertTrue(level.getBlockState(zand).getBlock().asItem() == Items.AIR, "the sandbox sand is no item");
            helper.succeed();
        } finally {
            weg(helper, p);
            weg(helper, q);
        }
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void plekSchommel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(7, 2, 7);
        BlockPos pos = helper.absolutePos(rel);
        level.setBlockAndUpdate(pos, PlekA.SCHOMMEL.get().defaultBlockState().setValue(SpeelBlokken.Schommel.FACING, Direction.SOUTH));
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 5));
        ParkZitje z = ParkZitje.zet(level, pos, ParkZitje.Soort.SCHOMMEL, 0, p);
        helper.assertTrue(z != null && p.getVehicle() == z && adv(p, "parkplek_schommel"), "sitting on the swing");
        helper.assertTrue(ParkZitje.op(level, pos, 0) == z, "the swing is taken");
        double[] zMin = {Double.MAX_VALUE}, zMax = {-Double.MAX_VALUE};
        Set<Integer> standen = new HashSet<>();
        helper.onEachTick(() -> {
            zMin[0] = Math.min(zMin[0], z.getZ());
            zMax[0] = Math.max(zMax[0], z.getZ());
            standen.add(level.getBlockState(pos).getValue(SpeelBlokken.STAND));
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(zMax[0] - zMin[0] > 1.6 && standen.contains(0) && standen.contains(6),
                        "it swings: " + (zMax[0] - zMin[0]) + " " + standen))
                .thenExecute(() -> {
                    double mid = pos.getZ() + 0.5;
                    helper.assertTrue(Math.abs(zMax[0] - mid - ParkZitje.TOUW * Math.sin(Math.toRadians(ParkZitje.SCHOMMEL_GRADEN))) < 0.15,
                            "towards FACING as far as the rope goes: " + (zMax[0] - mid));
                    p.stopRiding();
                })
                .thenWaitUntil(() -> helper.assertTrue(z.isRemoved() && level.getBlockState(pos).getValue(SpeelBlokken.STAND) == SpeelBlokken.STAND_RECHT,
                        "off: the seat gone, the swing hanging straight"))
                .thenExecute(() -> weg(helper, p))
                .thenSucceed();
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void plekWip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(7, 2, 7));
        level.setBlockAndUpdate(pos, PlekA.WIP.get().defaultBlockState().setValue(SpeelBlokken.Wip.FACING, Direction.EAST));
        ServerPlayer a = speler(helper, new BlockPos(9, 2, 7));
        ServerPlayer b = speler(helper, new BlockPos(5, 2, 7));
        BlockHitResultHolder.klik(level, pos, a);
        BlockHitResultHolder.klik(level, pos, b);
        ParkZitje za = ParkZitje.op(level, pos, 1), zb = ParkZitje.op(level, pos, -1);
        helper.assertTrue(za != null && zb != null && a.getVehicle() == za && b.getVehicle() == zb, "each on the end nearest to them");
        helper.assertTrue(za.getX() > pos.getX() + 1 && zb.getX() < pos.getX(), "the ends: east and west");
        Set<SpeelBlokken.Kant> kanten = new HashSet<>();
        boolean[] tegen = {true};
        helper.onEachTick(() -> {
            kanten.add(level.getBlockState(pos).getValue(SpeelBlokken.KANT));
            if (!za.isRemoved() && !zb.isRemoved() && Math.abs((za.getY() - pos.getY() - ParkZitje.WIP_ZIT) + (zb.getY() - pos.getY() - ParkZitje.WIP_ZIT)) > 0.05) {
                tegen[0] = false;
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(kanten.contains(SpeelBlokken.Kant.A) && kanten.contains(SpeelBlokken.Kant.B), "up and down: " + kanten))
                .thenExecute(() -> {
                    helper.assertTrue(tegen[0], "the two ends always move against each other");
                    helper.assertTrue(adv(a, "parkplek_wip") && adv(b, "parkplek_wip"), "both advancements");
                    a.stopRiding();
                    b.stopRiding();
                })
                .thenWaitUntil(() -> helper.assertTrue(za.isRemoved() && zb.isRemoved() && level.getBlockState(pos).getValue(SpeelBlokken.KANT) == SpeelBlokken.Kant.VLAK,
                        "off: level again"))
                .thenExecute(() -> {
                    weg(helper, a);
                    weg(helper, b);
                })
                .thenSucceed();
    }

    /** A right-click on a block with an empty hand, as the game does it. */
    private static final class BlockHitResultHolder {
        static void klik(ServerLevel level, BlockPos pos, ServerPlayer p) {
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            level.getBlockState(pos).useWithoutItem(level, p, hit);
        }
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void plekGlijbaan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // three slide pieces east, down from y 4 to y 2, the ground at y 1
        for (int i = 0; i < 3; i++) {
            helper.setBlock(new BlockPos(4 + i, 4 - i, 7), PlekA.GLIJBAAN.get().defaultBlockState().setValue(SpeelBlokken.Glijbaan.FACING, Direction.EAST));
        }
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        BlockPos top = helper.absolutePos(new BlockPos(4, 4, 7));
        guh.setBehavior(GuhEntity.Behavior.PASSIVE);
        guh.snapTo(top.getX() + 0.3, top.getY() + 0.7, top.getZ() + 0.5, 0, 0);
        level.addFreshEntity(guh);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(guh.getX() > helper.absolutePos(new BlockPos(7, 2, 7)).getX() && guh.onGround(),
                        "the guh slid down and out: " + guh.position()))
                .thenExecute(() -> {
                    helper.assertTrue(guh.fallDistance < 1 && guh.getHealth() == guh.getMaxHealth(), "no fall");
                    guh.discard();
                    // a player's slide: wieee (the advancement)
                    ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
                    SpeelBlokken.Glijbaan.glij(Direction.EAST, level, p);
                    helper.assertTrue(adv(p, "parkplek_glijbaan") && p.getDeltaMovement().x > 0.3 && p.getDeltaMovement().y < -0.3, "down the slope");
                    weg(helper, p);
                })
                .thenSucceed();
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void plekJukebox(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(7, 2, 7));
        level.setBlockAndUpdate(pos, Blocks.JUKEBOX.defaultBlockState());
        JukeboxBlockEntity box = (JukeboxBlockEntity) level.getBlockEntity(pos);
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 5));
        try {
            helper.assertTrue(PlekAEvents.jukebox(p, pos, ItemStack.EMPTY), "an empty hand: handled");
            helper.assertTrue(PlekAEvents.isPlaatje(box.getTheItem()) && box.getSongPlayer().isPlaying() && adv(p, "parkplek_muziek"), "a park record plays");
            helper.assertTrue(PlekAEvents.jukebox(p, pos, new ItemStack(Items.DIRT)) && box.getTheItem().isEmpty(), "a click: off, and the record is not dropped");
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(pos).inflate(3)).isEmpty(), "nothing on the ground");
            helper.assertTrue(PlekAEvents.jukebox(p, pos, ItemStack.EMPTY) && box.getSongPlayer().isPlaying(), "on again");
            box.getSongPlayer().stop(level, box.getBlockState());
            ItemStack eerst = box.getTheItem().copy();
            helper.assertTrue(PlekAEvents.jukebox(p, pos, ItemStack.EMPTY) && box.getSongPlayer().isPlaying() && PlekAEvents.isPlaatje(box.getTheItem()),
                    "the song was over: another record " + eerst);
            box.setTheItem(ItemStack.EMPTY);
            helper.assertTrue(!PlekAEvents.jukebox(p, pos, new ItemStack(Items.MUSIC_DISC_CAT)), "your own record: the jukebox's own");
            box.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
            helper.assertTrue(!PlekAEvents.jukebox(p, pos, ItemStack.EMPTY), "your own record comes out as always");
            box.setTheItem(ItemStack.EMPTY);
            helper.succeed();
        } finally {
            weg(helper, p);
        }
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void plekEendjes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 3));
        GuhEendjeEntity mama = PlekA.eendje(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 2, 7))));
        mama.getPersistentData().putString(Bezetting.TAG, PlekA.EEND_ID + "1@test");
        level.addFreshEntity(mama);
        try {
            List<GuhEendjeEntity> rij = mama.rijtje();
            helper.assertTrue(rij.size() == PlekA.KUIKENS && rij.stream().allMatch(k -> k.isBaby() && PlekAEvents.isParkEendje(k) && k.isInvulnerable()),
                    "a rijtje of three park kuikentjes: " + rij.size());
            helper.assertTrue(PlekAEvents.isParkEendje(mama), "a park mama");
            ItemStack brood = new ItemStack(Items.BREAD, 3);
            PlekAEvents.voerEendje(p, mama, brood);
            helper.assertTrue(brood.getCount() == 2 && adv(p, "parkplek_eendjes"), "fed: one bread, the advancement");
            helper.assertTrue(PlekAEvents.isEendenvoer(new ItemStack(ModItems.KAAS_KNABBELS.get())) && PlekAEvents.isEendenvoer(new ItemStack(Items.WHEAT_SEEDS))
                    && !PlekAEvents.isEendenvoer(new ItemStack(Items.DIRT)), "what they eat");
            rij.get(0).setAge(-10);
            rij.get(1).discard();
            rij.get(2).discard();
            helper.assertTrue(PlekAEvents.rijtje(level, mama) == 2 && mama.rijtje().size() == PlekA.KUIKENS, "a lost rijtje comes back");
            helper.assertTrue(mama.rijtje().stream().allMatch(k -> k.getAge() <= GuhEendjeEntity.KUIKEN_TIJD / 2), "the kuikentjes stay little");
            helper.succeed();
        } finally {
            mama.rijtje().forEach(Entity::discard);
            mama.discard();
            weg(helper, p);
        }
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void plekBoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 4; x <= 8; x++) {
            for (int z = 4; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
            }
        }
        BlockPos ligplaats = helper.absolutePos(new BlockPos(6, 1, 6));
        helper.assertTrue(PlekAEvents.boot(level, ligplaats, Rotation.NONE), "a boat comes");
        helper.assertTrue(!PlekAEvents.boot(level, ligplaats, Rotation.NONE), "never two");
        List<RoeibootjeEntity> boten = level.getEntitiesOfClass(RoeibootjeEntity.class, new AABB(ligplaats).inflate(4));
        helper.assertTrue(boten.size() == 1 && ligplaats.equals(boten.get(0).thuis()), "one roeibootje, at home in the berth");
        boten.forEach(Entity::discard);
        helper.assertTrue(!PlekAEvents.boot(level, helper.absolutePos(new BlockPos(2, 2, 2)), Rotation.NONE), "no water: no boat");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void plekKnabbelkoi(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 4; x <= 8; x++) {
            for (int z = 4; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
            }
        }
        Knabbelkoi koi = PlekA.koi(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 1, 6))));
        level.addFreshEntity(koi);
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 6));
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            koi.interact(p, InteractionHand.MAIN_HAND, koi.position());
            helper.assertTrue(p.getMainHandItem().is(Items.WATER_BUCKET) && koi.isAlive(), "no bucket for a Knabbelkoi");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
            koi.interact(p, InteractionHand.MAIN_HAND, koi.position());
            helper.assertTrue(p.getMainHandItem().getCount() == 1 && adv(p, "parkplek_knabbelkoi"), "a kaasknabbel: eaten, plons");
            helper.assertTrue(!koi.uitgerust() && !koi.removeWhenFarAway(10000), "no kleintjes, it stays");
            helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.KNABBELKOI) && GuhDex.EXTRA.contains(GuhVariant.KNABBELKOI)
                    && GuhDex.isCreaturePage(GuhVariant.KNABBELKOI) && GuhVariant.KNABBELKOI.isCharacter(), "a bonus creature page");
            helper.succeed();
        } finally {
            koi.discard();
            weg(helper, p);
        }
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void plekKraamguhEnThuis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.KRAAMGUH);
        BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
        npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        level.addFreshEntity(npc);
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 5));
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        BlockPos ver = helper.absolutePos(new BlockPos(12, 2, 12));
        guh.snapTo(ver.getX() + 0.5, ver.getY(), ver.getZ() + 0.5, 0, 0);
        level.addFreshEntity(guh);
        try {
            NpcRole rol = NpcRollen.rol(GuhNpcEntity.Kind.KRAAMGUH);
            helper.assertTrue(rol != null, "the kraampjesguh has a role");
            var offers = rol.offers(npc);
            helper.assertTrue(offers != null && offers.size() >= 8 && offers.stream().allMatch(o -> o.getItemCostA().item().value() == ModItems.VAHOEGE_VADS.get()),
                    "a shop, all for vads");
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(PlekA.SCHOMMEL_ITEM.get())), "a swing for sale");
            rol.talk(npc, p);
            helper.assertTrue(adv(p, "parkplek_kraampje"), "the advancement");
            BlockPos thuis = helper.absolutePos(new BlockPos(3, 2, 10));
            helper.assertTrue(!PlekAEvents.thuis(guh, ver, 3), "at home: stays");
            helper.assertTrue(PlekAEvents.thuis(guh, thuis, 3) && guh.getNavigation().isInProgress(), "far from home: goes back");
            helper.succeed();
        } finally {
            npc.discard();
            guh.discard();
            weg(helper, p);
        }
    }
}
