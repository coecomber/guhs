package nl.juiced.guhs.feature.vadsrijk.vfx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval.Anker;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval.Effect;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval.Stap;

/**
 * The bosses as data (roadmap 3.1, "bosses are data-ish"): per boss (key = {@link BaasVfx#sleutel}) the extra bouwdoos attacks
 * that come on top of its own rotation. Every major boss of zones 1-3 gets at least one NEW mechanic here (an interrupt, a safe
 * spot, in-out, a stack, a spread, a rain...). Solo stays doable: telegraphs 30+ ticks, factors like the bosses' own moves.
 * <p>
 * Colours (Telegraaf): red = damage, pink = safe, purple = mechanic (interrupt), yellow = stack.
 */
public final class BaasProfielen {
    static final int ROOD = 0xFF2A2A, ROZE = 0xFF5FC8, PAARS = 0xA040FF, GEEL = 0xFFD62A;
    private static final Map<String, List<Aanval>> PROFIELEN = new LinkedHashMap<>();

    private BaasProfielen() {
    }

    public static List<Aanval> van(String sleutel) {
        if (PROFIELEN.isEmpty()) {
            vul();
        }
        return PROFIELEN.getOrDefault(sleutel, List.of());
    }

    /** All bosses with a profile (dev command, gametests, SPIEK). */
    public static Map<String, List<Aanval>> alle() {
        if (PROFIELEN.isEmpty()) {
            vul();
        }
        return PROFIELEN;
    }

    /** Finds an attack by id over all bosses (dev command). */
    @javax.annotation.Nullable
    public static Aanval aanval(String id) {
        for (List<Aanval> l : alle().values()) {
            for (Aanval a : l) {
                if (a.id().equals(id)) {
                    return a;
                }
            }
        }
        return null;
    }

    private static void zet(String baas, Aanval... aanvallen) {
        PROFIELEN.put(baas, List.of(aanvallen));
    }

