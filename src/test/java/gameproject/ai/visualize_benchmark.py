#!/usr/bin/env python3

# AI-assisted (GitHub Copilot Chat), reviewed and adapted by Kilian.
# Restructured with Claude (Anthropic): story layout, night tournament, decision section.
# If committed, disclose this assistance with:
# AI-Assisted-By: GitHub Copilot Chat
# AI-Assisted-By: Claude

"""Generate one self-contained HTML page from the AI test results.

The page tells the story in five steps:
  1. Efficiency       - how fast one decision is (output.csv, board "standard")
  2. Speed vs strength - what a faster or deeper search buys (strength.csv)
  3. Coefficients     - which evaluator weights are best (night_games.csv)
  4. Night tournament - the ladder that picked the champion (night_games.csv)
  5. Decision         - the best agent within the time limit (night_report.md)
  6. Difficulty levels - which agent the GUI uses for Easy, Medium and Advanced (strength.csv)

Every input is optional: a missing file only empties its section.
"""

from __future__ import annotations

import csv
import json
import re
from pathlib import Path


HERE = Path(__file__).resolve().parent
INPUT = HERE / "output.csv"                    # AIAgentBenchmarkTest: one row per dice roll
STRENGTH_INPUT = HERE / "strength.csv"         # AIAgentStrengthTest: one row per game
NIGHT_GAMES_INPUT = HERE / "night_games.csv"   # NightTournamentTest: one row per game
NIGHT_REPORT_INPUT = HERE / "night_report.md"  # NightTournamentTest: summary and timing table
OUTPUT = HERE / "benchmark_visualization.html"

STRENGTH_NUMERIC = ["game", "depthA", "depthB", "points", "turns", "msPerMoveA", "msPerMoveB"]
NIGHT_NUMERIC = ["game", "points", "turns", "avgMsA", "maxMsA", "avgMsB", "maxMsB"]

# Columns converted to numbers; blank cells become null (not recorded).
NUMERIC_FIELDS = [
    "depth", "probability", "legalFirstPlays", "chosenMoves", "nodeVisits", "leafEvaluations",
    "generatedTurns", "duplicateTurns", "roundCopies", "boardCopies", "evaluatorCalls",
    "cacheHits", "cacheMisses",
]
# Timing columns, converted from nanoseconds to milliseconds.
TIME_FIELDS = ["minNs", "medianNs", "averageNs", "maxNs"]

# Evaluator weights used in the night tournament (same values as NightTournamentTest),
# in BoardEvaluator order.
FEATURES = ["Distance", "On bar", "Borne off", "Made points", "Blots",
            "Safe checkers", "Home checkers", "Home made points"]
WEIGHT_SETS = {
    "equal": [0.125] * 8,
    "tuned-best": [0.224, 0.152, 0.227, 0.041, 0.034, 0.088, 0.133, 0.101],
    "tuned-tophalf": [0.22, 0.1672, 0.227, 0.025, 0.0544, 0.088, 0.1282, 0.0902],
    "tuned-new": [0.225, 0.217, 0.119, 0.057, 0.038, 0.006, 0.091, 0.247],   # OvernightExperimentsTest
    "tuned-blend": [0.175, 0.171, 0.122, 0.091, 0.0815, 0.0655, 0.108, 0.186],  # Medium in App: halfway equal/tuned-new
    "tuned-quarter": [0.15, 0.148, 0.1235, 0.108, 0.10325, 0.09525, 0.1165, 0.1555],  # 3/4 equal, 1/4 tuned-new
}


def to_number(text: str | None) -> float | None:
    if text is None or not text.strip():
        return None
    return float(text)


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open(newline="", encoding="utf-8") as file:
        return list(csv.DictReader(file))


def clean_row(row: dict[str, str]) -> dict[str, object]:
    cleaned: dict[str, object] = {key: row[key] for key in ("algorithm", "improvements", "board", "dice")}
    for field in NUMERIC_FIELDS:
        cleaned[field] = to_number(row.get(field))
    for field in TIME_FIELDS:
        value = to_number(row.get(field))
        cleaned[field.replace("Ns", "Ms")] = None if value is None else value / 1_000_000
    return cleaned


def numeric_rows(rows: list[dict[str, str]], fields: list[str]) -> list[dict[str, object]]:
    for row in rows:
        for field in fields:
            row[field] = to_number(row.get(field))
    return rows


def load_night_report() -> dict[str, object]:
    """Limit, champion and the timing table from night_report.md (written by NightTournamentTest)."""
    if not NIGHT_REPORT_INPUT.exists():
        return {}
    text = NIGHT_REPORT_INPUT.read_text(encoding="utf-8")
    limit = re.search(r"Limit ([\d.]+) s per move", text)
    champion = re.search(r"## Champion: `([^`]+)`", text)
    code = re.search(r"```java\n(.*?)\n```", text, re.S)
    timing = []
    in_table = False
    for line in text.splitlines():
        if line.startswith("## Time per decision"):
            in_table = True
        elif line.startswith("## "):
            in_table = False
        elif in_table and line.startswith("| ") and not line.startswith("| setting"):
            cells = [cell.strip() for cell in line.strip("|").split("|")]
            if len(cells) == 7:
                setting, decisions, mean, p95, high, slowest, allowed = cells
                timing.append({
                    "setting": setting, "decisions": int(decisions), "meanMs": float(mean),
                    "p95Ms": float(p95), "maxMs": float(high), "slowest": slowest,
                    "allowed": allowed == "yes", "note": allowed,
                })
    return {
        "limitMs": float(limit.group(1)) * 1000 if limit else 4000.0,
        "champion": champion.group(1) if champion else None,
        "code": code.group(1) if code else None,
        "timing": timing,
    }


def to_json(value: object) -> str:
    # Keep "</script>" inside the data from closing the script tag.
    return json.dumps(value).replace("</", "<\\/")


def build_report() -> str:
    rows = [clean_row(row) for row in read_csv(INPUT)]
    night = {
        "games": numeric_rows(read_csv(NIGHT_GAMES_INPUT), NIGHT_NUMERIC),
        "report": load_night_report(),
        "weights": WEIGHT_SETS,
        "features": FEATURES,
    }
    return (TEMPLATE
            .replace("__DATA__", to_json(rows))
            .replace("__STRENGTH__", to_json(numeric_rows(read_csv(STRENGTH_INPUT), STRENGTH_NUMERIC)))
            .replace("__NIGHT__", to_json(night))
            .replace("__ROW_COUNT__", str(len(rows))))


