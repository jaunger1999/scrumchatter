/*
 * Copyright 2013-2017 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.team;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.dialog.InputDialogFragment.InputValidator;
import ca.rmen.android.scrumchatter.provider.TeamColumns;
import ca.rmen.android.scrumchatter.settings.Prefs;
import ca.rmen.android.scrumchatter.util.Log;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Provides both UI and DB logic regarding the management of teams: renaming, choosing, creating, and deleting teams.
 */
public class Teams {
    private static final String TAG = Constants.TAG + "/" + Teams.class.getSimpleName();
    public static final String EXTRA_TEAM_URI = "team_uri";
    public static final String EXTRA_TEAM_ID = "team_id";
    private static final String EXTRA_TEAM_NAME = "team_name";
    private final FragmentActivity mActivity;

    public static class Team {
        private final Uri teamUri;
        public final String teamName;

        private Team(Uri teamUri, String teamName) {
            this.teamUri = teamUri;
            this.teamName = teamName;
        }

        @Override
        public String toString() {
            return "Team [teamUri=" + teamUri + ", teamName=" + teamName + "]";
        }

    }

    public static final class TeamsData {
        public final Team currentTeam;
        public final List<Team> teams;

        private TeamsData(Team currentTeam, List<Team> teams) {
            this.currentTeam = currentTeam;
            this.teams = teams;
        }
    }

    public Teams(FragmentActivity activity) {
        mActivity = activity;
    }

