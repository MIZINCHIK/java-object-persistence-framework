package io.github.mizinchik.persistence.logging;

import static java.lang.System.lineSeparator;

@SuppressWarnings("RegexpSinglelineJava")
public class Banner {
    private static final String MESSAGE = "Enjoy this majestic almost ORM almost framework";

    static {
        System.out.println(
                lineSeparator() + "                                      "
                        + lineSeparator() + "       ___    _______    ______  "
                        + lineSeparator() + "      / / |  / /  _/ |  / / __ \\ "
                        + lineSeparator() + " __  / /| | / // / | | / / / / / "
                        + lineSeparator() + "/ /_/ / | |/ // /  | |/ / /_/ /  "
                        + lineSeparator() + "\\____/  |___/___/  |___/\\____/   " + MESSAGE + lineSeparator()
        );
    }
}
