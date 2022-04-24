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
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MeetingChartFragmentBinding;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.util.TextUtils;
import io.reactivex.Single;

/**
 * Displays charts for one meeting
 */
public class MeetingChartFragment extends Fragment {

    private static final String TAG = Constants.TAG + "/" + MeetingChartFragment.class.getSimpleName();
    private static final int LOADER_MEMBER_SPEAKING_TIME = 0;

    private MeetingChartFragmentBinding mBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        mBinding = DataBindingUtil.inflate(inflater, R.layout.meeting_chart_fragment, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_MEMBER_SPEAKING_TIME, null, mLoaderCallbacks);
        setHasOptionsMenu(true);
        loadMeeting(getActivity().getIntent().getLongExtra(Meetings.EXTRA_MEETING_ID, -1));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.meeting_chart_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            ChartExportTask.export(getContext(), mBinding.memberSpeakingTimeChartContent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            long meetingId = getActivity().getIntent().getLongExtra(Meetings.EXTRA_MEETING_ID, -1);

            String[] projection = new String[]{
                    MeetingMemberColumns._ID,
                    MeetingMemberColumns.MEMBER_ID,
                    MemberColumns.NAME,
                    MeetingMemberColumns.DURATION,
                    MeetingMemberColumns.TALK_START_TIME};
            String selection = MeetingMemberColumns.DURATION + ">0";
            String orderBy = MeetingMemberColumns.DURATION + " DESC";

            Uri uri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, String.valueOf(meetingId));
            return new CursorLoader(getActivity(), uri, projection, selection, null, orderBy);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                if (loader.getId() == LOADER_MEMBER_SPEAKING_TIME) {
                    MeetingSpeakingTimeColumnChart.populateMeeting(getContext(),
                            mBinding.memberSpeakingTimeChart,
                            cursor);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private static class MeetingDisplayInfo {
        final String teamName;
        final String meetingDuration;
        final String meetingStartDate;

        MeetingDisplayInfo(String teamName, String meetingDuration, String meetingStartDate) {
            this.teamName = teamName;
            this.meetingDuration = meetingDuration;
            this.meetingStartDate = meetingStartDate;
        }
    }

    private MeetingDisplayInfo createMeetingDisplayInfo(Teams.Team team, Meeting meeting) {
        return new MeetingDisplayInfo(
                getString(R.string.chart_member_speaking_time_title, team.teamName),
                getString(R.string.chart_total_duration, DateUtils.formatElapsedTime(meeting.getDuration())),
                TextUtils.formatDateTime(getContext(), meeting.getStartDate()));
    }

    @MainThread
    private void displayMeetingInfo(MeetingDisplayInfo meetingDisplayInfo) {
        mBinding.tvTitleMemberSpeakingTimeChart.setText(meetingDisplayInfo.teamName);
        mBinding.tvSubtitleDateMemberSpeakingTimeChart.setText(meetingDisplayInfo.meetingStartDate);
        mBinding.tvSubtitleDurationMemberSpeakingTimeChart.setText(meetingDisplayInfo.meetingDuration);
    }

    private void loadMeeting(long meetingId) {
        Single.zip(new Teams(getActivity()).readCurrentTeam(),
                new Meetings(getActivity()).readMeeting(meetingId),
                this::createMeetingDisplayInfo)

                .subscribe(this::displayMeetingInfo,
                        throwable -> Log.v(TAG, "Couldn't load meeting " + meetingId, throwable));
    }
}
