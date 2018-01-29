package mrriegel.playerstorage.registry;

import java.util.ArrayList;
import java.util.List;

import mrriegel.limelib.tile.CommonTile;
import mrriegel.limelib.util.StackWrapper;
import mrriegel.playerstorage.ExInventory;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class TileKeeper extends CommonTile {

	private List<StackWrapper> items = new ArrayList<>();
	private List<FluidStack> fluids = new ArrayList<>();
	private String name;

	public void create(ExInventory exi) {
		for (StackWrapper sw : exi.items)
			items.add(sw);
		exi.items.clear();
		for (FluidStack fs : exi.fluids)
			fluids.add(fs);
		exi.fluids.clear();
		exi.markForSync();
		name = exi.player.getName();
		if (items.isEmpty() && fluids.isEmpty())
			world.destroyBlock(pos, false);
	}

	public void destroy(EntityPlayerMP player) {
		if (player.getName().equals(name)) {
			ExInventory exi = ExInventory.getInventory(player);
			for (StackWrapper sw : items) {
				for (ItemStack s : StackWrapper.toStackList(sw))
					Block.spawnAsEntity(world, pos.up(), exi.insertItem(s, false));
			}
			for (FluidStack fs : fluids) {
				exi.insertFluid(fs, false);
			}
			world.destroyBlock(pos, false);
		}
	}

	@Override
	public boolean openGUI(EntityPlayerMP player) {
		destroy(player);
		return super.openGUI(player);
	}

}
