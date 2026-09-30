package nl.juiced.guhs.feature.reisguh;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.RegisterEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * 2.8: the Reisguh is a real guh-conductor (a conducteurspetje and a fluitje, his own model: tools/make_v2.py
 * reisguh_conducteur). Now and then, and whenever someone travels with him, he blows his whistle: "tuut tuut!", a few
 * notes and puffs of steam, and on the clients the whistle goes up to his mouth ({@link Fluit}, client:
 * ReisguhFluitClient). Registers itself (sound, message) on the mod bus.
 */
@EventBusSubscriber(modid = Guhs.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ReisguhFluit {
    /** "Tuut tuut!" (sounds.json reisguh.tuut, sounds/reisguh_tuut.ogg). */
    public static final SoundEvent TUUT = SoundEvent.createVariableRangeEvent(Guhs.id("reisguh.tuut"));
    /** On average once every this many ticks (with a player close by) he blows his whistle just like that. */
    public static final int SOMS = 20 * 40;
    /** How long a blow takes (ticks), and the least time between two. */
    public static final int DUUR = 32, RUST = 60;
    /** Someone walks up to him (closer than GROET_AFSTAND blocks): "tuut!", at most once every GROET_RUST ticks. */
    public static final double GROET_AFSTAND = 6;
    public static final int GROET_RUST = 20 * 30;
    private static final Map<GuhNpcEntity, Long> LAATST = new WeakHashMap<>();
    private static final Map<GuhNpcEntity, Boolean> DICHTBIJ = new WeakHashMap<>();

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> helper.register(Guhs.id("reisguh.tuut"), TUUT));
    }

    @SubscribeEvent
    static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Fluit.TYPE, Fluit.STREAM_CODEC, Fluit::handle);
    }

    /**
     * Every tick of a Reisguh (server): now and then, when someone is around, a whistle; and a welcome "tuut!" when
     * someone walks up to him (not more than once every {@link #GROET_RUST} ticks).
     */
    public static void tick(GuhNpcEntity npc) {
        if (npc.getKind() != GuhNpcEntity.Kind.REISGUH) {
            return;
        }
        if (npc.tickCount % 5 == 0) {
            boolean close = npc.level().getNearestPlayer(npc, GROET_AFSTAND) != null;
            Boolean before = DICHTBIJ.put(npc, close);
            if (close && Boolean.FALSE.equals(before) && (laatst(npc) < 0 || npc.level().getGameTime() - laatst(npc) > GROET_RUST)) {
                fluit(npc);
                return;
            }
        }
        if (npc.getRandom().nextInt(SOMS) == 0 && npc.level().getNearestPlayer(npc, 12) != null) {
            fluit(npc);
        }
    }

    /** Someone travels: the Reisguh they leave blows his whistle, and so does the one at the other end (if he's there). */
    public static void reis(GuhNpcEntity from, ServerLevel level, UUID to) {
        fluit(from);
        if (level.getEntity(to) instanceof GuhNpcEntity other && other.getKind() == GuhNpcEntity.Kind.REISGUH) {
            fluit(other);
        }
    }

    /** Tuut tuut! Returns false when he just blew it (a little rest in between). */
    public static boolean fluit(GuhNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        Long last = LAATST.get(npc);
        if (last != null && now - last < RUST) {
            return false;
        }
        LAATST.put(npc, now);
        Vec3 mond = mond(npc);
        level.playSound(null, mond.x, mond.y, mond.z, TUUT, SoundSource.NEUTRAL, 1.0f, 0.95f + npc.getRandom().nextFloat() * 0.1f);
        level.sendParticles(ParticleTypes.NOTE, mond.x, mond.y + 0.35, mond.z, 2, 0.25, 0.15, 0.25, 1.0);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, mond.x, mond.y + 0.1, mond.z, 5, 0.08, 0.05, 0.08, 0.02);
        level.sendParticles(ParticleTypes.CLOUD, mond.x, mond.y + 0.25, mond.z, 2, 0.05, 0.05, 0.05, 0.01);
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {           // (only real clients that know the message)
            if (p.distanceToSqr(npc) < 96 * 96) {
                nl.juiced.guhs.network.ModNetworking.sendTo(p, new Fluit(npc.getId()));
            }
        }
        return true;
    }

    /** Where his mouth (and so the whistle) is, in the world. */
    public static Vec3 mond(Entity npc) {
        float scale = npc instanceof GuhNpcEntity g ? g.getKind().scale : 1f;
        Vec3 look = Vec3.directionFromRotation(0, npc.getYHeadRot());
        return npc.position().add(look.scale(0.55 * scale)).add(0, 0.98 * scale, 0);
    }

    /** (game tests) when he last blew his whistle (game time), or -1. */
    public static long laatst(GuhNpcEntity npc) {
        Long last = LAATST.get(npc);
        return last == null ? -1 : last;
    }

    /** Server to client: this Reisguh blows his whistle now (the animation). */
    public record Fluit(int entityId) implements CustomPacketPayload {
        public static final Type<Fluit> TYPE = new Type<>(Guhs.id("reisguh_fluit"));
        public static final StreamCodec<FriendlyByteBuf, Fluit> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Fluit::new, Fluit::entityId).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Fluit payload, IPayloadContext context) {
            context.enqueueWork(() -> nl.juiced.guhs.feature.reisguh.client.ReisguhFluitClient.fluit(payload.entityId()));
        }
    }

    private ReisguhFluit() {
    }
}
