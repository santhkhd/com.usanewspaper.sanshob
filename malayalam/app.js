/**
 * Premium Cinematic Vault - Global App Logic
 * Handle state, routing, and dynamic rendering for all pages.
 */

// --- State Management ---
const state = {
    allMovies: [],
    loading: true,
    favorites: JSON.parse(localStorage.getItem('imdbFavorites_premium_v1') || '[]'),
    params: new URLSearchParams(window.location.search),
    theme: localStorage.getItem('imdbTheme_premium') || 'dark',
    searchTerm: '',
    page: 1,
    pageSize: 36,
    peoplePage: 1,
    peoplePageSize: 48,
    activePeopleTab: 'cast', // 'cast' or 'director'
    filters: {
        sortBy: 'rating_desc',
        genre: null,
        era: null,
        star: null,
        minRating: 0
    },
    wikiArtists: {}
};

const app = document.getElementById('app');

// --- Utils ---

const getHighResImage = (url) => {
    if (!url) return null;
    try {
        if (url.includes("media-amazon.com") || url.includes("imdb.com")) {
            return url.replace(/_V1_.*(\.[a-zA-Z]+)$/, "_V1_QL75_UX1000_$1");
        }
    } catch (e) { console.warn("Image upgrade failed", e); }
    return url;
};

const parseRuntime = (str) => {
    if (!str) return 0;
    const match = str.match(/(\d+)/);
    return match ? parseInt(match[1], 10) : 0;
};

const parseReleaseDate = (str, year) => {
    if (str) {
        const d = new Date(str);
        if (!isNaN(d.getTime())) return d.getTime();
    }
    if (year) return new Date(year, 0, 1).getTime();
    return 0;
};

const normalizeMovie = (item) => {
    const runtimeMins = parseRuntime(item.runtime);
    const releaseTimestamp = parseReleaseDate(item.released, item.year);
    const cast = Array.isArray(item.cast) ? item.cast : (item.cast ? item.cast.split(',').map(s => s.trim()) : []);
    const director = Array.isArray(item.director) ? item.director.join(', ') : (item.director || "Unknown Director");
    const genre = Array.isArray(item.genre) ? item.genre : (item.genre ? item.genre.split(',').map(s => s.trim()) : []);
    const rawPoster = item.image || item.poster || null;

    return {
        id: item._id || item.index || crypto.randomUUID(),
        title: item.title?.trim() || "Untitled",
        year: Number(item.year) || null,
        rating: Number(item.rating) || null,
        genre,
        runtime: item.runtime || "N/A",
        runtimeMins,
        released: item.released || "",
        releaseTimestamp,
        plot: item.plot || "No scientific analysis available for this cinematic entry.",
        director,
        cast,
        poster: getHighResImage(rawPoster)
    };
};

const debounce = (func, wait) => {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => func(...args), wait);
    };
};

const updateMetaTags = (title, description, keywords = "") => {
    document.title = `${title} - Premium Cinematic Vault`;
    const highCPC = "Premium Streaming, VOD Platform, Digital Cinema Rights, Subscription Services, Movie Reviews, High Definition Content";
    let desc = document.querySelector('meta[name="description"]');
    if (!desc) { desc = document.createElement('meta'); desc.name = "description"; document.head.appendChild(desc); }
    desc.content = description;
    let key = document.querySelector('meta[name="keywords"]');
    if (!key) { key = document.createElement('meta'); key.name = "keywords"; document.head.appendChild(key); }
    key.content = keywords ? `${keywords}, ${highCPC}` : highCPC;
};

const generateMovieDeepContent = (movie) => {
    return `The ${movie.year} production, "${movie.title}", represents a critical milestone in modern cinematic history. Directed by ${movie.director}, film explores deep thematic elements that resonate with global audiences. From a technical perspective, the Dolby Atmos mastering and 4K-ready visual architecture ensure a premium viewing experience across all digital VOD platforms. It is widely considered an essential masterpiece for any serious collector of professional cinematography.`;
};

// --- Filter & Sort Configuration & Logic ---

const SORT_OPTIONS = [
    { id: 'rating_desc', label: 'Rating (High)', icon: 'star', desc: 'Highest rated first' },
    { id: 'year_desc', label: 'Newest First', icon: 'sparkles', desc: '2024 to classic releases' },
    { id: 'year_asc', label: 'Oldest First', icon: 'history', desc: 'Earliest cinema beginnings' },
    { id: 'title_asc', label: 'Title (A - Z)', icon: 'arrow-down-a-z', desc: 'Alphabetical ascending' },
    { id: 'title_desc', label: 'Title (Z - A)', icon: 'arrow-up-z-a', desc: 'Alphabetical descending' },
    { id: 'runtime_desc', label: 'Run Time', icon: 'clock', desc: 'Longest duration first' }
];

const ERA_OPTIONS = [
    { id: 'all', label: 'All Eras' },
    { id: '2024_2025', label: '2024 - 2025 (Latest)' },
    { id: '2020_2023', label: '2020 - 2023 (Modern)' },
    { id: '2010s', label: '2010s (New Wave)' },
    { id: '2000s', label: '2000s (Millennium)' },
    { id: '1990s', label: '1990s (Golden 90s)' },
    { id: 'classics', label: 'Vintage Classics (< 1990)' }
];

const GENRE_OPTIONS = [
    'Action', 'Comedy', 'Drama', 'Thriller', 'Crime', 'Romance',
    'Horror', 'Mystery', 'Family', 'Adventure', 'Sci-Fi', 'Musical', 'Fantasy'
];

const STAR_OPTIONS = [
    'Mohanlal', 'Mammootty', 'Fahadh Faasil', 'Dulquer Salmaan', 'Prithviraj',
    'Tovino Thomas', 'Nivin Pauly', 'Manju Warrier', 'Dileep', 'Kunchacko Boban',
    'Biju Menon', 'Asif Ali', 'Urvashi', 'Shobana', 'Suresh Gopi', 'Jayasurya', 'Jayaram'
];

const RATING_OPTIONS = [
    { val: 0, label: 'Any Rating' },
    { val: 6.0, label: '6.0+ ⭐' },
    { val: 7.0, label: '7.0+ ⭐' },
    { val: 7.5, label: '7.5+ ⭐' },
    { val: 8.0, label: '8.0+ ⭐ Top Rated' },
    { val: 8.5, label: '8.5+ ⭐ Masterpiece' }
];

const getSortLabel = (sortBy) => {
    const opt = SORT_OPTIONS.find(o => o.id === sortBy);
    return opt ? opt.label : 'Rating';
};

const getEraLabel = (era) => {
    const opt = ERA_OPTIONS.find(o => o.id === era);
    return opt ? opt.label : era;
};

const getActiveFiltersCount = (filters = state.filters) => {
    let count = 0;
    if (filters.genre) count++;
    if (filters.era && filters.era !== 'all') count++;
    if (filters.star) count++;
    if (filters.minRating && filters.minRating > 0) count++;
    if (filters.sortBy && filters.sortBy !== 'rating_desc') count++;
    return count;
};

const getSortedMoviesWithKey = (movies, sortBy = 'rating_desc') => {
    const [key, dir] = (sortBy || 'rating_desc').split('_');
    return [...movies].sort((a, b) => {
        let valA = a[key];
        let valB = b[key];

        if (key === 'runtime') { valA = a.runtimeMins || 0; valB = b.runtimeMins || 0; }
        if (key === 'released' || key === 'year') { valA = a.releaseTimestamp || 0; valB = b.releaseTimestamp || 0; }
        if (key === 'title') { valA = (valA || "").toLowerCase(); valB = (valB || "").toLowerCase(); }
        if (key === 'rating') { valA = valA || 0; valB = valB || 0; }

        if (valA < valB) return dir === 'asc' ? -1 : 1;
        if (valA > valB) return dir === 'asc' ? 1 : -1;

        if (key !== 'title') {
            const titleA = (a.title || "").toLowerCase();
            const titleB = (b.title || "").toLowerCase();
            if (titleA < titleB) return -1;
            if (titleA > titleB) return 1;
        }
        return 0;
    });
};

