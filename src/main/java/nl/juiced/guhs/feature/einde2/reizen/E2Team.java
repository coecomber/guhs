package nl.juiced.guhs.feature.einde2.reizen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2 phase B (CONTRACT_E2_B 3, 4.3, 4.4), owner: slice reizen. Team Guhvatar as it travels with a player: Aang-guh after
 * {@code guhvatar_lucht} (Momo is the player's own Momo-guh on their shoulder), Katara-guh + Sokka-guh after
 * {@code guhvatar_water}, Toph-guh after {@code guhvatar_aarde}, Zuka from the flag {@code zuka_team} (look {@code ZUKA_TEAM},
 * from the coronation on {@code ZUKA_VUURHEER}).
 * <p>
 * Whoever stands in one of the four story structures (its veil box) and is not flying sees their team round the campfire at
 * the {@code team_*} markers, made for that player alone ({@link Zicht#alleenVoor}); a guest sees the host's team instead of
 * their own. Each member says two lines of small talk per structure ({@link TeamRol}). The copies go when the player leaves,
 * takes off, or leaves the game, and a copy that was saved with its chunk is thrown away when it loads again
 * ({@link Reis#TIJDELIJK}).
 */
public final class E2Team {
    /** A member of the team: their marker name ({@code team_<id>}) and their look. */
    public enum Lid {
        AANG("aang", GuhNpcEntity.Kind.AANG_GUH), KATARA("katara", GuhNpcEntity.Kind.KATARA_GUH), SOKKA("sokka", GuhNpcEntity.Kind.SOKKA_GUH),
        TOPH("toph", GuhNpcEntity.Kind.TOPH_GUH), ZUKA("zuka", GuhNpcEntity.Kind.ZUKA_TEAM);

        private final String id;
        private final GuhNpcEntity.Kind basis;

        Lid(String id, GuhNpcEntity.Kind basis) {
            this.id = id;
            this.basis = basis;
        }

        public String id() {
            return id;
        }

        /** The look of the member in the team (Zuka: before the coronation). */
        public GuhNpcEntity.Kind basis() {
            return basis;
        }

        /** The look for this player now (CONTRACT_E2_B 7.4): Zuka is Vuurheer from the coronation (the finale) on. */
        public GuhNpcEntity.Kind kind(ServerPlayer p) {
            return this == ZUKA && E2Verhaal.finaleKlaar(p) ? GuhNpcEntity.Kind.ZUKA_VUURHEER : basis;
        }

        @Nullable
        public static Lid van(String id) {
            for (Lid l : values()) {
                if (l.id.equals(id)) {
                    return l;
                }
            }
            return null;
        }
    }

    /** What one player sees in one structure: whose team, which members (entity UUIDs). */
    private record Plaatsing(String structuur, UUID wiens, Map<Lid, UUID> leden) {
    }

    private static final Map<UUID, Plaatsing> GEZET = new ConcurrentHashMap<>();
    static final TeamRol ROL = new TeamRol();

    private E2Team() {
    }

    static void register() {
        for (String s : Einde2Ids.VERHAAL_STRUCTUREN) {
            for (Lid l : Lid.values()) {
                NpcRollen.zet(l.basis(), plek(s, l), ROL);
                if (l == Lid.ZUKA) {
                    NpcRollen.zet(GuhNpcEntity.Kind.ZUKA_VUURHEER, plek(s, l), ROL);
                }
            }
        }
    }

    /** The spot name of a member's copy ({@code <structuur>/team_<lid>}, the role's key). */
    static String plek(String structuur, Lid l) {
        return structuur + "/team_" + l.id();
    }

    /** The members of this player's team, in their order. */
    public static List<Lid> leden(ServerPlayer p) {
        List<Lid> uit = new ArrayList<>();
        if (E2Verhaal.klaar(p, E2Verhaal.LUCHT)) {
            uit.add(Lid.AANG);
        }
        if (E2Verhaal.klaar(p, E2Verhaal.WATER)) {
            uit.add(Lid.KATARA);
            uit.add(Lid.SOKKA);
        }
        if (E2Verhaal.klaar(p, E2Verhaal.AARDE)) {
            uit.add(Lid.TOPH);
        }
        if (E2Verhaal.vlag(p, E2Verhaal.VUUR, E2Verhaal.V_ZUKA_TEAM) || E2Verhaal.klaar(p, E2Verhaal.VUUR)) {
            uit.add(Lid.ZUKA);
        }
        return uit;
    }

    /**
     * einde2 fix-reizen: who rides along on Appa's back now, from the story state alone: the team of {@link #leden}, and
     * Aang-guh from the moment he rolled out of the kaasbol (chapter 1 past its step 0: he flies with you to his temple and
     * stays with you), however the player got to that step. Katara- and Sokka-guh join when chapter 2 is done, Toph-guh
     * after chapter 3, Zuka-guh with the flag {@code zuka_team} (all as in {@link #leden}).
     */
    public static List<Lid> opAppa(ServerPlayer p) {
        List<Lid> uit = leden(p);
        if (!uit.contains(Lid.AANG) && (E2Verhaal.stap(p, E2Verhaal.LUCHT) >= 1 || E2Verhaal.klaar(p, E2Verhaal.LUCHT))) {
            uit.add(0, Lid.AANG);
        }
        return uit;
    }

    /** The members of this player's team (their looks). */
    public static List<GuhNpcEntity.Kind> team(ServerPlayer p) {
        List<GuhNpcEntity.Kind> uit = new ArrayList<>();
        for (Lid l : leden(p)) {
            uit.add(l.kind(p));
        }
        return uit;
    }

    /** The team at the {@code team_*} markers of this structure, seen only by p and their guests. */
    public static void zetInStructuur(ServerPlayer p, String structuur) {
        zet(p, p, structuur);
        for (ServerPlayer g : E2Reizen.gasten(p)) {
            zet(g, p, structuur);
        }
    }

    /** Takes p's team away from this structure (null: wherever it stands), and the copies p's guests see of it. */
    public static void weg(ServerPlayer p, @Nullable String structuur) {
        haalWeg(p.level().getServer(), p.getUUID(), structuur);
        for (ServerPlayer g : E2Reizen.gasten(p)) {
            haalWeg(p.level().getServer(), g.getUUID(), structuur);
        }
    }

    // =====================================================================================================================

    /** Every second: each player in a story structure (not flying) sees the team that belongs with them there. */
    static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String s = E2Reizen.structuurBij(p);
            boolean vliegt = p.getVehicle() instanceof AppaGuhEntity appa && appa.isLucht() || Appa.vliegt(p) || E2Reizen.REIZEN.containsKey(p.getUUID())
                    && !E2Reizen.REIZEN.get(p.getUUID()).wacht();
            if (s == null || vliegt) {
                haalWeg(server, p.getUUID(), null);
                continue;
            }
            ServerPlayer host = E2Reizen.gastheer(p);
            zet(p, host != null ? host : p, s);
        }
        GEZET.keySet().removeIf(uuid -> {
            if (server.getPlayerList().getPlayer(uuid) == null) {
                Plaatsing pl = GEZET.get(uuid);
                if (pl != null) {
                    ruim(server, pl);
                }
                return true;
            }
            return false;
        });
    }

    /** Makes sure kijker sees the team of {@code wiens} in this structure (members that are missing come, others go). */
    private static void zet(ServerPlayer kijker, ServerPlayer wiens, String structuur) {
        if (!(kijker.level() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level) || !Plekken.bekend(level, structuur)) {
            return;
        }
        List<Lid> leden = leden(wiens);
        Plaatsing pl = GEZET.get(kijker.getUUID());
        if (pl != null && (!pl.structuur().equals(structuur) || !pl.wiens().equals(wiens.getUUID()))) {
            ruim(level.getServer(), pl);
            GEZET.remove(kijker.getUUID());
            pl = null;
        }
        if (pl == null) {
            pl = new Plaatsing(structuur, wiens.getUUID(), new EnumMap<>(Lid.class));
            GEZET.put(kijker.getUUID(), pl);
        }
        for (Lid l : Lid.values()) {
            UUID id = pl.leden().get(l);
            Entity e = id == null ? null : level.getEntity(id);
            boolean hoort = leden.contains(l);
            if (!hoort) {
                if (e != null) {
                    e.discard();
                }
                pl.leden().remove(l);
                continue;
            }
            if (e instanceof GuhNpcEntity npc && npc.isAlive()) {
                if (npc.getKind() != l.kind(wiens)) {
                    npc.setKind(l.kind(wiens));                   // (the coronation happened: Zuka gets his crown)
                }
                continue;
            }
            GuhNpcEntity npc = maak(level, kijker, structuur, l, l.kind(wiens));
            if (npc != null) {
                pl.leden().put(l, npc.getUUID());
            }
        }
    }

    /** One member's copy at their marker, for one player. Null: the spot is not loaded (next second again). */
    @Nullable
    private static GuhNpcEntity maak(ServerLevel level, ServerPlayer kijker, String structuur, Lid l, GuhNpcEntity.Kind kind) {
        Plekken.Plek plek = Plekken.plek(structuur, "team_" + l.id());
        BlockPos pos = Plekken.wereld(level, structuur, "team_" + l.id());
        if (plek == null || pos == null || !level.hasChunkAt(pos)) {
            return null;
        }
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (npc == null) {
            return null;
        }
        npc.setKind(kind);
        npc.roleData.putString(NpcRollen.PLEK, plek(structuur, l));
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, plek.yaw(), 0f);
        npc.setYHeadRot(plek.yaw());
        npc.setYBodyRot(plek.yaw());
        npc.setInvulnerable(true);
        npc.addTag(Reis.TIJDELIJK);
        Zicht.alleenVoor(npc, kijker.getUUID());
        level.addFreshEntity(npc);
        return npc;
    }

    private static void haalWeg(@Nullable MinecraftServer server, UUID kijker, @Nullable String structuur) {
        Plaatsing pl = GEZET.get(kijker);
        if (pl == null || structuur != null && !pl.structuur().equals(structuur)) {
            return;
        }
        GEZET.remove(kijker);
        if (server != null) {
            ruim(server, pl);
        }
    }

    private static void ruim(MinecraftServer server, Plaatsing pl) {
        ServerLevel level = server.getLevel(GuheindeFeature.GUHEINDE);
        if (level == null) {
            return;
        }
        for (UUID id : pl.leden().values()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
    }

    /** (tests, dev) the entities this player sees as their team right now. */
    static List<Entity> gezet(ServerPlayer kijker) {
        List<Entity> uit = new ArrayList<>();
        Plaatsing pl = GEZET.get(kijker.getUUID());
        if (pl != null && kijker.level() instanceof ServerLevel level) {
            for (UUID id : pl.leden().values()) {
                Entity e = level.getEntity(id);
                if (e != null) {
                    uit.add(e);
                }
            }
        }
        return uit;
    }

    static void leeg() {
        GEZET.clear();
    }
}
