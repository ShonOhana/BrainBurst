# 📥 Installing Google Cloud SDK

The `brew install google-cloud-sdk` might not have completed. Here are options:

## Option 1: Install via Homebrew (Recommended)

```bash
# Install the cask version (more reliable)
brew install --cask google-cloud-sdk

# After installation, add to PATH
echo 'source "$(brew --prefix)/Caskroom/google-cloud-sdk/latest/google-cloud-sdk/path.zsh.inc"' >> ~/.zshrc
echo 'source "$(brew --prefix)/Caskroom/google-cloud-sdk/latest/google-cloud-sdk/completion.zsh.inc"' >> ~/.zshrc

# Reload shell
source ~/.zshrc
```

## Option 2: Direct Download (Most Reliable)

1. **Download the installer:**
   ```bash
   curl https://sdk.cloud.google.com | bash
   ```

2. **Restart your terminal** or run:
   ```bash
   exec -l $SHELL
   ```

3. **Initialize:**
   ```bash
   gcloud init
   ```

## Option 3: Manual Installation

1. **Download from:**
   https://cloud.google.com/sdk/docs/install

2. **Extract and run:**
   ```bash
   tar -xzf google-cloud-sdk-*.tar.gz
   ./google-cloud-sdk/install.sh
   ```

## After Installation

1. **Login:**
   ```bash
   gcloud auth login
   ```

2. **Set project:**
   ```bash
   gcloud config set project brainburst-bb78e
   ```

3. **Verify:**
   ```bash
   gcloud --version
   ```

## Then Deploy!

```bash
cd /Users/shon.ohana/AndroidStudioProjects/BrainBurst/backend
./deploy.sh
./setup_scheduler.sh
```