    /** Heals a fraction of max Vads with a pink-green swirl (what an un-interrupted heal cast does). */
    static void heel(VadsrijkBaas b, ServerLevel sl, double fractie) {
        b.heal(b.maxVads() * fractie);
        Vfx.deeltjes(sl, Deeltje.PLUIS, b.position().add(0, b.getBbHeight() * 0.5, 0), 30, 0x8AF0A0, b.getBbWidth() * 0.6, 0.12);
        Vfx.zuil(sl, b.position(), b.getBbHeight() + 4, 0x8AF0A0, 30);
        for (net.minecraft.server.level.ServerPlayer p : Vfx.kijkers(b, sl)) {
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.vfx.geheeld",
                    net.minecraft.network.chat.Component.translatable(b.titelKey())).withStyle(net.minecraft.ChatFormatting.RED));
        }
    }

    private static void vul() {
        // ---- zone 1 ---------------------------------------------------------------------------------------------------------
        // De Grauwe Klodderreus: a slime rain from phase 1 (decals early!), the in-out wave from phase 2
        zet("klodderreus",
                Aanval.nieuw("kloddertjesregen").wacht(520).eerst(200)
                        .stap(Stap.op(0, 40, Vorm.cirkel(2.6), Anker.WILLEKEURIG).aantal(6).factor(0.9).effect(Effect.SLIJM).status(Status.PLAKKERIG, 60))
                        .stap(Stap.op(12, 38, Vorm.cirkel(2.6), Anker.ELKE_SPELER).factor(0.9).effect(Effect.SLIJM).status(Status.PLAKKERIG, 60)),
                Aanval.nieuw("kloddergolf").fase(2).wacht(440).eerst(120)
                        .stap(Stap.op(0, 44, Vorm.donut(4.5, 13), Anker.BAAS).factor(1.3).duw(0.9).effect(Effect.SLIJM).schud(0.55f)));
        // Koning Klodder: crowns on everyone, and the interruptible royal bath (heal) from phase 2
        zet("koning_klodder",
                Aanval.nieuw("kroonregen").wacht(480).treffer("land")
                        .stap(Stap.op(0, 36, Vorm.cirkel(3), Anker.ELKE_SPELER).factor(1.0).effect(Effect.STER))
                        .stap(Stap.op(14, 36, Vorm.cirkel(3), Anker.WILLEKEURIG).aantal(4).factor(1.0).effect(Effect.STER)),
                Aanval.nieuw("koninklijk_bad").fase(2).wacht(700).eerst(160).onderbreek(0.05).treffer("brul")
                        .stap(Stap.op(0, 80, Vorm.cirkel(3.5), Anker.BAAS).factor(0.6).kleur(PAARS).effect(Effect.SLIJM))
                        .nietOnderbroken((b, sl) -> heel(b, sl, 0.08)));
        // Opperzwam: a spore cone; from phase 2 front+back, then the sides
        zet("opperzwam",
                Aanval.nieuw("sporenkegel").totFase(1).wacht(360).treffer("stamp")
                        .stap(Stap.op(0, 36, Vorm.kegel(11, 70), Anker.BAAS).factor(1.3).status(Status.GESPOREN, 80).effect(Effect.SPOREN)),
                Aanval.nieuw("sporenkruis").fase(2).wacht(420).treffer("stamp")
                        .stap(Stap.op(0, 34, Vorm.kegel(11, 70), Anker.BAAS).factor(1.3).status(Status.GESPOREN, 80).effect(Effect.SPOREN))
                        .stap(Stap.op(0, 34, Vorm.kegel(11, 70), Anker.BAAS).draai(180).factor(1.3).status(Status.GESPOREN, 80).effect(Effect.SPOREN))
                        .stap(Stap.op(26, 30, Vorm.kegel(11, 70), Anker.BAAS).draai(90).factor(1.3).effect(Effect.SPOREN))
                        .stap(Stap.op(26, 30, Vorm.kegel(11, 70), Anker.BAAS).draai(270).factor(1.3).effect(Effect.SPOREN)));
        // ---- zone 2 ---------------------------------------------------------------------------------------------------------
        // Galmwolf: an echo roar (front, then behind); from phase 2 in-OUT: first the close circle, then the ring
        zet("galmwolf",
                Aanval.nieuw("echobrul").wacht(400).treffer("adem")
                        .stap(Stap.op(0, 32, Vorm.kegel(12, 60), Anker.BAAS).factor(1.2).effect(Effect.KRISTAL))
                        .stap(Stap.op(16, 32, Vorm.kegel(12, 60), Anker.BAAS).draai(180).factor(1.2).effect(Effect.KRISTAL)),
                Aanval.nieuw("galmring").fase(2).wacht(460).treffer("galm")
                        .stap(Stap.op(0, 38, Vorm.cirkel(5.5), Anker.BAAS).factor(1.3).duw(0.6).effect(Effect.KRISTAL).schud(0.45f))
                        .stap(Stap.op(38, 22, Vorm.donut(5.5, 14), Anker.BAAS).factor(1.3).effect(Effect.KRISTAL).schud(0.45f)));
        // Vrouwe Spiegelzucht: a fan of mirror beams; from phase 2 the mirror hall (only the two pink spots are safe)
        zet("spiegelzucht",
                Aanval.nieuw("spiegelscherven").wacht(380).treffer("straal")
                        .stap(Stap.op(0, 34, Vorm.lijn(17, 3), Anker.BAAS).draai(-40).factor(1.2).effect(Effect.KRISTAL))
                        .stap(Stap.op(0, 34, Vorm.lijn(17, 3), Anker.BAAS).factor(1.2).effect(Effect.KRISTAL))
                        .stap(Stap.op(0, 34, Vorm.lijn(17, 3), Anker.BAAS).draai(40).factor(1.2).effect(Effect.KRISTAL)),
                Aanval.nieuw("spiegelzaal").fase(2).wacht(620).eerst(160).treffer("spiegel")
                        .stap(Stap.op(0, 70, Vorm.veilig(3.2), Anker.WILLEKEURIG).aantal(2).factor(1.6).effect(Effect.KRISTAL).schud(0.6f)));
        // Boormeester: a fan of drill lanes; from phase 2 the interruptible overpressure (or run to a safe spot)
        zet("boormeester",
                Aanval.nieuw("boorlinie").wacht(380).treffer("stoot")
                        .stap(Stap.op(0, 30, Vorm.lijn(18, 3.5), Anker.BAAS).factor(1.3).effect(Effect.VUUR).status(Status.BRANDEND, 60))
                        .stap(Stap.op(12, 30, Vorm.lijn(18, 3.5), Anker.BAAS).draai(28).factor(1.3).effect(Effect.VUUR))
                        .stap(Stap.op(24, 30, Vorm.lijn(18, 3.5), Anker.BAAS).draai(-28).factor(1.3).effect(Effect.VUUR)),
                Aanval.nieuw("overdruk").fase(2).wacht(700).eerst(200).onderbreek(0.06).treffer("stoom")
                        .stap(Stap.op(0, 90, Vorm.veilig(3.5), Anker.WILLEKEURIG).aantal(2).factor(1.6).effect(Effect.VUUR).schud(0.6f)));
        // ---- zone 3 ---------------------------------------------------------------------------------------------------------
        // Kruinroofvogel: a feather rain (spread + random), from phase 2 a wind cone that blows you back
        zet("kruinroofvogel",
                Aanval.nieuw("veerregen").wacht(440).treffer("wind")
                        .stap(Stap.op(0, 40, Vorm.cirkel(2.6), Anker.WILLEKEURIG).aantal(8).factor(1.0).effect(Effect.VEREN))
                        .stap(Stap.op(14, 36, Vorm.cirkel(3), Anker.ELKE_SPELER).factor(1.0).effect(Effect.VEREN)),
                Aanval.nieuw("windkegel").fase(2).wacht(420).treffer("wind")
                        .stap(Stap.op(0, 32, Vorm.kegel(15, 50), Anker.BAAS).factor(0.9).duw(1.7).effect(Effect.VEREN).schud(0.4f)));
        // Takelmeester Taleggio: a wide rope sweep; from phase 2 cheese barrels that make you sticky
        zet("taleggio",
                Aanval.nieuw("touwzwiep").wacht(360).treffer("slag")
                        .stap(Stap.op(0, 30, Vorm.kegel(9, 140), Anker.BAAS).factor(1.4).duw(1.0).effect(Effect.KAAS)),
                Aanval.nieuw("kaasvaten").fase(2).wacht(460).treffer("worp")
                        .stap(Stap.op(0, 42, Vorm.cirkel(3), Anker.WILLEKEURIG).aantal(5).factor(1.0).status(Status.PLAKKERIG, 80).effect(Effect.KAAS))
                        .stap(Stap.op(10, 40, Vorm.cirkel(3), Anker.ELKE_SPELER).factor(1.0).status(Status.PLAKKERIG, 80).effect(Effect.KAAS)));
        // Bombardier Brokkel: crossfire on you; from phase 2 a second cross turned 45 degrees
        zet("brokkel",
                Aanval.nieuw("kruisvuur").totFase(1).wacht(400).treffer("worp")
                        .stap(Stap.op(0, 36, Vorm.kruis(14, 3), Anker.DOEL).factor(1.3).effect(Effect.VUUR)),
                Aanval.nieuw("dubbel_kruisvuur").fase(2).wacht(420).treffer("worp")
                        .stap(Stap.op(0, 34, Vorm.kruis(14, 3), Anker.DOEL).factor(1.3).effect(Effect.VUUR))
                        .stap(Stap.op(20, 34, Vorm.kruis(14, 3), Anker.DOEL).draai(45).factor(1.3).effect(Effect.VUUR)));
        // Kapitein Roquefort: the sabre tankbuster on his target, from phase 2 a turning sabre cross on himself
        zet("roquefort",
                Aanval.nieuw("sabelsnede").wacht(420).treffer("sabel")
                        .stap(Stap.op(0, 40, Vorm.cirkel(3.4), Anker.DOEL).factor(2.0).effect(Effect.KAAS).schud(0.5f)),
                Aanval.nieuw("sabelstorm").fase(2).wacht(480).treffer("sabel")
                        .stap(Stap.op(0, 32, Vorm.kruis(13, 3), Anker.BAAS).factor(1.3).effect(Effect.VUUR))
                        .stap(Stap.op(18, 32, Vorm.kruis(13, 3), Anker.BAAS).draai(45).factor(1.3).effect(Effect.VUUR)));
        // Graaf Piepvel: a cape sweep front then back; from phase 2 the interruptible squeak choir (heal + a big ring)
        zet("piepvel",
                Aanval.nieuw("capezwiep").wacht(400).treffer("capezwier")
                        .stap(Stap.op(0, 30, Vorm.kegel(10, 120), Anker.BAAS).factor(1.3).effect(Effect.SCHADUW))
                        .stap(Stap.op(16, 30, Vorm.kegel(10, 120), Anker.BAAS).draai(180).factor(1.3).effect(Effect.SCHADUW)),
                Aanval.nieuw("piepkoor").fase(2).wacht(700).eerst(160).onderbreek(0.05).treffer("buig")
                        .stap(Stap.op(0, 80, Vorm.donut(4, 15), Anker.BAAS).factor(1.3).kleur(PAARS).effect(Effect.SCHADUW).schud(0.5f))
                        .nietOnderbroken((b, sl) -> heel(b, sl, 0.06)));
        // De Wortelwurger: roots grab under everyone (spread, sticky)
        zet("wortelwurger",
                Aanval.nieuw("wortelgreep").wacht(400).treffer("grijp")
                        .stap(Stap.op(0, 34, Vorm.cirkel(2.8), Anker.ELKE_SPELER).factor(1.1).status(Status.PLAKKERIG, 60).effect(Effect.WORTEL))
                        .stap(Stap.op(10, 34, Vorm.cirkel(2.8), Anker.WILLEKEURIG).aantal(5).factor(1.1).effect(Effect.WORTEL)));
        // De Nestwachter: OUT-in (the ring first, then the close circle: the reverse of the Galmwolf)
        zet("nestwachter",
                Aanval.nieuw("nestschok").wacht(440).treffer("stamp")
                        .stap(Stap.op(0, 36, Vorm.donut(5.5, 14), Anker.BAAS).factor(1.3).effect(Effect.KRISTAL).schud(0.4f))
                        .stap(Stap.op(36, 22, Vorm.cirkel(5.5), Anker.BAAS).factor(1.3).duw(0.8).effect(Effect.KRISTAL).schud(0.5f)));
        // Baron Bleu: a rapier combo at you; from phase 2 the "duet" stack (share it with a friend; alone it hits for 60 %)
        zet("bleu",
                Aanval.nieuw("degenreeks").wacht(380).treffer("slag")
                        .stap(Stap.op(0, 24, Vorm.lijn(13, 2.4), Anker.BAAS).factor(1.1).effect(Effect.STER))
                        .stap(Stap.op(10, 24, Vorm.lijn(13, 2.4), Anker.BAAS).draai(20).factor(1.1).effect(Effect.STER))
                        .stap(Stap.op(20, 24, Vorm.lijn(13, 2.4), Anker.BAAS).draai(-20).factor(1.1).effect(Effect.STER)),
                Aanval.nieuw("duet").fase(2).wacht(560).treffer("draai")
                        .stap(Stap.op(0, 46, Vorm.cirkel(3.5), Anker.DOEL).factor(2.0).stapel().effect(Effect.STER).schud(0.5f)));
        // Vahoegnir (the showcase): fire rain, from phase 2 the sky storm (safe spots), phase 3 a turning flame cross and the last roar
        // (interrupt it, or stand close to the middle). Phase 4 (the scripted dives at the egg + the ending) gets nothing extra.
        zet("vahoegnir",
                Aanval.nieuw("vuurregen").totFase(3).wacht(440)
                        .stap(Stap.op(0, 44, Vorm.cirkel(3), Anker.WILLEKEURIG).aantal(10).factor(1.1).effect(Effect.VUUR).status(Status.BRANDEND, 60))
                        .stap(Stap.op(14, 40, Vorm.cirkel(3), Anker.ELKE_SPELER).factor(1.1).effect(Effect.VUUR)),
                Aanval.nieuw("hemelstorm").fase(2).totFase(3).wacht(640).eerst(140).treffer("brul")
                        .stap(Stap.op(0, 80, Vorm.veilig(3.6), Anker.WILLEKEURIG).aantal(2).factor(1.7).effect(Effect.STER).schud(0.7f)),
                Aanval.nieuw("vlammenkruis").fase(3).totFase(3).wacht(520).treffer("adem")
                        .stap(Stap.op(0, 30, Vorm.kruis(24, 4), Anker.MIDDEN).factor(1.4).effect(Effect.VUUR))
                        .stap(Stap.op(22, 30, Vorm.kruis(24, 4), Anker.MIDDEN).draai(45).factor(1.4).effect(Effect.VUUR))
                        .stap(Stap.op(44, 30, Vorm.kruis(24, 4), Anker.MIDDEN).factor(1.4).effect(Effect.VUUR)),
                Aanval.nieuw("laatste_brul").fase(3).totFase(3).wacht(900).eerst(300).onderbreek(0.04).treffer("brul")
                        .stap(Stap.op(0, 100, Vorm.donut(5, 30), Anker.MIDDEN).factor(1.6).kleur(PAARS).effect(Effect.VUUR).schud(0.8f))
                        .nietOnderbroken((b, sl) -> heel(b, sl, 0.05)));
    }
}
