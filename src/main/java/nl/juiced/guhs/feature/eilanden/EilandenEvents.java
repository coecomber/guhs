package nl.juiced.guhs.feature.eilanden;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * Everything that happens on and around the floating guh islands:
 * <ul>
 *     <li>the Wolkguh only lets itself be tamed once it's been fed {@link #KNABBELS_NEEDED} kaas knabbels (then the
 *     usual chance per knabbel); it floats down like a cloud and drifts back home when it wanders off its island;
 *     while it's wild nothing can hurt it (so nobody can take its outfit without taming it), and it stays one of a
 *     kind: a baby of a tamed Wolkguh is a snow guh;</li>
 *     <li>your own tamed Wolkguh catches you: no fall damage while it's near;</li>
 *     <li>the clouds catch anyone falling off the islands, or off the top of any wolkenlift (slow falling), and the
 *     wolkenlift says VAHOEG!</li>
 * </ul>
 */
public final class EilandenEvents {
    /** Kaas knabbels the Wolkguh has to eat before it can be tamed at all (a normal guh: from the first one). */
    public static final int KNABBELS_NEEDED = 8;
    /** How far your tamed Wolkguh can be to catch you. */
    public static final double CATCH_RANGE = 16;
    /** A wild Wolkguh further than this from home (or this far below it) drifts back. */
    public static final int HOME_RANGE = 20, HOME_DROP = 5;
    public static final double FLOAT_SPEED = 0.12;
    /** For this many ticks after riding a wolkenlift, the clouds still catch you (a lift of your own drops you anywhere). */
    public static final int LIFT_GRACE = 100;
    /** 1.2.7: the knabbels are counted per player (FED_PER: player UUID -> knabbels; FED: the count of before 1.2.7, for everyone). */
    static final String FED_PER = "guhs_wolk_knabbels_per";
    /** Player data (GuhQuests.saved): the Wolkguh's outfit was handed out (once per player). */
    public static final String PAKJE = "guhs_wolk_pakje";
    /** Where the Wolkguh stands in the islands' template (tools/features/eilanden.py), and how big it is. */
    public static final BlockPos WOLK_PLEK = new BlockPos(50, 63, 74);
    public static final float WOLK_SCHAAL = 1.25f;
    public static final String SOORT = "wolkguh";
    /** 1.2.7: islands without a wild Wolkguh (somebody tamed it) get a new one after this long (one Minecraft day). */
    public static final long NIEUWE_WOLK_NA = 24000L;
    static final String FED = "guhs_wolk_knabbels", HOME = "guhs_wolk_home", LIFT_MSG = "guhs_eilanden_lift", CAUGHT = "guhs_eilanden_caught";

    public static boolean isWolk(Object entity) {
        return entity instanceof GuhEntity guh && guh.getVariant() == GuhVariant.WOLK;
    }

    /** The knabbels of the player who fed this Wolkguh the most (plus what it ate before 1.2.7). */
    public static int knabbelsFed(GuhEntity guh) {
        CompoundTag per = guh.getPersistentData().getCompoundOrEmpty(FED_PER);
        int best = 0;
        for (String key : per.keySet()) {
            best = Math.max(best, per.getIntOr(key, 0));
        }
        return guh.getPersistentData().getIntOr(FED, 0) + best;
    }

    /** The knabbels this player fed this Wolkguh (1.2.7: everybody earns its trust themselves). */
    public static int knabbelsFed(GuhEntity guh, java.util.UUID player) {
        return guh.getPersistentData().getIntOr(FED, 0) + guh.getPersistentData().getCompoundOrEmpty(FED_PER).getIntOr(player.toString(), 0);
    }

    // --- taming the Wolkguh: patience (and a lot of knabbels) ---------------------------------------------------------

