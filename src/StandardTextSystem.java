package tengine;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Input system using simple allCommands followed by a series of strings
 *
 * @author austinletson
 */
public abstract class StandardTextSystem {


    private Scanner stdInput;
    private PrintStream stdOutput;
    private String defaultPrompt = ">_";
    private String unrecognizedInputText = "Input unrecognized. Type help for more information about commands";
    private List<Command> allCommands = new ArrayList();
    private List<Command> activeCommands = new ArrayList();


    /**
     * Use this annotation to create Standard Text Input System Command
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    protected @interface IsCommand {
        String infoText() default "Command does not have info text";

        String[] altCommandTexts() default {};
    }



    /**
     * Constructor for using a simple Scanner for input
     *
     * @param stdIn  InputStream
     * @param stdOut PrintStream
     */
    public StandardTextSystem(InputStream stdIn, PrintStream stdOut) {
        stdInput = new Scanner(stdIn);
        stdOutput = stdOut;
        Method[] allMethods = this.getClass().getMethods();
        for (Method method : allMethods) {
            Annotation[] methodDeclaredAnnotations = method.getDeclaredAnnotations();
            for (Annotation annotation : methodDeclaredAnnotations) {
                if (annotation instanceof IsCommand) {
                    String methodName = method.getName();
                    String infoText = ((IsCommand) annotation).infoText();

                    if ((((IsCommand) annotation).altCommandTexts().length == 0)) {
                        allCommands.add(new Command(methodName, method, infoText));
                    } else {
                        ArrayList<String> cmdTexts = new ArrayList<>(Arrays.asList(((IsCommand) annotation).altCommandTexts()));
                        cmdTexts.add(0, methodName);
                        allCommands.add(new Command(cmdTexts, method, infoText));
                    }


                }
            }
        }

        activateAllCommands();


    }

    /**
     * Activates all commands in Text System
     */
    public void activateAllCommands() {
        activeCommands = allCommands;
    }

    /**
     * disactivates all commands in Text System
     */
    public void disactivateAllCommands() {
        activeCommands.clear();
    }

    public void activateCommand(String commandName) {
        for (Command command : allCommands) {
            for (String commandText : command.getCommandTexts()) {
                if (commandText.equals(commandName) && !activeCommands.contains(command)) {
                    activeCommands.add(command);
                }
            }
        }
    }

    /**
     * Calls upon the InputSystem to prompt the user. (Will not return until user has entered a valid command)
     */
    public void prompt() {
        stdOutput.print(defaultPrompt);
        searchForAndExecuteCommand();
    }

    /**
     * Same as prompt() but uses a custom prompt text
     *
     * @param promptText String
     */
    public void prompt(String promptText) {
        stdOutput.print(promptText);
        searchForAndExecuteCommand();
    }

    /**
     * Searches the Standard Input for a valid command and then executes that command method
     */
    protected void searchForAndExecuteCommand() {

        List<String> words = new ArrayList(Arrays.asList(stdInput.nextLine().split(" ")));

        for (Command cmd : activeCommands) {
            for (String commandText : cmd.getCommandTexts()) {
                if (!words.isEmpty()) {
                    if (commandText.equals(words.get(0))) {
                        words.remove(0);
                        if (cmd.getCommandMethod().getParameterCount() == words.size()) {
                            try {
                                cmd.getCommandMethod().invoke(this, words.toArray());
                                return;
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                stdOutput.println(e.getMessage());
                            }
                        } else {
                            //If need can add more sophisticated information here
                            println("The command " + commandText + " takes " + cmd.getCommandMethod().getParameterCount() +
                                    " arguments.\nYou entered " + words.size() + "Type help for more information about commands.");
                            return;
                        }
                    }

                } else {
                    try {
                        cmd.getCommandMethod().invoke(this, words.toArray());
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        stdOutput.println(e.getMessage());
                    }
                }
            }
        }

        println(unrecognizedInputText);


    }


    /**
     * Prints to standard output
     *
     * @param output String
     */
    public void println(String output) {
        stdOutput.println(output);
    }

    /**
     * Sets defaults prompt text
     *
     * @param defaultPromptTxt String
     */
    public void setDefaultPromptText(String defaultPromptTxt) {
        defaultPrompt = defaultPromptTxt;
    }

    /**
     * Sets defaults prompt text
     *
     * @param textToSet String
     */
    public void setUnRecognizedInputText(String textToSet) {
        unrecognizedInputText = textToSet;
    }

    /**
     * Default command that displays all other commands
     */
    @IsCommand(infoText = "Displays all possible commands")
    public void help() {
        for (Command c : activeCommands) {
            println(c.getCommandTexts().get(0) + " : " + c.getInfoText());
        }

    }

    class Command {


        private List<String> commandTexts;
        private Method commandMethod;
        private String infoText;

        /**
         * Standard Constructor for Command
         *
         * @param cmndText   String
         * @param cmndMethod Method
         * @param infText String
         */
        protected Command(String cmndText, Method cmndMethod, String infText) {
            commandTexts = new ArrayList<>();
            commandTexts.add(cmndText);
            commandMethod = cmndMethod;
            infoText = infText;

        }

        protected Command(List<String> cmndTexts, Method cmndMethod, String infText) {
            commandTexts = cmndTexts;
            commandMethod = cmndMethod;
            infoText = infText;

        }

        public List<String> getCommandTexts() {
            return commandTexts;
        }

        public String getInfoText() {
            return infoText;
        }

        public Method getCommandMethod() {

            return commandMethod;
        }
    }




}
