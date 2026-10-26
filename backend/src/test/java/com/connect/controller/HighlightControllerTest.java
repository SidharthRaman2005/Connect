package com.connect.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * End-to-end tests for the M4.1 highlight endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.dev.seed-dummies=false")
class HighlightControllerTest {

    private static final String BODY = """
            {"title":"%s","description":"d","tags":["Badminton","National"],
             "link":"https://news.example/win","status":"COMPLETED"}""";

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
        s.setGoogleId("hl-" + key);
        s.setEmail(key + "@vitapstudent.ac.in");
        s.setName("Student " + key);
        s.setProfileVisibility(ProfileVisibility.PUBLIC);
        return studentRepository.save(s);
    }

    private String token(Student s) {
        return jwtService.issueToken(s);
    }

    private long create(Student owner, String title) throws Exception {
        String res = mockMvc.perform(post("/api/highlights")
                        .header("Authorization", "Bearer " + token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted(title)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("id").asLong();
    }

    @Test
    void requiresAuth() throws Exception {
        mockMvc.perform(get("/api/highlights/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void createAndListMine() throws Exception {
        Student me = save("mine");
        long id = create(me, "National Badminton Title");

        mockMvc.perform(get("/api/highlights/me").header("Authorization", "Bearer " + token(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) id)))
                .andExpect(jsonPath("$[0].tags", hasItem("National")));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        Student me = save("blank");
        mockMvc.perform(post("/api/highlights")
                        .header("Authorization", "Bearer " + token(me))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateByNonOwnerIsForbidden() throws Exception {
        Student owner = save("owner");
        Student other = save("other");
        long id = create(owner, "Owned");

        mockMvc.perform(put("/api/highlights/" + id)
                        .header("Authorization", "Bearer " + token(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("Hijack")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRemoves() throws Exception {
        Student me = save("del");
        long id = create(me, "Temp");

        mockMvc.perform(delete("/api/highlights/" + id).header("Authorization", "Bearer " + token(me)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/highlights/me").header("Authorization", "Bearer " + token(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void studentHighlightsShowsOwners() throws Exception {
        Student owner = save("showcase");
        Student viewer = save("viewer");
        long id = create(owner, "Public Highlight");

        mockMvc.perform(get("/api/students/" + owner.getId() + "/highlights")
                        .header("Authorization", "Bearer " + token(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
    }
}
