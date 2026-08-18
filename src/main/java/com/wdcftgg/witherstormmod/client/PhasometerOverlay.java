package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.item.PhasometerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public final class PhasometerOverlay {

    private static final ResourceLocation SCOPE = new ResourceLocation(
            Tags.MOD_ID, "textures/misc/phasometer_scope.png");
    private static final int PACKET_TICK_TOLERANCE = 20;
    private static final float SPYGLASS_FOV_SCALE = 0.1F;
    private static final float INITIAL_SCOPE_SCALE = 0.5F;
    private static final float TARGET_SCOPE_SCALE = 1.125F;
    private static final float SCOPE_SCALE_SPEED = 0.5F;
    private static NBTTagCompound observation = new NBTTagCompound();
    private static World sessionWorld;
    private static EnumHand sessionHand;
    private static int previousUseTicks;
    private static int observationAge;
    private static boolean wasScoping;
    private static float previousScopeScale = INITIAL_SCOPE_SCALE;
    private static float scopeScale = INITIAL_SCOPE_SCALE;
    private static float previousFovScale = 1.0F;
    private static float fovScale = 1.0F;

    private PhasometerOverlay() {
    }

    public static boolean isScoping(EntityPlayer player) {
        return player != null && player.isHandActive()
                && player.getActiveItemStack().getItem() instanceof PhasometerItem;
    }

    public static boolean isFirstPersonScoping(Minecraft minecraft) {
        return minecraft != null && minecraft.gameSettings.thirdPersonView == 0
                && isScoping(minecraft.player);
    }

    public static float getFovScale(float partialTicks) {
        return previousFovScale + (fovScale - previousFovScale)
                * MathHelper.clamp(partialTicks, 0.0F, 1.0F);
    }

    static float updateScopeScale(float currentScale, boolean firstPersonScoping) {
        return firstPersonScoping
                ? currentScale + (TARGET_SCOPE_SCALE - currentScale) * SCOPE_SCALE_SPEED
                : INITIAL_SCOPE_SCALE;
    }

    static float updateFovScale(float currentScale, boolean firstPersonScoping) {
        float target = firstPersonScoping ? SPYGLASS_FOV_SCALE : 1.0F;
        return MathHelper.clamp(currentScale + (target - currentScale) * 0.5F,
                SPYGLASS_FOV_SCALE, 1.5F);
    }

    static int calculateScopeSize(int screenWidth, int screenHeight, float animatedScale) {
        float baseSize = Math.min(screenWidth, screenHeight);
        float fitScale = Math.min((float) screenWidth / baseSize,
                (float) screenHeight / baseSize);
        return MathHelper.ceil(baseSize * fitScale * animatedScale);
    }

    public static float getScopeScale(float partialTicks) {
        return previousScopeScale + (scopeScale - previousScopeScale)
                * MathHelper.clamp(partialTicks, 0.0F, 1.0F);
    }

    public static void tick(Minecraft minecraft) {
        EntityPlayer player = minecraft == null ? null : minecraft.player;
        boolean scoping = isScoping(player);
        boolean firstPersonScoping = isFirstPersonScoping(minecraft);
        previousFovScale = fovScale;
        fovScale = updateFovScale(fovScale, firstPersonScoping);
        previousScopeScale = scopeScale;
        scopeScale = updateScopeScale(scopeScale, firstPersonScoping);
        if (!firstPersonScoping) {
            previousScopeScale = INITIAL_SCOPE_SCALE;
        }
        if (!scoping) {
            clearSession();
            return;
        }

        EnumHand hand = player.getActiveHand();
        int useTicks = player.getItemInUseCount();
        if (!wasScoping || sessionWorld != player.world || sessionHand != hand
                || useTicks > previousUseTicks + 1) {
            observation = new NBTTagCompound();
            sessionWorld = player.world;
            sessionHand = hand;
        }
        previousUseTicks = useTicks;
        if (observationAge < Integer.MAX_VALUE) observationAge++;
        if (observationAge > PACKET_TICK_TOLERANCE) observation = new NBTTagCompound();
        wasScoping = true;
    }

    public static void acceptObservation(EnumHand hand, int dimension,
                                         int remainingUseTicks,
                                         NBTTagCompound data) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (!isScoping(player) || player.world == null || player.dimension != dimension
                || player.getActiveHand() != hand
                || Math.abs(player.getItemInUseCount() - remainingUseTicks)
                > PACKET_TICK_TOLERANCE) {
            return;
        }
        sessionWorld = player.world;
        sessionHand = hand;
        previousUseTicks = player.getItemInUseCount();
        observationAge = 0;
        wasScoping = true;
        observation = data == null ? new NBTTagCompound() : data.copy();
    }

    static int searchDotCount(int ticksExisted) {
        return Math.floorMod(ticksExisted / 5, 4);
    }

    public static void render(Minecraft minecraft, ScaledResolution resolution,
                              float partialTicks) {
        EntityPlayer player = minecraft.player;
        // F1 隐藏界面时与上游一样不绘制望远镜遮罩，避免遮罩状态影响世界渲染。
        if (!isFirstPersonScoping(minecraft) || minecraft.gameSettings.hideGUI) return;

        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        int size = calculateScopeSize(width, height, getScopeScale(partialTicks));
        int left = (width - size) / 2;
        int top = (height - size) / 2;

        try {
            // The upstream scope has a deliberately low-alpha center. Alpha testing
            // would discard that center and make the whole view appear black.
            GlStateManager.disableAlpha();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            enableScopeBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.drawRect(0, 0, width, top, 0xFF000000);
            Gui.drawRect(0, top + size, width, height, 0xFF000000);
            Gui.drawRect(0, top, left, top + size, 0xFF000000);
            Gui.drawRect(left + size, top, width, top + size, 0xFF000000);

            // Gui.drawRect restores alpha testing and disables blending internally.
            // It also leaves the current color set to black, so restore white before
            // the textured pass or the complete scope is multiplied by black.
            GlStateManager.disableAlpha();
            enableScopeBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            minecraft.getTextureManager().bindTexture(SCOPE);
            Gui.drawScaledCustomSizeModalRect(left, top, 0.0F, 0.0F,
                    256, 256, size, size, 256.0F, 256.0F);

            NBTTagCompound tag = observation;
            int line = 0;
            if (tag != null && tag.hasKey(PhasometerItem.DataEntry.PHASE.tagName)) {
                for (PhasometerItem.DataEntry entry : PhasometerItem.getEntries(tag)) {
                    drawCentered(minecraft, entry.getDisplayText(tag), width,
                            30 + line * (minecraft.fontRenderer.FONT_HEIGHT + 2));
                    line++;
                }
            } else if (tag != null && tag.hasKey(PhasometerItem.DataEntry.OBSTRUCTED.tagName)) {
                drawCentered(minecraft,
                        PhasometerItem.DataEntry.OBSTRUCTED.getDisplayText(tag), width, 30);
            } else {
                StringBuilder dots = new StringBuilder();
                for (int index = 0; index < searchDotCount(player.ticksExisted); index++) {
                    dots.append('.');
                }
                drawCentered(minecraft, TextFormatting.GRAY
                        + I18n.format("description.phasometer.searching", dots.toString()), width, 30);
            }
        } finally {
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void enableScopeBlend() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
    }

    private static void drawCentered(Minecraft minecraft, String text, int screenWidth, int y) {
        minecraft.fontRenderer.drawString(text,
                (screenWidth - minecraft.fontRenderer.getStringWidth(text)) / 2,
                y, 0xFFFFFFFF);
    }

    private static void clearSession() {
        observation = new NBTTagCompound();
        sessionWorld = null;
        sessionHand = null;
        previousUseTicks = 0;
        observationAge = 0;
        wasScoping = false;
    }
}
