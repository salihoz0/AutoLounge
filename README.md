# 🏎️ AutoLounge — Modern Android Auto Cockpit & Multimedia Lounge

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20Auto%20%7C%20CarPlay%20Style-10B981?style=for-the-badge&logo=android" alt="Android Auto" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/License-VIP%20%2B%20Freemium-F59E0B?style=for-the-badge" alt="License" />
</p>

---

## 🌟 Nedir? / What is AutoLounge?

**AutoLounge**, aracınızın multimedya ekranını (Android Auto / Head Unit) son teknoloji lüks bir kokpite dönüştüren açık kaynaklı ve modüler bir eğlence platformudur. 

Fermata Auto veya AAAD gibi 2012'den kalma, hantal ve karmaşık arayüzlerin aksine; **Porsche Taycan / Tesla / Apple CarPlay** estetiğinde ultra lüks, akıcı ve yüksek okunurluğa sahip bir gösterge paneli sunar.

> **Önemli (Android sürümüne göre kurulum):** Android 13 ve altında KingInstaller ile Google Play'den
> yüklenmiş gibi damgalanır ve aracınız anında tanır. **Android 14 ve üzerinde KingInstaller çalışmaz**;
> sideload edilen uygulamayı Android Auto'da görmek için **AA Geliştirici Ayarları → "Bilinmeyen kaynaklar"
> (Unknown sources)** açık olmalıdır (veya AAWireless gibi bir dongle'ın geliştirici modu).

---

## ✨ Öne Çıkan Özellikler (Key Features)

### 1. 🎛️ Zengin Uygulama & Canlı Yayın Kataloğu (`+ Ekle`)
Ekranı gereksiz ikonlarla boğmaz; zengin kütüphanesinden dilediğiniz servisi tek tıkla kokpitinize eklersiniz:
- **Popüler Yayınlar:** YouTube, Netflix, Disney+, Prime Video, Twitch, Kick, Exxen, GAİN, BluTV, Spotify.
- **Canlı Ulusal TV:** TRT 1, TRT Spor, TRT Haber, NTV, Habertürk, Sözcü TV, CNN Türk, Show TV, TV8, NOW TV.
- **Kesintisiz Radyolar:** Power FM, Power Türk, Kral Pop, Virgin Radio, Radyo Fenomen, Joy Türk, Metro FM.
- **Yapay Zeka & Finans:** Google Gemini AI, ChatGPT, TradingView, Bloomberg HT, Investing.com.
- **Özel Web Sitesi / URL Ekleme:** Dilediğiniz internet sitesini (ör. şirket portalı, özel kamera yayını) başlık ve URL ile anında kokpit kartı yapın.

### 2. 🎨 Modern Otomotiv Ergonomisi & Tasarım
- **Onyx & Sapphire Glass Estetiği:** Gece sürüşlerinde gözü yormayan saf siyah OLED arka plan, zarif neon vurgular ve okunabilir tipografi (14-16sp).
- **Sürükle-Bırak Düzenleme:** Kartlara uzun basarak veya "Düzenle" butonuna dokunarak kokpitinizi dilediğiniz gibi kişiselleştirin.
- **Gündüz / Gece / Otomatik Modlar:** Araç farlarına veya ortam ışığına göre otomatik uyum sağlayan tema motoru.

### 3. 🛡️ Donanım İmzalı 60 Dakika Deneme & Lisanslama Sistemi
- **60 Dakika Ücretsiz Deneme:** Her yeni cihaz için araç bağlıyken video oynatıldığında işleyen 60 dakikalık deneme süresi.
- **Sıfırlanamaz Güvenlik Mimarisi:** Süre; cihazın benzersiz donanım imzası (`Android ID` + SHA-256 tuzlanmış hash) ile saklanır. Uygulamayı silip tekrar yüklemek veya verileri temizlemek süreyi sıfırlamaz.
- **VIP Lisans Anahtarı Etkinleştirme:** Shopier, Stripe veya sponsorluk yoluyla temin edilen `AL-XXXX-YYYY-ZZZZ` formatındaki lisanslar ve promosyon kodları (`AUTOLOUNGE-VIP`) ile tek dokunuşta ömür boyu sınırsız erişim.

---

## 🚀 Kurulum Rehberi (GitHub Releases)

Android Auto, Google Play dışından yüklenen APK'ların araç ekranında görünmesini engeller. Bu engeli aşmanın yolu Android sürümüne göre değişir.

### Android 13 ve altı — KingInstaller ile (tek tıkla)
```
[ AutoLounge APK İndir ] ──> [ KingInstaller ] ──> [ Install as Google Play Store ] ──> [ Araca Bağlan 🏎️ ]
```
1. **APK'yı İndirin:** Bu reponun **Releases** bölümünden en son `AutoLounge-vX.X.apk` dosyasını indirin.
2. **[KingInstaller](https://github.com/fcaronte/KingInstaller/releases)** uygulamasını kurup açın.
3. **"Select file"** ile AutoLounge APK'sını seçip **"Install as Google Play Store"** deyin.
4. Telefonu araca bağlayın; **AutoLounge Kokpit** araç ekranındaki uygulama listesinde görünür.

### Android 14 ve üzeri — "Bilinmeyen kaynaklar" ile
KingInstaller bu sürümlerde çalışmaz. Sideload edilen uygulamanın AA'da görünmesi için:
1. APK'yı `adb install -r AutoLounge-vX.X.apk` ile (veya dosya yöneticisiyle) kurun.
2. Telefonda **Ayarlar → Bağlı cihazlar → Android Auto** açın.
3. En altta **"Sürüm ve izin bilgisi"** bölümüne ~10 kez dokunup **Geliştirici Modu**'nu açın.
4. Sağ üst **⋮ → Geliştirici Ayarları → "Bilinmeyen kaynaklar" (Unknown sources)** seçeneğini işaretleyin.
5. Telefonu araca bağlayın; **AutoLounge** uygulama listesinde görünür. (Alternatif: AAWireless geliştirici modu.)

> **Not:** Videolar yalnızca araç park halindeyken görüntülenebilir; sürüşte ses devam eder, görüntü güvenlik gereği gizlenir.

---

## 🏗️ Teknik Mimari (Architecture)

```
app/src/main/java/com/autohub/park/
├── data/
│   ├── AppCatalogRepository.kt     # 30+ Popüler servis, canlı TV ve radyo kataloğu
│   ├── UsageSecurityManager.kt     # Donanım imzalı SHA-256 kullanım ve lisans motoru
│   ├── BookmarkViewModel.kt        # Jetpack MVVM ViewModel
│   └── database/                   # Room Persistence (Özel kartlar ve ayarlar)
├── service/
│   ├── ParkAudioService.kt         # Android Auto MediaBrowserService (Root & Now Playing)
│   └── AutoLoungeSyncBridge.kt     # Telefon-Araç senkronizasyon köprüsü
└── ui/
    ├── MainActivity.kt             # Telefon Arayüzü & Özelleştirici
    ├── CarMainActivity.kt          # Araç Ekranı (Desktop Head Unit / AA Launcher)
    ├── components/
    │   ├── CarDashboardContent.kt  # Ultra modern otomotiv gösterge kartları
    │   ├── AddAppModal.kt          # Kategori filtreli zengin ekleme modalı
    │   └── PaywallSheet.kt         # Lisans etkinleştirme ve VIP yönetim diyaloğu
    └── theme/                      # Onyx, Sapphire, Emerald otomotiv renk paletleri
```

### Sıfır Sunucu Yükü & CDN Mimarisi
- Video ve TV yayınları doğrudan resmi Akamai, Cloudflare ve YouTube CDN'leri üzerinden doğrudan cihazınıza akar.
- Özel bir proxy veya video aktarım sunucusu gerektirmez; bu sayede bant genişliği maliyeti $0'dır.

---

## 🔑 Lisans Anahtarları & Satın Alma

AutoLounge'ı geliştirmeye devam etmemizi desteklemek ve 60 dakikalık deneme süresini ömür boyu sınırsız VIP'ye yükseltmek için:
- **Shopier / Kredi Kartı:** Anında teslimat ile SMS/E-posta üzerinden lisans kodu.
- **GitHub Sponsors:** Projeye sponsor olan tüm geliştiricilere özel VIP lisansı.
- **Lisans Girişi:** Uygulama içindeki `Üyelik` sekmesinden veya araç ekranındaki `[Kilidi Aç]` menüsünden anahtarınızı yazıp `[Onayla]` demeniz yeterlidir.

---

## 🛠️ Geliştirici Ortamı (Developer Setup)

```bash
# Projeyi klonlayın
git clone https://github.com/your-username/AutoLounge.git
cd AutoLounge

# Debug APK derleyin
./gradlew assembleDebug

# Cihaza veya Desktop Head Unit'e (DHU) kurun
adb forward tcp:5277 tcp:5277
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📜 Lisans & Sorumluluk Reddi (Disclaimer)
Bu yazılım araştırma ve kişisel araç içi eğlence konsepti amacıyla geliştirilmiştir. Sürüş güvenliği kurallarına uymak sürücünün sorumluluğundadır; video içerikleri yalnızca araç park halindeyken veya yolcu eğlencesi için kullanılmalıdır.

