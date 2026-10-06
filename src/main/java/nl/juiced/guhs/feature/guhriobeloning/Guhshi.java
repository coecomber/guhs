package nl.juiced.guhs.feature.guhriobeloning;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VariantGedrag;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Guhshi "is yours", once per player, after the duel with the Grote Nether-Mika.
 * <ul>
 *     <li>In the tower room of every castle a Guhshi waits next to Prinses Perzikguh: a story copy (never tameable
 *     itself, it stays; {@link #kopie} makes it, the template brings it). Whoever won the duel clicks it and is asked
 *     "may I come with you?": yes = a NEW Guhshi of your own next to it ({@link #geef}: tamed, rideable, already wearing
 *     his red saddle). Everybody keeps their own chance; the copy stays for the next player.</li>
 *     <li>A Guhshi of your own ({@link Gedrag}, the VariantGedrag of GuhVariant GUHSHI):
 *     <ul>
 *         <li>ridden, he jumps when you press jump, and holding jump in the air makes him flutter: for
 *         {@link Gedrag#FLADDER_TICKS} ticks he stops falling and even climbs a little, once per jump, so he carries
 *         you over gaps of about seven blocks;</li>
 *         <li>he eats kaasknabbels from a distance: a knabbel that lies on the ground within {@link Gedrag#TONG} blocks
 *         in his sight is licked up with his long tongue (one every two seconds; it heals him and makes him happy). The
 *         button in his guh menu switches the tongue off and on.</li>
 *     </ul>
 *     The Guhshi that carries you IN a level (the engine's look: a guh without AI) and the copy in the tower room do
 *     none of this.</li>
 * </ul>
 */
public final class Guhshi {
    /** The key of the "may I come with you?" screen (Praat). */
    public static final String SLEUTEL = "guhriobeloning_guhshi";
    public static final int JA = 1, NEE = 2;
    private static final String T = "quest.guhs.guhriobeloning.guhshi.";

    private Guhshi() {
    }

    // =====================================================================================================================
    // the copy in the tower room, and "he is yours"
    // =====================================================================================================================

    /** (Bezetting.wezen) the Guhshi that waits in the tower room: a story copy, not yet added to the world. */
    @Nullable
    static Entity kopie(ServerLevel level, Vec3 plek, Rotation draai) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.setVariant(GuhVariant.GUHSHI);
        guh.setGuhScale(VerhaalGuhs.SCHAAL);
        guh.snapTo(plek.x, plek.y, plek.z, guh.rotate(draai), 0f);
        VerhaalGuhs.markeer(guh, VerhaalGuh.GUHSHI, BlockPos.containing(plek));
        return guh;
    }

    /** (GuhrioKasteel.BIJ_DUEL) the duel is won: from now on this player may take a Guhshi along. */
    static void duelGewonnen(ServerPlayer p) {
        boolean nieuw = !VerhaalGuhs.isVrij(p, VerhaalGuh.GUHSHI);
        VerhaalGuhs.geefVrij(p, VerhaalGuh.GUHSHI);
        if (nieuw && !VerhaalGuhs.heeftGetemd(p, VerhaalGuh.GUHSHI)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhriobeloning.guhshi.vrij").withStyle(ChatFormatting.GREEN));
        }
    }

    /** (VerhaalGuhs.Klik) a click on the Guhshi of the tower room. */
    static InteractionResult klikKopie(GuhEntity kopie, ServerPlayer p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        if (GuhrioKasteel.duelGewonnen(p) && !VerhaalGuhs.isVrij(p, VerhaalGuh.GUHSHI)) {
            VerhaalGuhs.geefVrij(p, VerhaalGuh.GUHSHI);      // (the duel was won before this listener existed)
        }
        if (VerhaalGuhs.magTemmen(p, VerhaalGuh.GUHSHI)) {
            Praat.open(p, kopie, SLEUTEL, T + "vraag", new Object[0], new Praat.Optie(JA, T + "ja"), new Praat.Optie(NEE, T + "nee"));
        } else if (VerhaalGuhs.heeftGetemd(p, VerhaalGuh.GUHSHI)) {
            GuhQuests.say(p, kopie, T + "al_getemd");
        } else {
            GuhQuests.say(p, kopie, T + (GuhrioKasteel.heeftEi(p) ? "wacht_ei" : "wacht"));
        }
        kopie.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.25f);
        return InteractionResult.SUCCESS;
    }

    /** (Praat) the answer to "may I come with you?". */
    static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (optie == JA) {
            Vec3 waar = spreker != null ? spreker.position().add(p.position().subtract(spreker.position()).multiply(1, 0, 1).normalize().scale(1.5))
                    : p.position().add(p.getLookAngle().multiply(2, 0, 2));
            geef(p, waar);
            Praat.sluit(p);
        } else if (optie == NEE) {
            Praat.sluit(p);
        }
    }

    /**
     * Your own Guhshi at this spot: tamed by you (the normal tame path), big enough to ride and already saddled. Null when
     * this player may not (the duel is not won, or their Guhshi was given before).
     */
    @Nullable
    public static GuhEntity geef(ServerPlayer p, Vec3 waar) {
        GuhEntity guh = VerhaalGuhs.tem(p, VerhaalGuh.GUHSHI, waar);
        if (guh == null) {
            return null;
        }
        guh.equipSaddle(new ItemStack(Items.SADDLE), null);
        ServerLevel level = p.level();
        level.playSound(null, guh, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.6f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, guh.getX(), guh.getY() + 0.8, guh.getZ(), 24, 0.7, 0.6, 0.7, 0.0);
        p.sendSystemMessage(Component.translatable("gui.guhs.guhriobeloning.guhshi.gelukt").withStyle(ChatFormatting.GOLD));
        GuhAdvancements.grant(p, "guhrio_beloning_guhshi");
        GidsFeature.grant(p, "guhrio/guhrio_beloning_guhshi");
        GuhrioBeloningFeature.bijwerken(p);
        return guh;
    }

    // =====================================================================================================================
    // a Guhshi of your own
    // =====================================================================================================================

    /** What a Guhshi does (VariantGedragen.zet(GuhVariant.GUHSHI, ...)): the flutter jump and the long tongue. */
    public static final class Gedrag implements VariantGedrag {
        /** How far his tongue reaches (blocks), ticks between two licks, what a knabbel heals. */
        public static final double TONG = 5.0;
        public static final int TONG_RUST = 40;
        public static final float HAP_GENEEST = 4f;
        /** Ridden: how hard he jumps, how long a held jump flutters, the speed a flutter climbs to. */
        public static final double SPRONG = 0.52;
        public static final int FLADDER_TICKS = 24;
        public static final double FLADDER_STIJG = 0.06;
        /** Persistent keys: the tongue is switched off; the game time of the next lick. */
        public static final String TONG_UIT = "guhs_guhriobeloning_tong_uit", TONG_TOT = "guhs_guhriobeloning_tong_tot";
        /** (tests) pretend the rider holds jump; on the rider's own client it is the jump key. */
        public static volatile boolean testSpring;

        /** Per ridden Guhshi: ticks of flutter used in this jump, ticks until he may jump again. */
        private static final Map<GuhEntity, int[]> LUCHT = java.util.Collections.synchronizedMap(new WeakHashMap<>());

        /** A Guhshi that is somebody's own (not the copy of the tower room, not the look that carries you in a level). */
        public static boolean eigen(GuhEntity guh) {
            return guh.isTame() && guh.getOwnerUUID() != null && guh.isAlive() && !guh.isNoAi() && !VerhaalGuhs.isKopie(guh)
                    && !guh.getPersistentData().contains(GuhrioSpel.GUHSHI_TAG);
        }

        /** May he lick now (his own tongue, switched on, not in a huisje, not carrying anybody)? */
        public static boolean magTong(GuhEntity guh) {
            return eigen(guh) && !guh.getPersistentData().getBooleanOr(TONG_UIT, false) && !Huisjes.isBinnen(guh) && !guh.isVehicle() && !guh.isBaby();
        }

        @Override
        public void tick(GuhEntity guh) {
            if (guh.level().isClientSide() || (guh.tickCount + guh.getId()) % 10 != 0 || !magTong(guh)) {
                return;
            }
            if (guh.level().getGameTime() >= guh.getPersistentData().getLongOr(TONG_TOT, 0L)) {
                tong(guh);
            }
        }

        /**
         * One lick: the nearest kaasknabbel that lies on the ground within {@link #TONG} blocks and in sight (not just
         * dropped, not meant for somebody, not loaned) is eaten: a pink tongue shoots out, he heals and is happy. True when
         * he ate one.
         */
        public static boolean tong(GuhEntity guh) {
            if (!(guh.level() instanceof ServerLevel level)) {
                return false;
            }
            ItemEntity doel = null;
            double beste = TONG * TONG + 0.01;
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, guh.getBoundingBox().inflate(TONG), ItemEntity::isAlive)) {
                double d = item.distanceToSqr(guh);
                if (d < beste && item.getItem().is(ModItems.KAAS_KNABBELS.get()) && !item.hasPickUpDelay() && item.getTarget() == null
                        && !Features.isLoaned(item.getItem()) && guh.hasLineOfSight(item)) {
                    doel = item;
                    beste = d;
                }
            }
            if (doel == null) {
                return false;
            }
            ItemStack hap = doel.getItem().copyWithCount(1);
            Vec3 bek = guh.getEyePosition().add(guh.getLookAngle().multiply(0.5, 0, 0.5)), naar = doel.position().add(0, 0.2, 0);
            DustParticleOptions tong = new DustParticleOptions(0xFF6FA8, 1.1f);
            int stappen = Math.max(2, (int) (bek.distanceTo(naar) * 5));
            for (int i = 0; i <= stappen; i++) {
                Vec3 punt = bek.lerp(naar, i / (double) stappen);
                level.sendParticles(tong, punt.x, punt.y, punt.z, 1, 0, 0, 0, 0);
            }
            guh.getLookControl().setLookAt(doel);
            ItemStack rest = doel.getItem().copy();
            rest.shrink(1);
            if (rest.isEmpty()) {
                doel.discard();
            } else {
                doel.setItem(rest);
            }
            level.playSound(null, guh, SoundEvents.FROG_TONGUE, SoundSource.NEUTRAL, 1f, 1.3f);
            level.playSound(null, guh, ModSounds.GUH_EAT.get(), SoundSource.NEUTRAL, 0.9f, guh.getVoicePitch());
            guh.heal(HAP_GENEEST);
            guh.triggerAnim("action", "happy");
            level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 2, 0.3, 0.2, 0.3, 0.02);
            guh.getPersistentData().putLong(TONG_TOT, level.getGameTime() + TONG_RUST);
            LivingEntity baas = guh.getOwner();
            if (baas instanceof ServerPlayer sp && sp.level() == level && sp.distanceTo(guh) <= 24) {
                nl.juiced.guhs.feature.band.BandEvents.gevoerd(guh, sp, hap);   // (hearts for feeding, with the day's cap)
                GuhAdvancements.grant(sp, "guhrio_beloning_tong");
            }
            return true;
        }

        /**
         * Ridden by a player (this runs in the rider's own game, which tells the server where they went): jump on the
         * jump key, and a held jump flutters at the top: no falling, a slow climb, for {@link #FLADDER_TICKS} ticks, once
         * per jump. The normal walking goes on (false).
         */
        @Override
        public boolean travel(GuhEntity guh, Vec3 input) {
            if (!(guh.getControllingPassenger() instanceof Player) || !eigen(guh)) {
                LUCHT.remove(guh);
                return false;
            }
            int[] t = LUCHT.computeIfAbsent(guh, g -> new int[2]);
            boolean spring = guh.level().isClientSide() ? GuhEntity.riderJumping.getAsBoolean() : testSpring;
            if (t[1] > 0) {
                t[1]--;
            }
            if (guh.onGround() || guh.isInWater() || guh.onClimbable()) {
                t[0] = 0;
                if (spring && t[1] == 0 && guh.onGround() && !guh.isGravityEnabled()) {
                    Vec3 v = guh.getDeltaMovement();
                    guh.setDeltaMovement(v.x, SPRONG, v.z);
                    guh.needsSync = true;
                    t[1] = 10;
                    guh.playSound(SoundEvents.SLIME_JUMP_SMALL, 0.5f, 1.4f);
                }
                return false;
            }
            Vec3 v = guh.getDeltaMovement();
            if (spring && v.y < FLADDER_STIJG && t[0] < FLADDER_TICKS) {
                t[0]++;
                guh.setDeltaMovement(v.x, v.y + (FLADDER_STIJG - v.y) * 0.35, v.z);
                guh.fallDistance = 0;
                if (guh.level().isClientSide()) {
                    if (t[0] % 3 == 1) {
                        guh.level().addParticle(ParticleTypes.CLOUD, guh.getX(), guh.getY() + 0.1, guh.getZ(), 0, -0.02, 0);
                    }
                    if (t[0] % 5 == 1) {
                        guh.level().playLocalSound(guh.getX(), guh.getY(), guh.getZ(), SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 0.5f, 1.6f, false);
                    }
                }
            }
            return false;
        }

        /** (tests) the flutter ticks this ridden Guhshi has used in this jump. */
        public static int fladderTicks(GuhEntity guh) {
            int[] t = LUCHT.get(guh);
            return t == null ? 0 : t[0];
        }

        @Override
        public String speciaalKnop() {
            return "gui.guhs.guhriobeloning.guhshi.tong";
        }

        @Override
        public void speciaal(GuhEntity guh, ServerPlayer owner) {
            boolean uit = !guh.getPersistentData().getBooleanOr(TONG_UIT, false);
            guh.getPersistentData().putBoolean(TONG_UIT, uit);
            owner.sendOverlayMessage(Component.translatable(uit ? "gui.guhs.guhriobeloning.guhshi.tong.uit" : "gui.guhs.guhriobeloning.guhshi.tong.aan",
                    guh.getDisplayName()).withStyle(ChatFormatting.GREEN));
            guh.playSound(SoundEvents.FROG_TONGUE, 0.8f, uit ? 0.8f : 1.4f);
        }
    }
}
