package com.secure.notes.services;

import com.secure.notes.model.Note;

import java.util.List;

public interface NoteService {
    Note createNoteForUser(String username, String content);

    List<Note> getNotesForUser(String username);

    Note updateNoteForUser(Long noteId, String content, String userName);

    Note deleteNoteForUser(Long noteId, String userName);
}
