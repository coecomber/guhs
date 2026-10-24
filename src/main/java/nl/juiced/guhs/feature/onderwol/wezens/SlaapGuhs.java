package nl.juiced.guhs.feature.onderwol.wezens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * 1.7.0 "Onder de Wol" (round 28): the sleeping guhs of the <b>Slaapgrot</b>. A wild guh that the top-up brings into
 * the Slaapgrot ({@link OnderWolWezens#grotguh}) is mostly fast asleep: it lies curled up (the looping SLAPEN emote: the
 * pose, closed eyes and zzz, all synced through the emote data), doesn't wander (EmoteGoal holds MOVE / LOOK / JUMP)
 * and snores now and then (the emote's soft snore, plus once in a while a {@code guhs:onderwol.snurkje}).
 * <p>
 * Pet it (right-click with an empty hand) and it wakes: a big yawn and a stretch (the GAPEN emote), a happy squeak
 * and hearts; from then on it is a completely normal wild guh. Feeding it kaasknabbels wakes it too (and the feeding
 * goes on as usual). It also wakes when it gets hurt, tamed, leashed, picked up or ridden.
 * <p>
 * Asleep is the persistent-data flag {@link #SLAAPT} (saved with the guh when it is saved at all: a plain wild guh of
 * the Guhmension comes and goes and is never written to disk). The emote itself is not saved for a guh that started it
 * by itself, so {@link #tick} starts it again (after loading, or after a short interruption).
 */
public final class SlaapGuhs {
    /** The Slaapgrot (biome made by the main onderwol worldgen; tools/features/onderwol.py). */
    public static final ResourceKey<Biome> SLAAPGROT = ResourceKey.create(Registries.BIOME, Guhs.id("slaapgrot"));
    /** Persistent data: this wild guh is fast asleep (until somebody pets it). */
    public static final String SLAAPT = "guhs_onderwol_slaapt";
    /** Out of the guhs the top-up brings into the Slaapgrot, this share is asleep. */
    public static final float SLAAP_KANS = 0.8f;
    /** The snurkje: looked at every this many ticks per sleeping guh, 1 in {@link #SNURK_KANS} times, at this volume. */
    public static final int SNURK_TIJD = 100, SNURK_KANS = 4;
    public static final float SNURK_VOLUME = 0.3f;
    /** (not registered here: the sound event of sounds.json; a direct holder is fine for playing it) */
    private static final SoundEvent SNURKJE = SoundEvent.createVariableRangeEvent(Guhs.id("onderwol.snurkje"));

    private SlaapGuhs() {
    }

    /** (OnderWolWezens.register) */
    static void register() {
        GuhHooks.tick(SlaapGuhs::tick);
        GuhHooks.klik(SlaapGuhs::klik);
        NeoForge.EVENT_BUS.addListener(SlaapGuhs::onDamage);
    }

    /** Is this guh one of the Slaapgrot sleepers (server: the flag; it may be between two SLAPEN emotes)? */
    public static boolean slaapt(GuhEntity guh) {
        return !guh.isTame() && guh.getPersistentData().getBooleanOr(SLAAPT, false);
    }

    /** Puts a wild guh to sleep (it curls up as soon as it stands on something). */
    public static void inslapen(GuhEntity guh) {
        if (guh.isTame()) {
            return;
        }
        guh.getPersistentData().putBoolean(SLAAPT, true);
        guh.getNavigation().stop();
        if (GuhEmotes.canStart(guh)) {
            guh.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF);
        }
    }

    /** Wakes it without any fuss (hurt, tamed, picked up...). */
    public static void wakker(GuhEntity guh) {
        guh.getPersistentData().remove(SLAAPT);
        if (guh.emotes.current() == Emote.SLAPEN) {
            guh.emotes.stop();
        }
    }

    /** Petted awake: a yawn and a stretch (GAPEN), a happy squeak, hearts and a note; then a normal wild guh. */
    public static void wakkerGeaaid(GuhEntity guh, Player player) {
        wakker(guh);
        if (guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF)) {
            guh.emotes.setLookTarget(player.getUUID());
        }
        guh.getLookControl().setLookAt(player, 30f, 30f);
        guh.playSound(ModSounds.GUH_HAPPY.get(), 0.8f, guh.getVoicePitch());
        if (guh.level() instanceof ServerLevel level) {
            double y = guh.getY() + guh.getBbHeight() * 1.1;
            level.sendParticles(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), guh.getX(), y, guh.getZ(), 4,
                    guh.getBbWidth() * 0.3, 0.15, guh.getBbWidth() * 0.3, 0.02);
            level.sendParticles(ParticleTypes.NOTE, guh.getX(), y + 0.3, guh.getZ(), 0, level.getRandom().nextInt(25) / 24.0, 0, 0, 1);
        }
    }

    /** (GuhHooks.tick, server, every tick per guh) keeps a sleeper asleep, wakes it when something happened, snores. */
    static void tick(GuhEntity guh) {
        if (!guh.getPersistentData().getBooleanOr(SLAAPT, false)) {
            return;
        }
        if (guh.isTame() || guh.isPassenger() || guh.isVehicle() || guh.isLeashed() || guh.isNoAi()) {
            wakker(guh);
            return;
        }
        if (guh.emotes.current() != Emote.SLAPEN) {
            // (after loading, a wave or a jukebox dance in between, a bob in the water: curl up again once it can)
            if (GuhEmotes.canStart(guh)) {
                guh.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF);
            }
            return;
        }
        if ((guh.tickCount + guh.getId()) % SNURK_TIJD == 0 && guh.getRandom().nextInt(SNURK_KANS) == 0) {
            guh.level().playSound(null, guh.getX(), guh.getY(), guh.getZ(), SNURKJE, SoundSource.NEUTRAL, SNURK_VOLUME,
                    guh.getVoicePitch() * (0.9f + guh.getRandom().nextFloat() * 0.15f));
        }
    }

    /**
     * (GuhHooks.klik) Petting a sleeper (empty main hand) wakes it. With kaasknabbels it wakes too and the click goes on
     * (feeding, taming). Anything else: as usual. The client only knows the SLAPEN emote, so it says SUCCESS for an
     * empty-hand click on a napping wild guh (the arm swings); the server decides.
     */
    static InteractionResult klik(GuhEntity guh, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || guh.isTame() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (guh.level().isClientSide()) {
            return stack.isEmpty() && guh.emotes.current() == Emote.SLAPEN ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!slaapt(guh)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            wakkerGeaaid(guh, player);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.KAAS_KNABBELS.get()) || stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get())) {
            wakkerGeaaid(guh, player);   // (then eats it: the feeding / taming goes on)
        }
        return InteractionResult.PASS;
    }

    /** Getting hurt wakes it (the emote stops by itself, EmotesFeature). */
    static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof GuhEntity guh && !guh.level().isClientSide() && slaapt(guh)) {
            wakker(guh);
        }
    }
}
