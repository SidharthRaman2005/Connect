package com.connect.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.ChatConversationDto;
import com.connect.dto.ChatMessageDto;
import com.connect.dto.PageResponse;
import com.connect.dto.StudentBrief;
import com.connect.entity.ChatMessage;
import com.connect.entity.Connection;
import com.connect.entity.ConnectionStatus;
import com.connect.entity.Student;
import com.connect.exception.BadRequestException;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.ChatMessageRepository;
import com.connect.repository.ConnectionRepository;
import com.connect.repository.StudentRepository;

/** Direct messaging, restricted to students with an accepted connection. */
@Service
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ConnectionRepository connectionRepository;
    private final StudentRepository studentRepository;

    public ChatService(ChatMessageRepository messageRepository,
                       ConnectionRepository connectionRepository,
                       StudentRepository studentRepository) {
        this.messageRepository = messageRepository;
        this.connectionRepository = connectionRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public ChatMessageDto send(Long senderId, Long recipientId, String rawContent) {
        if (recipientId == null || senderId.equals(recipientId)) {
            throw new BadRequestException("Choose another student to message.");
        }
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty() || content.length() > 2000) {
            throw new BadRequestException("Messages must contain 1 to 2000 characters.");
        }
        Student sender = load(senderId);
        Student recipient = load(recipientId);
        requireConnected(senderId, recipientId);
        return ChatMessageDto.from(messageRepository.save(new ChatMessage(sender, recipient, content)));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatMessageDto> history(Long meId, Long otherId, int page, int size) {
        requireConnected(meId, otherId);
        Page<ChatMessage> result = messageRepository.findConversation(meId, otherId,
                PageRequest.of(page, size));
        List<ChatMessageDto> chronological = new ArrayList<>(result.getContent().stream()
                .map(ChatMessageDto::from).toList());
        chronological.sort(Comparator.comparing(ChatMessageDto::sentAt).thenComparing(ChatMessageDto::id));
        return PageResponse.of(chronological, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public void markRead(Long meId, Long otherId) {
        requireConnected(meId, otherId);
        messageRepository.markConversationRead(otherId, meId, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<ChatConversationDto> conversations(Long meId) {
        List<ChatConversationDto> conversations = new ArrayList<>();
        for (Connection connection : connectionRepository.findByStudentAndStatus(meId, ConnectionStatus.ACCEPTED)) {
            Student other = connection.getRequester().getId().equals(meId)
                    ? connection.getAddressee() : connection.getRequester();
            List<ChatMessage> latest = messageRepository.findLatest(meId, other.getId(), PageRequest.of(0, 1));
            ChatMessage last = latest.isEmpty() ? null : latest.get(0);
            long unread = messageRepository.countBySenderIdAndRecipientIdAndReadAtIsNull(other.getId(), meId);
            conversations.add(new ChatConversationDto(StudentBrief.from(other),
                    last == null ? null : last.getContent(), last == null ? null : last.getSentAt(), unread));
        }
        conversations.sort(Comparator.comparing(ChatConversationDto::lastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return conversations;
    }

    private void requireConnected(Long firstId, Long secondId) {
        if (firstId.equals(secondId)) throw new BadRequestException("You cannot chat with yourself.");
        boolean connected = connectionRepository.findBetween(firstId, secondId).stream()
                .anyMatch(connection -> connection.getStatus() == ConnectionStatus.ACCEPTED);
        if (!connected) throw new ForbiddenException("Chat is available only between connected students.");
    }

    private Student load(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
