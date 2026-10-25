package com.connect.seed;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.connect.entity.Tag;
import com.connect.entity.TagCategory;
import com.connect.entity.TagStatus;
import com.connect.repository.TagRepository;

/**
 * Seeds the predefined, approved tag catalog on startup (M2.2).
 *
 * <p>Runs on every boot but is idempotent: a tag is inserted only if no tag of
 * the same category and name already exists, so restarts and manual edits are
 * safe. This stands in for a migration while the project uses
 * {@code ddl-auto=update}; it should move to a Flyway migration alongside the
 * schema when migrations are introduced.
 */
@Component
public class TagSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TagSeeder.class);

    /**
     * The curated catalog students choose from — a broad mix spanning tech,
     * the arts, sports, and campus life, not just one branch. Edit here to grow
     * it; the seeder only adds names that are missing, so appending is safe.
     */
    private static final Map<TagCategory, List<String>> CATALOG = Map.of(
            TagCategory.SKILL, List.of(
                    // Technical
                    "Java", "Python", "C", "C++", "JavaScript", "TypeScript",
                    "HTML/CSS", "React", "Angular", "Node.js", "Spring Boot",
                    "Flutter", "Android", "Machine Learning", "Data Science",
                    "SQL", "Cloud Computing", "DevOps", "Cybersecurity", "Git",
                    // Creative
                    "UI/UX Design", "Graphic Design", "Video Editing",
                    "Photography", "Photo Editing", "Content Writing",
                    "Public Speaking", "Singing", "Guitar", "Piano", "Dancing",
                    "Painting", "Sketching",
                    // Other
                    "Chess", "Cooking", "Leadership", "Project Management"),
            TagCategory.INTEREST, List.of(
                    // Tech
                    "Web Development", "App Development", "Artificial Intelligence",
                    "Competitive Programming", "Open Source", "Robotics",
                    "Internet of Things", "Blockchain", "Game Development",
                    "Data Analytics", "Product Design", "Research",
                    // Business
                    "Entrepreneurship", "Startups", "Finance & Investing",
                    // Arts & culture
                    "Music", "Dance", "Drama & Theatre", "Photography",
                    "Film & Cinema", "Painting & Art", "Creative Writing",
                    "Reading",
                    // Sports & fitness
                    "Sports", "Cricket", "Football", "Basketball", "Badminton",
                    "Table Tennis", "Fitness & Gym", "Yoga",
                    // Lifestyle
                    "Travel", "Gaming", "Cooking", "Volunteering",
                    "Environment & Sustainability", "Astronomy"),
            TagCategory.CLUB, List.of(
                    "Coding Club", "Google Developer Student Club", "IEEE",
                    "Robotics Club", "Entrepreneurship Cell", "Cultural Club",
                    "Music Club", "Dance Club", "Drama & Theatre Club",
                    "Photography Club", "Film Club", "Fine Arts Club",
                    "Literary Club", "Debate Society", "Quiz Club", "Sports Club",
                    "Fitness Club", "NSS", "NCC", "Social Service Club"));

    private final TagRepository tagRepository;

    public TagSeeder(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int inserted = 0;
        for (var entry : CATALOG.entrySet()) {
            TagCategory category = entry.getKey();
            for (String name : entry.getValue()) {
                if (!tagRepository.existsByCategoryAndNameIgnoreCase(category, name)) {
                    tagRepository.save(new Tag(category, name, TagStatus.APPROVED));
                    inserted++;
                }
            }
        }
        if (inserted > 0) {
            log.info("Tag catalog seeded: {} new approved tag(s) inserted.", inserted);
        }
    }
}
