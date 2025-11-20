BrainBurst: Daily Puzzles

MVP Spec – Compose Multiplatform + GPT + Firebase

1. Product Overview
   1.1 App Name

Name: BrainBurst: Daily Puzzles

Tagline: One fresh challenge a day, across all your devices.

1.2 Core Idea (MVP)

Platforms: Android + iOS using Compose Multiplatform.

Game: Only Mini Sudoku for now, but:

Board size: 6×6.

Allowed values: 1–6.

Standard Sudoku rules:

Each row: 1–6 without repetition.

Each column: 1–6 without repetition.

Each block: 1–6 without repetition.

For 6×6, we will use blocks of size 2×3 (2 rows, 3 columns).

One puzzle per day, same puzzle for all users.

Each registered user can play once per day:

If they solve it, puzzle is marked completed for that user and date.

After solving, a rewarded ad is shown.

After ad is completed, user sees daily leaderboard with other users.

Authentication:

Email + password (Firebase Auth).

Google Sign-In (Gmail).

Backend:

Uses OpenAI Agents SDK to generate daily puzzles.

Stores puzzles and results in Firestore.

Monetization:

Rewarded ads after solving the puzzle.

Push notification at 9:00: Post-MVP.

1.3 Long Term Vision

Add more games: Zip, Tango, and more.

Each game:

Has its own rules and UI.

Is generated daily by GPT, using the same game infrastructure.

Architecture must make it trivial to:

Add a new game type.

Add a new GPT prompt + generator.

Plug the game into the shared daily puzzle / leaderboard / ad flow.

2. Tech & Architecture
   2.1 Client

Kotlin + Compose Multiplatform:

Shared module with:

UI (Compose).

Domain layer (Use cases, models).

Data layer (repositories, Firebase via Kotlin MPP SDK).

Targets:

androidApp – standard Android app module.

iosApp – iOS app, using ComposeUIViewController() from shared module.

Architecture pattern:

Clean-ish:

presentation: ViewModels, screen states, navigation.

domain: use cases, models, game engines.

data: repositories, Firebase adapters, local storage.

DI:

Koin, configured in shared module and wired per platform.

2.2 Firebase (Multiplatform)

Use Firebase Kotlin SDK (GitLive) in shared code for MPP:

Auth: dev.gitlive:firebase-auth

Firestore: dev.gitlive:firebase-firestore
GitHub
+1

This lets you call Firebase directly from commonMain, which works well with Compose Multiplatform.

2.3 Backend

Firebase Firestore:

Stores puzzles and results.

Firebase Auth:

Email + password.

Google Sign-In.

Cloud Functions / Cloud Run (Python):

Uses OpenAI Agents SDK to generate puzzles every day.

Writes puzzles into Firestore.

Cloud Scheduler:

Triggers generator once per day.

2.4 Game & GPT Architecture Goals

Each game is represented by:

A GameType (MINI_SUDOKU_6X6, ZIP, TANGO, ...).

A GameDefinition in shared Kotlin code (rules, validation).

A PuzzlePayload schema (per game) shared between:

GPT responses.

Firestore documents.

Client decoding.

The backend GPT agent:

Receives { gameType, date }.

Uses a game-specific prompt or tool to generate a puzzle in a generic JSON format.

Writes payload to Firestore.

Client:

Fetches generic Puzzle document.

Uses appropriate GameDefinition to interpret payload and check completion.

3. Data Model
   3.1 Firestore Collections (Generic for all games)
   games collection (optional metadata)

Not critical for MVP, but helpful for future flexibility.

Doc id: gameType (e.g. MINI_SUDOKU_6X6)

{
"gameType": "MINI_SUDOKU_6X6",
"displayName": "Mini Sudoku 6x6",
"description": "Fill a 6x6 grid with digits 1-6 with no repeats in rows, columns, or 2x3 blocks.",
"active": true
}

puzzles collection

Doc id: <gameType>_<yyyy-MM-dd>
Example: MINI_SUDOKU_6X6_2025-11-18

Generic fields:

{
"gameType": "MINI_SUDOKU_6X6",
"date": "2025-11-18",
"puzzleId": "MINI_SUDOKU_6X6_2025-11-18",
"payload": { ... },    // game-specific JSON
"createdAt": "Timestamp"
}


For Mini Sudoku 6x6, payload has schema:

"payload": {
"size": 6,
"blockRows": 2,
"blockCols": 3,
"initialBoard": [
[0, 0, 3, 0, 0, 0],
[0, 0, 0, 4, 0, 0],
[0, 2, 0, 0, 5, 0],
[0, 0, 0, 0, 0, 6],
[1, 0, 0, 0, 0, 0],
[0, 0, 4, 0, 0, 0]
],
"solutionBoard": [
[6, 4, 3, 5, 2, 1],
[2, 5, 1, 4, 6, 3],
[3, 2, 6, 1, 5, 4],
[5, 1, 2, 3, 4, 6],
[1, 6, 5, 2, 3, 4],
[4, 3, 4, 6, 1, 2]
]
}


Rules for Sudoku payload:

size must be 6.

blockRows * blockCols = 6.

initialBoard and solutionBoard are 6×6 arrays.

initialBoard values 0 or 1–6.

solutionBoard values 1–6.

solutionBoard must be a valid Sudoku solution.

results collection

Doc id: auto generated.

{
"userId": "uid",
"puzzleId": "MINI_SUDOKU_6X6_2025-11-18",
"gameType": "MINI_SUDOKU_6X6",
"date": "2025-11-18",
"durationMs": 123456,
"movesCount": 40,
"completedAt": "Timestamp"
}


Indexes:

(puzzleId ASC, durationMs ASC) for leaderboard.

(userId ASC, puzzleId ASC) to prevent duplicates.

users collection

Doc id: uid

{
"uid": "string",
"email": "string",
"displayName": "string",
"createdAt": "Timestamp",
"lastSeenAt": "Timestamp"
}

3.2 Shared Kotlin Models

In shared:commonMain:

enum class GameType {
MINI_SUDOKU_6X6,
ZIP,
TANGO
}

@Serializable
data class PuzzleDto(
val gameType: GameType,
val date: String,         // yyyy-MM-dd
val puzzleId: String,
val payload: JsonElement  // generic payload
)

@Serializable
data class ResultDto(
val userId: String,
val puzzleId: String,
val gameType: GameType,
val date: String,
val durationMs: Long,
val movesCount: Int
)


Game specific payload:

@Serializable
data class Sudoku6x6Payload(
val size: Int,
val blockRows: Int,
val blockCols: Int,
val initialBoard: List<List<Int>>,
val solutionBoard: List<List<Int>>
)

4. Game Engine Architecture
   4.1 Generic Game Definitions

In shared:commonMain, define:

interface GameDefinition<Payload : Any, PlayerState : Any> {
val type: GameType
val displayName: String
val description: String

    fun decodePayload(json: JsonElement): Payload

    fun initialState(payload: Payload): PlayerState
    fun applyMove(state: PlayerState, move: GameMove): PlayerState
    fun validateState(state: PlayerState, payload: Payload): ValidationResult
    fun isCompleted(state: PlayerState, payload: Payload): Boolean
}

sealed interface GameMove

data class ValidationResult(
val isValid: Boolean,
val invalidPositions: List<Position> = emptyList()
)

data class Position(val row: Int, val col: Int)


For Sudoku:

data class SudokuMove(val position: Position, val value: Int) : GameMove

data class SudokuState(
val board: List<List<Int>>,
val fixedCells: Set<Position>,
val startedAtMillis: Long,
val movesCount: Int
)

4.2 Sudoku GameDefinition
class Sudoku6x6Definition(
private val json: Json
) : GameDefinition<Sudoku6x6Payload, SudokuState> {

    override val type: GameType = GameType.MINI_SUDOKU_6X6
    override val displayName: String = "Mini Sudoku 6x6"
    override val description: String =
        "Fill the 6x6 grid with digits 1 to 6 with no repeats in rows, columns, or 2x3 blocks."

    override fun decodePayload(jsonElement: JsonElement): Sudoku6x6Payload {
        return json.decodeFromJsonElement(Sudoku6x6Payload.serializer(), jsonElement)
    }

    override fun initialState(payload: Sudoku6x6Payload): SudokuState {
        val fixed = mutableSetOf<Position>()
        payload.initialBoard.forEachIndexed { r, row ->
            row.forEachIndexed { c, value ->
                if (value != 0) fixed += Position(r, c)
            }
        }
        return SudokuState(
            board = payload.initialBoard,
            fixedCells = fixed,
            startedAtMillis = currentTimeMillis(),
            movesCount = 0
        )
    }

    override fun applyMove(state: SudokuState, move: GameMove): SudokuState {
        require(move is SudokuMove)

        if (move.position in state.fixedCells) return state

        val newBoard = state.board.mapIndexed { r, row ->
            row.mapIndexed { c, value ->
                if (r == move.position.row && c == move.position.col) move.value else value
            }
        }

        return state.copy(
            board = newBoard,
            movesCount = state.movesCount + 1
        )
    }

    override fun validateState(
        state: SudokuState,
        payload: Sudoku6x6Payload
    ): ValidationResult {
        val invalid = mutableListOf<Position>()

        // Check rows, cols, blocks as needed...
        // Cursor to implement isRowValid, isColValid, isBlockValid.

        return ValidationResult(
            isValid = invalid.isEmpty(),
            invalidPositions = invalid
        )
    }

    override fun isCompleted(
        state: SudokuState,
        payload: Sudoku6x6Payload
    ): Boolean {
        return state.board == payload.solutionBoard
    }
}

