package nl.juiced.guhs.feature.speelgoed;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * What playing gives (2.10): after a guh played with a toy ({@link #gespeeld}) it gets SPEELGOED hearts, the SPEELTJE
 * moment goes out (favourites!), its dagboek counts SPEELTJES and writes "eerste speeltje" / a wist-je-datje, and it does
 * a happy VAHOEG. Its owner gets the advancements (first toy, all four kinds). Players pushing a swing or kicking the
 * ball: {@link #geduwd}, {@link #geschopt} (a kick invites your guhs to chase the ball).
 */
public final class Spelen {
    /** The four toys (Speeltje ids, also the favourite SPEELTJE candidates). */
    public static final List<String> SPEELTJES = List.of("knabbelbal", "glijbaantje", "tunnel", "wip_schommel");
    /** Player data: which kinds your guhs played with. */
    static final String KEY = "guhs_speelgoed_soorten";
    /** Guh persistent data: VAHOEG as soon as it can (after getting off a toy). */
    static final String JUICH = "guhs_speelgoed_juich";
    /** Guh persistent data: invited to chase this ball (entity id) until BAL_TOT. */
    static final String BAL = "guhs_speelgoed_bal", BAL_TOT = "guhs_speelgoed_bal_tot";

    private Spelen() {
    }

    /** A guh (or maatje) played with a toy: hearts, moment, dagboek, advancements, a happy cheer. */
    public static void gespeeld(Mob wie, String speeltje, @Nullable ServerPlayer duwer) {
        if (!(wie.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), wie.getX(), wie.getY() + wie.getBbHeight() + 0.3, wie.getZ(),
                3, 0.3, 0.15, 0.3, 0);
        if (wie instanceof GuhEntity g) {
            g.getPersistentData().putLong(JUICH, level.getGameTime() + 2);
        }
        if (!Band.isBandGuh(wie)) {
            return;
        }
        ServerPlayer baas = Band.eigenaarOnline(wie);
        Band.geefHartjes(wie, baas, Reden.SPEELGOED.standaard(), Reden.SPEELGOED);
        Band.moment(wie, baas, Moment.SPEELTJE, speeltje);
        Dagboek.tel(wie, DagboekStat.SPEELTJES, 1);
        if (Dagboek.eersteKeer(wie, baas, "eerste_speeltje")) {
            Dagboek.wistJeDat(wie, "gui.guhs.wistjedat.speelgoed.eerste_" + speeltje);
        } else if (level.random.nextInt(4) == 0) {
            Dagboek.wistJeDat(wie, "gui.guhs.wistjedat.speelgoed." + speeltje + "_" + (1 + level.random.nextInt(2)));
        }
        if (baas != null) {
            GidsFeature.grant(baas, "lieve_vadsjes/speelgoed_eerste");
            if (soort(baas, speeltje)) {
                GidsFeature.grant(baas, "lieve_vadsjes/speelgoed_alle");
            }
        }
    }

    /** Remembers that the player's guhs played with this kind; true when all four kinds are done. */
    static boolean soort(ServerPlayer player, String speeltje) {
        CompoundTag saved = GuhQuests.saved(player);
        ListTag lijst = saved.getList(KEY, Tag.TAG_STRING);
        boolean nieuw = lijst.stream().noneMatch(t -> t.getAsString().equals(speeltje));
        if (nieuw) {
            lijst.add(StringTag.valueOf(speeltje));
            saved.put(KEY, lijst);
        }
        return SPEELTJES.stream().allMatch(s -> lijst.stream().anyMatch(t -> t.getAsString().equals(s)));
    }

    /** Which kinds this player's guhs played with (tests, the Guhdex). */
    public static List<String> soorten(Player player) {
        return GuhQuests.saved(player).getList(KEY, Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
    }

    /** A player plays on a toy themselves (glijbaantje, schommel, wip). */
    public static void spelerSpeelt(ServerPlayer player, String speeltje) {
        GidsFeature.grant(player, "lieve_vadsjes/speelgoed_zelf");
    }

    /** A player pushed the swing/wip with this guh on it: a heart from its own owner, and the advancement. */
    public static void geduwd(ServerPlayer player, GuhEntity guh) {
        GidsFeature.grant(player, "lieve_vadsjes/speelgoed_duw");
        if (player.getUUID().equals(guh.getOwnerUUID())) {
            Band.geefHartjes(guh, player, 1, Reden.SPEELGOED);
        }
    }

    /** A player kicked the knabbelbal: the advancement, and their own guhs nearby want to chase it. */
    public static void geschopt(ServerPlayer player, KnabbelbalEntity bal) {
        GidsFeature.grant(player, "lieve_vadsjes/speelgoed_schop");
        long tot = player.level().getGameTime() + 60;
        for (GuhEntity g : player.serverLevel().getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(12),
                g -> g.getType() == ModEntities.GUH.get() && g.isTame() && player.getUUID().equals(g.getOwnerUUID()) && !g.isOrderedToSit())) {
            g.getPersistentData().putInt(BAL, bal.getId());
            g.getPersistentData().putLong(BAL_TOT, tot);
        }
    }

    /** (GuhHooks.tick) The happy VAHOEG after playing, as soon as the guh stands on its feet again. */
    static void tick(GuhEntity guh) {
        long juich = guh.getPersistentData().getLong(JUICH);
        if (juich == 0 || guh.level().getGameTime() < juich) {
            return;
        }
        if (guh.isPassenger()) {
            return;
        }
        guh.getPersistentData().remove(JUICH);
        if (GuhEmotes.canStart(guh)) {
            guh.emotes.start(guh.getRandom().nextInt(3) == 0 ? Emote.HARTJES : Emote.VAHOEG, false, GuhEmotes.Source.SELF);
        }
        if (guh.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, guh.getX(), guh.getY() + guh.getBbHeight(), guh.getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        TunnelSpel.wis();
    }
}
