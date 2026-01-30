"""
BrainBurst Backend - Daily Puzzle Generator
Cloud Function entry point for generating and storing daily puzzles
"""
import os
import json
from datetime import datetime, timezone
from typing import Dict, Any

# Load environment variables from .env file (for local development)
from dotenv import load_dotenv
load_dotenv()

# Third-party imports
import firebase_admin
from firebase_admin import credentials, firestore
from openai import OpenAI
import functions_framework

# Local imports
from generators import SudokuGenerator, ZipGenerator, TangoGenerator
from validators import SudokuValidator
from firestore_writer import FirestoreWriter


# Initialize Firebase Admin (only once)
if not firebase_admin._apps:
    # In Cloud Functions, credentials are automatic
    # For local development, use service account JSON
    service_account_path = os.getenv('FIREBASE_SERVICE_ACCOUNT_PATH', './serviceAccountKey.json')
    
    # Try to load from environment variable path, or default location
    if os.path.exists(service_account_path):
        print(f"🔐 Loading Firebase credentials from: {service_account_path}")
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred)
    else:
        # Try default location
        default_path = './serviceAccountKey.json'
        if os.path.exists(default_path):
            print(f"🔐 Loading Firebase credentials from: {default_path}")
            cred = credentials.Certificate(default_path)
            firebase_admin.initialize_app(cred)
        else:
            # Use default credentials (works in Cloud Functions)
            print("⚠️  No service account key found, trying default credentials...")
            firebase_admin.initialize_app()

# Initialize clients
db = firestore.client()

# Initialize generator registry (no OpenAI dependency needed)
GENERATORS = {
    "MINI_SUDOKU_6X6": SudokuGenerator(),  # Deterministic generator, no API key needed
    "ZIP": ZipGenerator(),  # ZIP puzzle generator
    "TANGO": TangoGenerator(),  # Tango puzzle generator
}

VALIDATORS = {
    "MINI_SUDOKU_6X6": SudokuValidator(size=6, block_rows=2, block_cols=3),
    # ZIP doesn't need complex validation - basic structure is enough
}


@functions_framework.http
def generate_daily_puzzle(request):
    """
    HTTP Cloud Function to generate daily puzzles.
    
    Request body (JSON):
    {
        "gameType": "MINI_SUDOKU_6X6",
        "date": "2025-12-25"  // Optional, defaults to today
    }
    
    Response:
    {
        "success": true,
        "puzzleId": "MINI_SUDOKU_6X6_2025-12-25",
        "message": "Puzzle generated successfully"
    }
    """
    try:
        # Parse request
        request_json = request.get_json(silent=True)
        if not request_json:
            return {
                "success": False,
                "error": "No JSON body provided"
            }, 400
        
        game_type = request_json.get('gameType', 'MINI_SUDOKU_6X6')
        date_str = request_json.get('date', datetime.now(timezone.utc).strftime('%Y-%m-%d'))
        
        # Validate game type
        if game_type not in GENERATORS:
            return {
                "success": False,
                "error": f"Unknown game type: {game_type}"
            }, 400
        
        # Generate puzzle
        result = _generate_and_store_puzzle(game_type, date_str)
        
        if result["success"]:
            return result, 200
        else:
            return result, 500
            
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        return {
            "success": False,
            "error": str(e)
        }, 500


