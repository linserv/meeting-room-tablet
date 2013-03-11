package com.futurice.android.reservator;

import java.util.Comparator;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.Window;
import android.widget.DigitalClock;
import android.widget.LinearLayout;

import com.futurice.android.reservator.model.DataProxy;
import com.futurice.android.reservator.model.DataUpdatedListener;
import com.futurice.android.reservator.model.DateTime;
import com.futurice.android.reservator.model.ReservatorException;
import com.futurice.android.reservator.model.Room;
import com.futurice.android.reservator.model.rooms.RoomsInfo;
import com.futurice.android.reservator.view.LobbyReservationRowView;
import com.futurice.android.reservator.view.LobbyReservationRowView.OnReserveListener;

public class LobbyActivity extends ReservatorActivity implements OnMenuItemClickListener,
		DataUpdatedListener {
	MenuItem settingsMenu, refreshMenu;
	LinearLayout container = null;
	DataProxy proxy;

	private ProgressDialog progressDialog = null;
	int showLoadingCount = 0;

	final Handler handler = new Handler();
	
	AlertDialog alertDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.lobby_activity);
		proxy = this.getResApplication().getDataProxy();
		DigitalClock clock =  (DigitalClock)findViewById(R.id.digitalClock1);  //FIXME deprecated
        clock.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/EHSMB.TTF"));
	}

	@Override
	public void onResume() {
		super.onResume();
		showLoadingCount = 0; //TODO better fix
		proxy.addDataUpdatedListener(this);
		refreshRoomInfo();
	}

	@Override
	public void onPause() {
		super.onPause();
		proxy.removeDataUpdatedListener(this);
		if(progressDialog != null){
			progressDialog.dismiss();
			showLoadingCount = 0;
			progressDialog = null;
		}
	}
	
	@Override
	protected Boolean isPrehensible() {
		String favouriteRoomName = getResApplication().getFavouriteRoomName();
		return !(favouriteRoomName.equals(getString(R.string.lobbyRoomName)));
	}
	
	private void refreshRoomInfo() {
		updateLoadingWindow(1);
		container = (LinearLayout) findViewById(R.id.linearLayout1);
		container.removeAllViews();
		proxy.refreshRooms();
	}
	
	/*
	 * @param howMuch If howMuch > 0, an item has been added to our ProgressDialog and 
	 * we need to increase the number of max items. If howMuch is less than zero, 
	 * something has been completed and we can increase the progress. This should be
	 * split to two different functions, incrementMaxItems() and itemFinished(),
	 * because (at least in the current version) there is always only one increment
	 * at time.
	 */
	private void updateLoadingWindow(int howMuch) {
		showLoadingCount += howMuch;

		// if loadingcount <= 0 => dismiss
		if (showLoadingCount <= 0 && progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
			return;
		}
		
		if (showLoadingCount > 0 && (progressDialog == null || !progressDialog.isShowing())) {
			if (progressDialog != null)
				progressDialog.dismiss();
			progressDialog = constructNewProgressDialog();
			progressDialog.setMax(showLoadingCount);
			progressDialog.show();
		}
		
		if (progressDialog != null) {
			// Increment the maximum number of items if needed
			if (showLoadingCount > progressDialog.getMax()) {
				progressDialog.setMax(showLoadingCount);
			}
			// "howMuch < 0" => something has been completed.
			if (howMuch < 0) {
				progressDialog.incrementProgressBy(Math.abs(howMuch));
			}
		}
	}
	
	private ProgressDialog constructNewProgressDialog() {
		ProgressDialog d = new ProgressDialog(this);
		d.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		d.setMessage("Refreshing room list...");
		d.setCancelable(false);
		d.setMax(1);
		return d;
	}

