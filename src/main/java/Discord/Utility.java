package Discord;

import java.time.Duration;
import java.util.ArrayList;


public class Utility {
    public static String msToMinuteSec(long ms) {
        Duration d = Duration.ofMillis(ms);
        int hrs = d.toHoursPart();
        int min = d.toMinutesPart();
        int sec = d.toSecondsPart();
        String hours = String.format("%02d", hrs);
        String minutes = String.format("%02d", min);
        String seconds = String.format("%02d", sec);

        String result = "";
        if (hrs != 0) {
            result = result.concat(hours + ":");
        }
        result = result.concat(minutes + ":");
        result = result.concat(seconds);
        return result;
    }

    public static int containsAnyString(String orig, ArrayList<String> blacklist) {
        int matches = 0;
        orig = orig.toLowerCase();

        for (String s : blacklist) {
            s = s.toLowerCase();
            if (orig.contains(s)) {
                matches++;
            }
        }
        return matches;
    }
}
