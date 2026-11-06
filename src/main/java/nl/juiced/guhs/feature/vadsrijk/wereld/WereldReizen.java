package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.IntConsumer;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * The airship line Knuffelkamp &lt;-&gt; De Haven (design ronde 11: luchtschip-lijnen, no fast travel otherwise), as a
 * sightseeing flight: right-click the ship's bell at a dock (or let an NPC call {@link #vlieg}) and a non-click camera scene of
 * ~32 s flies you from the deck out over the sea, past a passing airship whose guhs wave at you, through the clouds, along the
 * Zwamzoet skyline and down to the other dock. After the first time you can skip it by sneaking.
 * <p>
 * Safety (fix round 1): the camera scene needs the chunks around the camera, so the player's (invisible, damage-free) body
 * rides along with the camera; the landing is computed from the real map ({@link WereldPlekken}): the target chunks are loaded
 * (generated) first, then a safe floor is searched near the planned spot (never inside blocks, never below the map). The body is
 * put there at the end, and checked again a second later.
 * <p>
 * {@link #MAG_VLIEGEN}: the npc slice may lock the line until the camp story is done (default: always open).
 */
public final class WereldReizen {
    public enum Dok {
        KAMP, HAVEN;

        public BlockPos aankomst() {
            return this == KAMP ? WereldPlekken.KAMP_LANDING : WereldPlekken.HAVEN_LANDING;
        }

        public BlockPos bel() {
            return this == KAMP ? WereldPlekken.KAMP_BEL : WereldPlekken.HAVEN_BEL;
        }

        public Dok ander() {
            return this == KAMP ? HAVEN : KAMP;
        }

        /** Where you look when you arrive: Knuffelkamp = west over the jetty to the camp, De Haven = north to the arch. */
        float aankomstYaw() {
            return this == KAMP ? 90f : 180f;
        }
    }

    /** May this player fly from -> to? Default: yes. The npc slice may replace this. */
    public static BiPredicate<ServerPlayer, Dok> MAG_VLIEGEN = (p, naar) -> true;

    /** gezien-flag: this player has made the sightseeing flight once (from then on sneaking skips it). */
    public static final String GEZIEN = "vr_w_rondvlucht";
    static final int DUUR = 720;
    /** The airship that passes on the way (built by tools/vadsrijk/wereld/bouwsels.rondvlucht): its balcony with the guhs. */
    static final Vec3 SCHIP = nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(40, 118, 760);
    static final Vec3 SKYLINE = new Vec3(0, 165, 0);

    private record Vlucht(long start, Dok naar, Vec3 landing, List<Vec3> pad, boolean overslaanbaar, List<UUID> zwaaiers) {
    }

    private static final Map<UUID, Vlucht> BEZIG = new HashMap<>();

    private WereldReizen() {
    }

    /** Fly this player from one dock to the other. */
    public static void vlieg(ServerPlayer p, Dok van, Dok naar) {
        vlieg(p, van, naar, false);
    }

    /** {@code dev}: the dev command flies even when the line is still locked for this player. */
    public static void vlieg(ServerPlayer p, Dok van, Dok naar, boolean dev) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl) || RijkScenes.bezig(p) || BEZIG.containsKey(p.getUUID())) {
            return;
        }
        if (!dev && !MAG_VLIEGEN.test(p, naar)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.luchtschip.nog_niet").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        Vec3 landing = veiligeLanding(sl, naar.aankomst());
        boolean gezien = Spelers.data(p).gezien.contains(GEZIEN);
        Spelers.data(p).gezien.add(GEZIEN);
        Spelers.opgeslagen(p);
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.4f, 1.1f);
        List<Vec3> pad = route(van, naar, landing);
        RijkScene.Builder b = RijkScene.maak("vr_w_luchtschip", DUUR);
        for (int t = 0; t <= DUUR; t += 2) {
            Vec3[] c = camera(pad, t, naar, landing);
            b.camera(t, c[0], c[1]);
        }
        b.titel("vadsrijk.wereld.luchtschip.titel." + naar.name().toLowerCase(), "vadsrijk.wereld.luchtschip.onder", DUUR - 150, 110, 0x9AD5FF);
        Vlucht v = new Vlucht(sl.getGameTime(), naar, landing, pad, gezien, new ArrayList<>());
        BEZIG.put(p.getUUID(), v);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DUUR + 20, 0, false, false, false));
        volg(p, v, 0);
        RijkScenes.speel(p, b.bouw());
        PacketDistributor.sendToPlayer(p, new Info(gezien ? DUUR : 0));
        if (gezien) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.luchtschip.overslaan").withStyle(ChatFormatting.GRAY));
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, 0.5f, 1.2f);
    }

    // ------------------------------------------------------------------------------------------------ the route

    /** Control points of the flight (positions only; the look direction follows the path and glances at the sights). */
    private static List<Vec3> route(Dok van, Dok naar, Vec3 landing) {
        List<Vec3> heen = new ArrayList<>(List.of(
                nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(140, 100, 985), nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(122, 118, 905), nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(96, 124, 805), nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(98, 130, 700),
                nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(112, 138, 600), new Vec3(126, 146, 450), new Vec3(118, 150, 300), new Vec3(104, 148, 175),
                new Vec3(150, 138, 230), new Vec3(150, 120, 340), new Vec3(122, 98, 430), new Vec3(98, 78, 468)));
        Vec3 a = Vec3.atBottomCenterOf(van.aankomst()).add(0, 1.62, 0);
        Vec3 eind = landing.add(0, 1.62, 0);
        List<Vec3> pad = new ArrayList<>();
        pad.add(a);
        if (naar == Dok.HAVEN) {
            pad.addAll(heen);
            pad.add(eind.add(0, 0.6, 3));
        } else {
            List<Vec3> terug = new ArrayList<>(List.of(
                    new Vec3(98, 82, 470), new Vec3(130, 104, 420), new Vec3(150, 128, 330), new Vec3(140, 145, 210),
                    new Vec3(100, 150, 180), new Vec3(112, 148, 300), new Vec3(126, 142, 450), nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(112, 136, 600),
                    nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(98, 128, 700), nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(96, 122, 805), nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(122, 112, 910), nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kampMeng(160, 98, 1000)));
            pad.addAll(terug);
            pad.add(eind.add(3, 0.6, 0));
        }
        pad.add(eind);
        return pad;
    }

    /** Catmull-Rom position along the control points at 0..1 (uniform in segment count). */
    private static Vec3 spline(List<Vec3> pts, double u) {
        int n = pts.size() - 1;
        double f = Mth.clamp(u, 0, 1) * n;
        int i = Math.min(n - 1, (int) Math.floor(f));
        double t = f - i;
        Vec3 p0 = pts.get(Math.max(0, i - 1)), p1 = pts.get(i), p2 = pts.get(i + 1), p3 = pts.get(Math.min(n, i + 2));
        double t2 = t * t, t3 = t2 * t;
        return p0.scale(-0.5 * t3 + t2 - 0.5 * t).add(p1.scale(1.5 * t3 - 2.5 * t2 + 1)).add(p2.scale(-1.5 * t3 + 2 * t2 + 0.5 * t))
                .add(p3.scale(0.5 * t3 - 0.5 * t2));
    }

    /** Time -> path parameter: gentle start and end, otherwise steady. */
    private static double tijd(int t) {
        double x = Mth.clamp(t / (double) DUUR, 0, 1);
        double r = 0.06;
        if (x < r) {
            return x * x / (2 * r) / (1 - r);
        }
        if (x > 1 - r) {
            double y = 1 - x;
            return 1 - y * y / (2 * r) / (1 - r);
        }
        return (x - r / 2) / (1 - r);
    }

    private static double bult(double d, double r) {
        double w = Mth.clamp((r - d) / (r * 0.6), 0, 1);
        return w * w * (3 - 2 * w);
    }

    /** Path progress -> path parameter: the camera slows down along the Zwamzoet skyline (u ~ 0.5..0.65), so the client has time
     * to load and show the city. Table of the normalised cumulative "slowness". */
    private static final double[] TRAAG = new double[1001];

    static {
        double som = 0;
        for (int i = 0; i <= 1000; i++) {
            double u = i / 1000.0;
            TRAAG[i] = som;
            som += 1 + 1.8 * Math.exp(-Math.pow((u - 0.57) / 0.07, 2));
        }
        for (int i = 0; i <= 1000; i++) {
            TRAAG[i] /= TRAAG[1000];
        }
    }

    private static double traag(double s) {
        int lo = 0, hi = 1000;
        while (hi - lo > 1) {
            int m = (lo + hi) / 2;
            if (TRAAG[m] <= s) {
                lo = m;
            } else {
                hi = m;
            }
        }
        double d = TRAAG[hi] - TRAAG[lo];
        return (lo + (d <= 0 ? 0 : (s - TRAAG[lo]) / d)) / 1000.0;
    }

    /** {position, look-at} of the camera at tick t. */
    static Vec3[] camera(List<Vec3> pad, int t, Dok naar, Vec3 landing) {
        double u = traag(tijd(t));
        Vec3 pos = spline(pad, u);
        Vec3 vooruit = spline(pad, Math.min(1, u + 0.035));
        Vec3 kijk = vooruit.subtract(pos).lengthSqr() < 1e-4 ? pos.add(0, 0, -1) : pos.add(vooruit.subtract(pos).normalize().scale(20));
        kijk = kijk.add(0, -4, 0);
        double ws = bult(pos.distanceTo(SCHIP), 110) * 0.85;
        kijk = kijk.lerp(SCHIP, ws);
        double wk = bult(Math.hypot(pos.x, pos.z), 330) * 0.9;
        kijk = kijk.lerp(SKYLINE, wk);
        // the last seconds: look where you arrive
        double eind = Mth.clamp((t - (DUUR - 90)) / 70.0, 0, 1);
        if (eind > 0) {
            float yaw = naar.aankomstYaw();
            Vec3 voor = landing.add(0, 1.62, 0).add(-Math.sin(Math.toRadians(yaw)) * 20, 1.5, Math.cos(Math.toRadians(yaw)) * 20);
            eind = eind * eind * (3 - 2 * eind);
            kijk = kijk.lerp(voor, eind);
        }
        return new Vec3[] {pos, kijk};
    }

    // ------------------------------------------------------------------------------------------------ during the flight

    /** Every 4 ticks: the body follows the camera (chunks load around it), clouds, the waving guhs, and at the end the landing. */
    private static void volg(ServerPlayer p, Vlucht v, int t) {
        Later.na(t == 0 ? 1 : 4, () -> {
            if (BEZIG.get(p.getUUID()) != v || p.isRemoved() || !(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
                ruimOp(p.getUUID(), v);
                return;
            }
            int nu = (int) (sl.getGameTime() - v.start());
            if (nu >= DUUR - 6) {
                land(p, v);
                return;
            }
            Vec3[] c = camera(v.pad(), Math.min(DUUR, nu + 4), v.naar(), v.landing());
            // the (invisible) body flies a little ahead of the camera, so the chunks it is about to see are already sent
            Vec3 lijf = camera(v.pad(), Math.min(DUUR - 8, nu + 30), v.naar(), v.landing())[0].add(0, -1.62, 0);
            p.connection.teleport(lijf.x, lijf.y, lijf.z, p.getYRot(), p.getXRot());
            p.resetFallDistance();
            p.setDeltaMovement(Vec3.ZERO);
            // through the clouds: a puff of cloud around the camera between ~z 560 and 720
            if (c[0].z > 560 && c[0].z < 720 && nu % 8 == 0) {
                sl.sendParticles(p, ParticleTypes.CLOUD, true, true, c[0].x, c[0].y, c[0].z, 40, 6, 2.5, 6, 0.02);
            }
            if (v.zwaaiers().isEmpty() && c[0].distanceTo(SCHIP) < 140) {
                zwaaiers(sl, v);
            }
            volg(p, v, nu);
        });
    }

    private static void zwaaiers(ServerLevel sl, Vlucht v) {
        GuhVariant[] soorten = {GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.SNOW, GuhVariant.NORMAL};
        sl.getChunk(Mth.floor(SCHIP.x) >> 4, Mth.floor(SCHIP.z) >> 4);
        for (int i = 0; i < 4; i++) {
            GuhEntity g = nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
            if (g == null) {
                continue;
            }
            g.setVariant(soorten[i]);
            g.snapTo(SCHIP.x - 6 + i * 4 + 0.5, SCHIP.y - 12, SCHIP.z + 3.5, 0f, 0f);
            g.setYHeadRot(0f);
            g.setYBodyRot(0f);
            g.setWandering(false);
            g.setNoAi(true);
            g.setInvulnerable(true);
            sl.addFreshEntity(g);
            g.emotes.start(Emote.ZWAAIEN, true, GuhEmotes.Source.SELF);
            v.zwaaiers().add(g.getUUID());
        }
        Later.na(20 * 12, () -> weg(sl, v.zwaaiers()));
    }

    private static void weg(ServerLevel sl, List<UUID> ids) {
        for (UUID id : ids) {
            Entity e = sl.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
    }

    private static void ruimOp(UUID speler, Vlucht v) {
        BEZIG.remove(speler, v);
    }

    /** Put the player on the safe landing spot (also when the flight is skipped), and check it again a second later. */
    private static void land(ServerPlayer p, Vlucht v) {
        BEZIG.remove(p.getUUID(), v);
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
            return;
        }
        Vec3 doel = veiligeLanding(sl, v.naar().aankomst());
        p.teleport(new TeleportTransition(sl, doel, Vec3.ZERO, v.naar().aankomstYaw(), 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        p.setDeltaMovement(Vec3.ZERO);
        p.removeEffect(MobEffects.INVISIBILITY);
        // the scene restores the client's old view direction when it ends: turn the player to the arrival again after it
        Later.na(14, () -> {
            if (!p.isRemoved() && p.level() == sl) {
                p.connection.teleport(p.getX(), p.getY(), p.getZ(), v.naar().aankomstYaw(), 0f);
            }
        });
        Later.na(30, () -> {
            if (!p.isRemoved() && p.level() == sl && (p.getY() < doel.y - 2.5 || !vrij(sl, p.blockPosition()))) {
                Vec3 nog = veiligeLanding(sl, v.naar().aankomst());
                p.teleport(new TeleportTransition(sl, nog, Vec3.ZERO, v.naar().aankomstYaw(), 0f, TeleportTransition.DO_NOTHING));
                p.resetFallDistance();
            }
        });
    }

    /** Sneak during a flight you have seen before: skip to the landing (a short black-bar moment at the dock). */
    static void overslaan(ServerPlayer p) {
        Vlucht v = BEZIG.get(p.getUUID());
        if (v == null || !v.overslaanbaar() || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 oog = v.landing().add(0, 1.62, 0);
        float yaw = v.naar().aankomstYaw();
        Vec3 voor = oog.add(-Math.sin(Math.toRadians(yaw)) * 20, 1.5, Math.cos(Math.toRadians(yaw)) * 20);
        land(p, v);
        RijkScenes.speel(p, RijkScene.maak("vr_w_luchtschip_kort", 24).camera(0, oog.add(0, 3, 0), voor).camera(22, oog, voor).bouw());
        weg(sl, v.zwaaiers());
    }

    // ------------------------------------------------------------------------------------------------ safe landing

    /**
     * A safe standing spot near {@code plek}: the chunks around it are loaded (and generated) first; then the nearest column
     * position (spiral up to 4 blocks) with a solid, non-liquid floor and two free blocks above, searching from 3 below the
     * planned y up to 24 above it. Never below the map; falls back to the highest solid block of the column.
     */
    public static Vec3 veiligeLanding(ServerLevel sl, BlockPos plek) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sl.getChunk((plek.getX() >> 4) + dx, (plek.getZ() >> 4) + dz);
            }
        }
        for (int r = 0; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos v = vloer(sl, plek.offset(dx, 0, dz));
                    if (v != null) {
                        return Vec3.atBottomCenterOf(v);
                    }
                }
            }
        }
        int top = sl.getMaxY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(plek.getX(), top, plek.getZ());
        while (m.getY() > sl.getMinY() + 1 && sl.getBlockState(m).getCollisionShape(sl, m).isEmpty()) {
            m.move(0, -1, 0);
        }
        return Vec3.atBottomCenterOf(m.above());
    }

    /** The first stand position in this column from y-3 up to y+24 (null: none). */
    private static BlockPos vloer(ServerLevel sl, BlockPos plek) {
        int min = Math.max(sl.getMinY() + 1, plek.getY() - 3);
        int max = Math.min(sl.getMaxY() - 2, plek.getY() + 24);
        for (int y = min; y <= max; y++) {
            BlockPos s = new BlockPos(plek.getX(), y, plek.getZ());
            BlockPos onder = s.below();
            BlockState o = sl.getBlockState(onder);
            if (!o.getCollisionShape(sl, onder).isEmpty() && o.getFluidState().isEmpty() && vrij(sl, s)) {
                return s;
            }
        }
        return null;
    }

    private static boolean vrij(ServerLevel sl, BlockPos s) {
        for (BlockPos q : new BlockPos[] {s, s.above()}) {
            BlockState b = sl.getBlockState(q);
            if (!b.getCollisionShape(sl, q).isEmpty() || !b.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Right-click on a block: is it one of the two ships' bells? */
    static boolean klik(ServerPlayer p, BlockPos pos) {
        for (Dok d : Dok.values()) {
            if (d.bel().distManhattan(pos) <= 1) {
                // g-int (round 2): the bell opens g-ux's travel map (all destinations + locks); the map flies through vlieg
                p.level().playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 1.1f);
                nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart.open(p);
                return true;
            }
        }
        return false;
    }

    static void vergeet(UUID id) {
        BEZIG.remove(id);
    }

    // ------------------------------------------------------------------------------------------------ payloads

    /** Server -> client: a flight started; {@code overslaanTicks} > 0 = sneaking skips it during that many ticks. */
    public record Info(int overslaanTicks) implements CustomPacketPayload {
        public static final Type<Info> TYPE = new Type<>(Guhs.id("vr_wereld_vlucht"));
        public static final StreamCodec<ByteBuf, Info> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Info::new, Info::overslaanTicks);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: the player sneaked during a skippable flight. */
    public record Overslaan() implements CustomPacketPayload {
        public static final Type<Overslaan> TYPE = new Type<>(Guhs.id("vr_wereld_vlucht_overslaan"));
        public static final StreamCodec<ByteBuf, Overslaan> STREAM_CODEC = StreamCodec.unit(new Overslaan());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Overslaan o, IPayloadContext c) {
            if (c.player() instanceof ServerPlayer p) {
                overslaan(p);
            }
        }
    }

    /** Set by the client (WereldClient): receives {@link Info}. */
    public static IntConsumer ontvanger = t -> {
    };
}
