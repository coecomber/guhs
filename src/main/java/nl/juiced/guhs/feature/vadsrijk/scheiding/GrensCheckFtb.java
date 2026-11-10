package nl.juiced.guhs.feature.vadsrijk.scheiding;

import javax.annotation.Nullable;

import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.ftb.FtbClaimRegel;
import nl.juiced.guhs.feature.vadsrijk.vitrine.Vitrine;

/**
 * Y-INT (dev only): the FTB Quests half of {@link GrensCheck}, in its own class so it only loads when FTB Quests is installed.
 * It claims the vitrine reward of "Word rekruut van De Knuffelorde" through {@code TeamData.claimReward} (where every FTB claim
 * ends, the target of {@code mixin.FtbClaimMixin}): refused inside, works at home.
 */
final class GrensCheckFtb {
    private GrensCheckFtb() {
    }

    @Nullable
    static ItemReward vitrineBeloning() {
        ServerQuestFile f = ServerQuestFile.getInstance();
        if (f == null) {
            return null;
        }
        for (ItemReward r : f.collect(ItemReward.class)) {
            if (r.getItem().is(Vitrine.ITEM.get())) {
                return r;
            }
        }
        return null;
    }

    static void binnen(ServerPlayer p) {
        ItemReward r = vitrineBeloning();
        TeamData d = r == null ? null : ServerQuestFile.getInstance().getTeamData(p).orElse(null);
        if (!GrensCheck.check(p, r != null && d != null, "FTB: the vitrine reward of the rekruut quest exists")) {
            return;
        }
        int voor = FtbClaimRegel.geweigerd, items = p.getInventory().countItem(Vitrine.ITEM.get());
        boolean al = d.isRewardClaimed(p.getUUID(), r);
        d.claimReward(p, (Reward) r, true, System.currentTimeMillis());
        GrensCheck.check(p, !al && FtbClaimRegel.geweigerd == voor + 1 && !d.isRewardClaimed(p.getUUID(), r)
                && p.getInventory().countItem(Vitrine.ITEM.get()) == items, "FTB: claiming the vitrine reward inside is refused (not marked claimed, no item)");
    }

    static void thuis(ServerPlayer p) {
        ItemReward r = vitrineBeloning();
        TeamData d = r == null ? null : ServerQuestFile.getInstance().getTeamData(p).orElse(null);
        if (r == null || d == null) {
            GrensCheck.check(p, false, "FTB: the vitrine reward exists");
            return;
        }
        int items = p.getInventory().countItem(Vitrine.ITEM.get());
        d.claimReward(p, (Reward) r, true, System.currentTimeMillis());
        GrensCheck.check(p, d.isRewardClaimed(p.getUUID(), r) && p.getInventory().countItem(Vitrine.ITEM.get()) == items + 1,
                "FTB: claiming the vitrine reward at home works (claimed, the vitrine is in the inventory)");
    }
}
