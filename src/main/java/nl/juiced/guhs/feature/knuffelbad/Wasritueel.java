package nl.juiced.guhs.feature.knuffelbad;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;

import net.minecraft.core.UUIDUtil;
/**
 * The washing ritual: your own tamed guh gets a bath in a guh wash tub and comes out fluffy and shiny for a whole day.
 * <ol>
 *   <li>Inzepen: use guhshampoo on your guh next to a free wash tub: it hops in (the tub fills with water);</li>
 *   <li>Schuimen: scrub it {@value #SCHROBBEN} times (right-click it with an empty hand): lots of pink foam;</li>
 *   <li>Spoelen: the tub's shower (right-click the tub with an empty hand, or pour a bucket of water): the foam is gone;</li>
 *   <li>Föhnen: hold the guh-föhn on it until it's dry ({@value #FOHN_TICKS} ticks): GLANZEND for a day (the flag of
 *       {@link GuhHooks}: it sparkles and shimmers), a happy VAHOEG jump, a little prize.</li>
 * </ol>
 * The state lives in the guh's persistent data (keys guhs_knuffelbad_*). Wrong order: a friendly hint. Nobody washes it
 * for two minutes: it hops out.
 */
public final class Wasritueel {
    public static final String STAP = "guhs_knuffelbad_stap", WASSER = "guhs_knuffelbad_wasser", TOBBE = "guhs_knuffelbad_tobbe",
            TIJD = "guhs_knuffelbad_tijd", SCHROB = "guhs_knuffelbad_schrob", GEFOHND = "guhs_knuffelbad_fohn", GLANS_TOT = "guhs_knuffelbad_glans_tot",
            ZAT = "guhs_knuffelbad_zat";
    public static final int INGEZEEPT = 1, GESCHUIMD = 2, GESPOELD = 3;
    public static final int SCHROBBEN = 5, FOHN_TICKS = 50, GLANS_DUUR = 24000, VERGEET = 2400;
    /** A clean guh is worth an eendjesmunt (2.8: one more than its base). */
    public static final int WAS_MUNTEN = 1 + 1;
    public static final double TOBBE_BEREIK = 6.0;

    /** Who is washing which guh right now (for "one game at a time"). */
    private static final Map<UUID, Long> WASSERS = new ConcurrentHashMap<>();

    static void register() {
        GuhHooks.klik(Wasritueel::klik);
        GuhHooks.tick(Wasritueel::tick);
    }

    public static int stap(GuhEntity guh) {
        return guh.getPersistentData().getIntOr(STAP, 0);
    }

    public static boolean glanst(GuhEntity guh) {
        return GuhHooks.heeft(guh, GuhHooks.GLANZEND);
    }

    /** Is this player busy washing (a wash that was touched in the last minute)? */
    public static boolean bezig(ServerPlayer player) {
        Long t = WASSERS.get(player.getUUID());
        return t != null && player.level().getGameTime() - t < 1200;
    }

    static void vergeetAlles() {
        WASSERS.clear();
    }

    // --- 1. soap ------------------------------------------------------------------------------------------------------------

