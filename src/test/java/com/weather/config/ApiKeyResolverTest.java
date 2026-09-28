package com.weather.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiKeyResolverTest {

    private static final String KEY = "a".repeat(32);

    @TempDir
    Path tempDir;

    private ApiKeyResolver resolver(String envValue, Path startDirectory) {
        return new ApiKeyResolver(envValue, startDirectory);
    }

    private void writeEnvFile(Path directory, String content) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(ApiKeyResolver.ENV_FILE), content);
    }

    @Test
    @DisplayName("Ortam değişkeni varsa doğrudan onu kullanır")
    void ortamDegiskeniOncelikli() {
        assertEquals(KEY, resolver(KEY, tempDir).resolve());
    }

    @Test
    @DisplayName("Ortam değişkeni .env dosyasına göre önceliklidir")
    void ortamDegiskeniDosyadanOncelikli() throws IOException {
        writeEnvFile(tempDir, "OWM_API_KEY=dosyadaki-deger\n");

        assertEquals(KEY, resolver(KEY, tempDir).resolve());
    }

    @Test
    @DisplayName(".env dosyası okunur (dotenv biçimi)")
    void envDosyasindanOkunur() throws IOException {
        writeEnvFile(tempDir, "# yorum satırı\n\nOWM_API_KEY=" + KEY + "\n");

        assertEquals(KEY, resolver(null, tempDir).resolve());
    }

    @Test
    @DisplayName("Tırnak içine alınmış değer çözülür")
    void tirnakliDegerCozulur() throws IOException {
        writeEnvFile(tempDir, "OWM_API_KEY=\"" + KEY + "\"\n");
        assertEquals(KEY, resolver(null, tempDir).resolve());

        writeEnvFile(tempDir, "OWM_API_KEY='" + KEY + "'\n");
        assertEquals(KEY, resolver(null, tempDir).resolve());
    }

    @Test
    @DisplayName("Anahtar adı birebir eşleşmeli, benzer isimler kabul edilmez")
    void benzerIsimKabulEdilmez() throws IOException {
        writeEnvFile(tempDir, "OWM_API_KEY_OLD=" + KEY + "\nMY_OWM_API_KEY=" + KEY + "\n");

        assertNull(resolver(null, tempDir).resolve());
    }

    @Test
    @DisplayName("Boş ortam değişkeni .env dosyasına düşer")
    void bosOrtamDegiskeniDosyayaDuser() throws IOException {
        writeEnvFile(tempDir, "OWM_API_KEY=" + KEY + "\n");

        assertEquals(KEY, resolver("   ", tempDir).resolve());
    }

    @Test
    @DisplayName("Boş değer .env içinde de olsa geçersiz sayılır")
    void bosDegerGecersiz() throws IOException {
        writeEnvFile(tempDir, "OWM_API_KEY=\n");

        assertNull(resolver(null, tempDir).resolve());
    }

    @Test
    @DisplayName("Üst dizinlerdeki .env de bulunur (en fazla 3 seviye)")
    void ustDizindekiEnvBulunur() throws IOException {
        writeEnvFile(tempDir, "OWM_API_KEY=" + KEY + "\n");
        Path nested = tempDir.resolve("target").resolve("classes").resolve("nested");

        assertEquals(KEY, resolver(null, nested).resolve());
    }

    @Test
    @DisplayName("Hiçbir kaynak yoksa null döner, exception fırlatmaz")
    void kaynakYoksaNull() {
        assertNull(resolver(null, tempDir).resolve());
    }

    @Test
    @DisplayName("Okunamayan/eksik dizin çökmez")
    void olmayanDizinCokmez() {
        assertNull(resolver(null, tempDir.resolve("yok").resolve("boyle").resolve("bir")).resolve());
    }

    @Test
    @DisplayName("Anahtarın kaynağı teşhis için bildirilir")
    void kaynakBildirilir() throws IOException {
        assertEquals(ApiKeyResolver.ENV_VAR + " ortam değişkeni",
                resolver(KEY, tempDir).describeSource());

        Path withFile = tempDir.resolve("dosyali");
        writeEnvFile(withFile, "OWM_API_KEY=" + KEY + "\n");
        assertEquals(ApiKeyResolver.ENV_FILE + " dosyası",
                resolver(null, withFile).describeSource());

        Path withoutFile = tempDir.resolve("dosyasiz");
        Files.createDirectories(withoutFile);
        assertEquals("hiçbir kaynak bulunamadı",
                resolver(null, withoutFile).describeSource());
    }

    @Test
    @DisplayName("Üst dizin araması en fazla 3 seviye ile sınırlıdır")
    void ustDizinAramasiSinirlidir() throws IOException {
        Path kok = tempDir.resolve("derin");
        Path l1 = kok.resolve("l1");
        writeEnvFile(l1, "OWM_API_KEY=" + KEY + "\n");

        Path l3 = l1.resolve("l2").resolve("l3");
        Files.createDirectories(l3);
        // l3 -> l3, l2, l1 denenir; bulunur.
        assertEquals(KEY, resolver(null, l3).resolve());

        Path l5 = l3.resolve("l4").resolve("l5");
        Files.createDirectories(l5);
        // l5 -> l5, l4, l3, l2 denenir; l1 sınırın dışında kalır.
        assertNull(resolver(null, l5).resolve());
    }
}
