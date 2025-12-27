# 🚀 BrainBurst Backend Deployment Guide

Complete guide to deploy automatic daily puzzle generation to Google Cloud.

## 📋 Prerequisites Checklist

Before deploying, make sure you have:

- [ ] **Google Cloud Account** with billing enabled (free tier is enough!)
- [ ] **Google Cloud SDK** installed (`gcloud` command)

**Note:** 
- ❌ **No OpenAI API key needed** - generator uses deterministic algorithms!
- ❌ **No Firebase console setup needed** - Cloud Functions auto-authenticate!
- ✅ **Python 3.11+** only needed for local testing (optional)

---

## 🎯 Quick Deployment (5 minutes)

### Step 1: Install Google Cloud SDK

**Mac:**
```bash
brew install google-cloud-sdk
```

**Or download from:**
https://cloud.google.com/sdk/docs/install

### Step 2: Login to Google Cloud

```bash
gcloud auth login
gcloud config set project brainburst-bb78e
```

### Step 3: (Skip - Not Needed!)

**No API keys needed!** The generator uses deterministic backtracking algorithms, not AI.

### Step 4: Deploy Cloud Function

```bash
cd backend
chmod +x deploy.sh
./deploy.sh
```

This will:
- ✅ Enable required Google Cloud APIs
- ✅ Deploy the function to Cloud Functions (Gen 2)
- ✅ Set environment variables
- ✅ Give you the function URL

**Wait ~2-3 minutes** for deployment to complete.

### Step 5: Setup Cloud Scheduler

```bash
chmod +x setup_scheduler.sh
./setup_scheduler.sh
```

This will:
- ✅ Enable Cloud Scheduler API
- ✅ Create a daily job at **9:00 AM UTC**
- ✅ Configure it to call your function

**Done!** 🎉 Your puzzles will now generate automatically every day!

---

## 🧪 Testing

### Test the Deployed Function

```bash
# Get function URL
FUNCTION_URL=$(gcloud functions describe generate-daily-puzzle \
  --gen2 \
  --region=us-central1 \
  --format="value(serviceConfig.uri)")

# Test it
curl -X POST ${FUNCTION_URL} \
  -H "Content-Type: application/json" \
  -d '{"gameType": "MINI_SUDOKU_6X6"}'
```

### Test the Scheduler Manually

```bash
gcloud scheduler jobs run daily-puzzle-sudoku --location=us-central1
```

### Verify in Firestore

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to **Firestore Database**
4. Check `puzzles` collection
5. You should see a new puzzle document!

---

## ⏰ Schedule Configuration

### Current Schedule: 9:00 AM UTC

The scheduler is configured to run at **9:00 AM UTC** every day.

### Change the Time

To change when puzzles generate:

```bash
# Example: 9:00 AM Eastern Time
gcloud scheduler jobs update http daily-puzzle-sudoku \
  --location=us-central1 \
  --schedule="0 9 * * *" \
  --time-zone="America/New_York"
```

**Common timezones:**
- `UTC` (default)
- `America/New_York` (Eastern)
- `America/Los_Angeles` (Pacific)
- `Europe/London` (UK)

**Cron format:** `0 9 * * *` means "9:00 AM every day"
- First `0` = minute (0-59)
- `9` = hour (0-23)
- `*` = every day of month
- `*` = every month
- `*` = every day of week

---

## 📊 Monitoring

### View Function Logs

```bash
gcloud functions logs read generate-daily-puzzle \
  --gen2 \
  --region=us-central1 \
  --limit=50
```

### View Scheduler Logs

```bash
gcloud logging read "resource.type=cloud_scheduler_job" --limit=20
```

### Check Function Status

```bash
gcloud functions describe generate-daily-puzzle \
  --gen2 \
  --region=us-central1
```

### Check Scheduler Status

```bash
gcloud scheduler jobs describe daily-puzzle-sudoku \
  --location=us-central1
```

---

## 🔧 Troubleshooting

### "Permission denied" errors

Make sure you're logged in:
```bash
gcloud auth login
gcloud auth application-default login
```

### "API not enabled" errors

The deployment script enables APIs automatically, but if you see errors:
```bash
gcloud services enable cloudfunctions.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable cloudscheduler.googleapis.com
```

### Function deployment fails

