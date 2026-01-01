# 🔧 Setting Up Google Cloud SDK

After installing with `brew install google-cloud-sdk`, you need to initialize it.

## Quick Setup

### 1. Add gcloud to your PATH

Add this to your `~/.zshrc` file:

```bash
# Google Cloud SDK
if [ -f '/opt/homebrew/Caskroom/google-cloud-sdk/latest/google-cloud-sdk/path.zsh.inc' ]; then
  source '/opt/homebrew/Caskroom/google-cloud-sdk/latest/google-cloud-sdk/path.zsh.inc'
fi

# Enable shell command completion
if [ -f '/opt/homebrew/Caskroom/google-cloud-sdk/latest/google-cloud-sdk/completion.zsh.inc' ]; then
  source '/opt/homebrew/Caskroom/google-cloud-sdk/latest/google-cloud-sdk/completion.zsh.inc'
fi
```

### 2. Reload your shell

```bash
source ~/.zshrc
```

### 3. Initialize gcloud

```bash
gcloud init
```

This will:
- Ask you to login
- Let you select/create a project
- Set default region

### 4. Set your project

```bash
gcloud config set project brainburst-bb78e
```

## Alternative: Use Full Path

If you don't want to modify your shell config, you can use the full path:

```bash
/opt/homebrew/Caskroom/google-cloud-sdk/latest/google-cloud-sdk/bin/gcloud init
/opt/homebrew/Caskroom/google-cloud-sdk/latest/google-cloud-sdk/bin/gcloud config set project brainburst-bb78e
```

## Verify Installation

```bash
gcloud --version
```

You should see something like:
```
Google Cloud SDK 450.0.0
```

## Then Deploy!

Once gcloud is working:

```bash
cd /Users/shon.ohana/AndroidStudioProjects/BrainBurst/backend
./deploy.sh
./setup_scheduler.sh
```



