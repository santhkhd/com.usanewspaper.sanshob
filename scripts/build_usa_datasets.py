import json
import os

assets_dir = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
os.makedirs(assets_dir, exist_ok=True)

def favicon(domain):
    return f"https://www.google.com/s2/favicons?sz=128&domain={domain}"

# ============================================================================
# 1. USA STATES NEWSPAPERS (All 50 States + DC)
# ============================================================================
state_papers_data = [
    # ALABAMA
    {"title": "The Birmingham News / AL.com", "domain": "al.com", "state": "Alabama", "city": "Birmingham", "year": "Est. 1888"},
    {"title": "Mobile Press-Register", "domain": "al.com/mobile", "state": "Alabama", "city": "Mobile", "year": "Est. 1813"},
    {"title": "Montgomery Advertiser", "domain": "montgomeryadvertiser.com", "state": "Alabama", "city": "Montgomery", "year": "Est. 1829"},

    # ALASKA
    {"title": "Anchorage Daily News", "domain": "adn.com", "state": "Alaska", "city": "Anchorage", "year": "Est. 1946"},
    {"title": "Fairbanks Daily News-Miner", "domain": "newsminer.com", "state": "Alaska", "city": "Fairbanks", "year": "Est. 1903"},

    # ARIZONA
    {"title": "The Arizona Republic", "domain": "azcentral.com", "state": "Arizona", "city": "Phoenix", "year": "Est. 1890"},
    {"title": "Arizona Daily Star", "domain": "tucson.com", "state": "Arizona", "city": "Tucson", "year": "Est. 1877"},

    # ARKANSAS
    {"title": "Arkansas Democrat-Gazette", "domain": "arkansasonline.com", "state": "Arkansas", "city": "Little Rock", "year": "Est. 1819"},

    # CALIFORNIA
    {"title": "Los Angeles Times", "domain": "latimes.com", "state": "California", "city": "Los Angeles", "year": "Est. 1881"},
    {"title": "San Francisco Chronicle", "domain": "sfchronicle.com", "state": "California", "city": "San Francisco", "year": "Est. 1865"},
    {"title": "The San Diego Union-Tribune", "domain": "sandiegouniontribune.com", "state": "California", "city": "San Diego", "year": "Est. 1868"},
    {"title": "The Sacramento Bee", "domain": "sacbee.com", "state": "California", "city": "Sacramento", "year": "Est. 1857"},
    {"title": "The Mercury News", "domain": "mercurynews.com", "state": "California", "city": "San Jose", "year": "Est. 1851"},
    {"title": "Orange County Register", "domain": "ocregister.com", "state": "California", "city": "Anaheim / OC", "year": "Est. 1905"},
    {"title": "The Fresno Bee", "domain": "fresnobee.com", "state": "California", "city": "Fresno", "year": "Est. 1922"},

    # COLORADO
    {"title": "The Denver Post", "domain": "denverpost.com", "state": "Colorado", "city": "Denver", "year": "Est. 1892"},
    {"title": "The Gazette", "domain": "gazette.com", "state": "Colorado", "city": "Colorado Springs", "year": "Est. 1872"},
    {"title": "Daily Camera", "domain": "dailycamera.com", "state": "Colorado", "city": "Boulder", "year": "Est. 1890"},

    # CONNECTICUT
    {"title": "Hartford Courant", "domain": "courant.com", "state": "Connecticut", "city": "Hartford", "year": "Est. 1764"},
    {"title": "New Haven Register", "domain": "nhregister.com", "state": "Connecticut", "city": "New Haven", "year": "Est. 1812"},
    {"title": "Connecticut Post", "domain": "ctpost.com", "state": "Connecticut", "city": "Bridgeport", "year": "Est. 1883"},

    # DELAWARE
    {"title": "The News Journal", "domain": "delawareonline.com", "state": "Delaware", "city": "Wilmington", "year": "Est. 1871"},

    # DISTRICT OF COLUMBIA
    {"title": "The Washington Post", "domain": "washingtonpost.com", "state": "District of Columbia", "city": "Washington D.C.", "year": "Est. 1877"},
    {"title": "The Washington Times", "domain": "washingtontimes.com", "state": "District of Columbia", "city": "Washington D.C.", "year": "Est. 1982"},
    {"title": "Politico", "domain": "politico.com", "state": "District of Columbia", "city": "Washington D.C.", "year": "Est. 2007"},
    {"title": "The Hill", "domain": "thehill.com", "state": "District of Columbia", "city": "Washington D.C.", "year": "Est. 1994"},
    {"title": "Roll Call", "domain": "rollcall.com", "state": "District of Columbia", "city": "Washington D.C.", "year": "Est. 1955"},

    # FLORIDA
    {"title": "Tampa Bay Times", "domain": "tampabay.com", "state": "Florida", "city": "Tampa Bay", "year": "Est. 1884"},
    {"title": "Miami Herald", "domain": "miamiherald.com", "state": "Florida", "city": "Miami", "year": "Est. 1903"},
    {"title": "Orlando Sentinel", "domain": "orlandosentinel.com", "state": "Florida", "city": "Orlando", "year": "Est. 1876"},
    {"title": "South Florida Sun-Sentinel", "domain": "sun-sentinel.com", "state": "Florida", "city": "Fort Lauderdale", "year": "Est. 1910"},
    {"title": "The Florida Times-Union", "domain": "jacksonville.com", "state": "Florida", "city": "Jacksonville", "year": "Est. 1864"},
    {"title": "The Palm Beach Post", "domain": "palmbeachpost.com", "state": "Florida", "city": "West Palm Beach", "year": "Est. 1916"},

    # GEORGIA
    {"title": "The Atlanta Journal-Constitution", "domain": "ajc.com", "state": "Georgia", "city": "Atlanta", "year": "Est. 1868"},
    {"title": "The Augusta Chronicle", "domain": "augustachronicle.com", "state": "Georgia", "city": "Augusta", "year": "Est. 1785"},
    {"title": "Savannah Morning News", "domain": "savannahnow.com", "state": "Georgia", "city": "Savannah", "year": "Est. 1850"},

    # HAWAII
    {"title": "Honolulu Star-Advertiser", "domain": "staradvertiser.com", "state": "Hawaii", "city": "Honolulu", "year": "Est. 1882"},
    {"title": "The Maui News", "domain": "mauinews.com", "state": "Hawaii", "city": "Maui", "year": "Est. 1900"},

    # IDAHO
    {"title": "Idaho Statesman", "domain": "idahostatesman.com", "state": "Idaho", "city": "Boise", "year": "Est. 1864"},
    {"title": "Idaho Press", "domain": "idahopress.com", "state": "Idaho", "city": "Nampa / Boise", "year": "Est. 1893"},

    # ILLINOIS
    {"title": "Chicago Tribune", "domain": "chicagotribune.com", "state": "Illinois", "city": "Chicago", "year": "Est. 1847"},
    {"title": "Chicago Sun-Times", "domain": "chicago.suntimes.com", "state": "Illinois", "city": "Chicago", "year": "Est. 1948"},
    {"title": "Daily Herald", "domain": "dailyherald.com", "state": "Illinois", "city": "Suburban Chicago", "year": "Est. 1871"},
    {"title": "The State Journal-Register", "domain": "sj-r.com", "state": "Illinois", "city": "Springfield", "year": "Est. 1831"},

    # INDIANA
    {"title": "The Indianapolis Star", "domain": "indystar.com", "state": "Indiana", "city": "Indianapolis", "year": "Est. 1903"},
    {"title": "South Bend Tribune", "domain": "southbendtribune.com", "state": "Indiana", "city": "South Bend", "year": "Est. 1872"},
    {"title": "The Journal Gazette", "domain": "journalgazette.net", "state": "Indiana", "city": "Fort Wayne", "year": "Est. 1863"},

    # IOWA
    {"title": "The Des Moines Register", "domain": "desmoinesregister.com", "state": "Iowa", "city": "Des Moines", "year": "Est. 1849"},
    {"title": "The Gazette", "domain": "thegazette.com", "state": "Iowa", "city": "Cedar Rapids", "year": "Est. 1883"},

    # KANSAS
    {"title": "The Wichita Eagle", "domain": "kansas.com", "state": "Kansas", "city": "Wichita", "year": "Est. 1872"},
    {"title": "The Topeka Capital-Journal", "domain": "cjonline.com", "state": "Kansas", "city": "Topeka", "year": "Est. 1879"},

    # KENTUCKY
    {"title": "The Courier-Journal", "domain": "courier-journal.com", "state": "Kentucky", "city": "Louisville", "year": "Est. 1868"},
    {"title": "Lexington Herald-Leader", "domain": "kentucky.com", "state": "Kentucky", "city": "Lexington", "year": "Est. 1870"},

    # LOUISIANA
    {"title": "The Times-Picayune | The New Orleans Advocate", "domain": "nola.com", "state": "Louisiana", "city": "New Orleans", "year": "Est. 1837"},
    {"title": "The Advocate", "domain": "theadvocate.com", "state": "Louisiana", "city": "Baton Rouge", "year": "Est. 1925"},

    # MAINE
    {"title": "Portland Press Herald", "domain": "pressherald.com", "state": "Maine", "city": "Portland", "year": "Est. 1862"},
    {"title": "Bangor Daily News", "domain": "bangordailynews.com", "state": "Maine", "city": "Bangor", "year": "Est. 1889"},

    # MARYLAND
    {"title": "The Baltimore Sun", "domain": "baltimoresun.com", "state": "Maryland", "city": "Baltimore", "year": "Est. 1837"},
    {"title": "The Capital Gazette", "domain": "capitalgazette.com", "state": "Maryland", "city": "Annapolis", "year": "Est. 1884"},

    # MASSACHUSETTS
    {"title": "The Boston Globe", "domain": "bostonglobe.com", "state": "Massachusetts", "city": "Boston", "year": "Est. 1872"},
    {"title": "Boston Herald", "domain": "bostonherald.com", "state": "Massachusetts", "city": "Boston", "year": "Est. 1846"},
    {"title": "Telegram & Gazette", "domain": "telegram.com", "state": "Massachusetts", "city": "Worcester", "year": "Est. 1866"},
    {"title": "The Republican / MassLive", "domain": "masslive.com", "state": "Massachusetts", "city": "Springfield", "year": "Est. 1824"},

    # MICHIGAN
    {"title": "Detroit Free Press", "domain": "freep.com", "state": "Michigan", "city": "Detroit", "year": "Est. 1831"},
    {"title": "The Detroit News", "domain": "detroitnews.com", "state": "Michigan", "city": "Detroit", "year": "Est. 1873"},
    {"title": "The Grand Rapids Press / MLive", "domain": "mlive.com", "state": "Michigan", "city": "Grand Rapids", "year": "Est. 1893"},
    {"title": "Lansing State Journal", "domain": "lansingstatejournal.com", "state": "Michigan", "city": "Lansing", "year": "Est. 1855"},

    # MINNESOTA
    {"title": "Star Tribune", "domain": "startribune.com", "state": "Minnesota", "city": "Minneapolis", "year": "Est. 1867"},
    {"title": "St. Paul Pioneer Press", "domain": "twincities.com", "state": "Minnesota", "city": "St. Paul", "year": "Est. 1849"},

    # MISSISSIPPI
    {"title": "The Clarion-Ledger", "domain": "clarionledger.com", "state": "Mississippi", "city": "Jackson", "year": "Est. 1837"},
    {"title": "Sun Herald", "domain": "sunherald.com", "state": "Mississippi", "city": "Biloxi", "year": "Est. 1884"},

    # MISSOURI
    {"title": "St. Louis Post-Dispatch", "domain": "stltoday.com", "state": "Missouri", "city": "St. Louis", "year": "Est. 1878"},
    {"title": "The Kansas City Star", "domain": "kansascity.com", "state": "Missouri", "city": "Kansas City", "year": "Est. 1880"},

    # MONTANA
    {"title": "Billings Gazette", "domain": "billingsgazette.com", "state": "Montana", "city": "Billings", "year": "Est. 1885"},
    {"title": "Missoulian", "domain": "missoulian.com", "state": "Montana", "city": "Missoula", "year": "Est. 1870"},

    # NEBRASKA
    {"title": "Omaha World-Herald", "domain": "omaha.com", "state": "Nebraska", "city": "Omaha", "year": "Est. 1885"},
    {"title": "Lincoln Journal Star", "domain": "journalstar.com", "state": "Nebraska", "city": "Lincoln", "year": "Est. 1867"},

    # NEVADA
    {"title": "Las Vegas Review-Journal", "domain": "reviewjournal.com", "state": "Nevada", "city": "Las Vegas", "year": "Est. 1909"},
    {"title": "Reno Gazette-Journal", "domain": "rgj.com", "state": "Nevada", "city": "Reno", "year": "Est. 1870"},
    {"title": "Las Vegas Sun", "domain": "lasvegassun.com", "state": "Nevada", "city": "Las Vegas", "year": "Est. 1950"},

    # NEW HAMPSHIRE
    {"title": "New Hampshire Union Leader", "domain": "unionleader.com", "state": "New Hampshire", "city": "Manchester", "year": "Est. 1863"},
    {"title": "Concord Monitor", "domain": "concordmonitor.com", "state": "New Hampshire", "city": "Concord", "year": "Est. 1864"},

    # NEW JERSEY
    {"title": "The Star-Ledger / NJ.com", "domain": "nj.com", "state": "New Jersey", "city": "Newark", "year": "Est. 1832"},
    {"title": "The Record / NorthJersey.com", "domain": "northjersey.com", "state": "New Jersey", "city": "Bergen / North Jersey", "year": "Est. 1895"},
    {"title": "Asbury Park Press", "domain": "app.com", "state": "New Jersey", "city": "Asbury Park / Jersey Shore", "year": "Est. 1879"},

    # NEW MEXICO
    {"title": "Albuquerque Journal", "domain": "abqjournal.com", "state": "New Mexico", "city": "Albuquerque", "year": "Est. 1880"},
    {"title": "The Santa Fe New Mexican", "domain": "santafenewmexican.com", "state": "New Mexico", "city": "Santa Fe", "year": "Est. 1849"},

    # NEW YORK
    {"title": "The New York Times", "domain": "nytimes.com", "state": "New York", "city": "New York City", "year": "Est. 1851"},
    {"title": "New York Post", "domain": "nypost.com", "state": "New York", "city": "New York City", "year": "Est. 1801"},
    {"title": "New York Daily News", "domain": "nydailynews.com", "state": "New York", "city": "New York City", "year": "Est. 1919"},
    {"title": "Newsday", "domain": "newsday.com", "state": "New York", "city": "Long Island", "year": "Est. 1940"},
    {"title": "The Buffalo News", "domain": "buffalonews.com", "state": "New York", "city": "Buffalo", "year": "Est. 1880"},
    {"title": "Times Union", "domain": "timesunion.com", "state": "New York", "city": "Albany", "year": "Est. 1856"},
    {"title": "The Post-Standard / Syracuse.com", "domain": "syracuse.com", "state": "New York", "city": "Syracuse", "year": "Est. 1829"},
    {"title": "Democrat and Chronicle", "domain": "democratandchronicle.com", "state": "New York", "city": "Rochester", "year": "Est. 1833"},

    # NORTH CAROLINA
    {"title": "The News & Observer", "domain": "newsobserver.com", "state": "North Carolina", "city": "Raleigh", "year": "Est. 1865"},
    {"title": "The Charlotte Observer", "domain": "charlotteobserver.com", "state": "North Carolina", "city": "Charlotte", "year": "Est. 1886"},
    {"title": "Winston-Salem Journal", "domain": "journalnow.com", "state": "North Carolina", "city": "Winston-Salem", "year": "Est. 1897"},

    # NORTH DAKOTA
    {"title": "The Forum of Fargo-Moorhead", "domain": "inforum.com", "state": "North Dakota", "city": "Fargo", "year": "Est. 1878"},
    {"title": "The Bismarck Tribune", "domain": "bismarcktribune.com", "state": "North Dakota", "city": "Bismarck", "year": "Est. 1873"},

    # OHIO
    {"title": "The Plain Dealer / cleveland.com", "domain": "cleveland.com", "state": "Ohio", "city": "Cleveland", "year": "Est. 1842"},
    {"title": "The Columbus Dispatch", "domain": "dispatch.com", "state": "Ohio", "city": "Columbus", "year": "Est. 1871"},
    {"title": "The Cincinnati Enquirer", "domain": "cincinnati.com", "state": "Ohio", "city": "Cincinnati", "year": "Est. 1841"},
    {"title": "The Blade", "domain": "toledoblade.com", "state": "Ohio", "city": "Toledo", "year": "Est. 1835"},
    {"title": "Akron Beacon Journal", "domain": "beaconjournal.com", "state": "Ohio", "city": "Akron", "year": "Est. 1839"},

    # OKLAHOMA
    {"title": "The Oklahoman", "domain": "oklahoman.com", "state": "Oklahoma", "city": "Oklahoma City", "year": "Est. 1889"},
    {"title": "Tulsa World", "domain": "tulsaworld.com", "state": "Oklahoma", "city": "Tulsa", "year": "Est. 1905"},

    # OREGON
    {"title": "The Oregonian / OregonLive", "domain": "oregonlive.com", "state": "Oregon", "city": "Portland", "year": "Est. 1850"},
    {"title": "The Register-Guard", "domain": "registerguard.com", "state": "Oregon", "city": "Eugene", "year": "Est. 1867"},
    {"title": "Statesman Journal", "domain": "statesmanjournal.com", "state": "Oregon", "city": "Salem", "year": "Est. 1851"},

    # PENNSYLVANIA
    {"title": "The Philadelphia Inquirer", "domain": "inquirer.com", "state": "Pennsylvania", "city": "Philadelphia", "year": "Est. 1829"},
    {"title": "Pittsburgh Post-Gazette", "domain": "post-gazette.com", "state": "Pennsylvania", "city": "Pittsburgh", "year": "Est. 1786"},
    {"title": "PennLive / The Patriot-News", "domain": "pennlive.com", "state": "Pennsylvania", "city": "Harrisburg", "year": "Est. 1854"},
    {"title": "The Morning Call", "domain": "mcall.com", "state": "Pennsylvania", "city": "Allentown", "year": "Est. 1883"},

    # RHODE ISLAND
    {"title": "The Providence Journal", "domain": "providencejournal.com", "state": "Rhode Island", "city": "Providence", "year": "Est. 1829"},

    # SOUTH CAROLINA
    {"title": "The Post and Courier", "domain": "postandcourier.com", "state": "South Carolina", "city": "Charleston", "year": "Est. 1803"},
    {"title": "The State", "domain": "thestate.com", "state": "South Carolina", "city": "Columbia", "year": "Est. 1891"},

    # SOUTH DAKOTA
    {"title": "Argus Leader", "domain": "argusleader.com", "state": "South Dakota", "city": "Sioux Falls", "year": "Est. 1881"},
    {"title": "Rapid City Journal", "domain": "rapidcityjournal.com", "state": "South Dakota", "city": "Rapid City", "year": "Est. 1878"},

    # TENNESSEE
    {"title": "The Tennessean", "domain": "tennessean.com", "state": "Tennessee", "city": "Nashville", "year": "Est. 1907"},
    {"title": "The Commercial Appeal", "domain": "commercialappeal.com", "state": "Tennessee", "city": "Memphis", "year": "Est. 1841"},
    {"title": "Knoxville News Sentinel", "domain": "knoxnews.com", "state": "Tennessee", "city": "Knoxville", "year": "Est. 1886"},

    # TEXAS
    {"title": "The Dallas Morning News", "domain": "dallasnews.com", "state": "Texas", "city": "Dallas", "year": "Est. 1885"},
    {"title": "Houston Chronicle", "domain": "houstonchronicle.com", "state": "Texas", "city": "Houston", "year": "Est. 1901"},
    {"title": "Austin American-Statesman", "domain": "statesman.com", "state": "Texas", "city": "Austin", "year": "Est. 1871"},
    {"title": "San Antonio Express-News", "domain": "expressnews.com", "state": "Texas", "city": "San Antonio", "year": "Est. 1865"},
    {"title": "Fort Worth Star-Telegram", "domain": "star-telegram.com", "state": "Texas", "city": "Fort Worth", "year": "Est. 1906"},
    {"title": "El Paso Times", "domain": "elpasotimes.com", "state": "Texas", "city": "El Paso", "year": "Est. 1881"},

    # UTAH
    {"title": "Deseret News", "domain": "deseret.com", "state": "Utah", "city": "Salt Lake City", "year": "Est. 1850"},
    {"title": "The Salt Lake Tribune", "domain": "sltrib.com", "state": "Utah", "city": "Salt Lake City", "year": "Est. 1871"},

    # VERMONT
    {"title": "The Burlington Free Press", "domain": "burlingtonfreepress.com", "state": "Vermont", "city": "Burlington", "year": "Est. 1827"},

    # VIRGINIA
    {"title": "Richmond Times-Dispatch", "domain": "richmond.com", "state": "Virginia", "city": "Richmond", "year": "Est. 1850"},
    {"title": "The Virginian-Pilot", "domain": "pilotonline.com", "state": "Virginia", "city": "Norfolk", "year": "Est. 1865"},
    {"title": "The Roanoke Times", "domain": "roanoke.com", "state": "Virginia", "city": "Roanoke", "year": "Est. 1886"},

    # WASHINGTON
    {"title": "The Seattle Times", "domain": "seattletimes.com", "state": "Washington", "city": "Seattle", "year": "Est. 1891"},
    {"title": "The News Tribune", "domain": "thenewstribune.com", "state": "Washington", "city": "Tacoma", "year": "Est. 1883"},
    {"title": "The Spokesman-Review", "domain": "spokesman.com", "state": "Washington", "city": "Spokane", "year": "Est. 1883"},
    {"title": "The Columbian", "domain": "columbian.com", "state": "Washington", "city": "Vancouver", "year": "Est. 1890"},

    # WEST VIRGINIA
    {"title": "The Charleston Gazette-Mail", "domain": "wvgazettemail.com", "state": "West Virginia", "city": "Charleston", "year": "Est. 1873"},
    {"title": "The Herald-Dispatch", "domain": "herald-dispatch.com", "state": "West Virginia", "city": "Huntington", "year": "Est. 1909"},

    # WISCONSIN
    {"title": "Milwaukee Journal Sentinel", "domain": "jsonline.com", "state": "Wisconsin", "city": "Milwaukee", "year": "Est. 1837"},
    {"title": "Wisconsin State Journal", "domain": "madison.com", "state": "Wisconsin", "city": "Madison", "year": "Est. 1839"},
    {"title": "Green Bay Press-Gazette", "domain": "greenbaypressgazette.com", "state": "Wisconsin", "city": "Green Bay", "year": "Est. 1915"},

    # WYOMING
    {"title": "Casper Star-Tribune", "domain": "trib.com", "state": "Wyoming", "city": "Casper", "year": "Est. 1891"},
    {"title": "Wyoming Tribune Eagle", "domain": "wyomingnews.com", "state": "Wyoming", "city": "Cheyenne", "year": "Est. 1867"},
]

