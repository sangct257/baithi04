// ==========================================
// DANH SÁCH 10 TÀI XẾ GIẢ LẬP TẠI 10 TỌA ĐỘ HÀ NỘI
// ==========================================
const SIMULATED_DRIVERS = [
  {
    id: 1,
    name: "Nguyễn Văn An",
    phone: "0987111001",
    lat: 21.03780,
    lng: 105.78390,
    area: "Cầu Giấy (ĐH Quốc Gia, Xuân Thủy)",
    vehicleType: "BIKE",
    status: "ONLINE",
    marker: null,
  },
  {
    id: 2,
    name: "Trần Văn Bình",
    phone: "0987111002",
    lat: 21.01730,
    lng: 105.78380,
    area: "Nam Từ Liêm (Keangnam Landmark 72)",
    vehicleType: "BIKE",
    status: "ONLINE",
    marker: null,
  },
  {
    id: 3,
    name: "Lê Văn Cường",
    phone: "0987111003",
    lat: 21.02850,
    lng: 105.77580,
    area: "Mỹ Đình (Bến xe Mỹ Đình, Lê Đức Thọ)",
    vehicleType: "CAR_4_SEAT",
    status: "ONLINE",
    marker: null,
  },
  {
    id: 4,
    name: "Phạm Văn Dũng",
    phone: "0987111004",
    lat: 21.03250,
    lng: 105.81220,
    area: "Ba Đình (Lotte Center Liễu Giai)",
    vehicleType: "BIKE",
    status: "ONLINE",
    marker: null,
  },
  {
    id: 5,
    name: "Hoàng Văn Em",
    phone: "0987111005",
    lat: 21.02870,
    lng: 105.85240,
    area: "Hoàn Kiếm (Hồ Gươm / Tràng Tiền)",
    vehicleType: "CAR_4_SEAT",
    status: "ONLINE",
    marker: null,
  },
  {
    id: 6,
    name: "Vũ Văn Phong",
    phone: "0987111006",
    lat: 21.00280,
    lng: 105.81550,
    area: "Đống Đa (Ngã Tư Sở / Royal City)",
    vehicleType: "BIKE",
    status: "ONLINE",
    marker: null,
  },
  {
    id: 7,
    name: "Đỗ Văn Giang",
    phone: "0987111007",
    lat: 21.01180,
    lng: 105.85040,
    area: "Hai Bà Trưng (Vincom Bà Triệu)",
    vehicleType: "CAR_7_SEAT",
    status: "ONLINE",
    marker: null,
  },
  {
    id: 8,
    name: "Bùi Văn Hải",
    phone: "0987111008",
    lat: 21.05050,
    lng: 105.82700,
    area: "Tây Hồ (Hồ Tây / Đường Thanh Niên)",
    vehicleType: "BIKE",
    status: "ONLINE",
    marker: null,
  },
  {
    id: 9,
    name: "Ngô Văn Kiên",
    phone: "0987111009",
    lat: 21.00450,
    lng: 105.79500,
    area: "Thanh Xuân (Trung Hòa Nhân Chính)",
    vehicleType: "CAR_4_SEAT",
    status: "ONLINE",
    marker: null,
  },
  {
    id: 10,
    name: "Đinh Văn Long",
    phone: "0987111010",
    lat: 20.97850,
    lng: 105.78950,
    area: "Hà Đông (Khu đô thị Văn Quán)",
    vehicleType: "CAR_7_SEAT",
    status: "ONLINE",
    marker: null,
  },
];

// ==========================================
// TOÀN CỤC: STATE QUẢN LÝ
// ==========================================
let map;
let currentDriverId = 1;
let currentDriver = SIMULATED_DRIVERS[0];

// Marker & Route cho giả lập đặt xe
let pickupMarker = null;
let dropoffMarker = null;
let routeLine = null;
let nearestDriverLine = null;

// Biến lưu vị trí gửi gần nhất (tránh gửi lặp lại khi không di chuyển)
let lastSentLat = null;
let lastSentLng = null;

// WebSocket STOMP
let stompClient = null;
let wsConnected = false;
let gpsStreamInterval = null;

// Chuyến đi hiện tại (Active Trip)
let activeTrip = null;
let currentAssignedDriver = null;
let offerCountdownInterval = null;
let movementSimulationInterval = null;
let sortedCandidateDrivers = []; // Danh sách tài xế theo thứ tự từ gần nhất đến xa nhất

// Tọa độ & Cấu hình Đặt Xe Khách Hàng
let customerPickupCoords = { lat: 21.0173, lng: 105.7838 }; // Mặc định: Keangnam
let customerDropoffCoords = { lat: 21.0287, lng: 105.8524 }; // Mặc định: Hồ Gươm
let customerVehicleType = "BIKE";
let customerBaseRate = 14000;
let customerPerKmRate = 5000;

// ==========================================
// KHỞI TẠO BẢN ĐỒ LEAFLET & HỆ THỐNG
// ==========================================
window.onload = function () {
  initMap();
  initDriverProfiles();
  renderAll10DriversOnMap();
  connectWebSocket();
  startGpsStreamLoop();
  updateEstimateFare();
  updateNearestDriverRadar();

  // Tự động đồng bộ nhẹ 10 tài xế lên Redis sau 1.5s
  setTimeout(() => {
    syncAll10DriversToBackend(true);
  }, 1500);
};

function initMap() {
  map = L.map("map").setView([21.0200, 105.8100], 12);

  const LOCATIONIQ_TOKEN = "pk.7939da0d43354ae446e39ac07086b84f";
  L.tileLayer(
    `https://{s}-tiles.locationiq.com/v3/streets/r/{z}/{x}/{y}.png?key=${LOCATIONIQ_TOKEN}`,
    { maxZoom: 19, attribution: "© LocationIQ / OpenStreetMap" },
  ).addTo(map);

  setTimeout(() => map.invalidateSize(), 300);

  map.on("click", function (e) {
    const activeTab = document.querySelector(".tab-pane.active")?.id;
    if (activeTab === "tabDriverContent") {
      // Ở tab Tài xế: click để di chuyển tài xế đang chọn
      currentDriver.lat = e.latlng.lat;
      currentDriver.lng = e.latlng.lng;
      if (currentDriver.marker) {
        currentDriver.marker.setLatLng(e.latlng);
      }
      updateDriverCoordsUI(currentDriver.lat, currentDriver.lng);
      addLog(`📍 [TÀI XẾ #${currentDriver.id}] Bạn vừa dời vị trí tới: (${e.latlng.lat.toFixed(5)}, ${e.latlng.lng.toFixed(5)})`);
      updateNearestDriverRadar();
    } else {
      // Ở tab Khách hàng: click để chọn điểm đón khách
      handleCustomerMapClick(e.latlng);
    }
  });
}

