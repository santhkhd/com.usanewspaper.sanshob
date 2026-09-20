import json
import urllib.request
import re
import sys

sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/assets/live_tv_malayalam.json', 'r', encoding='utf-8') as f:
    channels = json.load(f)

for item in channels:
    title = item.get('title', '')
    if "DD Malayalam" in title:
        try:
            req = urllib.request.Request('https://www.youtube.com/@DDMalayalam', headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
            with urllib.request.urlopen(req, timeout=10) as resp:
                html = resp.read().decode('utf-8', errors='ignore')
                m = re.search(r'"avatar":\s*\{\s*"thumbnails":\s*\[\{"url":\s*"([^"]+)"', html)
                if m:
                    u = m.group(1).replace(r'\u0026', '&')
                    if u.startswith('//'):
                        u = 'https:' + u
                    item['image'] = u
                    print(f"Found avatar for {title} -> {u}")
        except Exception as e:
            print(f"Error {title}: {e}")

with open('app/src/main/assets/live_tv_malayalam.json', 'w', encoding='utf-8') as f:
    json.dump(channels, f, indent=2, ensure_ascii=False)

print("Finished!")
