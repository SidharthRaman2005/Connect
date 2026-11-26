package com.connect.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.connect.dto.StudentBrief;
import com.connect.entity.Student;
import com.connect.entity.StudentBlock;
import com.connect.exception.BadRequestException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.ChatMessageRepository;
import com.connect.repository.StudentBlockRepository;
import com.connect.repository.StudentRepository;

@Service
public class UserSafetyService {
    private final StudentBlockRepository blockRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService notificationService;
    private final StudentRepository studentRepository;

    public UserSafetyService(StudentBlockRepository blockRepository, ChatMessageRepository chatMessageRepository,
                             NotificationService notificationService, StudentRepository studentRepository) {
        this.blockRepository = blockRepository; this.chatMessageRepository = chatMessageRepository;
        this.notificationService = notificationService;
        this.studentRepository = studentRepository;
    }

    /** True only when the first student has blocked the second. */
    public boolean hasBlocked(Long blockerId, Long targetId) {
        return blockerId != null && targetId != null && !blockerId.equals(targetId)
                && blockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId);
    }

    @Transactional
    public void block(Long actorId, Long targetId) {
        if (actorId.equals(targetId)) throw new BadRequestException("You cannot block yourself.");
        Student actor = load(actorId); Student target = load(targetId);
        if (!blockRepository.existsByBlockerIdAndBlockedId(actorId, targetId)) {
            blockRepository.save(new StudentBlock(actor, target));
        }
        chatMessageRepository.hidePendingForRecipient(targetId, actorId);
        notificationService.markMessageNotificationRead(actorId, targetId);
    }

    @Transactional
    public void unblock(Long actorId, Long targetId) {
        blockRepository.deleteByBlockerIdAndBlockedId(actorId, targetId);
    }

    @Transactional(readOnly = true)
    public List<StudentBrief> blocked(Long actorId) {
        return blockRepository.findByBlockerIdOrderByCreatedAtDesc(actorId).stream()
                .map(block -> StudentBrief.from(block.getBlocked())).toList();
    }

    @Transactional(readOnly = true)
    public List<Long> blockedByMeIds(Long studentId) { return blockRepository.findBlockedIdsByBlockerId(studentId); }

    private Student load(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
