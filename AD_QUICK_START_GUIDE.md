# Quick Start Guide: Testing Your New Ad Strategy

## 🚀 Quick Setup (5 Minutes)

### Step 1: Create Production Ad Units in AdMob

1. Go to https://apps.admob.com
2. Select your app "BrainBurst"
3. Click "Ad units" → "Add ad unit"
4. Create **2 new ad units**:

#### Banner Ad Unit:
- Type: **Banner**
- Format: **Adaptive**
- Size: **320x50**
- Copy the Ad Unit ID (looks like: `ca-app-pub-XXXXX/YYYYY`)

#### Native Ad Unit:
- Type: **Native**
- Format: **Advanced**
- Copy the Ad Unit ID (looks like: `ca-app-pub-XXXXX/ZZZZZ`)

---

### Step 2: Update Production Ad IDs

Open: `shared/src/androidMain/kotlin/com/brainburst/domain/ads/AdManager.android.kt`

**Find lines 28-29:**
```kotlin
private val bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111" // Test banner ID
private val nativeAdUnitId = "ca-app-pub-3940256099942544/2247696110" // Test native ID
```

**Replace with your production IDs:**
```kotlin
private val bannerAdUnitId = "ca-app-pub-XXXXX/YYYYY" // Your banner ID from Step 1
private val nativeAdUnitId = "ca-app-pub-XXXXX/ZZZZZ" // Your native ID from Step 1
```

**Save the file.**

---

### Step 3: Build and Test

```bash
cd /Users/shon.ohana/AndroidStudioProjects/BrainBurst
./gradlew :androidApp:assembleProdDebug
```

Install on device and test all ad types (see testing checklist below).

---

## ✅ Testing Checklist

### Banner Ads
- [ ] Open Sudoku game
- [ ] See banner ad at bottom of screen
- [ ] Play game - banner stays visible
- [ ] Open Zip game
- [ ] See banner ad at bottom of screen
- [ ] Rotate device - banner adapts to new size

### Native Ads
- [ ] Complete any game
- [ ] Navigate to leaderboard
- [ ] Scroll through entries
- [ ] See native ad after every 5 entries
- [ ] Native ad looks like leaderboard entry (blends in)

### Interstitial Ads (Frequency Capping)
- [ ] Complete 1 game → **No ad** (need 3 games)
- [ ] Complete 2nd game → **No ad** (need 3 games)
- [ ] Complete 3rd game → **See interstitial ad** ✅
- [ ] Complete 4th game immediately → **No ad** (5-minute cooldown)
- [ ] Wait 5 minutes, complete 3 more games → **See ad** ✅

### Rewarded Ads (Unchanged)
- [ ] During game, click "Hint" button
- [ ] See 30-second rewarded ad
- [ ] Watch full ad
- [ ] Get hint after watching
- [ ] If you skip ad → **No hint** (correct behavior)

---

## 📊 Monitor AdMob Dashboard

### Day 1-3: Initial Testing
- Check "Ad units" page
- Verify all 4 ad types showing impressions:
  - Banner (high volume)
  - Native (medium volume)
  - Interstitial (low volume - frequency capped)
  - Rewarded (low volume - user choice)

### Week 1: Performance Tracking
Track these metrics:

**Fill Rate** (Goal: >80%)
- How often AdMob has an ad to show
- Low fill rate? Contact AdMob support

**eCPM** (Revenue per 1000 views)
- Banner: $0.10-1 ✅
- Native: $3-15 ✅
- Interstitial: $2-10 ✅
- Rewarded: $10-40 ✅

**CTR** (Click-Through Rate)
- Native should be highest (2-5%)
- Banner should be 0.5-2%
- Low CTR? Ads are well-targeted ✅

### Week 2-4: Optimization
- If revenue is low → Increase native ad frequency
- If users complaining → Increase interstitial cooldown
- Compare revenue to old strategy (should be +30-50%)

---

## 🐛 Troubleshooting

### "No ads showing"
1. Using test ad IDs? → Replace with production IDs
2. AdMob account approved? → Check email/dashboard
3. App published on Play Store? → AdMob requires published app
4. Using emulator? → Test on real device

### "Only test ads showing"
- AdMob takes 24-48 hours to approve new apps
- Keep using test IDs during development
- Switch to production IDs before release

### "Low fill rate (<50%)"
- Mediation not enabled? → Enable in AdMob
- Geographic restrictions? → Check targeting
- Contact AdMob support

### "Revenue lower than expected"
- Wait 2 weeks for data stabilization
- Check eCPM by ad type (focus on highest)
- Test different placements with A/B testing
- Consider adding more rewarded ad triggers

---

## 💰 Revenue Expectations

### Conservative Estimate (1000 daily users):
- **Month 1:** $300-400/month
- **Month 3:** $500-700/month (as fill rate improves)
- **Month 6:** $700-1000/month (with optimization)

### At Scale (10,000 daily users):
- **Revenue:** $3,000-4,000/month
- **Annual:** $36,000-48,000/year

### Growth Tips:
1. **Retain users** - Better UX = More ad views
2. **Encourage hints** - Rewarded ads = Highest revenue
3. **Monitor metrics** - Optimize based on data
4. **Seasonal peaks** - Holiday eCPMs are 2-3x higher

---

## 🎯 Quick Reference

### Ad Placement Strategy
| Ad Type | Location | Frequency | eCPM | Notes |
|---------|----------|-----------|------|-------|
| **Banner** | Game bottom | Always visible | $0.10-1 | Passive income |
| **Native** | Leaderboard | Every 5 entries | $3-15 | Blends with UI |
| **Interstitial** | After game | Every 3 games + 5min | $2-10 | Frequency capped |
| **Rewarded** | Hints | User choice | $10-40 | Highest revenue |

### File Locations (for tweaking)
- **Ad IDs:** `shared/src/androidMain/kotlin/com/brainburst/domain/ads/AdManager.android.kt` (lines 28-29)
- **Frequency caps:** Same file (lines 32-34)
- **Banner UI:** `shared/src/androidMain/kotlin/com/brainburst/presentation/ads/BannerAdView.kt`
- **Native UI:** `shared/src/androidMain/kotlin/com/brainburst/presentation/ads/NativeAdCard.kt`

---

## 📞 Need Help?

### Common Adjustments

**Increase Interstitial Frequency:**
```kotlin
// Change from 3 games to 2 games
private val GAMES_BETWEEN_INTERSTITIALS = 2
```

**Increase Interstitial Cooldown:**
```kotlin
// Change from 5 minutes to 10 minutes
private val MIN_INTERSTITIAL_INTERVAL_MS = 10 * 60 * 1000L
```

**Show Native Ads More Often:**
In `LeaderboardScreen.kt`, change line 120:
```kotlin
// From every 5 entries
if (index > 0 && index % 5 == 0) {

// To every 3 entries
if (index > 0 && index % 3 == 0) {
```

---

## ✅ Launch Checklist

Before releasing to production:

- [ ] Created production banner ad unit
- [ ] Created production native ad unit
- [ ] Updated ad IDs in code
- [ ] Tested all 4 ad types on real device
- [ ] Verified frequency capping works
- [ ] Checked AdMob dashboard shows impressions
- [ ] Confirmed app version incremented
- [ ] Released to Play Store
- [ ] Monitored metrics for 2 weeks
- [ ] Optimized based on data

---

## 🎉 You're Done!

Your app now has a **professional ad monetization strategy**. 

Focus on:
1. **User retention** - Happy users = More revenue
2. **Data-driven decisions** - Monitor and optimize
3. **Continuous improvement** - A/B test placements

**Good luck! 🚀**
