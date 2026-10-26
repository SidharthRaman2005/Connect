package com.connect.seed;

import java.util.ArrayList;
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

import com.connect.entity.CollabPost;
import com.connect.entity.CollabType;
import com.connect.entity.Highlight;
import com.connect.entity.HighlightStatus;
import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.entity.Tag;
import com.connect.entity.TagStatus;
import com.connect.repository.CollabPostRepository;
import com.connect.repository.HighlightRepository;
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
    private final HighlightRepository highlightRepository;
    private final CollabPostRepository collabPostRepository;

    public DevDataSeeder(StudentRepository studentRepository,
                         TagRepository tagRepository,
                         HighlightRepository highlightRepository,
                         CollabPostRepository collabPostRepository) {
        this.studentRepository = studentRepository;
        this.tagRepository = tagRepository;
        this.highlightRepository = highlightRepository;
        this.collabPostRepository = collabPostRepository;
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

        List<Student> saved = new ArrayList<>();
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
            saved.add(studentRepository.save(s));
        }
        log.info("Dev data seeded: {} dummy students inserted.", n);

        seedExampleHighlights(saved);
        seedExampleCollabs(saved);
    }

    /** A few example highlights so the showcase has something to model. */
    private void seedExampleHighlights(List<Student> students) {
        highlight(students.get(1), "Published a paper on CNN pruning", HighlightStatus.COMPLETED,
                "Accepted at a student research symposium.", List.of("Machine Learning", "Research"),
                "https://example.com/paper");
        highlight(students.get(3), "National-level Bharatanatyam performance", HighlightStatus.COMPLETED,
                "Represented the university at the national youth festival.", List.of("Dance", "Culture"), null);
        highlight(students.get(4), "Captained the inter-college football team", HighlightStatus.COMPLETED,
                "Led the team to the regional finals.", List.of("Football", "Leadership"), null);
        highlight(students.get(10), "Runner-up, state badminton championship", HighlightStatus.COMPLETED,
                "Men's singles, state-level tournament.", List.of("Badminton", "Sports"), null);
    }

    private void highlight(Student owner, String title, HighlightStatus status,
                           String description, List<String> tags, String link) {
        Highlight h = new Highlight(owner);
        h.setTitle(title);
        h.setStatus(status);
        h.setDescription(description);
        h.setTags(new ArrayList<>(tags));
        h.setLink(link);
        highlightRepository.save(h);
    }

    /** A few open collab posts so the recent-posts feed has content. */
    private void seedExampleCollabs(List<Student> students) {
        collabPostRepository.save(new CollabPost(students.get(0),
                "Looking for a frontend dev for a campus events app",
                "Building a React app for club events; need someone for the UI.", CollabType.PROJECT));
        collabPostRepository.save(new CollabPost(students.get(5),
                "Team for the national fintech hackathon",
                "Forming a 4-person team; need backend + design.", CollabType.HACKATHON));
        collabPostRepository.save(new CollabPost(students.get(8),
                "Volunteers for a campus photo-walk",
                "Organising a weekend photo-walk and exhibition.", CollabType.EVENT));
    }
}
