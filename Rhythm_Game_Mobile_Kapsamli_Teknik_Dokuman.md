# RHYTHM GAME MOBILE (PROJECT PULSE)
# KAPSAMLI YAZILIM MİMARİSİ VE TEKNİK TASARIM DOKÜMANI
**(Software Architecture Document & Product Requirements Specification)**

---

**DOKÜMAN KİMLİĞİ**
*   **Proje Adı:** Rhythm Game Mobile (Project Pulse)
*   **Sürüm:** 1.0.0 (Release Candidate)
*   **Tarih:** 23 Şubat 2026
*   **Yazar:** Avni Cem Ersoy (Lead Software Architect & Product Owner)
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

### 1.1 Dokümanın Amacı
Bu Yazılım Mimari Dokümanı (SAD), **"Rhythm Game Mobile (Project Pulse)"** projesinin teknik temellerini, mimari kararlarını ve uygulama detaylarını eksiksiz bir şekilde tanımlamak amacıyla hazırlanmıştır. Doküman, projeye dahil olacak **Kıdemli Yazılım Geliştiriciler** ve **Sistem Mimarları** için birincil referans kaynağı (Single Source of Truth) niteliğindedir.

Dokümanın temel hedefleri şunlardır:
1.  **Hibrit Mimariyi Tanımlamak:** Android Native (Kotlin) ve Gömülü Python (Chaquopy) arasındaki karmaşık veri akışını ve entegrasyon noktalarını standardize etmek.
2.  **Algoritmik Şeffaflık:** `lane_mapper.py` ve `beat_tracker.py` gibi prosedürel içerik üretimi (PCG) algoritmalarının çalışma mantığını matematiksel kesinlikle açıklamak.
3.  **Sürdürülebilirlik:** Projenin bakımını ve ölçeklenebilirliğini sağlamak için kod standartlarını ve modüler yapıyı belirlemek.

### 1.2 Kapsam
Bu doküman, Project Pulse'ın **v1.0.0 (MVP)** sürümünü kapsar. Doküman içerisinde; istemci tarafı (Android), yerel veri işleme katmanı (Python DSP), veri kalıcılığı (Room Database) ve kullanıcı arayüzü (Jetpack Compose/XML) detaylandırılmıştır. Sunucu tarafı mimarisi (Cloud), bu sürümde "Future Scope" olarak ele alınmıştır.

---

# 2. DOKÜMAN KONTROLÜ

### 2.1 Sürüm Geçmişi

| Sürüm | Tarih | Yazar | Değişiklik Özeti |
| :--- | :--- | :--- | :--- |
| **0.1.0** | 01.01.2026 | Avni Cem Ersoy | İlk konsept ve fizibilite çalışması. |
| **0.5.0** | 15.02.2026 | Avni Cem Ersoy | Python (Chaquopy) entegrasyonu ve prototip. |
| **1.0.0** | 23.02.2026 | Avni Cem Ersoy | Tam kapsamlı teknik mimari ve detaylı tasarım (Mevcut Sürüm). |

### 2.2 Onay Otoritesi
Bu doküman, projenin teknik bütünlüğü ve iş hedeflerine uygunluğu açısından aşağıdaki otorite tarafından onaylanmıştır:

*   **Adı Soyadı:** Avni Cem Ersoy
*   **Unvan:** Lead Software Architect & Product Owner
*   **Onay Tarihi:** 23.02.2026

---

# 3. YÖNETİCİ ÖZETİ (EXECUTIVE SUMMARY)

**Project Pulse**, kullanıcının cihazındaki yerel ses dosyalarını (MP3/WAV/OGG) işleyerek, prosedürel olarak ritim oyun seviyeleri (Beatmaps) üreten, yüksek performanslı bir Android mobil uygulamasıdır.

