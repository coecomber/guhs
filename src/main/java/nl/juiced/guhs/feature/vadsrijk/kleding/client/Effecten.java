package nl.juiced.guhs.feature.vadsrijk.kleding.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kleding.KledingPayloads;

/**
 * T-KLEDING effects (§4.3), all drawn by each client itself from the synced looks (no particle packets): the trail while walking or
 * riding, the aura after standing still {@link #STIL} ticks (gone as soon as you move), and the cheer-up effect on {@code Plof}.
 * <p>FPS budget: at most {@link #BUDGET} particles per tick for everybody together (MINDER: a third), nothing further than
 * {@link #AFSTAND} blocks; other players' effects follow "Effecten van anderen", yours "Eigen effecten" (ALLES / MINDER / UIT). In a
 * boss arena (yours) trails are thinned and auras are off, so fights stay readable.
 */
public final class Effecten {
    public static final int STIL = 60, BUDGET = 48;
    public static final double AFSTAND = 32;
    private static final Map<UUID, long[]> STILSTAAN = new ConcurrentHashMap<>();
    private static int budget;

    private Effecten() {
    }

    private static KledingCache.Niveau niveau(Player p) {
        return p == Minecraft.getInstance().player ? KledingCache.eigen() : KledingCache.anderen();
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lvl = mc.level;
        if (lvl == null || mc.player == null || mc.isPaused()) {
            return;
        }
        budget = BUDGET;
        boolean arena = KledingCache.staat().arena();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        long nu = lvl.getGameTime();
        for (Player p : lvl.players()) {
            if (p.isInvisible() || p.isSpectator() || p.position().distanceToSqr(cam) > AFSTAND * AFSTAND) {
                continue;
            }
            KledingCache.Niveau n = niveau(p);
            if (n == KledingCache.Niveau.UIT) {
                continue;
            }
            int deler = n == KledingCache.Niveau.MINDER || arena ? 3 : 1;
            String spoor = KledingCache.keuze(p.getUUID(), "spoor"), aura = KledingCache.keuze(p.getUUID(), "aura");
            Entity voet = p.getVehicle() != null ? p.getVehicle() : p;
            double dx = voet.getX() - voet.xo, dz = voet.getZ() - voet.zo;
            boolean beweegt = dx * dx + dz * dz > 0.0036;
            long[] stil = STILSTAAN.computeIfAbsent(p.getUUID(), k -> new long[] {nu});
            if (beweegt || Math.abs(p.getY() - p.yo) > 0.01) {
                stil[0] = nu;
            }
            if (!spoor.isEmpty() && beweegt && (voet.onGround() || p.isPassenger()) && (nu + p.getId()) % (2L * deler) == 0) {
                spoor(lvl, spoor, voet.getX(), voet.getY() + 0.1, voet.getZ(), lvl.getRandom());
            }
            if (!aura.isEmpty() && !arena && nu - stil[0] > STIL && (nu + p.getId()) % (3L * deler) == 0) {
                aura(lvl, aura, p, nu);
            }
        }
        if (STILSTAAN.size() > 128) {
            STILSTAAN.clear();
        }
    }

    private static boolean deeltje(ClientLevel l, ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
        if (budget <= 0) {
            return false;
        }
        budget--;
        l.addParticle(o, x, y, z, vx, vy, vz);
        return true;
    }

    private static void spoor(ClientLevel l, String id, double x, double y, double z, RandomSource r) {
        double ox = (r.nextDouble() - 0.5) * 0.5, oz = (r.nextDouble() - 0.5) * 0.5;
        switch (id) {
            case "spoor:bloemblaadjes" -> deeltje(l, ParticleTypes.CHERRY_LEAVES, x + ox, y + 0.2, z + oz, 0, 0, 0);
            case "spoor:sneeuwvlokjes" -> deeltje(l, ParticleTypes.SNOWFLAKE, x + ox, y + 0.15, z + oz, 0, 0.01, 0);
            case "spoor:sterretjes" -> deeltje(l, ParticleTypes.END_ROD, x + ox, y + 0.15, z + oz, 0, 0.015, 0);
            case "spoor:honingdruppels" -> deeltje(l, ParticleTypes.FALLING_HONEY, x + ox, y + 0.6, z + oz, 0, 0, 0);
            case "spoor:muzieknootjes" -> deeltje(l, ParticleTypes.NOTE, x + ox, y + 0.3, z + oz, r.nextInt(24) / 24.0, 0, 0);
            default -> {
            }
        }
    }

    private static void aura(ClientLevel l, String id, Player p, long nu) {
        double a = nu * 0.25 + p.getId();
        double r = 0.75;
        double x = p.getX() + Math.cos(a) * r, z = p.getZ() + Math.sin(a) * r;
        switch (id) {
            case "aura:rozegloed" -> {
                deeltje(l, new DustParticleOptions(0xFF8CC8, 1.1f), x, p.getY() + 0.15, z, 0, 0.02, 0);
                deeltje(l, new DustParticleOptions(0xFFC8E4, 0.8f), p.getX() - Math.cos(a) * r, p.getY() + 0.15, p.getZ() - Math.sin(a) * r, 0, 0.02, 0);
            }
            case "aura:pluisjes" -> deeltje(l, new DustParticleOptions(0xFFFFFF, 0.9f), x, p.getY() + 0.2 + (nu % 40) / 25.0, z, 0, 0.01, 0);
            case "aura:vuurvliegjes" -> {
                RandomSource rs = l.getRandom();
                deeltje(l, ParticleTypes.FIREFLY, p.getX() + (rs.nextDouble() - 0.5) * 2.4, p.getY() + 0.4 + rs.nextDouble() * 1.6,
                        p.getZ() + (rs.nextDouble() - 0.5) * 2.4, 0, 0, 0);
            }
            default -> {
            }
        }
    }

