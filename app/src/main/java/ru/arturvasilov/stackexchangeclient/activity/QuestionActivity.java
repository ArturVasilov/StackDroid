package ru.arturvasilov.stackexchangeclient.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import java.util.List;

import ru.arturvasilov.sqlite.core.SQLite;
import ru.arturvasilov.sqlite.core.Where;
import ru.arturvasilov.stackexchangeclient.R;
import ru.arturvasilov.stackexchangeclient.adapter.QuestionAdapter;
import ru.arturvasilov.stackexchangeclient.app.analytics.Analytics;
import ru.arturvasilov.stackexchangeclient.app.analytics.EventKeys;
import ru.arturvasilov.stackexchangeclient.app.analytics.EventTags;
import ru.arturvasilov.stackexchangeclient.data.database.QuestionTable;
import ru.arturvasilov.stackexchangeclient.dialog.LoadingDialog;
import ru.arturvasilov.stackexchangeclient.model.content.Answer;
import ru.arturvasilov.stackexchangeclient.model.content.Question;
import ru.arturvasilov.stackexchangeclient.presenter.QuestionPresenter;
import ru.arturvasilov.stackexchangeclient.rx.RxError;
import ru.arturvasilov.stackexchangeclient.utils.TextUtils;
import ru.arturvasilov.stackexchangeclient.utils.Views;
import ru.arturvasilov.stackexchangeclient.view.QuestionView;

/**
 * @author Artur Vasilov
 */
public class QuestionActivity extends AppCompatActivity implements QuestionView {

    private static final String QUESTION_KEY = "question";

    private QuestionAdapter mAdapter;

    private QuestionPresenter mPresenter;

    public static void start(@NonNull Activity activity, @NonNull Question question) {
        Intent intent = new Intent(activity, QuestionActivity.class);
        intent.putExtra(QUESTION_KEY, question);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_question);
        setSupportActionBar(Views.findById(this, R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        RecyclerView recyclerView = Views.findById(this, R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new QuestionAdapter();
        recyclerView.setAdapter(mAdapter);

        Question question = obtainQuestionFromIntent(getIntent());
        if (question == null) {
            finish();
            return;
        }

        Analytics.buildEvent()
                .putString(EventKeys.QUESTION_ID, String.valueOf(question.getQuestionId()))
                .log(EventTags.SCREEN_QUESTION);
        mPresenter = new QuestionPresenter(this, getLoaderManager(), this,
                LoadingDialog.view(getSupportFragmentManager()),
                RxError.view(this, getSupportFragmentManager()),
                question);
        mPresenter.init(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPresenter.onSaveInstanceState(outState);
    }

    @Nullable
    private Question obtainQuestionFromIntent(@NonNull Intent intent) {
        if (intent.hasExtra(QUESTION_KEY)) {
            return (Question) getIntent().getSerializableExtra(QUESTION_KEY);
        }

        String data = intent.getDataString();
        String queryFormat = "/questions";
        if (TextUtils.equals(Intent.ACTION_VIEW, intent.getAction())
                && data != null && data.contains(queryFormat)) {
            int questionId = Integer.parseInt(data.substring(data.lastIndexOf(queryFormat) + queryFormat.length() + 1));
            return SQLite.get().querySingle(
                    QuestionTable.TABLE,
                    Where.create().equalTo(QuestionTable.QUESTION_ID, questionId)
            );
        }
        return null;
    }

    @Override
    public void showQuestion(@NonNull Question question) {
        mAdapter.setQuestion(question);
    }

    @Override
    public void showAnswers(@NonNull List<Answer> answers) {
        mAdapter.changeDataSet(answers);
    }
}
