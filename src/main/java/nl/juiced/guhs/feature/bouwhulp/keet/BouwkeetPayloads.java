package nl.juiced.guhs.feature.bouwhulp.keet;

import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bouwhulp.BouwhulpFeature;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.bouwhulp.kern.Spoken;

/**
 * The Bouwkeet menu on the wire: server to client the whole menu ({@code guhs:bouwkeet_open}: the keet, its project and
 * the open tab's page), client to server a button ({@code guhs:bouwkeet_doe}: tab "" is the menu itself, else the tab's
 * own {@link BouwkeetTabs.Tab#doe}). Looking is for everybody within 12 blocks; changing for who may
 * ({@link BouwkeetBlock#magBewerken}).
 */
public final class BouwkeetPayloads {
    /** Client: opens/refreshes the menu (set by client.BouwhulpClient). */
    public static volatile Consumer<CompoundTag> opener = t -> {
    };

    /** The menu's own buttons (tab ""; sent by ordinal: append only). */
    public enum Actie { KIES, AAN, BEDIENING, BOUW, ALSNOG, PAUZE, TERUG, LOSLATEN, ANNULEER,
        /** Test round 5: the missing blocks as a written book. */
        LIJSTJE }

    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("bouwkeet_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Open p, IPayloadContext context) {
            opener.accept(p.data());
        }
    }

    public record Doe(BlockPos pos, String tab, int actie, int waarde, String tekst) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("bouwkeet_doe"));
        public static final StreamCodec<FriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Doe::pos,
                ByteBufCodecs.stringUtf8(32), Doe::tab, ByteBufCodecs.VAR_INT, Doe::actie, ByteBufCodecs.VAR_INT, Doe::waarde,
                ByteBufCodecs.stringUtf8(256), Doe::tekst, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Doe p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                doe(sp, p);
            }
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Doe.TYPE, Doe.STREAM_CODEC, Doe::handle);
    }

    /** Sends the menu with this tab open (null: the first tab). */
    public static void open(ServerPlayer p, BouwkeetBlockEntity keet, @Nullable String tab) {
        ServerLevel level = p.level();
        CompoundTag t = new CompoundTag();
        BlockPos pos = keet.getBlockPos();
        t.putLong("Pos", pos.asLong());
        t.putString("Eigenaar", keet.eigenaar() == null ? "" : naam(level, keet.eigenaar()));
        t.putBoolean("Mag", BouwkeetBlock.magBewerken(keet, p));
        t.putInt("Niveau", keet.niveau().nummer());
        t.putInt("WerkNiveau", keet.werkNiveau().nummer());
        t.putBoolean("Aan", keet.aanGezet());
        t.putBoolean("Kracht", keet.heeftKracht());
        t.putBoolean("Bank", keet.bank() != null);
        // the project
        Spook s = keet.project();
        if (s != null) {
            CompoundTag pr = new CompoundTag();
            pr.putString("Naam", s.naam());
            pr.putInt("Status", s.status().ordinal());
            pr.putInt("Gedaan", s.gedaan());
            pr.putInt("Totaal", s.totaal());
            pr.putInt("Rest", s.nogTeGaan());
            var g = s.ontwerp().grootte();
            pr.putIntArray("Grootte", new int[] {g.getX(), g.getY(), g.getZ()});
            pr.put("Tekort", items(Spookbouw.tekort(p, s)));
            pr.putInt("Ongedaan", Spoken.van(level).ongedaanTeMaken(p.getUUID()));
            pr.putBoolean("Pof", keet.werkNiveau().pof());
            nl.juiced.guhs.feature.bouwhulp.kern.SpookPayloads.bereik(level, s, pr);                         // test round 5: the reach, blocks outside it
            pr.putDouble("Tempo", keet.tempo(level, s));               // (for "nog ~2 min")
            t.put("Project", pr);
        }
        // the tabs and the open one's page
        ListTag tabs = new ListTag();
        String open = tab;
        for (BouwkeetTabs.Tab x : BouwkeetTabs.lijst()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", x.id());
            c.putInt("Vanaf", x.vanaf().nummer());
            tabs.add(c);
            if (open == null) {
                open = x.id();
            }
        }
        t.put("Tabs", tabs);
        BouwkeetTabs.Tab x = open == null ? null : BouwkeetTabs.get(open);
        if (x != null) {
            t.putString("Open", x.id());
            CompoundTag pagina = new CompoundTag();
            x.schrijf(p, level, keet, pagina);
            t.put("Tab", pagina);
        }
        PacketDistributor.sendToPlayer(p, new Open(t));
    }

    static void doe(ServerPlayer p, Doe d) {
        ServerLevel level = p.level();
        if (!d.pos().closerThan(p.blockPosition(), 12) || !(level.getBlockEntity(d.pos()) instanceof BouwkeetBlockEntity keet)) {
            return;
        }
        boolean mag = BouwkeetBlock.magBewerken(keet, p);
        String naarTab = d.tab().isEmpty() ? null : d.tab();
        if (d.tab().isEmpty()) {
            Actie a = Actie.values()[Math.floorMod(d.actie(), Actie.values().length)];
            Spook s = keet.project();
            switch (a) {
                case KIES -> naarTab = d.tekst();
                case AAN -> {
                    if (mag) {
                        keet.zetAan(d.waarde() != 0);
                    }
                }
                case BEDIENING -> {
                    if (s != null) {
                        Spookbouw.toonBediening(p, s);
                        return;
                    }
                }
                case BOUW, ALSNOG -> {
                    if (mag && s != null) {
                        Spoken.Bouwen b = Spookbouw.bouw(p, s, a == Actie.ALSNOG);
                        if (!b.begonnen()) {
                            Spookbouw.toonBediening(p, s);   // (the panel asks "alsnog bouwen?" with the list)
                            return;
                        }
                    }
                }
                case PAUZE -> {
                    if (mag && s != null) {
                        Spookbouw.pauzeer(level, s);
                    }
                }
                case TERUG -> {
                    if (!Spookbouw.ongedaan(p)) {
                        p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.terug.niets"));
                    }
                }
                case LOSLATEN -> {
                    if (mag) {
                        keet.laatProjectLos(level);
                    }
                }
                case ANNULEER -> {
                    if (mag && s != null && s.mag(p)) {
                        Spookbouw.annuleer(level, s);
                    }
                }
                case LIJSTJE -> {
                    if (s != null) {
                        nl.juiced.guhs.feature.bouwhulp.kern.Lijstje.geef(p, s.naam().isEmpty()
                                ? Component.translatable("gui.guhs.bouwhulp.project").getString() : s.naam(), Spookbouw.tekort(p, s));
                    }
                }
            }
        } else {
            BouwkeetTabs.Tab tab = BouwkeetTabs.get(d.tab());
            if (tab != null && mag && keet.werkNiveau().compareTo(tab.vanaf()) >= 0) {
                tab.doe(p, level, keet, d.actie(), d.waarde(), d.tekst());
            }
        }
        open(p, keet, naarTab);
    }

    public static ListTag items(Map<Item, Integer> items) {
        ListTag l = new ListTag();
        items.entrySet().stream().sorted(Map.Entry.<Item, Integer>comparingByValue().reversed()).forEach(e -> {
            CompoundTag c = new CompoundTag();
            c.putString("Id", BuiltInRegistries.ITEM.getKey(e.getKey()).toString());
            c.putInt("N", e.getValue());
            l.add(c);
        });
        return l;
    }

    static String naam(ServerLevel level, java.util.UUID id) {
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
        if (p != null) {
            return p.getGameProfile().name();
        }
        return BouwhulpFeature.naamVan(level.getServer(), id);
    }

    private BouwkeetPayloads() {
    }
}
