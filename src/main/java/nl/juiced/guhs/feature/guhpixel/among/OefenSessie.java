package nl.juiced.guhs.feature.guhpixel.among;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.SessieStart;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.guhpixel.Vertrek;
import nl.juiced.guhs.feature.guhpixel.among.model.Kleur;
import nl.juiced.guhs.feature.guhpixel.among.model.Schip;
import nl.juiced.guhs.feature.guhpixel.among.model.Uitspraak;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.taal.Tekst;

/**
 * The oefenrondje: the one-time parody round of Among Guhs (joke game "among"; it unlocks the real queue). A scripted
 * round of about four minutes on the real ship, alone with eight guhs, in four steps:
 * <ol>
 *   <li>do your task (a real task panel: the kruimelbak in the Kantine) while the crew walks off to its own tasks;</li>
 *   <li>go and look: every guh fell asleep at its panel; report one (or press the button);</li>
 *   <li>the meeting: everybody accuses Rood ("Rood is sus, njeg"), the vote is about who ate the last knabbel, nobody is
 *   voted out;</li>
 *   <li>everybody dozes off at the table; something snores in a vent in Elektra: the Mika (Bruin, who was missing from the
 *   table all along) fell asleep in there. The crew wins because nobody did anything.</li>
 * </ol>
 * Nothing can go wrong and nothing is lost; there is no clock. The first time: 100 muntjes, the SUS-stickerbord and the
 * film (Grappen.voltooi, when the player is back in the lobby with the own inventory); the Kapitein-guh then asks whether
 * you want to try it for real.
 * Options: {@code Tempo} (tests: every wait is divided by it).
 */
public final class OefenSessie extends Sessie {
    public static final String GRAP = "among", ID = "among_oefenrondje";
    public static final int STAPPEN = 4;
    public static final SpelSoort SPEL = new SpelSoort(ID, AmongSlice.ARENA, 1, 1, LobbyPlek.SPEL_AMONG, OefenSessie::new);
    /** The crew at the table (participant 1..7), and the one nobody misses (8). */
    static final Kleur[] CREW = {Kleur.ROOD, Kleur.BLAUW, Kleur.GROEN, Kleur.GEEL, Kleur.ROZE, Kleur.ORANJE, Kleur.PAARS};
    static final Kleur MIKA = Kleur.BRUIN, SPELER = Kleur.WIT;
    static final int ROOD = 1, BRUIN = 8;
    private static final String[] PANELEN = {"slaap_droom", "nav_worst", "reactor_schakel", "zieken_sorteer", "machine_tank", "elektra_worst", "schild_droom"};
    private static final String TAAK_PANEEL = "kantine_kruimel", LUIK_KAMER = "elektra";
    private static final double SNELHEID = 0.17;
    /** Who says the scripted lines 1..8 of the meeting (0: the guh that was found). */
    private static final int[] SPREKERS = {0, 4, ROOD, 3, 7, ROOD, 5, 2};

    public enum Fase { TAAK, ZOEKEN, VERGADERING, LUIK, GEVONDEN }

    /** One guh of the crew: where it is, where it walks to, whether it sleeps. */
    private static final class Guh {
        final int idx;
        final Kleur kleur;
        AmongGuhEntity entiteit;
        double x, z;
        float yaw;
        List<Integer> pad;
        int padI, vertrek, werk = -1;
        boolean slaapt, inLuik;
        int kamer = -1;

        Guh(int idx, Kleur kleur) {
            this.idx = idx;
            this.kleur = kleur;
        }
    }

    private final Schip schip;
    private final int tempo;
    private final Random rng = new Random(42);
    private final List<Guh> guhs = new ArrayList<>();
    private Fase fase = Fase.TAAK;
    private int faseTicks;
    private final Schip.Paneel taakPaneel;
    private final Schip.Luik luik;
    private CompoundTag opgave;
    private boolean taakKlaar;
    // the meeting
    private final List<int[]> gezegd = new ArrayList<>();          // {speaker, index in teksten}
    private final List<Component> teksten = new ArrayList<>();
    private final int[] stemmen = new int[9];
    private int vergaderStap, gevonden = ROOD, eigenRegels, antwoordOver = -1;
    private boolean metKnop, gewonnen, uitbetaald;

