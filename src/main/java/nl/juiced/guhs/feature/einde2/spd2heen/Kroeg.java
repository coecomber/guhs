package nl.juiced.guhs.feature.einde2.spd2heen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2 "Sponsguh", slice sp_d2_heen: the stand-still game in De Ruige Kroeg (part 2 step {@code K_SLEUTEL}, variant
 * {@code 4_b} while it runs; CONTRACT_E2_C 7.6). The player sneaks from just inside the swing gates (the first point of
 * {@code kroeg_sluip}) to the sleeping Mika's table ({@code sleutel}); now and then the bar's Mikas look up.
 * <ul>
 *   <li>START: a title and its one-line rule, the music ({@code deuntje.kroeg}), a soft column of light on the key;</li>
 *   <li>the cycle: {@link Fase#RUST} (walk freely, {@value #RUST_MIN}-{@value #RUST_MAX} ticks), {@link Fase#WAARSCHUW}
 *       ({@value #WAARSCHUW_TICKS} ticks: the barman tinks his glass, a line over the hotbar), {@link Fase#KIJKT}
 *       ({@value #KIJK_TICKS} ticks: the music stops, a big eye on the screen, every Mika turns his head to you). The signal
 *       always comes 1.5 s before a look: there is no hidden timer, only the music and the glass;</li>
 *   <li>moving more than {@value #BEWEEG} blocks sideways while they look = "Wie bewoog daar?!" and back to the last rest
 *       point FACING the route (the start, or one of the two checkpoints on {@code kroeg_sluip}); never an item, never
 *       damage; a fresh rest follows at once;</li>
 *   <li>a COUNTER over the hotbar (blocks to the key, rest point n/2, seen n times); FINISH: within {@value #GEHAALD} blocks of
 *       the key: a title, a sound, the step's flag {@code sp_d2_heen_sleutel}, then {@link SpD2Heen#sleutelGepakt};</li>
 *   <li>it stops (nothing kept but the step) when the player leaves the bar, dies, logs out or the step moves on, and starts
 *       again when they come back in. Everything runs on the SERVER tick count.</li>
 * </ul>
 * Co-op: every player has their own copies of the bar's Mikas ({@code Zicht.alleenVoor}; the shared residents are hidden for
 * players at this step, {@link SpD2Heen}) and their own Sponsguh and Patguh by the gates.
 */
public final class Kroeg {
    public enum Fase { RUST, WAARSCHUW, KIJKT }

    public static final int WAARSCHUW_TICKS = 30, KIJK_TICKS = 50, RUST_MIN = 50, RUST_MAX = 110, RUST_START = 80, RUST_NA_GEZIEN = 70;
    public static final double BEWEEG = 0.15, GEHAALD = 1.7, RUSTPUNT = 1.4;
    /** The checkpoints: these points of {@code kroeg_sluip}. */
    static final int[] RUSTPUNTEN = {2, 3};
    /** The bar's Mikas: marker -> the Kind of the player's copy. */
    static final Map<String, GuhNpcEntity.Kind> MIKAS = mikas();
    static final String MUZIEK = "deuntje.kroeg";

    /** One player's game. */
    static final class Spel {
        Fase fase = Fase.RUST;
        long tot;
        int rustpunt, gezien;
        Vec3 vanaf = Vec3.ZERO;
        final List<Vec3> route;
        final Vec3 sleutel;
        final List<Entity> eigen = new ArrayList<>();
        final List<GuhNpcEntity> kijkers = new ArrayList<>();

        Spel(List<Vec3> route, Vec3 sleutel) {
            this.route = route;
            this.sleutel = sleutel;
        }
    }

    private static final Map<UUID, Spel> SPELLEN = new ConcurrentHashMap<>();

    private Kroeg() {
    }

    private static Map<String, GuhNpcEntity.Kind> mikas() {
        Map<String, GuhNpcEntity.Kind> m = new java.util.LinkedHashMap<>();
        m.put("barman", GuhNpcEntity.Kind.KROEG_BARMAN_MIKA);
        m.put("uitsmijter", GuhNpcEntity.Kind.KROEG_UITSMIJTER_MIKA);
        for (int i = 1; i <= 6; i++) {
            m.put("stoere_mika_" + i, GuhNpcEntity.Kind.STOERE_MIKA);
        }
        m.put("slapende_mika", GuhNpcEntity.Kind.SLAPENDE_MIKA);
        return m;
    }

    public static boolean actief(ServerPlayer p) {
        return SPELLEN.containsKey(p.getUUID());
    }

    /** (tests, dev) the phase of the player's game (null: no game). */
    @Nullable
    public static Fase fase(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        return s == null ? null : s.fase;
    }

    /** (tests) how often the player was seen, and the rest point reached (0 = the start). */
    static int[] stand(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        return s == null ? new int[] {-1, -1} : new int[] {s.gezien, s.rustpunt};
    }

    // =====================================================================================================================
    // start and stop
    // =====================================================================================================================

    /** Starts the game for this player (at the gates, facing in). False: the bar's spots are not known (yet). */
    public static boolean start(ServerPlayer p, long nu) {
        stop(p, false);
        List<Vec3> route = D2Plek.pad(p, D2Plek.SP, "kroeg_sluip");
        Vec3 sleutel = D2Plek.staan(p, D2Plek.SP, "sleutel");
        if (route.size() < 2 || sleutel == null) {
            return false;
        }
        Spel s = new Spel(route, sleutel);
        SPELLEN.put(p.getUUID(), s);
        terugNaar(p, s);
        maakEigen(p, s);
        rust(p, s, nu, RUST_START);
        titel(p, "kroeg.titel", "kroeg.regel");
        SpAudio.stinger(p, MUZIEK);
        return true;
    }

    /** Stops the game (copies gone, music off); {@code melding}: tell the player it starts again when they come back. */
    public static void stop(ServerPlayer p, boolean melding) {
        Spel s = SPELLEN.remove(p.getUUID());
        if (s == null) {
            return;
        }
        s.eigen.forEach(Entity::discard);
        stopMuziek(p);
        if (melding) {
            p.sendSystemMessage(Component.translatable(SpD2Heen.G + "kroeg.weg").withStyle(ChatFormatting.GRAY));
        }
    }

    // =====================================================================================================================
    // the tick
    // =====================================================================================================================

    /** Every tick per player (SpD2Heen.opTick), with the server's tick count. */
    public static void tick(ServerPlayer p, long nu) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        if (!p.isAlive() || !SpD2Heen.opStap(p, nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal.K_SLEUTEL)) {
            stop(p, false);
            return;
        }
        if (!D2Plek.in(p, D2Plek.SP, "ruige_kroeg", 1)) {
            stop(p, true);
            return;
        }
        Vec3 hier = p.position();
        // caught: moved while they look
        if (s.fase == Fase.KIJKT) {
            double dx = hier.x - s.vanaf.x, dz = hier.z - s.vanaf.z;
            if (dx * dx + dz * dz > BEWEEG * BEWEEG) {
                gezien(p, s, nu);
                return;
            }
            if (nu % 5 == 0) {
                for (GuhNpcEntity m : s.kijkers) {
                    float y = D2Plek.yaw(m.position(), hier);
                    m.setYHeadRot(y);
                }
            }
        }
        // the key
        if (D2Plek.dichtbij(hier, s.sleutel, GEHAALD)) {
            gehaald(p, s);
            return;
        }
        // the checkpoints
        for (int i = 0; i < RUSTPUNTEN.length; i++) {
            int punt = RUSTPUNTEN[i];
            if (s.rustpunt < i + 1 && punt < s.route.size() && D2Plek.dichtbij(hier, s.route.get(punt), RUSTPUNT)) {
                s.rustpunt = i + 1;
                p.sendSystemMessage(Component.translatable(SpD2Heen.G + "kroeg.rustpunt", s.rustpunt).withStyle(ChatFormatting.GREEN));
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_CHIME.value(), p.blockPosition(), 0.6f, 1.6f);
            }
        }
        // the cycle
        if (nu >= s.tot) {
            switch (s.fase) {
                case RUST -> {
                    s.fase = Fase.WAARSCHUW;
                    s.tot = nu + WAARSCHUW_TICKS;
                    Vec3 bar = D2Plek.staan(p, D2Plek.SP, "barman");
                    Schijn.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, bar == null ? p.blockPosition() : BlockPos.containing(bar), 1.0f, 1.8f);
                    p.sendOverlayMessage(Component.translatable(SpD2Heen.G + "kroeg.tink").withStyle(ChatFormatting.YELLOW));
                }
                case WAARSCHUW -> {
                    s.fase = Fase.KIJKT;
                    s.tot = nu + KIJK_TICKS;
                    s.vanaf = hier;
                    stopMuziek(p);
                    titel(p, "kroeg.kijkt", "kroeg.kijkt_sub");
                    for (GuhNpcEntity m : s.kijkers) {
                        m.speel("kijk_op");
                        m.setYHeadRot(D2Plek.yaw(m.position(), hier));
                    }
                }
                case KIJKT -> {
                    for (GuhNpcEntity m : s.kijkers) {
                        m.speel("");
                        m.setYHeadRot(m.getYRot());
                    }
                    p.sendOverlayMessage(Component.translatable(SpD2Heen.G + "kroeg.weg_kijk").withStyle(ChatFormatting.GREEN));
                    SpAudio.stinger(p, MUZIEK);
                    rust(p, s, nu, -1);
                }
                default -> {
                }
            }
        }
        // the counter and the light on the key
        if (nu % 10 == 0) {
            if (s.fase != Fase.KIJKT && s.fase != Fase.WAARSCHUW) {
                int blokken = (int) Math.ceil(Math.sqrt(Math.pow(hier.x - s.sleutel.x, 2) + Math.pow(hier.z - s.sleutel.z, 2)));
                p.sendOverlayMessage(Component.translatable(SpD2Heen.G + "kroeg.balk", blokken, s.rustpunt, s.gezien).withStyle(ChatFormatting.GOLD));
            }
            Schijn.deeltjes(p, ParticleTypes.END_ROD, s.sleutel.add(0, 1.2, 0), 3, 0.05, 0.02);
        }
    }

    /** (tests) puts the player's game in this phase now (KIJKT: watched from where they stand). */
    static void zetFase(ServerPlayer p, Fase fase, long nu) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s != null) {
            s.fase = fase;
            s.tot = nu + (fase == Fase.KIJKT ? KIJK_TICKS : fase == Fase.WAARSCHUW ? WAARSCHUW_TICKS : RUST_MAX);
            s.vanaf = p.position();
        }
    }

    // =====================================================================================================================
    // what happens
    // =====================================================================================================================

    private static void rust(ServerPlayer p, Spel s, long nu, int ticks) {
        RandomSource r = p.getRandom();
        s.fase = Fase.RUST;
        s.tot = nu + (ticks > 0 ? ticks : RUST_MIN + r.nextInt(RUST_MAX - RUST_MIN + 1));
    }

    private static void gezien(ServerPlayer p, Spel s, long nu) {
        s.gezien++;
        titel(p, "kroeg.gezien", "kroeg.gezien_sub");
        SpAudio.stinger(p, "stinger.mislukt");
        terugNaar(p, s);
        for (GuhNpcEntity m : s.kijkers) {
            m.speel("");
            m.setYHeadRot(m.getYRot());
        }
        SpAudio.stinger(p, MUZIEK);
        rust(p, s, nu, RUST_NA_GEZIEN);
    }

    private static void gehaald(ServerPlayer p, Spel s) {
        stop(p, false);
        titel(p, "kroeg.gehaald", "kroeg.gehaald_sub");
        Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 0.7f, 1.3f);
        SpAudio.stinger(p, "stinger.opgelost");
        SpD2Heen.sleutelGepakt(p);
    }

    /** Back to the last rest point, facing along the route (playtest 2 item 5: never with your back to the way). */
    private static void terugNaar(ServerPlayer p, Spel s) {
        int punt = s.rustpunt == 0 ? 0 : RUSTPUNTEN[s.rustpunt - 1];
        punt = Math.min(punt, s.route.size() - 1);
        Vec3 plek = s.route.get(punt);
        Vec3 volgende = punt + 1 < s.route.size() ? s.route.get(punt + 1) : s.sleutel;
        float yaw = D2Plek.yaw(plek, volgende);
        if (p.isPassenger()) {
            p.stopRiding();
        }
        p.teleportTo(p.level(), plek.x, plek.y + 0.05, plek.z, java.util.Set.of(), yaw, 10f, true);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        s.vanaf = plek;
    }

    /** The player's own copies of the bar's Mikas, and their Sponsguh and Patguh by the gates. */
    private static void maakEigen(ServerPlayer p, Spel s) {
        ServerLevel level = p.level();
        for (Map.Entry<String, GuhNpcEntity.Kind> e : MIKAS.entrySet()) {
            Vec3 plek = D2Plek.staan(p, D2Plek.SP, e.getKey());
            Plekken.Plek m = Plekken.plek(D2Plek.SP, e.getKey());
            if (plek == null || m == null) {
                continue;
            }
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            if (npc == null) {
                continue;
            }
            npc.setKind(e.getValue());
            npc.snapTo(plek.x, plek.y, plek.z, m.yaw(), 0);
            npc.setYHeadRot(m.yaw());
            npc.setYBodyRot(m.yaw());
            npc.setNoAi(true);                                        // (they only look when the game says so)
            SpD2Heen.eigen(npc, p);
            level.addFreshEntity(npc);
            s.eigen.add(npc);
            if (e.getKey().equals("slapende_mika")) {
                npc.speel("slaap");
            } else {
                s.kijkers.add(npc);
            }
        }
        Vec3 start = s.route.get(0);
        Vec3 sp = D2Plek.staan(p, D2Plek.SP, "d2h_sponsguh_deur"), pa = D2Plek.staan(p, D2Plek.SP, "d2h_patguh_deur");
        maakWezen(p, s, E2SponsguhFeature.SPONSGUH.get().create(level, EntitySpawnReason.TRIGGERED), sp != null ? sp : start.add(2, 0, 0));
        maakWezen(p, s, E2SponsguhFeature.PATGUH.get().create(level, EntitySpawnReason.TRIGGERED), pa != null ? pa : start.add(3, 0, 0));
    }

    private static void maakWezen(ServerPlayer p, Spel s, @Nullable SponsguhWezen w, Vec3 plek) {
        if (w == null) {
            return;
        }
        w.snapTo(plek.x, plek.y, plek.z, 180f, 0);
        w.setYHeadRot(180f);
        w.zetStil(true);
        SpD2Heen.eigen(w, p);
        p.level().addFreshEntity(w);
        s.eigen.add(w);
    }

    private static void titel(ServerPlayer p, String key, String sub) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(4, 36, 8));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(SpD2Heen.G + sub).withStyle(ChatFormatting.YELLOW)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(SpD2Heen.G + key).withStyle(ChatFormatting.GOLD)));
    }

    private static void stopMuziek(ServerPlayer p) {
        if (p.connection != null) {
            p.connection.send(new ClientboundStopSoundPacket(Guhs.id("einde2_sp_audio." + MUZIEK), null));
        }
    }

    static void vergeet(UUID u) {
        Spel s = SPELLEN.remove(u);
        if (s != null) {
            s.eigen.forEach(Entity::discard);
        }
    }
}
