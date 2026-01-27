# 🔧 Development Environment Setup

This guide helps you set up a **separate development environment** for BrainBurst backend, so you can safely test changes without affecting production users.

---

## Overview

Your backend now supports two isolated environments:

- **Production** (`brainburst-bb78e`): Live users
- **Development** (`brainburst-dev`): Testing & new features

Each environment has:
- Separate Firebase project
- Separate Cloud Function (`generate-daily-puzzle` vs `generate-daily-puzzle-dev`)
- Separate Cloud Scheduler jobs
- Separate Firestore data

---

## Quick Setup (4 Steps)

### 1. Create Development Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add Project"
3. Enter project name: `brainburst-dev`
4. Project ID will be: `brainburst-dev` (or similar)
5. Disable Google Analytics (optional for dev)
6. Click "Create Project"

### 2. Download Service Account Key (for local testing)

1. In Firebase Console, go to: **Project Settings** > **Service Accounts**
2. Click "Generate New Private Key"
3. Save as `backend/serviceAccountKey-dev.json`
4. **Important**: This file is gitignored, never commit it!

### 3. Configure Local Environment

```bash
cd backend

# Copy dev environment template
cp .env.dev .env

# Edit .env and verify the project ID matches your Firebase project
# FIREBASE_PROJECT_ID=brainburst-dev
```

### 4. Deploy to Development

```bash
# Deploy Cloud Function to dev environment
./deploy.sh --dev

# Setup scheduler jobs for dev
./setup_scheduler.sh --dev
```

---

## Usage

### Deploying Changes

**Development (safe):**
```bash
./deploy.sh --dev
```

**Production (requires confirmation):**
```bash
./deploy.sh --prod
# You'll be prompted to confirm
```

### Testing Locally

Run the generator locally against dev environment:

```bash
cd backend

# Make sure .env points to dev
python main.py --test --game-type MINI_SUDOKU_6X6

# Test ZIP puzzle
python main.py --test --game-type ZIP

# Generate for specific date
python main.py --date 2026-01-28 --game-type MINI_SUDOKU_6X6
```

### Testing Cloud Function

After deploying to dev:

```bash
# Get your dev function URL from deployment output, then:
curl -X POST https://YOUR-DEV-FUNCTION-URL \
  -H 'Content-Type: application/json' \
  -d '{"gameType": "MINI_SUDOKU_6X6"}'
```

### Manual Scheduler Trigger

Test scheduler jobs without waiting for the schedule:

```bash
# Trigger dev scheduler manually
gcloud scheduler jobs run daily-puzzle-sudoku-dev --location=us-central1

# Trigger prod scheduler manually
gcloud scheduler jobs run daily-puzzle-sudoku --location=us-central1
```

---

## Workflow: Adding a New Game

Here's the safe workflow to add a new game type:

### 1. Create Generator (Local)

```bash
cd backend/generators

# Create new generator file
# Example: tango_generator.py
```

Follow the pattern from `sudoku_generator.py` or `zip_generator.py`.

### 2. Test Locally

```python
# Add to main.py GENERATORS dict
GENERATORS = {
    "MINI_SUDOKU_6X6": SudokuGenerator(),
    "ZIP": ZipGenerator(),
    "TANGO": TangoGenerator(),  # NEW
}
```

Test locally against dev:
```bash
python main.py --test --game-type TANGO
```

### 3. Deploy to Dev

```bash
./deploy.sh --dev
./setup_scheduler.sh --dev
```

### 4. Test in Dev Firebase

Check Firestore console for the new puzzle type in `brainburst-dev` project.

### 5. Update Mobile App (Dev)

Point your mobile app to dev backend temporarily:
- Update Firebase config to use `brainburst-dev`
- Test the new game type
- Make sure everything works

### 6. Deploy to Production

Once everything is tested:

```bash
./deploy.sh --prod
./setup_scheduler.sh --prod
```

### 7. Update Mobile App (Prod)

- Revert mobile app to production Firebase config
- Release new app version with the new game

---

## Firestore Data Isolation

**Development Firestore:**
- Project: `brainburst-dev`
- Collections: `puzzles`, `users`, `leaderboards`
- URL: `https://console.firebase.google.com/project/brainburst-dev/firestore`

**Production Firestore:**
- Project: `brainburst-bb78e`
- Collections: `puzzles`, `users`, `leaderboards`
- URL: `https://console.firebase.google.com/project/brainburst-bb78e/firestore`

They are completely separate - changes in dev won't affect production users.

---

## Cost

Both dev and production environments stay within **Google Cloud Free Tier**:

- Cloud Functions: 2M invocations/month (you use ~60/month per env)
- Cloud Scheduler: 3 jobs free (you have 2 per env)
- Firestore: 1GB storage, 50k reads/day
- **Total cost: $0** for dev and prod combined

---

## Switching Between Environments

The deploy scripts automatically handle switching:

```bash
# Deploys to brainburst-dev, function name: generate-daily-puzzle-dev
./deploy.sh --dev

# Deploys to brainburst-bb78e, function name: generate-daily-puzzle
./deploy.sh --prod
```

You don't need to manually change gcloud config.

---

## Troubleshooting

### "Permission denied" on dev project

Make sure you have access to the Firebase project:
```bash
gcloud projects list
# Should show both brainburst-bb78e and brainburst-dev
```

If not, add your account in Firebase Console > Project Settings > Users and permissions.

### Local testing fails "Firebase app not initialized"

Check that `serviceAccountKey-dev.json` exists and `.env` has correct path:
```bash
ls -la backend/serviceAccountKey-dev.json
cat backend/.env | grep FIREBASE_SERVICE_ACCOUNT_PATH
```

### Scheduler jobs not running

Check scheduler status:
```bash
gcloud scheduler jobs list --location=us-central1
```

View logs:
```bash
gcloud logging read "resource.type=cloud_scheduler_job" --limit=20
```

---

## Security Notes

**Never commit:**
- `serviceAccountKey.json` (production)
- `serviceAccountKey-dev.json` (development)
- `.env` (local config)

These are already in `.gitignore` and `.gcloudignore`.

**Safe to commit:**
- `.env.dev` (template)
- `.env.prod` (template)
- `deploy.sh`
- `setup_scheduler.sh`

---

## Summary

**Development workflow:**
1. Write code
2. Test locally with `python main.py --test`
3. Deploy to dev with `./deploy.sh --dev`
4. Test in dev Firebase
5. Deploy to prod with `./deploy.sh --prod`

**Production is always protected:**
- Requires explicit `--prod` flag
- Shows confirmation prompt
- Separate Cloud Function and data

---

## Quick Reference

| Action | Development | Production |
|--------|-------------|------------|
| **Deploy Function** | `./deploy.sh --dev` | `./deploy.sh --prod` |
| **Setup Scheduler** | `./setup_scheduler.sh --dev` | `./setup_scheduler.sh --prod` |
| **Test Locally** | `python main.py --test` | Point `.env` to prod (not recommended) |
| **Firebase Console** | [brainburst-dev](https://console.firebase.google.com/project/brainburst-dev) | [brainburst-bb78e](https://console.firebase.google.com/project/brainburst-bb78e) |
| **Function Name** | `generate-daily-puzzle-dev` | `generate-daily-puzzle` |
| **Scheduler Jobs** | `daily-puzzle-*-dev` | `daily-puzzle-*` |

---

Now you can safely experiment with new games and features! 🎮
