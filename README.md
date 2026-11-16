# Smart City Route Navigator

A small plain-Java app that plans a trip through an invented city. It was built as practice for graph algorithms: the city is a weighted graph, single legs come from Dijkstra's algorithm, and the stop order is improved by brute force or by a nearest-stop rule.

There is no framework and no build tool. The server is the JDK's own `com.sun.net.httpserver.HttpServer`, the JSON reader and writer are part of the project, and the whole app compiles with `javac` and runs with `java`.

## How it is different from Google Maps

The one difference worth naming: this planner tries to reorder your stops to save time, and it shows your order and its order side by side so you can check the result. It makes no claim about how Google Maps works inside.

## Quick start

**Windows: double-click `run.bat`.** That is the entire one-click start — no terminal commands, no build tool, no downloads. It checks for JDK 17 or newer, compiles the sources with `javac` into `out\`, starts the server on port 8080 and opens the planner page at <http://localhost:8080> once the server answers. It polls `/api/locations` to know the server is up and then opens `/`, not the JSON list. Closing the window stops the server. If the browser does not open, the window prints the address to paste.

You need JDK 17 or newer (<https://adoptium.net>). Nothing is downloaded: there are no dependencies.

The launcher compiles first, so a compile error is shown in the same window and the server is not started. It calls `javac` on the three source folders, which means no build tool is involved and no dependency is downloaded.

## How it works

**The city is a graph.** `CityData` builds it: 18 locations and 31 two-way roads. Each road has a distance in km and a fixed traffic level. A road is stored twice, once for each direction, and `CityMap.getRoadCount()` divides by two to report the real count.

**One leg is a Dijkstra run.** `Dijkstra.shortestPath` keeps a `PriorityQueue` of (location, time) steps. There is no decrease-key: when a place is reached again with a better time it is pushed again, and the old entry is skipped by the check `if (current.time > bestTime.get(current.name))`. Equal times are ordered by location name, so the result is deterministic. Distance is carried along as a second total.

**Traffic multiplies the time.** Base speed is 30 km/h, so a road takes `distanceKm * 2 * multiplier` minutes. The multipliers are `LOW` 1.0, `MEDIUM` 1.5 and `HIGH` 2.0, fixed in the data. A short HIGH road can therefore lose against a longer quiet detour.

**The drawing is only a picture.** `CityLayout` holds one `(x, y)` per place inside a 640 by 730 box, and `GET /api/map` hands those positions to the page together with the 31 roads. The page works out nothing: it joins the positions in the order the planner returned to draw the route. The map is not to scale, and the road lines are not distances.

The positions came from a throwaway Node script (not part of the app): a Fruchterman-Reingold layout whose edge targets were the real `distanceKm` values, run with 900 seeds. The script kept the layout with the fewest road crossings and the largest gap between places, then the old-town core was nudged into the middle and the airport to the edge. The result has 6 crossings (the same as the earlier, hand-placed map) and a minimum gap of 88 map units, about 83px at a 1440px window. The script also scored four label sides per place against the roads and the other labels, and those first choices sit in `LABEL_SIDES` in `app.js`; the page checks them again after the fonts load and when the zoom changes.

**The km/h on each leg is a display number.** The page divides the leg's rounded distance by its rounded time and multiplies by 60. With the 30 km/h base, a lone LOW road shows about 30 km/h, a MEDIUM road about 20 km/h and a HIGH road about 15 km/h, and legs that mix roads fall in between. Because the numbers are already rounded, the figure is approximate; the API is not involved.

**The planner tries stop orders.** For up to 7 stops (`RoutePlanner.MAX_BRUTE_FORCE_STOPS`) it walks every permutation. Dijkstra is run once from each point a leg can leave and the results are cached, so each extra order only costs a few additions. The worst case is `n!`: 7 stops means 5,040 orders, and one more stop would mean 40,320, which is why there is a cutoff.

**Above 7 stops it takes the nearest next stop.** From the current place it hops to the closest unvisited stop, then to the next closest, and so on. That is fast but not guaranteed to be the best order.

**The order is renumbered by the first time each stop is really reached.** A leg can pass straight through a stop that would otherwise be numbered later: in the bug trip, City Hospital to Grand Mall runs through Central Market, so number 2 was reached after number 3 and the map numbers jumped backwards. After either search finds its order, `RoutePlanner.reorderByFirstVisit` joins every leg into one path, puts the middle stops in the order they first appear on that path, and rebuilds the legs from the cached Dijkstra results. The start stays first and the destination stays last, so a round trip still begins and ends at the start. The new total time is compared with the old one using `TIME_EPSILON`; if it were somehow worse, the old order would be kept. The nearest-next-stop result goes through the same method, so the two search paths do not duplicate this logic. A repeated stop is left exactly as it was.

**The user's order is kept when nothing is better.** Both totals are compared and the greedy order is used only if it is faster by more than `RoutePlanner.TIME_EPSILON` (1e-9). Without that epsilon, two orders with the same real time could add up to numbers like 38.6 and 38.599999999999994 and the planner would reorder stops for no gain. On a tie the order the user typed stays.

## The web layer

**The server.** `Main` reads the port, checks that `static/` exists and starts `server.WebServer`. `WebServer` creates one `HttpServer` on that port with an 8-thread pool and two contexts: `/api/` goes to `ApiHandler`, everything else goes to `StaticFileHandler`. Ctrl+C runs a shutdown hook that stops the server.

**The API.** `ApiHandler` matches the four paths itself. It checks the HTTP method (405 with an `Allow` header otherwise), reads the request body as UTF-8, and writes JSON with the right status code. `RouteService` and the engine behind it are unchanged from the earlier version, so validation messages, ordering and numbers are the same.

**The JSON.** `json/Json.java` is a small recursive-descent parser and a writer, about 400 lines, using only `java.util`. The parser turns an object into a `LinkedHashMap`, an array into an `ArrayList`, a string into a `String`, a number into a `Double`, `true`/`false` into a `Boolean` and `null` into `null`. It rejects trailing text, unquoted keys, single quotes, leading zeros, unfinished strings and bad escapes by throwing `JsonException`. The writer escapes quotes, backslashes, control characters and the usual `\b \f \n \r \t`, and keeps object key order. `server.ApiJson` maps the response classes to JSON values explicitly, so the field names and order are easy to read and cannot drift silently.

**The page.** `StaticFileHandler` serves `static/` from disk. It only accepts `GET` and `HEAD`, picks a content type from the file extension (HTML, CSS, JS, JSON, SVG, PNG, JPEG, GIF, ICO, WOFF2, WOFF, TTF and TXT), and refuses any path that resolves outside the folder, which blocks `../` and encoded traversal attempts.

## The API

| Route | What it returns |
| --- | --- |
| `GET /api/locations` | All 18 location names, sorted alphabetically. |
| `GET /api/map` | Every location with its map position, and every road once, so the page can draw the city. |
| `POST /api/route` | The plan as JSON, or an error with HTTP 400. |
| `GET /api/nearest` | The fastest path to the closest emergency place, or an error with HTTP 400. |

`GET /api/nearest` takes `from` (a location name) and `type` (`hospital`, `fire` or `police`), for example `/api/nearest?from=College%20Gate&type=hospital`. It answers `{"success": true, "path": [...], "distanceKm": ..., "timeMinutes": ...}`. The place is the quickest one to reach, not the nearest on the map, so the trip time decides. An unknown place, an unknown type or a missing type answers HTTP 400 with the usual error body.

`GET /api/map` answers with `{"locations": [{"name", "x", "y"}], "roads": [{"from", "to", "distanceKm", "traffic"}]}`. The 31 roads are listed once each, not once per direction, and the positions are the ones `CityLayout` holds.

A sample request:

```
curl -X POST http://localhost:8080/api/route \
  -H "Content-Type: application/json" \
  -d '{"start":"College Gate","end":"Grand Mall","stops":["Central Market","City Hospital","Public Library"],"returnToStart":false}'
