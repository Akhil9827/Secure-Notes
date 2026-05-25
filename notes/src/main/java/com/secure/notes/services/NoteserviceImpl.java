package com.secure.notes.services;

import com.secure.notes.exception.ResourceNotFoundException;
import com.secure.notes.model.Note;
import com.secure.notes.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteserviceImpl implements NoteService{

    @Autowired
    private NoteRepository noteRepository;


    @Override
    public Note createNoteForUser(String username, String content) {
        Note note=new Note();
        note.setContent(content);
        note.setOwnerUsername(username);
        Note savedNote=noteRepository.save(note);
        return savedNote;
    }

    @Override
    public List<Note> getNotesForUser(String username) {
        List<Note> personalNotes=noteRepository.findByOwnerUsername(username);
        return personalNotes;
    }

    @Override
    public Note updateNoteForUser(Long noteId, String content, String userName) {
        Note note=noteRepository.findById(noteId)
                .orElseThrow(()-> new ResourceNotFoundException("Note with noteId " + noteId + "not available"));
        note.setContent(content);
        Note updatedNote=noteRepository.save(note);
        return updatedNote;
    }

    @Override
    public Note deleteNoteForUser(Long noteId, String userName) {
        Note note=noteRepository.findById(noteId)
                .orElseThrow(()-> new ResourceNotFoundException("Note with NoteId " + noteId + "not available"));
         noteRepository.delete(note);
         return note;
    }
}
