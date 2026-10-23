package com.connect.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.connect.entity.Student;
import com.connect.repository.StudentRepository;
import com.connect.service.JwtService;

/**
 * Verifies the security chain end to end: public endpoints stay open, protected
 * endpoints require a valid JWT, and a valid JWT authenticates the request.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtService jwtService;

    private Student savedStudent() {
        Student s = new Student();
        s.setGoogleId("sec-test-sub");
        s.setEmail("sec@vitapstudent.ac.in");
        s.setName("Sec Test");
        return studentRepository.save(s);
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointAcceptsValidToken() throws Exception {
        Student student = savedStudent();
        String token = jwtService.issueToken(student);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sec@vitapstudent.ac.in"))
                .andExpect(jsonPath("$.id").value(student.getId()));
    }

    @Test
    void protectedEndpointRejectsGarbageToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleSignInEndpointIsPublic() throws Exception {
        // Reaches validation (400) rather than being blocked by security (401).
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
