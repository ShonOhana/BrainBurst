# BrainBurst Backend - AI Puzzle Generator

This Python backend uses **OpenAI GPT-4** to generate daily Sudoku puzzles and stores them in Firestore.

## Architecture

```
Daily Trigger (Cloud Scheduler)
        ↓
   Cloud Function
        ↓
   OpenAI GPT-4 (Generate Puzzle)
        ↓
   Validate Puzzle
        ↓
   Firestore (Store Puzzle)
```

## Local Development

### 1. Install Python 3.11+
```bash
python3 --version
```

### 2. Create Virtual Environment
```bash
cd backend
python3 -m venv venv
source venv/bin/activate  # On Mac/Linux
```

### 3. Install Dependencies
```bash
pip install -r requirements.txt
```

### 4. Setup Environment Variables
```bash
cp .env.example .env
# Edit .env with your keys:
# - OPENAI_API_KEY (from OpenAI console)
# - Firebase credentials
```

### 5. Get Firebase Service Account Key
1. Go to Firebase Console → Project Settings → Service Accounts
2. Click "Generate New Private Key"
3. Save as `backend/serviceAccountKey.json`
4. ⚠️ Never commit this file!

### 6. Run Locally
```bash
# Test puzzle generation
python main.py --test

# Run as HTTP function (for testing)
functions-framework --target=generate_daily_puzzle --debug
```

## Deployment

### Option 1: Cloud Functions (Simpler)
```bash
gcloud functions deploy generate_daily_puzzle \
  --runtime python311 \
  --trigger-http \
  --entry-point generate_daily_puzzle \
  --set-env-vars OPENAI_API_KEY=your-key,FIREBASE_PROJECT_ID=brainburst-bb78e \
  --allow-unauthenticated
```

### Option 2: Cloud Run (More flexible)
```bash
# Build and deploy
gcloud run deploy brainburst-backend \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

## Cloud Scheduler Setup

Create a daily trigger at midnight UTC:

```bash
gcloud scheduler jobs create http daily-puzzle-generation \
  --schedule="0 0 * * *" \
  --uri="https://REGION-PROJECT_ID.cloudfunctions.net/generate_daily_puzzle" \
  --http-method=POST \
  --time-zone="UTC"
```

## File Structure

```
backend/
├── main.py                    # Cloud Function entry point
├── generators/
│   ├── __init__.py
│   ├── base.py               # GameGenerator protocol
│   └── sudoku_generator.py   # Sudoku-specific generator
├── validators/
│   ├── __init__.py
│   └── sudoku_validator.py   # 6×6 Sudoku validation
├── firestore_writer.py       # Write puzzles to Firestore
├── requirements.txt          # Python dependencies
├── .env.example              # Environment template
└── README.md                 # This file
```

## Testing

### Manual Test
```bash
python main.py --test
```

### Generate Today's Puzzle
```bash
curl -X POST http://localhost:8080 \
  -H "Content-Type: application/json" \
  -d '{"gameType": "MINI_SUDOKU_6X6", "date": "2025-12-25"}'
```

## Costs

**OpenAI API:**
- Model: gpt-4o-mini (cheapest)
- ~500 tokens per puzzle
- ~$0.0001 per puzzle
- **~$0.04 per year** for daily puzzles

**Cloud Functions:**
- Free tier: 2M invocations/month
- 1 invocation per day = FREE

**Total: < $1/year** 💰

## Features

- ✅ AI-generated valid Sudoku puzzles
- ✅ Automatic validation
- ✅ Daily scheduling
- ✅ Firestore integration
- ✅ Multiple difficulty levels (easy to add)
- ✅ Extensible to other game types

## Future Enhancements

- [ ] Multiple difficulty levels
- [ ] Zip game generator
- [ ] Tango game generator
- [ ] Puzzle quality scoring
- [ ] A/B testing different prompts
- [ ] Fallback to pre-generated puzzles if OpenAI fails

