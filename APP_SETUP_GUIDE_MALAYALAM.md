# ആപ്പ് നിർമ്മാണത്തിനും ഓട്ടോമേഷനുമുള്ള സമ്പൂർണ്ണ സഹായി (App Setup & Automation Guide)

ഈ ഗൈഡിൽ നിങ്ങളുടെ ഓരോ ആപ്പുകൾക്കും (Malayalam, Tamil, Hindi, Troll മുതലായവ) ആവശ്യമായ **Google Sheet Web App**, **Firebase FCM**, **google-services.json**, **GitHub Automation** എന്നിവ എങ്ങനെ എളുപ്പത്തിൽ ചെയ്യാം എന്ന് വ്യക്തമായി വിശദീകരിക്കുന്നു.

---

## ഉള്ളടക്കം (Table of Contents)
1. [ഘട്ടം 1: Google Sheet Web App നിർമ്മാണം (Comments & Likes)](#ഘട്ടം-1-google-sheet-web-app-നിർമ്മാണം-comments--likes)
2. [ഘട്ടം 2: Firebase പ്രോജക്റ്റും google-services.json ഫയലും](#ഘട്ടം-2-firebase-പ്രോജക്റ്റും-google-servicesjson-ഫയലും)
3. [ഘട്ടം 3: ഫയർബേസ് FCM കീ (FIREBASE_KEY) ഡൗൺലോഡ് ചെയ്യൽ](#ഘട്ടം-3-ഫയർബേസ്-fcm-കീ-firebase_key-ഡൗൺലോഡ്-ചെയ്യൽ)
4. [ഘട്ടം 4: ആപ്പിലെ ടോപ്പിക്ക് ക്രമീകരിക്കൽ (notifications.xml)](#ഘട്ടം-4-ആപ്പിലെ-ടോപ്പിക്ക്-ക്രമീകരിക്കൽ-notificationsxml)
5. [ഘട്ടം 5: GitHub Automation & ഫയൽ ക്രമീകരണം (santhkhd/config)](#ഘട്ടം-5-github-automation--ഫയൽ-ക്രമീകരണം-santhkhdconfig)
6. [ഘട്ടം 6: ഭാവിയിൽ പുതിയൊരു ആപ്പ് ചേർക്കുമ്പോൾ ചെയ്യേണ്ട കാര്യങ്ങൾ](#ഘട്ടം-6-ഭാവിയിൽ-പുതിയൊരു-ആപ്പ്-ചേർക്കുമ്പോൾ-ചെയ്യേണ്ട-കാര്യങ്ങൾ)

---

## ഘട്ടം 1: Google Sheet Web App നിർമ്മാണം (Likes, Dislikes, Comments & Settings)

ആപ്പിലെ വാർത്തകൾക്ക് താഴെ ഉപയോക്താക്കൾ നൽകുന്ന **Likes**, **Dislikes**, **Comments**, ഒപ്പം ആപ്പിന്റെ റിമോട്ട് **Settings** എന്നിവ മാനേജ് ചെയ്യാനുള്ള ഓൾ-ഇൻ-വൺ ഗൂഗിൾ ഷീറ്റ് ബാക്കെൻഡ്:

### 1.1 പുതിയ ഗൂഗിൾ ഷീറ്റ് ഉണ്ടാക്കുക
1. [Google Sheets](https://sheets.google.com) തുറന്ന് പുതിയൊരു ബ്ലാങ്ക് സ്പ്രെഡ്ഷീറ്റ് (Blank Spreadsheet) ഉണ്ടാക്കുക.
2. പേര് `Malayalam Movie & News Backend` എന്ന് നൽകാം. (കോളങ്ങൾ നിങ്ങൾ സ്വയം ഉണ്ടാക്കേണ്ടതില്ല, സ്ക്രിപ്റ്റ് തനിയെ 3 ടാബുകൾ ഡിസൈൻ ചെയ്തുകൊള്ളും).

### 1.2 Apps Script കോഡ് ചേർക്കുക
1. ഷീറ്റിന്റെ മുകളിലെ മെനുവിൽ നിന്ന് **Extensions** -> **Apps Script** തുറക്കുക.
2. അവിടെയുള്ള പഴയ കോഡ് മുഴുവൻ മായ്ച്ചുകളയുക.
3. നിങ്ങളുടെ പ്രോജക്റ്റിലെ [`GoogleSheet_Code.gs`](file:///e:/My_works/pub/com.shobmc.san/GoogleSheet_Code.gs) ഫയലിലുള്ള മുഴുവൻ കോഡും കോപ്പി ചെയ്ത് അവിടെ പേസ്റ്റ് ചെയ്യുക.
4. മുകളിലെ **Save (സേവ്)** ഐക്കൺ ക്ലിക്ക് ചെയ്യുക (Ctrl + S).
5. (നിങ്ങൾക്ക് വേണമെങ്കിൽ മുകളിലെ ഡ്രോപ്ഡൗണിൽ നിന്ന് `initializeSheets` സെലക്ട് ചെയ്ത് **Run** ക്ലിക്ക് ചെയ്താൽ ഉടൻ തന്നെ **Reactions**, **Comments**, **Settings** എന്നീ 3 ടാബുകൾ വർണ്ണാഭമായി രൂപപ്പെടുന്നത് കാണാം).

### 1.3 Web App ആയി Deploy ചെയ്യുക (വളരെ പ്രധാനം!)
1. മുകളിൽ വലതുവശത്തുള്ള നീല **Deploy** -> **New deployment** ക്ലിക്ക് ചെയ്യുക.
2. ഇടതുവശത്തെ ഗിയർ (⚙️) ഐക്കണിൽ ക്ലിക്ക് ചെയ്ത് **Web app** തിരഞ്ഞെടുക്കുക.
3. താഴെ പറയുന്ന സെറ്റിംഗ്സ് കൃത്യമായി നൽകുക:
   * **Description:** `App Backend v1.0`
   * **Execute as:** `Me` (നിങ്ങളുടെ ഇമെയിൽ)
   * **Who has access:** `Anyone` *(ഇത് നിർബന്ധമായും "Anyone" എന്ന് തന്നെ കൊടുക്കണം, എങ്കിൽ മാത്രമേ ആപ്പിൽ നിന്ന് ലോഗിൻ ഇല്ലാതെ ലൈക്കുകളും കമന്റുകളും സേവാവുകയുള്ളൂ)*.
4. **Deploy** ക്ലിക്ക് ചെയ്യുക. അനുമതി ചോദിച്ചാൽ (**Authorize access**) നിങ്ങളുടെ ഗൂഗിൾ അക്കൗണ്ട് സെലക്ട് ചെയ്ത് *Advanced -> Go to ... (unsafe) -> Allow* നൽകുക.
5. അവസാനം ലഭിക്കുന്ന **Web app URL** (`https://script.google.com/macros/s/AKfycb.../exec`) കോപ്പി ചെയ്യുക.

### 1.4 ലിങ്ക് ആപ്പിൽ ചേർക്കുക
ഈ ലിങ്ക് നിങ്ങളുടെ `app/src/main/assets/config.json` ഫയലിലെ രണ്ടാമത്തെ വരിയിലുള്ള `"google_sheet_url"` എന്നതിൽ പേസ്റ്റ് ചെയ്യുക:
```json
{
    "google_sheet_url": "https://script.google.com/macros/s/AKfycb.../exec",
    "app": {
...
```
*(അല്ലെങ്കിൽ `Config.java`-യിലെ `GOOGLE_SHEET_WEBAPP_URL`-ലും നൽകാം).*

### 1.5 ഈ ബാക്കെൻഡിൽ ലഭിക്കുന്ന 3 ടാബുകൾ:
1. **Reactions (Likes & Dislikes):** ഓരോ വാർത്തയ്ക്കും ഉപയോക്താക്കൾ നൽകിയ യഥാർത്ഥ ലൈക്കുകളും ഡിസ്‌ലൈക്കുകളും കൃത്യമായി ട്രാക്ക് ചെയ്യുന്നു.
2. **Comments:** വായനക്കാരുടെ പേര്, സമയം, കമന്റ്, ആർട്ടിക്കിൾ ടൈറ്റിൽ എന്നിവ വരിവരിയായി ഇവിടെ രേഖപ്പെടുത്തുന്നു.
3. **Settings:** ആപ്പിലെ നോട്ടീസുകൾ, ബാനർ പരസ്യങ്ങൾ (true/false), മെയിന്റനൻസ് മോഡ് (true/false), ആപ്പ് അപ്ഡേറ്റ് മെസ്സേജ് എന്നിവ ആപ്പ് അപ്ഡേറ്റ് ചെയ്യാതെ തന്നെ ഷീറ്റിൽ നിന്ന് മാറ്റാൻ സാധിക്കുന്നു.

---

## ഘട്ടം 2: Firebase പ്രോജക്റ്റും google-services.json ഫയലും

നിങ്ങളുടെ എല്ലാ ആപ്പുകൾക്കും കൂടി ഒരൊറ്റ ഫയർബേസ് പ്രോജക്റ്റ് മതിയാകും.

1. [Firebase Console](https://console.firebase.google.com) തുറക്കുക.
2. **Add project** ക്ലിക്ക് ചെയ്ത് ഒരു പേര് നൽകുക (ഉദാ: `My-Media-Network`).
3. പ്രോജക്റ്റ് തുറന്ന ശേഷം **Android (ആൻഡ്രോയിഡ് ചിഹ്നം)** ക്ലിക്ക് ചെയ്യുക.
4. നിങ്ങളുടെ ആപ്പിന്റെ Package Name നൽകുക (ഉദാ: `com.shobmc.san`).
5. **Register app** ക്ലിക്ക് ചെയ്ത ശേഷം **Download google-services.json** എന്ന നീല ബട്ടൺ ക്ലിക്ക് ചെയ്യുക.
6. ഡൗൺലോഡ് ചെയ്ത `google-services.json` ഫയൽ നിങ്ങളുടെ പ്രോജക്റ്റിലെ `app/` ഫോൾഡറിലേക്ക് മാറ്റി വെക്കുക (`app/google-services.json`).

> **ഓർക്കുക:** ഭാവിയിൽ തമിഴ്, ഹിന്ദി, ട്രോൾ ആപ്പുകൾ ചെയ്യുമ്പോൾ ഇതേ Firebase പ്രോജക്റ്റിൽ തന്നെ വീണ്ടും **Add App** കൊടുത്ത് അവയുടെ പാക്കേജ് നെയിം നൽകി അതത് ആപ്പിന്റെ `google-services.json` ഡൗൺലോഡ് ചെയ്താൽ മതി.

---

## ഘട്ടം 3: ഫയർബേസ് FCM കീ (FIREBASE_KEY) ഡൗൺലോഡ് ചെയ്യൽ

GitHub Actions വഴി അൺലിമിറ്റഡ് ആയി സൗജന്യ നോട്ടിഫിക്കേഷൻ അയക്കാൻ ഈ കീ ആവശ്യമാണ്.

1. ഫയർബേസ് കൺസോളിൽ മുകളിൽ ഇടതുവശത്തുള്ള **Settings (⚙️) -> Project settings** എടുക്കുക.
2. മുകളിലുള്ള **Service accounts** ടാബ് ക്ലിക്ക് ചെയ്യുക.
3. താഴെയുള്ള **Generate new private key** ക്ലിക്ക് ചെയ്യുക. ഒരു പോപ്പ്-അപ്പ് വരും, വീണ്ടും **Generate key** ക്ലിക്ക് ചെയ്യുക.
4. ഇപ്പോൾ നിങ്ങളുടെ കമ്പ്യൂട്ടറിൽ ഒരു `.json` ഫയൽ ഡൗൺലോഡ് ആകും (ഉദാ: `my-media-network-firebase-adminsdk-xxxx.json`).
5. ആ ഫയൽ നോട്ട്പാഡിലോ കോഡ് എഡിറ്ററിലോ തുറന്ന് അതിലുള്ള **മുഴുവൻ കോഡും കോപ്പി ചെയ്യുക**.
6. നിങ്ങളുടെ GitHub റിപ്പോസിറ്ററിയിൽ ([santhkhd/config](https://github.com/santhkhd/config)) പോകുക:
   * **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**.
   * **Name:** `FIREBASE_KEY`
   * **Secret:** കോപ്പി ചെയ്ത മുഴുവൻ ഫയൽ ഉള്ളടക്കവും ഇവിടെ പേസ്റ്റ് ചെയ്ത് **Add secret** കൊടുക്കുക.

---

## ഘട്ടം 4: ആപ്പിലെ ടോപ്പിക്ക് ക്രമീകരിക്കൽ (notifications.xml)

ആപ്പ് ഇൻസ്റ്റാൾ ചെയ്യുമ്പോൾ ഏത് കാറ്റഗറി നോട്ടിഫിക്കേഷൻ ആണ് ലഭിക്കേണ്ടത് എന്ന് തീരുമാനിക്കുന്നത് ഇവിടെയാണ്.

ഫയൽ ലൊക്കേഷൻ: `app/src/main/res/values/notifications.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">

    <!-- ഈ ആപ്പിലെ ഉപയോക്താക്കൾ സബ്സ്ക്രൈബ് ചെയ്യുന്ന ഫയർബേസ് ടോപ്പിക്ക് -->
    <string name="fcm_notification_topic">malayalam_topic</string>

    <!-- ഫയർബേസ് മാത്രം ഉപയോഗിക്കാൻ ഇത് 0000... ആയി തന്നെ വെക്കുക -->
    <string name="onesignal_app_id">00000000-0000-0000-0000-000000000000</string>

</resources>
```
* തമിഴ് ആപ്പിൽ: `<string name="fcm_notification_topic">tamil_topic</string>`
* ഹിന്ദി ആപ്പിൽ: `<string name="fcm_notification_topic">hindi_topic</string>`
* ട്രോൾ ആപ്പിൽ: `<string name="fcm_notification_topic">malayalamtroll_topic</string>`

---

## ഘട്ടം 5: GitHub Automation & ഫയൽ ക്രമീകരണം (santhkhd/config)

നിങ്ങളുടെ GitHub റിപ്പോസിറ്ററി ([santhkhd/config](https://github.com/santhkhd/config)) താഴെ കാണുന്ന രീതിയിൽ ചിട്ടയായി സൂക്ഷിക്കുക:

```text
santhkhd/config/
│
├── .github/
│   └── workflows/
│       ├── fcm_monitor.yml          <-- ഫയർബേസ് അൺലിമിറ്റഡ് ഓട്ടോമേഷൻ (ഓരോ മണിക്കൂറിലും പ്രവർത്തിക്കും)
│       └── feed_monitor.yml         <-- OneSignal ഓട്ടോമേഷൻ (നിങ്ങൾക്ക് വേണമെങ്കിൽ ഉപയോഗിക്കാം)
│
├── automation/
│   ├── fcm_monitor.py               <-- ഹൈ-സ്പീഡ് FCM പൈത്തൺ പ്രോഗ്രാം
│   ├── feed_monitor.py              <-- OneSignal പൈത്തൺ പ്രോഗ്രാം
│   ├── apps.json                    <-- എല്ലാ ആപ്പുകളുടെയും ലിസ്റ്റ്
│   ├── requirements.txt             <-- ലൈബ്രറികൾ (feedparser, requests, firebase-admin)
│   └── state/                       <-- നോട്ടിഫിക്കേഷൻ അയച്ച വിവരങ്ങൾ സേവ് ചെയ്യുന്ന സ്ഥലം
│
├── malayalam/                       <-- മലയാളം ആപ്പിന്റെ കോൺഫിഗ് ഫയലുകൾ
│   ├── config.json
│   ├── home.json
│   └── ...
│
├── tamil/                           <-- തമിഴ് ആപ്പിന്റെ കോൺഫിഗ് ഫയലുകൾ
│   ├── config.json
│   └── ...
│
├── hindi/                           <-- ഹിന്ദി ആപ്പിന്റെ കോൺഫിഗ് ഫയലുകൾ
│   ├── config.json
│   └── ...
│
└── malayalamtroll/                  <-- ട്രോൾ ആപ്പിന്റെ കോൺഫിഗ് ഫയലുകൾ
    ├── config.json
    └── ...
```

---

## ഘട്ടം 6: ഭാവിയിൽ പുതിയൊരു ആപ്പ് ചേർക്കുമ്പോൾ ചെയ്യേണ്ട കാര്യങ്ങൾ

ഉദാഹരണത്തിന് നാളെ നിങ്ങൾ **"Telugu Cinema"** എന്നൊരു പുതിയ ആപ്പ് തുടങ്ങുന്നു എന്നിരിക്കട്ടെ:

1. **Firebase Console-ൽ**:
   * പഴയ അതേ പ്രോജക്റ്റിൽ **Add App** കൊടുത്ത് തെലുങ്ക് ആപ്പിന്റെ പാക്കേജ് നെയിം നൽകുക.
   * `google-services.json` ഡൗൺലോഡ് ചെയ്ത് തെലുങ്ക് പ്രോജക്റ്റിന്റെ `app/` ഫോൾഡറിൽ വെക്കുക.
2. **ആപ്പിലെ notifications.xml-ൽ**:
   * `<string name="fcm_notification_topic">telugu_topic</string>` എന്ന് നൽകുക.
3. **GitHub റിപ്പോസിറ്ററിയിൽ**:
   * `telugu/` എന്നൊരു പുതിയ ഫോൾഡർ ഉണ്ടാക്കി അതിൽ `config.json` തുടങ്ങിയ ഫയലുകൾ വെക്കുക.
   * `automation/apps.json` ഫയലിൽ താഴെ കാണുന്നതുപോലെ 5 വരികൾ ചേർക്കുക:
   ```json
   {
     "app_id": "telugu",
     "name": "Telugu Movies & TV",
     "folder": "telugu",
     "fcm_topic": "telugu_topic",
     "accent_color": "FFF59E0B",
     "emoji": "🎥",
     "daily_limit": 5,
     "min_gap_hours": 2,
     "active_hours": [8, 22]
   }
   ```

ഇത്രയും ചെയ്താൽ മതി! പുതിയ ആപ്പിനും ഫയർബേസ് നോട്ടിഫിക്കേഷൻ തനിയെ പ്രവർത്തിച്ചു തുടങ്ങും. ഒരൊറ്റ പൈസ പോലും ചിലവില്ലാതെ ലക്ഷക്കണക്കിന് ഉപയോക്താക്കൾക്ക് പരിധിയില്ലാതെ നോട്ടിഫിക്കേഷനുകൾ അയക്കാം.
