import json
import sys
try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

built_in = [
    'comedy_malayalam.json',
    'travel_malayalam.json',
    'cookery_malayalam.json',
    'reviews_malayalam.json',
    'trailers_malayalam.json',
    'kids_malayalam.json',
    'tech_malayalam.json',
    'local_iptv_malayalam.json',
    'news_channels.json',
    'tv_channels.json',
    'music_channels.json',
    'devotional_malayalam.json',
    'channels.json'
]

for name in built_in:
    path = f'app/src/main/assets/{name}'
    with open(path, 'r', encoding='utf-8') as f:
        items = json.load(f)
    print(f"=== {name} ({len(items)} items) ===")
    for idx, it in enumerate(items[:4]):
        img = it.get('image', '')
        t = it.get('title', '')
        print(f"  [{idx+1}] {t[:25]} -> {img[:50]}...")
