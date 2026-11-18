package nl.juiced.guhs.feature.guhwinter.verhaal;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.npc.GuhwinterNpcFeature;
import nl.juiced.guhs.feature.guhwinter.npc.GuhwinterNpcs;
import nl.juiced.guhs.feature.guhwinter.npc.NpcDefs;
import nl.juiced.guhs.feature.guhwinter.spellen.Oliebollen;
import nl.juiced.guhs.feature.guhwinter.tekenen.KerstboomTekeningen;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Guhwinter (verhaal slice): the talking scenes of the winter story, in the verhaal engine's talking screen ({@link Praat}:
 * a portrait, a speech balloon, "Verder »" per page, the answers on the last page). A page of a plein guh shows the real
 * guh when it is near, otherwise its portrait from its {@link NpcDefs} definition (our {@link Praat.PortretBron}).
 * Texts: {@code guhwinter.verhaal.praat.<gesprek>.<n>} (tools/features/guhwinter_verhaal.py).
 */
public final class WinterGesprek {
    static final String P = "guhwinter.verhaal.praat.";
    static final String VERTELLER = "guhwinter.verhaal.verteller";
    static final String NAAM = "guhwinter.npc.naam.";

    static final String BOOM = "guhwinter_boom", BOOM_GEKOZEN = "guhwinter_boom_gekozen", VERSIEREN = "guhwinter_versieren", OPA = "guhwinter_opa",
            OPA_ANTWOORD = "guhwinter_opa_antwoord", LIN = "guhwinter_lin", PROEVEN = "guhwinter_proeven", VOORNEMENS = "guhwinter_voornemens",
            PAKJESAVOND_KORT = "guhwinter_pakjesavond_kort", LIN_NOG_EEN = "guhwinter_lin_nog";

    private WinterGesprek() {
    }

