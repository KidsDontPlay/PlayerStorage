package mrriegel.playerstorage;

import java.util.List;

import org.apache.commons.lang3.text.WordUtils;

import mrriegel.limelib.helper.RegistryHelper;
import mrriegel.playerstorage.ConfigHandler.Unit2;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class ItemApple extends ItemFood {

	protected final int num;

	public ItemApple() {
		super(0, 0, false);
		num = ConfigHandler.apples.size();
		setRegistryName("apple");
		setUnlocalizedName(getRegistryName().toString());
		setHasSubtypes(true);
		setCreativeTab(CreativeTabs.MISC);
	}

	public void registerItem() {
		RegistryHelper.register(this);
	}

	public void initModel() {
		for (int i = 0; i < num; i++)
			RegistryHelper.initModel(this, i, new ModelResourceLocation(getRegistryName(), "inventory"));
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
		if (isInCreativeTab(tab) && !ConfigHandler.infiniteSpace)
			for (int i = 0; i < num; i++)
				subItems.add(new ItemStack(this, 1, i));
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName() + "_" + stack.getItemDamage();
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		String ore = ConfigHandler.appleList.get(stack.getItemDamage());
		int firstUp = -1;
		for (int i = 0; i < ore.length(); i++) {
			if (Character.isUpperCase(ore.charAt(i))) {
				firstUp = i;
				break;
			}
		}
		ore = firstUp != -1 ? ore = ore.substring(firstUp) : WordUtils.capitalize(ore);
		return I18n.format("item.playerstorage:apple.name") + " (" + ore + ")";
	}

	@Override
	protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
		super.onFoodEaten(stack, worldIn, player);
		if (!worldIn.isRemote && !ConfigHandler.infiniteSpace) {
			ExInventory ei = ExInventory.getInventory(player);
			Unit2 u = ConfigHandler.apples.get(ConfigHandler.appleList.get(stack.getItemDamage()));
			ei.itemLimit += Math.abs(u.itemLimit);
			ei.fluidLimit += Math.abs(u.fluidLimit);
			player.sendStatusMessage(new TextComponentString("Player Storage increased by " + u.itemLimit + "/" + u.fluidLimit + "."), true);
		}
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		Unit2 u = ConfigHandler.apples.get(ConfigHandler.appleList.get(stack.getItemDamage()));
		tooltip.add(u.itemLimit + "/" + u.fluidLimit);
	}

}
