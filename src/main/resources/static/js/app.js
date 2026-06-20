// Global State
let currentProjectId = null;
let pollInterval = null;
let availableImages = [];

// DOM Elements
const views = document.querySelectorAll('.app-view');
const menuItems = document.querySelectorAll('.menu-item');
const viewTitle = document.getElementById('view-title');

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    loadProjects();
    loadModels();
    loadSettings();
    loadAvailableImages();
});

// 1. NAVIGATION & ROUTING
function initNavigation() {
    menuItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const target = item.getAttribute('data-target');
            
            // Update Active Link
            menuItems.forEach(m => m.classList.remove('active'));
            item.classList.add('active');
            
            // Switch View
            switchView(target);
        });
    });
}

function switchView(targetViewId) {
    // Clear Polling when leaving detail view
    if (targetViewId !== 'project-detail-view') {
        stopPolling();
    }

    views.forEach(view => {
        view.classList.remove('active');
    });
    
    const targetView = document.getElementById(targetViewId);
    targetView.classList.add('active');
    
    // Update Header Title
    if (targetViewId === 'dashboard-view') {
        viewTitle.innerText = "Meine Vorträge";
        loadProjects();
    } else if (targetViewId === 'new-project-view') {
        viewTitle.innerText = "Neuen Vortrag erstellen";
        document.getElementById('new-project-form').reset();
        document.getElementById('upload-pptx-form').reset();
        document.getElementById('file-chosen-text').innerText = "Es wurde keine Datei ausgewählt.";
        switchCreateTab('prompt');
    } else if (targetViewId === 'history-view') {
        viewTitle.innerText = "Prompt-Historie";
        loadPromptHistory();
    } else if (targetViewId === 'settings-view') {
        viewTitle.innerText = "API-Einstellungen";
        loadSettings();
    }
}

// Helper to show dashboard
function showDashboard() {
    const dashMenu = document.getElementById('nav-dashboard');
    menuItems.forEach(m => m.classList.remove('active'));
    dashMenu.classList.add('active');
    switchView('dashboard-view');
}

// 2. PROJECT MANAGEMENT
async function loadProjects() {
    const grid = document.getElementById('projects-grid');
    grid.innerHTML = `<div class="loading-state"><i class="fa-solid fa-spinner fa-spin"></i><p>Lade Projekte...</p></div>`;
    
    try {
        const response = await fetch('/api/projects');
        const projects = await response.json();
        
        if (projects.length === 0) {
            grid.innerHTML = `
                <div class="loading-state">
                    <i class="fa-solid fa-folder-open" style="font-size: 40px; opacity: 0.5;"></i>
                    <p>Keine Projekte vorhanden. Lege ein neues an!</p>
                    <button class="btn btn-primary btn-sm mt-2" onclick="switchView('new-project-view')">
                        <i class="fa-solid fa-plus"></i> Erstes Projekt erstellen
                    </button>
                </div>`;
            return;
        }
        
        grid.innerHTML = '';
        projects.forEach(project => {
            const card = document.createElement('div');
            card.className = 'project-card';
            card.onclick = () => showProjectDetail(project.id);
            
            const dateStr = project.createdAt ? new Date(project.createdAt).toLocaleDateString('de-DE') : 'Jetzt';
            const statusClass = project.status.toLowerCase();
            
            const versionBadge = project.version ? `<span class="version-badge-small" style="font-size: 11px; background: rgba(255, 255, 255, 0.1); border: 1px solid var(--border-overlay); padding: 1px 6px; border-radius: 4px; margin-left: 8px; vertical-align: middle; opacity: 0.8; font-weight: 500;">${escapeHtml(project.version)}</span>` : '';
            card.innerHTML = `
                <div class="project-card-header">
                    <h3><i class="fa-solid fa-graduation-cap" style="color: var(--accent-cyan); margin-right: 8px;"></i>${escapeHtml(project.name)}${versionBadge}</h3>
                    <span class="status-badge ${statusClass}">${project.status}</span>
                </div>
                <div class="project-card-body">
                    <p>${escapeHtml(project.prompt)}</p>
                </div>
                <div class="project-card-footer">
                    <span><i class="fa-regular fa-calendar-days"></i> ${dateStr}</span>
                    <div class="card-actions">
                        <button onclick="handleDeleteProject(event, ${project.id})" title="Löschen">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    </div>
                </div>
            `;
            grid.appendChild(card);
        });
        
    } catch (error) {
        grid.innerHTML = `<div class="loading-state"><i class="fa-solid fa-triangle-exclamation" style="color: var(--accent-red)"></i><p>Fehler beim Laden: ${error.message}</p></div>`;
    }
}

