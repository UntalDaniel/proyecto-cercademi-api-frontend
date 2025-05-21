// script.js (Versión completa y revisada)

// --- Configuración Global ---
const backendUrl = 'http://localhost:8082'; 
let map = null;
let pdiLayerGroup = null; 
const BASE_NAMESPACE_ANUNCIO = "http://www.example.org/cercademiurentals#anuncio_";
const BASE_NAMESPACE_USUARIO = "http://www.example.org/cercademiurentals#usuario_";

// --- Función de Utilidad para Limpiar Títulos ---
function limpiarTituloAnuncio(tituloOriginal) {
    if (typeof tituloOriginal === 'string') {
        return tituloOriginal.replace(/\s*\[[a-f0-9]{4,}\]$/i, '').trim();
    }
    return 'Sin título'; 
}

// --- Manejo de Sesión (localStorage) ---
function getLoggedInUserUri() {
    return localStorage.getItem('loggedInUserUri');
}

function setLoggedInUserUri(uri) {
    if (uri) {
        localStorage.setItem('loggedInUserUri', uri);
        console.log("Usuario loggeado:", uri);
    } else {
        localStorage.removeItem('loggedInUserUri');
        console.log("URI de usuario inválida o nula, sesión limpiada.");
    }
    updateNavigation();
}

function logoutUser() {
    localStorage.removeItem('loggedInUserUri');
    localStorage.removeItem('jwtToken'); 
    console.log("Usuario desloggeado y token JWT eliminado.");
    updateNavigation();
}

// --- Funciones de Utilidad ---
function renderBoolean(value) {
    if (value === null || value === undefined) return 'N/A';
    return value ? 'Sí' : 'No';
}

function showCustomAlert(message, type = 'info') {
    console.log(`[ALERTA - ${type.toUpperCase()}]: ${message}`);
    const alertBoxId = 'custom-alert-box';
    let alertBox = document.getElementById(alertBoxId);

    if (!alertBox) {
        alertBox = document.createElement('div');
        alertBox.id = alertBoxId;
        Object.assign(alertBox.style, {
            position: 'fixed', top: '20px', right: '20px', padding: '15px 20px',
            borderRadius: '8px', color: 'white', zIndex: '2000',
            boxShadow: '0 4px 15px rgba(0,0,0,0.2)', fontSize: '1rem',
            opacity: '0', transition: 'opacity 0.3s ease, transform 0.3s ease',
            transform: 'translateY(-20px)', maxWidth: '90%', wordBreak: 'break-word'
        });
        document.body.appendChild(alertBox);
    }

    alertBox.textContent = message;
    switch (type) {
        case 'success': alertBox.style.backgroundColor = 'var(--success-color, #28a745)'; break;
        case 'error': alertBox.style.backgroundColor = 'var(--danger-color, #dc3545)'; break;
        case 'warning': alertBox.style.backgroundColor = 'var(--warning-color, #ffc107)'; alertBox.style.color = 'var(--text-dark, #212529)'; break;
        default: alertBox.style.backgroundColor = 'var(--info-color, #17a2b8)'; break;
    }
    
    alertBox.style.opacity = '0';
    alertBox.style.transform = 'translateY(-20px)';

    requestAnimationFrame(() => {
        alertBox.style.opacity = '1';
        alertBox.style.transform = 'translateY(0)';
    });

    if (alertBox.customTimeout) clearTimeout(alertBox.customTimeout);
    if (alertBox.customRemovalTimeout) clearTimeout(alertBox.customRemovalTimeout);

    alertBox.customTimeout = setTimeout(() => {
        alertBox.style.opacity = '0';
        alertBox.style.transform = 'translateY(-20px)';
        alertBox.customRemovalTimeout = setTimeout(() => { 
            if (alertBox.parentElement) {
                try { alertBox.parentElement.removeChild(alertBox); } catch(e) {}
            }
        }, 300);
    }, 3500); 
}

// --- Actualización Dinámica de la Navegación ---
function updateNavigation() {
    const loggedInUserUri = getLoggedInUserUri();
    const jwtToken = localStorage.getItem('jwtToken'); 
    const mainLinksContainer = document.querySelector('.nav-group.main-links');
    const authLinksContainer = document.querySelector('.nav-group.auth-links');

    if (!mainLinksContainer || !authLinksContainer) {
        console.warn("Contenedores de navegación no encontrados, no se puede actualizar.");
        return;
    }

    mainLinksContainer.innerHTML = ''; 
    authLinksContainer.innerHTML = '';  

    let mainLinksHTML = `<a href="index.html" class="nav-link">Anuncios</a>`; 

    if (loggedInUserUri && jwtToken) { 
        mainLinksHTML += `
            <a href="crear-anuncio.html" class="nav-link">Publicar Anuncio</a>
            <a href="my-anuncios.html" class="nav-link">Mis Anuncios</a>
            <a href="my-intereses.html" class="nav-link">Mis Intereses</a>
            <a href="profile.html" class="nav-link">Mi Perfil</a>
        `;
        authLinksContainer.innerHTML = `
            <button id="logout-button" class="nav-link button-link" style="background:none; border:none; color:rgba(255,255,255,0.85); cursor:pointer; font-size:0.95rem; font-weight:500; padding:0.5rem 0; font-family:inherit;">Cerrar Sesión</button>
        `;
        const logoutButton = document.getElementById('logout-button');
        if (logoutButton) {
            logoutButton.addEventListener('click', () => {
                logoutUser();
                showCustomAlert("Sesión cerrada exitosamente.");
                const currentPage = window.location.pathname.split('/').pop();
                if (!['index.html', 'login.html', 'register.html', ''].includes(currentPage)) {
                    setTimeout(() => window.location.href = 'index.html', 1500);
                } else if (currentPage !== 'index.html' && currentPage !== '') {
                     setTimeout(() => window.location.reload(), 1500); 
                }
            });
        }
    } else {
         authLinksContainer.innerHTML = `
            <a href="login.html" class="nav-link user-menu" style="display: flex; align-items: center; gap: 0.5rem;">
                <i class="fas fa-user-circle" style="font-size: 1.5rem;"></i>
                <span>Iniciar Sesión</span>
            </a>
            <a href="register.html" class="btn btn-outline-light btn-sm">Registrarse</a>
        `;
    }
    mainLinksContainer.innerHTML = mainLinksHTML;

    const currentPageFile = window.location.pathname.split('/').pop() || 'index.html'; 
    mainLinksContainer.querySelectorAll('.nav-link').forEach(link => {
        const linkFile = (link.getAttribute('href') || '').split('/').pop() || 'index.html';
        link.classList.toggle('active', linkFile === currentPageFile);
    });
     authLinksContainer.querySelectorAll('.nav-link, .button-link, .btn').forEach(link => { 
        const linkFile = (link.getAttribute('href') || '').split('/').pop() || 'index.html';
        if (link.getAttribute('href')) {
            link.classList.toggle('active', linkFile === currentPageFile);
        }
    });
}

// --- Funciones del Mapa y Anuncios (index.html) ---
function initializeMap() {
    const mapDiv = document.getElementById('map');
    if (!mapDiv) {
        console.warn("Elemento #map no encontrado. No se inicializará el mapa.");
        return null;
    }
    if (map && map.getContainer()) { 
        console.log("Mapa ya estaba inicializado y su contenedor existe.");
        return map;
    } else if (map) { 
        console.warn("Mapa existía pero su contenedor no. Re-inicializando.");
        try { map.remove(); } catch(e) { console.warn("Error al remover mapa previo:", e); }
        map = null;
    }

    console.log("Intentando inicializar mapa Leaflet...");
    try {
        const florenciaCoords = [1.615, -75.61];
        const initialZoom = 13;
        map = L.map('map').setView(florenciaCoords, initialZoom);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);
        
        window.customPdiIcon = L.icon({ 
            iconUrl: 'img/marker-icon-2x-violet.png', 
            shadowUrl: 'img/marker-shadow.png',   
            iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41]
        });

        pdiLayerGroup = L.layerGroup().addTo(map); 
        console.log("Mapa inicializado exitosamente y asignado a la variable global 'map'.");
        return map;
    } catch (error) {
        console.error("Error CRÍTICO inicializando Leaflet:", error);
        if (mapDiv) mapDiv.innerHTML = `<p style="color: red;">Error al cargar el mapa: ${error.message}</p>`;
        map = null; 
        return null;
    }
}

function getFilterValues() {
    const filterForm = document.getElementById('filter-form');
    if (!filterForm) return {};
    const formData = new FormData(filterForm);
    const filters = {};

    const searchText = formData.get('searchText');
    if (searchText && searchText.trim() !== '') filters.searchText = searchText.trim();

    const tipoVivienda = formData.get('tipoVivienda');
    if (tipoVivienda && tipoVivienda !== '') filters.tipoVivienda = tipoVivienda;

    const precioMax = formData.get('precioMax');
    if (precioMax && precioMax.trim() !== '' && !isNaN(parseFloat(precioMax))) {
        filters.precioMax = parseFloat(precioMax);
    }

    const numeroHabitacionesDisponibles = formData.get('numeroHabitacionesDisponibles');
    if (numeroHabitacionesDisponibles && numeroHabitacionesDisponibles.trim() !== '' && !isNaN(parseInt(numeroHabitacionesDisponibles, 10))) {
        filters.habMin = parseInt(numeroHabitacionesDisponibles, 10);
    }
    
    const comodidadesCheckboxes = filterForm.querySelectorAll('input[name="comodidades"]:checked');
    if (comodidadesCheckboxes.length > 0) {
        filters.comodidades = Array.from(comodidadesCheckboxes).map(cb => cb.value);
    }

    const pdiUriValue = formData.get('pdiUri');
    if (pdiUriValue && pdiUriValue.trim() !== '') {
        filters.pdiUri = pdiUriValue.trim();
    }

    const distanciaMaxKmValue = formData.get('distanciaMaxKm');
    if (distanciaMaxKmValue && distanciaMaxKmValue.trim() !== '' && !isNaN(parseFloat(distanciaMaxKmValue))) {
        filters.distMaxKm = parseFloat(distanciaMaxKmValue);
    }
    
    console.log("Filtros recogidos (UI):", filters);
    return filters;
}

