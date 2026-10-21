package nl.juiced.guhs.feature.safari.gedrag;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.safari.SafariDier;

/**
 * Round 3 (ronde 45): your own guhs react to the park animals. A tamed guh near an animal now and then does the reaction of
 * that species: it is startled by the crocodile, waves at the penguins, stands up like a meerkat ({@link Emote#RECHTOP}),
 * or climbs onto a capybara for a little ride ({@link SafariDier#neemMee}). Pure charm, with a cooldown per guh.
 */
public final class GuhReacties {
    /** How close a guh must be, and how long (ticks) a guh waits before its next reaction. */
    static final double AFSTAND = 6;
    static final int COOLDOWN = 20 * 45;
    private static final Map<GuhEntity, Long> LAATST = new WeakHashMap<>();

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
                return;
            }
            Emote e = reactie(dier.soort(), dier.isBaby());
            if (e == null) {
                continue;
            }
            guh.getLookControl().setLookAt(dier, 30f, 30f);
            if (guh.emotes.start(e, false, GuhEmotes.Source.SELF)) {
                LAATST.put(guh, nu);
                return;
            }
        }
    }

    private GuhReacties() {
    }
}
