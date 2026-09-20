import glob
import json
import urllib.request
import time
import re
import sys

try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

json_files = [
    "app/src/main/assets/comedy_malayalam.json",
    "app/src/main/assets/travel_malayalam.json",
    "app/src/main/assets/cookery_malayalam.json",
    "app/src/main/assets/reviews_malayalam.json",
    "app/src/main/assets/trailers_malayalam.json",
    "app/src/main/assets/kids_malayalam.json",
    "app/src/main/assets/tech_malayalam.json",
    "app/src/main/assets/local_iptv_malayalam.json",
    "app/src/main/assets/news_channels.json",
    "app/src/main/assets/tv_channels.json",
    "app/src/main/assets/music_channels.json",
    "app/src/main/assets/devotional_malayalam.json",
    "app/src/main/assets/agriculture_malayalam.json",
    "app/src/main/assets/auto_malayalam.json",
    "app/src/main/assets/health_malayalam.json",
    "app/src/main/assets/interviews_malayalam.json",
    "app/src/main/assets/podcasts_malayalam.json",
    "app/src/main/assets/shortfilms_malayalam.json",
    "app/src/main/assets/trolls_malayalam.json"
]

# Build existing channel avatar cache from channels.json
cache = {}
try:
    with open("app/src/main/assets/channels.json", "r", encoding="utf-8") as f:
        for ch in json.load(f):
            img = ch.get("image", "")
            title = ch.get("title", "").strip().lower()
            args = ch.get("arguments", [])
            cid = ""
            if args and "channel_id=" in args[0]:
                cid = args[0].split("channel_id=")[1].split("&")[0]
            elif args and args[0].startswith("UC") and len(args[0]) == 24:
                cid = args[0]
            if img and "yt3.googleusercontent.com" in img:
                if cid: cache[cid] = img
                if title: cache[title] = img
except Exception as e:
    print(f"Error loading channels.json: {e}")

print(f"Initial cache from channels.json has {len(cache)} entries.")

def fetch_avatar_from_yt(channel_id):
    try:
        req = urllib.request.Request(
            'https://www.youtube.com/youtubei/v1/browse?prettyPrint=false',
            data=json.dumps({
                'context': {
                    'client': {
                        'clientName': 'WEB',
                        'clientVersion': '2.20240101.00.00'
                    }
                },
                'browseId': channel_id
            }).encode('utf-8'),
            headers={
                'Content-Type': 'application/json',
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
            }
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            avatar = data.get('metadata', {}).get('channelMetadataRenderer', {}).get('avatar', {}).get('thumbnails', [{}])[-1].get('url')
            if avatar:
                if avatar.startswith("//"):
                    avatar = "https:" + avatar
                return avatar
    except Exception as e:
        print(f"Error fetching {channel_id}: {e}")
    return None

total_updated = 0

for file_path in json_files:
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            items = json.load(f)
    except Exception as e:
        print(f"Skip {file_path}: {e}")
        continue
    
    file_modified = False
    for item in items:
        cur_img = item.get("image", "")
        title = item.get("title", "").strip()
        args = item.get("arguments", [])
        
        # Check if already a valid YouTube avatar
        if cur_img and "yt3.googleusercontent.com" in cur_img:
            continue
            
        channel_id = None
        if args:
            arg0 = args[0]
            if "channel_id=" in arg0:
                channel_id = arg0.split("channel_id=")[1].split("&")[0]
            elif arg0.startswith("UC") and len(arg0) == 24:
                channel_id = arg0
        
        avatar = None
        if channel_id and channel_id in cache:
            avatar = cache[channel_id]
        elif title.lower() in cache:
            avatar = cache[title.lower()]
        
        if not avatar and channel_id:
            print(f"Fetching from YouTube for {title} ({channel_id})...")
            avatar = fetch_avatar_from_yt(channel_id)
            if avatar:
                cache[channel_id] = avatar
                cache[title.lower()] = avatar
                time.sleep(0.05)
        
        if avatar:
            item["image"] = avatar
            file_modified = True
            total_updated += 1
            print(f"[{file_path}] {title} -> {avatar[:50]}...")
        else:
            print(f"[{file_path}] Could NOT resolve avatar for {title} (args={args})")

    if file_modified:
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(items, f, indent=2, ensure_ascii=False)
        print(f"Saved {file_path}")

print(f"\nFinished! Total updated channel images: {total_updated}")
