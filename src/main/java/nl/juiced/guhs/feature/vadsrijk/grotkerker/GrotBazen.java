package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval.Anker;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval.Effect;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval.Stap;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vorm;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensRegistry;

/**
 * U-GROTKERKER: the six cave bosses (one {@link GrotBaas} entity type each, {@code guhs:vr_grot_baas_<cave>}), what they look like
 * (an existing cave creature model, scaled up, with its elite glow and a soft zone tint), their attacks (VFX-kit bouwdoos: decal
 * telegraphs, cast bars; every boss has its own signature mechanic), the cave's pack creatures + mid-elite, the rare cave pet
 * (a chibi of the boss) and the rare cave mount (a Pluimguh colour, rolled through {@code vint.RijdierBuit}: 10 % + pity 15).
 */
public final class GrotBazen {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    static final int ROOD = 0xFF2A2A, PAARS = 0xA040FF, GEEL = 0xFFD62A;

    /**
     * One cave boss.
     *
     * @param model    GeckoLib model/animation path under entity/ ("vadsrijk/wezens/sporenpriester")
     * @param tex      texture path under textures/ without .png ("vadsrijk/wezens/sporenpriester"; + "_elite" is the glow)
     * @param schaal   render scale of the model
     * @param loop     the model's walk animation ("loop", "kruip", ...)
     * @param aanval   the model's attack animation
     * @param tint     ARGB tint multiplied on the model (soft guh tone of the zone)
     * @param thema    the boss music theme (geluid: vr.geluid.baas.&lt;thema&gt;; a borrowed theme as the fallback)
     * @param hulp     the creature it calls for help at half health (phase 2)
     * @param pet      the rare cave pet id; {@code petSchaal} its render scale
     * @param pluim    the rare cave mount colour
     */
    public record Def(String grot, String model, String tex, float schaal, String loop, String aanval, int tint, String thema, Element element,
            Supplier<? extends EntityType<? extends VadsrijkMob>> hulp, List<Aanval> aanvallen, String pet, float petSchaal, PluimKleur pluim) {
        public String petTex() {
            return "textures/" + tex + (tex.startsWith("vadsrijk/wezens/") || tex.startsWith("vadsrijk/zone4/") || tex.startsWith("vadsrijk/zone6/") ? "_blij" : "")
                    + ".png";
        }

        public String mountBron() {
            return "grot_" + grot + "_pluim";
        }
    }

    /** Pack creatures per cave: two kinds that alternate, and the mid-elite. */
    public record Pak(Supplier<? extends EntityType<? extends VadsrijkMob>> a, Supplier<? extends EntityType<? extends VadsrijkMob>> b,
            Supplier<? extends EntityType<? extends VadsrijkMob>> elite) {
    }

