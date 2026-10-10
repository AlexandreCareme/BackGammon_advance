# Night tournament 2026-10-03 00:40

Limit 4.0 s per move, 4 threads, within the time budget.

## Champion: `tuned-best/d2-k10`

For App.createAIPlayer:

```java
new AdvancedAgent(new BoardEvaluator(new double[]{0.224, 0.152, 0.227, 0.041, 0.034, 0.088, 0.133, 0.101}), 2, 10)
```

## Matches (win rate of A, 95% Wilson interval)

| A | B | games | A wins | 95% interval | verdict | points/game A | max ms A | max ms B | stopped |
|---|---|---:|---:|---|---|---:|---:|---:|---|
| tuned-best/d2-k2 | equal/d2-k2 | 100 | 72.0% | 62.5-79.9% | **A better** | +0.870 | 374 | 337 | clear at 99% |
| tuned-tophalf/d2-k2 | tuned-best/d2-k2 | 600 | 46.5% | 42.5-50.5% | not significant | -0.125 | 489 | 462 | game limit |
| tuned-best/d2-k3 | tuned-best/d2-k2 | 600 | 52.7% | 48.7-56.6% | not significant | +0.135 | 728 | 321 | game limit |
| tuned-best/d2-k5 | tuned-best/d2-k3 | 600 | 53.7% | 49.7-57.6% | not significant | +0.123 | 1458 | 775 | game limit |
| tuned-best/d2-k8 | tuned-best/d2-k5 | 600 | 51.8% | 47.8-55.8% | not significant | +0.043 | 3354 | 1762 | game limit |
| tuned-best/d2-k10 | tuned-best/d2-k8 | 600 | 51.0% | 47.0-55.0% | not significant | +0.033 | 5644 | 3764 | game limit |
| tuned-best/d3-k2 | tuned-best/d2-k10 | 300 | 45.0% | 39.5-50.7% | not significant | -0.187 | 8466 | 5428 | game limit |
| tuned-best/d2-k10 | equal/d2-k2 | 50 | 70.0% | 56.2-80.9% | **A better** | +0.880 | 4413 | 575 | clear at 99% |
| tuned-best/d2-k10 | equal/d1-full | 150 | 63.3% | 55.4-70.6% | **A better** | +0.560 | 5415 | 285 | clear at 99% |

Max ms in games is measured while 4 games run at once, so it is higher than in the GUI.

## Time per decision (single-threaded, 6 positions x 21 rolls, equal weights)

| setting | decisions | mean ms | p95 ms | max ms | slowest | allowed |
|---|---:|---:|---:|---:|---|---|
| d2-k2 | 116 | 59 | 111 | 144 | position 1, roll 1-5 | yes |
| d2-k3 | 116 | 133 | 244 | 332 | position 4, roll 4-5 | yes |
| d2-k5 | 116 | 336 | 670 | 954 | position 1, roll 1-3 | yes |
| d2-k8 | 116 | 783 | 1710 | 1862 | position 1, roll 1-2 | yes |
| d2-k10 | 116 | 1127 | 2670 | 2875 | position 1, roll 2-3 | yes |
| d2-k12 | 30 | 2684 | 3848 | 4109 | position 1, roll 2-4 | no, stopped: over the limit |
| d2-k16 | 0 | 0 | 0 | 0 |  | no, skipped: a smaller K was too slow |
| d3-k2 | 116 | 1925 | 3194 | 3263 | position 2, roll 4-6 | yes |
| d3-k3 | 1 | 9936 | 9936 | 9936 | position 0, roll 1-1 | no, stopped: over the limit |
| champion d2-k10 (20 positions) | 388 | 741 | 2088 | 2778 | position 1, roll 2-3 | yes |

## Log

