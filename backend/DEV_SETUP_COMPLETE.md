# 🎉 Dev Environment Setup Complete!

Your BrainBurst backend now has **full dev/prod separation**.

---

## What Was Created

### Configuration Files
- ✅ `.env.dev` - Development environment template
- ✅ `.env.prod` - Production environment template
- ✅ `DEV_ENVIRONMENT.md` - Complete setup guide
- ✅ `dev.sh` - Quick command helper script

### Modified Scripts
- ✅ `deploy.sh` - Now supports `--dev` and `--prod` flags
- ✅ `setup_scheduler.sh` - Now supports `--dev` and `--prod` flags
- ✅ `.gcloudignore` - Updated to exclude dev files from deployment

---

## Quick Start

### 1. Create Firebase Dev Project

Go to [Firebase Console](https://console.firebase.google.com/) and create:
- Project name: `brainburst-dev`
- Download service account key as `serviceAccountKey-dev.json`

### 2. Deploy to Dev

```bash
cd backend

# Deploy everything to dev
./dev.sh deploy-dev

# Test it
./dev.sh trigger-dev
```

### 3. Test New Game Locally

```bash
# Test locally first
./dev.sh test-sudoku
./dev.sh test-zip

# Then deploy to dev
./dev.sh deploy-dev
```

---

## Common Commands

```bash
# Quick helper commands
./dev.sh deploy-dev       # Deploy to dev
./dev.sh deploy-prod      # Deploy to prod (with confirmation)
./dev.sh test-sudoku      # Test Sudoku locally
./dev.sh trigger-dev      # Manually run dev scheduler
./dev.sh logs-dev         # View dev logs
./dev.sh status           # Check both environments

# Full deployment (traditional way)
./deploy.sh --dev         # Deploy Cloud Function to dev
./setup_scheduler.sh --dev # Setup scheduler for dev

./deploy.sh --prod        # Deploy to production (asks confirmation)
./setup_scheduler.sh --prod # Setup prod scheduler
```

---

## Workflow: Adding a New Game

```bash
# 1. Create generator in backend/generators/
#    Example: tango_generator.py

# 2. Add to main.py GENERATORS dict

# 3. Test locally
./dev.sh test-local

# 4. Deploy to dev
./dev.sh deploy-dev

# 5. Test in dev Firebase
./dev.sh trigger-dev

# 6. Once working, deploy to prod
./dev.sh deploy-prod
```

---

## Key Features

✅ **Full Isolation**: Dev and prod use separate Firebase projects  
✅ **Safe Testing**: Test games and changes without affecting users  
✅ **Easy Switching**: Simple `--dev` and `--prod` flags  
✅ **Protection**: Production requires confirmation prompt  
✅ **Quick Commands**: Helper script for common tasks  
✅ **Local Testing**: Test generators before deploying  

---

## Environment Details

| Feature | Development | Production |
|---------|-------------|------------|
| **Firebase Project** | `brainburst-dev` | `brainburst-bb78e` |
| **Cloud Function** | `generate-daily-puzzle-dev` | `generate-daily-puzzle` |
| **Scheduler Jobs** | `daily-puzzle-*-dev` | `daily-puzzle-*` |
| **Firestore** | Separate database | Separate database |
| **Deploy Command** | `./deploy.sh --dev` | `./deploy.sh --prod` |

---

## Next Steps

1. **Create dev Firebase project** (see step 1 above)
2. **Download service account key** and save as `serviceAccountKey-dev.json`
3. **Deploy to dev**: `./dev.sh deploy-dev`
4. **Start developing!** Add new games, test changes, iterate

---

## Documentation

- **Full Setup Guide**: `DEV_ENVIRONMENT.md`
- **Helper Commands**: Run `./dev.sh` without arguments to see all commands

---

**You're ready to develop safely!** 🚀

Changes in dev won't affect production users. Test freely!