usa_states_items = []
for p in state_papers_data:
    url = f"https://www.{p['domain']}" if not p['domain'].startswith("http") else p['domain']
    if not url.startswith("http"):
        url = "https://" + url
    clean_domain = p['domain'].split('/')[0]
    usa_states_items.append({
        "title": p["title"],
        "provider": "web",
        "arguments": [url],
        "image": favicon(clean_domain),
        "year": p["state"],
        "genre": p["city"],
        "rating": p.get("year", "Local Press")
    })

with open(os.path.join(assets_dir, "usa_states_newspapers.json"), "w", encoding="utf-8") as f:
    json.dump(usa_states_items, f, indent=2, ensure_ascii=False)
print(f"Generated usa_states_newspapers.json with {len(usa_states_items)} newspapers covering all 50 states + DC.")

# ============================================================================
# 2. AMERICAN NEWS CHANNELS (InnerTube + Live)
# ============================================================================
channels_data = [
    {
        "title": "CNN",
        "provider": "youtube_channel",
        "arguments": ["UCupvZG-5ko_eiXAupbDfxWw"],
        "image": "https://yt3.googleusercontent.com/y_gTfC512Y1b5x2QkQ3bVz3g7h5E2K8G8J_7K9M-o8_jQ=s900-c-k-c0x00ffffff-no-rj",
        "year": "Breaking News 24/7",
        "genre": "National Cable News",
        "rating": "Live"
    },
    {
        "title": "Fox News",
        "provider": "youtube_channel",
        "arguments": ["UCXIJgqnII2ZOINSWNOGFThA"],
        "image": "https://yt3.googleusercontent.com/upload/foxnews.png",
        "year": "Opinion & Politics",
        "genre": "National Cable News",
        "rating": "Live"
    },
    {
        "title": "MSNBC",
        "provider": "youtube_channel",
        "arguments": ["UCaXkIU1QidjPwiAYu6GcHjg"],
        "image": "https://yt3.googleusercontent.com/msnbc.png",
        "year": "In-Depth Analysis",
        "genre": "National News",
        "rating": "Live"
    },
    {
        "title": "ABC News",
        "provider": "youtube_channel",
        "arguments": ["UCBi2mrWuNuyYy4gbM6fU18Q"],
        "image": "https://yt3.googleusercontent.com/abcnews.png",
        "year": "Good Morning America & World News",
        "genre": "Broadcast Network",
        "rating": "Live"
    },
    {
        "title": "CBS News",
        "provider": "youtube_channel",
        "arguments": ["UC8p1vwvWtl6T73JiExfWs1g"],
        "image": "https://yt3.googleusercontent.com/cbsnews.png",
        "year": "60 Minutes & Morning News",
        "genre": "Broadcast Network",
        "rating": "Live"
    },
    {
        "title": "NBC News",
        "provider": "youtube_channel",
        "arguments": ["UCeY0bbntWzzVIaj2z3QigXg"],
        "image": "https://yt3.googleusercontent.com/nbcnews.png",
        "year": "Nightly News & Today",
        "genre": "Broadcast Network",
        "rating": "Live"
    },
    {
        "title": "PBS NewsHour",
        "provider": "youtube_channel",
        "arguments": ["UC6ZFN9Tx6xh-skXCuRHCDpQ"],
        "image": "https://yt3.googleusercontent.com/pbsnewshour.png",
        "year": "Independent Journalism",
        "genre": "Public Broadcasting",
        "rating": "Live"
    },
    {
        "title": "CNBC",
        "provider": "youtube_channel",
        "arguments": ["UCvJJ_dzjViJCoLf5uKUTwoA"],
        "image": "https://yt3.googleusercontent.com/cnbc.png",
        "year": "Markets, Stocks & Economy",
        "genre": "Business News",
        "rating": "Live"
    },
    {
        "title": "Bloomberg Markets & Finance",
        "provider": "youtube_channel",
        "arguments": ["UCIALMKvObZNtJ6AmdCLP7Lg"],
        "image": "https://yt3.googleusercontent.com/bloomberg.png",
        "year": "Global Markets & Technology",
        "genre": "Financial News",
        "rating": "Live"
    },
    {
        "title": "Associated Press (AP)",
        "provider": "youtube_channel",
        "arguments": ["UC52X5ev3BM3U535pkJQu4UW"],
        "image": "https://yt3.googleusercontent.com/apnews.png",
        "year": "Global News Wire",
        "genre": "Wire Service",
        "rating": "Live"
    },
    {
        "title": "Reuters",
        "provider": "youtube_channel",
        "arguments": ["UChqUTb7kYRX8-EiaN3XFrSQ"],
        "image": "https://yt3.googleusercontent.com/reuters.png",
        "year": "International & Financial",
        "genre": "Wire Service",
        "rating": "Live"
    },
    {
        "title": "C-SPAN",
        "provider": "youtube_channel",
        "arguments": ["UCb--64Gl51jIEVE-GLDAVTg"],
        "image": "https://yt3.googleusercontent.com/cspan.png",
        "year": "US Congress & Capitol Hill",
        "genre": "Public Affairs",
        "rating": "Live"
    },
    {
        "title": "The Hill",
        "provider": "youtube_channel",
        "arguments": ["UCPWXiRWZ29zrxPFIQT7eHSA"],
        "image": "https://yt3.googleusercontent.com/thehill.png",
        "year": "Rising & Political Debates",
        "genre": "US Politics",
        "rating": "HD"
    },
    {
        "title": "Politico",
        "provider": "youtube_channel",
        "arguments": ["UCkhn9Ylq7pZqZzC0fQ5r2bg"],
        "image": "https://yt3.googleusercontent.com/politico.png",
        "year": "White House & Elections",
        "genre": "Politics & Policy",
        "rating": "HD"
    },
    {
        "title": "USA TODAY",
        "provider": "youtube_channel",
        "arguments": ["UCPqHl3P54hZ-7q7g_K-w4cw"],
        "image": "https://yt3.googleusercontent.com/usatoday.png",
        "year": "National Reports & Visuals",
        "genre": "National Newspaper",
        "rating": "HD"
    },
    {
        "title": "Forbes Breaking News",
        "provider": "youtube_channel",
        "arguments": ["UCg40OxZ1GYh3u3jB228x11g"],
        "image": "https://yt3.googleusercontent.com/forbes.png",
        "year": "Congressional Hearings & Briefings",
        "genre": "Business & Politics",
        "rating": "HD"
    },
    {
        "title": "The Wall Street Journal",
        "provider": "youtube_channel",
        "arguments": ["UCK7tptUDHh-RYDsdxO1-5QQ"],
        "image": "https://yt3.googleusercontent.com/wsj.png",
        "year": "Tech, Business & Investigations",
        "genre": "Investigative Video",
        "rating": "HD"
    },
    {
        "title": "The Washington Post",
        "provider": "youtube_channel",
        "arguments": ["UCHd62-u_vZKUvvBOojmg0Sg"],
        "image": "https://yt3.googleusercontent.com/wapo.png",
        "year": "First Look & Politics",
        "genre": "National News",
        "rating": "HD"
    },
    {
        "title": "Newsmax",
        "provider": "youtube_channel",
        "arguments": ["UCx6h-dWzJ5S14GpOgz20ibA"],
        "image": "https://yt3.googleusercontent.com/newsmax.png",
        "year": "News & Analysis",
        "genre": "Cable News",
        "rating": "HD"
    },
    {
        "title": "Scripps News",
        "provider": "youtube_channel",
        "arguments": ["UC43fJ3d2i4N0e_fG7eE-n4g"],
        "image": "https://yt3.googleusercontent.com/scripps.png",
        "year": "Live 24/7 Context & Facts",
        "genre": "All-News Network",
        "rating": "Live"
    },
    {
        "title": "Yahoo Finance",
        "provider": "youtube_channel",
        "arguments": ["UCEAZeUIeJs0IjQiqTCdVSIg"],
        "image": "https://yt3.googleusercontent.com/yahoofinance.png",
        "year": "Market Movers & Earnings",
        "genre": "Financial News",
        "rating": "Live"
    }
]