4.3 GameRegistry

To make future games trivial to plug in:

class GameRegistry(
private val games: List<GameDefinition<*, *>>
) {
private val byType = games.associateBy { it.type }

    @Suppress("UNCHECKED_CAST")
    fun <Payload : Any, State : Any> get(type: GameType): GameDefinition<Payload, State> {
        return byType[type] as GameDefinition<Payload, State>
    }
}


MVP: registry contains only Sudoku6x6Definition, but it is ready for ZipDefinition, TangoDefinition, etc.

5. GPT / Backend Architecture
   5.1 Generic Generator

Each day, backend receives commands like:

{
"gameType": "MINI_SUDOKU_6X6",
"date": "2025-11-18"
}


It maps gameType to a GameGenerator:

class GameGenerator(Protocol):
def generate_payload(self, date_str: str) -> dict:
...


Then:

generators = {
"MINI_SUDOKU_6X6": MiniSudoku6x6Generator(openai_client),
# future: "ZIP": ZipGenerator(...),
}


Common flow:

Look up generator for gameType.

Call generator.generate_payload(date_str).

Validate payload for that game.

Write to Firestore as puzzles/<gameType>_<date> with payload.

5.2 Sudoku Mini 6x6 Generator (concept)

Prompt idea:

You are a Sudoku puzzle generator. Create a valid 6×6 Sudoku puzzle that uses digits 1 to 6, with 2×3 blocks. The puzzle must have a unique solution. Return only JSON with fields size, blockRows, blockCols, initialBoard, solutionBoard. initialBoard must use 0 for empty cells and digits 1-6 for givens. solutionBoard must be a complete and valid Sudoku solution.

Use gpt-4.1-mini for very low cost.

Validate:

Check arrays size.

Check numbers range (0, 1–6).

Optionally run a simple Sudoku solver to ensure uniqueness (can be done later).

5.3 Costs

Same logic as before, but with slightly bigger boards:

Around a few hundred tokens per puzzle.

With cheap models, we are still around a few cents per year, negligible compared to ad revenue.

6. UX / UI
   6.1 Home Screen

Title: Today’s Puzzles.

Subtitle: One shot per game, every day.

Cards:

Mini Sudoku 6x6

Status chip:

Available (if user did not solve today).

Completed (if solved today).

Shows “Daily 6x6 Sudoku challenge.”

Zip

Card with label Coming soon.

Tango

Card with label Coming soon.

6.2 Sudoku Screen

Top bar:

Back arrow.

Title: Mini Sudoku 6x6.

Content:

Row with:

“Today’s puzzle”.

Timer mm:ss.

6x6 grid:

Thicker borders around 2×3 blocks.

Prefilled numbers are bold and not editable.

User entries are medium weight.

Validation feedback:

On invalid move, highlight cell in error color briefly.

Number pad (1–6) below grid.

Buttons:

Erase to clear current selected cell.

Submit once all cells are non-zero.

6.3 Completion Flow

User submits.

If board matches solution:

Show success dialog:

“You solved today’s puzzle in X:XX.”

Button: Watch reward and see leaderboard.

Show rewarded ad.

After completion:

Navigate to Leaderboard.

6.4 Leaderboard Screen

Top section:

“Today’s leaderboard”.

Your time + your rank.

List:

Top N players:

Position.

Name (use displayName or anonymized).

Solve time.

Note: No chat or comments, just ranking.