    /** Guhshampoo on a guh. True: it was soaped (the shampoo loses a use). */
    public static boolean inzepen(ServerPlayer player, GuhEntity guh) {
        ServerLevel level = player.level();
        if (!guh.isTame() || !guh.isOwnedBy(player)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.was.niet_van_jou").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (stap(guh) > 0) {
            hint(player, guh);
            return false;
        }
        if (Minigames.busyElsewhere(player, Minigames.KNUFFELBAD)) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.minigame.busy").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        BlockPos tobbe = vrijeTobbe(level, guh.blockPosition());
        if (tobbe == null) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.was.geen_tobbe").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        CompoundTag d = guh.getPersistentData();
        d.putBoolean(ZAT, guh.isOrderedToSit());
        d.putInt(STAP, INGEZEEPT);
        d.store(WASSER, UUIDUtil.CODEC, player.getUUID());
        d.putLong(TOBBE, tobbe.asLong());
        d.putLong(TIJD, level.getGameTime());
        d.putInt(SCHROB, 0);
        d.putInt(GEFOHND, 0);
        guh.setOrderedToSit(true);
        guh.getNavigation().stop();
        inTobbe(guh, tobbe);
        GuhHooks.bezig(guh, VERGEET);
        zetVulling(level, tobbe, KnuffelbadBlocks.Vulling.WATER);
        WASSERS.put(player.getUUID(), level.getGameTime());
        level.sendParticles(KnuffelbadFeature.ZEEPBELLETJE.get(), guh.getX(), guh.getY() + 0.6, guh.getZ(), 16, 0.4, 0.3, 0.4, 0.02);
        level.playSound(null, guh.blockPosition(), KnuffelbadFeature.SCHUIM.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.was.ingezeept", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** A free wash tub near a spot (no other guh being washed in it). */
    @Nullable
    static BlockPos vrijeTobbe(ServerLevel level, BlockPos near) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        int r = (int) Math.ceil(TOBBE_BEREIK);
        for (BlockPos p : BlockPos.betweenClosed(near.offset(-r, -2, -r), near.offset(r, 2, r))) {
            if (!level.getBlockState(p).is(KnuffelbadFeature.GUH_WASTOBBE.get())) {
                continue;
            }
            double d = p.distSqr(near);
            if (d < bestD && d <= TOBBE_BEREIK * TOBBE_BEREIK && gastIn(level, p) == null) {
                best = p.immutable();
                bestD = d;
            }
        }
        return best;
    }

    /** The guh being washed in this tub, if any. */
    @Nullable
    static GuhEntity gastIn(ServerLevel level, BlockPos tobbe) {
        for (GuhEntity g : level.getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(tobbe).inflate(3))) {
            if (stap(g) > 0 && g.getPersistentData().getLongOr(TOBBE, 0L) == tobbe.asLong()) {
                return g;
            }
        }
        return null;
    }

    private static void inTobbe(GuhEntity guh, BlockPos tobbe) {
        guh.snapTo(tobbe.getX() + 0.5, tobbe.getY() + 0.15, tobbe.getZ() + 0.5, guh.getYRot(), 0);
        guh.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }

    private static void zetVulling(ServerLevel level, BlockPos tobbe, KnuffelbadBlocks.Vulling vulling) {
        BlockState st = level.getBlockState(tobbe);
        if (st.is(KnuffelbadFeature.GUH_WASTOBBE.get())) {
            level.setBlock(tobbe, st.setValue(KnuffelbadBlocks.VULLING, vulling), 3);
        }
    }

    // --- 2. foam: scrubbing with an empty hand ----------------------------------------------------------------------------------

