package mrriegel.playerstorage.registry;

import java.util.Optional;

import mrriegel.limelib.item.CommonItem;
import mrriegel.playerstorage.ExInventory;
import mrriegel.playerstorage.PlayerStorage;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class ItemRemote extends CommonItem {

	public ItemRemote() {
		super("remote");
		setCreativeTab(CreativeTabs.MISC);
		setMaxStackSize(1);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		Optional.ofNullable(ExInventory.getInventory(playerIn)).ifPresent(ei -> playerIn.openGui(PlayerStorage.MODID, 0, worldIn, 0, 0, 0));
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	//	@Override
	//	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
	//		if (!ConfigHandler.remote)
	//			stack.setCount(0);
	//		return super.initCapabilities(stack, nbt);
	//	}

}
