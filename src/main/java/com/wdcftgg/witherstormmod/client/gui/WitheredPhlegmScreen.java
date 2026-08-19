package com.wdcftgg.witherstormmod.client.gui;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.inventory.WitheredPhlegmContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class WitheredPhlegmScreen extends GuiContainer {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/container/withered_phlegm.png");

    private final IInventory phlegmInventory;

    public WitheredPhlegmScreen(WitheredPhlegmContainer container, IInventory phlegmInventory) {
        super(container);
        this.phlegmInventory = phlegmInventory;
        xSize = 176;
        ySize = 207;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(phlegmInventory.getDisplayName().getUnformattedText(), 8, 6, 4210752);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 114, 4210752);

        int xp = ((WitheredPhlegmContainer) inventorySlots).getXp();
        if (xp > 0) {
            fontRenderer.drawString("XP:", 8, 83, 4210752);
            String value = Integer.toString(xp);
            fontRenderer.drawString(value, 9, 94, 0);
            fontRenderer.drawString(value, 7, 94, 0);
            fontRenderer.drawString(value, 8, 95, 0);
            fontRenderer.drawString(value, 8, 93, 0);
            fontRenderer.drawString(value, 8, 94, 8453920);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(BACKGROUND);
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        drawTexturedModalRect(left, top, 0, 0, xSize, ySize);
    }

    /** Keep the container's item tooltip path explicit for custom 25-slot GUIs. */
    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        Slot slot = getSlotUnderMouse();
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            renderToolTip(stack, mouseX, mouseY);
        }
    }
}
