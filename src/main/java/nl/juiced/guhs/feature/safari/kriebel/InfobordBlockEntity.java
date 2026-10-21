package nl.juiced.guhs.feature.safari.kriebel;

import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.safari.quests.SafariDialoog;

/**
 * The info board's species ("Soort": a soorten.json id, set by the park generator, {@code kit.infobord_nbt}). Reading it
 * opens one page in the talking screen: the species' name above it and its Guhdex text
 * ({@code gui.guhs.safari.infobord.<soort>}; a species without one gets the general text with its name). No species: the
 * board says it is waiting for an animal.
 */
public class InfobordBlockEntity extends BlockEntity {
    private String soort = "";

    public InfobordBlockEntity(BlockPos pos, BlockState state) {
        super(KriebelBlokken.INFOBORD_BE.get(), pos, state);
    }

    public String soort() {
        return soort;
    }

    public void zetSoort(String soort) {
        this.soort = soort == null ? "" : soort;
        setChanged();
    }

    /** The text key this board shows (the species' own text, else the general one, else "no animal yet"). */
    public String tekstKey() {
        if (soort.isEmpty()) {
            return "gui.guhs.safari.infobord.leeg";
        }
        String eigen = "gui.guhs.safari.infobord." + soort;
        return Language.getInstance().has(eigen) ? eigen : "gui.guhs.safari.infobord.algemeen";
    }

    void lees(ServerPlayer p) {
        if (soort.isEmpty()) {
            SafariDialoog.zeg(p, null, "block.guhs.infobord", tekstKey());
        } else {
            SafariDialoog.zeg(p, null, "entity.guhs.safari." + soort, tekstKey(), Component.translatable("entity.guhs.safari." + soort));
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        soort = in.getStringOr("Soort", "");
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("Soort", soort);
    }
}
