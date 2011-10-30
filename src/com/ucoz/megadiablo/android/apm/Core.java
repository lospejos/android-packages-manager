package com.ucoz.megadiablo.android.apm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.adbhelper.adb.AdbDevice;
import com.adbhelper.adb.AdbModule;
import com.adbhelper.adb.AdbPackage;
import com.adbhelper.adb.exseptions.DeviceIsEmulatorRebootException;
import com.adbhelper.adb.exseptions.NotAccessPackageManager;
import com.adbhelper.adb.exseptions.NotFoundActivityException;
import com.ucoz.megadiablo.android.apm.iface.DevicesListener;
import com.ucoz.megadiablo.android.apm.iface.PackagesListener;

/**
 * @author MegaDiablo
 * */
public class Core {

	private Events mEvents;

	private AdbModule mAdbModule;
	private AdbDevice mSelectDevice = null;

	private List<DevicesListener> mDevicesListeners = new ArrayList<DevicesListener>();
	private List<PackagesListener> mPackagesListeners = new ArrayList<PackagesListener>();

	public Core(AdbModule pAdbModule, Events pEvents) {
		mAdbModule = pAdbModule;
		mEvents = pEvents;
	}

	public AdbDevice getSelectDevice() {
		return mSelectDevice;
	}

	public void setSelectDevice(final AdbDevice pAdbDevice) {
		String name = "Выбор устройства";
		String desc = String.format("Смена устройства на %s.", pAdbDevice);

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {

				if (mSelectDevice == null && mSelectDevice != pAdbDevice
						|| mSelectDevice != null
						&& !mSelectDevice.equals(pAdbDevice)) {

					mSelectDevice = pAdbDevice;

					fireListener(mDevicesListeners,
							new EventListener<DevicesListener>() {
								@Override
								public void perfom(DevicesListener pItem) {
									pItem.changeSelectDevice(pAdbDevice);
								}
							});

					refreshPackages();
				}
			}
		});
	}

	public void refreshDevices() {

		String name = "Обновление списка устройств";
		String desc = "Обновления списка подключенных устройств.";

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				final List<AdbDevice> devices = mAdbModule.devices();

				fireListener(mDevicesListeners,
						new EventListener<DevicesListener>() {
							@Override
							public void perfom(DevicesListener pItem) {
								pItem.updateListDevices(devices);
							}
						});

				if (!devices.contains(mSelectDevice)) {

					final AdbDevice device = mSelectDevice;

					if (mSelectDevice != null) {
						fireListener(mDevicesListeners,
								new EventListener<DevicesListener>() {
									@Override
									public void perfom(DevicesListener pItem) {
										pItem.lostSelectDevice(device);
									}
								});
					}

					if (devices.size() > 0) {
						setSelectDevice(devices.get(0));
					} else {
						setSelectDevice(null);
					}
				}
			}
		});
	}

	public void refreshPackages() {

		String name = "Обновление списка пакетов";
		String desc = "Обновляется список пакетов на текущем выделеннойм утсройстве.";

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				List<AdbPackage> packs = new ArrayList<AdbPackage>();

				if (mSelectDevice == null) {
					packs = new ArrayList<AdbPackage>();
				} else {
					try {
						packs = mSelectDevice.refreshListPackages();
					} catch (NotAccessPackageManager e) {
						e.printStackTrace();
					}
				}

				final List<AdbPackage> packages = packs;
				fireListener(mPackagesListeners,
						new EventListener<PackagesListener>() {
							@Override
							public void perfom(PackagesListener pItem) {
								pItem.updatePackages(packages);
							}
						});
			}
		});

	}

	public void rebootDevice() {
		String name = "Перезагрузка";
		String desc = String.format("Перезагрузка устройства %s.",
				mSelectDevice);

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				if (mSelectDevice != null) {
					try {
						mSelectDevice.reboot();
					} catch (DeviceIsEmulatorRebootException e) {
						e.printStackTrace();
					}
					refreshDevices();
				}
			}
		});
	}

	public void install(final String pFile) {
		String name = "Установка приложения";
		String desc = String.format("Установка приложение %s.", pFile);

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				mSelectDevice.install(pFile);
			}
		});
	}

	public void install(final File pFile) {
		install(pFile.getAbsolutePath());
	}

	public void uninstall(final AdbPackage pAdbPackage) {
		String name = "Удаление приложения";
		String desc = String.format("Удаляется приложение %s.", pAdbPackage);

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				pAdbPackage.uninstall();
			}
		});

	}

	public void download(final AdbPackage pAdbPackage, final String pPath) {
		String name = "Скачка приложения";
		String desc = String.format("Скачивает приложение %s.", pAdbPackage);

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				if (pPath == null) {
					pAdbPackage.download();
				} else {
					pAdbPackage.download(pPath);
				}
			}
		});
	}

	public void startApp(final AdbPackage pAdbPackage) {
		String name = "Запуск приложения";
		String desc = String.format("Запускает приложение %s.", pAdbPackage);

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				try {
					pAdbPackage.start();
				} catch (NotFoundActivityException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void sendKey(final int pKey) {
		String name = "Нажать клавишу";
		String desc = String.format("Нажать клавишу с кодом %s.", pKey);

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				mSelectDevice.sendKeyCode(pKey);
			}
		});
	}

	public void termantedAllTasks() {
		mEvents.clearList();
		terminatedCurrentTask();
	}

	public void terminatedCurrentTask() {
		mAdbModule.stopCurrentProcess();
	}

	public void stopAdb() {

		String name = "Остановка ADB";
		String desc = "Останавливается ADB";

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				mAdbModule.stop();

				mSelectDevice = null;
				refreshEmptyDevices();
				refreshEmptyPackages();
			}
		});

	}

	public void startAdb() {

		String name = "Запуск ADB";
		String desc = "Запускается ADB";

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				mAdbModule.start();

				refreshDevices();
				refreshPackages();
			}
		});

	}

	public void restartAdb() {

		String name = "Перезапуск ADB";
		String desc = "Перезапускается ADB";

		mEvents.add(name, desc, new Runnable() {
			@Override
			public void run() {
				mAdbModule.restart();

				mSelectDevice = null;
				refreshDevices();
//				refreshPackages();
			}
		});

	}

	// ====================================================
	// Devices listener
	// ====================================================
	public void addDevicesListener(DevicesListener pDevicesListener) {
		addListener(mDevicesListeners, pDevicesListener);
	}

	public void removeDevicesListener(DevicesListener pDevicesListener) {
		removeListener(mDevicesListeners, pDevicesListener);
	}

	public void removeAllDevicesListener() {
		removeAllListener(mDevicesListeners);
	}

	// ====================================================
	// Packages listener
	// ====================================================
	public void addPackagesListener(PackagesListener pListener) {
		addListener(mPackagesListeners, pListener);
	}

	public void removePackagesListener(PackagesListener pListener) {
		removeListener(mPackagesListeners, pListener);
	}

	public void removeAllPackagesListener() {
		removeAllListener(mPackagesListeners);
	}

	// ====================================================
	// private methods/interface listeners
	// ====================================================
	private <T> void addListener(final List<T> pList, final T pItem) {
		if (pList != null && pItem != null) {
			pList.add(pItem);
		}
	}

	private <T> void removeListener(final List<T> pList, final T pItem) {
		if (pList != null && pItem != null) {
			pList.remove(pItem);
		}
	}

	private <T> void removeAllListener(final List<T> pList) {
		pList.clear();
	}

	private <T> void fireListener(final List<T> pList,
			final EventListener<T> pEvent) {

		if (pList == null) {
			return;
		}

		for (T item : pList) {
			if (item != null) {
				pEvent.perfom(item);
			}
		}
	}

	private void refreshEmptyPackages() {
		final List<AdbPackage> packages = new ArrayList<AdbPackage>();
		fireListener(mPackagesListeners, new EventListener<PackagesListener>() {
			@Override
			public void perfom(PackagesListener pItem) {
				pItem.updatePackages(packages);
			}
		});
	}

	private void refreshEmptyDevices() {
		final List<AdbDevice> devices = new ArrayList<AdbDevice>();
		fireListener(mDevicesListeners, new EventListener<DevicesListener>() {
			@Override
			public void perfom(DevicesListener pItem) {
				pItem.updateListDevices(devices);
			}
		});
	}

	private interface EventListener<T> {
		public void perfom(final T pItem);
	}

}