    // ---- cheer-up effects --------------------------------------------------------------------------------------------------------

    private static final int[] PASTEL = {0xFF8CC8, 0xFFD24A, 0x8CDCFF, 0xA8F0C8, 0xD0B0FF, 0xFFFFFF, 0xFFB070};
    private static final int[] REGENBOOG = {0xFF4A4A, 0xFF9A3A, 0xFFE04A, 0x5AD25A, 0x4AA8FF, 0x6A5AE0, 0xC05AE0};

    static void plof(KledingPayloads.Plof pl) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.player == null) {
            return;
        }
        boolean eigen = pl.speler().equals(mc.player.getUUID());
        KledingCache.Niveau n = eigen ? KledingCache.eigen() : KledingCache.anderen();
        if (n == KledingCache.Niveau.UIT || mc.gameRenderer.getMainCamera().position().distanceToSqr(pl.x(), pl.y(), pl.z()) > 48 * 48) {
            return;
        }
        int veel = n == KledingCache.Niveau.MINDER ? 3 : 1;
        budget = Math.max(budget, 60 / veel);
        RandomSource r = l.getRandom();
        double x = pl.x(), y = pl.y(), z = pl.z();
        switch (pl.effect()) {
            case "plof:regenboog" -> {
                for (int i = 0; i < 28 / veel; i++) {
                    double t = i / (28.0 / veel - 1);
                    double ang = Math.PI * t;
                    for (int b = 0; b < REGENBOOG.length; b += veel) {
                        double rr = 1.0 + b * 0.12;
                        deeltje(l, new DustParticleOptions(REGENBOOG[b], 1.0f), x + Math.cos(ang) * rr, y + Math.sin(ang) * rr, z, 0, 0, 0);
                    }
                }
            }
            case "plof:hartjes" -> {
                for (int i = 0; i < 10 / veel; i++) {
                    deeltje(l, ParticleTypes.HEART, x + (r.nextDouble() - 0.5), y + r.nextDouble() * 0.6, z + (r.nextDouble() - 0.5), 0, 0.1, 0);
                }
                l.playLocalSound(x, y, z, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.PLAYERS, 0.5f, 1.6f, false);
            }
            case "plof:bloemen" -> {
                for (int i = 0; i < 24 / veel; i++) {
                    deeltje(l, i % 3 == 0 ? ParticleTypes.SPORE_BLOSSOM_AIR : ParticleTypes.CHERRY_LEAVES, x + (r.nextDouble() - 0.5) * 2.2,
                            y + 0.8 + r.nextDouble(), z + (r.nextDouble() - 0.5) * 2.2, 0, -0.02, 0);
                }
            }
            case "plof:knuffels" -> {
                for (int i = 0; i < 12 / veel; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    deeltje(l, new ItemParticleOption(ParticleTypes.ITEM, i % 2 == 0 ? Items.PINK_WOOL : Items.WHITE_WOOL), x, y, z,
                            Math.cos(a) * 0.18, 0.35 + r.nextDouble() * 0.15, Math.sin(a) * 0.18);
                }
                l.playLocalSound(x, y, z, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 1.4f, false);
            }
            case "plof:giechel" -> {
                for (int i = 0; i < 8 / veel; i++) {
                    deeltje(l, ParticleTypes.NOTE, x + (r.nextDouble() - 0.5), y + 0.3 + r.nextDouble() * 0.6, z + (r.nextDouble() - 0.5), r.nextInt(24) / 24.0, 0, 0);
                }
                l.playLocalSound(x, y, z, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 0.8f, 1.8f, false);
            }
            case "plof:njeg" -> njeg(l, x, y + 0.8, z, mc, veel);
            default -> {   // confetti
                for (int i = 0; i < 26 / veel; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    deeltje(l, new DustParticleOptions(PASTEL[r.nextInt(PASTEL.length)], 1.2f), x, y, z, Math.cos(a) * 0.25, 0.15 + r.nextDouble() * 0.25,
                            Math.sin(a) * 0.25);
                }
            }
        }
    }

    /** "NJEG!" in pink dots (a 4x5 pixel font), facing the camera. */
    private static void njeg(ClientLevel l, double x, double y, double z, Minecraft mc, int veel) {
        // 4x5 glyphs: N J E G !
        String[][] glyph = {
                {"1..1", "11.1", "1.11", "1..1", "1..1"},
                {"..11", "...1", "...1", "1..1", ".11."},
                {"1111", "1...", "111.", "1...", "1111"},
                {".111", "1...", "1.11", "1..1", ".111"},
                {"1", "1", "1", ".", "1"}};
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        Vec3 naar = new Vec3(x - cam.x, 0, z - cam.z);
        if (naar.lengthSqr() < 1e-4) {
            naar = new Vec3(0, 0, 1);
        }
        naar = naar.normalize();
        Vec3 rechts = new Vec3(-naar.z, 0, naar.x);
        double px = 0.16;
        int breedte = 4 + 1 + 4 + 1 + 4 + 1 + 4 + 1 + 1;
        double start = -breedte * px / 2;
        int kol = 0;
        for (String[] g : glyph) {
            for (int rij = 0; rij < 5; rij++) {
                for (int c = 0; c < g[rij].length(); c++) {
                    if (g[rij].charAt(c) == '1' && (veel == 1 || (rij + c) % 2 == 0)) {
                        double off = start + (kol + c) * px;
                        deeltje(l, new DustParticleOptions(0xFF3F9F, 0.45f), x + rechts.x * off, y + (4 - rij) * px, z + rechts.z * off, 0, 0, 0);
                    }
                }
            }
            kol += g[0].length() + 1;
        }
        l.playLocalSound(x, y, z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6f, 1.8f, false);
    }
}
