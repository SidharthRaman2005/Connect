package com.connect.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.connect.dto.ReportUserRequest;
import com.connect.dto.StudentBrief;
import com.connect.dto.StudentReportDto;
import com.connect.entity.Student;
import com.connect.entity.StudentBlock;
import com.connect.entity.StudentReport;
import com.connect.exception.BadRequestException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.exception.ForbiddenException;
import com.connect.repository.ConnectionRepository;
import com.connect.repository.StudentBlockRepository;
import com.connect.repository.StudentReportRepository;
import com.connect.repository.StudentRepository;

@Service
public class UserSafetyService {
    private final StudentBlockRepository blockRepository;
    private final StudentReportRepository reportRepository;
    private final StudentRepository studentRepository;
    private final ConnectionRepository connectionRepository;

    public UserSafetyService(StudentBlockRepository blockRepository, StudentReportRepository reportRepository,
                             StudentRepository studentRepository, ConnectionRepository connectionRepository) {
        this.blockRepository = blockRepository; this.reportRepository = reportRepository;
        this.studentRepository = studentRepository; this.connectionRepository = connectionRepository;
    }

    public boolean isBlockedEitherWay(Long first, Long second) {
        return first != null && second != null && !first.equals(second)
                && blockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(first, second, second, first);
    }

    public void requireNotBlocked(Long first, Long second) {
        if (isBlockedEitherWay(first, second)) throw new ForbiddenException("This action is unavailable because one of you has blocked the other.");
    }

    @Transactional
    public void block(Long actorId, Long targetId) {
        if (actorId.equals(targetId)) throw new BadRequestException("You cannot block yourself.");
        Student actor = load(actorId); Student target = load(targetId);
        if (!blockRepository.existsByBlockerIdAndBlockedId(actorId, targetId)) {
            blockRepository.save(new StudentBlock(actor, target));
        }
        connectionRepository.deleteAll(connectionRepository.findBetween(actorId, targetId));
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

    @Transactional
    public StudentReportDto report(Long reporterId, Long targetId, ReportUserRequest request) {
        if (reporterId.equals(targetId)) throw new BadRequestException("You cannot report yourself.");
        StudentReport report = reportRepository.save(new StudentReport(load(reporterId), load(targetId),
                request.reason().trim(), request.details() == null ? null : request.details().trim()));
        return StudentReportDto.from(report);
    }

    @Transactional(readOnly = true)
    public List<StudentReportDto> myReports(Long reporterId) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId).stream().map(StudentReportDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Long> blockedIds(Long studentId) { return blockRepository.findBlockedEitherWay(studentId); }

    private Student load(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
