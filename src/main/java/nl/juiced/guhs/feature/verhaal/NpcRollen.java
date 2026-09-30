package nl.juiced.guhs.feature.verhaal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Binnenkort;
import nl.juiced.guhs.feature.NpcRole;

/**
 * 3.0 (Guhverhalen): the roles of the story characters (GuhNpcEntity.Kind from TIMMERGUH on), registered by the owner slices
 * from their Feature.register, so nobody edits {@code Features.role}:
 * <ul>
 *   <li>{@link #zet(GuhNpcEntity.Kind, NpcRole)}: the default role of a kind;</li>
 *   <li>{@link #zet(GuhNpcEntity.Kind, String, NpcRole)}: the role of the NPCs of that kind whose roleData string
 *       {@code guhs_plek} is plek (set it in the template NBT: {@code RoleData:{guhs_plek:"surf"}}), e.g. Lilo-guh at the surf
 *       beach (guhwaii-spellen) next to Lilo-guh at home (guhwaii), Steele-Mika at the sled race (balto-slee).</li>
 * </ul>
 * {@link #rol} (asked by {@code Features.role}) gives a dispatcher per 3.0 kind that picks the role by the NPC's plek (falling
 * back to the default, then to {@link Binnenkort#ROLE}), and null for the older kinds.
 */
public final class NpcRollen {
    /** The roleData key that says which plek-role an NPC has. */
    public static final String PLEK = "guhs_plek";

    private static final Map<GuhNpcEntity.Kind, NpcRole> STANDAARD = new ConcurrentHashMap<>();
    private static final Map<String, NpcRole> PLEKKEN = new ConcurrentHashMap<>();
    private static final Map<GuhNpcEntity.Kind, NpcRole> DISPATCHERS = new ConcurrentHashMap<>();

    public static void zet(GuhNpcEntity.Kind kind, NpcRole rol) {
        STANDAARD.put(kind, rol);
    }

    public static void zet(GuhNpcEntity.Kind kind, String plek, NpcRole rol) {
        PLEKKEN.put(kind.id() + "|" + plek, rol);
    }

    /** Is this one of the 3.0 kinds (their roles come from here)? */
    public static boolean isVerhaalKind(GuhNpcEntity.Kind kind) {
        return kind.ordinal() >= GuhNpcEntity.Kind.TIMMERGUH.ordinal();
    }

    /** The role for this kind: a dispatcher by plek for the 3.0 kinds (never null), null for older kinds. */
    @Nullable
    public static NpcRole rol(GuhNpcEntity.Kind kind) {
        if (!isVerhaalKind(kind)) {
            return null;
        }
        return DISPATCHERS.computeIfAbsent(kind, Dispatcher::new);
    }

    /** The role this very NPC has (its plek, else the default of its kind, else "binnenkort"). */
    public static NpcRole van(GuhNpcEntity npc) {
        String plek = npc.roleData.getStringOr(PLEK, "");
        if (!plek.isEmpty()) {
            NpcRole r = PLEKKEN.get(npc.getKind().id() + "|" + plek);
            if (r != null) {
                return r;
            }
        }
        return STANDAARD.getOrDefault(npc.getKind(), Binnenkort.ROLE);
    }

    /** Hands every call to the role of the NPC's own plek. */
    private record Dispatcher(GuhNpcEntity.Kind kind) implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            van(npc).talk(npc, player);
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            van(npc).tick(npc);
        }

        @Nullable
        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            return van(npc).offers(npc);
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
            van(npc).antwoord(npc, player, optie);
        }
    }

    private NpcRollen() {
    }
}