    public OefenSessie(SessieStart start) {
        super(start);
        this.schip = Schip.standaard();
        this.tempo = Math.max(1, start.opties().getIntOr("Tempo", 1));
        this.taakPaneel = schip.panelen.get(schip.paneelVan(TAAK_PANEEL));
        this.luik = schip.luikIn(schip.zoneVan(LUIK_KAMER));
        java.util.Arrays.fill(stemmen, -2);
    }

    private int wacht(int ticks) {
        return Math.max(1, ticks / tempo);
    }

    public Fase fase() {
        return fase;
    }

    private ServerPlayer speler() {
        List<ServerPlayer> s = spelers();
        return s.isEmpty() ? null : s.get(0);
    }

    private Vec3 wereld(double x, double z, double dy) {
        return arena().wereld(new Vec3(x, schip.voet + dy, z));
    }

    private MutableComponent naam(int idx) {
        ServerPlayer p = speler();
        Kleur k = idx == 0 ? SPELER : idx == BRUIN ? MIKA : CREW[idx - 1];
        MutableComponent c = idx == 0 && p != null ? Component.empty().append(p.getName()) : Component.translatable("gui.guhs.among.kleur." + k.id);
        return c.withStyle(s -> s.withColor(k.rgb));
    }

    private void kapitein(ServerPlayer p, String sleutel, Object... args) {
        p.sendSystemMessage(Component.translatable("gui.guhs.among.oefen.kapitein", Component.translatable("gui.guhs.among.oefen." + sleutel, args))
                .withStyle(ChatFormatting.AQUA));
    }