// ==========================================
// 1. RENDER 10 TÀI XẾ LÊN BẢN ĐỒ VÀ MINI LIST
// ==========================================
function initDriverProfiles() {
  const select = document.getElementById("selectDriverProfile");
  if (!select) return;

  select.innerHTML = "";
  SIMULATED_DRIVERS.forEach((d) => {
    const opt = document.createElement("option");
    opt.value = d.id;
    opt.innerText = `Tài xế #${d.id} - ${d.name} (${d.vehicleType})`;
    select.appendChild(opt);
  });

  select.value = currentDriverId;
  updateDriverProfileUI();
}

function renderAll10DriversOnMap() {
  SIMULATED_DRIVERS.forEach((d) => {
    if (d.marker) {
      map.removeLayer(d.marker);
    }

    const iconUrl = d.vehicleType === "BIKE"
      ? "https://cdn-icons-png.flaticon.com/512/3082/3082383.png"
      : (d.vehicleType === "CAR_7_SEAT"
        ? "https://cdn-icons-png.flaticon.com/512/3202/3202926.png"
        : "https://cdn-icons-png.flaticon.com/512/1048/1048314.png");

    const customIcon = L.divIcon({
      className: "driver-map-marker",
      html: `
        <div style="position:relative; text-align:center;">
          <img src="${iconUrl}" style="width:36px; height:36px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));" />
          <span class="driver-marker-badge ${d.id === currentDriverId ? "selected" : ""} ${d.status === "BUSY" ? "busy" : ""}">
            #${d.id}
          </span>
        </div>
      `,
      iconSize: [36, 46],
      iconAnchor: [18, 40],
      popupAnchor: [0, -38],
    });

    d.marker = L.marker([d.lat, d.lng], {
      icon: customIcon,
      draggable: (d.id === currentDriverId),
    }).addTo(map);

    d.marker.bindPopup(`
      <b>🚕 Tài xế #${d.id}: ${d.name}</b><br>
      📍 Khu vực: ${d.area}<br>
      🚗 Loại xe: <b>${d.vehicleType}</b><br>
      ⚡ Trạng thái: <b style="color:${d.status === "ONLINE" ? "#22c55e" : (d.status === "BUSY" ? "#eab308" : "#94a3b8")}">${d.status}</b>
    `);

    // Kéo thả marker của tài xế đang chọn
    d.marker.on("dragend", function () {
      const pos = d.marker.getLatLng();
      d.lat = pos.lat;
      d.lng = pos.lng;
      if (d.id === currentDriverId) {
        updateDriverCoordsUI(pos.lat, pos.lng);
        addLog(`📍 [MAP DRAG] Đã dời Tài xế #${d.id} tới: (${pos.lat.toFixed(5)}, ${pos.lng.toFixed(5)})`);
      }
      updateNearestDriverRadar();
    });

    d.marker.on("click", function () {
      onSwitchDriverProfile(d.id);
    });
  });

  renderDriversMiniList();
}

function renderDriversMiniList() {
  const container = document.getElementById("driversMiniList");
  if (!container) return;

  container.innerHTML = "";
  SIMULATED_DRIVERS.forEach((d) => {
    const item = document.createElement("div");
    item.className = `driver-mini-item ${d.id === currentDriverId ? "active" : ""}`;
    item.onclick = () => onSwitchDriverProfile(d.id);

    const statusClass = d.status.toLowerCase();
    item.innerHTML = `
      <span class="driver-mini-badge">#${d.id}</span>
      <div class="driver-mini-info">
        <span class="driver-mini-name">${d.name} (${d.vehicleType})</span>
        <span class="driver-mini-area">${d.area}</span>
      </div>
      <span class="driver-mini-status ${statusClass}">${d.status}</span>
    `;
    container.appendChild(item);
  });
}

function onSwitchDriverProfile(driverId) {
  currentDriverId = driverId;
  currentDriver = SIMULATED_DRIVERS.find((d) => d.id === driverId) || SIMULATED_DRIVERS[0];

  const select = document.getElementById("selectDriverProfile");
  if (select) select.value = driverId;

  const badge = document.getElementById("badgeDriverId");
  if (badge) badge.innerText = `ID: #${currentDriver.id}`;

  updateDriverProfileUI();
  renderAll10DriversOnMap();

  // Di chuyển map đến vị trí tài xế được chọn
  map.panTo([currentDriver.lat, currentDriver.lng], { animate: true });
  addLog(`🔄 [CHUYỂN PROFILE] Đang điều khiển: Tài xế #${currentDriver.id} - ${currentDriver.name}`);
}

function updateDriverProfileUI() {
  updateDriverCoordsUI(currentDriver.lat, currentDriver.lng);

  const areaBadge = document.getElementById("driverAreaBadge");
  if (areaBadge) {
    areaBadge.innerHTML = `<i class="fa-solid fa-map-pin"></i> ${currentDriver.area}`;
  }

  const vTypeSelect = document.getElementById("vehicleType");
  if (vTypeSelect) {
    vTypeSelect.value = currentDriver.vehicleType;
  }

  updateStatusUI(currentDriver.status);
}

function updateDriverCoordsUI(lat, lng) {
  const input = document.getElementById("selectedCoords");
  if (input) input.value = `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
}

// ==========================================
// 2. KẾT NỐI WEBSOCKET QUA STOMP / SOCKJS
// ==========================================
function connectWebSocket() {
  const wsDot = document.getElementById("wsDot");
  const wsText = document.getElementById("wsStatusText");

  try {
    const wsUrl = "http://localhost:8086/ws";
    const socket = new SockJS(wsUrl);
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Tắt log debug rác

    stompClient.connect(
      {},
      function () {
        wsConnected = true;
        if (wsDot) wsDot.className = "ws-dot connected";
        if (wsText) wsText.innerText = "WebSocket: Đã kết nối (Cổng 8086)";
        addLog("✅ [WEBSOCKET CONNECTED] Kết nối Realtime thành công!");

        // Đăng ký nhận thông báo nổ cuốc cho tất cả 10 tài xế
        SIMULATED_DRIVERS.forEach((d) => {
          stompClient.subscribe(`/topic/driver/${d.id}/ride-request`, function (message) {
            try {
              const offerData = JSON.parse(message.body);
              addLog(`🔔 [KAFKA WS] Nhận sự kiện nổ cuốc #${offerData.tripId} cho Tài xế #${d.id}!`);
              showRideOfferModal(offerData, d);
            } catch (e) {
              console.error("Lỗi parse ride offer", e);
            }
          });
        });
      },
      function () {
        wsConnected = false;
        if (wsDot) wsDot.className = "ws-dot disconnected";
        if (wsText) wsText.innerText = "WebSocket: Mất kết nối (Thử lại sau 4s)";
        setTimeout(connectWebSocket, 4000);
      }
    );
  } catch (err) {
    setTimeout(connectWebSocket, 4000);
  }
}

