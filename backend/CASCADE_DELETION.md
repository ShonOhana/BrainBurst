# Cascade Deletion: Puzzles & User Results

## ✅ What Happens When a New Puzzle is Generated

When the daily puzzle generator runs, it performs **cascade deletion** to maintain data consistency:

### Step-by-Step Process:

1. **Generate New Puzzle** → Creates today's puzzle
2. **Delete Old Puzzles** → Removes all previous puzzles for this game type
3. **Delete Associated Results** → Removes all user results linked to deleted puzzles
4. **Keep Only Today's Data** → Only today's puzzle remains (results will accumulate during the day)

## 🗑️ What Gets Deleted

### Puzzles Collection:
- ❌ All old puzzles of the same game type (e.g., `MINI_SUDOKU_6X6_2025-01-09`)
- ✅ **Keeps:** Today's puzzle (e.g., `MINI_SUDOKU_6X6_2025-01-10`)

### Results Collection:
- ❌ All user results for deleted puzzles
- ✅ **Keeps:** Results for today's puzzle (accumulates as users complete it)

## 📊 Example Scenario

### Before Generation (January 10, 2025):
```
Puzzles:
  ├─ MINI_SUDOKU_6X6_2025-01-09 (yesterday)
  
Results:
  ├─ user1 → MINI_SUDOKU_6X6_2025-01-09 (42 seconds)
  ├─ user2 → MINI_SUDOKU_6X6_2025-01-09 (58 seconds)
  └─ user3 → MINI_SUDOKU_6X6_2025-01-09 (75 seconds)
```

### After Generation (January 10, 2025):
```
Puzzles:
  ├─ MINI_SUDOKU_6X6_2025-01-10 (today - NEW!)
  
Results:
  └─ (empty - will fill as users complete today's puzzle)
```

## 🔧 Implementation

### In `firestore_writer.py`:

```python
def delete_old_puzzles(self, game_type: str, keep_date: str) -> int:
    """
    Delete all puzzles for a game type except the one for keep_date.
    Also deletes all associated user results.
    """
    # For each old puzzle:
    #   1. Delete all results (cascade)
    #   2. Delete the puzzle itself
    
def _delete_results_for_puzzle(self, puzzle_id: str) -> int:
    """
    Delete all user results associated with a specific puzzle.
    Queries: results WHERE puzzleId == puzzle_id
    """
```

## 💡 Why Cascade Deletion?

### Benefits:
1. **Data Consistency** → No orphaned results referencing non-existent puzzles
2. **Clean Database** → No accumulation of old data
3. **Storage Efficiency** → Reduces Firestore storage costs
4. **Leaderboard Accuracy** → Old leaderboards don't show for deleted puzzles
5. **Fresh Daily Experience** → Each day is a clean slate for all users

### Daily Puzzle App Design:
- Each puzzle is meant for **one day only**
- Users compete on **today's leaderboard**
- Yesterday's results are **no longer relevant**
- Fresh competition every day

## 🛡️ Safety Features

### Duplicate Prevention:
- Won't regenerate if puzzle already exists for the date
- Only one puzzle per day per game type

### Protected Data:
- **Keeps:** Today's puzzle and all its results
- **Deletes:** Only old puzzles and their results

## 📝 Logging

The system logs all deletions:
```
🗑️  Deleted old puzzle: MINI_SUDOKU_6X6_2025-01-09 (3 results)
✅ Deleted 1 old puzzle(s) and 3 result(s), kept: MINI_SUDOKU_6X6_2025-01-10
```

## ⚠️ Important Notes

1. **Automatic Cleanup**: Happens every time a new puzzle is generated
2. **Data Loss**: Old results are permanently deleted (this is intentional!)
3. **No History**: The app doesn't maintain historical puzzle data
4. **Daily Competition**: Each day is independent

## 🔄 When This Runs

### Automatic (Production):
- Daily at 9:00 AM UTC via Cloud Scheduler
- Runs automatically, no manual intervention

### Manual (Testing):
```bash
cd backend
source venv/bin/activate
python main.py --test  # Generates today's puzzle, cleans old ones
```

## 🎯 Summary

**One puzzle per day, one set of results per day.**
- Clean, simple, and efficient
- No data confusion
- Fresh competition daily
- Automatic maintenance

---

**Status**: ✅ Implemented and Active
**Date**: January 2026

