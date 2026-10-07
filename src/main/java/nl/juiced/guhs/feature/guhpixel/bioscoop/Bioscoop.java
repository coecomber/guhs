package nl.juiced.guhs.feature.guhpixel.bioscoop;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhpixel.Films;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.taal.Tekst;

/**
 * The rules of the Guhbioscoop (server side): what the projector screen shows a player ({@link #stand}: THEIR films, the
 * films are unlocked per player), starting and stopping a film (everything checked here, the client only asks), and what
 * a player has watched ({@link #gezien}; per player, in {@code PxData.deel(p, "bioscoop")}).
 * <p>
 * Texts per film (tools/features/guhpixel_bioscoop_tekst.py): {@code gui.guhs.guhbioscoop.film.<id>.naam}, {@code .uitleg}
 * (one line about it) and {@code .slot} (how to get it).
 */
public final class Bioscoop {
    /** The slice's name in PxData. */
    public static final String DEEL = "bioscoop";
    private static final String GEZIEN = "Gezien";
    /** How far from the projector a player may stand to work it. */
    public static final double BEDIEN = 8;
    /** Who stands or sits this close to the screen when the film ends has watched it. */
    public static final double KIJK = 24;
    public static final String ADV_EERSTE = "guhbioscoop_eerste_film", ADV_ALLES = "guhbioscoop_alles";

    public enum Uitkomst { OK, GEEN_PROJECTOR, TE_VER, ONBEKEND, OP_SLOT, GEEN_DOEK }

    public static MutableComponent naam(String film) {
        return Component.translatable("gui.guhs.guhbioscoop.film." + film + ".naam");
    }

    public static MutableComponent uitleg(String film) {
        return Component.translatable("gui.guhs.guhbioscoop.film." + film + ".uitleg");
    }

    /** How to get a film you do not have yet. */
    public static MutableComponent slot(String film) {
        return Component.translatable("gui.guhs.guhbioscoop.film." + film + ".slot");
    }

    // --- what a player has watched ------------------------------------------------------------------------------------------

    /** How often this player watched this film to the end. */
    public static int gezien(ServerPlayer p, String film) {
        return PxData.deel(p, DEEL).getCompoundOrEmpty(GEZIEN).getIntOr(film, 0);
    }

    /** How many different films this player has watched to the end. */
    public static int aantalGezien(ServerPlayer p) {
        int n = 0;
        for (String id : Films.IDS) {
            if (gezien(p, id) > 0) {
                n++;
            }
        }
        return n;
    }

    /** The film ran to its end while this player was there. */
    public static void markeerGezien(ServerPlayer p, String film) {
        CompoundTag gezien = PxData.sub(PxData.deel(p, DEEL), GEZIEN);
        gezien.putInt(film, gezien.getIntOr(film, 0) + 1);
        PxData.vuil(p.level().getServer());
        GuhAdvancements.grant(p, ADV_EERSTE);
        if (aantalGezien(p) >= Films.IDS.size()) {
            GuhAdvancements.grant(p, ADV_ALLES);
        }
    }

    /** (Dev) forgets what this player watched. */
    public static void wisGezien(ServerPlayer p) {
        PxData.deel(p, DEEL).remove(GEZIEN);
        PxData.vuil(p.level().getServer());
    }

    // --- the projector ------------------------------------------------------------------------------------------------------

    @Nullable
    static ProjectorBlockEntity projector(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ProjectorBlockEntity be ? be : null;
    }

    /** Opens (or refreshes) the projector screen for this player. */
    public static void open(ServerPlayer p, BlockPos pos) {
        open(p, pos, null);
    }

    static void open(ServerPlayer p, BlockPos pos, @Nullable Component melding) {
        if (projector(p.level(), pos) != null) {
            ModNetworking.sendTo(p, new BioscoopPayloads.Open(stand(p, pos, melding)));
        }
    }

