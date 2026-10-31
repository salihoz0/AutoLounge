<p align="center">
  <img src="website/assets/logo.png" alt="AutoLounge Logo" width="120" height="120" />
</p>

# 🏎️ AutoLounge — Modern Android Auto Kokpit & Multimedya Lounge

<p align="center">
  <a href="https://github.com"><img src="https://img.shields.io/badge/Status-100%25%20Free%20%26%20Open%20Source-10B981?style=for-the-badge" alt="Free and Open Source" /></a>
  <a href="https://android.com"><img src="https://img.shields.io/badge/Platform-Android%20Auto-10B981?style=for-the-badge&logo=android" alt="Android Auto" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose" /></a>
  <a href="#-lisans--sorumluluk-reddi-disclaimer"><img src="https://img.shields.io/badge/Root-Gerekmez%20(No%20Root)-blue?style=for-the-badge" alt="No Root" /></a>
</p>

---

## 📸 Ekran Görüntüleri (Screenshots)

<p align="center">
  <b>Araç Ekranı (Android Auto Cockpit Display — 1280x720 OLED Dark Glass)</b><br/>
  <img src="website/assets/car_cockpit_display.png" alt="AutoLounge Araç Kokpit Ekranı" width="100%" />
</p>

<p align="center">
  <br/>
  <b>Telefon Arayüzü & Kokpit Yöneticisi (Mobile Companion App)</b><br/>
  <img src="website/assets/mobile_app_screen.png" alt="AutoLounge Telefon Eşlikçi Uygulaması" width="340" />
</p>

---

## 🌟 Nedir? (What is AutoLounge?)

**AutoLounge**, aracınız park halindeyken Android Auto multimedya ekranınızı son teknoloji lüks bir sinema ve eğlence merkezine dönüştüren **tamamen ücretsiz ve açık kaynaklı** bir Android Auto uygulamasıdır.

- **Porsche Taycan, Tesla ve Apple CarPlay** estetiğinde koyu cam (Onyx Void), yüksek okunurluklu otomotiv tipografisi ve sürücü dostu butonlar.
- **YouTube 4K, 100+ Canlı TV kanalı**, radyo istasyonları, web siteleri ve dijital yayın platformları doğrudan araç ekranınızda.
- **Root gerektirmez.**
- **Hiçbir abonelik, gizli ücret veya süre kısıtlaması yoktur.**

---

## ⚠️ Kritik Bilgi: Normal APK vs. Bilgisayar (CMD) Kurulumu

> ### ❓ Neden APK'yı telefona indirip doğrudan üzerine tıklayarak kuramıyoruz?
> **Google Play & Android Auto Güvenlik Kısıtlaması:**
> Google, sürüş güvenliği (Driver Distraction) kuralları gereği araç ekranında video oynatan uygulamaların Google Play Store'da yer almasına izin vermez. 
> 
> Eğer AutoLounge APK'sını telefonunuza indirip normal bir uygulama gibi kurarsanız:
> 1. Android Auto, uygulamanın yükleyicisinin Google Play (`com.android.vending`) olmadığını fark eder.
> 2. Uygulamanın araç ekranına video/grafik arayüz göndermesini **engeller.**
> 3. **Sonuç:** Uygulama araç ekranında hiç görünmez veya Android Auto bunu standart bir ses çalıcı (radyo gibi) sanarak **yalnızca arka planda sesini çalar; arabada ekran ve video açılmaz!**

---

### ✅ Çözüm: Bilgisayara Bağlayıp Tek Komutla "Google'dan Yüklenmiş" Göstermek (Root Gerekmez!)

Telefonunuzu **asla rootlamanıza** veya telefona üçüncü parti şüpheli yazılımlar kurmanıza gerek yoktur. Telefonu bilgisayara USB kablosuyla bağlayıp tek bir CMD komutuyla yüklediğinizde, Android sisteme:
> *"Bu uygulama doğrudan Google Play Store tarafından kuruldu"* (`-i "com.android.vending"`)

damgası vurulur. Android Auto bu damgayı gördüğü anda uygulamayı resmi kabul eder ve **araç ekranında tam grafik arayüzü, YouTube'u ve TV yayınlarını anında açar.**

---

## 🚀 Kurulum Rehberi (Installation Guide)

