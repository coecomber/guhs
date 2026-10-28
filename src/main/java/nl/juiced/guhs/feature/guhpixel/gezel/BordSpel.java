package nl.juiced.guhs.feature.guhpixel.gezel;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The two board games of the Gezelschapshoek in the Guhpixel lobby: {@link #OPEENRIJ} (Guh op een rij) and
 * {@link #WIEISHET} (Wie is het, guh-editie). No arena: a game is a screen. Talking to the game's NPC opens the screen
 * ("kies": against the guh-bot at a level, or look for an opponent). Two players who both look for an opponent of the
 * same game are paired at once; the first to ask begins. Everything is decided here; the screen only shows {@link #staat}.
 * <p>
 * Rewards: a win against the bot pays muntjes from a daily pot (more for a harder bot), a win against a player too;
 * the very first win pays 50 extra once. Wins count on the Highscores board of the game (total wins) and grant the quest
 * advancement px_&lt;game&gt;_gewonnen.
 */
public final class BordSpel {
    public static final String OPEENRIJ = "opeenrij", WIEISHET = "wieishet";
    public static final int KIES_BOT = 0, ZOEK = 1, ANNULEER = 2, ZET = 3, VRAAG = 4, RAAD = 5, OPNIEUW = 6, SLUIT = 7;
    /** How long the guh-bot "thinks" (ticks). */
    static final int BOT_DENKT = 25;
    private static final String WINST = "guhs_gezel_winst";

    /** One game between two sides; side 1 can be the guh-bot. */
    static final class Partij {
        final String spel;
        final UUID a;
        @Nullable
        final UUID b;
        final String naamA, naamB;
        final int niveau;
        final OpEenRij rij = new OpEenRij();
        List<WieIsHet.Kaart> kaarten = List.of();
        int geheimA, geheimB;
        BitSet openA = WieIsHet.alles(), openB = WieIsHet.alles();
        int beurt;
        /** -1 = still playing, 0 = a won, 1 = b won, 2 = draw, 3 = stopped (somebody left). */
        int uitslag = -1;
        int botOp = -1;
        final List<Component> log = new ArrayList<>();

        Partij(String spel, UUID a, String naamA, @Nullable UUID b, String naamB, int niveau, RandomSource rnd) {
            this.spel = spel;
            this.a = a;
            this.naamA = naamA;
            this.b = b;
            this.naamB = naamB;
            this.niveau = niveau;
            if (WIEISHET.equals(spel)) {
                kaarten = WieIsHet.bord(rnd.nextLong());
                geheimA = rnd.nextInt(kaarten.size());
                geheimB = rnd.nextInt(kaarten.size());
            }
        }

        boolean bot() {
            return b == null;
        }

        int kant(UUID id) {
            return id.equals(a) ? 0 : 1;
        }

        void log(Component c) {
            log.add(c);
            while (log.size() > 6) {
                log.remove(0);
            }
        }
    }

    static final Map<UUID, Partij> VAN = new HashMap<>();
    static final Map<String, UUID> WACHT = new HashMap<>();

    private BordSpel() {
    }

    // --- opening --------------------------------------------------------------------------------------------------------

    /** The NPC was clicked: the screen of this game (your running game, the queue, or the choice). */
    public static void open(ServerPlayer p, String spel) {
        stuur(p, spel);
    }

    @Nullable
    static Partij van(ServerPlayer p) {
        return VAN.get(p.getUUID());
    }

    // --- actions --------------------------------------------------------------------------------------------------------

    public static void doe(ServerPlayer p, String spel, int actie, int arg) {
        if (!OPEENRIJ.equals(spel) && !WIEISHET.equals(spel)) {
            return;
        }
        Partij partij = van(p);
        switch (actie) {
            case KIES_BOT -> {
                if (partij == null || partij.uitslag >= 0) {
                    stop(p, false);
                    begin(spel, p, null, Math.max(0, Math.min(2, arg)), p.getRandom());
                }
            }
            case ZOEK -> {
                if (partij == null || partij.uitslag >= 0) {
                    stop(p, false);
                    zoek(p, spel);
                }
            }
            case ANNULEER -> {
                if (p.getUUID().equals(WACHT.get(spel))) {
                    WACHT.remove(spel);
                }
                stuur(p, spel);
            }
            case ZET -> {
                if (partij != null && OPEENRIJ.equals(partij.spel)) {
                    zet(partij, partij.kant(p.getUUID()), arg, p.level().getServer());
                }
            }
            case VRAAG -> {
                if (partij != null && WIEISHET.equals(partij.spel)) {
                    vraag(partij, partij.kant(p.getUUID()), arg, p.level().getServer());
                }
            }
            case RAAD -> {
                if (partij != null && WIEISHET.equals(partij.spel)) {
                    raad(partij, partij.kant(p.getUUID()), arg, p.level().getServer());
                }
            }
            case OPNIEUW -> {
                if (partij != null && partij.uitslag >= 0) {
                    VAN.remove(p.getUUID());
                }
                stuur(p, spel);
            }
            case SLUIT -> stop(p, true);
            default -> {
            }
        }
    }

    private static void zoek(ServerPlayer p, String spel) {
        UUID ander = WACHT.get(spel);
        ServerPlayer tegen = ander == null || ander.equals(p.getUUID()) ? null : p.level().getServer().getPlayerList().getPlayer(ander);
        if (tegen != null && Guhpixel.in(tegen) && van(tegen) == null) {
            WACHT.remove(spel);
            begin(spel, tegen, p, OpEenRij.NORMAAL, p.getRandom());
            return;
        }
        WACHT.put(spel, p.getUUID());
        stuur(p, spel);
    }

    /** A new game: `a` begins; b = null is the guh-bot. */
    static Partij begin(String spel, ServerPlayer a, @Nullable ServerPlayer b, int niveau, RandomSource rnd) {
        Component botNaam = Component.translatable("gui.guhs.gezel.bot." + niveau);
        Partij partij = new Partij(spel, a.getUUID(), a.getName().getString(), b == null ? null : b.getUUID(),
                b == null ? botNaam.getString() : b.getName().getString(), niveau, rnd);
        VAN.put(a.getUUID(), partij);
        if (b != null) {
            VAN.put(b.getUUID(), partij);
        }
        partij.log(Component.translatable("gui.guhs.gezel.log.begin", partij.naamA));
        stuurBeide(partij, a.level().getServer());
        return partij;
    }

    // --- Guh op een rij --------------------------------------------------------------------------------------------------

    static boolean zet(Partij partij, int kant, int kolom, @Nullable MinecraftServer server) {
        if (partij.uitslag >= 0 || partij.beurt != kant || partij.rij.laat(kolom, kant == 0 ? OpEenRij.GUH : OpEenRij.BLUB) < 0) {
            return false;
        }
        byte w = partij.rij.winnaar();
        if (w != OpEenRij.LEEG) {
            einde(partij, w == OpEenRij.GUH ? 0 : 1, server);
        } else if (partij.rij.vol()) {
            einde(partij, 2, server);
        } else {
            partij.beurt = 1 - kant;
            if (partij.bot() && partij.beurt == 1 && server != null) {
                partij.botOp = server.getTickCount() + BOT_DENKT;
            }
        }
        if (server != null) {
            stuurBeide(partij, server);
        }
        return true;
    }

    // --- Wie is het -------------------------------------------------------------------------------------------------------

    static boolean vraag(Partij partij, int kant, int vraagNr, @Nullable MinecraftServer server) {
        if (partij.uitslag >= 0 || partij.beurt != kant || vraagNr < 0 || vraagNr >= WieIsHet.VRAGEN.size()) {
            return false;
        }
        WieIsHet.Vraag v = WieIsHet.VRAGEN.get(vraagNr);
        int geheim = kant == 0 ? partij.geheimB : partij.geheimA;
        boolean antwoord = v.geldt(partij.kaarten.get(geheim));
        if (kant == 0) {
            partij.openA = WieIsHet.wegstrepen(partij.kaarten, partij.openA, v, antwoord);
        } else {
            partij.openB = WieIsHet.wegstrepen(partij.kaarten, partij.openB, v, antwoord);
        }
        Component vraagTekst = vraagTekst(v);
        partij.log(Component.translatable(antwoord ? "gui.guhs.gezel.log.ja" : "gui.guhs.gezel.log.nee", kant == 0 ? partij.naamA : partij.naamB,
                vraagTekst));
        partij.beurt = 1 - kant;
        if (partij.bot() && partij.beurt == 1 && server != null) {
            partij.botOp = server.getTickCount() + BOT_DENKT;
        }
        if (server != null) {
            stuurBeide(partij, server);
        }
        return true;
    }

    public static Component vraagTekst(WieIsHet.Vraag v) {
        String arg = v.argSleutel();
        return arg.isEmpty() ? Component.translatable(v.sleutel()) : Component.translatable(v.sleutel(), Component.translatable(arg));
    }

    static boolean raad(Partij partij, int kant, int kaart, @Nullable MinecraftServer server) {
        if (partij.uitslag >= 0 || partij.beurt != kant || kaart < 0 || kaart >= partij.kaarten.size()) {
            return false;
        }
        int geheim = kant == 0 ? partij.geheimB : partij.geheimA;
        String wie = kant == 0 ? partij.naamA : partij.naamB;
        String naam = partij.kaarten.get(kaart).naam();
        if (kaart == geheim) {
            partij.log(Component.translatable("gui.guhs.gezel.log.goed", wie, naam));
            einde(partij, kant, server);
        } else {
            partij.log(Component.translatable("gui.guhs.gezel.log.fout", wie, naam, partij.kaarten.get(geheim).naam()));
            einde(partij, 1 - kant, server);
        }
        if (server != null) {
            stuurBeide(partij, server);
        }
        return true;
    }

    // --- the bot ----------------------------------------------------------------------------------------------------------

    static void botBeurt(Partij partij, MinecraftServer server) {
        RandomSource rnd = server.overworld().getRandom();
        partij.botOp = -1;
        if (OPEENRIJ.equals(partij.spel)) {
            int k = partij.rij.botZet(OpEenRij.BLUB, partij.niveau, rnd);
            if (k >= 0) {
                zet(partij, 1, k, server);
            }
            return;
        }
        int open = partij.openB.cardinality();
        int vraag = WieIsHet.botVraag(partij.kaarten, partij.openB, rnd);
        // the bot guesses when one guh is left (an easy bot sometimes guesses too early)
        if (open == 1 || vraag < 0 || (partij.niveau == OpEenRij.MAKKELIJK && open <= 3 && rnd.nextInt(3) == 0)) {
            List<Integer> l = new ArrayList<>();
            for (int i = partij.openB.nextSetBit(0); i >= 0; i = partij.openB.nextSetBit(i + 1)) {
                l.add(i);
            }
            raad(partij, 1, l.isEmpty() ? 0 : l.get(rnd.nextInt(l.size())), server);
        } else {
            vraag(partij, 1, vraag, server);
        }
    }

    // --- the end ----------------------------------------------------------------------------------------------------------

    static void einde(Partij partij, int uitslag, @Nullable MinecraftServer server) {
        partij.uitslag = uitslag;
        partij.botOp = -1;
        if (uitslag == 2) {
            partij.log(Component.translatable("gui.guhs.gezel.log.gelijk"));
        } else if (uitslag <= 1) {
            partij.log(Component.translatable("gui.guhs.gezel.log.winnaar", uitslag == 0 ? partij.naamA : partij.naamB));
        }
        if (server == null || uitslag > 1) {
            return;
        }
        ServerPlayer winnaar = server.getPlayerList().getPlayer(uitslag == 0 ? partij.a : partij.b == null ? partij.a : partij.b);
        if (winnaar != null && !(uitslag == 1 && partij.bot())) {
            beloon(winnaar, partij.spel, partij.bot() ? partij.niveau : -1);
        }
        for (UUID id : new UUID[] {partij.a, partij.b}) {
            ServerPlayer p = id == null ? null : server.getPlayerList().getPlayer(id);
            if (p != null) {
                boolean won = uitslag == partij.kant(id);
                p.level().playSound(null, p.blockPosition(), won ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS,
                        0.6f, won ? 1.2f : 0.7f);
            }
        }
    }

    /** A win: muntjes (daily pot), the first-win bonus, the total on the Highscores board and the quest advancement. */
    static void beloon(ServerPlayer p, String spel, int botNiveau) {
        int n = botNiveau < 0 ? 25 : 10 + 10 * botNiveau;
        int betaald = Muntjes.verdienDagelijks(p, "gezel_" + spel, n, 150);
        if (betaald > 0) {
            p.sendSystemMessage(Component.translatable("gui.guhs.gezel.muntjes", betaald).withStyle(ChatFormatting.GOLD));
        }
        Muntjes.verdienEens(p, "gezel:" + spel, 50);
        CompoundTag winst = GuhQuests.saved(p).getCompoundOrEmpty(WINST);
        int totaal = winst.getIntOr(spel, 0) + 1;
        winst.putInt(spel, totaal);
        GuhQuests.saved(p).put(WINST, winst);
        Scorebord.submit(p, spel, totaal, false);
        GuhAdvancements.grant(p, "px_" + spel + "_gewonnen");
    }

    public static int gewonnen(ServerPlayer p, String spel) {
        return GuhQuests.saved(p).getCompoundOrEmpty(WINST).getIntOr(spel, 0);
    }

    /** Leaves the game (the screen was closed with "stop", or the player left the lobby / logged out). */
    static void stop(ServerPlayer p, boolean melden) {
        WACHT.values().removeIf(id -> id.equals(p.getUUID()));
        Partij partij = VAN.remove(p.getUUID());
        if (partij == null) {
            return;
        }
        UUID anderId = partij.kant(p.getUUID()) == 0 ? partij.b : partij.a;
        if (partij.uitslag < 0) {
            partij.uitslag = 3;
            partij.log(Component.translatable("gui.guhs.gezel.log.weg", p.getName().getString()));
        }
        if (anderId != null && melden) {
            ServerPlayer ander = p.level().getServer().getPlayerList().getPlayer(anderId);
            if (ander != null) {
                stuur(ander, partij.spel);
            }
        }
    }

    // --- every tick ------------------------------------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        int nu = server.getTickCount();
        for (Partij partij : new ArrayList<>(new java.util.LinkedHashSet<>(VAN.values()))) {
            if (partij.botOp >= 0 && nu >= partij.botOp && partij.uitslag < 0) {
                botBeurt(partij, server);
            }
        }
        if (nu % 20 == 0) {
            for (UUID id : new ArrayList<>(VAN.keySet())) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null || !Guhpixel.in(p)) {
                    Partij partij = VAN.remove(id);
                    if (partij != null && partij.uitslag < 0) {
                        partij.uitslag = 3;
                        UUID ander = id.equals(partij.a) ? partij.b : partij.a;
                        ServerPlayer ap = ander == null ? null : server.getPlayerList().getPlayer(ander);
                        if (ap != null) {
                            partij.log(Component.translatable("gui.guhs.gezel.log.weg", id.equals(partij.a) ? partij.naamA : partij.naamB));
                            stuur(ap, partij.spel);
                        }
                    }
                }
            }
            WACHT.values().removeIf(id -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                return p == null || !Guhpixel.in(p);
            });
        }
    }

    // --- the screen's state ------------------------------------------------------------------------------------------

    static void stuurBeide(Partij partij, MinecraftServer server) {
        for (UUID id : new UUID[] {partij.a, partij.b}) {
            ServerPlayer p = id == null ? null : server.getPlayerList().getPlayer(id);
            if (p != null && VAN.get(id) == partij) {
                stuur(p, partij.spel);
            }
        }
    }

    static void stuur(ServerPlayer p, String spel) {
        ModNetworking.sendTo(p, new GezelPayloads.Scherm(staat(p, spel)));
    }

    /** What the screen of this player shows. */
    public static CompoundTag staat(ServerPlayer p, String spel) {
        CompoundTag t = new CompoundTag();
        Partij partij = van(p);
        if (partij != null && !partij.spel.equals(spel)) {
            spel = partij.spel;
        }
        t.putString("Spel", spel);
        t.putInt("Gewonnen", gewonnen(p, spel));
        if (partij == null) {
            t.putString("Fase", p.getUUID().equals(WACHT.get(spel)) ? "wacht" : "kies");
            return t;
        }
        int kant = partij.kant(p.getUUID());
        t.putString("Fase", partij.uitslag >= 0 ? "einde" : "spel");
        t.putInt("Uitslag", partij.uitslag < 0 ? -1 : partij.uitslag == 2 ? 2 : partij.uitslag == 3 ? 3 : partij.uitslag == kant ? 0 : 1);
        t.putString("Tegen", kant == 0 ? partij.naamB : partij.naamA);
        t.putBoolean("Bot", partij.bot());
        t.putInt("Niveau", partij.niveau);
        t.putBoolean("Mijn", partij.beurt == kant && partij.uitslag < 0);
        ListTag log = new ListTag();
        for (Component c : partij.log) {
            log.add(ComponentSerialization.CODEC.encodeStart(p.registryAccess().createSerializationContext(NbtOps.INSTANCE), c).getOrThrow());
        }
        t.put("Log", log);
        if (OPEENRIJ.equals(partij.spel)) {
            t.put("Bord", new ByteArrayTag(partij.rij.vakken()));
            t.putByte("Ik", kant == 0 ? OpEenRij.GUH : OpEenRij.BLUB);
        } else {
            ListTag kaarten = new ListTag();
            for (WieIsHet.Kaart k : partij.kaarten) {
                CompoundTag c = new CompoundTag();
                c.putString("N", k.naam());
                c.putInt("K", k.kleur());
                c.putInt("H", k.hoed());
                c.putBoolean("B", k.bril());
                c.putBoolean("S", k.strik());
                c.putInt("A", k.aard());
                kaarten.add(c);
            }
            t.put("Kaarten", kaarten);
            BitSet open = kant == 0 ? partij.openA : partij.openB;
            t.putLong("Open", open.isEmpty() ? 0L : open.toLongArray()[0]);
            t.putInt("Geheim", kant == 0 ? partij.geheimA : partij.geheimB);
            t.putInt("AnderOpen", (kant == 0 ? partij.openB : partij.openA).cardinality());
            if (partij.uitslag >= 0) {
                t.putInt("AnderGeheim", kant == 0 ? partij.geheimB : partij.geheimA);
            }
        }
        return t;
    }
}
