package ru.arturvasilov.stackexchangeclient.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;

import java.util.ArrayList;
import java.util.List;

import ru.arturvasilov.sqlite.core.SQLite;
import ru.arturvasilov.stackexchangeclient.BuildConfig;
import ru.arturvasilov.stackexchangeclient.data.database.QuestionTable;
import ru.arturvasilov.stackexchangeclient.model.content.Question;

/**
 * @author Artur Vasilov
 */
public class AppIndexingService extends IntentService {

    public AppIndexingService() {
        super("AppIndexingService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        List<Indexable> indexableNotes = new ArrayList<>();
        List<Question> questions = SQLite.get().query(QuestionTable.TABLE);

        for (Question question : questions) {
            Indexable noteToIndex = Indexables.noteDigitalDocumentBuilder()
                    .setName(question.getTitle())
                    .setText(question.getBody())
                    .setUrl(BuildConfig.QUESTIONS_ENDPOINT + question.getQuestionId())
                    .build();
            indexableNotes.add(noteToIndex);
        }

        if (indexableNotes.size() > 0) {
            Indexable[] notesArr = new Indexable[indexableNotes.size()];
            notesArr = indexableNotes.toArray(notesArr);

            // batch insert indexable notes into index
            FirebaseAppIndex.getInstance().update(notesArr);
        }
    }
}
