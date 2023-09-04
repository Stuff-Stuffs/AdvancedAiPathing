package io.github.stuff_stuffs.advanced_ai.common.api.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class CollisionHelper {
    private static final VoxelShape FULL_CUBE = VoxelShapes.fullCube();
    private static final VoxelShape EMPTY = VoxelShapes.empty();
    private final Box box;
    private final Box floorBox;
    private final VoxelShape boxShape;
    private final VoxelShape floorShape;

    public CollisionHelper.FloorCollision open(final double xOff, final double yOff, final double zOff, final ShapeCache world) {
        if (test(xOff, yOff, zOff, world)) {
            return FloorCollision.CLOSED;
        }
        return testFloor(xOff, yOff, zOff, world) ? FloorCollision.FLOOR : FloorCollision.OPEN;
    }

    private boolean testFloor(final double xOff, final double yOff, final double zOff, final ShapeCache world) {
        final int minX = MathHelper.floor(floorBox.minX + xOff);
        final int maxX = MathHelper.ceil(floorBox.maxX + xOff);
        final int minY = MathHelper.floor(floorBox.minY + yOff);
        final int maxY = MathHelper.ceil(floorBox.maxY + yOff);
        final int minZ = MathHelper.floor(floorBox.minZ + zOff);
        final int maxZ = MathHelper.ceil(floorBox.maxZ + zOff);
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    final VoxelShape voxelShape = world.getCollisionShape(x, y, z);
                    if (voxelShape != EMPTY && !voxelShape.isEmpty()) {
                        if (voxelShape == FULL_CUBE) {
                            final double xBox = (double) x - xOff;
                            final double yBox = (double) y - yOff;
                            final double zBox = (double) z - zOff;
                            if (floorBox.intersects(xBox, yBox, zBox, xBox + 1.0D, yBox + 1.0D, zBox + 1.0D)) {
                                return true;
                            }
                        } else if (VoxelShapes.matchesAnywhere(floorShape, voxelShape.offset(-x, -y, -z), BooleanBiFunction.AND)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean test(final double xOff, final double yOff, final double zOff, final ShapeCache world) {
        final Box box = this.box;
        final VoxelShape shape = boxShape;
        final int minX = MathHelper.floor(box.minX + xOff);
        final int maxX = MathHelper.ceil(box.maxX + xOff);
        final int minY = MathHelper.floor(box.minY + yOff) - 1;
        final int maxY = MathHelper.ceil(box.maxY + yOff);
        final int minZ = MathHelper.floor(box.minZ + zOff);
        final int maxZ = MathHelper.ceil(box.maxZ + zOff);

        for (int y = minY; y <= maxY; ++y) {
            final int yEdge = y == minY | y == maxY ? 1 : 0;

            for (int x = minX; x <= maxX; ++x) {
                final int xEdge = x == minX | x == maxX ? 1 : 0;

                for (int z = minZ; z <= maxZ; ++z) {
                    final int count = (z == minZ | z == maxZ ? 1 : 0) + xEdge + yEdge;
                    if (count != 3) {
                        final BlockState state = world.getBlockState(x, y, z);
                        if (!state.isAir() && (count != 1 || state.exceedsCube()) && (count != 2 || state.getBlock() == Blocks.MOVING_PISTON)) {
                            final VoxelShape voxelShape = world.getCollisionShape(x, y, z);
                            if (voxelShape != EMPTY && !voxelShape.isEmpty()) {
                                if (voxelShape == FULL_CUBE) {
                                    final double xBox = (double) x - xOff;
                                    final double yBox = (double) y - yOff;
                                    final double zBox = (double) z - zOff;
                                    if (box.intersects(xBox, yBox, zBox, xBox + 1.0D, yBox + 1.0D, zBox + 1.0D)) {
                                        return true;
                                    }
                                } else {
                                    if (VoxelShapes.matchesAnywhere(shape, voxelShape, BooleanBiFunction.AND)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public CollisionHelper(final double width, final double height, final double floor) {
        box = new Box(0, 0, 0, width, height, width);
        floorBox = new Box(0, -floor, 0, width, 0, width);
        boxShape = VoxelShapes.cuboid(box);
        floorShape = VoxelShapes.cuboid(floorBox);
    }

    public enum FloorCollision {
        OPEN,
        FLOOR,
        CLOSED;

        FloorCollision() {
        }
    }
}
