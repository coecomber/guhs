package nl.juiced.guhs.feature.guhpixel.grap2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.emotes.EmotesFeature;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.SessieStart;
import nl.juiced.guhs.feature.guhpixel.Vertrek;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.taal.Tekst;

/**
 * One visit to the gym of Gymleider Dutjes: walk in, challenge him (click him or step on the challenger's mat), pick a guh,
 * battle ({@link GuhmonGevecht}), and after a win get the badge. The battle lives here on the server; the screen only
 * shows what this session sends and sends back which button was pressed.
 * <p>
 * The keepsakes and the 100 muntjes are handed out in {@link #spelerWeg}: only then does the player have their own
 * inventory back (anything given during the game would vanish with the game inventory).
 */
public final class GuhmonSessie extends Sessie {
    enum Fase { AANKOMST, KIEZEN, GEVECHT, GEWONNEN }

    /** How long a won battle waits for "Verder" before it goes on by itself, the walk-out after the speech, the longest visit. */
    static final int AUTO_VERDER = 20 * 25, UITLOOP = 20 * 5, MAX_TICKS = 20 * 60 * 15;

    private final RandomSource random;
    private Fase fase = Fase.AANKOMST;
    @Nullable
    private GuhNpcEntity leider;
    @Nullable
    private StandInGuh mijn, tegen;
    @Nullable
    private GuhmonGevecht gevecht;
    private CompoundTag looks = new CompoundTag();
    private Component naam = Component.empty();
    private boolean leenguh, opVak, beloon;
    private int autoVerder = -1, wegOver = -1, verlorenNu;

    GuhmonSessie(SessieStart start) {
        super(start);
        this.random = RandomSource.create(start.opties().getLongOr("Seed", start.id().getLeastSignificantBits()));
    }

    Fase fase() {
        return fase;
    }

    @Nullable
    GuhmonGevecht gevecht() {
        return gevecht;
    }

    @Nullable
    StandInGuh mijn() {
        return mijn;
    }

    @Nullable
    GuhNpcEntity leider() {
        return leider;
    }

    boolean leenguh() {
        return leenguh;
    }

    private static CompoundTag tegenLooks() {
        CompoundTag l = new CompoundTag();
        l.putString("Variant", "starry");
        CompoundTag c = new CompoundTag();
        c.putString(GuhClothes.Slot.HEAD.name(), GuhClothes.SLAAPMUTSJE.id());
        l.put("Clothes", c);
        return l;
    }

    private static CompoundTag leenLooks() {
        CompoundTag l = new CompoundTag();
        l.putString("Variant", "mint");
        CompoundTag c = new CompoundTag();
        c.putString(GuhClothes.Slot.HEAD.name(), GuhClothes.GUHMON_TRAINERSPET.id());
        l.put("Clothes", c);
        return l;
    }

    private static Component tegenNaam() {
        return Component.translatable("gui.guhs.guhmon.tegen.naam");
    }

