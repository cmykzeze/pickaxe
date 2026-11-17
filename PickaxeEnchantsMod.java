package com.example.pickaxeenchants;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class PickaxeEnchantsMod implements ModInitializer {
	public static final String MOD_ID = "pickaxeenchants";

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(PickaxeEffects::tick); // Haste + Night Vision effects
		BreakingEnchants.register();  // Ore Smelter, Explosive and Excavation
		ExcavationLoot.register();    // rare books: Excavation and Haste VI-X in chests
	}
}