TEMPLATE = r"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Backgammon AI results</title>
<style>
  :root {
    color-scheme: light;
    font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
    --bg: #f4f4f2; --surface: #fcfcfb; --text: #0b0b0b; --muted: #52514e;
    --border: #dcdbd6; --grid: #e9e8e4; --soft: #f0efec;
    --s1: #2a78d6; --s2: #eb6834; --s3: #1baf7a; --gray: #8a8984;   /* blue, orange, aqua (validated) */
    --good: #0ca30c; --critical: #d03b3b; --good-text: #006300;
  }
  @media (prefers-color-scheme: dark) {
    :root:not([data-theme="light"]) {
      color-scheme: dark;
      --bg: #121211; --surface: #1a1a19; --text: #ffffff; --muted: #c3c2b7;
      --border: #383835; --grid: #2a2a28; --soft: #242422;
      --s1: #3987e5; --s2: #d95926; --s3: #199e70; --gray: #8a8984;
      --good-text: #4cc94c;
    }
  }
  :root[data-theme="dark"] {
    color-scheme: dark;
    --bg: #121211; --surface: #1a1a19; --text: #ffffff; --muted: #c3c2b7;
    --border: #383835; --grid: #2a2a28; --soft: #242422;
    --s1: #3987e5; --s2: #d95926; --s3: #199e70; --gray: #8a8984;
    --good-text: #4cc94c;
  }
  * { box-sizing: border-box; }
  [hidden] { display: none !important; }
  body { margin: 0; background: var(--bg); color: var(--text); line-height: 1.45; }
  main { max-width: 1180px; margin: 0 auto; padding: 24px 16px 64px; }
  h1 { margin: 0 0 4px; font-size: 1.6rem; }
  h2 { font-size: 1.25rem; margin: 0 0 4px; }
  h3 { font-size: 1rem; margin: 18px 0 4px; }
  .muted { color: var(--muted); }
  .note { font-size: .9rem; color: var(--muted); margin: 0 0 12px; max-width: 80ch; }
  .partial { color: var(--s2); }
  code { font-size: .88em; }
  a { color: var(--s1); }

  nav { display: flex; flex-wrap: wrap; gap: 6px; margin: 14px 0 0; }
  nav a { text-decoration: none; color: var(--text); border: 1px solid var(--border); background: var(--surface);
          border-radius: 999px; padding: 4px 12px; font-size: .88rem; }
  nav a span { color: var(--muted); margin-right: 4px; }

  section { background: var(--surface); border: 1px solid var(--border); border-radius: 10px;
            padding: 20px; margin-top: 16px; }
  .step { font-size: .78rem; text-transform: uppercase; letter-spacing: .06em; color: var(--muted); }
  .takeaway { border-left: 3px solid var(--s1); padding: 6px 12px; margin: 4px 0 14px; background: var(--soft);
              border-radius: 0 6px 6px 0; max-width: 90ch; }
  details { border-top: 1px solid var(--border); margin-top: 16px; padding-top: 12px; }
  summary { cursor: pointer; font-weight: 600; }
  details[open] > summary { margin-bottom: 10px; }

  .answer { background: var(--surface); border: 2px solid var(--s1); border-radius: 12px; padding: 18px 20px; margin-top: 16px; }
  .answer h2 { margin-bottom: 10px; }

  .toolbar { display: flex; flex-wrap: wrap; gap: 12px 20px; align-items: center; margin: 12px 0; }
  .tabs { display: inline-flex; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; background: var(--surface); }
  .tabs button { border: 0; border-right: 1px solid var(--border); background: transparent; color: inherit;
                 padding: 7px 16px; font: inherit; cursor: pointer; }
  .tabs button:last-child { border-right: 0; }
  .tabs button[aria-selected="true"] { background: var(--s1); color: #fff; }
  label { display: inline-flex; align-items: center; gap: 6px; }
  select, input[type="search"] { font: inherit; padding: 6px 8px; border: 1px solid var(--border);
                                 border-radius: 6px; background: var(--surface); color: inherit; }

  .configs { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 8px; }
  .config { display: flex; gap: 10px; border: 1px solid var(--border); border-radius: 8px; padding: 10px 12px; }
  .swatch { width: 12px; height: 12px; border-radius: 3px; margin-top: 4px; flex: none; display: inline-block; }
  .role { font-size: .72rem; text-transform: uppercase; letter-spacing: .05em; color: var(--muted); }
  .name { font-weight: 600; word-break: break-word; }
  .coverage { font-size: .8rem; color: var(--muted); }

  .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 8px; margin: 8px 0; }
  .card { border: 1px solid var(--border); border-radius: 8px; padding: 12px 14px; background: var(--surface); }
  .card .label { color: var(--muted); font-size: .85rem; }
  .card .value { font-size: 1.55rem; font-weight: 650; margin: 2px 0; font-variant-numeric: tabular-nums; }
  .card .sub { color: var(--muted); font-size: .8rem; }

  .legend { display: flex; flex-wrap: wrap; gap: 6px 16px; font-size: .85rem; margin: 8px 0; }
  .legend span { display: inline-flex; align-items: center; gap: 6px; }
  .legend i { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }
  .chart-wrap { overflow-x: auto; }
  .chart-wrap svg { display: block; width: 100%; height: auto; min-width: 640px; }
  svg text { fill: var(--text); font-size: 12px; }
  svg .axis { fill: var(--muted); }
  svg .grid { stroke: var(--grid); }
  .empty { color: var(--muted); padding: 16px 0; }

  .table-wrap { overflow-x: auto; }
  table { border-collapse: collapse; width: 100%; font-size: .86rem; font-variant-numeric: tabular-nums; }
  th, td { border-bottom: 1px solid var(--border); padding: 6px 8px; text-align: right; white-space: nowrap; }
  th:first-child, td:first-child, td.left, th.left { text-align: left; }
  th { color: var(--muted); font-weight: 600; }
  th[data-key] { cursor: pointer; }

  .badge { display: inline-flex; align-items: center; gap: 4px; font-size: .78rem; font-weight: 600;
           border-radius: 999px; padding: 1px 8px; border: 1px solid var(--border); white-space: nowrap; }
  .badge.good { color: var(--good-text); border-color: var(--good); }
  .badge.bad { color: var(--critical); border-color: var(--critical); }
  .badge.neutral { color: var(--muted); }

  /* tournament ladder */
  .ladder { list-style: none; margin: 8px 0 0; padding: 0 0 0 26px; position: relative; }
  .ladder::before { content: ""; position: absolute; left: 9px; top: 6px; bottom: 6px; width: 2px; background: var(--s1); opacity: .5; }
  .ladder > li { position: relative; margin: 0 0 10px; }
  .ladder > li::before { content: ""; position: absolute; left: -22px; top: 12px; width: 12px; height: 12px;
                         border-radius: 50%; background: var(--surface); border: 2px solid var(--s1); }
  .ladder > li.holder::before { background: var(--s1); }
  .ladder > li.final::before { background: var(--s1); width: 16px; height: 16px; left: -24px; top: 10px; }
  .ladder > li.stage { margin-top: 18px; }
  .ladder > li.stage::before { display: none; }
  .stage-title { font-size: .78rem; text-transform: uppercase; letter-spacing: .06em; color: var(--muted); font-weight: 600; }
  .holder-chip { display: inline-block; font-weight: 650; padding: 4px 0; }
  .round { border: 1px solid var(--border); border-radius: 8px; padding: 10px 12px; background: var(--surface);
           display: grid; grid-template-columns: minmax(0, 1fr) 230px; gap: 4px 16px; align-items: center; }
  .round .who { font-weight: 600; }
  .round .why { grid-column: 1 / -1; font-size: .86rem; color: var(--muted); }
  .round.won { border-color: var(--s1); }
  .vs { color: var(--muted); font-weight: 400; margin: 0 6px; }
  .codeblock { background: var(--soft); border: 1px solid var(--border); border-radius: 8px; padding: 10px 12px;
               overflow-x: auto; font-size: .85rem; }
  ul.caveats { margin: 6px 0 0; padding-left: 20px; max-width: 90ch; }
  ul.caveats li { margin: 4px 0; }
  @media (max-width: 640px) { .round { grid-template-columns: minmax(0, 1fr); } }

  /* difficulty levels */
  :root { --div-pos: #2a78d6; --div-neg: #e34948; --div-mid: #f0efec; }
  @media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) { --div-pos: #3987e5; --div-neg: #e66767; --div-mid: #383835; } }
  :root[data-theme="dark"] { --div-pos: #3987e5; --div-neg: #e66767; --div-mid: #383835; }
  .levels { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 10px; margin: 10px 0; }
  .level { border: 1px solid var(--border); border-radius: 10px; padding: 12px 14px; position: relative; }
  .level .tier { font-size: .78rem; text-transform: uppercase; letter-spacing: .06em; color: var(--muted); font-weight: 600; }
  .level .what { font-size: 1.15rem; font-weight: 650; margin: 2px 0 6px; }
  .level .facts { font-size: .86rem; color: var(--muted); }
  .level .step-up { margin-top: 8px; font-size: .86rem; }
  table.matrix td, table.matrix th { text-align: center; }
  table.matrix th.rowhead { text-align: left; }
  table.matrix td.cell { min-width: 74px; border: 2px solid var(--surface); border-radius: 6px; }
  table.matrix td.cell small { display: block; color: var(--muted); font-size: .72rem; }
  table.matrix tr.chosen th.rowhead { font-weight: 700; color: var(--text); }
</style>
</head>
<body>
<main>
  <header>
    <h1>Backgammon AI: speed, strength and the final agent</h1>
    <p class="note">Generated from <code>output.csv</code> (__ROW_COUNT__ timing rows), <code>strength.csv</code>,
      <code>night_games.csv</code> and <code>night_report.md</code>. Read top to bottom: first how the search
      got fast, then what speed buys in strength, then which weights and settings won, and finally the agent we pick.</p>
    <nav>
      <a href="#efficiency"><span>1</span>Efficiency</a>
      <a href="#tradeoff"><span>2</span>Speed vs strength</a>
      <a href="#coefficients"><span>3</span>Coefficients</a>
      <a href="#tournament"><span>4</span>Night tournament</a>
      <a href="#decision"><span>5</span>Decision</a>
      <a href="#levels"><span>6</span>Difficulty levels</a>
      <a href="#appendix"><span>+</span>All data</a>
    </nav>
  </header>

  <div class="answer" id="answer"></div>

  <!-- 1 ---------------------------------------------------------------- -->
  <section id="efficiency">
    <div class="step">Step 1</div>
    <h2>Efficiency: how long one decision takes</h2>
    <p class="note">Measured on the opening position for all 21 dice rolls (board <code>standard</code>) by
      <code>AIAgentBenchmarkTest</code>. Each depth shows its first run, the previous improvement and the current one.
      Times are weighted by how likely each roll is.</p>
    <div class="takeaway" id="eff-takeaway"></div>
    <div class="toolbar">
      <div class="tabs" id="depth-tabs" role="tablist" aria-label="Search depth"></div>
      <label><input type="checkbox" id="show-all"> <span id="show-all-text">Show all improvements</span></label>
    </div>
    <div class="configs" id="configs"></div>
    <div class="cards" id="cards"></div>

    <h3>Time per decision</h3>
    <div class="chart-wrap"><svg id="progress-chart" role="img" aria-label="Time per decision by configuration"></svg></div>

    <h3>Where the speed came from: fewer nodes or cheaper nodes</h3>
    <p class="note">Time per decision = nodes searched × cost per node. Each dot is one run, coloured by depth; both axes
      are log scales. Moving <em>left</em> = fewer nodes (dedup, move filter); moving <em>down</em> to a lower dashed line =
      each node got cheaper (apply/undo, faster turn generation).</p>
    <div class="legend" id="nodes-legend"></div>
    <div class="chart-wrap"><svg id="nodes-chart" role="img" aria-label="Time per decision against nodes per decision"></svg></div>
    <div class="table-wrap"><table id="nodes-table"></table></div>

    <details>
      <summary>Details per dice roll (time, search work, dedup, doubles)</summary>
      <h3>Time per dice roll</h3>
      <p class="note">Median of the measured runs. Log scale, so every factor of 10 is the same height.</p>
      <div class="legend" id="time-legend"></div>
      <div class="chart-wrap"><svg id="time-chart" role="img" aria-label="Median time per dice roll"></svg></div>
      <h3>Search work per dice roll</h3>
      <div class="toolbar"><label>Metric <select id="metric"></select></label></div>
      <div class="legend" id="work-legend"></div>
      <div class="chart-wrap"><svg id="work-chart" role="img" aria-label="Search metric per dice roll"></svg></div>
      <div id="dedup-block">
        <h3>Turn deduplication in the current run</h3>
        <p class="note">Unique turns kept (<code>generatedTurns</code>) and turns skipped because an earlier turn reached
          the same final board (<code>duplicateTurns</code>).</p>
        <div class="legend">
          <span><i style="background: var(--s1)"></i>Unique turns kept</span>
          <span><i style="background: var(--s2)"></i>Duplicates skipped</span>
        </div>
        <div class="chart-wrap"><svg id="dedup-chart" role="img" aria-label="Unique and skipped turns per dice roll"></svg></div>
      </div>
      <h3>Doubles vs non-doubles</h3>
      <div class="table-wrap"><table id="roll-table"></table></div>
    </details>
  </section>

  <!-- 2 ---------------------------------------------------------------- -->
  <section id="tradeoff">
    <div class="step">Step 2</div>
    <h2>Speed vs strength: what a deeper search buys</h2>
    <p class="note">Depth 2 only searches the best K turns deeper (move filter). Smaller K is faster but can cut good moves.
      Each dot is one K against full depth 1 (equal weights); the bar is the 95% range of the win rate. A setting only
      counts as stronger when the whole bar is above 50%. Games come in pairs with the same dice and swapped colours.</p>
    <div class="takeaway" id="tradeoff-takeaway"></div>
    <div class="cards" id="tradeoff-cards"></div>
    <div class="chart-wrap"><svg id="tradeoff-chart" role="img" aria-label="Win rate against depth 1 versus time per decision for each move filter"></svg></div>
    <div class="table-wrap"><table id="tradeoff-table"></table></div>
    <h3>One level deeper: depth 3</h3>
    <div class="cards" id="depth3-cards"></div>
    <div class="table-wrap"><table id="depth3-table"></table></div>
  </section>

  <!-- 3 ---------------------------------------------------------------- -->
  <section id="coefficients">
    <div class="step">Step 3</div>
    <h2>Coefficients: which evaluator weights play best</h2>
    <p class="note">The weights were tuned by the team's <code>Coefficients</code> tournament and then tested in the
      night tournament at depth 2 with K = 2. Paired games, so each weight set gets the same dice with both colours.</p>
    <div class="takeaway" id="coeff-takeaway"></div>
    <div class="chart-wrap"><svg id="coeff-chart" role="img" aria-label="Win rate of each weight set in head-to-head matches"></svg></div>
    <h3>What the weights look like</h3>
    <p class="note">Share of the evaluation each feature gets (all weights sum to 1). Equal weights give every feature 12.5%.</p>
    <div class="legend" id="weights-legend"></div>
    <div class="chart-wrap"><svg id="weights-chart" role="img" aria-label="Weights per feature for each weight set"></svg></div>
  </section>

  <!-- 4 ---------------------------------------------------------------- -->
  <section id="tournament">
    <div class="step">Step 4</div>
    <h2>Night tournament: who made it further and why</h2>
    <p class="note"><code>NightTournamentTest</code> runs a "king of the hill" ladder. First every setting must pass the
      time limit. Then challengers play the current champion, cheapest first. A challenger <strong>takes over</strong> when it
      wins more than half of the games; it is <strong>proven</strong> stronger only when the whole 95% range is above 50%.
      A match stops early once it is clear at 99%, otherwise after the game limit.</p>
    <div class="takeaway" id="tournament-takeaway"></div>
    <ol class="ladder" id="ladder"></ol>
  </section>

  <!-- 5 ---------------------------------------------------------------- -->
  <section id="decision">
    <div class="step">Step 5</div>
    <h2>Decision: the best search setting within the time limit</h2>
    <p class="note">Single-threaded time of every setting on positions from a real game, all 21 rolls each, doubles
      included. The dot is the <strong>slowest</strong> decision, which is what the limit is about; the bar shows the
      average and the tick the 95th percentile.</p>
    <div class="cards" id="decision-cards"></div>
    <div class="legend" id="timing-legend"></div>
    <div class="chart-wrap"><svg id="timing-chart" role="img" aria-label="Slowest, 95th percentile and average time per decision per setting, with the time limit"></svg></div>
    <h3>Use it in the game</h3>
    <p class="note">The night champion's search setting (depth 2, K = 10) is the Advanced level in
      <code>App.createAIPlayer</code>, now with the newer tuned weights (see <a href="#coefficients">step 3</a> and
      <a href="#levels">step 6</a>):</p>
    <pre class="codeblock" id="decision-code"></pre>
    <div id="afterfix-block" hidden>
      <h3>Re-checked after the tie-break fix</h3>
      <p class="note">The champion against the same opponents, with the same dice, once the colour tie-break fix was in
        (<code>OvernightExperimentsTest</code>).</p>
      <div class="table-wrap"><table id="afterfix-table"></table></div>
    </div>
    <h3>How sure are we?</h3>
    <ul class="caveats" id="caveats"></ul>
  </section>

  <!-- 6 ---------------------------------------------------------------- -->
  <section id="levels">
    <div class="step">Step 6</div>
    <h2>Difficulty levels: which agent the game uses for Easy, Medium and Advanced</h2>
    <p class="note">The GUI offers three levels (<code>App.createAIPlayer</code>): <strong>Easy</strong> = depth 0 with equal
      weights, <strong>Medium</strong> = depth 0 with the first tuned weights (tuned-best),
      <strong>Advanced</strong> = depth 2, K = 10 with the tuned-new weights and a 4 s limit (depth-1 answer when it runs out).
      Advanced must be the strongest agent within its time budget; each level
      must beat the one below it <strong>noticeably</strong> (at least <span id="gap-text"></span> of the games, with the whole
      95% range above 50%); Medium should sit in the middle. Candidates differ in search depth, move filter and weights.
      Matches come from <code>OvernightExperimentsTest</code> (paired games, after the tie-break fix where available).</p>
    <div class="takeaway" id="levels-takeaway"></div>
    <div class="levels" id="levels-cards"></div>
    <h3>All candidates on one strength scale</h3>
    <p class="note">A rating (Elo scale, Easy = 0) fitted to every head-to-head result with the Bradley–Terry model, so
      candidates that never met can still be compared. 70 points ≈ winning 60% of the games, the "noticeable" gap;
      200 points ≈ 76%. The chosen levels are highlighted.</p>
    <div class="chart-wrap"><svg id="levels-scale" role="img" aria-label="Rating of every candidate agent"></svg></div>
    <h3>Every candidate against every other</h3>
    <p class="note">Win rate of the <em>row</em> agent against the <em>column</em> agent. Blue = the row agent is stronger,
      red = weaker, grey = about equal; small text = games, and “pre-fix” marks games played before the tie-break fix.
      Rows are sorted from weakest to strongest (rating).</p>
    <div class="table-wrap"><table class="matrix" id="levels-matrix"></table></div>
    <h3>Use it in the game</h3>
    <p class="note">The three cases for <code>App.createAIPlayer</code>:</p>
    <pre class="codeblock" id="levels-code"></pre>
    <h3>Why these three</h3>
    <ul class="caveats" id="levels-why"></ul>
  </section>

  <!-- appendix --------------------------------------------------------- -->
  <section id="appendix">
    <div class="step">Appendix</div>
    <h2>All data</h2>
    <details>
      <summary>All strength matches (strength.csv)</summary>
      <div class="table-wrap"><table id="strength-table"></table></div>
    </details>
    <details>
      <summary>All timing rows (output.csv)</summary>
      <div class="toolbar"><input type="search" id="filter" placeholder="Filter by improvement, board or dice"></div>
      <div class="table-wrap"><table id="rows-table"></table></div>
    </details>
  </section>
</main>

<script>
const ROWS = __DATA__;
const STRENGTH = __STRENGTH__;
const NIGHT = __NIGHT__;
const EFF_BOARD = "standard";     // efficiency and trade-off use the opening position

// ---------- data model ----------
const cmpDice = (a, b) => {
  const [a1, a2] = a.split("-").map(Number), [b1, b2] = b.split("-").map(Number);
  return a1 - b1 || a2 - b2;
};
const ALL_DICE = [...new Set(ROWS.map(r => r.dice))].sort(cmpDice);
const PROB = {};
ROWS.forEach(r => { PROB[r.dice] = r.probability; });

// One configuration = board + algorithm + depth + improvement label; "order" = first CSV line (chronological).
const CONFIGS = new Map();
ROWS.forEach((r, index) => {
  const key = [r.board, r.algorithm, r.depth, r.improvements].join("|");
  if (!CONFIGS.has(key)) {
    CONFIGS.set(key, { key, board: r.board, algorithm: r.algorithm, depth: r.depth,
                       label: r.improvements, order: index, rows: new Map() });
  }
  CONFIGS.get(key).rows.set(r.dice, r);
});
const EFF_CONFIGS = [...CONFIGS.values()].filter(c => c.board === EFF_BOARD);

const METRICS = [
  ["nodeVisits", "Node visits"], ["generatedTurns", "Generated turns"],
  ["duplicateTurns", "Duplicate turns skipped"], ["leafEvaluations", "Leaf evaluations"],
  ["evaluatorCalls", "Evaluator calls"], ["roundCopies", "Round copies"],
  ["cacheHits", "Cache hits"], ["cacheMisses", "Cache misses"],
];

const state = { depth: null, showAll: false, metric: "nodeVisits", sortKey: null, sortAsc: true };

// ---------- helpers ----------
const $ = id => document.getElementById(id);
const compact = new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 });
const fmtCount = v => compact.format(v);
const pct = (p, digits = 0) => (p * 100).toFixed(digits) + "%";

function fmtMs(v) {
  if (v >= 60000) return (v / 60000).toFixed(1) + " min";
  if (v >= 1000) return (v / 1000).toFixed(v >= 10000 ? 0 : 1) + " s";
  if (v >= 10) return v.toFixed(0) + " ms";
  if (v >= 1) return v.toFixed(1) + " ms";
  return v.toFixed(2) + " ms";
}
function fmtTick(v) {
  if (v >= 1000) return (v / 1000).toLocaleString("en") + " s";
  return v.toLocaleString("en", { maximumFractionDigits: 3 }) + " ms";
}
function fmtFactor(f) {
  if (f >= 0.95 && f <= 1.05) return "about the same";
  if (f >= 1) return f.toFixed(f >= 10 ? 0 : 1) + "× faster";
  return (1 / f).toFixed(1) + "× slower";
}
function escapeHtml(text) {
  return String(text).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}