    @Override
    protected void begin() {
        ServerLevel level = level();
        leider = Grap2Slice.npc(level, GuhNpcEntity.Kind.GUHMON_GYMLEIDER, arena().wereld(Guhmon.LEIDER), 0f);
        tegen = Grap2Slice.standIn(level, Grap2Slice.GUHMON_GUH.get(), arena().wereld(Guhmon.TEGEN_WACHT), 0f, tegenLooks(), tegenNaam());
        String[] soorten = {"choco", "snow", "mint", "normal", "golden", "rainbow"};
        for (int i = 0; i < Guhmon.PUBLIEK.length; i++) {
            CompoundTag l = new CompoundTag();
            l.putString("Variant", soorten[i % soorten.length]);
            StandInGuh g = Grap2Slice.standIn(level, Grap2Slice.GUHMON_GUH.get(), arena().wereld(Guhmon.PUBLIEK[i]), i < 3 ? -90f : 90f, l, null);
            if (g != null) {
                g.slaap(true);
            }
        }
        Scorebord.show(level, arena().wereld(Guhmon.LEIDER).add(0, 2.4, 0), "guhmon_leider",
                Component.translatable("gui.guhs.guhmon.gym.klik").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        for (ServerPlayer p : spelers()) {
            Grappen.zetStap(p, Guhmon.ID, 1);
            if (leider != null) {
                GuhQuests.say(p, leider, "quest.guhs.guhmon.gym.welkom");
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
            boolean nu = p.position().distanceToSqr(arena().wereld(Guhmon.VAK)) < 1.6 * 1.6;
            if (nu && !opVak && (fase == Fase.AANKOMST || fase == Fase.KIEZEN || fase == Fase.GEVECHT)) {
                daagUit(p);
            }
            opVak = nu;
            if (autoVerder > 0 && --autoVerder == 0) {
                verder(p);
            }
            if (wegOver > 0 && --wegOver == 0) {
                klaar(p);
                return;
            }
        }
    }

    @Override
    public boolean magEntiteit(ServerPlayer p, Entity e) {
        return true;
    }

    // --- challenge, pick ---------------------------------------------------------------------------------------------------

    /** The player clicked the gym leader or stepped on the challenger's mat: the picker, or the running battle again. */
    void daagUit(ServerPlayer p) {
        if (!speelt(p)) {
            return;
        }
        switch (fase) {
            case AANKOMST, KIEZEN -> {
                fase = Fase.KIEZEN;
                Grappen.zetStap(p, Guhmon.ID, 2);
                ModNetworking.sendTo(p, new Grap2Payloads.GuhmonScherm(kiesStand(p)));
            }
            case GEVECHT -> ModNetworking.sendTo(p, new Grap2Payloads.GuhmonScherm(stand(false, List.of())));
            default -> {
            }
        }
    }

    /** What the picker shows: the player's own living guhs and, last, the gym's leenguh. */
    CompoundTag kiesStand(ServerPlayer p) {
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "kies");
        ListTag guhs = GuhKiezer.lijst(p, r -> true);
        data.putInt("Eigen", guhs.size());
        CompoundTag leen = new CompoundTag();
        leen.store("Id", UUIDUtil.CODEC, Guhmon.LEENGUH_ID);
        Tekst.put(leen, "Naam", Component.translatable("gui.guhs.guhmon.leenguh.naam"));
        leen.put("Looks", leenLooks());
        leen.putInt("Hartjes", 0);
        leen.putBoolean("Leenguh", true);
        guhs.add(leen);
        data.put("Guhs", guhs);
        return data;
    }

    /** The player picked a guh: one of their own (its band id) or the leenguh. Anything else is refused. */
    boolean kies(ServerPlayer p, String arg) {
        if (!speelt(p) || fase != Fase.KIEZEN) {
            return false;
        }
        if (Guhmon.LEENGUH.equals(arg) || Guhmon.LEENGUH_ID.toString().equals(arg)) {
            looks = leenLooks();
            naam = Component.translatable("gui.guhs.guhmon.leenguh.naam");
            leenguh = true;
        } else {
            UUID id;
            try {
                id = UUID.fromString(arg);
            } catch (IllegalArgumentException e) {
                return false;
            }
            BandData.Rec rec = null;
            for (BandData.Rec r : BandData.get(p.level().getServer()).guhsVan(p.getUUID())) {
                if (r.id.equals(id) && r.guh && !r.dood) {
                    rec = r;
                }
            }
            if (rec == null) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.guhmon.kies.niet_van_jou").withStyle(ChatFormatting.LIGHT_PURPLE));
                return false;
            }
            looks = rec.looks.copy();
            naam = rec.weergave();
            leenguh = false;
        }
        ServerLevel level = level();
        if (mijn != null) {
            mijn.discard();
        }
        mijn = Grap2Slice.standIn(level, Grap2Slice.GUHMON_GUH.get(), arena().wereld(Guhmon.MIJN), 180f, looks, naam);
        if (tegen != null) {
            Vec3 t = arena().wereld(Guhmon.TEGEN);
            tegen.snapTo(t.x, t.y, t.z, 0f, 0f);
            tegen.setYHeadRot(0f);
            tegen.setYBodyRot(0f);
            level.sendParticles(ParticleTypes.POOF, t.x, t.y + 0.4, t.z, 10, 0.3, 0.3, 0.3, 0.02);
        }
        Vec3 m = arena().wereld(Guhmon.MIJN);
        level.sendParticles(ParticleTypes.POOF, m.x, m.y + 0.4, m.z, 10, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, m.x, m.y, m.z, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.8f, 1.1f);
        nieuwGevecht(p, true);
        return true;
    }

