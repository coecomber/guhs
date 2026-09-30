package nl.juiced.guhs.feature.favorietjes;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandEvents;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.FavorietSoort;
import nl.juiced.guhs.feature.band.Favorieten;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisjes;

/**
 * Finding the secret favourites (2.10, favorietjes). Everything a band guh does goes over the moments bus; every moment
 * that can be a favourite is tried with {@link #probeer}:
 * <ul>
 *   <li>its favourite, not found yet, with its owner nearby: <b>ontdekt!</b> A big heart explosion, a happy "VAHOEG" sound,
 *       {@link Reden#FAVORIET_ONTDEKT} hearts, the dagboek ({@code eerste_favoriet} + {@code favorietjes_<soort>}, a
 *       wist-je-datje), the advancement, and the happy buff ({@link Band#maakBlij}) for five minutes;</li>
 *   <li>its favourite again: {@link Reden#FAVORIET} hearts, a small heart burst and two happy minutes;</li>
 *   <li>not its favourite (and that favourite not found yet): a hint to the owner, warm when it is close ({@link Hints}):
 *       "Guh kijkt nieuwsgierig..." or cold: "Guh snuffelt... njeg?".</li>
 * </ul>
 * Being AT a favourite (its favourite biome, wearing its favourite colour, next to its favourite friend) keeps a guh blij
 * ({@link #tick}); that also finds those favourites when the owner is around. Hearts only ever go up.
 */
public final class Favorietjes {
    /** Salt of the hint rolls (CONTRACT par. 2). */
    public static final long SALT = 20210701L;
    /** Hint texts per kind and temperature (lang gui.guhs.favorietjes.hint.&lt;warm|koud&gt;.&lt;soort&gt;.&lt;n&gt;). */
    public static final int HINT_VARIANTEN = 3;
    /** How long the happy buff lasts: a new favourite, the favourite again, being at a favourite (refreshed every 5 s). */
    public static final int BLIJ_ONTDEKT = 6000, BLIJ_WEER = 2400, BLIJ_BIJ = 300;
    /** The owner must be this close to see it (and a favourite must be seen to be found). */
    public static final double GETUIGE = 32;
    /** Rest between two hints for the same guh and kind (ticks); places and friends rest longer. */
    static final int HINT_RUST = 160, HINT_RUST_LANG = 1200, WEER_RUST = 600;

    public enum Uitkomst { ONTDEKT, WEER, BLIJ, WARM, KOUD, NIETS }

    /** band id -&gt; kind -&gt; game time until which the next hint / "again" message waits. Not saved (a restart may hint once more). */
    private static final Map<UUID, Map<FavorietSoort, Long>> HINT_TOT = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<FavorietSoort, Long>> WEER_TOT = new ConcurrentHashMap<>();

    private Favorietjes() {
    }

    // =====================================================================================================================
    // the moments
    // =====================================================================================================================

    /** The moments bus: every moment that can be a favourite. */
    static void moment(Mob guh, @Nullable ServerPlayer speler, Moment m, String waarde) {
        switch (m) {
            case GEGETEN -> probeer(guh, speler, FavorietSoort.ETEN, waarde);
            case PLEK -> probeer(guh, speler, FavorietSoort.PLEK, waarde);
            case KNUFFEL -> probeer(guh, speler, FavorietSoort.KNUFFEL, waarde);
            case LIEDJE -> probeer(guh, speler, FavorietSoort.LIEDJE, waarde);
            case SPEELTJE -> probeer(guh, speler, FavorietSoort.SPEELTJE, waarde);
            case EMOTE -> probeer(guh, speler, FavorietSoort.EMOTE, waarde);
            case KLEDING -> {
                GuhClothes stuk = GuhClothes.byId(waarde);
                String kleur = stuk == null ? null : Favorieten.kleur(stuk);
                if (kleur != null) {
                    probeer(guh, speler, FavorietSoort.KLEUR, kleur);
                }
            }
            case VRIENDJE -> probeer(guh, speler, FavorietSoort.VRIEND, waarde);
            default -> {
            }
        }
    }

