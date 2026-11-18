package nl.juiced.guhs.feature.guhwinter.npc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import net.minecraft.server.MinecraftServer;

/**
 * Guhwinter (npc slice): where the NPCs learn the event phase. kern's {@code Guhwinter.fase(server)} did not exist yet
 * when this slice was built, so the default ({@link #ECHT}) reads the real date in Europe/Amsterdam with the same dates
 * as kern's contract.
 * <p>INTEGRATION: wire kern in ONE place, e.g. in GuhwinterNpcFeature.register:
 * <pre>
 * GuhwinterNpcs.faseBron(server -> switch (Guhwinter.fase(server)) {
 *     case VOOR, SINT, PAKJESAVOND -> NpcFase.SINT;
 *     case KERST -> NpcFase.KERST;
 *     case OUDJAAR -> NpcFase.OUDJAAR;
 *     case AFGELOPEN -> NpcFase.WEG;
 * });
 * </pre>
 * (then kern's dev date override moves the NPCs too; this slice's own {@code /guhs winter npc fase} override still wins).
 */
@FunctionalInterface
public interface NpcFaseBron {
    NpcFase fase(MinecraftServer server);

    ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");
    /** 6 Dec 2026 00:00: the Sint leaves, Christmas begins. */
    LocalDateTime KERST_VANAF = LocalDateTime.of(2026, 12, 6, 0, 0);
    /** 26 Dec 2026 00:00: Oud &amp; Nieuw opens. */
    LocalDateTime OUDJAAR_VANAF = LocalDateTime.of(2026, 12, 26, 0, 0);
    /** 3 Jan 2027 00:00: everything melts away. */
    LocalDateTime WEG_VANAF = LocalDateTime.of(2027, 1, 3, 0, 0);

    /** The default: the real clock (before the release counts as SINT, like kern's VOOR in dev). */
    NpcFaseBron ECHT = server -> opDatum(ZonedDateTime.now(AMSTERDAM).toLocalDateTime());

    /** The phase at this local (Amsterdam) time. */
    static NpcFase opDatum(LocalDateTime t) {
        if (!t.isBefore(WEG_VANAF)) {
            return NpcFase.WEG;
        }
        if (!t.isBefore(OUDJAAR_VANAF)) {
            return NpcFase.OUDJAAR;
        }
        if (!t.isBefore(KERST_VANAF)) {
            return NpcFase.KERST;
        }
        return NpcFase.SINT;
    }
}