### ⚡ YOL 1: Tek Tıkla Otomatik Kurulum (Tavsiye Edilen — En Kolay)

Teknik bilgi veya manuel CMD komutlarıyla uğraşmak istemiyorsanız, sizin için hazırladığımız akıllı kurulum aracını kullanabilirsiniz. Bu araç:
* Bilgisayarınızda **ADB kurulu değilse otomatik tespit eder** ve *"Google resmi Platform Tools indirilsin mi?"* diye sorup tek tıkla indirir.
* Telefonunuzun USB bağlantısını ve izin durumunu otomatik kontrol eder.
* AutoLounge'ı tek seferde Google Play Store damgasıyla (`com.android.vending`) telefonunuza yükler.

---

#### 🪟 Windows Kullanıcıları İçin:
1. Telefonunuzu USB kablosuyla bilgisayara bağlayın (ve [Telefonda USB Hata Ayıklama](#-ön-hazırlık-telefonda-yapılacak-2-küçük-ayar)'nın açık olduğundan emin olun).
2. Repodan indirdiğiniz klasördeki **`install.bat`** dosyasına **çift tıklayın**.
3. Ekrandaki yönlendirmeleri takip edin; araç gereken her şeyi otomatik tamamlayacaktır!

#### 🍏 macOS / 🐧 Linux Kullanıcıları İçin:
Terminali açıp şu komutu çalıştırmanız yeterlidir:
```bash
bash install.sh
```

---

### 🛠️ YOL 2: Manuel CMD / Terminal ile Kurulum

Eğer komutları kendiniz tek tek çalıştırmak isterseniz:

#### 📋 Ön Hazırlık: Telefonda Yapılacak Ayarlar:

> [!IMPORTANT]
> **1. USB Hata Ayıklama (USB Debugging) Açık Olmak Zorunda mı?**  
> **EVET, KESİNLİKLE ZORUNLUDUR.** Bilgisayarınızın telefonunuza USB kablosu üzerinden resmi Google Play damgasını basabilmesi için bu izin şarttır. Açık olmazsa bilgisayar telefonu algılayamaz veya komut gönderemez. *(Kurulum bitince dilerseniz bu ayarı kapatabilirsiniz).*
> 
> * **Nasıl Açılır?**
>   1. Telefonunuzda **Ayarlar → Telefon Hakkında → Yazılım Bilgileri** bölümüne gidin.
>   2. **"Yapım Numarası" (Build Number)** yazısına peş peşe **7 kez dokunun** ("Geliştirici modu açıldı" der).
>   3. Ayarlar ana sayfasına dönüp en alttaki **Geliştirici Seçenekleri**'ne girin ve **"USB Hata Ayıklama" (USB Debugging)** seçeneğini açın.

---

> [!WARNING]
> **2. Cihazlara Özel Kritik İpuçları (Xiaomi / Samsung / Huawei):**
> 
> * **Xiaomi / Redmi / POCO (MIUI & HyperOS Kullanıcıları — ÇOK ÖNEMLİ):**  
>   Xiaomi cihazlarda yalnızca USB Hata Ayıklama yetmez! Geliştirici Seçenekleri menüsünde biraz aşağı inip şu 2 ayarı da mutlaka **AÇIK** yapmalısınız:
>   - **"USB üzerinden yükle" (Install via USB)**
>   - **"USB Hata Ayıklama (Güvenlik ayarları)" (USB debugging - Security settings)**  
>   *(Bu iki ayar açılmazsa telefon `INSTALL_FAILED_USER_RESTRICTED` hatası verir ve bilgisayardan yüklemeyi engeller).*
> 
> * **Samsung Kullanıcıları:**  
>   Kabloyu taktığınızda ekrana gelen *"Bu bilgisayara her zaman izin ver"* onay kutusunu işaretleyip **İzin Ver**'e dokunun.
> 
> * **Huawei / Honor:**  
>   Geliştirici seçeneklerinde *"Yalnızca şarj modunda ADB hata ayıklamasına izin ver"* seçeneğini aktif edin veya kablo takıldığında USB modunu **"Dosya Aktarımı" (MTP)** seçin.

---

#### 3. Android Auto'da "Bilinmeyen Kaynaklar"ı Açın:
1. Telefonunuzda **Ayarlar → Bağlı Cihazlar → Android Auto** menüsüne girin.
2. En alttaki **"Sürüm"** yazısına peş peşe **10 kez dokunup** geliştirici modunu aktif edin.
3. Sağ üstteki **üç nokta (⋮) → Geliştirici Ayarları → "Bilinmeyen kaynaklar" (Unknown sources)** kutusunu işaretleyin.

> [!NOTE]
> **❓ Neden Google Play Damgası Varken Bu Ayar da Gerekiyor?**  
> Google Play damgası (`-i "com.android.vending"`), telefonun Android Auto projeksiyon servisine bağlanabilmesi için zorunlu kimlik kartıdır. Ancak Google, sürüş güvenliği kuralları gereği araç ekranında video ve web oynatan hiçbir uygulamayı Android Auto'nun resmi beyaz listesine (whitelist) dahil etmez. Android Auto Geliştirici Seçenekleri'ndeki **"Bilinmeyen kaynaklar"** anahtarı, Google'ın resmi beyaz listesinde yer almayan video projeksiyon uygulamalarının araç ekranı menüsünde listelenmesini sağlayan kilittir. Tek seferlik açılır ve telefonunuzu araca taktığınızda AutoLounge araç ekranında görünür.

---

### 🤖 Kendi Telefon Modelinize Özel Yapay Zekaya Sorun (Tek Tıkla)

Telefonunuzun marka/modeline göre (Samsung, Xiaomi, Vivo, Oppo vb.) menülerin yerini bulmakta zorlanıyorsanız, aşağıdaki butonlara dokunduğunuzda hazır soruyla birlikte doğrudan yapay zeka açılır. Sadece telefon modelinizi yazıp Enter'a basmanız yeterlidir:

<p align="center">
  <a href="https://chatgpt.com/?q=Android+telefonumda+Android+Auto+i%C3%A7inde+Geli%C5%9Ftirici+Se%C3%A7enekleri+ve+Bilinmeyen+Kaynaklar+(Unknown+sources)+nas%C4%B1l+a%C3%A7%C4%B1l%C4%B1r%3F+Telefon+modelim%3A+" target="_blank">
    <img src="https://img.shields.io/badge/ChatGPT-Kendi%20Modeline%20G%C3%B6re%20Sor-10a37f?style=for-the-badge&logo=openai&logoColor=white" alt="Ask ChatGPT" />
  </a>
  &nbsp;
  <a href="https://claude.ai/new?q=Android+telefonumda+Android+Auto+i%C3%A7inde+Geli%C5%9Ftirici+Se%C3%A7enekleri+ve+Bilinmeyen+Kaynaklar+(Unknown+sources)+nas%C4%B1l+a%C3%A7%C4%B1l%C4%B1r%3F+Telefon+modelim%3A+" target="_blank">
    <img src="https://img.shields.io/badge/Claude-Kendi%20Modeline%20G%C3%B6re%20Sor-D97706?style=for-the-badge&logo=anthropic&logoColor=white" alt="Ask Claude" />
  </a>
  &nbsp;
  <a href="https://gemini.google.com/app?q=Android+telefonumda+Android+Auto+i%C3%A7inde+Geli%C5%9Ftirici+Se%C3%A7enekleri+ve+Bilinmeyen+Kaynaklar+(Unknown+sources)+nas%C4%B1l+a%C3%A7%C4%B1l%C4%B1r%3F+Telefon+modelim%3A+" target="_blank">
    <img src="https://img.shields.io/badge/Gemini-Kendi%20Modeline%20G%C3%B6re%20Sor-8E75C4?style=for-the-badge&logo=googlegemini&logoColor=white" alt="Ask Gemini" />
  </a>
  <br/><br/>
  <a href="https://www.perplexity.ai/search?q=Android+Auto+Gelistirici+Ayarlari+ve+Bilinmeyen+Kaynaklar+nasil+acilir" target="_blank">
    <img src="https://img.shields.io/badge/Perplexity-Rehberi%20G%C3%B6r-22b8cd?style=for-the-badge&logo=perplexity&logoColor=white" alt="Search Perplexity" />
  </a>
  &nbsp;
  <a href="https://www.google.com/search?q=Android+Auto+Bilinmeyen+Kaynaklar+Unknown+sources+nasil+acilir" target="_blank">
    <img src="https://img.shields.io/badge/Google-Web'de%20Ara-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Search Google" />
  </a>
</p>

> 📋 **Hazır Kopyalanabilir Soru:**
> ```text
> Android telefonumda Android Auto içinde Geliştirici Seçenekleri ve Bilinmeyen Kaynaklar (Unknown sources) nasıl açılır? Telefon modelim: [Modelinizi Yazın]
> ```

---

#### 💻 Kurulum Komutunu Çalıştırın:
Telefonunuzu kabloyla bilgisayara bağlayın (telefonda *"Bu bilgisayara izin verilsin mi?"* uyarısı çıkarsa onaylayın). Ardından APK dosyasının bulunduğu klasörde terminal açıp çalıştırın:

**Windows (CMD / PowerShell):**
```cmd
adb install -i "com.android.vending" -r AutoLounge.apk
```

**macOS / Linux (Terminal):**
```bash
adb install -i "com.android.vending" -r AutoLounge.apk
```

> **İşlem Tamam!** Ekranda `Success` yazdığında telefonunuz araca bağlanmaya hazırdır.

---

### 📱 YOL 3: Bilgisayarsız Kurulum (KingInstaller / AAAD)

Eğer bilgisayarınız yoksa telefon üzerinden kurulum yapabilirsiniz:
* **Android 13 ve altı cihazlar:** [KingInstaller](https://github.com/fcaronte/KingInstaller/releases) uygulamasını kurun, AutoLounge APK'sını seçin ve **"Install as Google Play Store"** butonuna basarak yükleyin.
* **Android 14 ve üzeri cihazlar:** Google Android 14'te paket yükleyici kısıtlamalarını sıkılaştırdığı için [Shizuku](https://shizuku.rikka.app/) + KingInstaller eşleştirmesi gerekir. Bu nedenle **Bilgisayar (CMD)** yöntemi her zaman çok daha hızlı ve sorunsuzdur.

---

## ✨ Öne Çıkan Özellikler (Features)

| Özellik | Açıklama |
| :--- | :--- |
| **📺 YouTube 4K & Canlı TV** | YouTube videolarını, müzikleri ve 100+ ulusal canlı televizyon kanalını (TRT 1, Habertürk, NTV, Sözcü vb.) araç ekranında donmadan izleyin. |
| **🛡️ Akıllı Sürüş Güvenliği** | Araç **Park [P]** modundayken video ekranda oynar; araç hareket ettiğinde sürücü güvenliği için görüntü kararır ve **ses arka planda kesintisiz devam eder.** |
| **⚡ 50s Hücresel Tamponlama** | Tünel geçişlerinde veya zayıf baz istasyonu bölgelerinde yayının donmasını engelleyen optimize ExoPlayer hücre profili. |
| **🎛️ Özel Kokpit (+ Ekle)** | Dilediğiniz web sitesini, canlı kamera akışını veya video portalını tek dokunuşla araç menünüze kısayol kartı olarak ekleyin. |
| **🎨 Otomotiv Temaları** | Onyx Emerald, Sapphire Blue, Amethyst ve Cyber Amber renk seçenekleriyle aracınızın iç ambiyansına tam uyum. |
| **🔒 %100 Ücretsiz & Reklamsız** | Hiçbir deneme süresi, lisans kodu, abonelik veya üçüncü parti izleyici içermez. |

---

## 🛠️ Geliştirici Ortamı (Developer Build)

Projeyi kendi ortamınızda derlemek isterseniz:

```bash
# Repoyu klonlayın
git clone https://github.com/autolounge/AutoLounge.git
cd AutoLounge

# Debug APK derleyin
./gradlew assembleDebug

# Cihazınıza Google Play imzasıyla yükleyin
adb install -i "com.android.vending" -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📜 Lisans & Sorumluluk Reddi (Disclaimer)

* **Trafik ve Sürüş Güvenliği:** Bu uygulama araştırma, kişisel araç içi eğlence ve yolcu konsepti amacıyla açık kaynak olarak sunulmuştur. Trafik kurallarına ve sürüş güvenliğine uymak tamamen sürücünün yasal sorumluluğundadır. Hareket halindeyken sürücünün ekrana odaklanması yasaktır ve tehlikelidir; video özelliklerini yalnızca güvenli park halindeyken veya yolcularınız için kullanınız.
* **Açık Kaynak:** AutoLounge özgür yazılım ilkelerine uygun olarak topluluk için geliştirilmiştir.
