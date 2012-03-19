package org.joval.intf.ssh;

public interface ITerminal {
    /**
     * Interrupt character; 255 if none.  Similarly
     * for the other characters.  Not all of these
     * characters are supported on all systems.
     */
    int VINTR = 1;

    /**
     * The quit character (sends SIGQUIT signal on
     * POSIX systems).
     */
    int VQUIT = 2;

    /**
     * Erase the character to left of the cursor.
     */
    int VERASE = 3;

    /**
     * Kill the current input line.
     */
    int VKILL = 4;

    /**
     * End-of-file character (sends EOF from the
     * terminal).
     */
    int VEOF = 5;

    /**
     * End-of-line character in addition to
     * carriage return and/or linefeed.
     */
    int VEOL = 6;

    /**
     * Additional end-of-line character.
     */
    int VEOL2 = 7;

          8     VSTART      Continues paused output (normally
                             control-Q).
          9     VSTOP       Pauses output (normally control-S).
          10    VSUSP       Suspends the current program.
          11    VDSUSP      Another suspend character.
          12    VREPRINT    Reprints the current input line.
          13    VWERASE     Erases a word left of cursor.
          14    VLNEXT      Enter the next character typed literally,
                             even if it is a special character
          15    VFLUSH      Character to flush output.
          16    VSWTCH      Switch to a different shell layer.
          17    VSTATUS     Prints system status line (load, command,
                             pid, etc).
          18    VDISCARD    Toggles the flushing of terminal output.
          30    IGNPAR      The ignore parity flag.  The parameter
                             SHOULD be 0 if this flag is FALSE,
                             and 1 if it is TRUE.
          31    PARMRK      Mark parity and framing errors.
          32    INPCK       Enable checking of parity errors.
          33    ISTRIP      Strip 8th bit off characters.
          34    INLCR       Map NL into CR on input.
          35    IGNCR       Ignore CR on input.
          36    ICRNL       Map CR to NL on input.
          37    IUCLC       Translate uppercase characters to
                             lowercase.
          38    IXON        Enable output flow control.
          39    IXANY       Any char will restart after stop.
          40    IXOFF       Enable input flow control.
          41    IMAXBEL     Ring bell on input queue full.
          50    ISIG        Enable signals INTR, QUIT, [D]SUSP.
          51    ICANON      Canonicalize input lines.
          52    XCASE       Enable input and output of uppercase
                             characters by preceding their lowercase
                             equivalents with "\".
          53    ECHO        Enable echoing.
          54    ECHOE       Visually erase chars.
          55    ECHOK       Kill character discards current line.
          56    ECHONL      Echo NL even if ECHO is off.
          57    NOFLSH      Don't flush after interrupt.
          58    TOSTOP      Stop background jobs from output.
          59    IEXTEN      Enable extensions.
          60    ECHOCTL     Echo control characters as ^(Char).
          61    ECHOKE      Visual erase for line kill.
          62    PENDIN      Retype pending input.
          70    OPOST       Enable output processing.
          71    OLCUC       Convert lowercase to uppercase.
          72    ONLCR       Map NL to CR-NL.
          73    OCRNL       Translate carriage return to newline
                             (output).
          74    ONOCR       Translate newline to carriage
                             return-newline (output).
          75    ONLRET      Newline performs a carriage return
                             (output).
          90    CS7         7 bit mode.
          91    CS8         8 bit mode.
          92    PARENB      Parity enable.
          93    PARODD      Odd parity, else even.

          128 TTY_OP_ISPEED  Specifies the input baud rate in
                              bits per second.
    /**
     * Specifies the output baud rate in
     * bits per second.
     */
    int TTY_OP_OSPEED = 129;
}
