package mathax.legacy.client.mixin.sodium;

import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.modules.render.WallHack;
import mathax.legacy.client.systems.modules.render.Xray;
import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadColorProvider;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumBlockRendererMixin {
    @Shadow @Final private BiomeColorBlender biomeColorBlender;

    @Inject(method = "renderQuad", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderQuad(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, ModelVertexSink vertices, IndexBufferBuilder indices, Vec3d blockOffset, ModelQuadColorProvider<BlockState> colorProvider, BakedQuad bakedQuad, QuadLightData light, ChunkModelBuilder model, CallbackInfo ci) {
        WallHack wallHack = Modules.get().get(WallHack.class);
        Xray xray = Modules.get().get(Xray.class);

        if (wallHack.isActive()) {
            if (wallHack.blocks.get().contains(state.getBlock())) {
                int alpha;

                if (xray.isActive()) {
                    alpha = xray.opacity.get();
                } else {
                    alpha = wallHack.opacity.get();
                }

                whRenderQuad(world, state, pos, origin, vertices, indices, blockOffset, colorProvider, bakedQuad, light, model, alpha);
                ci.cancel();
            }
        } else if(xray.isActive() && !wallHack.isActive() && xray.isBlocked(state.getBlock())) {
            whRenderQuad(world, state, pos, origin, vertices, indices, blockOffset, colorProvider, bakedQuad, light, model, xray.opacity.get());
            ci.cancel();
        }
    }

    private void whRenderQuad(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, ModelVertexSink vertices, IndexBufferBuilder indices, Vec3d blockOffset, ModelQuadColorProvider<BlockState> colorProvider, BakedQuad bakedQuad, QuadLightData light, ChunkModelBuilder model, int alpha) {
        ModelQuadView src = (ModelQuadView) bakedQuad;
        ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(light.br);

        int[] colors = null;

        if (bakedQuad.hasColor()) {
            colors = this.biomeColorBlender.getColors(world, pos, src, colorProvider, state);
        }

        int vertexStart = vertices.getVertexCount();

        for (int i = 0; i < 4; i++) {
            int j = orientation.getVertexIndex(i);

            float x = src.getX(j) + (float) blockOffset.getX();
            float y = src.getY(j) + (float) blockOffset.getY();
            float z = src.getZ(j) + (float) blockOffset.getZ();

            int color = ColorABGR.mul(colors != null ? colors[j] : 0xFFFFFFFF, light.br[j]);

            int blue = ColorABGR.unpackBlue(color);
            int green = ColorABGR.unpackGreen(color);
            int red = ColorABGR.unpackRed(color);

            color = ColorABGR.pack(red, green, blue, alpha);

            float u = src.getTexU(j);
            float v = src.getTexV(j);

            int lm = light.lm[j];

            vertices.writeVertex(origin, x, y, z, color, u, v, lm, model.getChunkId());
        }

        indices.add(vertexStart, ModelQuadWinding.CLOCKWISE);

        Sprite sprite = src.getSprite();

        if (sprite != null) {
            model.addSprite(sprite);
        }
    }
}
