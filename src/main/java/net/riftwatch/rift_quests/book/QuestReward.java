package net.riftwatch.rift_quests.book;

import java.util.List;

public record QuestReward(String key, List<String> replaces, Reward reward) {
}
