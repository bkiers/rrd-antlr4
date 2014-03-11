package nl.bigo.rrdantlr4;

import java.io.InputStream;
import java.util.Scanner;

public class Utils {

    /**
     * Converts an input stream into a String.
     *
     * @param input
     *         the input to convert into a String.
     *
     * @return the input stream as a String.
     */
    public static String slurp(InputStream input) {

        StringBuilder builder = new StringBuilder();
        Scanner scan = new Scanner(input);

        while (scan.hasNextLine()) {
            builder.append(scan.nextLine()).append(scan.hasNextLine() ? "\n" : "");
        }

        return builder.toString();
    }
}
