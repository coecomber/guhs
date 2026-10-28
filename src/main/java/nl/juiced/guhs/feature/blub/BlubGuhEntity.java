package nl.juiced.guhs.feature.blub;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.landdiertjes.Landdiertje;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.Schouder;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

/**
 * 1.8.0 (Guhdromenland): the Blub-guh, a guh-ified Blub from Minecraft Dungeons II. One soft block of a body with two flat
 * little ears, four stubby legs and three stripes for a face... and a bit of guh: its {@link BlubTrekking Blub-Guh-Meter}
 * (0 = a pure Blub, 100 = almost a guh) decides how much (blush, guh eyes, a whole guh face, pink ears, round ear tips, real
 * guh ears, and its colour slides to guh pink). Its soort (blauw, weide, schildje, ravijn, zwam), tint and size are dice too;
 * they never change. Babyblubjes get numbers between their parents' ({@link BlubTrekking#erf}).
 * <ul>
 *   <li>A Landdiertje / piep-maatje: tame it with a kaasknabbel (wild ones only once you helped Snoes: {@link BlubSnoes}), pick it up, Guhhuisje,
 *       "Mijn andere vadsjes", the Knabbelvelden, your shoulder (its big button).</li>
 *   <li>Like the real one: it can't be hurt (only /kill) and can't be pushed. It hops and squeaks.</li>
 *   <li>Its own menu ({@link BlubMenu}): name, strikje, volgen/blijven, op je schouder, oppakken, slaapliedje piepen,
 *       verstoppertje, trucjes, hupsen.</li>
 *   <li>By itself: ears show how it feels (rechtop / plat / krul), jumps in puddles when it rains and shakes itself dry,
 *       plays tikkertje with guhs and shares a knabbel with them, sleeps with a guh or in a guhnestje at night (zzz and a
 *       dream cloud), blows bubbles (in the water: its owner nearby gets longer breath).</li>
 * </ul>
 */
public class BlubGuhEntity extends Landdiertje {
    /** A kaasknabbel tames one 1 in this many times. */
    public static final int TAME_CHANCE = 3;
    /** Ear emotions (synced, the renderer turns the ears / antennae). */
    public static final int RECHTOP = 0, PLAT = 1, KRUL = 2;
    /** Verstoppertje: how far it hides, how long you have. */
    public static final int VERSTOP_BEREIK = 12, VERSTOP_TICKS = 20 * 60;