7. Detailed Tasks & Phases (Cursor Friendly)

Below, each task is as small and explicit as possible.
Use @task tags to assign to Cursor / Claude Sonnet 4.5.

PHASE 1 – Project & KMP Setup
// @task P1-T1: Create Compose Multiplatform project
//  - Use Android Studio KMP + Compose template.
//  - Modules: :shared, :androidApp, :iosApp.
//  - Confirm Android app runs with a simple "Hello BrainBurst" Composable.
//  - Confirm iOS app runs on simulator with same Composable.

// @task P1-T2: Configure Kotlin Multiplatform targets
//  - In :shared build.gradle.kts add targets: android(), iosArm64(), iosX64(), iosSimulatorArm64().
//  - Enable Compose Multiplatform plugin in :shared.
//  - Ensure commonMain depends on compose.runtime, compose.foundation, compose.material3.

// @task P1-T3: Setup Material 3 theme in shared module
//  - Create BrainBurstTheme composable in shared:commonMain.
//  - Define light and dark color schemes.
//  - Add typography and shapes.
//  - Wrap root UI in BrainBurstTheme.

// @task P1-T4: Wire shared UI into Android and iOS
//  - Android: In MainActivity, call setContent { AppRoot() } from shared.
//  - iOS: Implement ComposeUIViewController() entry and integrate into Swift main (Xcode).
//  - Verify both platforms show a basic "Loading..." screen.

PHASE 2 – Firebase MPP Integration (Auth + Firestore)
// @task P2-T1: Add Firebase Kotlin SDK dependencies
//  - In :shared build.gradle.kts add dev.gitlive:firebase-auth and dev.gitlive:firebase-firestore.
//  - Configure version catalog or hard-code versions as needed.
//  - Sync project successfully.

// @task P2-T2: Platform Firebase initialization (Android)
//  - Add google-services.json to :androidApp.
//  - Apply Google services plugin in androidApp build.gradle.kts.
//  - Initialize Firebase in Android Application class and pass context as needed.

// @task P2-T3: Platform Firebase initialization (iOS)
//  - Add GoogleService-Info.plist to iosApp.
//  - Initialize Firebase in iOS entrypoint (AppDelegate / main).
//  - Use Firebase Kotlin SDK recommended setup for iOS (GitLive documentation).

// @task P2-T4: Define AuthRepository interface in shared
//  - Functions: signInEmail, signUpEmail, signOut, currentUser, signInWithGoogleToken(token).
//  - Use a simple User model: uid, email, displayName.
//  - No implementation yet.

// @task P2-T5: Implement AuthRepository using Firebase Kotlin SDK (commonMain)
//  - Use Firebase.auth from GitLive library.
//  - Implement signInEmail/signUpEmail/ signOut/currentUser.
//  - For Google sign in, accept idToken string and link with Firebase.
//  - Map Firebase user to shared User data class.

// @task P2-T6: Implement Firestore access for puzzles/results in shared
//  - Create PuzzleRemoteDataSource using Firebase.firestore.
//  - Functions: getPuzzle(gameType, date), saveResult(resultDto), getResultsForPuzzle(puzzleId).
//  - Use Kotlin serialization with JsonElement for payload field.

PHASE 3 – Domain & Game Infrastructure
// @task P3-T1: Define core domain models in shared
//  - GameType enum with MINI_SUDOKU_6X6, ZIP, TANGO.
//  - PuzzleDto, ResultDto as previously specified.
//  - Sudoku6x6Payload data class.
//  - ValidationResult, Position data classes.

// @task P3-T2: Define GameMove, GameDefinition, GameRegistry
//  - Add GameMove sealed interface and SudokuMove data class.
//  - Add GameDefinition interface with Payload and State generics.
//  - Add GameRegistry storing GameDefinition by GameType.
//  - Provide simple get(type) method with unchecked cast and documentation.

// @task P3-T3: Implement Sudoku6x6Definition
//  - Implement decodePayload using Json and Sudoku6x6Payload.serializer.
//  - Implement initialState: build fixedCells from payload.initialBoard.
//  - Implement applyMove: ignore if cell is fixed, update board, increment movesCount.
//  - Implement isCompleted: compare board with solutionBoard.
//  - Leave validateState with TODO or basic implementation for now, but expose it in API.