const getSortedMovies = (movies) => {
    return getSortedMoviesWithKey(movies, state.filters.sortBy || 'rating_desc');
};

const getFilteredAndSortedMovies = (baseList = state.allMovies, filters = state.filters) => {
    let list = baseList || [];

    // 1. Search Query
    if (state.searchTerm && state.searchTerm.trim()) {
        const q = state.searchTerm.trim().toLowerCase();
        list = list.filter(m => {
            const inTitle = m.title && m.title.toLowerCase().includes(q);
            const inCast = m.cast && m.cast.some(c => c.toLowerCase().includes(q));
            const inDir = m.director && m.director.toLowerCase().includes(q);
            const inPlot = m.plot && m.plot.toLowerCase().includes(q);
            const inYear = m.year && String(m.year).includes(q);
            return inTitle || inCast || inDir || inPlot || inYear;
        });
    }

    // 2. Genre Filter
    if (filters.genre) {
        const g = filters.genre.toLowerCase();
        list = list.filter(m => m.genre && m.genre.some(item => item.toLowerCase() === g));
    }

    // 3. Era Filter
    if (filters.era && filters.era !== 'all') {
        list = list.filter(m => {
            if (!m.year) return false;
            const y = m.year;
            switch (filters.era) {
                case '2024_2025': return y >= 2024;
                case '2020_2023': return y >= 2020 && y <= 2023;
                case '2010s': return y >= 2010 && y <= 2019;
                case '2000s': return y >= 2000 && y <= 2009;
                case '1990s': return y >= 1990 && y <= 1999;
                case 'classics': return y < 1990;
                default: return true;
            }
        });
    }

    // 4. Star Filter
    if (filters.star) {
        const s = filters.star.toLowerCase();
        list = list.filter(m => m.cast && m.cast.some(c => c.toLowerCase().includes(s)));
    }

    // 5. Min Rating Filter
    if (filters.minRating && filters.minRating > 0) {
        list = list.filter(m => m.rating && m.rating >= filters.minRating);
    }

    // 6. Sort
    return getSortedMoviesWithKey(list, filters.sortBy || 'rating_desc');
};

// --- Beautiful Glassmorphic Filter & Sort Modal ---
let modalDraftFilters = { ...state.filters };
let activeGridCallback = null;

const ensureModalContainer = () => {
    let container = document.getElementById('filterModalContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'filterModalContainer';
        document.body.appendChild(container);
    }
    return container;
};

const openFilterModal = (callback = null) => {
    activeGridCallback = callback;
    modalDraftFilters = { ...state.filters };
    renderModalContent();
};

const closeFilterModal = () => {
    const container = document.getElementById('filterModalContainer');
    if (container) container.innerHTML = '';
};

