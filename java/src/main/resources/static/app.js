/* ==========================================================================
   Fleet Telemetry Platform — Interactive Command Center JavaScript
   ========================================================================== */

// Initial Fleet of 8 realistic connected vehicles
const FLEET = [
    { vin: "1FA6CVNCNCXW927TC", oem: "OEM-A", model: "Sedan 2.0T", miles: false, odo: 14250.0, speed: 64.5, lat: 12.9716, lon: 77.5946, heading: 85, soc: null, ignition: true },
    { vin: "1HGCR2F83HA014920", oem: "OEM-B", model: "Crossover V6", miles: true, odo: 8940.5, speed: 52.0, lat: 12.9820, lon: 77.6050, heading: 140, soc: null, ignition: true },
    { vin: "5YJ3E1EB8KF314251", oem: "OEM-D", model: "EV Long Range", miles: false, odo: 21400.2, speed: 78.2, lat: 12.9650, lon: 77.5890, heading: 270, soc: 84.5, ignition: true },
    { vin: "3FA6P0H74HR129845", oem: "OEM-A", model: "Fleet Van HD", miles: false, odo: 45800.0, speed: 45.0, lat: 12.9900, lon: 77.6150, heading: 310, soc: null, ignition: true },
    { vin: "WAUZZZ8V7FA019284", oem: "OEM-C", model: "Executive Sport", miles: false, odo: 12300.8, speed: 92.4, lat: 12.9580, lon: 77.6200, heading: 45, soc: null, ignition: true },
    { vin: "JN1AZ4EH9DM492104", oem: "OEM-B", model: "Light Truck", miles: true, odo: 18500.0, speed: 58.0, lat: 12.9780, lon: 77.5750, heading: 180, soc: null, ignition: true },
    { vin: "WBA3A5C59DF391024", oem: "OEM-D", model: "Electric Delivery", miles: false, odo: 31200.4, speed: 68.0, lat: 12.9690, lon: 77.6320, heading: 90, soc: 62.0, ignition: true },
    { vin: "KL4CJ1SB6EB892014", oem: "OEM-C", model: "Compact City", miles: false, odo: 9840.1, speed: 38.5, lat: 12.9850, lon: 77.5820, heading: 220, soc: null, ignition: true }
];

let selectedVin = FLEET[0].vin;
let autoSimInterval = null;
let stats = {
    total: 0,
    accepted: 0,
    rejected: 0
};

// DOM Elements
const vehicleListContainer = document.getElementById("vehicle-list-container");
const btnToggleSim = document.getElementById("btn-toggle-sim");
const simBtnText = document.getElementById("sim-btn-text");
const btnSendBurst = document.getElementById("btn-send-burst");
const eventFeedList = document.getElementById("event-feed-list");
const btnClearFeed = document.getElementById("btn-clear-feed");
const ingestForm = document.getElementById("ingest-form");
const btnDupPing = document.getElementById("btn-dup-ping");
const radarBlips = document.getElementById("radar-blips");

// Telematics Display Elements
const activeVinTitle = document.getElementById("active-vin-title");
const activeOemBadge = document.getElementById("active-oem-badge");
const gaugeSpeed = document.getElementById("gauge-speed");
const speedBarFill = document.getElementById("speed-bar-fill");
const speedLimitStatus = document.getElementById("speed-limit-status");
const gaugeOdometer = document.getElementById("gauge-odometer");
const gaugeUnit = document.getElementById("gauge-unit");
const gaugeSoc = document.getElementById("gauge-soc");
const socBarFill = document.getElementById("soc-bar-fill");
const socStatus = document.getElementById("soc-status");
const compassNeedle = document.getElementById("compass-needle");
const gaugeHeading = document.getElementById("gauge-heading");
const gaugeIgnition = document.getElementById("gauge-ignition");
const gaugeCoords = document.getElementById("gauge-coords");

// Stats Elements
const statTotal = document.getElementById("stat-total-pings");
const statAccepted = document.getElementById("stat-accepted");
const statRejected = document.getElementById("stat-rejected");
const statLatency = document.getElementById("stat-latency");

