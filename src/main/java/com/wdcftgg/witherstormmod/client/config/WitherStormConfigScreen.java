package com.wdcftgg.witherstormmod.client.config;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.client.WitherStormClientEvents;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.input.Mouse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;





public final class WitherStormConfigScreen extends GuiScreen {

    private static final ResourceLocation PANORAMA = new ResourceLocation(Tags.MOD_ID,
            "textures/gui/title/background/panorama_0.png");
    private static final int CATEGORY_CLIENT = 0;
    private static final int CATEGORY_SERVER = 1;
    private static final int CATEGORY_PRESETS = 2;
    private static final int DONE = 3;
    private static final int REFRESH_SOUNDS = 4;
    private static final int PRESET_MEDIUM = 5;
    private static final int PRESET_LOW = 6;
    private static final int PRESET_ULTRA_LOW = 7;
    private static final int PRESET_PERFORMANCE = 8;
    private static final int PRESET_MASS_DESTRUCTION = 9;
    private static final int ROW_BASE_ID = 100;

    private final GuiScreen previous;
    private Category category = Category.CLIENT;
    private int scroll;
    private List<OptionEntry> options = new ArrayList<OptionEntry>();
    private final Map<Integer, OptionEntry> rowButtons = new LinkedHashMap<Integer, OptionEntry>();

    public WitherStormConfigScreen(GuiScreen previous) {
        this.previous = previous;
    }

    private enum Category {
        CLIENT(WitherStormClientConfig.class),
        SERVER(WitherStormConfig.class),
        PRESETS(null);

        private final Class<?> owner;

        Category(Class<?> owner) {
            this.owner = owner;
        }
    }

    @Override
    public void initGui() {
        buttonList.clear();
        rowButtons.clear();
        int left = 8;
        int top = 8;
        buttonList.add(new GuiButton(CATEGORY_CLIENT, left, top, 72, 20,
                I18n.format("gui.witherstormmod.config.category.client.title")));
        buttonList.add(new GuiButton(CATEGORY_SERVER, left + 76, top, 72, 20,
                I18n.format("gui.witherstormmod.config.category.server.title")));
        buttonList.add(new GuiButton(CATEGORY_PRESETS, left + 152, top, 72, 20,
                I18n.format("gui.witherstormmod.config.category.presets.title")));
        buttonList.add(new GuiButton(REFRESH_SOUNDS, width - 28, top, 20, 20, "S"));
        buttonList.add(new GuiButton(DONE, width / 2 - 50, height - 28, 100, 20,
                I18n.format("gui.done")));
        if (category == Category.PRESETS) {
            buildPresetButtons();
        } else {
            options = buildOptions(category.owner);
        }
        rebuildRowButtons();
    }

    private void buildPresetButtons() {
        int x = width / 2 - 110;
        int y = 60;
        buttonList.add(new GuiButton(PRESET_MEDIUM, x, y, 220, 20,
                I18n.format("config.witherstormmod.preset.client.medium.title")));
        buttonList.add(new GuiButton(PRESET_LOW, x, y + 26, 220, 20,
                I18n.format("config.witherstormmod.preset.client.low.title")));
        buttonList.add(new GuiButton(PRESET_ULTRA_LOW, x, y + 52, 220, 20,
                I18n.format("config.witherstormmod.preset.client.ultra_low.title")));
        buttonList.add(new GuiButton(PRESET_PERFORMANCE, x, y + 90, 220, 20,
                I18n.format("config.witherstormmod.preset.server.performance.title")));
        buttonList.add(new GuiButton(PRESET_MASS_DESTRUCTION, x, y + 116, 220, 20,
                I18n.format("config.witherstormmod.preset.server.mass_destruction.title")));
    }

