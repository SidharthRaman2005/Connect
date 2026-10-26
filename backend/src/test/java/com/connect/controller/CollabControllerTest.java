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
 * End-to-end tests for the M4.2 collab endpoints: publishing, requesting,
 * accepting, closing, and notifications. Dummy seeding is off for clean data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.dev.seed-dummies=false")
class CollabControllerTest {

    private static final String POST_BODY =
            "{\"title\":\"%s\",\"description\":\"d\",\"type\":\"PROJECT\"}";

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
        s.setGoogleId("collab-" + key);
        s.setEmail(key + "@vitapstudent.ac.in");
        s.setName("Student " + key);
        s.setProfileVisibility(ProfileVisibility.PUBLIC);
        return studentRepository.save(s);
    }

    private String token(Student s) {
        return jwtService.issueToken(s);
    }

    private long createPost(Student owner, String title) throws Exception {
        String res = mockMvc.perform(post("/api/collabs")
                        .header("Authorization", "Bearer " + token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POST_BODY.formatted(title)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("id").asLong();
    }

    @Test
    void feedRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/collabs")).andExpect(status().isUnauthorized());
    }

    @Test
    void createAppearsInFeedAsOwner() throws Exception {
        Student owner = save("feed-owner");
        long id = createPost(owner, "Need a designer");

        mockMvc.perform(get("/api/collabs").header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].relation", hasItem("OWNER")));
    }

    @Test
    void requestAcceptAndNotifyFlow() throws Exception {
        Student owner = save("ra-owner");
        Student applicant = save("ra-applicant");
        long postId = createPost(owner, "Hackathon team");

        String reqRes = mockMvc.perform(post("/api/collabs/" + postId + "/requests")
                        .header("Authorization", "Bearer " + token(applicant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long requestId = objectMapper.readTree(reqRes).get("id").asLong();

        // Owner accepts.
        mockMvc.perform(post("/api/collabs/requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Applicant is notified ("you're in").
        mockMvc.perform(get("/api/collabs/notifications")
                        .header("Authorization", "Bearer " + token(applicant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].postId", hasItem((int) postId)));

        // Marking seen clears the notification.
        mockMvc.perform(post("/api/collabs/notifications/seen")
                        .header("Authorization", "Bearer " + token(applicant)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/collabs/notifications")
                        .header("Authorization", "Bearer " + token(applicant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.postId == " + postId + ")]").isEmpty());
    }

    @Test
    void cannotRequestOwnPost() throws Exception {
        Student owner = save("own-req");
        long postId = createPost(owner, "My own");

        mockMvc.perform(post("/api/collabs/" + postId + "/requests")
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void closedPostRejectsRequests() throws Exception {
        Student owner = save("close-owner");
        Student applicant = save("close-applicant");
        long postId = createPost(owner, "Closing soon");

        mockMvc.perform(post("/api/collabs/" + postId + "/close")
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(post("/api/collabs/" + postId + "/requests")
                        .header("Authorization", "Bearer " + token(applicant)))
                .andExpect(status().isBadRequest());
    }
}
