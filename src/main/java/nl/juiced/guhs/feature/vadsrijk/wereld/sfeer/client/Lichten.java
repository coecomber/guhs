package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.SfeerKlok;
import org.joml.Vector3f;

/**
 * Idea 5 "Lichtgolf van de skyline" (+ the glowing mushrooms of the spore storm). The lights of the map are blocks that always
 * shine, so the wave is a client-side glow layer on top of them (the brief's allowed fallback; no block toggling, no generator
 * export): the client finds the light blocks of Zwamzoet and the Sporenwoud in the loaded chunks (a few chunks per tick, palette
 * check first, one light per 3x3x3 cell), gives each a delay by its distance from the plein and its height, and at sunset they
 * switch on in a wave (a flash, a sparkle and a soft halo all night) from the plein up the caps to the beacon on the ordetempel,
 * with a horn signal at the start and when the beacon lights. Out at dawn.
 */
final class Lichten {
    private record Licht(float x, float y, float z, int vertraging, boolean stad, float fase) {
    }

    private static final Map<Long, List<Licht>> PER_CHUNK = new HashMap<>();
    private static final Set<Long> GESCAND = new HashSet<>();
    private static final List<Licht> ZICHTBAAR = new ArrayList<>();
    private static final RandomSource R = RandomSource.create();
    /** The plein (wave start) and the ordetempel beacon (the end of the wave). */
    private static final BlockPos PLEIN = WereldPlekken.ALLE.getOrDefault("plein", new BlockPos(0, 64, 59));
    private static final BlockPos TEMPEL = WereldPlekken.ALLE.getOrDefault("hoed_tempel", new BlockPos(0, 174, -42));
    static final int BAKEN_VERTRAGING = SfeerKlok.GOLF_DUUR - 20;
    private static int bakenY = -1;
    private static long vorigeFase = -1;
    private static ClientLevel vorigLevel;

    private Lichten() {
    }

    static void wis() {
        PER_CHUNK.clear();
        GESCAND.clear();
        ZICHTBAAR.clear();
        bakenY = -1;
        vorigeFase = -1;
    }

    private static boolean interessant(int cx, int cz) {
        int x = (cx << 4) + 8, z = (cz << 4) + 8;
        Gebieden.Gebied stad = Gebieden.van("zwamzoet"), woud = Gebieden.van("sporenwoud");
        return stad != null && GebiedMist.gewicht(stad, x, z) > 0 || woud != null && GebiedMist.gewicht(woud, x, z) > 0;
    }

    private static boolean lamp(BlockState s) {
        return s.getLightEmission() >= 10 && s.getFluidState().isEmpty() && !s.is(Blocks.LIGHT) && !s.is(Blocks.FIRE);
    }

