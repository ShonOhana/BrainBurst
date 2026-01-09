# 🚀 Simple Deployment Guide

**No OpenAI API key needed!** The generator uses deterministic algorithms, not AI.

**No Firebase console setup needed!** Cloud Functions automatically authenticate.

---

## Quick Deploy (3 steps)

### 1. Install Google Cloud SDK

```bash
# Mac
brew install google-cloud-sdk

# Or download from: https://cloud.google.com/sdk/docs/install
```

### 2. Login and Set Project

```bash
gcloud auth login
gcloud config set project brainburst-bb78e
```

### 3. Deploy

```bash
cd backend
./deploy.sh          # Deploy Cloud Function
./setup_scheduler.sh # Setup daily automation
```

**That's it!** 🎉

---

## What Gets Deployed

- ✅ **Cloud Function**: Generates puzzles using deterministic algorithms
- ✅ **Cloud Scheduler**: Triggers daily at 9:00 AM UTC
- ✅ **Auto-authentication**: Uses default service account (no setup needed!)

---

## Firebase Console

**You don't need to do anything in Firebase Console!**

Cloud Functions automatically:
- ✅ Authenticate with Firestore
- ✅ Use the default compute service account
- ✅ Have permissions to write to `puzzles` collection

The only thing you might want to check:
- Go to Firestore after deployment to see the puzzles being created!

---

## Testing

After deployment, test it:

```bash
# Manually trigger the scheduler
gcloud scheduler jobs run daily-puzzle-sudoku --location=us-central1

# Check Firestore - you should see a new puzzle!
```

---

## Cost

- **Google Cloud**: FREE (within free tier)
- **OpenAI**: $0 (not used!)
- **Total**: **FREE** 🎉

---

## Troubleshooting

**"Permission denied" errors?**
→ Make sure you're logged in: `gcloud auth login`

**"API not enabled" errors?**
→ The deploy script enables them automatically, but if needed:
```bash
gcloud services enable cloudfunctions.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable cloudscheduler.googleapis.com
```

**Function not found?**
→ Make sure you ran `./deploy.sh` first

---

**That's it!** No API keys, no Firebase setup, just deploy and go! 🚀





