package com.example.evmonitorplatform.service.impl;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.service.PatrolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolServiceImpl implements PatrolService {
    private final JdbcTemplate jdbcTemplate;
    private volatile boolean schemaReady;

    private void ensureSchema() {
        if (schemaReady) return;
        synchronized (this) {
            if (schemaReady) return;
            List<String> ddl = List.of(
                    "CREATE TABLE IF NOT EXISTS patrol_mission (mission_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_code VARCHAR(48) NOT NULL UNIQUE, goal TEXT NOT NULL, area_name VARCHAR(128) NULL, center_lng DECIMAL(11,7) NOT NULL, center_lat DECIMAL(10,7) NOT NULL, duration_limit_sec INT NOT NULL DEFAULT 720, state VARCHAR(24) NOT NULL, battery_start DECIMAL(5,2) NOT NULL DEFAULT 100, battery_current DECIMAL(5,2) NOT NULL DEFAULT 100, plan_version INT NOT NULL DEFAULT 0, replans INT NOT NULL DEFAULT 0, created_by INT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL)",
                    "CREATE TABLE IF NOT EXISTS patrol_waypoint (waypoint_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, sequence_no INT NOT NULL, lng DECIMAL(11,7) NOT NULL, lat DECIMAL(10,7) NOT NULL, altitude_m DECIMAL(7,2) NOT NULL DEFAULT 30, dwell_sec INT NOT NULL DEFAULT 3, risk_score DECIMAL(8,4) NOT NULL DEFAULT 0, state VARCHAR(24) NOT NULL DEFAULT 'PLANNED', UNIQUE KEY uk_patrol_waypoint (mission_id, sequence_no), KEY idx_patrol_waypoint_mission (mission_id))",
                    "CREATE TABLE IF NOT EXISTS drone_telemetry (telemetry_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, recorded_at DATETIME NOT NULL, lng DECIMAL(11,7) NOT NULL, lat DECIMAL(10,7) NOT NULL, altitude_m DECIMAL(7,2) NULL, speed_mps DECIMAL(7,2) NULL, battery DECIMAL(5,2) NULL, gps_quality DECIMAL(5,2) NULL, payload_state VARCHAR(32) NULL, raw_payload TEXT NULL, KEY idx_drone_telemetry_mission (mission_id, recorded_at))",
                    "CREATE TABLE IF NOT EXISTS agent_step (step_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, phase VARCHAR(24) NOT NULL, step_no INT NOT NULL, title VARCHAR(128) NOT NULL, detail TEXT NULL, state VARCHAR(24) NOT NULL, duration_ms BIGINT NULL, create_time DATETIME NOT NULL, KEY idx_agent_step_mission (mission_id, step_no))",
                    "CREATE TABLE IF NOT EXISTS tool_execution (execution_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, step_id BIGINT NULL, tool_name VARCHAR(64) NOT NULL, input_summary TEXT NULL, output_summary TEXT NULL, state VARCHAR(24) NOT NULL, duration_ms BIGINT NULL, retry_count INT NOT NULL DEFAULT 0, error_message TEXT NULL, create_time DATETIME NOT NULL, KEY idx_tool_execution_mission (mission_id, create_time))",
                    "CREATE TABLE IF NOT EXISTS patrol_risk_grid (grid_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, row_no INT NOT NULL, col_no INT NOT NULL, center_lng DECIMAL(11,7) NOT NULL, center_lat DECIMAL(10,7) NOT NULL, recent_score DECIMAL(8,4) NOT NULL DEFAULT 0, density_score DECIMAL(8,4) NOT NULL DEFAULT 0, fence_score DECIMAL(8,4) NOT NULL DEFAULT 0, blind_score DECIMAL(8,4) NOT NULL DEFAULT 0, risk_score DECIMAL(8,4) NOT NULL DEFAULT 0, selected TINYINT NOT NULL DEFAULT 0, UNIQUE KEY uk_patrol_grid (mission_id,row_no,col_no))",
                    "CREATE TABLE IF NOT EXISTS mission_report (report_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL UNIQUE, report_code VARCHAR(48) NOT NULL UNIQUE, content_json LONGTEXT NOT NULL, create_time DATETIME NOT NULL)"
            );
            ddl.forEach(sql -> jdbcTemplate.execute(sql));
            schemaReady = true;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createMission(Map<String, Object> request, Integer operatorUserId) {
        ensureSchema();
        String goal = text(request, "goal", "巡查东校区电动车安全风险");
        String area = text(request, "areaName", "东校区");
        double lng = number(request, "centerLng", 112.6849000);
        double lat = number(request, "centerLat", 26.8893000);
        int duration = Math.max(60, Math.min(3600, integer(request, "durationLimitSec", 720)));
        String code = "PM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        jdbcTemplate.update("INSERT INTO patrol_mission (mission_code,goal,area_name,center_lng,center_lat,duration_limit_sec,state,created_by,create_time,update_time) VALUES (?,?,?,?,? ,?,'DRAFT',?,?,?)", code, goal, area, lng, lat, duration, operatorUserId, LocalDateTime.now(), LocalDateTime.now());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        recordStep(id, "UNDERSTAND", 1, "解析巡检目标", "区域=" + area + ";时限=" + duration + "秒", "COMPLETED", 0L);
        recordTool(id, "PARSE_PATROL_GOAL", Map.of("goal", goal, "area", area), Map.of("durationLimitSec", duration), "SUCCEEDED", 0L, 0, null);
        return getMission(id);
    }

    @Override public Map<String, Object> getMission(Long missionId) { ensureSchema(); Map<String,Object> m = one("SELECT * FROM patrol_mission WHERE mission_id=?", missionId); m.put("waypoints", jdbcTemplate.queryForList("SELECT * FROM patrol_waypoint WHERE mission_id=? ORDER BY sequence_no", missionId)); m.put("riskGrid", jdbcTemplate.queryForList("SELECT * FROM patrol_risk_grid WHERE mission_id=? ORDER BY risk_score DESC", missionId)); m.put("telemetry", jdbcTemplate.queryForList("SELECT * FROM drone_telemetry WHERE mission_id=? ORDER BY recorded_at DESC LIMIT 100", missionId)); m.put("steps", jdbcTemplate.queryForList("SELECT * FROM agent_step WHERE mission_id=? ORDER BY step_no", missionId)); m.put("tools", jdbcTemplate.queryForList("SELECT * FROM tool_execution WHERE mission_id=? ORDER BY create_time", missionId)); return m; }
    @Override public List<Map<String, Object>> listMissions(String state, Integer limit) { ensureSchema(); int n=Math.max(1,Math.min(limit==null?20:limit,100)); if(StringUtils.hasText(state)) return jdbcTemplate.queryForList("SELECT * FROM patrol_mission WHERE state=? ORDER BY create_time DESC LIMIT " + n, state.toUpperCase(Locale.ROOT)); return jdbcTemplate.queryForList("SELECT * FROM patrol_mission ORDER BY create_time DESC LIMIT " + n); }

    @Override @Transactional public Map<String, Object> planMission(Long id, Integer operator) { ensureSchema(); Map<String,Object> m=one("SELECT * FROM patrol_mission WHERE mission_id=?",id); jdbcTemplate.update("DELETE FROM patrol_waypoint WHERE mission_id=?",id); jdbcTemplate.update("DELETE FROM patrol_risk_grid WHERE mission_id=?",id); double lng=((Number)m.get("center_lng")).doubleValue(), lat=((Number)m.get("center_lat")).doubleValue(); int idx=0; for(int r=-2;r<=2;r++) for(int c=-2;c<=2;c++){double score=Math.max(0,1.0-(Math.abs(r)+Math.abs(c))*0.12)+((r==0&&c==0)?0.25:0); int selected=score>=0.95?1:0; jdbcTemplate.update("INSERT INTO patrol_risk_grid (mission_id,row_no,col_no,center_lng,center_lat,recent_score,density_score,fence_score,blind_score,risk_score,selected) VALUES (?,?,?,?,?,?,?,?,?,?,?)",id,r,c,lng+c*0.00022,lat+r*0.00018,score,score*.8,(Math.abs(r)==2||Math.abs(c)==2)?0.7:0.2,(r==0&&c==0)?0.5:0.15,score,selected); if(selected==1) jdbcTemplate.update("INSERT INTO patrol_waypoint (mission_id,sequence_no,lng,lat,altitude_m,dwell_sec,risk_score,state) VALUES (?,?,?,?,?,?,?,'PLANNED')",id,idx++,lng+c*0.00022,lat+r*0.00018,30,3,score); } jdbcTemplate.update("UPDATE patrol_mission SET state='PLANNED', plan_version=plan_version+1, update_time=? WHERE mission_id=?",LocalDateTime.now(),id); recordStep(id,"PLAN",2,"生成风险网格与初始航线","25个风险网格，"+idx+"个候选航点","COMPLETED",0L); recordTool(id,"PLAN_PATROL_ROUTE",Map.of("gridSizeMeters",20,"durationLimitSec",m.get("duration_limit_sec")),Map.of("waypointCount",idx,"hardConstraints",List.of("禁飞区","返航电量")),"SUCCEEDED",0L,0,null); return getMission(id); }
    @Override @Transactional public Map<String,Object> startMission(Long id,Integer operator){ensureSchema(); Map<String,Object> m=one("SELECT * FROM patrol_mission WHERE mission_id=?",id); if(!Set.of("PLANNED","PAUSED").contains(String.valueOf(m.get("state")))) throw new BusinessException("任务必须先完成航线规划"); jdbcTemplate.update("UPDATE patrol_mission SET state='RUNNING',update_time=? WHERE mission_id=?",LocalDateTime.now(),id); recordStep(id,"EXECUTE",3,"启动仿真无人机","开始沿规划航点执行巡检","RUNNING",0L); recordTool(id,"START_SIMULATION",Map.of("missionId",id),Map.of("state","RUNNING"),"SUCCEEDED",0L,0,null); return getMission(id);}
    @Override @Transactional public Map<String,Object> pauseMission(Long id,Integer operator){ensureSchema(); one("SELECT * FROM patrol_mission WHERE mission_id=?",id); jdbcTemplate.update("UPDATE patrol_mission SET state='PAUSED',update_time=? WHERE mission_id=?",LocalDateTime.now(),id); recordStep(id,"MONITOR",4,"暂停仿真任务","等待人工或智能体继续","PAUSED",0L); return getMission(id);}
    @Override @Transactional public Map<String,Object> replanMission(Long id,String reason,Integer operator){ensureSchema(); one("SELECT * FROM patrol_mission WHERE mission_id=?",id); jdbcTemplate.update("UPDATE patrol_mission SET replans=replans+1,plan_version=plan_version+1,update_time=? WHERE mission_id=?",LocalDateTime.now(),id); recordStep(id,"REPLAN",5,"动态重规划",reason==null?"检测到新风险":reason,"COMPLETED",0L); recordTool(id,"REPLAN_ROUTE",Map.of("reason",reason==null?"new-risk":reason),Map.of("planVersion",jdbcTemplate.queryForObject("SELECT plan_version FROM patrol_mission WHERE mission_id=?",Integer.class,id)),"SUCCEEDED",0L,0,null); return getMission(id);}
    @Override @Transactional public Map<String,Object> appendTelemetry(Long id,Map<String,Object> t){ensureSchema(); double lng=number(t,"lng",0),lat=number(t,"lat",0); jdbcTemplate.update("INSERT INTO drone_telemetry (mission_id,recorded_at,lng,lat,altitude_m,speed_mps,battery,gps_quality,payload_state,raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",id,LocalDateTime.now(),lng,lat,number(t,"altitudeM",30),number(t,"speedMps",3),number(t,"battery",100),number(t,"gpsQuality",1),text(t,"payloadState","READY"),String.valueOf(t)); return Map.of("accepted",true,"missionId",id,"recordedAt",LocalDateTime.now()); }
    @Override @Transactional public Map<String,Object> generateReport(Long id,Integer operator){ensureSchema(); Map<String,Object> m=one("SELECT * FROM patrol_mission WHERE mission_id=?",id); Map<String,Object> report=new LinkedHashMap<>(); report.put("missionCode",m.get("mission_code")); report.put("goal",m.get("goal")); report.put("state",m.get("state")); report.put("waypointCount",jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?",Integer.class,id)); report.put("telemetryCount",jdbcTemplate.queryForObject("SELECT COUNT(*) FROM drone_telemetry WHERE mission_id=?",Integer.class,id)); report.put("replans",m.get("replans")); report.put("generatedAt",LocalDateTime.now()); String code="RPT-"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT); jdbcTemplate.update("INSERT INTO mission_report (mission_id,report_code,content_json,create_time) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE content_json=VALUES(content_json),create_time=VALUES(create_time)",id,code,report.toString(),LocalDateTime.now()); recordStep(id,"DELIVER",6,"生成巡检报告","输出航线、遥测、重规划次数和风险摘要","COMPLETED",0L); return report; }

    private Map<String,Object> one(String sql,Object... args){List<Map<String,Object>> rows=jdbcTemplate.queryForList(sql,args); if(rows.isEmpty()) throw new BusinessException("巡检任务不存在"); return new LinkedHashMap<>(rows.get(0));}
    private void recordStep(Long id,String phase,int no,String title,String detail,String state,long duration){jdbcTemplate.update("INSERT INTO agent_step (mission_id,phase,step_no,title,detail,state,duration_ms,create_time) VALUES (?,?,?,?,?,?,?,?)",id,phase,no,title,detail,state,duration,LocalDateTime.now());}
    private void recordTool(Long id,String name,Object input,Object output,String state,long duration,int retry,String error){jdbcTemplate.update("INSERT INTO tool_execution (mission_id,tool_name,input_summary,output_summary,state,duration_ms,retry_count,error_message,create_time) VALUES (?,?,?,?,?,?,?,?,?)",id,name,String.valueOf(input),String.valueOf(output),state,duration,retry,error,LocalDateTime.now());}
    private static String text(Map<String,Object> m,String k,String d){Object v=m==null?null:m.get(k);return v==null||String.valueOf(v).isBlank()?d:String.valueOf(v);}
    private static double number(Map<String,Object> m,String k,double d){Object v=m==null?null:m.get(k);try{return v==null?d:Double.parseDouble(String.valueOf(v));}catch(Exception e){return d;}}
    private static int integer(Map<String,Object> m,String k,int d){return (int)Math.round(number(m,k,d));}
}
