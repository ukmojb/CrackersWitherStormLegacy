package com.wdcftgg.witherstormmod.common.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraftforge.common.command.SelectorHandler;
import net.minecraftforge.common.command.SelectorHandlerManager;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class WitherStormSelectorHandler implements SelectorHandler {
    private static final Pattern SELECTOR_PATTERN = Pattern.compile("^@w(?:\\[([^ ]*)\\])?$");
    private static final String VANILLA_PREFIX =
            "@e[type=witherstormmod:wither_storm";

    public static void register() {
        SelectorHandlerManager.register("@w", new WitherStormSelectorHandler());
    }

    private WitherStormSelectorHandler() {
    }

    @Override
    public <T extends Entity> List<T> matchEntities(ICommandSender sender, String selector,
                                                    Class<? extends T> targetClass)
            throws CommandException {
        List<T> matches = EntitySelector.matchEntitiesDefault(sender, translate(selector), targetClass);
        for (T match : matches) {
            if (match instanceof WitherStormEntity && match.isEntityAlive()) {
                return Collections.singletonList(match);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean matchesMultiplePlayers(String selector) throws CommandException {
        translate(selector);
        return false;
    }

    @Override
    public boolean isSelector(String selector) {
        return SELECTOR_PATTERN.matcher(selector).matches();
    }

    private static String translate(String selector) throws CommandException {
        Matcher matcher = SELECTOR_PATTERN.matcher(selector);
        if (!matcher.matches()) {
            return selector;
        }

        String arguments = matcher.group(1);
        if (arguments == null || arguments.isEmpty()) {
            return VANILLA_PREFIX + "]";
        }

        for (String argument : arguments.split(",")) {
            String key = argument;
            int equals = key.indexOf('=');
            if (equals >= 0) {
                key = key.substring(0, equals);
            }
            if ("type".equals(key) || "c".equals(key)) {
                throw new CommandException("commands.generic.selector_argument", argument);
            }
        }
        return VANILLA_PREFIX + "," + arguments + "]";
    }
}
