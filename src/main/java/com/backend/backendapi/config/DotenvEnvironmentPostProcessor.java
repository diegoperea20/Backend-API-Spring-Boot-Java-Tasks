package com.backend.backendapi.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga el archivo {@code .env} de la raiz del proyecto con semantica dotenv
 * (compatible con el {@code .env} del backend FastAPI: comillas, comentarios y
 * prefijo {@code export}).
 *
 * <p>Se agrega con la prioridad mas baja posible para que las variables de
 * entorno del sistema y los archivos {@code application*.properties} siempre
 * tengan precedencia. Tambien traduce {@code DATABASE_URL} del formato
 * SQLAlchemy de SQLite ({@code sqlite:///./app.db}) al formato JDBC que
 * necesita {@code sqlite-jdbc}.</p>
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "dotenvFile";

    private static final String DATABASE_URL = "DATABASE_URL";
    private static final String EXPORT_PREFIX = "export ";
    private static final String SQLITE_PREFIX = "sqlite:///";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenv = Path.of(System.getProperty("user.dir"), ".env");
        if (!Files.isRegularFile(dotenv)) {
            return;
        }

        Map<String, Object> values;
        try {
            values = parse(Files.readAllLines(dotenv, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo .env: " + dotenv, ex);
        }

        Object databaseUrl = values.get(DATABASE_URL);
        if (databaseUrl instanceof String url) {
            values.put(DATABASE_URL, toJdbcSqliteUrl(url));
        }

        if (!values.isEmpty()) {
            environment.getPropertySources()
                    .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        }
    }

    /**
     * Se ejecuta despues de la carga normal de configuracion, de modo que el
     * contenido del {@code .env} queda como ultimo recurso.
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    static Map<String, Object> parse(List<String> lines) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String rawLine : lines) {
            String line = stripBom(rawLine).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith(EXPORT_PREFIX)) {
                line = line.substring(EXPORT_PREFIX.length()).strip();
            }

            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator).strip();
            if (!isValidKey(key)) {
                continue;
            }
            values.put(key, parseValue(line.substring(separator + 1).strip()));
        }
        return values;
    }

    static String toJdbcSqliteUrl(String url) {
        String value = url.strip();
        if (value.isEmpty() || value.startsWith("jdbc:")) {
            return value;
        }
        if (value.equals("sqlite:") || value.equals("sqlite://")) {
            return "jdbc:sqlite::memory:";
        }
        if (value.startsWith(SQLITE_PREFIX)) {
            String path = value.substring(SQLITE_PREFIX.length());
            return path.isEmpty() ? "jdbc:sqlite::memory:" : "jdbc:sqlite:" + path;
        }
        return value;
    }

    private static boolean isValidKey(String key) {
        if (key.isEmpty() || (!Character.isLetter(key.charAt(0)) && key.charAt(0) != '_')) {
            return false;
        }
        for (int i = 1; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '.') {
                return false;
            }
        }
        return true;
    }

    private static String parseValue(String value) {
        if (value.length() >= 2) {
            char quote = value.charAt(0);
            if ((quote == '\'' || quote == '"') && value.charAt(value.length() - 1) == quote) {
                String inner = value.substring(1, value.length() - 1);
                return quote == '"' ? unescape(inner) : inner;
            }
        }

        int comment = indexOfComment(value);
        return comment >= 0 ? value.substring(0, comment).strip() : value;
    }

    /**
     * En dotenv, {@code #} inicia un comentario solo cuando esta precedido por
     * un espacio, para no romper valores como {@code pass#word}.
     */
    private static int indexOfComment(String value) {
        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) == '#' && Character.isWhitespace(value.charAt(i - 1))) {
                return i;
            }
        }
        return -1;
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                result.append(c);
                continue;
            }
            char escaped = value.charAt(++i);
            switch (escaped) {
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case '"' -> result.append('"');
                case '\\' -> result.append('\\');
                default -> result.append(escaped);
            }
        }
        return result.toString();
    }

    private static String stripBom(String line) {
        return !line.isEmpty() && line.charAt(0) == '\uFEFF' ? line.substring(1) : line;
    }
}
