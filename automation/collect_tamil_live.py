import urllib.request
import urllib.parse
import re
import json
import time
import os

# Top 100 Tamil News, Live Media, Entertainment, Music, and Religious Queries
news_queries = [
    # --- News & Live Media (Original 50) ---
    "Polimer News", "Puthiyathalaimurai TV", "Thanthi TV", "News7 Tamil", "News18 Tamilnadu",
    "Sun News", "NewsTamil 24X7", "Sathiyam News", "Kalaignar Seithigal", "Malaimurasu TV Seithigal",
    "Jaya Plus", "Captain News", "Raj News Tamil", "News Fast Tamil", "Cauvery News",
    "Lotus News Tamil", "Asianet News Tamil", "IBC Tamil News", "Samayam Tamil", "Oneindia Tamil News",
    "Dinamalar News", "Vikatan News", "Nakkeeran News", "Zee Tamil News", "Puthiya Thalaimurai News",
    "Dinamani News", "Malaimurasu Live", "Tamil News 24x7", "Vendhar TV News", "News Tamil Live",
    "Polimer News Live", "News7 Tamil Live", "News18 Tamil Live", "Sathiyam TV Live", "Sun News Live",
    "Thanthi TV Live", "Puthiyathalaimurai Live", "IBC Tamil Live", "Tamil News Live", "RedPix News Tamil",
    "NewsGlitz Tamil", "Kalaignar TV News", "Velicham TV News", "Moon TV Tamil", "Jeyam TV Tamil",
    "Lotus TV News", "Captain TV News", "Raj News Live", "Win News Tamil", "Tamil Janam News",
    
    # --- General Entertainment, Music & Religious (Another 50) ---
    "Sun TV Live", "Star Vijay Live", "Zee Tamil Live", "KTV Live", "Sun Music Live",
    "Kalaignar TV Live", "Jaya TV Live", "Raj TV Live", "DD Tamil Live", "Imayam TV Live",
    "Makkal TV Live", "Vasanth TV Live", "Mega TV Live", "Captain TV Live", "Sankara TV Tamil Live",
    "Sri Sankara TV Live", "Jaya Max Live", "Jaya Movie Live", "Raj Musix Live", "Raj Digital Plus Live",
    "Adithya TV Live", "Chutti TV Live", "Colors Tamil Live", "Vendhar TV Live", "Puthuyugam TV Live",
    "News7 Tamil HD Live", "Puthiya Thalaimurai HD Live", "Polimer News HD Live", "News18 Tamilnadu HD Live", "Sathiyam TV Live News",
    "MK Tunes Live", "Aathavan TV Live", "7S Music Live", "Blessing TV Live", "Jesus Redeems TV Live",
    "Angel TV Tamil Live", "Dhyanam TV Live", "Madha TV Live", "Matha TV Live", "Salvation TV Tamil Live",
    "Immanuel TV Live", "Nambikkai TV Live", "Sathyavedham TV Live", "Arputhar Yesu TV Live", "Aaseervatham TV Live",
    "Jeevan TV Live", "Discovery Channel Tamil Live", "National Geographic Tamil Live", "History TV18 Tamil Live", "Cartoon Network Tamil Live"
]

def search_youtube_channel(query):
    """Searches YouTube for a channel and extracts its ID and avatar URL"""
    try:
        # Search query restricted to Channels to get the most accurate channel page first
        url = f"https://www.youtube.com/results?search_query={urllib.parse.quote(query)}&sp=EgIQAg%253D%253D"
        req = urllib.request.Request(
            url, 
            headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36'}
        )
        with urllib.request.urlopen(req, timeout=10) as response:
            html = response.read().decode('utf-8')
            
            # Extract Channel ID
            channel_id = None
            match = re.search(r'"browseId":"(UC[a-zA-Z0-9_-]{22})"', html)
            if match:
                channel_id = match.group(1)
            else:
                match = re.search(r'/channel/(UC[a-zA-Z0-9_-]{22})', html)
                if match:
                    channel_id = match.group(1)
            
            if not channel_id:
                return None
                
            # Extract Channel Avatar URL (Logo)
            avatar_url = ""
            # Search for thumbnails in the channel search result JSON structure in HTML
            avatar_matches = re.findall(r'(?:"avatar"|"thumbnail"):\s*\{\s*"thumbnails":\s*\[\s*\{\s*"url":\s*"([^"]+)"', html)
            if avatar_matches:
                avatar_url = avatar_matches[0]
                if not avatar_url.startswith('http'):
                    avatar_url = 'https:' + avatar_url
            
            # Clean up escape backslashes in URL
            if avatar_url:
                avatar_url = avatar_url.replace('\\u0026', '&').replace('\\', '')
            else:
                # Default backup logo (empty string as requested)
                avatar_url = ""

            return {
                "channel_id": channel_id,
                "avatar_url": avatar_url
            }
    except Exception as e:
        print(f"Error searching {query}: {e}")
    return None

def determine_category(name):
    name_lower = name.lower()
    if any(x in name_lower for x in ["blessing", "jesus", "angel", "dhyanam", "madha", "matha", "salvation", "immanuel", "nambikkai", "sathyavedham", "arputhar", "aaseervatham", "sankara", "devotional", "temple"]):
        return "Religious"
    elif any(x in name_lower for x in ["music", "tunes", "musix", "7s"]):
        return "Music"
    elif any(x in name_lower for x in ["sun tv", "vijay", "zee", "ktv", "kalaignar tv", "jaya tv", "raj tv", "imayam", "makkal", "vasanth", "mega", "captain tv", "max", "movie", "digital plus", "adithya", "chutti", "colors", "puthuyugam", "cartoon", "discovery", "geographic", "history"]):
        return "Entertainment"
    else:
        return "News"

def main():
    output_dir = "automation"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        
    output_file = os.path.join(output_dir, "tamil_live_news_youtube.json")
    channels_list = []
    
    print(f"Starting YouTube scrape for {len(news_queries)} Tamil Live News channels...")
    
    for i, q in enumerate(news_queries, 1):
        print(f"[{i}/{len(news_queries)}] Fetching details for: {q}...", end="", flush=True)
        res = search_youtube_channel(q)
        if res:
            channel_id = res["channel_id"]
            avatar_url = res["avatar_url"]
            
            # Unbreakable live stream URL direct to YouTube live stream handler
            # Dynamic link resolving to the current active stream
            live_url = f"https://www.youtube.com/channel/{channel_id}/live"
            
            category = determine_category(q)
            
            channel_obj = {
                "name": q,
                "url": live_url,
                "logo": avatar_url,
                "category": category
            }
            channels_list.append(channel_obj)
            print(f" SUCCESS: Channel ID = {channel_id} (Category: {category})")
        else:
            print(" FAILED")
            
        time.sleep(1.0) # Rate limiting delay
        
    # Save the output to JSON
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(channels_list, f, indent=4, ensure_ascii=False)
        
    print(f"\nDone! Scraped {len(channels_list)} channels successfully.")
    print(f"Output saved to: {output_file}")

if __name__ == "__main__":
    main()
