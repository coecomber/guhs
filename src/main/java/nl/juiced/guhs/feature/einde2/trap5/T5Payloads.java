package nl.juiced.guhs.feature.einde2.trap5;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.bank.BankAdressen;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The messages of tier 5 (ids {@code guhs:einde2_trap5_machines_*}). Server -> client: the door's answer ({@link Open}),
 * "this bank screen comes from a terminal" ({@link TerminalOpen}), the order list ({@link BestelLijst}), a Thuispoort's
 * screen ({@link PoortOpen}). Client -> server: the list from a terminal ({@link BestelVraag}), an order ({@link Bestel}),
 * a Thuispoort action ({@link PoortActie}). The client's side is {@link #KLANT} (set by {@code client.E2Trap5Client}: common
 * code never names a client class).
 */
public final class T5Payloads {
    /** What the client does with the messages that open a screen (null on a dedicated server). */
    public interface Klant {
        void terminal(BlockPos terminal);

        void lijst(BestelLijst lijst);

        void poort(PoortOpen open);
    }

    public static volatile Klant KLANT;

    private T5Payloads() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToClient(TerminalOpen.TYPE, TerminalOpen.STREAM_CODEC, TerminalOpen::handle);
        registrar.playToClient(BestelLijst.TYPE, BestelLijst.STREAM_CODEC, BestelLijst::handle);
        registrar.playToClient(PoortOpen.TYPE, PoortOpen.STREAM_CODEC, PoortOpen::handle);
        registrar.playToServer(BestelVraag.TYPE, BestelVraag.STREAM_CODEC, BestelVraag::handle);
        registrar.playToServer(Bestel.TYPE, Bestel.STREAM_CODEC, Bestel::handle);
        registrar.playToServer(PoortActie.TYPE, PoortActie.STREAM_CODEC, PoortActie::handle);
    }

    static void stuur(ServerPlayer p, CustomPacketPayload payload) {
        ModNetworking.sendTo(p, payload);
    }

    /** Sends the order list of these balies (their machines' results, and how many of each the bank holds). */
    static void stuurLijst(ServerPlayer p, BlockPos bron, boolean terminal, List<Bestelbalie.BlockEntityImpl> balies, UUID bank) {
        List<ItemStack> items = new ArrayList<>();
        for (Bestelbalie.BlockEntityImpl b : balies) {
            if (b.getLevel() instanceof ServerLevel level) {
                for (ItemStack s : b.kan(level)) {
                    if (items.stream().noneMatch(x -> ItemStack.isSameItemSameComponents(x, s))) {
                        items.add(s);
                    }
                }
            }
        }
        BankGuhBlockEntity bk = BankAdressen.zoek(p.level().getServer(), bank, false);
        List<Integer> inBank = new ArrayList<>();
        for (ItemStack s : items) {
            inBank.add(bk == null ? -1 : (int) Math.min(Integer.MAX_VALUE, bk.getStorage().count(s)));
        }
        stuur(p, new BestelLijst(bron, terminal, items, inBank, balies.size()));
    }

    // =====================================================================================================================
    // server -> client
    // =====================================================================================================================

    public record Open(boolean open) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("einde2_trap5_machines_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Open::open, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Open payload, IPayloadContext context) {
            E2Trap5.klantOpen = payload.open();
        }
    }

    public record TerminalOpen(BlockPos terminal) implements CustomPacketPayload {
        public static final Type<TerminalOpen> TYPE = new Type<>(Guhs.id("einde2_trap5_machines_terminal"));
        public static final StreamCodec<FriendlyByteBuf, TerminalOpen> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, TerminalOpen::terminal,
                TerminalOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(TerminalOpen payload, IPayloadContext context) {
            Klant k = KLANT;
            if (k != null) {
                k.terminal(payload.terminal());
            }
        }
    }

    /** The order list: what can be made, how many the bank holds of each (-1: the bank is not loaded), how many balies. */
    public record BestelLijst(BlockPos bron, boolean terminal, List<ItemStack> items, List<Integer> inBank, int balies) implements CustomPacketPayload {
        public static final Type<BestelLijst> TYPE = new Type<>(Guhs.id("einde2_trap5_machines_lijst"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BestelLijst> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, BestelLijst::bron,
                ByteBufCodecs.BOOL, BestelLijst::terminal,
                ItemStack.OPTIONAL_LIST_STREAM_CODEC, BestelLijst::items,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), BestelLijst::inBank,
                ByteBufCodecs.VAR_INT, BestelLijst::balies,
                BestelLijst::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(BestelLijst payload, IPayloadContext context) {
            Klant k = KLANT;
            if (k != null) {
                k.lijst(payload);
            }
        }
    }

    public record PoortOpen(BlockPos poort, CompoundTag data) implements CustomPacketPayload {
        public static final Type<PoortOpen> TYPE = new Type<>(Guhs.id("einde2_trap5_machines_poort"));
        public static final StreamCodec<FriendlyByteBuf, PoortOpen> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, PoortOpen::poort,
                ByteBufCodecs.COMPOUND_TAG, PoortOpen::data, PoortOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(PoortOpen payload, IPayloadContext context) {
            Klant k = KLANT;
            if (k != null) {
                k.poort(payload);
            }
        }
    }

    // =====================================================================================================================
    // client -> server
    // =====================================================================================================================

    /** The "Bestellen" button of a bank screen opened from this terminal. */
    public record BestelVraag(BlockPos terminal) implements CustomPacketPayload {
        public static final Type<BestelVraag> TYPE = new Type<>(Guhs.id("einde2_trap5_machines_vraag"));
        public static final StreamCodec<FriendlyByteBuf, BestelVraag> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, BestelVraag::terminal,
                BestelVraag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(BestelVraag payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer p) || !dichtbij(p, payload.terminal())
                        || !(p.level().getBlockEntity(payload.terminal()) instanceof Terminal.BlockEntityImpl t) || t.bank() == null) {
                    return;
                }
                List<Bestelbalie.BlockEntityImpl> balies = Bestelbalie.van(p.level().getServer(), t.bank());
                if (balies.isEmpty()) {
                    p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("block.guhs.guhterminal.geen_balie")
                            .withStyle(net.minecraft.ChatFormatting.GOLD));
                    return;
                }
                stuurLijst(p, payload.terminal(), true, balies, t.bank());
            });
        }
    }

    /** An order from the list: {@code bron} is the terminal or the balie the list came from. */
    public record Bestel(BlockPos bron, boolean terminal, ItemStack wat, int aantal) implements CustomPacketPayload {
        public static final Type<Bestel> TYPE = new Type<>(Guhs.id("einde2_trap5_machines_bestel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Bestel> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Bestel::bron,
                ByteBufCodecs.BOOL, Bestel::terminal,
                ItemStack.STREAM_CODEC, Bestel::wat,
                ByteBufCodecs.VAR_INT, Bestel::aantal,
                Bestel::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Bestel payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer p) || !dichtbij(p, payload.bron()) || payload.aantal() <= 0) {
                    return;
                }
                int aantal = Math.min(payload.aantal(), 9999);
                List<Bestelbalie.BlockEntityImpl> balies;
                if (payload.terminal()) {
                    if (!(p.level().getBlockEntity(payload.bron()) instanceof Terminal.BlockEntityImpl t) || t.bank() == null || !t.heeftKracht()) {
                        return;
                    }
                    balies = Bestelbalie.van(p.level().getServer(), t.bank());
                } else {
                    if (!(p.level().getBlockEntity(payload.bron()) instanceof Bestelbalie.BlockEntityImpl b)) {
                        return;
                    }
                    balies = List.of(b);
                }
                // the first free balie whose machines make it (a busy one says so if none is free)
                Bestelbalie.BlockEntityImpl kies = null, bezig = null;
                for (Bestelbalie.BlockEntityImpl b : balies) {
                    if (!(b.getLevel() instanceof ServerLevel level) || b.kan(level).stream().noneMatch(s -> ItemStack.isSameItemSameComponents(s, payload.wat()))) {
                        continue;
                    }
                    if (b.bezigMetBestelling()) {
                        bezig = b;
                    } else {
                        kies = b;
                        break;
                    }
                }
                Bestelbalie.BlockEntityImpl doel = kies != null ? kies : bezig;
                if (doel != null) {
                    doel.bestel(p, payload.wat().copyWithCount(1), aantal);
                }
            });
        }
    }

    public record PoortActie(BlockPos poort, int actie, String tekst) implements CustomPacketPayload {
        public static final int SPRING = 0, HERNOEM = 1, TOELATEN = 2, WEIGEREN = 3, ICOON = 4;
        public static final Type<PoortActie> TYPE = new Type<>(Guhs.id("einde2_trap5_machines_poort_actie"));
        public static final StreamCodec<FriendlyByteBuf, PoortActie> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, PoortActie::poort,
                ByteBufCodecs.VAR_INT, PoortActie::actie, ByteBufCodecs.stringUtf8(64), PoortActie::tekst, PoortActie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(PoortActie payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer p && dichtbij(p, payload.poort())
                        && p.level().getBlockEntity(payload.poort()) instanceof Thuispoort.BlockEntityImpl t) {
                    t.actie(p, payload.actie(), payload.tekst());
                }
            });
        }
    }

    static boolean dichtbij(ServerPlayer p, BlockPos pos) {
        return p.position().distanceToSqr(Vec3.atCenterOf(pos)) <= 64;
    }
}
