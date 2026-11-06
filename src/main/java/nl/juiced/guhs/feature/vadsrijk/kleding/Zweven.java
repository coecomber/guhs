package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Knuffelsprong;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * T-KLEDING: gliding with wings (VOORSTEL_ROADMAP §4.2 + R3/R4), the server side. The client moves you (the zone-3 Pluimguh glide,
 * {@code Z3SystemenClient}, now on foot: hold space in the air and you sink at {@link #VAL} blocks/tick; never up), the server:
 * <ul>
 *   <li>says where it is allowed ({@link #mag}): only in the Vadsrijk, never in a boss arena ({@code Arenas.op}), never in a dungeon
 *   ({@link #GEEN_ZWEEF_GEBIEDEN} + {@link #VERBODEN} for later zones), never during a scene;</li>
 *   <li>trusts a glide only from a player who has gliding (the Vahoegveertjes) and wears wings, keeps their fall distance at 0 and
 *   protects the landing (kern's {@link Knuffelsprong#beschermTot});</li>
 *   <li>the wing tricks: double jump / air dash ("truc": fall protection + a puff for everyone), soft landing (no fall damage, pink
 *   poof);</li>
 *   <li>the secret "zweefvlucht": one glide of {@link #GEHEIM_DAAL}+ blocks down gives the Pluisvleugeltjes.</li>
 * </ul>
 * Wings show in the home world too, but nothing here works there.
 */
public final class Zweven {
    /** Sink speed (blocks/tick) = the Pluimguh glide; the "longer glide" wings sink slower. */
    public static final double VAL = -0.12, VAL_LANG = -0.065;
    /** Forward speed while gliding with forward held (blocks/tick): about walking; the "faster glide" wings a bit more. */
    public static final double VOORUIT = 0.22, VOORUIT_SNEL = 0.34;
    /** Double jump upward impulse and the air dash's forward impulse (blocks/tick), once per flight. */
    public static final double SPRONG = 0.48, DASH = 0.95;
    public static final int GEHEIM_DAAL = 100;
    /** Dungeon areas (kern Gebieden ids) without gliding. */
    public static final Set<String> GEEN_ZWEEF_GEBIEDEN = ConcurrentHashMap.newKeySet();
    /** Other slices: more places without gliding (zone 4/5 dungeons...). */
    public static final List<Predicate<ServerPlayer>> VERBODEN = new CopyOnWriteArrayList<>();

    static {
        GEEN_ZWEEF_GEBIEDEN.addAll(List.of("z2_spiegelburcht", "z3_zeppelinwerf", "z3_hart"));
        // T-INT: zone 4/5 dungeons (the boss arenas themselves are covered by Arenas.op): Het Wrak, the whale's Buikgangen and
        // Schuilnest, the Proefarena (sparring)
        GEEN_ZWEEF_GEBIEDEN.addAll(List.of("z4_wrak", "z5_buikgangen", "z5_schuilnest", "z5_proefarena", "z5d_droom"));   // (+ t-droom's Droomwolk)
    }

    private static final Set<UUID> ZWEEFT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Double> START = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAATST = new ConcurrentHashMap<>();

    private Zweven() {
    }

    /** May this player glide here (place rules only)? */
    public static boolean mag(ServerPlayer p) {
        if (!Vadsrijk.in(p) || p.isSpectator() || RijkScenes.bezig(p)) {
            return false;
        }
        if (Arenas.op(p.getX(), p.getY(), p.getZ()) != null) {
            return false;
        }
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
        if (g != null && GEEN_ZWEEF_GEBIEDEN.contains(g.id())) {
            return false;
        }
        for (Predicate<ServerPlayer> v : VERBODEN) {
            if (v.test(p)) {
                return false;
            }
        }
        return true;
    }

    /** The trick of the wings this player wears (null: no wings on). */
    @Nullable
    public static Cosmetica.Kracht kracht(ServerPlayer p) {
        String rug = Kledingkast.keuze(p, "rug");
        return Kledingkast.heeft(p, rug) ? Cosmetica.kracht(rug) : null;
    }

    /** Can this player glide right here right now (gliding unlocked + wings on + allowed here)? */
    public static boolean kan(ServerPlayer p) {
        return Kledingkast.zweven(p) && kracht(p) != null && mag(p);
    }

    public static boolean zweeft(ServerPlayer p) {
        return ZWEEFT.contains(p.getUUID());
    }

    /** Tells the client what it may do (on change: unlock, choice, place). */
    public static void stuurStaat(ServerPlayer p) {
        Cosmetica.Kracht k = kracht(p);
        KledingPayloads.Staat s = new KledingPayloads.Staat(Kledingkast.zweven(p), mag(p), k == null ? "" : k.name(),
                Arenas.op(p.getX(), p.getY(), p.getZ()) != null);
        String sleutel = s.toString();
        LAATST.put(p.getUUID(), sleutel);
        KledingPayloads.stuur(p, s);
    }

    /** Every tick (server): gliders keep fall distance 0; twice a second the place rules are re-sent on change. */
    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        long nu = server.overworld().getGameTime();
        for (UUID id : List.copyOf(ZWEEFT)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null || !kan(p) || p.isPassenger()) {
                ZWEEFT.remove(id);
                START.remove(id);
                continue;
            }
            p.resetFallDistance();
            Knuffelsprong.beschermTot(p, p.level().getGameTime() + 10);
        }
        if (sl == null || nu % 10 != 3) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            Cosmetica.Kracht k = kracht(p);
            KledingPayloads.Staat s = new KledingPayloads.Staat(Kledingkast.zweven(p), mag(p), k == null ? "" : k.name(),
                    Arenas.op(p.getX(), p.getY(), p.getZ()) != null);
            if (!s.toString().equals(LAATST.get(p.getUUID()))) {
                LAATST.put(p.getUUID(), s.toString());
                KledingPayloads.stuur(p, s);
            }
        }
    }

    /** The client's glide state: 1 = started gliding, 0 = stopped, 2 = used its wing trick (double jump / dash). */
    static void ontvang(ServerPlayer p, int staat) {
        UUID id = p.getUUID();
        long t = p.level().getGameTime();
        switch (staat) {
            case 1 -> {
                if (kan(p) && !p.isPassenger()) {
                    ZWEEFT.add(id);
                    START.putIfAbsent(id, p.getY());
                    p.resetFallDistance();
                }
            }
            case 0 -> {
                Double s = START.remove(id);
                if (ZWEEFT.remove(id)) {
                    Knuffelsprong.beschermTot(p, t + 20);
                    p.resetFallDistance();
                    if (s != null && s - p.getY() >= GEHEIM_DAAL) {
                        Kledingkast.geheim(p, "zweefvlucht");
                    }
                }
            }
            case 2 -> {
                Cosmetica.Kracht k = kracht(p);
                if ((k == Cosmetica.Kracht.DUBBELE_SPRONG || k == Cosmetica.Kracht.LUCHTDASH) && mag(p) && p.level() instanceof ServerLevel sl) {
                    Knuffelsprong.beschermTot(p, t + 60);
                    p.resetFallDistance();
                    sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 10, 0.35, 0.1, 0.35, 0.03);
                    sl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.35f, 1.9f);
                }
            }
            default -> {
            }
        }
    }

    /** Soft landing wings (and every glider): no fall damage; the soft-landing wings also make a pink poof. */
    static void val(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Vadsrijk.in(p)) {
            return;
        }
        boolean zacht = kracht(p) == Cosmetica.Kracht.ZACHTE_LANDING;
        if (zacht || ZWEEFT.contains(p.getUUID())) {
            e.setCanceled(true);
            if (zacht && e.getDistance() > 2.5 && p.level() instanceof ServerLevel sl) {
                sl.sendParticles(new DustParticleOptions(0xFF8CC8, 1.6f), p.getX(), p.getY() + 0.1, p.getZ(), 24, 0.6, 0.15, 0.6, 0.02);
                sl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), 8, 0.4, 0.05, 0.4, 0.02);
                sl.playSound(null, p.blockPosition(), SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 1f, 1.3f);
            }
        }
        if (ZWEEFT.remove(p.getUUID())) {
            Double s = START.remove(p.getUUID());
            if (s != null && s - p.getY() >= GEHEIM_DAAL) {
                Kledingkast.geheim(p, "zweefvlucht");
            }
        }
    }

    static void vergeet(UUID id) {
        ZWEEFT.remove(id);
        START.remove(id);
        LAATST.remove(id);
    }
}
