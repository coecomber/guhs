package nl.juiced.guhs.feature.bibliotheek.client;

import java.util.Objects;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.bibliotheek.BibliotheekPayloads;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * A guh book lying open on a lectern of the guh library: the normal book screen, with a "take a copy" button next to
 * "Done" as long as this player hasn't taken their one copy of this book yet.
 */
public class LeesboekScreen extends BookViewScreen {
    private final BibliotheekPayloads.OpenBook open;

    public LeesboekScreen(BibliotheekPayloads.OpenBook open) {
        super(Objects.requireNonNullElse(BookViewScreen.BookAccess.fromItem(book(open).stack()), BookViewScreen.EMPTY_ACCESS));
        this.open = open;
    }

    private static Guhboek book(BibliotheekPayloads.OpenBook open) {
        return Guhboek.values()[Math.floorMod(open.book(), Guhboek.values().length)];
    }

    @Override
    protected void createMenuControls() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, 196, 98, 20).build());
        Button take = Button.builder(Component.translatable(open.canTake() ? "gui.guhs.bieb.take" : "gui.guhs.bieb.taken"), b -> {
                    ClientPacketDistributor.sendToServer(new BibliotheekPayloads.TakeBook(open.pos()));
                    onClose();
                }).bounds(width / 2 + 2, 196, 98, 20)
                .tooltip(Tooltip.create(Component.translatable(open.canTake() ? "gui.guhs.bieb.take.tooltip" : "gui.guhs.bieb.taken.tooltip")))
                .build();
        take.active = open.canTake();
        addRenderableWidget(take);
    }
}
