#!/bin/bash

# Generate today's Tango puzzle
# Usage: ./generate_tango.sh [date]
# If no date is provided, generates for today

cd "$(dirname "$0")"

if [ -z "$1" ]; then
    echo "Generating Tango puzzle for today..."
    python3 main.py --game-type TANGO
else
    echo "Generating Tango puzzle for $1..."
    python3 main.py --game-type TANGO --date "$1"
fi
