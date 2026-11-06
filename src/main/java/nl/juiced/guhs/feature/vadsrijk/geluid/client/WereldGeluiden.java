package nl.juiced.guhs.feature.vadsrijk.geluid.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.geluid.WereldBron;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * U-AUDIO2 (V4: world point sounds, SUBTLE; the lesson of the too-present harbour wind): soft little sounds of the places right
 * around you in Het Vadsrijk. Client only.
 * <ul>
 *   <li><b>Finding them</b>: a slow block scan of the box around the player (25 x 13 x 25, ~700 blocks a tick, the whole box in
 *       about a second; rescanned every few seconds) with {@link WereldBron#soortOp}, the fixed gondola/lift points, and every
 *       2 s a look for three or more NPC guhs close together (the market murmur).</li>
 *   <li><b>The budget</b>: only the {@link #MAX_BRONNEN} nearest sources within {@link #BEREIK} blocks count (one per kind
 *       per 6 blocks); at most one one-shot every {@link #MIN_PAUZE} ticks over everything; every kind has its own long
 *       interval; one soft fountain loop at most. Quiet and short range: the volume falls to nothing at ~12 blocks.</li>
 *   <li><b>Caves</b>: underground (well below the surface, no sky light) your footsteps get a short soft echo.</li>
 * </ul>
 * Slider: "wereldgeluiden" (guhs-vadsrijk-geluid.toml) on top of Minecraft's "Omgeving".
 */
public final class WereldGeluiden {
    static final int STRAAL = 12, HOOGTE = 6, PER_TICK = 700, MAX_BRONNEN = 4, MIN_PAUZE = 40;
    static final double BEREIK = 12;
    private static final RandomSource R = RandomSource.create();
    /** Volume per kind (before the slider) and the pause after it, in ticks: min + random. */
    private static final Map<String, float[]> SOORT = Map.of(
            "bijen", new float[] {0.35f, 120, 140}, "smidse", new float[] {0.32f, 160, 200}, "windgong", new float[] {0.28f, 240, 300},
            "kampvuur", new float[] {0.3f, 120, 160}, "kraak", new float[] {0.3f, 220, 260}, "markt", new float[] {0.3f, 90, 120},
            "fontein", new float[] {0.32f, 0, 0});

    private static final Map<BlockPos, String> GEVONDEN = new HashMap<>();
    private static final Map<String, Integer> VOLGENDE = new HashMap<>();
    @Nullable
    private static ClientLevel vorigLevel;
    @Nullable
    private static BlockPos scanMidden;
    private static int scanIndex, klok, volgendeLos, scanKlaarOp;
    @Nullable
    private static Vec3 markt;
    @Nullable
    private static Fontein fontein;
    private static double gelopen;
    @Nullable
    private static Vec3 vorigePlek;
    private static final List<Object[]> ECHO = new ArrayList<>();   // {tick, BlockState, x, y, z, vol}

    private WereldGeluiden() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        if (l != vorigLevel) {
            vorigLevel = l;
            wis();
        }
        if (l == null || p == null || mc.isPaused()) {
            return;
        }
        klok++;
        float vol = GeluidConfig.wereld();
        if (!Vadsrijk.in(l) || vol <= 0) {
            if (fontein != null) {
                fontein.doel = 0;
            }
            return;
        }
        scan(l, p);
        if (klok % 40 == 0) {
            markt = zoekMarkt(l, p);
        }
        echoTick(l, p, vol);
        // the nearest sources within range, one per kind per 6 blocks, at most MAX_BRONNEN
        List<Object[]> bronnen = bronnen(p);
        Vec3 fonteinPlek = null;
        for (Object[] b : bronnen) {
            if (b[0].equals("fontein")) {
                fonteinPlek = (Vec3) b[1];
                break;
            }
        }
        fonteinTick(mc, p, fonteinPlek, vol);
        if (klok < volgendeLos) {
            return;
        }
        for (Object[] b : bronnen) {
            String soort = (String) b[0];
            if (soort.equals("fontein") || klok < VOLGENDE.getOrDefault(soort, 0)) {
                continue;
            }
            speelLos(l, soort, (Vec3) b[1], vol);
            float[] s = SOORT.get(soort);
            VOLGENDE.put(soort, klok + (int) s[1] + R.nextInt((int) s[2] + 1));
            volgendeLos = klok + MIN_PAUZE + R.nextInt(40);
            break;
        }
    }

    // ---- finding -----------------------------------------------------------------------------------------------------------

    private static void scan(ClientLevel l, LocalPlayer p) {
        BlockPos hier = p.blockPosition();
        int b = STRAAL * 2 + 1, h = HOOGTE * 2 + 1, totaal = b * b * h;
        if (scanIndex == 0) {
            if (scanMidden != null && klok < scanKlaarOp && scanMidden.distManhattan(hier) <= 8) {
                return;   // (a full scan is done and you did not walk far: rest a few seconds)
            }
            scanMidden = hier;
            GEVONDEN.entrySet().removeIf(e -> e.getKey().distSqr(hier) > (STRAAL + 6) * (STRAAL + 6));
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int n = 0; n < PER_TICK && scanIndex < totaal; n++, scanIndex++) {
            int i = scanIndex;
            int dx = i % b - STRAAL, dz = (i / b) % b - STRAAL, dy = i / (b * b) - HOOGTE;
            m.set(scanMidden.getX() + dx, scanMidden.getY() + dy, scanMidden.getZ() + dz);
            if (!l.isLoaded(m)) {
                continue;
            }
            String s = WereldBron.soortOp(l, m);
            if (s != null) {
                GEVONDEN.put(m.immutable(), s);
            } else {
                GEVONDEN.remove(m);
            }
        }
        if (scanIndex >= totaal) {
            scanIndex = 0;
            scanKlaarOp = klok + 100;
        }
    }

    @Nullable
    private static Vec3 zoekMarkt(ClientLevel l, LocalPlayer p) {
        List<Entity> npcs = l.getEntities(p, new AABB(p.blockPosition()).inflate(BEREIK + 4), StemSpeler::npc);
        Vec3 best = null;
        double bestD = 1e9;
        for (Entity e : npcs) {
            int n = 0;
            double sx = 0, sy = 0, sz = 0;
            for (Entity o : npcs) {
                if (o.distanceToSqr(e) <= 36) {
                    n++;
                    sx += o.getX();
                    sy += o.getY();
                    sz += o.getZ();
                }
            }
            if (n >= 3) {
                Vec3 c = new Vec3(sx / n, sy / n + 1, sz / n);
                double d = c.distanceToSqr(p.position());
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }
        }
        return best;
    }

    /** [kind, position, distance²] of the sources that count now (nearest first, budgeted). */
    private static List<Object[]> bronnen(LocalPlayer p) {
        List<Object[]> alle = new ArrayList<>();
        Vec3 o = p.position();
        GEVONDEN.forEach((pos, s) -> alle.add(new Object[] {s, Vec3.atCenterOf(pos), Vec3.atCenterOf(pos).distanceToSqr(o)}));
        for (WereldBron.Punt pt : WereldBron.vastePunten()) {
            Vec3 c = Vec3.atCenterOf(pt.pos());
            alle.add(new Object[] {pt.soort(), c, c.distanceToSqr(o)});
        }
        if (markt != null) {
            alle.add(new Object[] {"markt", markt, markt.distanceToSqr(o)});
        }
        alle.removeIf(b -> (double) b[2] > BEREIK * BEREIK);
        alle.sort((a, b) -> Double.compare((double) a[2], (double) b[2]));
        List<Object[]> uit = new ArrayList<>();
        for (Object[] b : alle) {
            boolean dubbel = false;
            for (Object[] u : uit) {
                if (u[0].equals(b[0]) && ((Vec3) u[1]).distanceToSqr((Vec3) b[1]) < 36) {
                    dubbel = true;
                    break;
                }
            }
            if (!dubbel) {
                uit.add(b);
                if (uit.size() >= MAX_BRONNEN) {
                    break;
                }
            }
        }
        return uit;
    }

    // ---- playing -----------------------------------------------------------------------------------------------------------

    private static void speelLos(ClientLevel l, String soort, Vec3 c, float vol) {
        float v = SOORT.get(soort)[0] * vol;
        Minecraft mc = Minecraft.getInstance();
        if (soort.equals("smidse")) {
            // a few soft taps, not a whole forge
            int n = 2 + R.nextInt(3);
            for (int i = 0; i < n; i++) {
                ECHO.add(new Object[] {klok + i * 9, null, c.x, c.y, c.z, v * (i == n - 1 ? 1f : 0.8f), "smidse"});
            }
            return;
        }
        mc.getSoundManager().play(new SimpleSoundInstance(GeluidSpeler.event(WereldBron.id(soort)), SoundSource.AMBIENT, v,
                0.92f + R.nextFloat() * 0.16f, R, c.x, c.y, c.z));
    }

    private static void fonteinTick(Minecraft mc, LocalPlayer p, @Nullable Vec3 plek, float vol) {
        if (fontein != null && fontein.isStopped()) {
            fontein = null;
        }
        if (plek == null) {
            if (fontein != null) {
                fontein.doel = 0;
            }
            return;
        }
        double d = Math.sqrt(plek.distanceToSqr(p.getEyePosition()));
        float doel = (float) (SOORT.get("fontein")[0] * vol * Math.pow(Math.max(0, 1 - d / BEREIK), 1.5));
        if (fontein == null) {
            if (doel < 0.01f) {
                return;
            }
            fontein = new Fontein(plek);
            mc.getSoundManager().play(fontein);
        }
        fontein.naar(plek, doel);
    }

    /** Cave echo + the delayed smithy taps. */
    private static void echoTick(ClientLevel l, LocalPlayer p, float vol) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = ECHO.size() - 1; i >= 0; i--) {
            Object[] e = ECHO.get(i);
            if (klok < (int) e[0]) {
                continue;
            }
            ECHO.remove(i);
            if ("smidse".equals(e.length > 6 ? e[6] : null)) {
                mc.getSoundManager().play(new SimpleSoundInstance(GeluidSpeler.event(WereldBron.id("smidse")), SoundSource.AMBIENT, (float) e[5],
                        0.95f + R.nextFloat() * 0.1f, R, (double) e[2], (double) e[3], (double) e[4]));
            } else if (e[1] instanceof BlockState bs) {
                SoundType st = bs.getSoundType();
                mc.getSoundManager().play(new SimpleSoundInstance(st.getStepSound(), SoundSource.AMBIENT, (float) e[5], st.getPitch() * 0.82f, R,
                        (double) e[2], (double) e[3], (double) e[4]));
            }
        }
        Vec3 nu = p.position();
        if (vorigePlek == null || nu.distanceToSqr(vorigePlek) > 16) {
            vorigePlek = nu;
            return;
        }
        double dx = nu.x - vorigePlek.x, dz = nu.z - vorigePlek.z;
        vorigePlek = nu;
        if (!p.onGround() || p.isShiftKeyDown() || p.isInWater() || !onder(l, p)) {
            gelopen = 0;
            return;
        }
        gelopen += Math.sqrt(dx * dx + dz * dz);
        if (gelopen >= 1.7) {
            gelopen = 0;
            BlockState onder = l.getBlockState(p.getOnPos());
            if (!onder.isAir()) {
                ECHO.add(new Object[] {klok + 4, onder, p.getX(), p.getY(), p.getZ(), 0.07f * vol});
                ECHO.add(new Object[] {klok + 9, onder, p.getX(), p.getY(), p.getZ(), 0.035f * vol});
            }
        }
    }

    private static boolean onder(ClientLevel l, LocalPlayer p) {
        BlockPos b = p.blockPosition().above();
        int maaiveld = l.getHeight(Heightmap.Types.WORLD_SURFACE, b.getX(), b.getZ());
        return b.getY() < maaiveld - 6 && l.getBrightness(LightLayer.SKY, b) < 4;
    }

    // ---- dev ---------------------------------------------------------------------------------------------------------------

    /** Dev: one sound of this kind 4 blocks in front of you (fontein: 6 s of the loop). */
    static void voorbeeld(String soort) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            return;
        }
        Vec3 c = p.getEyePosition().add(p.getLookAngle().scale(4));
        float vol = Math.max(0.4f, GeluidConfig.wereld());
        if (soort.equals("fontein")) {
            Fontein f = new Fontein(c);
            f.naar(c, SOORT.get("fontein")[0] * vol);
            f.leven = 120;
            mc.getSoundManager().play(f);
            return;
        }
        speelLos(mc.level, soort, c, vol);
    }

    /** Dev: what is found around you. */
    static void info() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            return;
        }
        Map<String, Integer> tel = new HashMap<>();
        GEVONDEN.values().forEach(s -> tel.merge(s, 1, Integer::sum));
        StringBuilder sb = new StringBuilder();
        for (Object[] b : bronnen(p)) {
            Vec3 c = (Vec3) b[1];
            sb.append(b[0]).append(" @ ").append((int) c.x).append(",").append((int) c.y).append(",").append((int) c.z)
                    .append(" (").append((int) Math.sqrt((double) b[2])).append(" m)  ");
        }
        p.sendSystemMessage(Component.literal("Wereldgeluiden: gevonden " + tel + (markt != null ? " + markt" : "") + "  |  schuif "
                + Math.round(GeluidConfig.wereld() * 100) + "%  |  onder de grond: " + (mc.level != null && onder(mc.level, p))).withStyle(ChatFormatting.AQUA));
        p.sendSystemMessage(Component.literal("Nu hoorbaar (max " + MAX_BRONNEN + "): " + (sb.length() == 0 ? "-" : sb)).withStyle(ChatFormatting.AQUA));
    }

    private static void wis() {
        GEVONDEN.clear();
        VOLGENDE.clear();
        ECHO.clear();
        scanMidden = null;
        scanIndex = 0;
        markt = null;
        if (fontein != null) {
            Minecraft.getInstance().getSoundManager().stop(fontein);
            fontein = null;
        }
    }

    /** One soft fountain loop at the nearest small fall; it glides there and its volume follows the distance. */
    private static final class Fontein extends AbstractTickableSoundInstance {
        float doel;
        int leven = -1;
        private Vec3 naar;
        private int stil;

        Fontein(Vec3 c) {
            super(GeluidSpeler.event(WereldBron.id("fontein")), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.005f;
            this.attenuation = SoundInstance.Attenuation.NONE;   // (the distance is in the volume)
            this.x = c.x;
            this.y = c.y;
            this.z = c.z;
            this.naar = c;
        }

        void naar(Vec3 c, float v) {
            naar = c;
            doel = v;
            stil = 0;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (leven >= 0 && --leven <= 0) {
                doel = 0;
            } else if (leven < 0 && ++stil > 5) {
                doel = 0;
            }
            volume = Mth.lerp(0.06f, volume, doel);
            x = Mth.lerp(0.1, x, naar.x);
            y = Mth.lerp(0.1, y, naar.y);
            z = Mth.lerp(0.1, z, naar.z);
            if (doel == 0 && volume < 0.004f) {
                stop();
            }
        }
    }
}
