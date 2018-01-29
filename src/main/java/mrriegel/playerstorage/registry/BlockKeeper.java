package mrriegel.playerstorage.registry;

import java.util.Random;

import mrriegel.limelib.block.CommonBlockContainer;
import mrriegel.limelib.helper.RegistryHelper;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class BlockKeeper extends CommonBlockContainer<TileKeeper> {

	public BlockKeeper() {
		super(Material.ROCK, "keeper");
		setBlockUnbreakable();
		setResistance(6000000.0F);
		setSoundType(SoundType.STONE);
	}

	@Override
	public void registerBlock() {
		super.registerBlock();
		RegistryHelper.unregister(getItemBlock());
	}

	@Override
	protected Class<? extends TileKeeper> getTile() {
		return TileKeeper.class;
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return Items.AIR;
	}

}
