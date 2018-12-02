/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.events;

import net.fabricmc.fabric.events.PlayerInteractionEvent;
import net.fabricmc.fabric.util.HandlerList;
import net.minecraft.client.network.packet.BlockUpdateClientPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Facing;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class MixinServerPlayerInteractionManager {
	@Shadow
	public World world;
	@Shadow
	public ServerPlayerEntity player;

	@Inject(at = @At("HEAD"), method = "method_14263", cancellable = true)
	public void startBlockBreak(BlockPos pos, Facing facing, CallbackInfo info) {
		for (Object handler : ((HandlerList<PlayerInteractionEvent.Block>) PlayerInteractionEvent.BREAK_BLOCK).getBackingArray()) {
			PlayerInteractionEvent.Block event = (PlayerInteractionEvent.Block) handler;
			ActionResult result = event.interact(player, world, Hand.MAIN, pos, facing);
			if (result != ActionResult.PASS) {
				// The client might have broken the block on its side, so make sure to let it know.
				this.player.networkHandler.sendPacket(new BlockUpdateClientPacket(world, pos));
				info.cancel();
				return;
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
	public void interactBlock(PlayerEntity player, World world, ItemStack stack, Hand hand, BlockPos pos, Facing facing, float hitX, float hitY, float hitZ, CallbackInfoReturnable<ActionResult> info) {
		for (Object handler : ((HandlerList<PlayerInteractionEvent.BlockPositioned>) PlayerInteractionEvent.INTERACT_BLOCK).getBackingArray()) {
			PlayerInteractionEvent.BlockPositioned event = (PlayerInteractionEvent.BlockPositioned) handler;
			ActionResult result = event.interact(player, world, hand, pos, facing, hitX, hitY, hitZ);
			if (result != ActionResult.PASS) {
				info.setReturnValue(result);
				info.cancel();
				return;
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "interactItem", cancellable = true)
	public void interactItem(PlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> info) {
		for (Object handler : ((HandlerList<PlayerInteractionEvent.Item>) PlayerInteractionEvent.INTERACT_ITEM).getBackingArray()) {
			PlayerInteractionEvent.Item event = (PlayerInteractionEvent.Item) handler;
			ActionResult result = event.interact(player, world, hand);
			if (result != ActionResult.PASS) {
				info.setReturnValue(result);
				info.cancel();
				return;
			}
		}
	}
}
