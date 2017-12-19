package com.minecolonies.coremod.items;

import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.LanguageHandler;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.client.gui.WindowBuildTool;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.creativetab.ModCreativeTabs;
import com.minecolonies.coremod.tileentities.TileEntityColonyBuilding;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Caliper Item class. Calculates distances, areas, and volumes.
 */
public class ItemCaliper extends AbstractItemMinecolonies
{
    private static final RangedAttribute ATTRIBUTE_CALIPER_USE = new RangedAttribute((IAttribute) null, "player.caliperUse", 0.0, 0.0, 1.0);

    private static final double HALF                        = 0.5;
    private static final String ITEM_CALIPER_MESSAGE_LINE   = "item.caliper.message.line";
    private static final String ITEM_CALIPER_MESSAGE_SQUARE = "item.caliper.message.square";
    private static final String ITEM_CALIPER_MESSAGE_CUBE   = "item.caliper.message.cube";
    private static final String ITEM_CALIPER_MESSAGE_SAME   = "item.caliper.message.same";

    private BlockPos startPosition;

    /**
     * Caliper constructor. Sets max stack to 1, like other tools.
     */
    public ItemCaliper()
    {
        super("caliper");

        super.setCreativeTab(ModCreativeTabs.MINECOLONIES);
        maxStackSize = 1;
    }

    private static EnumActionResult handleZEqual(@NotNull final EntityPlayer playerIn, final int a, final int a2)
    {
        final int distance1 = Math.abs(a) + 1;
        final int distance2 = Math.abs(a2) + 1;

        LanguageHandler.sendPlayerMessage(
          playerIn, ITEM_CALIPER_MESSAGE_SQUARE, Integer.toString(distance1), Integer.toString(distance2));
        return EnumActionResult.SUCCESS;
    }

    @Override
    public EnumActionResult onItemUse(
                                       final EntityPlayer player,
                                       final World worldIn,
                                       final BlockPos pos,
                                       final EnumHand hand,
                                       final EnumFacing facing,
                                       final float hitX,
                                       final float hitY,
                                       final float hitZ)
    {
        //todo make own items.
        final ItemStack stack = player.getHeldItem(hand);
        if (!stack.hasTagCompound())
        {
            stack.setTagCompound(new NBTTagCompound());
        }
        final NBTTagCompound compound = stack.getTagCompound();

        if (!compound.hasKey("schematic"))
        {
            if (!worldIn.isRemote)
            {
                final TileEntity entity = worldIn.getTileEntity(pos);

                if (entity instanceof TileEntityColonyBuilding)
                {
                    final AbstractBuilding building = ((TileEntityColonyBuilding) entity).getBuilding();

                    if(building.getBaseY() == 0)
                    {
                        LanguageHandler.sendPlayerMessage(player, "This is an old building, please repair before copying.");
                        return EnumActionResult.FAIL;
                    }
                    final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> corners = building.getCorners();


                    final String name = building.getStyle() + "/" + building.getSchematicName() + building.getBuildingLevel();
                    ItemScanTool.saveStructure(worldIn,
                            new BlockPos(corners.getFirst().getFirst(), building.getBaseY(), corners.getSecond().getFirst()),
                            new BlockPos(corners.getFirst().getSecond(), building.getBaseY() + building.getHeight(), corners.getSecond().getSecond()), player, name);
                    compound.setString("schematic", "scans/new/" + name);
                    BlockPosUtil.writeToNBT(compound, "pos", building.getID());
                    LanguageHandler.sendPlayerMessage(player, "Click again to deconstruct it.");
                    return EnumActionResult.SUCCESS;
                }
            }
        }
        else if(!compound.hasKey("deconstructed"))
        {
            if (!worldIn.isRemote)
            {
                final TileEntity entity = worldIn.getTileEntity(pos);

                if (entity instanceof TileEntityColonyBuilding)
                {
                    final BlockPos id = BlockPosUtil.readFromNBT(compound, "pos");
                    if(!id.equals(((TileEntityColonyBuilding) entity).getBuilding().getID()))
                    {
                        LanguageHandler.sendPlayerMessage(player, "Trying to cheet, huh? That's another building!");
                        return EnumActionResult.FAIL;
                    }
                    ((TileEntityColonyBuilding) entity).getBuilding().deconstruct();
                    compound.setBoolean("deconstructed", true);
                    return EnumActionResult.SUCCESS;
                }
            }
        }
        else
        {
            if (worldIn.isRemote)
            {
                placeCopiedBuilding(pos, compound.getString("schematic"));
            }
        }

        return EnumActionResult.SUCCESS;
    }

    @NotNull
    @Override
    public ActionResult<ItemStack> onItemRightClick(final World worldIn, final EntityPlayer player, final EnumHand hand)
    {
        final ItemStack stack = player.getHeldItem(hand);
        if (!stack.hasTagCompound())
        {
            stack.setTagCompound(new NBTTagCompound());
        }

        final NBTTagCompound compound = stack.getTagCompound();
        if (compound.hasKey("schematic") && worldIn.isRemote)
        {
            placeCopiedBuilding(null, compound.getString("schematic"));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.FAIL, stack);
    }

