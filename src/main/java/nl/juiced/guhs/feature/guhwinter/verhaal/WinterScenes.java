package nl.juiced.guhs.feature.guhwinter.verhaal;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.guhwinter.npc.NpcDefs;
import nl.juiced.guhs.feature.guhwinter.npc.NpcFase;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Guhwinter (verhaal slice): the camera scenes of the winter story, played with the verhaal engine's {@link Cutscene}
 * (client-side actors, so every player sees their own scene and the real plein guhs stay where they are; they are hidden
 * near the scene for the viewer). The actors are guhs dressed exactly like the plein's NPCs ({@link NpcDefs}), with the
 * event's look pieces (the Sint's beard and mantle, Anthony's camera) drawn by client.WinterVerhaalClient from an entity tag
 * {@code guhwinter_look_<look>}. Each line waits for a click (the engine's Klikklok); the rest runs by itself.
 * <p>
 * Positions are written in world coordinates of the Winterplein (plein's anchors) and stored relative to the scene's
 * anchor block, rotation NONE. Texts: tools/features/guhwinter_verhaal.py ({@code scene.guhs.<id>.*}).
 */
public final class WinterScenes {
    public static Cutscene INTOCHT, INTOCHT_TERUG, OZOVADS, SINT_BLIJFT, SINT_BLIJFT_TERUG, PAKJESAVOND, BOOMHALEN, KERSTDINER;

    /** The scene names of the dev command, with their scene. */
    static final java.util.Map<String, Cutscene> NAMEN = new java.util.LinkedHashMap<>();

    private WinterScenes() {
    }

    static void register() {
        INTOCHT = intocht(false);
        INTOCHT_TERUG = intocht(true);
        OZOVADS = ozovads();
        SINT_BLIJFT = sintBlijft(false);
        SINT_BLIJFT_TERUG = sintBlijft(true);
        PAKJESAVOND = pakjesavond();
        BOOMHALEN = boomhalen();
        KERSTDINER = kerstdiner();
        for (Cutscene s : new Cutscene[] {INTOCHT, INTOCHT_TERUG, OZOVADS, SINT_BLIJFT, SINT_BLIJFT_TERUG, PAKJESAVOND, BOOMHALEN, KERSTDINER}) {
            NAMEN.put(s.id().substring("guhwinter_".length()), s);
        }
    }

    /** Plays a scene at its own anchor; false: the player is busy (nothing happens, daarna is not called). */
    public static boolean speel(ServerPlayer p, Cutscene s, @Nullable java.util.function.Consumer<ServerPlayer> daarna) {
        BlockPos anker = ANKERS.getOrDefault(s.id(), p.blockPosition());
        return Cutscenes.speel(p, s, anker, Rotation.NONE, daarna);
    }

    private static final java.util.Map<String, BlockPos> ANKERS = new java.util.HashMap<>();

    // =====================================================================================================================
    // a scene in world coordinates
    // =====================================================================================================================

    /** A scene being written: world coordinates in, anchor-relative out; every time is shifted by {@code o} ticks. */
    private static final class Film {
        final Cutscene.Builder b;
        final BlockPos anker;
        final int o;

        Film(String id, String ankerNaam, BlockPos reserve, int o) {
            GuhwinterAnkers.Anker a = GuhwinterAnkers.zoek(ankerNaam);
            this.anker = a == null ? reserve : a.pos();
            this.b = Cutscene.maak(id).cameraOntwijkt();
            this.o = o;
            ANKERS.put(id, anker);
        }

        Vec3 r(double x, double y, double z) {
            return new Vec3(x - anker.getX(), y - anker.getY(), z - anker.getZ());
        }

        /** A guh of the plein as it looks in that phase (look pieces, size, hair), standing or sitting. */
        Film guh(String npc, double x, double y, double z, float yaw, NpcFase fase, boolean zit) {
            NpcDefs.Def d = NpcDefs.van(npc);
            b.acteur(npc, ModEntities.GUH, r(x, y, z), yaw, tag -> vul(tag, d, fase, zit));
            return this;
        }

        Film paard(double x, double y, double z, float yaw) {
            b.acteur("ozovads", () -> EntityType.HORSE, r(x, y, z), yaw, tag -> tag.putInt("Variant", 0));
            return this;
        }

        Film speler(double x, double y, double z, float yaw) {
            b.speler(r(x, y, z), yaw);
            return this;
        }

        Film camera(int t, double x, double y, double z, double kx, double ky, double kz) {
            b.camera(t + o, r(x, y, z), r(kx, ky, kz));
            return this;
        }

        Film knip(int t, double x, double y, double z, double kx, double ky, double kz) {
            b.cameraKnip(t + o, r(x, y, z), r(kx, ky, kz));
            return this;
        }

        Film volgt(int t, double x, double y, double z, String acteur) {
            b.cameraVolgt(t + o, r(x, y, z), acteur);
            return this;
        }

        Film knipVolgt(int t, double x, double y, double z, String acteur) {
            b.cameraKnipVolgt(t + o, r(x, y, z), acteur);
            return this;
        }

        Film loop(String acteur, int t0, int t1, double x, double y, double z) {
            b.loop(acteur, t0 + o, t1 + o, r(x, y, z));
            return this;
        }

        Film kijk(String acteur, int t, double x, double y, double z) {
            b.kijk(acteur, t + o, r(x, y, z));
            return this;
        }

        /** Several actors look at one spot. */
        Film kijkAllen(int t, double x, double y, double z, String... acteurs) {
            for (String a : acteurs) {
                kijk(a, t, x, y, z);
            }
            return this;
        }

        Film anim(String acteur, int t, String naam) {
            b.animatie(acteur, t + o, naam);
            return this;
        }

        Film springAllen(int t, String... acteurs) {
            for (int i = 0; i < acteurs.length; i++) {
                anim(acteurs[i], t + (i % 3) * 2, "spring");
            }
            return this;
        }

        Film zeg(int t, String wie, String key, int ticks) {
            b.zeg(t + o, wie, key, ticks);
            return this;
        }

        Film geluid(int t, SoundEvent s, float volume, float pitch) {
            b.geluid(t + o, () -> s, volume, pitch);
            return this;
        }

        Film deeltjes(int t, ParticleOptions p, double x, double y, double z, int n, double spreiding) {
            b.deeltjes(t + o, p, r(x, y, z), n, spreiding);
            return this;
        }

        Film schud(int t, float kracht, int ticks) {
            b.schud(t + o, kracht, ticks);
            return this;
        }

        Film zwart(int t0, int t1) {
            b.zwart(t0 + o, t1 + o);
            return this;
        }

        Cutscene klaar(int duur, String lijn, double verberg) {
            return b.duur(duur + o).bij(lijn).verbergEcht(verberg).registreer();
        }
    }

    /** The save data of a guh actor (read on the client like a saved entity): variant, clothes, hair, size, look piece, sitting. */
    static void vul(CompoundTag tag, @Nullable NpcDefs.Def d, NpcFase fase, boolean zit) {
        if (d == null) {
            return;
        }
        tag.putString("Variant", d.variant().id());
        for (GuhClothes c : d.kleren(fase)) {
            String slot = c.slot.name();
            tag.putString("Clothes" + slot.charAt(0) + slot.substring(1).toLowerCase(Locale.ROOT), c.id());
        }
        if (d.haarkleur() >= 0) {
            tag.putInt("Haarkleur", d.haarkleur());
        }
        if (d.look() != null) {
            ListTag tags = new ListTag();
            tags.add(StringTag.valueOf(LOOK + d.look()));
            tag.put("Tags", tags);
        }
        if (d.maat() != 1f) {
            ListTag attrs = new ListTag();
            CompoundTag a = new CompoundTag();
            a.putString("id", "minecraft:scale");
            a.putDouble("base", d.maat());
            attrs.add(a);
            tag.put("attributes", attrs);
        }
        if (zit) {
            tag.putBoolean("Sitting", true);
        }
    }

    /** The entity tag that gives an actor the plein look piece of that name (client.WinterVerhaalClient draws it). */
    public static final String LOOK = "guhwinter_look_";

    private static final String[] ZES = {"joost", "suus", "liek", "lin", "evi", "niels"};

    // =====================================================================================================================
    // chapter 1
    // =====================================================================================================================

    /**
     * The intocht at the kade: the Pakjesboot bumps into the kade (pakjes overboard), the Sint greets the "children of
     * GORINGUHM", the guhs set him straight, Anthony and Niels have opinions, and Ozovads bolts off to the portal. The
     * flashback version (after 6 December) opens with a "a few weeks ago..." line.
     */
    private static Cutscene intocht(boolean terug) {
        Film f = new Film(terug ? "guhwinter_intocht_terug" : "guhwinter_intocht", "kade_boot", new BlockPos(-286, 31, -672), terug ? 70 : 0);
        NpcFase s = NpcFase.SINT;
        f.speler(-283.5, 31, -675.5, 135)
                .guh("sint", -282.5, 31, -666.6, 90, s, false).paard(-280.0, 31, -666.6, 90)
                .guh("pakjesguh_0", -281.0, 31, -668.0, 90, s, false).guh("pakjesguh_1", -284.0, 31, -665.6, 90, s, false)
                .guh("lin", -292.5, 31, -674.6, -135, s, false).guh("liek", -291.2, 31, -675.2, -135, s, false)
                .guh("niels", -289.8, 31, -675.6, -150, s, false).guh("evi", -288.5, 31, -675.6, -160, s, false)
                .guh("joost", -287.2, 31, -675.2, 180, s, false).guh("suus", -286.0, 31, -674.6, 170, s, false)
                .guh("rins", -294.0, 31, -672.4, -120, s, false).guh("anthony", -295.2, 31, -673.2, -120, s, false);
        if (terug) {
            f.b.camera(0, f.r(-293, 38, -684), f.r(-283, 32, -667)).zeg(8, "", "terug", 60);
        }
        f.camera(0, -293, 37, -683, -283, 32, -667)
                .camera(70, -290, 35.5, -679, -283, 32, -668)
                .geluid(4, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1.0f, 0.6f)
                .zeg(8, "", "intro", 60)
                .schud(24, 1.0f, 12).geluid(24, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.6f, 0.7f)
                .deeltjes(28, ParticleTypes.SPLASH, -284.5, 30.6, -668.8, 40, 0.8)
                .geluid(32, SoundEvents.GENERIC_SPLASH, 0.8f, 1.2f).geluid(40, SoundEvents.GENERIC_SPLASH, 0.8f, 1.0f)
                .geluid(48, SoundEvents.GENERIC_SPLASH, 0.8f, 1.4f)
                .zeg(30, "", "plons", 50)
                // the Sint comes down the gangplank
                .loop("sint", 80, 125, -284.6, 31, -671.4)
                .knipVolgt(81, -289.5, 33.6, -676.5, "sint")
                .kijk("sint", 126, -288.5, 32, -675.5)
                .kijkAllen(80, -284.6, 32, -671.4, ZES)
                .anim("sint", 128, "zwaai")
                .zeg(128, "sint", "welkom", 60)
                .knip(190, -284.0, 33.2, -670.6, -289.0, 32.4, -675.2)
                .zeg(190, "allen", "guhmension", 50)
                .knip(245, -286.8, 32.9, -673.3, -284.6, 32.8, -671.4)
                .anim("sint", 245, "buk")
                .zeg(245, "sint", "kaart", 70)
                .anim("sint", 316, "sta")
                .zeg(316, "sint", "goringuhm", 70)
                .knip(390, -292.0, 32.9, -670.6, -295.2, 32.6, -673.2)
                .anim("anthony", 392, "zwaai").deeltjes(392, ParticleTypes.END_ROD, -295.0, 32.9, -673.6, 6, 0.15)
                .geluid(392, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.7f, 1.6f)
                .zeg(392, "anthony", "bishop", 55)
                .zeg(448, "rins", "spanje", 55)
                .zeg(504, "anthony", "boot", 45)
                .knip(552, -288.2, 32.9, -672.6, -289.6, 32.5, -675.5)
                .zeg(552, "niels", "trein", 60)
                .zeg(613, "evi", "geentrein", 50)
                .zeg(664, "niels", "nog", 40)
                // Ozovads bolts
                .geluid(708, SoundEvents.HORSE_AMBIENT, 1.0f, 1.3f)
                .anim("ozovads", 708, "spring")
                .loop("ozovads", 712, 745, -287.5, 31, -671.0).loop("ozovads", 745, 790, -299.0, 31, -663.0)
                .knipVolgt(709, -291.0, 35.5, -679.5, "ozovads")
                .volgt(790, -294.0, 35.0, -677.0, "ozovads")
                .kijkAllen(712, -297.0, 32, -665.0, ZES)
                .zeg(714, "pakjesguh_0", "ozovads", 60)
                .knip(796, -287.0, 32.9, -673.9, -284.6, 32.8, -671.4)
                .kijk("sint", 796, -287.0, 32.4, -674.5)
                .zeg(798, "sint", "pakjes", 70)
                .knip(870, -287.0, 32.9, -678.6, -286.0, 32.6, -674.6)
                .kijk("suus", 870, -283.5, 32.4, -675.5)
                .zeg(872, "suus", "helpen", 70)
                .zwart(944, 960);
        return f.klaar(965, WinterVerhaal.Hoofdstuk.SINT.id, 18);
    }

    /** At the end of the trail: Ozovads behind the rocks, a carrot, a sorry-whinny, and he walks back with you. */
    private static Cutscene ozovads() {
        Film f = new Film("guhwinter_ozovads", "paard_verstop", new BlockPos(-294, 30, -644), 0);
        f.speler(-295.6, 30, -650.0, 0).paard(-293.5, 30, -643.4, 160)
                .camera(0, -299.5, 33.0, -650.5, -294.0, 31, -645.0)
                .zeg(6, "", "spoor", 60)
                .loop("speler", 20, 70, -294.2, 30, -646.6)
                .camera(70, -297.5, 32.2, -648.5, -294.0, 31, -645.0)
                .anim("speler", 74, "buk")
                .zeg(78, "speler", "psst", 50)
                .anim("speler", 130, "sta")
                .kijk("ozovads", 130, -294.2, 31.6, -646.6)
                .geluid(134, SoundEvents.HORSE_AMBIENT, 1.0f, 1.25f)
                .deeltjes(136, ParticleTypes.HEART, -293.5, 31.9, -643.6, 5, 0.4)
                .knip(138, -291.6, 31.9, -647.8, -293.8, 31.3, -644.6)
                .zeg(140, "ozovads", "hinnik", 70)
                .loop("ozovads", 214, 250, -293.4, 30, -645.4)
                .deeltjes(250, ParticleTypes.HEART, -293.4, 31.9, -645.4, 4, 0.4)
                .zeg(252, "", "samen", 60)
                .loop("speler", 316, 380, -297.0, 30, -655.0).loop("ozovads", 318, 382, -295.6, 30, -655.6)
                .knipVolgt(316, -292.0, 33.5, -646.0, "ozovads")
                .zwart(370, 386);
        return f.klaar(390, WinterVerhaal.Hoofdstuk.SINT.id, 10);
    }

    /** Back at the boot with Ozovads and the pakjes: "Dan vieren we het dit jaar hier!", Het Grote Boek, the schoentje, Suus' wish list. */
    private static Cutscene sintBlijft(boolean terug) {
        Film f = new Film(terug ? "guhwinter_sintblijft_terug" : "guhwinter_sintblijft", "kade_boot", new BlockPos(-286, 31, -672), terug ? 70 : 0);
        NpcFase s = NpcFase.SINT;
        f.speler(-292.5, 31, -677.5, -120)
                .guh("sint", -284.4, 31, -672.4, 90, s, false).paard(-282.0, 31, -673.6, 90)
                .guh("pakjesguh_0", -283.5, 31, -675.0, 60, s, false).guh("pakjesguh_1", -282.6, 31, -670.8, 120, s, false)
                .guh("joost", -287.6, 31, -676.4, -150, s, false).guh("suus", -286.4, 31, -676.8, -170, s, false)
                .guh("evi", -289.0, 31, -676.6, -140, s, false).guh("niels", -290.2, 31, -676.0, -130, s, false)
                .guh("liek", -289.6, 31, -673.0, -100, s, false).guh("lin", -290.6, 31, -672.4, -100, s, false);
        if (terug) {
            f.b.camera(0, f.r(-293, 36, -683), f.r(-285, 32, -672)).zeg(8, "", "terug", 60);
        }
        f.camera(0, -293, 35.5, -682, -286, 32, -673)
                .loop("speler", 4, 46, -286.9, 31, -673.6)
                .kijk("sint", 46, -286.9, 32.4, -673.6)
                .knip(48, -288.6, 33.0, -675.2, -284.4, 32.8, -672.4)
                .deeltjes(48, ParticleTypes.HAPPY_VILLAGER, -285.6, 32.0, -673.0, 12, 0.6)
                .zeg(50, "sint", "held", 60)
                .knip(112, -285.5, 32.8, -677.2, -283.5, 32.4, -675.0)
                .zeg(114, "pakjesguh_0", "tel", 60)
                .knip(176, -288.6, 33.0, -675.2, -284.4, 32.8, -672.4)
                .zeg(178, "sint", "kaart", 60)
                .knip(240, -285.0, 32.8, -674.2, -287.6, 32.4, -676.4)
                .zeg(242, "joost", "blijf", 60)
                .camera(300, -291.5, 35.0, -680.5, -286, 32, -674)
                .anim("sint", 302, "zwaai")
                .zeg(302, "sint", "hier", 60)
                .springAllen(304, "joost", "suus", "evi", "niels", "liek", "lin", "pakjesguh_0", "pakjesguh_1", Cutscene.SPELER)
                .geluid(304, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.2f)
                .deeltjes(306, ParticleTypes.FIREWORK, -286.5, 33.5, -674.5, 40, 1.4)
                .knip(366, -288.6, 33.0, -675.2, -284.4, 32.8, -672.4)
                .zeg(368, "sint", "boek", 65)
                .zeg(434, "sint", "schoen", 70)
                .knip(506, -284.6, 32.8, -675.4, -286.4, 32.4, -676.8)
                .kijk("suus", 506, -284.4, 32.6, -672.4)
                .zeg(508, "suus", "lijstje", 70)
                .zeg(580, "sint", "suus", 55)
                .kijk("joost", 636, -286.4, 32.4, -676.8)
                .zeg(636, "joost", "donut", 55)
                .zeg(692, "suus", "zucht", 45)
                .zwart(740, 756);
        return f.klaar(760, WinterVerhaal.Hoofdstuk.SINT.id, 18);
    }

    /** Pakjesavond at Krijn and Corine's: a BONK on the door, a sack of pakjes, and Suus' poem for Joost that rhymes on "ring". */
    private static Cutscene pakjesavond() {
        Film f = new Film("guhwinter_pakjesavond", "huis_tafel_midden", new BlockPos(-312, 33, -727), 0);
        NpcFase s = NpcFase.SINT;
        double zn = 32.5;   // (sitting on the chairs: on top of the seat)
        f.speler(-310.5, zn, -725.5, 180)
                .guh("marrie", -314.5, zn, -728.5, 0, s, true).guh("niels", -313.5, zn, -728.5, 0, s, true)
                .guh("evi", -312.5, zn, -728.5, 0, s, true).guh("liek", -311.5, zn, -728.5, 0, s, true)
                .guh("lin", -310.5, zn, -728.5, 0, s, true).guh("rins", -309.5, zn, -728.5, 0, s, true)
                .guh("anthony", -314.5, zn, -725.5, 180, s, true).guh("suus", -313.5, zn, -725.5, 180, s, true)
                .guh("joost", -312.5, zn, -725.5, 180, s, true)
                .guh("krijn", -315.8, 32, -727.0, -90, s, false).guh("corine", -308.2, 32, -727.0, 90, s, false)
                .camera(0, -311.0, 35.6, -721.8, -312.0, 33.0, -727.0)
                .zeg(8, "", "avond", 55)
                .geluid(64, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.5f, 1.5f).geluid(72, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.5f, 1.5f)
                .geluid(80, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.5f, 1.5f).schud(64, 0.3f, 20)
                .kijkAllen(64, -313.0, 33.0, -719.0, "marrie", "niels", "evi", "liek", "lin", "rins", "anthony", "suus", "joost", "krijn", "corine")
                .zeg(70, "", "bonk", 40)
                .zeg(112, "corine", "deur", 50)
                .loop("krijn", 120, 162, -313.0, 32, -719.8)
                .knipVolgt(122, -310.0, 34.6, -723.0, "krijn")
                .zeg(166, "krijn", "zak", 70)
                .loop("krijn", 238, 280, -315.8, 32, -727.0)
                .knip(240, -311.0, 35.6, -721.8, -312.0, 33.0, -727.0)
                .deeltjes(282, ParticleTypes.HAPPY_VILLAGER, -312.0, 33.4, -727.0, 30, 1.4)
                .geluid(284, SoundEvents.BUNDLE_DROP_CONTENTS, 0.8f, 1.0f).geluid(298, SoundEvents.BUNDLE_DROP_CONTENTS, 0.8f, 1.2f)
                .zeg(284, "", "uitpakken", 60)
                .knip(346, -312.6, 34.4, -723.4, -313.5, 33.3, -725.5)
                .kijk("suus", 346, -312.5, 33.4, -725.5)
                .zeg(348, "suus", "gedicht", 55)
                .zeg(404, "suus", "rijm1", 70)
                .zeg(476, "suus", "rijm2", 70)
                .zeg(548, "suus", "rijm3", 70)
                .zeg(620, "suus", "rijm4", 55)
                .knip(676, -311.6, 34.4, -723.4, -312.5, 33.3, -725.5)
                .kijk("joost", 676, -313.5, 33.4, -725.5)
                .zeg(678, "joost", "snap", 65)
                .zeg(744, "suus", "toeval", 40)
                .zeg(786, "joost", "uienring", 55)
                .knip(842, -312.0, 34.6, -722.6, -314.5, 33.3, -725.5)
                .zeg(844, "anthony", "santa", 60)
                .knip(906, -312.0, 34.4, -731.4, -311.5, 33.3, -728.5)
                .zeg(908, "liek", "suus", 50)
                .zwart(964, 980);
        return f.klaar(985, WinterVerhaal.Hoofdstuk.SINT.id, 12);
    }

    // =====================================================================================================================
    // chapter 2
    // =====================================================================================================================

    /** In front of the house: a tree is needed, Niels plans a train route, nothing fits, and you suggest... carrying it. */
    private static Cutscene boomhalen() {
        Film f = new Film("guhwinter_boomhalen", "huis_deur", new BlockPos(-313, 31, -717), 0);
        NpcFase k = NpcFase.KERST;
        f.speler(-313.0, 31, -711.8, 180)
                .guh("joost", -314.6, 31, -714.4, -30, k, false).guh("niels", -311.4, 31, -714.4, 30, k, false)
                .camera(0, -313.0, 33.4, -708.0, -313.0, 32.2, -714.0)
                .zeg(8, "joost", "boom", 60)
                .knip(70, -312.0, 32.8, -711.6, -311.4, 32.6, -714.4)
                .anim("niels", 72, "zwaai")
                .zeg(72, "niels", "plan", 70)
                .knip(144, -314.0, 32.8, -711.6, -314.6, 32.6, -714.4)
                .kijk("joost", 144, -311.4, 32.4, -714.4)
                .zeg(146, "joost", "meter", 50)
                .knip(198, -312.0, 32.8, -711.6, -311.4, 32.6, -714.4)
                .zeg(200, "niels", "fiets", 50)
                .zeg(252, "joost", "past", 35)
                .zeg(290, "niels", "bakfiets", 35)
                .zeg(328, "joost", "past", 35)
                .zeg(366, "niels", "ballon", 65)
                .camera(434, -313.0, 33.4, -708.5, -313.0, 32.2, -713.0)
                .kijkAllen(434, -313.0, 32.4, -711.8, "joost", "niels")
                .zeg(436, Cutscene.SPELER, "dragen", 45)
                .zeg(484, "joost", "stil", 40)
                .springAllen(528, "joost", "niels")
                .zeg(528, "niels", "briljant", 55)
                .zeg(586, "joost", "plek", 60)
                .zwart(650, 666);
        return f.klaar(670, WinterVerhaal.Hoofdstuk.KERST.id, 8);
    }

    /** The Christmas dinner: gourmetten at the long table, Anthony's first gourmet, Suus hints, opa "Jochum", the little ones pop in. */
    private static Cutscene kerstdiner() {
        Film f = new Film("guhwinter_kerstdiner", "huis_tafel_midden", new BlockPos(-312, 33, -727), 0);
        NpcFase k = NpcFase.KERST;
        double zn = 32.5;
        f.speler(-315.8, 32, -727.0, -90)
                .guh("wijnand", -314.5, zn, -728.5, 0, k, true).guh("marrie", -313.5, zn, -728.5, 0, k, true)
                .guh("niels", -312.5, zn, -728.5, 0, k, true).guh("evi", -311.5, zn, -728.5, 0, k, true)
                .guh("lin", -310.5, zn, -728.5, 0, k, true).guh("liek", -309.5, zn, -728.5, 0, k, true)
                .guh("corine", -314.5, zn, -725.5, 180, k, true).guh("krijn", -313.5, zn, -725.5, 180, k, true)
                .guh("suus", -312.5, zn, -725.5, 180, k, true).guh("joost", -311.5, zn, -725.5, 180, k, true)
                .guh("rins", -310.5, zn, -725.5, 180, k, true).guh("anthony", -309.5, zn, -725.5, 180, k, true)
                .guh("daan", -313.4, 32, -719.6, 180, k, false).guh("kim", -312.4, 32, -719.6, 180, k, false)
                .guh("finn", -313.0, 32, -720.4, 180, k, false).guh("liv", -312.0, 32, -720.4, 180, k, false)
                .camera(0, -307.4, 35.4, -727.0, -312.0, 33.0, -727.0)
                .deeltjes(4, ParticleTypes.SMOKE, -312.0, 33.6, -727.0, 10, 0.8)
                .zeg(8, "corine", "gourmet", 55)
                .knip(66, -309.6, 34.2, -723.6, -309.5, 33.3, -725.5)
                .zeg(68, "anthony", "zelf", 70)
                .knip(140, -313.6, 34.2, -723.6, -313.5, 33.3, -725.5)
                .zeg(142, "krijn", "gezellig", 45)
                .knip(190, -310.5, 34.2, -723.4, -310.0, 33.3, -725.5)
                .zeg(192, "rins", "pannetje", 55)
                .deeltjes(250, ParticleTypes.SMOKE, -309.5, 33.6, -726.6, 8, 0.2)
                .geluid(250, SoundEvents.CAMPFIRE_CRACKLE, 1.0f, 1.4f)
                .anim("anthony", 252, "spring")
                .zeg(252, "anthony", "chef", 60)
                .knip(316, -310.5, 34.2, -731.6, -310.5, 33.3, -728.5)
                .zeg(318, "lin", "gefrituurd", 55)
                .knip(376, -312.5, 34.2, -731.6, -312.5, 33.3, -728.5)
                .zeg(378, "niels", "optijd", 55)
                .knip(436, -312.0, 34.2, -723.4, -312.0, 33.3, -725.5)
                .kijk("suus", 436, -311.5, 33.4, -725.5)
                .zeg(438, "suus", "toetje", 70)
                .kijk("joost", 510, -312.5, 33.4, -725.5)
                .zeg(510, "joost", "nogeen", 45)
                .zeg(558, "suus", "ja", 45)
                .knip(606, -314.0, 34.2, -731.6, -314.5, 33.3, -728.5)
                .kijk("corine", 606, -314.5, 33.4, -728.5)
                .zeg(608, "corine", "opa", 45)
                .zeg(656, "wijnand", "naam", 35)
                .geluid(696, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.4f, 1.6f).geluid(704, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.4f, 1.6f)
                .camera(696, -307.4, 35.4, -724.0, -312.0, 33.0, -723.0)
                .zeg(698, "", "deur", 55)
                .loop("daan", 704, 760, -308.2, 32, -723.4).loop("kim", 706, 762, -308.2, 32, -730.4)
                .loop("finn", 700, 744, -315.0, 32, -730.0).loop("liv", 702, 750, -308.0, 32, -725.0)
                .kijk("wijnand", 746, -315.0, 33.0, -730.0)
                .knip(756, -317.2, 33.6, -731.4, -315.0, 32.6, -730.0)
                .anim("finn", 758, "spring")
                .zeg(758, "finn", "jochum", 40)
                .zeg(800, "wijnand", "finn", 50)
                .knip(852, -306.2, 33.4, -725.4, -308.0, 32.5, -725.0)
                .anim("liv", 854, "spring")
                .zeg(854, "liv", "kaas", 50)
                .knip(906, -306.0, 34.0, -722.4, -308.2, 33.0, -723.4)
                .zeg(908, "daan", "even", 55)
                .zeg(964, "kim", "kerst", 45)
                .camera(1010, -307.0, 36.0, -727.0, -312.0, 33.0, -727.0)
                .zeg(1012, "allen", "vahoeg", 55)
                .springAllen(1014, "finn", "liv", "daan", "kim", Cutscene.SPELER)
                .deeltjes(1016, ParticleTypes.HEART, -312.0, 34.0, -727.0, 16, 2.0)
                .geluid(1016, SoundEvents.PLAYER_LEVELUP, 0.7f, 1.4f)
                .zwart(1070, 1086);
        return f.klaar(1090, WinterVerhaal.Hoofdstuk.KERST.id, 12);
    }
}