async function handleDeleteProject(event, id) {
    event.stopPropagation(); // Avoid triggering card click
    
    if (!confirm("Möchtest du dieses Projekt und alle dazugehörigen Dateien wirklich unwiderruflich löschen?")) {
        return;
    }
    
    try {
        const response = await fetch(`/api/projects/${id}`, { method: 'DELETE' });
        if (response.ok) {
            loadProjects();
        } else {
            alert("Fehler beim Löschen des Projekts.");
        }
    } catch (e) {
        alert("Serverfehler: " + e.message);
    }
}

async function handleCreateProject(event) {
    event.preventDefault();
    
    const name = document.getElementById('project-name').value;
    const model = document.getElementById('project-model').value;
    const prompt = document.getElementById('project-prompt').value;
    
    try {
        const response = await fetch('/api/projects', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, prompt, modelName: model })
        });
        
        if (response.ok) {
            showDashboard();
        } else {
            alert("Projekt konnte nicht erstellt werden.");
        }
    } catch (e) {
        alert("Serverfehler: " + e.message);
    }
}

// 3. PROJECT DETAIL VIEW
async function showProjectDetail(id) {
    currentProjectId = id;
    switchView('project-detail-view');
    viewTitle.innerText = "Projektdetails";
    
    await updateProjectDetailFields();
}

async function updateProjectDetailFields() {
    try {
        const resProject = await fetch(`/api/projects/${currentProjectId}`);
        if (!resProject.ok) throw new Error("Projekt nicht gefunden");
        const project = await resProject.json();
        
        document.getElementById('detail-project-name').innerText = project.name;
        
        const statusBadge = document.getElementById('detail-project-status');
        statusBadge.innerText = project.status;
        statusBadge.className = 'status-badge ' + project.status.toLowerCase();
        
        document.getElementById('detail-project-model').innerText = "Verwendetes Modell: " + (project.modelName || 'anthropic/claude-opus-4.8');
        document.getElementById('detail-project-prompt').innerText = project.prompt;
        
        // Load other versions of this project
        await loadProjectVersions(project);
        
        // Progress Bars & Stage Controls UI mapping
        updatePipelineUI(project);
        
        // Export Actions Bar
        const exportBar = document.getElementById('export-actions-bar');
        const linkPptx = document.getElementById('link-download-pptx');
        const linkVideo = document.getElementById('link-download-video');
        
        if (project.status === 'COMPLETED' || project.status === 'PPTX_READY') {
            exportBar.style.display = 'flex';
            linkPptx.href = `/api/projects/${currentProjectId}/pptx`;
        } else {
            exportBar.style.display = 'none';
        }
        
        if (project.status === 'COMPLETED') {
            linkVideo.style.display = 'inline-flex';
            linkVideo.href = `/api/projects/${currentProjectId}/video`;
            
            // Video Player
            document.getElementById('video-preview-card').style.display = 'block';
            const player = document.getElementById('project-video-player');
            player.src = `/api/projects/${currentProjectId}/video`;
            player.load();
        } else {
            document.getElementById('video-preview-card').style.display = 'none';
        }
        
        // Load slides and custom images
        loadSlidesList();
        await loadProjectImages();
        await loadAvailableImageSets();
        
        // Setup polling if running
        if (project.status === 'GENERATING_PPTX' || project.status === 'GENERATING_AUDIO') {
            startPolling();
        } else {
            stopPolling();
        }
        
    } catch (e) {
        alert("Fehler beim Abrufen der Details: " + e.message);
    }
}