    private void nee(ServerPlayer p, String sleutel) {
        p.sendOverlayMessage(Component.translatable("gui.guhs.among." + sleutel).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void zetFase(Fase nieuw) {
        fase = nieuw;
        faseTicks = 0;
    }

    // --- life cycle -------------------------------------------------------------------------------------------------------

    @Override
    protected void uitrusting(ServerPlayer p) {
        AmongSessie.geefPak(p, SPELER);
        p.getInventory().setItem(8, new ItemStack(AmongSlice.STEMBRIEFJE.get()));
    }

    @Override
    protected void begin() {
        ServerPlayer p = speler();
        for (int i = 1; i <= 8; i++) {
            Guh g = new Guh(i, i == BRUIN ? MIKA : CREW[i - 1]);
            Schip.Plek stoel = schip.stoelen.get(i);
            g.x = stoel.x();
            g.z = stoel.z();
            g.yaw = stoel.yaw();
            g.vertrek = wacht(50 + i * 14);
            int doel = i == BRUIN ? luik.knoop() : schip.panelen.get(schip.paneelVan(PANELEN[i - 1])).knoop();
            g.kamer = i == BRUIN ? luik.kamer() : schip.panelen.get(schip.paneelVan(PANELEN[i - 1])).kamer();
            int van = schip.dichtsteKnoop(g.x, g.z);
            List<Integer> pad = schip.pad(van, doel, Set.of());
            if (pad != null) {
                g.pad = new ArrayList<>(pad.size() + 1);
                g.pad.add(van);
                g.pad.addAll(pad);
            }
            g.entiteit = maakGuh(g, false);
            guhs.add(g);
        }
        if (p == null) {
            return;
        }
        Schip.Plek stoel = schip.stoelen.get(0);
        Vec3 pos = wereld(stoel.x(), stoel.z(), 0);
        p.teleportTo(level(), pos.x, pos.y, pos.z, Set.of(), stoel.yaw(), 0f, true);
        PxGeluid.titel(p, Component.translatable("gui.guhs.among.oefen.titel").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.among.oefen.titel.onder").withStyle(ChatFormatting.LIGHT_PURPLE), 70);
        PxGeluid.speel(p, AmongSlice.GELUID_BEGIN.get(), SoundSource.NEUTRAL, 0.9f, 1f);
        kapitein(p, "begin");
        Grappen.zetStap(p, GRAP, 1);
        stuurHud(p);
    }

    private AmongGuhEntity maakGuh(Guh g, boolean slaapt) {
        AmongGuhEntity e = AmongSlice.AMONG_GUH.get().create(level(), EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        e.deelnemer = g.idx;
        e.setKleur(g.kleur);
        e.setSlaapt(slaapt);
        e.setCustomName(naam(g.idx));
        Vec3 pos = wereld(g.x, g.z, 0);
        e.snapTo(pos.x, pos.y, pos.z, g.yaw, 0f);
        e.setYHeadRot(g.yaw);
        e.setYBodyRot(g.yaw);
        level().addFreshEntity(e);
        return e;
    }

    @Override
    protected void tick() {
        ServerPlayer p = speler();
        if (p == null) {
            return;
        }
        faseTicks++;
        if (!arena().doos().inflate(3).contains(p.position())) {
            Vec3 start = arena().start();
            p.teleportTo(level(), start.x, start.y, start.z, Set.of(), p.getYRot(), p.getXRot(), true);
        }
        switch (fase) {
            case TAAK, ZOEKEN -> {
                loop();
                if (fase == Fase.ZOEKEN && faseTicks % wacht(240) == 0) {
                    nee(p, "oefen.hint.zoeken");
                }
            }
            case VERGADERING -> vergaderTick(p);
            case LUIK -> {
                Vec3 bij = arena().wereld(new Vec3(luik.x() + 0.5, luik.y() + 0.3, luik.z() + 0.5));
                if (faseTicks % 40 == 1) {
                    level().sendParticles(ParticleTypes.CLOUD, bij.x, bij.y, bij.z, 3, 0.15, 0.1, 0.15, 0.01);
                    level().playSound(null, bij.x, bij.y, bij.z, AmongSlice.GELUID_SNURK.get(), SoundSource.NEUTRAL, 1.6f, 0.8f);
                }
                if (faseTicks % wacht(200) == 0) {
                    nee(p, "oefen.hint.luik");
                }
            }
            case GEVONDEN -> {
                if (faseTicks == wacht(70)) {
                    PxGeluid.titel(p, Component.translatable("gui.guhs.among.einde.crew_wint").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.among.oefen.crew_wint.onder"), 70);
                    PxGeluid.speel(p, AmongSlice.GELUID_TAAK.get(), SoundSource.PLAYERS, 1f, 1.3f);
                    kapitein(p, "einde");
                } else if (faseTicks >= wacht(190)) {
                    klaar(p);
                }
            }
            default -> {
            }
        }
    }

    /** The crew walks off to its panels (the Mika to its vent), works for a moment and dozes off. */
    private void loop() {
        for (Guh g : guhs) {
            if (g.slaapt || g.inLuik || g.entiteit == null) {
                continue;
            }
            if (faseTicks < g.vertrek) {
                continue;
            }
            if (g.pad != null && g.padI < g.pad.size()) {
                Schip.Knoop k = schip.knopen.get(g.pad.get(g.padI));
                double dx = k.x() - g.x, dz = k.z() - g.z, afstand = Math.hypot(dx, dz);
                if (afstand <= SNELHEID) {
                    g.x = k.x();
                    g.z = k.z();
                    g.padI++;
                } else {
                    g.x += dx / afstand * SNELHEID;
                    g.z += dz / afstand * SNELHEID;
                    g.yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                }
            } else if (g.idx == BRUIN) {
                // into the vent: gone, with a clatter
                g.inLuik = true;
                Vec3 bij = arena().wereld(new Vec3(luik.x() + 0.5, luik.y(), luik.z() + 0.5));
                level().playSound(null, bij.x, bij.y, bij.z, AmongSlice.GELUID_LUIK.get(), SoundSource.NEUTRAL, 0.6f, 1f);
                level().sendParticles(ParticleTypes.SMOKE, bij.x, bij.y + 0.2, bij.z, 6, 0.2, 0.1, 0.2, 0.01);
                g.entiteit.discard();
                g.entiteit = null;
                continue;
            } else if (g.werk < 0) {
                g.werk = wacht(60 + rng.nextInt(60));
                g.entiteit.setWerkt(true);
            } else if (--g.werk <= 0) {
                inSlaap(g);
                continue;
            }
            Vec3 pos = wereld(g.x, g.z, 0);
            g.entiteit.setPos(pos.x, pos.y, pos.z);
            g.entiteit.setYRot(g.yaw);
            g.entiteit.setYBodyRot(g.yaw);
            g.entiteit.setYHeadRot(g.yaw);
        }
    }

    private void inSlaap(Guh g) {
        g.slaapt = true;
        if (g.entiteit != null) {
            g.entiteit.setWerkt(false);
            g.entiteit.setSlaapt(true);
            Vec3 pos = wereld(g.x, g.z, 0);
            level().sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.6, pos.z, 4, 0.2, 0.1, 0.2, 0.01);
        }
    }

    /**
     * The reward comes here, when the player has the own inventory back (a keepsake given while the game items are still in
     * the pockets would be thrown away with them): for every way of leaving after the Mika was found.
     */
    @Override
    protected void spelerWeg(ServerPlayer p, Vertrek reden) {
        if (gewonnen && !uitbetaald) {
            uitbetaald = true;
            Grappen.voltooi(p, GRAP);
        }
        ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.HUD, new CompoundTag()));
        CompoundTag dicht = new CompoundTag();
        dicht.putBoolean("Dicht", true);
        ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.VERGADERING, dicht));
    }

