package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.erfenis.ErfenisDeco;
import nl.juiced.guhs.feature.erfenis.ErfenisEconomie;
import nl.juiced.guhs.feature.erfenis.sherlock.Voortgang.Status;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaak.Getuige;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaak.Kaart;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The Sherlock story of De Erfenis (1.9): the prologue and eleven linear chapters, started only at fixed moments
 * ({@link #opMoment}, from ErfenisMomenten). The player is the detective; Sherlock Guh (sleepy, bubble pipe) is the mentor
 * and Dokter Vadson keeps the Speurboekje. Clues are found with the {@link Vergrootglas} ({@link Sporen}), witnesses are
 * interrogated, and on the speurbord three matching cards are pinned to one suspect: wrong is never punished. The culprit
 * is always something sweet. The finale is the courtroom ({@link Rechtbank}).
 * <p>
 * API for the other slices: {@link #start}, {@link #openBureau} (the Detectivebureau), {@link #zaakKlaar} (the magnifier
 * bubble above the bureau in the overview), {@link #beloningen} (deco and reward flags), {@link Guhrrit} (the tutorial's makelaar).
 */
public final class Sherlock {
    /** (optional, kern) where the Detectivebureau of a player stands; unused when null. */
    public static BiFunction<MinecraftServer, UUID, BlockPos> bureauPlek = (s, u) -> null;

    static final String GETUIGE = "sherlock_getuige", VADSON = "sherlock_vadson", TERUG_NAAR_BORD = "sherlock_terugbord";
    private static final int GEZIEN = 1, WAAR = 2, TOON = 3, BORD = 4, DOEI = 9;
    /** The last tick a player used the glass (a click on a block can also arrive as a use of the item). */
    private static final Map<UUID, Long> GESPEURD = new ConcurrentHashMap<>();

    private Sherlock() {
    }

    // =====================================================================================================================
    // API
    // =====================================================================================================================

    /** Starts (or, when it runs, opens the board of) this case for the player; a solved one opens its board to read back. */
    public static void start(ServerPlayer p, String zaakId) {
        Zaak z = Zaken.van(zaakId);
        if (z == null) {
            return;
        }
        MinecraftServer server = p.level().getServer();
        Voortgang v = SherlockData.van(server, p.getUUID());
        switch (v.status(z)) {
            case OPGELOST -> stuurBord(p, z);
            case BEZIG -> {
                if (z.finale()) {
                    Rechtbank.begin(p);
                } else {
                    stuurBord(p, z);
                }
            }
            case WACHT -> p.sendSystemMessage(Component.translatable("sherlock.bericht.wacht", titel(z), Component.translatable("sherlock.zaak." + z.id() + ".wacht"))
                    .withStyle(ChatFormatting.GRAY));
            case KLAAR -> {
                v.wisZaak();
                v.actief = z.id();
                SherlockData.veranderd(server);
                Sporen.vergeet(p.getUUID());
                geefGlas(p, v);
                if (z.finale()) {
                    Rechtbank.begin(p);
                    return;
                }
                Gesprek.scene(p, "zaak." + z.id() + ".intro");
                p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
            }
        }
    }

    /** The Detectivebureau: the Speurboekje with every chapter (and a new vergrootglas when you lost yours). */
    public static void openBureau(ServerPlayer p) {
        Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
        geefGlas(p, v);
        stuurBoekje(p);
    }

    /** Is a case waiting in the bureau's letterbox (the magnifier bubble above the bureau in the overview)? */
    public static boolean zaakKlaar(MinecraftServer server, UUID speler) {
        Voortgang v = SherlockData.van(server, speler);
        Zaak h = v.huidige();
        return h != null && v.status(h) == Status.KLAAR;
    }

    /** The deco ids and reward flags this player got from the story (e.g. "hartjesnest", "recept_theetaartje", "oma_bestelling"). */
    public static Set<String> beloningen(MinecraftServer server, UUID speler) {
        return Set.copyOf(SherlockData.van(server, speler).beloningen);
    }

    public static boolean opgelost(MinecraftServer server, UUID speler, String zaakId) {
        return SherlockData.van(server, speler).isOpgelost(zaakId);
    }

    // =====================================================================================================================
    // the fixed moments
    // =====================================================================================================================

    static void opMoment(ServerPlayer p, String moment) {
        MinecraftServer server = p.level().getServer();
        Voortgang v = SherlockData.van(server, p.getUUID());
        Zaak h = v.huidige();
        Status voor = h == null ? null : v.status(h);
        String[] d = moment.split(":");
        switch (d[0]) {
            case "vondst" -> {
                if (!v.sleuteltje) {
                    v.sleuteltje = true;
                    Gesprek.scene(p, "proloog");
                }
            }
            case "gebouw_gebouwd" -> {
                if (d.length > 1) {
                    v.gebouwd.add(d[1]);
                    if (d[1].equals("detectivebureau") && !v.bureau) {
                        v.bureau = true;
                        geefGlas(p, v);
                        Gesprek.scene(p, "welkom");
                    }
                }
            }
            case "hoeve_level" -> v.hoeve = Math.max(v.hoeve, getal(d, 1));
            case "bestelling_geleverd" -> v.bestellingen = Math.max(v.bestellingen, getal(d, 1));
            default -> {
                return;
            }
        }
        SherlockData.veranderd(server);
        if (h != null && voor != Status.KLAAR && v.status(h) == Status.KLAAR) {
            meldKlaar(p, h);
        }
    }

    private static int getal(String[] d, int i) {
        try {
            return d.length > i ? Integer.parseInt(d[i]) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** "There is a new case in the letterbox of the Detectivebureau" (click: open the bureau). */
    static void meldKlaar(ServerPlayer p, Zaak z) {
        MutableComponent klik = Component.translatable("sherlock.bericht.klaar.klik").withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/guhs erfenis sherlock bureau")));
        p.sendSystemMessage(Component.translatable("sherlock.bericht.klaar", titel(z), klik).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    static Component titel(Zaak z) {
        return Component.translatable("sherlock.zaak." + z.id() + ".titel");
    }

    private static void geefGlas(ServerPlayer p, Voortgang v) {
        if (v.bureau && !Vergrootglas.heeft(p)) {
            ItemStack glas = new ItemStack(SherlockFeature.VERGROOTGLAS.get());
            if (!p.getInventory().add(glas)) {
                p.drop(glas, false);
            }
            p.sendSystemMessage(Component.translatable("sherlock.bericht.glas").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // =====================================================================================================================
    // the vergrootglas
    // =====================================================================================================================

    static void speur(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long vorig = GESPEURD.put(p.getUUID(), nu);
        if (vorig != null && nu - vorig < 4) {
            return;
        }
        Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
        Zaak z = v.actief == null ? null : Zaken.van(v.actief);
        if (z == null || z.finale()) {
            p.sendSystemMessage(Component.translatable("sherlock.glas.grap." + p.getRandom().nextInt(6)).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (Sporen.eigenPlot(p) == null) {
            p.sendSystemMessage(Component.translatable("sherlock.glas.niet_hier").withStyle(ChatFormatting.GRAY));
            return;
        }
        Kaart k = Sporen.gezien(p, v);
        if (k != null) {
            vondst(p, z, v, k);
            return;
        }
        double d = Sporen.dichtstbij(p, v);
        String key = d < 0 ? "sherlock.glas.alles" : d < 6 ? "sherlock.glas.heet" : d < 14 ? "sherlock.glas.warm" : "sherlock.glas.koud";
        p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    /** A clue becomes a card (chat: its name and what Vadson says about it). */
    static void vondst(ServerPlayer p, Zaak z, Voortgang v, Kaart k) {
        if (!v.gevonden.add(k.id())) {
            return;
        }
        SherlockData.veranderd(p.level().getServer());
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.3f);
        Map<String, BlockPos> plekken = Sporen.eigenPlot(p) == null ? Map.of() : Sporen.plekken(p, Sporen.eigenPlot(p), z);
        BlockPos b = plekken.get(k.id());
        if (b != null) {
            p.level().sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, false, b.getX() + 0.5, b.getY() + 0.3, b.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0);
        }
        kaartBericht(p, z, k);
        Sporen.hud(p, z, v);
        if (alleKaarten(z, v)) {
            p.sendSystemMessage(Component.translatable(z.verzamelZaak() ? "sherlock.bericht.alles_verzameld" : "sherlock.bericht.alles_gevonden")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void kaartBericht(ServerPlayer p, Zaak z, Kaart k) {
        String b = "sherlock.zaak." + z.id() + ".kaart." + k.id();
        p.sendSystemMessage(Component.translatable("sherlock.bericht.kaartje", Component.translatable(b).withStyle(ChatFormatting.GOLD),
                Component.translatable(b + ".tekst")).withStyle(ChatFormatting.WHITE));
    }

    static boolean alleKaarten(Zaak z, Voortgang v) {
        return z.kaarten().stream().allMatch(k -> v.gevonden.contains(k.id()));
    }

    // =====================================================================================================================
    // every half second: glitter, the corner line, the witnesses
    // =====================================================================================================================

    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int t = server.getTickCount();
        if (t % 10 != 0) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Voortgang v = SherlockData.als(server, p.getUUID());
            Zaak z = v == null || v.actief == null ? null : Zaken.van(v.actief);
            if (z == null || z.finale()) {
                continue;
            }
            if (Vergrootglas.vast(p) && !z.glasKaarten().isEmpty()) {
                Sporen.glinster(p, v, t);
                if (t % 20 == 0 && Sporen.eigenPlot(p) != null) {
                    Sporen.hud(p, z, v);
                }
            }
            if (t % 40 == 0) {
                getuigen(p, z);
            }
        }
    }

    /** The witnesses of the running case stand at their spots while the owner is in their own village. */
    static void getuigen(ServerPlayer p, Zaak z) {
        BlockPos o = Sporen.eigenPlot(p);
        if (o == null) {
            return;
        }
        for (Getuige g : z.getuigen()) {
            if (Cast.van(p.level().getServer(), p.getUUID(), "getuige:" + g.id()) != null) {
                continue;
            }
            BlockPos b = Sporen.plek(p.level(), o, g.x(), g.z());
            Vec3 pos = Vec3.atBottomCenterOf(b);
            String naam = kindVan(g.id()) == null ? "sherlock.wie." + g.id() : null;
            Entity e = Cast.zet(p.level(), p.getUUID(), "getuige:" + g.id(), "getuige", g.kind(), pos, Cast.kijkNaar(pos, p.position()), naam, false);
            if (e != null) {
                (e instanceof GuhNpcEntity n ? n.roleData : e.getPersistentData()).putString(Cast.GETUIGE, g.id());
                (e instanceof GuhNpcEntity n ? n.roleData : e.getPersistentData()).putString(Cast.ZAAK, z.id());
            }
        }
    }

    // =====================================================================================================================
    // clicking the characters
    // =====================================================================================================================

    static void opInteractie(PlayerInteractEvent.EntityInteract event) {
        Entity e = event.getTarget();
        if (!e.entityTags().contains(Cast.TAG)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        klik(p, e);
    }

    static void klik(ServerPlayer p, Entity e) {
        UUID eigenaar = Cast.eigenaar(e);
        String rol = Cast.rol(e);
        boolean eigen = eigenaar == null || eigenaar.equals(p.getUUID()) || eigenaar.equals(Cast.IEDEREEN);
        if (!eigen) {
            Praat.open(p, e, Gesprek.VERHAAL, "sherlock.klik.ander", new Object[0], new Praat.Optie(DOEI, "sherlock.optie.doei"));
            return;
        }
        switch (rol) {
            case "getuige" -> ondervraag(p, e);
            case "sherlock" -> openBureau(p);
            case "vadson" -> vadson(p, e);
            case "guhrrit" -> Guhrrit.klik(p, e);
            case "rechtbank" -> Rechtbank.klik(p, e);
            default -> Praat.open(p, e, Gesprek.VERHAAL, "sherlock.klik." + (rol.isEmpty() ? "iemand" : rol), new Object[0],
                    new Praat.Optie(DOEI, "sherlock.optie.doei"));
        }
    }

    /** The role of the story kinds when one stands somewhere without the tag (e.g. a building's decoration). */
    static NpcRole rol(String wie) {
        return new NpcRole() {
            @Override
            public void talk(GuhNpcEntity npc, ServerPlayer player) {
                switch (wie) {
                    case "sherlock" -> openBureau(player);
                    case "vadson" -> vadson(player, npc);
                    case "guhrrit" -> Guhrrit.klik(player, npc);
                    default -> Praat.open(player, npc, Gesprek.VERHAAL, "sherlock.klik." + wie, new Object[0], new Praat.Optie(DOEI, "sherlock.optie.doei"));
                }
            }
        };
    }

    // --- interrogating a witness ---

    static void ondervraag(ServerPlayer p, Entity npc) {
        Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
        Zaak z = v.actief == null ? null : Zaken.van(v.actief);
        String g = Cast.getuige(npc);
        if (z == null || z.getuige(g) == null) {
            Praat.open(p, npc, GETUIGE, "sherlock.getuige.klaar", new Object[0], new Praat.Optie(DOEI, "sherlock.optie.doei"));
            return;
        }
        Praat.open(p, npc, GETUIGE, "sherlock.zaak." + z.id() + ".getuige." + g + ".hallo", new Object[0], vraagOpties(z, v));
    }

    private static Praat.Optie[] vraagOpties(Zaak z, Voortgang v) {
        List<Praat.Optie> o = new ArrayList<>();
        o.add(new Praat.Optie(GEZIEN, "sherlock.optie.gezien"));
        o.add(new Praat.Optie(WAAR, "sherlock.optie.waar"));
        if (!z.verzamelZaak() && z.glasKaarten().stream().anyMatch(k -> v.gevonden.contains(k.id()))) {
            o.add(new Praat.Optie(TOON, "sherlock.optie.toon"));
        }
        o.add(new Praat.Optie(DOEI, "sherlock.optie.doei"));
        return o.toArray(new Praat.Optie[0]);
    }

    static void opGetuigeAntwoord(ServerPlayer p, @Nullable Entity npc, int optie) {
        if (optie < 0 || npc == null) {
            return;
        }
        if (optie == DOEI) {
            Praat.sluit(p);
            return;
        }
        Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
        Zaak z = v.actief == null ? null : Zaken.van(v.actief);
        String g = Cast.getuige(npc);
        if (z == null || z.getuige(g) == null) {
            Praat.sluit(p);
            return;
        }
        String basis = "sherlock.zaak." + z.id() + ".getuige." + g + ".";
        String key = switch (optie) {
            case GEZIEN -> basis + "gezien";
            case WAAR -> basis + "waar";
            default -> basis + "toon";
        };
        Praat.open(p, npc, GETUIGE, key, new Object[0], vraagOpties(z, v));
        if (optie == GEZIEN) {
            for (Kaart k : z.kaarten()) {
                if (g.equals(k.getuige()) && !v.gevonden.contains(k.id())) {
                    v.gevonden.add(k.id());
                    SherlockData.veranderd(p.level().getServer());
                    kaartBericht(p, z, k);
                    p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.2f);
                    if (z.verzamelZaak() && alleKaarten(z, v)) {
                        losOp(p, z);
                    } else if (alleKaarten(z, v)) {
                        p.sendSystemMessage(Component.translatable("sherlock.bericht.alles_gevonden").withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }
        }
    }

    // --- Dokter Vadson ---

    static void vadson(ServerPlayer p, Entity npc) {
        Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
        Zaak z = v.actief == null ? null : Zaken.van(v.actief);
        if (z == null || z.finale()) {
            Zaak h = v.huidige();
            boolean klaar = h != null && v.status(h) == Status.KLAAR;
            Praat.open(p, npc, VADSON, klaar ? "sherlock.vadson.klaar" : "sherlock.vadson.rust", new Object[0],
                    new Praat.Optie(BORD, "sherlock.optie.boekje"), new Praat.Optie(DOEI, "sherlock.optie.doei"));
            return;
        }
        long glas = z.glasKaarten().stream().filter(k -> !v.gevonden.contains(k.id())).count();
        List<String> nogGetuigen = new ArrayList<>();
        for (Kaart k : z.kaarten()) {
            if (k.getuige() != null && !v.gevonden.contains(k.id())) {
                nogGetuigen.add(k.getuige());
            }
        }
        String key;
        Object[] args;
        if (glas > 0) {
            key = "sherlock.vadson.tip.sporen";
            args = new Object[] {glas};
        } else if (!nogGetuigen.isEmpty()) {
            key = "sherlock.vadson.tip.getuige";
            args = new Object[] {Component.translatable("sherlock.wie." + nogGetuigen.get(0))};
        } else {
            key = "sherlock.vadson.tip.bord";
            args = new Object[0];
        }
        Praat.open(p, npc, VADSON, key, args, new Praat.Optie(BORD, "sherlock.optie.bord"), new Praat.Optie(DOEI, "sherlock.optie.doei"));
    }

    static void opVadsonAntwoord(ServerPlayer p, @Nullable Entity npc, int optie) {
        if (optie == BORD) {
            Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
            Zaak z = v.actief == null ? null : Zaken.van(v.actief);
            if (z != null && !z.finale()) {
                stuurBord(p, z);
            } else {
                stuurBoekje(p);
            }
        } else if (optie == DOEI) {
            Praat.sluit(p);
        }
    }

    // =====================================================================================================================
    // the speurbord
    // =====================================================================================================================

    /** What the client's screen asked (BordActie). */
    static void bordActie(ServerPlayer p, String soort, String a, String b) {
        MinecraftServer server = p.level().getServer();
        Voortgang v = SherlockData.van(server, p.getUUID());
        Zaak z = v.actief == null ? null : Zaken.van(v.actief);
        switch (soort) {
            case "start" -> start(p, a);
            case "bord" -> {
                Zaak w = Zaken.van(a);
                if (w != null && (v.isOpgelost(w.id()) || w.id().equals(v.actief))) {
                    stuurBord(p, w);
                }
            }
            case "boekje" -> stuurBoekje(p);
            case "pin" -> {
                if (z != null && v.gevonden.contains(a) && z.verdachten().contains(b)) {
                    v.draadjes.put(a, b);
                    if (a.equals(v.rood)) {
                        v.rood = null;
                    }
                    SherlockData.veranderd(server);
                }
                stuurBord(p, z);
            }
            case "los" -> {
                if (z != null && v.draadjes.remove(a) != null) {
                    if (a.equals(v.rood)) {
                        v.rood = null;
                    }
                    SherlockData.veranderd(server);
                }
                stuurBord(p, z);
            }
            case "wijs" -> {
                if (z != null && z.verdachten().contains(a)) {
                    wijsAan(p, z, v, a);
                }
            }
            default -> {
            }
        }
    }

    /** The accusation: three cards on the culprit and none of them the red herring solves it; anything else gets a sweet hint. */
    static void wijsAan(ServerPlayer p, Zaak z, Voortgang v, String verdachte) {
        List<String> aanHem = new ArrayList<>();
        v.draadjes.forEach((kaart, wie) -> {
            if (wie.equals(verdachte)) {
                aanHem.add(kaart);
            }
        });
        if (aanHem.size() < 3) {
            Praat.scene(p, TERUG_NAAR_BORD, List.of(new Praat.Regel(Cast.bijSpeler(p, "vadson", 1), "", "sherlock.bord.te_weinig",
                    Component.translatable("sherlock.wie." + verdachte))));
            return;
        }
        if (!verdachte.equals(z.dader())) {
            v.foutGeraden = true;
            SherlockData.veranderd(p.level().getServer());
            Entity hij = Cast.van(p.level().getServer(), p.getUUID(), "getuige:" + verdachte);
            if (hij == null) {
                hij = kindVan(verdachte) != null ? Cast.bijSpeler(p, verdachte, 3) : null;
            }
            Praat.scene(p, TERUG_NAAR_BORD, List.of(
                    new Praat.Regel(hij, hij == null ? "sherlock.wie." + verdachte : "", "sherlock.zaak." + z.id() + ".fout." + verdachte),
                    new Praat.Regel(Cast.bijSpeler(p, "vadson", 1), "", "sherlock.bord.fout." + p.getRandom().nextInt(3))));
            return;
        }
        for (String kaart : aanHem) {
            Kaart k = z.kaart(kaart);
            if (k != null && !k.goed()) {
                v.rood = kaart;
                v.foutGeraden = true;
                SherlockData.veranderd(p.level().getServer());
                Praat.scene(p, TERUG_NAAR_BORD, List.of(new Praat.Regel(Cast.bijSpeler(p, "vadson", 1), "", "sherlock.bord.haring",
                        Component.translatable("sherlock.zaak." + z.id() + ".kaart." + kaart))));
                return;
            }
        }
        losOp(p, z);
    }

    // =====================================================================================================================
    // solved!
    // =====================================================================================================================

    /** The case is solved: the reveal, the rewards, the board into the Speurboekje, and the next chapter if its moment came. */
    static void losOp(ServerPlayer p, Zaak z) {
        MinecraftServer server = p.level().getServer();
        Voortgang v = SherlockData.van(server, p.getUUID());
        if (v.isOpgelost(z.id())) {
            return;
        }
        Map<String, String> bord = new LinkedHashMap<>(v.draadjes);
        if (z.verzamelZaak()) {
            z.kaarten().forEach(k -> bord.put(k.id(), "testament"));
        }
        v.borden.put(z.id(), bord);
        v.opgelost.add(z.id());
        boolean eersteKeer = !v.foutGeraden && !z.verzamelZaak() && !z.finale();
        if (eersteKeer) {
            v.eersteKeer.add(z.id());
        }
        v.wisZaak();
        Cast.weg(server, p.getUUID(), "getuige:");
        Sporen.vergeet(p.getUUID());
        beloon(p, z, v);
        SherlockData.veranderd(server);
        if (!z.finale()) {
            Gesprek.scene(p, "zaak." + z.id() + ".oplossing");
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.2f);
        p.sendSystemMessage(Component.translatable("sherlock.bericht.opgelost", titel(z), z.guhlden()).withStyle(ChatFormatting.GOLD));
        if (eersteKeer) {
            p.sendSystemMessage(Component.translatable("sherlock.bericht.sticker").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        String titel = Zaken.TITELS.get(z.id());
        if (titel != null) {
            p.sendSystemMessage(Component.translatable("sherlock.bericht.titel", Component.translatable("gui.guhs.titels.naam." + titel))
                    .withStyle(ChatFormatting.AQUA));
        }
        // FTB hook (later): the quests "erfenis_zaak_<nr>" / "erfenis_bezwaar" can check Sherlock.opgelost(...)
        Zaak n = v.huidige();
        if (n != null && v.status(n) == Status.KLAAR) {
            meldKlaar(p, n);
        }
    }

    private static void beloon(ServerPlayer p, Zaak z, Voortgang v) {
        MinecraftServer server = p.level().getServer();
        ErfenisEconomie.geef(server, p.getUUID(), "guhlden", z.guhlden());
        for (String b : z.beloningen()) {
            if (!v.beloningen.add(b)) {
                continue;
            }
            if (Zaken.DECO.contains(b)) {
                ErfenisDeco.geef(server, p.getUUID(), b);
                p.sendSystemMessage(Component.translatable("sherlock.bericht.deco", Component.translatable("sherlock.deco." + b)).withStyle(ChatFormatting.GREEN));
            } else if (b.equals("detective_pet")) {
                Item pet = BuiltInRegistries.ITEM.getValue(Identifier.parse("guhs:detective_pet"));
                if (pet != Items.AIR) {
                    ItemStack s = new ItemStack(pet);
                    if (!p.getInventory().add(s)) {
                        p.drop(s, false);
                    }
                    p.sendSystemMessage(Component.translatable("sherlock.bericht.pet").withStyle(ChatFormatting.GREEN));
                }
            } else {
                p.sendSystemMessage(Component.translatable("sherlock.beloning." + b).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    // =====================================================================================================================
    // to the client
    // =====================================================================================================================

    static void stuurBoekje(ServerPlayer p) {
        stuur(p, "boekje", null);
    }

    static void stuurBord(ServerPlayer p, @Nullable Zaak z) {
        stuur(p, z == null ? "boekje" : "bord", z);
    }

    /** The whole state the screen needs: every chapter's status, and the board of one case. */
    static CompoundTag staat(MinecraftServer server, UUID speler, String modus, @Nullable Zaak z) {
        Voortgang v = SherlockData.van(server, speler);
        CompoundTag t = new CompoundTag();
        t.putString("Modus", modus);
        t.putBoolean("Sleuteltje", v.sleuteltje);
        t.putBoolean("Bureau", v.bureau);
        ListTag zaken = new ListTag();
        for (Zaak w : Zaken.ALLE) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", w.id());
            c.putInt("Nr", w.nr());
            c.putString("Status", v.status(w).name());
            c.putBoolean("EersteKeer", v.eersteKeer.contains(w.id()));
            c.putBoolean("Finale", w.finale());
            zaken.add(c);
        }
        t.put("Zaken", zaken);
        if (z != null) {
            boolean klaar = v.isOpgelost(z.id());
            Map<String, String> draadjes = klaar ? v.borden.getOrDefault(z.id(), Map.of()) : v.draadjes;
            Set<String> gevonden = klaar ? new LinkedHashSet<>(draadjes.keySet()) : v.gevonden;
            if (klaar) {
                z.kaarten().forEach(k -> gevonden.add(k.id()));
            }
            CompoundTag b = new CompoundTag();
            b.putString("Zaak", z.id());
            b.putBoolean("Opgelost", klaar);
            b.putBoolean("Verzamel", z.verzamelZaak());
            b.putInt("Totaal", z.kaarten().size());
            ListTag kaarten = new ListTag();
            for (Kaart k : z.kaarten()) {
                if (gevonden.contains(k.id())) {
                    CompoundTag c = new CompoundTag();
                    c.putString("Id", k.id());
                    c.putString("Icoon", k.icoon());
                    c.putBoolean("Getuige", k.getuige() != null);
                    kaarten.add(c);
                }
            }
            b.put("Kaarten", kaarten);
            ListTag verdachten = new ListTag();
            for (String w : z.verdachten()) {
                CompoundTag c = new CompoundTag();
                c.putString("Id", w);
                c.putString("Icoon", Zaken.WIE.getOrDefault(w, "minecraft:paper"));
                verdachten.add(c);
            }
            b.put("Verdachten", verdachten);
            CompoundTag d = new CompoundTag();
            draadjes.forEach(d::putString);
            b.put("Draadjes", d);
            if (!klaar && v.rood != null) {
                b.putString("Rood", v.rood);
            }
            if (klaar && z.dader() != null) {
                b.putString("Dader", z.dader());
            }
            t.put("Bord", b);
        }
        return t;
    }

    private static void stuur(ServerPlayer p, String modus, @Nullable Zaak z) {
        ModNetworking.sendTo(p, new SherlockPayloads.Bord(staat(p.level().getServer(), p.getUUID(), modus, z)));
    }

    // =====================================================================================================================

    /** The kind of a story character by speaker id (null: not one of ours: Hendrik, the babyguhs, the narrator...). */
    @Nullable
    public static Kind kindVan(String wie) {
        return switch (wie) {
            case "sherlock" -> Kind.SHERLOCK_GUH;
            case "vadson" -> Kind.DOKTER_VADSON;
            case "guhrrit", "makelaar" -> Kind.MAKELAAR_GUHRRIT;
            case "guhrrit_vakantie" -> Kind.GUHRRIT_VAKANTIE;
            case "morris" -> Kind.MORRIS_MIKA;
            case "pim" -> Kind.PIM_MIKA;
            case "oehoe" -> Kind.OEHOE;
            case "knabbelhamer" -> Kind.KNABBELHAMER;
            default -> null;
        };
    }

    /** (dev, tests) solves the running (or the next ready) case at once, as if the culprit was pointed out. */
    static boolean losHuidigeOp(ServerPlayer p) {
        Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
        Zaak z = v.actief != null ? Zaken.van(v.actief) : v.huidige();
        if (z == null) {
            return false;
        }
        if (z.finale()) {
            Rechtbank.klaar(p, false);
        } else {
            losOp(p, z);
        }
        return true;
    }

    /** (dev) everything of this player forgotten. */
    static void reset(ServerPlayer p) {
        SherlockData.get(p.level().getServer()).vergeet(p.getUUID());
        Cast.vergeet(p.level().getServer(), p.getUUID());
        Sporen.vergeet(p.getUUID());
    }

    /** (dev) makes every moment up to this case true, solves everything before it and starts it. */
    static void forceer(ServerPlayer p, Zaak z) {
        Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
        v.bureau = true;
        v.sleuteltje = true;
        v.gebouwd.addAll(List.of("detectivebureau", "bakkerij", "weverij", "muziektent", "kaarsenmakerij"));
        v.bestellingen = Math.max(v.bestellingen, 10);
        v.hoeve = Math.max(v.hoeve, 3);
        while (!z.trigger().test(v) && v.hoeve < 10) {
            v.hoeve++;
        }
        v.wisZaak();
        Cast.weg(p.level().getServer(), p.getUUID(), "getuige:");
        for (Zaak w : Zaken.ALLE) {
            if (w == z) {
                break;
            }
            v.opgelost.add(w.id());
        }
        v.opgelost.remove(z.id());
        for (Zaak w : Zaken.ALLE.subList(Zaken.ALLE.indexOf(z), Zaken.ALLE.size())) {
            v.opgelost.remove(w.id());
        }
        SherlockData.veranderd(p.level().getServer());
        start(p, z.id());
    }
}