function updatePipelineUI(project) {
    const btnPptx = document.getElementById('btn-generate-pptx');
    const btnAudio = document.getElementById('btn-generate-audio');
    
    const pptxProgress = document.getElementById('pptx-progress');
    const audioProgress = document.getElementById('audio-progress');
    
    const pptxBox = document.getElementById('step-pptx-box');
    const audioBox = document.getElementById('step-audio-box');
    
    // Reset Classes
    pptxBox.classList.remove('processing');
    audioBox.classList.remove('processing');
    pptxProgress.className = 'progress';
    audioProgress.className = 'progress';
    
    // Enable state logic
    btnPptx.disabled = false;
    btnAudio.disabled = true;
    
    if (project.status === 'CREATED') {
        pptxProgress.style.width = '0%';
        audioProgress.style.width = '0%';
    } 
    else if (project.status === 'GENERATING_PPTX') {
        btnPptx.disabled = true;
        pptxBox.classList.add('processing');
        pptxProgress.classList.add('running');
        audioProgress.style.width = '0%';
    } 
    else if (project.status === 'PPTX_READY') {
        pptxProgress.classList.add('completed');
        audioProgress.style.width = '0%';
        btnAudio.disabled = false;
    } 
    else if (project.status === 'GENERATING_AUDIO') {
        btnPptx.disabled = true;
        pptxProgress.classList.add('completed');
        audioBox.classList.add('processing');
        audioProgress.classList.add('running');
    } 
    else if (project.status === 'COMPLETED') {
        pptxProgress.classList.add('completed');
        audioProgress.classList.add('completed');
        btnAudio.disabled = false; // Allow re-run if slides edited
    }
    else if (project.status === 'FAILED') {
        pptxProgress.style.width = '0%';
        audioProgress.style.width = '0%';
        alert("Der letzte Prozessschritt ist fehlgeschlagen. Bitte prüfe die Konsolen-Logs des Servers.");
    }
}

// Actions for stage execution
async function runStage1() {
    try {
        const response = await fetch(`/api/projects/${currentProjectId}/generate-pptx`, { method: 'POST' });
        if (response.ok) {
            updateProjectDetailFields();
        } else {
            alert("Fehler beim Starten von Stufe 1.");
        }
    } catch (e) {
        alert("Fehler: " + e.message);
    }
}

async function runStage2() {
    try {
        const response = await fetch(`/api/projects/${currentProjectId}/generate-audio`, { method: 'POST' });
        if (response.ok) {
            updateProjectDetailFields();
        } else {
            alert("Fehler beim Starten von Stufe 2.");
        }
    } catch (e) {
        alert("Fehler: " + e.message);
    }
}

// Background polling for status updates
function startPolling() {
    if (!currentProjectId) return;
    if (pollInterval) return;
    pollInterval = setInterval(async () => {
        try {
            const response = await fetch(`/api/projects/${currentProjectId}`);
            if (!response.ok) return;
            const p = await response.json();
            
            updatePipelineUI(p);
            
            if (p.status !== 'GENERATING_PPTX' && p.status !== 'GENERATING_AUDIO') {
                stopPolling();
                updateProjectDetailFields(); // refresh full page state
            }
        } catch (e) {
            console.error("Polling error: " + e.message);
        }
    }, 2000);
}

function stopPolling() {
    if (pollInterval) {
        clearInterval(pollInterval);
        pollInterval = null;
    }
}

// SLIDES GRID
async function loadSlidesList() {
    const container = document.getElementById('slides-grid');
    container.innerHTML = '';
    
    try {
        const response = await fetch(`/api/projects/${currentProjectId}/slides`);
        const slides = await response.json();
        
        if (slides.length === 0) {
            container.innerHTML = `
                <div class="loading-state" style="grid-column: 1/-1;">
                    <p>Noch keine Folien generiert. Führe Stufe 1 aus!</p>
                </div>`;
            return;
        }
        
        slides.forEach(slide => {
            const card = document.createElement('div');
            card.className = 'slide-card';
            card.onclick = () => openSlideModal(slide);
            
            // Build bullets text preview
            let bulletsArr = [];
            try { bulletsArr = JSON.parse(slide.bullets); } catch(e) {}
            const bulletsText = bulletsArr.join(' • ');
            
            // Image source with caching token if slides are regenerated
            const imgUrl = `/api/projects/${currentProjectId}/slides/${slide.slideNumber}/image?t=${new Date().getTime()}`;
            
            card.innerHTML = `
                <div class="slide-card-image">
                    <span class="slide-badge">Folie ${slide.slideNumber}</span>
                    <img src="${imgUrl}" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';" alt="Folie ${slide.slideNumber}">
                    <div class="slide-placeholder" style="display: none;">
                        <i class="fa-regular fa-image"></i>
                        <span>Folie wird gerendert</span>
                    </div>
                </div>
                <div class="slide-card-body">
                    <h4>${escapeHtml(slide.title)}</h4>
                    <p>${escapeHtml(bulletsText || 'Keine Aufzählungspunkte')}</p>
                </div>
            `;
            container.appendChild(card);
        });
        
    } catch (e) {
        container.innerHTML = `<p style="color: var(--accent-red)">Fehler beim Laden der Folien: ${e.message}</p>`;
    }
}

