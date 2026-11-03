package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.erfenis.ErfenisDeco;
import nl.juiced.guhs.feature.erfenis.ErfenisEconomie;
import nl.juiced.guhs.feature.erfenis.ErfenisHulp;

/**
 * De Erfenis: "HANDJE HELPEN" (the map's old "Aanvallen" button; ontwerp rounds 7 + 12, balans.md section 7). Once per day per
 * village of a REAL player (not your own, not an NPC village) you either speed up a running build/production by
 * {@value #MINUTEN} minutes (kern: at most 25% of that job, {@link ErfenisHulp#versnel}) or give one of their orders +20% Ƒ.
 * The helper gets 20 Ƒ x their own tier + 1 burenstempel, for the first {@value #BELOOND_PER_DAG} villages a day; 10 stamps
 * give the Burenhulp-bloempot, then every 25 more the next burenstuk. The helped player sees "X hielp je een handje!" (at
 * once, or at their next login).
 */
public final class Handje {
    public static final int MINUTEN = 15, BELOOND_PER_DAG = 5, EERSTE_STUK = 10, ELKE = 25;
    public static final int BOUW = 0, BESTELLING = 1;
    /** The burenstukken in order: the first at 10 stamps, then one every 25 (the last one repeats). */
    public static final String[] BURENSTUKKEN = {"burenhulp_bloempot", "burenhulp_bankje", "burenhulp_vlaggetjes"};

    private Handje() {
    }

    /** A new day: forget whom you helped yesterday. */
    static void vandaag(BestelData.Speler s, long nu) {
        long dag = Bestellingen.dag(nu);
        if (s.hulpDag != dag) {
            s.hulpDag = dag;
            s.hulpBeloond = 0;
            s.geholpen.clear();
        }
    }

    /** The burenstuk earned with exactly this many stamps (10, 35, 60, ...), or null. */
    @Nullable
    public static String burenstuk(int stempels) {
        if (stempels < EERSTE_STUK || (stempels - EERSTE_STUK) % ELKE != 0) {
            return null;
        }
        int i = (stempels - EERSTE_STUK) / ELKE;
        return BURENSTUKKEN[Math.min(i, BURENSTUKKEN.length - 1)];
    }

    /** What one help did for the helper (pure; for the tests too). */
    public record Beloning(int guhlden, boolean stempel, @Nullable String stuk) {
    }

    /**
     * Books a help of {@code helper} at village {@code dorp} (already checked: allowed and it worked). Returns the helper's
     * reward: nothing extra after the first {@value #BELOOND_PER_DAG} villages of the day.
     */
    public static Beloning boek(BestelData.Speler helper, UUID dorp, int helperHoeve, long nu) {
        vandaag(helper, nu);
        helper.geholpen.add(dorp);
        if (helper.hulpBeloond >= BELOOND_PER_DAG) {
            return new Beloning(0, false, null);
        }
        helper.hulpBeloond++;
        helper.stempels++;
        return new Beloning(20 * Bestellingen.tier(helperHoeve), true, burenstuk(helper.stempels));
    }

    /** Whether {@code helper} may still help {@code dorp} today. */
    public static boolean mag(BestelData.Speler helper, UUID dorp, long nu) {
        vandaag(helper, nu);
        return !helper.geholpen.contains(dorp);
    }

    /** The map's "Handje helpen" with choice {@link #BOUW} or {@link #BESTELLING}. */
    static void help(ServerPlayer p, UUID dorp, int keuze) {
        MinecraftServer server = p.level().getServer();
        UUID helper = p.getUUID();
        KaartDorp doel = null;
        for (KaartDorp d : KaartFeature.BRON.spelerDorpen(server, System.currentTimeMillis())) {
            if (d.id().equals(dorp)) {
                doel = d;
            }
        }
        if (doel == null || doel.npc()) {
            p.sendSystemMessage(Component.translatable("erfenis.kaart.hulp.alleen_spelers"));
            return;
        }
        if (dorp.equals(helper)) {
            p.sendSystemMessage(Component.translatable("erfenis.kaart.hulp.zelf"));
            return;
        }
        BestelData data = BestelData.get(server);
        BestelData.Speler hs = data.van(helper);
        long nu = System.currentTimeMillis();
        if (!mag(hs, dorp, nu)) {
            p.sendSystemMessage(Component.translatable("erfenis.kaart.hulp.al_gedaan", doel.eigenaar()));
            return;
        }
        String wat;
        if (keuze == BOUW) {
            int min = ErfenisHulp.versnel(server, dorp, MINUTEN);
            if (min <= 0) {
                p.sendSystemMessage(Component.translatable("erfenis.kaart.hulp.niks_bezig", doel.eigenaar()));
                return;
            }
            wat = "bouw:" + min;
        } else {
            BestelData.Speler os = Bestellingen.bij(server, dorp);
            if (!Bestellingen.geefHulp(os, p.getGameProfile().name())) {
                p.sendSystemMessage(Component.translatable("erfenis.kaart.hulp.geen_bestelling", doel.eigenaar()));
                return;
            }
            wat = "bestelling";
        }
        Beloning bel = boek(hs, dorp, KaartFeature.BRON.hoeveLevel(server, helper), nu);
        data.setDirty();
        if (bel.guhlden() > 0) {
            ErfenisEconomie.geef(server, helper, "guhlden", bel.guhlden());
        }
        if (bel.stuk() != null) {
            ErfenisDeco.geef(server, helper, bel.stuk());
        }
        // the helper
        p.sendSystemMessage(wat.startsWith("bouw:")
                ? Component.translatable("erfenis.kaart.hulp.gedaan_bouw", doel.eigenaar(), wat.substring(5))
                : Component.translatable("erfenis.kaart.hulp.gedaan_bestelling", doel.eigenaar()));
        p.sendSystemMessage(bel.stempel()
                ? Component.translatable("erfenis.kaart.hulp.beloning", bel.guhlden(), hs.stempels)
                : Component.translatable("erfenis.kaart.hulp.geen_beloning", BELOOND_PER_DAG));
        if (bel.stuk() != null) {
            p.sendSystemMessage(Component.translatable("erfenis.kaart.hulp.burenstuk", hs.stempels,
                    Component.translatable("erfenis.kaart.deco." + bel.stuk())));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
        // the helped one
        String bericht = p.getGameProfile().name() + "|" + wat;
        ServerPlayer eigenaar = server.getPlayerList().getPlayer(dorp);
        if (eigenaar != null) {
            meld(eigenaar, bericht);
        } else {
            data.van(dorp).berichten.add(bericht);
            data.setDirty();
        }
    }

    /** "X hielp je een handje!" from a stored line {@code naam|bouw:<min>} or {@code naam|bestelling}. */
    static void meld(ServerPlayer p, String bericht) {
        int i = bericht.indexOf('|');
        String naam = i < 0 ? bericht : bericht.substring(0, i), wat = i < 0 ? "" : bericht.substring(i + 1);
        p.sendSystemMessage(wat.startsWith("bouw:")
                ? Component.translatable("erfenis.kaart.hulp.ontvangen_bouw", naam, wat.substring(5))
                : Component.translatable("erfenis.kaart.hulp.ontvangen_bestelling", naam));
    }

    /** At login: the help that came while you were away. */
    static void bijInloggen(ServerPlayer p) {
        BestelData data = BestelData.get(p.level().getServer());
        BestelData.Speler s = data.bestaand(p.getUUID());
        if (s == null || s.berichten.isEmpty()) {
            return;
        }
        for (String b : s.berichten) {
            meld(p, b);
        }
        s.berichten.clear();
        data.setDirty();
    }
}
