
RHYTHM GAME: BAŞTAN SONA MÜHENDİSLİK KILAVUZU 

  Sürüm:   1.0.0 
  Uyumluluk:   Android Jetpack Compose (API 26+) & Python 3.10+ FastAPI
  Hazırlayan:   Avni Cem Ersoy (Baş Sistem Mimarı)




# İÇİNDEKİLER

1.   Sistem Mimarisi ve Teknoloji Seçimleri (Neden Bu Teknolojiler?)  
2.   Veri Modelleri, Veritabanı Şemaları (ER) ve Depolama Mimarileri  
3.   Akış Diyagramları: Uygulama İçi Veri Hattı (Pipeline)  
4.   Arka Uç (Backend): Python FastAPI ve Asenkron Çekirdek  
5.   Ses Sinyal İşleme (DSP) ve Librosa Algoritmaları (Matematiksel Alt Yapı)  
6.   Android Mimari: Jetpack Compose, Hilt ve Clean Architecture  
7.   Mobil Oyun Motoru: SurfaceView, 60 FPS Render ve ExoPlayer Audio Sync  
8.   Chaquopy: Kenar Bilişim (Edge Computing) Gelecek Planı Geçiş Kodları  
9.   Klonlama, Derleme ve Sorun Giderme Koruması  
10.   API Sözleşmesi (REST Endpoints & Swagger Contract)  
11.   Proje Klasör Ağacı (Directory Structure) ve İskelet Mimarisi  
12.   Tasarım Dili (Design System) ve Neon Siberpunk Mimarisi  
13.   Güvenlik, Exception Handling (Hata Yönetimi)  
14.   Gelecek Vizyonu ve Beklentiler (Proje Master Planı)  
15.   Sistem Güvenliği ve Tersine Mühendislik Koruması (Security & Obfuscation)  
16.   Derleme Ortamları (Build Flavors) ve Varyasyonlar  
17.   Git Mimarisi ve Takım Çalışması (Git Flow)  
18.   Telemetri, APM (App Performance Monitoring) ve UX İzleme  
19.   Donanım Limitasyonları ve Cihaz Matrisi (Hardware Targets)  
20.   Kodlama Standartları ve Stil Rehberi (Code Conventions)  
21.   Yerelleştirme (L10n), Küreselleşme (I18n) ve Erişilebilirlik  
22.   Monetizasyon (Gelir Modeli) ve In-App Purchases (IAP) Mimarisi  
23.   Veri Gizliliği, GDPR ve KVKK Uyumluluğu  
24.   Merkezi Oyun Mekanikleri ve Oyun Fiziği Matematiği  
25.   Oyun Durumu (Game State) Mimarisi ve Sonlu Durum Makinesi (FSM)  
26.   DevOps ve CI/CD Otomasyon Rehberi (GitHub Actions)  
27.   Bellek Profili (Memory Profiling) ve Performans Bütçeleri  
28.   Açık Kaynak Lisansları ve Hukuki (Legal) Uyumluluk  
29.   Sunucu Güvenliği ve API İstek Sınırlandırmaları (Rate Limiting & JWT)  
30.   Veritabanı Taşıma ve Sürüm Güncellemeleri (Room Migrations)  
31.   Analitik Olayları Sözlüğü (Firebase Analytics Event Dictionary)  
---




# BÖLÜM 1: SİSTEM MİMARİSİ VE TEKNOLOJİ SEÇİMLERİ (NEDEN BU TEKNOLOJİLER?)

Rhythm Game, yüksek yoğunluklu ses analizleri (Fourier Analizleri) ile ultra düşük gecikmeli mobil kullanıcı arayüzünü (Jetpack Compose UI) birleştirmesi gereken komplike bir üründür. Seçilen hiçbir teknoloji tesadüfi değildir; hepsi belirli darboğazları (bottleneck) aşmak üzere seçilmiştir.

## 1.1 Teknoloji Stratejisi ve Doğrulayıcı Nedenler Tablosu

| Teknoloji / Kütüphane | Konum | Neden Seçildi? / Alternatiflerden Üstünlüğü Nedir? |
| :--- | :--- | :--- |
|   Python & Librosa   | Sinyal İşleme Motoru | Müzikal sinyal işleme (MIR) alanında akademi standardıdır. C++ veya Java kütüphanelerinden (örneğin TarsosDSP) en az 10 yıl ileride spectral analiz sağlar. Notaların frekans haritalamasını milisaniye bazında yapabilir. |
|   NumPy   | Matris Veri İşleme | Librosa'nın döndürdüğü on binlerce satırlık Float32 dizilerini (Audio Arrays) Python'un yavaş (GIL) döngüsü yerine doğrudan C arka-ucunu (C Backend) kullanarak saniyenin kesirlerinde hesaplar. |
|   FastAPI   | REST Sunucusu | Django veya Flask çok ağır (Synchronous) çalışır. FastAPI, Uvicorn altyapısıyla ASGI (Asynchronous Server Gateway Interface) destekli çalıştıgı için 1 saniyede yüzlerce kullanıcının MP3 dosyasını bloke (Block) olmadan alır. |
|   Jetpack Compose   | Mobil Arayüz | Eski XML Android arayüzleri, "Imperative" (Emirsel) çalışırken yavaş renderlanıyordu. Compose "Declarative" (Bildirimsel) çalışarak React Hook mantığıyla sadece State'i değişen değişkeni anlık günceller. |
|   SurfaceView & Canvas   | Oyun Motoru | Standart Compose öğeleri (Box, Column) 60 FPS saniyede 150 nota çizimi yaptığında "Recomposition" şişmesine sebep olurdu. SurfaceView, oyun çizimini (Rendering) Android'in UI ana thread'inden asenkron (Dedicated Render Thread) bir donanım hızlandırıcısına (GPU Hardware Acceleration) taşır. |
|   Room Database   | Mobil Veritabanı | Saf SQLite yazmak, veri dökümleri (Types) için risklidir. Room, doğrudan Kotlin Coroutine kütüphanesine `Flow<List<Song>>` olarak bağlı olduğu için, arka planda bir şarkı insilirse ya da eklenirse EKRAN (UI) 0 kod yazımıyla kendini yeniler (Reactive Data). |
|   ExoPlayer (Media3)   | Ses Çalar | Standart `android.media.MediaPlayer` donanım buffer gecikmelerine (Latency) sahiptir (Bazen 200ms sapma). ExoPlayer, AudioTrack API'sinin en derin yollarını kullanarak oyunu nota düşüş animasyonu ile milisaniyesi milisaniyesine kilitler (Sync). |
|   Chaquopy   | (Gelecek Vizyonu) Edge AI | Sunucu masraflarını (AWS EC2, Bandwidth) sıfıra indirgemek için seçildi. Android'in C++ (NDK) katmanı üzerinden cihazın içinde gerçek bir Python Sanal Makinesi (VM) ayağa kaldırır ve Librosa algoritmalarını telefonda (Offline) çalıstırır. |

---






# BÖLÜM 2: VERİ MODELLERİ, VERİTABANI ŞEMALARI (ER) VE DEPOLAMA MİMARİLERİ

Verilerin istemcide (Android) ve ileride Sunucu tarafında (Python) standartlaştırılabilmesi için keskin bir JSON sözleşmesi (Contract) oluşturulmuştur. Bu bölüm uygulamanın temel organlarını çizer.

## 2.1 İstemci ve Sunucu Ortak Sözleşmesi: JSON Olarak Müzik Haritaları (Chart Contracts)

Proje, herhangi bir ritim parçasını şu JSON protokolünde yorumlar. Bu yapı, hem SQLite'ta hem de Room'da (Android) TEXT/BLOB olarak korunur.

```json
{
  "song_id": "8fa2-4cbd-91ba",
  "title": "Megalovania.mp3",
  "duration_ms": 156000,
  "bpm": 120.0,
  "difficulty": "hard",
  "notes": [
    {
      "time_ms": 4000,     # Nota şarkının tam 4. saniyesinde çizgiye vurmalıdır
      "lane": 1,           # Gitar Sol veya Davul (Bas Frekanslar 1 ve 2. şerittedir)
      "duration_ms": 0,    # Sıfır ise kullanıcı ekrana Tıkla/Bırak (TAP) yapmalıdır
      "type": "tap"
    },
    {
      "time_ms": 4500,     # Nota şarkının tam 4.5. saniyesinde vurulmalıdır
      "lane": 5,           # Sağ şerit (Tiz Ses / Crash / Hi-Hat zilleri bulunur)
      "duration_ms": 1200, # Kullanıcı bu notaya dokunduğunda 1.2 saniye parmağını basılı sürdürmelidir 
      "type": "hold"       # Basılı tutma mekaniği (HOLD)
    }
  ]
}
```

## 2.2 Arka Uç Veritabanı (Server - SQLite / SQLAlchemy) Şeması
Sunucuda veriler `backend/app/models/` altındaki `database.py` tanımlarına göre ilişkisel veri tabanında tutulur. Asenkron okuma için `aiosqlite` entegredir.

[BURAYA "ARKA UÇ VERİTABANI (ER) DİYAGRAMI" GELECEK]
  Neden JSON Kolonu Kullanılıyor (notes_json)?    
Relational (İlişkisel) sistemlerde her notayı `notes` adında bir tabloya koysaydık; 3 dakikalık bir şarkıda ortalama 600 nota olurdu. 10.000 Şarkıda veritabanı 6 Milyon nota satırından şişer, Read-Write gecikmeleri API'yi (FastAPI) çökertirdi. JSON kullanarak veritabanı bir NoSQL (MongoDB varyasyonlu) motor gibi O(1) hızında çalışır.

## 2.3 Ön Yüz Veritabanı (Android - Room Database) Şeması ve DAO Yapısı
Android tarafında kodlar `app/src/main/java/com/rhythmgame/data/local/` altında konumlanmıştır. Jetpack Room ORM Kullanılır.

[BURAYA "ÖN YÜZ VERİTABANI (ROOM) CLASS DİYAGRAMI" GELECEK]

  `SongDao.kt` Özel Fonksiyonları: (Atomic Ops)  
Aşağıdaki özel fonksiyon, Android'in eski skorunu kontrol eder; sadece eğer yeni elde edilen skor eskisinden DAHA BÜYÜKSE (Record Breaking) Update yapar. Bu işlem CPU'nun `SELECT` ve `UPDATE` işlemini aynı transaction (işlem) içerisine gömerek thread-safe (eşzamanlı kilitli) yürütmesini sağlar (SQLite `AND highScore < :score` kuralı).
```kotlin
@Query("UPDATE songs SET highScoreMedium = :score WHERE songId = :songId AND highScoreMedium < :score")
suspend fun updateHighScoreMedium(songId: String, score: Int)
```

