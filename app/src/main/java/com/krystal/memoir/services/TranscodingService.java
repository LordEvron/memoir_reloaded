package com.krystal.memoir.services;

import static androidx.core.content.FileProvider.getUriForFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.googlecode.mp4parser.authoring.tracks.TextTrackImpl;
import com.krystal.memoir.MemoirApplication;
import com.krystal.memoir.database.Video;
import com.krystal.memoir.utils;

public class TranscodingService extends IntentService {

	public String extStorePath;
	public static String ActionCreateMyLife = "ActionCreateMyLife";
	public static String ActionTrimVideo = "ActionTrimVideo";

	public TranscodingService() {
		super("TranscodingService");
	}

	public void createMyLife(Intent intent) {
		String myLifePath = null;
		long startDate, endDate;
		startDate = intent.getLongExtra("startDate", 0);
		endDate = intent.getLongExtra("endDate", -1);
		Intent broadcastIntent = new Intent(
				TranscodingService.ActionCreateMyLife);

		myLifePath = getExternalFilesDir(Environment.DIRECTORY_MOVIES) + "/MyLife.mp4";
		File file = new File(MemoirApplication.convertPath(myLifePath));
		if (file.exists())
			file.delete();

		file = new File(myLifePath);
		if (file.exists())
			file.delete();

		List<List<Video>> dateList = ((MemoirApplication) getApplication())
				.getDBA().getVideos(startDate, endDate, true, false);

		if (dateList == null) {
			broadcastIntent.putExtra("OutputFileName", "");
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					broadcastIntent);
			return;
		}

		int i = 0, j = 0;
		ArrayList<Video> videoList = null;
		Video v = null;
		ArrayList<Movie> inMovies = new ArrayList<Movie>();
		ArrayList<String> inMoviesSubText = new ArrayList<String>();
		ArrayList<Long> inMoviesSubTextTime = new ArrayList<Long>();
		File videoFile = null;