1. Check you have billing enabled on Google Cloud
2. Make sure you have the correct permissions
3. Try deploying with more verbose output:
```bash
gcloud functions deploy generate-daily-puzzle \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=generate_daily_puzzle \
  --trigger-http \
  --allow-unauthenticated \
  --set-env-vars OPENAI_API_KEY="${OPENAI_API_KEY}" \
  --verbosity=debug
```

### Scheduler not running

1. Check if the job exists:
```bash
gcloud scheduler jobs list --location=us-central1
```

2. Check the job state:
```bash
gcloud scheduler jobs describe daily-puzzle-sudoku --location=us-central1
```

3. Check for 400 errors (Content-Type issue):
If scheduler shows `status.code: 3` and logs show HTTP 400 errors, the Content-Type header might be wrong:
```bash
# Fix the Content-Type header
FUNCTION_URL=$(gcloud functions describe generate-daily-puzzle --gen2 --region=us-central1 --format="value(serviceConfig.uri)")
gcloud scheduler jobs update http daily-puzzle-sudoku \
  --location=us-central1 \
  --update-headers="Content-Type=application/json"
```

4. Manually trigger it to test:
```bash
gcloud scheduler jobs run daily-puzzle-sudoku --location=us-central1
```

### Generator errors

- Generator uses deterministic algorithms (no API needed)
- If puzzles fail validation, it will retry automatically
- Check function logs for details

---

## 💰 Costs

### Google Cloud (FREE tier)

- **Cloud Functions**: 2M invocations/month FREE
- **Cloud Scheduler**: 3 jobs FREE
- **Cloud Build**: 120 build-minutes/day FREE
- **Total**: **FREE** for this use case! 🎉

### Generator

- **Method**: Deterministic backtracking algorithms
- **Cost**: $0 (no external APIs!)
- **Speed**: Instant generation
- **Reliability**: 100% (no API failures)

**Total cost: FREE** 🎉

---

## 🔐 Security

### Environment Variables

Only `FIREBASE_PROJECT_ID` is needed. Cloud Functions automatically:
- ✅ Authenticate with Firestore using default service account
- ✅ Have permissions to write to `puzzles` collection
- ✅ No API keys or secrets needed!

### Function Access

The function is currently set to `--allow-unauthenticated` for simplicity. For production, you might want to:
- Require authentication
- Use Cloud Scheduler with service account
- Add IP restrictions

### Service Account

Cloud Functions automatically uses the default compute service account. For more control:
```bash
gcloud functions deploy generate-daily-puzzle \
  --service-account=your-service-account@project.iam.gserviceaccount.com
```

---

## 📝 Manual Deployment (Alternative)

If the scripts don't work, you can deploy manually:

### 1. Deploy Function

```bash
cd backend

gcloud functions deploy generate-daily-puzzle \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=generate_daily_puzzle \
  --trigger-http \
  --allow-unauthenticated \
  --set-env-vars FIREBASE_PROJECT_ID="brainburst-bb78e" \
  --memory=256MB \
  --timeout=540s
```

### 2. Create Scheduler

```bash
# Get function URL first
FUNCTION_URL=$(gcloud functions describe generate-daily-puzzle \
  --gen2 \
  --region=us-central1 \
  --format="value(serviceConfig.uri)")

# Create scheduler
gcloud scheduler jobs create http daily-puzzle-sudoku \
  --location=us-central1 \
  --schedule="0 9 * * *" \
  --uri="${FUNCTION_URL}" \
  --http-method=POST \
  --message-body='{"gameType":"MINI_SUDOKU_6X6"}' \
  --time-zone="UTC" \
  --description="Daily puzzle generation at 9:00 AM UTC"
```

---

## ✅ Verification Checklist

After deployment, verify:

- [ ] Function deployed successfully
- [ ] Function URL is accessible
- [ ] Manual function test works
- [ ] Scheduler job created
- [ ] Scheduler can be triggered manually
- [ ] Puzzle appears in Firestore after test
- [ ] App can load the puzzle

---

## 🎯 Next Steps

Once deployment is working:

1. **Test for a few days** - Make sure puzzles generate automatically
2. **Monitor costs** - Should be negligible
3. **Remove admin button** - No longer needed!
4. **Add error alerts** - Set up Cloud Monitoring alerts
5. **Add more game types** - Extend to Zip, Tango, etc.

---

## 📞 Support

If you run into issues:

1. Check the logs (see Monitoring section)
2. Verify all prerequisites are met
3. Test locally first: `python main.py --test`
4. Check Google Cloud Console for errors

---

**Status**: Ready to deploy! 🚀

Run `./deploy.sh` to get started!