# Set fallback favicons for any channel images that don't have custom ones
for ch in channels_data:
    if "yt3.googleusercontent.com/upload" in ch["image"] or "yt3.googleusercontent.com/" in ch["image"]:
        domain = "youtube.com"
        title_lower = ch["title"].lower()
        if "cnn" in title_lower: domain = "cnn.com"
        elif "fox" in title_lower: domain = "foxnews.com"
        elif "msnbc" in title_lower: domain = "msnbc.com"
        elif "abc" in title_lower: domain = "abcnews.go.com"
        elif "cbs" in title_lower: domain = "cbsnews.com"
        elif "nbc" in title_lower: domain = "nbcnews.com"
        elif "pbs" in title_lower: domain = "pbs.org"
        elif "cnbc" in title_lower: domain = "cnbc.com"
        elif "bloomberg" in title_lower: domain = "bloomberg.com"
        elif "ap" in title_lower or "associated" in title_lower: domain = "apnews.com"
        elif "reuters" in title_lower: domain = "reuters.com"
        elif "c-span" in title_lower: domain = "c-span.org"
        elif "hill" in title_lower: domain = "thehill.com"
        elif "politico" in title_lower: domain = "politico.com"
        elif "usa today" in title_lower: domain = "usatoday.com"
        elif "forbes" in title_lower: domain = "forbes.com"
        elif "wsj" in title_lower or "wall street" in title_lower: domain = "wsj.com"
        elif "washington post" in title_lower: domain = "washingtonpost.com"
        elif "newsmax" in title_lower: domain = "newsmax.com"
        elif "scripps" in title_lower: domain = "scrippsnews.com"
        elif "yahoo" in title_lower: domain = "finance.yahoo.com"
        ch["image"] = favicon(domain)

