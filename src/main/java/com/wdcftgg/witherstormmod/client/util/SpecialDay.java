package com.wdcftgg.witherstormmod.client.util;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.function.BiFunction;




public enum SpecialDay {
    HALLOWEEN(new Color(135, 82, 28),
            (month, day) -> month == 10 && day == 31),
    CHRISTMAS((entity, partialTicks, head) -> {
        float tick = (entity.ticksExisted + head * 1000
                + entity.getEntityId() * 1000 + partialTicks) / 100.0F;
        float red = MathHelper.sqrt(Math.max(MathHelper.sin(tick), 0.0F));
        float green = MathHelper.sqrt(Math.max(
                MathHelper.sin(tick + (float) Math.PI), 0.0F));
        return new Color(red * 0.5F, green * 0.5F, 0.0F);
    }, (month, day) -> month == 12 && (day == 24 || day == 25));

    private final ColorGetter color;
    private final BiFunction<Integer, Integer, Boolean> shouldShow;

    SpecialDay(ColorGetter color, BiFunction<Integer, Integer, Boolean> shouldShow) {
        this.color = color;
        this.shouldShow = shouldShow;
    }

    SpecialDay(Color fixedColor, BiFunction<Integer, Integer, Boolean> shouldShow) {
        this((storm, partialTicks, head) -> fixedColor, shouldShow);
    }

    public Color getColor(Entity entity, float partialTicks, int head) {
        return color.getColor(entity, partialTicks, head);
    }

    public static SpecialDay getForCurrentDate() {
        LocalDate date = LocalDate.now();
        int month = date.get(ChronoField.MONTH_OF_YEAR);
        int day = date.get(ChronoField.DAY_OF_MONTH);
        for (SpecialDay specialDay : values()) {
            if (specialDay.shouldShow.apply(month, day)) return specialDay;
        }
        return null;
    }

    public static boolean isAprilFoolsDate() {
        return WitherStormMod.isAprilFools();
    }

    interface ColorGetter {
        Color getColor(Entity entity, float partialTicks, int head);
    }
}
