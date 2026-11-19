package com.backend.backendapi.web;

import com.backend.backendapi.dto.task.CountSameTitleDto;
import com.backend.backendapi.dto.task.ResultsResponse;
import com.backend.backendapi.dto.task.SameTitleEmailsDto;
import com.backend.backendapi.dto.task.TaskCreateRequest;
import com.backend.backendapi.dto.task.TaskDto;
import com.backend.backendapi.dto.task.TaskUpdateRequest;
import com.backend.backendapi.security.AuthenticatedUser;
import com.backend.backendapi.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public TaskDto create(@Valid @RequestBody TaskCreateRequest request,
                          @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return taskService.create(request, currentUser.username());
    }

    @GetMapping
    public List<TaskDto> findAll() {
        return taskService.findAll();
    }

    @GetMapping("/user/{user}")
    public List<TaskDto> findByUser(@PathVariable String user,
                                    @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return taskService.findByUser(user, currentUser.username());
    }

    @GetMapping("/{taskId}")
    public TaskDto findById(@PathVariable Long taskId,
                            @RequestParam(required = false) String user,
                            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return taskService.findById(taskId, user, currentUser.username());
    }

    @PutMapping("/{taskId}")
    public TaskDto update(@PathVariable Long taskId,
                          @RequestBody TaskUpdateRequest request,
                          @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return taskService.update(taskId, request, currentUser.username());
    }

    @DeleteMapping("/{taskId}")
    public TaskDto delete(@PathVariable Long taskId,
                          @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return taskService.delete(taskId, currentUser.username());
    }

    @DeleteMapping("/user/{user}/deleteall")
    public Object deleteAll(@PathVariable String user,
                            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return taskService.deleteAll(user, currentUser.username());
    }

    @GetMapping("/countsames/{user}")
    public ResultsResponse<CountSameTitleDto> countSameTitles(@PathVariable String user,
                                                              @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return taskService.countSameTitles(user, currentUser.username());
    }

    @GetMapping("/countsame/{user}")
    public ResultsResponse<SameTitleEmailsDto> countSameTitleEmails(@PathVariable String user,
                                                                    @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return taskService.countSameTitleEmails(user, currentUser.username());
    }
}
