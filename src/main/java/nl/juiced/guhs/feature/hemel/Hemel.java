package nl.juiced.guhs.feature.hemel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.Wolkjes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * What the Knuffelhart does (server side): the revive screen ({@link #openScherm}: all your dead tamed guhs, from
 * {@link Wolkjes#dood}), bringing one back ({@link #terug}: {@link Wolkjes#terug} + a celebration + a minute of glans), and a
 * {@link Herinnering} star brought to the heart ({@link #ster}). Free and unlimited once the heart beats for you
 * ({@link HemelQuest#klopt}); only your own guhs, only standing near the heart.
 */
public final class Hemel {
    /** How close you must stand to the heart. */
    public static final double BEREIK = 8.0;
    /** How long a guh sparkles after coming back (ticks). */
    public static final int GLANS_TICKS = 1200;
    public static final String GLANS_TOT = "guhs_hemel_glans_tot";

    /** Which heart each player has the screen of open (a Terug must come from that one). */
    private static final Map<UUID, BlockPos> OPEN = new ConcurrentHashMap<>();

    /** Is this the Knuffelhart, close enough to the player? */
    public static boolean bijHetHart(ServerPlayer p, BlockPos hart) {
        return p.level().getBlockState(hart).is(HemelFeature.KNUFFELHART.get())
                && p.position().distanceToSqr(Vec3.atCenterOf(hart)) <= BEREIK * BEREIK;
    }

    /** The screen's data: the heart, today, and every dead tamed guh of the player (newest first). */
    public static CompoundTag data(ServerPlayer p, BlockPos hart, @Nullable String net) {
        CompoundTag t = new CompoundTag();
        t.putLong("Pos", hart.asLong());
        t.putLong("Dag", Band.dag(p.server));
        if (net != null) {
            t.putString("Net", net);
        }
        ListTag lijst = new ListTag();
        for (Wolkjes.DodeGuh d : Wolkjes.dood(p.server, p.getUUID())) {
            CompoundTag g = new CompoundTag();
            g.putUUID("Id", d.bandId());
            g.putString("Naam", d.naam());
            g.putString("Variant", d.variant());
            g.putInt("Hartjes", d.hartjes());
            g.putInt("Niveau", d.niveau().ordinal());
            g.put("Looks", d.looks());
            g.putLong("DoodDag", d.doodDag());
            lijst.add(g);
        }
        t.put("Guhs", lijst);
        return t;
    }

    /** Opens the revive screen at this heart (only when it beats for the player). */
    public static boolean openScherm(ServerPlayer p, BlockPos hart) {
        if (!HemelQuest.klopt(p)) {
            p.displayClientMessage(Component.translatable("gui.guhs.hemel.slaapt").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return false;
        }
        OPEN.put(p.getUUID(), hart.immutable());
        ModNetworking.sendTo(p, new HemelPayloads.Open(data(p, hart, null)));
        return true;
    }

    /** (HemelPayloads.Terug) the player picked a guh in the screen. */
    public static void kies(ServerPlayer p, BlockPos hart, UUID id) {
        BlockPos open = OPEN.get(p.getUUID());
        if (open == null || !open.equals(hart)) {
            return;
        }
        GuhEntity guh = terug(p, hart, id);
        if (guh != null) {
            ModNetworking.sendTo(p, new HemelPayloads.Open(data(p, hart, guh.getName().getString())));
        }
    }

    /**
     * Brings one of the player's dead tamed guhs back, next to them at the heart: free and unlimited, as long as the heart
     * beats for them and they stand near it. Null (with a message) when it can't.
     */
    @Nullable
    public static GuhEntity terug(ServerPlayer p, BlockPos hart, UUID id) {
        if (!HemelQuest.klopt(p)) {
            p.displayClientMessage(Component.translatable("gui.guhs.hemel.slaapt").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return null;
        }
        if (!bijHetHart(p, hart)) {
            p.displayClientMessage(Component.translatable("gui.guhs.hemel.te_ver").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return null;
        }
        ServerLevel level = (ServerLevel) p.level();
        GuhEntity guh = Wolkjes.terug(level, p, id, plek(p, hart));
        if (guh == null) {
            p.displayClientMessage(Component.translatable("gui.guhs.hemel.terug.mislukt").withStyle(ChatFormatting.GRAY), true);
            return null;
        }
        feest(level, p, hart, guh);
        return guh;
    }

    /** Where a guh comes back: between the heart and the player, on the player's floor, facing the player. */
    static Vec3 plek(ServerPlayer p, BlockPos hart) {
        Vec3 h = Vec3.atBottomCenterOf(hart);
        Vec3 naar = new Vec3(p.getX() - h.x, 0, p.getZ() - h.z);
        double d = naar.length();
        Vec3 spot = d < 1.6 ? p.position() : h.add(naar.scale(Math.max(1.4, d - 1.4) / d));
        return new Vec3(spot.x, p.getY(), spot.z);
    }

    /** The celebration: hearts, sparkles, the sound, the line, a minute of glans, the advancements. */
    static void feest(ServerLevel level, ServerPlayer p, BlockPos hart, GuhEntity guh) {
        double x = guh.getX(), y = guh.getY() + guh.getBbHeight() * 0.6, z = guh.getZ();
        level.sendParticles(ParticleTypes.HEART, x, y + 0.4, z, 10, 0.5, 0.4, 0.5, 0.05);
        level.sendParticles(HemelFeature.STERRETJE.get(), x, y, z, 36, 0.7, 0.7, 0.7, 0.05);
        level.sendParticles(ParticleTypes.CLOUD, x, guh.getY() + 0.2, z, 16, 0.6, 0.1, 0.6, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, hart.getX() + 0.5, hart.getY() + 0.7, hart.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, guh, HemelFeature.TERUG.get(), SoundSource.NEUTRAL, 1f, 1f);
        level.playSound(null, hart, HemelFeature.HARTKLOP.get(), SoundSource.BLOCKS, 1f, 1.1f);
        glans(guh, level.getGameTime());
        guh.getLookControl().setLookAt(p);
        p.sendSystemMessage(Component.translatable("gui.guhs.hemel.terug", guh.getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        HemelQuest.telTerug(p);
        GuhAdvancements.grant(p, "hemel_terug");
        GidsFeature.grant(p, "verhalen/hemel_terug");
    }

    /** A minute of sparkles (KnusVlag GLANS; HemelEvents.glans switches it off again). */
    public static void glans(GuhEntity guh, long nu) {
        GuhHooks.zet(guh, VerhaalVlaggen.GLANS, true);
        guh.getPersistentData().putLong(GLANS_TOT, nu + GLANS_TICKS);
    }

    /** What a star brought to the heart does. Returns true when it brought its guh back (the star is then used up). */
    public static boolean ster(ServerPlayer p, BlockPos hart, ItemStack stack) {
        ServerLevel level = (ServerLevel) p.level();
        level.playSound(null, hart, HemelFeature.STER.get(), SoundSource.BLOCKS, 0.8f, 1f);
        level.sendParticles(HemelFeature.STERRETJE.get(), hart.getX() + 0.5, hart.getY() + 0.7, hart.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.03);
        if (!HemelQuest.klopt(p)) {
            p.displayClientMessage(Component.translatable("gui.guhs.hemel.slaapt").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return false;
        }
        GuhAdvancements.grant(p, "hemel_ster");
        GidsFeature.grant(p, "verhalen/hemel_ster");
        CompoundTag data = Herinnering.data(stack);
        if (!data.hasUUID("Band")) {
            p.displayClientMessage(Component.translatable("gui.guhs.hemel.ster.leeg").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return false;
        }
        UUID id = data.getUUID("Band");
        String naam = data.getString("Naam");
        if (Wolkjes.isDood(p.server, p.getUUID(), id)) {
            GuhEntity guh = terug(p, hart, id);
            if (guh != null) {
                stack.shrink(1);
                return true;
            }
            return false;
        }
        BandData.Rec rec = BandData.get(p.server).vind(p.getUUID(), id);
        p.displayClientMessage(Component.translatable(rec != null ? "gui.guhs.hemel.ster.leeft" : "gui.guhs.hemel.ster.niet_van_jou", naam)
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return false;
    }

    /** (tests) the dead guhs the screen would list. */
    public static List<Wolkjes.DodeGuh> lijst(ServerPlayer p) {
        return Wolkjes.dood(p.server, p.getUUID());
    }

    static void vergeet(ServerPlayer p) {
        OPEN.remove(p.getUUID());
    }

    private Hemel() {
    }
}
