package nl.juiced.guhs.feature.einde2.verhaal;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;

/**
 * einde2 fix round 1 (fix-kern, playtest 1 item 11): "Verder met het verhaal". After dying (you wake in the overworld),
 * logging in, or being teleported away, a player must always know where they are in the Guhvatar story and have ONE easy
 * way back. Nothing here ever changes a step: the story state stays main's {@code Verhaallijn}s.
 * <ul>
 *   <li><b>Where am I</b> ({@link #meld}): on respawn, on login and when leaving or entering the Guheinde, one chat line
 *       with the chapter and the step ("Je verhaal: Hoofdstuk 1 ... - Zweef door de ringen") and how to go on: outside the
 *       Guheinde "go back through your portal, click the kaasbol"; inside it "click the kaasbol at the Knabbelberg" (when
 *       the step is far away) or the direction to the step.</li>
 *   <li><b>The way back</b> ({@link #verder}): the glowing kaasbol at the Knabbelberg, where the portal sets you down, stays
 *       for every player who is in the story; a click when the story has begun does NOT play the intro again but puts the
 *       player at their current step's place ({@link #waar}), calls Appa there when it is his landing spot, and shows the
 *       step's hint ({@link StapHint}). Anyone else may call {@link #verder} too (the whistle list, a Guhpad button).</li>
 *   <li><b>The resume location of every step</b> ({@link #waar}): the {@link StapHint} of the step when it has one (its
 *       {@code terug} spot, else its target); else from the line's own {@code doel}: a structure -> its {@code aankomst}
 *       (Appa's landing spot); a spot inside a chapter's structure -> that chapter structure's {@code aankomst}; any
 *       other spot in the Guheinde -> that spot; a step in the Geestenwereld -> the chapter's structure, with the line
 *       "mediteer opnieuw" (the way into the Geestenwereld stays the story's own); a line without any place -> nothing.</li>
 * </ul>
 * The current story ({@link #huidig}): the followed line ({@code Verhaallijnen.gevolgd}) when it is an einde2 line that is
 * on, else the first chapter that is not done (once it is open). Texts: {@code gui.guhs.einde2_verhaal.hervat.*}.
 */
public final class E2Hervat {
    public static final String P = "gui.guhs.einde2_verhaal.hervat.";
    /** The non-chapter lines of the Guheinde story that live in another group. */
    public static final List<String> EXTRA = List.of("werktuigguh", "vredesballon");
    /** A step farther than this (blocks) gets "klik op de kaasbol" instead of only the direction. */
    public static final int VER = 160;
    /** Not more than one "where am I" line on leaving / login per player in this many ticks (dimension changes come in a row). */
    public static final int RUST = 20 * 45;

    /** Where the kaasbol of the Knabbelberg lies (h1 sets it: {@code Kaasbol.plek}); null: not known. */
    public static volatile Function<ServerLevel, BlockPos> KAASBOL = level -> null;

    /** A resume location: dimension, spot, the way to face, and what kind ("hint", "aankomst", "plek", "geest", "wacht"). */
    public record Plek(ResourceKey<Level> dim, @Nullable Vec3 pos, float yaw, String soort) {
    }

    private static final Map<UUID, Long> LAATST = new ConcurrentHashMap<>();

    private E2Hervat() {
    }

    // =====================================================================================================================
    // what is the player's story now
    // =====================================================================================================================

    /** Is this one of the Guheinde story's lines? */
    public static boolean isEinde2(Verhaallijn l) {
        return E2Verhaal.GROEP.equals(l.groep()) || EXTRA.contains(l.id());
    }

    /** Is the line on for the player: not done, and begun (or open because the line before it is done)? */
    public static boolean actief(ServerPlayer p, Verhaallijn l) {
        return !l.klaar(p) && (l.begonnen(p) || l.na() != null && l.aanDeBeurt(p));
    }

