package nl.juiced.guhs.feature.emotes;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import com.geckolib.animation.RawAnimation;

/**
 * The emotes of one guh (lives in {@link GuhEntity#emotes}). The emote it is doing is synced as one int
 * ({@link GuhEntity#getEmoteData()}: 0 = none, else the Emote index + 1, plus {@link #LOOP} for "blijven doen");
 * the timers, the wild guh moments and the effects are worked out here.
 * <ul>
 *   <li>Its owner picks one in the Guh menu: "nu" (once) or "blijven doen" (until stopped), and a favourite that a tamed
 *   guh does by itself now and then.</li>
 *   <li>Wild guhs: rarely a random one that fits their personality; they wave at a player who comes close (shy ones hide
 *   their eyes); every guh dances near a playing jukebox.</li>
 *   <li>Never while ridden, in a vehicle, in water, launching, hidden or without AI (show guhs, castle guards); it stops
 *   when the guh gets hurt or goes for a mob, and the guh stands still meanwhile ({@link EmoteGoal}).</li>
 * </ul>
 */
public final class GuhEmotes {
    public static final int LOOP = 0x100;
    /** How the emote started: asked by the owner, by itself (favourite, random, a wave), or a jukebox dance. */
    public enum Source { OWNER, SELF, JUKEBOX }

    /** Wild guhs: 1 in N every 10 seconds does a random emote (so about once in 7 minutes); tamed: its favourite. */
    public static final int WILD_CHANCE = 40, FAVORITE_CHANCE = 12;
    /** Wild guhs wave at a player who comes within this distance, 1 in N times, at most once per cooldown. */
    public static final double WAVE_RANGE = 6, WAVE_RESET_RANGE = 10;
    public static final int WAVE_CHANCE = 3, WAVE_COOLDOWN = 20 * 120;
    /** A playing jukebox this close makes guhs dance (checked every 2 seconds). */
    public static final int JUKEBOX_RANGE = 8, JUKEBOX_DANCE_TICKS = 60;
    /** A looping emote stops when the owner walks this far away (so the guh can follow). */
    public static final double OWNER_LEAVE_RANGE = 16;
    /** After getting hurt: no emotes by itself for a while. */
    public static final int HURT_QUIET_TICKS = 20 * 20;

    private final GuhEntity guh;
    // server
    private int ticksLeft;
    private int elapsed;
    private Source source = Source.OWNER;
    private long quietUntil;
    private long waveCooldownUntil;
    @Nullable
    private UUID nearPlayer;
    @Nullable
    private UUID lookTarget;
    // client
    private int shownData;
    private int clientTicks;

    public GuhEmotes(GuhEntity guh) {
        this.guh = guh;
    }

    // ------------------------------------------------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------------------------------------------------

    @Nullable
    public Emote current() {
        return Emote.byIndex((guh.getEmoteData() & 0xFF) - 1);
    }

    public boolean isLooping() {
        return (guh.getEmoteData() & LOOP) != 0;
    }

    public Source source() {
        return source;
    }

    /** Client: the animation to play instead of idle / walk / sit, or null. */
    @Nullable
    public RawAnimation animation() {
        Emote e = current();
        return e == null ? null : e.animation;
    }

    @Nullable
    public Emote favorite() {
        return Emote.byIndex(guh.getFavoriteEmote());
    }

    public void setFavorite(@Nullable Emote emote) {
        guh.setFavoriteEmote(emote == null ? -1 : emote.ordinal());
    }

    /** The player a waving guh looks at (null = nobody in particular). */
    @Nullable
    public UUID lookTarget() {
        return lookTarget;
    }

    /** 2.10 (samen): the player the running emote is for (a cheer, a hug, a wave): the guh keeps looking at them. */
    public void setLookTarget(@Nullable UUID player) {
        this.lookTarget = player;
    }

    /** Can it do an emote right now? (a running one also stops when this turns false) */
    public static boolean canContinue(GuhEntity guh) {
        return !guh.isVehicle() && vrij(guh);
    }

