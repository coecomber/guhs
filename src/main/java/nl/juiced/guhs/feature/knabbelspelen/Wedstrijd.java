package nl.juiced.guhs.feature.knabbelspelen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.doolhof.Anker;
import nl.juiced.guhs.feature.doolhof.Adv;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModSounds;

/**
 * One round of De Knabbelspelen at Juf Vahoegsakee's tent: one event, or the Grote Zeskamp (all six in a row). The
 * player who starts it may wait a little for friends: up to {@value #MAX} guhs play at the same time, each in their own
 * lane with their own score or time. Every event: to the lanes, 3-2-1, whistle, play ({@link Spel}), the results; after
 * the last one the spelenlintjes (+1 like every reward), the boards, the records and the advancements.
 */
public final class Wedstrijd {
    /** Actions of Juf Vahoegsakee's screen: an event (0..5), the zeskamp, join, start now, the shop. */
    public static final int ONDERDEEL = 0, ZESKAMP = 6, MEEDOEN = 7, NU = 8, SHOP = 9;
    public static final int MAX = Speelvelden.BANEN;
    public static final int INSCHRIJF_TICKS = 20 * 15, AFTEL_TICKS = 60, UITSLAG_TICKS = 20 * 4;
    public enum Fase { INSCHRIJVEN, AFTELLEN, BEZIG, UITSLAG }

    /** What an event does, per player (its state lives in {@link Deelnemer#staat}). */
    public interface Spel {
        /** To the lane (teleport, items, effects, the event's things). */
        void klaarzetten(Wedstrijd w, Deelnemer d, ServerPlayer p, ServerLevel level);

        /** 3-2-1: by default you stay at the start. */
        default void aftellen(Wedstrijd w, Deelnemer d, ServerPlayer p, ServerLevel level) {
            w.houdBijStart(d, p, level);
        }

        /** The whistle. */
        default void start(Wedstrijd w, Deelnemer d, ServerPlayer p, ServerLevel level) {
        }

        /** Every tick while playing (until {@link Wedstrijd#klaar}). */
        void tick(Wedstrijd w, Deelnemer d, ServerPlayer p, ServerLevel level, int t);

        /** The score when time's up (points so far; -1 for an unfinished time event). */
        default int eindScore(Deelnemer d) {
            return -1;
        }

        /** Clean up (items, effects, entities); p is null for a player who left. */
        void einde(Wedstrijd w, Deelnemer d, @Nullable ServerPlayer p, ServerLevel level);

        /** Where you stand at the start (u along the lane) and look. */
        default double startU() {
            return 0;
        }
    }

    public static Spel spel(Onderdeel o) {
        return switch (o) {
            case KNABBELHAPPEN -> Knabbelhappen.SPEL;
            case ZAKLOPEN -> Zaklopen.SPEL;
            case BLIKGOOIEN -> Blikgooien.SPEL;
            case EIERLOPEN -> Eierlopen.SPEL;
            case SPIJKERPOEPEN -> Spijkerpoepen.SPEL;
            case GUHGUHTJE_PRIK -> GuhguhtjePrik.SPEL;
        };
    }

    /** A player in the round. */
    public static final class Deelnemer {
        public final UUID id;
        public final String naam;
        public int baan;
        /** Score per event (points or ticks; -1 = not played / not finished) and its zeskamp points (0..1000). */
        public final int[] score = new int[Onderdeel.values().length];
        public final int[] punten = new int[Onderdeel.values().length];
        public boolean klaar, weg;
        @Nullable
        public Object staat;

        Deelnemer(ServerPlayer p) {
            this.id = p.getUUID();
            this.naam = p.getGameProfile().getName();
            java.util.Arrays.fill(score, -1);
        }

        public int totaal() {
            int t = 0;
            for (int x : punten) {
                t += x;
            }
            return t;
        }
    }

    private static final Map<UUID, Wedstrijd> GAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYERS = new ConcurrentHashMap<>();