    /**
     * Tries one thing against a guh's favourite of this kind (see the class comment). The owner must be nearby to find a
     * favourite or get a hint (the speler of the moment when it is the owner, else the owner within {@link #GETUIGE}).
     */
    public static Uitkomst probeer(Mob guh, @Nullable ServerPlayer speler, FavorietSoort soort, String waarde) {
        if (!Band.isBandGuh(guh) || !(guh.level() instanceof ServerLevel level) || waarde == null || waarde.isEmpty()) {
            return Uitkomst.NIETS;
        }
        String favoriet = Favorieten.waarde(guh, soort);
        if (favoriet == null) {
            return Uitkomst.NIETS;
        }
        ServerPlayer getuige = getuige(guh, speler);
        boolean ontdekt = Favorieten.ontdekt(guh, soort);
        if (favoriet.equals(waarde)) {
            if (!ontdekt && getuige != null && Favorieten.ontdek(guh, getuige, soort)) {
                ontdekt(level, guh, getuige, soort, favoriet);
                return Uitkomst.ONTDEKT;
            }
            if (ontdekt) {
                weer(level, guh, getuige, soort, favoriet);
                return Uitkomst.WEER;
            }
            Band.maakBlij(guh, BLIJ_WEER);   // nobody saw it, but it is happy anyway
            return Uitkomst.BLIJ;
        }
        if (ontdekt || getuige == null) {
            return Uitkomst.NIETS;
        }
        return hint(level, guh, getuige, soort, favoriet, waarde);
    }

    /** The owner as a witness: the given player when it is the owner (online, same level, within 32), else the nearby owner. */
    @Nullable
    static ServerPlayer getuige(Mob guh, @Nullable ServerPlayer speler) {
        UUID eigenaar = Band.eigenaar(guh);
        if (speler != null && speler.getUUID().equals(eigenaar) && speler.level() == guh.level() && speler.distanceTo(guh) <= GETUIGE) {
            return speler;
        }
        return BandEvents.nabijeEigenaar(guh, GETUIGE);
    }

    // =====================================================================================================================
    // found it!
    // =====================================================================================================================

    private static void ontdekt(ServerLevel level, Mob guh, ServerPlayer eigenaar, FavorietSoort soort, String favoriet) {
        Component naam = Favorieten.naam(soort, favoriet);
        explosie(level, guh);
        level.playSound(null, guh.blockPosition(), FavorietjesFeature.ONTDEKT_GELUID.get(), SoundSource.NEUTRAL, 1.2f, 1f);
        level.playSound(null, guh.blockPosition(), BandFeature.HARTJES_GELUID.get(), SoundSource.NEUTRAL, 1f, 1.3f);
        if (guh instanceof GuhEntity g) {
            g.triggerAnim("action", "happy");
        }
        guh.getLookControl().setLookAt(eigenaar);
        Band.geefHartjes(guh, eigenaar, Reden.FAVORIET_ONTDEKT.standaard(), Reden.FAVORIET_ONTDEKT);
        Band.maakBlij(guh, BLIJ_ONTDEKT);
        eigenaar.sendSystemMessage(Component.translatable("gui.guhs.favorietjes.ontdekt." + soort.id(), guh.getDisplayName(),
                naam.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).withStyle(ChatFormatting.LIGHT_PURPLE));
        eigenaar.sendOverlayMessage(Component.translatable("gui.guhs.favorietjes.ontdekt_titel").withStyle(ChatFormatting.LIGHT_PURPLE,
                ChatFormatting.BOLD));
        Dagboek.eersteKeer(guh, eigenaar, "eerste_favoriet");
        Dagboek.eersteKeer(guh, eigenaar, "favorietjes_fav_" + soort.id());
        Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.favorietjes.ontdekt_" + soort.id(), naam.getString());
        GidsFeature.grant(eigenaar, "lieve_vadsjes/favorietjes_eerste");
        GidsFeature.grant(eigenaar, "lieve_vadsjes/favorietjes_" + soort.id());
        Set<FavorietSoort> alle = Favorieten.ontdekt(level.getServer(), eigenaar.getUUID(), Band.id(guh));
        if (alle.size() == FavorietSoort.values().length) {
            eigenaar.sendSystemMessage(Component.translatable("gui.guhs.favorietjes.alle", guh.getDisplayName()).withStyle(ChatFormatting.GOLD));
            Dagboek.eersteKeer(guh, eigenaar, "favorietjes_alle");
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.favorietjes.alle", eigenaar.getGameProfile().name());
            GidsFeature.grant(eigenaar, "lieve_vadsjes/favorietjes_alle");
            level.sendParticles(BandFeature.GROOT_HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 1.2, guh.getZ(), 1, 0, 0, 0, 0);
        }
    }