// 4. SLIDE EDITOR MODAL
function openSlideModal(slide) {
    document.getElementById('edit-slide-id').value = slide.id;
    document.getElementById('edit-slide-number').value = slide.slideNumber;
    document.getElementById('edit-slide-title').value = slide.title;
    document.getElementById('edit-slide-notes').value = slide.notes;
    
    // Set slide preview image
    const imgElement = document.getElementById('modal-slide-image');
    imgElement.src = `/api/projects/${currentProjectId}/slides/${slide.slideNumber}/image?t=${new Date().getTime()}`;
    
    // Handle Audio preview
    const audioContainer = document.getElementById('slide-audio-player-container');
    const audioElement = document.getElementById('slide-audio-element');
    if (slide.audioFilePath) {
        audioContainer.style.display = 'block';
        audioElement.src = `/api/projects/${currentProjectId}/slides/${slide.slideNumber}/audio`;
    } else {
        audioContainer.style.display = 'none';
        audioElement.src = "";
    }
    
    // Populate Cartoon / Screenshot image list dropdown
    const select = document.getElementById('edit-slide-image-select');
    select.innerHTML = '';
    
    // Custom uploaded project images group
    if (customImages && customImages.length > 0) {
        const optGroupCustom = document.createElement('optgroup');
        optGroupCustom.label = "Eigene Projektbilder / Screenshots";
        customImages.forEach(img => {
            const opt = document.createElement('option');
            opt.value = img.filename;
            opt.innerText = img.filename + (img.description ? ` (${img.description})` : '');
            if (slide.imageName === img.filename) {
                opt.selected = true;
            }
            optGroupCustom.appendChild(opt);
        });
        select.appendChild(optGroupCustom);
    }
    
    // Standard mascot images group
    const optGroupDefault = document.createElement('optgroup');
    optGroupDefault.label = "Standard Grafiken";
    availableImages.forEach(img => {
        const opt = document.createElement('option');
        opt.value = img;
        opt.innerText = img;
        if (slide.imageName === img || (slide.imageName + '.png') === img) {
            opt.selected = true;
        }
        optGroupDefault.appendChild(opt);
    });
    select.appendChild(optGroupDefault);
    
    // Populate bullet points container
    const bulletsContainer = document.getElementById('edit-slide-bullets-container');
    bulletsContainer.innerHTML = '';
    
    let bulletsArr = [];
    try { bulletsArr = JSON.parse(slide.bullets); } catch(e) {}
    
    bulletsArr.forEach(bullet => {
        addBulletField(bullet);
    });
    
    if (bulletsArr.length === 0) {
        addBulletField(''); // show at least one input if empty
    }
    
    // Open modal
    document.getElementById('slide-editor-modal').style.display = 'flex';
}

function addBulletField(val) {
    const container = document.getElementById('edit-slide-bullets-container');
    const div = document.createElement('div');
    div.className = 'bullet-input-row';
    div.innerHTML = `
        <input type="text" class="bullet-input-field" value="${escapeHtml(val)}" required>
        <button type="button" class="btn-remove-bullet" onclick="this.parentNode.remove()" title="Entfernen">
            <i class="fa-solid fa-trash"></i>
        </button>
    `;
    container.appendChild(div);
}

function closeSlideModal() {
    document.getElementById('slide-editor-modal').style.display = 'none';
    const player = document.getElementById('slide-audio-element');
    player.pause();
}

function playSlideAudio() {
    const player = document.getElementById('slide-audio-element');
    player.play();
}

function previewSelectedImage(val) {
    // Optionally change preview icon or mascot on click
    console.log("Mascot selected: " + val);
}