    static void register() {
        Praat.portretBron(naamKey -> {
            if (!naamKey.startsWith(NAAM)) {
                return null;
            }
            String naam = naamKey.substring(NAAM.length());
            NpcDefs.Def d = NpcDefs.van(naam);
            if (d == null) {
                return null;
            }
            if (naam.equals("ozovads")) {
                CompoundTag t = new CompoundTag();
                t.putString("Type", "minecraft:horse");
                return t;
            }
            return Praat.guhPortret(GuhwinterNpcFeature.GUH.get(), d.variant(), d.kleren(nl.juiced.guhs.feature.guhwinter.npc.NpcFase.KERST), false);
        });
        Praat.luister(BOOM, (p, spreker, optie) -> {
            if (optie >= 0 && WinterVerhaal.KERST.stap(p) == WinterVerhaal.K_SPARREN) {
                boomGekozen(p, optie);
            }
        });
        Praat.luister(BOOM_GEKOZEN, (p, spreker, optie) -> {
            if (optie < 0) {
                WinterVerhaal.KERST.verder(p, WinterVerhaal.K_SPARREN);
            }
        });
        Praat.luister(VERSIEREN, (p, spreker, optie) -> {
            if (optie < 0 || WinterVerhaal.KERST.stap(p) != WinterVerhaal.K_VERSIEREN) {
                return;
            }
            WinterVerhaal.KERST.verder(p, WinterVerhaal.K_VERSIEREN);
            if (optie == 1) {
                KerstboomTekeningen.open(p);
            } else {
                Praat.sluit(p);
                p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.4f);
                p.sendSystemMessage(Component.translatable("guhwinter.verhaal.bal").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        });
        Praat.luister(OPA, (p, spreker, optie) -> {
            if (optie >= 0 && WinterVerhaal.KERST.stap(p) == WinterVerhaal.K_OPA) {
                opaAntwoord(p, optie);
            }
        });
        Praat.luister(OPA_ANTWOORD, (p, spreker, optie) -> {
            if (optie < 0 && WinterVerhaal.KERST.verder(p, WinterVerhaal.K_OPA)) {
                ItemStack koekje = new ItemStack(Items.COOKIE, 2);
                if (!p.getInventory().add(koekje)) {
                    p.drop(koekje, false);
                }
            }
        });
        Praat.luister(LIN, (p, spreker, optie) -> {
            if (optie >= 0 && WinterVerhaal.OUDJAAR.stap(p) == WinterVerhaal.O_OLIEBOLLEN) {
                Praat.sluit(p);
                bakken(p, optie == 1);
            }
        });
        Praat.luister(LIN_NOG_EEN, (p, spreker, optie) -> {
            if (optie >= 0 && WinterVerhaal.OUDJAAR.stap(p) == WinterVerhaal.O_OLIEBOLLEN) {
                Praat.sluit(p);
                bakken(p, optie == 1);
            }
        });
        Praat.luister(PROEVEN, (p, spreker, optie) -> {
            if (optie < 0) {
                WinterVerhaal.OUDJAAR.verder(p, WinterVerhaal.O_OLIEBOLLEN);
            }
        });
        Praat.luister(VOORNEMENS, (p, spreker, optie) -> {
            if (optie < 0 && WinterVerhaal.OUDJAAR.stap(p) == WinterVerhaal.O_VOORNEMENS) {
                WinterVerhaal.OUDJAAR.vlag(p, "gehoord", true);
                Voornemens.openSchrijven(p);
            }
        });
        Praat.luister(PAKJESAVOND_KORT, (p, spreker, optie) -> {
            if (optie < 0) {
                WinterVerhaal.SINT.verder(p, WinterVerhaal.S_PAKJESAVOND);
            }
        });
    }

    // =====================================================================================================================
    // building a talk
    // =====================================================================================================================

    /** A page: "speler" = the player, "" = the narrator, else a plein guh (the real one when near, else its portrait). */
    static Praat.Regel r(ServerPlayer p, String wie, String key, Object... args) {
        if (wie.equals("speler")) {
            return new Praat.Regel(p, "", P + key, args);
        }
        if (wie.isEmpty()) {
            return new Praat.Regel(null, VERTELLER, P + key, args);
        }
        Entity e = null;
        if (p.level() instanceof ServerLevel sl) {
            Entity echt = GuhwinterNpcs.get(sl, wie);
            if (echt != null && echt.distanceTo(p) < 10) {
                e = echt;
            }
        }
        return new Praat.Regel(e, NAAM + wie, P + key, args);
    }

    static Praat.Optie optie(int id, String key) {
        return new Praat.Optie(id, P + key);
    }

    private static void toon(ServerPlayer p, String sleutel, List<Praat.Regel> regels, Praat.Optie... opties) {
        Praat.scene(p, sleutel, regels, opties);
    }

    // =====================================================================================================================
    // chapter 1 (the short form after 6 December)
    // =====================================================================================================================

    /** Pakjesavond missed (the Sint has left): Joost and Suus tell you about it, poem included. */
    static void pakjesavondKort(ServerPlayer p) {
        toon(p, PAKJESAVOND_KORT, List.of(
                r(p, "joost", "kort.0"),
                r(p, "suus", "kort.1"),
                r(p, "suus", "kort.2"),
                r(p, "joost", "kort.3"),
                r(p, "suus", "kort.4")));
    }

    // =====================================================================================================================
    // chapter 2
    // =====================================================================================================================

    /** At the little spruces outside the plein: which one? */
    static void boomKiezen(ServerPlayer p) {
        toon(p, BOOM, List.of(
                r(p, "", "boom.0"),
                r(p, "joost", "boom.1"),
                r(p, "niels", "boom.2")),
                optie(0, "boom.klein"), optie(1, "boom.groot"));
    }

    private static void boomGekozen(ServerPlayer p, int optie) {
        toon(p, BOOM_GEKOZEN, List.of(
                r(p, "niels", optie == 0 ? "boom.klein.0" : "boom.groot.0"),
                r(p, "joost", "boom.3"),
                r(p, "", "boom.4")));
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(p, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, false, false, p.getX(), p.getY() + 1, p.getZ(), 12, 1.2, 0.8, 1.2, 0.1);
        }
    }

    /** At the tree: Suus hangs a bauble that looks a lot like a ring box; then your drawing (or a plain bauble). */
    static void versieren(ServerPlayer p) {
        toon(p, VERSIEREN, List.of(
                r(p, "corine", "versieren.0"),
                r(p, "suus", "versieren.1"),
                r(p, "joost", "versieren.2"),
                r(p, "suus", "versieren.3"),
                r(p, "evi", "versieren.4")),
                optie(1, "versieren.tekening"), optie(0, "versieren.bal"));
    }

    /** Opa arrives: "Dit is opa Jochum!" "...Wijnand." */
    static void opa(ServerPlayer p) {
        toon(p, OPA, List.of(
                r(p, "corine", "opa.0"),
                r(p, "wijnand", "opa.1"),
                r(p, "corine", "opa.2"),
                r(p, "krijn", "opa.3"),
                r(p, "marrie", "opa.4"),
                r(p, "wijnand", "opa.5")),
                optie(0, "opa.wijnand"), optie(1, "opa.jochum"));
    }