function displayName(c) {
  return c.algorithm === "ADVANCED" ? c.label : `${c.algorithm} · ${c.label}`;
}
function card(label, value, sub = "") {
  return `<div class="card"><div class="label">${label}</div><div class="value">${value}</div><div class="sub">${sub}</div></div>`;
}
function badge(kind, text) {
  const icon = kind === "good" ? "✓" : kind === "bad" ? "✕" : "·";
  return `<span class="badge ${kind}">${icon} ${escapeHtml(text)}</span>`;
}
function emptySvg(svg, message) {
  svg.setAttribute("viewBox", "0 0 1200 80");
  svg.innerHTML = `<text x="600" y="44" text-anchor="middle" class="axis">${escapeHtml(message)}</text>`;
}
function logRange(values) {
  const positive = values.filter(v => v > 0);
  if (!positive.length) return null;
  let lo = Math.floor(Math.log10(Math.min(...positive)));
  if (Math.pow(10, lo) === Math.min(...positive)) lo -= 1;
  let hi = Math.ceil(Math.log10(Math.max(...positive)));
  if (hi <= lo) hi = lo + 1;
  return { lo, hi };
}

// Probability-weighted mean of a field over the given dice (default: all rolls of c).
function weighted(c, field, diceSet) {
  let total = 0, weight = 0, n = 0;
  c.rows.forEach((row, dice) => {
    if (diceSet && !diceSet.has(dice)) return;
    const value = row[field];
    if (value === null || value === undefined) return;
    total += PROB[dice] * value;
    weight += PROB[dice];
    n++;
  });
  return n ? { value: total / weight, n } : null;
}
// How much faster b is than a, on the dice rolls both have.
function speedup(a, b, field = "medianMs") {
  const shared = new Set([...a.rows.keys()].filter(d => b.rows.has(d)));
  if (!shared.size) return null;
  const wa = weighted(a, field, shared), wb = weighted(b, field, shared);
  if (!wa || !wb || wa.n !== shared.size || wb.n !== shared.size || wb.value === 0) return null;
  return { factor: wa.value / wb.value, n: shared.size };
}

// Wilson score interval: a 95% range for the true win rate given wins out of games.
function wilson(wins, games, z = 1.96) {
  const p = wins / games, z2 = z * z;
  const center = (p + z2 / (2 * games)) / (1 + z2 / games);
  const half = z * Math.sqrt(p * (1 - p) / games + z2 / (4 * games * games)) / (1 + z2 / games);
  return { p, lo: Math.max(0, center - half), hi: Math.min(1, center + half), wins, games };
}
const ciText = ci => `${pct(ci.p, 1)} <span class="muted">(${pct(ci.lo)}–${pct(ci.hi)}, ${ci.games} games)</span>`;
function verdictKind(ci) { return ci.lo > 0.5 ? "better" : ci.hi < 0.5 ? "worse" : "unclear"; }
function gamesNeeded(p) {
  if (Math.abs(p - 0.5) < 0.005) return null;
  return Math.ceil(1.96 * 1.96 * p * (1 - p) / ((p - 0.5) ** 2));
}

// Small win-rate bar: grey track, shaded 95% range, dot = win rate, dashed line = 50%.
function ciBar(ci, color = "var(--s1)") {
  const w = 220, h = 20, x = v => 6 + v * (w - 12);
  return `<svg width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" aria-hidden="true" style="display:block">
    <rect x="${x(0)}" y="8" width="${x(1) - x(0)}" height="4" rx="2" style="fill: var(--grid)"/>
    <rect x="${x(ci.lo)}" y="4" width="${Math.max(2, x(ci.hi) - x(ci.lo))}" height="12" rx="3" style="fill: ${color}; opacity: .3"/>
    <line x1="${x(0.5)}" y1="1" x2="${x(0.5)}" y2="19" style="stroke: var(--muted)" stroke-dasharray="2 2"/>
    <circle cx="${x(ci.p)}" cy="10" r="4.5" style="fill: ${color}; stroke: var(--surface)" stroke-width="2"/>
  </svg>`;
}

// =====================================================================
// 1. EFFICIENCY
// =====================================================================
const ROLE_COLORS = { "First run": "var(--gray)", "Previous": "var(--s2)", "Current": "var(--s1)", "Only run": "var(--s1)" };
const DEPTH_COLORS = { 0: "var(--gray)", 1: "var(--s1)", 2: "var(--s2)", 3: "var(--s3)" };

function intermediateColor(i, n) {
  const lightness = n <= 1 ? 62 : 78 - (i / (n - 1)) * 20;
  return `hsl(215 25% ${lightness}%)`;
}
function depths() {
  return [...new Set(EFF_CONFIGS.map(c => c.depth))].sort((a, b) => a - b);
}
function selection() {
  const all = EFF_CONFIGS.filter(c => c.depth === state.depth).sort((a, b) => a.order - b.order);
  const advanced = all.filter(c => c.algorithm === "ADVANCED");
  const first = advanced[0] || null;
  const current = advanced[advanced.length - 1] || null;
  const previous = advanced.length >= 3 ? advanced[advanced.length - 2] : null;
  const roles = new Map();
  if (first) roles.set(first.key, current === first ? "Only run" : "First run");
  if (previous) roles.set(previous.key, "Previous");
  if (current && current !== first) roles.set(current.key, "Current");
  const visible = state.showAll ? all : all.filter(c => roles.has(c.key));
  const others = visible.filter(c => !roles.has(c.key));
  visible.forEach(c => {
    c.color = roles.has(c.key) ? ROLE_COLORS[roles.get(c.key)] : intermediateColor(others.indexOf(c), others.length);
  });
  return { all, first, previous, current, roles, visible };
}
function defaultDepth() {
  // Depth 2 (the depth the final agent uses) if it exists, else the deepest complete depth.
  const ds = depths();
  if (ds.includes(2)) return 2;
  for (let i = ds.length - 1; i >= 0; i--) {
    state.depth = ds[i];
    const { current } = selection();
    if (current && current.rows.size === ALL_DICE.length) return ds[i];
  }
  return ds[ds.length - 1];
}

// Headline: biggest speed-up per depth from its first to its latest complete run.
function renderEfficiencyTakeaway() {
  const parts = [];
  depths().forEach(d => {
    const runs = EFF_CONFIGS.filter(c => c.depth === d && c.algorithm === "ADVANCED").sort((a, b) => a.order - b.order);
    if (runs.length < 2) return;
    const s = speedup(runs[0], runs[runs.length - 1]);
    const t = weighted(runs[runs.length - 1], "medianMs");
    if (s && t) parts.push(`depth ${d}: <strong>${fmtFactor(s.factor)}</strong> than the first version (now ${fmtMs(t.value)} per decision)`);
  });
  $("eff-takeaway").innerHTML = parts.length
    ? "From the first to the latest version: " + parts.join("; ") + "."
    : "Run AIAgentBenchmarkTest to measure the search.";
}

function renderToolbar(sel) {
  const tabs = $("depth-tabs");
  tabs.innerHTML = depths().map(d =>
    `<button role="tab" data-depth="${d}" aria-selected="${d === state.depth}">Depth ${d}</button>`).join("");
  tabs.querySelectorAll("button").forEach(button => button.addEventListener("click", () => {
    state.depth = Number(button.dataset.depth);
    renderEfficiency();
  }));
  const hidden = sel.all.length - sel.all.filter(c => sel.roles.has(c.key)).length;
  $("show-all-text").textContent = hidden === 0 ? "Show all improvements"
    : state.showAll ? `Show all improvements (${hidden} intermediate shown)` : `Show all improvements (${hidden} hidden)`;
  $("show-all").disabled = hidden === 0;
}

function renderConfigs(sel) {
  $("configs").innerHTML = sel.visible.map(c => {
    const role = sel.roles.get(c.key) || (c.algorithm === "ADVANCED" ? "Intermediate" : "Other agent");
    const partial = c.rows.size < ALL_DICE.length;
    return `<div class="config"><span class="swatch" style="background:${c.color}"></span><div>
        <div class="role">${role}</div><div class="name">${escapeHtml(displayName(c))}</div>
        <div class="coverage ${partial ? "partial" : ""}">${c.rows.size}/${ALL_DICE.length} dice rolls${partial ? " · incomplete" : ""}</div>
      </div></div>`;
  }).join("");
}

function renderCards(sel) {
  const { current, previous, first } = sel;
  if (!current) { $("cards").innerHTML = ""; return; }
  const cards = [];
  const time = weighted(current, "medianMs");
  cards.push(card("Current: time per decision", fmtMs(time.value), `over ${time.n}/${ALL_DICE.length} dice rolls`));
  if (previous) {
    const s = speedup(previous, current);
    cards.push(card("Compared to previous", s ? fmtFactor(s.factor) : "n/a", s ? `than ${escapeHtml(previous.label)}` : ""));
  }
  if (first && first !== current) {
    const s = speedup(first, current);
    cards.push(card("Compared to first run", s ? fmtFactor(s.factor) : "n/a", s ? `than ${escapeHtml(first.label)}` : ""));
  }
  const nodes = weighted(current, "nodeVisits");
  if (nodes) cards.push(card("Search size", fmtCount(nodes.value), "node visits per decision"));
  const unique = weighted(current, "generatedTurns"), skipped = weighted(current, "duplicateTurns");
  if (unique && skipped && unique.value + skipped.value > 0) {
    cards.push(card("Duplicate turns skipped", pct(skipped.value / (unique.value + skipped.value)), "removed by turn dedup"));
  }
  $("cards").innerHTML = cards.join("");
}

function renderProgress(sel) {
  const svg = $("progress-chart");
  const items = sel.visible.map(c => ({ c, w: weighted(c, "medianMs") })).filter(item => item.w);
  if (!items.length) return emptySvg(svg, "No timing data for this depth");
  const max = Math.max(...items.map(item => item.w.value));
  const width = 1200, rowH = 34, top = 8, bottom = 28, labelW = 300, valueW = 320;
  const height = top + items.length * rowH + bottom;
  const x0 = labelW, x1 = width - valueW;
  const x = v => x0 + v / max * (x1 - x0);
  const parts = [];
  for (let t = 0; t <= 4; t++) {
    const v = max * t / 4;
    parts.push(`<line class="grid" x1="${x(v)}" y1="${top}" x2="${x(v)}" y2="${height - bottom}"/>`);
    parts.push(`<text class="axis" x="${x(v)}" y="${height - 8}" text-anchor="middle">${t === 0 ? "0" : fmtMs(v)}</text>`);
  }
  items.forEach(({ c, w }, i) => {
    const y = top + i * rowH;
    let detail = `${w.n}/${ALL_DICE.length} rolls`;
    if (sel.first && c !== sel.first && c.algorithm === "ADVANCED") {
      const s = speedup(sel.first, c);
      if (s) detail += ` · ${fmtFactor(s.factor)} than first`;
    }
    parts.push(`<text x="${labelW - 10}" y="${y + rowH / 2 + 4}" text-anchor="end">${escapeHtml(displayName(c))}</text>`);
    parts.push(`<rect x="${x0}" y="${y + 8}" width="${Math.max(2, x(w.value) - x0)}" height="${rowH - 16}" rx="4" style="fill:${c.color}"><title>${escapeHtml(displayName(c))}: ${fmtMs(w.value)} per decision</title></rect>`);
    parts.push(`<text x="${x(w.value) + 8}" y="${y + rowH / 2 + 4}"><tspan font-weight="600">${fmtMs(w.value)}</tspan><tspan class="axis" dx="8">${detail}</tspan></text>`);
  });
  svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
  svg.innerHTML = parts.join("");
}

function legend(target, series) {
  $(target).innerHTML = series.map(s => `<span><i style="background:${s.color}"></i>${escapeHtml(s.name)}</span>`).join("");
}