    private static InteractionResult klik(GuhEntity guh, Player player, InteractionHand hand) {
        int stap = stap(guh);
        if (stap == 0 || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && !held.is(KnuffelbadFeature.GUHSHAMPOO.get())) {
            return held.is(KnuffelbadFeature.GUH_FOHN.get()) ? InteractionResult.PASS : InteractionResult.PASS;
        }
        if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
            schrobben(sp, guh);
        }
        return InteractionResult.SUCCESS;
    }

    /** One scrub. After {@value #SCHROBBEN}: the guh is all foam. */
    public static void schrobben(ServerPlayer player, GuhEntity guh) {
        ServerLevel level = player.level();
        if (!wasser(player, guh)) {
            return;
        }
        if (stap(guh) != INGEZEEPT) {
            hint(player, guh);
            return;
        }
        CompoundTag d = guh.getPersistentData();
        int n = d.getIntOr(SCHROB, 0) + 1;
        d.putInt(SCHROB, n);
        d.putLong(TIJD, level.getGameTime());
        WASSERS.put(player.getUUID(), level.getGameTime());
        level.sendParticles(KnuffelbadFeature.SCHUIMVLOKJE.get(), guh.getX(), guh.getY() + 0.6, guh.getZ(), 6 + n * 3, 0.35, 0.3, 0.35, 0.03);
        level.playSound(null, guh.blockPosition(), KnuffelbadFeature.SCHUIM.get(), SoundSource.NEUTRAL, 0.6f, 0.9f + n * 0.08f);
        if (n >= SCHROBBEN) {
            d.putInt(STAP, GESCHUIMD);
            BlockPos tobbe = BlockPos.of(d.getLongOr(TOBBE, 0L));
            zetVulling(level, tobbe, KnuffelbadBlocks.Vulling.SCHUIM);
            player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.was.geschuimd").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.was.schrobben", n, SCHROBBEN).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // --- 3. rinse: the tub's shower ------------------------------------------------------------------------------------------------

    /** The shower of a wash tub (right-click it). */
    public static void douche(ServerPlayer player, BlockPos tobbe) {
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.SPLASH, tobbe.getX() + 0.5, tobbe.getY() + 1.2, tobbe.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(ParticleTypes.FALLING_WATER, tobbe.getX() + 0.5, tobbe.getY() + 1.8, tobbe.getZ() + 0.5, 12, 0.25, 0.1, 0.25, 0);
        level.playSound(null, tobbe, KnuffelbadFeature.SPETTER.get(), SoundSource.BLOCKS, 0.7f, 1.2f);
        GuhEntity guh = gastIn(level, tobbe);
        if (guh == null) {
            return;
        }
        if (!wasser(player, guh)) {
            return;
        }
        if (stap(guh) != GESCHUIMD) {
            hint(player, guh);
            return;
        }
        CompoundTag d = guh.getPersistentData();
        d.putInt(STAP, GESPOELD);
        d.putLong(TIJD, level.getGameTime());
        WASSERS.put(player.getUUID(), level.getGameTime());
        zetVulling(level, tobbe, KnuffelbadBlocks.Vulling.WATER);
        level.sendParticles(ParticleTypes.SPLASH, guh.getX(), guh.getY() + 0.8, guh.getZ(), 40, 0.4, 0.3, 0.4, 0.1);
        player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.was.gespoeld").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // --- 4. blow-dry --------------------------------------------------------------------------------------------------------------

    /** A tick of the föhn aimed at a guh. */
    public static void fohn(ServerPlayer player, GuhEntity guh) {
        ServerLevel level = player.level();
        int stap = stap(guh);
        if (stap == 0) {
            if (level.getGameTime() % 10 == 0) {
                level.sendParticles(ParticleTypes.WHITE_ASH, guh.getX(), guh.getY() + 0.7, guh.getZ(), 3, 0.3, 0.2, 0.3, 0.01);
            }
            return;
        }
        if (!wasser(player, guh)) {
            return;
        }
        if (stap != GESPOELD) {
            if (level.getGameTime() % 30 == 0) {
                hint(player, guh);
            }
            return;
        }
        CompoundTag d = guh.getPersistentData();
        int n = d.getIntOr(GEFOHND, 0) + 1;
        d.putInt(GEFOHND, n);
        d.putLong(TIJD, level.getGameTime());
        WASSERS.put(player.getUUID(), level.getGameTime());
        if (n % 4 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, guh.getX(), guh.getY() + 0.6, guh.getZ(), 2, 0.3, 0.25, 0.3, 0.01);
            level.sendParticles(KnuffelbadFeature.GLINSTERING.get(), guh.getX(), guh.getY() + 0.8, guh.getZ(), 2, 0.4, 0.3, 0.4, 0.01);
        }
        if (n % 10 == 0) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.was.fohnen", Math.min(100, n * 100 / FOHN_TICKS)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (n >= FOHN_TICKS) {
            klaar(player, guh);
        }
    }

    /** Dry, fluffy and shiny: GLANZEND for a day. */
    static void klaar(ServerPlayer player, GuhEntity guh) {
        ServerLevel level = player.level();
        CompoundTag d = guh.getPersistentData();
        BlockPos tobbe = BlockPos.of(d.getLongOr(TOBBE, 0L));
        boolean zat = d.getBooleanOr(ZAT, false);
        vergeet(guh);
        zetVulling(level, tobbe, KnuffelbadBlocks.Vulling.LEEG);
        guh.setOrderedToSit(zat);
        glans(guh, level.getGameTime());
        guh.snapTo(tobbe.getX() + 0.5, tobbe.getY() + 1.0, tobbe.getZ() + 0.5, guh.getYRot(), 0);
        guh.emotes.start(Emote.VAHOEG, false, GuhEmotes.Source.SELF);
        level.sendParticles(KnuffelbadFeature.GLINSTERING.get(), guh.getX(), guh.getY() + 0.7, guh.getZ(), 40, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, guh.blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.3f);
        WASSERS.remove(player.getUUID());
        Minigames.give(player, new ItemStack(KnuffelbadFeature.EENDJESMUNT.get(), WAS_MUNTEN));
        KnusVoortgang.tel(player, KnuffelbadVoortgang.WASSEN, 1);
        GuhAdvancements.grant(player, "knuffelbad_gewassen");
        KnuffelbadVoortgang.toon(player, "knuffelbad_gewassen");
        player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.was.klaar", guh.getDisplayName()).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("gui.guhs.knuffelbad.was.klaar.chat", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Makes a guh shiny from now on for {@value #GLANS_DUUR} ticks (a Minecraft day). */
    public static void glans(GuhEntity guh, long nu) {
        guh.getPersistentData().putLong(GLANS_TOT, nu + GLANS_DUUR);
        GuhHooks.zet(guh, GuhHooks.GLANZEND, true);
    }

    // --- every guh tick ---------------------------------------------------------------------------------------------------------

    private static void tick(GuhEntity guh) {
        if (((guh.tickCount + guh.getId()) & 31) == 0 && GuhHooks.heeft(guh, GuhHooks.GLANZEND)
                && guh.level().getGameTime() >= guh.getPersistentData().getLongOr(GLANS_TOT, 0L)) {
            GuhHooks.zet(guh, GuhHooks.GLANZEND, false);        // a day has gone: just a normal (lovely) guh again
        }
        int stap = stap(guh);
        if (stap == 0) {
            return;
        }
        CompoundTag d = guh.getPersistentData();
        ServerLevel level = (ServerLevel) guh.level();
        BlockPos tobbe = BlockPos.of(d.getLongOr(TOBBE, 0L));
        if (!level.getBlockState(tobbe).is(KnuffelbadFeature.GUH_WASTOBBE.get()) || level.getGameTime() - d.getLongOr(TIJD, 0L) > VERGEET) {
            stop(guh);
            return;
        }
        // it stays in the tub (bathing is serious business)
        if (guh.position().distanceToSqr(tobbe.getX() + 0.5, tobbe.getY() + 0.15, tobbe.getZ() + 0.5) > 0.5) {
            inTobbe(guh, tobbe);
        }
        if (stap == GESCHUIMD && (guh.tickCount & 7) == 0) {
            level.sendParticles(KnuffelbadFeature.ZEEPBELLETJE.get(), guh.getX(), guh.getY() + 0.9, guh.getZ(), 1, 0.3, 0.2, 0.3, 0.01);
        }
    }

    /** The wash is off (the tub is gone, or nobody came back): the guh hops out. */
    public static void stop(GuhEntity guh) {
        CompoundTag d = guh.getPersistentData();
        if (d.getIntOr(STAP, 0) == 0) {
            return;
        }
        boolean zat = d.getBooleanOr(ZAT, false);
        BlockPos tobbe = BlockPos.of(d.getLongOr(TOBBE, 0L));
        if (d.read(WASSER, UUIDUtil.CODEC).isPresent()) {
            WASSERS.remove(d.read(WASSER, UUIDUtil.CODEC).orElseThrow());
        }
        vergeet(guh);
        guh.setOrderedToSit(zat);
        if (guh.level() instanceof ServerLevel level) {
            zetVulling(level, tobbe, KnuffelbadBlocks.Vulling.LEEG);
        }
        GuhHooks.bezig(guh, 0);
    }

    private static void vergeet(GuhEntity guh) {
        CompoundTag d = guh.getPersistentData();
        for (String k : new String[]{STAP, WASSER, TOBBE, TIJD, SCHROB, GEFOHND, ZAT}) {
            d.remove(k);
        }
        GuhHooks.bezig(guh, 0);
    }

    private static boolean wasser(ServerPlayer player, GuhEntity guh) {
        CompoundTag d = guh.getPersistentData();
        if (d.read(WASSER, UUIDUtil.CODEC).isPresent() && !d.read(WASSER, UUIDUtil.CODEC).orElseThrow().equals(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.was.iemand_anders").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        return true;
    }

    /** What to do next (a friendly hint in the right order). */
    static void hint(ServerPlayer player, GuhEntity guh) {
        String key = switch (stap(guh)) {
            case INGEZEEPT -> "gui.guhs.knuffelbad.was.hint.schuimen";
            case GESCHUIMD -> "gui.guhs.knuffelbad.was.hint.spoelen";
            case GESPOELD -> "gui.guhs.knuffelbad.was.hint.fohnen";
            default -> "gui.guhs.knuffelbad.was.hint.inzepen";
        };
        player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private Wasritueel() {
    }
}