Projenin en ayırt edici teknik özelliği, **Edge Computing (Uçta Hesaplama)** yaklaşımıdır. Geleneksel ritim oyunlarının aksine, sunucu tabanlı önceden hazırlanmış haritalar yerine, oyun içeriği çalışma zamanında (runtime) kullanıcının cihazında üretilir. Bu işlem için Android uygulama katmanına (Kotlin), bilimsel hesaplama kütüphaneleri (NumPy, Librosa) içeren optimize edilmiş bir Python çalışma zamanı (Chaquopy) gömülmüştür.

**Temel Teknik Kazanımlar:**
*   **Çevrimdışı Yetenek:** İnternet bağımsız tam fonksiyonel oyun deneyimi.
*   **Sıfır Gecikme:** Sunucu yanıt süresinin (RTT) eliminasyonu.
*   **Dinamik Zorluk:** `difficulty.py` modülü ile kişiselleştirilmiş zorluk seviyeleri.

---

# 4. TERMİNOLOJİ VE KISALTMALAR

| Terim | Açıklama |
| :--- | :--- |
| **Onset Detection** | Ses sinyalindeki ani genlik değişimlerini (vuruşları) tespit eden DSP algoritması. |
| **Lane Mapping** | Zaman eksenindeki vuruşların (Time Domain), oyun alanındaki uzamsal konumlara (Spatial Domain - 5 Lane) deterministik olarak dağıtılması işlemi (`lane_mapper.py`). |
| **Chaquopy** | Android uygulamaları içinde CPython çalışma zamanını barındıran ve Java Native Interface (JNI) üzerinden çift yönlü iletişimi sağlayan SDK. |
| **Beatmap (Chart)** | Bir şarkının oyun verisini içeren JSON yapısı (Notalar, BPM, Metadata). |
| **DSP** | Digital Signal Processing (Sayısal Sinyal İşleme). |
| **Hit Window** | Oyuncunun bir notaya basması için tanınan milisaniye cinsinden tolerans aralığı. |

---

# 5. PROJE KAPSAMI VE SINIRLAR

### 5.1 Kapsam Dahili (In-Scope)
Proje, aşağıdaki modüllerin geliştirilmesini ve entegrasyonunu kapsar:

1.  **Android İstemci (Client):**
    *   UI Katmanı (Kotlin, MVVM Mimarisi).
    *   Oyun Döngüsü (Game Loop, SurfaceView/Canvas tabanlı çizim).
    *   Ses Motoru (AudioTrack/OpenSL ES - Düşük gecikme).
2.  **Python Hesaplama Çekirdeği (Core Logic):**
    *   `analyzer.py`: Ses dosyası I/O ve özellik çıkarımı.
    *   `beat_tracker.py`: Tempo (BPM) ve vuruş zamanlaması tespiti.
    *   `lane_mapper.py`: Notaların şeritlere dağıtılması (Heuristic Pattern Generation).
    *   `difficulty.py`: Beatmap yoğunluğuna (Note Density) göre zorluk puanlaması.
    *   `chart.py`: Verinin serileştirilmesi ve JSON formatına dönüşümü.
3.  **Veri Katmanı:**
    *   Kullanıcı profili, skorlar ve ayarlar için **Room Database (SQLite)**.
    *   Büyük dosya yönetimi (Cachelenen beatmapler).

### 5.2 Kapsam Dışı (Out-of-Scope - v1.0)
*   **iOS Sürümü:** Chaquopy'nin teknik kısıtları nedeniyle sadece Android hedeflenmiştir.
*   **Online Multiplayer (PvP):** Gerçek zamanlı senkronizasyon v2.0 için planlanmıştır.
*   **Bulut Senkronizasyonu:** Kullanıcı verilerinin Firebase/AWS'e yedeklenmesi.

---

# 6. PAYDAŞLAR VE KULLANICI PERSONALARI

### 6.1 Proje Ekibi
*   **Avni Cem Ersoy:** Proje Lideri, Mimar, Full-Stack Geliştirici.

