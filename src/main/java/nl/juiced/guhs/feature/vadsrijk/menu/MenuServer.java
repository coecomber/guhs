package nl.juiced.guhs.feature.vadsrijk.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.karakter.Karakter;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPayloads;
import nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart;

/**
 * U-TABS, the server side of the Vadsrijk-menu: the data of the Karakter tab (level, class, all class levels, stats, gear incl.
 * the virtual slots) and the Reiskaart to LOOK at (no {@code Reiskaart.open}, so a click on the map never flies).
 * Client -> server {@link Vraag}("karakter" | "reiskaart"); server -> client {@link Antwoord}(wat, data, items).
 * <p>Other slices can add lines to the Karakter tab: {@link #KARAKTER_EXTRA} (put a ListTag "extra" of {k: lang key, a: arg}
 * compounds, e.g. the specialisation or the guild).
 */
public final class MenuServer {
    public record Vraag(String wat) implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("vr_menu_vraag"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Vraag> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Vraag::wat, Vraag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Antwoord(String wat, CompoundTag data, List<ItemStack> items) implements CustomPacketPayload {
        public static final Type<Antwoord> TYPE = new Type<>(Guhs.id("vr_menu_antwoord"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Antwoord> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Antwoord::wat,
                ByteBufCodecs.COMPOUND_TAG, Antwoord::data, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Antwoord::items, Antwoord::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client. */
    public static Consumer<Antwoord> ontvanger = a -> {
    };

    /** Extra lines for the Karakter tab (other slices): add compounds {k, a} to {@code data.getList("extra")}. */
    public static final List<BiConsumer<ServerPlayer, CompoundTag>> KARAKTER_EXTRA = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** The gear slots of the Karakter tab, in screen order (weapon + armour left, the virtual slots right). */
    public static final Slot[] GEAR = {Slot.WAPEN, Slot.HOOFD, Slot.BORST, Slot.BENEN, Slot.VOETEN, Slot.RING, Slot.RING2, Slot.KETTING, Slot.OORBEL,
            Slot.ARMBAND, Slot.HANDSCHOENEN, Slot.CAPE};

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private MenuServer() {
    }

    /** The tab ids for the command's suggestions (the client registry is the real list; other slices add theirs). */
    public static final List<String> TABS = new java.util.concurrent.CopyOnWriteArrayList<>(List.of("karakter", "skillboom", "questlog", "voltooiing",
            "rijdieren", "zeilbootje", "huisdieren", "kledingkast", "knapzak", "materiaaltas", "ambachten", "puzzels", "ridders", "reiskaart", "instellingen"));

    public static void register(IEventBus modBus) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MenuServer::commando);
    }

    /** {@code /guhs vadsrijk menu [tab]}: opens the Vadsrijk-menu (on that tab), like the H key (for everyone, it only opens a screen). */
    private static void commando(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        var m = net.minecraft.commands.Commands.literal("menu").executes(c -> {
            open(c.getSource().getPlayerOrException(), "");
            return 1;
        });
        m.then(net.minecraft.commands.Commands.argument("tab", com.mojang.brigadier.arguments.StringArgumentType.word())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(TABS, b))
                .executes(c -> {
                    open(c.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "tab"));
                    return 1;
                }));
        event.getDispatcher().register(nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando.wortel(m));
    }

    /** Tells the client to open the menu on a tab ("" = the last one). */
    public static void open(ServerPlayer p, String tab) {
        CompoundTag t = new CompoundTag();
        t.putString("tab", tab);
        PacketDistributor.sendToPlayer(p, new Antwoord("open", t, List.of()));
    }

