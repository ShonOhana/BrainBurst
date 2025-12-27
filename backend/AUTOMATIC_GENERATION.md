# Automatic Daily Puzzle Generation

## 🚀 Quick Start: Deploy Now!

**Ready to deploy?** Follow the complete guide:
👉 **[DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)** 👈

Or use the automated scripts:

```bash
cd backend

# Step 1: Deploy Cloud Function (no API keys needed!)
./deploy.sh

# Step 2: Setup Cloud Scheduler (after deployment)
./setup_scheduler.sh
```

**Note:** No OpenAI API key needed! The generator uses deterministic algorithms.

---

## 🔄 Current Status

### Before Deployment: **Manual Only**

Right now, puzzles are **NOT** generated automatically. You need to:

```bash
cd backend
source venv/bin/activate
python main.py --test  # Generate today's puzzle
```

### After Deployment: **Fully Automatic!** ✅

Once deployed, puzzles will generate automatically every day at 9:00 AM UTC.

---

## ⏰ How Automatic Generation Will Work

Once deployed:

1. **Cloud Scheduler** triggers daily at **9:00 AM UTC**
2. **Cloud Function** runs automatically
3. **Deterministic algorithm** generates a new puzzle (no AI needed!)
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
**Cost**: FREE! (No OpenAI needed - generator is deterministic)

---

**Next Steps**: 
1. Deploy to Cloud Functions (see DEPLOYMENT_SIMPLE.md)
2. Setup Cloud Scheduler
3. Test by manually triggering scheduler
4. Verify puzzle appears in Firestore