### 6.2 Hedef Kullanıcı Personası: "Teknik Oyuncu"
*   **Profil:** 18-35 yaş, mobil oyunlarda performansa duyarlı, ritim oyunları (osu!, Beat Saber) deneyimi olan.
*   **Beklenti:**
    *   **Performans:** Sabit 60 FPS kare hızı (Frame Rate).
    *   **Hassasiyet:** <50ms giriş gecikmesi (Input Latency).
    *   **Özelleştirme:** Kendi müzik kütüphanesini kullanabilme özgürlüğü.

---

# 7. ÜRÜN GEREKSİNİMLERİ (PRD)

### 7.1 Fonksiyonel Gereksinimler

| ID | Kategori | Gereksinim Tanımı | Öncelik |
| :--- | :--- | :--- | :--- |
| **FR-01** | **Oyun Mekaniği** | Oyun alanı, dikey (vertical) veya yatay (horizontal) akan **5 Şerit (Lane)** yapısına sahip olmalıdır. | **P0** |
| **FR-02** | **Nota Tipleri** | Sistem, anlık vuruşlar için **Tap Note** ve süreli basılı tutmalar için **Hold Note** üretebilmelidir. Slide note desteklenmeyecektir. | **P0** |
| **FR-03** | **Analiz Süreci** | Kullanıcı şarkı seçtiğinde, analiz süreci boyunca ilerlemeyi gösteren bir **Loading Ekranı** (ProgressBar) görüntülenmelidir. | **P1** |
| **FR-04** | **Şerit Dağıtımı** | Notaların şeritlere dağılımı, sesin frekans karakteristiğine (Spectral Centroid) göre belirlenmeli; bas sesler sol, tiz sesler sağ şeride düşmelidir. | **P1** |
| **FR-05** | **Zorluk Algoritması** | Sistem, `difficulty.py` modülü aracılığıyla notaların yoğunluğunu (Note Density) analiz ederek "Easy", "Medium", "Hard" etiketlemesi yapmalıdır. | **P2** |
| **FR-06** | **Skor Kaydı** | Her oyun sonunda elde edilen skor, kombo ve doğruluk oranı yerel veritabanına (Room) kaydedilmelidir. | **P1** |

### 7.2 Fonksiyonel Olmayan Gereksinimler (Performans & UX)

*   **NFR-01 (Tepki Süresi):** 4 dakikalık standart bir MP3 dosyasının analizi ve `Chart` nesnesine dönüştürülmesi, modern cihazlarda (Snapdragon 8 Gen 2 ve üzeri) **< 15 saniye** sürmelidir.
*   **NFR-02 (FPS):** Oyun içi kare hızı (Frame Rate), %99 percentile değerinde **58 FPS**'in altına düşmemelidir.
*   **NFR-03 (Input Latency):** Dokunmatik ekran tepki süresi **< 50ms** olmalıdır.
*   **NFR-04 (Stabilite):** Uygulama, arka arkaya 20 şarkı oynatıldığında bellek sızıntısı (Memory Leak) nedeniyle çökmemelidir (Crash-free session rate > %99).

---

# 8. OYUN TASARIM DOKÜMANI (GDD)

### 8.1 Oyun Alanı ve Kontroller
Oyun, **5 Şeritli (Lane 1-5)** bir yapı üzerine kuruludur.
*   **Lane 1 (En Sol):** Düşük frekanslı sesler (Bass/Kick).
*   **Lane 2:** Düşük-Orta frekanslar.
*   **Lane 3 (Orta):** Orta frekanslı sesler (Vokal/Snare).
*   **Lane 4:** Orta-Yüksek frekanslar.
*   **Lane 5 (En Sağ):** Yüksek frekanslı sesler (Hi-Hat/Synth).

### 8.2 Puanlama Sistemi (Scoring Algorithm)
Puanlama, oyuncunun vuruş zamanlamasının (Hit Time), notanın ideal zamanına (Target Time) olan uzaklığına (`delta_ms`) göre hesaplanır.

