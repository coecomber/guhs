package nl.juiced.guhs.feature.vadsrijk.gilden;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * U-GILDEN: "to our castle" from any hub (town area) and back with the teleport stone (V1 "teleportsteen"). One cooldown per
 * character ({@link BondXp#tpMinuten}: 30 min, shorter with bond level) shared by hub -> castle and stone -> another hub;
 * "back to where I came from" and the walk between the square and the castles are free.
 */
public final class BondTeleport {
    private BondTeleport() {
    }

    /** ms until the next paid teleport (0 = now). */
    public static long wacht(ServerPlayer p) {
        return Math.max(0, BondXp.tag(p).getLongOr("tp", 0L) - BondXp.KLOK.getAsLong());
    }

    static String tijd(long ms) {
        long s = (ms + 999) / 1000;
        return s >= 60 ? (s / 60) + " min" : s + " s";
    }

    private static void start(ServerPlayer p, Bond b) {
        BondXp.tag(p).putLong("tp", BondXp.KLOK.getAsLong() + BondXp.tpMinuten(b.level()) * 60_000L);
        Spelers.opgeslagen(p);
    }

    /** {@code /bond thuis}: from a town (or anywhere in the valley) to the castle. */
    public static BondActies.Uitslag thuis(ServerPlayer p, boolean dev) {
        Bond b = Bonden.van(p);
        if (b == null) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.fout.geen_bond");
        }
        BlockPos a = Vallei.kasteelAankomst(b);
        if (a == null) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.fout.geen_kasteel");
        }
        boolean inVallei = Vallei.in(p.getX(), p.getZ());
        if (!dev) {
            if (!Vadsrijk.in(p)) {
                return BondActies.Uitslag.nee("vadsrijk.gilden.fout.rijk");
            }
            if (!inVallei && !BondPaneel.inStad(p)) {
                return BondActies.Uitslag.nee("vadsrijk.gilden.fout.stad");
            }
            if (!inVallei && wacht(p) > 0) {
                return BondActies.Uitslag.nee("vadsrijk.gilden.tp.wacht", tijd(wacht(p)));
            }
        }
        if (!inVallei) {
            CompoundTag t = BondXp.tag(p);
            t.putDouble("terug_x", p.getX());
            t.putDouble("terug_y", p.getY());
            t.putDouble("terug_z", p.getZ());
            if (!dev) {
                start(p, b);
            }
        }
        Kasteel.bouwAlsNodig(p.level().getServer(), b);
        Vallei.zet(p, a, 180);
        Kasteel.trofeeen(p.level().getServer(), b);
        return BondActies.Uitslag.ja("vadsrijk.gilden.tp.kasteel", b.naam);
    }

    /** {@code /bond reis <doel>} from the teleport stone (used it within the last minute). */
    public static BondActies.Uitslag reis(ServerPlayer p, String doel, boolean dev) {
        Bond b = Bonden.van(p);
        if (b == null) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.fout.geen_bond");
        }
        if (!dev && BondXp.KLOK.getAsLong() - BondXp.tag(p).getLongOr("steen", 0L) > 60_000L) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.fout.steen");
        }
        CompoundTag t = BondXp.tag(p);
        if (doel.equals("plein")) {
            Vallei.zet(p, Vallei.pleinAankomst(), 180);
            return BondActies.Uitslag.ja("vadsrijk.gilden.tp.plein");
        }
        if (doel.equals("terug")) {
            if (!t.contains("terug_x")) {
                return BondActies.Uitslag.nee("vadsrijk.gilden.fout.geen_terug");
            }
            BlockPos pos = BlockPos.containing(t.getDoubleOr("terug_x", 0), t.getDoubleOr("terug_y", 0), t.getDoubleOr("terug_z", 0));
            t.remove("terug_x");
            t.remove("terug_y");
            t.remove("terug_z");
            Vallei.zet(p, pos, p.getYRot());
            return BondActies.Uitslag.ja("vadsrijk.gilden.tp.terug");
        }
        for (BondPaneel.Hub h : BondPaneel.HUBS) {
            if (!h.id().equals(doel)) {
                continue;
            }
            if (!BondPaneel.hubOpen(p, h)) {
                return BondActies.Uitslag.nee("vadsrijk.gilden.fout.hub_dicht");
            }
            if (!dev && wacht(p) > 0) {
                return BondActies.Uitslag.nee("vadsrijk.gilden.tp.wacht", tijd(wacht(p)));
            }
            BlockPos pos = h.plek().get();
            if (pos == null) {
                return BondActies.Uitslag.nee("vadsrijk.gilden.fout.hub_dicht");
            }
            if (!dev) {
                start(p, b);
            }
            net.minecraft.server.level.ServerLevel sl = Vadsrijk.level(p.level().getServer());
            if (sl != null) {   // (a free floor next to the square's POI: never on a campfire or a fountain)
                Vallei.laad(sl, pos.offset(-8, 0, -8), 16, 16);
                pos = nl.juiced.guhs.feature.vadsrijk.npc.Npcs.staanPlek(sl, pos.offset(2, 0, 2));
            }
            Vallei.zet(p, pos, p.getYRot());
            return BondActies.Uitslag.ja("vadsrijk.gilden.tp.hub", Component.translatable("vadsrijk.gilden.hub." + h.id()));
        }
        return BondActies.Uitslag.nee("vadsrijk.gilden.fout.hub_dicht");
    }

    static void meld(ServerPlayer p, BondActies.Uitslag u) {
        if (!u.tekst().getString().isEmpty()) {
            p.sendSystemMessage(u.tekst().copy().withStyle(u.ok() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
        }
    }
}
