package nl.juiced.guhs.feature.weer;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The Weer vadser's block entity: a guh machine that asks {@link WeerFeature#KRACHT} vadskracht while it makes a weather
 * other than {@link Weertje#NORMAAL} (switched to normaal it asks nothing and sleeps). Every tick it has vadskracht it
 * keeps its zone ({@link WeerZones#houdVast}); without vadskracht the zone fades within {@link WeerZones#VERVAL} ticks and
 * the weather is normal again.
 */
public class WeerVadserBlockEntity extends MachineBlockEntity {
    private Weertje weertje = Weertje.NORMAAL;

    public WeerVadserBlockEntity(BlockPos pos, BlockState state) {
        super(WeerFeature.WEER_VADSER_BE.get(), pos, state, WeerFeature.KRACHT, 0);
    }

    public Weertje weertje() {
        return weertje;
    }

    /** A player chose a weather in the menu (a locked guh-weertje is refused). */
    public void kies(ServerPlayer player, Weertje w) {
        if (!WeerUnlocks.heeft(player, w)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.weer.op_slot", Component.translatable(w.key())).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        zet(w);
        ServerLevel level = player.level();
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.2f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                8, 0.4, 0.3, 0.4, 0);
        player.sendOverlayMessage(w == Weertje.NORMAAL ? Component.translatable("gui.guhs.weer.uitgezet")
                : heeftKracht() ? Component.translatable("gui.guhs.weer.gekozen", Component.translatable(w.key()))
                : Component.translatable("gui.guhs.weer.gekozen_slaapt", Component.translatable(w.key()), WeerFeature.KRACHT));
    }

    /** Sets the weather (also for tests); "normaal" lets the zone go at once. */
    public void zet(Weertje w) {
        weertje = w;
        if (w == Weertje.NORMAAL && level instanceof ServerLevel sl) {
            WeerZones.laatLos(sl, worldPosition);
        }
        sync();
    }

    /** Opens its menu for this player. */
    public void open(ServerPlayer player) {
        ModNetworking.sendTo(player, new WeerPayloads.Open(worldPosition, weertje.ordinal(), heeftKracht(), WeerUnlocks.ontgrendeldeIds(player)));
    }

    @Override
    protected boolean aan() {
        return weertje != Weertje.NORMAAL;
    }

    @Override
    protected boolean kanWerken() {
        return true;
    }

    @Override
    protected boolean isVol() {
        return false;
    }

    @Override
    protected void werk() {
        if (level instanceof ServerLevel sl) {
            WeerZones.houdVast(sl, worldPosition, weertje);
        }
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.weer.uitlezing", Component.translatable(weertje.key())).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putString("Weertje", weertje.id());
    }

    @Override
    protected void laden(ValueInput in) {
        Weertje w = Weertje.byId(in.getStringOr("Weertje", "normaal"));
        weertje = w == null ? Weertje.NORMAAL : w;
    }
}
