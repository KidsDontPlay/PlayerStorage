package mrriegel.playerstorage;

import java.util.List;
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
import mezz.jei.transfer.RecipeTransferErrorTooltip;
import mrriegel.limelib.LimeLib;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.Enums.MessageAction;
import mrriegel.playerstorage.gui.ContainerExI;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

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
				if (container.ei.mode != GuiMode.ITEM)
					return new RecipeTransferErrorTooltip("You are in fluid mode.");
				if (doTransfer) {
					Map<Integer, ? extends IGuiIngredient<ItemStack>> inputs = recipeLayout.getItemStacks().getGuiIngredients();
					NBTTagCompound nbt = new NBTTagCompound();
					for (int i = 1; i < 10; i++) {
						List<ItemStack> ings = inputs.get(i).getAllIngredients();
						String ore = Internal.getStackHelper().getOreDictEquivalent(ings);
						if (ore != null)
							nbt.setString(i - 1 + "", ore);
						else {
							NBTTagList tag = new NBTTagList();
							nbt.setTag(i - 1 + "", tag);
							for (ItemStack s : ings)
								tag.appendTag(s.writeToNBT(new NBTTagCompound()));
						}
					}

					if (NBTHelper.getSize(nbt) > 32000) {
						LimeLib.log.error("Too much ingredients for " + inputs.get(0).getAllIngredients());
						nbt.getKeySet().stream().map(s -> s + ": " + nbt.getTag(s)).forEach(LimeLib.log::error);
						Minecraft.getMinecraft().player.closeScreen();
						TextComponentString tcs1 = new TextComponentString(TextFormatting.RED + "Error! Open an issue ");
						TextComponentString tcs2 = new TextComponentString(TextFormatting.RED + " and attach the logfile.");
						TextComponentString click = new TextComponentString("[here]");
						Style s = new Style();
						s.setColor(TextFormatting.GREEN);
						s.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/MrRiegel/PlayerStorage/issues"));
						click.setStyle(s);
						tcs1.appendSibling(click);
						tcs1.appendSibling(tcs2);
						Minecraft.getMinecraft().player.sendStatusMessage(tcs1, false);
						return null;
					}
					MessageAction.JEITRANSFER.set(nbt);
					PacketHandler.sendToServer(new Message2Server(nbt));
				}
				return null;
			}

		}, VanillaRecipeCategoryUid.CRAFTING);
	}

}