**Maksimum Skor:** 1,000,000 Puan (Sabit Tavan)

| Yargı (Judgement) | Zaman Penceresi (±ms) | Temel Puan (Base Score) | Combo Etkisi |
| :--- | :--- | :--- | :--- |
| **PERFECT** | 0 - 45ms | 1000 | +1 |
| **GREAT** | 46 - 90ms | 800 | +1 |
| **GOOD** | 91 - 135ms | 500 | Bozulmaz (Keep) |
| **MISS** | > 135ms | 0 | Sıfırlanır (Reset) |

**Hold Note Puanlaması:**
*   **Başlangıç:** Tap note gibi değerlendirilir.
*   **Süreç (Tick):** Basılı tutulan her 100ms için ekstra puan (Tick Score).
*   **Bitiş:** Zamanında bırakılırsa bonus puan. Erken bırakılırsa "Miss" sayılır.

### 8.3 Combo Çarpanı (Combo Multiplier)
Oyuncu hata yapmadan seriyi sürdürdükçe puan çarpanı artar:
*   **0 - 10 Combo:** x1.0
*   **11 - 30 Combo:** x1.2
*   **31 - 50 Combo:** x1.5
*   **50+ Combo:** x2.0 (Maksimum)

**Doğruluk (Accuracy) Hesabı:**
$$ Accuracy \% = \frac{\sum (Not Puanları)}{	ext{Toplam Nota Sayısı} 	imes 1000} 	imes 100 $$

---

# 9. SİSTEM MİMARİSİ (SAD)

### 9.1 Yüksek Seviye Mimari (Haftalık Akış)

Sistem, **MVVM (Model-View-ViewModel)** mimarisini kullanır ve veri akışı tek yönlüdür (Unidirectional Data Flow).

```mermaid
graph TD
    User[Kullanıcı] -->|MP3 Seçimi| UI[Android UI (Activity/Fragment)]
    UI -->|Loading State| VM[ViewModel]
    VM -->|Async Call| Repo[SongRepository]
    
    subgraph "Data Layer (Kotlin)"
        Repo -->|JNI Bridge| PyService[PythonServiceWrapper]
    end
    
    subgraph "Core Logic (Python/Chaquopy)"
        PyService -->|Path| Analyzer[analyzer.py]
        Analyzer -->|Audio Data| BeatTracker[beat_tracker.py]
        BeatTracker -->|Onsets/Centroids| LaneMapper[lane_mapper.py]
        LaneMapper -->|Map(1-5)| ChartGen[chart.py]
    end
    
    ChartGen -->|JSON String| PyService
    PyService -->|Chart Object| Repo
    Repo -->|Success| VM
    VM -->|Navigate| GameLoop[Game Engine]
```

### 9.2 Modüller Arası Etkileşim
1.  **Giriş:** `analyzer.py` ses dosyasını okur ve Librosa kütüphanesi ile `y` (audio time series) ve `sr` (sampling rate) verilerini çıkarır.
2.  **İşleme:**
    *   `beat_tracker.py`: `librosa.onset.onset_detect` fonksiyonunu kullanarak vuruş zamanlarını (`time_ms`) bulur.
    *   `lane_mapper.py`: Her vuruş anındaki `spectral_centroid` değerini hesaplar. Bu değerleri `np.percentile` ile %20, %40, %60, %80 dilimlerine ayırarak 5 şeride (1, 2, 3, 4, 5) atar.
3.  **Çıktı:** `chart.py`, elde edilen verileri `Note` nesnelerine dönüştürür ve `Chart` sınıfı üzerinden JSON formatında Android tarafına gönderir.

---

# 10. TEKNİK TASARIM VE KOD YAPISI (TDD)

### 10.1 Veri Yapıları (Data Structures)

