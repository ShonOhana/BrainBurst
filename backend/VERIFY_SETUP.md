# ✅ Verify Google Cloud Setup & Scheduler Configuration

This guide helps you verify that Google Cloud is properly linked to your project and that the scheduler will generate puzzles at the scheduled time.

## 🔍 Quick Verification Checklist

Run these commands to check your setup:

### 1. Check Google Cloud Authentication & Project

```bash
# Check if you're logged in
gcloud auth list

# Check current project
gcloud config get-value project

# Should show: brainburst-bb78e
# If not, set it:
gcloud config set project brainburst-bb78e
```

### 2. Check Cloud Function Status

```bash
# Check if the function exists and is deployed
gcloud functions describe generate-daily-puzzle \
  --gen2 \
  --region=us-central1

# Get the function URL (should return a URL)
gcloud functions describe generate-daily-puzzle \
  --gen2 \
  --region=us-central1 \
  --format="value(serviceConfig.uri)"
```

### 3. Check Cloud Scheduler Job

```bash
# List all scheduler jobs
gcloud scheduler jobs list --location=us-central1

# Get detailed info about the scheduler job
gcloud scheduler jobs describe daily-puzzle-sudoku \
  --location=us-central1

# Check the schedule and state
gcloud scheduler jobs describe daily-puzzle-sudoku \
  --location=us-central1 \
  --format="yaml(name,schedule,timeZone,httpTarget.uri,state)"
```

**What to look for:**
- ✅ `state: ENABLED` - Job is active
- ✅ `schedule: "0 9 * * *"` - Runs at 9:00 AM UTC daily
- ✅ `timeZone: UTC` - Timezone is set correctly
- ✅ `httpTarget.uri` - Points to your Cloud Function URL

### 4. Test the Function Manually

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

### 5. Test the Scheduler Manually

```bash
# Manually trigger the scheduler (this will generate a puzzle now)
gcloud scheduler jobs run daily-puzzle-sudoku --location=us-central1
```

After running this, check Firestore to see if a puzzle was created.

### 6. Check Recent Logs

```bash
# View Cloud Function logs
gcloud functions logs read generate-daily-puzzle \
  --gen2 \
  --region=us-central1 \
  --limit=20

# View Cloud Scheduler logs
gcloud logging read "resource.type=cloud_scheduler_job" --limit=20

# View all recent logs for puzzle generation
gcloud logging read "resource.type=cloud_function AND resource.labels.function_name=generate-daily-puzzle" --limit=20
```

### 7. Verify in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select project: **brainburst-bb78e**
3. Go to **Firestore Database**
4. Check the `puzzles` collection
5. You should see puzzle documents with format: `MINI_SUDOKU_6X6_YYYY-MM-DD`

## 🚨 Common Issues & Fixes

### Issue: "Project not found" or "Permission denied"

**Fix:**
```bash
# Re-authenticate
gcloud auth login
gcloud auth application-default login

# Set project
gcloud config set project brainburst-bb78e
```

### Issue: Scheduler job doesn't exist

**Fix:**
```bash
cd backend
chmod +x setup_scheduler.sh
./setup_scheduler.sh
```

### Issue: Cloud Function doesn't exist

**Fix:**
```bash
cd backend
chmod +x deploy.sh
./deploy.sh
```

### Issue: Scheduler state is "PAUSED"

**Fix:**
```bash
# Resume the scheduler job
gcloud scheduler jobs resume daily-puzzle-sudoku --location=us-central1
```

### Issue: Scheduler state is "DISABLED"

**Fix:**
```bash
# Enable the scheduler job
gcloud scheduler jobs enable daily-puzzle-sudoku --location=us-central1
```

## 📅 Check Next Run Time

To see when the scheduler will run next:

```bash
# Get next run time (if available)
gcloud scheduler jobs describe daily-puzzle-sudoku \
  --location=us-central1 \
  --format="value(scheduleTime)"
```

Or check in the [Google Cloud Console](https://console.cloud.google.com/cloudscheduler):
1. Go to **Cloud Scheduler**
2. Find job: `daily-puzzle-sudoku`
3. Check the "Next run" column

## 🎯 Expected Schedule

- **Schedule:** `0 9 * * *` (9:00 AM UTC every day)
- **Timezone:** UTC
- **Next run:** Tomorrow at 9:00 AM UTC

To convert to your timezone:
- **UTC 9:00 AM** = **5:00 AM EST** / **2:00 AM PST**

## ✅ Success Indicators

You'll know everything is working if:

1. ✅ `gcloud config get-value project` shows `brainburst-bb78e`
2. ✅ `gcloud functions describe` returns function details
3. ✅ `gcloud scheduler jobs describe` shows `state: ENABLED`
4. ✅ Manual test (`gcloud scheduler jobs run`) creates a puzzle in Firestore
5. ✅ Function logs show successful puzzle generation
6. ✅ Scheduler logs show successful job executions

## 🔄 Re-run Setup (if needed)

If something is missing, you can re-run the setup:

```bash
cd backend

# Deploy function (if missing)
./deploy.sh

# Setup scheduler (if missing)
./setup_scheduler.sh
```

---

**Quick Test Command:**
```bash
# One-liner to check everything
echo "Project: $(gcloud config get-value project)" && \
echo "Function: $(gcloud functions describe generate-daily-puzzle --gen2 --region=us-central1 --format='value(name)' 2>/dev/null || echo 'NOT FOUND')" && \
echo "Scheduler: $(gcloud scheduler jobs describe daily-puzzle-sudoku --location=us-central1 --format='value(state)' 2>/dev/null || echo 'NOT FOUND')"
```


