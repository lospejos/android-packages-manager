package ru.ucoz.megadiablo.android.apm;

/**
 * @author MegaDiablo
 * */
public class ImplEvent implements IEvent {

	private Runnable mRunnable = null;
	private int mType = 0;
	private String mName = null;
	private String mDecription = null;

	@Override
	public void run() {
		if (mRunnable != null) {
			mRunnable.run();
		}
	}

	@Override
	public String getName() {
		if (mName == null) {
			mName = super.toString();
		}
		return mName;
	}

	@Override
	public String getDescription() {
		if (mDecription == null) {
			mDecription = "";
		}
		return mDecription;
	}

	public static ImplEvent createEvent(final int pType,
			final String pName,
			final String pDescription,
			final Runnable pRunnable) {

		ImplEvent event = new ImplEvent();

		event.mType = pType;
		event.mName = pName;
		event.mDecription = pDescription;
		event.mRunnable = pRunnable;

		return event;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int getType() {
		return mType;
	}
}