async function applyFiltersAndLoadAnuncios(event) {
    if (event) event.preventDefault();
    const anunciosContainer = document.getElementById('anuncios-container'); 
    if (!anunciosContainer) { console.warn("Contenedor 'anuncios-container' no encontrado."); return; }

    const filters = getFilterValues();
    const queryParams = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
        if (Array.isArray(value)) value.forEach(v => queryParams.append(key, v));
        else if (value !== undefined && value !== null && value !== '') queryParams.append(key, value);
    });

    const url = `${backendUrl}/api/viviendas?${queryParams.toString()}`;
    console.log("Fetching anuncios (index) con URL:", url);
    anunciosContainer.innerHTML = '<div class="loading-state"><div class="spinner"></div><p>Cargando propiedades...</p></div>'; 

    if (map && map.getContainer()) {
        map.eachLayer(layer => {
            if (layer instanceof L.Marker && (!pdiLayerGroup || !pdiLayerGroup.hasLayer(layer))) map.removeLayer(layer);
        });
    }
    
    const resultsCountSpan = document.getElementById('results-count'); 

    try {
        const response = await fetch(url); 
        if (!response.ok) throw new Error(`Error HTTP: ${response.status} - ${await response.text()}`);
        const anuncios = await response.json();
        anunciosContainer.innerHTML = ''; 
        if(resultsCountSpan) resultsCountSpan.textContent = anuncios.length;

        if (anuncios.length === 0) {
            anunciosContainer.innerHTML = '<div class="empty-state" style="padding: 2rem; text-align:center;"><i class="fas fa-search-minus" style="font-size: 3rem; color: var(--border-color); margin-bottom: 1rem;"></i><p>No hay anuncios que coincidan con tu búsqueda.</p></div>';
            if (map && map.getContainer()) map.setView([1.615, -75.61], 13); 
            return;
        }

        const bounds = (map && map.getContainer()) ? L.latLngBounds() : null;
        anuncios.forEach(anuncio => {
            const anuncioCard = document.createElement('div');
            anuncioCard.classList.add('anuncio-card'); 
            const idLocal = anuncio.uri ? anuncio.uri.substring(BASE_NAMESPACE_ANUNCIO.length) : 'id-desconocido';
            const tituloLimpio = limpiarTituloAnuncio(anuncio.titulo);
            const precioFormateado = anuncio.precio ? anuncio.precio.toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 0 }) : 'Precio no disponible';
            let fechaFormateada = 'Fecha no disponible';
            if (anuncio.fechaCreacionAnuncio) {
                try {
                    fechaFormateada = new Date(anuncio.fechaCreacionAnuncio).toLocaleDateString('es-ES', {
                        day: 'numeric', month: 'short', year: 'numeric'
                    });
                } catch (e) { console.warn("Error formateando fecha para anuncio:", anuncio.uri, e); }
            }

            anuncioCard.innerHTML = `
                <div class="anuncio-image">
                    <img src="${anuncio.imagenUrl || 'img/default-placeholder.png'}" alt="Imagen de ${tituloLimpio}" onerror="this.onerror=null;this.src='img/default-placeholder.png';">
                    ${anuncio.esRecomendado ? '<span class="anuncio-badge">Recomendado</span>' : ''}
                </div>
                <div class="anuncio-content">
                    <div class="anuncio-header">
                        <div class="anuncio-precio">${precioFormateado}</div>
                        <h3 class="anuncio-titulo">${tituloLimpio}</h3>
                        <p class="anuncio-direccion">
                            <i class="fas fa-map-marker-alt"></i> ${anuncio.barrio || 'Barrio no especificado'}, ${anuncio.ciudad || 'Florencia'}
                        </p>
                    </div>
                    <div class="anuncio-caracteristicas">
                        ${anuncio.numeroHabitacionesDisponibles ? `<span class="caracteristica"><i class="fas fa-bed"></i> ${anuncio.numeroHabitacionesDisponibles} hab.</span>` : ''}
                        ${anuncio.numeroBanosDisponibles ? `<span class="caracteristica"><i class="fas fa-bath"></i> ${anuncio.numeroBanosDisponibles} ba.</span>` : ''}
                        ${anuncio.metrosCuadrados ? `<span class="caracteristica"><i class="fas fa-ruler-combined"></i> ${anuncio.metrosCuadrados} m²</span>` : ''}
                    </div>
                    <div class="anuncio-footer">
                        <a href="detalle.html?id=${idLocal}" class="btn btn-primary btn-sm">Ver Detalles</a>
                        <span class="anuncio-fecha">Publicado: ${fechaFormateada}</span>
                    </div>
                </div>
            `;
            anunciosContainer.appendChild(anuncioCard);

            if (map && map.getContainer() && anuncio.latitud != null && anuncio.longitud != null && typeof anuncio.latitud === 'number' && typeof anuncio.longitud === 'number') {
                const marker = L.marker([anuncio.latitud, anuncio.longitud]).addTo(map);
                marker.bindPopup(`<strong>${tituloLimpio}</strong><br>Precio: ${precioFormateado}<br><a href="detalle.html?id=${idLocal}">Ver Detalles</a>`);
                if (bounds) bounds.extend([anuncio.latitud, anuncio.longitud]);
            }
        });

        if (map && map.getContainer() && bounds && bounds.isValid()) map.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
        else if (map && map.getContainer()) map.setView([1.615, -75.61], 13); 

    } catch (error) {
        console.error('Error al cargar anuncios (index):', error);
        anunciosContainer.innerHTML = `<div class="empty-state" style="padding: 2rem; text-align:center; color: var(--danger-color);"><i class="fas fa-exclamation-triangle" style="font-size: 3rem; margin-bottom: 1rem;"></i><p>Error al cargar anuncios: ${error.message}.</p></div>`;
        if (map && map.getContainer()) map.setView([1.615, -75.61], 13);
        if(resultsCountSpan) resultsCountSpan.textContent = 'Error';
    }
}

function resetFilters() {
    const filterForm = document.getElementById('filter-form');
    if (filterForm) {
        filterForm.reset();
        console.log("Filtros limpiados.");
        applyFiltersAndLoadAnuncios(); 
    }
}

async function loadAndDisplayPuntosDeInteres(mapInstance) {
    console.log("Intentando cargar PDI...");
    if (!mapInstance || !mapInstance.getContainer()) { 
        console.warn("Instancia de mapa no proporcionada o no lista para PDI.");
        return;
    }
    if (!pdiLayerGroup) { 
        if (L && mapInstance) pdiLayerGroup = L.layerGroup().addTo(mapInstance);
        else { console.error("Leaflet (L) o mapInstance no definidos para PDI."); return; }
    }
    pdiLayerGroup.clearLayers();
    console.log("Capas de PDI limpiadas.");

    try {
        const response = await fetch(`${backendUrl}/api/puntos-de-interes`); 
        if (!response.ok) {
            if (response.status === 204) { console.info("No hay PDI (204)."); return; }
            throw new Error(`Error HTTP PDI: ${response.status} - ${await response.text()}`);
        }
        const pdis = await response.json();
        console.log("PDI recibidos:", pdis);

        if (pdis && pdis.length > 0) {
            const pdiIconToUse = window.customPdiIcon || L.icon({ 
                iconUrl: 'img/marker-icon-2x-violet.png', 
                shadowUrl: 'img/marker-shadow.png',   
                iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41]
            });
            pdis.forEach(pdi => {
                if (pdi.latitud != null && pdi.longitud != null) {
                    const nombrePdiLimpio = limpiarTituloAnuncio(pdi.nombre);
                    L.marker([pdi.latitud, pdi.longitud], { icon: pdiIconToUse })
                        .addTo(pdiLayerGroup)
                        .bindPopup(`<strong>${nombrePdiLimpio}</strong><br>Tipo: Punto de Interés`);
                }
            });
            console.log(`${pdis.length} PDI procesados y añadidos al mapa.`);
        } else {
            console.info("No se encontraron PDI para mostrar en el mapa.");
        }
    } catch (error) {
        console.error("Error CRÍTICO cargando PDI:", error);
    }
}

async function populatePdiFilterDropdown() {
    const pdiSelect = document.getElementById('puntoDeInteres'); 
    if (!pdiSelect) {
        console.warn("Dropdown de PDI no encontrado.");
        return;
    }
    console.log("Poblando dropdown PDI...");
    try {
        const response = await fetch(`${backendUrl}/api/puntos-de-interes`); 
        if (!response.ok) {
            if (response.status === 204) { 
                pdiSelect.innerHTML = '<option value="">No hay PDI disponibles</option>'; return; 
            }
            throw new Error(`Error HTTP PDI dropdown: ${response.status}`);
        }
        const pdis = await response.json();
        
        const firstOption = pdiSelect.options[0] && pdiSelect.options[0].value === "" ? pdiSelect.options[0] : new Option("Cualquier ubicación", "");
        pdiSelect.innerHTML = ''; 
        pdiSelect.appendChild(firstOption);

        if (pdis && pdis.length > 0) {
            pdis.forEach(pdi => {
                const option = document.createElement('option');
                option.value = pdi.uri; 
                option.textContent = limpiarTituloAnuncio(pdi.nombre);
                pdiSelect.appendChild(option);
            });
        } else {
             if (pdiSelect.options.length <= 1) { 
                const noPdiOption = new Option("No hay PDI disponibles", "");
                noPdiOption.disabled = true;
                pdiSelect.appendChild(noPdiOption);
            }
        }
    } catch (error) {
        console.error("Error poblando dropdown PDI:", error);
        pdiSelect.innerHTML = '<option value="">Error al cargar PDI</option>';
    }
}

// --- Inicialización de Páginas ---
document.addEventListener('DOMContentLoaded', () => {
    console.log("DOM completamente cargado. Página actual:", window.location.pathname);
    updateNavigation(); 
    const currentPageFile = window.location.pathname.split('/').pop() || 'index.html'; 

    if (currentPageFile === 'index.html' || currentPageFile === '') { 
        console.log("Inicializando página principal (index)...");
        const localMapInstance = initializeMap(); 
        
        const filterForm = document.getElementById('filter-form');
        const resetFiltersButton = document.getElementById('reset-filters');
        
        if (filterForm) filterForm.addEventListener('submit', applyFiltersAndLoadAnuncios);
        if (resetFiltersButton) resetFiltersButton.addEventListener('click', resetFilters);
        
        applyFiltersAndLoadAnuncios(); 
        
        if (localMapInstance && localMapInstance.getContainer()) { 
            loadAndDisplayPuntosDeInteres(localMapInstance); 
            populatePdiFilterDropdown();
        } else {
            console.warn("El mapa NO está definido en index.html o su contenedor no existe, no se cargarán los PDI ni se poblará el dropdown.");
        }

        const toggleFiltersBtn = document.getElementById('toggle-filters');
        const filtersFormElement = document.getElementById('filter-form'); 
        if (toggleFiltersBtn && filtersFormElement) {
            toggleFiltersBtn.addEventListener('click', function() {
                filtersFormElement.classList.toggle('hidden'); 
                const icon = toggleFiltersBtn.querySelector('i');
                if (filtersFormElement.classList.contains('hidden')) {
                    toggleFiltersBtn.innerHTML = '<i class="fas fa-filter"></i> Mostrar Filtros';
                } else {
                    toggleFiltersBtn.innerHTML = '<i class="fas fa-times"></i> Ocultar Filtros';
                }
            });
        }
    } else if (currentPageFile === 'detalle.html') {
        console.log("Inicializando página de detalle...");
        cargarDetalleAnuncio();
    } else if (currentPageFile === 'crear-anuncio.html') {
        console.log("Inicializando página de crear anuncio...");
        if (!getLoggedInUserUri() || !localStorage.getItem('jwtToken')) {
            showCustomAlert("Debes iniciar sesión para publicar un anuncio.", "warning");
            setTimeout(() => window.location.href = 'login.html', 2000);
        } else { handleCrearAnuncioForm(); }
    } else if (currentPageFile === 'editar-anuncio.html') {
        console.log("Inicializando página de editar anuncio.");
        if (!getLoggedInUserUri() || !localStorage.getItem('jwtToken')) {
            showCustomAlert("Debes iniciar sesión para editar un anuncio.", "warning");
            setTimeout(() => window.location.href = 'login.html', 2000);
        } else { loadAnuncioForEdit(); handleEditarAnuncioForm(); }
    } else if (currentPageFile === 'login.html') {
        console.log("Inicializando página de login...");
        handleLoginForm();
    } else if (currentPageFile === 'register.html') {
        console.log("Inicializando página de registro...");
        handleRegisterForm();
    } else if (currentPageFile === 'profile.html') {
        console.log("Inicializando página de perfil...");
        loadUserProfile(); 
    } else if (currentPageFile === 'my-anuncios.html') {
        console.log("Inicializando página de mis anuncios...");
        loadMyAnuncios();
    } else if (currentPageFile === 'my-intereses.html') {
        console.log("Inicializando página de mis intereses...");
        loadMyInterestedAnuncios();
    } else {
        console.warn("Página desconocida:", currentPageFile);
    }
});

