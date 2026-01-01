"""
Helper script to delete ALL puzzles from Firestore
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
        print("⚠️  No service account key found, trying default credentials...")
        firebase_admin.initialize_app()

db = firestore.client()

def delete_all_puzzles():
    """Delete all puzzles from Firestore"""
    puzzles_ref = db.collection("puzzles")
    puzzles = puzzles_ref.stream()
    
    count = 0
    for puzzle in puzzles:
        puzzle.reference.delete()
        count += 1
        print(f"✅ Deleted: {puzzle.id}")
    
    print(f"\n🗑️  Deleted {count} puzzle(s) from Firestore")
    return count

if __name__ == "__main__":
    print("🗑️  Deleting ALL puzzles from Firestore...")
    print("⚠️  This will delete all puzzles in the collection!")
    
    import sys
    if len(sys.argv) > 1 and sys.argv[1] != "--confirm":
        print("Usage: python delete_all_puzzles.py --confirm")
        print("This is a destructive operation. Use --confirm to proceed.")
        sys.exit(1)
    
    count = delete_all_puzzles()
    print(f"✅ Done! Deleted {count} puzzle(s)")