**Python Tarafı (`chart.py`):**
```python
class Note:
    def __init__(self, time_ms, lane, duration_ms=0, note_type="tap"):
        self.time_ms = int(time_ms)
        self.lane = int(lane) # 1-5 arası
        self.duration_ms = int(duration_ms)
        self.type = str(note_type) # "tap" veya "hold"
```

**Kotlin Tarafı (Data Class):**
```kotlin
data class NoteEntity(
    val timeMs: Long,
    val lane: Int, // 1, 2, 3, 4, 5
    val durationMs: Long,
    val type: NoteType // Enum: TAP, HOLD
)

data class ChartEntity(
    val songId: String,
    val bpm: Float,
    val difficulty: String,
    val notes: List<NoteEntity>
)
```

### 10.2 Lane Mapping Algoritması (`lane_mapper.py`) detaylandırılması
Algoritma, deterministik bir yaklaşım sergiler. Rastgelelik (Randomness) yerine sesin frekans karakteristiğini kullanır.

1.  **Centroid Calculation:** Her vuruş anı için sesin "ağırlık merkezi" (brightness) hesaplanır.
2.  **Percentile Thresholding:** Şarkının tamamındaki centroid değerleri toplanır ve dağılım analiz edilir.
    *   En düşük %20 -> **Lane 1**
    *   %20 - %40 -> **Lane 2**
    *   %40 - %60 -> **Lane 3**
    *   %60 - %80 -> **Lane 4**
    *   En yüksek %20 -> **Lane 5**
3.  **Sonuç:** Bu sayede bas vuruşlar tutarlı bir şekilde solda, tiz vuruşlar sağda yer alır, oyuncuya doğal bir "piyano" hissi verir.

---

# 11. VERİ MODELİ VE DEPOLAMA

### 11.1 Veritabanı Stratejisi
Veri kalıcılığı için Android Jetpack Room kütüphanesi (SQLite soyutlaması) kullanılır. İlişkisel veri modeli tercih edilmiştir.

### 11.2 Veritabanı Şeması

**Tablo: `UserProfiles`**
| Alan Adı | Veri Tipi | Açıklama |
| :--- | :--- | :--- |
| `user_id` | INTEGER (PK) | Benzersiz kullanıcı ID'si. |
| `username` | TEXT | Görünen isim. |
| `total_score` | BIGINT | Toplam kazanılan puan. |
| `play_count` | INTEGER | Toplam oynanan şarkı sayısı. |

**Tablo: `Songs`**
| Alan Adı | Veri Tipi | Açıklama |
| :--- | :--- | :--- |
| `song_hash` | TEXT (PK) | Dosyanın MD5 hash'i. |
| `title` | TEXT | Şarkı adı. |
| `artist` | TEXT | Sanatçı adı. |
| `duration_ms` | LONG | Süre (ms). |
| `chart_path` | TEXT | JSON dosyasının önbellek yolu. |

**Tablo: `ScoreHistory`**
| Alan Adı | Veri Tipi | Açıklama |
| :--- | :--- | :--- |
| `history_id` | INTEGER (PK) | Kayıt ID'si. |
| `song_hash` | TEXT (FK) | Hangi şarkı. |
| `user_id` | INTEGER (FK) | Hangi kullanıcı. |
| `score` | INTEGER | Puan. |
| `accuracy` | FLOAT | Doğruluk yüzdesi. |
| `combo` | INTEGER | Maksimum kombo. |
| `grade` | TEXT | Harf notu (S, A, B, C, D, F). |
| `timestamp` | LONG | Oynama tarihi. |

### 11.3 Büyük Dosya Yönetimi (Caching)
Python tarafından üretilen Beatmap JSON dosyaları (boyutları 100KB - 2MB arası değişebilir), veritabanında saklanmak yerine `Context.getFilesDir() + "/charts/"` dizininde dosya sistemi üzerinde saklanır. Veritabanındaki `chart_path` sütunu bu dosyanın yolunu tutar. Bu yaklaşım, veritabanı şişmesini önler.