// Vertical grouped bars per dice roll on a log scale.
function groupedChart(svg, series, format, axisLabel, tickFormat) {
  const dice = ALL_DICE.filter(d => series.some(s => s.values.get(d) > 0));
  const values = series.flatMap(s => dice.map(d => s.values.get(d))).filter(v => v > 0);
  if (!dice.length) return emptySvg(svg, "No data recorded for this selection");
  const range = logRange(values);
  const width = 1200, height = 340;
  const m = { top: 16, right: 16, bottom: 40, left: 84 };
  const plotW = width - m.left - m.right, plotH = height - m.top - m.bottom;
  const y = v => m.top + plotH - (Math.log10(v) - range.lo) / (range.hi - range.lo) * plotH;
  const groupW = plotW / dice.length;
  const barW = Math.min(20, groupW * 0.8 / series.length);
  const parts = [];
  const step = range.hi - range.lo > 8 ? 2 : 1;
  for (let e = range.lo; e <= range.hi; e += step) {
    parts.push(`<line class="grid" x1="${m.left}" y1="${y(10 ** e)}" x2="${width - m.right}" y2="${y(10 ** e)}"/>`);
    parts.push(`<text class="axis" x="${m.left - 8}" y="${y(10 ** e) + 4}" text-anchor="end">${(tickFormat || format)(10 ** e)}</text>`);
  }
  dice.forEach((d, i) => {
    const center = m.left + i * groupW + groupW / 2;
    series.forEach((s, k) => {
      const v = s.values.get(d);
      if (!(v > 0)) return;
      const bx = center + (k - series.length / 2) * barW;
      parts.push(`<rect x="${bx}" y="${y(v)}" width="${Math.max(1, barW - 2)}" height="${m.top + plotH - y(v)}" style="fill:${s.color}"><title>${d} · ${escapeHtml(s.name)}: ${format(v)}</title></rect>`);
    });
    parts.push(`<text class="axis" x="${center}" y="${height - 18}" text-anchor="middle" font-weight="${d[0] === d[2] ? 700 : 400}">${d}</text>`);
  });
  parts.push(`<text class="axis" x="16" y="${m.top + plotH / 2}" transform="rotate(-90 16 ${m.top + plotH / 2})" text-anchor="middle">${axisLabel}</text>`);
  svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
  svg.innerHTML = parts.join("");
}
function seriesFor(sel, field) {
  return sel.visible.map(c => ({ name: displayName(c), color: c.color,
    values: new Map([...c.rows].map(([d, r]) => [d, r[field]])) }));
}
function renderTime(sel) {
  const series = seriesFor(sel, "medianMs");
  legend("time-legend", series);
  groupedChart($("time-chart"), series, fmtMs, "median time", fmtTick);
}
function renderWork(sel) {
  const available = METRICS.filter(([field]) => sel.visible.some(c => [...c.rows.values()].some(r => r[field] > 0)));
  const select = $("metric");
  select.innerHTML = available.map(([field, label]) => `<option value="${field}">${label}</option>`).join("");
  if (!available.length) { legend("work-legend", []); return emptySvg($("work-chart"), "No search metrics recorded for these runs"); }
  if (!available.some(([field]) => field === state.metric)) state.metric = available[0][0];
  select.value = state.metric;
  const label = available.find(([field]) => field === state.metric)[1];
  const series = seriesFor(sel, state.metric).filter(s => [...s.values.values()].some(v => v > 0));
  legend("work-legend", series);
  groupedChart($("work-chart"), series, fmtCount, label.toLowerCase());
}
function renderDedup(sel) {
  const c = sel.current;
  const rows = c ? ALL_DICE.map(d => c.rows.get(d)).filter(r => r && r.generatedTurns !== null && r.duplicateTurns !== null) : [];
  $("dedup-block").hidden = rows.length === 0;
  if (!rows.length) return;
  const svg = $("dedup-chart");
  const width = 1200, height = 320;
  const m = { top: 24, right: 16, bottom: 40, left: 84 };
  const plotW = width - m.left - m.right, plotH = height - m.top - m.bottom;
  const max = Math.max(...rows.map(r => r.generatedTurns + r.duplicateTurns), 1);
  const y = v => m.top + plotH - v / max * plotH;
  const step = plotW / rows.length, barW = Math.min(28, step * 0.6);
  const parts = [];
  for (let t = 0; t <= 4; t++) {
    const v = max * t / 4;
    parts.push(`<line class="grid" x1="${m.left}" y1="${y(v)}" x2="${width - m.right}" y2="${y(v)}"/>`);
    parts.push(`<text class="axis" x="${m.left - 8}" y="${y(v) + 4}" text-anchor="end">${fmtCount(v)}</text>`);
  }
  rows.forEach((r, i) => {
    const cx = m.left + i * step + step / 2;
    const total = r.generatedTurns + r.duplicateTurns;
    const share = total ? r.duplicateTurns / total * 100 : 0;
    parts.push(`<rect x="${cx - barW / 2}" y="${y(r.generatedTurns)}" width="${barW}" height="${m.top + plotH - y(r.generatedTurns)}" style="fill: var(--s1)"><title>${r.dice}: ${fmtCount(r.generatedTurns)} unique turns</title></rect>`);
    parts.push(`<rect x="${cx - barW / 2}" y="${y(total)}" width="${barW}" height="${Math.max(0, y(r.generatedTurns) - y(total) - 2)}" style="fill: var(--s2)"><title>${r.dice}: ${fmtCount(r.duplicateTurns)} duplicates skipped (${share.toFixed(1)}%)</title></rect>`);
    parts.push(`<text class="axis" x="${cx}" y="${y(total) - 6}" text-anchor="middle" font-size="10">${share.toFixed(0)}%</text>`);
    parts.push(`<text class="axis" x="${cx}" y="${height - 18}" text-anchor="middle">${r.dice}</text>`);
  });
  svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
  svg.innerHTML = parts.join("");
}
function renderRollTable(sel) {
  const doubles = ["1-1", "2-2", "3-3", "4-4", "5-5", "6-6"];
  const head = `<thead><tr><th>Configuration</th><th>Non-doubles (mean)</th>${doubles.map(d => `<th>${d}</th>`).join("")}</tr></thead>`;
  const body = sel.visible.map(c => {
    const nonDoubles = [...c.rows.values()].filter(r => r.dice[0] !== r.dice[2] && r.medianMs !== null);
    const mean = nonDoubles.length ? nonDoubles.reduce((s, r) => s + r.medianMs, 0) / nonDoubles.length : null;
    const cells = doubles.map(d => c.rows.has(d) ? fmtMs(c.rows.get(d).medianMs) : "–");
    return `<tr><td><span class="swatch" style="background:${c.color};margin:0 6px 0 0"></span>${escapeHtml(displayName(c))}</td>
      <td>${mean === null ? "–" : fmtMs(mean)}</td>${cells.map(v => `<td>${v}</td>`).join("")}</tr>`;
  }).join("");
  $("roll-table").innerHTML = head + `<tbody>${body}</tbody>`;
}

function nodeStats(c, restrictTo) {
  const dice = new Set([...c.rows].filter(([d, r]) => r.nodeVisits > 0 && r.medianMs !== null && (!restrictTo || restrictTo.has(d))).map(([d]) => d));
  if (!dice.size) return null;
  const nodes = weighted(c, "nodeVisits", dice).value;
  const ms = weighted(c, "medianMs", dice).value;
  return { nodes, ms, usPerNode: ms * 1000 / nodes, n: dice.size };
}
function renderNodes(sel) {
  const svg = $("nodes-chart");
  const all = EFF_CONFIGS.filter(c => c.algorithm === "ADVANCED").map(c => ({ c, s: nodeStats(c) })).filter(item => item.s);
  const ds = [...new Set(all.map(item => item.c.depth))].sort((a, b) => a - b);
  $("nodes-legend").innerHTML = ds.map(d => `<span><i style="background:${DEPTH_COLORS[d] || "var(--gray)"}"></i>Depth ${d}</span>`).join("")
    + `<span class="muted">Larger, labelled dots: the runs shown for depth ${state.depth}</span>`;
  if (!all.length) { emptySvg(svg, "No runs with node counts yet"); $("nodes-table").innerHTML = ""; return; }
  const width = 1200, height = 420;
  const m = { top: 16, right: 24, bottom: 48, left: 84 };
  const plotW = width - m.left - m.right, plotH = height - m.top - m.bottom;
  const xr = logRange(all.map(item => item.s.nodes)), yr = logRange(all.map(item => item.s.ms));
  const x = v => m.left + (Math.log10(v) - xr.lo) / (xr.hi - xr.lo) * plotW;
  const y = v => m.top + plotH - (Math.log10(v) - yr.lo) / (yr.hi - yr.lo) * plotH;
  const parts = [`<defs><clipPath id="nodes-clip"><rect x="${m.left}" y="${m.top}" width="${plotW}" height="${plotH}"/></clipPath></defs>`];
  for (let e = xr.lo; e <= xr.hi; e++) {
    parts.push(`<line class="grid" x1="${x(10 ** e)}" y1="${m.top}" x2="${x(10 ** e)}" y2="${m.top + plotH}"/>`);
    parts.push(`<text class="axis" x="${x(10 ** e)}" y="${height - 26}" text-anchor="middle">${fmtCount(10 ** e)}</text>`);
  }
  for (let e = yr.lo; e <= yr.hi; e++) {
    parts.push(`<line class="grid" x1="${m.left}" y1="${y(10 ** e)}" x2="${m.left + plotW}" y2="${y(10 ** e)}"/>`);
    parts.push(`<text class="axis" x="${m.left - 8}" y="${y(10 ** e) + 4}" text-anchor="end">${fmtTick(10 ** e)}</text>`);
  }
  [0.1, 1, 10, 100].forEach(us => {
    const n0 = 10 ** xr.lo, n1 = 10 ** xr.hi;
    parts.push(`<line clip-path="url(#nodes-clip)" x1="${x(n0)}" y1="${y(n0 * us / 1000)}" x2="${x(n1)}" y2="${y(n1 * us / 1000)}" style="stroke: var(--muted)" stroke-dasharray="4 4" opacity=".6"/>`);
    const labelNodes = Math.min(n1, Math.max(n0, 10 ** (yr.hi - 0.15) * 1000 / us));
    if (labelNodes > n0 && labelNodes <= n1) {
      parts.push(`<text class="axis" x="${x(labelNodes) - 4}" y="${y(labelNodes * us / 1000) + 14}" text-anchor="end" font-size="11">${us} µs/node</text>`);
    }
  });
  const visibleKeys = new Set(sel.visible.map(c => c.key));
  const placed = [];
  all.sort((a, b) => visibleKeys.has(a.c.key) - visibleKeys.has(b.c.key));
  all.forEach(({ c, s }) => {
    const hi = visibleKeys.has(c.key);
    parts.push(`<circle cx="${x(s.nodes)}" cy="${y(s.ms)}" r="${hi ? 7 : 4.5}" style="fill:${DEPTH_COLORS[c.depth] || "var(--gray)"}; stroke: var(--surface)" stroke-width="2" opacity="${hi ? 1 : 0.55}"><title>${escapeHtml(displayName(c))} (depth ${c.depth}): ${fmtCount(s.nodes)} nodes, ${fmtMs(s.ms)}, ${s.usPerNode.toFixed(2)} µs per node</title></circle>`);
    if (hi) {
      let ly = y(s.ms) + 4;
      while (placed.some(p => Math.abs(p.x - x(s.nodes)) < 160 && Math.abs(p.y - ly) < 13)) ly += 13;
      placed.push({ x: x(s.nodes), y: ly });
      const nearRight = x(s.nodes) > width - 220;     // keep labels inside the chart
      parts.push(`<text x="${x(s.nodes) + (nearRight ? -10 : 10)}" y="${ly}" font-size="11" text-anchor="${nearRight ? "end" : "start"}">${escapeHtml(displayName(c))}</text>`);
    }
  });
  parts.push(`<text class="axis" x="${m.left + plotW / 2}" y="${height - 6}" text-anchor="middle">nodes searched per decision</text>`);
  parts.push(`<text class="axis" x="16" y="${m.top + plotH / 2}" transform="rotate(-90 16 ${m.top + plotH / 2})" text-anchor="middle">time per decision</text>`);
  svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
  svg.innerHTML = parts.join("");

  const rows = sel.visible.map(c => ({ c, s: nodeStats(c) })).filter(item => item.s);
  const base = rows[0];
  rows.forEach(row => {
    if (!base || row === base) return;
    const shared = new Set([...row.c.rows.keys()].filter(d => base.c.rows.has(d)));
    const mine = nodeStats(row.c, shared), theirs = nodeStats(base.c, shared);
    if (mine && theirs) row.vsBase = { mine, base: theirs };
  });
  const fmtX = f => (f >= 0.95 && f <= 1.05) ? "about the same" : (f >= 1 ? f.toFixed(f >= 10 ? 0 : 1) + "× less" : (1 / f).toFixed(1) + "× more");
  $("nodes-table").innerHTML = `<thead><tr><th>Run (depth ${state.depth})</th><th>Nodes / decision</th><th>Time / decision</th>
      <th>Cost per node</th><th>Nodes vs first</th><th>Cost per node vs first</th></tr></thead><tbody>`
    + rows.map(({ c, s, vsBase }) => `<tr>
      <td><span class="swatch" style="background:${c.color};margin:0 6px 0 0"></span>${escapeHtml(displayName(c))}</td>
      <td>${fmtCount(s.nodes)}</td><td>${fmtMs(s.ms)}</td><td>${s.usPerNode.toFixed(2)} µs</td>
      <td>${vsBase ? fmtX(vsBase.base.nodes / vsBase.mine.nodes) : "–"}</td>
      <td>${vsBase ? fmtX(vsBase.base.usPerNode / vsBase.mine.usPerNode) : "–"}</td></tr>`).join("") + `</tbody>`;
}

function renderEfficiency() {
  const sel = selection();
  renderToolbar(sel);
  renderConfigs(sel);
  renderCards(sel);
  renderProgress(sel);
  renderNodes(sel);
  renderTime(sel);
  renderWork(sel);
  renderDedup(sel);
  renderRollTable(sel);
  renderRowsTable();
}

// =====================================================================
// 2. SPEED VS STRENGTH
// =====================================================================
function filterK(label) {
  const match = /move-filter-k(\d+)/.exec(label);
  return match ? Number(match[1]) : null;
}
// Tag at the end of a match name: "night" (copied from night_games.csv), "fixed" (played
// after the colour tie-break fix) or "" (older games).
function matchTag(name) {
  const m = /\((night|fixed)\)$/.exec(name);
  return m ? m[1] : "";
}
// Win rate of agent `name` against opponents at `opponentDepth` (optionally one opponent),
// counting the agent's games as A or as B, only in matches with the given tag.
function winRateAgainst(name, opponentDepth, opponentName, tag = "") {
  let wins = 0, games = 0, ms = 0;
  STRENGTH.forEach(g => {
    if (matchTag(g.match) !== tag) return;
    const asA = g.agentA === name && g.depthB === opponentDepth && (!opponentName || g.agentB === opponentName);
    const asB = g.agentB === name && g.depthA === opponentDepth && (!opponentName || g.agentA === opponentName);
    if (!asA && !asB) return;
    games++;
    if ((asA && g.winner === "A") || (asB && g.winner === "B")) wins++;
    ms += asA ? g.msPerMoveA : g.msPerMoveB;
  });
  return games ? { ...wilson(wins, games), inGameMs: ms / games } : null;
}
function verdictOf(ci, reference) {
  if (!ci) return { text: "no games yet", kind: "neutral" };
  if (ci.lo > 0.5) return { text: `stronger than ${reference}`, kind: "good" };
  if (ci.hi < 0.5) return { text: `weaker than ${reference}`, kind: "bad" };
  const need = gamesNeeded(ci.p);
  return { text: "unclear" + (need && need > ci.games ? ` (~${need} games needed)` : ""), kind: "neutral" };
}
const VERDICT_COLOR = { good: "var(--s1)", bad: "var(--s2)", neutral: "var(--gray)" };

