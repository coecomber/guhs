package nl.juiced.guhs.feature.vadsrijk.huisdieren;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;

/**
 * One pet (G-PETS): a tiny chibi copy of a boss, unlocked for a player on their first kill of that boss ({@link #baas}).
 * Purely cosmetic for now; {@link #effect} is the extension point for later stats/buffs (unused, null).
 *
 * @param id       the pet id ("koning_klodder"); texts: {@code vadsrijk.huisdier.<id>.naam|tekst|bron}
 * @param baas     the boss entity type id that unlocks it ("guhs:vr_baas_koning_klodder"); its kern first-kill key is
 *                 {@code "baaskill:" + baas} ({@code VadsrijkBaas#killSleutel})
 * @param model    GeckoLib model id (geckolib/models/entity/&lt;model&gt;.geo.json; usually the boss's own model)
 * @param textuur  full texture path ("textures/vadsrijk/bazen/koning_klodder.png")
 * @param animatie GeckoLib animation id (geckolib/animations/entity/&lt;animatie&gt;.animation.json: idle, hop, sprong)
 * @param schaal   render scale of the model (the pets are tiny: about 0.4-0.5 blocks tall)
 * @param gloed    glowmask texture (the texture path with "_glowmask"), or null
 * @param effect   future stats/buffs/heals (unused now)
 */
public record Huisdier(String id, String baas, String model, String textuur, String animatie, float schaal, @Nullable String gloed,
        @Nullable HuisdierEffect effect) {

    public Huisdier(String id, String baas, String model, String textuur, String animatie, float schaal, @Nullable String gloed) {
        this(id, baas, model, textuur, animatie, schaal, gloed, null);
    }

    public String naamKey() {
        return "vadsrijk.huisdier." + id + ".naam";
    }

    public String tekstKey() {
        return "vadsrijk.huisdier." + id + ".tekst";
    }

    public String bronKey() {
        return "vadsrijk.huisdier." + id + ".bron";
    }

    /** The kern first-kill key of the boss (SpelerData.gezien). */
    public String killSleutel() {
        return "baaskill:" + baas;
    }

    public Identifier modelId() {
        return Guhs.id("entity/" + model);   // (GeckoLib resolves geckolib/models/<id>.geo.json)
    }

    public Identifier textuurId() {
        return Guhs.id(textuur);
    }

    public Identifier animatieId() {
        return Guhs.id("entity/" + animatie);
    }
}