    /** The big heart explosion (one emitter particle; the client makes the hearts and sparkles), plus a few for everyone. */
    public static void explosie(ServerLevel level, Mob guh) {
        double y = guh.getY() + guh.getBbHeight() * 0.7;
        level.sendParticles(FavorietjesFeature.EXPLOSIE.get(), guh.getX(), y, guh.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), y, guh.getZ(), 12, guh.getBbWidth() * 0.8, 0.5, guh.getBbWidth() * 0.8, 0.12);
    }

    /** Its favourite again: hearts, a little burst and two happy minutes. */
    private static void weer(ServerLevel level, Mob guh, @Nullable ServerPlayer eigenaar, FavorietSoort soort, String favoriet) {
        Band.maakBlij(guh, BLIJ_WEER);
        if (eigenaar == null) {
            return;
        }
        Band.geefHartjes(guh, eigenaar, Reden.FAVORIET.standaard(), Reden.FAVORIET);
        long nu = level.getGameTime();
        Map<FavorietSoort, Long> tot = WEER_TOT.computeIfAbsent(Band.id(guh), k -> new EnumMap<>(FavorietSoort.class));
        if (tot.getOrDefault(soort, 0L) > nu) {
            return;
        }
        tot.put(soort, nu + WEER_RUST);
        level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() * 0.8, guh.getZ(), 8,
                guh.getBbWidth() * 0.6, 0.3, guh.getBbWidth() * 0.6, 0.06);
        level.sendParticles(FavorietjesFeature.GLINSTER.get(), guh.getX(), guh.getY() + guh.getBbHeight() * 0.8, guh.getZ(), 6,
                guh.getBbWidth() * 0.6, 0.3, guh.getBbWidth() * 0.6, 0.02);
        level.playSound(null, guh.blockPosition(), BandFeature.HARTJES_GELUID.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        eigenaar.sendOverlayMessage(Component.translatable("gui.guhs.favorietjes.weer." + soort.id(), guh.getDisplayName(),
                Favorieten.naam(soort, favoriet)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // warm / cold
    // =====================================================================================================================

    private static Uitkomst hint(ServerLevel level, Mob guh, ServerPlayer eigenaar, FavorietSoort soort, String favoriet, String waarde) {
        long nu = level.getGameTime();
        UUID id = Band.id(guh);
        Map<FavorietSoort, Long> tot = HINT_TOT.computeIfAbsent(id, k -> new EnumMap<>(FavorietSoort.class));
        if (tot.getOrDefault(soort, 0L) > nu) {
            return Uitkomst.NIETS;
        }
        tot.put(soort, nu + (soort == FavorietSoort.PLEK || soort == FavorietSoort.VRIEND ? HINT_RUST_LANG : HINT_RUST));
        boolean warm = Hints.warm(level.getServer(), id, soort, favoriet, waarde);
        Random rng = new Random(SALT ^ id.getLeastSignificantBits() ^ nu * 31L ^ soort.ordinal());
        String key = "gui.guhs.favorietjes.hint." + (warm ? "warm." : "koud.") + soort.id() + "." + rng.nextInt(HINT_VARIANTEN);
        eigenaar.sendOverlayMessage(Component.translatable(key, guh.getDisplayName())
                .withStyle(warm ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
        double top = guh.getY() + guh.getBbHeight();
        if (warm) {
            level.sendParticles(FavorietjesFeature.VRAAGJE.get(), guh.getX(), top + 0.35, guh.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), top + 0.1, guh.getZ(), 2, guh.getBbWidth() * 0.3, 0.1,
                    guh.getBbWidth() * 0.3, 0.01);
            level.playSound(null, guh.blockPosition(), FavorietjesFeature.NIEUWSGIERIG_GELUID.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        } else {
            double a = Math.toRadians(guh.getYHeadRot());
            double r = guh.getBbWidth() * 0.6;
            level.sendParticles(FavorietjesFeature.SNUFFEL.get(), guh.getX() - Math.sin(a) * r, guh.getY() + guh.getBbHeight() * 0.45,
                    guh.getZ() + Math.cos(a) * r, 3, 0.08, 0.05, 0.08, 0.01);
            level.playSound(null, guh.blockPosition(), FavorietjesFeature.SNUFFEL_GELUID.get(), SoundSource.NEUTRAL, 0.7f, 1f);
        }
        guh.getLookControl().setLookAt(eigenaar);
        return warm ? Uitkomst.WARM : Uitkomst.KOUD;
    }

    // =====================================================================================================================
    // being at a favourite (every band guh, every 5 seconds)
    // =====================================================================================================================

    static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 100 == 37) {
            kijk(guh);
        }
    }

    /** Is this guh at one of its favourites right now (place, colour, friend)? Blij, and found when its owner is near. */
    static void kijk(GuhEntity guh) {
        if (!Band.isBandGuh(guh) || Huisjes.isBinnen(guh) || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        Map<FavorietSoort, String> fav = Favorieten.van(guh);
        if (fav.isEmpty()) {
            return;
        }
        ServerPlayer eigenaar = BandEvents.nabijeEigenaar(guh, 16);
        // its favourite place
        String plek = fav.get(FavorietSoort.PLEK);
        String hier = level.getBiome(guh.blockPosition()).unwrapKey().map(k -> k.identifier().toString()).orElse("");
        if (plek != null && plek.equals(hier)) {
            bij(guh, eigenaar, FavorietSoort.PLEK, plek);
        }
        // its favourite colour
        String kleur = fav.get(FavorietSoort.KLEUR);
        if (KledingKleuren.draagt(guh, kleur)) {
            bij(guh, eigenaar, FavorietSoort.KLEUR, kleur);
        }
        // its favourite friend (or, with the owner around, a hint about the others)
        String vriend = fav.get(FavorietSoort.VRIEND);
        for (GuhEntity ander : level.getEntitiesOfClass(GuhEntity.class, guh.getBoundingBox().inflate(6),
                g -> g != guh && Band.isBandGuh(g) && Band.eigenaar(g) != null && Band.eigenaar(g).equals(Band.eigenaar(guh))
                        && !Huisjes.isBinnen(g))) {
            String anderId = Band.id(ander).toString();
            if (anderId.equals(vriend)) {
                bij(guh, eigenaar, FavorietSoort.VRIEND, vriend);
                break;
            } else if (eigenaar != null && vriend != null && guh.distanceTo(ander) < 4) {
                probeer(guh, eigenaar, FavorietSoort.VRIEND, anderId);
            }
        }
    }

    /** At a favourite: blij while it lasts; with its owner around it is found (the first time) or loved again. */
    private static void bij(GuhEntity guh, @Nullable ServerPlayer eigenaar, FavorietSoort soort, String favoriet) {
        Band.maakBlij(guh, BLIJ_BIJ);
        if (eigenaar != null && !Favorieten.ontdekt(guh, soort)) {
            probeer(guh, eigenaar, soort, favoriet);
        }
    }

    /** (tests) forget the rests between hints of this guh. */
    public static void vergeetRust(Mob guh) {
        HINT_TOT.remove(Band.id(guh));
        WEER_TOT.remove(Band.id(guh));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HINT_TOT.clear();
        WEER_TOT.clear();
    }
}
