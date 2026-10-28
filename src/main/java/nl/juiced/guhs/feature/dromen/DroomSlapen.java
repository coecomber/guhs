package nl.juiced.guhs.feature.dromen;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.Dagdeel;

/**
 * Tucking a guh in and falling asleep together (ontwerp ronde 3):
 * <ol>
 *   <li>In the evening or at night, right-click a sleeping place ({@link DromenFeature#SLAAPPLEKKEN}: the doos met knabbels,
 *       a kattenhuisje, the guhbedje, a guhnestje) with one of your own tamed guhs within {@link #ZOEK} blocks: the closest
 *       one climbs in, curls up (the SLAPEN emote) and stays. One of what you hold goes along with it: it picks the dream's
 *       island ({@link DroomStart}) and comes back when you wake up. Sneak + right-click the place again: tucked out.</li>
 *   <li>Lie down in a bed within {@link #BED_BEREIK} blocks of your tucked-in guh: after a moment of sleep you wake up in
 *       its dream ({@link DroomReis#naarDroom}).</li>
 *   <li>In the morning a tucked-in guh whose owner is not dreaming wakes by itself, with a yawn.</li>
 * </ol>
 * A tucked-in guh carries {@link #TAG_PLEK} (its place) and {@link #TAG_BAAS} (its owner) in its persistent data; the owner
 * carries the guh's UUID ({@link DroomReis#data} "Guh"). Part B (slaapzorg) adds tiredness and guhs that find a place by
 * themselves; it only has to use the same tag.
 */
public final class DroomSlapen {
    static final String TAG_PLEK = "guhs_droom_ingestopt", TAG_BAAS = "guhs_droom_baas";
    /** How far your guh may be from the place you click, and from your bed. */
    public static final int ZOEK = 10, BED_BEREIK = 6;
    /** How long you sleep before the dream starts (vanilla skips the night at 100). */
    public static final int INSLAAP_TICKS = 40;
    /** Test command and game tests: tucking in is allowed at any hour. */
    public static volatile boolean altijd;

