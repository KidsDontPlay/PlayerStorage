package mrriegel.playerstorage;

import java.util.Map;

import mezz.jei.Internal;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.Enums.MessageAction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

@JEIPlugin
public class JeiModPlugin implements IModPlugin {

	@Override
	public void register(IModRegistry registry) {
		registry.getRecipeTransferRegistry().addRecipeTransferHandler(new IRecipeTransferHandler<ContainerExI>() {

			@Override
			public Class<ContainerExI> getContainerClass() {
				return ContainerExI.class;
			}

			@Override
			public IRecipeTransferError transferRecipe(ContainerExI container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
				if (doTransfer && container.ei.mode == GuiMode.ITEM) {
					Map<Integer, ? extends IGuiIngredient<ItemStack>> inputs = recipeLayout.getItemStacks().getGuiIngredients();
					NBTTagCompound nbt = new NBTTagCompound();
					for (int i = 1; i < 10; i++) {
						if (Internal.getStackHelper().getOreDictEquivalent(inputs.get(i).getAllIngredients()) != null)
							NBTHelper.set(nbt, i - 1 + "s", Internal.getStackHelper().getOreDictEquivalent(inputs.get(i).getAllIngredients()));
						else
							NBTHelper.setList(nbt, i - 1 + "l", inputs.get(i).getAllIngredients());
					}
					NBTHelper.set(nbt, "action", MessageAction.JEITRANSFER);
					PacketHandler.sendToServer(new Message2Server(nbt));
				}
				return null;
			}

		}, VanillaRecipeCategoryUid.CRAFTING);
	}

}