// --- Funciones de Páginas Específicas ---
async function cargarDetalleAnuncio() {
    const detalleContainer = document.getElementById('detalle-anuncio-container');
    if (!detalleContainer) { console.warn("Contenedor de detalle no encontrado."); return; }
    const urlParams = new URLSearchParams(window.location.search);
    const anuncioIdParam = urlParams.get('id'); 

    if (!anuncioIdParam) {
        detalleContainer.innerHTML = '<p style="color: orange;">ID de anuncio no proporcionado.</p>';
        return;
    }
    const idFinalParaApi = anuncioIdParam.startsWith("anuncio_") ? anuncioIdParam.substring("anuncio_".length) : anuncioIdParam;
    const anuncioUriApi = `${backendUrl}/api/anuncios/${idFinalParaApi}`;
    detalleContainer.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Cargando detalles para anuncio ${idFinalParaApi}...</p></div>`;
    try {
        const response = await fetch(anuncioUriApi); 
        if (!response.ok) {
            const errorText = await response.text();
            if (response.status === 404) throw new Error(`Anuncio no encontrado (ID: ${idFinalParaApi}).`);
            throw new Error(`Error HTTP: ${response.status} - ${errorText}`);
        }
        const anuncio = await response.json();
        renderAnuncioDetails(anuncio, detalleContainer);
    } catch (error) {
        console.error('Error al cargar detalles del anuncio:', error);
        detalleContainer.innerHTML = `<div class="empty-state" style="color: var(--danger-color);"><i class="fas fa-exclamation-triangle"></i><p>Error: ${error.message}.</p><p><a href="index.html" class="btn btn-secondary">Volver</a></p></div>`;
    }
}

function renderAnuncioDetails(anuncio, detalleContainer) {
    if (!detalleContainer || !anuncio) return;
    const tituloLimpio = limpiarTituloAnuncio(anuncio.titulo); 
    const formatDate = (d) => d ? new Date(d).toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' }) : 'N/A';
    const formatDateTime = (d) => d ? new Date(d).toLocaleString('es-ES', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'N/A';
    const precioFormateado = anuncio.precioMonto ? anuncio.precioMonto.toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 0 }) : 'N/A';
    const idLocalParaInteres = anuncio.uri ? anuncio.uri.substring(BASE_NAMESPACE_ANUNCIO.length) : 'id-desconocido';

    let proveedorHtml = '<div class="detalle-section"><h3><i class="fas fa-user-tie"></i> Información del Proveedor</h3><p>No disponible.</p></div>';
    if (anuncio.proveedor && anuncio.proveedor.uri) {
        const proveedorNombreCompleto = `${anuncio.proveedor.nombres || ''} ${anuncio.proveedor.apellidos || ''}`.trim();
        const nombreProveedorDisplay = proveedorNombreCompleto || anuncio.proveedor.nombreUsuario || 'Proveedor Anónimo';
        const proveedorUsuarioLink = `profile.html?userUri=${encodeURIComponent(anuncio.proveedor.uri)}`;

        proveedorHtml = `
            <div class="detalle-section">
                <h3><i class="fas fa-user-tie"></i> Información del Proveedor</h3>
                <div class="detalle-item"><strong>Nombre:</strong> <span>${nombreProveedorDisplay}</span></div>
                <div class="detalle-item"><strong>Usuario:</strong> <span><a href="${proveedorUsuarioLink}" class="provider-profile-link">@${anuncio.proveedor.nombreUsuario || 'N/A'}</a></span></div>
                <div class="detalle-item"><strong>Calificación Promedio:</strong> <span>${anuncio.proveedor.calificacionPromedio !== null && anuncio.proveedor.calificacionPromedio !== undefined ? `${parseFloat(anuncio.proveedor.calificacionPromedio).toFixed(1)} / 5.0` : 'Sin calificaciones'}</span></div>
            </div>`;
    }

    detalleContainer.innerHTML = `
        <h2>${tituloLimpio} <span class="anuncio-id-chip">ID: ${idLocalParaInteres}</span></h2>
        <div class="anuncio-image-detail">
             <img src="${anuncio.imagenUrl || 'img/default-placeholder.png'}" alt="Imagen de ${tituloLimpio}" onerror="this.onerror=null;this.src='img/default-placeholder.png';">
        </div>
        <div class="detalle-grid">
            <div class="detalle-section">
                <h3><i class="fas fa-info-circle"></i> Información Principal</h3>
                <div class="detalle-item"><strong>Descripción:</strong> <span>${anuncio.descripcionDetallada || 'No proporcionada'}</span></div>
                <div class="detalle-item"><strong>Precio:</strong> <span>${precioFormateado} / mes</span></div>
                <div class="detalle-item"><strong>Término Contrato:</strong> <span>${anuncio.terminoContrato || 'N/A'}</span></div>
                <div class="detalle-item"><strong>Compartido:</strong> <span>${renderBoolean(anuncio.esAnuncioCompartido)}</span></div>
                <div class="detalle-item"><strong>Hab. Disponibles (anuncio):</strong> <span>${anuncio.numeroHabitacionesDisponibles ?? 'N/A'}</span></div>
                <div class="detalle-item"><strong>Baños Disponibles (anuncio):</strong> <span>${anuncio.numeroBanosDisponibles ?? 'N/A'}</span></div>
                <div class="detalle-item"><strong>Disponible Desde:</strong> <span>${formatDate(anuncio.fechaDisponibleDesde)}</span></div>
                <div class="detalle-item"><strong>Estado Actual:</strong> <span class="estado-${(anuncio.estadoAnuncio || 'desconocido').toLowerCase().replace(/\s+/g, '-')}">${anuncio.estadoAnuncio || 'N/A'}</span></div>
                <div class="detalle-item"><strong>Fecha Creación:</strong> <span>${formatDateTime(anuncio.fechaCreacionAnuncio)}</span></div>
                <div class="detalle-item"><strong>Última Actualiz.:</strong> <span>${formatDateTime(anuncio.fechaUltimaActualizacion)}</span></div>
            </div>
            <div class="detalle-section">
                <h3><i class="fas fa-home"></i> Sobre la Vivienda</h3>
                <div class="detalle-item"><strong>Dirección:</strong> <span>${anuncio.direccion || 'N/A'}</span></div>
                <div class="detalle-item"><strong>Barrio:</strong> <span>${anuncio.barrio || 'N/A'}</span></div>
                <div class="detalle-item"><strong>Ciudad:</strong> <span>${anuncio.ciudad || 'Florencia'}</span></div>
                <div class="detalle-item"><strong>Departamento:</strong> <span>${anuncio.departamento || 'Caquetá'}</span></div>
                <div class="detalle-item"><strong>Ubicación (Lat, Lon):</strong> <span>${anuncio.latitud ? parseFloat(anuncio.latitud).toFixed(5) : 'N/A'}, ${anuncio.longitud ? parseFloat(anuncio.longitud).toFixed(5) : 'N/A'}</span></div>
                <div class="detalle-item"><strong>Tipo de Vivienda:</strong> <span>${anuncio.tipoVivienda || 'N/A'}</span></div>
                <div class="detalle-item"><strong>Total Habitaciones (propiedad):</strong> <span>${anuncio.numeroTotalHabitaciones ?? 'N/A'}</span></div>
                <div class="detalle-item"><strong>Total Baños (propiedad):</strong> <span>${anuncio.numeroTotalBanos ?? 'N/A'}</span></div>
                <div class="detalle-item"><strong>Área (m²):</strong> <span>${anuncio.metrosCuadrados ? `${anuncio.metrosCuadrados} m²` : 'N/A'}</span></div>
            </div>
            ${proveedorHtml}
            <div class="detalle-section">
                <h3><i class="fas fa-star"></i> Comodidades Incluidas</h3>
                ${anuncio.comodidades && anuncio.comodidades.length > 0 ? 
                    `<ul>${anuncio.comodidades.map(com => `<li><i class="fas fa-check-circle"></i> ${limpiarTituloAnuncio(com.replace('http://www.example.org/cercademiurentals#comodidad_', '').replace(/_/g, ' '))}</li>`).join('')}</ul>`
                    : '<p>No hay comodidades listadas.</p>'}
            </div>
            <div class="detalle-section">
                <h3><i class="fas fa-gavel"></i> Reglas y Preferencias</h3>
                <div class="detalle-item"><strong>Permite Mascotas:</strong> <span>${renderBoolean(anuncio.permiteMascotas)}</span></div>
                <div class="detalle-item"><strong>Permite Fumar:</strong> <span>${renderBoolean(anuncio.permiteFumar)}</span></div>
                <div class="detalle-item"><strong>Permite Invitados:</strong> <span>${renderBoolean(anuncio.permiteInvitados)}</span></div>
                <div class="detalle-item"><strong>Solo para Estudiantes:</strong> <span>${renderBoolean(anuncio.soloEstudiantes)}</span></div>
                <div class="detalle-item"><strong>Preferencia de Género:</strong> <span>${anuncio.preferenciaGenero || 'Indiferente'}</span></div>
                <div class="detalle-item"><strong>Horas de Silencio:</strong> <span>${renderBoolean(anuncio.estableceHorasSilencio)}</span></div>
            </div>
        </div>
        <div class="action-buttons">
            <button id="interest-button" class="btn btn-primary" data-anuncio-id="${idLocalParaInteres}"><i class="fas fa-heart"></i> Marcar como Interesante</button>
            </div>
        <p style="margin-top: 20px; text-align:center;"><a href="index.html" class="btn btn-secondary"><i class="fas fa-arrow-left"></i> Volver a la lista</a></p>
    `;
    
    const interestButton = document.getElementById('interest-button');
    if (interestButton) interestButton.addEventListener('click', handleMarkAsInterested);
}

async function loadUserProfile() {
    const urlParams = new URLSearchParams(window.location.search);
    const userUriParam = urlParams.get('userUri'); 
    const jwtToken = localStorage.getItem('jwtToken');
    const loggedInUserUri = getLoggedInUserUri(); 
    const mainProfileContainer = document.querySelector('main.profile-container');

    let apiEndpoint = '';
    let isOwnProfileView = false; 

    if (userUriParam) { 
        const decodedUserUri = decodeURIComponent(userUriParam);
        apiEndpoint = `${backendUrl}/api/usuarios/publico?userUri=${encodeURIComponent(decodedUserUri)}`;
        isOwnProfileView = (loggedInUserUri === decodedUserUri);
        
        if (isOwnProfileView && jwtToken) { 
            apiEndpoint = `${backendUrl}/api/usuarios/yo`;
        }
        console.log(`Cargando perfil para URI: ${decodedUserUri}. URL API: ${apiEndpoint}. ¿Es propio?: ${isOwnProfileView}`);
    } else if (jwtToken && loggedInUserUri) { 
        apiEndpoint = `${backendUrl}/api/usuarios/yo`;
        isOwnProfileView = true;
        console.log("Cargando perfil del usuario logueado.");
    } else { 
        if (mainProfileContainer) {
            mainProfileContainer.innerHTML = `<div class="empty-state" style="text-align:center; padding: 2rem;">
                <i class="fas fa-user-lock fa-3x" style="color: var(--warning-color); margin-bottom: 1rem;"></i>
                <h2>Acceso Restringido</h2>
                <p>Debes <a href="login.html" class="btn btn-primary">iniciar sesión</a> para ver tu perfil, o especificar el perfil de un usuario.</p>
            </div>`;
        }
        console.warn("Token JWT ausente y no hay userUriParam, no se puede cargar el perfil.");
        updateNavigation(); 
        return;
    }

    const fetchOptions = (isOwnProfileView && jwtToken && apiEndpoint === `${backendUrl}/api/usuarios/yo`) ? { headers: { 'Authorization': `Bearer ${jwtToken}` } } : {};
    
    if (mainProfileContainer) { 
        mainProfileContainer.innerHTML = `<div class="loading-state" style="padding:3rem; text-align:center;"><div class="spinner"></div><p>Cargando perfil...</p></div>`;
    } else {
        console.error("El contenedor principal del perfil ('main.profile-container') no se encontró en el DOM.");
        return; 
    }

    try {
        const profileResponse = await fetch(apiEndpoint, fetchOptions);
        if (!profileResponse.ok) {
            const errorText = await profileResponse.text().catch(() => "Error desconocido del servidor.");
            let friendlyMessage = `Error HTTP ${profileResponse.status}: ${errorText}`;
            if (profileResponse.status === 404) friendlyMessage = "El perfil solicitado no fue encontrado.";
            else if (profileResponse.status === 400) friendlyMessage = "La solicitud para cargar el perfil es incorrecta. Verifica la URL.";
            else if (profileResponse.status === 401 || profileResponse.status === 403) friendlyMessage = "No tienes permiso para ver este perfil o tu sesión ha expirado.";
            throw new Error(friendlyMessage);
        }
        const userProfile = await profileResponse.json();
        console.log("Datos del perfil recibidos:", userProfile);
        
        const fullName = `${userProfile.nombres || ''} ${userProfile.apellidos || ''}`.trim() || userProfile.nombreUsuario || 'Usuario';
        
        mainProfileContainer.innerHTML = ` 
            <header class="profile-header">
                <div class="profile-banner">
                    <div class="profile-avatar">
                        <img src="img/default-avatar.jpg" alt="Avatar de Usuario" id="profile-avatar-img" style="display:none;">
                        <div class="default-avatar-placeholder" id="profile-avatar-placeholder">
                            <span id="profile-avatar-initial">U</span>
                        </div>
                    </div>
                    <div class="profile-info">
                        <h1 id="profile-fullname"><span class="placeholder-text">Cargando...</span></h1>
                        <p class="user-email" id="profile-email"><span class="placeholder-text">Cargando...</span></p>
                        <div class="profile-stats">
                            <div class="stat">
                                <span class="stat-number" id="profile-num-anuncios">0</span>
                                <span class="stat-label">Anuncios</span>
                            </div>
                            <div class="stat">
                                <span class="stat-number" id="profile-num-intereses">0</span>
                                <span class="stat-label">Intereses</span>
                            </div>
                            <div class="stat">
                                <span class="stat-number" id="profile-rating">N/A</span>
                                <span class="stat-label">Valoración</span>
                            </div>
                        </div>
                    </div>
                    ${isOwnProfileView ? '<button class="btn-edit-profile" id="edit-main-profile-btn" style="display:none;"><i class="fas fa-pencil-alt"></i> Editar Perfil</button>' : ''}
                </div>
            </header>
            <div class="profile-content">
                <aside class="profile-sidebar">
                    <div class="profile-card">
                        <h3>Información de Contacto</h3>
                        <ul class="contact-info">
                            <li><i class="fas fa-user fa-fw"></i><span id="profile-username"><span class="placeholder-text">Cargando...</span></span></li>
                            <li><i class="fas fa-envelope fa-fw"></i><span id="profile-contact-email"><span class="placeholder-text">Cargando...</span></span></li>
                            <li><i class="fas fa-calendar-alt fa-fw"></i>Miembro desde: <span id="profile-member-since"><span class="placeholder-text">Cargando...</span></span></li>
                        </ul>
                    </div>
                    <div class="profile-card">
                        <div class="card-header">
                            <h3>Preferencias de Búsqueda</h3>
                            <button class="btn-text" id="profile-edit-prefs-btn" style="display: ${isOwnProfileView ? 'inline-flex' : 'none'};"><i class="fas fa-pencil-alt"></i> Editar</button>
                        </div>
                        <div id="profile-search-prefs-display">
                            <div class="preference-item">
                                <span class="preference-label">Tipo de vivienda:</span>
                                <span class="preference-value" id="pref-tipo-vivienda-display"><span class="placeholder-text">(No configurado)</span></span>
                            </div>
                            <div class="preference-item">
                                <span class="preference-label">Rango de precios (Max):</span>
                                <span class="preference-value" id="pref-rango-precios-display"><span class="placeholder-text">(No configurado)</span></span>
                            </div>
                        </div>
                        <div id="profile-prefs-edit-container" style="display:none;">
                            <div class="form-group">
                                <label for="pref-edit-tipo-vivienda">Tipo de Vivienda Preferido:</label>
                                <select id="pref-edit-tipo-vivienda" name="prefTipoVivienda" class="form-control">
                                    <option value="">Cualquiera</option>
                                    <option value="Apartamento">Apartamento</option>
                                    <option value="Casa">Casa</option>
                                    <option value="HabitacionCompartida">Habitación Compartida</option>
                                    <option value="Apartaestudio">Apartaestudio</option>
                                    <option value="HabitacionIndependiente">Habitación Independiente</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="pref-edit-precio-max">Precio Máximo Deseado:</label>
                                <input type="number" id="pref-edit-precio-max" name="prefPrecioMax" class="form-control" placeholder="Ej: 800000" step="50000">
                            </div>
                            <div class="edit-actions">
                                <button class="btn btn-primary btn-sm" id="profile-save-prefs-btn">Guardar</button>
                                <button class="btn btn-secondary btn-sm" id="profile-cancel-prefs-btn">Cancelar</button>
                            </div>
                        </div>
                    </div>
                </aside>
                <section class="profile-main">
                    <div class="profile-card">
                        <div class="card-header">
                            <h3>Acerca de mí</h3>
                            <button class="btn-text" id="profile-edit-about-btn" style="display: ${isOwnProfileView ? 'inline-flex' : 'none'};"><i class="fas fa-pencil-alt"></i> Editar</button>
                        </div>
                        <div class="about-content">
                            <p id="profile-about-text"><span class="placeholder-text">Aún no has añadido una biografía.</span></p>
                            <div id="profile-about-edit-container" style="display:none;">
                                <textarea id="profile-about-textarea" rows="4" class="form-control" placeholder="Escribe algo sobre ti..."></textarea>
                                <div class="edit-actions">
                                    <button class="btn btn-primary btn-sm" id="profile-save-about-btn">Guardar</button>
                                    <button class="btn btn-secondary btn-sm" id="profile-cancel-about-btn">Cancelar</button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="profile-card">
                        <div class="card-header">
                            <h3>Anuncios Publicados</h3>
                            <a href="my-anuncios.html" class="btn-text" id="profile-view-all-anuncios" style="display:none;">Ver todos <i class="fas fa-arrow-right"></i></a>
                        </div>
                        <div id="profile-anuncios-list-cards" class="anuncios-grid-profile">
                            </div>
                        <div class="empty-state" id="profile-anuncios-empty" style="display:none;">
                            <i class="fas fa-folder-open"></i>
                            <p>No hay anuncios publicados por este usuario.</p>
                            ${isOwnProfileView ? '<a href="crear-anuncio.html" class="btn btn-primary"><i class="fas fa-plus-circle"></i> Publicar mi primer anuncio</a>' : ''}
                        </div>
                    </div>
                    <div class="profile-card">
                        <div class="card-header">
                            <h3>Intereses Guardados</h3>
                            <a href="my-intereses.html" class="btn-text" id="profile-view-all-intereses" style="display:none;">Ver todos <i class="fas fa-arrow-right"></i></a>
                        </div>
                        <div id="profile-intereses-list-cards" class="anuncios-grid-profile">
                            </div>
                        <div class="empty-state" id="profile-intereses-empty" style="display:none;">
                            <i class="fas fa-heart-crack"></i>
                            <p>Este usuario no tiene intereses marcados.</p>
                            ${isOwnProfileView ? '<a href="index.html" class="btn btn-primary"><i class="fas fa-search"></i> Explorar Anuncios</a>' : ''}
                        </div>
                    </div>
                </section>
            </div>
        `;
        
        const fullNameElem = document.getElementById('profile-fullname');
        if (fullNameElem) fullNameElem.textContent = fullName;
        
        const profileEmailElem = document.getElementById('profile-email'); 
        const contactEmailElem = document.getElementById('profile-contact-email');

        if (isOwnProfileView && userProfile.correo) { 
            if (profileEmailElem) profileEmailElem.textContent = userProfile.correo;
            if (contactEmailElem) contactEmailElem.textContent = userProfile.correo;
        } else {
            if (profileEmailElem) profileEmailElem.innerHTML = `<span class="placeholder-text">(Correo no público)</span>`;
            if (contactEmailElem) contactEmailElem.innerHTML = `<span class="placeholder-text">(Correo no público)</span>`;
        }
        
        const avatarImg = document.getElementById('profile-avatar-img');
        const avatarPlaceholder = document.getElementById('profile-avatar-placeholder');
        const avatarInitial = document.getElementById('profile-avatar-initial');
        
        if (avatarImg && avatarPlaceholder && avatarInitial) {
            avatarImg.src = userProfile.avatarUrl || 'img/default-avatar.jpg'; 
            avatarImg.onload = () => { avatarImg.style.display = 'block'; avatarPlaceholder.style.display = 'none'; };
            avatarImg.onerror = () => {
                avatarImg.style.display = 'none'; avatarPlaceholder.style.display = 'flex';
                avatarInitial.textContent = fullName.charAt(0).toUpperCase() || (userProfile.nombreUsuario ? userProfile.nombreUsuario.charAt(0).toUpperCase() : 'U');
            };
        }
        
        const numAnunciosElem = document.getElementById('profile-num-anuncios');
        if (numAnunciosElem) numAnunciosElem.textContent = userProfile.cantidadAnunciosPublicados !== undefined ? userProfile.cantidadAnunciosPublicados : 'N/A';
        
        const numInteresesElem = document.getElementById('profile-num-intereses');
        if (numInteresesElem) numInteresesElem.textContent = userProfile.cantidadInteresesMarcados !== undefined ? userProfile.cantidadInteresesMarcados : 'N/A';
        
        const ratingElem = document.getElementById('profile-rating');
        if (ratingElem) ratingElem.textContent = (userProfile.calificacionPromedio !== null && userProfile.calificacionPromedio !== undefined) ? parseFloat(userProfile.calificacionPromedio).toFixed(1) : 'N/A';
        
        const usernameElem = document.getElementById('profile-username');
        if (usernameElem) usernameElem.textContent = userProfile.nombreUsuario || 'N/A';
        
        const memberSinceElem = document.getElementById('profile-member-since');
        if (memberSinceElem) memberSinceElem.textContent = userProfile.fechaCreacion ? new Date(userProfile.fechaCreacion).toLocaleDateString('es-ES', { year: 'numeric', month: 'long' }) : 'N/A';

        const editAboutBtn = document.getElementById('profile-edit-about-btn');
        const editPrefsBtn = document.getElementById('profile-edit-prefs-btn');
        if (editAboutBtn) editAboutBtn.style.display = isOwnProfileView ? 'inline-flex' : 'none';
        if (editPrefsBtn) editPrefsBtn.style.display = isOwnProfileView ? 'inline-flex' : 'none';

        const anunciosListDiv = document.getElementById('profile-anuncios-list-cards');
        const anunciosEmpty = document.getElementById('profile-anuncios-empty');
        const viewAllAnunciosLink = document.getElementById('profile-view-all-anuncios');

        if (anunciosListDiv && anunciosEmpty && viewAllAnunciosLink) {
            if (isOwnProfileView) { 
                try {
                    const misAnunciosResponse = await fetch(`${backendUrl}/api/mis-anuncios`, fetchOptions);
                    if (!misAnunciosResponse.ok) throw new Error(`Error HTTP mis anuncios: ${misAnunciosResponse.status}`);
                    const misAnuncios = await misAnunciosResponse.json();
                    anunciosListDiv.innerHTML = ''; 
                    if (misAnuncios.length > 0) {
                        anunciosEmpty.style.display = 'none';
                        misAnuncios.slice(0, 3).forEach(anuncio => {
                            const idLocal = anuncio.uri ? anuncio.uri.substring(BASE_NAMESPACE_ANUNCIO.length) : 'id-desconocido';
                            const card = document.createElement('div');
                            card.className = 'anuncio-card-profile'; 
                            card.innerHTML = `
                                <h4>${limpiarTituloAnuncio(anuncio.titulo) || 'Anuncio'}</h4>
                                <p><i class="fas fa-map-marker-alt fa-fw"></i> ${anuncio.barrio || 'N/A'}</p>
                                <p class="item-price">${anuncio.precio ? anuncio.precio.toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }) : 'N/A'}</p>
                                <a href="detalle.html?id=${idLocal}" class="view-details-link">Ver detalles <i class="fas fa-arrow-right fa-xs"></i></a>
                            `;
                            anunciosListDiv.appendChild(card);
                        });
                        viewAllAnunciosLink.style.display = misAnuncios.length > 3 ? 'inline-flex' : 'none';
                        viewAllAnunciosLink.href = 'my-anuncios.html';
                    } else {
                        anunciosEmpty.style.display = 'block'; viewAllAnunciosLink.style.display = 'none';
                    }
                } catch (errorAnuncios) { 
                    console.error("Perfil: Error cargando mis anuncios:", errorAnuncios); 
                    anunciosEmpty.style.display = 'block'; viewAllAnunciosLink.style.display = 'none';
                    anunciosListDiv.innerHTML = `<p style="color:var(--danger-color);">Error al cargar tus anuncios.</p>`;
                }
            } else { 
                if (userProfile.cantidadAnunciosPublicados > 0) {
                    anunciosListDiv.innerHTML = `<p>${userProfile.cantidadAnunciosPublicados} anuncio(s) publicado(s) por este usuario.</p>`;
                    anunciosEmpty.style.display = 'none';
                    viewAllAnunciosLink.style.display = 'none'; 
                } else {
                    anunciosEmpty.style.display = 'block'; viewAllAnunciosLink.style.display = 'none';
                }
            }
        }

        const interesesListDiv = document.getElementById('profile-intereses-list-cards');
        const interesesEmpty = document.getElementById('profile-intereses-empty');
        const viewAllInteresesLink = document.getElementById('profile-view-all-intereses');

        if (interesesListDiv && interesesEmpty && viewAllInteresesLink) {
            if (isOwnProfileView) { 
                try {
                    const misInteresesResponse = await fetch(`${backendUrl}/api/mis-intereses`, fetchOptions);
                    if (!misInteresesResponse.ok) throw new Error(`Error HTTP mis intereses: ${misInteresesResponse.status}`);
                    const misIntereses = await misInteresesResponse.json();
                    interesesListDiv.innerHTML = '';
                    if (misIntereses.length > 0) {
                        interesesEmpty.style.display = 'none';
                        misIntereses.slice(0, 3).forEach(interes => {
                             const idLocal = interes.uri ? interes.uri.substring(BASE_NAMESPACE_ANUNCIO.length) : 'id-desconocido';
                             const card = document.createElement('div');
                             card.className = 'anuncio-card-profile'; 
                             card.innerHTML = `
                                 <h4>${limpiarTituloAnuncio(interes.titulo) || 'Interés'}</h4>
                                 <p><i class="fas fa-map-marker-alt fa-fw"></i> ${interes.barrio || 'N/A'}</p>
                                 <p class="item-price">${interes.precio ? interes.precio.toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }) : 'N/A'}</p>
                                 <a href="detalle.html?id=${idLocal}" class="view-details-link">Ver detalles <i class="fas fa-arrow-right fa-xs"></i></a>
                             `;
                             interesesListDiv.appendChild(card);
                        });
                        viewAllInteresesLink.style.display = misIntereses.length > 3 ? 'inline-flex' : 'none';
                        viewAllInteresesLink.href = 'my-intereses.html';
                    } else {
                        interesesEmpty.style.display = 'block'; viewAllInteresesLink.style.display = 'none';
                    }
                } catch (errorIntereses) { 
                    console.error("Perfil: Error cargando mis intereses:", errorIntereses);
                    interesesEmpty.style.display = 'block'; viewAllInteresesLink.style.display = 'none';
                    interesesListDiv.innerHTML = `<p style="color:var(--danger-color);">Error al cargar tus intereses.</p>`;
                }
            } else { 
                 if (userProfile.cantidadInteresesMarcados > 0) {
                     interesesListDiv.innerHTML = `<p>Este usuario ha marcado ${userProfile.cantidadInteresesMarcados} anuncio(s) como interesante(s).</p>`;
                     interesesEmpty.style.display = 'none';
                } else {
                    const interesesEmptyElem = document.getElementById('profile-intereses-empty');
                    if (interesesEmptyElem) { 
                        interesesEmptyElem.innerHTML = '<i class="fas fa-heart-crack"></i><p>Este usuario no tiene intereses marcados públicamente.</p>';
                        interesesEmptyElem.style.display = 'block';
                    }
                }
                if(viewAllInteresesLink) viewAllInteresesLink.style.display = 'none';
            }
        }

    } catch (error) {
        console.error('Error general al cargar perfil:', error);
        if (mainProfileContainer) {
            mainProfileContainer.innerHTML = `<div class="empty-state" style="text-align:center; padding: 2rem; color:var(--danger-color);">
                <i class="fas fa-exclamation-triangle fa-3x" style="margin-bottom: 1rem;"></i>
                <h2>Oops! Algo Salió Mal</h2>
                <p>${error.message}</p>
                <p>No pudimos cargar la información del perfil. <a href="index.html" class="btn btn-secondary">Volver al inicio</a></p>
            </div>`;
        }
    }
}


// --- Funciones de Modales Personalizados (reemplazo de alert y confirm) ---
function showCustomConfirm(message) {
    return new Promise((resolve) => {
        const existingModal = document.querySelector('.custom-modal-overlay');
        if (existingModal) existingModal.remove();

        const modalOverlay = document.createElement('div');
        modalOverlay.className = 'custom-modal-overlay';
        
        const modalDialog = document.createElement('div');
        modalDialog.className = 'custom-modal-dialog';
        
        const modalMessage = document.createElement('p');
        modalMessage.textContent = message;
        
        const confirmButton = document.createElement('button');
        confirmButton.textContent = 'Aceptar';
        confirmButton.className = 'btn btn-primary';
        confirmButton.onclick = () => {
            document.body.removeChild(modalOverlay);
            resolve(true);
        };
        
        const cancelButton = document.createElement('button');
        cancelButton.textContent = 'Cancelar';
        cancelButton.className = 'btn btn-secondary';
        cancelButton.onclick = () => {
            document.body.removeChild(modalOverlay);
            resolve(false);
        };
        
        const buttonContainer = document.createElement('div');
        buttonContainer.className = 'custom-modal-actions';
        buttonContainer.appendChild(cancelButton); 
        buttonContainer.appendChild(confirmButton); 
        
        modalDialog.appendChild(modalMessage);
        modalDialog.appendChild(buttonContainer);
        modalOverlay.appendChild(modalDialog);
        document.body.appendChild(modalOverlay);

        requestAnimationFrame(() => {
            modalOverlay.classList.add('visible');
            modalDialog.style.transform = 'scale(1)';
        });
    });
}

function showCustomPrompt(message, defaultValue = '') {
    return new Promise((resolve) => {
        const existingModal = document.querySelector('.custom-modal-overlay');
        if (existingModal) existingModal.remove();

        const modalOverlay = document.createElement('div');
        modalOverlay.className = 'custom-modal-overlay';
        
        const modalDialog = document.createElement('div');
        modalDialog.className = 'custom-modal-dialog';
        
        const modalMessage = document.createElement('p');
        modalMessage.textContent = message;
        
        const inputField = document.createElement('input');
        inputField.type = 'text';
        inputField.value = defaultValue;
        inputField.className = 'form-control'; 
        inputField.style.marginBottom = '1rem';

        const confirmButton = document.createElement('button');
        confirmButton.textContent = 'Aceptar';
        confirmButton.className = 'btn btn-primary';
        confirmButton.onclick = () => {
            document.body.removeChild(modalOverlay);
            resolve(inputField.value);
        };
        
        const cancelButton = document.createElement('button');
        cancelButton.textContent = 'Cancelar';
        cancelButton.className = 'btn btn-secondary';
        cancelButton.onclick = () => {
            document.body.removeChild(modalOverlay);
            resolve(null); 
        };
        
        const buttonContainer = document.createElement('div');
        buttonContainer.className = 'custom-modal-actions';
        buttonContainer.appendChild(cancelButton);
        buttonContainer.appendChild(confirmButton);
        
        modalDialog.appendChild(modalMessage);
        modalDialog.appendChild(inputField);
        modalDialog.appendChild(buttonContainer);
        modalOverlay.appendChild(modalDialog);
        document.body.appendChild(modalOverlay);
        inputField.focus();

        requestAnimationFrame(() => {
            modalOverlay.classList.add('visible');
            modalDialog.style.transform = 'scale(1)';
        });
    });
}

// (Otras funciones como handleMarkAsInterested, loadMyAnuncios, etc., deben estar aquí)
async function handleCrearAnuncioForm() {
    const crearAnuncioForm = document.getElementById('crear-anuncio-form');
    if (!crearAnuncioForm) return;

    crearAnuncioForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const loggedInUserUri = getLoggedInUserUri();
        const jwtToken = localStorage.getItem('jwtToken');
        if (!loggedInUserUri || !jwtToken) {
            showCustomAlert("Debes iniciar sesión para crear un anuncio.", "warning");
            setTimeout(() => window.location.href = 'login.html', 2000);
            return;
        }

        const formData = new FormData(crearAnuncioForm);
        const anuncioData = {
            providerUri: loggedInUserUri, 
            titulo: formData.get('titulo'),
            descripcionDetallada: formData.get('descripcionDetallada') || null,
            precioMonto: parseFloat(formData.get('precioMonto')),
            terminoContrato: formData.get('terminoContrato'),
            esAnuncioCompartido: formData.has('esAnuncioCompartido'), 
            numeroHabitacionesDisponibles: parseInt(formData.get('numeroHabitacionesDisponibles'), 10),
            numeroBanosDisponibles: formData.get('numeroBanosDisponibles') ? parseInt(formData.get('numeroBanosDisponibles'), 10) : null,
            fechaDisponibleDesde: formData.get('fechaDisponibleDesde'), 
            direccion: formData.get('direccion'),
            barrio: formData.get('barrio'),
            latitud: parseFloat(formData.get('latitud')),
            longitud: parseFloat(formData.get('longitud')),
            tipoVivienda: formData.get('tipoVivienda'),
            numeroTotalHabitaciones: parseInt(formData.get('numeroTotalHabitaciones'), 10),
            numeroTotalBanos: parseInt(formData.get('numeroTotalBanos'), 10),
            metrosCuadrados: formData.get('metrosCuadrados') ? parseInt(formData.get('metrosCuadrados'), 10) : null,
            comodidadesUris: Array.from(crearAnuncioForm.querySelectorAll('input[name="comodidades"]:checked')).map(cb => cb.value),
            permiteMascotas: formData.has('permiteMascotas'),
            permiteFumar: formData.has('permiteFumar'),
            permiteInvitados: formData.has('permiteInvitados'),
            soloEstudiantes: formData.has('soloEstudiantes'),
            preferenciaGenero: formData.get('preferenciaGenero') || "", 
            estableceHorasSilencio: formData.has('estableceHorasSilencio'),
            ciudad: "Florencia", 
            departamento: "Caquetá" 
        };

        const requiredFields = ['titulo', 'precioMonto', 'terminoContrato', 'numeroHabitacionesDisponibles', 'fechaDisponibleDesde', 'direccion', 'barrio', 'latitud', 'longitud', 'tipoVivienda', 'numeroTotalHabitaciones', 'numeroTotalBanos'];
        for (const field of requiredFields) {
            if (anuncioData[field] === null || anuncioData[field] === undefined || (typeof anuncioData[field] === 'string' && anuncioData[field].trim() === '') || (typeof anuncioData[field] === 'number' && isNaN(anuncioData[field]))) {
                showCustomAlert(`Por favor, completa el campo obligatorio: ${field}`, "error");
                const inputElement = crearAnuncioForm.querySelector(`[name="${field}"]`);
                if (inputElement) inputElement.focus();
                return;
            }
        }

        console.log("Datos del anuncio a crear:", JSON.stringify(anuncioData, null, 2));
        try {
            const response = await fetch(`${backendUrl}/api/anuncios`, { 
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${jwtToken}` 
                },
                body: JSON.stringify(anuncioData)
            });
            const responseBody = await response.json().catch(async () => ({ message: await response.text() || "Respuesta no es JSON" }));
            if (!response.ok) throw new Error(`Error HTTP: ${response.status} - ${responseBody.message || response.statusText}`);
            
            console.log('Anuncio creado:', responseBody);
            showCustomAlert('Anuncio creado exitosamente.', "success");
            setTimeout(() => window.location.href = 'my-anuncios.html', 1500); 
        } catch (error) {
            console.error('Error al crear el anuncio:', error);
            showCustomAlert(`Error al crear el anuncio: ${error.message}`, "error");
        }
    });
}

