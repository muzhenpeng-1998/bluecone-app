/**
 * 文件路径：app-application/src/main/resources/public/admin/app.js
 * 作用：BlueCone Admin Ops UI 的核心逻辑，包含 Token 管理、API Client 以及各个面板（Outbox / Scheduler / Config / Notification）的 Alpine.js 数据模型。
 * 后端接口假定：
 *   - Outbox：GET /api/admin/outbox?page=1&pageSize=20 · POST /api/admin/outbox/{id}/retry
 *   - Scheduler：GET /api/admin/scheduler/jobs · POST /api/admin/scheduler/jobs/{id}/trigger
 *   - ConfigCenter：GET /api/admin/config/properties?tenantId={tid}&env={env}
 *   - Notification：POST /api/admin/notify/test
 * 如与实际 Controller 路径不同，请根据实际路径在此文件中调整。
 */
(function () {
    const TOKEN_KEY = 'bluecone_admin_token';

    /**
     * 极简 API Client：负责拼装 Authorization 头、序列化请求体并将错误透出到 UI。
     */
    const apiClient = {
        async request(method, url, body) {
            const headers = { 'Content-Type': 'application/json' };
            const token = localStorage.getItem(TOKEN_KEY);
            if (token) {
                headers.Authorization = `Bearer ${token}`;
            }

            const response = await fetch(url, {
                method,
                headers,
                body: body ? JSON.stringify(body) : undefined,
            });

            if (!response.ok) {
                const text = await response.text();
                const message = text || `请求 ${url} 失败(${response.status})`;
                console.error('[AdminOps] API Error:', message);
                throw new Error(message);
            }

            const contentType = response.headers.get('content-type') || '';
            if (contentType.includes('application/json')) {
                return response.json();
            }
            return response.text();
        },
        get(url) {
            return this.request('GET', url);
        },
        post(url, body) {
            return this.request('POST', url, body);
        },
    };

    /**
     * 工具函数：用于从不同 Controller 的响应中提取统一的数据数组。
     */
    function normalizeListPayload(payload) {
        if (Array.isArray(payload)) {
            return payload;
        }
        if (payload?.items && Array.isArray(payload.items)) {
            return payload.items;
        }
        if (payload?.data && Array.isArray(payload.data)) {
            return payload.data;
        }
        if (payload?.records && Array.isArray(payload.records)) {
            return payload.records;
        }
        return [];
    }

    /**
     * 公共的日期格式化，保证表格内显示友好。
     */
    function formatDate(value) {
        if (!value) {
            return '-';
        }
        try {
            const date = new Date(value);
            if (Number.isNaN(date.getTime())) {
                return value;
            }
            return date.toLocaleString('zh-CN', { hour12: false });
        } catch (e) {
            return value;
        }
    }

    window.AdminApp = {
        tokenKey: TOKEN_KEY,
        apiClient,
        formatDate,

        /**
         * 顶层 Shell：负责导航状态、Token 存储以及头部展示信息。
         */
        shell() {
            return {
                activePanel: 'dashboard',
                tokenInput: '',
                tokenMessage: '',
                context: {
                    tenantId: 'default',
                    env: 'dev',
                    user: 'ops-user',
                },
                init() {
                    const savedToken = localStorage.getItem(TOKEN_KEY);
                    if (savedToken) {
                        this.tokenInput = savedToken;
                        this.tokenMessage = '已从 localStorage 读取 Token';
                    }
                },
                switchPanel(panel) {
                    this.activePanel = panel;
                },
                navClass(panel) {
                    return this.activePanel === panel
                        ? 'bg-white/10 text-white'
                        : 'text-slate-300 hover:text-white';
                },
                saveToken() {
                    if (!this.tokenInput) {
                        this.tokenMessage = '请输入 Token';
                        return;
                    }
                    localStorage.setItem(TOKEN_KEY, this.tokenInput.trim());
                    this.tokenMessage = 'Token 已保存，可开始调用 API';
                    setTimeout(() => (this.tokenMessage = ''), 3000);
                },
                clearToken() {
                    localStorage.removeItem(TOKEN_KEY);
                    this.tokenInput = '';
                    this.tokenMessage = 'Token 已清除';
                    setTimeout(() => (this.tokenMessage = ''), 3000);
                },
            };
        },

        /**
         * Outbox 面板：展示最近 Outbox 记录，支持手动重试。
         */
        outboxPanel() {
            return {
                list: [],
                loading: false,
                error: null,
                actionLoading: null,
                formatDate,
                statusClass(status) {
                    if (!status) return 'badge-muted';
                    if (status.includes('SUCCESS')) return 'badge-success';
                    if (status.includes('FAIL') || status.includes('ERROR')) return 'badge-danger';
                    return 'badge-muted';
                },
                async load() {
                    this.error = null;
                    this.loading = true;
                    try {
                        const payload = await apiClient.get('/api/admin/outbox?page=1&pageSize=20');
                        this.list = normalizeListPayload(payload);
                    } catch (err) {
                        this.error = err.message || '加载 Outbox 数据失败';
                    } finally {
                        this.loading = false;
                    }
                },
                async retry(id) {
                    if (!id) return;
                    this.error = null;
                    this.actionLoading = id;
                    try {
                        await apiClient.post(`/api/admin/outbox/${id}/retry`);
                        await this.load();
                    } catch (err) {
                        this.error = err.message || '重试失败';
                    } finally {
                        this.actionLoading = null;
                    }
                },
            };
        },

        /**
         * Scheduler 面板：列出 JobDefinition，并提供手动触发入口。
         */
        schedulerPanel() {
            return {
                jobs: [],
                loading: false,
                error: null,
                actionLoading: null,
                formatDate,
                async load() {
                    this.error = null;
                    this.loading = true;
                    try {
                        const payload = await apiClient.get('/api/admin/scheduler/jobs');
                        this.jobs = normalizeListPayload(payload);
                    } catch (err) {
                        this.error = err.message || '加载 Scheduler 数据失败';
                    } finally {
                        this.loading = false;
                    }
                },
                async trigger(jobId) {
                    if (!jobId) return;
                    this.error = null;
                    this.actionLoading = jobId;
                    try {
                        await apiClient.post(`/api/admin/scheduler/jobs/${jobId}/trigger`);
                    } catch (err) {
                        this.error = err.message || '触发任务失败';
                    } finally {
                        this.actionLoading = null;
                    }
                },
            };
        },

        /**
         * Config 面板：按照 tenant/env 拉取配置，支持前端关键字过滤。
         */
        configPanel() {
            return {
                items: [],
                tenantId: 'default',
                env: 'dev',
                query: '',
                loading: false,
                error: null,
                async load() {
                    if (!this.tenantId || !this.env) {
                        this.error = '请填写 tenantId 与 env';
                        return;
                    }
                    this.error = null;
                    this.loading = true;
                    const params = new URLSearchParams({ tenantId: this.tenantId, env: this.env });
                    try {
                        const payload = await apiClient.get(`/api/admin/config/properties?${params.toString()}`);
                        this.items = normalizeListPayload(payload);
                    } catch (err) {
                        this.error = err.message || '加载配置失败';
                    } finally {
                        this.loading = false;
                    }
                },
                filteredItems() {
                    if (!this.query) {
                        return this.items;
                    }
                    const keyword = this.query.toLowerCase();
                    return this.items.filter((item) =>
                        (item.key || '').toLowerCase().includes(keyword)
                    );
                },
            };
        },

        /**
         * Notification 面板：发送调试通知，展示执行结果。
         */
        notificationPanel() {
            return {
                form: {
                    scenarioCode: '',
                    title: '',
                    content: '',
                    priority: 'NORMAL',
                },
                loading: false,
                message: '',
                error: '',
                reset() {
                    this.form = { scenarioCode: '', title: '', content: '', priority: 'NORMAL' };
                    this.message = '';
                    this.error = '';
                },
                async send() {
                    this.error = '';
                    this.message = '';
                    if (!this.form.scenarioCode || !this.form.title) {
                        this.error = 'ScenarioCode 与 Title 必填';
                        return;
                    }
                    this.loading = true;
                    try {
                        const payload = await apiClient.post('/api/admin/notify/test', this.form);
                        this.message = typeof payload === 'string' ? payload : '调用成功，已发送通知';
                    } catch (err) {
                        this.error = err.message || '发送失败';
                    } finally {
                        this.loading = false;
                    }
                },
            };
        },

        /**
         * Cache Invalidation 面板：展示失效事件统计与最近记录。
         */
        cacheInvalPanel() {
            return {
                window: '5m',
                tenantId: '',
                scope: '',
                namespace: '',
                summary: null,
                recent: [],
                cursor: null,
                loadingSummary: false,
                loadingRecent: false,
                errorSummary: '',
                errorRecent: '',
                formatDate,
                async loadSummary() {
                    this.errorSummary = '';
                    this.loadingSummary = true;
                    try {
                        const params = new URLSearchParams();
                        if (this.window) params.append('window', this.window);
                        this.summary = await apiClient.get(`/ops/api/cache-inval/summary?${params.toString()}`);
                    } catch (err) {
                        this.errorSummary = err.message || '加载统计失败';
                    } finally {
                        this.loadingSummary = false;
                    }
                },
                async loadRecent(reset = false) {
                    if (reset) {
                        this.cursor = null;
                        this.recent = [];
                    }
                    this.errorRecent = '';
                    this.loadingRecent = true;
                    try {
                        const params = new URLSearchParams();
                        if (this.window) params.append('window', this.window);
                        if (this.cursor) params.append('cursor', this.cursor);
                        params.append('limit', '50');
                        if (this.tenantId) params.append('tenantId', this.tenantId);
                        if (this.scope) params.append('scope', this.scope);
                        if (this.namespace) params.append('namespace', this.namespace);
                        const page = await apiClient.get(`/ops/api/cache-inval/recent?${params.toString()}`);
                        const items = normalizeListPayload(page);
                        this.recent = reset ? items : this.recent.concat(items);
                        this.cursor = page.nextCursor || null;
                    } catch (err) {
                        this.errorRecent = err.message || '加载事件列表失败';
                    } finally {
                        this.loadingRecent = false;
                    }
                },
                stormBadge(item) {
                    return item && item.countPerMinute >= item.thresholdPerMinute;
                },
            };
        },
    };

    // 暴露给 Alpine 的函数，方便在 HTML 中直接通过 x-data="xxx()" 调用。
    window.adminShell = () => window.AdminApp.shell();
    window.outboxPanel = () => window.AdminApp.outboxPanel();
    window.schedulerPanel = () => window.AdminApp.schedulerPanel();
    window.configPanel = () => window.AdminApp.configPanel();
    window.notificationPanel = () => window.AdminApp.notificationPanel();
    window.cacheInvalPanel = () => window.AdminApp.cacheInvalPanel();
})();
