package nl.juiced.guhs.feature.knuffeldal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Seizoen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The seasons in the Knuffeldal (2.8): what you can do in each season, and the seizoensplakboek (two entries per
 * season) on the Knus tab.
 * <ul>
 *   <li><b>Lente</b>: 3 flowers on a blooming seizoensbloembak: a bloesemkransje (put it on your guh in spring).</li>
 *   <li><b>Zomer</b>: 3 wheat on the sunny bloembak: a straw zonnehoedje (for your guh in summer).</li>
 *   <li><b>Herfst</b>: the bladerhoopjes are big: jump in (your tamed guhs jump in with you)! Leaves on the bloembak: more
 *       bladerhoopjes to take home.</li>
 *   <li><b>Winter</b>: wool on the snowy bloembak: a knus sjaaltje; snowballs: a sneeuwguhkopje; two snow blocks with a
 *       sneeuwguhkopje on top: a sneeuwpopguh.</li>
 * </ul>
 * And when the season changes, everyone in the Guhmensie hears it (a chime and a message).
 */
public final class Seizoensactiviteiten {
    /** Falling at least this far into a full bladerhoopje counts as jumping in. */
    public static final float SPRONG = 1.5f;
    /** How many of the ingredient go into one seasonal thing. */
    public static final int NODIG = 3;

    private Seizoensactiviteiten() {
    }

    static void register() {
        Seizoen.bijWissel((server, oud, nieuw) -> {
            if (oud == null) {
                return;   // (the server just started: no announcement)
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.level().dimension() == ModDimensions.GUHMENSION) {
                    player.sendSystemMessage(Component.translatable("gui.guhs.seizoen.wissel." + nieuw.id()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    player.playNotifySound(KnuffeldalFeature.SEIZOEN_GELUID.get(), SoundSource.AMBIENT, 1f, 1f);
                }
            }
        });
    }

    /** A thing used on a seizoensbloembak: the season's craft. True when something was made. */
    public static boolean bloembak(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack stack) {
        Seizoen seizoen = Seizoen.huidig(level);
        ItemStack result = ItemStack.EMPTY;
        int cost = NODIG;
        String entry = null;
        switch (seizoen) {
            case LENTE -> {
                if (stack.is(ItemTags.SMALL_FLOWERS)) {
                    result = new ItemStack(ModItems.clothingItem(GuhClothes.BLOESEMKRANSJE));
                    entry = "lente_kransje";
                }
            }
            case ZOMER -> {
                if (stack.is(Items.WHEAT)) {
                    result = new ItemStack(ModItems.clothingItem(GuhClothes.ZONNEHOEDJE));
                    entry = "zomer_hoedje";
                }
            }
            case HERFST -> {
                if (stack.is(ItemTags.LEAVES)) {
                    result = new ItemStack(KnuffeldalFeature.BLADERHOOPJE.get(), 2);
                    cost = 4;
                }
            }
            case WINTER -> {
                if (stack.is(ItemTags.WOOL)) {
                    result = new ItemStack(ModItems.clothingItem(GuhClothes.KNUS_SJAALTJE));
                } else if (stack.is(Items.SNOWBALL)) {
                    result = new ItemStack(KnuffeldalFeature.SNEEUWGUHKOPJE.get());
                }
            }
        }
        if (result.isEmpty()) {
            return false;
        }
        if (stack.getCount() < cost) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.seizoen.meer_nodig", cost, stack.getHoverName()).withStyle(ChatFormatting.GOLD));
            return true;
        }
        stack.consume(cost, player);
        Minigames.give(player, result);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.3f);
        level.sendParticles(seizoen == Seizoen.WINTER ? KnuffeldalFeature.SNEEUWVLOKJE.get() : seizoen == Seizoen.LENTE
                ? KnuffeldalFeature.BLOESEMBLAADJE.get() : ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 14, 0.4, 0.3, 0.4, 0.02);
        player.sendOverlayMessage(Component.translatable("gui.guhs.seizoen.gemaakt", result.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (entry != null) {
            plakboek(player, entry);
        }
        return true;
    }

    /** Right-clicked with an empty hand (or the wrong thing): what you can make here this season. */
    public static void bloembakTip(ServerPlayer player, Seizoen seizoen) {
        player.sendOverlayMessage(Component.translatable("gui.guhs.seizoen.tip." + seizoen.id()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Someone jumped into a full bladerhoopje: leaves everywhere! For a player: it counts, and their tamed guhs jump in too. */
    public static void gesprongen(ServerLevel level, BlockPos pos, Entity entity) {
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, KnuffeldalFeature.BLADERHOOPJE.get()
                .defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 40, 0.5, 0.4, 0.5, 0.15);
        level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1.2f, 0.9f);
        level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1f, 1.2f);
        if (!(entity instanceof ServerPlayer player)) {
            if (entity instanceof GuhEntity guh) {
                GuhEmotes emotes = guh.emotes;
                emotes.start(Emote.ROLLEN, false, GuhEmotes.Source.SELF);
            }
            return;
        }
        KnusVoortgang.tel(player, KnuffeldalVoortgang.BLADERHOOPJES, 1);
        plakboek(player, "herfst_hoopje");
        boolean guhMee = false;
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(8), g -> g.isOwnedBy(player) && !g.isOrderedToSit())) {
            guh.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.2);
            guh.emotes.start(Emote.ROLLEN, false, GuhEmotes.Source.SELF);
            guhMee = true;
        }
        if (guhMee) {
            plakboek(player, "herfst_guh");
        }
    }

    /** A sneeuwpopguh was built (in winter it goes in the plakboek). */
    public static void sneeuwpop(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (Seizoen.huidig(level) == Seizoen.WINTER) {
            plakboek(player, "winter_sneeuwpop");
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.seizoen.sneeuwpop").withStyle(ChatFormatting.AQUA));
    }

    /** Every 5 seconds, per tamed guh: wearing the season's piece in its season counts for its owner (who must be near). */
    static void guhTick(GuhEntity guh) {
        if (!(guh.getOwner() instanceof ServerPlayer owner) || owner.level() != guh.level() || owner.distanceTo(guh) > 32) {
            return;
        }
        Seizoen seizoen = Seizoen.huidig(guh.level());
        String entry = switch (seizoen) {
            case LENTE -> guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.BLOESEMKRANSJE ? "lente_guh" : null;
            case ZOMER -> guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.ZONNEHOEDJE ? "zomer_guh" : null;
            case WINTER -> guh.getClothes(GuhClothes.Slot.NECK) == GuhClothes.KNUS_SJAALTJE ? "winter_guh" : null;
            default -> null;
        };
        if (entry != null) {
            plakboek(owner, entry);
        }
    }

    /**
     * Fills in a seizoensplakboek entry; a new one counts for its season (2 per season), and a season with both counts
     * for "alle seizoenen".
     */
    public static void plakboek(ServerPlayer player, String entry) {
        if (!KnusVoortgang.ontdek(player, KnuffeldalVoortgang.PLAKBOEK, entry)) {
            return;
        }
        String seizoen = entry.substring(0, entry.indexOf('_'));
        int now = KnusVoortgang.tel(player, "seizoenen." + seizoen, 1);
        if (now == 2) {
            int alle = KnusVoortgang.tel(player, KnuffeldalVoortgang.SEIZOEN_ALLE, 1);
            if (alle >= 4) {
                GuhAdvancements.grant(player, "knuffeldal_seizoen_alle");
                KnuffeldalAdvancements.toon(player, "seizoen_alle");
            }
        }
    }
}