    // --- what the player does ---------------------------------------------------------------------------------------------------

    /** A right-click on a block of the ship: the task panel, the button, a vent. True when it was one of those. */
    public boolean gebruikBlok(ServerPlayer p, BlockPos pos) {
        BlockPos l = arena().lokaal(pos);
        if (l.getX() == schip.knopX && l.getY() == schip.knopY && l.getZ() == schip.knopZ) {
            if (fase == Fase.ZOEKEN) {
                metKnop = true;
                beginVergadering(p, ROOD);
            } else {
                nee(p, fase == Fase.TAAK ? "oefen.nee.eerst_taak" : "nee.niet_nu");
            }
            return true;
        }
        for (Schip.Paneel paneel : schip.panelen) {
            if (l.getX() == paneel.bx() && l.getY() == paneel.by() && l.getZ() == paneel.bz()) {
                if (paneel != taakPaneel || taakKlaar) {
                    nee(p, "nee.geen_taak");
                } else if (p.distanceToSqr(Vec3.atCenterOf(pos)) > 36.0) {
                    nee(p, "nee.te_ver");
                } else {
                    opgave = TaakSoorten.van(Taken.KRUIMELBAK).opgave(rng, 0);
                    CompoundTag data = new CompoundTag();
                    data.putInt("Paneel", paneel.idx());
                    Tekst.put(data, "Kamer", AmongSessie.kamer(schip.zone(paneel.kamer())));
                    data.putString("Soort", Taken.KRUIMELBAK);
                    data.putInt("Stap", 1);
                    data.putInt("Stappen", 1);
                    data.put("Opgave", opgave.copy());
                    ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.TAAK, data));
                }
                return true;
            }
        }
        for (Schip.Luik lk : schip.luiken) {
            if (l.getX() == lk.x() && l.getY() == lk.y() && l.getZ() == lk.z()) {
                if (fase != Fase.LUIK) {
                    nee(p, "nee.luik");
                } else if (lk != luik) {
                    nee(p, "oefen.nee.stil_luik");
                } else {
                    vindMika(p);
                }
                return true;
            }
        }
        return false;
    }

    /** What the task panel asks right now (tests; an empty tag: the panel is not open). */
    public CompoundTag opgave() {
        return opgave == null ? new CompoundTag() : opgave;
    }

    public boolean taakKlaar(ServerPlayer p, CompoundTag resultaat) {
        if (fase != Fase.TAAK || opgave == null || !TaakSoorten.van(Taken.KRUIMELBAK).geldig(opgave, resultaat)) {
            return false;
        }
        opgave = null;
        taakKlaar = true;
        zetFase(Fase.ZOEKEN);
        PxGeluid.speel(p, AmongSlice.GELUID_TAAK.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
        kapitein(p, "taak_klaar");
        Grappen.zetStap(p, GRAP, 2);
        stuurHud(p);
        return true;
    }

    public void taakStop(ServerPlayer p) {
        opgave = null;
    }

    /** A click on a guh: a sleeper is reported (that starts the meeting). */
    public void klikGuh(ServerPlayer p, AmongGuhEntity guh) {
        if (fase == Fase.ZOEKEN) {
            if (guh.slaapt()) {
                beginVergadering(p, guh.deelnemer);
            } else {
                nee(p, "oefen.nee.nog_wakker");
            }
        } else if (fase == Fase.TAAK) {
            nee(p, "oefen.nee.eerst_taak");
        }
    }

    // --- the meeting ----------------------------------------------------------------------------------------------------------------

    private void beginVergadering(ServerPlayer p, int vinder) {
        gevonden = vinder >= 1 && vinder <= 7 ? vinder : ROOD;
        zetFase(Fase.VERGADERING);
        vergaderStap = 0;
        Guh slaper = guhs.get(gevonden - 1);
        Component kamer = slaper.kamer >= 0 ? AmongSessie.kamer(schip.zone(slaper.kamer)) : Component.empty();
        for (Guh g : guhs) {
            if (g.idx == BRUIN) {
                if (g.entiteit != null) {       // (still on its way: it is in the vent now)
                    g.entiteit.discard();
                    g.entiteit = null;
                }
                g.inLuik = true;
                continue;
            }
            Schip.Plek stoel = schip.stoelen.get(g.idx);
            g.x = stoel.x();
            g.z = stoel.z();
            g.yaw = stoel.yaw();
            g.slaapt = false;
            g.pad = null;
            if (g.entiteit != null) {
                Vec3 pos = wereld(g.x, g.z, 0);
                g.entiteit.snapTo(pos.x, pos.y, pos.z, g.yaw, 0f);
                g.entiteit.setYBodyRot(g.yaw);
                g.entiteit.setYHeadRot(g.yaw);
                g.entiteit.setSlaapt(false);
                g.entiteit.setWerkt(false);
            }
        }
        Schip.Plek stoel = schip.stoelen.get(0);
        Vec3 pos = wereld(stoel.x(), stoel.z(), 0);
        p.teleportTo(level(), pos.x, pos.y, pos.z, Set.of(), stoel.yaw(), 0f, true);
        PxGeluid.speel(p, AmongSlice.GELUID_VERGADERING.get(), SoundSource.NEUTRAL, 0.9f, metKnop ? 1.2f : 1f);
        p.sendSystemMessage(Component.translatable(metKnop ? "gui.guhs.among.knop" : "gui.guhs.among.gemeld", naam(0), naam(gevonden)).withStyle(ChatFormatting.YELLOW));
        zegRegel(p, 0, metKnop ? Component.translatable("gui.guhs.among.uitspraak.knop.0")
                : Component.translatable("gui.guhs.among.uitspraak.gevonden.0", naam(gevonden), kamer));
        Grappen.zetStap(p, GRAP, 3);
        stuurVergadering(p, true);
    }

    private void zegRegel(ServerPlayer p, int spreker, Component tekst) {
        gezegd.add(new int[]{spreker, teksten.size()});
        teksten.add(tekst);
        p.sendSystemMessage(Component.translatable("gui.guhs.among.zegt", naam(spreker), tekst));
    }

    private static final int REGELS = 8, REGEL_TIJD = 44, STEM_TIJD = 600;

    private int bespreekTijd() {
        return wacht(REGEL_TIJD) * (REGELS + 1);
    }

    private void vergaderTick(ServerPlayer p) {
        int regel = wacht(REGEL_TIJD);
        if (vergaderStap == 0) {
            if (faseTicks % regel == 0 && faseTicks / regel <= REGELS) {
                int n = faseTicks / regel;
                int spreker = SPREKERS[n - 1] == 0 ? gevonden : SPREKERS[n - 1];
                zegRegel(p, spreker, Component.translatable("gui.guhs.among.oefen.zeg." + n));
                stuurVergadering(p, false);
            }
            if (antwoordOver >= 0 && --antwoordOver < 0) {
                zegRegel(p, 2 + rng.nextInt(6), Component.translatable("gui.guhs.among.uitspraak.sus." + rng.nextInt(3), naam(ROOD), Component.empty()));
                stuurVergadering(p, false);
            }
            if (faseTicks >= bespreekTijd()) {
                vergaderStap = 1;
                faseTicks = 0;
                p.sendSystemMessage(Component.translatable("gui.guhs.among.oefen.stemmen").withStyle(ChatFormatting.YELLOW));
                stuurVergadering(p, false);
            }
        } else if (vergaderStap == 1) {
            // the crew votes one by one, all for Rood (Rood too, by accident)
            int beurt = wacht(24);
            if (faseTicks % beurt == 0 && faseTicks / beurt >= 1 && faseTicks / beurt <= 7) {
                stemmen[faseTicks / beurt] = ROOD;
                PxGeluid.speel(p, AmongSlice.GELUID_STEM.get(), SoundSource.NEUTRAL, 0.9f, 1f);
                stuurVergadering(p, false);
            }
            if ((stemmen[0] != -2 && faseTicks > beurt * 8) || faseTicks >= wacht(STEM_TIJD)) {
                if (stemmen[0] == -2) {
                    stemmen[0] = -1;
                }
                vergaderStap = 2;
                faseTicks = 0;
                Component uitslag = uitslag();
                p.sendSystemMessage(uitslag.copy().withStyle(ChatFormatting.GOLD));
                PxGeluid.titel(p, Component.translatable("gui.guhs.among.uitslag").withStyle(ChatFormatting.GOLD),
                        Component.translatable("gui.guhs.among.oefen.uitslag.kort"), 80);
                PxGeluid.speel(p, AmongSlice.GELUID_WEGGESTEMD.get(), SoundSource.NEUTRAL, 0.9f, 1.3f);
                stuurVergadering(p, false);
            }
        } else if (faseTicks >= wacht(150)) {
            naVergadering(p);
        }
    }

    private Component uitslag() {
        String hoe = stemmen[0] == ROOD ? "rood" : stemmen[0] == BRUIN ? "bruin" : stemmen[0] < 0 ? "niemand" : "anders";
        return Component.translatable("gui.guhs.among.oefen.uitslag." + hoe, naam(ROOD), naam(BRUIN));
    }

    /** A vote of the player (a participant, or -1: skip). It changes nothing, except what the verdict says about you. */
    public boolean stem(ServerPlayer p, int doel) {
        if (fase != Fase.VERGADERING || vergaderStap != 1 || stemmen[0] != -2 || doel > BRUIN || doel == 0) {
            return false;
        }
        stemmen[0] = doel < 0 ? -1 : doel;
        PxGeluid.speel(p, AmongSlice.GELUID_STEM.get(), SoundSource.NEUTRAL, 0.9f, 1f);
        stuurVergadering(p, false);
        return true;
    }

    /** A ready-made statement of the player. Whatever it is, somebody answers that Rood is sus. */
    public boolean zeg(ServerPlayer p, int soort, int over, int zone) {
        if (fase != Fase.VERGADERING || vergaderStap != 0 || eigenRegels >= 4 || soort < 0 || soort >= Uitspraak.Soort.values().length) {
            return false;
        }
        Uitspraak.Soort s = Uitspraak.Soort.values()[soort];
        Component wie = over >= 1 && over <= BRUIN ? naam(over) : naam(ROOD);
        Component waar = zone >= 0 && zone < schip.zones.size() ? AmongSessie.kamer(schip.zone(zone)) : AmongSessie.kamer(schip.zone(schip.zoneVan("kantine")));
        eigenRegels++;
        zegRegel(p, 0, Component.translatable("gui.guhs.among.uitspraak." + s.id() + ".0", wie, waar));
        antwoordOver = wacht(16);
        stuurVergadering(p, false);
        return true;
    }

    /** The Stembriefje: the meeting screen again. */
    public void briefje(ServerPlayer p) {
        if (fase == Fase.VERGADERING) {
            stuurVergadering(p, true);
        } else {
            nee(p, "nee.geen_vergadering");
        }
    }

    private void naVergadering(ServerPlayer p) {
        CompoundTag dicht = new CompoundTag();
        dicht.putBoolean("Dicht", true);
        ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.VERGADERING, dicht));
        zetFase(Fase.LUIK);
        for (Guh g : guhs) {
            if (g.idx != BRUIN) {
                inSlaap(g);
            }
        }
        kapitein(p, "na_vergadering", AmongSessie.kamer(schip.zone(luik.kamer())));
        Grappen.zetStap(p, GRAP, 4);
        stuurHud(p);
    }

    private void vindMika(ServerPlayer p) {
        zetFase(Fase.GEVONDEN);
        gewonnen = true;
        Guh bruin = guhs.get(BRUIN - 1);
        bruin.x = luik.x() + 0.5;
        bruin.z = luik.z() + 0.5;
        bruin.slaapt = true;
        bruin.entiteit = maakGuh(bruin, true);
        if (bruin.entiteit != null) {
            // half out of the hatch
            Vec3 pos = arena().wereld(new Vec3(luik.x() + 0.5, luik.y() - 0.3, luik.z() + 0.5));
            bruin.entiteit.snapTo(pos.x, pos.y, pos.z, 0f, 0f);
        }
        Vec3 bij = arena().wereld(new Vec3(luik.x() + 0.5, luik.y() + 0.3, luik.z() + 0.5));
        level().playSound(null, bij.x, bij.y, bij.z, AmongSlice.GELUID_LUIK.get(), SoundSource.NEUTRAL, 0.8f, 1f);
        level().sendParticles(ParticleTypes.POOF, bij.x, bij.y, bij.z, 8, 0.3, 0.2, 0.3, 0.01);
        PxGeluid.titel(p, Component.translatable("gui.guhs.among.oefen.gevonden").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.among.oefen.gevonden.onder", naam(BRUIN)), 60);
        p.sendSystemMessage(Component.translatable("gui.guhs.among.oefen.gevonden.chat", naam(BRUIN)).withStyle(ChatFormatting.GOLD));
        stuurHud(p);
    }

    // --- what the client gets ---------------------------------------------------------------------------------------------------

    private void stuurHud(ServerPlayer p) {
        ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.HUD, hudTag()));
    }

    /** The same HUD as the real game: crew, the one task, the crew's bar (which nobody else fills). */
    public CompoundTag hudTag() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Mika", false);
        t.putBoolean("Wakker", true);
        t.putInt("Kleur", SPELER.rgb);
        t.putInt("Klaar", taakKlaar ? 1 : 0);
        t.putInt("Totaal", 9);
        t.putBoolean("Knop", true);
        ListTag taken = new ListTag();
        CompoundTag k = new CompoundTag();
        k.putString("Soort", Taken.KRUIMELBAK);
        k.putInt("Stap", taakKlaar ? 1 : 0);
        k.putInt("Stappen", 1);
        Tekst.put(k, "Kamer", AmongSessie.kamer(schip.zone(taakPaneel.kamer())));
        taken.add(k);
        t.put("Taken", taken);
        return t;
    }

    private void stuurVergadering(ServerPlayer p, boolean open) {
        ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.VERGADERING, vergaderTag(open)));
    }

    /** The meeting in the shape the meeting screen of the real game reads (AmongSessie.vergaderTag). */
    public CompoundTag vergaderTag(boolean open) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Open", open);
        t.putInt("Ik", 0);
        t.putInt("Stap", vergaderStap);
        t.putInt("Over", vergaderStap == 0 ? Math.max(0, bespreekTijd() - faseTicks) : vergaderStap == 1 ? Math.max(0, wacht(STEM_TIJD) - faseTicks) : 0);
        t.putBoolean("Wakker", true);
        t.putInt("MijnStem", stemmen[0]);
        t.putInt("Zeggen", Math.max(0, 4 - eigenRegels));
        Tekst.put(t, "Onderwerp", Component.translatable("gui.guhs.among.oefen.onderwerp"));
        int[] telling = new int[9];
        int overgeslagen = 0;
        for (int stem : stemmen) {
            if (stem >= 0) {
                telling[stem]++;
            } else if (stem == -1) {
                overgeslagen++;
            }
        }
        ListTag deelnemers = new ListTag();
        for (int i = 0; i <= BRUIN; i++) {
            CompoundTag k = new CompoundTag();
            k.putInt("Idx", i);
            k.putInt("Kleur", (i == 0 ? SPELER : i == BRUIN ? MIKA : CREW[i - 1]).rgb);
            Tekst.put(k, "Naam", naam(i));
            k.putBoolean("Wakker", true);
            k.putBoolean("Weg", false);
            k.putBoolean("Gestemd", stemmen[i] != -2);
            if (vergaderStap == 2) {
                k.putInt("Stemmen", telling[i]);
                k.putInt("Stem", stemmen[i]);
            }
            deelnemers.add(k);
        }
        t.put("Deelnemers", deelnemers);
        ListTag uitspraken = new ListTag();
        for (int[] g : gezegd) {
            CompoundTag k = new CompoundTag();
            k.putInt("Spreker", g[0]);
            Tekst.put(k, "Tekst", teksten.get(g[1]));
            uitspraken.add(k);
        }
        t.put("Uitspraken", uitspraken);
        ListTag kamers = new ListTag();
        for (Schip.Zone z : schip.kamers()) {
            CompoundTag k = new CompoundTag();
            k.putInt("Idx", z.idx());
            Tekst.put(k, "Naam", AmongSessie.kamer(z));
            kamers.add(k);
        }
        t.put("Kamers", kamers);
        if (vergaderStap == 2) {
            t.putInt("Weg", -1);
            t.putInt("Overgeslagen", overgeslagen);
            Tekst.put(t, "UitslagTekst", uitslag());
        }
        return t;
    }

    /** (Tests, dev) how many guhs of the crew sleep right now. */
    public int slapers() {
        int n = 0;
        for (Guh g : guhs) {
            n += g.slaapt && g.idx != BRUIN ? 1 : 0;
        }
        return n;
    }

    /** (Tests, dev) the guh entity of a participant (1..8; null when it is in the vent). */
    public AmongGuhEntity guh(int idx) {
        return idx >= 1 && idx <= guhs.size() ? guhs.get(idx - 1).entiteit : null;
    }

    public AABB doos() {
        return arena().doos();
    }

    /** (Tests, dev) skips the walking: every guh is at its panel, asleep; the Mika is in the vent. */
    public void iedereenSlaapt() {
        for (Guh g : guhs) {
            if (g.idx == BRUIN) {
                if (g.entiteit != null) {
                    g.entiteit.discard();
                    g.entiteit = null;
                }
                g.inLuik = true;
            } else if (!g.slaapt) {
                if (g.pad != null && !g.pad.isEmpty()) {
                    Schip.Knoop k = schip.knopen.get(g.pad.get(g.pad.size() - 1));
                    g.x = k.x();
                    g.z = k.z();
                    g.padI = g.pad.size();
                }
                if (g.entiteit != null) {
                    Vec3 pos = wereld(g.x, g.z, 0);
                    g.entiteit.setPos(pos.x, pos.y, pos.z);
                }
                inSlaap(g);
            }
        }
    }
}
