# BrainBurst Backend Setup Guide

Complete step-by-step guide to get the AI puzzle generator running.

## 📋 Prerequisites

- ✅ Python 3.11 or higher
- ✅ OpenAI API account
- ✅ Firebase project (already have this!)
- ✅ Google Cloud SDK (for deployment)

---

## 🚀 Quick Start (Local Testing)

### Step 1: Get OpenAI API Key

1. Go to: https://platform.openai.com/api-keys
2. Sign in or create account
3. Click **"Create new secret key"**
4. Copy the key (starts with `sk-proj-...`)
5. **Save it somewhere safe!**

💰 **Cost**: ~$0.0001 per puzzle (~$0.04/year for daily generation)

### Step 2: Get Firebase Service Account Key

1. Go to: https://console.firebase.google.com
2. Select your **brainburst** project
3. Click ⚙️ (Settings) → **Project settings**
4. Go to **Service accounts** tab
5. Click **"Generate new private key"**
6. Save as `backend/serviceAccountKey.json`
7. ⚠️ **NEVER commit this file to Git!**

### Step 3: Run Setup Script

```bash
cd backend
chmod +x setup.sh
./setup.sh
```

This will:
- Create Python virtual environment
- Install all dependencies
- Create `.env` file template

### Step 4: Configure Environment

Edit `backend/.env`:

```bash
# OpenAI API Key (from Step 1)
OPENAI_API_KEY=sk-proj-YOUR_KEY_HERE

# Firebase Service Account (from Step 2)
FIREBASE_SERVICE_ACCOUNT_PATH=./serviceAccountKey.json

# Firebase Project ID
FIREBASE_PROJECT_ID=brainburst-bb78e
```

### Step 5: Test Puzzle Generation! 🎉

```bash
# Activate virtual environment
source venv/bin/activate

# Generate a test puzzle
python main.py --test
```

You should see:
```
🚀 BrainBurst Puzzle Generator
📅 Date: 2025-12-25
🎮 Game Type: MINI_SUDOKU_6X6

🎮 Generating MINI_SUDOKU_6X6 puzzle for 2025-12-25...
✅ Payload generated
✅ Payload validated
✅ Puzzle written to Firestore: MINI_SUDOKU_6X6_2025-12-25

📊 Result:
{
  "success": true,
  "puzzleId": "MINI_SUDOKU_6X6_2025-12-25",
  "message": "Puzzle generated and stored successfully",
  "givens": 20
}

✅ Success! Puzzle ID: MINI_SUDOKU_6X6_2025-12-25
🎯 Check your Firestore console to see the puzzle!
```

### Step 6: Verify in Firestore

1. Go to Firebase Console → Firestore Database
2. Open `puzzles` collection
3. You should see: `MINI_SUDOKU_6X6_2025-12-25`
4. Click it to see the puzzle data

### Step 7: Test in Your App! 📱

1. Open your Android app
2. You should see the puzzle is now available!
3. Play and complete it
4. Check the leaderboard

---

## ☁️ Deployment to Google Cloud

### Option 1: Cloud Functions (Recommended for MVP)

#### 1. Install Google Cloud SDK

```bash
# Mac
brew install google-cloud-sdk

# Or download from:
# https://cloud.google.com/sdk/docs/install
```

#### 2. Login and Set Project

```bash
gcloud auth login
gcloud config set project brainburst-bb78e
```

#### 3. Deploy Function

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

Wait ~2 minutes for deployment.

#### 4. Test Deployed Function

```bash
# Get the function URL
gcloud functions describe generate_daily_puzzle \
  --gen2 \
  --region=us-central1 \
  --format="value(serviceConfig.uri)"

# Test it
curl -X POST https://YOUR_FUNCTION_URL \
  -H "Content-Type: application/json" \
  -d '{"gameType": "MINI_SUDOKU_6X6"}'
```

### Option 2: Cloud Scheduler (Daily Automation)

#### 1. Create Scheduler Job

```bash
gcloud scheduler jobs create http daily-puzzle-sudoku \
  --location=us-central1 \
  --schedule="0 0 * * *" \
  --uri="https://YOUR_FUNCTION_URL/generate_daily_puzzle" \
  --http-method=POST \
  --message-body='{"gameType":"MINI_SUDOKU_6X6"}' \
  --time-zone="UTC"
```

This runs every day at midnight UTC.

#### 2. Test Scheduler

```bash
gcloud scheduler jobs run daily-puzzle-sudoku --location=us-central1
```

#### 3. Verify Execution

```bash
gcloud scheduler jobs describe daily-puzzle-sudoku --location=us-central1
```

---

## 🧪 Testing Commands

### Generate Specific Date

```bash
python main.py --date 2025-12-26
```

### Test HTTP Locally

```bash
# Terminal 1: Start local server
functions-framework --target=generate_daily_puzzle --debug

# Terminal 2: Send request
curl -X POST http://localhost:8080 \
  -H "Content-Type: application/json" \
  -d '{"gameType": "MINI_SUDOKU_6X6", "date": "2025-12-25"}'
```

---

## 🐛 Troubleshooting

### "ModuleNotFoundError: No module named 'openai'"

```bash
source venv/bin/activate
pip install -r requirements.txt
```

### "OpenAI API key not found"

Check your `.env` file has:
```
OPENAI_API_KEY=sk-proj-...
```

### "Firebase permission denied"

Make sure `serviceAccountKey.json` exists and has correct permissions.

### "Validation failed"

OpenAI sometimes generates invalid puzzles. Just run again - it learns!

---

## 💰 Cost Breakdown

### OpenAI API:
- Model: gpt-4o-mini
- ~500 tokens per puzzle
- $0.00015 per 1K tokens
- **~$0.04 per year** (365 puzzles)

### Cloud Functions:
- 2M free invocations/month
- 1 call per day = **FREE**

### Cloud Scheduler:
- 3 jobs free
- **FREE**

### Total: < $1/year 🎉

---

## 📈 Monitoring

### View Logs

```bash
# Cloud Functions logs
gcloud functions logs read generate_daily_puzzle \
  --gen2 \
  --region=us-central1 \
  --limit=50

# Scheduler logs
gcloud logging read "resource.type=cloud_scheduler_job" --limit=20
```

### Check Firestore

Go to Firebase Console → Firestore → `puzzles` collection

You should see new puzzles added daily!

---

## 🎯 Next Steps

Once this is working:

1. ✅ **Remove the admin upload button** from the app
2. ✅ **Test for a few days** - make sure puzzles generate automatically
3. ✅ **Add more game types** (Zip, Tango)
4. ✅ **Add difficulty levels** (Easy, Medium, Hard)
5. ✅ **Monitor costs** (should be negligible)

---

## 🔐 Security Notes

- ⚠️ **NEVER commit** `serviceAccountKey.json`
- ⚠️ **NEVER commit** `.env` with real keys
- ✅ Use environment variables in Cloud Functions
- ✅ Restrict Cloud Function access if needed
- ✅ Rotate API keys periodically

---

**Status**: Backend code complete! ✅
**Next**: Setup API keys and test locally 🚀

