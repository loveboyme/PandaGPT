package cn.panda.pandagpt.util;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerContext {

    public static String getContext(Player player) {
        StringBuilder contextBuilder = new StringBuilder("Current player status:\n");
        contextBuilder.append("  - Health: ").append((int) player.getHealth()).append(" / ").append((int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()).append("\n");
        contextBuilder.append("  - Experience: ").append(player.getTotalExperience()).append(" (Level: ").append(player.getLevel()).append(")\n");

        // Inventory summary
        contextBuilder.append("  - Inventory: ");
        String inventorySummary = summarizeInventory(player);
        contextBuilder.append(inventorySummary.isEmpty() ? "Empty" : inventorySummary).append("\n");

        // Equipment information
        contextBuilder.append("  - Equipment: ").append(summarizeEquipment(player)).append("\n");

        // Potion effects
        Collection<PotionEffect> activePotionEffects = player.getActivePotionEffects();
        if (!activePotionEffects.isEmpty()) {
            List<String> potionEffectSummaries = new ArrayList<>();
            for (PotionEffect effect : activePotionEffects) {
                potionEffectSummaries.add(effect.getType().getName() + " (" + effect.getAmplifier() + ", Duration: " + effect.getDuration() / 20 + " seconds)");
            }
            contextBuilder.append("  - Potion Effects: ").append(String.join(", ", potionEffectSummaries)).append("\n");
        }

        // Nearby entities
        Collection<Entity> nearbyEntities = player.getNearbyEntities(20, 20, 20);
        String nearbySummary = summarizeNearbyEntities(nearbyEntities);
        if (!nearbySummary.isEmpty()) {
            contextBuilder.append("  - Nearby Entities: ").append(nearbySummary).append("\n");
        }

        contextBuilder.append("\n");
        return contextBuilder.toString();
    }

    private static String summarizeInventory(Player player) {
        List<String> inventoryItems = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                inventoryItems.add(item.getType().toString() + " x " + item.getAmount());
            }
            if (inventoryItems.size() >= 5) break;
        }
        return String.join(", ", inventoryItems);
    }

    private static String summarizeEquipment(Player player) {
        StringBuilder equipment = new StringBuilder();
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (helmet != null && helmet.getType() != Material.AIR)
            equipment.append("Helmet: ").append(helmet.getType().toString()).append(", ");
        if (chestplate != null && chestplate.getType() != Material.AIR)
            equipment.append("Chestplate: ").append(chestplate.getType().toString()).append(", ");
        if (leggings != null && leggings.getType() != Material.AIR)
            equipment.append("Leggings: ").append(leggings.getType().toString()).append(", ");
        if (boots != null && boots.getType() != Material.AIR)
            equipment.append("Boots: ").append(boots.getType().toString()).append(", ");
        if (mainHand != null && mainHand.getType() != Material.AIR)
            equipment.append("Main Hand: ").append(mainHand.getType().toString()).append(", ");
        if (offHand != null && offHand.getType() != Material.AIR)
            equipment.append("Off Hand: ").append(offHand.getType().toString());

        return equipment.length() > 0 ? equipment.substring(0, equipment.length() - 2) : "None";
    }

    private static String summarizeNearbyEntities(Collection<Entity> entities) {
        List<String> nearby = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                // getName() is deprecated in updated Bukkit API, but it's commonly used in 1.12.2 for getting the entity type name
                String type = entity.getType().getName();
                String coords = String.format("%.1f, %.1f, %.1f", entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ());
                nearby.add(type + " (" + coords + ")");
            }
            if (nearby.size() >= 5) break;
        }
        return String.join(", ", nearby);
    }
}
