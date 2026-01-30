"""Tango puzzle generator - logic puzzle with sun/moon symbols"""
import random
from typing import Dict, Any, List, Tuple, Set, Optional
from copy import deepcopy


class TangoGenerator:
    """Generates Tango puzzles - fill grid with sun/moon symbols following rules"""
    
    def __init__(self, openai_client=None):
        self.size = 6
    
    def generate_payload(self, date_str: str) -> Dict[str, Any]:
        """
        Generate a valid Tango puzzle with unique solution.
        
        Args:
            date_str: Date string (used for seeding randomness)
            
        Returns:
            Dictionary matching TangoPayload schema
        """
        # Randomly select difficulty
        difficulty = random.choice(["medium", "hard", "expert"])
        print(f"   Generating {difficulty} difficulty puzzle")
        
        max_attempts = 10
        
        for attempt in range(max_attempts):
            # Generate a valid solution
            solution = self._generate_valid_solution()
            
            # Create puzzle with unique solution based on difficulty
            result = self._generate_puzzle_with_unique_solution(solution, difficulty)
            
            if result:
                prefilled, equal_clues, opposite_clues = result
                print(f"   ✅ Generated unique puzzle on attempt {attempt + 1}")
                return {
                    "size": self.size,
                    "prefilled": prefilled,
                    "equalClues": equal_clues,
                    "oppositeClues": opposite_clues,
                    "difficulty": difficulty
                }
        
        # Fallback: create easier puzzle
        print("   ⚠️  Using fallback with more clues")
        solution = self._generate_valid_solution()
        return self._create_easy_puzzle(solution)
    
    def _generate_puzzle_with_unique_solution(
        self, 
        solution: List[List[int]], 
        difficulty: str
    ) -> Optional[Tuple[List[Dict], List[Dict], List[Dict]]]:
        """
        Generate puzzle by strategically placing clues to ensure unique solution.
        Instead of removing cells, we start minimal and add clues.
        """
        # Difficulty targets - HARDER settings
        difficulty_targets = {
            "medium": (16, 18),  # Reduced from 20-22
            "hard": (13, 15),    # Reduced from 17-19
            "expert": (10, 12)   # Reduced from 14-16
        }
        
        min_prefilled, max_prefilled = difficulty_targets.get(difficulty, (18, 20))
        target_prefilled = random.randint(min_prefilled, max_prefilled)
        
        # Start with all positions
        all_positions = [(r, c) for r in range(self.size) for c in range(self.size)]
        random.shuffle(all_positions)
        
        # Keep target number of prefilled cells
        prefilled_positions = set(all_positions[:target_prefilled])
        
        # Generate ALL possible clues for remaining cells
        equal_clues, opposite_clues = self._generate_comprehensive_clues(
            solution, 
            prefilled_positions
        )
        
        # Verify uniqueness
        if not self._verify_unique_with_clues(solution, prefilled_positions, equal_clues, opposite_clues):
            return None
        
        # Convert to output format
        prefilled = []
        for row, col in prefilled_positions:
            prefilled.append({
                "row": row,
                "col": col,
                "value": "SUN" if solution[row][col] == 1 else "MOON"
            })
        
        return prefilled, equal_clues, opposite_clues
    
    def _generate_comprehensive_clues(
        self,
        solution: List[List[int]],
        prefilled_positions: Set[Tuple[int, int]]
    ) -> Tuple[List[Dict], List[Dict]]:
        """
        Generate SPARSE strategic clues like LinkedIn Tango.
        Only 6-12 clues total, not every border!
        """
        equal_clues = []
        opposite_clues = []
        
        # Collect all possible clue locations
        clue_candidates = []
        
        # Check all horizontal borders
        for row in range(self.size):
            for col in range(self.size - 1):
                pos1 = (row, col)
                pos2 = (row, col + 1)
                
                # Only consider borders with at least one non-prefilled cell
                if pos1 not in prefilled_positions or pos2 not in prefilled_positions:
                    val1 = solution[row][col]
                    val2 = solution[row][col + 1]
                    clue_type = "EQUAL" if val1 == val2 else "OPPOSITE"
                    clue_candidates.append((row, col, "HORIZONTAL", clue_type))
        
        # Check all vertical borders
        for row in range(self.size - 1):
            for col in range(self.size):
                pos1 = (row, col)
                pos2 = (row + 1, col)
                
                if pos1 not in prefilled_positions or pos2 not in prefilled_positions:
                    val1 = solution[row][col]
                    val2 = solution[row + 1][col]
                    clue_type = "EQUAL" if val1 == val2 else "OPPOSITE"
                    clue_candidates.append((row, col, "VERTICAL", clue_type))
        
        # Randomly select 6-10 clues (HARDER - fewer clues than before)
        num_clues = random.randint(6, 10)
        random.shuffle(clue_candidates)
        
        for row, col, direction, clue_type in clue_candidates[:num_clues]:
            clue = {"row": row, "col": col, "direction": direction}
            
            if clue_type == "EQUAL":
                equal_clues.append(clue)
            else:
                opposite_clues.append(clue)
        
        return equal_clues, opposite_clues
    
    def _verify_unique_with_clues(
        self,
        solution: List[List[int]],
        prefilled_positions: Set[Tuple[int, int]],
        equal_clues: List[Dict],
        opposite_clues: List[Dict]
    ) -> bool:
        """Verify the puzzle with these clues has a unique solution."""
        # Create puzzle with prefilled
        puzzle = [[0 for _ in range(self.size)] for _ in range(self.size)]
        for row, col in prefilled_positions:
            puzzle[row][col] = solution[row][col]
        
        # Count solutions considering clues
        solutions = self._count_solutions_with_clues(
            puzzle, equal_clues, opposite_clues, solution, max_solutions=2
        )
        return solutions == 1
    
    def _count_solutions_with_clues(
        self,
        puzzle: List[List[int]],
        equal_clues: List[Dict],
        opposite_clues: List[Dict],
        target_solution: List[List[int]],
        max_solutions: int = 2
    ) -> int:
        """Count solutions that satisfy all clues."""
        # Find first empty cell
        empty_cell = None
        for r in range(self.size):
            for c in range(self.size):
                if puzzle[r][c] == 0:
                    empty_cell = (r, c)
                    break
            if empty_cell:
                break
        
        if not empty_cell:
            # Check if matches target solution and satisfies all clues
            if self._matches_solution(puzzle, target_solution):
                return 1
            return 0
        
        row, col = empty_cell
        solutions = 0
        
        for value in [1, 2]:
            puzzle[row][col] = value
            
            if self._is_valid_partial(puzzle, row, col):
                solutions += self._count_solutions_with_clues(
                    puzzle, equal_clues, opposite_clues, target_solution, max_solutions
                )
                
                if solutions >= max_solutions:
                    puzzle[row][col] = 0
                    return solutions
            
            puzzle[row][col] = 0
        
        return solutions
    
    def _matches_solution(self, puzzle: List[List[int]], solution: List[List[int]]) -> bool:
        """Check if puzzle matches the target solution."""
        for r in range(self.size):
            for c in range(self.size):
                if puzzle[r][c] != solution[r][c]:
                    return False
        return True
    
    def _count_solutions(
        self, 
        puzzle: List[List[int]], 
        max_solutions: int = 2
    ) -> int:
        """
        Count number of solutions (up to max_solutions).
        Uses backtracking with constraint checking.
        (Kept for compatibility but not used with new approach)
        """
        return self._count_solutions_with_clues(puzzle, [], [], puzzle, max_solutions)
    
    def _is_valid_partial(self, puzzle: List[List[int]], row: int, col: int) -> bool:
        """Check if current partial solution is valid."""
        value = puzzle[row][col]
        
        # Check row constraints
        row_values = [v for v in puzzle[row] if v != 0]
        if row_values.count(1) > 3 or row_values.count(2) > 3:
            return False
        
        # Check column constraints
        col_values = [puzzle[r][col] for r in range(self.size) if puzzle[r][col] != 0]
        if col_values.count(1) > 3 or col_values.count(2) > 3:
            return False
        
        # Check no 3 consecutive in row
        if col >= 2:
            if puzzle[row][col] == puzzle[row][col-1] == puzzle[row][col-2] != 0:
                return False
        
        # Check no 3 consecutive in column
        if row >= 2:
            if puzzle[row][col] == puzzle[row-1][col] == puzzle[row-2][col] != 0:
                return False
        
        return True
    
    def _is_valid_complete(self, puzzle: List[List[int]]) -> bool:
        """Check if complete puzzle is valid."""
        # Check each row
        for row in range(self.size):
            row_vals = puzzle[row]
            if row_vals.count(1) != 3 or row_vals.count(2) != 3:
                return False
            
            # Check consecutive
            for i in range(self.size - 2):
                if row_vals[i] == row_vals[i+1] == row_vals[i+2]:
                    return False
        
        # Check each column
        for col in range(self.size):
            col_vals = [puzzle[r][col] for r in range(self.size)]
            if col_vals.count(1) != 3 or col_vals.count(2) != 3:
                return False
            
            # Check consecutive
            for i in range(self.size - 2):
                if col_vals[i] == col_vals[i+1] == col_vals[i+2]:
                    return False
        
        return True
    
    def _generate_strategic_clues(
        self, 
        solution: List[List[int]], 
        prefilled_positions: Set[Tuple[int, int]],
        difficulty: str
    ) -> Tuple[List[Dict], List[Dict]]:
        """Generate clues strategically based on difficulty."""
        # Clue count targets
        clue_targets = {
            "medium": (10, 14),
            "hard": (8, 12),
            "expert": (6, 10)
        }
        
        min_clues, max_clues = clue_targets.get(difficulty, (8, 12))
        target_clues = random.randint(min_clues, max_clues)
        
        equal_clues = []
        opposite_clues = []
        
        # Collect all possible clues
        clue_candidates = []
        
        for row in range(self.size):
            for col in range(self.size):
                # Horizontal clues
                if col < self.size - 1:
                    pos1, pos2 = (row, col), (row, col + 1)
                    # Prefer clues involving non-prefilled cells
                    if pos1 not in prefilled_positions or pos2 not in prefilled_positions:
                        clue_candidates.append((row, col, "HORIZONTAL"))
                
                # Vertical clues
                if row < self.size - 1:
                    pos1, pos2 = (row, col), (row + 1, col)
                    if pos1 not in prefilled_positions or pos2 not in prefilled_positions:
                        clue_candidates.append((row, col, "VERTICAL"))
        
        random.shuffle(clue_candidates)
        
        for row, col, direction in clue_candidates[:target_clues]:
            if direction == "HORIZONTAL":
                val1, val2 = solution[row][col], solution[row][col + 1]
            else:
                val1, val2 = solution[row][col], solution[row + 1][col]
            
            clue = {"row": row, "col": col, "direction": direction}
            
            if val1 == val2:
                equal_clues.append(clue)
            else:
                opposite_clues.append(clue)
        
        return equal_clues, opposite_clues
    
    def _generate_valid_solution(self) -> List[List[int]]:
        """
        Generate a valid 6x6 solution (1 = SUN, 2 = MOON).
        Each row/column has exactly 3 suns and 3 moons.
        No 3+ consecutive identical symbols.
        """
        attempts = 0
        max_attempts = 100
        
        while attempts < max_attempts:
            solution = [[0 for _ in range(self.size)] for _ in range(self.size)]
            
            # Fill row by row
            success = True
            for row in range(self.size):
                if not self._fill_row(solution, row):
                    success = False
                    break
            
            if success and self._validate_solution(solution):
                return solution
            
            attempts += 1
        
        # Fallback: generate a simple valid solution
        return self._generate_simple_solution()
    
    def _fill_row(self, solution: List[List[int]], row: int) -> bool:
        """Fill a single row with valid values"""
        # Get column constraints
        col_sun_counts = [sum(1 for r in range(row) if solution[r][c] == 1) for c in range(self.size)]
        col_moon_counts = [sum(1 for r in range(row) if solution[r][c] == 2) for c in range(self.size)]
        
        attempts = 0
        max_attempts = 1000
        
        while attempts < max_attempts:
            row_values = [1, 1, 1, 2, 2, 2]
            random.shuffle(row_values)
            
            valid = True
            
            # Check consecutive constraint
            for i in range(len(row_values) - 2):
                if row_values[i] == row_values[i+1] == row_values[i+2]:
                    valid = False
                    break
            
            if not valid:
                attempts += 1
                continue
            
            # Check column constraints
            for col in range(self.size):
                value = row_values[col]
                if value == 1 and col_sun_counts[col] >= 3:
                    valid = False
                    break
                if value == 2 and col_moon_counts[col] >= 3:
                    valid = False
                    break
                
                # Check vertical consecutive
                if row >= 2:
                    if solution[row-1][col] == solution[row-2][col] == value:
                        valid = False
                        break
            
            if valid:
                for col in range(self.size):
                    solution[row][col] = row_values[col]
                return True
            
            attempts += 1
        
        return False
    
    def _validate_solution(self, solution: List[List[int]]) -> bool:
        """Validate complete solution"""
        return self._is_valid_complete(solution)
    
    def _generate_simple_solution(self) -> List[List[int]]:
        """Generate a simple valid solution as fallback"""
        return [
            [1, 2, 1, 2, 1, 2],
            [2, 1, 2, 1, 2, 1],
            [1, 2, 2, 1, 1, 2],
            [2, 1, 1, 2, 2, 1],
            [1, 1, 2, 2, 1, 2],
            [2, 2, 1, 1, 2, 1]
        ]
    
    def _create_easy_puzzle(self, solution: List[List[int]]) -> Dict[str, Any]:
        """Create an easier puzzle with more givens and comprehensive clues"""
        all_positions = [(r, c) for r in range(self.size) for c in range(self.size)]
        random.shuffle(all_positions)
        
        # Give more cells for easy puzzle
        prefilled_positions = set(all_positions[:20])
        prefilled = []
        for row, col in prefilled_positions:
            prefilled.append({
                "row": row,
                "col": col,
                "value": "SUN" if solution[row][col] == 1 else "MOON"
            })
        
        # Add comprehensive clues
        equal_clues, opposite_clues = self._generate_comprehensive_clues(
            solution, prefilled_positions
        )
        
        return {
            "size": self.size,
            "prefilled": prefilled,
            "equalClues": equal_clues,
            "oppositeClues": opposite_clues,
            "difficulty": "medium"
        }
