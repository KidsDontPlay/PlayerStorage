package mrriegel.playerstorage;

import mrriegel.limelib.block.CommonBlock;
import mrriegel.limelib.helper.RegistryHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class Registry {

	//blocks
	public static final CommonBlock interfac = new BlockInterface();

	//items
	public static final ItemApple apple = new ItemApple();

	public static void init() {
		interfac.registerBlock();
		apple.registerItem();

		for (int i = 0; i < ConfigHandler.appleList.size(); i++) {
			String ore = ConfigHandler.appleList.get(i);
			ResourceLocation rl = new ResourceLocation(PlayerStorage.MODID, "apple_" + i);
			RegistryHelper.register(new ShapedOreRecipe(rl, new ItemStack(apple, 1, i), " o ", "oao", " o ", 'a', Items.APPLE, 'o', ore).setRegistryName(rl));
		}
	}

	public static void initClient() {
		interfac.initModel();
		apple.initModel();
	}

}
