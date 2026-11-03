package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.erfenis.ErfenisPayloads.Actie;
import nl.juiced.guhs.feature.erfenis.koppeling.Dorpsbeloningen;
import nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen;

/**
 * De Erfenis, the server side of deco (deco slice): the {@link Actie#DECO} actions of the overview (place from the
 * decoschuurtje, put away, buy in the decowinkel, draw / erase paths and hekjes, move and turn a placed piece) and keeping the
 * world in step: every changed tile is stamped again, the paths around it get their new corners ({@link Stempel#paden}) and
 * the hekjes, muurtjes and tralies around it re-connect.
 */
public final class DecoActies {
    private DecoActies() {
    }

    /** One deco action of the owner (after a catch-up; {@link Erfenis#actie} syncs afterwards). Null = fine, else what went wrong. */
    @Nullable
    static Base.Fout doe(MinecraftServer server, ServerPlayer p, Base b, Actie a) {
        switch (a.x2()) {
            case Actie.DECO_WEG -> {
                Base.Deco d = b.decoOp(a.x(), a.z());
                if (d == null) {
                    return new Base.Fout("erfenis.fout.weg");
                }
                b.bergDecoOp(d);
                int[] m = Base.decoMaat(d.id(), d.rot());
                tegels(server, b, vak(d.x(), d.z(), m[0], m[1]));
                Erfenis.melding(p, "erfenis.ok.deco_weg", true, "erfenis.deco." + d.id());
                return null;
            }
            case Actie.DECO_KOOP -> {
                return koop(server, p, b, a.tekst(), a.x(), a.z(), a.rot(), (a.id() & Actie.KOOP_PLAATS) != 0, (a.id() & Actie.KOOP_BON) != 0);
            }
            case Actie.DECO_TREK -> {
                String t = a.tekst();
                int i = t.indexOf(':');
                if (i <= 0) {
                    return null;
                }
                String soort = t.substring(0, i);
                Paden.Uitkomst u = Paden.trek(b, soort, Paden.decodeer(t.substring(i + 1)));
                tegels(server, b, u.veranderd());
                if (u.gezet() > 0) {
                    geluid(p, DecoWinkel.isPad(soort) ? SoundEvents.ROOTED_DIRT_PLACE : SoundEvents.BAMBOO_WOOD_PLACE);
                    if (u.betaald() > 0 || u.uitVoorraad() > 0) {
                        Erfenis.melding(p, u.uitVoorraad() > 0 ? "erfenis.decowinkel.ok.getrokken_voorraad" : "erfenis.decowinkel.ok.getrokken", true,
                                Integer.toString(u.gezet()), DecoWinkel.stuk(soort).naamKey(), Integer.toString(u.betaald()),
                                Integer.toString(u.uitVoorraad()));
                    }
                }
                if (u.geldOp()) {
                    return new Base.Fout("erfenis.decowinkel.fout.geld_op", Integer.toString(u.gezet()));
                }
                return u.gezet() == 0 && u.overgeslagen() > 0 ? new Base.Fout("erfenis.decowinkel.fout.hier_niet") : null;
            }
            case Actie.DECO_GUM -> {
                Paden.Uitkomst u = Paden.gum(b, Paden.decodeer(a.tekst()));
                tegels(server, b, u.veranderd());
                if (u.gezet() > 0) {
                    geluid(p, SoundEvents.ROOTED_DIRT_BREAK);
                    Erfenis.melding(p, "erfenis.decowinkel.ok.gegumd", true, Integer.toString(u.gezet()));
                }
                return null;
            }
            case Actie.DECO_VERPLAATS -> {
                Base.Deco d = b.decoOp(a.x(), a.z());
                if (d == null) {
                    return new Base.Fout("erfenis.fout.weg");
                }
                return verplaats(server, p, b, d, a.id(), a.z2(), a.rot());
            }
            case Actie.DECO_DRAAI -> {
                Base.Deco d = b.decoOp(a.x(), a.z());
                if (d == null) {
                    return new Base.Fout("erfenis.fout.weg");
                }
                return verplaats(server, p, b, d, d.x(), d.z(), d.rot() + 1);
            }
            default -> {   // DECO_PLAATS: from the decoschuurtje
                Base.Fout f = b.plaatsDeco(a.tekst(), a.x(), a.z(), a.rot());
                if (f == null) {
                    int[] m = Base.decoMaat(a.tekst(), a.rot());
                    tegels(server, b, vak(a.x(), a.z(), m[0], m[1]));
                    geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME);
                }
                return f;
            }
        }
    }

    /** The decowinkel: buy one piece into the decoschuurtje, or (plaats) buy-and-place, using one from the schuurtje first. */
    @Nullable
    static Base.Fout koop(MinecraftServer server, ServerPlayer p, Base b, String id, int x, int z, int rot, boolean plaats, boolean bon) {
        if (plaats && b.decoOpslag.getOrDefault(id, 0) > 0) {   // there is one waiting in the schuurtje: that one, for free
            return doe(server, p, b, new Actie(Actie.DECO, 0, x, z, Actie.DECO_PLAATS, 0, rot, id));
        }
        if (plaats) {
            Base.Fout f = b.decoPlekFout(id, x, z, rot, null);
            if (f != null) {
                return f;
            }
        }
        Base.Fout f = koop(server, b, id, bon);
        if (f != null) {
            return f;
        }
        geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP);
        if (!plaats) {
            Erfenis.melding(p, "erfenis.decowinkel.ok.gekocht", true, "erfenis.deco." + id);
            return null;
        }
        return doe(server, p, b, new Actie(Actie.DECO, 0, x, z, Actie.DECO_PLAATS, 0, rot, id));
    }

    /** Buys one piece into the decoschuurtje (Guhlden, or one of the owner's Decobonnen). Null = bought. */
    @Nullable
    public static Base.Fout koop(MinecraftServer server, Base b, String id, boolean bon) {
        int bonnen = b.eigenaar == null ? 0 : Dorpsbeloningen.decobonnen(server, b.eigenaar);
        Base.Fout f = DecoWinkel.kanKopen(b, id, bon, bonnen);
        if (f != null) {
            return f;
        }
        if (bon && !Dorpsbeloningen.neemDecobon(server, b.eigenaar)) {
            return new Base.Fout("erfenis.decowinkel.fout.geen_bon");
        }
        return DecoWinkel.koop(b, id, bon, 1);
    }

    @Nullable
    private static Base.Fout verplaats(MinecraftServer server, ServerPlayer p, Base b, Base.Deco d, int x, int z, int rot) {
        int[] oud = Base.decoMaat(d.id(), d.rot());
        Base.Fout f = b.verplaatsDeco(d, x, z, rot & 3);
        if (f != null) {
            return f;
        }
        int[] m = Base.decoMaat(d.id(), rot & 3);
        List<int[]> l = vak(d.x(), d.z(), oud[0], oud[1]);
        l.addAll(vak(x, z, m[0], m[1]));
        tegels(server, b, l);
        geluid(p, SoundEvents.WOOD_PLACE);
        return null;
    }

    // =====================================================================================================================
    // the world
    // =====================================================================================================================

    /** Puts these tiles of a base into the world again (when somebody is there; else on the next visit). */
    static void tegels(MinecraftServer server, Base b, List<int[]> tegels) {
        ServerLevel level = Erfenis.level(server);
        if (level == null || tegels.isEmpty()) {
            return;
        }
        if (b.moetStempelen || !Erfenis.bezocht(server, b)) {
            b.moetStempelen = true;
            return;
        }
        stempelTegels(level, Plot.oorsprong(b.cx, b.cz), b, tegels);
    }

    /**
     * Stamps these tiles of {@code b} with the plot's min corner at {@code o}: a placed piece is stamped whole (once), a bare
     * tile becomes grass; then the paths in and around them (their corners follow the neighbours) and the connecting blocks
     * (hekjes, muurtjes, tralies) around them. Tiles under a building are left alone.
     */
    public static void stempelTegels(ServerLevel level, BlockPos o, Base b, List<int[]> tegels) {
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        List<Base.Deco> gedaan = new ArrayList<>();
        for (int[] t : Paden.uniek(tegels)) {
            int x = t[0], z = t[1];
            if (!Plot.opPlot(x, z)) {
                continue;
            }
            x0 = Math.min(x0, x);
            z0 = Math.min(z0, z);
            x1 = Math.max(x1, x);
            z1 = Math.max(z1, z);
            if (b.op(x, z) != null) {
                continue;
            }
            Base.Deco d = b.decoOp(x, z);
            if (d != null) {
                if (!gedaan.contains(d)) {
                    gedaan.add(d);
                }
            } else if (Paden.padOp(b, x, z) == null) {   // (a path tile is stamped below, with its corners)
                GebouwVormen.wis(level, o.offset(x, 0, z), 1, 1);
            }
        }
        if (x0 == Integer.MAX_VALUE) {
            return;
        }
        for (Base.Deco d : gedaan) {
            GebouwVormen.stempelDeco(level, d.id(), o.offset(d.x(), 0, d.z()), d.rot());
        }
        Stempel.paden(level, o, b, Math.max(0, x0 - 1), Math.max(0, z0 - 1), Math.min(Plot.MAAT, x1 + 2) - Math.max(0, x0 - 1),
                Math.min(Plot.MAAT, z1 + 2) - Math.max(0, z0 - 1));
        verbind(level, o, x0 - 1, z0 - 1, x1 + 1, z1 + 1);
    }

    /** After a building was placed, moved or turned: the paths and hekjes around its footprint follow. */
    static void rand(MinecraftServer server, Base b, int x, int z, int w, int h) {
        ServerLevel level = Erfenis.level(server);
        if (level == null || b.moetStempelen || !Erfenis.bezocht(server, b)) {
            return;
        }
        BlockPos o = Plot.oorsprong(b.cx, b.cz);
        int bx0 = Math.max(0, x - 1), bz0 = Math.max(0, z - 1);
        Stempel.paden(level, o, b, bx0, bz0, Math.min(Plot.MAAT, x + w + 1) - bx0, Math.min(Plot.MAAT, z + h + 1) - bz0);
        verbind(level, o, x - 1, z - 1, x + w, z + h);
    }

    /** Hekjes, muurtjes and tralies in this rectangle (inclusive) connect to what is around them now. */
    static void verbind(ServerLevel level, BlockPos o, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = 1; y <= 2; y++) {
                    BlockPos q = o.offset(x, y, z);
                    BlockState s = level.getBlockState(q);
                    if (s.getBlock() instanceof CrossCollisionBlock || s.getBlock() instanceof WallBlock) {
                        BlockState n = Block.updateFromNeighbourShapes(s, level, q);
                        if (n != s) {
                            level.setBlock(q, n, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        }
                    }
                }
            }
        }
    }

    static List<int[]> vak(int x, int z, int w, int h) {
        List<int[]> l = new ArrayList<>();
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < h; dz++) {
                l.add(new int[] {x + dx, z + dz});
            }
        }
        return l;
    }

    private static void geluid(ServerPlayer p, SoundEvent s) {
        p.level().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, 0.6f, 1.1f);
    }
}
