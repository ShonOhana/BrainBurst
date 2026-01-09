# Firestore Index Setup for BrainBurst

## The Error You're Seeing:
```
FAILED_PRECONDITION: The query requires an index.
```

## Why This Happens:
Firestore requires a composite index when you:
- Filter by one field (`puzzleId`)
- Sort by another field (`durationMs`)

## How to Fix:

### Option 1: Use the Error Link (Easiest!)

1. Copy the URL from the error message
2. Paste in browser
3. Click "Create Index"
4. Wait 2-3 minutes for it to build
5. Retry in the app

### Option 2: Create Manually

1. Go to: **Firebase Console** → **Firestore Database** → **Indexes** tab

2. Click **"Create Index"**

3. Fill in:
   ```
   Collection ID: results
   
   Fields to index:
   - Field: puzzleId
     Query scope: Collection
     Order: Ascending
   
   - Field: durationMs
     Query scope: Collection
     Order: Ascending
   ```

4. Click **"Create"**

5. Wait 2-3 minutes (you'll see "Building..." status)

6. Retry in app

### Option 3: Use firestore.indexes.json (For Deployment)

Create this file if deploying with Firebase CLI:

```json
{
  "indexes": [
    {
      "collectionGroup": "results",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "puzzleId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "durationMs",
          "order": "ASCENDING"
        }
      ]
    }
  ],
  "fieldOverrides": []
}
```

## What This Index Does:

Allows the leaderboard query:
```kotlin
firestore.collection("results")
    .where("puzzleId", isEqualTo, "MINI_SUDOKU_6X6_2025-12-25")
    .orderBy("durationMs", Direction.ASCENDING)
    .limit(100)
```

This query finds all results for today's puzzle and sorts them by completion time (fastest first).

## How Long Does It Take?

- **Small data**: ~30 seconds
- **Your case** (1-2 results): ~1 minute
- **Large data**: Up to 5 minutes

You'll see a "Building" status in Firebase Console.

## After Index is Created:

✅ Leaderboard will load instantly
✅ No more errors
✅ Query will be fast (even with thousands of results)

## Future Indexes Needed:

If you add more game types or leaderboard filters, you might need additional indexes. Firebase will always tell you with a similar error + link to create them.

---

**Current Status**: Waiting for you to create the index 🔧
**Next Step**: Click the link in your app or create manually in Firebase Console





