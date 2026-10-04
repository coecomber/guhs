package nl.juiced.guhs.feature.knuffeldal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.knus.Seizoen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * The feestbuffet (2.8): the feestbuffettafels of the town are laid for a Knusfeest ({@link KnusfeestEvenement}), you
 * can put things you baked, grew or poured on them, all your tamed guhs come to eat, and afterwards the rewards:
 * after the Grote Knusfeest the knus_oorkonde, the burgemeesterssjerp and the title Knuffelburgemeester; after a
 * seasonal feast a treat of the season.
 */
public final class Feestbuffet {
    /** GuhQuests.saved flag: the player is Knuffelburgemeester. */
    public static final String TITEL = "guhs_knuffelburgemeester";
    /** Tables this close to the Burgemeester (or the event's middle) are laid. */
    public static final int RADIUS = 28;

    /**
     * Game tests: the tests' own areas. A scan from a spot inside one only counts the tables in it (the radius reaches into
     * the neighbouring tests, and structures of earlier batches stay in the world).
     */
    public static final List<net.minecraft.world.phys.AABB> TEST_GRENZEN = new java.util.concurrent.CopyOnWriteArrayList<>();

    private Feestbuffet() {
    }

    /** The feestbuffettafels around a spot (a box of radius x 8 x radius). */
    public static List<BlockPos> tafels(ServerLevel level, BlockPos centre, int radius) {
        List<BlockPos> out = new ArrayList<>();
        net.minecraft.world.phys.AABB grens = TEST_GRENZEN.stream().filter(b -> b.contains(net.minecraft.world.phys.Vec3.atCenterOf(centre)))
                .findFirst().orElse(null);
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-radius, -4, -radius), centre.offset(radius, 4, radius))) {
            if (grens != null && !grens.contains(net.minecraft.world.phys.Vec3.atCenterOf(p))) {
                continue;
            }
            if (level.isLoaded(p) && level.getBlockState(p).is(KnuffeldalFeature.FEESTBUFFETTAFEL.get())) {
                out.add(p.immutable());
            }
        }
        return out;
    }

    /** Lays (or clears) the tables. */
    public static void dek(ServerLevel level, List<BlockPos> tafels, boolean gedekt) {
        for (BlockPos p : tafels) {
            BlockState state = level.getBlockState(p);
            if (state.is(KnuffeldalFeature.FEESTBUFFETTAFEL.get()) && state.getValue(KnuffeldalBlocks.Feestbuffettafel.GEDEKT) != gedekt) {
                level.setBlock(p, state.setValue(KnuffeldalBlocks.Feestbuffettafel.GEDEKT, gedekt), Block.UPDATE_ALL);
                if (gedekt) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 8, 0.4, 0.2, 0.4, 0.02);
                }
            }
        }
    }

    /** Buffet food: things you baked, grew or poured (#guhs:knus/gebak, oogst, thee, kaasmelk), and kaasknabbels. */
    public static boolean buffetEten(ItemStack stack) {
        if (Feesttaak.isFeestItem(stack)) {
            return false;   // (the feesttaart & co are for the Burgemeester: never eaten by accident, 1.2.7)
        }
        return stack.is(KnusTags.GEBAK) || stack.is(KnusTags.OOGST) || stack.is(KnusTags.THEE) || stack.is(KnusTags.KAASMELK)
                || stack.is(ModItems.KAAS_KNABBELS.get()) || stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get());
    }

    /** Something put on a laid table: it goes on the buffet (true), or nothing happens (false). */
    public static boolean opTafel(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        if (!state.getValue(KnuffeldalBlocks.Feestbuffettafel.GEDEKT) || !buffetEten(stack)) {
            return false;
        }
        boolean zelfgemaakt = stack.is(KnusTags.GEBAK) || stack.is(KnusTags.OOGST) || stack.is(KnusTags.THEE) || stack.is(KnusTags.KAASMELK);
        Component naam = stack.getHoverName();
        stack.consume(1, player);
        level.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 1.2f);
        player.sendOverlayMessage(Component.translatable(zelfgemaakt ? "gui.guhs.knuffeldal.buffet.zelfgemaakt" : "gui.guhs.knuffeldal.buffet.erop", naam)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        KnusVoortgang.tel(player, "knuffeldal.buffet", zelfgemaakt ? 2 : 1);
        return true;
    }

    /**
     * The feast of this player's round is over (or couldn't be held): the rewards, and the round is done. Nothing when
     * the round isn't complete (e.g. an op started a Knusfeest by command).
     */
    public static void gevierd(ServerPlayer player, boolean metFeest) {
        if (!Knusfeest.rondeBezig(player) || !Knusfeest.alleGebracht(player)) {
            return;
        }
        long ronde = Knusfeest.ronde(player);
        Knusfeest.rondeGevierd(player);
        if (ronde == 0) {
            Knusfeest.markeerSeizoensfeest(player, Seizoen.nummer(player.level()) + 1);   // (the seasonal feasts start next season)
            // the Grote Knusfeest: the trophy, the sash and the title
            Minigames.give(player, new ItemStack(KnuffeldalFeature.KNUS_OORKONDE.get()));
            Minigames.give(player, new ItemStack(ModItems.clothingItem(GuhClothes.BURGEMEESTERSSJERP)));
            GuhQuests.saved(player).putBoolean(TITEL, true);
            nl.juiced.guhs.feature.titels.Titels.ververs(player);
            KnusVoortgang.hoogste(player, KnuffeldalVoortgang.FINALE, 1);
            GuhAdvancements.grant(player, "knuffeldal_finale");
            KnuffeldalAdvancements.toon(player, "knusfeest_klaar");
            KnuffeldalAdvancements.toon(player, "knuffelburgemeester");
            player.level().getServer().getPlayerList().broadcastSystemMessage(Component.translatable("gui.guhs.knuffeldal.titel_gekregen", player.getName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        } else {
            // a seasonal feast: a treat of the season
            Minigames.give(player, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
            Minigames.give(player, switch (Seizoen.van(ronde - 1)) {
                case LENTE -> new ItemStack(ModItems.clothingItem(GuhClothes.BLOESEMKRANSJE));
                case ZOMER -> new ItemStack(ModItems.clothingItem(GuhClothes.ZONNEHOEDJE));
                case HERFST -> new ItemStack(KnuffeldalFeature.BLADERHOOPJE.get(), 4);
                case WINTER -> new ItemStack(ModItems.clothingItem(GuhClothes.KNUS_SJAALTJE));
            });
            KnusVoortgang.tel(player, KnuffeldalVoortgang.SEIZOENSFEESTEN, 1);
            GuhAdvancements.grant(player, "knuffeldal_seizoensfeest");
            player.sendSystemMessage(Component.translatable("gui.guhs.knuffeldal.seizoensfeest_klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.2f);
    }
}