```

The response from that request:

```json
{
  "success": true,
  "message": "Route planned.",
  "method": "tried every order",
  "guaranteedBest": true,
  "yourOrder": {
    "stops": ["College Gate", "Central Market", "City Hospital", "Public Library", "Grand Mall"],
    "distanceKm": 11.5,
    "timeMinutes": 25.0
  },
  "optimized": {
    "stops": ["College Gate", "Public Library", "City Hospital", "Central Market", "Grand Mall"],
    "distanceKm": 8.3,
    "timeMinutes": 18.6
  },
  "legs": [
    {"from": "College Gate", "to": "Public Library", "path": ["College Gate", "Public Library"], "distanceKm": 0.9, "timeMinutes": 1.8, "passesThrough": [], "segments": [{"from": "College Gate", "to": "Public Library", "distanceKm": 0.9, "traffic": "LOW"}]},
    {"from": "Public Library", "to": "City Hospital", "path": ["Public Library", "City Hospital"], "distanceKm": 3.0, "timeMinutes": 6.0, "passesThrough": [], "segments": [{"from": "Public Library", "to": "City Hospital", "distanceKm": 3.0, "traffic": "LOW"}]},
    {"from": "City Hospital", "to": "Central Market", "path": ["City Hospital", "Old Town Square", "Central Market"], "distanceKm": 3.0, "timeMinutes": 8.0, "passesThrough": [], "segments": [{"from": "City Hospital", "to": "Old Town Square", "distanceKm": 2.0, "traffic": "MEDIUM"}, {"from": "Old Town Square", "to": "Central Market", "distanceKm": 1.0, "traffic": "LOW"}]},
    {"from": "Central Market", "to": "Grand Mall", "path": ["Central Market", "Grand Mall"], "distanceKm": 1.4, "timeMinutes": 2.8, "passesThrough": [], "segments": [{"from": "Central Market", "to": "Grand Mall", "distanceKm": 1.4, "traffic": "LOW"}]}
  ],
  "originalLegs": [
    {"from": "College Gate", "to": "Central Market", "path": ["College Gate", "Public Library", "Old Town Square", "Central Market"], "distanceKm": 2.5, "timeMinutes": 5.0, "passesThrough": ["Public Library"], "segments": [{"from": "College Gate", "to": "Public Library", "distanceKm": 0.9, "traffic": "LOW"}, {"from": "Public Library", "to": "Old Town Square", "distanceKm": 0.6, "traffic": "LOW"}, {"from": "Old Town Square", "to": "Central Market", "distanceKm": 1.0, "traffic": "LOW"}]},
    {"from": "Central Market", "to": "City Hospital", "path": ["Central Market", "Old Town Square", "City Hospital"], "distanceKm": 3.0, "timeMinutes": 8.0, "passesThrough": [], "segments": [{"from": "Central Market", "to": "Old Town Square", "distanceKm": 1.0, "traffic": "LOW"}, {"from": "Old Town Square", "to": "City Hospital", "distanceKm": 2.0, "traffic": "MEDIUM"}]},
    {"from": "City Hospital", "to": "Public Library", "path": ["City Hospital", "Public Library"], "distanceKm": 3.0, "timeMinutes": 6.0, "passesThrough": [], "segments": [{"from": "City Hospital", "to": "Public Library", "distanceKm": 3.0, "traffic": "LOW"}]},
    {"from": "Public Library", "to": "Grand Mall", "path": ["Public Library", "Old Town Square", "Central Market", "Grand Mall"], "distanceKm": 3.0, "timeMinutes": 6.0, "passesThrough": ["Central Market"], "segments": [{"from": "Public Library", "to": "Old Town Square", "distanceKm": 0.6, "traffic": "LOW"}, {"from": "Old Town Square", "to": "Central Market", "distanceKm": 1.0, "traffic": "LOW"}, {"from": "Central Market", "to": "Grand Mall", "distanceKm": 1.4, "traffic": "LOW"}]}
  ],
  "minutesSaved": 6.4
}
```

`legs` is the optimized trip and `originalLegs` is the same trip in your order; the page draws the first as the solid gradient and the second as the dashed cream line. Each leg also carries `passesThrough`, the other chosen stops its path crosses between its own start and end, and `segments`, one entry per road on its path, which is what the small traffic bar under the leg is built from. After the first-visit reordering `passesThrough` is normally empty, but a later leg can still pass a stop that was already visited, and the page shows "Passes through ..." for it.

A round trip sends `"returnToStart": true`. The end is then ignored completely, even if it is a place that does not exist. With start `Public Library` and stops `Central Market`, `College Gate`, `Grand Mall`, the totals are 11.0 km / 22.0 min for your order and 7.8 km / 15.6 min for the suggested one, so `minutesSaved` is 6.4.

Every error uses the same shape, `{"success": false, "message": "..."}`, with HTTP 400. The six validation messages, checked in this order:

1. `Pick a start location.`
2. `Pick a destination location.` (only when not returning to the start)
3. `Unknown location: <name>.`
4. `Stop '<name>' appears more than once.`
5. `Stop '<name>' is already your start or destination.`
6. `Start and destination are the same place. Tick 'return to start' for a round trip.`

A missing or malformed JSON body gets its own message: `Could not read the request.`

## Project structure

```
src/routenav/
  Main.java                                  entry point: reads the port, starts the server
  RouteService.java                          trims input, runs the six checks, builds the response
  EmergencyFinder.java                       closest emergency place by travel time
  NearestResponse.java                       the answer of GET /api/nearest
  RouteRequest.java                          request body (start, end, stops, returnToStart)
  RouteResponse.java                         success body
  ErrorResponse.java                         error body, success is always false
  ItinerarySummary.java                      one stop order with rounded totals
  LegSummary.java                            one leg with its path, rounded totals and road segments
  LegSegment.java                            one road of a leg with its traffic level
  CityLayout.java                            the drawn map's one set of x/y positions
  CityPoint.java                             one place on the map
  CityRoad.java                              one road for the drawing
  CityMapResponse.java                       the answer of GET /api/map
  RoutePlanner.java                          stop order: every permutation up to 7, then nearest next stop
  TripPlan.java                              planner result: your order, optimized order, method
  Dijkstra.java                              shortest path by time, priority queue, stale entries skipped
  CityMap.java                               the graph, road count, connectivity check
  CityData.java                              the 18 locations and 31 roads
  Road.java                                  one road and its travel time
  Traffic.java                               LOW 1.0, MEDIUM 1.5, HIGH 2.0
  PathResult.java                            one path: stops in order, distance, time
  Tests.java                                 plain checks for the planner numbers and Dijkstra
  json/
    Json.java                                the JSON parser and writer
    JsonException.java                       thrown for malformed JSON
  server/
    WebServer.java                           HttpServer setup, thread pool, shutdown
    ApiHandler.java                          GET /api/locations, GET /api/map, POST /api/route, GET /api/nearest
    ApiJson.java                             response classes to JSON values
    StaticFileHandler.java                   serves static/, MIME types, traversal check
