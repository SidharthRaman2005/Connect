package com.connect.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.entity.Tag;
import com.connect.entity.TagCategory;
import com.connect.entity.TagStatus;
import com.connect.repository.StudentRepository;
import com.connect.repository.TagRepository;
import com.connect.service.JwtService;

/**
 * End-to-end tests for the M3.1 discovery/search endpoints. Dummy seeding is off
 * so the only candidates are the students each test creates.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.dev.seed-dummies=false")
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private Student save(String key, String branch, ProfileVisibility visibility, List<Tag> tags) {
        Student s = new Student();
        s.setGoogleId("disc-" + key);
        s.setEmail(key + "@vitapstudent.ac.in");
        s.setName("Student " + key);
        s.setBranch(branch);
        s.setBatch(26);
        s.setProfileVisibility(visibility);
        s.setProfileCompleted(true);
        s.getTags().addAll(tags);
        return studentRepository.save(s);
    }

    private List<Tag> skills() {
        return tagRepository.findByCategoryAndStatusOrderByNameAsc(TagCategory.SKILL, TagStatus.APPROVED);
    }

    private List<Long> idsIn(String jsonArray) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (JsonNode node : objectMapper.readTree(jsonArray)) {
            ids.add(node.get("id").asLong());
        }
        return ids;
    }

    @Test
    void discoverRanksBySharedTagsAndExcludesPrivateAndSelf() throws Exception {
        List<Tag> s = skills();
        Tag t1 = s.get(0);
        Tag t2 = s.get(1);
        Tag t3 = s.get(2);

        Student me = save("me", "ME-BR", ProfileVisibility.PUBLIC, List.of(t1, t2));
        Student high = save("high", "HIGH-BR", ProfileVisibility.PUBLIC, List.of(t1, t2));
        Student low = save("low", "LOW-BR", ProfileVisibility.PUBLIC, List.of(t3));
        Student priv = save("priv", "PRIV-BR", ProfileVisibility.PRIVATE, List.of(t1, t2));

        String body = mockMvc.perform(get("/api/discover")
                        .header("Authorization", "Bearer " + jwtService.issueToken(me)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Long> ids = idsIn(body);
        assertThat(ids).contains(high.getId(), low.getId());
        assertThat(ids).doesNotContain(me.getId(), priv.getId());
        // Higher shared-tag count ranks first.
        assertThat(ids.indexOf(high.getId())).isLessThan(ids.indexOf(low.getId()));
    }

    @Test
    void searchFiltersByBranch() throws Exception {
        List<Tag> s = skills();
        Student me = save("sb-me", "SB-ME", ProfileVisibility.PUBLIC, List.of(s.get(0)));
        Student target = save("sb-target", "UNIQUEBR", ProfileVisibility.PUBLIC, List.of(s.get(0)));

        mockMvc.perform(get("/api/students/search").param("branch", "UNIQUEBR")
                        .header("Authorization", "Bearer " + jwtService.issueToken(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(target.getId()));
    }

    @Test
    void searchFiltersByTag() throws Exception {
        List<Tag> s = skills();
        Tag tagA = s.get(0);
        Tag tagB = s.get(1);
        Student me = save("st-me", "ST-ME", ProfileVisibility.PUBLIC, List.of(tagA));
        Student hasB = save("st-hasB", "ST-HASB", ProfileVisibility.PUBLIC, List.of(tagB));
        Student hasAOnly = save("st-hasA", "ST-HASA", ProfileVisibility.PUBLIC, List.of(tagA));

        mockMvc.perform(get("/api/students/search").param("tagIds", String.valueOf(tagB.getId()))
                        .header("Authorization", "Bearer " + jwtService.issueToken(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(hasB.getId().intValue())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(hasAOnly.getId().intValue()))));
    }

    @Test
    void discoverRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/discover")).andExpect(status().isUnauthorized());
    }
}
