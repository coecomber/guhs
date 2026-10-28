package nl.juiced.guhs.feature.fabriek;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.bio.dieren.WolkenschaapjeEntity;
import nl.juiced.guhs.feature.techmachine.MachineSoort;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;

/**
 * De Scheerstal (1.8.0, the endgame pluis source): a little stable with a shearing machine and a guh face. Every
 * {@link FabriekGetallen#SCHEERSTAL_TICKS} ticks it shears a fluffy kept wolkenschaapje within
 * {@link FabriekGetallen#SCHEERSTAL_STRAAL} blocks (fence them in around it) and keeps the wolkenpluis in its nine slots, for a
 * pipe or the Spinmachine.
 */
public class ScheerstalBlockEntity extends TechBlockEntity {
    public ScheerstalBlockEntity(BlockPos pos, BlockState state) {
        super(FabriekFeature.SCHEERSTAL_BE.get(), pos, state, FabriekGetallen.SCHEERSTAL, 9);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.SCHEERSTAL;
    }

    @Override
    public int duur() {
        return FabriekGetallen.SCHEERSTAL_TICKS;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return false;
    }

    /** The nearest fluffy kept wolkenschaapje in reach (null: none). */
    @Nullable
    WolkenschaapjeEntity schaapje() {
        if (level == null) {
            return null;
        }
        List<WolkenschaapjeEntity> l = level.getEntitiesOfClass(WolkenschaapjeEntity.class,
                new AABB(worldPosition).inflate(FabriekGetallen.SCHEERSTAL_STRAAL), s -> s.isAlive() && s.isGehouden() && !s.isGeschoren() && !s.isBaby());
        return l.stream().min((a, b) -> Double.compare(a.distanceToSqr(worldPosition.getCenter()), b.distanceToSqr(worldPosition.getCenter()))).orElse(null);
    }

    private boolean ruimte() {
        return pastAlles(List.of(new ItemStack(nl.juiced.guhs.feature.bio.Bio.item("wolkenpluis", net.minecraft.world.item.Items.WHITE_WOOL), 3)), 0, 8);
    }

    @Override
    protected boolean kanWerken() {
        return schaapje() != null && ruimte();
    }

    @Override
    protected boolean isVol() {
        return !ruimte();
    }

    @Override
    protected void werk() {
        if (++voortgang < duur() || !(level instanceof ServerLevel server)) {
            return;
        }
        voortgang = 0;
        WolkenschaapjeEntity s = schaapje();
        if (s == null) {
            return;
        }
        ItemStack pluis = s.scheer(null, server);
        if (!stopAlles(List.of(pluis), 0, 8)) {
            net.minecraft.world.Containers.dropItemStack(server, s.getX(), s.getY() + 0.5, s.getZ(), pluis);
        }
        FabriekFeature.beloon(this, "fabriek_scheerstal");
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (isVol()) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (schaapje() == null) {
            regels.accept(Component.translatable("gui.guhs.fabriek.scheerstal.geen_schaapje", FabriekGetallen.SCHEERSTAL_STRAAL).withStyle(ChatFormatting.GRAY));
        }
    }
}