// @task P3-T4: Add Sudoku utilities for validation
//  - Implement functions: isRowValid(board, row), isColValid(board, col), isBlockValid(board, row, col, blockRows, blockCols).
//  - Use them inside Sudoku6x6Definition.validateState to report invalid positions.
//  - Add simple unit tests in shared for these functions.

// @task P3-T5: Create PuzzleRepository interface and implementation
//  - Interface methods: getTodayPuzzle(gameType), saveLocalProgress, getLocalProgress, submitResult.
//  - Implementation uses Firestore remote data source and local storage (to be defined).
//  - For MVP, local progress can be simple in-memory or DataStore, later Room/SQLDelight.

PHASE 4 – Auth & Navigation UI
// @task P4-T1: Setup Navigation in shared (Compose)
//  - Implement a simple navigator that works cross platform (e.g. Voyager or own sealed routes).
//  - Screens: Splash, Auth, Home, Sudoku, Leaderboard.
//  - AppRoot decides which screen to show based on auth state.

// @task P4-T2: Splash screen with auth check
//  - SplashViewModel uses AuthRepository.currentUser().
//  - If user exists, navigate to Home.
//  - Else, navigate to Auth.

// @task P4-T3: Auth screen UI (email/password + Google)
//  - AuthScreen composable with:
//      - Email text field
//      - Password text field
//      - Sign In button
//      - "Create account" button
//      - "Continue with Google" button.
//  - AuthViewModel interacts with AuthRepository.
//  - On success, navigate to Home.

// @task P4-T4: Integrate platform Google sign-in flows
//  - Android: use GoogleSignInClient, get idToken, call AuthRepository.signInWithGoogleToken.
//  - iOS: use GoogleSignIn for iOS, get idToken, call same function.
//  - Ensure both platforms pass token to shared code safely.

PHASE 5 – Home Screen & Daily Gating
// @task P5-T1: Define GameStateUI sealed class
//  - Available, Completed, ComingSoon.
//  - Include information: title, subtitle, iconRes or logical identifier.

// @task P5-T2: Implement HomeViewModel
//  - For MINI_SUDOKU_6X6:
//      - Fetch today’s puzzle from PuzzleRepository.getTodayPuzzle(GameType.MINI_SUDOKU_6X6).
//      - Fetch today’s result for current user (if exists).
//      - Set game state to Available or Completed.
//  - For Zip/Tango: always ComingSoon for MVP.
//  - Expose UI state as StateFlow.

// @task P5-T3: Implement HomeScreen UI
//  - Compose list of 3 cards: Mini Sudoku, Zip, Tango.
//  - Mini Sudoku card clickable only if Available.
//  - Zip/Tango cards disabled with "Coming soon" chip.
//  - Show proper Material 3 styling: elevated cards, rounded corners, chips, icons.

PHASE 6 – Sudoku 6x6 UI & Logic
// @task P6-T1: SudokuViewModel
//  - Inputs: GameType (fixed to MINI_SUDOKU_6X6 for now).
//  - On init:
//      - Load today’s puzzle via PuzzleRepository.
//      - Decode payload with GameRegistry and Sudoku6x6Definition.
//      - Initialize SudokuState.
//  - Expose: board, selectedPosition, timer, movesCount, errors.
//  - Provide actions: onCellSelected, onNumberPressed, onErase, onSubmit.

// @task P6-T2: Sudoku board UI (6x6 grid)
//  - Composable SudokuBoard(
//        board: List<List<Int>>,
//        fixedCells: Set<Position>,
//        selected: Position?,
//        onCellClick: (Position) -> Unit
//    )
//  - Draw 6x6 cells with thicker line every 2 rows and every 3 columns.
//  - Highlight selected cell and fixed cells in different style.

// @task P6-T3: Number pad UI (1-6)
//  - Row of six buttons labelled 1..6.
//  - On click: call ViewModel.onNumberPressed(value).
//  - Add Erase button to clear selected cell.
//  - Add Submit button to trigger validation and completion logic.

// @task P6-T4: Timer implementation
//  - Store start time in SudokuState.
//  - Use coroutine in ViewModel to update elapsed time every second.
//  - Expose formatted time string to UI (mm:ss).

// @task P6-T5: Validation and completion flow
//  - On Submit:
//      - If any zero cell remains, show error.
//      - Else, compare current board vs solutionBoard.
//      - If match:
//          - Expose event: PuzzleCompleted(durationMs, movesCount).
//      - If not match:
//          - Show error message "Something is wrong, check again".

