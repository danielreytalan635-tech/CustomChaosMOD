package me.yourname.customchaos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.core.Holder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Central Fabric/Brigadier command registry.
 *
 * This class intentionally uses Minecraft 26.2's server command APIs rather
 * than Bukkit/Paper APIs. Commands are grouped by reusable handlers so the
 * project can grow without creating hundreds of nearly identical classes.
 */
public final class ChaosCommands {
    private static final String ROOT = "chaos";
    private static final int HELP_PAGE_SIZE = 12;
    private static final Map<String, String> COMMANDS = new LinkedHashMap<>();

    private ChaosCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerRoot(dispatcher);
            registerCommands(dispatcher);
        });
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal(ROOT)
                .executes(ChaosCommands::helpDefault)
                .then(Commands.literal("help")
                    .executes(ChaosCommands::helpDefault)
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ChaosCommands::helpPage)))
                .then(Commands.literal("info").executes(ChaosCommands::info))
                .then(Commands.literal("menu").executes(ChaosCommands::menu))
        );
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Original CustomChaos commands.
        targetVelocity(dispatcher, "yeet", "Launches a player upward.", 0.0, 1.8, 0.0);
        targetVelocity(dispatcher, "launch", "Launches a player upward.", 0.0, 1.8, 0.0);
        targetVelocity(dispatcher, "bounce", "Bounces a player upward.", 0.0, 1.25, 0.0);
        targetVelocity(dispatcher, "jump", "Gives a player a huge jump impulse.", 0.0, 1.4, 0.0);
        targetVelocity(dispatcher, "superjump", "Gives a player an extreme jump impulse.", 0.0, 2.2, 0.0);
        targetVelocity(dispatcher, "slap", "Knocks a player backward.", -1.2, 0.8, -1.2);

        registerTargetEffect(dispatcher, "glowing", "Glowing outline.", MobEffects.GLOWING, 300, 0);
        registerTargetEffect(dispatcher, "speed", "Temporary speed boost.", MobEffects.SPEED, 300, 1);
        registerTargetEffect(dispatcher, "slow", "Temporary slowness.", MobEffects.SLOWNESS, 200, 2);
        registerTargetEffect(dispatcher, "jumpboost", "Temporary jump boost.", MobEffects.JUMP_BOOST, 300, 2);
        registerTargetEffect(dispatcher, "levitate", "Brief levitation.", MobEffects.LEVITATION, 80, 1);
        registerTargetEffect(dispatcher, "blind", "Temporary blindness.", MobEffects.BLINDNESS, 100, 0);
        registerTargetEffect(dispatcher, "nausea", "Temporary nausea.", MobEffects.NAUSEA, 160, 0);
        registerTargetEffect(dispatcher, "nightvision", "Night vision.", MobEffects.NIGHT_VISION, 6000, 0);
        registerTargetEffect(dispatcher, "fireresist", "Fire resistance.", MobEffects.FIRE_RESISTANCE, 6000, 0);
        registerTargetEffect(dispatcher, "waterbreathing", "Water breathing.", MobEffects.WATER_BREATHING, 6000, 0);
        registerTargetEffect(dispatcher, "resistance", "Temporary resistance.", MobEffects.RESISTANCE, 300, 1);
        registerTargetEffect(dispatcher, "regeneration", "Regeneration burst.", MobEffects.REGENERATION, 200, 1);
        registerTargetEffect(dispatcher, "absorption", "Absorption hearts.", MobEffects.ABSORPTION, 600, 2);
        registerTargetEffect(dispatcher, "strength", "Temporary strength.", MobEffects.STRENGTH, 300, 1);
        registerTargetEffect(dispatcher, "haste", "Temporary haste.", MobEffects.HASTE, 300, 1);
        registerTargetEffect(dispatcher, "luck", "Temporary luck.", MobEffects.LUCK, 600, 1);
        registerTargetEffect(dispatcher, "slowfall", "Slow falling.", MobEffects.SLOW_FALLING, 300, 0);

        // Utility / player actions.
        playerOrTarget(dispatcher, "heal", "Fully heals a player.", ChaosCommands::heal);
        playerOrTarget(dispatcher, "feed", "Fills a player's hunger.", ChaosCommands::feed);
        playerOrTarget(dispatcher, "clear", "Clears all status effects.", ChaosCommands::clearEffects);
        selfOnly(dispatcher, "flight", "Toggles creative-style flight.", ChaosCommands::flight);
        selfOnly(dispatcher, "confetti", "Bursts colorful particles.", c -> particles(c, ParticleTypes.HAPPY_VILLAGER, 50));
        selfOnly(dispatcher, "hearts", "Bursts heart particles.", c -> particles(c, ParticleTypes.HEART, 30));
        selfOnly(dispatcher, "smoke", "Creates a smoke cloud.", c -> particles(c, ParticleTypes.CLOUD, 50));
        selfOnly(dispatcher, "flame", "Creates a flame burst.", c -> particles(c, ParticleTypes.FLAME, 50));
        selfOnly(dispatcher, "soulflame", "Creates a soul flame burst.", c -> particles(c, ParticleTypes.SOUL_FIRE_FLAME, 50));
        selfOnly(dispatcher, "portal", "Creates portal particles.", c -> particles(c, ParticleTypes.PORTAL, 80));
        selfOnly(dispatcher, "endrod", "Creates end rod particles.", c -> particles(c, ParticleTypes.END_ROD, 60));
        selfOnly(dispatcher, "crit", "Creates critical-hit particles.", c -> particles(c, ParticleTypes.CRIT, 60));
        selfOnly(dispatcher, "witch", "Creates witch particles.", c -> particles(c, ParticleTypes.WITCH, 50));
        selfOnly(dispatcher, "totem", "Creates totem particles.", c -> particles(c, ParticleTypes.TOTEM_OF_UNDYING, 40));
        selfOnly(dispatcher, "spark", "Creates electric spark particles.", c -> particles(c, ParticleTypes.ELECTRIC_SPARK, 60));
        selfOnly(dispatcher, "dragonbreath", "Creates a dragon-breath-like particle burst.", c -> particles(c, ParticleTypes.CLOUD, 50));
        selfOnly(dispatcher, "snowstorm", "Creates a snow particle burst.", c -> particles(c, ParticleTypes.SNOWFLAKE, 70));
        selfOnly(dispatcher, "ash", "Creates an ash burst.", c -> particles(c, ParticleTypes.ASH, 70));
        selfOnly(dispatcher, "bubbles", "Creates bubble particles.", c -> particles(c, ParticleTypes.BUBBLE, 60));

        // World control.
        selfOnly(dispatcher, "day", "Sets the world to daytime.", c -> worldCommand(c, "time set day"));
        selfOnly(dispatcher, "night", "Sets the world to nighttime.", c -> worldCommand(c, "time set night"));
        selfOnly(dispatcher, "sunrise", "Sets morning time.", c -> worldCommand(c, "time set 0"));
        selfOnly(dispatcher, "midnight", "Sets midnight.", c -> worldCommand(c, "time set midnight"));
        selfOnly(dispatcher, "clearweather", "Clears the weather.", c -> worldCommand(c, "weather clear 6000"));
        selfOnly(dispatcher, "rain", "Starts rain.", c -> worldCommand(c, "weather rain 6000"));
        selfOnly(dispatcher, "thunderstorm", "Starts a thunderstorm.", c -> worldCommand(c, "weather thunder 6000"));
        selfOnly(dispatcher, "bloodmoon", "Turns the current world into a stormy night.", ChaosCommands::bloodmoon);
        selfOnly(dispatcher, "earthquake", "Shakes nearby players.", ChaosCommands::earthquake);

        // Fun utilities.
        selfOnly(dispatcher, "coinflip", "Flips a coin.", ChaosCommands::coinflip);
        selfOnly(dispatcher, "roll", "Rolls a six-sided die.", ChaosCommands::rollDefault);
        dispatcher.register(Commands.literal(ROOT).then(
            Commands.literal("roll")
                .then(Commands.argument("sides", IntegerArgumentType.integer(2, 1000))
                    .executes(ChaosCommands::rollCustom))
        ));
        dispatcher.register(Commands.literal(ROOT).then(
            Commands.literal("rps")
                .then(Commands.argument("choice", StringArgumentType.word())
                    .executes(ChaosCommands::rps))
        ));
        selfOnly(dispatcher, "coords", "Shows your coordinates.", ChaosCommands::coords);
        selfOnly(dispatcher, "dimension", "Shows your dimension.", ChaosCommands::dimension);
        selfOnly(dispatcher, "facing", "Shows your facing direction.", ChaosCommands::facing);
        selfOnly(dispatcher, "online", "Shows online player count.", ChaosCommands::online);
        selfOnly(dispatcher, "randomteleport", "Teleports you a short random distance.", ChaosCommands::randomTeleport);

        // Sound/prank commands: kept local to the command source.
        selfOnly(dispatcher, "creeperhiss", "Plays a creeper priming sound locally.", c -> soundCommand(c, "playsound minecraft:entity.creeper.primed master @s ~ ~ ~ 1 1"));
        selfOnly(dispatcher, "anvil", "Plays an anvil sound locally.", c -> soundCommand(c, "playsound minecraft:block.anvil.land master @s ~ ~ ~ 1 1"));
        selfOnly(dispatcher, "thunder", "Plays thunder locally.", c -> soundCommand(c, "playsound minecraft:entity.lightning_bolt.thunder master @s ~ ~ ~ 1 1"));
        selfOnly(dispatcher, "bell", "Plays a bell sound locally.", c -> soundCommand(c, "playsound minecraft:block.bell.use master @s ~ ~ ~ 1 1"));
        selfOnly(dispatcher, "ghast", "Plays a ghast sound locally.", c -> soundCommand(c, "playsound minecraft:entity.ghast.warn master @s ~ ~ ~ 1 1"));
        selfOnly(dispatcher, "villager", "Plays a villager sound locally.", c -> soundCommand(c, "playsound minecraft:entity.villager.no master @s ~ ~ ~ 1 1"));
        selfOnly(dispatcher, "goat", "Plays a goat sound locally.", c -> soundCommand(c, "playsound minecraft:entity.goat.screaming.prepare_ram master @s ~ ~ ~ 1 1"));
        selfOnly(dispatcher, "guardian", "Plays a guardian sound locally.", c -> soundCommand(c, "playsound minecraft:entity.guardian.attack master @s ~ ~ ~ 1 1"));
        selfOnly(dispatcher, "portalnoise", "Plays a portal sound locally.", c -> soundCommand(c, "playsound minecraft:block.portal.ambient master @s ~ ~ ~ 1 1"));

        // Additional self-effect aliases. Each is a real action, not a placeholder.
        Map<String, FunctionLike> extra = new LinkedHashMap<>();
        extra.put("dizzy", c -> addEffect(c, MobEffects.NAUSEA, 120, 1, true));
        extra.put("wobbly", c -> addEffect(c, MobEffects.NAUSEA, 220, 0, true));
        extra.put("sprint", c -> addEffect(c, MobEffects.SPEED, 600, 2, true));
        extra.put("superstrength", c -> addEffect(c, MobEffects.STRENGTH, 300, 2, true));
        extra.put("tank", c -> addEffect(c, MobEffects.RESISTANCE, 300, 3, true));
        extra.put("regen", c -> addEffect(c, MobEffects.REGENERATION, 180, 2, true));
        extra.put("glow", c -> addEffect(c, MobEffects.GLOWING, 600, 0, true));
        extra.put("invisiblefx", c -> particles(c, ParticleTypes.SMOKE, 80));
        extra.put("magic", c -> particles(c, ParticleTypes.ENCHANT, 100));
        extra.put("void", c -> particles(c, ParticleTypes.SQUID_INK, 100));
        extra.put("explosionfx", c -> particles(c, ParticleTypes.EXPLOSION, 20));
        extra.put("fireworkfx", c -> particles(c, ParticleTypes.FIREWORK, 80));
        extra.put("poof", c -> particles(c, ParticleTypes.POOF, 100));
        extra.put("sweep", c -> particles(c, ParticleTypes.SWEEP_ATTACK, 40));
        extra.put("lavafx", c -> particles(c, ParticleTypes.LAVA, 60));
        extra.put("dust", c -> particles(c, ParticleTypes.CLOUD, 60));
        extra.put("rainfx", c -> particles(c, ParticleTypes.RAIN, 80));
        extra.put("note", c -> particles(c, ParticleTypes.NOTE, 50));
        extra.put("soul", c -> particles(c, ParticleTypes.SOUL, 60));
        extra.put("portalburst", c -> particles(c, ParticleTypes.PORTAL, 160));
        extra.put("heartstorm", c -> particles(c, ParticleTypes.HEART, 120));
        extra.put("cloudburst", c -> particles(c, ParticleTypes.CLOUD, 120));
        extra.put("firestormfx", c -> particles(c, ParticleTypes.FLAME, 140));
        extra.put("critstorm", c -> particles(c, ParticleTypes.CRIT, 140));
        extra.put("totemstorm", c -> particles(c, ParticleTypes.TOTEM_OF_UNDYING, 120));
        extra.put("enchanted", c -> particles(c, ParticleTypes.ENCHANT, 140));
        extra.put("soulstorm", c -> particles(c, ParticleTypes.SOUL_FIRE_FLAME, 120));
        extra.put("bubbleburst", c -> particles(c, ParticleTypes.BUBBLE, 120));
        extra.put("snowburst", c -> particles(c, ParticleTypes.SNOWFLAKE, 120));
        extra.put("ashstorm", c -> particles(c, ParticleTypes.ASH, 120));
        extra.put("witchstorm", c -> particles(c, ParticleTypes.WITCH, 120));
        extra.put("sparkstorm", c -> particles(c, ParticleTypes.ELECTRIC_SPARK, 120));

        extra.forEach((name, handler) -> selfOnly(dispatcher, name, "CustomChaos effect.", handler));

        // Many safe aliases provide a large, discoverable command set while
        // remaining backed by real behavior.
        String[] speedAliases = {
            "speedy","fast","zoom","rush","dash","haste","quick","rapid","turbo","hyper"
        };
        for (String name : speedAliases) {
            registerSelfEffect(dispatcher, name, MobEffects.SPEED, 220, 1);
        }

        String[] jumpAliases = {
            "bouncy","boing","hopper","moonjump","highjump","jumping","jumper","spring","springy","trampoline"
        };
        for (String name : jumpAliases) {
            registerSelfEffect(dispatcher, name, MobEffects.JUMP_BOOST, 220, 2);
        }

        String[] defenseAliases = {
            "shield","armored","fortify","guard","turtle","iron","stone","durable","safe","barrier"
        };
        for (String name : defenseAliases) {
            registerSelfEffect(dispatcher, name, MobEffects.RESISTANCE, 220, 1);
        }

        String[] visionAliases = {
            "vision","bright","darkvision","seeinthedark","lights"
        };
        for (String name : visionAliases) {
            registerSelfEffect(dispatcher, name, MobEffects.NIGHT_VISION, 1800, 0);
        }

        // Keep a stable command catalog for help/info.
        buildCatalog();
    }

    private static void buildCatalog() {
        COMMANDS.clear();
        String[] names = {
            "help","info","menu","yeet","launch","bounce","jump","superjump","slap",
            "glowing","speed","slow","jumpboost","levitate","blind","nausea","nightvision",
            "fireresist","waterbreathing","resistance","regeneration","absorption","strength",
            "haste","luck","slowfall","heal","feed","clear","flight","confetti","hearts","smoke",
            "flame","soulflame","portal","endrod","crit","witch","totem","spark","dragonbreath",
            "snowstorm","ash","bubbles","day","night","sunrise","midnight","clearweather","rain",
            "thunderstorm","bloodmoon","earthquake","coinflip","roll","rps","coords","dimension",
            "facing","online","randomteleport","creeperhiss","anvil","thunder","bell","ghast",
            "villager","goat","guardian","portalnoise","dizzy","wobbly","sprint","superstrength",
            "tank","regen","glow","invisiblefx","magic","void","explosionfx","fireworkfx","poof",
            "sweep","lavafx","dust","rainfx","note","soul","portalburst","heartstorm","cloudburst",
            "firestormfx","critstorm","totemstorm","enchanted","soulstorm","bubbleburst",
            "snowburst","ashstorm","witchstorm","sparkstorm","speedy","fast","zoom","rush","dash",
            "haste","quick","rapid","turbo","hyper","bouncy","boing","hopper","moonjump","highjump",
            "jumping","jumper","spring","springy","trampoline","shield","armored","fortify","guard",
            "turtle","iron","stone","durable","safe","barrier","vision","bright","darkvision",
            "seeinthedark","lights"
        };
        for (String name : names) {
            COMMANDS.put(name, "CustomChaos command");
        }
    }

    private static void registerSelfEffect(CommandDispatcher<CommandSourceStack> dispatcher, String name,
                                           Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier) {
        selfOnly(dispatcher, name, "Temporary effect.", c -> addEffect(c, effect, duration, amplifier, true));
    }

    private static void registerTargetEffect(CommandDispatcher<CommandSourceStack> dispatcher, String name,
                                             String description, Holder<net.minecraft.world.effect.MobEffect> effect,
                                             int duration, int amplifier) {
        var root = Commands.literal(ROOT).then(
            Commands.literal(name)
                .executes(c -> addEffect(c, effect, duration, amplifier, true))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(c -> {
                        ServerPlayer target = EntityArgument.getPlayer(c, "target");
                        applyEffect(target, effect, duration, amplifier);
                        success(c, "Applied " + name + " to " + target.getName().getString() + ".");
                        return 1;
                    }))
        );
        dispatcher.register(root);
    }

    private static void targetVelocity(CommandDispatcher<CommandSourceStack> dispatcher, String name, String description,
                                       double x, double y, double z) {
        dispatcher.register(Commands.literal(ROOT).then(
            Commands.literal(name)
                .requires(source -> true)
                .executes(c -> velocitySelf(c, new Vec3(x, y, z), name))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(c -> {
                        ServerPlayer target = EntityArgument.getPlayer(c, "target");
                        Vec3 vector = new Vec3(x, y, z);
                        target.setDeltaMovement(vector);
                        success(c, name + " -> " + target.getName().getString());
                        return 1;
                    }))
        ));
    }

    private static void playerOrTarget(CommandDispatcher<CommandSourceStack> dispatcher, String name, String description,
                                       PlayerHandler handler) {
        dispatcher.register(Commands.literal(ROOT).then(
            Commands.literal(name)
                .executes(c -> handler.run(c.getSource().getPlayerOrException(), c))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(c -> handler.run(EntityArgument.getPlayer(c, "target"), c)))
        ));
    }

    private static void selfOnly(CommandDispatcher<CommandSourceStack> dispatcher, String name, String description,
                                 FunctionLike handler) {
        dispatcher.register(Commands.literal(ROOT).then(
            Commands.literal(name).executes(handler::run)
        ));
    }

    private static int helpDefault(CommandContext<CommandSourceStack> context) {
        return sendHelp(context, 1);
    }

    private static int helpPage(CommandContext<CommandSourceStack> context) {
        int page = IntegerArgumentType.getInteger(context, "page");
        return sendHelp(context, page);
    }

    private static int sendHelp(CommandContext<CommandSourceStack> context, int page) {
        List<String> names = new ArrayList<>(COMMANDS.keySet());
        int maxPages = Math.max(1, (int) Math.ceil(names.size() / (double) HELP_PAGE_SIZE));
        page = Math.min(page, maxPages);
        int start = (page - 1) * HELP_PAGE_SIZE;

        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("=== CustomChaos Commands " + page + "/" + maxPages + " ==="), false);
        for (int i = start; i < Math.min(start + HELP_PAGE_SIZE, names.size()); i++) {
            String name = names.get(i);
            source.sendSuccess(() -> Component.literal("/chaos " + name + " - " + COMMANDS.get(name)), false);
        }
        source.sendSuccess(() -> Component.literal("Use /chaos help " + Math.min(page + 1, maxPages) + " for more."), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("CustomChaos 1.0.0"), false);
        context.getSource().sendSuccess(() -> Component.literal("Minecraft target: 26.2"), false);
        context.getSource().sendSuccess(() -> Component.literal("Fabric mod | Java 25"), false);
        context.getSource().sendSuccess(() -> Component.literal("Registered command entries: " + COMMANDS.size()), false);
        return 1;
    }

    private static int menu(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ChaosMenu.open(player);
        return 1;
    }

    /**
     * Executes a registered self-targeting Chaos command from the server-side GUI.
     * The command text is fixed by the GUI and never comes from the player.
     */
    public static int executeMenuCommand(ServerPlayer player, String command) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        level.getServer().getCommands().performPrefixedCommand(
            player.createCommandSourceStack(),
            ROOT + " " + command
        );
        return 1;
    }

    private static int heal(ServerPlayer target, CommandContext<CommandSourceStack> context) {
        target.setHealth(target.getMaxHealth());
        success(context, "Healed " + target.getName().getString() + ".");
        return 1;
    }

    private static int feed(ServerPlayer target, CommandContext<CommandSourceStack> context) {
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20.0F);
        success(context, "Fed " + target.getName().getString() + ".");
        return 1;
    }

    private static int clearEffects(ServerPlayer target, CommandContext<CommandSourceStack> context) {
        target.removeAllEffects();
        success(context, "Cleared effects from " + target.getName().getString() + ".");
        return 1;
    }

    private static int flight(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.getAbilities().mayfly = !player.getAbilities().mayfly;
        if (!player.getAbilities().mayfly) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
        success(context, "Flight " + (player.getAbilities().mayfly ? "enabled" : "disabled") + ".");
        return 1;
    }

    private static int coinflip(CommandContext<CommandSourceStack> context) {
        String result = ThreadLocalRandom.current().nextBoolean() ? "Heads" : "Tails";
        success(context, "Coinflip: " + result);
        return 1;
    }

    private static int rollDefault(CommandContext<CommandSourceStack> context) {
        return doRoll(context, 6);
    }

    private static int rollCustom(CommandContext<CommandSourceStack> context) {
        return doRoll(context, IntegerArgumentType.getInteger(context, "sides"));
    }

    private static int doRoll(CommandContext<CommandSourceStack> context, int sides) {
        int result = ThreadLocalRandom.current().nextInt(1, sides + 1);
        success(context, "You rolled " + result + "/" + sides + ".");
        return 1;
    }

    private static int rps(CommandContext<CommandSourceStack> context) {
        String choice = StringArgumentType.getString(context, "choice").toLowerCase();
        if (!(choice.equals("rock") || choice.equals("paper") || choice.equals("scissors"))) {
            context.getSource().sendFailure(Component.literal("Use rock, paper, or scissors."));
            return 0;
        }
        String[] choices = {"rock", "paper", "scissors"};
        String server = choices[ThreadLocalRandom.current().nextInt(choices.length)];
        String result = choice.equals(server) ? "Tie!" :
            ((choice.equals("rock") && server.equals("scissors")) ||
             (choice.equals("paper") && server.equals("rock")) ||
             (choice.equals("scissors") && server.equals("paper")) ? "You win!" : "You lose!");
        success(context, "You: " + choice + " | Server: " + server + " | " + result);
        return 1;
    }

    private static int coords(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayerOrException();
        success(context, "XYZ: " + String.format("%.1f %.1f %.1f", player.getX(), player.getY(), player.getZ()));
        return 1;
    }

    private static int dimension(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayerOrException();
        success(context, "Dimension: " + player.level().dimension().identifier().toString());
        return 1;
    }

    private static int facing(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayerOrException();
        success(context, "Facing: " + player.getDirection().getName());
        return 1;
    }

    private static int online(CommandContext<CommandSourceStack> context) {
        int count = context.getSource().getServer().getPlayerList().getPlayerCount();
        success(context, "Online players: " + count);
        return 1;
    }

    private static int randomTeleport(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double x = player.getX() + random.nextInt(-24, 25);
        double z = player.getZ() + random.nextInt(-24, 25);
        player.teleportTo(x, player.getY(), z);
        success(context, "Random teleport: " + String.format("%.1f %.1f %.1f", x, player.getY(), z));
        return 1;
    }

    private static int bloodmoon(CommandContext<CommandSourceStack> context) {
        context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), "time set night");
        context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), "weather thunder 6000");
        success(context, "Blood moon activated in this world.");
        return 1;
    }

    private static int earthquake(CommandContext<CommandSourceStack> context) {
        ServerPlayer source = context.getSource().getPlayerOrException();
        for (ServerPlayer player : ((ServerLevel) source.level()).players()) {
            if (player.distanceToSqr(source) <= 100.0D) {
                Vec3 delta = player.getDeltaMovement();
                double x = (ThreadLocalRandom.current().nextDouble() - 0.5D) * 0.6D;
                double z = (ThreadLocalRandom.current().nextDouble() - 0.5D) * 0.6D;
                player.setDeltaMovement(delta.x + x, Math.max(delta.y, 0.35D), delta.z + z);
            }
        }
        particles(context, ParticleTypes.CLOUD, 100);
        success(context, "The ground shakes!");
        return 1;
    }

    private static int velocitySelf(CommandContext<CommandSourceStack> context, Vec3 vector, String name)
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.setDeltaMovement(vector);
        success(context, name + "!");
        return 1;
    }

    private static int addEffect(CommandContext<CommandSourceStack> context,
                                  Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier, boolean notify) {
        ServerPlayer player = context.getSource().getPlayerOrException();
        applyEffect(player, effect, duration, amplifier);
        if (notify) {
            success(context, "Applied effect.");
        }
        return 1;
    }

    private static void applyEffect(ServerPlayer player, Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
    }

    private static int particles(CommandContext<CommandSourceStack> context, ParticleOptions particle, int count) {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(particle, player.getX(), player.getY() + 1.0, player.getZ(),
            count, 0.7, 1.0, 0.7, 0.02);
        success(context, "Chaos effect triggered.");
        return 1;
    }

    private static int worldCommand(CommandContext<CommandSourceStack> context, String command) {
        context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), command);
        success(context, "World command executed.");
        return 1;
    }

    private static int soundCommand(CommandContext<CommandSourceStack> context, String command) {
        context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), command);
        success(context, "Sound played.");
        return 1;
    }

    private static void success(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal("[CustomChaos] " + message), false);
    }

    @FunctionalInterface
    private interface FunctionLike {
        int run(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException;
    }

    @FunctionalInterface
    private interface PlayerHandler {
        int run(ServerPlayer player, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException;
    }
}
