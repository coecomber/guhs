package nl.juiced.guhs.feature.vadsrijk.voltooiing;

import net.minecraft.ChatFormatting;
import nl.juiced.guhs.feature.titels.Titels;

/** The 100 % titles ("Kenner van Zwamzoet"...): earned once the zone's reward was handed out (shown in the normal world too). */
final class VoltooiingTitels {
    private VoltooiingTitels() {
    }

    static void registreer(Voltooiing.ZoneBoek z) {
        if (Titels.van(z.titelId()) != null) {
            return;
        }
        Titels.registreer(new Titels.Titel(z.titelId(), "gui.guhs.titels.naam." + z.titelId(), ChatFormatting.LIGHT_PURPLE, "minecraft:writable_book",
                p -> Voltooiing.beloond(p, z.id())));
    }
}
