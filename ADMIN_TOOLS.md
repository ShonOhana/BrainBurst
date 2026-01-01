# Admin Tools for BrainBurst

## 🔧 How to Enable Admin Upload Button

The admin upload button is commented out in production but can be easily re-enabled for testing.

### To Enable:

1. Open: `shared/src/commonMain/kotlin/com/brainburst/presentation/home/HomeScreen.kt`
2. Find the section marked: `// ADMIN TOOLS (Development Only)`
3. Uncomment the entire block (remove `/*` and `*/`)
4. Rebuild and run the app
5. You'll see the "🔧 Upload Today's Test Puzzle" button at the bottom of Home screen

### What It Does:

- Uploads a valid 6×6 Sudoku puzzle to Firestore
- Automatically uses today's date
- Creates document: `puzzles/MINI_SUDOKU_6X6_yyyy-MM-dd`
- Works on both iOS and Android

### When to Use:

- **Daily testing**: Generate a new puzzle each day for testing
- **Multiple testers**: Each tester needs their own puzzle
- **Demo purposes**: Quickly add puzzles for presentations
- **Before Phase 8**: Manual puzzle generation until backend is ready

### The Test Puzzle:

```
Initial Board (0 = empty):
1 _ 3 _ 5 _
_ 5 _ 1 _ 3
2 _ 4 _ 6 _
_ 6 _ 2 _ 4
3 _ 5 _ 1 _
_ 1 _ 3 _ 5

Solution:
1 2 3 4 5 6
4 5 6 1 2 3
2 3 4 5 6 1
5 6 1 2 3 4
3 4 5 6 1 2
6 1 2 3 4 5
```

### Important Notes:

- ⚠️ **Remove before production release!**
- Only shows in development (commented out by default)
- Each puzzle can only be played once per user per day
- Overwrites existing puzzle if you run it multiple times on same day

---

## 🔮 Future: Phase 8 Automated Generation

Once Phase 8 is complete, puzzles will be generated automatically:
- Python Cloud Function with OpenAI
- Triggered daily at midnight UTC
- No manual intervention needed
- This admin button can be removed entirely

---

**Current Status**: Admin button commented out ✅
**To re-enable**: Uncomment the block in `HomeScreen.kt`




