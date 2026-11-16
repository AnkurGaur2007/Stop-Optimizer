// The page never calculates a route: every plan comes from the Java engine.
// It only draws what /api/map and /api/route sent.
var SVG_NS = 'http://www.w3.org/2000/svg';

var startSelect = document.getElementById('start-select');
var endSelect = document.getElementById('end-select');
var returnCheckbox = document.getElementById('return-checkbox');
var addStopButton = document.getElementById('add-stop-button');
var stopsContainer = document.getElementById('stops-container');
var exampleButton = document.getElementById('example-button');
var swapButton = document.getElementById('swap-button');
var resetButton = document.getElementById('reset-button');
var planButton = document.getElementById('plan-button');
var copyButton = document.getElementById('copy-button');
var statusMessage = document.getElementById('status-message');
var routeSummary = document.getElementById('route-summary');
var comparisonTable = document.getElementById('comparison-table');
var summaryText = document.getElementById('summary-text');
var stepsSection = document.getElementById('steps-section');
var routeSteps = document.getElementById('route-steps');
var emptyState = document.getElementById('empty-state');
var loadingCard = document.getElementById('loading-card');
var savedNumber = document.getElementById('saved-number');
var savedLabel = document.getElementById('saved-label');
var departTime = document.getElementById('depart-time');
var arrivalLine = document.getElementById('arrival-line');

var cityMap = document.getElementById('city-map');
var mapFrame = document.getElementById('map-frame');
var mapPopover = document.getElementById('map-popover');
var popoverName = document.getElementById('popover-name');

// The last plan shown, kept so the copy button can read it again.
var lastPlan = null;

var locations = [];
var stopCount = 0;

// The map drawing state.
var mapCity = null;
var mapPoints = {};
var popoverPlace = '';

loadCity();

function loadCity() {
    Promise.all([fetch('/api/locations'), fetch('/api/map')])
        .then(function (responses) {
            if (!responses[0].ok || !responses[1].ok) {
                throw new Error('Could not load the city.');
            }
            return Promise.all([responses[0].json(), responses[1].json()]);
        })
        .then(function (data) {
            locations = data[0];
            fillLocationSelect(startSelect);
            fillLocationSelect(endSelect);
            var stopSelects = stopsContainer.querySelectorAll('select');
            for (var i = 0; i < stopSelects.length; i++) {
                fillLocationSelect(stopSelects[i]);
            }
            buildMap(data[1]);
            restorePlanFromHash();
        })
        .catch(function () {
            // The list is a network call too, so a failure means the server is
            // not answering. Do not show whatever the browser wrote in the error.
            clearResults();
            showStatus('Could not reach the server. Is it running?', true);
        });
}

// Every location dropdown starts with an empty option so nothing is sent by
// accident, and the list always comes from the API instead of the page.
function fillLocationSelect(select) {
    var chosen = select.value;
    select.textContent = '';
    var empty = document.createElement('option');
    empty.value = '';
    empty.textContent = 'Choose…';
    select.appendChild(empty);
    for (var i = 0; i < locations.length; i++) {
        var option = document.createElement('option');
        option.value = locations[i];
        option.textContent = locations[i];
        select.appendChild(option);
    }
    select.value = chosen;
}

addStopButton.addEventListener('click', function () {
    addStopRow('');
});

// Adds one stop row. The example button and the map call this too, with a place
// already picked.
function addStopRow(chosen) {
    stopCount = stopCount + 1;

    var row = document.createElement('div');
    row.className = 'stop-row';

    var field = document.createElement('div');
    field.className = 'field';

    var selectId = 'stop-select-' + stopCount;
    var label = document.createElement('label');
    label.setAttribute('for', selectId);
    label.textContent = 'Stop ' + stopCount;

    var select = document.createElement('select');
    select.id = selectId;
    fillLocationSelect(select);

    var removeButton = document.createElement('button');
    removeButton.type = 'button';
    removeButton.className = 'pill pill-ghost pill-small';
    removeButton.textContent = 'Remove';
    removeButton.addEventListener('click', function () {
        stopsContainer.removeChild(row);
        renumberStops();
    });

    field.appendChild(label);
    field.appendChild(select);
    row.appendChild(field);
    row.appendChild(removeButton);
    stopsContainer.appendChild(row);

    if (chosen !== '') {
        select.value = chosen;
    }
}

// Removes every stop row and starts the numbering again at Stop 1.
function removeAllStops() {
    while (stopsContainer.firstChild) {
        stopsContainer.removeChild(stopsContainer.firstChild);
    }
    stopCount = 0;
}

// After a stop is removed the rows would say Stop 1, Stop 3, Stop 4. Walk the
// rows that are left and number them again, and move each label's for and the
// select's id together so the pair stays linked.
function renumberStops() {
    var rows = stopsContainer.querySelectorAll('.stop-row');
    for (var i = 0; i < rows.length; i++) {
        var number = i + 1;
        var select = rows[i].querySelector('select');
        var label = rows[i].querySelector('label');
        select.id = 'stop-select-' + number;
        label.setAttribute('for', select.id);
        label.textContent = 'Stop ' + number;
    }
}

// A round trip ends where it started, so the destination is not needed and
// there is nothing to swap.
returnCheckbox.addEventListener('change', function () {
    endSelect.disabled = returnCheckbox.checked;
    swapButton.disabled = returnCheckbox.checked;
    if (returnCheckbox.checked) {
        endSelect.value = '';
    }
});

// One fixed sample trip. Every name is checked against the list the server sent,
// so a changed city says so instead of filling a place that does not exist.
exampleButton.addEventListener('click', function () {
    var exampleStart = 'College Gate';
    var exampleEnd = 'City Hospital';
    var exampleStops = ['Grand Mall', 'Public Library', 'Central Market'];

    var missing = locations.indexOf(exampleStart) === -1
        || locations.indexOf(exampleEnd) === -1;
    for (var i = 0; i < exampleStops.length; i++) {
        if (locations.indexOf(exampleStops[i]) === -1) {
            missing = true;
        }
    }
    if (missing) {
        showStatus('The example does not match the city list.', true);
        return;
    }

    returnCheckbox.checked = false;
    endSelect.disabled = false;
    swapButton.disabled = false;
    startSelect.value = exampleStart;
    endSelect.value = exampleEnd;

    removeAllStops();
    for (var j = 0; j < exampleStops.length; j++) {
        addStopRow(exampleStops[j]);
    }
    showStatus('Example loaded. Press Plan route.', false);
});

swapButton.addEventListener('click', function () {
    var start = startSelect.value;
    startSelect.value = endSelect.value;
    endSelect.value = start;
});

resetButton.addEventListener('click', function () {
    startSelect.value = '';
    endSelect.value = '';
    returnCheckbox.checked = false;
    endSelect.disabled = false;
    swapButton.disabled = false;
    removeAllStops();
    clearResults();
    closePopover();
    resetView(true);
    lastPlan = null;
    showStatus('', false);
});

// The selected places keep their labels on a narrow screen, so a change in any
// dropdown has to refresh what the map shows.
document.addEventListener('change', function (event) {
    if (event.target.tagName === 'SELECT') {
        refreshLabelVisibility();
    }
});

planButton.addEventListener('click', function () {
    planRoute(readForm());
});

// Reads the form into the body the API expects. A shared link calls planRoute
// with its own request instead.
function readForm() {
    var request = {
        start: startSelect.value,
        stops: [],
        returnToStart: returnCheckbox.checked
    };
    if (!returnCheckbox.checked) {
        request.end = endSelect.value;
    }

    var stopSelects = stopsContainer.querySelectorAll('select');
    for (var i = 0; i < stopSelects.length; i++) {
        if (stopSelects[i].value !== '') {
            request.stops.push(stopSelects[i].value);
        }
    }

    return request;
}