    private static DeferredHolder<EntityType<?>, EntityType<GrotBaas>> baas(String grot, float b, float h) {
        String id = "vr_grot_baas_" + grot;
        return ENTITIES.register(id, () -> EntityType.Builder.<GrotBaas>of(GrotBaas::new, MobCategory.MONSTER).sized(b, h).eyeHeight(h * 0.75f)
                .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    public static final DeferredHolder<EntityType<?>, EntityType<GrotBaas>> ZWAMGROT = baas("zwamgrot", 1.6f, 3.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<GrotBaas>> KRISTALGROT = baas("kristalgrot", 3.0f, 2.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<GrotBaas>> WORTELGROT = baas("wortelgrot", 2.0f, 3.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<GrotBaas>> KORAALGROT = baas("koraalgrot", 2.4f, 2.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<GrotBaas>> WOLKENGROT = baas("wolkengrot", 2.2f, 2.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<GrotBaas>> ZOUTGROT = baas("zoutgrot", 3.2f, 2.2f);

    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<GrotBaas>>> TYPES = new LinkedHashMap<>();
    static final Map<String, Def> DEFS = new LinkedHashMap<>();
    static final Map<String, Pak> PAKKEN = new LinkedHashMap<>();

    static {
        TYPES.put("zwamgrot", ZWAMGROT);
        TYPES.put("kristalgrot", KRISTALGROT);
        TYPES.put("wortelgrot", WORTELGROT);
        TYPES.put("koraalgrot", KORAALGROT);
        TYPES.put("wolkengrot", WOLKENGROT);
        TYPES.put("zoutgrot", ZOUTGROT);
        PAKKEN.put("zwamgrot", new Pak(WezensRegistry.GLIMZWAM, WezensRegistry.SPORENPUFFER, WezensRegistry.ZWAMWACHTER));
        PAKKEN.put("kristalgrot", new Pak(Z2WezensRegistry.KRISTALNAALDKRUIPER, Z2WezensRegistry.SPLINTERLING, Z2WezensRegistry.VERSTEENDE_WACHT));
        PAKKEN.put("wortelgrot", new Pak(Z3WezensRegistry.WORTELSCHIM, Z3WezensRegistry.SCHORSKRUIPER, Z3WezensRegistry.WORTELSLANG));
        PAKKEN.put("koraalgrot", new Pak(Z4WezensRegistry.KORAALKOBOLD, Z4WezensRegistry.STEKELPLUIS, Z4WezensRegistry.MOPPERKRAB));
        PAKKEN.put("wolkengrot", new Pak(Z5WezensRegistry.MISTSLIER, Z5WezensRegistry.GRAUWE_WOLKDUIVEL, Z5WezensRegistry.GRAUWE_WOLKDUIVEL));
        PAKKEN.put("zoutgrot", new Pak(Z6WezensRegistry.ZANDGEESTJE, Z6WezensRegistry.SPIEGELSLANG, Z6WezensRegistry.ZOUTSLANG));
    }

    private GrotBazen() {
    }

    @Nullable
    public static Def def(String grot) {
        if (DEFS.isEmpty()) {
            vul();
        }
        return DEFS.get(grot);
    }

    /** The def of a boss entity type ("guhs:vr_grot_baas_zwamgrot"). */
    @Nullable
    public static Def van(EntityType<?> t) {
        String pad = BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath();
        return pad.startsWith("vr_grot_baas_") ? def(pad.substring("vr_grot_baas_".length())) : null;
    }

    public static Map<String, Def> alle() {
        if (DEFS.isEmpty()) {
            vul();
        }
        return DEFS;
    }

    private static void zet(Def d) {
        DEFS.put(d.grot(), d);
    }

    private static synchronized void vul() {
        if (!DEFS.isEmpty()) {
            return;
        }
        // ---- zone 1: Moeder Glimhoed (a big Sporenpriester). Signature: Glimschuil, hide in the pink glow circles from the spore burst.
        zet(new Def("zwamgrot", "vadsrijk/wezens/sporenpriester", "vadsrijk/wezens/sporenpriester", 1.9f, "loop", "spreuk", 0xFFE8FFE8, "opperzwam",
                Element.NATUUR, WezensRegistry.GLIMZWAM, List.of(
                        Aanval.nieuw("grot_sporenkegel").wacht(340).treffer("spreuk")
                                .stap(Stap.op(0, 36, Vorm.kegel(11, 70), Anker.BAAS).factor(1.2).status(Status.GESPOREN, 80).effect(Effect.SPOREN)),
                        Aanval.nieuw("grot_sporenregen").wacht(420).eerst(120)
                                .stap(Stap.op(0, 40, Vorm.cirkel(2.6), Anker.WILLEKEURIG).aantal(6).factor(0.9).effect(Effect.SPOREN))
                                .stap(Stap.op(12, 36, Vorm.cirkel(2.6), Anker.ELKE_SPELER).factor(0.9).status(Status.GESPOREN, 60).effect(Effect.SPOREN)),
                        Aanval.nieuw("grot_glimschuil").fase(2).wacht(600).eerst(100).treffer("spreuk")
                                .stap(Stap.op(0, 70, Vorm.veilig(3.0), Anker.WILLEKEURIG).aantal(2).factor(1.5).effect(Effect.SPOREN).schud(0.5f))),
                "glimhoedje", 0.28f, PluimKleur.GLIMZWAM));
        // ---- zone 2: Tante Tinkelpoot (a big Kristalspin). Signature: Kristalzang, an interruptible song that heals her.
        zet(new Def("kristalgrot", "vadsrijk/zone2/wezens/kristalspin", "vadsrijk/zone2/wezens/kristalspin", 2.8f, "loop", "spuug", 0xFFEAF4FF, "spiegelzucht",
                Element.WATER, Z2WezensRegistry.KRISTALNAALDKRUIPER, List.of(
                        Aanval.nieuw("grot_draadwaaier").wacht(360).treffer("spuug")
                                .stap(Stap.op(0, 34, Vorm.lijn(16, 2.8), Anker.BAAS).draai(-35).factor(1.1).effect(Effect.KRISTAL))
                                .stap(Stap.op(0, 34, Vorm.lijn(16, 2.8), Anker.BAAS).factor(1.1).effect(Effect.KRISTAL))
                                .stap(Stap.op(0, 34, Vorm.lijn(16, 2.8), Anker.BAAS).draai(35).factor(1.1).effect(Effect.KRISTAL)),
                        Aanval.nieuw("grot_rinkelregen").wacht(440).eerst(140)
                                .stap(Stap.op(0, 40, Vorm.cirkel(2.4), Anker.WILLEKEURIG).aantal(7).factor(0.9).effect(Effect.KRISTAL))
                                .stap(Stap.op(12, 36, Vorm.cirkel(2.6), Anker.ELKE_SPELER).factor(0.9).status(Status.PLAKKERIG, 40).effect(Effect.KRISTAL)),
                        Aanval.nieuw("grot_kristalzang").fase(2).wacht(640).eerst(120).onderbreek(0.05).treffer("aanval")
                                .stap(Stap.op(0, 80, Vorm.donut(3.5, 13), Anker.BAAS).factor(1.3).kleur(PAARS).effect(Effect.KRISTAL).schud(0.5f))
                                .nietOnderbroken((b, sl) -> GrotBaas.heel(b, sl, 0.06))),
                "tinkeltje", 0.5f, PluimKleur.AMETHIST));
        // ---- zone 3: Ouwe Knoestbuik (a big Harsgolem). Signature: Samen tillen!, a stack: share the hit (alone it hits for 60 %).
        zet(new Def("wortelgrot", "vadsrijk/zone3/wezens/harsgolem", "vadsrijk/zone3/wezens/harsgolem", 1.5f, "loop", "klap", 0xFFFFF0DC, "wortelwurger",
                Element.NATUUR, Z3WezensRegistry.WORTELSCHIM, List.of(
                        Aanval.nieuw("grot_wortelgreep").wacht(380).treffer("hef")
                                .stap(Stap.op(0, 34, Vorm.cirkel(2.8), Anker.ELKE_SPELER).factor(1.1).status(Status.PLAKKERIG, 60).effect(Effect.WORTEL))
                                .stap(Stap.op(10, 34, Vorm.cirkel(2.8), Anker.WILLEKEURIG).aantal(4).factor(1.1).effect(Effect.WORTEL)),
                        Aanval.nieuw("grot_knoestkruis").wacht(420).eerst(140).treffer("klap")
                                .stap(Stap.op(0, 32, Vorm.kruis(13, 3), Anker.BAAS).factor(1.3).effect(Effect.WORTEL).schud(0.4f))
                                .stap(Stap.op(20, 32, Vorm.kruis(13, 3), Anker.BAAS).draai(45).factor(1.3).effect(Effect.WORTEL).schud(0.4f)),
                        Aanval.nieuw("grot_samen_tillen").fase(2).wacht(560).eerst(100).treffer("uithaal")
                                .stap(Stap.op(0, 46, Vorm.cirkel(3.5), Anker.DOEL).factor(1.9).stapel().effect(Effect.WORTEL).schud(0.6f))),
                "knoestje", 0.22f, PluimKleur.WORTELHOUT));
        // ---- zone 4: De Grote Zeurslak. Signature: Getij, in-OUT (first the close circle, then the ring); from phase 2 the Schelpzeur.
        zet(new Def("koraalgrot", "vadsrijk/zone4/wezens/zeurslak", "vadsrijk/zone4/wezens/zeurslak", 4.0f, "kruip", "aanval", 0xFFFFF0F0, "zeeslurf",
                Element.WATER, Z4WezensRegistry.KORAALKOBOLD, List.of(
                        Aanval.nieuw("grot_bubbelregen").wacht(400).treffer("aanval")
                                .stap(Stap.op(0, 40, Vorm.cirkel(2.6), Anker.WILLEKEURIG).aantal(7).factor(0.9).effect(Effect.PLOF))
                                .stap(Stap.op(12, 36, Vorm.cirkel(2.6), Anker.ELKE_SPELER).factor(0.9).effect(Effect.PLOF)),
                        Aanval.nieuw("grot_getij").wacht(440).eerst(100).treffer("schelp")
                                .stap(Stap.op(0, 38, Vorm.cirkel(5.0), Anker.BAAS).factor(1.3).duw(0.6).effect(Effect.PLOF).schud(0.45f))
                                .stap(Stap.op(38, 22, Vorm.donut(5.0, 14), Anker.BAAS).factor(1.3).effect(Effect.PLOF).schud(0.45f)),
                        Aanval.nieuw("grot_schelpzeur").fase(2).wacht(680).eerst(160).onderbreek(0.05).treffer("schelp")
                                .stap(Stap.op(0, 80, Vorm.cirkel(3.5), Anker.BAAS).factor(0.7).kleur(PAARS).effect(Effect.SCHADUW))
                                .nietOnderbroken((b, sl) -> GrotBaas.heel(b, sl, 0.05))),
                "zeurtje", 0.65f, PluimKleur.KORAAL));
        // ---- zone 5: Opperpluis (a huge Grauwe Pluismijt). Signature: Wolkenschuil, OUT-in (the ring first, then the close circle).
        zet(new Def("wolkengrot", "vadsrijk/zone5/wezens/grauwe_pluismijt", "vadsrijk/zone5/wezens/grauwe_pluismijt", 7.0f, "loop", "aanval", 0xFFF4F0FF,
                "donderbuik", Element.WATER, Z5WezensRegistry.MISTSLIER, List.of(
                        Aanval.nieuw("grot_pluisbui").wacht(400)
                                .stap(Stap.op(0, 40, Vorm.cirkel(2.6), Anker.WILLEKEURIG).aantal(8).factor(1.0).effect(Effect.VEREN))
                                .stap(Stap.op(14, 36, Vorm.cirkel(3), Anker.ELKE_SPELER).factor(1.0).effect(Effect.VEREN)),
                        Aanval.nieuw("grot_donderknuffel").wacht(420).eerst(160).treffer("aanval")
                                .stap(Stap.op(0, 40, Vorm.cirkel(3.4), Anker.DOEL).factor(1.9).effect(Effect.STER).schud(0.5f)),
                        Aanval.nieuw("grot_wolkenschuil").fase(2).wacht(480).eerst(100).treffer("aanval")
                                .stap(Stap.op(0, 36, Vorm.donut(5.5, 14), Anker.BAAS).factor(1.3).effect(Effect.VEREN).schud(0.4f))
                                .stap(Stap.op(36, 22, Vorm.cirkel(5.5), Anker.BAAS).factor(1.3).duw(0.8).effect(Effect.VEREN).schud(0.5f))),
                "pluisje", 1.2f, PluimKleur.ONWEER));
        // ---- zone 6: Koning Galmschaar (a huge Zoutkreeft). Signature: Kristalschuil, only the pink crystal spots are safe from the song.
        zet(new Def("zoutgrot", "vadsrijk/zone6/wezens/zoutkreeft", "vadsrijk/zone6/wezens/zoutkreeft", 5.0f, "loop", "aanval", 0xFFFFF0F4, "zoutkolos",
                Element.WATER, Z6WezensRegistry.ZANDGEESTJE, List.of(
                        Aanval.nieuw("grot_zoutzang").wacht(380).treffer("aanval")
                                .stap(Stap.op(0, 32, Vorm.kegel(12, 60), Anker.BAAS).factor(1.2).effect(Effect.KRISTAL))
                                .stap(Stap.op(16, 32, Vorm.kegel(12, 60), Anker.BAAS).draai(180).factor(1.2).effect(Effect.KRISTAL)),
                        Aanval.nieuw("grot_galmring").wacht(440).eerst(120).treffer("aanval")
                                .stap(Stap.op(0, 38, Vorm.cirkel(5.5), Anker.BAAS).factor(1.3).duw(0.6).effect(Effect.KRISTAL).schud(0.45f))
                                .stap(Stap.op(38, 22, Vorm.donut(5.5, 14), Anker.BAAS).factor(1.3).effect(Effect.KRISTAL).schud(0.45f)),
                        Aanval.nieuw("grot_kristalschuil").fase(2).wacht(620).eerst(100).treffer("aanval")
                                .stap(Stap.op(0, 70, Vorm.veilig(3.2), Anker.WILLEKEURIG).aantal(2).factor(1.6).effect(Effect.KRISTAL).schud(0.6f))),
                "galmpje", 1.0f, PluimKleur.ZOUTROZE));
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            for (var t : TYPES.values()) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 32)
                        .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.STEP_HEIGHT, 1.1).build());
            }
        });
        for (Def d : alle().values()) {
            // the rare cave pet: a chibi of the boss. Its "boss" is a key nobody can kill (grotpet:<cave>), so it never comes by the
            // first-kill rule: only the end chest's rare roll hands it out (KerkerLoot)
            Huisdieren.registreer(new Huisdier(d.pet(), "guhs:grotpet_" + d.grot(), d.model(), d.petTex(), "vadsrijk/zone5/wezens/huisdier", d.petSchaal(), null));
            // the rare cave mount: its Pluimguh colour, 10 % per player per clear, sure after 15 (only once you have a Pluimguh)
            PluimKleur k = d.pluim();
            nl.juiced.guhs.feature.vadsrijk.vint.RijdierBuit.bron(d.mountBron(), "Pluimguh " + k.id() + " (eindkist " + d.grot() + ")", null,
                    p -> Rijdieren.heeft(p, RijdierSoort.PLUIMGUH), p -> Rijdieren.heeftKleur(p, k), p -> Rijdieren.geefKleur(p, k));
        }
    }
}
