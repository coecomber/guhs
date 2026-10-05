package nl.juiced.guhs.feature.huisje;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * A chore residents of a Guhhuisje can do (implementations: the klusjes feature, registered with
 * {@link Klusjes#registreer}; ids and order in CONTRACT_210 par. 5.4). {@link HuisjeGoal} tries a random enabled chore every
 * {@link #wacht()} ticks and runs the {@link KlusTaak} it finds.
 */
public interface Klus {
    String id();

    ItemStack icoon();

    default Component naam() {
        return Component.translatable("gui.guhs.klus." + id());
    }

    /** What it needs nearby (shown in the huisje screen). */
    default Component tip() {
        return Component.translatable("gui.guhs.klus." + id() + ".tip");
    }

    /** Who can do it (guhs; muisjes/turtles only where it fits). */
    boolean kan(Mob bewoner);

    default boolean standaardAan(Mob bewoner) {
        return true;
    }

    /** Minimum ticks between two tries by the same resident. */
    default int wacht() {
        return 200;
    }

    /**
     * 1.2.8: who can do it, for the overview (text gui.guhs.huisje.overzicht.wie.&lt;this&gt;): "guhs", "guhs_muisjes",
     * "guhs_schildpadjes", "sjokkel"; "" = not told. Keep it next to {@link #kan}.
     */
    default String doeners() {
        return "";
    }

    /** 1.2.8: what there is to do for this chore around this huisje right now (the overview in the huisje screen). */
    default KlusStand stand(ServerLevel level, Huisje huisje) {
        return KlusStand.ONBEKEND;
    }

    /** Something to do now, or null. */
    @Nullable
    KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner);
}
