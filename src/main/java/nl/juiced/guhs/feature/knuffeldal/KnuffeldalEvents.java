package nl.juiced.guhs.feature.knuffeldal;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The Knuffeldal's own events: the Pluisguh (wild guhs born in the Knuffeldal), the residents of the town (they stay
 * near their house, say hello when you right-click them, love a kaasknabbel but never go with you: they live here) and
 * the town counting for the Knus tab.
 */
public final class KnuffeldalEvents {
    /** Out of the wild guhs born in the Knuffeldal, this many are a Pluisguh. */
    public static final float PLUISGUH_CHANCE = 0.35f;
    /** A resident walks back home when it gets this far away. */
    public static final double THUIS_AFSTAND = 9.0;
    static final String CHECKED = "guhs_knuffeldal_checked";
    public static final int BEWONER_LINES = 3;

    private KnuffeldalEvents() {
    }

    /** The GuhHooks of the knuffeldal feature (called from KnuffeldalFeature.register). */
    static void hooks() {
        GuhHooks.doelen((guh, goals) -> goals.addGoal(5, new NaarHuisGoal(guh)));
        GuhHooks.klik(KnuffeldalEvents::klik);
        GuhHooks.tick(guh -> {
            if ((guh.tickCount + guh.getId()) % 100 == 0 && guh.isTame()) {
                Seizoensactiviteiten.guhTick(guh);
            }
        });
    }

