package com.example.scanner;

import android.os.SystemClock;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class scannerAppTools {

    private static DecimalFormat df = new DecimalFormat("0.00");

    double[] getMw(ArrayList list) {

        if (list.size() > 0) {
            double Wsum = 0;
            double dBm;
            double mW;
            for (int i = 0; i < list.size(); i++) {
                dBm = (long) list.get(i);
                if (dBm < 0 && dBm > -200) {
                    mW = Math.pow(10, ((dBm - 30) / 10));
                    Wsum += mW;
                }
            }

            double nsum = Double.parseDouble(df.format(Wsum * 1000000000));
            double dBmSum = Double.parseDouble(df.format(10 * (Math.log10(1000 * Wsum))));

            return new double[]{dBmSum, nsum};
        } else {
            return new double[]{0, 0};
        }
    }

    ArrayList[] telephonyDBm(List<CellInfo> getAllCellInfo) {
        ArrayList<Long> netList = new ArrayList<>();
        ArrayList<Long> timeList = new ArrayList<>();
        ArrayList<String> nameList = new ArrayList<>();
        ArrayList<String> statusList = new ArrayList<>();
        long time;
        for (final CellInfo cellInfo : getAllCellInfo) {
            if (cellInfo != null) {
                long dBm;
                long millisecondsSinceEvent = (SystemClock.elapsedRealtimeNanos() - cellInfo.getTimeStamp()) / 1000000L;
                long timeOfEvent = System.currentTimeMillis() - millisecondsSinceEvent;
                time = (System.currentTimeMillis() - timeOfEvent) / 1000;

                if (cellInfo.isRegistered()) {
                    statusList.add(" - In use!");
                } else {
                    statusList.add("");
                }

                if (cellInfo instanceof CellInfoCdma) {
                    dBm = ((CellInfoCdma) cellInfo).getCellSignalStrength().getDbm();
                    nameList.add("CDMA / 3G / GPS");
                } else {
                    if (cellInfo instanceof CellInfoGsm) {
                        dBm = ((CellInfoGsm) cellInfo).getCellSignalStrength().getDbm();
                        nameList.add("2G");
                    } else {
                        if (cellInfo instanceof CellInfoLte) {
                            dBm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                            nameList.add("LTE / 4G");
                        } else {
                            if (cellInfo instanceof CellInfoWcdma) {
                                dBm = ((CellInfoWcdma) cellInfo).getCellSignalStrength().getDbm();
                                nameList.add("W-CDMA / 3G");
                            } else {
                                dBm = -getValue(cellInfo.toString(), "CellSignalStrength", "level");
                                nameList.add(cellInfo.getClass().getSimpleName());
                            }
                        }
                    }
                }
                if (dBm > -250) {
                    netList.add(dBm);
                    timeList.add(time);
                }
            }
        }
        return new ArrayList[]{netList, timeList, nameList, statusList};
    }

    Long getValue(String fullS, String startS, String stopS) {
        int index = fullS.indexOf(startS) + (startS).length();
        int endIndex = fullS.indexOf(stopS, index);
        String segment = fullS.substring(index, endIndex).trim();
        return new Scanner(segment).useDelimiter("\\D+").nextLong();
    }

}
