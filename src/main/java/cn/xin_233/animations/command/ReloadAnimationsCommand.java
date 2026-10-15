package cn.xin_233.animations.command;

import cn.xin_233.animations.config.AnimationConfigManager;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public final class ReloadAnimationsCommand extends CommandBase {
    private final AnimationConfigManager configs;

    public ReloadAnimationsCommand(AnimationConfigManager configs) {
        this.configs = configs;
    }

    @Override
    public String getName() {
        return "pixelmonanimations";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/pixelmonanimations reload";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1 || !"reload".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getUsage(sender));
        }
        if (!configs.reload()) {
            throw new CommandException("Animation reload failed. Previous configuration retained; see the server log.");
        }
        sender.sendMessage(new TextComponentString("PixelmonAnimations: loaded " + configs.getSkillCount()
                + " skills and " + configs.getEffectCount() + " effects."));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                         @Nullable BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "reload") : Collections.emptyList();
    }
}
