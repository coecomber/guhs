package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker.Run;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker.Staat;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kamer;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPayloads;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.4 (dungeons): the last 3 run creatures glow for the members and the tracker points at the nearest; a creature that got out
 * of the cave is put back on a free spawn spot of its OWN room (spread, never all on one spot) and goes after the nearest member.
 * Run: {@code runGameTestServer -Pgt=VadsrijkDungeonGloed}.
 */
public class VadsrijkDungeonGloedGameTests {
    private static final String BATCH = "vadsrijk_dungeon_gloed";

    /** A small 2-room test cave with 9 spawn spots per room (3 x 3 round the room centre). */
    static Kerker kerker(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (int x = -1; x <= 13; x++) {
            for (int z = -1; z <= 13; z++) {
                sl.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y < 5; y++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        int X = o.getX(), Y = o.getY(), Z = o.getZ();
        return new Kerker("zwamgrot", 1, X - 1, Y - 2, Z - 1, X + 13, Y + 8, Z + 13, 11, 13, 13, 14,
                List.of(new Kamer("kamer_1", X + 3, Y, Z + 3, 2, plekken(X + 3, Y, Z + 3)), new Kamer("kamer_2", X + 9, Y, Z + 3, 2, plekken(X + 9, Y, Z + 3))),
                new Kamer("baaskamer", X + 6, Y, Z + 10, 3),
                List.of(new int[] {X + 5, Y, Z + 7, Y + 4}, new int[] {X + 6, Y, Z + 7, Y + 4}, new int[] {X + 7, Y, Z + 7, Y + 4}),
                new BlockPos(X + 6, Y, Z + 7),
                List.of(new int[] {X + 6, Y, Z + 10, 3}));
    }

    static int[] plekken(int x, int y, int z) {
        int[] p = new int[27];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                p[i++] = x + dx;
                p[i++] = y;
                p[i++] = z + dz;
            }
        }
        return p;
    }

