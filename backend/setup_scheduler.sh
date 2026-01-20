#!/bin/bash

# BrainBurst Backend - Cloud Scheduler Setup Script
# This script sets up automatic daily puzzle generation at 9:00 AM UTC
# Creates jobs for both Sudoku and ZIP puzzles

set -e  # Exit on error

echo "⏰ BrainBurst Cloud Scheduler Setup"
echo "===================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}❌ Google Cloud SDK not found!${NC}"
    exit 1
fi

# Set project
PROJECT_ID="brainburst-bb78e"
gcloud config set project ${PROJECT_ID}

# Function configuration
FUNCTION_NAME="generate-daily-puzzle"
REGION="us-central1"

# Get function URL
echo "🔍 Getting Cloud Function URL..."
FUNCTION_URL=$(gcloud functions describe ${FUNCTION_NAME} \
  --gen2 \
  --region=${REGION} \
  --format="value(serviceConfig.uri)" 2>/dev/null)

if [ -z "$FUNCTION_URL" ]; then
    echo -e "${RED}❌ Cloud Function not found!${NC}"
    echo "Please deploy the function first: ./deploy.sh"
    exit 1
fi

echo -e "${GREEN}✅ Found function: ${FUNCTION_URL}${NC}"
echo ""

# Enable Cloud Scheduler API
echo "🔧 Enabling Cloud Scheduler API..."
gcloud services enable cloudscheduler.googleapis.com --quiet

# Function to create or update a scheduler job
create_or_update_job() {
    local JOB_NAME=$1
    local GAME_TYPE=$2
    local DESCRIPTION=$3
    
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🎮 Setting up: ${JOB_NAME}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    # Check if scheduler job already exists
    EXISTING_JOB=$(gcloud scheduler jobs describe ${JOB_NAME} \
      --location=${REGION} \
      --format="value(name)" 2>/dev/null || echo "")
    
    if [ -n "$EXISTING_JOB" ]; then
        echo -e "${YELLOW}⚠️  Job already exists, updating...${NC}"
        
        gcloud scheduler jobs update http ${JOB_NAME} \
          --location=${REGION} \
          --schedule="0 9 * * *" \
          --uri="${FUNCTION_URL}" \
          --http-method=POST \
          --message-body="{\"gameType\":\"${GAME_TYPE}\"}" \
          --update-headers="Content-Type=application/json" \
          --time-zone="UTC" \
          --description="${DESCRIPTION}"
        
        echo -e "${GREEN}✅ Job updated successfully!${NC}"
    else
        echo "📅 Creating new scheduler job..."
        echo "   Schedule: Every day at 9:00 AM UTC"
        echo "   Game Type: ${GAME_TYPE}"
        
        gcloud scheduler jobs create http ${JOB_NAME} \
          --location=${REGION} \
          --schedule="0 9 * * *" \
          --uri="${FUNCTION_URL}" \
          --http-method=POST \
          --message-body="{\"gameType\":\"${GAME_TYPE}\"}" \
          --headers="Content-Type=application/json" \
          --time-zone="UTC" \
          --description="${DESCRIPTION}" \
          --attempt-deadline=600s
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Job created successfully!${NC}"
        else
            echo -e "${RED}❌ Failed to create job${NC}"
            return 1
        fi
    fi
}

# Create/update both scheduler jobs
create_or_update_job "daily-puzzle-sudoku" "MINI_SUDOKU_6X6" "Daily Sudoku puzzle generation at 9:00 AM UTC"
create_or_update_job "daily-puzzle-zip" "ZIP" "Daily ZIP puzzle generation at 9:00 AM UTC"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 All Scheduler Jobs:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
gcloud scheduler jobs list --location=${REGION} --format="table(name,schedule,state,httpTarget.uri)"

echo ""
echo -e "${BLUE}🧪 Test the schedulers manually:${NC}"
echo "  gcloud scheduler jobs run daily-puzzle-sudoku --location=${REGION}"
echo "  gcloud scheduler jobs run daily-puzzle-zip --location=${REGION}"
echo ""
echo -e "${BLUE}📋 View scheduler logs:${NC}"
echo "  gcloud logging read \"resource.type=cloud_scheduler_job\" --limit=20"
echo ""
echo -e "${GREEN}🎉 Setup complete!${NC}"
echo ""
echo "Both Sudoku and ZIP puzzles will now generate automatically every day at 9:00 AM UTC!"
echo "Check Firestore tomorrow to see the new puzzles."


