# 🇺🇸 Free US News Aggregator & Summarizer (Android Java + GitHub)

A production-ready, zero-cost US news aggregation and summarization ecosystem. The application discovers breaking and national US stories via Google News RSS, extracts article bodies politely, performs free local extractive summarization, and commits formatted JSON to GitHub via automated GitHub Actions. The modern Material 3 Android Java application consumes the raw JSON with full offline support, category filtering, search, sorting, and background sync.

---

## 📑 Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [How to Create the GitHub Repository](#2-how-to-create-the-github-repository)
3. [How to Upload Files to GitHub](#3-how-to-upload-files-to-github)
4. [How to Enable and Configure GitHub Actions](#4-how-to-enable-and-configure-github-actions)
5. [How to Run Python Locally](#5-how-to-run-python-locally)
6. [How to Test Google News RSS Collection](#6-how-to-test-google-news-rss-collection)
7. [How to Generate and Validate JSON](#7-how-to-generate-and-validate-json)
8. [How to Connect the Android App](#8-how-to-connect-the-android-app)
9. [How to Change GitHub Repository URL](#9-how-to-change-github-repository-url)
10. [How to Add New Categories](#10-how-to-add-new-categories)
11. [How to Add or Customize Search Prompts](#11-how-to-add-or-customize-search-prompts)
12. [How to Change Update Frequency](#12-how-to-change-update-frequency)
13. [Troubleshooting Common Issues](#13-troubleshooting-common-issues)
14. [Copyright and Ethical Guidelines](#14-copyright-and-ethical-guidelines)

---

## 1. Architecture Overview

```text
GitHub Repository (backend & data)
       ↓
GitHub Actions (Hourly cron & workflow_dispatch)
       ↓
Python News Collector (Google News RSS + 1,000 Prompts)
       ↓
Article URLs & Polite Trafilatura / BeautifulSoup Extraction
       ↓
Free Local Extractive Summarizer (3–5 factual sentences, 50–100 words)
       ↓
Generated JSON (data/us_news.json & data/categories.json)
       ↓
GitHub Raw JSON (cdn / raw.githubusercontent.com)
       ↓
Android Java Application (MVVM, Retrofit, Material 3, Offline Cache, WorkManager)
```

- **Zero API Key Requirement**: Requires no OpenAI, Anthropic, NewsAPI, or paid proxies.
- **Copyright Safe**: Full article texts are never republished. Only the headline, source, publication time, a 3–5 sentence original extractive summary, and a direct "Read Full Story" link are provided.

---

## 2. How to Create the GitHub Repository

1. Sign in to [GitHub](https://github.com).
2. Click **New repository** (or navigate to `https://github.com/new`).
3. Set the repository name (e.g. `us-news-app` or `com.usanewspaper.sanshob`).
4. Set visibility to **Public** (required for `raw.githubusercontent.com` access without auth tokens).
5. Leave "Initialize this repository with a README" unchecked if pushing an existing local codebase.
6. Click **Create repository**.

---

## 3. How to Upload Files to GitHub

From your local project terminal:

```bash
# Initialize git if not already initialized
git init

# Add remote repository URL
git remote add origin https://github.com/santhkhd/com.usanewspaper.sanshob.git

# Stage all files
git add .

# Commit changes
git commit -m "Initial commit: Free US News Aggregator and Summarizer"

# Set main branch and push
git branch -M main
git push -u origin main
```

---

## 4. How to Enable and Configure GitHub Actions

1. In your GitHub repository, navigate to **Settings** > **Actions** > **General**.
2. Under **Workflow permissions**, select **Read and write permissions**.
3. Check the box **Allow GitHub Actions to create and approve pull requests**.
4. Click **Save**.
5. Navigate to the **Actions** tab in your repository.
6. Select **Update US News** from the left sidebar and click **Run workflow** to test execution manually.

---

## 5. How to Run Python Locally

### Prerequisites
- Python 3.10, 3.11, 3.12, or 3.13 installed.

### Setup Steps
```bash
# 1. Open terminal in project root
cd /path/to/project

# 2. (Optional) Create and activate virtual environment
python -m venv venv
# On Windows:
.\venv\Scripts\activate
# On Linux/macOS:
source venv/bin/activate

# 3. Install required packages
pip install -r backend/requirements.txt

# 4. Verify installation
python -c "import feedparser, trafilatura, bs4, googlenewsdecoder; print('Dependencies OK!')"
```

---

## 6. How to Test Google News RSS Collection

Run the test suite to verify RSS discovery, URL canonicalization, and deduplication:

```bash
# Run unit tests
python -m unittest discover -s backend/tests -p "test_*.py" -v
```

All 10 tests should pass with `OK`.

---

## 7. How to Generate and Validate JSON

Run the pipeline from the command line:

```bash
# Fast priority mode (fetches top categories, e.g. 20-30 stories):
python backend/generate_news.py --mode=PRIORITY --max-stories=30

# Rotation mode (standard hourly cycle):
python backend/generate_news.py --mode=ROTATION --max-stories=100

# Full mode (all 100 categories):
python backend/generate_news.py --mode=FULL
```

### Validate Output
Check `data/us_news.json` and `data/categories.json`:

```bash
python -c "
import json
with open('data/us_news.json', 'r', encoding='utf-8') as f:
    data = json.load(f)
print(f'Stories count: {len(data[\"stories\"])}')
if data['stories']:
    s = data['stories'][0]
    print('Sample story:', s['title'])
    print('Summary:', s['summary'])
    print('Summary type:', s['summary_type'])
"
```

---

## 8. How to Connect the Android App

The Android app is pre-configured to download the generated raw JSON directly from your repository:

```text
https://raw.githubusercontent.com/santhkhd/com.usanewspaper.sanshob/main/data/us_news.json
```

Or via GitHub Pages CDN:
```text
https://santhkhd.github.io/com.usanewspaper.sanshob/data/us_news.json
```

The configuration is centralized in `app/src/main/java/com/app/webdroid/Config.java`:

```java
public static final String US_NEWS_GITHUB_RAW_URL = "https://raw.githubusercontent.com/santhkhd/com.usanewspaper.sanshob/main/data/us_news.json";
public static final String US_NEWS_CATEGORIES_RAW_URL = "https://raw.githubusercontent.com/santhkhd/com.usanewspaper.sanshob/main/data/categories.json";
```

Build and run the Android app in Android Studio. It will automatically fetch and cache the stories!

---

## 9. How to Change GitHub Repository URL

If you rename your repository or change branches:
1. Open `app/src/main/java/com/app/webdroid/Config.java`.
2. Update `US_NEWS_GITHUB_RAW_URL` and `US_NEWS_CATEGORIES_RAW_URL`.

---

## 10. How to Add New Categories

To add a new news category (e.g. `biotechnology`):

1. Open `backend/prompts.json`.
2. Append a new category object to the `"categories"` array:

```json
{
  "id": "biotechnology",
  "name": "Biotechnology",
  "prompts": [
    "US biotechnology latest news",
    "Biotech drug discovery developments USA",
    "Gene therapy clinical trials US news"
  ]
}
```

3. (Optional) In `backend/generate_news.py`, add an icon name in `get_category_icon`:
```python
"biotechnology": "biotech",
```
4. Run `python backend/generate_news.py` to regenerate categories and news.

---

## 11. How to Add or Customize Search Prompts

1. Open `backend/prompts.json`.
2. Locate the desired category (e.g. `artificial_intelligence`).
3. Add search queries to the `"prompts"` array:

```json
"prompts": [
  "Latest AI news in the US",
  "US artificial intelligence news today",
  "US quantum computing developments today"
]
```

---

## 12. How to Change Update Frequency

The automated update schedule is defined in `.github/workflows/update-news.yml`:

```yaml
on:
  schedule:
    - cron: "0 * * * *"  # Every 1 hour
```

To adjust the schedule:
- **Every 2 hours**: `- cron: "0 */2 * * *"`
- **Every 30 minutes**: `- cron: "*/30 * * * *"`
- **Twice daily (morning and evening)**: `- cron: "0 12,23 * * *"`

Commit and push the changes to `main`.

---

## 13. Troubleshooting Common Issues

### Issue 1: GitHub Actions fails with "Permission to ... denied to github-actions[bot]"
- **Solution**: Go to **Settings** > **Actions** > **General** > **Workflow permissions** and select **Read and write permissions**.

### Issue 2: HTTP 403 or HTTP 401 when fetching publisher websites
- **Solution**: The collector handles paywalls and anti-bot blocks automatically. If direct article extraction fails, the pipeline falls back gracefully to the factual RSS description without fabricating any summary.

### Issue 3: Android displays "You're offline"
- **Solution**:
  1. Check device Wi-Fi/mobile data connectivity.
  2. Verify that `data/us_news.json` has been pushed to GitHub `main` branch.
  3. Ensure `US_NEWS_GITHUB_RAW_URL` in `Config.java` points to your public repository URL.
  4. Notice that cached stories and bundled fallback stories remain accessible offline.

### Issue 4: Duplicate stories appear for the same major event
- **Solution**: Adjust `TITLE_SIMILARITY_THRESHOLD` in `backend/config.py` (default: `0.75`). Lower values group more aggressively (e.g. `0.65`).

---

## 14. Copyright and Ethical Guidelines

- **No Reproduction of Full Articles**: The backend extracts text strictly for local algorithmic sentence importance scoring. Full text is discarded and never stored in `data/`.
- **Factual Extractive Summaries**: Summaries are generated using sentence extraction (3–5 sentences, 50–100 words) from original text, preserving names, numbers, and dates.
- **Attribution**: Every story card clearly displays the source publisher name, domain, relative publication time, and an explicit "Read Full Story →" button opening the original publisher URL.