with open(os.path.join(assets_dir, "news_channels.json"), "w", encoding="utf-8") as f:
    json.dump(channels_data, f, indent=2, ensure_ascii=False)
with open(os.path.join(assets_dir, "channels.json"), "w", encoding="utf-8") as f:
    json.dump(channels_data, f, indent=2, ensure_ascii=False)
print(f"Generated news_channels.json and channels.json with {len(channels_data)} American news channels.")

# ============================================================================
# 3. COMPLETE USA NEWS RSS FEEDS CATALOG
# ============================================================================
rss_feeds = [
    # TOP HEADLINES & BREAKING
    {"title": "The New York Times (Top Stories)", "url": "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml", "category": "TOP"},
    {"title": "The Washington Post (National)", "url": "https://feeds.washingtonpost.com/rss/national", "category": "TOP"},
    {"title": "The Wall Street Journal (Business)", "url": "https://feeds.a.dj.com/rss/WSJcomUSBusiness.xml", "category": "TOP"},
    {"title": "USA TODAY (Top News)", "url": "http://rssfeeds.usatoday.com/usatoday-NewsTopStories", "category": "TOP"},
    {"title": "Fox News (Latest Headlines)", "url": "https://moxie.foxnews.com/google-publisher/latest.xml", "category": "TOP"},
    {"title": "CNN (Top Stories)", "url": "http://rss.cnn.com/rss/cnn_topstories.rss", "category": "TOP"},
    {"title": "NBC News (Breaking Headlines)", "url": "https://feeds.nbcnews.com/nbcnews/public/news", "category": "TOP"},
    {"title": "CBS News (Latest News)", "url": "https://www.cbsnews.com/latest/rss/main", "category": "TOP"},
    {"title": "ABC News (Top Stories)", "url": "https://abcnews.go.com/abcnews/topstories", "category": "TOP"},
    {"title": "NPR News (National Headlines)", "url": "https://feeds.npr.org/1001/rss.xml", "category": "TOP"},
    {"title": "PBS NewsHour (Headlines)", "url": "https://www.pbs.org/newshour/feeds/rss/headlines", "category": "TOP"},
    {"title": "Google News USA (Top News)", "url": "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en", "category": "TOP"},

    # US POLITICS & WHITE HOUSE
    {"title": "Politico (US Politics)", "url": "https://rss.politico.com/politics-news.xml", "category": "POLITICS"},
    {"title": "The Hill (Capitol Hill & Politics)", "url": "https://thehill.com/feed/", "category": "POLITICS"},
    {"title": "The Washington Post (Politics)", "url": "https://feeds.washingtonpost.com/rss/politics", "category": "POLITICS"},
    {"title": "The New York Times (Politics)", "url": "https://rss.nytimes.com/services/xml/rss/nyt/Politics.xml", "category": "POLITICS"},
    {"title": "USA TODAY (Washington & Politics)", "url": "http://rssfeeds.usatoday.com/usatoday-NewsPolitics", "category": "POLITICS"},
    {"title": "Google News (US Politics)", "url": "https://news.google.com/rss/headlines/section/topic/POLITICS?hl=en-US&gl=US&ceid=US:en", "category": "POLITICS"},

    # BUSINESS & MARKETS
    {"title": "The Wall Street Journal (Markets)", "url": "https://feeds.a.dj.com/rss/RSSMarketsMain.xml", "category": "BUSINESS"},
    {"title": "CNBC (Business & Finance)", "url": "https://search.cnbc.com/rs/search/view.html?partnerId=2000&keywords=business&format=rss", "category": "BUSINESS"},
    {"title": "Yahoo Finance (Top Stories)", "url": "https://finance.yahoo.com/news/rssindex", "category": "BUSINESS"},
    {"title": "The New York Times (Business)", "url": "https://rss.nytimes.com/services/xml/rss/nyt/Business.xml", "category": "BUSINESS"},
    {"title": "Google News (Business & Economy)", "url": "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=en-US&gl=US&ceid=US:en", "category": "BUSINESS"},

    # TECHNOLOGY & AI
    {"title": "The Verge (Tech & Gadgets)", "url": "https://www.theverge.com/rss/index.xml", "category": "TECH"},
    {"title": "TechCrunch (Startups & Tech)", "url": "https://techcrunch.com/feed/", "category": "TECH"},
    {"title": "Wired (Technology & Culture)", "url": "https://www.wired.com/feed/rss", "category": "TECH"},
    {"title": "The New York Times (Technology)", "url": "https://rss.nytimes.com/services/xml/rss/nyt/Technology.xml", "category": "TECH"},
    {"title": "Google News (Technology & AI)", "url": "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en", "category": "TECH"},

    # WORLD NEWS
    {"title": "The New York Times (World News)", "url": "https://rss.nytimes.com/services/xml/rss/nyt/World.xml", "category": "WORLD"},
    {"title": "The Washington Post (World)", "url": "https://feeds.washingtonpost.com/rss/world", "category": "WORLD"},
    {"title": "The Wall Street Journal (World)", "url": "https://feeds.a.dj.com/rss/RSSWorldNews.xml", "category": "WORLD"},
    {"title": "Google News (World Affairs)", "url": "https://news.google.com/rss/headlines/section/topic/WORLD?hl=en-US&gl=US&ceid=US:en", "category": "WORLD"},

    # SPORTS
    {"title": "The New York Times (Sports)", "url": "https://rss.nytimes.com/services/xml/rss/nyt/Sports.xml", "category": "SPORTS"},
    {"title": "The Washington Post (Sports)", "url": "https://feeds.washingtonpost.com/rss/sports", "category": "SPORTS"},
    {"title": "USA TODAY (Sports Headlines)", "url": "http://rssfeeds.usatoday.com/usatoday-NewsSports", "category": "SPORTS"},
    {"title": "Google News (US & Global Sports)", "url": "https://news.google.com/rss/headlines/section/topic/SPORTS?hl=en-US&gl=US&ceid=US:en", "category": "SPORTS"},

    # ENTERTAINMENT & HOLLYWOOD
    {"title": "Variety (Hollywood & Film)", "url": "https://variety.com/feed/", "category": "ENTERTAINMENT"},
    {"title": "The Hollywood Reporter", "url": "https://www.hollywoodreporter.com/feed/", "category": "ENTERTAINMENT"},
    {"title": "The New York Times (Arts & Movies)", "url": "https://rss.nytimes.com/services/xml/rss/nyt/Movies.xml", "category": "ENTERTAINMENT"},
    {"title": "Google News (Entertainment)", "url": "https://news.google.com/rss/headlines/section/topic/ENTERTAINMENT?hl=en-US&gl=US&ceid=US:en", "category": "ENTERTAINMENT"},

    # SCIENCE & SPACE
    {"title": "The New York Times (Science)", "url": "https://rss.nytimes.com/services/xml/rss/nyt/Science.xml", "category": "SCIENCE"},
    {"title": "Space.com (NASA & Astronomy)", "url": "https://www.space.com/feeds/all", "category": "SCIENCE"},
    {"title": "Google News (Science & Space)", "url": "https://news.google.com/rss/headlines/section/topic/SCIENCE?hl=en-US&gl=US&ceid=US:en", "category": "SCIENCE"},

    # HEALTH & WELLNESS
    {"title": "The New York Times (Health)", "url": "https://rss.nytimes.com/services/xml/rss/nyt/Health.xml", "category": "HEALTH"},
    {"title": "Google News (Health & Medicine)", "url": "https://news.google.com/rss/headlines/section/topic/HEALTH?hl=en-US&gl=US&ceid=US:en", "category": "HEALTH"}
]

