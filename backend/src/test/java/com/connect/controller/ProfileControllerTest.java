package com.connect.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
 * End-to-end tests for the M2.1 profile endpoints: own-profile read/update and
 * visibility-aware viewing of other students' profiles, through the real
 * security chain (JWT auth) and persistence (H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @TempDir
    static Path uploadsDir;

    @DynamicPropertySource
    static void uploadsProperty(DynamicPropertyRegistry registry) {
        registry.add("app.uploads.dir", () -> uploadsDir.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private JwtService jwtService;

    private Tag approvedTag(TagCategory category) {
        return tagRepository.findByCategoryAndStatusOrderByNameAsc(category, TagStatus.APPROVED)
                .get(0); // seeded by TagSeeder when the context starts
    }

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
    void updateMyTagsAttachesApprovedTagsAndReturnsThemOnProfile() throws Exception {
        Student me = save("me-tags", ProfileVisibility.PUBLIC);
        Tag skill = approvedTag(TagCategory.SKILL);
        Tag club = approvedTag(TagCategory.CLUB);
        String token = tokenFor(me);

        mockMvc.perform(put("/api/profile/me/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[" + skill.getId() + "," + club.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[*].name", hasItem(skill.getName())))
                .andExpect(jsonPath("$.tags[*].name", hasItem(club.getName())));

        // Persisted: GET /me reflects the attached tags.
        mockMvc.perform(get("/api/profile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[*].name", hasItem(skill.getName())));
    }

    @Test
    void updateMyTagsWithEmptyListClearsTags() throws Exception {
        Student me = save("me-tags-clear", ProfileVisibility.PUBLIC);
        Tag skill = approvedTag(TagCategory.SKILL);
        String token = tokenFor(me);

        mockMvc.perform(put("/api/profile/me/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[" + skill.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(1));

        mockMvc.perform(put("/api/profile/me/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(0));
    }

    @Test
    void updateMyTagsRejectsUnknownTagId() throws Exception {
        Student me = save("me-tags-bad", ProfileVisibility.PUBLIC);

        mockMvc.perform(put("/api/profile/me/tags")
                        .header("Authorization", "Bearer " + tokenFor(me))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[99999999]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateMyTagsRequiresAuth() throws Exception {
        mockMvc.perform(put("/api/profile/me/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadAndDeletePicture() throws Exception {
        Student me = save("me-pic", ProfileVisibility.PUBLIC);
        String token = tokenFor(me);
        MockMultipartFile image =
                new MockMultipartFile("file", "p.png", "image/png", new byte[] {1, 2, 3, 4});

        mockMvc.perform(multipart("/api/profile/me/picture").file(image)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pictureUrl", startsWith("/uploads/")));

        mockMvc.perform(delete("/api/profile/me/picture")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pictureUrl").value(nullValue()));
    }

    @Test
    void uploadPictureRejectsNonImage() throws Exception {
        Student me = save("me-pic-bad", ProfileVisibility.PUBLIC);
        MockMultipartFile notImage =
                new MockMultipartFile("file", "x.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/profile/me/picture").file(notImage)
                        .header("Authorization", "Bearer " + tokenFor(me)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void uploadPictureRequiresAuth() throws Exception {
        MockMultipartFile image =
                new MockMultipartFile("file", "p.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/profile/me/picture").file(image))
                .andExpect(status().isUnauthorized());
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
