package nl.juiced.guhs.feature.bibliotheek;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The reading room of the guh library: every guh book lies open on a lectern (the secret one on the book stand in the
 * secret room). A right-click opens the book right away (no lending, no waiting); of every book each player may take one
 * copy home, once. The lecterns themselves stay full: the book on them never leaves.
 */
public final class Leeszaal {
    /** Saved with the player: the books they have opened on a lectern / the books they took a copy of. */
    static final String READ = "guhs_bieb_gelezen", TAKEN = "guhs_bieb_genomen";
    /** How close you must stand to take the copy. */
    static final double REACH_SQR = 8 * 8;

    private Leeszaal() {
    }

    public static int read(ServerPlayer player) {
        return GuhQuests.saved(player).getInt(READ);
    }

    public static boolean taken(ServerPlayer player, Guhboek book) {
        return (GuhQuests.saved(player).getInt(TAKEN) & book.bit()) != 0;
    }

    /** Which guh book lies here for reading: a library lectern with a guh book on it, or the secret book stand. */
    @Nullable
    public static Guhboek bookAt(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(BibliotheekFeature.BOEKALTAAR.get())) {
            return Guhboek.GEHEIM;
        }
        if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern && BibliotheekProtection.inLibrary(level, pos)) {
            return Guhboek.of(lectern.getBook());
        }
        return null;
    }

    /** Opens the book on this lectern for the player (and remembers they have read it). */
    public static void open(ServerPlayer player, BlockPos pos, Guhboek book) {
        CompoundTag saved = GuhQuests.saved(player);
        saved.putInt(READ, saved.getInt(READ) | book.bit());
        player.serverLevel().playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1f);
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new BibliotheekPayloads.OpenBook(pos, book.ordinal(), !taken(player, book)));
    }

    /** The "take a copy" button: one copy per book per player, ever. */
    public static void take(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQR || !player.level().isLoaded(pos)) {
            return;                              // (first: the position comes from the client, don't load far-away chunks for it)
        }
        Guhboek book = bookAt(player.level(), pos);
        if (book == null) {
            return;
        }
        if (taken(player, book)) {
            player.displayClientMessage(Component.translatable("quest.guhs.bieb.taken_already", book.title())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        CompoundTag saved = GuhQuests.saved(player);
        saved.putInt(TAKEN, saved.getInt(TAKEN) | book.bit());
        saved.putInt(READ, saved.getInt(READ) | book.bit());
        Bibliothecaris.give(player, book.stack());
        ServerLevel level = player.serverLevel();
        if (book.secret()) {
            player.sendSystemMessage(Component.translatable("quest.guhs.bieb.altar").withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, 1.2f);
            level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.6);
        } else {
            player.displayClientMessage(Component.translatable("quest.guhs.bieb.taken", book.title()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 0.8f);
        }
        Bibliothecaris.collect(player);
    }
}
