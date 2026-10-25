package com.connect.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.repository.StudentRepository;
import com.connect.service.JwtService;

/**
 * End-to-end tests for the M2.1 profile endpoints: own-profile read/update and
 * visibility-aware viewing of other students' profiles, through the real
 * security chain (JWT auth) and persistence (H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtService jwtService;

    /**
     * Persists a student. The {@code key} is used to keep the Google id and
     * email unique across tests, since the shared context is not rolled back
     * between methods.
     */
    private Student save(String key, ProfileVisibility visibility) {
        Student s = new Student();
        s.setGoogleId("profile-" + key);
        s.setEmail(key + "@vitapstudent.ac.in");
        s.setName("Student " + key);
        s.setBranch("CSE");
        s.setProfileVisibility(visibility);
        return studentRepository.save(s);
    }

    private String tokenFor(Student student) {
        return jwtService.issueToken(student);
    }

    @Test
    void getMyProfileReturnsFullOwnProfile() throws Exception {
        Student me = save("me-full", ProfileVisibility.PRIVATE);

        mockMvc.perform(get("/api/profile/me").header("Authorization", "Bearer " + tokenFor(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(me.getId()))
                .andExpect(jsonPath("$.email").value("me-full@vitapstudent.ac.in"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }

    @Test
    void getMyProfileRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMyProfileAppliesChangesAndCompletesOnboarding() throws Exception {
        Student me = save("me-update", ProfileVisibility.PUBLIC);

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + tokenFor(me))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branch\":\"ECE\",\"bio\":\"hello world\",\"visibility\":\"PRIVATE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branch").value("ECE"))
                .andExpect(jsonPath("$.bio").value("hello world"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.profileCompleted").value(true));

        Student reloaded = studentRepository.findById(me.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.isProfileCompleted()).isTrue();
        org.assertj.core.api.Assertions.assertThat(reloaded.getBranch()).isEqualTo("ECE");
    }

    @Test
    void updateMyProfileRejectsBlankBranch() throws Exception {
        Student me = save("me-blank", ProfileVisibility.PUBLIC);

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + tokenFor(me))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branch\":\"\",\"bio\":\"x\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getPublicProfileOmitsEmail() throws Exception {
        Student viewer = save("viewer-pub", ProfileVisibility.PUBLIC);
        Student target = save("target-pub", ProfileVisibility.PUBLIC);

        mockMvc.perform(get("/api/profile/" + target.getId())
                        .header("Authorization", "Bearer " + tokenFor(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId()))
                .andExpect(jsonPath("$.name").value("Student target-pub"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.visibility").doesNotExist());
    }

    @Test
    void getPrivateProfileIsForbiddenForOtherStudents() throws Exception {
        Student viewer = save("viewer-priv", ProfileVisibility.PUBLIC);
        Student target = save("target-priv", ProfileVisibility.PRIVATE);

        mockMvc.perform(get("/api/profile/" + target.getId())
                        .header("Authorization", "Bearer " + tokenFor(viewer)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getPrivateProfileIsVisibleToItsOwner() throws Exception {
        Student owner = save("owner-priv", ProfileVisibility.PRIVATE);

        mockMvc.perform(get("/api/profile/" + owner.getId())
                        .header("Authorization", "Bearer " + tokenFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.getId()));
    }

    @Test
    void getMissingProfileReturnsNotFound() throws Exception {
        Student viewer = save("viewer-404", ProfileVisibility.PUBLIC);

        mockMvc.perform(get("/api/profile/99999999")
                        .header("Authorization", "Bearer " + tokenFor(viewer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