    /** The story line the player is in now (null: none: the story has not begun, or everything is done). */
    @Nullable
    public static Verhaallijn huidig(ServerPlayer p) {
        if (!E2Verhaal.magBeginnen(p)) {
            return null;
        }
        Verhaallijn g = Verhaallijnen.gevolgd(p);
        if (g != null && isEinde2(g) && actief(p, g)) {
            return g;
        }
        for (String id : E2Verhaal.HOOFDSTUKKEN) {
            Verhaallijn l = E2Verhaal.lijn(id);
            if (l == null || l.klaar(p)) {
                continue;
            }
            return actief(p, l) ? l : null;
        }
        return null;
    }

    /** May the kaasbol bring this player back (they are in the story, past its very start)? */
    public static boolean kanVerder(ServerPlayer p) {
        Verhaallijn l = huidig(p);
        return l != null && !(l.id().equals(E2Verhaal.LUCHT) && l.stap(p) == 0);
    }

    // =====================================================================================================================
    // where to resume
    // =====================================================================================================================

    /** The resume location of the player's current story (null: no story on). */
    @Nullable
    public static Plek waar(ServerPlayer p) {
        Verhaallijn l = huidig(p);
        return l == null ? null : waar(p, l);
    }

    /** The resume location of this line's current step for the player ({@link Plek#pos} null: not known yet / none). */
    @Nullable
    public static Plek waar(ServerPlayer p, Verhaallijn l) {
        if (l.klaar(p)) {
            return null;
        }
        ServerLevel guheinde = p.level().getServer() == null ? null : p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        StapHint.Hint h = StapHint.hint(l.id(), l.stap(p));
        if (h != null) {
            StapHint.Doel t = h.terugDoel();
            BlockPos b = t.blok(p);
            ServerLevel level = p.level().getServer().getLevel(t.dim());
            if (b != null && level != null) {
                BlockPos naar = h.doel().blok(p);
                Vec3 pos = veilig(level, Vec3.atBottomCenterOf(b));
                float yaw = naar == null || naar.equals(b) ? p.getYRot() : yawNaar(pos, Vec3.atCenterOf(naar));
                return new Plek(t.dim(), pos, yaw, "hint");
            }
        }
        boolean hoofdstuk = E2Verhaal.HOOFDSTUKKEN.contains(l.id());
        Doel d = l.doel(p);
        String structuur = null;
        String soort = "aankomst";
        if (d != null && d.dim() == GuheindeFeature.GUHEINDE) {
            structuur = d.structuur();
            if (structuur == null && d.plek() != null) {
                if (!hoofdstuk) {
                    return guheinde == null ? null : new Plek(GuheindeFeature.GUHEINDE, veilig(guheinde, Vec3.atBottomCenterOf(d.plek())), p.getYRot(), "plek");
                }
                structuur = E2Verhaal.structuurVan(l.id());
            }
        } else if (hoofdstuk) {
            structuur = E2Verhaal.structuurVan(l.id());
            if (d != null && d.dim() == Geestenwereld.DIM) {
                soort = "geest";
            }
        }
        if (structuur == null || guheinde == null) {
            return null;
        }
        if (!Plekken.bekend(guheinde, structuur)) {
            Plekken.zoekVooruit(guheinde);
            return new Plek(GuheindeFeature.GUHEINDE, null, 0f, "wacht");
        }
        Vec3 a = Plekken.aankomst(guheinde, structuur);
        if (a == null) {
            BoundingBox doos = Plekken.doos(guheinde, structuur);
            if (doos == null) {
                return new Plek(GuheindeFeature.GUHEINDE, null, 0f, "wacht");
            }
            BlockPos m = doos.getCenter();
            laad(guheinde, m);
            a = new Vec3(m.getX() + 0.5, guheinde.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, m.getX(), m.getZ()), m.getZ() + 0.5);
        }
        BoundingBox doos = Plekken.doos(guheinde, structuur);
        Vec3 midden = doos == null ? a : Vec3.atCenterOf(doos.getCenter());
        return new Plek(GuheindeFeature.GUHEINDE, veilig(guheinde, a), yawNaar(a, midden), soort);
    }

    private static float yawNaar(Vec3 van, Vec3 naar) {
        Vec3 d = naar.subtract(van);
        return d.horizontalDistanceSqr() < 1e-4 ? 0f : (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    private static void laad(ServerLevel level, BlockPos b) {
        if (!level.isLoaded(b)) {
            level.getChunk(b.getX() >> 4, b.getZ() >> 4);
        }
    }

    /**
     * A spot near {@code pos} where a player can stand: first on the same level (the marker itself, else the nearest free
     * spot with ground at most 3 below within 6 blocks: a marker on a door or a statue puts you next to it, not on its roof),
     * then by climbing out of blocks (at most 16) and down onto ground (at most 6); nothing found: {@code pos} itself.
     */
    public static Vec3 veilig(ServerLevel level, Vec3 pos) {
        laad(level, BlockPos.containing(pos));
        for (int klim : new int[] {0, 16}) {
            for (int r = 0; r <= 6; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        Vec3 q = staan(level, new Vec3(pos.x + dx, pos.y, pos.z + dz), klim, klim == 0 ? 3 : 6);
                        if (q != null) {
                            return q;
                        }
                    }
                }
            }
        }
        return pos;
    }

    @Nullable
    private static Vec3 staan(ServerLevel level, Vec3 pos, int klim, int zak) {
        Vec3 q = pos;
        for (int up = 0; up < klim && !vrij(level, q); up++) {
            q = q.add(0, 1, 0);
        }
        if (!vrij(level, q)) {
            return null;
        }
        for (int down = 0; down <= zak; down++) {
            Vec3 f = q.add(0, -down, 0);
            if (!vrij(level, f)) {
                return null;
            }
            if (!vrij(level, f.add(0, -1, 0))) {
                return new Vec3(f.x, Math.floor(f.y) + 0.05, f.z);       // (ground right under the feet)
            }
        }
        return null;
    }

    private static boolean vrij(ServerLevel level, Vec3 voeten) {
        AABB doos = EntityType.PLAYER.getDimensions().makeBoundingBox(voeten.x, Math.floor(voeten.y) + 0.01, voeten.z);
        return level.noCollision(doos);
    }

    // =====================================================================================================================
    // the way back
    // =====================================================================================================================

    /**
     * "Verder met het verhaal": puts the player at their current step's resume location (never a step change), calls
     * Appa there when it is his landing spot, and shows the step's hint a moment later. True = brought back; false = a line
     * says why not (no story on, the place is still being worked out, a Geestenwereld step, they watch a scene).
     */
    public static boolean verder(ServerPlayer p) {
        Verhaallijn l = huidig(p);
        if (l == null) {
            p.sendSystemMessage(Component.translatable(P + "niets").withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (Cutscenes.bezig(p)) {
            return false;
        }
        Plek w = waar(p, l);
        if (w == null || w.pos() == null) {
            p.sendSystemMessage(Component.translatable(w == null ? P + "geen_plek" : P + "wacht").withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (p.getVehicle() != null) {
            p.stopRiding();
        }
        Duwtje.terug(p, w.dim(), w.pos(), w.yaw());
        p.sendSystemMessage(Component.translatable(P + "verder").withStyle(ChatFormatting.LIGHT_PURPLE));
        p.sendSystemMessage(waarZin(p, l).withStyle(ChatFormatting.GOLD));
        if (w.soort().equals("geest")) {
            p.sendSystemMessage(Component.translatable(P + "geest").withStyle(ChatFormatting.AQUA));
        }
        if (w.soort().equals("aankomst") || w.soort().equals("geest")) {
            StapHint.later(p, 10, s -> {
                if (Appa.heeft(s) && !Appa.isWeg(s) && s.getVehicle() == null) {
                    Appa.roep(s);
                }
            });
        }
        String lijn = l.id();
        StapHint.later(p, 30, s -> StapHint.toon(s, lijn, true));
        return true;
    }

    // =====================================================================================================================
    // "where am I"
    // =====================================================================================================================

    /** "Je verhaal: <chapter> - <what to do now>". */
    public static MutableComponent waarZin(ServerPlayer p, Verhaallijn l) {
        return Component.translatable(P + "waar", Component.translatable("gui.guhs.verhalen." + l.id() + ".naam"),
                Component.translatable("gui.guhs.verhalen." + l.id() + ".nu." + l.sleutel(p)));
    }

    /** The reason a "where am I" line is sent. */
    public enum Waarom { RESPAWN, LOGIN, WEG, TERUG }

    /** Sends the "where am I" line and how to go on (nothing when no story is on, or in the Geestenwereld). */
    public static boolean meld(ServerPlayer p, Waarom waarom) {
        Verhaallijn l = huidig(p);
        if (l == null || Cutscenes.bezig(p) || p.level().dimension() == Geestenwereld.DIM || !kanVerder(p)) {
            return false;
        }
        long nu = p.level().getGameTime();
        Long vorig = LAATST.get(p.getUUID());
        if ((waarom == Waarom.WEG || waarom == Waarom.LOGIN) && vorig != null && Math.abs(nu - vorig) < RUST) {
            return false;
        }
        LAATST.put(p.getUUID(), nu);
        boolean inGuheinde = GuheindeFeature.isGuheinde(p.level());
        BlockPos bolHier = inGuheinde ? KAASBOL.apply(p.level()) : null;
        if (waarom == Waarom.TERUG && bolHier != null && bolHier.closerToCenterThan(p.position(), 96)) {
            // (back through the portal: the kaasbol is right here)
            p.sendSystemMessage(waarZin(p, l).withStyle(ChatFormatting.GOLD));
            p.sendSystemMessage(Component.translatable(P + "kaasbol_hier").withStyle(ChatFormatting.YELLOW));
            p.sendOverlayMessage(Component.translatable(P + "kaasbol_balk").withStyle(ChatFormatting.GOLD));
            return true;
        }
        p.sendSystemMessage(waarZin(p, l).withStyle(ChatFormatting.GOLD));
        if (!inGuheinde) {
            p.sendSystemMessage(Component.translatable(P + "portaal").withStyle(ChatFormatting.YELLOW));
            return true;
        }
        Plek w = waar(p, l);
        if (w != null && w.pos() != null && w.dim() == p.level().dimension() && p.position().distanceTo(w.pos()) <= VER) {
            StapHint.toon(p, l.id(), false);
        } else {
            BlockPos bol = KAASBOL.apply(p.level());
            MutableComponent r = bol == null ? Component.translatable(StapHint.P + "elders") : Richting.van(p.position(), Vec3.atCenterOf(bol));
            p.sendSystemMessage(Component.translatable(P + "kaasbol", r).withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    /** From E2Verhaal.register (common code). */
    static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (!e.isEndConquered() && e.getEntity() instanceof ServerPlayer p) {
                StapHint.later(p, 40, s -> meld(s, Waarom.RESPAWN));
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                StapHint.later(p, 100, s -> meld(s, Waarom.LOGIN));
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || e.getFrom() == Geestenwereld.DIM || e.getTo() == Geestenwereld.DIM) {
                return;
            }
            if (e.getTo() == GuheindeFeature.GUHEINDE) {
                StapHint.later(p, 40, s -> meld(s, Waarom.TERUG));
            } else if (e.getFrom() == GuheindeFeature.GUHEINDE) {
                StapHint.later(p, 40, s -> meld(s, Waarom.WEG));
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            LAATST.remove(e.getEntity().getUUID());
            StapHint.vergeet(e.getEntity().getUUID());
        });
    }
}
