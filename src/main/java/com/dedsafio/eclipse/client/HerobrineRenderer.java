package com.dedsafio.eclipse.client;

import com.dedsafio.eclipse.DedsafioEclipse;
import com.dedsafio.eclipse.entity.HerobrineEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HerobrineRenderer extends HumanoidMobRenderer<HerobrineEntity, PlayerModel<HerobrineEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(DedsafioEclipse.MOD_ID, "textures/entity/herobrine.png");

    public HerobrineRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HerobrineEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(HerobrineEntity entity) {
        return TEXTURE;
    }
}
