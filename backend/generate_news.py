"""Main News Generation Pipeline.

Coordinates:
  1. Discovery & prompt selection (PRIORITY, ROTATION, FULL modes)
  2. Google News RSS polling
  3. Deduplication & freshness calculation
  4. Polite article text extraction
  5. Zero-cost extractive factual summarization
  6. Data retention pruning (24-48 hours)
  7. Emits data/us_news.json and data/categories.json
"""

import argparse
import concurrent.futures
import datetime
import json
import logging
import os
import sys
import time
from typing import Dict, List, Any

# Ensure backend package is importable
sys.path.insert(0, str(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from backend import config
from backend import fetch_news
from backend import extract_article
from backend import summarize

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("generate_news")


def get_category_icon(category_id: str) -> str:
    """Map category IDs to clean Material / UI icon names."""
    icon_map = {
        "us_top_news": "flag",
        "us_breaking_news": "bolt",
        "white_house": "account_balance",
        "us_president": "person",
        "congress": "gavel",
        "senate": "account_balance",
        "house": "domain",
        "supreme_court": "balance",
        "federal_government": "corporate_fare",
        "us_economy": "attach_money",
        "inflation": "trending_up",
        "federal_reserve": "savings",
        "interest_rates": "percent",
        "jobs": "work",
        "unemployment": "people",
        "stock_market": "show_chart",
        "dow_jones": "trending_up",
        "sp500": "insert_chart",
        "nasdaq": "analytics",
        "business": "business_center",
        "corporate_earnings": "receipt_long",
        "technology": "laptop",
        "artificial_intelligence": "psychology",
        "semiconductors": "memory",
        "chips_act": "developer_board",
        "cybersecurity": "security",
        "weather": "wb_sunny",
        "storms": "thunderstorm",
        "hurricanes": "cyclone",
        "tornadoes": "tornado",
        "flooding": "flood",
        "wildfires": "local_fire_department",
        "earthquakes": "vibration",
        "health": "medical_services",
        "cdc": "health_and_safety",
        "fda": "science",
        "science": "biotech",
        "space": "rocket_launch",
        "nasa": "public",
        "spacex": "flight_takeoff",
        "crime": "shield",
        "police": "local_police",
        "fbi": "verified_user",
        "justice_department": "gavel",
        "immigration": "travel_explore",
        "border": "fence",
        "national_security": "security",
        "military": "military_tech",
        "pentagon": "domain",
        "foreign_policy": "public",
        "sports": "sports_baseball",
        "nfl": "sports_football",
        "nba": "sports_basketball",
        "mlb": "sports_baseball",
        "nhl": "sports_hockey",
        "entertainment": "movie",
        "movies": "theaters",
        "music": "music_note",
        "social_media": "share",
        "privacy": "lock",
        "environment": "eco",
        "climate": "thermostat",
        "agriculture": "agriculture",
        "food": "restaurant",
        "travel": "flight",
        "tourism": "luggage",
        "local_news": "location_city",
        "daily_morning": "wb_twilight",
        "daily_evening": "bedtime",
        "news_roundup": "newspaper",
    }
    return icon_map.get(category_id, "newspaper")


def generate_categories_json(prompts_data: Dict[str, Any]) -> Dict[str, Any]:
    """Generate categories.json from prompts definitions."""
    categories_list = []
    for cat in prompts_data.get("categories", []):
        cat_id = cat["id"]
        categories_list.append({
            "id": cat_id,
            "name": cat["name"],
            "icon": get_category_icon(cat_id)
        })
    return {"categories": categories_list}


def process_single_article(
    story: Dict[str, Any],
    summarizer_inst: summarize.BaseSummarizer,
    delay: float = 0.5
) -> Dict[str, Any]:
    """Extract and summarize a single story."""
    url = story.get("url", "")
    title = story.get("title", "")

    # Attempt polite extraction
    extract_res = extract_article.extract_article_content(url, delay=delay)
    article_text = extract_res.get("text", "")
    resolved_url = extract_res.get("resolved_url", url)

    # Use resolved original URL if available
    story["url"] = resolved_url

    # Summarize article body
    if article_text and len(article_text.split()) >= 30:
        summary_text = summarizer_inst.summarize(article_text, headline=title)
        story["summary"] = summary_text
        story["summary_type"] = summarizer_inst.summary_type
    else:
        # Fallback to RSS snippet or title explanation
        raw_summary = story.get("raw_summary", "")
        # Clean HTML in raw_summary if present
        clean_raw = extract_article.BeautifulSoup(raw_summary, "html.parser").get_text(strip=True) if raw_summary else ""
        if len(clean_raw.split()) >= 15:
            story["summary"] = clean_raw
            story["summary_type"] = "rss_snippet"
        else:
            story["summary"] = f"Developing story regarding: {title}. Read the full report from {story.get('source')} for comprehensive coverage."
            story["summary_type"] = "bulletin"

    # Remove temporary raw fields
    story.pop("raw_summary", None)
    story.pop("normalized_title", None)
    story.pop("canonical_url", None)

    return story


def run_pipeline(
    mode: str = "ROTATION",
    max_stories: int = config.MAX_TOTAL_STORIES,
    dry_run: bool = False
) -> Dict[str, Any]:
    """Run the end-to-end news aggregation and summarization pipeline."""
    start_time = time.time()
    logger.info(f"Starting news collection pipeline in mode: {mode}")

    # Ensure output directories exist
    os.makedirs(config.DATA_DIR, exist_ok=True)

    # 1. Load prompts
    prompts_data = fetch_news.load_prompts(config.PROMPTS_FILE)
    categories_data = generate_categories_json(prompts_data)

    # Write categories.json
    if not dry_run:
        with open(config.OUTPUT_CATEGORIES_FILE, "w", encoding="utf-8") as f:
            json.dump(categories_data, f, indent=2, ensure_ascii=False)
        logger.info(f"Updated {config.OUTPUT_CATEGORIES_FILE} with {len(categories_data['categories'])} categories.")

    # 2. Select prompts for this run
    current_hour = datetime.datetime.now(datetime.timezone.utc).hour
    selected_prompts = fetch_news.select_prompts_for_execution(
        prompts_data, mode=mode, rotation_index=current_hour
    )
    logger.info(f"Selected {len(selected_prompts)} search prompts for execution.")

    # 3. Discover stories across RSS feeds
    raw_discovered: List[Dict[str, Any]] = []
    # Limit number of prompts fetched per run to keep workflow snappy
    max_prompts_to_run = min(len(selected_prompts), 35 if mode != "FULL" else len(selected_prompts))

    for idx, (cat_id, cat_name, prompt) in enumerate(selected_prompts[:max_prompts_to_run]):
        query = fetch_news.clean_query_prompt(prompt)
        rss_items = fetch_news.fetch_google_news_rss(query)
        for item in rss_items[:config.MAX_STORIES_PER_CATEGORY]:
            item["category_id"] = cat_id
            item["category"] = cat_name
            raw_discovered.append(item)

        time.sleep(0.2)  # brief pause between search queries

    logger.info(f"Discovered {len(raw_discovered)} raw items from Google News RSS.")

    # 4. Deduplicate and rank by freshness
    deduped_stories = fetch_news.deduplicate_and_rank_stories(raw_discovered)
    logger.info(f"{len(deduped_stories)} unique stories after deduplication and ranking.")

    # Limit to max_stories
    target_stories = deduped_stories[:max_stories]

    # 5. Extract and summarize stories with concurrent worker pool
    summarizer_inst = summarize.get_summarizer("extractive")
    processed_stories = []

    logger.info(f"Extracting and summarizing {len(target_stories)} stories (workers={config.MAX_WORKERS})...")
    with concurrent.futures.ThreadPoolExecutor(max_workers=config.MAX_WORKERS) as executor:
        future_to_story = {
            executor.submit(process_single_article, story, summarizer_inst, 0.2): story
            for story in target_stories
        }
        for future in concurrent.futures.as_completed(future_to_story):
            try:
                completed_story = future.result()
                processed_stories.append(completed_story)
            except Exception as e:
                logger.error(f"Error processing story: {e}")

    # 6. Retention merge: Load existing data/us_news.json and merge recent stories
    existing_stories = []
    now_utc = datetime.datetime.now(datetime.timezone.utc)
    cutoff_time = now_utc - datetime.timedelta(hours=config.DATA_RETENTION_HOURS)

    if os.path.exists(config.OUTPUT_NEWS_FILE):
        try:
            with open(config.OUTPUT_NEWS_FILE, "r", encoding="utf-8") as f:
                old_data = json.load(f)
                for s in old_data.get("stories", []):
                    # Check age
                    pub_str = s.get("published_at", "")
                    try:
                        pub_dt = fetch_news.date_parser.parse(pub_str)
                        if pub_dt.tzinfo is None:
                            pub_dt = pub_dt.replace(tzinfo=datetime.timezone.utc)
                        if pub_dt >= cutoff_time:
                            # Recalculate age_minutes
                            age_m = max(0, int((now_utc - pub_dt).total_seconds() / 60))
                            s["age_minutes"] = age_m
                            existing_stories.append(s)
                    except Exception:
                        pass
        except Exception as e:
            logger.warning(f"Could not load previous news for retention merge: {e}")

    # Combine newly processed with existing, avoiding duplicates by story ID or title
    all_stories = list(processed_stories)
    seen_ids = {s["id"] for s in all_stories if "id" in s}
    seen_titles = {fetch_news.normalize_title(s["title"]) for s in all_stories if "title" in s}

    for s in existing_stories:
        if s.get("id") not in seen_ids and fetch_news.normalize_title(s.get("title", "")) not in seen_titles:
            all_stories.append(s)
            seen_ids.add(s["id"])

    # Final sort by freshness (age_minutes ascending)
    all_stories.sort(key=lambda s: s.get("age_minutes", 99999))
    final_stories = all_stories[:config.MAX_TOTAL_STORIES]

    # Format JSON payload
    payload = {
        "version": "1.0",
        "generated_at": now_utc.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "country": "US",
        "language": "en-US",
        "stories": final_stories
    }

    if not dry_run:
        with open(config.OUTPUT_NEWS_FILE, "w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2, ensure_ascii=False)
        logger.info(f"Saved {len(final_stories)} stories to {config.OUTPUT_NEWS_FILE}")

    elapsed = time.time() - start_time
    logger.info(f"Pipeline completed successfully in {elapsed:.2f} seconds.")
    return payload


def main():
    parser = argparse.ArgumentParser(description="Generate Free US News JSON via Google News RSS & Summarizer")
    parser.add_argument(
        "--mode",
        choices=["PRIORITY", "ROTATION", "FULL"],
        default="ROTATION",
        help="Prompt selection mode (default: ROTATION)"
    )
    parser.add_argument(
        "--max-stories",
        type=int,
        default=config.MAX_TOTAL_STORIES,
        help=f"Maximum stories to generate (default: {config.MAX_TOTAL_STORIES})"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Run without saving files to disk"
    )
    args = parser.parse_args()
    run_pipeline(mode=args.mode, max_stories=args.max_stories, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
