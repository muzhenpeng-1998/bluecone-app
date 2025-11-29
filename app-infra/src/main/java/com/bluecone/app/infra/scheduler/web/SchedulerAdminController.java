package com.bluecone.app.infra.scheduler.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bluecone.app.infra.scheduler.entity.JobDefinitionEntity;
import com.bluecone.app.infra.scheduler.service.JobDefinitionService;

/**
 * 管理接口：任务查看、启停与立即执行。
 */
@RestController
@RequestMapping("/api/admin/scheduler")
public class SchedulerAdminController {

    private final JobDefinitionService jobDefinitionService;

    public SchedulerAdminController(JobDefinitionService jobDefinitionService) {
        this.jobDefinitionService = jobDefinitionService;
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobDefinitionEntity>> listJobs() {
        return ResponseEntity.ok(jobDefinitionService.listAll());
    }

    @PostMapping("/jobs/{code}/run-now")
    public ResponseEntity<Void> runNow(@PathVariable String code) {
        jobDefinitionService.triggerRunNow(code);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/jobs/{code}/enable")
    public ResponseEntity<Void> enable(@PathVariable String code) {
        jobDefinitionService.enable(code);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/jobs/{code}/disable")
    public ResponseEntity<Void> disable(@PathVariable String code) {
        jobDefinitionService.disable(code);
        return ResponseEntity.ok().build();
    }
}
