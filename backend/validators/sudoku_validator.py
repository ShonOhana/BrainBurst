"""Sudoku 6×6 puzzle validator"""
from typing import List, Tuple


class SudokuValidator:
    """Validates 6×6 Sudoku puzzles with 2×3 blocks"""
    
    def __init__(self, size: int = 6, block_rows: int = 2, block_cols: int = 3):
        self.size = size
        self.block_rows = block_rows
        self.block_cols = block_cols
    
    def validate_payload(self, payload: dict) -> Tuple[bool, str]:
        """
        Validate a Sudoku payload.
        
        Returns:
            (is_valid, error_message) tuple
        """
        # Check required fields
        required_fields = ["size", "blockRows", "blockCols", "initialBoard", "solutionBoard"]
        for field in required_fields:
            if field not in payload:
                return False, f"Missing required field: {field}"
        
        # Check dimensions
        if payload["size"] != self.size:
            return False, f"Invalid size: expected {self.size}, got {payload['size']}"
        
        if payload["blockRows"] != self.block_rows or payload["blockCols"] != self.block_cols:
            return False, f"Invalid block dimensions"
        
        initial_board = payload["initialBoard"]
        solution_board = payload["solutionBoard"]
        
        # Validate board structures
        if not self._validate_board_structure(initial_board):
            return False, "initialBoard has invalid structure"
        
        if not self._validate_board_structure(solution_board):
            return False, "solutionBoard has invalid structure"
        
        # Validate solution board is complete and correct
        is_valid, msg = self._validate_complete_sudoku(solution_board)
        if not is_valid:
            return False, f"solutionBoard is invalid: {msg}"
        
        # Validate initial board respects solution
        if not self._validate_initial_matches_solution(initial_board, solution_board):
            return False, "initialBoard does not match solutionBoard"
        
        # Check initial board has reasonable number of givens
        # Allow range for Easy (24-28), Medium (18-22), Hard (12-16)
        givens = sum(1 for row in initial_board for cell in row if cell != 0)
        if givens < 12 or givens > 28:
            return False, f"Invalid number of givens: {givens} (expected 12-28 for valid difficulty levels)"
        
        return True, "Valid"
    
    def _validate_board_structure(self, board: List[List[int]]) -> bool:
        """Check if board is a valid 6×6 grid with numbers 0-6"""
        if not isinstance(board, list) or len(board) != self.size:
            return False
        
        for row in board:
            if not isinstance(row, list) or len(row) != self.size:
                return False
            for cell in row:
                if not isinstance(cell, int) or cell < 0 or cell > self.size:
                    return False
        
        return True
    
    def _validate_complete_sudoku(self, board: List[List[int]]) -> Tuple[bool, str]:
        """Validate that a complete Sudoku board follows all rules"""
        # Check rows
        for i, row in enumerate(board):
            if sorted(row) != list(range(1, self.size + 1)):
                return False, f"Row {i} is invalid: {row}"
        
        # Check columns
        for col_idx in range(self.size):
            column = [board[row_idx][col_idx] for row_idx in range(self.size)]
            if sorted(column) != list(range(1, self.size + 1)):
                return False, f"Column {col_idx} is invalid"
        
        # Check blocks
        for block_row in range(self.size // self.block_rows):
            for block_col in range(self.size // self.block_cols):
                block = self._get_block(board, block_row, block_col)
                if sorted(block) != list(range(1, self.size + 1)):
                    return False, f"Block ({block_row}, {block_col}) is invalid"
        
        return True, "Valid"
    
    def _get_block(self, board: List[List[int]], block_row: int, block_col: int) -> List[int]:
        """Extract a single block from the board"""
        start_row = block_row * self.block_rows
        start_col = block_col * self.block_cols
        
        block = []
        for r in range(start_row, start_row + self.block_rows):
            for c in range(start_col, start_col + self.block_cols):
                block.append(board[r][c])
        
        return block
    
    def _validate_initial_matches_solution(
        self, 
        initial_board: List[List[int]], 
        solution_board: List[List[int]]
    ) -> bool:
        """Check that all non-zero entries in initial_board match solution_board"""
        for r in range(self.size):
            for c in range(self.size):
                if initial_board[r][c] != 0:
                    if initial_board[r][c] != solution_board[r][c]:
                        return False
        return True

