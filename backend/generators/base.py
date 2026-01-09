"""Base protocol for game generators"""
from typing import Protocol, Dict, Any


class GameGenerator(Protocol):
    """Protocol for game puzzle generators"""
    
    def generate_payload(self, date_str: str) -> Dict[str, Any]:
        """
        Generate a puzzle payload for a specific date.
        
        Args:
            date_str: Date in format "YYYY-MM-DD"
            
        Returns:
            Dictionary containing the puzzle payload
        """
        ...





