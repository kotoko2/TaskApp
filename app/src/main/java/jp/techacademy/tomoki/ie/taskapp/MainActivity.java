package jp.techacademy.tomoki.ie.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;


public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_TASK = "jp.techacademy.tomoki.ie.taskapp.MainActivity";

    private Realm mRealm;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            reloadListView();
            reloadEditText();
        }

    };
    private ListView mListView;
    private TaskAdapter mTaskAdapter;

    private EditText mEditText; //追加
    private Button mSearch_Button;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                startActivity(intent);
            }
        });

        // Realmの設定
        mRealm = Realm.getDefaultInstance();
        mRealm.addChangeListener(mRealmListener);
        // ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        // EditTextの設定　追加
        mEditText = (EditText) findViewById(R.id.search_edit_text) ;

        // ListViewをタップしたときの処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task.getId());

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // タスクを削除する

                final Task task = (Task) parent.getAdapter().getItem(position);

                // ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id", task.getId()).findAll();

                        mRealm.beginTransaction();
                        results.deleteAllFromRealm();
                        mRealm.commitTransaction();
                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendingIntent);

                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        reloadListView();

        //EditTextに文字入力したときの処理
        mSearch_Button = (Button) findViewById(R.id.search_button);
        mEditText = (EditText) findViewById(R.id.search_edit_text);

        mSearch_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadListView();
            }
        });


    }

    private void reloadEditText() {
        String search = mEditText.getText().toString();
        //RealmQueryオブジェクトとしてのクエリを生成
        RealmQuery<Task> query = mRealm.where(Task.class);
        //categoryの値が検索欄に入力されてボタンが押された
        query.equalTo("category", search);
        //クエリはfindAll()メソッドが呼ばれた時点で実行される。
        RealmResults<Task> result1 = query.findAll();

    }
    private void reloadListView() {

        String search = mEditText.getText().toString();
        // Realm DBから全てのデータを日付順で取得,
        RealmResults<Task> mTaskRealmResults;

        if(search.length() > 0) {
            mTaskRealmResults = mRealm.where(Task.class).contains("category",search).findAllSorted("date", Sort.DESCENDING);
        } else {
            mTaskRealmResults = mRealm.where(Task.class).findAllSorted("date", Sort.DESCENDING);
        }

        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.setTaskList(mRealm.copyFromRealm(mTaskRealmResults));
        // TaskのListView用のアダプタに渡す
        mListView.setAdapter(mTaskAdapter);
        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }
}