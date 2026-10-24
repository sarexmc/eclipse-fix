package com.dedsafio.eclipse.client;

import com.dedsafio.eclipse.DedsafioEclipse;
import com.dedsafio.eclipse.entity.HerobrineEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Ojos blancos que brillan en la oscuridad. */
public class HerobrineEyesLayer extends EyesLayer<HerobrineEntity, PlayerModel<HerobrineEntity>> {
    private static final RenderType EYES = RenderType.eyes(
            new ResourceLocation(DedsafioEclipse.MOD_ID, "textures/entity/herobrine_eyes.png"));

    public HerobrineEyesLayer(RenderLayerParent<HerobrineEntity, PlayerModel<HerobrineEntity>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
