package nl.juiced.guhs.feature.droomplein;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Which islands of Guhdromenland a player has stood on (the Droomposthuisje only posts you to those). Once a second,
 * every dreaming player within {@link #STRAAL} blocks of an island's centre has visited it. Saved per player in
 * {@code GuhQuests.saved(player)["guhs_droom_bezocht"]} (island id -> true).
 */
public final class DroomBezocht {
    static final String KEY = "guhs_droom_bezocht";
    /** How close to an island's centre counts as "been there" (the islands are about 100 blocks wide). */
    static final int STRAAL = 40;

    private DroomBezocht() {
    }

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag t = saved.getCompoundOrEmpty(KEY);
        saved.put(KEY, t);
        return t;
    }

    public static boolean bezocht(ServerPlayer p, String eiland) {
        return eiland.equals(DroomEilanden.PLEIN) || data(p).getBooleanOr(eiland, false);
    }

    public static void zet(ServerPlayer p, String eiland, boolean ja) {
        if (ja) {
            data(p).putBoolean(eiland, true);
        } else {
            data(p).remove(eiland);
        }
    }

    /** The visited islands as one line ("het Sterrenkussenveld, het Wolkenbadje"), or "nog nergens". */
    static Component lijst(ServerPlayer p) {
        MutableComponent uit = null;
        for (String e : DroomEilanden.IDS) {
            if (e.equals(DroomEilanden.PLEIN) || !bezocht(p, e)) {
                continue;
            }
            Component naam = Component.translatable("gui.guhs.dromen.eiland." + e);
            uit = uit == null ? naam.copy() : uit.append(", ").append(naam);
        }
        return uit != null ? uit : Component.translatable(DroomBrievenbusBlock.T + "bus.nergens");
    }

    static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 13) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(DromenFeature.DIM);
        if (level == null) {
            return;
        }
        for (ServerPlayer p : level.players()) {
            String e = DroomEilanden.dichtstbij(p.position());
            BlockPos m = DroomEilanden.midden(e);
            if (!e.equals(DroomEilanden.PLEIN) && !bezocht(p, e)
                    && p.position().distanceToSqr(m.getX() + 0.5, p.getY(), m.getZ() + 0.5) < STRAAL * STRAAL) {
                zet(p, e, true);
            }
        }
    }
}
