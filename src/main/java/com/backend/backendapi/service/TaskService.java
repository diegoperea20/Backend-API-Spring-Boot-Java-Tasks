package com.backend.backendapi.service;

import com.backend.backendapi.domain.Task;
import com.backend.backendapi.dto.task.CountSameTitleDto;
import com.backend.backendapi.dto.task.DeleteAllResponse;
import com.backend.backendapi.dto.task.ResultsResponse;
import com.backend.backendapi.dto.task.SameTitleEmailsDto;
import com.backend.backendapi.dto.task.TaskCreateRequest;
import com.backend.backendapi.dto.task.TaskDto;
import com.backend.backendapi.dto.task.TaskUpdateRequest;
import com.backend.backendapi.dto.task.TitleUserRow;
import com.backend.backendapi.repository.TaskRepository;
import com.backend.backendapi.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final TaskRepository tasks;

    public TaskService(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public List<TaskDto> findAll() {
        return tasks.findAllByOrderByIdAsc().stream().map(TaskDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDto> findByUser(String user, String currentUsername) {
        requireSameUser(user, currentUsername, "No autorizado para ver tareas de otro usuario");
        return tasks.findByUserOrderByIdAsc(user).stream().map(TaskDto::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskDto findById(Long taskId, String requestedUser, String currentUsername) {
        // Igual que FastAPI: se busca por id + usuario y luego se verifica que sea
        // el usuario autenticado (404 si no existe para ese usuario, 403 si es ajeno).
        String searchUser = StringUtils.hasText(requestedUser) ? requestedUser : currentUsername;
        Task task = tasks.findByIdAndUser(taskId, searchUser)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));

        if (!task.getUser().equals(currentUsername)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No autorizado para ver esta tarea");
        }
        return TaskDto.from(task);
    }

    @Transactional
    public TaskDto create(TaskCreateRequest request, String currentUsername) {
        if (!request.user().equals(currentUsername)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "No autorizado para crear tareas de otro usuario");
        }

        Task task = tasks.save(new Task(request.user(), request.title(), request.description()));
        return TaskDto.from(task);
    }

    @Transactional
    public TaskDto update(Long taskId, TaskUpdateRequest request, String currentUsername) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));

        if (!task.getUser().equals(currentUsername)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "No autorizado para actualizar esta tarea");
        }

        if (request.user() != null && !request.user().isEmpty()) {
            task.setUser(request.user());
        }
        if (request.title() != null && !request.title().isEmpty()) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }

        tasks.saveAndFlush(task);
        return TaskDto.from(task);
    }

    @Transactional
    public TaskDto delete(Long taskId, String currentUsername) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));

        if (!task.getUser().equals(currentUsername)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "No autorizado para eliminar esta tarea");
        }

        TaskDto deleted = TaskDto.from(task);
        tasks.delete(task);
        return deleted;
    }

    @Transactional
    public Object deleteAll(String user, String currentUsername) {
        requireSameUser(user, currentUsername,
                "No autorizado para eliminar tareas de otro usuario");

        List<Task> found = tasks.findByUserOrderByIdAsc(user);
        if (found.isEmpty()) {
            return new DeleteAllResponse("No hay tareas para eliminar", List.of());
        }

        List<TaskDto> deleted = found.stream().map(TaskDto::from).toList();
        tasks.deleteAll(found);
        return deleted;
    }

    @Transactional(readOnly = true)
    public ResultsResponse<CountSameTitleDto> countSameTitles(String user, String currentUsername) {
        requireSameUser(user, currentUsername, "No autorizado");

        if (!tasks.existsByUser(user)) {
            return ResultsResponse.withMessage("No tienes tareas creadas.");
        }

        List<CountSameTitleDto> results = tasks.countOtherUsersWithSameTitles(user).stream()
                .map(CountSameTitleDto::from)
                .toList();

        if (results.isEmpty()) {
            return ResultsResponse.withMessage("Ningún título coincide con otros usuarios.");
        }
        return ResultsResponse.of(results);
    }

    @Transactional(readOnly = true)
    public ResultsResponse<SameTitleEmailsDto> countSameTitleEmails(String user, String currentUsername) {
        requireSameUser(user, currentUsername, "No autorizado");

        List<TitleUserRow> rows = tasks.findOtherUsersWithSameTitles(user);
        if (rows.isEmpty()) {
            return ResultsResponse.withMessage("Ningún título coincide con otros usuarios.");
        }

        Map<String, List<String>> emailsByTitle = new LinkedHashMap<>();
        for (TitleUserRow row : rows) {
            emailsByTitle.computeIfAbsent(row.title(), key -> new ArrayList<>()).add(row.user());
        }

        List<SameTitleEmailsDto> results = emailsByTitle.entrySet().stream()
                .map(entry -> new SameTitleEmailsDto(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
        return ResultsResponse.of(results);
    }

    private static void requireSameUser(String user, String currentUsername, String message) {
        if (!user.equals(currentUsername)) {
            throw new ApiException(HttpStatus.FORBIDDEN, message);
        }
    }
}
