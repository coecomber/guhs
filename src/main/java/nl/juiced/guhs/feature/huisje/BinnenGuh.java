package nl.juiced.guhs.feature.huisje;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.guhpixel.grap2.StandInGuh;

/**
 * 1.3.2: the stand-in of a resident asleep in its bed inside a Guhhuisje (entity type guhs:huisje_slaper). It only looks
 * like the real guh (variant, hair, size, name) and sleeps: nobody's guh, never fed, dressed, hurt, pushed or saved (all of
 * that is {@link StandInGuh}), made when somebody comes into the room and removed when the last one leaves. The real guh
 * stays where it is; a click here goes to {@link BinnenInrichting#guhKlik} (the owner tucks it in or pets it softly).
 */
public class BinnenGuh extends StandInGuh {
    /** The band id of the resident it stands in for. */
    @Nullable
    private UUID bewoner;

    public BinnenGuh(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Nullable
    public UUID bewoner() {
        return bewoner;
    }

    void bewoner(UUID id) {
        this.bewoner = id;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide() && player instanceof ServerPlayer sp) {
            BinnenInrichting.guhKlik(sp, this);
        }
        return InteractionResult.SUCCESS;
    }
}