    private void placeCopiedBuilding(@Nullable final BlockPos pos, @NotNull final String name)
    {
        if(pos == null)
        {
            MineColonies.proxy.openBuildToolWindow(null, name, 0, WindowBuildTool.FreeMode.MOVE);
            return;
        }
        MineColonies.proxy.openBuildToolWindow(pos, name, 0, WindowBuildTool.FreeMode.MOVE);
    }

    /**
     * Checks if the camp can be placed.
     * @param world the world.
     * @param pos the position.
     * @param size the size.
     * @return true if so.
     */
    @NotNull
    public static boolean canCampBePlaced(@NotNull final World world, @NotNull final BlockPos pos, final BlockPos size)
    {
        for(int z = pos.getZ() - size.getZ() / 2 + 1; z < pos.getZ() + size.getZ() / 2 + 1; z++)
        {
            for(int x = pos.getX() - size.getX() / 2 + 1; x < pos.getX() + size.getX() / 2 + 1; x++)
            {
                if(!world.isAirBlock(new BlockPos(x, pos.getY(), z)))
                {
                    return false;
                }
            }
        }

        for(int z = pos.getZ() - size.getZ() / 2 + 1; z < pos.getZ() + size.getZ() / 2 + 1; z++)
        {
            for(int x = pos.getX() - size.getX() / 2 + 1; x < pos.getX() + size.getX() / 2 + 1; x++)
            {
                if(!world.isAirBlock(new BlockPos(x, pos.getY()+1, z)))
                {
                    return false;
                }
            }
        }
        return true;
    }


    private EnumActionResult save(final EntityPlayer player,
            final World worldIn,
            final BlockPos pos,
            final EnumHand hand,
            final EnumFacing facing,
            final float hitX,
            final float hitY,
            final float hitZ)
    {
// if client world, do nothing
        if (worldIn.isRemote)
        {
            return EnumActionResult.FAIL;
        }

        // if attribute instance is not known, register it.
        IAttributeInstance attribute = player.getEntityAttribute(ATTRIBUTE_CALIPER_USE);
        if (attribute == null)
        {
            attribute = player.getAttributeMap().registerAttribute(ATTRIBUTE_CALIPER_USE);
        }
        // if the value of the attribute is still 0, set the start values. (first point)
        if (attribute.getAttributeValue() < HALF)
        {
            startPosition = pos;
            attribute.setBaseValue(1.0);
            return EnumActionResult.SUCCESS;
        }
        attribute.setBaseValue(0.0);
        //Start == end, same location
        if (startPosition.getX() == pos.getX() && startPosition.getY() == pos.getY() && startPosition.getZ() == pos.getZ())
        {
            LanguageHandler.sendPlayerMessage(player, ITEM_CALIPER_MESSAGE_SAME);
            return EnumActionResult.FAIL;
        }

        return handlePlayerMessage(player, pos);
    }

    private EnumActionResult handlePlayerMessage(@NotNull final EntityPlayer playerIn, @NotNull final BlockPos pos)
    {
        if (startPosition.getX() == pos.getX())
        {
            return handleXEqual(playerIn, pos);
        }
        if (startPosition.getY() == pos.getY())
        {
            return handleYEqual(playerIn, pos, pos.getX() - startPosition.getX(), pos.getY() - startPosition.getZ());
        }
        if (startPosition.getZ() == pos.getZ())
        {
            return handleZEqual(playerIn, pos.getX() - startPosition.getX(), pos.getY() - startPosition.getY());
        }

        final int distance1 = Math.abs(pos.getX() - startPosition.getX()) + 1;
        final int distance2 = Math.abs(pos.getY() - startPosition.getY()) + 1;
        final int distance3 = Math.abs(pos.getZ() - startPosition.getZ()) + 1;

        LanguageHandler.sendPlayerMessage(
          playerIn,
          ITEM_CALIPER_MESSAGE_CUBE,
          Integer.toString(distance1), Integer.toString(distance2), Integer.toString(distance3));
        return EnumActionResult.SUCCESS;
    }

    private EnumActionResult handleYEqual(@NotNull final EntityPlayer playerIn, @NotNull final BlockPos pos, final int a, final int a2)
    {
        if (startPosition.getZ() == pos.getZ())
        {
            final int distance = Math.abs(a) + 1;
            LanguageHandler.sendPlayerMessage(playerIn, ITEM_CALIPER_MESSAGE_LINE, Integer.toString(distance));
            return EnumActionResult.SUCCESS;
        }
        return handleZEqual(playerIn, a, a2);
    }

    private EnumActionResult handleXEqual(@NotNull final EntityPlayer playerIn, @NotNull final BlockPos pos)
    {
        if (startPosition.getY() == pos.getY())
        {
            final int distance = Math.abs(pos.getZ() - startPosition.getZ()) + 1;
            LanguageHandler.sendPlayerMessage(playerIn, ITEM_CALIPER_MESSAGE_LINE, Integer.toString(distance));
            return EnumActionResult.SUCCESS;
        }
        return handleYEqual(playerIn, pos, pos.getY() - startPosition.getY(), pos.getZ() - startPosition.getZ());
    }
}
