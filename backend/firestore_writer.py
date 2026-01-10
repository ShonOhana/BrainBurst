"""Firestore writer for puzzle storage"""
import json
from typing import Dict, Any
from firebase_admin import firestore
from datetime import datetime


class FirestoreWriter:
    """Writes puzzles to Firestore"""
    
    def __init__(self, db):
        """
        Initialize writer with Firestore database instance.
        
        Args:
            db: firebase_admin.firestore.client() instance
        """
        self.db = db
    
    def write_puzzle(
        self, 
        game_type: str, 
        date_str: str, 
        payload: Dict[str, Any]
    ) -> str:
        """
        Write a puzzle to Firestore.
        
        Args:
            game_type: e.g., "MINI_SUDOKU_6X6"
            date_str: e.g., "2025-12-25"
            payload: The puzzle data (will be serialized to JSON string)
            
        Returns:
            puzzle_id: The document ID that was created
        """
        puzzle_id = f"{game_type}_{date_str}"
        
        # Serialize payload to JSON string (same as AdminPuzzleUploader)
        payload_json = json.dumps(payload)
        
        puzzle_doc = {
            "puzzleId": puzzle_id,  # Include puzzleId field
            "gameType": game_type,
            "date": date_str,
            "payloadJson": payload_json,  # Store as JSON string
            "createdAt": firestore.SERVER_TIMESTAMP,
            "generatedBy": "deterministic-algorithm"
        }
        
        # Write to Firestore
        doc_ref = self.db.collection("puzzles").document(puzzle_id)
        doc_ref.set(puzzle_doc)
        
        print(f"✅ Puzzle written to Firestore: {puzzle_id}")
        return puzzle_id
    
    def puzzle_exists(self, game_type: str, date_str: str) -> bool:
        """Check if a puzzle already exists for the given game type and date"""
        puzzle_id = f"{game_type}_{date_str}"
        doc_ref = self.db.collection("puzzles").document(puzzle_id)
        return doc_ref.get().exists
    
    def delete_old_puzzles(self, game_type: str, keep_date: str) -> int:
        """
        Delete all puzzles for a game type except the one for keep_date.
        Also deletes all associated user results.
        
        Args:
            game_type: e.g., "MINI_SUDOKU_6X6"
            keep_date: Date string to keep (e.g., "2025-12-27")
            
        Returns:
            Number of puzzles deleted
        """
        puzzles_ref = self.db.collection("puzzles")
        # Query all puzzles for this game type
        puzzles = puzzles_ref.where("gameType", "==", game_type).stream()
        
        deleted_puzzle_count = 0
        deleted_results_count = 0
        keep_puzzle_id = f"{game_type}_{keep_date}"
        
        for puzzle in puzzles:
            if puzzle.id != keep_puzzle_id:
                puzzle_id = puzzle.id
                
                # First, delete all results associated with this puzzle
                results_deleted = self._delete_results_for_puzzle(puzzle_id)
                deleted_results_count += results_deleted
                
                # Then delete the puzzle itself
                puzzle.reference.delete()
                deleted_puzzle_count += 1
                print(f"🗑️  Deleted old puzzle: {puzzle_id} ({results_deleted} results)")
        
        if deleted_puzzle_count > 0:
            print(f"✅ Deleted {deleted_puzzle_count} old puzzle(s) and {deleted_results_count} result(s), kept: {keep_puzzle_id}")
        else:
            print(f"ℹ️  No old puzzles to delete")
        
        return deleted_puzzle_count
    
    def _delete_results_for_puzzle(self, puzzle_id: str) -> int:
        """
        Delete all user results associated with a specific puzzle.
        
        Args:
            puzzle_id: The puzzle ID to delete results for
            
        Returns:
            Number of results deleted
        """
        results_ref = self.db.collection("results")
        # Query all results for this puzzle
        results = results_ref.where("puzzleId", "==", puzzle_id).stream()
        
        deleted_count = 0
        for result in results:
            result.reference.delete()
            deleted_count += 1
        
        return deleted_count