with open(os.path.join(assets_dir, "news_rss.json"), "w", encoding="utf-8") as f:
    json.dump(rss_feeds, f, indent=2, ensure_ascii=False)
print(f"Generated news_rss.json with {len(rss_feeds)} premier American RSS feeds.")

# ============================================================================
# 4. COMPLETE USA RADIO (News, Talk, Public Radio, NPR, Bloomberg, Sports)
# ============================================================================
usa_radio_stations = [
    {
        "name": "NPR 24/7 News & Talk",
        "description": "National Public Radio • All Things Considered & Morning Edition",
        "stream_url": "https://npr-ice.streamguys1.com/live.mp3",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=npr.org",
        "category": "National News"
    },
    {
        "name": "Bloomberg Radio",
        "description": "Global Business, Markets & Financial News 24/7",
        "stream_url": "https://stream.bloombergradio.com/live.mp3",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=bloomberg.com",
        "category": "Business & Finance"
    },
    {
        "name": "Fox News Talk Radio",
        "description": "Live News Updates, Analysis & Opinion",
        "stream_url": "https://foxnews.streamguys1.com/foxnews",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=foxnews.com",
        "category": "News & Talk"
    },
    {
        "name": "CBS News Radio",
        "description": "National & International News Reports 24/7",
        "stream_url": "https://cbsnews.streamguys1.com/cbsnews",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=cbsnews.com",
        "category": "National News"
    },
    {
        "name": "C-SPAN Radio Washington",
        "description": "Capitol Hill, Congress, White House & Supreme Court",
        "stream_url": "https://cspanradio.streamguys1.com/cspan",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=c-span.org",
        "category": "Politics"
    },
    {
        "name": "WTOP 103.5 FM - Washington D.C.",
        "description": "Top-Ranked All-News Station in Nation's Capital",
        "stream_url": "https://stream.revma.ihrhls.com/zc1417",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=wtop.com",
        "category": "Washington D.C."
    },
    {
        "name": "WNYC 93.9 FM - New York",
        "description": "New York Public Radio • News, Culture & Talk",
        "stream_url": "https://fm939.wnyc.org/wnycfm",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=wnyc.org",
        "category": "New York"
    },
    {
        "name": "KQED 88.5 FM - San Francisco",
        "description": "Northern California & Bay Area Public Radio",
        "stream_url": "https://streams.kqed.org/kqedradio",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=kqed.org",
        "category": "California"
    },
    {
        "name": "KCRW 89.9 FM - Los Angeles",
        "description": "Southern California News, Culture & NPR",
        "stream_url": "https://kcrw.streamguys1.com/kcrw_192k_mp3_on_air",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=kcrw.com",
        "category": "California"
    },
    {
        "name": "KFI AM 640 - Los Angeles",
        "description": "Southern California's News & Talk Leader",
        "stream_url": "https://stream.revma.ihrhls.com/zc185",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=kfiam640.iheart.com",
        "category": "California"
    },
    {
        "name": "WBZ NewsRadio 1030 - Boston",
        "description": "New England's Premier All-News Station",
        "stream_url": "https://stream.revma.ihrhls.com/zc1425",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=wbznewsradio.iheart.com",
        "category": "Massachusetts"
    },
    {
        "name": "WBUR 90.9 FM - Boston",
        "description": "Boston's NPR News Station",
        "stream_url": "https://wbur-ice.streamguys1.com/wbur_mp3",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=wbur.org",
        "category": "Massachusetts"
    },
    {
        "name": "WLS 890 AM - Chicago",
        "description": "Chicago's Talk & News Powerhouse",
        "stream_url": "https://stream.revma.ihrhls.com/zc1409",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=wlsam.com",
        "category": "Illinois"
    },
    {
        "name": "WBEZ 91.5 FM - Chicago",
        "description": "Chicago Public Media • NPR News",
        "stream_url": "https://wbez.streamguys1.com/wbez128",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=wbez.org",
        "category": "Illinois"
    },
    {
        "name": "WHYY 90.9 FM - Philadelphia",
        "description": "Greater Philadelphia & Delaware Valley NPR",
        "stream_url": "https://whyy.streamguys1.com/whyy-mp3",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=whyy.org",
        "category": "Pennsylvania"
    },
    {
        "name": "KRLD NewsRadio 1080 - Dallas / Texas",
        "description": "Texas News, Traffic, Weather & Business",
        "stream_url": "https://stream.revma.ihrhls.com/zc1421",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=audacy.com/krld",
        "category": "Texas"
    },
    {
        "name": "KERA 90.1 FM - North Texas",
        "description": "Dallas-Fort Worth NPR News Station",
        "stream_url": "https://kera.streamguys1.com/keralive",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=kera.org",
        "category": "Texas"
    },
    {
        "name": "KUT 90.5 FM - Austin",
        "description": "Austin's NPR Station • University of Texas",
        "stream_url": "https://kut.streamguys1.com/kut",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=kut.org",
        "category": "Texas"
    },
    {
        "name": "KUOW 94.9 FM - Seattle",
        "description": "Puget Sound & Washington NPR Public Radio",
        "stream_url": "https://kuow.streamguys1.com/live",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=kuow.org",
        "category": "Washington"
    },
    {
        "name": "WABE 90.1 FM - Atlanta",
        "description": "Atlanta's NPR Station • News & Conversations",
        "stream_url": "https://wabe.streamguys1.com/wabe",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=wabe.org",
        "category": "Georgia"
    },
    {
        "name": "WUSF 89.7 FM - Tampa Bay",
        "description": "Florida's West Coast NPR News & Jazz",
        "stream_url": "https://wusf.streamguys1.com/wusf",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=wusf.org",
        "category": "Florida"
    },
    {
        "name": "CPR News - Colorado",
        "description": "Colorado Public Radio • Denver Statewide News",
        "stream_url": "https://cpr.streamguys1.com/cpr1_mp3",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=cpr.org",
        "category": "Colorado"
    },
    {
        "name": "MPR News - Minneapolis",
        "description": "Minnesota Public Radio • Upper Midwest News",
        "stream_url": "https://stream.mpr.org/news_128",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=mprnews.org",
        "category": "Minnesota"
    },
    {
        "name": "Fox Sports Radio",
        "description": "Live American Sports News, Scores & Commentary",
        "stream_url": "https://stream.revma.ihrhls.com/zc249",
        "image": "https://www.google.com/s2/favicons?sz=128&domain=foxsportsradio.iheart.com",
        "category": "Sports"
    }
]

