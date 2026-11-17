package com.example.pickaxeenchants;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Everything that happens when you mine a block with an enchanted pickaxe:
 *
 *  - Ore Smelter: the ore you mine drops ingots instead of raw ore (iron, gold, copper, ancient debris).
 *    Can't be combined with Silk Touch.
 *  - Explosive (I to X): a chance of 10% per level (X = 100%) that the block "explodes": every block within
 *    EXPLOSION_RADIUS of it breaks too. It is NOT a real explosion, so nothing is damaged (not you, not mobs, not
 *    items, nothing but blocks).
 *  - Excavation (I): every time, the 3x3x3 cube around the block breaks too.
 *    (Excavation and Explosive can't be put on the same pickaxe, but the code copes if a command does it.)
 *
 * Extra blocks give their drops straight to your inventory (fortune / silk touch on the pickaxe still apply, and
 * Ore Smelter smelts them too). If the inventory is full, what doesn't fit drops on the ground where the block was.
 * Only blocks a pickaxe is meant for (stone, ores, ...) are broken, and never unbreakable blocks or blocks
 * with contents (chests, spawners, furnaces...).
 */
public class BreakingEnchants {
	public static final ResourceKey<Enchantment> EXPLOSIVE = key("explosive");
	public static final ResourceKey<Enchantment> EXCAVATION = key("excavation");
	public static final ResourceKey<Enchantment> ORE_SMELTER = key("ore_smelter");

	// ---- Tweak these numbers if you want ----
	/** Chance per mined block: level x 10% (I = 10% ... X = 100%) */
	private static final float CHANCE_PER_LEVEL = 0.10f;
	/** How far the explosion reaches, in blocks (a sphere around the mined block) */
	private static final double EXPLOSION_RADIUS = 2.0;
	/** Excavation reaches 1 block each way from the mined block = a 3x3x3 cube */
	private static final int EXCAVATION_REACH = 1;

	/** What Ore Smelter turns drops into. Add a line here to smelt something else. */
	private static final Map<Item, Item> SMELTS = Map.of(
			Items.RAW_IRON, Items.IRON_INGOT,
			Items.RAW_GOLD, Items.GOLD_INGOT,
			Items.RAW_COPPER, Items.COPPER_INGOT,
			Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP);

	private static boolean busy = false; // safety: our own breaking must never start another round

	private static ResourceKey<Enchantment> key(String name) {
		return ResourceKey.create(Registries.ENCHANTMENT,
				Identifier.fromNamespaceAndPath(PickaxeEnchantsMod.MOD_ID, name));
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (busy || !(world instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)
					|| serverPlayer.isCreative()) {
				return;
			}
			ItemStack tool = serverPlayer.getMainHandItem();
			if (!tool.is(ItemTags.PICKAXES) || !state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
				return;
			}
			boolean smelter = getLevel(level, tool, ORE_SMELTER) > 0;
			int explosive = getLevel(level, tool, EXPLOSIVE);
			boolean excavation = getLevel(level, tool, EXCAVATION) > 0;

			// The block you mined already dropped its items on the ground: smelt them there
			if (smelter && smeltDroppedItems(level, pos)) {
				flames(level, pos);
			}

			boolean explode = explosive > 0 && serverPlayer.getRandom().nextFloat() < explosive * CHANCE_PER_LEVEL;
			if (!explode && !excavation) {
				return;
			}

			Set<BlockPos> blocks = new LinkedHashSet<>();
			if (excavation) {
				for (int dx = -EXCAVATION_REACH; dx <= EXCAVATION_REACH; dx++) {
					for (int dy = -EXCAVATION_REACH; dy <= EXCAVATION_REACH; dy++) {
						for (int dz = -EXCAVATION_REACH; dz <= EXCAVATION_REACH; dz++) {
							blocks.add(pos.offset(dx, dy, dz));
						}
					}
				}
			}
			if (explode) {
				int r = (int) Math.ceil(EXPLOSION_RADIUS);
				for (int dx = -r; dx <= r; dx++) {
					for (int dy = -r; dy <= r; dy++) {
						for (int dz = -r; dz <= r; dz++) {
							if (dx * dx + dy * dy + dz * dz <= EXPLOSION_RADIUS * EXPLOSION_RADIUS + 0.5) {
								blocks.add(pos.offset(dx, dy, dz));
							}
						}
					}
				}
				// Looks like an explosion, but it is only particles and sound
				level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
						1, 0, 0, 0, 0);
				level.playSound(null, pos, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.BLOCKS, 0.6f, 1.3f);
			}
			blocks.remove(pos); // the block you mined was already broken normally

			busy = true;
			try {
				for (BlockPos target : blocks) {
					breakIntoInventory(level, serverPlayer, tool, target, smelter);
				}
			} finally {
				busy = false;
			}
		});
	}

	private static void breakIntoInventory(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos pos,
			boolean smelter) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || !state.is(BlockTags.MINEABLE_WITH_PICKAXE)
				|| state.getDestroySpeed(level, pos) < 0   // unbreakable (bedrock, ...)
				|| level.getBlockEntity(pos) != null) {    // chests, spawners, furnaces, ...
			return;
		}
		// Work out the drops first (this is what makes fortune and silk touch work), then remove the block
		List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, player, tool));
		level.destroyBlock(pos, false, player);
		state.spawnAfterBreak(level, pos, tool, true); // experience from ores etc.

		boolean smeltedAny = false;
		for (ItemStack drop : drops) {
			ItemStack rest = drop.copy();
			if (smelter && SMELTS.containsKey(rest.getItem())) {
				rest = smelted(rest);
				smeltedAny = true;
			}
			player.getInventory().add(rest);   // takes what fits and shrinks the stack
			if (!rest.isEmpty()) {
				Block.popResource(level, pos, rest); // inventory full: the rest drops on the ground
			}
		}
		if (smeltedAny) {
			flames(level, pos);
		}
	}

	// ---------------------------------------------------------------- Ore Smelter helpers

	private static ItemStack smelted(ItemStack stack) {
		Item result = SMELTS.get(stack.getItem());
		return result == null ? stack : new ItemStack(result, stack.getCount());
	}

	/** Turns the items the block just dropped (they are brand new, 0 ticks old) into smelted ones. */
	private static boolean smeltDroppedItems(ServerLevel level, BlockPos pos) {
		boolean any = false;
		for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.75),
				e -> e.tickCount == 0)) {
			ItemStack stack = drop.getItem();
			if (SMELTS.containsKey(stack.getItem())) {
				drop.setItem(smelted(stack));
				any = true;
			}
		}
		return any;
	}

	private static void flames(ServerLevel level, BlockPos pos) {
		level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				8, 0.25, 0.25, 0.25, 0.02);
	}

	static int getLevel(ServerLevel server, ItemStack stack, ResourceKey<Enchantment> key) {
		Optional<? extends Holder<Enchantment>> holder =
				server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
		if (holder.isEmpty()) {
			return 0;
		}
		return EnchantmentHelper.getItemEnchantmentLevel(holder.get(), stack);
	}
}