def _generate_and_store_puzzle(game_type: str, date_str: str, force: bool = False) -> Dict[str, Any]:
    """
    Generate and store a puzzle in Firestore.
    
    Args:
        game_type: Game type string
        date_str: Date string
        force: If True, regenerate even if puzzle exists
    
    Returns:
        Dictionary with success status and details
    """
    writer = FirestoreWriter(db)
    
    # Check if puzzle already exists (unless forcing)
    if not force and writer.puzzle_exists(game_type, date_str):
        return {
            "success": True,
            "puzzleId": f"{game_type}_{date_str}",
            "message": "Puzzle already exists (not regenerated). Use --force to regenerate.",
            "alreadyExists": True
        }
    
    print(f"🎮 Generating {game_type} puzzle for {date_str}...")
    
    # Generate with retry logic (up to 3 attempts)
    generator = GENERATORS[game_type]
    validator = VALIDATORS.get(game_type)  # ZIP doesn't have a validator
    max_attempts = 5  # More attempts to get valid puzzle
    payload = None
    
    for attempt in range(1, max_attempts + 1):
        try:
            print(f"   Attempt {attempt}/{max_attempts}...")
            payload = generator.generate_payload(date_str)
            print(f"✅ Payload generated")
            
            # Validate payload if validator exists
            if validator:
                is_valid, error_msg = validator.validate_payload(payload)
                
                if is_valid:
                    print(f"✅ Payload validated")
                    break
                else:
                    print(f"❌ Validation failed: {error_msg}")
                    if attempt < max_attempts:
                        print(f"   Retrying with new generation...")
                        continue
                    else:
                        return {
                            "success": False,
                            "error": f"Validation failed after {max_attempts} attempts: {error_msg}"
                        }
            else:
                # No validator - basic structure check passed
                print(f"✅ Basic structure validated")
                break
        except Exception as e:
            print(f"❌ Generation failed: {e}")
            if attempt < max_attempts:
                print(f"   Retrying...")
                continue
            else:
                return {
                    "success": False,
                    "error": f"Failed to generate puzzle after {max_attempts} attempts: {str(e)}"
                }
    
    print(f"✅ Payload validated")
    
    # 3. Delete old puzzles for this game type (keep only today's puzzle)
    # This also deletes all associated user results to maintain data consistency
    deleted_count = writer.delete_old_puzzles(game_type, date_str)
    
    # 4. Write new puzzle to Firestore
    puzzle_id = writer.write_puzzle(game_type, date_str, payload)
    
    # Build success message with appropriate stats
    result_data = {
        "success": True,
        "puzzleId": puzzle_id,
        "message": "Puzzle generated and stored successfully (old puzzles and results cleaned up)",
        "deletedOldPuzzles": deleted_count
    }
    
    # Add game-specific stats
    if game_type == "MINI_SUDOKU_6X6":
        result_data["givens"] = sum(1 for row in payload["initialBoard"] for cell in row if cell != 0)
    elif game_type == "ZIP":
        result_data["dots"] = len(payload.get("dots", []))
    elif game_type == "TANGO":
        result_data["prefilled"] = len(payload.get("prefilled", []))
        result_data["clues"] = len(payload.get("equalClues", [])) + len(payload.get("oppositeClues", []))
    
    return result_data


def main_cli():
    """Command-line interface for local testing"""
    import argparse
    from dotenv import load_dotenv
    
    # Load environment variables
    load_dotenv()
    
    parser = argparse.ArgumentParser(description='Generate BrainBurst puzzles')
    parser.add_argument('--test', action='store_true', help='Generate a test puzzle')
    parser.add_argument('--game-type', default='MINI_SUDOKU_6X6', help='Game type')
    parser.add_argument('--date', help='Date (YYYY-MM-DD), defaults to today')
    parser.add_argument('--force', action='store_true', help='Force regeneration even if puzzle exists')
    
    args = parser.parse_args()
    
    if args.test or not args.date:
        date_str = datetime.now(timezone.utc).strftime('%Y-%m-%d')
    else:
        date_str = args.date
    
    print(f"\n🚀 BrainBurst Puzzle Generator")
    print(f"📅 Date: {date_str}")
    print(f"🎮 Game Type: {args.game_type}\n")
    
    result = _generate_and_store_puzzle(args.game_type, date_str, force=args.force)
    
    print(f"\n📊 Result:")
    print(json.dumps(result, indent=2))
    
    if result["success"]:
        print(f"\n✅ Success! Puzzle ID: {result['puzzleId']}")
        print(f"🎯 Check your Firestore console to see the puzzle!")
    else:
        print(f"\n❌ Failed: {result.get('error', 'Unknown error')}")


if __name__ == '__main__':
    main_cli()

