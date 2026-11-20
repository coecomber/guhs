package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.GuhwinterZone;
import nl.juiced.guhs.feature.guhwinter.tekenen.Doel;
import nl.juiced.guhs.feature.guhwinter.tekenen.Tekening;
import nl.juiced.guhs.feature.guhwinter.tekenen.Tekeningen;

/**
 * Lootjes trekken (design §0 Lootjes, R1, R5, R6, R9, R10): the public API and the whole flow.
 * <ol>
 *   <li><b>Signing up</b> at Het Grote Boek ({@link GroteBoekBlok}, {@link #openBoek}) from the start through 30 Nov 23:59; first
 *       come, first served, at most {@link #MAX}; signing out is fine until the deadline;</li>
 *   <li><b>the draw</b> on 1 Dec 00:00 ({@code trekking:lootjes}): a derangement ({@link Trekking}); each giver gets one plot
 *       and a letter from the Sint (now, or at the next login). Fewer than two names: no draw, a sweet letter and 25 pepernoten;</li>
 *   <li><b>building</b> until 5 Dec 18:00 in creative on your own plot ({@link Bouwen}), and your drawing for your lootje
 *       (from the book or the plot's sign: {@link #teken});</li>
 *   <li><b>capture</b> at 5 Dec 18:00 ({@code bouwen_dicht:lootjes}): every builder back to normal, every plot read into a gift
 *       (the builds stay standing on the plaza);</li>
 *   <li><b>pakjesavond</b> 5 Dec 19:00 ({@code pakjesavond:lootjes}): bonk bonk bonk! A Zak van Sinterklaas with the Surprise
 *       and the drawing for everyone (online now, else at the next login; a full bag waits, nothing on the ground).</li>
 * </ol>
 * For the story: {@link #isIngeschreven}, {@link #status}, {@link #openBoek}.
 */
public final class Lootjes {
    public static final int MAX = 8;
    public static final int PEPERNOTEN_TROOST = 25;
    static final String T = "guhwinter.lootjes.";

    /** Where a player is in the lootjes. */
    public enum Status {
        /** Not signed up. */
        NIET,
        /** Signed up; the Sint hasn't drawn yet. */
        INGESCHREVEN,
        /** Has drawn a name: building and drawing time (until pakjesavond). */
        GETROKKEN,
        /** Pakjesavond is done for them (their sack came), or there was no draw and they got the Sint's letter. */
        KLAAR
    }

    /** Knocks in progress: player -> ticks since the first bonk. */
    private static final Map<UUID, Integer> KLOPPEN = new HashMap<>();
    /** When we last told someone their bag is full (so it isn't every half minute). */
    private static final Map<UUID, Long> VOL_GEZEGD = new HashMap<>();

    private Lootjes() {
    }

    // =====================================================================================================================
    // API (story)
    // =====================================================================================================================

    public static boolean isIngeschreven(ServerPlayer p) {
        return isIngeschreven(p.level().getServer(), p.getUUID());
    }

    public static boolean isIngeschreven(MinecraftServer server, UUID id) {
        return LootjesData.get(server).isIngeschreven(id);
    }

    public static Status status(ServerPlayer p) {
        return status(p.level().getServer(), p.getUUID());
    }

    public static Status status(MinecraftServer server, UUID id) {
        LootjesData d = LootjesData.get(server);
        if (!d.isIngeschreven(id)) {
            return Status.NIET;
        }
        if (d.klaar.contains(id) || d.mislukt) {
            return Status.KLAAR;
        }
        return d.getrokken && d.lootje.containsKey(id) ? Status.GETROKKEN : Status.INGESCHREVEN;
    }

    /** How many names are in Het Grote Boek. */
    public static int aantal(MinecraftServer server) {
        return LootjesData.get(server).ingeschreven.size();
    }

