import pandas as pd

file_path = 'playlist_tracks_with_images_and_genres.csv'
df = pd.read_csv(file_path)

def assign_price_by_year(release_year):
    try:
        year = int(release_year[:4])  
        if year < 1980:
            return 29.99  
        elif 1980 <= year < 2000:
            return 27.99  
        else:
            return 24.99 
    except:
        return 21.99  

df['Price'] = df['Release Date'].apply(assign_price_by_year)

output_file_path = 'playlist_tracks_with_prices.csv'
df.to_csv(output_file_path, index=False)

output_file_path
