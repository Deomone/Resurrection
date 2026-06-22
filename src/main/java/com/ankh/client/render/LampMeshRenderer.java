package com.ankh.client.render;

import com.ankh.AnkhResurrection;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class LampMeshRenderer {

    private LampMeshRenderer() {}

    public static final Identifier TEXTURE = AnkhResurrection.id("textures/entity/lamp_post.png");

    public static void draw(MatrixStack matrices, VertexConsumerProvider vcp, double dx, double dy, double dz) {
        matrices.push();
        matrices.translate(dx, dy, dz);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        final int fb = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        final int ov = OverlayTexture.DEFAULT_UV;

        final float[] pos = LampMeshData.POS;
        final int[] tri = LampMeshData.TRI;
        final float[] uv = LampMeshData.UV;
        for (int t = 0; t < LampMeshData.TRIANGLES; t++) {
            int ia = tri[t * 3], ib = tri[t * 3 + 1], ic = tri[t * 3 + 2];
            emit(vc, entry, pos, uv, ia, t * 3, fb, ov);
            emit(vc, entry, pos, uv, ib, t * 3 + 1, fb, ov);
            emit(vc, entry, pos, uv, ic, t * 3 + 2, fb, ov);
            emit(vc, entry, pos, uv, ic, t * 3 + 2, fb, ov);
        }
        matrices.pop();
    }

    private static void emit(VertexConsumer vc, MatrixStack.Entry entry, float[] pos, float[] uv,
                             int posIdx, int uvCorner, int light, int overlay) {
        vc.vertex(entry, pos[posIdx * 3], pos[posIdx * 3 + 1], pos[posIdx * 3 + 2])
                .color(255, 255, 255, 255)
                .texture(uv[uvCorner * 2], uv[uvCorner * 2 + 1])
                .overlay(overlay)
                .light(light)
                .normal(entry, 0.0f, 1.0f, 0.0f);
    }
}