    static Set<BlockPos> plekkenVan(Kamer k) {
        Set<BlockPos> s = new HashSet<>();
        for (int i = 0; i < k.aantalPlekken(); i++) {
            s.add(k.plek(i));
        }
        return s;
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void laatsteGloedEnTerug(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Kerker k = kerker(helper);
        Kerkers.TEST.put(k.id(), k);
        Run r = GrotKerker.run(k.id());
        GrotKerker.reset(sl, k, r, null);
        ServerPlayer p = GuhMockPlayer.of(helper), vreemd = GuhMockPlayer.of(helper);
        Spelers.data(p).extra.remove(GrotKerker.DATA);
        p.teleportTo(k.kamers().get(0).x() + 0.5, k.kamers().get(0).y(), k.kamers().get(0).z() + 0.5);
        p.setGameMode(GameType.SURVIVAL);
        vreemd.teleportTo(k.maxX() + 40.5, k.minY() + 2, k.maxZ() + 40.5);   // (outside: never a participant)
        GrotKerker.TEST_KLAAR = true;
        DungeonRij.wisAlles();
        helper.runAfterDelay(5, () -> {
            try {
                DungeonRij.devToegang(p, k.id());
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.staat == Staat.BEZIG && r.plan.size() == 8, "the run started solo: 3 + 4 + the elite, got " + r.plan.size());
                // the slots of room 2 (k2_0..3 + the elite) stay; kill room 1 and k2_0: 4 left
                List<String> slots = new ArrayList<>(r.plan.keySet());
                for (String s : slots) {
                    if (s.startsWith("k1_") || s.equals("k2_0")) {
                        ((VadsrijkMob) sl.getEntity(r.levend.get(s))).verslagen(p);
                    }
                }
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(GrotKerker.over(r) == 4 && r.gloeit.isEmpty(), "4 left: no glow yet (" + GrotKerker.over(r) + ")");
                helper.assertTrue(GrotKerker.volgRegel(p) == null, "4 left: no tracker line");
                ((VadsrijkMob) sl.getEntity(r.levend.get("k2_1"))).verslagen(p);
                GrotKerker.tick(sl, k, r);
                List<VadsrijkMob> rest = GrotKerker.resterend(sl, r);
                helper.assertTrue(GrotKerker.over(r) == 3 && rest.size() == 3, "3 left: " + GrotKerker.over(r));
                Set<UUID> ids = new HashSet<>();
                rest.forEach(m -> ids.add(m.getUUID()));
                helper.assertTrue(r.gloeit.equals(ids), "the last 3 glow for the member");
                for (VadsrijkMob m : rest) {
                    helper.assertFalse(m.isCurrentlyGlowing(), "not for everyone: the creature itself has no glow flag");
                }
                // the tracker: "Nog 3 over" + the nearest as the target (members only)
                NpcPayloads.VolgRegel regel = GrotKerker.volgRegel(p);
                VadsrijkMob dichtst = rest.stream().min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).orElseThrow();
                helper.assertTrue(regel != null && regel.doel().isPresent() && regel.doel().get().equals(dichtst.blockPosition()), "the arrow points at the nearest");
                helper.assertTrue(regel.wat().getContents() instanceof TranslatableContents tc && tc.getKey().equals("vadsrijk.grotkerker.volg.over")
                        && tc.getArgs().length == 1 && Integer.valueOf(3).equals(tc.getArgs()[0]), "\"Nog 3 over\"");
                helper.assertTrue(GrotKerker.volgRegel(vreemd) == null, "a non-member gets no line");
                // the put-back: two creatures of room 2 thrown out of the cave, ten times
                Kamer kamer2 = k.kamers().get(1);
                Set<BlockPos> eigen = plekkenVan(kamer2);
                VadsrijkMob a = (VadsrijkMob) sl.getEntity(r.levend.get("k2_2")), b = (VadsrijkMob) sl.getEntity(r.levend.get("k2_3"));
                Set<BlockPos> gezien = new HashSet<>();
                for (int ronde = 0; ronde < 10; ronde++) {
                    a.teleportTo(k.maxX() + 20.5, a.getY(), k.maxZ() + 20.5);
                    b.teleportTo(k.maxX() + 22.5, b.getY(), k.maxZ() + 20.5);
                    a.setTarget(null);
                    b.setTarget(null);
                    GrotKerker.leiband(sl, k, r);
                    for (Entity m : List.of(a, b)) {
                        helper.assertTrue(k.bevat(m.getX(), m.getY(), m.getZ()) && eigen.contains(m.blockPosition()),
                                "put back on a spawn spot of its own room: " + m.blockPosition().toShortString());
                    }
                    helper.assertFalse(a.blockPosition().equals(b.blockPosition()), "never both on the same spot: " + a.blockPosition().toShortString());
                    helper.assertTrue(a.getTarget() == p && b.getTarget() == p, "they go after the member right away");
                    gezien.add(a.blockPosition());
                    gezien.add(b.blockPosition());
                }
                helper.assertTrue(gezien.size() >= 4, "spread over the room's spots, not one fixed point: " + gezien.size() + " spots");
                // only standable spots, and a free one before a taken one: fill all but two spots of room 2 with rock
                List<BlockPos> lijst = new ArrayList<>(eigen);
                BlockPos vrij1 = lijst.get(0);
                BlockPos vrij2 = lijst.stream().max((x, y) -> Double.compare(x.distSqr(vrij1), y.distSqr(vrij1))).orElseThrow();   // (>= 1.5 apart)
                for (BlockPos s : lijst) {
                    if (!s.equals(vrij1) && !s.equals(vrij2)) {
                        sl.setBlock(s, Blocks.STONE.defaultBlockState(), 2);
                    }
                }
                b.teleportTo(vrij1.getX() + 0.5, vrij1.getY(), vrij1.getZ() + 0.5);
                Entity elite = sl.getEntity(r.levend.get("elite"));   // (out of the way: in room 1, still inside the cave)
                elite.teleportTo(k.kamers().get(0).x() + 0.5, k.kamers().get(0).y(), k.kamers().get(0).z() + 0.5);
                for (int ronde = 0; ronde < 5; ronde++) {
                    a.teleportTo(k.maxX() + 20.5, a.getY(), k.maxZ() + 20.5);
                    GrotKerker.leiband(sl, k, r);
                    helper.assertTrue(a.blockPosition().equals(vrij2), "the only free standable spot (b stands on the other): a " + a.blockPosition().toShortString()
                            + " vrij1 " + vrij1.toShortString() + " vrij2 " + vrij2.toShortString() + " b " + b.blockPosition().toShortString() + " round " + ronde);
                }
                helper.succeed();
            } finally {
                GrotKerker.reset(sl, k, r, null);
                GrotKerker.TEST_KLAAR = false;
                DungeonRij.wisAlles();
                Kerkers.TEST.remove(k.id());
                Spelers.data(p).extra.remove(GrotKerker.DATA);
            }
        });
    }
}