const renderModalContent = () => {
    const container = ensureModalContainer();
    const matchCount = getFilteredAndSortedMovies(state.allMovies, modalDraftFilters).length;

    container.innerHTML = `
        <div id="filterBackdrop" class="fixed inset-0 z-[100] bg-black/85 backdrop-blur-md flex items-end sm:items-center justify-center p-0 sm:p-4 fade-in">
            <div id="filterDialogCard" class="w-full sm:max-w-2xl max-h-[90vh] sm:max-h-[85vh] bg-[#0c0d14] border border-white/10 rounded-t-[2.5rem] sm:rounded-[2.5rem] shadow-2xl flex flex-col overflow-hidden animate-slide-up text-slate-100">
                
                <!-- Mobile Drag Indicator -->
                <div class="sm:hidden pt-3 pb-1 flex justify-center">
                    <div class="w-12 h-1.5 bg-white/20 rounded-full"></div>
                </div>

                <!-- Modal Header -->
                <div class="px-6 sm:px-8 py-5 flex items-center justify-between border-b border-white/5 bg-slate-900/40">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-2xl premium-gradient flex items-center justify-center text-white shadow-lg shadow-primary-500/30">
                            <i data-lucide="sliders-horizontal" class="w-5 h-5"></i>
                        </div>
                        <div>
                            <h3 class="font-display font-black text-lg sm:text-xl tracking-tight leading-none text-white">Filter & Sort Vault</h3>
                            <p class="text-[10px] font-bold uppercase tracking-widest text-primary-400 mt-1">6,150+ Malayalam Cinema Archives</p>
                        </div>
                    </div>
                    <button id="closeFilterModalBtn" type="button" class="w-10 h-10 rounded-full glass hover:bg-white/10 flex items-center justify-center text-slate-400 hover:text-white transition-colors">
                        <i data-lucide="x" class="w-5 h-5"></i>
                    </button>
                </div>

                <!-- Scrollable Body -->
                <div class="flex-1 overflow-y-auto filter-scroll px-6 sm:px-8 py-6 space-y-7 text-xs">
                    
                    <!-- Section 1: Sort By -->
                    <div class="space-y-3">
                        <span class="font-display font-black uppercase tracking-widest text-slate-400 text-[11px] flex items-center gap-2">
                            <i data-lucide="arrow-up-down" class="w-3.5 h-3.5 text-primary-500"></i> Sort Movies By
                        </span>
                        <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
                            ${SORT_OPTIONS.map(opt => {
                                const active = modalDraftFilters.sortBy === opt.id;
                                return `
                                    <button type="button" data-modal-sort="${opt.id}" class="chip-pill text-left px-3.5 py-3 rounded-2xl border flex items-center gap-2.5 transition-all ${active ? 'premium-gradient text-white border-primary-500/50 shadow-md shadow-primary-500/20 font-bold' : 'bg-white/5 border-white/5 text-slate-300 hover:bg-white/10 hover:border-white/15'}">
                                        <i data-lucide="${opt.icon}" class="w-4 h-4 ${active ? 'text-white' : 'text-slate-400'}"></i>
                                        <span class="truncate">${opt.label}</span>
                                    </button>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    <!-- Section 2: Release Era / Decade -->
                    <div class="space-y-3">
                        <span class="font-display font-black uppercase tracking-widest text-slate-400 text-[11px] flex items-center gap-2">
                            <i data-lucide="calendar" class="w-3.5 h-3.5 text-primary-500"></i> Release Era / Decade
                        </span>
                        <div class="flex flex-wrap gap-2">
                            ${ERA_OPTIONS.map(era => {
                                const active = (modalDraftFilters.era === era.id) || (!modalDraftFilters.era && era.id === 'all');
                                return `
                                    <button type="button" data-modal-era="${era.id}" class="chip-pill px-4 py-2 rounded-full border text-xs transition-all ${active ? 'premium-gradient text-white border-primary-500 font-bold shadow-md shadow-primary-500/20' : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10'}">
                                        ${era.label}
                                    </button>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    <!-- Section 3: Genre Filter -->
                    <div class="space-y-3">
                        <span class="font-display font-black uppercase tracking-widest text-slate-400 text-[11px] flex items-center gap-2">
                            <i data-lucide="clapperboard" class="w-3.5 h-3.5 text-primary-500"></i> Genre
                        </span>
                        <div class="flex flex-wrap gap-2">
                            <button type="button" data-modal-genre="all" class="chip-pill px-4 py-2 rounded-full border text-xs transition-all ${!modalDraftFilters.genre ? 'premium-gradient text-white border-primary-500 font-bold shadow-md shadow-primary-500/20' : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10'}">
                                All Genres
                            </button>
                            ${GENRE_OPTIONS.map(g => {
                                const active = modalDraftFilters.genre === g;
                                return `
                                    <button type="button" data-modal-genre="${g}" class="chip-pill px-4 py-2 rounded-full border text-xs transition-all ${active ? 'premium-gradient text-white border-primary-500 font-bold shadow-md shadow-primary-500/20' : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10'}">
                                        ${g}
                                    </button>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    <!-- Section 4: Top Malayalam Stars & Icons -->
                    <div class="space-y-3">
                        <span class="font-display font-black uppercase tracking-widest text-slate-400 text-[11px] flex items-center gap-2">
                            <i data-lucide="sparkle" class="w-3.5 h-3.5 text-primary-500"></i> Top Stars & Icons
                        </span>
                        <div class="flex flex-wrap gap-2">
                            <button type="button" data-modal-star="all" class="chip-pill px-4 py-2 rounded-full border text-xs transition-all ${!modalDraftFilters.star ? 'premium-gradient text-white border-primary-500 font-bold shadow-md shadow-primary-500/20' : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10'}">
                                All Stars
                            </button>
                            ${STAR_OPTIONS.map(star => {
                                const active = modalDraftFilters.star === star;
                                return `
                                    <button type="button" data-modal-star="${star}" class="chip-pill px-4 py-2 rounded-full border text-xs transition-all ${active ? 'premium-gradient text-white border-primary-500 font-bold shadow-md shadow-primary-500/20' : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10'}">
                                        ${star}
                                    </button>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    <!-- Section 5: Minimum Rating -->
                    <div class="space-y-3">
                        <span class="font-display font-black uppercase tracking-widest text-slate-400 text-[11px] flex items-center gap-2">
                            <i data-lucide="star" class="w-3.5 h-3.5 text-accent"></i> Minimum IMDb Rating
                        </span>
                        <div class="flex flex-wrap gap-2">
                            ${RATING_OPTIONS.map(r => {
                                const active = (modalDraftFilters.minRating || 0) === r.val;
                                return `
                                    <button type="button" data-modal-rating="${r.val}" class="chip-pill px-4 py-2 rounded-full border text-xs transition-all ${active ? 'premium-gradient text-white border-primary-500 font-bold shadow-md shadow-primary-500/20' : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10'}">
                                        ${r.label}
                                    </button>
                                `;
                            }).join('')}
                        </div>
                    </div>

                </div>

                <!-- Footer Actions -->
                <div class="px-6 sm:px-8 py-4 border-t border-white/5 bg-[#090a10] flex items-center justify-between gap-4">
                    <button id="modalResetBtn" type="button" class="px-4 py-3 text-xs font-black uppercase tracking-widest text-slate-400 hover:text-white transition-colors">
                        Reset All
                    </button>
                    <button id="modalApplyBtn" type="button" class="flex-1 sm:flex-initial px-8 py-3.5 rounded-[1.5rem] premium-gradient text-white font-display font-black text-xs uppercase tracking-wider shadow-xl shadow-primary-500/25 hover:scale-[1.02] active:scale-95 transition-all">
                        Apply (${matchCount} Movies)
                    </button>
                </div>

            </div>
        </div>
    `;

    attachModalListeners();
    if (window.lucide) lucide.createIcons();
};

const attachModalListeners = () => {
    const backdrop = document.getElementById('filterBackdrop');
    if (backdrop) {
        backdrop.addEventListener('click', (e) => {
            if (e.target === backdrop) closeFilterModal();
        });
    }

    const closeBtn = document.getElementById('closeFilterModalBtn');
    if (closeBtn) closeBtn.addEventListener('click', closeFilterModal);

    // Sort buttons
    document.querySelectorAll('[data-modal-sort]').forEach(btn => {
        btn.addEventListener('click', () => {
            modalDraftFilters.sortBy = btn.getAttribute('data-modal-sort');
            renderModalContent();
        });
    });

    // Era buttons
    document.querySelectorAll('[data-modal-era]').forEach(btn => {
        btn.addEventListener('click', () => {
            const val = btn.getAttribute('data-modal-era');
            modalDraftFilters.era = (val === 'all' || modalDraftFilters.era === val) ? null : val;
            renderModalContent();
        });
    });

    // Genre buttons
    document.querySelectorAll('[data-modal-genre]').forEach(btn => {
        btn.addEventListener('click', () => {
            const val = btn.getAttribute('data-modal-genre');
            modalDraftFilters.genre = (val === 'all' || modalDraftFilters.genre === val) ? null : val;
            renderModalContent();
        });
    });

    // Star buttons
    document.querySelectorAll('[data-modal-star]').forEach(btn => {
        btn.addEventListener('click', () => {
            const val = btn.getAttribute('data-modal-star');
            modalDraftFilters.star = (val === 'all' || modalDraftFilters.star === val) ? null : val;
            renderModalContent();
        });
    });

    // Rating buttons
    document.querySelectorAll('[data-modal-rating]').forEach(btn => {
        btn.addEventListener('click', () => {
            const val = parseFloat(btn.getAttribute('data-modal-rating')) || 0;
            modalDraftFilters.minRating = (modalDraftFilters.minRating === val) ? 0 : val;
            renderModalContent();
        });
    });

    // Reset button
    const resetBtn = document.getElementById('modalResetBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            modalDraftFilters = { sortBy: 'rating_desc', genre: null, era: null, star: null, minRating: 0 };
            renderModalContent();
        });
    }

    // Apply button
    const applyBtn = document.getElementById('modalApplyBtn');
    if (applyBtn) {
        applyBtn.addEventListener('click', () => {
            state.filters = { ...modalDraftFilters };
            state.page = 1;
            closeFilterModal();
            if (activeGridCallback) activeGridCallback();
            else route(true);
        });
    }
};

// --- Components ---

const movieCard = (movie) => {
    const genresStr = movie.genre.slice(0, 2).join(' • ');
    const ratingHtml = movie.rating ? `
        <div class="flex items-center gap-1 text-accent text-xs font-black shadow-sm">
            <i data-lucide="star" class="w-3 h-3 fill-current"></i>
            <span>${movie.rating}</span>
        </div>
    ` : '';
    const imageHtml = movie.poster ?
        `<img src="${movie.poster}" alt="${movie.title}" loading="lazy" class="absolute inset-0 w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"><div class="absolute inset-0 bg-gradient-to-t from-black/90 via-black/40 to-transparent"></div>` :
        `<div class="absolute inset-0 movie-card-placeholder"></div><div class="absolute inset-0 premium-gradient opacity-0 group-hover:opacity-10 transition-opacity"></div>`;

    return `
        <a href="details.html?id=${movie.id}" class="group relative block fade-in">
            <div class="aspect-[2/3] w-full relative rounded-[2rem] overflow-hidden shadow-xl group-hover:shadow-primary-500/20 group-hover:-translate-y-2 transition-all duration-500 border border-white/5 bg-slate-900">
                ${imageHtml}
                <div class="absolute inset-0 p-6 flex flex-col justify-between">
                    <div class="flex justify-between items-start relative z-10">
                        <span class="text-[10px] font-black tracking-widest ${movie.poster ? 'text-white/80' : 'text-slate-500'} uppercase group-hover:text-white transition-all">${movie.year || 'TBA'}</span>
                        ${ratingHtml}
                    </div>
                    <div class="space-y-3 relative z-10">
                        <div class="w-8 h-1 bg-primary-500 rounded-full group-hover:w-16 transition-all duration-500"></div>
                        <h3 class="text-white font-display font-extrabold text-lg sm:text-xl leading-tight line-clamp-3 group-hover:text-primary-400 transition-colors drop-shadow-lg">${movie.title}</h3>
                        <p class="text-[10px] ${movie.poster ? 'text-slate-300' : 'text-slate-500'} font-bold uppercase tracking-[0.2em] transition-colors truncate">${genresStr || 'Cinematic'}</p>
                    </div>
                </div>
                <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-500 translate-y-4 group-hover:translate-y-0 z-20">
                    <div class="w-14 h-14 rounded-full glass flex items-center justify-center text-white shadow-2xl"><i data-lucide="play" class="w-6 h-6 fill-current ml-1"></i></div>
                </div>
            </div>
        </a>
    `;
};

const renderSearchAndFilter = (title, showSort = false) => {
    const activeCount = getActiveFiltersCount();

    const filterSortHtml = showSort ? `
        <button id="openFilterBtn" type="button" class="glass rounded-[2rem] px-5 py-3 flex items-center gap-2.5 border transition-all duration-300 font-display font-black text-xs uppercase tracking-wider ${activeCount > 0 ? 'border-primary-500/70 bg-primary-500/15 text-primary-400 shadow-lg shadow-primary-500/20 ring-1 ring-primary-500/30' : 'border-white/10 text-slate-700 dark:text-slate-300 hover:border-white/20 hover:bg-white/5'}">
             <i data-lucide="sliders-horizontal" class="w-4 h-4 ${activeCount > 0 ? 'text-primary-500 animate-pulse' : 'text-slate-400'}"></i>
             <span>Filter & Sort</span>
             ${activeCount > 0 ? `<span class="w-5 h-5 rounded-full bg-primary-500 text-white text-[10px] font-black flex items-center justify-center shadow-md">${activeCount}</span>` : ''}
        </button>
    ` : '';

    let activeChipsHtml = '';
    if (showSort && activeCount > 0) {
        activeChipsHtml = `
            <div class="flex flex-wrap items-center gap-2 pt-2 pb-1 w-full fade-in">
                <span class="text-[10px] font-black uppercase tracking-wider text-slate-500 mr-1">Active:</span>
                ${state.filters.genre ? `<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold bg-primary-500/20 border border-primary-500/40 text-primary-300">Genre: ${state.filters.genre} <button type="button" data-clear-filter="genre" class="hover:text-white font-bold ml-1">✕</button></span>` : ''}
                ${state.filters.era && state.filters.era !== 'all' ? `<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold bg-primary-500/20 border border-primary-500/40 text-primary-300">Era: ${getEraLabel(state.filters.era)} <button type="button" data-clear-filter="era" class="hover:text-white font-bold ml-1">✕</button></span>` : ''}
                ${state.filters.star ? `<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold bg-primary-500/20 border border-primary-500/40 text-primary-300">Star: ${state.filters.star} <button type="button" data-clear-filter="star" class="hover:text-white font-bold ml-1">✕</button></span>` : ''}
                ${state.filters.minRating > 0 ? `<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold bg-primary-500/20 border border-primary-500/40 text-primary-300">⭐ ${state.filters.minRating}+ <button type="button" data-clear-filter="minRating" class="hover:text-white font-bold ml-1">✕</button></span>` : ''}
                ${state.filters.sortBy && state.filters.sortBy !== 'rating_desc' ? `<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold bg-white/10 border border-white/20 text-slate-300">Sort: ${getSortLabel(state.filters.sortBy)} <button type="button" data-clear-filter="sortBy" class="hover:text-white font-bold ml-1">✕</button></span>` : ''}
                <button id="clearAllFiltersBtn" type="button" class="text-[11px] uppercase font-black tracking-wider text-slate-400 hover:text-primary-400 transition-colors ml-1 underline underline-offset-4">Clear All</button>
            </div>
        `;
    }

    return `
        <div class="space-y-4 mb-4">
            <div class="flex flex-col md:flex-row justify-between items-end gap-6">
                <div>
                    <h2 class="text-4xl sm:text-6xl font-display font-black tracking-tighter mb-3 leading-none bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600 dark:from-white dark:to-slate-400">${title}</h2>
                    <div class="flex items-center gap-3">
                        <div class="w-12 h-1 bg-primary-500 rounded-full"></div>
                        <p class="text-slate-500 font-bold text-xs uppercase tracking-[0.4em] font-display">Archives</p>
                    </div>
                </div>
                <div class="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center w-full md:w-auto mt-6 md:mt-0">
                    ${filterSortHtml}
                    <div class="glass rounded-[2rem] p-1 flex items-center border border-white/10 focus-within:ring-4 focus-within:ring-primary-500/20 transition-all w-full sm:w-80">
                        <input type="text" id="searchInput" value="${state.searchTerm}" placeholder="Search title, actor, director..." class="flex-1 bg-transparent border-none px-6 py-3 text-sm font-bold focus:outline-none placeholder:text-slate-500 text-slate-700 dark:text-slate-200">
                        <div class="p-3 text-slate-400 bg-slate-900/5 dark:bg-slate-900/50 rounded-full mr-1"><i data-lucide="search" class="w-5 h-5"></i></div>
                    </div>
                </div>
            </div>
            ${activeChipsHtml}
        </div>
    `;
};

const renderGrid = (movies, title = "Discover", showSort = false) => {
    const totalPages = Math.ceil(movies.length / state.pageSize) || 1;
    if (state.page > totalPages) state.page = totalPages;
    if (state.page < 1) state.page = 1;

    const startIndex = (state.page - 1) * state.pageSize;
    const moviesToShow = movies.slice(startIndex, startIndex + state.pageSize);

    const paginationHtml = totalPages > 1 ? `
        <div class="flex flex-col sm:flex-row justify-center items-center gap-6 pt-12 pb-16 fade-in">
            <div class="flex items-center gap-3">
                <button id="prevPageBtn" class="w-12 h-12 glass rounded-full flex items-center justify-center hover:premium-gradient hover:text-white transition-all text-slate-700 dark:text-slate-300 border border-white/5 ${state.page === 1 ? 'opacity-30 cursor-not-allowed pointer-events-none' : 'shadow-lg hover:shadow-primary-500/30'}">
                    <i data-lucide="chevron-left" class="w-5 h-5"></i>
                </button>
                <div class="px-6 py-3 glass rounded-[2rem] text-xs font-black tracking-[0.2em] uppercase text-slate-700 dark:text-slate-300 border border-white/5 shadow-inner">
                    Page <span class="text-primary-600 dark:text-primary-400 mx-1">${state.page}</span> / ${totalPages}
                </div>
                <button id="nextPageBtn" class="w-12 h-12 glass rounded-full flex items-center justify-center hover:premium-gradient hover:text-white transition-all text-slate-700 dark:text-slate-300 border border-white/5 ${state.page === totalPages ? 'opacity-30 cursor-not-allowed pointer-events-none' : 'shadow-lg hover:shadow-primary-500/30'}">
                    <i data-lucide="chevron-right" class="w-5 h-5"></i>
                </button>
            </div>
            <div class="flex items-center gap-3">
                <div class="glass pl-4 pr-1 py-1 rounded-[2rem] flex items-center border border-white/5 focus-within:border-primary-500/50 transition-all shadow-inner">
                    <input type="number" id="gotoPageInput" min="1" max="${totalPages}" placeholder="Go to..." class="w-20 bg-transparent border-none text-xs font-black text-center focus:outline-none text-slate-700 dark:text-slate-300 placeholder:text-slate-400/50 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none">
                    <button id="gotoPageBtn" class="w-10 h-10 rounded-full premium-gradient flex items-center justify-center text-white shadow-lg hover:scale-105 transition-transform"><i data-lucide="arrow-right" class="w-4 h-4"></i></button>
                </div>
            </div>
        </div>
    ` : '';

    return `
        <div class="space-y-12">
            ${renderSearchAndFilter(title, showSort)}
            <div class="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3 md:gap-6 min-h-[40vh]">
                ${moviesToShow.map(movieCard).join('')}
            </div>
            ${moviesToShow.length === 0 ? `<div class="flex flex-col items-center justify-center py-32 text-slate-500 space-y-6"><div class="w-20 h-20 rounded-full glass flex items-center justify-center opacity-20"><i data-lucide="film" class="w-10 h-10"></i></div><p class="font-bold tracking-[0.5em] uppercase text-xs animate-pulse">Record Unavailable</p></div>` : ''}
            ${paginationHtml}
        </div>
    `;
};

// --- Page Renderers & Router ---

const navSearchListener = (callback) => {
    const input = document.getElementById('searchInput');
    if (input) input.addEventListener('input', debounce((e) => {
        state.searchTerm = e.target.value;
        state.page = 1;
        state.peoplePage = 1; // Reset people pagination too
        callback();
    }, 300));
};

const mountGrid = (movies, title, searchCallback = null, showSort = false) => {
    app.innerHTML = renderGrid(movies, title, showSort);

    // Default Global Search (redirects to Home) if no specific callback matches
    if (searchCallback) {
        navSearchListener(searchCallback);
    } else {
        navSearchListener(() => renderHome());
    }

    const prevBtn = document.getElementById('prevPageBtn');
    if (prevBtn) prevBtn.addEventListener('click', () => {
        state.page--;
        route(); // Scroll to top
    });

    const nextBtn = document.getElementById('nextPageBtn');
    if (nextBtn) nextBtn.addEventListener('click', () => {
        state.page++;
        route(); // Scroll to top
    });

    const gotoBtn = document.getElementById('gotoPageBtn');
    const gotoInput = document.getElementById('gotoPageInput');
    
    const handleGoto = () => {
        if (!gotoInput) return;
        const target = parseInt(gotoInput.value, 10);
        if (!isNaN(target)) {
            state.page = target;
            route(); // route will auto clamp to valid page range in renderGrid
        }
    };

    if (gotoBtn) gotoBtn.addEventListener('click', handleGoto);
    if (gotoInput) gotoInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleGoto();
    });

    // Open Beautiful Filter Modal
    const openFilterBtn = document.getElementById('openFilterBtn');
    if (openFilterBtn) {
        openFilterBtn.addEventListener('click', () => {
            openFilterModal(searchCallback);
        });
    }

    // Individual chip clear buttons
    document.querySelectorAll('[data-clear-filter]').forEach(btn => {
        btn.addEventListener('click', () => {
            const key = btn.getAttribute('data-clear-filter');
            if (key === 'sortBy') state.filters.sortBy = 'rating_desc';
            else if (key === 'minRating') state.filters.minRating = 0;
            else state.filters[key] = null;
            state.page = 1;
            if (searchCallback) searchCallback();
            else route(true);
        });
    });

    // Clear all filters button
    const clearAllBtn = document.getElementById('clearAllFiltersBtn');
    if (clearAllBtn) {
        clearAllBtn.addEventListener('click', () => {
            state.filters = { sortBy: 'rating_desc', genre: null, era: null, star: null, minRating: 0 };
            state.page = 1;
            if (searchCallback) searchCallback();
            else route(true);
        });
    }

    if (window.lucide) lucide.createIcons();
};

const getPersonWiki = (name) => {
    if (!name) return { image: null, wikiUrl: null };
    const clean = name.trim().toLowerCase();
    if (state.wikiArtists && state.wikiArtists[clean]) {
        const item = state.wikiArtists[clean];
        return {
            image: item.image || null,
            wikiUrl: item.wiki_url || `https://en.wikipedia.org/wiki/${encodeURIComponent(name)}`
        };
    }
    return {
        image: null,
        wikiUrl: `https://en.wikipedia.org/wiki/Special:Search?search=${encodeURIComponent(name)}`
    };
};

const mountPeopleGrid = (items, type) => {
    // Pagination slicing
    const totalPages = Math.ceil(items.length / state.peoplePageSize) || 1;
    if (state.peoplePage > totalPages) state.peoplePage = totalPages;
    if (state.peoplePage < 1) state.peoplePage = 1;

    const startIndex = (state.peoplePage - 1) * state.peoplePageSize;
    const itemsToShow = items.slice(startIndex, startIndex + state.peoplePageSize);

    // Tab UI
    const tabsHtml = `
        <div class="flex gap-4 mb-12">
            <button id="tab-cast" class="px-8 py-3 rounded-2xl font-bold text-sm tracking-wide transition-all ${state.activePeopleTab === 'cast' ? 'bg-primary-600 text-white shadow-lg shadow-primary-600/30' : 'glass text-slate-500 hover:text-white'}">Casts (Actors & Actresses)</button>
            <button id="tab-director" class="px-8 py-3 rounded-2xl font-bold text-sm tracking-wide transition-all ${state.activePeopleTab === 'director' ? 'bg-primary-600 text-white shadow-lg shadow-primary-600/30' : 'glass text-slate-500 hover:text-white'}">Directors</button>
        </div>
    `;

    const icon = type === 'director' ? 'clapperboard' : 'user';
    const label = type === 'director' ? 'Films Directed' : 'Movies in Vault';
    const linkParam = type === 'director' ? 'director' : 'cast';

    const gridHtml = `
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5 gap-3 md:gap-6">
            ${itemsToShow.map(item => {
                const info = getPersonWiki(item.name);
                const photo = info.image;
                const wikiUrl = info.wikiUrl;
                const initials = item.name.split(' ').map(w => w[0]).filter(Boolean).slice(0, 2).join('').toUpperCase() || '★';
                return `
                <div class="group p-4 md:p-6 glass rounded-[2rem] md:rounded-[2.5rem] text-center space-y-3 hover:border-primary-500 transition-all border border-white/5 flex flex-col items-center">
                    <a href="index.html?${linkParam}=${encodeURIComponent(item.name)}" class="w-full">
                        <div class="aspect-square w-full rounded-[1.5rem] md:rounded-[2rem] bg-slate-900 flex items-center justify-center relative overflow-hidden shadow-lg border border-white/5">
                            ${photo ? `
                                <img src="${photo}" alt="${item.name}" loading="lazy" class="w-full h-full object-cover group-hover:scale-110 transition-all duration-500" onerror="this.style.display='none'; this.nextElementSibling.classList.remove('hidden');">
                                <div class="hidden w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-slate-800 to-slate-950 font-display font-black text-2xl text-primary-400">
                                    <span>${initials}</span>
                                </div>
                            ` : `
                                <div class="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-slate-800 to-slate-950 space-y-1">
                                    <span class="font-display font-black text-2xl md:text-3xl text-primary-400">${initials}</span>
                                    <i data-lucide="${icon}" class="w-4 h-4 text-slate-600"></i>
                                </div>
                            `}
                            <div class="absolute inset-0 premium-gradient opacity-0 group-hover:opacity-15 transition-opacity"></div>
                        </div>
                    </a>
                    <div class="w-full flex-1 flex flex-col justify-center gap-1">
                        <a href="index.html?${linkParam}=${encodeURIComponent(item.name)}" class="text-sm md:text-base leading-tight font-display font-black text-slate-700 dark:text-slate-300 group-hover:text-primary-600 dark:group-hover:text-white transition-all break-words">${item.name}</a>
                        <p class="text-[9px] md:text-[10px] font-black text-slate-500 uppercase tracking-widest leading-tight">${item.count} ${label}</p>
                    </div>
                    <div class="flex items-center gap-2 w-full pt-1">
                        <a href="index.html?${linkParam}=${encodeURIComponent(item.name)}" class="flex-1 py-2 px-2 rounded-xl glass text-[10px] font-bold uppercase tracking-wider text-slate-300 hover:text-white hover:bg-white/10 transition-all text-center">
                            Films
                        </a>
                        <a href="${wikiUrl}" target="_blank" rel="noopener noreferrer" class="py-2 px-3 rounded-xl bg-sky-500/10 border border-sky-500/30 text-sky-400 hover:bg-sky-500/20 text-[10px] font-bold uppercase tracking-wider transition-all flex items-center justify-center gap-1">
                            🌐 Wiki
                        </a>
                    </div>
                </div>
                `;
            }).join('')}
        </div>
    `;

    const paginationHtml = totalPages > 1 ? `
        <div class="flex flex-col sm:flex-row justify-center items-center gap-6 pt-12 pb-16 fade-in">
            <div class="flex items-center gap-3">
                <button id="prevPeopleBtn" class="w-12 h-12 glass rounded-full flex items-center justify-center hover:premium-gradient hover:text-white transition-all text-slate-700 dark:text-slate-300 border border-white/5 ${state.peoplePage === 1 ? 'opacity-30 cursor-not-allowed pointer-events-none' : 'shadow-lg hover:shadow-primary-500/30'}"><i data-lucide="chevron-left" class="w-5 h-5"></i></button>
                <div class="px-6 py-3 glass rounded-[2rem] text-xs font-black tracking-[0.2em] uppercase text-slate-700 dark:text-slate-300 border border-white/5 shadow-inner">Page <span class="text-primary-600 dark:text-primary-400 mx-1">${state.peoplePage}</span> / ${totalPages}</div>
                <button id="nextPeopleBtn" class="w-12 h-12 glass rounded-full flex items-center justify-center hover:premium-gradient hover:text-white transition-all text-slate-700 dark:text-slate-300 border border-white/5 ${state.peoplePage === totalPages ? 'opacity-30 cursor-not-allowed pointer-events-none' : 'shadow-lg hover:shadow-primary-500/30'}"><i data-lucide="chevron-right" class="w-5 h-5"></i></button>
            </div>
            <div class="flex items-center gap-3">
                <div class="glass pl-4 pr-1 py-1 rounded-[2rem] flex items-center border border-white/5 focus-within:border-primary-500/50 transition-all shadow-inner">
                    <input type="number" id="gotoPeopleInput" min="1" max="${totalPages}" placeholder="Go to..." class="w-20 bg-transparent border-none text-xs font-black text-center focus:outline-none text-slate-700 dark:text-slate-300 placeholder:text-slate-400/50 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none">
                    <button id="gotoPeopleBtn" class="w-10 h-10 rounded-full premium-gradient flex items-center justify-center text-white shadow-lg hover:scale-105 transition-transform"><i data-lucide="arrow-right" class="w-4 h-4"></i></button>
                </div>
            </div>
        </div>
    ` : '';

    app.innerHTML = `<div class="fade-in space-y-8 pb-32">
        <div>${renderSearchAndFilter("People")}</div>
        ${tabsHtml}
        ${gridHtml}
        ${paginationHtml}
    </div>`;

    // Listeners
    navSearchListener(() => renderPeople()); // Generalized callback

    document.getElementById('tab-cast').addEventListener('click', () => {
        state.activePeopleTab = 'cast';
        state.peoplePage = 1;
        state.searchTerm = ''; // Optional pattern: reset search on tab switch? Or keep it? keeping it is better UX usually, but let's see. Maybe clear it to avoid confusion.
        renderPeople();
    });

    document.getElementById('tab-director').addEventListener('click', () => {
        state.activePeopleTab = 'director';
        state.peoplePage = 1;
        state.searchTerm = '';
        renderPeople();
    });

    const prevPeople = document.getElementById('prevPeopleBtn');
    if (prevPeople) prevPeople.addEventListener('click', () => { state.peoplePage--; renderPeople(); window.scrollTo(0,0); });

    const nextPeople = document.getElementById('nextPeopleBtn');
    if (nextPeople) nextPeople.addEventListener('click', () => { state.peoplePage++; renderPeople(); window.scrollTo(0,0); });

    const gotoPeopleBtn = document.getElementById('gotoPeopleBtn');
    const gotoPeopleInput = document.getElementById('gotoPeopleInput');
    
    const handleGotoPeople = () => {
        if (!gotoPeopleInput) return;
        const target = parseInt(gotoPeopleInput.value, 10);
        if (!isNaN(target)) { state.peoplePage = target; renderPeople(); window.scrollTo(0,0); }
    };

    if (gotoPeopleBtn) gotoPeopleBtn.addEventListener('click', handleGotoPeople);
    if (gotoPeopleInput) gotoPeopleInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') handleGotoPeople(); });

    if (window.lucide) lucide.createIcons();
};

const renderPeople = () => {
    const hero = document.getElementById('hero');
    if (hero) hero.style.display = 'none';

    let items = [];
    if (state.activePeopleTab === 'cast') {
        const actorsMap = {};
        state.allMovies.forEach(m => { if (Array.isArray(m.cast)) m.cast.forEach(c => { actorsMap[c] = (actorsMap[c] || 0) + 1; }); });
        items = Object.entries(actorsMap).map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count);
    } else {
        const directorsMap = {};
        state.allMovies.forEach(m => {
            if (m.director && m.director !== "Unknown Director") {
                const dirs = m.director.split(',').map(d => d.trim());
                dirs.forEach(d => { directorsMap[d] = (directorsMap[d] || 0) + 1; });
            }
        });
        items = Object.entries(directorsMap).map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count);
    }

    // Filter
    if (state.searchTerm) {
        items = items.filter(i => i.name.toLowerCase().includes(state.searchTerm.toLowerCase()));
    }

    mountPeopleGrid(items, state.activePeopleTab);
};


const renderHome = () => {
    const hero = document.getElementById('hero');
    if (hero) hero.style.display = 'block';

    const results = getFilteredAndSortedMovies(state.allMovies);
    const hasFilter = getActiveFiltersCount() > 0 || (state.searchTerm && state.searchTerm.trim().length > 0);
    const titleText = hasFilter ? `Vault (${results.length} Matches)` : `Malayalam Movie Vault (${state.allMovies.length})`;

    mountGrid(results, titleText, renderHome, true);
};

const renderFiltered = (movies, title) => {
    const hero = document.getElementById('hero');
    if (hero) hero.style.display = 'none';

    const results = getFilteredAndSortedMovies(movies);
    const hasFilter = getActiveFiltersCount() > 0 || (state.searchTerm && state.searchTerm.trim().length > 0);
    const titleText = hasFilter ? `${title} (${results.length})` : title;

    mountGrid(results, titleText, null, true);
};

const renderDetails = () => {
    const hero = document.getElementById('hero');
    if (hero) hero.style.display = 'none';
    const id = state.params.get('id');
    const movie = state.allMovies.find(m => String(m.id) === String(id));

    if (!movie) {
        app.innerHTML = `<div class="text-center py-40 font-display text-2xl font-black text-slate-500 tracking-tighter uppercase opacity-20">Security Clearance Required / Invalid Record</div>`;
        return;
    }
    const isFav = state.favorites.some(f => f.id === movie.id);
    updateMetaTags(movie.title, movie.plot, movie.genre.join(', '));
    const posterHtml = movie.poster ? `<img src="${movie.poster}" class="absolute inset-0 w-full h-full object-cover"><div class="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent"></div>` : `<div class="absolute inset-0 premium-gradient opacity-10 blur-3xl group-hover:opacity-30 transition-opacity"></div><div class="relative z-10 flex flex-col items-center"><i data-lucide="clapperboard" class="w-24 h-24 text-primary-500/20 mb-8 animate-pulse"></i><h2 class="text-white font-display font-black text-4xl leading-tight tracking-tighter">${movie.title}</h2></div>`;

    // New Queries with Old Movie Detection
    const isOld = movie.year && movie.year < 2011;
    const oldTag = isOld ? "Old " : "";

    const fullMovieQuery = `${movie.title} ${oldTag}Malayalam full movie`;
    const trailerQuery = `${movie.title} ${oldTag}Malayalam movie trailer`;
    const songsQuery = `${movie.title} ${oldTag}Malayalam movie songs`;

    app.innerHTML = `
        <div class="fade-in max-w-6xl mx-auto pb-32">
            <div class="flex items-center gap-6 mb-12">
                <button onclick="history.back()" class="w-14 h-14 glass rounded-[1.5rem] flex items-center justify-center hover:premium-gradient hover:text-white transition-all text-slate-500 shadow-xl"><i data-lucide="chevron-left" class="w-6 h-6"></i></button>
                <div class="h-px flex-1 bg-gradient-to-r from-slate-200 dark:from-slate-800 to-transparent"></div>
            </div>
            <div class="grid grid-cols-1 lg:grid-cols-12 gap-16">
                <!-- Poster & Actions Column -->
                <div class="lg:col-span-5 space-y-8">
                    <div class="aspect-[2/3] w-full rounded-[3rem] movie-card-placeholder flex flex-col items-center justify-center p-16 text-center shadow-[0_35px_60px_-15px_rgba(0,0,0,0.5)] relative overflow-hidden group border border-white/5 bg-slate-900">
                        ${posterHtml}
                    </div>
                    
                    <div class="space-y-4">
                        <!-- Watch Full Movie -->
                        <button onclick="window.open('https://www.youtube.com/results?search_query=${encodeURIComponent(fullMovieQuery)}', '_blank')" class="w-full flex items-center justify-center gap-3 premium-gradient text-white py-4 rounded-[1.5rem] font-black uppercase tracking-[0.15em] text-xs shadow-xl shadow-primary-500/20 hover:scale-[1.02] transition-all group">
                            <i data-lucide="play-circle" class="w-5 h-5 fill-current"></i> Watch Full Movie
                        </button>
                        
                        <div class="grid grid-cols-2 gap-4">
                            <!-- Watch Trailer -->
                            <button onclick="window.open('https://www.youtube.com/results?search_query=${encodeURIComponent(trailerQuery)}', '_blank')" class="w-full flex items-center justify-center gap-2 glass text-slate-700 dark:text-slate-300 py-4 rounded-[1.5rem] font-bold uppercase tracking-[0.1em] text-[10px] hover:bg-white/10 hover:text-primary-600 dark:hover:text-white transition-all border border-white/5">
                                <i data-lucide="film" class="w-4 h-4"></i> Trailer
                            </button>
                            
                            <!-- Songs -->
                            <button onclick="window.open('https://www.youtube.com/results?search_query=${encodeURIComponent(songsQuery)}', '_blank')" class="w-full flex items-center justify-center gap-2 glass text-slate-700 dark:text-slate-300 py-4 rounded-[1.5rem] font-bold uppercase tracking-[0.1em] text-[10px] hover:bg-white/10 hover:text-primary-600 dark:hover:text-white transition-all border border-white/5">
                                <i data-lucide="music" class="w-4 h-4"></i> Songs
                            </button>
                        </div>

                        <!-- Save -->
                        <button id="favBtn" class="w-full flex items-center justify-center gap-3 glass py-4 rounded-[1.5rem] font-black uppercase tracking-[0.15em] text-xs hover:bg-white/10 transition-all ${isFav ? 'text-primary-500 border-primary-500/50' : 'text-slate-700 dark:text-slate-500 border-white/10'}">
                            <i data-lucide="heart" class="w-5 h-5 ${isFav ? 'fill-current' : ''}"></i> ${isFav ? 'Saved' : 'Save'}
                        </button>
                    </div>
                </div>

                <!-- Details Column -->
                <div class="lg:col-span-7 space-y-14">
                   <h1 class="text-6xl sm:text-8xl font-display font-black tracking-tighter bg-clip-text text-transparent bg-gradient-to-br from-slate-900 via-slate-600 to-slate-400 dark:from-white dark:via-slate-200 dark:to-slate-600 leading-[0.9]">${movie.title}</h1>
                   <div class="flex flex-wrap gap-3">${movie.genre.map(g => `<a href="index.html?genre=${encodeURIComponent(g)}" class="px-6 py-2 glass rounded-full text-[10px] font-black uppercase tracking-[0.3em] text-slate-600 dark:text-slate-500 hover:text-primary-500 hover:border-primary-500 transition-all">${g}</a>`).join('')}</div>
                   <div class="glass rounded-[3rem] p-10 sm:p-16 space-y-12 border border-white/10 shadow-2xl">
                        <p class="text-xl sm:text-2xl text-slate-600 dark:text-slate-300 font-medium leading-[1.6] italic">"${movie.plot}"</p>
                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-12 pt-12 border-t border-white/5">
                            <div class="space-y-4">
                                <h4 class="text-[9px] font-black uppercase tracking-[0.4em] text-slate-500">Direction</h4>
                                <p class="text-2xl font-display font-black text-slate-900 dark:text-white leading-none">${movie.director}</p>
                            </div>
                            <div class="space-y-4">
                                <h4 class="text-[9px] font-black uppercase tracking-[0.4em] text-slate-500">Main Roster</h4>
                                <div class="flex flex-wrap gap-x-2 gap-y-1 text-2xl font-display font-black text-slate-900 dark:text-white leading-none">
                                    ${movie.cast.map(c => `<a href="index.html?cast=${encodeURIComponent(c)}" class="hover:text-primary-500 transition-colors cursor-pointer">${c}</a>`).join('<span class="text-slate-700">/</span>')}
                                </div>
                            </div>
                        </div>
                   </div>
                   <div class="space-y-8">
                        <h3 class="text-3xl font-display font-black tracking-tight text-slate-900 dark:text-white">Critical Industry Dossier</h3>
                        <p class="text-slate-600 dark:text-slate-400 text-sm sm:text-base leading-[2] font-medium italic opacity-70">${generateMovieDeepContent(movie)}</p>
                   </div>
                </div>
            </div>
        </div>
    `;
    const favB = document.getElementById('favBtn');
    if (favB) favB.addEventListener('click', () => { toggleFavorite(movie); renderDetails(); });
    if (window.lucide) lucide.createIcons();
};

const renderFavorites = () => {
    const hero = document.getElementById('hero');
    if (hero) hero.style.display = 'none';

    const results = getFilteredAndSortedMovies(state.favorites);
    const hasFilter = getActiveFiltersCount() > 0 || (state.searchTerm && state.searchTerm.trim().length > 0);
    const titleText = hasFilter ? `Saved Movies (${results.length})` : "Saved Premium Selection";

    mountGrid(results, titleText, renderFavorites, true);
};

const renderYears = () => {
    const hero = document.getElementById('hero');
    if (hero) hero.style.display = 'none';
    const yearsSet = new Set(state.allMovies.map(m => m.year).filter(y => y));
    const years = Array.from(yearsSet).sort((a, b) => b - a);

    // Filter
    let displayYears = years;
    if (state.searchTerm) {
        displayYears = years.filter(y => String(y).includes(state.searchTerm));
    }

    app.innerHTML = `<div class="fade-in space-y-16 pb-32"><div>${renderSearchAndFilter("Timeline")}</div><div class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-3 md:gap-5">${displayYears.map(year => `<a href="index.html?year=${year}" class="h-24 md:h-32 glass rounded-[1.5rem] md:rounded-[2rem] flex flex-col items-center justify-center group hover:premium-gradient transition-all border border-white/10 hover:scale-105 shadow-xl"><span class="text-xl md:text-2xl font-display font-black text-slate-400 dark:text-slate-400 text-slate-600 group-hover:text-white transition-all">${year}</span></a>`).join('')}</div></div>`;
    navSearchListener(renderYears);
    if (window.lucide) lucide.createIcons();
};

const renderGenres = () => {
    const hero = document.getElementById('hero');
    if (hero) hero.style.display = 'none';
    const genresMap = {};
    state.allMovies.forEach(m => { if (Array.isArray(m.genre)) m.genre.forEach(g => { const name = g.trim(); if (name) genresMap[name] = (genresMap[name] || 0) + 1; }); });
    const genres = Object.entries(genresMap).map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count);

    let displayGenres = genres;
    if (state.searchTerm) {
        displayGenres = genres.filter(g => g.name.toLowerCase().includes(state.searchTerm.toLowerCase()));
    }

    app.innerHTML = `<div class="fade-in space-y-16 pb-32"><div>${renderSearchAndFilter("DNA")}</div><div class="grid grid-cols-2 gap-3 md:gap-8">${displayGenres.map(genre => `<a href="index.html?genre=${encodeURIComponent(genre.name)}" class="p-6 md:p-10 glass rounded-[2rem] md:rounded-[3rem] group hover:border-primary-500 transition-all border border-white/5 flex flex-col items-center text-center space-y-3 md:space-y-4"><div class="w-12 h-12 md:w-16 md:h-16 rounded-[1rem] md:rounded-[1.5rem] premium-gradient flex items-center justify-center text-white shadow-xl opacity-20 group-hover:opacity-100 transition-all"><i data-lucide="clapperboard" class="w-6 h-6 md:w-8 md:h-8"></i></div><div class="w-full flex-1 flex flex-col justify-center"><p class="text-base md:text-xl leading-tight font-display font-black text-slate-700 dark:text-slate-300 group-hover:text-primary-600 dark:group-hover:text-white transition-all uppercase tracking-tighter break-words">${genre.name}</p><p class="text-[9px] md:text-[10px] font-black text-slate-500 uppercase tracking-widest mt-1">${genre.count} Records</p></div></a>`).join('')}</div></div>`;
    navSearchListener(renderGenres);
    if (window.lucide) lucide.createIcons();
};

