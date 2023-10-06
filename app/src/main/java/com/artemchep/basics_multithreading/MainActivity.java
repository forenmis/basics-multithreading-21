package com.artemchep.basics_multithreading;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artemchep.basics_multithreading.cipher.CipherUtil;
import com.artemchep.basics_multithreading.domain.Message;
import com.artemchep.basics_multithreading.domain.WithMillis;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<WithMillis<Message>> mList = new ArrayList<>();

    private MessageAdapter mAdapter = new MessageAdapter(mList);
    private LooperThread looperThread = new LooperThread();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        looperThread.start();

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        showWelcomeDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        looperThread.quit();
    }

    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
                .setMessage("What are you going to need for this task: Thread, Handler.\n" +
                        "\n" +
                        "1. The main thread should never be blocked.\n" +
                        "2. Messages should be processed sequentially.\n" +
                        "3. The elapsed time SHOULD include the time message spent in the queue.")
                .show();
    }

    public void onPushBtnClick(View view) {
        Message message = Message.generate();
        insert(new WithMillis<>(message));
    }

    @UiThread
    public void insert(final WithMillis<Message> message) {
        mList.add(message);
        mAdapter.notifyItemInserted(mList.size() - 1);
        looperThread.handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (MainActivity.this) {
                    if (looperThread.isQuit()) return;
                    long timestamp = System.currentTimeMillis();
                    final String cipherText = CipherUtil.encrypt(message.value.plainText);
                    long diff = System.currentTimeMillis() - timestamp;
                    final Message messageNew = message.value.copy(cipherText);
                    final WithMillis<Message> messageNewWithMillis = new WithMillis<>(messageNew, diff);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update(messageNewWithMillis);
                        }
                    });
                }
            }
        });
    }

    @UiThread
    public void update(final WithMillis<Message> message) {
        for (int i = 0; i < mList.size(); i++) {
            if (mList.get(i).value.key.equals(message.value.key)) {
                mList.set(i, message);
                mAdapter.notifyItemChanged(i);
                return;
            }
        }

        throw new IllegalStateException();
    }

    private static class LooperThread extends Thread {
        public Handler handler;
        private Looper looper;
        private boolean quit;

        @Override
        public void run() {
            Looper.prepare();
            looper = Looper.myLooper();
            handler = new Handler(looper);
            Looper.loop();
        }

        public void quit() {
            looper.quit();
            quit = true;
        }

        public boolean isQuit() {
            return quit;
        }
    }
}