function planRoute(request) {
    lastPlan = null;
    planButton.disabled = true;
    showStatus('Planning your route…', false);
    showLoading();

    fetch('/api/route', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
    })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (!data.success) {
                throw serverError(data.message);
            }
            showStatus('', false);
            renderPlan(data, request.returnToStart);
            savePlanInHash(request);
        })
        .catch(function (error) {
            // Messages the server sent are shown as sent. Anything else came from
            // the network or a broken response, so say that in plain words.
            clearResults();
            var message = error.fromServer
                ? error.message
                : 'Could not reach the server. Is it running?';
            showStatus(message, true);
        })
        .then(function () {
            planButton.disabled = false;
        });
}

function showLoading() {
    emptyState.hidden = true;
    routeSummary.hidden = true;
    stepsSection.hidden = true;
    copyButton.hidden = true;
    loadingCard.hidden = false;
    clearRoute();
}

function renderPlan(data, roundTrip) {
    lastPlan = { data: data, roundTrip: roundTrip };
    closePopover();

    renderSaved(data);

    summaryText.textContent = summarySentence(data);
    renderArrival(data);

    comparisonTable.textContent = '';
    comparisonTable.appendChild(buildComparison(data));

    routeSteps.textContent = '';
    routeSteps.appendChild(buildSteps(data.legs));

    loadingCard.hidden = true;
    emptyState.hidden = true;
    routeSummary.hidden = false;
    stepsSection.hidden = false;
    copyButton.hidden = false;

    // Restart the rise animation even when a second plan replaces the first.
    routeSummary.classList.remove('is-revealed');
    stepsSection.classList.remove('is-revealed');
    void routeSummary.offsetWidth;
    routeSummary.classList.add('is-revealed');
    stepsSection.classList.add('is-revealed');

    drawRoute(data);
}

// One very large serif number, the same way the theme shows its temperature.
function renderSaved(data) {
    if (data.minutesSaved > 0) {
        savedNumber.textContent = data.minutesSaved.toFixed(1);
        savedNumber.classList.remove('is-word');
        savedLabel.textContent = 'minutes saved';
    } else {
        savedNumber.textContent = data.guaranteedBest
            ? 'Already the fastest'
            : 'No quicker order found';
        savedNumber.classList.add('is-word');
        savedLabel.textContent = '';
    }
}

// With more than 7 stops the planner cannot try every order, so it never claims
// the order it found is the best one. The sentence is shared with the copy text.
function summarySentence(data) {
    var summary;
    if (data.minutesSaved > 0) {
        summary = 'You save ' + data.minutesSaved.toFixed(1) + ' min.';
    } else if (data.guaranteedBest) {
        summary = 'Your order was already the fastest.';
    } else {
        summary = 'Your order was the fastest we found.';
    }
    if (!data.guaranteedBest) {
        summary = summary + ' With this many stops the order is a good guess,'
            + ' not guaranteed to be the best.';
    }
    return summary;
}

// The two stops orders as thin timelines of dots, not a table.
function buildComparison(data) {
    var wrapper = document.createElement('div');
    wrapper.className = 'comparison';
    wrapper.appendChild(buildStrip('Your order', data.yourOrder, false));
    var optimizedLabel = data.guaranteedBest ? 'Optimized order' : 'Suggested order';
    wrapper.appendChild(buildStrip(optimizedLabel, data.optimized, true));
    return wrapper;
}

function buildStrip(label, itinerary, isOptimized) {
    var strip = document.createElement('div');
    strip.className = isOptimized ? 'strip is-optimized' : 'strip';

    var head = document.createElement('div');
    head.className = 'strip-head';

    var name = document.createElement('span');
    name.className = 'strip-label';
    name.textContent = label;
    head.appendChild(name);

    var stats = document.createElement('span');
    stats.className = 'strip-stats';
    stats.appendChild(statPair(itinerary.distanceKm, 'km'));
    stats.appendChild(statPair(itinerary.timeMinutes, 'min'));
    head.appendChild(stats);

    strip.appendChild(head);

    var dots = document.createElement('ol');
    dots.className = 'strip-dots';
    for (var i = 0; i < itinerary.stops.length; i++) {
        var item = document.createElement('li');
        var stop = document.createElement('span');
        stop.textContent = itinerary.stops[i];
        item.appendChild(stop);
        dots.appendChild(item);
    }
    strip.appendChild(dots);

    return strip;
}

function statPair(value, unit) {
    var span = document.createElement('span');
    var number = document.createElement('b');
    number.textContent = value.toFixed(1);
    span.appendChild(number);
    span.appendChild(document.createTextNode(' ' + unit));
    return span;
}

function buildSteps(legs) {
    var list = document.createElement('ol');
    list.className = 'leg-list';

    for (var i = 0; i < legs.length; i++) {
        var leg = legs[i];

        var item = document.createElement('li');
        item.className = 'leg';
        item.setAttribute('data-leg', String(i));

        var number = document.createElement('span');
        number.className = 'leg-number';
        number.textContent = String(i + 1);
        item.appendChild(number);

        var title = document.createElement('p');
        title.className = 'leg-title';
        title.appendChild(document.createTextNode(leg.from));
        var arrow = document.createElement('span');
        arrow.className = 'arrow';
        arrow.textContent = '\u2192';
        title.appendChild(arrow);
        title.appendChild(document.createTextNode(leg.to));
        item.appendChild(title);

        var numbers = document.createElement('p');
        numbers.className = 'leg-numbers';
        numbers.appendChild(statPair(leg.distanceKm, 'km'));
        numbers.appendChild(statPair(leg.timeMinutes, 'min'));
        numbers.appendChild(speedPair(leg));
        item.appendChild(numbers);

        if (leg.path.length > 2) {
            var path = document.createElement('p');
            path.className = 'leg-path';
            path.textContent = 'via ' + leg.path.slice(1, -1).join(', ');
            item.appendChild(path);
        }

        // The planner reorders stops by first visit, so this is usually empty.
        // It still shows when a later leg crosses a stop that was already visited.
        if (leg.passesThrough && leg.passesThrough.length > 0) {
            var through = document.createElement('p');
            through.className = 'leg-through';
            through.textContent = 'Passes through ' + leg.passesThrough.join(', ');
            item.appendChild(through);
        }

        item.appendChild(buildTrafficBar(leg.segments));

        item.addEventListener('mouseenter', function () {
            focusLeg(Number(this.getAttribute('data-leg')));
        });
        item.addEventListener('mouseleave', function () {
            focusLeg(-1);
        });

        list.appendChild(item);
    }

    return list;
}

function speedPair(leg) {
    var span = document.createElement('span');
    span.appendChild(document.createTextNode('about '));
    var number = document.createElement('b');
    number.textContent = String(averageSpeed(leg));
    span.appendChild(number);
    span.appendChild(document.createTextNode(' km/h'));
    return span;
}

// A thin bar showing how much of the leg ran through each traffic level.
function buildTrafficBar(segments) {
    var bar = document.createElement('div');
    bar.className = 'traffic-bar';

    var kmByLevel = { LOW: 0, MEDIUM: 0, HIGH: 0 };
    var total = 0;
    for (var i = 0; i < segments.length; i++) {
        var level = segments[i].traffic;
        if (kmByLevel[level] === undefined) {
            kmByLevel[level] = 0;
        }
        kmByLevel[level] = kmByLevel[level] + segments[i].distanceKm;
        total = total + segments[i].distanceKm;
    }

    var parts = [];
    var levels = ['LOW', 'MEDIUM', 'HIGH'];
    var labels = { LOW: 'quiet', MEDIUM: 'busy', HIGH: 'jammed' };
    for (var j = 0; j < levels.length; j++) {
        var km = kmByLevel[levels[j]];
        if (km <= 0 || total <= 0) {
            continue;
        }
        var piece = document.createElement('span');
        piece.className = levels[j].toLowerCase();
        piece.style.width = (km / total * 100).toFixed(2) + '%';
        bar.appendChild(piece);
        parts.push(labels[levels[j]] + ' ' + km.toFixed(1) + ' km');
    }

    bar.setAttribute('aria-label', parts.join(', '));
    bar.setAttribute('role', 'img');

    var key = document.createElement('p');
    key.className = 'traffic-key';
    key.textContent = parts.join(', ');

    var wrap = document.createElement('div');
    wrap.appendChild(bar);
    wrap.appendChild(key);
    return wrap;
}