    private void nieuwGevecht(ServerPlayer p, boolean eerste) {
        gevecht = new GuhmonGevecht();
        fase = Fase.GEVECHT;
        autoVerder = -1;
        if (mijn != null) {
            mijn.slaap(false);
        }
        if (tegen != null) {
            tegen.slaap(false);
        }
        Grappen.zetStap(p, Guhmon.ID, 3);
        ModNetworking.sendTo(p, new Grap2Payloads.GuhmonScherm(stand(true, List.of())));
    }

    // --- the battle --------------------------------------------------------------------------------------------------------

    /** The whole state for the battle screen; vers = a new battle (the screen plays its intro), regels = this round's text. */
    CompoundTag stand(boolean vers, List<GuhmonGevecht.Regel> regels) {
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "gevecht");
        data.putBoolean("Vers", vers);
        Tekst.put(data, "Naam", naam);
        data.put("Looks", looks.copy());
        Tekst.put(data, "TegenNaam", tegenNaam());
        data.put("TegenLooks", tegenLooks());
        GuhmonGevecht g = gevecht == null ? new GuhmonGevecht() : gevecht;
        data.putInt("SlaapSpeler", g.slaap(GuhmonGevecht.SPELER));
        data.putInt("SlaapTegen", g.slaap(GuhmonGevecht.TEGEN));
        data.putInt("MaagSpeler", g.maag(GuhmonGevecht.SPELER));
        data.putInt("MaagTegen", g.maag(GuhmonGevecht.TEGEN));
        data.putInt("Uitkomst", g.uitkomst().ordinal());
        data.putBoolean("DutjeMislukt", g.dutjeMislukt(GuhmonGevecht.SPELER));
        data.putInt("Ronde", g.ronde());
        data.putInt("Verloren", verlorenNu);
        ListTag lijst = new ListTag();
        for (GuhmonGevecht.Regel r : regels) {
            CompoundTag t = new CompoundTag();
            t.putString("Sleutel", r.sleutel());
            t.putInt("Wie", r.wie());
            t.putInt("A", r.slaapSpeler());
            t.putInt("B", r.slaapTegen());
            lijst.add(t);
        }
        data.put("Regels", lijst);
        return data;
    }

    /** One of the four moves (0..3). Returns this round's lines (empty: refused). */
    List<GuhmonGevecht.Regel> zet(ServerPlayer p, int zet) {
        if (!speelt(p) || fase != Fase.GEVECHT || gevecht == null || gevecht.uitkomst() != GuhmonGevecht.Uitkomst.BEZIG || zet < 0 || zet > 3) {
            return List.of();
        }
        return ronde(p, gevecht.speel(GuhmonGevecht.Zet.op(zet), random));
    }

    /** (Tests) one round in which the other guh's move is given too. */
    List<GuhmonGevecht.Regel> zet(ServerPlayer p, GuhmonGevecht.Zet zet, GuhmonGevecht.Zet tegenZet) {
        if (!speelt(p) || fase != Fase.GEVECHT || gevecht == null || gevecht.uitkomst() != GuhmonGevecht.Uitkomst.BEZIG) {
            return List.of();
        }
        return ronde(p, gevecht.speel(zet, tegenZet));
    }

    /** What a played round does in the gym (particles, sounds, who sleeps) and on the screen. */
    private List<GuhmonGevecht.Regel> ronde(ServerPlayer p, List<GuhmonGevecht.Regel> gespeeld) {
        List<GuhmonGevecht.Regel> regels = new ArrayList<>(gespeeld);
        ServerLevel level = level();
        for (GuhmonGevecht.Regel r : regels) {
            StandInGuh wie = r.wie() == GuhmonGevecht.SPELER ? mijn : tegen;
            if (wie == null) {
                continue;
            }
            switch (r.sleutel()) {
                case "vadsen", "dutje", "buikje" -> level.sendParticles(EmotesFeature.GUH_ZZZ.get(), wie.getX(), wie.getY() + 1.0, wie.getZ(), 3, 0.25, 0.1, 0.25, 0.0);
                case "njeg" -> wie.playSound(ModSounds.GUH_AMBIENT.get(), 0.9f, 1.5f);
                case "knabbel", "knabbel_vol" -> wie.playSound(ModSounds.GUH_EAT.get(), 0.9f, 1.0f);
                case "dutje_mislukt" -> level.sendParticles(ParticleTypes.SMOKE, wie.getX(), wie.getY() + 0.8, wie.getZ(), 6, 0.2, 0.1, 0.2, 0.01);
                default -> {
                }
            }
        }
        switch (gevecht.uitkomst()) {
            case GEWONNEN -> {
                if (mijn != null) {
                    mijn.slaap(true);
                }
                if (tegen != null) {
                    tegen.triggerAnim("action", "happy");
                }
                autoVerder = AUTO_VERDER;
            }
            case VERLOREN -> {
                if (tegen != null) {
                    tegen.slaap(true);
                }
                verlorenNu++;
                Guhmon.tel(p, false);
            }
            default -> {
            }
        }
        ModNetworking.sendTo(p, new Grap2Payloads.GuhmonScherm(stand(false, regels)));
        return regels;
    }

    /** After a lost battle: the same guhs, a fresh battle. Losing never costs anything. */
    boolean opnieuw(ServerPlayer p) {
        if (!speelt(p) || fase != Fase.GEVECHT || gevecht == null || gevecht.uitkomst() != GuhmonGevecht.Uitkomst.VERLOREN) {
            return false;
        }
        nieuwGevecht(p, false);
        return true;
    }

    /** After a won battle: the speech of the gym leader, fireworks, and a few seconds later back to the lobby with the badge. */
    boolean verder(ServerPlayer p) {
        if (!speelt(p) || fase != Fase.GEVECHT || gevecht == null || gevecht.uitkomst() != GuhmonGevecht.Uitkomst.GEWONNEN) {
            return false;
        }
        fase = Fase.GEWONNEN;
        beloon = true;
        autoVerder = -1;
        wegOver = UITLOOP;
        Grappen.zetStap(p, Guhmon.ID, 4);
        if (leider != null) {
            GuhQuests.say(p, leider, "quest.guhs.guhmon.gym.gewonnen", naam);
        }
        Grap2Slice.vuurwerk(level(), arena().wereld(Guhmon.TEGEN.add(0, 0, 6)));
        return true;
    }

    @Override
    protected void spelerWeg(ServerPlayer p, Vertrek reden) {
        if (!beloon || gevecht == null) {
            return;
        }
        beloon = false;
        Guhmon.tel(p, true);
        Grappen.voltooi(p, Guhmon.ID);
        Guhmon.verdien(p, Guhmon.Badge.DUTJES);
        if (gevecht.njegBlokte()) {
            Guhmon.verdien(p, Guhmon.Badge.NJEG);
        }
        if (gevecht.volBuikje()) {
            Guhmon.verdien(p, Guhmon.Badge.KNABBEL);
        }
    }
}