PHASE 7 – Ads & Leaderboard
// @task P7-T1: Integrate AdMob Rewarded Ads (Android)
//  - Android-only implementation for now; iOS can show placeholder or skip ads in MVP.
//  - Setup AdMob App ID and rewarded ad unit ID.
//  - Implement RewardedAdManager for Android platform.
//  - Expose expect/actual or callback-based API to shared code.

// @task P7-T2: Wire Sudoku completion to rewarded ad
//  - When SudokuViewModel emits PuzzleCompleted:
//      - Navigate to a "RewardGate" flow on Android.
//      - Trigger RewardedAdManager.showAd(onReward=...).
//      - On reward:
//          - Call PuzzleRepository.submitResult(...).
//          - Navigate to Leaderboard screen.

// @task P7-T3: Implement ResultRepository in shared
//  - Uses Firestore to create result document.
//  - Prevent duplicate result for same user + puzzleId.
//  - Provide method getResultsForPuzzleSorted(puzzleId).

// @task P7-T4: LeaderboardViewModel and screen
//  - Inputs: gameType, date.
//  - Load puzzleId and top results from Firestore (e.g. limit 50).
//  - Also load current user result and compute rank (in memory).
//  - UI: show your result, your rank, then list of top results with name and time.

PHASE 8 – Backend GPT + Firestore writer
# @task P8-T1: Setup Python environment for Cloud Functions or Cloud Run
#  - Create Python project with requirements: openai, firebase-admin, agents SDK.
#  - Initialize Firebase Admin SDK using service account.

# @task P8-T2: Implement MiniSudoku6x6Generator class
#  - Method generate_payload(date_str) -> dict as Sudoku6x6Payload schema.
#  - Uses OpenAI client and Agents SDK.
#  - Prompts model to generate valid 6x6 Sudoku with unique solution.
#  - Parses JSON from model output and returns dict.

# @task P8-T3: Validate generated Sudoku
#  - Implement Python Sudoku validator for 6x6 with 2x3 blocks.
#  - Check solutionBoard validity (rows, cols, blocks).
#  - Ensure initialBoard respects solutionBoard (non-zero entries match).
#  - Optionally check uniqueness (can be a later optimization).

# @task P8-T4: Firestore writer function
#  - HTTP endpoint or scheduled function.
#  - For given gameType and date:
#      - Generate payload via appropriate generator.
#      - Validate payload.
#      - Write to Firestore in puzzles collection as generic puzzle doc.

# @task P8-T5: Cloud Scheduler
#  - Configure daily trigger (e.g. 00:05 UTC).
#  - Hit endpoint with gameType MINI_SUDOKU_6X6 and date = today's date.

8. Time Estimates (MVP)

Rough estimates for one developer using Cursor + Claude Sonnet 4.5:

Phase 1: KMP + Compose setup: 6–8h

Phase 2: Firebase MPP integration: 8–10h

Phase 3: Domain & game infra: 8–10h

Phase 4: Auth + navigation: 8–10h

Phase 5: Home + gating: 4–6h

Phase 6: Sudoku UI + logic: 10–12h

Phase 7: Ads + leaderboard: 8–10h

Phase 8: Backend GPT + Firestore writer: 10–14h

Total MVP: ~60–80 hours depending on friction (KMP + Firebase can be tricky).

9. Extra Ideas / Twists (You can decide later)

You asked me to treat this like the most important project, so a few strategic ideas:

Streak mechanic

Track how many days in a row user solves the puzzle.

Display streak on home screen to increase retention.

Difficulty ramp

Start with easy 6x6 puzzles.

As user keeps solving, generator can increase puzzle difficulty (still one per day).

Theme / focus mode

Simple toggle: “Focus mode” hides timer until completion, for users who get anxious.

Game templates for future games

Keep all new games following the same pattern:

GameType, GameDefinition, Payload, Generator, Screen, Leaderboard.

Document this pattern clearly when you add Zip/Tango later.

Anti-cheat basics (later)

Cross check solve times with minimal plausible human time, flag weird results.

10. Short Conclusion

The spec is now fully updated for Compose Multiplatform, Mini Sudoku 6x6, and a flexible game + GPT architecture.

All future games can plug into the same structure with minimal changes.

Tasks are broken into small, clear units that Cursor / Claude can pick up and you can easily supervise