async function handleSaveSlide(event) {
    event.preventDefault();
    
    const slideId = document.getElementById('edit-slide-id').value;
    const number = document.getElementById('edit-slide-number').value;
    const title = document.getElementById('edit-slide-title').value;
    const notes = document.getElementById('edit-slide-notes').value;
    const imageName = document.getElementById('edit-slide-image-select').value;
    
    // Gather bullets
    const inputs = document.querySelectorAll('.bullet-input-field');
    const bulletsArr = [];
    inputs.forEach(input => {
        if (input.value.trim() !== '') {
            bulletsArr.push(input.value.trim());
        }
    });
    
    const payload = {
        title: title,
        notes: notes,
        imageName: imageName,
        bullets: JSON.stringify(bulletsArr)
    };
    
    try {
        const response = await fetch(`/api/projects/${currentProjectId}/slides/${slideId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (response.ok) {
            closeSlideModal();
            updateProjectDetailFields(); // refresh detail view slides and buttons state
        } else {
            alert("Fehler beim Speichern der Folie.");
        }
    } catch(e) {
        alert("Serverfehler: " + e.message);
    }
}

// 5. PROMPT HISTORY
async function loadPromptHistory() {
    const container = document.getElementById('history-list');
    container.innerHTML = `<div class="loading-state"><i class="fa-solid fa-spinner fa-spin"></i><p>Prompt-Historie wird geladen...</p></div>`;
    
    try {
        const response = await fetch('/api/prompt-history');
        const history = await response.json();
        
        if (history.length === 0) {
            container.innerHTML = `<div class="loading-state"><p>Keine Historie vorhanden.</p></div>`;
            return;
        }
        
        container.innerHTML = '';
        history.forEach(item => {
            const card = document.createElement('div');
            card.className = 'history-card glassmorphism';
            
            const dateStr = item.usedAt ? new Date(item.usedAt).toLocaleString('de-DE') : 'Jetzt';
            
            card.innerHTML = `
                <div class="history-card-header">
                    <h4>${escapeHtml(item.title)}</h4>
                    <span class="history-card-time"><i class="fa-solid fa-clock"></i> ${dateStr}</span>
                </div>
                <p>${escapeHtml(item.prompt)}</p>
                <div class="history-actions">
                    <button class="btn btn-secondary btn-sm" onclick="usePromptFromHistory(\`${escapeJsString(item.title)}\`, \`${escapeJsString(item.prompt)}\`)">
                        <i class="fa-solid fa-arrow-right-to-bracket"></i> Diesen Prompt verwenden
                    </button>
                </div>
            `;
            container.appendChild(card);
        });
        
    } catch (e) {
        container.innerHTML = `<p style="color: var(--accent-red)">Fehler beim Laden: ${e.message}</p>`;
    }
}

function usePromptFromHistory(title, prompt) {
    const menuNewProj = document.getElementById('nav-new-project');
    menuItems.forEach(m => m.classList.remove('active'));
    menuNewProj.classList.add('active');
    
    switchView('new-project-view');
    document.getElementById('project-name').value = title;
    document.getElementById('project-prompt').value = prompt;
}

// 6. SETTINGS
async function loadSettings() {
    try {
        const response = await fetch('/api/settings');
        const settings = await response.json();
        
        document.getElementById('setting-openrouter').value = settings.OPENROUTER_API_KEY || "";
        document.getElementById('setting-elevenlabs').value = settings.ELEVENLABS_API_KEY || "";
        document.getElementById('setting-voiceid').value = settings.ELEVENLABS_VOICE_ID || "";
        
    } catch (e) {
        console.error("Fehler beim Laden der Einstellungen: " + e.message);
    }
}

async function handleSaveSettings(event) {
    event.preventDefault();
    
    const openrouter = document.getElementById('setting-openrouter').value;
    const elevenlabs = document.getElementById('setting-elevenlabs').value;
    const voiceid = document.getElementById('setting-voiceid').value;
    
    const payload = {
        OPENROUTER_API_KEY: openrouter,
        ELEVENLABS_API_KEY: elevenlabs,
        ELEVENLABS_VOICE_ID: voiceid
    };
    
    try {
        const response = await fetch('/api/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (response.ok) {
            alert("Einstellungen erfolgreich gespeichert!");
            loadSettings(); // reload to get masked view
        } else {
            alert("Fehler beim Speichern der Einstellungen.");
        }
    } catch (e) {
        alert("Serverfehler: " + e.message);
    }
}

// 7. DYNAMIC API CONFIG DATA
async function loadModels() {
    try {
        const select = document.getElementById('project-model');
        const response = await fetch('/api/models');
        const models = await response.json();
        
        select.innerHTML = '';
        models.forEach(model => {
            const opt = document.createElement('option');
            opt.value = model.id;
            opt.innerText = model.name;
            select.appendChild(opt);
        });
    } catch (e) {
        console.error("Fehler beim Laden der Modelle: " + e.message);
    }
}

async function loadAvailableImages() {
    try {
        const response = await fetch('/api/images');
        availableImages = await response.json();
    } catch (e) {
        console.error("Fehler beim Laden der Bildnamen: " + e.message);
    }
}

// 8. UTILS
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
}

function escapeJsString(str) {
    if (!str) return '';
    return str.replace(/\\/g, '\\\\')
              .replace(/'/g, "\\'")
              .replace(/"/g, '\\"')
              .replace(/\n/g, '\\n')
              .replace(/\r/g, '\\r');
}

// 9. VERSIONING & POWERPOINT UPLOAD HELPERS
async function loadProjectVersions(project) {
    const select = document.getElementById('detail-project-version');
    try {
        const res = await fetch(`/api/projects/${project.id}/versions`);
        if (res.ok) {
            const versions = await res.json();
            select.innerHTML = '';
            versions.forEach(v => {
                const opt = document.createElement('option');
                opt.value = v.id;
                opt.innerText = `${v.version} (${v.status})`;
                if (v.id === project.id) {
                    opt.selected = true;
                }
                select.appendChild(opt);
            });
        }
    } catch (e) {
        console.error("Fehler beim Laden der Versionen: " + e.message);
    }
}

function switchProjectVersion(projectId) {
    showProjectDetail(parseInt(projectId));
}

async function cloneCurrentVersion() {
    if (!currentProjectId) return;
    
    const url = `/api/projects/${currentProjectId}/clone`;
    
    try {
        const res = await fetch(url, { method: 'POST' });
        if (res.ok) {
            const newProject = await res.json();
            alert(`Erfolgreich geklont als Version ${newProject.version}!`);
            showProjectDetail(newProject.id);
        } else {
            alert("Fehler beim Klonen der Version.");
        }
    } catch (e) {
        alert("Fehler: " + e.message);
    }
}

function toggleEditPrompt(show = true) {
    const promptDisplay = document.getElementById('detail-project-prompt');
    const promptEditContainer = document.getElementById('edit-prompt-container');
    const editButton = document.getElementById('btn-edit-prompt');
    const textarea = document.getElementById('edit-project-prompt-input');
    
    if (show) {
        textarea.value = promptDisplay.innerText;
        promptDisplay.style.display = 'none';
        promptEditContainer.style.display = 'block';
        editButton.style.display = 'none';
    } else {
        promptDisplay.style.display = 'block';
        promptEditContainer.style.display = 'none';
        editButton.style.display = 'inline-flex';
    }
}

async function saveProjectPrompt() {
    const textarea = document.getElementById('edit-project-prompt-input');
    const newPrompt = textarea.value.trim();
    if (newPrompt === '') {
        alert("Prompt darf nicht leer sein.");
        return;
    }
    
    try {
        const res = await fetch(`/api/projects/${currentProjectId}/prompt`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt: newPrompt })
        });
        
        if (res.ok) {
            toggleEditPrompt(false);
            updateProjectDetailFields();
        } else {
            alert("Fehler beim Speichern des Prompts.");
        }
    } catch (e) {
        alert("Fehler: " + e.message);
    }
}

function switchCreateTab(tabType) {
    const tabPrompt = document.getElementById('tab-btn-prompt');
    const tabUpload = document.getElementById('tab-btn-upload');
    const formPrompt = document.getElementById('new-project-form');
    const formUpload = document.getElementById('upload-pptx-form');
    
    if (tabType === 'prompt') {
        tabPrompt.classList.add('active');
        tabPrompt.style.color = '#fff';
        tabUpload.classList.remove('active');
        tabUpload.style.color = 'rgba(255,255,255,0.6)';
        formPrompt.style.display = 'block';
        formUpload.style.display = 'none';
    } else {
        tabUpload.classList.add('active');
        tabUpload.style.color = '#fff';
        tabPrompt.classList.remove('active');
        tabPrompt.style.color = 'rgba(255,255,255,0.6)';
        formPrompt.style.display = 'none';
        formUpload.style.display = 'block';
    }
}

function handleFileChange(event) {
    const file = event.target.files[0];
    const textSpan = document.getElementById('file-chosen-text');
    if (file) {
        textSpan.innerText = `Ausgewählt: ${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
    } else {
        textSpan.innerText = "Es wurde keine Datei ausgewählt.";
    }
}

async function handleUploadPptx(event) {
    event.preventDefault();
    
    const fileInput = document.getElementById('pptx-file-input');
    const nameInput = document.getElementById('upload-project-name');
    const btnSubmit = document.getElementById('btn-submit-upload');
    
    if (!fileInput.files || fileInput.files.length === 0) {
        alert("Bitte wähle eine PowerPoint-Datei (.pptx) aus.");
        return;
    }
    
    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append("file", file);
    if (nameInput.value.trim() !== '') {
        formData.append("name", nameInput.value.trim());
    }
    
    btnSubmit.disabled = true;
    btnSubmit.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Importiere PowerPoint...`;
    
    try {
        const res = await fetch('/api/projects/upload-pptx', {
            method: 'POST',
            body: formData
        });
        
        if (res.ok) {
            alert("PowerPoint erfolgreich importiert und Folien gerendert!");
            document.getElementById('upload-pptx-form').reset();
            document.getElementById('file-chosen-text').innerText = "Es wurde keine Datei ausgewählt.";
            showDashboard();
        } else {
            alert("Fehler beim Importieren der PowerPoint-Präsentation. Bitte prüfe das Server-Log.");
        }
    } catch (e) {
        alert("Serverfehler: " + e.message);
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = `<i class="fa-solid fa-cloud-arrow-up"></i> PowerPoint importieren`;
    }
}

// 10. CUSTOM PROJECT IMAGES
let customImages = [];

async function loadProjectImages() {
    const grid = document.getElementById('project-images-grid');
    if (!currentProjectId) return;
    
    try {
        const res = await fetch(`/api/projects/${currentProjectId}/custom-images`);
        customImages = await res.json();
        
        grid.innerHTML = '';
        if (customImages.length === 0) {
            grid.innerHTML = `
                <div class="loading-state" style="grid-column: 1/-1; padding: 20px;">
                    <p style="font-size: 13px; opacity: 0.6;"><i class="fa-solid fa-image" style="margin-right: 6px;"></i>Keine eigenen Bilder hochgeladen. Nutze das obige Formular zum Hochladen.</p>
                </div>`;
            return;
        }
        
        customImages.forEach(img => {
            const card = document.createElement('div');
            card.className = 'glassmorphism';
            card.style.cssText = 'padding: 12px; border-radius: 8px; border: 1px solid var(--border-overlay); display: flex; flex-direction: column; gap: 8px; position: relative;';
            
            const imgUrl = `/api/projects/${currentProjectId}/custom-images/file/${img.filename}`;
            
            card.innerHTML = `
                <div style="height: 120px; border-radius: 6px; overflow: hidden; background: #000; display: flex; align-items: center; justify-content: center; position: relative;">
                    <img src="${imgUrl}" style="max-height: 100%; max-width: 100%; object-fit: contain;">
                    <button onclick="handleDeleteProjectImage(event, ${img.id})" style="position: absolute; top: 6px; right: 6px; background: rgba(220,53,69,0.8); border: none; border-radius: 4px; color: white; width: 26px; height: 26px; cursor: pointer; display: flex; align-items: center; justify-content: center; z-index: 10;" title="Bild löschen">
                        <i class="fa-solid fa-trash-can" style="font-size: 12px;"></i>
                    </button>
                </div>
                <div style="flex: 1; display: flex; flex-direction: column; gap: 4px;">
                    <span style="font-size: 11px; color: var(--accent-cyan); font-weight: 600; word-break: break-all;">${escapeHtml(img.filename)}</span>
                    <textarea onchange="handleUpdateImageDesc(${img.id}, this.value)" style="font-size: 11px; color: rgba(255,255,255,0.7); background: rgba(0,0,0,0.2); border: 1px solid var(--border-overlay); padding: 4px; border-radius: 4px; resize: vertical; height: 40px; font-family: inherit;" placeholder="Beschreibung für KI hinzufügen...">${escapeHtml(img.description || '')}</textarea>
                </div>
            `;
            grid.appendChild(card);
        });
    } catch (e) {
        console.error("Fehler beim Laden der Projektbilder: " + e.message);
    }
}

async function handleUploadProjectImage(event) {
    event.preventDefault();
    if (!currentProjectId) return;
    
    const fileInput = document.getElementById('project-image-file');
    const descInput = document.getElementById('project-image-desc');
    const btn = document.getElementById('btn-upload-project-image');
    
    if (!fileInput.files || fileInput.files.length === 0) {
        alert("Bitte wähle ein Bild aus.");
        return;
    }
    
    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append("file", file);
    formData.append("description", descInput.value.trim());
    
    btn.disabled = true;
    btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Lädt...`;
    
    try {
        const res = await fetch(`/api/projects/${currentProjectId}/custom-images`, {
            method: 'POST',
            body: formData
        });
        
        if (res.ok) {
            fileInput.value = '';
            descInput.value = '';
            await loadProjectImages();
        } else {
            alert("Fehler beim Hochladen des Bildes.");
        }
    } catch (e) {
        alert("Fehler: " + e.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = `<i class="fa-solid fa-cloud-arrow-up"></i> Hochladen`;
    }
}

async function handleUpdateImageDesc(imageId, newDesc) {
    if (!currentProjectId) return;
    try {
        const res = await fetch(`/api/projects/${currentProjectId}/custom-images/${imageId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ description: newDesc })
        });
        if (!res.ok) {
            console.error("Fehler beim Speichern der Bildbeschreibung.");
        }
    } catch (e) {
        console.error("Fehler: " + e.message);
    }
}

async function handleDeleteProjectImage(event, imageId) {
    event.stopPropagation();
    if (!confirm("Möchtest du dieses Bild wirklich löschen?")) return;
    
    try {
        const res = await fetch(`/api/projects/${currentProjectId}/custom-images/${imageId}`, {
            method: 'DELETE'
        });
        if (res.ok) {
            await loadProjectImages();
        } else {
            alert("Fehler beim Löschen des Bildes.");
        }
    } catch (e) {
        alert("Fehler: " + e.message);
    }
}

// 10. SCREENSHOT PREVIEW MODAL HELPERS
function openScreenshotPreview(src, alt) {
    document.getElementById('screenshot-preview-img').src = src;
    document.getElementById('screenshot-preview-caption').textContent = alt;
    document.getElementById('screenshot-preview-modal').style.display = 'flex';
}

function closeScreenshotPreview() {
    document.getElementById('screenshot-preview-modal').style.display = 'none';
}

// 11. REUSABLE IMAGE SETS HELPERS
async function loadAvailableImageSets() {
    const select = document.getElementById('image-set-import-select');
    if (!select) return;
    
    try {
        const res = await fetch('/api/image-sets');
        if (res.ok) {
            const sets = await res.json();
            select.innerHTML = '<option value="">-- Set auswählen --</option>';
            sets.forEach(set => {
                const opt = document.createElement('option');
                opt.value = set.id;
                opt.innerText = set.name;
                select.appendChild(opt);
            });
        }
    } catch (e) {
        console.error("Fehler beim Laden der Bilder-Sets: " + e.message);
    }
}

async function handleSaveImageSet() {
    if (!currentProjectId) return;
    
    const input = document.getElementById('image-set-save-name');
    const name = input.value.trim();
    if (!name) {
        alert("Bitte gib einen Namen für das Bilder-Set ein.");
        return;
    }
    
    try {
        const res = await fetch('/api/image-sets', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: name, projectId: currentProjectId })
        });
        
        if (res.ok) {
            alert("Bilder-Set erfolgreich gespeichert!");
            input.value = '';
            await loadAvailableImageSets();
        } else {
            alert("Fehler beim Speichern des Bilder-Sets. Vergewissere dich, dass das Projekt Bilder enthält.");
        }
    } catch (e) {
        alert("Fehler: " + e.message);
    }
}

async function handleImportImageSet() {
    if (!currentProjectId) return;
    
    const select = document.getElementById('image-set-import-select');
    const setId = select.value;
    if (!setId) {
        alert("Bitte wähle ein Bilder-Set aus.");
        return;
    }
    
    try {
        const res = await fetch(`/api/image-sets/${setId}/import?projectId=${currentProjectId}`, {
            method: 'POST'
        });
        
        if (res.ok) {
            alert("Bilder-Set erfolgreich importiert!");
            select.value = '';
            await loadProjectImages(); // reload custom images grid
        } else {
            alert("Fehler beim Importieren des Bilder-Sets.");
        }
    } catch (e) {
        alert("Fehler: " + e.message);
    }
}

async function handleDeleteImageSet() {
    const select = document.getElementById('image-set-import-select');
    const setId = select.value;
    if (!setId) {
        alert("Bitte wähle ein Bilder-Set aus, das gelöscht werden soll.");
        return;
    }
    
    if (!confirm("Möchtest du dieses Bilder-Set wirklich dauerhaft aus der Datenbank löschen? (Die Bilder in bestehenden Projekten bleiben erhalten)")) {
        return;
    }
    
    try {
        const res = await fetch(`/api/image-sets/${setId}`, {
            method: 'DELETE'
        });
        
        if (res.ok) {
            alert("Bilder-Set gelöscht!");
            select.value = '';
            await loadAvailableImageSets();
        } else {
            alert("Fehler beim Löschen des Bilder-Sets.");
        }
    } catch (e) {
        alert("Fehler: " + e.message);
    }
}

