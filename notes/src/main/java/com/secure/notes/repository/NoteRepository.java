package com.secure.notes.repository;

import com.secure.notes.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note,Long> {
    List<Note> findByOwnerUsername(String ownerUsername);
}
