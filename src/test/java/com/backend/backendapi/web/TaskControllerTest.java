package com.backend.backendapi.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void taskLifecycleWorksForOwner() throws Exception {
        String username = uniqueUsername();
        String token = register(username);

        long taskId = createTask(token, username, "Comprar pan", "Ir a la panadería");

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .param("user", username)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.user").value(username))
                .andExpect(jsonPath("$.title").value("Comprar pan"))
                .andExpect(jsonPath("$.description").value("Ir a la panadería"))
                .andExpect(jsonPath("$.created_at").isNotEmpty());

        mockMvc.perform(get("/api/tasks/user/{user}", username)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + taskId + ")]").exists());

        mockMvc.perform(get("/api/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + taskId + ")]").exists());

        // Actualizar solo el título no debe borrar la descripción
        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Comprar pan integral\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Comprar pan integral"))
                .andExpect(jsonPath("$.description").value("Ir a la panadería"));

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Comprar pan integral"));

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tarea no encontrada"));
    }

    @Test
    void otherUsersCannotTouchForeignTasks() throws Exception {
        String owner = uniqueUsername();
        String intruder = uniqueUsername();
        String ownerToken = register(owner);
        String intruderToken = register(intruder);

        long taskId = createTask(ownerToken, owner, "Privada", "Solo del dueño");

        mockMvc.perform(get("/api/tasks/user/{user}", owner)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado para ver tareas de otro usuario"));

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .param("user", owner)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado para ver esta tarea"));

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Secuestrada\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado para actualizar esta tarea"));

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado para eliminar esta tarea"));

        mockMvc.perform(delete("/api/tasks/user/{user}/deleteall", owner)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("No autorizado para eliminar tareas de otro usuario"));

        // La tarea sigue intacta
        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Privada"));

        // Crear una tarea a nombre de otro usuario también falla
        mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson(owner, "Intrusa", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado para crear tareas de otro usuario"));
    }

    @Test
    void deleteAllReturnsMessageWhenEmptyAndArrayWhenTasksExist() throws Exception {
        String username = uniqueUsername();
        String token = register(username);

        mockMvc.perform(delete("/api/tasks/user/{user}/deleteall", username)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No hay tareas para eliminar"))
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks").isEmpty());

        createTask(token, username, "Tarea 1", null);
        createTask(token, username, "Tarea 2", null);

        mockMvc.perform(delete("/api/tasks/user/{user}/deleteall", username)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/tasks/user/{user}", username)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void countSameTitlesMatchesFastApiShape() throws Exception {
        String first = uniqueUsername();
        String second = uniqueUsername();
        String title = "Titulo compartido " + UUID.randomUUID();

        String firstToken = register(first);
        String secondToken = register(second);

        createTask(firstToken, first, title, null);
        createTask(secondToken, second, title, null);

        mockMvc.perform(get("/api/tasks/countsames/{user}", first)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0]['Number of titles']").value(1))
                .andExpect(jsonPath("$.results[0].title").value(title));

        mockMvc.perform(get("/api/tasks/countsame/{user}", first)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].title").value(title))
                .andExpect(jsonPath("$.results[0].emails.length()").value(1))
                .andExpect(jsonPath("$.results[0].emails[0]").value(second));
    }

    @Test
    void countSameTitlesExplainsWhenThereIsNothingToCompare() throws Exception {
        String lonely = uniqueUsername();
        String lonelyToken = register(lonely);

        mockMvc.perform(get("/api/tasks/countsames/{user}", lonely)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + lonelyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No tienes tareas creadas."))
                .andExpect(jsonPath("$.results").isEmpty());

        createTask(lonelyToken, lonely, "Sin coincidencias " + UUID.randomUUID(), null);

        mockMvc.perform(get("/api/tasks/countsames/{user}", lonely)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + lonelyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ningún título coincide con otros usuarios."))
                .andExpect(jsonPath("$.results").isEmpty());

        mockMvc.perform(get("/api/tasks/countsame/{user}", lonely)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + lonelyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ningún título coincide con otros usuarios."))
                .andExpect(jsonPath("$.results").isEmpty());

        String intruder = uniqueUsername();
        String intruderToken = register(intruder);

        mockMvc.perform(get("/api/tasks/countsames/{user}", lonely)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado"));
    }

    private String register(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
    }

    private long createTask(String token, String user, String title, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson(user, title, description)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value(user))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private static String taskJson(String user, String title, String description) {
        String descriptionJson = description == null ? "null" : "\"" + description + "\"";
        return "{\"user\":\"" + user + "\",\"title\":\"" + title + "\",\"description\":"
                + descriptionJson + "}";
    }

    private static String uniqueUsername() {
        return "task-" + UUID.randomUUID();
    }
}
