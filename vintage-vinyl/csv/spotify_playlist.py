import requests
import csv
import os

# Spotify API credentials
client_id = 'bf1fb094f5bd47c1bb11c3f81b1f0cd2'
client_secret = 'f3cc5b671f5749aa89ac00897b357706'

# Function to get Access Token from Spotify
def get_access_token(client_id, client_secret):
    url = 'https://accounts.spotify.com/api/token'
    headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    }
    data = {
        'grant_type': 'client_credentials',
        'client_id': client_id,
        'client_secret': client_secret
    }
    
    response = requests.post(url, headers=headers, data=data)
    
    token_info = response.json()
    
    # Check if access token is in the response
    if 'access_token' in token_info:
        return token_info['access_token']
    else:
        raise Exception("Failed to retrieve access token. Response was: " + response.text)

# Function to get artist genre from Spotify API
def get_artist_genre(artist_id, access_token):
    url = f"https://api.spotify.com/v1/artists/{artist_id}"
    headers = {
        'Authorization': f'Bearer {access_token}'
    }
    response = requests.get(url, headers=headers)
    data = response.json()
    
    # Return the first genre found or "Unknown" if not available
    if 'genres' in data and len(data['genres']) > 0:
        return ', '.join(data['genres'])  # Multiple genres are joined with a comma
    return "Unknown"

# Function to get all playlist tracks, handling pagination
def get_all_playlist_tracks(playlist_id, access_token):
    tracks = []
    url = f'https://api.spotify.com/v1/playlists/{playlist_id}/tracks'
    headers = {
        'Authorization': f'Bearer {access_token}'
    }

    while url:
        response = requests.get(url, headers=headers)
        data = response.json()
        tracks.extend(data['items'])
        url = data['next'] 

    return tracks

# Function to download album cover image to a local folder
def download_image(image_url, track_name):
    # Create a folder to store images
    if not os.path.exists('album_covers'):
        os.makedirs('album_covers')
    
    response = requests.get(image_url)
    if response.status_code == 200:
        # Ensure the filename is valid
        safe_track_name = "".join([c if c.isalnum() else "_" for c in track_name])
        file_path = f"album_covers/{safe_track_name}.jpg"
        with open(file_path, 'wb') as file:
            file.write(response.content)
        print(f"Downloaded: {safe_track_name}.jpg")
    else:
        print(f"Failed to download image for {track_name}")

# Function to save all tracks into a CSV file and download album cover images
def save_all_tracks_to_csv_with_images(tracks, access_token, file_name='playlist_tracks_with_images_and_genres.csv'):
    with open(file_name, mode='w', newline='') as file:
        writer = csv.writer(file)
        # Write the CSV header
        writer.writerow(['Track Name', 'Artist', 'Album', 'Release Date', 'Cover Image URL', 'Genre'])

        for item in tracks:
            track = item['track']
            track_name = track['name']
            artist_name = track['artists'][0]['name']
            album_name = track['album']['name']
            release_date = track['album']['release_date']

            # Get album cover image URL
            cover_image_url = track['album']['images'][0]['url'] if track['album']['images'] else 'No image available'

            # Get artist ID and then fetch genre from Spotify API
            artist_id = track['artists'][0]['id']
            genre = get_artist_genre(artist_id, access_token)

            # Write the data to CSV
            writer.writerow([track_name, artist_name, album_name, release_date, cover_image_url, genre])

            # Download the album cover image
            if cover_image_url != 'No image available':
                download_image(cover_image_url, track_name)

if __name__ == "__main__":
    # Get the access token using the client ID and client secret
    access_token = get_access_token(client_id, client_secret)

    # Playlist ID (from the Spotify URL)
    playlist_id = '2LDh9QxwqkTrO3StGJOBk4'  

    # Fetch all tracks (handle pagination)
    all_tracks = get_all_playlist_tracks(playlist_id, access_token)

    # Save all tracks into a CSV file and download cover images
    save_all_tracks_to_csv_with_images(all_tracks, access_token)

    print("CSV file created and cover images downloaded!")
