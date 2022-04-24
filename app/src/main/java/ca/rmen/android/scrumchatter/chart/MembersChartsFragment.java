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
import ca.rmen.android.scrumchatter.databinding.MembersChartsFragmentBinding;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberStatsColumns;
import ca.rmen.android.scrumchatter.settings.Prefs;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.util.Log;

/**
 * Displays charts for members.
 */
public class MembersChartsFragment extends Fragment {

    private static final String TAG = Constants.TAG + "/" + MembersChartsFragment.class.getSimpleName();
    private static final int LOADER_MEMBER_SPEAKING_TIME = 0;
    private static final int LOADER_MEETING_DATES= 1;

    private MembersChartsFragmentBinding mBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        mBinding = DataBindingUtil.inflate(inflater, R.layout.members_charts_fragment, container, false);
        FabListener listener = new FabListener(getContext());

        mBinding.pieChartCardAvg.setFabListener(listener);
        mBinding.pieChartCardTotal.setFabListener(listener);
        mBinding.pieChartCardAvg.fabShareMemberSpeakingTime.setTag(mBinding.pieChartCardAvg.pieChartContent.memberSpeakingTimeChartContent);
        mBinding.pieChartCardTotal.fabShareMemberSpeakingTime.setTag(mBinding.pieChartCardTotal.pieChartContent.memberSpeakingTimeChartContent);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_MEMBER_SPEAKING_TIME, null, mLoaderCallbacks);
        getLoaderManager().initLoader(LOADER_MEETING_DATES, null, mLoaderCallbacks);
        loadTeam();
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            long teamId = Prefs.getInstance(getContext()).getTeamId();
            String[] selectionArgs = new String[]{String.valueOf(teamId)};

            if (id == LOADER_MEMBER_SPEAKING_TIME) {
                String[] projection = new String[]{MemberColumns._ID, MemberColumns.NAME, MemberStatsColumns.SUM_DURATION, MemberStatsColumns.AVG_DURATION};
                String selection = MemberStatsColumns.TEAM_ID + " =? AND " + MemberColumns.DELETED + "=0 ";
                return new CursorLoader(getContext(), MemberStatsColumns.CONTENT_URI, projection, selection, selectionArgs, null);
            } else {
                String[] projection = new String[]{
                        "MIN(" + MeetingColumns.MEETING_DATE + ")",
                        "MAX(" + MeetingColumns.MEETING_DATE + ")",
                };
                String selection = MeetingColumns.TEAM_ID + " = ?";
                return new CursorLoader(getContext(), MeetingColumns.CONTENT_URI, projection, selection, selectionArgs, null);
            }

        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                if (loader.getId() == LOADER_MEMBER_SPEAKING_TIME) {
                    MemberSpeakingTimePieChart.populateMemberSpeakingTimeChart(getContext(),
                            mBinding.pieChartCardAvg.pieChartContent,
                            mBinding.pieChartCardTotal.pieChartContent,
                            cursor);
                } else {
                    MemberSpeakingTimePieChart.updateMeetingDateRanges(getContext(),
                            mBinding.pieChartCardAvg.pieChartContent.tvSubtitleDateMemberSpeakingTimeChart,
                            mBinding.pieChartCardTotal.pieChartContent.tvSubtitleDateMemberSpeakingTimeChart,
                            cursor);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private void loadTeam() {
        new Teams(getActivity()).readCurrentTeam().subscribe(team -> {
            mBinding.pieChartCardAvg.pieChartContent.tvTitleMemberSpeakingTimeChart.setText(getString(R.string.chart_member_average_speaking_time_title, team.teamName));
            mBinding.pieChartCardTotal.pieChartContent.tvTitleMemberSpeakingTimeChart.setText(getString(R.string.chart_member_total_speaking_time_title, team.teamName));
        });
    }
}