static/
  index.html                                 the page
  styles.css                                 the page's one stylesheet
  app.js                                     fills the lists, draws and zooms the map, draws the plan, sends the request, keeps the plan in the URL
  fonts/bodoni-moda-latin.woff2              the display serif, served from this folder
  fonts/inter-latin.woff2                    the body sans, served from this folder
  fonts/OFL-BodoniModa.txt                   its OFL licence
  fonts/OFL-Inter.txt                        its OFL licence
run.bat                                      one-click start on Windows: Java check, compile, server, browser
run.sh                                       the same start on Linux and macOS
.gitattributes                               *.bat and *.cmd as CRLF, *.sh as LF
.gitignore                                   ignores out/ and editor folders
out/                                         compiled classes (created by the launchers, ignored by git)
```

## How to test

Compile the way the launcher does, then start the server on a spare port:

```
run.bat 8081
```

The automated tests are plain Java, with no library. Compile the way the launcher does and run them:

```
java -cp out routenav.Tests
```

The class prints `All tests passed (28 checks).` and throws an `AssertionError` with the failing check on the first wrong number. It covers the sample trip and the round trip totals in this README, the unknown-location message, the fact that Dijkstra avoids the short but jammed `College Gate` to `Central Market` road, that the bug trip and an 8-stop greedy trip reach their stops in the order they are numbered, and that a repeated stop does not crash the planner.

With the server running, these checks cover the whole API:

```
curl http://localhost:8081/api/locations
curl http://localhost:8081/api/map
curl -X POST http://localhost:8081/api/route -H "Content-Type: application/json" \
  -d '{"start":"College Gate","end":"Grand Mall","stops":["Central Market","City Hospital","Public Library"],"returnToStart":false}'
