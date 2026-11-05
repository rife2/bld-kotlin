import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Hello {

    static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Hello <output-file>");
            System.exit(1);
        }

        try {
            Path path = Path.of(args[0]);
            Files.writeString(path, "Hello World", StandardCharsets.UTF_8);
        } catch (InvalidPathException e) {
            System.err.println("Invalid path: " + args[0]);
            System.exit(2);
        } catch (IOException e) {
            System.err.println("Failed to write file: " + e.getMessage());
            System.exit(3);
        }
    }
}