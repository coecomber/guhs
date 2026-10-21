package nl.juiced.guhs.feature.safari.verblijf;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * Op Safari! step 2 (S5): the verblijfsbord menu's messages: server to client the board's state ({@code guhs:verblijf_open},
 * opens or refreshes the screen), client to server a button ({@code guhs:verblijf_doe}). Changing things is for the owner (or
 * an op) within 16 blocks; anybody may look.
 */
public final class VerblijfPayloads {
    /** Client: opens/refreshes the screen (set by client.VerblijfClient). */
    public static volatile Consumer<CompoundTag> opener = t -> {
    };

    /** (Sent by ordinal: append only.) */
    public enum Actie { VERVERS, NAAM, TERUG, VERSTOP, STRAAL, VORM, BREEDTE, DIEPTE, RAND, SLAAPPLEK, SLAAPPLEK_WEG, CADEAUS, BEZORG_KIES, BEZORG_STUUR,
        /** Deel 2 (F10b): the Bouwopdracht tab, in the order of Bouwopdracht.Knop. */
        BOUW_KIES, BOUW_VARIANT, BOUW_ZOEK, BOUW_WIJS, BOUW_HERBEREKEN, BOUW_BEDIEN, BOUW_STOP, BOUW_VERBLIJF }

    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("verblijf_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            opener.accept(p.data());
        }
    }

    public record Doe(BlockPos pos, int actie, int waarde, String tekst) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("verblijf_doe"));
        public static final StreamCodec<FriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Doe::pos, ByteBufCodecs.VAR_INT, Doe::actie, ByteBufCodecs.VAR_INT, Doe::waarde,
                ByteBufCodecs.stringUtf8(64), Doe::tekst, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Doe p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                doe(sp, p);
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Doe.TYPE, Doe.STREAM_CODEC, Doe::handle);
    }

    /** Sends the board's state to this player (opening or refreshing the menu). */
    public static void open(ServerPlayer p, VerblijfsbordBlockEntity be) {
        if (!(p.level() instanceof ServerLevel level)) {
            return;
        }
        be.herreken(level);
        PacketDistributor.sendToPlayer(p, new Open(staat(p, level, be)));
    }

    static CompoundTag staat(ServerPlayer p, ServerLevel level, VerblijfsbordBlockEntity be) {
        CompoundTag t = new CompoundTag();
        String soort = be.soort();
        t.putLong("Pos", be.getBlockPos().asLong());
        t.putString("Soort", soort);
        t.putString("Eigenaar", be.eigenaarNaam());
        t.putBoolean("Mag", be.magBewerken(p));
        t.putInt("Straal", be.straal());
        t.putBoolean("Rechthoek", be.rechthoek());
        t.putInt("Breedte", be.breedte());
        t.putInt("Diepte", be.diepte());
        if (be.slaapplek() != null) {
            t.putLong("Slaapplek", be.slaapplek().asLong());
        }
        ListTag dieren = new ListTag();
        for (int i = 0; i < VerblijfsbordBlockEntity.FAMILIE.length; i++) {
            CompoundTag d = new CompoundTag();
            d.putString("Rol", VerblijfsbordBlockEntity.FAMILIE[i]);
            d.putString("Naam", be.naam(i));
            UUID id = be.lid(i);
            if (id != null && level.getEntity(id) instanceof ThuisDier dier) {
                d.putInt("Entity", dier.getId());
                d.putString("Stemming", be.stemming(dier));
                d.putBoolean("Weg", dier.weg());
            } else {
                d.putInt("Entity", -1);
                d.putString("Stemming", "");
            }
            dieren.add(d);
        }
        t.put("Dieren", dieren);
        t.put("Tevreden", be.uitslag().copy());
        VerblijfData.Dier info = VerblijfData.dier(soort);
        if (info != null) {
            ListTag voer = new ListTag();
            info.voer().forEach(v -> voer.add(StringTag.valueOf(v)));
            t.put("Voer", voer);
            ListTag taken = new ListTag();
            info.taken().forEach(v -> taken.add(StringTag.valueOf(v)));
            t.put("Taken", taken);
            t.putString("Speeltje", info.speeltje());
            t.putBoolean("Bezorger", info.bezorger());
            t.putBoolean("Rijdier", info.rijdier());
        }
        ListTag cadeaus = new ListTag();
        for (int i = 0; i < be.cadeaus().getContainerSize(); i++) {
            ItemStack s = be.cadeaus().getItem(i);
            CompoundTag c = new CompoundTag();
            c.putString("Id", s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            c.putInt("N", s.getCount());
            cadeaus.add(c);
        }
        t.put("Cadeaus", cadeaus);
        t.putInt("Sterren", SafariSpeler.dex(p, soort).sterren());
        t.put("Bouw", nl.juiced.guhs.feature.bouwhulp.helpers.Bouwopdracht.staat(level, be));
        t.putLong("Cooldown", Math.max(0, SafariSpeler.bordVrijOp(level.getServer(), p.getUUID(), soort) - Klok.nu()));
        if (info != null && info.bezorger()) {
            ListTag vrienden = new ListTag();
            for (Map.Entry<UUID, String> e : Ooievaar.Bussen.get(level.getServer()).anderen(be.eigenaar()).entrySet()) {
                CompoundTag v = new CompoundTag();
                v.putString("Id", e.getKey().toString());
                v.putString("Naam", e.getValue());
                vrienden.add(v);
            }
            t.put("Vrienden", vrienden);
            t.putString("Bestemming", be.bestemming == null ? "" : be.bestemming.toString());
            t.putBoolean("Onderweg", be.aankomst >= 0);
        }
        return t;
    }

    static void doe(ServerPlayer p, Doe d) {
        if (!(p.level() instanceof ServerLevel level) || !(level.getBlockEntity(d.pos()) instanceof VerblijfsbordBlockEntity be)
                || p.distanceToSqr(d.pos().getX() + 0.5, d.pos().getY() + 0.5, d.pos().getZ() + 0.5) > 16 * 16) {
            return;
        }
        Actie a = d.actie() >= 0 && d.actie() < Actie.values().length ? Actie.values()[d.actie()] : Actie.VERVERS;
        if (a != Actie.VERVERS && !be.magBewerken(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.niet_jouw", be.eigenaarNaam()).withStyle(ChatFormatting.RED));
            open(p, be);
            return;
        }
        switch (a) {
            case VERVERS -> {
            }
            case NAAM -> be.hernoem(level, d.waarde(), d.tekst().length() > 24 ? d.tekst().substring(0, 24) : d.tekst());
            case TERUG -> be.terugroepen(level);
            case VERSTOP -> be.verstoppen(level);
            case STRAAL -> be.zetStraal(level, d.waarde());
            case VORM -> be.zetVorm(level, d.waarde() != 0);
            case BREEDTE -> be.zetBreedte(level, d.waarde());
            case DIEPTE -> be.zetDiepte(level, d.waarde());
            case RAND -> be.randTonen(p, 400);
            case SLAAPPLEK -> {
                BlockPos hier = p.blockPosition();
                if (be.binnen(hier.getX(), hier.getZ()) && Math.abs(hier.getY() - be.getBlockPos().getY()) <= 8) {
                    be.zetSlaapplek(hier);
                } else {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.thuisplek.te_ver").withStyle(ChatFormatting.RED));
                }
            }
            case SLAAPPLEK_WEG -> be.zetSlaapplek(null);
            case CADEAUS -> {
                for (int i = 0; i < be.cadeaus().getContainerSize(); i++) {
                    ItemStack s = be.cadeaus().getItem(i);
                    if (!s.isEmpty()) {
                        be.cadeaus().setItem(i, ItemStack.EMPTY);
                        if (!p.getInventory().add(s)) {
                            p.drop(s, false);
                        }
                    }
                }
                be.setChanged();
            }
            case BEZORG_KIES -> {
                try {
                    be.bestemming = d.tekst().isEmpty() ? null : UUID.fromString(d.tekst());
                } catch (IllegalArgumentException e) {
                    be.bestemming = null;
                }
                be.setChanged();
            }
            case BOUW_KIES, BOUW_VARIANT, BOUW_ZOEK, BOUW_WIJS, BOUW_HERBEREKEN, BOUW_BEDIEN, BOUW_STOP, BOUW_VERBLIJF ->
                    nl.juiced.guhs.feature.bouwhulp.helpers.Bouwopdracht.doe(p, level, be,
                            nl.juiced.guhs.feature.bouwhulp.helpers.Bouwopdracht.Knop.values()[a.ordinal() - Actie.BOUW_KIES.ordinal()], d.waarde());
            case BEZORG_STUUR -> {
                Ooievaar.Fout f = Ooievaar.stuur(level, be);
                if (f != null) {
                    p.sendOverlayMessage(Component.translatable(switch (f) {
                        case GEEN_KIST, GEEN_STORK -> "gui.guhs.verblijf.bezorgen.geen_kist";
                        case GEEN_BUS -> "gui.guhs.verblijf.bezorgen.geen_bus";
                        case ONDERWEG -> "gui.guhs.verblijf.bezorgen.onderweg";
                    }).withStyle(ChatFormatting.RED));
                }
            }
        }
        open(p, be);
    }

    private VerblijfPayloads() {
    }
}
