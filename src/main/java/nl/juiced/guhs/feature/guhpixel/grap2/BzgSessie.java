package nl.juiced.guhs.feature.guhpixel.grap2;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.emotes.EmotesFeature;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.SessieStart;
import nl.juiced.guhs.feature.guhpixel.Vertrek;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModSounds;

/**
 * One episode of Boer zoekt Guh, in five steps (the saved questline of the Grap "bzg"):
 * <ol>
 *   <li>INTRO: talk to Presentatrice Guhvon in the studio (the show intro);</li>
 *   <li>BRIEVEN: take the three letters out of the mailbox and read all three;</li>
 *   <li>LOGEREN: the logeerweek on the farm set: the three candidates and Boer Guhrrit sleep, tuck all four in;</li>
 *   <li>KEUZE: Boer Guhrrit has to choose; whatever you advise, he cannot;</li>
 *   <li>SAMEN / AFTITELING: they all become friends and nap together; the credits roll.</li>
 * </ol>
 * The keepsakes and the 100 muntjes are handed out in {@link #spelerWeg} (the player has their own inventory back then).
 */
public final class BzgSessie extends Sessie {
    enum Fase { INTRO, BRIEVEN, LOGEREN, KEUZE, SAMEN, AFTITELING }

    static final String SCENE_INTRO = "bzg_intro", SCENE_KEUZE = "bzg_keuze", SCENE_SAMEN = "bzg_samen";
    /** All three letters / all four sleepers. */
    static final int ALLE_BRIEVEN = 7, ALLEN_INGESTOPT = 15, BOER_BIT = 8;
    static final int AFTITELING_MIN = 40, AFTITELING_MAX = 20 * 75, BRIEVEN_WACHT = 20 * 30, MAX_TICKS = 20 * 60 * 20;

    private Fase fase = Fase.INTRO;
    @Nullable
    private GuhNpcEntity guhvon, boer;
    private final List<StandInGuh> kandidaten = new ArrayList<>();
    private int gelezen, ingestopt, brievenOver = -1, aftitelingSinds = -1, keuze = -1;
    private boolean beloon;

    BzgSessie(SessieStart start) {
        super(start);
    }

    Fase fase() {
        return fase;
    }

    int gelezenMasker() {
        return gelezen;
    }

    int ingestoptMasker() {
        return ingestopt;
    }

    List<StandInGuh> kandidaten() {
        return kandidaten;
    }

    @Nullable
    GuhNpcEntity boer() {
        return boer;
    }

    @Nullable
    GuhNpcEntity guhvon() {
        return guhvon;
    }

