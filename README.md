# 🌤️ JavaFX Weather App

JavaFX ile yazılmış masaüstü hava durumu uygulaması. OpenWeatherMap API'sinden gerçek
zamanlı veri çeker, katmanlı ve test edilebilir bir mimari üzerine kuruludur.

[![CI](https://github.com/emrelab/javafx-weather-app/actions/workflows/ci.yml/badge.svg)](https://github.com/emrelab/javafx-weather-app/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.15-blue.svg)](https://openjfx.io/)

## Ekran görüntüsü

![Uygulama](src/image/weather-v2.png)

## Özellikler

- Şehir bazlı arama, gerçek zamanlı hava durumu verisi
- Sıcaklık, hissedilen sıcaklık, min/max, nem ve rüzgar hızı gösterimi
- Hava durumuna göre dinamik ikonlar (dilden bağımsız `icon` kodu ile eşlenir)
- Ağ işlemleri arka planda; arayüz donmaz
- Kullanıcıya ham exception metni veya hata kodu gösterilmez — her hata durumu
  Türkçe, okunabilir bir mesaja eşlenir (şehir bulunamadı, geçersiz API anahtarı,
  limit aşıldı, bağlantı yok, …)
- 76 birim testi, 3 işletim sisteminde CI

## Mimari

Katmanlar tek yönlü bağımlıdır; UI katmanı somut servislere değil arayüzlere bağlıdır.

```mermaid
flowchart TD
    Main["Main (JavaFX Application)"] --> FXML["weather.fxml"]
    FXML -->|"fx:controller"| Controller["WeatherController<br/><i>UI olayları ve ekran güncellemesi</i>"]
    Controller -->|IWeatherService| Service["OpenWeatherMapService<br/><i>HTTP taşıma + durum kodu eşlemesi</i>"]
    Controller -->|IWeatherIconMapper| IconMapper["WeatherIconMapper<br/><i>Hava durumu → ikon dosyası</i>"]
    Controller -->|ApiKeyResolver| Config["ApiKeyResolver<br/><i>OWM_API_KEY → .env</i>"]
    Service -->|IWeatherResponseParser| Parser["WeatherResponseParser<br/><i>JSON → model</i>"]
    Service -.->|HttpClient| API(["OpenWeatherMap API"])
    Parser --> Model["WeatherData<br/><i>veri taşıma</i>"]
    IconMapper --> Model
    Controller --> Model
```

### SOLID eşlemesi

| Prensip | Uygulama |
|---|---|
| **S** — Single Responsibility | `OpenWeatherMapService` yalnızca HTTP taşıma ve durum kodu eşlemesi yapar; JSON çözümleme `WeatherResponseParser`'a, ikon seçimi `WeatherIconMapper`'a, anahtar bulma `ApiKeyResolver`'a, ekran güncellemesi `WeatherController`'a aittir |
| **O** — Open/Closed | Yeni bir sağlayıcı (ör. farklı bir hava durumu API'si) `IWeatherService` implement edilerek eklenir; mevcut sınıflar değişmez |
| **L** — Liskov Substitution | Testler herhangi bir `IWeatherService` / `IWeatherResponseParser` implementasyonuyla çalışır |
| **I** — Interface Segregation | `IWeatherService`, `IWeatherIconMapper`, `IWeatherResponseParser` — her biri tek ve dar bir sözleşme |
| **D** — Dependency Inversion | `WeatherController` somut sınıflara değil arayüzlere bağlıdır; somut nesneler yalnızca composition root'ta (varsayılan constructor) oluşturulur |

## Teknolojiler

| Teknoloji | Sürüm | Kullanım |
|---|---|---|
| Java | 17 (LTS) | Platform, `java.net.http.HttpClient`, switch expression, text block |
| JavaFX | 17.0.15 | Arayüz, FXML |
| Maven | 3.x (wrapper dahil) | Build, test |
| JUnit 5 | 5.10.2 | Birim ve entegrasyon testleri |
| org.json | 20231013 | JSON çözümleme |
| OpenWeatherMap API | 2.5 | Hava durumu verisi |

## Başlarken

### Gereksinimler

- JDK 17 veya üzeri (`java -version` ile kontrol edin)
- İnternet bağlantısı

Projeyi klonlayıp çalıştırın — Maven kurmanıza gerek yok, wrapper geliyor:

```bash
git clone https://github.com/emrelab/javafx-weather-app.git
cd javafx-weather-app
./mvnw clean javafx:run
```

Windows'ta: `mvnw.cmd clean javafx:run`

### API anahtarı

Uygulama [OpenWeatherMap](https://openweathermap.org/api) API anahtarı gerektirir.
Anahtar **kaynak kodda tutulmaz**. İki kaynak desteklenir ve şu sırayla denenir:

1. `OWM_API_KEY` ortam değişkeni
2. Proje kökündeki `.env` dosyası (çalışma dizininden en fazla 3 üst dizine kadar aranır)

**Önerilen yol — `.env`:** uygulamayı nasıl başlattığınızdan bağımsız çalışır
(terminal, IntelliJ, Dock).

```bash
cp .env.example .env
# .env dosyasını açıp değeri kendi anahtarınızla değiştirin
```

`.env` sürüm kontrolüne girmez (`.gitignore`'da).

**Alternatif — ortam değişkeni:**

```bash
# macOS / Linux
export OWM_API_KEY="buraya_anahtariniz"

# Windows (PowerShell)
$env:OWM_API_KEY="buraya_anahtariniz"

# Windows (cmd)
set OWM_API_KEY=buraya_anahtariniz
```

> **Neden iki yol var:** ortam değişkenleri yalnızca onları tanımlayan kabuk oturumunda
> görünür. macOS'ta Dock/Finder'dan (veya IntelliJ'i Dock'tan) başlatılan bir uygulama
> `~/.zshrc` okumaz; bu durumda anahtar sessizce kaybolur ve hata "anahtar tanımlı değil"
> gibi görünür. `.env` bu tuzağı ortadan kaldırır.

Anahtar hiçbir kaynakta yoksa uygulama çökmez; arayüzde anlaşılır bir uyarı gösterilir.

## Testler

```bash
./mvnw test
```

76 test, 4 sınıf:

| Test sınıfı | Kapsam |
|---|---|
| `WeatherIconMapperTest` | Tüm OpenWeatherMap ikon kodları, `main` alanına geri düşme, harf duyarsızlık, bilinmeyen/eksik kod, `null` güvenliği |
| `WeatherResponseParserTest` | Tam/eksik yanıtlar, boş ve bozuk gövde, zorunlu alan eksikleri, kök nedenin korunması |
| `OpenWeatherMapServiceTest` | Gerçek HTTP üzerinden durum kodu eşlemesi (200/401/404/429/500), URL kodlaması, erişilemeyen sunucu, ağ isteği yapılmadan reddetme |
| `ApiKeyResolverTest` | Ortam değişkeni önceliği, `.env` çözümlemesi (yorum, tırnak, boş değer), üst dizin aramasının sınırı, kaynak yokken `null` |

Servis testleri gerçek bir soket üzerinden çalışır; `StubHttpServer` sahte API görevi
görür. Böylece URL kurulumu, hata eşlemesi ve JSON çözümleme birlikte doğrulanır —
ne gerçek ağa ne de harici bir mock kütüphanesine ihtiyaç duyulur.

## Proje yapısı

```
src/main/java/com/weather/
├── Main.java                       # JavaFX giriş noktası
├── WeatherController.java          # UI olayları, ekran güncellemesi
├── config/
│   └── ApiKeyResolver.java         # Anahtar bulma: OWM_API_KEY → .env
├── model/
│   └── WeatherData.java            # Veri modeli
├── service/
│   ├── IWeatherService.java        # Servis sözleşmesi
│   ├── OpenWeatherMapService.java  # HTTP taşıma + durum kodu eşlemesi
│   ├── IWeatherResponseParser.java # Parser sözleşmesi
│   ├── WeatherResponseParser.java  # JSON → model
│   └── WeatherServiceException.java
└── mapper/
    ├── IWeatherIconMapper.java     # İkon eşleme sözleşmesi
    └── WeatherIconMapper.java      # Hava durumu → ikon dosyası

src/main/java/module-info.java      # JPMS modül tanımı
src/main/resources/com/weather/     # FXML ve ikon dosyaları
src/test/java/com/weather/          # JUnit 5 testleri
.env.example                        # Anahtar şablonu (sürüm kontrolünde)
.env                                # Gerçek anahtar (sürüm kontrolü dışında)
```

## Yol haritası

- [ ] Arayüz testleri (TestFX)

## Lisans

[MIT](LICENSE) © 2026 Yunus Emre Karaman
