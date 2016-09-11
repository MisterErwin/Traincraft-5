package si.meansoft.traincraft.track;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;
import si.meansoft.traincraft.blocks.BlockTrack;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by MisterErwin on 11.09.2016.
 * In case you need it, ask me ;)
 */
public class TrackPlanner {
    //The planner will try to get closer to the finish. It will start trying with the first entry here
    private final static BlockTrack.TrackTypes[] TRACK_TYPE_CHECK_ORDER = new BlockTrack.TrackTypes[]{
            BlockTrack.TrackTypes.STRAIGHT_ULTIMATE,
            BlockTrack.TrackTypes.STRAIGHT_EXTREME,
            BlockTrack.TrackTypes.STRAIGHT_LONG,
            BlockTrack.TrackTypes.STRAIGHT_MIDDLE,
            BlockTrack.TrackTypes.STRAIGHT_SHORT,
            BlockTrack.TrackTypes.STRAIGHT_SINGLE,
            BlockTrack.TrackTypes.TEST_CURVE,
            BlockTrack.TrackTypes.TEST_CURVE_SMALL
    };


    private final BlockPos start, finish;
    private BlockPos current;
    private final List<TrackPlannerSegment> list = new LinkedList<>();
    private EnumFacing currentFacing;

    public TrackPlanner(BlockPos start, BlockPos finish) {
        this.start = start;
        this.finish = finish;
        this.currentFacing = getFacing(start, finish);
        this.current = start.add(currentFacing.getOpposite().getDirectionVec());
        System.out.println("Facing==" + this.currentFacing.name());
    }

    public BlockPos getStart() {
        return start;
    }

    public Status doWork() {
        double curDist = current.distanceSq(finish), dist;
        double curDistCopy = curDist;
        BlockPos pos, bestPos = null;
        BlockTrack.TrackTypes bestType = null;
        Variant bestVariant = null;
        EnumFacing bestFacing = null;
        Pair<BlockPos, EnumFacing> rV;
        for (BlockTrack.TrackTypes trackType : TRACK_TYPE_CHECK_ORDER) {
            for (Variant variant : Variant.VARIANTS) {

                rV = variant.moveTo(trackType, currentFacing, current);
                if (rV == null) continue; //Variant is not applicable
                pos = rV.getKey();
                if (pos.equals(finish)) {
                    current = pos;
                    list.add(new TrackPlannerSegment(trackType, variant, finish, rV.getValue()));
                    return Status.SUCCESS;
                } else if (isOverTarget(pos)) {
                    continue;
                }
                dist = distanceSq(finish, pos);

                if (dist <= curDist) { //New best
                    curDist = dist;
                    bestPos = pos;
                    bestType = trackType;
                    bestVariant = variant;
                    bestFacing = rV.getRight();
                }
            }
        }
        if (bestPos == null)
            return Status.FAILURE;
        current = bestPos;
        list.add(new TrackPlannerSegment(bestType, bestVariant, bestPos, bestFacing));
        System.out.println(curDistCopy + "==>" + curDist);
        return Status.RUNNING;
    }

    /**
     * Checks if the given pos has gone further than the target
     *
     * @param pos the BlockPos to check
     * @return true if the given pos is "am Ziel vorbei"
     */
    private boolean isOverTarget(BlockPos pos) {
        if (pos.getZ() < finish.getZ() && current.getZ() > finish.getZ())
            return true;
        if (pos.getZ() > finish.getZ() && current.getZ() < finish.getZ())
            return true;
        if (pos.getX() < finish.getX() && current.getX() > finish.getX())
            return true;
        if (pos.getX() > finish.getX() && current.getX() < finish.getX())
            return true;
        if (pos.getY() < finish.getY() && current.getY() > finish.getY())
            return true;
        if (pos.getY() > finish.getY() && current.getY() < finish.getY())
            return true;
        return false;
    }


    public List<TrackPlannerSegment> getPath() {
        return list;
    }

    private double distanceSq(BlockPos a, BlockPos b) {
        return a.distanceSq(b.getX(), b.getY(), b.getZ());
    }