		Collections.reverse(dateList);
		for (i = 0; i < dateList.size(); i++) {
			videoList = (ArrayList<Video>) dateList.get(i);

			for (j = 0; j < videoList.size(); j++) {
				v = videoList.get(j);
				videoFile = new File(v.path);

				if (videoFile != null) {
					try {
						Uri videoUri= getUriForFile(getBaseContext().getApplicationContext(),"com.krystal.fileprovider",videoFile);
						//File myfile = utils.getFile(getBaseContext().getApplicationContext(), videoUri);
						File myfile = new File(v.path);
						FileInputStream fis = new FileInputStream(videoFile);
						if (fis != null) {
							FileChannel fc = fis.getChannel();
							if (fc != null) {
								inMovies.add(MovieCreator.build(fc));
								inMoviesSubText.add(MemoirApplication
										.convertDate(v.date, ""));
								inMoviesSubTextTime.add(new Long(v.length));
							}
							fis.close();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		List<Track> videoTracks = new LinkedList<Track>();
		List<Track> audioTracks = new LinkedList<Track>();

		for (Movie m : inMovies) {
			for (Track t : m.getTracks()) {
				if (t.getHandler().equals("soun")) {
					audioTracks.add(t);
				}
				if (t.getHandler().equals("vide")) {
					videoTracks.add(t);
				}
			}
		}

		try {
			Movie result = new Movie();

			if (audioTracks.size() > 0) {
				result.addTrack(new AppendTrack(audioTracks
						.toArray(new Track[audioTracks.size()])));
			}
			if (videoTracks.size() > 0) {
				result.addTrack(new AppendTrack(videoTracks
						.toArray(new Track[videoTracks.size()])));
			}

			/** NOTE : Subtext */
			TextTrackImpl subTitleEng = new TextTrackImpl() {
				public String getHandler() {
					return "sbtl";
				}
			};

			subTitleEng.getTrackMetaData().setLanguage("eng");

			int len = inMoviesSubText.size();
			long time = 0;
			long t = 0;
			for (i = 0; i < len; i++) {
				t = inMoviesSubTextTime.get(i).longValue();
				subTitleEng.getSubs().add(
						new TextTrackImpl.Line(time, time + t, inMoviesSubText
								.get(i)));
				time = time + t;
			}
			result.addTrack(subTitleEng);

			/** END OF Subtext */
			IsoFile out = new DefaultMp4Builder().build(result);
			FileOutputStream fos = new FileOutputStream(file);
			FileChannel fc = fos.getChannel();
			fc.position(0);
			out.getBox(fc);
			fos.close();
			fc.close();
		} catch (IOException e) {
			broadcastIntent.putExtra("OutputFileName", "");
			broadcastIntent.putExtra("Error",
					"Error creating video. Try removing recent imports.");
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					broadcastIntent);

			e.printStackTrace();
			return;
		}

		MemoirApplication.mTL.convertThumbnail(myLifePath,
				MediaStore.Video.Thumbnails.MINI_KIND);

		broadcastIntent.putExtra("OutputFileName", myLifePath);
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
	}

	protected static long getDuration(Track track) {
		long duration = 0;
		for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
			duration += entry.getCount() * entry.getDelta();
		}
		return duration;
	}

	/*
	 * private static double correctTimeToSyncSample(Track track, double
	 * cutHere, boolean next) { double[] timeOfSyncSamples = new
	 * double[track.getSyncSamples().length]; long currentSample = 0; double
	 * currentTime = 0; for (int i = 0; i <
	 * track.getDecodingTimeEntries().size(); i++) { TimeToSampleBox.Entry entry
	 * = track.getDecodingTimeEntries().get(i); for (int j = 0; j <
	 * entry.getCount(); j++) { if (Arrays.binarySearch(track.getSyncSamples(),
	 * currentSample + 1) >= 0) { // samples always start with 1 but we start
	 * with zero // therefore +1 timeOfSyncSamples[Arrays.binarySearch(
	 * track.getSyncSamples(), currentSample + 1)] = currentTime; } currentTime
	 * += (double) entry.getDelta() / (double)
	 * track.getTrackMetaData().getTimescale(); currentSample++; } } double
	 * previous = 0; for (double timeOfSyncSample : timeOfSyncSamples) { if
	 * (timeOfSyncSample > cutHere) { if (next) { return timeOfSyncSample; }
	 * else { return previous; } } previous = timeOfSyncSample; } return
	 * timeOfSyncSamples[timeOfSyncSamples.length - 1]; }
	 */

	public void trimVideo(Intent intent) {
		Intent broadcastIntent = new Intent(TranscodingService.ActionTrimVideo);
		Movie movie = null;
		try {
			movie = MovieCreator.build(new FileInputStream(intent
					.getStringExtra("filePath")).getChannel());
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		List<Track> tracks = movie.getTracks();
		movie.setTracks(new LinkedList<Track>());

		double startTime1 = intent.getDoubleExtra("startTime", 0);
		double endTime1 = intent.getDoubleExtra("endTime", 0);

		/*
		 * //NOTE : This has been moved to the ImportVideoActivity while showing
		 * the video itself
		 * 
		 * boolean timeCorrected = false;
		 * 
		 * // Here we try to find a track that has sync samples. Since we can
		 * only // start decoding // at such a sample we SHOULD make sure that
		 * the start of the new // fragment is exactly // such a frame for
		 * (Track track : tracks) { if (track.getSyncSamples() != null &&
		 * track.getSyncSamples().length > 0) { if (timeCorrected) { // This
		 * exception here could be a false positive in case we // have multiple
		 * tracks // with sync samples at exactly the same positions. E.g. a //
		 * single movie containing // multiple qualities of the same video
		 * (Microsoft Smooth // Streaming file)
		 * 
		 * throw new RuntimeException(
		 * "The startTime has already been corrected by another track with SyncSample. Not Supported."
		 * ); } startTime1 = correctTimeToSyncSample(track, startTime1, false);
		 * endTime1 = correctTimeToSyncSample(track, endTime1, true);
		 * timeCorrected = true; } }
		 */

		for (Track track : tracks) {
			long currentSample = 0;
			double currentTime = 0;
			double lastTime = -1;
			long startSample1 = -1;
			long endSample1 = -1;

			for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
				TimeToSampleBox.Entry entry = track.getDecodingTimeEntries()
						.get(i);
				for (int j = 0; j < entry.getCount(); j++) {

					if (currentTime > lastTime && currentTime <= startTime1) {
						// current sample is still before the new starttime
						startSample1 = currentSample;
					}
					if (currentTime > lastTime && currentTime <= endTime1) {
						// current sample is after the new start time and still
						// before the new endtime
						endSample1 = currentSample;
					}
					lastTime = currentTime;
					currentTime += (double) entry.getDelta()
							/ (double) track.getTrackMetaData().getTimescale();
					currentSample++;
				}
			}

			try {
				movie.addTrack(new AppendTrack(new CroppedTrack(track,
						startSample1, endSample1)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		IsoFile out = new DefaultMp4Builder().build(movie);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(intent.getStringExtra("outputFilePath"));
			FileChannel fc = fos.getChannel();
			out.getBox(fc);
			fc.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		broadcastIntent.putExtra("OutputFileName",
				intent.getStringExtra("outputFilePath"));
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		if (intent.getAction().equals(TranscodingService.ActionCreateMyLife)) {
			createMyLife(intent);
		} else if (intent.getAction()
				.equals(TranscodingService.ActionTrimVideo)) {
			trimVideo(intent);
		}
	}
}
