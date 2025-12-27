#!/bin/bash

# BrainBurst Backend - Cloud Scheduler Setup Script
# This script sets up automatic daily puzzle generation at 9:00 AM UTC

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
JOB_NAME="daily-puzzle-sudoku"

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

# Check if scheduler job already exists
EXISTING_JOB=$(gcloud scheduler jobs describe ${JOB_NAME} \
  --location=${REGION} \
  --format="value(name)" 2>/dev/null || echo "")

if [ -n "$EXISTING_JOB" ]; then
    echo -e "${YELLOW}⚠️  Scheduler job already exists!${NC}"
    echo ""
    read -p "Do you want to update it? (y/n): " update_choice
    if [ "$update_choice" != "y" ]; then
        echo "Exiting..."
        exit 0
    fi
    
    # Update existing job
    echo ""
    echo "🔄 Updating existing scheduler job..."
    gcloud scheduler jobs update http ${JOB_NAME} \
      --location=${REGION} \
      --schedule="0 9 * * *" \
      --uri="${FUNCTION_URL}" \
      --http-method=POST \
      --message-body='{"gameType":"MINI_SUDOKU_6X6"}' \
      --update-headers="Content-Type=application/json" \
      --time-zone="UTC" \
      --description="Daily puzzle generation at 9:00 AM UTC"
    
    echo ""
    echo -e "${GREEN}✅ Scheduler job updated!${NC}"
else
    # Enable Cloud Scheduler API
    echo "🔧 Enabling Cloud Scheduler API..."
    gcloud services enable cloudscheduler.googleapis.com --quiet
    
    # Create new job
    echo ""
    echo "📅 Creating daily scheduler job..."
    echo "   Schedule: Every day at 9:00 AM UTC"
    echo "   Target: ${FUNCTION_URL}"
    echo ""
    
    gcloud scheduler jobs create http ${JOB_NAME} \
      --location=${REGION} \
      --schedule="0 9 * * *" \
      --uri="${FUNCTION_URL}" \
      --http-method=POST \
      --message-body='{"gameType":"MINI_SUDOKU_6X6"}' \
      --update-headers="Content-Type=application/json" \
      --time-zone="UTC" \
      --description="Daily puzzle generation at 9:00 AM UTC" \
      --attempt-deadline=600s
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ Scheduler job created successfully!${NC}"
    else
        echo ""
        echo -e "${RED}❌ Failed to create scheduler job${NC}"
        exit 1
    fi
fi

echo ""
echo "📊 Scheduler Details:"
echo "===================="
gcloud scheduler jobs describe ${JOB_NAME} \
  --location=${REGION} \
  --format="yaml(name,schedule,timeZone,httpTarget.uri,state)"

echo ""
echo -e "${BLUE}🧪 Test the scheduler manually:${NC}"
echo "  gcloud scheduler jobs run ${JOB_NAME} --location=${REGION}"
echo ""
echo -e "${BLUE}📋 View scheduler logs:${NC}"
echo "  gcloud logging read \"resource.type=cloud_scheduler_job\" --limit=20"
echo ""
echo -e "${GREEN}🎉 Setup complete!${NC}"
echo ""
echo "Your puzzles will now generate automatically every day at 9:00 AM UTC!"
echo "Check Firestore tomorrow to see the new puzzle."


