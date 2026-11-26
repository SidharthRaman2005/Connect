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
import com.connect.dto.ChatPreferenceRequest;
import com.connect.dto.PageResponse;
import com.connect.dto.StudentBrief;
import com.connect.entity.ChatMessage;
import com.connect.entity.ChatPreference;
import com.connect.entity.Connection;
import com.connect.entity.ConnectionStatus;
import com.connect.entity.Student;
import com.connect.exception.BadRequestException;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.ChatMessageRepository;
import com.connect.repository.ChatPreferenceRepository;
import com.connect.repository.ConnectionRepository;
import com.connect.repository.StudentRepository;

/** Direct messaging, restricted to students with an accepted connection. */
@Service
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatPreferenceRepository preferenceRepository;
    private final ConnectionRepository connectionRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;
    private final UserSafetyService safetyService;

    public ChatService(ChatMessageRepository messageRepository,
                       ChatPreferenceRepository preferenceRepository,
                       ConnectionRepository connectionRepository,
                       StudentRepository studentRepository,
                       NotificationService notificationService,
                       UserSafetyService safetyService) {
        this.messageRepository = messageRepository;
        this.preferenceRepository = preferenceRepository;
        this.connectionRepository = connectionRepository;
        this.studentRepository = studentRepository;
        this.notificationService = notificationService;
        this.safetyService = safetyService;
    }

    @Transactional
    public ChatMessageDto send(Long senderId, Long recipientId, String rawContent, boolean recipientOnline) {
        if (recipientId == null || senderId.equals(recipientId)) {
            throw new BadRequestException("Choose another student to message.");
        }
        String content = clean(rawContent);
        Student sender = load(senderId);
        Student recipient = load(recipientId);
        requireConnected(senderId, recipientId);
        boolean recipientBlockedSender = safetyService.hasBlocked(recipientId, senderId);
        boolean deliverToRecipient = recipientOnline && !recipientBlockedSender;
        ChatMessage message = new ChatMessage(sender, recipient, content);
        if (recipientBlockedSender) message.hideFromRecipient();
        if (deliverToRecipient) message.markDelivered();
        ChatMessage saved = messageRepository.save(message);

        // Offline recipients get one plain notification. Online recipients only
        // get the unread dot in the chat navigation, driven by /chat/unread.
        if (!recipientBlockedSender && !recipientOnline) notificationService.recordMessage(recipient, sender);
        return ChatMessageDto.from(saved);
    }

    /** A blocked sender may submit a message, but it is never delivered to this recipient. */
    @Transactional(readOnly = true)
    public boolean canReceiveMessage(Long recipientId, Long senderId) {
        return !safetyService.hasBlocked(recipientId, senderId);
    }

    /** Marks pending messages as delivered when their recipient reconnects. */
    @Transactional
    public List<ChatMessageDto> markDeliveredOnConnect(Long studentId) {
        List<ChatMessage> pending = messageRepository
                .findByRecipientIdAndDeliveredAtIsNullAndReadAtIsNull(studentId);
        if (pending.isEmpty()) return List.of();
        return pending.stream().filter(message -> !message.isHiddenFromRecipient()
                && !safetyService.hasBlocked(studentId, message.getSender().getId())).map(message -> {
            message.markDelivered();
            return ChatMessageDto.from(messageRepository.save(message));
        }).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long studentId) {
        long total = messageRepository.countByRecipientIdAndReadAtIsNullAndHiddenFromRecipientFalse(studentId);
        List<Long> blocked = safetyService.blockedByMeIds(studentId);
        return blocked.isEmpty() ? total : Math.max(0, total - messageRepository.countByRecipientIdAndSenderIdInAndReadAtIsNullAndHiddenFromRecipientFalse(studentId, blocked));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatMessageDto> history(Long meId, Long otherId, int page, int size) {
        requireConnected(meId, otherId);
        Instant after = clearedAt(meId, otherId);
        Page<ChatMessage> result = after == null
                ? messageRepository.findConversation(meId, otherId, PageRequest.of(page, size))
                : messageRepository.findConversationAfter(meId, otherId, after, PageRequest.of(page, size));
        List<ChatMessageDto> chronological = new ArrayList<>(result.getContent().stream()
                .map(ChatMessageDto::from).toList());
        chronological.sort(Comparator.comparing(ChatMessageDto::sentAt).thenComparing(ChatMessageDto::id));
        return PageResponse.of(chronological, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    /** Edits one of your own messages. The update is broadcast by the controller. */
    @Transactional
    public ChatMessageDto editMessage(Long meId, Long messageId, String rawContent) {
        ChatMessage message = ownMessage(meId, messageId);
        requireConnected(meId, otherParty(message, meId));
        if (message.isDeleted()) throw new BadRequestException("This message was unsent and cannot be edited.");
        message.edit(clean(rawContent));
        return ChatMessageDto.from(messageRepository.save(message));
    }

    /** Unsends one of your own messages for both participants. */
    @Transactional
    public ChatMessageDto unsendMessage(Long meId, Long messageId) {
        ChatMessage message = ownMessage(meId, messageId);
        requireConnected(meId, otherParty(message, meId));
        if (message.isDeleted()) throw new BadRequestException("This message was already unsent.");
        message.unsend();
        return ChatMessageDto.from(messageRepository.save(message));
    }

    @Transactional
    public boolean markRead(Long meId, Long otherId) {
        requireConnected(meId, otherId);
        if (safetyService.hasBlocked(meId, otherId)) return false;
        Instant now = Instant.now();
        messageRepository.markConversationRead(otherId, meId, now);
        notificationService.markMessageNotificationRead(meId, otherId);
        return true;
    }

    @Transactional(readOnly = true)
    public List<ChatConversationDto> conversations(Long meId, boolean includeArchived) {
        List<ChatConversationDto> conversations = new ArrayList<>();
        for (Connection connection : connectionRepository.findByStudentAndStatus(meId, ConnectionStatus.ACCEPTED)) {
            Student other = connection.getRequester().getId().equals(meId)
                    ? connection.getAddressee() : connection.getRequester();
            ChatPreference preference = preferenceRepository.findByStudentIdAndOtherId(meId, other.getId()).orElse(null);
            if (preference != null && preference.isArchived() && !includeArchived) continue;
            boolean muted = preference != null && preference.isMuted();
            boolean archived = preference != null && preference.isArchived();

            Instant after = preference == null ? null : preference.getClearedAt();
            Page<ChatMessage> lastPage = after == null
                    ? messageRepository.findConversation(meId, other.getId(), PageRequest.of(0, 1))
                    : messageRepository.findConversationAfter(meId, other.getId(), after, PageRequest.of(0, 1));
            ChatMessage last = lastPage.hasContent() ? lastPage.getContent().get(0) : null;
            long unread = safetyService.hasBlocked(meId, other.getId()) ? 0 : after == null
                    ? messageRepository.countBySenderIdAndRecipientIdAndReadAtIsNullAndHiddenFromRecipientFalse(other.getId(), meId)
                    : messageRepository.countUnreadAfter(other.getId(), meId, after);

            conversations.add(new ChatConversationDto(
                    StudentBrief.from(other),
                    last == null || last.isDeleted() ? (last == null ? null : "Message unsent") : last.getContent(),
                    last == null ? null : last.getSentAt(),
                    unread,
                    muted,
                    archived));
        }
        conversations.sort(Comparator.comparing(ChatConversationDto::lastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return conversations;
    }

    /** Mutes or archives a conversation for the caller only. */
    @Transactional
    public void updatePreference(Long meId, Long otherId, ChatPreferenceRequest request) {
        load(otherId);
        ChatPreference preference = preferenceRepository.findByStudentIdAndOtherId(meId, otherId)
                .orElseGet(() -> new ChatPreference(meId, otherId));
        preference.apply(request.muted(), request.archived());
        preferenceRepository.save(preference);
    }

    /** Hides history and removes the conversation from the caller's inbox. */
    @Transactional
    public void clearConversation(Long meId, Long otherId) {
        load(otherId);
        ChatPreference preference = preferenceRepository.findByStudentIdAndOtherId(meId, otherId)
                .orElseGet(() -> new ChatPreference(meId, otherId));
        preference.clearHistory();
        preferenceRepository.save(preference);
    }

    private Instant clearedAt(Long meId, Long otherId) {
        return preferenceRepository.findByStudentIdAndOtherId(meId, otherId)
                .map(ChatPreference::getClearedAt).orElse(null);
    }

    private ChatMessage ownMessage(Long meId, Long messageId) {
        ChatMessage message = messageRepository.findByIdAndSenderId(messageId, meId)
                .orElseThrow(() -> new ForbiddenException("You can only modify your own messages."));
        return message;
    }

    private Long otherParty(ChatMessage message, Long meId) {
        return message.getSender().getId().equals(meId)
                ? message.getRecipient().getId() : message.getSender().getId();
    }

    private String clean(String raw) {
        String content = raw == null ? "" : raw.trim();
        if (content.isEmpty() || content.length() > 2000) {
            throw new BadRequestException("Messages must contain 1 to 2000 characters.");
        }
        return content;
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
