package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.Guhs;

/**
 * U-LUCHTSCHIP: walking around on the deck while the ship flies (a "virtual deck").
 * <p>
 * The passengers stay passengers of the ship (so they can never fall off, the ship carries them smoothly from the timetable, and
 * there is no collision deck to fall through or to rubber-band on), but WHERE on the deck each one stands is their own: the client
 * of a passenger walks its own place with WASD ({@code client/DekLopen}, the same speed as on the ground, clamped inside the
 * railing: {@link LuchtschipEntity#klem}) and tells the server ({@link Stap}); the server checks it (on the deck, not too far in one
 * step) and sends everybody's places to the players around ({@link Stand}), whose clients walk the other passengers there softly.
 * Your own walking is predicted locally, so it never lags.
 */
public final class LuchtschipDek {
    /** At most this far per tick between two steps of a client (sprinting is 0.28; room for a late packet). */
    static final double MAX_STAP = 0.45;

    /** (client) the local player's entity id, -1 on the server; the deck sync never overwrites your own place. */
    public static IntSupplier eigen = () -> -1;

    /** Server: the last accepted step per player (game time), to bound how far a step may go. */
    private static final Map<UUID, Long> LAATSTE = new HashMap<>();

    private LuchtschipDek() {
    }

    static int eigenId() {
        return eigen.getAsInt();
    }

    // ------------------------------------------------------------------------------------------------ server

    /** A client walked on the deck: check and keep it. */
    static void stap(ServerPlayer p, Stap s) {
        if (!(p.getVehicle() instanceof LuchtschipEntity e) || e.getId() != s.schip()) {
            return;
        }
        Vec3 oud = e.dekPlek(p);
        Vec3 nieuw = LuchtschipEntity.klem(s.x(), s.z());
        long nu = p.level().getServer().getTickCount();
        long vorig = LAATSTE.getOrDefault(p.getUUID(), nu - 1);
        double mag = MAX_STAP * Math.max(1, Math.min(10, nu - vorig)) + 0.1;
        LAATSTE.put(p.getUUID(), nu);
        if (nieuw.distanceTo(oud) > mag) {
            // too far in one go (lag, or another start place than the client thought): take a step that way and put the client right
            nieuw = LuchtschipEntity.klem(oud.x + (nieuw.x - oud.x) * mag / nieuw.distanceTo(oud), oud.z + (nieuw.z - oud.z) * mag / nieuw.distanceTo(oud));
            e.zetDek(p, nieuw);
            PacketDistributor.sendToPlayer(p, Stand.van(e, true));
            return;
        }
        e.zetDek(p, nieuw);
    }

    /** (server, every other tick) send the deck of every ship whose places changed; every second all of them (new onlookers). */
    static void stuur(LuchtschipEntity e, boolean altijd) {
        if (!(e.dekVuil || altijd && !e.getPassengers().isEmpty()) || e.level().isClientSide()) {
            return;
        }
        e.dekVuil = false;
        PacketDistributor.sendToPlayersTrackingEntity(e, Stand.van(e, false));
    }

    static void vergeet(ServerPlayer p) {
        LAATSTE.remove(p.getUUID());
    }

    // ------------------------------------------------------------------------------------------------ client

    /** (client) the server's places of a ship's passengers ({@code eigen}: also yours, the server put you right). */
    public static void ontvang(Level level, Stand s) {
        if (!(level.getEntity(s.schip()) instanceof LuchtschipEntity e)) {
            return;
        }
        int ik = eigenId();
        e.dekDoel.clear();
        for (int i = 0; i < s.ids().length; i++) {
            Vec3 v = LuchtschipEntity.klem(s.xs()[i], s.zs()[i]);
            if (s.ids()[i] == ik) {
                if (s.eigen() || !e.dek.containsKey(ik)) {   // (a correction, or your start place before you walked)
                    e.dek.put(ik, v);
                    correctie = v;
                }
                continue;
            }
            e.dekDoel.put(s.ids()[i], v);
        }
    }

    /** (client) your own walked place on the deck. */
    public static void zetEigen(LuchtschipEntity e, Entity p, Vec3 plek) {
        e.dek.put(p.getId(), plek);
    }

    /** (client) set when the server put your own place right; the walker takes it over once. */
    public static volatile Vec3 correctie;

