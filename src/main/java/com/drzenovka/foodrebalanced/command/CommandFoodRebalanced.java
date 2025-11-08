package com.drzenovka.foodrebalanced.command;

import com.drzenovka.foodrebalanced.handler.FoodEffectHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class CommandFoodRebalanced extends CommandBase {

    @Override
    public String getCommandName() {
        return "fb";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fb reload - Reloads the food_overrides.json configuration (OP only)";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (canCommandSenderUseCommand(sender)) {
                FoodEffectHandler.reloadConfig();
                sender.addChatMessage(new ChatComponentText("[FoodRebalanced] food_overrides.json reloaded."));
            } else {
                sender.addChatMessage(new ChatComponentText("[FoodRebalanced] You do not have permission to execute this command."));
            }
        } else {
            sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, new String[]{"reload"});
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            // func_152596_g checks if the player is OP in 1.7.10
            return server.getConfigurationManager().func_152596_g(player.getGameProfile());
        }
        // Allow console to execute the command
        return true;
    }
}