// ==========================================================================
// Initialization
// ==========================================================================
function init() {
    renderVehicleList();
    renderRadarBlips();
    updateTelematicsDisplay();
    setupEventListeners();
    setupPresets();
    checkHealth();
    setInterval(checkHealth, 15000); // Check /health every 15s
}

// Render the Left-hand Vehicle Cards
function renderVehicleList() {
    vehicleListContainer.innerHTML = "";
    FLEET.forEach(v => {
        const card = document.createElement("div");
        card.className = `vehicle-card ${v.vin === selectedVin ? 'selected' : ''}`;
        card.id = `card-${v.vin}`;
        
        const badgeClass = v.oem === 'OEM-D' ? 'ev' : (v.miles ? 'miles' : '');
        const badgeText = v.oem === 'OEM-D' ? 'EV • ' + v.soc + '%' : (v.miles ? 'OEM-B (mi)' : v.oem);

        card.innerHTML = `
            <div class="vc-top">
                <span class="vc-vin">${v.vin.substring(0, 8)}...${v.vin.substring(13)}</span>
                <span class="vc-badge ${badgeClass}">${badgeText}</span>
            </div>
            <div class="vc-stats">
                <span>${v.model}</span>
                <span class="vc-speed ${v.speed > 100 ? 'overspeed' : ''}" id="speed-${v.vin}">
                    ${v.speed.toFixed(1)} km/h
                </span>
            </div>
        `;

        card.addEventListener("click", () => selectVehicle(v.vin));
        vehicleListContainer.appendChild(card);
    });
}

// Render the Radar blips
function renderRadarBlips() {
    radarBlips.innerHTML = "";
    FLEET.forEach((v, index) => {
        const blip = document.createElement("div");
        blip.className = `radar-blip ${v.vin === selectedVin ? 'active' : ''}`;
        blip.id = `blip-${v.vin}`;
        
        // Spread coordinates across radar circular space
        const angle = (index / FLEET.length) * 2 * Math.PI;
        const radius = 30 + ((index * 17) % 55); // percentage distance from center
        const x = 50 + radius * Math.cos(angle);
        const y = 50 + radius * Math.sin(angle);

        blip.style.left = `${x}%`;
        blip.style.top = `${y}%`;
        blip.title = `${v.vin} (${v.speed.toFixed(1)} km/h)`;
        blip.addEventListener("click", () => selectVehicle(v.vin));
        radarBlips.appendChild(blip);
    });
}

// Select Active Vehicle
function selectVehicle(vin) {
    selectedVin = vin;
    document.querySelectorAll(".vehicle-card").forEach(c => c.classList.remove("selected"));
    const activeCard = document.getElementById(`card-${vin}`);
    if (activeCard) activeCard.classList.add("selected");

    document.querySelectorAll(".radar-blip").forEach(b => b.classList.remove("active"));
    const activeBlip = document.getElementById(`blip-${vin}`);
    if (activeBlip) activeBlip.classList.add("active");

    updateTelematicsDisplay();
    populateFormWithActiveVehicle();
}

// Update Active Vehicle Center Dashboard
function updateTelematicsDisplay() {
    const v = FLEET.find(x => x.vin === selectedVin);
    if (!v) return;

    activeVinTitle.textContent = v.vin;
    activeOemBadge.textContent = v.oem + (v.miles ? " (Miles)" : "");
    gaugeSpeed.textContent = v.speed.toFixed(1);

    // Speed bar (0 - 140 scale)
    const speedPct = Math.min(100, (v.speed / 140) * 100);
    speedBarFill.style.width = `${speedPct}%`;
    if (v.speed > 100) {
        speedBarFill.classList.add("overspeed");
        speedLimitStatus.textContent = "⚠️ OVERSPEED ALERT (>100 km/h)";
        speedLimitStatus.style.color = "var(--color-red)";
    } else {
        speedBarFill.classList.remove("overspeed");
        speedLimitStatus.textContent = "Normal Cruising Speed";
        speedLimitStatus.style.color = "var(--text-muted)";
    }

    // Odometer
    gaugeOdometer.textContent = v.odo.toFixed(1);
    gaugeUnit.textContent = v.miles ? "mi" : "km";

    // Battery / Fuel
    if (v.soc !== null) {
        gaugeSoc.textContent = `${v.soc.toFixed(1)} %`;
        socBarFill.style.width = `${v.soc}%`;
        socStatus.textContent = v.soc > 20 ? "Battery Healthy" : "Low Battery Warning";
    } else {
        gaugeSoc.textContent = "N/A";
        socBarFill.style.width = "0%";
        socStatus.textContent = "Internal Combustion / Hybrid";
    }

    // Compass
    compassNeedle.style.transform = `rotate(${v.heading}deg)`;
    gaugeHeading.textContent = `${v.heading}°`;

    // Coordinates
    gaugeCoords.textContent = `LAT: ${v.lat.toFixed(6)} | LON: ${v.lon.toFixed(6)}`;
}

