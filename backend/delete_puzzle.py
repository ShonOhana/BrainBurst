"""
Helper script to delete a puzzle from Firestore
Useful for testing and cleanup
"""
import os
from dotenv import load_dotenv
import firebase_admin
from firebase_admin import credentials, firestore

# Load environment variables
load_dotenv()

# Initialize Firebase Admin
if not firebase_admin._apps:
    service_account_path = os.getenv('FIREBASE_SERVICE_ACCOUNT_PATH', './serviceAccountKey.json')
    if os.path.exists(service_account_path):
        print(f"🔐 Loading Firebase credentials from: {service_account_path}")
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred)
    else:
        print("❌ Firebase service account key not found!")
        exit(1)

db = firestore.client()

def delete_puzzle(game_type: str, date_str: str):
    """Delete a puzzle from Firestore"""
    puzzle_id = f"{game_type}_{date_str}"
    doc_ref = db.collection("puzzles").document(puzzle_id)
    
    if doc_ref.get().exists:
        doc_ref.delete()
        print(f"✅ Deleted puzzle: {puzzle_id}")
    else:
        print(f"⚠️  Puzzle not found: {puzzle_id}")

if __name__ == "__main__":
    import sys
    from datetime import datetime, timezone
    
    if len(sys.argv) > 1:
        date_str = sys.argv[1]
    else:
        date_str = datetime.now(timezone.utc).strftime('%Y-%m-%d')
    
    game_type = "MINI_SUDOKU_6X6"
    
    print(f"🗑️  Deleting puzzle for {date_str}...")
    delete_puzzle(game_type, date_str)
    print("✅ Done!")

