package nl.juiced.guhs.feature.ringsausuman;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-sausuman): what happens at the blocks of the tower's questline, per player.
 * <ul>
 *   <li>{@link #klikVoorraad}: a station (Deegkneder, Sauskraan, Kaaskast) gives its ingredient to a player whose questline is
 *       at "collect" or "pull the lever", once, and again when they lost it. The third one finishes "collect".</li>
 *   <li>{@link #klikBakker}: the lever of the Ringenbakker. With the three ingredients: they go in (remembered in the flag
 *       {@link #GEVULD}, so a scene that was cut off can simply be played again) and the baking scene {@link #BAKKEN} plays;
 *       after it the player has an onion ring and Sausuman sulks. Once the questline is done the lever gives one onion ring
 *       per player per day.</li>
 * </ul>
 * Nothing in the world changes: the stations never run out, the machine bakes for everybody, every player has their own
 * progress in {@link RingSausumanFeature#LIJN}.
 */
public final class Bakkerij {
    private static final String T = "gui.guhs.ringsausuman.";
    /** Flag of the questline: this player's three ingredients are in the machine. */
    public static final String GEVULD = "gevuld";
    /** Counter of the questline: the day (1..) this player last took their daily onion ring. */
    public static final String RING_DAG = "ring_dag";
    /** The length of the baking scene in ticks. */
    public static final int DUUR = 436;

    /**
     * The baking scene. Anchored on the Ringenbakker's face (which looks south in the unturned template): x to the east,
     * z into the hall, y 0 = the machine's face, the hall's floor is y -1. Actors: Sausuman on his own spot, the player's
     * stand-in in front of the machine, Sam-guh next to them, and the onion ring (an item display that slides out of the
     * machine's mouth onto the tray).
     */
    public static Cutscene BAKKEN;

    static void registreer() {
        Vec3 machine = new Vec3(0.5, 0.4, 0.6), bak = new Vec3(0.5, -0.45, 1.5), mokhoek = new Vec3(-3.4, -0.2, 1.2);
        BAKKEN = Cutscene.maak("ringsausuman_bakken").duur(DUUR).bij("ring_sausuman").verbergEcht(9)
                .speler(new Vec3(0.5, -1, 3.5), 180)
                .npc("sausuman", GuhNpcEntity.Kind.SAUSUMAN, new Vec3(-2.5, -1, 2.5), -45)
                .guh("sam", GuhVariant.SAM_GUH, new Vec3(2.5, -1, 4.5), 150)
                .acteur("uienring", () -> EntityType.ITEM_DISPLAY, new Vec3(0.5, 0.1, 0.5), 180, Bakkerij::uienringActeur)
                // 1: the wizard announces it; the hall from the door
                .camera(0, new Vec3(3.6, 2.4, 7.4), new Vec3(0.2, 0.2, 1.0))
                .camera(78, new Vec3(2.6, 1.6, 6.2), new Vec3(0.2, 0.2, 1.0))
                .kijk("sausuman", 0, machine).kijk(Cutscene.SPELER, 0, machine).kijk("sam", 0, machine)
                .animatie("sausuman", 8, "toover")
                .zeg(8, "sausuman", "start", 66)
                // 2: the machine wakes up: smoke, sparks, the whole tower shakes
                .cameraKnip(80, new Vec3(1.9, 0.6, 4.6), machine)
                .camera(150, new Vec3(1.2, 0.5, 3.6), machine)
                .geluid(80, RingSausumanFeature.RONK, 1.0f, 0.8f)
                .schud(82, 0.6f, 40)
                .deeltjes(84, ParticleTypes.CAMPFIRE_COSY_SMOKE, new Vec3(-1.5, 2.2, 0.5), 6, 0.2)
                .deeltjes(84, ParticleTypes.CAMPFIRE_COSY_SMOKE, new Vec3(2.5, 2.2, 0.5), 6, 0.2)
                .zeg(88, "sausuman", "spreuk", 60)
                .geluid(104, RingSausumanFeature.SPUTTER, 1.0f, 0.7f)
                .deeltjes(104, ParticleTypes.LARGE_SMOKE, machine, 14, 0.5)
                .geluid(118, RingSausumanFeature.SPUTTER, 1.0f, 1.1f)
                .deeltjes(118, ParticleTypes.LAVA, machine, 6, 0.3)
                .schud(120, 1.3f, 36)
                .animatie("sam", 120, "schrik")
                .geluid(132, RingSausumanFeature.SPUTTER, 1.0f, 1.4f)
                .deeltjes(132, ParticleTypes.FLAME, machine, 16, 0.4)
                .deeltjes(144, ParticleTypes.LARGE_SMOKE, machine, 20, 0.6)
                // 3: PLING. Something rolls out
                .geluid(156, RingSausumanFeature.PLING, 1.0f, 1.0f)
                .deeltjes(156, ParticleTypes.END_ROD, new Vec3(0.5, 0.0, 1.1), 12, 0.3)
                .loop("uienring", 156, 172, bak)
                .cameraKnip(160, new Vec3(1.7, 0.1, 3.0), bak)
                .camera(226, new Vec3(1.3, -0.1, 2.6), bak)
                .zeg(166, "", "pling", 58)
                .animatie("sausuman", 160, "")
                // 4: he looks. It is not a Knabbelring
                .cameraKnipVolgt(228, new Vec3(0.9, 0.2, 5.0), "sausuman")
                .loop("sausuman", 230, 258, new Vec3(-0.6, -1, 2.4))
                .kijk("sausuman", 258, bak)
                .animatie("sausuman", 262, "kijk")
                .zeg(236, "sausuman", "ui", 74)
                .animatie("sausuman", 286, "schrik")
                // 5: Sam-guh thinks it smells nice
                .cameraKnip(314, new Vec3(-1.4, 0.5, 1.6), new Vec3(1.8, -0.4, 4.2))
                .kijk("sam", 314, bak)
                .animatie("sam", 316, "knik")
                .zeg(318, "sam", "sam", 46)
                // 6: and the wizard goes to sulk in his corner
                .cameraKnipVolgt(366, new Vec3(1.6, 0.5, 5.4), "sausuman")
                .animatie("sausuman", 366, "schud")
                .zeg(368, "sausuman", "mok", 60)
                .loop("sausuman", 372, 400, new Vec3(-2.5, -1, 2.5))
                .kijk("sausuman", 400, mokhoek)
                .animatie("sausuman", 402, "mok")
                .geluid(404, RingSausumanFeature.MOK, 1.0f, 1.0f)
                .zwart(DUUR - 14, DUUR)
                .registreer();
    }

    /** The onion ring of the scene: an item display that always faces the camera. */
    private static void uienringActeur(CompoundTag tag) {
        CompoundTag item = new CompoundTag();
        item.putString("id", "guhs:ringsausuman_uienring");
        item.putInt("count", 1);
        tag.put("item", item);
        tag.putString("billboard", "vertical");
    }

    private static Verhaallijn lijn() {
        return RingSausumanFeature.LIJN;
    }

    /** Does this player carry this ingredient? */
    public static boolean heeft(ServerPlayer p, Ingredient soort) {
        return GuhQuests.count(p, soort.item()) > 0;
    }

    /** How many of the three ingredients this player carries (0..3). */
    public static int aantal(ServerPlayer p) {
        int n = 0;
        for (Ingredient soort : Ingredient.values()) {
            n += heeft(p, soort) ? 1 : 0;
        }
        return n;
    }

    /** The day of this world, from 1 (for "once a day"). */
    static int dag(ServerPlayer p) {
        return (int) (p.level().getGameTime() / 24000L) + 1;
    }

    private static void bericht(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.GRAY));
    }

    /** A click on a station. */
    public static void klikVoorraad(ServerPlayer p, BlockPos pos, Ingredient soort) {
        Verhaallijn lijn = lijn();
        int stap = lijn.stap(p);
        if (!lijn.aanDeBeurt(p) || stap == 0) {
            bericht(p, T + "voorraad.eerst_praten");
            return;
        }
        if (stap >= 3 || (stap == 2 && lijn.vlag(p, GEVULD))) {
            bericht(p, T + "voorraad.klaar." + soort.id());
            return;
        }
        if (heeft(p, soort)) {
            bericht(p, T + "voorraad.heb_je." + soort.id());
            return;
        }
        Minigames.give(p, new ItemStack(soort.item()));
        p.level().playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 0.8f);
        bericht(p, T + "voorraad.pak." + soort.id());
        int n = aantal(p);
        if (stap == 1 && n == Ingredient.values().length) {
            if (lijn.verder(p, 1)) {
                GuhQuests.hint(p, "quest.guhs.ringsausuman.hint.hendel");
            }
        } else if (stap == 1) {
            p.sendOverlayMessage(Component.translatable(T + "voorraad.nog", Ingredient.values().length - n));
        }
    }

    /** A click on the Ringenbakker: the lever. */
    public static void klikBakker(ServerPlayer p, BlockPos pos, BlockState state) {
        Verhaallijn lijn = lijn();
        int stap = lijn.stap(p);
        if (!lijn.aanDeBeurt(p) || stap == 0) {
            bericht(p, T + "bakker.afblijven");
            return;
        }
        if (stap == 1) {
            bericht(p, T + "bakker.mist", aantal(p), Ingredient.values().length);
            return;
        }
        if (stap == 2) {
            if (Cutscenes.bezig(p)) {
                return;
            }
            if (!lijn.vlag(p, GEVULD)) {
                if (aantal(p) < Ingredient.values().length) {
                    bericht(p, T + "bakker.mist", aantal(p), Ingredient.values().length);
                    return;
                }
                for (Ingredient soort : Ingredient.values()) {
                    GuhQuests.take(p, soort.item(), 1);
                }
                lijn.vlag(p, GEVULD, true);
            }
            // (a scene that is cut off by a logout never ran `gebakken`: the ingredients are in, the lever simply plays it again)
            Cutscenes.speel(p, BAKKEN, pos, Toren.draai(state.getValue(HorizontalDirectionalBlock.FACING)), Bakkerij::gebakken);
            return;
        }
        if (stap == 3) {
            bericht(p, T + "bakker.koelt_af");
            return;
        }
        // the questline is done: one onion ring a day, for every player their own
        int dag = dag(p);
        if (lijn.teller(p, RING_DAG) == dag) {
            bericht(p, T + "bakker.morgen");
            return;
        }
        lijn.teller(p, RING_DAG, dag);
        Minigames.give(p, new ItemStack(RingSausumanFeature.UIENRING.get()));
        p.level().playSound(null, pos, RingSausumanFeature.PLING.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        bericht(p, T + "bakker.dagelijks");
        GuhAdvancements.grant(p, "ring_sausuman_dagelijks");
    }

    /** After the baking scene: the onion ring is the player's, Sausuman sulks. */
    static void gebakken(ServerPlayer p) {
        if (lijn().verder(p, 2)) {
            Minigames.give(p, new ItemStack(RingSausumanFeature.UIENRING.get()));
            GuhAdvancements.grant(p, "ring_sausuman_uienring");
            GuhQuests.hint(p, "quest.guhs.ringsausuman.hint.mok");
        }
    }

    private Bakkerij() {
    }
}
