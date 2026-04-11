# RHYTHM GAME MOBILE (PROJECT PULSE)
# DETAYLI YAZILIM MİMARİSİ VE TEKNİK TASARIM DOKÜMANI (v2.0)

**DOKÜMAN KİMLİĞİ**
*   **Proje Adı:** Rhythm Game Mobile (Project Pulse)
*   **Sürüm:** 2.0.0 (Extended Technical Specification)
*   **Tarih:** 23 Şubat 2026
*   **Hazırlayan:** Avni Cem Ersoy (Lead Software Architect)
*   **Onaylayan:** Proje Yönetim Ofisi (PMO)
*   **Gizlilik Derecesi:** C2 - ŞİRKET İÇİ (INTERNAL USE ONLY)

---

# İÇİNDEKİLER

1.  **GİRİŞ VE KAPSAM**
    1.1. Dokümanın Amacı ve Hedef Kitle
    1.2. Projenin Arka Planı ve İş İhtiyacı
    1.3. Kapsam Dahili ve Harici Öğeler
    1.4. Referans Dokümanlar ve Standartlar

2.  **GENEL SİSTEM TANIMI**
    2.1. Ürün Perspektifi
    2.2. Ürün Fonksiyonları (Özet)
    2.3. Kullanıcı Karakteristikleri ve Personalar
    2.4. Genel Kısıtlamalar (Constraints)
    2.5. Varsayımlar ve Bağımlılıklar

3.  **SİSTEM MİMARİSİ (ARCHITECTURAL DESIGN)**
    3.1. Mimari Genel Bakış (High-Level Design)
    3.2. Katmanlı Mimari Detayları
        3.2.1. Sunum Katmanı (Presentation Layer)
        3.2.2. İş Mantığı Katmanı (Domain Layer)
        3.2.3. Veri Katmanı (Data Layer)
        3.2.4. Entegrasyon Katmanı (Python Bridge)
    3.3. Teknoloji Yığını (Tech Stack)
    3.4. Dağıtım Görünümü (Deployment View)

4.  **ALGORİTMİK DETAYLAR VE MANTIK TASARIMI (CORE LOGIC)**
    4.1. Ses İşleme Motoru (Audio Engine)
    4.2. Beat Tracking Algoritması (`beat_tracker.py`)
    4.3. Şerit Haritalama Algoritması (`lane_mapper.py`)
    4.4. Zorluk Hesaplama Matriksi (`difficulty.py`)
    4.5. JSON Serileştirme Formatı (`chart.py`)

5.  **VERİ TASARIMI (DATA DESIGN)**
    5.1. Veri Varlıkları ve İlişkiler (ER Diagram Descr.)
    5.2. Veritabanı Şeması (Room / SQLite)
    5.3. Dosya Sistemi Yapısı ve Önbellekleme
    5.4. Veri Göçü (Migration) Stratejileri

6.  **ARAYÜZ TASARIMI (UI/UX DESIGN)**
    6.1. Tasarım Prensipleri ve Renk Paleti
    6.2. Ekran Akış Diyagramları
    6.3. Ekran Detayları
        6.3.1. Splash ve Yükleme Ekranı
        6.3.2. Şarkı Seçim ve Kütüphane Ekranı
        6.3.3. Oyun Arayüzü (HUD)
        6.3.4. Sonuç ve İstatistik Ekranı
    6.4. Hata Mesajları ve Geri Bildirimler

7.  **GÜVENLİK VE UYUMLULUK**
    7.1. Erişim Kontrolü ve Yetkilendirme
    7.2. Veri Güvenliği ve Şifreleme
    7.3. Tersine Mühendislik Koruması (Obfuscation)
    7.4. GDPR/KVKK Uyumluluğu

8.  **TEST VE KALİTE GÜVENCE (QA)**
    8.1. Test Stratejisi
    8.2. Birim Testleri (Unit Tests)
    8.3. Entegrasyon Testleri
    8.4. UI ve Kullanılabilirlik Testleri
    8.5. Performans Testleri ve Benchmark Hedefleri

9.  **KURULUM, DAĞITIM VE BAKIM**
    9.1. Geliştirme Ortamı Kurulumu
    9.2. Yapılandırma Yönetimi (Gradle & Chaquopy)
    9.3. Sürüm Yönetimi (Versioning)
    9.4. Bakım Prosedürleri

