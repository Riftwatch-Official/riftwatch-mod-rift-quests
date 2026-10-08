package net.riftwatch.rift_quests.network;

public enum QuestState {
    LOCKED,
    AVAILABLE,
    IN_PROGRESS,
    COMPLETE,
    CLAIMED;

    public boolean done() {
        return this == COMPLETE || this == CLAIMED;
    }
}
