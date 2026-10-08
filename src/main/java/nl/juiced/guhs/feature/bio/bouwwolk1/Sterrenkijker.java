package nl.juiced.guhs.feature.bio.bouwwolk1;

import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;
import nl.juiced.guhs.feature.sterrenwacht.Sterrenkijken;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The sterrenkijkerguh of the Sterrenwacht-ruine (NPC STERRENWACHT_RUINE_STERRENKIJKER, in Professor Sterretje's
 * clothes). He only wakes when it is dark ({@link #wakker}: the same rule as the telescope's): then he talks about the
 * stars, dreamily, a line each time. By day he sleeps in his chair and only mumbles.
 * <p>
 * The treat: looking through the telescope beside him in the dark ({@link #kijk}; the telescope's own star game opens
 * as always) leaves a little sterrenstof in your hand, once per night per player. It is the sterrenstof falling stars
 * leave (feature evenementen); two decorative recipes use it (the sterrenkaart, a sterrenlantaarn).
 * <p>
 * Per player (GuhQuests.saved): {@value #NACHT} = the night of the last treat + 1, {@value #GESPROKEN} = how often spoken.
 */
public final class Sterrenkijker implements NpcRole {
    public static final String NACHT = "guhs_bio_bouw_wolk1_stof_nacht", GESPROKEN = "guhs_bio_bouw_wolk1_sterren_gesproken";
    public static final int REGELS = 5;
    /** The telescope counts as his when he sits within this many blocks of it. */
    public static final int DICHTBIJ = 12;
    private static final String Q = "quest.guhs.sterrenwacht_ruine.";
    /** Is it dark enough for stars? (Tests put their own rule here and put this one back.) */
    public static Predicate<Level> donker = Sterrenkijken::donker;

    public static boolean wakker(Level level) {
        return donker.test(level);
    }

    public static Component naam() {
        return Component.translatable("entity.guhs.guh_npc.sterrenwacht_ruine_sterrenkijker");
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer speler) {
        Bewijs.geef(speler, Bewijs.RUINE_GEVONDEN);
        if (!wakker(npc.level())) {
            npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.35f, 0.6f);
            GuhQuests.say(speler, npc, Q + "slaapt");
            speler.sendOverlayMessage(Component.translatable("gui.guhs.sterrenwacht_ruine.slaapt").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.8f, 0.85f);
        Praat.open(speler, npc, null, regel(speler), new Object[0]);
    }

    /** The line of this visit: the greeting first, then the five dreamy ones in turn. */
    static String regel(ServerPlayer speler) {
        CompoundTag d = GuhQuests.saved(speler);
        int keer = d.getIntOr(GESPROKEN, 0);
        d.putInt(GESPROKEN, keer + 1);
        return Q + (keer == 0 ? "hallo" : "nacht" + Math.floorMod(keer - 1, REGELS));
    }

    /**
     * The player looks through the telescope at {@code telescoop}: in the dark, with a sterrenkijkerguh of the ruin
     * close by, and not yet this night: a little sterrenstof. Returns whether it was given.
     */
    public static boolean kijk(ServerPlayer speler, BlockPos telescoop) {
        ServerLevel level = speler.level();
        if (!wakker(level) || level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(telescoop).inflate(DICHTBIJ),
                n -> n.getKind() == GuhNpcEntity.Kind.STERRENWACHT_RUINE_STERRENKIJKER).isEmpty()) {
            return false;
        }
        return geefStof(speler, telescoop, Sterrenkijken.nacht(level));
    }

    /** The rule itself: once per night number per player. */
    static boolean geefStof(ServerPlayer speler, BlockPos telescoop, long nacht) {
        CompoundTag d = GuhQuests.saved(speler);
        if (d.getLongOr(NACHT, Long.MIN_VALUE) == nacht + 1) {
            return false;
        }
        d.putLong(NACHT, nacht + 1);
        ItemStack stof = new ItemStack(Bio.item("sterrenstof", Items.GLOWSTONE_DUST));
        if (!speler.getInventory().add(stof)) {
            speler.drop(stof, false);
        }
        Buiten.zeg(speler, naam(), Q + "stof");
        ServerLevel level = speler.level();
        level.sendParticles(ParticleTypes.END_ROD, telescoop.getX() + 0.5, telescoop.getY() + 1.4, telescoop.getZ() + 0.5, 14, 0.4, 0.5, 0.4, 0.03);
        level.playSound(null, telescoop, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, 1.5f);
        Bewijs.geef(speler, Bewijs.RUINE_GEVONDEN);
        Bewijs.geef(speler, Bewijs.RUINE_STERRENSTOF);
        return true;
    }
}