// ==========================================
// 3. VÒNG LẶP GỬI GPS QUA WEBSOCKET 1S / LẦN
// (KIỂM TRA TỌA ĐỘ THAY ĐỔI TRÁNH SPAM)
// ==========================================
function startGpsStreamLoop() {
  if (gpsStreamInterval) clearInterval(gpsStreamInterval);

  gpsStreamInterval = setInterval(() => {
    checkAndSendGpsStream();
  }, 1000); // 1 giây / lần
}

function checkAndSendGpsStream() {
  if (currentDriver.status === "OFFLINE") return;

  const streamToggle = document.getElementById("wsGpsStreamToggle");
  if (streamToggle && !streamToggle.checked) return;

  // Kiểm tra di chuyển
  const threshold = 0.00001; // ~1 mét
  const deltaLat = lastSentLat === null ? 999 : Math.abs(currentDriver.lat - lastSentLat);
  const deltaLng = lastSentLng === null ? 999 : Math.abs(currentDriver.lng - lastSentLng);

  const hasMoved = deltaLat > threshold || deltaLng > threshold;
  if (!hasMoved) return; // Không di chuyển -> Bỏ qua

  lastSentLat = currentDriver.lat;
  lastSentLng = currentDriver.lng;

  const payload = {
    driverId: currentDriver.id,
    tripId: activeTrip ? activeTrip.id : null,
    lat: parseFloat(currentDriver.lat.toFixed(6)),
    lng: parseFloat(currentDriver.lng.toFixed(6)),
    vehicleType: currentDriver.vehicleType,
  };

  if (stompClient && stompClient.connected) {
    stompClient.send("/app/location/stream", {}, JSON.stringify(payload));
    addLog(`📡 [WS GPS 1s] Tài xế #${currentDriver.id} đã gửi: (${payload.lat}, ${payload.lng})`);
  } else {
    sendDriverLocationRestSilently(payload);
  }
}

async function sendDriverLocationRestSilently(payload) {
  try {
    await fetch("http://localhost:8080/location-tracking-service/api/v1/location/driver", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        driverId: payload.driverId,
        latitude: payload.lat,
        longitude: payload.lng,
        vehicleType: payload.vehicleType,
      }),
    });
  } catch (e) {}
}

// ĐỒNG BỘ CẢ 10 TÀI XẾ LÊN REDIS GEO (1 CLICK)
async function syncAll10DriversToBackend(silent = false) {
  if (!silent) addLog("🚀 Đang đồng bộ toàn bộ 10 Tài xế vào Redis GEO qua WebSocket & REST...");

  let syncedCount = 0;
  for (const d of SIMULATED_DRIVERS) {
    const payload = {
      driverId: d.id,
      tripId: null,
      lat: d.lat,
      lng: d.lng,
      vehicleType: d.vehicleType,
    };

    if (stompClient && stompClient.connected) {
      stompClient.send("/app/location/stream", {}, JSON.stringify(payload));
      syncedCount++;
    } else {
      sendDriverLocationRestSilently(payload);
      syncedCount++;
    }
  }

  if (!silent) {
    addLog(`🎉 [REDIS GEO] Đã lưu tọa độ GPS của cả 10 tài xế lên hệ thống Redis thành công!`);
    alert("✅ Đã đồng bộ 10 tài xế vào Redis GEO thành công!");
  }
}

function onToggleGpsStream(enabled) {
  if (enabled) {
    addLog("🟢 Đã BẬT Stream GPS WebSocket (1s/lần).");
    lastSentLat = null;
  } else {
    addLog("🔴 Đã TẮT Stream GPS WebSocket.");
  }
}

function onChangeVehicleType(type) {
  currentDriver.vehicleType = type;
  addLog(`🚗 Tài xế #${currentDriver.id} đổi sang loại xe: ${type}`);
  renderAll10DriversOnMap();
  updateNearestDriverRadar();
  lastSentLat = null;
  checkAndSendGpsStream();
}

function simulateRandomStep() {
  const stepLat = (Math.random() - 0.5) * 0.0008;
  const stepLng = (Math.random() - 0.5) * 0.0008;
  currentDriver.lat += stepLat;
  currentDriver.lng += stepLng;
  if (currentDriver.marker) {
    currentDriver.marker.setLatLng([currentDriver.lat, currentDriver.lng]);
  }
  updateDriverCoordsUI(currentDriver.lat, currentDriver.lng);
  addLog(`👟 [NHÍCH XE] Tài xế #${currentDriver.id} vừa nhích tới: (${currentDriver.lat.toFixed(5)}, ${currentDriver.lng.toFixed(5)})`);
  updateNearestDriverRadar();
}

// ==========================================
// 4. QUẢN LÝ TAB (TÀI XẾ vs ĐẶT XE KHÁCH)
// ==========================================
function switchTab(tab) {
  const btnDriver = document.getElementById("tabBtnDriver");
  const btnCustomer = document.getElementById("tabBtnCustomer");
  const paneDriver = document.getElementById("tabDriverContent");
  const paneCustomer = document.getElementById("tabCustomerContent");

  if (tab === "driver") {
    btnDriver.classList.add("active");
    btnCustomer.classList.remove("active");
    paneDriver.classList.add("active");
    paneCustomer.classList.remove("active");
  } else {
    btnCustomer.classList.add("active");
    btnDriver.classList.remove("active");
    paneCustomer.classList.add("active");
    paneDriver.classList.remove("active");
    updateCustomerMapMarkers();
    updateNearestDriverRadar();
  }
}

