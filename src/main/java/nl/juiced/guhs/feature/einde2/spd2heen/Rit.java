package nl.juiced.guhs.feature.einde2.spd2heen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.Knabbelburgermobiel;
import nl.juiced.guhs.feature.einde2.sponsguh.KnabbelburgermobielEntity;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;

/**
 * einde2 "Sponsguh", slice sp_d2_heen: the drive of part 2 step {@code K_RIT} (CONTRACT_E2_C 7.6). The player's own
 * Knabbelburgermobiel ({@link Knabbelburgermobiel#zet} with {@code voor} = the player: only they drive) stands at
 * {@code bikinibodem/mobiel_start}, their own Sponsguh and Patguh wait beside it; near the car the scene {@code rit} tells
 * who drives (you), then you sit at the wheel and the two ride along ({@link KnabbelburgermobielEntity#zetMee}). Out of the
 * town gate, along the road to {@code schelpenstad/kroeg_parkeer}: arriving there (by car, or on foot) parks the car neatly in
 * the middle bay and plays {@code kroeg}; the step moves on in its {@code daarna}.
 * <ul>
 *   <li>the variant {@code 3_b} ({@link #rijdt}: the line's flag {@value #RIJDT}) once you got in; cleared when the step ends;</li>
 *   <li>getting out early: the car waits where it is with the two in it, a line in the chat says so; a lost car (another
 *       dimension, a restart) comes back at {@code mobiel_start} when you stand there;</li>
 *   <li>travel banter in the chat (the town gate, the seam, the last stretch), once per player.</li>
 * </ul>
 */
final class Rit {
    /** The line's flag while the player drives (the variant 3_b). */
    static final String RIJDT = "sp_d2_heen_rijdt";
    /** The car's yaw at the start (east, along the Hoofdstraat) and in the parking bay (north, nose to the bar). */
    static final float YAW_START = -90f, YAW_PARKEER = 180f;
    /** The player's own riders and waiters (not saved: tagged temporary). */
    private static final Map<UUID, List<Entity>> EIGEN = new ConcurrentHashMap<>();
    /** Was the player at the wheel at the last look (to say "we wait here" once when they get out). */
    private static final Map<UUID, Boolean> REED = new ConcurrentHashMap<>();

    private Rit() {
    }

    static boolean rijdt(ServerPlayer p) {
        return SpVerhaal.KROON_LIJN.vlag(p, RIJDT);
    }

    /** Every 10 ticks while the player's step is K_RIT and they watch nothing (SpD2Heen.regie). */
    static void regie(ServerPlayer p) {
        KnabbelburgermobielEntity auto = Knabbelburgermobiel.van(p);
        Vec3 parkeer = D2Plek.staan(p, D2Plek.SP, "kroeg_parkeer");
        if (parkeer != null && (D2Plek.dichtbij(p.position(), parkeer, 7) || auto != null && p.getVehicle() == auto && D2Plek.dichtbij(auto.position(), parkeer, 7))) {
            aankomst(p);
            return;
        }
        Vec3 start = D2Plek.staan(p, D2Plek.BB, "mobiel_start");
        if (!rijdt(p)) {
            if (start == null || p.position().distanceTo(start) > 64) {
                return;                                              // (the car and the two wait for you in front of De Krokante Knabbel)
            }
            if (auto == null || auto.position().distanceTo(start) > 12) {
                auto = zetAuto(p);
            }
            if (auto == null) {
                return;
            }
            wachters(p, auto);
            if (p.getVehicle() == auto || p.position().distanceTo(auto.position()) <= 4.5) {
                SpD2Heen.speel(p, D2Scenes.RIT, D2Plek.BB, "mobiel_start", Rit::instappen);
            }
            return;
        }
        boolean rijdtNu = auto != null && p.getVehicle() == auto;
        if (rijdtNu) {
            meerijders(p, auto);
            babbel(p);
        } else {
            if (Boolean.TRUE.equals(REED.get(p.getUUID()))) {
                p.sendSystemMessage(Component.translatable(SpD2Heen.Q + "rit.uitgestapt").withStyle(ChatFormatting.WHITE));
            }
            if (auto == null && start != null && D2Plek.dichtbij(p.position(), start, 10)) {
                auto = zetAuto(p);
                p.sendSystemMessage(Component.translatable(SpD2Heen.Q + "rit.nieuw").withStyle(ChatFormatting.WHITE));
            }
            if (auto != null && p.position().distanceTo(auto.position()) <= 6) {
                p.sendOverlayMessage(Component.translatable(SpD2Heen.Q + "rit.instappen").withStyle(ChatFormatting.GOLD));
            } else if (auto == null || p.position().distanceTo(auto.position()) > 64) {
                weg(p);                                              // (they come back when you sit at the wheel again)
            }
        }
        REED.put(p.getUUID(), rijdtNu);
    }

    /** The player's car at mobiel_start (a car they had before disappears). */
    @Nullable
    static KnabbelburgermobielEntity zetAuto(ServerPlayer p) {
        Vec3 start = D2Plek.staan(p, D2Plek.BB, "mobiel_start");
        ServerLevel level = D2Plek.guheinde(p);
        if (start == null || level == null) {
            return null;
        }
        weg(p);
        return Knabbelburgermobiel.zet(level, start, YAW_START, p);
    }

