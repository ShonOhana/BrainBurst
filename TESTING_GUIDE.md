# BrainBurst Testing Guide

## Quick Start Testing

### Step 1: Add Test Puzzle to Firestore

1. **Open Firebase Console:**
   - Go to https://console.firebase.google.com
   - Select your BrainBurst project
   - Navigate to Firestore Database

2. **Create the puzzle document:**
   - Go to the `puzzles` collection (create if it doesn't exist)
   - Click "Add document"
   - **Document ID:** `MINI_SUDOKU_6X6_2025-12-25` (⚠️ Change date to TODAY)
   - Click "Add field" for each field below:

3. **Add these fields:**

```
gameType: "MINI_SUDOKU_6X6" (string)
date: "2025-12-25" (string) ⚠️ USE TODAY'S DATE in yyyy-MM-dd format
puzzleId: "MINI_SUDOKU_6X6_2025-12-25" (string) ⚠️ USE TODAY'S DATE
payload: (map)
  ├─ size: 6 (number)
  ├─ blockRows: 2 (number)
  ├─ blockCols: 3 (number)
  ├─ initialBoard: (array)
  │   [0]: (array) [1, 0, 3, 0, 5, 0]
  │   [1]: (array) [0, 5, 0, 1, 0, 3]
  │   [2]: (array) [2, 0, 4, 0, 6, 0]
  │   [3]: (array) [0, 6, 0, 2, 0, 4]
  │   [4]: (array) [3, 0, 5, 0, 1, 0]
  │   [5]: (array) [0, 1, 0, 3, 0, 5]
  └─ solutionBoard: (array)
      [0]: (array) [1, 2, 3, 4, 5, 6]
      [1]: (array) [4, 5, 6, 1, 2, 3]
      [2]: (array) [2, 3, 4, 5, 6, 1]
      [3]: (array) [5, 6, 1, 2, 3, 4]
      [4]: (array) [3, 4, 5, 6, 1, 2]
      [5]: (array) [6, 1, 2, 3, 4, 5]
```

**OR** use the Firebase Console's "Import JSON" feature with `sample_puzzle_firestore.json` (remember to update the date!)

### Step 2: Set Up Firestore Indexes (Optional but Recommended)

For better performance, add these composite indexes:

1. **For leaderboard queries:**
   - Collection: `results`
   - Fields: `puzzleId` (Ascending), `durationMs` (Ascending)

2. **For user completion checks:**
   - Collection: `results`
   - Fields: `userId` (Ascending), `puzzleId` (Ascending)

Firebase will prompt you to create these indexes when you first run queries that need them.

### Step 3: Run the App

#### Android:
```bash
./gradlew :androidApp:installDebug
# OR open Android Studio and click Run
```

#### iOS:
```bash
cd iosApp
pod install
open iosApp.xcworkspace
# Run from Xcode
```

### Step 4: Test the Complete Flow

1. ✅ **Launch & Auth:**
   - App opens to splash screen
   - Navigates to auth screen
   - Sign up with email/password or Google (Android)

2. ✅ **Home Screen:**
   - See your display name in the top bar
   - See Mini Sudoku 6×6 card with "Play Now" chip
   - See Zip and Tango cards with "Coming Soon"

3. ✅ **Play Puzzle:**
   - Click on Mini Sudoku card
   - See the puzzle grid with some numbers pre-filled
   - Timer starts at 00:00
   - Click a blank cell to select it (turns blue)
   - Use number buttons (1-6) to fill cells
   - Try entering wrong numbers - conflicts turn red
   - Use "Erase" to clear selected cell
   - Fill all cells completely

4. ✅ **Submit:**
   - "Submit Solution" button becomes enabled
   - Click Submit
   - If correct: Returns to home, card shows "Completed"
   - If wrong: Shows error message to check your answers

5. ✅ **Completion:**
   - Navigate back to home
   - Mini Sudoku card now shows "Completed" with your time
   - Can't play again (one puzzle per day)

6. ✅ **Check Firestore:**
   - Open Firebase Console → Firestore
   - Check `results` collection
   - Should see your result with userId, durationMs, movesCount

### Expected Behavior

#### First Time Playing (Today):
- Home: "Play Now" chip visible
- Can click and play puzzle
- After solving: Result saved to Firestore

#### After Completing (Same Day):
- Home: "Completed" chip visible
- Card is disabled (can't play again)
- Shows your completion time

#### Next Day:
- Home: "Play Now" chip visible again
- Need to add new puzzle with new date
- Can play fresh puzzle

## 🐛 Troubleshooting

### "No puzzle available for today"
- ✅ Make sure the puzzle document date matches today's date
- ✅ Document ID must be: `MINI_SUDOKU_6X6_yyyy-MM-dd`
- ✅ Check Firestore rules allow reads

### "Failed to load puzzle"
- ✅ Check Firebase initialization (google-services.json / GoogleService-Info.plist)
- ✅ Check internet connection
- ✅ Check Firestore rules
- ✅ Verify puzzle document structure

### Can't click cells
- ✅ Pre-filled cells (bold numbers) are not editable
- ✅ Only empty cells or user-filled cells can be edited

### Submit button disabled
- ✅ All 36 cells must be filled (no zeros)
- ✅ Check for red highlighted invalid entries

### Validation errors
- ✅ Each row must have 1-6 without duplicates
- ✅ Each column must have 1-6 without duplicates
- ✅ Each 2×3 block must have 1-6 without duplicates

## 📱 Quick Test Scenarios

### Scenario 1: Valid Solution
1. Fill all cells correctly
2. Submit → Success, saved to Firestore
3. Home shows "Completed"

### Scenario 2: Invalid Solution
1. Fill all cells with duplicates
2. See red highlighting
3. Submit → Error message
4. Can fix and resubmit

### Scenario 3: Incomplete Puzzle
1. Leave some cells empty
2. Submit button disabled
3. Must fill all cells first

### Scenario 4: Daily Gating
1. Complete puzzle
2. Go to home
3. Try to play again → Can't (completed)
4. Change device date to tomorrow
5. Add puzzle for tomorrow's date
6. Can play new puzzle

## 🎮 Solution to Test Puzzle

If you want to quickly test submission, here's the solution:

```
Row 1: 1 2 3 4 5 6
Row 2: 4 5 6 1 2 3
Row 3: 2 3 4 5 6 1
Row 4: 5 6 1 2 3 4
Row 5: 3 4 5 6 1 2
Row 6: 6 1 2 3 4 5
```

Pre-filled cells (don't change):
- Row 1: 1, 3, 5
- Row 2: 5, 1, 3
- Row 3: 2, 4, 6
- Row 4: 6, 2, 4
- Row 5: 3, 5, 1
- Row 6: 1, 3, 5

Fill in the blanks with the solution above!

## 🔥 Firestore Rules (For Testing)

For development, use these permissive rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow all authenticated users to read puzzles
    match /puzzles/{puzzleId} {
      allow read: if request.auth != null;
    }
    
    // Allow users to create their own results
    match /results/{resultId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && 
                     request.resource.data.userId == request.auth.uid;
    }
    
    // Allow users to read/write their own user document
    match /users/{userId} {
      allow read, write: if request.auth != null && 
                           request.auth.uid == userId;
    }
  }
}
```

⚠️ **Important:** Update to more restrictive rules for production!

---

Happy Testing! 🎉