async function handleLoginForm() {
    const loginForm = document.getElementById('login-form');
    if (!loginForm) return;

    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const formData = new FormData(loginForm);
        const loginData = {
            identifier: formData.get('identifier'), 
            password: formData.get('password')
        };
        if (!loginData.identifier || !loginData.password) {
            showCustomAlert("Por favor, ingresa tu correo/usuario y contraseña.", "warning");
            return;
        }
        console.log("Intentando login con:", loginData.identifier);
        try {
            const response = await fetch(`${backendUrl}/api/usuarios/login`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(loginData)
            });
            
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: "Respuesta de error no es JSON o está vacía."}));
                throw new Error(`Error HTTP: ${response.status} - ${errorData.message || response.statusText}`);
            }

            const responseData = await response.json(); 
            
            if (responseData.accessToken && responseData.userProfile && responseData.userProfile.uri) {
                localStorage.setItem('jwtToken', responseData.accessToken);
                setLoggedInUserUri(responseData.userProfile.uri); 
                showCustomAlert("Login exitoso!", "success");
                setTimeout(() => window.location.href = 'index.html', 1500); 
            } else {
                throw new Error("Login exitoso, pero formato de respuesta inesperado del backend (faltan token o URI de usuario).");
            }
        } catch (error) {
            console.error('Error durante el login:', error);
            setLoggedInUserUri(null); 
            localStorage.removeItem('jwtToken');
            showCustomAlert(`Login fallido: ${error.message}. Verifica tus credenciales.`, "error");
        }
    });
}

