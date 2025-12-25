package net.pinkcats.createlazytick.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;

public class LazyTickCommand {

    public static void RegisterCLTCommand(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();


        dispatcher.register(Commands.literal("createlazytick")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(context -> listForcedMachines(context, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> listForcedMachines(context, IntegerArgumentType.getInteger(context, "page")))
                        )
                )
        );



    }


    private static int listForcedMachines(CommandContext<CommandSourceStack> context, int page){
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();



        return 1;
    }





}
