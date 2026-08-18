package com.wdcftgg.witherstormmod.client.gui;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.inventory.SuperBeaconContainer;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.tile.AbstractSuperBeaconTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SuperBeaconScreen extends GuiContainer {
    private static final ResourceLocation BORDER = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/container/super_beacon.png");
    private static final ResourceLocation BUTTONS = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/container/super_beacon_buttons.png");
    private static final ResourceLocation WINDOW = new ResourceLocation(
            Tags.MOD_ID, "textures/gui/container/super_beacon_window.png");

    private static final int WINDOW_X = 10;
    private static final int WINDOW_Y = 9;
    private static final int WINDOW_WIDTH = 210;
    private static final int WINDOW_HEIGHT = 116;
    private static final int HALF_WIDTH = 105;
    private static final int BUTTON_WIDTH = 22;
    private static final int BOTTOM_HEIGHT = 32;

    private final SuperBeaconContainer container;
    private final AbstractSuperBeaconTileEntity beacon;
    private final List<EffectButton> effectButtons = new ArrayList<EffectButton>();
    private final List<Potion> validEffects = new ArrayList<Potion>();
    private Potion candidate;
    private IconButton select;
    private IconButton unselect;
    private IconButton showArea;
    private GuiButton info;
    private GuiButton exitInfo;
    private boolean showingInfo;
    private int effectScroll;

    public SuperBeaconScreen(SuperBeaconContainer container, AbstractSuperBeaconTileEntity beacon) {
        super(container);
        this.container = container;
        this.beacon = beacon;
        this.validEffects.addAll(beacon.getValidEffects());
        xSize = 230;
        ySize = 157;
    }

    @Override
    public void initGui() {
        super.initGui();
        rebuildEffectButtons();

        int bottomY = guiTop + WINDOW_Y + WINDOW_HEIGHT + 5;
        int centerX = guiLeft + xSize / 2;
        select = addButton(new IconButton(1, centerX - 24, bottomY, 88));
        unselect = addButton(new IconButton(2, centerX + 2, bottomY, 110));
        info = addButton(new GuiButton(3, guiLeft + 10, bottomY + 1, 20, 20, "i"));
        showArea = addButton(new IconButton(4, guiLeft + xSize - 32, bottomY, 132));
        exitInfo = addButton(new GuiButton(5, guiLeft + xSize - 30, guiTop - 24,
                20, 20, TextFormatting.RED + "X"));
        layoutEffectButtons();
        refreshButtons();
    }

    public void setValidEffects(Set<Potion> effects) {
        validEffects.clear();
        validEffects.addAll(effects);
        validEffects.sort(Comparator.comparingInt(Potion::getIdFromPotion));
        if (candidate != null && !validEffects.contains(candidate)) candidate = null;
        if (select != null) {
            rebuildEffectButtons();
            layoutEffectButtons();
            refreshButtons();
        }
    }

    private void rebuildEffectButtons() {
        buttonList.removeAll(effectButtons);
        effectButtons.clear();
        validEffects.sort(Comparator.comparingInt(Potion::getIdFromPotion));
        for (int index = 0; index < validEffects.size(); index++) {
            EffectButton button = new EffectButton(100 + index, index, validEffects.get(index));
            effectButtons.add(button);
            buttonList.add(button);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        refreshButtons();
    }

    private void refreshButtons() {
        Potion primary = container.getPrimaryEffect();
        select.enabled = !showingInfo && candidate != null && candidate != primary
                && container.getCooldown() <= 0;
        unselect.enabled = !showingInfo && primary != null;
        info.enabled = !showingInfo;
        showArea.enabled = !showingInfo;
        showArea.overlayY = container.shouldShowArea() ? 132 : 154;
        exitInfo.visible = showingInfo;
        for (EffectButton button : effectButtons) {
            button.visible = !showingInfo && button.index >= effectScroll
                    && button.index < effectScroll + 4;
            button.selected = button.effect == candidate;
        }
    }

    private void layoutEffectButtons() {
        int maxScroll = Math.max(0, effectButtons.size() - 4);
        effectScroll = MathHelper.clamp(effectScroll, 0, maxScroll);
        for (EffectButton button : effectButtons) {
            button.x = guiLeft + WINDOW_X + HALF_WIDTH + 6;
            button.y = guiTop + WINDOW_Y + 24 + (button.index - effectScroll) * 22;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (showingInfo || effectButtons.size() <= 4) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int left = guiLeft + WINDOW_X + HALF_WIDTH;
        int top = guiTop + WINDOW_Y + 24;
        if (mouseX < left || mouseX >= left + HALF_WIDTH || mouseY < top || mouseY >= top + 88) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        effectScroll += wheel < 0 ? 1 : -1;
        layoutEffectButtons();
        refreshButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button instanceof EffectButton) {
            candidate = ((EffectButton) button).effect;
        } else if (button.id == 1 && candidate != null) {
            ModNetwork.setSuperBeaconEffect(Potion.getIdFromPotion(candidate));
            mc.player.closeScreen();
        } else if (button.id == 2) {
            ModNetwork.setSuperBeaconEffect(-1);
        } else if (button.id == 3) {
            showingInfo = true;
        } else if (button.id == 4) {
            ModNetwork.toggleSuperBeaconArea(!container.shouldShowArea());
            mc.player.closeScreen();
        } else if (button.id == 5) {
            showingInfo = false;
        }
        refreshButtons();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (showingInfo) {
            drawInfoPage();
            return;
        }
        drawCentered(I18n.format("container.witherstormmod.withered_beacon.selected"),
                WINDOW_X + HALF_WIDTH / 2, WINDOW_Y + 10, 0xFFFFFF);
        drawCentered(I18n.format("container.witherstormmod.withered_beacon.available_effects"),
                WINDOW_X + HALF_WIDTH + HALF_WIDTH / 2, WINDOW_Y + 10, 0xFFFFFF);
        String level = I18n.format("container.witherstormmod.withered_beacon.level",
                container.getLevel() > 0
                        ? I18n.format("enchantment.level." + container.getLevel()) : "");
        drawCentered(level, WINDOW_X + HALF_WIDTH / 2, WINDOW_Y + WINDOW_HEIGHT - 11, 0xFFFFFF);

        Potion primary = container.getPrimaryEffect();
        if (primary != null) {
            PotionIconRenderer.draw(primary, 35, 32, 3.0F);
            drawCentered(I18n.format(primary.getName()), WINDOW_X + HALF_WIDTH / 2,
                    86, 0xFFFFFF);
        }
    }

    private void drawInfoPage() {
        String title = beacon.hasCustomName()
                ? beacon.getNameForGui() : I18n.format(beacon.getNameForGui());
        drawCentered(title, xSize / 2, 14, 0xFFFFFF);
        List<String> lines = fontRenderer.listFormattedStringToWidth(
                I18n.format("withered_beacon.info"), WINDOW_WIDTH - 20);
        int y = 36;
        for (String line : lines) {
            fontRenderer.drawString(line, WINDOW_X + 10, y, 0xD0D0D0);
            y += fontRenderer.FONT_HEIGHT + 2;
        }
    }

    private void drawCentered(String value, int x, int y, int color) {
        fontRenderer.drawString(value, x - fontRenderer.getStringWidth(value) / 2, y, color);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(BORDER);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        mc.getTextureManager().bindTexture(WINDOW);
        drawTexturedModalRect(guiLeft + WINDOW_X, guiTop + WINDOW_Y,
                8, 7, WINDOW_WIDTH, WINDOW_HEIGHT);
        if (showingInfo) return;
        mc.getTextureManager().bindTexture(BORDER);
        drawTexturedModalRect(guiLeft, guiTop + 13, 116, 13, HALF_WIDTH, 20);
        if (effectButtons.size() > 4) {
            int barX = guiLeft + WINDOW_X + WINDOW_WIDTH - 5;
            int barY = guiTop + WINDOW_Y + 25;
            drawRect(barX, barY, barX + 2, barY + 86, 0xFF151515);
            int maxScroll = effectButtons.size() - 4;
            int thumbY = barY + effectScroll * 64 / maxScroll;
            drawRect(barX, thumbY, barX + 2, thumbY + 22, 0xFF888888);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!showingInfo) {
            for (EffectButton button : effectButtons) {
                if (button.isMouseOver()) {
                    drawHoveringText(I18n.format(button.effect.getName()), mouseX, mouseY);
                    return;
                }
            }
            if (select.isMouseOver() && container.getCooldown() > 0) {
                drawHoveringText(I18n.format("gui.witherstormmod.button.select.cooldown.description"),
                        mouseX, mouseY);
            } else if (showArea.isMouseOver()) {
                drawHoveringText(I18n.format("gui.witherstormmod.button.showArea.description"), mouseX, mouseY);
            }
        }
    }

    private class IconButton extends GuiButton {
        private int overlayY;

        private IconButton(int id, int x, int y, int overlayY) {
            super(id, x, y, BUTTON_WIDTH, BUTTON_WIDTH, "");
            this.overlayY = overlayY;
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            minecraft.getTextureManager().bindTexture(BUTTONS);
            int row = !enabled ? 0 : hovered ? 44 : 22;
            drawTexturedModalRect(x, y, 0, row, BUTTON_WIDTH, BUTTON_WIDTH);
            drawTexturedModalRect(x, y, 0, overlayY, BUTTON_WIDTH, BUTTON_WIDTH);
        }
    }

    private final class EffectButton extends GuiButton {
        private final int index;
        private final Potion effect;
        private boolean selected;

        private EffectButton(int id, int index, Potion effect) {
            super(id, 0, 0, 93, 20, "");
            this.index = index;
            this.effect = effect;
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            int fill = selected ? 0xFF6C789A : hovered ? 0xFF555555 : 0xFF292929;
            drawRect(x, y, x + width, y + height, fill);
            drawRect(x, y, x + width, y + 1, 0xFF888888);
            drawRect(x, y, x + 1, y + height, 0xFF888888);
            PotionIconRenderer.draw(effect, x + 1, y + 1, 1.0F);
            String name = fontRenderer.trimStringToWidth(I18n.format(effect.getName()), width - 23);
            fontRenderer.drawString(name, x + 22, y + 6, 0xFFFFFF);
        }
    }

    private static final class PotionIconRenderer {
        private static final ResourceLocation INVENTORY = new ResourceLocation(
                "textures/gui/container/inventory.png");

        private PotionIconRenderer() {
        }

        static void draw(Potion effect, int x, int y, float scale) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (!effect.hasStatusIcon()) {
                minecraft.getRenderItem().renderItemIntoGUI(new ItemStack(Items.POTIONITEM), x, y);
                return;
            }
            minecraft.getTextureManager().bindTexture(INVENTORY);
            int icon = effect.getStatusIconIndex();
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0.0F);
            GlStateManager.scale(scale, scale, 1.0F);
            Gui.drawModalRectWithCustomSizedTexture(0, 0, icon % 8 * 18,
                    198 + icon / 8 * 18, 18, 18, 256, 256);
            GlStateManager.popMatrix();
        }
    }
}
