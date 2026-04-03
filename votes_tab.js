// 投票管理功能模块
const VotesTab = {
    template: `
        <div v-if="currentView === 'votes'">
            <div class="flex items-center justify-between mb-6">
                <div>
                    <h1 class="text-2xl font-bold">投票管理</h2>
                    <p class="text-gray-400">跨服投票系统和投票历史</p>
                </div>
                <button @click="openNewVoteModal" class="btn btn-primary">
                    <i data-lucide="plus" class="w-4 h-4"></i>
                    发起投票
                </button>
            </div>
            
            <div class="grid-2 gap-6 mb-8">
                <!-- 进行中投票 -->
                <div class="card">
                    <div class="flex items-center justify-between mb-4">
                        <h2 class="text-lg font-bold">进行中投票</h2>
                        <span class="status-badge status-voting">{{ activeVotes.length }} 进行中</span>
                    </div>
                    
                    <div v-if="activeVotes.length === 0" class="text-center py-8 text-gray-500">
                        <i data-lucide="inbox" class="w-12 h-12 mx-auto mb-3 opacity-50"></i>
                        <p>暂无进行中的投票</p>
                    </div>
                    
                    <div v-else class="space-y-4">
                        <div v-for="vote in activeVotes" :key="vote.id" class="p-4 border border-[var(--border)] rounded-xl hover:border-primary transition-colors">
                            <div class="flex items-start justify-between mb-3">
                                <div>
                                    <div class="font-bold">{{ vote.action }} - {{ vote.targetName }}</div>
                                    <div class="text-sm text-gray-400 mt-1">发起者: {{ vote.starter }}</div>
                                </div>
                                <div class="flex flex-col items-end">
                                    <div class="text-xs text-gray-500">剩余: {{ Math.ceil((vote.expiresAt - Date.now()) / 60000) }}分钟</div>
                                    <div :class="['status-badge mt-2', vote.agree >= vote.threshold ? 'status-success' : 'status-voting']">
                                        {{ vote.agree }}/{{ vote.threshold }}
                                    </div>
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <div class="text-sm text-gray-500 mb-1">原因: {{ vote.reason }}</div>
                                <div class="text-sm text-gray-500">服务器: {{ vote.server }}</div>
                            </div>
                            
                            <div class="space-y-2">
                                <div class="flex justify-between text-xs">
                                    <span class="text-green-400">同意: {{ vote.agree }}</span>
                                    <span class="text-red-400">反对: {{ vote.deny }}</span>
                                    <span class="text-gray-400">弃权: {{ vote.abstain }}</span>
                                </div>
                                <div class="progress-bar">
                                    <div class="progress-fill" :style="{
                                        width: ((vote.agree + vote.deny) ? (vote.agree / (vote.agree + vote.deny) * 100) : 0) + '%',
                                        background: vote.agree >= vote.threshold ? '#10b981' : '#3b82f6'
                                    }"></div>
                                </div>
                            </div>
                            
                            <div class="flex gap-2 mt-3">
                                <button @click="voteAction(vote.id, 'agree')" class="btn btn-success btn-sm flex-1">
                                    <i data-lucide="thumbs-up" class="w-3 h-3"></i>
                                    同意
                                </button>
                                <button @click="voteAction(vote.id, 'deny')" class="btn btn-danger btn-sm flex-1">
                                    <i data-lucide="thumbs-down" class="w-3 h-3"></i>
                                    反对
                                </button>
                                <button @click="closeVote(vote.id)" class="btn btn-outline btn-sm">
                                    <i data-lucide="x" class="w-3 h-3"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- 投票统计 -->
                <div class="card">
                    <h2 class="text-lg font-bold mb-4">投票统计</h2>
                    
                    <div class="grid-2 gap-3 mb-6">
                        <div class="p-4 bg-[var(--bg-dark)] rounded-lg">
                            <div class="text-xs text-gray-400">今日投票数</div>
                            <div class="text-2xl font-bold">{{ stats.todayVotes }}</div>
                        </div>
                        <div class="p-4 bg-[var(--bg-dark)] rounded-lg">
                            <div class="text-xs text-gray-400">通过率</div>
                            <div class="text-2xl font-bold text-green-400">{{ stats.passRate }}%</div>
                        </div>
                    </div>
                    
                    <div class="space-y-4">
                        <div>
                            <div class="text-sm font-medium mb-2">投票类型分布</div>
                            <div class="space-y-2">
                                <div v-for="type in voteTypes" :key="type.name" class="space-y-1">
                                    <div class="flex justify-between text-xs">
                                        <span>{{ type.name }}</span>
                                        <span>{{ type.count }}</span>
                                    </div>
                                    <div class="progress-bar">
                                        <div class="progress-fill" :style="{
                                            width: (type.count / stats.totalVotes * 100) + '%',
                                            background: type.color
                                        }"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- 投票历史 -->
            <div class="card">
                <h2 class="text-lg font-bold mb-4">投票历史</h2>
                
                <table class="table">
                    <thead>
                        <tr>
                            <th>投票内容</th>
                            <th>类型</th>
                            <th>结果</th>
                            <th>票数</th>
                            <th>发起时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="vote in voteHistory" :key="vote.id">
                            <td>
                                <div class="font-medium">{{ vote.action }} - {{ vote.targetName }}</div>
                                <div class="text-xs text-gray-500">原因: {{ vote.reason }}</div>
                            </td>
                            <td>
                                <span :class="['status-badge', getTypeClass(vote.action)]">
                                    {{ getActionName(vote.action) }}
                                </span>
                            </td>
                            <td>
                                <span :class="['status-badge', vote.passed ? 'status-success' : 'status-danger']">
                                    {{ vote.passed ? '通过' : '否决' }}
                                </span>
                            </td>
                            <td>
                                <div class="text-sm">
                                    <span class="text-green-400">✓ {{ vote.agree }}</span>
                                    <span class="text-red-400"> ✗ {{ vote.deny }}</span>
                                </div>
                            </td>
                            <td>
                                <div class="text-sm">{{ formatTime(vote.createdAt) }}</div>
                            </td>
                            <td>
                                <button @click="viewVoteDetails(vote)" class="btn btn-sm btn-outline">
                                    <i data-lucide="eye" class="w-3 h-3"></i>
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
                
                <div class="flex justify-center mt-4">
                    <button @click="loadMoreHistory" class="btn btn-outline">
                        加载更多历史记录
                    </button>
                </div>
            </div>
            
            <!-- 投票统计图表会在更高级版本中实现 -->
        </div>
    `,
    
    data() {
        return {
            activeVotes: [
                {
                    id: 'vote-001',
                    action: 'kick',
                    targetName: 'Griefer123',
                    reason: '恶意破坏他人建筑',
                    starter: 'Player001',
                    server: '主城服务器',
                    agree: 3,
                    deny: 1,
                    abstain: 0,
                    threshold: 4,
                    expiresAt: Date.now() + 1800000 // 30分钟后过期
                },
                {
                    id: 'vote-002',
                    action: 'mute',
                    targetName: 'Spammer456',
                    reason: '刷屏广告信息',
                    starter: 'Moderator001',
                    server: '生存服务器',
                    agree: 5,
                    deny: 0,
                    abstain: 2,
                    threshold: 3,
                    expiresAt: Date.now() + 900000 // 15分钟后过期
                }
            ],
            
            voteHistory: [
                {
                    id: 'vote-history-001',
                    action: 'kick',
                    targetName: 'Troller789',
                    reason: '恶意捣乱新手区',
                    passed: true,
                    agree: 7,
                    deny: 2,
                    createdAt: Date.now() - 86400000
                },
                {
                    id: 'vote-history-002',
                    action: 'ban',
                    targetName: 'Hacker000',
                    reason: '使用外挂程序',
                    passed: true,
                    agree: 12,
                    deny: 0,
                    createdAt: Date.now() - 172800000
                },
                {
                    id: 'vote-history-003',
                    action: 'mute',
                    targetName: 'Noisy123',
                    reason: '辱骂其他玩家',
                    passed: false,
                    agree: 4,
                    deny: 5,
                    createdAt: Date.now() - 259200000
                }
            ],
            
            stats: {
                todayVotes: 8,
                passRate: 68,
                totalVotes: 156
            },
            
            voteTypes: [
                { name: '踢出投票', count: 72, color: '#f59e0b' },
                { name: '封禁投票', count: 34, color: '#ef4444' },
                { name: '禁言投票', count: 28, color: '#6366f1' },
                { name: '其他投票', count: 22, color: '#10b981' }
            ]
        };
    },
    
    props: {
        currentView: String
    },
    
    mounted() {
        this.$nextTick(() => {
            lucide.createIcons();
        });
    },
    
    methods: {
        formatTime(timestamp) {
            return new Date(timestamp).toLocaleString('zh-CN', {
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        },
        
        getActionName(action) {
            const actions = {
                kick: '踢出',
                ban: '封禁',
                mute: '禁言',
                warn: '警告'
            };
            return actions[action] || action;
        },
        
        getTypeClass(action) {
            const classes = {
                kick: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
                ban: 'bg-red-500/20 text-red-400 border-red-500/30',
                mute: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
                warn: 'bg-orange-500/20 text-orange-400 border-orange-500/30'
            };
            return classes[action] || 'bg-gray-500/20 text-gray-400 border-gray-500/30';
        },
        
        openNewVoteModal() {
            this.$emit('open-modal', '发起投票', this.getNewVoteModalContent(), this.submitNewVote);
        },
        
        getNewVoteModalContent() {
            return `
                <div class="space-y-4">
                    <div>
                        <label class="block text-sm font-medium mb-2">投票类型</label>
                        <select id="newVoteType" class="select">
                            <option value="kick">踢出投票</option>
                            <option value="ban">封禁投票</option>
                            <option value="mute">禁言投票</option>
                            <option value="warn">警告投票</option>
                        </select>
                    </div>
                    
                    <div>
                        <label class="block text-sm font-medium mb-2">目标玩家</label>
                        <input id="newVoteTarget" type="text" class="input" placeholder="输入玩家名">
                    </div>
                    
                    <div>
                        <label class="block text-sm font-medium mb-2">原因</label>
                        <select id="newVoteReason" class="select">
                            <option value="恶意破坏">恶意破坏</option>
                            <option value="使用外挂">使用外挂</option>
                            <option value="辱骂他人">辱骂他人</option>
                            <option value="广告刷屏">广告刷屏</option>
                            <option value="其他">其他原因</option>
                        </select>
                    </div>
                    
                    <div v-if="document.getElementById('newVoteReason').value === '其他'">
                        <input id="newVoteCustomReason" type="text" class="input mt-2" placeholder="请输入具体原因">
                    </div>
                    
                    <div>
                        <label class="block text-sm font-medium mb-2">目标服务器</label>
                        <select id="newVoteServer" class="select">
                            <option value="all">所有服务器</option>
                            <option value="spigot-1">主城服务器</option>
                            <option value="fabric-1">生存服务器</option>
                            <option value="forge-1">模组服务器</option>
                        </select>
                    </div>
                    
                    <div>
                        <label class="block text-sm font-medium mb-2">持续时间 (分钟)</label>
                        <input id="newVoteDuration" type="number" class="input" value="10" min="1" max="60">
                    </div>
                </div>
            `;
        },
        
        submitNewVote() {
            const type = document.getElementById('newVoteType')?.value || 'kick';
            const target = document.getElementById('newVoteTarget')?.value || '';
            let reason = document.getElementById('newVoteReason')?.value || '';
            
            if (reason === '其他') {
                reason = document.getElementById('newVoteCustomReason')?.value || '其他原因';
            }
            
            const server = document.getElementById('newVoteServer')?.value || 'all';
            const duration = parseInt(document.getElementById('newVoteDuration')?.value) || 10;
            
            if (!target) {
                alert('请填写目标玩家名');
                return;
            }
            
            const newVote = {
                id: 'vote-' + Date.now(),
                action: type,
                targetName: target,
                reason: reason,
                starter: '管理员',
                server: server === 'all' ? '所有服务器' : server,
                agree: 0,
                deny: 0,
                abstain: 0,
                threshold: 4,
                expiresAt: Date.now() + duration * 60000
            };
            
            this.activeVotes.push(newVote);
            this.stats.todayVotes++;
            
            this.$emit('show-notification', 'success', '投票已发起', `${this.getActionName(type)}投票已发起`);
            this.$emit('add-log', '投票管理', `发起投票: ${target} (${reason})`);
        },
        
        voteAction(voteId, action) {
            const vote = this.activeVotes.find(v => v.id === voteId);
            if (!vote) return;
            
            // 在实际应用中，这里会发送API请求
            if (action === 'agree') {
                vote.agree++;
                this.$emit('show-notification', 'info', '投票已提交', '您已投出同意票');
            } else if (action === 'deny') {
                vote.deny++;
                this.$emit('show-notification', 'info', '投票已提交', '您已投出反对票');
            }
            
            // 检查是否通过
            if (vote.agree >= vote.threshold) {
                this.$emit('show-notification', 'success', '投票通过', `${vote.action}投票已通过`);
                this.moveToHistory(vote);
            }
        },
        
        closeVote(voteId) {
            if (!confirm('确定要关闭这个投票吗？')) return;
            
            const voteIndex = this.activeVotes.findIndex(v => v.id === voteId);
            if (voteIndex === -1) return;
            
            const vote = this.activeVotes[voteIndex];
            const passed = vote.agree > vote.deny;
            
            this.addToHistory(vote, passed);
            this.activeVotes.splice(voteIndex, 1);
            
            this.$emit('show-notification', 'info', '投票已关闭', `投票已关闭，结果: ${passed ? '通过' : '否决'}`);
        },
        
        addToHistory(vote, passed) {
            this.voteHistory.unshift({
                ...vote,
                passed: passed,
                createdAt: Date.now()
            });
            
            if (this.voteHistory.length > 50) {
                this.voteHistory.pop();
            }
        },
        
        moveToHistory(vote) {
            const voteIndex = this.activeVotes.findIndex(v => v.id === vote.id);
            if (voteIndex === -1) return;
            
            this.activeVotes.splice(voteIndex, 1);
            
            this.voteHistory.unshift({
                ...vote,
                passed: true,
                createdAt: Date.now()
            });
            
            this.updateStats();
        },
        
        viewVoteDetails(vote) {
            const content = `
                <div class="space-y-4">
                    <div class="grid-2 gap-3">
                        <div class="p-3 bg-[var(--bg-dark)] rounded-lg">
                            <div class="text-xs text-gray-400">投票类型</div>
                            <div class="font-medium">${this.getActionName(vote.action)}</div>
                        </div>
                        <div class="p-3 bg-[var(--bg-dark)] rounded-lg">
                            <div class="text-xs text-gray-400">目标玩家</div>
                            <div class="font-medium">${vote.targetName}</div>
                        </div>
                    </div>
                    
                    <div>
                        <div class="text-sm font-medium mb-1">投票原因</div>
                        <div class="p-3 bg-[var(--bg-dark)] rounded-lg text-sm">${vote.reason}</div>
                    </div>
                    
                    <div>
                        <div class="text-sm font-medium mb-1">投票结果</div>
                        <div class="grid-3 gap-2">
                            <div class="p-3 bg-green-500/20 border border-green-500/30 rounded-lg text-center">
                                <div class="text-lg font-bold text-green-400">${vote.agree}</div>
                                <div class="text-xs text-green-400">同意</div>
                            </div>
                            <div class="p-3 bg-red-500/20 border border-red-500/30 rounded-lg text-center">
                                <div class="text-lg font-bold text-red-400">${vote.deny}</div>
                                <div class="text-xs text-red-400">反对</div>
                            </div>
                            <div class="p-3 bg-blue-500/20 border border-blue-500/30 rounded-lg text-center">
                                <div class="text-lg font-bold text-blue-400">${vote.abstain || 0}</div>
                                <div class="text-xs text-blue-400">弃权</div>
                            </div>
                        </div>
                    </div>
                    
                    <div v-if="vote.starter">
                        <div class="text-sm font-medium mb-1">发起信息</div>
                        <div class="text-sm">发起者: ${vote.starter}</div>
                        <div class="text-sm text-gray-500">服务器: ${vote.server}</div>
                        <div class="text-sm text-gray-500">时间: ${this.formatTime(vote.createdAt)}</div>
                    </div>
                </div>
            `;
            
            this.$emit('open-modal', '投票详情 - ' + vote.targetName, content);
        },
        
        loadMoreHistory() {
            // 模拟加载更多历史记录
            this.$emit('show-notification', 'info', '加载中', '正在加载更多投票历史...');
            
            setTimeout(() => {
                // 在实际应用中，这里会从API加载更多数据
                this.$emit('show-notification', 'success', '加载完成', '已加载更多投票历史');
            }, 1000);
        },
        
        updateStats() {
            // 更新统计数据
            const totalVotes = this.voteHistory.length;
            const passedVotes = this.voteHistory.filter(v => v.passed).length;
            this.stats.passRate = totalVotes > 0 ? Math.round((passedVotes / totalVotes) * 100) : 0;
        }
    }
};