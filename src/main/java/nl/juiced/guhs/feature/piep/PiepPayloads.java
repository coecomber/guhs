package nl.juiced.guhs.feature.piep;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * Piep's messages. Server to client: whether a player carries a muisje on their shoulder (guhs:piep_schouder), a guh that
 * wiggles while Poepschilly is inside it (guhs:piep_wiebel), "open the menu of this maatje" (guhs:piep_menu) and "your
 * turtle is ready for a guh" (guhs:piep_klaar). Client to server: a menu button (guhs:piep_actie) and "I clicked this guh
 * while my turtle was ready" (guhs:piep_op_guh: the guh's own tap/hold handler swallows empty-hand clicks on your own tamed
 * guh, so the client asks for the poetsbeurt itself, see client.PiepMenuClient).
 */
public final class PiepPayloads {
    /** Client: player entity id -> the look of the muisje on their shoulder (its name). */
    public static final Map<Integer, CompoundTag> CLIENT_SCHOUDERS = new ConcurrentHashMap<>();
    /** Client: guh entity id -> game time until which it wiggles. */
    public static final Map<Integer, Long> CLIENT_WIEBEL = new ConcurrentHashMap<>();

    /** Client: turtle entity id -> game time until which it is ready for a guh (guhs:piep_klaar). */
    public static final Map<Integer, Long> CLIENT_KLAAR = new ConcurrentHashMap<>();
    /** Client: opens the menu (set by client.PiepMenuClient; the dedicated server never gets this payload). */
    public static volatile java.util.function.Consumer<MenuOpen> menuOpener = m -> {
    };

    public record SchouderData(int speler, boolean heeft, CompoundTag uiterlijk) implements CustomPacketPayload {
        public static final Type<SchouderData> TYPE = new Type<>(Guhs.id("piep_schouder"));
        public static final StreamCodec<FriendlyByteBuf, SchouderData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SchouderData::speler, ByteBufCodecs.BOOL, SchouderData::heeft, ByteBufCodecs.COMPOUND_TAG,
                SchouderData::uiterlijk, SchouderData::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SchouderData p, IPayloadContext context) {
            if (p.heeft()) {
                CLIENT_SCHOUDERS.put(p.speler(), p.uiterlijk());
            } else {
                CLIENT_SCHOUDERS.remove(p.speler());
            }
        }
    }

    public record Wiebel(int guh, int ticks) implements CustomPacketPayload {
        public static final Type<Wiebel> TYPE = new Type<>(Guhs.id("piep_wiebel"));
        public static final StreamCodec<FriendlyByteBuf, Wiebel> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Wiebel::guh, ByteBufCodecs.VAR_INT, Wiebel::ticks, Wiebel::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Wiebel p, IPayloadContext context) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) {
                CLIENT_WIEBEL.put(p.guh(), level.getGameTime() + p.ticks());
            }
        }
    }

    /** Server to client: open the menu of this maatje; rust = seconds its big button still rests. */
    public record MenuOpen(int dier, int rust) implements CustomPacketPayload {
        public static final Type<MenuOpen> TYPE = new Type<>(Guhs.id("piep_menu"));
        public static final StreamCodec<FriendlyByteBuf, MenuOpen> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MenuOpen::dier, ByteBufCodecs.VAR_INT, MenuOpen::rust, MenuOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MenuOpen p, IPayloadContext context) {
            menuOpener.accept(p);
        }
    }

    /** Client to server: a menu button ({@link PiepMenu.Actie}; waarde = the setting for WISSEL, tekst = the new name). */
    public record MenuActie(int dier, int actie, int waarde, String tekst) implements CustomPacketPayload {
        public static final Type<MenuActie> TYPE = new Type<>(Guhs.id("piep_actie"));
        public static final StreamCodec<FriendlyByteBuf, MenuActie> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MenuActie::dier, ByteBufCodecs.VAR_INT, MenuActie::actie, ByteBufCodecs.VAR_INT, MenuActie::waarde,
                ByteBufCodecs.stringUtf8(PiepMenu.MAX_NAAM), MenuActie::tekst, MenuActie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MenuActie p, IPayloadContext context) {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp
                    && sp.level().getEntity(p.dier()) instanceof PiepMaatje maatje) {
                PiepMenu.doe(sp, maatje, PiepMenu.Actie.byIndex(p.actie()), p.waarde(), p.tekst());
            }
        }
    }

    /** Server to client: this turtle is ready for a guh for this many ticks (0: not any more). */
    public record Klaar(int schilly, int ticks) implements CustomPacketPayload {
        public static final Type<Klaar> TYPE = new Type<>(Guhs.id("piep_klaar"));
        public static final StreamCodec<FriendlyByteBuf, Klaar> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Klaar::schilly, ByteBufCodecs.VAR_INT, Klaar::ticks, Klaar::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Klaar p, IPayloadContext context) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null || p.ticks() <= 0) {
                CLIENT_KLAAR.remove(p.schilly());
            } else {
                CLIENT_KLAAR.put(p.schilly(), level.getGameTime() + p.ticks());
            }
        }
    }

    /** Client to server: the player right-clicked this guh (empty hand) while one of their turtles was ready. */
    public record OpGuh(int guh) implements CustomPacketPayload {
        public static final Type<OpGuh> TYPE = new Type<>(Guhs.id("piep_op_guh"));
        public static final StreamCodec<FriendlyByteBuf, OpGuh> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, OpGuh::guh, OpGuh::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(OpGuh p, IPayloadContext context) {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp
                    && sp.level().getEntity(p.guh()) instanceof nl.juiced.guhs.entity.GuhEntity guh) {
                PoepschillyEntity.opGuhGeklikt(guh, sp);
            }
        }
    }

    /** Sends to every player that can see this entity (and the entity itself, if it is a player); test players are skipped. */
    static void naarKijkers(net.minecraft.world.entity.Entity entity, CustomPacketPayload payload) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        int range = entity.getType().clientTrackingRange() * 16 + 16;
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if (p == entity || p.distanceToSqr(entity) <= (double) range * range) {
                nl.juiced.guhs.network.ModNetworking.sendTo(p, payload);
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(SchouderData.TYPE, SchouderData.STREAM_CODEC, SchouderData::handle);
        registrar.playToClient(Wiebel.TYPE, Wiebel.STREAM_CODEC, Wiebel::handle);
        registrar.playToClient(MenuOpen.TYPE, MenuOpen.STREAM_CODEC, MenuOpen::handle);
        registrar.playToClient(Klaar.TYPE, Klaar.STREAM_CODEC, Klaar::handle);
        registrar.playToServer(MenuActie.TYPE, MenuActie.STREAM_CODEC, MenuActie::handle);
        registrar.playToServer(OpGuh.TYPE, OpGuh.STREAM_CODEC, OpGuh::handle);
    }

    private PiepPayloads() {
    }
}
