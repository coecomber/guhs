package nl.juiced.guhs.feature.techbezorg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The Bezorgguhtje-fluitje (bbq2; the reward of the torenwachter-guh of the Rookguh-vuurtoren, slice toren-peper: no
 * recipe). Blow it and the nearest Bezorgguhtje of one of YOUR Stepstations within {@link Bezorgnet#BEREIK} blocks breaks
 * off its round and rides to you; when it arrives its backpack opens for you ({@link StepstationBlockEntity#roep}). Blow it
 * while sneaking and all your guhtjes within reach ride home and start their round again
 * ({@link StepstationBlockEntity#naarHuis}). Not used up; a short cooldown.
 */
public class FluitjeItem extends Item {
    public static final int WACHT = 40;

    public FluitjeItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            fluit(sp, player.isSecondaryUseActive());
            player.getCooldowns().addCooldown(player.getItemInHand(hand), WACHT);
        }
        return InteractionResult.SUCCESS;
    }

    /** The Stepstations this player placed that are loaded and within reach, the one whose guhtje is nearest first. */
    public static List<StepstationBlockEntity> mijnStations(ServerPlayer speler) {
        List<StepstationBlockEntity> mijn = new ArrayList<>();
        for (StepstationBlockEntity station : Bezorgnet.stations(speler.level(), speler.blockPosition())) {
            if (speler.getUUID().equals(station.eigenaar())) {
                mijn.add(station);
            }
        }
        mijn.sort(Comparator.comparingDouble(station -> {
            BezorgguhtjeEntity k = station.koerier();
            return k == null ? Double.MAX_VALUE : k.distanceToSqr(speler);
        }));
        return mijn;
    }

    /**
     * Blows the whistle for this player. Returns how many guhtjes listen: 1 when one comes ({@code naarHuis} false), the
     * number sent home ({@code naarHuis} true), 0 when there is none (or it sleeps).
     */
    public static int fluit(ServerPlayer speler, boolean naarHuis) {
        ServerLevel sl = speler.level();
        sl.playSound(null, speler.blockPosition(), TechbezorgFeature.FLUITJE.get(), SoundSource.PLAYERS, 1f, 1f);
        List<StepstationBlockEntity> mijn = mijnStations(speler);
        if (naarHuis) {
            for (StepstationBlockEntity station : mijn) {
                station.naarHuis();
            }
            speler.sendOverlayMessage(mijn.isEmpty() ? Component.translatable("gui.guhs.techbezorg.fluit.niemand").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("gui.guhs.techbezorg.fluit.naar_huis", mijn.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return mijn.size();
        }
        for (StepstationBlockEntity station : mijn) {
            if (station.roep(speler)) {
                speler.sendOverlayMessage(Component.translatable("gui.guhs.techbezorg.fluit.komt").withStyle(ChatFormatting.LIGHT_PURPLE));
                GuhAdvancements.grant(speler, "tech_bezorg_fluitje");
                GidsFeature.grant(speler, "techniek/tech_bezorg_fluitje");
                return 1;
            }
        }
        speler.sendOverlayMessage(Component.translatable(mijn.isEmpty() ? "gui.guhs.techbezorg.fluit.niemand" : "gui.guhs.techbezorg.fluit.slaapt")
                .withStyle(ChatFormatting.GRAY));
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.bezorgguhtje_fluitje.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.bezorgguhtje_fluitje.lore.sluip").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
