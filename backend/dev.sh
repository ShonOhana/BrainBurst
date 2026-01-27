#!/bin/bash

# BrainBurst Dev Environment - Quick Commands
# Common tasks for development workflow

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

show_help() {
    echo "🔧 BrainBurst Development Helper"
    echo "================================"
    echo ""
    echo "Usage: ./dev.sh <command>"
    echo ""
    echo "Commands:"
    echo "  deploy-dev      Deploy to development environment"
    echo "  deploy-prod     Deploy to production environment"
    echo "  test-local      Test puzzle generation locally (dev config)"
    echo "  test-sudoku     Generate test Sudoku puzzle locally"
    echo "  test-zip        Generate test ZIP puzzle locally"
    echo "  trigger-dev     Manually trigger dev scheduler"
    echo "  trigger-prod    Manually trigger prod scheduler"
    echo "  logs-dev        View dev Cloud Function logs"
    echo "  logs-prod       View prod Cloud Function logs"
    echo "  status          Show deployment status"
    echo ""
    echo "Examples:"
    echo "  ./dev.sh deploy-dev     # Deploy to dev, test changes"
    echo "  ./dev.sh test-sudoku    # Test Sudoku locally before deploy"
    echo "  ./dev.sh trigger-dev    # Manually run dev scheduler"
}

if [ -z "$1" ]; then
    show_help
    exit 0
fi

case "$1" in
    deploy-dev)
        echo -e "${BLUE}Deploying to DEVELOPMENT...${NC}"
        ./deploy.sh --dev
        echo ""
        echo -e "${GREEN}✅ Dev deployment complete!${NC}"
        echo "Next: ./dev.sh trigger-dev to test"
        ;;
    
    deploy-prod)
        echo -e "${YELLOW}⚠️  Deploying to PRODUCTION...${NC}"
        ./deploy.sh --prod
        ;;
    
    test-local)
        echo -e "${BLUE}Testing locally with dev config...${NC}"
        python main.py --test
        ;;
    
    test-sudoku)
        echo -e "${BLUE}Generating test Sudoku puzzle...${NC}"
        python main.py --test --game-type MINI_SUDOKU_6X6
        ;;
    
    test-zip)
        echo -e "${BLUE}Generating test ZIP puzzle...${NC}"
        python main.py --test --game-type ZIP
        ;;
    
    trigger-dev)
        echo -e "${BLUE}Triggering dev scheduler jobs...${NC}"
        gcloud config set project brainburst-dev
        echo "Sudoku:"
        gcloud scheduler jobs run daily-puzzle-sudoku-dev --location=us-central1
        echo ""
        echo "ZIP:"
        gcloud scheduler jobs run daily-puzzle-zip-dev --location=us-central1
        echo ""
        echo -e "${GREEN}✅ Dev jobs triggered! Check Firestore in ~30 seconds.${NC}"
        ;;
    
    trigger-prod)
        echo -e "${YELLOW}⚠️  Triggering PRODUCTION scheduler jobs...${NC}"
        gcloud config set project brainburst-bb78e
        echo "Sudoku:"
        gcloud scheduler jobs run daily-puzzle-sudoku --location=us-central1
        echo ""
        echo "ZIP:"
        gcloud scheduler jobs run daily-puzzle-zip --location=us-central1
        ;;
    
    logs-dev)
        echo -e "${BLUE}Viewing dev Cloud Function logs...${NC}"
        gcloud config set project brainburst-dev
        gcloud functions logs read generate-daily-puzzle-dev --region=us-central1 --limit=50
        ;;
    
    logs-prod)
        echo -e "${BLUE}Viewing prod Cloud Function logs...${NC}"
        gcloud config set project brainburst-bb78e
        gcloud functions logs read generate-daily-puzzle --region=us-central1 --limit=50
        ;;
    
    status)
        echo -e "${BLUE}=== Development Environment ===${NC}"
        gcloud config set project brainburst-dev --quiet
        echo "Functions:"
        gcloud functions list --gen2 --region=us-central1 2>/dev/null || echo "  No functions deployed"
        echo ""
        echo "Scheduler Jobs:"
        gcloud scheduler jobs list --location=us-central1 2>/dev/null || echo "  No jobs configured"
        echo ""
        echo -e "${BLUE}=== Production Environment ===${NC}"
        gcloud config set project brainburst-bb78e --quiet
        echo "Functions:"
        gcloud functions list --gen2 --region=us-central1 2>/dev/null || echo "  No functions deployed"
        echo ""
        echo "Scheduler Jobs:"
        gcloud scheduler jobs list --location=us-central1 2>/dev/null || echo "  No jobs configured"
        ;;
    
    *)
        echo -e "${YELLOW}Unknown command: $1${NC}"
        echo ""
        show_help
        exit 1
        ;;
esac
