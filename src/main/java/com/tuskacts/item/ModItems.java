package com.tuskacts.item;

import com.tuskacts.TuskActs;
import com.tuskacts.server.Manager;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class ModItems {
    private ModItems() {}

    private static final class Rectangle extends Item {
        Rectangle(Settings s) {
            super(s);
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient || !(user instanceof ServerPlayerEntity sp)) return TypedActionResult.success(stack);
            if (Manager.evolve(sp)) {
                if (!sp.isCreative()) stack.decrement(1);
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.fail(stack);
        }
    }

    /** The Golden Rectangle: first use gives Tusk Act 1, every next one evolves it (Act 2, 3, 4). */
    public static final Item GOLDEN_RECTANGLE = Registry.register(Registries.ITEM, TuskActs.id("golden_rectangle"),
            new Rectangle(new Item.Settings().maxCount(16).rarity(Rarity.EPIC)));

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> e.add(GOLDEN_RECTANGLE));
    }
}
