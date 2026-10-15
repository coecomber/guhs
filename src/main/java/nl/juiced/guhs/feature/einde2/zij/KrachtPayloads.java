package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/**
 * einde2 zij: the two payloads of the bending key (CONTRACT_E2_B 7.6).
 * <ul>
 *   <li>{@link Stand} (server -&gt; client): the powers this player may use where they stand now ({@link Krachten#beschikbaar}),
 *       with their icons and cooldowns, and until when they walk on water; sent when it changes and after every use;</li>
 *   <li>{@link Gebruik} (client -&gt; server): a tap of the key ({@link Krachten#gebruik}; the server checks everything again).</li>
 * </ul>
 * The client keeps the last {@link Stand} in {@link #CLIENT} (plain data: the client classes read it).
 */
public final class KrachtPayloads {
    /** One power as the client draws it. */
    public record Regel(String id, int element, String icoon, int afkoel, int rest) {
    }

    /** Client: the last stand the server sent, and the game time it came. */
    public static final class ClientStand {
        public volatile List<Regel> krachten = List.of();
        public volatile long waterTot;
        public volatile long ontvangen;
    }

    public static final ClientStand CLIENT = new ClientStand();

    /** Server: per player, what was sent last (so nothing is sent when nothing changed). */
    private static final Map<UUID, String> LAATST = new ConcurrentHashMap<>();

    private KrachtPayloads() {
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
        registrar.playToServer(Gebruik.TYPE, Gebruik.STREAM_CODEC, Gebruik::handle);
    }

    /** Server: sends this player's stand when it differs from the last one (or always: {@code altijd}). */
    static void stuur(ServerPlayer p, boolean altijd) {
        List<Regel> regels = new ArrayList<>();
        StringBuilder sleutel = new StringBuilder();
        for (Krachten.Kracht k : Krachten.beschikbaar(p)) {
            int rest = Krachten.afkoeling(p, k.id());
            regels.add(new Regel(k.id(), k.element().ordinal(), k.icoon(), k.afkoelTicks(), rest));
            sleutel.append(k.id()).append(rest > 0 ? "*" : "").append(',');
        }
        long water = ZijKrachten.waterTot(p);
        sleutel.append(water > 0 ? "w" : "");
        if (!altijd && sleutel.toString().equals(LAATST.get(p.getUUID()))) {
            return;
        }
        LAATST.put(p.getUUID(), sleutel.toString());
        CompoundTag tag = new CompoundTag();
        ListTag lijst = new ListTag();
        for (Regel r : regels) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", r.id());
            t.putInt("Element", r.element());
            t.putString("Icoon", r.icoon());
            t.putInt("Afkoel", r.afkoel());
            t.putInt("Rest", r.rest());
            lijst.add(t);
        }
        tag.put("Krachten", lijst);
        tag.putInt("Water", water > 0 ? (int) Math.max(0, water - p.level().getGameTime()) : 0);
        ModNetworking.sendTo(p, new Stand(tag));
    }

    /** (Krachten.gebruik) a power was used: the HUD shows its cooldown at once. */
    static void gebruikt(ServerPlayer p) {
        stuur(p, true);
    }

    static void vergeet(UUID id) {
        LAATST.remove(id);
    }

    /** Server -&gt; client: the powers of this moment ({@code Krachten: [{Id, Element, Icoon, Afkoel, Rest}], Water: ticks left}). */
    public record Stand(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("einde2_zij_krachten"));
        public static final StreamCodec<ByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Stand::data, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand s, IPayloadContext context) {
            List<Regel> regels = new ArrayList<>();
            ListTag lijst = s.data().getListOrEmpty("Krachten");
            for (int i = 0; i < lijst.size(); i++) {
                CompoundTag t = lijst.getCompoundOrEmpty(i);
                regels.add(new Regel(t.getStringOr("Id", ""), t.getIntOr("Element", 0), t.getStringOr("Icoon", "minecraft:barrier"),
                        t.getIntOr("Afkoel", 0), t.getIntOr("Rest", 0)));
            }
            long nu = context.player().level().getGameTime();
            int water = s.data().getIntOr("Water", 0);
            CLIENT.krachten = List.copyOf(regels);
            CLIENT.waterTot = water > 0 ? nu + water : 0;
            CLIENT.ontvangen = nu;
        }
    }

    /** Client -&gt; server: use this power now. */
    public record Gebruik(String id) implements CustomPacketPayload {
        public static final Type<Gebruik> TYPE = new Type<>(Guhs.id("einde2_zij_kracht_gebruik"));
        public static final StreamCodec<ByteBuf, Gebruik> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(64), Gebruik::id, Gebruik::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Gebruik g, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer p && p.isAlive() && !p.isSpectator()
                    && !nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(p)) {
                if (!Krachten.gebruik(p, g.id())) {
                    stuur(p, true);
                }
            }
        }
    }
}
