<template>
  <div class="patrol-page">
    <header class="command-bar">
      <div><span class="eyebrow">SPATIOTEMPORAL AGENT</span><h1>低空巡检指挥台</h1></div>
      <div class="mission-state"><span :class="['state-dot', stateClass]"></span>{{ mission?.state || '未创建任务' }}</div>
    </header>

    <main class="command-grid">
      <aside class="left-rail">
        <section class="panel task-panel">
          <div class="panel-title">巡检目标</div>
          <textarea v-model="form.goal" rows="5" />
          <div class="field-row"><label>区域<input v-model="form.areaName" /></label><label>时限<input v-model.number="durationMinutes" type="number" min="1" max="60" /></label></div>
          <button class="primary" :disabled="loading" @click="create">创建任务</button>
        </section>
        <section class="panel actions-panel">
          <div class="panel-title">任务控制</div>
          <div class="action-grid">
            <button :disabled="!mission" @click="run(planMission)">规划航线</button>
            <button :disabled="!mission" @click="run(startMission)">启动仿真</button>
            <button :disabled="!mission" @click="run(pauseMission)">暂停任务</button>
            <button :disabled="!mission" @click="replan">动态重规划</button>
            <button class="wide" :disabled="!mission" @click="report">生成巡检报告</button>
          </div>
        </section>
        <section class="panel telemetry-panel">
          <div class="panel-title">飞行状态</div>
          <div class="metrics"><div><strong>{{ mission?.battery_current ?? '--' }}</strong><span>电量 %</span></div><div><strong>{{ mission?.plan_version ?? 0 }}</strong><span>航线版本</span></div><div><strong>{{ mission?.replans ?? 0 }}</strong><span>重规划</span></div><div><strong>{{ waypoints.length }}</strong><span>航点</span></div></div>
        </section>
      </aside>

      <section class="map-stage panel">
        <div class="stage-head"><div><span>东校区风险态势</span><small>20m 风险网格 · 航线仿真</small></div><span>{{ mission?.mission_code || '等待任务' }}</span></div>
        <div class="map-canvas">
          <div class="grid-layer">
            <div v-for="cell in sortedGrid" :key="cell.grid_id" class="risk-cell" :class="riskClass(cell.risk_score)" :title="`风险 ${Number(cell.risk_score).toFixed(2)}`"></div>
          </div>
          <svg class="route-layer" viewBox="0 0 100 100" preserveAspectRatio="none">
            <polyline v-if="routePoints" :points="routePoints" fill="none" stroke="#39d7ff" stroke-width="0.8" stroke-dasharray="2 1" />
          </svg>
          <div v-for="(point, index) in waypointMarkers" :key="point.id" class="waypoint" :style="{left:point.x+'%',top:point.y+'%'}"><span>{{ index + 1 }}</span></div>
          <div class="map-label north">N</div><div class="coverage-legend"><i></i>计划覆盖区域</div>
        </div>
        <div class="stage-foot"><span>风险网格 {{ riskGrid.length }}</span><span>遥测点 {{ telemetry.length }}</span><span>计划版本 V{{ mission?.plan_version || 0 }}</span></div>
      </section>

      <aside class="right-rail panel">
        <div class="panel-title">智能体执行轨迹</div>
        <div class="agent-loop"><span v-for="phase in phases" :key="phase" :class="{active: activePhases.has(phase)}">{{ phase }}</span></div>
        <div class="timeline">
          <article v-for="step in steps" :key="step.step_id"><i></i><div><header><b>{{ step.title }}</b><span>{{ step.state }}</span></header><p>{{ step.detail }}</p><small>{{ step.phase }}</small></div></article>
          <div v-if="!steps.length" class="empty">创建任务后显示执行过程</div>
        </div>
        <div class="panel-title tools-title">工具调用</div>
        <div class="tool-list"><div v-for="tool in tools" :key="tool.execution_id"><b>{{ tool.tool_name }}</b><span>{{ tool.state }}</span><p>{{ tool.output_summary }}</p></div></div>
      </aside>
    </main>

    <el-dialog v-model="reportVisible" title="巡检报告" width="620px"><pre class="report-json">{{ JSON.stringify(reportData, null, 2) }}</pre></el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createMission, generateMissionReport, listMissions, pauseMission, planMission, replanMission, startMission } from '@/api/patrol'

