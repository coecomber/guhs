package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * The post screen of the Guh-parkour over the network:
 * <ul>
 *   <li>{@code guhs:guhparkour_scherm} (to the client): open / refresh the screen of one Startpaaltje ({@link #stand});</li>
 *   <li>{@code guhs:guhparkour_doe} (to the server): move a piece up or down, take it out, put a guh on the route or take
 *       it off, start laying out, wipe the scores, refresh. Everything is checked here: the player stands at the post,
 *       may edit it, the index exists, the guh is the player's own, near and free, at most four.</li>
 * </ul>
 */
public final class ParkourPayloads {
    public static final int OMHOOG = 0, OMLAAG = 1, WEG = 2, GUH_OP = 3, GUH_AF = 4, UITZETTEN = 5, WIS_SCORES = 6, VERVERS = 7;
    /** How near the player must stand to the post to use its screen. */
    private static final double BIJ = 8;
    private static final UUID GEEN = new UUID(0L, 0L);

    public static volatile Consumer<Scherm> schermOpener = p -> { };

    public record Scherm(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("guhparkour_scherm"));
        public static final StreamCodec<FriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Scherm::data, Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Scherm p, IPayloadContext context) {
            schermOpener.accept(p);
        }
    }

    public record Doe(BlockPos pos, int actie, int index, UUID id) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("guhparkour_doe"));
        public static final StreamCodec<FriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Doe::pos, ByteBufCodecs.VAR_INT,
                Doe::actie, ByteBufCodecs.VAR_INT, Doe::index, UUIDUtil.STREAM_CODEC, Doe::id, Doe::new);

        public Doe(BlockPos pos, int actie, int index) {
            this(pos, actie, index, GEEN);
        }

        public Doe(BlockPos pos, int actie, UUID id) {
            this(pos, actie, 0, id);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Doe p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                doe(sp, p.pos(), p.actie(), p.index(), p.id());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, Scherm::handle);
        registrar.playToServer(Doe.TYPE, Doe.STREAM_CODEC, Doe::handle);
    }

    public static void open(ServerPlayer p, StartpaalBlockEntity paal) {
        ModNetworking.sendTo(p, new Scherm(stand(p, paal, null)));
    }

    /** The post near enough to this player to use its screen, or null. */
    @Nullable
    private static StartpaalBlockEntity paal(ServerPlayer p, BlockPos pos) {
        if (!p.level().isLoaded(pos) || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > BIJ * BIJ) {
            return null;
        }
        return p.level().getBlockEntity(pos) instanceof StartpaalBlockEntity paal ? paal : null;
    }

    /** What the server makes of a screen action; returns the message it showed (null: none) for tests. */
    @Nullable
    public static Component doe(ServerPlayer p, BlockPos pos, int actie, int index, UUID id) {
        StartpaalBlockEntity paal = paal(p, pos);
        if (paal == null) {
            return null;
        }
        Component melding = null;
        if (actie != VERVERS && !paal.magBewerken(p)) {
            melding = Component.translatable("gui.guhs.guhparkour.niet_van_jou", paal.eigenaarNaam());
        } else {
            switch (actie) {
                case OMHOOG -> paal.verplaats(index, -1);
                case OMLAAG -> paal.verplaats(index, 1);
                case WEG -> paal.verwijder(index);
                case GUH_OP -> melding = guhOp(p, paal, id);
                case GUH_AF -> {
                    if (paal.haalAf(id)) {
                        melding = Component.translatable("gui.guhs.guhparkour.scherm.melding.af", naam(p, paal, id));
                    }
                }
                case UITZETTEN -> {
                    Uitzetten.start(p, paal);
                    return null;      // (the client closed the screen)
                }
                case WIS_SCORES -> {
                    paal.wisScores();
                    melding = Component.translatable("gui.guhs.guhparkour.scherm.melding.gewist");
                }
                default -> {
                }
            }
        }
        ModNetworking.sendTo(p, new Scherm(stand(p, paal, melding)));
        return melding;
    }

    /** Puts one of the player's guhs on the route; the message says how it went. */
    public static Component guhOp(ServerPlayer p, StartpaalBlockEntity paal, UUID id) {
        paal.ruimOp();
        if (paal.heeftGuh(id)) {
            return Component.translatable("gui.guhs.guhparkour.scherm.melding.al_op");
        }
        if (paal.guhs().size() >= Routes.MAX_GUHS) {
            return Component.translatable("gui.guhs.guhparkour.scherm.melding.vol", Routes.MAX_GUHS);
        }
        GuhEntity guh = GuhKiezer.zoek(p, id, Routes.BEREIK + BIJ);
        if (guh == null || !guh.blockPosition().closerThan(paal.getBlockPos(), Routes.BEREIK + 0.5)) {
            return Component.translatable("gui.guhs.guhparkour.scherm.uit.ver");
        }
        Component bezet = GuhKiezer.bezet(guh);
        if (bezet != null) {
            return bezet;
        }
        if (!paal.zetOp(guh)) {
            return Component.translatable("gui.guhs.guhparkour.scherm.melding.vol", Routes.MAX_GUHS);
        }
        return Component.translatable(paal.stukken().isEmpty() ? "gui.guhs.guhparkour.scherm.melding.op_leeg" : "gui.guhs.guhparkour.scherm.melding.op", guh.getName());
    }

    private static Component naam(ServerPlayer p, StartpaalBlockEntity paal, UUID id) {
        Entity e = ((ServerLevel) p.level()).getEntity(id);
        if (e != null) {
            return e.getName();
        }
        StartpaalBlockEntity.Score s = paal.scores().get(id);
        if (s != null) {
            return s.naam();
        }
        BandData.Rec r = BandData.get(p.level().getServer()).vindOveral(id);
        return r == null ? Component.translatable("gui.guhs.guhparkour.scherm.guh_onbekend") : r.weergave();
    }

    /** Everything the screen shows of this post, for this player. */
    public static CompoundTag stand(ServerPlayer p, StartpaalBlockEntity paal, @Nullable Component melding) {
        paal.ruimOp();
        ServerLevel level = (ServerLevel) p.level();
        CompoundTag t = new CompoundTag();
        t.putLong("Pos", paal.getBlockPos().asLong());
        t.putBoolean("Mag", paal.magBewerken(p));
        Tekst.put(t, "Eigenaar", paal.eigenaarNaam());
        List<BlockPos> stukken = paal.stukken();
        ListTag lijst = new ListTag();
        for (BlockPos pos : stukken) {
            CompoundTag s = new CompoundTag();
            Routes.Soort soort = Routes.soort(level, pos);
            Tekst.put(s, "Naam", Routes.naam(level, pos));
            ItemStack icoon = Routes.icoon(level, pos);
            if (!icoon.isEmpty()) {
                s.put("Icoon", Nbt.saveStack(level.registryAccess(), icoon));
            }
            s.putBoolean("Weg", soort == null);
            s.putBoolean("Finish", soort == Routes.Soort.FINISH);
            s.putBoolean("Hapert", paal.hapertBij(pos));
            lijst.add(s);
        }
        t.put("Stukken", lijst);
        t.putInt("Aantal", Routes.aantal(level, stukken));
        t.putInt("Max", Routes.MAX_STUKKEN);
        t.putInt("MaxGuhs", Routes.MAX_GUHS);
        t.putBoolean("Finish", Routes.heeftFinish(level, stukken));
        // the player's guhs, with why one cannot be put on (the ones on this route are picked to take them off)
        ListTag guhs = GuhKiezer.lijst(p, r -> true);
        for (int i = 0; i < guhs.size(); i++) {
            CompoundTag g = guhs.getCompoundOrEmpty(i);
            UUID id = g.read("Id", UUIDUtil.CODEC).orElse(GEEN);
            boolean op = paal.heeftGuh(id);
            g.putBoolean("Op", op);
            if (op) {
                continue;
            }
            Component uit = null;
            if (!(level.getEntity(id) instanceof GuhEntity guh) || !guh.isAlive() || !guh.blockPosition().closerThan(paal.getBlockPos(), Routes.BEREIK + 0.5)) {
                uit = Component.translatable("gui.guhs.guhparkour.scherm.uit.ver");
            } else if (guh.getPersistentData().contains(RouteGoal.PAAL)) {
                uit = Component.translatable("gui.guhs.guhparkour.scherm.uit.andere_route");
            } else {
                uit = GuhKiezer.bezet(guh);
            }
            if (uit != null) {
                Tekst.put(g, "Uit", uit);
            }
        }
        t.put("Guhs", guhs);
        // who runs here now, with their scores
        ListTag leden = new ListTag();
        for (UUID id : paal.guhs()) {
            CompoundTag l = new CompoundTag();
            l.store("Id", UUIDUtil.CODEC, id);
            Tekst.put(l, "Naam", naam(p, paal, id));
            StartpaalBlockEntity.Score s = paal.scores().get(id);
            l.putInt("Rondjes", s == null ? 0 : s.rondjes());
            l.putInt("Beste", s == null ? 0 : s.beste());
            l.putInt("Laatste", s == null ? 0 : s.laatste());
            leden.add(l);
        }
        t.put("Leden", leden);
        if (melding != null) {
            Tekst.put(t, "Melding", melding);
        }
        return t;
    }

    private ParkourPayloads() {
    }
}
