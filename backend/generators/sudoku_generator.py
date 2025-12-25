"""Sudoku 6×6 puzzle generator using deterministic algorithms"""
import json
import random
from typing import Dict, Any, List, Tuple, Optional


class SudokuGenerator:
    """Generates 6×6 Sudoku puzzles using backtracking algorithms"""
    
    def __init__(self, openai_client=None):
        # openai_client parameter kept for API compatibility but not used
        self.size = 6
        self.block_rows = 2
        self.block_cols = 3
        self.numbers = [1, 2, 3, 4, 5, 6]
    
    def generate_payload(self, date_str: str) -> Dict[str, Any]:
        """
        Generate a valid 6×6 Sudoku puzzle.
        
        Args:
            date_str: Date string (used for seeding randomness for variety)
            
        Returns:
            Dictionary matching Sudoku6x6Payload schema:
            {
                "size": 6,
                "blockRows": 2,
                "blockCols": 3,
                "initialBoard": [[...]], # 6×6 with 0 for empty
                "solutionBoard": [[...]] # 6×6 complete solution
            }
        """
        # Use system time for true randomness - each generation will be different
        # This ensures variety: different puzzles, different zero positions, different visible numbers
        
        # Randomly select difficulty
        difficulty = random.choice(["easy", "medium", "hard"])
        
        # Generate solution board
        solution_board = self._generate_solution_board()
        
        # Generate puzzle with unique solution
        initial_board = self._generate_puzzle_with_unique_solution(solution_board, difficulty)
        
        return {
            "size": self.size,
            "blockRows": self.block_rows,
            "blockCols": self.block_cols,
            "initialBoard": initial_board,
            "solutionBoard": solution_board
        }
    
    def _generate_solution_board(self) -> List[List[int]]:
        """Generate a valid complete 6×6 Sudoku solution using backtracking"""
        board = [[0] * self.size for _ in range(self.size)]
        
        # Create shuffled list of all positions for random traversal
        positions = [(r, c) for r in range(self.size) for c in range(self.size)]
        random.shuffle(positions)
        
        # Create shuffled list of numbers for each position
        number_order = self.numbers.copy()
        random.shuffle(number_order)
        
        def is_valid(row: int, col: int, num: int) -> bool:
            """Check if placing num at (row, col) is valid"""
            # Check row
            if num in board[row]:
                return False
            
            # Check column
            if num in [board[r][col] for r in range(self.size)]:
                return False
            
            # Check 2×3 block
            block_row_start = (row // self.block_rows) * self.block_rows
            block_col_start = (col // self.block_cols) * self.block_cols
            
            for r in range(block_row_start, block_row_start + self.block_rows):
                for c in range(block_col_start, block_col_start + self.block_cols):
                    if board[r][c] == num:
                        return False
            
            return True
        
        def solve_backtrack(pos_index: int) -> bool:
            """Backtracking solver to fill the board"""
            if pos_index >= len(positions):
                return True
            
            row, col = positions[pos_index]
            
            # Try numbers in random order
            nums = self.numbers.copy()
            random.shuffle(nums)
            
            for num in nums:
                if is_valid(row, col, num):
                    board[row][col] = num
                    if solve_backtrack(pos_index + 1):
                        return True
                    board[row][col] = 0
            
            return False
        
        # Generate solution
        solve_backtrack(0)
        return board
    
    def _solve_and_count_solutions(self, board: List[List[int]], max_solutions: int = 2) -> Tuple[int, int]:
        """
        Solve the puzzle and count solutions, tracking maximum backtracking depth.
        
        Returns:
            (solution_count, max_backtrack_depth)
            max_backtrack_depth: maximum depth reached during solving (0 = pure logic, >0 = branching)
        """
        solution_count = 0
        max_depth = 0
        
        def is_valid(row: int, col: int, num: int, board_state: List[List[int]]) -> bool:
            """Check if placing num at (row, col) is valid"""
            # Check row
            if num in board_state[row]:
                return False
            
            # Check column
            if num in [board_state[r][col] for r in range(self.size)]:
                return False
            
            # Check 2×3 block
            block_row_start = (row // self.block_rows) * self.block_rows
            block_col_start = (col // self.block_cols) * self.block_cols
            
            for r in range(block_row_start, block_row_start + self.block_rows):
                for c in range(block_col_start, block_col_start + self.block_cols):
                    if board_state[r][c] == num:
                        return False
            
            return True
        
        def count_solutions_backtrack(board_state: List[List[int]], depth: int) -> None:
            """Recursive backtracking to count solutions"""
            nonlocal solution_count, max_depth
            
            max_depth = max(max_depth, depth)
            
            # Find first empty cell
            empty_pos = None
            for r in range(self.size):
                for c in range(self.size):
                    if board_state[r][c] == 0:
                        empty_pos = (r, c)
                        break
                if empty_pos:
                    break
            
            if empty_pos is None:
                # Board is complete - found a solution
                solution_count += 1
                return
            
            row, col = empty_pos
            
            # Try all valid numbers
            for num in self.numbers:
                if solution_count >= max_solutions:
                    return  # Early exit if we found multiple solutions
                
                if is_valid(row, col, num, board_state):
                    board_state[row][col] = num
                    count_solutions_backtrack(board_state, depth + 1)
                    board_state[row][col] = 0
        
        # Create copy of board to avoid modifying original
        board_copy = [row[:] for row in board]
        count_solutions_backtrack(board_copy, 0)
        
        return solution_count, max_depth
    
    def _generate_puzzle_with_unique_solution(
        self, 
        solution_board: List[List[int]], 
        difficulty: str
    ) -> List[List[int]]:
        """
        Generate initial board by removing numbers, ensuring unique solution.
        
        Difficulty levels based on solver backtracking depth:
        - Easy: depth = 0 (pure logic, no branching)
        - Medium: depth = 1 (limited branching)
        - Hard: depth > 1 (deep branching)
        """
        # Start with complete solution
        initial_board = [row[:] for row in solution_board]
        
        # Create shuffled list of all positions for random removal order
        positions = [(r, c) for r in range(self.size) for c in range(self.size)]
        random.shuffle(positions)
        
        # Target givens based on difficulty (approximate ranges)
        difficulty_targets = {
            "easy": (24, 28),      # More givens = easier
            "medium": (18, 22),    # Moderate givens
            "hard": (12, 16)       # Fewer givens = harder
        }
        min_givens, max_givens = difficulty_targets.get(difficulty, (18, 22))
        target_givens = random.randint(min_givens, max_givens)
        
        # Track what we've tried to remove
        removal_order = positions.copy()
        random.shuffle(removal_order)
        
        attempts = 0
        max_attempts = 200  # Safety limit
        
        for row, col in removal_order:
            if attempts >= max_attempts:
                break
            
            # Skip if already empty
            if initial_board[row][col] == 0:
                continue
            
            # Check current givens count
            current_givens = sum(1 for r in range(self.size) for c in range(self.size) if initial_board[r][c] != 0)
            
            # Stop if we've reached or gone below target minimum
            if current_givens <= min_givens:
                break
            
            # Try removing this cell
            original_value = initial_board[row][col]
            initial_board[row][col] = 0
            
            # Count solutions with backtracking depth tracking
            solution_count, max_depth = self._solve_and_count_solutions(initial_board, max_solutions=2)
            
            # Check if puzzle still has unique solution
            if solution_count == 1:
                # Successfully removed - check if we should continue
                current_givens_after = sum(1 for r in range(self.size) for c in range(self.size) if initial_board[r][c] != 0)
                
                # If we're still above target, continue
                # If we're at or below target, we're done
                if current_givens_after <= min_givens:
                    break
            else:
                # Multiple solutions or no solution - revert removal
                initial_board[row][col] = original_value
            
            attempts += 1
        
        # Final verification
        solution_count, max_depth = self._solve_and_count_solutions(initial_board, max_solutions=2)
        
        if solution_count != 1:
            # If we somehow ended up with non-unique solution, restore some cells
            # This should rarely happen, but provide safety fallback
            for row, col in removal_order[:5]:
                if initial_board[row][col] == 0:
                    initial_board[row][col] = solution_board[row][col]
                    solution_count, _ = self._solve_and_count_solutions(initial_board, max_solutions=2)
                    if solution_count == 1:
                        break
        
        return initial_board