// ==========================================
// 5. THUẬT TOÁN QUÉT TÀI XẾ GẦN NHẤT (RADAR)
// ==========================================
function findNearestDrivers(pickupLat, pickupLng, vehicleType = null) {
  // Lọc các tài xế ONLINE (nếu có lọc theo vehicleType thì ưu tiên cùng loại xe)
  let eligibleDrivers = SIMULATED_DRIVERS.filter((d) => d.status === "ONLINE");

  if (vehicleType) {
    const sameType = eligibleDrivers.filter((d) => d.vehicleType === vehicleType);
    if (sameType.length > 0) eligibleDrivers = sameType;
  }

  // Tính khoảng cách Haversine từ điểm đón đến từng tài xế
  const withDistance = eligibleDrivers.map((d) => {
    const distKm = getDistanceFromLatLonInKm(pickupLat, pickupLng, d.lat, d.lng);
    return {
      driver: d,
      distanceKm: distKm,
      distanceMeters: Math.round(distKm * 1000),
    };
  });

  // Sắp xếp tăng dần theo khoảng cách (GẦN NHẤT LÊN ĐẦU TIÊN)
  withDistance.sort((a, b) => a.distanceKm - b.distanceKm);
  return withDistance;
}

function updateNearestDriverRadar() {
  const radarName = document.getElementById("radarNearestDriverName");
  const radarMeta = document.getElementById("radarNearestDriverMeta");
  if (!radarName || !radarMeta) return;

  const results = findNearestDrivers(customerPickupCoords.lat, customerPickupCoords.lng, customerVehicleType);

  if (results.length > 0) {
    const nearest = results[0];
    const d = nearest.driver;
    radarName.innerHTML = `🎯 <b>Tài xế #${d.id} - ${d.name}</b> (${d.vehicleType})`;
    radarMeta.innerHTML = `<i class="fa-solid fa-location-arrow"></i> Cách điểm đón: <b>${nearest.distanceMeters < 1000 ? nearest.distanceMeters + " m" : nearest.distanceKm.toFixed(2) + " km"}</b> (Khu vực: ${d.area})`;

    // Vẽ đường nối nét đứt mờ từ tài xế gần nhất đến điểm đón khách
    if (nearestDriverLine) map.removeLayer(nearestDriverLine);
    nearestDriverLine = L.polyline([[d.lat, d.lng], [customerPickupCoords.lat, customerPickupCoords.lng]], {
      color: "#22c55e",
      weight: 2,
      dashArray: "4, 6",
      opacity: 0.7,
    }).addTo(map);

  } else {
    radarName.innerText = "⚠️ Không có tài xế nào Online xung quanh";
    radarMeta.innerText = "Vui lòng bật Online cho ít nhất 1 tài xế";
    if (nearestDriverLine) map.removeLayer(nearestDriverLine);
  }
}

// ==========================================
// 6. GIẢ LẬP ĐẶT XE PHÍA KHÁCH HÀNG (CUSTOMER)
// ==========================================
function selectCustomerVehicle(type, base, perKm) {
  customerVehicleType = type;
  customerBaseRate = base;
  customerPerKmRate = perKm;

  document.querySelectorAll(".vehicle-card").forEach((c) => c.classList.remove("active"));
  if (type === "BIKE") document.getElementById("cardBike")?.classList.add("active");
  if (type === "CAR_4_SEAT") document.getElementById("cardCar4")?.classList.add("active");
  if (type === "CAR_7_SEAT") document.getElementById("cardCar7")?.classList.add("active");

  updateEstimateFare();
  updateNearestDriverRadar();
}

function setPickupPreset(preset, btnElement) {
  document.querySelectorAll(".preset-scroll-group .btn-preset").forEach(b => b.classList.remove("active"));
  if (btnElement) btnElement.classList.add("active");

  const presets = {
    keangnam: { name: "Keangnam Landmark 72, Nam Từ Liêm, Hà Nội", lat: 21.0173, lng: 105.7838 },
    mydinh: { name: "Bến xe Mỹ Đình, Nam Từ Liêm, Hà Nội", lat: 21.0285, lng: 105.7778 },
    dhqg: { name: "ĐH Quốc Gia Hà Nội, 144 Xuân Thủy, Cầu Giấy", lat: 21.0371, lng: 105.7818 },
    lotte: { name: "Lotte Center Liễu Giai, Ba Đình, Hà Nội", lat: 21.0325, lng: 105.8122 },
    royal: { name: "Royal City, 72A Nguyễn Trãi, Thanh Xuân, Hà Nội", lat: 21.0028, lng: 105.8155 },
    hoguom: { name: "Hồ Hoàn Kiếm, Tràng Tiền, Hoàn Kiếm, Hà Nội", lat: 21.0287, lng: 105.8524 },
    vincom: { name: "Vincom Center Bà Triệu, Hai Bà Trưng, Hà Nội", lat: 21.0118, lng: 105.8504 },
    hadong: { name: "Khu đô thị Văn Quán, Hà Đông, Hà Nội", lat: 20.9785, lng: 105.7895 },
  };

  const p = presets[preset];
  if (!p) return;

  customerPickupCoords = { lat: p.lat, lng: p.lng };
  document.getElementById("custPickupAddress").value = p.name;
  document.getElementById("custPickupCoordsTag").innerText = `📍 ${p.lat.toFixed(4)}, ${p.lng.toFixed(4)}`;

  updateCustomerMapMarkers();
  updateEstimateFare();
  updateNearestDriverRadar();
  addLog(`📍 [ĐIỂM ĐÓN] Khách hàng chọn: ${p.name}`);
}

function setDropoffPreset(preset, btnElement) {
  const presets = {
    hoguom: { name: "Hồ Hoàn Kiếm, Tràng Tiền, Hoàn Kiếm, Hà Nội", lat: 21.0287, lng: 105.8524 },
    vincom: { name: "Vincom Center Bà Triệu, Hai Bà Trưng, Hà Nội", lat: 21.0118, lng: 105.8504 },
    noibai: { name: "Sân bay Quốc tế Nội Bài, Sóc Sơn, Hà Nội", lat: 21.2187, lng: 105.8042 },
    royal: { name: "Royal City, 72A Nguyễn Trãi, Thanh Xuân, Hà Nội", lat: 21.0028, lng: 105.8155 },
  };

  const p = presets[preset];
  if (!p) return;

  customerDropoffCoords = { lat: p.lat, lng: p.lng };
  document.getElementById("custDropoffAddress").value = p.name;
  document.getElementById("custDropoffCoordsTag").innerText = `📍 ${p.lat.toFixed(4)}, ${p.lng.toFixed(4)}`;

  updateCustomerMapMarkers();
  updateEstimateFare();
}

function handleCustomerMapClick(latlng) {
  customerPickupCoords = { lat: latlng.lat, lng: latlng.lng };
  document.getElementById("custPickupAddress").value = `Vị trí chọn trên Map (${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)})`;
  document.getElementById("custPickupCoordsTag").innerText = `📍 ${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)}`;

  updateCustomerMapMarkers();
  updateEstimateFare();
  updateNearestDriverRadar();
  addLog(`📍 [MAP PICKUP] Khách hàng chọn điểm đón tại: (${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)})`);
}