// Display only: the speed comes from rounded API numbers, so it is approximate.
function averageSpeed(leg) {
    if (leg.timeMinutes <= 0) {
        return 0;
    }
    return Math.round(leg.distanceKm / leg.timeMinutes * 60);
}

copyButton.addEventListener('click', function () {
    if (!lastPlan) {
        return;
    }
    var text = buildPlanText(lastPlan);
    if (!navigator.clipboard || !navigator.clipboard.writeText) {
        showStatus('Could not copy the plan.', true);
        return;
    }
    navigator.clipboard.writeText(text)
        .then(function () {
            showStatus('Plan copied.', false);
        })
        .catch(function () {
            showStatus('Could not copy the plan.', true);
        });
});

// Plain lines of text. Every value here came from the API or from the page, and
// the whole string is handed to the clipboard as text, never as markup.
function buildPlanText(plan) {
    var data = plan.data;
    var lines = [];
    if (plan.roundTrip) {
        lines.push('Round trip from ' + data.yourOrder.stops[0]);
    } else {
        lines.push('Start: ' + data.yourOrder.stops[0]);
        lines.push('Destination: '
            + data.yourOrder.stops[data.yourOrder.stops.length - 1]);
    }
    lines.push('Your order: ' + itineraryText(data.yourOrder));
    var optimizedLabel = data.guaranteedBest ? 'Optimized order' : 'Suggested order';
    lines.push(optimizedLabel + ': ' + itineraryText(data.optimized));
    var arrival = arrivalText(data.optimized.timeMinutes);
    if (arrival !== '') {
        lines.push(arrival);
    }
    lines.push(summarySentence(data));
    lines.push('');
    lines.push('Legs:');
    for (var i = 0; i < data.legs.length; i++) {
        var leg = data.legs[i];
        lines.push(leg.from + ' -> ' + leg.to + ': '
            + leg.distanceKm.toFixed(1) + ' km, '
            + leg.timeMinutes.toFixed(1) + ' min, about '
            + averageSpeed(leg) + ' km/h');
    }
    return lines.join('\n');
}

function itineraryText(itinerary) {
    return itinerary.stops.join(' -> ') + ' (' + itinerary.distanceKm.toFixed(1)
        + ' km, ' + itinerary.timeMinutes.toFixed(1) + ' min)';
}

// The time input is read as plain hours and minutes, so the page does not need
// the browser's own date parsing.
function departureMinutes() {
    var parts = (departTime.value || '').split(':');
    if (parts.length !== 2) {
        return null;
    }
    var hours = Number(parts[0]);
    var minutes = Number(parts[1]);
    if (isNaN(hours) || isNaN(minutes)) {
        return null;
    }
    return hours * 60 + minutes;
}

// Past midnight the clock wraps to 00:xx instead of showing 24:xx.
function arrivalText(timeMinutes) {
    var departure = departureMinutes();
    if (departure === null) {
        return '';
    }
    var total = (departure + Math.round(timeMinutes)) % 1440;
    if (total < 0) {
        total = total + 1440;
    }
    return 'You arrive at ' + padTime(Math.floor(total / 60)) + ':'
        + padTime(total % 60) + '.';
}

function padTime(number) {
    return number < 10 ? '0' + number : String(number);
}

function renderArrival(data) {
    var text = arrivalText(data.optimized.timeMinutes);
    arrivalLine.textContent = text;
    arrivalLine.hidden = text === '';
}

departTime.addEventListener('change', function () {
    if (lastPlan) {
        renderArrival(lastPlan.data);
    }
});

var EMERGENCY_WORDS = {
    hospital: 'hospital',
    fire: 'fire station',
    police: 'police station'
};

// The page only sends the chosen start; the Java side picks the closest place.
function findNearest(type) {
    var from = startSelect.value;
    if (from === '') {
        showStatus('Pick a start location first.', true);
        return;
    }
    showStatus('Looking for the nearest ' + EMERGENCY_WORDS[type] + '…', false);

    fetch('/api/nearest?from=' + encodeURIComponent(from)
            + '&type=' + encodeURIComponent(type))
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (!data.success) {
                throw serverError(data.message);
            }
            clearResults();
            emptyState.hidden = true;
            lastPlan = null;
            closePopover();
            var place = data.path[data.path.length - 1];
            showStatus('Nearest ' + EMERGENCY_WORDS[type] + ': ' + place + ', '
                + data.distanceKm.toFixed(1) + ' km, '
                + data.timeMinutes.toFixed(1) + ' min.', false);
            // The plan's own drawing code, with the returned path as one leg.
            drawRoute({
                legs: [{
                    from: data.path[0],
                    to: place,
                    path: data.path,
                    distanceKm: data.distanceKm,
                    timeMinutes: data.timeMinutes,
                    segments: []
                }],
                originalLegs: [],
                optimized: { stops: [data.path[0], place] }
            });
        })
        .catch(function (error) {
            clearResults();
            showStatus(error.fromServer
                ? error.message
                : 'Could not reach the server. Is it running?', true);
        });
}

var emergencyButtons = document.querySelectorAll('[data-emergency]');
for (var emergencyIndex = 0; emergencyIndex < emergencyButtons.length; emergencyIndex++) {
    emergencyButtons[emergencyIndex].addEventListener('click', function () {
        findNearest(this.getAttribute('data-emergency'));
    });
}

// The request is saved after a plan, so the URL can be sent to someone else.
function savePlanInHash(request) {
    try {
        location.hash = encodeURIComponent(JSON.stringify(request));
    } catch (error) {
        // Sharing is a bonus; the plan still works without it.
    }
}

// A shared link fills the form once and plans straight away. Anything that is
// not a sensible request is ignored, so a broken link shows the normal page.
function restorePlanFromHash() {
    if (location.hash.length <= 1) {
        return;
    }
    var request;
    try {
        request = JSON.parse(decodeURIComponent(location.hash.slice(1)));
    } catch (error) {
        return;
    }
    if (!request || typeof request.start !== 'string'
            || locations.indexOf(request.start) === -1) {
        return;
    }
    if (request.stops !== undefined && !Array.isArray(request.stops)) {
        return;
    }

    returnCheckbox.checked = !!request.returnToStart;
    endSelect.disabled = returnCheckbox.checked;
    swapButton.disabled = returnCheckbox.checked;
    startSelect.value = request.start;
    if (!returnCheckbox.checked && typeof request.end === 'string'
            && locations.indexOf(request.end) !== -1) {
        endSelect.value = request.end;
    }

    removeAllStops();
    var stops = request.stops || [];
    for (var i = 0; i < stops.length; i++) {
        if (locations.indexOf(stops[i]) !== -1) {
            addStopRow(stops[i]);
        }
    }
    planRoute(request);
}

// Wraps a message that really came from the server, so the catch block knows
// the difference between "the server said no" and "the server is not there".
function serverError(message) {
    var error = new Error(message);
    error.fromServer = true;
    return error;
}

// An old plan must never sit under a new error, so empty both result sections
// and hide them again.
function clearResults() {
    comparisonTable.textContent = '';
    summaryText.textContent = '';
    arrivalLine.textContent = '';
    arrivalLine.hidden = true;
    routeSteps.textContent = '';
    savedNumber.textContent = '0';
    savedLabel.textContent = 'minutes saved';
    routeSummary.hidden = true;
    stepsSection.hidden = true;
    loadingCard.hidden = true;
    copyButton.hidden = true;
    emptyState.hidden = false;
    routeSummary.classList.remove('is-revealed');
    stepsSection.classList.remove('is-revealed');
    clearRoute();
}

