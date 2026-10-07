package nl.juiced.guhs.feature.einde2.appa;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.taal.Tekst;

/**
 * einde2, slice appa: a player's own Momo-guh (one per player).
 * <p>
 * Momo lives on his owner's RIGHT shoulder (the left one is the piep maatje's): then there is no entity, only the player's
 * saved data ({@value #KEY} in {@code GuhQuests.saved}: Heeft, Plek, Gen, Naam, and his knabbel clock) and a layer on the
 * player ({@link E2AppaPayloads.MomoSchouder}). On the shoulder he picks up what lies within {@value #RAAP_STRAAL} blocks
 * into his owner's inventory ({@link #spelerTick}) and about once in twenty minutes he takes ONE kaasknabbel out of the bag,
 * says something about it, and gives it back a moment later (it is never lost: what he owes is saved too).
 * He can be told to sit at home ({@link #zetThuis}: a {@link MomoGuhEntity} that stays where it is put); called back
 * ({@link #roep}) that entity flutters to its owner and hops on. As with the Appa-guh a number ("Gen") makes an older
 * copy that turns up later step aside.
 * <p>
 * hook fase B: the story gives him ({@link #geef}); nothing in phase A does in survival.
 */
public final class Momo {
    public static final String KEY = "guhs_einde2_appa_momo";
    /** Where he is: on the shoulder (no entity), sitting at home, or on his way to the shoulder. */
    public static final String SCHOUDER = "schouder", THUIS = "thuis", LOS = "los";
    public static final double RAAP_STRAAL = 6.0;
    /** Ticks on the shoulder between two borrowed knabbels: 15 to 25 minutes. */
    public static final int JAT_MIN = 20 * 60 * 15, JAT_MAX = 20 * 60 * 25;
    /** Ticks until the knabbel comes back. */
    public static final int TERUG = 60;
    /** How many different lines he has for a borrowed knabbel (gui.guhs.einde2_appa.momo.jat.N). */
    public static final int JAT_ZINNEN = 5;
    /** On a dropped item: whose death dropped it (a UUID string) and when (game time): not Momo's to take for 5 seconds. */
    public static final String DOOD_VAN = "guhs_einde2_appa_dood_van", DOOD_TIJD = "guhs_einde2_appa_dood_tijd";
    /** How often {@link #spelerTick} is called. */
    public static final int STAP = 10;

    /** Server: owner -> their Momo in the world (at home or on his way). */
    private static final Map<UUID, MomoGuhEntity> GELADEN = new ConcurrentHashMap<>();

    // --- the saved state --------------------------------------------------------------------------------------------------------

