package nl.juiced.guhs.feature.einde2.spcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.2), slice sp_cast: the Mikas of De Ruige Kroeg as {@link GuhNpcEntity.Kind}s (the Kind
 * block {@code <einde2_sp_cast>}), one row per Kind with its animations besides {@code idle} and their length in ticks (0: a
 * pose that lasts until {@code npc.speel("")}). Every Kind has {@code praat}, {@code kijk_op} (the stand-still game's look:
 * up on its paws, ears up, two big staring eyes; the slapende Mika opens one eye) and {@code slaap} (head down, z's float
 * up; the slapende Mika's idle is his sleep).
 * <p>
 * tools/features/einde2_sp_cast_kroeg.py holds the same table as {@code KINDS} and its check compares the two. Models
 * {@code geckolib/models/entity/guh_npc_<kind>.geo.json}, textures {@code textures/entity/npc_<kind>.png}, animations
 * {@code geckolib/animations/entity/guh_npc_<kind>.animation.json} (names {@code animation.guh_sitting.<name>}).
 */
public enum KroegMikas {
    KROEG_BARMAN_MIKA("praat:24", "kijk_op:0", "slaap:0"),
    KROEG_UITSMIJTER_MIKA("praat:24", "kijk_op:0", "slaap:0"),
    STOERE_MIKA("praat:24", "kijk_op:0", "slaap:0"),
    STOERE_MIKA_2("praat:24", "kijk_op:0", "slaap:0"),
    SLAPENDE_MIKA("praat:24", "kijk_op:0", "slaap:0");

    /** The small talk of a click: {@code gui.guhs.einde2_sp_cast.praatje.<kind>.0} / {@code .1}. */
    public static final String PRAATJE = "gui.guhs.einde2_sp_cast.praatje.";

    public final Kind kind;
    private final List<String> animaties;
    private final int[] duren;

    KroegMikas(String... animaties) {
        this.kind = Kind.valueOf(name());
        this.animaties = Arrays.stream(animaties).map(a -> a.substring(0, a.indexOf(':'))).toList();
        this.duren = Arrays.stream(animaties).mapToInt(a -> Integer.parseInt(a.substring(a.indexOf(':') + 1))).toArray();
    }

    @Nullable
    public static KroegMikas rij(Kind kind) {
        for (KroegMikas r : values()) {
            if (r.kind == kind) {
                return r;
            }
        }
        return null;
    }

    public static List<Kind> kinds() {
        List<Kind> out = new ArrayList<>();
        for (KroegMikas r : values()) {
            out.add(r.kind);
        }
        return out;
    }

    /** The names besides idle. */
    public List<String> animaties() {
        return animaties;
    }

    /** Ticks; 0 = a pose that lasts; -1: no such animation. */
    public int duur(String naam) {
        int i = animaties.indexOf(naam);
        return i < 0 ? -1 : duren[i];
    }

    /** The default role of a Kroeg Mika: a click plays praat and says one of its two lines (a role for its spot wins). */
    public static final NpcRole ROL = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            npc.speel("praat");
            GuhQuests.say(player, npc, PRAATJE + npc.getKind().id() + "." + npc.level().getRandom().nextInt(2));
        }
    };
}
