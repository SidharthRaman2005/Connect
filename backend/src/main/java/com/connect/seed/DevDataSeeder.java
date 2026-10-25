package com.connect.seed;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.entity.Tag;
import com.connect.entity.TagStatus;
import com.connect.repository.StudentRepository;
import com.connect.repository.TagRepository;

/**
 * Seeds a set of dummy students for development so discovery, search, and
 * connections can be exercised without real accounts. Enabled by default; turn
 * off with {@code app.dev.seed-dummies=false}.
 *
 * <p>Dummy accounts use googleId {@code dummy-N} and cannot sign in (no Google
 * identity). Idempotent: skips seeding if the first dummy already exists.
 */
@Component
@Order(2) // after TagSeeder
@ConditionalOnProperty(name = "app.dev.seed-dummies", havingValue = "true", matchIfMissing = true)
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    /** A dummy student spec: name, branch, admission batch, bio, and tag names. */
    private record Dummy(String name, String branch, int batch, String bio, List<String> tags) {}

    private static final List<Dummy> DUMMIES = List.of(
            new Dummy("Aarav Reddy", "CSE", 26, "Full-stack dev who loves a good hackathon.",
                    List.of("Java", "React", "Web Development", "Competitive Programming", "Coding Club")),
            new Dummy("Diya Nair", "CSE", 25, "ML enthusiast and weekend photographer.",
                    List.of("Python", "Machine Learning", "Artificial Intelligence", "Photography", "Photography Club")),
            new Dummy("Kabir Sharma", "ECE", 26, "Robotics, circuits, and the occasional guitar riff.",
                    List.of("C++", "Robotics", "Internet of Things", "Guitar", "Music", "Robotics Club")),
            new Dummy("Ananya Rao", "MEC", 24, "Into design, dance, and building things that move.",
                    List.of("UI/UX Design", "Dance", "Drama & Theatre", "Dance Club", "Product Design")),
            new Dummy("Vihaan Gupta", "CSE", 27, "Backend + cloud. Also captain of the football team.",
                    List.of("Java", "Spring Boot", "Cloud Computing", "Football", "Sports", "Sports Club")),
            new Dummy("Ishita Menon", "BBA", 25, "Startups, finance, and public speaking.",
                    List.of("Public Speaking", "Entrepreneurship", "Startups", "Finance & Investing", "Entrepreneurship Cell")),
            new Dummy("Arjun Verma", "CSE", 26, "Competitive programmer. Chess on the side.",
                    List.of("C++", "Competitive Programming", "Data Science", "Chess", "Coding Club")),
            new Dummy("Sara Thomas", "ECE", 25, "Singer, painter, and signal-processing nerd.",
                    List.of("Singing", "Painting", "Music", "Painting & Art", "Music Club")),
            new Dummy("Rohan Iyer", "CIV", 24, "Photography, travel, and sustainable design.",
                    List.of("Photography", "Travel", "Environment & Sustainability", "Film & Cinema", "Film Club")),
            new Dummy("Meera Krishnan", "CSE", 27, "Android dev who volunteers on weekends.",
                    List.of("Android", "Flutter", "App Development", "Volunteering", "NSS")),
            new Dummy("Aditya Pillai", "MEC", 26, "Gym, basketball, and a bit of game dev.",
                    List.of("Fitness & Gym", "Basketball", "Game Development", "Sports", "Fitness Club")),
            new Dummy("Zara Khan", "BBA", 25, "Content, design, and debate.",
                    List.of("Content Writing", "Graphic Design", "Reading", "Debate Society", "Literary Club")));

    private final StudentRepository studentRepository;
    private final TagRepository tagRepository;

    public DevDataSeeder(StudentRepository studentRepository, TagRepository tagRepository) {
        this.studentRepository = studentRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (studentRepository.findByGoogleId("dummy-1").isPresent()) {
            return; // already seeded
        }

        // Approved tags keyed by lowercased name (names are effectively unique
        // across the catalog for the ones referenced here).
        Map<String, Tag> byName = tagRepository
                .findByStatusOrderByCategoryAscNameAsc(TagStatus.APPROVED).stream()
                .collect(Collectors.toMap(
                        t -> t.getName().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (a, b) -> a));

        int n = 0;
        for (Dummy d : DUMMIES) {
            n++;
            Student s = new Student();
            s.setGoogleId("dummy-" + n);
            s.setEmail("dummy" + n + "@vitapstudent.ac.in");
            s.setName(d.name());
            s.setBranch(d.branch());
            s.setBatch(d.batch());
            s.setBio(d.bio());
            s.setProfileVisibility(ProfileVisibility.PUBLIC);
            s.setProfileCompleted(true);
            for (String tagName : d.tags()) {
                Tag tag = byName.get(tagName.toLowerCase(Locale.ROOT));
                if (tag != null) {
                    s.getTags().add(tag);
                }
            }
            studentRepository.save(s);
        }
        log.info("Dev data seeded: {} dummy students inserted.", n);
    }
}