    private static CompoundTag staat(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        if (!saved.contains(KEY)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(KEY);
    }

    public static boolean heeft(ServerPlayer p) {
        return GuhQuests.saved(p).getCompoundOrEmpty(KEY).getBooleanOr("Heeft", false);
    }

    /** {@link #SCHOUDER}, {@link #THUIS} or {@link #LOS} ("" when they have none). */
    public static String plek(ServerPlayer p) {
        return heeft(p) ? GuhQuests.saved(p).getCompoundOrEmpty(KEY).getStringOr("Plek", SCHOUDER) : "";
    }

    public static boolean opSchouder(ServerPlayer p) {
        return SCHOUDER.equals(plek(p));
    }

    static int gen(ServerPlayer p) {
        return GuhQuests.saved(p).getCompoundOrEmpty(KEY).getIntOr("Gen", 0);
    }

    /** How he is called in a line: his own name, else "Momo-guh". */
    public static Component naam(ServerPlayer p) {
        return Tekst.read(staat(p), "Naam").orElseGet(() -> Component.translatable("entity.guhs.momo_guh"));
    }

    // --- Momo in the world ------------------------------------------------------------------------------------------------------

    /** As {@link Appa#meld}: an entity that is not the owner's current Momo in the world removes itself. */
    static void meld(MomoGuhEntity momo) {
        UUID eigenaar = momo.eigenaar();
        if (eigenaar == null || momo.isRemoved()) {
            return;
        }
        ServerPlayer baas = momo.baas();
        if (baas == null) {
            return;
        }
        if (!heeft(baas) || opSchouder(baas) || momo.gen() != gen(baas)) {
            momo.discard();
            return;
        }
        MomoGuhEntity oud = GELADEN.put(eigenaar, momo);
        if (oud != null && oud != momo && !oud.isRemoved()) {
            oud.discard();
        }
        CompoundTag staat = staat(baas);
        if (momo.hasCustomName() && !momo.getCustomName().equals(Tekst.read(staat, "Naam").orElse(null))) {
            Tekst.put(staat, "Naam", momo.getCustomName());
        }
    }

    static void vergeet(MomoGuhEntity momo) {
        UUID eigenaar = momo.eigenaar();
        if (eigenaar != null) {
            GELADEN.remove(eigenaar, momo);
        }
    }

    static void leeg() {
        GELADEN.clear();
    }

    /** This player's Momo in the world when it is loaded, or null (on the shoulder, or in an unloaded chunk). */
    @Nullable
    public static MomoGuhEntity geladen(ServerPlayer p) {
        MomoGuhEntity momo = GELADEN.get(p.getUUID());
        return momo == null || momo.isRemoved() || !momo.isAlive() || momo.gen() != gen(p) || !momo.isEigenaar(p) ? null : momo;
    }

    /** Makes the player's Momo in the world at this spot (a new number: any older copy is an old one from now on). */
    @Nullable
    private static MomoGuhEntity maak(ServerPlayer p, Vec3 at, boolean thuis) {
        MomoGuhEntity oud = geladen(p);
        if (oud != null) {
            oud.discard();
        }
        CompoundTag staat = staat(p);
        staat.putInt("Gen", gen(p) + 1);
        staat.putString("Plek", thuis ? THUIS : LOS);
        MomoGuhEntity momo = E2AppaFeature.MOMO_GUH.get().create(p.level(), EntitySpawnReason.TRIGGERED);
        if (momo == null) {
            staat.putString("Plek", SCHOUDER);
            return null;
        }
        momo.zetEigenaar(p.getUUID(), gen(p));
        momo.setThuis(thuis);
        Tekst.read(staat, "Naam").ifPresent(momo::setCustomName);
        momo.snapTo(at.x, at.y, at.z, p.getYRot() + 180f, 0);
        p.level().addFreshEntity(momo);
        GELADEN.put(p.getUUID(), momo);
        E2AppaPayloads.sync(p);
        return momo;
    }

    /** A spot next to the player's right shoulder, where he hops off to. */
    private static Vec3 naast(ServerPlayer p) {
        return p.position().add(new Vec3(-0.9, 1.0, 0.6).yRot(-p.getYRot() * Mth.DEG_TO_RAD));
    }

    /** He hops onto the shoulder (the entity, if there is one, leaves the world). */
    static void opSchouder(ServerPlayer p, @Nullable MomoGuhEntity momo) {
        if (!heeft(p)) {
            return;
        }
        if (momo == null) {
            momo = geladen(p);
        }
        if (momo != null) {
            momo.discard();
        }
        staat(p).putString("Plek", SCHOUDER);
        p.level().playSound(null, p.blockPosition(), E2AppaFeature.MOMO.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.momo.schouder", naam(p)).withStyle(ChatFormatting.LIGHT_PURPLE));
        E2AppaPayloads.sync(p);
    }

    // --- the API ----------------------------------------------------------------------------------------------------------------

    /**
     * Gives this player their Momo-guh, once (with his Guhdex page). Returns Momo in the world, next to the player and on
     * his way to their shoulder: a new owner sees him come, and for one who had him already he hops off and on again (or
     * comes from home). Call {@link #zetThuis} right after it to keep him in the world.
     */
    @Nullable
    public static MomoGuhEntity geef(ServerPlayer p) {
        if (!heeft(p)) {
            CompoundTag staat = staat(p);
            staat.putBoolean("Heeft", true);
            staat.putString("Plek", SCHOUDER);
            staat.putInt("Jat", wachttijd(p));
            GuhDex.zie(p, GuhVariant.MOMO_GUH);
        }
        if (opSchouder(p)) {
            geefTerug(p);
            return maak(p, naast(p), false);
        }
        return roep(p);
    }

    /**
     * Calls Momo from home (or from wherever he is on his way) to the player's shoulder: returns the entity that flutters
     * to them. Null: they have none, or he sits on the shoulder already.
     */
    @Nullable
    public static MomoGuhEntity roep(ServerPlayer p) {
        if (!heeft(p) || opSchouder(p)) {
            return null;
        }
        MomoGuhEntity momo = geladen(p);
        if (momo != null && momo.level() == p.level() && momo.distanceToSqr(p) < 40 * 40) {
            staat(p).putString("Plek", LOS);
            momo.setThuis(false);
            return momo;
        }
        return maak(p, naast(p), false);
    }

    /**
     * Tells Momo to stay at home or to come along again. Home is where he is: from the shoulder he hops down next to the
     * player (or onto {@code plek} with {@link #zetThuis(ServerPlayer, Vec3)}).
     */
    public static void zetThuis(ServerPlayer p, boolean thuis) {
        if (!heeft(p)) {
            return;
        }
        if (!thuis) {
            if (THUIS.equals(plek(p))) {
                roep(p);
            }
            return;
        }
        if (opSchouder(p)) {
            zetThuis(p, grond(p, naast(p)));
            return;
        }
        MomoGuhEntity momo = geladen(p);
        if (momo != null) {
            staat(p).putString("Plek", THUIS);
            momo.setThuis(true);
        } else {
            zetThuis(p, grond(p, naast(p)));
        }
    }

    /** Momo sits at home on this spot from now on. */
    public static void zetThuis(ServerPlayer p, Vec3 plek) {
        if (!heeft(p)) {
            return;
        }
        geefTerug(p);
        if (maak(p, plek, true) != null) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.momo.thuis", naam(p)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The spot under {@code at} he can sit on (at most 4 blocks down; else {@code at} itself). */
    private static Vec3 grond(ServerPlayer p, Vec3 at) {
        ServerLevel level = p.level();
        var pos = net.minecraft.core.BlockPos.containing(at);
        for (int i = 0; i < 5; i++) {
            if (!level.getBlockState(pos.below(i + 1)).getCollisionShape(level, pos.below(i + 1)).isEmpty()) {
                return new Vec3(at.x, pos.getY() - i, at.z);
            }
        }
        return at;
    }

    // --- on the shoulder: picking up, and the knabbel -----------------------------------------------------------------------------

    private static int wachttijd(ServerPlayer p) {
        return JAT_MIN + p.getRandom().nextInt(JAT_MAX - JAT_MIN);
    }

    /** Every {@value #STAP} ticks for every player (E2AppaEvents). */
    static void spelerTick(ServerPlayer p) {
        if (!opSchouder(p) || !p.isAlive() || p.isSpectator()) {
            return;
        }
        CompoundTag staat = staat(p);
        if (staat.getIntOr("Geleend", 0) > 0) {
            int terug = staat.getIntOr("Terug", 0) - STAP;
            if (terug <= 0) {
                geefTerug(p);
            } else {
                staat.putInt("Terug", terug);
            }
        } else {
            int jat = staat.getIntOr("Jat", JAT_MIN) - STAP;
            if (jat <= 0) {
                jat = jat(p) ? wachttijd(p) : 20 * 60;       // (no knabbel in the bag: he looks again in a minute)
            }
            staat.putInt("Jat", jat);
        }
        raap(p);
    }

    /**
     * He takes ONE kaasknabbel out of the bag and says so; {@value #TERUG} ticks later it is back ({@link #geefTerug}).
     * False: there was no knabbel to take.
     */
    static boolean jat(ServerPlayer p) {
        Item knabbel = ModItems.KAAS_KNABBELS.get();
        if (!opSchouder(p) || GuhQuests.count(p, knabbel) < 1) {
            return false;
        }
        GuhQuests.take(p, knabbel, 1);
        CompoundTag staat = staat(p);
        staat.putInt("Geleend", staat.getIntOr("Geleend", 0) + 1);
        staat.putInt("Terug", TERUG);
        p.level().playSound(null, p.blockPosition(), E2AppaFeature.MOMO.get(), SoundSource.NEUTRAL, 0.9f, 1.5f);
        p.sendSystemMessage(Component.translatable("gui.guhs.einde2_appa.momo.jat." + (1 + p.getRandom().nextInt(JAT_ZINNEN)), naam(p))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Every knabbel he still has goes back to the player (into the bag, or at their feet when it is full). */
    static void geefTerug(ServerPlayer p) {
        CompoundTag staat = staat(p);
        int geleend = staat.getIntOr("Geleend", 0);
        if (geleend <= 0) {
            return;
        }
        staat.remove("Geleend");
        staat.remove("Terug");
        Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), geleend));
        p.sendSystemMessage(Component.translatable("gui.guhs.einde2_appa.momo.terug").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** How many knabbels he still has to give back (tests). */
    static int geleend(ServerPlayer p) {
        return GuhQuests.saved(p).getCompoundOrEmpty(KEY).getIntOr("Geleend", 0);
    }

    /**
     * Picks up what lies around: at most four item entities a time. Not what its owner threw away themselves, not what
     * can't be picked up yet, not what another player's death dropped in the last 5 seconds, and nothing when it doesn't
     * fit (then he points at it now and then).
     */
    static void raap(ServerPlayer p) {
        ServerLevel level = p.level();
        int n = 0;
        boolean vol = false;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(RAAP_STRAAL),
                e -> e.isAlive() && !e.hasPickUpDelay() && !e.getItem().isEmpty())) {
            if (n >= 4 || !magRapen(p, item)) {
                continue;
            }
            if (!past(p, item.getItem())) {
                vol = true;
                continue;
            }
            item.playerTouch(p);                    // (the vanilla pick-up: events, stats, the item flies to the player)
            n++;
        }
        if (n > 0 && p.getRandom().nextInt(3) == 0) {
            level.playSound(null, p.blockPosition(), E2AppaFeature.MOMO.get(), SoundSource.NEUTRAL, 0.4f, 1.3f + p.getRandom().nextFloat() * 0.3f);
        }
        CompoundTag staat = staat(p);
        long nu = level.getGameTime();
        if (vol && n == 0 && nu - staat.getLongOr("VolGezegd", -10000L) > 20 * 60) {
            staat.putLong("VolGezegd", nu);
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.momo.vol", naam(p)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    static boolean magRapen(ServerPlayer p, ItemEntity item) {
        if (item.getOwner() == p) {
            return false;                           // (you threw it away yourself: he leaves it)
        }
        CompoundTag data = item.getPersistentData();
        String van = data.getStringOr(DOOD_VAN, "");
        return van.isEmpty() || van.equals(p.getUUID().toString()) || p.level().getGameTime() - data.getLongOr(DOOD_TIJD, 0L) >= 100;
    }

    private static boolean past(ServerPlayer p, ItemStack stack) {
        return p.getInventory().getFreeSlot() >= 0 || p.getInventory().getSlotWithRemainingSpace(stack) >= 0;
    }

    private Momo() {
    }
}
