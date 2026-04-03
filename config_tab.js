// 配置页面功能模块
const ConfigTab = {
    template: `
        <div v-if="currentView === 'config'">
            <div class="flex items-center justify-between mb-6">
                <div>
                    <h1 class="text-2xl font-bold">系统配置</h1>
                    <p class="text-gray-400">系统参数和投票规则设置</p>
                </div>
                <div class="flex gap-2">
                    <button @click="loadDefaultConfig" class="btn btn-outline">恢复默认</button>
                    <button @click="saveConfig" class="btn btn-primary">保存配置</button>
                </div>
            </div>
            
            <div class="grid-2 gap-6">
                <!-- 投票设置 -->
                <div class="card">
                    <h2 class="text-lg font-bold mb-4">投票设置</h2>
                    <div class="space-y-4">
                        <div>
                            <label class="block text-sm font-medium mb-2">通过票数阈值</label>
                            <input v-model="config.voteThreshold" type="number" class="input" min="1" max="20">
                            <div class="text-xs text-gray-500 mt-1">达到此票数后投票自动通过</div>
                        </div>
                        <div>
                            <label class="block text-sm font-medium mb-2">投票持续时间 (分钟)</label>
                            <input v-model="config.voteDuration" type="number" class="input" min="1" max="60">
                        </div>
                        <div>
                            <label class="block text-sm font-medium mb-2">投票冷却时间 (分钟)</label>
                            <input v-model="config.voteCooldown" type="number" class="input" min="0" max="1440">
                        </div>
                        <div>
                            <label class="block text-sm font-medium mb-2">允许玩家发起投票</label>
                            <div class="flex items-center gap-2 mt-2">
                                <input type="checkbox" v-model="config.allowPlayerVotes" id="allowPlayerVotes">
                                <label for="allowPlayerVotes" class="text-sm">启用玩家发起的投票</label>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- 信用评分设置 -->
                <div class="card">
                    <h2 class="text-lg font-bold mb-4">信用评分设置</h2>
                    <div class="space-y-4">
                        <div>
                            <label class="block text-sm font-medium mb-2">初始信用分</label>
                            <input v-model="config.defaultCreditScore" type="number" class="input" min="0" max="10" step="0.1">
                        </div>
                        <div>
                            <label class="block text-sm font-medium mb-2">低分警报阈值</label>
                            <input v-model="config.lowScoreThreshold" type="number" class="input" min="0" max="10" step="0.1">
                        </div>
                        <div>
                            <label class="block text-sm font-medium mb-2">高分奖励</label>
                            <input v-model="config.highScoreReward" type="text" class="input" placeholder="如: VIP权限, 额外金币等">
                        </div>
                        <div class="grid-2 gap-3">
                            <div class="p-3 bg-[var(--bg-dark)] rounded-lg">
                                <div class="text-xs text-gray-400">好评权重</div>
                                <div class="text-lg font-bold text-green-400">+{{ config.positiveWeight }}</div>
                            </div>
                            <div class="p-3 bg-[var(--bg-dark)] rounded-lg">
                                <div class="text-xs text-gray-400">差评权重</div>
                                <div class="text-lg font-bold text-red-400">{{ config.negativeWeight }}</div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- 封禁原因管理 -->
                <div class="card">
                    <h2 class="text-lg font-bold mb-4">封禁原因管理</h2>
                    <div class="space-y-4">
                        <div class="flex gap-2 mb-4">
                            <input v-model="newBanReason" type="text" class="input flex-1" placeholder="输入新的封禁原因">
                            <button @click="addBanReason" class="btn btn-primary">添加</button>
                        </div>
                        <div class="flex flex-wrap gap-2">
                            <span v-for="(reason, index) in config.banReasons" :key="index" class="px-3 py-1 bg-[var(--bg-dark)] border border-[var(--border)] rounded-full text-sm flex items-center gap-2">
                                {{ reason }}
                                <button @click="removeBanReason(index)" class="text-gray-400 hover:text-red-400">
                                    <i data-lucide="x" class="w-3 h-3"></i>
                                </button>
                            </span>
                        </div>
                    </div>
                </div>
                
                <!-- 服务器配置同步 -->
                <div class="card">
                    <h2 class="text-lg font-bold mb-4">配置同步</h2>
                    <div class="space-y-4">
                        <div>
                            <label class="block text-sm font-medium mb-2">自动同步间隔 (分钟)</label>
                            <input v-model="config.syncInterval" type="number" class="input" min="0" max="1440">
                            <div class="text-xs text-gray-500 mt-1">0 = 不自动同步</div>
                        </div>
                        <div>
                            <label class="block text-sm font-medium mb-2">同步目标服务器</label>
                            <div class="space-y-2 mt-2">
                                <div v-for="server in servers" :key="server.id" class="flex items-center gap-2">
                                    <input type="checkbox" :id="'server-' + server.id" :value="server.id" v-model="config.syncServers">
                                    <label :for="'server-' + server.id">{{ server.name }}</label>
                                </div>
                            </div>
                        </div>
                        <button @click="syncConfig" class="btn btn-info w-full mt-4">
                            <i data-lucide="refresh-cw" class="w-4 h-4"></i>
                            立即同步到所有服务器
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `,
    
    data() {
        return {
            config: {
                voteThreshold: 3,
                voteDuration: 5,
                voteCooldown: 30,
                allowPlayerVotes: true,
                defaultCreditScore: 5.0,
                lowScoreThreshold: 3.0,
                highScoreReward: '额外的聊天颜色',
                positiveWeight: 1.0,
                negativeWeight: 1.5,
                banReasons: ['作弊/外挂', '恶意破坏', '辱骂他人', '广告宣传', '团队破坏'],
                syncInterval: 30,
                syncServers: []
            },
            newBanReason: ''
        };
    },
    
    props: {
        currentView: String,
        servers: Array
    },
    
    mounted() {
        this.loadConfig();
        this.$nextTick(() => {
            lucide.createIcons();
        });
    },
    
    methods: {
        loadConfig() {
            const savedConfig = localStorage.getItem('pcs-config');
            if (savedConfig) {
                try {
                    this.config = { ...this.config, ...JSON.parse(savedConfig) };
                } catch (e) {
                    console.error('Failed to load config:', e);
                }
            }
        },
        
        saveConfig() {
            localStorage.setItem('pcs-config', JSON.stringify(this.config));
            this.$emit('show-notification', 'success', '配置保存', '系统配置已保存');
            this.$emit('add-log', '配置更新', '系统配置已保存');
        },
        
        loadDefaultConfig() {
            this.config = {
                voteThreshold: 3,
                voteDuration: 5,
                voteCooldown: 30,
                allowPlayerVotes: true,
                defaultCreditScore: 5.0,
                lowScoreThreshold: 3.0,
                highScoreReward: '额外的聊天颜色',
                positiveWeight: 1.0,
                negativeWeight: 1.5,
                banReasons: ['作弊/外挂', '恶意破坏', '辱骂他人', '广告宣传', '团队破坏'],
                syncInterval: 30,
                syncServers: []
            };
            this.$emit('show-notification', 'info', '配置重置', '已恢复默认配置');
        },
        
        addBanReason() {
            if (this.newBanReason.trim()) {
                this.config.banReasons.push(this.newBanReason.trim());
                this.newBanReason = '';
            }
        },
        
        removeBanReason(index) {
            this.config.banReasons.splice(index, 1);
        },
        
        syncConfig() {
            this.$emit('show-notification', 'info', '配置同步', '正在同步配置到所有服务器...');
            this.$emit('add-log', '配置同步', '开始同步系统配置');
            
            // Simulate sync process
            setTimeout(() => {
                this.$emit('show-notification', 'success', '同步完成', '配置已同步到 ' + this.config.syncServers.length + ' 个服务器');
                this.$emit('add-log', '配置同步', '配置同步完成');
            }, 2000);
        }
    }
};