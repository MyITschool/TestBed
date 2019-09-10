package ru.samsung.itschool.book.testbed;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexFile;

public class MainActivity extends Activity {

    Handler hOut;
    Handler hIn;
    private RunUserProgram userProgram;
    private TextView consoleWrite;
    private EditText valuePrompt;
    private Button restartButton;
    private AndroidOutputStream out;
    private AndroidInputStream in;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        hOut = new PrintoutHandler();
        hIn = new ScanInHandler();
        out = new AndroidOutputStream(hOut);
        System.setOut(new PrintStream(out));
        in = new AndroidInputStream(hIn);
        System.setIn(in);

        getUserClassName();
        consoleWrite = (TextView) findViewById(R.id.consoleWrite);
        valuePrompt = (EditText) findViewById(R.id.valuePrompt);
        restartButton = (Button) findViewById(R.id.closeButton);

        valuePrompt.setVisibility(View.GONE);


        userProgram = new RunUserProgram();
        userProgram.execute();

        valuePrompt.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String value = valuePrompt.getText().toString();
                    in.addString(value);
                    valuePrompt.setText("");
                    valuePrompt.setVisibility(View.INVISIBLE);

                    return true;
                }
                return false;
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.restart) {
            restartUserProgram(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void restartUserProgram(View v) {
        userProgram.cancel(true);
        consoleWrite.setText("");
        valuePrompt.setVisibility(View.INVISIBLE);
        valuePrompt.setText("");
        valuePrompt.setEnabled(true);
        restartButton.setVisibility(View.GONE);
        Toast.makeText(this, R.string.program_restarted, Toast.LENGTH_SHORT).show();
        userProgram = new RunUserProgram();
        userProgram.execute();
    }

    public void stop(View v) {
        System.exit(0);
    }

    private String getUserClassName() {

        String packageCodePath = getPackageCodePath();
        DexFile df = null;
        try {
            df = new DexFile(packageCodePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return df.entries().nextElement();

    }

    private void showErrorDialogAndDie(String err) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.error);
        builder.setMessage(err);
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { // Кнопка ОК
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.this.finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class ScanInHandler extends Handler {
        public void handleMessage(Message msg) {
            valuePrompt.setVisibility(View.VISIBLE);
        }
    }

    private class PrintoutHandler extends Handler {
        public void handleMessage(Message msg) {
            // update TextView
            String readText = consoleWrite.getText().toString();
            consoleWrite.setText(readText + msg.obj);
        }
    }

    private class RunUserProgram extends AsyncTask<Void, Void, String> {
        String userClassName = "?";
        private Class UserClass;
        private Method main;


        @Override
        protected void onPreExecute() {
            try {
                userClassName = getUserClassName();
                UserClass = Class.forName(getUserClassName());
            } catch (ClassNotFoundException e) {
                MainActivity.this.showErrorDialogAndDie(getString(R.string.user_program_not_found));
            }
            MainActivity.this.setTitle("TestBed (" + userClassName + ".java)");
            try {
                main = UserClass.getDeclaredMethod("main", new String[0].getClass());
            } catch (NoSuchMethodException e) {
                MainActivity.this.showErrorDialogAndDie(getString(R.string.main_not_found));
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                // USER PROGRAM START
                main.invoke(null, new Object[]{new String[0]});
            } catch (Throwable error) {
                if (error instanceof InvocationTargetException)
                    error = ((InvocationTargetException) error).getTargetException();
                if (isCancelled()) return getString(R.string.program_restarted);
                String message = "\n" + getString(R.string.error) + " : " + error.toString();

                for (StackTraceElement e : error.getStackTrace()) {
                    String pos = e.toString();
                    String filter = userClassName;
                    if (pos.contains(filter)) {
                        message += "\n" + pos;
                    }
                }
                System.out.println(message);

                return getString(R.string.program_stopped);
            }
            return getString(R.string.program_finished);
        }

        protected void onPostExecute(String stopMessage) {
            valuePrompt.setVisibility(View.VISIBLE);
            valuePrompt.setText(stopMessage);
            valuePrompt.setEnabled(false);
            restartButton.setVisibility(View.VISIBLE);
        }
    }
}
