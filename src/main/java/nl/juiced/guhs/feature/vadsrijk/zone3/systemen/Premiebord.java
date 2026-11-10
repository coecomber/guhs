package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The board screen in the Jagershut (one screen, three tabs): "premies" (the 3 daily bounties, claim + the free swap),
 * "jacht" (today's 2 hunting contracts) and "winkel" (the penningwinkel). API for z3-npc: {@link #open}(p, tab) on the
 * Premiebord / Jachtbord / Jagersguh. Dev: {@code /guhs vadsrijk z3systemen bord [tab]}.
 */
public final class Premiebord {
    public record Scherm(CompoundTag data, List<ItemStack> items) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("vr_z3_systemen_bord"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Scherm::data, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Scherm::items, Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Doe(String actie, CompoundTag args) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("vr_z3_systemen_bord_doe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Doe::actie, ByteBufCodecs.COMPOUND_TAG, Doe::args, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static final String PREMIES = "premies", JACHT = "jacht", WINKEL = "winkel";
    /** Set by the client class. */
    public static Consumer<Scherm> ontvanger = s -> {
    };

    private Premiebord() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, (p, c) -> ontvanger.accept(p));
        r.playToServer(Doe.TYPE, Doe.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                doe(sp, p.actie(), p.args());
            }
        });
    }

    public static void open(ServerPlayer p, String tab) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        stuur(p, tab, "");
    }

    static void doe(ServerPlayer p, String actie, CompoundTag a) {
        if (!Vadsrijk.in(p) || !nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).binnen) {
            return;   // Y-SCHEIDING: a packet from outside paid Vadsrijk rewards (gear, premiekist) into the survival inventory
        }
        String tab = a.getStringOr("tab", PREMIES);
        String melding = switch (actie) {
            case "claim" -> Premies.claim(p, a.getIntOr("slot", -1));
            case "ruil" -> Premies.ruil(p, a.getIntOr("slot", -1));
            case "contract" -> Contracten.neem(p, a.getStringOr("id", ""));
            case "koop" -> Premies.koop(p, a.getStringOr("id", ""));
            case "tab" -> "";
            default -> null;
        };
        if (melding != null) {
            stuur(p, tab, melding);
        }
    }

    /** The screen data (also used by the gametest). */
    static CompoundTag data(ServerPlayer p, String tab, List<ItemStack> items) {
        CompoundTag d = new CompoundTag();
        d.putString("tab", tab);
        d.putLong("munten", Spelers.data(p).munten);
        d.putInt("penningen", Z3Data.penningen(p));
        d.putInt("veren", Z3Data.veren(p));
        d.putInt("verenNodig", GoudenVeren.NODIG);
        d.putInt("level", Spelers.level(p));
        d.putInt("minLevel", Premies.MIN_LEVEL);
        d.putInt("contractLevel", Contracten.MIN_LEVEL);
        d.putLong("reset", Z3Data.secondenTotReset());
        CompoundTag t = Premies.vandaag(p);
        d.putBoolean("geruild", t.getBooleanOr("geruild", false));
        d.putInt("weekN", t.getIntOr("weekN", 0));
        d.putInt("weekDoel", Premies.WEEKDOEL);
        d.putBoolean("weekKist", t.getBooleanOr("weekKist", false));
        ListTag l = new ListTag();
        List<String> ids = Premies.ids(p);
        for (int i = 0; i < ids.size(); i++) {
            Premies.Premie x = Premies.POOL.get(ids.get(i));
            if (x == null) {
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putString("key", x.naamKey());
            e.putInt("aantal", x.aantal());
            e.putInt("voortgang", Premies.voortgang(p, i));
            e.putBoolean("geclaimd", Premies.geclaimd(p, i));
            e.putBoolean("baas", x.baas());
            l.add(e);
        }
        d.put("premies", l);
        ListTag c = new ListTag();
        for (String id : Contracten.vandaag()) {
            CompoundTag e = new CompoundTag();
            e.putString("id", id);
            e.putString("key", Contracten.naamKey(id));
            c.add(e);
        }
        d.put("contracten", c);
        d.putString("actief", Contracten.actief(p));
        ListTag w = new ListTag();
        for (Premies.Waar x : Premies.WINKEL) {
            CompoundTag e = new CompoundTag();
            e.putString("id", x.id());
            e.putString("key", x.naamKey());
            e.putInt("prijs", x.prijs());
            e.putInt("item", items.size());
            items.add(x.item().get());
            w.add(e);
        }
        d.put("winkel", w);
        return d;
    }

    static void stuur(ServerPlayer p, String tab, String melding) {
        List<ItemStack> items = new ArrayList<>();
        CompoundTag d = data(p, tab, items);
        d.putString("melding", melding);
        PacketDistributor.sendToPlayer(p, new Scherm(d, items));
    }
}
