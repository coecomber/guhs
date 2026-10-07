package nl.juiced.guhs.feature.snuffel;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A resident of Het Snuffeleiland (entity {@code guhs:snuffel_bewoner}): ONE entity type for every dog that lives there.
 * Which dog it is, is data ({@link SnuffelHond}): one of the generator's named residents (Dokter Pleisterpoot, Meester
 * Truffelneus...: breed, coat, accessories and puppy flag come with the name) or any breed + coat + puppy flag. It stays
 * where it is put, looks at whoever comes near and holds its pose; a right-click goes to its {@link Bewoners.Rol role}.
 * Everything a resident tells or gives is per player: the entity itself is the same for everybody.
 * <p>
 * The residents of the island are not in a template: {@link Eiland#bewoon} keeps one of each on its spot. Anywhere else
 * (the captain at a dock in the Guhmensie) one is placed with {@link Bewoners#plaats}.
 */
public class BewonerEntity extends SnuffelHond {
    public BewonerEntity(EntityType<? extends BewonerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /** The key its role is looked up with: its spot on the island (eiland.json "sleutel"), else the resident's name id. */
    public String sleutel() {
        String plek = getPersistentData().getStringOr(Eiland.TAG_SLEUTEL, "");
        return plek.isEmpty() ? bewoner() : plek;
    }

    /** Is this still the dog that the island's data wants on this spot? */
    boolean klopt(Eiland.BewonerPlek b) {
        if (!b.bewoner().isEmpty()) {
            return b.bewoner().equals(bewoner());
        }
        return bewoner().isEmpty() && b.ras().equals(ras()) && b.kleur().equals(kleur()) && b.pup() == pup();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            Bewoners.klik(this, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 10 == 0) {
            Bewoners.Rol rol = Bewoners.rol(sleutel());
            if (rol != null) {
                rol.tick(this);
            }
        }
    }
}