    public static void payloads(PayloadRegistrar r) {
        r.playToServer(Vraag.TYPE, Vraag.STREAM_CODEC, (m, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                c.enqueueWork(() -> beantwoord(sp, m.wat()));
            }
        });
        r.playToClient(Antwoord.TYPE, Antwoord.STREAM_CODEC, (m, c) -> ontvanger.accept(m));
    }

    static void beantwoord(ServerPlayer p, String wat) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        switch (wat) {
            case "karakter" -> PacketDistributor.sendToPlayer(p, new Antwoord(wat, karakter(p), gear(p)));
            case "reiskaart" -> PacketDistributor.sendToPlayer(p, new Antwoord(wat, reiskaart(p), List.of()));
            default -> {
            }
        }
    }

    /** The Karakter tab: who you are, levels, stats (all non-zero, main stats first). */
    public static CompoundTag karakter(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        SpelerData d = Spelers.data(p);
        t.putString("naam", Karakter.naam(p));
        t.putString("modus", Karakter.modus(p).naamKey());
        t.putBoolean("pensioen", Karakter.gepensioneerd(p));
        int lv = d.level();
        t.putInt("level", lv);
        long gx = d.generiekXp;
        t.putLong("xp", gx - Levels.totaalVoorGeneriek(lv));
        t.putLong("xpNodig", lv >= Levels.MAX_GENERIEK ? 0 : Levels.xpVoorGeneriek(lv));
        if (nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.opPlafond(p, d)) {   // X-ZONE1: the soft cap: "hoogste level voor nu" + the bank
            t.putLong("xpNodig", 0);
            t.putString("maxKey", "vadsrijk.zonepoort.menu.max");
            t.putLong("gespaard", nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.gespaard(d, null));
            t.putInt("volgendeZone", nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.open() + 1);
        }
        Klasse k = Spelers.klasse(p);
        t.putString("klasse", k.naamKey());
        t.putString("titel", k.titelKey(d.level(k.id())));
        t.putInt("klasseLevel", d.level(k.id()));
        ListTag kl = new ListTag();
        for (Klasse o : Klassen.alle()) {
            CompoundTag c = new CompoundTag();
            c.putString("naam", o.naamKey());
            c.putInt("lv", d.level(o.id()));
            c.putBoolean("actief", o.id().equals(k.id()));
            kl.add(c);
        }
        t.put("klassen", kl);
        t.putLong("munten", d.munten);
        t.putDouble("wapenSchade", Spelers.wapenSchade(p));
        Stats s = Spelers.stats(p);
        ListTag sl = new ListTag();
        for (Stat st : Stat.values()) {
            double v = s.get(st);
            if (Math.abs(v) < 0.005 && !st.hoofd()) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putString("k", st.key());
            c.putDouble("v", v);
            c.putBoolean("p", st.procent);
            c.putBoolean("h", st.hoofd());
            sl.add(c);
        }
        t.put("stats", sl);
        t.put("extra", new ListTag());
        for (BiConsumer<ServerPlayer, CompoundTag> f : KARAKTER_EXTRA) {
            try {
                f.accept(p, t);
            } catch (RuntimeException e) {
                LOG.warn("[vadsrijk menu] karakter extra failed", e);
            }
        }
        return t;
    }

    /** The worn gear in {@link #GEAR} order. */
    public static List<ItemStack> gear(ServerPlayer p) {
        List<ItemStack> l = new ArrayList<>();
        for (Slot s : GEAR) {
            if (s == Slot.WAPEN) {
                l.add(p.getMainHandItem().copy());
            } else if (s.vanilla != null) {
                l.add(p.getItemBySlot(s.vanilla).copy());
            } else {
                l.add(Sieraden.get(p, s).copy());
            }
        }
        return l;
    }

    /** The travel map as data (the client builds an {@link NpcPayloads.Reiskaart} from it and shows it, look-only). */
    public static CompoundTag reiskaart(ServerPlayer p) {
        NpcPayloads.Reiskaart k = Reiskaart.bouw(p);
        CompoundTag t = new CompoundTag();
        t.putString("hier", k.hier());
        ListTag l = new ListTag();
        for (NpcPayloads.ReisPlek pl : k.plekken()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", pl.id());
            c.putInt("staat", pl.staat());
            c.putString("reden", pl.reden());
            c.putInt("arg", pl.arg());
            l.add(c);
        }
        t.put("plekken", l);
        return t;
    }

    /** (client) back from {@link #reiskaart}. */
    public static NpcPayloads.Reiskaart reiskaartVan(CompoundTag t) {
        List<NpcPayloads.ReisPlek> l = new ArrayList<>();
        ListTag pl = t.getListOrEmpty("plekken");
        for (int i = 0; i < pl.size(); i++) {
            CompoundTag c = pl.getCompoundOrEmpty(i);
            l.add(new NpcPayloads.ReisPlek(c.getStringOr("id", ""), c.getIntOr("staat", 0), c.getStringOr("reden", ""), c.getIntOr("arg", 0)));
        }
        return new NpcPayloads.Reiskaart(t.getStringOr("hier", ""), l);
    }

}
