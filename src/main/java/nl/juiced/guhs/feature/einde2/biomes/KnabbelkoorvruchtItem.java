package nl.juiced.guhs.feature.einde2.biomes;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.mikas.Kaasmijten;
import nl.juiced.guhs.feature.gids.GidsFeature;

/**
 * einde2, slice biomes: the knabbelkoorvrucht. Eating one flashes you a few blocks away (a chorus fruit with a shorter
 * hop: {@link #BEREIK} blocks across instead of sixteen), and now and then a Kaasmijt pops out of it where you stood
 * (the mikas slice decides: {@link Kaasmijten#uitVrucht}).
 */
public class KnabbelkoorvruchtItem extends Item {
    /** The size of the box around the eater in which the fruit looks for a landing spot. */
    public static final double BEREIK = 9.0;

    public KnabbelkoorvruchtItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel server) {
            flits(server, entity);
        }
        return rest;
    }

    /** The flash on its own: hops the eater to a safe spot nearby. Returns whether it moved. */
    public static boolean flits(ServerLevel level, LivingEntity eter) {
        Vec3 van = eter.position();
        boolean weg = false;
        for (int poging = 0; poging < 16 && !weg; poging++) {
            double x = eter.getX() + (eter.getRandom().nextDouble() - 0.5) * BEREIK;
            double y = Mth.clamp(eter.getY() + (eter.getRandom().nextDouble() - 0.5) * BEREIK, level.getMinY(), level.getMinY() + level.getLogicalHeight() - 1);
            double z = eter.getZ() + (eter.getRandom().nextDouble() - 0.5) * BEREIK;
            if (eter.isPassenger()) {
                eter.stopRiding();
            }
            weg = eter.randomTeleport(x, y, z, true);
        }
        if (weg) {
            level.gameEvent(GameEvent.TELEPORT, van, GameEvent.Context.of(eter));
            level.playSound(null, eter.getX(), eter.getY(), eter.getZ(), E2BiomesFeature.VRUCHT_FLITS.get(), SoundSource.PLAYERS, 0.8f,
                    0.9f + eter.getRandom().nextFloat() * 0.2f);
            eter.resetFallDistance();
            eter.resetCurrentImpulseContext();
            if (eter instanceof ServerPlayer speler) {
                GidsFeature.grant(speler, "guheinde/einde2_biomes_flits");
            }
        }
        // (the mite pops out where the fruit was eaten, also when the flash found no room)
        Kaasmijten.uitVrucht(level, van, eter instanceof ServerPlayer speler ? speler : null);
        return weg;
    }
}