    // --- the Pluisguh -----------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof GuhEntity guh)) {
            return;
        }
        if (GuhHooks.isBewoner(guh) && GuhHooks.thuis(guh) == null) {
            // a resident fresh from the town template: its spot is its home, its name from the template
            GuhHooks.maakBewoner(guh, guh.blockPosition());
            String naam = guh.getPersistentData().getStringOr(GuhHooks.BEWONER_NAAM, "");
            if (!naam.isEmpty()) {
                guh.setCustomName(Component.translatable("entity.guhs.bewoner." + naam));
            }
        }
        maybePluisguh(level, guh);
    }

    /** Decides (once per guh) whether a new wild guh in the Knuffeldal becomes a Pluisguh. Returns true when it did. */
    public static boolean maybePluisguh(ServerLevel level, GuhEntity guh) {
        if (guh.getClass() != GuhEntity.class || guh.getPersistentData().getBooleanOr(CHECKED, false)) {
            return false;
        }
        return decide(guh, level.dimension() == ModDimensions.GUHMENSION && inKnuffeldal(level, guh.blockPosition()));
    }

    /** The decision itself (once per guh): in the Knuffeldal, a plain wild grown-up guh may become a Pluisguh. */
    public static boolean decide(GuhEntity guh, boolean inKnuffeldal) {
        CompoundTag data = guh.getPersistentData();
        if (data.getBooleanOr(CHECKED, false)) {
            return false;
        }
        data.putBoolean(CHECKED, true);
        if (!inKnuffeldal || guh.isTame() || guh.isBaby() || guh.hasCustomName() || guh.getVariant() != GuhVariant.NORMAL || GuhHooks.isBewoner(guh)) {
            return false;
        }
        if (guh.getRandom().nextFloat() < PLUISGUH_CHANCE) {
            guh.setVariant(GuhVariant.PLUISGUH);
            return true;
        }
        return false;
    }

    public static boolean inKnuffeldal(Level level, BlockPos pos) {
        return level.getBiome(pos).is(KnuffeldalFeature.KNUFFELDAL);
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getAnimal() instanceof GuhEntity guh && guh.getVariant() == GuhVariant.PLUISGUH && event.getTamer() instanceof ServerPlayer player) {
            KnusVoortgang.hoogste(player, KnuffeldalVoortgang.PLUISGUH, 1);
        }
    }

    // --- the title -------------------------------------------------------------------------------------------------------

    /** The Knuffelburgemeester (the Grote Knusfeest's finale) has the title behind their name in the player list. */
    @SubscribeEvent
    public static void onTabName(net.neoforged.neoforge.event.entity.player.PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player && Burgemeester.isKnuffelburgemeester(player)) {
            Component base = event.getDisplayName() != null ? event.getDisplayName() : player.getName();
            event.setDisplayName(base.copy().append(Component.literal(" ✿ ").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.translatable("gui.guhs.knuffeldal.titel").withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }

    // --- the town ---------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator() || player.tickCount % 40 != 7) {
            return;
        }
        if (player.level().dimension() == ModDimensions.GUHMENSION && PleinSlot.inStadje(player.level(), player.blockPosition())) {
            if (KnusVoortgang.teller(player, KnuffeldalVoortgang.STADJE) == 0) {
                KnusVoortgang.hoogste(player, KnuffeldalVoortgang.STADJE, 1);
                player.sendSystemMessage(Component.translatable("gui.guhs.knuffeldal.welkom").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        KruimelMikaEntity.playerTick(player);
    }

    /** A resident: a cute line, and it counts as a friend (knuffelvriendjes). With a kaasknabbel: it eats it, but stays home. */
    static InteractionResult klik(GuhEntity guh, Player player, InteractionHand hand) {
        if (!GuhHooks.isBewoner(guh) || guh.isTame() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean knabbel = stack.is(ModItems.KAAS_KNABBELS.get());
        if (!stack.isEmpty() && !knabbel) {
            return InteractionResult.PASS;
        }
        if (guh.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            String naam = guh.getPersistentData().getStringOr(GuhHooks.BEWONER_NAAM, "");
            if (knabbel) {
                stack.consume(1, player);
                guh.playSound(ModSounds.GUH_EAT.get(), 1f, guh.getVoicePitch());
                guh.level().broadcastEntityEvent(guh, (byte) 7);   // hearts
                GuhQuests.say(sp, guh, "quest.guhs.knuffeldal.bewoner.knabbel");
            } else {
                guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch());
                GuhQuests.say(sp, guh, naam.isEmpty() ? "quest.guhs.knuffeldal.bewoner.hoi"
                        : "quest.guhs.knuffeldal.bewoner." + naam + "." + guh.getRandom().nextInt(BEWONER_LINES));
            }
            guh.triggerAnim("action", "happy");
            if (!naam.isEmpty()) {
                vriendje(sp, naam);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** A friend in the town: an entry in the knuffelvriendjes (the residents also count up to the milestone). */
    public static void vriendje(ServerPlayer player, String naam) {
        if (KnusVoortgang.ontdek(player, KnuffeldalVoortgang.VRIENDJES_BOEK, naam) && KnuffeldalVoortgang.BEWONERS.contains(naam)) {
            KnusVoortgang.tel(player, KnuffeldalVoortgang.VRIENDJES, 1);
            if (KnusVoortgang.teller(player, KnuffeldalVoortgang.VRIENDJES) >= KnuffeldalVoortgang.BEWONERS.size()) {
                GuhAdvancements.grant(player, "knuffeldal_alle_vriendjes");
            }
        }
    }

    /** A resident that wandered off walks back home (not while tamed, sitting, or busy with an activity). */
    static class NaarHuisGoal extends Goal {
        private final GuhEntity guh;
        @Nullable
        private BlockPos thuis;

        NaarHuisGoal(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!GuhHooks.isBewoner(guh) || guh.isTame() || guh.isOrderedToSit() || GuhHooks.isBezig(guh)) {
                return false;
            }
            thuis = GuhHooks.thuis(guh);
            if (thuis == null) {
                return false;
            }
            double d = guh.blockPosition().distSqr(thuis);
            if (d <= 4) {
                onderweg = false;
            }
            // (once on its way it keeps wanting to go home, also after a wave or a hop in between)
            return d > THUIS_AFSTAND * THUIS_AFSTAND || onderweg && d > 4;
        }

        private boolean onderweg;

        private int ticks;

        @Override
        public void start() {
            ticks = 0;
            onderweg = true;
            loop();
        }

        private void loop() {
            if (thuis != null) {
                guh.getNavigation().moveTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5, 0.9);
            }
        }

        @Override
        public void tick() {
            // (a path can end short of home, or get pushed aside: keep going until it's there)
            if (++ticks % 20 == 0 && guh.getNavigation().isDone()) {
                loop();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return thuis != null && ticks < 20 * 30 && guh.blockPosition().distSqr(thuis) > 4 && !GuhHooks.isBezig(guh) && !guh.isTame();
        }

        @Override
        public void stop() {
            guh.getNavigation().stop();
        }
    }
}