// Merged Render People with Tabs removed. Correct implementation of renderPeople is already present above.

const route = (keepScroll = false) => {
    const currentPath = window.location.pathname;
    const params = new URLSearchParams(window.location.search);
    state.params = params;

    // Explicitly reset search term on route change
    state.searchTerm = '';

    const genreParam = params.get('genre');
    const yearParam = params.get('year');
    const castParam = params.get('cast');
    const directorParam = params.get('director');
    const idParam = params.get('id');

    console.log("Routing:", { currentPath, genreParam, yearParam, castParam, directorParam }); // Debug

    // Scroll to top on route change
    if (!keepScroll) window.scrollTo(0, 0);

    // Update Active Nav State
    document.querySelectorAll('.nav-item').forEach(nav => {
        const navPath = nav.getAttribute('href');
        // Simple logic: if path matches exactly or loosely
        if (currentPath.includes(navPath) || (currentPath === '/' && navPath === 'index.html')) {
            nav.classList.add('active-nav', 'text-primary-500', 'nav-glow', 'active');
            nav.classList.remove('text-slate-500');
        } else {
            nav.classList.remove('active-nav', 'text-primary-500', 'nav-glow', 'active');
            nav.classList.add('text-slate-500');
        }
    });

    if (currentPath.includes('details') || idParam) {
        renderDetails();
    } else if (currentPath.includes('favorites')) {
        renderFavorites();
    } else if (currentPath.includes('years')) {
        renderYears();
    } else if (currentPath.includes('genres')) {
        renderGenres();
    } else if (currentPath.includes('cast')) {
        state.activePeopleTab = 'cast'; // Default to cast tab
        renderPeople(); // Use merged renderer
    } else if (currentPath.includes('directors')) {
        state.activePeopleTab = 'director'; // Switch to director tab
        renderPeople(); // Use merged renderer
    } else if (genreParam) {
        const p = genreParam.trim();
        const filtered = state.allMovies.filter(m => m.genre && m.genre.some(g => g.toLowerCase() === p.toLowerCase()));
        renderFiltered(filtered, `Genre: ${p}`);
    } else if (yearParam) {
        const p = yearParam.trim();
        const filtered = state.allMovies.filter(m => String(m.year) === p);
        renderFiltered(filtered, `Vault ${p}`);
    } else if (castParam) {
        const p = castParam.trim();
        const filtered = state.allMovies.filter(m => m.cast && m.cast.some(c => c.toLowerCase() === p.toLowerCase()));
        renderFiltered(filtered, `Star: ${p}`);
    } else if (directorParam) { // New Filter
        const p = directorParam.trim();
        const filtered = state.allMovies.filter(m => m.director && m.director.includes(p));
        renderFiltered(filtered, `Director: ${p}`);
    } else {
        renderHome();
    }
};