// Populate the right-hand testing form with current vehicle's telemetry
function populateFormWithActiveVehicle() {
    const v = FLEET.find(x => x.vin === selectedVin);
    if (!v) return;

    document.getElementById("input-vin").value = v.vin;
    document.getElementById("input-oem").value = v.oem;
    document.getElementById("input-speed").value = v.speed.toFixed(1);
    document.getElementById("input-odometer").value = v.odo.toFixed(1);
    document.getElementById("input-unit").value = v.miles ? "mi" : "km";
    document.getElementById("input-lat").value = v.lat.toFixed(4);
    document.getElementById("input-lon").value = v.lon.toFixed(4);
    document.getElementById("input-soc").value = v.soc !== null ? v.soc.toFixed(1) : "";
}

// ==========================================================================
// Simulation Physics & Ingestion API
// ==========================================================================
function advanceVehicle(v, forceSpeed = null) {
    if (forceSpeed !== null) {
        v.speed = forceSpeed;
    } else {
        // Natural speed variation (-5 to +5 km/h, bounded 15 to 125 km/h)
        const delta = (Math.random() - 0.48) * 6;
        v.speed = Math.max(15, Math.min(130, v.speed + delta));
    }

    // Odometer distance increment
    v.odo += (v.speed / 3600) * 3; // 3 seconds travel
    
    // GPS delta
    v.lat += (Math.random() - 0.5) * 0.001;
    v.lon += (Math.random() - 0.5) * 0.001;
    v.heading = (v.heading + Math.floor((Math.random() - 0.5) * 15) + 360) % 360;

    if (v.soc !== null) {
        v.soc = Math.max(5, v.soc - (v.speed / 100) * 0.05);
    }
}

