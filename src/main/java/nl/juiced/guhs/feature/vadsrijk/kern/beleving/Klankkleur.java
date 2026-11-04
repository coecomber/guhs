package nl.juiced.guhs.feature.vadsrijk.kern.beleving;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;

/**
 * The combat sound layer (idea 20): vanilla sounds pitched and layered, no new assets. One hit sound per weapon kind
 * ({@code BasisType.soort}: zwaard, knots, hamer, boog, kruisboog, katapult, staf, toverstok, boek; else a fist), a glittering
 * "ding" on a crit, and an element sound (Vuur crackles, Water/Slijm squelches, Natuur/Zwam puffs spores, Roze chimes).
 */
public final class Klankkleur {
    private Klankkleur() {
    }

    /** A layer: sound, volume, pitch. */
    public record Laag(SoundEvent geluid, float volume, float toon) {
    }

    /** The layers of a weapon kind (pure, also used by the dev command). */
    public static Laag[] wapen(String soort) {
        return switch (soort == null ? "" : soort) {
            case "zwaard" -> new Laag[] {new Laag(SoundEvents.PLAYER_ATTACK_SWEEP, 0.35f, 1.5f), new Laag(SoundEvents.TRIDENT_HIT, 0.45f, 1.6f)};
            case "knots" -> new Laag[] {new Laag(SoundEvents.MACE_SMASH_AIR, 0.6f, 1.25f), new Laag(SoundEvents.WOOL_HIT, 0.9f, 0.7f)};
            case "hamer" -> new Laag[] {new Laag(SoundEvents.MACE_SMASH_GROUND, 0.7f, 1.1f), new Laag(SoundEvents.ANVIL_LAND, 0.12f, 1.9f)};
            case "boog" -> new Laag[] {new Laag(SoundEvents.ARROW_HIT, 0.7f, 1.3f), new Laag(SoundEvents.WOOL_HIT, 0.5f, 1.4f)};
            case "kruisboog" -> new Laag[] {new Laag(SoundEvents.CROSSBOW_HIT, 0.8f, 0.85f), new Laag(SoundEvents.TRIDENT_HIT, 0.4f, 0.8f)};
            case "katapult" -> new Laag[] {new Laag(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.7f, 1.5f), new Laag(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.8f, 0.9f)};
            case "staf" -> new Laag[] {new Laag(SoundEvents.EVOKER_CAST_SPELL, 0.35f, 1.7f), new Laag(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 0.8f)};
            case "toverstok" -> new Laag[] {new Laag(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5f, 1.8f), new Laag(SoundEvents.ILLUSIONER_CAST_SPELL, 0.25f, 2.0f)};
            case "boek" -> new Laag[] {new Laag(SoundEvents.BOOK_PAGE_TURN, 0.9f, 1.3f), new Laag(SoundEvents.ENCHANTMENT_TABLE_USE, 0.35f, 1.7f)};
            default -> new Laag[] {new Laag(SoundEvents.PLAYER_ATTACK_WEAK, 0.6f, 1.2f)};
        };
    }

    public static Laag[] element(Element e) {
        return switch (e) {
            case VUUR -> new Laag[] {new Laag(SoundEvents.CAMPFIRE_CRACKLE, 1.0f, 1.5f), new Laag(SoundEvents.FIRECHARGE_USE, 0.2f, 1.7f)};
            case WATER -> new Laag[] {new Laag(SoundEvents.SLIME_SQUISH, 0.6f, 1.3f), new Laag(SoundEvents.HONEY_BLOCK_SLIDE, 0.5f, 1.4f)};
            case NATUUR -> new Laag[] {new Laag(SoundEvents.PUFFER_FISH_BLOW_OUT, 0.5f, 1.6f), new Laag(SoundEvents.MOSS_BREAK, 0.6f, 1.2f)};
            case ROZE -> new Laag[] {new Laag(SoundEvents.AMETHYST_BLOCK_CHIME, 0.4f, 1.5f)};
            default -> new Laag[0];
        };
    }

    public static final Laag[] KRITIEK = {new Laag(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 1.9f), new Laag(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 2.0f),
            new Laag(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.25f, 1.8f)};

    static void speel(ServerLevel sl, LivingEntity bij, Laag[] lagen, float volume) {
        for (Laag l : lagen) {
            sl.playSound(null, bij.getX(), bij.getY() + bij.getBbHeight() * 0.5, bij.getZ(), l.geluid(), SoundSource.PLAYERS, l.volume() * volume,
                    l.toon() * (0.95f + sl.getRandom().nextFloat() * 0.1f));
        }
    }

    static void speel(ServerLevel sl, double x, double y, double z, Holder<SoundEvent> geluid, float volume, float toon) {
        sl.playSound(null, x, y, z, geluid.value(), SoundSource.PLAYERS, volume, toon);
    }
}