    public final UUID npcId;
    public final Anker anker;
    public final List<Onderdeel> programma;
    public final boolean zeskamp;
    final List<Deelnemer> deelnemers = new ArrayList<>();
    private int index;
    private Fase fase = Fase.INSCHRIJVEN;
    int ticks;
    private boolean nu;
    private long lastTick;

    private Wedstrijd(UUID npcId, Anker anker, List<Onderdeel> programma, boolean zeskamp) {
        this.npcId = npcId;
        this.anker = anker;
        this.programma = programma;
        this.zeskamp = zeskamp;
    }

    // --- who plays where -------------------------------------------------------------------------------------------------

    public static boolean isPlaying(Player player) {
        return PLAYERS.containsKey(player.getUUID());
    }

    @Nullable
    public static Wedstrijd of(GuhNpcEntity npc) {
        return GAMES.get(npc.getUUID());
    }

    @Nullable
    static Wedstrijd byNpc(@Nullable UUID npc) {
        return npc == null ? null : GAMES.get(npc);
    }

    @Nullable
    public static Wedstrijd van(Player player) {
        UUID npc = PLAYERS.get(player.getUUID());
        return npc == null ? null : GAMES.get(npc);
    }

    @Nullable
    public Deelnemer deelnemer(Player p) {
        for (Deelnemer d : deelnemers) {
            if (d.id.equals(p.getUUID()) && !d.weg) {
                return d;
            }
        }
        return null;
    }

    public Fase fase() {
        return fase;
    }

    public Onderdeel onderdeel() {
        return programma.get(Math.min(index, programma.size() - 1));
    }

    /** Is this player playing this event right now (after the whistle, not finished)? */
    public static boolean speelt(Player p, Onderdeel o) {
        Wedstrijd w = van(p);
        if (w == null || w.fase != Fase.BEZIG || w.onderdeel() != o) {
            return false;
        }
        Deelnemer d = w.deelnemer(p);
        return d != null && !d.klaar;
    }

    public List<Deelnemer> deelnemers() {
        return deelnemers;
    }