function showStatus(message, isError) {
    statusMessage.textContent = message;
    statusMessage.className = isError ? 'status error' : 'status';
}

/* --- the map ------------------------------------------------------------- */

var MAP_WIDTH = 640;
var MAP_HEIGHT = 730;
var MAP_MIN_ZOOM = 1;
var MAP_MAX_ZOOM = 3;

// First choice for each label, scored by a throwaway layout script against the
// roads and the other labels. The page checks the choice again after drawing
// and falls back to another side when the measured text does not fit.
var LABEL_SIDES = {
    'Art Museum': 'left',
    'Bus Station': 'below',
    'Central Market': 'right',
    'City Airport': 'below',
    'City Hospital': 'below',
    'City Stadium': 'above',
    'College Gate': 'left',
    'Court House': 'right',
    'Fire Station': 'right',
    'Grand Mall': 'right',
    'Lakeview Zoo': 'right',
    'North Police Station': 'below',
    'Old Town Square': 'right',
    'Public Library': 'left',
    'Railway Station': 'right',
    'Riverside Park': 'right',
    'Tech Park': 'below',
    'Wholesale Market': 'above'
};

var TRAFFIC_WORD = { LOW: 'quiet', MEDIUM: 'busy', HIGH: 'jammed' };

var mapRoads = [];
var mapLabels = {};
var mapNodes = {};
var mapLayers = {};
var mapView = { zoom: 1, cx: MAP_WIDTH / 2, cy: MAP_HEIGHT / 2 };
var mapPx = 1;
var mapUnit = 1;
var mapNarrow = false;
var mapFontPx = 14;
var hoveredPlace = '';
var routeStops = [];
var badgeItems = [];
var viewFrame = 0;
var dragState = null;
var activePointers = {};
var pinchState = null;

function svgElement(name, attributes) {
    var element = document.createElementNS(SVG_NS, name);
    for (var key in attributes) {
        if (Object.prototype.hasOwnProperty.call(attributes, key)) {
            element.setAttribute(key, attributes[key]);
        }
    }
    return element;
}

function reducedMotion() {
    return !!(window.matchMedia
        && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
}

function clamp(value, low, high) {
    return Math.max(low, Math.min(high, value));
}

function buildMap(city) {
    mapCity = city;
    cityMap.textContent = '';
    mapPoints = {};
    mapLabels = {};
    mapNodes = {};
    mapRoads = [];
    badgeItems = [];
    routeStops = [];
    mapLayers = {};

    for (var i = 0; i < city.locations.length; i++) {
        mapPoints[city.locations[i].name] = city.locations[i];
    }

    var styles = getComputedStyle(document.documentElement);
    var gradient = svgElement('linearGradient', {
        id: 'routeGradient',
        gradientUnits: 'userSpaceOnUse',
        x1: '0', y1: '0', x2: String(MAP_WIDTH), y2: String(MAP_HEIGHT)
    });
    var stopColors = ['--ember', '--accent', '--amber'];
    var stopOffsets = ['0', '0.55', '1'];
    for (var s = 0; s < stopColors.length; s++) {
        gradient.appendChild(svgElement('stop', {
            offset: stopOffsets[s],
            'stop-color': styles.getPropertyValue(stopColors[s]).trim()
        }));
    }

    var defs = svgElement('defs', {});
    defs.appendChild(gradient);
    cityMap.appendChild(defs);

    // Bottom to top: roads, road hit areas, the dimming veil, the original
    // order, the optimized route, places, labels, numbered badges.
    mapLayers.roads = svgElement('g', { 'class': 'map-roads' });
    mapLayers.hits = svgElement('g', { 'class': 'map-road-hits' });
    mapLayers.dim = svgElement('rect', {
        'class': 'map-dim', x: '0', y: '0',
        width: String(MAP_WIDTH), height: String(MAP_HEIGHT)
    });
    mapLayers.original = svgElement('g', { 'class': 'map-original-layer' });
    mapLayers.route = svgElement('g', { 'class': 'map-route-layer' });
    mapLayers.nodes = svgElement('g', { 'class': 'map-node-layer' });
    mapLayers.labels = svgElement('g', { 'class': 'map-label-layer' });
    mapLayers.badges = svgElement('g', { 'class': 'map-badge-layer' });

    for (var r = 0; r < city.roads.length; r++) {
        var road = city.roads[r];
        var from = mapPoints[road.from];
        var to = mapPoints[road.to];
        if (!from || !to) {
            continue;
        }
        var level = road.traffic.toLowerCase();
        var line = svgElement('line', {
            x1: from.x, y1: from.y, x2: to.x, y2: to.y,
            'class': 'map-road ' + level
        });
        mapLayers.roads.appendChild(line);

        var hit = svgElement('line', {
            x1: from.x, y1: from.y, x2: to.x, y2: to.y,
            'class': 'map-road-hit'
        });
        hit.setAttribute('data-from', road.from);
        hit.setAttribute('data-to', road.to);
        hit.setAttribute('data-distance', String(road.distanceKm));
        hit.setAttribute('data-traffic', road.traffic);
        attachRoadTooltip(hit, road);
        mapLayers.hits.appendChild(hit);

        mapRoads.push({
            from: road.from, to: road.to,
            distanceKm: road.distanceKm, traffic: road.traffic
        });
    }

    cityMap.appendChild(mapLayers.roads);
    cityMap.appendChild(mapLayers.hits);
    cityMap.appendChild(mapLayers.dim);
    cityMap.appendChild(mapLayers.original);
    cityMap.appendChild(mapLayers.route);

    for (var n = 0; n < city.locations.length; n++) {
        var place = city.locations[n];
        mapLayers.nodes.appendChild(buildNode(place));
    }
    cityMap.appendChild(mapLayers.nodes);

    for (var l = 0; l < city.locations.length; l++) {
        var labelPlace = city.locations[l];
        var text = svgElement('text', {
            'class': 'map-label',
            'text-anchor': 'middle',
            'data-name': labelPlace.name
        });
        text.textContent = labelPlace.name;
        mapLabels[labelPlace.name] = text;
        mapLayers.labels.appendChild(text);
    }
    cityMap.appendChild(mapLayers.labels);
    cityMap.appendChild(mapLayers.badges);

    mapNarrow = window.innerWidth < 600;
    mapView = { zoom: 1, cx: MAP_WIDTH / 2, cy: MAP_HEIGHT / 2 };
    measureMap();
    applyView(false);
    placeAllLabels(true);
    refreshLabelVisibility();

    // The first measurement can happen before the web fonts arrive, when the
    // text is still narrower than it will be. Score the sides again afterwards.
    if (document.fonts && document.fonts.ready) {
        document.fonts.ready.then(function () {
            placeAllLabels(true);
            refreshLabelVisibility();
        });
    }
}

function buildNode(place) {
    var group = svgElement('g', {
        'class': 'map-node',
        transform: 'translate(' + place.x + ',' + place.y + ')'
    });
    group.setAttribute('data-place', place.name);
    group.setAttribute('role', 'button');
    group.setAttribute('tabindex', '0');
    group.setAttribute('aria-label', place.name + '. Open start, destination and stop actions.');

    group.appendChild(svgElement('circle', { 'class': 'node-dot', cx: '0', cy: '0' }));
    group.appendChild(svgElement('circle', { 'class': 'node-ring', cx: '0', cy: '0' }));
    group.appendChild(svgElement('circle', { 'class': 'node-hit', cx: '0', cy: '0' }));

    var title = document.createElementNS(SVG_NS, 'title');
    title.textContent = place.name;
    group.appendChild(title);

    group.addEventListener('click', function (event) {
        openPopover(this.getAttribute('data-place'), this);
        event.stopPropagation();
    });
    group.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            openPopover(this.getAttribute('data-place'), this);
        }
    });
    group.addEventListener('mouseenter', function () {
        hoveredPlace = this.getAttribute('data-place');
        refreshLabelVisibility();
    });
    group.addEventListener('mouseleave', function () {
        hoveredPlace = '';
        refreshLabelVisibility();
    });
    group.addEventListener('focus', function () {
        hoveredPlace = this.getAttribute('data-place');
        refreshLabelVisibility();
    });
    group.addEventListener('blur', function () {
        hoveredPlace = '';
        refreshLabelVisibility();
    });

    mapNodes[place.name] = group;
    return group;
}