function updateCustomerMapMarkers() {
  if (pickupMarker) map.removeLayer(pickupMarker);
  const pickupIcon = L.icon({
    iconUrl: "https://cdn-icons-png.flaticon.com/512/14090/14090313.png",
    iconSize: [36, 36],
    iconAnchor: [18, 36],
  });
  pickupMarker = L.marker([customerPickupCoords.lat, customerPickupCoords.lng], {
    icon: pickupIcon,
  }).addTo(map).bindPopup("<b>🟢 Điểm đón khách</b><br>" + document.getElementById("custPickupAddress").value);

  if (dropoffMarker) map.removeLayer(dropoffMarker);
  const dropoffIcon = L.icon({
    iconUrl: "https://cdn-icons-png.flaticon.com/512/9131/9131546.png",
    iconSize: [36, 36],
    iconAnchor: [18, 36],
  });
  dropoffMarker = L.marker([customerDropoffCoords.lat, customerDropoffCoords.lng], {
    icon: dropoffIcon,
  }).addTo(map).bindPopup("<b>🔴 Điểm trả khách</b><br>" + document.getElementById("custDropoffAddress").value);

  drawRouteBetween([customerPickupCoords.lat, customerPickupCoords.lng], [customerDropoffCoords.lat, customerDropoffCoords.lng]);
}

function drawRouteBetween(pointA, pointB) {
  if (routeLine) map.removeLayer(routeLine);
  routeLine = L.polyline([pointA, pointB], {
    color: "#f59e0b",
    weight: 4,
    opacity: 0.8,
    dashArray: "6, 8",
  }).addTo(map);
}

function updateEstimateFare() {
  const dist = getDistanceFromLatLonInKm(
    customerPickupCoords.lat,
    customerPickupCoords.lng,
    customerDropoffCoords.lat,
    customerDropoffCoords.lng
  );

  let fare = customerBaseRate;
  if (dist > 2.0) fare += (dist - 2.0) * customerPerKmRate;
  fare = Math.round(fare / 1000) * 1000;

  const distText = document.getElementById("estDistanceText");
  const fareText = document.getElementById("estFareText");
  if (distText) distText.innerText = `~ ${dist.toFixed(1)} km`;
  if (fareText) fareText.innerText = `${fare.toLocaleString("vi-VN")} VNĐ`;
}

// BẤM NÚT ĐẶT CHUYẾN ĐI -> GÁN ĐƠN CHO TÀI XẾ GẦN NHẤT
async function createCustomerTrip() {
  const pickupAddr = document.getElementById("custPickupAddress").value;
  const dropoffAddr = document.getElementById("custDropoffAddress").value;
  const payMethod = document.getElementById("custPaymentMethod").value;

  const dist = getDistanceFromLatLonInKm(
    customerPickupCoords.lat,
    customerPickupCoords.lng,
    customerDropoffCoords.lat,
    customerDropoffCoords.lng
  );

  let fare = customerBaseRate;
  if (dist > 2.0) fare += (dist - 2.0) * customerPerKmRate;
  fare = Math.round(fare / 1000) * 1000;

  // 1. Quét tìm danh sách tài xế theo thứ tự từ GẦN NHẤT đến XA NHẤT
  sortedCandidateDrivers = findNearestDrivers(customerPickupCoords.lat, customerPickupCoords.lng, customerVehicleType);

  if (sortedCandidateDrivers.length === 0) {
    alert("Không tìm thấy tài xế nào đang Online để gán đơn!");
    return;
  }

  // Lấy tài xế ƯU TIÊN 1 (GẦN NHẤT)
  const nearestCandidate = sortedCandidateDrivers[0];
  currentAssignedDriver = nearestCandidate.driver;

  addLog(`🚖 [ĐẶT XE THÀNH CÔNG] Điểm đón: "${pickupAddr}". Đang tìm tài xế gần nhất...`);
  addLog(`🎯 [DISPATCH MATCH] Phát hiện Tài xế #${currentAssignedDriver.id} - ${currentAssignedDriver.name} ở khoảng cách GẦN NHẤT (${nearestCandidate.distanceMeters} m)!`);

  const newTrip = {
    id: Math.floor(Math.random() * 9000 + 1000),
    customerId: 99,
    pickupAddress: pickupAddr,
    dropoffAddress: dropoffAddr,
    pickupLat: customerPickupCoords.lat,
    pickupLng: customerPickupCoords.lng,
    dropoffLat: customerDropoffCoords.lat,
    dropoffLng: customerDropoffCoords.lng,
    vehicleType: customerVehicleType,
    paymentMethod: payMethod,
    distanceKm: parseFloat(dist.toFixed(1)),
    fareAmount: fare,
    status: "Requested",
    assignedDriverId: currentAssignedDriver.id,
    assignedDriverName: currentAssignedDriver.name,
    distanceToPickupMeters: nearestCandidate.distanceMeters,
  };

  // Cập nhật giao diện Khách hàng
  document.getElementById("custActiveTripBox").style.display = "flex";
  document.getElementById("custTripIdBadge").innerText = `Trip #${newTrip.id}`;
  document.getElementById("custTripStatusBadge").innerText = "GÁN ĐƠN...";
  document.getElementById("custTripStatusMessage").innerHTML = `
    <i class="fa-solid fa-spinner fa-spin"></i>
    <span>Đang gửi đề nghị chuyến đi tới Tài xế #${currentAssignedDriver.id} - ${currentAssignedDriver.name} (Cách bạn ${nearestCandidate.distanceMeters} m - Gần nhất)...</span>
  `;
  document.getElementById("custAssignedDriverText").innerText = `Tài xế #${currentAssignedDriver.id} - ${currentAssignedDriver.name}`;
  document.getElementById("custDriverDistanceText").innerText = `${nearestCandidate.distanceMeters} m`;

  // Chuyển profile điều khiển sang tài xế được gán và hiện popup nổ cuốc
  onSwitchDriverProfile(currentAssignedDriver.id);

  setTimeout(() => {
    showRideOfferModal(newTrip, currentAssignedDriver);
  }, 400);
}

// ==========================================
// 7. MODAL NỔ CUỐC 30S CHO TÀI XẾ ĐƯỢC GÁN
// ==========================================
let currentOfferData = null;

