package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.TegelBlokken;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Roquefort;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * After 1.10.29 (the owner at Kapitein Roquefort: "it says, far too briefly, 'spring naar het wrak (roze)' and then I die
 * immediately"): fase 3 on the REAL map. The wreck of the shipped map tiles is checked (a floor to stand on inside the pink
 * circle, reachable from the deck by stepping off its edge, 3 down), then the deck and the wreck are copied into the test world
 * (far away, same height) and the sinking runs there: 10 s of warning, no sinking hit before it ends, the first two hits halved,
 * nobody on the wreck is hit, and the whole wreck counts as the fight. Run: {@code runGameTestServer -Pgt=VadsrijkZ3Roquefort}.
 */
public class VadsrijkZ3RoquefortGameTests {
    private static final String BATCH = "vadsrijk_z3_roquefort";

    private static boolean vrij(TegelBlokken t, BlockPos p) {
        return t.staat(p).isAir();
    }

    /** Can a player stand with his feet at {@code p} in the map tiles (a full floor block under it, two blocks of air)? */
    private static boolean stand(TegelBlokken t, BlockPos p) {
        BlockState vloer = t.staat(p.below());
        return vloer.isSolidRender() && vrij(t, p) && vrij(t, p.above());
    }

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.CREATIVE);   // (creative: the boss shows his moves at them, but nothing knocks them out mid-test)
        p.snapTo(waar.x, waar.y, waar.z);
        Spelers.data(p).vads = Spelers.maxVads(p);
        return p;
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 500)
    public static void roquefortZinktOpDeEchteKaart(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        TegelBlokken tegels = new TegelBlokken(sl);
        BlockPos dek = Z3BazenPlekken.ROQUEFORT_CENTRUM, wrak = Z3BazenPlekken.ROQUEFORT_WRAK;
        helper.assertTrue(tegels.heeftTegel(dek) && tegels.heeftTegel(wrak), "the deck " + dek.toShortString() + " and the wreck " + wrak.toShortString() + " are on the map tiles");
        Roquefort maat = Z3BazenRegistry.ROQUEFORT.get().create(sl, EntitySpawnReason.COMMAND);   // (not added: only its safe-zone rule)

        // ---- the wreck in the shipped tiles: a floor inside the pink circle, 3 below the deck, right against its edge ----------
        helper.assertTrue(stand(tegels, dek) && stand(tegels, wrak), "the deck centre and the wreck centre are standable");
        int zakt = dek.getY() - wrak.getY();
        helper.assertTrue(zakt >= 0 && zakt <= 3, "the wreck floor lies at most 3 below the deck (no fall damage): " + zakt);
        int cirkel = 0, vloer = 0, rand = 0;
        List<BlockPos> wrakVloer = new ArrayList<>();
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                double d = Math.sqrt(x * x + z * z);
                BlockPos p = wrak.offset(x, 0, z);
                if (d <= 6) {
                    cirkel++;
                    if (stand(tegels, p)) {
                        vloer++;
                        wrakVloer.add(p);
                        helper.assertTrue(maat.veiligOpWrak(Vec3.atBottomCenterOf(p)), "standing on the wreck floor at " + p.toShortString() + " is safe");
                    }
                } else if (d <= 9.5) {
                    for (int dy = -3; dy <= -1; dy++) {   // the hull rim round the floor (lower: nobody climbs back from there)
                        if (stand(tegels, p.above(dy))) {
                            rand++;
                            helper.assertTrue(maat.veiligOpWrak(Vec3.atBottomCenterOf(p.above(dy))), "the hull rim at " + p.above(dy).toShortString() + " counts as on the wreck");
                        }
                    }
                }
            }
        }
        helper.assertTrue(vloer >= cirkel * 9 / 10, "the pink circle (r 6) lies on a floor: " + vloer + " of " + cirkel + " cells standable");
        helper.assertTrue(!maat.veiligOpWrak(Vec3.atBottomCenterOf(dek)), "the middle of the deck is not safe once it sank");
        // the deck's edge borders the wreck floor directly: step off, no gap to jump
        int grens = 0;
        for (BlockPos p : wrakVloer) {
            for (BlockPos n : new BlockPos[] {p.north(), p.south(), p.east(), p.west()}) {
                BlockPos boven = n.above(zakt);
                if (zakt > 0 && stand(tegels, boven) && vrij(tegels, p.above(zakt)) && vrij(tegels, p.above(zakt + 1))) {
                    grens++;
                }
            }
        }
        helper.assertTrue(zakt == 0 || grens >= 5, "deck cells right above the wreck floor's edge (step off, no jump): " + grens);
        // and the straight walk from the deck centre to the wreck centre has a floor all the way (deck height, then wreck height)
        Vec3 a = Vec3.atBottomCenterOf(dek), b = Vec3.atBottomCenterOf(wrak);
        double lang = Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.z - a.z, 2));
        for (double s = 0; s <= lang; s += 0.25) {   // (a floor = the first block under deck height, with room to stand on it:
            BlockPos p = BlockPos.containing(a.x + (b.x - a.x) * s / lang, dek.getY(), a.z + (b.z - a.z) * s / lang);   // the deck, the stub of the old ramp, the wreck)
            int y = dek.getY();
            while (y > wrak.getY() && vrij(tegels, p.atY(y - 1))) {
                y--;
            }
            helper.assertTrue(!vrij(tegels, p.atY(y - 1)) && vrij(tegels, p.atY(y)) && vrij(tegels, p.atY(y + 1)),
                    "the walk deck -> wreck has a floor at " + p.atY(y).toShortString() + " (" + s + " of " + lang + ")");
        }
        helper.assertTrue(lang - 6 <= 14, "from the deck centre the pink circle is " + (lang - 6) + " blocks away (10 s = 43 blocks walking)");

        // ---- the fight on a copy of the real deck + wreck -----------------------------------------------------------------------
        BlockPos naar = new BlockPos(61016, dek.getY(), 61016);
        BlockPos d = naar.subtract(dek);
        int x0 = Math.min(dek.getX() - 22, wrak.getX() - 11), x1 = Math.max(dek.getX() + 22, wrak.getX() + 11);
        int z0 = Math.min(dek.getZ() - 22, wrak.getZ() - 11), z1 = Math.max(dek.getZ() + 22, wrak.getZ() + 11);
        for (int cx = (x0 + d.getX() - 8) >> 4; cx <= (x1 + d.getX() + 8) >> 4; cx++) {
            for (int cz = (z0 + d.getZ() - 8) >> 4; cz <= (z1 + d.getZ() + 8) >> 4; cz++) {
                sl.setChunkForced(cx, cz, true);
            }
        }
        List<BlockPos> gezet = new ArrayList<>();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = dek.getY() - 8; y <= dek.getY() + 8; y++) {
                    BlockPos p = new BlockPos(x, y, z), q = p.offset(d);
                    BlockState s = tegels.staat(p);
                    if (!s.isAir()) {
                        sl.setBlock(q, s, 2 | 16);
                        gezet.add(q);
                    } else if (!sl.getBlockState(q).isAir()) {
                        sl.setBlock(q, Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                }
            }
        }
        Roquefort baas = Z3BazenRegistry.ROQUEFORT.get().create(sl, EntitySpawnReason.COMMAND);
        baas.snapTo(naar.getX() + 0.5, naar.getY(), naar.getZ() + 0.5, 0, 0);
        baas.stelIn(40, null, false, Set.of());
        baas.wrak = wrak.offset(d);
        baas.zetArena(new Arena("t:roquefort", naar, Z3BazenPlekken.ROQUEFORT_STRAAL, naar, 1, naar, "vadsrijk.z3bazen.roquefort")
                .met(Z3BazenPlekken.ROQUEFORT_STRAAL, Z3BazenPlekken.KERKER_ONDER, Z3BazenPlekken.KERKER_BOVEN));
        sl.addFreshEntity(baas);
        Vec3 w = Vec3.atBottomCenterOf(baas.wrak), m = Vec3.atBottomCenterOf(naar);
        Vec3 weg = w.subtract(m).multiply(1, 0, 1).normalize();
        // on the deck, away from the wreck; in the middle of the wreck; and on the wreck's far half (outside the arena's disc)
        ServerPlayer opDek = speler(helper, m.subtract(weg.scale(6)));
        ServerPlayer opWrak = speler(helper, w);
        ServerPlayer achter = speler(helper, w.add(weg.scale(5)));
        helper.assertTrue(stand(tegels, opDek.blockPosition().subtract(d)) && stand(tegels, achter.blockPosition().subtract(d)), "the test players stand on real floor");
        helper.assertTrue(!baas.arena().binnen(achter.getX(), achter.getY(), achter.getZ()), "the far half of the wreck lies outside the arena's disc");
        baas.testFilter = q -> q == opDek || q == opWrak || q == achter;
        Runnable opruimen = () -> {
            for (Entity e : sl.getEntitiesOfClass(Entity.class, new AABB(naar).inflate(40), e -> !(e instanceof Player))) {
                e.discard();
            }
            for (BlockPos q : gezet) {
                sl.setBlock(q, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
            for (int cx = (x0 + d.getX() - 8) >> 4; cx <= (x1 + d.getX() + 8) >> 4; cx++) {
                for (int cz = (z0 + d.getZ() - 8) >> 4; cz <= (z1 + d.getZ() + 8) >> 4; cz++) {
                    sl.setChunkForced(cx, cz, false);
                }
            }
        };
        int start = 60;
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(baas.zDoetMee(opDek) && baas.zDoetMee(opWrak) && !baas.zDoetMee(achter), "before the sinking only the arena's disc is the fight");
            baas.krijgSchade(baas.vads() - baas.maxVads() * 0.6, opDek);
            helper.assertTrue(baas.fase() == 2, "fase 2 at 66 % (" + baas.fase() + ")");
            for (Z3Doel ballon : sl.getEntitiesOfClass(Z3Doel.class, new AABB(naar).inflate(40))) {
                baas.doelKapot(ballon, opDek);
            }
            helper.assertTrue(baas.geprikt == 4, "the 4 balloons popped (" + baas.geprikt + ")");
        });
        helper.runAfterDelay(start, () -> {   // (the pops stunned him for a second: that is over now)
            baas.krijgSchade(baas.vads() - baas.maxVads() * 0.3, opDek);
            helper.assertTrue(baas.fase() == 3 && !baas.opWrak(), "fase 3 at 33 %: the deck sinks, but not yet (" + baas.fase() + ")");
            helper.assertTrue(Roquefort.ZINK_TICKS == 200 && baas.zinkRest() == 200, "the sinking telegraph lasts 200 ticks (" + baas.zinkRest() + ")");
            helper.assertTrue(baas.zDoetMee(achter) && baas.inGevecht(achter), "from the sinking on the whole wreck is part of the fight");
        });
        helper.runAfterDelay(start + 190, () -> {
            helper.assertTrue(!baas.opWrak() && baas.zinkRest() > 0 && baas.zinkRest() <= 15, "9.5 s later the deck still holds (" + baas.zinkRest() + " ticks left)");
            helper.assertTrue(baas.zinkHits.isEmpty(), "no sinking hit before the telegraph ends: " + baas.zinkHits);
        });
        helper.runAfterDelay(start + 212, () -> {
            helper.assertTrue(baas.opWrak() && baas.zinkRest() < 0, "after 10 s the deck sank");
            helper.assertTrue(baas.zinkHits.isEmpty(), "the first sinking hit comes a second after the deck sank, not at once: " + baas.zinkHits);
        });
        helper.runAfterDelay(start + 290, () -> {
            List<Double> l = baas.zinkHits.getOrDefault(opDek.getUUID(), List.of());
            helper.assertTrue(l.size() >= 3 && l.size() <= 5, "whoever stays on the deck is hit every second (" + l + ")");
            helper.assertTrue(l.get(0) == 0.15 && l.get(1) == 0.15 && l.get(2) == 0.3, "the first two sinking hits are halved, the third is full: " + l);
            helper.assertTrue(l.size() < 4 || l.get(3) == 0.3, "and it stays full: " + l);
            helper.assertTrue(!baas.zinkHits.containsKey(opWrak.getUUID()) && !baas.zinkHits.containsKey(achter.getUUID()),
                    "nobody inside the pink circle on the wreck is hit: " + baas.zinkHits.keySet());
            helper.assertTrue(baas.zDoetMee(achter), "the far half of the wreck is still in the fight");
            opruimen.run();
            helper.succeed();
        });
    }
}