function renderTradeoff() {
  const svg = $("tradeoff-chart");
  const depth2 = EFF_CONFIGS.filter(c => c.algorithm === "ADVANCED" && c.depth === 2).sort((a, b) => a.order - b.order);
  const depth1 = EFF_CONFIGS.filter(c => c.algorithm === "ADVANCED" && c.depth === 1).sort((a, b) => a.order - b.order);
  const depth1Ms = depth1.length ? weighted(depth1[depth1.length - 1], "medianMs").value : null;

  const byK = new Map();
  depth2.forEach(c => { const k = filterK(c.label); if (k !== null) byK.set(k, c); });
  const unfiltered = depth2.filter(c => filterK(c.label) === null && c.rows.size === ALL_DICE.length).pop();
  const entries = [...byK.entries()].map(([k, c]) => ({ k, label: `K = ${k}`, c, strengthName: `depth2-move-filter-k${k}` }));
  if (unfiltered) entries.push({ k: Infinity, label: "all turns", c: unfiltered, strengthName: "depth2-full" });
  entries.sort((a, b) => a.k - b.k);
  entries.forEach(e => {
    e.ci = winRateAgainst(e.strengthName, 1);
    e.fixed = winRateAgainst(e.strengthName, 1, null, "fixed");
    e.ms = weighted(e.c, "medianMs").value;
    e.complete = e.c.rows.size === ALL_DICE.length;
    e.nodes = weighted(e.c, "nodeVisits");
    e.verdict = verdictOf(e.ci, "depth 1");
  });
  if (!entries.length) {
    $("tradeoff-cards").innerHTML = ""; $("tradeoff-table").innerHTML = ""; $("tradeoff-takeaway").textContent = "";
    return emptySvg(svg, "No depth-2 move-filter runs yet");
  }

  // Pooled evidence over every K that played depth 1 (all depth-2 move-filter settings together).
  let pooledWins = 0, pooledGames = 0;
  entries.filter(e => e.ci && e.k !== Infinity).forEach(e => { pooledWins += e.ci.wins; pooledGames += e.ci.games; });
  const pooled = pooledGames ? wilson(pooledWins, pooledGames) : null;
  const proven = entries.filter(e => e.verdict.kind === "good").sort((a, b) => a.ms - b.ms);
  const fastest = entries.filter(e => e.k !== Infinity).sort((a, b) => a.ms - b.ms)[0];
  const full = entries.find(e => e.k === Infinity);
  $("tradeoff-takeaway").innerHTML = pooled
    ? `Depth 2 with a move filter beats depth 1: <strong>${pct(pooled.p, 1)}</strong> of ${pooled.games} games
       (95% range ${pct(pooled.lo, 1)}–${pct(pooled.hi, 1)}, all K together). The differences <em>between</em> the K values
       are within the noise, so the cheapest K already gets most of the gain.`
    : "No strength games against depth 1 yet.";

  const cards = [];
  if (pooled) cards.push(card("Depth 2 (filter) vs depth 1", pct(pooled.p, 1), `${pooled.games} games, 95% range ${pct(pooled.lo)}–${pct(pooled.hi)}`));
  if (proven.length) cards.push(card("Cheapest proven setting", proven[0].label, `${fmtMs(proven[0].ms)} per decision${depth1Ms ? `, ${(proven[0].ms / depth1Ms).toFixed(1)}× depth 1` : ""}`));
  if (full && fastest) cards.push(card("Speed gained by the filter", (full.ms / fastest.ms).toFixed(0) + "× faster", `${fastest.label} vs all turns (${fmtMs(full.ms)})`));
  $("tradeoff-cards").innerHTML = cards.join("");

  const width = 1200, height = 440;
  const m = { top: 20, right: 30, bottom: 88, left: 70 };
  const plotW = width - m.left - m.right, plotH = height - m.top - m.bottom;
  const xr = logRange(entries.map(e => e.ms).concat(depth1Ms ? [depth1Ms] : []));
  const x = v => m.left + (Math.log10(v) - xr.lo) / (xr.hi - xr.lo) * plotW;
  const cis = entries.filter(e => e.ci).flatMap(e => [e.ci.lo, e.ci.hi]);
  const yLo = Math.max(0, Math.floor((Math.min(0.4, ...cis) - 0.05) * 10) / 10);
  const yHi = Math.min(1, Math.ceil((Math.max(0.6, ...cis) + 0.05) * 10) / 10);
  const railY = m.top + plotH + 24;
  const y = p => m.top + plotH - (p - yLo) / (yHi - yLo) * plotH;
  const parts = [];
  for (let e = xr.lo; e <= xr.hi; e++) {
    parts.push(`<line class="grid" x1="${x(10 ** e)}" y1="${m.top}" x2="${x(10 ** e)}" y2="${m.top + plotH}"/>`);
    parts.push(`<text class="axis" x="${x(10 ** e)}" y="${railY + 30}" text-anchor="middle">${fmtTick(10 ** e)}</text>`);
  }
  for (let p = yLo; p <= yHi + 1e-9; p += 0.1) {
    parts.push(`<line class="grid" x1="${m.left}" y1="${y(p)}" x2="${m.left + plotW}" y2="${y(p)}"/>`);
    parts.push(`<text class="axis" x="${m.left - 8}" y="${y(p) + 4}" text-anchor="end">${Math.round(p * 100)}%</text>`);
  }
  if (pooled) {
    parts.push(`<rect x="${m.left}" y="${y(pooled.hi)}" width="${plotW}" height="${y(pooled.lo) - y(pooled.hi)}" style="fill: var(--s1)" opacity=".08"/>`);
    parts.push(`<text class="axis" x="${m.left + plotW - 6}" y="${y(pooled.hi) - 6}" text-anchor="end" font-size="11">all K pooled: ${pct(pooled.p, 1)} (band = 95% range)</text>`);
  }
  parts.push(`<line x1="${m.left}" y1="${y(0.5)}" x2="${m.left + plotW}" y2="${y(0.5)}" style="stroke: var(--text)" stroke-dasharray="6 4"/>`);
  parts.push(`<text x="${m.left + 8}" y="${y(0.5) - 8}" font-size="12">above: stronger than depth 1</text>`);
  if (depth1Ms) {
    parts.push(`<line x1="${x(depth1Ms)}" y1="${m.top}" x2="${x(depth1Ms)}" y2="${m.top + plotH}" style="stroke: var(--muted)" stroke-dasharray="2 4"/>`);
    parts.push(`<text class="axis" x="${x(depth1Ms) + 6}" y="${m.top + 14}" font-size="12">depth 1 (${fmtMs(depth1Ms)})</text>`);
  }
  parts.push(`<text class="axis" x="${m.left - 8}" y="${railY + 4}" text-anchor="end" font-size="11">no games</text>`);
  entries.forEach(e => {
    const cx = x(e.ms);
    if (e.ci) {
      const color = VERDICT_COLOR[e.verdict.kind];
      parts.push(`<line x1="${cx}" y1="${y(e.ci.lo)}" x2="${cx}" y2="${y(e.ci.hi)}" style="stroke: ${color}" stroke-width="3" stroke-linecap="round" opacity=".55"/>`);
      parts.push(`<circle cx="${cx}" cy="${y(e.ci.p)}" r="7" style="fill: ${color}; stroke: var(--surface)" stroke-width="2"><title>${e.label}: ${pct(e.ci.p, 1)} vs depth 1 over ${e.ci.games} games (95% range ${pct(e.ci.lo)}–${pct(e.ci.hi)}), ${fmtMs(e.ms)} per decision</title></circle>`);
      parts.push(`<text x="${cx + 10}" y="${y(e.ci.p) - 8}" font-size="12" font-weight="600">${e.label}</text>`);
    } else {
      parts.push(`<circle cx="${cx}" cy="${railY}" r="6" fill="none" style="stroke: var(--muted)" stroke-width="2"><title>${e.label}: ${fmtMs(e.ms)} per decision, no games against depth 1</title></circle>`);
      parts.push(`<text class="axis" x="${cx}" y="${railY - 10}" text-anchor="middle" font-size="11">${e.label}</text>`);
    }
  });
  parts.push(`<text class="axis" x="${m.left + plotW / 2}" y="${height - 6}" text-anchor="middle">time per decision on the opening position (log scale), faster on the left</text>`);
  parts.push(`<text class="axis" x="16" y="${m.top + plotH / 2}" transform="rotate(-90 16 ${m.top + plotH / 2})" text-anchor="middle">win rate vs depth 1</text>`);
  svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
  svg.innerHTML = parts.join("");

  const anyFixed = entries.some(e => e.fixed);
  $("tradeoff-table").innerHTML = `<thead><tr><th>Move filter</th><th>Time / decision</th><th>vs depth 1 time</th>
      <th>Nodes / decision</th><th>Win rate vs depth 1</th><th class="left">Verdict</th>${anyFixed ? "<th>After tie-break fix</th>" : ""}</tr></thead><tbody>`
    + entries.map(e => `<tr><td>${e.label}</td>
      <td>${fmtMs(e.ms)}${e.complete ? "" : ` <span class="partial">(${e.c.rows.size}/${ALL_DICE.length})</span>`}</td>
      <td>${depth1Ms ? (e.ms / depth1Ms).toFixed(1) + "×" : "–"}</td>
      <td>${e.nodes ? fmtCount(e.nodes.value) : "–"}</td>
      <td>${e.ci ? ciText(e.ci) : "–"}</td>
      <td class="left">${badge(e.verdict.kind, e.verdict.text)}</td>${anyFixed ? `<td>${e.fixed ? ciText(e.fixed) : "–"}</td>` : ""}</tr>`).join("") + `</tbody>`;
}

const DEPTH3_REFERENCE = "depth2-move-filter-k5";
function renderDepth3() {
  const reference = EFF_CONFIGS.filter(c => c.algorithm === "ADVANCED" && c.depth === 2 && c.label === DEPTH3_REFERENCE).pop();
  const referenceMs = reference ? weighted(reference, "medianMs").value : null;
  const depth3 = EFF_CONFIGS.filter(c => c.algorithm === "ADVANCED" && c.depth === 3)
    .sort((a, b) => (filterK(a.label) ?? Infinity) - (filterK(b.label) ?? Infinity));
  // Night tournament: depth 3 against the night champion (tuned weights).
  const nightDepth3 = nightMatches().filter(mt => parseAgent(mt.a).depth === 3);
  if (!depth3.length && !nightDepth3.length) {
    $("depth3-cards").innerHTML = "";
    $("depth3-table").innerHTML = `<tbody><tr><td class="muted">No depth-3 runs yet.</td></tr></tbody>`;
    return;
  }
  const items = depth3.map(c => {
    const k = filterK(c.label);
    const ci = winRateAgainst(c.label, 2, DEPTH3_REFERENCE);
    return { opponent: "depth 2, K = 5 (equal weights)", label: k === null ? "all turns" : `K = ${k}`,
             time: weighted(c, "medianMs"), ci, verdict: verdictOf(ci, "depth 2"), c };
  });
  nightDepth3.forEach(mt => items.push({ opponent: `${prettyAgent(mt.b)} (night)`, label: `K = ${parseAgent(mt.a).k} (tuned)`,
    time: null, ci: mt.ci, verdict: verdictOf(mt.ci, "depth 2"), c: null }));
  const played = items.filter(item => item.ci);
  let wins = 0, games = 0;
  played.forEach(item => { wins += item.ci.wins; games += item.ci.games; });
  const cards = [];
  if (games) {
    const all = wilson(wins, games);
    cards.push(card("Depth 3 vs depth 2, all games", pct(all.p, 1), `${games} games, 95% range ${pct(all.lo)}–${pct(all.hi)}: ${verdictOf(all, "depth 2").text}`));
  }
  const fastest = items.filter(item => item.time).sort((a, b) => a.time.value - b.time.value)[0];
  if (fastest && referenceMs) cards.push(card("Cost of one level deeper", `${(fastest.time.value / referenceMs).toFixed(0)}× slower`, `depth 3 ${fastest.label} vs depth 2 K = 5`));
  $("depth3-cards").innerHTML = cards.join("");
  $("depth3-table").innerHTML = `<thead><tr><th>Depth 3 setting</th><th class="left">Opponent</th><th>Time / decision</th>
      <th>Win rate of depth 3</th><th class="left">Verdict</th></tr></thead><tbody>`
    + items.map(item => `<tr><td>${escapeHtml(item.label)}</td><td class="left">${escapeHtml(item.opponent)}</td>
      <td>${item.time ? fmtMs(item.time.value) : "–"}</td><td>${item.ci ? ciText(item.ci) : "–"}</td>
      <td class="left">${badge(item.verdict.kind, item.verdict.text)}</td></tr>`).join("") + `</tbody>`;
}

// =====================================================================
// NIGHT TOURNAMENT DATA
// =====================================================================
// "tuned-best/d2-k10" -> { weights: "tuned-best", depth: 2, k: 10 }   (k null = all turns)
function parseAgent(name) {
  const [weights, setting] = name.split("/");
  const match = /^d(\d+)-(?:k(\d+)|full)$/.exec(setting || "") || [];
  return { weights, depth: Number(match[1]), k: match[2] ? Number(match[2]) : null, setting };
}
function prettySetting(name) {
  const a = parseAgent(name);
  return `depth ${a.depth}${a.k === null ? ", all turns" : `, K ${a.k}`}`;
}
function prettyAgent(name) {
  return `${parseAgent(name).weights} · ${prettySetting(name)}`;
}

let NIGHT_MATCHES = null;
function nightMatches() {
  if (NIGHT_MATCHES) return NIGHT_MATCHES;
  const byMatch = new Map();      // insertion order = order the matches were played
  NIGHT.games.forEach(g => {
    if (!byMatch.has(g.match)) byMatch.set(g.match, { name: g.match, a: g.agentA, b: g.agentB, games: [] });
    byMatch.get(g.match).games.push(g);
  });
  NIGHT_MATCHES = [...byMatch.values()].map(mt => {
    const wins = mt.games.filter(g => g.winner === "A").length;
    const net = mt.games.reduce((s, g) => s + (g.winner === "A" ? g.points : -g.points), 0) / mt.games.length;
    return { ...mt, ci: wilson(wins, mt.games.length), net,
             maxMsA: Math.max(...mt.games.map(g => g.maxMsA)), maxMsB: Math.max(...mt.games.map(g => g.maxMsB)) };
  });
  return NIGHT_MATCHES;
}

// Replays the ladder rule of NightTournamentTest: a challenger (A) that plays the current
// champion (B) takes over when it wins more than half; all other matches are final evidence.
function nightLadder() {
  const matches = nightMatches();
  if (!matches.length) return null;
  let champion = matches[0].b;
  const start = champion, rounds = [], evidence = [];
  matches.forEach(mt => {
    if (mt.b === champion && mt.a !== champion) {
      const takesOver = mt.ci.p > 0.5;
      const kind = parseAgent(mt.a).weights !== parseAgent(mt.b).weights ? "coefficients" : "setting";
      rounds.push({ ...mt, holder: champion, takesOver, kind });
      if (takesOver) champion = mt.a;
    } else {
      evidence.push(mt);
    }
  });
  return { start, rounds, evidence, champion };
}

// =====================================================================
// 3. COEFFICIENTS
// =====================================================================
const WEIGHT_COLORS = { "equal": "var(--gray)", "tuned-best": "var(--s1)", "tuned-tophalf": "var(--s2)" };