---

# BÖLÜM 3: AKIŞ DİYAGRAMLARI: UYGULAMA İÇİ VERİ HATTI (PIPELINE)

Kullanıcı arayüzünde "Bir Şarkı Seçildi (Import)" anından, o şarkının Müzik Oyununda Oynanmasına (Play) kadar geçen arka uç ve ön uç dansının detaylı diyagramı:

[BURAYA "UYGULAMA İÇİ VERİ HATTI SEQUENCE DİYAGRAMI" GELECEK]

---

# BÖLÜM 4: ARKA UÇ (BACKEND): PYTHON FASTAPI VE ASENKRON ÇEKİRDEK

FastAPI yapısı `backend/app` dizininde mikro-hizmet (microservice) mantığıyla yazılmıştır.

## 4.1 Ana Uygulama Çatısı (`main.py`)
`Uvicorn.run` ile ayağa kalkan modülde kritik bir `@app.on_event("startup")` Lifecycle'i (Yaşam Döngüsü) bulunur.
```python
@app.on_event("startup")
async def startup():
    # Asenkron (non-blocking) olarak SQLite tablolarını oluştur
    await init_db()

# Bütün IP'lerden gelecek REST çağrılarına Aç (Cloud Deploy Cihazları için mecburiyet)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[" "],
    allow_credentials=True,
    allow_methods=[" "],
    allow_headers=[" "],
)
```

## 4.2 Rest API Rotaları (`api/routes.py`)
Mimarimizde `File(...)` kullanılarak gelen medya `UploadFile` objesi diske senkron yazılır. Ancak `await file.read()` metodu bu sorunu AsyncIO'ya çekmektedir.

```python
# app/api/routes.py
@router.post("/songs/upload", response_model=UploadResponse)
async def upload_song(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db)
):
    song_id = file_manager.generate_song_id()
    file_path = file_manager.get_audio_path(song_id)

    # Fiziksel Diske Asenkron Yazım I/O
    content = await file.read()
    with open(file_path, "wb") as f:
        f.write(content)

    # Hemen kullanıcıya Dönmeden background'a ağır FFT yükleri atanıyor
    background_tasks.add_task(process_audio_task, song_id, file_path)

    return UploadResponse(song_id=song_id, status="uploading")
```

  Thread Mimarisine Dikkat!   FastAPI `background_tasks`, fonksiyonlarınızı varsayılan (Default) Event Loop üzerinde ASYNC ise asyncio ile, DEF (senkron) ise bir ThreadPool de atar. `process_audio_task`'ın `def` ile tanımlandığı repodan teyit edilebilir; böylece Event Loop bloklanmaz ve yüzlerce insan API'ye istek atmaya devam edebilir.

---

# BÖLÜM 5: SES SİNYAL İŞLEME (DSP) VE LİBROSA ALGORİTMALARI (MATEMATİKSEL ALT YAPI)

Bu bölüm, sıradan bir Audio Analizi ile "Eğitimli Ritim Oyunu" analizi arasındaki ince çizgiyi belirler. Hedefimiz 1 saniyede çıkan tüm gürültülere nota (Spam) koymak yerine, müziğin "Grid" (kafes / vuruş noktaları) dokusunu keşfetmektir.
Kodlar `backend/app/analysis/` dizinindedir.

## 5.1 Adım Adım Sinyal Akışı (`pipeline.py`)



Kaba ses dalgasının JSON'a dönüştürülme serüveni:
1.    Audio Yükleme:   `librosa.load(y, sr=22050)` ile performans için Sample Rate kasıtlı daraltılır, ses mono (Sıralı dizi) hale getirilir.
2.    Ölçüm ve Nabız (BPM):   `beat_tracker.py` içindeki analiz ile Şarkının Metronom hızı bulunur.
3.    Onset Algılama:   Şarkımdaki gürültülü patlama noktaları bulunur (Davullar, Tiz Ziller).
4.    Spektral Analiz (Lane Map):   Ses Tizse sağa (Şerit 5), Pesse sola (Şerit 1).

## 5.2 Kuvvetli Müzik Teorisini Kodlamak: Snapping (Ağ Kavrama)
`beat_tracker.py` içindeki efsanevi `snap_to_beats()` metodunu analiz edelim.

Gitar Hero, Tap Tap Revenge gibi oyunlarda hiçbir zaman notalar %33,2 veya %81,4 gibi garip ara bölmelere düşmez. Temel Notalar; Yarı Notalar veya Çeyrek/Onaltılık notalardır.

```python
def snap_to_beats(onset_times: np.ndarray, beat_times: np.ndarray, subdivision: int = 4):
    """
    Kaba bütün Onsetleri (vuruşlar) matematiksel onaltılık (sixteenth) kafese hapset.
    """
    grid = []
    # 1. Şarkının kalbi olan Beats'lerin her birisinin arasını eşit olarak 4 parçaya (subdivision) böl!
    for i in range(len(beat_times) - 1):
        beat_start = beat_times[i]
        beat_end = beat_times[i + 1]
        sub_duration = (beat_end - beat_start) / subdivision # Müzikteki 16'lık süresi
        for s in range(subdivision):
            grid.append(beat_start + s   sub_duration)
            
    # .... Array'e Düzleştir (Flatten/Set)

    # 2. Asıl büyü: Her onset (Darbe), bu Grid ağı üzerindeki en yakın mıknatısa çekilsin!
    snapped = np.empty_like(onset_times)
    for i, onset in enumerate(onset_times):
        # NP.ArgMin fonksiyonu Array subtraction ile Mesafeyi Bulup index döner
        nearest_idx = np.argmin(np.abs(grid - onset)) 
        snapped[i] = grid[nearest_idx]

    # ... Deduplicate (Aynı grid noktasına düşen ikiz notaları silmek) 
    return snapped_times
```
Bu matematik, oyundaki "Spam Notasyon" hatasını çözer. Eğer çok hızlı davul atan bir death metal dinleniyorsa sistem on onalti notayı filtreler, ve notaları ritmin 1,2,3,4 kısımlarına pürüzsüz oturtur.

## 5.3 Spektral Ağırlık Merkezi Yüzdeleri (Lane Mapping Matematiği)
Ses davulsa ekranın solunda (Şerit 1), zille sağında (Şerit 5). Bunu Frekans analizi ile yaparız.
  `librosa.feature.spectral_centroid` : Ses frekansının "kütle merkezini" hesplar. Pes bir davul vurunca merkez örneğin `200Hz` çıkar. Tiz bir Hi-Hat zil vurunca `3000Hz` çıkar.

  Fakat tehlike nedir?   Şarkı saf elektroniksa sadece tizler bulunur, saf hip-hopsa sadce baslar bulunur. Eğer sabit bir sınır koysaydık "Eğer 200 altıysa sol şeride koy" şeklinde; kimi şarkılarda hep 1-2 şeridine kimesinde hep 4-5. şeritlere nota konur, oyun SIKICI olurdu! O yüzden   Bağıl (Relative/Percentile) Dağıtım   mimarisi geliştirilmiştir.

```python
def centroids_to_lanes(centroids: list[float]) -> list[int]:
    """Her şarkı için kütle merkezinin "Sıralamasına" (Percentile) bakar"""
    arr = np.array(centroids)
    # Şarkının en pes ilk %20 dilimini bul, %40'ı bul... vs
    p20, p40, p60, p80 = np.percentile(arr, [20, 40, 60, 80])

    lanes = []
    for c in centroids:
        # Eğer nota şarkının geneline göre pes'se Sola (1)
        if c <= p20: lanes.append(1)
        elif c <= p40: lanes.append(2)
        elif c <= p60: lanes.append(3)
        # Eğer nota şarkının geneline göre tizse Sağa (4,5) at
        elif c <= p80: lanes.append(4)
        else: lanes.append(5)
    return lanes
```
Bu fonksiyon Android ekranda her şarkının 5 şeridini Eşit ve Dengeli Dağılım ile kaplar! Büyük bir Game UX (Kullanıcı Deneyimi) mühendisliğidir.

## 5.4 Zorluk Seçimi Algoritmaları (`difficulty.py`)
Müzik içinde bir darbenin nereye çaptığını bildiğimizden Classify işlemini yapıyoruz: `ON_BEAT` (Tam Vuruş), `HALF_BEAT` (Yarım), `QUARTER_BEAT` (Onaltılık).
Özel algoritma basittir:
    EASY (Kolay):   Sadece `ON_BEAT` barındıran notaları tut, `HALF` ve `QUARTER` geldiğinde onları array'dan uçur! Notaları sadce 1-3-5 lane'lerine hapis et.
    MEDIUM (Orta):   `QUARTER` atılır, Sadece `ON_BEAT` ve `HALF` kalsın. 5 Lane aktif.
    HARD (Zor):   Silinen notu yoktur (`QUARTER` ler barındırılır). Fakat ek bir algoritma: Eğer iki nota asında aramızda `>400ms` uzun bir boşluk varsa, oyuncu sıkılmasın diye o `TAP` notasını, müzikal bir uzatma (Sustain/Synths) akorunu varmış gibi `HOLD` (Basılı Tutma) statüsüne çevir! Müzisyen bunu parmaklarıyla çalar gibi hissedecektir.

---

# BÖLÜM 6: ANDROID MİMARi: JETPACK COMPOSE, HILT VE CLEAN ARCHITECTURE

Android katmanı `android/app/src/main/java/com/rhythmgame/` içindedir.
MVI (Model-View-Intent) akış tabanlı bir Redux mimarisinin modern Kotlin karşılığı (ViewModel + StateFlow) kullanılmaktadır.

## 6.1 State Yönlendiricileri (ViewModel)

Android sayfalarında direkt I/O sorgusu yapılmaz. Repository'e giden tünel ViewModel'dir.

