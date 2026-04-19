package pl.zubermc.npc;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.v1_21_R4.CraftServer;
import org.bukkit.craftbukkit.v1_21_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class ZubrNPC extends JavaPlugin implements Listener {

    private ServerPlayer npc;
    private int npcId;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommand();
    }

    @Override
    public void onDisable() {
        removeNPC();
    }

    // ================= KOMENDA =================

    private void registerCommand() {
        try {
            CommandMap map = (CommandMap) Bukkit.getServer()
                    .getClass()
                    .getDeclaredField("commandMap")
                    .get(Bukkit.getServer());

            map.register("zubrnpc", new Command("npczuber") {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {

                    if (!(sender instanceof Player p)) return false;

                    if (args.length == 0) {
                        p.sendMessage("/npczuber stworz <nazwa>");
                        p.sendMessage("/npczuber usun");
                        return true;
                    }

                    if (args[0].equalsIgnoreCase("stworz")) {

                        if (args.length < 2) {
                            p.sendMessage("Podaj nazwę NPC!");
                            return true;
                        }

                        String name = args[1];
                        spawnNPC(p.getLocation(), name);

                        p.sendMessage("§aNPC stworzony: " + name);
                        return true;
                    }

                    if (args[0].equalsIgnoreCase("usun")) {
                        removeNPC();
                        p.sendMessage("§cNPC usunięty!");
                        return true;
                    }

                    return false;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= NPC SPAWN =================

    private void spawnNPC(Location loc, String name) {
        removeNPC();

        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();

        GameProfile profile = new GameProfile(UUID.randomUUID(), name);

        npc = new ServerPlayer(server, world, profile);

        npc.setPos(loc.getX(), loc.getY(), loc.getZ());
        npc.setYRot(loc.getYaw());
        npc.setXRot(loc.getPitch());

        world.addFreshEntity(npc);

        npcId = npc.getId();

        for (Player p : Bukkit.getOnlinePlayers()) {
            var conn = ((CraftPlayer) p).getHandle().connection;

            // pokazanie NPC
            conn.send(new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                    npc
            ));

            conn.send(new ClientboundAddEntityPacket(npc));

            conn.send(new ClientboundRotateHeadPacket(npc,
                    (byte) (loc.getYaw() * 256 / 360)
            ));

            // ukrycie z TAB po chwili
            Bukkit.getScheduler().runTaskLater(this, () ->
                    conn.send(new ClientboundPlayerInfoRemovePacket(List.of(npc.getUUID())))
            , 40L);
        }
    }

    // ================= USUWANIE =================

    private void removeNPC() {
        if (npc == null) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            var conn = ((CraftPlayer) p).getHandle().connection;
            conn.send(new ClientboundRemoveEntitiesPacket(npcId));
        }

        npc.kill();
        npc = null;
    }

    // ================= CLICK =================

    @EventHandler
    public void onClick(PlayerInteractEntityEvent e) {
        if (npc == null) return;
        if (e.getRightClicked().getEntityId() != npcId) return;

        e.setCancelled(true);
        e.getPlayer().sendMessage("§eKliknąłeś NPC: §6" + npc.getGameProfile().getName());
    }
}