10. **EKLER**
    10.1. Ek-A: Örnek Beatmap Dosyası
    10.2. Ek-B: Hata Kodları Listesi
    10.3. Ek-C: Lisans Bilgileri

---

# 1. GİRİŞ VE KAPSAM

## 1.1. Dokümanın Amacı ve Hedef Kitle
Bu "Yazılım Mimari ve Teknik Tasarım Dokümanı" (SDD), **Rhythm Game Mobile (Project Pulse)** projesinin teknik altyapısını, bileşenlerini, arayüzlerini ve verilerini en ince ayrıntısına kadar tanımlamak üzere hazırlanmıştır. Dokümanın temel amacı, projeye yeni katılan bir geliştiricinin, harici bir kaynağa ihtiyaç duymadan sistemi anlayabilmesi, geliştirebilmesi ve bakımını yapabilmesidir.

**Hedef Kitle:**
*   **Yazılım Geliştiriciler (Android & Python):** Kodlama standartlarını ve modül yapılarını anlamak için.
*   **Sistem Mimarları:** Büyük resmi ve bileşenler arası etkileşimi görmek için.
*   **Test Mühendisleri (QA):** Test senaryolarını ve kabul kriterlerini belirlemek için.
*   **Proje Yöneticileri:** Teknik riskleri ve kilometre taşlarını takip etmek için.

## 1.2. Projenin Arka Planı ve İş İhtiyacı
Mobil oyun pazarında ritim oyunları popüler bir kategori olmasına rağmen, mevcut çözümler genellikle iki ana sorunla karşı karşıyadır:
1.  **Sınırlı İçerik:** Sadece geliştiricinin lisansladığı şarkıların oynanabilmesi.
2.  **Çevrimiçi Bağımlılık:** Şarkı analizi veya veri indirme için sürekli internet bağlantısı gereksinimi.

**Project Pulse**, kullanıcının kendi cihazındaki yerel müzik kütüphanesini (MP3, WAV, OGG) kullanarak, prosedürel içerik üretimi (PCG - Procedural Content Generation) yöntemleriyle sonsuz ve kişiselleştirilmiş bir oyun deneyimi sunarak bu boşluğu doldurmayı hedefler. İş ihtiyacı, sunucu maliyetlerini (Backendless Architecture) sıfıra indirirken kullanıcı bağlılığını (Retention) artırmaktır.

## 1.3. Kapsam Dahili ve Harici Öğeler

**Kapsam Dahili (In-Scope):**
*   **Android Native Uygulama:** Kotlin dili ile geliştirilmiş, MVVM mimarisine sahip istemci.
*   **Gömülü Python Motoru:** Chaquopy SDK ile entegre edilmiş, NumPy ve Librosa kütüphanelerini kullanan analiz motoru.
*   **Yerel Veritabanı:** Room (SQLite) tabanlı skor, profil ve ayar yönetimi.
*   **Ses Motoru:** Düşük gecikmeli (Low-latency) ses çalma ve senkronizasyon altyapısı.
*   **Oyun Döngüsü:** SurfaceView üzerinde 60 FPS hedeflenmiş özel çizim motoru.

