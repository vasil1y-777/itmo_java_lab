package classes.console;

import classes.abs.NamedCommand;
import classes.movie.Movie;
import exceptions.NoSuchCommandException;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Class, which manages implemented commands
 */
public class CommandHandler {
    private final Set<Class<? extends NamedCommand>>
            allCommands = new Reflections("classes.commands").getSubTypesOf(NamedCommand.class);

    /**
     * @param commandName String with name of command
     * @return Command instance
     */
    public NamedCommand getCommand(String commandName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchCommandException {
        for (Class<? extends NamedCommand> command : allCommands) {
            if (camelToSnake(command.getName().split("\\.")[2]).equals(commandName))
                return command.getDeclaredConstructor().newInstance();
        }
        throw new NoSuchCommandException();
    }

    /**
     * Converts camel case string to snake case
     *
     * @param str String in camel case
     * @return String in snake case
     */
    public static String camelToSnake(String str) {
        String result = str.substring(0, 1).toLowerCase();
        for (int i = 1; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch))
                result += "_" + Character.toLowerCase(ch);
            else result += ch;
        }
        return result;
    }

    public static void handle(String inputString,
                                ObjectOutputStream out) throws IOException, NoSuchCommandException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String commandName = inputString.split(" ")[0];
        String[] commandArguments = null;
        if (inputString.split(" ").length > 1) {
            String[] arr = inputString.split(" ");
            commandArguments = Arrays.copyOfRange(arr, 1, arr.length);
        }
        if (!commandName.isBlank()) {
            NamedCommand command = new CommandHandler().getCommand(commandName);
            if (Objects.equals(command.getName(), "exit"))
                command.execute(null);
            if (Objects.equals(command.getName(), "execute_script") && commandArguments != null && commandArguments.length == 1) {
                command.execute(commandArguments[0]);
            }
            if (command.isNeedInput()) {
                if (Objects.equals(command.getName(), "add")) {
                    Movie movie;
                    if (commandArguments != null && commandArguments.length == 1 && commandArguments[0].equals("random")) {
                        out.writeObject(command);
                        out.flush();
                        out.writeObject(commandArguments);
                    } else if (commandArguments == null || commandArguments.length == 0) {
                        InputHandler inputHandler = new InputHandler();
                        movie = inputHandler.readMovie();
                        out.writeObject(command);
                        out.flush();
                        out.writeObject(movie);
                    }
                } else if (Objects.equals(command.getName(), "update") && commandArguments != null) {
                    Movie movie;
                    InputHandler inputHandler = new InputHandler();
                    try {
                        UUID filmUUID = UUID.fromString(commandArguments[0]);
                        movie = inputHandler.readMovie();
                        movie.setId(filmUUID);
                        out.writeObject(command);
                        out.flush();
                        out.writeObject(movie);
                    } catch (IllegalArgumentException e) {
                        out.writeObject(command);
                        out.flush();
                        out.writeObject(null);
                    }
                }
            }
            if (command.hasTransferData()) {
                out.writeObject(command);
                out.flush();
                out.writeObject(commandArguments);
            }else out.writeObject(command);
        }
        out.flush();
    }
}