    /** Counts the knabbels a wild Wolkguh eats (this runs just before the guh itself eats it and rolls for taming). */
    @SubscribeEvent
    public static void onFeed(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof GuhEntity guh) || !isWolk(guh) || guh.isTame()
                || guh.getHiddenBy() != null || !event.getItemStack().is(ModItems.KAAS_KNABBELS.get())
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int fed = knabbelsFed(guh, player.getUUID()) + 1;
        CompoundTag per = guh.getPersistentData().getCompoundOrEmpty(FED_PER);
        per.putInt(player.getUUID().toString(), per.getIntOr(player.getUUID().toString(), 0) + 1);
        guh.getPersistentData().put(FED_PER, per);
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.CLOUD, guh.getX(), guh.getY() + guh.getBbHeight() * 0.6, guh.getZ(), 6, 0.3, 0.2, 0.3, 0.01);
        if (fed < KNABBELS_NEEDED) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.eilanden.feed", KNABBELS_NEEDED - fed)
                    .withStyle(ChatFormatting.AQUA));
        } else {
            if (fed == KNABBELS_NEEDED) {
                player.sendOverlayMessage(Component.translatable("quest.guhs.eilanden.fed").withStyle(ChatFormatting.LIGHT_PURPLE));
                level.playSound(null, guh.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1f, 1.4f);
            }
            GuhAdvancements.grant(player, "eilanden_gevoerd");
        }
    }

    /** Not vads enough yet: the Wolkguh doesn't trust you (the knabbel is still eaten). */
    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (!isWolk(event.getAnimal())) {
            return;
        }
        GuhEntity guh = (GuhEntity) event.getAnimal();
        if (event.getTamer() == null || knabbelsFed(guh, event.getTamer().getUUID()) < KNABBELS_NEEDED) {
            event.setCanceled(true);
        } else if (event.getTamer() instanceof ServerPlayer player) {
            CompoundTag data = guh.getPersistentData();
            if (data.contains(HOME) && player.level() instanceof ServerLevel thuis) {
                // (the next one comes a day after this one left, not a day after it was last looked at)
                nl.juiced.guhs.world.Terugkeer.vertrokken(thuis, SOORT, BlockPos.of(data.getLongOr(HOME, 0L)));
            }
            data.remove(HOME);
            data.remove(FED_PER);
            wolkPakje(player);
            player.sendSystemMessage(Component.translatable("quest.guhs.eilanden.tamed").withStyle(ChatFormatting.AQUA));
            player.level().sendParticles(ParticleTypes.CLOUD, guh.getX(), guh.getY() + 0.5, guh.getZ(), 30, 0.8, 0.5, 0.8, 0.02);
        }
    }

    /**
     * 1.2.7: the Wolkguh's own outfit (wolkenmuts, wolkenkraag) for the player who tames one: the pieces they don't have
     * yet, once per player (the wolkenkist on the islands only has them for whoever opens it first). Returns how many.
     */
    public static int wolkPakje(ServerPlayer player) {
        CompoundTag saved = nl.juiced.guhs.quest.GuhQuests.saved(player);
        if (saved.getBooleanOr(PAKJE, false)) {
            return 0;
        }
        saved.putBoolean(PAKJE, true);
        int n = 0;
        for (nl.juiced.guhs.entity.GuhClothes c : List.of(nl.juiced.guhs.entity.GuhClothes.WOLKENMUTS, nl.juiced.guhs.entity.GuhClothes.WOLKENKRAAG)) {
            net.minecraft.world.item.Item item = ModItems.clothingItem(c);
            if (!nl.juiced.guhs.feature.kleding.KledingUnlocks.heeft(player, c) && nl.juiced.guhs.quest.GuhQuests.count(player, item) == 0) {
                nl.juiced.guhs.quest.GuhQuests.give(player, item);
                n++;
            }
        }
        if (n > 0) {
            player.sendSystemMessage(Component.translatable("quest.guhs.eilanden.pakje").withStyle(ChatFormatting.AQUA));
        }
        return n;
    }

    /** A wild Wolkguh is a cloud: nothing hurts it (only /kill). Its outfit is for whoever tames it, not for whoever hits hardest. */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!isWolk(event.getEntity()) || ((GuhEntity) event.getEntity()).isTame()
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        event.setCanceled(true);
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.eilanden.no_hurt").withStyle(ChatFormatting.AQUA));
            player.level().sendParticles(ParticleTypes.CLOUD, event.getEntity().getX(), event.getEntity().getY() + 0.5,
                    event.getEntity().getZ(), 8, 0.4, 0.3, 0.4, 0.02);
        }
    }

    /** There's only one Wolkguh: its babies are snow guhs (white and fluffy, but no cloud). */
    @SubscribeEvent
    public static void onBaby(BabyEntitySpawnEvent event) {
        if (event.getChild() instanceof GuhEntity baby && baby.getVariant() == GuhVariant.WOLK) {
            baby.setVariant(GuhVariant.SNOW);
        }
    }

    // --- the Wolkguh itself -------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!isWolk(event.getEntity()) || event.getEntity().level().isClientSide()) {
            return;
        }
        GuhEntity guh = (GuhEntity) event.getEntity();
        ServerLevel level = (ServerLevel) guh.level();
        // light as a cloud: it never falls fast
        Vec3 v = guh.getDeltaMovement();
        if (!guh.onGround() && !guh.isPassenger() && v.y < -FLOAT_SPEED) {
            guh.setDeltaMovement(v.x, -FLOAT_SPEED, v.z);
        }
        guh.resetFallDistance();
        if (guh.tickCount % 40 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, guh.getX(), guh.getY() + 0.2, guh.getZ(), 2, 0.4, 0.1, 0.4, 0.005);
        }
        if (!guh.isTame() && guh.tickCount % 20 == 0 && !guh.isLeashed() && !guh.isPassenger()) {
            keepHome(guh, level);
        }
    }

    /** A wild Wolkguh remembers where it lives; wander off (or fall off the island) and it drifts back. */
    static void keepHome(GuhEntity guh, ServerLevel level) {
        CompoundTag data = guh.getPersistentData();
        if (!data.contains(HOME)) {
            if (guh.onGround()) {
                data.putLong(HOME, guh.blockPosition().asLong());
            }
            return;
        }
        BlockPos home = BlockPos.of(data.getLongOr(HOME, 0L));
        double dx = guh.getX() - (home.getX() + 0.5), dz = guh.getZ() - (home.getZ() + 0.5);
        if (dx * dx + dz * dz > HOME_RANGE * HOME_RANGE || guh.getY() < home.getY() - HOME_DROP) {
            level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.4, guh.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            guh.getNavigation().stop();
            guh.setDeltaMovement(Vec3.ZERO);
            guh.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            level.sendParticles(ParticleTypes.CLOUD, guh.getX(), guh.getY() + 0.4, guh.getZ(), 16, 0.4, 0.3, 0.4, 0.02);
            level.playSound(null, home, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1f, 1.5f);
        }
    }

    // --- a new Wolkguh for the next player (1.2.7) ---------------------------------------------------------------------

    /** Every 10 seconds per player on the islands: is there still a wild Wolkguh? If not for a day, a new one floats in. */
    @SubscribeEvent
    public static void onIslandTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % 200 != 0 || player.isSpectator()
                || player.level().dimension() != nl.juiced.guhs.world.ModDimensions.GUHMENSION) {
            return;
        }
        ServerLevel level = player.level();
        for (var stuk : nl.juiced.guhs.world.Terugkeer.stukken(level, EilandenFeature.ISLANDS, player.blockPosition(), "zwevende_eilanden")) {
            net.minecraft.world.level.levelgen.structure.BoundingBox b = stuk.getBoundingBox();
            wolkTerug(level, nl.juiced.guhs.world.Terugkeer.wereld(stuk, WOLK_PLEK),
                    new net.minecraft.world.phys.AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(8));
        }
    }

    /**
     * One island group: {@code plek} is where its Wolkguh lives, {@code eilanden} the whole building. A wild Wolkguh
     * anywhere on it counts; when there has been none for {@link #NIEUWE_WOLK_NA} a new one comes. Returns the new one.
     */
    @javax.annotation.Nullable
    public static GuhEntity wolkTerug(ServerLevel level, BlockPos plek, net.minecraft.world.phys.AABB eilanden) {
        boolean wild = !level.getEntitiesOfClass(GuhEntity.class, eilanden, g -> isWolk(g) && !g.isTame() && g.isAlive()).isEmpty();
        if (!nl.juiced.guhs.world.Terugkeer.moetTerug(level, SOORT, plek, wild, NIEUWE_WOLK_NA)) {
            return null;
        }
        return nieuweWolkguh(level, plek);
    }

    /** A new wild Wolkguh at home, in its own outfit (like the one the islands come with). */
    public static GuhEntity nieuweWolkguh(ServerLevel level, BlockPos plek) {
        GuhEntity guh = nl.juiced.guhs.registry.ModEntities.GUH.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        guh.snapTo(plek.getX() + 0.5, plek.getY() + 0.1, plek.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
        guh.setVariant(GuhVariant.WOLK);
        guh.setGuhScale(WOLK_SCHAAL);
        guh.wear(nl.juiced.guhs.entity.GuhClothes.WOLKENMUTS);
        guh.wear(nl.juiced.guhs.entity.GuhClothes.WOLKENKRAAG);
        guh.setPersistenceRequired();
        guh.getPersistentData().putLong(HOME, plek.asLong());
        level.addFreshEntity(guh);
        level.sendParticles(ParticleTypes.CLOUD, guh.getX(), guh.getY() + 0.5, guh.getZ(), 30, 0.8, 0.5, 0.8, 0.02);
        level.playSound(null, plek, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1f, 1.5f);
        return guh;
    }

    // --- falling -------------------------------------------------------------------------------------------------------

    /** Your own Wolkguh catches you like a pillow of cloud. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getDistance() <= 3) {
            return;
        }
        List<GuhEntity> wolken = ownWolkguhs(player);
        if (wolken.isEmpty()) {
            return;
        }
        event.setDamageMultiplier(0);
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 20, 0.5, 0.1, 0.5, 0.03);
        level.playSound(null, player.blockPosition(), SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 1f, 0.8f);
        if (event.getDistance() >= 6) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.eilanden.caught_by_guh", wolken.get(0).getDisplayName())
                    .withStyle(ChatFormatting.AQUA));
            GuhAdvancements.grant(player, "eilanden_opgevangen");
        }
    }

    /** The tamed Wolkguhs of this player close enough to catch them. */
    public static List<GuhEntity> ownWolkguhs(ServerPlayer player) {
        return player.level().getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(CATCH_RANGE),
                g -> isWolk(g) && g.isTame() && player.getUUID().equals(g.getOwnerUUID()) && g.isAlive());
    }

    /** Falling off (or next to) the floating islands, or off the top of a wolkenlift: the clouds catch you (slow falling). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 4 != 0) {
            return;
        }
        if (player.fallDistance < 5 || player.getAbilities().flying || player.isFallFlying() || player.hasEffect(MobEffects.SLOW_FALLING)) {
            if (player.onGround()) {
                player.getPersistentData().remove(CAUGHT);
            }
            return;
        }
        CompoundTag data = player.getPersistentData();
        boolean lifted = data.contains(LIFT_MSG) && player.level().getGameTime() - data.getLongOr(LIFT_MSG, 0L) < LIFT_GRACE;
        if (lifted || EilandenFeature.islandsAt(player.level(), player.blockPosition()) != null) {
            catchFalling(player);
        }
    }

    /** Gives a falling player slow falling (and says so, once per fall). */
    public static void catchFalling(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 12, 0, false, false, true));
        player.resetFallDistance();
        if (!player.getPersistentData().getBooleanOr(CAUGHT, false)) {
            player.getPersistentData().putBoolean(CAUGHT, true);
            player.sendOverlayMessage(Component.translatable("quest.guhs.eilanden.caught").withStyle(ChatFormatting.AQUA));
            player.level().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 16, 0.6, 0.3, 0.6, 0.02);
        }
    }

    // --- the wolkenlift ------------------------------------------------------------------------------------------------

    /** A player rides a wolkenlift (server side): VAHOEG! (at most once every few seconds) and the quest advancement. */
    public static void onLift(ServerPlayer player, boolean down) {
        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();
        if (now - data.getLongOr(LIFT_MSG, 0L) > 60) {
            player.sendOverlayMessage(Component.translatable(down ? "quest.guhs.eilanden.lift_down" : "quest.guhs.eilanden.lift_up")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.level().playSound(null, player.blockPosition(), down ? SoundEvents.WOOL_PLACE : SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.PLAYERS, 0.8f, down ? 0.8f : 1.3f);
        }
        data.putLong(LIFT_MSG, now);
        if (!down) {
            GuhAdvancements.grant(player, "eilanden_lift");
        }
    }

    private EilandenEvents() {
    }
}