/* --- map sizes, labels and the view ------------------------------------- */

// The SVG is drawn close to 1:1, but the text size is computed from the real
// rendered width so 14px on screen stays 14px in a wider or narrower card.
function measureMap() {
    var rect = cityMap.getBoundingClientRect();
    mapNarrow = window.innerWidth < 600;
    mapFontPx = mapNarrow ? 13 : 14;
    mapUnit = MAP_WIDTH / (rect.width || MAP_WIDTH);
    mapPx = mapUnit / mapView.zoom;
}

function applySizing() {
    if (!mapLayers.roads) {
        return;
    }
    var px = mapPx;
    var roadLines = mapLayers.roads.querySelectorAll('.map-road');
    for (var i = 0; i < roadLines.length; i++) {
        roadLines[i].setAttribute('stroke-width', String(2 * px));
        if (roadLines[i].classList.contains('high')) {
            roadLines[i].setAttribute('stroke-dasharray', (6 * px) + ' ' + (4 * px));
        }
    }

    var hits = mapLayers.hits.querySelectorAll('.map-road-hit');
    for (var h = 0; h < hits.length; h++) {
        hits[h].setAttribute('stroke-width', String(12 * px));
    }

    var dots = mapLayers.nodes.querySelectorAll('.node-dot');
    for (var d = 0; d < dots.length; d++) {
        dots[d].setAttribute('r', String(7 * px));
    }
    var rings = mapLayers.nodes.querySelectorAll('.node-ring');
    for (var g = 0; g < rings.length; g++) {
        rings[g].setAttribute('r', String(9 * px));
        rings[g].setAttribute('stroke-width', String(2 * px));
    }
    var hitDots = mapLayers.nodes.querySelectorAll('.node-hit');
    for (var c = 0; c < hitDots.length; c++) {
        hitDots[c].setAttribute('r', String(18 * px));
    }

    var labels = mapLayers.labels.querySelectorAll('.map-label');
    for (var t = 0; t < labels.length; t++) {
        labels[t].setAttribute('font-size', String(mapFontPx * px));
        labels[t].setAttribute('stroke-width', String(4.5 * px));
    }

    var routeWidth = 5 * px;
    var routes = mapLayers.route.querySelectorAll('.map-route');
    for (var p = 0; p < routes.length; p++) {
        var focused = routes[p].classList.contains('is-focused');
        routes[p].setAttribute('stroke-width', String(focused ? routeWidth * 1.45 : routeWidth));
    }
    var original = mapLayers.original.querySelector('.map-original');
    if (original) {
        original.setAttribute('stroke-width', String(2 * px));
        original.setAttribute('stroke-dasharray', (6 * px) + ' ' + (5 * px));
    }

    for (var b = 0; b < badgeItems.length; b++) {
        positionBadge(badgeItems[b]);
    }
}

// The label sits 10px clear of the dot; the exact baseline is corrected from
// the measured box, so the gap does not depend on the font's metrics.
function setLabelPosition(label, name, side, shift, gapPx) {
    var point = mapPoints[name];
    var gap = (gapPx || 17) * mapPx;
    var anchor = 'middle';
    var x = point.x;
    var y = point.y;
    if (side === 'above') {
        y = point.y - gap;
        x = point.x + (shift || 0);
    } else if (side === 'below') {
        y = point.y + gap;
        x = point.x + (shift || 0);
    } else if (side === 'left') {
        anchor = 'end';
        x = point.x - gap;
        y = point.y + (shift || 0);
    } else {
        anchor = 'start';
        x = point.x + gap;
        y = point.y + (shift || 0);
    }
    label.setAttribute('text-anchor', anchor);
    label.setAttribute('x', String(x));
    label.setAttribute('y', String(y));

    // SVGRect has x, y, width and height only, so the edges are computed here.
    var box = label.getBBox();
    var left = box.x;
    var top = box.y;
    var right = box.x + box.width;
    var bottom = box.y + box.height;
    var dx = 0;
    var dy = 0;
    if (side === 'above') {
        dy = (point.y - gap) - bottom;
    } else if (side === 'below') {
        dy = (point.y + gap) - top;
    } else if (side === 'left') {
        dx = (point.x - gap) - right;
    } else {
        dx = (point.x + gap) - left;
    }
    // A place on the map's edge must not push its label out of the drawing,
    // so the measured box is pushed back inside before it is drawn.
    var edge = 3 * mapUnit;
    var shiftX = 0;
    var shiftY = 0;
    if (left + dx < edge) {
        shiftX = edge - (left + dx);
    }
    if (right + dx + shiftX > MAP_WIDTH - edge) {
        shiftX = MAP_WIDTH - edge - (right + dx);
    }
    if (top + dy < edge) {
        shiftY = edge - (top + dy);
    }
    if (bottom + dy + shiftY > MAP_HEIGHT - edge) {
        shiftY = MAP_HEIGHT - edge - (bottom + dy);
    }
    dx += shiftX;
    dy += shiftY;
    label.setAttribute('x', String(x + dx));
    label.setAttribute('y', String(y + dy));

    return {
        left: left + dx, right: right + dx,
        top: top + dy, bottom: bottom + dy
    };
}

function boxesOverlap(a, b, pad) {
    return a.left < b.right + pad && a.right + pad > b.left
        && a.top < b.bottom + pad && a.bottom + pad > b.top;
}

function pointToBoxDistance(point, box) {
    var dx = Math.max(box.left - point.x, 0, point.x - box.right);
    var dy = Math.max(box.top - point.y, 0, point.y - box.bottom);
    return Math.sqrt(dx * dx + dy * dy);
}

function pointToSegmentDistance(point, start, end) {
    var dx = end.x - start.x;
    var dy = end.y - start.y;
    var length = dx * dx + dy * dy || 1;
    var t = ((point.x - start.x) * dx + (point.y - start.y) * dy) / length;
    t = Math.max(0, Math.min(1, t));
    return Math.sqrt(Math.pow(point.x - (start.x + t * dx), 2)
        + Math.pow(point.y - (start.y + t * dy), 2));
}

// A cheap segment-to-box distance: walk the segment and keep the closest point.
function segmentToBoxDistance(start, end, box) {
    var best = Infinity;
    for (var i = 0; i <= 16; i++) {
        var t = i / 16;
        best = Math.min(best, pointToBoxDistance({
            x: start.x + (end.x - start.x) * t,
            y: start.y + (end.y - start.y) * t
        }, box));
    }
    return best;
}

function labelSideCost(box, name) {
    var cost = 0;
    if (box.left < 3 * mapUnit || box.right > MAP_WIDTH - 3 * mapUnit
            || box.top < 3 * mapUnit || box.bottom > MAP_HEIGHT - 3 * mapUnit) {
        cost += 4000;
    }
    // The zoom buttons float over the top-right corner; no label may hide there.
    var controls = {
        left: MAP_WIDTH - 54 * mapUnit,
        top: 0,
        right: MAP_WIDTH,
        bottom: 158 * mapUnit
    };
    if (boxesOverlap(box, controls, 2 * mapUnit)) {
        cost += 6000;
    }
    for (var other in mapNodes) {
        if (!Object.prototype.hasOwnProperty.call(mapNodes, other) || other === name) {
            continue;
        }
        var point = mapPoints[other];
        if (pointToBoxDistance(point, box) < 9 * mapPx) {
            cost += 3000;
        }
    }
    for (var r = 0; r < mapRoads.length; r++) {
        var road = mapRoads[r];
        if (road.from === name || road.to === name) {
            continue;
        }
        var from = mapPoints[road.from];
        var to = mapPoints[road.to];
        var distance = segmentToBoxDistance(from, to, box);
        if (distance < 4 * mapPx) {
            cost += 1800 + (4 * mapPx - distance) * 400;
        }
    }
    return cost;
}