const handleNavigation = (e) => {
    const link = e.target.closest('a');
    if (!link) return;

    const href = link.getAttribute('href');
    // Skip external, anchors, mailto, etc.
    if (!href || href.startsWith('http') || href.startsWith('//') || href.startsWith('#') || href.startsWith('mailto:') || href.startsWith('tel:')) return;

    // Skip if target blank
    if (link.target === '_blank') return;

    e.preventDefault();
    window.history.pushState({}, '', href);
    route();
};

const applyTheme = () => {
    const root = document.documentElement;
    if (state.theme === 'dark') root.classList.add('dark');
    else root.classList.remove('dark');
};

const setupThemeToggle = () => {
    const btn = document.getElementById('themeToggle');
    if (btn) btn.addEventListener('click', () => {
        state.theme = state.theme === 'dark' ? 'light' : 'dark';
        localStorage.setItem('imdbTheme_premium', state.theme);
        applyTheme();
    });
};

const toggleFavorite = (movie) => {
    const idx = state.favorites.findIndex(f => f.id === movie.id);
    if (idx === -1) {
        state.favorites.push({ ...movie, savedAt: Date.now() });
    } else {
        state.favorites.splice(idx, 1);
    }
    localStorage.setItem('imdbFavorites_premium_v1', JSON.stringify(state.favorites));
};

