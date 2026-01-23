"""ZIP puzzle generator - connect numbered dots on 6x6 grid"""
import random
import time
from typing import Dict, Any, List, Tuple, Set, Optional


class ZipGenerator:
    """Generates ZIP puzzles - connect the dots with a continuous path"""
    
    def __init__(self, openai_client=None):
        # openai_client parameter kept for API compatibility but not used
        self.size = 6
        self.min_dots = 4  # Increased from 2 - harder
        self.max_dots = 16  # Decreased from 16 - harder with fewer dots
        self.wall_probability = 0.7  # 70% chance to add walls
    
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
        dots, solution_path = self._generate_valid_zip_puzzle(num_dots)
        
        # Maybe add walls (70% chance)
        walls = []
        if random.random() < self.wall_probability:
            walls = self._generate_walls(solution_path)
        
        payload = {
            "size": self.size,
            "dots": dots
        }
        
        # Only add walls if we generated any
        if walls:
            payload["walls"] = walls
        
        return payload
    
    def _generate_valid_zip_puzzle(self, num_dots: int) -> Tuple[List[Dict[str, int]], List[Tuple[int, int]]]:
        """
        Generate dots that can be connected by a path that fills all 36 cells.
        
        Returns: (dots, solution_path)
        
        Strategy: Time-boxed Hamiltonian path with safe fallback
        - Tries to find interesting Hamiltonian paths (3 second limit)
        - Falls back to snake pattern if timeout
        - Best of both worlds: variety + reliability
        """
        # Try Hamiltonian with timeout
        start_time = time.time()
        timeout = 3.0  # 3 second limit
        max_attempts = 15  # Try multiple starting positions
        
        for attempt in range(max_attempts):
            # Check timeout
            if time.time() - start_time > timeout:
                print(f"   Hamiltonian timeout after {attempt} attempts, using fallback")
                break
            
            # Try to generate Hamiltonian path
            path = self._try_hamiltonian_path(start_time, timeout)
            
            if path and len(path) == 36:
                print(f"   ✅ Found Hamiltonian path on attempt {attempt + 1}")
                # Select dot positions along the path
                dot_positions = self._select_dot_positions(path, num_dots)
                
                # Convert to dot format
                dots = []
                for i, pos in enumerate(dot_positions):
                    dots.append({
                        "row": pos[0],
                        "col": pos[1],
                        "index": i + 1
                    })
                
                return dots, path
        
        # Fallback to snake pattern (guaranteed to work)
        print("   Using snake pattern fallback")
        return self._generate_snake_dots(num_dots), self._generate_snake_path()
    
    def _try_hamiltonian_path(self, start_time: float, timeout: float) -> Optional[List[Tuple[int, int]]]:
        """
        Try to find a Hamiltonian path with timeout protection.
        Uses Warnsdorff's heuristic for faster search.
        """
        # Random starting position (corners and edges work best)
        start_positions = [
            (0, 0), (0, 5), (5, 0), (5, 5),  # corners
            (0, 2), (0, 3),                   # top edge
            (5, 2), (5, 3),                   # bottom edge
            (2, 0), (3, 0),                   # left edge
            (2, 5), (3, 5)                    # right edge
        ]
        
        start_pos = random.choice(start_positions)
        path = [start_pos]
        visited = {start_pos}
        
        # Use backtracking with Warnsdorff's heuristic
        if self._hamiltonian_backtrack(path, visited, start_time, timeout):
            return path
        
        return None
    
    def _hamiltonian_backtrack(
        self, 
        path: List[Tuple[int, int]], 
        visited: Set[Tuple[int, int]],
        start_time: float,
        timeout: float
    ) -> bool:
        """
        Backtracking algorithm with Warnsdorff's heuristic and timeout.
        """
        # Check timeout
        if time.time() - start_time > timeout:
            return False
        
        # Success: visited all cells
        if len(path) == 36:
            return True
        
        current = path[-1]
        
        # Get neighbors and sort by Warnsdorff's rule
        # (visit cells with fewer unvisited neighbors first)
        neighbors = self._get_adjacent_cells(current[0], current[1])
        
        # Filter unvisited and score by accessibility
        candidates = []
        for neighbor in neighbors:
            if neighbor not in visited:
                # Count unvisited neighbors of this neighbor
                next_neighbors = self._get_adjacent_cells(neighbor[0], neighbor[1])
                accessibility = sum(1 for n in next_neighbors if n not in visited)
                candidates.append((accessibility, neighbor))
        
        # Sort by accessibility (lower is better - visit "harder" cells first)
        candidates.sort(key=lambda x: (x[0], random.random()))
        
        # Try each candidate
        for _, neighbor in candidates:
            path.append(neighbor)
            visited.add(neighbor)
            
            if self._hamiltonian_backtrack(path, visited, start_time, timeout):
                return True
            
            # Backtrack
            path.pop()
            visited.remove(neighbor)
        
        return False
    
    def _generate_winding_path(self) -> List[Tuple[int, int]]:
        """
        Generate a randomized winding path (LinkedIn Tango style).
        Creates natural, curved patterns with directional preference.
        Guaranteed to visit all 36 cells and fast to compute.
        """
        # Random starting corner/edge
        start_positions = [
            (0, 0), (0, 5), (5, 0), (5, 5),  # corners
            (0, random.randint(1, 4)),        # top edge
            (5, random.randint(1, 4)),        # bottom edge
            (random.randint(1, 4), 0),        # left edge
            (random.randint(1, 4), 5)         # right edge
        ]
        
        start = random.choice(start_positions)
        path = [start]
        visited = {start}
        
        # Directions: right, down, left, up
        directions = [(0, 1), (1, 0), (0, -1), (-1, 0)]
        current_direction = random.randint(0, 3)
        
        # Directional momentum - prefer continuing in same direction
        momentum = 0
        
        while len(path) < 36:
            current_pos = path[-1]
            found_next = False
            
            # Try directions with preference for current direction
            direction_attempts = []
            
            # High momentum: strongly prefer current direction
            if momentum > 2:
                direction_attempts = [current_direction] * 4 + [(current_direction + i) % 4 for i in [1, 3, 2]]
            # Some momentum: prefer current direction
            elif momentum > 0:
                direction_attempts = [current_direction] * 2 + [(current_direction + i) % 4 for i in [1, 3, 2]]
            # No momentum: allow more turning
            else:
                direction_attempts = [current_direction] + [(current_direction + i) % 4 for i in [1, 2, 3]]
            
            for dir_idx in direction_attempts:
                dr, dc = directions[dir_idx]
                new_pos = (current_pos[0] + dr, current_pos[1] + dc)
                
                # Check if valid and unvisited
                if (0 <= new_pos[0] < self.size and 
                    0 <= new_pos[1] < self.size and 
                    new_pos not in visited):
                    
                    path.append(new_pos)
                    visited.add(new_pos)
                    
                    # Update momentum
                    if dir_idx == current_direction:
                        momentum = min(momentum + 1, 5)  # Build momentum
                    else:
                        current_direction = dir_idx
                        momentum = 0  # Reset on turn
                    
                    found_next = True
                    break
            
            # If stuck, find any available neighbor
            if not found_next:
                for dr, dc in directions:
                    new_pos = (current_pos[0] + dr, current_pos[1] + dc)
                    if (0 <= new_pos[0] < self.size and 
                        0 <= new_pos[1] < self.size and 
                        new_pos not in visited):
                        path.append(new_pos)
                        visited.add(new_pos)
                        current_direction = directions.index((dr, dc))
                        momentum = 0
                        found_next = True
                        break
            
            # If still stuck, backtrack
            if not found_next and len(path) < 36:
                # This shouldn't happen often, but fallback to snake if needed
                if len(path) < 10:
                    return self._generate_snake_path()
                # Try to continue from a different visited cell
                break
        
        # If we didn't visit all cells, fill remaining with snake pattern
        if len(path) < 36:
            return self._generate_snake_path()
        
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
    
    def _generate_walls(self, solution_path: List[Tuple[int, int]]) -> List[Dict[str, Any]]:
        """
        Generate walls that add difficulty but don't block the solution path.
        
        Strategy:
        - Place walls on edges NOT used by the solution path
        - Random placement with 15-30% coverage of available edges
        - Creates barriers that make wrong paths more likely
        """
        # Build set of edges used by solution
        solution_edges = set()
        for i in range(len(solution_path) - 1):
            curr = solution_path[i]
            next_pos = solution_path[i + 1]
            
            # Record this edge (normalized so order doesn't matter)
            edge = self._normalize_edge(curr, next_pos)
            if edge:
                solution_edges.add(edge)
        
        # Collect all possible edges in grid
        all_edges = []
        for row in range(self.size):
            for col in range(self.size):
                # Right edge (if not at right boundary)
                if col < self.size - 1:
                    edge = ((row, col), (row, col + 1))
                    if edge not in solution_edges:
                        all_edges.append((row, col, "RIGHT"))
                
                # Bottom edge (if not at bottom boundary)
                if row < self.size - 1:
                    edge = ((row, col), (row + 1, col))
                    if edge not in solution_edges:
                        all_edges.append((row, col, "BOTTOM"))
        
        # Randomly select 15-30% of available edges to place walls
        num_walls = random.randint(int(len(all_edges) * 0.25), int(len(all_edges) * 0.45))
        selected_edges = random.sample(all_edges, min(num_walls, len(all_edges)))
        
        walls = []
        for row, col, side in selected_edges:
            walls.append({
                "row": row,
                "col": col,
                "side": side
            })
        
        print(f"   Generated {len(walls)} walls (out of {len(all_edges)} available edges)")
        return walls
    
    def _normalize_edge(self, pos1: Tuple[int, int], pos2: Tuple[int, int]) -> Optional[Tuple[Tuple[int, int], Tuple[int, int]]]:
        """
        Normalize an edge so (A, B) and (B, A) are treated as the same edge.
        Returns None if positions are not adjacent.
        """
        # Check if positions are adjacent
        row_diff = abs(pos1[0] - pos2[0])
        col_diff = abs(pos1[1] - pos2[1])
        
        if (row_diff == 1 and col_diff == 0) or (row_diff == 0 and col_diff == 1):
            # Return edge with smaller position first
            return tuple(sorted([pos1, pos2]))
        
        return None
