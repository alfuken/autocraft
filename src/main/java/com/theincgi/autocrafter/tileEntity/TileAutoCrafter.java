package com.theincgi.autocrafter.tileEntity;

import com.theincgi.autocrafter.Recipe;
import com.theincgi.autocrafter.Utils;

import java.util.List;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;

public class TileAutoCrafter extends TileEntity implements ITickableTileEntity, ISidedInventory {

   public static final int OUTPUT_SLOT = 9;
   public static final int TARGET_SLOT = 10;
   private static final int[] INPUT_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
   private static final int[] OUTPUT_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
   NonNullList inventory;
   private Recipe recipe;
   private ItemStack crafts;
   private String customName;
   private List recipes;
   private ItemStackHandlerAutoCrafter ishac;
   private int currentRecipeIndex;


   public TileAutoCrafter() {
      super();
      this.inventory = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
      this.recipe = new Recipe();
      this.crafts = ItemStack.EMPTY;
      this.currentRecipeIndex = 0;
      this.ishac = new ItemStackHandlerAutoCrafter(this);
   }

   @Override
   public CompoundNBT write(CompoundNBT compound) {
      super.write(compound);
      if(this.hasCustomName()) {
         compound.putString("customName", this.customName);
      }

      compound.put("inventory", ItemStackHelper.saveAllItems(new CompoundNBT(), this.inventory));
      compound.put("recipe", this.recipe.getNBT());
      compound.put("crafts", this.crafts.serializeNBT());
      return compound;
   }

   @Override
   public void read(CompoundNBT compound) {
      super.read(compound);
      if(compound.contains("customName")) {
         this.customName = compound.getString("customName");
      }

      if(compound.contains("inventory")) {
         ItemStackHelper.loadAllItems(compound.getCompound("inventory"), this.inventory);
      }

      if(compound.contains("recipe")) {
         this.recipe = Recipe.fromNBT(compound.getList("recipe", 10));
      }

      if(compound.contains("crafts")) {
         this.crafts =  ItemStack.read(compound.getCompound("crafts"));
      }

   }

   @Override
   public int getSizeInventory() {
      return 11;
   }