function placeAllLabels(rebuild) {
    var names = [];
    for (var name in mapNodes) {
        if (Object.prototype.hasOwnProperty.call(mapNodes, name)) {
            names.push(name);
        }
    }
    // Long names first: they are the hard ones to fit.
    names.sort(function (a, b) {
        return mapLabels[b].getComputedTextLength() - mapLabels[a].getComputedTextLength();
    });

    var placed = [];
    var sides = ['above', 'below', 'left', 'right'];
    for (var i = 0; i < names.length; i++) {
        var name = names[i];
        var label = mapLabels[name];
        var preferred = LABEL_SIDES[name] || 'above';
        var gaps = [17, 36, 55];
        var order = [{ side: preferred, shift: 0, gap: 17 }];
        var sideShift = 10 * mapPx;
        var labelShift = label.getComputedTextLength() * 0.4;
        for (var g = 0; g < gaps.length; g++) {
            for (var s = 0; s < sides.length; s++) {
                var side = sides[s];
                var shifts = (side === 'above' || side === 'below')
                    ? [0, -labelShift, labelShift]
                    : [0, -sideShift, sideShift];
                for (var v = 0; v < shifts.length; v++) {
                    if (side === preferred && shifts[v] === 0 && gaps[g] === 17) {
                        continue;
                    }
                    order.push({ side: side, shift: shifts[v], gap: gaps[g] });
                }
            }
        }

        var best = null;
        for (var o = 0; o < order.length; o++) {
            var box = setLabelPosition(label, name, order[o].side,
                order[o].shift, order[o].gap);
            var cost = labelSideCost(box, name) + (order[o].gap - 17) * 2;
            for (var p = 0; p < placed.length; p++) {
                if (boxesOverlap(box, placed[p], 3 * mapPx)) {
                    cost += 5000;
                }
            }
            if (!best || cost < best.cost) {
                best = {
                    side: order[o].side, shift: order[o].shift,
                    gap: order[o].gap, cost: cost, box: box
                };
            }
            if (cost === 0) {
                break;
            }
        }
        setLabelPosition(label, name, best.side, best.shift, best.gap);
        placed.push(best.box);
        label.setAttribute('data-side', best.side);
        label.setAttribute('data-shift', String(best.shift));
        label.setAttribute('data-gap', String(best.gap));
    }
}

// Zooming keeps the text at the same pixel size, so the labels keep their side
// and only the gap is rescaled.
function repositionLabels() {
    for (var name in mapLabels) {
        if (!Object.prototype.hasOwnProperty.call(mapLabels, name)) {
            continue;
        }
        setLabelPosition(mapLabels[name], name,
            mapLabels[name].getAttribute('data-side') || LABEL_SIDES[name] || 'above',
            Number(mapLabels[name].getAttribute('data-shift')) || 0,
            Number(mapLabels[name].getAttribute('data-gap')) || 17);
    }
}

function selectionNames() {
    var names = [];
    if (startSelect.value) {
        names.push(startSelect.value);
    }
    if (endSelect.value) {
        names.push(endSelect.value);
    }
    var stopSelects = stopsContainer.querySelectorAll('select');
    for (var i = 0; i < stopSelects.length; i++) {
        if (stopSelects[i].value) {
            names.push(stopSelects[i].value);
        }
    }
    return names;
}

// Under 600px a route would drown in labels, so only the places that matter
// keep theirs; the rest show the name when tapped or hovered.
function refreshLabelVisibility() {
    mapNarrow = window.innerWidth < 600;
    var hasRoute = cityMap.classList.contains('has-route');
    var keep = {};
    var selected = selectionNames();
    for (var i = 0; i < selected.length; i++) {
        keep[selected[i]] = true;
    }
    for (var r = 0; r < routeStops.length; r++) {
        keep[routeStops[r]] = true;
    }
    if (hoveredPlace) {
        keep[hoveredPlace] = true;
    }
    if (popoverPlace) {
        keep[popoverPlace] = true;
    }

    for (var name in mapLabels) {
        if (!Object.prototype.hasOwnProperty.call(mapLabels, name)) {
            continue;
        }
        var show = true;
        if (mapNarrow && hasRoute) {
            show = !!keep[name];
        }
        mapLabels[name].classList.toggle('is-hidden-label', !show);
    }
}

// A viewBox that is smaller than the drawing: the map is always at least 1x
// and the drawing is cropped, never a downscaled picture in a big empty card.
function applyView() {
    if (!mapLayers.roads) {
        return;
    }
    var width = MAP_WIDTH / mapView.zoom;
    var height = MAP_HEIGHT / mapView.zoom;
    mapView.cx = clamp(mapView.cx, width / 2, MAP_WIDTH - width / 2);
    mapView.cy = clamp(mapView.cy, height / 2, MAP_HEIGHT - height / 2);
    cityMap.setAttribute('viewBox',
        (mapView.cx - width / 2) + ' ' + (mapView.cy - height / 2) + ' '
        + width + ' ' + height);
    mapPx = mapUnit / mapView.zoom;
    applySizing();
    repositionLabels();
}

function animateView(target) {
    if (viewFrame) {
        cancelAnimationFrame(viewFrame);
        viewFrame = 0;
    }
    if (reducedMotion()) {
        mapView = { zoom: target.zoom, cx: target.cx, cy: target.cy };
        applyView();
        return;
    }
    var from = { zoom: mapView.zoom, cx: mapView.cx, cy: mapView.cy };
    var started = 0;
    var duration = 520;
    function step(now) {
        if (!started) {
            started = now;
        }
        var t = Math.min(1, (now - started) / duration);
        var eased = 1 - Math.pow(1 - t, 3);
        mapView.zoom = from.zoom + (target.zoom - from.zoom) * eased;
        mapView.cx = from.cx + (target.cx - from.cx) * eased;
        mapView.cy = from.cy + (target.cy - from.cy) * eased;
        applyView();
        if (t < 1) {
            viewFrame = requestAnimationFrame(step);
        } else {
            viewFrame = 0;
            // At the final zoom the labels are a different size in map units,
            // so their sides are scored once more here.
            placeAllLabels(true);
        }
    }
    viewFrame = requestAnimationFrame(step);
}

function resetView(animate) {
    var target = { zoom: 1, cx: MAP_WIDTH / 2, cy: MAP_HEIGHT / 2 };
    if (animate) {
        animateView(target);
    } else {
        mapView = target;
        applyView();
    }
}

// Fit the planned route with a little room around it, no closer than 1x.
function fitViewToRoute(data) {
    var points = legsToPoints(data.legs);
    if (points.length === 0) {
        return;
    }
    var minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
    for (var i = 0; i < points.length; i++) {
        minX = Math.min(minX, points[i].x);
        maxX = Math.max(maxX, points[i].x);
        minY = Math.min(minY, points[i].y);
        maxY = Math.max(maxY, points[i].y);
    }
    var pad = 58 * mapUnit;
    minX -= pad;
    maxX += pad;
    minY -= pad;
    maxY += pad;
    var zoom = Math.min(MAP_WIDTH / Math.max(1, maxX - minX),
        MAP_HEIGHT / Math.max(1, maxY - minY));
    zoom = clamp(zoom, MAP_MIN_ZOOM, MAP_MAX_ZOOM);
    animateView({
        zoom: zoom,
        cx: clamp((minX + maxX) / 2, MAP_WIDTH / (2 * zoom), MAP_WIDTH - MAP_WIDTH / (2 * zoom)),
        cy: clamp((minY + maxY) / 2, MAP_HEIGHT / (2 * zoom), MAP_HEIGHT - MAP_HEIGHT / (2 * zoom))
    });
}