    /** Client tick: scan a few chunks, refresh the visible list, sparkles + horn of the wave. */
    static void tick(Minecraft mc) {
        ClientLevel l = mc.level;
        if (l == null || mc.player == null) {
            return;
        }
        if (l != vorigLevel) {
            wis();
            vorigLevel = l;
        }
        int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
        int straal = Math.min(12, mc.options.getEffectiveRenderDistance());
        int budget = 6;
        outer:
        for (int r = 0; r <= straal; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int cx = pcx + dx, cz = pcz + dz;
                    long k = ChunkPos.pack(cx, cz);
                    if (GESCAND.contains(k) || !l.hasChunk(cx, cz)) {
                        continue;
                    }
                    GESCAND.add(k);
                    if (interessant(cx, cz)) {
                        scan(l, l.getChunk(cx, cz));
                    }
                    if (--budget <= 0) {
                        break outer;
                    }
                }
            }
        }
        if (l.getGameTime() % 100 == 0) {
            // forget far chunks (they may be unloaded and come back)
            Iterator<Long> it = GESCAND.iterator();
            while (it.hasNext()) {
                long k = it.next();
                if (Math.max(Math.abs(ChunkPos.getX(k) - pcx), Math.abs(ChunkPos.getZ(k) - pcz)) > straal + 2) {
                    it.remove();
                    PER_CHUNK.remove(k);
                }
            }
        }
        if (l.getGameTime() % 20 == 0) {
            verzamel(mc);
        }
        if (bakenY < 0 && l.hasChunk(TEMPEL.getX() >> 4, TEMPEL.getZ() >> 4)) {
            bakenY = l.getHeight(Heightmap.Types.MOTION_BLOCKING, TEMPEL.getX(), TEMPEL.getZ()) + 2;
        }
        golf(mc, l);
    }

    private static void scan(ClientLevel l, LevelChunk c) {
        LevelChunkSection[] secties = c.getSections();
        List<Licht> lijst = new ArrayList<>();
        Set<Long> cellen = new HashSet<>();
        int bx = c.getPos().getMinBlockX(), bz = c.getPos().getMinBlockZ();
        for (int i = 0; i < secties.length; i++) {
            LevelChunkSection s = secties[i];
            if (s == null || s.hasOnlyAir() || !s.maybeHas(Lichten::lamp)) {
                continue;
            }
            int by = SectionPos.sectionToBlockCoord(c.getSectionYFromSectionIndex(i));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!lamp(s.getBlockState(x, y, z))) {
                            continue;
                        }
                        int wx = bx + x, wy = by + y, wz = bz + z;
                        if (!cellen.add(BlockPos.asLong(Math.floorDiv(wx, 3), Math.floorDiv(wy, 3), Math.floorDiv(wz, 3)))) {
                            continue;
                        }
                        boolean stad = Gebieden.van("zwamzoet") != null && Gebieden.van("zwamzoet").bevat(wx, wz);
                        double hd = Math.sqrt((wx - PLEIN.getX()) * (double) (wx - PLEIN.getX()) + (wz - PLEIN.getZ()) * (double) (wz - PLEIN.getZ()));
                        int v = (int) Mth.clamp((hd * 0.9 + Math.max(0, wy - 64) * 1.5) * 0.85, 0, BAKEN_VERTRAGING - 30);
                        lijst.add(new Licht(wx + 0.5f, wy + 0.5f, wz + 0.5f, v, stad, R.nextFloat() * 6.28f));
                    }
                }
            }
        }
        if (!lijst.isEmpty()) {
            PER_CHUNK.put(c.getPos().pack(), lijst);
        }
    }

    /** The nearest lights (within 200 blocks), at most 900 (ZACHT: 350). */
    private static void verzamel(Minecraft mc) {
        ZICHTBAAR.clear();
        Vec3 p = mc.player.position();
        List<Licht> alle = new ArrayList<>();
        for (List<Licht> l : PER_CHUNK.values()) {
            for (Licht x : l) {
                double dx = x.x() - p.x, dy = x.y() - p.y, dz = x.z() - p.z;
                if (dx * dx + dy * dy + dz * dz < 200 * 200) {
                    alle.add(x);
                }
            }
        }
        int max = SfeerConfig.niveau() == SfeerConfig.Niveau.VOL ? 900 : 350;
        if (alle.size() > max) {
            alle.sort((a, b) -> Double.compare(afst2(a, p), afst2(b, p)));
            alle = alle.subList(0, max);
        }
        ZICHTBAAR.addAll(alle);
    }

    private static double afst2(Licht a, Vec3 p) {
        double dx = a.x() - p.x, dy = a.y() - p.y, dz = a.z() - p.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Sparkles where a light switches on now, and the horn at the start and at the beacon. */
    private static void golf(Minecraft mc, ClientLevel l) {
        long fase = Momenten.golfFase(l);
        long vorig = vorigeFase;
        vorigeFase = fase;
        if (fase < 0 || vorig < 0 && fase > 40 || vorig > fase) {
            return;     // off, or the game started/jumped in the middle of the night: no sparkles
        }
        Vec3 p = mc.player.position();
        double totStad = Math.sqrt(p.distanceToSqr(PLEIN.getX(), p.y, PLEIN.getZ()));
        if (totStad > 320) {
            return;
        }
        if (vorig < 0) {
            hoorn(mc, l, PLEIN, 0.75f);
        }
        if (vorig < BAKEN_VERTRAGING && fase >= BAKEN_VERTRAGING) {
            hoorn(mc, l, TEMPEL, 0.95f);
            if (p.distanceToSqr(TEMPEL.getX(), p.y, TEMPEL.getZ()) < 200 * 200) {
                for (int i = 0; i < 40; i++) {
                    l.addAlwaysVisibleParticle(ParticleTypes.FIREWORK, TEMPEL.getX() + 0.5, baken() + 1, TEMPEL.getZ() + 0.5,
                            R.nextGaussian() * 0.3, 0.2 + R.nextDouble() * 0.4, R.nextGaussian() * 0.3);
                }
            }
        }
        int n = 0, max = (int) (24 * SfeerConfig.sterkte());
        for (Licht x : ZICHTBAAR) {
            if (x.stad() && x.vertraging() > vorig && x.vertraging() <= fase && n < max) {
                if (afst2(x, p) < 90 * 90) {
                    n++;
                    for (int i = 0; i < 3; i++) {
                        l.addParticle(ParticleTypes.END_ROD, x.x(), x.y() + 0.6, x.z(), R.nextGaussian() * 0.06, 0.05 + R.nextDouble() * 0.06,
                                R.nextGaussian() * 0.06);
                    }
                }
            }
        }
    }

    /** The horn: a goat-horn call from the direction of {@code bron}, ~20 blocks away from the player (ambient, not music). */
    private static void hoorn(Minecraft mc, ClientLevel l, BlockPos bron, float pitch) {
        Vec3 p = mc.player.position();
        Vec3 r = new Vec3(bron.getX() - p.x, 0, bron.getZ() - p.z);
        r = r.lengthSqr() < 1 ? Vec3.ZERO : r.normalize().scale(Math.min(20, r.length()));
        l.playLocalSound(p.x + r.x, p.y + 8, p.z + r.z, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(5).value(), SoundSource.AMBIENT, 0.9f, pitch, false);
    }

    static int baken() {
        return bakenY > 0 ? bakenY : 214;
    }

    /** World pass: the halos (city wave, storm mushrooms) and the beacon. Coordinates relative to the camera. */
    static void teken(BufferBuilder b, Vec3 cam, Vector3f rechts, Vector3f op, float tijd, ClientLevel l) {
        long fase = Momenten.golfFase(l);
        float licht = Momenten.golfLicht(l);
        float storm = Momenten.storm(l);
        float pt = tijd - (long) tijd;
        float f = fase < 0 ? -1 : fase + pt;
        boolean nacht = SfeerKlok.nacht(l) > 0.2f || fase >= 0;
        for (Licht x : ZICHTBAAR) {
            float rx = (float) (x.x() - cam.x), ry = (float) (x.y() - cam.y), rz = (float) (x.z() - cam.z);
            if (x.stad()) {
                if (f < x.vertraging() || licht <= 0) {
                    continue;
                }
                float in = f - x.vertraging();
                float a = Math.min(1f, in / 8f) * licht;
                float flits = 1f + 1.6f * (float) Math.exp(-in / 6f);
                float flikker = 0.9f + 0.1f * Mth.sin(tijd * 0.15f + x.fase());
                Teken.halo(b, rx, ry, rz, 1.7f * flits, Teken.kleur(0.42f * a * flikker * (nacht ? 1f : 0.6f), 1f, 0.78f, 0.46f), rechts, op);
            } else if (storm > 0.01f) {
                float puls = 0.75f + 0.25f * Mth.sin(tijd * 0.2f + x.fase());
                Teken.halo(b, rx, ry, rz, 2.6f * puls, Teken.kleur(0.5f * storm * puls, 1f, 0.5f, 0.85f), rechts, op);
            }
        }
        // the beacon on the ordetempel
        if (f >= BAKEN_VERTRAGING && licht > 0) {
            float in = f - BAKEN_VERTRAGING;
            float a = Math.min(1f, in / 10f) * licht;
            float bx = TEMPEL.getX() + 0.5f - (float) cam.x, by = baken() - (float) cam.y, bz = TEMPEL.getZ() + 0.5f - (float) cam.z;
            float flits = 1f + 2.5f * (float) Math.exp(-in / 10f);
            Teken.halo(b, bx, by, bz, 7f * flits, Teken.kleur(0.7f * a, 1f, 0.7f, 0.8f), rechts, op);
            straal(b, bx, by, bz, 1.1f, 90f, Teken.kleur(0.45f * a, 1f, 0.72f, 0.82f));
        }
    }

    /** A vertical beam of light (two crossed planes, fading upwards). */
    static void straal(BufferBuilder b, float x, float y, float z, float w, float h, int kleur) {
        int weg = net.minecraft.util.ARGB.color(0, net.minecraft.util.ARGB.red(kleur), net.minecraft.util.ARGB.green(kleur), net.minecraft.util.ARGB.blue(kleur));
        float[][] as = {{w, 0}, {0, w}};
        for (float[] a : as) {
            Teken.quad(b, new Vector3f(x - a[0], y, z - a[1]), new Vector3f(x + a[0], y, z + a[1]), new Vector3f(x + a[0], y + h, z + a[1]),
                    new Vector3f(x - a[0], y + h, z - a[1]), kleur, kleur, weg, weg);
        }
    }
}
