# Juan's Hi-Fi

**An Android music player for a self-hosted Navidrome server, designed like a thrift-shop record store.**

Instead of a generic streaming-app layout, every screen borrows from a real record shop: weekly mixes sit in an old fruit crate stamped *No.19*, albums live in a red milk crate, genres in a blue one, and the player is a turntable whose needle lifts and drops as you pause and play.

> **Status: v0.1.0 — working prototype (pre-release).** Core playback, browsing and favorites work end to end. The design is still evolving; see the roadmap below.

---

## Features

**Home**
- Greeting and "shelf" that change with the time of day (morning, afternoon, evening, late night)
- A coffee ring that fades through the afternoon, replaced by a tea bag in the evening
- **This week's bins:** three mixes standing crooked in a fruit crate. Tap one to pull the record out, flip the sleeve to read the tracklist, play it, and put it back in its slot
- **Recently played** as vinyl records that spin with the page's scroll speed and direction
- **Album of the day**, **Fresh from the converter** (songs added in the last 30 days; the section disappears on its own when nothing new arrives) and a time-of-day shelf
- Slide-out side menu styled as the shop floor

**Now Playing**
- A turntable: on pause the needle lifts, the record slides back into its sleeve and the sleeve moves to center; on play the sequence reverses and the needle drops last
- Progress bar with tap-to-seek, previous / play-pause / next
- **♥** favorites the song; tapping the price sticker slaps a **MINE** sticker on it and favorites the whole album (both saved to Navidrome)

**Albums (red milk crate)**
- Grid of every album, with search and A–Z / artist sorting
- Tapping a cover lifts the sleeve into the album page without resizing it; play, repeat album, shuffle, or start from any track

**Genres (blue milk crate)**
- One vinyl record per genre; the tapped record flies to the header and spins while the genre's albums and songs load

**Everywhere**
- A wooden mini player with a spinning record that keeps playing across screens
- Shop-entrance login screen; only a Subsonic login token is stored on the device, never the password

---

## How it's built

| Area | Tools |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Playback | AndroidX Media3 (ExoPlayer) |
| Navigation | Navigation Compose, with custom slide transitions |
| Images | Coil |
| Server | [Navidrome](https://www.navidrome.org/) via the Subsonic API (token + salt authentication) |
| Storage | SharedPreferences (app-private) for the session |

Everything is drawn in code: the wood grain, crates, records, stains and tonearm are Compose `Canvas` drawings and layered modifiers, with no image assets.

**Project layout**

| File | What it does |
|---|---|
| `Navidrome.kt` | Subsonic API calls, login/session, song and album models |
| `Turntable.kt` | The single shared player and its live state (current song, favorites, repeat) |
| `HomeScreen.kt` | Home page, side menu |
| `Crate.kt` | The weekly-bins crate and the pull-out / flip / play animation |
| `NowPlayingScreen.kt` | Turntable screen |
| `AlbumsScreen.kt`, `GenresScreen.kt` | Browse pages and their detail views |
| `MiniPlayer.kt`, `Vinyl.kt`, `Theme.kt` | Shared pieces: mini player, record/wood/stain drawings, colors and fonts |
| `LoginScreen.kt` | Shop-entrance login |

---

## Setup

1. Run a [Navidrome](https://www.navidrome.org/) server reachable from your phone (HTTPS recommended).
2. Clone this repo and open it in Android Studio.
3. Download these fonts from Google Fonts and place them in `app/src/main/res/font` with these names:
   `fraunces_extrabold.ttf`, `dmsans_regular.ttf`, `dmsans_bold.ttf`, `plexmono_regular.ttf`, `plexmono_semibold.ttf`, `alfaslab_regular.ttf`, `permanentmarker_regular.ttf`, `knewave_regular.ttf`
4. Run on a device, then sign the guest book with your server address, username and password.

---

## Testing & QA

The app was tested on a real phone throughout development by recording the screen, reviewing the recordings frame by frame, fixing the cause, and retesting. Issues found and fixed:

| # | Issue found | Cause | Fix |
|---|---|---|---|
| 1 | Crate records sat too far left on narrower phones | Positions were fixed for a 390dp-wide screen | Positions are now offsets from the crate's center |
| 2 | Pulling a record had a stop-and-go "bump" | Two separate animations, each easing to a full stop | Merged into one continuous animation with derived phases |
| 3 | App froze after putting a record back | The animation ran in a scope that was disposed halfway, leaving a `busy` flag stuck | Scope moved to a long-lived owner, plus `try/finally` so state always resets |
| 4 | Half-second delay before a record went back | Waiting on "undo" animations that had nothing to undo | Only run animations that actually need to move |
| 5 | Record appeared to resize mid-animation | Switching between two renderers mid-motion dropped frames | One renderer for the whole motion; the crate's copy is hidden with alpha instead of being removed |
| 6 | Top of the record cut off by the screen edge | The record had to rise its full height to clear the records in front | Front records dip behind the crate's front board, halving the lift |
| 7 | Visible edge of a crooked record chopped off | Clipping used a straight line across the full width | Clip out the exact shapes of the records in front instead |
| 8 | Visible wait when opening a page from the menu | The menu finished closing before the page opened | The page opens immediately over the menu; the menu closes out of sight |
| 9 | Now Playing layout collapsed after an edit | A misplaced closing bracket moved content into the deck's layer stack | Restored the correct structure |
| 10 | Album favorite failed silently | Errors only displayed on a screen that wasn't visible | Errors now show on Now Playing too, with a clear message when data is missing |

---

## Roadmap

- **Weekly mixes:** real genre-based mixes generated every Monday at 3 a.m. Eastern
- **Better tags:** upgrade the companion YouTube-to-MP3 converter to tag genre, artist, album and year, so Genres and mixes have proper data
- **Favorites with real effects:** hearted songs more likely to appear in mixes; perks for albums marked MINE
- **Sticker variety:** the MINE sticker lands at a slightly different angle each time
- **Popularity sticker:** the price sticker reflects how popular an album is (Last.fm listeners)
- **Liner notes** from Wikipedia / MusicBrainz
- **Artists, Search and Favorites** pages
- **Radio:** tune in to a station mid-song
- **Background playback** with lock-screen controls
- **Late Set:** a dark, jazz-label theme
- Design tweaks (including the evening tea bag)
- Your own playlists
