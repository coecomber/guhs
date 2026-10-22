package nl.juiced.guhs.feature.safari.quests;

import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.kern.SafariEvents;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The rewards around the Safari-dex and the titles of the safari.
 * <ul>
 *   <li>hidden advancements guhs:quest/safari_* (the FTB chapter guhs_safari): the first photo, a 3-star photo, 10 and 25
 *       species, a whole zone, a hider ({@link #SPOOR}), the full dex; granted from {@link SafariEvents} and caught up on
 *       login ({@link #inhalen});</li>
 *   <li>the full dex (every species that counts, {@link SafariSpeler#telt}): the golden safari camera, the golden animal
 *       statue, the voucher for the Gouden dikdik's board (S5), the flags {@link #KONING} and {@link #GOUDEN_CAMERA};</li>
 *   <li>the titles Speurneus (all geheimpjes), Beverbaas ({@link #BEVERDAMMEN} &gt;= 5) and Safarikoning.</li>
 * </ul>
 */
public final class Beloningen {
    public static final String KONING = "safarikoning", GOUDEN_CAMERA = "gouden_camera", BEVERDAMMEN = "beverdammen";
    public static final int BEVERBAAS = 5;
    /** The hiders: a photo of one of these is the FTB quest "Spoorzoeker" (the footprint trails lead to them). */
    public static final Set<String> SPOOR = Set.of("okapi", "das", "bever", "quokka", "vuursalamander", "sabeltandtijger");

    static void register() {
        Titels.registreer(new Titels.Titel("speurneus", "gui.guhs.titels.naam.speurneus", ChatFormatting.YELLOW, "guhs:geheimpjes_trofee",
                Geheimpjes::allemaal));
        Titels.registreer(new Titels.Titel("beverbaas", "gui.guhs.titels.naam.beverbaas", ChatFormatting.GOLD, "guhs:olifantenloopband",
                p -> beverdammen(p) >= BEVERBAAS));
        Titels.registreer(new Titels.Titel(KONING, "gui.guhs.titels.naam.safarikoning", ChatFormatting.GOLD, "guhs:gouden_safaricamera",
                p -> SafariSpeler.vlag(p, KONING) || dexVol(p.level().getServer(), p.getUUID())));
        SafariEvents.opDexNieuw((server, id, soort, sterren, was) -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                dex(p, soort, sterren);
            }
        });
        SafariEvents.opZoneCompleet((server, id, zone) -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                adv(p, "safari_zone_vol");
            }
        });
    }

    /** A new or better photo of this species: the milestones, and the full dex. */
    static void dex(ServerPlayer p, String soort, int sterren) {
        int n = aantal(p.level().getServer(), p.getUUID());
        if (n >= 1) {
            adv(p, "safari_eerste_foto");
        }
        if (n >= 10) {
            adv(p, "safari_dex_10");
        }
        if (n >= 25) {
            adv(p, "safari_dex_25");
        }
        if (sterren >= 3) {
            adv(p, "safari_drie_sterren");
        }
        if (SPOOR.contains(soort)) {
            adv(p, "safari_spoorzoeker");
        }
        if (dexVol(p.level().getServer(), p.getUUID())) {
            dexVolBeloning(p);
        }
    }

    /** The full dex: once. */
    static void dexVolBeloning(ServerPlayer p) {
        adv(p, "safari_dex_vol");
        if (SafariSpeler.vlag(p, KONING)) {
            return;
        }
        SafariSpeler.zetVlag(p, KONING, true);
        SafariSpeler.zetVlag(p, GOUDEN_CAMERA, true);
        SafariQuests.zeg(p, "gui.guhs.safari.dexvol");
        SafariQuests.geef(p, new ItemStack(SafariQuests.GOUDEN_SAFARICAMERA.get()));
        SafariQuests.geef(p, new ItemStack(SafariQuests.GOUDEN_DIERENBEELD_ITEM.get()));
        SafariQuests.geef(p, new ItemStack(SafariQuests.BORDBON_GOUDEN_DIKDIK.get()));
    }

    /** How many species that count for the dex this player photographed. */
    public static int aantal(MinecraftServer server, UUID id) {
        int n = 0;
        for (String s : SafariSpeler.gefotografeerd(server, id)) {
            if (SafariSpeler.telt(s)) {
                n++;
            }
        }
        return n;
    }

    /** How many species count for the dex (46 with the park's data). */
    public static int totaal() {
        int n = 0;
        for (String s : ParkData.get().soorten.keySet()) {
            if (SafariSpeler.telt(s)) {
                n++;
            }
        }
        return n;
    }

    public static boolean dexVol(MinecraftServer server, UUID id) {
        int t = totaal();
        return t > 0 && aantal(server, id) >= t;
    }

    /** Login: the advancements of what was done before this slice (or while an event could not reach the player). */
    static void inhalen(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        int n = aantal(server, p.getUUID());
        if (n >= 1) {
            adv(p, "safari_eerste_foto");
        }
        if (n >= 10) {
            adv(p, "safari_dex_10");
        }
        if (n >= 25) {
            adv(p, "safari_dex_25");
        }
        for (String s : SafariSpeler.gefotografeerd(server, p.getUUID())) {
            if (SPOOR.contains(s)) {
                adv(p, "safari_spoorzoeker");
            }
            if (SafariSpeler.dex(server, p.getUUID(), s).sterren() >= 3) {
                adv(p, "safari_drie_sterren");
            }
        }
        if (Geheimpjes.gevonden(p) > 0) {
            adv(p, "safari_geheimpje");
        }
        if (Geheimpjes.allemaal(p)) {
            Geheimpjes.allesGevonden(p);
        }
        if (SafariSpeler.quest(server, p.getUUID(), VermomdeGuh.QUEST).equals("klaar")) {
            adv(p, "safari_vermomde_guh");
        }
        if (SafariSpeler.quest(server, p.getUUID(), VaheOlifant.QUEST).equals("klaar")) {
            adv(p, "safari_vahe_olifant");
        }
        if (dexVol(server, p.getUUID())) {
            dexVolBeloning(p);
        }
    }

    // --- Beverbaas -----------------------------------------------------------------------------------------------------------

    static int beverdam(ServerPlayer p) {
        int n = beverdammen(p) + 1;
        SafariSpeler.zetQuest(p.level().getServer(), p.getUUID(), BEVERDAMMEN, String.valueOf(n));
        SafariQuests.zeg(p, "gui.guhs.safari.beverdam", n);
        if (n >= BEVERBAAS) {
            adv(p, "bouwhulp_beverbaas");                // F13: the FTB quest "Beverbaas"
        }
        return n;
    }

    public static int beverdammen(ServerPlayer p) {
        try {
            return Integer.parseInt(SafariSpeler.quest(p.level().getServer(), p.getUUID(), BEVERDAMMEN));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Grants the hidden advancement guhs:quest/&lt;name&gt; (an FTB task). */
    public static void adv(ServerPlayer p, String name) {
        GuhAdvancements.grant(p, name);
    }

    private Beloningen() {
    }
}