```
00:40:00  Start: 8.5 h, limit 4.0 s per move, 4 threads
00:40:03  Timing d1-full: mean 15 ms, p95 67 ms, max 144 ms (allowed)
00:40:10  Timing d2-k2: mean 59 ms, p95 111 ms, max 144 ms (allowed)
00:40:26  Timing d2-k3: mean 133 ms, p95 244 ms, max 332 ms (allowed)
00:41:05  Timing d2-k5: mean 336 ms, p95 670 ms, max 954 ms (allowed)
00:42:35  Timing d2-k8: mean 783 ms, p95 1710 ms, max 1862 ms (allowed)
00:44:46  Timing d2-k10: mean 1127 ms, p95 2670 ms, max 2875 ms (allowed)
00:46:07  Timing d2-k12: mean 2684 ms, p95 3848 ms, max 4109 ms (stopped: over the limit)
00:49:50  Timing d3-k2: mean 1925 ms, p95 3194 ms, max 3263 ms (allowed)
00:50:00  Timing d3-k3: mean 9936 ms, p95 9936 ms, max 9936 ms (stopped: over the limit)
00:50:00  Match tuned-best/d2-k2 vs equal/d2-k2 (0 games already recorded)
00:50:29    50 games: A wins 68.0% (95%: 54.2-79.2%)
00:50:57    100 games: A wins 72.0% (95%: 62.5-79.9%)
00:50:57  New champion (coefficients): tuned-best/d2-k2
00:50:57  Match tuned-tophalf/d2-k2 vs tuned-best/d2-k2 (0 games already recorded)
00:51:28    50 games: A wins 46.0% (95%: 33.0-59.6%)
00:51:59    100 games: A wins 47.0% (95%: 37.5-56.7%)
00:52:29    150 games: A wins 46.7% (95%: 38.9-54.6%)
00:53:03    200 games: A wins 45.5% (95%: 38.7-52.4%)
00:53:34    250 games: A wins 46.8% (95%: 40.7-53.0%)
00:54:06    300 games: A wins 44.7% (95%: 39.1-50.3%)
00:54:34    350 games: A wins 45.1% (95%: 40.0-50.4%)
00:55:05    400 games: A wins 45.8% (95%: 40.9-50.6%)
00:55:38    450 games: A wins 46.0% (95%: 41.4-50.6%)
00:56:15    500 games: A wins 45.0% (95%: 40.7-49.4%)
00:56:46    550 games: A wins 45.5% (95%: 41.3-49.6%)
00:57:14    600 games: A wins 46.5% (95%: 42.5-50.5%)
00:57:14  Match tuned-best/d2-k3 vs tuned-best/d2-k2 (0 games already recorded)
00:58:06    50 games: A wins 40.0% (95%: 27.6-53.8%)
00:58:56    100 games: A wins 47.0% (95%: 37.5-56.7%)
00:59:47    150 games: A wins 50.0% (95%: 42.1-57.9%)
01:00:38    200 games: A wins 50.0% (95%: 43.1-56.9%)
01:01:29    250 games: A wins 52.0% (95%: 45.8-58.1%)
01:02:20    300 games: A wins 52.7% (95%: 47.0-58.2%)
01:03:05    350 games: A wins 51.4% (95%: 46.2-56.6%)
01:03:57    400 games: A wins 51.2% (95%: 46.4-56.1%)
01:04:48    450 games: A wins 51.1% (95%: 46.5-55.7%)
01:05:41    500 games: A wins 50.8% (95%: 46.4-55.2%)
01:06:34    550 games: A wins 51.3% (95%: 47.1-55.4%)
01:07:27    600 games: A wins 52.7% (95%: 48.7-56.6%)
01:07:27  New champion (setting): tuned-best/d2-k3
01:07:27  Match tuned-best/d2-k5 vs tuned-best/d2-k3 (0 games already recorded)
01:09:40    50 games: A wins 50.0% (95%: 36.6-63.4%)
01:11:54    100 games: A wins 48.0% (95%: 38.5-57.7%)
01:14:05    150 games: A wins 51.3% (95%: 43.4-59.2%)
01:16:13    200 games: A wins 52.5% (95%: 45.6-59.3%)
01:18:09    250 games: A wins 51.2% (95%: 45.0-57.3%)
01:20:12    300 games: A wins 51.7% (95%: 46.0-57.3%)
01:22:12    350 games: A wins 51.4% (95%: 46.2-56.6%)
01:24:15    400 games: A wins 52.0% (95%: 47.1-56.9%)
01:26:21    450 games: A wins 52.2% (95%: 47.6-56.8%)
01:28:28    500 games: A wins 53.8% (95%: 49.4-58.1%)
01:30:31    550 games: A wins 53.8% (95%: 49.6-57.9%)
01:32:32    600 games: A wins 53.7% (95%: 49.7-57.6%)
01:32:32  New champion (setting): tuned-best/d2-k5
01:32:32  Match tuned-best/d2-k8 vs tuned-best/d2-k5 (0 games already recorded)
01:37:47    50 games: A wins 44.0% (95%: 31.2-57.7%)
01:43:10    100 games: A wins 45.0% (95%: 35.6-54.8%)
01:47:59    150 games: A wins 46.0% (95%: 38.2-54.0%)
01:53:24    200 games: A wins 48.0% (95%: 41.2-54.9%)
01:57:38    250 games: A wins 48.8% (95%: 42.7-55.0%)
02:02:17    300 games: A wins 48.7% (95%: 43.1-54.3%)
02:07:18    350 games: A wins 48.9% (95%: 43.7-54.1%)
02:12:07    400 games: A wins 51.0% (95%: 46.1-55.9%)
02:16:29    450 games: A wins 51.3% (95%: 46.7-55.9%)
02:20:58    500 games: A wins 51.6% (95%: 47.2-56.0%)
02:25:29    550 games: A wins 51.3% (95%: 47.1-55.4%)
02:29:39    600 games: A wins 51.8% (95%: 47.8-55.8%)
02:29:39  New champion (setting): tuned-best/d2-k8
02:29:39  Match tuned-best/d2-k10 vs tuned-best/d2-k8 (0 games already recorded)
02:37:47    50 games: A wins 52.0% (95%: 38.5-65.2%)
02:45:44    100 games: A wins 51.0% (95%: 41.3-60.6%)
02:53:19    150 games: A wins 50.7% (95%: 42.7-58.6%)
03:02:02    200 games: A wins 50.0% (95%: 43.1-56.9%)
03:08:50    250 games: A wins 49.6% (95%: 43.5-55.8%)
03:16:46    300 games: A wins 50.3% (95%: 44.7-56.0%)
03:24:34    350 games: A wins 50.3% (95%: 45.1-55.5%)
03:32:57    400 games: A wins 49.8% (95%: 44.9-54.6%)
03:40:55    450 games: A wins 50.2% (95%: 45.6-54.8%)
03:48:31    500 games: A wins 50.6% (95%: 46.2-55.0%)
03:56:21    550 games: A wins 50.5% (95%: 46.4-54.7%)
04:03:21    600 games: A wins 51.0% (95%: 47.0-55.0%)
04:03:21  New champion (setting): tuned-best/d2-k10
04:03:21  Match tuned-best/d3-k2 vs tuned-best/d2-k10 (0 games already recorded)
04:18:13    50 games: A wins 44.0% (95%: 31.2-57.7%)
04:31:54    100 games: A wins 48.0% (95%: 38.5-57.7%)
04:45:34    150 games: A wins 46.0% (95%: 38.2-54.0%)
04:59:21    200 games: A wins 45.0% (95%: 38.3-51.9%)
05:13:03    250 games: A wins 45.2% (95%: 39.1-51.4%)
05:27:23    300 games: A wins 45.0% (95%: 39.5-50.7%)
05:27:23  Match tuned-best/d2-k10 vs equal/d2-k2 (0 games already recorded)
05:31:51    50 games: A wins 70.0% (95%: 56.2-80.9%)
05:31:51  Match tuned-best/d2-k10 vs equal/d1-full (0 games already recorded)
05:36:46    50 games: A wins 60.0% (95%: 46.2-72.4%)
05:42:18    100 games: A wins 61.0% (95%: 51.2-70.0%)
05:46:58    150 games: A wins 63.3% (95%: 55.4-70.6%)
05:51:45  Timing d2-k10: mean 741 ms, p95 2088 ms, max 2778 ms (allowed)
05:51:45  Done. Champion: tuned-best/d2-k10
```
