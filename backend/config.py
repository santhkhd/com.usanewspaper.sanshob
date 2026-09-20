"""Configuration for Free US News Aggregator & Summarizer."""

import os
from pathlib import Path

# Base Paths
BASE_DIR = Path(__file__).resolve().parent.parent
BACKEND_DIR = BASE_DIR / "backend"
DATA_DIR = BASE_DIR / "data"

PROMPTS_FILE = BACKEND_DIR / "prompts.json"
OUTPUT_NEWS_FILE = DATA_DIR / "us_news.json"
OUTPUT_CATEGORIES_FILE = DATA_DIR / "categories.json"

# Collection Limits
MAX_STORIES_PER_CATEGORY = 20
MAX_TOTAL_STORIES = 500
ARTICLE_TIMEOUT = 15
SUMMARY_SENTENCES = 4  # 3 to 5 sentences
MIN_SUMMARY_WORDS = 40
MAX_SUMMARY_WORDS = 100
MAX_ARTICLE_LENGTH = 30000
REQUEST_DELAY = 1.0  # polite delay in seconds
MAX_WORKERS = 5

# Data Retention
DATA_RETENTION_HOURS = 48

# Duplicate Detection
TITLE_SIMILARITY_THRESHOLD = 0.75

# Freshness Prioritization Weights
FRESHNESS_WEIGHTS = {
    "1h": 1.0,
    "3h": 0.85,
    "6h": 0.70,
    "12h": 0.55,
    "24h": 0.40,
    "48h": 0.20,
}

# Google News RSS Base URL
GOOGLE_NEWS_RSS_URL = "https://news.google.com/rss/search?q={query}&hl=en-US&gl=US&ceid=US:en"

# Priority Categories (for PRIORITY execution mode)
PRIORITY_CATEGORY_IDS = [
    "us_top_news",
    "us_breaking_news",
    "white_house",
    "us_president",
    "congress",
    "us_economy",
    "inflation",
    "federal_reserve",
    "stock_market",
    "business",
    "technology",
    "artificial_intelligence",
    "weather",
    "storms",
    "health",
    "science",
    "national_security",
    "sports",
]

# User-Agent for ethical requests
USER_AGENT = (
    "Mozilla/5.0 (compatible; USNewsAggregator/1.0; "
    "+https://github.com/free-us-news-aggregator)"
)
