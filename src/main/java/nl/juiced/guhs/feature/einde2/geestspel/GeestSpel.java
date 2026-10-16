package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestWezen;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice geest_spel: the hub of the play in the Geestenwereld. It hands every player tick, every click on a
 * prop marker and every click on a creature to the parts ({@link WitteLotus}, {@link Proeven}, {@link Lantaarns},
 * {@link HeiBai}, {@link KohRaadsels}, {@link Chakras}, {@link Snelreis}, {@link Verschijningen}, {@link Mysterie}), and has
 * the small helpers they share (a line in the chat, a reward, a sound for one player).
 */
public final class GeestSpel {
    /** The per-player data of this slice besides its lines (CONTRACT_E2_B 2.1: {@code guhs_einde2_<slice>_*}). */
    public static final String DATA = "guhs_einde2_geest_spel_";

    private GeestSpel() {
    }

    // =====================================================================================================================
    // events
    // =====================================================================================================================

    /** A player arrived in the Geestenwereld (through {@link GeestToegang#stuur} or any other door). */
    static void binnen(ServerPlayer p) {
        GuhAdvancements.grant(p, "einde2_geest_spel_binnen");
        Verschijningen.sfeer(p);
    }

    static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (event.getTo() == Geestenwereld.DIM) {
            binnen(p);
        }
        if (event.getFrom() == Geestenwereld.DIM) {
            weg(p);
        }
    }

    static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            weg(p);
            Snelreis.vergeet(p);
        }
    }

    /** The player left the Geestenwereld (or the game): their actors and every running trial go. */
    static void weg(ServerPlayer p) {
        GeestActeurs.weg(p.getUUID());
        Proeven.stop(p);
        Lantaarns.vergeet(p);
        KohRaadsels.stop(p);
        Chakras.stop(p);
        Mysterie.vergeet(p);
        Verschijningen.vergeet(p);
        GeestLagen.opWeg(p);
        Verschijningen.sfeer(p);
    }

    static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if ((p.tickCount + p.getId()) % 20 == 0) {
            Chakras.gloedTick(p);
        }
        if (p.level().dimension() != Geestenwereld.DIM) {
            if ((p.tickCount + p.getId()) % 200 == 0) {
                Snelreis.buitenTick(p);
            }
            return;
        }
        if (!p.isAlive() || p.isSpectator()) {
            return;
        }
        Proeven.elkeTick(p);
        Lantaarns.elkeTick(p);
        if ((p.tickCount + p.getId()) % 10 != 0) {
            return;
        }
        if (Cutscenes.bezig(p)) {
            return;
        }
        WitteLotus.tick(p);
        Proeven.tick(p);
        Lantaarns.tick(p);
        HeiBai.tick(p);
        KohRaadsels.tick(p);
        Chakras.tick(p);
        Snelreis.tick(p);
        Verschijningen.tick(p);
        Mysterie.tick(p);
    }

    /** A right-click on a block in the Geestenwereld: a prop marker of this slice? */
    static void opBlokKlik(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer p) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || p.level().dimension() != Geestenwereld.DIM || p.isSpectator()) {
            return;
        }
        BlockPos pos = event.getPos();
        String prop = GeestPlek.propOp(pos);
        if (prop == null) {
            prop = GeestPlek.propOp(pos.above());
        }
        if (prop == null || !klik(p, prop, pos)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** A prop was clicked: true when a part of this slice took it. */
    static boolean klik(ServerPlayer p, String prop, BlockPos pos) {
        if (Cutscenes.bezig(p)) {
            return false;
        }
        return WitteLotus.klik(p, prop) || Proeven.klik(p, prop) || HeiBai.klik(p, prop) || Chakras.klik(p, prop) || Mysterie.klik(p, prop);
    }

    /** {@link GeestWezen#BIJ_KLIK}: a click on a spirit creature (one of our own actors, or a resident of the build). */
    static boolean opWezenKlik(GeestWezen w, ServerPlayer p) {
        if (p.level().dimension() != Geestenwereld.DIM || Cutscenes.bezig(p)) {
            return false;
        }
        return Lantaarns.klik(p, w) || HeiBai.klikWezen(p, w) || KohRaadsels.klikWezen(p, w) || Mysterie.klikWezen(p, w);
    }

    /** A click on any other entity (creatures that are not GeestWezens yet: the fox spirits, the heron, Wan-guh). */
    static void opEntityKlik(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer p) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || event.getTarget() instanceof GeestWezen || p.level().dimension() != Geestenwereld.DIM || Cutscenes.bezig(p)) {
            return;
        }
        if (Mysterie.klikAnder(p, event.getTarget()) || Verschijningen.klik(p, event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** A narrator line in the chat (light purple, only for this player). */
    public static void verteller(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
    }

    /** A line of a speaker in the chat ("&lt;Name&gt; text"); the speaker may be missing (then its name key). */
    public static void zeg(ServerPlayer p, @Nullable Entity spreker, String naamKey, String key, Object... args) {
        if (spreker != null) {
            GuhQuests.say(p, spreker, key, args);
            return;
        }
        p.sendSystemMessage(Component.literal("<").append(Component.translatable(naamKey)).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key, args).withStyle(ChatFormatting.WHITE)));
    }

    /** A short line over the hotbar. */
    public static void balk(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** One item into the player's bag (or at their feet when it is full). */
    public static void geef(ServerPlayer p, Item item, int n) {
        ItemStack stack = new ItemStack(item, n);
        if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
    }

    /** A sound only this player hears, at their spot. */
    public static void geluid(ServerPlayer p, SoundEvent s, float volume, float pitch) {
        nl.juiced.guhs.feature.bestaand.Schijn.geluid(p, s, p.blockPosition(), volume, pitch);
    }

    /** One of the audio slice's sounds (CONTRACT_E2_B 6.1), only for this player. */
    public static void geluid(ServerPlayer p, String naam, float volume, float pitch) {
        geluid(p, E2Audio.geluid(naam).get(), volume, pitch);
    }

    /** A sound at a spot for everyone near it. */
    public static void geluid(ServerPlayer p, Vec3 waar, SoundEvent s, float volume, float pitch) {
        p.level().playSound(null, waar.x, waar.y, waar.z, s, SoundSource.NEUTRAL, volume, pitch);
    }

    /** Opens the talking screen: pages of (speaker, name key, text key); options last. */
    public static void praat(ServerPlayer p, String sleutel, List<Praat.Regel> regels, Praat.Optie... opties) {
        Praat.scene(p, sleutel, regels, opties);
    }

    /** A page of a speaker (the resident at this marker when it is there, else a page with just the name). */
    public static Praat.Regel regel(@Nullable Entity spreker, String naamKey, String tekstKey, Object... args) {
        return new Praat.Regel(spreker, spreker != null ? "" : naamKey, tekstKey, args);
    }

    /** A narrator page. */
    public static Praat.Regel verteller(String tekstKey, Object... args) {
        return new Praat.Regel(null, "", tekstKey, args);
    }

    /** Is this player looking at that point (within the angle, in degrees) and near enough? */
    public static boolean kijktNaar(ServerPlayer p, Vec3 punt, double graden, double max) {
        Vec3 oog = p.getEyePosition();
        Vec3 d = punt.subtract(oog);
        double afstand = d.length();
        if (afstand > max || afstand < 0.01) {
            return false;
        }
        double cos = d.normalize().dot(p.getViewVector(1f));
        return cos >= Math.cos(Math.toRadians(graden));
    }
}