// Send Telemetry Heartbeat to POST /v1/heartbeats
async function sendHeartbeat(payload) {
    const startTime = performance.now();
    stats.total++;
    statTotal.textContent = stats.total;

    try {
        const response = await fetch("/v1/heartbeats", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const elapsed = Math.round(performance.now() - startTime);
        statLatency.textContent = `${elapsed} ms`;

        const data = await response.json();
        
        if (response.status === 202) {
            stats.accepted++;
            statAccepted.textContent = stats.accepted;
            addFeedItem(202, payload.vin, `Accepted • ${payload.speed_kmph} km/h • Odo: ${payload.odometer} ${payload.odometer_unit || 'km'}`);
        } else {
            stats.rejected++;
            statRejected.textContent = stats.rejected;
            const errDetail = data.details ? data.details.map(d => `${d.field}: ${d.message}`).join(", ") : (data.message || "Validation Error");
            addFeedItem(response.status, payload.vin || "UNKNOWN", `Rejected (400) • ${errDetail}`);
        }

        return { status: response.status, data };
    } catch (err) {
        stats.rejected++;
        statRejected.textContent = stats.rejected;
        addFeedItem(500, payload.vin || "ERROR", `Network Error: ${err.message}`);
        return { status: 500, error: err.message };
    }
}

// Add Item to Live Event Stream
function addFeedItem(status, vin, message) {
    const emptyState = document.querySelector(".feed-empty-state");
    if (emptyState) emptyState.remove();

    const item = document.createElement("div");
    item.className = "feed-item";

    const now = new Date().toLocaleTimeString();
    const isSuccess = status === 202;

    item.innerHTML = `
        <div class="feed-item-top">
            <span class="feed-status-badge ${isSuccess ? 'status-202' : 'status-400'}">HTTP ${status}</span>
            <span style="font-weight: 700; color: #fff;">${vin}</span>
            <span class="feed-time">${now}</span>
        </div>
        <div class="feed-detail">${message}</div>
    `;

    eventFeedList.prepend(item);

    // Keep feed trimmed to 50 items for optimal performance
    if (eventFeedList.children.length > 50) {
        eventFeedList.removeChild(eventFeedList.lastChild);
    }
}

// Generate valid HeartbeatRequest JSON payload from Vehicle object
function generatePayload(v) {
    return {
        vin: v.vin,
        ts: new Date().toISOString(),
        speed_kmph: Math.round(v.speed * 10) / 10,
        odometer: Math.round(v.odo * 10) / 10,
        odometer_unit: v.miles ? "mi" : "km",
        lat: Math.round(v.lat * 1000000) / 1000000,
        lon: Math.round(v.lon * 1000000) / 1000000,
        heading: v.heading,
        soc_pct: v.soc !== null ? Math.round(v.soc * 10) / 10 : null,
        ignition: v.ignition,
        source_oem: v.oem
    };
}

// Check Backend /health
async function checkHealth() {
    try {
        const res = await fetch("/health");
        const dot = document.querySelector(".status-dot");
        const statusText = document.getElementById("system-status-text");

        if (res.ok) {
            statusText.textContent = "SYSTEM LIVE";
            dot.style.background = "var(--color-emerald)";
        } else if (res.status === 503) {
            statusText.textContent = "API ACTIVE (DB STANDBY)";
            dot.style.background = "var(--color-amber)";
        }
    } catch {
        const statusText = document.getElementById("system-status-text");
        statusText.textContent = "CONNECTING...";
    }
}

// ==========================================================================
// Event Listeners & Preset Handlers
// ==========================================================================
function setupEventListeners() {
    // Start / Stop Auto Simulation
    btnToggleSim.addEventListener("click", () => {
        if (autoSimInterval) {
            clearInterval(autoSimInterval);
            autoSimInterval = null;
            btnToggleSim.classList.remove("active");
            simBtnText.textContent = "Start Auto Sim";
        } else {
            btnToggleSim.classList.add("active");
            simBtnText.textContent = "Stop Sim";
            autoSimInterval = setInterval(() => {
                // Pick 1-2 vehicles at random to send a ping
                const randomVehicles = [FLEET[Math.floor(Math.random() * FLEET.length)]];
                randomVehicles.forEach(v => {
                    advanceVehicle(v);
                    const speedEl = document.getElementById(`speed-${v.vin}`);
                    if (speedEl) {
                        speedEl.textContent = `${v.speed.toFixed(1)} km/h`;
                        if (v.speed > 100) speedEl.classList.add("overspeed");
                        else speedEl.classList.remove("overspeed");
                    }
                    if (v.vin === selectedVin) updateTelematicsDisplay();
                    sendHeartbeat(generatePayload(v));
                });
            }, 1800);
        }
    });

    // Burst: Send ping for every vehicle once
    btnSendBurst.addEventListener("click", () => {
        FLEET.forEach(v => {
            advanceVehicle(v);
            sendHeartbeat(generatePayload(v));
        });
        updateTelematicsDisplay();
    });

    // Ingest Form Submit
    ingestForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const payload = {
            vin: document.getElementById("input-vin").value.trim(),
            ts: new Date().toISOString(),
            speed_kmph: parseFloat(document.getElementById("input-speed").value) || 0,
            odometer: parseFloat(document.getElementById("input-odometer").value) || 0,
            odometer_unit: document.getElementById("input-unit").value,
            lat: parseFloat(document.getElementById("input-lat").value) || 0,
            lon: parseFloat(document.getElementById("input-lon").value) || 0,
            heading: 90,
            soc_pct: document.getElementById("input-soc").value ? parseFloat(document.getElementById("input-soc").value) : null,
            ignition: true,
            source_oem: document.getElementById("input-oem").value
        };
        await sendHeartbeat(payload);
    });

    // Send Twice (Duplicate Idempotency Test)
    btnDupPing.addEventListener("click", async () => {
        const payload = {
            vin: document.getElementById("input-vin").value.trim(),
            ts: new Date().toISOString(),
            speed_kmph: parseFloat(document.getElementById("input-speed").value) || 0,
            odometer: parseFloat(document.getElementById("input-odometer").value) || 0,
            odometer_unit: document.getElementById("input-unit").value,
            lat: parseFloat(document.getElementById("input-lat").value) || 0,
            lon: parseFloat(document.getElementById("input-lon").value) || 0,
            heading: 90,
            soc_pct: null,
            ignition: true,
            source_oem: document.getElementById("input-oem").value
        };

        addFeedItem(202, payload.vin, "Sending 1st ping (stored to Postgres)...");
        await sendHeartbeat(payload);
        
        setTimeout(async () => {
            addFeedItem(202, payload.vin, "Sending duplicate ping with same (vin, ts)...");
            await sendHeartbeat(payload);
        }, 500);
    });

    // Clear Feed
    btnClearFeed.addEventListener("click", () => {
        eventFeedList.innerHTML = `<div class="feed-empty-state">Event log cleared. Send a ping to stream events!</div>`;
    });
}

