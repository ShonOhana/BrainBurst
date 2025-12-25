#!/bin/bash
# BrainBurst Backend Setup Script

set -e

echo "🚀 Setting up BrainBurst Backend..."
echo ""

# Check Python version
echo "📦 Checking Python version..."
python3 --version || { echo "❌ Python 3 not found. Please install Python 3.11+"; exit 1; }

# Create virtual environment
echo "🔧 Creating virtual environment..."
python3 -m venv venv

# Activate virtual environment
echo "✅ Activating virtual environment..."
source venv/bin/activate

# Upgrade pip
echo "📦 Upgrading pip..."
pip install --upgrade pip

# Install dependencies
echo "📦 Installing dependencies..."
pip install -r requirements.txt

# Create .env if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file..."
    if [ -f .env.example ]; then
        cp .env.example .env
        echo "⚠️  Please edit .env and add your API keys!"
    else
        # Create .env from scratch if .env.example doesn't exist
        cat > .env << EOF
# OpenAI API Key
OPENAI_API_KEY=sk-proj-...your-key-here

# Firebase Service Account JSON path
FIREBASE_SERVICE_ACCOUNT_PATH=./serviceAccountKey.json

# Firebase Project ID
FIREBASE_PROJECT_ID=brainburst-bb78e
EOF
        echo "⚠️  Please edit .env and add your API keys!"
    fi
else
    echo "✅ .env file already exists"
fi

echo ""
echo "✅ Setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit .env file and add your keys:"
echo "   - OPENAI_API_KEY=sk-..."
echo "   - Download Firebase service account key"
echo ""
echo "2. Activate the virtual environment:"
echo "   source venv/bin/activate"
echo ""
echo "3. Test puzzle generation:"
echo "   python main.py --test"
echo ""

