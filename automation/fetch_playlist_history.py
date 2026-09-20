import sys
import os
import json
import xml.etree.ElementTree as ET
import urllib.request
import urllib.parse
from datetime import datetime

def parse_rfc3339_to_millis(date_str):
    try:
        # e.g., 2026-06-25T13:45:00Z
        dt = datetime.strptime(date_str, "%Y-%m-%dT%H:%M:%SZ")
        return int(dt.timestamp() * 1000)
    except Exception:
        try:
            dt = datetime.strptime(date_str.split('.')[0], "%Y-%m-%dT%H:%M:%S")
            return int(dt.timestamp() * 1000)
        except Exception:
            return 0

def main():
    if len(sys.argv) < 2:
        print("Usage: python fetch_playlist_history.py <PLAYLIST_ID> [SHOW_NAME]")
        sys.exit(1)
        
    playlist_id = sys.argv[1].strip()
    show_name = sys.argv[2].strip() if len(sys.argv) > 2 else "YouTube Show"
    
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
        sys.exit(1)

    print(f"Using API Key: {api_key[:10]}...")
    print(f"Fetching all videos for playlist: {playlist_id}...")

    videos = []
    next_page_token = ""
    
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
                        
                    # Extract high-res thumbnail
                    thumbnails = snippet.get('thumbnails', {})
                    thumb_url = ''
                    for size in ['maxres', 'standard', 'high', 'medium', 'default']:
                        if size in thumbnails:
                            thumb_url = thumbnails[size].get('url', '')
                            break
                            
                    # Calculate millis
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
                print(f"Retrieved {len(items)} items... Cumulative count: {len(videos)}")
                if not next_page_token:
                    break
        except Exception as e:
            print(f"Error fetching playlist items: {e}")
            break
            
    # Write output to local assets path
    os.makedirs(r'app/src/main/assets/playlists', exist_ok=True)
    output_file = os.path.join(r'app/src/main/assets/playlists', f"{playlist_id}.json")
    
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(videos, f, indent=4, ensure_ascii=False)
        print(f"Successfully saved {len(videos)} videos to {output_file}!")
        print("Deploy this file to your remote server inside 'playlists/' folder so it matches!")
    except Exception as e:
        print("Failed to save output:", e)

if __name__ == '__main__':
    main()
