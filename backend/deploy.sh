#!/bin/bash

# BrainBurst Backend - Cloud Function Deployment Script
# This script deploys the puzzle generator to Google Cloud Functions

set -e  # Exit on error

echo "🚀 BrainBurst Backend Deployment"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}❌ Google Cloud SDK not found!${NC}"
    echo "Install it from: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if logged in
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
    echo -e "${YELLOW}⚠️  Not logged in to Google Cloud${NC}"
    echo "Running: gcloud auth login"
    gcloud auth login
fi

# Set project
PROJECT_ID="brainburst-bb78e"
echo -e "${GREEN}📦 Setting project to: ${PROJECT_ID}${NC}"
gcloud config set project ${PROJECT_ID}

# Note: OpenAI API key NOT needed - generator is deterministic!
# The generator uses backtracking algorithms, not AI

# Function configuration
FUNCTION_NAME="generate-daily-puzzle"
REGION="us-central1"
RUNTIME="python311"
ENTRY_POINT="generate_daily_puzzle"

echo ""
echo -e "${GREEN}📤 Deploying Cloud Function...${NC}"
echo "  Function: ${FUNCTION_NAME}"
echo "  Region: ${REGION}"
echo "  Runtime: ${RUNTIME}"
echo ""

# Enable required APIs
echo "🔧 Enabling required Google Cloud APIs..."
gcloud services enable cloudfunctions.googleapis.com --quiet
gcloud services enable cloudbuild.googleapis.com --quiet
gcloud services enable run.googleapis.com --quiet
gcloud services enable artifactregistry.googleapis.com --quiet

# Deploy the function
echo ""
echo "⏳ Deploying (this may take 2-3 minutes)..."
gcloud functions deploy ${FUNCTION_NAME} \
  --gen2 \
  --runtime=${RUNTIME} \
  --region=${REGION} \
  --source=. \
  --entry-point=${ENTRY_POINT} \
  --trigger-http \
  --allow-unauthenticated \
  --set-env-vars FIREBASE_PROJECT_ID="${PROJECT_ID}" \
  --memory=256MB \
  --timeout=540s \
  --max-instances=1

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Deployment successful!${NC}"
    echo ""
    
    # Get function URL
    FUNCTION_URL=$(gcloud functions describe ${FUNCTION_NAME} \
      --gen2 \
      --region=${REGION} \
      --format="value(serviceConfig.uri)")
    
    echo "🌐 Function URL: ${FUNCTION_URL}"
    echo ""
    echo "📝 Save this URL - you'll need it for Cloud Scheduler!"
    echo ""
    echo "🧪 Test the function:"
    echo "  curl -X POST ${FUNCTION_URL} \\"
    echo "    -H 'Content-Type: application/json' \\"
    echo "    -d '{\"gameType\": \"MINI_SUDOKU_6X6\"}'"
    echo ""
    echo "📋 Next step: Run ./setup_scheduler.sh to configure daily automation"
    echo ""
    echo "ℹ️  Note: No OpenAI API key needed - generator uses deterministic algorithms!"
else
    echo ""
    echo -e "${RED}❌ Deployment failed!${NC}"
    exit 1
fi

