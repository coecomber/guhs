package nl.juiced.guhs.feature.favorietjes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.FavorietSoort;
import nl.juiced.guhs.feature.band.Favorieten;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.wereldleven.Koortje;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The favourite song in minigames (2.10, favorietjes): a blij guh cheers LOUDER, and it cheers with its favourite song.
 * <ul>
 *   <li>A minigame starts ({@link Moment#MINIGAME_START}) and you know your guh's favourite song: it hums it (it is
 *       ready!) and is blij for the whole game (x1.5 hearts).</li>
 *   <li>Every score you hand in with a blij guh around ({@code Scorebord.opInzending}), and every new record
 *       ({@link Moment#RECORD}), your guh sings a piece of its favourite song, loud, with music notes and sparkles.</li>
 * </ul>
 * <b>Hook for the cheering of other features</b> (samen: {@code SamenSpel.goed/record}): call {@link #juich(Mob)} when a guh
 * cheers; it returns the volume factor for the cheer sound (1.6 when blij, else 1.0) and, when blij, sings the favourite song
 * along. {@link #volume(Mob)} alone only gives the factor.
 */
public final class FavorietLiedje {
    /** How much louder a blij guh cheers. */
    public static final float BLIJ_VOLUME = 1.6f;
    /** A minigame with its (known) favourite song on its mind: blij this long (or until the game ends). */
    public static final int MINIGAME_BLIJ = 6000;

    /**
     * The disco songs are real tracks (no bars to read), so here is the little hook of each that a guh sings: notes on
     * the xylofoon scale (0-7, {@link Koortje#pitch}).
     */
    static final Map<String, int[]> DISCO_MOTIEF = Map.of(
            "vadsige_tango", new int[]{4, 4, 3, 2, 3, 4, 0, 0},
            "ze_hangen_disco70", new int[]{0, 2, 4, 5, 4, 2, 0, 4},
            "mika_mambo", new int[]{4, 5, 4, 2, 4, 5, 7, 4},
            "njeg_njeg_boogie", new int[]{0, 0, 2, 3, 4, 3, 2, 0});

    private record Zang(ServerLevel level, int guh, int[] noten, float volume, int tempo, int[] stap) {
    }

    private static final List<Zang> ZANGEN = new ArrayList<>();

    private FavorietLiedje() {
    }

    // =====================================================================================================================
    // the hook
    // =====================================================================================================================

    /** The volume factor of a guh's cheer: louder when it is blij. */
    public static float volume(Mob guh) {
        return Band.isBlij(guh) ? BLIJ_VOLUME : 1f;
    }

    /** A guh cheers (hook for samen): when blij it sings its favourite song along. Returns the cheer's volume factor. */
    public static float juich(Mob guh) {
        float v = volume(guh);
        if (v > 1f) {
            zing(guh, v, 6);
        }
        return v;
    }

    /** The notes of a favourite song ("koortje:&lt;id&gt;" or "disco:&lt;id&gt;"), or null. */
    @Nullable
    public static int[] noten(@Nullable String liedje) {
        if (liedje == null) {
            return null;
        }
        if (liedje.startsWith("koortje:")) {
            Koortje.Liedje l = Koortje.Liedje.byId(liedje.substring(8));
            return l == null ? null : l.noten;
        }
        if (liedje.startsWith("disco:")) {
            return DISCO_MOTIEF.get(liedje.substring(6));
        }
        return null;
    }

    /** A guh sings (at most n notes of) its favourite song. False when it has none (not a band guh). */
    public static boolean zing(Mob guh, float volume, int n) {
        int[] alle = noten(Favorieten.waarde(guh, FavorietSoort.LIEDJE));
        if (alle == null || !(guh.level() instanceof ServerLevel level)) {
            return false;
        }
        int[] noten = java.util.Arrays.copyOf(alle, Math.min(n, alle.length));
        synchronized (ZANGEN) {
            ZANGEN.removeIf(z -> z.guh() == guh.getId() && z.level() == level);   // one song at a time
            ZANGEN.add(new Zang(level, guh.getId(), noten, volume, 4, new int[]{0, 0}));
        }
        if (guh instanceof GuhEntity g) {
            g.triggerAnim("action", "happy");
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        synchronized (ZANGEN) {
            if (ZANGEN.isEmpty()) {
                return;
            }
            Iterator<Zang> it = ZANGEN.iterator();
            while (it.hasNext()) {
                Zang z = it.next();
                int[] stap = z.stap();   // [0] = ticks waited, [1] = next note
                Entity e = z.level().getEntity(z.guh());
                if (e == null || !e.isAlive() || stap[1] >= z.noten().length) {
                    it.remove();
                    continue;
                }
                if (stap[0]++ % z.tempo() != 0) {
                    continue;
                }
                int noot = z.noten()[stap[1]++];
                z.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.NOTE_BLOCK_FLUTE, SoundSource.NEUTRAL,
                        0.7f * z.volume(), Koortje.pitch(noot));
                z.level().sendParticles(WereldlevenFeature.ZANGNOOTJE.get(), e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(),
                        z.volume() > 1f ? 2 : 1, e.getBbWidth() * 0.3, 0.1, e.getBbWidth() * 0.3, 0);
                if (z.volume() > 1f && stap[1] % 2 == 0) {
                    z.level().sendParticles(FavorietjesFeature.GLINSTER.get(), e.getX(), e.getY() + e.getBbHeight() * 0.8, e.getZ(), 2,
                            e.getBbWidth() * 0.5, 0.2, e.getBbWidth() * 0.5, 0.01);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (ZANGEN) {
            ZANGEN.clear();
        }
    }

    /** (tests) is this guh singing right now? */
    public static boolean zingt(Mob guh) {
        synchronized (ZANGEN) {
            return ZANGEN.stream().anyMatch(z -> z.guh() == guh.getId() && z.level() == guh.level());
        }
    }

    // =====================================================================================================================
    // minigames
    // =====================================================================================================================

    static void moment(Mob guh, @Nullable ServerPlayer speler, Moment m, String waarde) {
        switch (m) {
            case MINIGAME_START -> {
                if (Favorieten.ontdekt(guh, FavorietSoort.LIEDJE)) {
                    Band.maakBlij(guh, MINIGAME_BLIJ);
                    zing(guh, 0.9f, 5);
                    if (speler != null) {
                        speler.sendOverlayMessage(Component.translatable("gui.guhs.favorietjes.neuriet", guh.getDisplayName(),
                                Favorieten.naam(FavorietSoort.LIEDJE, Favorieten.waarde(guh, FavorietSoort.LIEDJE)))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }
            case RECORD -> {
                if (Band.isBlij(guh) || Favorieten.ontdekt(guh, FavorietSoort.LIEDJE)) {
                    Band.maakBlij(guh, Favorietjes.BLIJ_WEER);
                    if (zing(guh, BLIJ_VOLUME + 0.4f, 8) && speler != null) {
                        GuhAdvancements.grant(speler, "favorietjes_juichen");
                    }
                }
            }
            default -> {
            }
        }
    }

    /** A score handed in (not a record: that is the RECORD moment): every blij guh of the player around cheers with its song. */
    static void ingezonden(ServerPlayer player, String board, int score, boolean lowerIsBetter, boolean nieuwRecord) {
        if (nieuwRecord) {
            return;
        }
        for (GuhEntity guh : Band.samenGuhs(player, 32)) {
            if (Band.isBlij(guh) && juich(guh) > 1f) {
                GuhAdvancements.grant(player, "favorietjes_juichen");
            }
        }
    }
}
