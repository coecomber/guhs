package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The ghosts on the wire. Server to client: a whole ghost ({@code guhs:spook}: where, how turned, the ontwerp as a dense
 * grid with a palette, at most ~0.5 MB for a 64-box), a ghost that is gone, the progress of a building ghost (every second),
 * and the control panel ({@link #bediening}). Client to server: a button of the control panel ({@code guhs:spook_bedien}).
 * <p>
 * The client draws only the spots where its world differs from the ghost, so building needs no updates of the grid.
 */
public final class SpookPayloads {
    /** Client hooks (set by client.BouwhulpClient). */
    public static volatile Consumer<CompoundTag> ontvang = t -> {
    };
    public static volatile Consumer<UUID> wegHaak = id -> {
    };
    public static volatile Runnable allesWegHaak = () -> {
    };
    public static volatile Consumer<CompoundTag> voortgangHaak = t -> {
    };
    public static volatile Consumer<CompoundTag> bedieningHaak = t -> {
    };

    /** The buttons of the control panel (sent by ordinal: append only). */
    public enum Actie { VERVERS, DRAAI, SPIEGEL, SCHUIF, BOUW, ALSNOG, PAUZE, ANNULEER, TERUG, TOEVOEGEN, WEGHALEN,
        /** Test round 5: the missing blocks as a written book. */
        LIJSTJE }

    public record Data(String soort, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(Guhs.id("spook"));
        public static final StreamCodec<FriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), Data::soort,
                ByteBufCodecs.COMPOUND_TAG, Data::data, Data::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Data p, IPayloadContext context) {
            switch (p.soort()) {
                case "spook" -> ontvang.accept(p.data());
                case "weg" -> {
                    if (p.data().getBooleanOr("Alles", false)) {
                        allesWegHaak.run();
                    } else {
                        p.data().read("Id", UUIDUtil.CODEC).ifPresent(wegHaak);
                    }
                }
                case "voortgang" -> voortgangHaak.accept(p.data());
                case "bediening" -> bedieningHaak.accept(p.data());
                default -> {
                }
            }
        }
    }

    public record Bedien(UUID id, int actie, int waarde, String tekst) implements CustomPacketPayload {
        public static final Type<Bedien> TYPE = new Type<>(Guhs.id("spook_bedien"));
        public static final StreamCodec<FriendlyByteBuf, Bedien> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Bedien::id,
                ByteBufCodecs.VAR_INT, Bedien::actie, ByteBufCodecs.VAR_INT, Bedien::waarde, ByteBufCodecs.stringUtf8(32), Bedien::tekst, Bedien::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Bedien p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                Spookbouw.bedien(sp, p.id(), p.actie(), p.waarde(), p.tekst());
            }
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Data.TYPE, Data.STREAM_CODEC, Data::handle);
        registrar.playToServer(Bedien.TYPE, Bedien.STREAM_CODEC, Bedien::handle);
    }

    // =====================================================================================================================
    // sending
    // =====================================================================================================================

    /** A ghost (new or changed) to everybody in its dimension. */
    static void stuur(ServerLevel level, Spook s) {
        CompoundTag t = pak(level, s);
        for (ServerPlayer p : level.players()) {
            PacketDistributor.sendToPlayer(p, new Data("spook", t));
        }
    }

    /** All ghosts of the player's dimension (login, another dimension): first "forget everything". */
    public static void stuurAlles(ServerPlayer p) {
        CompoundTag leeg = new CompoundTag();
        leeg.putBoolean("Alles", true);
        PacketDistributor.sendToPlayer(p, new Data("weg", leeg));
        for (Spook s : Spoken.van(p.level()).alle()) {
            PacketDistributor.sendToPlayer(p, new Data("spook", pak(p.level(), s)));
        }
    }

    static void weg(ServerLevel level, UUID id) {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, id);
        for (ServerPlayer p : level.players()) {
            PacketDistributor.sendToPlayer(p, new Data("weg", t));
        }
    }

    static void voortgang(ServerLevel level, Spook s) {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, s.id);
        t.putInt("Gedaan", s.gedaan);
        t.putInt("Rest", s.nogTeGaan());
        t.putInt("Mist", s.mist.size());
        t.putInt("Status", s.status.ordinal());
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(s.oorsprong, 160)) {
                PacketDistributor.sendToPlayer(p, new Data("voortgang", t));
            }
        }
    }

    /** Opens (or refreshes) the control panel of a ghost for this player. */
    public static void bediening(ServerPlayer p, Spook s, Map<Item, Integer> tekort, boolean vraagAlsnog) {
        ServerLevel level = p.level();
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, s.id);
        t.putString("Naam", s.naam);
        t.putString("Eigenaar", s.eigenaarNaam);
        t.putBoolean("Mag", s.mag(p));
        t.putInt("Status", s.status.ordinal());
        t.putInt("Blokken", s.totaal());
        t.putInt("Gedaan", s.gedaan);
        t.putInt("Rest", s.nogTeGaan());
        t.putInt("Mist", s.mist.size());
        Vec3i g = s.ontwerp.grootte();
        t.putIntArray("Grootte", new int[] {g.getX(), g.getY(), g.getZ()});
        t.putLong("Oorsprong", s.oorsprong.asLong());
        t.putInt("Draai", s.draai.ordinal());
        t.putInt("Spiegel", s.spiegel.ordinal());
        t.putBoolean("Keet", s.bouwerPos != null);
        bereik(level, s, t);
        t.putBoolean("VraagAlsnog", vraagAlsnog);
        t.putInt("Ongedaan", Spoken.van(level).ongedaanTeMaken(p.getUUID()));
        t.put("Tekort", items(tekort));
        t.put("Nodig", items(Spoken.van(level).nogNodig(level, s)));
        ListTag h = new ListTag();
        for (UUID u : s.helpers) {
            ServerPlayer hp = level.getServer().getPlayerList().getPlayer(u);
            h.add(net.minecraft.nbt.StringTag.valueOf(hp != null ? hp.getGameProfile().name() : u.toString().substring(0, 8)));
        }
        t.put("Helpers", h);
        PacketDistributor.sendToPlayer(p, new Data("bediening", t));
    }

    static ListTag items(Map<Item, Integer> items) {
        ListTag l = new ListTag();
        List<Map.Entry<Item, Integer>> lijst = new ArrayList<>(items.entrySet());
        lijst.sort(Map.Entry.<Item, Integer>comparingByValue().reversed());
        for (Map.Entry<Item, Integer> e : lijst) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", BuiltInRegistries.ITEM.getKey(e.getKey()).toString());
            c.putInt("N", e.getValue());
            l.add(c);
        }
        return l;
    }

    /**
     * Test round 5: the builder's reach (Bereik, around Bouwer), how many of the ghost's blocks lie outside it (Buiten), and a
     * keet's reach at its next level (BereikVolgend), for the red ghost blocks and the warning.
     */
    public static void bereik(ServerLevel level, Spook s, CompoundTag t) {
        Bouwer b = Spoken.van(level).bouwerVan(level, s);
        if (b == null || s.bouwerPos == null || b.bereik(level) == Integer.MAX_VALUE) {
            return;
        }
        t.putInt("Bereik", b.bereik(level));
        t.putInt("Buiten", Spoken.buiten(level, s, b));
        if (b instanceof nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity keet && keet.werkNiveau().volgende() != null) {
            t.putInt("BereikVolgend", keet.werkNiveau().volgende().bereik);
        }
    }

    /** One ghost as the client gets it. */
    static CompoundTag pak(ServerLevel level, Spook s) {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, s.id);
        t.store("EigenaarId", UUIDUtil.CODEC, s.eigenaar);
        t.putString("Eigenaar", s.eigenaarNaam);
        t.putString("Naam", s.naam);
        t.putLong("Oorsprong", s.oorsprong.asLong());
        t.putInt("Draai", s.draai.ordinal());
        t.putInt("Spiegel", s.spiegel.ordinal());
        t.putInt("Status", s.status.ordinal());
        t.putInt("Gedaan", s.gedaan);
        t.putInt("Rest", s.nogTeGaan());
        if (s.bouwerPos != null) {
            t.putLong("Bouwer", s.bouwerPos.asLong());
            bereik(level, s, t);
        }
        ListTag helpers = new ListTag();
        s.helpers.forEach(u -> helpers.add(net.minecraft.nbt.StringTag.valueOf(u.toString())));
        t.put("Helpers", helpers);
        // the ontwerp: palette (global state ids) + a dense grid x, y, z (0 = nothing, else palette index + 1)
        Vec3i g = s.ontwerp.grootte();
        Map<BlockState, Integer> index = new LinkedHashMap<>();
        for (BlockState st : s.ontwerp.blokken().values()) {
            index.computeIfAbsent(st, k -> index.size());
        }
        boolean breed = index.size() > 254;
        int cellen = g.getX() * g.getY() * g.getZ();
        byte[] raster = new byte[cellen * (breed ? 2 : 1)];
        for (Map.Entry<BlockPos, BlockState> e : s.ontwerp.blokken().entrySet()) {
            BlockPos p = e.getKey();
            int i = (p.getX() * g.getY() + p.getY()) * g.getZ() + p.getZ();
            int v = index.get(e.getValue()) + 1;
            if (breed) {
                raster[i * 2] = (byte) (v >> 8);
                raster[i * 2 + 1] = (byte) v;
            } else {
                raster[i] = (byte) v;
            }
        }
        t.putIntArray("Palet", index.keySet().stream().mapToInt(Block::getId).toArray());
        t.putIntArray("Grootte", new int[] {g.getX(), g.getY(), g.getZ()});
        t.putBoolean("Breed", breed);
        t.putByteArray("Raster", raster);
        return t;
    }

    /** The six arrow directions of the panel (SCHUIF's waarde). */
    public static Direction richting(int i) {
        return Direction.values()[Math.floorMod(i, 6)];
    }

    /** The panel's text for an item (client and server share it). */
    public static Component itemNaam(String id) {
        return BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.parse(id)).map(i -> i.getDefaultInstance().getHoverName())
                .orElse(Component.literal(id));
    }

    /** (Client) a rotation / mirror from their ordinals. */
    public static Rotation draai(int i) {
        return Rotation.values()[Math.floorMod(i, Rotation.values().length)];
    }

    public static Mirror spiegel(int i) {
        return Mirror.values()[Math.floorMod(i, Mirror.values().length)];
    }

    private SpookPayloads() {
    }
}
