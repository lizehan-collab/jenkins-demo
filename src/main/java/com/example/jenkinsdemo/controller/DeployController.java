package com.example.jenkinsdemo.controller;

import com.example.jenkinsdemo.service.JenkinsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deploy")
public class DeployController {

    @Autowired
    private JenkinsService jenkinsService;

    /**
     * 触发构建
     * @param jobName
     * @param branch
     * @param env
     * @return
     */
    @PostMapping("/trigger")
    public String trigger(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam(defaultValue = "dev") String env) {
        String buildNumber = jenkinsService.triggerBuild(jobName, branch, env);
        if (buildNumber != null) {
            return "构建已触发！构建编号: " + buildNumber;
        }
        return "触发失败，请检查 Jenkins 服务或任务名称";
    }

    /**
     * 查询构建状态
     * @param jobName
     * @param buildNumber
     * @return
     */
    @GetMapping("/status")
    public String status(@RequestParam String jobName, @RequestParam int buildNumber) {
        String result = jenkinsService.getBuildStatus(jobName, buildNumber);
        return result != null ? result : "查询失败";
    }

    /**
     * 获取构建日志
     * @param jobName
     * @param buildNumber
     * @return
     */
    @GetMapping("/log")
    public String log(@RequestParam String jobName, @RequestParam int buildNumber) {
        String log = jenkinsService.getBuildLog(jobName, buildNumber);
        return log != null ? log : "获取日志失败";
    }
}