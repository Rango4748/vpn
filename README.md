# NetShield — Android VPN Client (V2Ray/Xray)

اپلیکیشن اندروید VPN با ظاهر مینیمال و حرفه‌ای، ساخته‌شده با Kotlin + Jetpack Compose.

## امکانات پیاده‌سازی‌شده در این نسخه
- طراحی مینیمال تیره با لهجه‌ی رنگی کهربایی (Compose, Material 3)
- دکمه اتصال/قطع با مدیریت پرمیشن `VpnService`
- **افزودن کانفیگ به دو روش:**
  - **دستی:** پیست کردن لینک `vmess://` `vless://` `trojan://` `ss://` (پارسر کامل در `ConfigLinkParser.kt`)
  - **پنل:** ورود با یوزر/پس به یک پنل سازگار با Marzban (توکن + دریافت لیست سرور) در `PanelRepository.kt`
  - **سابسکریپشن:** لینک ساب‌اسکریپشن base64 (رایج در فروشنده‌های کانفیگ)
- ذخیره‌سازی محلی سرورها با Room (`ConfigDatabase.kt`)
- صفحه سرورها با تب دستی/پنل/سابسکریپشن و امکان حذف سرور
- صفحه تنظیمات (کیل‌سوییچ، اتصال خودکار، اجرا هنگام بوت — سوییچ‌های UI آماده اتصال به منطق واقعی)
- **صفحه خرید اشتراک (Subscription):** سه پلن آماده با UI کامل؛ طبق درخواستتون فعلاً فقط رابط کاربری است — وقتی جزئیات درگاه (Google Play Billing یا بک‌اند اختصاصی) رو مشخص کردید، به `SubscriptionScreen.kt` وصل می‌شه (وابستگی `billing-ktx` از قبل در `build.gradle.kts` اضافه شده)
- Foreground `VpnTunnelService` با نوتیفیکیشن دائمی و مدیریت TUN

## نکته مهم درباره‌ی هسته V2Ray/Xray
این ریپو اسکلت کامل اپ (UI، ناوبری، دیتابیس، پارس کانفیگ، اتصال به پنل، مدیریت VpnService) رو داره و **کامپایل و اجرا می‌شه**، اما موتور واقعی پروکسی (تبدیل پکت‌های TUN به ترافیک V2Ray/Xray) باید از یک کتابخانه‌ی native اضافه بشه، چون این بخش یک باینری Go کامپایل‌شده (AAR) است که در کد سورس جاوا/کاتلین قرار نمی‌گیره:

1. برو به [Releases](https://github.com/2dust/AndroidLibXrayLite/releases) همون ریپازیتوری — هر ریلیز چند فایل asset داره (از جمله `libv2ray.aar`، خودکار توسط GitHub Actions ساخته و منتشر می‌شن). آخرین نسخه رو دانلود کن.
2. فایل رو دقیقاً در مسیر `app/libs/libv2ray.aar` قرار بدید (پوشه‌ی `libs` رو خودتون بسازید اگه نیست).
3. `implementation(files("libs/libv2ray.aar"))` توی `app/build.gradle.kts` از قبل فعاله.
4. Sync/Build بگیرید.

⚠️ **API این کتابخونه بین نسخه‌ها عوض شده.** نسخه‌ی فعلی که `XrayCoreBridge.kt` باهاش نوشته شده از `CoreController`/`NewCoreController`/`StartLoop(config, tunFd)` استفاده می‌کنه (نه `V2RayPoint` قدیمی). یک نکته‌ی تأییدنشده: در API فعلی، `CoreCallbackHandler` دیگه متد `protect()` نداره — یعنی مکانیزم protect کردن سوکت خروجی هسته (که برای جلوگیری از loop شدن ترافیک ضروریه) ممکنه داخل خود کتابخونه انجام بشه یا به روش دیگه‌ای لازم باشه. قبل از تست نهایی، کد `XrayCoreBridge.kt` رو با سورس فعلی [v2rayNG](https://github.com/2dust/v2rayNG) (که دقیقاً از همین کتابخونه استفاده می‌کنه) تطبیق بده — مسیر `app/src/main/kotlin/.../service/` توی اون ریپو، مرجع رسمی و به‌روزه.
5. اگه بعد از وصل شدن دیدی ترافیک واقعاً از سرور رد نمی‌شه (یا لوپ می‌زنه)، تقریباً قطعاً همین مسئله‌ی protect هست.
4. در `VpnTunnelService.kt`، کلاس `ProxyCoreBridge` رو با فراخوانی متدهای AAR (شروع core با JSON کانفیگ Xray، هدایت ترافیک TUN) پیاده‌سازی کنید.

این جداسازی عمدی هست: بقیه‌ی اپ (که بخش اصلی کار محصولیه) الان کامل و قابل تست‌کردنه، و اتصال هسته فقط توی همون یک فایل انجام می‌شه.

## اجرا
```bash
# با Android Studio (پیشنهادی): پروژه رو باز کنید و Run بزنید
# یا از خط فرمان:
./gradlew assembleDebug
```

## ساختار پروژه
```
app/src/main/java/com/netshield/vpn/
├── data/
│   ├── model/        # ServerConfig, PanelProfile
│   ├── parser/        # ConfigLinkParser (vmess/vless/trojan/ss)
│   ├── network/       # PanelApi (Retrofit, Marzban-style)
│   ├── repository/    # ConfigRepository, PanelRepository
│   └── db/            # Room
├── vpn/                # VpnTunnelService, VpnController, ProxyCoreBridge
├── ui/
│   ├── screens/        # Status, Locations, Settings, Subscription + ViewModels
│   ├── components/     # ConnectButton
│   └── theme/          # رنگ‌ها و تایپوگرافی
└── MainActivity.kt      # Navigation + entry point
```

## قدم بعدی که با هم مشخص می‌کنیم
- جزئیات درگاه پرداخت اشتراک (Google Play Billing یا بک‌اند خودتون)
- اتصال AAR هسته Xray برای اتصال واقعی
- آیکون و برندینگ نهایی
