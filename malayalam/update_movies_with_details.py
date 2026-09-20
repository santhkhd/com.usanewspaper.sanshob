import json
import requests
import time
from urllib.parse import quote_plus

# List of API keys to rotate through (using the valid key we found)
API_KEYS = [
    "b5c868a4",  # Valid key found in update_2025_movies.py
    "e8839cc3",
    "c459749d",
    "e43e073e",
    "86fe17c",
    "b34e386c",
    "6c2d2f59",
    "e6514a3a",
    "1916b9ca"
]

def fetch_movie_details(movie_title, movie_year=None, api_key_index=0):
    """
    Fetch full movie details using OMDB API
    """
    # Using rotating API keys
    api_key = API_KEYS[api_key_index % len(API_KEYS)]
    
    # Construct the API URL
    if movie_year:
        url = f"http://www.omdbapi.com/?t={quote_plus(movie_title)}&y={movie_year}&apikey={api_key}&type=movie"
    else:
        url = f"http://www.omdbapi.com/?t={quote_plus(movie_title)}&apikey={api_key}&type=movie"
    
    try:
        response = requests.get(url)
        if response.status_code == 200:
            data = response.json()
            if data.get('Response') == 'True':
                # Extract details
                cast = data.get('Actors', '')
                director = data.get('Director', '')
                genre = data.get('Genre', '')
                plot = data.get('Plot', '')
                runtime = data.get('Runtime', '')
                
                details = {}
                
                if cast and cast != 'N/A':
                    details['cast'] = [actor.strip() for actor in cast.split(',')]
                else:
                    details['cast'] = []
                    
                if director and director != 'N/A':
                    details['director'] = [d.strip() for d in director.split(',')]
                else:
                    details['director'] = []
                    
                if genre and genre != 'N/A':
                    details['genre'] = [g.strip() for g in genre.split(',')]
                
                if plot and plot != 'N/A':
                    details['plot'] = plot
                    
                if runtime and runtime != 'N/A':
                    details['runtime'] = runtime
                
                return details
            else:
                # Movie not found
                print(f"  Movie not found in OMDB: {data.get('Error', 'Unknown error')}")
                return None
        # Check if we hit the API limit
        elif response.status_code == 429:
            print(f"API limit reached for key {api_key}.")
            return "LIMIT_REACHED"
        else:
            print(f"HTTP Error {response.status_code} for {movie_title} with key {api_key}")
            # If it's an authentication error (401), try the next key
            if response.status_code == 401:
                return "LIMIT_REACHED"
            return None
    except Exception as e:
        print(f"Error fetching data for {movie_title}: {e}")
        return None

def update_movies_with_details(input_file, output_file, delay=0.1, max_requests=None):
    """
    Update movies JSON file with full details and remove images
    Processes movies in descending order by year
    """
    # Load the existing movies data
    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            movies = json.load(f)
    except FileNotFoundError:
        print(f"Error: File {input_file} not found.")
        return
    except json.JSONDecodeError:
        print(f"Error: Invalid JSON in file {input_file}.")
        return
    
    # Sort movies by year in descending order (newest first)
    movies.sort(key=lambda x: str(x.get('year', '')) or '', reverse=True)
    
    print(f"Loaded {len(movies)} movies from {input_file}")
    
    if max_requests is None:
        max_requests = len(movies)
    
    print(f"Will process up to {max_requests} movies using {len(API_KEYS)} API keys")
    
    # Process movies
    updated_movies = []
    requests_made = 0
    current_api_key_index = 0
    skipped_count = 0
    
    for i, movie in enumerate(movies):
        # Remove image field as requested
        if 'image' in movie:
            del movie['image']
            
        if requests_made >= max_requests:
            # Continue to next movie to remove images from remaining ones
            updated_movies.append(movie)
            continue
            
        # Check if we should fetch (if cast or director is missing)
        has_cast = 'cast' in movie and movie['cast']
        has_director = 'director' in movie and movie['director']
        
        if has_cast and has_director:
            print(f"Processing {i+1}/{len(movies)}: {movie.get('title')} ({movie.get('year')}) - Already has details, skipping API call")
            updated_movies.append(movie)
            skipped_count += 1
            continue

        print(f"Processing {i+1}/{len(movies)}: {movie.get('title')} ({movie.get('year')}) with API key {current_api_key_index + 1}")
        
        # Fetch movie details
        title = movie.get('title', '')
        year = movie.get('year', '')
        
        if title:
            details = fetch_movie_details(title, year, current_api_key_index)
            
            # Handle API limit reached
            while details == "LIMIT_REACHED":
                current_api_key_index = (current_api_key_index + 1) % len(API_KEYS)
                print(f"Switching to API key {current_api_key_index + 1}: {API_KEYS[current_api_key_index]}")
                details = fetch_movie_details(title, year, current_api_key_index)
                
                if current_api_key_index == 0:
                    print("All API keys reached limit. Saving progress.")
                    break
            
            if details == "LIMIT_REACHED":
                # Add current movie and mark it as processed from here on for image removal only
                updated_movies.append(movie)
                requests_made = max_requests # Trigger skip for API calls for the rest
                continue
                
            if details:
                # Update movie with new details
                movie.update(details)
                print(f"  Updated: Cast({len(movie.get('cast', []))}), Director({len(movie.get('director', []))})")
                requests_made += 1
            else:
                print(f"  No details found")
                # Ensure fields exist
                if 'cast' not in movie: movie['cast'] = []
                if 'director' not in movie: movie['director'] = []
        
        updated_movies.append(movie)
        
        # Add a small delay
        if i < len(movies) - 1 and requests_made < max_requests:
            time.sleep(delay)
            
        # Periodic save
        if requests_made > 0 and requests_made % 100 == 0:
            try:
                with open(output_file, 'w', encoding='utf-8') as f:
                    json.dump(updated_movies + [m for m in movies[i+1:]], f, indent=2, ensure_ascii=False)
                print(f"  Progress saved: {i+1} total processed, {requests_made} requests made")
            except Exception as e:
                print(f"  Error saving: {e}")
    
    # Save the final data
    try:
        # Before final save, make sure ALL movies had their image removed (in case we broke early)
        for m in updated_movies:
            if 'image' in m:
                del m['image']
                
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(updated_movies, f, indent=2, ensure_ascii=False)
        print(f"\nUpdated movies data saved to {output_file}")
        print(f"Processed: {len(updated_movies)} movies")
        print(f"API requests made: {requests_made}")
    except Exception as e:
        print(f"Error saving file: {e}")

# Run the update
if __name__ == "__main__":
    print("Starting Movie Details Update (Full Data, No Images)...")
    update_movies_with_details("imdb_final_6150.json", "imdb_final_6150.json")