function showRideOfferModal(offerData, driverTarget) {
  currentOfferData = offerData;
  const modal = document.getElementById("rideOfferModal");
  if (!modal) return;

  const tripId = offerData.tripId || offerData.id || "101";
  document.getElementById("offerTripTitle").innerText = `Chuyến đi #${tripId}`;
  document.getElementById("offerPickupAddress").innerText = offerData.pickupAddress || "Điểm đón khách";
  document.getElementById("offerDropoffAddress").innerText = offerData.dropoffAddress || "Điểm trả khách";
  document.getElementById("offerFare").innerText = (offerData.amount || offerData.fareAmount || 41500).toLocaleString("vi-VN") + " đ";
  document.getElementById("offerDistance").innerText = (offerData.distanceKm || 5.2) + " km";
  document.getElementById("offerVehicle").innerText = offerData.vehicleType || currentDriver.vehicleType;

  const banner = document.getElementById("offerAssignedBanner");
  if (banner && driverTarget) {
    banner.innerHTML = `<i class="fa-solid fa-user-check"></i> <span>Gán cho <b>Tài xế #${driverTarget.id} - ${driverTarget.name}</b> (Cách đón: <b>${offerData.distanceToPickupMeters || 350} m</b> - GẦN NHẤT)</span>`;
  }

  modal.style.display = "flex";

  // Đếm ngược 30 giây
  let secondsLeft = 30;
  const timerBadge = document.getElementById("offerTimer");
  timerBadge.innerText = `${secondsLeft}s`;

  if (offerCountdownInterval) clearInterval(offerCountdownInterval);
  offerCountdownInterval = setInterval(() => {
    secondsLeft--;
    timerBadge.innerText = `${secondsLeft}s`;

    if (secondsLeft <= 0) {
      clearInterval(offerCountdownInterval);
      rejectCurrentOffer(true);
    }
  }, 1000);
}

// TÀI XẾ TỪ CHỐI ĐƠN -> CHUYỂN TIẾP TÀI XẾ GẦN THỨ 2
function rejectCurrentOffer(isTimeout = false) {
  if (offerCountdownInterval) clearInterval(offerCountdownInterval);
  document.getElementById("rideOfferModal").style.display = "none";

  const tripId = currentOfferData?.tripId || currentOfferData?.id;
  const rejectedDriver = currentAssignedDriver;

  if (isTimeout) {
    addLog(`⏰ [HẾT GIỜ 30S] Tài xế #${rejectedDriver.id} - ${rejectedDriver.name} không phản hồi đơn #${tripId}!`);
  } else {
    addLog(`❌ [TỪ CHỐI] Tài xế #${rejectedDriver.id} - ${rejectedDriver.name} đã TỪ CHỐI đơn #${tripId}!`);
  }

  // Xóa tài xế vừa từ chối khỏi danh sách ứng viên
  sortedCandidateDrivers = sortedCandidateDrivers.filter((item) => item.driver.id !== rejectedDriver.id);

  if (sortedCandidateDrivers.length > 0) {
    // Chuyển tiếp tới tài xế gần thứ 2!
    const nextCandidate = sortedCandidateDrivers[0];
    currentAssignedDriver = nextCandidate.driver;

    addLog(`🔄 [DISPATCH CASCADE] Đang tự động chuyển tiếp đơn #${tripId} tới Tài xế #${currentAssignedDriver.id} - ${currentAssignedDriver.name} (Cách ${nextCandidate.distanceMeters} m - Gần thứ 2)...`);

    // Cập nhật thông báo bên Khách hàng
    document.getElementById("custTripStatusMessage").innerHTML = `
      <i class="fa-solid fa-rotate fa-spin"></i>
      <span>Tài xế #${rejectedDriver.id} từ chối. Đang chuyển đơn tới Tài xế #${currentAssignedDriver.id} - ${currentAssignedDriver.name} (${nextCandidate.distanceMeters} m)...</span>
    `;
    document.getElementById("custAssignedDriverText").innerText = `Tài xế #${currentAssignedDriver.id} - ${currentAssignedDriver.name}`;
    document.getElementById("custDriverDistanceText").innerText = `${nextCandidate.distanceMeters} m`;

    // Chuyển sang profile tài xế tiếp theo và mở popup nổ cuốc
    onSwitchDriverProfile(currentAssignedDriver.id);
    currentOfferData.distanceToPickupMeters = nextCandidate.distanceMeters;

    setTimeout(() => {
      showRideOfferModal(currentOfferData, currentAssignedDriver);
    }, 600);

  } else {
    addLog(`⚠️ [DISPATCH FAILED] Tất cả tài xế xung quanh đều đã từ chối đơn #${tripId}!`);
    const custMsg = document.getElementById("custTripStatusMessage");
    if (custMsg) {
      custMsg.innerHTML = `<i class="fa-solid fa-triangle-exclamation" style="color:#ef4444"></i> <span style="color:#ef4444">Tất cả tài xế xung quanh đều bận hoặc từ chối. Vui lòng thử lại sau!</span>`;
    }
    const custStatus = document.getElementById("custTripStatusBadge");
    if (custStatus) {
      custStatus.innerText = "HẾT XE";
      custStatus.style.background = "#ef4444";
    }
  }
}

