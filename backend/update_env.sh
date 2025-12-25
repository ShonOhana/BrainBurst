#!/bin/bash
# Helper script to update OPENAI_API_KEY in .env file

if [ -z "$1" ]; then
    echo "Usage: ./update_env.sh YOUR_OPENAI_API_KEY"
    echo ""
    echo "Example:"
    echo "  ./update_env.sh sk-proj-ABC123xyz..."
    exit 1
fi

API_KEY="$1"

# Update the .env file
cd "$(dirname "$0")"

# Use sed to replace the OPENAI_API_KEY line
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s|OPENAI_API_KEY=.*|OPENAI_API_KEY=$API_KEY|" .env
else
    # Linux
    sed -i "s|OPENAI_API_KEY=.*|OPENAI_API_KEY=$API_KEY|" .env
fi

echo "✅ OpenAI API key updated in .env file!"
echo ""
echo "Current .env content (API key partially hidden):"
grep OPENAI_API_KEY .env | sed 's/\(sk-proj-[A-Za-z0-9]\{8\}\).*/\1.../'

