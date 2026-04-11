# RHYTHM GAME MOBILE (PROJECT PULSE)
# KAPSAMLI YAZILIM MİMARİSİ VE TEKNİK TASARIM DOKÜMANI
**(Software Architecture Document & Product Requirements Specification)**

---

**DOKÜMAN KİMLİĞİ**
*   **Proje Adı:** Rhythm Game Mobile (Migration to Native Android)
*   **Sürüm:** 2.0.0 (Release Candidate)
*   **Tarih:** 23 Şubat 2026
*   **Yazar:** Gemini CLI (Senior Solutions Architect)
*   **Proje Sahibi:** Ace Me
*   **Durum:** ONAYLANMIŞ (APPROVED)
*   **Gizlilik:** KURUMSAL / DAHİLİ (INTERNAL)

---

# İÇİNDEKİLER

1.  [Giriş ve Amaç](#1-giriş-ve-amaç)
2.  [Doküman Kontrolü](#2-doküman-kontrolü)
3.  [Yönetici Özeti](#3-yönetici-özeti)
4.  [Terminoloji ve Kısaltmalar](#4-terminoloji-ve-kısaltmalar)
5.  [Proje Kapsamı ve Sınırlar](#5-proje-kapsamı-ve-sınırlar)
6.  [Paydaşlar ve Kullanıcı Personaları](#6-paydaşlar-ve-kullanıcı-personaları)
7.  [Ürün Gereksinimleri (PRD)](#7-ürün-gereksinimleri-prd)
8.  [Oyun Tasarım Dokümanı (GDD)](#8-oyun-tasarım-dokümanı-gdd)
9.  [Sistem Mimarisi (SAD)](#9-sistem-mimarisi-sad)
10. [Teknik Tasarım ve Kod Yapısı (TDD)](#10-teknik-tasarım-ve-kod-yapısı-tdd)
11. [Veri Modeli ve Depolama](#11-veri-modeli-ve-depolama)
12. [Python Entegrasyon Spesifikasyonu](#12-python-entegrasyon-spesifikasyonu)
13. [Güvenlik Mimarisi](#13-güvenlik-mimarisi)
14. [DevOps ve Sürüm Yönetimi](#14-devops-ve-sürüm-yönetimi)
15. [Test Stratejisi](#15-test-stratejisi)
16. [Risk Yönetimi](#16-risk-yönetimi)
17. [Proje Yol Haritası](#17-proje-yol-haritası)
18. [Kullanıcı Kılavuzu Taslağı](#18-kullanıcı-kılavuzu-taslağı)
19. [Ekler](#19-ekler)

---

# 1. GİRİŞ VE AMAÇ

Bu doküman, **Rhythm Game Mobile** projesinin, PC tabanlı bir sunucu mimarisinden, tamamen Android cihaz üzerinde çalışan (on-device) bağımsız bir yapıya göç sürecini ve nihai teknik özelliklerini tanımlar. Dokümanın birincil amacı, projeye yeni dahil olacak bir yazılım mühendisinin, harici bir kaynağa ihtiyaç duymadan sistemi sıfırdan kurabilmesini, geliştirebilmesini ve yayına alabilmesini sağlamaktır.

Proje, **Android Native (Kotlin)** ve **Python (Chaquopy SDK)** teknolojilerinin hibrit bir şekilde kullanılmasını esas alır.

---

# 2. DOKÜMAN KONTROLÜ

### 2.1 Sürüm Geçmişi

| Sürüm | Tarih | Yazar | Açıklama |
| :--- | :--- | :--- | :--- |
| **1.0.0** | 01.01.2025 | Ace Me | İlk PC prototipi ve Node.js backend tasarımı. |
| **1.5.0** | 10.02.2026 | Ace Me | Mobil fizibilite çalışması ve hibrit yapı denemeleri. |
| **2.0.0** | 23.02.2026 | Gemini CLI | Tamamen yerel mimariye (Local-Only) geçiş. Veritabanı şeması, Python arayüz tanımları ve oyun döngüsü revizyonu. |

### 2.2 Onay Mekanizması

Bu doküman aşağıdaki paydaşlar tarafından teknik ve idari açıdan onaylanmıştır:
*   **Teknik Lider:** Ace Me (23.02.2026)
*   **Ürün Yöneticisi:** Ace Me (23.02.2026)

---

# 3. YÖNETİCİ ÖZETİ

**Rhythm Game Mobile**, kullanıcıların kendi cihazlarındaki yerel müzik dosyalarını (MP3, WAV) kullanarak oynayabildikleri, prosedürel içerik üretimi (PCG) tabanlı bir ritim oyunudur.

Piyasadaki rakiplerden (osu!, Beat Saber vb.) ayrılan en temel özellik; beatmap (bölüm) oluşturma sürecinin sunucu tabanlı değil, kullanıcının cihazında çalışan gömülü bir **Python Motoru** tarafından gerçekleştirilmesidir. Bu mimari tercih (Edge Computing), sunucu maliyetlerini ortadan kaldırmakta ve kullanıcılara internet bağlantısı olmadan sınırsız içerik sunmaktadır.

**Hedef:** 4 hafta içerisinde, 60 FPS stabil çalışan, yerel veritabanı entegrasyonlu ve Play Store uyumlu bir MVP (Minimum Viable Product) ortaya çıkarmaktır.

---

# 4. TERMİNOLOJİ VE KISALTMALAR

*   **Chaquopy:** Android için Python SDK'sı. Python kodlarının Java/Kotlin içinden çağrılmasını sağlar.
*   **Beatmap (Chart):** Bir şarkının notalarını, zamanlamasını ve meta verilerini içeren JSON dosyası.
*   **Hit Window:** Bir notanın vurulabilir olduğu zaman aralığı (örn: ±50ms).
*   **Audio Latency:** Sesin üretilmesi ile kullanıcının duyması arasındaki süre.
*   **Input Latency:** Kullanıcının ekrana dokunması ile oyunun bunu işlemesi arasındaki süre.
*   **Game Loop:** Oyunun durumunu güncelleyen ve ekrana çizen sonsuz döngü.
*   **SurfaceView:** Android'de ana UI thread'ini bloklamadan çizim yapmayı sağlayan görünüm bileşeni.

---

# 5. PROJE KAPSAMI VE SINIRLAR

### 5.1 Kapsam Dahili (In-Scope)
1.  **Platform:** Android 10.0 (API 29) ve üzeri.
2.  **Dil:** Kotlin (UI/Core), Python 3.8+ (Analiz/Mantık).
3.  **Özellikler:**
    *   Yerel dosya okuma (SAF - Storage Access Framework).
    *   Python tabanlı BPM ve Onset tespiti (Librosa/NumPy).
    *   SQLite (Room) ile skor ve ayar saklama.
    *   Özelleştirilebilir ses gecikmesi (Audio Offset).
    *   Çoklu dokunmatik (Multi-touch) oyun kontrolü.

### 5.2 Kapsam Harici (Out-of-Scope)
1.  iOS Sürümü (Chaquopy desteği yoktur).
2.  Online Multiplayer (v2.0 MVP kapsamı dışıdır).
3.  Bulut Kayıt (Cloud Save) - v2.5 sürümüne ertelenmiştir.
4.  Müzik Streaming Servisleri (Spotify vb.) entegrasyonu.

### 5.3 Teknik Kısıtlar
*   **APK Boyutu:** Maksimum 150 MB (Python kütüphaneleri dahil).
*   **RAM Kullanımı:** Maksimum 300 MB Heap.
*   **Başlatma Süresi:** Soğuk açılış (Cold Start) < 5 saniye.

---

# 6. PAYDAŞLAR VE KULLANICI PERSONALARI

### 6.1 Persona: "Rekabetçi Ritimci" (Ali, 22)
*   **Davranış:** Günde 2 saat oynar, milisaniyelik gecikmeleri fark eder.
*   **Beklenti:** Stabil 60 FPS, kesin yargılama sistemi, detaylı istatistikler.
*   **Hayal Kırıklığı:** Dokunmatik ekran gecikmesi, FPS düşüşü (stutter).

### 6.2 Persona: "Casual Dinleyici" (Ayşe, 28)
*   **Davranış:** Otobüste/metroda oynar, kendi şarkılarını kullanmak ister.
*   **Beklenti:** Basit arayüz, hızlı yükleme, görsel şölen.
*   **Hayal Kırıklığı:** Karmaşık ayarlar, uzun analiz süreleri.

---

# 7. ÜRÜN GEREKSİNİMLERİ (PRD)

### 7.1 Fonksiyonel Gereksinimler

| ID | Gereksinim | Öncelik | Açıklama |
| :--- | :--- | :--- | :--- |
| **FR-01** | Dosya İçe Aktarma | P0 | Kullanıcı cihazdan .mp3, .wav dosyası seçebilmelidir. |
| **FR-02** | Ritim Analizi | P0 | Sistem, seçilen şarkıyı analiz edip beatmap JSON'ı üretmelidir. |
| **FR-03** | Oyun Oynama | P0 | Müzik ile senkronize notalar akmalı ve dokunma algılanmalıdır. |
| **FR-04** | Skor Kaydı | P1 | Oyun sonu skorları yerel veritabanına yazılmalıdır. |
| **FR-05** | Kalibrasyon | P1 | Kullanıcı +/- ms cinsinden ses gecikmesini ayarlayabilmelidir. |
| **FR-06** | Duraklatma | P2 | Uygulama arka plana atıldığında oyun otomatik durmalıdır. |

### 7.2 Fonksiyonel Olmayan Gereksinimler

| ID | Metrik | Hedef |
| :--- | :--- | :--- |
| **NFR-01** | Frame Rate | Min 58 FPS (%99 percentile). |
| **NFR-02** | Input Latency | < 50ms (Donanım izin verdiği ölçüde). |
| **NFR-03** | Analiz Süresi | 3 dakikalık şarkı için < 20 saniye. |
| **NFR-04** | Crash Rate | %0.1'den az (Firebase Crashlytics). |

---

# 8. OYUN TASARIM DOKÜMANI (GDD)

### 8.1 Temel Oynanış (Core Gameplay)
Ekran dikey (Portrait) veya yatay (Landscape) olabilir (Tercih: Landscape). Ekranın üst kısmından aşağıya doğru (veya merkezden dışa) notalar akar. Notalar "Vuruş Çizgisi" (Judgement Line) üzerine geldiğinde oyuncu dokunmalıdır.

### 8.2 Nota Tipleri
1.  **Tap Note:** Tek dokunuş.
2.  **Hold Note:** Basılı tutma. Başlangıçta bas, bitişte bırak.
3.  **Slide Note:** Belirtilen yöne parmağı kaydır.

### 8.3 Skorlama Sistemi (Scoring Formula)
Maksimum Skor: 1.000.000 Puan.

$$ Score = \left( \frac{BaseScore}{TotalNotes} ight) 	imes AccuracyMultiplier + ComboBonus $$

*   **Marvelous (±40ms):** %100 Puan
*   **Perfect (±80ms):** %80 Puan
*   **Good (±120ms):** %50 Puan
*   **Miss (>120ms):** %0 Puan + Combo Sıfırlama

### 8.4 Zorluk Seviyeleri
Zorluk, Python algoritmasına gönderilen parametrelerle belirlenir:
*   **Easy:** Sadece 1/1 ve 1/2 vuruşlar (Bass drum).
*   **Normal:** 1/4 vuruşlar dahil.
*   **Hard:** 1/8 vuruşlar ve karmaşık ritimler (Melodi takibi).

---

# 9. SİSTEM MİMARİSİ (SAD)

### 9.1 Yüksek Seviye Mimari
Proje **MVVM (Model-View-ViewModel)** mimarisini kullanır ve Clean Architecture prensiplerini takip eder.

```mermaid
graph TD
    UI[Android UI Layer] -->|Events| VM[ViewModel]
    VM -->|Use Cases| Domain[Domain Layer]
    Domain -->|Interface| Repo[Repository Layer]
    
    subgraph Data Layer
        Repo -->|SQL| DB[(Room DB)]
        Repo -->|JNI| Python[Python Service]
    end
    
    subgraph Python Environment (Chaquopy)
        Python -->|Call| Analyzer[Audio Analyzer]
        Python -->|Call| Generator[Beatmap Gen]
        Analyzer -->|Libs| Librosa[Librosa/NumPy]
    end
```

### 9.2 Katmanlar
1.  **Presentation (UI):** Fragment'ler, Custom View (GameSurfaceView), ViewModel.
2.  **Domain:** İş kuralları. Örn: `CalculateScoreUseCase`, `AnalyzeSongUseCase`.
3.  **Data:** Veri kaynakları. `SongRepository`, `ScoreRepository`.
4.  **Infrastructure:** Platforma özgü kodlar. `AudioPlayerImpl`, `PythonBridge`.

---

# 10. TEKNİK TASARIM VE KOD YAPISI (TDD)

### 10.1 Proje Dizin Yapısı

```
app/src/main/
├── java/com/aceme/rhythmgame/
│   ├── core/           # Base classes, Constants
│   ├── data/           # Repository impl, Data Sources
│   │   ├── local/      # Room DB, DAO
│   │   └── python/     # Python Bridge Classes
│   ├── domain/         # Models, Repository Interfaces
│   ├── presentation/   # UI, ViewModels
│   │   ├── game/       # Game Loop, Renderer
│   │   └── home/       # Menus
│   └── di/             # Hilt Modules
├── python/             # PYTHON KODLARI BURADA
│   ├── __init__.py
│   ├── analyzer.py
│   └── beatmap.py
├── res/                # XML Layouts, Drawables
└── AndroidManifest.xml
```

### 10.2 Oyun Döngüsü (Game Loop)
`SurfaceView` kullanılarak ayrı bir thread üzerinde çalıştırılır.

**Pseudocode (Kotlin):**
```kotlin
class GameLoop(private val surfaceHolder: SurfaceHolder) : Thread() {
    var running = false
    
    override fun run() {
        while (running) {
            val startTime = System.nanoTime()
            
            // 1. UPDATE
            gameEngine.update(getCurrentAudioTime())
            
            // 2. DRAW
            val canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                gameRenderer.draw(canvas)
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
            
            // 3. SLEEP (Cap at 60 FPS)
            val timeTaken = System.nanoTime() - startTime
            val sleepTime = (TARGET_TIME - timeTaken) / 1000000
            if (sleepTime > 0) Thread.sleep(sleepTime)
        }
    }
}
```

---

# 11. VERİ MODELİ VE DEPOLAMA

### 11.1 Veritabanı Şeması (Room)

**Entity: Song**
```kotlin
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val hash: String, // SHA-256 of file
    val title: String,
    val artist: String,
    val durationMs: Long,
    val filePath: String,
    val bpm: Float,
    val isAnalyzed: Boolean
)
```

**Entity: Score**
```kotlin
@Entity(tableName = "scores")
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val songHash: String,
    val score: Int,
    val maxCombo: Int,
    val accuracy: Float, // 0.0 - 1.0
    val timestamp: Long
)
```

---

# 12. PYTHON ENTEGRASYON SPESİFİKASYONU

### 12.1 Chaquopy Ayarları (`build.gradle`)
```groovy
plugins {
    id 'com.android.application'
    id 'com.chaquo.python'
}

android {
    defaultConfig {
        python {
            version "3.8"
            pip {
                install "numpy"
                install "librosa==0.8.1" // Hafif sürüm
            }
        }
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a"
        }
    }
}
```

### 12.2 Python Modül API (`analyzer.py`)

**Fonksiyon:** `analyze_audio(file_path: str) -> str`

**Girdi:** Dosya yolu (String).
**Çıktı:** JSON formatında String.

**JSON Çıktı Örneği:**
```json
{
  "status": "success",
  "bpm": 120.5,
  "offset_ms": 50,
  "beats": [
    {"time": 1000, "is_strong": true},
    {"time": 1500, "is_strong": false},
    {"time": 2000, "is_strong": true}
  ]
}
```

---

# 13. GÜVENLİK MİMARİSİ

1.  **Kod Karartma (Obfuscation):** R8/ProGuard aktif edilerek Java/Kotlin sınıfları ve metod isimleri gizlenecektir.
2.  **Dosya Erişimi:** Android Scoped Storage kurallarına sıkı sıkıya uyulacak, sadece kullanıcının seçtiği dosyalara erişim sağlanacaktır.
3.  **Veritabanı:** SQL Injection riskine karşı Room kütüphanesi (Precompiled Statements) kullanılmaktadır.

---

# 14. DEVOPS VE SÜRÜM YÖNETİMİ

### 14.1 Build Pipeline
1.  **Clean:** `./gradlew clean`
2.  **Test:** `./gradlew testDebugUnitTest`
3.  **Build:** `./gradlew assembleRelease`
4.  **Bundle:** `./gradlew bundleRelease` (Play Store için .aab formatı).

### 14.2 Versiyonlama
Semantik Versiyonlama (Major.Minor.Patch) kullanılacaktır.

---

# 15. TEST STRATEJİSİ

| Test Türü | Araç | Kapsam |
| :--- | :--- | :--- |
| **Unit Test** | JUnit 5, MockK | ViewModel mantığı, Veri dönüşümleri. |
| **Entegrasyon** | AndroidJUnit4 | Veritabanı okuma/yazma, Python çağrıları. |
| **UI Test** | Espresso | Menü geçişleri, Buton tıklamaları. |
| **Manuel Test** | İnsan Gözü | Ses senkronizasyonu, Oynanış hissi. |

---

# 16. RİSK YÖNETİMİ

| Risk | Olasılık | Etki | Önlem |
| :--- | :--- | :--- | :--- |
| **Python Başlatma Yavaşlığı** | Yüksek | Orta | Uygulama açılışında (Splash) asenkron başlatma. |
| **Aşırı Pil Tüketimi** | Orta | Yüksek | Menülerde FPS'i 30'a düşürmek. Dark mode kullanımı. |
| **Cihaz Isınması** | Orta | Orta | CPU yoğun Python analizini oyun sırasında yapmamak (Pre-process). |

---

# 17. PROJE YOL HARİTASI

*   **Hafta 1:** Proje iskeleti, Chaquopy kurulumu, Temel UI.
*   **Hafta 2:** Ses motoru (AudioTrack), Python analiz entegrasyonu.
*   **Hafta 3:** Oyun döngüsü, Nota çizimi, Hit algılama.
*   **Hafta 4:** Skor sistemi, Veritabanı, UI cilalama, Play Store hazırlığı.

---

# 18. KULLANICI KILAVUZU TASLAĞI

1.  **Kurulum:** APK'yı yükleyin.
2.  **İzinler:** "Dosya Erişimi" iznini onaylayın.
3.  **Şarkı Ekleme:** Ana ekrandaki "+" butonuna basın ve bir MP3 seçin.
4.  **Oynama:** Şarkı analiz edildikten sonra (ilk seferde 10-20sn sürer) "Play" tuşuna basın.
5.  **Senkronizasyon:** Eğer müzik ile notalar uyuşmuyorsa, Ayarlar > Audio Offset menüsünden gecikmeyi ayarlayın.

---

# 19. EKLER

### Ek A: Lisans Bilgileri
Bu proje **MIT Lisansı** ile lisanslanmıştır. Kullanılan açık kaynak kütüphanelerin lisansları (Apache 2.0, GPL) ilgili dosyalarda belirtilmiştir.
