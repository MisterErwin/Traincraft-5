package si.meansoft.traincraft.items;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import si.meansoft.traincraft.track.TrackPlanner;

import java.util.List;

/**
 * Created by MisterErwin on 11.09.2016.
 * In case you need it, ask me ;)
 */
public class ItemTrackPlanner extends ItemBase {

    public ItemTrackPlanner() {
        super("trackPlanner", true);
    }

    public static TrackPlanner static_TrackPlanner_instance = null;


    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return EnumActionResult.PASS;


        NBTTagCompound compound = stack.getTagCompound();
        if (compound == null)
            stack.setTagCompound(compound = new NBTTagCompound());

        if (/*pos == null || worldIn.getBlockState(pos).getMaterial()== Material.AIR ||*/ playerIn.isSneaking()) {
            compound.removeTag("start");
            playerIn.addChatMessage(new TextComponentString("Planner Data wiped"));
            static_TrackPlanner_instance = null;
            return EnumActionResult.SUCCESS;
        }

        if (static_TrackPlanner_instance != null) {
            TrackPlanner.Status status = static_TrackPlanner_instance.doWork();
            if (status == TrackPlanner.Status.RUNNING) {
                playerIn.addChatMessage(new TextComponentString("...path planned..."
                        + static_TrackPlanner_instance.getPath().get(static_TrackPlanner_instance.getPath().size() - 1).getType().name()));

                return EnumActionResult.SUCCESS;
            } else if (status == TrackPlanner.Status.SUCCESS) {
                showPath(worldIn,static_TrackPlanner_instance.getStart(),static_TrackPlanner_instance.getPath());
            } else
                playerIn.addChatMessage(new TextComponentString("Failed..."));
            static_TrackPlanner_instance = null;
            compound.removeTag("start");
            return EnumActionResult.SUCCESS;
        }


        pos = pos.add(facing.getDirectionVec());

        if (worldIn.getBlockState(pos).getMaterial() != Material.AIR) {
            playerIn.addChatMessage(new TextComponentString("Position is occupied!"));
            return EnumActionResult.SUCCESS;
        }


        if (!compound.hasKey("start")) {
            compound.setTag("start", new NBTTagLong(pos.toLong()));
        } else {
            BlockPos start = BlockPos.fromLong(compound.getLong("start"));
            if (start == pos) {
                playerIn.addChatMessage(new TextComponentString("The end must not be the start position"));
                return EnumActionResult.SUCCESS;
            }
            playerIn.addChatMessage(new TextComponentString("Planner started. Right click the ground to do work"));
            static_TrackPlanner_instance = new TrackPlanner(start, pos);

        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        NBTTagCompound compound = stack.getTagCompound();
        if (compound == null || !compound.hasKey("start"))
            return super.getItemStackDisplayName(stack);
        BlockPos blockPos = BlockPos.fromLong(compound.getLong("start"));
        return "Pointing to " + blockPos.getX() + "," + blockPos.getY() + "," + blockPos.getZ();
    }

    //Debug function
    private void showPath(World world, BlockPos start, List<TrackPlanner.TrackPlannerSegment> segmentList) {
        boolean b = true;
        boolean first = true;
        for (TrackPlanner.TrackPlannerSegment segment : segmentList) {
            if (!first){
                start = start.add(segment.getFacing().getDirectionVec());
            }else
                first=false;
            List<BlockPos> poses = segment.getType().getGrid().getPosesToAffect(start, segment.getFacing(),
                    segment.getVariant() == TrackPlanner.Variant.CURVE_RIGHT);
            if (b)
                for (BlockPos pos : poses)
                    setBlock(world,pos,Blocks.BRICK_BLOCK.getDefaultState());
            else
                for (BlockPos pos : poses)
                    setBlock(world,pos,Blocks.WOOL.getDefaultState());
            b=!b;
            start = segment.getEnd();
        }

    }

    private void setBlock(World world, BlockPos pos, IBlockState type) {
        while (!world.isAirBlock(pos))
            pos = pos.add(0, 1, 0);
        world.setBlockState(pos, type);
    }
}
