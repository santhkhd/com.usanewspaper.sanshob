
import json

def update_images():
    # Load the source file with images
    print("Loading imdb_final_6150 copy.json...")
    with open('imdb_final_6150 copy.json', 'r', encoding='utf-8') as f:
        source_data = json.load(f)
    
    # Create a lookup dictionary
    image_lookup = {}
    for item in source_data:
        idx = item.get('index')
        img = item.get('image')
        if idx is not None and img:
            image_lookup[idx] = img
            
    print(f"Loaded {len(image_lookup)} images from source.")

    # Load the target file
    print("Loading movie_data.json...")
    with open('movie_data.json', 'r', encoding='utf-8') as f:
        target_data = json.load(f)
        
    updated_count = 0
    
    # Update target data
    for item in target_data:
        idx = item.get('index')
        if idx in image_lookup:
            item['image'] = image_lookup[idx]
            updated_count += 1
            
    print(f"Updated {updated_count} movies with images.")
    
    # Save the updated file
    print("Saving updated movie_data.json...")
    with open('movie_data.json', 'w', encoding='utf-8') as f:
        json.dump(target_data, f, indent=2, ensure_ascii=False)
        
    print("Done!")

if __name__ == "__main__":
    update_images()
