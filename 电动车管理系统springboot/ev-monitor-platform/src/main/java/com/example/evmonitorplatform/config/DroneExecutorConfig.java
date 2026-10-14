package com.example.evmonitorplatform.config;

import com.example.evmonitorplatform.service.drone.DroneAdapter;
import com.example.evmonitorplatform.service.drone.PyBulletDroneAdapter;
import com.example.evmonitorplatform.service.drone.SimulatedDroneAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 无人机执行器装配：ev-monitor.drone.executor 决定 PatrolServiceImpl 注入的实现。
 * simulated=确定性Java仿真（默认）；pybullet=PyBullet物理仿真（需本机启动桥接服务）。
 * 未来接入 PX4 SITL 时新增 px4 分支即可，上层契约不变。
 */
@Configuration
public class DroneExecutorConfig {

    @Bean
    @Primary
    public DroneAdapter droneAdapter(SimulatedDroneAdapter simulated,
                                     PyBulletDroneAdapter pybullet,
                                     @Value("${ev-monitor.drone.executor:simulated}") String executor) {
        return switch (executor == null ? "simulated" : executor.toLowerCase()) {
            case "pybullet" -> pybullet;
            // case "px4" -> px4;  // PX4 SITL 预留
            default -> simulated;
        };
    }
}
