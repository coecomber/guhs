package nl.juiced.guhs.feature.einde2.spverhaal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Sluiers;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 4.2), the skelet: the friendly side of the gate. Guhdalfs sluier ({@link Sluiers}) stands
 * round Bikinibodem and Schelpenstad for every player who has not finished the Guhvatar story (pink smoke you see from
 * afar, a gentle shove back, the Sponsguh line over the hotbar: {@code gui.guhs.einde2_sp_verhaal.poort.balk}). When such a
 * player comes within {@value #NADER} blocks of either box, he also gets, at most once per {@value #RUST} ticks, three chat
 * lines: Sponsguh shouting through the bubbles, and plainly what to do first. Never damage, never an item.
 */
public final class SpPoort {
    /** How near (blocks, sideways, to the box of the build with its sluier) the explanation comes. */
    public static final int NADER = 40;
    /** At most once per this many ticks per player. */
    public static final int RUST = 20 * 60 * 5;

    private static final Map<UUID, Long> GEZEGD = new ConcurrentHashMap<>();

    private SpPoort() {
    }

    /** Every 20 ticks per player (SpVerhaal.opTick). */
    static void tick(ServerPlayer p) {
        if (!GuheindeFeature.isGuheinde(p.level()) || SpVerhaal.open(p) || Sluiers.passeert(p)) {
            return;
        }
        ServerLevel level = p.level();
        for (String s : Einde2Ids.SPONS_STRUCTUREN) {
            BoundingBox doos = Plekken.bekend(level, s) ? Plekken.doos(level, s) : null;
            if (doos == null) {
                continue;
            }
            int r = SpVerhaal.SLUIER_RAND + NADER;
            if (p.getX() >= doos.minX() - r && p.getX() <= doos.maxX() + 1 + r && p.getZ() >= doos.minZ() - r && p.getZ() <= doos.maxZ() + 1 + r
                    && p.getY() >= doos.minY() - r && p.getY() <= doos.maxY() + 1 + r) {
                vertel(p, level.getServer().getTickCount());
                return;
            }
        }
    }

    /** The three lines (at most once per {@link #RUST}); true when they were said now. */
    static boolean vertel(ServerPlayer p, long nu) {
        Long toen = GEZEGD.get(p.getUUID());
        if (toen != null && nu - toen < RUST) {
            return false;
        }
        GEZEGD.put(p.getUUID(), nu);
        p.sendSystemMessage(Component.translatable(SpVerhaal.P + "poort.1").withStyle(ChatFormatting.LIGHT_PURPLE));
        p.sendSystemMessage(Component.translatable(SpVerhaal.P + "poort.2").withStyle(ChatFormatting.YELLOW));
        p.sendSystemMessage(Component.translatable(SpVerhaal.P + "poort.3").withStyle(ChatFormatting.GRAY));
        return true;
    }

    static void vergeet(UUID u) {
        GEZEGD.remove(u);
    }
}
