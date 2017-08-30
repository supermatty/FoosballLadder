package com.storefront.foosballladder;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.storefront.foosballladder.models.TeamResult;
import com.storefront.foosballladder.models.TeamResultCollection;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@SuppressLint("UseSparseArrays")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final boolean USE_NUMBER_PICKER = false;
    private static final String LEAGUE_NAME = "LeagueName";
    private static final String FIREBASE_LEAGUE_ROOT_NODE = "League";

    private MatchDatabase mMatchDatabase = new MatchDatabase(this);
    private String mLeagueName;
    private DatabaseReference mFirebaseDatabase;
    private HashMap<Integer, TeamResult> mResults;
    private boolean mUseFireBaseForStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFirebaseDatabase = FirebaseDatabase.getInstance().getReference();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addGameScore();
            }
        });

        setupFirebase();
    }

    private void addGameScore() {
        final HashMap<String, Integer> teams = getTeams();

        final String[] teamNames = teams.keySet().toArray(new String[teams.size()]);

        final AlertDialog.Builder winningTeamSelect = new AlertDialog.Builder(MainActivity.this);
        winningTeamSelect.setTitle("Winning team")
                .setItems(teamNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, final int winner) {
                        dialogInterface.dismiss();

                        pickLosingTeam(teamNames[winner], teamNames, teams);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).create().show();
    }

    private void pickLosingTeam(final String winningTeamName, String[] teamNames, final HashMap<String, Integer> teams) {
        AlertDialog.Builder losingTeamSelect = new AlertDialog.Builder(MainActivity.this);

        List<String> result = new ArrayList<>();

        for (String item : teamNames) {
            if (!winningTeamName.equals(item)) {
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
                        pickLosingAmount(teams.get(losers[loser]), teams.get(winningTeamName));

                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).create().show();
    }

    private void pickLosingAmount(final Integer losingTeamId, final Integer winningTeamId) {
        AlertDialog.Builder losingScoreSelect = new AlertDialog.Builder(MainActivity.this);

        View picker;
        class LosingTeamScore {
            int score;
        }
        final LosingTeamScore losingTeamScore = new LosingTeamScore();

        if (USE_NUMBER_PICKER) {
            NumberPicker numberPicker = new NumberPicker(MainActivity.this);
            numberPicker.setMaxValue(9);
            numberPicker.setMinValue(0);
            numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
            numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {

                @Override
                public void onValueChange(NumberPicker numberPicker, int oldValue, int newValue) {
                    losingTeamScore.score = newValue;
                }
            });
            picker = numberPicker;
        } else {
            picker = getLayoutInflater().inflate(R.layout.number_picker, null);
            final ImageButton[] buttons = new ImageButton[10];
            for (int i = 0; i < 10; i++) {
                buttons[i] = (ImageButton) picker.findViewById(getResources().getIdentifier("button_number_" + i, "id", getPackageName()));
                buttons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Clear all buttons tint...
                        for (int j = 0; j < 10; j++) {
                            buttons[j].setColorFilter(null);
                        }

                        losingTeamScore.score = Integer.parseInt(view.getTag().toString());
                        // Set tint on selected button
                        ((ImageButton) view).setColorFilter(Color.argb(155, 155, 155, 155));
                    }
                });
            }
        }

        losingScoreSelect.setTitle("Losing team score")
                .setView(picker)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, final int score) {
                        if (mUseFireBaseForStorage) {
                            TeamResult winningTeamResults = mResults.get(winningTeamId);
                            winningTeamResults.addGoalsFor(10);
                            winningTeamResults.addGoalsAgainst(losingTeamScore.score);
                            winningTeamResults.addWin();

                            TeamResult losingTeamResults = mResults.get(losingTeamId);
                            losingTeamResults.addGoalsAgainst(10);
                            losingTeamResults.addGoalsFor(losingTeamScore.score);
                            losingTeamResults.addLoss();

                            updateFirebase();
                        } else {
                            addScoreToDatabase(winningTeamId, losingTeamId, losingTeamScore.score);
                            populateTableFromDatabase();
                        }

                        populateTable();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).create().show();
    }

    private void updateFirebase() {
        final DatabaseReference leagueNode = mFirebaseDatabase.child(FIREBASE_LEAGUE_ROOT_NODE);
        leagueNode.child(mLeagueName).child(getYearString()).child(getMonthString()).setValue(new ArrayList<>(mResults.values()));
    }

    private void addScoreToDatabase(Integer winningTeamId, Integer loadingTeamId, int loserScore) {
        SQLiteDatabase db = mMatchDatabase.getWritableDatabase();

        ContentValues insert = new ContentValues();
        insert.put(DatabaseContract.MatchResult.COLUMN_NAME_WINNER_ID, winningTeamId);
        insert.put(DatabaseContract.MatchResult.COLUMN_NAME_LOSER_ID, loadingTeamId);
        insert.put(DatabaseContract.MatchResult.COLUMN_NAME_LOSER_SCORE, loserScore);

        db.insert(DatabaseContract.MatchResult.TABLE_NAME, null, insert);
        db.close();
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

                            if (mUseFireBaseForStorage) {
                                int currentId = 0;
                                for (TeamResult teamResults : mResults.values()) {

                                    if (teamResults.getId() > currentId) {
                                        currentId = teamResults.getId();
                                    }
                                }

                                Integer nextTeamId = currentId + 1;
                                mResults.put(nextTeamId, new TeamResult(editText.getText().toString(), nextTeamId));

                                updateFirebase();
                            } else {

                                SQLiteDatabase db = mMatchDatabase.getWritableDatabase();
                                Calendar calendar = Calendar.getInstance();

                                ContentValues insert = new ContentValues();
                                insert.put(DatabaseContract.Team.COLUMN_NAME_NAME, editText.getText().toString());
                                insert.put(DatabaseContract.Team.COLUMN_NAME_ACTIVE_MONTH, calendar.get(Calendar.MONTH));
                                insert.put(DatabaseContract.Team.COLUMN_NAME_ACTIVE_YEAR, calendar.get(Calendar.YEAR));

                                db.insert(DatabaseContract.Team.TABLE_NAME, null, insert);

                                populateTableFromDatabase();
                            }

                            populateTable();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    }).create().show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupFirebase() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mLeagueName = preferences.getString(LEAGUE_NAME, null);
        if (mLeagueName == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Do you wish to synchronize your league with the cloud?")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            importIntoFirebaseStep1();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            useLocalDatabase();
                        }
                    })
                    .show();

        } else {
            loadOrImportFirebase();
        }
    }

    private void useLocalDatabase() {
        mUseFireBaseForStorage = false;
        populateTableFromDatabase();
        populateTable();
    }

    private void importIntoFirebaseStep1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        builder.setTitle("Enter League Name").setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        importIntoFirebaseStep2(editText.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        useLocalDatabase();
                    }
                })
                .show();
    }

    private void importIntoFirebaseStep2(final String leagueName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LEAGUE_NAME, leagueName);
        editor.apply();
        mLeagueName = leagueName;
        loadOrImportFirebase();
    }

    private void loadOrImportFirebase() {
        final DatabaseReference leagueNode = mFirebaseDatabase.child(FIREBASE_LEAGUE_ROOT_NODE);
        leagueNode.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DataSnapshot leagueSnapshot = dataSnapshot.child(mLeagueName);
                if (leagueSnapshot.exists()) {
                    DataSnapshot yearSnapshot = leagueSnapshot.child(getYearString());
                    if (yearSnapshot.exists()) {
                        mResults = new HashMap<>();
                        if (yearSnapshot.child(getMonthString()).exists()) {
                            TeamResultCollection teamResults = yearSnapshot.child(getMonthString()).getValue(TeamResultCollection.class);
                            for (TeamResult teamResult : teamResults) {
                                mResults.put(teamResult.getId(), teamResult);
                            }
                        }

                        populateTable();
                        return;
                    }
                }

                populateTableFromDatabase();
                importIntoFirebaseFromDatabase(leagueNode);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "On Cancelled Calling " + databaseError.getDetails() + "\n\n" + databaseError.getMessage() + "\n\n" + databaseError.toString());
            }
        });
    }

    private void importIntoFirebaseFromDatabase(DatabaseReference leagueNode) {
        mUseFireBaseForStorage = true;

        leagueNode.child(mLeagueName).child(getYearString()).child(getMonthString()).setValue(new TeamResultCollection(mResults.values()));

        populateTable();
    }

    private String getMonthString() {
        Calendar calendar = Calendar.getInstance();
        return Integer.toString(calendar.get(Calendar.MONTH));
    }

    private String getYearString() {
        Calendar calendar = Calendar.getInstance();
        return Integer.toString(calendar.get(Calendar.YEAR));
    }

    private void populateTableFromDatabase() {
        String teamsQuery = "SELECT * FROM " + DatabaseContract.Team.TABLE_NAME + " " +
                "WHERE " + DatabaseContract.Team.TABLE_NAME + "." + DatabaseContract.Team.COLUMN_NAME_ACTIVE_MONTH + " = ? AND " +
                DatabaseContract.Team.TABLE_NAME + "." + DatabaseContract.Team.COLUMN_NAME_ACTIVE_YEAR + " = ?";

        String[] selectionArgs = {getMonthString(), getYearString()};

        SQLiteDatabase db = mMatchDatabase.getReadableDatabase();
        Cursor cursor = db.rawQuery(teamsQuery, selectionArgs);

        mResults = new HashMap<>();
        while (cursor.moveToNext()) {
            String teamName = cursor.getString(cursor.getColumnIndex(DatabaseContract.Team.COLUMN_NAME_NAME));
            if (teamName != null) {
                int teamId = cursor.getInt(cursor.getColumnIndex(DatabaseContract.Team._ID));
                TeamResult teamResults = new TeamResult(teamName, teamId);
                mResults.put(teamId, teamResults);
            }
        }

        if (mResults.size() == 0) {
            return;
        }

        String idArgs = "(" + TextUtils.join(",", mResults.keySet().toArray()) + ")";

        String resultsQuery = "SELECT * " +
                "FROM " + DatabaseContract.MatchResult.TABLE_NAME + " " +
                "WHERE " + DatabaseContract.MatchResult.COLUMN_NAME_WINNER_ID + " in " + idArgs;

        cursor = db.rawQuery(resultsQuery, null);

        while (cursor.moveToNext()) {
            int winnerId = cursor.getInt(cursor.getColumnIndex(DatabaseContract.MatchResult.COLUMN_NAME_WINNER_ID));
            int loserId = cursor.getInt(cursor.getColumnIndex(DatabaseContract.MatchResult.COLUMN_NAME_LOSER_ID));
            int loserGoals = cursor.getInt(cursor.getColumnIndex(DatabaseContract.MatchResult.COLUMN_NAME_LOSER_SCORE));

            mResults.get(winnerId).addWin();
            mResults.get(winnerId).addGoalsFor(10);
            mResults.get(winnerId).addGoalsAgainst(loserGoals);
            mResults.get(loserId).addLoss();
            mResults.get(loserId).addGoalsFor(loserGoals);
            mResults.get(loserId).addGoalsAgainst(10);
        }

        cursor.close();
        db.close();
    }

    private void populateTable() {
        ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.constraint_layout);
        TableLayout table = (TableLayout) findViewById(R.id.table);

        table.removeAllViews();

        List<TeamResult> sorted = new ArrayList<>(mResults.values());
        Collections.sort(sorted);

        final TableRow headers = (TableRow) getLayoutInflater().inflate(R.layout.table_row, layout);
        ((TextView) headers.findViewById(R.id.table_name)).setText(R.string.header_name);
        ((TextView) headers.findViewById(R.id.table_wins)).setText(R.string.header_wins);
        ((TextView) headers.findViewById(R.id.table_losses)).setText(R.string.header_losses);
        ((TextView) headers.findViewById(R.id.table_win_percentage)).setText(R.string.header_win_percent);
        ((TextView) headers.findViewById(R.id.table_goals_for)).setText(R.string.header_goals_for);
        ((TextView) headers.findViewById(R.id.table_goals_against)).setText(R.string.header_goals_against);
        ((TextView) headers.findViewById(R.id.table_name)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_wins)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_losses)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_win_percentage)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_goals_for)).setTypeface(null, Typeface.BOLD);
        ((TextView) headers.findViewById(R.id.table_goals_against)).setTypeface(null, Typeface.BOLD);
        table.addView(headers);

        for (TeamResult result : sorted) {
            final TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.table_row, layout);
            ((TextView) row.findViewById(R.id.table_name)).setText(result.getName());
            ((TextView) row.findViewById(R.id.table_wins)).setText(String.valueOf(result.getWins()));
            ((TextView) row.findViewById(R.id.table_losses)).setText(String.valueOf(result.getLosses()));
            ((TextView) row.findViewById(R.id.table_win_percentage)).setText(String.format(Locale.getDefault(), "%.3f", result.getWinPercentage()));
            ((TextView) row.findViewById(R.id.table_goals_for)).setText(String.valueOf(result.getGoalsFor()));
            ((TextView) row.findViewById(R.id.table_goals_against)).setText(String.valueOf(result.getGoalsAgainst()));
            table.addView(row);
        }
    }

    private HashMap<String, Integer> getTeams() {
        String teamsQuery = "SELECT " + DatabaseContract.Team.COLUMN_NAME_NAME + ", " + DatabaseContract.Team._ID + " FROM " + DatabaseContract.Team.TABLE_NAME + " " +
                "WHERE " + DatabaseContract.Team.TABLE_NAME + "." + DatabaseContract.Team.COLUMN_NAME_ACTIVE_MONTH + " = ? AND " +
                DatabaseContract.Team.TABLE_NAME + "." + DatabaseContract.Team.COLUMN_NAME_ACTIVE_YEAR + " = ?";

        String[] selectionArgs = {getMonthString(), getYearString()};

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
}
