package nl.juiced.guhs.feature.ringh4;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * bbq2 (ring-h4): the boat trip down the Guhduin, past the Arguhnath (DESIGN_130 4, chapter 4: "elf boats on the sauce
 * river past two giant guh statues (calm stretch)"; Leguhlas guides).
 * <ul>
 *   <li>Step 6 of the chapter: click the guide boat at the jetty (or ask Leguhlas): a trip boat appears where it lies,
 *       you sit in it, three seconds later it leaves. A friend who clicks it in those seconds comes along (two seats).
 *       Leguhlas paddles at the stern, Gimguh grumbles at the prow (drawn in the boat; the two on the quay are out of sight
 *       for as long as the trip lasts), and they talk you past the statues. At the landing everybody is put ashore; a
 *       player for whom this was step 6 has finished the chapter.</li>
 *   <li>Afterwards the trip is free for whoever finished the chapter, and the boat at the landing sails back up.</li>
 *   <li>One trip at a time per boat: the next player waits about a minute. The story is per player, the boat is not.</li>
 * </ul>
 * Nobody can be hurt on the way: the river is kaassaus (harmless), the boat follows its path by itself, and getting out
 * half-way is refused ({@link RingH4Events}).
 */
public final class Vaart {
    static final String T = "quest.guhs.ringh4.vaart.";
    /** {how far along the trip (0..1), who says it (0 Leguhlas, 1 Gimguh, 2 Sam-guh), text}: the talk on the way down. */
    private static final Object[][] PRAATJES = {
            {0.03, 0, "af"}, {0.14, 1, "water"}, {0.24, 0, "rivier"}, {0.36, 0, "kijk"}, {0.45, 1, "groot"}, {0.52, 0, "koningen"},
            {0.60, 2, "sam"}, {0.70, 1, "tel"}, {0.78, 0, "tel_terug"}, {0.90, 0, "aanleg"}};
    private static final String[] SPREKERS = {"entity.guhs.guh_npc.leguhlas", "entity.guhs.guh_npc.gimguh", "entity.guhs.guh.sam_guh"};

    /** "&lt;Leguhlas&gt; text", only for this player (the guides in the boat are drawn, they are no entities). */
    static void zeg(ServerPlayer p, int spreker, String key, Object... args) {
        MutableComponent line = Component.literal("<").append(Component.translatable(SPREKERS[spreker])).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key, args).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(line);
    }

    /**
     * The player wants to get into this boat. A waiting trip boat: get in. The guide boat: a trip down the river, for a
     * player at step 6 or later. The boat at the landing: back up, for whoever finished the chapter. True when the player
     * sits in a boat now.
     */
    public static boolean stapIn(ServerPlayer p, ElfenbootjeEntity boot) {
        if (!Duwtje.mag(p) || p.isPassenger() || boot.isWeg()) {
            return false;
        }
        Verhaallijn lijn = RingH4Feature.LIJN;
        switch (boot.soort()) {
            case ElfenbootjeEntity.RIT -> {
                if (boot.vaart() || boot.getPassengers().size() >= ElfenbootjeEntity.PLAATSEN || !p.startRiding(boot, true, true)) {
                    return false;
                }
                p.sendOverlayMessage(Component.translatable(T + "erbij").withStyle(ChatFormatting.AQUA));
                return true;
            }
            case ElfenbootjeEntity.DECO -> {
                p.sendOverlayMessage(Component.translatable(T + "deco").withStyle(ChatFormatting.AQUA));
                return false;
            }
            case ElfenbootjeEntity.TERUG -> {
                if (!lijn.klaar(p)) {
                    p.sendOverlayMessage(Component.translatable(T + "terug_nee").withStyle(ChatFormatting.AQUA));
                    return false;
                }
                return vertrek(p, boot, true);
            }
            default -> {
                if (!lijn.aanDeBeurt(p) || lijn.stap(p) < 6) {
                    zeg(p, 0, T + "nog_niet");
                    return false;
                }
                return vertrek(p, boot, false);
            }
        }
    }

    /** A trip boat appears where this moored boat lies, with the player in it. */
    private static boolean vertrek(ServerPlayer p, ElfenbootjeEntity thuis, boolean terug) {
        ServerLevel level = p.level();
        Boomstad.Kopie kopie = Boomstad.bij(level, thuis.blockPosition());
        if (kopie == null) {
            return false;
        }
        List<Vec3> route = kopie.route(terug);
        ElfenbootjeEntity rit = RingH4Feature.ELFENBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (rit == null || route.size() < 2) {
            return false;
        }
        rit.maakRit(route, kopie.punt(terug ? "steiger" : "aanleg"), thuis.getUUID(), terug, !terug);
        rit.setInvulnerable(true);
        level.addFreshEntity(rit);
        if (!p.startRiding(rit, true, true)) {
            rit.discard();
            return false;
        }
        thuis.setWeg(true);
        if (!terug) {
            gidsen(thuis, true);
            zeg(p, 0, T + "instappen");
        } else {
            p.sendOverlayMessage(Component.translatable(T + "terug").withStyle(ChatFormatting.AQUA));
        }
        Sam.wacht(p, 20 * 90);
        return true;
    }

    /** The trip leaves the jetty. */
    static void vertrokken(ElfenbootjeEntity rit) {
        for (Entity e : rit.getPassengers()) {
            if (e instanceof ServerPlayer p) {
                Sam.wacht(p, 20 * 90);
                Ring.behaald(p, "ring_h4_aan_boord");
            }
        }
    }

    /** (every tick of a sailing trip) the talk on the way; returns the new phase. */
    static int onderweg(ElfenbootjeEntity rit, int fase) {
        if (rit.terug || fase >= PRAATJES.length || rit.fractie() < (double) PRAATJES[fase][0]) {
            return fase;
        }
        Object[] praatje = PRAATJES[fase];
        for (Entity e : rit.getPassengers()) {
            if (e instanceof ServerPlayer p && ((int) praatje[1] != 2 || Sam.looptMee(p))) {
                zeg(p, (int) praatje[1], T + praatje[2]);
            }
        }
        return fase + 1;
    }

    /**
     * The trip is over: the passengers stand on the landing (arrived) or back on the jetty they left from (broken off),
     * the moored boat lies there again, the guides are back on the quay. Arrived down the river at step 6: the chapter
     * is done.
     */
    static void voorbij(ElfenbootjeEntity rit, List<Entity> passagiers, boolean aangekomen) {
        ServerLevel level = (ServerLevel) rit.level();
        List<Vec3> pad = rit.pad();
        Vec3 uit = aangekomen && rit.uitstap != null ? rit.uitstap : pad != null ? pad.get(0).add(0, 0.6, 0) : rit.position();
        Boomstad.Kopie kopie = Boomstad.bij(level, rit.blockPosition());
        if (!aangekomen && kopie != null) {
            uit = kopie.punt(rit.terug ? "aanleg" : "steiger");
        }
        int i = 0;
        for (Entity e : passagiers) {
            Vec3 plek = uit.add((i % 2) * 0.8, 0.05, (i / 2) * 0.8);
            i++;
            e.teleportTo(plek.x, plek.y, plek.z);
            e.resetFallDistance();
            if (!(e instanceof ServerPlayer p)) {
                continue;
            }
            Sam.kom(p);
            if (!aangekomen) {
                continue;
            }
            if (rit.terug) {
                p.sendOverlayMessage(Component.translatable(T + "terug_er").withStyle(ChatFormatting.AQUA));
                continue;
            }
            Ring.behaald(p, "ring_h4_gevaren");
            if (Ring.aanZet(p, RingH4Feature.LIJN, 6)) {
                RingH4Events.klaar(p, true);
            } else {
                zeg(p, 0, T + "nog_eens");
            }
        }
        if (rit.thuis != null && level.getEntity(rit.thuis) instanceof ElfenbootjeEntity thuis) {
            thuis.setWeg(false);
            gidsen(thuis, false);
        }
    }

    /** Leguhlas and Gimguh on the quay near this boat are out of sight (they sit in the trip boat) or back. */
    static void gidsen(ElfenbootjeEntity thuis, boolean weg) {
        if (thuis.soort() != ElfenbootjeEntity.GIDS) {
            return;
        }
        for (GuhNpcEntity npc : thuis.level().getEntitiesOfClass(GuhNpcEntity.class, new AABB(thuis.blockPosition()).inflate(24, 8, 24),
                n -> RingH4Feature.STEIGER.equals(n.roleData.getStringOr(NpcRollen.PLEK, "")))) {
            if (npc.isInvisible() != weg) {
                npc.setInvisible(weg);
            }
        }
    }

    /** (the moored guide boat, now and then) the guides stand on the quay whenever their boat lies there. */
    static void gidsenTerug(ElfenbootjeEntity thuis) {
        gidsen(thuis, false);
    }

    /** The moored boat of this kind near a spot (the guide boat for Leguhlas' "stap maar in"). */
    @Nullable
    static ElfenbootjeEntity boot(ServerLevel level, Vec3 bij, int soort) {
        ElfenbootjeEntity beste = null;
        for (ElfenbootjeEntity b : level.getEntitiesOfClass(ElfenbootjeEntity.class, AABB.ofSize(bij, 48, 16, 48), b -> b.soort() == soort)) {
            if (beste == null || b.distanceToSqr(bij) < beste.distanceToSqr(bij)) {
                beste = b;
            }
        }
        return beste;
    }

    private Vaart() {
    }
}
