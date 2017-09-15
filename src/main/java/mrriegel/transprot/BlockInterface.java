package mrriegel.transprot;

import mrriegel.limelib.block.CommonBlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockInterface extends CommonBlockContainer<TileInterface> {

	public BlockInterface() {
		super(Material.IRON, "interface");
		setHardness(2.8f);
	}

	@Override
	protected Class<? extends TileInterface> getTile() {
		return TileInterface.class;
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
		TileEntity t;
		if ((t = worldIn.getTileEntity(pos)) instanceof TileInterface)
			((TileInterface) t).setPlayer(placer.getName());
	}

}
