package net.riftwatch.rift_quests.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;

public record Quest(
        ResourceLocation id,
        ResourceLocation chapter,
        String file,
        Text title,
        Optional<Text> subtitle,
        Optional<Text> description,
        Icon icon,
        QuestSize size,
        int x,
        int y,
        List<ResourceLocation> dependencies,
        DependencyMode dependencyMode,
        Visibility visibility,
        List<QuestTask> tasks,
        List<QuestReward> rewards,
        boolean defaultRewards,
        Optional<TeamRule> team,
        Optional<List<QuestReward>> teamRepeatRewards,
        OptionalInt repeatCooldownMinutes,
        List<ResourceLocation> replaces) {

    public static final String DEFAULT_XP_KEY = "default_xp";
    public static final String DEFAULT_TICKETS_KEY = "default_tickets";

    public List<QuestReward> effectiveRewards() {
        List<QuestReward> all = new ArrayList<>(rewards);
        if (defaultRewards) {
            if (size.defaultXpLevels() > 0) {
                all.add(new QuestReward(DEFAULT_XP_KEY, List.of(), new Reward.Xp(size.defaultXpLevels())));
            }
            if (size.defaultTickets() > 0) {
                all.add(new QuestReward(DEFAULT_TICKETS_KEY, List.of(), new Reward.Ticket(size.defaultTickets())));
            }
        }
        return List.copyOf(all);
    }

    public List<QuestReward> effectiveTeamRepeatRewards() {
        if (teamRepeatRewards.isPresent()) {
            return teamRepeatRewards.get();
        }
        List<QuestReward> halved = new ArrayList<>();
        for (QuestReward reward : rewards) {
            if (reward.reward() instanceof Reward.Item item && item.count() >= 2) {
                halved.add(new QuestReward(reward.key(), reward.replaces(), new Reward.Item(item.item(), item.count() / 2)));
            }
        }
        return List.copyOf(halved);
    }

    public boolean checkmarkOnly() {
        return tasks.stream().allMatch(task -> task.task() instanceof Task.Checkmark);
    }

    public boolean givesTickets() {
        return effectiveRewards().stream().anyMatch(reward -> reward.reward().givesTickets());
    }
}
