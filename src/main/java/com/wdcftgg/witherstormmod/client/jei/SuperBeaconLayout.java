package com.wdcftgg.witherstormmod.client.jei;


final class SuperBeaconLayout {
    static final int WIDTH = 180;
    static final int HEIGHT = 120;
    static final int SLOT_SIZE = 18;
    private static final int INGREDIENT_RADIUS = 40;

    private SuperBeaconLayout() {
    }

    static int centerY(String condition) {
        return HEIGHT / 2 - (hasCondition(condition) ? 5 : 0);
    }

    static int inputX(int index, int totalSize) {
        if (totalSize <= 0) return WIDTH / 2 - 8;
        double angle = Math.toRadians(360.0D * index / totalSize);
        return (int) (Math.sin(angle) * INGREDIENT_RADIUS + WIDTH / 2.0D - 8.0D);
    }

    static int inputY(int index, int totalSize, int centerY) {
        if (totalSize <= 0) return centerY - 8;
        double angle = Math.toRadians(360.0D * index / totalSize);
        return (int) (Math.cos(angle) * INGREDIENT_RADIUS + centerY - 8.0D);
    }

    static boolean hasCondition(String condition) {
        return condition != null && !"none".equals(condition);
    }

    static boolean shouldShowSummoningEntity(String entityId) {
        return entityId != null && !"minecraft:pig".equals(entityId);
    }
}
