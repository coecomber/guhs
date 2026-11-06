package nl.juiced.guhs.feature.vadsrijk.dubbel;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.ArenaData;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-DUBBEL: one boss per arena however often its chunks load and unload, no ghosts of unsaved entities, one per unique key.
 * Run: {@code runGameTestServer -Pgt=DubbelGameTests}.
 */
public class DubbelGameTests {
    private static final String BATCH = "vadsrijk_dubbel";

    // ---- the key rules (unit-like) ------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_sleutel", timeoutTicks = 60)
    public static void eenPerSleutel(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 2, 1));
        String k = "test:dubbel_" + c.asLong();
        RijkGuh a = guh(sl, c, k);
        helper.assertTrue(sl.addFreshEntity(a), "the first one is placed");
        helper.assertTrue(Dubbel.zoek(sl, k) == a, "it is the one of its key");
        // the chunk "comes back" with an old saved copy (loadedFromDisk): refused, the live one stays
        RijkGuh oud = guh(sl, c, k);
        sl.addLegacyChunkEntities(Stream.of(oud));
        helper.assertTrue(sl.getEntity(oud.getUUID()) == null && !a.isRemoved(), "a disk copy of a live key is refused");
        // the manager spawns afresh (it found none): the new one replaces the leftover
        RijkGuh nieuw = guh(sl, c.east(), k);
        helper.assertTrue(sl.addFreshEntity(nieuw), "the fresh one is placed");
        helper.assertTrue(a.isRemoved() && Dubbel.zoek(sl, k) == nieuw, "the fresh one replaced the old one");
        // a legacy boss (saved before the tag) of a registered arena gets its key from the arena id
        Arena ar = new Arena("dubbel:sleutel", c, 4, c, 1, c, "vadsrijk.kern.baas.klodderreus");
        Arenas.registreer(ar, x -> null);
        try {
            Klodderreus b = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.COMMAND);
            b.zetArena(ar);
            helper.assertTrue(("arena:dubbel:sleutel").equals(Dubbel.sleutel(b)), "legacy key from the arena id: " + Dubbel.sleutel(b));
        } finally {
            Arenas.afmelden(ar.id());
        }
        // the scan sees two copies of one key (made by hand, bypassing the join check) and opruimen leaves one
        RijkGuh x1 = guh(sl, c.west(), "test:scan_" + c.asLong()), x2 = guh(sl, c.west(), "test:scan_" + c.asLong());
        sl.addFreshEntity(x1);
        x2.addTag("tijdelijk");
        x2.entityTags().removeIf(t -> t.startsWith(Dubbel.PREFIX));
        sl.addFreshEntity(x2);
        x2.addTag(Dubbel.PREFIX + "test:scan_" + c.asLong());
        helper.assertTrue(Dubbel.scan(sl).stream().anyMatch(g -> g.wat().equals("test:scan_" + c.asLong()) && g.kopieen().size() == 2), "the scan finds the pair");
        Dubbel.opruimen(sl, false);
        helper.assertTrue(x1.isRemoved() != x2.isRemoved(), "opruimen leaves exactly one");
        nieuw.discard();
        (x1.isRemoved() ? x2 : x1).discard();
        helper.succeed();
    }

    private static RijkGuh guh(ServerLevel sl, BlockPos c, String sleutel) {
        RijkGuh g = NpcRegistry.GUH.get().create(sl, EntitySpawnReason.COMMAND);
        g.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        g.setNoGravity(true);
        g.setNoAi(true);
        return Dubbel.merk(g, sleutel);
    }

    // ---- the playtest bug: load / unload an arena again and again -----------------------------------------------------------

    private static final String ARENA = "dubbel:test";
    private static final int RONDES = 3;

    /** State of the reload test (one run at a time). */
    private static final class Rit {
        int ronde, fase, wacht;
        RijkGuh geest;
        boolean schijfkopieGeprobeerd;
    }

    @GuhTest(template = "empty", batch = BATCH + "_arena", timeoutTicks = 4000)
    public static void arenaHerladenEenBaas(GameTestHelper helper) {
        ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
        if (vr == null) {
            helper.fail("no Vadsrijk dimension on this test server");
            return;
        }
        BlockPos c = new BlockPos(24008, 200, 24008);
        ChunkPos cp = ChunkPos.containing(c);
        Arena a = new Arena(ARENA, c, 6, c, 1, c.above(4), "vadsrijk.kern.baas.klodderreus");
        Arenas.registreer(a, sl -> {
            Klodderreus k = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.SPAWNER);
            if (k != null) {
                k.stelIn(3, Element.WATER, false, Set.of());
                k.setNoAi(true);
                k.setNoGravity(true);
            }
            return k;
        });
        ArenaData.get(vr.getServer()).zet(ARENA, -1);
        Rit r = new Rit();
        helper.onEachTick(() -> {
            r.wacht++;
            switch (r.fase) {
                case 0 -> {   // load the arena chunks (3x3)
                    forceer(vr, cp, true);
                    r.fase = 1;
                    r.wacht = 0;
                }
                case 1 -> {   // wait for the boss (kern ticks the arena every 5 ticks once its entities are there)
                    if (r.wacht == 1) {
                        for (int x = -4; x <= 4; x++) {
                            for (int z = -4; z <= 4; z++) {
                                vr.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                            }
                        }
                    }
                    if (Arenas.baas(vr, ARENA) == null) {
                        if (r.wacht > 400) {
                            throw helper.assertionException(net.minecraft.network.chat.Component.literal("round " + r.ronde + ": no boss after 400 ticks"));
                        }
                        return;
                    }
                    if (r.wacht < 30) {
                        return;   // (let any old saved copy arrive: it must be refused)
                    }
                    List<VadsrijkBaas> l = bazen(vr, c);
                    helper.assertTrue(l.size() == 1, "round " + r.ronde + ": exactly one boss after loading (" + l.size() + ")");
                    if (r.ronde == 0) {
                        // an unsaved Vadsrijk entity in the arena: it must not survive the unload as a hidden ghost
                        r.geest = NpcRegistry.GUH.get().create(vr, EntitySpawnReason.COMMAND);
                        r.geest.snapTo(c.getX() + 3.5, c.getY(), c.getZ() + 3.5, 0, 0);
                        r.geest.setNoGravity(true);
                        r.geest.setNoAi(true);
                        vr.addFreshEntity(r.geest);
                    } else if (r.geest != null) {
                        helper.assertTrue(r.geest.isRemoved() && vr.getEntity(r.geest.getUUID()) == null, "the unsaved guh did not come back as a ghost");
                    }
                    if (!r.schijfkopieGeprobeerd) {
                        // an old untagged copy as it would come from disk (saved before U-DUBBEL): refused
                        r.schijfkopieGeprobeerd = true;
                        Klodderreus kopie = KernRegistry.KLODDERREUS.get().create(vr, EntitySpawnReason.SPAWNER);
                        kopie.zetArena(a);
                        kopie.setNoAi(true);
                        kopie.setNoGravity(true);
                        kopie.snapTo(c.getX() + 1.5, c.getY(), c.getZ() + 0.5, 0, 0);
                        vr.addLegacyChunkEntities(Stream.of(kopie));
                        helper.assertTrue(vr.getEntity(kopie.getUUID()) == null, "the old disk copy is refused");
                        helper.assertTrue(bazen(vr, c).size() == 1, "still one boss after the disk copy");
                    }
                    if (r.ronde >= RONDES) {
                        Arenas.reset(vr.getServer(), ARENA);
                        bazen(vr, c).forEach(Entity::discard);
                        Arenas.afmelden(ARENA);
                        forceer(vr, cp, false);
                        helper.succeed();
                        r.fase = 9;
                        return;
                    }
                    forceer(vr, cp, false);   // unload: the boss is saved with its chunk
                    r.fase = 2;
                    r.wacht = 0;
                }
                case 2 -> {   // wait until the chunk and its entities are really gone
                    if (vr.isLoaded(c) || vr.areEntitiesLoaded(cp.pack())) {
                        if (r.wacht > 900) {
                            throw helper.assertionException(net.minecraft.network.chat.Component.literal("round " + r.ronde + ": the arena chunk did not unload"));
                        }
                        return;
                    }
                    r.ronde++;
                    r.fase = 0;
                    r.wacht = 0;
                }
                default -> {
                }
            }
        });
    }

    private static List<VadsrijkBaas> bazen(ServerLevel vr, BlockPos c) {
        return vr.getEntitiesOfClass(VadsrijkBaas.class, new AABB(c).inflate(48, 64, 48), x -> ARENA.equals(x.arenaId) && !x.isRemoved());
    }

    private static void forceer(ServerLevel vr, ChunkPos cp, boolean aan) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                vr.setChunkForced(cp.x() + dx, cp.z() + dz, aan);
            }
        }
    }
}
