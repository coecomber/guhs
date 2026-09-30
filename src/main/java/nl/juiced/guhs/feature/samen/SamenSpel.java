package nl.juiced.guhs.feature.samen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Your guh at the minigames (2.10, samen). The games call one line each at a good throw / catch / step
 * ({@link #goed}), a miss ({@link #mis}) or with the result of a try ({@link #uitslag}); every own guh within
 * {@link #BEREIK} blocks that isn't busy reacts:
 * <ul>
 *   <li>goed: a happy VAHOEG jump towards you, little hearts, a cheer;</li>
 *   <li>mis: the lovingly sad VERDRIETJE ("ooh njeg..."), a little tear, never angry;</li>
 *   <li>record (a new personal best, from the Scorebord through the RECORD moment, or {@link #record}): the knuffeldansje
 *       with hearts and notes.</li>
 * </ul>
 * Each guh reacts at most once per {@link #RUST} ticks (a quick game doesn't make it jump all the time). A guh that rides
 * along (the kart) can't jump: it cheers with hearts and its voice only.
 * <p>
 * The moments of the band bus give the rest: at the start of a game your guh waves "succes!"; at the end playing
 * together counts: MINIGAME hearts, the dagboek stat MINIGAMES_SAMEN and the first time "eerste_minigame"; a record gives
 * RECORD hearts; every 64 blocks travelled together (REIS) gives REIZEN hearts.
 */
public final class SamenSpel {
    /** Own guhs this close to you react to your game. */
    public static final double BEREIK = 24;
    /** Ticks between two reactions of the same guh. */
    public static final int RUST = 50;
    /** Persistent data: game time until which this guh doesn't react again. */
    static final String RUST_TOT = "guhs_samen_juich_tot";

    public enum Soort { GOED, MIS, RECORD, SUCCES }

    /** The minigames with their own texts (gui.guhs.samen.spel.&lt;id&gt;, gui.guhs.wistjedat.samen.spel.&lt;id&gt;; samen.py). */
    public static final java.util.Set<String> SPELLEN = java.util.Set.of(
            "beauty", "race", "meppen", "disco", "golf", "smul", "vissen", "verstop", "bakkerij", "creche", "theehuis", "kapper",
            "sterrenwacht", "ballon", "knuffelbad", "grijpmachine", "sjoelen", "doolhof", "katapult", "knabbelspelen", "elftocht",
            "circuit", "beroepen");

    private SamenSpel() {
    }

    // =====================================================================================================================
    // the one-line hooks of the games
    // =====================================================================================================================

    /** A good throw / catch / step / time in game {@code spel} (a {@link nl.juiced.guhs.feature.Minigames} id). */
    public static int goed(@Nullable ServerPlayer player, String spel) {
        return reageer(player, spel, Soort.GOED);
    }

    /** A miss: your guh is lovingly sad (never cross). */
    public static int mis(@Nullable ServerPlayer player, String spel) {
        return reageer(player, spel, Soort.MIS);
    }

    /** A record: your guh dances. */
    public static int record(@Nullable ServerPlayer player, String spel) {
        return reageer(player, spel, Soort.RECORD);
    }

    /** {@link #goed} or {@link #mis}, for a game that knows how it went. */
    public static int uitslag(@Nullable ServerPlayer player, String spel, boolean goed) {
        return reageer(player, spel, goed ? Soort.GOED : Soort.MIS);
    }

    /** The same hooks for a game that only knows its player's UUID. */
    public static int uitslag(@Nullable MinecraftServer server, @Nullable UUID player, String spel, boolean goed) {
        if (server == null || player == null) {
            return 0;
        }
        return uitslag(server.getPlayerList().getPlayer(player), spel, goed);
    }

    /** Every free own guh within {@link #BEREIK} reacts (the ones that rested long enough). Returns how many did. */
    static int reageer(@Nullable ServerPlayer player, String spel, Soort soort) {
        if (player == null || player.level().isClientSide) {
            return 0;
        }
        int n = 0;
        try {
            for (GuhEntity guh : juichers(player)) {
                if (juich(guh, player, soort)) {
                    n++;
                }
            }
            if (n > 0 && soort == Soort.GOED) {
                GidsFeature.grant(player, "lieve_vadsjes/samen_juichen");
            }
        } catch (RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().warn("samen: cheering at {} failed", spel, e);
        }
        return n;
    }

    /** Your guhs that can see your game: own band guhs close by, not asleep in a huisje, not in someone else's show or busy. */
    public static List<GuhEntity> juichers(ServerPlayer player) {
        List<GuhEntity> out = new ArrayList<>();
        for (GuhEntity guh : Band.samenGuhs(player, BEREIK)) {
            if (guh.isAlive() && !guh.isNoAi() && !guh.getTags().contains("guhs_beauty_model") && !GuhHooks.isBezig(guh)
                    && guh.emotes.current() != Emote.SLAPEN) {
                out.add(guh);
            }
        }
        out.sort(Comparator.comparingDouble(g -> g.distanceToSqr(player)));
        return out;
    }

    /** One guh reacts (if it rested long enough). */
    public static boolean juich(GuhEntity guh, ServerPlayer player, Soort soort) {
        long nu = guh.level().getGameTime();
        if (guh.getPersistentData().getLong(RUST_TOT) > nu || !(guh.level() instanceof ServerLevel level)) {
            return false;
        }
        guh.getPersistentData().putLong(RUST_TOT, nu + RUST);
        kijk(guh, player.position());
        Emote emote = switch (soort) {
            case GOED -> Emote.VAHOEG;
            case MIS -> Emote.VERDRIETJE;
            case RECORD -> Emote.KNUFFELDANSJE;
            case SUCCES -> Emote.ZWAAIEN;
        };
        if (guh.emotes.current() == null || guh.emotes.source() != GuhEmotes.Source.OWNER) {
            if (guh.emotes.start(emote, false, GuhEmotes.Source.SELF)) {
                guh.emotes.setLookTarget(player.getUUID());
            }
        }
        double top = guh.getY() + guh.getBbHeight() + 0.2;
        float pitch = guh.getVoicePitch();
        switch (soort) {
            case GOED, SUCCES -> {
                // favorietjes: a blij guh cheers louder (and sings its favourite song along at a good throw)
                float luid = soort == Soort.GOED ? nl.juiced.guhs.feature.favorietjes.FavorietLiedje.juich(guh)
                        : nl.juiced.guhs.feature.favorietjes.FavorietLiedje.volume(guh);
                level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), top, guh.getZ(), 3, guh.getBbWidth() * 0.4, 0.15,
                        guh.getBbWidth() * 0.4, 0.02);
                geluid(guh, ModSounds.GUH_HAPPY.get(), 0.8f * luid, pitch * 1.15f);
                geluid(guh, SamenFeature.JUICH.get(), (soort == Soort.SUCCES ? 0.5f : 0.8f) * luid, 1f);
            }
            case MIS -> {
                Vec3 oog = guh.position().add(Vec3.directionFromRotation(0, guh.yBodyRot).scale(guh.getBbWidth() * 0.45))
                        .add(0, guh.getBbHeight() * 0.65, 0);
                level.sendParticles(ParticleTypes.FALLING_WATER, oog.x, oog.y, oog.z, 2, guh.getBbWidth() * 0.2, 0.02, 0.05, 0);
                geluid(guh, SamenFeature.OOH.get(), 0.8f, 1f);
            }
            case RECORD -> {
                level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), top, guh.getZ(), 10, guh.getBbWidth() * 0.6, 0.3,
                        guh.getBbWidth() * 0.6, 0.05);
                level.sendParticles(ParticleTypes.NOTE, guh.getX(), top + 0.2, guh.getZ(), 5, 0.5, 0.2, 0.5, 1);
                // favorietjes sings the favourite song itself at a record; here only the louder cheer
                float luid = nl.juiced.guhs.feature.favorietjes.FavorietLiedje.volume(guh);
                geluid(guh, ModSounds.GUH_HAPPY.get(), luid, pitch * 1.25f);
                geluid(guh, SamenFeature.JUICH.get(), luid, 1.15f);
            }
        }
        return true;
    }

    private static void geluid(GuhEntity guh, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (guh.isTame() && !guh.areSoundsEnabled()) {
            return;   // "sounds off" in the Guh menu
        }
        guh.level().playSound(null, guh.getX(), guh.getY(), guh.getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
    }

    /** Turns the guh (body and head) towards a spot at once, and keeps looking. */
    static void kijk(Mob guh, Vec3 naar) {
        double dx = naar.x - guh.getX(), dz = naar.z - guh.getZ();
        if (dx * dx + dz * dz > 1.0E-4) {
            float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90f;
            guh.setYRot(yaw);
            guh.yBodyRot = yaw;
            guh.yHeadRot = yaw;
        }
        guh.getLookControl().setLookAt(naar.x, naar.y + 1.4, naar.z);
    }

    // =====================================================================================================================
    // the band bus: minigames together, records, travelling together
    // =====================================================================================================================

    static void moment(Mob mob, @Nullable ServerPlayer speler, Moment m, String waarde) {
        if (!(mob instanceof GuhEntity guh) || speler == null || !speler.getUUID().equals(guh.getOwnerUUID())) {
            return;
        }
        switch (m) {
            case MINIGAME_START -> {
                if (!Huisjes.isBinnen(guh)) {
                    juich(guh, speler, Soort.SUCCES);   // "succes!"
                }
            }
            case MINIGAME_EINDE -> samenGespeeld(guh, speler, waarde);
            case RECORD -> {
                Band.geefHartjes(guh, speler, Reden.RECORD.standaard(), Reden.RECORD);
                guh.getPersistentData().remove(RUST_TOT);
                juich(guh, speler, Soort.RECORD);
                Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.record", speler.getGameProfile().getName());
                GidsFeature.grant(speler, "lieve_vadsjes/samen_record");
            }
            case REIS -> {
                Band.geefHartjes(guh, speler, Reden.REIZEN.standaard(), Reden.REIZEN);
                if (Dagboek.eersteKeer(guh, speler, "samen_op_reis")) {
                    Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.op_reis", speler.getGameProfile().getName());
                }
            }
            default -> {
            }
        }
    }

    /** A game together is over: hearts, the stat, the first time. */
    static void samenGespeeld(GuhEntity guh, ServerPlayer speler, String spel) {
        Band.geefHartjes(guh, speler, Reden.MINIGAME.standaard(), Reden.MINIGAME);
        Dagboek.tel(guh, DagboekStat.MINIGAMES_SAMEN, 1);
        boolean bekend = SPELLEN.contains(spel);
        Component naam = bekend ? Component.translatable("gui.guhs.samen.spel." + spel) : Component.literal(spel);
        if (Dagboek.eersteKeer(guh, speler, "eerste_minigame")) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.eerste_minigame", speler.getGameProfile().getName());
        } else if (bekend && guh.getRandom().nextInt(3) == 0) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.samen.spel." + spel);   // (each game has its own little line)
        }
        GuhAdvancements.grant(speler, "samen_gespeeld");
        GidsFeature.grant(speler, "lieve_vadsjes/samen_gespeeld");
        speler.displayClientMessage(Component.translatable("gui.guhs.samen.samen_gespeeld", guh.getDisplayName(), naam)
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
}
