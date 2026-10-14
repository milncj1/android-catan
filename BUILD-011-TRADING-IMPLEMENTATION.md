# Build 011 — Trading & Surprise Market Events
Date: 14 October 2026
Branch: `dev/build-011-trading` (branched from `dev/android-modernisation` after confirmed working Build 010).
Status: **Development in progress — source not yet compiled or phone-tested. No signed Build 011 APK.**

## Owner-approved behaviour (do not re-open agreed choices)
- Keep classical costs, scarcity, harbours, turn restrictions, building and robber rules.
- One Market Events on/off switch in New Game, remember owner's preference; balanced surprise events on one complete round, first event round 4, later starts spaced 3–5 rounds, no identical consecutive events, mostly (~80%) positive.
- Five events: Market Day (one 3:1 bank trade), Harbour Festival (harbour owners one 2:1 trade from any resource), Merchant Caravan (one 2:1 trade GIVING the announced resource), Trading Fair (AI more willing to negotiate, no cheating), Merchant Shortage (announced resource unavailable as RECEIVE side of bank/harbour exchanges only).
- Better owned harbour rates take precedence. Special event allowance consumed only if actually giving a better exchange than the owner's normal harbour rate. Shortage does NOT affect dice production, player trading or development cards.
- A single familiar Trade button opens an integrated landscape-friendly Players / Bank & Harbours interface, with scrolling, clear resource quantities and no obstruction of board/dice. Bank & Harbours finds owned ports automatically; no board-intersection tapping.
- Player trading allows mixed multi-resource offers and counteroffers, target one opponent or all; compare all responses before choosing one; no automatic selection of first acceptance. Only the active player initiates negotiations. Each accepted broadcast completes just one trade.
- CPUs proactively offer trades to the human, max one per computer turn, max three human interruptions per complete round (balanced); counteroffer once, brief CPU-to-CPU negotiations in history, fair rotation when 4–6 opponents.
- AI decisions based on OWN inventory plus PUBLIC board, received offers and trade history, not secret opponent hands. Four difficulty levels and four trading personalities. Optional advice labels Promising / Uncertain / Unlikely should not guarantee acceptance.
- Atomic engine validation of BOTH hands and consent before transfer; no free gifts or negative quantities; pending offers cancelled harmlessly on app restart; finished exchanges and market allowances are saved; full match victory halts negotiations.
- Build only one signed APK after end-to-end integration and automated checks. Do not merge to `master`. Do not alter package or permanent signing key. Never tell owner to uninstall.

## Implemented in source (011A and first part of 011B)
1. New `CatanMarketState`, optional and Serializable, copied into `CatanGameState` without changing its existing serial UID. Null means legacy classic save. Controls 5 events, round timing, eligible harbour owners and one-use allowances.
2. New `CatanTradingRules` authoritative bank/harbour/event rate calculator, checks ownership against actual Board ports, shortages and affordability, and includes a NON-EXPOSED validated two-player exchange helper. **Never expose the helper as a GameAction without recipient consent.**
3. New `CatanTradeWithAutoBankAction`, validated and applied by `CatanLocalGame`; existing explicit old bank/harbour actions still work but respect Merchant Shortage. Auto exchanges get public turn-history descriptions.
4. CPU Easy/Normal/Hard/Expert bank decisions now use the same rate calculator and new AutoBank action.
5. New `new_game_market_events` Switch in existing New Game card, preference persisted. On for this owner's preferred mode; old restored games are left untouched.
6. Existing Trade button opens a compact scrollable **Bank & Harbours** dialog; live legal rate, available quantity, event announcement and invalid-trade warning. **Players tab not built yet — do not ship partial UI.**
7. New JUnit `CatanTradingRulesTest` tests legal 4:1 bank transfers, owned general/specialist ports, non-gifting atomic player transfers, round-four timing, consumed discount surviving deep-copy save, shortages and legacy mode.

## Next development tasks, in order
1. **011B player negotiations:** secure proposal / response / confirm / cancel state machine on the game thread. No game action may debit another player's hand without a recorded acceptance. Enforce actor identity on all accepted actions, not just canMove. Expire offers when turn ends/engine changes and on restart. Counteroffers from CPUs should be computed from their own cards and public info. Broadcast results must be presented together and choice stays with human.
2. Integrate a landscape-safe **Players** tab into the same Trade panel (five resource quantities on each side; quick repeat/adjust; opponents/all; comparative responses; single confirmation). Ensure selected deals only are applied, and human turn offers don't block AI loops.
3. **011C:** proactive CPU offers (max 3 visible per round), simple personalities independent of difficulty, CPU-to-CPU trades, trade history and trade suggestions/indicators. Prevent offers to human if it is outside the active CPU turn, and never leak resource holdings.
4. **011D:** in-game Market Event announcement and persistent small Trade-button indicator, Trading Fair's actual behaviour, resumption and game-over behaviour, UI polish and extensive match simulations; error/empty-bank/shortage tests across all island and player counts.
5. Compile, run tests, build one original-key-signed APK, verify signature unchanged and owner test installation over Build 010; no master changes without approval.

## Quality gate
- Build 010 is owner's confirmed working baseline.
- **Current Build 011 source is not ready to install.** No GitHub Actions or Codex run and no APK requested/produced.
- Never claim new tests pass until a real test task is run. Existing source-only checks are not a compilation.