// Preset Scenario Buttons
function setupPresets() {
    document.getElementById("preset-valid").addEventListener("click", () => {
        document.getElementById("input-vin").value = "1FA6CVNCNCXW927TC";
        document.getElementById("input-oem").value = "OEM-A";
        document.getElementById("input-speed").value = "64.5";
        document.getElementById("input-odometer").value = "14250.0";
        document.getElementById("input-unit").value = "km";
        document.getElementById("input-lat").value = "12.9716";
        document.getElementById("input-lon").value = "77.5946";
        document.getElementById("input-soc").value = "";
    });

    document.getElementById("preset-miles").addEventListener("click", () => {
        document.getElementById("input-vin").value = "1HGCR2F83HA014920";
        document.getElementById("input-oem").value = "OEM-B";
        document.getElementById("input-speed").value = "55.0";
        document.getElementById("input-odometer").value = "8940.5";
        document.getElementById("input-unit").value = "mi"; // Normalizer converts miles to km!
        document.getElementById("input-lat").value = "12.9820";
        document.getElementById("input-lon").value = "77.6050";
        document.getElementById("input-soc").value = "";
    });

    document.getElementById("preset-bad-vin").addEventListener("click", () => {
        document.getElementById("input-vin").value = "1HGCM82633A0O4352"; // contains 'O' -> 400 Bad Request
        document.getElementById("input-speed").value = "45.0";
        document.getElementById("input-unit").value = "km";
    });

    document.getElementById("preset-future").addEventListener("click", () => {
        document.getElementById("input-vin").value = "1FA6CVNCNCXW927TC";
        const future = new Date(Date.now() + 10 * 60 * 1000).toISOString(); // 10 minutes in future -> 400
        sendHeartbeat({
            vin: "1FA6CVNCNCXW927TC",
            ts: future,
            speed_kmph: 50.0,
            odometer: 1000.0,
            odometer_unit: "km",
            lat: 12.97,
            lon: 77.59,
            heading: 90,
            soc_pct: null,
            ignition: true,
            source_oem: "OEM-A"
        });
    });

    document.getElementById("preset-speed").addEventListener("click", () => {
        document.getElementById("input-vin").value = "1FA6CVNCNCXW927TC";
        document.getElementById("input-speed").value = "124.8"; // Overspeed (>100 km/h)
        document.getElementById("input-unit").value = "km";
    });
}

// Start on DOM Ready
document.addEventListener("DOMContentLoaded", init);
