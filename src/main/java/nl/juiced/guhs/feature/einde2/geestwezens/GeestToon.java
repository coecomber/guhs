package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2, slice geestmodellen: what the group {@code geest} of {@code /guhs einde2 toon} (feature/einde2/Einde2Toon, a dev run
 * only) puts in its row: every creature of {@link GeestWezens} in every look (Koh with each of his six faces, Hei Bai-guh small
 * and as the giant, Tui and La, the four lantern colours), each standing still ({@code stil}), and every kind of
 * {@link GeestKinds}. A row entry says how much room it takes along the row ({@code breed}) and in depth ({@code diep}: Koh's
 * body and Fang's tail reach about ten blocks back), and how far behind the row line it stands ({@code achter}).
 */
public final class GeestToon {
    /** One thing of the row. */
    public record Rij(String naam, Function<ServerLevel, Entity> maak, int breed, int diep, int achter, boolean alleen) {
    }

    /** The whole group, in order. */
    public static List<Rij> rij() {
        List<Rij> uit = new ArrayList<>();
        for (GeestWezens w : GeestWezens.values()) {
            for (int s = 0; s < Math.max(1, w.soorten); s++) {
                int soort = s;
                if (w == GeestWezens.GEESTREUS) {
                    continue;                                       // (a giant hides when you come near: see /guhs einde2 geestwezens ga reus)
                }
                int breed = switch (w) {
                    case KOH, HEI_BAI_GUH, GEEST_KOI, OUDE_REIGERGUH -> 6;
                    case FANG -> 5;
                    case KIJKMASKER -> 2;
                    default -> 3;
                };
                int diep = switch (w) {
                    case KOH -> 14;
                    case FANG -> 12;
                    case GEEST_KOI, OUDE_REIGERGUH -> 8;
                    case HEI_BAI_GUH -> 6;
                    default -> 5;
                };
                uit.add(new Rij(w.id() + (w.soorten > 0 ? " " + s : ""), level -> {
                    GeestWezen g = E2GeestWezensFeature.type(w).create(level, EntitySpawnReason.COMMAND);
                    if (g != null) {
                        g.zetSoort(soort);
                        g.zetStil(true);
                        g.setNoAi(w == GeestWezens.WAN_GUH);       // (in the row Wan-guh does not vanish)
                    }
                    return g;
                }, breed, diep, 0, false));
            }
        }
        for (GuhNpcEntity.Kind kind : GeestKinds.kinds()) {
            int sets = GeestKinds.mix(kind);
            for (int i = 0; i < Math.max(1, sets); i++) {           // (a crowd kind once per set; where it lands picks its set)
                uit.add(new Rij(kind.id() + (sets > 0 ? " " + i : ""), level -> {
                    GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
                    if (npc != null) {
                        npc.setKind(kind);
                    }
                    return npc;
                }, 3, 5, 0, false));
            }
        }
        return uit;
    }

    /** How many things the group puts down. */
    public static int aantal() {
        return rij().size();
    }

    private GeestToon() {
    }
}