async function handleRegisterForm() {
    const registerForm = document.getElementById('register-form');
    if (!registerForm) return;

    registerForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const formData = new FormData(registerForm);

        const password = formData.get('password');
        const confirmPassword = formData.get('confirmPassword');

        if (password !== confirmPassword) {
            showCustomAlert("Las contraseñas no coinciden.", "error");
            const confirmPasswordInput = document.getElementById('confirm-password');
            if(confirmPasswordInput) confirmPasswordInput.focus();
            return;
        }
        
        const registrationData = {
            nombres: formData.get('nombres'),
            apellidos: formData.get('apellidos'),
            correo: formData.get('correo'),
            nombreUsuario: formData.get('nombreUsuario'),
            password: password 
        };

        const requiredFields = ['nombres', 'apellidos', 'correo', 'nombreUsuario', 'password'];
        for (const field of requiredFields) {
            if (!registrationData[field] || registrationData[field].trim() === '') {
                showCustomAlert(`Por favor, completa el campo: ${field}`, "warning");
                return;
            }
        }
        if (registrationData.password.length < 8) {
             showCustomAlert("La contraseña debe tener al menos 8 caracteres.", "warning");
             return;
        }
        if (!document.getElementById('terms').checked) {
            showCustomAlert("Debes aceptar los Términos de Servicio y la Política de Privacidad.", "warning");
            return;
        }

        console.log("Intentando registrar usuario:", registrationData.nombreUsuario);
        try {
            const response = await fetch(`${backendUrl}/api/usuarios/register`, { 
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(registrationData)
            });
            const responseBody = await response.json().catch(async () => ({ message: await response.text() || "Respuesta no es JSON" }));
            
            if (!response.ok) {
                 throw new Error(`Error HTTP: ${response.status} - ${responseBody.message || response.statusText}`);
            }
            console.log('Registro exitoso:', responseBody);
            showCustomAlert('Usuario registrado exitosamente. Ahora puedes iniciar sesión.', "success");
            setTimeout(() => window.location.href = 'login.html', 2000); 
        } catch (error) {
            console.error('Error durante el registro:', error);
            showCustomAlert(`Registro fallido: ${error.message}`, "error");
        }
    });
}

