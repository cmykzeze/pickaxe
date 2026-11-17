package com.example.pickaxeenchants;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * The rare books, found in chests only:
 *  - Excavation I: ancient city chests only, 4% per chest.
 *  - Haste VI to X: ancient city, bastion treasure, end city and woodland mansion chests, 8-10% per chest
 *    (higher levels are rarer). Haste I to V come from the enchanting table instead.
 */
public class ExcavationLoot {
	private static final float EXCAVATION_CHANCE = 0.04f;   // ancient city
	private static final float HASTE_CHANCE_ANCIENT_CITY = 0.08f;
	private static final float HASTE_CHANCE_BASTION = 0.10f;
	private static final float HASTE_CHANCE_END_CITY = 0.08f;
	private static final float HASTE_CHANCE_MANSION = 0.08f;

	public static void register() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			boolean ancientCity = key.equals(BuiltInLootTables.ANCIENT_CITY);
			float hasteChance = 0f;
			if (ancientCity) {
				hasteChance = HASTE_CHANCE_ANCIENT_CITY;
			} else if (key.equals(BuiltInLootTables.BASTION_TREASURE)) {
				hasteChance = HASTE_CHANCE_BASTION;
			} else if (key.equals(BuiltInLootTables.END_CITY_TREASURE)) {
				hasteChance = HASTE_CHANCE_END_CITY;
			} else if (key.equals(BuiltInLootTables.WOODLAND_MANSION)) {
				hasteChance = HASTE_CHANCE_MANSION;
			}
			if (!ancientCity && hasteChance == 0f) {
				return;
			}

			if (ancientCity) {
				Holder<Enchantment> excavation = registries.lookupOrThrow(Registries.ENCHANTMENT)
						.getOrThrow(BreakingEnchants.EXCAVATION);
				LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
				pool.when(LootItemRandomChanceCondition.randomChance(EXCAVATION_CHANCE));
				pool.add(book(excavation, 1, 1));
				tableBuilder.withPool(pool);
			}

			if (hasteChance > 0f) {
				Holder<Enchantment> haste = registries.lookupOrThrow(Registries.ENCHANTMENT)
						.getOrThrow(PickaxeEffects.HASTE);
				LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
				pool.when(LootItemRandomChanceCondition.randomChance(hasteChance));
				pool.add(book(haste, 6, 30));
				pool.add(book(haste, 7, 25));
				pool.add(book(haste, 8, 20));
				pool.add(book(haste, 9, 15));
				pool.add(book(haste, 10, 10));
				tableBuilder.withPool(pool);
			}
		});
	}

	private static LootItem.Builder<?> book(Holder<Enchantment> enchantment, int level, int weight) {
		return LootItem.lootTableItem(Items.ENCHANTED_BOOK)
				.setWeight(weight)
				.apply(new SetEnchantmentsFunction.Builder()
						.withEnchantment(enchantment, ContextIntProviders.exactly(level)));
	}
}
