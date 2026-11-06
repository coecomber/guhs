package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * T-OPSLAG: the server side of the HUD additions (§6.1): every 5 ticks each player in the dimension gets their statuses
 * (with ticks left) and their target (the last thing they hit, kept {@link #DOEL_TICKS}; the client prefers what you look at)
 * with that target's statuses. Nothing is sent while there is nothing to show (one empty message clears the row).
 */
public final class HudBron {
    /** How long the last thing you hit stays your target. */
    public static final int DOEL_TICKS = 100;

    private record Doel(int id, long tijd) {
    }

    private static final Map<UUID, Doel> DOEL = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> LEEG_GESTUURD = new ConcurrentHashMap<>();

    private HudBron() {
    }

    static void init() {
        Schade.NA_KLAP.add(u -> {
            if (u.klap().bron() instanceof ServerPlayer p && u.klap().doel() != p) {
                DOEL.put(p.getUUID(), new Doel(u.klap().doel().getId(), p.level().getGameTime()));
            }
        });
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        long nu = sl.getGameTime();
        for (ServerPlayer p : sl.players()) {
            Gietbalk.peil(p, nu);
            List<OpslagPayloads.StatusRegel> eigen = regels(p);
            int doelId = -1;
            List<OpslagPayloads.StatusRegel> doelSt = List.of();
            Doel d = DOEL.get(p.getUUID());
            if (d != null) {
                Entity e = sl.getEntity(d.id());
                if (nu - d.tijd() > DOEL_TICKS || !(e instanceof LivingEntity le) || !e.isAlive()) {
                    DOEL.remove(p.getUUID());
                } else {
                    doelId = d.id();
                    doelSt = regels(le);
                }
            }
            boolean leeg = eigen.isEmpty() && doelId < 0;
            if (leeg && Boolean.TRUE.equals(LEEG_GESTUURD.get(p.getUUID()))) {
                continue;
            }
            LEEG_GESTUURD.put(p.getUUID(), leeg);
            PacketDistributor.sendToPlayer(p, new OpslagPayloads.HudInfo(eigen, doelId, doelSt));
        }
    }

    static List<OpslagPayloads.StatusRegel> regels(LivingEntity e) {
        List<OpslagPayloads.StatusRegel> l = new ArrayList<>();
        for (Map.Entry<Status, Statussen.Actief> en : Statussen.van(e).entrySet()) {
            l.add(new OpslagPayloads.StatusRegel(en.getKey().ordinal(), en.getValue().ticks, (float) en.getValue().sterkte));
        }
        return l;
    }

    static void vergeet(UUID id) {
        DOEL.remove(id);
        LEEG_GESTUURD.remove(id);
        Gietbalk.vergeet(id);
    }

    /** Dev: make this entity the player's target now. */
    public static void zetDoel(ServerPlayer p, Entity e) {   // (U-HUD: public, party click-to-target)
        DOEL.put(p.getUUID(), new Doel(e.getId(), p.level().getGameTime()));
    }
}