    private EnumFacing getFacing(BlockPos start, BlockPos finish) {
        start = finish.subtract(start);
        if (Math.abs(start.getZ()) >= Math.abs(start.getX())) {
            return start.getZ() <= 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
        } else
            return start.getX() <= 0 ? EnumFacing.WEST : EnumFacing.EAST;

    }

    public static class TrackPlannerSegment {
        private final BlockTrack.TrackTypes type;
        private final Variant variant;
        private final BlockPos end;
        private final EnumFacing facing;

        private TrackPlannerSegment(BlockTrack.TrackTypes type, Variant variant, BlockPos end, EnumFacing facing) {
            this.type = type;
            this.variant = variant;
            this.end = end;
            this.facing = facing;
        }

        public BlockTrack.TrackTypes getType() {
            return type;
        }

        public Variant getVariant() {
            return variant;
        }

        public BlockPos getEnd() {
            return end;
        }

        public EnumFacing getFacing() {
            return facing;
        }
    }

    public enum Status {
        SUCCESS, FAILURE, RUNNING
    }

    public enum Variant {
        DEFAULT_STRAIGHT() {
            @Override
            Pair<BlockPos, EnumFacing> moveTo(BlockTrack.TrackTypes trackType, EnumFacing facing, BlockPos current) {
                if (trackType.isCurve()) return null; //Curves will be handled below
                switch (facing) {
                    case NORTH:
                        return Pair.of(current.add(trackType.getGrid().getWidthX() - 1, 0, -trackType.getGrid().getWidthZ()), facing);
                    case SOUTH:
                        return Pair.of(current.add(-trackType.getGrid().getWidthX() + 1, 0, trackType.getGrid().getWidthZ()), facing);
                    case WEST:
                        return Pair.of(current.add(trackType.getGrid().getWidthZ(), 0, -trackType.getGrid().getWidthX() + 1), facing);
                    default: //EAST
                        return Pair.of(current.add(-trackType.getGrid().getWidthZ(), 0, trackType.getGrid().getWidthX() - 1), facing);

                }
            }
        },
        CURVE_RIGHT() {
            @Override
            Pair<BlockPos, EnumFacing> moveTo(BlockTrack.TrackTypes trackType, EnumFacing facing, BlockPos current) {
                if (!trackType.isCurve())return null;
                switch (facing) {
                    case NORTH:
                        return Pair.of(current.add(trackType.getGrid().getWidthX() - 1, 0, -trackType.getGrid().getWidthZ()), facing.rotateY());
                    case SOUTH:
                        return Pair.of(current.add(-trackType.getGrid().getWidthX() + 1, 0, trackType.getGrid().getWidthZ()), facing.rotateY());
                    case WEST:
                        return Pair.of(current.add(trackType.getGrid().getWidthZ(), 0, -trackType.getGrid().getWidthX() + 1), facing.rotateY());
                    default: //EAST
                        return Pair.of(current.add(-trackType.getGrid().getWidthZ(), 0, trackType.getGrid().getWidthX() - 1), facing.rotateY());

                }
            }
        },
        CURVE_LEFT() {
            @Override
            Pair<BlockPos, EnumFacing> moveTo(BlockTrack.TrackTypes trackType, EnumFacing facing, BlockPos current) {
                if (!trackType.isCurve()) return null;
                switch (facing) {
                    case NORTH:
                        return Pair.of(current.add(-trackType.getGrid().getWidthX() + 1, 0, -trackType.getGrid().getWidthZ()), facing.rotateYCCW());
                    case SOUTH:
                        return Pair.of(current.add(trackType.getGrid().getWidthX() - 1, 0, trackType.getGrid().getWidthZ()), facing.rotateYCCW());
                    case WEST:
                        return Pair.of(current.add(trackType.getGrid().getWidthZ(), 0, trackType.getGrid().getWidthX() - 1), facing.rotateYCCW());
                    default: //EAST
                        return Pair.of(current.add(-trackType.getGrid().getWidthZ(), 0, -trackType.getGrid().getWidthX() + 1), facing.rotateYCCW());
                }
            }
        };


        abstract Pair<BlockPos, EnumFacing> moveTo(BlockTrack.TrackTypes trackType, EnumFacing facing, BlockPos current);

        private final static Variant[] VARIANTS = values(); //Cached
    }

}
