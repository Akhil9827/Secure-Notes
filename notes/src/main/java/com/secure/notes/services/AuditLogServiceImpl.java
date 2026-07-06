package com.secure.notes.services;

import com.secure.notes.exception.ResourceNotFoundException;
import com.secure.notes.model.AuditLog;
import com.secure.notes.model.Note;
import com.secure.notes.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService{

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public void logNoteCreation(String username, Note note){
        AuditLog log=new AuditLog();
        log.setAction("CREATE");
        log.setUsername(username);
        log.setNoteId(note.getId());
        log.setNoteContent(note.getContent());
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);

    }

    @Override
    public void logNoteUpdate(String username, Note note){
        AuditLog log=new AuditLog();
        log.setAction("UPDATE");
        log.setUsername(username);
        log.setNoteId(note.getId());
        log.setNoteContent(note.getContent());
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);

    }

    @Override
    public void logNoteDeletion(String username, Long noteId){
        AuditLog log=new AuditLog();
        log.setAction("DELETE");
        log.setUsername(username);
        log.setNoteId(noteId);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public List<AuditLog> getAuditLogsForNoteId(Long id) {
        return auditLogRepository.findByNoteId(id);
    }
}
