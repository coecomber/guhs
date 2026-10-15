package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.reizen.E2Team;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * Tea with the team (DESIGN 5 "Wat je overhoudt": they come for tea in your guhhuisje ONLY when Oom Miroh's theeset stands
 * inside that huisje's area). Every few seconds: for every theeset that is loaded, whose guhhuisje area it stands in; when
 * that huisje's owner is near it, the owner's team ({@link E2Team#team}: whoever travels with them by now) sits round the
 * set, for the owner alone. They go again when the owner walks away, or when the set is gone.
 */
public final class H3Theebezoek {
    public static final int ELKE = 100;
    public static final double DICHTBIJ = 16, WEG = 24;
    /** The role spot of a tea guest (H3Rollen: a word about the tea). */
    public static final String PLEK = "einde2_h3_aarde/theebezoek";

    /** owner -> the set their team sits at. */
    private static final Map<UUID, BlockPos> AAN = new ConcurrentHashMap<>();

    private H3Theebezoek() {
    }

    static void opTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % ELKE != 17) {
            return;
        }
        for (Map.Entry<UUID, BlockPos> e : AAN.entrySet()) {
            ServerPlayer eigenaar = event.getServer().getPlayerList().getPlayer(e.getKey());
            if (eigenaar == null || !e.getValue().closerToCenterThan(eigenaar.position(), WEG)
                    || !(eigenaar.level().getBlockEntity(e.getValue()) instanceof H3Blokken.TheesetBlockEntity)) {
                if (eigenaar != null) {
                    H3Acteurs.wegAlle(eigenaar, "thee_");
                }
                AAN.remove(e.getKey());
            }
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (BlockPos set : H3Blokken.TheesetBlockEntity.in(level)) {
                ServerPlayer eigenaar = eigenaar(level, set);
                if (eigenaar != null && !AAN.containsKey(eigenaar.getUUID()) && set.closerToCenterThan(eigenaar.position(), DICHTBIJ)) {
                    kom(eigenaar, level, set);
                }
            }
        }
    }

    /** The owner of the guhhuisje whose area holds this spot, when they are online in this level (null: none). */
    @Nullable
    static ServerPlayer eigenaar(ServerLevel level, BlockPos set) {
        for (Huisje h : Huisjes.rond(level, set, Huisjes.BEREIK + 2)) {
            if (h.inGebied(set) && level.getPlayerByUUID(h.eigenaar()) instanceof ServerPlayer p && p.level() == level) {
                return p;
            }
        }
        return null;
    }

    /** The team sits down round the set: one on each side that has room, facing the tea. */
    static int kom(ServerPlayer eigenaar, ServerLevel level, BlockPos set) {
        List<GuhNpcEntity.Kind> team = E2Team.team(eigenaar);
        if (team.isEmpty()) {
            return 0;
        }
        int n = 0;
        Direction[] kanten = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int i = 0; i < team.size() && n < kanten.length * 2; i++) {
            BlockPos plek = null;
            for (int k = n; k < kanten.length * 2 && plek == null; k++) {
                BlockPos b = set.relative(kanten[k % 4], 1 + k / 4 + 1);
                if (level.getBlockState(b).getCollisionShape(level, b).isEmpty() && level.getBlockState(b.above()).getCollisionShape(level, b.above()).isEmpty()) {
                    plek = b;
                    n = k + 1;
                }
            }
            if (plek == null) {
                break;
            }
            Vec3 waar = Vec3.atBottomCenterOf(plek);
            float yaw = Cutscene.yawVan(Vec3.atCenterOf(set).subtract(waar));
            H3Acteurs.npc(eigenaar, "thee_" + i, team.get(i), level, waar, yaw, PLEK, false);
        }
        AAN.put(eigenaar.getUUID(), set.immutable());
        return n;
    }
}
