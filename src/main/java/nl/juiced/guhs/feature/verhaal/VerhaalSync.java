package nl.juiced.guhs.feature.verhaal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (verhaal engine): the story state of a player on their own client. Server to client {@code guhs:verhaal_stand}
 * (every registered line's step and text variant, the followed line, the scenes and narrator cards seen, the structures
 * Guhdalfs sluier still hides), sent at login and whenever something of it changes; client to server
 * {@code guhs:verhaal_volg} (the line picked in the Guhdex, "" = follow by itself). The client side reads {@link Client}.
 */
public final class VerhaalSync {
    /** GuhQuests.saved: the counter behind "touched last" and the line the player picked to follow. */
    static final String TELLER = "guhs_verhaal_teller", GEKOZEN = "guhs_verhaal_volg";
    /** How often the server looks whether a text variant or a sluier changed without a step (ticks). */
    public static final int CHECK_TICKS = 40;

    /** Server: what each player was sent last (to notice changes that are not a step). */
    private static final Map<UUID, CompoundTag> LAATST = new ConcurrentHashMap<>();

    public record Stand(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("verhaal_stand"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Stand::data, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand p, IPayloadContext context) {
            context.enqueueWork(() -> Client.zet(p.data()));
        }
    }

    public record Volg(String lijn) implements CustomPacketPayload {
        public static final Type<Volg> TYPE = new Type<>(Guhs.id("verhaal_volg"));
        public static final StreamCodec<FriendlyByteBuf, Volg> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Volg::lijn, Volg::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Volg p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> kies(sp, p.lijn()));
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
        registrar.playToServer(Volg.TYPE, Volg.STREAM_CODEC, Volg::handle);
    }

    // =====================================================================================================================
    // server
    // =====================================================================================================================

    /** The line this player picked in the Guhdex ("" = none: the story is followed by itself). */
    public static String gekozen(ServerPlayer p) {
        return GuhQuests.saved(p).getStringOr(GEKOZEN, "");
    }

    /** Picks the line to follow (a registered line, or "" / null to follow by itself). */
    public static void kies(ServerPlayer p, @Nullable String lijn) {
        if (lijn == null || lijn.isEmpty() || Verhaallijnen.van(lijn) == null) {
            GuhQuests.saved(p).remove(GEKOZEN);
        } else {
            GuhQuests.saved(p).putString(GEKOZEN, lijn);
        }
        sync(p);
    }

    /** Everything the client knows about this player's stories. */
    static CompoundTag stand(ServerPlayer p) {
        CompoundTag data = new CompoundTag();
        CompoundTag lijnen = new CompoundTag();
        for (Verhaallijn l : Verhaallijnen.alle()) {
            CompoundTag t = new CompoundTag();
            t.putInt("S", l.stap(p));
            t.putString("K", l.sleutel(p));
            t.putBoolean("B", l.begonnen(p));
            lijnen.put(l.id(), t);
        }
        data.put("Lijnen", lijnen);
        Verhaallijn volg = Verhaallijnen.gevolgd(p);
        data.putString("Volg", volg == null ? "" : volg.id());
        data.putString("Gekozen", gekozen(p));
        ListTag scenes = new ListTag();
        for (Cutscene s : Cutscene.alle()) {
            if (Cutscenes.gezien(p, s.id())) {
                scenes.add(StringTag.valueOf(s.id()));
            }
        }
        data.put("Scenes", scenes);
        ListTag kaarten = new ListTag();
        for (String k : Verteller.ids()) {
            if (Verteller.gezien(p, k)) {
                kaarten.add(StringTag.valueOf(k));
            }
        }
        data.put("Kaarten", kaarten);
        ListTag verborgen = new ListTag();
        for (String s : Sluiers.structuren()) {
            if (Sluiers.isVerborgen(p, s)) {
                verborgen.add(StringTag.valueOf(s));
            }
        }
        data.put("Verborgen", verborgen);
        return data;
    }

    /** Sends this player's story state. */
    public static void sync(ServerPlayer p) {
        CompoundTag data = stand(p);
        LAATST.put(p.getUUID(), data);
        ModNetworking.sendTo(p, new Stand(data));
    }

    /** (every {@link #CHECK_TICKS} ticks) a text variant, the followed line or a sluier changed without a step: sync. */
    static void kijk(ServerPlayer p) {
        if (!stand(p).equals(LAATST.get(p.getUUID()))) {
            sync(p);
        }
    }

    static void vergeet(UUID speler) {
        LAATST.remove(speler);
    }

    // =====================================================================================================================
    // client
    // =====================================================================================================================

    /** The local player's story state as the server sent it (client side; only plain data, no client classes). */
    public static final class Client {
        private record Lijn(int stap, String sleutel, boolean begonnen) {
        }

        private static volatile Map<String, Lijn> lijnen = Map.of();
        private static volatile String volg = "", gekozen = "";
        private static volatile Set<String> scenes = Set.of(), kaarten = Set.of(), verborgen = Set.of();
        private static volatile int versie;
        /** Told after new data arrived (the Guhdex redraws). */
        public static volatile Runnable vernieuwd = () -> {
        };

        static void zet(CompoundTag data) {
            Map<String, Lijn> m = new HashMap<>();
            CompoundTag l = data.getCompoundOrEmpty("Lijnen");
            for (String id : l.keySet()) {
                CompoundTag t = l.getCompoundOrEmpty(id);
                m.put(id, new Lijn(t.getIntOr("S", 0), t.getStringOr("K", "0"), t.getBooleanOr("B", false)));
            }
            lijnen = Map.copyOf(m);
            volg = data.getStringOr("Volg", "");
            gekozen = data.getStringOr("Gekozen", "");
            scenes = lijst(data, "Scenes");
            kaarten = lijst(data, "Kaarten");
            verborgen = lijst(data, "Verborgen");
            versie++;
            vernieuwd.run();
        }

        private static Set<String> lijst(CompoundTag data, String key) {
            Set<String> out = new HashSet<>();
            for (Tag t : data.getListOrEmpty(key)) {
                t.asString().ifPresent(out::add);
            }
            return Set.copyOf(out);
        }

        /** The step of the local player in this line (-1: unknown). */
        public static int stap(String id) {
            Lijn l = lijnen.get(id);
            return l == null ? -1 : l.stap();
        }

        /** The text variant of this line now ("2", "3_bewoner", "klaar"; "0" when unknown). */
        public static String sleutel(String id) {
            Lijn l = lijnen.get(id);
            return l == null ? "0" : l.sleutel();
        }

        public static boolean begonnen(String id) {
            Lijn l = lijnen.get(id);
            return l != null && l.begonnen();
        }

        /** The followed line ("" = none). */
        public static String volg() {
            return volg;
        }

        /** The line the player picked ("" = following by itself). */
        public static String gekozen() {
            return gekozen;
        }

        public static boolean sceneGezien(String id) {
            return scenes.contains(id);
        }

        public static boolean kaartGezien(String id) {
            return kaarten.contains(id);
        }

        /** Is this structure still behind Guhdalfs sluier for the local player (not in the Superkompas)? */
        public static boolean verborgen(String structuur) {
            return verborgen.contains(structuur);
        }

        public static int versie() {
            return versie;
        }

        private Client() {
        }
    }

    private VerhaalSync() {
    }
}
