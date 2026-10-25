package com.connect.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.repository.StudentRepository;
import com.connect.service.JwtService;

/**
 * End-to-end tests for the M3.2 connection endpoints through the real security
 * chain and persistence. Dummy seeding is disabled for deterministic data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.dev.seed-dummies=false")
class ConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private Student save(String key) {
        Student s = new Student();
        s.setGoogleId("conn-" + key);
        s.setEmail(key + "@vitapstudent.ac.in");
        s.setName("Student " + key);
        s.setBranch("CSE");
        s.setProfileVisibility(ProfileVisibility.PUBLIC);
        return studentRepository.save(s);
    }

    private String token(Student s) {
        return jwtService.issueToken(s);
    }

    @Test
    void sendRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addresseeId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendAcceptAndListFlow() throws Exception {
        Student a = save("flow-a");
        Student b = save("flow-b");

        String response = mockMvc.perform(post("/api/connections")
                        .header("Authorization", "Bearer " + token(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addresseeId\":" + b.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.direction").value("OUTGOING"))
                .andExpect(jsonPath("$.other.id").value(b.getId()))
                .andReturn().getResponse().getContentAsString();

        long connectionId = objectMapper.readTree(response).get("id").asLong();

        // B sees it as incoming and accepts.
        mockMvc.perform(get("/api/connections/incoming")
                        .header("Authorization", "Bearer " + token(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) connectionId)));

        mockMvc.perform(post("/api/connections/" + connectionId + "/accept")
                        .header("Authorization", "Bearer " + token(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Now A lists B among accepted connections.
        mockMvc.perform(get("/api/connections/accepted")
                        .header("Authorization", "Bearer " + token(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].other.id", hasItem(b.getId().intValue())));
    }

    @Test
    void duplicateRequestIsRejected() throws Exception {
        Student a = save("dup-a");
        Student b = save("dup-b");
        String content = "{\"addresseeId\":" + b.getId() + "}";

        mockMvc.perform(post("/api/connections")
                        .header("Authorization", "Bearer " + token(a))
                        .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/connections")
                        .header("Authorization", "Bearer " + token(a))
                        .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotConnectWithSelf() throws Exception {
        Student a = save("self-a");

        mockMvc.perform(post("/api/connections")
                        .header("Authorization", "Bearer " + token(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addresseeId\":" + a.getId() + "}"))
                .andExpect(status().isBadRequest());
    }
}
