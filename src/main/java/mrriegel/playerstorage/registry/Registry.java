package mrriegel.playerstorage.registry;

import mrriegel.limelib.block.CommonBlock;
import mrriegel.limelib.helper.RecipeHelper;
import mrriegel.limelib.helper.RegistryHelper;
import mrriegel.limelib.item.CommonItem;
import mrriegel.playerstorage.ConfigHandler;
import mrriegel.playerstorage.PlayerStorage;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class Registry {

	//blocks
	public static final CommonBlock interfac = new BlockInterface();
	public static final CommonBlock keeper = new BlockKeeper();

	//items
	public static final ItemApple apple = new ItemApple();
	public static final CommonItem remote = new ItemRemote();

	public static void init() {
		interfac.registerBlock();
		if (false)
			keeper.registerBlock();
		apple.registerItem();
		remote.registerItem();

		for (int i = 0; i < ConfigHandler.appleList.size(); i++) {
			String ore = ConfigHandler.appleList.get(i);
			ResourceLocation rl = new ResourceLocation(PlayerStorage.MODID, "apple_" + i);
			RegistryHelper.register(new ShapedOreRecipe(rl, new ItemStack(apple, 1, i), " o ", "oao", " o ", 'a', Items.APPLE, 'o', ore).setRegistryName(rl));
		}
		RecipeHelper.addShapedRecipe(new ItemStack(interfac.getItemBlock(), 2), "ses", "did", "ses", 's', "stone", 'e', "enderpearl", 'd', "gemDiamond", 'i', "ingotIron");
		RecipeHelper.addShapedRecipe(new ItemStack(remote), "iii", "ipi", "iii", 'i', "nuggetIron", 'p', "paper");
	}

	public static void initClient() {
		interfac.initModel();
		apple.initModel();
		remote.initModel();
	}

}