// "tuned-best-depth2-move-filter-k5" -> { weights: "tuned-best", depth: 2, k: 5 }; plain names are equal weights.
function parseStrengthName(name) {
  const m = /^(?:(.+)-)?depth(\d+)-(?:full|move-filter-k(\d+))$/.exec(name);
  return m ? { weights: m[1] || "equal", depth: Number(m[2]), k: m[3] ? Number(m[3]) : null } : null;
}
// Weight comparisons in strength.csv (e.g. from OvernightExperimentsTest): same setting, different weights.
function strengthCoefficientMatches() {
  const matches = new Map();
  STRENGTH.forEach(g => {
    if (matchTag(g.match) === "night") return;
    const a = parseStrengthName(g.agentA), b = parseStrengthName(g.agentB);
    if (!a || !b || a.weights === b.weights || a.depth !== b.depth || a.k !== b.k) return;
    if (!matches.has(g.match)) matches.set(g.match, { a: `${a.weights}/d${a.depth}-${a.k === null ? "full" : "k" + a.k}`,
      b: `${b.weights}/d${b.depth}-${b.k === null ? "full" : "k" + b.k}`, tag: matchTag(g.match), wins: 0, games: 0 });
    const mt = matches.get(g.match);
    mt.games++;
    if (g.winner === "A") mt.wins++;
  });
  return [...matches.values()].map(mt => ({ ...mt, ci: wilson(mt.wins, mt.games) }));
}

function renderCoefficients() {
  const ladder = nightLadder();
  const svg = $("coeff-chart");
  const rounds = (ladder ? ladder.rounds.filter(r => r.kind === "coefficients") : [])
    .concat(strengthCoefficientMatches());
  if (!rounds.length) {
    $("coeff-takeaway").textContent = "Run NightTournamentTest to compare weight sets.";
    emptySvg(svg, "No coefficient matches yet");
  } else {
    const best = ladder.rounds.reduce((holder, r) => r.kind === "coefficients" && r.takesOver ? r.a : holder, ladder.start);
    const first = rounds[0];
    const extra = rounds.filter(r => r.tag !== undefined).map(r => `${escapeHtml(parseAgent(r.a).weights)} vs ${escapeHtml(parseAgent(r.b).weights)}: ${pct(r.ci.p, 1)} (${verdictOf(r.ci, parseAgent(r.b).weights).text})`);
    $("coeff-takeaway").innerHTML = `<strong>${escapeHtml(parseAgent(best).weights)}</strong> are the best weights tested.
      Against equal weights they won <strong>${pct(first.ci.p)}</strong> of ${first.ci.games} games
      (95% range ${pct(first.ci.lo)}–${pct(first.ci.hi)}): the biggest single improvement of the whole project.
      The other tuned set was not better.${extra.length ? ` Later tests: ${extra.join("; ")}.` : ""}`;
    // Forest plot: one row per match, win rate of the challenger with its 95% range.
    const rowH = 54, width = 1200, labelW = 430, m = { top: 26, right: 40, bottom: 34 };
    const height = m.top + rounds.length * rowH + m.bottom;
    const lo = Math.min(0.3, Math.floor(Math.min(...rounds.map(r => r.ci.lo)) * 10) / 10);
    const hi = Math.max(0.9, Math.ceil(Math.max(...rounds.map(r => r.ci.hi)) * 10) / 10);
    const x = p => labelW + (p - lo) / (hi - lo) * (width - labelW - m.right);
    const parts = [];
    for (let p = lo; p <= hi + 1e-9; p += 0.1) {
      parts.push(`<line class="grid" x1="${x(p)}" y1="${m.top - 6}" x2="${x(p)}" y2="${height - m.bottom}"/>`);
      parts.push(`<text class="axis" x="${x(p)}" y="${height - 12}" text-anchor="middle">${Math.round(p * 100)}%</text>`);
    }
    parts.push(`<line x1="${x(0.5)}" y1="${m.top - 10}" x2="${x(0.5)}" y2="${height - m.bottom}" style="stroke: var(--text)" stroke-dasharray="6 4"/>`);
    parts.push(`<text class="axis" x="${x(0.5) + 6}" y="${m.top - 12}" font-size="11">50% = equally strong</text>`);
    rounds.forEach((r, i) => {
      const cy = m.top + i * rowH + rowH / 2;
      const a = parseAgent(r.a).weights, b = parseAgent(r.b).weights;
      const color = WEIGHT_COLORS[a] || "var(--s3)";
      parts.push(`<text x="12" y="${cy - 4}" font-weight="600">${escapeHtml(a)} <tspan class="axis" font-weight="400">vs</tspan> ${escapeHtml(b)}</text>`);
      parts.push(`<text class="axis" x="12" y="${cy + 13}" font-size="11">${prettySetting(r.a)} · ${r.ci.games} games · ${verdictOf(r.ci, b).text}${r.tag ? ` · after tie-break fix` : ""}</text>`);
      parts.push(`<line x1="${x(r.ci.lo)}" y1="${cy}" x2="${x(r.ci.hi)}" y2="${cy}" style="stroke: ${color}" stroke-width="6" stroke-linecap="round" opacity=".35"/>`);
      parts.push(`<circle cx="${x(r.ci.p)}" cy="${cy}" r="7" style="fill: ${color}; stroke: var(--surface)" stroke-width="2"><title>${escapeHtml(a)} vs ${escapeHtml(b)}: ${pct(r.ci.p, 1)} (95% ${pct(r.ci.lo, 1)}–${pct(r.ci.hi, 1)})</title></circle>`);
      parts.push(`<text x="${x(r.ci.hi) + 10}" y="${cy + 4}" font-weight="600">${pct(r.ci.p, 1)}</text>`);
    });
    svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
    svg.innerHTML = parts.join("");
  }

  // Weights per feature: grouped horizontal bars, one group per feature.
  const sets = Object.keys(NIGHT.weights);
  legend("weights-legend", sets.map(s => ({ name: s, color: WEIGHT_COLORS[s] || "var(--s3)" })));
  const wsvg = $("weights-chart");
  const features = NIGHT.features;
  const barH = 9, gap = 3, groupH = sets.length * (barH + gap) + 12;
  const width = 1200, labelW = 170, m = { top: 10, right: 60, bottom: 30 };
  const height = m.top + features.length * groupH + m.bottom;
  const max = Math.max(...sets.flatMap(s => NIGHT.weights[s]));
  const x = v => labelW + v / max * (width - labelW - m.right);
  const parts = [];
  [0, 0.1, 0.2].filter(v => v <= max).forEach(v => {
    parts.push(`<line class="grid" x1="${x(v)}" y1="${m.top}" x2="${x(v)}" y2="${height - m.bottom}"/>`);
    parts.push(`<text class="axis" x="${x(v)}" y="${height - 10}" text-anchor="middle">${Math.round(v * 100)}%</text>`);
  });
  features.forEach((f, i) => {
    const gy = m.top + i * groupH;
    parts.push(`<text x="${labelW - 10}" y="${gy + groupH / 2}" text-anchor="end">${escapeHtml(f)}</text>`);
    sets.forEach((s, k) => {
      const v = NIGHT.weights[s][i];
      const by = gy + 4 + k * (barH + gap);
      parts.push(`<rect x="${labelW}" y="${by}" width="${Math.max(2, x(v) - labelW)}" height="${barH}" rx="3" style="fill: ${WEIGHT_COLORS[s] || "var(--s3)"}"><title>${escapeHtml(s)} · ${escapeHtml(f)}: ${(v * 100).toFixed(1)}%</title></rect>`);
      if (s !== "equal") parts.push(`<text class="axis" x="${x(v) + 6}" y="${by + barH - 1}" font-size="10">${(v * 100).toFixed(1)}%</text>`);
    });
  });
  wsvg.setAttribute("viewBox", `0 0 ${width} ${height}`);
  wsvg.innerHTML = parts.join("");
}

// =====================================================================
// 4. NIGHT TOURNAMENT (ladder)
// =====================================================================
function timingFor(setting) {
  return (NIGHT.report.timing || []).find(t => t.setting === setting) || null;
}
function renderTournament() {
  const ladder = nightLadder();
  const list = $("ladder");
  if (!ladder) {
    $("tournament-takeaway").textContent = "Run NightTournamentTest to fill this section.";
    list.innerHTML = "";
    return;
  }
  const limit = NIGHT.report.limitMs || 4000;
  const timing = (NIGHT.report.timing || []).filter(t => !t.setting.startsWith("champion"));
  const allowed = timing.filter(t => t.allowed), excluded = timing.filter(t => !t.allowed);
  const takeOvers = ladder.rounds.filter(r => r.takesOver);
  const provenTakeOvers = takeOvers.filter(r => r.ci.lo > 0.5);
  $("tournament-takeaway").innerHTML = `<strong>${escapeHtml(prettyAgent(ladder.champion))}</strong> won the ladder.
    ${takeOvers.length} of ${ladder.rounds.length} challengers took over, but only ${provenTakeOvers.length}
    ${provenTakeOvers.length === 1 ? "was" : "were"} <em>proven</em> stronger; the other take-overs won just over half their games.`;

  const items = [];
  // Qualifying: the time limit decides who may play at all.
  items.push(`<li class="stage"><div class="stage-title">Qualifying · slowest move must stay under ${fmtMs(limit)}</div></li>`);
  items.push(`<li><div class="round"><div>
      ${allowed.map(t => badge("good", `${prettySetting("x/" + t.setting)} · ${fmtMs(t.maxMs)}`)).join(" ")}
      ${excluded.map(t => badge("bad", `${prettySetting("x/" + t.setting)} · ${t.decisions ? fmtMs(t.maxMs) : "not timed"}`)).join(" ")}
    </div><div></div>
    <div class="why">${allowed.length} settings qualified. ${excluded.map(t => `${prettySetting("x/" + t.setting)} ${t.decisions ? `was stopped after a ${fmtMs(t.maxMs)} move` : "was skipped because a smaller K was already too slow"}`).join("; ")}.</div>
  </div></li>`);

  let lastKind = null;
  items.push(`<li class="holder"><span class="holder-chip">Start: ${escapeHtml(prettyAgent(ladder.start))}</span>
    <span class="muted"> · the old agent (equal weights, cheapest depth-2 setting)</span></li>`);
  ladder.rounds.forEach((r, i) => {
    if (r.kind !== lastKind) {
      items.push(`<li class="stage"><div class="stage-title">${r.kind === "coefficients" ? "Round A · which weights?" : "Round B · which search setting? (with the winning weights)"}</div></li>`);
      lastKind = r.kind;
    }
    const proven = r.ci.lo > 0.5, provenWorse = r.ci.hi < 0.5;
    const status = r.takesOver ? badge(proven ? "good" : "neutral", proven ? "takes over · proven" : "takes over · not proven")
                               : badge(provenWorse ? "bad" : "neutral", provenWorse ? "loses · proven weaker" : "loses · not proven");
    const stopped = r.games.length < 600 && (r.ci.lo > 0.5 || r.ci.hi < 0.5) ? " The match stopped early because the result was already clear at 99%." : "";
    const why = r.takesOver
      ? `${escapeHtml(prettyAgent(r.a))} won ${r.ci.wins} of ${r.ci.games} games, more than half, so it takes over.`
        + (proven ? " The whole 95% range is above 50%: a real improvement." : ` But the 95% range (${pct(r.ci.lo)}–${pct(r.ci.hi)}) still includes 50%, so it is at most a small improvement.`)
      : `${escapeHtml(prettyAgent(r.a))} won only ${r.ci.wins} of ${r.ci.games} games, so ${escapeHtml(prettyAgent(r.holder))} stays champion.`
        + (provenWorse ? " It is proven weaker." : "");
    const t = timingFor(parseAgent(r.a).setting);
    items.push(`<li><div class="round ${r.takesOver ? "won" : ""}">
        <div class="who"><span class="agent">${escapeHtml(prettyAgent(r.a))}</span><span class="vs">challenges</span><span class="agent">${escapeHtml(prettyAgent(r.holder))}</span>
          <div style="margin-top:4px">${status} <span class="muted" style="font-weight:400">${pct(r.ci.p, 1)} · points/game ${r.net >= 0 ? "+" : ""}${r.net.toFixed(2)}${t ? ` · slowest move ${fmtMs(t.maxMs)}` : ""}</span></div></div>
        <div>${ciBar(r.ci, r.takesOver ? "var(--s1)" : "var(--gray)")}</div>
        <div class="why">${why}${stopped}</div>
      </div></li>`);
    if (r.takesOver || i === ladder.rounds.length - 1) {
      const holder = r.takesOver ? r.a : r.holder;
      const final = i === ladder.rounds.length - 1;
      items.push(`<li class="${final ? "final" : "holder"}"><span class="holder-chip">${final ? "Champion: " : "Champion now: "}${escapeHtml(prettyAgent(holder))}</span></li>`);
    }
  });
  if (ladder.evidence.length) {
    items.push(`<li class="stage"><div class="stage-title">Final check · champion against the agents we had before</div></li>`);
    ladder.evidence.forEach(r => {
      const k = verdictKind(r.ci);
      items.push(`<li><div class="round ${k === "better" ? "won" : ""}">
        <div class="who"><span class="agent">${escapeHtml(prettyAgent(r.a))}</span><span class="vs">vs</span><span class="agent">${escapeHtml(prettyAgent(r.b))}</span>
          <div style="margin-top:4px">${badge(k === "better" ? "good" : k === "worse" ? "bad" : "neutral", k === "better" ? "champion is stronger · proven" : k === "worse" ? "champion is weaker" : "not proven")}
          <span class="muted" style="font-weight:400">${pct(r.ci.p, 1)} of ${r.ci.games} games · points/game ${r.net >= 0 ? "+" : ""}${r.net.toFixed(2)}</span></div></div>
        <div>${ciBar(r.ci)}</div>
        <div class="why">All the small steps add up: the champion wins ${pct(r.ci.p)} (95% range ${pct(r.ci.lo)}–${pct(r.ci.hi)}) against ${escapeHtml(prettyAgent(r.b))}.</div>
      </div></li>`);
    });
  }
  list.innerHTML = items.join("");
}