    private static final EntityDataAccessor<Integer> DATA_METER = SynchedEntityData.defineId(BlubGuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SOORT = SynchedEntityData.defineId(BlubGuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TINT = SynchedEntityData.defineId(BlubGuhEntity.class, EntityDataSerializers.INT);
    /** What it wears (BlubKleding): "plek=stuk;plek=stuk" (a string: there is no synced compound in 26.1). */
    private static final EntityDataAccessor<String> DATA_KLEDING = SynchedEntityData.defineId(BlubGuhEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_EMOTIE = SynchedEntityData.defineId(BlubGuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SLAAPT = SynchedEntityData.defineId(BlubGuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_VERSTOPT = SynchedEntityData.defineId(BlubGuhEntity.class, EntityDataSerializers.BOOLEAN);

    static final RawAnimation SLAAP = RawAnimation.begin().thenLoop("slaap");

    /** Its dice were rolled (by a spawn, a command or its parents): finalizeSpawn leaves them alone. */
    private boolean getrokken;
    private long emotieTot, volgendeBel, volgendePlas, volgendeBlij, volgendeKrul, volgendeGlinster;
    /** Verstoppertje: until when it hides (0: not), where. */
    private long verstopTot;
    @Nullable
    private BlockPos verstopPlek;
    /** Hupsen: hops still to do, done so far, its record; ticks until the next hop. */
    private int hupsenOver, hupsenGedaan, hupsRecord, hupsWacht;
    /** The slaapliedje: ticks it still sings (0: not). */
    private int liedje;
    /** How long the lullaby (one sound file, blubguh.liedje / blubguh.klus_zingen) lasts: 23.5 s after the import's trim. */
    public static final int LIEDJE_TICKS = 20 * 24;
    /** Asleep: the next little sleep sound (every 3.5-5 seconds). */
    private long volgendSlaapGeluid;
    /** Ticks in the air (for the landing sound). */
    private int inDeLucht;
    @Nullable
    private ServerPlayer liedjeVoor;

    public BlubGuhEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // --- the dice ----------------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_METER, BlubTrekking.METER_MIDDEN);
        builder.define(DATA_SOORT, 0);
        builder.define(DATA_TINT, 0);
        builder.define(DATA_KLEDING, "");
        builder.define(DATA_EMOTIE, RECHTOP);
        builder.define(DATA_SLAAPT, false);
        builder.define(DATA_VERSTOPT, false);
    }

    public int meter() {
        return entityData.get(DATA_METER);
    }

    public BlubTrekking.Soort blubSoort() {
        return BlubTrekking.Soort.van(entityData.get(DATA_SOORT));
    }

    public int tint() {
        return entityData.get(DATA_TINT);
    }

    public float grootte() {
        var a = getAttribute(Attributes.SCALE);
        return a == null ? 1f : (float) a.getBaseValue();
    }

    /** The size in whole percents (75..125). */
    public int grootteProcent() {
        return BlubTrekking.procent(grootte());
    }

    public BlubTrekking.Waarden waarden() {
        return new BlubTrekking.Waarden(meter(), blubSoort(), tint(), grootte());
    }

    /** Sets all its dice at once (spawn, command, parents, test). They don't change after that in normal play. */
    public void zetWaarden(BlubTrekking.Waarden w) {
        entityData.set(DATA_METER, Mth.clamp(w.meter(), 0, 100));
        entityData.set(DATA_SOORT, w.soort().ordinal());
        entityData.set(DATA_TINT, Mth.clamp(w.tint(), 0, 2));
        var a = getAttribute(Attributes.SCALE);
        if (a != null) {
            a.setBaseValue(BlubTrekking.rond(w.grootte()));   // whole percents 75..125 (old floats are rounded here)
        }
        getrokken = true;
        refreshDimensions();
    }

    /** What it wears on this place (null: nothing). */
    @Nullable
    public BlubKleding.Stuk draagt(BlubKleding.Plek plek) {
        return BlubKleding.stuk(kleding().getOrDefault(plek.id(), ""));
    }

    /** What it wears, plek id -> stuk id. */
    public java.util.Map<String, String> kleding() {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        for (String deel : entityData.get(DATA_KLEDING).split(";")) {
            int i = deel.indexOf('=');
            if (i > 0) {
                m.put(deel.substring(0, i), deel.substring(i + 1));
            }
        }
        return m;
    }

    private void zetKleding(java.util.Map<String, String> m) {
        StringBuilder b = new StringBuilder();
        m.forEach((k, v) -> b.append(b.isEmpty() ? "" : ";").append(k).append('=').append(v));
        entityData.set(DATA_KLEDING, b.toString());
    }

    /** 1.8.0: what a Blub-knuffel keeps of this Blub (its dice, its clothes, its name): see knuffel.BlubKnuffelBlockEntity. */
    public net.minecraft.nbt.CompoundTag knuffelData() {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putInt("Meter", meter());
        t.putString("BlubSoort", blubSoort().id());
        t.putInt("Tint", tint());
        t.putInt("Grootte", grootteProcent());
        t.putString("Kleding", entityData.get(DATA_KLEDING));
        if (hasCustomName()) {
            nl.juiced.guhs.taal.Tekst.put(t, "Naam", getCustomName());
        }
        return t;
    }

    /** Looks like this knuffel's Blub (the client copy a Blub-knuffel draws). */
    public void laadKnuffel(net.minecraft.nbt.CompoundTag t) {
        zetWaarden(new BlubTrekking.Waarden(t.getIntOr("Meter", BlubTrekking.METER_MIDDEN), BlubTrekking.Soort.van(t.getStringOr("BlubSoort", "")),
                t.getIntOr("Tint", 0), BlubTrekking.uitProcent(t.getIntOr("Grootte", 100))));
        entityData.set(DATA_KLEDING, t.getStringOr("Kleding", ""));
    }

    /** Dresses it (stuk null: takes the piece of this place off). */
    public void kleedAan(BlubKleding.Plek plek, @Nullable BlubKleding.Stuk stuk) {
        java.util.Map<String, String> m = kleding();
        if (stuk == null) {
            m.remove(plek.id());
        } else {
            m.put(plek.id(), stuk.id());
        }
        zetKleding(m);
    }

    /** The colour of its strikje (0xRRGGBB), -1: none. */
    public int strik() {
        BlubKleding.Stuk s = draagt(BlubKleding.Plek.STRIK);
        return s == null ? -1 : s.kleur();
    }

    public int emotie() {
        return entityData.get(DATA_EMOTIE);
    }

    public void emotie(int e, int ticks) {
        entityData.set(DATA_EMOTIE, e);
        emotieTot = level().getGameTime() + ticks;
    }

    public boolean slaapt() {
        return entityData.get(DATA_SLAAPT);
    }

    public void setSlaapt(boolean ja) {
        if (slaapt() && !ja && !level().isClientSide()) {
            BlubGeluid.WAKKER.speel(this);
        }
        entityData.set(DATA_SLAAPT, ja);
    }

    /** Ears by hand (its Trucjes): up, flat or curled for a few seconds. */
    public void oortjes(int e) {
        emotie(e, 100);
        (e == PLAT ? BlubGeluid.SCHRIK : e == KRUL ? BlubGeluid.NIEUWSGIERIG : BlubGeluid.BLIJ).speel(this);
    }

    public boolean verstopt() {
        return entityData.get(DATA_VERSTOPT);
    }

    public int hupsRecord() {
        return hupsRecord;
    }

    /** "Pure Blub" (exactly 0) and "Volle Guh" (exactly 100) are rare and have a title. */
    @Nullable
    public String titel() {
        int m = meter();
        return m == 0 ? "puur" : m == 100 ? "vol" : null;
    }

    /** Wild: "Blub" (or its title: "Pure Blub", "Volle Guh"). */
    @Override
    protected Component getTypeName() {
        String t = titel();
        return t == null ? super.getTypeName() : Component.translatable("entity.guhs.blubguh." + t);
    }

    /** Its own name, and a Pure Blub / Volle Guh keeps its title next to it ("Bubbels \u2726 Volle Guh"). */
    @Override
    public Component getDisplayName() {
        Component naam = super.getDisplayName();
        String t = titel();
        if (t == null || !hasCustomName()) {
            return naam;
        }
        return naam.copy().append(Component.literal(" \u2726 ").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("entity.guhs.blubguh." + t).withStyle(ChatFormatting.GOLD));
    }

    /** Like a guh: its name always floats above it (not while it hides). */
    @Override
    public boolean shouldShowName() {
        return !verstopt() && !isInvisible();
    }

    /** (Client) a wild Blub you can't see yet (Snoes!) can't be clicked either. */
    @Override
    public boolean isPickable() {
        if (level().isClientSide() && !isTame() && !BlubSnoes.inDroom(level()) && !BlubSnoes.Client.geholpen) {
            return false;
        }
        return super.isPickable();
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        if (!getrokken) {
            // (1.8.0: in De Zingende Weide the kind follows the part of the meadow)
            zetWaarden(BlubTrekking.trek(random, nl.juiced.guhs.feature.zingendeweide.WeideDeel.blubSoort(level, blockPosition())));
        }
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    // --- what a Landdiertje asks ---------------------------------------------------------------------------------------------------

    @Override
    public String soort() {
        return "blubguh";
    }

    @Override
    public Item oppakItem() {
        return BlubFeature.BLUBGUH_ITEM.get();
    }

    /** Kaasknabbels: taming, snacks, falling in love. */
    @Override
    public boolean isVoer(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get());
    }

    @Override
    public int temKans() {
        return TAME_CHANCE;
    }

    @Override
    protected SoundEvent geluid() {
        return BlubGeluid.IDLE.get();
    }

    /** Which pick-up sound the shared maatje code (PiepDierItem, Schouder) gets next. */
    @Nullable
    private BlubGeluid oppakModus;
    /** Saved while it sits on a shoulder: the next time it comes down it is "eraf" (not "neerzetten"). */
    private boolean vanSchouder;

    /** The shared code plays this at a fixed pitch (1.5 up, 1.3 down); sounds.json undoes that (blub_audio_import.py). */
    @Override
    public SoundEvent oppakGeluid() {
        BlubGeluid g = oppakModus != null ? oppakModus : vanSchouder ? BlubGeluid.SCHOUDER_AF : BlubGeluid.NEERZETTEN;
        oppakModus = null;
        vanSchouder = false;
        return g.get();
    }

    @Override
    public boolean kanOpSchouder() {
        return true;
    }

    @Override
    public float schouderSchaal() {
        return 0.8f;
    }

    /** Its strikje for the Guhdex tab "Mijn andere vadsjes": the colour and the name of the piece. */
    @Override
    public net.minecraft.nbt.CompoundTag rijLook() {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        BlubKleding.Stuk s = draagt(BlubKleding.Plek.STRIK);
        if (s != null) {
            t.putInt("Strik", s.kleur());
            nl.juiced.guhs.taal.Tekst.put(t, "StrikNaam", s.naam());
        }
        return t;
    }

    /** 1.8.0: a Blub sits on top of your head (like a little hat), not on your shoulder. */
    @Override
    public boolean opHoofd() {
        return true;
    }

    @Override
    public boolean isBezig() {
        return verstopt() || hupsenOver > 0;
    }

    /** Its big button (and "Op je schouder" in its menu): onto its owner's shoulder. */
    @Override
    public void speciaal(ServerPlayer player) {
        if (Schouder.heeft(player)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.landdiertjes.schouder_vol").withStyle(ChatFormatting.GRAY));
            return;
        }
        Component naam = getDisplayName();
        setSlaapt(false);
        oppakModus = BlubGeluid.SCHOUDER_OP;
        vanSchouder = true;                                       // (saved with it on the shoulder: next time down = eraf)
        Schouder.zet(player, this);
        player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.op_schouder", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // --- like the real Blub: no damage, no pushing -----------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);              // (/kill, the void)
        }
        if (source.getEntity() != null || source.getDirectEntity() != null) {
            emotie(PLAT, 60);                                             // a fright: ears flat, a squeak
            BlubGeluid.SCHRIK.speel(this);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    protected int calculateFallDamage(double distance, float multiplier) {
        return 0;
    }

    /**
     * Wild Blubs stay, like vanilla animals (fix 1.8.0 playtest): they mostly spawn once, with the chunk, and a creature
     * that despawns when you walk away hardly ever comes back (the creature cap is full of guhs), so the Zingende Weide was
     * empty by the time you got there.
     */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // --- babyblubjes -------------------------------------------------------------------------------------------------------------

    @Override
    protected void eigenDoelen() {
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(2, new SlaapGoal());
        goalSelector.addGoal(3, new HupsGoal());
        goalSelector.addGoal(3, new VerstopGoal());
        goalSelector.addGoal(5, new TikkertjeGoal());
        goalSelector.addGoal(5, new DeelGoal());
        goalSelector.addGoal(3, new KnuffelGoal());
    }

    @Override
    public boolean canMate(Animal other) {
        return other != this && other instanceof BlubGuhEntity b && isTame() && b.isTame() && isInLove() && b.isInLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        if (!(other instanceof BlubGuhEntity ander)) {
            return null;
        }
        BlubGuhEntity baby = BlubFeature.BLUBGUH.get().create(level, EntitySpawnReason.BREEDING);
        if (baby == null) {
            return null;
        }
        baby.zetWaarden(BlubTrekking.erf(waarden(), ander.waarden(), random));
        BlubGeluid.BABY.speel(this);
        if (getOwner() instanceof Player owner) {
            baby.tame(owner);
            if (owner instanceof ServerPlayer sp) {
                nl.juiced.guhs.feature.dexdromen.BlubMeterkaart.vul(sp, baby);   // (1.8.0: a box on the meterkaart)
            }
        }
        return baby;
    }

    /** Tamed: a kaasknabbel makes it fall in love (when it can), otherwise a happy snack. Wild: taming, as every Landdiertje. */
    @Override
    protected InteractionResult voer(ServerPlayer player, ItemStack stack) {
        if (isTame() && !isBaby() && canFallInLove()) {
            stack.consume(1, player);
            eetEffect(stack);
            setInLove(player);
            return InteractionResult.SUCCESS;
        }
        return super.voer(player, stack);
    }

    // --- clicking ----------------------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isTame() && !BlubSnoes.zichtbaar(this, player)) {
            // a wild Blub only knows you once you helped Snoes fall asleep (until then you can't even see it)
            if (!level().isClientSide() && isVoer(stack)) {
                ((ServerPlayer) player).sendOverlayMessage(Component.translatable("gui.guhs.blubguh.snoes_eerst").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.PASS;
        }
        if (verstopt()) {
            if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
                gevonden((ServerPlayer) player);
            }
            return InteractionResult.SUCCESS;
        }
        boolean voer = isVoer(stack);
        boolean leeg = stack.isEmpty() && hand == InteractionHand.MAIN_HAND;
        if (!voer && !leeg) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (voer && !isTame() && !BlubSnoes.heeft(sp)) {
            // in the dream wild Blubs can be seen, but they only become yours after Snoes' story (Bubbels is the exception)
            sp.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.snoes_eerst_droom").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        if (voer) {
            return voer(sp, stack);
        }
        if (player.isSecondaryUseActive() && isTame() && isOwnedBy(player)) {
            oppakModus = BlubGeluid.OPPAKKEN;
            PiepDierItem.pakOp(this, sp);
            return InteractionResult.SUCCESS;
        }
        squish(sp);
        if (isTame() && isOwnedBy(player) && !isBezig()) {
            BlubMenu.open(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    /** A squish-knuffel: flat as a pudding, a squeak, hearts, ears up (and it wakes up). */
    public void squish(@Nullable ServerPlayer player) {
        setSlaapt(false);
        if (player != null) {
            aaien(player);
        } else if (level() instanceof ServerLevel sl) {
            deeltjes(sl, ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 2, 0.2, 0.1, 0.2, 0);
        }
        triggerAnim("actie", "squish");
        BlubGeluid.SQUISH.speel(this);
        emotie(RECHTOP, 80);
    }

    // --- the menu's things ---------------------------------------------------------------------------------------------------------

    /** A trick: 0 zitten (switches Rondvadsen), 1 een rondje, 2 dood spelen (and up again), 3 een hoge sprong. Guhs nearby cheer. */
    public void truc(@Nullable ServerPlayer player, int welke) {
        setSlaapt(false);
        switch (welke) {
            case 0 -> zet(nl.juiced.guhs.feature.piep.PiepInstelling.RONDVADSEN, !aan(nl.juiced.guhs.feature.piep.PiepInstelling.RONDVADSEN));
            case 1 -> triggerAnim("actie", "rondje");
            case 2 -> triggerAnim("actie", "dood");
            default -> {
                triggerAnim("actie", "sprong");
                setDeltaMovement(getDeltaMovement().add(0, 0.62, 0));
                needsSync = true;
            }
        }
        (welke == 0 ? BlubGeluid.TRUC_ZIT : welke == 1 ? BlubGeluid.TRUC_RONDJE : welke == 2 ? BlubGeluid.TRUC_DOOD : BlubGeluid.TRUC_SPRONG).speel(this);
        if (welke == 2) {
            weerOp = level().getGameTime() + 46;                       // (the "dood" animation gets up at 2.3 s)
        }
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.truc_melding." + welke, getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (welke != 0) {
            juichen(welke == 2 ? 70 : 20);
        }
    }

    /** Guhs within 8 blocks cheer for a trick (a little later: after the trick). */
    private void juichen(int na) {
        juichOp = level().getGameTime() + na;
    }

    private long juichOp, weerOp;

    // --- by itself: now and then a trick (about once a minute) -------------------------------------------------------------------

    /** Game time of its next try at a trick of its own. */
    private long volgendeTruc = -1;

    /** May it do a trick of its own right now? Not asleep, hiding, busy, riding, on a shoulder or walking somewhere. */
    public boolean magZelfTruc() {
        return !isRemoved() && !slaapt() && !verstopt() && !isBezig() && !isPassenger() && !opSchouder && liedje == 0 && weerOp == 0
                && knuffelGuh == null && !getNavigation().isInProgress();
    }

    /** Is there room around it for hupsen (a ring of about 2 blocks)? */
    private boolean vrijOmHeen() {
        BlockPos hier = blockPosition();
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            if (!kanStaan(hier.offset((int) Math.round(Math.cos(a) * 2), 0, (int) Math.round(Math.sin(a) * 2)))) {
                return false;
            }
        }
        return true;
    }

    /** Does one random trick of its own (BlubTruc#zelf; hupsen only with room). Returns it, or null when it can't now. */
    @Nullable
    public BlubTruc zelfTruc() {
        if (!magZelfTruc()) {
            return null;
        }
        java.util.List<BlubTruc> kan = new java.util.ArrayList<>();
        for (BlubTruc t : BlubTruc.values()) {
            if (t.zelf && (t != BlubTruc.HUPSEN || vrijOmHeen())) {
                kan.add(t);
            }
        }
        BlubTruc t = kan.get(random.nextInt(kan.size()));
        t.doe(this, null);
        return t;
    }

    // --- a hug with a guh --------------------------------------------------------------------------------------------------------

    /** The guh it is on its way to hug (null: none). */
    @Nullable
    private GuhEntity knuffelGuh;
    private long volgendeKnuffel = -1;
    /** How far it looks for a guh to hug. */
    public static final int KNUFFEL_BEREIK = 8;

    /** The trick "Knuffelen met een guh": the nearest tame guh within 8 blocks (its own owner's first), or a little joke. */
    public boolean knuffelGuh(@Nullable ServerPlayer player) {
        GuhEntity g = knuffelPartner();
        if (g == null) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.knuffel.geen", getDisplayName()).withStyle(ChatFormatting.GRAY));
            }
            return false;
        }
        setSlaapt(false);
        knuffelGuh = g;
        return true;
    }

    /** A tame guh nearby that is free for a hug: its own owner's guhs first, then the nearest. */
    @Nullable
    public GuhEntity knuffelPartner() {
        java.util.UUID eigen = nl.juiced.guhs.entity.Owners.uuid(this);
        return level().getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(KNUFFEL_BEREIK), g -> g.isAlive() && g.isTame()
                        && g.emotes.current() == null && !nl.juiced.guhs.feature.knus.GuhHooks.isBezig(g) && !nl.juiced.guhs.feature.huisje.Huisjes.isBinnen(g))
                .stream().min(java.util.Comparator.<GuhEntity>comparingInt(g -> eigen != null && eigen.equals(nl.juiced.guhs.entity.Owners.uuid(g)) ? 0 : 1)
                        .thenComparingDouble(this::distanceToSqr)).orElse(null);
    }

    /** Do it and this guh live in the same Guhhuisje? (Then they hug more often.) */
    private boolean zelfdeHuisje(GuhEntity g) {
        if (!(level() instanceof ServerLevel sl) || nl.juiced.guhs.entity.Owners.uuid(this) == null || nl.juiced.guhs.entity.Owners.uuid(g) == null) {
            return false;
        }
        try {
            var a = nl.juiced.guhs.feature.huisje.Huisjes.vanBewoner(sl.getServer(), nl.juiced.guhs.entity.Owners.uuid(this), nl.juiced.guhs.feature.band.Band.id(this));
            var b = nl.juiced.guhs.feature.huisje.Huisjes.vanBewoner(sl.getServer(), nl.juiced.guhs.entity.Owners.uuid(g), nl.juiced.guhs.feature.band.Band.id(g));
            return a != null && b != null && a.pos().equals(b.pos());
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The hug itself: the guh hugs (KNUFFELEN), the Blub squishes, hearts and a peep. */
    public void knuffelMoment(GuhEntity g) {
        getNavigation().stop();
        g.getNavigation().stop();
        g.getLookControl().setLookAt(this, 90f, 90f);
        getLookControl().setLookAt(g, 90f, 90f);
        g.emotes.start(Emote.KNUFFELEN, false, GuhEmotes.Source.SELF);
        triggerAnim("actie", "squish");
        emotie(RECHTOP, 80);
        BlubGeluid.KNUFFEL_GUH.speel(this);
        if (level() instanceof ServerLevel sl) {
            double mx = (getX() + g.getX()) / 2, mz = (getZ() + g.getZ()) / 2;
            deeltjes(sl, ParticleTypes.HEART, mx, getY() + Math.max(getBbHeight(), g.getBbHeight()) + 0.2, mz, 6, 0.4, 0.2, 0.4, 0);
        }
    }

    /** Walks to the guh (the guh comes a little closer too) and hugs it. */
    class KnuffelGoal extends Goal {
        private int ticks;
        private boolean klaar;

        KnuffelGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (knuffelGuh != null) {
                return true;
            }
            long nu = level().getGameTime();
            if (volgendeKnuffel < 0) {
                volgendeKnuffel = nu + 600 + random.nextInt(2400);
            }
            if (nu < volgendeKnuffel || !isTame() || isBezig() || slaapt() || isPassenger() || liedje > 0) {
                return false;
            }
            GuhEntity g = knuffelPartner();
            if (g == null) {
                volgendeKnuffel = nu + 200;                          // (nobody around: look again in 10 seconds)
                return false;
            }
            // about once every few minutes per pair; housemates of one Guhhuisje a bit more often
            volgendeKnuffel = nu + (zelfdeHuisje(g) ? 1200 + random.nextInt(1800) : 2400 + random.nextInt(3600));
            knuffelGuh = g;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !klaar && knuffelGuh != null && knuffelGuh.isAlive() && ticks < 300 && !isBezig();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ticks = 0;
            klaar = false;
        }

        @Override
        public void tick() {
            GuhEntity g = knuffelGuh;
            if (g == null) {
                return;
            }
            ticks++;
            getLookControl().setLookAt(g);
            double reik = 1.3 + (getBbWidth() + g.getBbWidth()) * 0.5;
            if (distanceToSqr(g) <= reik * reik) {
                knuffelMoment(g);
                klaar = true;
            } else if (ticks % 10 == 1) {
                getNavigation().moveTo(g, 1.2);
                if (!g.isOrderedToSit() && ticks > 20) {
                    g.getNavigation().moveTo(BlubGuhEntity.this, 0.9);   // (the guh comes to meet it halfway)
                }
            }
        }

        @Override
        public void stop() {
            knuffelGuh = null;
            getNavigation().stop();
        }
    }

    /** Slaapliedje piepen: the whole lullaby once (not twice when asked again while it sings); the guhs around yawn and fall asleep. */
    public void slaapliedje(@Nullable ServerPlayer player) {
        if (liedje > 0) {
            return;                                                    // (it is still singing)
        }
        liedje = LIEDJE_TICKS;
        BlubGeluid.LIEDJE.speel(this);
        if (player == null) {
            return;
        }
        liedjeVoor = player;
        player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.liedje", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Verstoppertje: it hides somewhere within {@value #VERSTOP_BEREIK} blocks, out of sight if it can; find it within a minute. */
    public boolean verstoppertje(ServerPlayer player) {
        BlockPos plek = verstopPlek(player);
        if (plek == null) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.verstop.geen_plek", getDisplayName()).withStyle(ChatFormatting.GRAY));
            return false;
        }
        setSlaapt(false);
        zet(nl.juiced.guhs.feature.piep.PiepInstelling.RONDVADSEN, true);
        verstopPlek = plek;
        verstopTot = level().getGameTime() + VERSTOP_TICKS;
        entityData.set(DATA_VERSTOPT, true);
        getNavigation().moveTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 1.6);
        BlubGeluid.VERSTOPPEN.speel(this);
        player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.verstop.start", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    @Nullable
    private BlockPos verstopPlek(ServerPlayer player) {
        BlockPos beste = null;
        for (int i = 0; i < 60; i++) {
            BlockPos kolom = blockPosition().offset(random.nextInt(VERSTOP_BEREIK * 2 + 1) - VERSTOP_BEREIK, 0,
                    random.nextInt(VERSTOP_BEREIK * 2 + 1) - VERSTOP_BEREIK);
            BlockPos p = null;
            for (int dy = 3; dy >= -3 && p == null; dy--) {
                if (kanStaan(kolom.above(dy))) {
                    p = kolom.above(dy);
                }
            }
            if (p == null || p.distSqr(player.blockPosition()) < 16) {
                continue;
            }
            if (getNavigation().createPath(p, 0) == null) {
                continue;
            }
            beste = p;
            Vec3 oog = player.getEyePosition();
            HitResult hit = level().clip(new ClipContext(oog, Vec3.atCenterOf(p), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                return p;                                                  // out of sight: a real hiding place
            }
        }
        return beste;
    }

    private boolean kanStaan(BlockPos p) {
        return level().getBlockState(p).getCollisionShape(level(), p).isEmpty()
                && level().getBlockState(p.above()).getCollisionShape(level(), p.above()).isEmpty()
                && level().getBlockState(p.below()).isFaceSturdy(level(), p.below(), net.minecraft.core.Direction.UP)
                && level().getFluidState(p).isEmpty();
    }

    /** Found! A rain of hearts. */
    private void gevonden(ServerPlayer player) {
        entityData.set(DATA_VERSTOPT, false);
        verstopTot = 0;
        verstopPlek = null;
        zet(nl.juiced.guhs.feature.piep.PiepInstelling.RONDVADSEN, true);
        triggerAnim("actie", "sprong");
        emotie(RECHTOP, 100);
        BlubGeluid.GEVONDEN.speel(this);
        if (level() instanceof ServerLevel sl) {
            deeltjes(sl, ParticleTypes.HEART, getX(), getY() + 1.6, getZ(), 24, 1.6, 0.6, 1.6, 0.02);
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.verstop.gevonden", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Hupsen: a few hops in a ring around its owner, faster and faster (its record is kept). */
    public void hupsen(@Nullable ServerPlayer player) {
        setSlaapt(false);
        if (isTame()) {
            zet(nl.juiced.guhs.feature.piep.PiepInstelling.RONDVADSEN, true);
        }
        hupsenOver = 6 + random.nextInt(5);
        hupsenGedaan = 0;
        hupsWacht = 0;
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.hupsen.start", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // --- every tick -----------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        long nu = sl.getGameTime();
        if (emotie() != RECHTOP && nu >= emotieTot) {
            entityData.set(DATA_EMOTIE, RECHTOP);
        }
        if (slaapt()) {
            if (sl.getGameTime() >= volgendSlaapGeluid) {
                volgendSlaapGeluid = sl.getGameTime() + 70 + random.nextInt(31);   // (every 3.5-5 s one of its sleep sounds)
                BlubGeluid.SLAPEN.speel(this);
                deeltjes(sl, nl.juiced.guhs.feature.bio.bouwwolk2.BouwWolk2Slice.ZZZ.get(), getX(), getY() + getBbHeight() + 0.2, getZ(),
                        0, random.nextInt(3), 0.02, 0, 1);
            }
            if (tickCount % 120 == 60) {                               // a little dream cloud
                deeltjes(sl, ParticleTypes.WHITE_SMOKE, getX() + 0.3, getY() + getBbHeight() + 0.5, getZ(), 4, 0.12, 0.05, 0.12, 0);
            }
        }
        // the slaapliedje
        if (liedje > 0) {
            liedje--;
            if (liedje % 15 == 0) {                                    // (notes over the whole song)
                deeltjes(sl, ParticleTypes.NOTE, getX(), getY() + getBbHeight() + 0.3, getZ(), 0, random.nextFloat(), 0, 0, 1);
            }
            if (liedje % 100 == 50) {                                  // (the guhs around get sleepier while it sings)
                for (GuhEntity guh : sl.getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(10))) {
                    if (random.nextInt(3) == 0) {
                        guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
                    }
                }
            }
            if (liedje == 0) {
                int slapers = 0;
                for (GuhEntity guh : sl.getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(10))) {
                    if (guh.emotes.start(sl.isDarkOutside() || random.nextBoolean() ? Emote.SLAPEN : Emote.GAPEN, false, GuhEmotes.Source.SELF)) {
                        slapers++;
                    }
                }
                if (liedjeVoor != null) {
                    liedjeVoor.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.liedje.klaar", slapers).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                liedjeVoor = null;
            }
        }
        if (juichOp > 0 && nu >= juichOp) {
            juichOp = 0;
            for (GuhEntity guh : sl.getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(8))) {
                guh.emotes.start(Emote.HARTJES, false, GuhEmotes.Source.SELF);
                deeltjes(sl, ParticleTypes.HAPPY_VILLAGER, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 4, 0.3, 0.2, 0.3, 0);
            }
        }
        // verstoppertje: time's up
        if (verstopt() && nu >= verstopTot) {
            entityData.set(DATA_VERSTOPT, false);
            verstopPlek = null;
            BlubGeluid.GEVONDEN.speel(this, 0.85f);                  // (not found: it pops out by itself, a bit lower)
            triggerAnim("actie", "sprong");
            if (getOwner() instanceof ServerPlayer owner) {
                owner.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.verstop.op", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        // bubbles (in the water: its owner nearby breathes longer)
        if (nu >= volgendeBel) {
            volgendeBel = nu + (isInWater() ? 60 : 300 + random.nextInt(400));
            if (isInWater()) {
                deeltjes(sl, ParticleTypes.BUBBLE, getX(), getY() + 0.3, getZ(), 8, 0.25, 0.2, 0.25, 0.05);
                if (isTame() && getOwner() instanceof ServerPlayer owner && owner.isUnderWater() && owner.distanceToSqr(this) < 36) {
                    owner.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, true, true, true));
                }
            } else if (!slaapt()) {
                Vec3 voor = Vec3.directionFromRotation(0, getYRot()).scale(0.5);
                deeltjes(sl, ParticleTypes.BUBBLE_POP, getX() + voor.x, getY() + getBbHeight() * 0.5, getZ() + voor.z, 6, 0.15, 0.15, 0.15, 0.02);
                BlubGeluid.BELLEN.speel(this);
            }
        }
        // puddles: in the rain it jumps about and shakes itself dry
        if (nu >= volgendePlas && onGround() && !slaapt() && nl.juiced.guhs.feature.weer.WeerZones.regentOp(sl, blockPosition().above())) {
            volgendePlas = nu + 40 + random.nextInt(60);
            setDeltaMovement(getDeltaMovement().add(0, 0.42, 0));
            needsSync = true;
            deeltjes(sl, ParticleTypes.SPLASH, getX(), getY() + 0.05, getZ(), 14, 0.3, 0.05, 0.3, 0.1);
            BlubGeluid.PLAS.speel(this);
            if (random.nextInt(3) == 0) {
                triggerAnim("actie", "uitschudden");
                BlubGeluid.UITSCHUDDEN.speel(this);
                deeltjes(sl, ParticleTypes.FALLING_WATER, getX(), getY() + getBbHeight() * 0.7, getZ(), 10, 0.3, 0.2, 0.3, 0);
            }
        }
        // guhs nearby are happy to see it
        if (nu >= volgendeBlij) {
            volgendeBlij = nu + 200 + random.nextInt(200);
            for (GuhEntity guh : sl.getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(5))) {
                if (random.nextInt(3) == 0) {
                    deeltjes(sl, ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.2, 0.1, 0.2, 0);
                }
            }
        }
        // something new nearby: a curious curl in its ears
        if (nu >= volgendeKrul && emotie() == RECHTOP && !slaapt()) {
            volgendeKrul = nu + 80 + random.nextInt(120);
            boolean nieuw = !sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, getBoundingBox().inflate(4)).isEmpty()
                    || !sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(4), p -> !isOwnedBy(p) && !p.isSpectator()).isEmpty();
            if (nieuw && random.nextInt(2) == 0) {
                emotie(KRUL, 60);
                BlubGeluid.NIEUWSGIERIG.speel(this);
            }
        }
        // now and then a trick of its own (about once a minute; wild and tame)
        if (volgendeTruc < 0) {
            volgendeTruc = nu + 600 + random.nextInt(1200);
        } else if (nu >= volgendeTruc) {
            volgendeTruc = nu + 600 + random.nextInt(1200);
            zelfTruc();
        }
        // a Pure Blub and a Volle Guh glitter a little
        if (titel() != null && nu >= volgendeGlinster) {
            volgendeGlinster = nu + 30 + random.nextInt(30);
            deeltjes(sl, ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.8, getZ(), 1, 0.25, 0.2, 0.25, 0.005);
            if (random.nextInt(3) == 0) {
                BlubGeluid.GLINSTER.speel(this);
            }
        }
        // the trick "dood spelen": up again
        if (weerOp > 0 && nu >= weerOp) {
            weerOp = 0;
            BlubGeluid.TRUC_OP.speel(this);
        }
        // hidden: now and then a tiny peep (a hint)
        if (verstopt() && tickCount % 120 == 60) {
            BlubGeluid.VERSTOPT_PIEP.speel(this);
        }
        // landing after a jump
        if (onGround()) {
            if (inDeLucht > 6) {
                BlubGeluid.LANDEN.speel(this);
            }
            inDeLucht = 0;
        } else if (!isInWater() && !isPassenger()) {
            inDeLucht++;
        }
    }

    // --- goals --------------------------------------------------------------------------------------------------------------------------

    /** At night: to a guh nearby (or a guhnestje) and asleep, zzz. Wakes up in the morning, or when you squish it. */
    class SlaapGoal extends Goal {
        @Nullable
        private Vec3 doel;
        private long volgende;
        private int ticks;

        SlaapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            long nu = level().getGameTime();
            if (nu < volgende || !level().isDarkOutside() || isBezig() || isPassenger() || isInWater()) {
                return false;
            }
            volgende = nu + 100;
            if (isTame() && aan(nl.juiced.guhs.feature.piep.PiepInstelling.VOLGEN) && getOwner() != null && getOwner().distanceToSqr(BlubGuhEntity.this) > 64) {
                return false;                                               // (following its owner far away: no nap)
            }
            doel = slaapPlek();
            return true;
        }

        @Nullable
        private Vec3 slaapPlek() {
            GuhEntity guh = level().getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(10)).stream()
                    .min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b))).orElse(null);
            if (guh != null) {
                return guh.position().add(0.6, 0, 0.3);
            }
            BlockPos hier = blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(hier.offset(-8, -2, -8), hier.offset(8, 2, 8))) {
                if (level().getBlockState(p).is(VadswoudFeature.GUHNESTJE.get())) {
                    return Vec3.atBottomCenterOf(p);
                }
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return level().isDarkOutside() && !isBezig() && !isPassenger();
        }

        @Override
        public void start() {
            ticks = 0;
            if (doel != null && distanceToSqr(doel) > 1.5) {
                getNavigation().moveTo(doel.x, doel.y, doel.z, 1.0);
            } else {
                setSlaapt(true);
            }
        }

        @Override
        public void tick() {
            ticks++;
            if (!slaapt()) {
                if (doel == null || distanceToSqr(doel) < 1.5 || ticks > 300 || getNavigation().isDone()) {
                    getNavigation().stop();
                    setSlaapt(true);
                }
            } else {
                getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            setSlaapt(false);
            doel = null;
        }
    }

    /** Tikkertje: run to a guh nearby, tap it (a heart), and run off; the guh is happy. */
    class TikkertjeGoal extends Goal {
        @Nullable
        private GuhEntity guh;
        private long volgende;
        private int ticks;
        private boolean getikt;

        TikkertjeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            long nu = level().getGameTime();
            if (nu < volgende || isBezig() || slaapt() || isOrderedToSit() || isPassenger()) {
                return false;
            }
            volgende = nu + 200 + random.nextInt(400);
            guh = level().getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(8)).stream().findAny().orElse(null);
            return guh != null && random.nextInt(2) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return guh != null && guh.isAlive() && ticks < 200 && !isBezig() && !slaapt();
        }

        @Override
        public void start() {
            ticks = 0;
            getikt = false;
            getNavigation().moveTo(guh, 1.35);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            if (guh == null) {
                return;
            }
            if (!getikt) {
                if (distanceToSqr(guh) < 2.0) {
                    getikt = true;
                    BlubGeluid.TIKKERTJE.speel(BlubGuhEntity.this);
                    if (level() instanceof ServerLevel sl) {
                        deeltjes(sl, ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 2, 0.2, 0.1, 0.2, 0);
                    }
                    Vec3 weg = position().subtract(guh.position()).normalize().scale(6).add(position());
                    getNavigation().moveTo(weg.x, weg.y, weg.z, 1.45);
                } else if (ticks % 10 == 0) {
                    getNavigation().moveTo(guh, 1.35);
                }
            } else if (getNavigation().isDone()) {
                ticks = 999;
            }
        }

        @Override
        public void stop() {
            guh = null;
        }
    }

    /** Now and then it brings a knabbel to a guh nearby: they nibble together, the guh gives a heart back. */
    class DeelGoal extends Goal {
        @Nullable
        private GuhEntity guh;
        private long volgende;
        private int ticks;

        DeelGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            long nu = level().getGameTime();
            if (nu < volgende || isBezig() || slaapt() || isOrderedToSit() || isPassenger()) {
                return false;
            }
            volgende = nu + 600 + random.nextInt(1200);
            guh = level().getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(8)).stream().findAny().orElse(null);
            return guh != null;
        }

        @Override
        public boolean canContinueToUse() {
            return guh != null && guh.isAlive() && ticks < 160;
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().moveTo(guh, 1.0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            getLookControl().setLookAt(guh);
            if (distanceToSqr(guh) < 2.2) {
                deelEffect();
                guh.emotes.start(Emote.SMAKKEN, false, GuhEmotes.Source.SELF);
                if (level() instanceof ServerLevel sl) {
                    deeltjes(sl, ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.1, 0.1, 0.1, 0);
                    deeltjes(sl, ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 1, 0.1, 0.1, 0.1, 0);
                }
                ticks = 999;
            } else if (ticks % 15 == 0) {
                getNavigation().moveTo(guh, 1.0);
            }
        }

        @Override
        public void stop() {
            guh = null;
        }
    }

    /** Verstoppertje: walk to the hiding place, then sit very still. */
    class VerstopGoal extends Goal {
        VerstopGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return verstopt();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (verstopPlek != null && distanceToSqr(Vec3.atBottomCenterOf(verstopPlek)) > 1.2) {
                if (getNavigation().isDone()) {
                    getNavigation().moveTo(verstopPlek.getX() + 0.5, verstopPlek.getY(), verstopPlek.getZ() + 0.5, 1.6);
                }
            } else {
                getNavigation().stop();
            }
        }
    }

    /** Hupsen: hop after hop in a ring around its owner, a little faster each time. */
    class HupsGoal extends Goal {
        HupsGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return hupsenOver > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (hupsWacht > 0) {
                hupsWacht--;
                return;
            }
            if (!onGround()) {
                return;
            }
            net.minecraft.world.entity.LivingEntity owner = getOwner();
            Vec3 midden = owner != null && owner.distanceToSqr(BlubGuhEntity.this) < 100 ? owner.position() : position();
            double hoek = Math.atan2(getZ() - midden.z, getX() - midden.x) + 0.7;
            Vec3 doel = midden.add(Math.cos(hoek) * 2.2, 0, Math.sin(hoek) * 2.2);
            Vec3 stap = doel.subtract(position()).multiply(1, 0, 1);
            double snel = 0.18 + hupsenGedaan * 0.02;
            stap = stap.lengthSqr() > 0.0001 ? stap.normalize().scale(snel) : Vec3.ZERO;
            setDeltaMovement(stap.x, 0.42, stap.z);
            setYRot((float) (Mth.atan2(stap.z, stap.x) * (180 / Math.PI)) - 90f);
            yBodyRot = getYRot();
            needsSync = true;
            BlubGeluid.HUPSEN.speel(BlubGuhEntity.this, 1f + hupsenGedaan * 0.06f);
            hupsenGedaan++;
            hupsenOver--;
            hupsWacht = Math.max(4, 12 - hupsenGedaan);
            if (hupsenOver == 0) {
                boolean record = hupsenGedaan > hupsRecord;
                hupsRecord = Math.max(hupsRecord, hupsenGedaan);
                if (owner instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.translatable(record ? "gui.guhs.blubguh.hupsen.record" : "gui.guhs.blubguh.hupsen.klaar",
                            getDisplayName(), hupsenGedaan, hupsRecord).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                if (level() instanceof ServerLevel sl) {
                    deeltjes(sl, ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), record ? 8 : 3, 0.4, 0.2, 0.4, 0);
                }
            }
        }
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------------------

    @Override
    protected RawAnimation beweging(com.geckolib.animation.state.AnimationTest<Landdiertje> state) {
        if (slaapt()) {
            return SLAAP;
        }
        if (verstopt() && !state.isMoving()) {
            return ZIT;
        }
        return super.beweging(state);
    }

    @Override
    protected void extraActies(AnimationController<Landdiertje> actie) {
        for (String a : List.of("squish", "rondje", "dood", "sprong", "uitschudden")) {
            actie.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
        }
    }

    /** Hidden: no name tag giving it away. */
    @Override
    protected SoundEvent getAmbientSound() {
        return verstopt() || slaapt() ? null : geluid();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BlubGeluid.SCHRIK.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BlubGeluid.WEG.get();
    }

    // --- who may hear and see a wild Blub (only who helped Snoes; tamed ones and the dream: everyone) ------------------------------

    /** Everyone may see and hear it: a tamed Blub, or any Blub in the dream. A wild one elsewhere: only who helped Snoes. */
    public boolean voorIedereen() {
        return isTame() || BlubSnoes.inDroom(level());
    }

    /** Its sounds (squeaks, steps, the lullaby...): a wild one only for the players who may see it (the others hear nothing). */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (isSilent() || !(level() instanceof ServerLevel sl) || voorIedereen()) {
            super.playSound(sound, volume, pitch);
            return;
        }
        net.minecraft.core.Holder<SoundEvent> holder = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        double bereik = Math.max(16.0, 16.0 * volume);
        long seed = sl.getRandom().nextLong();
        for (ServerPlayer p : sl.players()) {
            if (BlubSnoes.heeft(p) && p.distanceToSqr(this) < bereik * bereik) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(holder, getSoundSource(), getX(), getY(), getZ(),
                        volume, pitch, seed));
            }
        }
    }

    /** sendParticles, but a wild Blub's only to the players who may see it. */
    <T extends net.minecraft.core.particles.ParticleOptions> void deeltjes(ServerLevel sl, T soort, double x, double y, double z, int n,
                                                                          double dx, double dy, double dz, double snelheid) {
        if (voorIedereen()) {
            sl.sendParticles(soort, x, y, z, n, dx, dy, dz, snelheid);
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (BlubSnoes.heeft(p)) {
                sl.sendParticles(p, soort, false, false, x, y, z, n, dx, dy, dz, snelheid);
            }
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        BlubGeluid.HUPPELEN.speel(this);
    }

    /** Eating (a snack, taming, falling in love): its own sound, crumbs. */
    @Override
    protected void eetEffect(ItemStack stack) {
        triggerAnim("actie", "eet");
        BlubGeluid.ETEN.speel(this);
        if (level() instanceof ServerLevel sl && !stack.isEmpty()) {
            deeltjes(sl, new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1).getItem()),
                    getX(), getY() + getBbHeight() * 0.6, getZ(), 6, 0.12, 0.05, 0.12, 0.03);
        }
    }

    /** Sharing a knabbel with a guh. */
    private void deelEffect() {
        triggerAnim("actie", "eet");
        BlubGeluid.DELEN.speel(this);
        if (level() instanceof ServerLevel sl) {
            deeltjes(sl, new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, ModItems.KAAS_KNABBELS.get()),
                    getX(), getY() + getBbHeight() * 0.6, getZ(), 6, 0.12, 0.05, 0.12, 0.03);
        }
    }

    /** Petting: hearts, a happy hop and its happy squeak (Landdiertje's, with its own sound). */
    @Override
    public void aaien(ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        deeltjes(level, ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 2, 0.2, 0.1, 0.2, 0);
        if (!isBezig()) {
            triggerAnim("actie", "blij");
        }
        BlubGeluid.BLIJ.speel(this);
        nl.juiced.guhs.feature.gids.GidsFeature.grant(player, "diertjes/landdiertjes_geaaid");
    }

    @Override
    public void temmen(ServerPlayer player) {
        super.temmen(player);
        BlubGeluid.GETEMD.speel(this);
        nl.juiced.guhs.feature.dexdromen.BlubMeterkaart.vul(player, this);   // (1.8.0: a box on the meterkaart)
    }

    /** Puts on a piece from its Kledingkast (with a little sound; null takes it off). */
    public void kleed(BlubKleding.Plek plek, @Nullable BlubKleding.Stuk stuk) {
        kleedAan(plek, stuk);
        if (stuk != null) {
            BlubGeluid.STRIKJE.speel(this);
            emotie(RECHTOP, 60);
        }
    }

    // --- save ---------------------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Meter", meter());
        tag.putString("BlubSoort", blubSoort().id());
        tag.putInt("Tint", tint());
        tag.putFloat("Grootte", grootte());
        net.minecraft.nbt.CompoundTag kleren = new net.minecraft.nbt.CompoundTag();
        kleding().forEach(kleren::putString);
        tag.store("Kleding", net.minecraft.nbt.CompoundTag.CODEC, kleren);
        tag.putBoolean("VanSchouder", vanSchouder);
        tag.putInt("HupsRecord", hupsRecord);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getString("BlubSoort").isPresent()) {
            zetWaarden(new BlubTrekking.Waarden(tag.getIntOr("Meter", BlubTrekking.METER_MIDDEN), BlubTrekking.Soort.van(tag.getStringOr("BlubSoort", "")),
                    tag.getIntOr("Tint", 0), tag.getFloatOr("Grootte", 1f)));
        }
        net.minecraft.nbt.CompoundTag kleren = tag.read("Kleding", net.minecraft.nbt.CompoundTag.CODEC).orElseGet(net.minecraft.nbt.CompoundTag::new);
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        for (String k : kleren.keySet()) {
            if (BlubKleding.stuk(kleren.getStringOr(k, "")) != null) {
                m.put(k, kleren.getStringOr(k, ""));
            }
        }
        zetKleding(m);
        vanSchouder = tag.getBooleanOr("VanSchouder", false);
        hupsRecord = tag.getIntOr("HupsRecord", 0);
    }
}
