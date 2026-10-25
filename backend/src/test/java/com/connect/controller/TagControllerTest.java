package com.connect.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.connect.entity.Student;
import com.connect.entity.Tag;
import com.connect.entity.TagCategory;
import com.connect.entity.TagStatus;
import com.connect.repository.StudentRepository;
import com.connect.repository.TagRepository;
import com.connect.service.JwtService;

/**
 * End-to-end tests for the M2.2 tag catalog endpoint through the real security
 * chain (JWT auth) and persistence (H2). The approved catalog is populated by
 * {@link com.connect.seed.TagSeeder}, which runs when the context starts.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private JwtService jwtService;

    private String token(String key) {
        Student s = new Student();
        s.setGoogleId("tag-" + key);
        s.setEmail(key + "@vitapstudent.ac.in");
        s.setName("Student " + key);
        return jwtService.issueToken(studentRepository.save(s));
    }

    @Test
    void listTagsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listTagsReturnsSeededApprovedCatalog() throws Exception {
        mockMvc.perform(get("/api/tags").header("Authorization", "Bearer " + token("list-all")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Java")))
                .andExpect(jsonPath("$[*].name", hasItem("IEEE")));
    }

    @Test
    void listTagsFiltersByCategory() throws Exception {
        mockMvc.perform(get("/api/tags").param("category", "CLUB")
                        .header("Authorization", "Bearer " + token("list-clubs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", everyItem(org.hamcrest.Matchers.equalTo("CLUB"))))
                .andExpect(jsonPath("$[*].name", hasItem("IEEE")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Java"))));
    }

    @Test
    void listTagsExcludesNonApprovedTags() throws Exception {
        tagRepository.save(new Tag(TagCategory.SKILL, "Pending Only Skill", TagStatus.PENDING));

        mockMvc.perform(get("/api/tags").header("Authorization", "Bearer " + token("list-pending")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem("Pending Only Skill"))));
    }

    @Test
    void listTagsRejectsUnknownCategory() throws Exception {
        mockMvc.perform(get("/api/tags").param("category", "BOGUS")
                        .header("Authorization", "Bearer " + token("list-bad")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
