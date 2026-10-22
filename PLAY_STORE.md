# Publishing Between Sets on Google Play

## What you need

| Item | Notes |
|---|---|
| Google Play developer account | $25 one-time at https://play.google.com/console/signup. Google checks your ID, which can take a few days. You must do this yourself. |
| Testers (personal accounts only) | New personal accounts must run a **closed test with at least 12 testers for 14 days in a row** before they can publish to everyone. Friends and gym mates with Android phones and a Gmail address work. Organization accounts skip this but need a D-U-N-S number. |
| App bundle | `BetweenSets-vN-playstore.aab` from the latest GitHub release. |
| App icon | 512 × 512 PNG. |
| Feature graphic | 1024 × 500 PNG or JPG. |
| Screenshots | At least 2 phone screenshots (take them on your phone: the app, the widget and the lock screen). |
| Privacy policy URL | https://github.com/ngfamclam-cpu/between-sets/blob/main/PRIVACY.md |
| Short description (80 chars max) | Gym rest timer that chimes on time, even locked. Widget and auto-repeat. |
| Full description | See below. |

## Steps in Play Console

1. **Create app**: name *Between Sets*, type *App*, *Free*. Accept the declarations.
2. **App content** (left menu, *Policy → App content*):
   - Privacy policy: paste the URL above.
   - Ads: *No ads*.
   - App access: *All functionality is available without special access*.
   - Content rating: fill in the questionnaire (category *Utility*, answer No to everything). You'll get *Everyone*.
   - Target audience: 18+ (or 13+; avoid under 13, which adds Families policy rules).
   - Data safety: *No data collected or shared*. The app has no internet permission.
   - **Exact alarms**: the app uses `USE_EXACT_ALARM`. When asked, choose that the app's core function is a **timer** that must ring at an exact time. This permission is allowed for timer and alarm apps.
   - Government apps, financial features, health: No.
3. **Store listing**: descriptions, icon, feature graphic, screenshots. Category *Health & Fitness*.
4. **Testing → Closed testing**: create a track, upload the `.aab`, add your 12+ testers' emails, roll out.
   Testers opt in from the link Play gives you and install from the Play Store.
5. After 14 days, **apply for production** from the Dashboard, answer the few questions about the test, then
   upload the `.aab` to **Production** and roll out. Review usually takes a few days.

## App signing

The `.aab` is signed with this app's **upload key** (the `.p12` file kept outside the repo). On first upload,
accept **Play App Signing**: Google then holds the real signing key, and if the upload key is ever lost you
can ask Google to reset it.

Note: the APK from GitHub and the Play Store version are signed with different keys once Play App Signing is
on, so Android won't update one with the other. Uninstall the GitHub APK before installing from Play.

## Updating

Push to `main`. GitHub builds a new release with a higher version number. Upload that release's `.aab` in
Play Console as a new release on the same track.

## Full description

Between Sets is a rest timer built for the gym floor.

• One tap to start a 30 second, 1, 1.5, 2 or 3 minute rest, or save your own.
• A clear chime when the rest is over, at alarm volume, even with your phone locked in your pocket.
• Auto-repeat: the next rest starts the moment the chime plays, so you never touch your phone between sets.
• Home screen widget with a live countdown, Pause, +15s, Stop and all five presets.
• Lock-screen controls: pause, add 15 seconds or stop without unlocking.
• Set counter that ticks up every rest.
• No ads, no account, no internet access.
