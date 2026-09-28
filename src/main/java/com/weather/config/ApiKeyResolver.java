package com.weather.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * API anahtarını bulur.
 *
 * <p>Sıra:
 * <ol>
 *   <li>{@code OWM_API_KEY} ortam değişkeni — birincil kaynak.</li>
 *   <li>{@code .env} dosyası (dotenv biçimi) — çalışma dizininden en fazla
 *       {@value #MAX_PARENT_LEVELS} üst dizine kadar aranır.</li>
 * </ol>
 *
 * <p>Neden iki kaynak: ortam değişkenleri yalnızca onları tanımlayan kabuk
 * oturumunda görünür. macOS'ta Dock/Finder'dan (veya IntelliJ'i Dock'tan)
 * başlatılan bir uygulama {@code ~/.zshrc} okumaz; bu durumda anahtar
 * sessizce kayboluyordu. {@code .env} dosyası, başlatma biçiminden bağımsız
 * olarak aynı sonucu verir.
 *
 * <p>{@code .env} sürüm kontrolüne girmez (.gitignore'da).
 */
public final class ApiKeyResolver {

    public static final String ENV_VAR = "OWM_API_KEY";
    public static final String ENV_FILE = ".env";
    static final int MAX_PARENT_LEVELS = 3;

    private final String envValue;
    private final Path startDirectory;

    /** Uygulama için: gerçek ortam değişkeni ve geçerli çalışma dizini. */
    public ApiKeyResolver() {
        this(System.getenv(ENV_VAR), Path.of("").toAbsolutePath());
    }

    /** Test için: her iki kaynak da enjekte edilebilir. */
    ApiKeyResolver(String envValue, Path startDirectory) {
        this.envValue = envValue;
        this.startDirectory = startDirectory;
    }

    /**
     * @return Bulunan anahtar, ya da hiçbir kaynakta yoksa {@code null}
     */
    public String resolve() {
        if (isUsable(envValue)) {
            return envValue.strip();
        }
        return readFromEnvFile();
    }

    /** Anahtarın nereden geldiğini açıklayan kısa metin (günlük/teşhis için). */
    public String describeSource() {
        if (isUsable(envValue)) {
            return ENV_VAR + " ortam değişkeni";
        }
        if (readFromEnvFile() != null) {
            return ENV_FILE + " dosyası";
        }
        return "hiçbir kaynak bulunamadı";
    }

    private String readFromEnvFile() {
        Path directory = startDirectory;

        for (int level = 0; level <= MAX_PARENT_LEVELS && directory != null; level++) {
            String value = readKey(Files.exists(directory.resolve(ENV_FILE))
                    ? directory.resolve(ENV_FILE)
                    : null);
            if (value != null) {
                return value;
            }
            directory = directory.getParent();
        }
        return null;
    }

    private String readKey(Path envFile) {
        if (envFile == null) {
            return null;
        }
        try {
            List<String> lines = Files.readAllLines(envFile);
            for (String line : lines) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator < 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).strip();
                if (ENV_VAR.equals(key)) {
                    String value = unquote(trimmed.substring(separator + 1).strip());
                    return isUsable(value) ? value : null;
                }
            }
        } catch (IOException e) {
            // Okunamayan bir .env ölümcül değil: çağıran tarafta anlamlı bir
            // "anahtar tanımlı değil" mesajı gösterilir.
            return null;
        }
        return null;
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private boolean isUsable(String value) {
        return value != null && !value.isBlank();
    }
}
