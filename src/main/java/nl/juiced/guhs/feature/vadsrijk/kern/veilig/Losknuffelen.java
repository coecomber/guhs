package nl.juiced.guhs.feature.vadsrijk.kern.veilig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * "Losknuffelen": the unstuck key (U, rebindable, category Vadsrijk) and {@code /guhs vadsrijk vast}. Stand still for 3 s
 * (moving or getting hit stops it) and the knuffelwind puts you on the nearest safe spot of the map (a walkable floor on the
 * main network within 64 blocks, paths/campfires preferred; else your last campfire). Once a minute; not in a boss fight
 * (there you faint or walk out through the door). Also: a "Vast? Druk op U" hint when you hang around in a closed hole for
 * 20 s or are below y 40, and falling under the map (y < 20) brings you back to safety right away.
 */
public final class Losknuffelen {
    public static final int KANAAL_TICKS = 60, COOLDOWN_TICKS = 1200, VAST_TICKS = 400;
    /** Below this (feet y) the "Vast?" hint shows; below {@link #BODEM_Y} you are rescued at once. Lowest walkable floor of the map: ~53. */
    public static final int DIEP_Y = 40, BODEM_Y = 20;
    static final int STRAAL_OPSLUITING = 8, MAX_KNOPEN = 3000;
    private static final String L = "vadsrijk.kern.vast.";

    public enum Reden {
        OK, NIET_IN_RIJK, IN_GEVECHT, WACHT, BEZIG
    }

    private record Kanaal(Vec3 start, long begin) {
    }

    private static final class Waak {
        Vec3 anker = Vec3.ZERO;
        long sinds;
        long hintOp;
        @Nullable
        Vec3 laatsteGoed;
    }

    private static final Map<UUID, Kanaal> KANALEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> KLAAR_OP = new ConcurrentHashMap<>();
    private static final Map<UUID, Waak> WAAK = new ConcurrentHashMap<>();

    private Losknuffelen() {
    }

    // ------------------------------------------------------------------ rules (pure, for the gametests)

    /** May this player start? {@code nu} = game time. */
    public static Reden mag(UUID id, long nu, boolean inRijk, boolean inGevecht) {
        if (!inRijk) {
            return Reden.NIET_IN_RIJK;
        }
        if (KANALEN.containsKey(id)) {
            return Reden.BEZIG;
        }
        if (inGevecht) {
            return Reden.IN_GEVECHT;
        }
        if (nu < KLAAR_OP.getOrDefault(id, Long.MIN_VALUE)) {
            return Reden.WACHT;
        }
        return Reden.OK;
    }

    /** Seconds until this player may use it again (0 = now). */
    public static int wachtSeconden(UUID id, long nu) {
        long op = KLAAR_OP.getOrDefault(id, Long.MIN_VALUE);
        return nu >= op ? 0 : (int) Math.ceil((op - nu) / 20.0);
    }

    static void zetCooldown(UUID id, long nu) {
        KLAAR_OP.put(id, nu + COOLDOWN_TICKS);
    }

    /** T-OPSLAG: the game time this player's channel began (-1 = none), for the cast bar (opslag.Gietbalk). */
    public static long kanaalBegin(UUID id) {
        Kanaal k = KANALEN.get(id);
        return k == null ? -1 : k.begin();
    }

    /** Dev/tests: forget this player's cooldown and channel. */
    public static void vergeet(UUID id) {
        KLAAR_OP.remove(id);
        KANALEN.remove(id);
        WAAK.remove(id);
    }

    /** In an active boss fight: inside an arena whose boss is alive, or within the radius of a loose (arena-less) boss. */
    public static boolean inGevecht(ServerLevel sl, Vec3 v) {
        Arena a = Arenas.op(v.x, v.y, v.z);
        if (a != null && Arenas.baas(sl, a.id()) != null) {
            return true;
        }
        return !sl.getEntitiesOfClass(VadsrijkBaas.class, new AABB(v, v).inflate(64),
                b -> b.arena() == null && b.losseStraal() > 0 && !b.geplofd() && b.position().distanceTo(v) <= b.losseStraal()).isEmpty();
    }