    private static Field fSnelOud, fSnel, fPlek;
    private static boolean geenVelden;
    /** Per entity: the walk animation's own speed and position (the passenger tick stops the animation every tick). */
    private static final Map<Integer, float[]> LOOP = new HashMap<>();

    /**
     * (client, from the ship's tick) Passengers do not swing their legs (LivingEntity stops the walk animation of riders every
     * tick): keep their own walk animation, the same as LivingEntity#updateWalkAnimation would; {@link #zetLoop} puts it back
     * after the passenger's own tick.
     */
    static void loopAnimatie(Entity q, float afstand) {
        if (!(q instanceof LivingEntity) || geenVelden) {
            return;
        }
        float[] st = LOOP.computeIfAbsent(q.getId(), id -> new float[3]);
        float doel = Math.min(afstand * 4f, 1f);
        st[2] = st[0];
        st[0] = st[0] + (doel - st[0]) * 0.4f;
        st[1] += st[0];
        if (st[0] < 1e-3f && doel == 0) {
            LOOP.remove(q.getId());
        }
    }

    /** (client, when the ship places its passenger, after the passenger's tick) the walk animation back on. */
    static void zetLoop(Entity q) {
        float[] st = LOOP.get(q.getId());
        if (st == null || !(q instanceof LivingEntity le) || geenVelden) {
            return;
        }
        try {
            if (fSnel == null) {
                fSnelOud = veld("speedOld");
                fSnel = veld("speed");
                fPlek = veld("position");
            }
            WalkAnimationState w = le.walkAnimation;
            fSnelOud.setFloat(w, st[2]);
            fSnel.setFloat(w, st[0]);
            fPlek.setFloat(w, st[1]);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            geenVelden = true;   // (another mapping: then no legs, walking works anyway)
        }
    }

    private static Field veld(String naam) throws NoSuchFieldException {
        Field f = WalkAnimationState.class.getDeclaredField(naam);
        f.setAccessible(true);
        return f;
    }

    // ------------------------------------------------------------------------------------------------ payloads

    /** Client -&gt; server: my place on the deck of this ship (ship frame). */
    public record Stap(int schip, float x, float z) implements CustomPacketPayload {
        public static final Type<Stap> TYPE = new Type<>(Guhs.id("vr_luchtschip_stap"));
        public static final StreamCodec<ByteBuf, Stap> STREAM_CODEC = StreamCodec.of((b, s) -> {
            b.writeInt(s.schip);
            b.writeFloat(s.x);
            b.writeFloat(s.z);
        }, b -> new Stap(b.readInt(), b.readFloat(), b.readFloat()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -&gt; client: the places of a ship's passengers ({@code eigen}: yours is meant too, a correction). */
    public record Stand(int schip, int[] ids, float[] xs, float[] zs, boolean eigen) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("vr_luchtschip_dek"));
        public static final StreamCodec<ByteBuf, Stand> STREAM_CODEC = StreamCodec.of((b, s) -> {
            b.writeInt(s.schip);
            b.writeBoolean(s.eigen);
            b.writeShort(s.ids.length);
            for (int i = 0; i < s.ids.length; i++) {
                b.writeInt(s.ids[i]);
                b.writeFloat(s.xs[i]);
                b.writeFloat(s.zs[i]);
            }
        }, b -> {
            int schip = b.readInt();
            boolean eigen = b.readBoolean();
            int n = Math.min(64, b.readShort());
            int[] ids = new int[n];
            float[] xs = new float[n], zs = new float[n];
            for (int i = 0; i < n; i++) {
                ids[i] = b.readInt();
                xs[i] = b.readFloat();
                zs[i] = b.readFloat();
            }
            return new Stand(schip, ids, xs, zs, eigen);
        });

        static Stand van(LuchtschipEntity e, boolean eigen) {
            List<Entity> q = new ArrayList<>(e.getPassengers());
            int[] ids = new int[q.size()];
            float[] xs = new float[q.size()], zs = new float[q.size()];
            for (int i = 0; i < q.size(); i++) {
                Vec3 v = e.dekPlek(q.get(i));
                ids[i] = q.get(i).getId();
                xs[i] = (float) v.x;
                zs[i] = (float) v.z;
            }
            return new Stand(e.getId(), ids, xs, zs, eigen);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
