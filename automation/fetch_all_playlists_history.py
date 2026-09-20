import os
import sys
import json
import xml.etree.ElementTree as ET
import urllib.request
import urllib.parse
from datetime import datetime

# Reconfigure stdout to prevent UnicodeEncodeError in Windows console
try:
    sys.stdout.reconfigure(encoding='utf-8', errors='backslashreplace')
except Exception:
    pass

def parse_rfc3339_to_millis(date_str):
    try:
        dt = datetime.strptime(date_str, "%Y-%m-%dT%H:%M:%SZ")
        return int(dt.timestamp() * 1000)
    except Exception:
        try:
            dt = datetime.strptime(date_str.split('.')[0], "%Y-%m-%dT%H:%M:%S")
            return int(dt.timestamp() * 1000)
        except Exception:
            return 0

def fetch_playlist(playlist_id, show_name, api_key):
    videos = []
    next_page_token = ""
    print(f"\n[START] Fetching: '{show_name}' (ID: {playlist_id})...")
    
    while True:
        url = f"https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId={playlist_id}&maxResults=50&key={api_key}"
        if next_page_token:
            url += f"&pageToken={next_page_token}"
            
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req) as response:
                res_data = json.loads(response.read().decode('utf-8'))
                items = res_data.get('items', [])
                for item in items:
                    snippet = item.get('snippet', {})
                    resource_id = snippet.get('resourceId', {})
                    video_id = resource_id.get('videoId', '')
                    title = snippet.get('title', '')
                    description = snippet.get('description', '')
                    published_at = snippet.get('publishedAt', '')
                    
                    if not video_id or title == "Private video" or title == "Deleted video":
                        continue
                        
                    thumbnails = snippet.get('thumbnails', {})
                    thumb_url = ''
                    for size in ['maxres', 'standard', 'high', 'medium', 'default']:
                        if size in thumbnails:
                            thumb_url = thumbnails[size].get('url', '')
                            break
                            
                    pub_millis = parse_rfc3339_to_millis(published_at)
                    
                    video_item = {
                        "video_id": video_id,
                        "title": title,
                        "thumb_url": thumb_url,
                        "pubDate": published_at,
                        "pubDateMillis": pub_millis,
                        "channelId": playlist_id,
                        "channelName": show_name,
                        "description": description,
                        "link": f"https://www.youtube.com/watch?v={video_id}",
                        "fetchedAt": int(datetime.now().timestamp() * 1000)
                    }
                    videos.append(video_item)
                
                next_page_token = res_data.get('nextPageToken', '')
                if not next_page_token:
                    break
        except Exception as e:
            print(f"Error fetching {show_name}: {e}")
            break
            
    # Save file
    os.makedirs(r'app/src/main/assets/playlists', exist_ok=True)
    output_file = os.path.join(r'app/src/main/assets/playlists', f"{playlist_id}.json")
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(videos, f, indent=4, ensure_ascii=False)
        print(f"[SUCCESS] Saved {len(videos)} videos to {output_file}")
    except Exception as e:
        print(f"[ERROR] Failed to save {show_name}: {e}")

def main():
    # 1. Load API Key
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
        return

    # 2. Load shows config
    shows_path = r'app/src/main/assets/top_shows_malayalam.json'
    if not os.path.exists(shows_path):
        shows_path = r'app/src/main/assets/popular_shows.json'
    if not os.path.exists(shows_path):
        print(f"File not found: {shows_path}")
        return

    try:
        with open(shows_path, 'r', encoding='utf-8') as f:
            shows = json.load(f)
    except Exception as e:
        print(f"Error loading popular_shows.json: {e}")
        return

    print(f"Found {len(shows)} TV shows to process.")

    # 3. Process each show
    for idx, show in enumerate(shows, 1):
        title = show.get('title', 'Unknown Show')
        args = show.get('arguments', [])
        if not args:
            continue
            
        feed_url = args[0]
        # Extract playlist_id from URL
        playlist_id = ""
        parsed_url = urllib.parse.urlparse(feed_url)
        query_params = urllib.parse.parse_qs(parsed_url.query)
        if 'playlist_id' in query_params:
            playlist_id = query_params['playlist_id'][0]
            
        if not playlist_id:
            # Fallback string partition
            if "playlist_id=" in feed_url:
                playlist_id = feed_url.split("playlist_id=")[1]
                if "&" in playlist_id:
                    playlist_id = playlist_id.split("&")[0]
                    
        if playlist_id:
            print(f"\n--- Progress: {idx}/{len(shows)} ---")
            fetch_playlist(playlist_id, title, api_key)
        else:
            print(f"Skipping {title} - No playlist_id found in URL: {feed_url}")

    print("\n[COMPLETE] All playlist histories fetched successfully!")

if __name__ == '__main__':
    main()