    /**
     * Upon selecting a team, update the shared preference for the selected team.
     */
    public void switchTeam(final CharSequence teamName) {
        Log.v(TAG, "switchTeam " + teamName);
        Schedulers.io().scheduleDirect(() -> {
            Cursor c = mActivity.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { TeamColumns._ID }, TeamColumns.TEAM_NAME + " = ?",
                    new String[] { String.valueOf(teamName) }, null);
            if (c != null) {
                try {
                    c.moveToFirst();
                    if (c.getCount() == 1) {
                        int teamId = c.getInt(0);
                        Prefs.getInstance(mActivity).setTeamId(teamId);
                    } else {
                        Log.wtf(TAG, "Found " + c.getCount() + " teams for " + teamName);
                    }

                } finally {
                    c.close();
                }
            }
        });
    }

    /**
     * Show a dialog with a text input for the new team name. Validate that the team doesn't already exist. Upon pressing "OK", create the team.
     */
    public void promptCreateTeam() {
        Log.v(TAG, "promptCreateTeam");
        DialogFragmentFactory.showInputDialog(mActivity, mActivity.getString(R.string.action_new_team), mActivity.getString(R.string.hint_team_name), null,
                TeamNameValidator.class, R.id.action_team, null);
    }

    public void createTeam(final String teamName) {
        Log.v(TAG, "createTeam, name=" + teamName);
        // Ignore an empty name.
        if (!TextUtils.isEmpty(teamName)) {
            // Create the new team in a background thread.
            Schedulers.io().scheduleDirect(() -> {
                ContentValues values = new ContentValues(1);
                values.put(TeamColumns.TEAM_NAME, teamName);
                Uri newTeamUri = mActivity.getContentResolver().insert(TeamColumns.CONTENT_URI, values);
                if (newTeamUri != null) {
                    int newTeamId = Integer.valueOf(newTeamUri.getLastPathSegment());
                    Prefs.getInstance(mActivity).setTeamId(newTeamId);
                }
            });
        }
    }

    /**
     * Retrieve the currently selected team. Show a dialog with a text input to rename this team. Validate that the new name doesn't correspond to any other
     * existing team. Upon pressing ok, rename the current team.
     */
    public void promptRenameTeam(final Team team) {
        Log.v(TAG, "promptRenameTeam, team=" + team);
        if (team != null) {
            // Show a dialog to input a new team name for the current team.
            Bundle extras = new Bundle(1);
            extras.putParcelable(EXTRA_TEAM_URI, team.teamUri);
            extras.putString(EXTRA_TEAM_NAME, team.teamName);
            DialogFragmentFactory.showInputDialog(mActivity, mActivity.getString(R.string.action_team_rename), mActivity.getString(R.string.hint_team_name),
                    team.teamName, TeamNameValidator.class, R.id.action_team_rename, extras);
        }
    }

    public void renameTeam(final Uri teamUri, final String teamName) {
        Log.v(TAG, "renameTeam, uri = " + teamUri + ", name = " + teamName);

        // Ignore an empty name.
        if (!TextUtils.isEmpty(teamName)) {
            // Rename the team in a background thread.
            Schedulers.io().scheduleDirect(() -> {
                ContentValues values = new ContentValues(1);
                values.put(TeamColumns.TEAM_NAME, teamName);
                mActivity.getContentResolver().update(teamUri, values, null, null);
            });
        }
    }

    /**
     * Shows a confirmation dialog to the user to delete a team.
     */
    @MainThread
    public void confirmDeleteTeam(final Team team) {
        Log.v(TAG, "confirmDeleteTeam, team = " + team);

        getTeamCount()
                .subscribe(teamCount -> {
                    // We need at least one team in the app.
                    if (teamCount <= 1) {
                        DialogFragmentFactory.showInfoDialog(mActivity, R.string.action_team_delete, R.string.dialog_error_one_team_required);
                    }
                    // Delete this team
                    else if (team != null) {
                        Bundle extras = new Bundle(1);
                        extras.putParcelable(EXTRA_TEAM_URI, team.teamUri);
                        DialogFragmentFactory.showConfirmDialog(mActivity, mActivity.getString(R.string.action_team_delete),
                                mActivity.getString(R.string.dialog_message_delete_team_confirm, team.teamName), R.id.action_team_delete, extras);
                    }
                });
    }

    /**
     * Deletes the team and all its members from the DB.
     */
    public void deleteTeam(final Uri teamUri) {
        Log.v(TAG, "deleteTeam, uri = " + teamUri);
        Schedulers.io().scheduleDirect(() -> {
            // delete this team
            mActivity.getContentResolver().delete(teamUri, null, null);
            // pick another current team
            selectFirstTeam();
        });
    }

    /**
     * Select the first team in our DB.
     */
    private Team selectFirstTeam() {
        Cursor c = mActivity.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { TeamColumns._ID, TeamColumns.TEAM_NAME }, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int teamId = c.getInt(0);
                    String teamName = c.getString(1);
                    Prefs.getInstance(mActivity).setTeamId(teamId);
                    Uri teamUri = Uri.withAppendedPath(TeamColumns.CONTENT_URI, String.valueOf(teamId));
                    return new Team(teamUri, teamName);
                }
            } finally {
                c.close();
            }
        }
        return null;
    }

    /**
     * @return the Team currently selected by the user.
     */
    @WorkerThread
    private Team getCurrentTeam() {
        // Retrieve the current team name and construct a uri for the team based on the current team id.
        int teamId = Prefs.getInstance(mActivity).getTeamId();
        Uri teamUri = Uri.withAppendedPath(TeamColumns.CONTENT_URI, String.valueOf(teamId));
        Cursor c = mActivity.getContentResolver().query(teamUri, new String[] { TeamColumns.TEAM_NAME }, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    String teamName = c.getString(0);
                    return new Team(teamUri, teamName);
                }
            } finally {
                c.close();
            }
        }
        Log.wtf(TAG, "Could not get the current team", new Throwable());
        return selectFirstTeam();
    }

    /**
     * Query the teams table, and return a list of all teams, and the current team.
     */
    public Single<TeamsData> getAllTeams() {
        Log.v(TAG, "getAllTeams");
        return Single.fromCallable(() -> {
            Log.v(TAG, "getAllTeams work");
            List<Team> teams = new ArrayList<>();
            Cursor c = mActivity.getContentResolver().query(
                    TeamColumns.CONTENT_URI,
                    new String[]{TeamColumns._ID, TeamColumns.TEAM_NAME},
                    null, null,
                    TeamColumns.TEAM_NAME + " COLLATE NOCASE");

            if (c != null) {
                try {
                    // Add the names of all the teams
                    while (c.moveToNext()) {
                        int teamId = c.getInt(0);
                        String teamName = c.getString(1);
                        Uri teamUri = Uri.withAppendedPath(TeamColumns.CONTENT_URI, String.valueOf(teamId));
                        teams.add(new Team(teamUri, teamName));
                    }
                } finally {
                    c.close();
                }
            }
            return new TeamsData(getCurrentTeam(), teams);

        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * @return the total number of teams
     */
    public Single<Integer> getTeamCount() {
        return Single.fromCallable(() -> {

            Cursor c = mActivity.getContentResolver().query(TeamColumns.CONTENT_URI, new String[]{"count(*)"}, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) return c.getInt(0);
                } finally {
                    c.close();
                }
            }
            return 0;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Team> readCurrentTeam() {
        return Single.fromCallable(this::getCurrentTeam)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns an error if the user entered the name of an existing team. To prevent renaming or creating multiple teams with the same name.
     */
    public static class TeamNameValidator implements InputValidator { // NO_UCD (use private)

        public TeamNameValidator() {}

        @Override
        public String getError(Context context, CharSequence input, Bundle extras) {
            // teamName is optional. If given, we won't show an error for renaming a team to its current name.
            String teamName = extras == null ? null : extras.getString(Teams.EXTRA_TEAM_NAME);

            // In the case of changing a team name, teamName will not be null, and we won't show an error if the name the user enters is the same as the existing team.
            // In the case of adding a new team, mTeamName will be null.
            if (!TextUtils.isEmpty(teamName) && !TextUtils.isEmpty(input) && teamName.equals(input.toString())) return null;

            // Query for a team with this name.
            Cursor cursor = context.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { "count(*)" }, TeamColumns.TEAM_NAME + "=?",
                    new String[] { String.valueOf(input) }, null);

            // Now Check if the team member exists.
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int existingTeamCount = cursor.getInt(0);
                    cursor.close();
                    if (existingTeamCount > 0) return context.getString(R.string.error_team_exists, input);
                }
            }
            return null;
        }
    }

}
