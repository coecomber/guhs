package nl.juiced.guhs.feature.einde2.zij;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature;
import nl.juiced.guhs.feature.guheinde.Enderguhs;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.mewtwo.MewtwoFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 zij (CONTRACT_E2_B 5): the five scenes of this slice (texts {@code scene.guhs.<id>.*}, tools/features/einde2_zij_tekst.py
 * {@code SCENES}). Every spoken line waits for a click (1.4.4); only the credit roll of the poem runs on
 * ({@link Cutscene.Builder#lied}). Running times (the waits not counted):
 * <ul>
 *   <li>{@link #OPKOMST} {@code einde2_zij_opper_mika_opkomst}: 18 s, the Opper-Mika rises out of the void on the hungry
 *       Enderguh when a fight begins (once per player, replayable);</li>
 *   <li>{@link #GEDICHT} {@code einde2_zij_einde_gedicht}: 76 s (big), the parody of the poem at the end and the credits, the
 *       first time a player leaves the Guheinde after beating the Opper-Mika;</li>
 *   <li>{@link #ROKU} {@code einde2_zij_roku_zonnestraal}: 22 s, the sun beam in Roku-guh's sanctum;</li>
 *   <li>{@link #ZEGEN} {@code einde2_zij_leeuwschildpad_zegen}: 17 s, the blessing on the lion turtle's palm;</li>
 *   <li>{@link #MEW} {@code einde2_zij_knipoog_mew}: 9.5 s, wink 10 (Mew-guh in the library).</li>
 * </ul>
 */
public final class ZijScenes {
    public static final String GEDICHT_BEZIG = "guhs_einde2_zij_gedicht";

    /** Roku-guh's sanctum: anchor = the statue's foot ({@code vuurtempel_roku/roku_beeld}). */
    public static final Cutscene ROKU = Cutscene.maak("einde2_zij_roku_zonnestraal").duur(440).bij(ZijLijnen.VUURTEMPEL).verbergEcht(6)
            .speler(new Vec3(0.5, 0, 7.5), 180f)
            .npc("roku", GuhNpcEntity.Kind.ROKU_GUH, new Vec3(0.5, -3.2, 4.5), 0f)
            .camera(0, new Vec3(0.5, 2.2, 10.5), new Vec3(0.5, 3.0, 0.5))
            .camera(70, new Vec3(0.5, 2.0, 8.9), new Vec3(0.5, 3.4, 0.5))
            .cameraKnip(80, new Vec3(8.5, 4.0, 3.5), new Vec3(2.0, 4.2, -0.5))
            .camera(170, new Vec3(6.5, 3.6, 4.5), new Vec3(0.5, 4.2, -0.5))
            .cameraKnip(180, new Vec3(0.5, 1.7, 10.0), new Vec3(0.5, 1.4, 4.5))
            .camera(260, new Vec3(1.2, 1.8, 7.6), new Vec3(0.5, 1.7, 4.5))
            .cameraKnipVolgt(300, new Vec3(-1.6, 2.3, 9.4), "roku")
            .camera(440, new Vec3(-1.0, 2.1, 8.6), new Vec3(0.5, 1.7, 4.5))
            .zeg(60, "", "zon", 60)
            .zeg(160, "", "ogen", 60)
            .zeg(260, "roku", "hallo", 60)
            .zeg(320, "roku", "wie", 60)
            .zeg(380, "roku", "praten", 60)
            .loop("roku", 190, 240, new Vec3(0.5, 0, 4.5))
            .animatie("roku", 190, "verschijn")
            .animatie("roku", 330, "raad")
            .geluid(85, () -> SoundEvents.BEACON_ACTIVATE, 0.8f, 1.4f)
            .geluid(150, () -> SoundEvents.BLAZE_AMBIENT, 0.5f, 0.6f)
            .geluid(190, () -> SoundEvents.BEACON_POWER_SELECT, 0.9f, 0.7f)
            .geluid(200, () -> SoundEvents.FIRECHARGE_USE, 0.6f, 0.6f)
            .deeltjes(150, ParticleTypes.FLAME, new Vec3(-0.5, 4.5, -0.5), 6, 0.08)
            .deeltjes(150, ParticleTypes.FLAME, new Vec3(1.5, 4.5, -0.5), 6, 0.08)
            .deeltjes(200, ParticleTypes.SOUL_FIRE_FLAME, new Vec3(0.5, 0.3, 4.5), 30, 0.5)
            .deeltjes(220, ParticleTypes.END_ROD, new Vec3(0.5, 1.2, 4.5), 30, 0.6)
            .deeltjes(240, ParticleTypes.FLAME, new Vec3(0.5, 1.5, 4.5), 20, 0.4)
            .registreer();

    /** The blessing: anchor = the middle pad of the palm ({@code leeuwschildpad_eiland/zegen_plek}). */
    public static final Cutscene ZEGEN;

    static {
        Cutscene.Builder b = Cutscene.maak("einde2_zij_leeuwschildpad_zegen").duur(340).bij(ZijLijnen.LEEUWSCHILDPAD)
                .speler(new Vec3(0.5, 0, 0.5), 180f)
                .camera(0, new Vec3(16.5, 10, 14.5), new Vec3(1.5, 8, -8.5))
                .camera(100, new Vec3(11.5, 7, 9.5), new Vec3(1.5, 6, -6.5))
                .cameraKnip(110, new Vec3(0.5, 2.2, 5.5), new Vec3(0.5, 1.2, 0.5))
                .camera(190, new Vec3(1.5, 2.0, 4.0), new Vec3(0.5, 1.2, 0.5))
                .cameraKnip(200, new Vec3(0.5, 1.6, 3.5), new Vec3(1.5, 8.5, -7.5))
                .camera(300, new Vec3(0.5, 1.4, 2.8), new Vec3(1.5, 9.0, -7.5))
                .cameraKnip(310, new Vec3(-6.5, 5.5, 6.5), new Vec3(0.5, 1.5, 0.5))
                .zeg(40, "schildpad", "gegeven", 60)
                .zeg(130, "schildpad", "zegen1", 60)
                .zeg(185, "schildpad", "zegen2", 60)
                .zeg(250, "schildpad", "zegen3", 60)
                .geluid(10, () -> SoundEvents.TURTLE_AMBIENT_LAND, 1f, 0.4f)
                .geluid(200, () -> SoundEvents.BEACON_ACTIVATE, 0.8f, 0.8f)
                .geluid(270, () -> SoundEvents.AMETHYST_CLUSTER_BREAK, 1f, 0.6f)
                .schud(265, 1f, 30);
        for (int t = 200; t <= 300; t += 10) {
            b.deeltjes(t, ParticleTypes.END_ROD, new Vec3(0.5, 1.0 + (t - 200) / 50.0, 0.5), 6, 0.5);
            b.deeltjes(t, ParticleTypes.GLOW, new Vec3(0.5, 0.6, 0.5), 4, 0.8);
        }
        b.deeltjes(270, ParticleTypes.TOTEM_OF_UNDYING, new Vec3(0.5, 1.0, 0.5), 60, 0.8);
        b.deeltjes(270, ParticleTypes.HEART, new Vec3(0.5, 2.2, 0.5), 8, 0.6);
        ZEGEN = b.registreer();
    }

    /** Wink 10: anchor = Wan Shi Guh's dais ({@code bibliotheek_wan_shi_guh/wan_shi_guh}). */
    public static final Cutscene MEW;

    static {
        Cutscene.Builder b = Cutscene.maak("einde2_zij_knipoog_mew").duur(190).verbergEcht(3)
                .acteur("uil", E2CastSchurkFeature.WAN_SHI_GUH, new Vec3(0.5, 0, 0.5), 0f)
                .acteur("mew", MewtwoFeature.MEW, new Vec3(-2.5, 3, -1.5), 90f)
                .camera(0, new Vec3(0.5, 2.5, 9.5), new Vec3(0.5, 1.6, 0.5))
                .camera(120, new Vec3(0.5, 2.4, 8.4), new Vec3(0.5, 1.8, 0.5))
                .camera(190, new Vec3(0.5, 2.3, 7.8), new Vec3(0.5, 1.8, 0.5))
                .zeg(10, "uil", "regels", 60)
                .loop("mew", 20, 118, new Vec3(3.5, 3, -1.5))
                .kijk("uil", 122, new Vec3(0.5, 2, -8))
                .loop("mew", 120, 130, new Vec3(3.5, 7.5, -1.5))
                .kijk("uil", 150, new Vec3(0.5, 2, 9))
                .zeg(160, "uil", "niks", 40)
                .geluid(40, () -> SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 0.8f, 1.6f)
                .geluid(100, () -> SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 0.8f, 1.8f)
                .geluid(124, () -> SoundEvents.BOOK_PUT, 0.6f, 0.8f);
        for (int t = 50; t <= 110; t += 20) {
            double x = -2.5 + 6.0 * (t - 20) / 98.0;
            b.deeltjes(t, ParticleTypes.POOF, new Vec3(x, 2.6, -1.5), 6, 0.25);
            b.deeltjes(t, ParticleTypes.ENCHANT, new Vec3(x, 2.2, -1.0), 12, 0.4);
            b.geluid(t, () -> SoundEvents.BOOK_PAGE_TURN, 1f, 1.3f);
        }
        MEW = b.registreer();
    }

    /** The Opper-Mika rises: anchor = the middle of the Knabbelberg's plateau ({@code GuheindeGevecht.center()}). */
    public static final Cutscene OPKOMST = Cutscene.maak("einde2_zij_opper_mika_opkomst").duur(360)
            .acteur("enderguh", GuheindeFeature.HONGERIGE_ENDERGUH, new Vec3(0.5, -40, -90.5), 0f)
            .acteur("opper", GuheindeFeature.OPPER_MIKA, new Vec3(0.5, -37.4, -90.5), 0f)
            .camera(0, new Vec3(0.5, 6, 18.5), new Vec3(0.5, 4, -70.5))
            .camera(130, new Vec3(0.5, 9, 12.5), new Vec3(0.5, 22, -72.5))
            .cameraKnipVolgt(150, new Vec3(9.5, 34, -50.5), "opper")
            .camera(220, new Vec3(4.5, 34, -58.5), new Vec3(0.5, 32.6, -70.5))
            .cameraKnipVolgt(260, new Vec3(-3.5, 33.5, -61.5), "opper")
            .cameraKnip(300, new Vec3(0.5, 12, 22.5), new Vec3(0.5, 28, -70.5))
            .camera(360, new Vec3(0.5, 14, 26.5), new Vec3(0.5, 30, -70.5))
            .loop("enderguh", 30, 150, new Vec3(0.5, 30, -70.5))
            .loop("opper", 30, 150, new Vec3(0.5, 32.6, -70.5))
            .kijk("opper", 160, new Vec3(0.5, 6, 18.5))
            .zeg(170, "opper", "wie", 60)
            .zeg(220, "opper", "alles", 60)
            .zeg(265, "opper", "duw", 60)
            .zeg(315, "", "titel", 45)
            .geluid(30, () -> SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 1.5f)
            .geluid(150, () -> SoundEvents.ENDER_DRAGON_FLAP, 1f, 0.8f)
            .geluid(300, () -> SoundEvents.ENDER_DRAGON_GROWL, 1f, 1.2f)
            .schud(150, 2f, 40)
            .deeltjes(150, ParticleTypes.REVERSE_PORTAL, new Vec3(0.5, 30, -70.5), 80, 2.5)
            .deeltjes(300, ParticleTypes.PORTAL, new Vec3(0.5, 32, -70.5), 40, 2.0)
            .registreer();

    /** The poem at the end, and the credits: anchor = where the player stands just after leaving the Guheinde. */
    public static final Cutscene GEDICHT;

    static {
        Cutscene.Builder b = Cutscene.maak("einde2_zij_einde_gedicht").duur(1520).cameraOntwijkt()
                .camera(0, new Vec3(0.5, 2.5, 0.5), new Vec3(0.5, 60, 8.5))
                .camera(1380, new Vec3(0.5, 6, 0.5), new Vec3(0.5, 70, 10.5))
                .camera(1520, new Vec3(0.5, 7, 0.5), new Vec3(0.5, 40, 20.5))
                .zwart(0, 1400);
        String[] stem = {"roze", "goud"};
        int t = 60;
        for (int i = 1; i <= 16; i++) {
            b.zeg(t, stem[(i + 1) % 2], "regel." + i, 60);
            t += 60;
        }
        for (int i = 1; i <= 6; i++) {
            b.lied(t, "credits." + i, 70);
            t += 70;
        }
        b.geluid(40, () -> SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f, 0.5f);
        b.geluid(t, () -> SoundEvents.PLAYER_LEVELUP, 0.5f, 0.6f);
        GEDICHT = b.registreer();
    }

    private ZijScenes() {
    }

    static void register() {
        // (the scenes register themselves when this class loads)
        Einde2Haken.GEVECHT_BEGINT.add(ZijScenes::gevechtBegint);
    }

    /** A fight with the Opper-Mika begins: every player on the Knabbelberg who never saw his entrance sees it now. */
    static void gevechtBegint(ServerLevel level) {
        BlockPos midden = GuheindeGevecht.of(level).center();
        for (ServerPlayer p : level.getPlayers(p -> !p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(midden)) < 160 * 160)) {
            if (!Cutscenes.gezien(p, OPKOMST.id()) && !Cutscenes.bezig(p)) {
                Cutscenes.speel(p, OPKOMST, midden, Rotation.NONE, null);
            }
        }
    }

    /** The first time a player leaves the Guheinde after beating the Opper-Mika: the poem, a moment after arriving. */
    static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getFrom() != GuheindeFeature.GUHEINDE || event.getTo() == GuheindeFeature.GUHEINDE
                || !Enderguhs.heeftVerslagen(p) || Cutscenes.gezien(p, GEDICHT.id())) {
            return;
        }
        var naar = event.getTo();
        ZijEvents.later(60, () -> {
            if (p.isRemoved() || p.level().dimension() != naar || Cutscenes.bezig(p) || Cutscenes.gezien(p, GEDICHT.id())) {
                return;
            }
            speelGedicht(p);
        });
    }

    static boolean speelGedicht(ServerPlayer p) {
        E2Audio.moment(p, "aftiteling");
        boolean ok = Cutscenes.speel(p, GEDICHT, p.blockPosition(), Rotation.NONE, speler -> E2Audio.stopMoment(speler, 60));
        if (!ok) {
            E2Audio.stopMoment(p, 20);
        }
        return ok;
    }
}