    // ------------------------------------------------------------------ the key / command
    public static void probeer(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        long nu = sl.getGameTime();
        Reden r = mag(p.getUUID(), nu, Vadsrijk.in(p), Vadsrijk.in(p) && inGevecht(sl, p.position()));
        switch (r) {
            case NIET_IN_RIJK -> p.sendOverlayMessage(Component.translatable(L + "alleen_rijk").withStyle(ChatFormatting.GRAY));
            case BEZIG -> p.sendOverlayMessage(Component.translatable(L + "bezig").withStyle(ChatFormatting.LIGHT_PURPLE));
            case IN_GEVECHT -> p.sendOverlayMessage(Component.translatable(L + "gevecht").withStyle(ChatFormatting.RED));
            case WACHT -> p.sendOverlayMessage(Component.translatable(L + "wacht", wachtSeconden(p.getUUID(), nu)).withStyle(ChatFormatting.GRAY));
            case OK -> {
                KANALEN.put(p.getUUID(), new Kanaal(p.position(), nu));
                Tips.toon(p, "vadsrijk.kern.tip.vast");
                p.sendOverlayMessage(Component.translatable(L + "bezig").withStyle(ChatFormatting.LIGHT_PURPLE));
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.8f);
            }
        }
    }

    /** Getting hit stops the channel (kern's damage pipeline, {@code Schade.NA_KLAP}). */
    static void geraakt(ServerPlayer p) {
        if (KANALEN.remove(p.getUUID()) != null) {
            p.sendOverlayMessage(Component.translatable(L + "gestopt.geraakt").withStyle(ChatFormatting.GRAY));
        }
    }

    // ------------------------------------------------------------------ ticking
    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            KANALEN.clear();
            return;
        }
        long nu = sl.getGameTime();
        KANALEN.keySet().removeIf(id -> !(sl.getEntity(id) instanceof ServerPlayer));
        for (ServerPlayer p : sl.players()) {
            Kanaal k = KANALEN.get(p.getUUID());
            if (k != null) {
                kanaal(sl, p, k, nu);
            }
            if (p.isCreative() || p.isSpectator()) {
                continue;
            }
            if (p.getY() < BODEM_Y) {
                red(sl, p);
                continue;
            }
            if ((nu + p.getId()) % 20 == 0) {
                waak(sl, p, nu);
            }
        }
    }

    private static void kanaal(ServerLevel sl, ServerPlayer p, Kanaal k, long nu) {
        if (p.position().distanceToSqr(k.start) > 0.75 * 0.75) {
            KANALEN.remove(p.getUUID());
            p.sendOverlayMessage(Component.translatable(L + "gestopt.bewogen").withStyle(ChatFormatting.GRAY));
            return;
        }
        long t = nu - k.begin;
        if (t % 4 == 0) {   // a rising pink swirl
            double a = t * 0.6;
            for (int i = 0; i < 3; i++) {
                double h = (t / (double) KANAAL_TICKS) * 2.2, hoek = a + i * 2.094;
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX() + Math.cos(hoek) * 0.8, p.getY() + h, p.getZ() + Math.sin(hoek) * 0.8, 1, 0, 0, 0, 0);
            }
            sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 0.2, p.getZ(), 1, 0.3, 0.05, 0.3, 0.01);
        }
        if (t % 20 == 0 && t > 0 && t < KANAAL_TICKS) {
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 0.9f + t / 60f);
        }
        if (t >= KANAAL_TICKS) {
            KANALEN.remove(p.getUUID());
            zetCooldown(p.getUUID(), nu);
            Doel d = doel(sl, p);
            breng(sl, p, d.pos, Component.translatable(d.kampvuur ? L + "kampvuur" : L + "klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void waak(ServerLevel sl, ServerPlayer p, long nu) {
        Waak w = WAAK.computeIfAbsent(p.getUUID(), id -> new Waak());
        if (p.onGround() && p.getY() >= DIEP_Y && !p.isInWater()) {
            w.laatsteGoed = p.position();
        }
        if (p.position().distanceToSqr(w.anker) > 3 * 3) {
            w.anker = p.position();
            w.sinds = nu;
        }
        if (nu < w.hintOp || KANALEN.containsKey(p.getUUID())) {
            return;
        }
        boolean diep = p.getY() < DIEP_Y;
        if (diep || (nu - w.sinds >= VAST_TICKS && Opsluiting.ingesloten(sl, p.blockPosition(), STRAAL_OPSLUITING, MAX_KNOPEN))) {
            hint(p);
            w.hintOp = nu + (diep ? 300 : 600);
        } else if (nu - w.sinds >= VAST_TICKS) {
            w.hintOp = nu + 200;   // standing still but not shut in (AFK at a campfire): look again in 10 s
        }
    }

    static void hint(ServerPlayer p) {
        p.sendOverlayMessage(Component.translatable(L + "hint", Component.keybind("key.guhs.vr_vast").withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        Tips.toon(p, "vadsrijk.kern.tip.vast");
    }

    /** Fell under the map: straight back, no channel, no cooldown. */
    static void red(ServerLevel sl, ServerPlayer p) {
        KANALEN.remove(p.getUUID());
        Doel d = doel(sl, p);
        breng(sl, p, d.pos, Component.translatable(L + "gered").withStyle(ChatFormatting.LIGHT_PURPLE));
        Tips.toon(p, "vadsrijk.kern.tip.vast");
    }

    // ------------------------------------------------------------------ where to
    record Doel(BlockPos pos, boolean kampvuur) {
    }

    static Doel doel(ServerLevel sl, ServerPlayer p) {
        Waak w = WAAK.get(p.getUUID());
        BlockPos b = zoek(sl, p.position());
        if (b == null && w != null && w.laatsteGoed != null) {
            b = zoek(sl, w.laatsteGoed);
        }
        if (b != null) {
            return new Doel(b, false);
        }
        BlockPos c = Spelers.data(p).checkpoint;
        return new Doel(c != null ? c : Flauwvallen.standaard(p), true);
    }

    @Nullable
    static BlockPos zoek(ServerLevel sl, Vec3 van) {
        var kand = VeiligePlekken.rond(sl.getServer(), van, VeiligePlekken.STRAAL);
        return VeiligePlekken.kies(kand, van, pl -> geldig(sl, pl));
    }

    /** A real stand spot: floor below, feet + head free, no water/lava at the feet, not in an active boss fight. */
    static boolean geldig(ServerLevel sl, VeiligePlekken.Plek pl) {
        BlockPos b = pl.pos();
        if (inGevecht(sl, Vec3.atBottomCenterOf(b))) {
            return false;
        }
        if (!sl.isLoaded(b)) {
            return pl.soort() != VeiligePlekken.POI;   // generated spots are walkable by construction (audit); POIs need a look
        }
        return staanplek(sl, b);
    }

    static boolean staanplek(ServerLevel sl, BlockPos b) {
        return Opsluiting.vrij(sl, b) && Opsluiting.vrij(sl, b.above()) && !sl.getBlockState(b.below()).getCollisionShape(sl, b.below()).isEmpty()
                && !sl.getFluidState(b).is(FluidTags.WATER) && !sl.getFluidState(b).is(FluidTags.LAVA);
    }

    static void breng(ServerLevel sl, ServerPlayer p, BlockPos doel, Component bericht) {
        sl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 15, 0.4, 0.6, 0.4, 0.02);
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(doel), Vec3.ZERO, p.getYRot(), p.getXRot(), Relative.ROTATION, TeleportTransition.DO_NOTHING));
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, doel.getX() + 0.5, doel.getY() + 1, doel.getZ() + 0.5, 30, 0.7, 0.8, 0.7, 0.02);
        sl.sendParticles(ParticleTypes.HEART, doel.getX() + 0.5, doel.getY() + 2, doel.getZ() + 0.5, 3, 0.4, 0.2, 0.4, 0);
        sl.playSound(null, doel, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
        p.sendOverlayMessage(bericht);
        Waak w = WAAK.computeIfAbsent(p.getUUID(), id -> new Waak());
        w.anker = p.position();
        w.sinds = sl.getGameTime();
        w.laatsteGoed = p.position();
    }
}