function zoomAround(nextZoom, clientX, clientY) {
    var rect = cityMap.getBoundingClientRect();
    var relX = clamp((clientX - rect.left) / rect.width, 0, 1);
    var relY = clamp((clientY - rect.top) / rect.height, 0, 1);
    var oldWidth = MAP_WIDTH / mapView.zoom;
    var oldHeight = MAP_HEIGHT / mapView.zoom;
    var baseX = (mapView.cx - oldWidth / 2) + relX * oldWidth;
    var baseY = (mapView.cy - oldHeight / 2) + relY * oldHeight;

    var zoom = clamp(nextZoom, MAP_MIN_ZOOM, MAP_MAX_ZOOM);
    var width = MAP_WIDTH / zoom;
    var height = MAP_HEIGHT / zoom;
    mapView.zoom = zoom;
    mapView.cx = baseX - relX * width + width / 2;
    mapView.cy = baseY - relY * height + height / 2;
    applyView();
}

/* --- road tooltip -------------------------------------------------------- */

var roadTooltip = document.getElementById('road-tooltip');

function attachRoadTooltip(hit, road) {
    hit.addEventListener('mouseenter', function (event) {
        roadTooltip.textContent = road.from + ' to ' + road.to + ', '
            + road.distanceKm.toFixed(1) + ' km, ' + TRAFFIC_WORD[road.traffic];
        roadTooltip.hidden = false;
        moveRoadTooltip(event);
    });
    hit.addEventListener('mousemove', moveRoadTooltip);
    hit.addEventListener('mouseleave', function () {
        roadTooltip.hidden = true;
    });
}

function moveRoadTooltip(event) {
    if (roadTooltip.hidden) {
        return;
    }
    var rect = mapFrame.getBoundingClientRect();
    var left = event.clientX - rect.left + 14;
    var top = event.clientY - rect.top + 14;
    left = Math.max(8, Math.min(left, mapFrame.clientWidth - roadTooltip.offsetWidth - 8));
    top = Math.max(8, Math.min(top, mapFrame.clientHeight - roadTooltip.offsetHeight - 8));
    roadTooltip.style.left = left + 'px';
    roadTooltip.style.top = top + 'px';
}

/* --- pan, wheel zoom, pinch --------------------------------------------- */

function beginDrag(event) {
    if (event.pointerType === 'mouse' && event.button !== 0) {
        return;
    }
    if (event.target.closest
            && event.target.closest('.map-node, .map-popover, .map-controls')) {
        return;
    }
    activePointers[event.pointerId] = { x: event.clientX, y: event.clientY };
    if (Object.keys(activePointers).length === 2) {
        startPinch();
        dragState = null;
        return;
    }
    dragState = {
        id: event.pointerId,
        x: event.clientX,
        y: event.clientY,
        cx: mapView.cx,
        cy: mapView.cy,
        width: MAP_WIDTH / mapView.zoom
    };
    mapFrame.classList.add('is-panning');
    if (cityMap.setPointerCapture) {
        cityMap.setPointerCapture(event.pointerId);
    }
    if (viewFrame) {
        cancelAnimationFrame(viewFrame);
        viewFrame = 0;
    }
}

function moveDrag(event) {
    if (!activePointers[event.pointerId]) {
        return;
    }
    activePointers[event.pointerId] = { x: event.clientX, y: event.clientY };
    if (Object.keys(activePointers).length === 2) {
        movePinch();
        return;
    }
    if (!dragState || dragState.id !== event.pointerId) {
        return;
    }
    var rect = cityMap.getBoundingClientRect();
    var scale = dragState.width / rect.width;
    mapView.cx = dragState.cx - (event.clientX - dragState.x) * scale;
    mapView.cy = dragState.cy - (event.clientY - dragState.y) * scale;
    applyView();
}

function endDrag(event) {
    delete activePointers[event.pointerId];
    if (Object.keys(activePointers).length < 2) {
        pinchState = null;
    }
    if (dragState && dragState.id === event.pointerId) {
        dragState = null;
        mapFrame.classList.remove('is-panning');
    }
    if (Object.keys(activePointers).length === 1) {
        var id = Object.keys(activePointers)[0];
        var point = activePointers[id];
        dragState = {
            id: Number(id), x: point.x, y: point.y,
            cx: mapView.cx, cy: mapView.cy,
            width: MAP_WIDTH / mapView.zoom
        };
    }
}

function touchPair() {
    var ids = Object.keys(activePointers);
    if (ids.length !== 2) {
        return null;
    }
    var a = activePointers[ids[0]];
    var b = activePointers[ids[1]];
    return {
        distance: Math.hypot(a.x - b.x, a.y - b.y) || 1,
        midX: (a.x + b.x) / 2,
        midY: (a.y + b.y) / 2
    };
}

function startPinch() {
    var pair = touchPair();
    if (!pair) {
        return;
    }
    pinchState = {
        distance: pair.distance,
        midX: pair.midX,
        midY: pair.midY,
        zoom: mapView.zoom
    };
}

function movePinch() {
    var pair = touchPair();
    if (!pair || !pinchState) {
        return;
    }
    zoomAround(pinchState.zoom * (pair.distance / pinchState.distance),
        pair.midX, pair.midY);
    if (Math.abs(pair.midX - pinchState.midX) + Math.abs(pair.midY - pinchState.midY) > 0.5) {
        var rect = cityMap.getBoundingClientRect();
        var width = MAP_WIDTH / mapView.zoom;
        mapView.cx -= (pair.midX - pinchState.midX) * (width / rect.width);
        mapView.cy -= (pair.midY - pinchState.midY) * (MAP_HEIGHT / mapView.zoom / rect.height);
        applyView();
    }
    pinchState.midX = pair.midX;
    pinchState.midY = pair.midY;
    pinchState.distance = pair.distance;
    pinchState.zoom = mapView.zoom;
}

cityMap.addEventListener('pointerdown', beginDrag);
cityMap.addEventListener('pointermove', moveDrag);
cityMap.addEventListener('pointerup', endDrag);
cityMap.addEventListener('pointercancel', endDrag);

cityMap.addEventListener('wheel', function (event) {
    event.preventDefault();
    var factor = Math.exp(-event.deltaY * 0.0015);
    zoomAround(mapView.zoom * factor, event.clientX, event.clientY);
}, { passive: false });

document.getElementById('zoom-in').addEventListener('click', function () {
    zoomAround(mapView.zoom * 1.4, 
        mapFrame.getBoundingClientRect().left + mapFrame.clientWidth / 2,
        mapFrame.getBoundingClientRect().top + mapFrame.clientHeight / 2);
});
document.getElementById('zoom-out').addEventListener('click', function () {
    zoomAround(mapView.zoom / 1.4,
        mapFrame.getBoundingClientRect().left + mapFrame.clientWidth / 2,
        mapFrame.getBoundingClientRect().top + mapFrame.clientHeight / 2);
});
document.getElementById('zoom-reset').addEventListener('click', function () {
    resetView(true);
});

window.addEventListener('resize', function () {
    measureMap();
    applyView();
    placeAllLabels(false);
    refreshLabelVisibility();
});

/* --- the popover --------------------------------------------------------- */