**Kapsam Harici (Out-of-Scope - Faz 1 İçin):**
*   **iOS Sürümü:** Apple ekosistemindeki kısıtlamalar ve Python entegrasyon zorlukları nedeniyle kapsam dışıdır.
*   **Çok Oyunculu Mod:** Gerçek zamanlı sunucu senkronizasyonu gerektiren PvP modları.
*   **Müzik Streaming Entegrasyonu:** Spotify/Apple Music API'leri (DRM kısıtlamaları nedeniyle).
*   **Bulut Kayıt:** Google Drive veya Firebase entegrasyonu (Faz 2'de eklenecektir).

## 1.4. Referans Dokümanlar ve Standartlar
*   IEEE 1016-2009 - Standard for Information Technology—Systems Design—Software Design Descriptions.
*   Google Android Developer Guidelines (Core App Quality).
*   PEP 8 -- Style Guide for Python Code.
*   Kotlin Coding Conventions.

---

# 2. GENEL SİSTEM TANIMI

## 2.1. Ürün Perspektifi
Project Pulse, bağımsız bir mobil uygulamadır. Herhangi bir merkezi sunucuya ihtiyaç duymadan çalışır (Offline-First). Sistem, Android işletim sisteminin sağladığı dosya sistemi, ses sürücüleri ve grafik arayüzleri ile doğrudan etkileşime girer.

## 2.2. Ürün Fonksiyonları (Özet)
1.  **Dosya Tarama ve İçe Aktarma:** Cihaz depolamasındaki ses dosyalarını indeksleme.
2.  **Otomatik Beatmap Üretimi:** Seçilen şarkıyı analiz edip ritim haritası (Chart) oluşturma.
3.  **İnteraktif Oynanış:** Müzik ritmine eş zamanlı dokunmatik oyun deneyimi.
4.  **Performans Değerlendirme:** Doğruluk, kombo ve zamanlama sapmasına göre puanlama.
5.  **İlerleme Takibi:** Yerel profil üzerinde seviye atlama ve istatistik tutma.

## 2.3. Kullanıcı Karakteristikleri ve Personalar
*   **Persona A (Hardcore Gamer):** Milisaniyelik gecikmelere duyarlı, yüksek zorluk seviyesini tercih eden, teknik detaylara hakim kullanıcı. Beklentisi: Stabilite ve Özelleştirme.
*   **Persona B (Casual Listener):** Kendi sevdiği şarkılarla vakit geçirmek isteyen, karmaşık ayarlardan kaçınan kullanıcı. Beklentisi: Kolay kullanım ve Görsellik.

## 2.4. Genel Kısıtlamalar (Constraints)
*   **Donanım:** Uygulama, minimum 2GB RAM ve 4 çekirdekli işlemciye sahip Android cihazlarda çalışmalıdır.
*   **İşletim Sistemi:** Minimum SDK Sürümü: 29 (Android 10).
*   **Depolama:** Uygulama boyutu 150 MB'ı geçmemelidir (Python runtime dahil).
*   **Zamanlama:** Python analiz işlemi, şarkı süresinin %10'unu geçmemelidir (3 dakikalık şarkı için max 18 sn).

## 2.5. Varsayımlar ve Bağımlılıklar
*   Kullanıcının cihazında DRM koruması olmayan ses dosyaları bulunduğu varsayılır.
*   Android cihazın `SoundPool` veya `AudioTrack` API'lerinin standart gecikme sürelerine uyduğu varsayılır.
*   Chaquopy kütüphanesinin gelecekteki Android sürümleriyle uyumlu kalacağı varsayılır.

---

# 3. SİSTEM MİMARİSİ (ARCHITECTURAL DESIGN)

## 3.1. Mimari Genel Bakış (High-Level Design)
Sistem, "Clean Architecture" prensiplerine sadık kalınarak tasarlanmıştır. Ancak, tipik bir Android uygulamasından farklı olarak, veri işleme katmanında (Data Layer) bir "Python Alt Sistemi" barındırır.

**Mimari Diyagramı (Kavramsal):**
`[UI Layer] <--> [ViewModel] <--> [Domain/UseCase] <--> [Repository] <--> [Data Source (Local DB / Python Bridge)]`

## 3.2. Katmanlı Mimari Detayları

### 3.2.1. Sunum Katmanı (Presentation Layer)
Bu katman, kullanıcı ile etkileşimi yönetir.
*   **Teknolojiler:** Kotlin, XML Layouts (veya Jetpack Compose), Android Architecture Components.
*   **Bileşenler:**
    *   `MainActivity`: Tek aktivite (Single Activity) yaklaşımı.
    *   `GameSurfaceView`: Oyun döngüsünü barındıran özel görünüm. `Canvas` üzerine çizim yapar.
    *   `ViewModels`: UI durumunu (State) tutar ve konfigürasyon değişikliklerinde (ekran döndürme vb.) veriyi korur.

### 3.2.2. İş Mantığı Katmanı (Domain Layer)
Uygulamanın "ne yaptığını" tanımlayan, platformdan bağımsız iş kurallarıdır.
*   **UseCase Örnekleri:**
    *   `AnalyzeSongUseCase`: Bir dosya yolu alır, `Chart` nesnesi döndürür.
    *   `CalculateScoreUseCase`: Vuruş zamanlamasına göre puanı hesaplar.
    *   `GetBestScoresUseCase`: Veritabanından geçmiş skorları getirir.

### 3.2.3. Veri Katmanı (Data Layer)
Verinin kaynağını (Veritabanı, Dosya, Python) soyutlar.
*   **Repository Pattern:** `SongRepository`, `ScoreRepository`.
*   **Data Sources:**
    *   `LocalDataSource`: Room DAO'larını kullanır.
    *   `PythonDataSource`: Chaquopy üzerinden Python betiklerini çağırır.

### 3.2.4. Entegrasyon Katmanı (Python Bridge)
Bu proje için özel olarak geliştirilmiş katmandır. Kotlin ve Python arasındaki veri marshalling/unmarshalling işlemlerini yapar.
*   **Sınıf:** `PythonServiceWrapper`
*   **Görev:** Büyük JSON verilerini string olarak alır, GSON kütüphanesi ile Kotlin nesnelerine (`ChartEntity`) dönüştürür. Hata durumlarını (`PyException`) yakalar ve anlamlı Kotlin hatalarına çevirir.

## 3.3. Teknoloji Yığını (Tech Stack)

| Kategori | Teknoloji / Kütüphane | Sürüm / Notlar |
| :--- | :--- | :--- |
| **Programlama Dili** | Kotlin | 1.9.0+ |
| **Script Dili** | Python | 3.8 (via Chaquopy) |
| **Veritabanı** | Room (SQLite) | 2.6.0+ |
| **DI (Dependency Inj.)** | Hilt (Dagger) | 2.48+ |
| **Asenkron İşlem** | Coroutines & Flow | Kotlin Standardı |
| **Ses Analizi** | Librosa, NumPy | Python Paketleri |
| **JSON Parser** | Gson | Google |
| **Build System** | Gradle | 8.0+ |

## 3.4. Dağıtım Görünümü (Deployment View)
Uygulama, Google Play Store üzerinden `.aab` (Android App Bundle) formatında dağıtılır. Chaquopy eklentisi, kullanıcının cihaz mimarisine (ABI: `arm64-v8a`, `armeabi-v7a`) uygun Python kütüphanelerini otomatik olarak pakete dahil eder. `x86` ve `x86_64` mimarileri, üretim ortamında (Production) APK boyutunu küçültmek için hariç tutulur.

---

# 4. ALGORİTMİK DETAYLAR VE MANTIK TASARIMI (CORE LOGIC)

Bu bölüm, projenin "kalbi" olan Python modüllerinin çalışma prensiplerini matematiksel ve mantıksal detaylarla açıklar.

## 4.1. Ses İşleme Motoru (Audio Engine)
*Dosya:* `analyzer.py`
Ses dosyası yüklenirken `librosa.load` fonksiyonu kullanılır. Performans optimizasyonu için:
*   **Sample Rate (SR):** 22050 Hz'e düşürülür (Analiz için yeterlidir, 44.1kHz gereksiz işlem yükü yaratır).
*   **Mono:** Stereo kanallar birleştirilerek tek kanala indirilir.

**Kod Akışı (Python):**
```python
def load_audio(file_path):
    # Load audio with reduced sampling rate for speed
    y, sr = librosa.load(file_path, sr=22050, mono=True)
    return y, sr
```

## 4.2. Beat Tracking Algoritması
*Dosya:* `beat_tracker.py`
Müziğin temposunu ve vuruş anlarını tespit eder.
1.  **Onset Envelope:** Ses sinyalinin enerjisindeki ani değişimler (türev) hesaplanarak bir zarf (envelope) çıkarılır.
2.  **Peak Picking:** Zarf üzerindeki tepe noktaları (peaks) bulunur.
3.  **Dynamic Programming:** Vuruşların periyodik olması beklendiğinden, tepe noktaları arasından en tutarlı aralıkları seçen bir dinamik programlama algoritması çalıştırılır.

**Matematiksel Model:**
$$ O(t) = \sum_{k} |S_k(t) - S_k(t-1)|^+ $$
Burada $S_k(t)$, spektrogramın logaritmik genliğidir.

## 4.3. Şerit Haritalama Algoritması
*Dosya:* `lane_mapper.py`
Zaman ekseninde bulunan vuruşların, oyun alanındaki 5 şeride (Lane) dağıtılması işlemidir. Rastgele dağıtım yerine, sesin frekans karakteristiği kullanılır.

**Algoritma Adımları:**
1.  **Spektral Centroid Hesabı:** Her vuruş anı ($t$) için, o andaki frekans bileşenlerinin ağırlıklı ortalaması (Center of Mass) hesaplanır.
    $$ Centroid = \frac{\sum f \cdot M(f)}{\sum M(f)} $$
2.  **Normalizasyon:** Şarkı boyunca elde edilen tüm centroid değerleri toplanır.
3.  **Yüzdelik Dilimleme (Percentile Binning):**
    *   Değer < 20. persentil => **Lane 1** (En Bas)
    *   20 < Değer < 40 => **Lane 2**
    *   40 < Değer < 60 => **Lane 3** (Mid)
    *   60 < Değer < 80 => **Lane 4**
    *   Değer > 80. persentil => **Lane 5** (En Tiz)

Bu yöntem, bas davulların (Kick) solda, zillerin (Hi-hat) sağda olmasını garanti eder.

## 4.4. Zorluk Hesaplama Matriksi
*Dosya:* `difficulty.py`
Oluşturulan haritanın zorluğu, **NPS (Notes Per Second)** değeri üzerinden hesaplanır.
*   **Easy:** Max NPS < 2.0
*   **Medium:** 2.0 < Max NPS < 4.0
*   **Hard:** Max NPS > 4.0

Ayrıca "Stream" (kesintisiz notalar) ve "Jump" (şeritler arası uzak mesafe) kalıpları tespit edilerek zorluk puanına katsayı olarak eklenir.

## 4.5. JSON Serileştirme Formatı
*Dosya:* `chart.py`
Kotlin tarafına gönderilecek nihai veri yapısıdır.

```json
{
  "metadata": {
    "title": "Song Title",
    "bpm": 120.5,
    "total_notes": 450
  },
  "notes": [
    { "time": 1050, "lane": 1, "type": "tap" },
    { "time": 1200, "lane": 3, "type": "hold", "duration": 500 }
  ]
}
```

---

# 5. VERİ TASARIMI (DATA DESIGN)

## 5.1. Veri Varlıkları ve İlişkiler (ER Diagram Descr.)
Sistemde iki ana varlık grubu vardır:
1.  **Statik Medya Verisi:** Şarkılar ve analiz sonuçları.
2.  **Dinamik Kullanıcı Verisi:** Skorlar ve istatistikler.
İlişki: Bir Şarkı (Song) birden fazla Skora (Score) sahip olabilir (1-to-Many).

## 5.2. Veritabanı Şeması (Room / SQLite)

**Entity: SongEntity**
```kotlin
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val fileHash: String, // MD5
    val title: String,
    val artist: String,
    val duration: Long,
    val filePath: String,
    val bpm: Float,
    val dateAdded: Long
)
```

**Entity: ScoreEntity**
```kotlin
@Entity(
    tableName = "scores",
    foreignKeys = [ForeignKey(
        entity = SongEntity::class,
        parentColumns = ["fileHash"],
        childColumns = ["songHash"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["songHash"])]
)
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songHash: String,
    val score: Int,
    val maxCombo: Int,
    val accuracy: Float,
    val grade: String, // S, A, B, C, F
    val playedAt: Long
)
```

## 5.3. Dosya Sistemi Yapısı ve Önbellekleme
Android `Context.filesDir` altında aşağıdaki yapı kurulur:
*   `/charts/`: JSON formatındaki analiz dosyaları. Dosya adı: `{fileHash}.json`.
*   `/logs/`: Hata logları (release modunda temizlenir).
*   `/cache/`: Geçici ses dönüştürme dosyaları.

## 5.4. Veri Göçü (Migration) Stratejileri
Uygulama güncellemelerinde veri kaybını önlemek için Room Migration stratejileri belirlenmiştir.
*   **v1 -> v2:** `scores` tablosuna `perfect_count` ve `miss_count` sütunları eklenecek.
*   **Strateji:** `ALTER TABLE scores ADD COLUMN perfect_count INTEGER DEFAULT 0 NOT NULL` SQL komutu çalıştırılır.

---

# 6. ARAYÜZ TASARIMI (UI/UX DESIGN)

## 6.1. Tasarım Prensipleri ve Renk Paleti
*   **Tema:** Cyberpunk / Neon. Koyu arkaplan üzerine parlak renkler.
*   **Renkler:**
    *   Birincil: Neon Mavi (#00F0FF)
    *   İkincil: Neon Pembe (#FF00AA)
    *   Arkaplan: Derin Siyah (#050505)
*   **Tipografi:** Fütüristik, monospace veya geometrik sans-serif fontlar (örn: Rajdhani, Orbitron).

## 6.2. Ekran Akış Diyagramları
`Splash -> (Permission Check) -> Home -> Song List -> (Analysis Dialog) -> Difficulty Select -> Game HUD -> Result -> Song List`

## 6.3. Ekran Detayları

### 6.3.1. Splash ve Yükleme Ekranı
*   **Fonksiyon:** Uygulama kaynaklarını yükler, Python motorunu ısıtır (warm-up).
*   **Görsel:** Logo ortada, altta "System Initializing..." yazısı ve progress bar.

### 6.3.2. Şarkı Seçim ve Kütüphane Ekranı
*   **Fonksiyon:** Cihazdaki MP3'leri listeler.
*   **Bileşenler:** `RecyclerView` kullanılır. Her satırda Albüm kapağı (varsa), Şarkı adı, Sanatçı ve En iyi skor gösterilir.
*   **Arama:** Üst barda arama ikonu ile filtreleme yapılır.

### 6.3.3. Oyun Arayüzü (HUD)
*   **Yapı:** Ekran 5 dikey şeride bölünmüştür.
*   **Judgement Line:** Ekranın altından %10 yukarıda sabit bir çizgi.
*   **Scoreboard:** Sol üstte anlık skor.
*   **Combo:** Ekranın tam ortasında, her vuruşta büyüyen sayaç.
*   **Health Bar:** Hata yaptıkça azalan, başarılı vuruşta artan can çubuğu (opsiyonel/zor moda özel).

### 6.3.4. Sonuç ve İstatistik Ekranı
*   **Özet:** Büyük harf notu (S, A, B...).
*   **Detay:** Perfect/Great/Good/Miss sayıları tablo halinde.
*   **Grafik:** Zaman içindeki doğruluk değişimini gösteren çizgi grafik (Line Chart).

## 6.4. Hata Mesajları ve Geri Bildirimler
*   "Dosya formatı desteklenmiyor." (Toast mesajı)
*   "Analiz başarısız oldu. Lütfen dosyanın bozuk olmadığından emin olun." (Dialog)
*   "Depolama izni reddedildi. Oyunun çalışması için bu izin gereklidir." (Snackbar + Settings Action)

---

# 7. GÜVENLİK VE UYUMLULUK

## 7.1. Erişim Kontrolü ve Yetkilendirme
*   Uygulama "Offline" çalıştığı için kullanıcı girişi (Login) mekanizması yoktur.
*   Android `READ_EXTERNAL_STORAGE` (API < 33) veya `READ_MEDIA_AUDIO` (API >= 33) izinleri, çalışma zamanında (Runtime Permission) istenir. Kullanıcı izin vermezse, sadece dahili demo şarkılar listelenir.

## 7.2. Veri Güvenliği ve Şifreleme
*   Kullanıcı skorları yerel veritabanında düz metin (plaintext) olarak saklanır. Ancak, skor manipülasyonunu (Cheating) önlemek için, her skor kaydına bir `checksum` (SHA-256 hash) eklenebilir: `Hash(Score + Timestamp + SecretSalt)`. Oyun yüklenirken bu hash kontrol edilir, uyuşmazlık varsa skor geçersiz sayılır.

## 7.3. Tersine Mühendislik Koruması (Obfuscation)
*   **R8 / ProGuard:** Release derlemesinde aktiftir. Sınıf ve metod isimleri (`a.b()`) şeklinde kısaltılır.
*   **Python:** Chaquopy, kaynak kodları ZIP dosyası içinde saklar. Tam güvenlik sağlamasa da sıradan kullanıcıların erişimini engeller.

## 7.4. GDPR/KVKK Uyumluluğu
*   Uygulama, kişisel veri (isim, e-posta, konum) toplamaz.
*   Cihazdaki müzik dosyaları sadece analiz (RAM üzerinde) için kullanılır, sunucuya yüklenmez veya kalıcı olarak kopyalanmaz.

---

# 8. TEST VE KALİTE GÜVENCE (QA)

## 8.1. Test Stratejisi
"Test Piramidi" yaklaşımı benimsenmiştir:
1.  **Birim Testleri (%60):** Python algoritmaları ve Kotlin ViewModel mantığı.
2.  **Entegrasyon Testleri (%30):** Veritabanı ve Python Köprüsü.
3.  **UI Testleri (%10):** Ekran geçişleri ve kullanıcı akışları.

## 8.2. Birim Testleri (Unit Tests)
**Örnek Python Testi (`test_lane_mapper.py`):**
```python
def test_centroids_to_lanes():
    # Mock data: artan frekanslar
    centroids = [100, 200, 300, 400, 500] 
    lanes = centroids_to_lanes(centroids)
    assert lanes == [1, 2, 3, 4, 5]
```

## 8.3. Entegrasyon Testleri
*   **Senaryo:** `SongRepository`, Python'dan dönen bozuk bir JSON verisini nasıl işliyor?
*   **Beklenen:** Uygulama çökmemeli, `Result.Error` dönmeli ve UI'da hata mesajı gösterilmeli.

## 8.4. UI ve Kullanılabilirlik Testleri
*   Farklı ekran boyutlarında (Tablet, Telefon, Foldable) arayüzün düzgün ölçeklendiği (ConstraintLayout kullanımı) doğrulanır.
*   "Monkey Test" aracı ile rastgele dokunuşlar yapılarak uygulamanın stabilitesi test edilir.

## 8.5. Performans Testleri ve Benchmark Hedefleri
*   **Memory Leak:** LeakCanary kütüphanesi entegre edilerek Activity sızıntıları izlenir.
*   **FPS:** Android Profiler ile oyun sırasındaki GPU render süresi ölçülür. Hedef: Her kare < 16ms.

---

# 9. KURULUM, DAĞITIM VE BAKIM

## 9.1. Geliştirme Ortamı Kurulumu
1.  **JDK:** OpenJDK 17 yükleyin.
2.  **Android Studio:** Hedgehog veya daha yeni sürüm.
3.  **Python:** Sisteminizde Python 3.8+ yüklü olmalıdır.
4.  **Repo:** Git deposunu klonlayın.
5.  **Build:** `local.properties` dosyasına `ndk.dir` ve `sdk.dir` yollarını ekleyin. `./gradlew assembleDebug` komutunu çalıştırın.

## 9.2. Yapılandırma Yönetimi (Gradle & Chaquopy)
`app/build.gradle` dosyası, Python sürümünü ve pip paketlerini yönetir. Yeni bir Python kütüphanesi (örn: scipy) eklenecekse, `python { pip { install "scipy" } }` bloğuna eklenmelidir.

## 9.3. Sürüm Yönetimi (Versioning)
Semantik Versiyonlama (SemVer) kullanılır: `MAJOR.MINOR.PATCH`.
*   **MAJOR:** Uyumsuz API değişiklikleri (Veritabanı şeması tamamen değişirse).
*   **MINOR:** Geriye uyumlu yeni özellikler (Yeni oyun modu).
*   **PATCH:** Hata düzeltmeleri.

## 9.4. Bakım Prosedürleri
*   Her ay Android güvenlik yamalarına göre bağımlılıklar güncellenir.
*   Kullanıcı geri bildirimlerine göre (Google Play Console) kilitlenme (ANR/Crash) raporları incelenir ve yamalar yayınlanır.

---

# 10. EKLER

## 10.1. Ek-A: Örnek Beatmap Dosyası
*(Tam JSON yapısı)*
```json
{
  "version": "1.0",
  "metadata": {
    "song_id": "f4a1c8...",
    "title": "Nightcall",
    "artist": "Kavinsky",
    "duration_ms": 256000,
    "bpm": 128.0,
    "offset_ms": 0
  },
  "difficulty": {
    "level": "hard",
    "note_count": 542,
    "nps": 2.1
  },
  "notes": [
    { "t": 1050, "l": 1, "d": 0, "type": "tap" },
    { "t": 1420, "l": 5, "d": 0, "type": "tap" },
    { "t": 1800, "l": 3, "d": 400, "type": "hold" }
  ]
}
```

## 10.2. Ek-B: Hata Kodları Listesi
| Kod | Mesaj | Çözüm |
| :--- | :--- | :--- |
| `ERR_PY_01` | Module not found | Python kurulumunu kontrol et. |
| `ERR_IO_02` | File permission denied | Ayarlardan depolama izni ver. |
| `ERR_AUDIO_03` | Decoding failed | Dosya bozuk veya desteklenmiyor (DRM). |

## 10.3. Ek-C: Lisans Bilgileri
*   **Chaquopy:** Ticari/GPL lisans modeline sahiptir. Proje açık kaynak ise GPL, ticari ise lisans anahtarı gerektirir.
*   **Librosa:** ISC Lisansı (MIT uyumlu).
*   **Gson:** Apache 2.0.

---
**DOKÜMAN SONU**