    private DroomSlapen() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(DroomSlapen::onKlik);
        NeoForge.EVENT_BUS.addListener(DroomSlapen::onSpelerTick);
        NeoForge.EVENT_BUS.addListener(DroomSlapen::tick);
        nl.juiced.guhs.feature.verhaal.Praat.luister(SLEUTEL, DroomSlapen::antwoord);
    }

    // =====================================================================================================================
    // tucking in
    // =====================================================================================================================

    private static void onKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(DromenFeature.SLAAPPLEKKEN)) {
            return;
        }
        if (p.isShiftKeyDown()) {
            GuhEntity er = ingestoptOp((ServerLevel) event.getLevel(), event.getPos());
            if (er != null && er.isOwnedBy(p)) {
                wek(er, p, false);
                p.sendOverlayMessage(Component.translatable("gui.guhs.dromen.instoppen.uit", er.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        if (instoppen(p, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Is it bedtime here? (evening or night; always with {@link #altijd}) */
    public static boolean bedtijd(ServerLevel level) {
        if (altijd) {
            return true;
        }
        Dagdeel d = Dagdeel.huidig(level);
        return d == Dagdeel.AVOND || d == Dagdeel.NACHT;
    }

    /** Tucks the player's closest own guh in on this place. False (with a message) when it can't. */
    public static boolean instoppen(ServerPlayer p, BlockPos plek) {
        ServerLevel level = (ServerLevel) p.level();
        if (!bedtijd(level)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.dromen.instoppen.geen_bedtijd").withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;   // (still "handled": a box is no chest)
        }
        if (ingestoptOp(level, plek) != null) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.dromen.instoppen.bezet").withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
        }
        GuhEntity guh = kies(p, plek);
        if (guh == null) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.dromen.instoppen.geen_guh").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        GuhEntity oud = ingestopt(p);
        if (oud != null && oud != guh) {
            wek(oud, p, false);
        }
        ItemStack hand = p.getMainHandItem();
        ItemStack mee = hand.isEmpty() ? ItemStack.EMPTY : hand.copyWithCount(1);
        DroomReis.geefMeeTerug(p);              // (a previous gift that never came back)
        if (!mee.isEmpty() && !p.getAbilities().instabuild) {
            hand.shrink(1);
        }
        DroomReis.bewaarMee(p, mee);
        leg(guh, plek, p);
        String eiland = DroomStart.eilandVoor(mee);
        Component waar = Component.translatable("gui.guhs.dromen.eiland." + eiland);
        p.sendSystemMessage((mee.isEmpty() ? Component.translatable("gui.guhs.dromen.instoppen.zonder", guh.getDisplayName(), waar)
                : Component.translatable("gui.guhs.dromen.instoppen.met", guh.getDisplayName(), mee.getHoverName(), waar)).withStyle(ChatFormatting.LIGHT_PURPLE));
        level.playSound(null, plek, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1f, 1.2f);
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + 0.6, guh.getZ(), 4, 0.3, 0.2, 0.3, 0);
        return true;
    }

    /** The player's own tamed guh closest to the place (not sitting in a game, not ridden, not already tucked in elsewhere). */
    @Nullable
    static GuhEntity kies(ServerPlayer p, BlockPos plek) {
        List<GuhEntity> guhs = p.level().getEntitiesOfClass(GuhEntity.class, new AABB(plek).inflate(ZOEK),
                g -> g.isAlive() && g.isTame() && g.isOwnedBy(p) && !g.isPassenger() && !g.isVehicle());
        GuhEntity beste = null;
        double d = Double.MAX_VALUE;
        for (GuhEntity g : guhs) {
            double dd = g.distanceToSqr(Vec3.atCenterOf(plek));
            if (dd < d) {
                d = dd;
                beste = g;
            }
        }
        return beste;
    }

    /** Lays a guh in its place: on it, sitting (so nothing else walks it away), curled up asleep. */
    static void leg(GuhEntity guh, BlockPos plek, ServerPlayer baas) {
        double h = guh.level().getBlockState(plek).getBlock() instanceof SlaapplekBlock s ? s.lighoogte() : 0.6;
        guh.getNavigation().stop();
        guh.snapTo(plek.getX() + 0.5, plek.getY() + h, plek.getZ() + 0.5, guh.getYRot(), 0f);
        guh.setOrderedToSit(true);
        guh.setInSittingPose(true);
        guh.getPersistentData().putLong(TAG_PLEK, plek.asLong());
        guh.getPersistentData().putString(TAG_BAAS, baas.getUUID().toString());
        DroomReis.data(baas).putString("Guh", guh.getUUID().toString());
        slaapIn(guh);
    }

    private static void slaapIn(GuhEntity guh) {
        if (guh.emotes.current() != Emote.SLAPEN) {
            guh.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF);
        }
    }

    public static boolean isIngestopt(GuhEntity guh) {
        return guh.getPersistentData().contains(TAG_PLEK);
    }

    /** The guh tucked in on this place (null: none, or not loaded). */
    @Nullable
    static GuhEntity ingestoptOp(ServerLevel level, BlockPos plek) {
        long l = plek.asLong();
        List<GuhEntity> er = level.getEntitiesOfClass(GuhEntity.class, new AABB(plek).inflate(1.5),
                g -> g.getPersistentData().getLongOr(TAG_PLEK, Long.MIN_VALUE) == l);
        return er.isEmpty() ? null : er.get(0);
    }

    /** This player's tucked-in guh, when it is loaded in the player's own level or the saved home level. */
    @Nullable
    public static GuhEntity ingestopt(ServerPlayer p) {
        String id = DroomReis.data(p).getStringOr("Guh", "");
        if (id.isEmpty()) {
            return null;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e instanceof GuhEntity g && isIngestopt(g)) {
                return g;
            }
        }
        return null;
    }

    /** Wakes a tucked-in guh (and forgets it was tucked in); with a yawn when asked. */
    static void wek(GuhEntity guh, @Nullable ServerPlayer baas, boolean gapen) {
        guh.getPersistentData().remove(TAG_PLEK);
        guh.getPersistentData().remove(TAG_BAAS);
        if (guh.emotes.current() == Emote.SLAPEN) {
            guh.emotes.stop();
        }
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        if (gapen && GuhEmotes.canStart(guh)) {
            guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
        }
        if (baas != null) {
            DroomReis.data(baas).remove("Guh");
        }
    }

    /** Waking up from a dream: the player's guh wakes too (and comes to stand next to them when it is far away). */
    static void wekGuh(ServerPlayer p, boolean gapen) {
        GuhEntity guh = ingestopt(p);
        if (guh != null) {
            if (guh.level() == p.level() && guh.distanceToSqr(p) > 12 * 12) {
                guh.snapTo(p.getX() + 0.8, p.getY(), p.getZ() + 0.8, guh.getYRot(), 0f);
            }
            wek(guh, p, gapen);
        } else {
            DroomReis.data(p).remove("Guh");
        }
    }

    // =====================================================================================================================
    // falling asleep beside it
    // =====================================================================================================================

    /** The talking screen's sleutel of the bed question ("gewoon slapen" or "dromen"). */
    static final String SLEUTEL = "dromen_bed";
    /** After how many sleeping ticks the bed question comes (before the night is over: the night skips at 100). */
    public static final int VRAAG_TICKS = 12;
    /** Blocks a player sleeps in that count as a bed for dreaming (and for Snoes): beds, the wolkenbed, the guh-slaapzak. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> DROOMBEDDEN =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, nl.juiced.guhs.Guhs.id("droombedden"));
    /** The story's dream buddy at this bed (Snoes, set by snoesverhaal; null: none). */
    public static java.util.function.BiFunction<ServerPlayer, BlockPos, Entity> SNOES_BIJ_BED = (p, bed) -> null;
    /** The story's way into the dream with Snoes (set by snoesverhaal; false: it did not go). */
    public static java.util.function.Predicate<ServerPlayer> DROOM_MET_SNOES = p -> false;

    /** This player's dream buddy at their bed: their tucked-in guh close by, else the story's Snoes (null: dream alone? no: sleep). */
    @Nullable
    static Entity maatjeBijBed(ServerPlayer p, BlockPos bed) {
        GuhEntity guh = ingestopt(p);
        if (guh != null && guh.level() == p.level() && guh.distanceToSqr(Vec3.atCenterOf(bed)) <= BED_BEREIK * BED_BEREIK) {
            return guh;
        }
        return SNOES_BIJ_BED.apply(p, bed);
    }

    /** Lying in bed with a dream buddy close by: a question, gewoon slapen or dromen (playtest: never straight into the dream). */
    private static void onSpelerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isSleeping() || p.getSleepTimer() != (nl.juiced.guhs.feature.slaapkleding.Droompyjama.draagtSet(p) ? nl.juiced.guhs.feature.slaapkleding.Droompyjama.VRAAG_TICKS : VRAAG_TICKS) || DroomReis.inDroom(p)) {
            return;
        }
        BlockPos bed = p.getSleepingPos().orElse(p.blockPosition());
        Entity maatje = maatjeBijBed(p, bed);
        if (maatje == null) {
            return;
        }
        nl.juiced.guhs.feature.verhaal.Praat.open(p, maatje, SLEUTEL, "gui.guhs.dromen.bed.vraag", new Object[]{maatje.getDisplayName()},
                new nl.juiced.guhs.feature.verhaal.Praat.Optie(1, "gui.guhs.dromen.bed.dromen"),
                new nl.juiced.guhs.feature.verhaal.Praat.Optie(0, "gui.guhs.dromen.bed.slapen"));
    }

    /** The bed question was answered: 1 = off to the dream (with the guh, else with Snoes); 0 = sleep as usual. */
    static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (optie != 1 || DroomReis.inDroom(p)) {
            return;
        }
        BlockPos bed = p.getSleepingPos().orElse(p.blockPosition());
        GuhEntity guh = ingestopt(p);
        if (guh != null && guh.level() == p.level() && guh.distanceToSqr(Vec3.atCenterOf(bed)) <= BED_BEREIK * BED_BEREIK) {
            DroomReis.naarDroom(p, DroomStart.eilandVoor(DroomReis.mee(p)));
        } else if (!DROOM_MET_SNOES.test(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.dromen.bed.weg").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Once a second: tucked-in guhs keep sleeping on their place; in the morning (owner not dreaming) they wake up. */
    private static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 3) {
            return;
        }
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            GuhEntity guh = ingestopt(p);
            if (guh != null && guh.level() instanceof ServerLevel level) {
                BlockPos plek = BlockPos.of(guh.getPersistentData().getLongOr(TAG_PLEK, 0L));
                if (!level.getBlockState(plek).is(DromenFeature.SLAAPPLEKKEN)) {
                    wek(guh, p, false);                         // (its place is gone)
                } else if (!DroomReis.inDroom(p) && !altijd && Dagdeel.huidig(level) == Dagdeel.OCHTEND) {
                    wek(guh, p, true);
                    DroomReis.geefMeeTerug(p);
                } else {
                    slaapIn(guh);
                    if (!guh.isInSittingPose()) {
                        guh.setOrderedToSit(true);
                        guh.setInSittingPose(true);
                    }
                    if (level.getRandom().nextInt(4) == 0) {
                        level.sendParticles(ParticleTypes.NOTE, guh.getX(), guh.getY() + 0.9, guh.getZ(), 1, 0.1, 0.05, 0.1, 0);
                    }
                }
            }
        }
    }
}
