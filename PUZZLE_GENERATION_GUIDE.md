# Daily Puzzle Generation Guide

This guide shows how to generate daily puzzles for all three games: Sudoku, Zip, and Tango.

## Quick Start

Generate today's puzzles for all games:
```bash
cd backend

# Sudoku
python3 main.py --game-type MINI_SUDOKU_6X6

# Zip
python3 main.py --game-type ZIP

# Tango
python3 main.py --game-type TANGO
```

Or use the helper scripts:
```bash
./generate_tango.sh
```

## Generate for Specific Date

```bash
python3 main.py --game-type TANGO --date 2026-01-30
```

## Force Regeneration

If a puzzle already exists for a date and you want to regenerate it:
```bash
python3 main.py --game-type TANGO --date 2026-01-30 --force
```

## All Games at Once

Generate all three games for today:
```bash
python3 main.py --game-type MINI_SUDOKU_6X6
python3 main.py --game-type ZIP
python3 main.py --game-type TANGO
```

## Testing in Dev

1. Generate test puzzles:
```bash
# In backend directory
python3 main.py --game-type MINI_SUDOKU_6X6 --test
python3 main.py --game-type ZIP --test
python3 main.py --game-type TANGO --test
```

2. Run the app and check the home screen - all three game cards should appear

3. Play each game to verify:
   - ✅ Puzzle loads correctly
   - ✅ Rules are enforced
   - ✅ Timer works
   - ✅ Progress saves
   - ✅ Completion detected
   - ✅ Results submit to leaderboard

## Automated Daily Generation

Set up Cloud Scheduler to generate all three games at 8:00 AM UTC daily:

```bash
# In backend directory
./setup_scheduler.sh
```

This creates three scheduled jobs:
- `daily-sudoku-puzzle` - Generates Mini Sudoku 6×6
- `daily-zip-puzzle` - Generates Zip
- `daily-tango-puzzle` - Generates Tango

All run at 8:00 AM UTC every day.

## Puzzle Format Examples

### Sudoku
```json
{
  "size": 6,
  "blockRows": 2,
  "blockCols": 3,
  "initialBoard": [[0,0,3,0,5,0], ...],
  "solutionBoard": [[1,2,3,4,5,6], ...]
}
```

### Zip
```json
{
  "size": 6,
  "dots": [
    {"row": 0, "col": 1, "index": 1},
    {"row": 2, "col": 3, "index": 2}
  ],
  "walls": [...],
  "solution": [...]
}
```

### Tango
```json
{
  "size": 6,
  "prefilled": [
    {"row": 5, "col": 0, "value": "SUN"},
    {"row": 1, "col": 3, "value": "MOON"}
  ],
  "equalClues": [
    {"row": 0, "col": 4, "direction": "VERTICAL"}
  ],
  "oppositeClues": [
    {"row": 3, "col": 0, "direction": "HORIZONTAL"}
  ]
}
```

## Troubleshooting

### Puzzle Generation Fails
- Check backend logs for validation errors
- Try regenerating with `--force` flag
- Verify Firestore credentials are correct

### Puzzle Doesn't Appear in App
- Check puzzle date matches today's date in UTC
- Verify puzzle was written to Firestore successfully
- Check app logs for loading errors

### Timer/Progress Issues
- Clear app data and try again
- Check device time zone settings
- Verify DataStore is persisting correctly

## Game Rules

### Sudoku (6×6)
- Fill 6×6 grid with numbers 1-6
- Each row, column, and 2×3 block has 1-6 exactly once
- Some numbers are pre-filled (givens)

### Zip
- Connect numbered dots in order (1→2→3...)
- Path must be continuous and orthogonal
- Fill all 36 cells
- Don't cross walls
- Don't revisit cells

### Tango
- Fill 6×6 grid with sun ☀️ and moon 🌙
- Each row/column has exactly 3 of each
- No 3+ consecutive identical symbols
- "=" clues: adjacent cells must match
- "×" clues: adjacent cells must be opposite
- Some cells are pre-filled
