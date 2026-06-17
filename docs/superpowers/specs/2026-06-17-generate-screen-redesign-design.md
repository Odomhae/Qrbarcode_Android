# Generate Screen Redesign

## Context

`GenerateFragment` (`app/src/main/java/com/odom/barcodeqr/ui/GenerateFragment.kt`) and its layout (`app/src/main/res/layout/fragment_generate.xml`) currently present everything at once: a content input, an always-expanded color/logo customization panel, a banner ad, and then the generated QR preview — all stacked in a single scroll view, with Share appearing only after generation.

This is a visual/UX redesign of this one screen. It does not touch:
- The QR bitmap generation algorithms (`generateColoredQRCodeWithBorder`, `generateQRCodeWithCircularLogoAndBorder`, etc.)
- The image/logo picker flow (gallery, camera, EXIF correction)
- The ad-counter gating logic in `AdManager`
- Any other screen (Scan, History) or the app-wide theme (`Theme.MaterialComponents.DayNight.DarkActionBar` stays as-is)

## Goals

1. Reduce what's shown by default — customization is opt-in, not always on screen.
2. Make the generated QR result prominent and immediately visible, not buried below the fold.
3. Refresh the visual style toward a Material 3 look (cards, rounded corners, elevation, cleaner typography/spacing) using the existing color palette, without migrating the app's theme.
4. Persist the user's last-used customization colors so they carry over to the next visit.

## Non-goals

- No app-wide Material 3 theme migration.
- No changes to bitmap generation, sharing, or gallery-save logic.
- No persistence of the logo/image selection — only colors are remembered.
- No new automated tests — this is a UI-only change plus a trivial SharedPreferences read/write.

## Design

### 1. Layout & Screen Flow

New top-to-bottom order in `fragment_generate.xml`, all within the existing `ScrollView`:

1. **Result card** (`MaterialCardView`), pinned near the top. Contains two mutually-exclusive child groups, only one `VISIBLE` at a time:
   - **Empty state**: icon + hint text (e.g. "내용을 입력하고 생성해보세요"), shown before any generation.
   - **Result state**: the QR `ImageView` plus an inline Share/Save action row, shown after generation.
2. **Content input** — existing `EditText`, restyled as a card/outlined field.
3. **Customize toggle row** — label ("커스터마이징") + chevron icon; tapping it expands/collapses the panel below.
4. **Customize panel** — collapsed by default. Same controls as today (foreground/background/border color pickers, add-image checkbox, gallery/camera buttons, image preview), inside a `MaterialCardView`.
5. **Generate button** — primary, full width.
6. **Banner ad** — bottom of screen, after the Generate button.

Only the result card area is intended to be visible without scrolling on a typical phone.

### 2. Color Persistence

A new `SharedPreferences` file (`"qr_customization"`, following the existing pattern used by `AdManager`) stores the three customization colors:

- Keys: `fg_color`, `bg_color`, `border_color` (stored as `Int`).
- **Write**: in each color picker's `onOk` callback, immediately persist the picked value (in addition to updating the in-memory variable and the preview swatch), so the save happens at pick time, not at Generate time.
- **Read**: in `onViewCreated`, before wiring up listeners, load saved values into `foregroundColor` / `backgroundColor` / `borderColor`, falling back to today's hardcoded defaults (black fg, white bg, black border) when nothing is saved yet. Update the three preview swatches to reflect the loaded values immediately.
- The image/logo selection and the "add image" checkbox state remain session-only (not persisted), since only colors were requested.

### 3. Visual Styling

- Result card, input, and customize panel become `MaterialCardView`s: ~16dp corner radius, 2–4dp elevation, consistent 16dp internal padding — replacing the current flat `LinearLayout` + custom background drawables (`customization_panel_background.xml`, etc.).
- Keep the existing palette as-is (`primary_blue`, `secondary_teal`, `surface_white`, `text_primary`, `text_secondary`) — the M3 feel comes from shape, elevation, and spacing, not new colors.
- Buttons: `Generate` stays a filled primary button (`btn_primary_background`). Secondary actions (color pickers, gallery/camera, Share) move to outlined/text-button styling so they read as secondary against the new card backgrounds.
- Typography: extract a shared `TextAppearance` style for section headers (e.g. "색상 선택") instead of repeating bold/18sp/color attributes inline on each `TextView`.
- Color preview swatches keep their current shape but gain a thin outline stroke so they stay legible against card backgrounds.

### 4. Interaction Behavior & States

- **Customize toggle**: tapping flips a boolean, animates the panel's visibility (`GONE` ↔ `VISIBLE`) via `TransitionManager.beginDelayedTransition` on the parent container, and rotates the chevron icon 180° to reflect state.
- **Result card states**: switching from empty-state to result-state on successful generation is a simple visibility swap between the two child groups within the card — no animation required beyond what `generateCustomizedQRCode()` already does.
- **Color pickers**: unchanged `AmbilWarnaDialog` flow, except `onOk` now also writes to `SharedPreferences` as described above.
- **Untouched flows**: image picker/camera launchers, EXIF correction, bitmap generation functions, file save/share via `FileProvider`, and the interstitial-ad counter gating in `AdManager` — none of this changes; only where results render and that colors seed from prefs.

### 5. Error Handling & Testing

- No new error paths. Existing `Toast` messages (invalid image load, camera failure, invalid URL) are unchanged.
- This is a UI/layout change plus a trivial prefs get/put — not worth unit testing in isolation. Verification is manual:
  1. Build and run; open Generate.
  2. Confirm the Customize panel starts collapsed and expands/collapses smoothly via the toggle.
  3. Confirm the result card shows the empty state initially, then the generated QR + Share/Save row after tapping Generate.
  4. Pick custom colors, navigate away (e.g. switch bottom-nav tabs) and back to Generate — confirm the picked colors are still applied to the swatches and to a newly generated code.
  5. Confirm Share and gallery-save still work from the new result card location.

## Affected files

- `app/src/main/res/layout/fragment_generate.xml` — restructure, restyle.
- `app/src/main/java/com/odom/barcodeqr/ui/GenerateFragment.kt` — toggle logic, result-state switching, color persistence read/write.
- Possibly new: a small shared `TextAppearance` style entry in `res/values/themes.xml` or a new `styles.xml`.
- `res/values/strings.xml` — any new hint/label strings needed for the empty state.
