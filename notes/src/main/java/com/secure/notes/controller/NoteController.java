package com.secure.notes.controller;

import com.secure.notes.model.Note;
import com.secure.notes.services.NoteService;
import org.aspectj.weaver.ast.Not;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @PostMapping
    public ResponseEntity<Note> createNote(@RequestBody String content,
                                           @AuthenticationPrincipal UserDetails userDetails) {  //
        String username = userDetails.getUsername();
        Note savedNote = noteService.createNoteForUser(username, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedNote);
    }

    @GetMapping()
    public ResponseEntity<List<Note>> getUserNotes(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        List<Note> allNotes = noteService.getNotesForUser(username);
        return ResponseEntity.status(HttpStatus.OK).body(allNotes);
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<Note> updateNote(@PathVariable Long noteId,
                                           @RequestBody String content,
                                           @AuthenticationPrincipal UserDetails userDetails){
        String userName=userDetails.getUsername();
        Note updatedNote=noteService.updateNoteForUser(noteId,content,userName);
        return ResponseEntity.status(HttpStatus.OK).body(updatedNote);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Note> deleteNote(@PathVariable Long noteId,@AuthenticationPrincipal UserDetails userDetails){
        String userName=userDetails.getUsername();
        Note deletedNote=noteService.deleteNoteForUser(noteId,userName);
        return ResponseEntity.status(HttpStatus.OK).body(deletedNote);
    }
}