    /** After the scene rit: at the wheel, the two beside you, the flag on. */
    static void instappen(ServerPlayer p) {
        if (!SpD2Heen.opStap(p, SpVerhaal.K_RIT)) {
            return;
        }
        KnabbelburgermobielEntity auto = Knabbelburgermobiel.van(p);
        if (auto == null) {
            auto = zetAuto(p);
        }
        if (auto == null) {
            return;
        }
        weg(p);
        SpVerhaal.KROON_LIJN.vlag(p, RIJDT, true);
        if (p.getVehicle() != auto) {
            p.teleportTo(p.level(), auto.getX(), auto.getY() + 0.2, auto.getZ(), java.util.Set.of(), auto.getYRot(), 0f, true);
            auto.stapIn(p);
        }
        meerijders(p, auto);
        REED.put(p.getUUID(), true);
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);          // (the objective line shows 3_b at once)
    }

    /** Arrival at De Ruige Kroeg: out of the car, the car parked in the middle bay, the scene kroeg; then the step. */
    static void aankomst(ServerPlayer p) {
        Vec3 parkeer = D2Plek.staan(p, D2Plek.SP, "kroeg_parkeer");
        ServerLevel level = D2Plek.guheinde(p);
        if (parkeer == null || level == null) {
            return;
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        weg(p);
        Knabbelburgermobiel.zet(level, parkeer, YAW_PARKEER, p);     // (the car stays parked here for the rest of the trip)
        REED.remove(p.getUUID());
        SpD2Heen.speel(p, D2Scenes.KROEG, D2Plek.SP, "kroeg_parkeer", q -> {
            SpVerhaal.KROON_LIJN.vlag(q, RIJDT, false);
            SpVerhaal.KROON_LIJN.verder(q, SpVerhaal.K_RIT);
        });
    }

    /** Sponsguh and Patguh wait beside the car (the player's own copies). */
    private static void wachters(ServerPlayer p, KnabbelburgermobielEntity auto) {
        List<Entity> eigen = EIGEN.computeIfAbsent(p.getUUID(), u -> new ArrayList<>());
        eigen.removeIf(e -> !e.isAlive());
        if (eigen.size() >= 2) {
            return;
        }
        eigen.forEach(Entity::discard);
        eigen.clear();
        Vec3 a = auto.position();
        eigen.add(wezen(p, E2SponsguhFeature.SPONSGUH.get().create(p.level(), EntitySpawnReason.TRIGGERED), a.add(-1.1, 0, -2.2), 0f));
        eigen.add(wezen(p, E2SponsguhFeature.PATGUH.get().create(p.level(), EntitySpawnReason.TRIGGERED), a.add(1.1, 0, -2.2), 0f));
        eigen.removeIf(java.util.Objects::isNull);
    }

    /** The two ride along (made again when they are gone: a riding copy is never saved). */
    private static void meerijders(ServerPlayer p, KnabbelburgermobielEntity auto) {
        List<Entity> eigen = EIGEN.computeIfAbsent(p.getUUID(), u -> new ArrayList<>());
        eigen.removeIf(e -> !e.isAlive() || e.getVehicle() != auto);
        if (eigen.size() >= 2) {
            return;
        }
        eigen.forEach(Entity::discard);
        eigen.clear();
        Vec3 a = auto.position();
        for (SponsguhWezen w : new SponsguhWezen[] {E2SponsguhFeature.SPONSGUH.get().create(p.level(), EntitySpawnReason.TRIGGERED),
                E2SponsguhFeature.PATGUH.get().create(p.level(), EntitySpawnReason.TRIGGERED)}) {
            Entity e = wezen(p, w, a, auto.getYRot());
            if (e != null && auto.zetMee(e)) {
                eigen.add(e);
            } else if (e != null) {
                e.discard();
            }
        }
    }

    @Nullable
    private static Entity wezen(ServerPlayer p, @Nullable SponsguhWezen w, Vec3 plek, float yaw) {
        if (w == null) {
            return null;
        }
        w.snapTo(plek.x, plek.y, plek.z, yaw, 0);
        w.setYHeadRot(yaw);
        w.zetStil(true);
        SpD2Heen.eigen(w, p);
        p.level().addFreshEntity(w);
        return w;
    }

    /** Chat on the way, once per player per spot. */
    private static void babbel(ServerPlayer p) {
        if (D2Plek.bij(p, D2Plek.BB, "stadspoort", 8) && SpVerhaal.KROON_LIJN.eenmalig(p, "sp_d2_heen_rit_poort")) {
            p.sendSystemMessage(Component.translatable(SpD2Heen.Q + "rit.poort"));
        } else if (D2Plek.bij(p, D2Plek.SP, "weg_naad", 8) && SpVerhaal.KROON_LIJN.eenmalig(p, "sp_d2_heen_rit_naad")) {
            p.sendSystemMessage(Component.translatable(SpD2Heen.Q + "rit.naad"));
        } else if (D2Plek.bij(p, D2Plek.SP, "kroeg_parkeer", 30) && SpVerhaal.KROON_LIJN.eenmalig(p, "sp_d2_heen_rit_bijna")) {
            p.sendSystemMessage(Component.translatable(SpD2Heen.Q + "rit.bijna"));
        }
    }

    /** The player's riders and waiters gone (the car stays). */
    static void weg(ServerPlayer p) {
        List<Entity> eigen = EIGEN.remove(p.getUUID());
        if (eigen != null) {
            eigen.forEach(Entity::discard);
        }
    }

    static void vergeet(UUID u) {
        List<Entity> eigen = EIGEN.remove(u);
        if (eigen != null) {
            eigen.forEach(Entity::discard);
        }
        REED.remove(u);
    }
}