    // --- Juf Vahoegsakee's screen ---------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        Wedstrijd w = of(npc);
        Adv.grant(player, "knabbelspelen_gevonden");
        boolean mee = w != null && w.deelnemer(player) != null;
        String key = w == null ? (GuhQuests.saved(player).getBoolean(PLAYED_KEY) ? "quest.guhs.knabbelspelen.hello_again" : "quest.guhs.knabbelspelen.hello")
                : w.fase == Fase.INSCHRIJVEN ? (mee ? "quest.guhs.knabbelspelen.wacht" : "quest.guhs.knabbelspelen.doe_mee") : "quest.guhs.knabbelspelen.bezig";
        GuhQuests.say(player, npc, key, w == null ? "" : w.programmaNaam());
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.2f);
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", w != null);
        if (w != null) {
            data.putInt("Fase", w.fase.ordinal());
            data.putBoolean("Zeskamp", w.zeskamp);
            data.putInt("Onderdeel", w.onderdeel().ordinal());
            data.putBoolean("Mee", mee);
            data.putBoolean("Host", !w.deelnemers.isEmpty() && w.deelnemers.get(0).id.equals(player.getUUID()));
            data.putInt("Nog", Math.max(0, (INSCHRIJF_TICKS - w.ticks) / 20));
            ListTag namen = new ListTag();
            for (Deelnemer d : w.deelnemers) {
                if (!d.weg) {
                    namen.add(StringTag.valueOf(d.naam));
                }
            }
            data.put("Namen", namen);
        }
        for (Onderdeel o : Onderdeel.values()) {
            data.putInt("Best" + o.ordinal(), best(player, o));
        }
        data.putInt("BestZeskamp", bestZeskamp(player));
        data.putBoolean("Anker", Speelvelden.anker(npc) != null);
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new KnabbelspelenPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.SPELLEIDERGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        if (action == SHOP) {
            GuhQuests.say(player, npc, "quest.guhs.knabbelspelen.shop");
            npc.openShop(player);
        } else if (action == MEEDOEN) {
            Wedstrijd w = of(npc);
            if (w != null) {
                w.meedoen(npc, player);
            }
        } else if (action == NU) {
            Wedstrijd w = of(npc);
            if (w != null && w.fase == Fase.INSCHRIJVEN && !w.deelnemers.isEmpty() && w.deelnemers.get(0).id.equals(player.getUUID())) {
                w.nu = true;
            }
        } else if (action >= ONDERDEEL && action <= ZESKAMP) {
            start(npc, player, action == ZESKAMP ? null : Onderdeel.of(action - ONDERDEEL));
        }
    }

    /** Opens a round (one event, or the zeskamp when o is null); friends can join for a while. */
    @Nullable
    public static Wedstrijd start(GuhNpcEntity npc, ServerPlayer player, @Nullable Onderdeel o) {
        if (player.isSpectator() || !player.isAlive()) {
            return null;
        }
        Wedstrijd running = of(npc);
        if (running != null) {
            GuhQuests.say(player, npc, "quest.guhs.knabbelspelen.bezig", running.programmaNaam());
            return null;
        }
        if (isPlaying(player) || Minigames.refuse(player, npc, Minigames.KNABBELSPELEN)) {
            return null;
        }
        Anker anker = Speelvelden.anker(npc);
        if (anker == null) {
            GuhQuests.say(player, npc, "quest.guhs.knabbelspelen.broken");
            return null;
        }
        Wedstrijd w = new Wedstrijd(npc.getUUID(), anker, o == null ? List.of(Onderdeel.values()) : List.of(o), o == null);
        w.lastTick = npc.level().getGameTime();
        GAMES.put(npc.getUUID(), w);
        w.voegToe(player);
        GuhQuests.say(player, npc, "quest.guhs.knabbelspelen.open", w.programmaNaam(), INSCHRIJF_TICKS / 20);
        ((ServerLevel) npc.level()).playSound(null, npc.blockPosition(), KnabbelspelenFeature.FLUIT.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        return w;
    }

    void meedoen(GuhNpcEntity npc, ServerPlayer player) {
        if (deelnemer(player) != null) {
            GuhQuests.say(player, npc, "quest.guhs.knabbelspelen.wacht", programmaNaam());
        } else if (fase != Fase.INSCHRIJVEN) {
            GuhQuests.say(player, npc, "quest.guhs.knabbelspelen.te_laat");
        } else if (actief() >= MAX) {
            GuhQuests.say(player, npc, "quest.guhs.knabbelspelen.vol");
        } else if (!isPlaying(player) && !Minigames.refuse(player, npc, Minigames.KNABBELSPELEN) && !player.isSpectator()) {
            voegToe(player);
            for (ServerPlayer p : spelers((ServerLevel) npc.level())) {
                p.displayClientMessage(Component.translatable("quest.guhs.knabbelspelen.erbij", player.getDisplayName(), actief(), MAX)
                        .withStyle(ChatFormatting.AQUA), false);
            }
        }
    }

    /** (Also for the tests) adds a player to the round. */
    Deelnemer voegToe(ServerPlayer player) {
        Deelnemer d = new Deelnemer(player);
        deelnemers.add(d);
        PLAYERS.put(player.getUUID(), npcId);
        Minigames.startKeeping(player);
        return d;
    }

    int actief() {
        return (int) deelnemers.stream().filter(d -> !d.weg).count();
    }

    Component programmaNaam() {
        return zeskamp ? Component.translatable("gui.guhs.knabbelspelen.zeskamp") : programma.get(0).naam();
    }

    List<ServerPlayer> spelers(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (Deelnemer d : deelnemers) {
            if (!d.weg && level.getServer().getPlayerList().getPlayer(d.id) instanceof ServerPlayer p) {
                out.add(p);
            }
        }
        return out;
    }

    // --- every tick of Juf Vahoegsakee ----------------------------------------------------------------------------------

    void tick(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        lastTick = level.getGameTime();
        for (Deelnemer d : deelnemers) {
            if (d.weg) {
                continue;
            }
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(d.id);
            if (p == null || !p.isAlive() || p.level() != level || !Speelvelden.opTerrein(anker, p.position(), 12)) {
                verlaat(level, d, p, "quest.guhs.knabbelspelen.weggelopen");
            } else {
                Minigames.keep(p);
            }
        }
        if (actief() == 0) {
            eind(level, false);
            return;
        }
        ticks++;
        switch (fase) {
            case INSCHRIJVEN -> {
                if (ticks % 60 == 1) {
                    uitnodigen(npc, level);
                }
                if (nu || ticks >= INSCHRIJF_TICKS || actief() >= MAX) {
                    klaarzetten(level);
                }
            }
            case AFTELLEN -> aftellen(level);
            case BEZIG -> bezig(level);
            case UITSLAG -> {
                if (ticks >= UITSLAG_TICKS) {
                    volgende(npc, level);
                }
            }
        }
    }

    private void uitnodigen(GuhNpcEntity npc, ServerLevel level) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(32))) {
            if (deelnemer(p) == null && !p.isSpectator()) {
                p.displayClientMessage(Component.literal("<").append(npc.getDisplayName()).append("> ")
                        .append(Component.translatable("quest.guhs.knabbelspelen.uitnodiging", programmaNaam(), Math.max(1, (INSCHRIJF_TICKS - ticks) / 20)))
                        .withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    private void klaarzetten(ServerLevel level) {
        fase = Fase.AFTELLEN;
        ticks = 0;
        Onderdeel o = onderdeel();
        int baan = 0;
        for (Deelnemer d : deelnemers) {
            if (d.weg) {
                continue;
            }
            d.baan = baan++;
            d.klaar = false;
            d.staat = null;
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(d.id);
            if (p != null) {
                p.closeContainer();
                p.stopRiding();
                spel(o).klaarzetten(this, d, p, level);
                p.sendSystemMessage(Component.translatable("gui.guhs.knabbelspelen.uitleg." + o.id()).withStyle(ChatFormatting.AQUA));
            }
        }
    }

    private void aftellen(ServerLevel level) {
        Onderdeel o = onderdeel();
        for (Deelnemer d : deelnemers) {
            ServerPlayer p = d.weg ? null : level.getServer().getPlayerList().getPlayer(d.id);
            if (p == null) {
                continue;
            }
            spel(o).aftellen(this, d, p, level);
            if (ticks == 1 || ticks == 21 || ticks == 41) {
                int n = 3 - (ticks - 1) / 20;
                title(p, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), o.naam(), 0, 22, 2);
                level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 0.8f + 0.2f * (3 - n));
            }
        }
        if (ticks >= AFTEL_TICKS) {
            fase = Fase.BEZIG;
            ticks = 0;
            for (Deelnemer d : deelnemers) {
                ServerPlayer p = d.weg ? null : level.getServer().getPlayerList().getPlayer(d.id);
                if (p != null) {
                    title(p, Component.translatable("quest.guhs.knabbelspelen.title.go").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.knabbelspelen.kort." + o.id()), 0, 30, 10);
                    level.playSound(null, p.blockPosition(), KnabbelspelenFeature.FLUIT.get(), SoundSource.PLAYERS, 1f, 1.3f);
                    spel(o).start(this, d, p, level);
                }
            }
        }
    }

    private void bezig(ServerLevel level) {
        Onderdeel o = onderdeel();
        boolean allemaal = true;
        for (Deelnemer d : deelnemers) {
            if (d.weg || d.klaar) {
                continue;
            }
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(d.id);
            if (p == null) {
                continue;
            }
            spel(o).tick(this, d, p, level, ticks);
            if (!d.klaar) {
                allemaal = false;
                if (ticks >= o.maxTicks) {
                    klaar(d, p, spel(o).eindScore(d), true);
                } else if (o.maxTicks - ticks <= 100 && (o.maxTicks - ticks) % 20 == 0) {
                    level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1f, 1.6f);
                }
            }
        }
        if (allemaal || ticks >= o.maxTicks) {
            uitslag(level);
        }
    }

    /** This player is done with the event (a time, or points). */
    public void klaar(Deelnemer d, ServerPlayer p, int score, boolean tijdOp) {
        if (d.klaar) {
            return;
        }
        nl.juiced.guhs.feature.samen.SamenSpel.uitslag(p, "knabbelspelen", score >= 0); // samen
        Onderdeel o = onderdeel();
        d.klaar = true;
        d.score[o.ordinal()] = score;
        d.punten[o.ordinal()] = o.zeskamp(score);
        ServerLevel level = p.serverLevel();
        if (score < 0) {
            p.displayClientMessage(Component.translatable("quest.guhs.knabbelspelen.niet_gehaald").withStyle(ChatFormatting.GRAY), false);
        } else {
            title(p, Component.translatable(tijdOp ? "quest.guhs.knabbelspelen.title.tijd_op" : "quest.guhs.knabbelspelen.title.klaar")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), uitslagTekst(o, score), 0, 40, 10);
            p.displayClientMessage(Component.translatable("quest.guhs.knabbelspelen.jouw_uitslag", o.naam(), uitslagTekst(o, score), d.punten[o.ordinal()])
                    .withStyle(ChatFormatting.AQUA), false);
            level.playSound(null, p.blockPosition(), KnabbelspelenFeature.JUICH.get(), SoundSource.PLAYERS, 1f, 1f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.5, p.getZ(), 10, 0.5, 0.4, 0.5, 0.02);
        }
    }

    private void uitslag(ServerLevel level) {
        Onderdeel o = onderdeel();
        fase = Fase.UITSLAG;
        ticks = 0;
        List<Deelnemer> rij = new ArrayList<>(deelnemers.stream().filter(d -> !d.weg).toList());
        rij.sort(Comparator.comparingInt((Deelnemer d) -> -d.punten[o.ordinal()]));
        MutableComponent tekst = Component.translatable("quest.guhs.knabbelspelen.uitslag", o.naam()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        int plek = 1;
        for (Deelnemer d : rij) {
            int s = d.score[o.ordinal()];
            tekst.append(Component.literal("\n " + plek++ + ". " + d.naam + " - ").withStyle(ChatFormatting.WHITE))
                    .append(s < 0 ? Component.translatable("quest.guhs.knabbelspelen.dnf") : uitslagTekst(o, s))
                    .append(Component.literal(" (" + d.punten[o.ordinal()] + ")").withStyle(ChatFormatting.GRAY));
        }
        for (Deelnemer d : deelnemers) {
            ServerPlayer p = d.weg ? null : level.getServer().getPlayerList().getPlayer(d.id);
            spel(o).einde(this, d, p, level);
            if (p == null) {
                continue;
            }
            neemTerug(p);
            p.sendSystemMessage(tekst);
            int s = d.score[o.ordinal()];
            if (s >= 0 && (o.tijd || s > 0)) {
                Scorebord.submit(p, o.board(), s, o.tijd);
                int best = best(p, o);
                if (best < 0 || (o.tijd ? s < best : s > best)) {
                    GuhQuests.saved(p).putInt(BEST_KEY + o.id(), s);
                    p.sendSystemMessage(Component.translatable("quest.guhs.knabbelspelen.record", o.naam(), uitslagTekst(o, s)).withStyle(ChatFormatting.YELLOW));
                }
            }
            Adv.grant(p, "knabbelspelen_gespeeld");
            if (o == Onderdeel.GUHGUHTJE_PRIK && s >= 950) {
                Adv.grant(p, "knabbelspelen_raak");
            }
        }
        showScores(level, anker);
    }

    private void volgende(GuhNpcEntity npc, ServerLevel level) {
        index++;
        if (index < programma.size()) {
            for (ServerPlayer p : spelers(level)) {
                p.displayClientMessage(Component.translatable("quest.guhs.knabbelspelen.volgende", onderdeel().naam(), index + 1, programma.size())
                        .withStyle(ChatFormatting.AQUA), false);
            }
            klaarzetten(level);
            return;
        }
        beloning(npc, level);
        eind(level, true);
    }

    // --- rewards ---------------------------------------------------------------------------------------------------------

    /** Lintjes for one event: 1, +1 from 500 zeskamp points, +1 from 850, and +1 like every reward. */
    public static int lintjes(int punten) {
        return 1 + (punten >= 500 ? 1 : 0) + (punten >= 850 ? 1 : 0) + 1;
    }

    /** Lintjes for the whole zeskamp: 2 + one per 1000 points, and +1 like every reward. */
    public static int zeskampLintjes(int totaal) {
        return 2 + totaal / 1000 + 1;
    }

    private void beloning(GuhNpcEntity npc, ServerLevel level) {
        List<Deelnemer> rij = new ArrayList<>(deelnemers.stream().filter(d -> !d.weg).toList());
        rij.sort(Comparator.comparingInt(d -> -d.totaal()));
        for (Deelnemer d : rij) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(d.id);
            if (p == null) {
                continue;
            }
            CompoundTag saved = GuhQuests.saved(p);
            saved.putBoolean(PLAYED_KEY, true);
            saved.putInt(GAMES_KEY, saved.getInt(GAMES_KEY) + 1);
            int lintjes;
            if (zeskamp) {
                int totaal = d.totaal();
                lintjes = zeskampLintjes(totaal);
                boolean winnaar = rij.size() >= 2 && rij.get(0) == d && totaal > 0;
                if (winnaar) {
                    lintjes++;
                    Adv.grant(p, "knabbelspelen_winnaar");
                }
                Scorebord.submit(p, Onderdeel.ZESKAMP_BOARD, totaal, false);
                int best = bestZeskamp(p);
                if (totaal > best) {
                    saved.putInt(BEST_KEY + "zeskamp", totaal);
                }
                Adv.grant(p, "knabbelspelen_zeskamp");
                if (totaal >= 5000) {
                    Adv.grant(p, "knabbelspelen_zeskamp_5000");
                }
                title(p, Component.translatable(winnaar ? "quest.guhs.knabbelspelen.title.winnaar" : "quest.guhs.knabbelspelen.title.zeskamp")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.translatable("quest.guhs.knabbelspelen.title.totaal", totaal), 5, 70, 20);
                GuhQuests.say(p, npc, totaal >= 4500 ? "quest.guhs.knabbelspelen.end.super" : totaal >= 2500 ? "quest.guhs.knabbelspelen.end.good"
                        : "quest.guhs.knabbelspelen.end.ok", totaal);
            } else {
                Onderdeel o = programma.get(0);
                lintjes = lintjes(d.punten[o.ordinal()]);
                GuhQuests.say(p, npc, d.punten[o.ordinal()] >= 850 ? "quest.guhs.knabbelspelen.end.super1" : "quest.guhs.knabbelspelen.end.ok1", o.naam());
            }
            if (actief() >= 2) {
                Adv.grant(p, "knabbelspelen_samen");
            }
            Minigames.give(p, new ItemStack(KnabbelspelenFeature.SPELENLINTJE.get(), lintjes));
            p.sendSystemMessage(Component.translatable("quest.guhs.knabbelspelen.lintjes", lintjes).withStyle(ChatFormatting.YELLOW));
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.2f);
        }
        showScores(level, anker);
    }

    // --- the end ---------------------------------------------------------------------------------------------------------

    /** A player left the round (walked off, logged out, died): their event is cleaned up, no lintjes. */
    void verlaat(ServerLevel level, Deelnemer d, @Nullable ServerPlayer p, String key) {
        if (d.weg) {
            return;
        }
        if (fase == Fase.AFTELLEN || fase == Fase.BEZIG) {
            spel(onderdeel()).einde(this, d, p, level);
        }
        d.weg = true;
        PLAYERS.remove(d.id, npcId);
        if (p != null) {
            neemTerug(p);
            p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    void eind(ServerLevel level, boolean netjes) {
        GAMES.remove(npcId, this);
        for (Deelnemer d : deelnemers) {
            if (!d.weg) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(d.id);
                if (!netjes && (fase == Fase.AFTELLEN || fase == Fase.BEZIG)) {
                    spel(onderdeel()).einde(this, d, p, level);
                }
                if (p != null) {
                    neemTerug(p);
                }
            }
            PLAYERS.remove(d.id, npcId);
        }
    }

    /** Stops this player's round part (without rewards). */
    public static void stopFor(ServerPlayer p) {
        Wedstrijd w = van(p);
        if (w != null) {
            Deelnemer d = w.deelnemer(p);
            if (d != null) {
                w.verlaat(p.serverLevel(), d, p, "quest.guhs.knabbelspelen.gestopt");
            }
        }
        PLAYERS.remove(p.getUUID());
        neemTerug(p);
    }

    /** Takes the loaned things of the Knabbelspelen back, the effects go, the blindfold comes off. */
    public static void neemTerug(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (KnabbelspelenFeature.geleend(inv.getItem(i))) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (KnabbelspelenFeature.geleend(p.containerMenu.getCarried())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
        GuhguhtjePrik.blinddoek(p, false);
        for (var effect : List.of(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, net.minecraft.world.effect.MobEffects.BLINDNESS,
                net.minecraft.world.effect.MobEffects.DARKNESS)) {
            MobEffectInstance e = p.getEffect(effect);
            if (e != null && e.getDuration() <= 20 * 200 && !e.isVisible()) {
                p.removeEffect(effect);
            }
        }
    }

    // --- helpers for the events ------------------------------------------------------------------------------------------

    /** To the start of your lane, looking along it. */
    public void naarStart(Deelnemer d, ServerPlayer p, ServerLevel level, double u) {
        Vec3 s = Speelvelden.punt(anker, onderdeel(), d.baan, u, 0, Speelvelden.G + 1);
        teleport(p, level, s.x, s.y, s.z, Speelvelden.yaw(anker, onderdeel()));
    }

    /** During the countdown you stay at the start. */
    void houdBijStart(Deelnemer d, ServerPlayer p, ServerLevel level) {
        double[] b = Speelvelden.baan(anker, onderdeel(), d.baan, p.position());
        double u0 = spel(onderdeel()).startU();
        if (Math.abs(b[0] - u0) > 1.2 || Math.abs(b[1]) > 1.8) {
            Vec3 s = Speelvelden.punt(anker, onderdeel(), d.baan, u0, 0, Speelvelden.G + 1);
            teleport(p, level, s.x, s.y, s.z, p.getYRot());
        }
    }

    /** Puts a loaned thing in your hand (a free hotbar slot, which becomes the selected one). */
    public static void inHand(ServerPlayer p, ItemStack stack) {
        Inventory inv = p.getInventory();
        int slot = inv.getItem(inv.selected).isEmpty() ? inv.selected : -1;
        for (int i = 0; i < 9 && slot < 0; i++) {
            if (inv.getItem(i).isEmpty()) {
                slot = i;
            }
        }
        if (slot < 0) {
            slot = inv.selected;
            ItemStack oud = inv.getItem(slot);
            inv.setItem(slot, ItemStack.EMPTY);
            if (!inv.add(oud)) {
                p.drop(oud, false);
            }
        }
        inv.setItem(slot, stack);
        inv.selected = slot;
        if (!(p instanceof FakePlayer) && p.connection != null) {
            p.connection.send(new ClientboundSetCarriedItemPacket(slot));
        }
    }

    /** An effect for the event (hidden, so neemTerug knows it's ours). */
    public static void effect(ServerPlayer p, net.minecraft.core.Holder<MobEffect> effect, int ticks, int level) {
        p.addEffect(new MobEffectInstance(effect, ticks, level, false, false, false));
    }

    public static Component uitslagTekst(Onderdeel o, int score) {
        return o.tijd ? Component.literal(Highscores.tijd(score)) : Component.translatable("quest.guhs.knabbelspelen.punten", score);
    }

    public static void teleport(ServerPlayer p, ServerLevel world, double x, double y, double z, float yRot) {
        if (p instanceof FakePlayer || p.connection == null) {
            p.moveTo(x, y, z, yRot, 0);
        } else {
            p.teleportTo(world, x, y, z, yRot, 0);
        }
    }

    public static void title(ServerPlayer p, Component title, Component subtitle, int in, int stay, int out) {
        if (p instanceof FakePlayer || p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    // --- records and the boards ------------------------------------------------------------------------------------------

    static final String BEST_KEY = "guhs_spelen_best_", GAMES_KEY = "guhs_spelen_games", PLAYED_KEY = "guhs_spelen_played";

    /** Your record of an event (points or ticks), -1 when you never did it. */
    public static int best(Player player, Onderdeel o) {
        CompoundTag saved = GuhQuests.saved(player);
        return saved.contains(BEST_KEY + o.id()) ? saved.getInt(BEST_KEY + o.id()) : -1;
    }

    public static int bestZeskamp(Player player) {
        return GuhQuests.saved(player).getInt(BEST_KEY + "zeskamp");
    }

    /** The floating boards: the zeskamp in the tent (over Juf Vahoegsakee), one per event over its field. */
    public static void showScores(ServerLevel level, Anker anker) {
        var server = level.getServer();
        Scorebord.show(level, anker.punt(Speelvelden.AX + 0.5, Speelvelden.G + 3.6, Speelvelden.AZ + 0.5), "knabbelspelen",
                Scorebord.text(server, Component.translatable("gui.guhs.scorebord.knabbelspelen"), List.of(Onderdeel.ZESKAMP_BOARD),
                        List.of(Component.translatable("gui.guhs.knabbelspelen.zeskamp")), s -> s + " pt"));
        for (Onderdeel o : Onderdeel.values()) {
            Scorebord.show(level, Speelvelden.bord(anker, o), "knabbelspelen_" + o.id(),
                    Scorebord.text(server, o.naam(), List.of(o.board()), List.of(Component.translatable("gui.guhs.knabbelspelen.kort." + o.id())),
                            o.tijd ? Highscores::tijd : s -> s + " pt"));
        }
    }

    // --- events ----------------------------------------------------------------------------------------------------------

    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && isPlaying(p) && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p);
        }
    }

    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p);
        }
    }

    public static void onChangeDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p);
        }
    }

    /** A round whose Juf stopped ticking (her chunk unloaded) is over; loaned things left over after a restart go. */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) {
            return;
        }
        Wedstrijd w = van(p);
        if (w == null) {
            PLAYERS.remove(p.getUUID());
            if (p.tickCount % 200 == 0 && KnabbelspelenFeature.heeftGeleend(p)) {
                neemTerug(p);
            }
            return;
        }
        if (p.serverLevel().getGameTime() - w.lastTick > 40) {
            w.eind(p.serverLevel(), false);
            neemTerug(p);
            p.sendSystemMessage(Component.translatable("quest.guhs.knabbelspelen.gestopt").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        GAMES.clear();
        PLAYERS.clear();
    }

    // --- (tests) ---------------------------------------------------------------------------------------------------------

    /** Skip the waiting for friends and the countdown: straight to the whistle of the current event. */
    void meteen(ServerLevel level) {
        if (fase == Fase.INSCHRIJVEN) {
            klaarzetten(level);
        }
        ticks = AFTEL_TICKS;
        aftellen(level);
    }

    int index() {
        return index;
    }
}
