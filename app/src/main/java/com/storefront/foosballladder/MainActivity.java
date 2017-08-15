package com.storefront.foosballladder;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MatchDatabase mMatchDatabase = new MatchDatabase(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final HashMap<String, Integer> teams = getTeams();

                final String[] teamNames = teams.keySet().toArray(new String[teams.size()]);

                final AlertDialog.Builder winningTeamSelect = new AlertDialog.Builder(MainActivity.this);
                winningTeamSelect.setTitle("Winning team")
                        .setItems(teamNames, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, final int winner) {
                                dialogInterface.dismiss();

                                AlertDialog.Builder losingTeamSelect = new AlertDialog.Builder(MainActivity.this);

                                List result = new LinkedList();

                                for (String item : teamNames) {
                                    if (teamNames[winner] != item) {
                                        result.add(item);
                                    }
                                }

                                final String losers[] = new String[result.size()];
                                result.toArray(losers);

                                losingTeamSelect.setTitle("Losing team")
                                        .setItems(losers, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, final int loser) {
                                                dialogInterface.dismiss();
                                                AlertDialog.Builder losingScoreSelect = new AlertDialog.Builder(MainActivity.this);
                                                final NumberPicker picker = new NumberPicker(MainActivity.this);
                                                picker.setMaxValue(9);
                                                picker.setMinValue(0);
                                                picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                                                losingScoreSelect.setTitle("Losing team score")
                                                        .setView(picker)
                                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, final int score) {
                                                                SQLiteDatabase db = mMatchDatabase.getWritableDatabase();

                                                                ContentValues insert = new ContentValues();
                                                                insert.put(DatabaseContract.MatchResult.COLUMN_NAME_WINNER_ID, teams.get(teamNames[winner]));
                                                                insert.put(DatabaseContract.MatchResult.COLUMN_NAME_LOSER_ID, teams.get(losers[loser]));
                                                                insert.put(DatabaseContract.MatchResult.COLUMN_NAME_LOSER_SCORE, picker.getValue());

                                                                db.insert(DatabaseContract.MatchResult.TABLE_NAME, null, insert);
                                                                db.close();

                                                                populateTable();
                                                            }
                                                        })
                                                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) { }
                                                        }).create().show();

                                            }
                                        })
                                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) { }
                                        }).create().show();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) { }
                        }).create().show();
            }
        });

        populateTable();
    }

    @Override
    protected void onDestroy() {
        mMatchDatabase.close();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_team) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final EditText editText = new EditText(this);
            builder.setTitle("Enter team name").setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SQLiteDatabase db = mMatchDatabase.getWritableDatabase();
                        Calendar calendar = Calendar.getInstance();

                        ContentValues insert = new ContentValues();
                        insert.put(DatabaseContract.Team.COLUMN_NAME_NAME, editText.getText().toString());
                        insert.put(DatabaseContract.Team.COLUMN_NAME_ACTIVE_MONTH, calendar.get(Calendar.MONTH));
                        insert.put(DatabaseContract.Team.COLUMN_NAME_ACTIVE_YEAR, calendar.get(Calendar.YEAR));

                        db.insert(DatabaseContract.Team.TABLE_NAME, null, insert);

                        populateTable();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }
                }).create().show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void populateTable() {
        String teamsQuery = "SELECT * FROM " + DatabaseContract.Team.TABLE_NAME + " " +
                "WHERE " + DatabaseContract.Team.TABLE_NAME + "." + DatabaseContract.Team.COLUMN_NAME_ACTIVE_MONTH + " = ? AND " +
                DatabaseContract.Team.TABLE_NAME + "." + DatabaseContract.Team.COLUMN_NAME_ACTIVE_YEAR + " = ?";

        Calendar calendar = Calendar.getInstance();
        String[] selectionArgs = { String.valueOf(calendar.get(Calendar.MONTH)), String.valueOf(calendar.get(Calendar.YEAR)) };

        SQLiteDatabase db = mMatchDatabase.getReadableDatabase();
        Cursor cursor = db.rawQuery(teamsQuery, selectionArgs);

        HashMap<Integer, Results> results = new HashMap<>();
        while (cursor.moveToNext()) {
            String teamName = cursor.getString(cursor.getColumnIndex(DatabaseContract.Team.COLUMN_NAME_NAME));
            if (teamName != null) {
                Results teamResults = new Results();
                teamResults.Name = teamName;
                results.put(cursor.getInt(cursor.getColumnIndex(DatabaseContract.Team._ID)), teamResults);
            }
        }

        if (results.size() == 0) return;

        String idArgs = "(" + TextUtils.join(",", results.keySet().toArray()) + ")";

        String resultsQuery = "SELECT * " +
                "FROM " + DatabaseContract.MatchResult.TABLE_NAME + " " +
                "WHERE " + DatabaseContract.MatchResult.COLUMN_NAME_WINNER_ID + " in " + idArgs;

        cursor = db.rawQuery(resultsQuery, null);

        while (cursor.moveToNext()) {
            int winnerId = cursor.getInt(cursor.getColumnIndex(DatabaseContract.MatchResult.COLUMN_NAME_WINNER_ID));
            int loserId = cursor.getInt(cursor.getColumnIndex(DatabaseContract.MatchResult.COLUMN_NAME_LOSER_ID));
            int loserGoals = cursor.getInt(cursor.getColumnIndex(DatabaseContract.MatchResult.COLUMN_NAME_LOSER_SCORE));

            results.get(winnerId).Wins++;
            results.get(winnerId).GoalsFor += 10;
            results.get(winnerId).GoalsAgainst += loserGoals;
            results.get(loserId).Losses++;
            results.get(loserId).GoalsFor += loserGoals;
            results.get(loserId).GoalsAgainst += 10;
        }

        cursor.close();
        db.close();

        TableLayout table = (TableLayout) findViewById(R.id.table);

        table.removeAllViews();

        List<Results> sorted = new ArrayList<>(results.values());
        Collections.sort(sorted);

        final TableRow headers = (TableRow) getLayoutInflater().inflate(R.layout.table_row, null);
        ((TextView) headers.findViewById(R.id.table_name)).setText("Name");
        ((TextView) headers.findViewById(R.id.table_wins)).setText("Wins");
        ((TextView) headers.findViewById(R.id.table_losses)).setText("Losses");
        ((TextView) headers.findViewById(R.id.table_win_percentage)).setText("%");
        ((TextView) headers.findViewById(R.id.table_goals_for)).setText("GF");
        ((TextView) headers.findViewById(R.id.table_goals_against)).setText("GA");
        ((TextView) headers.findViewById(R.id.table_name)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_wins)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_losses)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_win_percentage)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_goals_for)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_goals_against)).setTypeface(null, Typeface.BOLD);
        table.addView(headers);

        for (Results result : sorted) {
            final TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.table_row, null);
            ((TextView) row.findViewById(R.id.table_name)).setText(result.Name);
            ((TextView) row.findViewById(R.id.table_wins)).setText(String.valueOf(result.Wins));
            ((TextView) row.findViewById(R.id.table_losses)).setText(String.valueOf(result.Losses));
            ((TextView) row.findViewById(R.id.table_win_percentage)).setText(String.format("%.3f", result.getWinPercentage()));
            ((TextView) row.findViewById(R.id.table_goals_for)).setText(String.valueOf(result.GoalsFor));
            ((TextView) row.findViewById(R.id.table_goals_against)).setText(String.valueOf(result.GoalsAgainst));
            table.addView(row);
        }
    }

    private HashMap<String, Integer> getTeams() {
        String teamsQuery = "SELECT " + DatabaseContract.Team.COLUMN_NAME_NAME + ", " + DatabaseContract.Team._ID + " FROM " + DatabaseContract.Team.TABLE_NAME + " " +
                "WHERE " + DatabaseContract.Team.TABLE_NAME + "." + DatabaseContract.Team.COLUMN_NAME_ACTIVE_MONTH + " = ? AND " +
                DatabaseContract.Team.TABLE_NAME + "." + DatabaseContract.Team.COLUMN_NAME_ACTIVE_YEAR + " = ?";

        Calendar calendar = Calendar.getInstance();
        String[] selectionArgs = { String.valueOf(calendar.get(Calendar.MONTH)), String.valueOf(calendar.get(Calendar.YEAR)) };

        SQLiteDatabase db = mMatchDatabase.getReadableDatabase();
        Cursor cursor = db.rawQuery(teamsQuery, selectionArgs);

        HashMap<String, Integer> teams = new HashMap<>();
        while (cursor.moveToNext()) {
            String teamName = cursor.getString(cursor.getColumnIndex(DatabaseContract.Team.COLUMN_NAME_NAME));
            if (teamName != null) {
                teams.put(teamName, cursor.getInt(cursor.getColumnIndex(DatabaseContract.Team._ID)));
            }
        }

        cursor.close();
        return teams;
    }

    private class Results implements Comparable<Results> {
        public String Name;
        public int Wins = 0;
        public int Losses = 0;
        public int GoalsFor = 0;
        public int GoalsAgainst = 0;

        public double getWinPercentage() {
            return Wins + Losses == 0 ? 0.5 : (double)Wins / (Wins + Losses);
        }

        @Override
        public int compareTo(@NonNull Results results) {
            if (Double.compare(results.getWinPercentage(), this.getWinPercentage()) == 0) {
                if (Integer.compare(results.GoalsFor, this.GoalsFor) == 0) {
                    return Integer.compare(this.GoalsAgainst, results.GoalsAgainst);
                }

                return Integer.compare(results.GoalsFor, this.GoalsFor);
            }

            return Double.compare(results.getWinPercentage(), this.getWinPercentage());
        }
    }
}
