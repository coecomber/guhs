package nl.juiced.guhs.feature.guhwinter.verhaal.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.guhwinter.verhaal.WinterScenes;
import nl.juiced.guhs.feature.guhwinter.verhaal.WinterVerhaalPayloads;

/**
 * Guhwinter (verhaal slice), client: the plein look pieces on the story scenes' actors (the Sint's beard, mantle and staff,
 * Anthony's camera, Liek's nose ring, the Pakjesguhs' suits; the same bones and textures as npc.client.GuhwinterNpcClient
 * draws on the real NPCs, picked here by the actor's entity tag {@code guhwinter_look_<look>}), and the prikbord screens.
 */
public final class WinterVerhaalClient {
    private static final List<String> PAKJE = List.of("outfit_suit", "outfit_beret", "outfit_scarf", "outfit_guhwinter_roet");
    private static final Map<String, List<String>> BOTTEN = Map.of(
            "sint", List.of("outfit_ring_baard", "outfit_long_cape", "outfit_guhwinter_staf"),
            "anthony", List.of("outfit_guhwinter_camera"),
            "liek", List.of("outfit_guhwinter_neusring"),
            "pakje_0", PAKJE, "pakje_1", PAKJE, "pakje_2", PAKJE, "pakje_3", PAKJE);

    private record Look(Identifier texture, Predicate<String> toont) {
    }

    private static final Map<String, Look> LOOKS = new HashMap<>();

    static {
        for (Map.Entry<String, List<String>> e : BOTTEN.entrySet()) {
            List<String> prefixes = e.getValue();
            LOOKS.put(WinterScenes.LOOK + e.getKey(), new Look(Guhs.id("textures/entity/guh_clothes/guhwinter_" + e.getKey() + ".png"),
                    bone -> prefixes.stream().anyMatch(bone::startsWith)));
        }
    }

    private WinterVerhaalClient() {
    }

    /** Is this one of the two Ozovads horses, and is it not where this player's story has him? */
    private static boolean ozovadsElders(net.minecraft.world.entity.Entity e) {
        if (!(e instanceof net.minecraft.world.entity.animal.equine.Horse) || e.getCustomName() == null
                || !(e.getCustomName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t)) {
            return false;
        }
        boolean verstopt = t.getKey().equals("guhwinter.npc.naam.ozovads_verstop");
        if (!verstopt && !t.getKey().equals("guhwinter.npc.naam.ozovads")) {
            return false;
        }
        int stap = nl.juiced.guhs.feature.verhaal.VerhaalSync.Client.stap("guhwinter_sint");
        return verstopt ? stap != nl.juiced.guhs.feature.guhwinter.verhaal.WinterVerhaal.S_OZOVADS
                : stap <= nl.juiced.guhs.feature.guhwinter.verhaal.WinterVerhaal.S_OZOVADS;
    }

    public static void init(IEventBus modBus) {
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (guh.entityTags().isEmpty()) {
                return;
            }
            for (String tag : guh.entityTags()) {
                Look look = LOOKS.get(tag);
                if (look != null) {
                    frame.pass(look.texture(), 0xFFFFFFFF, look.toont());
                }
            }
        });
        // (playtest 1) Ozovads is where YOUR story says: hiding down the valley until you found him (the plein's horse is
        // not drawn for you), on the plein from then on (the hiding one is not drawn). Before the intocht: neither.
        nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler.OOK_VERBORGEN.add(WinterVerhaalClient::ozovadsElders);
        WinterVerhaalPayloads.opener = p -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(p.data().getBooleanOr("Schrijf", false) ? new VoornemenScherm(p.data()) : new PrikbordScherm(p.data())));
        };
    }
}