with open(os.path.join(assets_dir, "news_radio.json"), "w", encoding="utf-8") as f:
    json.dump(usa_radio_stations, f, indent=2, ensure_ascii=False)
print(f"Generated news_radio.json with {len(usa_radio_stations)} premier USA news and talk stations.")

# ============================================================================
# 5. CATEGORIES.JSON & NEWS_TOPICS.JSON
# ============================================================================
categories_data = [
    {
        "title": "⚡ Breaking & Top Headlines",
        "image": "https://img.icons8.com/color/96/news.png",
        "provider": "rss",
        "arguments": ["https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en"]
    },
    {
        "title": "🇺🇸 All 50 States Newspapers",
        "image": "https://img.icons8.com/color/96/usa.png",
        "provider": "overview",
        "arguments": ["usa_states_newspapers.json"]
    },
    {
        "title": "🏛️ US National Daily Newspapers",
        "image": "https://img.icons8.com/color/96/newspaper.png",
        "provider": "overview",
        "arguments": ["usa_national_newspapers.json"]
    },
    {
        "title": "📺 US News Channels (Live & Videos)",
        "image": "https://img.icons8.com/color/96/retro-tv.png",
        "provider": "overview",
        "arguments": ["news_channels.json"]
    },
    {
        "title": "🏛️ US Politics & White House",
        "image": "https://img.icons8.com/color/96/capitol.png",
        "provider": "rss",
        "arguments": ["https://rss.politico.com/politics-news.xml"]
    },
    {
        "title": "💼 Wall Street & Business",
        "image": "https://img.icons8.com/color/96/bullish.png",
        "provider": "rss",
        "arguments": ["https://feeds.a.dj.com/rss/WSJcomUSBusiness.xml"]
    },
    {
        "title": "📱 Technology & AI",
        "image": "https://img.icons8.com/color/96/artificial-intelligence.png",
        "provider": "rss",
        "arguments": ["https://www.theverge.com/rss/index.xml"]
    },
    {
        "title": "🌎 World & Global Affairs",
        "image": "https://img.icons8.com/color/96/globe.png",
        "provider": "rss",
        "arguments": ["https://rss.nytimes.com/services/xml/rss/nyt/World.xml"]
    },
    {
        "title": "⚽ Sports (NFL, NBA, MLB)",
        "image": "https://img.icons8.com/color/96/american-football.png",
        "provider": "rss",
        "arguments": ["https://rss.nytimes.com/services/xml/rss/nyt/Sports.xml"]
    },
    {
        "title": "🎬 Hollywood & Entertainment",
        "image": "https://img.icons8.com/color/96/clapperboard.png",
        "provider": "rss",
        "arguments": ["https://variety.com/feed/"]
    },
    {
        "title": "🔬 Science & Space Exploration",
        "image": "https://img.icons8.com/color/96/rocket.png",
        "provider": "rss",
        "arguments": ["https://www.space.com/feeds/all"]
    },
    {
        "title": "🏥 Health & Wellness",
        "image": "https://img.icons8.com/color/96/caduceus.png",
        "provider": "rss",
        "arguments": ["https://rss.nytimes.com/services/xml/rss/nyt/Health.xml"]
    },
    {
        "title": "🎙️ 24/7 US News & Talk Radio",
        "image": "https://img.icons8.com/color/96/radio-tower.png",
        "provider": "radio",
        "arguments": ["news_radio.json"]
    }
]

