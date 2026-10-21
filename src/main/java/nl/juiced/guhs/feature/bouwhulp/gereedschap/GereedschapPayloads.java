package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Blauwdrukken;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;

/**
 * F9 on the wire. Server to client ({@code guhs:bouwhulp_gereedschap}): "instel" (a tool's settings screen, open or
 * refresh), "staf" (the vormstaf's settings, for the radial menu and the preview), "vastleg" (the capture frame screen;
 * {@code Klaar}: close it), "bureau" (the Ontwerpbureau's blueprint browser). Client to server
 * ({@code guhs:bouwhulp_doe}): "optie" (a setting changed), "staf" (please send the staf's settings), "vastleg" (a button
 * of the frame screen), "bureau" (a button of the browser).
 */
public final class GereedschapPayloads {
    /** Client hook (set by client.GereedschapClient). */
    public static volatile Consumer<Data> ontvang = d -> {
    };

    public record Data(String soort, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(Guhs.id("bouwhulp_gereedschap"));
        public static final StreamCodec<FriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), Data::soort,
                ByteBufCodecs.COMPOUND_TAG, Data::data, Data::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Doe(String soort, String gereedschap, String key, int waarde, String tekst) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("bouwhulp_doe"));
        public static final StreamCodec<FriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), Doe::soort,
                ByteBufCodecs.stringUtf8(32), Doe::gereedschap, ByteBufCodecs.stringUtf8(32), Doe::key, ByteBufCodecs.VAR_INT, Doe::waarde,
                ByteBufCodecs.stringUtf8(64), Doe::tekst, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Doe d, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer p)) {
                return;
            }
            switch (d.soort()) {
                case "optie" -> {
                    Gereedschap g = Gereedschap.van(d.gereedschap());
                    BouwNiveau n = niveauInHand(p, g);
                    Opties.zet(g, p, n, d.key(), d.waarde(), d.tekst());
                    CompoundTag t = Opties.schrijf(g, p, n, null, -1);
                    stuur(p, "instel", t);
                    if (g == Gereedschap.VORMSTAF) {
                        stuurStaf(p);
                    }
                }
                case "staf" -> stuurStaf(p);
                case "vastleg" -> VastlegDienst.doe(p, d.waarde() >> 8, d.waarde() & 255, d.tekst());
                case "bureau" -> Blauwdrukken.doeBureau(p, d.waarde() >> 8, d.waarde() & 255, d.tekst());
                default -> {
                }
            }
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Data.TYPE, Data.STREAM_CODEC, (d, c) -> ontvang.accept(d));
        registrar.playToServer(Doe.TYPE, Doe.STREAM_CODEC, Doe::handle);
    }

    public static void stuur(ServerPlayer p, String soort, CompoundTag t) {
        PacketDistributor.sendToPlayer(p, new Data(soort, t));
    }

    /** Opens a tool's settings screen. */
    public static void openInstellingen(ServerPlayer p, Gereedschap g, BouwNiveau n) {
        CompoundTag t = Opties.schrijf(g, p, n, null, -1);
        t.putBoolean("Open", true);
        stuur(p, "instel", t);
    }

    /** The vormstaf's settings and level (for the radial menu and the preview ghost). */
    public static void stuurStaf(ServerPlayer p) {
        BouwNiveau n = niveauInHand(p, Gereedschap.VORMSTAF);
        CompoundTag t = Opties.schrijf(Gereedschap.VORMSTAF, p, n, null, -1);
        t.put("Instelling", Opties.van(p, Gereedschap.VORMSTAF).copy());
        stuur(p, "staf", t);
    }

    public static void sluitVastleg(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Klaar", true);
        stuur(p, "vastleg", t);
    }

    /** The level of the tool in the player's hand (Handwerk when not holding it). */
    public static BouwNiveau niveauInHand(ServerPlayer p, Gereedschap g) {
        for (ItemStack s : new ItemStack[] {p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof GereedschapItem gi && gi.soort == g) {
                return GereedschapItem.niveau(p.level(), s);
            }
        }
        return BouwNiveau.HANDWERK;
    }

    private GereedschapPayloads() {
    }
}
