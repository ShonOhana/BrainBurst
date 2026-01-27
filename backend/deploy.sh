#!/bin/bash

# BrainBurst Backend - Cloud Function Deployment Script
# This script deploys the puzzle generator to Google Cloud Functions
# Usage: ./deploy.sh [--dev|--prod]

set -e  # Exit on error

echo "🚀 BrainBurst Backend Deployment"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse environment argument
ENVIRONMENT="prod"
if [ "$1" == "--dev" ]; then
    ENVIRONMENT="dev"
elif [ "$1" == "--prod" ]; then
    ENVIRONMENT="prod"
elif [ -n "$1" ]; then
    echo -e "${RED}❌ Invalid argument: $1${NC}"
    echo "Usage: ./deploy.sh [--dev|--prod]"
    echo "  --dev   Deploy to development environment"
    echo "  --prod  Deploy to production environment (default)"
    exit 1
fi

# Set project ID based on environment
if [ "$ENVIRONMENT" == "dev" ]; then
    PROJECT_ID="brainburst-dev"
    echo -e "${BLUE}🔧 DEVELOPMENT MODE${NC}"
else
    PROJECT_ID="brainburst-bb78e"
    echo -e "${GREEN}🚀 PRODUCTION MODE${NC}"
fi

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

# Confirm production deployment
if [ "$ENVIRONMENT" == "prod" ]; then
    echo -e "${YELLOW}⚠️  You are about to deploy to PRODUCTION!${NC}"
    echo -n "Continue? (y/N): "
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "Deployment cancelled."
        exit 0
    fi
fi

echo -e "${GREEN}📦 Setting project to: ${PROJECT_ID}${NC}"
gcloud config set project ${PROJECT_ID}

# Function configuration (add environment suffix for dev)
if [ "$ENVIRONMENT" == "dev" ]; then
    FUNCTION_NAME="generate-daily-puzzle-dev"
else
    FUNCTION_NAME="generate-daily-puzzle"
fi

REGION="us-central1"
RUNTIME="python311"
ENTRY_POINT="generate_daily_puzzle"

echo ""
echo -e "${GREEN}📤 Deploying Cloud Function...${NC}"
echo "  Environment: ${ENVIRONMENT}"
echo "  Project: ${PROJECT_ID}"
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
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}📋 Deployment Summary${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    # Get function URL
    FUNCTION_URL=$(gcloud functions describe ${FUNCTION_NAME} \
      --gen2 \
      --region=${REGION} \
      --format="value(serviceConfig.uri)")
    
    echo -e "  Environment: ${ENVIRONMENT}"
    echo -e "  Project: ${PROJECT_ID}"
    echo -e "  Function: ${FUNCTION_NAME}"
    echo -e "  URL: ${FUNCTION_URL}"
    echo ""
    echo "🧪 Test the function:"
    echo "  curl -X POST ${FUNCTION_URL} \\"
    echo "    -H 'Content-Type: application/json' \\"
    echo "    -d '{\"gameType\": \"MINI_SUDOKU_6X6\"}'"
    echo ""
    
    if [ "$ENVIRONMENT" == "dev" ]; then
        echo "📝 Next steps for dev environment:"
        echo "  1. Run: ./setup_scheduler.sh --dev"
        echo "  2. Create Firebase project 'brainburst-dev' if not exists"
        echo "  3. Test your changes safely!"
    else
        echo "📋 Next step: Run ./setup_scheduler.sh to configure daily automation"
    fi
    
    echo ""
    echo "ℹ️  Note: No OpenAI API key needed - generator uses deterministic algorithms!"
else
    echo ""
    echo -e "${RED}❌ Deployment failed!${NC}"
    exit 1
fi