// TÀI XẾ NHẬN CHUYẾN
async function acceptCurrentOffer() {
  if (offerCountdownInterval) clearInterval(offerCountdownInterval);
  document.getElementById("rideOfferModal").style.display = "none";

  activeTrip = currentOfferData;
  const tripId = activeTrip.tripId || activeTrip.id;
  const driver = currentAssignedDriver;

  addLog(`🎉 [NHẬN CHUYẾN] Tài xế #${driver.id} - ${driver.name} đã NHẬN đơn #${tripId}!`);

  // Chuyển trạng thái tài xế này sang BUSY
  driver.status = "BUSY";
  updateStatusUI("BUSY");
  renderAll10DriversOnMap();

  // 1. Cập nhật giao diện bên Tab Tài Xế
  document.getElementById("driverActiveTripBox").style.display = "flex";
  document.getElementById("driverTripIdBadge").innerText = `Trip #${tripId}`;
  document.getElementById("driverTripStatusBadge").innerText = "ACCEPTED";
  document.getElementById("driverTripPickupText").innerText = activeTrip.pickupAddress || "Điểm đón";
  document.getElementById("driverTripDropoffText").innerText = activeTrip.dropoffAddress || "Điểm trả";
  document.getElementById("driverTripFareText").innerText = (activeTrip.amount || activeTrip.fareAmount || 41500).toLocaleString("vi-VN") + " VNĐ";
  document.getElementById("driverSimAlertText").innerText = `Tài xế #${driver.id} đang tự động di chuyển đến điểm đón (GPS 1s/lần)...`;
  document.getElementById("btnDriverNextStatus").innerHTML = `<i class="fa-solid fa-flag-checkered"></i> ĐẾN ĐIỂM ĐÓN (ARRIVED)`;

  // 2. Cập nhật giao diện bên Tab Khách Hàng
  document.getElementById("custTripStatusBadge").innerText = "TÀI XẾ ĐANG TỚI";
  document.getElementById("custTripStatusBadge").style.background = "#22c55e";
  document.getElementById("custTripStatusMessage").innerHTML = `
    <i class="fa-solid fa-car-side fa-beat"></i>
    <span>Tài xế #${driver.id} (${driver.name}) đã nhận chuyến! Đang di chuyển tới điểm đón bạn...</span>
  `;
  document.getElementById("custAssignedDriverText").innerText = `Tài xế #${driver.id} - ${driver.name} (${driver.vehicleType})`;

  // Vẽ lộ trình từ Tài xế -> Điểm đón của Khách
  const pickupLat = activeTrip.pickupLat || customerPickupCoords.lat;
  const pickupLng = activeTrip.pickupLng || customerPickupCoords.lng;
  drawRouteBetween([driver.lat, driver.lng], [pickupLat, pickupLng]);

  // Bắt đầu mô phỏng tài xế tự động di chuyển tới điểm đón (GPS stream 1s/lần)
  startDriverMovementSimulation(driver, pickupLat, pickupLng, onArrivedAtPickup);
}

// ==========================================
// 8. GIẢ LẬP TÀI XẾ DI CHUYỂN TỚI ĐIỂM ĐÓN
// (MỖI 1S NHÍCH 1 ĐOẠN -> TỰ ĐỘNG STREAM WEBSOCKET)
// ==========================================
function startDriverMovementSimulation(driver, targetLat, targetLng, onCompleteCallback) {
  if (movementSimulationInterval) clearInterval(movementSimulationInterval);

  addLog(`🚗 [SIMULATION MOVE] Bắt đầu mô phỏng Tài xế #${driver.id} di chuyển về (${targetLat.toFixed(4)}, ${targetLng.toFixed(4)})...`);

  movementSimulationInterval = setInterval(() => {
    const distKm = getDistanceFromLatLonInKm(driver.lat, driver.lng, targetLat, targetLng);
    const distMeters = Math.round(distKm * 1000);

    const driverDist = document.getElementById("driverDistanceToPickupText");
    const custDist = document.getElementById("custDriverDistanceText");
    if (driverDist) driverDist.innerText = `${distMeters} m`;
    if (custDist) custDist.innerText = `${distMeters} m`;

    if (distMeters <= 35) {
      clearInterval(movementSimulationInterval);
      driver.lat = targetLat;
      driver.lng = targetLng;
      if (driver.marker) driver.marker.setLatLng([driver.lat, driver.lng]);
      updateDriverCoordsUI(driver.lat, driver.lng);

      if (driverDist) driverDist.innerText = `ĐÃ ĐẾN NƠI`;
      if (custDist) custDist.innerText = `ĐÃ ĐẾN NƠI`;

      if (onCompleteCallback) onCompleteCallback(driver);
      return;
    }

    // Nhích dần mỗi 1 giây (di chuyển ~0.00035 độ ≈ 35-40 mét)
    const step = 0.00035;
    const ratio = Math.min(step / (distKm || 0.001), 0.25);

    driver.lat += (targetLat - driver.lat) * ratio;
    driver.lng += (targetLng - driver.lng) * ratio;

    if (driver.marker) driver.marker.setLatLng([driver.lat, driver.lng]);
    if (driver.id === currentDriverId) {
      updateDriverCoordsUI(driver.lat, driver.lng);
    }

    // Bắn WebSocket cập nhật Geo Redis mỗi 1s
    checkAndSendGpsStream();
  }, 1000);
}

function onArrivedAtPickup(driver) {
  addLog(`🎯 [ARRIVED] Tài xế #${driver.id} - ${driver.name} đã đến điểm đón khách hàng!`);

  document.getElementById("driverTripStatusBadge").innerText = "ARRIVED";
  document.getElementById("driverSimAlertText").innerText = "Đã đến điểm đón! Đang đợi khách lên xe...";
  document.getElementById("btnDriverNextStatus").innerHTML = `<i class="fa-solid fa-play"></i> BẮT ĐẦU CHUYẾN (IN_PROGRESS)`;

  document.getElementById("custTripStatusBadge").innerText = "TÀI XẾ ĐÃ ĐẾN";
  document.getElementById("custTripStatusMessage").innerHTML = `
    <i class="fa-solid fa-person-walking" style="color:#22c55e"></i>
    <span>Tài xế #${driver.id} đã có mặt tại điểm đón! Vui lòng ra xe.</span>
  `;
}

function advanceTripStatusByDriver() {
  if (!activeTrip || !currentAssignedDriver) return;
  const driver = currentAssignedDriver;
  const currentBadge = document.getElementById("driverTripStatusBadge").innerText;

  if (currentBadge === "ACCEPTED") {
    if (movementSimulationInterval) clearInterval(movementSimulationInterval);
    const dropLat = activeTrip.pickupLat || customerPickupCoords.lat;
    const dropLng = activeTrip.pickupLng || customerPickupCoords.lng;
    driver.lat = dropLat;
    driver.lng = dropLng;
    if (driver.marker) driver.marker.setLatLng([driver.lat, driver.lng]);
    updateDriverCoordsUI(driver.lat, driver.lng);
    onArrivedAtPickup(driver);

  } else if (currentBadge === "ARRIVED") {
    addLog(`🚀 [IN_PROGRESS] Khách đã lên xe! Tài xế #${driver.id} bắt đầu chở khách tới điểm trả...`);
    document.getElementById("driverTripStatusBadge").innerText = "IN_PROGRESS";
    document.getElementById("driverSimAlertText").innerText = "Đang chở khách đến điểm trả (GPS 1s/lần)...";
    document.getElementById("btnDriverNextStatus").innerHTML = `<i class="fa-solid fa-circle-check"></i> HOÀN THÀNH CHUYẾN (COMPLETED)`;

    document.getElementById("custTripStatusBadge").innerText = "ĐANG DI CHUYỂN";
    document.getElementById("custTripStatusMessage").innerHTML = `
      <i class="fa-solid fa-route fa-beat"></i>
      <span>Chuyến đi đang diễn ra! Tài xế #${driver.id} đang chở bạn tới điểm đến.</span>
    `;

    const dropoffLat = activeTrip.dropoffLat || customerDropoffCoords.lat;
    const dropoffLng = activeTrip.dropoffLng || customerDropoffCoords.lng;
    drawRouteBetween([driver.lat, driver.lng], [dropoffLat, dropoffLng]);

    startDriverMovementSimulation(driver, dropoffLat, dropoffLng, onArrivedAtDropoff);

  } else if (currentBadge === "IN_PROGRESS") {
    onArrivedAtDropoff(driver);
  }
}