async function loadMyAnuncios() {
    const myAnunciosList = document.getElementById('my-anuncios-list');
    if (!myAnunciosList) return;

    const jwtToken = localStorage.getItem('jwtToken');
    if (!jwtToken) { 
        myAnunciosList.innerHTML = '<div class="empty-state" style="padding: 2rem; text-align:center;"><i class="fas fa-sign-in-alt" style="font-size: 3rem; color: var(--border-color); margin-bottom: 1rem;"></i><p>Debes iniciar sesión para ver tus anuncios.</p><p><a href="login.html" class="btn btn-primary">Ir a Login</a></p></div>';
        return;
    }
    myAnunciosList.innerHTML = '<div class="loading-state"><div class="spinner"></div><p>Cargando tus anuncios...</p></div>';
    try {
        const response = await fetch(`${backendUrl}/api/mis-anuncios`, {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        if (!response.ok) throw new Error(`Error HTTP: ${response.status} - ${await response.text()}`);
        const misAnuncios = await response.json();
        myAnunciosList.innerHTML = '';

        if (misAnuncios.length === 0) {
            myAnunciosList.innerHTML = `
                <div class="empty-state" style="padding: 2rem; text-align:center;">
                    <i class="fas fa-folder-open" style="font-size: 3rem; color: var(--border-color); margin-bottom: 1rem;"></i>
                    <p>No has publicado anuncios todavía.</p>
                    <a href="crear-anuncio.html" class="btn btn-primary"><i class="fas fa-plus-circle"></i> Publicar mi primer anuncio</a>
                </div>`;
            return;
        }

        misAnuncios.forEach(anuncio => {
            const anuncioCard = document.createElement('div');
            anuncioCard.classList.add('anuncio-card'); 
            const idLocal = anuncio.uri ? anuncio.uri.substring(BASE_NAMESPACE_ANUNCIO.length) : 'id-desconocido';
            const estadoAnuncio = anuncio.estadoAnuncio || 'Desconocido'; 
            const tituloLimpio = limpiarTituloAnuncio(anuncio.titulo);
            const precioFormateado = anuncio.precio ? anuncio.precio.toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 0 }) : 'N/A';

            anuncioCard.innerHTML = `
                <div class="anuncio-image">
                    <img src="${anuncio.imagenUrl || 'img/default-placeholder.png'}" alt="Imagen de ${tituloLimpio}" onerror="this.onerror=null;this.src='img/default-placeholder.png';">
                </div>
                <div class="anuncio-content">
                    <h3 class="anuncio-titulo">${tituloLimpio}</h3>
                    <p class="anuncio-precio">${precioFormateado}</p>
                    <p><strong>Estado:</strong> <span class="estado-${estadoAnuncio.toLowerCase().replace(/\s+/g, '-')}">${estadoAnuncio}</span></p>
                    <p><strong>Ubicación:</strong> ${anuncio.barrio || 'N/A'}, ${anuncio.ciudad || 'Florencia'}</p>
                     <p><strong>Tipo:</strong> ${anuncio.tipoVivienda || 'N/A'}</p>
                    <div class="anuncio-card-actions">
                        <a href="detalle.html?id=${idLocal}" class="btn btn-primary btn-sm view-details-btn">Ver Detalles</a>
                        <a href="editar-anuncio.html?id=${idLocal}" class="btn btn-secondary btn-sm edit-anuncio-full-btn">Editar</a>
                        <button class="btn btn-info btn-sm edit-anuncio-estado-btn" data-anuncio-id="${idLocal}" data-anuncio-titulo="${tituloLimpio}">Cambiar Estado</button>
                        <button class="btn btn-danger btn-sm delete-anuncio-btn" data-anuncio-id="${idLocal}" data-anuncio-titulo="${tituloLimpio}">Eliminar</button>
                    </div>
                </div>
            `;
            myAnunciosList.appendChild(anuncioCard);
        });

        myAnunciosList.querySelectorAll('.delete-anuncio-btn').forEach(button => button.addEventListener('click', handleDeleteAnuncio));
        myAnunciosList.querySelectorAll('.edit-anuncio-estado-btn').forEach(button => button.addEventListener('click', handleEditAnuncioState));

    } catch (error) {
        console.error('Error al cargar mis anuncios:', error);
        myAnunciosList.innerHTML = `<div class="empty-state" style="color: var(--danger-color);"><i class="fas fa-exclamation-triangle"></i><p>Error al cargar tus anuncios: ${error.message}.</p></div>`;
    }
}