    private static boolean vrij(GuhEntity guh) {
        return guh.getType() == ModEntities.GUH.get() && guh.isAlive() && !guh.isNoAi() && guh.getHiddenBy() == null
                && !guh.isPassenger() && !guh.isInWater() && !guh.isInLava()
                && guh.getLaunchState() == GuhEntity.LAUNCH_NONE && guh.getTarget() == null;
    }

    /** 1.2.5: the pet you give when you get on a saddled guh: its squish goes on under you, until you ride off. */
    private static boolean aaienBereden(GuhEntity guh, Emote emote) {
        return emote == Emote.AAIEN && guh.isVehicle() && vrij(guh) && guh.getDeltaMovement().horizontalDistanceSqr() < 0.0025;
    }

    /** Can it start one? (also standing on something: not while falling or flying) */
    public static boolean canStart(GuhEntity guh) {
        return canContinue(guh) && (guh.onGround() || guh.isOrderedToSit());
    }

    /** Starts an emote (server): once, or looping until stopped. Returns false if it can't right now. */
    public boolean start(Emote emote, boolean loop, Source source) {
        if (guh.level().isClientSide() || !canStart(guh)) {
            return false;
        }
        this.source = source;
        this.ticksLeft = loop ? -1 : emote.onceTicks;
        this.elapsed = 0;
        this.lookTarget = null;
        guh.setEmoteData((emote.ordinal() + 1) | (loop ? LOOP : 0));
        guh.getNavigation().stop();
        if (guh.isTame() && emote.kiesbaar()) {   // 2.10: the moments bus (favourite emote, dagboek...; petting has its own moment)
            nl.juiced.guhs.feature.band.Band.moment(guh, guh.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner && owner.level() == guh.level()
                    && owner.distanceTo(guh) < 32 ? owner : null, nl.juiced.guhs.feature.band.Moment.EMOTE, emote.id());
        }
        return true;
    }

    public void stop() {
        if (guh.getEmoteData() != 0) {
            guh.setEmoteData(0);
        }
        ticksLeft = 0;
        lookTarget = null;
    }

