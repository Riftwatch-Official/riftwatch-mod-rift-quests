package net.riftwatch.rift_quests.engine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.book.TeamRule;
import net.riftwatch.rift_quests.progress.PlayerQuestData;

public final class Teams {
    private Teams() {
    }

    public static List<ServerPlayer> nearby(ServerPlayer player, Quest quest) {
        TeamRule rule = quest.team().orElseThrow();
        double range = (double) rule.radius() * rule.radius();
        ServerBook book = ServerBook.current();
        List<ServerPlayer> team = new ArrayList<>();
        team.add(player);
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player || other.isSpectator() || other.distanceToSqr(player) > range) {
                continue;
            }
            PlayerQuestData data = QuestEngine.data(other);
            if (data.isCompleted(quest.id()) || QuestEngine.unlocked(book, data, quest)) {
                team.add(other);
            }
        }
        return team;
    }
}
