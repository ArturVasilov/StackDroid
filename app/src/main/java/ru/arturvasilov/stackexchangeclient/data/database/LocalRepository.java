package ru.arturvasilov.stackexchangeclient.data.database;

import android.support.annotation.NonNull;

import java.util.List;

import ru.arturvasilov.sqlite.core.SQLite;
import ru.arturvasilov.sqlite.core.Where;
import ru.arturvasilov.sqlite.rx.RxSQLite;
import ru.arturvasilov.stackexchangeclient.api.RepositoryProvider;
import ru.arturvasilov.stackexchangeclient.app.analytics.Analytics;
import ru.arturvasilov.stackexchangeclient.app.analytics.EventKeys;
import ru.arturvasilov.stackexchangeclient.app.analytics.EventTags;
import ru.arturvasilov.stackexchangeclient.model.content.Question;
import ru.arturvasilov.stackexchangeclient.model.content.Tag;
import ru.arturvasilov.stackexchangeclient.model.content.User;
import ru.arturvasilov.stackexchangeclient.rx.RxSchedulers;
import ru.arturvasilov.stackexchangeclient.rx.StubAction;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * @author Artur Vasilov
 */
public class LocalRepository {

    @NonNull
    public Observable<User> getCurrentUser() {
        return RepositoryProvider.provideKeyValueStorage().getCurrentUserId()
                .take(1)
                .filter(id -> id > 0)
                .map(String::valueOf)
                .flatMap(id -> RxSQLite.get().querySingle(UserTable.TABLE, Where.create().equalTo(UserTable.USER_ID, id)))
                .compose(RxSchedulers.async());
    }

    @NonNull
    public Observable<List<Question>> questions(@NonNull String tag) {
        return RxSQLite.get().query(QuestionTable.TABLE, Where.create().equalTo(QuestionTable.TAG, tag))
                .compose(RxSchedulers.async());
    }

    @NonNull
    public Observable<List<String>> tags() {
        return RxSQLite.get().query(TagTable.TABLE, Where.create()).compose(RxSchedulers.async());
    }

    public boolean updateTag(@NonNull Tag tag) {
        if (tag.isFavourite()) {
            SQLite.get().delete(TagTable.TABLE, Where.create().equalTo(TagTable.TAG, tag.getName()));
            Analytics.buildEvent()
                    .putString(EventKeys.TAG, tag.getName())
                    .log(EventTags.TAGS_REMOVE_FAVOURITE);
            return false;
        } else {
            SQLite.get().insert(TagTable.TABLE, tag.getName());
            Analytics.buildEvent()
                    .putString(EventKeys.TAG, tag.getName())
                    .log(EventTags.TAGS_ADD_FAVOURITE);
            return true;
        }
    }

    public void logout() {
        Observable.just(true)
                .flatMap(value -> {
                    SQLite.get().delete(UserTable.TABLE);
                    SQLite.get().delete(QuestionTable.TABLE);
                    SQLite.get().delete(TagTable.TABLE);
                    SQLite.get().delete(AnswerTable.TABLE);
                    return Observable.just(value);
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new StubAction<>(), new StubAction<>());
    }
}