function openPopover(place, node) {
    if (!mapPoints[place]) {
        return;
    }
    popoverPlace = place;
    popoverName.textContent = place;
    refreshLabelVisibility();

    var destinationAction = mapPopover.querySelector('[data-action="destination"]');
    destinationAction.disabled = returnCheckbox.checked;

    mapPopover.hidden = false;

    var frameRect = mapFrame.getBoundingClientRect();
    var nodeRect = node.getBoundingClientRect();
    var left = nodeRect.left - frameRect.left + nodeRect.width / 2 + 14;
    var top = nodeRect.top - frameRect.top - 12;

    left = Math.max(8, Math.min(left,
        mapFrame.clientWidth - mapPopover.offsetWidth - 8));
    top = Math.max(8, Math.min(top,
        mapFrame.clientHeight - mapPopover.offsetHeight - 8));

    mapPopover.style.left = left + 'px';
    mapPopover.style.top = top + 'px';
}

function closePopover() {
    mapPopover.hidden = true;
    popoverPlace = '';
    refreshLabelVisibility();
}

mapPopover.addEventListener('click', function (event) {
    var action = event.target.getAttribute('data-action');
    if (!action || !popoverPlace) {
        return;
    }
    if (action === 'start') {
        startSelect.value = popoverPlace;
    } else if (action === 'destination') {
        if (!returnCheckbox.checked) {
            endSelect.value = popoverPlace;
        }
    } else if (action === 'stop') {
        addStopRow(popoverPlace);
    }
    closePopover();
});

document.addEventListener('click', function (event) {
    if (mapPopover.hidden) {
        return;
    }
    if (mapPopover.contains(event.target)) {
        return;
    }
    if (event.target.closest && event.target.closest('.map-node')) {
        return;
    }
    closePopover();
});

document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && !mapPopover.hidden) {
        closePopover();
    }
});

/* --- drawing the plan ---------------------------------------------------- */

function pointOf(name) {
    var point = mapPoints[name];
    return point ? { x: point.x, y: point.y } : null;
}

function pathData(points) {
    var d = '';
    var last = null;
    for (var i = 0; i < points.length; i++) {
        var point = points[i];
        if (!point) {
            continue;
        }
        if (last && last.x === point.x && last.y === point.y) {
            continue;
        }
        d = d + (d === '' ? 'M' : 'L') + point.x + ' ' + point.y + ' ';
        last = point;
    }
    return d.trim();
}

function legsToPoints(legs) {
    var points = [];
    for (var i = 0; i < legs.length; i++) {
        for (var j = 0; j < legs[i].path.length; j++) {
            points.push(pointOf(legs[i].path[j]));
        }
    }
    return points;
}

function positionBadge(item) {
    var point = pointOf(item.place);
    if (!point) {
        return;
    }
    var side = mapLabels[item.place]
        ? (mapLabels[item.place].getAttribute('data-side') || 'above')
        : 'above';
    var offset = 26 * mapPx;
    var x = point.x;
    var y = point.y;
    if (side === 'above') {
        y = point.y + offset;
    } else if (side === 'below') {
        y = point.y - offset;
    } else if (side === 'left') {
        x = point.x + offset;
    } else {
        x = point.x - offset;
    }
    item.group.setAttribute('transform', 'translate(' + x + ',' + y + ')');
    item.disc.setAttribute('r', String(9 * mapPx));
    item.text.setAttribute('font-size', String(11 * mapPx));
    item.text.setAttribute('y', String(4 * mapPx));
}

function drawRoute(data) {
    if (!mapCity) {
        return;
    }
    clearRoute();

    var originalPoints = legsToPoints(data.originalLegs || []);
    var originalPath = svgElement('path', {
        'class': 'map-original',
        d: pathData(originalPoints)
    });
    mapLayers.original.appendChild(originalPath);

    // The gradient runs from the first point to the last, across the whole trip.
    var allPoints = legsToPoints(data.legs);
    var gradient = cityMap.querySelector('#routeGradient');
    if (gradient && allPoints.length > 0) {
        gradient.setAttribute('x1', allPoints[0].x);
        gradient.setAttribute('y1', allPoints[0].y);
        gradient.setAttribute('x2', allPoints[allPoints.length - 1].x);
        gradient.setAttribute('y2', allPoints[allPoints.length - 1].y);
    }

    var reduceMotion = reducedMotion();

    for (var i = 0; i < data.legs.length; i++) {
        var leg = data.legs[i];
        var path = svgElement('path', {
            'class': 'map-route',
            d: pathData(legsToPoints([leg])),
            'data-leg': String(i)
        });
        mapLayers.route.appendChild(path);

        var length = 0;
        try {
            length = path.getTotalLength();
        } catch (e) {
            length = 0;
        }
        if (length > 0 && !reduceMotion) {
            path.style.strokeDasharray = length;
            path.style.strokeDashoffset = length;
            (function (drawn) {
                requestAnimationFrame(function () {
                    drawn.style.transition = 'stroke-dashoffset 1000ms ease-out';
                    drawn.style.strokeDashoffset = 0;
                });
            }(path));
        }

        path.addEventListener('mouseenter', function () {
            focusLeg(Number(this.getAttribute('data-leg')));
        });
        path.addEventListener('mouseleave', function () {
            focusLeg(-1);
        });
    }

    routeStops = data.optimized.stops;
    for (var s = 0; s < routeStops.length; s++) {
        var place = routeStops[s];
        var point = pointOf(place);
        if (!point) {
            continue;
        }
        var group = svgElement('g', { 'class': 'stop-badge-group' });
        var disc = svgElement('circle', { 'class': 'stop-badge-disc' });
        var number = svgElement('text', {
            'class': 'stop-badge', 'text-anchor': 'middle'
        });
        number.textContent = String(s + 1);
        group.appendChild(disc);
        group.appendChild(number);
        mapLayers.badges.appendChild(group);
        var item = { place: place, group: group, disc: disc, text: number };
        badgeItems.push(item);
        positionBadge(item);
    }

    var start = routeStops[0];
    var end = routeStops[routeStops.length - 1];
    for (var name in mapNodes) {
        if (!Object.prototype.hasOwnProperty.call(mapNodes, name)) {
            continue;
        }
        var isStop = routeStops.indexOf(name) !== -1;
        mapNodes[name].classList.toggle('is-off-route', !isStop);
        mapNodes[name].classList.toggle('is-start', name === start);
        mapNodes[name].classList.toggle('is-end', name === end && name !== start);
        if (mapLabels[name]) {
            mapLabels[name].classList.toggle('is-on-route', isStop);
        }
    }

    cityMap.classList.add('has-route');
    applySizing();
    fitViewToRoute(data);
    focusLeg(-1);
    refreshLabelVisibility();
}

function clearRoute() {
    if (!mapLayers.route) {
        return;
    }
    mapLayers.original.textContent = '';
    mapLayers.route.textContent = '';
    mapLayers.badges.textContent = '';
    badgeItems = [];
    routeStops = [];
    cityMap.classList.remove('has-route');
    for (var name in mapNodes) {
        if (!Object.prototype.hasOwnProperty.call(mapNodes, name)) {
            continue;
        }
        mapNodes[name].classList.remove('is-off-route', 'is-start', 'is-end');
        if (mapLabels[name]) {
            mapLabels[name].classList.remove('is-on-route');
        }
    }
    refreshLabelVisibility();
}

// Hovering a leg row lights its line on the map, and the other way round.
function focusLeg(index) {
    var paths = mapLayers.route ? mapLayers.route.querySelectorAll('.map-route') : [];
    var rows = routeSteps.querySelectorAll('.leg');
    for (var i = 0; i < paths.length; i++) {
        var focused = Number(paths[i].getAttribute('data-leg')) === index;
        paths[i].classList.toggle('is-focused', focused);
        paths[i].setAttribute('stroke-width', String((focused ? 7.2 : 5) * mapPx));
    }
    for (var j = 0; j < rows.length; j++) {
        rows[j].classList.toggle('is-focused', Number(rows[j].getAttribute('data-leg')) === index);
    }
}

returnCheckbox.addEventListener('change', function () {
    if (!mapPopover.hidden) {
        var destinationAction = mapPopover.querySelector('[data-action="destination"]');
        destinationAction.disabled = returnCheckbox.checked;
    }
});
