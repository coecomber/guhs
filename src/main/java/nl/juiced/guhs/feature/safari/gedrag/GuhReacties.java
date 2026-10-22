package nl.juiced.guhs.feature.safari.gedrag;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.safari.SafariDier;

/**
 * Round 3 (ronde 45): your own guhs react to the park animals. A tamed guh near an animal now and then does the reaction of
 * that species: it is startled by the crocodile, waves at the penguins, stands up like a meerkat ({@link Emote#RECHTOP}),
 * or climbs onto a capybara for a little ride ({@link SafariDier#neemMee}). Pure charm, with a cooldown per guh.
 * G1 (design 1.2): now and then it also says something about it to its owner ("Njeg, een olifant!"), at most once a
 * minute per owner ({@link #lijn}).
 */
public final class GuhReacties {
    /** How close a guh must be, and how long (ticks) a guh waits before its next reaction. */
    static final double AFSTAND = 6;
    static final int COOLDOWN = 20 * 45;
    private static final Map<GuhEntity, Long> LAATST = new WeakHashMap<>();
    /** G1: how long (ticks) an owner hears no new line, how far away the owner may be, and lines per kind. */
    static final int LIJN_COOLDOWN = 20 * 60, LIJNEN = 3;
    static final double LIJN_AFSTAND = 24;
    private static final Map<UUID, Long> LAATSTE_LIJN = new ConcurrentHashMap<>();

    /** The reaction to a species (null: none). */
    @Nullable
    public static Emote reactie(String soort, boolean baby) {
        return switch (soort) {
            case "krokodil", "sabeltandtijger", "ijsbeer", "leeuw", "nijlpaard" -> Emote.SCHRIKKEN;
            case "pinguin", "flamingo", "toekan", "zeehond", "otter", "ooievaar", "gibbon" -> Emote.ZWAAIEN;
            case "stokstaartje" -> Emote.RECHTOP;
            case "olifant", "giraffe", "zebra", "kangoeroe" -> Emote.VAHOEG;
            default -> baby ? Emote.HARTJES : null;
        };
    }

    /** Called every couple of seconds by an animal with a player nearby. */
    public static void kijk(ServerLevel level, SafariDier dier) {
        if (dier.slaapt() || level.getRandom().nextInt(3) != 0) {
            return;
        }
        long nu = level.getGameTime();
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, dier.getBoundingBox().inflate(AFSTAND),
                g -> g.isTame() && g.getOwner() != null && g.isAlive() && !g.isPassenger())) {
            Long t = LAATST.get(guh);
            if (t != null && nu - t < COOLDOWN) {
                continue;
            }
            if ("capibara".equals(dier.soort()) && !dier.isBaby() && dier.neemMee(guh)) {
                LAATST.put(guh, nu);
                lijn(level, guh, dier, "rit");
                return;
            }
            Emote e = reactie(dier.soort(), dier.isBaby());
            if (e == null) {
                continue;
            }
            guh.getLookControl().setLookAt(dier, 30f, 30f);
            if (guh.emotes.start(e, false, GuhEmotes.Source.SELF)) {
                LAATST.put(guh, nu);
                lijn(level, guh, dier, soortLijn(e, dier.isBaby()));
                return;
            }
        }
    }

    /** The kind of line that goes with a reaction: "eng", "zwaai", "groot", "baby" or "zie". */
    static String soortLijn(Emote e, boolean baby) {
        if (baby) {
            return "baby";
        }
        return e == Emote.SCHRIKKEN ? "eng" : e == Emote.ZWAAIEN ? "zwaai" : e == Emote.VAHOEG ? "groot" : "zie";
    }

    /** G1: the guh tells its owner (near, not too often) what it sees, in the chat: "Pluis: Njeg, een olifant!". */
    static void lijn(ServerLevel level, GuhEntity guh, SafariDier dier, String soort) {
        if (!(guh.getOwner() instanceof ServerPlayer p) || p.level() != level || p.distanceToSqr(guh) > LIJN_AFSTAND * LIJN_AFSTAND) {
            return;
        }
        long nu = level.getGameTime();
        Long t = LAATSTE_LIJN.get(p.getUUID());
        if (t != null && nu - t < LIJN_COOLDOWN) {
            return;
        }
        LAATSTE_LIJN.put(p.getUUID(), nu);
        int n = level.getRandom().nextInt(LIJNEN);
        p.sendSystemMessage(Component.translatable("gui.guhs.safari.guhziet." + soort + "." + n, guh.getDisplayName(),
                Component.translatable("gui.guhs.safari.foto.naam." + dier.soort())).withStyle(ChatFormatting.YELLOW));
    }

    private GuhReacties() {
    }
}
