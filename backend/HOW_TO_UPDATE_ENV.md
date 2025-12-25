# How to Update Your OpenAI API Key in .env

## Easy Method (Command Line):

1. Open Terminal
2. Run this command (replace with your actual key):

```bash
cd /Users/shon.ohana/AndroidStudioProjects/BrainBurst/backend
./update_env.sh sk-proj-YOUR_ACTUAL_KEY_HERE
```

**Example:**
```bash
./update_env.sh sk-proj-ABC123xyz789...
```

## Alternative: Manual Edit

1. Open Finder
2. Press `Cmd+Shift+G`
3. Paste: `/Users/shon.ohana/AndroidStudioProjects/BrainBurst/backend`
4. Press `Cmd+Shift+.` (to show hidden files)
5. Find `.env` file
6. Right-click → Open With → TextEdit
7. Edit the line: `OPENAI_API_KEY=sk-proj-...your-key-here`
8. Replace with your actual key
9. **Important:** Press `Cmd+S` to save
10. **Close TextEdit** (Cmd+Q)

Then verify it saved:
```bash
cat .env | grep OPENAI_API_KEY
```

You should see your key (not the placeholder).