    /** Opens Het Grote Boek for this player (a page in the chat). */
    public static void openBoek(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        LootjesData d = LootjesData.get(server);
        p.sendSystemMessage(Component.literal(" "));
        p.sendSystemMessage(Component.translatable(T + "boek.titel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        // the hearts: one per place, pink = taken
        MutableComponent hartjes = Component.empty();
        for (int i = 0; i < MAX; i++) {
            hartjes.append(Component.literal("❤").withStyle(i < d.ingeschreven.size() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY));
        }
        p.sendSystemMessage(Component.translatable(T + "boek.teller", hartjes, d.ingeschreven.size(), MAX).withStyle(ChatFormatting.WHITE));
        if (!d.ingeschreven.isEmpty()) {
            MutableComponent namen = Component.empty();
            for (int i = 0; i < d.ingeschreven.size(); i++) {
                if (i > 0) {
                    namen.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                }
                LootjesData.Deelnemer x = d.ingeschreven.get(i);
                namen.append(Component.literal(x.naam()).withStyle(x.id().equals(p.getUUID()) ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
            }
            p.sendSystemMessage(Component.translatable(T + "boek.namen", namen).withStyle(ChatFormatting.GRAY));
        }
        boolean erin = d.isIngeschreven(p.getUUID());
        if (Guhwinter.inschrijvenOpen(server) && !d.getrokken) {
            p.sendSystemMessage(Component.translatable(T + "boek.sint").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            if (erin) {
                p.sendSystemMessage(Component.translatable(T + "boek.jij_erin").withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(" ").append(knop("boek.knop.uit", "uitschrijven", ChatFormatting.GRAY)));
            } else if (d.ingeschreven.size() >= MAX) {
                p.sendSystemMessage(Component.translatable(T + "boek.vol").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                p.sendSystemMessage(knop("boek.knop.in", "inschrijven", ChatFormatting.GREEN));
            }
            p.sendSystemMessage(Component.translatable(T + "boek.wanneer").withStyle(ChatFormatting.GRAY));
            return;
        }
        // after the deadline
        if (!d.getrokken) {
            p.sendSystemMessage(Component.translatable(T + (erin ? "boek.trekken_straks" : "boek.dicht")).withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (d.mislukt) {
            p.sendSystemMessage(Component.translatable(T + "boek.mislukt").withStyle(ChatFormatting.YELLOW));
            return;
        }
        UUID ontvanger = d.lootjeVan(p.getUUID());
        if (ontvanger == null) {
            p.sendSystemMessage(Component.translatable(T + (d.bezorgd ? "boek.klaar" : "boek.niet_mee")).withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (Guhwinter.bouwenOpen(server)) {
            p.sendSystemMessage(Component.translatable(T + "boek.jouw_lootje", Component.literal(d.naam(ontvanger)).withStyle(ChatFormatting.AQUA),
                    d.veldVan(p.getUUID()) + 1).withStyle(ChatFormatting.LIGHT_PURPLE));
            p.sendSystemMessage(knop(d.tekening.containsKey(p.getUUID()) ? "boek.knop.teken_verder" : "boek.knop.teken", "teken", ChatFormatting.GREEN)
                    .append(" ").append(Guhwinter.hoeKomIkDaar()));
            p.sendSystemMessage(Component.translatable(T + "boek.tot").withStyle(ChatFormatting.GRAY));
        } else if (!d.bezorgd) {
            p.sendSystemMessage(Component.translatable(T + "boek.wachten").withStyle(ChatFormatting.YELLOW));
        } else {
            p.sendSystemMessage(Component.translatable(T + "boek.klaar").withStyle(ChatFormatting.YELLOW));
        }
    }

    private static MutableComponent knop(String sleutel, String sub, ChatFormatting kleur) {
        return Component.translatable(T + sleutel).withStyle(s -> s.withColor(kleur).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/guhs winter lootjes " + sub))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable(T + sleutel + ".hover"))));
    }

    // =====================================================================================================================
    // signing up
    // =====================================================================================================================

    static void inschrijven(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        LootjesData d = LootjesData.get(server);
        if (!Guhwinter.bezig(server) || !Guhwinter.inschrijvenOpen(server) || d.getrokken) {
            zeg(p, "in.dicht");
            return;
        }
        if (d.isIngeschreven(p.getUUID())) {
            zeg(p, "in.al");
            return;
        }
        if (d.ingeschreven.size() >= MAX) {
            zeg(p, "boek.vol");
            return;
        }
        d.ingeschreven.add(new LootjesData.Deelnemer(p.getUUID(), p.getGameProfile().name(), false));
        d.setDirty();
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8f, 1.2f);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.4f);
        p.sendSystemMessage(Component.translatable(T + "in.gelukt", d.ingeschreven.size(), MAX).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    static void uitschrijven(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        LootjesData d = LootjesData.get(server);
        if (!Guhwinter.inschrijvenOpen(server) || d.getrokken) {
            zeg(p, "uit.te_laat");
            return;
        }
        if (!d.ingeschreven.removeIf(x -> x.id().equals(p.getUUID()))) {
            zeg(p, "uit.niet");
            return;
        }
        d.setDirty();
        p.sendSystemMessage(Component.translatable(T + "uit.gelukt").withStyle(ChatFormatting.GRAY));
    }

    /** Opens the drawing for this player's lootje (only givers, only during building time). */
    static void teken(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        LootjesData d = LootjesData.get(server);
        String nee = magTekenen(server, p.getUUID());
        if (nee != null) {
            p.sendSystemMessage(Component.translatable(nee).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        Tekeningen.openEditor(p, Doel.LOOTJE, d.tekening.get(p.getUUID()));
        p.sendOverlayMessage(Component.translatable(T + "teken.voor", d.naam(d.lootjeVan(p.getUUID()))).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Null = this player may save a drawing for their lootje now; else the lang key why not. */
    @Nullable
    static String magTekenen(MinecraftServer server, UUID id) {
        LootjesData d = LootjesData.get(server);
        if (!d.getrokken || d.lootjeVan(id) == null) {
            return T + "teken.geen_lootje";
        }
        if (!Guhwinter.bouwenOpen(server)) {
            return T + "teken.dicht";
        }
        return null;
    }

    /** (tekenen: Doel.LOOTJE saved) remember this giver's drawing. */
    static void tekeningOpgeslagen(ServerPlayer p, Tekening t) {
        LootjesData d = LootjesData.get(p.level().getServer());
        if (d.lootjeVan(p.getUUID()) == null) {
            return;
        }
        boolean nieuw = !t.id().equals(d.tekening.get(p.getUUID()));
        d.tekening.put(p.getUUID(), t.id());
        d.setDirty();
        p.sendSystemMessage(Component.translatable(T + (nieuw ? "teken.bewaard" : "teken.bijgewerkt"), d.naam(d.lootjeVan(p.getUUID())))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // the draw (1 Dec 00:00)
    // =====================================================================================================================

    /** The Sint draws. Returns a line for the dev command / log. */
    static String trek(MinecraftServer server) {
        LootjesData d = LootjesData.get(server);
        if (d.getrokken) {
            return "al getrokken";
        }
        d.getrokken = true;
        List<LootjesData.Deelnemer> namen = new ArrayList<>(d.ingeschreven);
        if (namen.size() < 2) {
            d.mislukt = true;
            for (LootjesData.Deelnemer x : namen) {
                if (!x.nep()) {
                    d.brief(x.id(), "geen_trekking");
                }
            }
            d.setDirty();
            bezorgBrieven(server);
            return "minder dan 2 namen (" + namen.size() + "): geen trekking, troostbriefjes";
        }
        int[] p = Trekking.trek(namen.size(), new Random());
        int vrij = 0;
        for (int i = 0; i < namen.size(); i++) {
            UUID gever = namen.get(i).id();
            d.lootje.put(gever, namen.get(p[i]).id());
            while (vrij < Bouwvelden.AANTAL && d.eigenaarVanVeld(vrij) != null) {
                vrij++;
            }
            d.veld.put(gever, vrij++);
            if (!namen.get(i).nep()) {
                d.brief(gever, "trekking");
            }
        }
        d.setDirty();
        ServerLevel level = server.getLevel(GuhwinterZone.DIMENSIE);
        if (level != null) {
            LootjesPlaatsing.schrijfBorden(level);
        }
        bezorgBrieven(server);
        return "getrokken: " + namen.size() + " namen";
    }

    // =====================================================================================================================
    // capture (5 Dec 18:00)
    // =====================================================================================================================

    /** Everyone out of creative, every plot read into its gift. Returns a line for the dev command / log. */
    static String vang(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Bouwkist.herstel(p)) {
                p.sendSystemMessage(Component.translatable(T + "bouwen.tijd_om").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        LootjesData d = LootjesData.get(server);
        if (d.gevangen) {
            return "al gevangen";
        }
        ServerLevel level = server.getLevel(GuhwinterZone.DIMENSIE);
        if (level == null) {
            return "geen Guhmension";
        }
        int n = 0;
        for (var e : d.veld.entrySet()) {
            Bouwvelden.Veld v = Bouwvelden.veld(e.getValue());
            if (v == null) {
                continue;
            }
            Inhoud in = Inhoud.vang(level, v.min()).gedraaid(v.naarVoorkant());
            d.cadeau.put(e.getKey(), in.opslaan());
            n++;
        }
        d.gevangen = true;
        d.setDirty();
        return n + " bouwplekjes ingepakt";
    }

    // =====================================================================================================================
    // pakjesavond (5 Dec 19:00)
    // =====================================================================================================================

    static String bezorg(MinecraftServer server) {
        LootjesData d = LootjesData.get(server);
        if (!d.gevangen && d.getrokken && !d.mislukt) {
            vang(server);
        }
        if (d.bezorgd) {
            return "al bezorgd";
        }
        d.bezorgd = true;
        for (UUID ontvanger : d.lootje.values()) {
            d.teBezorgen.add(ontvanger);
        }
        d.setDirty();
        int n = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (d.teBezorgen.contains(p.getUUID())) {
                KLOPPEN.put(p.getUUID(), 0);
                n++;
            }
        }
        return d.teBezorgen.size() + " zakken klaar, " + n + " spelers online krijgen hem nu";
    }

    /** The sack for this receiver (null when there's nothing for them). */
    @Nullable
    static ItemStack maakZak(MinecraftServer server, UUID ontvanger) {
        LootjesData d = LootjesData.get(server);
        UUID gever = d.geverVoor(ontvanger);
        if (gever == null) {
            return null;
        }
        String maker = d.naam(gever), voor = d.naam(ontvanger);
        net.minecraft.nbt.CompoundTag tag = d.cadeau.get(gever);
        SurpriseData gift = new SurpriseData(tag != null ? tag : new Inhoud().opslaan(), maker, voor);
        List<ItemStack> inhoud = new ArrayList<>();
        inhoud.add(SurpriseItem.maak(gift));
        UUID tekeningId = d.tekening.get(gever);
        boolean tekening = tekeningId != null && Tekeningen.get(server, tekeningId) != null;
        if (tekening) {
            inhoud.add(Tekeningen.maakItem(tekeningId, maker));
        }
        ItemStack zak = new ItemStack(LootjesFeature.ZAK.get());
        zak.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inhoud));
        List<Component> regels = new ArrayList<>();
        regels.add(Component.translatable(T + "zak.voor", voor).withStyle(ChatFormatting.LIGHT_PURPLE));
        regels.add(Component.translatable(T + "zak.van", maker).withStyle(ChatFormatting.AQUA));
        if (gift.leeg()) {
            regels.add(Component.translatable(T + "zak.leeg").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (!tekening) {
            regels.add(Component.translatable(T + "zak.geen_tekening").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        zak.set(DataComponents.LORE, new ItemLore(regels));
        return zak;
    }

    /** Tries to hand this player their sack now (true: done or nothing to give). Never on the ground. */
    static boolean probeerZak(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        LootjesData d = LootjesData.get(server);
        if (!d.teBezorgen.contains(p.getUUID())) {
            return true;
        }
        if (Bouwkist.bouwt(p)) {
            return false;
        }
        ItemStack zak = maakZak(server, p.getUUID());
        if (zak == null) {
            d.teBezorgen.remove(p.getUUID());
            d.klaar.add(p.getUUID());
            d.setDirty();
            return true;
        }
        if (p.getInventory().getFreeSlot() < 0 || !p.getInventory().add(zak)) {
            long nu = server.getTickCount();
            Long laatst = VOL_GEZEGD.get(p.getUUID());
            if (laatst == null || nu - laatst > 20 * 60 * 5) {
                VOL_GEZEGD.put(p.getUUID(), nu);
                p.sendSystemMessage(Component.translatable(T + "zak.tas_vol").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return false;
        }
        d.teBezorgen.remove(p.getUUID());
        d.klaar.add(p.getUUID());
        d.setDirty();
        p.inventoryMenu.broadcastChanges();
        p.sendSystemMessage(Component.translatable(T + "zak.gekregen", d.naam(d.geverVoor(p.getUUID()))).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.3f);
        return true;
    }

    // =====================================================================================================================
    // letters, logins, the tick
    // =====================================================================================================================

    /** Shows every waiting letter to the online players who have one. */
    static void bezorgBrieven(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            brieven(p);
        }
    }

    /** The letters waiting for this player (shown and gone). */
    static void brieven(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        LootjesData d = LootjesData.get(server);
        List<String> l = d.brieven.remove(p.getUUID());
        if (l == null || l.isEmpty()) {
            return;
        }
        d.setDirty();
        for (String soort : l) {
            brief(p, d, soort);
        }
    }

    private static void brief(ServerPlayer p, LootjesData d, String soort) {
        String naam = p.getGameProfile().name();
        p.sendSystemMessage(Component.literal(" "));
        p.sendSystemMessage(Component.translatable(T + "brief.kop").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.translatable(T + "brief.aanhef", naam).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        if ("trekking".equals(soort)) {
            UUID ontvanger = d.lootjeVan(p.getUUID());
            p.sendSystemMessage(Component.translatable(T + "brief.trekking.1", Component.literal(d.naam(ontvanger)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            p.sendSystemMessage(Component.translatable(T + "brief.trekking.2", d.veldVan(p.getUUID()) + 1).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            p.sendSystemMessage(Component.translatable(T + "brief.trekking.3").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            p.sendSystemMessage(Component.translatable(T + "brief.groet").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            p.sendSystemMessage(knop("boek.knop.teken", "teken", ChatFormatting.GREEN).append(" ").append(Guhwinter.hoeKomIkDaar()));
        } else {
            p.sendSystemMessage(Component.translatable(T + "brief.geen.1").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            p.sendSystemMessage(Component.translatable(T + "brief.geen.2", PEPERNOTEN_TROOST).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            p.sendSystemMessage(Component.translatable(T + "brief.groet").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            Guhwinter.geefPepernoten(p, PEPERNOTEN_TROOST);
            d.klaar.add(p.getUUID());
            d.setDirty();
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.6f);
    }

    /** At login: a left-over stash back, the waiting letters, the sack (with a knock). */
    static void opLogin(ServerPlayer p) {
        Bouwkist.herstel(p);
        brieven(p);
        LootjesData d = LootjesData.get(p.level().getServer());
        if (d.teBezorgen.contains(p.getUUID())) {
            KLOPPEN.put(p.getUUID(), -60);   // (a few seconds after joining)
        }
    }

    static void uitgelogd(UUID id) {
        KLOPPEN.remove(id);
        VOL_GEZEGD.remove(id);
    }

    /** Every tick: the knocks; every 30 s: sacks that still wait. */
    static void tick(MinecraftServer server) {
        if (!KLOPPEN.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> it = KLOPPEN.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p == null) {
                    it.remove();
                    continue;
                }
                int t = e.getValue() + 1;
                e.setValue(t);
                if (t == 0 || t == 8 || t == 16) {
                    p.level().playSound(null, p.blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 0.45f, 1.5f);
                }
                if (t == 2) {
                    p.sendSystemMessage(Component.translatable(T + "zak.bonk").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                }
                if (t == 40) {
                    p.sendSystemMessage(Component.translatable(T + "zak.voor_de_deur").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                    probeerZak(p);
                    it.remove();
                }
            }
        }
        if (server.getTickCount() % 600 == 0) {
            LootjesData d = LootjesData.get(server);
            if (!d.teBezorgen.isEmpty()) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (d.teBezorgen.contains(p.getUUID()) && !KLOPPEN.containsKey(p.getUUID())) {
                        probeerZak(p);
                    }
                }
            }
        }
    }

    static void vergeetAlles() {
        KLOPPEN.clear();
        VOL_GEZEGD.clear();
    }

    private static void zeg(ServerPlayer p, String sleutel, Object... args) {
        p.sendSystemMessage(Component.translatable(T + sleutel, args).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** (dev) a plot's current content as a gift item (for testing the Surprise without waiting for 5 December). */
    @Nullable
    static ItemStack testSurprise(MinecraftServer server, int veld, String voor) {
        ServerLevel level = server.getLevel(GuhwinterZone.DIMENSIE);
        Bouwvelden.Veld v = Bouwvelden.veld(veld);
        if (level == null || v == null) {
            return null;
        }
        LootjesData d = LootjesData.get(server);
        UUID eigenaar = d.eigenaarVanVeld(veld);
        String maker = eigenaar == null ? "Sinterklaas" : d.naam(eigenaar);
        return SurpriseItem.maak(SurpriseData.van(Inhoud.vang(level, v.min()).gedraaid(v.naarVoorkant()), maker, voor));
    }
}
