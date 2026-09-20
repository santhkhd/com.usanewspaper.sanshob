import os
import json
import re

assets_dir = "app/src/main/assets"
states_dir = os.path.join(assets_dir, "states")
os.makedirs(states_dir, exist_ok=True)

# Load existing 145 newspapers
raw_path = os.path.join(assets_dir, "usa_states_newspapers.json")
with open(raw_path, "r", encoding="utf-8") as f:
    existing_items = json.load(f)

# Save all states backup/complete list
all_states_path = os.path.join(states_dir, "all_states.json")
with open(all_states_path, "w", encoding="utf-8") as f:
    json.dump(existing_items, f, indent=2, ensure_ascii=False)

# Group by state
state_groups = {}
for item in existing_items:
    state_name = item.get("year", "Unknown").strip()
    if not state_name or state_name == "Unknown":
        # Fallback to genre or title check
        state_name = "United States"
    state_groups.setdefault(state_name, []).append(item)

# US Regions mapping
REGION_MAP = {
    "Connecticut": "Northeast", "Maine": "Northeast", "Massachusetts": "Northeast",
    "New Hampshire": "Northeast", "Rhode Island": "Northeast", "Vermont": "Northeast",
    "New Jersey": "Mid-Atlantic", "New York": "Mid-Atlantic", "Pennsylvania": "Mid-Atlantic",
    "Illinois": "Midwest", "Indiana": "Midwest", "Michigan": "Midwest", "Ohio": "Midwest",
    "Wisconsin": "Midwest", "Iowa": "Midwest", "Kansas": "Midwest", "Minnesota": "Midwest",
    "Missouri": "Midwest", "Nebraska": "Midwest", "North Dakota": "Midwest", "South Dakota": "Midwest",
    "Delaware": "South", "Florida": "South", "Georgia": "South", "Maryland": "South",
    "North Carolina": "South", "South Carolina": "South", "Virginia": "South",
    "District of Columbia": "Capital Region", "West Virginia": "South", "Alabama": "South",
    "Kentucky": "South", "Mississippi": "South", "Tennessee": "South", "Arkansas": "South",
    "Louisiana": "South", "Oklahoma": "South", "Texas": "South",
    "Arizona": "Mountain West", "Colorado": "Mountain West", "Idaho": "Mountain West",
    "Montana": "Mountain West", "Nevada": "Mountain West", "New Mexico": "Mountain West",
    "Utah": "Mountain West", "Wyoming": "Mountain West",
    "Alaska": "Pacific", "California": "Pacific", "Hawaii": "Pacific",
    "Oregon": "Pacific", "Washington": "Pacific"
}

# State postal codes for clean badges / icons
STATE_CODES = {
    "Alabama": "AL", "Alaska": "AK", "Arizona": "AZ", "Arkansas": "AR",
    "California": "CA", "Colorado": "CO", "Connecticut": "CT", "Delaware": "DE",
    "District of Columbia": "DC", "Florida": "FL", "Georgia": "GA", "Hawaii": "HI",
    "Idaho": "ID", "Illinois": "IL", "Indiana": "IN", "Iowa": "IA",
    "Kansas": "KS", "Kentucky": "KY", "Louisiana": "LA", "Maine": "ME",
    "Maryland": "MD", "Massachusetts": "MA", "Michigan": "MI", "Minnesota": "MN",
    "Mississippi": "MS", "Missouri": "MO", "Montana": "MT", "Nebraska": "NE",
    "Nevada": "NV", "New Hampshire": "NH", "New Jersey": "NJ", "New Mexico": "NM",
    "New York": "NY", "North Carolina": "NC", "North Dakota": "ND", "Ohio": "OH",
    "Oklahoma": "OK", "Oregon": "OR", "Pennsylvania": "PA", "Rhode Island": "RI",
    "South Carolina": "SC", "South Dakota": "SD", "Tennessee": "TN", "Texas": "TX",
    "Utah": "UT", "Vermont": "VT", "Virginia": "VA", "Washington": "WA",
    "West Virginia": "WV", "Wisconsin": "WI", "Wyoming": "WY"
}

state_category_index = []

# First card: All 50 States
state_category_index.append({
    "title": "🇺🇸 All 50 States (All Newspapers)",
    "provider": "overview",
    "arguments": ["states/all_states.json"],
    "image": "https://img.icons8.com/color/144/usa.png",
    "year": "50 States + DC",
    "genre": f"{len(existing_items)} Newspapers",
    "rating": "All States"
})

# Sort state names alphabetically
sorted_states = sorted(state_groups.keys())

for state_name in sorted_states:
    papers = state_groups[state_name]
    slug = re.sub(r'[^a-zA-Z0-9]+', '_', state_name).strip('_').lower()
    state_filename = f"{slug}.json"
    state_filepath = os.path.join(states_dir, state_filename)
    
    # Save this state's individual papers list
    with open(state_filepath, "w", encoding="utf-8") as sf:
        json.dump(papers, sf, indent=2, ensure_ascii=False)
    
    code = STATE_CODES.get(state_name, "US")
    region = REGION_MAP.get(state_name, "United States")
    count_str = f"{len(papers)} Newspaper" if len(papers) == 1 else f"{len(papers)} Newspapers"
    
    # Representative image: use primary paper's favicon or clean state badge
    rep_image = papers[0].get("image") if papers and papers[0].get("image") else "https://img.icons8.com/color/96/newspaper.png"
    
    # State category item
    state_category_index.append({
        "title": f"🏛️ {state_name}",
        "provider": "overview",
        "arguments": [f"states/{state_filename}"],
        "image": rep_image,
        "year": region,
        "genre": count_str,
        "rating": code
    })

# Write the new categorized usa_states_newspapers.json
with open(raw_path, "w", encoding="utf-8") as f:
    json.dump(state_category_index, f, indent=2, ensure_ascii=False)

print(f"Generated {len(sorted_states)} individual state JSON files in {states_dir}")
print(f"Updated {raw_path} with {len(state_category_index)} categories (1 All-States + {len(sorted_states)} individual states).")
