"""Google News RSS Collector and Deduplication Engine.

Discovers stories across 1,000 prompts, parses Google News RSS, calculates
freshness (age_minutes), normalizes URLs, detects duplicates using Levenshtein/Jaccard
similarity, and groups major stories under story_group_id.
"""

import datetime
import hashlib
import json
import logging
import re
import urllib.parse
from typing import Dict, List, Optional, Tuple, Any
from dateutil import parser as date_parser
import feedparser
import requests

from backend import config

logger = logging.getLogger(__name__)


def load_prompts(prompts_path=config.PROMPTS_FILE) -> Dict[str, Any]:
    """Load the prompts database from JSON."""
    with open(prompts_path, "r", encoding="utf-8") as f:
        return json.load(f)


def select_prompts_for_execution(
    prompts_data: Dict[str, Any],
    mode: str = "ROTATION",
    rotation_index: int = 0
) -> List[Tuple[str, str, str]]:
    """
    Selects prompts according to execution mode:
      - 'PRIORITY': Only priority categories (Top News, Breaking, Politics, Economy, Tech, Weather).
      - 'ROTATION': A slice of 15-20 categories based on current hour/rotation.
      - 'FULL': All 100 categories.

    Returns list of tuples: (category_id, category_name, prompt_query)
    """
    categories = prompts_data.get("categories", [])
    selected = []

    if mode == "PRIORITY":
        target_ids = set(config.PRIORITY_CATEGORY_IDS)
        for cat in categories:
            if cat["id"] in target_ids:
                # Pick top 2 prompts per category to keep execution fast and fresh
                for p in cat.get("prompts", [])[:2]:
                    selected.append((cat["id"], cat["name"], p))

    elif mode == "ROTATION":
        batch_size = 15
        total_cats = len(categories)
        start_idx = (rotation_index * batch_size) % total_cats
        end_idx = start_idx + batch_size
        batch_cats = categories[start_idx:end_idx]
        if end_idx > total_cats:
            batch_cats.extend(categories[: end_idx - total_cats])

        # Always include top news and breaking news in rotation
        top_cats = [c for c in categories if c["id"] in ["us_top_news", "us_breaking_news"]]
        for c in top_cats:
            if c not in batch_cats:
                batch_cats.insert(0, c)

        for cat in batch_cats:
            for p in cat.get("prompts", [])[:2]:
                selected.append((cat["id"], cat["name"], p))

    else:  # FULL
        for cat in categories:
            for p in cat.get("prompts", [])[:1]:
                selected.append((cat["id"], cat["name"], p))

    return selected


def clean_query_prompt(prompt: str) -> str:
    """Format search prompt for Google News RSS query."""
    # Remove redundant punctuation
    cleaned = re.sub(r"[^\w\s-]", " ", prompt)
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return cleaned


def normalize_title(title: str) -> str:
    """Normalize a headline for deduplication comparisons."""
    if not title:
        return ""
    # Strip common publisher suffixes like "- CNN", "- The New York Times"
    title = re.sub(r"\s+[-–|]\s+[^-–|]+$", "", title)
    title = title.lower()
    # Keep alphanumeric characters and spaces
    title = re.sub(r"[^a-z0-9\s]", "", title)
    title = re.sub(r"\s+", " ", title).strip()
    # Normalize common synonyms for duplicate detection
    title = re.sub(r"\bfed\b", "federal reserve", title)
    return title


def extract_domain(url: str) -> str:
    """Extract registered domain or hostname from URL."""
    try:
        parsed = urllib.parse.urlparse(url)
        netloc = parsed.netloc.lower()
        if netloc.startswith("www."):
            netloc = netloc[4:]
        return netloc
    except Exception:
        return ""


def clean_canonical_url(url: str) -> str:
    """Strip tracking query parameters (utm_*, ref, etc.) to get canonical URL."""
    if not url:
        return ""
    try:
        parsed = urllib.parse.urlparse(url)
        query_params = urllib.parse.parse_qsl(parsed.query)
        filtered_params = [
            (k, v) for k, v in query_params
            if not k.lower().startswith("utm_") and k.lower() not in ("ref", "fbclid", "gclid", "src")
        ]
        clean_query = urllib.parse.urlencode(filtered_params)
        clean_url = urllib.parse.urlunparse((
            parsed.scheme,
            parsed.netloc.lower(),
            parsed.path,
            parsed.params,
            clean_query,
            ""  # strip fragment
        ))
        return clean_url
    except Exception:
        return url


def calculate_title_similarity(t1: str, t2: str) -> float:
    """Calculate word Jaccard similarity between two normalized headlines."""
    words1 = set(t1.split())
    words2 = set(t2.split())
    if not words1 or not words2:
        return 0.0
    intersection = words1.intersection(words2)
    union = words1.union(words2)
    return len(intersection) / len(union)