const init = async () => {
    applyTheme();
    setupThemeToggle();

    // SPA Navigation Interceptor
    document.addEventListener('click', handleNavigation);
    window.addEventListener('popstate', route);

    const loadWikiData = () => {
        return new Promise((resolve) => {
            const populate = (list) => {
                if (Array.isArray(list)) {
                    list.forEach(item => {
                        if (item.clean) state.wikiArtists[item.clean] = item;
                        if (item.name) state.wikiArtists[item.name.toLowerCase()] = item;
                    });
                }
            };
            fetch('malayalam_artists_wiki.json')
                .then(res => {
                    if (!res.ok) throw new Error("HTTP " + res.status);
                    return res.json();
                })
                .then(list => {
                    populate(list);
                    resolve();
                })
                .catch(() => {
                    const xhr = new XMLHttpRequest();
                    xhr.overrideMimeType("application/json");
                    xhr.open("GET", "malayalam_artists_wiki.json", true);
                    xhr.onreadystatechange = function() {
                        if (xhr.readyState === 4) {
                            if (xhr.status === 200 || xhr.status === 0) {
                                try {
                                    populate(JSON.parse(xhr.responseText));
                                } catch(e) {}
                            }
                            resolve();
                        }
                    };
                    xhr.onerror = () => resolve();
                    xhr.send(null);
                });
        });
    };

    const loadData = () => {
        return new Promise((resolve, reject) => {
            fetch('movie_data.json')
                .then(res => {
                    if (!res.ok) throw new Error("HTTP " + res.status);
                    return res.json();
                })
                .then(resolve)
                .catch(() => {
                    const xhr = new XMLHttpRequest();
                    xhr.overrideMimeType("application/json");
                    xhr.open("GET", "movie_data.json", true);
                    xhr.onreadystatechange = function() {
                        if (xhr.readyState === 4) {
                            if (xhr.status === 200 || xhr.status === 0) {
                                try {
                                    resolve(JSON.parse(xhr.responseText));
                                } catch(e) { reject(e); }
                            } else {
                                reject(new Error("XHR Status: " + xhr.status));
                            }
                        }
                    };
                    xhr.onerror = reject;
                    xhr.send(null);
                });
        });
    };

    try {
        await loadWikiData();
        const data = await loadData();
        state.allMovies = data.map(normalizeMovie);
        route();
    } catch (err) {
        console.error("Vault Initialization Failed:", err);
        app.innerHTML = `<div class="text-center py-40 opacity-30 uppercase font-black tracking-widest text-xs">Critical System Error: Remote Data Unreachable</div>`;
    }
};

document.addEventListener('DOMContentLoaded', init);
