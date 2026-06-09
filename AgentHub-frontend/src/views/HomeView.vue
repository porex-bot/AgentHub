<template>
  <div class="home">
    <header class="home-top">
      <div class="home-brand">
        <span class="home-mark">
          <Boxes :size="20" />
        </span>
        <span class="home-logo">AgentHub AI 工作台</span>
      </div>
      <div class="home-status">
        <span class="home-status-dot"></span>
        <span>本地后端</span>
      </div>
    </header>

    <main class="home-main">
      <section class="home-intro">
        <p class="home-kicker">统一智能体入口</p>
        <h1 class="home-title">选择一个智能体模式开始工作</h1>
        <p class="home-desc">
          关系咨询、通用任务和研究报告共用一套流式聊天工作区，按场景切换能力。
        </p>
      </section>

      <section class="home-summary" aria-label="platform capabilities">
        <div class="summary-item">
          <span class="summary-value">3</span>
          <span class="summary-label">智能体模式</span>
        </div>
        <div class="summary-item">
          <span class="summary-value">SSE</span>
          <span class="summary-label">实时流式响应</span>
        </div>
        <div class="summary-item">
          <span class="summary-value">Tools</span>
          <span class="summary-label">搜索 / 抓取 / PDF</span>
        </div>
      </section>

      <section class="home-grid" aria-label="agent list">
        <button
          v-for="app in chatApps"
          :key="app.key"
          class="agent-card"
          :class="`agent-card--${app.key}`"
          @click="goTo(app.route)"
        >
          <span class="agent-accent"></span>
          <span class="agent-icon">
            <component :is="app.icon" :size="20" />
          </span>
          <span class="agent-category">{{ app.category }}</span>
          <span class="agent-name">{{ app.name }}</span>
          <span class="agent-desc">{{ app.description }}</span>
          <span class="agent-tags">
            <span v-for="capability in app.capabilities" :key="capability" class="agent-tag">
              {{ capability }}
            </span>
          </span>
          <span class="agent-action">
            进入工作区
            <ArrowRight :size="16" />
          </span>
        </button>
      </section>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowRight, Boxes } from 'lucide-vue-next'
import { chatApps } from '../config/chatApps'

const router = useRouter()

function goTo(route) {
  router.push(route)
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

.home-top {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  background: var(--bg-surface);
  border-bottom: 1px solid var(--color-neutral-200);
  flex-shrink: 0;
}

.home-brand,
.home-status,
.agent-tags,
.agent-action {
  display: flex;
  align-items: center;
}

.home-brand {
  gap: var(--space-3);
}

.home-mark {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-sm);
}

.home-logo {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.home-status {
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
}

.home-status-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-sm);
  background: var(--color-success);
}

.home-main {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: var(--space-12) var(--space-6);
}

.home-intro {
  max-width: 720px;
  margin-bottom: var(--space-8);
}

.home-kicker {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: var(--space-2);
}

.home-title {
  font-size: var(--text-2xl);
  color: var(--text-primary);
  margin-bottom: var(--space-3);
}

.home-desc {
  font-size: var(--text-base);
  color: var(--text-secondary);
  line-height: 1.5;
}

.home-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-4);
  background: var(--bg-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-md);
}

.summary-value {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.summary-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.home-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

.agent-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-height: 320px;
  padding: var(--space-6);
  text-align: left;
  background: var(--bg-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-md);
  transition: border-color 0.16s, box-shadow 0.16s, transform 0.16s;
}

.agent-card:hover {
  border-color: var(--color-neutral-300);
  box-shadow: var(--shadow-1);
  transform: translateY(-4px);
}

.agent-card--relationship {
  --agent-accent: var(--color-rose);
  --agent-light: #fff1f2;
}

.agent-card--task {
  --agent-accent: var(--color-primary);
  --agent-light: var(--color-primary-light);
}

.agent-card--report {
  --agent-accent: var(--color-teal);
  --agent-light: var(--color-success-light);
}

.agent-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--agent-accent);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.agent-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--agent-accent);
  background: var(--agent-light);
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-4);
}

.agent-category {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  margin-bottom: var(--space-2);
}

.agent-name {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-3);
}

.agent-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: var(--space-4);
}

.agent-tags {
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: auto;
  margin-bottom: var(--space-4);
}

.agent-tag {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-sm);
  background: var(--bg-subtle);
}

.agent-action {
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-primary);
}

@media (max-width: 900px) {
  .home-summary,
  .home-grid {
    grid-template-columns: 1fr;
  }

  .agent-card {
    min-height: 0;
  }
}

@media (max-width: 640px) {
  .home-top {
    height: auto;
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-3);
    padding: var(--space-4);
  }

  .home-main {
    padding: var(--space-8) var(--space-4);
  }

  .home-intro {
    margin-bottom: var(--space-6);
  }

  .home-title {
    font-size: var(--text-xl);
  }

  .home-desc {
    font-size: var(--text-sm);
  }

  .home-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: var(--space-2);
    margin-bottom: var(--space-12);
  }

  .home-grid {
    gap: var(--space-3);
  }

  .summary-item {
    align-items: center;
    min-width: 0;
    padding: var(--space-2);
    text-align: center;
  }

  .summary-value {
    font-size: var(--text-sm);
  }

  .summary-label {
    font-size: var(--text-xs);
    line-height: 1.5;
  }

  .agent-card {
    padding: var(--space-4);
  }

  .agent-icon {
    width: 32px;
    height: 32px;
    margin-bottom: var(--space-3);
  }

  .agent-name {
    font-size: var(--text-base);
    margin-bottom: var(--space-2);
  }

  .agent-desc {
    margin-bottom: var(--space-3);
  }

  .agent-tags {
    gap: var(--space-1);
    margin-bottom: var(--space-3);
  }
}
</style>