// =====================================================================
// 5. DECISION
// =====================================================================
function renderDecision() {
  const ladder = nightLadder();
  const report = NIGHT.report || {};
  const limit = report.limitMs || 4000;
  const timing = report.timing || [];
  const champion = report.champion || (ladder && ladder.champion);
  if (!champion) {
    $("decision-cards").innerHTML = "";
    emptySvg($("timing-chart"), "No night timing yet");
    return;
  }
  const champTiming = timing.find(t => t.setting.startsWith("champion")) || timingFor(parseAgent(champion).setting);
  const evidence = ladder ? ladder.evidence : [];
  const vsOld = evidence.find(r => r.b === ladder.start);
  const vsDepth1 = evidence.find(r => parseAgent(r.b).depth === 1);

  const cards = [
    card("Night champion", escapeHtml(prettySetting(champion)), `with ${escapeHtml(parseAgent(champion).weights)} weights`),
    champTiming ? card("Slowest move", fmtMs(champTiming.maxMs), `limit ${fmtMs(limit)} · average ${fmtMs(champTiming.meanMs)} · ${champTiming.decisions} timed moves`) : "",
    vsOld ? card("vs the old agent", pct(vsOld.ci.p), `${escapeHtml(prettyAgent(vsOld.b))} · 95% range ${pct(vsOld.ci.lo)}–${pct(vsOld.ci.hi)}`) : "",
    vsDepth1 ? card("vs depth 1", pct(vsDepth1.ci.p), `95% range ${pct(vsDepth1.ci.lo)}–${pct(vsDepth1.ci.hi)}, ${vsDepth1.ci.games} games`) : "",
  ].join("");
  $("decision-cards").innerHTML = cards;
  $("decision-code").textContent = `// Advanced level (night champion setting, newer tuned weights)\n`
    + `new AdvancedAgent(new BoardEvaluator(TUNED_NEW), 2, 10)\n\n`
    + `// Night champion as measured (tuned-best weights)\n` + (report.code || "");

  // Timing chart: one row per setting; bar = mean, tick = p95, dot = slowest move; dashed line = limit.
  const svg = $("timing-chart");
  const rows = timing.slice();
  const width = 1200, rowH = 30, labelW = 250, valueW = 230, m = { top: 26, bottom: 34 };
  const height = m.top + rows.length * rowH + m.bottom;
  const maxMs = Math.max(limit * 1.25, ...rows.map(t => t.maxMs));
  const x = v => labelW + v / maxMs * (width - labelW - valueW);
  const parts = [];
  for (let s = 0; s * 1000 <= maxMs; s += maxMs > 8000 ? 2 : 1) {
    parts.push(`<line class="grid" x1="${x(s * 1000)}" y1="${m.top - 6}" x2="${x(s * 1000)}" y2="${height - m.bottom}"/>`);
    parts.push(`<text class="axis" x="${x(s * 1000)}" y="${height - 12}" text-anchor="middle">${s} s</text>`);
  }
  parts.push(`<rect x="${x(limit)}" y="${m.top - 6}" width="${x(maxMs) - x(limit)}" height="${height - m.bottom - m.top + 6}" style="fill: var(--critical)" opacity=".06"/>`);
  parts.push(`<line x1="${x(limit)}" y1="${m.top - 14}" x2="${x(limit)}" y2="${height - m.bottom}" style="stroke: var(--critical)" stroke-width="2" stroke-dasharray="6 4"/>`);
  parts.push(`<text x="${x(limit) + 6}" y="${m.top - 10}" font-size="12" font-weight="600">limit ${fmtMs(limit)}</text>`);
  rows.forEach((t, i) => {
    const cy = m.top + i * rowH + rowH / 2;
    const isChampion = t.setting.startsWith("champion");
    const name = isChampion ? "Champion, 20 positions" : prettySetting("x/" + t.setting);
    parts.push(`<text x="${labelW - 10}" y="${cy + 4}" text-anchor="end" font-weight="${isChampion ? 700 : 400}">${escapeHtml(name)}</text>`);
    if (!t.decisions) {
      parts.push(`<text class="axis" x="${labelW}" y="${cy + 4}">not timed (a smaller K was already too slow)</text>`);
      return;
    }
    const color = t.allowed ? (isChampion ? "var(--s1)" : "var(--gray)") : "var(--critical)";
    parts.push(`<rect x="${labelW}" y="${cy - 5}" width="${Math.max(2, x(t.meanMs) - labelW)}" height="10" rx="4" style="fill: ${color}" opacity=".35"><title>average ${fmtMs(t.meanMs)}</title></rect>`);
    parts.push(`<line x1="${x(t.p95Ms)}" y1="${cy - 8}" x2="${x(t.p95Ms)}" y2="${cy + 8}" style="stroke: ${color}" stroke-width="2"><title>95th percentile ${fmtMs(t.p95Ms)}</title></line>`);
    parts.push(`<circle cx="${x(t.maxMs)}" cy="${cy}" r="6.5" style="fill: ${color}; stroke: var(--surface)" stroke-width="2"><title>slowest ${fmtMs(t.maxMs)} (${escapeHtml(t.slowest)}), ${t.decisions} moves timed${t.allowed ? "" : ", timing stopped at the first move over the limit"}</title></circle>`);
    const label = t.allowed ? `${fmtMs(t.maxMs)} ✓ allowed` : `${fmtMs(t.maxMs)} ✕ over the limit`;
    parts.push(`<text x="${width - valueW + 12}" y="${cy + 4}" ${t.allowed ? "" : 'style="fill: var(--critical)"'}>${label}</text>`);
  });
  svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
  svg.innerHTML = parts.join("");
  $("timing-legend").innerHTML = `<span><i style="background: var(--s1)"></i>champion</span>
    <span><i style="background: var(--gray)"></i>allowed</span><span><i style="background: var(--critical)"></i>over the limit</span>
    <span class="muted">● slowest move · | 95th percentile · bar = average</span>`;

  // Champion re-checked after the colour tie-break fix (OvernightExperimentsTest).
  const champName = (() => { const a = parseAgent(champion); return (a.weights === "equal" ? "" : a.weights + "-") + `depth${a.depth}` + (a.k === null ? "-full" : `-move-filter-k${a.k}`); })();
  const afterFix = new Map();
  STRENGTH.filter(g => matchTag(g.match) === "fixed" && g.agentA === champName).forEach(g => {
    if (!afterFix.has(g.agentB)) afterFix.set(g.agentB, { wins: 0, games: 0 });
    const r = afterFix.get(g.agentB); r.games++; if (g.winner === "A") r.wins++;
  });
  $("afterfix-block").hidden = afterFix.size === 0;
  $("afterfix-table").innerHTML = `<thead><tr><th>Opponent</th><th>Before the fix (night)</th><th>After the fix</th></tr></thead><tbody>`
    + [...afterFix.entries()].map(([opponent, r]) => {
      const before = evidence.find(e => { const b = parseAgent(e.b); return parseStrengthName(opponent) && b.weights === parseStrengthName(opponent).weights && b.depth === parseStrengthName(opponent).depth && b.k === parseStrengthName(opponent).k; });
      return `<tr><td>${escapeHtml(opponent)}</td><td>${before ? ciText(before.ci) : "–"}</td><td>${ciText(wilson(r.wins, r.games))}</td></tr>`;
    }).join("") + `</tbody>`;

  // Caveats, computed where possible.
  const caveats = [];
  if (ladder) {
    const unproven = ladder.rounds.filter(r => r.takesOver && r.ci.lo <= 0.5 && r.kind === "setting");
    if (unproven.length) {
      // Cheapest setting that took over in the ladder, as a faster alternative to the champion.
      const champSetting = timingFor(parseAgent(champion).setting);
      const cheaper = ladder.rounds.filter(r => r.takesOver && r.kind === "setting" && r.a !== champion)
        .map(r => timingFor(parseAgent(r.a).setting)).filter(t => t && champSetting && t.maxMs < champSetting.maxMs / 2).pop();
      caveats.push(`The steps between search settings were each too small to prove on their own (${unproven.map(r => `${prettySetting(r.a)}: ${pct(r.ci.p, 1)}`).join(", ")}). `
        + (cheaper ? `So a cheaper setting plays almost as well: ${prettySetting("x/" + cheaper.setting)} has a slowest move of ${fmtMs(cheaper.maxMs)} instead of ${fmtMs(champSetting.maxMs)}, a good choice if the game should feel snappier.` : ""));
    }
    const coeff = ladder.rounds.find(r => r.kind === "coefficients" && r.ci.lo > 0.5);
    if (coeff) caveats.push(`The weights are the clearest result: ${escapeHtml(parseAgent(coeff.a).weights)} beat ${escapeHtml(parseAgent(coeff.b).weights)} with ${pct(coeff.ci.p)} (95% range ${pct(coeff.ci.lo)}–${pct(coeff.ci.hi)}).`);
  }
  if (champTiming) caveats.push(`The time limit is measured on one machine with one thread. The champion's slowest move (${fmtMs(champTiming.maxMs)}) leaves ${fmtMs(limit - champTiming.maxMs)} of margin; a slower laptop could need a smaller K.`);
  if (afterFix.size === 0) caveats.push("All games were played before the colour tie-break fix in <code>Round.moveBoard</code>/<code>moveBearOff</code>. Paired games cancel the colour advantage, but exact win rates can shift by a few points once the fix is in (<code>OvernightExperimentsTest</code> re-checks this).");
  else caveats.push("The champion was re-checked after the colour tie-break fix; see the table above.");
  $("caveats").innerHTML = caveats.map(c => `<li>${c}</li>`).join("");
}

// =====================================================================
// 6. DIFFICULTY LEVELS
// =====================================================================
const GAP = 0.60;      // a level must win at least this share of games against the level below
const LEVEL_CANDIDATES = [
  { name: "depth0-full", role: "easy" },
  { name: "tuned-new-depth0-full", role: "medium" },
  { name: "tuned-best-depth0-full", role: "medium" },
  { name: "depth1-full", role: "medium" },
  { name: "tuned-blend-depth1-full", role: "medium" },
  { name: "tuned-quarter-depth1-full", role: "medium" },
  { name: "tuned-blend-depth0-full", role: "medium" },
  { name: "tuned-best-depth1-full", role: "medium" },
  { name: "tuned-new-depth1-full", role: "medium" },
  { name: "tuned-best-depth2-move-filter-k10", role: "advanced" },
  { name: "tuned-new-depth2-move-filter-k10", role: "advanced" },
];
// The levels used in App.createAIPlayer. The page checks them against the data and
// mentions it when another candidate would fit better.
const PLANNED_LEVELS = { easy: "depth0-full", medium: "tuned-best-depth0-full", advanced: "tuned-new-depth2-move-filter-k10" };

function prettyStrengthName(name) {
  const a = parseStrengthName(name);
  if (!a) return name;
  return `depth ${a.depth}${a.k === null ? "" : `, K ${a.k}`} · ${a.weights} weights`;
}
// Head-to-head of x against y from strength.csv: games after the fix win over older ones over night copies.
function headToHead(x, y) {
  const byTag = { fixed: { wins: 0, games: 0 }, "": { wins: 0, games: 0 }, night: { wins: 0, games: 0 } };
  STRENGTH.forEach(g => {
    const asA = g.agentA === x && g.agentB === y, asB = g.agentA === y && g.agentB === x;
    if (!asA && !asB) return;
    const t = byTag[matchTag(g.match)];
    t.games++;
    if ((asA && g.winner === "A") || (asB && g.winner === "B")) t.wins++;
  });
  for (const tag of ["fixed", "", "night"]) {
    if (byTag[tag].games) return { ...wilson(byTag[tag].wins, byTag[tag].games), preFix: tag !== "fixed" };
  }
  return null;
}
// Average time per move of an agent over all its games (as measured while games ran in parallel).
function inGameMs(name) {
  let total = 0, n = 0;
  STRENGTH.forEach(g => {
    if (g.agentA === name) { total += g.msPerMoveA; n++; }
    if (g.agentB === name) { total += g.msPerMoveB; n++; }
  });
  return n ? total / n : null;
}
function slowestTimedMove(name) {
  const rows = ROWS.filter(r => r.improvements === name && r.board === "night-20-positions" && r.maxMs !== null);
  return rows.length ? Math.max(...rows.map(r => r.maxMs)) : null;
}
const constName = weights => weights.toUpperCase().replace(/-/g, "_");
function javaFor(name) {
  const a = parseStrengthName(name);
  return `new AdvancedAgent(new BoardEvaluator(${constName(a.weights)}), ${a.depth}${a.k === null ? "" : `, ${a.k}`})`;
}
const shortName = name => { const a = parseStrengthName(name); return a ? `d${a.depth}${a.k === null ? "" : ` K${a.k}`} · ${a.weights}` : name; };

// Bradley–Terry ratings from all head-to-heads (MM iterations), on the Elo scale with `anchor` = 0.
function ratings(names, anchor) {
  const pairs = [];
  names.forEach((x, i) => names.forEach((y, j) => {
    if (j <= i) return;
    const h = headToHead(x, y);
    if (h) pairs.push({ i, j, wi: h.wins + 0.5, wj: h.games - h.wins + 0.5 });   // +0.5: no infinite ratings
  }));
  const s = names.map(() => 1);
  for (let it = 0; it < 500; it++) {
    names.forEach((_, k) => {
      let wins = 0, denom = 0;
      pairs.forEach(p => {
        if (p.i !== k && p.j !== k) return;
        const other = p.i === k ? p.j : p.i, n = p.wi + p.wj;
        wins += p.i === k ? p.wi : p.wj;
        denom += n / (s[k] + s[other]);
      });
      if (denom > 0) s[k] = wins / denom;
    });
  }
  const base = s[names.indexOf(anchor)] || 1;
  return new Map(names.map((n, k) => [n, 400 * Math.log10(s[k] / base)]));
}

function renderLevelScale(order, rating, chosen) {
  const svg = $("levels-scale");
  if (!order.length) return emptySvg(svg, "No games between the candidates yet");
  const values = order.map(n => rating.get(n));
  const lo = Math.floor(Math.min(0, ...values) / 50) * 50 - 25, hi = Math.ceil(Math.max(...values) / 50) * 50 + 25;
  const width = 1200, rowH = 30, labelW = 330, m = { top: 20, right: 90, bottom: 34 };
  const height = m.top + order.length * rowH + m.bottom;
  const x = v => labelW + (v - lo) / (hi - lo) * (width - labelW - m.right);
  const levelOf = n => Object.keys(chosen).find(k => chosen[k] === n);
  const parts = [];
  for (let v = Math.ceil(lo / 50) * 50; v <= hi; v += 50) {
    parts.push(`<line class="grid" x1="${x(v)}" y1="${m.top - 8}" x2="${x(v)}" y2="${height - m.bottom}"/>`);
    parts.push(`<text class="axis" x="${x(v)}" y="${height - 12}" text-anchor="middle">${v > 0 ? "+" : ""}${v}</text>`);
  }
  // Chosen levels: bands showing the gap between them.
  const levelNames = ["easy", "medium", "advanced"].map(k => chosen[k]).filter(n => rating.has(n));
  for (let i = 1; i < levelNames.length; i++) {
    const a = rating.get(levelNames[i - 1]), b = rating.get(levelNames[i]);
    parts.push(`<rect x="${x(Math.min(a, b))}" y="${m.top - 8}" width="${Math.abs(x(b) - x(a))}" height="${height - m.bottom - m.top + 8}" style="fill: var(--s1)" opacity=".07"/>`);
    parts.push(`<text class="axis" x="${(x(a) + x(b)) / 2}" y="${m.top - 10}" text-anchor="middle" font-size="11">gap ${Math.round(b - a)}</text>`);
  }
  order.forEach((n, i) => {
    const cy = m.top + i * rowH + rowH / 2, v = rating.get(n), level = levelOf(n);
    parts.push(`<text x="${labelW - 10}" y="${cy + 4}" text-anchor="end" font-weight="${level ? 700 : 400}">${escapeHtml(prettyStrengthName(n))}</text>`);
    parts.push(`<line x1="${x(lo)}" y1="${cy}" x2="${x(v)}" y2="${cy}" style="stroke: var(--grid)" stroke-width="2"/>`);
    parts.push(`<circle cx="${x(v)}" cy="${cy}" r="${level ? 8 : 6}" style="fill: ${level ? "var(--s1)" : "var(--gray)"}; stroke: var(--surface)" stroke-width="2"><title>${escapeHtml(n)}: ${Math.round(v)}</title></circle>`);
    parts.push(`<text x="${x(v) + 14}" y="${cy + 4}" font-size="12">${Math.round(v)}${level ? ` · ${level}` : ""}</text>`);
  });
  svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
  svg.innerHTML = parts.join("");
}

