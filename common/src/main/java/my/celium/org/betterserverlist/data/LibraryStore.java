package my.celium.org.betterserverlist.data;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Reads and writes the {@link Library} as pretty JSON. Writes go to a temp
 * file first and replace the real one in a single move, so a crash never
 * leaves half a file behind. A file that no longer parses is kept next to
 * the new one as {@code .broken} instead of being overwritten.
 */
public final class LibraryStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path file;

    public LibraryStore(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    /** The saved library, or {@code null} when there is none or it is unreadable (then moved aside). */
    public Library load() throws IOException {
        if (!exists()) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Library library = GSON.fromJson(reader, Library.class);
            if (library == null) {
                throw new JsonParseException("empty file");
            }
            library.sanitize();
            return library;
        } catch (JsonParseException | IllegalStateException e) {
            Path broken = file.resolveSibling(file.getFileName() + ".broken");
            Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
            throw new IOException("Unreadable " + file.getFileName() + ", kept as " + broken.getFileName(), e);
        }
    }

    public void save(Library library) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(library, writer);
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String toJson(Library library) {
        return GSON.toJson(library);
    }
}
