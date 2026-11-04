package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.klassen.Advancement;

/**
 * Vahoeg mode support (ZONE3_PLAN §8.5, lighter after ronde 18: a replayable dungeon, no lockout, no leaderboard). The bazen slice
 * builds the altar at the dungeon door and the Vahoeg fights; this class is the key item and the gate:
 * <ul>
 *   <li>{@link #magVahoeg}(p): lv 50 (generic) and the 4th job advancement in any class, or at least lv 50;</li>
 *   <li>{@link #heeftZegel}(p) / {@link #neemZegel}(p): the Vahoegzegel ({@code vr_z3_vahoegzegel}), bought for Jachtpenningen in
 *   the penningwinkel (or dropped); placing one on the altar starts Vahoeg form for whoever is in the arena;</li>
 *   <li>rewards after a Vahoeg boss: {@link GoudenVeren#geef} and {@link GoudenVeren#rolGoudenPluimguh}.</li>
 * </ul>
 */
public final class Vahoeg {
    public static final int LEVEL = 50, ZEGEL_PRIJS = 30;

    private Vahoeg() {
    }

    public static boolean magVahoeg(ServerPlayer p) {
        return Spelers.level(p) >= LEVEL;
    }

    public static boolean heeftZegel(ServerPlayer p) {
        return p.getInventory().countItem(Z3Spul.VAHOEGZEGEL.get()) > 0;
    }

    /** Takes one seal from the bag (false + a message when there is none or the player is under lv 50). */
    public static boolean neemZegel(ServerPlayer p) {
        if (!magVahoeg(p)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.z3.systemen.vahoeg.te_laag", LEVEL).withStyle(ChatFormatting.GRAY));
            return false;
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(Z3Spul.VAHOEGZEGEL.get())) {
                s.shrink(1);
                return true;
            }
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.z3.systemen.vahoeg.geen_zegel").withStyle(ChatFormatting.GRAY));
        return false;
    }

    /** Has this player finished every class's 4th advancement? (for a future Meesterschap check) */
    static boolean alleNestvormen(ServerPlayer p) {
        for (var k : nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.alle()) {
            if (Advancement.stap(p, k.id()) < Advancement4.STAP) {
                return false;
            }
        }
        return true;
    }
}
