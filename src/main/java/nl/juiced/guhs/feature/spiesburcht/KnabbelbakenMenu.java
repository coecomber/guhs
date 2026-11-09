package nl.juiced.guhs.feature.spiesburcht;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * 1.9.3: right-clicking the Knabbelbaken opens a small menu (the talking screen without a speaker): switch each effect
 * this pyramid has unlocked on or off (several at once), or hide / show the beam. Which baken a player has open is kept per player.
 */
public final class KnabbelbakenMenu {
    static final String SLEUTEL = "spiesburcht_knabbelbaken";
    /** Answer ids: 0..3 = Gunst ordinal, STRAAL = hide/show the beam. */
    static final int STRAAL = 10, KLAAR = 11;
    private static final Map<UUID, BlockPos> OPEN = new ConcurrentHashMap<>();

    private KnabbelbakenMenu() {
    }

    static void init() {
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
            BlockPos pos = OPEN.get(p.getUUID());
            if (optie < 0) {
                OPEN.remove(p.getUUID());
                return;
            }
            if (pos == null || p.distanceToSqr(pos.getCenter()) > 8 * 8
                    || !(p.level().getBlockEntity(pos) instanceof KnabbelbakenBlockEntity baken)) {
                return;
            }
            if (optie == KLAAR) {
                Praat.sluit(p);
                OPEN.remove(p.getUUID());
                return;
            }
            antwoord(p, baken, optie);
            open(p, baken);   // (the menu stays open: switch more on or off)
        });
    }

    /** What an answer does (also for the game tests). */
    static void antwoord(ServerPlayer p, KnabbelbakenBlockEntity baken, int optie) {
        if (optie == STRAAL) {
            baken.setStraalUit(!baken.straalUit());
            p.level().playSound(null, baken.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.6f, 1.2f);
            p.sendOverlayMessage(Component.translatable(baken.straalUit() ? "quest.guhs.knabbelbaken.straal_uit" : "quest.guhs.knabbelbaken.straal_aan")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (optie >= 0 && optie < KnabbelbakenBlockEntity.Gunst.values().length) {
            baken.schakel(p, KnabbelbakenBlockEntity.Gunst.values()[optie]);
        }
    }

    public static void open(ServerPlayer p, KnabbelbakenBlockEntity baken) {
        baken.refresh();
        int lagen = baken.levels();
        if (lagen == 0) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelbaken.geen_piramide").withStyle(ChatFormatting.GRAY));
            return;
        }
        List<Praat.Optie> opties = new ArrayList<>();
        for (KnabbelbakenBlockEntity.Gunst g : KnabbelbakenBlockEntity.Gunst.values()) {
            if (g.levels <= lagen) {
                opties.add(new Praat.Optie(g.ordinal(), "quest.guhs.knabbelbaken.menu." + (baken.aan(g) ? "aan." : "uit.") + g.id()));
            }
        }
        opties.add(new Praat.Optie(STRAAL, baken.straalUit() ? "quest.guhs.knabbelbaken.menu.toon" : "quest.guhs.knabbelbaken.menu.verberg"));
        opties.add(new Praat.Optie(KLAAR, "quest.guhs.knabbelbaken.menu.klaar"));
        OPEN.put(p.getUUID(), baken.getBlockPos());
        Praat.scene(p, SLEUTEL, List.of(new Praat.Regel(null, "block.guhs.knabbelbaken", "quest.guhs.knabbelbaken.menu",
                lagen, baken.range())),
                opties.toArray(Praat.Optie[]::new));
    }
}