Örn: `HomeViewModel.kt`:
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SongRepository
) : ViewModel() {
    
    # Tüm şarkılar bir Stream (Akan Veri Borusu) gibi Flow ile okutuluyor:
    val songs: StateFlow<List<Song>> = repository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
```
`stateIn` burada hayat kurtarıcı bir coroutine aktarım metodu. Ekran kapanırsa (`onPause`), Android pil ömrü için dinlemeleri keser (Subscribed kuralı). Ekran açılınca devam eder. Listeye (Room db'ye) bir şarkı Drop (Düşerse), ekran `HomeScreen` hiç komut olmadan yenilenir! (UI Reactive).

## 6.2 Uygulama Gezinti Rotası (NavGraph)
`components` (Modülerleşitilmiş ufak UI parçaları, örneğin Müzik Çubuğu vb.) ve `screens` ayrılmıştır.
Rotasyon: 

Splash Screen:
                          


Home Screen:
                        















Upload Screen:
                        
















Song Details Screen:
                                
















Game Screen:
                           
















Result Screen:


                       
Geri Tuşunda (Back) hafızanın çöpmemesi için `popUpTo(Screen.Home.route)` kuralıyla `GameScreen` kapatıldığında `popBackStack` kullanılarak Activity Stack'den kazınır (Engine Çöpe Gider).

---








# BÖLÜM 7: MOBİL OYUN MOTORU: SURFACEVIEW, 60 FPS RENDER VE EXOPLAYER SİHİRLERİ

Standart Compose `Canvas`'i kullanarak saniyede 120 nota, 50 partikül çizmek imkansızdır (Thermal Throtting/Kasma Yapar). Proje `android/app/src/main/java/com/rhythmgame/game/` içinde bu sorunu donanımla ele alır.

## 7.1 Saf Java/Kotlin SurfaceView Mimarisi (`GameEngine.kt`)
`SurfaceView`, OS'nin Frame Buffer alanında (Ekran kartının RAM'i) kendi özel bir "Delik" (Hole/Window) açar ve o deliğe kendi Thread'indeki Canvas'ı dayar (Direct Pushing). 

  Render Thread / Infinite Game Loop (Oyun Döngüsü)  
```kotlin
// Threading & 60 FPS Controller
val targetFrameTimeMs = 16L  # 1000/60 = 16.6ms
val frameStart = System.currentTimeMillis()
// FİZİK ve KONUMLARI GÜNCELLE
updateGame() 
// GRAFİĞE ÇİZ
renderFrame() 
val elapsed = System.currentTimeMillis() - frameStart
val sleepTime = targetFrameTimeMs - elapsed
if (sleepTime > 0) delay(sleepTime) 
// Eğer çizimim sadece 3ms sürdüyse işlemcim 13ms YATSIN Kİ BATARYA bitmesin!
```
Bu sayede ekranınız kilitlenmeden, batarya harcanmadan "Akıcı (Butter-Smooth)" çalışır.

## 7.2 Notanın İniş Statiği ve Görecelik Denklemi (`NoteManager.kt`)
Bu projenin en muazzzam, en ileri düzey mühendislik başarısı piksellerin `y_axis += 5` şeklinde indirilmemesidir!
Bu yapı; "A Geçen Zaman = B Piksellik Uzaklık" felsefesiyle uzayzamanı büker.

```kotlin
// Y-Interpolation Formülü (Sadece zaman faktörlü)
val progress = (currentTimeMs - (noteTimeMs - approachTimeMs)).toFloat() / approachTimeMs.toFloat()
activeNote.y = progress   hitLineY
```
  Kanıt:  
  `hitLineY` = Ekranda dokunmanız gereken bölgenin pixel cinsinden yeri (Örn Y= 1600 piksel).
  `approachTimeMs` = Bir notanın ekranda belirdiği andan vurulacağı ana kadar geçen ön-izleme süresi (Örn 2000 ms, 2 saniye). 
  `noteTimeMs` = Bu notanın vurulmasının şart olduğu GERÇEK saniye (Örn Şarkının 10,000. saniyesi / 10sn)

1. Eğer şarkı `8000. milisaniyede` ise (Müzik Oynatıcı oraya geldi):
   Formül: (8000 - (10000 - 2000)) / 2000 => (8000 - 8000) / 2000 => 0/2000 => 0.0 Progress.
   Sonuç: `activeNote.y = 0   1600 = 0` (Nota Ekranın en tepesinde Doğar).
2. Eğer şarkı `9000. milisaniyede` ise:
   Formül: (9000 - 8000)/2000 => 1000/2000 => 0.5 Progress (Tam Yarısı).
   Sonuç: `activeNote.y = 0.5   1600 = 800` (Nota ekranın TAM ORTASINDADIR).
3. Eğer şarkı `10000. milisaniyede` ise:
   Formül: (10000 - 8000)/2000 => 2000/2000 => 1.0 Progress.
   Sonuç: `activeNote.y = 1.0   1600 = 1600` (TAM VURUŞ NOKTASININ ÜSTÜNDEDİR!!).

  Donma Anı Koruması (Lag Compansation):   Eğer arama gelirse ve telefonunuz 2 saniye takılırsa (Lag), ExoPlayer zamanı direk `10000.ms`'ye atacağından (Çünkü ses devam etmiştir), döngü (Loop) tekrar çalıştığında pikselller sırayla ilerlemez, bir anda progress 1.0 olacağı için nota vurulma çizgisine   IŞINLANIR (Teleporting)  . Oyununuz bir daha asla Ritim dışı (Desync) kalmaz!

## 7.3 Multi-Threaded Audio Krizi ve AtomicLong Barajı (`AudioSyncManager.kt`)
Siz `ExoPlayer.getCurrentPosition()` çağırdığınızda Android Sistemi, Ses yongasına ve IPC (Inter-Process Communication) C++ arabirimine giden ağır bir kilitli Thread sorgusu yapar (Blocking call).
Bunu GameEngine'in `updateGame()` içinde Loop içinde (saniyede 60 kere) ifşalarsanız (call), oyununuz titremeye (Stutter/Framedrop) başlar.
    Kriz Çözümü:   İki dev işlemcinin arasına "Atomic" bellek yazıcısı (Thread-Safe Buffer) koyalım!
```kotlin
// Main Thread'de çalışan Hayalet Runable
private val cachedPositionMs = AtomicLong(0L) // Atomic değişken asla KİLİT (Lock/Mutex) çıkarmaz!
private var positionUpdater = object : Runnable {
    override fun run() {
        if (player.isPlaying) {
            cachedPositionMs.set(player.currentPosition) // Sürekli Atomiği Gücelle! (YAZ)
        }
        mainHandler.postDelayed(this, 4) // Saniyede 250 kere güncelle
    }
}

// OYUN MOTORU (İşlemci 2): 
fun getCurrentPositionMs(): Long {
    return cachedPositionMs.get() + offsetMs  // Gram işlemcisi çalmadan ÇEK AL! (OKU)
}
```
Bu tasarım Mimari kalıcılık sağlar ve Game Engine ile Android Framework'ünü tamamen dekupaj(Decouple) eder.


---



# BÖLÜM 8: CHAQUOPY GEÇİŞİ: KENAR BİLİŞİM (EDGE COMPUTING) VE ÇEVRİMDIŞI İŞLEMCİ

Bilimsel BAP projenizin Temel vaadi, arka ucu tamamen yok edip; veritabanı (API) ücretlerini `0$`'a (SIFIR DOLAR) indirgemek. NPU (Neuromotor Processing Unit) ve ARM işlemcilerinde librosa çalıştırmak (Edge Computing).
Bunun yol rehberi kod bazlı olarak kanıtlanmalıdır:

## 8.1 Android Chaquopy Enstalasyonu
Uygulamanın Python tabanlı DSP kodlarına erişimi, `build.gradle.kts` içerisinde Native (C++) C köprüsü gerektirir. Android donanımınız (Telefon) Python'u görebilmesi için:
```kotlin
plugins {
    id("com.chaquo.python") // Chaquopy
}

chaquopy {
    defaultConfig {
        version = "3.10"
        buildPython("C:/Python310/python.exe")
        // Cihazın JNI tarafına tekerlek (wheel) kur:
        pip {
            install("numpy")
            install("librosa")
        }
    }
}

android {
    defaultConfig {
        ndk {
            // Native ARM mimari kilidi (Matematiksel Stabilite ve Performans için)
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }
}
```

## 8.2 Arka Uç (Python) Dosyalarının Aktarımı
`backend/app/analysis/` içerisindeki Python kodlarının uzayan işlemleri kırmak için Jetpack dizininden olan `app/src/main/python/` isimli   özel python kök klasörüne   taşınması gerekir!

## 8.3 Kotlin ile Python Sanal Makinesini Tetikleme (`SongRepository.kt` Güncellemesi)
Aşağıdaki kod parçası, projenin mevcut İstemci-Sunucu (Client-Server) bağımlılığını söküp atan, devrimsel ve kalıcı çözümdür. 

Eski API ile yükleyen bölüm KALKAR (Silinir):
```kotlin
// ARTIK BU BLOK KULLANILMAZ (DEPRECATED):
// val uploadResponse = api.uploadSong(body)
// val status = api.getStatus(songId)
```

  YENİ DEVRE (YEREL KENAR BİLİŞİM C++ PYTHON KÖPRÜSÜ)  
```kotlin
// GELECEKTEKİ UÇ BİLİŞİM (EDGE COMPUTE) YAPISI: 
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines. 
import org.json.JSONObject

@Singleton
class EdgeSongRepository @Inject constructor(
    private val songDao: SongDao,
    private val chartDao: ChartDao
) {
    suspend fun localAnalyzeSong(localFilePath: String, songId: String): Unit = withContext(Dispatchers.Default) {
        try {
            // DIKKAT: Dispatchers.Default, yoğun CPU harcayan matematik blokları içindir! (IO Degildir)
            
            // 1. Android işletim sistemi C/C++ üzerinde Python makinesi Boot-Up ediyor!
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            // 2. Makineden Instance (Örnek) al.
            val py = Python.getInstance()
            
            // 3. Bizim backend 'pipeline.py' dosyamızı çek!
            val pipelineModule = py.getModule("pipeline") 

            // 4. Parametreyi Yolla: Python'a 'Bu diskteki MP3" metodunu çalıştır diyelim 
            // (1-5 Saniye sürer, coroutine ile UI dondurmaz!)
            val jsonRawResponse = pipelineModule.callAttr("process_audio_file", localFilePath).toString()

            // 5. Python Tarafı her şeyi bitirdi, bize kocaman JSON bıraktı:
            val jsonObj = JSONObject(jsonRawResponse)
            val bpm = jsonObj.getDouble("bpm")
            val durationMs = jsonObj.getInt("duration_ms")
            
            // 6. JSON ayrıştırıcı ile Local Android Room Veritabanına Bas:
            val diffs = listOf("easy", "medium", "hard")
            for (diff in diffs) {
                 val contentJson = jsonObj.getJSONObject("charts").getString(diff)
                 chartDao.insertChart(ChartEntity(songId = songId, difficulty = diff, chartJson = contentJson))
            }
            
            // 7. İşlem Başarılı! 
            val rawSong = songDao.getSongById(songId)
            songDao.updateSong(rawSong.copy(status = "ready", bpm = bpm.toFloat(), durationMs = durationMs))

        } catch (e: Exception) {
            val rawSong = songDao.getSongById(songId)
            songDao.updateSong(rawSong.copy(status = "error", errorMessage = e.message))
        }
    }
}
```

---

# BÖLÜM 9: KLONLAMA, DERLEME VE SORUN GİDERME KORUMASI

  1. Projeyi GitHub'dan Klonlama ve Kurulum (Getting Started):  
Koda taze başlayacak bir mühendis için repo kurulumu komutları (Terminal/CMD):
```bash
# 1. Repoyu Yerel Bilgisayara İndir
$ git clone https://github.com/acemersoy/Ritim-Oyunu-.git
$ cd Ritim-Oyunu-

# 2. Android Klasörünü Aç ve SDK'ları Çek
# Android Studio'da 'android/app' klasörünü açın ve Gradle Sync butonuna basın. Veya terminalden:
$ cd android
$ ./gradlew assembleDebug

# 3. Backend (Python FastAPI) Kurulumu ve Başlatılması
$ cd ../backend
$ python -m venv venv
$ venv\Scripts\activate   # (Mac/Linux için: source venv/bin/activate)
$ pip install -r requirements.txt
$ uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
 Not: API çalışırken Postman veya Tarayıcı üzerinden `http://localhost:8000/docs` adresine giderek Swagger üzerinden test alabilirsiniz. 

  2. Veritabanı ve Migration (Taşıma) Hataları:  
Mobil Cihazda Room Schema güncellendiğinde Uygulama Crashliyorsa (Düşüyorsa): Cihaz Ayarlarından "Storage -> Clear Data" yapın. (Room destructiveMigration izin verilmediği için şema değişince kilitlenir).

  2. Görüntü (Render) Kasma Sorunu (Thermal Throtting)  
Aşırı partikül (Particle Effect) üretimi (Üst üste 20 Perfection Combo geldiğinde) ekran kasarsa; `GameEngine.kt` içinde particle Spawn limitlerini (Count) 25'ten 10'a sabit kod (Hardcode) çekebilirsiniz.

  3. Chaquopy Librosa Numba Error  
Python'da Librosa import edildiğinde arka tarafta "Numba" library'si JIT Compile deneyecektir. LLVM eksikliği hatalarında Android'e yönelik olan `pip install librosa --no-deps` veya spesifik ABI yapılandırmaları kurulmalıdır.

  4. Backend Geliştiricisi Startup Süreci:  
Test etmek veya Chaquopy'ye geçmeden backend'e hakim olmak için:
```bash
$ python -m venv venv
$ venv\Scripts\activate
$ pip install -r requirements.txt
$ uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
---

# BÖLÜM 10: API SÖZLEŞMESİ (REST ENDPOINTS & SWAGGER CONTRACT)

Projenin istemci (Android) ve sunucu (FastAPI) arasındaki iletişim kuralları aşağıdaki uç noktalarla (Endpoints) sabitlenmiştir. Yeni bir frontend yazılımcısı backend koduna girmeden bu tablodan beslenebilir.

| Metot | Uç Nokta (Endpoint) | Body / Params | Beklenen Cevap (JSON / Status) | Açıklama |
| :--- | :--- | :--- | :--- | :--- |
|   POST   | `/api/v1/songs/upload` | `multipart/form-data` (File: MP3) | `202 Accepted` <br> `{"song_id": "UUID", "status": "uploading"}` | Büyük medya dosyasını sunucu donanımına yazar ve Background Worker başlatır. |
|   GET   | `/api/v1/songs/{song_id}/status` | Path Param: `{song_id}` | `200 OK` <br> `{"status": "ready\|analyzing\|error"}` | Android'in 2 saniyede bir attığı Polling (Yoklama) isteğidir. DB okur, yük yapmaz. |
|   GET   | `/api/v1/songs/{song_id}/charts/{difficulty}` | Path Param: `{song_id}`, `{diff: String}` | `200 OK` <br> `{"song_id": "..", "difficulty": "..", "notes": [...]}` | "Ready" olduktan sonra cihazın Haritayı JSON (ChartContract) olarak toplu indirmesidir. |

---


# BÖLÜM 11: PROJE KLASÖR AĞACI (DIRECTORY STRUCTURE) VE İSKELET MİMARİSİ

Projenin modülleri "Separation of Concerns" (Sorumlulukların Ayrılığı) prensibiyle klasörlenmiştir:

```text
📦 Rhythm Game Project/
┣ 📂 android/app/src/main/
┃ ┣ 📂 java/com/rhythmgame/
┃ ┃ ┣ 📂 data/          # Room DB, Retrofit Clients ve Repositories
┃ ┃ ┣ 📂 di/            # Dagger-Hilt Singleton Kurulumları (AppModule.kt)
┃ ┃ ┣ 📂 game/          # ! OYUN MOTORU ! SurfaceView, ExoPlayer Sync, Y-Axis Matematiği
┃ ┃ ┣ 📂 ui/            # Jetpack Compose Ekranları
┃ ┃ ┃ ┣ 📂 components/  # Buttons, ProgressBar, Slider gibi ufak zerreler.
┃ ┃ ┃ ┗ 📂 screens/     # GameScreen, HomeScreen (NavHost Yönlendirmeleri)
┃ ┃ ┗ 📜 MainActivity.kt
┃ ┗ 📜 AndroidManifest.xml
┣ 📂 backend/
┃ ┣ 📂 app/
┃ ┃ ┣ 📂 api/           # REST Routerlar (routes.py)
┃ ┃ ┣ 📂 analysis/      # Librosa DSP Motoru (beat_tracker.py, difficulty.py)
┃ ┃ ┣ 📂 models/        # SQLAlchemy Veritabanı Modelleri.
┃ ┃ ┗ 📜 main.py        # Uvicorn ve FastAPI Çıkış Kapısı
┃ ┣ 📂 storage/         # /songs (MP3) ve SQLite Dosyaları
┃ ┗ 📜 requirements.txt
┗ 📜 rhythm_game_developer_guide_v3.md
```

---

# BÖLÜM 12: TASARIM DİLİ (DESIGN SYSTEM) VE NEON SİBERPUNK MİMARİSİ

Bir UI/UX eklentisi gerektiğinde kod tabanına gömülü olan "Design Token"lar kullanılmalıdır. Doğrudan `Color.Red` kullanımı YASAKTIR. Dosya: `Theme.kt` ve `Color.kt`

      Arka Plan (Zemin):   `Color(0xFF0A0A1A)` (Koyu Lacivert / Uzay Siyahı). OLED ekranlarda pil tasarrufu sağlar.
      Ana Renk (Primary):   `Color(0xFF00FFCC)` (Neon Camgöbeği/Turkuaz). Oyna butonları, mükemmel vurular ve seçimlerde vurgu yapar.
      İkincil Renk (Secondary):   `Color(0xFFFF007F)` (Neon Pembe/Macenta). Hata durumlarında, uyarı ve "MISS" (Kaçırdın) efektlerinde kullanılır.
      Font (Typography):   Google Fonts üzerinden asenkron yüklenen   "Exo 2"   ve   "Inter"  . Ritim panellerinde (High Score) daima sabit genişlikli (Monospace) Sayılar zorunludur ki skor 1000'den 9999'a çıkarken ekranda titreme (Layout Shift) yapmasın.
      Şerit Rengi (Lane Grid):   Yarı şeffaf Alpha(`0.15f`) çizgi beyazları.



# BÖLÜM 13: GÜVENLİK, EXCEPTION HANDLING (HATA YÖNETİMİ)

Sistemin devrilmesini önlemek için konan "Fail-Safe" kilit mekanizmalarının yönetimi:

1.    Corrupt MP3 Zafiyeti (Bozuk Sinyal):  
    Eğer kullanıcı şifrelenmiş veya bozuk bir medya ("fake.mp3") yüklerse, `librosa.load()` metodu "SoundFileError" atar. API "Internal Server Error" (500) verip kapatmaz. Arka plandaki `process_audio_task` istisna (Exception) yakalayarak SQLite tablosuna `song.status = "error"` yazar. Android'deki Polling'de cihaz `Ready` yerine `Error` JSON'u aldğı an yükleme ekranını (Spinning) kapatır, ekranda kırmızı bir "Dosya Okunamadı" uyarı çıkarır (Graceful Degradation).
2.    Veritabanı Okuma (I/O) Hatası:   Room'da disk doluluğu nedeniyle veri yazılamazsa Coroutine Context'ine takılan `catch` blokları Firebase Crashlytics üzerinden iz bırakmalıdır.

---

# 🚀 BÖLÜM 14: GELECEK VİZYONU VE BEKLENTİLER (PROJE MASTER PLANI)

Bu mimarinin inşasından sonra projenin önünde devasa, küresel bir ölçeklenme (Scaling) potansiyeli uzanmaktadır. Kurucu ekibin, yatırımcıların veya gelecek dev-team'in (Geliştirici takımı) önüne açık bir yol haritası çizilmiştir.

### FAZ 1: TAM ÖZERKLİK (CHAQUOPY ENTEGRASYONU) - [AĞIRLIK: KRİTİK]
Bulut barındırma maliyetlerini (AWS, Heroku) ve kullanıcıların internetini tüketme bağımlılığını yok eden   "Sunucusuz Kenar Bilişim" (Serverless Edge Computing)  .
      Beklenti:   Pyhton tabanlı Librosa ve Numpy analiz modülleri `backend/` klasöründen silinerek tamamen Native Android `com.chaquo.python` içerisine paketlenecektir.
      Sonuç:   Oyun   "Airplane Mode" (Uçak Modunda)   dahi çalışacak, dünyadaki milyarlarca kullanıcının cebindeki telefonun işlemcisi (ARM), devasa bir dağıtık analitik ağına (Distributed Computing Network) dönüşecektir. İş zekası (Business Model) maliyetini `Sıfıra` indirecektir.

### FAZ 2: VİZÜEL AŞIMI (SHADERS & OPENGL ES) - [AĞIRLIK: YÜKSEK]
SurfaceView harika bir 2D GPU hizlandırıcısıdır. Ancak oyun stüdyosu seviyesine geçmek için `Android Canvas` sınırlarının aşılması gerekir.
      Beklenti:   Oyun motoru (GameEngine), C++ NDK ve   OpenGL ES   (veya Vulkan) kütüphaneleriyle yeniden yazılacak. Notalara JNI (Java Native Interface) aracılığıyla "Glow", "Motion Blur" (Hareket İzi) ve 3 Boyutlu perspektif kazandırılacaktır (Notalar düz `y_axis+=x` aşağı yerine, Z ekseninden "Ekrana Yüzme" hissiyle gelebilir).

### FAZ 3: O2O (OFFLINE TO ONLINE) LİDERLİK TABLOSU - [AĞIRLIK: ORTA]
Kullanıcılar "Yerel High Score" yerine, Chaquopy algoritmasından çıkan JSON Haritanın SHA-256 Hash'ini (Dosya parmak izini) bir merkezi Firebase Sunucusuna atarak, X şarkısını oynayan dünyadaki diğer insanlarla rekabet edecektir.
      Beklenti:   Skor şifrelemesi (Anti-Cheat). Ritim oyunlarında skor hilelerini engellemek için "GameState" ve "ExoPlayer Cache" loglarının bir Seed (Tohum) üzerinden doğrulanması.

### FAZ 4: MÜZİKAL KAPSAYICILIK (OGG / WAV / MIDI DESTEĞİ)
Librosa zaten FFmpeg desteğine açık tasarlanmıştır. Yalnızca MP3 değil, Apple M4A, FLAC formatlarındaki sıkıştırılmamış ses formatları da oyuna yüklenecek ve algoritmanın "Sample Rate (Bant genişliği)" hassasiyeti sayesinde Vuruş Kılavuzu (Snap Grid) çok daha isabetli (99.8%) çalışacaktır.

---

# BÖLÜM 15: SİSTEM GÜVENLİĞİ VE TERSİNE MÜHENDİSLİK KORUMASI (SECURITY & OBFUSCATION)

Oyun sektöründe liderlik tablolarının ve premium müzik dosyalarının korunması kritiktir. 
      ProGuard / R8 Obfuscation:   Android APK çıktısı (Release) alınırken `minifyEnabled = true` yapılarak tüm sınıf isimleri (`GameEngine.kt` -> `a.b.c()`) şifrelenir. Bu, rakiplerin APK dosyanızı çözüp (Decompile) matematiksel (NoteManager) formüllerinizi kopyalamasını %99 oranında engeller.
      Scoped Storage (Kapsamlı Depolama):   Yüklenen `.mp3` ve Json harita verileri Android sisteminin ortak Müzik klasörüne   ASLA   çıkarılmaz. Sadece uygulamanın okuyabileceği `Context.filesDir` köküne (İzole Kutu) atılır. SQLCipher entegrasyonuyla gelecekte Room Database'i 256-bit AES ile şifrelenebilir.

# BÖLÜM 16: DERLEME ORTAMLARI (BUILD FLAVORS) VE VARYASYONLAR

Bir "Startup" ekibinde tek bir APK kullanılmaz. Geliştiriciler test yaparken gerçek kullanıcı verilerini bozmamalıdır.
`build.gradle.kts` içerisinde Product Flavors (Ürün Varyasyonları) yaratılır:

      `dev` (Geliştirme):   API İstekleri `http://192.168.1.x:8000` adresine (Lokal Backend) atılır. Ekranda Debug Logları ve FPS Sayacı her an görünür ("FPS: 59"). 
      `staging` (Test / Beta Sunucusu):   Kapalı Alpha test kullanıcıları içindir (Firebase App Distribution). API İstekleri `https://test-api.rhythmgame.com` adresine atılır.
      `prod` (Production / Google Play):   Nihai son kullanıcı ortamıdır. Tüm Loglar (`Timber.d`) susturulur (Performans için). Sunucu: `https://api.rhythmgame.com`.

# BÖLÜM 17: GİT MİMARİSİ VE TAKIM ÇALIŞMASI (GIT FLOW)

Koda yeni bir Android veya Python Geliştiricisi katıldığında uyması gereken Git Dallanma (Branching) kuralları:
1.    `main` Branch:   Sadece Production (Google Play'de yayında olan) hatasız kodu içerir. Doğrudan Commit atmak YASAKTIR.
2.    `develop` Branch:   Ekibin aktif geliştirme yaptığı ana dalıdır. Tüm yeni özellikler buradan türetilir.
3.    Feature Branch:   Yeni bir eklenti (Örn: Puzzle Modu) yazılacağında `feature/puzzle-mode` adında bir branch açılır. Kod bittikten sonra "Pull Request (PR)" ile `develop`'a birleştirilir. Kod, baş mimar tarafından okunmadan Onay (Merge) alamaz.

# BÖLÜM 18: TELEMETRİ, APM (APP PERFORMANCE MONITORING) VE UX İZLEME

Tasarımın veri ile test edilmesi gerekir (Data-Driven Design). 
      Firebase Crashlytics:   Oyun motoru (Thread) çökerse veya C++ NDK Kernel (Chaquopy) Panic (Hata) verip uygulamayı kapatırsa, hatanın kullanıcının hangi telefon modelinde (Örn: Samsung S21) ve hangi satırda olduğu anında ekibe anlık (Push) mail atılır.
      Kullanıcı Deneyimi İzleme (Analytics Events):   Oyuncu bir şarkıyı terk ettiğinde `song_abandoned` eventi fırlatılır. Eğer 10.000 kullanıcı "Medium" zorluğundaki `X` şarkısında ekranı terk ediyorsa, "O şarkı gereğinden fazla zor yapılmıştır" tespiti yapılıp librosa kalibrayonlarına (Backend) yama yapılır.

# BÖLÜM 19: DONANIM LİMİTASYONLARI VE CİHAZ MATRİSİ (HARDWARE TARGETS)

Ritim oyunları düşük bütçeli (RAM yoksunu) ekranlarda çalışamaz.
      Minimum CPU:   `arm64-v8a` mimarisi (Onaltılık sayı hesaplamaları nedeniyle 32-bit x86 çöpe atılır).
      Refresh Rate (Ekran Yenileme):   SurfaceView, ekranın HZ (Hertz) değerini `Display.Mode` ile tarar. Akıllı telefon 60 Hz ise, GameLoop gecikmesi 16.6ms; 120 Hz eSpor oyuncu telefonu ise GameLoop gecikmesi `8.3ms` olarak kendini otomatik "Down-Sync" eder (Dinamik Frame Pacing).

---

# BÖLÜM 20: KODLAMA STANDARTLARI VE STİL REHBERİ (CODE CONVENTIONS)

Dağınık kodları önlemek için ekibe katılacak yeni mühendislerin IDE'lerinde (Android Studio / VS Code) mutlaka şu kurallar aktif edilmelidir:
      Kotlin (Android):   `ktlint` veya `detekt` statik analiz araçları kullanılacaktır. Sınıf isimleri `PascalCase`, değişkenler `camelCase`, sabitler (Constants) `UPPER_SNAKE_CASE` zorunluluğundadır. Interface (Arayüz) sınıfları "I" harfi ile başlamaz (ör: `ISongRepo` yerine `SongRepository` kullanılır, Base `Impl` ile ayrılır).
      Python (Backend):   `black` formatter ve `flake8` aracı ile PEP-8 uyumluluğu zorunludur. Tüm fonksiyonlar, Type Hinting (`-> dict`) kullanılarak belgelendirilmek zorundadır.

# BÖLÜM 21: YERELLEŞTİRME (L10N), KÜRESELLEŞME (I18N) VE ERİŞİLEBİLİRLİK

Ritim oyunu global olarak yayınlanacağı için "Hardcoded" (string "Oyna") terimleri yasaktır.
      Dil Desteği (I18N):   `res/values/strings.xml` altyapısı mecburi tutulmuştur. İleride oyun Çin veya Japonya pazarına açıldığında `strings-zh.xml` ile 1 tıkla tüm UI dönebilir.
      Erişilebilirlik (A11Y - Accessibility):   Ritim oyununda menülerde gezinirken renk körleri (Color Blind) için Contrast-Ratio (Zıtlık Oranı) Jetpack Compose `MaterialTheme.colors` tarafından izlenir. Müzikal zorluklarda (Easy/Hard), butonların üzerindeki metin boyutları `.sp (Scaled Pixels)` ile yazıldığından, kullanıcının cihazındaki font büyüklüğüne göre dinamik esner.

# BÖLÜM 22: MONETİZASYON (GELİR MODELİ) VE IN-APP PURCHASES (IAP) MİMARİSİ

Uygulamanın gelecekte donanım ve geliştirme masraflarını finanse edeceği mimari:
      Premium Müzik Paketleri:   Google Play Billing Service v6.0 entegrasyonu ile kilitli şarkılar sunuma açılacaktır. SQLite (Room) `SongRecord` entitesine `isUnlocked: Boolean = false` kolonu getirilerek, arka ucun attığı bir "Token" yardımıyla kilit kalkar.
      Non-Intrusive Ads (Reklamlar):   Oyuncunun ritim hissini (Akışı/Flow) bozmamak için oyun içi geçişlerde asla pop-up reklam çıkartılmaz. Ana menüde sessiz bir AdMob Banner veya ResultScreen'de "Skoru İkiye Katla" modelli ödüllendirilmiş (Rewarded Video) izleme mimarileri planlanmıştır.

# BÖLÜM 23: VERİ GİZLİLİĞİ, GDPR VE KVKK UYUMLULUĞU

Uygulamanın mağazalardan (Apple Store / Play Store) banlanmaması (Red yememesi) için kritik kurallar:
      Cihaz Dosya Okuması (Scoped Storage):   Uygulama tüm fotoğraflara/medyaya erişmez! Yalnızca SAF (Storage Access Framework) ile kullanıcının KENDİSİNİN seçtiği hedef dosyaya (Megalovatia.mp3) read-only (Salt Okunur) dokunuruz.
      Telemetri (Crashlytics):   Çökmeler (Crash) gönderilirken Firebase'e kullanıcının IP adresi veya Mac Adresi maskelenmektedir (Anonymization). Herhangi PII (Personal Identifiable Information) sunucularımızda saklanmaz.

---

# BÖLÜM 24: MERKEZİ OYUN MEKANİKLERİ VE OYUN FİZİĞİ MATEMATİĞİ

Sıradan bir uygulamanın "Arayüzünden" farklı olan, `GameEngine.kt` içinde 60 FPS (Saniyede 60 Kare) ile renderlanan ve milyarlarca oyuncuya "Ritim Hissi" sağlayan 4 büyük oyunlaştırma mekaniği şunlardır. (Aşağıdaki tüm kodlar ve mantık süreçleri projenin teknik belkemiğidir):

### 24.1 İsabet Pencereleri (Hit Windows) ve Puanlama Matematiği
Bir ritim oyununda oyuncunun tam notaya bastığı an (`ACTION_DOWN`) ile müziğin aslında o notayı çalması gereken an (`targetTimeMs`) arasındaki zaman farkına   Delta (Hata Payı)   denir. Motorumuz bu farkı `Mutlak Değer (Abs)` üzerinden Puan ve Combo çarpanına dönüştürür.

```kotlin
// Dokunmatik ekrana basıldığında (Touch Event)
fun onLaneTapped(laneIndex: Int, tapTimeMs: Long) {
    // İlgili şeritte (lane) o an en altta bekleyen notayı bul
    val targetNote = activeNotes.firstOrNull { it.lane == laneIndex } ?: return
    
    // DELTA HESABI (Fark)
    val deltaMs = Math.abs(targetNote.noteTimeMs - tapTimeMs)

    when {
        deltaMs <= 50L -> {
            // PERFECT (KUSURSUZ)
            score += 100   comboMultiplier
            combo++
            spawnParticles(targetNote.x, hitLineY, "CYAN")
        }
        deltaMs <= 110L -> {
            // GREAT (İYİ) - Gecikmeli ama kabul edilebilir
            score += 50   comboMultiplier
            combo++
        }
        deltaMs <= 180L -> {
            // OK/BAD - Zor kurtarma
            score += 10
            combo = 1 // Combo Kırıldı!
        }
        else -> {
            // MISS (KAÇIRILDI) - Çok erken basıldı!
            combo = 0
            health -= 5.0f // Can (HP) barı düşer
        }
    }
    
    // Nota Vuruldu, Ekranda Göstermeyi Bırak
    targetNote.isHit = true 
}
```

    Game Over Sensörü:   Ekranın altından vurulmadan kaçıp giden notalara ne olur? GameLoop motoru `if (currentTime > targetNote.noteTimeMs + 200)` diyerek 200ms geride kalmış bir notayı otomatik patlatır, Can Puanını (HP) düşürür ve `MISS` statüsü yazar.

### 24.2 Çöp Toplayıcı Koruması (Object Pooling) ve RAM Yönetimi
3 dakikalık "Hard" zorlukta bir şarkıda ortalama   1800 adet nota   üretilir. Android Java/Kotlin Heap (RAM) belleğinde ritim akarken saniyede 15 kere `val newNote = Note(...)` sınıfı (Memory Allocation) yaratmak ve silmek, Android "Garbage Collector"ın (GC) sürekli devreye girip o çöpleri silmek için   oyunu 10'ar milisaniye dondurmasına (Stutter/Lag)   neden olur. Ritim oyunlarında müzik akarken ekran "Titremez"; eğer titriyorsa tasarım amatorcedir.

      Mimari Çözüm (Object Pool Pattern):   Oyun başladığında ArrayList içerisine sadece   100 adet hayalet (Dummy) nota objesi   tek seferde yaratılır (`List<Note>`). 
      Geri Dönüşüm (Recycle):   Ekranın altına düşen veya vurulan bir nota bellekten silinmez (`delete` edilmez)! Sadece `isVisible = false` yapılır. Ekranın en tepesinden (Müzik aktıkça) yeni bir nota düşmesi gerektiğinde; havuzdaki ilk karanlık nota çekilir, yeni `targetTimeMs` verilir ve ekrana salınır. 
      Sonuç:   Oyun 3 dakika değil 300 saat bile sürse Android Cihazı SADECE 100 Adet objeyle Memory-Leak (RAM sızıntısı) olmadan çalışır.

### 24.3 Bluetooth / Ses Kalibrasyonu (Audio Calibration Offset)
Dünyadaki en zorlu GameUX (Deneyim) problemlerinden biri   Bluetooth Gecikmesidir  . Oyuncu kablosuz kulaklık kullandığında (Örn: AirPods), telefon işlemcisinin sesi kulaklık donanımına yollaması Bluetooth protokolü sebebiyle ~150 ile ~400ms arasında (Latency) gecikir. 
      Kriz:   Ekrandaki nota Tam Hit-Line çizgisine değer, oyuncu vurur (PERFECT alır), AMA kulaklıktaki davul sesi yarım saniye sonra (Dan!) diye gelir. Bu müzisyeni çıldırtır.
      Motor Düzeltmesi (`AudioSyncManager`):   `SettingsScreen` üzerinden kullanıcıya `-500ms` ile `+500ms` arası bir "Kalibrasyon Testi" (Metronom Sürgüsü) verilir.
```kotlin
// Oyuncunun Kulaklık Gecikme Kalibrasyonu (Örn: -200ms)
val userCalibrationMs = sharedPrefs.getLong("audio_offset", 0L)

// Oyun Saatimiz (GameClock) Gerçek Ses Çalar (Exo) saatinden Manipüle Edilir
fun getCurrentGameTime(): Long {
    val realAudioTime = ExoPlayer.currentPosition
    // Ses geriden geliyorsa, motor saatimiz BAŞTAN GECİKTİRİLİR!
    return realAudioTime + userCalibrationMs 
}
```
Böylece oyun motoru sesin gecikmesini algılayıp grafikleri bükerek pikselleri Bluetooth kulaklığın ses verdiği milisaniyeye senkronize eder.

### 24.4 Müzikal Partikül Fiziği (Particle Emitters)
Notaya `PERFECT` basıldığında PNG patlama görseli konmaz; yüksek zevk (Juiciness/Game Feel) uyandırmak için 2D Frame Buffer'a vektörel (C++) pikseller saçılır.
Modüler bir Parçacık Döngüsü (UpdateGame loop):
1.    Patlama (Explosion):   Tek notadan rastgele (Random X/Y velocity ile) `30-40 adet` küçük daire/kıvılcım yaratılır.
2.    Fizik Hareketi:   Her Frame'de (Saniyede 60 kez):
        `particle.x += speedX` (Partikül saçağa fırlar)
        `particle.y += speedY` (Yerçekimi uygulanabilir)
        `particle.alpha -= 0.05f` (Partikül gitgide görünmez olur).
3.    Yok Oluş:   Saydamlık (Alpha) `0.0f` olduğunda partikül ParticlePool (Havuz) içine geri iade edilir. Bu sayede görsel şölen ekran kartını (GPU) veya (CPU) boğmadan 60 FPS'de pürüzsüzce gerçekleşir.

# BÖLÜM 25: OYUN DURUMU (GAME STATE) MİMARİSİ VE SONLU DURUM MAKİNESİ (FSM)

Oyun motorunda `GameEngine.kt` ekranında gerçekleşen her olay (Oynanıyor, Duraklatıldı, Oyun Bitti) bir   Finite State Machine (FSM)   ile yönetilir. Eğer State Machine (Durum Makinesi) kullanılmazsa motor bug'a girer (Örn: Oyun bitmişken ekrana hala nota düşmeye devam eder).

### 25.1 Sealed Class ile State Koruması
Kotlin'in `sealed class` yapısı kullanılarak durumlar matematiksel olarak kilitlenir:
```kotlin
sealed class GameState {
    object Loading : GameState()       // Şarkı RAM'e çekilirken
    object Playing : GameState()       // Notalar Akarken (60 FPS)
    object Paused : GameState()        // Back tuşuna veya menüye basılınca
    data class GameOver(val reason: String) : GameState() // Can barı bitince (HP = 0)
    data class Victory(val score: Int) : GameState()      // Müzik hatasız (HP > 0) bittiğinde
}
```

### 25.2 State Flow Diagramı:

# BÖLÜM 26: DEVOPS VE CI/CD OTOMASYON REHBERİ (GITHUB ACTIONS)

Kurumsal firmalarda bir yazılımcı kendi bilgisayarında (`localhost`) kod derleyip Google Play'e atmaz. "Bende çalışıyor" mazeretini yok etmek için projenin kök dizininde `.github/workflows/android.yml` dosyası yapılandırılmıştır.

      # Eğer testler geçerse, otomatik Firebase App Distribution veya Google Play'e atılır.
```
Bu sistem koda hatalı/kötü (Buggy) bir özellik eklendiğinde derlemeyi (Build) iptal eder ve `main` branch güvenliğini sağlar.

# BÖLÜM 27: BELLEK PROFİLİ (MEMORY PROFILING) VE PERFORMANS BÜTÇELERİ

Bu uygulama eski (Low-end) cihazlarda çalışırken çökmemesi için belli bir MegaByte (MB) bütçesiyle sınırlandırılmıştır. Dalvik/ART (Android RunTime) sınırlamaları:

      Audio Buffer (ExoPlayer):   ~15 MB
      Object Pool (100 Nota x 4 KB):   ~400 KB (Oyun döngüsü bedavadır).
      SurfaceView FrameBuffer (1080x2400 çözünürlük x 32-bit renk):   ~10 MB
      Python Chaquopy VM (Numpy ve Librosa Loaded):   ~80 MB RAM işgal eder.
      Toplam Maximum Bütçe:   ~105 MB.
Bir cihaz 2 GB RAM'e sahip olsa bile oyun "Memory Exhausted (OOM)" hatasına düşmeden akıcı şekilde oynatır. Yalnızca Bitmap resimleri (Örn: Neon Arka Planlar) yüklenirken belleği boğmaması için Glide veya Coil gibi "Resampling / Downsampling" kütüphaneleriyle 1/4 çözünürlüğe ölçeklenip RAM'e çıkarılır.

# BÖLÜM 28: AÇIK KAYNAK LİSANSLARI VE HUKUKİ (LEGAL) UYUMLULUK

Google Play veya Apple App Store algoritmasının otomatik telif atmasını önlemek için uygulamanın "Açık Kaynak Kod Bildirimi" yapması mecburidir. `SettingsScreen` (Ayarlar) içine `Open Source Licenses` butonu konulmalı ve şu paketler deklare edilmelidir:
1.    Librosa (Python):   ISC License
      `prod` (Production / Google Play):   Nihai son kullanıcı ortamıdır. Tüm Loglar (`Timber.d`) susturulur (Performans için). Sunucu: `https://api.rhythmgame.com`.

# BÖLÜM 17: GİT MİMARİSİ VE TAKIM ÇALIŞMASI (GIT FLOW)

Koda yeni bir Android veya Python Geliştiricisi katıldığında uyması gereken Git Dallanma (Branching) kuralları:
1.    `main` Branch:   Sadece Production (Google Play'de yayında olan) hatasız kodu içerir. Doğrudan Commit atmak YASAKTIR.
2.    `develop` Branch:   Ekibin aktif geliştirme yaptığı ana dalıdır. Tüm yeni özellikler buradan türetilir.
3.    Feature Branch:   Yeni bir eklenti (Örn: Puzzle Modu) yazılacağında `feature/puzzle-mode` adında bir branch açılır. Kod bittikten sonra "Pull Request (PR)" ile `develop`'a birleştirilir. Kod, baş mimar tarafından okunmadan Onay (Merge) alamaz.

# BÖLÜM 18: TELEMETRİ, APM (APP PERFORMANCE MONITORING) VE UX İZLEME

Tasarımın veri ile test edilmesi gerekir (Data-Driven Design). 
      Firebase Crashlytics:   Oyun motoru (Thread) çökerse veya C++ NDK Kernel (Chaquopy) Panic (Hata) verip uygulamayı kapatırsa, hatanın kullanıcının hangi telefon modelinde (Örn: Samsung S21) ve hangi satırda olduğu anında ekibe anlık (Push) mail atılır.
      Kullanıcı Deneyimi İzleme (Analytics Events):   Oyuncu bir şarkıyı terk ettiğinde `song_abandoned` eventi fırlatılır. Eğer 10.000 kullanıcı "Medium" zorluğundaki `X` şarkısında ekranı terk ediyorsa, "O şarkı gereğinden fazla zor yapılmıştır" tespiti yapılıp librosa kalibrayonlarına (Backend) yama yapılır.

# BÖLÜM 19: DONANIM LİMİTASYONLARI VE CİHAZ MATRİSİ (HARDWARE TARGETS)

Ritim oyunları düşük bütçeli (RAM yoksunu) ekranlarda çalışamaz.
      Minimum CPU:   `arm64-v8a` mimarisi (Onaltılık sayı hesaplamaları nedeniyle 32-bit x86 çöpe atılır).
      Refresh Rate (Ekran Yenileme):   SurfaceView, ekranın HZ (Hertz) değerini `Display.Mode` ile tarar. Akıllı telefon 60 Hz ise, GameLoop gecikmesi 16.6ms; 120 Hz eSpor oyuncu telefonu ise GameLoop gecikmesi `8.3ms` olarak kendini otomatik "Down-Sync" eder (Dinamik Frame Pacing).

---

# BÖLÜM 20: KODLAMA STANDARTLARI VE STİL REHBERİ (CODE CONVENTIONS)

Dağınık kodları önlemek için ekibe katılacak yeni mühendislerin IDE'lerinde (Android Studio / VS Code) mutlaka şu kurallar aktif edilmelidir:
      Kotlin (Android):   `ktlint` veya `detekt` statik analiz araçları kullanılacaktır. Sınıf isimleri `PascalCase`, değişkenler `camelCase`, sabitler (Constants) `UPPER_SNAKE_CASE` zorunluluğundadır. Interface (Arayüz) sınıfları "I" harfi ile başlamaz (ör: `ISongRepo` yerine `SongRepository` kullanılır, Base `Impl` ile ayrılır).
      Python (Backend):   `black` formatter ve `flake8` aracı ile PEP-8 uyumluluğu zorunludur. Tüm fonksiyonlar, Type Hinting (`-> dict`) kullanılarak belgelendirilmek zorundadır.

# BÖLÜM 21: YERELLEŞTİRME (L10N), KÜRESELLEŞME (I18N) VE ERİŞİLEBİLİRLİK

Ritim oyunu global olarak yayınlanacağı için "Hardcoded" (string "Oyna") terimleri yasaktır.
      Dil Desteği (I18N):   `res/values/strings.xml` altyapısı mecburi tutulmuştur. İleride oyun Çin veya Japonya pazarına açıldığında `strings-zh.xml` ile 1 tıkla tüm UI dönebilir.
      Erişilebilirlik (A11Y - Accessibility):   Ritim oyununda menülerde gezinirken renk körleri (Color Blind) için Contrast-Ratio (Zıtlık Oranı) Jetpack Compose `MaterialTheme.colors` tarafından izlenir. Müzikal zorluklarda (Easy/Hard), butonların üzerindeki metin boyutları `.sp (Scaled Pixels)` ile yazıldığından, kullanıcının cihazındaki font büyüklüğüne göre dinamik esner.

# BÖLÜM 22: MONETİZASYON (GELİR MODELİ) VE IN-APP PURCHASES (IAP) MİMARİSİ

Uygulamanın gelecekte donanım ve geliştirme masraflarını finanse edeceği mimari:
      Premium Müzik Paketleri:   Google Play Billing Service v6.0 entegrasyonu ile kilitli şarkılar sunuma açılacaktır. SQLite (Room) `SongRecord` entitesine `isUnlocked: Boolean = false` kolonu getirilerek, arka ucun attığı bir "Token" yardımıyla kilit kalkar.
      Non-Intrusive Ads (Reklamlar):   Oyuncunun ritim hissini (Akışı/Flow) bozmamak için oyun içi geçişlerde asla pop-up reklam çıkartılmaz. Ana menüde sessiz bir AdMob Banner veya ResultScreen'de "Skoru İkiye Katla" modelli ödüllendirilmiş (Rewarded Video) izleme mimarileri planlanmıştır.

# BÖLÜM 23: VERİ GİZLİLİĞİ, GDPR VE KVKK UYUMLULUĞU

Uygulamanın mağazalardan (Apple Store / Play Store) banlanmaması (Red yememesi) için kritik kurallar:
      Cihaz Dosya Okuması (Scoped Storage):   Uygulama tüm fotoğraflara/medyaya erişmez! Yalnızca SAF (Storage Access Framework) ile kullanıcının KENDİSİNİN seçtiği hedef dosyaya (Megalovatia.mp3) read-only (Salt Okunur) dokunuruz.
      Telemetri (Crashlytics):   Çökmeler (Crash) gönderilirken Firebase'e kullanıcının IP adresi veya Mac Adresi maskelenmektedir (Anonymization). Herhangi PII (Personal Identifiable Information) sunucularımızda saklanmaz.

---

# BÖLÜM 24: MERKEZİ OYUN MEKANİKLERİ VE OYUN FİZİĞİ MATEMATİĞİ

Sıradan bir uygulamanın "Arayüzünden" farklı olan, `GameEngine.kt` içinde 60 FPS (Saniyede 60 Kare) ile renderlanan ve milyarlarca oyuncuya "Ritim Hissi" sağlayan 4 büyük oyunlaştırma mekaniği şunlardır. (Aşağıdaki tüm kodlar ve mantık süreçleri projenin teknik belkemiğidir):

### 24.1 İsabet Pencereleri (Hit Windows) ve Puanlama Matematiği
Bir ritim oyununda oyuncunun tam notaya bastığı an (`ACTION_DOWN`) ile müziğin aslında o notayı çalması gereken an (`targetTimeMs`) arasındaki zaman farkına   Delta (Hata Payı)   denir. Motorumuz bu farkı `Mutlak Değer (Abs)` üzerinden Puan ve Combo çarpanına dönüştürür.

[BURAYA "İSABET PENCERELERİ VE PUANLAMA MANTIĞI DİYAGRAMI" GELECEK]

```kotlin
// Dokunmatik ekrana basıldığında (Touch Event)
fun onLaneTapped(laneIndex: Int, tapTimeMs: Long) {
    // İlgili şeritte (lane) o an en altta bekleyen notayı bul
    val targetNote = activeNotes.firstOrNull { it.lane == laneIndex } ?: return
    
    // DELTA HESABI (Fark)
    val deltaMs = Math.abs(targetNote.noteTimeMs - tapTimeMs)

    when {
        deltaMs <= 50L -> {
            // PERFECT (KUSURSUZ)
            score += 100   comboMultiplier
            combo++
            spawnParticles(targetNote.x, hitLineY, "CYAN")
        }
        deltaMs <= 110L -> {
            // GREAT (İYİ) - Gecikmeli ama kabul edilebilir
            score += 50   comboMultiplier
            combo++
        }
        deltaMs <= 180L -> {
            // OK/BAD - Zor kurtarma
            score += 10
            combo = 1 // Combo Kırıldı!
        }
        else -> {
            // MISS (KAÇIRILDI) - Çok erken basıldı!
            combo = 0
            health -= 5.0f // Can (HP) barı düşer
        }
    }
    
    // Nota Vuruldu, Ekranda Göstermeyi Bırak
    targetNote.isHit = true 
}
```

    Game Over Sensörü:   Ekranın altından vurulmadan kaçıp giden notalara ne olur? GameLoop motoru `if (currentTime > targetNote.noteTimeMs + 200)` diyerek 200ms geride kalmış bir notayı otomatik patlatır, Can Puanını (HP) düşürür ve `MISS` statüsü yazar.

### 24.2 Çöp Toplayıcı Koruması (Object Pooling) ve RAM Yönetimi
3 dakikalık "Hard" zorlukta bir şarkıda ortalama   1800 adet nota   üretilir. Android Java/Kotlin Heap (RAM) belleğinde ritim akarken saniyede 15 kere `val newNote = Note(...)` sınıfı (Memory Allocation) yaratmak ve silmek, Android "Garbage Collector"ın (GC) sürekli devreye girip o çöpleri silmek için   oyunu 10'ar milisaniye dondurmasına (Stutter/Lag)   neden olur. Ritim oyunlarında müzik akarken ekran "Titremez"; eğer titriyorsa tasarım amatorcedir.

      Mimari Çözüm (Object Pool Pattern):  


    Oyun başladığında ArrayList içerisine sadece   100 adet hayalet (Dummy) nota objesi   tek seferde yaratılır (`List<Note>`). 
      Geri Dönüşüm (Recycle):   Ekranın altına düşen veya vurulan bir nota bellekten silinmez (`delete` edilmez)! Sadece `isVisible = false` yapılır. Ekranın en tepesinden (Müzik aktıkça) yeni bir nota düşmesi gerektiğinde; havuzdaki ilk karanlık nota çekilir, yeni `targetTimeMs` verilir ve ekrana salınır. 
      Sonuç:   Oyun 3 dakika değil 300 saat bile sürse Android Cihazı SADECE 100 Adet objeyle Memory-Leak (RAM sızıntısı) olmadan çalışır.

### 24.3 Bluetooth / Ses Kalibrasyonu (Audio Calibration Offset)
Dünyadaki en zorlu GameUX (Deneyim) problemlerinden biri   Bluetooth Gecikmesidir  . Oyuncu kablosuz kulaklık kullandığında (Örn: AirPods), telefon işlemcisinin sesi kulaklık donanımına yollaması Bluetooth protokolü sebebiyle ~150 ile ~400ms arasında (Latency) gecikir. 
      Kriz:   Ekrandaki nota Tam Hit-Line çizgisine değer, oyuncu vurur (PERFECT alır), AMA kulaklıktaki davul sesi yarım saniye sonra (Dan!) diye gelir. Bu müzisyeni çıldırtır.
      Motor Düzeltmesi (`AudioSyncManager`):   `SettingsScreen` üzerinden kullanıcıya `-500ms` ile `+500ms` arası bir "Kalibrasyon Testi" (Metronom Sürgüsü) verilir.
```kotlin
// Oyuncunun Kulaklık Gecikme Kalibrasyonu (Örn: -200ms)
val userCalibrationMs = sharedPrefs.getLong("audio_offset", 0L)

// Oyun Saatimiz (GameClock) Gerçek Ses Çalar (Exo) saatinden Manipüle Edilir
fun getCurrentGameTime(): Long {
    val realAudioTime = ExoPlayer.currentPosition
    // Ses geriden geliyorsa, motor saatimiz BAŞTAN GECİKTİRİLİR!
    return realAudioTime + userCalibrationMs 
}
```
Böylece oyun motoru sesin gecikmesini algılayıp grafikleri bükerek pikselleri Bluetooth kulaklığın ses verdiği milisaniyeye senkronize eder.

### 24.4 Müzikal Partikül Fiziği (Particle Emitters)
Notaya `PERFECT` basıldığında PNG patlama görseli konmaz; yüksek zevk (Juiciness/Game Feel) uyandırmak için 2D Frame Buffer'a vektörel (C++) pikseller saçılır.
Modüler bir Parçacık Döngüsü (UpdateGame loop):
1.    Patlama (Explosion):   Tek notadan rastgele (Random X/Y velocity ile) `30-40 adet` küçük daire/kıvılcım yaratılır.
2.    Fizik Hareketi:   Her Frame'de (Saniyede 60 kez):
        `particle.x += speedX` (Partikül saçağa fırlar)
        `particle.y += speedY` (Yerçekimi uygulanabilir)
        `particle.alpha -= 0.05f` (Partikül gitgide görünmez olur).
3.    Yok Oluş:   Saydamlık (Alpha) `0.0f` olduğunda partikül ParticlePool (Havuz) içine geri iade edilir. Bu sayede görsel şölen ekran kartını (GPU) veya (CPU) boğmadan 60 FPS'de pürüzsüzce gerçekleşir.

# BÖLÜM 25: OYUN DURUMU (GAME STATE) MİMARİSİ VE SONLU DURUM MAKİNESİ (FSM)

Oyun motorunda `GameEngine.kt` ekranında gerçekleşen her olay (Oynanıyor, Duraklatıldı, Oyun Bitti) bir   Finite State Machine (FSM)   ile yönetilir. Eğer State Machine (Durum Makinesi) kullanılmazsa motor bug'a girer (Örn: Oyun bitmişken ekrana hala nota düşmeye devam eder).

### 25.1 Sealed Class ile State Koruması


Kotlin'in `sealed class` yapısı kullanılarak durumlar matematiksel olarak kilitlenir:
```kotlin
sealed class GameState {
    object Loading : GameState()       // Şarkı RAM'e çekilirken
    object Playing : GameState()       // Notalar Akarken (60 FPS)
    object Paused : GameState()        // Back tuşuna veya menüye basılınca
    data class GameOver(val reason: String) : GameState() // Can barı bitince (HP = 0)
    data class Victory(val score: Int) : GameState()      // Müzik hatasız (HP > 0) bittiğinde
}
```

### 25.2 State Flow Diagramı:
1. `Loading` -> Yükleme biter -> `Playing`
2. `Playing` -> Can Sıfırlanır -> `GameOver`
3. `Playing` -> ExoPlayer "STATE_ENDED" fırlatır -> `Victory`
4. `Playing` -> Telefon Aranır / Uygulama Alta alınır -> `Paused` -> ExoPlayer.pause()

# BÖLÜM 26: DEVOPS VE CI/CD OTOMASYON REHBERİ (GITHUB ACTIONS)

Kurumsal firmalarda bir yazılımcı kendi bilgisayarında (`localhost`) kod derleyip Google Play'e atmaz. "Bende çalışıyor" mazeretini yok etmek için projenin kök dizininde `.github/workflows/android.yml` dosyası yapılandırılmıştır.

```yaml
# Kaba Otomasyon Taslağı
name: Android CI/CD Pipeline
on:
  push:
    branches: [ "main", "develop" ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
    - name: Run JUnit & Espresso Tests
      run: ./gradlew test connectedAndroidTest
    - name: Build Signed APK
      run: ./gradlew assembleRelease
      # Eğer testler geçerse, otomatik Firebase App Distribution veya Google Play'e atılır.
```
Bu sistem koda hatalı/kötü (Buggy) bir özellik eklendiğinde derlemeyi (Build) iptal eder ve `main` branch güvenliğini sağlar.

# BÖLÜM 27: BELLEK PROFİLİ (MEMORY PROFILING) VE PERFORMANS BÜTÇELERİ

Bu uygulama eski (Low-end) cihazlarda çalışırken çökmemesi için belli bir MegaByte (MB) bütçesiyle sınırlandırılmıştır. Dalvik/ART (Android RunTime) sınırlamaları:

      Audio Buffer (ExoPlayer):   ~15 MB
      Object Pool (100 Nota x 4 KB):   ~400 KB (Oyun döngüsü bedavadır).
      SurfaceView FrameBuffer (1080x2400 çözünürlük x 32-bit renk):   ~10 MB
      Python Chaquopy VM (Numpy ve Librosa Loaded):   ~80 MB RAM işgal eder.
      Toplam Maximum Bütçe:   ~105 MB.
Bir cihaz 2 GB RAM'e sahip olsa bile oyun "Memory Exhausted (OOM)" hatasına düşmeden akıcı şekilde oynatır. Yalnızca Bitmap resimleri (Örn: Neon Arka Planlar) yüklenirken belleği boğmaması için Glide veya Coil gibi "Resampling / Downsampling" kütüphaneleriyle 1/4 çözünürlüğe ölçeklenip RAM'e çıkarılır.

# BÖLÜM 28: AÇIK KAYNAK LİSANSLARI VE HUKUKİ (LEGAL) UYUMLULUK

Google Play veya Apple App Store algoritmasının otomatik telif atmasını önlemek için uygulamanın "Açık Kaynak Kod Bildirimi" yapması mecburidir. `SettingsScreen` (Ayarlar) içine `Open Source Licenses` butonu konulmalı ve şu paketler deklare edilmelidir:
1.    Librosa (Python):   ISC License
2.    NumPy / SciPy:   BSD-3-Clause License
3.    FastAPI / Pydantic:   MIT License
4.    Google ExoPlayer (Media3):   Apache License 2.0
5.    Chaquopy:   GNU GPL v3 veya Ticari Lisans (Chaquopy ticari projeler için lisans isteyebilir, projenin büyümesi anında $ lisanslamaya geçilmelidir).

---

# BÖLÜM 29: SUNUCU GÜVENLİĞİ VE API İSTEK SINIRLANDIRMALARI (RATE LIMITING & JWT)

Uygulamanın Python FastAPI sunucusu dünyaya açıldığında, kötü niyetli (DDoS) botların saniyede 10.000 MP3 dosyası atıp sunucuyu kilitlemesini ve bulut (Cloud) faturasını şişirmesini engellemek için güvenlik mimarimiz:

### 29.1 Rate Limiting (İstek Sınırlandırma)
Redis (veya Memory tabanlı) `slowapi` kütüphanesi kullanılarak her IP adresine `/upload` rotası için kota konmuştur.
```python
from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)

@router.post("/songs/upload")
@limiter.limit("5/minute") # Bir kullanıcı dakikada en fazla 5 mp3 yükleyebilir!
async def upload_song(request: Request, file: UploadFile = File(...)):
    # ...
```

### 29.2 Kimlik Doğrulama (Firebase JWT Auth)
İleride kayıtlı hesap sistemine geçildiğinde, Android istemciden atılan her istekte `Authorization: Bearer <JWT_TOKEN>` aranır. API rotası `Depends(verify_token)` ile kilitlenerek yetkisiz erişimler 401 Unauthorized hatasıyla anında savuşturulur.

# BÖLÜM 30: VERİTABANI TAŞIMA VE SÜRÜM GÜNCELLEMELERİ (ROOM MIGRATIONS)

Uygulama yayına alındıktan aylar sonra Veritabanı (Room DB) tablolarına yeni bir kolon eklendiğinde (Örn: `SongEntity`'ye `isFavorite` kolonu), Android işletim sistemi tabanı değiştiremediği için eski sürümdeki cihazlarda oyunu   çökertir   (Crash). Bunu önlemek için SQL Göç (Migration) kodlaması şarttır.

```kotlin
// Android Room Veritabanı Versiyonu 1'den 2'ye geçerken:
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Eski kullanıcının yüksek skorlarını silmemek için "ALTER TABLE" ile kolon eklenir
        db.execSQL("ALTER TABLE songs ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

// Room Builder'a entegrasyon
Room.databaseBuilder(context, AppDatabase::class.java, "rhythm_db")
    .addMigrations(MIGRATION_1_2) 
    .build()
```
Bu sayede milyonlarca kullanıcının High-Score veya Unlock edilmiş premium içerikleri ASLA silinmez.

# BÖLÜM 31: ANALİTİK OLAYLARI SÖZLÜĞÜ (FIREBASE EVENT DICTIONARY)

Uygulamanın pazar payını ölçmek ve "Telemetri" sağlamak (Data-Driven Design) için izlenecek spesifik aksiyon listesi:

      `song_imported`:   Kullanıcı telefondan MP3 yüklediğinde. (Param: `file_size`, `duration`). Neden? Kullanıcılar hep 10MB üstü dosya atıyorsa sunucu disk sınırımızı büyüteceğimizi anlarız.
      `game_started`:   Bir ritim maçı başladığında. (Param: `song_id`, `difficulty`). En çok oynanan şarkı ve mod belirlenir.
      `game_over_heat`:   Oyuncu   MISS   yapıp canı bularak öldüğünde. (Param: `song_id`, `death_time_ms`). Neden? Eğer oyuncuların çoğu 'Megalovania' şarkısının "65. saniyesinde" ölüyorsa, o saniyedeki DSP algoritmasında çok abartılı bir nota dizilimi vardır; şarkı kodu yumuşatılarak kullanıcı öfkesi (Rage quit) önlenir.
      `settings_calibration_changed`:   Ses offset ayarı değiştiğinde. Param: `new_offset`. Kullanıcı ortalaması `-200ms` ise motorun varsayılan değerini doğrudan `-200` yazarız.

---
  Mühendislik Özeti (Son Söz):  
 Rhythm Game  Mimari kodu, modern teknolojiye "Cihazın Kas Gücü" ile hükmeden eksiksiz bir sistemdir. Zaman manipülasyonu ile tasarlanmış senkron oyun yapısı ve buluttan uzaklaştırılan kenar bilişim sinyal analitik modeli ile muazzam bir ticari gücü (ROI) elinde tutar. Bu proje devralınmaya ve milyarlara servis edilmeye (Scalable) %100 Hazırdır.
