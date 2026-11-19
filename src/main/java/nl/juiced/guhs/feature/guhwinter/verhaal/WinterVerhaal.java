package nl.juiced.guhs.feature.guhwinter.verhaal;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.GuhwinterFase;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Guhwinter 1.10.10, slice "verhaal": the winter story in three chapters (design guhswinter/GUHSWINTER.md, R3/R4/R8/R10),
 * played once per player with the six main guhs. Each chapter is a {@link Verhaallijn} of the verhaal engine (group
 * "guhwinter"), so the Guhdex tab Verhalen, the objective line on the screen ("➜ ...") and the Superkompas entry "Mijn
 * verhaal" follow it by themselves.
 * <ol>
 *   <li>{@link #SINT} "Verdwaalde Sint" (from the start): intocht, Ozovads' trail, the lost pakjes, "dan vieren we het
 *       hier", the schoentje, pakjesavond. Reward: the Pakjesstapel. After 6 December (the Sint has left) it still works:
 *       the Sint scenes play as a flashback (the cutscenes have their own Sint), the schoentje is filled at once and
 *       pakjesavond is told by Joost and Suus.</li>
 *   <li>{@link #KERST} "Kerst bij Papa &amp; Mama" (from 6 December): boom halen, versieren, the kerstwandeling with the
 *       waterhoen and the alpine hoentje, opa "Jochum", the gourmet dinner. Reward: the Kerststukje.</li>
 *   <li>{@link #OUDJAAR} "Oud &amp; Nieuw" (from 26 December): oliebollen with Lin's airfryer, goede voornemens and your own
 *       card on the prikbord. Reward: the Oliebollenkraam.</li>
 * </ol>
 * Every chapter can be done until 3 January. The steps: {@link VerhaalStappen}; scenes: {@link WinterScenes},
 * {@link WinterGesprek}, {@link Wandeling}; the prikbord: {@link Voornemens}; dev: {@link WinterVerhaalCommando}.
 */
public final class WinterVerhaal {
    public static final String GROEP = "guhwinter";

    /** The three chapters and when they open. */
    public enum Hoofdstuk {
        SINT("guhwinter_sint", 6),
        KERST("guhwinter_kerst", 6),
        OUDJAAR("guhwinter_oudjaar", 2);

        public final String id;
        public final int stappen;

        Hoofdstuk(String id, int stappen) {
            this.id = id;
            this.stappen = stappen;
        }

        /** Can this chapter be played in this phase? Sint all event long, Kerst from 6 Dec, Oud &amp; Nieuw from 26 Dec; never after 3 Jan. */
        public boolean open(GuhwinterFase f) {
            if (!f.bezig()) {
                return false;
            }
            return switch (this) {
                case SINT -> true;
                case KERST -> f.kerst() || f.oudjaar();
                case OUDJAAR -> f.oudjaar();
            };
        }

        public Verhaallijn lijn() {
            return switch (this) {
                case SINT -> WinterVerhaal.SINT;
                case KERST -> WinterVerhaal.KERST;
                case OUDJAAR -> WinterVerhaal.OUDJAAR;
            };
        }

        @Nullable
        public static Hoofdstuk van(String naam) {
            for (Hoofdstuk h : values()) {
                if (h.name().equalsIgnoreCase(naam) || h.id.equals(naam)) {
                    return h;
                }
            }
            return null;
        }
    }

    // --- the steps of chapter 1 ------------------------------------------------------------------------------------------
    public static final int S_INTOCHT = 0, S_OZOVADS = 1, S_PAKJES = 2, S_TERUG = 3, S_SCHOEN = 4, S_PAKJESAVOND = 5;
    // --- chapter 2
    public static final int K_BOOM = 0, K_SPARREN = 1, K_VERSIEREN = 2, K_WANDELING = 3, K_OPA = 4, K_DINER = 5;
    // --- chapter 3
    public static final int O_OLIEBOLLEN = 0, O_VOORNEMENS = 1;

    /** How many of the lost pakjes you need to find (of the pakje_verstop_* spots). */
    public static final int PAKJES_NODIG = 5;
    /** Pepernoten for finishing a chapter. */
    public static final int NOTEN_SINT = 15, NOTEN_KERST = 15, NOTEN_OUDJAAR = 10;

    public static Verhaallijn SINT, KERST, OUDJAAR;

    private WinterVerhaal() {
    }

    // =====================================================================================================================
    // registration (GuhwinterFeature: one line each)
    // =====================================================================================================================

    public static void register(IEventBus modBus) {
        VerhaalBlokken.register(modBus);
        SINT = lijn(Hoofdstuk.SINT, "guhs:guhwinter_pakjesstapel", (p, stap) -> sleutelSint(p, stap), "5_wacht", "5_kort")
                .nodig((p, stap) -> stap == S_PAKJES ? List.of(Verhaallijn.nodig("guhs:guhwinter_pakjesstapel", "gui.guhs.verhalen.guhwinter_sint.nodig.pakjes",
                        VerhaalStappen.pakjesGevonden(p), PAKJES_NODIG)) : List.of())
                .doel(WinterVerhaal::doelSint).registreer();
        KERST = lijn(Hoofdstuk.KERST, "guhs:guhwinter_kerststukje", (p, stap) -> String.valueOf(stap)).doel(WinterVerhaal::doelKerst).registreer();
        OUDJAAR = lijn(Hoofdstuk.OUDJAAR, "guhs:guhwinter_oliebollenkraam", (p, stap) -> String.valueOf(stap)).doel(WinterVerhaal::doelOudjaar).registreer();
        for (Hoofdstuk h : Hoofdstuk.values()) {
            h.lijn().opStap((p, oud, nieuw) -> {
                if (nieuw >= h.stappen && oud < h.stappen) {
                    beloon(p, h);
                }
            });
        }
        WinterScenes.register();
        WinterGesprek.register();
        VerhaalStappen.register();
        Wandeling.register();
        Voornemens.register();
        WinterVerhaalCommando.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
        WinterVerhaalPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        VerhaalBlokken.creative(output);
    }

    /** A line of the chapter; extra sleutels: "wacht" (not open yet), "voorbij" (the event is over) plus the given ones. */
    private static Verhaallijn.Builder lijn(Hoofdstuk h, String icoon, java.util.function.BiFunction<ServerPlayer, Integer, String> sleutel, String... extra) {
        String[] alle = new String[extra.length + 2];
        alle[0] = "wacht";
        alle[1] = "voorbij";
        System.arraycopy(extra, 0, alle, 2, extra.length);
        return Verhaallijn.maak(h.id, GROEP).stappen(h.stappen).icoon(icoon).doelregel(true)
                .sleutel((p, stap) -> {
                    if (stap >= h.stappen) {
                        return "klaar";
                    }
                    GuhwinterFase f = fase(p);
                    if (!f.bezig()) {
                        return "voorbij";
                    }
                    if (!h.open(f)) {
                        return "wacht";
                    }
                    return sleutel.apply(p, stap);
                })
                .extraSleutels(alle)
                .beloningen(p -> List.of(Verhaallijn.beloning(icoon, h.lijn() != null && h.lijn().klaar(p))));
    }

    private static String sleutelSint(ServerPlayer p, int stap) {
        if (stap == S_PAKJESAVOND && fase(p).ordinal() < GuhwinterFase.PAKJESAVOND.ordinal()) {
            return "5_wacht";
        }
        if (stap == S_PAKJESAVOND && !fase(p).sint()) {
            return "5_kort";
        }
        return String.valueOf(stap);
    }

    public static GuhwinterFase fase(ServerPlayer p) {
        return Guhwinter.fase(p.level().getServer());
    }

    /** Is this chapter playable for this player right now? */
    public static boolean open(ServerPlayer p, Hoofdstuk h) {
        return h.open(fase(p));
    }

    /** The Sint and his company have left (6 Dec): the Sint chapter plays as a flashback / in its short form. */
    public static boolean terugblik(MinecraftServer server) {
        GuhwinterFase f = Guhwinter.fase(server);
        return !f.sint();
    }

    // =====================================================================================================================
    // goals (Superkompas "Mijn verhaal")
    // =====================================================================================================================

    @Nullable
    private static Doel plek(String anker, String tekst) {
        GuhwinterAnkers.Anker a = GuhwinterAnkers.zoek(anker);
        return a == null ? null : Doel.plek(ModDimensions.GUHMENSION, a.pos(), Component.translatable("guhwinter.verhaal.doel." + tekst));
    }

    @Nullable
    private static Doel doelSint(ServerPlayer p, int stap) {
        if (!open(p, Hoofdstuk.SINT)) {
            return null;
        }
        return switch (stap) {
            case S_INTOCHT -> plek("kade_boot", "plein");
            case S_OZOVADS -> plek("paard_verstop", "spoor");
            case S_PAKJES -> plek("plein_midden", "pakjes");
            case S_TERUG -> plek("kade_boot", "boot");
            case S_SCHOEN -> plek("schoorsteen_schoen", "schoen");
            default -> terugblik(p.level().getServer()) ? plek("npc_joost", "joost") : plek("huis_tafel_midden", "huis");
        };
    }

    @Nullable
    private static Doel doelKerst(ServerPlayer p, int stap) {
        if (!open(p, Hoofdstuk.KERST)) {
            return null;
        }
        return switch (stap) {
            case K_BOOM -> plek("npc_joost", "joost");
            case K_SPARREN -> plek("paard_verstop", "sparren");
            case K_VERSIEREN -> plek("kerstboom_voor", "boom");
            case K_WANDELING -> plek("wandel_0", "vijver");
            case K_OPA -> plek("npc_corine", "huis");
            default -> plek("huis_tafel_midden", "tafel");
        };
    }

    @Nullable
    private static Doel doelOudjaar(ServerPlayer p, int stap) {
        if (!open(p, Hoofdstuk.OUDJAAR)) {
            return null;
        }
        return stap == O_OLIEBOLLEN ? plek("npc_lin", "lin") : plek("prikbord", "prikbord");
    }

    // =====================================================================================================================
    // rewards
    // =====================================================================================================================

    /** A chapter is done: its deco block (once) and a handful of pepernoten. */
    static void beloon(ServerPlayer p, Hoofdstuk h) {
        if (!h.lijn().eenmalig(p, "beloning")) {
            return;
        }
        ItemStack blok = switch (h) {
            case SINT -> new ItemStack(VerhaalBlokken.PAKJESSTAPEL_ITEM.get());
            case KERST -> new ItemStack(VerhaalBlokken.KERSTSTUKJE_ITEM.get());
            case OUDJAAR -> new ItemStack(VerhaalBlokken.OLIEBOLLENKRAAM_ITEM.get());
        };
        int noten = switch (h) {
            case SINT -> NOTEN_SINT;
            case KERST -> NOTEN_KERST;
            case OUDJAAR -> NOTEN_OUDJAAR;
        };
        Component naam = blok.getHoverName();
        if (!p.getInventory().add(blok)) {
            p.drop(blok, false);
        }
        Guhwinter.geefPepernoten(p, noten);
        p.sendSystemMessage(Component.translatable("guhwinter.verhaal.klaar." + h.name().toLowerCase(java.util.Locale.ROOT),
                naam.copy().withStyle(ChatFormatting.LIGHT_PURPLE), noten).withStyle(ChatFormatting.GOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
    }
}
