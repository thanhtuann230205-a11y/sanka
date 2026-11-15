# Interface languages

The interface supports Vietnamese (`vi`), English (`en`) and Japanese (`ja`). The choice is stored in `sanka.ui.language` in localStorage and restored across pages. It also works without a visible picker, and without localStorage access.

`static/js/ui-translations.js` contains Vietnamese-source, English and Japanese triples. Later duplicate source labels override earlier ones after whitespace normalization. `home-language.js` preserves original text and attributes so language switching is reversible. Dynamic text changes are observed; `placeholder`, `title` and `aria-label` are translated too, including textarea placeholders. Input values are never rewritten.

Add changing numbers/dates as explicit patterns in `translate()` rather than as a dictionary row for a single example. Current patterns cover quiz counters and summaries, flashcard counters, writing length, score summaries, completion dates, calendar activity tooltips and leaderboard date ranges.

Keep `data-i18n-ignore` on student names, nicknames, submitted writing, teacher feedback, Japanese study text, vocabulary meanings, quiz prompts/options/explanations and actual AI conversation messages. Do not put it on UI-only labels such as answer-result badges or category tags. The chatbot greeting is UI and is translated; real conversation text remains in its original language. New chatbot requests include the chosen interface language.

## Browser regression check

From the repository root, with Chrome and Node installed:

```powershell
npm install --prefix .tools/ui-check --no-save playwright
$env:NODE_PATH = (Resolve-Path .tools/ui-check/node_modules).Path
node scripts/check-ui-language.cjs
```

The test uses local assets and mocked API responses, without real credentials or email sends. It checks every dictionary entry in EN/JA, static UI text from all HTML templates, changing card text, placeholders, accessible labels, title translation, VI/EN/JA round trips, input preservation and chatbot language. Uncovered static UI labels are written to `.tools/i18n-template-uncovered.json`. New educational content may deliberately remain untranslated; mark it explicitly instead of adding it to an exception list in the test.