    private void rebuildRowButtons() {
        List<GuiButton> keep = new ArrayList<GuiButton>();
        for (Object object : buttonList) {
            GuiButton button = (GuiButton) object;
            if (button.id < ROW_BASE_ID) keep.add(button);
        }
        buttonList.clear();
        buttonList.addAll(keep);
        rowButtons.clear();
        if (category == Category.PRESETS) return;

        int visible = Math.max(1, (height - 70) / 24);
        for (int index = scroll; index < options.size() && index < scroll + visible; index++) {
            OptionEntry entry = options.get(index);
            int y = 44 + (index - scroll) * 24;
            int decrementId = ROW_BASE_ID + index * 2;
            int incrementId = decrementId + 1;
            if (entry.type == OptionType.BOOLEAN) {
                GuiButton toggle = new GuiButton(incrementId, width - 118, y, 110, 20,
                        String.valueOf(entry.currentValue()));
                buttonList.add(toggle);
                rowButtons.put(incrementId, entry);
            } else if (entry.type != OptionType.LIST) {
                GuiButton decrement = new GuiButton(decrementId, width - 138, y, 20, 20, "-");
                GuiButton increment = new GuiButton(incrementId, width - 20, y, 20, 20, "+");
                buttonList.add(decrement);
                buttonList.add(increment);
                rowButtons.put(decrementId, entry);
                rowButtons.put(incrementId, entry);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case CATEGORY_CLIENT:
                category = Category.CLIENT;
                scroll = 0;
                initGui();
                return;
            case CATEGORY_SERVER:
                category = Category.SERVER;
                scroll = 0;
                initGui();
                return;
            case CATEGORY_PRESETS:
                category = Category.PRESETS;
                scroll = 0;
                initGui();
                return;
            case DONE:
                mc.displayGuiScreen(previous);
                return;
            case REFRESH_SOUNDS:
                WitherStormClientEvents.refreshAllLoopSounds();
                return;
            case PRESET_MEDIUM:
                applyClientPreset(false, false, false, false, true);
                return;
            case PRESET_LOW:
                applyClientPreset(false, false, false, true, false);
                return;
            case PRESET_ULTRA_LOW:
                applyClientPreset(false, false, true, false, false);
                return;
            case PRESET_PERFORMANCE:
                setServerField("squashHitbox", true);
                return;
            case PRESET_MASS_DESTRUCTION:
                setServerField("hunchbackClusterPickupInterval", 10);
                setServerField("clusterPickupInterval", 10);
                setServerField("devourerClusterPickupInterval", 10);
                setServerField("flamingSkullExplosionSize", 12.0D);
                setServerField("flamingSkullSpeedModifier", 4.0D);
                return;
            default:
                OptionEntry entry = rowButtons.get(button.id);
                if (entry == null) return;
                boolean increment = button.id % 2 == 1;
                applyStep(entry, increment ? 1 : -1);
        }
    }

    private void applyClientPreset(boolean debris, boolean particles, boolean lowRes,
                                   boolean lod, boolean blockClusters) {
        setClientField("renderDebrisCloud", debris);
        setClientField("tractorBeamParticles", particles);
        setClientField("lowResModels", lowRes);
        setClientField("witherStormLOD", lod);
        setClientField("blockClusterRendering", blockClusters);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int visible = Math.max(1, (height - 70) / 24);
        int maximum = Math.max(0, options.size() - visible);
        scroll = Math.max(0, Math.min(maximum, scroll - Integer.signum(wheel)));
        rebuildRowButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackground();
        drawCenteredString(fontRenderer, Tags.MOD_NAME, width / 2, 20, 0xFFFFFFFF);
        if (category == Category.PRESETS) {
            drawPresetDescriptions();
        } else {
            drawOptionRows(mouseX, mouseY);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBackground() {
        if (WitherStormClientConfig.customPanorama) {
            mc.getTextureManager().bindTexture(PANORAMA);
            Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height,
                    width, height);
        } else {
            drawBackground(0);
        }
        drawRect(0, 0, width, height, 0x99000000);
    }

    private void drawOptionRows(int mouseX, int mouseY) {
        int visible = Math.max(1, (height - 70) / 24);
        for (int index = scroll; index < options.size() && index < scroll + visible; index++) {
            OptionEntry entry = options.get(index);
            int y = 44 + (index - scroll) * 24;
            drawRect(4, y, width - 4, y + 22, 0x33000000);
            String title = I18n.format("gui.witherstormmod.config." + entry.key + ".title");
            fontRenderer.drawStringWithShadow(title, 10, y + 6, 0xFFFFFFFF);
            if (entry.type != OptionType.BOOLEAN) {
                String value;
                if (entry.type == OptionType.ENUM) {
                    value = ((Enum<?>) entry.currentValue()).name();
                } else if (entry.type == OptionType.LIST) {
                    value = String.join(", ", (String[]) entry.currentValue());
                } else {
                    value = String.valueOf(entry.currentValue());
                }
                drawCenteredString(fontRenderer, value, width - 79, y + 6, 0xFFFFFFA0);
            }
            if (mouseX >= 10 && mouseX <= width - 10 && mouseY >= y && mouseY <= y + 22) {
                String description = I18n.format(
                        "gui.witherstormmod.config." + entry.key + ".description");
                if (description != null && !description.startsWith("gui.")) {
                    drawHoveringText(fontRenderer.listFormattedStringToWidth(
                            description, width - 40), mouseX, mouseY);
                }
            }
        }
    }

    private void drawPresetDescriptions() {
        String[] keys = {
                "config.witherstormmod.preset.client.medium.description",
                "config.witherstormmod.preset.client.low.description",
                "config.witherstormmod.preset.client.ultra_low.description",
                "config.witherstormmod.preset.server.performance.description",
                "config.witherstormmod.preset.server.mass_destruction.description"
        };
        int y = 92;
        for (String key : keys) {
            String description = I18n.format(key);
            if (description != null && !description.startsWith("config.")) {
                drawString(fontRenderer, description, width / 2 - 110, y, 0xFFCCCCCC);
            }
            y += 26;
        }
    }

    private void applyStep(OptionEntry entry, int delta) {
        try {
            Object current = entry.currentValue();
            Object updated;
            switch (entry.type) {
                case BOOLEAN:
                    updated = !((Boolean) current);
                    break;
                case NUMBER:
                    if (entry.field.getType() == double.class || entry.field.getType() == float.class) {
                        double value = ((Number) current).doubleValue() + delta * 0.1D;
                        if (entry.minimum != null) value = Math.max((Double) entry.minimum, value);
                        if (entry.maximum != null) value = Math.min((Double) entry.maximum, value);
                        updated = entry.field.getType() == float.class ? (float) value : value;
                    } else {
                        int value = ((Number) current).intValue() + delta;
                        if (entry.minimum != null) value = Math.max(((Number) entry.minimum).intValue(), value);
                        if (entry.maximum != null) value = Math.min(((Number) entry.maximum).intValue(), value);
                        updated = value;
                    }
                    break;
                case ENUM:
                    Enum<?>[] constants = ((Enum<?>) current).getDeclaringClass().getEnumConstants();
                    int ordinal = (((Enum<?>) current).ordinal() + delta % constants.length
                            + constants.length) % constants.length;
                    updated = constants[ordinal];
                    break;
                default:
                    return;
            }
            entry.field.set(null, updated);
            persist(entry.owner, entry.key, updated);
            refreshCurrentValue(entry);
        } catch (IllegalAccessException exception) {
            WitherStormMod.LOGGER.error("Failed to apply config change for {}",
                    entry.key, exception);
        }
    }

    private void refreshCurrentValue(OptionEntry entry) {
        try {
            entry.cachedValue = entry.field.get(null);
        } catch (IllegalAccessException ignored) {
        }
        rebuildRowButtons();
    }

    private void setClientField(String key, Object value) {
        setField(WitherStormClientConfig.class, key, value);
    }

    private void setServerField(String key, Object value) {
        setField(WitherStormConfig.class, key, value);
    }

    private static void setField(Class<?> owner, String key, Object value) {
        try {
            for (Field field : owner.getDeclaredFields()) {
                Config.Name name = field.getAnnotation(Config.Name.class);
                if (name == null || !name.value().equals(key)) continue;
                field.setAccessible(true);
                field.set(null, value);
                persist(owner, key, value);
                return;
            }
        } catch (IllegalAccessException exception) {
            WitherStormMod.LOGGER.error("Failed to apply preset value for {}", key, exception);
        }
    }

    private static void persist(Class<?> owner, String key, Object value) {
        String configName = owner == WitherStormClientConfig.class
                ? "witherstormmod/client" : "witherstormmod/server";
        try {
            File file = new File(Loader.instance().getConfigDir(), configName + ".cfg");
            Configuration configuration = new Configuration(file);
            configuration.load();
            Property property = configuration.get("general", key, "");
            if (value instanceof Boolean) property.set((Boolean) value);
            else if (value instanceof Integer) property.set((Integer) value);
            else if (value instanceof Double) property.set((Double) value);
            else if (value instanceof Float) property.set(((Float) value).doubleValue());
            else if (value instanceof String) property.set((String) value);
            else if (value instanceof String[]) property.set((String[]) value);
            else if (value instanceof Enum) property.set(((Enum<?>) value).name());
            configuration.save();
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        } catch (Exception exception) {
            WitherStormMod.LOGGER.error("Failed to persist config {}={}", key, value, exception);
        }
    }

    private static List<OptionEntry> buildOptions(Class<?> owner) {
        List<OptionEntry> result = new ArrayList<OptionEntry>();
        for (Field field : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            Config.Name name = field.getAnnotation(Config.Name.class);
            if (name == null) continue;
            if ("aprilFools".equals(name.value()) && !WitherStormMod.isAprilFools()) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(null);
                OptionType type = value instanceof Boolean ? OptionType.BOOLEAN
                        : value instanceof Enum ? OptionType.ENUM
                        : value instanceof String[] ? OptionType.LIST
                        : value instanceof Number ? OptionType.NUMBER : OptionType.LIST;
                Object minimum = null;
                Object maximum = null;
                if (type == OptionType.NUMBER) {
                    Config.RangeInt rangeInt = field.getAnnotation(Config.RangeInt.class);
                    Config.RangeDouble rangeDouble = field.getAnnotation(Config.RangeDouble.class);
                    if (rangeInt != null) {
                        minimum = rangeInt.min();
                        maximum = rangeInt.max();
                    } else if (rangeDouble != null) {
                        minimum = rangeDouble.min();
                        maximum = rangeDouble.max();
                    }
                }
                result.add(new OptionEntry(owner, field, name.value(), value,
                        type, minimum, maximum));
            } catch (IllegalAccessException ignored) {
            }
        }
        return result;
    }

    private enum OptionType {
        BOOLEAN,
        NUMBER,
        ENUM,
        LIST
    }

    private static final class OptionEntry {
        private final Class<?> owner;
        private final Field field;
        private final String key;
        private final OptionType type;
        private final Object minimum;
        private final Object maximum;
        private Object cachedValue;

        private OptionEntry(Class<?> owner, Field field, String key, Object cachedValue,
                            OptionType type, Object minimum, Object maximum) {
            this.owner = owner;
            this.field = field;
            this.key = key;
            this.cachedValue = cachedValue;
            this.type = type;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        private Object currentValue() {
            return cachedValue;
        }
    }
}