    /** What the projector screen shows this player: every film (theirs or locked), what runs now, the screen it found. */
    public static CompoundTag stand(ServerPlayer p, BlockPos pos, @Nullable Component melding) {
        ServerLevel level = p.level();
        CompoundTag t = new CompoundTag();
        t.putLong("Pos", pos.asLong());
        ProjectorBlockEntity be = projector(level, pos);
        String speelt = be == null ? "" : be.film();
        t.putString("Speelt", speelt);
        Doek doek = be == null ? null : be.speelt() && be.doek() != null ? be.doek() : be.zoekDoek();
        t.putInt("DoekB", doek == null ? 0 : doek.breedte());
        t.putInt("DoekH", doek == null ? 0 : doek.hoogte());
        if (melding != null) {
            Tekst.put(t, "Melding", melding);
        }
        ListTag films = new ListTag();
        for (String id : Films.IDS) {
            CompoundTag f = new CompoundTag();
            f.putString("Id", id);
            f.putBoolean("Heeft", Films.heeft(p, id));
            FilmInfo info = FilmInfo.van(level.getServer(), id);
            f.putInt("Duur", info == null ? 0 : info.duur());
            f.putInt("Gezien", gezien(p, id));
            films.add(f);
        }
        t.put("Films", films);
        return t;
    }

    /**
     * The player starts a film on this projector. Checked: the projector is there and near, the film exists and is THEIRS,
     * and there is a screen in front of the projector. A film that already runs is replaced.
     */
    public static Uitkomst speel(ServerPlayer p, BlockPos pos, String film) {
        ServerLevel level = p.level();
        ProjectorBlockEntity be = projector(level, pos);
        if (be == null) {
            return Uitkomst.GEEN_PROJECTOR;
        }
        if (p.distanceToSqr(Vec3.atCenterOf(pos)) > BEDIEN * BEDIEN) {
            return Uitkomst.TE_VER;
        }
        FilmInfo info = Films.IDS.contains(film) ? FilmInfo.van(level.getServer(), film) : null;
        if (info == null) {
            return Uitkomst.ONBEKEND;
        }
        if (!Films.heeft(p, film)) {
            return Uitkomst.OP_SLOT;
        }
        return start(level, be, info) ? Uitkomst.OK : Uitkomst.GEEN_DOEK;
    }

    /** Starts a film without asking whose it is (the dev command, the tests); false: no screen. */
    static boolean start(ServerLevel level, ProjectorBlockEntity be, FilmInfo info) {
        Doek doek = be.zoekDoek();
        if (doek == null) {
            return false;
        }
        if (be.speelt()) {
            be.stop(level, false);
        }
        be.begin(level, info, doek);
        return true;
    }

    /** The player switches the projector off. */
    public static Uitkomst stop(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        ProjectorBlockEntity be = projector(level, pos);
        if (be == null) {
            return Uitkomst.GEEN_PROJECTOR;
        }
        if (p.distanceToSqr(Vec3.atCenterOf(pos)) > BEDIEN * BEDIEN) {
            return Uitkomst.TE_VER;
        }
        be.stop(level, false);
        return Uitkomst.OK;
    }

    /** (The projector's tick) the film ran to its end: everybody near the screen has watched it. */
    static void uitgespeeld(ServerLevel level, ProjectorBlockEntity be, FilmInfo info) {
        Doek doek = be.doek();
        Vec3 midden = doek == null ? Vec3.atCenterOf(be.getBlockPos()) : doek.midden();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && (p.distanceToSqr(midden) <= KIJK * KIJK || p.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) <= KIJK * KIJK)) {
                markeerGezien(p, info.id());
                p.sendOverlayMessage(Component.translatable("gui.guhs.guhbioscoop.einde", naam(info.id())).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** The screen's two buttons (the payload handler). */
    static void doe(ServerPlayer p, BlockPos pos, int actie, String film) {
        Uitkomst u = actie == BioscoopPayloads.STOP ? stop(p, pos) : speel(p, pos, film);
        Component melding = switch (u) {
            case OK -> actie == BioscoopPayloads.STOP ? Component.translatable("gui.guhs.guhbioscoop.melding.gestopt")
                    : Component.translatable("gui.guhs.guhbioscoop.melding.speelt", naam(film)).withStyle(ChatFormatting.GREEN);
            case OP_SLOT -> slot(film).withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("gui.guhs.guhbioscoop.melding." + u.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GOLD);
        };
        open(p, pos, melding);
    }

    private Bioscoop() {
    }
}