---

# 12. PYTHON ENTEGRASYON SPESİFİKASYONU

### 12.1 Chaquopy Konfigürasyonu (`build.gradle`)
Projenin `build.gradle` dosyasında aşağıdaki yapılandırma zorunludur:

```groovy
android {
    defaultConfig {
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a" // Sadece ARM mimarileri
        }
        python {
            version "3.8"
            pip {
                install "numpy"
                install "librosa"
                install "scipy"
            }
        }
    }
}
```

### 12.2 Python Servis Sarmalayıcı (Service Wrapper)
Kotlin tarafında `Python.getInstance()` çağrılarını sarmalayan bir Singleton sınıf kullanılır.

```kotlin
object PythonBridge {
    private val python by lazy { Python.getInstance() }
    private val analyzerModule by lazy { python.getModule("analyzer") }

    fun analyzeSong(filePath: String): String {
        // Python: def analyze(path) -> json_str
        val jsonResult = analyzerModule.callAttr("analyze", filePath).toString()
        return jsonResult
    }
}
```

### 12.3 Hata Yönetimi
Python tarafında oluşabilecek hatalar (örn: dosya okunamadı, bellek yetersiz) `try-except` blokları ile yakalanmalı ve JSON formatında hata kodu olarak dönülmelidir: `{"error": "FILE_NOT_FOUND", "code": 404}`. Kotlin tarafı bu JSON'ı parse ederek kullanıcıya anlamlı bir hata mesajı gösterir.

---

# 13. GÜVENLİK MİMARİSİ

### 13.1 Veri Güvenliği
*   **Scoped Storage:** Android 10+ standartlarına uygun olarak, uygulama sadece kullanıcının açıkça izin verdiği medya dosyalarına erişir (`READ_MEDIA_AUDIO`).
*   **Veritabanı Şifreleme:** İleride SQLCipher entegrasyonu ile yerel veritabanı şifrelenebilir (Opsiyonel).

### 13.2 Kod Güvenliği
*   **Obfuscation:** ProGuard/R8 kuralları ile Kotlin kodları karartılarak tersine mühendislik (Reverse Engineering) zorlaştırılır.
*   **Python Bytecode:** Chaquopy, `.py` dosyalarını `.pyc` (derlenmiş bytecode) formatında paketleyerek kaynak kodun doğrudan okunmasını engeller.

---

# 14. DEVOPS VE SÜRÜM YÖNETİMİ

### 14.1 Sürüm Kontrolü (Git)
Dallanma stratejisi olarak **GitFlow** kullanılır:
*   `main`: Kararlı sürüm.
*   `develop`: Geliştirme dalı.
*   `feature/feature-name`: Yeni özellikler.
*   `release/v1.0.0`: Sürüm hazırlığı.

### 14.2 CI/CD Pipeline (GitHub Actions)
Her `push` işleminde otomatik olarak:
1.  **Build:** Proje derlenir.
2.  **Lint:** Kod standartları kontrol edilir.
3.  **Unit Tests:** Kotlin ve Python birim testleri çalıştırılır.

---

# 15. TEST STRATEJİSİ

### 15.1 Birim Testler (Unit Tests)
*   **Python:** `pytest` kullanılarak `lane_mapper` fonksiyonunun farklı girdi setleri (örneğin sadece bas sesler, sadece tiz sesler) için doğru şeritleri (1 veya 5) döndürdüğü doğrulanır.
*   **Kotlin:** `ViewModel` ve `Repository` sınıfları `MockK` ile mocklanarak test edilir.

### 15.2 Entegrasyon Testleri
*   Gerçek bir MP3 dosyası cihaza yüklenerek, analiz sürecinin baştan sona (File -> Python -> JSON -> DB -> UI) hatasız tamamlandığı test edilir.

