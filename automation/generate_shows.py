import os
import xml.etree.ElementTree as ET
import urllib.request
import json

# 1. Load API Key from strings.xml
strings_path = r'app/src/main/res/values/strings.xml'
api_key = ''
try:
    tree = ET.parse(strings_path)
    root = tree.getroot()
    for string_elem in root.findall('string'):
        if string_elem.attrib.get('name') == 'youtube_api_key':
            api_key = string_elem.text.strip()
            break
except Exception as e:
    print("Error parsing strings.xml:", e)

if not api_key:
    print("Could not find youtube_api_key in strings.xml!")
    exit(1)

print("Found API Key:", api_key[:10] + "...")

# 2. Query Playlists for Vijay TV and Sun TV (Zee Tamil/Colors removed)
channels = {
    "Vijay TV": "UCvrhwpnp2DHYQ1CbXby9ypQ",
    "Sun TV": "UCBnxEdpoZwstJqC1yZpOjRA"
}

shows = []

for name, channel_id in channels.items():
    print(f"Fetching playlists for {name} ({channel_id})...")
    # Fetch up to 50 playlists per channel
    api_url = f"https://www.googleapis.com/youtube/v3/playlists?part=snippet&channelId={channel_id}&maxResults=50&key={api_key}"
    try:
        req = urllib.request.Request(api_url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode('utf-8'))
            items = data.get('items', [])
            print(f"Loaded {len(items)} playlists.")
            for item in items:
                snippet = item.get('snippet', {})
                title = snippet.get('title', '')
                playlist_id = item.get('id', '')
                
                # Basic cleaning: filter out generic promotions or too short names
                if not title or len(title) < 3:
                    continue
                if "promo" in title.lower() and "serial" not in title.lower():
                    continue
                if "teaser" in title.lower() or "short" in title.lower() or "shorts" in title.lower():
                    continue
                
                # Extract high res thumbnail
                thumbnails = snippet.get('thumbnails', {})
                logo_url = ''
                for size in ['high', 'medium', 'default']:
                    if size in thumbnails:
                        logo_url = thumbnails[size].get('url', '')
                        break
                
                # Format into RSS source object
                show_item = {
                    "title": f"[{name}] {title}",
                    "provider": "rss",
                    "arguments": [
                        f"https://www.youtube.com/feeds/videos.xml?playlist_id={playlist_id}"
                    ],
                    "link": f"https://www.youtube.com/playlist?list={playlist_id}",
                    "logo": logo_url
                }
                shows.append(show_item)
    except Exception as e:
        print(f"Error fetching playlists for {name}: {e}")

# 3. Limit to 100 and write output
shows = shows[:100]
print(f"Total structured TV shows compiled: {len(shows)}")

output_path = r'app/src/main/assets/popular_shows.json'
try:
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(shows, f, indent=4, ensure_ascii=False)
    print(f"Successfully generated {output_path}!")
except Exception as e:
    print("Error saving output file:", e)