async function handleDeleteAnuncio(event) {
    const anuncioIdLocal = event.target.dataset.anuncioId;
    const tituloAnuncio = event.target.dataset.anuncioTitulo || anuncioIdLocal;
    if (!anuncioIdLocal) return;

    const jwtToken = localStorage.getItem('jwtToken');
    if (!jwtToken) {
        showCustomAlert("Debes iniciar sesión para realizar esta acción.", "warning");
        return;
    }
    
    if (!await showCustomConfirm(`¿Seguro que quieres eliminar el anuncio "${limpiarTituloAnuncio(tituloAnuncio)}"? Esta acción no se puede deshacer.`)) return;

    console.log("Intentando eliminar anuncio ID:", anuncioIdLocal);
    try {
        const response = await fetch(`${backendUrl}/api/anuncios/${anuncioIdLocal}`, { 
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${jwtToken}` } 
        });
        const responseText = await response.text();
        if (!response.ok) throw new Error(`Error HTTP: ${response.status} - ${responseText}`);
        console.log('Anuncio eliminado:', responseText);
        showCustomAlert('Anuncio eliminado exitosamente.', "success");
        loadMyAnuncios(); 
    } catch (error) {
        console.error('Error al eliminar el anuncio:', error);
        showCustomAlert(`Error al eliminar el anuncio: ${error.message}`, "error");
    }
}
async function handleEditAnuncioState(event) {
    const anuncioIdLocal = event.target.dataset.anuncioId;
    const tituloAnuncio = event.target.dataset.anuncioTitulo || anuncioIdLocal;
    if (!anuncioIdLocal) return;
    
    const jwtToken = localStorage.getItem('jwtToken');
    if (!jwtToken) {
        showCustomAlert("Debes iniciar sesión para realizar esta acción.", "warning");
        return;
    }
    
    const nuevoEstado = await showCustomPrompt(`Introduce el nuevo estado para "${limpiarTituloAnuncio(tituloAnuncio)}" (Ej: Disponible, Arrendado, Oculto, Pausado):`);

    if (nuevoEstado === null || nuevoEstado.trim() === "") {
        showCustomAlert("Actualización de estado cancelada.", "info");
        return;
    }

    console.log(`Intentando actualizar estado de ${anuncioIdLocal} a: ${nuevoEstado.trim()}`);
    try {
        const response = await fetch(`${backendUrl}/api/anuncios/${anuncioIdLocal}/estado`, {
            method: 'PUT', 
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}` 
            }, 
            body: JSON.stringify({ estado: nuevoEstado.trim() }) 
        });
        const responseText = await response.text();
        if (!response.ok) throw new Error(`Error HTTP: ${response.status} - ${responseText}`);
        console.log('Estado actualizado:', responseText);
        showCustomAlert('Estado del anuncio actualizado exitosamente.', "success");
        loadMyAnuncios(); 
    } catch (error) {
        console.error('Error al actualizar estado:', error);
        showCustomAlert(`Error al actualizar el estado: ${error.message}`, "error");
    }
}

async function loadAnuncioForEdit() {
    const editarAnuncioForm = document.getElementById('editar-anuncio-form');
    const loadingMsg = document.getElementById('loading-anuncio-msg'); 
    
    if (!editarAnuncioForm) {
        console.warn("Formulario de edición ('editar-anuncio-form') no encontrado.");
        if(loadingMsg) loadingMsg.innerHTML = '<p style="color: red;">Error: Formulario no encontrado.</p>';
        return;
    }
     if (!loadingMsg) {
        console.warn("Elemento de mensaje de carga ('loading-anuncio-msg') no encontrado.");
    }

    const urlParams = new URLSearchParams(window.location.search);
    const anuncioIdParam = urlParams.get('id'); 
    if (!anuncioIdParam) {
        if (loadingMsg) loadingMsg.innerHTML = `<div class="empty-state" style="color: red;"><i class="fas fa-exclamation-circle"></i><p>Error: No se proporcionó un ID de anuncio para editar.</p><p><a href="my-anuncios.html" class="btn btn-secondary">Volver a Mis Anuncios</a></p></div>`;
        editarAnuncioForm.style.display = 'none';
        return;
    }
    
    const idFinalParaApi = anuncioIdParam.startsWith("anuncio_") ? anuncioIdParam.substring("anuncio_".length) : anuncioIdParam;
    console.log(`Editar: ID original del param: ${anuncioIdParam}, ID para API: ${idFinalParaApi}`);

    const jwtToken = localStorage.getItem('jwtToken');
    if (!jwtToken) {
        showCustomAlert("Debes iniciar sesión para editar un anuncio.", "warning");
        setTimeout(() => window.location.href = 'login.html', 2000);
        return;
    }

    if (loadingMsg) loadingMsg.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Cargando datos del anuncio ID: ${idFinalParaApi}...</p></div>`;
    editarAnuncioForm.style.display = 'none'; 

    try {
        const response = await fetch(`${backendUrl}/api/anuncios/${idFinalParaApi}`, { 
             headers: { 'Authorization': `Bearer ${jwtToken}` } 
        }); 
        if (!response.ok) {
            let errorText = `Error HTTP: ${response.status}`;
            try { errorText += ` - ${await response.text()}`; } catch(e) {}
            throw new Error(errorText);
        }
        const anuncio = await response.json();

        populateEditForm(editarAnuncioForm, anuncio);
        if (loadingMsg) loadingMsg.style.display = 'none';
        editarAnuncioForm.style.display = 'block'; 
    } catch (error) {
        console.error('Error al cargar el anuncio para editar:', error);
        if (loadingMsg) loadingMsg.innerHTML = `<div class="empty-state" style="color: red;"><i class="fas fa-exclamation-triangle"></i><p>Error al cargar los datos del anuncio: ${error.message}</p><p><a href="my-anuncios.html" class="btn btn-secondary">Volver a Mis Anuncios</a></p></div>`;
        editarAnuncioForm.style.display = 'none';
    }
}

function populateEditForm(form, anuncio) {
    if (!form || !anuncio) return;

    form.titulo.value = limpiarTituloAnuncio(anuncio.titulo);
    form.descripcionDetallada.value = anuncio.descripcionDetallada || '';
    form.precioMonto.value = anuncio.precioMonto || '';
    form.terminoContrato.value = anuncio.terminoContrato || '';
    
    if (anuncio.fechaDisponibleDesde) {
        try {
            const dateObj = new Date(anuncio.fechaDisponibleDesde);
            const year = dateObj.getUTCFullYear();
            const month = String(dateObj.getUTCMonth() + 1).padStart(2, '0'); 
            const day = String(dateObj.getUTCDate()).padStart(2, '0');
            form.fechaDisponibleDesde.value = `${year}-${month}-${day}`;
        } catch (e) {
            console.warn("Error al parsear fechaDisponibleDesde para el formulario:", anuncio.fechaDisponibleDesde, e);
            form.fechaDisponibleDesde.value = ''; 
        }
    } else {
        form.fechaDisponibleDesde.value = '';
    }

    form.esAnuncioCompartido.checked = !!anuncio.esAnuncioCompartido;
    form.numeroHabitacionesDisponibles.value = anuncio.numeroHabitacionesDisponibles || 1;
    form.numeroBanosDisponibles.value = anuncio.numeroBanosDisponibles || '';
    
    form.direccion.value = anuncio.direccion || '';
    form.barrio.value = anuncio.barrio || '';
    form.latitud.value = anuncio.latitud || '';
    form.longitud.value = anuncio.longitud || '';
    form.tipoVivienda.value = anuncio.tipoVivienda || '';
    form.numeroTotalHabitaciones.value = anuncio.numeroTotalHabitaciones || '';
    form.numeroTotalBanos.value = anuncio.numeroTotalBanos || '';
    form.metrosCuadrados.value = anuncio.metrosCuadrados || '';

    form.querySelectorAll('input[name="comodidades"]').forEach(cb => cb.checked = false);
    
    if (anuncio.comodidades && Array.isArray(anuncio.comodidades)) {
        anuncio.comodidades.forEach(comodidadNombreOCompleto => { 
            let comodidadUriParaCheckbox = comodidadNombreOCompleto;
            if (!comodidadNombreOCompleto.startsWith("http://www.example.org/cercademiurentals#comodidad_")) {
                const nombreLimpio = limpiarTituloAnuncio(comodidadNombreOCompleto.replace(/_/g, ' '));
                comodidadUriParaCheckbox = `http://www.example.org/cercademiurentals#comodidad_${nombreLimpio.toLowerCase().replace(/\s+/g, '_')}`;
            }
            
            const checkbox = form.querySelector(`input[name="comodidades"][value="${comodidadUriParaCheckbox}"]`);
            if (checkbox) {
                checkbox.checked = true;
            } else {
                 console.warn(`Checkbox no encontrado para comodidad: ${comodidadNombreOCompleto} (intentado con ${comodidadUriParaCheckbox})`);
            }
        });
    }

    form.permiteMascotas.checked = !!anuncio.permiteMascotas;
    form.permiteFumar.checked = !!anuncio.permiteFumar;
    form.permiteInvitados.checked = !!anuncio.permiteInvitados;
    form.soloEstudiantes.checked = !!anuncio.soloEstudiantes;
    form.estableceHorasSilencio.checked = !!anuncio.estableceHorasSilencio;
    form.preferenciaGenero.value = anuncio.preferenciaGenero || ""; 
}

