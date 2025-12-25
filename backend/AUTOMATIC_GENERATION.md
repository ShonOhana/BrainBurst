# Automatic Daily Puzzle Generation

## 🔄 Current Status: **Manual Only**

Right now, puzzles are **NOT** generated automatically. You need to:

### Option 1: Manual Generation (Current)
```bash
cd backend
source venv/bin/activate
python main.py --test  # Generate today's puzzle
```

### Option 2: Automatic Generation (Requires Setup)

To make it automatic, you need to:

#### Step 1: Deploy Cloud Function
Deploy the backend to Google Cloud Functions so it can run automatically.

#### Step 2: Setup Cloud Scheduler
Configure a scheduled job to call the function every day at 9:00 AM.

---

## ⏰ How Automatic Generation Will Work

Once deployed:

1. **Cloud Scheduler** triggers daily at **9:00 AM UTC**
2. **Cloud Function** runs automatically
3. **OpenAI** generates a new puzzle
4. **Puzzle** saved to Firestore
5. **Users** see new puzzle in app!

### No Manual Intervention Needed! 🎉

---

## 📋 Setup Steps (When You're Ready)

### 1. Deploy Cloud Function

```bash
cd backend
gcloud functions deploy generate_daily_puzzle \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=generate_daily_puzzle \
  --trigger-http \
  --allow-unauthenticated \
  --set-env-vars OPENAI_API_KEY=your-key-here
```

### 2. Setup Cloud Scheduler

```bash
gcloud scheduler jobs create http daily-puzzle-sudoku \
  --location=us-central1 \
  --schedule="0 9 * * *" \
  --uri="https://YOUR_FUNCTION_URL" \
  --http-method=POST \
  --message-body='{"gameType":"MINI_SUDOKU_6X6"}' \
  --time-zone="UTC"
```

This runs **every day at 9:00 AM UTC**.

### 3. Test It

```bash
# Test the scheduler manually
gcloud scheduler jobs run daily-puzzle-sudoku --location=us-central1
```

---

## ⏰ Time Zone Notes

- **Current schedule**: 9:00 AM UTC
- **To change time zone**: Modify `--time-zone="America/New_York"` or your preferred timezone
- **Common timezones**:
  - `UTC` (default)
  - `America/New_York` (Eastern)
  - `America/Los_Angeles` (Pacific)
  - `Europe/London` (UK)

---

## ✅ Summary

**Now**: Manual generation only
**After deployment**: Fully automatic at 9 AM daily
**Cost**: ~$0.04/year for OpenAI + FREE for Cloud Functions/Scheduler

---

**Next Steps**: 
1. Fix OpenAI quota issue (add billing)
2. Test puzzle generation locally
3. Deploy to Cloud Functions (optional, for automation)
4. Setup Cloud Scheduler (optional, for automation)

