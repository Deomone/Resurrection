package com.ankh.client.render;

import com.ankh.AnkhResurrection;
import com.ankh.entity.LampEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LampEntityRenderer extends EntityRenderer<LampEntity> {

    private static final Identifier TEXTURE = AnkhResurrection.id("textures/entity/lamp_post.png");

    public LampEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(LampEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(LampEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void emit(VertexConsumer vc, MatrixStack.Entry entry, float[] pos, float[] uv,
                             int posIdx, int uvCorner, int light, int overlay) {
        float x = pos[posIdx * 3];
        float y = pos[posIdx * 3 + 1];
        float z = pos[posIdx * 3 + 2];
        float u = uv[uvCorner * 2];
        float v = uv[uvCorner * 2 + 1];
        vc.vertex(entry, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(entry, 0.0f, 1.0f, 0.0f);
    }
}
