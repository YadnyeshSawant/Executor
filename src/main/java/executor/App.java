package executor;

/**
 * The main entry point for the Python Executor application.
 * <p>
 * This class contains the {@code main} method that launches the application
 * by delegating to the {@link PythonExecutor#main(String[])} method.
 * This wrapper class is useful for creating executable JARs where the main
* class is clearly defined.
 */
public class App {

    /**
     * The main method which serves as the application's entry point.
     *
     * @param args Command-line arguments passed to the application (not used).
     */
    public static void main(String[] args) {
        PythonExecutor.main(args);
    }
}
