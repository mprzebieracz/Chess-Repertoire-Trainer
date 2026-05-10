# Chess Repertoire Trainer

A personal Android app for building and mastering chess opening repertoires — with spaced-repetition drilling, Stockfish analysis, opening-tree exploration from your own games, and a daily puzzle.

---

## Repertoire

The core of the app. Organize your opening theory as **courses → chapters → lines**, then learn and drill them until they stick.

### Building a repertoire
- Create a course for any opening (e.g. *Sicilian Defense — Black*, *London System — White*)
- Add chapters to group related lines (e.g. *vs. 2...d6*, *vs. 2...Nc6*)
- Record lines move by move on the interactive board, or **import any PGN** to populate a chapter instantly
- Edit and reorder lines at any time; annotate them with comments

### Learning
Walk through a chapter line by line. The board shows the position after each move, letting you absorb the ideas before being tested.

### Training
The app plays the opponent's side — you must recall and play each correct reply from memory. Wrong moves are rejected; hints and the full solution are available if you're stuck. Training sessions are scheduled using the **SM-2 spaced repetition algorithm**, so lines you know well appear less often and shaky lines resurface until they're solid.

### Review with engine
Go through a chapter's lines with the Stockfish engine running alongside. The evaluation bar and principal variation update in real time as you step through moves, letting you understand the key ideas and spot improvements.

---

## Analysis Board

A free-form board for exploring any position — no repertoire required. Make moves freely, step forward and back through your line, flip the board, or reset to the starting position. With Stockfish enabled, the engine runs continuously and shows the score and best continuation above the board.

You can also jump from any repertoire position directly into the analysis board to investigate a specific moment in depth.

---

## Opening Tree

Build a personal opening tree from your actual games on **Lichess** or **Chess.com** — no login needed, just enter your username.

- Filter by color (White or Black) and time control (Bullet, Blitz, Rapid, Classical)
- Navigate the tree position by position on a real board
- See how many times you've reached each position and your win/draw/loss record at that branch
- Use it to spot where you go wrong, find gaps in your repertoire, or discover which openings you actually play

---

## Daily Puzzle

Fetches today's puzzle from Lichess. A hint is available if needed, and the solution can be revealed at any time. Once solved, it marks itself complete for the day.

---

## My Games

Browse and replay your imported game history. Open any game to step through it move by move, optionally with the engine running alongside.

---

## Stockfish Engine

All analysis features — the evaluation bar, score label, and principal variation — are powered by a native Stockfish build. Depth, search time, and thread count are all configurable from Settings. The engine is optional: the app works fully without it, and analysis panels gracefully indicate when it's unavailable.

---

## Themes

Three app color themes and five board themes, all switchable from Settings.

**App themes**

| Theme | Feel | Accent |
|---|---|---|
| Dark Wood | chess.com-style dark | Warm gold |
| Lichess Green | Lichess dark | Signature green |
| Warm Parchment | Light, classic | Library red |

**Board themes:** Classic Green · Blue · Brown · Tournament (red) · Night (slate blue)

---

## Building

```bash
./gradlew assembleDebug
```

Requires Android Studio with the Android SDK. Min SDK 26 (Android 8.0), target SDK 35.

### Enabling Stockfish

Place a compiled `libstockfish.so` (arm64-v8a) at `app/src/main/jniLibs/arm64-v8a/libstockfish.so` and rebuild. Without it the app runs fully — only engine analysis is unavailable.
