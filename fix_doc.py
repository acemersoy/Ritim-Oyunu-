import codecs

path = r'c:\Users\aceme\OneDrive\Masaüstü\Proje\rhythm_game_developer_guide_v3.md'
f = codecs.open(path, 'r', 'utf-8')
lines = f.readlines()
f.close()

new_toc = """# İÇİNDEKİLER

1. **Sistem Mimarisi ve Teknoloji Seçimleri (Neden Bu Teknolojiler?)**
2. **Veri Modelleri, Veritabanı Şemaları (ER) ve Depolama Mimarileri**
3. **Akış Diyagramları: Uygulama İçi Veri Hattı (Pipeline)**
4. **Arka Uç (Backend): Python FastAPI ve Asenkron Çekirdek**
5. **Ses Sinyal İşleme (DSP) ve Librosa Algoritmaları (Matematiksel Alt Yapı)**
6. **Android Mimari: Jetpack Compose, Hilt ve Clean Architecture**
7. **Mobil Oyun Motoru: SurfaceView, 60 FPS Render ve ExoPlayer Audio Sync**
8. **Chaquopy: Kenar Bilişim (Edge Computing) Gelecek Planı Geçiş Kodları**
9. **Klonlama, Derleme ve Sorun Giderme Koruması**
10. **API Sözleşmesi (REST Endpoints & Swagger Contract)**
11. **Proje Klasör Ağacı (Directory Structure) ve İskelet Mimarisi**
12. **Tasarım Dili (Design System) ve Neon Siberpunk Mimarisi**
13. **Güvenlik, Exception Handling (Hata Yönetimi)**
14. **Gelecek Vizyonu ve Beklentiler (Proje Master Planı)**
15. **Sistem Güvenliği ve Tersine Mühendislik Koruması (Security & Obfuscation)**
16. **Derleme Ortamları (Build Flavors) ve Varyasyonlar**
17. **Git Mimarisi ve Takım Çalışması (Git Flow)**
18. **Telemetri, APM (App Performance Monitoring) ve UX İzleme**
19. **Donanım Limitasyonları ve Cihaz Matrisi (Hardware Targets)**
20. **Kodlama Standartları ve Stil Rehberi (Code Conventions)**
21. **Yerelleştirme (L10n), Küreselleşme (I18n) ve Erişilebilirlik**
22. **Monetizasyon (Gelir Modeli) ve In-App Purchases (IAP) Mimarisi**
23. **Veri Gizliliği, GDPR ve KVKK Uyumluluğu**
24. **Merkezi Oyun Mekanikleri ve Oyun Fiziği Matematiği**
25. **Oyun Durumu (Game State) Mimarisi ve Sonlu Durum Makinesi (FSM)**
26. **DevOps ve CI/CD Otomasyon Rehberi (GitHub Actions)**
27. **Bellek Profili (Memory Profiling) ve Performans Bütçeleri**
28. **Açık Kaynak Lisansları ve Hukuki (Legal) Uyumluluk**
"""

chapter_24 = []
new_lines = []
in_toc = False
in_chapter_24 = False

for i, line in enumerate(lines):
    if 'Python (Backend)::' in line:
        line = line.replace('Python (Backend)::', 'Python (Backend):')
        if '---' in line:
            line = line.replace('---', '')

    if line.startswith('# İÇİNDEKİLER'):
        in_toc = True
        new_lines.append(new_toc)
        continue

    if in_toc:
        if line.startswith('---'):
            in_toc = False
            new_lines.append(line)
        continue

    if line.startswith('# BÖLÜM 24:'):
        in_chapter_24 = True
        chapter_24.append(line)
        continue

    if in_chapter_24:
        if line.startswith('# BÖLÜM 21:'):
            in_chapter_24 = False
            new_lines.append(line)
        else:
            chapter_24.append(line)
            continue

    if not in_toc and not in_chapter_24:
        new_lines.append(line)

final_lines = []
for line in new_lines:
    if line.startswith('# BÖLÜM 25:'):
        final_lines.append('---\n\n')
        final_lines.extend(chapter_24)
        if not chapter_24[-1].endswith('\n'):
            final_lines.append('\n')
        
    final_lines.append(line)

f = codecs.open(path, 'w', 'utf-8')
f.writelines(final_lines)
f.close()
print('Done')