const loading = ref(false)
const mission = ref<any>(null)
const durationMinutes = ref(12)
const form = reactive({ goal: '巡查东校区晚高峰电动车安全情况，重点关注未戴头盔、禁行区和异常聚集。', areaName: '东校区', centerLng: 112.6849, centerLat: 26.8893 })
const reportVisible = ref(false)
const reportData = ref<any>({})
const riskGrid = computed(() => mission.value?.riskGrid || [])
const waypoints = computed(() => mission.value?.waypoints || [])
const telemetry = computed(() => mission.value?.telemetry || [])
const steps = computed(() => mission.value?.steps || [])
const tools = computed(() => mission.value?.tools || [])
const phases = ['UNDERSTAND','OBSERVE','PLAN','EXECUTE','MONITOR','REPLAN','DELIVER']
const activePhases = computed(() => new Set(steps.value.map((s:any) => s.phase)))
const stateClass = computed(() => String(mission.value?.state || '').toLowerCase())
const sortedGrid = computed(() => [...riskGrid.value].sort((a:any,b:any) => a.row_no-b.row_no || a.col_no-b.col_no))
const bounds = computed(() => { const pts=waypoints.value; if(!pts.length)return null; const lngs=pts.map((p:any)=>Number(p.lng)),lats=pts.map((p:any)=>Number(p.lat)); return {minLng:Math.min(...lngs)-.00015,maxLng:Math.max(...lngs)+.00015,minLat:Math.min(...lats)-.00012,maxLat:Math.max(...lats)+.00012} })
const waypointMarkers = computed(() => { const b=bounds.value; if(!b)return []; return waypoints.value.map((p:any)=>({id:p.waypoint_id,x:8+(Number(p.lng)-b.minLng)/(b.maxLng-b.minLng)*84,y:92-(Number(p.lat)-b.minLat)/(b.maxLat-b.minLat)*84})) })
const routePoints = computed(() => waypointMarkers.value.map((p:any)=>`${p.x},${p.y}`).join(' '))
const riskClass = (score:any) => Number(score)>=1.1?'critical':Number(score)>=.95?'high':Number(score)>=.7?'medium':'low'

async function create(){ loading.value=true; try{ mission.value=await createMission({...form,durationLimitSec:durationMinutes.value*60}); ElMessage.success('巡检任务已创建') }catch(e:any){ElMessage.error(e.message||'创建失败')}finally{loading.value=false} }
async function run(fn:any){ if(!mission.value)return; loading.value=true; try{mission.value=await fn(mission.value.mission_id);ElMessage.success('操作完成')}catch(e:any){ElMessage.error(e.message||'操作失败')}finally{loading.value=false} }
async function replan(){ if(!mission.value)return; mission.value=await replanMission(mission.value.mission_id,'发现高风险事件，插入补拍航点'); ElMessage.success('已生成新航线版本') }
async function report(){ if(!mission.value)return; reportData.value=await generateMissionReport(mission.value.mission_id); reportVisible.value=true }
onMounted(async()=>{try{const list=await listMissions();if(list.length)mission.value=list[0]}catch{/* 后端未启动时允许页面打开 */}})
</script>