    @Override
    protected void begin() {
        ServerLevel level = level();
        guhvon = Grap2Slice.npc(level, GuhNpcEntity.Kind.BZG_PRESENTATRICE, arena().wereld(Bzg.GUHVON), 0f);
        boer = Grap2Slice.npc(level, GuhNpcEntity.Kind.BZG_BOER, arena().wereld(Bzg.BOER), 0f);
        GuhClothes[] kleren = {GuhClothes.HEART_GLASSES, GuhClothes.SLAAPMUTSJE, GuhClothes.RED_BOWTIE};
        for (int i = 0; i < Bzg.KANDIDATEN; i++) {
            CompoundTag looks = new CompoundTag();
            looks.putString("Variant", Bzg.SOORTEN[i]);
            CompoundTag c = new CompoundTag();
            c.putString(kleren[i].slot.name(), kleren[i].id());
            looks.put("Clothes", c);
            StandInGuh g = Grap2Slice.standIn(level, Grap2Slice.BZG_GUH.get(), arena().wereld(Bzg.BANK[i]), 0f, looks, Bzg.kandidaat(i));
            if (g != null) {
                g.rol(i);
                kandidaten.add(g);
            }
        }
        Scorebord.show(level, arena().wereld(Bzg.GUHVON).add(0, 2.4, 0), "bzg_guhvon",
                Component.translatable("gui.guhs.bzg.studio.klik").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        for (ServerPlayer p : spelers()) {
            Grappen.zetStap(p, Bzg.ID, 1);
            if (guhvon != null) {
                GuhQuests.say(p, guhvon, "quest.guhs.bzg.studio.welkom");
            }
        }
    }

    @Override
    protected void tick() {
        if (ticks() > MAX_TICKS) {
            stop();
            return;
        }
        for (ServerPlayer p : spelers()) {
            if (brievenOver > 0 && --brievenOver == 0) {
                brievenDicht(p);
            }
            if (fase == Fase.AFTITELING && ticks() - aftitelingSinds > AFTITELING_MAX) {
                klaar(p);
                return;
            }
        }
        if ((fase == Fase.LOGEREN || fase == Fase.AFTITELING) && boer != null && ticks() % 45 == 0) {
            level().sendParticles(EmotesFeature.GUH_ZZZ.get(), boer.getX(), boer.getY() + 1.5, boer.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
        }
    }

    @Override
    public boolean magGebruiken(ServerPlayer p, BlockPos pos, BlockState s) {
        return s.is(Grap2Slice.BRIEVENBUS.get());
    }

    @Override
    public boolean magEntiteit(ServerPlayer p, Entity e) {
        return true;
    }

    // --- step 1: the intro -------------------------------------------------------------------------------------------------

    void praatGuhvon(ServerPlayer p) {
        if (!speelt(p) || guhvon == null) {
            return;
        }
        switch (fase) {
            case INTRO -> Praat.scene(p, SCENE_INTRO, List.of(
                    new Praat.Regel(guhvon, "", "quest.guhs.bzg.intro.1"),
                    new Praat.Regel(guhvon, "", "quest.guhs.bzg.intro.2"),
                    new Praat.Regel(guhvon, "", "quest.guhs.bzg.intro.3"),
                    new Praat.Regel(guhvon, "", "quest.guhs.bzg.intro.4")));
            case BRIEVEN -> GuhQuests.say(p, guhvon, "quest.guhs.bzg.studio.brieven");
            case LOGEREN -> GuhQuests.say(p, guhvon, "quest.guhs.bzg.studio.logeren");
            case KEUZE -> GuhQuests.say(p, guhvon, "quest.guhs.bzg.studio.keuze");
            default -> GuhQuests.say(p, guhvon, "quest.guhs.bzg.studio.samen");
        }
    }

    /** The intro was read to the end (or closed): on to the letters. */
    void introKlaar(ServerPlayer p) {
        if (!speelt(p) || fase != Fase.INTRO) {
            return;
        }
        fase = Fase.BRIEVEN;
        Grappen.zetStap(p, Bzg.ID, 2);
        BlockPos bus = arena().wereld(Bzg.BRIEVENBUS.getX(), Bzg.BRIEVENBUS.getY(), Bzg.BRIEVENBUS.getZ());
        level().sendParticles(ParticleTypes.HAPPY_VILLAGER, bus.getX() + 0.5, bus.getY() + 1.4, bus.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.0);
        level().playSound(null, bus, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8f, 1.4f);
    }

    // --- step 2: the letters -----------------------------------------------------------------------------------------------

    void brievenbus(ServerPlayer p, BlockPos pos) {
        if (!speelt(p)) {
            return;
        }
        if (fase == Fase.INTRO) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bzg.brievenbus.eerst").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        level().playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.9f, 1.0f);
        ModNetworking.sendTo(p, new Grap2Payloads.BzgScherm(brievenStand()));
    }

    CompoundTag brievenStand() {
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "brieven");
        data.putInt("Gelezen", gelezen);
        data.putBoolean("Telt", fase == Fase.BRIEVEN);
        return data;
    }

    /** The player opened letter i (0..2). Only counts in step 2. */
    boolean gelezen(ServerPlayer p, int i) {
        if (!speelt(p) || fase != Fase.BRIEVEN || i < 0 || i >= Bzg.BRIEVEN) {
            return false;
        }
        gelezen |= 1 << i;
        if (gelezen == ALLE_BRIEVEN && brievenOver < 0) {
            brievenOver = BRIEVEN_WACHT;   // (goes on by itself when the screen never reports "closed")
        }
        return true;
    }

    /** The letter screen was closed: with all three read the logeerweek begins. */
    void brievenDicht(ServerPlayer p) {
        if (!speelt(p) || fase != Fase.BRIEVEN) {
            return;
        }
        if (gelezen != ALLE_BRIEVEN) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bzg.brieven.nog", Integer.bitCount(gelezen), Bzg.BRIEVEN).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        brievenOver = -1;
        fase = Fase.LOGEREN;
        ServerLevel level = level();
        for (int i = 0; i < kandidaten.size(); i++) {
            StandInGuh g = kandidaten.get(i);
            Vec3 van = g.position();
            level.sendParticles(ParticleTypes.POOF, van.x, van.y + 0.4, van.z, 10, 0.3, 0.3, 0.3, 0.02);
            Vec3 naar = arena().wereld(Bzg.HOOI[i]);
            float yaw = 180f + (i - 1) * 25f;
            g.snapTo(naar.x, naar.y, naar.z, yaw, 0f);
            g.setYHeadRot(yaw);
            g.setYBodyRot(yaw);
            g.slaap(true);
        }
        Grappen.zetStap(p, Bzg.ID, 3);
        PxGeluid.titel(p, Component.translatable("gui.guhs.bzg.logeerweek.titel").withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.guhs.bzg.logeerweek.onder").withStyle(ChatFormatting.LIGHT_PURPLE), 60);
        if (guhvon != null) {
            GuhQuests.say(p, guhvon, "quest.guhs.bzg.studio.logeren");
        }
    }

    // --- step 3: the logeerweek ----------------------------------------------------------------------------------------------

    /** A click on a stand-in guh of this show. */
    void klikGuh(ServerPlayer p, StandInGuh guh) {
        if (!speelt(p) || !kandidaten.contains(guh)) {
            return;
        }
        int i = kandidaten.indexOf(guh);
        switch (fase) {
            case INTRO, BRIEVEN -> GuhQuests.say(p, guh, "quest.guhs.bzg.kandidaat.hoi." + (i + 1));
            case LOGEREN -> stopIn(p, 1 << i, guh, guh.getName(), 0.62);
            case KEUZE -> p.sendOverlayMessage(Component.translatable("gui.guhs.bzg.slaapt", guh.getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            default -> level().sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + 1.0, guh.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
        }
    }

    private void stopIn(ServerPlayer p, int bit, Entity wie, Component naam, double hoogte) {
        if ((ingestopt & bit) != 0) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bzg.al_ingestopt", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        ingestopt |= bit;
        Bzg.telIngestopt(p);
        ServerLevel level = level();
        Grap2Slice.dekentje(level, wie.position().add(0, hoogte, 0), wie.getYRot());
        level.sendParticles(ParticleTypes.HEART, wie.getX(), wie.getY() + 1.1, wie.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, wie.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.9f, 1.2f);
        int n = Integer.bitCount(ingestopt);
        p.sendOverlayMessage(Component.translatable("gui.guhs.bzg.ingestopt", 2 * n - 1, naam).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (ingestopt == ALLEN_INGESTOPT) {
            fase = Fase.KEUZE;
            Grappen.zetStap(p, Bzg.ID, 4);
            PxGeluid.titel(p, Component.translatable("gui.guhs.bzg.keuze.titel").withStyle(ChatFormatting.GOLD),
                    Component.translatable("gui.guhs.bzg.keuze.onder").withStyle(ChatFormatting.LIGHT_PURPLE), 60);
            if (boer != null) {
                boer.playSound(ModSounds.GUH_AMBIENT.get(), 0.9f, 0.8f);
                GuhQuests.say(p, boer, "quest.guhs.bzg.boer.wakker");
            }
        }
    }

    // --- step 4: the choice --------------------------------------------------------------------------------------------------

    void praatBoer(ServerPlayer p) {
        if (!speelt(p) || boer == null) {
            return;
        }
        switch (fase) {
            case INTRO, BRIEVEN -> GuhQuests.say(p, boer, "quest.guhs.bzg.boer.vroeg");
            case LOGEREN -> stopIn(p, BOER_BIT, boer, boer.getName(), 1.12);
            case KEUZE -> Praat.open(p, boer, SCENE_KEUZE, "quest.guhs.bzg.keuze.vraag", new Object[0],
                    new Praat.Optie(0, "quest.guhs.bzg.keuze.optie.1"), new Praat.Optie(1, "quest.guhs.bzg.keuze.optie.2"),
                    new Praat.Optie(2, "quest.guhs.bzg.keuze.optie.3"), new Praat.Optie(3, "quest.guhs.bzg.keuze.optie.4"));
            default -> p.sendOverlayMessage(Component.translatable("gui.guhs.bzg.slaapt", boer.getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The player's advice (0..2 a candidate, 3 "all of them"): it makes no difference, he cannot choose. */
    boolean keuze(ServerPlayer p, int optie) {
        if (!speelt(p) || fase != Fase.KEUZE || optie < 0 || optie > 3 || boer == null) {
            return false;
        }
        keuze = optie;
        fase = Fase.SAMEN;
        List<Praat.Regel> regels = new ArrayList<>();
        if (optie < Bzg.KANDIDATEN) {
            regels.add(new Praat.Regel(boer, "", "quest.guhs.bzg.samen.twijfel", Bzg.kandidaat(optie)));
        } else {
            regels.add(new Praat.Regel(boer, "", "quest.guhs.bzg.samen.allemaal"));
        }
        regels.add(new Praat.Regel(boer, "", "quest.guhs.bzg.samen.2"));
        if (!kandidaten.isEmpty()) {
            regels.add(new Praat.Regel(kandidaten.get(Math.min(1, kandidaten.size() - 1)), "", "quest.guhs.bzg.samen.3"));
        }
        if (guhvon != null) {
            regels.add(new Praat.Regel(guhvon, "", "quest.guhs.bzg.samen.4"));
        }
        Praat.scene(p, SCENE_SAMEN, regels);
        return true;
    }

    int keuze() {
        return keuze;
    }

    // --- step 5: everybody naps together, the credits -------------------------------------------------------------------------

    /** The last scene was read: the candidates lie down around the farmer, the credits roll. */
    void samenKlaar(ServerPlayer p) {
        if (!speelt(p) || fase != Fase.SAMEN) {
            return;
        }
        fase = Fase.AFTITELING;
        aftitelingSinds = ticks();
        beloon = true;
        ServerLevel level = level();
        for (int i = 0; i < kandidaten.size(); i++) {
            StandInGuh g = kandidaten.get(i);
            Vec3 naar = arena().wereld(Bzg.SAMEN[i]);
            float yaw = (i - 1) * -30f;
            g.snapTo(naar.x, naar.y, naar.z, yaw, 0f);
            g.setYHeadRot(yaw);
            g.setYBodyRot(yaw);
            g.wear(GuhClothes.BZG_STROHOED);
            g.slaap(true);
            level.sendParticles(ParticleTypes.HEART, naar.x, naar.y + 1.0, naar.z, 3, 0.3, 0.2, 0.3, 0.0);
        }
        Grappen.zetStap(p, Bzg.ID, 5);
        Grap2Slice.vuurwerk(level, arena().wereld(Bzg.BOER.add(0, 0, 6)));
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "aftiteling");
        data.putInt("Regels", Bzg.AFTITELING_REGELS);
        ModNetworking.sendTo(p, new Grap2Payloads.BzgScherm(data));
    }

    /** The credits screen closed (not sooner than two seconds after it opened): back to the lobby. */
    boolean aftitelingKlaar(ServerPlayer p) {
        if (!speelt(p) || fase != Fase.AFTITELING || ticks() - aftitelingSinds < AFTITELING_MIN) {
            return false;
        }
        klaar(p);
        return true;
    }

    @Override
    protected void spelerWeg(ServerPlayer p, Vertrek reden) {
        if (beloon) {
            beloon = false;
            Grappen.voltooi(p, Bzg.ID);
        }
    }
}