    /** Hurt: stop, and don't start anything by itself for a while. */
    public void onHurt() {
        stop();
        quietUntil = guh.level().getGameTime() + HURT_QUIET_TICKS;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------------------------

    public void tick() {
        if (guh.level().isClientSide()) {
            clientTick();
            return;
        }
        Emote emote = current();
        if (emote != null) {
            // (a standing guh is held still by EmoteGoal: if something else makes it walk, that went first)
            boolean walking = !guh.isOrderedToSit() && elapsed > 3 && !guh.getNavigation().isDone();
            if (!(canContinue(guh) || aaienBereden(guh, emote)) || ownerLeft() || walking) {
                stop();
            } else {
                elapsed++;
                sounds(emote);
                if (emote == Emote.ZINGEN && elapsed % 20 == 1 && guh.level() instanceof net.minecraft.server.level.ServerLevel server) {
                    nl.juiced.guhs.feature.knus.KnusSignalen.zang(server, guh.blockPosition(), 6);   // (the tuintjes listen)
                }
                if (ticksLeft > 0 && --ticksLeft == 0) {
                    stop();
                }
            }
        }
        if (!canContinue(guh)) {
            return;
        }
        int phase = guh.tickCount + guh.getId();
        if (phase % 40 == 0) {
            checkJukebox();
        }
        if (current() != null || guh.level().getGameTime() < quietUntil) {
            return;
        }
        if (phase % 20 == 0 && !guh.isTame()) {
            checkPlayerNear();
        }
        if (phase % 200 == 0 && current() == null && guh.getNavigation().isDone() && !guh.isInLove()) {
            if (!guh.isTame() && guh.getRandom().nextInt(WILD_CHANCE) == 0) {
                start(randomFor(guh.getPersonality(), guh.level().isDarkOutside(), guh.getRandom().nextInt(100)), false, Source.SELF);
            } else if (guh.isTame() && favorite() != null && guh.getRandom().nextInt(FAVORITE_CHANCE) == 0) {
                start(favorite(), false, Source.SELF);
            }
        }
    }

    /** A looping emote of a tamed, free guh ends when its owner walks away, so it can follow. */
    private boolean ownerLeft() {
        return source == Source.OWNER && isLooping() && guh.isTame() && !guh.isOrderedToSit() && guh.mayWander()
                && guh.getOwner() instanceof Player owner && (owner.level() != guh.level() || owner.distanceTo(guh) > OWNER_LEAVE_RANGE);
    }

    /** The emotes that fit a personality (a random one is picked; at night any guh may doze off). */
    public static List<Emote> personalityEmotes(GuhPersonality personality) {
        return switch (personality) {
            case PLAYFUL -> List.of(Emote.DANSEN, Emote.VAHOEG, Emote.ROLLEN);
            case LAZY -> List.of(Emote.SLAPEN, Emote.SLAPEN, Emote.ROLLEN, Emote.GAPEN);
            case VADSIG -> List.of(Emote.SMAKKEN, Emote.SMAKKEN, Emote.SLAPEN);
            case SHY -> List.of(Emote.VERLEGEN);
            case BRAVE -> List.of(Emote.VAHOEG, Emote.ZWAAIEN);
            case CHATTY -> List.of(Emote.ZWAAIEN, Emote.DANSEN, Emote.ZINGEN);
            case CUDDLY -> List.of(Emote.ROLLEN, Emote.VERLEGEN, Emote.KNUFFELEN);
            case CURIOUS -> List.of(Emote.ZWAAIEN, Emote.VAHOEG);
        };
    }

    /** roll = 0..99. */
    public static Emote randomFor(GuhPersonality personality, boolean night, int roll) {
        if (night && roll < 35) {
            return Emote.SLAPEN;
        }
        List<Emote> list = personalityEmotes(personality);
        return list.get(roll % list.size());
    }

    /** Wild guhs: a player who comes close gets a wave (sometimes; shy guhs hide their eyes instead). */
    private void checkPlayerNear() {
        Player player = guh.level().getNearestPlayer(guh.getX(), guh.getY(), guh.getZ(), WAVE_RANGE, EntitySelector.NO_SPECTATORS);
        if (player == null) {
            if (guh.level().getNearestPlayer(guh.getX(), guh.getY(), guh.getZ(), WAVE_RESET_RANGE, EntitySelector.NO_SPECTATORS) == null) {
                nearPlayer = null;
            }
            return;
        }
        if (player.getUUID().equals(nearPlayer)) {
            return;
        }
        nearPlayer = player.getUUID();
        if (guh.level().getGameTime() >= waveCooldownUntil && guh.getRandom().nextInt(WAVE_CHANCE) == 0 && !player.isInvisible()) {
            greet(player);
        }
    }

    /** Waves at a player (a shy guh goes "verlegen"). */
    public boolean greet(Player player) {
        boolean shy = guh.getPersonality() == GuhPersonality.SHY;
        if (shy && !guh.isTame() && !player.isHolding(ModItems.KAAS_KNABBELS.get())) {
            return false; // it keeps its distance instead (unless you have kaasknabbels)
        }
        Emote emote = shy ? Emote.VERLEGEN : Emote.ZWAAIEN;
        if (!start(emote, false, Source.SELF)) {
            return false;
        }
        lookTarget = player.getUUID();
        waveCooldownUntil = guh.level().getGameTime() + WAVE_COOLDOWN;
        if (player instanceof ServerPlayer sp && emote == Emote.ZWAAIEN) {
            GuhAdvancements.grant(sp, "emote_gezwaaid");
        }
        return true;
    }

    /** A playing jukebox close by: dance (and keep dancing while it plays). The owner's own emotes go first. */
    private void checkJukebox() {
        Emote emote = current();
        boolean jukeboxDance = emote == Emote.DANSEN && source == Source.JUKEBOX;
        if (emote != null && !jukeboxDance) {
            return;
        }
        BlockPos jukebox = playingJukebox(guh.level(), guh.blockPosition(), JUKEBOX_RANGE);
        if (jukebox == null) {
            return;
        }
        if (jukeboxDance) {
            ticksLeft = Math.max(ticksLeft, JUKEBOX_DANCE_TICKS);
        } else if (start(Emote.DANSEN, false, Source.JUKEBOX)) {
            ticksLeft = JUKEBOX_DANCE_TICKS;
            Player near = guh.level().getNearestPlayer(guh, 16);
            if (near instanceof ServerPlayer sp) {
                GuhAdvancements.grant(sp, "emote_jukebox");
            }
        }
    }

    /** The nearest jukebox that is playing, within range (only loaded chunks), or null. */
    @Nullable
    public static BlockPos playingJukebox(Level level, BlockPos center, int range) {
        BlockPos best = null;
        double bestDist = (double) range * range;
        for (int cx = (center.getX() - range) >> 4; cx <= (center.getX() + range) >> 4; cx++) {
            for (int cz = (center.getZ() - range) >> 4; cz <= (center.getZ() + range) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof JukeboxBlockEntity jukebox && jukebox.getSongPlayer().isPlaying()) {
                        double d = be.getBlockPos().distSqr(center);
                        if (d <= bestDist) {
                            bestDist = d;
                            best = be.getBlockPos();
                        }
                    }
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Effects: sounds on the server, particles on each client (from the synced state)
    // ------------------------------------------------------------------------------------------------------------

    private void sounds(Emote emote) {
        if (guh.isTame() && !guh.areSoundsEnabled()) {
            return; // "sounds off" in the Guh menu: quiet emotes too
        }
        float pitch = guh.getVoicePitch();
        switch (emote) {
            case ZWAAIEN -> play(elapsed == 2, ModSounds.GUH_AMBIENT.get(), 0.6f, pitch * 1.1f);
            case SLAPEN -> play(elapsed % 60 == 30, SoundEvents.FOX_SLEEP, 0.4f, pitch * 0.75f); // soft snoring
            case VAHOEG -> play(elapsed % 22 == 5, ModSounds.GUH_HAPPY.get(), 1f, pitch);
            case ROLLEN -> play(elapsed % 40 == 12, SoundEvents.WOOL_FALL, 0.5f, 1.2f);
            case SMAKKEN -> play(elapsed % 24 == 4, ModSounds.GUH_EAT.get(), 0.45f, pitch * 1.1f);
            case VERLEGEN -> play(elapsed == 2, ModSounds.GUH_AMBIENT.get(), 0.4f, pitch * 1.3f);
            case GAPEN -> play(elapsed == 8, ModSounds.GUH_AMBIENT.get(), 0.5f, pitch * 0.7f);
            case ZINGEN -> play(elapsed % 16 == 2, ModSounds.GUH_AMBIENT.get(), 0.55f, pitch * (1.0f + 0.12f * ((elapsed / 16) % 4)));
            case KNUFFELEN -> play(elapsed == 10, ModSounds.GUH_HAPPY.get(), 0.6f, pitch * 1.2f);
            // 2.10: blowing kisses (a soft smooch and a chime per heart), the twirl dance (happy squeaks going up and down),
            // the bff-knuffel (a happy squeak, then the warm bff chime), the verdrietje (a small sigh, "ooh njeg...")
            case HARTJES -> {
                play(elapsed % 20 == 8, ModSounds.GUH_AMBIENT.get(), 0.35f, pitch * 1.45f);
                play(elapsed % 20 == 10, nl.juiced.guhs.feature.band.BandFeature.HARTJES_GELUID.get(), 0.35f, 1.3f);
            }
            case KNUFFELDANSJE -> play(elapsed % 24 == 4, ModSounds.GUH_HAPPY.get(), 0.55f, pitch * (1.0f + 0.1f * ((elapsed / 24) % 3)));
            case BFF_KNUFFEL -> {
                play(elapsed == 6, ModSounds.GUH_HAPPY.get(), 0.7f, pitch * 1.2f);
                play(elapsed == 14, nl.juiced.guhs.feature.samen.SamenFeature.BFF.get(), 0.9f, 1f);
            }
            case VERDRIETJE -> {
                play(elapsed == 4, nl.juiced.guhs.feature.samen.SamenFeature.OOH.get(), 0.6f, 1f);
                play(elapsed == 30, ModSounds.GUH_AMBIENT.get(), 0.35f, pitch * 0.72f);
            }
            // 1.2.0: being petted (the happy squeak itself comes with the tap, BandEvents.aai): a soft content sigh at the wiggle
            case AAIEN -> play(elapsed == 14, ModSounds.GUH_AMBIENT.get(), 0.35f, pitch * 1.35f);
            default -> {
            }
        }
    }

    private void play(boolean now, SoundEvent sound, float volume, float pitch) {
        if (now) {
            guh.playSound(sound, volume, pitch);
        }
    }

    private void clientTick() {
        int data = guh.getEmoteData();
        if (data != shownData) {
            shownData = data;
            clientTicks = 0;
        }
        Emote emote = current();
        if (emote == null) {
            return;
        }
        clientTicks++;
        Level level = guh.level();
        Vec3 forward = Vec3.directionFromRotation(0, guh.yBodyRot);
        double w = guh.getBbWidth(), h = guh.getBbHeight();
        Vec3 head = guh.position().add(forward.scale(w * 0.4)).add(0, h * 1.05, 0);
        var random = guh.getRandom();
        switch (emote) {
            case SLAPEN -> {
                if (clientTicks % 30 == 10) {
                    level.addParticle(EmotesFeature.GUH_ZZZ.get(), head.x, head.y, head.z, 0.01 + random.nextDouble() * 0.01, 0.03, 0);
                }
            }
            case VAHOEG -> {
                if (clientTicks % 22 == 6) {
                    level.addParticle(EmotesFeature.GUH_VAHOEG.get(), guh.getX(), guh.getY() + h * 1.6 + 0.3, guh.getZ(), 0, 0.04, 0);
                }
            }
            case SMAKKEN -> {
                if (clientTicks % 5 == 0) {
                    Vec3 mouth = guh.position().add(forward.scale(w * 0.6)).add(0, h * 0.35, 0);
                    level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, ModItems.KAAS_KNABBELS.get()),
                            mouth.x, mouth.y, mouth.z, (random.nextDouble() - 0.5) * 0.08, 0.05 + random.nextDouble() * 0.05,
                            (random.nextDouble() - 0.5) * 0.08);
                }
            }
            case VERLEGEN -> {
                if (clientTicks % 30 == 5) {
                    level.addParticle(ParticleTypes.HEART, head.x + (random.nextDouble() - 0.5) * w * 0.6, head.y, head.z, 0, 0.05, 0);
                }
            }
            case DANSEN -> {
                if (clientTicks % 12 == 2) {
                    level.addParticle(ParticleTypes.NOTE, head.x + (random.nextDouble() - 0.5) * w, head.y + 0.1,
                            head.z + (random.nextDouble() - 0.5) * w, random.nextInt(25) / 24.0, 0, 0);
                }
            }
            case ZINGEN -> {
                if (clientTicks % 10 == 3) {
                    level.addParticle(ParticleTypes.NOTE, head.x + (random.nextDouble() - 0.5) * w * 0.8, head.y + 0.15,
                            head.z + (random.nextDouble() - 0.5) * w * 0.8, random.nextInt(25) / 24.0, 0, 0);
                }
            }
            case KNUFFELEN -> {
                if (clientTicks % 12 == 4) {
                    level.addParticle(ParticleTypes.HEART, head.x + (random.nextDouble() - 0.5) * w * 0.6, head.y, head.z, 0, 0.05, 0);
                }
            }
            case GAPEN -> {
                if (clientTicks == 10) {
                    Vec3 mouth = guh.position().add(forward.scale(w * 0.6)).add(0, h * 0.55, 0);
                    level.addParticle(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, forward.x * 0.02, 0.02, forward.z * 0.02);
                }
            }
            case ROLLEN -> {
                if (clientTicks % 40 == 12) {
                    for (int i = 0; i < 4; i++) {
                        level.addParticle(ParticleTypes.POOF, guh.getRandomX(0.8), guh.getY() + 0.1, guh.getRandomZ(0.8), 0, 0.02, 0);
                    }
                }
            }
            // 2.10
            case HARTJES -> {
                if (clientTicks % 20 == 10) {           // a heart blown from the snoet, floating towards you
                    Vec3 snoet = guh.position().add(forward.scale(w * 0.62)).add(0, h * 0.5, 0);
                    level.addParticle(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), snoet.x, snoet.y, snoet.z,
                            forward.x * 0.06, 0.035, forward.z * 0.06);
                }
            }
            case KNUFFELDANSJE -> {
                if (clientTicks % 12 == 3) {
                    boolean noot = clientTicks % 24 == 3;
                    level.addParticle(noot ? ParticleTypes.NOTE : nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(),
                            head.x + (random.nextDouble() - 0.5) * w, head.y + 0.1, head.z + (random.nextDouble() - 0.5) * w,
                            noot ? random.nextInt(25) / 24.0 : 0, noot ? 0 : 0.04, 0);
                }
            }
            case BFF_KNUFFEL -> {
                if (clientTicks % 8 == 2) {
                    double a = clientTicks * 0.7;
                    level.addParticle(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), head.x + Math.cos(a) * w * 0.5, head.y + 0.1,
                            head.z + Math.sin(a) * w * 0.5, 0, 0.05, 0);
                }
            }
            case AAIEN -> {
                if (clientTicks == 4 || clientTicks == 14) {   // little hearts popping up from its head as it squishes
                    level.addParticle(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), head.x + (random.nextDouble() - 0.5) * w * 0.5,
                            head.y + 0.1, head.z + (random.nextDouble() - 0.5) * w * 0.5, 0, 0.04, 0);
                }
            }
            case VERDRIETJE -> {
                if (clientTicks % 18 == 9) {            // a little tear rolls down
                    Vec3 right = new Vec3(-forward.z, 0, forward.x);
                    double kant = clientTicks % 36 == 9 ? 0.18 : -0.18;
                    Vec3 eye = guh.position().add(forward.scale(w * 0.5)).add(right.scale(w * kant)).add(0, h * 0.62, 0);
                    level.addParticle(ParticleTypes.FALLING_WATER, eye.x, eye.y, eye.z, 0, 0, 0);
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Saving (the favourite, and an emote it keeps doing)
    // ------------------------------------------------------------------------------------------------------------

    public void save(CompoundTag tag) {
        if (favorite() != null) {
            tag.putString("FavoriteEmote", favorite().id());
        }
        if (current() != null && isLooping() && source == Source.OWNER) {
            tag.putString("LoopEmote", current().id());
        }
    }

    public void load(CompoundTag tag) {
        setFavorite(Emote.byId(tag.getStringOr("FavoriteEmote", "")));
        Emote loop = Emote.byId(tag.getStringOr("LoopEmote", ""));
        if (loop != null) {
            source = Source.OWNER;
            ticksLeft = -1;
            guh.setEmoteData((loop.ordinal() + 1) | LOOP);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The owner's choice (from the emote picker, see EmotePayload)
    // ------------------------------------------------------------------------------------------------------------

    /** The bits of every emote that counts for "alle emotes": all but the hartjes-level emotes (2.10, samen). */
    public static int alleNietBand() {
        int mask = 0;
        for (Emote e : Emote.values()) {
            if (!nl.juiced.guhs.feature.samen.SamenBeloning.isBandEmote(e) && Emote.alleenVoor(e) == null && e.kiesbaar()) {   // (3.0: nor the ukelele; 1.2.0: nor petting)
                mask |= 1 << e.ordinal();
            }
        }
        return mask;
    }

    /** Remembers which emotes a player had their guhs do (survives death) and grants the quest advancements. */
    static void countForPlayer(ServerPlayer player, Emote emote) {
        GuhAdvancements.grant(player, "emote_gedaan");
        CompoundTag saved = GuhQuests.saved(player);
        int done = saved.getIntOr("guhs_emotes_done", 0) | (1 << emote.ordinal());
        saved.putInt("guhs_emotes_done", done);
        int alle = alleNietBand();
        if ((done & alle) == alle) {             // (2.10: the three hartjes emotes don't count: they're unlocks, see samen)
            GuhAdvancements.grant(player, "emote_alle");
        }
    }
}