/*	private void showLoading() {
//		if (showLoadingCount < 0)
//			showLoadingCount = 0;
		showLoadingCount++;
		if (this.progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage("Refreshing room list...");
			progressDialog.setCancelable(false);
			progressDialog.setMax(1);
			progressDialog.show();
		}

		if (this.progressDialog != null) {
			if (showLoadingCount > progressDialog.getMax()) {
				progressDialog.setMax(showLoadingCount);
			}
		}
	}

	private void hideLoading() {
		showLoadingCount--;
		if (this.progressDialog != null){
			progressDialog.setTitle("Loading count " + showLoadingCount);
			progressDialog.setProgress(progressDialog.getMax() - Math.max(0, showLoadingCount));
		}
		if (showLoadingCount <= 0) {
			if (this.progressDialog != null) {
				this.progressDialog.dismiss();
				this.progressDialog = null;
			}
		}

	}
	*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		refreshMenu = menu.add("Refresh").setOnMenuItemClickListener(this);
		refreshMenu.setIcon(android.R.drawable.ic_popup_sync);
		settingsMenu = menu.add("Settings").setOnMenuItemClickListener(this);
		settingsMenu.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		if (item == settingsMenu) {
			Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);
		} else if (item == refreshMenu) {
			refreshRoomInfo();
		}
		return true;
	}

	@Override
	public void roomListUpdated(Vector<Room> rooms) {
		//proceed to requesting room reservation data
		for (Room r : rooms) {
			RoomsInfo info = RoomsInfo.getRoomsInfo(r);
			if (info.isProjectRoom()) {
				continue; // skip project room
			}
			updateLoadingWindow(1);
			proxy.refreshRoomReservations(r);
		}
		updateLoadingWindow(-1);
	}

	@Override
	public void roomReservationsUpdated(final Room room) {
		processRoom(room);
		updateLoadingWindow(-1);
	}

/*	This is never used?
 * 
 **/ 
	@Override
	public void refreshFailed(ReservatorException e) {
	/*	
		if (alertDialog != null) {
			Toast.makeText(this, "dismissed an alert", Toast.LENGTH_SHORT).show();
			alertDialog.dismiss();
		}
			
		hideLoading();
		Builder alertBuilder = new AlertDialog.Builder(this);
		alertDialog = alertBuilder.setTitle("Error")
			.setMessage(e.getMessage())
			.show();
		Toast.makeText(this, "created an alert", Toast.LENGTH_SHORT).show();
		*/
	}

	private void processRoom(Room r) {
		LobbyReservationRowView v = new LobbyReservationRowView(LobbyActivity.this);
		if (v.getException() != null) {
			//updateLoadingWindow(-1);
			Log.d("LobbyReservator", "Exception in LobbyReservator: " + v.getException().getMessage());
			// show only one dialog at time
			if (alertDialog == null || !alertDialog.isShowing()) {
				if (alertDialog != null)
					alertDialog.dismiss();
				Builder alertBuilder = new AlertDialog.Builder(this);
				alertBuilder.setTitle("Error!");
				alertBuilder.setMessage(v.getException().getMessage());
				alertDialog = alertBuilder.show();
			}
		}
		
		v.setRoom(r);
		v.setOnReserveCallback(new OnReserveListener() {
			@Override
			public void call(LobbyReservationRowView v) {
				refreshRoomInfo();
			}
		});

		// This is ugly, adding views in order.
		Comparator<Room> roomCmp = new Comparator<Room>() {
			private DateTime now = new DateTime();

			@Override
			public int compare(Room room1, Room room2) {
				boolean room1Free = room1.isFree() && room1.minutesFreeFromNow() >= 30;
				boolean room2Free = room2.isFree() && room2.minutesFreeFromNow() >= 30;

				if (room1Free && !room2Free) {
					return -1;
				} else if (!room1Free && room2Free) {
					return 1;
				} else if (room1Free && room2Free) {
					// Log.d("Lobby", room1.toString() + " -- " + room2.toString());
					return room2.minutesFreeFrom(now)
							- room1.minutesFreeFrom(now);
				} else {
					return 0;
					//return room1.reservedForFrom(now) - room2.reservedForFrom(now);
				}
			}
		};

		int roomCount = container.getChildCount();
		boolean added = false;
		for (int index = 0; index < roomCount; index++) {
			Room r2 = ((LobbyReservationRowView) container.getChildAt(index))
					.getRoom();

			// Log.d("Lobby", "sorting: " + r.getName() + ":" + r.isFree() + " -- " + r2.getName() + ":" + r2.isFree());

			if (r.equals(r2)) {
				Log.d("LobbyActivity", "duplicate room -- " + r.getEmail());
				// XXX: minor logic error; same room
				// someone else requested also an update, and we got rooms twice
				container.removeViewAt(index);
				container.addView(v, index);
				added = true;
				break;
			}
			else if (roomCmp.compare(r, r2) < 0) {
				container.addView(v, index);
				added = true;
				break;
			}
		}
		if (!added) {
			container.addView(v);
		}
	}
}
