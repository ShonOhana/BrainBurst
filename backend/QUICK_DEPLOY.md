# ⚡ Quick Deploy Reference

**5-minute deployment guide**

## Prerequisites

- [ ] Google Cloud SDK installed (`gcloud` command)
- [ ] Logged in: `gcloud auth login`
- [ ] Project set: `gcloud config set project brainburst-bb78e`

**Note:** No OpenAI API key needed! Generator is deterministic.

## Deploy (2 commands)

```bash
cd backend

# 1. Deploy function (no API keys needed!)
./deploy.sh

# 2. Setup scheduler
./setup_scheduler.sh
```

## Test

```bash
# Test function
gcloud scheduler jobs run daily-puzzle-sudoku --location=us-central1

# Check Firestore for new puzzle
```

## Verify

```bash
# Check function
gcloud functions describe generate-daily-puzzle --gen2 --region=us-central1

# Check scheduler
gcloud scheduler jobs describe daily-puzzle-sudoku --location=us-central1
```

## Troubleshooting

**Function not found?**
→ Run `./deploy.sh` first

**Scheduler fails?**
→ Check function URL is correct
→ Check function logs: `gcloud functions logs read generate-daily-puzzle --gen2 --region=us-central1`

**Need help?**
→ See [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)

---

**Done!** Puzzles will generate automatically at 9:00 AM UTC daily! 🎉

