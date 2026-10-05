package nl.juiced.guhs.feature.vadskracht;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * The hover readout (there is no meter block): what you read when you look at any block that makes, carries or uses
 * vadskracht. Made on the server as translatable lines ({@code gui.guhs.vadskracht.*}, tools/features/vadskracht.py), sent by
 * {@link VadsPayloads} and drawn under the crosshair by {@code client.VadsHover}:
 * <ol>
 *   <li>"Deze opstelling gebruikt 16/20 vadskracht" (what the machines ask / what the sources give);</li>
 *   <li>when it stands still, why: too heavy (and how much is missing), no source, too big;</li>
 *   <li>the batteries of the net, and how long they last when the net runs on them;</li>
 *   <li>the block itself: what it gives (and "telt niet mee" for one source too many), asks or holds;</li>
 *   <li>the block's own lines ({@link VadsKnoop#vadsRegels}).</li>
 * </ol>
 */
public final class VadsUitlezing {
    private static final String K = "gui.guhs.vadskracht.";

    public static List<Component> regels(ServerLevel level, BlockPos pos) {
        VadsNet net = VadsKracht.net(level, pos);
        List<Component> uit = new ArrayList<>();
        if (net == VadsNet.EMPTY) {
            return uit;
        }
        VadsKnoop knoop = VadsKracht.knoop(level, pos);
        uit.add(Component.translatable(K + "gebruik", net.vraag(), net.aanbod())
                .withStyle(net.draait() ? ChatFormatting.WHITE : ChatFormatting.RED));
        switch (net.status()) {
            case TE_ZWAAR -> uit.add(Component.translatable(K + "stil.te_zwaar", net.tekort()).withStyle(ChatFormatting.GOLD));
            case GEEN_BRON -> uit.add(Component.translatable(K + "stil.geen_bron").withStyle(ChatFormatting.GOLD));
            case TE_GROOT -> uit.add(Component.translatable(K + "stil.te_groot", VadsGetallen.MAX_NET).withStyle(ChatFormatting.GOLD));
            default -> {
            }
        }
        if (net.bufferMax() > 0) {
            if (net.draait() && net.tekort() > 0) {
                long seconden = net.buffer() / net.tekort();
                uit.add(Component.translatable(K + "op_batterij", net.tekort(), tijd(seconden)).withStyle(ChatFormatting.YELLOW));
            }
            if (!(knoop instanceof VadsOpslag)) {
                uit.add(Component.translatable(K + "buffer", net.buffer(), net.bufferMax()).withStyle(ChatFormatting.AQUA));
            }
        }
        if (knoop instanceof VadsBron bron) {
            uit.add(Component.translatable(K + "geeft", bron.vadsAanbod()).withStyle(ChatFormatting.GREEN));
            if (!net.teltMee(bron) && bron.vadsSoort() != null) {
                uit.add(Component.translatable(K + "telt_niet_mee", bron.vadsSoort().max, bron.vadsSoort().naam()).withStyle(ChatFormatting.GOLD));
            }
        }
        if (knoop instanceof VadsVerbruiker verbruiker) {
            int vraag = verbruiker.vadsVraag();
            uit.add(vraag > 0 ? Component.translatable(K + "verbruikt", vraag).withStyle(ChatFormatting.LIGHT_PURPLE)
                    : Component.translatable(K + "staat_uit").withStyle(ChatFormatting.GRAY));
        }
        if (knoop instanceof VadsOpslag opslag) {
            uit.add(Component.translatable(K + "batterij", opslag.vadsInhoud(), opslag.vadsMax()).withStyle(ChatFormatting.AQUA));
        }
        if (knoop != null) {
            knoop.vadsRegels(uit::add);
        }
        return uit;
    }

    /** "12 s" / "7 min" / "2 uur": how long a battery lasts. */
    private static Component tijd(long seconden) {
        if (seconden < 120) {
            return Component.translatable(K + "tijd.s", seconden);
        }
        if (seconden < 2 * 3600) {
            return Component.translatable(K + "tijd.min", seconden / 60);
        }
        return Component.translatable(K + "tijd.uur", seconden / 3600);
    }

    private VadsUitlezing() {
    }
}
