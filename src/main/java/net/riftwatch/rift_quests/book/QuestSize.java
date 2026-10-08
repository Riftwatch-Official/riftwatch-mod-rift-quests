package net.riftwatch.rift_quests.book;

public enum QuestSize {
    SMALL(2, 0),
    MEDIUM(5, 1),
    LARGE(10, 3);

    private final int defaultXpLevels;
    private final int defaultTickets;

    QuestSize(int defaultXpLevels, int defaultTickets) {
        this.defaultXpLevels = defaultXpLevels;
        this.defaultTickets = defaultTickets;
    }

    public int defaultXpLevels() {
        return defaultXpLevels;
    }

    public int defaultTickets() {
        return defaultTickets;
    }
}