   @Override
   public boolean isEmpty() {
      for(int i = 0; i < this.inventory.size(); ++i) {
         if(!((ItemStack)this.inventory.get(i)).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getStackInSlot (int index) {
      return index >= 0 && index < this.getSizeInventory() ? (ItemStack) this.inventory.get(index):ItemStack.EMPTY;
   }

   @Override
   public ItemStack decrStackSize(int index, int count) {
      ItemStack s = ItemStackHelper.getAndSplit(this.inventory, index, count);
      if(this.getStackInSlot(index).getCount() == 0) {
         this.setInventorySlotContents(index, ItemStack.EMPTY);
      }

      return s;
   }


   public ItemStack SIMULATEdecrStackSize(int index, int count) {
      ItemStack temp = getStackInSlot(index).copy();
      return temp.split(count);
   }

   @Override
   public ItemStack removeStackFromSlot(int index) {
      return ItemStackHelper.getAndRemove(inventory, index);
   }

   @Override
   public void setInventorySlotContents(int index, ItemStack stack) {
      ItemStack itemstack = (ItemStack)this.inventory.get(index);
      this.inventory.set(index, stack);
      if(stack.getCount() > this.getInventoryStackLimit()) {
         stack.setCount(this.getInventoryStackLimit());
      }

      this.markDirty();
   }

   @Override
   public int getInventoryStackLimit() {
      return 64;
   }

   @Override
   public boolean isUsableByPlayer(PlayerEntity player) {
      return world.getTileEntity(getPos()) == this &&
              player.getDistanceSq(.5, .5, .5) <= 64;
   }

   @Override
   public void openInventory(PlayerEntity player) {
   }

   @Override
   public void closeInventory(PlayerEntity player) {
   }

   @Override
   public boolean isItemValidForSlot(int index, ItemStack stack) {
      return true;

   }

   @Override
   public int getField(int id) {
      return 0;
   }

   @Override
   public void setField(int id, int value) {
   }

   @Override
   public int getFieldCount() {
      return 0;
   }

   @Override
   public void clear() {

      for(int i = 0; i<inventory.size(); i++){
         inventory.set(i, ItemStack.EMPTY);
      }
   }


   @Override
   public String getName() {

      return hasCustomName()?customName:"Auto Crafter";
   }

   @Override
   public boolean hasCustomName() {
      return customName!=null;
   }
   @Override
   public ITextComponent getDisplayName() {
      return Utils.IText(getName());
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      if(side.equals(Direction.DOWN)){return OUTPUT_SLOTS;}
      return INPUT_SLOTS;
   }

   public boolean isSlotAllowed(int index, ItemStack itemStack) {
      return index < 9 && this.recipe.matchesRecipe(index, itemStack);
   }

   @Override
   public boolean canInsertItem(int index, ItemStack itemStackIn, Direction direction) {
      //System.out.printf("canInsertItem: %d %s %s\n",index, itemStackIn.getItem().getRegistryName().toString(), isSlotAllowed(index, itemStackIn) && nextHasSameOrMore(index, itemStackIn));
      return isSlotAllowed(index, itemStackIn);
   }

   @Override
   public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
      return index==OUTPUT_SLOT || (index<9&&!recipe.matchesRecipe(index, stack));
   }

   public void setCustomName(String displayName) {
      this.customName = displayName;
      this.markDirty();
   }

   public void setRecipe(IRecipe recipe) {
      this.recipe.setRecipe(recipe);
      this.markDirty();
   }

   public void setRecipe(ListNBT recipeTag) {
      this.recipe = Recipe.fromNBT(recipeTag);
   }

   public void updateRecipes(ItemStack crafts, int index) {
      this.crafts = crafts;
      this.recipes = Utils.getValid(world,crafts);
      this.currentRecipeIndex = index % Math.max(1, this.recipes.size());
      if(this.recipes.size() > 0) {
         this.setRecipe((IRecipe)this.recipes.get(this.currentRecipeIndex));
      } else {
         this.recipe.clearRecipe();
      }

      this.markDirty();
   }

   public Recipe getRecipe() {
      return this.recipe;
   }

   public ItemStack getCrafts() {
      return this.crafts;
   }

   public void nextRecipe() {
      if(this.recipes == null) {
         this.updateRecipes(this.getCrafts(), this.currentRecipeIndex);
      }

      if(this.recipes.size() != 0) {
         ++this.currentRecipeIndex;
         this.currentRecipeIndex %= this.recipes.size();
         this.setRecipe((IRecipe)this.recipes.get(this.currentRecipeIndex));
      }
   }

   public void prevRecipe() {
      if(this.recipes == null) {
         this.updateRecipes(this.getCrafts(), this.currentRecipeIndex);
      }

      if(this.recipes.size() != 0) {
         --this.currentRecipeIndex;
         if(this.currentRecipeIndex < 0) {
            this.currentRecipeIndex = this.recipes.size() - 1;
         }

         this.setRecipe((IRecipe)this.recipes.get(this.currentRecipeIndex));
      }
   }

   public int getCurrentRecipeIndex() {
      return this.currentRecipeIndex;
   }

   @Override
   public boolean hasCapability(Capability capability, Direction facing) {
      return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY?true:super.hasCapability(capability, facing);
   }

   @Override
   public Object getCapability(Capability capability, Direction facing) {
      return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY?this.ishac:super.getCapability(capability, facing);
   }

   @Override
   public void tick() {
      if(!this.world.isBlockPowered(pos) && world.isBlockIndirectlyGettingPowered(pos) <= 0) {
         if(!this.recipe.getOutput().isEmpty()) {
            if(getStackInSlot(OUTPUT_SLOT).getCount() + this.recipe.getOutput().getCount() <= this.recipe.getOutput().getMaxStackSize()) {
               if(Recipe.matches(getStackInSlot(OUTPUT_SLOT), this.recipe.getOutput()) || getStackInSlot(OUTPUT_SLOT).isEmpty()) {

                  this.distributeItems();

                  for(int leftovers = 0; leftovers < 9; ++leftovers) {
                     if(!this.recipe.matchesRecipe(leftovers, (ItemStack)this.inventory.get(leftovers))) {
                        return;
                     }
                  }

                  NonNullList <ItemStack> leftovers = this.recipe.getLeftovers(this.inventory, 0, 9);

                  for(int i = 0; i < 9; ++i) {
                     ((ItemStack)this.inventory.get(i)).shrink(1);
                     if(((ItemStack)this.inventory.get(i)).getCount() <= 0) {
                        this.setInventorySlotContents(i, ItemStack.EMPTY);
                     }

                     if(!((ItemStack)leftovers.get(i)).isEmpty()) {
                        if(((ItemStack)this.inventory.get(i)).isEmpty()) {
                           this.setInventorySlotContents(i, (ItemStack)leftovers.get(i));
                        } else {
                           InventoryHelper.spawnItemStack(this.world, (double)this.pos.getX(), (double)this.pos.getY(), (double)this.pos.getZ(), (ItemStack)leftovers.get(i));
                        }
                     }
                  }

                  if(getStackInSlot(OUTPUT_SLOT).isEmpty()) {
                     setInventorySlotContents(OUTPUT_SLOT, this.recipe.getOutput());
                  } else {
                     getStackInSlot(OUTPUT_SLOT).grow(this.recipe.getOutput().getCount());
                  }

                  this.markDirty();
               }
            }
         }
      }
   }

   private void distributeItems() {
      for(int i = 0; i < 9; ++i) {
         ItemStack current = getStackInSlot(i);
         if(!current.isEmpty()) {
            int nextMatch = this.nextMatch(i);
            if(nextMatch >= 0) {
               if(getStackInSlot(nextMatch).isEmpty()) {
                  if(current.getCount() >= 2) {
                     setInventorySlotContents(nextMatch, current.split(1));
                  }
               } else if(current.getCount() > getStackInSlot(nextMatch).getCount()) {
                  current.shrink(1);
                  getStackInSlot(nextMatch).grow(1);
               }
            }
         }
      }

   }

   private int nextMatch(int j) {
      ItemStack is = getStackInSlot(j);

      for(int i = 0; i < 9; ++i) {
         int c = (i + j + 1) % 9;
         if(Recipe.matches(is, getStackInSlot(c)) || getStackInSlot(c).isEmpty() && this.recipe.matchesRecipe(c, is)) {
            return c == j?-1:c;
         }
      }

      return -1;
   }

   public void setCurrentRecipeIndex(int integer) {
      this.currentRecipeIndex = integer;
   }

   public void setCrafts(ItemStack itemStack) {
      this.crafts = itemStack;
   }


}