async function handleEditarAnuncioForm() {
    const editarAnuncioForm = document.getElementById('editar-anuncio-form');
    if (!editarAnuncioForm) return;

    editarAnuncioForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const urlParams = new URLSearchParams(window.location.search);
        const anuncioIdParam = urlParams.get('id'); 
        
        if (!anuncioIdParam) {
            showCustomAlert("Error: No se pudo identificar el anuncio a actualizar.", "error");
            return;
        }
        const idFinalParaApi = anuncioIdParam.startsWith("anuncio_") ? anuncioIdParam.substring("anuncio_".length) : anuncioIdParam;
        console.log(`Editar Submit: ID original del param: ${anuncioIdParam}, ID para API: ${idFinalParaApi}`);

        const jwtToken = localStorage.getItem('jwtToken');
        if (!jwtToken) { 
            showCustomAlert("Debes iniciar sesión para actualizar un anuncio.", "warning");
            setTimeout(() => window.location.href = 'login.html', 2000);
            return;
        }

        const formData = new FormData(editarAnuncioForm);
        const anuncioData = {
            titulo: formData.get('titulo'),
            descripcionDetallada: formData.get('descripcionDetallada') || null,
            precioMonto: parseFloat(formData.get('precioMonto')),
            terminoContrato: formData.get('terminoContrato'),
            esAnuncioCompartido: formData.has('esAnuncioCompartido'),
            numeroHabitacionesDisponibles: parseInt(formData.get('numeroHabitacionesDisponibles'), 10),
            numeroBanosDisponibles: formData.get('numeroBanosDisponibles') ? parseInt(formData.get('numeroBanosDisponibles'), 10) : null,
            fechaDisponibleDesde: formData.get('fechaDisponibleDesde'),
            direccion: formData.get('direccion'),
            barrio: formData.get('barrio'),
            latitud: parseFloat(formData.get('latitud')),
            longitud: parseFloat(formData.get('longitud')),
            tipoVivienda: formData.get('tipoVivienda'),
            numeroTotalHabitaciones: parseInt(formData.get('numeroTotalHabitaciones'), 10),
            numeroTotalBanos: parseInt(formData.get('numeroTotalBanos'), 10),
            metrosCuadrados: formData.get('metrosCuadrados') ? parseInt(formData.get('metrosCuadrados'), 10) : null,
            comodidadesUris: Array.from(editarAnuncioForm.querySelectorAll('input[name="comodidades"]:checked')).map(cb => cb.value),
            permiteMascotas: formData.has('permiteMascotas'),
            permiteFumar: formData.has('permiteFumar'),
            permiteInvitados: formData.has('permiteInvitados'),
            soloEstudiantes: formData.has('soloEstudiantes'),
            preferenciaGenero: formData.get('preferenciaGenero') || "",
            estableceHorasSilencio: formData.has('estableceHorasSilencio')
        };

        const requiredFields = ['titulo', 'precioMonto', 'terminoContrato', 'numeroHabitacionesDisponibles', 'fechaDisponibleDesde', 'direccion', 'barrio', 'latitud', 'longitud', 'tipoVivienda', 'numeroTotalHabitaciones', 'numeroTotalBanos'];
        for (const field of requiredFields) {
            if (anuncioData[field] === null || anuncioData[field] === undefined || (typeof anuncioData[field] === 'string' && anuncioData[field].trim() === '') || (typeof anuncioData[field] === 'number' && isNaN(anuncioData[field]))) {
                showCustomAlert(`Por favor, completa el campo obligatorio: ${field}`, "error");
                const inputElement = editarAnuncioForm.querySelector(`[name="${field}"]`);
                if (inputElement) inputElement.focus();
                return;
            }
        }

        console.log("Datos del anuncio a actualizar:", JSON.stringify(anuncioData, null, 2));
        try {
            const response = await fetch(`${backendUrl}/api/anuncios/${idFinalParaApi}`, { 
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${jwtToken}` 
                },
                body: JSON.stringify(anuncioData) 
            });
            const responseText = await response.text();
            if (!response.ok) throw new Error(`Error HTTP: ${response.status} - ${responseText}`);
            console.log('Anuncio actualizado:', responseText);
            showCustomAlert('Anuncio actualizado exitosamente.', "success");
            setTimeout(() => window.location.href = 'my-anuncios.html', 1500); 
        } catch (error) {
            console.error('Error al actualizar el anuncio:', error);
            showCustomAlert(`Error al actualizar el anuncio: ${error.message}`, "error");
        }
    });
}

async function loadMyInterestedAnuncios() {
    const myInteresesList = document.getElementById('my-intereses-list');
    if (!myInteresesList) return;

    const jwtToken = localStorage.getItem('jwtToken');
    if (!jwtToken) { 
        myInteresesList.innerHTML = '<div class="empty-state" style="padding: 2rem; text-align:center;"><i class="fas fa-sign-in-alt" style="font-size: 3rem; color: var(--border-color); margin-bottom: 1rem;"></i><p>Debes iniciar sesión para ver tus intereses.</p><p><a href="login.html" class="btn btn-primary">Ir a Login</a></p></div>';
        return;
    }
    myInteresesList.innerHTML = '<div class="loading-state"><div class="spinner"></div><p>Cargando tus intereses...</p></div>';
    try {
        const response = await fetch(`${backendUrl}/api/mis-intereses`, { 
             headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        if (!response.ok) throw new Error(`Error HTTP: ${response.status} - ${await response.text()}`);
        const anunciosInteresantes = await response.json();
        myInteresesList.innerHTML = '';

        if (anunciosInteresantes.length === 0) {
            myInteresesList.innerHTML = `
                <div class="empty-state" style="padding: 2rem; text-align:center;">
                    <i class="fas fa-heart-crack" style="font-size: 3rem; color: var(--border-color); margin-bottom: 1rem;"></i>
                    <p>Aún no has marcado ningún anuncio como interesante.</p>
                    <a href="index.html" class="btn btn-primary"><i class="fas fa-search"></i> ¡Explora los anuncios!</a>
                </div>`;
            return;
        }

        anunciosInteresantes.forEach(anuncio => {
            const anuncioCard = document.createElement('div');
            anuncioCard.classList.add('anuncio-card'); 
            const idLocal = anuncio.uri ? anuncio.uri.substring(BASE_NAMESPACE_ANUNCIO.length) : 'id-desconocido';
            const tituloLimpio = limpiarTituloAnuncio(anuncio.titulo);
            const precioFormateado = anuncio.precio ? anuncio.precio.toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 0 }) : 'N/A';

            anuncioCard.innerHTML = `
                <div class="anuncio-image">
                     <img src="${anuncio.imagenUrl || 'img/default-placeholder.png'}" alt="Imagen de ${tituloLimpio}" onerror="this.onerror=null;this.src='img/default-placeholder.png';">
                </div>
                <div class="anuncio-content">
                    <h3 class="anuncio-titulo">${tituloLimpio}</h3>
                    <p class="anuncio-precio">${precioFormateado}</p>
                    <p><strong>Barrio:</strong> ${anuncio.barrio || 'N/A'}, ${anuncio.ciudad || 'Florencia'}</p>
                     <p><strong>Tipo:</strong> ${anuncio.tipoVivienda || 'N/A'}</p>
                    <div class="anuncio-card-actions">
                        <a href="detalle.html?id=${idLocal}" class="btn btn-primary btn-sm">Ver Detalles</a>
                        <button class="btn btn-danger btn-sm remove-interest-btn" data-anuncio-id="${idLocal}"><i class="fas fa-trash-alt"></i> Quitar Interés</button>
                    </div>
                </div>
            `;
            myInteresesList.appendChild(anuncioCard);
        });

        myInteresesList.querySelectorAll('.remove-interest-btn').forEach(button => button.addEventListener('click', handleRemoveInterest));
    } catch (error) {
        console.error('Error al cargar mis intereses:', error);
        myInteresesList.innerHTML = `<div class="empty-state" style="color: var(--danger-color);"><i class="fas fa-exclamation-triangle"></i><p>Error al cargar tus intereses: ${error.message}.</p></div>`;
    }
}
async function handleMarkAsInterested(event) {
    const anuncioIdLocal = event.target.dataset.anuncioId; 
    const jwtToken = localStorage.getItem('jwtToken');

    if (!jwtToken) { 
        showCustomAlert("Debes iniciar sesión para marcar un anuncio como interesante.", "warning");
        setTimeout(() => window.location.href = 'login.html', 2000);
        return;
    }
    if (!anuncioIdLocal) {
        console.error("No se encontró el ID del anuncio en el botón.");
        showCustomAlert("Error: No se pudo identificar el anuncio.", "error");
        return;
    }

    let tituloParaAlerta = anuncioIdLocal; 
    try { 
        const detalleContainer = document.getElementById('detalle-anuncio-container');
        if (detalleContainer) {
            const h2Titulo = detalleContainer.querySelector('h2');
            if(h2Titulo) tituloParaAlerta = h2Titulo.textContent.split("ID:")[0].trim(); 
        }
    } catch(e){ console.warn("No se pudo obtener título para alerta de interés."); }

    console.log(`Marcando anuncio ${anuncioIdLocal} como interesante.`);
    try {
        const response = await fetch(`${backendUrl}/api/anuncios/${anuncioIdLocal}/interes`, { 
            method: 'POST',
            headers: { 'Authorization': `Bearer ${jwtToken}` } 
        });
        const responseText = await response.text();
        if (!response.ok) throw new Error(`Error HTTP: ${response.status} - ${responseText}`);
        console.log('Interés marcado:', responseText);
        showCustomAlert(`Anuncio '${limpiarTituloAnuncio(tituloParaAlerta)}' añadido a tus intereses.`, "success");
        event.target.innerHTML = '<i class="fas fa-check-circle"></i> ¡Guardado!'; 
        event.target.disabled = true;
        event.target.classList.replace('btn-primary', 'btn-success'); 
    } catch (error) {
        console.error('Error al marcar interés:', error);
        showCustomAlert(`Error al guardar interés: ${error.message}`, "error");
    }
}

async function handleRemoveInterest(event) {
    const anuncioIdLocal = event.target.dataset.anuncioId; 
    const jwtToken = localStorage.getItem('jwtToken');

    if (!jwtToken || !anuncioIdLocal) { 
        showCustomAlert("Error de identificación o acción no permitida. Intenta iniciar sesión de nuevo.", "error");
        return;
    }

    let tituloParaAlerta = anuncioIdLocal; 
     try { 
        const card = event.target.closest('.anuncio-card');
        if (card) {
            const tituloElem = card.querySelector('h3.anuncio-titulo'); 
            if (tituloElem) tituloParaAlerta = tituloElem.textContent;
        }
    } catch(e){ console.warn("No se pudo obtener título para alerta de quitar interés."); }

    if (!await showCustomConfirm(`¿Seguro que quieres quitar el anuncio '${limpiarTituloAnuncio(tituloParaAlerta)}' de tus intereses?`)) return;

    console.log(`Intentando quitar interés en anuncio ID: ${anuncioIdLocal}`);
    try {
        const response = await fetch(`${backendUrl}/api/anuncios/${anuncioIdLocal}/interes`, { 
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${jwtToken}` } 
        });
        const responseText = await response.text();
        if (!response.ok) throw new Error(`Error HTTP: ${response.status} - ${responseText}`);
        console.log('Interés eliminado:', responseText);
        showCustomAlert("Anuncio eliminado de tus intereses.", "success");
        loadMyInterestedAnuncios(); 
    } catch (error) {
        console.error('Error al quitar interés:', error);
        showCustomAlert(`Error al quitar interés: ${error.message}.`, "error");
    }
}

// --- Funciones de Modales Personalizados (reemplazo de alert y confirm) ---
function showCustomConfirm(message) {
    return new Promise((resolve) => {
        const existingModal = document.querySelector('.custom-modal-overlay');
        if (existingModal) existingModal.remove();

        const modalOverlay = document.createElement('div');
        modalOverlay.className = 'custom-modal-overlay';
        
        const modalDialog = document.createElement('div');
        modalDialog.className = 'custom-modal-dialog';
        
        const modalMessage = document.createElement('p');
        modalMessage.textContent = message;
        
        const confirmButton = document.createElement('button');
        confirmButton.textContent = 'Aceptar';
        confirmButton.className = 'btn btn-primary';
        confirmButton.onclick = () => {
            document.body.removeChild(modalOverlay);
            resolve(true);
        };
        
        const cancelButton = document.createElement('button');
        cancelButton.textContent = 'Cancelar';
        cancelButton.className = 'btn btn-secondary';
        cancelButton.onclick = () => {
            document.body.removeChild(modalOverlay);
            resolve(false);
        };
        
        const buttonContainer = document.createElement('div');
        buttonContainer.className = 'custom-modal-actions';
        buttonContainer.appendChild(cancelButton); 
        buttonContainer.appendChild(confirmButton); 
        
        modalDialog.appendChild(modalMessage);
        modalDialog.appendChild(buttonContainer);
        modalOverlay.appendChild(modalDialog);
        document.body.appendChild(modalOverlay);

        requestAnimationFrame(() => {
            modalOverlay.classList.add('visible');
            modalDialog.style.transform = 'scale(1)';
        });
    });
}

function showCustomPrompt(message, defaultValue = '') {
    return new Promise((resolve) => {
        const existingModal = document.querySelector('.custom-modal-overlay');
        if (existingModal) existingModal.remove();

        const modalOverlay = document.createElement('div');
        modalOverlay.className = 'custom-modal-overlay';
        
        const modalDialog = document.createElement('div');
        modalDialog.className = 'custom-modal-dialog';
        
        const modalMessage = document.createElement('p');
        modalMessage.textContent = message;
        
        const inputField = document.createElement('input');
        inputField.type = 'text';
        inputField.value = defaultValue;
        inputField.className = 'form-control'; 
        inputField.style.marginBottom = '1rem';

        const confirmButton = document.createElement('button');
        confirmButton.textContent = 'Aceptar';
        confirmButton.className = 'btn btn-primary';
        confirmButton.onclick = () => {
            document.body.removeChild(modalOverlay);
            resolve(inputField.value);
        };
        
        const cancelButton = document.createElement('button');
        cancelButton.textContent = 'Cancelar';
        cancelButton.className = 'btn btn-secondary';
        cancelButton.onclick = () => {
            document.body.removeChild(modalOverlay);
            resolve(null); 
        };
        
        const buttonContainer = document.createElement('div');
        buttonContainer.className = 'custom-modal-actions';
        buttonContainer.appendChild(cancelButton);
        buttonContainer.appendChild(confirmButton);
        
        modalDialog.appendChild(modalMessage);
        modalDialog.appendChild(inputField);
        modalDialog.appendChild(buttonContainer);
        modalOverlay.appendChild(modalDialog);
        document.body.appendChild(modalOverlay);
        inputField.focus();

        requestAnimationFrame(() => {
            modalOverlay.classList.add('visible');
            modalDialog.style.transform = 'scale(1)';
        });
    });
}