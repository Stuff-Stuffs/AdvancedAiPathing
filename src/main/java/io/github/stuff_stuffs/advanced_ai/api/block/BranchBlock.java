package io.github.stuff_stuffs.advanced_ai.api.block;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Map;

public class BranchBlock extends Block {
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final int MAX_THICKNESS = 8;
    public static final IntProperty THICKNESS = IntProperty.of("thickness", 1, MAX_THICKNESS);
    private static final Map<State, VoxelShape> SHAPES;

    public BranchBlock(final Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(final StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(UP, DOWN, EAST, WEST, NORTH, SOUTH);
        builder.add(THICKNESS);
    }

    public static State fromBlockState(final BlockState state) {
        return new State(state.get(UP), state.get(DOWN), state.get(EAST), state.get(WEST), state.get(NORTH), state.get(SOUTH), state.get(THICKNESS));
    }

    public record State(boolean up, boolean down, boolean east, boolean west, boolean north, boolean south,
                        int thickness) {
    }

    static {
        SHAPES = new Object2ReferenceOpenHashMap<>();
        for (int i = 1; i <= MAX_THICKNESS; i++) {
            final double rad = (i*0.5) / (double) (MAX_THICKNESS + 1);
            final VoxelShape center = VoxelShapes.cuboid(8 - rad, 8 - rad, 8 - rad, 8 + rad, 8 + rad, 8 + rad);
            for (int j = 0; j < 64; j++) {
                final State state = new State((j & 1) == 1, (j & 2) == 2, (j & 4) == 4, (j & 8) == 8, (j & 16) == 16, (j & 32) == 32, i);
                VoxelShape shape = center;
                if(state.up()) {
                    shape = VoxelShapes.combine(shape, VoxelShapes.cuboid(8-rad,8-rad,8-rad,8+rad,1,8+rad), BooleanBiFunction.OR);
                }
            }
        }
    }
}