with open(os.path.join(assets_dir, "categories.json"), "w", encoding="utf-8") as f:
    json.dump(categories_data, f, indent=2, ensure_ascii=False)
with open(os.path.join(assets_dir, "news_topics.json"), "w", encoding="utf-8") as f:
    json.dump(categories_data, f, indent=2, ensure_ascii=False)
print("Generated categories.json and news_topics.json.")

# ============================================================================
# 6. HOME.JSON
# ============================================================================
home_data = [
    {
        "title": "US News Channels (Live & Videos)",
        "provider": "overview",
        "arguments": ["news_channels.json"],
        "image": "https://images.weserv.nl/?url=upload.wikimedia.org/wikipedia/commons/e/ef/Youtube_logo.png&w=400"
    },
    {
        "title": "Featured USA National Newspapers",
        "provider": "overview",
        "arguments": ["usa_national_newspapers.json"],
        "image": "https://www.google.com/s2/favicons?sz=128&domain=nytimes.com"
    },
    {
        "title": "Newspapers by State (50 States)",
        "provider": "overview",
        "arguments": ["usa_states_newspapers.json"],
        "image": "https://img.icons8.com/color/96/usa.png"
    },
    {
        "title": "Breaking News Live Feed (RSS)",
        "provider": "rss",
        "arguments": ["ALL_NEWS"],
        "image": "https://img.icons8.com/color/96/news.png"
    },
    {
        "title": "24/7 US News & Talk Radio",
        "provider": "radio",
        "arguments": ["news_radio.json"],
        "image": "https://www.google.com/s2/favicons?sz=128&domain=npr.org"
    },
    {
        "title": "Explore by Category & Topic",
        "provider": "overview",
        "arguments": ["categories.json"],
        "image": "https://img.icons8.com/color/96/categorize.png"
    }
]