def parse_published_date(entry: Any) -> Tuple[str, int]:
    """
    Parse published date from feedparser entry.
    Returns (iso_string, age_minutes).
    """
    now = datetime.datetime.now(datetime.timezone.utc)
    published_dt = None

    for attr in ("published", "updated", "pubDate"):
        if hasattr(entry, attr):
            try:
                published_dt = date_parser.parse(getattr(entry, attr))
                break
            except Exception:
                pass

    if hasattr(entry, "published_parsed") and entry.published_parsed and not published_dt:
        try:
            published_dt = datetime.datetime(
                *entry.published_parsed[:6], tzinfo=datetime.timezone.utc
            )
        except Exception:
            pass

    if not published_dt:
        published_dt = now

    # Ensure timezone aware (UTC)
    if published_dt.tzinfo is None:
        published_dt = published_dt.replace(tzinfo=datetime.timezone.utc)
    else:
        published_dt = published_dt.astimezone(datetime.timezone.utc)

    # Prevent future dates due to server clock skews
    if published_dt > now:
        published_dt = now

    diff = now - published_dt
    age_minutes = max(0, int(diff.total_seconds() / 60))
    iso_str = published_dt.strftime("%Y-%m-%dT%H:%M:%SZ")

    return iso_str, age_minutes


def extract_rss_image(entry: Any) -> Optional[str]:
    """Extract preview image URL safely from RSS enclosure or media:content if present."""
    try:
        # Check media_content
        if hasattr(entry, "media_content") and entry.media_content:
            for item in entry.media_content:
                if isinstance(item, dict) and "url" in item:
                    return item["url"]

        # Check media_thumbnail
        if hasattr(entry, "media_thumbnail") and entry.media_thumbnail:
            for item in entry.media_thumbnail:
                if isinstance(item, dict) and "url" in item:
                    return item["url"]

        # Check enclosures
        if hasattr(entry, "enclosures") and entry.enclosures:
            for enc in entry.enclosures:
                if isinstance(enc, dict) and enc.get("type", "").startswith("image/"):
                    return enc.get("href")
    except Exception:
        pass
    return None


def fetch_google_news_rss(query: str) -> List[Dict[str, Any]]:
    """Fetch and parse Google News RSS for a query."""
    encoded_query = urllib.parse.quote_plus(query)
    rss_url = config.GOOGLE_NEWS_RSS_URL.format(query=encoded_query)

    try:
        # Feedparser handles fetching, but using polite requests headers ensures consistency
        response = requests.get(rss_url, headers={"User-Agent": config.USER_AGENT}, timeout=config.ARTICLE_TIMEOUT)
        if response.status_code != 200:
            logger.warning(f"Google News RSS returned status {response.status_code} for query: {query}")
            return []

        feed = feedparser.parse(response.content)
        items = []
        for entry in feed.entries:
            title = getattr(entry, "title", "").strip()
            link = getattr(entry, "link", "").strip()
            if not title or not link:
                continue

            source_name = "Google News"
            if hasattr(entry, "source") and hasattr(entry.source, "title"):
                source_name = entry.source.title
            elif " - " in title:
                # Extract publisher name from headline suffix
                parts = title.rsplit(" - ", 1)
                source_name = parts[1].strip()
                title = parts[0].strip()

            published_at, age_minutes = parse_published_date(entry)
            image_url = extract_rss_image(entry)

            items.append({
                "title": title,
                "url": link,
                "source": source_name,
                "source_domain": extract_domain(link),
                "published_at": published_at,
                "age_minutes": age_minutes,
                "image_url": image_url,
                "raw_summary": getattr(entry, "summary", ""),
            })

        return items

    except Exception as e:
        logger.error(f"Error fetching Google News RSS for '{query}': {e}")
        return []


def deduplicate_and_rank_stories(stories: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Deduplicates stories:
    - Same canonical URL
    - High title similarity (> config.TITLE_SIMILARITY_THRESHOLD)
    Groups similar coverage under 'story_group_id'.
    Ranks by freshness (age_minutes) and relevance.
    """
    unique_stories = []
    seen_urls = set()
    norm_titles: List[Tuple[str, int]] = []  # (normalized_title, story_index)

    # Sort initially by freshness
    stories.sort(key=lambda s: s.get("age_minutes", 99999))

    for s in stories:
        raw_url = s.get("url", "")
        clean_url = clean_canonical_url(raw_url)
        if clean_url in seen_urls:
            continue

        raw_title = s.get("title", "")
        norm_title = normalize_title(raw_title)
        if not norm_title:
            continue

        # Check title similarity against already accepted stories
        is_duplicate = False
        matched_group_id = None

        for prev_title, prev_idx in norm_titles:
            sim = calculate_title_similarity(norm_title, prev_title)
            if sim >= config.TITLE_SIMILARITY_THRESHOLD:
                is_duplicate = True
                # Link to existing story's group
                existing_story = unique_stories[prev_idx]
                if not existing_story.get("story_group_id"):
                    grp_id = f"grp_{hashlib.md5(prev_title.encode()).hexdigest()[:8]}"
                    existing_story["story_group_id"] = grp_id
                    existing_story["source_count"] = 1
                matched_group_id = existing_story["story_group_id"]
                existing_story["source_count"] = existing_story.get("source_count", 1) + 1
                break

        if is_duplicate:
            # Skip duplicate headline to avoid feed clutter, but keep story grouping tracked
            continue

        # Assign unique ID
        story_id = f"us_{hashlib.md5((clean_url + norm_title).encode()).hexdigest()[:12]}"
        s["id"] = story_id
        s["canonical_url"] = clean_url
        s["normalized_title"] = norm_title
        if not s.get("story_group_id"):
            s["story_group_id"] = None
            s["source_count"] = 1

        seen_urls.add(clean_url)
        norm_titles.append((norm_title, len(unique_stories)))
        unique_stories.append(s)

    return unique_stories
