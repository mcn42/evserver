package net.mnilsen.evserver;

import java.util.Locale;

/**
 *
 * @author michaeln
 */
public class Util {

    static String formatDouble(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    
}