function onArrivedAtDropoff(driver) {
  if (movementSimulationInterval) clearInterval(movementSimulationInterval);

  addLog(`🏁 [COMPLETED] Chuyến đi #${activeTrip?.id} đã hoàn thành xuất sắc!`);
  alert(`🎉 Chúc mừng! Chuyến đi do Tài xế #${driver.id} - ${driver.name} đảm nhận đã hoàn thành.`);

  if (routeLine) map.removeLayer(routeLine);
  if (pickupMarker) map.removeLayer(pickupMarker);
  if (dropoffMarker) map.removeLayer(dropoffMarker);
  if (nearestDriverLine) map.removeLayer(nearestDriverLine);

  document.getElementById("driverActiveTripBox").style.display = "none";
  document.getElementById("custActiveTripBox").style.display = "none";

  driver.status = "ONLINE";
  updateStatusUI("ONLINE");
  renderAll10DriversOnMap();

  activeTrip = null;
  currentAssignedDriver = null;
  lastSentLat = null;
  checkAndSendGpsStream();
  updateNearestDriverRadar();
}

function cancelTripByDriver() {
  if (confirm("Bạn có chắc chắn muốn hủy chuyến đi này không?")) {
    if (movementSimulationInterval) clearInterval(movementSimulationInterval);
    addLog(`🚫 Tài xế #${currentAssignedDriver?.id} đã hủy chuyến đi!`);
    document.getElementById("driverActiveTripBox").style.display = "none";
    document.getElementById("custTripStatusMessage").innerHTML = `
      <i class="fa-solid fa-ban" style="color:#ef4444"></i>
      <span style="color:#ef4444">Tài xế đã hủy chuyến. Vui lòng đặt lại!</span>
    `;
    if (currentAssignedDriver) currentAssignedDriver.status = "ONLINE";
    updateStatusUI("ONLINE");
    renderAll10DriversOnMap();
    activeTrip = null;
    currentAssignedDriver = null;
  }
}

function cancelTripByCustomer() {
  if (confirm("Bạn có chắc chắn muốn hủy chuyến xe đã đặt không?")) {
    if (movementSimulationInterval) clearInterval(movementSimulationInterval);
    addLog("🚫 Khách hàng đã hủy chuyến xe!");
    document.getElementById("custActiveTripBox").style.display = "none";
    document.getElementById("driverActiveTripBox").style.display = "none";
    if (currentAssignedDriver) currentAssignedDriver.status = "ONLINE";
    updateStatusUI("ONLINE");
    renderAll10DriversOnMap();
    activeTrip = null;
    currentAssignedDriver = null;
  }
}

// ==========================================
// 9. CÔNG TẮC NGUỒN ONLINE / OFFLINE (TÀI XẾ)
// ==========================================
async function onTogglePowerSwitch(checkbox) {
  if (currentDriver.status === "BUSY") {
    alert("Tài xế này đang trong chuyến đi (BUSY), không thể tắt trạng thái hoạt động!");
    checkbox.checked = true;
    return;
  }

  const newStatus = checkbox.checked ? "ONLINE" : "OFFLINE";
  currentDriver.status = newStatus;
  updateStatusUI(newStatus);
  renderAll10DriversOnMap();
  updateNearestDriverRadar();

  if (newStatus === "ONLINE") {
    addLog(`🟢 Tài xế #${currentDriver.id} đã BẬT ONLINE.`);
    lastSentLat = null;
    checkAndSendGpsStream();
  } else {
    addLog(`🔴 Tài xế #${currentDriver.id} đã TẮT OFFLINE.`);
  }
}

function updateStatusUI(status) {
  const toggleInput = document.getElementById("driverStatusToggle");
  const statusText = document.getElementById("statusText");
  if (!toggleInput || !statusText) return;

  if (status === "ONLINE") {
    toggleInput.checked = true;
    toggleInput.disabled = false;
    statusText.innerText = `🟢 ONLINE (#${currentDriver.id})`;
    statusText.style.color = "#4ade80";
  } else if (status === "BUSY") {
    toggleInput.checked = true;
    toggleInput.disabled = true;
    statusText.innerText = `🟡 BUSY (#${currentDriver.id})`;
    statusText.style.color = "#facc15";
  } else {
    toggleInput.checked = false;
    toggleInput.disabled = false;
    statusText.innerText = `🔴 OFFLINE (#${currentDriver.id})`;
    statusText.style.color = "#94a3b8";
  }
}

// GỬI TỌA ĐỘ REST
async function sendDriverLocation() {
  const payload = {
    driverId: currentDriver.id,
    latitude: parseFloat(currentDriver.lat.toFixed(6)),
    longitude: parseFloat(currentDriver.lng.toFixed(6)),
    vehicleType: currentDriver.vehicleType,
  };

  addLog(`📡 [REST] Đang gửi tọa độ Tài xế #${currentDriver.id} (${payload.latitude}, ${payload.longitude})...`);

  try {
    const res = await fetch("http://localhost:8080/location-tracking-service/api/v1/location/driver", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (res.ok) {
      addLog(`✅ [GPS REST SUCCESS] Đã lưu tọa độ Tài xế #${currentDriver.id} vào Redis!`);
    } else {
      addLog(`❌ [LỖI GPS REST]: Status ${res.status}`);
    }
  } catch (err) {
    addLog(`❌ [LỖI KẾT NỐI] Không kết nối được Gateway (Port 8080)!`);
  }
}

// ==========================================
// TIỆN ÍCH HỖ TRỢ
// ==========================================
function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
  const R = 6371; // Bán kính Trái Đất (km)
  const dLat = deg2rad(lat2 - lat1);
  const dLon = deg2rad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function deg2rad(deg) {
  return deg * (Math.PI / 180);
}

function clearLogs() {
  const div = document.getElementById("logs");
  if (div) div.innerHTML = "";
}

function addLog(msg) {
  const div = document.getElementById("logs");
  if (!div) return;
  div.innerHTML += `[${new Date().toLocaleTimeString()}] ${msg}<br>`;
  div.scrollTop = div.scrollHeight;
}
