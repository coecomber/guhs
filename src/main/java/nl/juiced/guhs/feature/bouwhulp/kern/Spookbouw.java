package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * Spookbouw (design 2.1, deel 2 F8): the ONE way everything in the bouwhulp builds. A slice makes an {@link Ontwerp},
 * {@link #start}s a ghost with it, and from there the player (or the slice) turns, mirrors, moves, builds, cancels and
 * undoes through here or through the control panel ({@link #toonBediening}: arrows, turn, mirror, "Bouw!", the shortage
 * list, "alsnog bouwen?", undo).
 * <pre>
 * Spook s = Spookbouw.start(player, ontwerp, pos, Rotation.NONE, Mirror.NONE);   // a ghost, nothing built yet
 * if (s != null) Spookbouw.toonBediening(player, s);                              // let the player place it
 * // or straight away: Spookbouw.bouw(player, s, false) (false: stop at a shortage, its list comes back)
 * </pre>
 * A ghost linked to a {@link Bouwer} (the Bouwkeet: {@link #koppel}) builds at the keet's tempo with the keet's material;
 * otherwise at {@link Spook#tempo} from the builder's inventory (and {@link #zetBank}).
 */
public final class Spookbouw {
    /** Why {@link #start} said no (the message key under gui.guhs.bouwhulp.spook.*). */
    public enum Nee { LEEG, TE_VEEL, BESCHERMD }

    @Nullable
    private static Nee laatsteNee;

    /**
     * A new ghost of this ontwerp at {@code oorsprong} (the low corner before turning), owned by the player. Null (and a
     * message to the player) when the ontwerp is empty, the player has {@link Spoken#MAX_SPOKEN} open ghosts already, or
     * the spot lies in a protected structure.
     */
    @Nullable
    public static Spook start(ServerPlayer p, Ontwerp ontwerp, BlockPos oorsprong, Rotation draai, Mirror spiegel) {
        ServerLevel level = p.level();
        Spoken spoken = Spoken.van(level);
        laatsteNee = null;
        if (ontwerp.isLeeg()) {
            return nee(p, Nee.LEEG);
        }
        if (spoken.aantalVan(p.getUUID()) >= Spoken.MAX_SPOKEN) {
            return nee(p, Nee.TE_VEEL);
        }
        if (beschermd(level, oorsprong)) {
            return nee(p, Nee.BESCHERMD);
        }
        Spook s = spoken.voegToe(level, p, ontwerp, oorsprong);
        if (draai != Rotation.NONE || spiegel != Mirror.NONE) {
            s.zet(oorsprong, draai, spiegel);
            spoken.veranderd(level, s);
        }
        return s;
    }

    /** Why the last {@link #start} on this thread said no (null: it did not). For tests. */
    @Nullable
    public static Nee laatsteNee() {
        return laatsteNee;
    }

    @Nullable
    private static Spook nee(ServerPlayer p, Nee waarom) {
        laatsteNee = waarom;
        p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.spook.nee." + waarom.name().toLowerCase(java.util.Locale.ROOT)));
        return null;
    }

    /** Is building here forbidden (a protected guh structure)? */
    public static boolean beschermd(ServerLevel level, BlockPos pos) {
        return Protected.at(level, pos) || Bescherming.beschermd(level, pos);
    }

    @Nullable
    public static Spook get(ServerLevel level, UUID id) {
        return Spoken.van(level).get(id);
    }

    /** Links the ghost to a builder block (the Bouwkeet): its tempo, its material, its fun. Null unlinks. */
    public static void koppel(ServerLevel level, Spook s, @Nullable BlockPos bouwer) {
        s.bouwerPos = bouwer == null ? null : bouwer.immutable();
        Spoken.van(level).veranderd(level, s);
    }

    /** A Bank Guh of the ghost's own (for a ghost without a keet; the vormstaf). */
    public static void zetBank(ServerLevel level, Spook s, @Nullable UUID bank) {
        s.bank = bank;
        Spoken.van(level).veranderd(level, s);
    }

    /** Blocks per second of a ghost without a keet (default 20). */
    public static void zetTempo(ServerLevel level, Spook s, double tempo) {
        s.tempo = Math.max(0.1, tempo);
        Spoken.van(level).veranderd(level, s);
    }

    /** The name shown in the panel and the "Klaar!" ("Bol 24", "Guh-huisje"...). */
    public static void zetNaam(ServerLevel level, Spook s, String naam) {
        s.naam = naam;
        Spoken.van(level).veranderd(level, s);
    }

    /** Another ontwerp in the same ghost (a slider in the shape tab changed the size). Only while it is not building. */
    public static boolean vervang(ServerLevel level, Spook s, Ontwerp ontwerp) {
        if (s.status == Spook.Status.BOUWT || ontwerp.isLeeg()) {
            return false;
        }
        s.zetOntwerp(ontwerp);
        Spoken.van(level).veranderd(level, s);
        return true;
    }

    /** Turn (a quarter clockwise), mirror (front to back) or move one block; only while it is not building. */
    public static boolean draai(ServerLevel level, Spook s) {
        return zet(level, s, s.oorsprong, s.draai.getRotated(Rotation.CLOCKWISE_90), s.spiegel);
    }

    public static boolean spiegel(ServerLevel level, Spook s) {
        return zet(level, s, s.oorsprong, s.draai, s.spiegel == Mirror.NONE ? Mirror.FRONT_BACK : Mirror.NONE);
    }

    public static boolean schuif(ServerLevel level, Spook s, Direction d, int stappen) {
        return zet(level, s, s.oorsprong.relative(d, stappen), s.draai, s.spiegel);
    }

    public static boolean zet(ServerLevel level, Spook s, BlockPos oorsprong, Rotation draai, Mirror spiegel) {
        if (s.status == Spook.Status.BOUWT || beschermd(level, oorsprong) || level.isOutsideBuildHeight(oorsprong)) {
            return false;
        }
        s.zet(oorsprong, draai, spiegel);
        if (s.status == Spook.Status.WACHT) {
            s.status = Spook.Status.SPOOK;   // moved: what was half built stays, the ghost is a new plan
        }
        Spoken.van(level).veranderd(level, s);
        return true;
    }

    /** "Bouw!" (see {@link Spoken#bouw}): {@code alsnog} = build even when something is missing (the rest stays a ghost). */
    public static Spoken.Bouwen bouw(ServerPlayer p, Spook s, boolean alsnog) {
        return Spoken.van(p.level()).bouw(p.level(), p, s, alsnog);
    }

    /** Stop building (the ghost stays). */
    public static void pauzeer(ServerLevel level, Spook s) {
        Spoken.van(level).pauzeer(level, s);
    }

    /** Takes the ghost away (what is built stays built; undo is separate). */
    public static void annuleer(ServerLevel level, Spook s) {
        Spoken.van(level).weg(level, s);
    }

    /** Undoes this player's last action here (false: nothing to undo). */
    public static boolean ongedaan(ServerPlayer p) {
        return Spoken.van(p.level()).ongedaan(p.level(), p);
    }

    /** What is still missing for the rest of this ghost, with these sources. */
    public static Map<Item, Integer> tekort(ServerPlayer p, Spook s) {
        Spoken spoken = Spoken.van(p.level());
        return spoken.bronnen(p.level(), s, spoken.bouwer(p.level(), s), p).tekort(spoken.nogNodig(p.level(), s));
    }

    /** Opens the control panel of the ghost for the player (with the current shortage). */
    public static void toonBediening(ServerPlayer p, Spook s) {
        SpookPayloads.bediening(p, s, tekort(p, s), false);
    }

    /** A button of the control panel. */
    static void bedien(ServerPlayer p, UUID id, int actie, int waarde, String tekst) {
        ServerLevel level = p.level();
        Spook s = get(level, id);
        if (s == null) {
            return;
        }
        SpookPayloads.Actie a = SpookPayloads.Actie.values()[Math.floorMod(actie, SpookPayloads.Actie.values().length)];
        if (a != SpookPayloads.Actie.VERVERS && a != SpookPayloads.Actie.TERUG && !s.mag(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.spook.niet_van_jou", s.eigenaarNaam));
            return;
        }
        if (!p.blockPosition().closerThan(s.oorsprong, 96)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.spook.te_ver"));
            return;
        }
        boolean vraag = false;
        Map<Item, Integer> tekort = null;
        switch (a) {
            case VERVERS -> {
            }
            case DRAAI -> draai(level, s);
            case SPIEGEL -> spiegel(level, s);
            case SCHUIF -> schuif(level, s, SpookPayloads.richting(waarde), 1);
            case BOUW, ALSNOG -> {
                Spoken.Bouwen b = bouw(p, s, a == SpookPayloads.Actie.ALSNOG);
                if (!b.begonnen()) {
                    vraag = true;
                    tekort = b.tekort();
                }
            }
            case PAUZE -> pauzeer(level, s);
            case ANNULEER -> {
                annuleer(level, s);
                return;
            }
            case TERUG -> {
                if (!ongedaan(p)) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.terug.niets"));
                }
                if (get(level, id) == null) {
                    return;
                }
            }
            case TOEVOEGEN -> {
                ServerPlayer wie = level.getServer().getPlayerList().getPlayerByName(tekst);
                if (wie == null) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.spook.niemand", tekst));
                } else if (!wie.getUUID().equals(s.eigenaar)) {
                    s.helpers.add(wie.getUUID());
                    Spoken.van(level).veranderd(level, s);
                }
            }
            case WEGHALEN -> {
                s.helpers.removeIf(u -> {
                    ServerPlayer hp = level.getServer().getPlayerList().getPlayer(u);
                    return hp != null ? hp.getGameProfile().name().equalsIgnoreCase(tekst) : u.toString().startsWith(tekst);
                });
                Spoken.van(level).veranderd(level, s);
            }
        }
        SpookPayloads.bediening(p, s, tekort != null ? tekort : tekort(p, s), vraag);
    }

    private Spookbouw() {
    }
}