<style scoped>
.patrol-page{min-height:calc(100vh - 110px);padding:16px;background:#07111c;color:#d9e8f5;font-family:"Microsoft YaHei",sans-serif}.command-bar{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;padding:0 4px}.eyebrow{color:#38d7c4;font-size:11px}.command-bar h1{margin:3px 0 0;font-size:24px;letter-spacing:0}.mission-state{display:flex;align-items:center;gap:8px;color:#8fa9bc}.state-dot{width:9px;height:9px;border-radius:50%;background:#738091}.state-dot.running{background:#36d399;box-shadow:0 0 10px #36d399}.state-dot.planned{background:#39d7ff}.state-dot.paused{background:#ffb447}.command-grid{display:grid;grid-template-columns:270px minmax(440px,1fr) 330px;gap:12px;height:calc(100vh - 175px)}.panel{border:1px solid #17364b;border-radius:6px;background:#0b1a28;box-shadow:0 12px 30px rgba(0,0,0,.2)}.left-rail{display:flex;min-height:0;flex-direction:column;gap:12px}.left-rail .panel{padding:14px}.panel-title{margin-bottom:11px;color:#7fa6bf;font-size:12px;text-transform:uppercase}.task-panel textarea,.task-panel input{box-sizing:border-box;width:100%;border:1px solid #21465d;border-radius:4px;outline:0;color:#e4f0f7;background:#07141f}.task-panel textarea{padding:10px;resize:none;line-height:1.55}.field-row{display:grid;grid-template-columns:1fr 76px;gap:8px;margin:9px 0}.field-row label{color:#708ea2;font-size:11px}.field-row input{display:block;margin-top:4px;padding:7px}.primary,.actions-panel button{border:1px solid #28566e;border-radius:4px;padding:9px;color:#bcd8e8;background:#10283a;cursor:pointer}.primary{width:100%;border-color:#1ab8ac;color:#fff;background:#148f88}.action-grid{display:grid;grid-template-columns:1fr 1fr;gap:7px}.action-grid .wide{grid-column:1/-1}.actions-panel button:disabled,.primary:disabled{opacity:.45;cursor:not-allowed}.telemetry-panel{margin-top:auto}.metrics{display:grid;grid-template-columns:1fr 1fr;gap:9px}.metrics div{padding:9px;border-left:2px solid #1ab8ac;background:#0e2231}.metrics strong,.metrics span{display:block}.metrics strong{font-size:18px}.metrics span{margin-top:3px;color:#64859a;font-size:10px}.map-stage{display:flex;min-height:0;flex-direction:column;padding:13px}.stage-head,.stage-foot{display:flex;align-items:center;justify-content:space-between;color:#82a7bd;font-size:12px}.stage-head small{display:block;margin-top:3px;color:#4f7287}.map-canvas{position:relative;flex:1;margin:12px 0;overflow:hidden;border:1px solid #1c4259;background-color:#071722;background-image:linear-gradient(rgba(39,85,107,.2) 1px,transparent 1px),linear-gradient(90deg,rgba(39,85,107,.2) 1px,transparent 1px);background-size:28px 28px}.grid-layer{position:absolute;inset:12%;display:grid;grid-template-columns:repeat(5,1fr);grid-template-rows:repeat(5,1fr);gap:3px;transform:rotate(-5deg)}.risk-cell{border:1px solid rgba(93,153,177,.2);background:rgba(67,135,157,.12)}.risk-cell.medium{background:rgba(255,187,73,.18)}.risk-cell.high{background:rgba(255,122,61,.3)}.risk-cell.critical{background:rgba(255,60,82,.45);box-shadow:inset 0 0 12px rgba(255,60,82,.5)}.route-layer{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}.waypoint{position:absolute;width:22px;height:22px;transform:translate(-50%,-50%);border:1px solid #39d7ff;border-radius:50%;background:#08283a;box-shadow:0 0 10px rgba(57,215,255,.35);text-align:center;line-height:20px}.waypoint span{color:#b9f3ff;font-size:10px}.north{position:absolute;right:15px;top:14px;color:#39d7ff;font-weight:700}.coverage-legend{position:absolute;left:14px;bottom:14px;color:#6f91a5;font-size:11px}.coverage-legend i{display:inline-block;width:8px;height:8px;margin-right:5px;background:#ff7a3d}.stage-foot{gap:18px;justify-content:flex-start}.right-rail{display:flex;min-height:0;flex-direction:column;padding:14px}.agent-loop{display:flex;gap:4px;flex-wrap:wrap}.agent-loop span{padding:4px 6px;border:1px solid #1b3a4d;border-radius:3px;color:#4f7186;font-size:9px}.agent-loop span.active{border-color:#1ab8ac;color:#75e4d9;background:rgba(26,184,172,.1)}.timeline{margin-top:12px;overflow:auto}.timeline article{display:grid;grid-template-columns:11px 1fr;gap:8px;padding-bottom:13px}.timeline article>i{width:7px;height:7px;margin-top:5px;border-radius:50%;background:#39d7ff}.timeline header{display:flex;justify-content:space-between}.timeline b{font-size:12px}.timeline header span{color:#5bcdbf;font-size:9px}.timeline p{margin:4px 0;color:#7898ac;font-size:11px;line-height:1.45}.timeline small{color:#3f647a}.empty{padding:28px 0;color:#4f7186;text-align:center}.tools-title{margin-top:8px}.tool-list{overflow:auto}.tool-list div{margin-bottom:7px;padding:8px;border-left:2px solid #ffb447;background:#0e2231}.tool-list b{font-size:10px}.tool-list span{float:right;color:#69d5c8;font-size:9px}.tool-list p{margin:4px 0 0;color:#67879b;font-size:9px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.report-json{max-height:420px;overflow:auto;color:#24445a;background:#f3f7f9;padding:12px}@media(max-width:1200px){.command-grid{grid-template-columns:240px 1fr}.right-rail{display:none}}@media(max-width:800px){.command-grid{display:block;height:auto}.left-rail{margin-bottom:12px}.map-stage{height:520px}}
</style>
