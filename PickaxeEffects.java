package com.example.pickaxeenchants;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Haste (I to X) and Night Vision (I): while you hold the pickaxe you get the real potion effect.
 *  - Haste level N gives the Haste N effect (Haste I at level I ... Haste X at level X).
 *  - Night Vision gives the Night Vision effect.
 * The effect is refreshed every few ticks and removed again when you put the pickaxe away.
 * An effect that comes from somewhere else (potion, beacon) is never overwritten by a weaker one, and is only
 * removed if this enchantment put it there.
 */
public class PickaxeEffects {
	public static final ResourceKey<Enchantment> HASTE = key("haste");
	public static final ResourceKey<Enchantment> NIGHT_VISION = key("night_vision");

	// ---- Tweak these numbers if you want ----
	private static final int HASTE_DURATION = 20;       // 1 second, refreshed every ~10 ticks
	private static final int HASTE_REFRESH_BELOW = 10;
	// Night Vision flashes when it has under 10 seconds left, so it is given a long duration and refreshed early
	private static final int NV_DURATION = 400;
	private static final int NV_REFRESH_BELOW = 300;

	private static final Set<UUID> HASTE_OURS = new HashSet<>();
	private static final Set<UUID> NV_OURS = new HashSet<>();

	private static ResourceKey<Enchantment> key(String name) {
		return ResourceKey.create(Registries.ENCHANTMENT,
				Identifier.fromNamespaceAndPath(PickaxeEnchantsMod.MOD_ID, name));
	}

	/** Runs every server tick. */
	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerLevel level = (ServerLevel) player.level();
			ItemStack stack = player.getMainHandItem();
			boolean pickaxe = stack.is(ItemTags.PICKAXES);

			int haste = pickaxe ? BreakingEnchants.getLevel(level, stack, HASTE) : 0;
			// Haste level N = Haste N effect; the game counts effect levels from 0, so level N = amplifier N - 1
			int hasteAmp = haste > 0 ? Math.min(haste, 10) - 1 : -1;
			sync(player, MobEffects.HASTE, hasteAmp, HASTE_DURATION, HASTE_REFRESH_BELOW, HASTE_OURS);

			int nv = pickaxe ? BreakingEnchants.getLevel(level, stack, NIGHT_VISION) : 0;
			sync(player, MobEffects.NIGHT_VISION, nv > 0 ? 0 : -1, NV_DURATION, NV_REFRESH_BELOW, NV_OURS);
		}
	}

	private static void sync(ServerPlayer player, Holder<MobEffect> effect, int amp, int duration,
			int refreshBelow, Set<UUID> ours) {
		UUID id = player.getUUID();
		MobEffectInstance current = player.getEffect(effect);

		if (amp < 0) {
			// Not holding the enchanted pickaxe any more: take away only what we gave
			if (ours.remove(id) && current != null) {
				player.removeEffect(effect);
			}
			return;
		}

		boolean isOurs = ours.contains(id);
		if (current != null) {
			if (!isOurs && current.getAmplifier() >= amp) {
				return; // a potion or beacon already gives at least this much
			}
			if (current.getAmplifier() > amp) {
				return;
			}
		}
		if (current == null || !isOurs || current.getAmplifier() != amp || current.getDuration() < refreshBelow) {
			// ambient = true, no particles, but the icon stays visible in the corner
			player.addEffect(new MobEffectInstance(effect, duration, amp, true, false, true));
			ours.add(id);
		}
	}
}