function renderLevels() {
  $("gap-text").textContent = pct(GAP);
  const names = LEVEL_CANDIDATES.map(c => c.name);
  const played = names.filter(x => names.some(y => y !== x && headToHead(x, y)));

  // The planned levels (App); the data below only checks them.
  const { easy, medium, advanced } = PLANNED_LEVELS;
  const advancedOptions = LEVEL_CANDIDATES.filter(c => c.role === "advanced").map(c => c.name);
  const advH2H = headToHead(advancedOptions[1], advancedOptions[0]);
  const advancedByData = advH2H ? (advH2H.p >= 0.5 ? advancedOptions[1] : advancedOptions[0]) : advanced;

  // Medium: the candidate that is furthest from both neighbours (max of the smaller gap).
  const mediumOptions = LEVEL_CANDIDATES.filter(c => c.role === "medium").map(c => {
    const up = headToHead(c.name, easy), down = headToHead(advanced, c.name);
    return { name: c.name, up, down, score: up && down ? Math.min(up.p, down.p) : null };
  });
  const scored = mediumOptions.filter(m => m.score !== null).sort((a, b) => b.score - a.score);
  const mediumByData = scored.length ? scored[0].name : medium;
  const chosen = { easy, medium, advanced };

  const noticeable = ci => ci && ci.p >= GAP && ci.lo > 0.5;
  const step1 = headToHead(medium, easy), step2 = headToHead(advanced, medium), step3 = headToHead(advanced, easy);
  const slowest = slowestTimedMove(advanced);

  // Takeaway
  const parts = [];
  if (step1 && step2) {
    parts.push(`Easy = <strong>${prettyStrengthName(easy)}</strong>, Medium = <strong>${prettyStrengthName(medium)}</strong>,
      Advanced = <strong>${prettyStrengthName(advanced)}</strong>.`);
    parts.push(`Medium beats Easy ${pct(step1.p)}${noticeable(step1) ? "" : " (not a noticeable gap)"}; Advanced beats Medium ${pct(step2.p)}${noticeable(step2) ? "" : " (not a noticeable gap)"}.`);
  } else {
    parts.push(`The planned levels (as in <code>App</code>); not all of their matches are played yet. Run <code>OvernightExperimentsTest</code>.`);
  }
  if (mediumByData !== medium) parts.push(`The data would make ${prettyStrengthName(mediumByData)} the most balanced Medium.`);
  if (advancedByData !== advanced) parts.push(`At depth 2, K 10 the ${parseStrengthName(advancedByData).weights} weights did better.`);
  $("levels-takeaway").innerHTML = parts.join(" ");

  // Level cards
  const card3 = (tier, name, step, stepLabel) => {
    const ms = inGameMs(name);
    const time = tier === "Advanced" && slowest !== null ? `slowest move ${fmtMs(slowest)} (limit 4 s)` : ms !== null ? `about ${fmtMs(ms)} per move` : "";
    const stepText = step ? `${badge(noticeable(step) ? "good" : "neutral", noticeable(step) ? "noticeably stronger" : "gap too small")} ${pct(step.p, 1)} vs ${stepLabel}
      <span class="muted">(${pct(step.lo)}–${pct(step.hi)}, ${step.games} games${step.preFix ? ", pre-fix" : ""})</span>` : `<span class="muted">${stepLabel ? "no games vs " + stepLabel + " yet" : "the baseline"}</span>`;
    return `<div class="level"><div class="tier">${tier}</div><div class="what">${escapeHtml(prettyStrengthName(name))}</div>
      <div class="facts">${time}</div><div class="step-up">${stepText}</div></div>`;
  };
  $("levels-cards").innerHTML = card3("Easy", easy, null, "")
    + card3("Medium", medium, step1, "Easy") + card3("Advanced", advanced, step2, "Medium");

  // Ratings of everyone who played, sorted weakest to strongest.
  const playedNames = names.filter(n => played.includes(n));
  const rating = playedNames.length ? ratings(playedNames, playedNames.includes(easy) ? easy : playedNames[0]) : new Map();
  const order = playedNames.slice().sort((a, b) => rating.get(a) - rating.get(b));
  renderLevelScale(order, rating, chosen);
  if (!order.length) {
    $("levels-matrix").innerHTML = `<tbody><tr><td class="muted">No games between the candidates yet.</td></tr></tbody>`;
  } else {
    const chosenSet = new Set(Object.values(chosen));
    const cell = (x, y) => {
      if (x === y) return `<td class="cell" style="background: var(--soft)">–</td>`;
      const h = headToHead(x, y);
      if (!h) return `<td class="cell"></td>`;
      const d = Math.min(1, Math.abs(h.p - 0.5) / 0.3);             // 0 at 50%, full colour at 20% or 80%
      const pole = h.p >= 0.5 ? "var(--div-pos)" : "var(--div-neg)";
      return `<td class="cell" style="background: color-mix(in oklab, ${pole} ${Math.round(d * 65)}%, var(--div-mid))"
        title="${escapeHtml(x)} vs ${escapeHtml(y)}: ${pct(h.p, 1)} (95% ${pct(h.lo)}–${pct(h.hi)}), ${h.games} games${h.preFix ? ", before the fix" : ""}">
        <strong>${pct(h.p)}</strong><small>${h.games}${h.preFix ? " · pre-fix" : ""}</small></td>`;
    };
    $("levels-matrix").innerHTML = `<thead><tr><th class="left">row beats column →</th>${order.map(n => `<th>${escapeHtml(shortName(n)).replace(" · ", "<br>")}</th>`).join("")}</tr></thead><tbody>`
      + order.map(x => `<tr class="${chosenSet.has(x) ? "chosen" : ""}"><th class="rowhead">${escapeHtml(shortName(x))}${chosenSet.has(x) ? ` <span class="badge good">${Object.keys(chosen).find(k => chosen[k] === x)}</span>` : ""}</th>${order.map(y => cell(x, y)).join("")}</tr>`).join("")
      + `</tbody>`;
  }

  const usedWeights = [...new Set(Object.values(chosen).map(n => parseStrengthName(n).weights))];
  $("levels-code").textContent = usedWeights.map(w => `private static final double[] ${constName(w)} = {${(NIGHT.weights[w] || []).join(", ")}};`).join("\n")
    + `\n\n// in createAIPlayer(...)\n` + `if ("Easy".equals(difficulty)) {
    return new Player(name, color, ${javaFor(easy)});
} else if ("Medium".equals(difficulty)) {
    return new Player(name, color, ${javaFor(medium)});
}
return new Player(name, color, ${javaFor(advanced)});`;

  // Justification
  const why = [];
  if (advH2H) why.push(`Advanced uses the ${parseStrengthName(advanced).weights} weights: at depth 2, K 10 they won ${pct(Math.max(advH2H.p, 1 - advH2H.p), 1)} of ${advH2H.games} games against the other tuned weights (${verdictOf(advH2H, "").kind === "neutral" ? "not a significant difference, so either works" : "a real difference"}).`);
  else why.push("Advanced: depth 2 with K 10 is the strongest setting under the time limit (night tournament); its weights are still being compared.");
  if (slowest !== null) why.push(`Advanced's slowest timed move is ${fmtMs(slowest)}, ${slowest <= 4000 ? "within" : "over"} the 4 s limit; the depth-1 fallback after 4 s keeps it safe on slower computers.`);
  scored.forEach(m => why.push(`Medium candidate ${prettyStrengthName(m.name)}: beats Easy ${pct(m.up.p)}, loses to Advanced ${pct(1 - m.down.p)} → smaller gap ${pct(m.score)}${m.name === medium ? " (used in the game)" : ""}${m.name === mediumByData && m.name !== medium ? " (most balanced)" : ""}.`));
  why.push("Medium has the same search as Easy (depth 0) but the tuned-best weights: better judgement of positions, no look-ahead. That puts it between Easy and Advanced on both sides.");
  const easyTuned = headToHead("tuned-new-depth0-full", easy);
  if (easyTuned) why.push(`Easy keeps equal weights on purpose: the same depth 0 with the tuned weights wins ${pct(easyTuned.p)} against it, so tuning alone already makes a different level.`);
  $("levels-why").innerHTML = why.map(w => `<li>${w}</li>`).join("");

  return chosen;
}

// =====================================================================
// APPENDIX
// =====================================================================
function renderStrength() {
  const table = $("strength-table");
  if (!STRENGTH.length) { table.innerHTML = `<tbody><tr><td class="muted">No matches recorded yet.</td></tr></tbody>`; return; }
  const matches = new Map();
  STRENGTH.forEach(r => { if (!matches.has(r.match)) matches.set(r.match, []); matches.get(r.match).push(r); });
  const head = `<thead><tr><th>Agent A</th><th class="left">Agent B</th><th>Games</th><th>Win rate A (95% range)</th><th></th>
    <th>Net points / game</th><th>A time / move</th><th>B time / move</th></tr></thead>`;
  const body = [...matches.entries()].map(([name, games]) => {
    const first = games[0], n = games.length;
    const ci = wilson(games.filter(g => g.winner === "A").length, n);
    const net = games.reduce((s, g) => s + (g.winner === "A" ? g.points : -g.points), 0) / n;
    const msA = games.reduce((s, g) => s + g.msPerMoveA, 0) / n, msB = games.reduce((s, g) => s + g.msPerMoveB, 0) / n;
    return `<tr><td>${escapeHtml(first.agentA)}${name.endsWith("(night)") ? ' <span class="muted">(night)</span>' : ""}</td>
      <td class="left">${escapeHtml(first.agentB)}</td><td>${n}</td><td>${pct(ci.p)} <span class="muted">(${pct(ci.lo)}–${pct(ci.hi)})</span></td>
      <td>${ciBar(ci)}</td><td>${net >= 0 ? "+" : ""}${net.toFixed(2)}</td><td>${fmtMs(msA)}</td><td>${fmtMs(msB)}</td></tr>`;
  }).join("");
  table.innerHTML = head + `<tbody>${body}</tbody>`;
}

const TABLE_COLUMNS = [
  ["improvements", "Improvement", r => escapeHtml(r.improvements)],
  ["board", "Board", r => escapeHtml(r.board)],
  ["dice", "Dice", r => r.dice],
  ["medianMs", "Median", r => r.medianMs === null ? "" : fmtMs(r.medianMs)],
  ["maxMs", "Max", r => r.maxMs === null ? "" : fmtMs(r.maxMs)],
  ["nodeVisits", "Node visits", r => r.nodeVisits ?? ""],
  ["generatedTurns", "Generated turns", r => r.generatedTurns ?? ""],
  ["duplicateTurns", "Duplicate turns", r => r.duplicateTurns ?? ""],
];
function renderRowsTable() {
  const query = $("filter").value.trim().toLowerCase();
  let rows = ROWS.filter(r => !query || `${r.improvements} ${r.board} ${r.dice}`.toLowerCase().includes(query));
  if (state.sortKey) {
    const k = state.sortKey, dir = state.sortAsc ? 1 : -1;
    rows = rows.slice().sort((a, b) => {
      const va = a[k], vb = b[k];
      if (va === null || va === undefined) return 1;
      if (vb === null || vb === undefined) return -1;
      return (typeof va === "number" ? va - vb : String(va).localeCompare(String(vb), undefined, { numeric: true })) * dir;
    });
  }
  const head = `<thead><tr>${TABLE_COLUMNS.map(([key, label]) => `<th data-key="${key}">${label}${state.sortKey === key ? (state.sortAsc ? " ▲" : " ▼") : ""}</th>`).join("")}</tr></thead>`;
  $("rows-table").innerHTML = head + `<tbody>${rows.map(r => `<tr>${TABLE_COLUMNS.map(([, , cell]) => `<td>${cell(r)}</td>`).join("")}</tr>`).join("")}</tbody>`;
  $("rows-table").querySelectorAll("th[data-key]").forEach(th => th.addEventListener("click", () => {
    state.sortAsc = state.sortKey === th.dataset.key ? !state.sortAsc : true;
    state.sortKey = th.dataset.key;
    renderRowsTable();
  }));
}

// ---------- wiring ----------
$("show-all").addEventListener("change", () => { state.showAll = $("show-all").checked; renderEfficiency(); });
$("metric").addEventListener("change", () => { state.metric = $("metric").value; renderWork(selection()); });
$("filter").addEventListener("input", renderRowsTable);

state.depth = defaultDepth();
renderEfficiencyTakeaway();
renderEfficiency();
renderTradeoff();
renderDepth3();
renderCoefficients();
renderTournament();
renderDecision();
renderLevels();
// Answer box: the three levels the game uses (same cards as step 6).
$("answer").innerHTML = `<div class="step">Answer</div><h2>The game's three difficulty levels</h2>
  <div class="levels">${$("levels-cards").innerHTML}</div>
  <p class="note" style="margin:6px 0 0">Advanced = the night tournament's best search setting (depth 2, K 10, slowest move under 4 s,
    depth-1 answer if it runs out) with the newer tuned weights. Why: <a href="#tournament">night tournament</a>,
    <a href="#decision">time limit</a>, <a href="#levels">level matches</a>.</p>`;
renderStrength();
</script>
</body>
</html>
"""


def main() -> None:
    OUTPUT.write_text(build_report(), encoding="utf-8")
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