### 15.3 Performans Testleri
*   Android Profiler kullanılarak bellek (RAM) kullanımı izlenir. Analiz sırasında bellek kullanımının 300MB'ı geçmemesi hedeflenir.

---

# 16. RİSK YÖNETİMİ

| Risk | Olasılık | Etki | Önlem (Mitigation) |
| :--- | :--- | :--- | :--- |
| **Python Başlatma Gecikmesi** | Yüksek | Orta | Uygulama açılışında (Splash Screen) Python motorunu asenkron olarak başlatmak (Warm-up). |
| **Yüksek Pil Tüketimi** | Orta | Yüksek | Oyun döngüsünü kullanılmadığı zamanlarda durdurmak, analiz işlemlerini sadece şarj durumunda önermek. |
| **MP3 Format Uyumluluğu** | Düşük | Orta | FFMPEG tabanlı bir decoder kullanarak kodek desteğini genişletmek. |
| **APK Boyutu** | Kesin | Düşük | `abiFilters` ile sadece gerekli mimarileri (arm64-v8a) paketlemek. |

---

# 17. PROJE YOL HARİTASI

### Faz 1: MVP (Mevcut Durum)
*   Temel oyun motoru (5 Lane).
*   Python analiz entegrasyonu.
*   Yerel veritabanı.

### Faz 2: Geliştirme (Q2 2026)
*   Cloud Save (Firebase).
*   Skin Sistemi (Farklı nota görünümleri).
*   Global Liderlik Tablosu.

### Faz 3: Genişleme (Q4 2026)
*   iOS Portu (Python yerine C++ kütüphanesi ile).
*   Çok oyunculu mod.

---

# 18. KULLANICI KILAVUZU TASLAĞI

### 18.1 Kurulum
Google Play Store'dan "Rhythm Game (Project Pulse)" uygulamasını indirin ve yükleyin.

### 18.2 İlk Başlangıç
Uygulamayı ilk açtığınızda, medya dosyalarına erişim izni vermeniz istenecektir. Bu izin, cihazınızdaki müzikleri bulmak ve analiz etmek için gereklidir.

### 18.3 Şarkı Ekleme ve Oynama
1.  Ana menüden "Oyna" butonuna basın.
2.  Şarkı listesinden bir şarkı seçin. Eğer şarkı daha önce analiz edilmemişse, "Analiz Ediliyor" çubuğu görünecektir (yaklaşık 10-15 saniye).
3.  Analiz bittiğinde zorluk seviyesini (Easy/Normal/Hard) seçin ve "Başla" butonuna basın.

### 18.4 Oynanış
Müziğin ritmine göre yukarıdan aşağıya (veya merkezden dışa) notalar akacaktır. Notalar alttaki çizgiye geldiğinde doğru zamanda dokunun.
*   **Mavi Notalar:** Tek dokunuş (Tap).
*   **Yeşil Notalar:** Basılı tutun (Hold).

---

# 19. EKLER

### Ek A: Örnek Beatmap JSON Çıktısı

```json
{
  "song_id": "song_12345",
  "title": "Example Track",
  "bpm": 120.0,
  "difficulty": "medium",
  "notes": [
    {
      "time_ms": 1000,
      "lane": 1,
      "duration_ms": 0,
      "type": "tap"
    },
    {
      "time_ms": 1500,
      "lane": 3,
      "duration_ms": 500,
      "type": "hold"
    },
    {
      "time_ms": 2200,
      "lane": 5,
      "duration_ms": 0,
      "type": "tap"
    }
  ]
}
```

### Ek B: Geliştirme Ortamı Kurulumu
1.  Android Studio (Son Sürüm) yükleyin.
2.  Python 3.8 yükleyin.
3.  Projeyi klonlayın: `git clone ...`
4.  `local.properties` dosyasına `ndk.dir` yolunu ekleyin.
5.  Projeyi Sync edin ve çalıştırın.
