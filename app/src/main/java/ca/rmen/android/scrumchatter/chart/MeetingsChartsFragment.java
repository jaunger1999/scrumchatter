/*
 * Copyright 2016-2017 Carmen Alvarez
 * <p/>
 * This file is part of Scrum Chatter.
 * <p/>
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.chart;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MeetingsChartsFragmentBinding;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.settings.Prefs;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.util.Log;


/**
 * Displays charts for all meetings.
 */
public class MeetingsChartsFragment extends Fragment {

    private static final String TAG = Constants.TAG + "/" + MeetingsChartsFragment.class.getSimpleName();
    private static final int LOADER_MEETING_DURATION = 0;
    private static final int LOADER_MEMBER_SPEAKING_TIME = 1;

    private MeetingsChartsFragmentBinding mBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        mBinding = DataBindingUtil.inflate(inflater, R.layout.meetings_charts_fragment, container, false);
        mBinding.fabShareMeetingDuration.setTag(mBinding.meetingDurationChartContent);
        mBinding.fabShareSpeakerTime.setTag(mBinding.speakerTimeChartContent);
        mBinding.setFabListener(new FabListener(getContext()));
        loadTeam();
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_MEETING_DURATION, null, mLoaderCallbacks);
        getLoaderManager().initLoader(LOADER_MEMBER_SPEAKING_TIME, null, mLoaderCallbacks);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            long teamId = Prefs.getInstance(getContext()).getTeamId();
            String[] selectionArgs = new String[]{String.valueOf(teamId)};

            if (id == LOADER_MEETING_DURATION) {
                String selection = MeetingColumns.TEAM_ID + "=?";
                return new CursorLoader(
                        getContext(),
                        MeetingColumns.CONTENT_URI,
                        null,
                        selection,
                        selectionArgs,
                        MeetingColumns.MEETING_DATE);
            } else {
                return new CursorLoader(getContext(),
                        MeetingMemberColumns.CONTENT_URI,
                        new String[]{
                                MeetingMemberColumns.MEMBER_ID,
                                MeetingMemberColumns.MEETING_ID,
                                MeetingColumns.MEETING_DATE,
                                MemberColumns.NAME,
                                MeetingMemberColumns.DURATION},
                        MeetingMemberColumns.DURATION + ">0 AND " + MeetingColumns.TEAM_ID + "=?",
                        selectionArgs,
                        MeetingMemberColumns.MEETING_ID + ", " + MemberColumns.NAME + " DESC");
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                if (loader.getId() == LOADER_MEETING_DURATION) {
                    MeetingDurationLineChart.populateMeetingDurationChart(getContext(), mBinding.meetingDurationChart, cursor);
                } else {
                    MemberSpeakingTimeColumnChart.populateMemberSpeakingTimeChart(getContext(), mBinding.speakerTimeChart, mBinding.legend, cursor);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private void loadTeam() {
        new Teams(getActivity()).readCurrentTeam().subscribe(team -> {
            mBinding.tvTitleMeetingDurationChart.setText(getString(R.string.chart_meeting_duration_title, team.teamName));
            mBinding.tvTitleSpeakerTimeChart.setText(getString(R.string.chart_speaker_time_title, team.teamName));
        });
    }

}
