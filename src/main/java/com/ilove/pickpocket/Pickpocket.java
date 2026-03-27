package com.ilove.pickpocket;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Mod(Pickpocket.MODID)
public class Pickpocket {
    public static final String MODID = "pickpocket";
    static Logger log = LoggerFactory.getLogger(MODID);
    public static double maxDist = 1.75;
    public static double maxDistSq = maxDist * maxDist;
    public static Map<ServerPlayer, ServerPlayer> interactingPlayers = new HashMap<>();

    public Pickpocket() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer interacting)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        log.info("Interaction between players detected.");
        double distSq = interacting.distanceToSqr(target.getX(), target.getY(), target.getZ());
        if (distSq <= maxDistSq) {
            log.info("Close enough.");
            interacting.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, buf) -> new ChestMenu(
                                    MenuType.GENERIC_9x4,
                                    id,
                                    inv,
                                    target.getInventory(),
                                    4
                            ),
                            Component.literal(target.getName().getString() + "'s Inventory")
                    ),
                    target.blockPosition()
            );
            interactingPlayers.put(interacting, target);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteract event) {
        onEntityInteract(event);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        interactingPlayers.keySet().removeIf(player -> {
            if (event.getServer().getPlayerList().getPlayer(player.getUUID()) == null) return true;
            ServerPlayer target = interactingPlayers.get(player);
            if (event.getServer().getPlayerList().getPlayer(target.getUUID()) == null) return true;
            if (!player.isAlive() || !target.isAlive()) return true;
            if (!(player.containerMenu instanceof ChestMenu)) {
                return true;
            }
            double distSq = player.distanceToSqr(target.getX(), target.getY(), target.getZ());
            if (distSq > maxDistSq) {
                player.closeContainer();
                return true;
            }
            return false;
        });
    }
}
