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
    /** 1.8.0 (vadskracht): the Wolkenantennes that went in by right-click (0..3). One on top counts too ({@link #niveau}). */
    private int antenne;

    public WeerVadserBlockEntity(BlockPos pos, BlockState state) {
        super(WeerFeature.WEER_VADSER_BE.get(), pos, state, WeerFeature.KRACHT, 0);
    }

    public Weertje weertje() {
        return weertje;
    }

    /** The Wolkenantenne level that counts: what went in, or an antenna on top of it or next to it (the highest). */
    public int niveau() {
        return WeerZones.antenneNiveau(level, worldPosition, antenne);
    }

    /** The radius of its zone now (10 / 20 / 35 / 60): {@link WeerZones#straalVan}. */
    public int straal() {
        return WeerZones.straalVan(level, worldPosition, antenne);
    }

    /** What it asks while it makes a weather (more with a bigger antenna). */
    public int kosten() {
        return WeerFeature.KRACHTEN[Math.max(0, Math.min(3, niveau()))];
    }

    /** (tests, commands) Sets the antennas that went in. */
    public void zetAntenne(int niveau) {
        antenne = Math.max(0, Math.min(3, niveau));
        sync();
        meld();
    }

    /** A player puts a Wolkenantenne in: only the next one (I first, then II, then III). */
    public void antenneErin(ServerPlayer player, int niveau, net.minecraft.world.item.ItemStack stack) {
        if (niveau <= antenne) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.weer.antenne.al", niveau).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (niveau != antenne + 1) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.weer.antenne.eerst", antenne + 1).withStyle(ChatFormatting.GOLD));
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        zetAntenne(niveau);
        ServerLevel level = player.level();
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1.2f);
        level.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.3, worldPosition.getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0.02);
        player.sendOverlayMessage(Component.translatable("gui.guhs.weer.antenne.erin", niveau, straal(), kosten()).withStyle(ChatFormatting.AQUA));
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
                : heeftKracht() ? Component.translatable("gui.guhs.weer.gekozen", Component.translatable(w.key()), straal())
                : Component.translatable("gui.guhs.weer.gekozen_slaapt", Component.translatable(w.key()), kosten()));
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
        ModNetworking.sendTo(player, new WeerPayloads.Open(worldPosition, weertje.ordinal(), heeftKracht(), WeerUnlocks.ontgrendeldeIds(player),
                straal(), kosten()));
    }

    /** 1.8.0: a bigger antenna asks more ({@link #kosten}). */
    @Override
    public int vadsVraag() {
        return aan() ? kosten() : 0;
    }

    /** The antennas that went in come out again when it is broken. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            for (int i = 0; i < antenne && i < WeerFeature.ANTENNE_ITEMS.size(); i++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new net.minecraft.world.item.ItemStack(WeerFeature.ANTENNE_ITEMS.get(i).get()));
            }
        }
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
            WeerZones.houdVast(sl, worldPosition, weertje, straal());
        }
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.weer.uitlezing", Component.translatable(weertje.key())).withStyle(ChatFormatting.GRAY));
        regels.accept(Component.translatable("gui.guhs.weer.uitlezing.straal", straal(), niveau()).withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putString("Weertje", weertje.id());
        uit.putInt("Antenne", antenne);
    }

    @Override
    protected void laden(ValueInput in) {
        Weertje w = Weertje.byId(in.getStringOr("Weertje", "normaal"));
        weertje = w == null ? Weertje.NORMAAL : w;
        antenne = in.getIntOr("Antenne", 0);
    }
}
