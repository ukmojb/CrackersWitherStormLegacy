package com.wdcftgg.witherstormmod.client.jei;

import com.wdcftgg.witherstormmod.common.beacon.SuperBeaconRecipes;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;


public final class SuperBeaconSummoningRecipeWrapper extends SuperBeaconRecipeWrapper {
    private EntityLivingBase entityToRender;
    private World renderWorld;

    public SuperBeaconSummoningRecipeWrapper(SuperBeaconRecipes.Recipe recipe) {
        super(recipe);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight,
                         int mouseX, int mouseY) {
        EntityLivingBase entity = getEntityToRender(minecraft.world);
        if (entity != null) {
            int x = recipeWidth / 2;
            int y = SuperBeaconLayout.centerY(getRecipe().condition);
            y += (int) (entity.height * 10.0F);
            float angleX = x - mouseX;
            float angleY = y - mouseY - entity.getEyeHeight() * 20.0F;
            GlStateManager.pushMatrix();
            try {
                if (entity instanceof WitherStormEntity) {
                    GlStateManager.translate(80.0F, 60.0F, 0.0F);
                    GlStateManager.scale(0.1F, 0.1F, 0.1F);
                }
                GuiInventory.drawEntityOnScreen(x, y, 20, angleX, angleY, entity);
            } finally {
                GlStateManager.popMatrix();
            }
        }
        super.drawInfo(minecraft, recipeWidth, recipeHeight, mouseX, mouseY);
    }

    private EntityLivingBase getEntityToRender(World world) {
        if (world == null) return null;
        if (entityToRender != null && renderWorld == world) return entityToRender;
        renderWorld = world;
        entityToRender = null;
        Entity entity = EntityList.createEntityByIDFromName(
                new ResourceLocation(getRecipe().entity), world);
        if (entity instanceof EntityLivingBase) {
            entityToRender = (EntityLivingBase) entity;
            if (entityToRender instanceof WitherStormEntity) {
                ((WitherStormEntity) entityToRender).setPhase(4);
            }
        }
        return entityToRender;
    }
}
