#!/usr/bin/env python3
"""
Automated Malayalam OTT Release Sync Script
Fetches recent Malayalam releases and their active OTT watch providers (India) via TMDB API,
outputting the structured `ott_releases_malayalam.json` ready for live remote cloud hosting.

Usage:
    python sync_malayalam_ott.py [--api-key YOUR_TMDB_API_KEY]
"""

import os
import sys
import json
import urllib.request
import urllib.parse
from datetime import datetime

# Optional TMDB API Key (Environment variable or CLI argument)
TMDB_API_KEY = os.environ.get("TMDB_API_KEY", "")

DISCOVER_URL = "https://api.themoviedb.org/3/discover/movie"
MOVIE_DETAIL_URL = "https://api.themoviedb.org/3/movie"
IMAGE_BASE = "https://image.tmdb.org/t/p/w780"

# Known Provider Maps in India
PROVIDER_LINKS = {
    "Amazon Prime Video": "https://www.primevideo.com",
    "Disney Plus Hotstar": "https://www.hotstar.com",
    "Netflix": "https://www.netflix.com",
    "Sony Liv": "https://www.sonyliv.com",
    "Zee5": "https://www.zee5.com",
    "Sun Nxt": "https://www.sunnxt.com",
    "Manorama MAX": "https://www.manoramamax.com",
    "Saina Play": "https://www.sainaplay.com",
    "JioCinema": "https://www.jiocinema.com",
    "Apple TV": "https://tv.apple.com"
}

def fetch_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "MalayalamOTTBot/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"Error fetching {url}: {e}", file=sys.stderr)
        return None

def sync_ott_releases(api_key, output_path):
    if not api_key:
        print("TMDB_API_KEY is required to fetch live updates from TMDB. Pass via --api-key or TMDB_API_KEY env.", file=sys.stderr)
        return False

    current_year = datetime.now().year
    params = {
        "api_key": api_key,
        "with_original_language": "ml",
        "region": "IN",
        "sort_by": "primary_release_date.desc",
        "primary_release_date.gte": f"{current_year - 1}-01-01",
        "vote_count.gte": "2"
    }

    url = f"{DISCOVER_URL}?{urllib.parse.urlencode(params)}"
    data = fetch_json(url)
    if not data or "results" not in data:
        print("Failed to fetch discovery results", file=sys.stderr)
        return False

    movies_output = []
    results = data.get("results", [])[:30]

    for m in results:
        m_id = m.get("id")
        title = m.get("title", "")
        overview = m.get("overview", "")
        release_date = m.get("release_date", "")
        poster_path = m.get("poster_path", "")
        vote_avg = m.get("vote_average", 0.0)
        year_str = release_date[:4] if release_date else str(current_year)

        # Fetch watch providers
        prov_url = f"{MOVIE_DETAIL_URL}/{m_id}/watch/providers?api_key={api_key}"
        prov_data = fetch_json(prov_url)
        in_providers = prov_data.get("results", {}).get("IN", {}) if prov_data else {}
        
        flatrate = in_providers.get("flatrate", [])
        ott_name = ""
        ott_url = ""

        if flatrate:
            best_provider = flatrate[0].get("provider_name", "")
            ott_name = best_provider
            ott_url = PROVIDER_LINKS.get(best_provider, in_providers.get("link", "https://www.justwatch.com/in/search?q=" + urllib.parse.quote(title)))
        elif in_providers.get("rent"):
            best_provider = in_providers["rent"][0].get("provider_name", "")
            ott_name = f"{best_provider} (Rent)"
            ott_url = PROVIDER_LINKS.get(best_provider, in_providers.get("link", ""))

        # Fetch credits for Cast & Director
        credits_url = f"{MOVIE_DETAIL_URL}/{m_id}/credits?api_key={api_key}"
        credits_data = fetch_json(credits_url)
        cast_list = []
        director_name = ""

        if credits_data:
            for actor in credits_data.get("cast", [])[:4]:
                cast_list.append(actor.get("name", ""))
            for crew in credits_data.get("crew", []):
                if crew.get("job") == "Director":
                    director_name = crew.get("name", "")
                    break

        genre_label = f"Streaming on {ott_name}" if ott_name else "Malayalam Cinema"
        display_title = f"{title} ({ott_name})" if ott_name else title
        poster_url = f"{IMAGE_BASE}{poster_path}" if poster_path else "https://img.icons8.com/color/96/clapperboard.png"

        item = {
            "title": display_title,
            "year": year_str,
            "rating": str(round(vote_avg, 1)) if vote_avg > 0 else "N/A",
            "genre": genre_label,
            "plot": overview if overview else f"Malayalam film starring {', '.join(cast_list[:2])}.",
            "image": poster_url,
            "cast": cast_list,
            "director": director_name,
            "runtime": "140 min",
            "provider": "movies",
            "ott_url": ott_url if ott_url else f"https://www.justwatch.com/in/search?q={urllib.parse.quote(title + ' ' + year_str)}"
        }
        movies_output.append(item)

    if movies_output:
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(movies_output, f, indent=2, ensure_ascii=False)
        print(f"Successfully synced {len(movies_output)} Malayalam OTT releases to {output_path}")
        return True
    return False

if __name__ == "__main__":
    key = TMDB_API_KEY
    if len(sys.argv) > 1 and "--api-key" in sys.argv:
        idx = sys.argv.index("--api-key")
        if idx + 1 < len(sys.argv):
            key = sys.argv[idx + 1]

    target = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "ott_releases_malayalam.json")
    sync_ott_releases(key, target)
