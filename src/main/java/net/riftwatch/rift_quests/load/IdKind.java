package net.riftwatch.rift_quests.load;

public enum IdKind {
    ITEM("item"),
    BLOCK("block"),
    ENTITY("entity type"),
    BIOME("biome"),
    STRUCTURE("structure"),
    DIMENSION("dimension"),
    ADVANCEMENT("advancement"),
    LOOT_TABLE("loot table"),
    RECIPE("recipe"),
    STAT_TYPE("statistic type"),
    CUSTOM_STAT("custom statistic");

    private final String label;

    IdKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean taggable() {
        return switch (this) {
            case ITEM, BLOCK, ENTITY, BIOME, STRUCTURE -> true;
            default -> false;
        };
    }
}
