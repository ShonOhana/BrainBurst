"""ZIP puzzle generator - connect numbered dots on 6x6 grid"""
import random
from typing import Dict, Any, List, Tuple, Set, Optional


class ZipGenerator:
    """Generates ZIP puzzles - connect the dots with a continuous path"""
    
    def __init__(self, openai_client=None):
        # openai_client parameter kept for API compatibility but not used
        self.size = 6
        self.min_dots = 2
        self.max_dots = 16
    
    def generate_payload(self, date_str: str) -> Dict[str, Any]:
        """
        Generate a valid ZIP puzzle.
        
        Args:
            date_str: Date string (used for seeding randomness for variety)
            
        Returns:
            Dictionary matching ZipPayload schema:
            {
                "size": 6,
                "dots": [
                    {"row": 0, "col": 1, "index": 1},
                    {"row": 2, "col": 3, "index": 2},
                    ...
                ]
            }
        """
        # Randomly select number of dots (between min and max)
        num_dots = random.randint(self.min_dots, self.max_dots)
        
        # Generate valid dot placements and solution path
        dots = self._generate_valid_zip_puzzle(num_dots)
        
        return {
            "size": self.size,
            "dots": dots
        }
    
    def _generate_valid_zip_puzzle(self, num_dots: int) -> List[Dict[str, int]]:
        """
        Generate dots that can be connected by a path that fills all 36 cells.
        
        Strategy: Generate a Hamiltonian path (visits all cells exactly once),
        then place dots along it.
        """
        max_attempts = 100
        
        for attempt in range(max_attempts):
            # Generate a path that visits all 36 cells
            path = self._generate_hamiltonian_path()
            
            if path and len(path) == 36:  # Must visit all cells
                # Place dots at selected positions along the path
                dot_positions = self._select_dot_positions(path, num_dots)
                
                # Convert to dot format
                dots = []
                for i, pos in enumerate(dot_positions):
                    dots.append({
                        "row": pos[0],
                        "col": pos[1],
                        "index": i + 1
                    })
                
                return dots
        
        # Fallback: simple snake pattern (guaranteed to work)
        return self._generate_snake_dots(num_dots)
    
    def _generate_hamiltonian_path(self) -> List[Tuple[int, int]]:
        """
        Generate a path that visits all 36 cells exactly once.
        Uses backtracking to find a Hamiltonian path.
        """
        # Try different starting positions
        start_positions = [
            (0, 0), (0, 5), (5, 0), (5, 5),  # corners
            (0, 2), (2, 0), (2, 5), (5, 2),  # edges
            (2, 2), (3, 3)  # middle
        ]
        
        random.shuffle(start_positions)
        
        for start_pos in start_positions:
            path = [start_pos]
            visited = {start_pos}
            
            if self._find_hamiltonian_path_recursive(path, visited):
                return path
        
        # If no Hamiltonian path found, return snake pattern
        return self._generate_snake_path()
    
    def _find_hamiltonian_path_recursive(
        self, 
        path: List[Tuple[int, int]], 
        visited: Set[Tuple[int, int]],
        max_depth: int = 36
    ) -> bool:
        """
        Recursively find a Hamiltonian path using backtracking.
        """
        if len(path) == 36:
            return True  # Found complete path
        
        if len(path) >= max_depth:
            return False  # Depth limit
        
        current = path[-1]
        neighbors = self._get_adjacent_cells(current[0], current[1])
        
        # Randomize order for variety
        random.shuffle(neighbors)
        
        for neighbor in neighbors:
            if neighbor not in visited:
                path.append(neighbor)
                visited.add(neighbor)
                
                if self._find_hamiltonian_path_recursive(path, visited, max_depth):
                    return True
                
                # Backtrack
                path.pop()
                visited.remove(neighbor)
        
        return False
    
    def _generate_snake_path(self) -> List[Tuple[int, int]]:
        """
        Generate a simple snake pattern that visits all cells.
        Guaranteed to work as a fallback.
        """
        path = []
        
        for row in range(self.size):
            if row % 2 == 0:
                # Left to right
                for col in range(self.size):
                    path.append((row, col))
            else:
                # Right to left
                for col in range(self.size - 1, -1, -1):
                    path.append((row, col))
        
        return path
    
    def _get_adjacent_cells(self, row: int, col: int) -> List[Tuple[int, int]]:
        """Get orthogonally adjacent cells within grid bounds."""
        adjacent = []
        
        # Up, down, left, right
        directions = [(-1, 0), (1, 0), (0, -1), (0, 1)]
        
        for dr, dc in directions:
            new_row, new_col = row + dr, col + dc
            if 0 <= new_row < self.size and 0 <= new_col < self.size:
                adjacent.append((new_row, new_col))
        
        return adjacent
    
    def _select_dot_positions(
        self, 
        path: List[Tuple[int, int]], 
        num_dots: int
    ) -> List[Tuple[int, int]]:
        """
        Select positions along the path to place numbered dots.
        
        Ensures dots are spread out along the path.
        """
        if len(path) < num_dots:
            # Path too short, use what we have
            return path[:num_dots]
        
        # Select evenly spaced positions along the path
        indices = []
        step = (len(path) - 1) / (num_dots - 1)
        
        for i in range(num_dots):
            index = int(round(i * step))
            indices.append(index)
        
        # Ensure we always include first and last
        indices[0] = 0
        indices[-1] = len(path) - 1
        
        return [path[i] for i in indices]
    
    def _generate_snake_dots(self, num_dots: int) -> List[Dict[str, int]]:
        """
        Fallback: Generate dots along a snake pattern that visits all cells.
        Guaranteed to be solvable.
        """
        path = self._generate_snake_path()
        
        # Select evenly spaced positions
        step = (len(path) - 1) / (num_dots - 1)
        selected_positions = [path[int(round(i * step))] for i in range(num_dots)]
        
        dots = []
        for i, pos in enumerate(selected_positions):
            dots.append({
                "row": pos[0],
                "col": pos[1],
                "index": i + 1
            })
        
        return dots