with open(os.path.join(assets_dir, "home.json"), "w", encoding="utf-8") as f:
    json.dump(home_data, f, indent=2, ensure_ascii=False)
print("Generated home.json.")

# ============================================================================
# 7. CONFIG.JSON
# ============================================================================
youtube_source_list = []
for ch in channels_data:
    youtube_source_list.append({
        "name": ch["title"],
        "channel_id": ch["arguments"][0]
    })

rss_source_list = []
for rf in rss_feeds:
    rss_source_list.append({
        "title": rf["title"],
        "url": rf["url"]
    })

config_data = {
    "google_sheet_url": "",
    "github_cdn_base_url": "",
    "github_raw_base_url": "",
    "app": {
        "status": True,
        "toolbar": True,
        "navigation_drawer": True,
        "geolocation": True,
        "cache": True,
        "open_link_in_external_browser": False,
        "zoom_controls": False,
        "user_agent": "",
        "privacy_policy_url": "privacy_policy.html",
        "more_apps_url": "",
        "webview_disclaimer_active": False,
        "redirect_url": ""
    },
    "intro": {
        "status": False,
        "sliders": []
    },
    "menus": [
        {
            "name": "Home",
            "type": "category",
            "url": "home.json",
            "icon": "https://img.icons8.com/color/96/home.png"
        },
        {
            "name": "Newspapers by State",
            "type": "category",
            "url": "usa_states_newspapers.json",
            "icon": "https://img.icons8.com/color/96/map-marker.png"
        },
        {
            "name": "National Newspapers",
            "type": "category",
            "url": "usa_national_newspapers.json",
            "icon": "https://img.icons8.com/color/96/newspaper.png"
        },
        {
            "name": "Live News Channels",
            "type": "category",
            "url": "news_channels.json",
            "icon": "https://img.icons8.com/color/96/retro-tv.png"
        },
        {
            "name": "Latest Video News",
            "type": "YOUTUBE",
            "url": "ALL_VIDEOS",
            "icon": "https://img.icons8.com/color/96/youtube-play.png"
        },
        {
            "name": "Breaking News (RSS)",
            "type": "RSS",
            "url": "ALL_NEWS",
            "icon": "https://img.icons8.com/color/96/rss.png"
        },
        {
            "name": "24/7 US News Radio",
            "type": "radio",
            "url": "news_radio.json",
            "icon": "https://img.icons8.com/color/96/radio-tower.png"
        },
        {
            "name": "News by Category",
            "type": "category",
            "url": "categories.json",
            "icon": "https://img.icons8.com/color/96/categories.png"
        }
    ],
    "ads": {
        "ad_status": True,
        "main_ads": "admob",
        "backup_ads": "none",
        "admob_banner_unit_id": "ca-app-pub-3940256099942544/6300978111",
        "admob_interstitial_unit_id": "ca-app-pub-3940256099942544/1033173712",
        "admob_native_unit_id": "ca-app-pub-3940256099942544/2247696110",
        "admob_app_open_ad_unit_id": "ca-app-pub-3940256099942544/9257395921",
        "interstitial_ad_interval_on_drawer_menu": 4,
        "interstitial_ad_interval_on_web_page_link": 5,
        "native_ad_style_drawer_menu": "medium",
        "native_ad_style_exit_dialog": "medium",
        "native_ad_style_product_list": "medium",
        "native_ad_index": 4,
        "interstitial_ad_on_list_item_click": True,
        "interstitial_ad_interval_on_list_item_click": 4,
        "placement": {
            "banner_home": True,
            "interstitial_drawer_menu": True,
            "interstitial_web_page_link": True,
            "native_drawer_menu": True,
            "native_exit_dialog": True,
            "app_open_ad_on_start": False,
            "app_open_ad_on_resume": False
        }
    },
    "youtube_channels": youtube_source_list,
    "rss_news": rss_source_list
}

with open(os.path.join(assets_dir, "config.json"), "w", encoding="utf-8") as f:
    json.dump(config_data, f, indent=4, ensure_ascii=False)
print("Generated config.json successfully!")