    private static void opaAntwoord(ServerPlayer p, int optie) {
        List<Praat.Regel> regels = new ArrayList<>();
        regels.add(r(p, "speler", optie == 0 ? "opa.wijnand.0" : "opa.jochum.0"));
        regels.add(r(p, "wijnand", optie == 0 ? "opa.wijnand.1" : "opa.jochum.1"));
        regels.add(r(p, "corine", "opa.6"));
        regels.add(r(p, "wijnand", "opa.7"));
        toon(p, OPA_ANTWOORD, regels);
    }

    // =====================================================================================================================
    // chapter 3
    // =====================================================================================================================

    /** Lin and the airfryer (the churro incident); then you choose the pan or the airfryer. */
    static void lin(ServerPlayer p) {
        toon(p, LIN, List.of(
                r(p, "liek", "lin.0"),
                r(p, "lin", "lin.1"),
                r(p, "liek", "lin.2"),
                r(p, "lin", "lin.3"),
                r(p, "lin", "lin.4"),
                r(p, "joost", "lin.5"),
                r(p, "niels", "lin.6")),
                optie(0, "lin.pan"), optie(1, "lin.airfryer"));
    }

    /** The game: the score comes back once; -1 (closed at once or left) = try again later. */
    static void bakken(ServerPlayer p, boolean airfryer) {
        if (Minigames.busyElsewhere(p, Oliebollen.MINIGAME)) {
            p.sendSystemMessage(Component.translatable("guhwinter.verhaal.bezig").withStyle(ChatFormatting.GRAY));
            return;
        }
        Oliebollen.start(p, airfryer, score -> {
            if (p.hasDisconnected() || WinterVerhaal.OUDJAAR.stap(p) != WinterVerhaal.O_OLIEBOLLEN) {
                return;
            }
            if (score < 0) {
                toon(p, LIN_NOG_EEN, List.of(r(p, "lin", "lin.nog")), optie(0, "lin.pan"), optie(1, "lin.airfryer"));
                return;
            }
            proeven(p, score, airfryer);
        });
    }

    /** What your oliebollen were like (by score), then everyone tastes Lin's: the tastiest. */
    static void proeven(ServerPlayer p, int score, boolean airfryer) {
        String band = score >= 80 ? "top" : score >= 50 ? "goed" : score >= 20 ? "raar" : "vulkaan";
        String wie = switch (band) {
            case "top" -> "joost";
            case "goed" -> "evi";
            case "raar" -> "niels";
            default -> "liek";
        };
        List<Praat.Regel> regels = new ArrayList<>();
        regels.add(r(p, "", airfryer ? "proef.airfryer" : "proef.pan", score));
        regels.add(r(p, wie, "proef." + band));
        regels.add(r(p, "joost", "proef.0"));
        regels.add(r(p, "evi", "proef.1"));
        regels.add(r(p, "lin", "proef.2"));
        regels.add(r(p, "liek", "proef.3"));
        regels.add(r(p, "niels", "proef.4"));
        toon(p, PROEVEN, regels);
        Guhwinter.geefPepernoten(p, Math.max(1, score / 20));
    }

    /** Goede voornemens: each of the six, then you write your own card for the prikbord. */
    static void voornemens(ServerPlayer p) {
        toon(p, VOORNEMENS, List.of(
                r(p, "evi", "voornemen.0"),
                r(p, "suus", "voornemen.suus"),
                r(p, "joost", "voornemen.joost"),
                r(p, "niels", "voornemen.niels"),
                r(p, "evi", "voornemen.evi"),
                r(p, "niels", "voornemen.niels2"),
                r(p, "evi", "voornemen.evi2"),
                r(p, "lin", "voornemen.lin"),
                r(p, "liek", "voornemen.liek"),
                r(p, "lin", "voornemen.lin2"),
                r(p, "suus", "voornemen.suus2"),
                r(p, "joost", "voornemen.joost2"),
                r(p, "suus", "voornemen.suus3"),
                r(p, "evi", "voornemen.kaartje")));
    }

    /** (dev) a talk by name. */
    @Nullable
    static Runnable dev(ServerPlayer p, String naam) {
        return switch (naam) {
            case "boom" -> () -> boomKiezen(p);
            case "versieren" -> () -> versieren(p);
            case "opa" -> () -> opa(p);
            case "lin" -> () -> lin(p);
            case "proeven" -> () -> proeven(p, 64, true);
            case "voornemens" -> () -> voornemens(p);
            case "pakjesavond_kort" -> () -> pakjesavondKort(p);
            default -> null;
        };
    }

    static final List<String> DEV = List.of("boom", "versieren", "opa", "lin", "proeven", "voornemens", "pakjesavond_kort");
}