curl "http://localhost:8081/api/nearest?from=College%20Gate&type=hospital"
curl "http://localhost:8081/api/nearest?from=College%20Gate&type=train"
```

The trip above must answer `yourOrder` 11.5 km / 25.0 min, `optimized` 8.3 km / 18.6 min and `minutesSaved` 6.4. `GET /api/map` must hold 18 locations and 31 roads. A body of `not json` must answer HTTP 400 with `Could not read the request.`

## Known limitations

- The city is fictional, and the roads are two-way only.
- Traffic levels are fixed values in the data, not live traffic.
- Above 7 stops the nearest-next-stop order is not guaranteed to be the best one.
- The map is a drawing, not a real street map: road lines are not to scale.
- On a phone with no route planned, all 18 labels are drawn at 13px as the brief asks; a few of them sit over a road, which the halo keeps readable.
- There are no saved trips and no accounts.
- The page writes the last request into the URL, so a link re-plans it, but nothing is stored on the server and there is no browser history of past plans.
- The leave-at time is a page-only helper: the API and the planner do not know about it, so arrival times are not part of a plan or a shared link.
- The emergency lookup has one fixed place per type: City Hospital, Fire Station and North Police Station.
- The automated tests cover the planner numbers and the emergency lookup, not the page. The manual checklist above is still the way to check the page.

## Credits

The page uses two local fonts: Bodoni Moda for display and Inter for the body. Both are under the SIL Open Font License, and the `.woff2` files plus their licence texts sit in `static/fonts/`. Nothing is fetched from a remote font, image or script service.
